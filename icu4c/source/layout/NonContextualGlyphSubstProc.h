/*
 * @(#)NonContextualGlyphSubstProc.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __NONCONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H
#define __NONCONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"

U_NAMESPACE_BEGIN

class NonContextualGlyphSubstitutionProcessor : public SubtableProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount) = 0;

    static SubtableProcessor *createInstance(const MorphSubtableHeader *morphSubtableHeader);

protected:
    NonContextualGlyphSubstitutionProcessor();
    NonContextualGlyphSubstitutionProcessor(const MorphSubtableHeader *morphSubtableHeader);

    virtual ~NonContextualGlyphSubstitutionProcessor();

private:
    NonContextualGlyphSubstitutionProcessor(const NonContextualGlyphSubstitutionProcessor &other); // forbid copying of this class
    NonContextualGlyphSubstitutionProcessor &operator=(const NonContextualGlyphSubstitutionProcessor &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
