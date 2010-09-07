/**
*******************************************************************************
* Copyright (C) 2004-2010, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/**
 * Generate a list of ICU's public APIs, sorted by qualified name and signature
 * public APIs are all non-internal, non-package apis in com.ibm.icu.[lang|math|text|util].
 * For each API, list
 * - public, package, protected, or private (PB PK PT PR)
 * - static or non-static (STK NST)
 * - final or non-final (FN NF)
 * - synchronized or non-synchronized (SYN NSY)
 * - stable, draft, deprecated, obsolete (ST DR DP OB)
 * - abstract or non-abstract (AB NA)
 * - constructor, member, field (C M F)
 *
 * Requires JDK 1.4.2 or later
 * 
 * Sample invocation:
 * c:/j2sdk1.4.2/bin/javadoc 
 *   -classpath c:/jd2sk1.4.2/lib/tools.jar 
 *   -doclet com.ibm.icu.dev.tool.docs.CheckAPI 
 *   -docletpath c:/doug/cvsproj/icu4j/src 
 *   -sourcepath c:/eclipse2.1/workspace2/icu4j/src 
 *   -compare c:/doug/cvsproj/icu4j/src/com/ibm/icu/dev/tool/docs/api2_6_1.txt 
 *   -output foo 
 *   com.ibm.icu.text
 *
 * todo: separate generation of data files (which requires taglet) from 
 * comparison and report generation (which does not require it)
 * todo: provide command-line control of filters of which subclasses/packages to process
 * todo: record full inheritance heirarchy, not just immediate inheritance 
 * todo: allow for aliasing comparisons (force (pkg.)*class to be treated as though it 
 * were in a different pkg/class heirarchy (facilitates comparison of icu4j and java)
 */

package com.ibm.icu.dev.tool.docs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

public class CheckAPI {
    RootDoc root;
    String compare; // file
    String compareName;
    TreeSet compareSet;
    TreeSet results;
    boolean html;
    String srcName = "Current"; // default source name
    String output;
    
    private static final int DATA_FILE_VERSION = 1;
    private static final char SEP = ';';

    private static final int STA = 0, STA_DRAFT = 0, STA_STABLE = 1, STA_DEPRECATED = 2, STA_OBSOLETE = 3;
    private static final int VIS = 1, VIS_PACKAGE = 0, VIS_PUBLIC= 1, VIS_PROTECTED = 2, VIS_PRIVATE = 3;
    private static final int STK = 2, STK_STATIC = 1;
    private static final int FIN = 3, FIN_FINAL = 1;
    private static final int SYN = 4, SYN_SYNCHRONIZED = 1;
    private static final int ABS = 5, ABS_ABSTRACT = 1;
    private static final int CAT = 6, CAT_CLASS = 0, CAT_FIELD = 1, CAT_CONSTRUCTOR = 2, CAT_METHOD = 3;
    private static final int PAK = 7;
    private static final int CLS = 8;
    private static final int NAM = 9;
    private static final int SIG = 10;
    private static final int EXC = 11;
    private static final int NUM_TYPES = 11;

    static abstract class APIInfo {
        public abstract int getVal(int typ);
        public abstract String get(int typ, boolean brief);
        public abstract void write(BufferedWriter w, boolean brief, boolean html, boolean detail);
    }
        
    final static class Info extends APIInfo {
        private int    info;
        private String pack; // package
        private String cls; // enclosing class
        private String name; // name
        private String sig;  // signature, class: inheritance, method: signature, field: type, const: signature
        private String exc;  // throws 
        
        public int getVal(int typ) {
            validateType(typ);
            return (info >> (typ*2)) & 0x3;
        }

        public String get(int typ, boolean brief) {
            validateType(typ);
            String[] vals = brief ? shortNames[typ] : names[typ];
            if (vals == null) {
                switch (typ) {
                case PAK: return pack;
                case CLS: return cls;
                case NAM: return name;
                case SIG: return sig;
                case EXC: return exc;
                }
            }
            int val = (info >> (typ*2)) & 0x3;
            return vals[val];
        }

        private void setType(int typ, int val) {
            validateType(typ);
            info &= ~(0x3 << (typ*2));
            info |= (val&0x3) << (typ * 2);
        }

