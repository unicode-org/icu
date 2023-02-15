// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 **********************************************************************
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * Copyright (c) 2006-2013, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Created on 2006-7-24 ?
 * Moved from Java 1.4 to 1.5? API by srl 2009-01-16
 */
package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.*;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/*
A utility to report the status change between two ICU releases

To use the utility
1. Generate the XML files
    (put the two ICU releases on your machine ^_^ )
    (generate 'Doxygen' file on Windows platform with Cygwin's help)
    Edit the generated 'Doxygen' file under ICU4C source directory
    a) GENERATE_XML           = YES
    b) Sync the ALIASES definiation
       (For example, copy the ALIASES defination from ICU 3.6
       Doxygen file to ICU 3.4 Doxygen file.)
    c) gerenate the XML files
2. Build the tool
    Download Apache Xerces Java Parser
    Build this file with the library
3. Edit the api-report-config.xml file & Change the file according your real configuration
4. Run the tool to generate the report.
*/

/**
 * CLI tool to report the status change between two ICU releases
 * @author Raymond Yang
 */
public class StableAPI {

    private static final String DOC_FOLDER = "docFolder";
    private static final String INDEX_XML = "index.xml";
    private static final String ICU_SPACE_PREFIX = "ICU ";
    private static final String INITIALIZER_XPATH = "initializer";
    private static final String NAME_XPATH = "name";
    private static final String UVERSIONA = "uvernum_8h.xml";
    private static final String UVERSIONB = "uversion_8h.xml";
    private static final String U_ICU_VERSION = "U_ICU_VERSION";
    /* ICU 4.4+ */
    private static final String ICU_VERSION_XPATHA = "/doxygen/compounddef[@id='uvernum_8h'][@kind='file']/sectiondef[@kind='define']";
    /* ICU <4.4 */
    private static final String ICU_VERSION_XPATHB = "/doxygen/compounddef[@id='uversion_8h'][@kind='file']/sectiondef[@kind='define']";
    private static String ICU_VERSION_XPATH = ICU_VERSION_XPATHA;

    private String leftVer;
    private File leftDir = null;
    // private String leftStatus;
    private String leftMilestone = "";

    private String rightVer;
    private File rightDir = null;
    // private String rightStatus;
    private String rightMilestone = "";

    private InputStream dumpCppXsltStream = null;
    private InputStream dumpCXsltStream = null;
    private InputStream reportXslStream = null;
    private static final String CXSLT = "dumpAllCFunc.xslt";
    private static final String CPPXSLT = "dumpAllCppFunc.xslt";
    private static final String RPTXSLT = "genReport.xslt";

    private File dumpCppXslt;
    private File dumpCXslt;
    private File reportXsl;
    private File resultFile;

    static Map<String, Set<String>> simplifications = new TreeMap<String, Set<String>>();

    static void addSimplification(String prototype0, String prototype) {
        Set<String> s = simplifications.get(prototype);
        if (s == null) {
            s = new TreeSet<String>();
            simplifications.put(prototype, s);
        }
        s.add(prototype0);
    }

    static Set<String> getChangedSimplifications() {
        Set<String> output = new TreeSet<String>();
        for (Map.Entry<String, Set<String>> e : simplifications.entrySet()) {
            if (e.getValue().size() > 1) {
                output.add(e.getKey());
            }
        }
        return output;
    }

    final private static String notFound = "(missing)";

    public static void main(String[] args) throws TransformerException, ParserConfigurationException, SAXException,
            IOException, XPathExpressionException {

        StableAPI t = new StableAPI();
        t.run(args);
    }

    private void run(String[] args) throws XPathExpressionException, TransformerException, ParserConfigurationException,
            SAXException, IOException {
        this.parseArgs(args);
        Set<JoinedFunction> full = new TreeSet<JoinedFunction>();

        System.err.println("Reading C++...");
        Set<JoinedFunction> setCpp = this.getFullList(dumpCppXsltStream, dumpCppXslt.getName());
        full.addAll(setCpp);
        System.out.println("read " + setCpp.size() + " C++.  Reading C:");

        Set<JoinedFunction> setC = this.getFullList(dumpCXsltStream, dumpCXslt.getName());
        full.addAll(setC);

        System.out.println("read " + setC.size() + " C. Setting node:");

        Node fullList = this.setToNode(full);
        // t.dumpNode(fullList,"");

        System.out.println("Node set. Reporting:");

        this.reportSelectedFun(fullList);
        System.out.println("Done. Please check " + this.resultFile);

        Set<String> changedSimp = getChangedSimplifications();
        if (!changedSimp.isEmpty()) {
            System.out.println("--- changed simplifications ---");
            for (String k : changedSimp) {
                System.out.println(k);
                for (String s : simplifications.get(k)) {
                    System.out.println("\t" + s);
                }
            }
        }
    }

