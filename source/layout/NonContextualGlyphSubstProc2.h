/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
 *
 */

#ifndef __NONCONTEXTUALGLYPHSUBSTITUTIONPROCESSOR2_H
#define __NONCONTEXTUALGLYPHSUBSTITUTIONPROCESSOR2_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "NonContextualGlyphSubst.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class NonContextualGlyphSubstitutionProcessor2 : public SubtableProcessor2
{
public:
    virtual void process(LEGlyphStorage &glyphStorage) = 0;

    static SubtableProcessor2 *createInstance(const MorphSubtableHeader2 *morphSubtableHeader);

protected:
    NonContextualGlyphSubstitutionProcessor2();
    NonContextualGlyphSubstitutionProcessor2(const MorphSubtableHeader2 *morphSubtableHeader);

    virtual ~NonContextualGlyphSubstitutionProcessor2();

private:
    NonContextualGlyphSubstitutionProcessor2(const NonContextualGlyphSubstitutionProcessor2 &other); // forbid copying of this class
    NonContextualGlyphSubstitutionProcessor2 &operator=(const NonContextualGlyphSubstitutionProcessor2 &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif
