/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;

public class ArabicCharacterData
{
    public class Record
    {
        public int getCodePoint()
        {
            return codePoint;
        }
        
        public int getGeneralCategory()
        {
            return generalCategory;
        }
        
        public int getDecompositionType()
        {
            return decompositionType;
        }
        
        public String getDecomposition()
        {
            return decomposition;
        }
        
        private Record(int character)
        {
            codePoint         = character;
            generalCategory   = UCharacter.getType(character);
            decompositionType = UCharacter.getIntPropertyValue(character, UProperty.DECOMPOSITION_TYPE);
            
            switch (decompositionType) {
            case UCharacter.DecompositionType.FINAL:
            case UCharacter.DecompositionType.INITIAL:
            case UCharacter.DecompositionType.ISOLATED:
            case UCharacter.DecompositionType.MEDIAL:
                decomposition = Normalizer.compose(UCharacter.toString(character), true);
                break;
                
            case UCharacter.DecompositionType.CANONICAL:
                decomposition = Normalizer.decompose(UCharacter.toString(character), true);
                break;
                
            default:
                decomposition = null;
            }
        }
        
        private int codePoint;
        private int generalCategory;
        private int decompositionType;
        private String decomposition;
    }
    
    private ArabicCharacterData(int charCount)
    {
        records = new Record[charCount];
    }
    
    private void add(int character)
    {
        records[recordIndex++] = new Record(character);
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
 
    // TODO: do we need to change this to use UnicodeSetIterator?
    // That will mean not knowing the number of characters until
    // after the iteration is done, so we'd have to use a vector
    // to hold the Records at first and copy it to an array
    // when we're done...   
    public static ArabicCharacterData factory(UnicodeSet characterSet)
    {
        int charCount = characterSet.size();
        ArabicCharacterData data = new ArabicCharacterData(charCount);
        
        for (int i = 0; i < charCount; i += 1) {
            data.add(characterSet.charAt(i));
        }
        
        return data;
    }
    
    private Record[] records;
    private int recordIndex = 0;
}
