/*
 **********************************************************************
 * Copyright (c) 2006-2009, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Created on 2006-7-24 ?
 * Moved from Java 1.4 to 1.5? API by srl 2009-01-16
 */
package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
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


/**
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

 * @author Raymond Yang
 */
public class StableAPI {

    private static final String DOC_FOLDER = "docFolder";
	private static final String INDEX_XML = "index.xml";
	private static final String ICU_SPACE_PREFIX = "ICU ";
	private static final String INITIALIZER_XPATH = "initializer";
	private static final String NAME_XPATH = "name";
	private static final String UVERSION = "uversion_8h.xml";
    private static final String U_ICU_VERSION = "U_ICU_VERSION";
	private static final String ICU_VERSION_XPATH = "/doxygen/compounddef[@id='uversion_8h'][@kind='file']/sectiondef[@kind='define']";

	private String leftVer;
    private File leftDir = null;
//    private String leftStatus;
    
    private String rightVer;
    private File rightDir = null;
//    private String rightStatus;
    
    private File dumpCppXslt;
    private File dumpCXslt;
    private File reportXsl;
    private File resultFile;
    
    final private static String nul = "None"; 

    public static void main(String[] args) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        
        StableAPI t = new StableAPI();
        t.run(args); 
    }
    
    private void run(String[] args) throws XPathExpressionException, TransformerException, ParserConfigurationException, SAXException, IOException {
        this.parseArgs(args);
        Set full = new HashSet();

        System.err.println("Reading C++...");
        Set setCpp = this.getFullList(this.dumpCppXslt);
        full.addAll(setCpp);
        System.out.println("read "+setCpp.size() +" C++.  Reading C:");
        
        Set setC = this.getFullList(this.dumpCXslt);
        full.addAll(setC);

        System.out.println("read "+setC.size() +" C. Setting node:");
        
        Node fullList = this.setToNode(full);
//        t.dumpNode(fullList,"");
        
        System.out.println("Node set. Reporting:");
        
        this.reportSelectedFun(fullList);
        System.out.println("Done. Please check " + this.resultFile);
    }


    private void parseArgs(String[] args){
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.length() == 0) {
                continue;
            }
            if (arg.equals("--help") ) {
                printUsage();
            } else if (arg.equals("--oldver") ) {
                leftVer = args[++i];
            } else if (arg.equals("--olddir") ) {
                leftDir = new File(args[++i]);
            } else if (arg.equals("--newver")) {
                rightVer = args[++i];
            } else if (arg.equals("--newdir")) {
                rightDir = new File(args[++i]);
            } else if (arg.equals("--cxslt") ) {
                dumpCXslt = new File(args[++i]);
            } else if (arg.equals("--cppxslt") ) {
                dumpCppXslt = new File(args[++i]);
            } else if (arg.equals("--reportxslt") ) {
                reportXsl = new File(args[++i]);
            } else if (arg.equals("--resultfile")) {
                resultFile = new File(args[++i]);
            } else {
                System.out.println("Unknown option: "+arg);
               printUsage();
            } 
        }
                
        leftVer = trimICU(setVer(leftVer, "old", leftDir));
        rightVer = trimICU(setVer(rightVer, "new", rightDir));
    }

    private static Set warnSet = new HashSet(); // No Generics because we built with JDK 1.4 for now
    private static void warn(String what) {
        if(!warnSet.contains(what)) {
               System.out.println("Warning: "+what);
               if(warnSet.isEmpty()) {
                      System.out.println(" (These warnings are only printed one time each.)");
               }
               warnSet.add(what);
        }
    } 
    
    private static String trimICU(String ver) {
        final String ICU_ = ICU_SPACE_PREFIX;
        final String ICU = "ICU";
        if(ver != null) { // trim everything before the 'ICU...'
            ver = ver.trim();
            int icuidx = ver.lastIndexOf(ICU_);
            int icuidx1 = ver.lastIndexOf(ICU);
            if(icuidx>=0) {
                ver = ver.substring(icuidx+ICU_.length()).trim();
            } else if(icuidx1>=0) {
                warn("trimming: '" + ver + "'");
                ver = ver.substring(icuidx1+ICU.length()).trim();
            } else {
                int n;
                for(n=ver.length()-1;n>0 && ((ver.charAt(n)=='.') || Character.isDigit(ver.charAt(n))) ;n--)
                    ;
                warn("super-trimming: '" + ver + "'");
                if(n>0) {
                    ver = ver.substring(n+1).trim();
                }
            }
        } 
        return ver;
    }
    
    private String setVer(String prevVer, String whichVer, File dir) {
    	if(dir==null) {
    		System.out.println("--"+whichVer+"dir not set.");
    		printUsage(); /* exits */
    	} else if(!dir.exists()||!dir.isDirectory()) {
    		System.out.println("--"+whichVer+"dir="+dir.getName()+" does not exist or is not a directory.");
    		printUsage(); /* exits */
    	}
        String result = null;
        // looking for: <name>U_ICU_VERSION</name> in uversion_8h.xml:        <initializer>&quot;3.8.1&quot;</initializer>
        try {
        	File verFile = new File(dir, UVERSION);
            Document doc = getDocument(verFile);
            DOMSource uversion_h = new DOMSource(doc);
            XPath xpath = XPathFactory.newInstance().newXPath();

            Node defines = (Node)xpath.evaluate(ICU_VERSION_XPATH, uversion_h.getNode(), XPathConstants.NODE);

            NodeList nList = defines.getChildNodes();
            for (int i = 0; result==null&& (i < nList.getLength()); i++) {
                Node ln = nList.item(i);
                if(!"memberdef".equals(ln.getNodeName())) {
                    continue;
                }
                Node name = (Node)xpath.evaluate(NAME_XPATH, ln, XPathConstants.NODE);
                if(name==null) continue;
                
               // System.err.println("Gotta node: " + name);
                
                Node nameVal = name.getFirstChild();
                if(nameVal==null) nameVal = name;
                
                String nameStr = nameVal.getNodeValue();
                if(nameStr==null) continue;

               // System.err.println("Gotta name: " + nameStr);
                
                if(nameStr.trim().equals(U_ICU_VERSION)) {
                    Node initializer = (Node)xpath.evaluate(INITIALIZER_XPATH, ln, XPathConstants.NODE);
                    if(initializer==null) System.err.println("initializer with no value");
                    Node initVal = initializer.getFirstChild();
//                    if(initVal==null) initVal = initializer;
                    String initStr = initVal.getNodeValue().trim().replaceAll("\"","");
                    result = ICU_SPACE_PREFIX+initStr;
                    System.err.println("Detected "+whichVer + " version: " + result);
                }
                
            }
            //dumpNode(defines,"");
        } catch(Throwable t) {
            t.printStackTrace();
            System.err.println("Warning: Couldn't get " + whichVer+  " version from "+ UVERSION + " - reverting to " + prevVer);
            result = prevVer;
        }
        
        if(result != null) {
        	
        }
        
        if(prevVer != null) {
            if(result != null) {
                if(!result.equals(prevVer)) { 
                    System.err.println("Note: Detected " + result + " version but we'll use your requested --"+whichVer+"ver "+prevVer);
                    result = prevVer;
                } else {
                    System.err.println("Note: You don't need to use  '--"+whichVer+"ver "+result+"' anymore - we detected it correctly.");
                }
            }
        }
        
        if(result == null) {
            System.err.println("Error: You'll need to use the option  \"--"+whichVer+"ver\"  because we could not detect an ICU version in " + UVERSION );
            throw new InternalError("Error: You'll need to use the option  \"--"+whichVer+"ver\"  because we could not detect an ICU version in " + UVERSION );
        }
        
        
        
        return result;
    }
    
    private static void printUsage(){
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

    static String getAttr(Node node, String attrName){
        return node.getAttributes().getNamedItem(attrName).getNodeValue();
    }
    
    static String getAttr(NamedNodeMap attrList, String attrName){
        return attrList.getNamedItem(attrName).getNodeValue();
    }
    
    static class Function {
        public String prototype;
        public String id;
        public String status;
        public String version;
        public String file;
        public boolean equals(Function right){
            return this.prototype.equals(right.prototype);
        }
        static Function fromXml(Node n){
            Function f = new Function();
            f.prototype = getAttr(n, "prototype");
            f.id = getAttr(n, "id");
            f.status = getAttr(n, "status");
            f.version = trimICU(getAttr(n, "version"));
            f.file = getAttr(n, "file");
            f.purifyPrototype();
            f.file = Function.getBasename(f.file);
            return f;
        }
        
        /**
         * Convert string to basename.
         * @param str
         * @return
         */
        private static String getBasename(String str){
            int i = str.lastIndexOf("/");
            str = i == -1 ? str : str.substring(i+1);
            return str;
        }
        
        /**
         * Special cases:
         * 
         * Remove the status attribute embedded in the C prototype
         * 
         * Remove the virtual keyword in Cpp prototype
         */
        private void purifyPrototype(){
            //refer to 'umachine.h'
            String statusList[] = {"U_CAPI", "U_STABLE", "U_DRAFT", "U_DEPRECATED", "U_OBSOLETE", "U_INTERNAL", "virtual"};
            for (int i = 0; i < statusList.length; i++) {
                String s = statusList[i];
                prototype = prototype.replaceAll(s,"");
                prototype = prototype.trim();
            }
            prototype = prototype.trim();
        }
//        private Element toXml(Document doc){
//            Element  ele = doc.createElement("func");
//            ele.setAttribute("prototype", prototype);
//            ele.setAttribute("id", id);
//            ele.setAttribute("status", status);
//            return ele;
//        }
    }
    
    static class JoinedFunction {
        public String prototype;
        public String leftRefId;
        public String leftStatus;
        public String leftVersion;
        public String rightVersion;
        public String leftFile;
        public String rightRefId;
        public String rightStatus;
        public String rightFile;
        
        static JoinedFunction fromLeftFun(Function left){
            JoinedFunction u = new JoinedFunction();
            u.prototype = left.prototype;
            u.leftRefId = left.id;
            u.leftStatus = left.status;
            u.leftFile = left.file;
            u.rightRefId = nul;
           // u.rightVersion = nul;
            u.leftVersion = left.version;
            u.rightStatus = nul;
            u.rightFile = nul;
            return u;
        }
    
        static JoinedFunction fromRightFun(Function right){
            JoinedFunction u = new JoinedFunction();
            u.prototype = right.prototype;
            u.leftRefId = nul;
            u.leftStatus = nul;
            u.leftFile = nul;
           // u.leftVersion = nul;
            u.rightVersion = right.version;
            u.rightRefId = right.id;
            u.rightStatus = right.status;
            u.rightFile = right.file;
            return u;
        }
        
        static JoinedFunction fromTwoFun(Function left, Function right){
            if (!left.equals(right)) throw new Error();
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
            return u;
        }

        Element toXml(Document doc){
            Element  ele = doc.createElement("func");
            ele.setAttribute("prototype", prototype);
//            ele.setAttribute("leftRefId", leftRefId);
            
            ele.setAttribute("leftStatus", leftStatus);
//            ele.setAttribute("rightRefId", rightRefId);
            ele.setAttribute("rightStatus", rightStatus);
            ele.setAttribute("leftVersion", leftVersion);
//            ele.setAttribute("rightRefId", rightRefId);
            ele.setAttribute("rightVersion", rightVersion);
            
            
//            String f = rightRefId.equals(nul) ? leftRefId : rightRefId;
//            int tail = f.indexOf("_");
//            f = tail != -1 ? f.substring(0, tail) : f;
//            f = f.startsWith("class") ? f.replaceFirst("class","") : f;
            String f = rightFile.equals(nul) ? leftFile : rightFile;
            ele.setAttribute("file", f);
            return ele;
        }
    }

    TransformerFactory transFac = TransformerFactory.newInstance();

    private void reportSelectedFun(Node joinedNode) throws TransformerException, ParserConfigurationException, SAXException, IOException{
        Transformer report = transFac.newTransformer(new javax.xml.transform.stream.StreamSource(reportXsl));
//        report.setParameter("leftStatus", leftStatus);
        report.setParameter("leftVer", leftVer);
//        report.setParameter("rightStatus", rightStatus);
        report.setParameter("ourYear", new Integer(new java.util.GregorianCalendar().get(java.util.Calendar.YEAR)));
        report.setParameter("rightVer", rightVer);
        report.setParameter("dateTime", new GregorianCalendar().getTime());
        report.setParameter("nul", nul);
        
        DOMSource src = new DOMSource(joinedNode);

        Result res = new StreamResult(resultFile);
//        DOMResult res = new DOMResult();
        report.transform(src, res);
//        dumpNode(res.getNode(),"");
    }
    
    private Set getFullList(File dumpXsltFile) throws TransformerException, ParserConfigurationException, XPathExpressionException, SAXException, IOException{
        // prepare transformer
        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = "/list";
        Transformer transformer = transFac.newTransformer(new javax.xml.transform.stream.StreamSource(dumpXsltFile));

        //        InputSource leftSource = new InputSource(leftDir + "index.xml");
        DOMSource leftIndex = new DOMSource(getDocument(new File(leftDir,INDEX_XML)));
        DOMResult leftResult = new DOMResult();
        transformer.setParameter(DOC_FOLDER, leftDir);
        transformer.transform(leftIndex, leftResult);

//        Node leftList = XPathAPI.selectSingleNode(leftResult.getNode(),"/list");
        Node leftList = (Node)xpath.evaluate(expression, leftResult.getNode(), XPathConstants.NODE);
        if(leftList==null) {
           	//dumpNode(xsltSource.getNode());
          	dumpNode(leftResult.getNode());
//        	dumpNode(leftIndex.getNode());
        	System.out.flush();
        	System.err.flush();
        	throw new InternalError("getFullList("+dumpXsltFile.getName()+") returned a null left "+expression);
        }
        
        xpath.reset(); // reuse
        
        DOMSource rightIndex = new DOMSource(getDocument(new File(rightDir,INDEX_XML)));
        DOMResult rightResult = new DOMResult();
        transformer.setParameter(DOC_FOLDER, rightDir);
        transformer.transform(rightIndex, rightResult);
        Node rightList = (Node)xpath.evaluate(expression, rightResult.getNode(), XPathConstants.NODE);
        if(rightList==null) {
        	throw new InternalError("getFullList("+dumpXsltFile.getName()+") returned a null right "+expression);
        }
//        dumpNode(rightList,"");
        
        
        Set leftSet = nodeToSet(leftList);
        Set rightSet = nodeToSet(rightList);
        Set joined = fullJoin(leftSet, rightSet);
        return joined;
//        joinedNode = setToNode(joined);
//        dumpNode(joinedNode,"");
//        return joinedNode;
    }

    /**
     * @param node
     * @return      Set<Fun>
     */
    private Set nodeToSet(Node node){
        Set s = new HashSet();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            s.add(Function.fromXml(n));
        }
        return s;
    }

    /**
     * @param set       Set<JoinedFun>
     * @return
     * @throws ParserConfigurationException
     */
    private Node setToNode(Set set) throws ParserConfigurationException{
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc  = dbf.newDocumentBuilder().newDocument();
        Element root = doc.createElement("list");
        doc.appendChild(root);
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            JoinedFunction fun = (JoinedFunction) iter.next();
            root.appendChild(fun.toXml(doc));
        }
        return doc;
    }

    /**
     * full-join two Set on 'prototype' 
     * 
     * @param left     Set<Fun>
     * @param right    Set<Fun>
     * @return          Set<JoinedFun>
     */
    private static Set fullJoin(Set left, Set right){

        Set joined = new HashSet(); //Set<JoinedFun>
        Set common = new HashSet(); //Set<Fun>
        for (Iterator iter1 = left.iterator(); iter1.hasNext();) {
            Function f1 = (Function) iter1.next();
//            if (f1.prototype.matches(".*Transliterator::.*")){
//                System.err.println("left: " + f1.prototype);
//                System.err.println("left: " + f1.status);
//            }
            for (Iterator iter2 = right.iterator(); iter2.hasNext();) {
                Function f2 = (Function) iter2.next();
//                if ( f1.prototype.matches(".*filteredTransliterate.*")
//                  && f2.prototype.matches(".*filteredTransliterate.*")){
//                    System.err.println("right: " + f2.prototype);
//                    System.err.println("right: " + f2.status);
//                    System.err.println(f1.prototype.equals(f2.prototype));
//                    System.err.println(f1.prototype.getBytes()[0]);
//                    System.err.println(f2.prototype.getBytes()[0]);
//                }
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

        for (Iterator iter = common.iterator(); iter.hasNext();) {
            Function f = (Function) iter.next();
            left.remove(f);
        }
        
        for (Iterator iter = left.iterator(); iter.hasNext();) {
            Function f = (Function) iter.next();
            joined.add(JoinedFunction.fromLeftFun(f));
        }
        
        for (Iterator iter = right.iterator(); iter.hasNext();) {
            Function f = (Function) iter.next();
            joined.add(JoinedFunction.fromRightFun(f));
        }
        return joined;
    }
    
    private static void dumpNode(Node n) {
    	dumpNode(n,"");
    }
    /**
     * Dump out a node for debugging. Recursive fcn
     * @param n
     * @param pre
     */
    private static void dumpNode(Node n, String pre) {
    	String opre = pre;
		pre += " ";
		System.out.print(opre + "<" + n.getNodeName() );
		// dump attribute
		NamedNodeMap attr = n.getAttributes();
		if (attr != null) {
			for (int i = 0; i < attr.getLength(); i++) {
				System.out.print("\n"+pre+"   "+attr.item(i).getNodeName()+"=\"" + attr.item(i).getNodeValue()+"\"");
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
    private static DocumentBuilderFactory dbf  = null;
    private synchronized static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    	if(theBuilder == null) {
            dbf = DocumentBuilderFactory.newInstance();
            theBuilder = dbf.newDocumentBuilder();
    	}
    	return theBuilder;
    }
    
    private static Document getDocument(File file) throws ParserConfigurationException, SAXException, IOException{
        FileInputStream fis = new FileInputStream(file);
        InputSource inputSource = new InputSource(fis);
        Document doc = getDocumentBuilder().parse(inputSource);
        return doc;
    }

}
