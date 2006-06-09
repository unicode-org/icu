package com.ibm.text.UCD;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.Utility;

public class ToolUnicodePropertySource extends UnicodeProperty.Factory {
    static final boolean DEBUG = false;
    private UCD ucd;
    private Normalizer nfc, nfd, nfkd, nfkc;

    private static boolean needAgeCache = true;
    private static UCD[] ucdCache = new UCD[UCD_Types.LIMIT_AGE];
    
    private static HashMap factoryCache = new HashMap();
    
    public static synchronized ToolUnicodePropertySource make(String version) {
        ToolUnicodePropertySource result = (ToolUnicodePropertySource)factoryCache.get(version);
        if (result != null) return result;
        result = new ToolUnicodePropertySource(version);
        factoryCache.put(version, result);
        return result; 
    }
    
    private ToolUnicodePropertySource(String version) {
        ucd = UCD.make(version);
        nfc = new Normalizer(Normalizer.NFC, ucd.getVersion());
        nfd = new Normalizer(Normalizer.NFD, ucd.getVersion());
        nfkc = new Normalizer(Normalizer.NFKC, ucd.getVersion());
        nfkd = new Normalizer(Normalizer.NFKD, ucd.getVersion());

        version = ucd.getVersion(); // regularize

        // first the special cases
        if (DEBUG) System.out.println("Adding Simple Cases");

        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getName(codepoint);
            }
        }.setValues("<string>")
		.setMain("Name", "na", UnicodeProperty.MISC, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (DEBUG && codepoint == 0x1D100) {
                    System.out.println("here");
                }
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBlock(codepoint);
            }
            protected UnicodeMap _getUnicodeMap() {
                return ucd.blockData;
            }
        }.setValues(ucd.getBlockNames(null))
		.setMain("Block", "blk", UnicodeProperty.CATALOG, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getBidiMirror(codepoint);
            }
        }.setValues("<string>")
		.setMain("Bidi_Mirroring_Glyph", "bmg", UnicodeProperty.STRING, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                //if ((ODD_BALLS & ucd.getCategoryMask(codepoint)) != 0) return null;
                return ucd.getCase(codepoint,UCD_Types.FULL,UCD_Types.FOLD);
            }
        }.setValues("<string>")
		.setMain("Case_Folding", "cf", UnicodeProperty.STRING, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            NumberFormat nf = NumberFormat.getInstance();
            {
                nf.setGroupingUsed(false);
                nf.setMaximumFractionDigits(8);
                nf.setMinimumFractionDigits(1);
            }
            public String _getValue(int codepoint) {
                
                double num = ucd.getNumericValue(codepoint);
                if (Double.isNaN(num)) return null;
                return nf.format(num);
            }
        }.setMain("Numeric_Value", "nv", UnicodeProperty.NUMERIC, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int cp) {
                if (!ucd.isRepresented(cp)) return null;
                String b = nfkc.normalize(ucd.getCase(cp, UCD_Types.FULL, UCD_Types.FOLD));
                String c = nfkc.normalize(ucd.getCase(b, UCD_Types.FULL, UCD_Types.FOLD));
                if (c.equals(b)) return null;
                return c;
            }
            public int getMaxWidth(boolean isShort) {
                return 14;
            }
        }.setMain("FC_NFKC_Closure", "FC_NFKC", UnicodeProperty.STRING, version)
        //.addName("FNC")
        );

        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (!nfd.isNormalized(codepoint)) return "No";
                else if (nfd.isTrailing(codepoint)) throw new IllegalArgumentException("Internal Error!");
                else return "Yes";
            }
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases()
		.setMain("NFD_Quick_Check", "NFD_QC", UnicodeProperty.ENUMERATED, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (!nfc.isNormalized(codepoint)) return "No";
                else if (nfc.isTrailing(codepoint)) return "Maybe";
                else return "Yes";
            }
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases()
		.setMain("NFC_Quick_Check", "NFC_QC", UnicodeProperty.ENUMERATED, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (!nfkd.isNormalized(codepoint)) return "No";
                else if (nfkd.isTrailing(codepoint)) throw new IllegalArgumentException("Internal Error!");
                else return "Yes";
            }
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO, YES_NO).swapFirst2ValueAliases()
		.setMain("NFKD_Quick_Check", "NFKD_QC", UnicodeProperty.ENUMERATED, version));
        
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (!nfkc.isNormalized(codepoint)) return "No";
                else if (nfkc.isTrailing(codepoint)) return "Maybe";
                else return "Yes";
            }
            public int getMaxWidth(boolean isShort) {
                return 15;
            }
        }.setValues(LONG_YES_NO_MAYBE, YES_NO_MAYBE).swapFirst2ValueAliases()
		.setMain("NFKC_Quick_Check", "NFKC_QC", UnicodeProperty.ENUMERATED, version));



        
        /*
        add(new UnicodeProperty.SimpleProperty() {
            public String _getValue(int codepoint) {
                if (!nfx.isNormalized(codepoint)) return NO;
                else if (nfx.isTrailing(codepoint)) return MAYBE;
                else return "";
            }
        }.setMain("NFD_QuickCheck", "nv", UnicodeProperty.NUMERIC, version)
        .setValues("<number>"));
        */
        
        // Now the derived properties
        if (DEBUG) System.out.println("Derived Properties");
        for (int i = 0; i < DerivedProperty.DERIVED_PROPERTY_LIMIT; ++i) {
            UCDProperty prop = DerivedProperty.make(i);
            if (prop == null) continue;
            if (!prop.isStandard()) continue;
            String name = prop.getName();
            if (getProperty(name) != null) {
                if (DEBUG) System.out.println("Iterated Names: " + name + ", ALREADY PRESENT*");
                continue; // skip if already there
            }
            int type = prop.getValueType();
            if (i == UCD_Types.FC_NFKC_Closure) type = UnicodeProperty.STRING;
            else if (i == UCD_Types.FullCompExclusion) type = UnicodeProperty.BINARY;
            else type = remapUCDType(type);
            
            if (DEBUG) System.out.println(prop.getName());
            add(new UCDPropertyWrapper(prop,type,false));
        }

        // then the general stuff
        
        if (DEBUG) System.out.println("Other Properties");
        List names = new ArrayList();     
        UnifiedProperty.getAvailablePropertiesAliases(names,ucd);
        Iterator it = names.iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (getProperty(name) != null) {
                if (DEBUG) System.out.println("Iterated Names: " + name + ", ALREADY PRESENT");
                continue; // skip if already there
            }
            if (DEBUG) System.out.println("Iterated Names: " + name);
            add(new ToolUnicodeProperty(name));
        }

		add(new UnicodeProperty.UnicodeMapProperty() {
        	{
        		unicodeMap = new UnicodeMap();
        		unicodeMap.setErrorOnReset(true);
        		unicodeMap.put(0xD, "CR");
        		unicodeMap.put(0xA, "LF");
        		UnicodeProperty cat = getProperty("General_Category");
        		UnicodeSet temp = cat.getSet("Line_Separator")
				.addAll(cat.getSet("Paragraph_Separator"))
				.addAll(cat.getSet("Control"))
				.addAll(cat.getSet("Format"))
				.remove(0xD).remove(0xA).remove(0x200C).remove(0x200D);
        		unicodeMap.putAll(temp, "Control");
        		UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet("true");
        		unicodeMap.putAll(graphemeExtend,"Extend");
        		UnicodeProperty hangul = getProperty("Hangul_Syllable_Type");
        		unicodeMap.putAll(hangul.getSet("L"),"L");
        		unicodeMap.putAll(hangul.getSet("V"),"V");
        		unicodeMap.putAll(hangul.getSet("T"),"T");
        		unicodeMap.putAll(hangul.getSet("LV"),"LV");
        		unicodeMap.putAll(hangul.getSet("LVT"),"LVT");
        		unicodeMap.setMissing("Other");
        	}
        }.setMain("Grapheme_Cluster_Break", "GCB", UnicodeProperty.ENUMERATED, version)
		.addValueAliases(new String[][] {
				{"Control", "CN"},
				{"Extend", "EX"},
				{"Other", "XX"},
		}, true).swapFirst2ValueAliases());

        add(new UnicodeProperty.UnicodeMapProperty() {
        	{
        		unicodeMap = new UnicodeMap();
        		unicodeMap.setErrorOnReset(true);
        		UnicodeProperty cat = getProperty("General_Category");
        		unicodeMap.putAll(cat.getSet("Format").remove(0x200C).remove(0x200D), "Format");
        		UnicodeProperty script = getProperty("Script");
        		unicodeMap.putAll(script.getSet("Katakana")
        				.addAll(new UnicodeSet("[\u3031\u3032\u3033\u3034\u3035\u309B\u309C\u30A0\u30FC\uFF70\uFF9E\uFF9F]"))
						, "Katakana");
        		Object foo = unicodeMap.getSet("Katakana");
        		UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet("true");
        		UnicodeProperty lineBreak = getProperty("Line_Break");
        		unicodeMap.putAll(getProperty("Alphabetic").getSet("true")
        				.add(0x05F3)
						.removeAll(getProperty("Ideographic").getSet("true"))
						.removeAll(unicodeMap.getSet("Katakana"))
						//.removeAll(script.getSet("Thai"))
						//.removeAll(script.getSet("Lao"))
						.removeAll(lineBreak.getSet("SA"))
						.removeAll(script.getSet("Hiragana"))
						.removeAll(graphemeExtend),
						"ALetter");
        		unicodeMap.putAll(new UnicodeSet("[\\u0027\\u00B7\\u05F4\\u2019\\u2027\\u003A]")
								,"MidLetter");
        		unicodeMap.putAll(lineBreak.getSet("Infix_Numeric")
        				.remove(0x003A), "MidNum");
        		unicodeMap.putAll(lineBreak.getSet("Numeric"), "Numeric");
        		unicodeMap.putAll(cat.getSet("Connector_Punctuation").remove(0x30FB).remove(0xFF65), "ExtendNumLet");
        		unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none of the above touch it.
        		unicodeMap.setMissing("Other");
        	}
        }.setMain("Word_Break", "WB", UnicodeProperty.ENUMERATED, version)
		.addValueAliases(new String[][] {
				{"Format", "FO"},
				{"Katakana", "KA"},
				{"ALetter", "LE"},
				{"MidLetter", "ML"},
				{"MidNum", "MN"},
				{"Numeric", "NU"},
				{"ExtendNumLet", "EX"},
				{"Other", "XX"},
		}, true).swapFirst2ValueAliases());

        add(new UnicodeProperty.UnicodeMapProperty() {
        	{
        		unicodeMap = new UnicodeMap();
        		unicodeMap.setErrorOnReset(true);
        		unicodeMap.putAll(new UnicodeSet("[\\u000A\\u000D\\u0085\\u2028\\u2029]"), "Sep");
        		UnicodeProperty cat = getProperty("General_Category");
        		unicodeMap.putAll(cat.getSet("Format").remove(0x200C).remove(0x200D), "Format");
        		unicodeMap.putAll(getProperty("Whitespace").getSet("true")
        				.removeAll(unicodeMap.getSet("Sep"))
						.remove(0xA0), "Sp");
        		UnicodeSet graphemeExtend = getProperty("Grapheme_Extend").getSet("true");
        		unicodeMap.putAll(getProperty("Lowercase").getSet("true")
        				.removeAll(graphemeExtend), "Lower");
        		unicodeMap.putAll(getProperty("Uppercase").getSet("true")
        				.addAll(cat.getSet("Titlecase_Letter"))
						, "Upper");
        		UnicodeSet temp = getProperty("Alphabetic").getSet("true")
				.add(0xA0).add(0x5F3)
				.removeAll(unicodeMap.getSet("Lower"))
		        .removeAll(unicodeMap.getSet("Upper"))
				.removeAll(graphemeExtend);
        		unicodeMap.putAll(temp, "OLetter");
        		UnicodeProperty lineBreak = getProperty("Line_Break");
        		unicodeMap.putAll(lineBreak.getSet("Numeric"), "Numeric");
        		unicodeMap.put(0x002E, "ATerm");
        		unicodeMap.putAll(getProperty("STerm").getSet("true")
        				.removeAll(unicodeMap.getSet("ATerm")), "STerm");
        		unicodeMap.putAll(cat.getSet("Open_Punctuation")
        				.addAll(cat.getSet("Close_Punctuation"))
        				.addAll(lineBreak.getSet("Quotation"))
						.remove(0x05F3)
						.removeAll(unicodeMap.getSet("ATerm"))
						.removeAll(unicodeMap.getSet("STerm"))
						, "Close");
        		unicodeMap.putAll(graphemeExtend, "Other"); // to verify that none of the above touch it.
        		unicodeMap.setMissing("Other");
        	}
        }.setMain("Sentence_Break", "SB", UnicodeProperty.ENUMERATED, version)
		.addValueAliases(new String[][] {
				{"Sep", "SE"},
				{"Format", "FO"},
				{"Sp", "SP"},
				{"Lower", "LO"},
				{"Upper", "UP"},
				{"OLetter", "LE"},
				{"Numeric", "NU"},
				{"ATerm", "AT"},
				{"STerm", "ST"},
				{"Close", "CL"},
				{"Other", "XX"},
		}, false).swapFirst2ValueAliases());
    }
    
    static String[] YES_NO_MAYBE = {"N", "M", "Y"};
    static String[] LONG_YES_NO_MAYBE = {"No", "Maybe", "Yes"};

    static String[] YES_NO = {"N", "Y"};
    static String[] LONG_YES_NO = {"No", "Yes"};

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
    
    static class UCDPropertyWrapper extends UnicodeProperty {
        UCDProperty ucdProperty;
        boolean yes_no_maybe;
        
        UCDPropertyWrapper(UCDProperty ucdProperty, int type, boolean yes_no_maybe) {
            this.ucdProperty = ucdProperty;
            setType(type);
            String name = ucdProperty.getName(UCDProperty.LONG);
            if (name == null) ucdProperty.getName(UCDProperty.SHORT);
            setName(name);
            this.yes_no_maybe = yes_no_maybe;
        }
        protected String _getVersion() {
            return ucdProperty.getUCD().getVersion();
        }
        protected String _getValue(int codepoint) {
            return ucdProperty.getValue(codepoint, UCDProperty.LONG);
        }
        protected List _getNameAliases(List result) {
            addUnique(ucdProperty.getName(UCDProperty.SHORT), result);
            String name = getName();
            addUnique(name, result);
            if (name.equals("White_Space")) addUnique("space", result);
            return result;
        }
        protected List _getValueAliases(String valueAlias, List result) {
            if (isType(BINARY_MASK)) {
                if (valueAlias.equals("True")) addUnique("T", result);
                else if (valueAlias.equals("False")) addUnique("F", result);
                addUnique(valueAlias, result);                
            }
            if (yes_no_maybe) {
                if (valueAlias.equals("Yes")) addUnique("Y", result);
                else if (valueAlias.equals("No")) addUnique("N", result);
                else if (valueAlias.equals("Maybe")) addUnique("M", result);
                addUnique(valueAlias, result);                                
            }
            return result;
        }
        protected List _getAvailableValues(List result) {
            if (isType(BINARY_MASK)) {
                addUnique("True", result);
                addUnique("False", result);
            }
            if (yes_no_maybe) {
                addUnique("No",result);
                addUnique("Maybe",result);                                
                addUnique("Yes",result);
            }
            return result;
        }
    }
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
            if (up == null) throw new IllegalArgumentException("Not found: " + propertyAlias);
            if (propertyAlias.equals("Case_Fold_Turkish_I")) {
                System.out.println(propertyAlias + " " + getTypeName(getType()));
            }
            setType(getPropertyTypeInternal());
            setName(propertyAlias);
        }

        public List _getAvailableValues(List result) {
            if (result == null) result = new ArrayList();
            int type = getType() & CORE_MASK;
            if (type == STRING || type == MISC) result.add("<string>");
            else if (type == NUMERIC) result.add("<number>");
            else if (type == BINARY) {
                result.add("True");
                result.add("False");
            } else if (type == ENUMERATED || type == CATALOG) {
                byte style = UCD_Types.LONG;
                int prop = propMask>>8;
                String temp = null;
                boolean titlecase = false;
                for (int i = 0; i < 256; ++i) {
                    boolean check = false;
                      try {
                        switch (prop) {
                        case UCD_Types.CATEGORY>>8: temp = (ucd.getCategoryID_fromIndex((byte)i, style)); break;
                        case UCD_Types.COMBINING_CLASS>>8: temp = (ucd.getCombiningClassID_fromIndex((short)i, style)); break;
                        case UCD_Types.BIDI_CLASS>>8: temp = (ucd.getBidiClassID_fromIndex((byte)i, style)); break;
                        case UCD_Types.DECOMPOSITION_TYPE>>8: temp = (ucd.getDecompositionTypeID_fromIndex((byte)i, style)); 
                            //check = temp != null;
                            break;
                        case UCD_Types.NUMERIC_TYPE>>8: temp = (ucd.getNumericTypeID_fromIndex((byte)i, style));
                            titlecase = true;
                            break;
                        case UCD_Types.EAST_ASIAN_WIDTH>>8: temp = (ucd.getEastAsianWidthID_fromIndex((byte)i, style)); break;
                        case UCD_Types.LINE_BREAK>>8:  temp = (ucd.getLineBreakID_fromIndex((byte)i, style)); break;
                        case UCD_Types.JOINING_TYPE>>8: temp = (ucd.getJoiningTypeID_fromIndex((byte)i, style)); break;
                        case UCD_Types.JOINING_GROUP>>8: temp = (ucd.getJoiningGroupID_fromIndex((byte)i, style)); break;
                        case UCD_Types.SCRIPT>>8:
                        	temp = (ucd.getScriptID_fromIndex((byte)i, style)); titlecase = true;
                            if (UnicodeProperty.UNUSED.equals(temp)) continue;
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
                    if (check) System.out.println("Value: " + temp);
                    if (temp != null && temp.length() != 0 && !temp.equals(UNUSED)) {
                        result.add(Utility.getUnskeleton(temp, titlecase));
                    } 
                    if (check) System.out.println("Value2: " + temp);
                }
                //if (prop == (UCD_Types.DECOMPOSITION_TYPE>>8)) result.add("none");
                //if (prop == (UCD_Types.JOINING_TYPE>>8)) result.add("Non_Joining");
                //if (prop == (UCD_Types.NUMERIC_TYPE>>8)) result.add("None");
            }
            return result;
        }

        public List _getNameAliases(List result) {
            if (result == null) result = new ArrayList();
            addUnique(Utility.getUnskeleton(up.getName(UCD_Types.SHORT), false), result);
            String longName = up.getName(UCD_Types.LONG);
            addUnique(Utility.getUnskeleton(longName, true), result);
            // hack
            if (longName.equals("White_Space")) addUnique("space", result);
            return result;
        }
        
        public List _getValueAliases(String valueAlias, List result) {
            if (result == null) result = new ArrayList();
            int type = getType() & CORE_MASK;
            if (type == STRING || type == MISC || type == NUMERIC) {
            	UnicodeProperty.addUnique(valueAlias, result);
            	return result;
            } else if (type == BINARY) {
                UnicodeProperty.addUnique(valueAlias, result);
                return lookup(valueAlias, UCD_Names.YN_TABLE_LONG, UCD_Names.YN_TABLE, null, result);
            } else if (type == ENUMERATED || type == CATALOG) {
                byte style = UCD_Types.LONG;
                int prop = propMask>>8;
                boolean titlecase = false;
                for (int i = 0; i < 256; ++i) {
                    try {
                        switch (prop) {
                        case UCD_Types.CATEGORY>>8:
                            return lookup(valueAlias, UCD_Names.LONG_GENERAL_CATEGORY, UCD_Names.GENERAL_CATEGORY, UCD_Names.EXTRA_GENERAL_CATEGORY, result);
                        case UCD_Types.COMBINING_CLASS>>8:
                            addUnique(String.valueOf(0xFF&Utility.lookup(valueAlias, UCD_Names.LONG_COMBINING_CLASS, true)), result);
                            return lookup(valueAlias, UCD_Names.LONG_COMBINING_CLASS, UCD_Names.COMBINING_CLASS, null, result);
                        case UCD_Types.BIDI_CLASS>>8:
                            return lookup(valueAlias, UCD_Names.LONG_BIDI_CLASS, UCD_Names.BIDI_CLASS, null, result);
                        case UCD_Types.DECOMPOSITION_TYPE>>8:
                            return lookup(valueAlias, UCD_Names.LONG_DECOMPOSITION_TYPE, UCD_Names.DECOMPOSITION_TYPE, null, result);
                        case UCD_Types.NUMERIC_TYPE>>8:
                            return lookup(valueAlias, UCD_Names.LONG_NUMERIC_TYPE, UCD_Names.NUMERIC_TYPE, null, result);
                        case UCD_Types.EAST_ASIAN_WIDTH>>8:
                            return lookup(valueAlias, UCD_Names.LONG_EAST_ASIAN_WIDTH, UCD_Names.EAST_ASIAN_WIDTH, null, result);
                        case UCD_Types.LINE_BREAK>>8:
                            lookup(valueAlias, UCD_Names.LONG_LINE_BREAK, UCD_Names.LINE_BREAK, null, result);
                            if (valueAlias.equals("Inseparable")) addUnique("Inseperable", result);
                            // Inseparable; Inseperable
                            return result;
                        case UCD_Types.JOINING_TYPE>>8:
                            return lookup(valueAlias, UCD_Names.LONG_JOINING_TYPE, UCD_Names.JOINING_TYPE, null, result);
                        case UCD_Types.JOINING_GROUP>>8:
                            return lookup(valueAlias, UCD_Names.JOINING_GROUP, null, null, result);
                        case UCD_Types.SCRIPT>>8: 
                            return lookup(valueAlias, UCD_Names.LONG_SCRIPT, UCD_Names.SCRIPT, UCD_Names.EXTRA_SCRIPT, result);
                        case UCD_Types.AGE>>8:
                            return lookup(valueAlias, UCD_Names.AGE, null, null, result);
                        case UCD_Types.HANGUL_SYLLABLE_TYPE>>8: 
                            return lookup(valueAlias, UCD_Names.LONG_HANGUL_SYLLABLE_TYPE, UCD_Names.HANGUL_SYLLABLE_TYPE, null, result);
                        default: throw new IllegalArgumentException("Internal Error: " + prop);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        continue;
                    }
                }
            }
            throw new ArrayIndexOutOfBoundsException("not supported yet");
        }

        public String _getValue(int codepoint) {
            byte style = UCD_Types.LONG;
            String temp = null;
            boolean titlecase = false;
            switch (propMask>>8) {
            case UCD_Types.CATEGORY>>8: temp = (ucd.getCategoryID_fromIndex(ucd.getCategory(codepoint), style)); break;
            case UCD_Types.COMBINING_CLASS>>8: temp = (ucd.getCombiningClassID_fromIndex(ucd.getCombiningClass(codepoint), style)); 
                //if (temp.startsWith("Fixed_")) temp = temp.substring(6);
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
            if (isType(BINARY_MASK)) {
                return up.hasValue(codepoint) ? "True" : "False";
            }
            throw new IllegalArgumentException("Failed to find value for " + Utility.hex(codepoint));
        }
    
        public String getAge(int codePoint) {
            if (codePoint == 0xF0000) {
                System.out.println("debug point");
            }
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
            
            switch(propMask) {
                case UCD_Types.BINARY_PROPERTIES | UCD_Types.CaseFoldTurkishI:
                case UCD_Types.BINARY_PROPERTIES | UCD_Types.Non_break: 
                    return EXTENDED_BINARY;
            }
            
            switch(propMask>>8) {
                case UCD_Types.SCRIPT>>8: 
                case UCD_Types.AGE>>8: 
                    return CATALOG;
            }
            int mask = 0;
            if (!up.isStandard()) mask = EXTENDED_MASK;
            return remapUCDType(up.getValueType()) | mask;
        }

        public String _getVersion() {
            return up.ucd.getVersion();
        }

    }
    
    private int remapUCDType(int result) {
        switch (result) {
            case UCD_Types.NUMERIC_PROP: result = UnicodeProperty.NUMERIC; break;
            case UCD_Types.STRING_PROP: result = UnicodeProperty.STRING; break;
            case UCD_Types.MISC_PROP: result = UnicodeProperty.STRING; break;
            case UCD_Types.CATALOG_PROP: result = UnicodeProperty.ENUMERATED; break;
            case UCD_Types.FLATTENED_BINARY_PROP:
            case UCD_Types.ENUMERATED_PROP: result = UnicodeProperty.ENUMERATED; break;
            case UCD_Types.BINARY_PROP: result = UnicodeProperty.BINARY; break;
            case UCD_Types.UNKNOWN_PROP:
            default:
                result = UnicodeProperty.STRING;
                //throw new IllegalArgumentException("Type: UNKNOWN_PROP");
        }
        return result;
    }

    static List lookup(String valueAlias, String[] main, String[] aux, Map aux2, List result) {
        //System.out.println(valueAlias + "=>");
        //System.out.println("=>" + aux[pos]);
        if (aux != null) {
            int pos = 0xFF & Utility.lookup(valueAlias, main, true);
            UnicodeProperty.addUnique(aux[pos], result);
        }
        UnicodeProperty.addUnique(valueAlias, result);
        if (aux2 != null) {
        	String xtra = (String) aux2.get(valueAlias);
        	if (xtra != null) UnicodeProperty.addUnique(xtra, result);
        }
        return result;
    }

    /*
    static class DerivedPropertyWrapper extends UnicodeProperty {
        UCDProperty derivedProperty;
        UCD ucd;
        
        DerivedPropertyWrapper(int derivedPropertyID, UCD ucd) {
            this.ucd = ucd;
            derivedProperty = DerivedProperty.make(derivedPropertyID, ucd);
        }
        protected String _getVersion() {
            return ucd.getVersion();
        }

        protected String _getValue(int codepoint) {
            return derivedProperty.getValue(codepoint, UCD_Types.LONG);
        }
        protected List _getNameAliases(List result) {
            if (result != null) result = new ArrayList(1);
            addUnique(derivedProperty.getName(UCD_Types.SHORT), result);
            addUnique(derivedProperty.getName(UCD_Types.LONG), result);
            return null;
        }

        protected List _getValueAliases(String valueAlias, List result) {
            // TODO Auto-generated method stub
            return null;
        }
        protected List _getAvailableValues(List result) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    */
}
