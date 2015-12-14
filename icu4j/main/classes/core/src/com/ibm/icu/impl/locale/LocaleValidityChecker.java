/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.impl.locale;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.icu.impl.ValidIdentifiers;
import com.ibm.icu.impl.ValidIdentifiers.Datasubtype;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;
import com.ibm.icu.util.IllformedLocaleException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * @author markdavis
 *
 */
public class LocaleValidityChecker {
    private final Set<Datasubtype> datasubtypes;
    private final boolean allowsDeprecated;
    public static class Where {
        public Datatype fieldFailure;
        public String codeFailure;

        public boolean set(Datatype datatype, String code) {
            fieldFailure = datatype;
            codeFailure = code;
            return false;
        }
        @Override
        public String toString() {
            return fieldFailure == null ? "OK" : "{" + fieldFailure + ", " + codeFailure + "}";
        }
    }

    public LocaleValidityChecker(Set<Datasubtype> datasubtypes) {
        this.datasubtypes = EnumSet.copyOf(datasubtypes);
        allowsDeprecated = datasubtypes.contains(Datasubtype.deprecated);
    }

    public LocaleValidityChecker(Datasubtype... datasubtypes) {
        this.datasubtypes = EnumSet.copyOf(Arrays.asList(datasubtypes));
        allowsDeprecated = this.datasubtypes.contains(Datasubtype.deprecated);
    }

    /**
     * @return the datasubtypes
     */
    public Set<Datasubtype> getDatasubtypes() {
        return EnumSet.copyOf(datasubtypes);
    }

    static Pattern SEPARATOR = Pattern.compile("[-_]");

    public boolean isValid(ULocale locale, Where where) {
        where.set(null, null);
        if (!isValid(Datatype.language, locale.getLanguage(), where)) {
            // special case x
            if (locale.getLanguage().equals("x")) {
                where.set(null, null);
                // TODO check syntax is ok, only alphanum{1,8}
                return true;
            }
            return false;
        }
        if (!isValid(Datatype.script, locale.getScript(), where)) return false;
        if (!isValid(Datatype.region, locale.getCountry(), where)) return false;
        String variantString = locale.getVariant();
        if (!variantString.isEmpty()) {
            for (String variant : SEPARATOR.split(variantString)) {
                if (!isValid(Datatype.variant, variant, where)) return false;
            }
        }
        for (Character c : locale.getExtensionKeys()) {
            try {
                Datatype datatype = Datatype.valueOf(c+"");
                switch (datatype) {
                case x:
                    // TODO : check that the rest is syntactic
                    return true;
                case t:
                    if (!isValidT(locale.getExtension(c), where)) return false;
                    break;
                case u:
                    if (!isValidU(locale, locale.getExtension(c), where)) return false;
                    break;
                }
            } catch (Exception e) {
                return where.set(Datatype.illegal, c+"");
            }
        }
        return true;
    }

    enum SpecialCase {
        normal, anything, reorder, codepoints, subdivision;
        static SpecialCase get(String key) {
            if (key.equals("kr")) {
                return SpecialCase.reorder;
            } else if (key.equals("vt")) {
                return SpecialCase.codepoints;
            } else if (key.equals("sd")) {
                return subdivision;
            } else if (key.equals("x0")) {
                return anything;
            } else {
                return normal;
            }
        }
    }
    /**
     * @param locale 
     * @param extension
     * @param where
     * @return
     */
    private boolean isValidU(ULocale locale, String extensionString, Where where) {
        String key = "";
        int typeCount = 0;
        ValueType valueType = null;
        SpecialCase specialCase = null;
        StringBuilder prefix = new StringBuilder();
        // TODO: is empty -u- valid?
        for (String subtag : SEPARATOR.split(extensionString)) {
            if (subtag.length() == 2) {
                key = KeyTypeData.toBcpKey(subtag);
                if (key == null) {
                    return where.set(Datatype.u, subtag);
                }
                if (!allowsDeprecated && KeyTypeData.isDeprecated(key)) {
                    return where.set(Datatype.u, key);
                }
                valueType = ValueType.get(key);
                specialCase = SpecialCase.get(key);
                typeCount = 0;
            } else {
                ++typeCount;
                switch (valueType) {
                case single: 
                    if (typeCount > 1) {
                        return where.set(Datatype.u, key+"-"+subtag);
                    }
                    break;
                case incremental:
                    if (typeCount == 1) {
                        prefix.setLength(0);
                        prefix.append(subtag);
                    } else {
                        prefix.append('-').append(subtag);
                        subtag = prefix.toString();
                    }
                }
                switch (specialCase) {
                case anything: 
                    continue;
                case codepoints: 
                    try {
                        if (Integer.parseInt(subtag,16) > 0x10FFFF) {
                            return where.set(Datatype.u, key+"-"+subtag);
                        }
                    } catch (NumberFormatException e) {
                        return where.set(Datatype.u, key+"-"+subtag);
                    }
                    continue;
                case reorder:
                    if (!isScriptReorder(subtag)) {
                        return where.set(Datatype.u, key+"-"+subtag);
                    }
                    continue;
                case subdivision:
                    if (!isSubdivision(locale, subtag)) {
                        return where.set(Datatype.u, key+"-"+subtag);
                    }
                    continue;
                }

                // en-u-sd-usca
                // en-US-u-sd-usca
                Output<Boolean> isKnownKey = new Output<Boolean>();
                Output<Boolean> isSpecialType = new Output<Boolean>();
                String type = KeyTypeData.toBcpType(key, subtag, isKnownKey, isSpecialType);
                if (type == null) {
                    return where.set(Datatype.u, key+"-"+subtag);
                }
                if (!allowsDeprecated && KeyTypeData.isDeprecated(key, subtag)) {
                    return where.set(Datatype.u, key+"-"+subtag);
                }
            }
        }
        return true;
    }

