package com.ibm.text.UCD;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Tabber;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.UnicodeDataFile;
import com.ibm.text.utility.Utility;
import com.ibm.icu.text.Collator;

public class MakeUnicodeFiles {
    
    static boolean DEBUG = true;
    
    public static void main() throws IOException {
        generateFile("*");
    }

      static String[] FILE_OPTIONS = {
         "Script            nameStyle=none makeUppercase skipUnassigned=Common hackValues",
         "Age               nameStyle=none noLabel skipValue=unassigned",
         "Numeric_Type      nameStyle=none makeFirstLetterLowercase skipValue=None",
         "General_Category  nameStyle=none valueStyle=short noLabel",
         "Line_Break        nameStyle=none valueStyle=short skipUnassigned=Unknown",
         "Joining_Type      nameStyle=none valueStyle=short skipValue=Non_Joining",
         "Joining_Group     nameStyle=none skipValue=No_Joining_Group makeUppercase",
         "East_Asian_Width      nameStyle=none valueStyle=short skipUnassigned=Neutral",
         "Decomposition_Type    nameStyle=none skipValue=None makeFirstLetterLowercase hackValues",
         "Bidi_Class        nameStyle=none valueStyle=short skipUnassigned=Left_To_Right",
         "Block             nameStyle=none noLabel valueList",
         "Canonical_Combining_Class     nameStyle=none valueStyle=short skipUnassigned=Not_Reordered longValueHeading=ccc",
         "Hangul_Syllable_Type  nameStyle=none valueStyle=short skipValue=Not_Applicable",
         "NFD_Quick_Check   nameStyle=short valueStyle=short skipValue=Yes",
         "NFC_Quick_Check   nameStyle=short valueStyle=short skipValue=Yes",
         "NFKC_Quick_Check  nameStyle=short valueStyle=short skipValue=Yes",
         "NFKD_Quick_Check  nameStyle=short valueStyle=short skipValue=Yes",        
         "FC_NFKC_Closure   nameStyle=short"
     };
     
    static String[] hackNameList = {
       "noBreak", "Arabic_Presentation_Forms-A", "Arabic_Presentation_Forms-B", 
       "CJK_Symbols_and_Punctuation", "Combining_Diacritical_Marks_for_Symbols",
       "Enclosed_CJK_Letters_and_Months", "Greek_and_Coptic",
       "Halfwidth_and_Fullwidth_Forms", "Latin-1_Supplement", "Latin_Extended-A",
       "Latin_Extended-B", "Miscellaneous_Mathematical_Symbols-A",
       "Miscellaneous_Mathematical_Symbols-B", "Miscellaneous_Symbols_and_Arrows",
       "Superscripts_and_Subscripts", "Supplemental_Arrows-A", "Supplemental_Arrows-B",
       "Supplementary_Private_Use_Area-A", "Supplementary_Private_Use_Area-B",
       "Canadian-Aboriginal", "Old-Italic"
    };

    static class PrintStyle {
         static PrintStyle DEFAULT_PRINT_STYLE = new PrintStyle();
         static Map PRINT_STYLE_MAP = new TreeMap(UnicodeProperty.PROPERTY_COMPARATOR);
         boolean noLabel = false;
         boolean makeUppercase = false;
         boolean makeFirstLetterLowercase = false;
         boolean orderByRangeStart = false;
         boolean interleaveValues = false;
        boolean hackValues = false;
        String nameStyle = "none";
        String valueStyle = "long";
        String skipValue = null;
        String skipUnassigned = null;
        String longValueHeading = null;
        
