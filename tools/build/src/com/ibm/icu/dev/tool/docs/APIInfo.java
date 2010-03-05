/**
 *******************************************************************************
 * Copyright (C) 2005-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * Represents the API information on a doc element.
 */

package com.ibm.icu.dev.tool.docs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;

class APIInfo {
    // version id for the format of the APIInfo data

    public static final int VERSION = 2;

    // public keys and values for queries on info

    public static final int STA = 0, STA_DRAFT = 0, STA_STABLE = 1, STA_DEPRECATED = 2, STA_OBSOLETE = 3, STA_INTERNAL = 4;
    public static final int VIS = 1, VIS_PACKAGE = 0, VIS_PUBLIC= 1, VIS_PROTECTED = 2, VIS_PRIVATE = 3;
    public static final int STK = 2, STK_STATIC = 1;
    public static final int FIN = 3, FIN_FINAL = 1;
    public static final int SYN = 4, SYN_SYNCHRONIZED = 1;
    public static final int ABS = 5, ABS_ABSTRACT = 1;
    public static final int CAT = 6, CAT_CLASS = 0, CAT_FIELD = 1, CAT_CONSTRUCTOR = 2, CAT_METHOD = 3;
    public static final int PAK = 7;
    public static final int CLS = 8;
    public static final int NAM = 9;
    public static final int SIG = 10;
    public static final int EXC = 11;
    public static final int NUM_TYPES = 11;

    // the separator between tokens in the data file
    public int[] masks = { 0x7, 0x3, 0x1, 0x1, 0x1, 0x1, 0x3 };
    public int[] shifts = { 0, 3, 5, 6, 7, 8, 9 };

    public static final char SEP = ';';

    // Internal State
    private int    info; // information about numeric values packed into an int as variable-length nibbles
    private String pack = ""; // package
    private String cls  = "";  // enclosing class
    private String name = ""; // name
    private String sig  = "";  // signature, class: inheritance, method: signature, field: type, const: signature
    private String exc  = "";  // throws
    private String stver = ""; // status version

    private boolean includeStatusVer = false;

    public int hashCode() {
        return (((pack.hashCode() << 3) ^ cls.hashCode()) << 3) ^ name.hashCode();
    }

    public boolean equals(Object rhs) {
        if (rhs == this) return true;
        if (rhs == null) return false;
        try {
            APIInfo that = (APIInfo)rhs;
            return this.info == that.info &&
                this.pack.equals(that.pack) &&
                this.cls.equals(that.cls) &&
                this.name.equals(that.name) &&
                this.sig.equals(that.sig) &&
                this.exc.equals(that.exc) &&
                this.stver.equals(this.stver);
        }
        catch (ClassCastException e) {
            return false;
        }
    }

    public void setDraft() { setType(STA, STA_DRAFT); }
    public void setStable() { setType(STA, STA_STABLE); }
    public void setDeprecated() { setType(STA, STA_DEPRECATED); }
    public void setObsolete() { setType(STA, STA_OBSOLETE); }
    public void setInternal() { setType(STA, STA_INTERNAL); }
    public void setPackage() { setType(VIS, VIS_PACKAGE); }
    public void setPublic() { setType(VIS, VIS_PUBLIC); }
    public void setProtected() { setType(VIS, VIS_PROTECTED); }
    public void setPrivate() { setType(VIS, VIS_PRIVATE); }
    public void setStatic() { setType(STK, STK_STATIC); }
    public void setFinal() { setType(FIN, FIN_FINAL); }
    public void setSynchronized() { setType(SYN, SYN_SYNCHRONIZED); }
    public void setAbstract() { setType(ABS, ABS_ABSTRACT); }
    public void setClass() { setType(CAT, CAT_CLASS); }
    public void setField() { setType(CAT, CAT_FIELD); }
    public void setConstructor() { setType(CAT, CAT_CONSTRUCTOR); }
    public void setMethod() { setType(CAT, CAT_METHOD); }

    public void setPackage(String val) { setType(PAK, val); }
    public void setClassName(String val) { setType(CLS, val); }
    public void setName(String val) { setType(NAM, val); }
    public void setSignature(String val) { setType(SIG, val); }
    public void setExceptions(String val) { setType(EXC, val); }