        private void setType(int typ, String val) {
            validateType(typ);
            String[] vals = shortNames[typ];
            if (vals == null) {
                switch (typ) {
                case PAK: pack = val; break;
                case CLS: cls = val; break;
                case NAM: name = val; break;
                case SIG: sig = val; break;
                case EXC: exc = val; break;
                }
                return;
            }

            for (int i = 0; i < vals.length; ++i) {
                if (val.equalsIgnoreCase(vals[i])) {
                    info &= ~(0x3 << (typ*2));
                    info |= i << (typ*2);
                    return;
                }
            }

            throw new IllegalArgumentException("unrecognized value '" + val + "' for type '" + typeNames[typ] + "'");
        }

        public void write(BufferedWriter w, boolean brief, boolean html, boolean detail) {
            try {
                if (brief) {
                    for (int i = 0; i < NUM_TYPES; ++i) {
                        String s = get(i, true);
                        if (s != null) {
                            w.write(s);
                        }
                        w.write(SEP);
                    }
                } else {
                    // remove all occurrences of icu packages from the param string
                    // fortunately, all the packages have 4 chars (lang, math, text, util).
                    String xsig = sig;
                    if (!detail) {
                        final String ICUPACK = "com.ibm.icu.";
                        StringBuffer buf = new StringBuffer();
                        for (int i = 0; i < sig.length();) {
                            int n = sig.indexOf(ICUPACK, i);
                            if (n == -1) {
                                buf.append(sig.substring(i));
                                break;
                            }
                            buf.append(sig.substring(i, n));
                            i = n + ICUPACK.length() + 5; // trailing 'xxxx.'
                        }
                        xsig = buf.toString();
                    }

                    // construct signature
                    for (int i = STA; i < CAT; ++i) { // include status
                        String s = get(i, false);
                        if (s != null && s.length() > 0) {
                            if (i == STA) {
                                w.write('(');
                                w.write(s);
                                w.write(')');
                            } else {
                                w.write(s);
                            }
                            w.write(' ');
                        }
                    }

                    int val = getVal(CAT);
                    switch (val) {
                    case CAT_CLASS:
                        if (sig.indexOf("extends") == -1) {
                            w.write("interface ");
                        } else {
                            w.write("class ");
                        }
                        if (cls.length() > 0) {
                            w.write(cls);
                            w.write('.');
                        }
                        w.write(name);
                        if (detail) {
                            w.write(' ');
                            w.write(sig);
                        }
                        break;

                    case CAT_FIELD:
                        w.write(xsig);
                        w.write(' ');
                        w.write(name);
                        break;

                    case CAT_METHOD:
                    case CAT_CONSTRUCTOR:
                        int n = xsig.indexOf('(');
                        if (n > 0) {
                            w.write(xsig.substring(0, n));
                            w.write(' ');
                        } else {
                            n = 0;
                        }
                        w.write(name);
                        w.write(xsig.substring(n));
                        break;
                    }
                }
                w.newLine();
            }
            catch (IOException e) {
                RuntimeException re = new RuntimeException("IO Error");
                re.initCause(e);
                throw re;
            }
        }

        public boolean read(BufferedReader r) {
            int i = 0;
            try {
                for (; i < NUM_TYPES; ++i) {
                    setType(i, readToken(r));
                }
                r.readLine(); // swallow line end sequence
            }
            catch (IOException e) {
                if (i == 0) { // assume if first read returns error, we have reached end of input
                    return false;
                }
                RuntimeException re = new RuntimeException("IO Error");
                re.initCause(e);
                throw re;
            }

            return true;
        }