        static void add(String options) {
            PrintStyle result = new PrintStyle();
            PRINT_STYLE_MAP.put(result.parse(options), result);
        }
        static PrintStyle get(String propname) {
            PrintStyle result = (PrintStyle) PRINT_STYLE_MAP.get(propname);
            if (result != null) return result;
            if (DEBUG) System.out.println("Using default style!");
            return DEFAULT_PRINT_STYLE;
        }
        String parse(String options) {
            options = options.replace('\t', ' ');
            String[] pieces = Utility.split(options, ' ');
            for (int i = 1; i < pieces.length; ++i) {
                String piece = pieces[i];
                // binary
                if (piece.equals("noLabel")) noLabel = true;
                else if (piece.equals("makeUppercase")) makeUppercase = true;
                else if (piece.equals("makeFirstLetterLowercase")) makeFirstLetterLowercase = true;
                else if (piece.equals("orderByRangeStart")) orderByRangeStart = true;
                else if (piece.equals("valueList")) interleaveValues = true;
                else if (piece.equals("hackValues")) hackValues = true;
                // with parameter
                else if (piece.startsWith("valueStyle=")) valueStyle = afterEquals(piece);
                else if (piece.startsWith("nameStyle=")) nameStyle = afterEquals(piece);
                else if (piece.startsWith("longValueHeading=")) longValueHeading = afterEquals(piece);
                else if (piece.startsWith("skipValue=")) skipValue = afterEquals(piece);
                else if (piece.startsWith("skipUnassigned=")) skipUnassigned = afterEquals(piece);
                else if (piece.length() != 0) {
                    throw new IllegalArgumentException("Illegal PrintStyle Parameter: " + piece + " in " + pieces[0]);
                }
            }
            if (DEBUG && options.indexOf('=') >= 0) {
                System.out.println(pieces[0]);
                if (longValueHeading != null)System.out.println(" name " + longValueHeading);
                if (nameStyle != null) System.out.println(" nameStyle " + nameStyle);
                if (longValueHeading != null) System.out.println(" longValueHeading " + longValueHeading);
                if (skipValue != null) System.out.println(" skipValue " + skipValue);
                if (skipUnassigned != null) System.out.println(" skipUnassigned " + skipUnassigned);
            }
            return pieces[0];
        }
        String afterEquals(String source) {
            return source.substring(source.indexOf('=')+1);
        }
     }
     static {
         for (int i = 0; i < FILE_OPTIONS.length; ++i) {
             PrintStyle.add(FILE_OPTIONS[i]);
         }
     }

    static Map hackMap = new HashMap();
    static {
         for (int i = 0; i < hackNameList.length; ++i) {
             String item = hackNameList[i];
             String regularItem = UnicodeProperty.regularize(item,true);
             hackMap.put(regularItem, item);
         }
    }
    static UnicodeProperty.MapFilter hackMapFilter = new UnicodeProperty.MapFilter(hackMap);

    static class ValueComments {
        TreeMap propertyToValueToComments = new TreeMap();
        ValueComments add(String property, String value, String comments) {
             TreeMap valueToComments = (TreeMap) propertyToValueToComments.get(property);
             if (valueToComments == null) {
                 valueToComments = new TreeMap();
                 propertyToValueToComments.put(property, valueToComments);
             }
             valueToComments.put(value, comments);
             return this;
         }
         String get(String property, String value) {
             TreeMap valueToComments = (TreeMap) propertyToValueToComments.get(property);
             if (valueToComments != null) return (String) valueToComments.get(value);
             return null;
         }
     }
     static ValueComments valueComments = new ValueComments();
     static {
         for (int i = 0; i < UCD_Names.UNIFIED_PROPERTIES.length; ++i) {
             String name = Utility.getUnskeleton(UCD_Names.UNIFIED_PROPERTIES[i], false);
             valueComments.add(name, "*", "# " + UCD_Names.UNIFIED_PROPERTY_HEADERS[i]);
         }
         // HACK
         valueComments.add("Bidi_Mirroring", "*", "# " + UCD_Names.UNIFIED_PROPERTY_HEADERS[9]);
        try {
            BufferedReader br = Utility.openReadFile("MakeUnicodeFiles.txt", Utility.UTF8);
            String key = null;
            String value = "";
            while (true) {
                String line = br.readLine();
                if (line == null) break;
                if (!line.startsWith("#")) {
                    if (key != null) {// store
                        String[] pieces = Utility.split(key, '=');
                        if (pieces.length == 1) {
                            valueComments.add(pieces[0].trim(), "*", value); 
                        } else {
                            valueComments.add(pieces[0].trim(), pieces[1].trim(), value); 
                        }
                        value = "";
                    }
                    key = line;
                } else {
                    value += line + "\n";
                }
            }
            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new IllegalArgumentException("File missing");
        }
    }

         

