/*
 * @(#)MarkToLigaturePosnSubtables.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "AnchorTables.h"
#include "MarkArrays.h"
#include "GlyphPositioningTables.h"
#include "AttachmentPosnSubtables.h"
#include "MarkToLigaturePosnSubtables.h"
#include "GlyphIterator.h"
#include "LESwaps.h"

LEGlyphID MarkToLigaturePositioningSubtable::findLigatureGlyph(GlyphIterator *glyphIterator)
{
    if (glyphIterator->prev()) {
        return glyphIterator->getCurrGlyphID();
    }

    return 0xFFFF;
}

le_int32 MarkToLigaturePositioningSubtable::process(GlyphIterator *glyphIterator, const LEFontInstance *fontInstance)
{
    LEGlyphID markGlyph = glyphIterator->getCurrGlyphID();
    le_int32 markCoverage = getGlyphCoverage((LEGlyphID) markGlyph);

    if (markCoverage < 0) {
        // markGlyph isn't a covered mark glyph
        return 0;
    }

    LEPoint markAnchor;
    MarkArray *markArray = (MarkArray *) ((char *) this + SWAPW(markArrayOffset));
    le_int32 markClass = markArray->getMarkClass(markGlyph, markCoverage, fontInstance, markAnchor);
    le_uint16 mcCount = SWAPW(classCount);

    if (markClass < 0 || markClass >= mcCount) {
        // markGlyph isn't in the mark array or its
        // mark class is too big. The table is mal-formed!
        return 0;
    }

    // FIXME: we probably don't want to find a ligature before a previous base glyph...
    GlyphIterator ligatureIterator(*glyphIterator, lfIgnoreMarks /*| lfIgnoreBaseGlyphs*/);
    LEGlyphID ligatureGlyph = findLigatureGlyph(&ligatureIterator);
    le_int32 ligatureCoverage = getBaseCoverage((LEGlyphID) ligatureGlyph);
    LigatureArray *ligatureArray = (LigatureArray *) ((char *) this + SWAPW(baseArrayOffset));
    le_uint16 ligatureCount = SWAPW(ligatureArray->ligatureCount);

    if (ligatureCoverage < 0 || ligatureCoverage >= ligatureCount) {
        // The ligature glyph isn't covered, or the coverage
        // index is too big. The latter means that the
        // table is mal-formed...
        return 0;
    }

    le_int32 markPosition = glyphIterator->getCurrStreamPosition();
    le_int32 component = ligatureIterator.getMarkComponent(markPosition);
    Offset ligatureAttachOffset = SWAPW(ligatureArray->ligatureAttachTableOffsetArray[ligatureCoverage]);
    LigatureAttachTable *ligatureAttachTable = (LigatureAttachTable *) ((char *) ligatureArray + ligatureAttachOffset);
    ComponentRecord *componentRecord = &ligatureAttachTable->componentRecordArray[component * mcCount];
    Offset anchorTableOffset = SWAPW(componentRecord->ligatureAnchorTableOffsetArray[markClass]);
    AnchorTable *anchorTable = (AnchorTable *) ((char *) ligatureAttachTable + anchorTableOffset);
    LEPoint ligatureAnchor, markAdvance, pixels;

    anchorTable->getAnchor(ligatureGlyph, fontInstance, ligatureAnchor);

    fontInstance->getGlyphAdvance(markGlyph, pixels);
    fontInstance->pixelsToUnits(pixels, markAdvance);

    float anchorDiffX = ligatureAnchor.fX - markAnchor.fX;
    float anchorDiffY = ligatureAnchor.fY - markAnchor.fY;

    if (glyphIterator->isRightToLeft()) {
        float adjustX = markAdvance.fX + anchorDiffX;

        glyphIterator->adjustCurrGlyphPositionAdjustment(anchorDiffX, -anchorDiffY, -adjustX, anchorDiffY);
    } else {
        LEPoint ligatureAdvance;

        fontInstance->getGlyphAdvance(ligatureGlyph, pixels);
        fontInstance->pixelsToUnits(pixels, ligatureAdvance);

        float adjustX = ligatureAdvance.fX - anchorDiffX;
        float advAdjustX = adjustX - markAdvance.fX;

        glyphIterator->adjustCurrGlyphPositionAdjustment(-adjustX, -anchorDiffY, advAdjustX, anchorDiffY);
    }

    return 1;
}
