/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "IndicReordering.h"
#include "LEGlyphStorage.h"
#include "MPreFixups.h"

U_NAMESPACE_BEGIN

class ReorderingOutput : public UMemory {
private:
    le_int32 fOutIndex;
    LEUnicode *fOutChars;

    LEGlyphStorage &fGlyphStorage;

    LEUnicode fMpre;
    LEUnicode fMbelow;
    LEUnicode fMabove;
    LEUnicode fMpost;
    LEUnicode fLengthMark;
    le_int32 fMatraIndex;
    const LETag *fMatraTags;
    
    le_int32 fMPreOutIndex;
    MPreFixups *fMPreFixups;
    
    LEUnicode fVMabove;
    LEUnicode fVMpost;
    le_int32  fVMIndex;
    const LETag *fVMTags;
    
    LEUnicode fSMabove;
    LEUnicode fSMbelow;
    le_int32  fSMIndex;
    const LETag *fSMTags;

    void saveMatra(LEUnicode matra, IndicClassTable::CharClass matraClass)
    {
        // FIXME: check if already set, or if not a matra...
        if (IndicClassTable::isLengthMark(matraClass)) {
            fLengthMark = matra;
        } else {
            switch (matraClass & IndicClassTable::CF_POS_MASK) {
            case IndicClassTable::CF_POS_BEFORE:
                fMpre = matra;
                break;
               
            case IndicClassTable::CF_POS_BELOW:
                fMbelow = matra;
                break;
               
            case IndicClassTable::CF_POS_ABOVE:
                fMabove = matra;
                break;
               
            case IndicClassTable::CF_POS_AFTER:
                fMpost = matra;
                break;
               
            default:
                // can't get here...
                break;
           }
        }
    }

public:
    ReorderingOutput(LEUnicode *outChars, LEGlyphStorage &glyphStorage, MPreFixups *mpreFixups)
        : fOutIndex(0), fOutChars(outChars), fGlyphStorage(glyphStorage),
          fMpre(0), fMbelow(0), fMabove(0), fMpost(0), fLengthMark(0), fMatraIndex(0), fMatraTags(NULL),
          fMPreOutIndex(-1), fMPreFixups(mpreFixups),
          fVMabove(0), fVMpost(0), fVMIndex(0), fVMTags(NULL),
          fSMabove(0), fSMbelow(0), fSMIndex(0), fSMTags(NULL)
    {
        // nothing else to do...
    }

    ~ReorderingOutput()
    {
        // nothing to do here...
    }

    void reset()
    {
        fMpre = fMbelow = fMabove = fMpost = fLengthMark = 0;
        fMPreOutIndex = -1;
        
        fVMabove = fVMpost  = 0;
        fSMabove = fSMbelow = 0;
    }
    
    void noteMatra(const IndicClassTable *classTable, LEUnicode matra, le_uint32 matraIndex, const LETag *matraTags)
    {
        IndicClassTable::CharClass matraClass = classTable->getCharClass(matra);

        fMatraIndex = matraIndex;
        fMatraTags = matraTags;

        if (IndicClassTable::isMatra(matraClass)) {
            if (IndicClassTable::isSplitMatra(matraClass)) {
                const SplitMatra *splitMatra = classTable->getSplitMatra(matraClass);
                int i;

                for (i = 0; i < 3 && (*splitMatra)[i] != 0; i += 1) {
                    LEUnicode piece = (*splitMatra)[i];
                    IndicClassTable::CharClass pieceClass = classTable->getCharClass(piece);

                    saveMatra(piece, pieceClass);
                }
            } else {
                saveMatra(matra, matraClass);
            }
        }
    }
    
    void noteVowelModifier(const IndicClassTable *classTable, LEUnicode vowelModifier, le_uint32 vowelModifierIndex, const LETag *vowelModifierTags)
    {
        IndicClassTable::CharClass vmClass = classTable->getCharClass(vowelModifier);
        
        fVMIndex = vowelModifierIndex;
        fVMTags  = vowelModifierTags;
        
        if (IndicClassTable::isVowelModifier(vmClass)) {
           switch (vmClass & IndicClassTable::CF_POS_MASK) {
           case IndicClassTable::CF_POS_ABOVE:
               fVMabove = vowelModifier;
               break;
            
           case IndicClassTable::CF_POS_AFTER:
               fVMpost = vowelModifier;
               break;
           
           default:
               // FIXME: this is an error...
               break;
           }
        }
    }
    
