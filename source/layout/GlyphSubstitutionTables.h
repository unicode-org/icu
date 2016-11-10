/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __GLYPHSUBSTITUTIONTABLES_H
#define __GLYPHSUBSTITUTIONTABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "GlyphLookupTables.h"

U_NAMESPACE_BEGIN

class  LEGlyphStorage;
class  LEGlyphFilter;
struct GlyphDefinitionTableHeader;

struct GlyphSubstitutionTableHeader : public GlyphLookupTableHeader
{
  le_int32    process(const LEReferenceTo<GlyphSubstitutionTableHeader> &base, 
                      LEGlyphStorage &glyphStorage, 
                        le_bool rightToLeft, 
                        LETag scriptTag, 
                        LETag languageTag,
                        const LEReferenceTo<GlyphDefinitionTableHeader> &glyphDefinitionTableHeader, 
                        const LEGlyphFilter *filter,
                        const FeatureMap *featureMap, 
                        le_int32 featureMapCount, 
                        le_bool featureOrder,
                        LEErrorCode &success) const;
};

enum GlyphSubstitutionSubtableTypes
{
    gsstSingle          = 1,
    gsstMultiple        = 2,
    gsstAlternate       = 3,
    gsstLigature        = 4,
    gsstContext         = 5,
    gsstChainingContext = 6,
    gsstExtension       = 7,
    gsstReverseChaining = 8
};

typedef LookupSubtable GlyphSubstitutionSubtable;

U_NAMESPACE_END
#endif
