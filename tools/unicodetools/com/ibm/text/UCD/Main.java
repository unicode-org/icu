/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/Main.java,v $
* $Date: 2002/10/05 01:28:58 $
* $Revision: 1.25 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.text.utility.*;

public final class Main implements UCD_Types {
    
    static final String[] ALL_FILES = {
        "CaseFolding",
        "CompositionExclusions",
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
        "SpecialCasing",
        "DerivedAge",
        //"OtherDerivedProperties",
    };

    public static void main (String[] args) throws Exception {

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line

            Utility.fixDot();
            System.out.println("Argument: " + args[i]);

            if (arg.equalsIgnoreCase("verify")) {
                VerifyUCD.verify();
                VerifyUCD.checkCanonicalProperties();
                VerifyUCD.CheckCaseFold();
                VerifyUCD.checkAgainstUInfo();

            } else if (arg.equalsIgnoreCase("build")) ConvertUCD.main(new String[]{Default.ucdVersion});
            else if (arg.equalsIgnoreCase("version")) Default.setUCD(args[++i]);
            else if (arg.equalsIgnoreCase("statistics")) VerifyUCD.statistics();
            else if (arg.equalsIgnoreCase("NFSkippable")) NFSkippable.main(null);
            else if (arg.equalsIgnoreCase("diffIgnorable")) VerifyUCD.diffIgnorable();
            else if (arg.equalsIgnoreCase("generateXML")) VerifyUCD.generateXML();
            else if (arg.equalsIgnoreCase("checkSpeed")) VerifyUCD.checkSpeed();
            else if (arg.equalsIgnoreCase("onetime")) VerifyUCD.oneTime();
            else if (arg.equalsIgnoreCase("verifyNormalizationStability")) VerifyUCD.verifyNormalizationStability();
            
            else if (arg.equalsIgnoreCase("definitionTransliterator")) GenerateHanTransliterator.main(0);
            else if (arg.equalsIgnoreCase("romajiTransliterator")) GenerateHanTransliterator.main(1);
            else if (arg.equalsIgnoreCase("pinYinTransliterator")) GenerateHanTransliterator.main(2);
            else if (arg.equalsIgnoreCase("hanproperties")) GenerateHanTransliterator.readUnihan();
            
            else if (arg.equalsIgnoreCase("compareBlueberry")) VerifyUCD.compareBlueberry();
            
            else if (arg.equalsIgnoreCase("testenum")) SampleEnum.test();
            
            else if (arg.equalsIgnoreCase("quicktest")) QuickTest.test();
            else if (arg.equalsIgnoreCase("TernaryStore")) TernaryStore.test();
            
            else if (arg.equalsIgnoreCase("checkBIDI")) VerifyUCD.checkBIDI();
            else if (arg.equalsIgnoreCase("Buildnames")) BuildNames.main(null);
            else if (arg.equalsIgnoreCase("TestNormalization")) TestNormalization.main(null);
            
            
            else if (arg.equalsIgnoreCase("binary")) FastBinarySearch.test();
            
            else if (arg.equalsIgnoreCase("GenerateCaseTest")) GenerateCaseTest.main(null);
            else if (arg.equalsIgnoreCase("checkDecompFolding")) VerifyUCD.checkDecompFolding();
            
            else if (arg.equalsIgnoreCase("breaktest")) GenerateBreakTest.main(null);
            else if (arg.equalsIgnoreCase("checkcollator")) CheckCollator.main(null);

            else if (arg.equalsIgnoreCase("genSplit")) GenerateData.genSplit();
            else if (arg.equalsIgnoreCase("iana")) IANANames.testSensitivity();
            
            else if (arg.equalsIgnoreCase("testDerivedProperties")) DerivedProperty.test();
            else if (arg.equalsIgnoreCase("checkCase")) VerifyUCD.checkCase();
            else if (arg.equalsIgnoreCase("checkCase3")) VerifyUCD.checkCase3();
            else if (arg.equalsIgnoreCase("checkCaseLong")) VerifyUCD.checkCase2(true);
            else if (arg.equalsIgnoreCase("checkCaseShort")) VerifyUCD.checkCase2(false);
            else if (arg.equalsIgnoreCase("checkCanonicalProperties")) VerifyUCD.checkCanonicalProperties();
            else if (arg.equalsIgnoreCase("CheckCaseFold")) VerifyUCD.CheckCaseFold();
            else if (arg.equalsIgnoreCase("genIDN")) VerifyUCD.genIDN();
            else if (arg.equalsIgnoreCase("VerifyIDN")) VerifyUCD.VerifyIDN();
            else if (arg.equalsIgnoreCase("NFTest")) VerifyUCD.NFTest();
            else if (arg.equalsIgnoreCase("test1")) VerifyUCD.test1();
            else if (arg.equalsIgnoreCase("TrailingZeros")) GenerateData.genTrailingZeros();
            else if (arg.equalsIgnoreCase("GenerateThaiBreaks")) GenerateThaiBreaks.main(null);
            
            //else if (arg.equalsIgnoreCase("checkAgainstUInfo")) checkAgainstUInfo();
            else if (arg.equalsIgnoreCase("checkScripts")) VerifyUCD.checkScripts();
            else if (arg.equalsIgnoreCase("IdentifierTest")) VerifyUCD.IdentifierTest();
            else if (arg.equalsIgnoreCase("BuildNames")) BuildNames.main(null);
            else if (arg.equalsIgnoreCase("JavascriptProperties")) WriteJavaScriptInfo.assigned();
            else if (arg.equalsIgnoreCase("TestDirectoryIterator")) DirectoryIterator.test();
            else if (arg.equalsIgnoreCase("checkIdentical")) GenerateData.handleIdentical();
            
            //else if (arg.equalsIgnoreCase("NormalizationCharts")) ChartGenerator.writeNormalizationCharts();
            
            
            /*else if (arg.equalsIgnoreCase("writeNormalizerTestSuite"))
                GenerateData.writeNormalizerTestSuite("NormalizationTest-3.1.1d1.txt");
                */
            else extras(new String[] {arg});
        }
    }
    
    public static void extras (String[] args) throws Exception {
        //ubp = new UnifiedBinaryProperty(ucd);
        
        boolean expanding = false;
        
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.charAt(0) == '#') return; // skip rest of line
            long mask = 0;

            Utility.fixDot();
            if (expanding) System.out.println("Argument: " + args[i]);

            if (arg.equalsIgnoreCase("All")) {
                // Append all args at end
                /*
                String[] temp = new String[args.length + ALL_FILES.length];
                System.arraycopy(args, 0, temp, 0, args.length);
                System.arraycopy(ALL_FILES, 0, temp, args.length, ALL_FILES.length);
                */
                args = Utility.append(args, ALL_FILES);
                expanding = true;

            // EXTRACTED PROPERTIES
            
            } else if (arg.equalsIgnoreCase("DerivedBidiClass")) {
                GenerateData.generateVerticalSlice(BIDI_CLASS, BIDI_CLASS+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedBidiClass");
                    
            } else if (arg.equalsIgnoreCase("DerivedBinaryProperties")) {
                GenerateData.generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES+1, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedBinaryProperties" );
                    
            } else if (arg.equalsIgnoreCase("DerivedCombiningClass")) {
                GenerateData.generateVerticalSlice(COMBINING_CLASS, COMBINING_CLASS+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedCombiningClass" );
                    
            } else if (arg.equalsIgnoreCase("DerivedDecompositionType")) {
                GenerateData.generateVerticalSlice(DECOMPOSITION_TYPE, DECOMPOSITION_TYPE+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedDecompositionType" );

            } else if (arg.equalsIgnoreCase("DerivedEastAsianWidth")) {
                GenerateData.generateVerticalSlice(EAST_ASIAN_WIDTH, EAST_ASIAN_WIDTH+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedEastAsianWidth" );
                    
            } else if (arg.equalsIgnoreCase("DerivedGeneralCategory")) {
                GenerateData.generateVerticalSlice(CATEGORY, CATEGORY+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedGeneralCategory" );
                    
            } else if (arg.equalsIgnoreCase("DerivedJoiningGroup")) {
                GenerateData.generateVerticalSlice(JOINING_GROUP, JOINING_GROUP+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedJoiningGroup" );
                    
            } else if (arg.equalsIgnoreCase("DerivedJoiningType")) {
                GenerateData.generateVerticalSlice(JOINING_TYPE, JOINING_TYPE+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedJoiningType" );
                    
            } else if (arg.equalsIgnoreCase("DerivedLineBreak")) {
                GenerateData.generateVerticalSlice(LINE_BREAK, LINE_BREAK+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedLineBreak" );

            } else if (arg.equalsIgnoreCase("DerivedNumericType")) {
                GenerateData.generateVerticalSlice(NUMERIC_TYPE, NUMERIC_TYPE+NEXT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedNumericType" );

            } else if (arg.equalsIgnoreCase("DerivedNumericValues")) {
                GenerateData.generateVerticalSlice(LIMIT_ENUM, LIMIT_ENUM, GenerateData.HEADER_DERIVED,
                    "DerivedData/extracted/", "DerivedNumericValues" );
            
    // OTHER STANDARD PROPERTIES
    
            } else if (arg.equalsIgnoreCase("CaseFolding")) {
                GenerateCaseFolding.makeCaseFold(true);
                GenerateCaseFolding.makeCaseFold(false);
                    
            } else if (arg.equalsIgnoreCase("SpecialCasing")) {
                GenerateCaseFolding.generateSpecialCasing(true);
                GenerateCaseFolding.generateSpecialCasing(false);
                    
            } else if (arg.equalsIgnoreCase("CompositionExclusions")) {
                GenerateData.generateCompExclusions();

            } else if (arg.equalsIgnoreCase("DerivedAge")) {
                GenerateData.generateAge("DerivedData/", "DerivedAge");
                
            } else if (arg.equalsIgnoreCase("backwardsCompat")) {
                GenerateData.backwardsCompat("DerivedData/extracted/", "Compatibility_ID_START",
        			new int[] {ID_Start, ID_Continue_NO_Cf, Mod_ID_Start, Mod_ID_Continue_NO_Cf});
                
            } else if (arg.equalsIgnoreCase("DerivedCoreProperties")) {
                GenerateData.generateDerived(DERIVED_CORE, true, GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedCoreProperties");
                
            } else if (arg.equalsIgnoreCase("DerivedNormalizationProperties")) {
                GenerateData.generateDerived(DERIVED_NORMALIZATION, true, GenerateData.HEADER_DERIVED, "DerivedData/", 
                    "DerivedNormalizationProperties" );
                    
            } else if (arg.equalsIgnoreCase("NormalizationTest")) {
                GenerateData.writeNormalizerTestSuite("DerivedData/", "NormalizationTest");
                
            } else if (arg.equalsIgnoreCase("PropertyAliases")) {
                GenerateData.generatePropertyAliases();                

            } else if (arg.equalsIgnoreCase("PropList")) {
                GenerateData.generateVerticalSlice(BINARY_PROPERTIES + White_space, BINARY_PROPERTIES + NEXT_ENUM,
                        GenerateData.HEADER_EXTEND, "DerivedData/", "PropList");

            } else if (arg.equalsIgnoreCase("Scripts")) {
                GenerateData.generateVerticalSlice(SCRIPT+1, SCRIPT + NEXT_ENUM, 
                        GenerateData.HEADER_SCRIPTS, "DerivedData/", "Scripts");
    // OTHER TESTING
            
            } else if (arg.equalsIgnoreCase("OtherDerivedProperties")) {
                //mask = Utility.setBits(0, NFC_Leading, NFC_Resulting);
                GenerateData.generateDerived(ALL, false, GenerateData.HEADER_DERIVED, "OtherData/", "OtherDerivedProperties");

            } else if (arg.equalsIgnoreCase("AllBinary")) {
                GenerateData.generateVerticalSlice(BINARY_PROPERTIES, BINARY_PROPERTIES + NEXT_ENUM,
                        GenerateData.HEADER_EXTEND, "OtherDerived/", "AllBinary");
                        
            } else if (arg.equalsIgnoreCase("DerivedGeneralCategoryTEST")) {
                GenerateData.generateVerticalSlice(CATEGORY+29, CATEGORY+32, GenerateData.HEADER_DERIVED,
                    "DerivedData/", "DerivedGeneralCategory" );
                    
            } else if (arg.equalsIgnoreCase("differences")) {
                GenerateData.listDifferences();
                
            } else if (arg.equalsIgnoreCase("partition")) {
                GenerateData.partitionProperties();
                
            } else if (arg.equalsIgnoreCase("listAccents")) {
                GenerateData.listCombiningAccents();
                
            } else if (arg.equalsIgnoreCase("listGreekVowels")) {
                GenerateData.listGreekVowels();
                
            } else if (arg.equalsIgnoreCase("listKatakana")) {
                GenerateData.listKatakana();
                
            /* 
            } else if (arg.equalsIgnoreCase("DerivedFullNormalization")) {
                mask = Utility.setBits(0, DerivedProperty.GenNFD, DerivedProperty.GenNFKC);
                GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedFullNormalization" );
            } else if (arg.equalsIgnoreCase("caseignorable")) {
                mask = Utility.setBits(0, DerivedProperty.Other_Case_Ignorable, DerivedProperty.Type_i);
                GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "OtherData/", "CaseIgnorable" );
            } else if (arg.equalsIgnoreCase("nfunsafestart")) {
                mask = Utility.setBits(0, NFD_UnsafeStart, NFKC_UnsafeStart);
                GenerateData.generateDerived(mask, GenerateData.HEADER_DERIVED, "OtherData/", "NFUnsafeStart");
            */
            
            } else {
                throw new IllegalArgumentException(" ! Unknown option -- see Main.java for options");
            }


            //checkHoffman("\u05B8\u05B9\u05B1\u0591\u05C3\u05B0\u05AC\u059F");
            //checkHoffman("\u0592\u05B7\u05BC\u05A5\u05B0\u05C0\u05C4\u05AD");


                //GenerateData.generateDerived(Utility.setBits(0, DerivedProperty.PropMath, DerivedProperty.Mod_ID_Continue_NO_Cf),
                //    GenerateData.HEADER_DERIVED, "DerivedData/", "DerivedPropData2" );
            //GenerateData.generateVerticalSlice(SCRIPT, SCRIPT+1, "ScriptCommon" );
            //listStrings("LowerCase" , 0,0);
            //GenerateData.generateVerticalSlice(0, LIMIT_ENUM, SKIP_SPECIAL, PROPLIST1, "DerivedData/", "DerivedPropData1" );

            // AGE stuff
            //UCD ucd = UCD.make();
            //System.out.println(ucd.getAgeID(0x61));
            //System.out.println(ucd.getAgeID(0x2FA1D));

            //
        }
    }

}