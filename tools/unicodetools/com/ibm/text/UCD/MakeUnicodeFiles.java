package com.ibm.text.UCD;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.UnicodeDataFile;

public class MakeUnicodeFiles {
    
    static boolean DEBUG = true;
    
    public static void main() throws IOException {
        generateFile("Scripts","z");
    }
    
    static class OrderedMap {
         HashMap map = new HashMap();
         ArrayList keys = new ArrayList();
         void put(Object o, Object t) {
             map.put(o,t);
             keys.add(o);
         }
         List keyset() {
             return keys;
         }
     }
    
     static class PrintStyle {
         boolean longForm = false;
         boolean noLabel = false;
         boolean makeUppercase = false;
         boolean makeFirstLetterLowercase = false;
         String skipValue = null;
         String skipUnassigned = null;
         boolean orderByRangeStart = false;
         boolean valueList = false;
        
         PrintStyle setLongForm(boolean value) {
             longForm = value;
             return this;
         }        
         PrintStyle setSkipUnassigned(String value) {
             skipUnassigned = value;
             return this;
         }        
         PrintStyle setNoLabel(boolean value) {
             noLabel = value;
             return this;
         }        
         PrintStyle setMakeUppercase(boolean value) {
             makeUppercase = value;
             return this;
         }        
         PrintStyle setMakeFirstLetterLowercase(boolean value) {
             makeFirstLetterLowercase = value;
             return this;
         }        
         PrintStyle setSkipValue(String value) {
             skipValue = value;
             return this;
         }        
         PrintStyle setOrderByRangeStart(boolean value) {
             orderByRangeStart = value;
             return this;
         }        
         PrintStyle setValueList(boolean value) {
             valueList = value;
             return this;
         }        
     }
     static PrintStyle DEFAULT_PRINT_STYLE = new PrintStyle();
     static Comparator skeletonComparator = new UnicodeProperty.SkeletonComparator();
     static Map printStyles = new TreeMap(/*skeletonComparator*/);
     static {
         printStyles.put("Script", new PrintStyle().setLongForm(true)
            .setMakeUppercase(true).setSkipUnassigned("Common"));
         printStyles.put("Age", new PrintStyle().setNoLabel(true));
         printStyles.put("Numeric_Type", new PrintStyle().setLongForm(true)
             .setMakeFirstLetterLowercase(true).setSkipUnassigned("none"));
         printStyles.put("General_Category", new PrintStyle().setNoLabel(true)
             //.setSkipUnassigned(true)
         );
         printStyles.put("Line_Break", new PrintStyle().setSkipUnassigned("Unknown"));
         printStyles.put("Joining_Type", new PrintStyle().setSkipValue("Non_Joining"));
         printStyles.put("Joining_Group", new PrintStyle().setSkipValue("No_Joining_Group")
         .setMakeUppercase(true));
         printStyles.put("East_Asian_Width", new PrintStyle().setSkipUnassigned("Neutral"));
         printStyles.put("Decomposition_Type", new PrintStyle().setLongForm(true)
             .setSkipValue("None").setMakeFirstLetterLowercase(true));
         printStyles.put("Bidi_Class", new PrintStyle().setSkipUnassigned("Left_To_Right"));
         printStyles.put("Block", new PrintStyle().setNoLabel(true)
            .setValueList(true));
         printStyles.put("Age", new PrintStyle().setSkipValue("unassigned"));
         printStyles.put("Canonical_Combining_Class", new PrintStyle().setSkipValue("0"));
         printStyles.put("Hangul_Syllable_Type", new PrintStyle().setSkipValue("NA"));
         
     }
     //PropertyAliases
     //PropertyValueAliases
     //CompositionExclusions
     //SpecialCasing
     //NormalizationTest
     //add("CaseFolding", new String[] {"CaseFolding"});
     static Map contents = new TreeMap();
     static void add(String name, String[] properties) {
         contents.put(name, properties);
     }
     static {
         add("Blocks", new String[] {"Block"});
         add("DerivedAge", new String[] {"Age"});
         add("Scripts", new String[] {"Script"});
         add("HangulSyllableType", new String[] {"HangulSyllableType"});
         if (false) add("DerivedNormalizationProps", new String[] {
             "FNC", "Full_Composition_Exclusion", 
             "NFD_QuickCheck", "NFC_QuickCheck", "NFKD_QuickCheck", "NFKC_QuickCheck", 
             "Expands_On_NFD", "Expands_On_NFC", "Expands_On_NFKD", "Expands_On_NFKC"});
        
         add("DerivedBidiClass", new String[] {"BidiClass"});
         add("DerivedBinaryProperties", new String[] {"BidiMirrored"});
         add("DerivedCombiningClass", new String[] {"CanonicalCombiningClass"});
         add("DerivedDecompositionType", new String[] {"DecompositionType"});
         add("DerivedEastAsianWidth", new String[] {"EastAsianWidth"});
         add("DerivedGeneralCategory", new String[] {"GeneralCategory"});
         add("DerivedJoiningGroup", new String[] {"JoiningGroup"});
         add("DerivedJoiningType", new String[] {"JoiningType"});
         add("DerivedLineBreak", new String[] {"LineBreak"});
         add("DerivedNumericType", new String[] {"NumericType"});
         add("DerivedNumericValues", new String[] {"NumericValue"});
         add("PropList", new String[] {
             "White_Space", "Bidi_Control", "Join_Control",
             "Dash", "Hyphen", "Quotation_Mark",
             "Terminal_Punctuation", "Other_Math", 
             "Hex_Digit", "ASCII_Hex_Digit",
             "Other_Alphabetic",
             "Ideographic",
             "Diacritic", "Extender", 
             "Other_Lowercase", "Other_Uppercase",
             "Noncharacter_Code_Point",
             "Other_Grapheme_Extend",
             "Grapheme_Link",
             "IDS_Binary_Operator", "IDS_Trinary_Operator",
             "Radical", "Unified_Ideograph", 
             "Other_Default_Ignorable_Code_Point",
             "Deprecated", "Soft_Dotted",
             "Logical_Order_Exception",
             "Other_ID_Start"
         });
         add("DerivedCoreProperties", new String[] {
             "Math", "Alphabetic", "Lowercase", "Uppercase",
             "ID_Start", "ID_Continue", 
             "XID_Start", "XID_Continue", 
             "Default_Ignorable_Code_Point",
             "Grapheme_Extend", "Grapheme_Base"
         });
     }
    