    private void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.length() == 0) {
                continue;
            }
            if (arg.equals("--help")) {
                printUsage();
            } else if (arg.equals("--oldver")) {
                leftVer = args[++i];
            } else if (arg.equals("--olddir")) {
                leftDir = new File(args[++i]);
            } else if (arg.equals("--newver")) {
                rightVer = args[++i];
            } else if (arg.equals("--newdir")) {
                rightDir = new File(args[++i]);
            } else if (arg.equals("--cxslt")) {
                dumpCXslt = new File(args[++i]);
            } else if (arg.equals("--cppxslt")) {
                dumpCppXslt = new File(args[++i]);
            } else if (arg.equals("--reportxslt")) {
                reportXsl = new File(args[++i]);
            } else if (arg.equals("--resultfile")) {
                resultFile = new File(args[++i]);
            } else {
                System.out.println("Unknown option: " + arg);
                printUsage();
            }
        }

        dumpCppXsltStream = loadStream(CPPXSLT, "--cppxslt", dumpCppXslt);
        dumpCXsltStream = loadStream(CXSLT, "--cxslt", dumpCXslt);
        reportXslStream = loadStream(RPTXSLT, "--reportxslt", reportXsl);

        leftVer = trimICU(setVer(leftVer, "old", leftDir));
        rightVer = trimICU(setVer(rightVer, "new", rightDir));
    }

    @SuppressWarnings("resource")
    private InputStream loadStream(String name, String argName, File argFile) {
        InputStream stream = null;
        if (argFile != null) {
            try {
                stream = new FileInputStream(argFile);
                System.out.println("Loaded file " + argFile.getName());
            } catch (IOException ioe) {
                throw new RuntimeException(
                        "Error: Could not load " + argName + " " + argFile.getPath() + " - " + ioe.toString(), ioe);
            }
        } else {
            stream = StableAPI.class.getResourceAsStream(name);
            if (stream == null) {
                throw new InternalError("No resource found for " + StableAPI.class.getPackage().getName() + "/" + name
                        + " -   use " + argName);
            } else {
                System.out.println("Loaded resource " + name);
            }
        }
        return stream;
    }

    private static Set<String> warnSet = new TreeSet<String>();

    private static void warn(String what) {
        if (!warnSet.contains(what)) {
            System.out.println("Warning: " + what);
            if (warnSet.isEmpty()) {
                System.out.println(" (These warnings are only printed one time each.)");
            }
            warnSet.add(what);
        }
    }

    private static boolean didWarnSuperTrim = false;

    private static String trimICU(String ver) {
        Matcher icuVersionMatcher = Pattern.compile("ICU *\\d+(\\.\\d+){0,2}").matcher(ver);
        if (icuVersionMatcher.find()) {
            return icuVersionMatcher.group();
        } else {
            warn("@whatever not followed by ICU <version number>");
            return "";
        }
    }

    private String setVer(String prevVer, String whichVer, File dir) {
        String UVERSION = UVERSIONA;
        if (dir == null) {
            System.out.println("--" + whichVer + "dir not set.");
            printUsage(); /* exits */
        } else if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("--" + whichVer + "dir=" + dir.getName() + " does not exist or is not a directory.");
            printUsage(); /* exits */
        }
        String result = null;
        // looking for: <name>U_ICU_VERSION</name> in uversion_8h.xml:
        // <initializer>&quot;3.8.1&quot;</initializer>
        try {
            File verFile = new File(dir, UVERSION);
            if (!verFile.exists()) {
                UVERSION = UVERSIONB;
                ICU_VERSION_XPATH = ICU_VERSION_XPATHB;
                verFile = new File(dir, UVERSION);
            } else {
                ICU_VERSION_XPATH = ICU_VERSION_XPATHA;
            }
            Document doc = getDocument(verFile);
            DOMSource uvernum_h = new DOMSource(doc);
            XPath xpath = XPathFactory.newInstance().newXPath();

            Node defines = (Node) xpath.evaluate(ICU_VERSION_XPATH, uvernum_h.getNode(), XPathConstants.NODE);

            if (defines == null) {
                System.err.println("can't load from " + verFile.getName() + ":" + ICU_VERSION_XPATH);
            }

            NodeList nList = defines.getChildNodes();
            for (int i = 0; result == null && (i < nList.getLength()); i++) {
                Node ln = nList.item(i);
                if (!"memberdef".equals(ln.getNodeName())) {
                    continue;
                }
                Node name = (Node) xpath.evaluate(NAME_XPATH, ln, XPathConstants.NODE);
                if (name == null)
                    continue;

                // System.err.println("Gotta node: " + name);

                Node nameVal = name.getFirstChild();
                if (nameVal == null)
                    nameVal = name;

                String nameStr = nameVal.getNodeValue();
                if (nameStr == null)
                    continue;

                // System.err.println("Gotta name: " + nameStr);

                if (nameStr.trim().equals(U_ICU_VERSION)) {
                    Node initializer = (Node) xpath.evaluate(INITIALIZER_XPATH, ln, XPathConstants.NODE);
                    if (initializer == null)
                        System.err.println("initializer with no value");
                    Node initVal = initializer.getFirstChild();
                    // if(initVal==null) initVal = initializer;
                    String initStr = initVal.getNodeValue().trim().replaceAll("\"", "");
                    result = ICU_SPACE_PREFIX + initStr;
                    System.err.println("Detected " + whichVer + " version: " + result);

                    String milestoneOf = "";

                    // TODO: #1 use UVersionInfo. (this tool doesn't depend on ICU4J yet)
                    // #2 move this to a utility function: strip/"explain" an ICU version #.
                    if (result.startsWith("ICU ")) {
                        String vers[] = result.substring(4).split("\\.");
                        int maj = Integer.parseInt(vers[0]);
                        int min = vers.length > 1 ? Integer.parseInt(vers[1]) : 0;
                        int micr = vers.length > 2 ? Integer.parseInt(vers[2]) : 0;
                        int patch = vers.length > 3 ? Integer.parseInt(vers[3]) : 0;
                        System.err.println(
                                " == [" + vers.toString() + "] " + maj + " . " + min + " . " + micr + " . " + patch);
                        if (maj >= 49) {
                            // new scheme: 49 and following.
                            String truncVersion = "ICU " + maj;
                            if (min == 0) {
                                milestoneOf = " (m" + micr + ")";
                                System.err.println("    .. " + milestoneOf + " is a milestone towards " + truncVersion);
                            } else if (min == 1) {
                                // Don't denote as milestone
                                result = "ICU " + (maj);
                                System.err.println("    .. " + milestoneOf + " is the release of " + truncVersion);
                            } else {
                                milestoneOf = " (update #" + (min - 1) + ": " + result.substring(4) + ")";
                                result = "ICU " + (maj);
                                System.err.println("    .. " + milestoneOf + " is an update to  " + truncVersion);
                            }
                            // always truncate to major # for comparing tags.
                            result = truncVersion;
                            if (maj >= 71) {
                              // Clear minor and micro version in API change report.
                              milestoneOf = "";
                            }
                        } else {
                            // old scheme - 1.0.* .. 4.8.*
                            String truncVersion = "ICU " + maj + "." + min;
                            if ((min % 2) == 1) {
                                milestoneOf = " (" + maj + "." + (min + 1) + "m" + micr + ")";
                                truncVersion = "ICU " + (maj) + "." + (min + 1);
                                System.err.println("    .. " + milestoneOf + " is a milestone towards " + truncVersion);
                            } else if (micr == 0 && patch == 0) {
                                System.err.println("    .. " + milestoneOf + " is the release of " + truncVersion);
                            } else {
                                milestoneOf = " (update " + micr + "." + patch + ")";
                                System.err.println("    .. " + milestoneOf + " is an update to " + truncVersion);
                            }
                            result = truncVersion;
                        }
                        if (whichVer.equals("new")) {
                            rightMilestone = milestoneOf;
                        } else {
                            leftMilestone = milestoneOf;
                        }
                    }
                }

            }
            // dumpNode(defines,"");
        } catch (Throwable t) {
            t.printStackTrace();
            System.err.println(
                    "Warning: Couldn't get " + whichVer + " version from " + UVERSION + " - reverting to " + prevVer);
            result = prevVer;
        }

        if (result != null) {

        }

        if (prevVer != null) {
            if (result != null) {
                if (!result.equals(prevVer)) {
                    System.err.println("Note: Detected " + result + " version but we'll use your requested --"
                            + whichVer + "ver " + prevVer);
                    result = prevVer;
                    if (!rightMilestone.isEmpty() && whichVer.equals("new")) {
                        System.err.println(" .. ignoring milestone indicator " + rightMilestone);
                        rightMilestone = "";
                    }
                    if (!leftMilestone.isEmpty() && !whichVer.equals("new")) {
                        leftMilestone = "";
                    }
                } else {
                    System.err.println("Note: You don't need to use  '--" + whichVer + "ver " + result
                            + "' anymore - we detected it correctly.");
                }
            } else {
                System.err.println(
                        "Note: Didn't detect version so we'll use your requested --" + whichVer + "ver " + prevVer);
                result = prevVer;
                if (!rightMilestone.isEmpty() && whichVer.equals("new")) {
                    System.err.println(" .. ignoring milestone indicator " + rightMilestone);
                    rightMilestone = "";
                }
                if (!leftMilestone.isEmpty() && !whichVer.equals("new")) {
                    leftMilestone = "";
                }
            }
        }

        if (result == null) {
            System.err.println("prevVer=" + prevVer);
            System.err.println("Error: You'll need to use the option  \"--" + whichVer
                    + "ver\"  because we could not detect an ICU version in " + UVERSION);
            throw new InternalError("Error: You'll need to use the option  \"--" + whichVer
                    + "ver\"  because we could not detect an ICU version in " + UVERSION);
        }

        return result;
    }

    private static void printUsage() {
        System.out.println("Usage: StableAPI option* target*");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    --help          Print this text");
        System.out.println("    --oldver        Version of old version of ICU (optional)");
        System.out.println("    --olddir        Directory that contains xml docs of old version");
        System.out.println("    --newver        Version of new version of ICU (optional)");
        System.out.println("    --newdir        Directory that contains xml docs of new version");
        System.out.println("    --cxslt         XSLT file for C docs");
        System.out.println("    --cppxslt       XSLT file for C++ docs");
        System.out.println("    --reportxslt    XSLT file for report docs");
        System.out.println("    --resultfile    Output file");
        System.exit(-1);
    }

    static String getAttr(Node node, String attrName) {
        if (node.getAttributes() == null && node.getNodeType() == 3) {
            // return "(text node 3)";
            return "(Node: " + node.toString() + " )";
            // return
            // node.getFirstChild().getAttributes().getNamedItem(attrName).getNodeValue();
        }

        try {
            return node.getAttributes().getNamedItem(attrName).getNodeValue();
        } catch (NullPointerException npe) {
            if (node.getAttributes() == null) {
                throw new InternalError(
                        "[no attributes Can't get attr " + attrName + " out of node " + node.getNodeName() + ":"
                                + node.getNodeType() + ":" + node.getNodeValue() + "@" + node.getTextContent());
            } else if (node.getAttributes().getNamedItem(attrName) == null) {
                return null;
                // throw new InternalError("No attribute named: "+attrName);
            } else {
                System.err.println("Can't get attr " + attrName + ": " + npe.toString());
            }
            npe.printStackTrace();
            throw new InternalError("Can't get attr " + attrName);
        }
    }

    static String getAttr(NamedNodeMap attrList, String attrName) {
        return attrList.getNamedItem(attrName).getNodeValue();
    }

    static class Function implements Comparable<Function> {
        public String prototype;
        public String id;
        public String status;
        public String version;
        public String file;
        public String comparableName;
        public String comparablePrototype;

        public boolean equals(Function right) {
            return this.comparablePrototype.equals(right.comparablePrototype);
        }

        static Function fromXml(Node n) {
            Function f = new Function();
            f.prototype = getAttr(n, "prototype");

            if ("yes".equals(getAttr(n, "static")) && !f.prototype.contains("static")) {
                f.prototype = "static ".concat(f.prototype);
            }

            f.id = getAttr(n, "id");
            f.status = getAttr(n, "status");
            f.version = trimICU(getAttr(n, "version"));
            f.file = getAttr(n, "file");
            f.purifyPrototype();

            f.simplifyPrototype();

            f.comparablePrototype = f.prototype;
            // Modify the prototype here, but don't display it to the user. ( Char16Ptr -->
            // char16_t* etc )
            for (int i = 0; i < aliasList.length; i += 2) {
                f.comparablePrototype = f.comparablePrototype.replaceAll(aliasList[i + 0], aliasList[i + 1]);
            }

            if (f.file == null) {
                f.file = "{null}";
            } else {
                f.file = Function.getBasename(f.file);
            }
            f.comparableName = f.comparableName();
            return f;
        }

        /**
         * Convert string to basename.
         * 
         * @param str
         * @return
         */
        private static String getBasename(String str) {
            int i = str.lastIndexOf("/");
            str = i == -1 ? str : str.substring(i + 1);
            return str;
        }

        static private String replList[] = { "[ ]*\\([ ]*void[ ]*\\)", "() ", // (void) => ()
                "[ ]*,", ", ", // No spaces preceding commas.
                "[ ]*\\*[ ]*", "* ", // No spaces preceding '*'.
                "[ ]*=[ ]*0[ ]*$", "=0 ", // No spaces in " = 0".
                "[ ]{2,}", " ", "\n", " " // Multiple spaces collapse to single.
        };

        /**
         * these are noted as deltas.
         */
        static private String simplifyList[] = {
                "[ ]*=[ ]*0[ ]*$", "",// remove pure virtual
                                      //  TODO: notify about this difference, separately
                "[ ]*U_NOEXCEPT", "", // remove U_NOEXCEPT (this was fixed in Doxyfile, but fixing here so it is
                                      //  retroactive)
                "[ ]*override", "",   // remove override
                // Simplify possibly-covariant functions to void*
                "^([^\\* ]+)\\*(.*)::(clone|safeClone|cloneAsThawed|freeze|createBufferClone)\\((.*)", "void*$2::$3($4",
                "\\s+$", "", // remove trailing spaces.
                "^U_NAMESPACE_END ", "", // Bug in processing of uspoof.h
                "\\bUBool\\b", "bool"
        };

        /**
         * This list is applied only for comparisons. The resulting string is NOT shown
         * to the user. These should be ignored as far as changes go. func(UChar) ===
         * func(char16_t)
         */
        static private String aliasList[] = { "UChar", "char16_t", "ConstChar16Ptr", "const char16_t*", "Char16Ptr",
                "char16_t*", };

        /**
         * Special cases:
         *
         * Remove the status attribute embedded in the C prototype
         *
         * Remove the virtual keyword in Cpp prototype
         */
        private void purifyPrototype() {
            // refer to 'umachine.h'
            String statusList[] = { "U_CAPI", "U_STABLE", "U_DRAFT", "U_DEPRECATED", "U_OBSOLETE", "U_INTERNAL",
                    "virtual", "U_EXPORT2", "U_I18N_API", "U_COMMON_API" };
            for (int i = 0; i < statusList.length; i++) {
                String s = statusList[i];
                prototype = prototype.replaceAll(s, "");
                prototype = prototype.trim();
            }

            for (int i = 0; i < replList.length; i += 2) {
                prototype = prototype.replaceAll(replList[i + 0], replList[i + 1]);
            }

            prototype = prototype.trim();

            // Now, remove parameter names!
            StringBuffer out = new StringBuffer();
            StringBuffer in = new StringBuffer(prototype);
            int openParen = in.indexOf("(");
            int closeParen = in.lastIndexOf(")");

            if (openParen == -1 || closeParen == -1)
                return; // exit, malformed?
            if (openParen + 1 == closeParen)
                return; // exit: ()

            out.append(in, 0, openParen + 1); // prelude

            for (int left = openParen + 1; left < closeParen;) {
                int right = in.indexOf(",", left + 1); // right edge
                if (right >= closeParen || right == -1)
                    right = closeParen; // found last comma

                // System.err.println("Considering " + left + " / " + right + " - " + closeParen
                // + " : " + in.substring(left, right));

                if (left == right)
                    continue;

                // find variable name
                int rightCh = right - 1;
                if (rightCh == left) { // 1 ch- break
                    out.append(in, left, right);
                    continue;
                }
                // eat whitespace at right
                int nameEndCh = rightCh;
                while (nameEndCh > left && Character.isWhitespace(in.charAt(nameEndCh))) {
                    nameEndCh--;
                }
                int nameStartCh = nameEndCh;
                while (nameStartCh > left && Character.isJavaIdentifierPart(in.charAt(nameStartCh))) {
                    nameStartCh--;
                }

                // now, did we find something to skip?
                if (nameStartCh > left && nameEndCh > nameStartCh) {
                    out.append(in, left, nameStartCh + 1);
                } else {
                    // pass through
                    out.append(in, left, right);
                }

                left = right;
            }

            out.append(in, closeParen, in.length()); // postlude

            // Delete any doubled whitespace.
            for (int p = 1; p < out.length(); p++) {
                char prev = out.charAt(p - 1);
                if (Character.isWhitespace(prev)) {
                    while (out.length() > p && (Character.isWhitespace(out.charAt(p)))) {
                        out.deleteCharAt(p);
                    }
                    if (out.length() > p) {
                        // any trailings to delete?
                        char curr = out.charAt(p);
                        if (curr == ',' || curr == ')' || curr == '*' || curr == '&') { // delete spaces before these.
                            out.deleteCharAt(--p);
                            continue;
                        }
                    }
                }
            }

            // System.err.println(prototype+" -> " + out.toString());
            prototype = out.toString();
        }

        private void simplifyPrototype() {
            if (prototype.startsWith("#define")) {
                return;
            }
            final String prototype0 = prototype;
            for (int i = 0; i < simplifyList.length; i += 2) {
                prototype = prototype.replaceAll(simplifyList[i + 0], simplifyList[i + 1]);
            }
            if (!prototype0.equals(prototype)) {
                addSimplification(prototype0, prototype);
            }
        }

        /**
         * @Override
         */
        public int compareTo(Function o) {
            return comparableName.compareTo(((Function) o).comparableName);
        }

        public String comparableName() {
            return file + "|" + comparablePrototype + "|" + status + "|" + version + "|" + id;
        }
    }

    static class JoinedFunction implements Comparable<JoinedFunction> {
        public String prototype;
        public String leftRefId;
        public String leftStatus;
        public String leftVersion;
        public String rightVersion;
        public String leftFile;
        public String rightRefId;
        public String rightStatus;
        public String rightFile;

        public String comparableName;

        static JoinedFunction fromLeftFun(Function left) {
            JoinedFunction u = new JoinedFunction();
            u.prototype = left.prototype;
            u.leftRefId = left.id;
            u.leftStatus = left.status;
            u.leftFile = left.file;
            u.rightRefId = notFound;
            // u.rightVersion = nul;
            u.leftVersion = left.version;
            u.rightStatus = notFound;
            u.rightFile = notFound;
            u.comparableName = left.comparableName;
            return u;
        }

        static JoinedFunction fromRightFun(Function right) {
            JoinedFunction u = new JoinedFunction();
            u.prototype = right.prototype;
            u.leftRefId = notFound;
            u.leftStatus = notFound;
            u.leftFile = notFound;
            // u.leftVersion = nul;
            u.rightVersion = right.version;
            u.rightRefId = right.id;
            u.rightStatus = right.status;
            u.rightFile = right.file;
            u.comparableName = right.comparableName;
            return u;
        }

        static JoinedFunction fromTwoFun(Function left, Function right) {
            if (!left.equals(right))
                throw new Error();
            JoinedFunction u = new JoinedFunction();
            u.prototype = left.prototype;
            u.leftRefId = left.id;
            u.leftStatus = left.status;
            u.leftFile = left.file;
            u.rightRefId = right.id;
            u.rightStatus = right.status;
            u.leftVersion = left.version;
            u.rightVersion = right.version;
            u.rightFile = right.file;
            u.comparableName = left.comparableName + "+" + right.comparableName;
            return u;
        }

        Element toXml(Document doc) {
            Element ele = doc.createElement("func");
            ele.setAttribute("prototype", formatCode(prototype));
            // ele.setAttribute("leftRefId", leftRefId);

            ele.setAttribute("leftStatus", leftStatus);
            // ele.setAttribute("rightRefId", rightRefId);
            ele.setAttribute("rightStatus", rightStatus);
            ele.setAttribute("leftVersion", leftVersion);
            // ele.setAttribute("rightRefId", rightRefId);
            ele.setAttribute("rightVersion", rightVersion);

            // String f = rightRefId.equals(notFound) ? leftRefId : rightRefId;
            // int tail = f.indexOf("_");
            // f = tail != -1 ? f.substring(0, tail) : f;
            // f = f.startsWith("class") ? f.replaceFirst("class","") : f;
            String f = rightFile.equals(notFound) ? leftFile : rightFile;
            ele.setAttribute("file", f);
            return ele;
        }

        public int compareTo(JoinedFunction o) {
            return comparableName.compareTo(o.comparableName);
        }

        public boolean equals(Function right) {
            return this.prototype.equals(right.prototype);
        }
    }

    TransformerFactory transFac = TransformerFactory.newInstance();

    Transformer makeTransformer(InputStream is, String name) {
        if (is == null) {
            throw new InternalError("No inputstream set for " + name);
        }
        System.err.println("Transforming from: " + name);
        Transformer t;
        try {
            StreamSource ss = new StreamSource(is);
            ss.setSystemId(new File("."));
            t = transFac.newTransformer(ss);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            throw new InternalError("Couldn't make transformer for " + name + " - " + e.getMessageAndLocation());
        }
        if (t == null) {
            throw new InternalError("Couldn't make transformer for " + name);
        }
        return t;
    }

    private void reportSelectedFun(Node joinedNode)
            throws TransformerException, ParserConfigurationException, SAXException, IOException {
        Transformer report = makeTransformer(reportXslStream, RPTXSLT);
        // report.setParameter("leftStatus", leftStatus);
        report.setParameter("leftVer", leftVer);
        // report.setParameter("rightStatus", rightStatus);
        report.setParameter("ourYear", new Integer(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR)));
        report.setParameter("rightVer", rightVer);
        report.setParameter("rightMilestone", rightMilestone);
        report.setParameter("leftMilestone", leftMilestone);
        report.setParameter("dateTime", new GregorianCalendar().getTime());
        report.setParameter("notFound", notFound);

        DOMSource src = new DOMSource(joinedNode);

        Result res = new StreamResult(resultFile);
        // DOMResult res = new DOMResult();
        report.transform(src, res);
        // dumpNode(res.getNode(),"");
    }

    private Set<JoinedFunction> getFullList(InputStream dumpXsltStream, String dumpXsltFile)
            throws TransformerException, ParserConfigurationException, XPathExpressionException, SAXException,
            IOException {
        // prepare transformer
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/list";
        Transformer transformer = makeTransformer(dumpXsltStream, dumpXsltFile);

        // InputSource leftSource = new InputSource(leftDir + "index.xml");
        DOMSource leftIndex = new DOMSource(getDocument(new File(leftDir, INDEX_XML)));
        DOMResult leftResult = new DOMResult();
        transformer.setParameter(DOC_FOLDER, leftDir);
        transformer.transform(leftIndex, leftResult);

        // Node leftList = XPathAPI.selectSingleNode(leftResult.getNode(),"/list");
        Node leftList = (Node) xpath.evaluate(expression, leftResult.getNode(), XPathConstants.NODE);
        if (leftList == null) {
            // dumpNode(xsltSource.getNode());
            dumpNode(leftResult.getNode());
            // dumpNode(leftIndex.getNode());
            System.out.flush();
            System.err.flush();
            throw new InternalError("getFullList(" + dumpXsltFile.toString() + ") returned a null left " + expression);
        }

        xpath.reset(); // reuse

        DOMSource rightIndex = new DOMSource(getDocument(new File(rightDir, INDEX_XML)));
        DOMResult rightResult = new DOMResult();
        transformer.setParameter(DOC_FOLDER, rightDir);
        System.err.println("Loading: " + dumpXsltFile.toString());
        transformer.transform(rightIndex, rightResult);
        System.err.println("   .. loaded: " + dumpXsltFile.toString());
        Node rightList = (Node) xpath.evaluate(expression, rightResult.getNode(), XPathConstants.NODE);
        if (rightList == null) {
            throw new InternalError("getFullList(" + dumpXsltFile.toString() + ") returned a null right " + expression);
        }
        // dumpNode(rightList,"");

        Set<Function> leftSet = nodeToSet(leftList);
        Set<Function> rightSet = nodeToSet(rightList);
        Set<JoinedFunction> joined = fullJoin(leftSet, rightSet);
        return joined;
        // joinedNode = setToNode(joined);
        // dumpNode(joinedNode,"");
        // return joinedNode;
    }

    /**
     * @param node
     * @return Set<Fun>
     */
    private Set<Function> nodeToSet(Node node) {
        Set<Function> s = new TreeSet<Function>();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            s.add(Function.fromXml(n));
        }
        return s;
    }

    /**
     * @param set Set<JoinedFun>
     * @return
     * @throws ParserConfigurationException
     */
    private Node setToNode(Set<JoinedFunction> set) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc = dbf.newDocumentBuilder().newDocument();
        Element root = doc.createElement("list");
        doc.appendChild(root);
        for (Iterator<JoinedFunction> iter = set.iterator(); iter.hasNext();) {
            JoinedFunction fun = iter.next();
            root.appendChild(fun.toXml(doc));
        }

        // add the 'changed' stuff
        Element root2 = doc.createElement("simplifications");
        root.appendChild(root2);
        {
            for (String simplification : getChangedSimplifications()) {
                Element subSimplification = doc.createElement("simplification");
                Element baseElement = doc.createElement("base");
                baseElement.appendChild(doc.createTextNode(simplification));
                subSimplification.appendChild(baseElement);

                root2.appendChild(subSimplification);

                for (String change : simplifications.get(simplification)) {
                    Element changeElement = doc.createElement("change");
                    changeElement.appendChild(doc.createTextNode(change));
                    subSimplification.appendChild(changeElement);
                }
            }
        }

        return doc;
    }

    /**
     * full-join two Set on 'prototype'
     *
     * @param left  Set<Fun>
     * @param right Set<Fun>
     * @return Set<JoinedFun>
     */
    private static Set<JoinedFunction> fullJoin(Set<Function> left, Set<Function> right) {

        Set<JoinedFunction> joined = new TreeSet<JoinedFunction>(); // Set<JoinedFun>
        Set<Function> common = new TreeSet<Function>(); // Set<Fun>
        for (Iterator<Function> iter1 = left.iterator(); iter1.hasNext();) {
            Function f1 = iter1.next();
            for (Iterator<Function> iter2 = right.iterator(); iter2.hasNext();) {
                Function f2 = iter2.next();
                if (f1.equals(f2)) {
                    // should add left item to common set
                    // since we will remove common items with left set later
                    common.add(f1);
                    joined.add(JoinedFunction.fromTwoFun(f1, f2));
                    right.remove(f2);
                    break;
                }
            }
        }

        for (Iterator<Function> iter = common.iterator(); iter.hasNext();) {
            Function f = iter.next();
            left.remove(f);
        }

        for (Iterator<Function> iter = left.iterator(); iter.hasNext();) {
            Function f = iter.next();
            joined.add(JoinedFunction.fromLeftFun(f));
        }

        for (Iterator<Function> iter = right.iterator(); iter.hasNext();) {
            Function f = iter.next();
            joined.add(JoinedFunction.fromRightFun(f));
        }
        return joined;
    }

    private static void dumpNode(Node n) {
        dumpNode(n, "");
    }

    /**
     * Dump out a node for debugging. Recursive fcn
     * 
     * @param n
     * @param pre
     */
    private static void dumpNode(Node n, String pre) {
        String opre = pre;
        pre += " ";
        System.out.print(opre + "<" + n.getNodeName());
        // dump attribute
        NamedNodeMap attr = n.getAttributes();
        if (attr != null) {
            for (int i = 0; i < attr.getLength(); i++) {
                System.out.print(
                        "\n" + pre + "   " + attr.item(i).getNodeName() + "=\"" + attr.item(i).getNodeValue() + "\"");
            }
        }
        System.out.println(">");

        // dump value
        String v = pre + n.getNodeValue();
        if (n.getNodeType() == Node.TEXT_NODE)
            System.out.println(v);

        // dump sub nodes
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node ln = nList.item(i);
            dumpNode(ln, pre + " ");
        }
        System.out.println(opre + "</" + n.getNodeName() + ">");
    }

    private static DocumentBuilder theBuilder = null;
    private static DocumentBuilderFactory dbf = null;

    private synchronized static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        if (theBuilder == null) {
            dbf = DocumentBuilderFactory.newInstance();
            theBuilder = dbf.newDocumentBuilder();
        }
        return theBuilder;
    }

    private static Document getDocument(File file) throws ParserConfigurationException, SAXException, IOException {
        FileInputStream fis = new FileInputStream(file);
        InputSource inputSource = new InputSource(fis);
        Document doc = getDocumentBuilder().parse(inputSource);
        return doc;
    }

    static boolean tried = false;
    static Formatter aFormatter = null;

    public interface Formatter {
        public String formatCode(String s);
    }

    public static String format_keywords[] = { "enum", "#define", "static" };

    /**
     * Attempt to use a pretty formatter
     * 
     * @param prototype2
     * @return
     */
    public static String formatCode(String prototype2) {
        if (!tried) {
            String theFormatter = StableAPI.class.getPackage().getName() + ".CodeFormatter";
            try {
                @SuppressWarnings("unchecked")
                Class<Formatter> formatClass = (Class<Formatter>) Class.forName(theFormatter);
                aFormatter = (Formatter) formatClass.newInstance();
            } catch (Exception e) {
                System.err.println("Note: Couldn't load " + theFormatter);
                aFormatter = new Formatter() {

                    public String formatCode(String s) {
                        String str = HTMLSafe(s.trim());
                        for (String keyword : format_keywords) {
                            if (str.startsWith(keyword)) {
                                str = str.replaceFirst(keyword, "<tt>" + keyword + "</tt>");
                            }
                        }
                        return str;
                    }

                };
            }
            tried = true;
        }
        if (aFormatter != null) {
            return aFormatter.formatCode(prototype2);
        } else {
            return HTMLSafe(prototype2);
        }
    }

    public static String HTMLSafe(String s) {
        if (s == null)
            return null;

        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;");
    }

}