     //CompositionExclusions
     //SpecialCasing
     //NormalizationTest
     //add("CaseFolding", new String[] {"CaseFolding"});
     static Map contents = new TreeMap();
     static void add(String name, String[] properties) {
         contents.put(name, properties);
     }
     static {
         add("PropertyValueAliases", null);
         add("PropertyAliases", null);
         add("SpecialCasing", null);
         add("NormalizationTest", null);
         add("StandardizedVariants", null);
         add("CaseFolding", null);
         add("DerivedAge", new String[] {"Age"});
         add("Scripts", new String[] {"Script"});
         add("HangulSyllableType", new String[] {"HangulSyllableType"});
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
         add("DerivedNormalizationProps", new String[] {
             "FC_NFKC_Closure", 
             "Full_Composition_Exclusion", 
             "NFD_QuickCheck", "NFC_QuickCheck", "NFKD_QuickCheck", "NFKC_QuickCheck", 
             "Expands_On_NFD", "Expands_On_NFC", "Expands_On_NFKD", "Expands_On_NFKC"
            });
     }
    
    public static void generateFile(String atOrAfter, String atOrBefore) throws IOException {
         Iterator it = contents.keySet().iterator();
         while (it.hasNext()) {
             String propname = (String) it.next();
             if (propname.compareToIgnoreCase(atOrAfter) < 0) continue;
             if (propname.compareToIgnoreCase(atOrBefore) > 0) continue;
             generateFile(propname);
         }
     }
     
    public static void generateFile(String filename) throws IOException {
        if (filename.equals("*")) {
            generateFile("", "\uFFFD");
        } else if (filename.endsWith("Aliases")) {
            if (filename.endsWith("ValueAliases")) generateValueAliasFile(filename);
            else generateAliasFile(filename);
        } else if (filename.equals("NormalizationTest")) {
            GenerateData.writeNormalizerTestSuite("DerivedData/", "NormalizationTest");
        } else if (filename.equals("CaseFolding")) {
            GenerateCaseFolding.makeCaseFold(false);
        } else if (filename.equals("SpecialCasing")) {
            GenerateCaseFolding.generateSpecialCasing(false);
        } else if (filename.equals("StandardizedVariants")) {
            GenerateStandardizedVariants.generate();
        } else {
            generatePropertyFile(filename);
        }
    }
    
    static final String SEPARATOR = "# ================================================";
    
    public static void generateAliasFile(String filename) throws IOException {
        UnicodeDataFile udf = UnicodeDataFile.openAndWriteHeader("DerivedDataTest/", filename);
        PrintWriter pw = udf.out;
        UnicodeProperty.Factory ups 
                  = ToolUnicodePropertySource.make(Default.ucdVersion());
        TreeSet sortedSet = new TreeSet(CASELESS_COMPARATOR);
        BagFormatter bf = new BagFormatter();
        Tabber.MonoTabber mt = new Tabber.MonoTabber()
        .add(10,Tabber.LEFT);
        int count = 0;
        
        for (int i = UnicodeProperty.LIMIT_TYPE - 1; i >= UnicodeProperty.BINARY; --i) {
            if ((i & UnicodeProperty.EXTENDED_MASK) != 0) continue;
            List list = ups.getAvailableNames(1<<i);
            //if (list.size() == 0) continue;
            sortedSet.clear();
            StringBuffer buffer = new StringBuffer();
            for (Iterator it = list.iterator(); it.hasNext();) {
                String propAlias = (String)it.next();
                
                UnicodeProperty up = ups.getProperty(propAlias);
                List aliases = up.getNameAliases();
                if (aliases.size() == 1) {
                    sortedSet.add(mt.process(aliases.get(0) + "\t; " + aliases.get(0)));
                } else {
                    buffer.setLength(0);
                    boolean isFirst = true;
                    for (Iterator it2 = aliases.iterator(); it2.hasNext();) {
                        if (isFirst) isFirst = false;
                        else buffer.append("\t; ");
                        buffer.append(it2.next());
                    }
                    if (aliases.size() == 1) {
                        // repeat 
                        buffer.append("\t; ").append(aliases.get(0));
                    }
                    sortedSet.add(mt.process(buffer.toString()));
                }
            }
            if (i == UnicodeProperty.STRING) {
                for (int j = 0; j < specialString.length; ++j) {
                    sortedSet.add(mt.process(specialString[j]));
                }
            } else if (i == UnicodeProperty.MISC) {
                for (int j = 0; j < specialMisc.length; ++j) {
                    sortedSet.add(mt.process(specialMisc[j]));
                }
            }
            pw.println();
            pw.println(SEPARATOR);
            pw.println("# " + UnicodeProperty.getTypeName(i) + " Properties");
            pw.println(SEPARATOR);
            for (Iterator it = sortedSet.iterator(); it.hasNext();) {
                pw.println(it.next());
                count++;
            }
        }
        pw.println();
        pw.println(SEPARATOR);
        pw.println("#Total:    " + count);
        pw.println();
        udf.close();       
    }
    