    public boolean isDraft() { return getVal(STA) == STA_DRAFT; }
    public boolean isStable() { return getVal(STA) == STA_STABLE; }
    public boolean isDeprecated() { return getVal(STA) == STA_DEPRECATED; }
    public boolean isObsolete() { return getVal(STA) == STA_OBSOLETE; }
    public boolean isInternal() { return getVal(STA) == STA_INTERNAL; }
    public boolean isPackage() { return getVal(VIS) == VIS_PACKAGE; }
    public boolean isPublic() { return getVal(VIS) == VIS_PUBLIC; }
    public boolean isProtected() { return getVal(VIS) == VIS_PROTECTED; }
    public boolean isPrivate() { return getVal(VIS) == VIS_PRIVATE; }
    public boolean isStatic() { return getVal(STK) == STK_STATIC; }
    public boolean isFinal() { return getVal(FIN) == FIN_FINAL; }
    public boolean isSynchronized() { return getVal(SYN) == SYN_SYNCHRONIZED; }
    public boolean isAbstract() { return getVal(ABS) == ABS_ABSTRACT; }
    public boolean isClass() { return getVal(CAT) == CAT_CLASS; }
    public boolean isField() { return getVal(CAT) == CAT_FIELD; }
    public boolean isConstructor() { return getVal(CAT) == CAT_CONSTRUCTOR; }
    public boolean isMethod() { return getVal(CAT) == CAT_METHOD; }

    public String getPackageName() { return get(PAK, true); }
    public String getClassName() { return get(CLS, true); }
    public String getName() { return get(NAM, true); }
    public String getSignature() { return get(SIG, true); }
    public String getExceptions() { return get(EXC, true); }

    public void setStatusVersion(String v) { stver = v; }
    public String getStatusVersion() { return stver; }

    /**
     * Return the integer value for the provided type.  The type
     * must be one of the defined type names.  The return value
     * will be one of corresponding values for that type.
     */
    public int getVal(int typ) {
        validateType(typ);
        if (typ >= shifts.length) {
            return 0;
        }
        return (info >>> shifts[typ]) & masks[typ];
    }

