/**
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/CanonGSUBBuilder.java,v $ 
 * $Date: 2004/05/03 21:07:28 $ 
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */


package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UTF16;
import java.util.Vector;

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
    public static void buildLigatureTree(CanonicalCharacterData data, LigatureTree ligatureTree)
    {
        System.out.print("building composition ligature tree...");
        
        for (int i = 0; i < data.countRecords(); i += 1) {
            CanonicalCharacterData.Record record = data.getRecord(i);
            String composed = UCharacter.toString(record.getComposedCharacter());
            
            for (int e = 0; e < record.countEquivalents(); e += 1) {
                String equivalent = record.getEquivalent(e);
                
                ligatureTree.insert(equivalent + composed);
            }
        }
        
        System.out.println(" Done.");
    }
    
    public static DecompTable[] buildDecompTables(CanonicalCharacterData data)
    {
        int maxDecompCount = data.getMaxEquivalents();
        DecompTable[] decompTables = new DecompTable[maxDecompCount];
        
        System.out.print("Building decompositon tables... max number of decompositions is " + maxDecompCount + "...");
        
        for (int i = 0; i < maxDecompCount; i += 1) {
            DecompTable table = new DecompTable();
            
            for (int r = 0; r < data.countRecords(); r += 1) {
                CanonicalCharacterData.Record record = data.getRecord(r);
                
                if (record.countEquivalents() > i) {
                    table.add(record.getComposedCharacter(), record.getEquivalent(i));
                }
            }
            
            decompTables[i] = table;
        }
        
        System.out.println(" Done.");
        
        return decompTables;
    }
    
    public static void buildLatinTables(String fileName)
    {
        UnicodeSet latinSet = new UnicodeSet("[[\\p{Latin}] & [\\p{DecompositionType=Canonical}]]");
        CanonicalCharacterData data = CanonicalCharacterData.factory(latinSet);
        
        DecompTable[] decompTables = buildDecompTables(data);
        
        LigatureTree compTree = new LigatureTree();
        
        buildLigatureTree(data, compTree);
        
        LigatureTreeWalker compWalker = new LigatureTreeWalker();
        
        compTree.walk(compWalker);
        
        LookupList lookupList = new LookupList();
        FeatureList featureList = new FeatureList();
        ScriptList scriptList = new ScriptList();
        Lookup compLookup, dcmpLookup;
        int compLookupIndex, dcmpLookupIndex;
        
        compLookup = new Lookup(Lookup.GSST_Ligature, 0);
        compLookup.addSubtable(compWalker);
        
        dcmpLookup = new Lookup(Lookup.GSST_Multiple, 0);
        for (int i = 0; i < decompTables.length; i += 1) {
            dcmpLookup.addSubtable(decompTables[i]);
        }
        
        compLookupIndex = lookupList.addLookup(compLookup);
        dcmpLookupIndex = lookupList.addLookup(dcmpLookup);

        featureList.addLookup("ccmp", compLookupIndex);        
        featureList.addLookup("ccmp", dcmpLookupIndex);
        featureList.finalizeFeatureList();
        
        scriptList.addFeature("latn", "(default)", featureList.getFeatureIndex("ccmp"));
        
        GSUBWriter gsubWriter = new GSUBWriter("Canon", scriptList, featureList, lookupList);
        String[] includeFiles = {"LETypes.h", "CanonShaping.h"};        
        
        LigatureModuleWriter writer = new LigatureModuleWriter();
        
        writer.openFile(fileName);
        writer.writeHeader(null, includeFiles);
        writer.writeTable(gsubWriter);
        writer.writeTrailer();
        writer.closeFile();
    }
    
    public static void main(String[] args)
    {
        buildLatinTables(args[0]);
    }
}
