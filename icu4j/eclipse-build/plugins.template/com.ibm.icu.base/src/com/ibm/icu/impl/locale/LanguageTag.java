// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2010-2011, International Business Machines Corporation and    *
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

public class LanguageTag {
    private static final boolean JDKIMPL = false;

    //
    // static fields
    //
    public static final String SEP = "-";
    public static final String PRIVATEUSE = "x";
    public static String UNDETERMINED = "und";
    public static final String PRIVUSE_VARIANT_PREFIX = "lvariant";

    //
    // Language subtag fields
    //
    private String _language = "";      // language subtag
    private String _script = "";        // script subtag
    private String _region = "";        // region subtag
    private String _privateuse = "";    // privateuse

    private List<String> _extlangs = Collections.emptyList();   // extlang subtags
    private List<String> _variants = Collections.emptyList();   // variant subtags
    private List<String> _extensions = Collections.emptyList(); // extensions

    // The Map contains legacy language tags (marked as “Type: grandfathered” in BCP 47)
    // and their preferred mappings from BCP 47.
    private static final Map<AsciiUtil.CaseInsensitiveKey, String[]> LEGACY =
        new HashMap<AsciiUtil.CaseInsensitiveKey, String[]>();

    static {
        // legacy        = irregular           ; non-redundant tags registered
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
            {"cel-gaulish", "xtg-x-cel-gaulish"},   // fallback
            {"en-GB-oed",   "en-GB-x-oed"},         // fallback
            {"i-ami",       "ami"},
            {"i-bnn",       "bnn"},
            {"i-default",   "en-x-i-default"},      // fallback
            {"i-enochian",  "und-x-i-enochian"},    // fallback
            {"i-hak",       "hak"},
            {"i-klingon",   "tlh"},
            {"i-lux",       "lb"},
            {"i-mingo",     "see-x-i-mingo"},       // fallback
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
            {"zh-min",      "nan-x-zh-min"},        // fallback
            {"zh-min-nan",  "nan"},
            {"zh-xiang",    "hsn"},
        };
        for (String[] e : entries) {
            LEGACY.put(new AsciiUtil.CaseInsensitiveKey(e[0]), e);
        }
    }

    private LanguageTag() {
    }

    /**
     * See BCP 47 “Tags for Identifying Languages”:
     * https://www.rfc-editor.org/info/bcp47 -->
     * https://www.rfc-editor.org/rfc/rfc5646.html#section-2.1
     */
    public static LanguageTag parse(String languageTag, ParseStatus sts) {
        if (sts == null) {
            sts = new ParseStatus();
        } else {
            sts.reset();
        }

        StringTokenIterator itr;

        String[] gfmap = LEGACY.get(new AsciiUtil.CaseInsensitiveKey(languageTag));
        if (gfmap != null) {
            // use preferred mapping
            itr = new StringTokenIterator(gfmap[1], SEP);
        } else {
            itr = new StringTokenIterator(languageTag, SEP);
        }

        LanguageTag tag = new LanguageTag();

        // langtag must start with either language or privateuse
        if (tag.parseLanguage(itr, sts)) {
            tag.parseExtlangs(itr, sts);
            tag.parseScript(itr, sts);
            tag.parseRegion(itr, sts);
            tag.parseVariants(itr, sts);
            tag.parseExtensions(itr, sts);
        }
        tag.parsePrivateuse(itr, sts);

        if (!itr.isDone() && !sts.isError()) {
            String s = itr.current();
            sts._errorIndex = itr.currentStart();
            if (s.length() == 0) {
                sts._errorMsg = "Empty subtag";
            } else {
                sts._errorMsg = "Invalid subtag: " + s; 
            }
        }

        return tag;
    }

    //
    // Language subtag parsers
    //

    private boolean parseLanguage(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        String s = itr.current();
        if (isLanguage(s)) {
            found = true;
            _language = s;
            sts._parseLength = itr.currentEnd();
            itr.next();
        }

        return found;
    }

    private boolean parseExtlangs(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        while (!itr.isDone()) {
            String s = itr.current();
            if (!isExtlang(s)) {
                break;
            }
            found = true;
            if (_extlangs.isEmpty()) {
                _extlangs = new ArrayList<String>(3);
            }
            _extlangs.add(s);
            sts._parseLength = itr.currentEnd();
            itr.next();

            if (_extlangs.size() == 3) {
                // Maximum 3 extlangs
                break;
            }
        }

        return found;
    }

    private boolean parseScript(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        String s = itr.current();
        if (isScript(s)) {
            found = true;
            _script = s;
            sts._parseLength = itr.currentEnd();
            itr.next();
        }

        return found;
    }

    private boolean parseRegion(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        String s = itr.current();
        if (isRegion(s)) {
            found = true;
            _region = s;
            sts._parseLength = itr.currentEnd();
            itr.next();
        }

        return found;
    }

    private boolean parseVariants(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        while (!itr.isDone()) {
            String s = itr.current();
            if (!isVariant(s)) {
                break;
            }
            found = true;
            if (_variants.isEmpty()) {
                _variants = new ArrayList<String>(3);
            }
            _variants.add(s);
            sts._parseLength = itr.currentEnd();
            itr.next();
        }

        return found;
    }

    private boolean parseExtensions(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        while (!itr.isDone()) {
            String s = itr.current();
            if (isExtensionSingleton(s)) {
                int start = itr.currentStart();
                String singleton = s;
                StringBuilder sb = new StringBuilder(singleton);

                itr.next();
                while (!itr.isDone()) {
                    s = itr.current();
                    if (isExtensionSubtag(s)) {
                        sb.append(SEP).append(s);
                        sts._parseLength = itr.currentEnd();
                    } else {
                        break;
                    }
                    itr.next();
                }

                if (sts._parseLength <= start) {
                    sts._errorIndex = start;
                    sts._errorMsg = "Incomplete extension '" + singleton + "'";
                    break;
                }

                if (_extensions.size() == 0) {
                    _extensions = new ArrayList<String>(4);
                }
                _extensions.add(sb.toString());
                found = true;
            } else {
                break;
            }
        }
        return found;
    }

    private boolean parsePrivateuse(StringTokenIterator itr, ParseStatus sts) {
        if (itr.isDone() || sts.isError()) {
            return false;
        }

        boolean found = false;

        String s = itr.current();
        if (isPrivateusePrefix(s)) {
            int start = itr.currentStart();
            StringBuilder sb = new StringBuilder(s);

            itr.next();
            while (!itr.isDone()) {
                s = itr.current();
                if (!isPrivateuseSubtag(s)) {
                    break;
                }
                sb.append(SEP).append(s);
                sts._parseLength = itr.currentEnd();

                itr.next();
            }

            if (sts._parseLength <= start) {
                // need at least 1 private subtag
                sts._errorIndex = start;
                sts._errorMsg = "Incomplete privateuse";
            } else {
                _privateuse = sb.toString();
                found = true;
            }
        }

        return found;
    }

    public static LanguageTag parseLocale(BaseLocale baseLocale, LocaleExtensions localeExtensions) {
        LanguageTag tag = new LanguageTag();

        String language = baseLocale.getLanguage();
        String script = baseLocale.getScript();
        String region = baseLocale.getRegion();
        String variant = baseLocale.getVariant();

        boolean hasSubtag = false;

        String privuseVar = null;   // store ill-formed variant subtags

        if (language.length() > 0 && isLanguage(language)) {
            // Convert a deprecated language code used by Java to
            // a new code
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
            hasSubtag = true;
        }

        if (region.length() > 0 && isRegion(region)) {
            tag._region = canonicalizeRegion(region);
            hasSubtag = true;
        }

        if (JDKIMPL) {
            // Special handling for no_NO_NY - use nn_NO for language tag
            if (tag._language.equals("no") && tag._region.equals("NO") && variant.equals("NY")) {
                tag._language = "nn";
                variant = "";
            }
        }

        if (variant.length() > 0) {
            List<String> variants = null;
            StringTokenIterator varitr = new StringTokenIterator(variant, BaseLocale.SEP);
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
                hasSubtag = true;
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

        List<String> extensions = null;
        String privateuse = null;

        Set<Character> locextKeys = localeExtensions.getKeys();
        for (Character locextKey : locextKeys) {
            Extension ext = localeExtensions.getExtension(locextKey);
            if (isPrivateusePrefixChar(locextKey.charValue())) {
                privateuse = ext.getValue();
            } else {
                if (extensions == null) {
                    extensions = new ArrayList<String>();
                }
                extensions.add(locextKey.toString() + SEP + ext.getValue());
            }
        }

        if (extensions != null) {
            tag._extensions = extensions;
            hasSubtag = true;
        }

        // append ill-formed variant subtags to private use
        if (privuseVar != null) {
            if (privateuse == null) {
                privateuse = PRIVUSE_VARIANT_PREFIX + SEP + privuseVar;
            } else {
                privateuse = privateuse + SEP + PRIVUSE_VARIANT_PREFIX + SEP + privuseVar.replace(BaseLocale.SEP, SEP);
            }
        }

        if (privateuse != null) {
            tag._privateuse = privateuse;
        }

        if (tag._language.length() == 0 && (hasSubtag || privateuse == null)) {
            // use lang "und" when 1) no language is available AND
            // 2) any of other subtags other than private use are available or
            // no private use tag is available
            tag._language = UNDETERMINED;
        }

        return tag;
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

    public List<String> getExtensions() {
        return Collections.unmodifiableList(_extensions);
    }

    public String getPrivateuse() {
        return _privateuse;
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

    public static boolean isExtensionSingletonChar(char c) {
        return isExtensionSingleton(String.valueOf(c));
    }

    public static boolean isExtensionSubtag(String s) {
        // extension     = singleton 1*("-" (2*8alphanum))
        return (s.length() >= 2) && (s.length() <= 8) && AsciiUtil.isAlphaNumericString(s);
    }

    public static boolean isPrivateusePrefix(String s) {
        // privateuse    = "x" 1*("-" (1*8alphanum))
        return (s.length() == 1)
                && AsciiUtil.caseIgnoreMatch(PRIVATEUSE, s);
    }

    public static boolean isPrivateusePrefixChar(char c) {
        return (AsciiUtil.caseIgnoreMatch(PRIVATEUSE, String.valueOf(c)));
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

    public static String canonicalizeExtension(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeExtensionSingleton(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizeExtensionSubtag(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizePrivateuse(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public static String canonicalizePrivateuseSubtag(String s) {
        return AsciiUtil.toLowerString(s);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (_language.length() > 0) {
            sb.append(_language);

            for (String extlang : _extlangs) {
                sb.append(SEP).append(extlang);
            }

            if (_script.length() > 0) {
                sb.append(SEP).append(_script);
            }

            if (_region.length() > 0) {
                sb.append(SEP).append(_region);
            }

            for (String variant : _extlangs) {
                sb.append(SEP).append(variant);
            }

            for (String extension : _extensions) {
                sb.append(SEP).append(extension);
            }
        }
        if (_privateuse.length() > 0) {
            if (sb.length() > 0) {
                sb.append(SEP);
            }
            sb.append(_privateuse);
        }

        return sb.toString();
    }
}
