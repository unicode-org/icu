/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/Attic/ArabicGSUBBuilder.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16;

public class ArabicGSUBBuilder
{
    static public String convertString(int type, int ligature, String decomp, ClassTable isolClassTable)
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
               return decomp + UTF16.toString(ligature);
        }
        
        char[] chars = decomp.toCharArray();
              
        ArabicShaping.shape(chars, leftType, rightType, isolClassTable);
  
        return new String(chars) + UTF16.toString(ligature);
    }
    
    static void buildContextualForms(CharacterData data, ClassTable initClassTable, ClassTable mediClassTable,
                                     ClassTable finaClassTable, ClassTable isolClassTable)
    {
        System.out.print("Finding contextual forms... ");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            CharacterData.Record record = data.getRecord(i);
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

    static void buildLigatureTrees(CharacterData data, ClassTable isolClassTable,
                                   LigatureTree contextualTree, LigatureTree cannonicalTree)
    {
        System.out.print("Building ligature trees... ");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            CharacterData.Record record = data.getRecord(i);
            String decomposition = record.getDecomposition();
            
            if (decomposition != null && decomposition.length() > 1) {
                int ligature   = record.getCodePoint();
                int decompType = record.getDecompositionType();
                
                switch (decompType) {
                case UCharacter.DecompositionType.FINAL:
                case UCharacter.DecompositionType.INITIAL:
                case UCharacter.DecompositionType.MEDIAL:
                case UCharacter.DecompositionType.ISOLATED:
                    contextualTree.insert(convertString(decompType, ligature, decomposition, isolClassTable));
                    break;
                    
                case UCharacter.DecompositionType.CANONICAL:
                    cannonicalTree.insert(decomposition + UTF16.toString(ligature));
                    break;
                }
            }
        }
        
        System.out.println("Done.");
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

    static int getGlyphClass(CharacterData.Record record)
    {
        String decomp = record.getDecomposition();
        
        if (decomp != null && decomp.length() > 1) {
            return LIGATURE_GLYPH;
        }
        
        return categoryClassMap[record.getGeneralCategory()];
    }
    
    static ClassTable buildGlyphClassTable(CharacterData data)
    {
        System.out.print("Building glyph class table... ");
        
        ClassTable classTable = new ClassTable();
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            CharacterData.Record record = data.getRecord(i);
            classTable.addMapping(record.getCodePoint(), getGlyphClass(record));
        }
        
        System.out.println("Done.");
        
        return classTable;
    }
    
    private static void buildArabicTables(String fileName) {
        // TODO: Might want to have the ligature table builder explicitly check for ligatures
        // which start with space and tatweel rather than pulling them out here...
        UnicodeSet arabicBlock   = new UnicodeSet("[[\\p{block=Arabic}] & [[:Cf:][:Po:][:So:][:Mn:][:Nd:][:Lm:]]]");
        UnicodeSet oddLigatures  = new UnicodeSet("[\\uFC5E-\\uFC63\\uFCF2-\\uFCF4\\uFE70-\\uFE7F]");
        UnicodeSet arabicLetters = new UnicodeSet("[\\p{Arabic}]");
        CharacterData arabicData = CharacterData.factory(arabicLetters.addAll(arabicBlock).removeAll(oddLigatures));
        ClassTable classTable = buildGlyphClassTable(arabicData);
        
        ClassTable initClassTable = new ClassTable();
        ClassTable mediClassTable = new ClassTable();
        ClassTable finaClassTable = new ClassTable();
        ClassTable isolClassTable = new ClassTable();
        
        buildContextualForms(arabicData, initClassTable, mediClassTable, finaClassTable, isolClassTable);
        isolClassTable.snapshot();
        LigatureTree ccmpTree = new LigatureTree();
        LigatureTree ligaTree = new LigatureTree();
        
        buildLigatureTrees(arabicData, isolClassTable, ligaTree, ccmpTree);

        LigatureTreeWalker ccmpWalker = new LigatureTreeWalker();
        LigatureTreeWalker ligaWalker = new LigatureTreeWalker();

        ccmpTree.walk(ccmpWalker);        
        ligaTree.walk(ligaWalker);
        
        LookupList lookupList = new LookupList();
        FeatureList featureList = new FeatureList();
        ScriptList scriptList = new ScriptList();
        Lookup ccmpLookup, initLookup, mediLookup, finaLookup, ligaLookup;
        int ccmpLookupIndex, initLookupIndex, mediLookupIndex, finaLookupIndex, ligaLookupIndex;
        
        ccmpLookup = new Lookup(Lookup.GSST_Ligature, 0);
        ccmpLookup.addSubtable(ccmpWalker);
        
        initLookup = new Lookup(Lookup.GSST_Single, 0);
        initLookup.addSubtable(initClassTable);
        
        mediLookup = new Lookup(Lookup.GSST_Single, 0);
        mediLookup.addSubtable(mediClassTable);
        
        finaLookup = new Lookup(Lookup.GSST_Single, 0);
        finaLookup.addSubtable(finaClassTable);
        
        ligaLookup = new Lookup(Lookup.GSST_Ligature, Lookup.LF_IgnoreMarks);
        ligaLookup.addSubtable(ligaWalker);
        
        ccmpLookupIndex = lookupList.addLookup(ccmpLookup);
        initLookupIndex = lookupList.addLookup(initLookup);
        mediLookupIndex = lookupList.addLookup(mediLookup);
        finaLookupIndex = lookupList.addLookup(finaLookup);
        ligaLookupIndex = lookupList.addLookup(ligaLookup);

        featureList.addLookup("ccmp", ccmpLookupIndex);        
        featureList.addLookup("init", initLookupIndex);
        featureList.addLookup("medi", mediLookupIndex);
        featureList.addLookup("fina", finaLookupIndex);
        featureList.addLookup("liga", ligaLookupIndex);
        featureList.finalizeFeatureList();
        
        scriptList.addFeature("arab", "(default)", featureList.getFeatureIndex("ccmp"));
        scriptList.addFeature("arab", "(default)", featureList.getFeatureIndex("init"));
        scriptList.addFeature("arab", "(default)", featureList.getFeatureIndex("medi"));
        scriptList.addFeature("arab", "(default)", featureList.getFeatureIndex("fina"));
        scriptList.addFeature("arab", "(default)", featureList.getFeatureIndex("liga"));
        
        GSUBWriter gsubWriter = new GSUBWriter("Arabic", scriptList, featureList, lookupList);
        GDEFWriter gdefWriter = new GDEFWriter("Arabic", classTable);
        
        String[] includeFiles = {"LETypes.h", "ArabicShaping.h"};        
        
        LigatureModuleWriter writer = new LigatureModuleWriter();
        
        writer.openFile(fileName);
        writer.writeHeader(null, includeFiles);
        writer.writeTable(gsubWriter);
        writer.writeTable(gdefWriter);
        writer.writeTrailer();
        writer.closeFile();
    }

    private static void buildHebrewTables(String fileName)
    {
        UnicodeSet hebrewBlock   = new UnicodeSet("[[\\p{block=Hebrew}] & [[:Cf:][:Po:][:So:][:Mn:][:Nd:][:Lm:]]]");
        UnicodeSet oddLigatures  = new UnicodeSet("[\\uFC5E-\\uFC63\\uFCF2-\\uFCF4\\uFE70-\\uFE7F]");
        UnicodeSet hebrewLetters = new UnicodeSet("[\\p{Hebrew}]");
        CharacterData hebrewData = CharacterData.factory(hebrewLetters.addAll(hebrewBlock).removeAll(oddLigatures));
        ClassTable classTable = buildGlyphClassTable(hebrewData);
        
        LigatureTree ligaTree = new LigatureTree();
        
        buildLigatureTrees(hebrewData, null, null, ligaTree);
        LigatureTreeWalker ligaWalker = new LigatureTreeWalker();
        
        ligaTree.walk(ligaWalker);
        
        LookupList lookupList = new LookupList();
        FeatureList featureList = new FeatureList();
        ScriptList scriptList = new ScriptList();
        Lookup ligaLookup;
        int ligaLookupIndex;

        ligaLookup = new Lookup(Lookup.GSST_Ligature, 0);
        ligaLookup.addSubtable(ligaWalker);
        
        ligaLookupIndex = lookupList.addLookup(ligaLookup);
        
        featureList.addLookup("liga", ligaLookupIndex);
        featureList.finalizeFeatureList();
        
        scriptList.addFeature("hebr", "(default)", featureList.getFeatureIndex("liga"));
        
        GSUBWriter gsubWriter = new GSUBWriter("Hebrew", scriptList, featureList, lookupList);
        GDEFWriter gdefWriter = new GDEFWriter("Hebrew", classTable);

        String[] includeFiles = {"LETypes.h", "HebrewShaping.h"};
        
        LigatureModuleWriter writer = new LigatureModuleWriter();
        
        writer.openFile(fileName);
        writer.writeHeader(null, includeFiles);
        writer.writeTable(gsubWriter);
        writer.writeTable(gdefWriter);
        writer.writeTrailer();
        writer.closeFile();
    }
    
    /*
     * Conversion notes:
     * 
     * Use a UnicodeSet of [\p{Arab}] to get all the Arabic letters. (Might want to
     * subtract [\uFBE8\uFBE9] (Uighur Alef Maksura forms) and [\UFBF9-\UFBFB] Uighur
     * ligatures which decompose to the same characters as the corresponding "normal"
     * ones.)
     * 
     * Use UCharacter.getType(ch) to get the general category. Values are defined in
     * UCharacterCategory.
     * 
     * Use (something like) [\p{DecompositonType=INITIAL}] to get initial, medial
     * final (and isolated?) forms.
     * 
     * Use the normalizer to decompose the characters: if the decomposition is
     * a single letter, it's an initial, medial or final form, otherwise it's a
     * ligature.
     * 
     * Use ArabicShaping to convert the decomposed ligature back into shaped
     * presentation forms. Need to add kashida's on the front and / or back to
     * get it to generate the correct forms. (could add either a kashida or a
     * non-joiner so that we never need to look at the first or last character)
     * 
     * Can do contextual forms and ligatures in a single pass, since we have
     * to actually normalize the character to figure out if it's a ligature or
     * not. Also need the ligature-ness of the character to compute it's glyph
     * class... might work to keep a class table which says contextual or ligature
     * for each character. Build it while building contextual and ligature tables,
     * then use it to generate the actual glyph class table... (this is backwards
     * to how it's done now ;-)
     */
    public static void main(String[] args)
    {
        buildArabicTables(args[0]);
        buildHebrewTables(args[1]);
    }
}