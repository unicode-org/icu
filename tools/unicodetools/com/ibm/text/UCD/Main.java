/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/Main.java,v $
* $Date: 2001/09/06 01:29:48 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;

public final class Main {
    static String ucdVersion = "";

    public static void main (String[] args) throws Exception {

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line

            Utility.fixDot();
            System.out.println("Argument: " + args[i]);

            if (arg.equalsIgnoreCase("all")) {
                //checkCase();
                VerifyUCD.checkCanonicalProperties();
                VerifyUCD.CheckCaseFold();
                VerifyUCD.checkAgainstUInfo();

            } else if (arg.equalsIgnoreCase("build")) {
                ConvertUCD.main(new String[]{ucdVersion});
            } else if (arg.equalsIgnoreCase("version")) ucdVersion = args[++i];
            else if (arg.equalsIgnoreCase("generateXML")) VerifyUCD.generateXML();
            else if (arg.equalsIgnoreCase("checkSpeed")) VerifyUCD.checkSpeed();

            else if (arg.equalsIgnoreCase("testDerivedProperties")) DerivedProperty.test();
            else if (arg.equalsIgnoreCase("checkCase")) VerifyUCD.checkCase();
            else if (arg.equalsIgnoreCase("checkCase2")) VerifyUCD.checkCase2();
            else if (arg.equalsIgnoreCase("checkCanonicalProperties")) VerifyUCD.checkCanonicalProperties();
            else if (arg.equalsIgnoreCase("CheckCaseFold")) VerifyUCD.CheckCaseFold();
            else if (arg.equalsIgnoreCase("idn")) VerifyUCD.VerifyIDN();
            else if (arg.equalsIgnoreCase("NFTest")) VerifyUCD.NFTest();
            else if (arg.equalsIgnoreCase("test1")) VerifyUCD.test1();
            //else if (arg.equalsIgnoreCase("checkAgainstUInfo")) checkAgainstUInfo();
            else if (arg.equalsIgnoreCase("checkScripts")) VerifyUCD.checkScripts();
            else if (arg.equalsIgnoreCase("IdentifierTest")) VerifyUCD.IdentifierTest();
            else if (arg.equalsIgnoreCase("GenerateData")) GenerateData.main(Utility.split(args[++i],','));
            else if (arg.equalsIgnoreCase("BuildNames")) BuildNames.main(null);
            else if (arg.equalsIgnoreCase("writeNormalizerTestSuite"))
                GenerateData.writeNormalizerTestSuite("NormalizationTest-3.1.1d1.txt");
            else {
                System.out.println("Unknown option -- must be one of the following (case-insensitive)");
                System.out.println("generateXML, checkCase, checkCanonicalProperties, CheckCaseFold,");
                System.out.println("VerifyIDN, NFTest, test1, ");
                // System.out.println(checkAgainstUInfo,");
                System.out.println("checkScripts, IdentifierTest, writeNormalizerTestSuite");
            }
        }
    }
}