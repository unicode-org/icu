/**
 *******************************************************************************
 * Copyright (C) 2002-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.text.UTF16;
import java.util.Vector;

/**
 * @author Owner
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class DecompTable implements LookupSubtable
{
    static class DecompEntry
    {
        private int composed;
        private int[] decomp;
        
        DecompEntry(int composedChar, String decomposition)
        {
            int decompCount = UTF16.countCodePoint(decomposition);
            
            composed = composedChar;
            decomp = new int[decompCount];
            
            int out = 0, cp;
            
            for (int in = 0; in < decomposition.length(); in += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(decomposition, in);
                decomp[out++] = cp;
            }
        }
        
        public int getComposedCharacter()
        {
            return composed;
        }
        
        public int[] getDecomposition()
        {
            return decomp;
        }
        
        public int getDecompositionCount()
        {
            return decomp.length;
        }
        
        public int getDecomposedCharacter(int i)
        {
            if (i >= 0 && i < decomp.length) {
                return decomp[i];
            }
            
            return -1;
        }
        
        public int compareTo(DecompEntry that)
        {
            return this.composed - that.composed;
        }
        
        //
        // Straight insertion sort from Knuth vol. III, pg. 81
        //
        public static void sort(DecompEntry[] table, Vector decompVector)
        {
            for (int j = 0; j < table.length; j += 1) {
                int i;
                DecompEntry v = (DecompEntry) decompVector.elementAt(j);

                for (i = j - 1; i >= 0; i -= 1) {
                    if (v.compareTo(table[i]) >= 0) {
                      break;
                    }

                    table[i + 1] = table[i];
                }

                table[i + 1] = v;
            }
        }
    }
    
    private Vector decompVector;
    private DecompEntry[] decompEntries;
    private int snapshotSize;
    
    public DecompTable()
    {
        decompVector = new Vector();
        decompEntries = null;
        snapshotSize = -1;
    }
    
    public void add(int composed, String decomposition)
    {
        DecompEntry entry = new DecompEntry(composed, decomposition);
        
        decompVector.addElement(entry);
    }
    
    public int getComposedCharacter(int i)
    {
        if (i < 0 || i > decompEntries.length) {
            return -1;
        }
        
        return decompEntries[i].getComposedCharacter();
    }
    
    public int getDecompositionCount(int i)
    {
        if (i < 0 || i > decompEntries.length) {
            return -1;
        }
        
        return decompEntries[i].getDecompositionCount();
    }
    
    public boolean hasEntries()
    {
        return decompVector.size() > 0;
    }
    
    private void snapshot()
    {
        if (snapshotSize != decompVector.size()) {
            snapshotSize = decompVector.size();
            decompEntries = new DecompEntry[snapshotSize];
            DecompEntry.sort(decompEntries, decompVector);
        }
    }

    public void writeLookupSubtable(OpenTypeTableWriter writer)
    {
        snapshot();
        
        int multipleSubstitutionsBase = writer.getOutputIndex();
        int coverageTableIndex, sequenceOffsetIndex;
        int sequenceCount = decompEntries.length;
        
        writer.writeData(1); // format = 1
        
        coverageTableIndex = writer.getOutputIndex();
        writer.writeData(0); // coverage table offset (fixed later)
        
        writer.writeData(sequenceCount);
        
        sequenceOffsetIndex = writer.getOutputIndex();
        for (int s = 0; s < sequenceCount; s += 1) {
            writer.writeData(0); // offset to sequence table (fixed later);
        }
        
        for (int s = 0; s < sequenceCount; s += 1) {
            DecompEntry entry = decompEntries[s];
            int decompCount = entry.getDecompositionCount();
            
            writer.fixOffset(sequenceOffsetIndex++, multipleSubstitutionsBase);
            
            writer.writeData(decompCount); // glyphCount
            
            for (int g = 0; g < decompCount; g += 1) {
                writer.writeData(entry.getDecomposedCharacter(g));
            }
        }
        
        // write a format 1 coverage table
        writer.fixOffset(coverageTableIndex, multipleSubstitutionsBase);
        writer.writeData(1); // format = 1
        writer.writeData(sequenceCount);  // glyphCount
        
        for (int i = 0; i < sequenceCount; i += 1) {
            writer.writeData(decompEntries[i].getComposedCharacter());
        }
    }
}
