/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001, 2002 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "GlyphPositioningTables.h"
#include "CursiveAttachmentSubtables.h"
#include "AnchorTables.h"
#include "GlyphIterator.h"
#include "GlyphPositionAdjustments.h"
#include "OpenTypeUtilities.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

le_uint32 CursiveAttachmentSubtable::process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance) const
{
    LEGlyphID glyphID       = glyphIterator->getCurrGlyphID();
    le_int32  coverageIndex = getGlyphCoverage(glyphID);
    le_uint16 eeCount       = SWAPW(entryExitCount);

    if (coverageIndex < 0 || coverageIndex >= eeCount) {
        glyphIterator->resetCursiveLastExitPoint();
        return 0;
    }

    LEPoint entryAnchor, exitAnchor, pixels;

    if (glyphIterator->hasCursiveLastExitPoint()) {
        Offset entryOffset = SWAPW(entryExitRecords[coverageIndex].entryAnchor);

        const AnchorTable *entryAnchorTable = (const AnchorTable *) ((char *) this + entryOffset);

        entryAnchorTable->getAnchor(glyphID, fontInstance, entryAnchor);
        glyphIterator->getCursiveLastExitPoint(exitAnchor);

        float anchorDiffX        = exitAnchor.fX - entryAnchor.fX;
        float anchorDiffY        = exitAnchor.fY - entryAnchor.fY;
        float baselineAdjustment = glyphIterator->getCursiveBaselineAdjustment();

        if (glyphIterator->isRightToLeft()) {
            LEPoint secondAdvance;

            fontInstance->getGlyphAdvance(glyphID, pixels);
            fontInstance->pixelsToUnits(pixels, secondAdvance);

            glyphIterator->adjustCurrGlyphPositionAdjustment(0, anchorDiffY + baselineAdjustment, -(anchorDiffX + secondAdvance.fX), 0);
        } else {
            LEPoint firstAdvance;

            fontInstance->getGlyphAdvance(glyphIterator->getCursiveLastGlyphID(), pixels);
            fontInstance->pixelsToUnits(pixels, firstAdvance);

            glyphIterator->adjustCursiveLastGlyphPositionAdjustment(0, 0, anchorDiffX - firstAdvance.fX, 0);
            glyphIterator->adjustCurrGlyphPositionAdjustment(0, anchorDiffY + baselineAdjustment, 0, 0);
        }

        glyphIterator->setCursiveBaselineAdjustment(anchorDiffY + baselineAdjustment);
    }

    Offset exitOffset = SWAPW(entryExitRecords[coverageIndex].exitAnchor);

    const AnchorTable *exitAnchorTable = (const AnchorTable *) ((char *) this + exitOffset);

    exitAnchorTable->getAnchor(glyphID, fontInstance, exitAnchor);

    if (!glyphIterator->hasCursiveFirstExitPoint()) {
        glyphIterator->setCursiveFirstExitPoint();
    }

    glyphIterator->setCursiveLastExitPoint(exitAnchor);

    return 1;
}

U_NAMESPACE_END
