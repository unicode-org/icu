
/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;


/**
 * Provides a general interface for Unicode Properties, and
 * extracting sets based on those values.
 * @author Davis
 */
public abstract class UnicodePropertySource implements Cloneable {
    
    protected String propertyAlias;
    protected int nameChoice = UProperty.NameChoice.LONG;
    protected StringFilter filter = new StringFilter();
    protected UnicodeSetIterator matchIterator = new UnicodeSetIterator(new UnicodeSet(0,0x10FFFF));
    
    abstract public String getPropertyValue(int codepoint);
    abstract public Set getAvailablePropertyAliases(Set result);
    abstract public Set getAvailablePropertyValueAliases(Set result);

    abstract public String getPropertyAlias(int nameChoice);
    abstract public String getPropertyValueAlias(String valueAlias, int nameChoice);
    
    /**
     * Subclasses should override
     */
    public Object clone() {
        try {
            UnicodePropertySource result = (UnicodePropertySource)super.clone();
            result.filter = (StringFilter)filter.clone();
            return result;             
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Should never happen.");
        }
    }
    
    public UnicodePropertySource setPropertyAlias(String propertyAlias) {
        this.propertyAlias = propertyAlias;
        return this;
    }
    
    public String getPropertyAlias() {
        return propertyAlias;
    }
    
    public static final boolean equals(int codepoint, String other) {
        if (other.length() == 1) {
            return codepoint == other.charAt(0);
        }
        return other.equals(UTF16.valueOf(codepoint));
    }
    
    public UnicodeSet getPropertySet(boolean charEqualsValue, UnicodeSet result){
        if (result == null) result = new UnicodeSet();
        matchIterator.reset();
        while (matchIterator.next()) {
            String value = filter.remap(getPropertyValue(matchIterator.codepoint));
            if (equals(matchIterator.codepoint, value) == charEqualsValue) {
                result.add(matchIterator.codepoint);
            }
        }
        return result;
    }

    public UnicodeSet getPropertySet(String propertyValue, UnicodeSet result){
        if (result == null) result = new UnicodeSet();
        matchIterator.reset();
        while (matchIterator.next()) {
            String value = filter.remap(getPropertyValue(matchIterator.codepoint));
            if (propertyValue.equals(value)) {
                result.add(matchIterator.codepoint);
            } 
        }
        return result;
    }

    public UnicodeSet getPropertySet(Matcher matcher, UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        matchIterator.reset();
        while (matchIterator.next()) {
            String value = filter.remap(getPropertyValue(matchIterator.codepoint));
            if (value == null)
                continue;
            if (matcher.matches(value)) {
                result.add(matchIterator.codepoint);
            }
        }
        return result;
    }
    
    public interface Matcher {
        public boolean matches(String value);
    }
    
    public int getNameChoice() {
        return nameChoice;
    }

    public UnicodePropertySource setNameChoice(int choice) {
        nameChoice = choice;
        return this;
    }
    