    void noteStressMark(const IndicClassTable *classTable, LEUnicode stressMark, le_uint32 stressMarkIndex, const LETag *stressMarkTags)
    {
       IndicClassTable::CharClass smClass = classTable->getCharClass(stressMark);
        
        fSMIndex = stressMarkIndex;
        fSMTags  = stressMarkTags;
        
        if (IndicClassTable::isStressMark(smClass)) {
            switch (smClass & IndicClassTable::CF_POS_MASK) {
            case IndicClassTable::CF_POS_ABOVE:
                fSMabove = stressMark;
                break;
            
            case IndicClassTable::CF_POS_BELOW:
                fSMbelow = stressMark;
                break;
           
            default:
                // FIXME: this is an error...
                break;
           }
        }
    }

    void noteBaseConsonant()
    {
        if (fMPreFixups != NULL && fMPreOutIndex >= 0) {
            fMPreFixups->add(fOutIndex, fMPreOutIndex);
        }
    }

    void writeMpre()
    {
        if (fMpre != 0) {
            fMPreOutIndex = fOutIndex;
            writeChar(fMpre, fMatraIndex, fMatraTags);
        }
    }

    void writeMbelow()
    {
        if (fMbelow != 0) {
            writeChar(fMbelow, fMatraIndex, fMatraTags);
        }
    }

    void writeMabove()
    {
        if (fMabove != 0) {
            writeChar(fMabove, fMatraIndex, fMatraTags);
        }
    }

    void writeMpost()
    {
        if (fMpost != 0) {
            writeChar(fMpost, fMatraIndex, fMatraTags);
        }
    }

    void writeLengthMark()
    {
        if (fLengthMark != 0) {
            writeChar(fLengthMark, fMatraIndex, fMatraTags);
        }
    }
    
    void writeVMabove()
    {
        if (fVMabove != 0) {
            writeChar(fVMabove, fVMIndex, fVMTags);
        }
    }
        
    void writeVMpost()
    {
        if (fVMpost != 0) {
            writeChar(fVMpost, fVMIndex, fVMTags);
        }
    }
    
    void writeSMabove()
    {
        if (fSMabove != 0) {
            writeChar(fSMabove, fSMIndex, fSMTags);
        }
    }
    
    void writeSMbelow()
    {
        if (fSMbelow != 0) {
            writeChar(fSMbelow, fSMIndex, fSMTags);
        }
    }
    
    void writeChar(LEUnicode ch, le_uint32 charIndex, const LETag *charTags)
    {
        LEErrorCode success = LE_NO_ERROR;

        fOutChars[fOutIndex] = ch;

        fGlyphStorage.setCharIndex(fOutIndex, charIndex, success);
        fGlyphStorage.setAuxData(fOutIndex, (void *) charTags, success);

        fOutIndex += 1;
    }

    le_int32 getOutputIndex()
    {
        return fOutIndex;
    }
};

enum
{
    C_DOTTED_CIRCLE = 0x25CC
};

const LETag emptyTag       = 0x00000000; // ''

const LETag nuktFeatureTag = LE_NUKT_FEATURE_TAG;
const LETag akhnFeatureTag = LE_AKHN_FEATURE_TAG;
const LETag rphfFeatureTag = LE_RPHF_FEATURE_TAG;
const LETag blwfFeatureTag = LE_BLWF_FEATURE_TAG;
const LETag halfFeatureTag = LE_HALF_FEATURE_TAG;
const LETag pstfFeatureTag = LE_PSTF_FEATURE_TAG;
const LETag vatuFeatureTag = LE_VATU_FEATURE_TAG;
const LETag presFeatureTag = LE_PRES_FEATURE_TAG;
const LETag blwsFeatureTag = LE_BLWS_FEATURE_TAG;
const LETag abvsFeatureTag = LE_ABVS_FEATURE_TAG;
const LETag pstsFeatureTag = LE_PSTS_FEATURE_TAG;
const LETag halnFeatureTag = LE_HALN_FEATURE_TAG;

