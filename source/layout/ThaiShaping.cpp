/*
 * @(#)ThaiShaping.cpp	1.13 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "ThaiShaping.h"

U_NAMESPACE_BEGIN

enum {
    CH_SPACE        = 0x0020,
    CH_YAMAKKAN     = 0x0E4E,
    CH_MAI_HANAKAT  = 0x0E31,
    CH_SARA_AA      = 0x0E32,
    CH_SARA_AM      = 0x0E33,
    CH_SARA_UEE     = 0x0E37,
    CH_MAITAIKHU    = 0x0E47,
    CH_NIKHAHIT     = 0x0E4D,
    CH_SARA_U       = 0x0E38,
    CH_PHINTHU      = 0x0E3A,
    CH_YO_YING      = 0x0E0D,
    CH_THO_THAN     = 0x0E10,
    CH_DOTTED_CIRCLE = 0x25CC
};

    le_uint8 ThaiShaping::getCharClass(LEUnicode ch)
{
    le_uint8 charClass = NON;
    
    if (ch >= 0x0E00 && ch <= 0x0E5B) {
        charClass = classTable[ch - 0x0E00];
    }
    
    return charClass;
}


LEUnicode ThaiShaping::leftAboveVowel(LEUnicode vowel, le_uint8 glyphSet)
{
    static LEUnicode leftAboveVowels[][7] = {
        {0x0E61, 0x0E32, 0x0E33, 0x0E64, 0x0E65, 0x0E66, 0x0E67},
        {0xF710, 0x0E32, 0x0E33, 0xF701, 0xF702, 0xF703, 0xF704},
        {0xF884, 0x0E32, 0x0E33, 0xF885, 0xF886, 0xF887, 0xF788},
        {0x0E31, 0x0E32, 0x0E33, 0x0E34, 0x0E35, 0x0E36, 0x0E37}
    };
   
    if (vowel >= CH_MAI_HANAKAT && vowel <= CH_SARA_UEE) {
        return leftAboveVowels[glyphSet][vowel - CH_MAI_HANAKAT];
    }
    
    if (vowel == CH_YAMAKKAN && glyphSet == 0) {
        return 0x0E7E;
    }
    
    return vowel;
}

LEUnicode ThaiShaping::lowerRightTone(LEUnicode tone, le_uint8 glyphSet)
{
    static LEUnicode lowerRightTones[][7] = {
        {0x0E68, 0x0E69, 0x0E6A, 0x0E6B, 0x0E6C, 0x0E6D, 0x0E6E},
        {0x0E47, 0xF70A, 0xF70B, 0xF70C, 0xF70D, 0xF70E, 0x0E4D},
        {0x0E47, 0xF88B, 0xF88E, 0xF891, 0xF894, 0xF897, 0x0E4D},
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return lowerRightTones[glyphSet][tone - CH_MAITAIKHU];
    }
    
    return tone;
}

LEUnicode ThaiShaping::lowerLeftTone(LEUnicode tone, le_uint8 glyphSet)
{
    static LEUnicode lowerLeftTones[][7] = {
        {0x0E76, 0x0E77, 0x0E78, 0x0E79, 0x0E7A, 0x0E7B, 0x0E7C},
        {0xF712, 0xF705, 0xF706, 0xF707, 0xF708, 0xF709, 0xF711},
        {0xF889, 0xF88C, 0xF88F, 0xF892, 0xF895, 0xF898, 0xF899},
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return lowerLeftTones[glyphSet][tone - CH_MAITAIKHU];
    }
    
    return tone;
}

LEUnicode ThaiShaping::upperLeftTone(LEUnicode tone, le_uint8 glyphSet)
{
    static LEUnicode upperLeftTones[][7] = {
        {0x0E6F, 0x0E70, 0x0E71, 0x0E72, 0x0E73, 0x0E74, 0x0E75},
        {0xF712, 0xF713, 0xF714, 0xF715, 0xF716, 0xF717, 0xF711},
        {0xF889, 0xF88A, 0xF88D, 0xF890, 0xF893, 0xF896, 0xF899},
        {0x0E47, 0x0E48, 0x0E49, 0x0E4A, 0x0E4B, 0x0E4C, 0x0E4D}
    };

    if (tone >= CH_MAITAIKHU && tone <= CH_NIKHAHIT) {
        return upperLeftTones[glyphSet][tone - CH_MAITAIKHU];
    }
    
    return tone;
}

LEUnicode ThaiShaping::lowerBelowVowel(LEUnicode vowel, le_uint8 glyphSet)
{
    static LEUnicode lowerBelowVowels[][3] = {
        {0x0E3C, 0x0E3D, 0x0E3E},
        {0xF718, 0xF719, 0xF71A},
        {0x0E38, 0x0E39, 0x0E3A},
        {0x0E38, 0x0E39, 0x0E3A}

    };

    if (vowel >= CH_SARA_U && vowel <= CH_PHINTHU) {
        return lowerBelowVowels[glyphSet][vowel - CH_SARA_U];
    }
    
    return vowel;
}

LEUnicode ThaiShaping::noDescenderCOD(LEUnicode cod, le_uint8 glyphSet)
{
    static LEUnicode noDescenderCODs[][4] = {
        {0x0E60, 0x0E0E, 0x0E0F, 0x0E63},
        {0xF70F, 0x0E0E, 0x0E0F, 0xF700},
        {0x0E0D, 0x0E0E, 0x0E0F, 0x0E10},
        {0x0E0D, 0x0E0E, 0x0E0F, 0x0E10}

    };

    if (cod >= CH_YO_YING && cod <= CH_THO_THAN) {
        return noDescenderCODs[glyphSet][cod - CH_YO_YING];
    }
    
    return cod;
}

le_uint8 ThaiShaping::doTransition (StateTransition transition, LEUnicode currChar, le_int32 inputIndex, le_uint8 glyphSet,
        LEUnicode errorChar, LEUnicode *outputBuffer, le_int32 *charIndicies, le_int32 &outputIndex)
{
    switch (transition.action) {
    case tA:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = currChar;
        break;
        
    case tC:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = currChar;
        break;
        
    case tD:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = leftAboveVowel(currChar, glyphSet);
        break;
        
    case tE:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = lowerRightTone(currChar, glyphSet);
        break;
        
    case tF:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = lowerLeftTone(currChar, glyphSet);
        break;
    
    case tG:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = upperLeftTone(currChar, glyphSet);
        break;
        
    case tH:
    {
        LEUnicode cod = outputBuffer[outputIndex - 1];
        LEUnicode coa = noDescenderCOD(cod, glyphSet);

        if (cod != coa) {
            outputBuffer[outputIndex - 1] = coa;
            
            charIndicies[outputIndex] = inputIndex;
            outputBuffer[outputIndex++] = currChar;
            break;
        }

        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = lowerBelowVowel(currChar, glyphSet);
        break;
    }
        
    case tR:
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = errorChar;

        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = currChar;
        break;
        
    case tS:
        if (currChar == CH_SARA_AM) {
            charIndicies[outputIndex] = inputIndex;
            outputBuffer[outputIndex++] = errorChar;
        }

        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = currChar;
        break;
        
    default:
        // FIXME: if we get here, there's an error
        // in the state table!
        charIndicies[outputIndex] = inputIndex;
        outputBuffer[outputIndex++] = currChar;
        break;
     }
     
     return transition.nextState;
}

le_uint8 ThaiShaping::getNextState(LEUnicode ch, le_uint8 prevState, le_int32 inputIndex, le_uint8 glyphSet, LEUnicode errorChar,
                              le_uint8 &charClass, LEUnicode *output, le_int32 *charIndicies, le_int32 &outputIndex)
{
    StateTransition transition;

    charClass = getCharClass(ch);
    transition = getTransition(prevState, charClass);
    
    return doTransition(transition, ch, inputIndex, glyphSet, errorChar, output, charIndicies, outputIndex);
}

le_bool ThaiShaping::isLegalHere(LEUnicode ch, le_uint8 prevState)
{
    le_uint8 charClass = getCharClass(ch);
    StateTransition transition = getTransition(prevState, charClass);

    switch (transition.action) {
    case tA:
    case tC:
    case tD:
    case tE:
    case tF:
    case tG:
    case tH:
        return true;
            
    case tR:
    case tS:
        return false;
            
    default:
        // FIXME: if we get here, there's an error
        // in the state table!
        return false;
    }
}
    
le_int32 ThaiShaping::compose(const LEUnicode *input, le_int32 offset, le_int32 charCount, le_uint8 glyphSet,
                          LEUnicode errorChar, LEUnicode *output, le_int32 *charIndicies)
{
    le_uint8 state = 0;
    le_int32 inputIndex;
    le_int32 outputIndex = 0;
    le_uint8 conState = 0xFF;
    le_int32 conInput = -1;
    le_int32 conOutput = -1;
    
    for (inputIndex = 0; inputIndex < charCount; inputIndex += 1) {
        LEUnicode ch = input[inputIndex + offset];
        le_uint8 charClass;
        
        // Decompose SARA AM into NIKHAHIT + SARA AA
        if (ch == CH_SARA_AM && isLegalHere(ch, state)) {
            outputIndex = conOutput;
            state = getNextState(CH_NIKHAHIT, conState, inputIndex, glyphSet, errorChar, charClass,
                output, charIndicies, outputIndex);
            
            for (int j = conInput + 1; j < inputIndex; j += 1) {
                ch = input[j + offset];
                state = getNextState(ch, state, j, glyphSet, errorChar, charClass,
                    output, charIndicies, outputIndex);
            }
            
            ch = CH_SARA_AA;
        }
        
        state = getNextState(ch, state, inputIndex, glyphSet, errorChar, charClass,
            output, charIndicies, outputIndex);
        
        if (charClass >= CON && charClass <= COD) {
            conState = state;
            conInput = inputIndex;
            conOutput = outputIndex;
        }
    }
    
    return outputIndex;
}

U_NAMESPACE_END
