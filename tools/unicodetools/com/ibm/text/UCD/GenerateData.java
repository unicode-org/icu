/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateData.java,v $
* $Date: 2001/08/31 00:30:17 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ibm.text.utility.*;

public class GenerateData implements UCD_Types {

    public static void main (String[] args) throws IOException {
        System.out.println("START");
        ucd = UCD.make();
        System.out.println("Loaded UCD " + ucd.getVersion() + " " + (new Date(ucd.getDate())));
        String version = ucd.getVersion();

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line
            int mask = 0;

            Utility.fixDot();
            System.out.println("Argument: " + args[i]);

            if (arg.equalsIgnoreCase("version")) {
                version = args[++i];
                ucd = UCD.make(version);
            } else if (arg.equalsIgnoreCase("partition")) {
                partitionProperties();
            } else if (arg.equalsIgnoreCase("list")) {
                listProperties();
            } else if (arg.equalsIgnoreCase("diff")) {
                listDifferences();
            } else if (arg.equalsIgnoreCase("DerivedBidiClass")) {
                generateVerticalSlice(BIDI_CLASS, BIDI_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedBidiClass-" + version );
            } else if (arg.equalsIgnoreCase("DerivedNormalizationProperties")) {
                mask = Utility.setBits(0, DerivedPropertyLister.FC_NFKC_Closure, DerivedPropertyLister.ExpandsOnNFKC);
                mask = Utility.clearBit(mask, DerivedPropertyLister.FullCompInclusion);
                generateDerived(mask, HEADER_DERIVED, "DerivedNormalizationProperties-" + version );
            } else if (arg.equalsIgnoreCase("DerivedEastAsianWidth")) {
                generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedEastAsianWidth-" + version );
            } else if (arg.equalsIgnoreCase("DerivedGeneralCategory")) {
                generateVerticalSlice(CATEGORY, CATEGORY+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedGeneralCategory-" + version );
            } else if (arg.equalsIgnoreCase("DerivedCombiningClass")) {
                generateVerticalSlice(COMBINING_CLASS, COMBINING_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedCombiningClass-" + version );
            } else if (arg.equalsIgnoreCase("DerivedDecompositionType")) {
                generateVerticalSlice(DECOMPOSITION_TYPE, DECOMPOSITION_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedDecompositionType-" + version );
            } else if (arg.equalsIgnoreCase("DerivedNumericType")) {
                generateVerticalSlice(NUMERIC_TYPE, NUMERIC_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedNumericType-" + version );
            } else if (arg.equalsIgnoreCase("DerivedEastAsianWidth")) {
                generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedEastAsianWidth-" + version );
            } else if (arg.equalsIgnoreCase("DerivedJoiningType")) {
                generateVerticalSlice(JOINING_TYPE, JOINING_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedJoiningType-" + version );
            } else if (arg.equalsIgnoreCase("DerivedJoiningGroup")) {
                generateVerticalSlice(JOINING_GROUP, JOINING_GROUP+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedJoiningGroup-" + version );
            } else if (arg.equalsIgnoreCase("DerivedBinaryProperties")) {
                generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES+1, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedBinaryProperties-" + version );
            } else if (arg.equalsIgnoreCase("DerivedNumericValues")) {
                generateVerticalSlice(LIMIT_ENUM, LIMIT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedNumericValues-" + version );
            } else if (arg.equalsIgnoreCase("DerivedCoreProperties")) {
                mask = Utility.setBits(0, DerivedPropertyLister.PropMath, DerivedPropertyLister.Mod_ID_Continue_NO_Cf);
                generateDerived(mask, HEADER_DERIVED, "DerivedCoreProperties-" + version );
            } else if (arg.equalsIgnoreCase("DerivedLineBreak")) {
                generateVerticalSlice(LINE_BREAK, LINE_BREAK+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
                    "DerivedLineBreak-" + version );
            } else if (arg.equalsIgnoreCase("Scripts")) {
                generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM, KEEP_SPECIAL, HEADER_SCRIPTS, "Scripts-");
            } else if (arg.equalsIgnoreCase("PropList")) {
                generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + Noncharacter_Code_Point + 1,
                        KEEP_SPECIAL, HEADER_EXTEND, "PropList-" + version);
            } else if (arg.equalsIgnoreCase("AllBinary")) {
                generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES + NEXT_ENUM,
                        KEEP_SPECIAL, HEADER_EXTEND, "AllBinary-" + version);
            } else if (arg.equalsIgnoreCase("NormalizationTest")) {
                writeNormalizerTestSuite("NormalizationTest-" + version + ".txt" );
            } else if (arg.equalsIgnoreCase("generateCompExclusions")) {
                generateCompExclusions();
            }else {
                System.out.println(" ! Unknown option -- must be one of the following (case-insensitive)");
                System.out.println(" ! generateCompExclusions,...");
            }


            //checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
            //checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");


                //generateDerived(Utility.setBits(0, DerivedPropertyLister.PropMath, DerivedPropertyLister.Mod_ID_Continue_NO_Cf),
                //    HEADER_DERIVED, "DerivedPropData2-" + version );
            //generateVerticalSlice(SCRIPT, SCRIPT+1, KEEP_SPECIAL, "ScriptCommon-" + version );
            //listStrings("LowerCase-" + version , 0,0);
            //generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedPropData1-" + version );

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
        if (c < '0' || '9' < c) return s;
        System.out.println("Fixing File Name");
        return s.substring(0,len-6) + s.substring(len-4);
    }

    static final int HEADER_EXTEND = 0, HEADER_DERIVED = 1, HEADER_SCRIPTS = 2;

    public static void doHeader(String fileName, PrintStream output, int headerChoice) {
        output.println("# " + fileName + ".txt");
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
        output.println("# ================================================");
        output.println();
    }

    public static void generateDerived (int bitMask, int headerChoice, String fileName) throws IOException {
        ucd = UCD.make("310");
        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + fileName));
        doHeader(fileName, output, headerChoice);
        for (int i = 0; i < 32; ++i) {
            if ((bitMask & (1<<i)) == 0) continue;
            if (i >= DerivedPropertyLister.LIMIT) break;
            System.out.print('.');
            output.println("# ================================================");
            output.println();
            new DerivedPropertyLister(ucd, i, output).print();
        }
        output.close();
    }

    /*
    public static void listStrings(String file, int type, int subtype) throws IOException {
        ucd = UCD.make("310");
        UCD ucd30 = UCD.make("300");
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

    public static void generateCompExclusions() throws IOException {
        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + "CompositionExclusionsDelta.txt"));
        new CompLister(output).print();
        output.close();
    }

    static class CompLister extends PropertyLister {
        UCD oldUCD;
        int oldLength = 0;

        public CompLister(PrintStream output) {
            this.output = output;
            ucdData = UCD.make("310");
            oldUCD = UCD.make("300");
            showOnConsole = true;
        }
        public String propertyName(int cp) {
            return UTF32.length32(ucdData.getDecompositionMapping(cp)) + "";
        }
        public byte status(int cp) {
            if (ucdData.getDecompositionType(cp) == CANONICAL
              && oldUCD.getDecompositionType(cp) != CANONICAL) {
                int temp = oldLength;
                oldLength = UTF32.length32(ucdData.getDecompositionMapping(cp));
                if (temp != oldLength) return BREAK;
                return INCLUDE;
            }
            return EXCLUDE;
        }
    }

    public static void partitionProperties() throws IOException {

        // find properties

        int count = 0;
        int[] props = new int[500];
        for (int i = 1; i < LIMIT_ENUM; ++i) { //   || iType == SCRIPT
            int iType = i & 0xFF00;
            if (iType == JOINING_GROUP || iType == AGE || iType == COMBINING_CLASS) continue;
            if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, i)) continue;
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
                boolean iProp = MyPropertyLister.getUnifiedBinaryProperty(ucd, cp, props[i]);
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

        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + "PropertyDifferences.txt"));

        for (int i = 1; i < LIMIT_ENUM; ++i) {
            int iType = i & 0xFF00;
            if (iType == JOINING_GROUP || iType == AGE || iType == COMBINING_CLASS || iType == SCRIPT) continue;
            if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, i)) continue;
            String iNameShort = MyPropertyLister.getFullUnifiedBinaryPropertyID(ucd, i, MyPropertyLister.SHORT);
            String iNameLong = MyPropertyLister.getFullUnifiedBinaryPropertyID(ucd, i, MyPropertyLister.LONG);

            System.out.println();
            System.out.println();
            System.out.println(iNameLong);
            output.println("#" + iNameLong);

            int last = -1;
            for (int j = i+1; j < LIMIT_ENUM; ++j) {
                int jType = j & 0xFF00;
                if (jType == JOINING_GROUP || jType == AGE || jType == COMBINING_CLASS || jType == SCRIPT
                    || (jType == iType && jType != BINARY_PROPERTIES)) continue;
                if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, j)) continue;

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

                    boolean iProp = MyPropertyLister.getUnifiedBinaryProperty(ucd, cp, i);
                    boolean jProp = MyPropertyLister.getUnifiedBinaryProperty(ucd, cp, j);

                    if (jProp) ++jCount;
                    if (iProp) {
                        ++iCount;
                        if (jProp) ++bothCount;
                        else ++i_jPropCount;
                    } else if (jProp) ++j_iPropCount;
                }
                if (iCount == 0 || jCount == 0) continue;

                String jNameShort = MyPropertyLister.getFullUnifiedBinaryPropertyID(ucd, j, MyPropertyLister.SHORT);
                //String jNameLong = MyPropertyLister.getFullUnifiedBinaryPropertyID(ucd, j, MyPropertyLister.LONG);

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


    public static void listProperties() {
        for (int i = 0; i < LIMIT_ENUM; ++i) {
            int type = i & 0xFF00;
            if (type == JOINING_GROUP || type == AGE) continue;
            if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, i)) continue;
            String value = MyPropertyLister.getUnifiedBinaryPropertyID(ucd, i, MyPropertyLister.LONG);
            if (value.length() == 0) value = "none";
            else if (value.equals("<unused>")) continue;
            String abbvalue = MyPropertyLister.getUnifiedBinaryPropertyID(ucd, i, MyPropertyLister.SHORT);
            if (abbvalue.length() == 0) abbvalue = "no";

            if (type == COMBINING_CLASS) {
                value = MyPropertyLister.getCombiningName(i);
                if (value.length() == 0) {
                    if ((i & 0xFF) == 0) value = "99";
                    else continue;
                }
                abbvalue = value;
            }

            String elide = "";
            if (type == CATEGORY || type == SCRIPT || type == BINARY_PROPERTIES) elide = "\\p{"
                + abbvalue
                + "}";
            String abb = "";
            if (type != BINARY_PROPERTIES) abb = "\\p{"
                + UCD_Names.ABB_UNIFIED_PROPERTIES[i>>8]
                + "="
                + abbvalue
                + "}";
            String norm = "";
            if (type != BINARY_PROPERTIES) norm = "\\p{"
                + UCD_Names.SHORT_UNIFIED_PROPERTIES[i>>8]
                + "="
                + value
                + "}";
            System.out.println("<tr><td>" + elide + "</td><td>" + abb + "</td><td>" + norm + "</td></tr>");
        }
    }

    static final byte KEEP_SPECIAL = 0, SKIP_SPECIAL = 1;

    public static void generateVerticalSlice(int startEnum, int endEnum, byte skipSpecial,
            int headerChoice, String file) throws IOException {

        //System.out.println(ucd.toString(0x1E0A));
        /*
        System.out.println(ucd.getData(0xFFFF));
        System.out.println(ucd.getData(0x100000));
        System.out.println(ucd.getData(0x100000-1));
        System.out.println(ucd.getData(0x100000-2));
        System.out.println(ucd.getData(0x100000-3));
        if (true) return;
        String test2 = ucd.getName(0x2A6D6);
        //*/


        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + file + "dX.txt"));
        doHeader(file, output, headerChoice);
        int last = -1;
        for (int i = startEnum; i < endEnum; ++i) {
            if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, i)) continue;
            if (i == DECOMPOSITION_TYPE || i == NUMERIC_TYPE
                || i == (BINARY_PROPERTIES | Non_break)
                || i == (JOINING_TYPE | JT_U)
                || i == (JOINING_GROUP | NO_SHAPING)
                ) continue; // skip zero case
            if (skipSpecial == SKIP_SPECIAL
                    && i >= (BINARY_PROPERTIES | CompositionExclusion)
                    && i < (AGE + NEXT_ENUM)) continue;
            if ((last & 0xFF00) != (i & 0xFF00) && (i <= BINARY_PROPERTIES || i >= SCRIPT)) {
                output.println();
                output.println("# ================================================");
                output.println("# " + UCD_Names.UNIFIED_PROPERTIES[i>>8]);
                output.println("# ================================================");
                output.println();
                System.out.println();
                System.out.println(UCD_Names.UNIFIED_PROPERTIES[i>>8]);
                last = i;
            } else {
                output.println("# ================================================");
                output.println();
            }
            System.out.print(".");
            new MyPropertyLister(ucd, i, output).print();
        }
        if (endEnum == LIMIT_ENUM) {
            output.println();
                output.println("# ================================================");
            output.println("# Numeric Values (from UnicodeData.txt, field 6/7/8)");
                output.println("# ================================================");
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
        }
        output.close();
        System.out.println();
    }

    static UCD ucd;

    static public Normalizer formC, formD, formKC, formKD;

    static public void writeNormalizerTestSuite(String fileName) throws IOException {
        ucd = UCD.make();

        PrintWriter log = Utility.openPrintWriter(fileName);

	    formC = new Normalizer(Normalizer.NFC);
	    formD = new Normalizer(Normalizer.NFD);
	    formKC = new Normalizer(Normalizer.NFKC);
	    formKD = new Normalizer(Normalizer.NFKD);

        String[] example = new String[256];

        log.println("# " + fixFile(fileName));
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

}