const LETag blwmFeatureTag = LE_BLWM_FEATURE_TAG;
const LETag abvmFeatureTag = LE_ABVM_FEATURE_TAG;
const LETag distFeatureTag = LE_DIST_FEATURE_TAG;

// These are in the order in which the features need to be applied
// for correct processing
const LETag featureOrder[] =
{
    nuktFeatureTag, akhnFeatureTag, rphfFeatureTag, blwfFeatureTag, halfFeatureTag, pstfFeatureTag,
    vatuFeatureTag, presFeatureTag, blwsFeatureTag, abvsFeatureTag, pstsFeatureTag, halnFeatureTag,
    blwmFeatureTag, abvmFeatureTag, distFeatureTag, emptyTag
};

// The order of these is determined so that the tag array of each glyph can start
// at an offset into this array 
// FIXME: do we want a seperate tag array for each kind of character??
// FIXME: are there cases where this ordering causes glyphs to get tags
// that they shouldn't?
const LETag tagArray[] =
{
    rphfFeatureTag, blwfFeatureTag, halfFeatureTag, pstfFeatureTag, nuktFeatureTag, akhnFeatureTag,
    vatuFeatureTag, presFeatureTag, blwsFeatureTag, abvsFeatureTag, pstsFeatureTag, halnFeatureTag,
    blwmFeatureTag, abvmFeatureTag, distFeatureTag, emptyTag
};

const le_int8 stateTable[][IndicClassTable::CC_COUNT] =
{
//   xx  vm  sm  iv  ct  cn  nu  dv  vr  zw
    { 1,  1,  1,  5,  3,  2,  1,  1,  1,  1}, // 0
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1}, // 1
    {-1,  6,  1, -1, -1, -1, -1,  5,  4, -1}, // 2
    {-1,  6,  1, -1, -1, -1,  2,  5,  4, -1}, // 3
    {-1, -1, -1, -1,  3,  2, -1, -1, -1,  7}, // 4
    {-1,  6,  1, -1, -1, -1, -1, -1, -1, -1}, // 5
    {-1, -1,  1, -1, -1, -1, -1, -1, -1, -1}, // 6
    {-1, -1, -1, -1,  3,  2, -1, -1, -1, -1}  // 7

};

const LETag *IndicReordering::getFeatureOrder()
{
    return featureOrder;
}

le_int32 IndicReordering::findSyllable(const IndicClassTable *classTable, const LEUnicode *chars, le_int32 prev, le_int32 charCount)
{
    le_int32 cursor = prev;
    le_int8 state = 0;

    while (cursor < charCount) {
        IndicClassTable::CharClass charClass = classTable->getCharClass(chars[cursor]);

        state = stateTable[state][charClass & IndicClassTable::CF_CLASS_MASK];

        if (state < 0) {
            break;
        }

        cursor += 1;
    }

    return cursor;
}

