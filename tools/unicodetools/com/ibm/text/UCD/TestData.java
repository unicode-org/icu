/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/TestData.java,v $
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

public class TestData implements UCD_Types {

    public static void main (String[] args) throws IOException {
        System.out.println("START");
        ucd = UCD.make();
        System.out.println("Loaded UCD " + ucd.getVersion() + " " + (new Date(ucd.getDate())));

        checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
        checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");

        int mask = 0;

        if (false) {

        generateVerticalSlice(BIDI_CLASS, BIDI_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedBidiClass-3.1.1d1.txt");


        mask = Utility.setBits(0, DerivedPropertyLister.FC_NFKC_Closure, DerivedPropertyLister.ExpandsOnNFKC);
        mask = Utility.clearBit(mask, DerivedPropertyLister.FullCompInclusion);
        generateDerived(mask, HEADER_DERIVED, "DerivedNormalizationProperties-3.1.0d1.txt");

        generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedEastAsianWidth-3.1.0d1.txt");

        generateVerticalSlice(CATEGORY, CATEGORY+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedGeneralCategory-3.1.0d1.txt");
        generateVerticalSlice(COMBINING_CLASS, COMBINING_CLASS+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedCombiningClass-3.1.0d1.txt");
        generateVerticalSlice(DECOMPOSITION_TYPE, DECOMPOSITION_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedDecompositionType-3.1.0d1.txt");
        generateVerticalSlice(NUMERIC_TYPE, NUMERIC_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedNumericType-3.1.0d1.txt");
        generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedEastAsianWidth-3.1.0d1.txt");
        generateVerticalSlice(JOINING_TYPE, JOINING_TYPE+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedJoiningType-3.1.0d1.txt");
        generateVerticalSlice(JOINING_GROUP, JOINING_GROUP+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedJoiningGroup-3.1.0d1.txt");
        generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES+1, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedBinaryProperties-3.1.0d1.txt");
        generateVerticalSlice(LIMIT_ENUM, LIMIT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedNumericValues-3.1.0d1.txt");

        mask = Utility.setBits(0, DerivedPropertyLister.PropMath, DerivedPropertyLister.Mod_ID_Continue_NO_Cf);
        generateDerived(mask, HEADER_DERIVED, "DerivedCoreProperties-3.1.0d1.txt");

        generateVerticalSlice(LINE_BREAK, LINE_BREAK+NEXT_ENUM, KEEP_SPECIAL, HEADER_DERIVED,
            "DerivedLineBreak-3.1.0d1.txt");

        generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM, KEEP_SPECIAL, HEADER_SCRIPTS, "Scripts-3.1.0d4.txt");

        generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + Noncharacter_Code_Point + 1,
                KEEP_SPECIAL, HEADER_EXTEND, "PropList-3.1.0d5.txt");


            writeNormalizerTestSuite("NormalizationTest-3.1.0d1.txt");

        }




            //generateDerived(Utility.setBits(0, DerivedPropertyLister.PropMath, DerivedPropertyLister.Mod_ID_Continue_NO_Cf),
            //    HEADER_DERIVED, "DerivedPropData2-3.1.0d1.txt");
        //generateVerticalSlice(SCRIPT, SCRIPT+1, KEEP_SPECIAL, "ScriptCommon-3.1.0d1.txt");
        //listStrings("LowerCase-3.1.0d1.txt", 0,0);
        //generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedPropData1-3.1.0d1.txt");

        // AGE stuff
        //UCD ucd = UCD.make();
        //System.out.println(ucd.getAgeID(0x61));
        //System.out.println(ucd.getAgeID(0x2FA1D));


        //generateCompExclusions();
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
        output.println("# " + fixFile(fileName));
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

    static final byte KEEP_SPECIAL = 0, SKIP_SPECIAL = 1;

    public static void generateVerticalSlice(int startEnum, int endEnum, byte skipSpecial, int headerChoice, String file) throws IOException {

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


        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + file));
        doHeader(file, output, headerChoice);
        int last = -1;
        for (int i = startEnum; i < endEnum; ++i) {
            if (!MyPropertyLister.isUnifiedBinaryPropertyDefined(ucd, i)) continue;
            if (i == DECOMPOSITION_TYPE || i == NUMERIC_TYPE
                || i == (CATEGORY | UNUSED_CATEGORY)
                || i == (BINARY_PROPERTIES | Non_break)
                || i == (JOINING_TYPE | JT_U)
                || i == (SCRIPT | UNUSED_SCRIPT)
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

        PrintWriter log = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(GEN_DIR + fileName),
                "UTF8"),
            32*1024));
	    formC = new Normalizer(Normalizer.NFC);
	    formD = new Normalizer(Normalizer.NFD);
	    formKC = new Normalizer(Normalizer.NFKC);
	    formKD = new Normalizer(Normalizer.NFKD);

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

        String[] example = new String[256];

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!ucd.isAssigned(ch)) continue;
            if (ucd.isPUA(ch)) continue;
            int cc = ucd.getCombiningClass(ch);
            if (example[cc] == null) example[cc] = UTF32.valueOf32(ch);
        }

        Utility.fixDot();
        System.out.println("Writing Part 3");

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
    };

}