    /**
     * @param locale
     * @param subtag
     * @return
     */
    private boolean isSubdivision(ULocale locale, String subtag) {
        // First check if the subtag is valid
        if (subtag.length() < 3) {
            return false;
        }
        String region = subtag.substring(0, subtag.charAt(0) <= '9' ? 3 : 2);
        String subdivision = subtag.substring(region.length());
        if (ValidIdentifiers.isValid(Datatype.subdivision, datasubtypes, region, subdivision) == null) {
            return false;
        }
        // Then check for consistency with the locale's region
        String localeRegion = locale.getCountry();
        if (localeRegion.isEmpty()) {
            ULocale max = ULocale.addLikelySubtags(locale);
            localeRegion = max.getCountry();
        }
        if (!region.equalsIgnoreCase(localeRegion)) {
            return false;
        }
        return true;
    }

    static final Set<String> REORDERING_INCLUDE = new HashSet<String>(Arrays.asList("space", "punct", "symbol", "currency", "digit", "others"));
    static final Set<String> REORDERING_EXCLUDE = new HashSet<String>(Arrays.asList("zinh", "zyyy"));
    /**
     * @param subtag
     * @return
     */
    private boolean isScriptReorder(String subtag) {
        subtag = AsciiUtil.toLowerString(subtag);
        if (REORDERING_INCLUDE.contains(subtag)) {
            return true;
        } else if (REORDERING_EXCLUDE.contains(subtag)) {
            return false;
        }
        return ValidIdentifiers.isValid(Datatype.script, datasubtypes, subtag) != null;
        //        space, punct, symbol, currency, digit - core groups of characters below 'a'
        //        any script code except Common and Inherited.
        //      sc ; Zinh                             ; Inherited                        ; Qaai
        //      sc ; Zyyy                             ; Common
        //        Some pairs of scripts sort primary-equal and always reorder together. For example, Katakana characters are are always reordered with Hiragana.
        //        others - where all codes not explicitly mentioned should be ordered. The script code Zzzz (Unknown Script) is a synonym for others.        return false;
    }

    /**
     * @param extensionString
     * @param where
     * @return
     */
    private boolean isValidT(String extensionString, Where where) {
        // TODO: is empty -t- valid?
        // TODO stop at first tag ([a-z][0-9]) and check their validity separately
        try {
            ULocale locale = new ULocale.Builder().setLanguageTag(extensionString).build();
            return isValid(locale, where);
        } catch (IllformedLocaleException e) {
            int startIndex = e.getErrorIndex();
            String[] list = SEPARATOR.split(extensionString.substring(startIndex));
            return where.set(Datatype.t, list[0]);
        } catch (Exception e) {
            return where.set(Datatype.t, e.getMessage());
        }
    }

    /**
     * @param language
     * @param language2
     * @return
     */
    private boolean isValid(Datatype datatype, String code, Where where) {
        return datatype == Datatype.language && code.equalsIgnoreCase("root") ? true
                : code.isEmpty() ? true
                        : ValidIdentifiers.isValid(datatype, datasubtypes, code) != null ? true 
                                : where == null ? false : where.set(datatype, code);
    }

    public enum ValueType {
        single, multiple, incremental;
        private static Set<String> multipleValueTypes = new HashSet<String>(Arrays.asList("x0", "kr", "vt"));
        private static Set<String> specificValueTypes = new HashSet<String>(Arrays.asList("ca"));
        static ValueType get(String key) {
            if (multipleValueTypes.contains(key)) {
                return multiple;
            } else if (specificValueTypes.contains(key)) {
                return incremental;
            } else {
                return single;
            }
        }
    }
    /*
Type: any multiple
{"OK", "en-t-x0-SPECIAL"}
{"OK", "en-u-kr-REORDER_CODE"}, // Collation reorder codes; One or more collation reorder codes, see LDML Part 5: Collation
{"OK", "en-u-vt-CODEPOINTS"}, // deprecated Collation parameter key for variable top; The variable top (one or more Unicode code points: LDML Appendix Q)

Multiple-values, specific sequences
<type name="islamic-umalqura" description="Islamic calendar, Umm al-Qura" since="24"/>
     */

}
