/*
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 * $Source: /xsrl/Nsvn/icu/icu/source/layout/IndicReordering.cpp,v $
 * $Date: 2003/12/08 22:41:38 $
 * $Revision: 1.14 $
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "IndicReordering.h"
#include "MPreFixups.h"

U_NAMESPACE_BEGIN

class ReorderingOutput : public UMemory {
private:
    le_int32 fOutIndex;

    LEUnicode *fOutChars;
    le_int32 *fCharIndices;
    const LETag **fCharTags;

    LEUnicode fMpre;
    LEUnicode fMbelow;
    LEUnicode fMabove;
    LEUnicode fMpost;
    LEUnicode fLengthMark;
    le_int32 fMatraIndex;
    const LETag *fMatraTags;
    le_int32 fMPreOutIndex;

    MPreFixups *fMPreFixups;

    void saveMatra(LEUnicode matra, IndicClassTable::CharClass matraClass)
    {
        // FIXME: check if already set, or if not a matra...
        if (IndicClassTable::isMpre(matraClass)) {
            fMpre = matra;
        } else if (IndicClassTable::isMbelow(matraClass)) {
            fMbelow = matra;
        } else if (IndicClassTable::isMabove(matraClass)) {
            fMabove = matra;
        } else if (IndicClassTable::isMpost(matraClass)) {
            fMpost = matra;
        } else if (IndicClassTable::isLengthMark(matraClass)) {
            fLengthMark = matra;
        }
    }

public:
    ReorderingOutput(LEUnicode *outChars, le_int32 *charIndices, const LETag **charTags, MPreFixups *mpreFixups)
        : fOutIndex(0), fOutChars(outChars), fCharIndices(charIndices), fCharTags(charTags),
          fMpre(0), fMbelow(0), fMabove(0), fMpost(0), fLengthMark(0),
          fMatraIndex(0), fMatraTags(NULL), fMPreOutIndex(-1), fMPreFixups(mpreFixups)
    {
        // nothing else to do...
    }

    ~ReorderingOutput()
    {
        // nothing to do here...
    }

    void noteMatra(const IndicClassTable *classTable, LEUnicode matra, le_uint32 matraIndex, const LETag *matraTags)
    {
        IndicClassTable::CharClass matraClass = classTable->getCharClass(matra);

        fMpre = fMbelow = fMabove = fMpost = fLengthMark = 0;
        fMPreOutIndex = -1;
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

    void writeChar(LEUnicode ch, le_uint32 charIndex, const LETag *charTags)
    {
        fOutChars[fOutIndex] = ch;
        fCharIndices[fOutIndex] = charIndex;
        fCharTags[fOutIndex] = charTags;

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
//   xx  ma  mp  iv  ct  cn  nu  dv  vr  zw
    { 1,  1,  1,  5,  3,  2,  1,  1,  1,  1},
    {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
    {-1,  6,  1, -1, -1, -1, -1,  5,  4, -1},
    {-1,  6,  1, -1, -1, -1,  2,  5,  4, -1},
    {-1, -1, -1, -1,  3,  2, -1, -1, -1,  8},
    {-1,  6,  1, -1, -1, -1, -1, -1, -1, -1},
    {-1,  7,  1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1,  1, -1, -1, -1, -1, -1, -1, -1},
    {-1, -1, -1, -1,  3,  2, -1, -1, -1, -1}

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
                                  LEUnicode *outChars, le_int32 *charIndices, const LETag **charTags,
                                  MPreFixups **outMPreFixups)
{
    MPreFixups *mpreFixups = NULL;
    const IndicClassTable *classTable = IndicClassTable::getScriptClassTable(scriptCode);

    if (classTable->scriptFlags & IndicClassTable::SF_MPRE_FIXUP) {
        mpreFixups = new MPreFixups(charCount);
    }

    ReorderingOutput output(outChars, charIndices, charTags, mpreFixups);
    le_int32 i, prev = 0;

    while (prev < charCount) {
        le_int32 syllable = findSyllable(classTable, chars, prev, charCount);
        le_int32 matra, vmabove, vmpost = syllable;

        while (vmpost > prev && classTable->isVMpost(chars[vmpost - 1])) {
            vmpost -= 1;
        }

        vmabove = vmpost;
        while (vmabove > prev && classTable->isVMabove(chars[vmabove - 1])) {
            vmabove -= 1;
        }

        matra = vmabove - 1;
        output.noteMatra(classTable, chars[matra], matra, &tagArray[1]);

        switch (classTable->getCharClass(chars[prev]) & IndicClassTable::CF_CLASS_MASK) {
        case IndicClassTable::CC_RESERVED:
        case IndicClassTable::CC_INDEPENDENT_VOWEL:
        case IndicClassTable::CC_ZERO_WIDTH_MARK:
            for (i = prev; i < syllable; i += 1) {
                output.writeChar(chars[i], i, &tagArray[1]);
            }

            break;

        case IndicClassTable::CC_MODIFYING_MARK_ABOVE:
        case IndicClassTable::CC_MODIFYING_MARK_POST:
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
            le_uint32 length = vmabove - prev;
            le_int32  lastConsonant = vmabove - 1;
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

            if (bcSpan < vmabove && classTable->isNukta(chars[bcSpan])) {
                bcSpan += 1;
            }

            if (baseConsonant == lastConsonant && bcSpan < vmabove && classTable->isVirama(chars[bcSpan])) {
                bcSpan += 1;

                if (bcSpan < vmabove && chars[bcSpan] == C_SIGN_ZWNJ) {
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

            // write Mbelow, Mabove
            if ((classTable->scriptFlags & IndicClassTable::SF_MATRAS_AFTER_BASE) == 0) {
                output.writeMbelow();
                output.writeMabove();
            }

           if ((classTable->scriptFlags & IndicClassTable::SF_REPH_AFTER_BELOW) != 0) {
                if (baseLimit == prev + 2) {
                    output.writeChar(chars[prev], prev, &tagArray[0]);
                    output.writeChar(chars[prev + 1], prev + 1, &tagArray[0]);
                }

                // write VMabove
                for (i = vmabove; i < vmpost; i += 1) {
                    output.writeChar(chars[i], i, &tagArray[1]);
                }
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

                // write VMabove
                for (i = vmabove; i < vmpost; i += 1) {
                    output.writeChar(chars[i], i, &tagArray[1]);
                }
            }

            // write VMpost
            for (i = vmpost; i < syllable; i += 1) {
                output.writeChar(chars[i], i, &tagArray[1]);
            }

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

void IndicReordering::adjustMPres(MPreFixups *mpreFixups, LEGlyphID *glyphs, le_int32 *charIndices)
{
    if (mpreFixups != NULL) {
        mpreFixups->apply(glyphs, charIndices);
        
        delete mpreFixups;
    }
}

U_NAMESPACE_END
