/**
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/CanonicalCharacterData.java,v $ 
 * $Date: 2004/05/03 21:07:28 $ 
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.UTF16;
import java.util.Vector;

public class CanonicalCharacterData
{
    public class Record
    {
        // TODO: might want to save arrays of Char32's rather than UTF16 strings...
        Record(int character)
        {
            String char32 = UCharacter.toString(character);
            CanonicalIterator iterator = new CanonicalIterator(char32);
            Vector equivs = new Vector();
            
            composed = character;
            
            for (String equiv = iterator.next(); equiv != null; equiv = iterator.next()) {
                // Skip all equivalents of length 1; it's either the original
                // characeter or something like Angstrom for A-Ring, which we don't care about
                if (UTF16.countCodePoint(equiv) > 1) {
                    equivs.add(equiv);
                }
            }
            
            int nEquivalents = equivs.size();
            
            if (nEquivalents > maxEquivalents) {
                maxEquivalents = nEquivalents;
            }
            
            if (nEquivalents > 0) {
                equivalents = new String[nEquivalents];
                
                for (int e = 0; e < nEquivalents; e += 1) {
                    equivalents[e] = (String) equivs.elementAt(e);
                }
                
                sortEquivalents(equivalents);
            }
        }
        
        public int getComposedCharacter()
        {
            return composed;
        }
        
        public int countEquivalents()
        {
            if (equivalents == null) {
                return 0;
            }
            return equivalents.length;
        }
        
        public String[] getEquivalents()
        {
            return equivalents;
        }
        
        public String getEquivalent(int index)
        {
            if (equivalents == null || index < 0 || index >= equivalents.length) {
                return null;
            }
            
            return equivalents[index];
        }
        
        private int composed;
        private String[] equivalents = null;
    }
    
    public CanonicalCharacterData(int charCount)
    {
        records = new Record[charCount];
    }
    
    public void add(int character)
    {
        records[recordIndex++] = new Record(character);
    }
    
    public int getCharacterCount()
    {
        return recordIndex;
    }
    
    public int getMaxEquivalents()
    {
        return maxEquivalents;
    }
    
    public Record getRecord(int index)
    {
        if (index < 0 || index >= records.length) {
            return null;
        }
        
        return records[index];
    }
    
    public int countRecords()
    {
        return records.length;
    }
 
    public static CanonicalCharacterData factory(UnicodeSet characterSet)
    {
        int charCount = characterSet.size();
        CanonicalCharacterData data = new CanonicalCharacterData(charCount);
        
        for (int i = 0; i < charCount; i += 1) {
            data.add(characterSet.charAt(i));
        }
        
        return data;
    }

    private static int compareEquivalents(String a, String b)
    {
        int result = UTF16.countCodePoint(a) - UTF16.countCodePoint(b);
            
        if (result == 0) {
            return a.compareTo(b);
        }
            
        return result;
    }
        
    //
    // Straight insertion sort from Knuth vol. III, pg. 81
    //
    private static void sortEquivalents(String[] table)
    {
        for (int j = 1; j < table.length; j += 1) {
            int i;
            String v = table[j];

            for (i = j - 1; i >= 0; i -= 1) {
                if (compareEquivalents(v, table[i]) >= 0) {
                  break;
                }

                table[i + 1] = table[i];
            }

            table[i + 1] = v;
        }
    }
        
    private Record[] records;
    private int recordIndex = 0;
    private int maxEquivalents = 0;

}
