/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.InvalidLocaleException;

public final class LanguageTag {

    private String _languageTag;        // entire language tag
    private String _grandfathered;      // grandfathered tag
    private String _privateuse;         // privateuse, not including leading "x-"
    private String _language;           // language subtag
    private String[] _extlang;          // array of extlang subtags
    private String _script;             // script subtag
    private String _region;             // region subtag
    private String _variant;            // variant subtags in a single string
    private List/*<Extension>*/ _extension; // extension key/value pairs

    private static final int MINLEN = 2; // minimum length of a valid language tag

    private static final String SEP = "-";
    private static final String PRIVATEUSE = "x";

    // Map contains grandfathered tags and its preferred mappings from
    // http://www.ietf.org/internet-drafts/draft-ietf-ltru-4645bis-09.txt
//    private static final ConcurrentHashMap<String,String> GRANDFATHERED = 
//        new ConcurrentHashMap<String,String>();

    private static final Map GRANDFATHERED = new HashMap();

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
        //for (String[] e : entries) {
        for (int i = 0; i < entries.length; i++) {
            String[] e = entries[i];
            GRANDFATHERED.put(e[0], e[1]);
        }
    }

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

    public static LanguageTag parse(String tag) throws InvalidLocaleException {
        if (tag.length() < MINLEN) {
            throw new InvalidLocaleException("The specified tag '"
                    + tag + "' is too short");
        }

        if (tag.endsWith(SEP)) {
            // This code utilizes Stirng#split, which drops off the last empty segment.
            // We need to check if the tag ends with '-' here.
            throw new InvalidLocaleException("The specified tag '"
                    + tag + "' ends with " + SEP);
        }

        LanguageTag t = new LanguageTag(tag);

        // Check if the tag is grandfathered
        if (GRANDFATHERED.containsKey(tag)) {
            t._grandfathered = tag;
            // Preferred mapping
            String preferred = (String)GRANDFATHERED.get(tag);
            if (preferred.length() > 0) {
                t._language = preferred;
            }
            return t;
        }

        // langtag       = language
        //                 ["-" script]
        //                 ["-" region]
        //                 *("-" variant)
        //                 *("-" extension)
        //                 ["-" privateuse]

        //String[] subtags = tag.split(SEP);
        String[] subtags = Utility.split(tag, SEP.charAt(0));
        int idx = 0;
        int extlangIdx = 0;
        StringBuffer varBuf = null;
        String extSingleton = null;
        StringBuffer extBuf = null;
        int next = LANG | PRIV;
        while (true) {
            if (idx >= subtags.length) {
                break;
            }
            if ((next & LANG) != 0) {
                if (isLanguageSubtag(subtags[idx])) {
                    t._language = subtags[idx++];
                    next = EXTL | SCRT | REGN | VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & EXTL) != 0) {
                if (isExtlangSubtag(subtags[idx])) {
                    if (extlangIdx == 0) {
                        t._extlang = new String[3];
                    }
                    t._extlang[extlangIdx++] = subtags[idx++];
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
                    t._script = subtags[idx++];
                    next = REGN | VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & REGN) != 0) {
                if (isRegionSubtag(subtags[idx])) {
                    t._region = subtags[idx++];
                    next = VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & VART) != 0) {
                if (isVariantSubtag(subtags[idx])) {
                    if (varBuf == null) {
                        varBuf = new StringBuffer(subtags[idx++]);
                    } else {
                        varBuf.append(SEP);
                        varBuf.append(subtags[idx++]);
                    }
                    next = VART | EXTS | PRIV;
                    continue;
                }
            }
            if ((next & EXTS) != 0) {
                if (isExtensionSingleton(subtags[idx])) {
                    if (extSingleton != null) {
                        if (extBuf == null) {
                            throw new InvalidLocaleException("The specified tag '"
                                    + tag + "' contains an incomplete extension: "
                                    + extSingleton);
                        }
                        // Emit the previous extension key/value pair
                        if (t._extension == null) {
                            t._extension = new LinkedList/*<Extension>*/();
                        }
                        Extension e = new Extension(extSingleton, extBuf.toString());
                        t._extension.add(e);
                    }
                    extSingleton = subtags[idx++];
                    extBuf = null; // Clear the extension value buffer
                    next = EXTV;
                    continue;
                }
            }
            if ((next & EXTV) != 0) {
                if (isExtensionSubtag(subtags[idx])) {
                    if (extBuf == null) {
                        extBuf = new StringBuffer(subtags[idx++]);
                    } else {
                        extBuf.append(SEP);
                        extBuf.append(subtags[idx++]);
                    }
                    next = EXTS | EXTV | PRIV;
                    continue;
                }
            }
            if ((next & PRIV) != 0) {
                if (AsciiUtil.caseIgnoreMatch(PRIVATEUSE, subtags[idx])) {
                    // The rest of part will be private use value subtags
                    StringBuffer puBuf = new StringBuffer();
                    idx++;
                    for (boolean bFirst = true ; idx < subtags.length; idx++) {
                        if (!isPrivateuseValueSubtag(subtags[idx])) {
                            throw new InvalidLocaleException("The specified tag '"
                                    + tag + "' contains an illegal private use subtag: "
                                    + (subtags[idx].length() == 0 ? "<empty>" : subtags[idx]));
                        }
                        if (bFirst) {
                            bFirst = false;
                        } else {
                            puBuf.append(SEP);
                        }
                        puBuf.append(subtags[idx]);
                    }
                    t._privateuse = puBuf.toString();
                    if (t._privateuse.length() == 0) {
                        // Empty privateuse value
                        throw new InvalidLocaleException("The specified tag '"
                                + tag + "' contains an empty private use subtag");
                    }
                    break;
                }
            }
            // If we fell through here, it means this subtag is illegal
            throw new InvalidLocaleException("The specified tag '" + tag
                    + "' contains an illegal subtag: "
                    + (subtags[idx].length() == 0 ? "<empty>" : subtags[idx]));
        }

        if (varBuf != null) {
            t._variant = varBuf.toString();
        }

        if (extSingleton != null) {
            if (extBuf == null) {
                // extension singleton without following extension value
                throw new InvalidLocaleException("The specified tag '"
                        + tag + "' contains an incomplete extension: "
                        + extSingleton);
            }
            // Emit the last extension key/value pair
            if (t._extension == null) {
                t._extension = new LinkedList/*<Extension>*/();
            }
            Extension e = new Extension(extSingleton, extBuf.toString());
            t._extension.add(e);
        }

        return t;
    }

    public String getTag() {
        return _languageTag;
    }

    public String getLanguage() {
        return _language;
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
        return _variant;
    }

    public List/*<Extension>*/ getExtensions() {
        if (_extension != null) {
            return Collections.unmodifiableList(_extension);
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
                    && AsciiUtil.isAlpha(s.charAt(1))
                    && AsciiUtil.isAlpha(s.charAt(2))
                    && AsciiUtil.isAlpha(s.charAt(3));
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
    public static class Extension {
        private String _singleton;
        private String _value;

        public Extension(String singleton, String value) {
            _singleton = singleton;
            _value = value;
        }

        public String getSingleton() {
            return _singleton;
        }

        public String getValue() {
            return _value;
        }
    }
}
