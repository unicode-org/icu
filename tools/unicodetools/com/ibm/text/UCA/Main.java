/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/Main.java,v $ 
* $Date: 2002/06/04 01:59:01 $ 
* $Revision: 1.5 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;
import com.ibm.text.UCD.*;
import com.ibm.text.utility.*;


public class Main {
	static final String UCDVersion = "";
	static final String[] ICU_FILES = {"writeCollationValidityLog", "FractionalUCA", "writeconformance", "writeconformanceshifted", 
		"WriteRules", "WriteRulesWithNames", "WriteRulesXML"};
	
	public static void main(String args[]) throws Exception {
		
		// NOTE: so far, we don't need to build the UCA with anything but the latest versions.
		// A few changes would need to be made to the code to do older versions.
        
        System.out.println("Building UCA");
        WriteCollationData.collator = new UCA(null, UCDVersion);
        System.out.println("Built version " + WriteCollationData.collator.getDataVersion()
        	+ "/ucd: " + WriteCollationData.collator.getUCDVersion());
        
        System.out.println("Building UCD data");
        WriteCollationData.ucd = UCD.make(WriteCollationData.collator.getUCDVersion());
        
        if (args.length == 0) args = new String[] {"?"}; // force the help comment
        boolean shortPrint = false;
        
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            System.out.println("OPTION: " + arg);
            
            if		(arg.equalsIgnoreCase("ICU")) args = Utility.append(args, ICU_FILES);
			else if (arg.equalsIgnoreCase("WriteRulesWithNames")) WriteCollationData.writeRules(WriteCollationData.WITH_NAMES);
            else if (arg.equalsIgnoreCase("GenOverlap")) GenOverlap.test(WriteCollationData.collator);
            else if (arg.equalsIgnoreCase("validateUCA")) GenOverlap.validateUCA(WriteCollationData.collator);
            else if (arg.equalsIgnoreCase("writeNonspacingDifference")) WriteCollationData.writeNonspacingDifference();
            
            else if (arg.equalsIgnoreCase("collationChart")) WriteCharts.collationChart(WriteCollationData.collator);
            else if (arg.equalsIgnoreCase("normalizationChart")) WriteCharts.normalizationChart();
            else if (arg.equalsIgnoreCase("caseChart")) WriteCharts.caseChart();
            else if (arg.equalsIgnoreCase("indexChart")) WriteCharts.indexChart();
            else if (arg.equalsIgnoreCase("special")) WriteCharts.special();
            
            
            else if (arg.equalsIgnoreCase("CheckHash")) GenOverlap.checkHash(WriteCollationData.collator);
            else if (arg.equalsIgnoreCase("generateRevision")) GenOverlap.generateRevision(WriteCollationData.collator);
            else if (arg.equalsIgnoreCase("listCyrillic")) GenOverlap.listCyrillic(WriteCollationData.collator);
            
            else if (arg.equalsIgnoreCase("WriteRules")) WriteCollationData.writeRules(WriteCollationData.WITHOUT_NAMES);
            else if (arg.equalsIgnoreCase("WriteRulesXML")) WriteCollationData.writeRules(WriteCollationData.IN_XML);
            else if (arg.equalsIgnoreCase("checkDisjointIgnorables")) WriteCollationData.checkDisjointIgnorables();
            else if (arg.equalsIgnoreCase("writeContractions")) WriteCollationData.writeContractions();
            else if (arg.equalsIgnoreCase("FractionalUCA")) WriteCollationData.writeFractionalUCA("FractionalUCA");
            else if (arg.equalsIgnoreCase("writeConformance")) WriteCollationData.writeConformance("CollationTest_NON_IGNORABLE", UCA.NON_IGNORABLE, shortPrint);
            else if (arg.equalsIgnoreCase("writeConformanceSHIFTED")) WriteCollationData.writeConformance("CollationTest_SHIFTED", UCA.SHIFTED, shortPrint);
            else if (arg.equalsIgnoreCase("testCompatibilityCharacters")) WriteCollationData.testCompatibilityCharacters();
            else if (arg.equalsIgnoreCase("writeCollationValidityLog")) WriteCollationData.writeCollationValidityLog();
            else if (arg.equalsIgnoreCase("writeCaseExceptions")) WriteCollationData.writeCaseExceptions();
            else if (arg.equalsIgnoreCase("writeJavascriptInfo")) WriteCollationData.writeJavascriptInfo();
            else if (arg.equalsIgnoreCase("writeCaseFolding")) WriteCollationData.writeCaseFolding();
            else if (arg.equalsIgnoreCase("javatest")) WriteCollationData.javatest();
            else if (arg.equalsIgnoreCase("short")) shortPrint = true;
            else {
                System.out.println();
                System.out.println("UNKNOWN OPTION (" + arg + "): must be one of the following (case-insensitive)");
                System.out.println("\tWriteRulesXML, WriteRulesWithNames, WriteRules,");
                System.out.println("\tcheckDisjointIgnorables, writeContractions,");
                System.out.println("\tFractionalUCA, writeConformance, writeConformanceSHIFTED, testCompatibilityCharacters,");
                System.out.println("\twriteCollationValidityLog, writeCaseExceptions, writeJavascriptInfo, writeCaseFolding");
                System.out.println("\tjavatest, hex (used for conformance)");
            }
        }        
        System.out.println("Done");
        
        /*
        String s = WriteCollationData.collator.getSortKey("\u1025\u102E", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0595\u0325") + ", " + WriteCollationData.collator.toString(s));
        String t = WriteCollationData.collator.getSortKey("\u0596\u0325", UCA.NON_IGNORABLE, true);
        System.out.println(Utility.hex("\u0596\u0325") + ", " + WriteCollationData.collator.toString(t));
        
        
        Normalizer foo = new Normalizer(Normalizer.NFKD);
        char x = '\u1EE2';
        System.out.println(Utility.hex(x) + " " + ucd.getName(x));
        String nx = foo.normalize(x);
        for (int i = 0; i < nx.length(); ++i) {
            char c = nx.charAt(i);
            System.out.println(ucd.getCanonicalClass(c));
        }
        System.out.println(Utility.hex(nx, " ") + " " + ucd.getName(nx));
        */
        
    }
}