        public boolean read(ProgramElementDoc doc) {

            // Doc. name
            // Doc. isField, isMethod, isConstructor, isClass, isInterface
            // ProgramElementDoc. containingClass, containingPackage
            // ProgramElementDoc. isPublic, isProtected, isPrivate, isPackagePrivate
            // ProgramElementDoc. isStatic, isFinal
            // MemberDoc.isSynthetic
            // ExecutableMemberDoc isSynchronized, signature
            // Type.toString() // e.g. "String[][]"
            // ClassDoc.isAbstract, superClass, interfaces, fields, methods, constructors, innerClasses
            // FieldDoc type
            // ConstructorDoc qualifiedName
            // MethodDoc isAbstract, returnType

            
            // status
            setType(STA, tagStatus(doc));

            // visibility
            if (doc.isPublic()) {
                setType(VIS, VIS_PUBLIC);
            } else if (doc.isProtected()) {
                setType(VIS, VIS_PROTECTED);
            } else if (doc.isPrivate()) {
                setType(VIS, VIS_PRIVATE);
            } else {
                // default is package
            }

            // static
            if (doc.isStatic()) {
                setType(STK, STK_STATIC);
            } else {
                // default is non-static
            }

            // final
            if (doc.isFinal()) {
                setType(FIN, FIN_FINAL);
            } else {
                // default is non-final
            }

            // type
            if (doc.isField()) {
                setType(CAT, CAT_FIELD);
            } else if (doc.isMethod()) {
                setType(CAT, CAT_METHOD);
            } else if (doc.isConstructor()) {
                setType(CAT, CAT_CONSTRUCTOR);
            } else if (doc.isClass() || doc.isInterface()) {
                setType(CAT, CAT_CLASS);
            }

            setType(PAK, doc.containingPackage().name());
            setType(CLS, (doc.isClass() || doc.isInterface() || (doc.containingClass() == null)) ? "" : doc.containingClass().name());
            setType(NAM, doc.name());

            if (doc instanceof FieldDoc) {
                FieldDoc fdoc = (FieldDoc)doc;
                setType(SIG, fdoc.type().toString());
            } else if (doc instanceof ClassDoc) {
                ClassDoc cdoc = (ClassDoc)doc;

                if (cdoc.isClass() && cdoc.isAbstract()) { // interfaces are abstract by default, don't mark them as abstract
                    setType(ABS, ABS_ABSTRACT);
                }

                StringBuffer buf = new StringBuffer();
                if (cdoc.isClass()) {
                    buf.append("extends ");
                    buf.append(cdoc.superclass().qualifiedName());
                }
                ClassDoc[] imp = cdoc.interfaces();
                if (imp != null && imp.length > 0) {
                    if (buf.length() > 0) {
                        buf.append(" ");
                    }
                    buf.append("implements");
                    for (int i = 0; i < imp.length; ++i) {
                        if (i != 0) {
                            buf.append(",");
                        }
                        buf.append(" ");
                        buf.append(imp[i].qualifiedName());
                    }
                }
                setType(SIG, buf.toString());
            } else {
                ExecutableMemberDoc emdoc = (ExecutableMemberDoc)doc;
                if (emdoc.isSynchronized()) {
                    setType(SYN, SYN_SYNCHRONIZED);
                }

                if (doc instanceof MethodDoc) {
                    MethodDoc mdoc = (MethodDoc)doc;
                    if (mdoc.isAbstract()) {
                        setType(ABS, ABS_ABSTRACT);
                    }
                    setType(SIG, mdoc.returnType().toString() + emdoc.signature());
                } else {
                    // constructor
                    setType(SIG, emdoc.signature());
                }
            }

            return true;
        }

        public static Comparator defaultComparator() {
            final Comparator c = new Comparator() {
                    public int compare(Object lhs, Object rhs) {
                        Info lhi = (Info)lhs;
                        Info rhi = (Info)rhs;
                        int result = lhi.pack.compareTo(rhi.pack);
                        if (result == 0) {
                            result = (lhi.getVal(CAT) == CAT_CLASS ? lhi.name : lhi.cls)
                                .compareTo(rhi.getVal(CAT) == CAT_CLASS ? rhi.name : rhi.cls);
                            if (result == 0) {
                                result = lhi.getVal(CAT)- rhi.getVal(CAT);
                                if (result == 0) {
                                    result = lhi.name.compareTo(rhi.name);
                                    if (result == 0) {
                                        result = lhi.sig.compareTo(rhi.sig);
                                    }
                                }
                            }
                        }
                        return result;
                    }
                };
            return c;
        }

