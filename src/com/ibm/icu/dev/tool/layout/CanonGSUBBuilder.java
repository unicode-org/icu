/**
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;

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
    
    public static void buildDecompTables(String fileName)
    {
        UnicodeSet decompSet = new UnicodeSet("[[\\P{Hangul}] & [\\p{DecompositionType=Canonical}]]");
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
        
        GSUBWriter gsubWriter = new GSUBWriter("Canon", scriptList, featureList, lookupList);
        GDEFWriter gdefWriter = new GDEFWriter("Canon", classTable);
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
