/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public final class LanguageTag {

    private String _languageTag = "";   // entire language tag
    private String _grandfathered = ""; // grandfathered tag
    private String _privateuse = "";    // privateuse, not including leading "x-"
    private String _language = "";      // language subtag
    private String[] _extlang;          // array of extlang subtags
    private String _script = "";        // script subtag
    private String _region = "";        // region subtag
    private TreeSet<String> _variants;  // variant subtags in a single string
    private TreeSet<Extension> _extensions; // extension key/value pairs

    private static final int MINLEN = 2; // minimum length of a valid language tag

    private static final String SEP = "-";
    private static final char SEPCHAR = '-';
    private static final String PRIVATEUSE = "x";

    public static String UNDETERMINED = "und";

    // Map contains grandfathered tags and its preferred mappings from
    // http://www.ietf.org/internet-drafts/draft-ietf-ltru-4645bis-09.txt
    private static final HashMap<AsciiUtil.CaseInsensitiveKey, String[]> GRANDFATHERED =
        new HashMap<AsciiUtil.CaseInsensitiveKey, String[]>();

    static {
        final String[][] entries = {
          //{"tag",         "preferred"},
            {"art-lojban",  "jbo"},
            {"cel-gaulish", ""},
            {"en-GB-oed",   ""},
            {"i-ami",       "ami"},
            {"i-bnn",       "bnn"},
            {"i-default",   ""},
            {"i-enochian",  ""},
            {"i-hak",       "hak"},
            {"i-klingon",   "tlh"},
            {"i-lux",       "lb"},
            {"i-mingo",     ""},
            {"i-navajo",    "nv"},
            {"i-pwn",       "pwn"},
            {"i-tao",       "tao"},
            {"i-tay",       "tay"},
            {"i-tsu",       "tsu"},
            {"no-bok",      "nb"},
            {"no-nyn",      "nn"},
            {"sgn-BE-FR",   "sfb"},
            {"sgn-BE-NL",   "vgt"},
            {"sgn-CH-DE",   "sgg"},
            {"zh-guoyu",    "cmn"},
            {"zh-hakka",    "hak"},
            {"zh-min",      ""},
            {"zh-min-nan",  "nan"},
            {"zh-xiang",    "hsn"},
        };
        for (String[] e : entries) {
            GRANDFATHERED.put(new AsciiUtil.CaseInsensitiveKey(e[0]), e);
        }
    }

    private static final String[][] DEPRECATEDLANGS = {
        // {<deprecated>, <current>},
        {"iw", "he"},
        {"ji", "yi"},
        {"in", "id"},
    };

    private LanguageTag(String tag) {
        _languageTag = tag;
    }

    // Bit flags used by the language tag parser
    private static final int LANG = 0x0001;
    private static final int EXTL = 0x0002;
    private static final int SCRT = 0x0004;
    private static final int REGN = 0x0008;
    private static final int VART = 0x0010;
    private static final int EXTS = 0x0020;
    private static final int EXTV = 0x0040;
    private static final int PRIV = 0x0080;

    public static LanguageTag parse(String langtag) throws LocaleSyntaxException {
        if (langtag.length() < MINLEN) {
            throw new LocaleSyntaxException("The specified tag '"
                    + langtag + "' is too short");
        }

        if (langtag.endsWith(SEP)) {
            // This code utilizes String#split, which drops off the last empty segment.
            // We need to check if the tag ends with '-' here.
            int erridx = langtag.length() - 1;
            while (erridx - 1 >= 0 && langtag.charAt(erridx - 1) != SEPCHAR) {
                erridx--;
            }
            throw new LocaleSyntaxException("The specified tag '"
                    + langtag + "' ends with " + SEP, erridx);
        }

        LanguageTag t = new LanguageTag(langtag);

        // Check if the tag is grandfathered
        String[] gfmap = GRANDFATHERED.get(new AsciiUtil.CaseInsensitiveKey(langtag));
        if (gfmap != null) {
            t._grandfathered = gfmap[0];
            // Preferred mapping
            if (gfmap[1].length() > 0) {
                t._language = gfmap[1];
            }
            return t;
        }

        // langtag       = language
        //                 ["-" script]
        //                 ["-" region]
        //                 *("-" variant)
        //                 *("-" extension)
        //                 ["-" privateuse]

        String[] subtags = langtag.split(SEP);
        int idx = 0;
        int extlangIdx = 0;
        String extSingleton = null;
        StringBuilder extBuf = null;
        int next = LANG | PRIV;
        String errorMsg = null;

        PARSE:
        while (true) {
            if (idx >= subtags.length) {
                break;
            }
            if ((next & LANG) != 0) {
                if (isLanguageSubtag(subtags[idx])) {
                    t._language = AsciiUtil.toLowerString(subtags[idx++]);
                    next = EXTL | SCRT | REGN | VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & EXTL) != 0) {
                if (isExtlangSubtag(subtags[idx])) {
                    if (extlangIdx == 0) {
                        t._extlang = new String[3];
                    }
                    t._extlang[extlangIdx++] = AsciiUtil.toLowerString(subtags[idx++]);
                    if (extlangIdx < 3) {
                        next = EXTL | SCRT | REGN | VART | EXTS | PRIV;
                    } else {
                        next = SCRT | REGN | VART | EXTS | PRIV;
                    }
                    continue;
                }
            }
            if ((next & SCRT) != 0) {
                if (isScriptSubtag(subtags[idx])) {
                    t._script = AsciiUtil.toTitleString(subtags[idx++]);
                    next = REGN | VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & REGN) != 0) {
                if (isRegionSubtag(subtags[idx])) {
                    t._region = AsciiUtil.toUpperString(subtags[idx++]);
                    next = VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & VART) != 0) {
                if (isVariantSubtag(subtags[idx])) {
                    if (t._variants == null) {
                        t._variants = new TreeSet<String>();
                    }
                    t._variants.add(AsciiUtil.toLowerString(subtags[idx++]));
                    next = VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & EXTS) != 0) {
                if (isExtensionSingleton(subtags[idx])) {
                    if (extSingleton != null) {
                        if (extBuf == null) {
                            errorMsg = "The specified tag '"
                                        + langtag + "' contains an incomplete extension: "
                                        + extSingleton;
                            break PARSE;
                        }
                        // Emit the previous extension key/value pair
                        if (t._extensions == null) {
                            t._extensions = new TreeSet<Extension>();
                        }
                        Extension e = new Extension(extSingleton.charAt(0), extBuf.toString());
                        t._extensions.add(e);
                    }
                    extSingleton = AsciiUtil.toLowerString(subtags[idx++]);
                    extBuf = null; // Clear the extension value buffer
                    next = EXTV;
                    continue;
                }
            }
            if ((next & EXTV) != 0) {
                if (isExtensionSubtag(subtags[idx])) {
                    if (extBuf == null) {
                        extBuf = new StringBuilder(AsciiUtil.toLowerString(subtags[idx++]));
                    } else {
                        extBuf.append(SEP);
                        extBuf.append(AsciiUtil.toLowerString(subtags[idx++]));
                    }
                    next = EXTS | EXTV | PRIV;
                    continue;
                }
            }
            if ((next & PRIV) != 0) {
                if (AsciiUtil.caseIgnoreMatch(PRIVATEUSE, subtags[idx])) {
                    // The rest of part will be private use value subtags
                    StringBuilder puBuf = new StringBuilder();
                    idx++;
                    for (boolean bFirst = true ; idx < subtags.length; idx++) {
                        if (!isPrivateuseValueSubtag(subtags[idx])) {
                            errorMsg = "The specified tag '"
                                        + langtag + "' contains an illegal private use subtag: "
                                        + (subtags[idx].length() == 0 ? "<empty>" : subtags[idx]);
                            break PARSE;
                        }
                        if (bFirst) {
                            bFirst = false;
                        } else {
                            puBuf.append(SEP);
                        }
                        puBuf.append(AsciiUtil.toLowerString(subtags[idx]));
                    }
                    t._privateuse = puBuf.toString();
                    if (t._privateuse.length() == 0) {
                        // Empty privateuse value
                        errorMsg = "The specified tag '"
                                    + langtag + "' contains an empty private use subtag";
                        // for error index to point 'x'
                        idx--;
                        break PARSE;
                    }
                    break;
                }
            }
            // If we fell through here, it means this subtag is illegal
            errorMsg = "The specified tag '" + langtag
                        + "' contains an illegal subtag: "
                        + (subtags[idx].length() == 0 ? "<empty>" : subtags[idx]);
            break PARSE;
        }

        if (errorMsg == null) {
            if (extSingleton != null) {
                if (extBuf == null) {
                    // extension singleton without following extension value
                    errorMsg = "The specified tag '"
                                + langtag + "' contains an incomplete extension: "
                                + extSingleton;
                } else {
                    // Emit the last extension key/value pair
                    if (t._extensions == null) {
                        t._extensions = new TreeSet<Extension>();
                    }
                    Extension e = new Extension(extSingleton.charAt(0), extBuf.toString());
                    t._extensions.add(e);
                }
            }
        }

        if (errorMsg != null) {
            // restore the original string index
            int errIndex = 0;
            for (int i = 0; i < idx; i++) {
                errIndex += (subtags[i].length() + 1);
            }
            throw new LocaleSyntaxException(errorMsg, errIndex);
        }

        return t;
    
    }

    public String getTag() {
        return _languageTag;
    }

    public String getLanguage() {
        return _language;
    }

    public String getJDKLanguage() {
        String lang = _language;
        for (String[] langMap : DEPRECATEDLANGS) {
            if (AsciiUtil.caseIgnoreCompare(lang, langMap[1]) == 0) {
                // use the old code
                lang = langMap[0];
                break;
            }
        }
        return lang;
    }

    public String getExtlang(int idx) {
        if (_extlang != null && idx < _extlang.length) {
            return _extlang[idx];
        }
        return null;
    }

    public String getScript() {
        return _script;
    }

    public String getRegion() {
        return _region;
    }

    public String getVariant() {
        if (_variants != null) {
            StringBuilder buf = new StringBuilder();
            Iterator<String> itr = _variants.iterator();
            while (itr.hasNext()) {
                if (buf.length() > 0) {
                    buf.append(SEP);
                }
                buf.append(itr.next());
            }
            return buf.toString();
        }
        return "";
    }

    public Set<String> getVarinats() {
        return Collections.unmodifiableSet(_variants);
    }

    public Set<Extension> getExtensions() {
        if (_extensions != null) {
            return Collections.unmodifiableSet(_extensions);
        }
        return null;
    }

    public String getPrivateUse() {
        return _privateuse;
    }

    public String getGrandfathered() {
        return _grandfathered;
    }

    public static boolean isLanguageSubtag(String s) {
        // language      = 2*3ALPHA            ; shortest ISO 639 code
        //                 ["-" extlang]       ; sometimes followed by
        //                                     ;   extended language subtags
        //               / 4ALPHA              ; or reserved for future use
        //               / 5*8ALPHA            ; or registered language subtag
        return (s.length() >= 2) && (s.length() <= 8) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isExtlangSubtag(String s) {
        // extlang       = 3ALPHA              ; selected ISO 639 codes
        //                 *2("-" 3ALPHA)      ; permanently reserved
        return (s.length() == 3) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isScriptSubtag(String s) {
        // script        = 4ALPHA              ; ISO 15924 code
        return (s.length() == 4) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isRegionSubtag(String s) {
        // region        = 2ALPHA              ; ISO 3166-1 code
        //               / 3DIGIT              ; UN M.49 code
        return ((s.length() == 2) && AsciiUtil.isAlphaString(s))
                || ((s.length() == 3) && AsciiUtil.isNumericString(s));
    }

    public static boolean isVariantSubtag(String s) {
        // variant       = 5*8alphanum         ; registered variants
        //               / (DIGIT 3alphanum)
        int len = s.length();
        if (len >= 5 && len <= 8) {
            return AsciiUtil.isAlphaNumericString(s);
        }
        if (len == 4) {
            return AsciiUtil.isNumeric(s.charAt(0))
                    && AsciiUtil.isAlphaNumeric(s.charAt(1))
                    && AsciiUtil.isAlphaNumeric(s.charAt(2))
                    && AsciiUtil.isAlphaNumeric(s.charAt(3));
        }
        return false;
    }

    public static boolean isExtensionSingleton(String s) {
        // extension     = singleton 1*("-" (2*8alphanum))
        return (s.length() == 1)
                && AsciiUtil.isAlphaString(s)
                && !AsciiUtil.caseIgnoreMatch(PRIVATEUSE, s);
    }

    public static boolean isExtensionSubtag(String s) {
        // extension     = singleton 1*("-" (2*8alphanum))
        return (s.length() >= 2) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isPrivateuseValueSubtag(String s) {
        // privateuse    = "x" 1*("-" (1*8alphanum))
        return (s.length() >= 1) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    /*
     * Language tag extension key/value container
     */
    public static class Extension implements Comparable<Extension> {
        private char _singleton;
        private String _value;

        public Extension(char singleton, String value) {
            _singleton = AsciiUtil.toLower(singleton);
            _value = value;
        }

        public char getSingleton() {
            return _singleton;
        }

        public String getValue() {
            return _value;
        }

        public int compareTo(Extension other) {
            return (int)_singleton - (int)other._singleton;
        }
    }

    public static String toLanguageTag(BaseLocale base, LocaleExtensions ext) {
        StringBuilder buf = new StringBuilder();

        // language
        String language = base.getLanguage();
        if (language.length() == 0) {
            buf.append(UNDETERMINED);
        } else {
            if (isLanguageSubtag(language)) {
                // if deprecated language code, map to the current one
                for (String[] langMap : DEPRECATEDLANGS) {
                    if (AsciiUtil.caseIgnoreCompare(language, langMap[0]) == 0) {
                        language = langMap[1];
                        break;
                    }
                }
                buf.append(language);
            } else {
                buf.append(UNDETERMINED);
            }
        }

        // script
        String script = base.getScript();
        if (script.length() > 0 && isScriptSubtag(script)) {
            buf.append(SEP);
            buf.append(script);
        }

        // region
        String region = base.getRegion();
        if (region.length() > 0 && isRegionSubtag(region)) {
            buf.append(SEP);
            buf.append(region);
        }

        // variant
        String variant = AsciiUtil.toLowerString(base.getVariant());
        if (variant.length() > 0) {
            String[] variants = variant.split("_");
            TreeSet<String> validVars = new TreeSet<String>();
            for (String var : variants) {
                if (isVariantSubtag(var)) {
                    validVars.add(var);
                }
            }
            if (validVars.size() > 0) {
                Iterator<String> varIt = validVars.iterator();
                while (varIt.hasNext()) {
                    buf.append(SEP);
                    buf.append(varIt.next());
                }
            }
        }

        if (ext != null && !ext.equals(LocaleExtensions.EMPTY_EXTENSIONS)) {
            String exttags = ext.getCanonicalString();
            if (exttags.length() > 0) {
                // extensions including private use
                buf.append(SEP);
                buf.append(exttags);
            }
        }
        return buf.toString();
    }
}