        public static Comparator changedComparator() {
            final Comparator c = new Comparator() {
                    public int compare(Object lhs, Object rhs) {
                        Info lhi = (Info)lhs;
                        Info rhi = (Info)rhs;
                        int result = lhi.pack.compareTo(rhi.pack);
                        if (result == 0) {
                            result = (lhi.getVal(CAT) == CAT_CLASS ? lhi.name : lhi.cls)
                                .compareTo(rhi.getVal(CAT) == CAT_CLASS ? rhi.name : rhi.cls);
                            if (result == 0) {
                                result = lhi.getVal(CAT)- rhi.getVal(CAT);
                                if (result == 0) {
                                    result = lhi.name.compareTo(rhi.name);
                                    if (result == 0 && lhi.getVal(CAT) != CAT_CLASS) {
                                        result = lhi.sig.compareTo(rhi.sig);
                                    }
                                }
                            }
                        }
                        return result;
                    }
                };
            return c;
        }

        public static Comparator classFirstComparator() {
            final Comparator c = new Comparator() {
                    public int compare(Object lhs, Object rhs) {
                        Info lhi = (Info)lhs;
                        Info rhi = (Info)rhs;
                        int result = lhi.pack.compareTo(rhi.pack);
                        if (result == 0) {
                            boolean lcls = lhi.getVal(CAT) == CAT_CLASS;
                            boolean rcls = rhi.getVal(CAT) == CAT_CLASS;
                            result = lcls == rcls ? 0 : (lcls ? -1 : 1);
                            if (result == 0) {
                                result = (lcls ? lhi.name : lhi.cls).compareTo(rcls ? rhi.name : rhi.cls);
                                if (result == 0) {
                                    result = lhi.getVal(CAT)- rhi.getVal(CAT);
                                    if (result == 0) {
                                        result = lhi.name.compareTo(rhi.name);
                                        if (result == 0 && !lcls) {
                                            result = lhi.sig.compareTo(rhi.sig);
                                        }
                                    }
                                }
                            }
                        }
                        return result;
                    }
                };
            return c;
        }

        private static final String[] typeNames = {
            "status", "visibility", "static", "final", "synchronized", 
            "abstract", "category", "package", "class", "name", "signature"
        };

        private static final String[][] names = {
            { "draft     ", "stable    ", "deprecated", "obsolete  " },
            { "package", "public", "protected", "private" },
            { "", "static" },
            { "", "final" },
            { "", "synchronized" },
            { "", "abstract" },
            { "class", "field", "constructor", "method"  },
            null,
            null,
            null,
            null,
            null
        };

        private static final String[][] shortNames = {
            { "DR", "ST", "DP", "OB" },
            { "PK", "PB", "PT", "PR" },
            { "NS", "ST" },
            { "NF", "FN" },
            { "NS", "SY" },
            { "NA", "AB" },
            { "L", "F", "C", "M" },
            null,
            null,
            null,
            null,
            null
        };

        private static void validateType(int typ) {
            if (typ < 0 || typ > NUM_TYPES) {
                throw new IllegalArgumentException("bad type index: " + typ);
            }
        }

        public String toString() {
            return get(NAM, true);
        }
    }

    static final class DeltaInfo extends APIInfo {
        private Info a;
        private Info b;

        DeltaInfo(Info a, Info b) {
            this.a = a;
            this.b = b;
        }

        public int getVal(int typ) {
            return a.getVal(typ);
        }

        public String get(int typ, boolean brief) {
            return a.get(typ, brief);
        }

        public void write(BufferedWriter w, boolean brief, boolean html, boolean detail) {
            a.write(w, brief, html, detail);
            try {
                if (html) {
                    w.write("<br>");
                }
                w.newLine();
            } 
            catch (Exception e) {
            }
            b.write(w, brief, html, detail);
        }

        public String toString() {
            return a.get(NAM, true);
        }
    }

    public static int optionLength(String option) {
        if (option.equals("-html")) {
            return 1;
        } else if (option.equals("-name")) {
            return 2;
        } else if (option.equals("-output")) {
            return 2;
        } else if (option.equals("-compare")) {
            return 2;
        }
        return 0;
    }

    public static boolean start(RootDoc root) {
        return new CheckAPI(root).run();
    }

