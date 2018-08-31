// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 1999-2012, International Business Machines
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

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

class PropsBuilder {
public:
    PropsBuilder();
    virtual ~PropsBuilder();
    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setAlgNamesRange(UChar32 start, UChar32 end,
                                  const char *type, const char *prefix, UErrorCode &errorCode);
    virtual void setProps(const icu::UniProps &props, const icu::UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void build(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeJavaSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);
};

class PNamesBuilder : public PropsBuilder {
public:
    virtual const icu::PropertyNames *getPropertyNames() = 0;
};

PNamesBuilder *createPNamesBuilder(UErrorCode &errorCode);
PropsBuilder *createCorePropsBuilder(UErrorCode &errorCode);
PropsBuilder *createBiDiPropsBuilder(UErrorCode &errorCode);
PropsBuilder *createCasePropsBuilder(UErrorCode &errorCode);
PropsBuilder *createLayoutPropsBuilder(UErrorCode &errorCode);
PropsBuilder *createNamesPropsBuilder(UErrorCode &errorCode);

/* global flags */
extern UBool beVerbose;
extern UBool beQuiet;

#endif