le_int32 IndicReordering::reorder(const LEUnicode *chars, le_int32 charCount, le_int32 scriptCode,
                                  LEUnicode *outChars, LEGlyphStorage &glyphStorage,
                                  MPreFixups **outMPreFixups)
{
    MPreFixups *mpreFixups = NULL;
    const IndicClassTable *classTable = IndicClassTable::getScriptClassTable(scriptCode);

    if (classTable->scriptFlags & IndicClassTable::SF_MPRE_FIXUP) {
        mpreFixups = new MPreFixups(charCount);
    }

    ReorderingOutput output(outChars, glyphStorage, mpreFixups);
    le_int32 i, prev = 0;

    while (prev < charCount) {
        le_int32 syllable = findSyllable(classTable, chars, prev, charCount);
        le_int32 matra, markStart = syllable;

        output.reset();
        
        if (classTable->isStressMark(chars[markStart - 1])) {
            markStart -= 1;
            output.noteStressMark(classTable, chars[markStart], markStart, &tagArray[1]);
        }
        
        if (classTable->isVowelModifier(chars[markStart - 1])) {
            markStart -= 1;
            output.noteVowelModifier(classTable, chars[markStart], markStart, &tagArray[1]);
        }

        matra = markStart - 1;
        output.noteMatra(classTable, chars[matra], matra, &tagArray[1]);

        switch (classTable->getCharClass(chars[prev]) & IndicClassTable::CF_CLASS_MASK) {
        case IndicClassTable::CC_RESERVED:
        case IndicClassTable::CC_INDEPENDENT_VOWEL:
        case IndicClassTable::CC_ZERO_WIDTH_MARK:
            for (i = prev; i < syllable; i += 1) {
                output.writeChar(chars[i], i, &tagArray[1]);
            }

            break;

        case IndicClassTable::CC_VOWEL_MODIFIER:
        case IndicClassTable::CC_STRESS_MARK:
        case IndicClassTable::CC_NUKTA:
        case IndicClassTable::CC_VIRAMA:
            output.writeChar(C_DOTTED_CIRCLE, prev, &tagArray[1]);
            output.writeChar(chars[prev], prev, &tagArray[1]);
            break;

        case IndicClassTable::CC_DEPENDENT_VOWEL:
            output.writeMpre();
            output.writeChar(C_DOTTED_CIRCLE, prev, &tagArray[1]);
            output.writeMbelow();
            output.writeMabove();
            output.writeMpost();
            output.writeLengthMark();
            break;

        case IndicClassTable::CC_CONSONANT:
        case IndicClassTable::CC_CONSONANT_WITH_NUKTA:
        {
            le_uint32 length = markStart - prev;
            le_int32  lastConsonant = markStart - 1;
            le_int32  baseLimit = prev;

            // Check for REPH at front of syllable
            if (length > 2 && classTable->isReph(chars[prev]) && classTable->isVirama(chars[prev + 1])) {
                baseLimit += 2;

                // Check for eyelash RA, if the script supports it
                if ((classTable->scriptFlags & IndicClassTable::SF_EYELASH_RA) != 0 &&
                    chars[baseLimit] == C_SIGN_ZWJ) {
                    if (length > 3) {
                        baseLimit += 1;
                    } else {
                        baseLimit -= 2;
                    }
                }
            }

            while (lastConsonant > baseLimit && !classTable->isConsonant(chars[lastConsonant])) {
                lastConsonant -= 1;
            }

            le_int32 baseConsonant = lastConsonant;
            le_int32 postBase = lastConsonant + 1;
            le_int32 postBaseLimit = classTable->scriptFlags & IndicClassTable::SF_POST_BASE_LIMIT_MASK;
            le_bool  seenVattu = FALSE;
            le_bool  seenBelowBaseForm = FALSE;

            if (classTable->isNukta(chars[postBase])) {
                postBase += 1;
            }

            while (baseConsonant > baseLimit) {
                IndicClassTable::CharClass charClass = classTable->getCharClass(chars[baseConsonant]);

                if (IndicClassTable::isConsonant(charClass)) {
                    if (postBaseLimit == 0 || seenVattu ||
                        (baseConsonant > baseLimit && !classTable->isVirama(chars[baseConsonant - 1])) ||
                        !IndicClassTable::hasPostOrBelowBaseForm(charClass)) {
                        break;
                    }

                    seenVattu = IndicClassTable::isVattu(charClass);

                    if (IndicClassTable::hasPostBaseForm(charClass)) {
                        if (seenBelowBaseForm) {
                            break;
                        }

                        postBase = baseConsonant;
                    } else if (IndicClassTable::hasBelowBaseForm(charClass)) {
                        seenBelowBaseForm = TRUE;
                    }

                    postBaseLimit -= 1;
                }

                baseConsonant -= 1;
            }

            // Write Mpre
            output.writeMpre();

            // Write eyelash RA
            // NOTE: baseLimit == prev + 3 iff eyelash RA present...
            if (baseLimit == prev + 3) {
                output.writeChar(chars[prev], prev, &tagArray[2]);
                output.writeChar(chars[prev + 1], prev + 1, &tagArray[2]);
                output.writeChar(chars[prev + 2], prev + 2, &tagArray[2]);
            }

            // write any pre-base consonants
            le_bool supressVattu = TRUE;

            for (i = baseLimit; i < baseConsonant; i += 1) {
                LEUnicode ch = chars[i];
                const LETag *tag = &tagArray[1];
                IndicClassTable::CharClass charClass = classTable->getCharClass(ch);

                if (IndicClassTable::isConsonant(charClass)) {
                    if (IndicClassTable::isVattu(charClass) && supressVattu) {
                        tag = &tagArray[4];
                    }

                    supressVattu = IndicClassTable::isVattu(charClass);
                } else if (IndicClassTable::isVirama(charClass) && chars[i + 1] == C_SIGN_ZWNJ)
                {
                    tag = &tagArray[4];
                }

                output.writeChar(ch, i, tag);
            }

            le_int32 bcSpan = baseConsonant + 1;

            if (bcSpan < markStart && classTable->isNukta(chars[bcSpan])) {
                bcSpan += 1;
            }

            if (baseConsonant == lastConsonant && bcSpan < markStart && classTable->isVirama(chars[bcSpan])) {
                bcSpan += 1;

                if (bcSpan < markStart && chars[bcSpan] == C_SIGN_ZWNJ) {
                    bcSpan += 1;
                }
            }

            // note the base consonant for post-GSUB fixups
            output.noteBaseConsonant();

            // write base consonant
            for (i = baseConsonant; i < bcSpan; i += 1) {
                output.writeChar(chars[i], i, &tagArray[4]);
            }

            if ((classTable->scriptFlags & IndicClassTable::SF_MATRAS_AFTER_BASE) != 0) {
                output.writeMbelow();
                output.writeSMbelow(); // FIXME: there are no SMs in these scripts...
                output.writeMabove();
                output.writeMpost();
            }

            // write below-base consonants
            if (baseConsonant != lastConsonant) {
                for (i = bcSpan + 1; i < postBase; i += 1) {
                    output.writeChar(chars[i], i, &tagArray[1]);
                }

                if (postBase > lastConsonant) {
                    // write halant that was after base consonant
                    output.writeChar(chars[bcSpan], bcSpan, &tagArray[1]);
                }
            }

            // write Mbelow, SMbelow, Mabove
            if ((classTable->scriptFlags & IndicClassTable::SF_MATRAS_AFTER_BASE) == 0) {
                output.writeMbelow();
                output.writeSMbelow();
                output.writeMabove();
            }

            if ((classTable->scriptFlags & IndicClassTable::SF_REPH_AFTER_BELOW) != 0) {
                if (baseLimit == prev + 2) {
                    output.writeChar(chars[prev], prev, &tagArray[0]);
                    output.writeChar(chars[prev + 1], prev + 1, &tagArray[0]);
                }

                output.writeVMabove();
                output.writeSMabove(); // FIXME: there are no SM's in these scripts...
            }

            // write post-base consonants
            // FIXME: does this put the right tags on post-base consonants?
            if (baseConsonant != lastConsonant) {
                if (postBase <= lastConsonant) {
                    for (i = postBase; i <= lastConsonant; i += 1) {
                        output.writeChar(chars[i], i, &tagArray[3]);
                    }

                    // write halant that was after base consonant
                    output.writeChar(chars[bcSpan], bcSpan, &tagArray[1]);
                }

                // write the training halant, if there is one
                if (lastConsonant < matra && classTable->isVirama(chars[matra])) {
                    output.writeChar(chars[matra], matra, &tagArray[4]);
                }
            }

            // write Mpost
            if ((classTable->scriptFlags & IndicClassTable::SF_MATRAS_AFTER_BASE) == 0) {
                output.writeMpost();
            }

            output.writeLengthMark();

            // write reph
            if ((classTable->scriptFlags & IndicClassTable::SF_REPH_AFTER_BELOW) == 0) {
                if (baseLimit == prev + 2) {
                    output.writeChar(chars[prev], prev, &tagArray[0]);
                    output.writeChar(chars[prev + 1], prev + 1, &tagArray[0]);
                }

                output.writeVMabove();
                output.writeSMabove();
            }

            output.writeVMpost();

            break;
        }

        default:
            break;
        }

        prev = syllable;
    }

    *outMPreFixups = mpreFixups;

    return output.getOutputIndex();
}

void IndicReordering::adjustMPres(MPreFixups *mpreFixups, LEGlyphStorage &glyphStorage)
{
    if (mpreFixups != NULL) {
        mpreFixups->apply(glyphStorage);
        
        delete mpreFixups;
    }
}

U_NAMESPACE_END