    CheckAPI(RootDoc root) {
        this.root = root;

        //      this.compare = "c:/doug/cvsproj/icu4j/src/com/ibm/icu/dev/tool/docs/api2_8.txt";

        String[][] options = root.options();
        for (int i = 0; i < options.length; ++i) {
            String opt = options[i][0];
            if (opt.equals("-html")) {
                this.html = true;
            } else if (opt.equals("-name")) {
                this.srcName = options[i][1];
            } else if (opt.equals("-output")) {
                this.output = options[i][1];
            } else if (opt.equals("-compare")) {
                this.compare = options[i][1];
            }
        }

        if (compare != null) {
            try {
                // URL url = new URL(compare);
                File f = new File(compare);
                InputStream is = new FileInputStream(f);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                // read header line
                /*int version = */Integer.parseInt(readToken(br));
                // check version if we change it later, probably can just rebuild though
                this.compareName = readToken(br);
                br.readLine();

                // read data
                this.compareSet = new TreeSet(Info.defaultComparator());
                for (Info info = new Info(); info.read(br); info = new Info()) {
                    compareSet.add(info);
                }
            }
            catch (Exception e) {
                RuntimeException re = new RuntimeException("error reading " + compare);
                re.initCause(e);
                throw re;
            }
        }
            
        results = new TreeSet(Info.defaultComparator());
    }

    private boolean run() {
        doDocs(root.classes());

        OutputStream os = System.out;
        if (output != null) {
            try {
                os = new FileOutputStream(output);
            }
            catch (FileNotFoundException e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.initCause(e);
                throw re;
            }
        }

        BufferedWriter bw = null;
        try {
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            bw = new BufferedWriter(osw);

            if (compareSet == null) {
                // writing data file
                bw.write(String.valueOf(DATA_FILE_VERSION) + SEP); // header version
                bw.write(srcName + SEP); // source name
                bw.newLine();
                writeResults(results, bw, true, false, false);
            } else {
                // writing comparison info
                TreeSet removed = (TreeSet)compareSet.clone();
                removed.removeAll(results);

                TreeSet added = (TreeSet)results.clone();
                added.removeAll(compareSet);

                Iterator ai = added.iterator();
                Iterator ri = removed.iterator();
                ArrayList changed = new ArrayList();
                Comparator c = Info.changedComparator();
                Info a = null, r = null;
                while (ai.hasNext() && ri.hasNext()) {
                    if (a == null) a = (Info)ai.next();
                    if (r == null) r = (Info)ri.next();
                    int result = c.compare(a, r);
                    if (result < 0) {
                        a = null;
                    } else if (result > 0) {
                        r = null;
                    } else {
                        changed.add(new DeltaInfo(a, r));
                        a = null; ai.remove();
                        r = null; ri.remove();
                    }
                }

                added = stripAndResort(added);
                removed = stripAndResort(removed);

                if (html) {
                    String title = "ICU4J API Comparison: " + srcName + " with " + compareName;

                    bw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
                    bw.newLine();
                    bw.write("<html>");
                    bw.newLine();
                    bw.write("<head>");
                    bw.newLine();
                    bw.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
                    bw.newLine();
                    bw.write("<title>");
                    bw.write(title);
                    bw.write("</title>");
                    bw.newLine();
                    bw.write("<body>");
                    bw.newLine();

                    bw.write("<h1>");
                    bw.write(title);
                    bw.write("</h1>");
                    bw.newLine();

                    bw.write("<hr/>");
                    bw.newLine();
                    bw.write("<h2>");
                    bw.write("Removed from " + compareName);
                    bw.write("</h2>");
                    bw.newLine();

                    if (removed.size() > 0) {
                        writeResults(removed, bw, false, true, false);
                    } else {
                        bw.write("<p>(no API removed)</p>");
                    }
                    bw.newLine();

                    bw.write("<hr/>");
                    bw.newLine();
                    bw.write("<h2>");
                    bw.write("Changed in " + srcName);
                    bw.write("</h2>");
                    bw.newLine();

                    if (changed.size() > 0) {
                        writeResults(changed, bw, false, true, true);
                    } else {
                        bw.write("<p>(no API changed)</p>");
                    }
                    bw.newLine();

                    bw.write("<hr/>");
                    bw.newLine();
                    bw.write("<h2>");
                    bw.write("Added in " + srcName);
                    bw.write("</h2>");
                    bw.newLine();

                    if (added.size() > 0) {
                        writeResults(added, bw, false, true, false);
                    } else {
                        bw.write("<p>(no API added)</p>");
                    }
                    bw.write("<hr/>");
                    bw.newLine();
                    bw.write("<p><i>Contents generated by CheckAPI tool.<br/>Copyright (C) 2004, International Business Machines Corporation, All Rights Reserved.</i></p>");
                    bw.newLine();
                    bw.write("</body>");
                    bw.newLine();
                    bw.write("</html>");
                    bw.newLine();
                } else {
                    bw.write("Comparing " + srcName + " with " + compareName);
                    bw.newLine();
                    bw.newLine();

                    bw.newLine();
                    bw.write("=== Removed from " + compareName + " ===");
                    bw.newLine();
                    if (removed.size() > 0) {
                        writeResults(removed, bw, false, false, false);
                    } else {
                        bw.write("(no API removed)");
                    }
                    bw.newLine();

                    bw.newLine();
                    bw.write("=== Changed in " + srcName + " ===");
                    bw.newLine();
                    if (changed.size() > 0) {
                        writeResults(changed, bw, false, false, true);
                    } else {
                        bw.write("(no API changed)");
                    }
                    bw.newLine();

                    bw.newLine();
                    bw.write("=== Added in " + srcName + " ===");
                    bw.newLine();
                    if (added.size() > 0) {
                        writeResults(added, bw, false, false, false);
                    } else {
                        bw.write("(no API added)");
                    }
                    bw.newLine();
                }
            }

            bw.close();
        } catch (IOException e) {
            try { bw.close(); } catch (IOException e2) {}
            RuntimeException re = new RuntimeException("write error: " + e.getMessage());
            re.initCause(e);
            throw re;
        }

        return false;
    }

