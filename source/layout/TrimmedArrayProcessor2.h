/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
 *
 */

#ifndef __TRIMMEDARRAYPROCESSOR2_H
#define __TRIMMEDARRAYPROCESSOR2_H

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

class TrimmedArrayProcessor2 : public NonContextualGlyphSubstitutionProcessor2
{
public:
    virtual void process(LEGlyphStorage &glyphStorage);

    TrimmedArrayProcessor2(const MorphSubtableHeader2 *morphSubtableHeader);

    virtual ~TrimmedArrayProcessor2();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.8
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.8
     */
    static UClassID getStaticClassID();

private:
    TrimmedArrayProcessor2();

protected:
    TTGlyphID firstGlyph;
    TTGlyphID lastGlyph;
    const TrimmedArrayLookupTable *trimmedArrayLookupTable;

};

U_NAMESPACE_END
#endif

