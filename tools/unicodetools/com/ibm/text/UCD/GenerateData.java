/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateData.java,v $
* $Date: 2001/12/06 00:05:53 $
* $Revision: 1.12 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.text.utility.*;
import com.ibm.text.UTF16;


public class GenerateData implements UCD_Types {
    
    static final boolean DEBUG = false;
    
    static final String HORIZONTAL_LINE = "# ================================================";

    //static UnifiedBinaryProperty ubp;
    
    static final String[] ALL_FILES = {
        // "CaseFolding",
        "CompositionExclusions",
        "DerivedAge",
        "DerivedBidiClass",
        "DerivedBinaryProperties",
        "DerivedCombiningClass",
        "DerivedCoreProperties",
        "DerivedDecompositionType",
        "DerivedEastAsianWidth",
        "DerivedGeneralCategory",
        "DerivedJoiningGroup",
        "DerivedJoiningType",
        "DerivedLineBreak",
        "DerivedNormalizationProperties",
        "DerivedNumericType",
        "DerivedNumericValues",
        "NormalizationTest",
        "PropertyAliases",
        "PropList",
        "Scripts",
        //"OtherDerivedProperties",
    };
        
    
    public static void main (String inVersion, String[] args) throws IOException {
        System.out.println("START");
        ucd = UCD.make(inVersion);
        //ubp = new UnifiedBinaryProperty(ucd);
        
        System.out.println("Loaded UCD " + ucd.getVersion() + " " + (new Date(ucd.getDate())));
        String version = ucd.getVersion();

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line
            long mask = 0;

            Utility.fixDot();
            System.out.println("Argument: " + args[i]);

            if (arg.equalsIgnoreCase("partition")) {
                partitionProperties();
            } else if (arg.equalsIgnoreCase("All")) {
                // Append all args at end
                String[] temp = new String[args.length + ALL_FILES.length];
                System.arraycopy(args, 0, temp, 0, args.length);
                System.arraycopy(ALL_FILES, 0, temp, args.length, ALL_FILES.length);
                args = temp;
            } else if (arg.equalsIgnoreCase("PropertyAliases")) {
                listProperties();                
            } else if (arg.equalsIgnoreCase("listAccents")) {
                listCombiningAccents();
                
            } else if (arg.equalsIgnoreCase("listGreekVowels")) {
                listGreekVowels();
                
            } else if (arg.equalsIgnoreCase("listKatakana")) {
                listKatakana();
                
            } else if (arg.equalsIgnoreCase("diff")) {
                listDifferences();
            } else if (arg.equalsIgnoreCase("DerivedBidiClass")) {
                generateVerticalSlice(BIDI_CLASS, BIDI_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedBidiClass");
                    
            } else if (arg.equalsIgnoreCase("DerivedEastAsianWidth")) {
                generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedEastAsianWidth" );
            } else if (arg.equalsIgnoreCase("DerivedGeneralCategory")) {
                generateVerticalSlice(CATEGORY, CATEGORY+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedGeneralCategory" );
            } else if (arg.equalsIgnoreCase("DerivedGeneralCategoryTEST")) {
                generateVerticalSlice(CATEGORY+29, CATEGORY+32, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedGeneralCategory" );
            } else if (arg.equalsIgnoreCase("DerivedCombiningClass")) {
                generateVerticalSlice(COMBINING_CLASS, COMBINING_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedCombiningClass" );
            } else if (arg.equalsIgnoreCase("DerivedDecompositionType")) {
                generateVerticalSlice(DECOMPOSITION_TYPE, DECOMPOSITION_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedDecompositionType" );
            } else if (arg.equalsIgnoreCase("DerivedNumericType")) {
                generateVerticalSlice(NUMERIC_TYPE, NUMERIC_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedNumericType" );
            } else if (arg.equalsIgnoreCase("DerivedEastAsianWidth")) {
                generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedEastAsianWidth" );
            } else if (arg.equalsIgnoreCase("DerivedJoiningType")) {
                generateVerticalSlice(JOINING_TYPE, JOINING_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedJoiningType" );
            } else if (arg.equalsIgnoreCase("DerivedJoiningGroup")) {
                generateVerticalSlice(JOINING_GROUP, JOINING_GROUP+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedJoiningGroup" );
            } else if (arg.equalsIgnoreCase("DerivedBinaryProperties")) {
                generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES+1, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedBinaryProperties" );
            } else if (arg.equalsIgnoreCase("DerivedNumericValues")) {
                generateVerticalSlice(LIMIT_ENUM, LIMIT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedNumericValues" );
                    
            } else if (arg.equalsIgnoreCase("DerivedNormalizationProperties")) {
                generateDerived(DERIVED_NORMALIZATION, true, HEADER_DERIVED, "DerivedData/", 
                    "DerivedNormalizationProperties" );
            } else if (arg.equalsIgnoreCase("DerivedCoreProperties")) {
                generateDerived(DERIVED_CORE, true, HEADER_DERIVED, "DerivedData/", "DerivedCoreProperties");
                
            } else if (arg.equalsIgnoreCase("OtherDerivedProperties")) {
                //mask = Utility.setBits(0, NFC_Leading, NFC_Resulting);
                generateDerived(ALL, false, HEADER_DERIVED, "OtherData/", "OtherDerivedProperties");
            /* 
            } else if (arg.equalsIgnoreCase("DerivedFullNormalization")) {
                mask = Utility.setBits(0, DerivedProperty.GenNFD, DerivedProperty.GenNFKC);
                generateDerived(mask, HEADER_DERIVED, "DerivedData/", "DerivedFullNormalization" );
            } else if (arg.equalsIgnoreCase("caseignorable")) {
                mask = Utility.setBits(0, DerivedProperty.Other_Case_Ignorable, DerivedProperty.Type_i);
                generateDerived(mask, HEADER_DERIVED, "OtherData/", "CaseIgnorable" );
            } else if (arg.equalsIgnoreCase("nfunsafestart")) {
                mask = Utility.setBits(0, NFD_UnsafeStart, NFKC_UnsafeStart);
                generateDerived(mask, HEADER_DERIVED, "OtherData/", "NFUnsafeStart");
            */
            } else if (arg.equalsIgnoreCase("DerivedAge")) {
                generateAge("DerivedData/", "DerivedAge");
                
            } else if (arg.equalsIgnoreCase("DerivedLineBreak")) {
                generateVerticalSlice(LINE_BREAK, LINE_BREAK+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedData/", "DerivedLineBreak" );
            } else if (arg.equalsIgnoreCase("Scripts")) {
                generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM, 
                        KEEP_SPECIAL, HEADER_SCRIPTS, "DerivedData/", "Scripts");
            } else if (arg.equalsIgnoreCase("PropList")) {
                generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + NEXT_ENUM,
                        KEEP_SPECIAL, HEADER_EXTEND, "DerivedData/", "PropList");
            } else if (arg.equalsIgnoreCase("AllBinary")) {
                generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES + NEXT_ENUM,
                        KEEP_SPECIAL, HEADER_EXTEND, "OtherDerived/", "AllBinary");
            } else if (arg.equalsIgnoreCase("NormalizationTest")) {
                writeNormalizerTestSuite("DerivedData/", "NormalizationTest");
            } else if (arg.equalsIgnoreCase("CompositionExclusions")) {
                generateCompExclusions(version);
            }else {
                System.out.println(" ! Unknown option -- must be one of the following (case-insensitive)");
                System.out.println(" ! generateCompExclusions,...");
            }


            //checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
            //checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");


                //generateDerived(Utility.setBits(0, DerivedProperty.PropMath, DerivedProperty.Mod_ID_Continue_NO_Cf),
                //    HEADER_DERIVED, "DerivedData/", "DerivedPropData2" );
            //generateVerticalSlice(SCRIPT, SCRIPT+1, KEEP_SPECIAL, "ScriptCommon" );
            //listStrings("LowerCase" , 0,0);
            //generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedData/", "DerivedPropData1" );

            // AGE stuff
            //UCD ucd = UCD.make();
            //System.out.println(ucd.getAgeID(0x61));
            //System.out.println(ucd.getAgeID(0x2FA1D));

            //
        }
        System.out.println("END");
    }

   static Normalizer nfkc = new Normalizer(Normalizer.NFKC);

    public static void checkHoffman(String test) {
        String result = nfkc.normalize(test);
        System.out.println(Utility.hex(test) + " => " + Utility.hex(result));
        System.out.println();
        show(test, 0);
        System.out.println();
        show(result, 0);
    }

    public static void show(String s, int indent) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            String cc = " " + ucd.getCombiningClass(cp);
            cc = Utility.repeat(" ", 4 - cc.length()) + cc;
            System.out.println(Utility.repeat(" ", indent) + ucd.getCode(cp) + cc + " " + ucd.getName(cp));
            String decomp = nfkc.normalize(cp);
            if (!decomp.equals(UTF32.valueOf32(cp))) {
                show(decomp, indent + 4);
            }
        }
    }


    static DateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.S' GMT'");

    static {
        myDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    //Remove "d1" from DerivedJoiningGroup-3.1.0d1.txt type names

    public static String fixFile(String s) {
        int len = s.length();
        if (!s.endsWith(".txt")) return s;
        if (s.charAt(len-6) != 'd') return s;
        char c = s.charAt(len-5);
        if (c != 'X' && (c < '0' || '9' < c)) return s;
        s = s.substring(0,len-6) + s.substring(len-4);
        System.out.println("Fixing File Name: " + s);
        return s;
    }

    static final int HEADER_EXTEND = 0, HEADER_DERIVED = 1, HEADER_SCRIPTS = 2;

    public static void doHeader(String fileName, PrintWriter output, int headerChoice) {
        output.println("# " + fileName);
        output.println("#");
        if (headerChoice == HEADER_SCRIPTS) {
            output.println("# For documentation, see UTR #24: Script Names");
            output.println("#   http://www.unicode.org/unicode/reports/tr24/");
        } else if (headerChoice == HEADER_EXTEND) {
            output.println("# Unicode Character Database: Extended Properties");
            output.println("# For documentation, see PropList.html");
        } else {
            output.println("# Unicode Character Database: Derived Property Data");
            output.println("# Generated algorithmically from the Unicode Character Database");
            output.println("# For documentation, see DerivedProperties.html");
        }
        output.println("# Date: " + myDateFormat.format(new Date()) + " [MD]");
        output.println("# Note: Unassigned and Noncharacter codepoints are omitted,");
        output.println("#       except when listing Noncharacter or Cn.");
        output.println(HORIZONTAL_LINE);
        output.println();
    }

    public static String getFileSuffix(boolean withDVersion) {
        return "-" + ucd.getVersion() + (withDVersion ? "d1.txt" : ".txt");
    }
    
    public static void generateDerived (byte type, boolean checkTypeAndStandard, int headerChoice, String directory, String fileName) throws IOException {
        PrintWriter output = Utility.openPrintWriter(directory + fileName + getFileSuffix(true));
        generateBat(directory, fileName, getFileSuffix(true));
        doHeader(fileName + getFileSuffix(false), output, headerChoice);
        for (int i = 0; i < DERIVED_PROPERTY_LIMIT; ++i) {
            UnicodeProperty up = DerivedProperty.make(i, ucd);
            boolean keepGoing = true;
            if (!up.isStandard()) keepGoing = false;
            if ((up.getType() & type) == 0) keepGoing = false;
            
            if (checkTypeAndStandard != keepGoing) continue;
            //if ((bitMask & (1L<<i)) == 0) continue;
            
            System.out.print('.');
            output.println(HORIZONTAL_LINE);
            output.println();
            new DerivedPropertyLister(ucd, i, output).print();
            output.flush();
        }
        output.close();
    }

    /*
    public static void listStrings(String file, int type, int subtype) throws IOException {
        ucd = UCD.make("3.1.0");
        UCD ucd30 = UCD.make("3.0.0");
        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + file));

        for (int i = 0; i < 0x10FFFF; ++i) {
            if ((i & 0xFFF) == 0) System.out.println("# " + i);
            if (!ucd.isRepresented(i)) continue;
            if (ucd30.isRepresented(i)) continue;
            String string = "";
            switch(type) {
                case 0: string = ucd.getSimpleLowercase(i);
            }
            if (UTF32.length32(string) == 1 && UTF32.char32At(string,0) == i) continue;
            output.println(Utility.hex(i) + "; C; " + Utility.hex(string) + "; # " + ucd.getName(i));
        }
        output.close();
    }
    */

    public static void generateCompExclusions(String version) throws IOException {
        PrintWriter output = Utility.openPrintWriter("DerivedData/CompositionExclusions" + getFileSuffix(true));
        generateBat("DerivedData/", "CompositionExclusions", getFileSuffix(true));
        
        output.println("# CompositionExclusions" + getFileSuffix(false));
        output.println("#");
        output.println("# Composition Exclusions");
        output.println("# This file lists the characters from the UAX #15 Composition Exclusion Table.");
        output.println("#");
        output.println("# For more information, see");
        output.println("# http://www.unicode.org/unicode/reports/tr15/#Primary Exclusion List Table");
        output.println("# Generated: " + new Date() + ", MD");
        output.println(HORIZONTAL_LINE);
        output.println();
        output.println("# (1) Script Specifics");
        output.println("# This list of characters cannot be derived from the UnicodeData file.");
        output.println(HORIZONTAL_LINE);
        output.println();
        
        new CompLister(output, ucd, 1).print();
        
        output.println(HORIZONTAL_LINE);
        output.println("# (2) Post Composition Version precomposed characters");
        output.println("# These characters can be derived from a post-3.0.0 UnicodeData file");
        output.println("# by comparison to the 3.0.0 UnicodeData file.");
        output.println(HORIZONTAL_LINE);
        output.println();
        
        new CompLister(output, ucd, 2).print();

        output.println(HORIZONTAL_LINE);
        output.println("# (3) Singleton Decompositions");
        output.println("# These characters can be derived from the UnicodeData file");
        output.println("# by including all characters whose canonical decomposition");
        output.println("# consists of a single character.");
        output.println("# These characters are simply quoted here for reference.");
        output.println(HORIZONTAL_LINE);
        output.println();

        new CompLister(output, ucd, 3).print();
        
        output.println(HORIZONTAL_LINE);
        output.println("# (4) Non-Starter Decompositions");
        output.println("# These characters can be derived from the UnicodeData file");
        output.println("# by including all characters whose canonical decomposition consists");
        output.println("# of a sequence of characters, the first of which has a non-zero");
        output.println("# combining class.");
        output.println("# These characters are simply quoted here for reference.");        
        output.println(HORIZONTAL_LINE);
        output.println();
        new CompLister(output, ucd, 4).print();
        
        output.close();
    }

    static class CompLister extends PropertyLister {
        UCD oldUCD;
        int type;

        public CompLister(PrintWriter output, UCD ucd, int type) {
            this.output = output;
            ucdData = ucd;
            oldUCD = UCD.make("3.0.1");
            // showOnConsole = true;
            alwaysBreaks = type <= 2; // CHANGE LATER
            commentOut = type > 2;
            this.type = type;
        }

        public String optionalComment(int cp) { return ""; }
        /*
        public String valueName(int cp) {
            return UTF32.length32(ucdData.getDecompositionMapping(cp)) + "";
        }
        */
        public byte status(int cp) {
            if (getType(cp) == type) return INCLUDE;
            return EXCLUDE;
        }
        
        public int getType(int cp) {
            if (!ucdData.isAssigned(cp)) return -1;
            if (ucdData.getDecompositionType(cp) != CANONICAL) return -1;
            
            if (oldUCD.getBinaryProperty(cp, CompositionExclusion)) return 1;
            if (cp == 0xFB1D) return 1; // special
            
            String decomp = ucdData.getDecompositionMapping(cp);
            int len = UTF32.length32(decomp);
            if (len == 1) return 3;
            int first = UTF32.char32At(decomp,0);
            if (ucdData.getCombiningClass(first) != 0) return 4;
            
            if (oldUCD.getDecompositionType(cp) == CANONICAL) return -1;
            if (ucdData.getDecompositionType(cp) == CANONICAL) return 2;
            
            return -1;
        }
    }

    public static void partitionProperties() throws IOException {

        // find properties

        int count = 0;
        int[] props = new int[500];
        for (int i = 1; i < LIMIT_ENUM; ++i) { //   || iType == SCRIPT
            int iType = i & 0xFF00;
            if (iType == JOINING_GROUP || iType == AGE || iType == COMBINING_CLASS) continue;
            if (UnifiedBinaryProperty.make(i, ucd) == null) continue;
            props[count++] = i;
        }
        System.out.println("props: " + count);

        BitSet probe = new BitSet();
        Map map = new HashMap();
        int total = 0;
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            Utility.dot(cp);
            int cat = ucd.getCategory(cp);
            if (cat == UNASSIGNED || cat == PRIVATE_USE || cat == SURROGATE) continue;
            if (!ucd.isAllocated(cp)) continue;

            for (int i = 0; i < count; ++i) {
                UnicodeProperty up = UnifiedBinaryProperty.make(props[i], ucd);
                boolean iProp = up.hasValue(cp);
                if (iProp) probe.set(i); else probe.clear(i);
            }

            ++total;
            if (!map.containsKey(probe)) {
                map.put(probe.clone(), UTF32.valueOf32(cp));
                Utility.fixDot();
                System.out.println("Set Size: " + map.size() + ", total: " + total + ", " + ucd.getCodeAndName(cp));
            }
        }

        Utility.fixDot();
        System.out.println("Set Size: " + map.size());
    }

    public static void listDifferences() throws IOException {

        PrintWriter output = Utility.openPrintWriter("PropertyDifferences.txt");

        for (int i = 1; i < LIMIT_ENUM; ++i) {
            int iType = i & 0xFF00;
            if (iType == JOINING_GROUP || iType == AGE || iType == COMBINING_CLASS || iType == SCRIPT) continue;
            UnicodeProperty upi = UnifiedBinaryProperty.make(i, ucd);
            if (upi == null) continue;
            
            String iNameShort = upi.getFullName(SHORT);
            String iNameLong = upi.getFullName(LONG);

            System.out.println();
            System.out.println();
            System.out.println(iNameLong);
            output.println("#" + iNameLong);

            int last = -1;
            for (int j = i+1; j < LIMIT_ENUM; ++j) {
                int jType = j & 0xFF00;
                if (jType == JOINING_GROUP || jType == AGE || jType == COMBINING_CLASS || jType == SCRIPT
                    || (jType == iType && jType != BINARY_PROPERTIES)) continue;
                UnicodeProperty upj = UnifiedBinaryProperty.make(j, ucd);
                if (upj == null) continue;

                if ((j >> 8) != last) {
                    last = j >> 8;
                    System.out.println();
                    System.out.print("\t" + UCD_Names.SHORT_UNIFIED_PROPERTIES[last]);
                    output.flush();
                    output.println("#\t" + UCD_Names.SHORT_UNIFIED_PROPERTIES[last]);
                } else {
                    System.out.print('.');
                }
                System.out.flush();

                int bothCount = 0, i_jPropCount = 0, j_iPropCount = 0, iCount = 0, jCount = 0;

                for (int cp = 0; cp <= 0x10FFFF; ++cp) {
                    int cat = ucd.getCategory(cp);
                    if (cat == UNASSIGNED || cat == PRIVATE_USE || cat == SURROGATE) continue;
                    if (!ucd.isAllocated(cp)) continue;

                    boolean iProp = upi.hasValue(cp);
                    boolean jProp = upj.hasValue(cp);

                    if (jProp) ++jCount;
                    if (iProp) {
                        ++iCount;
                        if (jProp) ++bothCount;
                        else ++i_jPropCount;
                    } else if (jProp) ++j_iPropCount;
                }
                if (iCount == 0 || jCount == 0) continue;

                String jNameShort = upj.getFullName(SHORT);
                //String jNameLong = ubp.getFullID(j, LONG);

                String rel = bothCount == 0 ? "DISJOINT"
                    : i_jPropCount == 0 && j_iPropCount == 0 ? "EQUALS"
                    : i_jPropCount == 0 ? "CONTAINS" // depends on reverse output
                    : j_iPropCount == 0 ? "CONTAINS"
                    : "OVERLAPS";

                if (j_iPropCount > i_jPropCount) {
                    // reverse output
                    output.println(jNameShort + "\t" + iNameShort + "\t" + rel
                        + "\t" + bothCount + "\t" + j_iPropCount + "\t" + i_jPropCount);
               } else {
                    output.println(iNameShort + "\t" + jNameShort + "\t" + rel
                        + "\t" + bothCount + "\t" + i_jPropCount + "\t" + j_iPropCount);
                }
            }
        }
        output.close();
    }


    public static void listProperties() throws IOException {
        String prop = "";
        String propAbb = "";
        String value = "";
        String valueAbb = "";
        
        Map duplicates = new TreeMap();
        Set sorted = new TreeSet(java.text.Collator.getInstance());
        Set accumulation = new TreeSet(java.text.Collator.getInstance());
        String spacing;
        
        /*
        BufferedReader blocks = Utility.openUnicodeFile("Blocks", ucd.getVersion());
        String[] parts = new String[10];
        while (true) {
            String line = blocks.readLine();
            if (line == null) break;
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) line = line.substring(0,commentPos);
            line = line.trim();
            if (line.length() == 0) continue;
            int count = Utility.split(line,';',parts);
            if (count != 2) System.out.println("Whow!");
            value = fixGaps(parts[1].trim(), true);
            valueAbb = "n/a";
            spacing = Utility.repeat(" ", 10-valueAbb.length());
            sorted.add("blk; " + valueAbb + spacing + "; " + value);
            checkDuplicate(duplicates, accumulation, value, "Block=" + value);
        }
        blocks.close();
        */
        
        for (int k = 0; k < UCD_Names.NON_ENUMERATED.length; ++k) {
            propAbb = fixGaps(UCD_Names.NON_ENUMERATED[k][0], false);
            prop = fixGaps(UCD_Names.NON_ENUMERATED[k][1], true);
            spacing = Utility.repeat(" ", 10-propAbb.length());
            sorted.add("AA; " + propAbb + spacing + "; " + prop);
            checkDuplicate(duplicates, accumulation, propAbb, prop);
            if (!prop.equals(propAbb)) checkDuplicate(duplicates, accumulation, prop, prop);
        }
 
        for (int k = 0; k < UCD_Names.SUPER_CATEGORIES.length; ++k) {
            valueAbb = fixGaps(UCD_Names.SUPER_CATEGORIES[k][0], false);
            value = fixGaps(UCD_Names.SUPER_CATEGORIES[k][1], true);
            spacing = Utility.repeat(" ", 10-valueAbb.length());
            String baseLine = "gc; " + valueAbb + spacing + "; " + value;
            spacing = Utility.repeat(" ", 50-baseLine.length());
            sorted.add(baseLine + spacing + "# " + UCD_Names.SUPER_CATEGORIES[k][2]);
            checkDuplicate(duplicates, accumulation, value, "General_Category=" + value);
            if (!value.equals(valueAbb)) checkDuplicate(duplicates, accumulation, valueAbb, "General_Category=" + value);
        }
        
        /*
        sorted.add("xx; T         ; True");
        checkDuplicate(duplicates, accumulation, "T", "xx=True");
        sorted.add("xx; F         ; False");
        checkDuplicate(duplicates, accumulation, "F", "xx=False");
        */
        sorted.add("qc; Y         ; Yes");
        checkDuplicate(duplicates, accumulation, "Y", "qc=Yes");
        sorted.add("qc; N         ; No");
        checkDuplicate(duplicates, accumulation, "N", "qc=No");
        sorted.add("qc; M         ; Maybe");
        checkDuplicate(duplicates, accumulation, "M", "qc=Maybe");
        
        
        for (int i = 0; i < LIMIT_ENUM; ++i) {
            int type = i & 0xFF00;
            if (type == AGE) continue;
            if (i == (BINARY_PROPERTIES | CaseFoldTurkishI)) continue;
            
            UnicodeProperty up = UnifiedBinaryProperty.make(i, ucd);
            if (up == null) continue;
            if (!up.isStandard()) continue;
            
            System.out.println("At" + Utility.hex(i));
            
            if (type == i && type != BINARY_PROPERTIES && type != DERIVED) {
                propAbb = fixGaps(up.getProperty(SHORT), false);
                prop = fixGaps(up.getProperty(LONG), true);
                spacing = Utility.repeat(" ", 10-propAbb.length());
                sorted.add("BB; " + propAbb + spacing + "; " + prop);
                checkDuplicate(duplicates, accumulation, propAbb, prop);
                if (!prop.equals(propAbb)) checkDuplicate(duplicates, accumulation, prop, prop);
            }
            
            if (up.valueVaries()) continue;
            value = up.getValue(LONG);
            if (value.length() == 0) value = "none";
            else if (value.equals("<unused>")) continue;
            value = fixGaps(value, true);
            
            if (type == SCRIPT) {
                value = ucd.getCase(value, FULL, TITLE);
            }
            
            valueAbb = up.getValue(SHORT);
            if (valueAbb.length() == 0) valueAbb = "no";
            valueAbb = fixGaps(valueAbb, false);

            if (type == COMBINING_CLASS) {
                if (value.startsWith("Fixed_")) { continue; }
            }
            
            if (type == JOINING_GROUP) {
                valueAbb = "n/a";
            }
            
            /*
            String elide = "";
            if (type == CATEGORY || type == SCRIPT || type == BINARY_PROPERTIES) elide = "\\p{"
                + valueAbb
                + "}";
            String abb = "";
            if (type != BINARY_PROPERTIES) abb = "\\p{"
                + UCD_Names.ABB_UNIFIED_PROPERTIES[i>>8]
                + "="
                + valueAbb
                + "}";
            String norm = "";
            if (type != BINARY_PROPERTIES) norm = "\\p{"
                + UCD_Names.SHORT_UNIFIED_PROPERTIES[i>>8]
                + "="
                + value
                + "}";
            System.out.println("<tr><td>" + elide + "</td><td>" + abb + "</td><td>" + norm + "</td></tr>");
            */
            
            spacing = Utility.repeat(" ", 10-valueAbb.length());
            
            if (type == BINARY_PROPERTIES || type == DERIVED) {
                sorted.add("ZZ; " + valueAbb + spacing + "; " + value);
                checkDuplicate(duplicates, accumulation, value, value);
                if (!value.equalsIgnoreCase(valueAbb)) checkDuplicate(duplicates, accumulation, valueAbb, value);
                continue;
            }
            
            if (type == COMBINING_CLASS) {
                sorted.add(propAbb + "; " + up.getValue(NUMBER) + "; " + valueAbb + spacing + "; " + value);
            } else {
                sorted.add(propAbb + "; " + valueAbb + spacing + "; " + value);
            }
            checkDuplicate(duplicates, accumulation, value, prop + "=" + value);
            if (!value.equalsIgnoreCase(valueAbb) && !valueAbb.equals("n/a")) {
                checkDuplicate(duplicates, accumulation, valueAbb, prop + "=" + value);
            }
        }
        
        String filename = "PropertyAliases-" + ucd.getVersion();
        PrintWriter log = Utility.openPrintWriter("DerivedData/" + filename + getFileSuffix(true));
        generateBat("DerivedData/", filename, getFileSuffix(true));
        
        log.println("# " + filename + getFileSuffix(false));
        log.println("#");
        log.println("# Generated: " + new Date() + ", MD");
        Utility.appendFile("PropertyAliasHeader.txt", false, log);
        log.println(HORIZONTAL_LINE);
        log.println();
        Utility.print(log, sorted, "\r\n", new MyBreaker(true));
        log.close();
        
        filename = "PropertyValueAliases-" + ucd.getVersion();
        log = Utility.openPrintWriter("DerivedData/" + filename + getFileSuffix(true));
        generateBat("DerivedData/", filename, getFileSuffix(true));
        
        log.println("# " + filename + getFileSuffix(false));
        log.println("#");
        log.println("# Generated: " + new Date() + ", MD");
        Utility.appendFile("PropertyValueAliasHeader.txt", false, log);
        log.println("# Generated: " + new Date() + ", MD");
        log.println(HORIZONTAL_LINE);
        log.println();
        Utility.print(log, sorted, "\r\n", new MyBreaker(false));
        log.close();
        
        filename = "PropertyAliasSummary-" + ucd.getVersion();
        log = Utility.openPrintWriter("OtherData/" + filename + getFileSuffix(true));
        generateBat("OtherData/", filename, getFileSuffix(true));
        log.println();
        log.println(HORIZONTAL_LINE);
        log.println();
        log.println("# Non-Unique names: the same name (under either an exact or loose match)");
        log.println("# occurs as a property name or property value name");
        log.println("# Note: no two property names can be the same,");
        log.println("# nor can two property value names for the same property be the same.");
        log.println();
        Utility.print(log, accumulation, "\r\n", new MyBreaker(false));
        log.println();
        log.close();
    }
    
    static class MyBreaker implements Utility.Breaker {
        boolean status;
        
        public MyBreaker(boolean status) {
            this.status = status;
        }
        
        public boolean filter(Object current) {
            String c = current.toString();
            if (c.startsWith("AA") || c.startsWith("BB") || c.startsWith("ZZ")) return status;
            return !status;
        }
        
        public String get(Object current, Object old) {
            if (old == null) {
                old = "  ";
            }
            String c = current.toString();
            String o = old.toString();
            String sep = "";
            if (!c.substring(0,2).equals(o.substring(0,2))) {
                sep = "\r\n";
                if (status) {
                    if (c.startsWith("AA")) sep = sep + HORIZONTAL_LINE + sep + "# Non-enumerated Properties" + sep + HORIZONTAL_LINE + sep;
                    if (c.startsWith("BB")) sep = sep + HORIZONTAL_LINE + sep + "# Enumerated Non-Binary Properties" + sep + HORIZONTAL_LINE + sep;
                    if (c.startsWith("ZZ")) sep = sep + HORIZONTAL_LINE + sep + "# Binary Properties" + sep + HORIZONTAL_LINE + sep;
                }
            }
            if (status) {
                c = c.substring(4);
            }
            return sep + c;
        }
    }
    
    static void checkDuplicate(Map m, Set accumulation, String toCheck, String originalComment) {
        toCheck = skeleton(toCheck);
        String comment = "{" + originalComment + "}";
        
        Set result = (Set) m.get(toCheck);
        if (result != null) {
            // Warn on serious problem: two property-names collide
            // or two property names & values collide.
            // examples:
            // if (1) "c" stood for both "General_Category" and "Combining_Class"
            // or if (2) "X=cc" stood for "X=control" and "X=compatibility"
            // 1: comment doesn't contain "=", and something in the results doesn't contain "="
            // 2: comment does contain "X=", and something else in results contains "X="
            
            int equalPos = comment.indexOf('=');
            if (equalPos < 0) { // #1
                String conflict = Utility.findSubstring("=", result, false);
                if (conflict != null) {
                    System.out.println("Property Name Conflict " + toCheck);
                    System.out.println("  With " + comment);
                    System.out.println("  And  " + conflict);
                }
            } else {    // #2
                String trial = comment.substring(0,equalPos+1);
                String conflict = Utility.findSubstring(trial, result, true);
                if (conflict != null) {
                    System.out.println("Property Value Name Conflict " + toCheck);
                    System.out.println("  With " + comment);
                    System.out.println("  And  " + conflict);
                }
            }
            
            // accumulate differences
            /*
            String acc = (String)accumulation.get(toCheck);
            if (acc == null) {
                acc = "# \"" + toCheck + "\":\t" + originalComment;
            }
            acc += ";\t" + result;
            */
            result.add(comment);
            accumulation.add("# " + result.toString() + ":\t" + toCheck);
        } else {
            result = new TreeSet();
            result.add(comment);
            m.put(toCheck, result);
        }
    }
    
    static String fixGaps(String source, boolean titlecaseStart) {
        StringBuffer result = new StringBuffer();
        byte lastCat = -1;
        boolean haveFirstCased = true;
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == ' ' || c == '-') c = '_';
            byte cat = ucd.getCategory(c);
            if (lastCat == Ll && cat == Lu) {
                result.append('_');
            }
            if (haveFirstCased && (cat == Ll || cat == Lt || cat == Lu)) {
                if (titlecaseStart) {
                    c = ucd.getCase(c, SIMPLE, TITLE).charAt(0);
                }
                haveFirstCased = false;
            }
            result.append(c);
            lastCat = cat;
        }
        return result.toString();
    }
    
    static String skeleton(String source) {
        StringBuffer result = new StringBuffer();
        source = source.toLowerCase();
        for (int i = 0; i < source.length(); ++i) {
            char c = source.charAt(i);
            if (c == ' ' || c == '_' || c == '-') continue;
            result.append(c);
        }
        return result.toString();
    }
    
    static final byte KEEP_SPECIAL = 0, SKIP_SPECIAL = 1;
    
    public static void generateBat(String directory, String fileRoot, String suffix) throws IOException {
        String oldName = Utility.getMostRecentUnicodeDataFile(fixFile(fileRoot), ucd.getVersion(), true);
        if (oldName == null) {
            System.out.println("No previous version of: " + fileRoot + ".txt");
            return;
        }
        PrintWriter output = Utility.openPrintWriter(directory + "DIFF/Diff_" + fileRoot + ".bat");
        String newName = Utility.getOutputName(directory + fileRoot + suffix);
        System.out.println("Writing BAT to compare " + oldName + " and " + newName);
        
        File newFile = new File(newName);
        File oldFile = new File(oldName);
        output.println("\"C:\\Program Files\\wincmp.exe\" "
            + oldFile.getCanonicalFile()
            + " "
            + newFile.getCanonicalFile());
        output.close();
    }
        

    public static void generateVerticalSlice(int startEnum, int endEnum, byte skipSpecial,
            int headerChoice, String directory, String file) throws IOException {

        PrintWriter output = Utility.openPrintWriter(directory + file + getFileSuffix(true));
        generateBat(directory, file, getFileSuffix(true));
        
        doHeader(file + getFileSuffix(false), output, headerChoice);
        int last = -1;
        for (int i = startEnum; i < endEnum; ++i) {
            UnicodeProperty up = UnifiedBinaryProperty.make(i, ucd);
            if (up == null) continue;
            
            if (i == DECOMPOSITION_TYPE || i == NUMERIC_TYPE
                || i == (BINARY_PROPERTIES | Non_break)
                || i == (BINARY_PROPERTIES | CaseFoldTurkishI)
                || i == (JOINING_TYPE | JT_U)
                || i == (JOINING_GROUP | NO_SHAPING)
                ) continue; // skip zero case
            if (skipSpecial == SKIP_SPECIAL
                    && i >= (BINARY_PROPERTIES | CompositionExclusion)
                    && i < (AGE + NEXT_ENUM)) continue;
            if ((last & 0xFF00) != (i & 0xFF00) && (i <= BINARY_PROPERTIES || i >= SCRIPT)) {
                output.println();
                output.println(HORIZONTAL_LINE);
                output.println("# " + up.getHeader());
                output.println(HORIZONTAL_LINE);
                output.println();
                System.out.println();
                System.out.println(up.getHeader());
                last = i;
            } else {
                output.println(HORIZONTAL_LINE);
                output.println();
            }
            System.out.print(".");
            if (DEBUG) System.out.println(i);
            new MyPropertyLister(ucd, i, output).print();
            output.flush();
        }
        if (endEnum == LIMIT_ENUM) {
            output.println();
                output.println(HORIZONTAL_LINE);
            output.println("# Numeric Values (from UnicodeData.txt, field 6/7/8)");
                output.println(HORIZONTAL_LINE);
            output.println();
            System.out.println();
            System.out.println("@NUMERIC VALUES");

            Set floatSet = new TreeSet();
            for (int i = 0; i < 0x10FFFF; ++i) {
                float nv = ucd.getNumericValue(i);
                if (Float.isNaN(nv)) continue;
                floatSet.add(new Float(nv));
            }
            Iterator it = floatSet.iterator();
            while(it.hasNext()) {
                new MyFloatLister(ucd, ((Float)it.next()).floatValue(), output).print();
                output.println();
                System.out.print(".");
            }
            output.flush();
        }
        output.close();
        System.out.println();
    }

    static UCD ucd;

    static public Normalizer formC, formD, formKC, formKD;

    static public void writeNormalizerTestSuite(String directory, String fileName) throws IOException {
        PrintWriter log = Utility.openPrintWriter(directory + fileName + getFileSuffix(true));
        generateBat(directory, fileName, getFileSuffix(true));

	    formC = new Normalizer(Normalizer.NFC);
	    formD = new Normalizer(Normalizer.NFD);
	    formKC = new Normalizer(Normalizer.NFKC);
	    formKD = new Normalizer(Normalizer.NFKD);

        String[] example = new String[256];

        log.println("# " + fileName + getFileSuffix(false));
        log.println("#");
        log.println("# Normalization Test Suite");
        log.println("# Date: " + myDateFormat.format(new Date()) + " [MD]");
        log.println("# Format:");
        log.println("#");
        log.println("#   Columns (c1, c2,...) are separated by semicolons");
        log.println("#   Comments are indicated with hash marks");
        log.println("#");
        log.println("# CONFORMANCE:");
        log.println("# 1. The following invariants must be true for all conformant implementations");
        log.println("#");
        log.println("#    NFC");
        log.println("#      c2 ==  NFC(c1) ==  NFC(c2) ==  NFC(c3)");
        log.println("#      c4 ==  NFC(c4) ==  NFC(c5)");
        log.println("#");
        log.println("#    NFD");
        log.println("#      c3 ==  NFD(c1) ==  NFD(c2) ==  NFD(c3)");
        log.println("#      c5 ==  NFD(c4) ==  NFD(c5");
        log.println("#");
        log.println("#    NFKC");
        log.println("#      c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)");
        log.println("#");
        log.println("#    NFKD");
        log.println("#      c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)");
        log.println("#");
        log.println("# 2. For every assigned Unicode 3.1.0 code point X that is not specifically");
        log.println("#    listed in Part 1, the following invariants must be true for all conformant");
        log.println("#    implementations:");
        log.println("#");
        log.println("#      X == NFC(X) == NFD(X) == NFKC(X) == NFKD(X)");

        System.out.println("Writing Part 1");

        log.println("#");
        log.println("@Part0 # Specific cases");
        log.println("#");

        for (int j = 0; j < testSuiteCases.length; ++j) {
            writeLine(testSuiteCases[j], log, false);
        }

        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part1 # Character by character test");
        log.println("# All characters not explicitly occurring in c1 of Part 1 have identical NFC, D, KC, KD forms.");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            String cc = UTF32.valueOf32(ch);
            writeLine(cc,log, true);
        }
        Utility.fixDot();

        System.out.println("Finding Examples");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            int cc = ucd.getCombiningClass(ch);
            if (example[cc] == null) example[cc] = UTF32.valueOf32(ch);
        }

        Utility.fixDot();
        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part2 # Canonical Order Test");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {

            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            short c = ucd.getCombiningClass(ch);
            if (c == 0) continue;

            // add character with higher class, same class, lower class

            String sample = "";
            for (int i = c+1; i < example.length; ++i) {
                if (example[i] == null) continue;
                sample += example[i];
                break;
            }
            sample += example[c];
            for (int i = c-1; i > 0; --i) {
                if (example[i] == null) continue;
                sample += example[i];
                break;
            }

            writeLine("a" + sample + UTF32.valueOf32(ch) + "b", log, false);
            writeLine("a" + UTF32.valueOf32(ch) + sample + "b", log, false);
        }
        Utility.fixDot();
        log.println("#");
        log.println("# END OF FILE");
        log.close();
    }

    static void writeLine(String cc, PrintWriter log, boolean check) {
        String c = formC.normalize(cc);
        String d = formD.normalize(cc);
        String kc = formKC.normalize(cc);
        String kd = formKD.normalize(cc);
        if (check & cc.equals(c) && cc.equals(d) && cc.equals(kc) && cc.equals(kd)) return;

        // consistency check
        String dc = formD.normalize(c);
        String dkc = formD.normalize(kc);
        if (!dc.equals(d) || !dkc.equals(kd)) {
            System.out.println("Danger Will Robinson!");
            Normalizer.SHOW_PROGRESS = true;
            d = formD.normalize(cc);
        }

        // printout
        log.println(
            Utility.hex(cc," ") + ";" + Utility.hex(c," ") + ";" + Utility.hex(d," ") + ";"
            + Utility.hex(kc," ") + ";" + Utility.hex(kd," ")
            + "; # ("
            + comma(cc) + "; " + comma(c) + "; " + comma(d) + "; " + comma(kc) + "; " + comma(kd) + "; "
            + ") " + ucd.getName(cc));
    }

    static StringBuffer commaResult = new StringBuffer();

    // not recursive!!!
    static final String comma(String s) {
        commaResult.setLength(0);
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(i)) {
            cp = UTF32.char32At(s, i);
            if (ucd.getCategory(cp) == Mn) commaResult.append('\u25CC');
            UTF32.append32(commaResult, cp);
        }
        return commaResult.toString();
    }

    static final String[] testSuiteCases = {
        "\u1E0A",
        "\u1E0C",
        "\u1E0A\u0323",
        "\u1E0C\u0307",
        "D\u0307\u0323",
        "D\u0323\u0307",
        "\u1E0A\u031B",
        "\u1E0C\u031B",
        "\u1E0A\u031B\u0323",
        "\u1E0C\u031B\u0307",
        "D\u031B\u0307\u0323",
        "D\u031B\u0323\u0307",
        "\u00C8",
        "\u0112",
        "E\u0300",
        "E\u0304",
        "\u1E14",
        "\u0112\u0300",
        "\u1E14\u0304",
        "E\u0304\u0300",
        "E\u0300\u0304",
        "\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F",
        "\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD"

    };
    
    static final void generateAge(String directory, String filename) throws IOException {
        PrintWriter log = Utility.openPrintWriter(directory + filename + getFileSuffix(true));
        generateBat(directory, filename, getFileSuffix(true));
        try {
            log.println("# " + filename + getFileSuffix(false));
            log.println("#");
            log.println("# Unicode Character Database: Derived Property Data");
            log.println("# This file shows when various code points were designated in Unicode");
            log.println("# Generated: " + new Date() + ", MD");
            log.println("# Notes:");
            log.println("# - The term 'designated' means that a previously reserved code point was specified");
            log.println("#   to be a noncharacter or surrogate, or assigned as a character,");
            log.println("#   control or format code.");
            log.println("# - Versions are only tracked from 1.1 onwards, since version 1.0");
            log.println("#   predated changes required by the ISO 10646 merger.");
            log.println("# - The Hangul Syllables that were removed from 2.0 are not included in the 1.1 listing.");
            log.println("# - The supplementary private use code points and the non-character code points");
            log.println("#   were designated in version 2.0, but not specifically listed in the UCD");
            log.println("#   until versions 3.0 and 3.1 respectively.");
            log.println("#");
            log.println("# For details on the contents of each version, see");
            log.println("#   http://www.unicode.org/versions/enumeratedversions.html.");
            
            http://www.unicode.org/versions/enumeratedversions.html
            
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister(null, "1.1.0", log).print();
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("1.1.0", "2.0.0", log).print();
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("2.0.0", "2.1.2", log).print();
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("2.1.2", "3.0.0", log).print();
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("3.0.0", "3.1.0", log).print();
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("3.1.0", "3.2.0", log).print();
            /*
            printDiff("110", "200");
	        UnicodeSet u11 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-1.1.txt", false);
	        UnicodeSet u20 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-2.0.txt", false);
	        UnicodeSet u21 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-2.1.txt", false);
	        UnicodeSet u30 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-3.0.txt", false);
	        UnicodeSet u31 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-3.1.txt", false);

            log.println();
            log.println("# Code points assigned in Unicode 1.1 (minus Hangul Syllables): "
                + n.format(u11.count()));
            log.println();
            u11.print(log, false, false, "1.1");

            UnicodeSet u20m = new UnicodeSet(u20).remove(u11);
            log.println();
            log.println("# Code points assigned in Unicode 2.0 (minus Unicode 1.1): "
                + n.format(u20m.count()));
            log.println();
            u20m.print(log, false, false, "2.0");

            UnicodeSet u21m = new UnicodeSet(u21).remove(u20);
            log.println();
            log.println("# Code points assigned in Unicode 2.1 (minus Unicode 2.0): "
                + n.format(u21m.count()));
            log.println();
            u21m.print(log, false, false, "2.1");

            UnicodeSet u30m = new UnicodeSet(u30).remove(u21);
            log.println();
            log.println("# Code points assigned in Unicode 3.0 (minus Unicode 2.1): "
                + n.format(u30m.count()));
            log.println();
            u30m.print(log, false, false, "3.0");

            UnicodeSet u31m = new UnicodeSet(u31).remove(u30);
            log.println();
            log.println("# Code points assigned in Unicode 3.1 (minus Unicode 3.0): "
                + n.format(u31m.count()));
            log.println();
            u31m.print(log, false, false, "3.1");
            */
        } finally {
            if (log != null) log.close();
        }

    }
    
    public static void listCombiningAccents() throws IOException {
        PrintWriter log = Utility.openPrintWriter("ListAccents.txt");
        Normalizer nfd = new Normalizer(Normalizer.NFD);
        Set set = new TreeSet();
        Set set2 = new TreeSet();
        
        for (int i = 0; i < 0x10FFFF; ++i) {
            Utility.dot(i);
            if (!ucd.isRepresented(i)) continue;
            
            if (!nfd.hasDecomposition(i)) {
                if (ucd.getScript(i) == LATIN_SCRIPT) {
                    int cp = i;
                    String hex = "u" + Utility.hex(cp, 4);
                    set.add("# yyy $x <> \\" + hex + " ; # " + ucd.getName(cp));
                }
                continue;
            }
            
            String decomp = nfd.normalize(i);
            int j;
            for (j = 0; j < decomp.length(); j += UTF16.getCharCount(i)) {
                int cp = UTF16.charAt(decomp, j);
                byte cat = ucd.getCategory(cp);
                if (cat != Mn) continue;
                String hex = "u" + Utility.hex(cp, 4);
                set.add("# xxx $x <> \\" + hex + " ; # " + ucd.getName(cp));
            }
        }
        
        Iterator it = set.iterator();
        while (it.hasNext()) {
            log.println(it.next());
        }
        log.close();
    }
    
    public static void listGreekVowels() throws IOException {
        PrintWriter log = Utility.openPrintWriter("ListGreekVowels.txt");
        Normalizer nfd = new Normalizer(Normalizer.NFD);
        Normalizer nfc = new Normalizer(Normalizer.NFC);
        Set set = new TreeSet();
        Set set2 = new TreeSet();
        
        String vowels = "\u03B1\u03B5\u03B7\u03B9\u03BF\u03C5\u03C9\u0391\u0395\u0397\u0399\u039F\u03A5\u03A9";
        String diphthongEnd = "\u03B9\u03C5\u0399\u03A5";
        String diphthongStart = "\u03B1\u03B5\u03B7\u03BF\u03C5\u0391\u0395\u0397\u039F\u03A5";
        String etas = "\u03B7\u0397";
        String iotas = "\u03B9\u0399";
        
        for (char i = 0; i < 0xFFFF; ++i) {
            Utility.dot(i);
            if (!ucd.isRepresented(i)) continue;
            if (ucd.getScript(i) != GREEK_SCRIPT) continue;
            String decomp = nfd.normalize(i);
            
            if (decomp.indexOf('\u0306') >= 0) continue; // skip breve
            if (decomp.indexOf('\u0304') >= 0) continue; // skip macron
            
            String comp = nfc.normalize(decomp);
            if (!comp.equals(String.valueOf(i))) continue; // skip compats
            
            char first = decomp.charAt(0);
            
            if (vowels.indexOf(first) < 0) continue;
            
            String h = "";
            if (decomp.indexOf('\u0314') >= 0) h = "\uFFFF";
            
            if (diphthongEnd.indexOf(first) >= 0) {
                for (int j = 0; j < diphthongStart.length(); ++j) {
                    String v = diphthongStart.substring(j, j+1);
                    char vc = v.charAt(0);
                    if (ucd.getCategory(vc) == Ll && ucd.getCategory(first) == Lu) continue;
                    if (etas.indexOf(vc) >= 0 && iotas.indexOf(first) >= 0) continue;
                    set.add(new Pair(h + v + first, new Pair(v + decomp, v + i)));
                }
            }
            set.add(new Pair(h+first, new Pair(decomp, String.valueOf(i))));
        }
        
        Iterator it = set.iterator();
        Object last = "";
        while (it.hasNext()) {
            Pair p = (Pair) it.next();
            if (!last.equals(p.first)) {
                log.println();
                last = p.first;
            } else {
                log.print(", ");
            }
            p = (Pair) p.second;
            log.print(p.second);
        }
        log.close();
    }
    
    public static void listKatakana() throws IOException {
        
        for (char i = 'a'; i <= 'z'; ++i) {
            doKana(String.valueOf(i));
            if (i == 'c') doKana("ch");
            if (i == 's') doKana("sh");
            if (i == 'd') {
                doKana("dz");
                doKana("dj");
            }
        }
        
        System.out.println();
    }
    
    public static void doKana(String i) {
        
        String vowels = "aeiou";
        System.out.println();
        System.out.print(i + " " + i + i);
        System.out.println();
        for (int j = 0; j < vowels.length(); ++j) {
            char c = vowels.charAt(j);
            System.out.print(" " + i + c);
        }

        System.out.println();
        for (int j = 0; j < vowels.length(); ++j) {
            char c = vowels.charAt(j);
            System.out.print(" " + i + "y" + c);
        }
    }
}