    private void doDocs(ProgramElementDoc[] docs) {
        if (docs != null && docs.length > 0) {
            for (int i = 0; i < docs.length; ++i) {
                doDoc(docs[i]);
            }
        }
    }

    private void doDoc(ProgramElementDoc doc) {
        if (ignore(doc)) return;

        if (doc.isClass() || doc.isInterface()) {
            ClassDoc cdoc = (ClassDoc)doc;
            doDocs(cdoc.fields());
            doDocs(cdoc.constructors());
            doDocs(cdoc.methods());
            doDocs(cdoc.innerClasses());
        }

        Info info = new Info();
        if (info.read(doc)) {
            results.add(info);
        }
    }

    private boolean ignore(ProgramElementDoc doc) {
        if (doc == null) return true;
        if (doc.isPrivate() || doc.isPackagePrivate()) return true;
        if (doc instanceof ConstructorDoc && ((ConstructorDoc)doc).isSynthetic()) return true;
        if (doc.qualifiedName().indexOf(".misc") != -1) return true;
        Tag[] tags = doc.tags();
        for (int i = 0; i < tags.length; ++i) {
            if (tagKindIndex(tags[i].kind()) == INTERNAL) return true;
        }

        return false;
    }

    private static void writeResults(Collection c, BufferedWriter w, boolean brief, boolean html, boolean detail) {
        Iterator iter = c.iterator();
        String pack = null;
        String clas = null;
        while (iter.hasNext()) {
            APIInfo info = (APIInfo)iter.next();
            if (brief) {
                info.write(w, brief, false, detail);
            } else {
                try {
                    String p = info.get(PAK, true);
                    if (!p.equals(pack)) {
                        w.newLine();
                        if (html) {
                            if (clas != null) {
                                w.write("</ul>");
                                w.newLine();
                            }
                            if (pack != null) {
                                w.write("</ul>");
                                w.newLine();
                            }
                            
                            w.write("<h3>Package ");
                            w.write(p);
                            w.write("</h3>");
                            w.newLine();
                            w.write("<ul>");
                            w.newLine();
                        } else {
                            w.write("Package ");
                            w.write(p);
                            w.write(':');
                        }
                        w.newLine();
                        w.newLine();
                        
                        pack = p;
                        clas = null;
                    }

                    if (info.getVal(CAT) != CAT_CLASS) {
                        String name = info.get(CLS, true);
                        if (!name.equals(clas)) {
                            if (html) {
                                if (clas != null) {
                                    w.write("</ul>");
                                }
                                w.write("<li>");
                                w.write(name);
                                w.newLine();
                                w.write("<ul>");
                            } else {
                                w.write(name);
                                w.newLine();
                            }
                            clas = name;
                        }
                        w.write("    ");
                    }
                    if (html) {
                        w.write("<li>");
                        info.write(w, brief, html, detail);
                        w.write("</li>");
                    } else {
                        info.write(w, brief, html, detail);
                    }
                }
                catch (IOException e) {
                    System.err.println("IOException " + e.getMessage() + " writing " + info);
                }
            }
        }
        if (html) {
            try {
                if (clas != null) {
                    w.write("</ul>");
                    w.newLine();
                }
                if (pack != null) {
                    w.write("</ul>");
                    w.newLine();
                }
            } 
            catch (IOException e) {
            }
        }
    }

