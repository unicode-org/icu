/**
 *******************************************************************************
 * Copyright (C) 2004-2008, International Business Machines Corporation and    *
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
 * Sample compilation:
 * c:/doug/java/jdk1.4.2/build/windows-i586/bin/javac *.java
 *
 * Sample execution
 * c:/j2sdk1.4.2/bin/javadoc
 *   -classpath c:/jd2sk1.4.2/lib/tools.jar 
 *   -doclet com.ibm.icu.dev.tool.docs.GatherAPIData
 *   -docletpath c:/doug/cvsproj/icu4j/src 
 *   -sourcepath c:/doug/cvsproj/icu4j/src 
 *   -name "ICU4J 3.0"
 *   -output icu4j30.api
 *   -gzip
 *   -source 1.4
 *   com.ibm.icu.lang com.ibm.icu.math com.ibm.icu.text com.ibm.icu.util
 *
 * todo: provide command-line control of filters of which subclasses/packages to process
 * todo: record full inheritance heirarchy, not just immediate inheritance 
 * todo: allow for aliasing comparisons (force (pkg.)*class to be treated as though it 
 *       were in a different pkg/class heirarchy (facilitates comparison of icu4j and java)
 */

package com.ibm.icu.dev.tool.docs;

// standard release sdk won't work, need internal build to get access to javadoc
import com.sun.javadoc.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GatherAPIData {
    RootDoc root;
    TreeSet results;
    String srcName = "Current"; // default source name
    String output; // name of output file to write
    String base; // strip this prefix
    Pattern pat;
    boolean zip;
    boolean gzip;
    boolean internal;
    boolean version;

    public static int optionLength(String option) {
        if (option.equals("-name")) {
            return 2;
        } else if (option.equals("-output")) {
            return 2;
        } else if (option.equals("-base")) {
            return 2;
        } else if (option.equals("-filter")) {
            return 2;
        } else if (option.equals("-zip")) {
            return 1;
        } else if (option.equals("-gzip")) {
            return 1;
        } else if (option.equals("-internal")) {
            return 1;
        } else if (option.equals("-version")) {
            return 1;
        }
        return 0;
    }

    public static boolean start(RootDoc root) {
        return new GatherAPIData(root).run();
    }

    GatherAPIData(RootDoc root) {
        this.root = root;

        String[][] options = root.options();
        for (int i = 0; i < options.length; ++i) {
            String opt = options[i][0];
            if (opt.equals("-name")) {
                this.srcName = options[i][1];
            } else if (opt.equals("-output")) {
                this.output = options[i][1];
            } else if (opt.equals("-base")) {
                this.base = options[i][1]; // should not include '.'
            } else if (opt.equals("-filter")) {
                this.pat = Pattern.compile(options[i][1], Pattern.CASE_INSENSITIVE);
            } else if (opt.equals("-zip")) {
                this.zip = true;
            } else if (opt.equals("-gzip")) {
                this.gzip = true;
            } else if (opt.equals("-internal")) {
                this.internal = true;
            } else if (opt.equals("-version")) {
                this.version = true;
            }
        }

        results = new TreeSet(APIInfo.defaultComparator());
    }

    private boolean run() {
        doDocs(root.classes());

        OutputStream os = System.out;
        if (output != null) {
            try {
                if (zip) {
                    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(output + ".zip"));
                    zos.putNextEntry(new ZipEntry(output));
                    os = zos;
                } else if (gzip) {
                    os = new GZIPOutputStream(new FileOutputStream(output + ".gz"));
                } else {
                    os = new FileOutputStream(output);
                }
            }
            catch (IOException e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.initCause(e);
                throw re;
            }
        }

        BufferedWriter bw = null;
        try {
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            bw = new BufferedWriter(osw);

            // writing data file
            bw.write(String.valueOf(APIInfo.VERSION) + APIInfo.SEP); // header version
            bw.write(srcName + APIInfo.SEP); // source name
            bw.write((base == null ? "" : base) + APIInfo.SEP); // base
            bw.newLine();
            writeResults(results, bw);
            bw.close(); // should flush, close all, etc
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

        APIInfo info = createInfo(doc);
        if (info != null) {
            results.add(info);
        }
    }

    private boolean ignore(ProgramElementDoc doc) {
        if (doc == null) return true;
        if (doc.isPrivate() || doc.isPackagePrivate()) return true;
        if (doc instanceof ConstructorDoc && ((ConstructorDoc)doc).isSynthetic()) return true;
        if (doc.qualifiedName().indexOf(".misc") != -1) { 
            System.out.println("misc: " + doc.qualifiedName()); return true; 
        }
        if (!internal) { // debug
            Tag[] tags = doc.tags();
            for (int i = 0; i < tags.length; ++i) {
                if (tagKindIndex(tags[i].kind()) == INTERNAL) { return true; }
            }
        }
        if (pat != null && (doc.isClass() || doc.isInterface())) {
            if (!pat.matcher(doc.name()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static void writeResults(Collection c, BufferedWriter w) {
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            APIInfo info = (APIInfo)iter.next();
            info.writeln(w);
        }
    }

    private String trimBase(String arg) {
        if (base != null) {
            for (int n = arg.indexOf(base); n != -1; n = arg.indexOf(base, n)) {
                arg = arg.substring(0, n) + arg.substring(n+base.length());
            }
        }
        return arg;
    }

    public APIInfo createInfo(ProgramElementDoc doc) {

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

        APIInfo info = new APIInfo();
        if (version) {
            info.includeStatusVersion(true);
        }
            
        // status
        String[] version = new String[1];
        info.setType(APIInfo.STA, tagStatus(doc, version));
        info.setStatusVersion(version[0]);

        // visibility
        if (doc.isPublic()) {
            info.setPublic();
        } else if (doc.isProtected()) {
            info.setProtected();
        } else if (doc.isPrivate()) {
            info.setPrivate();
        } else {
            // default is package
        }

        // static
        if (doc.isStatic()) {
            info.setStatic();
        } else {
            // default is non-static
        }

        // final
        if (doc.isFinal()) {
            info.setFinal();
        } else {
            // default is non-final
        }

        // type
        if (doc.isField()) {
            info.setField();
        } else if (doc.isMethod()) {
            info.setMethod();
        } else if (doc.isConstructor()) {
            info.setConstructor();
        } else if (doc.isClass() || doc.isInterface()) {
            info.setClass();
        }

        info.setPackage(trimBase(doc.containingPackage().name()));
        info.setClassName((doc.isClass() || doc.isInterface() || (doc.containingClass() == null)) 
                          ? "" 
                          : trimBase(doc.containingClass().name()));
        info.setName(trimBase(doc.name()));

        if (doc instanceof FieldDoc) {
            FieldDoc fdoc = (FieldDoc)doc;
            info.setSignature(trimBase(fdoc.type().toString()));
        } else if (doc instanceof ClassDoc) {
            ClassDoc cdoc = (ClassDoc)doc;

            if (cdoc.isClass() && cdoc.isAbstract()) { 
                // interfaces are abstract by default, don't mark them as abstract
                info.setAbstract();
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
            info.setSignature(trimBase(buf.toString()));
        } else {
            ExecutableMemberDoc emdoc = (ExecutableMemberDoc)doc;
            if (emdoc.isSynchronized()) {
                info.setSynchronized();
            }

            if (doc instanceof MethodDoc) {
                MethodDoc mdoc = (MethodDoc)doc;
                if (mdoc.isAbstract()) {
                    info.setAbstract();
                }
                info.setSignature(trimBase(mdoc.returnType().toString() + emdoc.signature()));
            } else {
                // constructor
                info.setSignature(trimBase(emdoc.signature()));
            }
        }

        return info;
    }

    private int tagStatus(final Doc doc, String[] version) {
        class Result {
            int res = -1;
            void set(int val) { 
                if (res != -1) {
                    if (val == APIInfo.STA_DEPRECATED) {
                        // ok to have both a 'standard' tag and deprecated
                        return;
                    } else if (res != APIInfo.STA_DEPRECATED) {
                        // if already not deprecated, this is an error
                        System.err.println("bad doc: " + doc + " both: " + APIInfo.getTypeValName(APIInfo.STA, res) + " and: " + APIInfo.getTypeValName(APIInfo.STA, val)); 
                        return;
                    }
                }
                // ok to replace with new tag
                res = val;
            }
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
        String statusVer = "";
        for (int i = 0; i < tags.length; ++i) {
            Tag tag = tags[i];

            String kind = tag.kind();
            int ix = tagKindIndex(kind);

            switch (ix) {
            case INTERNAL:
                result.set(internal ? APIInfo.STA_INTERNAL : -2); // -2 for legacy compatibility
                statusVer = getStatusVersion(tag);
                break;

            case DRAFT:
                result.set(APIInfo.STA_DRAFT);
                statusVer = getStatusVersion(tag);
                break;

            case STABLE:
                result.set(APIInfo.STA_STABLE);
                statusVer = getStatusVersion(tag);
                break;

            case DEPRECATED:
                result.set(APIInfo.STA_DEPRECATED);
                statusVer = getStatusVersion(tag);
                break;

            case OBSOLETE:
                result.set(APIInfo.STA_OBSOLETE);
                statusVer = getStatusVersion(tag);
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

        if (version != null) {
            version[0] = statusVer;
        }
        return result.get();
    }

    private String getStatusVersion(Tag tag) {
        String text = tag.text();
        if (text != null && text.length() > 0) {
            // Extract version string
            int start = -1;
            int i = 0;
            for (; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '.' || (ch >= '0' && ch <= '9')) {
                    if (start == -1) {
                        start = i;
                    }
                } else if (start != -1) {
                    break;
                }
            }
            if (start != -1) {
                return text.substring(start, i);
            }
        }
        return "";
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
