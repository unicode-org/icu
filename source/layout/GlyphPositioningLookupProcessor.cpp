/*
 * @(#)GlyphPositioningLookupProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Features.h"
#include "Lookups.h"
#include "ScriptAndLanguage.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"
#include "SinglePositioningSubtables.h"
#include "PairPositioningSubtables.h"
//#include "CursivePositioningSubtables.h"
#include "MarkToBasePositioningSubtables.h"
#include "MarkToLigaturePositioningSubtables.h"
#include "MarkToMarkPositioningSubtables.h"
//#include "ContextualPositioningSubtables.h"
#include "ContextualSubstitutionSubtables.h"
#include "LookupProcessor.h"
#include "GlyphPositioningLookupProcessor.h"
#include "LESwaps.h"

// Aside from the names, the contextual positioning subtables are
// the same as the contextual substitution subtables.
typedef ContextualSubstitutionSubtable ContextualPositioningSubtable;
typedef ChainingContextualSubstitutionSubtable ChainingContextualPositioningSubtable;

GlyphPositioningLookupProcessor::GlyphPositioningLookupProcessor(
        GlyphPositioningTableHeader *glyphPositioningTableHeader,
        LETag scriptTag, LETag languageTag)
    : LookupProcessor(
                      (char *) glyphPositioningTableHeader,
                      SWAPW(glyphPositioningTableHeader->scriptListOffset),
                      SWAPW(glyphPositioningTableHeader->featureListOffset),
                      SWAPW(glyphPositioningTableHeader->lookupListOffset),
                      scriptTag, languageTag)
{
    // anything?
}

GlyphPositioningLookupProcessor::GlyphPositioningLookupProcessor()
{
}

le_uint32 GlyphPositioningLookupProcessor::applySubtable(LookupSubtable *lookupSubtable, le_uint16 lookupType,
                                                       GlyphIterator *glyphIterator,
                                                       const LEFontInstance *fontInstance)
{
    le_uint32 delta = 0;

    switch(lookupType)
    {
    case 0:
        break;

    case gpstSingle:
    {
        SinglePositioningSubtable *subtable = (SinglePositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }

    case gpstPair:
    {
        PairPositioningSubtable *subtable = (PairPositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }

#if 0
    case gpstCursive:
    {
        CursivePositioningSubtable *subtable = (CursivePositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }
#endif

    case gpstMarkToBase:
    {
        MarkToBasePositioningSubtable *subtable = (MarkToBasePositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }

     case gpstMarkToLigature:
    {
        MarkToLigaturePositioningSubtable *subtable = (MarkToLigaturePositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }

    case gpstMarkToMark:
    {
        MarkToMarkPositioningSubtable *subtable = (MarkToMarkPositioningSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fontInstance);
        break;
    }

   case gpstContext:
    {
        ContextualPositioningSubtable *subtable = (ContextualPositioningSubtable *) lookupSubtable;

        delta = subtable->process(this, glyphIterator, fontInstance);
        break;
    }

    case gpstChainedContext:
    {
        ChainingContextualPositioningSubtable *subtable = (ChainingContextualPositioningSubtable *) lookupSubtable;

        delta = subtable->process(this, glyphIterator, fontInstance);
        break;
    }

    default:
        break;
    }

    return delta;
}

GlyphPositioningLookupProcessor::~GlyphPositioningLookupProcessor()
{
}

