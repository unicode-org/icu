/*
 * @(#)GlyphSubstLookupProc.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "Features.h"
#include "Lookups.h"
#include "ScriptAndLanguage.h"
#include "GlyphDefinitionTables.h"
#include "GlyphSubstitutionTables.h"
#include "SingleSubstitutionSubtables.h"
#include "MultipleSubstitutionSubtables.h"
#include "AlternateSubstSubtables.h"
#include "LigatureSubstitutionSubtables.h"
#include "ContextualSubstSubtables.h"
#include "LookupProcessor.h"
#include "GlyphSubstLookupProc.h"
#include "LESwaps.h"

GlyphSubstitutionLookupProcessor::GlyphSubstitutionLookupProcessor(
        GlyphSubstitutionTableHeader *glyphSubstitutionTableHeader,
        LETag scriptTag, LETag languageTag, LEGlyphFilter *filter)
    : LookupProcessor(
                      (char *) glyphSubstitutionTableHeader,
                      SWAPW(glyphSubstitutionTableHeader->scriptListOffset),
                      SWAPW(glyphSubstitutionTableHeader->featureListOffset),
                      SWAPW(glyphSubstitutionTableHeader->lookupListOffset),
                      scriptTag, languageTag), fFilter(filter)
{
    // anything?
}

GlyphSubstitutionLookupProcessor::GlyphSubstitutionLookupProcessor()
{
}

le_uint32 GlyphSubstitutionLookupProcessor::applySubtable(LookupSubtable *lookupSubtable, le_uint16 lookupType,
                                                       GlyphIterator *glyphIterator, const LEFontInstance *fontInstance)
{
    le_uint32 delta = 0;

    switch(lookupType)
    {
    case 0:
        break;

    case gsstSingle:
    {
        SingleSubstitutionSubtable *subtable = (SingleSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fFilter);
        break;
    }

    case gsstMultiple:
    {
        MultipleSubstitutionSubtable *subtable = (MultipleSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fFilter);
        break;
    }

    case gsstAlternate:
    {
        AlternateSubstitutionSubtable *subtable = (AlternateSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fFilter);
        break;
    }

    case gsstLigature:
    {
        LigatureSubstitutionSubtable *subtable = (LigatureSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(glyphIterator, fFilter);
        break;
    }

    case gsstContext:
    {
        ContextualSubstitutionSubtable *subtable = (ContextualSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(this, glyphIterator, fontInstance);
        break;
    }

    case gsstChainingContext:
    {
        ChainingContextualSubstitutionSubtable *subtable = (ChainingContextualSubstitutionSubtable *) lookupSubtable;

        delta = subtable->process(this, glyphIterator, fontInstance);
        break;
    }

    default:
        break;
    }

    return delta;
}

GlyphSubstitutionLookupProcessor::~GlyphSubstitutionLookupProcessor()
{
}

