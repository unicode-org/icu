/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __SUBTABLEPROCESSOR_H
#define __SUBTABLEPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"

U_NAMESPACE_BEGIN

class LEGlyphStorage;

class SubtableProcessor : public UMemory {
public:
    virtual void process(LEGlyphStorage &glyphStorage, LEErrorCode &success) = 0;
    virtual ~SubtableProcessor();

protected:
    SubtableProcessor(const LEReferenceTo<MorphSubtableHeader> &morphSubtableHeader, LEErrorCode &success);

    SubtableProcessor();

    le_int16 length;
    SubtableCoverage coverage;
    FeatureFlags subtableFeatures;

    const LEReferenceTo<MorphSubtableHeader> subtableHeader;

private:

    SubtableProcessor(const SubtableProcessor &other); // forbid copying of this class
    SubtableProcessor &operator=(const SubtableProcessor &other); // forbid copying of this class
};

U_NAMESPACE_END
#endif