    private static String readToken(BufferedReader r) throws IOException {
        char[] buf = new char[256];
        int i = 0;
        for (; i < buf.length; ++i) {
            int c = r.read();
            if (c == -1) {
                throw new IOException("unexpected EOF");
            } else if (c == SEP) {
                break;
            }
            buf[i] = (char)c;
        }
        if (i == buf.length) {
            throw new IOException("unterminated token" + new String(buf));
        }
            
        return new String(buf, 0, i);
    }

    private static TreeSet stripAndResort(TreeSet t) {
        stripClassInfo(t);
        TreeSet r = new TreeSet(Info.classFirstComparator());
        r.addAll(t);
        return r;
    }

    private static void stripClassInfo(Collection c) {
        // c is sorted with class info first
        Iterator iter = c.iterator();
        String cname = null;
        while (iter.hasNext()) {
            Info info = (Info)iter.next();
            String cls = info.get(CLS, true);
            if (cname != null) {
                if (cname.equals(cls)) {
                    iter.remove();
                    continue;
                }
                cname = null;
            } 
            if (info.getVal(CAT) == CAT_CLASS) {
                cname = info.get(NAM, true);
            }
        }
    }

    private static int tagStatus(final Doc doc) {
        class Result {
            int res = -1;
            void set(int val) { if (res != -1) throw new RuntimeException("bad doc: " + doc); res = val; }
            int get() {
                if (res == -1) {
                    System.err.println("warning: no tag for " + doc);
                    return 0;
                }
                return res;
            }
        }

        Tag[] tags = doc.tags();
        Result result = new Result();
        for (int i = 0; i < tags.length; ++i) {
            Tag tag = tags[i];

            String kind = tag.kind();
            int ix = tagKindIndex(kind);

            switch (ix) {
            case INTERNAL:
                result.set(-2);
                break;

            case DRAFT:
                result.set(STA_DRAFT);
                break;

            case STABLE:
                result.set(STA_STABLE);
                break;

            case DEPRECATED:
                result.set(STA_DEPRECATED);
                break;

            case OBSOLETE:
                result.set(STA_OBSOLETE);
                break;

            case SINCE:
            case EXCEPTION:
            case VERSION:
            case UNKNOWN:
            case AUTHOR:
            case SEE:
            case PARAM:
            case RETURN:
            case THROWS:
            case SERIAL:
                break;

            default:
                throw new RuntimeException("unknown index " + ix + " for tag: " + kind);
            }
        }

        return result.get();
    }

    private static final int UNKNOWN = -1;
    private static final int INTERNAL = 0;
    private static final int DRAFT = 1;
    private static final int STABLE = 2;
    private static final int SINCE = 3;
    private static final int DEPRECATED = 4;
    private static final int AUTHOR = 5;
    private static final int SEE = 6;
    private static final int VERSION = 7;
    private static final int PARAM = 8;
    private static final int RETURN = 9;
    private static final int THROWS = 10;
    private static final int OBSOLETE = 11;
    private static final int EXCEPTION = 12;
    private static final int SERIAL = 13;

    private static int tagKindIndex(String kind) {
        final String[] tagKinds = {
            "@internal", "@draft", "@stable", "@since", "@deprecated", "@author", "@see", "@version",
            "@param", "@return", "@throws", "@obsolete", "@exception", "@serial"
        };

        for (int i = 0; i < tagKinds.length; ++i) {
            if (kind.equals(tagKinds[i])) {
                return i;
            }
        }
        return UNKNOWN;
    }
}
