/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.VersionInfo;


/**
 * Provides a general interface for Unicode Properties, and
 * extracting sets based on those values.
 * @author Davis
 */

public class ICUPropertyFactory extends UnicodeProperty.Factory {

    static class ICUProperty extends UnicodeProperty {
        protected int propEnum = Integer.MIN_VALUE;

        protected ICUProperty(String propName, int propEnum) {
            setName(propName);
            this.propEnum = propEnum;
            setType(internalGetPropertyType(propEnum));
            if (propEnum == UProperty.DEFAULT_IGNORABLE_CODE_POINT || propEnum == UProperty.BIDI_CLASS) {
                setUniformUnassigned(false);
            }
        }

        boolean shownException = false;

        public String _getValue(int codePoint) {
            switch (propEnum) {
            case UProperty.AGE:
                return getAge(codePoint);
            case UProperty.BIDI_MIRRORING_GLYPH:
                return UTF16.valueOf(UCharacter.getMirror(codePoint));
            case UProperty.CASE_FOLDING:
                return UCharacter.foldCase(UTF16.valueOf(codePoint), true);
            case UProperty.ISO_COMMENT:
                return UCharacter.getISOComment(codePoint);
            case UProperty.LOWERCASE_MAPPING:
                return UCharacter.toLowerCase(Locale.ENGLISH, UTF16.valueOf(codePoint));
            case UProperty.NAME:
                return UCharacter.getName(codePoint);
            case UProperty.SIMPLE_CASE_FOLDING:
                return UTF16.valueOf(UCharacter.foldCase(codePoint, true));
            case UProperty.SIMPLE_LOWERCASE_MAPPING:
                return UTF16.valueOf(UCharacter.toLowerCase(codePoint));
            case UProperty.SIMPLE_TITLECASE_MAPPING:
                return UTF16.valueOf(UCharacter.toTitleCase(codePoint));
            case UProperty.SIMPLE_UPPERCASE_MAPPING:
                return UTF16.valueOf(UCharacter.toUpperCase(codePoint));
            case UProperty.TITLECASE_MAPPING:
                return UCharacter.toTitleCase(Locale.ENGLISH, UTF16.valueOf(codePoint), null);
            case UProperty.UNICODE_1_NAME:
                return UCharacter.getName1_0(codePoint);
            case UProperty.UPPERCASE_MAPPING:
                return UCharacter.toUpperCase(Locale.ENGLISH, UTF16.valueOf(codePoint));
            // case NFC: return Normalizer.normalize(codePoint, Normalizer.NFC);
            // case NFD: return Normalizer.normalize(codePoint, Normalizer.NFD);
            // case NFKC: return Normalizer.normalize(codePoint, Normalizer.NFKC);
            // case NFKD: return Normalizer.normalize(codePoint, Normalizer.NFKD);
            case isNFC:
                return String.valueOf(Normalizer.normalize(codePoint, Normalizer.NFC).equals(UTF16.valueOf(codePoint)));
            case isNFD:
                return String.valueOf(Normalizer.normalize(codePoint, Normalizer.NFD).equals(UTF16.valueOf(codePoint)));
            case isNFKC:
                return String
                        .valueOf(Normalizer.normalize(codePoint, Normalizer.NFKC).equals(UTF16.valueOf(codePoint)));
            case isNFKD:
                return String
                        .valueOf(Normalizer.normalize(codePoint, Normalizer.NFKD).equals(UTF16.valueOf(codePoint)));
            case isLowercase:
                return String.valueOf(UCharacter.toLowerCase(Locale.ENGLISH, UTF16.valueOf(codePoint)).equals(
                        UTF16.valueOf(codePoint)));
            case isUppercase:
                return String.valueOf(UCharacter.toUpperCase(Locale.ENGLISH, UTF16.valueOf(codePoint)).equals(
                        UTF16.valueOf(codePoint)));
            case isTitlecase:
                return String.valueOf(UCharacter.toTitleCase(Locale.ENGLISH, UTF16.valueOf(codePoint), null).equals(
                        UTF16.valueOf(codePoint)));
            case isCasefolded:
                return String.valueOf(UCharacter.foldCase(UTF16.valueOf(codePoint), true).equals(
                        UTF16.valueOf(codePoint)));
            case isCased:
                return String.valueOf(UCharacter.toLowerCase(Locale.ENGLISH, UTF16.valueOf(codePoint)).equals(
                        UTF16.valueOf(codePoint)));
            }
            if (propEnum < UProperty.INT_LIMIT) {
                int enumValue = -1;
                String value = null;
                try {
                    enumValue = UCharacter.getIntPropertyValue(codePoint, propEnum);
                    if (enumValue >= 0)
                        value = fixedGetPropertyValueName(propEnum, enumValue, UProperty.NameChoice.LONG);
                } catch (IllegalArgumentException e) {
                    if (!shownException) {
                        System.out.println("Fail: " + getName() + ", " + Integer.toHexString(codePoint));
                        shownException = true;
                    }
                }
                return value != null ? value : String.valueOf(enumValue);
            } else if (propEnum < UProperty.DOUBLE_LIMIT) {
                double num = UCharacter.getUnicodeNumericValue(codePoint);
                if (num == UCharacter.NO_NUMERIC_VALUE)
                    return null;
                return Double.toString(num);
                // TODO: Fix HACK -- API deficient
            }
            return null;
        }

