// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author Eric Mader
 *
 * Notes:
 * 
 * The property \p{Decomposition_Type=Canonical} will match all characters with a canonical
 * decomposition.
 *
 * So "[[\\p{Latin}\\p{Greek}\\p{Cyrillic}] & [\\p{Decomposition_Type=Canonical}]]"
 * will match all Latin, Greek and Cyrillic characters with a canonical decomposition.
 * 
 * Are these three scripts enough? Do we want to collect them all at once and distribute by script,
 * or process them one script at a time. It's probably a good idea to build a single table for
 * however many scripts there are.
 * 
 * It might be better to collect all the characters that have a canonical decomposition and just
 * sort them into however many scripts there are... unless we'll get characters in COMMON???
 */
public class CanonGSUBBuilder
{
    static public String convertArabicString(int type, int ligature, String decomp, ClassTable isolClassTable)
    {
        int leftType  = ArabicShaping.VALUE_NONE;
        int rightType = ArabicShaping.VALUE_NONE;
        
        switch (type) {
            case UCharacter.DecompositionType.ISOLATED:
                break;
                
            case UCharacter.DecompositionType.FINAL:
                rightType = ArabicShaping.VALUE_LEFT;
                break;
            
            case UCharacter.DecompositionType.INITIAL:
                leftType = ArabicShaping.VALUE_RIGHT;
                break;
            
            case UCharacter.DecompositionType.MEDIAL:
               rightType = ArabicShaping.VALUE_LEFT;
               leftType  = ArabicShaping.VALUE_RIGHT;
               break;
               
           default:
               return decomp + UCharacter.toString(ligature);
        }
        
        char[] chars = decomp.toCharArray();
              
        ArabicShaping.shape(chars, leftType, rightType, isolClassTable);
  
        return new String(chars) + UCharacter.toString(ligature);
    }
    
    static void buildArabicContextualForms(ArabicCharacterData data, ClassTable initClassTable, ClassTable mediClassTable,
                                     ClassTable finaClassTable, ClassTable isolClassTable)
    {
        System.out.print("Finding Arabic contextual forms... ");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            ArabicCharacterData.Record record = data.getRecord(i);
            String decomposition = record.getDecomposition();
            
            if (decomposition != null && decomposition.length() == 1) {
                int contextual = record.getCodePoint();
                int isolated   = UTF16.charAt(record.getDecomposition(), 0);
            
                switch (record.getDecompositionType()) {
                case UCharacter.DecompositionType.INITIAL:
                    initClassTable.addMapping(isolated, contextual);
                    break;
                
                case UCharacter.DecompositionType.MEDIAL:
                    mediClassTable.addMapping(isolated, contextual);
                    break;
                
               case UCharacter.DecompositionType.FINAL:
                   finaClassTable.addMapping(isolated, contextual);
                   break;
                   
               case UCharacter.DecompositionType.ISOLATED:
                   isolClassTable.addMapping(isolated, contextual);
                   break;
               
               default:
                   // issue some error message?
                   break;
                }
            }
        }
        
