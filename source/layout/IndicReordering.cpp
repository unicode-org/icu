/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "IndicReordering.h"
#include "ScriptAndLanguageTags.h"

class ReorderingOutput
{
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
    ReorderingOutput(LEUnicode *outChars, le_int32 *charIndices, const LETag **charTags)
        : fOutIndex(0), fOutChars(outChars), fCharIndices(charIndices), fCharTags(charTags),
          fMpre(0), fMbelow(0), fMabove(0), fMpost(0), fLengthMark(0),
          fMatraIndex(0), fMatraTags(NULL)
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

    void writeMpre()
    {
        if (fMpre != 0) {
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

const LETag nuktFeatureTag = 0x6E756B74; // 'nukt'
const LETag akhnFeatureTag = 0x616B686E; // 'akhn'
const LETag rphfFeatureTag = 0x72706866; // 'rphf'
const LETag blwfFeatureTag = 0x626C7766; // 'blwf'
const LETag halfFeatureTag = 0x68616C66; // 'half'
const LETag pstfFeatureTag = 0x70737466; // 'pstf'
const LETag vatuFeatureTag = 0x76617475; // 'vatu'
const LETag presFeatureTag = 0x70726573; // 'pres'
const LETag blwsFeatureTag = 0x626C7773; // 'blws'
const LETag abvsFeatureTag = 0x61627673; // 'abvs'
const LETag pstsFeatureTag = 0x70737473; // 'psts'
const LETag halnFeatureTag = 0x68616C6E; // 'haln'

const LETag blwmFeatureTag = 0x626C776D; // 'blwm'
const LETag abvmFeatureTag = 0x6162766D; // 'abvm'
const LETag distFeatureTag = 0x64697374; // 'dist'

// FIXME: do we want a seperate tag array for each kind of character??
const LETag tagArray[] =
{
    rphfFeatureTag, blwfFeatureTag, halfFeatureTag, nuktFeatureTag, akhnFeatureTag, pstfFeatureTag,
    vatuFeatureTag, presFeatureTag, blwsFeatureTag, abvsFeatureTag, pstsFeatureTag, halnFeatureTag,
    blwmFeatureTag, abvmFeatureTag, emptyTag
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

le_int32 IndicReordering::reorder(const LEUnicode *chars, le_int32 charCount, le_int32 scriptCode, LEUnicode *outChars, le_int32 *charIndices, const LETag **charTags)
{
    const IndicClassTable *classTable = IndicClassTable::getScriptClassTable(scriptCode);
    ReorderingOutput output(outChars, charIndices, charTags);
    le_int32 i, prev = 0;

    while (prev < charCount) {
        le_int32 syllable = findSyllable(classTable, chars, prev, charCount);
        le_int32 matra, vmabove, vmpost = syllable;
        le_int16 flags = 0;

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
            le_int32 lastConsonant = vmabove - 1;
            le_int32 baseLimit = prev;

            // Check for REPH at front of syllable
            if (length > 2 && classTable->isReph(chars[prev]) && classTable->isVirama(chars[prev + 1])) {
                baseLimit = prev + 2;

                // Check for eyelash RA, if the script supports it
                if ((classTable->scriptFlags & IndicClassTable::SF_EYELASH_RA) != 0 &&
                    chars[prev + 2] == C_SIGN_ZWJ) {
                    if (length > 3) {
                        baseLimit += 1;
                    } else {
                        baseLimit = prev;
                    }
                }
            }

            while (lastConsonant >= baseLimit && !classTable->isConsonant(chars[lastConsonant])) {
                lastConsonant -= 1;
            }

            le_int32 baseConsonant = lastConsonant;
            le_int32 postBase = lastConsonant + 1;

            if (lastConsonant >= prev) {
                int postBaseLimit = classTable->scriptFlags & IndicClassTable::SF_POST_BASE_LIMIT_MASK;
                le_bool seenVattu = false;
                le_bool seenBelowBaseForm = false;

                while (baseConsonant >= baseLimit) {
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
                            seenBelowBaseForm = true;
                        }

                        postBaseLimit -= 1;
                    }

                    baseConsonant -= 1;
                }

                if (baseConsonant < baseLimit) {
                    baseConsonant = baseLimit;
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
                le_bool supressVattu = true;

                for (i = baseLimit; i < baseConsonant; i += 1) {
                    LEUnicode ch = chars[i];
                    const LETag *tag = &tagArray[1];
                    IndicClassTable::CharClass charClass = classTable->getCharClass(ch);

                    if (IndicClassTable::isConsonant(charClass)) {
                        if (IndicClassTable::isVattu(charClass) && supressVattu) {
                            tag = &tagArray[3];
                        }

                        supressVattu = IndicClassTable::isVattu(charClass);
                    } else if (IndicClassTable::isVirama(charClass) && chars[i + 1] == C_SIGN_ZWNJ)
                    {
                        tag = &tagArray[3];
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

                // write base consonant
                for (i = baseConsonant; i < bcSpan; i += 1) {
                    output.writeChar(chars[i], i, &tagArray[3]);
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
                        output.writeChar(chars[matra], matra, &tagArray[3]);
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
            }

            break;
        }

        default:
            break;
        }


        prev = syllable;
    }

    return output.getOutputIndex();
}

void IndicReordering::adjustMPres(const LEUnicode *chars, le_int32 charCount, LEGlyphID *glyphs, le_int32 *charIndices, le_int32 scriptCode)
{
    const IndicClassTable *classTable = IndicClassTable::getScriptClassTable(scriptCode);

    if (classTable->scriptFlags & IndicClassTable::SF_MPRE_FIXUP) {
        le_int32 i;

        for (i = 0; i < charCount; i += 1) {
            if (classTable->isMpre(chars[i])) {
                le_int32 j;
                le_bool cflag = true;

                for (j = i + 1; j < charCount; j += 1) {
                    IndicClassTable::CharClass charClass = classTable->getCharClass(chars[j]);

                    if (IndicClassTable::isConsonant(charClass)) {
                        if (! cflag) {
                            break;
                        }

                        cflag = false;
                    } else if (IndicClassTable::isVirama(charClass)) {
                        if (cflag) {
                            break;
                        }

                        cflag = true;
                    } else {
                        break;
                    }
                }

                // Don't bother to reorder if
                // there's one or fewer consonants
                if (j <= i + 2) {
                    continue;
                }

                int lastConsonant = j - 1;
                int base;

                for (base = lastConsonant; base > i; base -= 1) {
                    if (classTable->isConsonant(chars[base]) && glyphs[base] != 0xFFFF) {
                        break;
                    }
                }

                LEGlyphID matra = glyphs[i];
                le_int32 mIndex = charIndices[i];
                le_int32 x;

                for (x = i; x < base - 1; x += 1) {
                    glyphs[x] = glyphs[x + 1];
                    charIndices[x] = charIndices[x + 1];
                }

                glyphs[base - 1] = matra;
                charIndices[base - 1] = mIndex;
            }
        }
    }
}