        private String getAge(int codePoint) {
            String temp = UCharacter.getAge(codePoint).toString();
            if (temp.equals("0.0.0.0"))
                return "unassigned";
            if (temp.endsWith(".0.0"))
                return temp.substring(0, temp.length() - 4);
            return temp;
        }

        /**
         * @param valueAlias null if unused.
         * @param valueEnum -1 if unused
         * @param nameChoice
         * @return
         */
        private String getFixedValueAlias(String valueAlias, int valueEnum, int nameChoice) {
            if (propEnum >= UProperty.STRING_START) {
                if (nameChoice > UProperty.NameChoice.LONG)
                    throw new IllegalArgumentException();
                if (nameChoice != UProperty.NameChoice.LONG)
                    return null;
                return "<string>";
            } else if (propEnum >= UProperty.DOUBLE_START) {
                if (nameChoice > UProperty.NameChoice.LONG)
                    throw new IllegalArgumentException();
                if (nameChoice != UProperty.NameChoice.LONG)
                    return null;
                return "<number>";
            }
            if (valueAlias != null && !valueAlias.equals("<integer>")) {
                valueEnum = fixedGetPropertyValueEnum(propEnum, valueAlias);
            }
            // because these are defined badly, there may be no normal (long) name.
            // if there is
            String result = fixedGetPropertyValueName(propEnum, valueEnum, nameChoice);
            if (result != null)
                return result;
            // HACK try other namechoice
            if (nameChoice == UProperty.NameChoice.LONG) {
                result = fixedGetPropertyValueName(propEnum, valueEnum, UProperty.NameChoice.SHORT);
                if (result != null)
                    return result;
                if (isCombiningClassProperty())
                    return null;
                return "<integer>";
            }
            return null;
        }

        public boolean isCombiningClassProperty() {
            return (propEnum == UProperty.CANONICAL_COMBINING_CLASS
                    || propEnum == UProperty.LEAD_CANONICAL_COMBINING_CLASS
                    || propEnum == UProperty.TRAIL_CANONICAL_COMBINING_CLASS);
        }

        private static int fixedGetPropertyValueEnum(int propEnum, String valueAlias) {
            try {
                if (propEnum < BINARY_LIMIT) {
                    propEnum = UProperty.ALPHABETIC;
                }
                return UCharacter.getPropertyValueEnum(propEnum, valueAlias);
            } catch (Exception e) {
                return Integer.parseInt(valueAlias);
            }
        }

        static Map fixSkeleton = new HashMap();

        private static String fixedGetPropertyValueName(int propEnum, int valueEnum, int nameChoice) {
            String value = UCharacter.getPropertyValueName(propEnum, valueEnum, nameChoice);
            String newValue = (String) fixSkeleton.get(value);
            if (newValue == null) {
                newValue = value;
                if (propEnum == UProperty.JOINING_GROUP) {
                    newValue = newValue == null ? null : newValue.toLowerCase(Locale.ENGLISH);
                }
                newValue = regularize(newValue, true);
                fixSkeleton.put(value, newValue);
            }
            return newValue;
        }

        public List _getNameAliases(List result) {
            if (result == null)
                result = new ArrayList();
            // String alias = String_Extras.get(propEnum);
            // if (alias == null)
            String alias = Binary_Extras.get(propEnum);
            if (alias != null) {
                addUnique(alias, result);
            } else {
                addUnique(getFixedPropertyName(propEnum, UProperty.NameChoice.SHORT), result);
                addUnique(getFixedPropertyName(propEnum, UProperty.NameChoice.LONG), result);
            }
            return result;
        }