        System.out.println("Done.");
    }

    static LigatureTree buildArabicLigatureTree(ArabicCharacterData data, ClassTable isolClassTable)
    {
        LigatureTree contextualTree = new LigatureTree();
        int ligatureCount = 0;
        
        System.out.print("Building Arabic ligature tree... ");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            ArabicCharacterData.Record record = data.getRecord(i);
            String decomposition = record.getDecomposition();
            
            if (decomposition != null && decomposition.length() > 1) {
                int ligature   = record.getCodePoint();
                int decompType = record.getDecompositionType();
                
                switch (decompType) {
                case UCharacter.DecompositionType.FINAL:
                case UCharacter.DecompositionType.INITIAL:
                case UCharacter.DecompositionType.MEDIAL:
                case UCharacter.DecompositionType.ISOLATED:
                    contextualTree.insert(convertArabicString(decompType, ligature, decomposition, isolClassTable));
                    ligatureCount += 1;
                    break;
                    
                case UCharacter.DecompositionType.CANONICAL:
                    //cannonicalTree.insert(decomposition + UCharacter.toString(ligature));
                    break;
                }
            }
        }
        
        System.out.println(ligatureCount + " ligatures.");
        
        return contextualTree;
    }
    
    static final int SIMPLE_GLYPH = 1;
    static final int LIGATURE_GLYPH = 2;
    static final int MARK_GLYPH = 3;
    static final int COMPONENT_GLYPH = 4;
    
    static final int categoryClassMap[] = {
    0,              // UNASSIGNED
    SIMPLE_GLYPH,   // UPPERCASE_LETTER
    SIMPLE_GLYPH,   // LOWERCASE_LETTER
    SIMPLE_GLYPH,   // TITLECASE_LETTER
    SIMPLE_GLYPH,   // MODIFIER_LETTER
    SIMPLE_GLYPH,   // OTHER_LETTER
    MARK_GLYPH,     // NON_SPACING_MARK
    MARK_GLYPH,     // ENCLOSING_MARK ??
    MARK_GLYPH,     // COMBINING_SPACING_MARK ??
    SIMPLE_GLYPH,   // DECIMAL_NUMBER
    SIMPLE_GLYPH,   // LETTER_NUMBER
    SIMPLE_GLYPH,   // OTHER_NUMBER;
    0,              // SPACE_SEPARATOR
    0,              // LINE_SEPARATOR
    0,              // PARAGRAPH_SEPARATOR
    0,              // CONTROL
    0,              // FORMAT
    0,              // PRIVATE_USE
    0,              // SURROGATE
    SIMPLE_GLYPH,   // DASH_PUNCTUATION
    SIMPLE_GLYPH,   // START_PUNCTUATION
    SIMPLE_GLYPH,   // END_PUNCTUATION
    SIMPLE_GLYPH,   // CONNECTOR_PUNCTUATION
    SIMPLE_GLYPH,   // OTHER_PUNCTUATION
    SIMPLE_GLYPH,   // MATH_SYMBOL;
    SIMPLE_GLYPH,   // CURRENCY_SYMBOL
    SIMPLE_GLYPH,   // MODIFIER_SYMBOL
    SIMPLE_GLYPH,   // OTHER_SYMBOL
    SIMPLE_GLYPH,   // INITIAL_PUNCTUATION
    SIMPLE_GLYPH    // FINAL_PUNCTUATION
    };

    static int getGlyphClass(ArabicCharacterData.Record record)
    {
        String decomp = record.getDecomposition();
        
        if (decomp != null && decomp.length() > 1) {
            return LIGATURE_GLYPH;
        }
        
        return categoryClassMap[record.getGeneralCategory()];
    }
    
    static void addArabicGlyphClasses(ArabicCharacterData data, ClassTable classTable)
    {
        System.out.print("Adding Arabic glyph classes... ");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            ArabicCharacterData.Record record = data.getRecord(i);
            classTable.addMapping(record.getCodePoint(), getGlyphClass(record));
        }
        
        System.out.println("Done.");
    }
    
    private static void buildArabicTables(ScriptList scriptList, FeatureList featureList,
                                                LookupList lookupList, ClassTable classTable) {
        // TODO: Might want to have the ligature table builder explicitly check for ligatures
        // which start with space and tatweel rather than pulling them out here...
        UnicodeSet arabicBlock   = new UnicodeSet("[[\\p{block=Arabic}] & [[:Cf:][:Po:][:So:][:Mn:][:Nd:][:Lm:]]]");
        UnicodeSet oddLigatures  = new UnicodeSet("[\\uFC5E-\\uFC63\\uFCF2-\\uFCF4\\uFE70-\\uFE7F]");
        UnicodeSet arabicLetters = new UnicodeSet("[\\p{Arabic}]");
        ArabicCharacterData arabicData = ArabicCharacterData.factory(arabicLetters.addAll(arabicBlock).removeAll(oddLigatures));

        addArabicGlyphClasses(arabicData, classTable);
        
        ClassTable initClassTable = new ClassTable();
        ClassTable mediClassTable = new ClassTable();
        ClassTable finaClassTable = new ClassTable();
        ClassTable isolClassTable = new ClassTable();
        
        buildArabicContextualForms(arabicData, initClassTable, mediClassTable, finaClassTable, isolClassTable);
        isolClassTable.snapshot();
        LigatureTree ligaTree = buildArabicLigatureTree(arabicData, isolClassTable);

        LigatureTreeWalker ligaWalker = new LigatureTreeWalker();

        ligaTree.walk(ligaWalker);
        
        Lookup initLookup, mediLookup, finaLookup, ligaLookup;
        
        initLookup = new Lookup(Lookup.GSST_Single, 0);
        initLookup.addSubtable(initClassTable);
        
        mediLookup = new Lookup(Lookup.GSST_Single, 0);
        mediLookup.addSubtable(mediClassTable);
        
        finaLookup = new Lookup(Lookup.GSST_Single, 0);
        finaLookup.addSubtable(finaClassTable);
        
        ligaLookup = new Lookup(Lookup.GSST_Ligature, Lookup.LF_IgnoreMarks);
        ligaLookup.addSubtable(ligaWalker);
        
        Feature init = new Feature("init");
        Feature medi = new Feature("medi");
        Feature fina = new Feature("fina");
        Feature liga = new Feature("liga");
        
        init.addLookup(lookupList.addLookup(initLookup));
        medi.addLookup(lookupList.addLookup(mediLookup));
        fina.addLookup(lookupList.addLookup(finaLookup));
        liga.addLookup(lookupList.addLookup(ligaLookup));
        
        featureList.addFeature(init);
        featureList.addFeature(medi);
        featureList.addFeature(fina);
        featureList.addFeature(liga);
        
        scriptList.addFeature("arab", "(default)", init);
        scriptList.addFeature("arab", "(default)", medi);
        scriptList.addFeature("arab", "(default)", fina);
        scriptList.addFeature("arab", "(default)", liga);
        
        System.out.println();
    }

    public static void buildLigatureTree(CanonicalCharacterData data, int script, LigatureTree ligatureTree)
    {
        int ligatureCount = 0;
        
        System.out.print("building composition ligature tree for " + UScript.getName(script) + "... ");
        
        for (int i = 0; i < data.countRecords(script); i += 1) {
            CanonicalCharacterData.Record record = data.getRecord(script, i);
            String composed = UCharacter.toString(record.getComposedCharacter());
            
            for (int e = 0; e < record.countEquivalents(); e += 1) {
                String equivalent = record.getEquivalent(e);
                
                ligatureTree.insert(equivalent + composed);
                ligatureCount += 1;
            }
        }
        
        System.out.println(ligatureCount + " ligatures.");
    }
    
    public static DecompTable[] buildDecompTables(CanonicalCharacterData data, int script)
    {
        int maxDecompCount = data.getMaxEquivalents(script);
        DecompTable[] decompTables = new DecompTable[maxDecompCount];
        
        System.out.print("Building decompositon tables for " + UScript.getName(script) +
                         "... total decompositions: " + data.countRecords(script) + 
                         ", max: " + maxDecompCount + "...");
        
        for (int i = 0; i < maxDecompCount; i += 1) {
            DecompTable table = new DecompTable();
            
            for (int r = 0; r < data.countRecords(script); r += 1) {
                CanonicalCharacterData.Record record = data.getRecord(script, r);
                
                if (record.countEquivalents() > i) {
                    table.add(record.getComposedCharacter(), record.getEquivalent(i));
                }
            }
            
            decompTables[i] = table;
        }
        
        System.out.println(" Done.");
        
        return decompTables;
    }
    
    public static int[] buildLookups(CanonicalCharacterData data, LookupList lookupList, int script)
    {
        int[] lookups = new int[2];
        
        DecompTable[] decompTables = buildDecompTables(data, script);
        
        LigatureTree compTree = new LigatureTree();
        
        buildLigatureTree(data, script, compTree);
        
        System.out.println();
        
        LigatureTreeWalker compWalker = new LigatureTreeWalker();
        
        compTree.walk(compWalker);
        
        Lookup compLookup, dcmpLookup;
        //int compLookupIndex, dcmpLookupIndex;
        
        compLookup = new Lookup(Lookup.GSST_Ligature, 0);
        compLookup.addSubtable(compWalker);
        
        dcmpLookup = new Lookup(Lookup.GSST_Multiple, 0);
        for (int i = 0; i < decompTables.length; i += 1) {
            dcmpLookup.addSubtable(decompTables[i]);
        }
        
        lookups[0] = lookupList.addLookup(compLookup);
        lookups[1] = lookupList.addLookup(dcmpLookup);
        
        return lookups;
    }
    
    public static void addLookups(Feature feature, int[] lookups)
    {
        for (int i = 0; i < lookups.length; i += 1) {
            feature.addLookup(lookups[i]);
        }
    }
    
    /*
     * Hebrew mark order taken from the SBL Hebrew Font manual
     * Arabic mark order per Thomas Milo: hamza < shadda < combining_alef < sukun, vowel_marks < madda < qur'anic_marks
     */
    public static ClassTable buildCombiningClassTable()
    {
        UnicodeSet markSet = new UnicodeSet("[\\P{CanonicalCombiningClass=0}]");
        ClassTable exceptions = new ClassTable();
        ClassTable combiningClasses = new ClassTable();
        int markCount = markSet.size();
        
        exceptions.addMapping(0x05C1,  10); // Point Shin Dot
        exceptions.addMapping(0x05C2,  11); // Point Sin Dot
        exceptions.addMapping(0x05BC,  21); // Point Dagesh or Mapiq
        exceptions.addMapping(0x05BF,  23); // Point Rafe
        exceptions.addMapping(0x05B9,  27); // Point Holam
        exceptions.addMapping(0x0323, 220); // Comb. Dot Below (low punctum)
        exceptions.addMapping(0x0591, 220); // Accent Etnahta
        exceptions.addMapping(0x0596, 220); // Accent Tipeha
        exceptions.addMapping(0x059B, 220); // Accent Tevir
        exceptions.addMapping(0x05A3, 220); // Accent Munah
        exceptions.addMapping(0x05A4, 220); // Accent Mahapakh
        exceptions.addMapping(0x05A5, 220); // Accent Merkha
        exceptions.addMapping(0x05A6, 220); // Accent Merkha Kefula
        exceptions.addMapping(0x05A7, 220); // Accent Darga
        exceptions.addMapping(0x05AA, 220); // Accent Yerah Ben Yomo
        exceptions.addMapping(0x05B0, 220); // Point Sheva
        exceptions.addMapping(0x05B1, 220); // Point Hataf Segol
        exceptions.addMapping(0x05B2, 220); // Point Hataf Patah
        exceptions.addMapping(0x05B3, 220); // Point Hataf Qamats
        exceptions.addMapping(0x05B4, 220); // Point Hiriq
        exceptions.addMapping(0x05B5, 220); // Point Tsere
        exceptions.addMapping(0x05B6, 220); // Point Segol
        exceptions.addMapping(0x05B7, 220); // Point Patah
        exceptions.addMapping(0x05B8, 220); // Point Qamats
        exceptions.addMapping(0x05BB, 220); // Point Qubuts
        exceptions.addMapping(0x05BD, 220); // Point Meteg
        exceptions.addMapping(0x059A, 222); // Accent Yetiv
        exceptions.addMapping(0x05AD, 222); // Accent Dehi
        exceptions.addMapping(0x05C4, 230); // Mark Upper Dot (high punctum)
        exceptions.addMapping(0x0593, 230); // Accent Shalshelet
        exceptions.addMapping(0x0594, 230); // Accent Zaqef Qatan
        exceptions.addMapping(0x0595, 230); // Accent Zaqef Gadol
        exceptions.addMapping(0x0597, 230); // Accent Revia
        exceptions.addMapping(0x0598, 230); // Accent Zarqa
        exceptions.addMapping(0x059F, 230); // Accent Qarney Para
        exceptions.addMapping(0x059E, 230); // Accent Gershayim
        exceptions.addMapping(0x059D, 230); // Accent Geresh Muqdam
        exceptions.addMapping(0x059C, 230); // Accent Geresh
        exceptions.addMapping(0x0592, 230); // Accent Segolta
        exceptions.addMapping(0x05A0, 230); // Accent Telisha Gedola
        exceptions.addMapping(0x05AC, 230); // Accent Iluy
        exceptions.addMapping(0x05A8, 230); // Accent Qadma
        exceptions.addMapping(0x05AB, 230); // Accent Ole
        exceptions.addMapping(0x05AF, 230); // Mark Masora Circle
        exceptions.addMapping(0x05A1, 230); // Accent Pazer
      //exceptions.addMapping(0x0307, 230); // Mark Number/Masora Dot
        exceptions.addMapping(0x05AE, 232); // Accent Zinor
        exceptions.addMapping(0x05A9, 232); // Accent Telisha Qetana
        exceptions.addMapping(0x0599, 232); // Accent Pashta
        
        exceptions.addMapping(0x0655,  27); // ARABIC HAMZA BELOW
        exceptions.addMapping(0x0654,  27); // ARABIC HAMZA ABOVE

        exceptions.addMapping(0x0651,  28); // ARABIC SHADDA

        exceptions.addMapping(0x0656,  29); // ARABIC SUBSCRIPT ALEF
        exceptions.addMapping(0x0670,  29); // ARABIC LETTER SUPERSCRIPT ALEF

        exceptions.addMapping(0x064D,  30); // ARABIC KASRATAN
        exceptions.addMapping(0x0650,  30); // ARABIC KASRA

        exceptions.addMapping(0x0652,  31); // ARABIC SUKUN
        exceptions.addMapping(0x06E1,  31); // ARABIC SMALL HIGH DOTLESS HEAD OF KHAH

        exceptions.addMapping(0x064B,  31); // ARABIC FATHATAN
        exceptions.addMapping(0x064C,  31); // ARABIC DAMMATAN
        exceptions.addMapping(0x064E,  31); // ARABIC FATHA
        exceptions.addMapping(0x064F,  31); // ARABIC DAMMA
        exceptions.addMapping(0x0657,  31); // ARABIC INVERTED DAMMA
        exceptions.addMapping(0x0658,  31); // ARABIC MARK NOON GHUNNA

        exceptions.addMapping(0x0653,  32); // ARABIC MADDAH ABOVE
        
        exceptions.snapshot();
        
        for (int i = 0; i < markCount; i += 1) {
            int mark = markSet.charAt(i);
            int markClass = exceptions.getGlyphClassID(mark);
            
            if (markClass == 0) {
                markClass = UCharacter.getCombiningClass(mark);
            }
            
            combiningClasses.addMapping(mark, markClass);
        }
        
        combiningClasses.snapshot();
        return combiningClasses;
    }
    
    public static void buildDecompTables(String fileName)
    {
        // F900 - FAFF are compatibility ideographs. They all decompose to a single other character, and can be ignored.
      //UnicodeSet decompSet = new UnicodeSet("[[[\\P{Hangul}] & [\\p{DecompositionType=Canonical}]] - [\uF900-\uFAFF]]");
        UnicodeSet decompSet = new UnicodeSet("[[\\p{DecompositionType=Canonical}] & [\\P{FullCompositionExclusion}] & [\\P{Hangul}]]");
        CanonicalCharacterData data = CanonicalCharacterData.factory(decompSet);
        ClassTable classTable = new ClassTable();
        
        LookupList  lookupList  = new LookupList();
        FeatureList featureList = new FeatureList();
        ScriptList  scriptList  = new ScriptList();

        // build common, inherited lookups...
//        int[] commonLookups = buildLookups(data, lookupList, UScript.COMMON);
//        int[] inheritedLookups = buildLookups(data, lookupList, UScript.INHERITED);
        
        for (int script = 0; script < UScript.CODE_LIMIT; script += 1) {
            
            // This is a bit lame, but it's the only way I can think of
            // to make this work w/o knowing the values of COMMON and INHERITED...
            if (script == UScript.COMMON || script == UScript.INHERITED ||
                data.getMaxEquivalents(script) == 0) {
                continue;
            }
            
            int[] lookups = buildLookups(data, lookupList, script);

            Feature ccmp = new Feature("ccmp");
            
            addLookups(ccmp, lookups);
//            addLookups(ccmp, commonLookups);
//            addLookups(ccmp, inheritedLookups);
            
            featureList.addFeature(ccmp);
        
            String scriptTag = TagUtilities.tagLabel(UScript.getShortName(script));
            
            scriptList.addFeature(scriptTag, "(default)", ccmp);
            
            if (script == UScript.ARABIC) {
                buildArabicTables(scriptList, featureList, lookupList, classTable);
            }
        }
        
        featureList.finalizeFeatureList();
        
        ClassTable markClassTable = buildCombiningClassTable();
        
        GSUBWriter gsubWriter = new GSUBWriter("Canon", scriptList, featureList, lookupList);
        GDEFWriter gdefWriter = new GDEFWriter("Canon", classTable, markClassTable);
        String[] includeFiles = {"LETypes.h", "CanonShaping.h"};        
        
        LigatureModuleWriter writer = new LigatureModuleWriter();
        
        writer.openFile(fileName);
        writer.writeHeader(null, includeFiles);
        writer.writeTable(gsubWriter);
        writer.writeTable(gdefWriter);
        writer.writeTrailer();
        writer.closeFile();
    }
    
    public static void main(String[] args)
    {
        buildDecompTables(args[0]);
    }
}
