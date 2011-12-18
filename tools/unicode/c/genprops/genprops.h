/*
*******************************************************************************
*
*   Copyright (C) 1999-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genprops.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999dec13
*   created by: Markus W. Scherer
*/

#ifndef __GENPROPS_H__
#define __GENPROPS_H__

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/uniset.h"
#include "ppucd.h"
#include "unewdata.h"

class PropsWriter {
public:
    virtual ~PropsWriter();
    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const icu::UniProps &props, const icu::UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void finalizeData(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);
};

PropsWriter *createCorePropsWriter(UErrorCode &errorCode);

/* global flags */
U_CFUNC UBool beVerbose;

#endif
