/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class LanguageTag {

    private static final boolean JDKIMPL = false;

    //
    // static fields
    //
    public static final String SEP = "-";
    public static final String PRIVATEUSE = "x";
    public static String UNDETERMINED = "und";

    private static final String JAVAVARIANT = "variant";
    private static final String JAVASEP = "_";

    private static final SortedMap<Character, Extension> EMPTY_EXTENSION_MAP = new TreeMap<Character, Extension>();

    //
    // Language tag parser instances
    //
    public static final Parser DEFAULT_PARSER = new Parser(false);
    public static final Parser JAVA_VARIANT_COMPATIBLE_PARSER = new Parser(true);

    //
    // Language subtag fields
    //
    private String _grandfathered = ""; // grandfathered tag
    private String _language = "";      // language subtag
    private String _script = "";        // script subtag
    private String _region = "";        // region subtag
    private String _privateuse = "";    // privateuse, not including leading "x-"
    private List<String> _extlangs = Collections.emptyList();   // extlang subtags
    private List<String> _variants = Collections.emptyList();   // variant subtags
    private SortedMap<Character, Extension> _extensions = EMPTY_EXTENSION_MAP;  // extension key/value pairs

    private boolean _javaCompatVariants = false;

    // Map contains grandfathered tags and its preferred mappings from
    // http://www.ietf.org/rfc/rfc5646.txt
    private static final Map<AsciiUtil.CaseInsensitiveKey, String[]> GRANDFATHERED =
        new HashMap<AsciiUtil.CaseInsensitiveKey, String[]>();

    static {
        // grandfathered = irregular           ; non-redundant tags registered
        //               / regular             ; during the RFC 3066 era
        //
        // irregular     = "en-GB-oed"         ; irregular tags do not match
        //               / "i-ami"             ; the 'langtag' production and
        //               / "i-bnn"             ; would not otherwise be
        //               / "i-default"         ; considered 'well-formed'
        //               / "i-enochian"        ; These tags are all valid,
        //               / "i-hak"             ; but most are deprecated
        //               / "i-klingon"         ; in favor of more modern
        //               / "i-lux"             ; subtags or subtag
        //               / "i-mingo"           ; combination
        //               / "i-navajo"
        //               / "i-pwn"
        //               / "i-tao"
        //               / "i-tay"
        //               / "i-tsu"
        //               / "sgn-BE-FR"
        //               / "sgn-BE-NL"
        //               / "sgn-CH-DE"
        //
        // regular       = "art-lojban"        ; these tags match the 'langtag'
        //               / "cel-gaulish"       ; production, but their subtags
        //               / "no-bok"            ; are not extended language
        //               / "no-nyn"            ; or variant subtags: their meaning
        //               / "zh-guoyu"          ; is defined by their registration
        //               / "zh-hakka"          ; and all of these are deprecated
        //               / "zh-min"            ; in favor of a more modern
        //               / "zh-min-nan"        ; subtag or sequence of subtags
        //               / "zh-xiang"

        final String[][] entries = {
          //{"tag",         "preferred"},
            {"art-lojban",  "jbo"},
            {"cel-gaulish", "cel-gaulish"}, // gaulish is parsed as a variant
            {"en-GB-oed",   "en-GB"},       // oed (Oxford English Dictionary spelling) is ignored
            {"i-ami",       "ami"},
            {"i-bnn",       "bnn"},
            {"i-default",   UNDETERMINED},  // fallback
            {"i-enochian",  UNDETERMINED},  // fallback
            {"i-hak",       "hak"},
            {"i-klingon",   "tlh"},
            {"i-lux",       "lb"},
            {"i-mingo",     UNDETERMINED},  // fallback
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
            {"zh-min",      "zh"},          // fallback
            {"zh-min-nan",  "nan"},
            {"zh-xiang",    "hsn"},
        };
        for (String[] e : entries) {
            GRANDFATHERED.put(new AsciiUtil.CaseInsensitiveKey(e[0]), e);
        }
    }

    private LanguageTag() {
    }

    //
    // Getter methods for language subtag fields
    //

    public String getLanguage() {
        return _language;
    }

    public List<String> getExtlangs() {
        return Collections.unmodifiableList(_extlangs);
    }

    public String getScript() {
        return _script;
    }

    public String getRegion() {
        return _region;
    }

    public List<String> getVariants() {
        return Collections.unmodifiableList(_variants);
    }

    public SortedMap<Character, Extension> getExtensions() {
        return Collections.unmodifiableSortedMap(_extensions);
    }

    public String getPrivateuse() {
        return _privateuse;
    }

    public String getGrandfathered() {
        return _grandfathered;
    }

    private String getJavaVariant() {
        StringBuilder buf = new StringBuilder();
        for (String var : _variants) {
            if (buf.length() > 0) {
                buf.append(JAVASEP);
            }
            buf.append(var);
        }
        if (_javaCompatVariants) {
            return getJavaCompatibleVariant(buf.toString(), _privateuse);
        }

        return buf.toString();
    }

    private String getJavaPrivateuse() {
        if (_javaCompatVariants) {
            return getJavaCompatiblePrivateuse(_privateuse);
        }
        return _privateuse;
    }

    static String getJavaCompatibleVariant(String bcpVariants, String bcpPrivuse) {
        StringBuilder buf = new StringBuilder(bcpVariants);
        if (bcpPrivuse.length() > 0) {
            int idx = -1;
            if (bcpPrivuse.startsWith(JAVAVARIANT + SEP)) {
                idx = (JAVAVARIANT + SEP).length();
            } else {
                idx = bcpPrivuse.indexOf(SEP + JAVAVARIANT + SEP);
                if (idx != -1) {
                    idx += (SEP + JAVAVARIANT + SEP).length();
                }
            }
            if (idx != -1) {
                if (buf.length() != 0) {
                    buf.append(JAVASEP);
                }
                buf.append(bcpPrivuse.substring(idx).replace(SEP, JAVASEP));
            }
        }
        return buf.toString();
    }

    static String getJavaCompatiblePrivateuse(String bcpPrivuse) {
        if (bcpPrivuse.length() > 0) {
            int idx = -1;
            if (bcpPrivuse.startsWith(JAVAVARIANT + SEP)) {
                idx = 0;
            } else {
                idx = bcpPrivuse.indexOf(SEP + JAVAVARIANT + SEP);
            }
            if (idx != -1) {
                return bcpPrivuse.substring(0, idx);
            }
        }
        return bcpPrivuse;
    }

    public BaseLocale getBaseLocale() {
        String lang = _language;
        if (_extlangs.size() > 0) {
            // Extended language subtags are used for various historical
            // and compatibility reasons.  Each extended language subtag
            // has a "Preferred-Value', that is exactly same with the extended
            // language subtag itself.  For example,
            //
            // Type: extlang
            // Subtag: aao
            // Description: Algerian Saharan Arabic
            // Added: 2009-07-29
            // Preferred-Value: aao
            // Prefix: ar
            // Macrolanguage: ar
            //
            // For example, language tag "ar-aao-DZ" is equivalent to
            // "aao-DZ".
            //
            // Strictly speaking, the mapping requires prefix validation 
            // (e.g. primary language must be "ar" in the example above).
            // However, this implementation does not check the prefix
            // and simply use the first extlang value as locale's language.
            lang = _extlangs.get(0);
        }
        if (lang.equals(UNDETERMINED)) {
            lang = "";
        }
        return BaseLocale.getInstance(lang, _script, _region, getJavaVariant());
    }

    public LocaleExtensions getLocaleExtensions() {
        String javaPrivuse = getJavaPrivateuse();
        if (_extensions == null && javaPrivuse.length() == 0) {
            return LocaleExtensions.EMPTY_EXTENSIONS;
        }
        SortedMap<Character, Extension> exts = new TreeMap<Character, Extension>();
        if (_extensions != null) {
            exts.putAll(_extensions);
        }
        if (javaPrivuse.length() > 0) {
            PrivateuseExtension pext = new PrivateuseExtension(javaPrivuse);
            exts.put(Character.valueOf(PrivateuseExtension.SINGLETON), pext);
        }
        return LocaleExtensions.getInstance(exts);
    }

    public String getID() {
        if (_grandfathered.length() > 0) {
            return _grandfathered;
        }
        StringBuilder buf = new StringBuilder();
        if (_language.length() > 0) {
            buf.append(_language);
            if (_extlangs.size() > 0) {
                for (String el : _extlangs) {
                    buf.append(SEP);
                    buf.append(el);
                }
            }
            if (_script.length() > 0) {
                buf.append(SEP);
                buf.append(_script);
            }
            if (_region.length() > 0) {
                buf.append(SEP);
                buf.append(_region);
            }
            if (_variants.size() > 0) {
                for (String var : _variants) {
                    buf.append(SEP);
                    buf.append(var);
                }
            }
            if (_extensions.size() > 0) {
                Set<Entry<Character, Extension>> exts = _extensions.entrySet();
                for (Entry<Character, Extension> ext : exts) {
                    buf.append(SEP);
                    buf.append(ext.getKey());
                    buf.append(SEP);
                    buf.append(ext.getValue().getValue());
                }
            }
        }
        if (_privateuse.length() > 0) {
            if (buf.length() > 0) {
                buf.append(SEP);
            }
            buf.append(PRIVATEUSE);
            buf.append(SEP);
            buf.append(_privateuse);
        }
        return buf.toString();
    }

    public String toString() {
        return getID();
    }

    //
    // Language subtag syntax checking methods
    //

    public static boolean isLanguage(String s) {
        // language      = 2*3ALPHA            ; shortest ISO 639 code
        //                 ["-" extlang]       ; sometimes followed by
        //                                     ;   extended language subtags
        //               / 4ALPHA              ; or reserved for future use
        //               / 5*8ALPHA            ; or registered language subtag
        return (s.length() >= 2) && (s.length() <= 8) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isExtlang(String s) {
        // extlang       = 3ALPHA              ; selected ISO 639 codes
        //                 *2("-" 3ALPHA)      ; permanently reserved
        return (s.length() == 3) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isScript(String s) {
        // script        = 4ALPHA              ; ISO 15924 code
        return (s.length() == 4) && AsciiUtil.isAlphaString(s);
    }

    public static boolean isRegion(String s) {
        // region        = 2ALPHA              ; ISO 3166-1 code
        //               / 3DIGIT              ; UN M.49 code
        return ((s.length() == 2) && AsciiUtil.isAlphaString(s))
                || ((s.length() == 3) && AsciiUtil.isNumericString(s));
    }

    public static boolean isVariant(String s) {
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
        // singleton     = DIGIT               ; 0 - 9
        //               / %x41-57             ; A - W
        //               / %x59-5A             ; Y - Z
        //               / %x61-77             ; a - w
        //               / %x79-7A             ; y - z

        return (s.length() == 1)
                && AsciiUtil.isAlphaString(s)
                && !AsciiUtil.caseIgnoreMatch(PRIVATEUSE, s);
    }

    public static boolean isExtensionSubtag(String s) {
        // extension     = singleton 1*("-" (2*8alphanum))
        return (s.length() >= 2) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isPrivateuseSingleton(String s) {
        // privateuse    = "x" 1*("-" (1*8alphanum))
        return (s.length() == 1)
                && AsciiUtil.caseIgnoreMatch(PRIVATEUSE, s);
    }

    public static boolean isPrivateuseSubtag(String s) {
        // privateuse    = "x" 1*("-" (1*8alphanum))
        return (s.length() >= 1) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    //
    // Language subtag canonicalization methods
    //

    public static String canonicalizeLanguage(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeExtlang(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeScript(String s) {
        return AsciiUtil.toTitleString(s);
    }

    public static String canonicalizeRegion(String s) {
        return AsciiUtil.toUpperString(s);
    }

    public static String canonicalizeVariant(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeExtensionSingleton(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeExtensionSubtag(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizePrivateuseSubtag(String s) {
        return AsciiUtil.toLowerString(s);
    }


    public static LanguageTag parse(String str, boolean javaCompatVar) {
        LanguageTag tag = new LanguageTag();
        tag.parseString(str, javaCompatVar);
        return tag;
    }

    public static LanguageTag parseStrict(String str, boolean javaCompatVar) throws LocaleSyntaxException {
        LanguageTag tag = new LanguageTag();
        ParseStatus sts = tag.parseString(str, javaCompatVar);
        if (sts.isError()) {
            throw new LocaleSyntaxException(sts.errorMsg, sts.errorIndex);
        }
        return tag;
    }

    public static LanguageTag parseLocale(BaseLocale base, LocaleExtensions locExts) {
        LanguageTag tag = new LanguageTag();
        tag._javaCompatVariants = true;

        String language = base.getLanguage();
        String script = base.getScript();
        String region = base.getRegion();
        String variant = base.getVariant();

        String privuseVar = null;   // store ill-formed variant subtags

        if (language.length() > 0 && isLanguage(language)) {
            // Convert a deprecated language code used by Java to
            // a new code
            language = canonicalizeLanguage(language);
            if (language.equals("iw")) {
                language = "he";
            } else if (language.equals("ji")) {
                language = "yi";
            } else if (language.equals("in")) {
                language = "id";
            }
            tag._language = language;
        }
        if (script.length() > 0 && isScript(script)) {
            tag._script = canonicalizeScript(script);
        }
        if (region.length() > 0 && isRegion(region)) {
            tag._region = canonicalizeRegion(region);
        }
        if (variant.length() > 0) {
            List<String> variants = null;
            StringTokenIterator varitr = new StringTokenIterator(variant, JAVASEP);
            while (!varitr.isDone()) {
                String var = varitr.current();
                if (!isVariant(var)) {
                    break;
                }
                if (variants == null) {
                    variants = new ArrayList<String>();
                }
                if (JDKIMPL) {
                    variants.add(var);  // Do not canonicalize!
                } else {
                    variants.add(canonicalizeVariant(var));
                }
                varitr.next();
            }
            if (variants != null) {
                tag._variants = variants;
            }
            if (!varitr.isDone()) {
                // ill-formed variant subtags
                StringBuilder buf = new StringBuilder();
                while (!varitr.isDone()) {
                    String prvv = varitr.current();
                    if (!isPrivateuseSubtag(prvv)) {
                        // cannot use private use subtag - truncated
                        break;
                    }
                    if (buf.length() > 0) {
                        buf.append(SEP);
                    }
                    if (!JDKIMPL) {
                        prvv = AsciiUtil.toLowerString(prvv);
                    }
                    buf.append(prvv);
                    varitr.next();
                }
                if (buf.length() > 0) {
                    privuseVar = buf.toString();
                }
            }
        }

        TreeMap<Character, Extension> extensions = null;
        String privateuse = null;

        Set<Character> locextKeys = locExts.getKeys();
        for (Character locextKey : locextKeys) {
            Extension ext = locExts.getExtension(locextKey);
            if (ext instanceof PrivateuseExtension) {
                privateuse = ext.getValue();
            } else {
                if (extensions == null) {
                    extensions = new TreeMap<Character, Extension>();
                }
                extensions.put(locextKey, ext);
            }
        }

        if (extensions != null) {
            tag._extensions = extensions;
        }

        // append ill-formed variant subtags to private use
        if (privuseVar != null) {
            if (privateuse == null) {
                privateuse = JAVAVARIANT + SEP + privuseVar;
            } else {
                privateuse = privateuse + SEP + JAVAVARIANT + SEP + privuseVar.replace(JAVASEP, SEP);
            }
        }

        if (privateuse != null) {
            tag._privateuse = privateuse;
        } else if (tag._language.length() == 0) {
            // use "und" if neither language nor privateuse is available
            tag._language = UNDETERMINED;
        }

        return tag;
    }

    private ParseStatus parseString(String str, boolean javaCompatVar) {
        // Check if the tag is grandfathered
        String[] gfmap = GRANDFATHERED.get(new AsciiUtil.CaseInsensitiveKey(str));
        ParseStatus sts;
        if (gfmap != null) {
            _grandfathered = gfmap[0];
            sts = parseLanguageTag(gfmap[1], javaCompatVar);
            sts.parseLength = str.length();
        } else {
            _grandfathered = "";
            sts = parseLanguageTag(str, javaCompatVar);
        }
        return sts;
    }

    /*
     * Parse Language-Tag, except grandfathered.
     * 
     * BNF in RFC5464
     *  
     * Language-Tag  = langtag             ; normal language tags
     *               / privateuse          ; private use tag
     *               / grandfathered       ; grandfathered tags
     *
     * 
     * langtag       = language
     *                 ["-" script]
     *                 ["-" region]
     *                 *("-" variant)
     *                 *("-" extension)
     *                 ["-" privateuse]
     * 
     * language      = 2*3ALPHA            ; shortest ISO 639 code
     *                 ["-" extlang]       ; sometimes followed by
     *                                     ; extended language subtags
     *               / 4ALPHA              ; or reserved for future use
     *               / 5*8ALPHA            ; or registered language subtag
     * 
     * extlang       = 3ALPHA              ; selected ISO 639 codes
     *                 *2("-" 3ALPHA)      ; permanently reserved
     * 
     * script        = 4ALPHA              ; ISO 15924 code
     * 
     * region        = 2ALPHA              ; ISO 3166-1 code
     *               / 3DIGIT              ; UN M.49 code
     * 
     * variant       = 5*8alphanum         ; registered variants
     *               / (DIGIT 3alphanum)
     * 
     * extension     = singleton 1*("-" (2*8alphanum))
     * 
     *                                     ; Single alphanumerics
     *                                     ; "x" reserved for private use
     * singleton     = DIGIT               ; 0 - 9
     *               / %x41-57             ; A - W
     *               / %x59-5A             ; Y - Z
     *               / %x61-77             ; a - w
     *               / %x79-7A             ; y - z
     * 
     * privateuse    = "x" 1*("-" (1*8alphanum))
     * 
     */
    private ParseStatus parseLanguageTag(String langtag, boolean javaCompat) {
        ParseStatus sts = new ParseStatus();
        StringTokenIterator itr = new StringTokenIterator(langtag, SEP);
        Parser parser = javaCompat ? JAVA_VARIANT_COMPATIBLE_PARSER : DEFAULT_PARSER;

        _javaCompatVariants = javaCompat;

        // langtag must start with either language or privateuse
        _language = parser.parseLanguage(itr, sts);
        if (_language.length() > 0) {
            _extlangs = parser.parseExtlangs(itr, sts);
            _script = parser.parseScript(itr, sts);
            _region = parser.parseRegion(itr, sts);
            _variants = parser.parseVariants(itr, sts);
            _extensions = parser.parseExtensions(itr, sts);
        }
        _privateuse = parser.parsePrivateuse(itr, sts);

        if (!itr.isDone() && !sts.isError()) {
            String s = itr.current();
            sts.errorIndex = itr.currentStart();
            if (s.length() == 0) {
                sts.errorMsg = "Empty subtag";
            } else {
                sts.errorMsg = "Invalid subtag: " + s; 
            }
        }

        return sts;
    }

    public static class ParseStatus {
        int parseLength = 0;
        int errorIndex = -1;
        String errorMsg = null;

        public void reset() {
            parseLength = 0;
            errorIndex = -1;
            errorMsg = null;
        }

        boolean isError() {
            return (errorIndex >= 0);
        }
    }

    static class Parser {
        private boolean _javaCompatVar;

        Parser(boolean javaCompatVar) {
            _javaCompatVar = javaCompatVar;
        }

        //
        // Language subtag parsers
        //

        public String parseLanguage(StringTokenIterator itr, ParseStatus sts) {
            String language = "";

            if (itr.isDone() || sts.isError()) {
                return language;
            }

            String s = itr.current();
            if (isLanguage(s)) {
                language = canonicalizeLanguage(s);
                sts.parseLength = itr.currentEnd();
                itr.next();
            }
            return language;
        }

        public List<String> parseExtlangs(StringTokenIterator itr, ParseStatus sts) {
            List<String> extlangs = null;

            if (itr.isDone() || sts.isError()) {
                return Collections.emptyList();
            }

            while (!itr.isDone()) {
                String s = itr.current();
                if (!isExtlang(s)) {
                    break;
                }
                if (extlangs == null) {
                    extlangs = new ArrayList<String>(3);
                }
                extlangs.add(canonicalizeExtlang(s));
                sts.parseLength = itr.currentEnd();
                itr.next();

                if (extlangs.size() == 3) {
                    // Maximum 3 extlangs
                    break;
                }
            }

            if (extlangs == null) {
                return Collections.emptyList();
            }

            return extlangs;
        }

        public String parseScript(StringTokenIterator itr, ParseStatus sts) {
            String script = "";

            if (itr.isDone() || sts.isError()) {
                return script;
            }

            String s = itr.current();
            if (isScript(s)) {
                script = canonicalizeScript(s);
                sts.parseLength = itr.currentEnd();
                itr.next();
            }

            return script;
        }

        public String parseRegion(StringTokenIterator itr, ParseStatus sts) {
            String region = "";

            if (itr.isDone() || sts.isError()) {
                return region;
            }

            String s = itr.current();
            if (isRegion(s)) {
                region = canonicalizeRegion(s);
                sts.parseLength = itr.currentEnd();
                itr.next();
            }

            return region;
        }

        public List<String> parseVariants(StringTokenIterator itr, ParseStatus sts) {
            List<String> variants = null;

            if (itr.isDone() || sts.isError()) {
                return Collections.emptyList();
            }

            while (!itr.isDone()) {
                String s = itr.current();
                if (!isVariant(s)) {
                    break;
                }
                if (variants == null) {
                    variants = new ArrayList<String>(3);
                }
                if (_javaCompatVar) {
                    // preserve casing when Java compatibility option
                    // is enabled
                    variants.add(s);
                } else {
                    variants.add(canonicalizeVariant(s));
                }
                sts.parseLength = itr.currentEnd();
                itr.next();
            }

            if (variants == null) {
                return Collections.emptyList();
            }

            return variants;
        }

        public SortedMap<Character, Extension> parseExtensions(StringTokenIterator itr, ParseStatus sts) {
            SortedMap<Character, Extension> extensionMap = null;

            if (itr.isDone() || sts.isError()) {
                return EMPTY_EXTENSION_MAP;
            }

            while (!itr.isDone()) {
                String s = itr.current();
                if (!isExtensionSingleton(s)) {
                    break;
                }
                if (!itr.hasNext()) {
                    sts.errorIndex = itr.currentStart();
                    sts.errorMsg = "Missing extension subtag for extension :" + s;
                    break;
                }

                if (extensionMap == null) {
                    extensionMap = new TreeMap<Character, Extension>();
                }

                String singletonStr = canonicalizeExtensionSingleton(s);
                Character singleton = Character.valueOf(singletonStr.charAt(0));

                if (extensionMap.containsKey(singleton)) {
                    sts.errorIndex = itr.currentStart();
                    sts.errorMsg = "Duplicated extension: " + s;
                    break;
                }

                itr.next();
                Extension ext = Extension.create(singleton.charValue(), itr, sts);
                if (ext != null) {
                    extensionMap.put(singleton, ext);
                }
                if (sts.isError()) {
                    break;
                }
            }

            if (extensionMap == null || extensionMap.size() == 0) {
                return EMPTY_EXTENSION_MAP;
            }

            return extensionMap;
        }

        public String parsePrivateuse(StringTokenIterator itr, ParseStatus sts) {
            String privateuse = "";

            if (itr.isDone() || sts.isError()) {
                return privateuse;
            }

            String s = itr.current();
            if (isPrivateuseSingleton(s)) {
                StringBuilder buf = new StringBuilder();
                int singletonOffset = itr.currentStart();
                boolean preserveCasing = false;
                itr.next();

                while (!itr.isDone()) {
                    s = itr.current();
                    if (!isPrivateuseSubtag(s)) {
                        break;
                    }
                    if (buf.length() != 0) {
                         buf.append(SEP);
                    }
                    if (!preserveCasing) {
                        s = canonicalizePrivateuseSubtag(s);
                    }
                    buf.append(s);
                    sts.parseLength = itr.currentEnd();

                    if (_javaCompatVar && s.equals(JAVAVARIANT)) {
                        // preserve casing after the special
                        // java reserved private use subtag
                        // when java compatibility variant option
                        // is enabled.
                        preserveCasing = true;
                    }
                    itr.next();
                }

                if (buf.length() == 0) {
                    // need at least 1 private subtag
                    sts.errorIndex = singletonOffset;
                    sts.errorMsg = "Incomplete privateuse";
                } else {
                    privateuse = buf.toString();
                }
            }

            return privateuse;
        }
    }
}
