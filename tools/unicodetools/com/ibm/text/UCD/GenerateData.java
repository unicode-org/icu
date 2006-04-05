/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateData.java,v $
* $Date: 2006/04/05 22:12:44 $
* $Revision: 1.40 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


public class GenerateData implements UCD_Types {
    
 /*   static final boolean DEBUG = false;
    
    static final String HORIZONTAL_LINE = "# ================================================";
    
    static final void genSplit () {
        UnicodeSet split = new UnicodeSet();
        UnicodeSet reordrant = new UnicodeSet(
            "[\u093F\u09BF\u09c7\u09c8\u0abf\u0abf\u0b47\u0bc6\u0bc7\u0bc8"
            + "\u0d46\u0d47\u0d48\u0dd9\u0dda\u0ddb\u1031\u17be\u17c1\u17c2\u17c3]");
        UnicodeSet subjoined = new UnicodeSet();
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!Default.ucd().isAssigned(i)) continue;
            Utility.dot(i);
            int cat = Default.ucd().getCategory(i);
            if (cat != Mc && cat != Mn && cat != Me) continue;
            if (Default.ucd().getName(i).indexOf("SUBJOINED") >= 0) {
                System.out.print('*');
                subjoined.add(i);
                continue;
            }
            String decomp = Default.nfd().normalize(i);
            //int count = countTypes(decomp, Mc);
            if (UTF16.countCodePoint(decomp) > 1) split.add(i);
        }
        Utility.fixDot();
        System.out.println("Split: " + split.size());
        Utility.showSetNames("", split, false, Default.ucd());
        
        System.out.println("Reordrant: " + reordrant.size());
        Utility.showSetNames("", reordrant, false, Default.ucd());
        
        System.out.println("Subjoined: " + subjoined.size());
        Utility.showSetNames("", subjoined, false, Default.ucd());
    }
    
    static int countTypes(String s, int filter) {
        int count = 0;
        int cp;
        for (int i = 0; i < s.length(); i+= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            int cat = Default.ucd().getCategory(i);
            if (cat == filter) count++;
        }
        return count;
    }

    //static UnifiedBinaryProperty ubp
        
    public static void checkHoffman(String test) {
        String result = Default.nfkc().normalize(test);
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
            String cc = " " + Default.ucd().getCombiningClass(cp);
            cc = Utility.repeat(" ", 4 - cc.length()) + cc;
            System.out.println(Utility.repeat(" ", indent) + Default.ucd().getCode(cp) + cc + " " + Default.ucd().getName(cp));
            String decomp = Default.nfkc().normalize(cp);
            if (!decomp.equals(UTF32.valueOf32(cp))) {
                show(decomp, indent + 4);
            }
        }
    }


    static final int HEADER_EXTEND = 0, HEADER_DERIVED = 1, HEADER_SCRIPTS = 2;

    public static void doHeader(String fileName, PrintWriter output, int headerChoice) {
        output.println("# " + fileName);
        output.println(UnicodeDataFile.generateDateLine());
        output.println("#");
        if (headerChoice == HEADER_SCRIPTS) {
        } else if (headerChoice == HEADER_EXTEND) {
            output.println("# Unicode Character Database: Extended Properties");
        } else {
            output.println("# Unicode Character Database: Derived Property Data");
            output.println("# Generated algorithmically from the Unicode Character Database");
        }
        output.println("# For documentation, see UCD.html");
        //output.println("# Note: Unassigned and Noncharacter codepoints may be omitted");
        //output.println("#       if they have default property values.");
        output.println(HORIZONTAL_LINE);
        output.println();
    }

    public static void checkDifferences (String targetVersion) throws IOException {
        System.out.println("Checking Differences");
        UCD target = UCD.make(targetVersion);
        
        PrintWriter log1 = Utility.openPrintWriter("Log1.xml", Utility.LATIN1_UNIX);
        log1.println("<diff version='" + target.getVersion() + "'>");

        PrintWriter log2 = Utility.openPrintWriter("Log2.xml", Utility.LATIN1_UNIX);
        log2.println("<diff version='" + Default.ucd().getVersion() + "'>");
        
        for (int i = 0; i <= 0x10FFFF; ++i) {
            if (!target.isAllocated(i)) continue;
            Utility.dot(i);
            UData t = target.get(i, true);
            UData current = Default.ucd().get(i, true);
            if (i == 0x5E) {
                System.out.println(target.getDecompositionTypeID(i) 
                    + ", " + Utility.hex(target.getDecompositionMapping(i)));
                System.out.println(Default.ucd().getDecompositionTypeID(i) 
                    + ", " + Utility.hex(Default.ucd().getDecompositionMapping(i)));
            }
            if (t.equals(current)) continue;
            
            // print both for comparison
            log1.println(t.toString(target, UData.ABBREVIATED));
            log2.println(current.toString(Default.ucd(), UData.ABBREVIATED));
        }
        log1.println("</diff>");
        log2.println("</diff>");
        log1.close();
        log2.close();
    }
    
    public static void generateDerived (byte type, boolean checkTypeAndStandard, int headerChoice, String directory, String fileName) throws IOException {

        
        String newFile = directory + fileName + UnicodeDataFile.getFileSuffix(true);
        System.out.println("New File: " + newFile);
        PrintWriter output = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        org.unicode.cldr.util.Utility.generateBat(directory, fileName, UnicodeDataFile.getFileSuffix(true), batName);
        
        doHeader(fileName + UnicodeDataFile.getFileSuffix(false), output, headerChoice);
        for (int i = 0; i < DERIVED_PROPERTY_LIMIT; ++i) {
            UCDProperty up = DerivedProperty.make(i, Default.ucd());
            if (up == null) continue;
            boolean keepGoing = true;
            if (!up.isStandard()) keepGoing = false;
            if ((up.getType() & type) == 0) keepGoing = false;
            
            if (checkTypeAndStandard != keepGoing) continue;
            //if ((bitMask & (1L<<i)) == 0) continue;
            
            System.out.print('.');
            output.println(HORIZONTAL_LINE);
            output.println();
            new DerivedPropertyLister(Default.ucd(), i, output).print();
            output.flush();
        }
        output.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
    }

    
    public static void listStrings(String file, int type, int subtype) throws IOException {
        Default.ucd = UCD.make("3.1.0");
        UCD ucd30 = UCD.make("3.0.0");
        PrintStream output = new PrintStream(new FileOutputStream(GEN_DIR + file));

        for (int i = 0; i < 0x10FFFF; ++i) {
            if ((i & 0xFFF) == 0) System.out.println("# " + i);
            if (!Default.ucd.isRepresented(i)) continue;
            if (ucd30.isRepresented(i)) continue;
            String string = "";
            switch(type) {
                case 0: string = Default.ucd.getSimpleLowercase(i);
            }
            if (UTF32.length32(string) == 1 && UTF32.char32At(string,0) == i) continue;
            output.println(Utility.hex(i) + "; C; " + Utility.hex(string) + "; # " + Default.ucd.getName(i));
        }
        output.close();
    }
    

    public static void generateCompExclusions() throws IOException {
        
        String newFile = "DerivedData/CompositionExclusions" + UnicodeDataFile.getFileSuffix(true);
        PrintWriter output = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = org.unicode.cldr.util.Utility.generateBat("DerivedData/", "CompositionExclusions", UnicodeDataFile.getFileSuffix(true), batName);
        
        output.println("# CompositionExclusions" + UnicodeDataFile.getFileSuffix(false));
        output.println(UnicodeDataFile.generateDateLine());
        output.println("#");
        output.println("# This file lists the characters from the UAX #15 Composition Exclusion Table.");
        output.println("#");
        if (Default.ucd().getVersion().equals("3.2.0")) {
            output.println("# The format of the comments in this file has been updated since the last version,");
            output.println("# CompositionExclusions-3.txt. The only substantive change to this file between that");
            output.println("# version and this one is the addition of U+2ADC FORKING.");
            output.println("#");
        }
        output.println("# For more information, see");
        output.println("# http://www.unicode.org/unicode/reports/tr15/#Primary Exclusion List Table");
        output.println(HORIZONTAL_LINE);
        output.println();
        output.println("# (1) Script Specifics");
        output.println("# This list of characters cannot be derived from the UnicodeData file.");
        output.println(HORIZONTAL_LINE);
        output.println();
        
        new CompLister(output, 1).print();
        
        output.println(HORIZONTAL_LINE);
        output.println("# (2) Post Composition Version precomposed characters");
        output.println("# These characters cannot be derived solely from the UnicodeData.txt file");
        output.println("# in this version of Unicode.");
        output.println(HORIZONTAL_LINE);
        output.println();
        
        new CompLister(output, 2).print();

        output.println(HORIZONTAL_LINE);
        output.println("# (3) Singleton Decompositions");
        output.println("# These characters can be derived from the UnicodeData file");
        output.println("# by including all characters whose canonical decomposition");
        output.println("# consists of a single character.");
        output.println("# These characters are simply quoted here for reference.");
        output.println(HORIZONTAL_LINE);
        output.println();

        new CompLister(output, 3).print();
        
        output.println(HORIZONTAL_LINE);
        output.println("# (4) Non-Starter Decompositions");
        output.println("# These characters can be derived from the UnicodeData file");
        output.println("# by including all characters whose canonical decomposition consists");
        output.println("# of a sequence of characters, the first of which has a non-zero");
        output.println("# combining class.");
        output.println("# These characters are simply quoted here for reference.");        
        output.println(HORIZONTAL_LINE);
        output.println();
        new CompLister(output, 4).print();
        
        output.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
    }
    
    static class CompLister extends PropertyLister {
        UCD oldUCD;
        int type;

        public CompLister(PrintWriter output, int type) {
            this.output = output;
            ucdData = Default.ucd();
            oldUCD = UCD.make("3.0.0");
            // showOnConsole = true;
            alwaysBreaks = type <= 2; // CHANGE LATER
            commentOut = type > 2;
            this.type = type;
        }

        public String optionalComment(int cp) { return ""; }
        
        public String valueName(int cp) {
            return UTF32.length32(ucdData.getDecompositionMapping(cp)) + "";
        }
        
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

    public static void generatePropertyAliases() throws IOException {
        
        String prop = "";
        String propAbb = "";
        String value = "";
        String valueAbb = "";
        
        Map duplicates = new TreeMap();
        Set sorted = new TreeSet(java.text.Collator.getInstance());
        Set accumulation = new TreeSet(java.text.Collator.getInstance());
        
        for (int k = 0; k < UCD_Names.NON_ENUMERATED_NAMES.length; ++k) {
            propAbb = Utility.getUnskeleton(UCD_Names.NON_ENUMERATED_NAMES[k][0], false);
            prop = Utility.getUnskeleton(UCD_Names.NON_ENUMERATED_NAMES[k][1], true);
            
            byte type = STRING_PROP;
            if (propAbb.equals("nv")) {
                type = NUMERIC_PROP;
            } else if (propAbb.equals("age")) {
                type = CATALOG_PROP;
            } else if (propAbb.equals("blk")) {
                type = CATALOG_PROP;
            } else if (propAbb.equals("na")) {
                type = MISC_PROP;
            } else if (propAbb.equals("na1")) {
                type = MISC_PROP;
            } else if (propAbb.equals("isc")) {
                type = MISC_PROP;
            }
            addLine(sorted, UCD_Names.PROP_TYPE_NAMES[type][1], propAbb, prop, null);
            checkDuplicate(duplicates, accumulation, propAbb, prop);
            if (!prop.equals(propAbb)) checkDuplicate(duplicates, accumulation, prop, prop);
        }
        addLine(sorted, UCD_Names.PROP_TYPE_NAMES[MISC_PROP][1], "URS", "Unicode_Radical_Stroke", null);
        // TODO: merge above
 
        for (int k = 0; k < UCD_Names.SUPER_CATEGORIES.length; ++k) {
            valueAbb = Utility.getUnskeleton(UCD_Names.SUPER_CATEGORIES[k][0], false);
            value = Utility.getUnskeleton(UCD_Names.SUPER_CATEGORIES[k][1], true);
            String extra = Utility.getUnskeleton(UCD_Names.SUPER_CATEGORIES[k][1], true);
            addLine(sorted, "gc", valueAbb, value, extra, "# " + UCD_Names.SUPER_CATEGORIES[k][2]);
            checkDuplicate(duplicates, accumulation, value, "General_Category=" + value);
            if (!value.equals(valueAbb)) checkDuplicate(duplicates, accumulation, valueAbb, "General_Category=" + value);
            if (extra != null) checkDuplicate(duplicates, accumulation, extra, "General_Category=" + value);
        }
        
        
        addLine(sorted, "xx; T         ; True");
        checkDuplicate(duplicates, accumulation, "T", "xx=True");
        addLine(sorted, "xx; F         ; False");
        checkDuplicate(duplicates, accumulation, "F", "xx=False");
        
        addLine(sorted, "qc", UCD_Names.YN_TABLE[1], UCD_Names.YN_TABLE_LONG[1], null);
        checkDuplicate(duplicates, accumulation, UCD_Names.YN_TABLE[1], "qc=" + UCD_Names.YN_TABLE_LONG[1]);
        addLine(sorted, "qc", UCD_Names.YN_TABLE[0], UCD_Names.YN_TABLE_LONG[0], null);
        checkDuplicate(duplicates, accumulation, UCD_Names.YN_TABLE[0], "qc=" + UCD_Names.YN_TABLE_LONG[0]);
        addLine(sorted, "qc", "M", "Maybe", null);
        checkDuplicate(duplicates, accumulation, "M", "qc=Maybe");
        
        addLine(sorted, "blk", "n/a", Utility.getUnskeleton("no block", true), null);
        
        for (int i = 0; i < LIMIT_ENUM; ++i) {
            int type = i & 0xFF00;
            if (type == AGE) continue;
            if (i == (BINARY_PROPERTIES | CaseFoldTurkishI)) continue;
            if (i == (BINARY_PROPERTIES | Non_break)) continue;
            if (i == (BINARY_PROPERTIES | Case_Sensitive)) continue;
            
            if (type == NUMERIC_TYPE) {
                //System.out.println("debug");
            }
            
            UCDProperty up = UnifiedBinaryProperty.make(i, Default.ucd());
            if (up == null) continue;
            if (!up.isStandard()) continue;
            
            // System.out.println("At" + Utility.hex(i));
            
            // Save the Type Name, under BB for binary
            
            if (type == i || type == BINARY_PROPERTIES || type == DERIVED) {
                if (propAbb.equals("") || propAbb.equals(UCD_Names.YN_TABLE[1])) {
                    System.out.println("WHOOPS: " + Utility.hex(i));
                }
                propAbb = Utility.getUnskeleton(up.getPropertyName(SHORT), false);
                prop = Utility.getUnskeleton(up.getPropertyName(LONG), true);
                addLine(sorted, 
                    type == SCRIPT
                    ? UCD_Names.PROP_TYPE_NAMES[CATALOG_PROP][1]
                    : type != DERIVED && type != BINARY_PROPERTIES 
                    ? UCD_Names.PROP_TYPE_NAMES[ENUMERATED_PROP][1] 
                    : UCD_Names.PROP_TYPE_NAMES[up.getValueType()][1], 
                    propAbb, prop, null);
                checkDuplicate(duplicates, accumulation, propAbb, prop);
                if (!prop.equals(propAbb)) checkDuplicate(duplicates, accumulation, prop, prop);
            }
            
            if (up.getValueType() < BINARY_PROP) continue;
            value = up.getValue(LONG);
            if (value.length() == 0) value = "none";
            else if (value.equals(UnicodeProperty.UNUSED)) continue;

            if (type != DECOMPOSITION_TYPE) {
                value = Utility.getUnskeleton(value, true);
            }
            
            //if (type == DERIVED) {
                //System.out.println("Derived " + up.getProperty());        
            //}
            
            
            if (type == SCRIPT) {
                value = Default.ucd().getCase(value, FULL, TITLE);
            }
            
            valueAbb = up.getValue(SHORT);
            valueAbb = Utility.getUnskeleton(valueAbb, false);
            if (valueAbb.length() == 0) valueAbb = "n/a";
            //else if (valueAbb.equals(value)) valueAbb = "n/a";

            
            if (type == COMBINING_CLASS) {
                if (value.charAt(0) <= '9') { continue; }
            }
            
            
            if (type == JOINING_GROUP) {
                valueAbb = "n/a";
            }
            
            
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
            
            
            
            if (type == BINARY_PROPERTIES || type == DERIVED) {
                //if (value.equals(YN_TABLE_LONG[1])) continue;
                addLine(sorted, PROP_TYPE_NAMES[BINARY][1], valueAbb, value);
                checkDuplicate(duplicates, accumulation, value, value);
                if (!value.equalsIgnoreCase(valueAbb)) checkDuplicate(duplicates, accumulation, valueAbb, value);
                continue;
            }
            
            
            if (type == COMBINING_CLASS) {
                String num = up.getValue(NUMBER);
                num = "; " + Utility.repeat(" ", 3-num.length()) + num;
                addLine(sorted, propAbb + num, valueAbb, value, null);
            } else if (!valueAbb.equals(UCD_Names.YN_TABLE[1])) {
                addLine(sorted, propAbb, valueAbb, value, null);
            }
            checkDuplicate(duplicates, accumulation, value, prop + "=" + value);
            if (!value.equalsIgnoreCase(valueAbb) && !valueAbb.equals("n/a")) {
                checkDuplicate(duplicates, accumulation, valueAbb, prop + "=" + value);
            }
        }
        
        Iterator blockIterator = Default.ucd().getBlockNames().iterator();
        while (blockIterator.hasNext()) {
            addLine(sorted, "blk", "n/a", (String)blockIterator.next(), null);
        }
        
        UCD.BlockData blockData = new UCD.BlockData();
        
        int blockId = 0;
        while (Default.ucd().getBlockData(blockId++, blockData)) {
            addLine(sorted, "blk", "n/a", blockData.name);
        }
        
        
        String filename = "PropertyAliases";
        String newFile = "DerivedData/" + filename + UnicodeDataFile.getFileSuffix(true);
        PrintWriter log = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = org.unicode.cldr.util.Utility.generateBat("DerivedData/", filename, UnicodeDataFile.getFileSuffix(true), batName);
        
        log.println("# " + filename + UnicodeDataFile.getFileSuffix(false));
        log.println(UnicodeDataFile.generateDateLine());
        log.println("#");
        Utility.appendFile("PropertyAliasesHeader.txt", Utility.LATIN1, log);
        log.println(HORIZONTAL_LINE);
        log.println();
        int count = Utility.print(log, sorted, "\r\n", new MyBreaker(true));
        log.println();
        log.println();
        log.println(HORIZONTAL_LINE);
        log.println("# Total: \t" + count);
        log.println();
        log.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
        
        filename = "PropertyValueAliases";
        newFile = "DerivedData/" + filename + UnicodeDataFile.getFileSuffix(true);
        log = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        mostRecent = org.unicode.cldr.util.Utility.generateBat("DerivedData/", filename, UnicodeDataFile.getFileSuffix(true), batName);
        
        log.println("# " + filename + UnicodeDataFile.getFileSuffix(false));
        log.println(UnicodeDataFile.generateDateLine());
        log.println("#");
        Utility.appendFile("PropertyValueAliasesHeader.txt", Utility.LATIN1, log);
        log.println(HORIZONTAL_LINE);
        log.println();
        Utility.print(log, sorted, "\r\n", new MyBreaker(false));
        log.println();
        log.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
        
        filename = "PropertyAliasSummary";
        newFile = "OtherData/" + filename + UnicodeDataFile.getFileSuffix(true);
        log = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        mostRecent = org.unicode.cldr.util.Utility.generateBat("OtherData/", filename, UnicodeDataFile.getFileSuffix(true), batName);
        
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
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
    }
    
    static void addLine(Set sorted, String f1, String f2, String f3, String f4) {
        addLine(sorted, f1, f2, f3, f4, null);
    }
    
    static void addLine(Set sorted, String f1, String f2, String f3, String f4, String comment) {
        //System.out.println("Adding: " + line);
        f1 += Utility.repeat(" ", 3 - f1.length());
        f1 += "; " + f2;
        f1 += Utility.repeat(" ", 15 - f1.length());
        f1 += "; " + f3;
        if (f4 != null) {
            f1 += Utility.repeat(" ", 30 - f1.length());
            f1 += f4;
        }
        if (comment != null) {
            f1 += Utility.repeat(" ", 50 - f1.length());
            f1 += comment;
        }
        sorted.add(f1);
    }
    
    static class MyBreaker implements Utility.Breaker {
        boolean status;
        int count;
        
        public MyBreaker(boolean status) {
            this.status = status;
        }
        
        public byte getType (String c) {
            for (byte i = 0; i <= BINARY_PROP; ++i) {
                if (c.startsWith(UCD_Names.PROP_TYPE_NAMES[i][1])) return i;
            }
            return UNKNOWN_PROP;
        }
        
        public boolean filter(Object current) {
            String c = current.toString();
            byte type = getType(c);
            if (type != UNKNOWN_PROP) return status;
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
                    byte type = getType(c);
                    sep = sep + HORIZONTAL_LINE + sep + "# " + UCD_Names.PROP_TYPE_NAMES[type][0] + " Properties" + sep + HORIZONTAL_LINE + sep;
                }
            }
            if (status) {
                int pos = c.indexOf(';');
                c = c.substring(pos+1).trim();
            }
            return sep + c;
        }
    }
    
    static void checkDuplicate(Map m, Set accumulation, String toCheck, String originalComment) {
        toCheck = Utility.getSkeleton(toCheck);
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
            
            String acc = (String)accumulation.get(toCheck);
            if (acc == null) {
                acc = "# \"" + toCheck + "\":\t" + originalComment;
            }
            acc += ";\t" + result;
            
            result.add(comment);
            accumulation.add("# " + result.toString() + ":\t" + toCheck);
        } else {
            result = new TreeSet();
            result.add(comment);
            m.put(toCheck, result);
        }
    }
    
    public static void generateVerticalSlice(int startEnum, int endEnum,
            int headerChoice, String directory, String file) throws IOException {

        
        String newFile = directory + file + UnicodeDataFile.getFileSuffix(true);
        PrintWriter output = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = org.unicode.cldr.util.Utility.generateBat(directory, file, UnicodeDataFile.getFileSuffix(true), batName);
        
        doHeader(file + UnicodeDataFile.getFileSuffix(false), output, headerChoice);
        int last = -1;
        for (int i = startEnum; i < endEnum; ++i) {
            UCDProperty up = UnifiedBinaryProperty.make(i, Default.ucd());
            if (up == null) continue;
            if (up.skipInDerivedListing()) continue;
            
            
            if (i == DECOMPOSITION_TYPE || i == NUMERIC_TYPE
                || i == (BINARY_PROPERTIES | Non_break)
                || i == (BINARY_PROPERTIES | CaseFoldTurkishI)
                || i == (HANGUL_SYLLABLE_TYPE | NA)
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
            new MyPropertyLister(Default.ucd(), i, output).print();
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

            Set numericValueSet = new TreeSet();
            for (int i = 0; i < 0x10FFFF; ++i) {
                double nv = Default.ucd().getNumericValue(i);
                if (Double.isNaN(nv)) continue;
                numericValueSet.add(new Double(nv));
            }
            Iterator it = numericValueSet.iterator();
            while(it.hasNext()) {
                new MyFloatLister(Default.ucd(), ((Double)it.next()).doubleValue(), output).print();
                output.println();
                System.out.print(".");
            }
            output.flush();
        }
        output.close();
        //System.out.println("HERE");
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
        System.out.println();
    }

    */    
    static public void writeNormalizerTestSuite(String directory, String fileName) throws IOException {
        
    	UnicodeDataFile fc = UnicodeDataFile.openAndWriteHeader(directory, fileName);
        PrintWriter log = fc.out;
        
        String newFile = directory + fileName + UnicodeDataFile.getFileSuffix(true);
        //PrintWriter log = Utility.openPrintWriter(newFile, Utility.UTF8_UNIX);
        //String[] batName = {""};
        //String mostRecent = org.unicode.cldr.util.Utility.generateBat(directory, fileName, UnicodeDataFile.getFileSuffix(true), batName);

        String[] example = new String[256];

        //log.println("# " + fileName + UnicodeDataFile.getFileSuffix(false));
        //log.println(UnicodeDataFile.generateDateLine());
//        log.println("#");
//        log.println("# Normalization Test Suite");
//        log.println("# Format:");
//        log.println("#");
//        log.println("#   Columns (c1, c2,...) are separated by semicolons");
//        log.println("#   Comments are indicated with hash marks");
//        log.println("#");
//        log.println("# CONFORMANCE:");
//        log.println("# 1. The following invariants must be true for all conformant implementations");
//        log.println("#");
//        log.println("#    NFC");
//        log.println("#      c2 ==  NFC(c1) ==  NFC(c2) ==  NFC(c3)");
//        log.println("#      c4 ==  NFC(c4) ==  NFC(c5)");
//        log.println("#");
//        log.println("#    NFD");
//        log.println("#      c3 ==  NFD(c1) ==  NFD(c2) ==  NFD(c3)");
//        log.println("#      c5 ==  NFD(c4) ==  NFD(c5)");
//        log.println("#");
//        log.println("#    NFKC");
//        log.println("#      c4 == NFKC(c1) == NFKC(c2) == NFKC(c3) == NFKC(c4) == NFKC(c5)");
//        log.println("#");
//        log.println("#    NFKD");
//        log.println("#      c5 == NFKD(c1) == NFKD(c2) == NFKD(c3) == NFKD(c4) == NFKD(c5)");
//        log.println("#");
//        log.println("# 2. For every code point X assigned in this version of Unicode that is not specifically");
//        log.println("#    listed in Part 1, the following invariants must be true for all conformant");
//        log.println("#    implementations:");
//        log.println("#");
//        log.println("#      X == NFC(X) == NFD(X) == NFKC(X) == NFKD(X)");

        System.out.println("Writing Part 1");

//        log.println("#");
//        log.println("@Part0 # Specific cases");
//        log.println("#");

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
            if (!Default.ucd().isAssigned(ch)) continue;
            if (Default.ucd().isPUA(ch)) continue;
            String cc = UTF32.valueOf32(ch);
            writeLine(cc,log, true);
        }
        Utility.fixDot();