        public String getFixedPropertyName(int propName, int nameChoice) {
            try {
                return UCharacter.getPropertyName(propEnum, nameChoice);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private static Map cccHack = new HashMap();
        private static Set cccExtras = new HashSet();
        static {
            for (int i = 0; i <= 255; ++i) {
                String alias = UCharacter.getPropertyValueName(UProperty.CANONICAL_COMBINING_CLASS, i,
                        UProperty.NameChoice.LONG);
                String numStr = String.valueOf(i);
                if (alias != null) {
                    cccHack.put(alias, numStr);
                } else {
                    cccHack.put(numStr, numStr);
                    cccExtras.add(numStr);
                }
            }
        }

        public List _getAvailableValues(List result) {
            if (result == null)
                result = new ArrayList();
            if (propEnum == UProperty.AGE) {
                addAllUnique(getAges(), result);
                return result;

            }
            if (propEnum < UProperty.INT_LIMIT) {
                if (Binary_Extras.isInRange(propEnum)) {
                    propEnum = UProperty.BINARY_START; // HACK
                }
                int start = UCharacter.getIntPropertyMinValue(propEnum);
                int end = UCharacter.getIntPropertyMaxValue(propEnum);
                for (int i = start; i <= end; ++i) {
                    String alias = getFixedValueAlias(null, i, UProperty.NameChoice.LONG);
                    String alias2 = getFixedValueAlias(null, i, UProperty.NameChoice.SHORT);
                    if (alias == null) {
                        alias = alias2;
                        if (alias == null && isCombiningClassProperty()) {
                            alias = String.valueOf(i);
                        }
                    }
                    // System.out.println(propertyAlias + "\t" + i + ":\t" + alias);
                    addUnique(alias, result);
                }
            } else if (propEnum >= UProperty.DOUBLE_START && propEnum < UProperty.DOUBLE_LIMIT) {
                UnicodeMap map = getUnicodeMap();
                Collection values = map.values();
                addAllUnique(values, result);
            } else {
                String alias = getFixedValueAlias(null, -1, UProperty.NameChoice.LONG);
                addUnique(alias, result);
            }
            return result;
        }

        static String[] AGES = null;

        private String[] getAges() {
            if (AGES == null) {
                Set ages = new TreeSet();
                for (int i = 0; i < 0x10FFFF; ++i) {
                    ages.add(getAge(i));
                }
                AGES = (String[]) ages.toArray(new String[ages.size()]);
            }
            return AGES;
        }

        public List _getValueAliases(String valueAlias, List result) {
            if (result == null)
                result = new ArrayList();
            if (propEnum == UProperty.AGE) {
                addUnique(valueAlias, result);
                return result;
            }
            if (isCombiningClassProperty()) {
                addUnique(cccHack.get(valueAlias), result); // add number
            }
            int type = getType();
            if (type == UnicodeProperty.NUMERIC || type == EXTENDED_NUMERIC) {
                addUnique(valueAlias, result);
                if (valueAlias.endsWith(".0")) {
                    addUnique(valueAlias.substring(0, valueAlias.length() - 2), result);
                }
            } else {
                for (int nameChoice = UProperty.NameChoice.SHORT;; ++nameChoice) {
                    try {
                        addUnique(getFixedValueAlias(valueAlias, -1, nameChoice), result);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
            return result;
        }

        /* (non-Javadoc)
         * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyType()
         */
        private int internalGetPropertyType(int prop) {
            switch (prop) {
            case UProperty.AGE:
            case UProperty.BLOCK:
            case UProperty.SCRIPT:
                return UnicodeProperty.CATALOG;
            case UProperty.ISO_COMMENT:
            case UProperty.NAME:
            case UProperty.UNICODE_1_NAME:
                return UnicodeProperty.MISC;
            case UProperty.BIDI_MIRRORING_GLYPH:
            case UProperty.CASE_FOLDING:
            case UProperty.LOWERCASE_MAPPING:
            case UProperty.SIMPLE_CASE_FOLDING:
            case UProperty.SIMPLE_LOWERCASE_MAPPING:
            case UProperty.SIMPLE_TITLECASE_MAPPING:
            case UProperty.SIMPLE_UPPERCASE_MAPPING:
            case UProperty.TITLECASE_MAPPING:
            case UProperty.UPPERCASE_MAPPING:
                return UnicodeProperty.EXTENDED_STRING;
            }
            if (prop < UProperty.BINARY_START)
                return UnicodeProperty.UNKNOWN;
            if (prop < UProperty.BINARY_LIMIT)
                return UnicodeProperty.BINARY;
            if (prop < UProperty.INT_START)
                return UnicodeProperty.EXTENDED_BINARY;
            if (prop < UProperty.INT_LIMIT)
                return UnicodeProperty.ENUMERATED;
            if (prop < UProperty.DOUBLE_START)
                return UnicodeProperty.EXTENDED_ENUMERATED;
            if (prop < UProperty.DOUBLE_LIMIT)
                return UnicodeProperty.NUMERIC;
            if (prop < UProperty.STRING_START)
                return UnicodeProperty.EXTENDED_NUMERIC;
            if (prop < UProperty.STRING_LIMIT)
                return UnicodeProperty.STRING;
            return UnicodeProperty.EXTENDED_STRING;
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.icu.dev.test.util.UnicodeProperty#getVersion()
         */
        public String _getVersion() {
            return VersionInfo.ICU_VERSION.toString();
        }
    }

  /*{
            matchIterator = new UnicodeSetIterator(
                new UnicodeSet("[^[:Cn:]-[:Default_Ignorable_Code_Point:]]"));
        }*/



    /*
     * Other Missing Functions:
            Expands_On_NFC
            Expands_On_NFD
            Expands_On_NFKC
            Expands_On_NFKD
            Composition_Exclusion
            Decomposition_Mapping
            FC_NFKC_Closure
            ISO_Comment
            NFC_Quick_Check
            NFD_Quick_Check
            NFKC_Quick_Check
            NFKD_Quick_Check
            Special_Case_Condition
            Unicode_Radical_Stroke
     */

    static final Names Binary_Extras = new Names(UProperty.BINARY_LIMIT,
          new String[] {
          "isNFC", "isNFD", "isNFKC", "isNFKD",
          "isLowercase", "isUppercase", "isTitlecase", "isCasefolded", "isCased",
    });

//    static final Names String_Extras = new Names(UProperty.STRING_LIMIT,
//          new String[] {
//          "toNFC", "toNFD", "toNFKC", "toNKFD",
//    });

    static final int
        isNFC = UProperty.BINARY_LIMIT,
        isNFD = UProperty.BINARY_LIMIT+1,
        isNFKC = UProperty.BINARY_LIMIT+2,
        isNFKD = UProperty.BINARY_LIMIT+3,
        isLowercase = UProperty.BINARY_LIMIT+4,
        isUppercase = UProperty.BINARY_LIMIT+5,
        isTitlecase = UProperty.BINARY_LIMIT+6,
        isCasefolded = UProperty.BINARY_LIMIT+7,
        isCased = UProperty.BINARY_LIMIT+8,
        BINARY_LIMIT = UProperty.BINARY_LIMIT+9

//        NFC  = UProperty.STRING_LIMIT,
//        NFD  = UProperty.STRING_LIMIT+1,
//        NFKC = UProperty.STRING_LIMIT+2,
//        NFKD = UProperty.STRING_LIMIT+3
        ;

    private ICUPropertyFactory() {
        Collection c = getInternalAvailablePropertyAliases(new ArrayList());
        Iterator it = c.iterator();
        while (it.hasNext()) {
            add(getInternalProperty((String) it.next()));
        }
    }

    private static ICUPropertyFactory singleton = null;

    public static synchronized ICUPropertyFactory make() {
        if (singleton != null)
            return singleton;
        singleton = new ICUPropertyFactory();
        return singleton;
    }

    public List getInternalAvailablePropertyAliases(List result) {
        int[][] ranges = {
                {UProperty.BINARY_START,    UProperty.BINARY_LIMIT},
                {UProperty.INT_START,       UProperty.INT_LIMIT},
                {UProperty.DOUBLE_START,    UProperty.DOUBLE_LIMIT},
                {UProperty.STRING_START,    UProperty.STRING_LIMIT},
        };
        for (int i = 0; i < ranges.length; ++i) {
            for (int j = ranges[i][0]; j < ranges[i][1]; ++j) {
                String alias = UCharacter.getPropertyName(j, UProperty.NameChoice.LONG);
                UnicodeProperty.addUnique(alias, result);
                if (!result.contains(alias))
                    result.add(alias);
            }
        }
        // result.addAll(String_Extras.getNames());
        result.addAll(Binary_Extras.getNames());
        return result;
    }

    public UnicodeProperty getInternalProperty(String propertyAlias) {
        int propEnum;
        main: {
            int possibleItem = Binary_Extras.get(propertyAlias);
            if (possibleItem >= 0) {
                propEnum = possibleItem;
                break main;
            }
            // possibleItem = String_Extras.get(propertyAlias);
            // if (possibleItem >= 0) {
            // propEnum = possibleItem;
            // break main;
            // }
            propEnum = UCharacter.getPropertyEnum(propertyAlias);
        }
        return new ICUProperty(propertyAlias, propEnum);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getProperty(java.lang.String)
     */
    // TODO file bug on getPropertyValueName for Canonical_Combining_Class
    public static class Names {
        private String[] names;
        private int base;

        public Names(int base, String[] names) {
            this.base = base;
            this.names = names;
        }

        public int get(String name) {
            for (int i = 0; i < names.length; ++i) {
                if (name.equalsIgnoreCase(names[i]))
                    return base + i;
            }
            return -1;
        }

        public String get(int number) {
            number -= base;
            if (number < 0 || names.length <= number)
                return null;
            return names[number];
        }

        public boolean isInRange(int number) {
            number -= base;
            return (0 <= number && number < names.length);
        }

        public List getNames() {
            return Arrays.asList(names);
        }
    }
}
