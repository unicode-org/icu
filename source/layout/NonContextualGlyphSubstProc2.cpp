/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc2.h"
#include "SimpleArrayProcessor2.h"
#include "SegmentSingleProcessor2.h"
#include "SegmentArrayProcessor2.h"
#include "SingleTableProcessor2.h"
#include "TrimmedArrayProcessor2.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

NonContextualGlyphSubstitutionProcessor2::NonContextualGlyphSubstitutionProcessor2()
{
}

NonContextualGlyphSubstitutionProcessor2::NonContextualGlyphSubstitutionProcessor2(const MorphSubtableHeader2 *morphSubtableHeader)
    : SubtableProcessor2(morphSubtableHeader)
{
}

NonContextualGlyphSubstitutionProcessor2::~NonContextualGlyphSubstitutionProcessor2()
{
}

SubtableProcessor2 *NonContextualGlyphSubstitutionProcessor2::createInstance(const MorphSubtableHeader2 *morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader2 *header = (const NonContextualGlyphSubstitutionHeader2 *) morphSubtableHeader;

    switch (SWAPW(header->table.format))
    {
    case ltfSimpleArray:
        return new SimpleArrayProcessor2(morphSubtableHeader);

    case ltfSegmentSingle:
        return new SegmentSingleProcessor2(morphSubtableHeader);

    case ltfSegmentArray:
        return new SegmentArrayProcessor2(morphSubtableHeader);

    case ltfSingleTable:
        return new SingleTableProcessor2(morphSubtableHeader);

    case ltfTrimmedArray:
        return new TrimmedArrayProcessor2(morphSubtableHeader);

    default:
        return NULL;
    }
}

U_NAMESPACE_END
