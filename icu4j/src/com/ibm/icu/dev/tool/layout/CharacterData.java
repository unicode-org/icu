/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/Attic/CharacterData.java,v $
 * $Date: 2003/12/09 01:18:11 $
 * $Revision: 1.1 $
 * 
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16;

public class CharacterData
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
                decomposition = Normalizer.compose(UTF16.toString(character), true);
                break;
                
            case UCharacter.DecompositionType.CANONICAL:
                decomposition = Normalizer.decompose(UTF16.toString(character), true);
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
    
    private CharacterData(int charCount)
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
    public static CharacterData factory(UnicodeSet characterSet)
    {
        int charCount = characterSet.size();
        CharacterData data = new CharacterData(charCount);
        
        for (int i = 0; i < charCount; i += 1) {
            data.add(characterSet.charAt(i));
        }
        
        return data;
    }
    
    private Record[] records;
    private int recordIndex = 0;
}