     public static void generateFile(String atOrAfter, String atOrBefore) throws IOException {
         Iterator it = contents.keySet().iterator();
         while (it.hasNext()) {
             String propname = (String) it.next();
             if (propname.compareTo(atOrAfter) < 0) continue;
             if (propname.compareTo(atOrBefore) > 0) continue;
             generateFile(propname);
         }
     }
    
     public static void generateFile(String filename) throws IOException {
         String[] propList = (String[]) contents.get(filename);
         UnicodeDataFile udf = UnicodeDataFile.openAndWriteHeader("DerivedDataTest/", filename);
         PrintWriter pw = udf.out; // bf2.openUTF8Writer(UCD_Types.GEN_DIR, "Test" + filename + ".txt");
         UnicodeProperty.Factory toolFactory 
           = ToolUnicodePropertySource.make(Default.ucdVersion());
         BagFormatter bf2 = new BagFormatter(toolFactory);
         UnicodeSet unassigned = toolFactory.getSet("gc=cn")
             .addAll(toolFactory.getSet("gc=cs"));
         //System.out.println(unassigned.toPattern(true));
         // .removeAll(toolFactory.getSet("noncharactercodepoint=true"));
         String separator = bf2.getLineSeparator() 
         + "# ================================================"
         + bf2.getLineSeparator() + bf2.getLineSeparator();

         for (int i = 0; i < propList.length; ++i) {
             UnicodeProperty prop = toolFactory.getProperty(propList[i]);
             System.out.println(prop.getName());
             pw.print(separator);
             PrintStyle ps = (PrintStyle) printStyles.get(prop.getName());
             if (ps == null) {
                 ps = DEFAULT_PRINT_STYLE;
                 System.out.println("Using default style!");
             }           
             if (ps.noLabel) bf2.setLabelSource(null);
            
             if (ps.valueList) {
                 bf2.setValueSource(new UnicodeProperty.FilteredProperty(prop, new ReplaceFilter()))
                 .setNameSource(null)
                 .setShowCount(false)
                 .showSetNames(pw,new UnicodeSet(0,0x10FFFF));                 
             } else if (prop.getType() <= prop.EXTENDED_BINARY) {
                 UnicodeSet s = prop.getSet("True");
                 bf2.setValueSource(prop.getName());
                 bf2.showSetNames(pw, s);
             } else {
                 bf2.setValueSource(prop);
                 Collection aliases = prop.getAvailableValueAliases();
                 if (ps.orderByRangeStart) {
                     System.out.println("Reordering");
                     TreeSet temp2 = new TreeSet(new RangeStartComparator(prop));
                     temp2.addAll(aliases);
                     aliases = temp2;
                 } 
                 Iterator it = aliases.iterator();
                 while (it.hasNext()) {
                     String value = (String)it.next();
                     UnicodeSet s = prop.getSet(value);

                     System.out.println(value + "\t" + prop.getShortestValueAlias(value) + "\t" + ps.skipValue);
                     System.out.println(s.toPattern(true));
                    
                     if (skeletonComparator.compare(value, ps.skipValue) == 0) continue;
                     if (skeletonComparator.compare(value, ps.skipUnassigned) == 0) {
                         s.removeAll(unassigned);
                     } 
                    
                     if (s.size() == 0) continue;
                     //if (unassigned.containsAll(s)) continue; // skip if all unassigned
                     //if (s.contains(0xD0000)) continue; // skip unassigned
                     pw.print(separator);
                     if (!ps.longForm) value = prop.getShortestValueAlias(value);
                     if (ps.makeUppercase) value = value.toUpperCase(Locale.ENGLISH);
                     if (ps.makeFirstLetterLowercase) {
                         // NOTE: this is ok since we are only working in ASCII
                         value = value.substring(0,1).toLowerCase(Locale.ENGLISH)
                             + value.substring(1);
                     } 
                     bf2.setValueSource(value);
                     bf2.showSetNames(pw, s);
                }
             }
         }
         udf.close();
     }
    static class RangeStartComparator implements Comparator {
        UnicodeProperty prop;
        CompareProperties.UnicodeSetComparator comp = new CompareProperties.UnicodeSetComparator();
        RangeStartComparator(UnicodeProperty prop) {
            this.prop = prop;
        }
        public int compare(Object o1, Object o2) {
            UnicodeSet s1 = prop.getSet((String)o1);
            UnicodeSet s2 = prop.getSet((String)o2);
            if (true) System.out.println("comparing " + o1 + ", " + o2
                + s1.toPattern(true) + "?" + s2.toPattern(true)
                + ", " + comp.compare(s1, s2));
            return comp.compare(s1, s2);
        }
        
    }
    
    public static class ReplaceFilter extends UnicodeProperty.StringFilter {
        public String remap(String original) {
            return original.replace('_',' ');
        }
    }
    


}