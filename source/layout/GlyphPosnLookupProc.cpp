/*
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"
#include "ICUFeatures.h"
#include "Lookups.h"
#include "ScriptAndLanguage.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"
#include "SinglePositioningSubtables.h"
#include "PairPositioningSubtables.h"
#include "CursiveAttachmentSubtables.h"
#include "MarkToBasePosnSubtables.h"
#include "MarkToLigaturePosnSubtables.h"
#include "MarkToMarkPosnSubtables.h"
//#include "ContextualPositioningSubtables.h"
#include "ContextualSubstSubtables.h"
#include "ExtensionSubtables.h"
#include "LookupProcessor.h"
#include "GlyphPosnLookupProc.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

// Aside from the names, the contextual positioning subtables are
// the same as the contextual substitution subtables.
typedef ContextualSubstitutionSubtable ContextualPositioningSubtable;
typedef ChainingContextualSubstitutionSubtable ChainingContextualPositioningSubtable;

GlyphPositioningLookupProcessor::GlyphPositioningLookupProcessor(
        const LEReferenceTo<GlyphPositioningTableHeader> &glyphPositioningTableHeader,
        LETag scriptTag, 
        LETag languageTag, 
        const FeatureMap *featureMap, 
        le_int32 featureMapCount, 
        le_bool featureOrder,
        LEErrorCode& success)
    : LookupProcessor(
                      glyphPositioningTableHeader,
                      SWAPW(glyphPositioningTableHeader->scriptListOffset),
                      SWAPW(glyphPositioningTableHeader->featureListOffset),
                      SWAPW(glyphPositioningTableHeader->lookupListOffset),
                      scriptTag, 
                      languageTag, 
                      featureMap, 
                      featureMapCount, 
                      featureOrder,
                      success
                      )
{
    // anything?
}

GlyphPositioningLookupProcessor::GlyphPositioningLookupProcessor()
{
}

le_uint32 GlyphPositioningLookupProcessor::applySubtable(const LEReferenceTo<LookupSubtable> &lookupSubtable, le_uint16 lookupType,
                                                       GlyphIterator *glyphIterator,
                                                       const LEFontInstance *fontInstance,
                                                       LEErrorCode& success) const
{
    if (LE_FAILURE(success)) { 
        return 0;
    } 

    le_uint32 delta = 0;

    switch(lookupType)
    {
    case 0:
        break;

    case gpstSingle:
    {
      LEReferenceTo<SinglePositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

    case gpstPair:
    {
        LEReferenceTo<PairPositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

    case gpstCursive:
    {
        LEReferenceTo<CursiveAttachmentSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

    case gpstMarkToBase:
    {
        LEReferenceTo<MarkToBasePositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

     case gpstMarkToLigature:
    {
        LEReferenceTo<MarkToLigaturePositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

    case gpstMarkToMark:
    {
        LEReferenceTo<MarkToMarkPositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(subtable, glyphIterator, fontInstance, success);
        break;
    }

   case gpstContext:
    {
        LEReferenceTo<ContextualPositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(this, glyphIterator, fontInstance, success);
        break;
    }

    case gpstChainedContext:
    {
        LEReferenceTo<ChainingContextualPositioningSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(this, glyphIterator, fontInstance, success);
        break;
    }

    case gpstExtension:
    {
        LEReferenceTo<ExtensionSubtable> subtable(lookupSubtable, success);

        delta = subtable->process(this, lookupType, glyphIterator, fontInstance, success);
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

U_NAMESPACE_END