        System.out.println("Finding Examples");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) continue;
            if (Default.ucd().isPUA(ch)) continue;
            int cc = Default.ucd().getCombiningClass(ch);
            if (example[cc] == null) example[cc] = UTF32.valueOf32(ch);
        }

        Utility.fixDot();
        System.out.println("Writing Part 2");

        log.println("#");
        log.println("@Part2 # Canonical Order Test");
        log.println("#");

        for (int ch = 0; ch < 0x10FFFF; ++ch) {

            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) continue;
            if (Default.ucd().isPUA(ch)) continue;
            short c = Default.ucd().getCombiningClass(ch);
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
        
        System.out.println("Writing Part 3");
        log.println("#");
        log.println("@Part3 # PRI #29 Test");
        log.println("#");
        
        Set prilist = new TreeSet();
        
        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd().isAssigned(ch)) continue;
            if (Default.ucd().isPUA(ch)) continue;
            if (0xAC00 <= ch && ch <= 0xD7FF) { // skip most
            	if (((ch - 0xAC00) % 91) != 0) continue;
            }
           // also gather data for pri29 test
            if (ch == 0x09CB) {
            	System.out.println("debug");
            }
            if (Default.ucd().getDecompositionType(ch) != CANONICAL) continue;
            //if (!Default.nfc().isNormalized(ch)) continue;
            String s = Default.ucd().getDecompositionMapping(ch);
            if (UTF16.hasMoreCodePointsThan(s, 2)) continue;
            if (!UTF16.hasMoreCodePointsThan(s, 1)) continue;
            int c1 = UTF16.charAt(s, 0);
            int c2 = UTF16.charAt(s, UTF16.getCharCount(c1));
            if (Default.ucd().getCombiningClass(c1) != 0) continue;
            if (Default.ucd().getCombiningClass(c2) != 0) continue;
            prilist.add(UTF16.valueOf(c1) + '\u0334' + UTF16.valueOf(c2));
        }
        Utility.fixDot();
        
        for (Iterator it = prilist.iterator(); it.hasNext();) {
            writeLine((String)it.next(),log, false);
        }

        Utility.fixDot();
        log.println("#");
        log.println("# END OF FILE");
        fc.close();
        //Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
    }
    /*
    
    static void handleIdentical() throws IOException {
        DirectoryIterator target = new DirectoryIterator(GEN_DIR + File.separator + "DerivedData");
        DirectoryIterator.RootFileFilter filter = new DirectoryIterator.RootFileFilter("");
        DirectoryIterator recent = new DirectoryIterator(UCD_DIR, filter);
        while (true) {
            File targetFile = target.next();
            if (targetFile == null) break;
            recent.reset();
            filter.setRoot(DirectoryIterator.getRoot(targetFile));
            File lastFile = recent.next();
            if (lastFile == null) break;
            System.out.println("Target: " + targetFile);
            System.out.println("Last: " + lastFile);
            if (!DirectoryIterator.isAlmostIdentical(targetFile, lastFile, true)) continue;
            System.out.println("Almost Identical");
        }
    }

*/
    static void writeLine(String cc, PrintWriter log, boolean check) {
        String c = Default.nfc().normalize(cc);
        String d = Default.nfd().normalize(cc);
        String kc = Default.nfkc().normalize(cc);
        String kd = Default.nfkd().normalize(cc);
        if (check & cc.equals(c) && cc.equals(d) && cc.equals(kc) && cc.equals(kd)) return;

        // consistency check
        String dc = Default.nfd().normalize(c);
        String dkc = Default.nfd().normalize(kc);
        if (!dc.equals(d) || !dkc.equals(kd)) {
            System.out.println("Danger Will Robinson!");
            Normalizer.SHOW_PROGRESS = true;
            d = Default.nfd().normalize(cc);
        }

        // printout
        log.println(
            Utility.hex(cc," ") + ";" + Utility.hex(c," ") + ";" + Utility.hex(d," ") + ";"
            + Utility.hex(kc," ") + ";" + Utility.hex(kd," ")
            + "; # ("
            + comma(cc) + "; " + comma(c) + "; " + comma(d) + "; " + comma(kc) + "; " + comma(kd) + "; "
            + ") " + Default.ucd().getName(cc));
    }

    static StringBuffer commaResult = new StringBuffer();

    // not recursive!!!
    static final String comma(String s) {
    	//if (true) return s;
        commaResult.setLength(0);
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (Default.ucd().getCategory(cp) == Mn) commaResult.append('\u25CC');
            UTF16.append(commaResult, cp);
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
    /*
    static final void backwardsCompat(String directory, String filename, int[] list) throws IOException {
    
        
        String newFile = directory + filename + UnicodeDataFile.getFileSuffix(true);
        PrintWriter log = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = org.unicode.cldr.util.Utility.generateBat(directory, filename, UnicodeDataFile.getFileSuffix(true), batName);
        DiffPropertyLister dpl;
        UnicodeSet cummulative = new UnicodeSet();
        
        try {
        	for (int i = 0; i < list.length; ++i) {
        		int prop = list[i];
            	log.println();
            	log.println(HORIZONTAL_LINE);
        		log.println("###### " + DerivedProperty.make(prop, Default.ucd()).getName());
            	//log.println();
            	//log.println(HORIZONTAL_LINE);
            	//new DiffPropertyLister("3.2.0", "1.1.0", log, prop).print();
            	log.println();
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "2.0.0", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "2.1.2", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "2.1.5", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "2.1.8", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "3.0.0", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "3.0.1", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "3.1.0", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	dpl = new DiffPropertyLister("3.2.0", "3.1.1", log, prop);
            	dpl.print();
            	cummulative.addAll(dpl.getSet());
            	log.println(HORIZONTAL_LINE);
            	
            	log.println();
            	log.println("Cummulative differences");
            	UCDProperty up = DerivedProperty.make(prop, Default.ucd());
            	UnicodeSet newProp = up.getSet();
            	Utility.showSetNames(log, "", cummulative.removeAll(newProp), false, false, Default.ucd());
            }
        } finally {
            if (log != null) {
                log.close();
                Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
            }
        }
    }
    
    static final void generateAge(String directory, String filename) throws IOException {
        
        String newFile = directory + filename + UnicodeDataFile.getFileSuffix(true);
        PrintWriter log = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String[] batName = {""};
        String mostRecent = org.unicode.cldr.util.Utility.generateBat(directory, filename, UnicodeDataFile.getFileSuffix(true), batName);
        try {
            log.println("# " + filename + UnicodeDataFile.getFileSuffix(false));
            log.println(UnicodeDataFile.generateDateLine());
            log.println("#");
            log.println("# Unicode Character Database: Derived Property Data");
            log.println("# This file shows when various code points were designated in Unicode");
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
            
            // http://www.unicode.org/versions/enumeratedversions.html
            
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
            log.println(HORIZONTAL_LINE);
            log.println();
            new DiffPropertyLister("3.2.0", "4.0.0", log).print();
            
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
            
        } finally {
            if (log != null) {
                log.close();
                Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName[0]);
            }
        }

    }
    
    public static void listCombiningAccents() throws IOException {
        
        PrintWriter log = Utility.openPrintWriter("ListAccents" + UnicodeDataFile.getFileSuffix(true), Utility.LATIN1_UNIX);
        Set set = new TreeSet();
        Set set2 = new TreeSet();
        
        for (int i = 0; i < 0x10FFFF; ++i) {
            Utility.dot(i);
            if (!Default.ucd().isRepresented(i)) continue;
            
            if (Default.nfd().isNormalized(i)) {
                if (Default.ucd().getScript(i) == LATIN_SCRIPT) {
                    int cp = i;
                    String hex = "u" + Utility.hex(cp, 4);
                    set.add("# yyy $x <> \\" + hex + " ; # " + Default.ucd().getName(cp));
                }
                continue;
            }
            
            String decomp = Default.nfd().normalize(i);
            int j;
            for (j = 0; j < decomp.length(); j += UTF16.getCharCount(i)) {
                int cp = UTF16.charAt(decomp, j);
                byte cat = Default.ucd().getCategory(cp);
                if (cat != Mn) continue;
                String hex = "u" + Utility.hex(cp, 4);
                set.add("# xxx $x <> \\" + hex + " ; # " + Default.ucd().getName(cp));
            }
        }
        
        Iterator it = set.iterator();
        while (it.hasNext()) {
            log.println(it.next());
        }
        log.close();
    }
    
    public static void listGreekVowels() throws IOException {
        
        PrintWriter log = Utility.openPrintWriter("ListGreekVowels" + UnicodeDataFile.getFileSuffix(true), Utility.LATIN1_UNIX);
        Set set = new TreeSet();
        Set set2 = new TreeSet();
        
        String vowels = "\u03B1\u03B5\u03B7\u03B9\u03BF\u03C5\u03C9\u0391\u0395\u0397\u0399\u039F\u03A5\u03A9";
        String diphthongEnd = "\u03B9\u03C5\u0399\u03A5";
        String diphthongStart = "\u03B1\u03B5\u03B7\u03BF\u03C5\u0391\u0395\u0397\u039F\u03A5";
        String etas = "\u03B7\u0397";
        String iotas = "\u03B9\u0399";
        
        for (char i = 0; i < 0xFFFF; ++i) {
            Utility.dot(i);
            if (!Default.ucd().isRepresented(i)) continue;
            if (Default.ucd().getScript(i) != GREEK_SCRIPT) continue;
            String decomp = Default.nfd().normalize(i);
            
            if (decomp.indexOf('\u0306') >= 0) continue; // skip breve
            if (decomp.indexOf('\u0304') >= 0) continue; // skip macron
            
            String comp = Default.nfc().normalize(decomp);
            if (!comp.equals(String.valueOf(i))) continue; // skip compats
            
            char first = decomp.charAt(0);
            
            if (vowels.indexOf(first) < 0) continue;
            
            String h = "";
            if (decomp.indexOf('\u0314') >= 0) h = "\uFFFF";
            
            if (diphthongEnd.indexOf(first) >= 0) {
                for (int j = 0; j < diphthongStart.length(); ++j) {
                    String v = diphthongStart.substring(j, j+1);
                    char vc = v.charAt(0);
                    if (Default.ucd().getCategory(vc) == Ll && Default.ucd().getCategory(first) == Lu) continue;
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
    
    public static void genTrailingZeros() {
        
        UnicodeSet result = new UnicodeSet();
        for (int i = 0; i < 0x10FFFF; ++i) {
            if ((i & 0xFFF) == 0) System.out.println("# " + i);
            if (!Default.ucd().isAssigned(i)) continue;
            if (Default.nfd().isNormalized(i)) continue;
            String decomp = Default.nfd().normalize(i);
            int cp;
            for (int j = 0; j < decomp.length(); j += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(decomp,j);
                if (j == 0) continue; // skip first
                if (Default.ucd().getCombiningClass(cp) == 0) {
                    result.add(cp);
                }
            }
        }
        int rangeCount = result.getRangeCount();
        for (int k = 0; k < rangeCount; ++k) {
            int start = result.getRangeStart(k);
            int end = result.getRangeEnd(k);
            System.out.println(
                Utility.hex(start)
                + (start != end ? ".." + Utility.hex(end) : "")
                + "; "
                + Default.ucd().getName(start)
                + (start != end ? ".." + Default.ucd().getName(end) : ""));
        }
        System.out.println("TrailingZero count: " + result.size());
    }*/
}