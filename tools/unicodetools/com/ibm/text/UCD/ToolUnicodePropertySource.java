package com.ibm.text.UCD;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.text.utility.Utility;

public class ToolUnicodePropertySource extends UnicodeProperty.Factory {
    private UCD ucd;
    private static boolean needAgeCache = true;
    private static UCD[] ucdCache = new UCD[UCD_Types.LIMIT_AGE];
    
    private static HashMap cache = new HashMap();
    
    public static synchronized ToolUnicodePropertySource make(String version) {
        ToolUnicodePropertySource result = (ToolUnicodePropertySource)cache.get(version);
        if (result != null) return result;
        result = new ToolUnicodePropertySource(version);
        cache.put(version, result);
        return result; 
    }
    
    private ToolUnicodePropertySource(String version) {
        ucd = UCD.make(version);
        TreeSet names = new TreeSet();
        UnifiedProperty.getAvailablePropertiesAliases(names,ucd);
        Iterator it = names.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            add(new ToolUnicodeProperty(name));
        }
        add(new UnicodeProperty.SimpleProperty() {
            {set("Name", "na", UnicodeProperty.STRING, "<string>");}
            public String getValue(int codepoint) {
                if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getName(codepoint);
            }
        });
        add(new UnicodeProperty.SimpleProperty() {
            {set("Block", "blk", UnicodeProperty.ENUMERATED,
                ucd.getBlockNames(null));}
            public String getValue(int codepoint) {
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBlock(codepoint);
            }
        });
        add(new UnicodeProperty.SimpleProperty() {
            {set("Bidi_Mirroring_Glyph", "bmg", UnicodeProperty.STRING, "<string>");}
            public String getValue(int codepoint) {
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBidiMirror(codepoint);
            }
        });
        add(new UnicodeProperty.SimpleProperty() {
            {set("Case_Folding", "cf", UnicodeProperty.STRING, "<string>");}
            public String getValue(int codepoint) {
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getCase(codepoint,UCD_Types.FULL,UCD_Types.FOLD);
            }
        });
        add(new UnicodeProperty.SimpleProperty() {
            {set("Numeric_Value", "nv", UnicodeProperty.NUMERIC, "<number>");}
            public String getValue(int codepoint) {
                double num = ucd.getNumericValue(codepoint);
                if (Double.isNaN(num)) return null;
                return Double.toString(num);
            }
        });
    }
    /*
           "Bidi_Mirroring_Glyph", "Block", "Case_Folding", "Case_Sensitive", "ISO_Comment",
           "Lowercase_Mapping", "Name", "Numeric_Value", "Simple_Case_Folding", 
           "Simple_Lowercase_Mapping", "Simple_Titlecase_Mapping", "Simple_Uppercase_Mapping", 
           "Titlecase_Mapping", "Unicode_1_Name", "Uppercase_Mapping", "isCased", "isCasefolded", 
           "isLowercase", "isNFC", "isNFD", "isNFKC", "isNFKD", "isTitlecase", "isUppercase",
           "toNFC", "toNFD", "toNFKC", "toNKFD"
   });
   */
    
    /*
    private class NameProperty extends UnicodeProperty.SimpleProperty {
        {set("Name", "na", "<string>", UnicodeProperty.STRING);}
        public String getPropertyValue(int codepoint) {
            if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
            return ucd.getName(codepoint);
        }
    }
    */
    static final int ODD_BALLS = (1<<UCD_Types.Cn) | (1<<UCD_Types.Co) | (1<<UCD_Types.Cs) | (1<<UCD.Cc);
    
    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyAliases(java.util.Collection)
     */
    private class ToolUnicodeProperty extends UnicodeProperty {
        com.ibm.text.UCD.UCDProperty up;
        int propMask;
    
        static final int EXTRA_START = 0x10000;
    
        private ToolUnicodeProperty(String propertyAlias) {
            propMask = UnifiedProperty.getPropmask(propertyAlias, ucd);
            up = UnifiedProperty.make(propMask, ucd);
            setType(getPropertyTypeInternal());
            setName(propertyAlias);
        }

        public Collection getAvailableValueAliases(Collection result) {
            int type = getType() & ~EXTENDED_BIT;
            if (type == STRING) result.add("<string>");
            else if (type == NUMERIC) result.add("<string>");
            else if (type == BINARY) {
                result.add("True");
                result.add("False");
            } else if (type == ENUMERATED) {
                byte style = UCD_Types.LONG;
                int prop = propMask>>8;
                String temp = null;
                boolean titlecase = false;
                for (int i = 0; i < 256; ++i) {
                    try {
                        switch (prop) {
                        case UCD_Types.CATEGORY>>8: temp = (ucd.getCategoryID_fromIndex((byte)i, style)); break;
                        case UCD_Types.COMBINING_CLASS>>8: temp = (ucd.getCombiningClassID_fromIndex((byte)i, style)); break;
                        case UCD_Types.BIDI_CLASS>>8: temp = (ucd.getBidiClassID_fromIndex((byte)i, style)); break;
                        case UCD_Types.DECOMPOSITION_TYPE>>8: temp = (ucd.getDecompositionTypeID_fromIndex((byte)i, style)); break;
                        case UCD_Types.NUMERIC_TYPE>>8: temp = (ucd.getNumericTypeID_fromIndex((byte)i, style));
                            titlecase = true;
                            break;
                        case UCD_Types.EAST_ASIAN_WIDTH>>8: temp = (ucd.getEastAsianWidthID_fromIndex((byte)i, style)); break;
                        case UCD_Types.LINE_BREAK>>8:  temp = (ucd.getLineBreakID_fromIndex((byte)i, style)); break;
                        case UCD_Types.JOINING_TYPE>>8: temp = (ucd.getJoiningTypeID_fromIndex((byte)i, style)); break;
                        case UCD_Types.JOINING_GROUP>>8: temp = (ucd.getJoiningGroupID_fromIndex((byte)i, style)); break;
                        case UCD_Types.SCRIPT>>8: temp = (ucd.getScriptID_fromIndex((byte)i, style)); titlecase = true;
                            if ("<unused>".equals(temp)) continue;
                            if (temp != null) temp = UCharacter.toTitleCase(Locale.ENGLISH,temp,null);
                            break;
                        case UCD_Types.AGE>>8: temp = (ucd.getAgeID_fromIndex((byte)i, style)); break;
                        case UCD_Types.HANGUL_SYLLABLE_TYPE>>8: 
                            temp = (ucd.getHangulSyllableTypeID_fromIndex((byte)i,style)); break;
                        default: throw new IllegalArgumentException("Internal Error: " + prop);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        continue;
                    }
                    if (temp != null && temp.length() != 0) result.add(Utility.getUnskeleton(temp, titlecase));
                }
                if (prop == (UCD_Types.DECOMPOSITION_TYPE>>8)) result.add("none");
                if (prop == (UCD_Types.JOINING_TYPE>>8)) result.add("Non_Joining");
                if (prop == (UCD_Types.NUMERIC_TYPE>>8)) result.add("None");
            }
            return result;
        }

        public Collection getAliases(Collection result) {
             String longName = up.getName(UCD_Types.LONG);
             addUnique(Utility.getUnskeleton(longName, true), result);
             String shortName = up.getName(UCD_Types.SHORT);
             addUnique(Utility.getUnskeleton(shortName, false), result);
             return result;
        }
        
        public Collection getValueAliases(String valueAlias, Collection result) {
            // TODO Auto-generated method stub
            return result;
        }

        public String getValue(int codepoint) {
            byte style = UCD_Types.LONG;
            String temp = null;
            boolean titlecase = false;
            switch (propMask>>8) {
            case UCD_Types.CATEGORY>>8: temp = (ucd.getCategoryID_fromIndex(ucd.getCategory(codepoint), style)); break;
            case UCD_Types.COMBINING_CLASS>>8: temp = (ucd.getCombiningClassID_fromIndex(ucd.getCombiningClass(codepoint), style)); 
                if (temp.startsWith("Fixed_")) temp = temp.substring(6);
                break;
            case UCD_Types.BIDI_CLASS>>8: temp =  (ucd.getBidiClassID_fromIndex(ucd.getBidiClass(codepoint), style)); break;
            case UCD_Types.DECOMPOSITION_TYPE>>8: temp =  (ucd.getDecompositionTypeID_fromIndex(ucd.getDecompositionType(codepoint), style));
                if (temp == null || temp.length() == 0) temp = "none";
                break;
            case UCD_Types.NUMERIC_TYPE>>8: temp =  (ucd.getNumericTypeID_fromIndex(ucd.getNumericType(codepoint), style));
                titlecase = true;
                if (temp == null || temp.length() == 0) temp = "None";      
                break;
            case UCD_Types.EAST_ASIAN_WIDTH>>8: temp =  (ucd.getEastAsianWidthID_fromIndex(ucd.getEastAsianWidth(codepoint), style)); break;
            case UCD_Types.LINE_BREAK>>8:  temp =  (ucd.getLineBreakID_fromIndex(ucd.getLineBreak(codepoint), style)); break;
            case UCD_Types.JOINING_TYPE>>8: temp =  (ucd.getJoiningTypeID_fromIndex(ucd.getJoiningType(codepoint), style)); 
                if (temp == null || temp.length() == 0) temp = "Non_Joining";      
                break;
            case UCD_Types.JOINING_GROUP>>8: temp =  (ucd.getJoiningGroupID_fromIndex(ucd.getJoiningGroup(codepoint), style)); break;
            case UCD_Types.SCRIPT>>8: temp =  (ucd.getScriptID_fromIndex(ucd.getScript(codepoint), style));
                if (temp != null) temp = UCharacter.toTitleCase(Locale.ENGLISH,temp,null);
                titlecase = true;
                break;
            case UCD_Types.AGE>>8: temp = getAge(codepoint); break;
            case UCD_Types.HANGUL_SYLLABLE_TYPE>>8: 
               temp =  (ucd.getHangulSyllableTypeID_fromIndex(ucd.getHangulSyllableType(codepoint),style)); break;
            }
            if (temp != null) return Utility.getUnskeleton(temp,titlecase);
            if (getType() == BINARY) {
                return up.hasValue(codepoint) ? "True" : "False";
            }
            return "<unknown>";
        }
    
        public String getAge(int codePoint) {
            if (needAgeCache) {
                for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
                    ucdCache[i] = UCD.make(UCD_Names.AGE_VERSIONS[i]);
                }
                needAgeCache = false;
            }
            for (int i = UCD_Types.AGE11; i < UCD_Types.LIMIT_AGE; ++i) {
                if (ucdCache[i].isAllocated(codePoint)) return UCD_Names.AGE[i];
            }
            return UCD_Names.AGE[UCD_Types.UNKNOWN];
        }
        
        /* (non-Javadoc)
         * @see com.ibm.icu.dev.test.util.UnicodePropertySource#getPropertyType()
         */
        private int getPropertyTypeInternal() {
            int result = 0;
            String name = up.getName(UCD_Types.LONG);
            if ("Age".equals(name)) return STRING;
            switch (up.getValueType()) {
                case UCD_Types.NUMERIC_PROP: result = NUMERIC; break;
                case UCD_Types.STRING_PROP: result = STRING; break;
                case UCD_Types.MISC_PROP: result = STRING; break;
                case UCD_Types.CATALOG_PROP: result = ENUMERATED; break;
                case UCD_Types.FLATTENED_BINARY_PROP:
                case UCD_Types.ENUMERATED_PROP: result = ENUMERATED; break;
                case UCD_Types.BINARY_PROP: result = BINARY; break;
                case UCD_Types.UNKNOWN_PROP:
                default:
                    throw new IllegalArgumentException("Type: UNKNOWN_PROP");
            }
            if (!up.isStandard()) result |= EXTENDED_BIT;
            return result;
        }

    }
}