    static String[] specialMisc = {
        "isc\t; ISO_Comment",
        "na1\t; Unicode_1_Name",
        "URS\t; Unicode_Radical_Stroke"};
        
    static String[] specialString = {
        "dm\t; Decomposition_Mapping",
        "lc\t; Lowercase_Mapping",
        "scc\t; Special_Case_Condition",
        "sfc\t; Simple_Case_Folding",
        "slc\t; Simple_Lowercase_Mapping",
        "stc\t; Simple_Titlecase_Mapping",
        "suc\t; Simple_Uppercase_Mapping",
        "tc\t; Titlecase_Mapping",
        "uc\t; Uppercase_Mapping"};

    static String[] specialGC = {
        "gc\t;\tC\t;\tOther\t# Cc | Cf | Cn | Co | Cs",
        "gc\t;\tL\t;\tLetter\t# Ll | Lm | Lo | Lt | Lu",
        "gc\t;\tLC\t;\tCased_Letter\t# Ll | Lt | Lu",
        "gc\t;\tM\t;\tMark\t# Mc | Me | Mn",
        "gc\t;\tN\t;\tNumber\t# Nd | Nl | No",
        "gc\t;\tP\t;\tPunctuation\t# Pc | Pd | Pe | Pf | Pi | Po | Ps",
        "gc\t;\tS\t;\tSymbol\t# Sc | Sk | Sm | So",
        "gc\t;\tZ\t;\tSeparator\t# Zl | Zp | Zs"};
        
