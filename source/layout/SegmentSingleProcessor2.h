/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __SEGMENTSINGLEPROCESSOR_H
#define __SEGMENTSINGLEPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc2.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class SegmentSingleProcessor2 : public NonContextualGlyphSubstitutionProcessor2
{
public:
    virtual void process(LEGlyphStorage &glyphStorage, LEErrorCode &success);

    SegmentSingleProcessor2(const LEReferenceTo<MorphSubtableHeader2> &morphSubtableHeader, LEErrorCode &success);

    virtual ~SegmentSingleProcessor2();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @deprecated ICU 54. See {@link icu::LayoutEngine}
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @deprecated ICU 54. See {@link icu::LayoutEngine}
     */
    static UClassID getStaticClassID();

private:
    SegmentSingleProcessor2();

protected:
    LEReferenceTo<SegmentSingleLookupTable> segmentSingleLookupTable;

};

U_NAMESPACE_END
#endif

