/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genuca.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created at the end of XX century
*   created by: Vladimir Weinstein
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
*/

#ifndef UCADATA_H
#define UCADATA_H

#include "ucol_elm.h"
#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/unicode.h"
#include "ucol_imp.h"
#include "ucmp32.h"
#include "compitr.h"
#include "uhash.h"
#include "umemstrm.h"
#include "unewdata.h"

/* This is the version of FractionalUCA.txt tailoring rules*/
/* Regular tailorings have versions from 1-199 and UCA */
/* has version numbers from 200 up */
#define UCA_TAILORING_RULES_VERSION 201

/* UDataInfo for UCA mapping table */
static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x55, 0x43, 0x6f, 0x6c},     /* dataFormat="UCol"            */
    {1, 0, 0, 0},                 /* formatVersion                */
    {3, 0, 0, 0}                  /* dataVersion = Unicode Version*/
};

/* UDataInfo for inverse UCA table */
static const UDataInfo invDataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x49, 0x6E, 0x76, 0x43},     /* dataFormat="InvC"            */
    {1, 0, 0, 0},                 /* formatVersion                */
    {3, 0, 0, 0}                  /* dataVersion = Unicode Version*/
};


void deleteElement(void *element);
int32_t readElement(char **from, char *to, char separator, UErrorCode *status);
uint32_t getSingleCEValue(char *primary, char *secondary, char *tertiary, UBool caseBit, UErrorCode *status);
void printOutTable(UCATableHeader *myData, UErrorCode *status);
UCAElements *readAnElement(FILE *data, UErrorCode *status);


#endif