    /**
     * Return the string value for the provided type.  The type
     * must be one of the defined type names.  The return value
     * will be one of corresponding values for that type.  Brief
     * should be true for writing data files, false for presenting
     * information to the user.
     */
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
        int val = (info >>> shifts[typ]) & masks[typ];
        return vals[val];
    }

    /**
     * Set the numeric value for the type.  The value should be a
     * value corresponding to the type.  Only the lower two bits
     * of the value are used.
     */
    public void setType(int typ, int val) {
        validateType(typ);
        if (typ < masks.length) {
            info &= ~(masks[typ] << shifts[typ]);
            info |= (val&masks[typ]) << shifts[typ];
        }
    }

    /**
     * Set the string value for the type.  For numeric types,
     * the value should be a string in 'brief' format.  For
     * non-numeric types, the value can be any
     * string.
     */
    private void setType(int typ, String val) {
        validateType(typ);
        String[] vals = shortNames[typ];
        if (vals == null) {
            if (val == null) {
                val = "";
            }
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
                info &= ~(masks[typ] << shifts[typ]);
                info |= i << shifts[typ];
                return;
            }
        }

        throw new IllegalArgumentException("unrecognized value '" + val + "' for type '" + typeNames[typ] + "'");
    }

    /**
     * Enable status version included in input/output
     */
    public void includeStatusVersion(boolean include) {
        includeStatusVer = include;
    }

    /**
     * Write the information out as a single line in brief format.
     * If there are IO errors, throws a RuntimeException.
     */
    public void writeln(BufferedWriter w) {
        try {
            for (int i = 0; i < NUM_TYPES; ++i) {
                String s = get(i, true);
                if (s != null) {
                    w.write(s);
                }
                if (includeStatusVer && i == STA) {
                    String ver = getStatusVersion();
                    if (ver.length() > 0) {
                        w.write("@");
                        w.write(getStatusVersion());
                    }
                }
                w.write(SEP);
            }
            w.newLine();
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException("IO Error");
            re.initCause(e);
            throw re;
        }
    }

    /**
     * Read a record from the input and initialize this APIInfo.
     * Return true if successful, false if EOF, otherwise throw
     * a RuntimeException.
     */
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

    /**
     * Read one token from input, which should have been written by
     * APIInfo.  Throws IOException if EOF is encountered before the
     * token is complete (i.e. before the separator character is
     * encountered) or if the token exceeds the maximum length of
     * 255 chars.
     */
    public static String readToken(BufferedReader r) throws IOException {
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

    /**
     * The default comparator for APIInfo.  This compares packages, class/name
     * (as the info represents a class or other object), category, name,
     * and signature.
     */
    public static Comparator defaultComparator() {
        final Comparator c = new Comparator() {
                public int compare(Object lhs, Object rhs) {
                    APIInfo lhi = (APIInfo)lhs;
                    APIInfo rhi = (APIInfo)rhs;
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

    /**
     * This compares two APIInfos by package, class/name, category, name, and then if
     * the APIInfo does not represent a class, by signature.  The difference between
     * this and the default comparator is that APIInfos representing classes are considered
     * equal regardless of their signatures (which represent inheritance for classes).
     */
    public static Comparator changedComparator() {
        final Comparator c = new Comparator() {
                public int compare(Object lhs, Object rhs) {
                    APIInfo lhi = (APIInfo)lhs;
                    APIInfo rhi = (APIInfo)rhs;
                    int result = lhi.pack.compareTo(rhi.pack);
                    if (result == 0) {
                        result = (lhi.getVal(CAT) == CAT_CLASS ? lhi.name : lhi.cls)
                            .compareTo(rhi.getVal(CAT) == CAT_CLASS ? rhi.name : rhi.cls);
                        if (result == 0) {
                            result = lhi.getVal(CAT)- rhi.getVal(CAT);
                            if (result == 0) {
                                result = lhi.name.compareTo(rhi.name);
                                if (result == 0 && lhi.getVal(CAT) != CAT_CLASS) {
                                    // signature change on fields ignored
                                    if (lhi.getVal(CAT) != CAT_FIELD) {
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

    /**
     * This compares two APIInfos by package, then sorts classes before non-classes, then
     * by class/name, category, name, and signature.
     */
    public static Comparator classFirstComparator() {
        final Comparator c = new Comparator() {
                public int compare(Object lhs, Object rhs) {
                    APIInfo lhi = (APIInfo)lhs;
                    APIInfo rhi = (APIInfo)rhs;
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

    /**
     * Write the data in report format.
     */
    public void print(PrintWriter pw, boolean detail, boolean html) {
        StringBuffer buf = new StringBuffer();

        // remove all occurrences of icu packages from the param string
        // fortunately, all the packages have 4 chars (lang, math, text, util).
        String xsig = sig;
        if (!detail) {
            final String ICUPACK = "com.ibm.icu.";
            StringBuffer tbuf = new StringBuffer();
            for (int i = 0; i < sig.length();) {
                int n = sig.indexOf(ICUPACK, i);
                if (n == -1) {
                    tbuf.append(sig.substring(i));
                    break;
                }
                tbuf.append(sig.substring(i, n));
                i = n + ICUPACK.length() + 5; // trailing 'xxxx.'
            }
            xsig = tbuf.toString();
        }

        // construct signature
        for (int i = STA; i < CAT; ++i) { // include status
            String s = get(i, false);
            if (s != null && s.length() > 0) {
                if (html && s.indexOf("internal") != -1) {
                    buf.append("<span style='color:red'>");
                    buf.append(s);
                    buf.append("</span>");
                } else {
                    buf.append(s);
                    buf.append(' ');
                }
            }
        }

        int val = getVal(CAT);
        switch (val) {
        case CAT_CLASS:
            if (sig.indexOf("extends") == -1) {
                buf.append("interface ");
            } else {
                buf.append("class ");
            }
        if (html) {
        buf.append("<i>");
        }
            if (cls.length() > 0) {
                buf.append(cls);
                buf.append('.');
            }
            buf.append(name);
        if (html) {
        buf.append("</i>");
        }
            if (detail) {
                buf.append(' ');
                buf.append(sig);
            }
            break;

        case CAT_FIELD:
            buf.append(xsig);
            buf.append(' ');
            buf.append(name);
            break;

        case CAT_METHOD:
        case CAT_CONSTRUCTOR:
            int n = xsig.indexOf('(');
            if (n > 0) {
                buf.append(xsig.substring(0, n));
                buf.append(' ');
            } else {
                n = 0;
            }
        if (html) {
        buf.append("<i>" + name + "</i>");
        } else {
        buf.append(name);
        }
            buf.append(xsig.substring(n));
            break;
        }

        pw.print(buf.toString());
    }

    public void println(PrintWriter pw, boolean detail, boolean html) {
        print(pw, detail, html);
        pw.println();
    }

    private static final String[] typeNames = {
        "status", "visibility", "static", "final", "synchronized",
        "abstract", "category", "package", "class", "name", "signature"
    };

    public static final String getTypeValName(int typ, int val) {
        try {
            return names[typ][val];
        }
        catch (Exception e) {
            return "";
        }
    }

    private static final String[][] names = {
        { "(draft)     ", "(stable)    ", "(deprecated)", "(obsolete)  ", "*internal*  " },
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
        { "DR", "ST", "DP", "OB", "IN" },
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
