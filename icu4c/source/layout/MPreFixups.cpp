/*
 * (C) Copyright IBM Corp. 2002-2003 - All Rights Reserved
 */

#include "LETypes.h"
#include "MPreFixups.h"

U_NAMESPACE_BEGIN

struct FixupData
{
    le_int32 fBaseIndex;
    le_int32 fMPreIndex;
};

MPreFixups::MPreFixups(le_int32 charCount)
    : fFixupData(NULL), fFixupCount(0)
{
    fFixupData = LE_NEW_ARRAY(FixupData, charCount);
}

MPreFixups::~MPreFixups()
{
    LE_DELETE_ARRAY(fFixupData);
    fFixupData = NULL;
}

void MPreFixups::add(le_int32 baseIndex, le_int32 mpreIndex)
{
    // NOTE: don't add the fixup data if the mpre is right
    // before the base consonant glyph.
    if (baseIndex - mpreIndex > 1) {
        fFixupData[fFixupCount].fBaseIndex = baseIndex;
        fFixupData[fFixupCount].fMPreIndex = mpreIndex;

        fFixupCount += 1;
    }
}

void MPreFixups::apply(LEGlyphID *glyphs, le_int32 *charIndices)
{
    for (le_int32 fixup = 0; fixup < fFixupCount; fixup += 1) {
        le_int32 baseIndex = fFixupData[fixup].fBaseIndex;
        le_int32 mpreIndex = fFixupData[fixup].fMPreIndex;
        le_int32 mpreLimit = mpreIndex + 1;

        while (glyphs[baseIndex] == 0xFFFF || glyphs[baseIndex] == 0xFFFE) {
            baseIndex -= 1;
        }

        while (glyphs[mpreLimit] == 0xFFFF || glyphs[mpreLimit] == 0xFFFE) {
            mpreLimit += 1;
        }

        if (mpreLimit == baseIndex) {
            continue;
        }

        le_int32   mpreCount = mpreLimit - mpreIndex;
        le_int32   moveCount = baseIndex - mpreLimit;
        le_int32   mpreDest  = baseIndex - mpreCount;
        LEGlyphID *mpreSave  = LE_NEW_ARRAY(LEGlyphID, mpreCount);
        le_int32  *indexSave = LE_NEW_ARRAY(le_int32, mpreCount);
        le_int32   i;

        for (i = 0; i < mpreCount; i += 1) {
            mpreSave[i]  = glyphs[mpreIndex + i];
            indexSave[i] = charIndices[mpreIndex + i];
        }

        for (i = 0; i < moveCount; i += 1) {
            glyphs[mpreIndex + i] = glyphs[mpreLimit + i];
            charIndices[mpreIndex + i] = charIndices[mpreLimit + i];
        }

        for (i = 0; i < mpreCount; i += 1) {
            glyphs[mpreDest + i] = mpreSave[i];
            charIndices[mpreDest + i] = indexSave[i];
        }
        
        LE_DELETE_ARRAY(indexSave);
        LE_DELETE_ARRAY(mpreSave);
    }
}

U_NAMESPACE_END
