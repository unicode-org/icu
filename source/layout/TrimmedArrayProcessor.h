/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#ifndef __TRIMMEDARRAYPROCESSOR_H
#define __TRIMMEDARRAYPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class TrimmedArrayProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphStorage &glyphStorage);

    TrimmedArrayProcessor(const MorphSubtableHeader *morphSubtableHeader);

    virtual ~TrimmedArrayProcessor();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.8
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.8
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:
    TrimmedArrayProcessor();

protected:
    TTGlyphID firstGlyph;
    TTGlyphID lastGlyph;
    const TrimmedArrayLookupTable *trimmedArrayLookupTable;

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END
#endif

