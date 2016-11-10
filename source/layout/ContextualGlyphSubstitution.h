/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __CONTEXTUALGLYPHSUBSTITUTION_H
#define __CONTEXTUALGLYPHSUBSTITUTION_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LayoutTables.h"
#include "MorphStateTables.h"
#include "MorphTables.h"
#include "StateTables.h"

U_NAMESPACE_BEGIN

struct ContextualGlyphSubstitutionHeader : MorphStateTableHeader
{
    ByteOffset  substitutionTableOffset;
};

struct ContextualGlyphHeader2 : MorphStateTableHeader2
{
    le_uint32  perGlyphTableOffset; // no more substitution tables
};

enum ContextualGlyphSubstitutionFlags
{
    cgsSetMark      = 0x8000,
    cgsDontAdvance  = 0x4000,
    cgsReserved     = 0x3FFF
};

struct ContextualGlyphSubstitutionStateEntry : StateEntry
{
    WordOffset markOffset;
    WordOffset currOffset;
};

struct ContextualGlyphStateEntry2 : StateEntry2
{
    le_uint16 markIndex;
    le_uint16 currIndex;
};

U_NAMESPACE_END
#endif
