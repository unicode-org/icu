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

#include "unicode/utypes.h"
#include "unicode/uniset.h"
#include "ppucd.h"
#include "propsvec.h"
#include "unewdata.h"

// TODO: remove
#define USE_NEW 1

class PropsWriter {
public:
    virtual ~PropsWriter();
    virtual void setUnicodeVersion(const UVersionInfo version);
    virtual void setProps(const UniProps &props, const UnicodeSet &newValues, UErrorCode &errorCode);
    virtual void finalizeData(UErrorCode &errorCode);
    virtual void writeCSourceFile(const char *path, UErrorCode &errorCode);
    virtual void writeBinaryData(const char *path, UBool withCopyright, UErrorCode &errorCode);
};

PropsWriter *createCorePropsWriter(UErrorCode &errorCode);
PropsWriter *createProps2Writer(UErrorCode &errorCode);

/* character properties */
typedef struct {
    uint32_t code;
    int32_t numericValue; /* see numericType */
    uint32_t denominator; /* 0: no value */
    uint8_t generalCategory, numericType, exponent;
} Props;

/* global flags */
U_CFUNC UBool beVerbose;

/* prototypes */
U_CFUNC void
writeUCDFilename(char *basename, const char *filename, const char *suffix);

U_CFUNC void
generateAdditionalProperties(char *filename, const char *suffix, UErrorCode *pErrorCode);

int32_t
props2FinalizeData(int32_t indexes[], UErrorCode &errorCode);

void
props2AppendToCSourceFile(FILE *f, UErrorCode &errorCode);

void
props2AppendToBinaryFile(UNewDataMemory *pData, UErrorCode &errorCode);

#endif