    public static class StringFilter implements Cloneable {
        public String remap(String original) {
            return original;
        }
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError("Should never happen.");
            }
        }
    }
    
    public static class MapFilter extends StringFilter {
        Map valueMap;
        public String remap(String original) {
            Object changed = valueMap.get(original);
            return changed == null ? original : (String) changed;
        }
        public Map getMap() {
            return valueMap;
        }

        public MapFilter setMap(Map map) {
            valueMap = map;
            return this;
        }
    }

    static public class ICU extends UnicodePropertySource {
        protected int propEnum = Integer.MIN_VALUE;
        {
            matchIterator = new UnicodeSetIterator(
                new UnicodeSet("[^[:Cn:]-[:Default_Ignorable_Code_Point:]]"));
        }
        
        public UnicodePropertySource setPropertyAlias(String propertyAlias) {
            super.setPropertyAlias(propertyAlias);
            int extraPosition = Extras.indexOf(propertyAlias);
            if (extraPosition >= 0) {
                propEnum = EXTRA_START + extraPosition;
            } else {
                propEnum = UCharacter.getPropertyEnum(propertyAlias);
            }
            return this;
        }

        public String getPropertyValue(int codePoint) {
            if (propEnum < UProperty.INT_LIMIT) {
                int enumValue = UCharacter.getIntPropertyValue(codePoint, propEnum);
                return UCharacter.getPropertyValueName(propEnum,enumValue, (int)nameChoice);
            } else if (propEnum < UProperty.DOUBLE_LIMIT) {
                return Double.toString(UCharacter.getUnicodeNumericValue(codePoint));
                // TODO: Fix HACK -- API deficient
            } else switch(propEnum) {
                case UProperty.AGE: return UCharacter.getAge(codePoint).toString();
                case UProperty.BIDI_MIRRORING_GLYPH: return UTF16.valueOf(UCharacter.getMirror(codePoint));
                case UProperty.CASE_FOLDING: return UCharacter.foldCase(UTF16.valueOf(codePoint),true);
                case UProperty.ISO_COMMENT: return UCharacter.getISOComment(codePoint);
                case UProperty.LOWERCASE_MAPPING: return UCharacter.toLowerCase(Locale.ENGLISH,UTF16.valueOf(codePoint));
                case UProperty.NAME: return UCharacter.getName(codePoint);
                case UProperty.SIMPLE_CASE_FOLDING: return UTF16.valueOf(UCharacter.foldCase(codePoint,true));
                case UProperty.SIMPLE_LOWERCASE_MAPPING: return UTF16.valueOf(UCharacter.toLowerCase(codePoint));
                case UProperty.SIMPLE_TITLECASE_MAPPING: return UTF16.valueOf(UCharacter.toTitleCase(codePoint));
                case UProperty.SIMPLE_UPPERCASE_MAPPING: return UTF16.valueOf(UCharacter.toUpperCase(codePoint));
                case UProperty.TITLECASE_MAPPING: return UCharacter.toTitleCase(Locale.ENGLISH,UTF16.valueOf(codePoint),null);
                case UProperty.UNICODE_1_NAME: return UCharacter.getName1_0(codePoint);
                case UProperty.UPPERCASE_MAPPING: return UCharacter.toUpperCase(Locale.ENGLISH,UTF16.valueOf(codePoint));
                case NFC: return Normalizer.normalize(codePoint, Normalizer.NFC);
                case NFD: return Normalizer.normalize(codePoint, Normalizer.NFD);
                case NFKC: return Normalizer.normalize(codePoint, Normalizer.NFKC);
                case NFKD: return Normalizer.normalize(codePoint, Normalizer.NFKD);
            }
            return null;
        }
        
        static final List Extras = Arrays.asList(new String[] {
            "NFC", "NFD", "NFKC", "NKFD"
        });
        
        static final int 
            NFC  = 0x8000,
            NFD  = 0x8001,
            NFKC = 0x8002,
            NFKD = 0x8003,
            EXTRA_START = NFC,
            EXTRA_LIMIT = NFKD+1;

        static final int[][] ranges = {
            {UProperty.BINARY_START,    UProperty.BINARY_LIMIT},
            {UProperty.INT_START,       UProperty.INT_LIMIT},
            {UProperty.DOUBLE_START,    UProperty.DOUBLE_LIMIT},
            {UProperty.STRING_START,    UProperty.STRING_LIMIT},
        };

        public Set getAvailablePropertyAliases(Set result) {
            for (int i = 0; i < ranges.length; ++i) {
                for (int j = ranges[i][0]; j < ranges[i][1]; ++j) {
                    result.add(UCharacter.getPropertyName(j, nameChoice));
                }
            }
            result.addAll(Extras);
            return result;
        }

        public Set getAvailablePropertyValueAliases(Set result) {
            if (propEnum < UProperty.INT_LIMIT) {
                int start = UCharacter.getIntPropertyMinValue(propEnum);
                int end = UCharacter.getIntPropertyMaxValue(propEnum);
                for (int i = start; i <= end; ++i) {
                    result.add(getFixedValueAlias(null, i,nameChoice));
                }
            } else {
                result.add(getFixedValueAlias(null, -1,nameChoice));
            }
            return result;
        }
        
        /**
         * @param valueAlias null if unused.
         * @param valueEnum -1 if unused
         * @param nameChoice
         * @return the alias
         */
        private String getFixedValueAlias(String valueAlias, int valueEnum, int nameChoice) {
            if (propEnum >= UProperty.STRING_START) {
                return "<string>";
            } else if (propEnum >= UProperty.DOUBLE_START) {
                return "<double>";
            }
            if (valueAlias != null && !valueAlias.equals("<integer>")) {
                valueEnum = UCharacter.getPropertyValueEnum(propEnum,valueAlias);
            }
            String result = fixedGetPropertyValueName(propEnum, valueEnum, nameChoice);
            if (result != null) return result;
            // try other namechoice
            result = fixedGetPropertyValueName(propEnum,valueEnum,
                nameChoice == UProperty.NameChoice.LONG ? UProperty.NameChoice.SHORT : UProperty.NameChoice.LONG);
            if (result != null) return result;
            return "<integer>";
        }

        private static String fixedGetPropertyValueName(int propEnum, int valueEnum, int nameChoice) {
            try {
                return UCharacter.getPropertyValueName(propEnum,valueEnum,nameChoice);
            } catch (Exception e) {
                return null;
            }
        }

        public String getPropertyAlias(int nameChoice) {
            if (propEnum < EXTRA_START) {
                return UCharacter.getPropertyName(propEnum, nameChoice);
            }
            return (String)Extras.get(propEnum-EXTRA_START);
        }

        public String getPropertyValueAlias(String valueAlias, int nameChoice) {
            return getFixedValueAlias(valueAlias, -1, nameChoice);
        }
    }
    // TODO file bug on getPropertyValueName for Canonical_Combining_Class

    public StringFilter getFilter() {
        return filter;
    }


    public UnicodePropertySource setFilter(StringFilter filter) {
        this.filter = filter;
        return this;
    }

    /**
     */
    static public void addAll(UnicodeSetIterator source, UnicodeSet result) {
        while (source.nextRange()) {
            if (source.codepoint == UnicodeSetIterator.IS_STRING) {
                result.add(source.string);
            } else {
                result.add(source.codepoint, source.codepointEnd);
            }
        }
    }
    
    public UnicodeSet getMatchSet(UnicodeSet result) {
        if (result == null) result = new UnicodeSet();
        addAll(matchIterator, result);
        return result;
    }

    /**
     * @param set
     */
    public void setMatchSet(UnicodeSet set) {
        matchIterator = new UnicodeSetIterator(set);
    }

}
