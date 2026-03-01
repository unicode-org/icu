// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.currency;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeSet;

/**
 * The tool used for ISO 4217 alpha3-numeric3 code mapping data maintenance.
 * This code is used for synchronizing ICU alpha3-numeric3 mapping data with
 * the data distributed by SIX Interbank Clearing.
 */
public class Main {
    private enum Command {
        CHECK,
        PRINT,
        BUILD,
    };

    private static final String RESNAME = "currencyNumericCodes";

    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        Command cmd = null;

        // 1st argument must be command
        if (args[0].equalsIgnoreCase(Command.CHECK.name())) {
            if (args.length == 3) {
                cmd = Command.CHECK;
            }
        } else if (args[0].equalsIgnoreCase(Command.PRINT.name())) {
            if (args.length == 3) {
                cmd = Command.PRINT;
            }
        } else if (args[0].equalsIgnoreCase(Command.BUILD.name())) {
            if (args.length == 2) {
                cmd = Command.BUILD;
            }
        }

        if (cmd == null) {
            printUsage();
            System.exit(1);
        }
 
        int status = 0;

        if (cmd == Command.BUILD) {
            File outfile = new File(args[1], RESNAME + ".txt");
            try {
                OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outfile), "UTF-8");
                NumericCodeData.getDefaultInstance().writeResource(osw, RESNAME);
            } catch (IOException e) {
                e.printStackTrace();
                status = 1;
            }
        } else {
            // 2nd argument is current data xml file
            // 3rd argument is historic data xml file
            File currentXml = new File(args[1]);
            File historicXml = new File(args[2]);
            Collection<CurrencyDataEntry> currentDataEntries = null;
            Collection<CurrencyDataEntry> historicDataEntries = null;

            try {
                currentDataEntries = CurrencyDataParser.parse(currentXml, false);
                historicDataEntries = CurrencyDataParser.parse(historicXml, true);
            } catch (IOException e) {
                e.printStackTrace();
                status = 1;
            }

            if (status == 0) {
                NumericCodeData numCodeData = NumericCodeData.createInstance(currentDataEntries);
                numCodeData.merge(NumericCodeData.createInstance(historicDataEntries));

                if (cmd == Command.PRINT) {
                    numCodeData.printArray();
                } else {
                    assert(cmd == Command.CHECK);
                    boolean isOK = checkData(numCodeData);
                    if (isOK) {
                        System.out.println("[OK] ICU data is synchronized with the reference data");
                    } else {
                        status = 1;
                    }
                }
            }
        }

        System.exit(status);
    }

    private static void printUsage() {
        System.out.println("[Usage]");
        System.out.println("");
        System.out.println("  1) java com.ibm.icu.dev.tool.currency.Main check <currentXML> <historicXML>");
        System.out.println("");
        System.out.println("  Verifies the ICU data (table in NumericCodeData) with the reference data.");
        System.out.println("");
        System.out.println("  Argument(s):");
        System.out.println("    <currentXML>  - Current currencies & funds data (Table A.1) in XML format");
        System.out.println("    <historicXML> - Historic denominations data (Table A.3) in XML format");
        System.out.println("");
        System.out.println("  2) java com.ibm.icu.dev.tool.currency.Main print <currentXML> <historicXML>");
        System.out.println("");
        System.out.println("  Prints out the alpha-numeric code mapping imported from the reference data.");
        System.out.println("");
        System.out.println("  Argument(s):");
        System.out.println("    <currentXML>  - Current currencies & funds data (Table A.1) in XML format");
        System.out.println("    <historicXML> - Historic denominations data (Table A.3) in XML format");
        System.out.println("");
        System.out.println("  3) java com.ibm.icu.dev.tool.currency.Main build <outtxt>");
        System.out.println("");
        System.out.println("  Writes out the alpha-numeric in NumericCodeData into ICU resource bundle source");
        System.out.println("  (.txt) format.");
        System.out.println("");
        System.out.println("  Argument(s):");
        System.out.println("    <outdir>      - Output directory of the ICU resource bundle source (.txt) format");
        System.out.println("");
        System.out.println("[Note]");
        System.out.println("  Reference XML files are distributed by the ISO 4217 maintenance agency at");
        System.out.println("  [http://www.currency-iso.org/iso_index/iso_tables.htm].");
    }

    private static boolean checkData(NumericCodeData refData) {
        boolean isOK = true;

        SortedMap<String, String> icuMap = NumericCodeData.getDefaultInstance().getAlphaNumericCodeMap();
        SortedMap<String, String> refMap = refData.getAlphaNumericCodeMap();

        for (Entry<String, String> refEntry : refMap.entrySet()) {
            String refAlpha = refEntry.getKey();
            String refNumeric = refEntry.getValue();

            String icuNumeric = icuMap.get(refAlpha);
            if (icuNumeric == null) {
                System.out.println("Missing alpha code in ICU map [" + refAlpha + "]");
                isOK = false;
            } else if (!icuNumeric.equals(refNumeric)) {
                System.out.println("Numeric code mismatch [" + refAlpha + "] ICU=" + icuNumeric + " - Reference=" + refNumeric);
                isOK = false;
            }
        }

        Set<String> icuKeySet = icuMap.keySet();
        Set<String> refKeySet = refMap.keySet();
        if (!refKeySet.containsAll(icuKeySet)) {
            isOK = false;
            Set<String> tmp = new TreeSet<String>(icuKeySet);
            tmp.removeAll(refKeySet);

            StringBuilder buf = new StringBuilder();
            for (String alpha : tmp) {
                if (buf.length() != 0) {
                    buf.append(", ");
                }
                buf.append(alpha);
            }
            
            System.out.println("Codes not found in the reference data: " + buf);
        }

        return isOK;
    }
}
