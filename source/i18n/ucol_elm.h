/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_elm.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 02/22/2001
*   created by: Vladimir Weinstein
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
*/
#ifndef UCOL_UCAELEMS_H
#define UCOL_UCAELEMS_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "ucol_cnt.h"
#include "ucol_imp.h"

#ifdef UCOL_DEBUG
#include "cmemory.h"
#include <stdio.h>
#endif

U_NAMESPACE_BEGIN

typedef struct {
    uint32_t *CEs;
    int32_t position;
    int32_t size;
} ExpansionTable;

typedef struct {
    UChar prefixChars[128];
    UChar *prefix;
    uint32_t prefixSize;
    UChar uchars[128];
    UChar *cPoints;
    uint32_t cSize;          /* Number of characters in sequence - for contraction */
    uint32_t noOfCEs;        /* Number of collation elements                       */
    uint32_t CEs[128];      /* These are collation elements - there could be more than one - in case of expansion */
    uint32_t mapCE;         /* This is the value element maps in original table   */
    uint32_t sizePrim[128];
    uint32_t sizeSec[128];
    uint32_t sizeTer[128];
    UBool variableTop;
    UBool caseBit;
    UBool isThai;
} UCAElements;

typedef struct {
  uint32_t *endExpansionCE;
  UBool    *isV;
  int       position;
  int       size;
  uint8_t   maxLSize;
  uint8_t   maxVSize;
  uint8_t   maxTSize;
} MaxJamoExpansionTable;

typedef struct {
  uint32_t *endExpansionCE;
  uint8_t  *expansionCESize;
  int      position;
  int      size;
} MaxExpansionTable;

typedef struct {
  /*CompactEIntArray      *mapping; */
  UNewTrie                 *mapping; 
  ExpansionTable        *expansions; 
  struct CntTable       *contractions;
  UCATableHeader        *image;
  UColOptionSet         *options;
  MaxExpansionTable     *maxExpansions;
  MaxJamoExpansionTable *maxJamoExpansions;
  uint8_t               *unsafeCP;
  uint8_t               *contrEndCP;
  const UCollator       *UCA;
  UHashtable      *prefixLookup;
} tempUCATable; 

U_CAPI tempUCATable * U_EXPORT2 uprv_uca_initTempTable(UCATableHeader *image, UColOptionSet *opts, const UCollator *UCA, UColCETags initTag, UColCETags supplementaryInitTag, UErrorCode *status);
U_CAPI tempUCATable * U_EXPORT2 uprv_uca_cloneTempTable(tempUCATable *t, UErrorCode *status);
U_CAPI void U_EXPORT2 uprv_uca_closeTempTable(tempUCATable *t);
U_CAPI uint32_t U_EXPORT2 uprv_uca_addAnElement(tempUCATable *t, UCAElements *element, UErrorCode *status);
U_CAPI UCATableHeader * U_EXPORT2 uprv_uca_assembleTable(tempUCATable *t, UErrorCode *status);
U_CAPI int32_t U_EXPORT2
uprv_uca_canonicalClosure(tempUCATable *t, UErrorCode *status);


#define paddedsize(something) ((something)+((((something)%4)!=0)?(4-(something)%4):0))
#define headersize (paddedsize(sizeof(UCATableHeader))+paddedsize(sizeof(UColOptionSet)))

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_COLLATION */

#endif