    public static void generateValueAliasFile(String filename) throws IOException {
        UnicodeDataFile udf = UnicodeDataFile.openAndWriteHeader("DerivedDataTest/", filename);
        PrintWriter pw = udf.out;
        UnicodeProperty.Factory toolFactory 
          = ToolUnicodePropertySource.make(Default.ucdVersion());
        BagFormatter bf = new BagFormatter(toolFactory);
        StringBuffer buffer = new StringBuffer();
        Set sortedSet = new TreeSet(CASELESS_COMPARATOR);
        
        //gc ; C         ; Other                            # Cc | Cf | Cn | Co | Cs
                        // 123456789012345678901234567890123

        // sc ; Arab      ; Arabic
        Tabber.MonoTabber mt2 = new Tabber.MonoTabber()
        .add(3,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(10,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(33,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(33,Tabber.LEFT);
        
        // ccc; 216; ATAR ; Attached_Above_Right
        Tabber.MonoTabber mt3 = new Tabber.MonoTabber()
        .add(3,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(3,Tabber.RIGHT)
        .add(2,Tabber.LEFT) // ;
        .add(5,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(33,Tabber.LEFT)
        .add(2,Tabber.LEFT) // ;
        .add(33,Tabber.LEFT);

        for (Iterator it = toolFactory.getAvailableNames(UnicodeProperty.ENUMERATED_OR_CATALOG_MASK).iterator(); it.hasNext();) {
            String propName = (String) it.next();
            UnicodeProperty up = toolFactory.getProperty(propName);
            String shortProp = up.getFirstNameAlias();
            sortedSet.clear();
            
            for (Iterator it2 = up.getAvailableValues().iterator(); it2.hasNext();) {
                String value = (String) it2.next();
                List l = up.getValueAliases(value);
                System.out.println(value + "\t" + bf.join(l));
                
                // HACK
                Tabber mt = mt2;
                if (l.size() == 1) {
                    if (propName.equals("Canonical_Combining_Class")) continue;
                    if (propName.equals("Block") 
                      || propName.equals("Joining_Group")
                      //|| propName.equals("Numeric_Type")
                      || propName.equals("Age")) {
                        l.add(0, "n/a");
                    } else {
                        l.add(0, l.get(0)); // double up
                    } 
                } else if (l.size() > 2) {
                    mt = mt3;
                }
                if (UnicodeProperty.equalNames(value,"Cyrillic_Supplement")) {
                    l.add("Cyrillic_Supplementary");
                }

                buffer.setLength(0);                
                buffer.append(shortProp);
                for (Iterator it3 = l.iterator(); it3.hasNext();) {
                    buffer.append("\t; \t" + it3.next());
                }
                
                sortedSet.add(mt.process(buffer.toString()));
            }
            // HACK
            if (propName.equals("General_Category")) {
                for (int i = 0; i < specialGC.length; ++i) {
                    sortedSet.add(mt2.process(specialGC[i]));
                }
            }
            pw.println();
            for (Iterator it4 = sortedSet.iterator(); it4.hasNext();) {
                String line = (String) it4.next();
                pw.println(line);
            }
        }
        udf.close();
    }
    
    public static void generatePropertyFile(String filename) throws IOException {
         String[] propList = (String[]) contents.get(filename);
         UnicodeDataFile udf = UnicodeDataFile.openAndWriteHeader("DerivedDataTest/", filename);
         PrintWriter pw = udf.out; // bf2.openUTF8Writer(UCD_Types.GEN_DIR, "Test" + filename + ".txt");
         UnicodeProperty.Factory toolFactory 
           = ToolUnicodePropertySource.make(Default.ucdVersion());
         UnicodeSet unassigned = toolFactory.getSet("gc=cn")
             .addAll(toolFactory.getSet("gc=cs"));
         //System.out.println(unassigned.toPattern(true));
         // .removeAll(toolFactory.getSet("noncharactercodepoint=true"));
 
         for (int i = 0; i < propList.length; ++i) {
             BagFormatter bf = new BagFormatter(toolFactory);
             UnicodeProperty prop = toolFactory.getProperty(propList[i]);
             String name = prop.getName();
             System.out.println("Property: " + name + "; " + prop.getTypeName(prop.getType()));
             pw.println("\n" + SEPARATOR + "\n");
             String propComment = valueComments.get(name, "*");
             if (propComment != null) {
                 pw.print(propComment);
             }
             pw.println();
             PrintStyle ps = PrintStyle.get(name);
             
             if (!ps.interleaveValues && prop.isType(UnicodeProperty.BINARY_MASK)) {
                 if (DEBUG) System.out.println("Resetting Binary Values");
                 ps.skipValue = "False";
                 if (ps.nameStyle.equals("none")) ps.nameStyle = "long";
                 ps.valueStyle = "none";
             }

             if (ps.noLabel) bf.setLabelSource(null);
             if (ps.nameStyle.equals("none")) bf.setPropName(null);
             else if (ps.nameStyle.equals("short")) bf.setPropName(prop.getFirstNameAlias());
             else bf.setPropName(name);
            
             if (ps.interleaveValues) {
                writeInterleavedValues(pw, bf, prop);
             } else if (prop.isType(UnicodeProperty.STRING_OR_MISC_MASK)) {
                writeStringValues(pw, bf, prop);
             //} else if (prop.isType(UnicodeProperty.BINARY_MASK)) {
             //   writeBinaryValues(pw, bf, prop);
             } else {
                 writeEnumeratedValues(pw, bf, unassigned, prop, ps);
             }
             pw.println();
         }
         udf.close();
     }
    private static void writeEnumeratedValues(
            PrintWriter pw,
            BagFormatter bf,
            UnicodeSet unassigned,
            UnicodeProperty prop,
            PrintStyle ps) {
        if (DEBUG) System.out.println("Writing Enumerated Values: " + prop.getName());
        
         bf.setValueSource(new UnicodeProperty.FilteredProperty(prop, hackMapFilter));
         Collection aliases = prop.getAvailableValues();
         if (ps.orderByRangeStart) {
             System.out.println("Reordering");
             TreeSet temp2 = new TreeSet(new RangeStartComparator(prop));
             temp2.addAll(aliases);
             aliases = temp2;
         } 
         for (Iterator it = aliases.iterator(); it.hasNext();) {
             String value = (String)it.next();
             UnicodeSet s = prop.getSet(value);
             if (DEBUG) System.out.println("Getting value " + value);
             String valueComment = valueComments.get(prop.getName(), value);
        
            if (DEBUG) {
                System.out.println(value + "\t" + prop.getFirstValueAlias(value) + "\tskip:" + ps.skipValue); 
                System.out.println(s.toPattern(true));
            }
            
            int totalSize = s.size();
            if (s.size() == 0) continue;
            
            if (UnicodeProperty.compareNames(value, ps.skipValue) == 0) {
                System.out.println("Skipping: " + value);
                continue;
            } 
             
            if (UnicodeProperty.compareNames(value, ps.skipUnassigned) == 0) {
                System.out.println("Removing Unassigneds: " + value);
                s.removeAll(unassigned);
            }
             
            //if (s.size() == 0) continue;
             //if (unassigned.containsAll(s)) continue; // skip if all unassigned
             //if (s.contains(0xD0000)) continue; // skip unassigned
             pw.print("\n" + SEPARATOR + "\n\n");
             
             String displayValue = value;
             if (ps.valueStyle.equals("none")) {
                 displayValue = null;
             } else if (ps.valueStyle.equals("short")) {
                 displayValue = prop.getFirstValueAlias(displayValue);
                 if (DEBUG) System.out.println("Changing value " + displayValue);
             } 
             if (ps.makeUppercase && displayValue != null) {
                 displayValue = displayValue.toUpperCase(Locale.ENGLISH);
                 if (DEBUG) System.out.println("Changing value2 " + displayValue);
             } 
             if (ps.makeFirstLetterLowercase && displayValue != null) {
                 // NOTE: this is ok since we are only working in ASCII
                 displayValue = displayValue.substring(0,1).toLowerCase(Locale.ENGLISH)
                     + displayValue.substring(1);
                 if (DEBUG) System.out.println("Changing value2 " + displayValue);
             }
             if (DEBUG) System.out.println("Setting value " + displayValue);
             bf.setValueSource(displayValue);
             if (valueComment != null) {
                 pw.println(valueComment);
                 pw.println();
             }
             if (ps.longValueHeading != null) {
                 String headingValue = value;
                 if (ps.longValueHeading == "ccc") {
                     headingValue = Utility.replace(value, "_", "");
                     char c = headingValue.charAt(0);
                     if ('0' <= c && c <= '9') headingValue = "Other Combining Class";
                 }
                 pw.println("# " + headingValue);
                 pw.println();
             }
             if (s.size() != 0) bf.showSetNames(pw, s);
             if (s.size() != totalSize) {
                 pw.println();
                 pw.print("# Not Listed: " + totalSize);
             }
             pw.println();
        }
        
    }
    /*
    private static void writeBinaryValues(
        PrintWriter pw,
        BagFormatter bf,
        UnicodeProperty prop) {
        if (DEBUG) System.out.println("Writing Binary Values: " + prop.getName());
         UnicodeSet s = prop.getSet("True");
         bf.setValueSource(prop.getName());
         bf.showSetNames(pw, s);
    }
    */
    
    private static void writeInterleavedValues(
        PrintWriter pw,
        BagFormatter bf,
        UnicodeProperty prop) {
        if (DEBUG) System.out.println("Writing Interleaved Values: " + prop.getName());
         bf.setValueSource(new UnicodeProperty.FilteredProperty(prop, new RestoreSpacesFilter()))
         .setNameSource(null)
         .setShowCount(false)
         .showSetNames(pw,new UnicodeSet(0,0x10FFFF));                 
    }
     
    private static void writeStringValues(
            PrintWriter pw,
            BagFormatter bf,
            UnicodeProperty prop) {
        if (DEBUG) System.out.println("Writing String Values: " + prop.getName());
        bf.setValueSource(prop).setHexValue(true).setMergeRanges(false);
        bf.showSetNames(pw,new UnicodeSet(0,0x10FFFF));                 
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
    
    static class RestoreSpacesFilter extends UnicodeProperty.StringFilter {
        public String remap(String original) {
            // ok, because doesn't change length
            String mod = (String) hackMap.get(original);
            if (mod != null) original = mod;
            return original.replace('_',' ');
        }
    }
    
    static Comparator CASELESS_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            String s = o1.toString();
            String t = o2.toString();
            return s.compareToIgnoreCase(t);
        }
    };
}

    
/*
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
    */