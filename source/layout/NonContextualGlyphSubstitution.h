/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __NONCONTEXTUALGLYPHSUBSTITUTION_H
#define __NONCONTEXTUALGLYPHSUBSTITUTION_H

#include "LETypes.h"
#include "LayoutTables.h"
#include "LookupTables.h"
#include "MorphTables.h"

struct NonContextualGlyphSubstitutionHeader : MorphSubtableHeader
{
    LookupTable table;
};

#endif

