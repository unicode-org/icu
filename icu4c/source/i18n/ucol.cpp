/*
*******************************************************************************
*   Copyright (C) 1996-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucol.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 1996-1999   various members of ICU team maintained C API for collation framework
* 02/16/2001  synwee    Added internal method getPrevSpecialCE
* 03/01/2001  synwee    Added maxexpansion functionality.
* 03/16/2001  weiv      Collation framework is rewritten in C and made UCA compliant
*/

#include "ucol_bld.h"
#include "ucol_imp.h"
#include "ucol_tok.h"
#include "ucol_elm.h"

#include "unicode/uloc.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/unorm.h"
#include "unicode/udata.h"

#include "cpputils.h"
#include "cstring.h"
#include "ucmp32.h"
#include "umutex.h"
#include "uhash.h"

#include <stdio.h>
#include <limits.h>

/* added by synwee for trie manipulation*/
#define STAGE_1_SHIFT_            10
#define STAGE_2_SHIFT_            4
#define STAGE_2_MASK_AFTER_SHIFT_ 0x3F
#define STAGE_3_MASK_             0xF
#define LAST_BYTE_MASK_           0xFF
#define SECOND_LAST_BYTE_SHIFT_   8

#define ZERO_CC_LIMIT_            0xC0

static UCollator* UCA = NULL;

extern "C" UBool checkFCD(const UChar*, int32_t, UErrorCode*);

/* Fixup table a la Markus */
/* see http://www.ibm.com/software/developer/library/utf16.html for further explanation */
static uint8_t utf16fixup[32] = {
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0x20, 0xf8, 0xf8, 0xf8, 0xf8
};

static UBool U_CALLCONV
isAcceptableUCA(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo){
  /* context, type & name are intentionally not used */
    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x55 &&   /* dataFormat="UCol" */
        pInfo->dataFormat[1]==0x43 &&
        pInfo->dataFormat[2]==0x6f &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==3 &&
        pInfo->dataVersion[1]==0 &&
        pInfo->dataVersion[2]==0 &&
        pInfo->dataVersion[3]==0) {
        return TRUE;
    } else {
        return FALSE;
    }
}

/* added for Han implicit CE */
static const uint32_t IMPLICIT_HAN_START_ = 0x3400;
static const uint32_t IMPLICIT_HAN_LIMIT_ = 0xA000;
static const uint32_t IMPLICIT_SUPPLEMENTARY_COUNT_ = 0x100000;
static const uint32_t IMPLICIT_BYTES_TO_AVOID_ = 3;
static const uint32_t IMPLICIT_OTHER_COUNT_ = 256 - IMPLICIT_BYTES_TO_AVOID_;
static const uint32_t IMPLICIT_LAST_COUNT_ = IMPLICIT_OTHER_COUNT_ / 2;
static const uint32_t IMPLICIT_LAST_COUNT2_ =
                       (IMPLICIT_SUPPLEMENTARY_COUNT_ - 1) /
                       (IMPLICIT_OTHER_COUNT_ * IMPLICIT_OTHER_COUNT_) + 1;
static const uint32_t IMPLICIT_HAN_SHIFT_ = IMPLICIT_LAST_COUNT_ *
                              IMPLICIT_OTHER_COUNT_ - IMPLICIT_HAN_START_;
static const uint32_t IMPLICIT_BOUNDARY_ = 2 * IMPLICIT_OTHER_COUNT_ *
                                  IMPLICIT_LAST_COUNT_ + IMPLICIT_HAN_START_;
static const uint32_t IMPLICIT_LAST2_MULTIPLIER_ = IMPLICIT_OTHER_COUNT_ /
                                                        IMPLICIT_LAST_COUNT2_;

inline void  IInit_collIterate(const UCollator *collator, const UChar *sourceString,
                              int32_t sourceLen, collIterate *s) {
    (s)->string = (s)->pos = (UChar *)(sourceString);
    (s)->origFlags = 0;
    (s)->flags = 0;
    if (sourceLen >= 0) {
        s->flags |= UCOL_ITER_HASLEN;
        (s)->endp = (UChar *)sourceString+sourceLen;
    }
    else {
        /* change to enable easier checking for end of string for fcdpositon */
        (s)->endp = NULL;
    }
    (s)->CEpos = (s)->toReturn = (s)->CEs;
    (s)->writableBuffer = (s)->stackWritableBuffer;
    (s)->writableBufSize = UCOL_WRITABLE_BUFFER_SIZE;
    (s)->coll = (collator);
    (s)->fcdPosition = 0;
    if(collator->normalizationMode == UCOL_ON) {
        (s)->flags |= UCOL_ITER_NORM; }
}

U_CAPI void init_collIterate(const UCollator *collator, const UChar *sourceString,
                             int32_t sourceLen, collIterate *s){
    /* Out-of-line version for use from other files. */
    IInit_collIterate(collator, sourceString, sourceLen, s);
}

/**
* Backup the state of the collIterate struct data
* @param data collIterate to backup
* @param backup storage
*/
inline void backupState(const collIterate *data, collIterateState *backup)
{
    backup->fcdPosition = data->fcdPosition;
    backup->flags       = data->flags;
    backup->origFlags   = data->origFlags;
    backup->pos         = data->pos;
    backup->bufferaddress = data->writableBuffer;
    backup->buffersize    = data->writableBufSize;
}

/**
* Loads the state into the collIterate struct data
* @param data collIterate to backup
* @param backup storage
* @param forwards boolean to indicate if forwards iteration is used,
*        false indicates backwards iteration
*/
inline void loadState(collIterate *data, const collIterateState *backup,
                      UBool        forwards)
{
    data->flags       = backup->flags;
    data->origFlags   = backup->origFlags;
    data->pos         = backup->pos;
    if ((data->flags & UCOL_ITER_INNORMBUF) &&
        data->writableBuffer != backup->bufferaddress) {
        /*
        this is when a new buffer has been reallocated and we'll have to
        calculate the new position.
        note the new buffer has to contain the contents of the old buffer.
        */
        if (forwards) {
            data->pos = data->writableBuffer +
                                         (data->pos - backup->bufferaddress);
        }
        else {
            /* backwards direction */
            uint32_t temp = backup->buffersize -
                                  (data->pos - backup->bufferaddress);
            data->pos = data->writableBuffer + (data->writableBufSize - temp);
        }
    }
    if ((data->flags & UCOL_ITER_INNORMBUF) == 0) {
        /*
        this is alittle tricky.
        if we are initially not in the normalization buffer, even if we
        normalize in the later stage, the data in the buffer will be
        ignored, since we skip back up to the data string.
        however if we are already in the normalization buffer, any
        further normalization will pull data into the normalization
        buffer and modify the fcdPosition.
        since we are keeping the data in the buffer for use, the
        fcdPosition can not be reverted back.
        arrgghh....
        */
        data->fcdPosition = backup->fcdPosition;
    }
}

/**
* Checks and free writable buffer if it is not the original stack buffer
* in collIterate. This function does not reassign the writable buffer.
* @param data collIterate struct to determine and free the writable buffer
*/
inline void freeHeapWritableBuffer(collIterate *data)
{
    if (data->writableBuffer != data->stackWritableBuffer) {
        uprv_free(data->writableBuffer);
    }
}

/****************************************************************************/
/* Following are the open/close functions                                   */
/*                                                                          */
/****************************************************************************/
U_CAPI UCollator*
ucol_open(    const    char         *loc,
        UErrorCode      *status)
{

  ucol_initUCA(status);

  /* New version */
  if(U_FAILURE(*status)) return 0;

  UCollator *result = NULL;
  UResourceBundle *b = ures_open(NULL, loc, status);
  /* first take on tailoring version: */
  /* get CollationElements -> Version */
  UResourceBundle *binary = ures_getByKey(b, "%%CollationNew", NULL, status);

  if(*status == U_MISSING_RESOURCE_ERROR) { /* if we don't find tailoring, we'll fallback to UCA */
    *status = U_USING_DEFAULT_ERROR;
    result = ucol_initCollator(UCA->image, result, status);
    /*result = UCA;*/
    result->hasRealData = FALSE;
  } else if(U_SUCCESS(*status)) { /* otherwise, we'll pick a collation data that exists */
    int32_t len = 0;
    const uint8_t *inData = ures_getBinary(binary, &len, status);
    if(U_FAILURE(*status)){
      goto clean;
    }
    if((uint32_t)len > (paddedsize(sizeof(UCATableHeader)) + paddedsize(sizeof(UColOptionSet)))) {
      result = ucol_initCollator((const UCATableHeader *)inData, result, status);
      if(U_FAILURE(*status)){
        goto clean;
      }
      result->hasRealData = TRUE;
    } else {
      result = ucol_initCollator(UCA->image, result, status);
      ucol_setOptionsFromHeader(result, (UColOptionSet *)(inData+((const UCATableHeader *)inData)->options), status);
      if(U_FAILURE(*status)){
        goto clean;
      }
      result->hasRealData = FALSE;
    }
  } else { /* There is another error, and we're just gonna clean up */
clean:
    ures_close(b);
    ures_close(binary);
    return NULL;
  }

  result->rb = b;
  ures_close(binary);

  return result;
}

U_CAPI UCollator * U_EXPORT2
ucol_openVersion(const char *loc,
                 UVersionInfo version,
                 UErrorCode *status) {
  UCollator *collator;
  UVersionInfo info;

  collator=ucol_open(loc, status);
  if(U_SUCCESS(*status)) {
    ucol_getVersion(collator, info);
    if(0!=uprv_memcmp(version, info, sizeof(UVersionInfo))) {
      ucol_close(collator);
      *status=U_MISSING_RESOURCE_ERROR;
      return NULL;
    }
  }
  return collator;
}

U_CAPI void
ucol_close(UCollator *coll)
{
  /* Here, it would be advisable to close: */
  /* - UData for UCA (unless we stuff it in the root resb */
  /* Again, do we need additional housekeeping... HMMM! */
  if(coll->freeOnClose == FALSE){
    return; /* for safeClone, if freeOnClose is FALSE,
            don't free the other instance data */
  }
  if(coll->freeOptionsOnClose != FALSE) {
    if(coll->options != NULL) {
      uprv_free(coll->options);
    }
  }
  if(coll->mapping != NULL) {
      ucmp32_close(coll->mapping);
  }
  if(coll->rules != NULL && coll->freeRulesOnClose) {
    uprv_free((UChar *)coll->rules);
  }
  if(coll->rb != NULL) { /* pointing to read-only memory */
    ures_close(coll->rb);
  } else if(coll->hasRealData == TRUE) {
    uprv_free((UCATableHeader *)coll->image);
  }
  uprv_free(coll);
}

U_CAPI UCollator*
ucol_openRules(    const    UChar                  *rules,
        int32_t                 rulesLength,
        UNormalizationMode      mode,
        UCollationStrength      strength,
        UErrorCode              *status)
{
  uint32_t listLen = 0;
  UColTokenParser src;
  UColAttributeValue norm;

  switch(mode) {
  case UNORM_NONE:
    norm = UCOL_OFF;
    break;
  case UNORM_NFD:
    norm = UCOL_ON;
    break;
  case UCOL_DEFAULT_NORMALIZATION:
  case UCOL_DEFAULT:
    norm = UCOL_DEFAULT;
    break;
  default:
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  ucol_initUCA(status);

  if(U_FAILURE(*status)) return 0;

  /*src.source = rules;*/
  src.source = (UChar *)uprv_malloc((rulesLength+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
  uprv_memcpy(src.source, rules, rulesLength*sizeof(UChar));
  src.current = src.source;
  src.end = src.source+rulesLength;
  src.sourceCurrent = src.source;
  src.extraCurrent = src.end;
  src.extraEnd = src.end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
  src.UCA = UCA;
  src.invUCA = ucol_initInverseUCA(status);
  src.resultLen = 0;
  src.lh = 0;
  src.varTop = NULL;

  src.opts = (UColOptionSet *)uprv_malloc(sizeof(UColOptionSet));

  uprv_memcpy(src.opts, UCA->options, sizeof(UColOptionSet));

  listLen = ucol_tok_assembleTokenList(&src, status);
  if(U_FAILURE(*status)) {
    /* if status is U_ILLEGAL_ARGUMENT_ERROR, src->current points at the offending option */
    /* if status is U_INVALID_FORMAT_ERROR, src->current points after the problematic part of the rules */
    /* so something might be done here... or on lower level */
#ifdef UCOL_DEBUG
    if(*status == U_ILLEGAL_ARGUMENT_ERROR) {
      fprintf(stderr, "bad option starting at offset %i\n", src.current-src.source);
    } else {
      fprintf(stderr, "invalid rule just before offset %i\n", src.current-src.source);
    }
#endif
    uprv_free(src.opts);
    ucol_tok_closeTokenList(&src);
    return NULL;
  }
  UCollator *result = NULL;
  UCATableHeader *table = NULL;

  if(src.resultLen > 0) { /* we have a set of rules, let's make something of it */
    table = ucol_assembleTailoringTable(&src, status);
    if(U_SUCCESS(*status)) {
      result = ucol_initCollator(table,0,status);
      result->hasRealData = TRUE;
    }
  } else { /* no rules, but no error either */
    /* must be only options */
    result = ucol_initCollator(UCA->image,0,status);
    ucol_setOptionsFromHeader(result, src.opts, status);
    result->freeOptionsOnClose = TRUE;
    result->hasRealData = FALSE;
  }


  if(U_SUCCESS(*status)) {
    result->dataInfo.dataVersion[0] = UCOL_BUILDER_VERSION;
    result->rules = (UChar *)uprv_malloc((u_strlen(rules)+1)*sizeof(UChar));
    u_strcpy((UChar *)result->rules, rules);
    result->freeRulesOnClose = TRUE;
    result->rb = 0;
    ucol_setAttribute(result, UCOL_STRENGTH, strength, status);
    ucol_setAttribute(result, UCOL_NORMALIZATION_MODE, norm, status);
  } else {
    uprv_free(src.opts);
    if(table != NULL) {
      uprv_free(table);
    }
    if(result != NULL) {
      ucol_close(result);
    }
    result = NULL;
  }
  ucol_tok_closeTokenList(&src);

  return result;
}

/* This one is currently used by genrb & tests. After constructing from rules (tailoring),*/
/* you should be able to get the binary chunk to write out...  Doesn't look very full now */
U_CAPI uint8_t *
ucol_cloneRuleData(UCollator *coll, int32_t *length, UErrorCode *status)
{
  uint8_t *result = NULL;
  if(coll->hasRealData == TRUE) {
    *length = coll->image->size;
    result = (uint8_t *)uprv_malloc(*length);
    uprv_memcpy(result, coll->image, *length);
  } else {
    *length = paddedsize(sizeof(UCATableHeader))+paddedsize(sizeof(UColOptionSet));
    result = (uint8_t *)uprv_malloc(*length);
    UCATableHeader *head = (UCATableHeader *)result;
    uprv_memcpy(result, UCA->image, sizeof(UCATableHeader));
    uprv_memcpy(result+paddedsize(sizeof(UCATableHeader)), coll->options, sizeof(UColOptionSet));
  }
  return result;
}

void ucol_setOptionsFromHeader(UCollator* result, UColOptionSet * opts, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
    result->caseFirst = opts->caseFirst;
    result->caseLevel = opts->caseLevel;
    result->frenchCollation = opts->frenchCollation;
    result->normalizationMode = opts->normalizationMode;
    result->strength = opts->strength;
    result->variableTopValue = opts->variableTopValue;
    result->alternateHandling = opts->alternateHandling;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;

    ucol_updateInternalState(result);

    result->options = opts;
}

void ucol_putOptionsToHeader(UCollator* result, UColOptionSet * opts, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
    opts->caseFirst = result->caseFirst;
    opts->caseLevel = result->caseLevel;
    opts->frenchCollation = result->frenchCollation;
    opts->normalizationMode = result->normalizationMode;
    opts->strength = result->strength;
    opts->variableTopValue = result->variableTopValue;
    opts->alternateHandling = result->alternateHandling;
}


U_CAPI const uint16_t * getFCHK_STAGE_1_(UErrorCode *);
U_CAPI const uint16_t * getFCHK_STAGE_2_(UErrorCode *);
U_CAPI const uint16_t * getFCHK_STAGE_3_(UErrorCode *);

static const uint16_t *FCD_STAGE_1_;
static const uint16_t *FCD_STAGE_2_;
static const uint16_t *FCD_STAGE_3_;


inline UBool ucol_unsafeCP(UChar c, const UCollator *coll) {
    if (c < coll->minUnsafeCP) {
        return FALSE;
    }

    int32_t  hash = c;
    uint8_t  htbyte;

    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
        if (hash >= 0xd800 && hash <= 0xf8ff) {
            /*  Part of a surrogate, or in private use area.            */
            /*   These are always considered unsafe.                    */
            return TRUE;
        }
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htbyte = coll->unsafeCP[hash>>3];
    return (((htbyte >> (hash & 7)) & 1) == 1);
}

inline UBool ucol_contractionEndCP(UChar c, const UCollator *coll) {
    if (c < coll->minContrEndCP) {
        return FALSE;
    }

    int32_t  hash = c;
    uint8_t  htbyte;

    if (hash >= UCOL_UNSAFECP_TABLE_SIZE*8) {
        hash = (hash & UCOL_UNSAFECP_TABLE_MASK) + 256;
    }
    htbyte = coll->contrEndCP[hash>>3];
    return (((htbyte >> (hash & 7)) & 1) == 1);
}



UCollator* ucol_initCollator(const UCATableHeader *image, UCollator *fillIn, UErrorCode *status) {
    UChar c;
    UCollator *result = fillIn;
    if(U_FAILURE(*status) || image == NULL) {
        return NULL;
    }

    if(result == NULL) {
        result = (UCollator *)uprv_malloc(sizeof(UCollator));
        if(result == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return result;
        }
        result->freeOnClose = TRUE;
    } else {
        result->freeOnClose = FALSE;
    }

    result->image = image;
    const uint8_t *mapping = (uint8_t*)result->image+result->image->mappingPosition;
    CompactIntArray *newUCAmapping = ucmp32_openFromData(&mapping, status);
    if(U_SUCCESS(*status)) {
        result->mapping = newUCAmapping;
    } else {
        if(result->freeOnClose == TRUE) {
            uprv_free(result);
            result = NULL;
        }
        return result;
    }

    result->latinOneMapping = (uint32_t*)((uint8_t*)result->image+result->image->latinOneMapping);
    result->contractionCEs = (uint32_t*)((uint8_t*)result->image+result->image->contractionCEs);
    result->contractionIndex = (UChar*)((uint8_t*)result->image+result->image->contractionIndex);
    result->expansion = (uint32_t*)((uint8_t*)result->image+result->image->expansion);

    result->options = (UColOptionSet*)((uint8_t*)result->image+result->image->options);
    result->freeOptionsOnClose = FALSE;

    /* set attributes */
    result->caseFirst = result->options->caseFirst;
    result->caseLevel = result->options->caseLevel;
    result->frenchCollation = result->options->frenchCollation;
    result->normalizationMode = result->options->normalizationMode;
    result->strength = result->options->strength;
    result->variableTopValue = result->options->variableTopValue;
    result->alternateHandling = result->options->alternateHandling;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;
    result->alternateHandlingisDefault = TRUE;

    result->scriptOrder = NULL;

    result->zero = 0;
    result->rules = NULL;
    /* get the version info form UCATableHeader and populate the Collator struct*/
    result->dataInfo.dataVersion[0] = result->image->version[0]; /* UCA Builder version*/
    result->dataInfo.dataVersion[1] = result->image->version[1]; /* UCA Tailoring rules version*/

    result->unsafeCP = (uint8_t *)result->image + result->image->unsafeCP;
    result->minUnsafeCP = 0;
    for (c=0; c<0x300; c++) {  // Find the smallest unsafe char.
        if (ucol_unsafeCP(c, result)) break;
    }
    result->minUnsafeCP = c;

    result->contrEndCP = (uint8_t *)result->image + result->image->contrEndCP;
    result->minContrEndCP = 0;
    for (c=0; c<0x300; c++) {  // Find the Contraction-ending char.
        if (ucol_contractionEndCP(c, result)) break;
    }
    result->minContrEndCP = c;

    /* max expansion tables */
    result->endExpansionCE = (uint32_t*)((uint8_t*)result->image +
                                         result->image->endExpansionCE);
    result->lastEndExpansionCE = result->endExpansionCE +
                                 result->image->endExpansionCECount - 1;
    result->expansionCESize = (uint8_t*)result->image +
                                               result->image->expansionCESize;

    if (FCD_STAGE_1_ == NULL) {
        FCD_STAGE_1_ = getFCHK_STAGE_1_(status);
    }
    if (FCD_STAGE_2_ == NULL) {
        FCD_STAGE_2_ = getFCHK_STAGE_2_(status);
    }
    if (FCD_STAGE_3_ == NULL) {
        FCD_STAGE_3_ = getFCHK_STAGE_3_(status);
    }

    result->errorCode = *status;
    ucol_updateInternalState(result);

    return result;
}

void ucol_initUCA(UErrorCode *status) {
  if(U_FAILURE(*status)) return;

  if(UCA == NULL) {
    UCollator *newUCA = (UCollator *)uprv_malloc(sizeof(UCollator));
    UDataMemory *result = udata_openChoice(NULL, UCA_DATA_TYPE, UCA_DATA_NAME, isAcceptableUCA, NULL, status);

    if(U_FAILURE(*status)) {
        udata_close(result);
        uprv_free(newUCA);
    }

    if(result != NULL) { /* It looks like sometimes we can fail to find the data file */
      newUCA = ucol_initCollator((const UCATableHeader *)udata_getMemory(result), newUCA, status);
      if(U_SUCCESS(*status)){
        newUCA->rb = NULL;
        umtx_lock(NULL);
        if(UCA == NULL) {
            UCA = newUCA;
            newUCA = NULL;
        }
        umtx_unlock(NULL);

        if(newUCA != NULL) {
            udata_close(result);
            uprv_free(newUCA);
        }
      }else{
        udata_close(result);
        uprv_free(newUCA);
        UCA= NULL;
      }

    }

  }
}

/*    collIterNormalize     Incremental Normalization happens here.                       */
/*                          pick up the range of chars identifed by FCD,                  */
/*                          normalize it into the collIterate's writable buffer,          */
/*                          switch the collIterate's state to use the writable buffer.    */
/*                                                                                        */
void collIterNormalize(collIterate *collationSource)
{
    UErrorCode  status = U_ZERO_ERROR;
    UChar      *srcP = collationSource->pos - 1;      /*  Start of chars to normalize    */
    UChar      *endP = collationSource->fcdPosition;  /* End of region to normalize+1    */
    uint32_t    normLen;

    normLen = unorm_normalize(srcP, endP-srcP, UNORM_NFD, 0, collationSource->writableBuffer,
                              collationSource->writableBufSize, &status);
    if (normLen == collationSource->writableBufSize) {
        UChar *temp = (UChar *)uprv_malloc((normLen+1)*sizeof(UChar));
        uprv_memcpy(temp, collationSource->writableBuffer, normLen * sizeof(UChar));
        temp[normLen] = 0;
        freeHeapWritableBuffer(collationSource);
        collationSource->writableBuffer = temp;
    }
    if (U_FAILURE(status)) { /* This would be buffer overflow */
        if (status == U_BUFFER_OVERFLOW_ERROR) {
            freeHeapWritableBuffer(collationSource);
            collationSource->writableBuffer = (UChar *)uprv_malloc((normLen+1)*sizeof(UChar));
            /* to enable null termination */
            collationSource->writableBufSize = normLen + 1;
            status = U_ZERO_ERROR;
            unorm_normalize(srcP, endP-srcP, UNORM_NFD, 0, collationSource->writableBuffer,
                            collationSource->writableBufSize, &status);
        }
        else {
            return;
        }
    }

    collationSource->pos        = collationSource->writableBuffer;
    collationSource->origFlags  = collationSource->flags;
    collationSource->flags     |= UCOL_ITER_INNORMBUF;
    collationSource->flags     &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);
}





/* Incremental FCD check and normalize                                                    */
/*   Called from getNextCE when normalization state is suspect.                           */
/*   When entering, the state is known to be this:                                        */
/*      o   We are working in the main buffer of the collIterate, not the side            */
/*          writable buffer.  When in the side buffer, normalization mode is always off,  */
/*          so we won't get here.                                                         */
/*      o   The leading combining class from the current character is 0 or                */
/*          the trailing combining class of the previous char was zero.                   */
/*          True because the previous call to this function will have always exited       */
/*          that way, and we get called for every char where cc might be non-zero.        */
inline UBool collIterFCD(collIterate *collationSource) {
    UChar32     codepoint;
    UChar       *srcP;
    int         length;
    int         count = 0;
    uint8_t     leadingCC;
    uint8_t     prevTrailingCC = 0;
    uint16_t    fcd;
    UBool       needNormalize = FALSE;

    srcP = collationSource->pos-1;

    // If the source string is null terminated, use a fake too-long string length
    //    (needed for UTF_NEXT_CHAR).  null will stop everything OK.)
    length = (collationSource->flags & UCOL_ITER_HASLEN) ? collationSource->endp - srcP : INT_MAX;

    // Get the trailing combining class of the current character.  If it's zero,
    //   we are OK.
    UTF_NEXT_CHAR(srcP, count, length, codepoint);
    /* trie access */
    fcd = FCD_STAGE_3_[
        FCD_STAGE_2_[FCD_STAGE_1_[codepoint >> STAGE_1_SHIFT_] +
        ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
        (codepoint & STAGE_3_MASK_)];
    prevTrailingCC = (uint8_t)(fcd & LAST_BYTE_MASK_);

    if (prevTrailingCC != 0) {
        // The current char has a non-zero trailing CC.  Scan forward until we find
        //   a char with a leading cc of zero.
        for (;;)
        {
            if (count >= length) {
                break;
            }
            UTF_NEXT_CHAR(srcP, count, length, codepoint);

            /* trie access */
            fcd = FCD_STAGE_3_[
                FCD_STAGE_2_[FCD_STAGE_1_[codepoint >> STAGE_1_SHIFT_] +
                ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
                (codepoint & STAGE_3_MASK_)];
            leadingCC = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
            if (leadingCC == 0) {
                count --;
                break;
            }

            if (leadingCC < prevTrailingCC) {
                needNormalize = TRUE;
            }

            prevTrailingCC = (uint8_t)(fcd & LAST_BYTE_MASK_);
        }
    }

    collationSource->fcdPosition = srcP + count;

    // if (codepoint == 0 && (collationSource->flags & UCOL_ITER_HASLEN)==0) {
        /*
        We checked the string's trailing null, which would advance
        fcdPosition past the null. back it up to point to the null.
        */
        /*collationSource->fcdPosition--;
    }*/

    return needNormalize;
}

/****************************************************************************/
/* Following are the CE retrieval functions                                 */
/*                                                                          */
/****************************************************************************/

/* there should be a macro version of this function in the header file */
/* This is the first function that tries to fetch a collation element  */
/* If it's not succesfull or it encounters a more difficult situation  */
/* some more sofisticated and slower functions are invoked             */
inline uint32_t ucol_IGetNextCE(const UCollator *coll, collIterate *collationSource, UErrorCode *status) {
    uint32_t order;
    if (collationSource->CEpos > collationSource->toReturn) {       /* Are there any CEs from previous expansions? */
      order = *(collationSource->toReturn++);                         /* if so, return them */
      if(collationSource->CEpos == collationSource->toReturn) {
        collationSource->CEpos = collationSource->toReturn = collationSource->CEs;
      }
      return order;
    }

    UChar ch;

    for (;;)                           /* Loop handles case when incremental normalize switches   */
    {                                  /*   to or from the side buffer / original string, and we  */
                                       /*   need to start again to get the next character.        */

        if ((collationSource->flags & (UCOL_ITER_HASLEN | UCOL_ITER_INNORMBUF | UCOL_ITER_NORM )) == 0)
        {
            // The source string is null terminated and we're not working from the side buffer,
            //   and we're not normalizing.  This is the fast path.
            //   (We can be in the side buffer for Thai pre-vowel reordering even when not normalizing.)
            ch = *collationSource->pos++;
            if (ch != 0) {
                break;
            }
            else {
                return UCOL_NO_MORE_CES;
            }
        }

        if (collationSource->flags & UCOL_ITER_HASLEN) {
            // Normal path for strings when length is specified.
            //   (We can't be in side buffer because it is always null terminated.)
            if (collationSource->pos >= collationSource->endp) {
                // Ran off of the end of the main source string.  We're done.
                return UCOL_NO_MORE_CES;
            }
            ch = *collationSource->pos++;
        }
        else
        {
            // Null terminated string.
            ch = *collationSource->pos++;
            if (ch == 0) {
                // Ran off end of buffer.
                if ((collationSource->flags & UCOL_ITER_INNORMBUF) == 0) {
                    // Ran off end of main string.
                    return UCOL_NO_MORE_CES;
                }
                else
                {
                    // Ran off the end of the normalize side buffer.  Revert to the main string and
                    //   loop back to top to try again to get a character.
                    collationSource->pos   = collationSource->fcdPosition;
                    collationSource->flags = collationSource->origFlags;
                    continue;
                }
            }
        }

        // We've got a character.  See if there's any fcd and/or normalization stuff to do.
        //    Note that UCOL_ITER_NORM flag is always zero when we are in the side buffer.
        if ((collationSource->flags & UCOL_ITER_NORM) == 0) {
            break;
        }

        if (collationSource->fcdPosition >= collationSource->pos) {
            // An earlier FCD check has already covered the current character.
            // We can go ahead and process this char.
            break;
        }

        if (ch < ZERO_CC_LIMIT_ ) {
            // Fast fcd safe path.  Trailing combining class == 0.  This char is OK.
            break;
        }

        if (ch < NFC_ZERO_CC_BLOCK_LIMIT_) {
            // We need to peek at the next character in order to tell if we are FCD
            if ((collationSource->flags & UCOL_ITER_HASLEN) && collationSource->pos >= collationSource->endp) {
                // We are at the last char of source string.
                //  It is always OK for FCD check.
                break;
            }

            // Not at last char of source string (or we'll check against terminating null).  Do the FCD fast test
            if (*collationSource->pos < NFC_ZERO_CC_BLOCK_LIMIT_) {
                break;
            }
        }

        // Need a more complete FCD check and possible normalization.
        if (collIterFCD(collationSource)) {
            collIterNormalize(collationSource);
        }
        if ((collationSource->flags & UCOL_ITER_INNORMBUF) == 0) {
            //  No normalization was needed.  Go ahead and process the char we already had.
            break;
        }

        // Some normalization happened.  Next loop iteration will pick up a char
        //   from the normalization buffer.

    }   // end for (;;)


      if (ch <= 0xFF) {
          /*  For latin-1 characters we never need to fall back to the UCA table        */
          /*    because all of the UCA data is replicated in the latinOneMapping array  */
          order = coll->latinOneMapping[ch];
          if (order > UCOL_NOT_FOUND) {
              order = getSpecialCE(coll, order, collationSource, status);
          }
      }
      else
      {
          order = ucmp32_get(coll->mapping, ch);                             /* we'll go for slightly slower trie */
          if(order > UCOL_NOT_FOUND) {                                       /* if a CE is special                */
              order = getSpecialCE(coll, order, collationSource, status);    /* and try to get the special CE     */
          }
          if(order == UCOL_NOT_FOUND) {   /* We couldn't find a good CE in the tailoring */
              order = ucol_getNextUCA(ch, collationSource, status);
          }
      }
    return order; /* return the CE */
}

/* ucol_getNextCE, out-of-line version for use from other files.   */
U_CAPI uint32_t ucol_getNextCE(const UCollator *coll, collIterate *collationSource, UErrorCode *status) {
    return ucol_IGetNextCE(coll, collationSource, status);
    }


/**
* Incremental previous normalization happens here. Pick up the range of chars
* identifed by FCD, normalize it into the collIterate's writable buffer,
* switch the collIterate's state to use the writable buffer.
* @param data collation iterator data
*/
void collPrevIterNormalize(collIterate *data)
{
    UErrorCode status  = U_ZERO_ERROR;
    UChar      *pEnd   = data->pos;         /* End normalize + 1 */
    UChar      *pStart;
    uint32_t    normLen;
    UChar      *pStartNorm;

    /* Start normalize */
    if (data->fcdPosition == NULL) {
        pStart = data->string;
    }
    else {
        pStart = data->fcdPosition + 1;
    }

    normLen = unorm_normalize(pStart, (pEnd - pStart) + 1, UNORM_NFD, 0,
                              data->writableBuffer, 0, &status);

    if (data->writableBufSize <= normLen) {
            freeHeapWritableBuffer(data);
            data->writableBuffer = (UChar *)uprv_malloc((normLen + 1) *
                                                        sizeof(UChar));
            /* to handle the zero termination */
            data->writableBufSize = normLen + 1;
    }
            status = U_ZERO_ERROR;
    /*
    this puts the null termination infront of the normalized string instead
    of the end
    */
    pStartNorm = data->writableBuffer + (data->writableBufSize - normLen);
    *(pStartNorm - 1) = 0;
    unorm_normalize(pStart, (pEnd - pStart) + 1, UNORM_NFD, 0, pStartNorm,
                    normLen, &status);

    data->pos        = data->writableBuffer + data->writableBufSize;
    data->origFlags  = data->flags;
    data->flags     |= UCOL_ITER_INNORMBUF;
    data->flags     &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);
}


/**
* Incremental FCD check for previous iteration and normalize. Called from
* getPrevCE when normalization state is suspect.
* When entering, the state is known to be this:
* o  We are working in the main buffer of the collIterate, not the side
*    writable buffer. When in the side buffer, normalization mode is always
*    off, so we won't get here.
* o  The leading combining class from the current character is 0 or the
*    trailing combining class of the previous char was zero.
*    True because the previous call to this function will have always exited
*    that way, and we get called for every char where cc might be non-zero.
* @param data collation iterate struct
* @return normalization status, TRUE for normalization to be done, FALSE
*         otherwise
*/
inline UBool collPrevIterFCD(collIterate *data)
{
    UChar32     codepoint;
    uint8_t     leadingCC;
    uint8_t     trailingCC = 0;
    uint16_t    fcd;
    UBool       result = FALSE;
    int         length;

    length = (data->pos + 1) - data->string;

    /* Get the trailing combining class of the current character. */
    UTF_PREV_CHAR(data->string, 0, length, codepoint);

    /* trie access */
    fcd = FCD_STAGE_3_[
        FCD_STAGE_2_[FCD_STAGE_1_[codepoint >> STAGE_1_SHIFT_] +
        ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
        (codepoint & STAGE_3_MASK_)];

    leadingCC = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);

    if (leadingCC != 0) {
        /*
        The current char has a non-zero leading combining class.
        Scan backward until we find a char with a trailing cc of zero.
        */
        for (;;)
        {
            if (length <= 0) {
                length = -1;
                break;
            }

            UTF_PREV_CHAR(data->string, 0, length, codepoint);

            /* trie access */
            fcd = FCD_STAGE_3_[
                FCD_STAGE_2_[FCD_STAGE_1_[codepoint >> STAGE_1_SHIFT_] +
                ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
                (codepoint & STAGE_3_MASK_)];

            trailingCC = (uint8_t)(fcd & LAST_BYTE_MASK_);

            if (trailingCC == 0) {
                break;
            }

            if (leadingCC < trailingCC) {
                result = TRUE;
            }

            leadingCC = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
        }
    }

    if (length < 0) {
        data->fcdPosition = NULL;
    }
    else {
        data->fcdPosition = data->string + length;
    }

    return result;
}

/**
* Inline function that gets a simple CE.
* So what it does is that it will first check the expansion buffer. If the
* expansion buffer is not empty, ie the end pointer to the expansion buffer
* is different from the string pointer, we return the collation element at the
* return pointer and decrement it.
* For more complicated CEs it resorts to getComplicatedCE.
* @param coll collator data
* @param data collation iterator struct
* @param status error status
*/
inline uint32_t ucol_IGetPrevCE(const UCollator *coll, collIterate *data,
                               UErrorCode *status)
{
    uint32_t result = UCOL_NULLORDER;
    if (data->CEpos > data->CEs) {
        data->toReturn --;
        result = *(data->toReturn);
        if (data->CEs == data->toReturn) {
            data->CEpos = data->toReturn = data->CEs;
        }
    }
    else {
        UChar ch;
        /*
        Loop handles case when incremental normalize switches to or from the
        side buffer / original string, and we need to start again to get the
        next character.
        */

        for (;;) {
            if ((data->flags & UCOL_ITER_INNORMBUF) == 0) {
                /*
                Normal path for strings when length is specified.
                Not in side buffer because it is always null terminated.
                */
                if (data->pos <= data->string) {
                    /* End of the main source string */
                    return UCOL_NO_MORE_CES;
                }
            }
            else {
                /* we are in the side buffer. */
                if (*(data->pos - 1) == 0) {
                    /*
                    At the start of the normalize side buffer.
                    Go back to string.
                    Because pointer points to the last accessed character,
                    hence we have to increment it by one here.
                    */
                    if (data->fcdPosition == NULL) {
                        data->pos = data->string;
                        return UCOL_NO_MORE_CES;
                    }
                    else {
                        data->pos   = data->fcdPosition + 1;
                    }
                    data->flags = data->origFlags;
                    continue;
                }
            }
            data->pos --;
            ch = *(data->pos);

            /*
            * if there's no fcd and/or normalization stuff to do.
            * if the current character is not fcd.
            * if current character is at the start of the string
            * Trailing combining class == 0.
            * Note if pos is in the writablebuffer, norm is always 0
            */
            if ((data->flags & UCOL_ITER_NORM) == 0 ||
                data->fcdPosition <= data->pos ||
                data->string == data->pos ||
                ch < ZERO_CC_LIMIT_) {
                break;
            }

            if (ch < NFC_ZERO_CC_BLOCK_LIMIT_) {
                /* if next character is FCD */
                if (data->pos == data->string) {
                    /* First char of string is always OK for FCD check */
                    break;
                }

                /* Not first char of string, do the FCD fast test */
                if (*(data->pos - 1) < NFC_ZERO_CC_BLOCK_LIMIT_) {
                    break;
                }
            }

            /* Need a more complete FCD check and possible normalization. */
            if (collPrevIterFCD(data)) {
                collPrevIterNormalize(data);
            }

            if ((data->flags & UCOL_ITER_INNORMBUF) == 0) {
                /*  No normalization. Go ahead and process the char. */
                break;
            }

            /*
            Some normalization happened.
            Next loop picks up a char from the normalization buffer.
            */
        }

        if (ch <= 0xFF) {
          result = coll->latinOneMapping[ch];
        }
        else {
            if ((data->flags & UCOL_ITER_INNORMBUF) == 0 &&
                UCOL_ISTHAIBASECONSONANT(ch) && data->pos > data->string &&
                UCOL_ISTHAIPREVOWEL(*(data->pos -1)))
            {
                result = UCOL_THAI;
            }
            else {
                result = ucmp32_get(coll->mapping, ch);
            }
        }

        if (result >= UCOL_NOT_FOUND) {
            result = getSpecialPrevCE(coll, result, data, status);
            if (result == UCOL_NOT_FOUND) {
                result = ucol_getPrevUCA(ch, data, status);
            }
        }
    }
    return result;
}


/*   ucol_getPrevCE, out-of-line version for use from other files.  */
U_CAPI uint32_t ucol_getPrevCE(const UCollator *coll, collIterate *data,
                        UErrorCode *status) {
    return ucol_IGetPrevCE(coll, data, status);
}


/* this should be connected to special Jamo handling */
uint32_t ucol_getFirstCE(const UCollator *coll, UChar u, UErrorCode *status) {
  collIterate colIt;
  uint32_t order;
  IInit_collIterate(coll, &u, 1, &colIt);
  order = ucol_IGetNextCE(coll, &colIt, status);
  /*UCOL_GETNEXTCE(order, coll, colIt, status);*/
  return order;
}

#if 0
/* bogus code, based on the wrong assumption */
void getSpecialJamo(const UCollator *coll, uint32_t CE, uint32_t **buffer) {
      for(;;) {
        uint32_t tag = getCETag(CE);
        if(tag == THAI_TAG || tag == EXPANSION_TAG) {
          uint32_t i = 0;
            uint32_t *CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
            uint32_t size = getExpansionCount(CE);
            if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
              for(i = 1; i<size; i++) {
                 *(*buffer++) = *CEOffset++;
              }
            } else { /* else, we do */
              while(*CEOffset != 0) {
                *(*buffer++) = *CEOffset++;
              }
            }
            break;
        } else if(tag == CONTRACTION_TAG) {
            const UChar *ContractionStart = (UChar *)coll->image+getContractOffset(CE);
            *(*buffer++) = *(coll->contractionCEs + (ContractionStart- coll->contractionIndex));
        }
      }
}

void ucol_getJamoCEs(const UCollator *coll, UChar ch, uint32_t **buffer) {
  uint32_t order;
  if(ch <= 0xFF) {                                                 /* if it's Latin One, we'll try to fast track it */
    order = coll->latinOneMapping[ch];                            /* by looking in up in an array */
  } else {                                                        /* otherwise, */
    order = ucmp32_get(coll->mapping, ch);                        /* we'll go for slightly slower trie */
  }
  if(order > UCOL_NOT_FOUND) {                                   /* if a CE is special */
    getSpecialJamo(coll, order, buffer);       /* and try to get the special CE */
  } else if(order == UCOL_NOT_FOUND) { /* consult the UCA */
    if(ch <= 0xFF) {                                                 /* if it's Latin One, we'll try to fast track it */
      order = UCA->latinOneMapping[ch];                            /* by looking in up in an array */
    } else {                                                        /* otherwise, */
      order = ucmp32_get(UCA->mapping, ch);                        /* we'll go for slightly slower trie */
    }
    if(order > UCOL_NOT_FOUND) {
      getSpecialJamo(UCA, order, buffer);       /* and try to get the special CE */
    }
  }
  *(*buffer++) = order;
}

#endif

/* This function tries to get a CE from UCA, which should be always around  */
/* UChar is passed in in order to speed things up                           */
/* here is also the generation of implicit CEs                              */
uint32_t ucol_getNextUCA(UChar ch, collIterate *collationSource, UErrorCode *status) {
    uint32_t order;
    if(ch < 0xFF) {               /* so we'll try to find it in the UCA */
      order = UCA->latinOneMapping[ch];
    } else {
      order = ucmp32_get(UCA->mapping, ch);
    }
    if(order >= UCOL_NOT_FOUND) { /* UCA also gives us a special CE */
      order = getSpecialCE(UCA, order, collationSource, status);
    }
    if(order == UCOL_NOT_FOUND) { /* This is where we have to resort to algorithmical generation */
      /* We have to check if ch is possibly a first surrogate - then we need to take the next code unit */
      /* and make a bigger CE */
      UChar nextChar;
      const uint32_t
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
        //LLimit = LBase + LCount,    // 1113
        //VLimit = VBase + VCount,    // 1176
        //TLimit = TBase + TCount,    // 11C3
        //SLimit = SBase + SCount;    // D7A4

        // once we have failed to find a match for codepoint cp, and are in the implicit code.

        uint32_t L = ch - SBase;
        //if (ch < SLimit) { // since it is unsigned, catchs zero case too
        if (L < SCount) { // since it is unsigned, catchs zero case too

          // divide into pieces

          uint32_t T = L % TCount; // we do it in this order since some compilers can do % and / in one operation
          L /= TCount;
          uint32_t V = L % VCount;
          L /= VCount;

          // offset them

          L += LBase;
          V += VBase;
          T += TBase;

          // return the first CE, but first put the rest into the expansion buffer
          if (!collationSource->coll->image->jamoSpecial) { // FAST PATH

            *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, V);
            if (T != TBase) {
                *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, T);
            }

            return ucmp32_get(UCA->mapping, L); // return first one

          } else { // Jamo is Special
            collIterate jamos;
            UChar jamoString[3];
            uint32_t CE = UCOL_NOT_FOUND;
            const UCollator *collator = collationSource->coll;
            jamoString[0] = (UChar)L;
            jamoString[1] = (UChar)V;
            if (T != TBase) {
              jamoString[2] = (UChar)T;
              IInit_collIterate(collator, jamoString, 3, &jamos);
            } else {
              IInit_collIterate(collator, jamoString, 2, &jamos);
            }

            CE = ucol_IGetNextCE(collator, &jamos, status);

            while(CE != UCOL_NO_MORE_CES) {
              *(collationSource->CEpos++) = CE;
              CE = ucol_IGetNextCE(collator, &jamos, status);
            }
            return *(collationSource->toReturn++);

            /* Code and pseudocode below is bogus - we didn't take into  */
            /* account that any combo of L,V,T could be */
            /* in fact a contraction - we cannot look at them separately */

/*
            ucol_getJamoCEs(collationSource->coll, L, &collationSource->CEpos);
            ucol_getJamoCEs(collationSource->coll, V, &collationSource->CEpos);
            if (T != TBase) {
              ucol_getJamoCEs(collationSource->coll, T, &collationSource->CEpos);
            }
            return *(collationSource->toReturn++);
*/
/*
            // do recursive processing of L, V, and T with fetchCE (but T only if not equal to TBase!!)
            // Since fetchCE returns a CE, and (potentially) stuffs items into the ce buffer,
            // this is how it is done.
            int firstCE = fetchCE(L, ...);
            int* lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
            *lastExpansion = fetchCE(V,...);
            if (T != TBase) {
              lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
              *lastExpansion = fetchCE(T,...);
            }
*/
          }
      }

      uint32_t cp = 0;

      if(UTF_IS_FIRST_SURROGATE(ch)) {
        if( (((collationSource->flags & UCOL_ITER_HASLEN) == 0 ) || (collationSource->pos<collationSource->endp)) &&
          UTF_IS_SECOND_SURROGATE((nextChar=*collationSource->pos))) {
          cp = (((ch)<<10UL)+(nextChar)-((0xd800<<10UL)+0xdc00));
          collationSource->pos++;
          if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
              return 0;  /* illegal code value, use completely ignoreable! */
          }
          /* This is a code point minus 0x10000, that's what algorithm requires */
          //order = 0xE0010303 | (cp & 0xFFE00) << 8;
          //*(collationSource->CEpos++) = 0x80200080 | (cp & 0x001FF) << 22;
        } else {
          return 0; /* completely ignorable */
        }
      } else {
        /* otherwise */
        if(UTF_IS_SECOND_SURROGATE((ch)) || (ch & 0xFFFE) == 0xFFFE) {
          return 0; /* completely ignorable */
        }
        cp = ch;
        /* Make up an artifical CE from code point as per UCA */
        //order = 0xD0800303 | (ch & 0xF000) << 12 | (ch & 0x0FE0) << 11;
        //*(collationSource->CEpos++) = 0x04000080 | (ch & 0x001F) << 27;
      }

      /*
      we must skip all 00, 01, 02 bytes, so most bytes have 253 values
      we must leave a gap of 01 between all values of the last byte, so the last byte has 126 values (3 byte case)
      we shift so that HAN all has the same first primary, for compression.
      for the 4 byte case, we make the gap as large as we can fit.
      Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
      Four byte forms (most supplementaries) are EF xx xx xx (with a gap of LAST2_MULTIPLIER == 14)
      */
      int32_t last0 = cp - IMPLICIT_BOUNDARY_;
      uint32_t r = 0;

      if (last0 < 0) {
          cp += IMPLICIT_HAN_SHIFT_; // shift so HAN shares single block
          int32_t last1 = cp / IMPLICIT_LAST_COUNT_;
          last0 = cp % IMPLICIT_LAST_COUNT_;
          int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
          last1 %= IMPLICIT_OTHER_COUNT_;
          r = 0xEC030300 + (last2 << 24) + (last1 << 16) + (last0 << 9);
      } else {
          int32_t last1 = last0 / IMPLICIT_LAST_COUNT2_;
          last0 %= IMPLICIT_LAST_COUNT2_;
          int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
          last1 %= IMPLICIT_OTHER_COUNT_;
          r = 0xEF030303 + (last2 << 16) + (last1 << 8) + (last0 * IMPLICIT_LAST2_MULTIPLIER_);
      }
      order = (r & 0xFFFF0000) | 0x00000303;
      *(collationSource->CEpos++) = ((r & 0x0000FFFF)<<16) | 0x000000C0;

    }
    return order; /* return the CE */
}

/*
* This function tries to get a CE from UCA, which should be always around
* UChar is passed in in order to speed things up here is also the generation
* of implicit CEs
*/
uint32_t ucol_getPrevUCA(UChar ch, collIterate *collationSource,
                         UErrorCode *status)
{
  uint32_t order;
  if (ch < 0xFF) {
    order = UCA->latinOneMapping[ch];
  }
  else {
    order = ucmp32_get(UCA->mapping, ch);
  }

  if (order > UCOL_NOT_FOUND) {
    order = getSpecialPrevCE(UCA, order, collationSource, status);
  }

  if (order == UCOL_NOT_FOUND)
  {
    uint32_t cp = 0;
    /*
    This is where we have to resort to algorithmical generation.
    We have to check if ch is possibly a first surrogate - then we need to
    take the next code unit and make a bigger CE
    */
    uint32_t
      SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
      LCount = 19, VCount = 21, TCount = 28,
      NCount = VCount * TCount,   /* 588 */
      SCount = LCount * NCount;   /* 11172 */

    /*
    once we have failed to find a match for codepoint cp, and are in the
    implicit code.
    */
    uint32_t L = ch - SBase;
    if (L < SCount)
    { /* since it is unsigned, catchs zero case too */

      /*
      divide into pieces.
      we do it in this order since some compilers can do % and / in one
      operation
      */
      uint32_t T = L % TCount;
      L /= TCount;
      uint32_t V = L % VCount;
      L /= VCount;

      /* offset them */
      L += LBase;
      V += VBase;
      T += TBase;

      /*
      return the first CE, but first put the rest into the expansion buffer
      */
      if (!collationSource->coll->image->jamoSpecial)
      {
        *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, L);
        *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, V);
        if (T != TBase)
          *(collationSource->CEpos ++) = ucmp32_get(UCA->mapping, T);

        collationSource->toReturn = collationSource->CEpos - 1;
        return *(collationSource->toReturn);
      } else {
        collIterate jamos;
        UChar jamoString[3];
        uint32_t CE = UCOL_NOT_FOUND;
        const UCollator *collator = collationSource->coll;
        jamoString[0] = (UChar)L;
        jamoString[1] = (UChar)V;
        if (T != TBase) {
          jamoString[2] = (UChar)T;
          IInit_collIterate(collator, jamoString, 3, &jamos);
        } else {
          IInit_collIterate(collator, jamoString, 2, &jamos);
        }

        CE = ucol_IGetNextCE(collator, &jamos, status);

        while(CE != UCOL_NO_MORE_CES) {
          *(collationSource->CEpos++) = CE;
          CE = ucol_IGetNextCE(collator, &jamos, status);
        }
        collationSource->toReturn = collationSource->CEpos - 1;
        return *(collationSource->toReturn);
        }
    }

    if (UTF_IS_SECOND_SURROGATE(ch))
    {
        UChar  prevChar;
        UChar *prev;
        if ((collationSource->string == collationSource->pos) ||
            (collationSource->pos == collationSource->writableBuffer &&
            collationSource->fcdPosition == NULL)) {
            /* we are at the start of the string, wrong place to be at */
            return 0;
        }
        if (collationSource->pos != collationSource->writableBuffer) {
            prev     = collationSource->pos - 1;
        }
        else {
            prev     = collationSource->fcdPosition;
        }
        prevChar = *prev;

        /* Handles Han and Supplementary characters here.*/
        if (UTF_IS_FIRST_SURROGATE(prevChar))
      {
            cp = ((prevChar << 10UL) + ch - ((0xd800 << 10UL) + 0xdc00));
            collationSource->pos = prev;
        if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
          return 0;  /* illegal code value, use completely ignoreable! */
        }
      }
      else {
        return 0; /* completely ignorable */
      }
    }
    else
    {
      /* otherwise */
      if (UTF_IS_FIRST_SURROGATE(ch) || (ch & 0xFFFE) == 0xFFFE) {
        return 0; /* completely ignorable */
      }
      cp = ch;
    }

      /* we must skip all 00, 01, 02 bytes, so most bytes have 253 values
       we must leave a gap of 01 between all values of the last byte, so the last byte has 126 values (3 byte case)
       we shift so that HAN all has the same first primary, for compression.
       for the 4 byte case, we make the gap as large as we can fit.
       Three byte forms are EC xx xx, ED xx xx, EE xx xx (with a gap of 1)
       Four byte forms (most supplementaries) are EF xx xx xx (with a gap of LAST2_MULTIPLIER == 14)
      */
      int32_t last0 = cp - IMPLICIT_BOUNDARY_;
      uint32_t r = 0;

      if (last0 < 0) {
          cp += IMPLICIT_HAN_SHIFT_; // shift so HAN shares single block
          int32_t last1 = cp / IMPLICIT_LAST_COUNT_;
          last0 = cp % IMPLICIT_LAST_COUNT_;
          int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
          last1 %= IMPLICIT_OTHER_COUNT_;
          r = 0xEC030300 + (last2 << 24) + (last1 << 16) + (last0 << 9);
      } else {
          int32_t last1 = last0 / IMPLICIT_LAST_COUNT2_;
          last0 %= IMPLICIT_LAST_COUNT2_;
          int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
          last1 %= IMPLICIT_OTHER_COUNT_;
          r = 0xEF030303 + (last2 << 16) + (last1 << 8) +
              (last0 * IMPLICIT_LAST2_MULTIPLIER_);
      }
      /*
      order = (r & 0xFFFF0000) | 0x00000303;
      *(collationSource->CEpos++) = ((r & 0x0000FFFF)<<16) | 0x00000080;
      */
      *(collationSource->CEpos++) = (r & 0xFFFF0000) | 0x00000303;
      collationSource->toReturn = collationSource->CEpos;
      order = ((r & 0x0000FFFF)<<16) | 0x000000C0;
  }
  return order; /* return the CE */
}

/**
* Inserts the argument character into the end of the buffer pushing back the
* null terminator.
* @param data collIterate struct data
* @param pNull pointer to the null termination
* @param ch character to be appended
* @return the position of the new addition
*/
inline UChar * insertBufferEnd(collIterate *data, UChar *pNull, UChar ch)
{
          uint32_t  size    = data->writableBufSize;
          UChar    *newbuffer;
    const uint32_t  incsize = 5;

    if ((data->writableBuffer + size) > (pNull + 1)) {
        *pNull = ch;
        *(pNull + 1) = 0;
        return pNull;
    }

    /*
    buffer will always be null terminated at the end.
    giving extra space since it is likely that more characters will be added.
    */
    size += incsize;
    newbuffer = (UChar *)uprv_malloc(sizeof(UChar) * size);
    uprv_memcpy(newbuffer, data->writableBuffer,
                data->writableBufSize * sizeof(UChar));

    freeHeapWritableBuffer(data);
    data->writableBufSize = size;
    data->writableBuffer  = newbuffer;

    newbuffer        = newbuffer + data->writableBufSize;
    *newbuffer       = ch;
    *(newbuffer + 1) = 0;
    return newbuffer;
}

/**
* Inserts the argument string into the end of the buffer pushing back the
* null terminator.
* @param data collIterate struct data
* @param pNull pointer to the null termination
* @param string to be appended
* @param length of the string to be appended
* @return the position of the new addition
*/
inline UChar * insertBufferEnd(collIterate *data, UChar *pNull, UChar *str,
                               int length)
{
    uint32_t  size = pNull - data->writableBuffer;
    UChar    *newbuffer;

    if (data->writableBuffer + data->writableBufSize > pNull + length + 1) {
        uprv_memcpy(pNull, str, length * sizeof(UChar));
        *(pNull + length) = 0;
        return pNull;
    }

    /*
    buffer will always be null terminated at the end.
    giving extra space since it is likely that more characters will be added.
    */
    newbuffer = (UChar *)uprv_malloc(sizeof(UChar) * (size + length + 1));
    uprv_memcpy(newbuffer, data->writableBuffer, size * sizeof(UChar));
    uprv_memcpy(newbuffer + size, str, length * sizeof(UChar));

    freeHeapWritableBuffer(data);
    data->writableBufSize = size + length + 1;
    data->writableBuffer  = newbuffer;

    return newbuffer;
}

/**
* Special normalization function for contraction in the forwards iterator.
* This normalization sequence will place the current character at source->pos
* and its following normalized sequence into the buffer.
* The fcd position, pos will be changed.
* pos will now point to positions in the buffer.
* Flags will be changed accordingly.
* @param data collation iterator data
*/
inline void normalizeNextContraction(collIterate *data)
{
    UChar      *buffer     = data->writableBuffer;
    uint32_t    buffersize = data->writableBufSize;
    uint32_t    strsize;
    UErrorCode  status     = U_ZERO_ERROR;
    /* because the pointer points to the next character */
    UChar      *pStart     = data->pos - 1;
    UChar      *pEnd;
    uint32_t    normLen;
    UChar      *pStartNorm;

    if ((data->flags & UCOL_ITER_INNORMBUF) == 0) {
        *data->writableBuffer = *(pStart - 1);
        strsize               = 1;
    }
    else {
        strsize = u_strlen(data->writableBuffer);
    }

    pEnd = data->fcdPosition;

    normLen = unorm_normalize(pStart, pEnd - pStart, UNORM_NFD, 0, buffer, 0,
                              &status);

    if (buffersize <= normLen + strsize) {
        uint32_t  size = strsize + normLen + 1;
        UChar    *temp = (UChar *)uprv_malloc(size * sizeof(UChar));
        uprv_memcpy(temp, buffer, sizeof(UChar) * strsize);
        freeHeapWritableBuffer(data);
        data->writableBuffer = temp;
        data->writableBufSize = size;
    }

    status            = U_ZERO_ERROR;
    pStartNorm        = buffer + strsize;
    /* null-termination will be added here */
    unorm_normalize(pStart, pEnd - pStart, UNORM_NFD, 0, pStartNorm,
                    normLen + 1, &status);

    data->pos        = data->writableBuffer + strsize;
    data->origFlags  = data->flags;
    data->flags     |= UCOL_ITER_INNORMBUF;
    data->flags     &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);
}

/**
* Contraction character management function that returns the next character
* for the forwards iterator.
* Does nothing if the next character is in buffer and not the first character
* in it.
* Else it checks next character in data string to see if it is normalizable.
* If it is not, the character is simply copied into the buffer, else
* the whole normalized substring is copied into the buffer, including the
* current character.
* @param data collation element iterator data
* @return next character
*/
inline UChar getNextNormalizedChar(collIterate *data)
{
    UChar  nextch;
    UChar  ch;
    UBool  innormbuf = (UBool)(data->flags & UCOL_ITER_INNORMBUF);
    UChar  *pEndWritableBuffer = NULL;
    if ((data->flags & (UCOL_ITER_NORM | UCOL_ITER_INNORMBUF)) == 0 ||
        (innormbuf && *data->pos != 0) ||
        (data->fcdPosition != NULL && !innormbuf &&
        data->pos < data->fcdPosition)) {
        /*
        if no normalization and not in buffer.
        if next character is in normalized buffer, no further normalization
        is required
        */
        return *(data->pos ++);
    }

    if (data->flags & UCOL_ITER_HASLEN) {
        /* in data string */
        if (data->pos + 1 == data->endp) {
            return *(data->pos ++);
        }
    }
    else {
        if (innormbuf) {
            /*
            in writable buffer, at this point fcdPosition can not be
            pointing to the end of the data string. see contracting tag.
            */
            if (*(data->fcdPosition + 1) == 0 ||
                data->fcdPosition + 1 == data->endp) {
                /* at the end of the string, dump it into the normalizer */
                data->pos = insertBufferEnd(data, data->pos,
                                            *(data->fcdPosition)) + 1;
                return *(data->fcdPosition ++);
            }
            pEndWritableBuffer = data->pos;
            data->pos = data->fcdPosition;
        }
        else {
            if (*(data->pos + 1) == 0) {
                return *(data->pos ++);
            }
        }
    }

    ch = *data->pos ++;
    nextch = *data->pos;

    /*
    * if the current character is not fcd.
    * Trailing combining class == 0.
    */
    if ((data->fcdPosition == NULL || data->fcdPosition < data->pos) &&
        (nextch >= NFC_ZERO_CC_BLOCK_LIMIT_ ||
         ch >= NFC_ZERO_CC_BLOCK_LIMIT_)) {
            /*
            Need a more complete FCD check and possible normalization.
            normalize substring will be appended to buffer
            */
        if (collIterFCD(data)) {
            normalizeNextContraction(data);
            return *(data->pos ++);
        }
        else if (innormbuf) {
            /* fcdposition shifted even when there's no normalization, if we
            don't input the rest into this, we'll get the wrong position when
            we reach the end of the writableBuffer */
            int length = data->fcdPosition - data->pos + 1;
            data->pos = insertBufferEnd(data, pEndWritableBuffer,
                                        data->pos - 1, length);
            return *(data->pos ++);
        }
    }

    if (innormbuf) {
        /*
        no normalization is to be done hence only one character will be
        appended to the buffer.
        */
        data->pos = insertBufferEnd(data, pEndWritableBuffer, ch) + 1;
    }

    /* points back to the pos in string */
    return ch;
}

/** 
* Function to copy the buffer into writableBuffer and sets the fcd position to 
* the correct position
* @param source data string source
* @param buffer character buffer
* @param tempdb current position in buffer that has been used up
*/
inline void setDiscontiguosAttribute(collIterate *source, UChar *buffer, 
                                     UChar *tempdb)
{
    /* okay confusing part here. to ensure that the skipped characters are 
    considered later, we need to place it in the appropriate position in the 
    normalization buffer and reassign the pos pointer. simple case if pos 
    reside in string, simply copy to normalization buffer and 
    fcdposition = pos, pos = start of normalization buffer. if pos in 
    normalization buffer, we'll insert the copy infront of pos and point pos 
    to the start of the normalization buffer. why am i doing these copies? 
    well, so that the whole chunk of codes in the getNextCE, getSpecialCE does 
    not require any changes, which be really painful. */
    uint32_t length = u_strlen(buffer);;
    if (source->flags & UCOL_ITER_INNORMBUF) {
        u_strcpy(tempdb, source->pos);
    }
    else {
        source->fcdPosition  = source->pos;
        source->origFlags    = source->flags;
        source->flags       |= UCOL_ITER_INNORMBUF;
        source->flags       &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);
    }

    if (length >= source->writableBufSize) {
        freeHeapWritableBuffer(source);
        source->writableBuffer =
                     (UChar *)uprv_malloc((length + 1) * sizeof(UChar));
        source->writableBufSize = length;
    }

    u_strcpy(source->writableBuffer, buffer);
    source->pos = source->writableBuffer;
}

/**
* Function to get the discontiguos collation element within the source.
* Note this function will set the position to the appropriate places.
* @param coll current collator used
* @param source data string source
* @param constart index to the start character in the contraction table
* @return discontiguos collation element offset
*/
inline uint32_t getDiscontiguos(const UCollator *coll, collIterate *source,
                                const UChar *constart)
{
    /* source->pos currently points to the second combining character after
       the start character */
          UChar   *temppos      = source->pos;
          UChar    buffer[UCOL_MAX_BUFFER];
          UChar   *tempdb       = buffer;
    const UChar   *tempconstart = constart;
          uint8_t  tempflags    = source->flags;
          UBool    multicontraction = FALSE;
          UChar   *tempbufferpos = 0;

    *tempdb = *(source->pos - 1);
    tempdb ++;
    while (TRUE) {
        UChar    *UCharOffset;
        UChar     schar,
                  tchar;
        uint32_t  result;

        if (((source->flags & UCOL_ITER_HASLEN) && source->pos >= source->endp)
            || (*source->pos == 0  &&
                ((source->flags & UCOL_ITER_INNORMBUF) == 0 ||
                 source->fcdPosition == NULL ||
                 source->fcdPosition == source->endp ||
                 *(source->fcdPosition) == 0 ||
                 u_getCombiningClass(*(source->fcdPosition)) == 0)) ||
                 /* end of string in null terminated string or stopped by a
                 null character, note fcd does not always point to a base
                 character after the discontiguos change */
                 u_getCombiningClass(*(source->pos)) == 0) {
            //constart = (UChar *)coll->image + getContractOffset(CE);
            if (multicontraction) {
                *tempbufferpos = 0;
                source->pos    = temppos - 1;
                setDiscontiguosAttribute(source, buffer, tempdb);
                return *(coll->contractionCEs + 
                                    (tempconstart - coll->contractionIndex));
            }
            constart = tempconstart;
            break;
        }

        UCharOffset = (UChar *)(tempconstart + 1); /* skip the backward offset*/
        schar = getNextNormalizedChar(source);

        while (schar > (tchar = *UCharOffset)) {
            UCharOffset++;
        }

        if (schar != tchar) {
            /* not the correct codepoint. we stuff the current codepoint into
            the discontiguos buffer and try the next character */
            *tempdb = schar;
            tempdb ++;
            continue;
        }
        else {
            if (u_getCombiningClass(schar) ==
                u_getCombiningClass(*(source->pos - 2))) {
                *tempdb = schar;
                tempdb ++;
                continue;
            }
            result = *(coll->contractionCEs +
                                      (UCharOffset - coll->contractionIndex));
        }
        *tempdb = 0;

        if (result == UCOL_NOT_FOUND) {
          break;
        } else if (isContraction(result)) {
            /* this is a multi-contraction*/
            tempconstart = (UChar *)coll->image + getContractOffset(result);
            if (*(coll->contractionCEs + (constart - coll->contractionIndex))
                != UCOL_NOT_FOUND) {
                multicontraction = TRUE;
                temppos       = source->pos + 1;
                tempbufferpos = buffer + u_strlen(buffer);
            }
        } else {
            setDiscontiguosAttribute(source, buffer, tempdb);
            return result;
        }
    }

    /* no problems simply reverting just like that,
    if we are in string before getting into this function, points back to
    string hence no problem.
    if we are in normalization buffer before getting into this function,
    since we'll never use another normalization within this function, we
    know that fcdposition points to a base character. the normalization buffer
    never change, hence this revert works. */
    source->pos   = temppos - 1;
    source->flags = tempflags;
    return *(coll->contractionCEs + (constart - coll->contractionIndex));
}

/* This function handles the special CEs like contractions, expansions, surrogates, Thai */
/* It is called by both getNextCE and getNextUCA                                         */
uint32_t getSpecialCE(const UCollator *coll, uint32_t CE, collIterate *source, UErrorCode *status) {
  uint32_t i = 0; /* general counter */
  uint32_t firstCE = UCOL_NOT_FOUND;
  //UChar   *firstUChar = source->pos;
  collIterateState state;
  backupState(source, &state);
  //uint32_t CE = *source->CEpos;
  for (;;) {
    const uint32_t *CEOffset = NULL;
    const UChar *UCharOffset = NULL;
    UChar schar, tchar;
    uint32_t size = 0;
    switch(getCETag(CE)) {
    case NOT_FOUND_TAG:
      /* This one is not found, and we'll let somebody else bother about it... no more games */
      return CE;
    case SURROGATE_TAG:
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
    case THAI_TAG:
      /* Thai/Lao reordering */
        if  (((source->flags) & UCOL_ITER_INNORMBUF) ||     /* Already Swapped     ||                 */
            source->endp == source->pos              ||     /* At end of string.  No swap possible || */
            UCOL_ISTHAIBASECONSONANT(*(source->pos)) == 0)  /* next char not Thai base cons.          */
        {
            // Treat Thai as a length one expansion */
            CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
            CE = *CEOffset++;
        }
        else
        {
            // Move the prevowel and the following base Consonant into the normalization buffer
            //   with their order swapped
            source->writableBuffer[0] = *source->pos;
            source->writableBuffer[1] = *(source->pos - 1);
            source->writableBuffer[2] = 0;

            source->fcdPosition       = source->pos+1;   // Indicate where to continue in main input string
                                                         //   after exhausting the writableBuffer
        source->pos   = source->writableBuffer;
            source->origFlags         = source->flags;
            source->flags            |= UCOL_ITER_INNORMBUF;
            source->flags            &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);

        CE = UCOL_IGNORABLE;
      }
      break;

    case CONTRACTION_TAG:
      /* This should handle contractions */
      for (;;) {
        /* First we position ourselves at the begining of contraction sequence */
        const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

        if (source->pos == source->endp ||
            /* end of string in non-null terminated string */
            (*source->pos == 0 && (source->flags & UCOL_ITER_HASLEN) == 0 &&
            /* end of null-termination string or normalization buffer */
                ((source->flags & UCOL_ITER_INNORMBUF) == 0 ||
                /* end of null-terminated string */
                source->fcdPosition == NULL ||
                source->fcdPosition == source->endp ||
                ((source->origFlags & UCOL_ITER_HASLEN) == 0 &&
                *source->fcdPosition == 0)))) {
            /* fcd does not point to a valid character*/
            CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
            // So we'll pick whatever we have at the point...
            if (CE == UCOL_NOT_FOUND) {
              // spit all the not found chars, which led us in this contraction
              if(firstCE != UCOL_NOT_FOUND) {
                CE = firstCE;
              }
              loadState(source, &state, TRUE);
            }
          break;
        }

        UCharOffset++; /* skip the backward offset, see above */

        schar = getNextNormalizedChar(source);
        while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          UCharOffset++;
        }
        if (schar != tchar) {
            UChar tempchar = 0;
            if (u_getCombiningClass(schar) != 0 &&
                source->pos != source->endp &&
                (*source->pos != 0 ||
                    ((source->flags & UCOL_ITER_INNORMBUF) &&
                    source->fcdPosition != NULL &&
                    source->fcdPosition != source->endp &&
                    *source->fcdPosition != 0))) {
                /* find the next character if schar is not a base character
                and we are not yet at the end of the string */
                tempchar = getNextNormalizedChar(source);
                source->pos --;
            }
            if (tempchar == 0 || u_getCombiningClass(tempchar) == 0) {
                source->pos --;
                /* Spit out the last char of the string, wasn't tasty enough */
                CE = *(coll->contractionCEs +
                     (ContractionStart - coll->contractionIndex));
            }
            else {
                CE = getDiscontiguos(coll, source, ContractionStart);
            }
        }
        else {
            CE = *(coll->contractionCEs +
                 (UCharOffset - coll->contractionIndex));
        }

        if(CE == UCOL_NOT_FOUND) {
          // source->pos   = firstUChar; /* spit all the not found chars, which led us in this contraction */
          loadState(source, &state, TRUE);
          CE = firstCE;
          break;
        } else if(isContraction(CE)) { /* fix for the bug. Other places need to be checked */
        /* this is contraction, and we will continue. However, we can fail along the */
        /* th road, which means that we have part of contraction correct */
          uint32_t tempCE = *(coll->contractionCEs + (ContractionStart - coll->contractionIndex));
          if(tempCE != UCOL_NOT_FOUND) {
            firstCE = tempCE;
            /* firstUChar = source->pos-1; */
            backupState(source, &state);
            state.pos --;
          }
        } else {
          break;
        }
      }
      break;
    case EXPANSION_TAG:
      /* This should handle expansion. */
      /* NOTE: we can encounter both continuations and expansions in an expansion! */
      /* I have to decide where continuations are going to be dealt with */
      CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
      size = getExpansionCount(CE);
      CE = *CEOffset++;
      if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
        for(i = 1; i<size; i++) {
          *(source->CEpos++) = *CEOffset++;
        }
      } else { /* else, we do */
        while(*CEOffset != 0) {
          *(source->CEpos++) = *CEOffset++;
        }
      }
      return CE;
    case CHARSET_TAG:
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
    default:
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) break;
  }
  return CE;
}

/**
* Inserts the argument character into the front of the buffer replacing the
* front null terminator.
* @param data collation element iterator data
* @param pNull pointer to the null terminator
* @param ch character to be appended
* @return positon of added character
*/
inline UChar * insertBufferFront(collIterate *data, UChar *pNull, UChar ch)
{
          uint32_t  size    = data->writableBufSize;
          UChar    *end;
          UChar    *newbuffer;
    const uint32_t  incsize = 5;

    if (pNull > data->writableBuffer + 1) {
        *pNull       = ch;
        *(pNull - 1) = 0;
        return pNull;
    }

    /*
    buffer will always be null terminated infront.
    giving extra space since it is likely that more characters will be added.
    */
    size += incsize;
    newbuffer = (UChar *)uprv_malloc(sizeof(UChar) * size);
    end = newbuffer + incsize;
    uprv_memcpy(end, data->writableBuffer,
                data->writableBufSize * sizeof(UChar));
    *end       = ch;
    *(end - 1) = 0;

    freeHeapWritableBuffer(data);

    data->writableBufSize = size;
    data->writableBuffer  = newbuffer;
    return end;
}

/**
* Special normalization function for contraction in the previous iterator.
* This normalization sequence will place the current character at source->pos
* and its following normalized sequence into the buffer.
* The fcd position, pos will be changed.
* pos will now point to positions in the buffer.
* Flags will be changed accordingly.
* @param data collation iterator data
*/
inline void normalizePrevContraction(collIterate *data)
{
    UChar      *buffer     = data->writableBuffer;
    uint32_t    buffersize = data->writableBufSize;
    uint32_t    nulltermsize;
    UErrorCode  status     = U_ZERO_ERROR;
    UChar      *pEnd       = data->pos + 1;         /* End normalize + 1 */
    UChar      *pStart;
    uint32_t    normLen;
    UChar      *pStartNorm;

    if (data->flags & UCOL_ITER_HASLEN) {
        /*
        normalization buffer not used yet, we'll pull down the next
        character into the end of the buffer
        */
        *(buffer + (buffersize - 1)) = *(data->pos + 1);
        nulltermsize                  = buffersize - 1;
    }
    else {
        nulltermsize = buffersize;
        UChar *temp = buffer + (nulltermsize - 1);
        while (*(temp --) != 0) {
            nulltermsize --;
        }
    }

    /* Start normalize */
    if (data->fcdPosition == NULL) {
        pStart = data->string;
    }
    else {
        pStart = data->fcdPosition + 1;
    }

    normLen = unorm_normalize(pStart, pEnd - pStart, UNORM_NFD, 0, buffer, 0,
                              &status);

    if (nulltermsize <= normLen) {
        uint32_t  size = buffersize - nulltermsize + normLen + 1;
        UChar    *temp = (UChar *)uprv_malloc(size * sizeof(UChar));
        nulltermsize   = normLen + 1;
        uprv_memcpy(temp + normLen, buffer,
                    sizeof(UChar) * (buffersize - nulltermsize));
        freeHeapWritableBuffer(data);
        data->writableBuffer = temp;
        data->writableBufSize = size;
    }

    status = U_ZERO_ERROR;
    /*
    this puts the null termination infront of the normalized string instead
    of the end
    */
    pStartNorm   = buffer + (nulltermsize - normLen);
    *(pStartNorm - 1) = 0;
    unorm_normalize(pStart, pEnd - pStart, UNORM_NFD, 0, pStartNorm, normLen,
                    &status);

    data->pos        = data->writableBuffer + nulltermsize;
    data->origFlags  = data->flags;
    data->flags     |= UCOL_ITER_INNORMBUF;
    data->flags     &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);
}

/**
* Contraction character management function that returns the previous character
* for the backwards iterator.
* Does nothing if the previous character is in buffer and not the first
* character in it.
* Else it checks previous character in data string to see if it is
* normalizable.
* If it is not, the character is simply copied into the buffer, else
* the whole normalized substring is copied into the buffer, including the
* current character.
* @param data collation element iterator data
* @return previous character
*/
inline UChar getPrevNormalizedChar(collIterate *data)
{
    UChar  prevch;
    UChar  ch;
    UChar *start;
    UBool  innormbuf = (UBool)(data->flags & UCOL_ITER_INNORMBUF);
    UChar *pNull = NULL;
    if ((data->flags & (UCOL_ITER_NORM | UCOL_ITER_INNORMBUF)) == 0 ||
        (innormbuf && *(data->pos - 1) != 0)) {
        /*
        if no normalization.
        if previous character is in normalized buffer, no further normalization
        is required
        */
        return *(data->pos - 1);
    }

    start = data->pos;
    if (data->flags & UCOL_ITER_HASLEN) {
        /* in data string */
        if ((start - 1) == data->string) {
            return *(start - 1);
        }
        start --;
        ch     = *start;
        prevch = *(start - 1);
    }
    else {
        /*
        in writable buffer, at this point fcdPosition can not be NULL.
        see contracting tag.
        */
        if (data->fcdPosition == data->string) {
            /* at the start of the string, just dump it into the normalizer */
            insertBufferFront(data, data->pos - 1, *(data->fcdPosition));
            data->fcdPosition = NULL;
            return *(data->pos - 1);
        }
        pNull  = data->pos - 1;
        start  = data->fcdPosition;
        ch     = *start;
        prevch = *(start - 1);
    }
    /*
    * if the current character is not fcd.
    * Trailing combining class == 0.
    */
    if (data->fcdPosition > start &&
       (ch >= NFC_ZERO_CC_BLOCK_LIMIT_ || prevch >= NFC_ZERO_CC_BLOCK_LIMIT_))
    {
        /*
        Need a more complete FCD check and possible normalization.
        normalize substring will be appended to buffer
        */
        UChar *backuppos = data->pos;
        data->pos = start;
        if (collPrevIterFCD(data)) {
            normalizePrevContraction(data);
            return *(data->pos - 1);
        }
        data->pos = backuppos;
        data->fcdPosition ++;
    }

    if (innormbuf) {
    /*
    no normalization is to be done hence only one character will be
    appended to the buffer.
    */
        insertBufferFront(data, pNull, ch);
        data->fcdPosition --;
    }

    return ch;
}

/**
* This function handles the special CEs like contractions, expansions,
* surrogates, Thai.
* It is called by both getPrevCE and getPrevUCA
*/
uint32_t getSpecialPrevCE(const UCollator *coll, uint32_t CE,
                          collIterate *source,
                          UErrorCode *status)
{
  const uint32_t *CEOffset    = NULL;
        UChar    *UCharOffset = NULL;
        UChar    schar;
  const UChar    *constart    = NULL;
        uint32_t size;
/*        uint32_t firstCE      = UCOL_NOT_FOUND;*/
        UChar    buffer[UCOL_MAX_BUFFER];

  for(;;)
  {
    /* the only ces that loops are thai and contractions */
    switch (getCETag(CE))
    {
    case NOT_FOUND_TAG:  /* this tag always returns */
      return CE;
    case SURROGATE_TAG:  /* this tag always returns */
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
    case THAI_TAG:
      if  ((source->flags & UCOL_ITER_INNORMBUF) || /* Already Swapped || */
            source->string == source->pos        || /* At start of string.|| */
            /* previous char not Thai prevowel */
            UCOL_ISTHAIBASECONSONANT(*(source->pos)) == FALSE ||
            UCOL_ISTHAIPREVOWEL(*(source->pos - 1)) == FALSE)
      {
          /* Treat Thai as a length one expansion */
          /* find the offset to expansion table */
          CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE);
          CE = *CEOffset ++;
      }
      else
      {
          /*
          Move the prevowel and the following base Consonant into the
          normalization buffer with their order swapped
          */
          UChar *tempbuffer = source->writableBuffer +
                              (source->writableBufSize - 1);
          *(tempbuffer - 2) = 0;
          *(tempbuffer - 1) = *source->pos;
          *(tempbuffer)     = *(source->pos - 1);

          /*
          Indicate where to continue in main input string after exhausting
          the writableBuffer
          */
          if (source->pos - 1 == source->string) {
              source->fcdPosition = NULL;
          }
          else {
            source->fcdPosition       = source->pos - 2;
          }

          source->pos               = tempbuffer;
          source->origFlags         = source->flags;
          source->flags            |= UCOL_ITER_INNORMBUF;
          source->flags            &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);

          CE = UCOL_IGNORABLE;
      }
      break;
    case CONTRACTION_TAG:
        /* to ensure that the backwards and forwards iteration matches, we
        take the current region of most possible match and pass it through
        the forward iteration. this will ensure that the obstinate problem of
        overlapping contractions will not occur.
        */
        schar = *(source->pos);
        constart = (UChar *)coll->image + getContractOffset(CE);
        if (source->pos == source->string ||
            ((source->flags & UCOL_ITER_INNORMBUF) &&
            *(source->pos - 1) == 0 && source->fcdPosition == NULL) ||
            !ucol_contractionEndCP(schar, coll)) {
            /* start of string or this is not the end of any contraction */
            CE = *(coll->contractionCEs + (constart - coll->contractionIndex));
            break;
        }
        UCharOffset = buffer + (UCOL_MAX_BUFFER - 1);
        *(UCharOffset --) = 0;
        while (ucol_unsafeCP(schar, coll)) {
            *(UCharOffset) = schar;
            UCharOffset --;
            schar = getPrevNormalizedChar(source);
            source->pos --;
            if (source->pos == source->string ||
                ((source->flags & UCOL_ITER_INNORMBUF) &&
                *(source->pos - 1) == 0 && source->fcdPosition == NULL)) {
                break;
            }
        }
        /* adds the initial base character to the string */
        *(UCharOffset) = schar;

        /* a new collIterate is used to simply things, since using the current
        collIterate will mean that the forward and backwards iteration will
        share and change the same buffers. we don't want to get into that. */
        collIterate temp;
        IInit_collIterate(coll, UCharOffset, -1, &temp);
        temp.flags &= ~UCOL_ITER_NORM;

        CE = ucol_IGetNextCE(coll, &temp, status);
        while (CE != UCOL_NO_MORE_CES) {
            *(source->CEpos ++) = CE;
            CE = ucol_IGetNextCE(coll, &temp, status);
        }
        freeHeapWritableBuffer(&temp);
        source->toReturn = source->CEpos - 1;
        if (source->toReturn == source->CEs) {
            source->CEpos = source->CEs;
        }
        return *(source->toReturn);
    case EXPANSION_TAG: /* this tag always returns */
      /*
      This should handle expansion.
      NOTE: we can encounter both continuations and expansions in an expansion!
      I have to decide where continuations are going to be dealt with
      */
      /* find the offset to expansion table */
      CEOffset = (uint32_t *)coll->image + getExpansionOffset(CE);
      size     = getExpansionCount(CE);
      if (size != 0) {
        /*
        if there are less than 16 elements in expansion, we don't terminate
        */
        uint32_t count;
        for (count = 0; count < size; count++) {
          *(source->CEpos ++) = *CEOffset++;
        }
      }
      else {
        /* else, we do */
        while (*CEOffset != 0) {
          *(source->CEpos ++) = *CEOffset ++;
        }
      }
      source->toReturn = source->CEpos - 1;
      return *(source->toReturn);
    case CHARSET_TAG:  /* this tag always returns */
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
    default:           /* this tag always returns */
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) {
      break;
    }
  }
  return CE;
}

/* This should really be a macro        */
/* However, it is used only when stack buffers are not sufficiently big, and then we're messed up performance wise */
/* anyway */
uint8_t *reallocateBuffer(uint8_t **secondaries, uint8_t *secStart, uint8_t *second, uint32_t *secSize, uint32_t newSize, UErrorCode *status) {
  fprintf(stderr, ".");
  uint8_t *newStart = NULL;

  if(secStart==second) {
    newStart=(uint8_t*)uprv_malloc(newSize);
    if(newStart==NULL) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
    uprv_memcpy(newStart, secStart, *secondaries-secStart);
  } else {
    newStart=(uint8_t*)uprv_realloc(secStart, newSize);
    if(newStart==NULL) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  }
  *secondaries=newStart+(*secondaries-secStart);
  *secSize=newSize;
  return newStart;
}


/* This should really be a macro                                                                      */
/* This function is used to reverse parts of a buffer. We need this operation when doing continuation */
/* secondaries in French                                                                              */
/*
void uprv_ucol_reverse_buffer(uint8_t *start, uint8_t *end) {
  uint8_t temp;
  while(start<end) {
    temp = *start;
    *start++ = *end;
    *end-- = temp;
  }
}
*/

#define uprv_ucol_reverse_buffer(TYPE, start, end) { \
  TYPE tempA; \
while((start)<(end)) { \
    tempA = *(start); \
    *(start)++ = *(end); \
    *(end)-- = tempA; \
} \
}

/****************************************************************************/
/* Following are the sortkey generation functions                           */
/*                                                                          */
/****************************************************************************/

/* sortkey API */
U_CAPI int32_t
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength)
{
  UErrorCode status = U_ZERO_ERROR;
  /* this uses the function pointer that is set in updateinternalstate */
  /* currently, there are two funcs: */
  /*ucol_calcSortKey(...);*/
  /*ucol_calcSortKeySimpleTertiary(...);*/

  int32_t keySize = coll->sortKeyGen(coll, source, sourceLength, &result, resultLength, FALSE, &status);
  ((UCollator *)coll)->errorCode = status; /*semantically const */
  return keySize;
}

/* this function is called by the C++ API for sortkey generation */
U_CFUNC uint8_t *ucol_getSortKeyWithAllocation(const UCollator *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        int32_t *resultLen) {
    uint8_t *result = NULL;
    UErrorCode status = U_ZERO_ERROR;
    *resultLen = coll->sortKeyGen(coll, source, sourceLength, &result, 0, TRUE, &status);
    return result;
}


/* This function tries to get the size of a sortkey. It will be invoked if the size of resulting buffer is 0  */
/* or if we run out of space while making a sortkey and want to return ASAP                                   */
int32_t ucol_getSortKeySize(const UCollator *coll, collIterate *s, int32_t currentSize, UColAttributeValue strength, int32_t len) {
    UErrorCode status = U_ZERO_ERROR;
    uint8_t compareSec   = (uint8_t)((strength >= UCOL_SECONDARY)?0:0xFF);
    uint8_t compareTer   = (uint8_t)((strength >= UCOL_TERTIARY)?0:0xFF);
    uint8_t compareQuad  = (uint8_t)((strength >= UCOL_QUATERNARY)?0:0xFF);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    UBool  doCase = (coll->caseLevel == UCOL_ON);
    UBool  shifted = (coll->alternateHandling == UCOL_SHIFTED);
    UBool  qShifted = shifted  && (compareQuad == 0);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);

    uint8_t variableMax1 = coll->variableMax1;
    uint8_t variableMax2 = coll->variableMax2;
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)(variableMax1+1);
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    uint32_t order = UCOL_NO_MORE_CES;
    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint32_t ce = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    int32_t caseShift = 0;
    uint32_t c2 = 0, c3 = 0, c4 = 0; /* variables for compression */

    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    uint8_t tertiaryCommon = coll->tertiaryCommon;

    UBool wasShifted = FALSE;
    UBool notIsContinuation = FALSE;
    uint8_t leadPrimary = 0;


    for(;;) {
          order = ucol_IGetNextCE(coll, s, &status);
          //UCOL_GETNEXTCE(order, coll, *s, &status);

          if(order == UCOL_NO_MORE_CES) {
              break;
          }
          /* fix me... we should check if we're in continuation first */
          if(isCEIgnorable(order)) {
            continue;
          }

          /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
          ce = order;
          notIsContinuation = !isContinuation(ce);


          if(notIsContinuation) {
            tertiary = (uint8_t)((order & UCOL_BYTE_SIZE_MASK));
          } else {
            tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
          }
          secondary = (uint8_t)((order >>= 8) & 0xFF);
          primary2 = (uint8_t)((order >>= 8) & 0xFF);
          primary1 = (uint8_t)(order >>= 8);


          if(shifted && ((notIsContinuation && primary1 <= variableMax1 && primary1 > 0
            && (primary1 < variableMax1 || primary1 == variableMax1 && primary2 < variableMax2))
            || (!notIsContinuation && wasShifted))) {
            if(compareQuad == 0) {
              if(c4 > 0) {
                currentSize += (c2/UCOL_BOT_COUNT4)+1;
                c4 = 0;
              }
              currentSize++;
              if(primary2 != 0) {
                currentSize++;
              }
            }
            wasShifted = TRUE;
          } else {
            wasShifted = FALSE;
            /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
            /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
#define UCOL_PRIM_COMPRESSION 1
#ifdef UCOL_PRIM_COMPRESSION
            /* calculate sortkey size */
            if(primary1 != UCOL_IGNORABLE) {
              if(notIsContinuation) {
                if(leadPrimary == primary1) {
                  currentSize++;
                } else {
                  if(leadPrimary != 0) {
                    currentSize++;
                  }
                  if(primary2 == UCOL_IGNORABLE) {
                  /* one byter, not compressed */
                      currentSize++;
                      leadPrimary = 0;
                  } else if(primary1<UCOL_BYTE_FIRST_NON_LATIN_PRIMARY ||
                      (primary1 > (UCOL_RESET_TOP_VALUE>>24) && primary1 < (UCOL_NEXT_TOP_VALUE>>24))) {
                  /* not compressible */
                      leadPrimary = 0;
                      currentSize+=2;
                  } else { /* compress */
                      leadPrimary = primary1;
                      currentSize+=2;
                  }
                }
              } else { /* we are in continuation, so we're gonna add primary to the key don't care about compression */
                currentSize++;
                if(primary2 != UCOL_IGNORABLE) {
                  currentSize++;
                }
              }
            }
#else
            if(primary1 != UCOL_IGNORABLE) {
              currentSize++;
              if(primary2 != UCOL_IGNORABLE) {
                currentSize++;
              }
            }
#endif

            if(secondary > compareSec) { /* I think that != 0 test should be != IGNORABLE */
              if(!isFrenchSec){
                if (secondary == UCOL_COMMON2 && notIsContinuation) {
                  c2++;
                } else {
                  if(c2 > 0) {
                    if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                      currentSize += (c2/(uint32_t)UCOL_TOP_COUNT2)+1;
                    } else {
                      currentSize += (c2/(uint32_t)UCOL_BOT_COUNT2)+1;
                    }
                    c2 = 0;
                  }
                  currentSize++;
                }
              } else {
                currentSize++;
              }
            }

            if(doCase) {
              if (caseShift  == 0) {
                currentSize++;
                caseShift = UCOL_CASE_SHIFT_START;
              }
              if((tertiary&0x3F) > 0 && notIsContinuation) {
                caseShift--;
                if((tertiary &0xC0) != 0) {
                  if (caseShift  == 0) {
                    currentSize++;
                    caseShift = UCOL_CASE_SHIFT_START;
                  }
                  caseShift--;
                }
              }
            } else {
              if(notIsContinuation) {
                tertiary ^= caseSwitch;
              }
            }

            tertiary &= tertiaryMask;
            if(tertiary > compareTer) { /* I think that != 0 test should be != IGNORABLE */
              if (tertiary == tertiaryCommon && notIsContinuation) {
                c3++;
              } else {
                if(c3 > 0) {
                  if((tertiary > tertiaryCommon && tertiaryCommon == UCOL_COMMON3_NORMAL)
                    || (tertiary <= tertiaryCommon && tertiaryCommon == UCOL_COMMON3_UPPERFIRST)) {
                    currentSize += (c3/(uint32_t)coll->tertiaryTopCount)+1;
                  } else {
                    currentSize += (c3/(uint32_t)coll->tertiaryBottomCount)+1;
                  }
                  c3 = 0;
                }
                currentSize++;
              }
            }

            if(qShifted  && notIsContinuation) {
              c4++;
            }

          }
    }

    if(c2 > 0) {
      currentSize += (c2/(uint32_t)UCOL_BOT_COUNT2)+1;
    }

    if(c3 > 0) {
      currentSize += (c3/(uint32_t)coll->tertiaryBottomCount)+1;
    }

    if(c4 > 0  && compareQuad == 0) {
      currentSize += (c4/UCOL_BOT_COUNT4)+1;
    }

    if(compareIdent) {
      UChar *ident = s->string;
      int32_t i = 0;
      int32_t c, prev=0x50;
      int32_t diff;
      // while(i<len) {
      for (;;) {
          if (len >=0 && i>=len) {
              break;
          }
          UTF_NEXT_CHAR(ident, i, len, c);
          if (c==0) {
              break;
          }
          diff = c-prev;
          if(diff>=SLOPE_REACH_NEG_1) {
              currentSize += (diff<=SLOPE_REACH_POS_1)?1:((diff<=SLOPE_REACH_POS_2)?2:3);
          } else {
              currentSize += (diff>=SLOPE_REACH_NEG_2)?2:3;
          }
          prev=c;
      }
    }
    return currentSize;

}

/* INTERNAL! */
/*
 * Encode a Unicode string for the identical level of a sort key.
 * Restrictions:
 * - byte stream (unsigned 8-bit bytes)
 * - lexical order of the identical-level run must be
 *   the same as code point order for the string
 * - avoid byte values 0, 1, 2
 *
 * Method: Slope Detection
 * Remember the previous code point (initial 0).
 * For each cp in the string, encode the difference to the previous one.
 *
 * With a compact encoding of differences, this yields good results for
 * small scripts and UTF-like results otherwise.
 *
 * Encoding of differences:
 * Similar to a UTF, encoding the length of the byte sequence in the lead bytes.
 * Does not need to be friendly for decoding or random access
 * (trail byte values may overlap with lead/single byte values).
 * The signedness must be encoded as the most significant part.
 *
 * We encode differences with few bytes if their absolute values are small.
 * For correct ordering, we must treat the entire value range -10ffff..+10ffff
 * in ascending order, which forbids encoding the sign and the absolute value separately.
 * Instead, we split the lead byte range in the middle and encode non-negative values
 * going up and negative values going down.
 *
 * For very small absolute values, the difference is added to a middle byte value.
 * For somewhat larger absolute values, the difference is divided by the number
 * of byte values available, the modulo is used for one trail byte, and the remainder
 * is added to a lead byte avoiding the single-byte range.
 * For large absolute values, the difference is similarly encoded in three bytes.
 *
 * This encoding does not use byte values 0, 1, 2, but uses all other byte values
 * for lead/single bytes so that the middle range of single bytes is as large
 * as possible.
 * Note that the lead byte ranges overlap some, but that the sequences as a whole
 * are well ordered. I.e., even if the lead byte is the same for sequences of different
 * lengths, the trail bytes establish correct order.
 * It would be possible to encode slightly larger ranges for each length (>1) by
 * subtracting the lower bound of the range. However, that would also slow down the
 * calculation.
 */

/*
 * encode one difference value -0x10ffff..+0x10ffff in 1..3 bytes,
 * preserving lexical order
 */
static uint8_t *
writeDiff(int32_t diff, uint8_t *p) {
    if(diff>=SLOPE_REACH_NEG_1) {
        if(diff<=SLOPE_REACH_POS_1) {
            *p++=(uint8_t)(SLOPE_MIDDLE+diff);
        } else {
            if(diff<=SLOPE_REACH_POS_2) {
                *p++=(uint8_t)(SLOPE_START_POS_2+(diff/SLOPE_TAIL_COUNT));
                *p++=(uint8_t)(SLOPE_MIN+diff%SLOPE_TAIL_COUNT);
            } else {
                p[2]=(uint8_t)(SLOPE_MIN+diff%SLOPE_TAIL_COUNT);
                diff/=SLOPE_TAIL_COUNT;
                p[1]=(uint8_t)(SLOPE_MIN+diff%SLOPE_TAIL_COUNT);
                *p=(uint8_t)(SLOPE_START_POS_3+(diff/SLOPE_TAIL_COUNT));
                p+=3;
            }
        }
    } else {
        int32_t m;

        if(diff>=SLOPE_REACH_NEG_2) {
            NEGDIVMOD(diff, SLOPE_TAIL_COUNT, m);
            *p++=(uint8_t)(SLOPE_START_NEG_2+diff);
            *p++=(uint8_t)(SLOPE_MIN+m);
        } else {
            NEGDIVMOD(diff, SLOPE_TAIL_COUNT, m);
            p[2]=(uint8_t)(SLOPE_MIN+m);
            NEGDIVMOD(diff, SLOPE_TAIL_COUNT, m);
            p[1]=(uint8_t)(SLOPE_MIN+m);
            *p=(uint8_t)(SLOPE_START_NEG_3+diff);
            p+=3;
        }
    }
    return p;
}
inline void doCaseShift(uint8_t **cases, uint32_t &caseShift) {
  if (caseShift  == 0) {
    *(*cases)++ = UCOL_CASE_BYTE_START;
    caseShift = UCOL_CASE_SHIFT_START;
  }
}

/* This is the sortkey work horse function */
int32_t
ucol_calcSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status)
{
    uint32_t i = 0; /* general purpose counter */

    /* Stack allocated buffers for buffers we use */
    uint8_t prim[UCOL_PRIMARY_MAX_BUFFER], second[UCOL_SECONDARY_MAX_BUFFER], tert[UCOL_TERTIARY_MAX_BUFFER], caseB[UCOL_CASE_MAX_BUFFER], quad[UCOL_QUAD_MAX_BUFFER];

    uint8_t *primaries = *result, *secondaries = second, *tertiaries = tert, *cases = caseB, *quads = quad;

    if(U_FAILURE(*status)) {
      return 0;
    }

    if(primaries == NULL && allocatePrimary == TRUE) {
        primaries = *result = prim;
        resultLength = UCOL_PRIMARY_MAX_BUFFER;
    }

    uint32_t secSize = UCOL_SECONDARY_MAX_BUFFER, terSize = UCOL_TERTIARY_MAX_BUFFER,
      caseSize = UCOL_CASE_MAX_BUFFER, quadSize = UCOL_QUAD_MAX_BUFFER;

    uint32_t sortKeySize = 1; /* it is always \0 terminated */

    UChar normBuffer[UCOL_NORMALIZATION_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = UCOL_NORMALIZATION_MAX_BUFFER;

    int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);

    uint8_t variableMax1 = coll->variableMax1;
    uint8_t variableMax2 = coll->variableMax2;
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)(variableMax1+1);
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    UColAttributeValue strength = coll->strength;

    uint8_t compareSec   = (uint8_t)((strength >= UCOL_SECONDARY)?0:0xFF);
    uint8_t compareTer   = (uint8_t)((strength >= UCOL_TERTIARY)?0:0xFF);
    uint8_t compareQuad  = (uint8_t)((strength >= UCOL_QUATERNARY)?0:0xFF);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    UBool  doCase = (coll->caseLevel == UCOL_ON);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);
    UBool  shifted = (coll->alternateHandling == UCOL_SHIFTED);
    UBool  qShifted = shifted && (compareQuad == 0);
    const uint8_t *scriptOrder = coll->scriptOrder;

    /* support for special features like caselevel and funky secondaries */
    uint8_t *frenchStartPtr = NULL;
    uint8_t *frenchEndPtr = NULL;
    uint32_t caseShift = 0;

    sortKeySize += ((compareSec?0:1) + (compareTer?0:1) + (doCase?1:0) + (qShifted?1:0)/*(compareQuad?0:1)*/ + (compareIdent?1:0));

    collIterate s;
    IInit_collIterate(coll, (UChar *)source, len, &s);

    /* If we need to normalize, we'll do it all at once at the beggining! */
    UColAttributeValue normMode = coll->normalizationMode;
    if(compareIdent) {
      if(unorm_quickCheck(source, len, UNORM_NFD, status) != UNORM_YES) {
        normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLen, status);
        if(U_FAILURE(*status)) {
            *status=U_ZERO_ERROR;
            normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
            normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, (normSourceLen+1), status);
        }
        normSource[normSourceLen] = 0;
        IInit_collIterate(coll, normSource, -1, &s);
        s.flags &= ~UCOL_ITER_NORM;
        len = normSourceLen;
      }
    } else if((normMode != UCOL_OFF)
      /* changed by synwee */
      && !checkFCD(source, len, status))
    {
        normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLen, status);
        if(U_FAILURE(*status)) {
            *status=U_ZERO_ERROR;
            normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
            normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, (normSourceLen+1), status);
        }
        normSource[normSourceLen] = 0;
        IInit_collIterate(coll, normSource, -1, &s);
        s.flags &= ~UCOL_ITER_NORM;
        len = normSourceLen;

    }

    if(resultLength == 0 || primaries == NULL) {
        return ucol_getSortKeySize(coll, &s, sortKeySize, strength, len);
    }
    uint8_t *primarySafeEnd = primaries + resultLength - 2;

    uint32_t minBufferSize = UCOL_MAX_BUFFER;

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;
    uint8_t *caseStart = cases;
    uint8_t *quadStart = quads;

    uint32_t order = 0;
    uint32_t ce = 0;

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    int32_t tertiaryAddition = coll->tertiaryAddition;
    uint8_t tertiaryTop = coll->tertiaryTop;
    uint8_t tertiaryCommon = coll->tertiaryCommon;
    uint8_t caseBits = 0;

    UBool finished = FALSE;
    UBool resultOverflow = FALSE;
    UBool wasShifted = FALSE;
    UBool notIsContinuation = FALSE;

    uint32_t prevBuffSize = 0;

    uint32_t count2 = 0, count3 = 0, count4 = 0;
    uint8_t leadPrimary = 0;

    for(;;) {
        for(i=prevBuffSize; i<minBufferSize; ++i) {

            order = ucol_IGetNextCE(coll, &s, status);
            // UCOL_GETNEXTCE(order, coll, s, status);

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            /* fix me... we should check if we're in continuation first */
            if(isCEIgnorable(order)) {
              continue;
            }

            /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
            ce = order;
            notIsContinuation = !isContinuation(ce);


            /*
            caseBit = (UBool)(order & UCOL_CASE_BIT_MASK);
            if(notIsContinuation) {
              tertiary = (uint8_t)((order & tertiaryMask));
            } else {
              tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
            }
            */
            if(notIsContinuation) {
              tertiary = (uint8_t)(order & UCOL_BYTE_SIZE_MASK);
            } else {
              tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
            }

            secondary = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary2 = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary1 = (uint8_t)(order >>= 8);

            if(notIsContinuation) {
              if(scriptOrder != NULL) {
                primary1 = scriptOrder[primary1];
              }
            }


            /* In the code below, every increase in any of buffers is followed by the increase to  */
            /* sortKeySize - this might look tedious, but it is needed so that we can find out if  */
            /* we're using too much space and need to reallocate the primary buffer or easily bail */
            /* out to ucol_getSortKeySizeNew.                                                      */

            if(shifted && ((notIsContinuation && primary1 <= variableMax1 && primary1 > 0
              && (primary1 < variableMax1 || primary1 == variableMax1 && primary2 < variableMax2))
              || (!notIsContinuation && wasShifted))) {
              if(count4 > 0) {
                while (count4 >= UCOL_BOT_COUNT4) {
                  *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                  count4 -= UCOL_BOT_COUNT4;
                }
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + count4);
                count4 = 0;
              }
              /* We are dealing with a variable and we're treating them as shifted */
              /* This is a shifted ignorable */
              if(primary1 != 0) {
                *quads++ = primary1;
              }
              if(primary2 != 0) {
                *quads++ = primary2;
              }
              wasShifted = TRUE;
            } else {
              wasShifted = FALSE;
              /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
              /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
#ifdef UCOL_PRIM_COMPRESSION
/* regular and simple sortkey calc */
              if(primary1 != UCOL_IGNORABLE) {
                if(notIsContinuation) {
                  if(leadPrimary == primary1) {
                    *primaries++ = primary2;
                  } else {
                    if(leadPrimary != 0) {
                      *primaries++ = (primary1 > leadPrimary) ? UCOL_BYTE_UNSHIFTED_MAX : UCOL_BYTE_UNSHIFTED_MIN;
                    }
                    if(primary2 == UCOL_IGNORABLE) {
                    /* one byter, not compressed */
                        *primaries++ = primary1;
                        leadPrimary = 0;
                    } else if(primary1<UCOL_BYTE_FIRST_NON_LATIN_PRIMARY ||
                        (primary1 > (UCOL_RESET_TOP_VALUE>>24) && primary1 < (UCOL_NEXT_TOP_VALUE>>24))) {
                    /* not compressible */
                        leadPrimary = 0;
                        *primaries++ = primary1;
                        *primaries++ = primary2;
                    } else { /* compress */
                        *primaries++ = leadPrimary = primary1;
                        *primaries++ = primary2;
                    }
                  }
                } else { /* we are in continuation, so we're gonna add primary to the key don't care about compression */
                  *primaries++ = primary1;
                  if(primary2 != UCOL_IGNORABLE) {
                    *primaries++ = primary2; /* second part */
                  }
                }
              }
#else
              if(primary1 != UCOL_IGNORABLE) {
                *primaries++ = primary1; /* scriptOrder[primary1]; */ /* This is the script ordering thingie */
                if(primary2 != UCOL_IGNORABLE) {
                  *primaries++ = primary2; /* second part */
                }
              }
#endif

            if(secondary > compareSec) {
              if(!isFrenchSec) {
                /* This is compression code. */
                if (secondary == UCOL_COMMON2 && notIsContinuation) {
                  ++count2;
                } else {
                  if (count2 > 0) {
                    if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                      while (count2 >= UCOL_TOP_COUNT2) {
                        *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2);
                        count2 -= (uint32_t)UCOL_TOP_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - count2);
                    } else {
                      while (count2 >= UCOL_BOT_COUNT2) {
                        *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
                        count2 -= (uint32_t)UCOL_BOT_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
                    }
                    count2 = 0;
                  }
                  *secondaries++ = secondary;
                }
              } else {
                  *secondaries++ = secondary;
                  /* Do the special handling for French secondaries */
                  /* We need to get continuation elements and do intermediate restore */
                  /* abc1c2c3de with french secondaries need to be edc1c2c3ba NOT edc3c2c1ba */
                  if(!notIsContinuation) {
                    if (frenchStartPtr == NULL) {
                      frenchStartPtr = secondaries - 2;
                    }
                    frenchEndPtr = secondaries-1;
                  } else if (frenchStartPtr != NULL) {
                      /* reverse secondaries from frenchStartPtr up to frenchEndPtr */
                    uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
                    frenchStartPtr = NULL;
                  }
                }
              }

              if(doCase) {
                doCaseShift(&cases, caseShift);
/*
                if (caseShift  == 0) {
                  *cases++ = UCOL_CASE_BYTE_START;
                  caseShift = UCOL_CASE_SHIFT_START;
                }
*/
                if(notIsContinuation) {
                  caseBits = (tertiary & 0xC0);

                  if(tertiary != 0) {
                    if(coll->caseFirst == UCOL_UPPER_FIRST) {
                      if((caseBits & 0xC0) == 0) {
                        *(cases-1) |= 1 << (--caseShift);
                      } else {
                        *(cases-1) |= 0 << (--caseShift);
                        /* second bit */
                        doCaseShift(&cases, caseShift);
                        *(cases-1) |= ((caseBits>>6)&1) << (--caseShift);
                      }
                    } else {
                      if((caseBits & 0xC0) == 0) {
                        *(cases-1) |= 0 << (--caseShift);
                      } else {
                        *(cases-1) |= 1 << (--caseShift);
                        /* second bit */
                        doCaseShift(&cases, caseShift);
                        *(cases-1) |= ((caseBits>>7)&1) << (--caseShift);
                      }
                    }
                  }

                }
              } else {
                if(notIsContinuation) {
                  tertiary ^= caseSwitch;
                }
              }

              tertiary &= tertiaryMask;
              if(tertiary > compareTer) {
                /* This is compression code. */
                /* sequence size check is included in the if clause */
                if (tertiary == tertiaryCommon && notIsContinuation) {
                  ++count3;
                } else {
                  if((tertiary > tertiaryCommon && tertiaryCommon == UCOL_COMMON3_NORMAL)
                    || (tertiary <= tertiaryCommon && tertiaryCommon == UCOL_COMMON3_UPPERFIRST)) {
                    tertiary += tertiaryAddition;
                  }
                  if (count3 > 0) {
                    if (tertiary > tertiaryCommon) {
                      while (count3 >= coll->tertiaryTopCount) {
                        *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
                        count3 -= (uint32_t)coll->tertiaryTopCount;
                      }
                      *tertiaries++ = (uint8_t)(tertiaryTop - count3);
                    } else {
                      while (count3 >= coll->tertiaryBottomCount) {
                        *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + coll->tertiaryBottomCount);
                        count3 -= (uint32_t)coll->tertiaryBottomCount;
                      }
                      *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
                    }
                    count3 = 0;
                  }
                  *tertiaries++ = tertiary;
                }
              }

              if(qShifted && notIsContinuation) {
                count4++;
              }
            }

            if(primaries > primarySafeEnd) { /* We have stepped over the primary buffer */
              int32_t sks = sortKeySize+(primaries - primStart)+(secondaries - secStart)+(tertiaries - terStart)+(cases-caseStart)+(quads-quadStart);
              if(allocatePrimary == FALSE) { /* need to save our butts if we cannot reallocate */
                resultOverflow = TRUE;
                sortKeySize = ucol_getSortKeySize(coll, &s, sks, strength, len);
                *status = U_MEMORY_ALLOCATION_ERROR;
                finished = TRUE;
                break;
              } else { /* It's much nicer if we can actually reallocate */
                primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sks, status);
                *result = primStart;
                primarySafeEnd = primStart + resultLength - 2;
              }
            }
        }
        if(finished) {
            break;
        } else {
          prevBuffSize = minBufferSize;
          secStart = reallocateBuffer(&secondaries, secStart, second, &secSize, 2*secSize, status);
          terStart = reallocateBuffer(&tertiaries, terStart, tert, &terSize, 2*terSize, status);
          caseStart = reallocateBuffer(&cases, caseStart, caseB, &caseSize, 2*caseSize, status);
          quadStart = reallocateBuffer(&quads, quadStart, quad, &quadSize, 2*quadSize, status);
          minBufferSize *= 2;
        }
    }

    /* Here, we are generally done with processing */
    /* bailing out would not be too productive */


    if(U_SUCCESS(*status)) {
      sortKeySize += (primaries - primStart);
      /* we have done all the CE's, now let's put them together to form a key */
      if(compareSec == 0) {
        if (count2 > 0) {
          while (count2 >= UCOL_BOT_COUNT2) {
            *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
            count2 -= (uint32_t)UCOL_BOT_COUNT2;
          }
          *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
        }
        *(primaries++) = UCOL_LEVELTERMINATOR;
        uint32_t secsize = secondaries-secStart;
        sortKeySize += secsize;
        if(sortKeySize <= resultLength) {
          if(isFrenchSec) { /* do the reverse copy */
            /* If there are any unresolved continuation secondaries, reverse them here so that we can reverse the whole secondary thing */
            if(frenchStartPtr != NULL) {
              uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
            }
            for(i = 0; i<secsize; i++) {
                *(primaries++) = *(secondaries-i-1);
            }
          } else {
            uprv_memcpy(primaries, secStart, secsize);
            primaries += secsize;
          }
        } else {
          if(allocatePrimary == TRUE) { /* need to save our butts if we cannot reallocate */
            primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
            *result = primStart;
            if(isFrenchSec) { /* do the reverse copy */
              /* If there are any unresolved continuation secondaries, reverse them here so that we can reverse the whole secondary thing */
              if(frenchStartPtr != NULL) {
                uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
              }
              for(i = 0; i<secsize; i++) {
                  *(primaries++) = *(secondaries-i-1);
              }
            } else {
              uprv_memcpy(primaries, secStart, secsize);
              primaries += secsize;
            }
          } else {
            *status = U_MEMORY_ALLOCATION_ERROR;
          }
        }
      }

      if(doCase) {
        uint32_t casesize = cases - caseStart;
        sortKeySize += casesize;
        *(primaries++) = UCOL_LEVELTERMINATOR;
        if(sortKeySize <= resultLength) {
          uprv_memcpy(primaries, caseStart, casesize);
          primaries += casesize;
        } else {
          if(allocatePrimary == TRUE) {
            primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
            *result = primStart;
            uprv_memcpy(primaries, caseStart, casesize);
          } else {
            *status = U_MEMORY_ALLOCATION_ERROR;
          }
        }
      }

      if(compareTer == 0) {
        if (count3 > 0) {
          if (coll->tertiaryCommon != UCOL_COMMON_BOT3) {
            while (count3 >= coll->tertiaryTopCount) {
              *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
              count3 -= (uint32_t)coll->tertiaryTopCount;
            }
            *tertiaries++ = (uint8_t)(tertiaryTop - count3);
          } else {
            while (count3 >= coll->tertiaryBottomCount) {
              *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + coll->tertiaryBottomCount);
              count3 -= (uint32_t)coll->tertiaryBottomCount;
            }
            *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
          }
        }
        uint32_t tersize = tertiaries - terStart;
        sortKeySize += tersize;
        *(primaries++) = UCOL_LEVELTERMINATOR;
        if(sortKeySize <= resultLength) {
          uprv_memcpy(primaries, terStart, tersize);
          primaries += tersize;
          if(/*compareQuad == 0*/qShifted == TRUE) {
              if(count4 > 0) {
                while (count4 >= UCOL_BOT_COUNT4) {
                  *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                  count4 -= UCOL_BOT_COUNT4;
                }
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + count4);
              }
              *(primaries++) = UCOL_LEVELTERMINATOR;
              uint32_t quadsize = quads - quadStart;
              sortKeySize += quadsize;
              if(sortKeySize <= resultLength) {
                uprv_memcpy(primaries, quadStart, quadsize);
                primaries += quadsize;
              } else {
                if(allocatePrimary == TRUE) {
                  primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
                  *result = primStart;
                  uprv_memcpy(primaries, quadStart, quadsize);
                } else {
                  *status = U_MEMORY_ALLOCATION_ERROR;
                }
              }
          }
        } else {
          if(allocatePrimary == TRUE) {
            primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
            *result = primStart;
            uprv_memcpy(primaries, terStart, tersize);
          } else {
            *status = U_MEMORY_ALLOCATION_ERROR;
          }
        }

        if(compareIdent) {
          const UChar *ident = s.string;
          *(primaries++) = UCOL_LEVELTERMINATOR;
          primarySafeEnd -= 2; /* since we can have up to four bytes encoded */
          /*
           * encode the code points of a string as
           * a sequence of byte-encoded differences (slope detection),
           * preserving lexical order
           */

          uint8_t *p0;
          int32_t c, prev;

          /*
           * Set prev to U+0050 so that an initial ASCII character needs only
           * a single byte, and initial characters up to Myanmar need only
           * two bytes.
           */
          prev=0x50;

          int32_t i=0;
          while(i<len) {
              p0=primaries;
              UTF_NEXT_CHAR(ident, i, len, c);
              primaries = writeDiff(c-prev, primaries);
              prev=c;
              if(primaries > primarySafeEnd) {
                if(allocatePrimary == TRUE) {
                  primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
                  *result = primStart;
                  primarySafeEnd = primaries + resultLength - 4;
                } else {
                  *status = U_MEMORY_ALLOCATION_ERROR;
                }
              }
              sortKeySize += primaries - p0;
          }
        }

      }
      *(primaries++) = '\0';
    }

    if(terStart != tert) {
        uprv_free(terStart);
        uprv_free(secStart);
        uprv_free(caseStart);
        uprv_free(quadStart);
    }

    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    if(allocatePrimary == TRUE) {
      *result = (uint8_t*)uprv_malloc(sortKeySize);
      uprv_memcpy(*result, primStart, sortKeySize);
      if(primStart != prim) {
        uprv_free(primStart);
      }
    }

    return sortKeySize;
}


int32_t
ucol_calcSortKeySimpleTertiary(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        uint32_t        resultLength,
        UBool allocatePrimary,
        UErrorCode *status)
{
    U_ALIGN_CODE(16);
    uint32_t i = 0; /* general purpose counter */

    /* Stack allocated buffers for buffers we use */
    uint8_t prim[UCOL_PRIMARY_MAX_BUFFER], second[UCOL_SECONDARY_MAX_BUFFER], tert[UCOL_TERTIARY_MAX_BUFFER];

    uint8_t *primaries = *result, *secondaries = second, *tertiaries = tert;

    if(U_FAILURE(*status)) {
      return 0;
    }

    if(primaries == NULL && allocatePrimary == TRUE) {
        primaries = *result = prim;
        resultLength = UCOL_PRIMARY_MAX_BUFFER;
    }

    uint32_t secSize = UCOL_SECONDARY_MAX_BUFFER, terSize = UCOL_TERTIARY_MAX_BUFFER;

    uint32_t sortKeySize = 3; /* it is always \0 terminated plus separators for secondary and tertiary */

    UChar normBuffer[UCOL_NORMALIZATION_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = UCOL_NORMALIZATION_MAX_BUFFER;

    int32_t len =  sourceLength;


    collIterate s;
    IInit_collIterate(coll, (UChar *)source, len, &s);

    /* If we need to normalize, we'll do it all at once at the beggining! */
    UColAttributeValue normMode = coll->normalizationMode;
    if(normMode != UCOL_OFF) {
        if (!checkFCD(source, len, status))
        {
            normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, normSourceLen, status);
            if(U_FAILURE(*status)) {
                *status=U_ZERO_ERROR;
                normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
                normSourceLen = unorm_normalize(source, sourceLength, UNORM_NFD, 0, normSource, (normSourceLen+1), status);
            }
            normSource[normSourceLen] = 0;
            IInit_collIterate(coll, normSource, -1, &s);
            s.flags &= ~(UCOL_ITER_NORM);
            len = normSourceLen;
        }
    }


    if(resultLength == 0 || primaries == NULL) {
        return ucol_getSortKeySize(coll, &s, sortKeySize, coll->strength, len);
    }
    uint8_t *primarySafeEnd = primaries + resultLength - 2;

    uint32_t minBufferSize = UCOL_MAX_BUFFER;

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;

    uint32_t order = 0;
    uint32_t ce = 0;

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    int8_t tertiaryAddition = coll->tertiaryAddition;
    uint8_t tertiaryTop = coll->tertiaryTop;
    uint8_t tertiaryCommon = coll->tertiaryCommon;

    uint32_t prevBuffSize = 0;

    UBool finished = FALSE;
    UBool resultOverflow = FALSE;
    UBool notIsContinuation = FALSE;

    uint32_t count2 = 0, count3 = 0;
    uint8_t leadPrimary = 0;

    for(;;) {
        for(i=prevBuffSize; i<minBufferSize; ++i) {

            order = ucol_IGetNextCE(coll, &s, status);

            if(isCEIgnorable(order)) {
              continue;
            }

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            /* We're saving order in ce, since we will destroy order in order to get primary, secondary, tertiary in order ;)*/
            ce = order;
            notIsContinuation = !isContinuation(ce);

            if(notIsContinuation) {
              tertiary = (uint8_t)((order & tertiaryMask));
            } else {
              tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
            }
            secondary = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary2 = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary1 = (uint8_t)(order >>= 8);

            /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
            /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
            /* be zero with non zero primary1. primary3 is different than 0 only for long primaries - see above.               */
#ifdef UCOL_PRIM_COMPRESSION
/* regular and simple sortkey calc */
            if(primary1 != UCOL_IGNORABLE) {
              if(notIsContinuation) {
                if(leadPrimary == primary1) {
                  *primaries++ = primary2;
                } else {
                  if(leadPrimary != 0) {
                    *primaries++ = (primary1 > leadPrimary) ? UCOL_BYTE_UNSHIFTED_MAX : UCOL_BYTE_UNSHIFTED_MIN;
                  }
                  if(primary2 == UCOL_IGNORABLE) {
                  /* one byter, not compressed */
                      *primaries++ = primary1;
                      leadPrimary = 0;
                  } else if(primary1<UCOL_BYTE_FIRST_NON_LATIN_PRIMARY ||
                      (primary1 > (UCOL_RESET_TOP_VALUE>>24) && primary1 < (UCOL_NEXT_TOP_VALUE>>24))) {
                  /* not compressible */
                      leadPrimary = 0;
                      *primaries++ = primary1;
                      *primaries++ = primary2;
                  } else { /* compress */
                      *primaries++ = leadPrimary = primary1;
                      *primaries++ = primary2;
                  }
                }
              } else { /* we are in continuation, so we're gonna add primary to the key don't care about compression */
                *primaries++ = primary1;
                if(primary2 != UCOL_IGNORABLE) {
                  *primaries++ = primary2; /* second part */
                }
              }
            }
#else
            if(primary1 != UCOL_IGNORABLE) {
              *primaries++ = primary1; /* scriptOrder[primary1]; */ /* This is the script ordering thingie */
              if(primary2 != UCOL_IGNORABLE) {
                *primaries++ = primary2; /* second part */
              }
            }
#endif

              if(secondary > 0) { /* I think that != 0 test should be != IGNORABLE */
              /* This is compression code. */
              if (secondary == UCOL_COMMON2 && notIsContinuation) {
                ++count2;
              } else {
                if (count2 > 0) {
                  if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                    while (count2 >= UCOL_TOP_COUNT2) {
                      *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2);
                      count2 -= (uint32_t)UCOL_TOP_COUNT2;
                    }
                    *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - count2);
                  } else {
                    while (count2 >= UCOL_BOT_COUNT2) {
                      *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
                      count2 -= (uint32_t)UCOL_BOT_COUNT2;
                    }
                    *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
                  }
                  count2 = 0;
                }
                *secondaries++ = secondary;
              }
            }

            if(notIsContinuation) {
              tertiary ^= caseSwitch;
            }

              if(tertiary > 0) {
              /* This is compression code. */
              /* sequence size check is included in the if clause */
              if (tertiary == tertiaryCommon && notIsContinuation) {
                ++count3;
              } else {
                if((tertiary > tertiaryCommon && tertiaryCommon == UCOL_COMMON3_NORMAL)
                  || (tertiary <= tertiaryCommon && tertiaryCommon == UCOL_COMMON3_UPPERFIRST)) {
                  tertiary += tertiaryAddition;
                }
                if (count3 > 0) {
                  if (tertiary > tertiaryCommon) {
                    while (count3 >= coll->tertiaryTopCount) {
                      *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
                      count3 -= (uint32_t)coll->tertiaryTopCount;
                    }
                    *tertiaries++ = (uint8_t)(tertiaryTop - count3);
                  } else {
                    while (count3 >= coll->tertiaryBottomCount) {
                      *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + coll->tertiaryBottomCount);
                      count3 -= (uint32_t)coll->tertiaryBottomCount;
                    }
                    *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
                  }
                  count3 = 0;
                }
                *tertiaries++ = tertiary;
              }
            }

            if(primaries > primarySafeEnd) { /* We have stepped over the primary buffer */
              int32_t sks = sortKeySize+(primaries - primStart)+(secondaries - secStart)+(tertiaries - terStart);
              if(allocatePrimary == FALSE) { /* need to save our butts if we cannot reallocate */
                resultOverflow = TRUE;
                sortKeySize = ucol_getSortKeySize(coll, &s, sks, coll->strength, len);
                *status = U_MEMORY_ALLOCATION_ERROR;
                finished = TRUE;
                break;
              } else { /* It's much nicer if we can actually reallocate */
                primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sks, status);
                *result = primStart;
                primarySafeEnd = primStart + resultLength - 2;
              }
            }
        }
        if(finished) {
            break;
        } else {
          prevBuffSize = minBufferSize;
          secStart = reallocateBuffer(&secondaries, secStart, second, &secSize, 2*secSize, status);
          terStart = reallocateBuffer(&tertiaries, terStart, tert, &terSize, 2*terSize, status);
          minBufferSize *= 2;
        }
    }

    if(U_SUCCESS(*status)) {
      sortKeySize += (primaries - primStart);
      /* we have done all the CE's, now let's put them together to form a key */
      if (count2 > 0) {
        while (count2 >= UCOL_BOT_COUNT2) {
          *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
          count2 -= (uint32_t)UCOL_BOT_COUNT2;
        }
        *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + count2);
      }
      uint32_t secsize = secondaries-secStart;
      sortKeySize += secsize;
      if(sortKeySize <= resultLength) {
        *(primaries++) = UCOL_LEVELTERMINATOR;
        uprv_memcpy(primaries, secStart, secsize);
        primaries += secsize;
      } else {
        if(allocatePrimary == TRUE) {
          primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
          *result = primStart;
          uprv_memcpy(primaries, secStart, secsize);
        } else {
          *status = U_MEMORY_ALLOCATION_ERROR;
        }
      }

      if (count3 > 0) {
        if (coll->tertiaryCommon != UCOL_COMMON_BOT3) {
          while (count3 >= coll->tertiaryTopCount) {
            *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
            count3 -= (uint32_t)coll->tertiaryTopCount;
          }
          *tertiaries++ = (uint8_t)(tertiaryTop - count3);
        } else {
          while (count3 >= coll->tertiaryBottomCount) {
            *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + coll->tertiaryBottomCount);
            count3 -= (uint32_t)coll->tertiaryBottomCount;
          }
          *tertiaries++ = (uint8_t)(UCOL_COMMON_BOT3 + count3);
        }
      }
      *(primaries++) = UCOL_LEVELTERMINATOR;
      uint32_t tersize = tertiaries - terStart;
      sortKeySize += tersize;
      if(sortKeySize <= resultLength) {
        uprv_memcpy(primaries, terStart, tersize);
        primaries += tersize;
      } else {
        if(allocatePrimary == TRUE) {
          primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
          *result = primStart;
          uprv_memcpy(primaries, terStart, tersize);
        } else {
          *status = U_MEMORY_ALLOCATION_ERROR;
        }
      }

      *(primaries++) = '\0';
    }

    if(terStart != tert) {
        uprv_free(terStart);
        uprv_free(secStart);
    }

    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    if(allocatePrimary == TRUE) {
      *result = (uint8_t*)uprv_malloc(sortKeySize);
      uprv_memcpy(*result, primStart, sortKeySize);
      if(primStart != prim) {
        uprv_free(primStart);
      }
    }

    return sortKeySize;
}

/* this function makes a string with representation of a sortkey */
U_CAPI char U_EXPORT2 *ucol_sortKeyToString(const UCollator *coll, const uint8_t *sortkey, char *buffer, uint32_t *len) {
  int32_t strength = UCOL_PRIMARY;
  uint32_t res_size = 0;
  UBool doneCase = FALSE;

  char *current = buffer;
  const uint8_t *currentSk = sortkey;

  sprintf(current, "[");
  current++;

  while(strength <= UCOL_QUATERNARY && strength <= coll->strength) {
    if(strength > UCOL_PRIMARY) {
      sprintf(current, " . ");
      current += 3;
    }
    while(*currentSk != 0x01 && *currentSk != 0x00) { /* print a level */
      sprintf(current, "%02X ", *currentSk++);
      current+=3;
    }
    if(coll->caseLevel == UCOL_ON && strength == UCOL_SECONDARY && doneCase == FALSE) {
        doneCase = TRUE;
    } else if(coll->caseLevel == UCOL_OFF || doneCase == TRUE || strength != UCOL_SECONDARY) {
      strength ++;
    }
    sprintf(current, "%02X", *(currentSk++)); /* This should print '01' */
    current +=2;
    if(strength == UCOL_QUATERNARY && coll->alternateHandling == UCOL_NON_IGNORABLE) {
      break;
    }
  }

  if(coll->strength == UCOL_IDENTICAL) {
    sprintf(current, " . ");
    current += 3;
    while(*currentSk != 0) {
      if(*currentSk == 0x01) {
        sprintf(current, "%02X", *(currentSk++));
        current +=2;
      }

      sprintf(current, "%02X%02X ", *currentSk, *(currentSk+1));
      current +=5;
      currentSk+=2;
    }

  sprintf(current, "%02X", *(currentSk++)); /* This should print '00' */
  current += 2;

  }
  sprintf(current, "]");
  current += 3;

  if(res_size > *len) {
    return NULL;
  }

  return buffer;


}


/****************************************************************************/
/* Following are the functions that deal with the properties of a collator  */
/* there are new APIs and some compatibility APIs                           */
/****************************************************************************/
void ucol_updateInternalState(UCollator *coll) {
/*
      uint32_t variableMaxCE = ucmp32_get(coll->mapping, coll->variableTopValue);
      coll->variableMax1 = (uint8_t)((variableMaxCE & 0xFF000000) >> 24);
      coll->variableMax2 = (uint8_t)((variableMaxCE & 0x00FF0000) >> 16);
*/
      coll->variableMax1 = (uint8_t)((coll->variableTopValue & 0xFF00) >> 8);
      coll->variableMax2 = (uint8_t)((coll->variableTopValue & 0x00FF));

      /* Set the compression values */
      uint8_t tertiaryTotal = coll->tertiaryTop - UCOL_COMMON_BOT3-1;
      coll->tertiaryTopCount = UCOL_PROPORTION3*tertiaryTotal;
      coll->tertiaryBottomCount = tertiaryTotal - coll->tertiaryTopCount;

      if(coll->caseFirst == UCOL_UPPER_FIRST) {
        coll->caseSwitch = UCOL_CASE_SWITCH;
      } else {
        coll->caseSwitch = UCOL_NO_CASE_SWITCH;
      }

      if(coll->caseLevel == UCOL_ON || coll->caseFirst == UCOL_OFF) {
        coll->tertiaryMask = UCOL_REMOVE_CASE;
        coll->tertiaryCommon = UCOL_COMMON3_NORMAL;
        coll->tertiaryAddition = UCOL_FLAG_BIT_MASK_CASE_SW_OFF;
        coll->tertiaryTop = UCOL_COMMON_TOP3_CASE_SW_OFF;
      } else {
        coll->tertiaryMask = UCOL_KEEP_CASE;
        if(coll->caseFirst == UCOL_UPPER_FIRST) {
          coll->tertiaryCommon = UCOL_COMMON3_UPPERFIRST;
          coll->tertiaryAddition = -UCOL_FLAG_BIT_MASK_CASE_SW_ON;
          coll->tertiaryTop = UCOL_COMMON_TOP3_CASE_SW_ON;
        } else {
          coll->tertiaryCommon = UCOL_COMMON3_NORMAL;
          coll->tertiaryAddition = UCOL_FLAG_BIT_MASK_CASE_SW_ON;
          coll->tertiaryTop = UCOL_COMMON_TOP3_CASE_SW_ON;
        }
      }
      if(coll->caseLevel == UCOL_OFF && coll->strength == UCOL_TERTIARY
        && coll->frenchCollation == UCOL_OFF && coll->alternateHandling == UCOL_NON_IGNORABLE) {
        coll->sortKeyGen = ucol_calcSortKeySimpleTertiary;
      } else {
        coll->sortKeyGen = ucol_calcSortKey;
      }

}

/* Attribute setter API */
U_CAPI void ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status) {
    switch(attr) {
    case UCOL_FRENCH_COLLATION: /* attribute for direction of secondary weights*/
        if(value == UCOL_ON) {
            coll->frenchCollation = UCOL_ON;
            coll->frenchCollationisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->frenchCollation = UCOL_OFF;
            coll->frenchCollationisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->frenchCollationisDefault = TRUE;
            coll->frenchCollation = coll->options->frenchCollation;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_ALTERNATE_HANDLING: /* attribute for handling variable elements*/
        if(value == UCOL_SHIFTED) {
            coll->alternateHandling = UCOL_SHIFTED;
            coll->alternateHandlingisDefault = FALSE;
        } else if (value == UCOL_NON_IGNORABLE) {
            coll->alternateHandling = UCOL_NON_IGNORABLE;
            coll->alternateHandlingisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->alternateHandlingisDefault = TRUE;
            coll->alternateHandling = coll->options->alternateHandling ;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_CASE_FIRST: /* who goes first, lower case or uppercase */
        if(value == UCOL_LOWER_FIRST) {
            coll->caseFirst = UCOL_LOWER_FIRST;
            coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_UPPER_FIRST) {
            coll->caseFirst = UCOL_UPPER_FIRST;
            coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_OFF) {
          coll->caseFirst = UCOL_OFF;
          coll->caseFirstisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->caseFirst = coll->options->caseFirst;
            coll->caseFirstisDefault = TRUE;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_CASE_LEVEL: /* do we have an extra case level */
        if(value == UCOL_ON) {
            coll->caseLevel = UCOL_ON;
            coll->caseLevelisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->caseLevel = UCOL_OFF;
            coll->caseLevelisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->caseLevel = coll->options->caseLevel;
            coll->caseLevelisDefault = TRUE;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_NORMALIZATION_MODE: /* attribute for normalization */
        if(value == UCOL_ON) {
            coll->normalizationMode = UCOL_ON;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_OFF) {
            coll->normalizationMode = UCOL_OFF;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_ON_WITHOUT_HANGUL) {
            coll->normalizationMode = UCOL_ON_WITHOUT_HANGUL ;
            coll->normalizationModeisDefault = FALSE;
        } else if (value == UCOL_DEFAULT) {
            coll->normalizationModeisDefault = TRUE;
            coll->normalizationMode = coll->options->normalizationMode;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_STRENGTH:         /* attribute for strength */
        if (value == UCOL_DEFAULT) {
            coll->strengthisDefault = TRUE;
            coll->strength = coll->options->strength;
        } else if (value <= UCOL_IDENTICAL) {
            coll->strengthisDefault = FALSE;
            coll->strength = value;
        } else {
            *status = U_ILLEGAL_ARGUMENT_ERROR  ;
        }
        break;
    case UCOL_ATTRIBUTE_COUNT:
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        break;
    }
    ucol_updateInternalState(coll);
}

U_CAPI UColAttributeValue ucol_getAttribute(const UCollator *coll, UColAttribute attr, UErrorCode *status) {
    switch(attr) {
    case UCOL_FRENCH_COLLATION: /* attribute for direction of secondary weights*/
        return coll->frenchCollation;
    case UCOL_ALTERNATE_HANDLING: /* attribute for handling variable elements*/
        return coll->alternateHandling;
    case UCOL_CASE_FIRST: /* who goes first, lower case or uppercase */
        return coll->caseFirst;
    case UCOL_CASE_LEVEL: /* do we have an extra case level */
        return coll->caseLevel;
    case UCOL_NORMALIZATION_MODE: /* attribute for normalization */
        return coll->normalizationMode;
    case UCOL_STRENGTH:         /* attribute for strength */
        return coll->strength;
    case UCOL_ATTRIBUTE_COUNT:
    default:
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        break;
    }
    return UCOL_DEFAULT;
}

U_CAPI void
ucol_setNormalization(  UCollator            *coll,
            UNormalizationMode    mode)
{
  UErrorCode status = U_ZERO_ERROR;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
    break;
  case UCOL_DECOMP_CAN:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    break;
  default:
    /* Shouldn't get here. */
    /* This is quite a bad API */
    /* deprecate */
    /* *status = U_ILLEGAL_ARGUMENT_ERROR; */
    return;
  }
}

U_CAPI UNormalizationMode
ucol_getNormalization(const UCollator* coll)
{
  UErrorCode status = U_ZERO_ERROR;
  if(ucol_getAttribute(coll, UCOL_NORMALIZATION_MODE, &status) == UCOL_ON) {
    return UNORM_NFD;
  } else {
    return UNORM_NONE;
  }
}

U_CAPI void
ucol_setStrength(    UCollator                *coll,
            UCollationStrength        strength)
{
  UErrorCode status = U_ZERO_ERROR;
  ucol_setAttribute(coll, UCOL_STRENGTH, strength, &status);
}

U_CAPI UCollationStrength
ucol_getStrength(const UCollator *coll)
{
  UErrorCode status = U_ZERO_ERROR;
  return ucol_getAttribute(coll, UCOL_STRENGTH, &status);
}

/****************************************************************************/
/* Following are misc functions                                             */
/* there are new APIs and some compatibility APIs                           */
/****************************************************************************/

U_CAPI UCollator *
ucol_safeClone(const UCollator *coll, void *stackBuffer, int32_t * pBufferSize, UErrorCode *status)
{
    UCollator * localCollator;
    int32_t bufferSizeNeeded = sizeof(UCollator);

    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }
    if (!pBufferSize || !coll){
       *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if (*pBufferSize == 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize =  bufferSizeNeeded;
        return 0;
    }
    if (*pBufferSize < bufferSizeNeeded || stackBuffer == NULL) {
        /* allocate one here...*/
        int32_t length;
        const UChar * rules = ucol_getRules(coll, &length);

        localCollator = ucol_openRules(rules,
                                       length,
                                       ucol_getNormalization(coll),
                                       ucol_getStrength(coll),
                                       status);
        if (U_SUCCESS(*status))
        {
            *status = U_SAFECLONE_ALLOCATED_ERROR;
        }
    } else {
        localCollator = (UCollator *)stackBuffer;
        memcpy(localCollator, coll, sizeof(UCollator));
        localCollator->freeOnClose = FALSE;
    }
    return localCollator;
}

U_CAPI int32_t
ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen) {
  int32_t len = 0;
  int32_t UCAlen = 0;
  const UChar* ucaRules = 0;
  const UChar *rules = ucol_getRules(coll, &len);
  if(delta == UCOL_FULL_RULES) {
    UErrorCode status = U_ZERO_ERROR;
    /* take the UCA rules and append real rules at the end */
    /* UCA rules will be probably coming from the root RB */
    ucaRules = ures_getStringByKey(coll->rb,"%%UCARULES",&UCAlen,&status);
  }
  if(buffer){
      *buffer=0;
      if(bufferLen >= len + UCAlen) {
        u_strcat(buffer, rules);
        if(UCAlen >0)
            u_strcat(buffer,ucaRules);
      } else {
        u_strncat(buffer, rules, (bufferLen-UCAlen)*sizeof(UChar));
      }
  }
  return len+UCAlen;
}

U_CAPI const UChar*
ucol_getRules(    const    UCollator       *coll,
        int32_t            *length)
{
  if(coll->rules != NULL) {
    *length = u_strlen(coll->rules);
    return coll->rules;
  } else {
    UErrorCode status = U_ZERO_ERROR;
    if(coll->rb != NULL) {
      UResourceBundle *collElem = ures_getByKey(coll->rb, "CollationElements", NULL, &status);
      if(U_SUCCESS(status)) {
        /*Semantic const */
        ((UCollator *)coll)->rules = ures_getStringByKey(collElem, "Sequence", length, &status);
        ((UCollator *)coll)->freeRulesOnClose = FALSE;
        ures_close(collElem);
        return coll->rules;
      }
    }
    *length = 0;
    return &coll->zero;
  }
}

U_CAPI int32_t
ucol_getDisplayName(    const    char        *objLoc,
            const    char        *dispLoc,
            UChar             *result,
            int32_t         resultLength,
            UErrorCode        *status)
{
  if(U_FAILURE(*status)) return -1;
  UnicodeString dst(result, resultLength, resultLength);
  Collator::getDisplayName(Locale(objLoc), Locale(dispLoc), dst);
  return uprv_fillOutputString(dst, result, resultLength, status);
}

U_CAPI const char*
ucol_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t
ucol_countAvailable()
{
  return uloc_countAvailable();
}

U_CAPI void
ucol_getVersion(const UCollator* coll,
                UVersionInfo versionInfo)
{
    /* RunTime version  */
    uint8_t rtVersion = UCOL_RUNTIME_VERSION;
    /* Builder version*/
    uint8_t bdVersion = coll->dataInfo.dataVersion[0];

    /* Charset Version. Need to get the version from cnv files
     * makeconv should populate cnv files with version and
     * an api has to be provided in ucnv.h to obtain this version
     */
    uint8_t csVersion = 0;

    /* combine the version info */
    uint16_t cmbVersion = (uint16_t)((rtVersion<<11) | (bdVersion<<6) | (csVersion));

    /* Tailoring rules */
    versionInfo[0] = (uint8_t)(cmbVersion>>8);
    versionInfo[1] = (uint8_t)cmbVersion;
    versionInfo[2] = coll->dataInfo.dataVersion[1];
    versionInfo[3] = UCA->dataInfo.dataVersion[1];
}


/* This internal API checks whether a character is tailored or not */
U_CAPI UBool isTailored(const UCollator *coll, const UChar u, UErrorCode *status) {
  uint32_t CE = UCOL_NOT_FOUND;
  const UChar *ContractionStart = NULL;
  if(U_SUCCESS(*status) && coll != NULL) {
    if(coll == UCA) {
      return FALSE;
    } else if(u < 0x100) { /* latin-1 */
      CE = coll->latinOneMapping[u];
      if(CE == UCA->latinOneMapping[u]) {
        return FALSE;
      }
    } else { /* regular */
      CE = ucmp32_get(coll->mapping, u);
    }

    if(isContraction(CE)) {
      ContractionStart = (UChar *)coll->image+getContractOffset(CE);
      CE = *(coll->contractionCEs + (ContractionStart- coll->contractionIndex));
    }

    if(CE == UCOL_NOT_FOUND) {
      return FALSE;
    } else {
      return TRUE;
    }
  } else {
    return FALSE;
  }
}



/****************************************************************************/
/* Following are the string compare functions                               */
/*                                                                          */
/****************************************************************************/


/*  ucol_checkIdent    internal function.  Does byte level string compare.   */
/*                     Used by strcoll if strength == identical and strings  */
/*                     are otherwise equal.  Moved out-of-line because this  */
/*                     is a rare case.                                       */
/*                                                                           */
/*                     Comparison must be done on NFD normalized strings.    */
/*                     FCD is not good enough.                               */
/*                                                                           */
/*      TODO:  make an incremental NFD Comparison function, which could      */
/*             be of general use                                             */

UCollationResult    ucol_checkIdent(collIterate *sColl, collIterate *tColl, UBool normalize)
{
    int32_t            comparison;
    uint32_t          sLen        = (sColl->flags & UCOL_ITER_HASLEN) ? sColl->endp - sColl->string : -1;
    UChar            *sBuf        = sColl->string;

    uint32_t          tLen        = (tColl->flags & UCOL_ITER_HASLEN) ? tColl->endp - tColl->string : -1;
    UChar            *tBuf        = tColl->string;
//    uint32_t          compLen     = 0;
    uint32_t          normLength;
    UErrorCode        status      = U_ZERO_ERROR;
    UCollationResult  result;
    UBool             sAlloc      = FALSE;
    UBool             tAlloc      = FALSE;

    if (normalize) {
        if (unorm_quickCheck(sColl->string, sLen, UNORM_NFD, &status) != UNORM_YES) {
            sBuf = sColl->writableBuffer;
            normLength = unorm_normalize(sColl->string, sLen, UNORM_NFD, 0,
                sBuf, UCOL_WRITABLE_BUFFER_SIZE, &status);
            if (U_FAILURE(status)) {  /*this would be buffer overflow  */
                sBuf = (UChar *)uprv_malloc((normLength+1)*sizeof(UChar));
                sAlloc = TRUE;
                status = U_ZERO_ERROR;
                normLength = unorm_normalize(sColl->string, sLen, UNORM_NFD, 0, sBuf, normLength+1, &status);
            }
            sLen = normLength;
        }

        status = U_ZERO_ERROR;
        if (unorm_quickCheck(tColl->string, tLen, UNORM_NFD, &status) != UNORM_YES) {
            tBuf = tColl->writableBuffer;
            normLength = unorm_normalize(tColl->string, tLen, UNORM_NFD, 0,
                tBuf, UCOL_WRITABLE_BUFFER_SIZE, &status);
            if (U_FAILURE(status)) {  /*this would be buffer overflow  */
                tBuf = (UChar *)uprv_malloc((normLength+1)*sizeof(UChar));
                tAlloc = TRUE;
                status = U_ZERO_ERROR;
                normLength = unorm_normalize(tColl->string, tLen, UNORM_NFD, 0, tBuf, normLength+1, &status);
            }
            tLen = normLength;
        }

    }

    if (sLen == -1 && tLen == -1) {
        comparison = u_strcmp(sBuf, tBuf);
    }
    else
    {
        if (sLen == -1) {
            sLen = u_strlen(sBuf);
        }
        if (tLen == -1) {
            tLen = u_strlen(tBuf);
        }
        comparison = u_strncmp(sBuf, tBuf, uprv_min(sLen, tLen));
    }

    result = UCOL_LESS;
    if (comparison > 0) {
        result = UCOL_GREATER;
    }
    else if (comparison == 0) {
        if(sLen > tLen) {
            result = UCOL_GREATER;
        } else if (sLen == tLen){
            result = UCOL_EQUAL;
        }
    }

    if (sAlloc) {
        delete sBuf;
    }
    if (tAlloc) {
        delete tBuf;
    }

    return result;
}

/*  CEBuf - A struct and some inline functions to handle the saving    */
/*          of CEs in a buffer within ucol_strcoll                     */

#define UCOL_CEBUF_SIZE 512
typedef struct ucol_CEBuf {
    uint32_t    *buf;
    uint32_t    *endp;
    uint32_t    *pos;
    uint32_t     localArray[UCOL_CEBUF_SIZE];
} ucol_CEBuf;


inline void UCOL_INIT_CEBUF(ucol_CEBuf *b) {
    (b)->buf = (b)->pos = (b)->localArray;
    (b)->endp = (b)->buf + UCOL_CEBUF_SIZE;
};

void ucol_CEBuf_Expand(ucol_CEBuf *b, collIterate *ci) {
    uint32_t  oldSize;
    uint32_t  newSize;
    uint32_t  *newBuf;

    ci->flags |= UCOL_ITER_ALLOCATED;
    oldSize = b->pos - b->buf;
    newSize = oldSize * 2;
    newBuf = (uint32_t *)uprv_malloc(newSize * sizeof(uint32_t));
    uprv_memcpy(newBuf, b->buf, oldSize * sizeof(uint32_t));
    if (b->buf != b->localArray) {
        delete b->buf;
    }
    b->buf = newBuf;
    b->endp = b->buf + newSize;
    b->pos  = b->buf + oldSize;
}

inline void UCOL_CEBUF_CHECK(ucol_CEBuf *b, collIterate *ci) {
    if ((b)->pos == (b)->endp) ucol_CEBuf_Expand(b, ci);
}

inline void UCOL_CEBUF_PUT(ucol_CEBuf *b, uint32_t ce) {
    *(b)->pos++ = ce;
};



/*                                                                      */
/* ucol_strcoll     Main public API string comparison function          */
/*                                                                      */
U_CAPI UCollationResult
ucol_strcoll( const UCollator    *coll,
              const UChar        *source,
              int32_t            sourceLength,
              const UChar        *target,
              int32_t            targetLength)
{
    U_ALIGN_CODE(16);

    /* Scan the strings.  Find:                                                             */
    /*    The length of any leading portion that is equal                                   */
    /*    Whether they are exactly equal.  (in which case we just return)                   */
    const UChar    *pSrc    = source;
    const UChar    *pTarg   = target;
    int32_t        equalLength;

    if (sourceLength == -1 && targetLength == -1) {
        // Both strings are null terminated.
        //    Check for them being the same string, and scan through
        //    any leading equal portion.
        if (source==target) {
            return UCOL_EQUAL;
        }

        for (;;) {
            if ( *pSrc != *pTarg || *pSrc == 0) {
                break;
            }
            pSrc++;
            pTarg++;
        }
        if (*pSrc == 0 && *pTarg == 0) {
            return UCOL_EQUAL;
        }
        equalLength = pSrc - source;
    }
    else
    {
        // One or both strings has an explicit length.
        /* check if source and target are same strings */

        if (source==target  && sourceLength==targetLength) {
            return UCOL_EQUAL;
        }
        const UChar    *pSrcEnd = source + sourceLength;
        const UChar    *pTargEnd = target + targetLength;


        // Scan while the strings are bitwise ==, or until one is exhausted.
            for (;;) {
                if (pSrc == pSrcEnd || pTarg == pTargEnd) {
                    break;
                }
                if ((*pSrc == 0 && sourceLength == -1) || (*pTarg == 0 && targetLength == -1)) {
                    break;
                }
                if (*pSrc != *pTarg) {
                    break;
                }
                pSrc++;
                pTarg++;
            }
            equalLength = pSrc - source;

            // If we made it all the way through both strings, we are done.  They are ==
            if ((pSrc ==pSrcEnd  || (pSrcEnd <pSrc  && *pSrc==0))  &&   /* At end of src string, however it was specified. */
                (pTarg==pTargEnd || (pTargEnd<pTarg && *pTarg==0)))  {  /* and also at end of dest string                  */
                return UCOL_EQUAL;
            }
    }
    if (equalLength > 0) {
        /* There is an identical portion at the beginning of the two strings.        */
        /*   If the identical portion ends within a contraction or a comibining      */
        /*   character sequence, back up to the start of that sequence.              */
        pSrc  = source + equalLength;        /* point to the first differing chars   */
        pTarg = target + equalLength;
        if (pSrc  != source+sourceLength && ucol_unsafeCP(*pSrc, coll) ||
            pTarg != target+targetLength && ucol_unsafeCP(*pTarg, coll))
        {
            // We are stopped in the middle of a contraction.
            // Scan backwards through the == part of the string looking for the start of the contraction.
            //   It doesn't matter which string we scan, since they are the same in this region.
            do
            {
                equalLength--;
                pSrc--;
            }
            while (equalLength>0 && ucol_unsafeCP(*pSrc, coll));
        }

        source += equalLength;
        target += equalLength;
        if (sourceLength > 0) {
            sourceLength -= equalLength;
        }
        if (targetLength > 0) {
            targetLength -= equalLength;
        }
    }



    UColAttributeValue strength = coll->strength;
    UBool initialCheckSecTer = (strength  >= UCOL_SECONDARY);

    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = (strength  >= UCOL_TERTIARY);
    UBool checkQuad = (strength  >= UCOL_QUATERNARY);
    UBool checkIdent = (strength == UCOL_IDENTICAL);
    UBool checkCase = (coll->caseLevel == UCOL_ON);
    UBool isFrenchSec = (coll->frenchCollation == UCOL_ON) && checkSecTer;
    UBool shifted = (coll->alternateHandling == UCOL_SHIFTED);
    UBool qShifted = shifted && checkQuad;

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    collIterate sColl, tColl;


    IInit_collIterate(coll, source, sourceLength, &sColl);
    IInit_collIterate(coll, target, targetLength, &tColl);

/*
    uint32_t sCEsArray[512], tCEsArray[512];
    uint32_t *sCEs = sCEsArray, *tCEs = tCEsArray;
    uint32_t *sCEend = sCEs+512, *tCEend = tCEs+512;
    */

    ucol_CEBuf   sCEs;
    ucol_CEBuf   tCEs;
    UCOL_INIT_CEBUF(&sCEs);
    UCOL_INIT_CEBUF(&tCEs);

    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    uint32_t LVT = (shifted)?((coll->variableMax1)<<24 | (coll->variableMax2)<<16):0;

    uint32_t secS = 0, secT = 0;

    uint32_t sOrder=0, tOrder=0;
    if(!shifted) {
      for(;;) {
          // TODO:  Verify that at most one CE an be added per buf per time through here.
        UCOL_CEBUF_CHECK(&sCEs , &sColl);
        UCOL_CEBUF_CHECK(&sCEs , &sColl);

        /* Get the next collation element in each of the strings, unless */
        /* we've been requested to skip it. */
        while(sOrder == 0) {
          // UCOL_GETNEXTCE(sOrder, coll, sColl, &status);
          sOrder = ucol_IGetNextCE(coll, &sColl, &status);
          //if(!isContinuation(sOrder)) {
          //  sOrder ^= caseSwitch;
          //}
          // *(sCEs++) = sOrder;
          UCOL_CEBUF_PUT(&sCEs, sOrder);
          sOrder &= 0xFFFF0000;
        }

        while(tOrder == 0) {
          // UCOL_GETNEXTCE(tOrder, coll, tColl, &status);
          tOrder = ucol_IGetNextCE(coll, &tColl, &status);
          //if(!isContinuation(tOrder)) {
          //  tOrder ^= caseSwitch;
          //}
          UCOL_CEBUF_PUT(&tCEs, tOrder);
          // *(tCEs++) = tOrder;
          tOrder &= 0xFFFF0000;
        }

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {

              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else {
            result = (sOrder < tOrder) ?  UCOL_LESS: UCOL_GREATER;
            goto commonReturn;
        }
      } /* no primary difference... do the rest from the buffers */
    } else { /* shifted - do a slightly more complicated processing */
      for(;;) {
        UBool sInShifted = FALSE;
        UBool tInShifted = FALSE;

/* This is where abridged version for shifted should go */
        for(;;) {
          // UCOL_GETNEXTCE(sOrder, coll, sColl, &status);
          sOrder = ucol_IGetNextCE(coll, &sColl, &status);
          if(sOrder == UCOL_NO_MORE_CES) {
            UCOL_CEBUF_PUT(&sCEs, sOrder);
            break;
          } else if((sOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder &= 0xFFFF0000;
                UCOL_CEBUF_PUT(&sCEs, sOrder);
                //  *(sCEs++) = sOrder;
                continue;
              } else {
                //sOrder ^= caseSwitch;
                // *(sCEs++) = sOrder;
                UCOL_CEBUF_PUT(&sCEs, sOrder);
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              } else {
                //sOrder ^= caseSwitch;
                UCOL_CEBUF_PUT(&sCEs, sOrder);
                // *(sCEs++) = sOrder;
                continue;
              }
            }
          } else { /* regular */
            if(sOrder > LVT) {
              UCOL_CEBUF_PUT(&sCEs, sOrder);
              // *(sCEs++) = sOrder;
              break;
            } else {
              if((sOrder & 0xFFFF0000) > 0) {
                sInShifted = TRUE;
                sOrder &= 0xFFFF0000;
                UCOL_CEBUF_PUT(&sCEs, sOrder);
                // *(sCEs++) = sOrder;
                continue;
              } else {
                //sOrder ^= caseSwitch;
                UCOL_CEBUF_PUT(&sCEs, sOrder);
                // *(sCEs++) = sOrder;
                continue;
              }
            }
          }
        }
        sOrder &= 0xFFFF0000;
        sInShifted = FALSE;

        for(;;) {
          // UCOL_GETNEXTCE(tOrder, coll, tColl, &status);
          tOrder = ucol_IGetNextCE(coll, &tColl, &status);
          if(tOrder == UCOL_NO_MORE_CES) {
            UCOL_CEBUF_PUT(&tCEs, tOrder);
            // *(tCEs++) = tOrder;
            break;
          } else if((tOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder &= 0xFFFF0000;
                UCOL_CEBUF_PUT(&tCEs, tOrder);
                // *(tCEs++) = tOrder;
                continue;
              } else {
                //tOrder ^= caseSwitch;
                UCOL_CEBUF_PUT(&tCEs, tOrder);
                // *(tCEs++) = tOrder;
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } else {
                //tOrder ^= caseSwitch;
                UCOL_CEBUF_PUT(&tCEs, tOrder);
                // *(tCEs++) = tOrder;
                continue;
              }
            }
          } else { /* regular */
            if(tOrder > LVT) {
              UCOL_CEBUF_PUT(&tCEs, tOrder);
              // *(tCEs++) = tOrder;
              break;
            } else {
              if((tOrder & 0xFFFF0000) > 0) {
                tInShifted = TRUE;
                tOrder &= 0xFFFF0000;
                // *(tCEs++) = tOrder;
                UCOL_CEBUF_PUT(&tCEs, tOrder);
                continue;
              } else {
                //tOrder ^= caseSwitch;
                UCOL_CEBUF_PUT(&tCEs, tOrder);
                // *(tCEs++) = tOrder;
                continue;
              }
            }
          }
        }
        tOrder &= 0xFFFF0000;
        tInShifted = FALSE;

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {
              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else {
            result = (sOrder < tOrder) ? UCOL_LESS : UCOL_GREATER;
            goto commonReturn;
        }
      } /* no primary difference... do the rest from the buffers */
    }

    /* now, we're gonna reexamine collected CEs */
    uint32_t    *sCE;
    uint32_t    *tCE;
    //sCEend = sCEs;
    //tCEend = tCEs;

    /* This is the secondary level of comparison */
    if(checkSecTer) {
      if(!isFrenchSec) { /* normal */
        sCE = sCEs.buf;
        tCE = tCEs.buf;
        for(;;) {
          while (secS == 0) {
            secS = *(sCE++) & 0xFF00;
          }

          while(secT == 0) {
              secT = *(tCE++) & 0xFF00;
          }

          if(secS == secT) {
            if(secS == 0x0100) {
              break;
            } else {
              secS = 0; secT = 0;
              continue;
            }
          } else {
               result = (secS < secT) ? UCOL_LESS : UCOL_GREATER;
               goto commonReturn;
          }
        }
      } else { /* do the French */
        uint32_t *sCESave = NULL;
        uint32_t *tCESave = NULL;
        sCE = sCEs.pos-2; /* this could also be sCEs-- if needs to be optimized */
        tCE = tCEs.pos-2;
        for(;;) {
          while (secS == 0 && sCE >= sCEs.buf) {
            if(sCESave == 0) {
              secS = *(sCE--) & 0xFF80;
              if(isContinuation(secS)) {
                while(isContinuation(secS = *(sCE--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                sCESave = sCE; /* we save it, so that we know where to come back AND that we need to go forward */
                sCE+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secS = *(sCE++) & 0xFF80;
              if(!isContinuation(secS)) { /* This means we have finished with this cont */
                sCE = sCESave;            /* reset the pointer to before continuation */
                sCESave = 0;
                continue;
              }
            }
            secS &= 0xFF00; /* remove the continuation bit */
          }

          while(secT == 0 && tCE >= tCEs.buf) {
            if(tCESave == 0) {
              secT = *(tCE--) & 0xFF80;
              if(isContinuation(secT)) {
                while(isContinuation(secT = *(tCE--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                tCESave = tCE; /* we save it, so that we know where to come back AND that we need to go forward */
                tCE+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secT = *(tCE++) & 0xFF80;
              if(!isContinuation(secT)) { /* This means we have finished with this cont */
                tCE = tCESave;          /* reset the pointer to before continuation */
                tCESave = 0;
                continue;
              }
            }
            secT &= 0xFF00; /* remove the continuation bit */
          }

          if(secS == secT) {
            if(secS == 0x0100 || (sCE < sCEs.buf && tCE < tCEs.buf)) {
              break;
            } else {
              secS = 0; secT = 0;
              continue;
            }
          } else {
              result = (secS < secT) ? UCOL_LESS : UCOL_GREATER;
              goto commonReturn;
          }
        }
      }
    }

    /* doing the case bit */
    if(checkCase) {
      sCE = sCEs.buf;
      tCE = tCEs.buf;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          if(!isContinuation(*sCE++)) {
            secS =*(sCE-1) & UCOL_TERT_CASE_MASK;
            secS ^= caseSwitch;
          } else {
            secS = 0;
          }
        }

        while((secT & UCOL_REMOVE_CASE) == 0) {
          if(!isContinuation(*tCE++)) {
            secT = *(tCE-1) & UCOL_TERT_CASE_MASK;
            secT ^= caseSwitch;
          } else {
            secT = 0;
          }
        }

        if((secS & UCOL_CASE_BIT_MASK) < (secT & UCOL_CASE_BIT_MASK)) {
          result = UCOL_LESS;
          goto commonReturn;
        } else if((secS & UCOL_CASE_BIT_MASK) > (secT & UCOL_CASE_BIT_MASK)) {
          result = UCOL_GREATER;
          goto commonReturn;
        }

        if((secS & UCOL_REMOVE_CASE) == 0x01 || (secT & UCOL_REMOVE_CASE) == 0x01 ) {
          break;
        } else {
          secS = 0;
          secT = 0;
        }
      }
    }

    /* Tertiary level */
    if(checkTertiary) {
      secS = 0;
      secT = 0;
      sCE = sCEs.buf;
      tCE = tCEs.buf;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCE++) & tertiaryMask;
          if(!isContinuation(secS)) {
            secS ^= caseSwitch;
          } else {
            secS &= UCOL_REMOVE_CASE;
          }
        }

        while((secT & UCOL_REMOVE_CASE)  == 0) {
          secT = *(tCE++) & tertiaryMask;
          if(!isContinuation(secT)) {
            secT ^= caseSwitch;
          } else {
            secT &= UCOL_REMOVE_CASE;
          }
        }

        if(secS == secT) {
          if((secS & UCOL_REMOVE_CASE) == 1) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else {
            result = (secS < secT) ? UCOL_LESS : UCOL_GREATER;
            goto commonReturn;
        }
      }
    }


    if(qShifted) {
      UBool sInShifted = TRUE;
      UBool tInShifted = TRUE;
      secS = 0;
      secT = 0;
      sCE = sCEs.buf;
      tCE = tCEs.buf;
      for(;;) {
        while(secS == 0 && secS != 0x00010101 || (isContinuation(secS) && !sInShifted)) {
          secS = *(sCE++);
          if(isContinuation(secS) && !sInShifted) {
            continue;
          }
          if(secS > LVT || (secS & 0xFFFF0000) == 0) {
            secS = 0xFFFF0000;
            sInShifted = FALSE;
          } else {
            sInShifted = TRUE;
          }
        }
        secS &= 0xFFFF0000;


        while(secT == 0 && secT != 0x00010101 || (isContinuation(secT) && !tInShifted)) {
          secT = *(tCE++);
          if(isContinuation(secT) && !tInShifted) {
            continue;
          }
          if(secT > LVT || (secT & 0xFFFF0000) == 0) {
            secT = 0xFFFF0000;
            tInShifted = FALSE;
          } else {
            tInShifted = TRUE;
          }
        }
        secT &= 0xFFFF0000;

        if(secS == secT) {
          if(secS == 0x00010000) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else {
            result = (secS < secT) ? UCOL_LESS : UCOL_GREATER;
            goto commonReturn;
        }
      }
    }

    /*  For IDENTICAL comparisons, we use a bitwise character comparison */
    /*  as a tiebreaker if all else is equal.                                */
    /*  Getting here  should be quite rare - strings are not identical -     */
    /*     that is checked first, but compared == through all other checks.  */
    if(checkIdent)
    {
        result = ucol_checkIdent(&sColl, &tColl, coll->normalizationMode == UCOL_ON);
    }

commonReturn:
    if ((sColl.flags | tColl.flags) & UCOL_ITER_ALLOCATED) {
        freeHeapWritableBuffer(&sColl);
        freeHeapWritableBuffer(&tColl);

        if (sCEs.buf != sCEs.localArray ) {
            uprv_free(sCEs.buf);
        }
        if (tCEs.buf != tCEs.localArray ) {
            uprv_free(tCEs.buf);
        }
    }

    return result;
}


void init_incrementalContext(const UCollator *coll, UCharForwardIterator *source, void *sourceContext, incrementalContext *s) {
    s->len = s->stringP = s->stackString ;
    s->capacity = s->stackString+UCOL_MAX_BUFFER;
    s->CEpos = s->toReturn = s->CEs;
    s->source = source;
    s->sourceContext = sourceContext;
    s->currentChar = 0xFFFF;
    s->lastChar = 0xFFFF;
    s->panic = FALSE;
    s->coll = coll;
}

/* This is the incremental function */
U_CAPI UCollationResult ucol_strcollinc(const UCollator *coll,
                                 UCharForwardIterator *source, void *sourceContext,
                                 UCharForwardIterator *target, void *targetContext)
{
    incrementalContext sColl, tColl;

    init_incrementalContext(coll, source, sourceContext, &sColl);
    init_incrementalContext(coll, target, targetContext, &tColl);

#if 0
    /* This is Andy's fast preparatory scan */
    /* It's good to have it - once the regular function is working */
    /* Scan the strings.  Find:                                                             */
    /*    their length, if not given by caller                                              */
    /*    The length of any leading portion that is equal                                   */
    /*    Whether they are exactly equal.  (in which case we just return                    */
    const UChar    *pSrc    = source;
    const UChar    *pTarg   = target;

    const UChar    *pSrcEnd = source + sourceLength;
    const UChar    *pTargEnd = target + targetLength;

    int32_t        equalLength = 0;

    // Scan while the strings are bitwise ==, or until one is exhausted.
    for (;;) {
        if (pSrc == pSrcEnd || pTarg == pTargEnd)
            break;
        if (*pSrc != *pTarg)
            break;
        if (*pSrc == 0 && (sourceLength == -1 || targetLength == -1))
            break;
        equalLength++;
        pSrc++;
        pTarg++;
    }

    // If we made it all the way through both strings, we are done.  They are ==
    if ((pSrc ==pSrcEnd  || (pSrcEnd <pSrc  && *pSrc==0))  &&   /* At end of src string, however it was specified. */
        (pTarg==pTargEnd || (pTargEnd<pTarg && *pTarg==0)))     /* and also at end of dest string                  */
        return UCOL_EQUAL;

    // If we don't know the length of the src string, continue scanning it to get the length..
    if (sourceLength == -1) {
        while (*pSrc != 0 ) {
            pSrc++;
        }
        sourceLength = pSrc - source;
    }

    // If we don't know the length of the targ string, continue scanning it to get the length..
    if (targetLength == -1) {
        while (*pTarg != 0 ) {
            pTarg++;
        }
        targetLength = pTarg - target;
    }


    if (equalLength > 2) {
        /* There is an identical portion at the beginning of the two strings.        */
        /*   If the identical portion ends within a contraction or a comibining      */
        /*   character sequence, back up to the start of that sequence.              */
        pSrc  = source + equalLength;        /* point to the first differing chars   */
        pTarg = target + equalLength;
        if (pSrc  != source+sourceLength && ucol_unsafeCP(*pSrc, coll) ||
            pTarg != target+targetLength && ucol_unsafeCP(*pTarg, coll))
        {
            // We are stopped in the middle of a contraction.
            // Scan backwards through the == part of the string looking for the start of the contraction.
            //   It doesn't matter which string we scan, since they are the same in this region.
            do
            {
                equalLength--;
                pSrc--;
            }
            while (equalLength>0 && ucol_unsafeCP(*pSrc, coll));
        }

        source += equalLength;
        target += equalLength;
        sourceLength -= equalLength;
        targetLength -= equalLength;
    }

#endif

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    if(coll->normalizationMode != UCOL_OFF) { /*  run away screaming!!!! */
        return alternateIncrementalProcessing(coll, &sColl, &tColl);
    }

    UColAttributeValue strength = coll->strength;
    UBool initialCheckSecTer = (strength  >= UCOL_SECONDARY);

    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = (strength  >= UCOL_TERTIARY);
    UBool checkQuad = (strength  >= UCOL_QUATERNARY);
    UBool checkIdent = (strength == UCOL_IDENTICAL);
    UBool checkCase = (coll->caseLevel == UCOL_ON);
    UBool isFrenchSec = (coll->frenchCollation == UCOL_ON) && checkSecTer;
    UBool shifted = (coll->alternateHandling == UCOL_SHIFTED);
    UBool qShifted = shifted && checkQuad;

    uint32_t sCEsArray[512], tCEsArray[512];
    uint32_t *sCEs = sCEsArray, *tCEs = tCEsArray;
    uint32_t *sCEend = sCEs+512, *tCEend = tCEs+512;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    uint32_t LVT = (shifted)?((coll->variableMax1)<<24 | (coll->variableMax2)<<16):0;

    uint32_t secS = 0, secT = 0;

    uint32_t sOrder=0, tOrder=0;
    if(!shifted) {
      for(;;) {
        if(sCEs == sCEend || tCEs == tCEend) {
          return alternateIncrementalProcessing(coll, &sColl, &tColl);
        }

        /* Get the next collation element in each of the strings, unless */
        /* we've been requested to skip it. */
        while(sOrder == 0) {
          sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
          sOrder ^= caseSwitch;
          *(sCEs++) = sOrder;
          sOrder &= 0xFFFF0000;
        }

        while(tOrder == 0) {
          tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
          tOrder ^= caseSwitch;
          *(tCEs++) = tOrder;
          tOrder &= 0xFFFF0000;
        }

        if((sOrder == (UCOL_NO_MORE_CES & UCOL_PRIMARYORDERMASK) && sColl.panic == TRUE) ||
          (tOrder == (UCOL_NO_MORE_CES & UCOL_PRIMARYORDERMASK) && tColl.panic == TRUE)) {
          return alternateIncrementalProcessing(coll, &sColl, &tColl);
        }

        if(sOrder == tOrder) {
            if(sOrder == (UCOL_NO_MORE_CES & UCOL_PRIMARYORDERMASK)) {

              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        }
      } /* no primary difference... do the rest from the buffers */
    } else { /* shifted - do a slightly more complicated processing */
      for(;;) {
        UBool sInShifted = FALSE;
        UBool tInShifted = FALSE;

        if(sCEs == sCEend || tCEs == tCEend) {
          return alternateIncrementalProcessing(coll, &sColl, &tColl);
        }

/* This is where abridged version for shifted should go */
        for(;;) {
          sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
          if(sOrder == UCOL_NO_MORE_CES) {
            if(sColl.panic == TRUE) {
              return alternateIncrementalProcessing(coll, &sColl, &tColl);
            }
            *(sCEs++) = sOrder;
            break;
          } else if((sOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          } else { /* regular */
            if(sOrder > LVT) {
              *(sCEs++) = sOrder;
              break;
            } else {
              if((sOrder & 0xFFFF0000) > 0) {
                sInShifted = TRUE;
                sOrder &= 0xFFFF0000;
                *(sCEs++) = sOrder;
                continue;
              } else {
                sOrder ^= caseSwitch;
                *(sCEs++) = sOrder;
                continue;
              }
            }
          }
        }
        sOrder &= 0xFFFF0000;
        sInShifted = FALSE;

        for(;;) {
          tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
          if(tOrder == UCOL_NO_MORE_CES) {
            if(tColl.panic == TRUE) {
              return alternateIncrementalProcessing(coll, &sColl, &tColl);
            }
            *(tCEs++) = tOrder;
            break;
          } else if((tOrder & 0xFFFFFFBF) == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & 0xFFFF0000) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          } else { /* regular */
            if(tOrder > LVT) {
              *(tCEs++) = tOrder;
              break;
            } else {
              if((tOrder & 0xFFFF0000) > 0) {
                tInShifted = TRUE;
                tOrder &= 0xFFFF0000;
                *(tCEs++) = tOrder;
                continue;
              } else {
                tOrder ^= caseSwitch;
                *(tCEs++) = tOrder;
                continue;
              }
            }
          }
        }
        tOrder &= 0xFFFF0000;
        tInShifted = FALSE;

        if(sOrder == tOrder) {
            if(sOrder == 0x00010000) {
              break;
            } else {
              sOrder = 0; tOrder = 0;
              continue;
            }
        } else if(sOrder < tOrder) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        }
      } /* no primary difference... do the rest from the buffers */
    }

    /* now, we're gonna reexamine collected CEs */
    sCEend = sCEs;
    tCEend = tCEs;

    /* This is the secondary level of comparison */
    if(checkSecTer) {
      if(!isFrenchSec) { /* normal */
        sCEs = sCEsArray;
        tCEs = tCEsArray;
        for(;;) {
          while (secS == 0) {
            secS = *(sCEs++) & 0xFF00;
          }

          while(secT == 0) {
              secT = *(tCEs++) & 0xFF00;
          }

          if(secS == secT) {
            if(secS == 0x0100) {
              break;
            } else {
              secS = 0; secT = 0;
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          }
        }
      } else { /* do the French */
        uint32_t *sCESave = NULL;
        uint32_t *tCESave = NULL;
        sCEs = sCEend-2; /* this could also be sCEs-- if needs to be optimized */
        tCEs = tCEend-2;
        for(;;) {
          while (secS == 0 && sCEs >= sCEsArray) {
            if(sCESave == 0) {
              secS = *(sCEs--) & 0xFF80;
              if(isContinuation(secS)) {
                while(isContinuation(secS = *(sCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                sCESave = sCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                sCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secS = *(sCEs++) & 0xFF80;
              if(!isContinuation(secS)) { /* This means we have finished with this cont */
                sCEs = sCESave;          /* reset the pointer to before continuation */
                sCESave = 0;
                continue;
              }
            }
            secS &= 0xFF00; /* remove the continuation bit */
          }

          while(secT == 0 && tCEs >= tCEsArray) {
            if(tCESave == 0) {
              secT = *(tCEs--) & 0xFF80;
              if(isContinuation(secT)) {
                while(isContinuation(secT = *(tCEs--) & 0xFF80));
                /* after this, secS has the start of continuation, and sCEs points before that */
                tCESave = tCEs; /* we save it, so that we know where to come back AND that we need to go forward */
                tCEs+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secT = *(tCEs++) & 0xFF80;
              if(!isContinuation(secT)) { /* This means we have finished with this cont */
                tCEs = tCESave;          /* reset the pointer to before continuation */
                tCESave = 0;
                continue;
              }
            }
            secT &= 0xFF00; /* remove the continuation bit */
          }

          if(secS == secT) {
            if(secS == 0x0100 || (sCEs < sCEsArray && tCEs < tCEsArray)) {
              break;
            } else {
              secS = 0; secT = 0;
              continue;
            }
          } else if(secS < secT) {
            return UCOL_LESS;
          } else {
            return UCOL_GREATER;
          }
        }
      }
    }

    /* doing the case bit */
    if(checkCase) {
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          if(!isContinuation(*sCEs++)) {
            secS =*(sCEs-1) & UCOL_TERT_CASE_MASK;
          }
        }

        while((secT & UCOL_REMOVE_CASE) == 0) {
          if(!isContinuation(*tCEs++)) {
            secT = *(tCEs-1) & UCOL_TERT_CASE_MASK;
          }
        }

        if((secS & UCOL_CASE_BIT_MASK) < (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_LESS;
        } else if((secS & UCOL_CASE_BIT_MASK) > (secT & UCOL_CASE_BIT_MASK)) {
          return UCOL_GREATER;
        }

        if((secS & UCOL_REMOVE_CASE) == 0x01 || (secT & UCOL_REMOVE_CASE) == 0x01 ) {
          break;
        } else {
          secS = 0;
          secT = 0;
        }
      }
    }

    /* Tertiary level */
    if(checkTertiary) {
      secS = 0;
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while((secS & UCOL_REMOVE_CASE) == 0) {
          secS = *(sCEs++) & tertiaryMask;
        }

        while((secT & UCOL_REMOVE_CASE)  == 0) {
            secT = *(tCEs++) & tertiaryMask;
        }

        if(secS == secT) {
          if((secS & UCOL_REMOVE_CASE) == 1) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        }
      }
    }


    if(qShifted) {
      UBool sInShifted = TRUE;
      UBool tInShifted = TRUE;
      secS = 0;
      secT = 0;
      sCEs = sCEsArray;
      tCEs = tCEsArray;
      for(;;) {
        while(secS == 0 && secS != 0x00010101 || (isContinuation(secS) && !sInShifted)) {
          secS = *(sCEs++);
          if(isContinuation(secS) && !sInShifted) {
            continue;
          }
          if(secS > LVT || (secS & 0xFFFF0000) == 0) {
            secS = 0xFFFF0000;
            sInShifted = FALSE;
          } else {
            sInShifted = TRUE;
          }
        }
        secS &= 0xFFFF0000;


        while(secT == 0 && secT != 0x00010101 || (isContinuation(secT) && !tInShifted)) {
          secT = *(tCEs++);
          if(isContinuation(secT) && !tInShifted) {
            continue;
          }
          if(secT > LVT || (secT & 0xFFFF0000) == 0) {
            secT = 0xFFFF0000;
            tInShifted = FALSE;
          } else {
            tInShifted = TRUE;
          }
        }
        secT &= 0xFFFF0000;

        if(secS == secT) {
          if(secS == 0x00010000) {
            break;
          } else {
            secS = 0; secT = 0;
            continue;
          }
        } else if(secS < secT) {
          return UCOL_LESS;
        } else {
          return UCOL_GREATER;
        }
      }
    }

    /*  For IDENTICAL comparisons, we use a bitwise character comparison */
    /*  as a tiebreaker if all else is equal */
    /*  NOTE: The java code compares result with 0, and  */
    /*  puts the result of the string comparison directly into result */
    /*    if (result == UCOL_EQUAL && strength == UCOL_IDENTICAL) */
    if(checkIdent)
    {
        UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;

        /* synwee : implemented in c++ since normalizer is implemented there */
        Normalizer::EMode mode = Normalizer::getNormalizerEMode(
                                                 ucol_getNormalization(coll), status);

        Normalizer::normalize(UnicodeString(sColl.stringP, sColl.len-sColl.stringP-1),
                              mode, 0, sourceDecomp,  status);

        Normalizer::normalize(UnicodeString(tColl.stringP, tColl.len-tColl.stringP-1),
                              mode, 0, targetDecomp,  status);

        comparison = sourceDecomp.compare(targetDecomp);

        if (comparison < 0)
        {
            result = UCOL_LESS;
        }
        else if (comparison == 0)
        {
            result = UCOL_EQUAL;
        }
        else
        {
            result = UCOL_GREATER;
        }
    }

    return result;
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_greater(    const    UCollator        *coll,
        const    UChar            *source,
        int32_t            sourceLength,
        const    UChar            *target,
        int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength)
      == UCOL_GREATER);
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_greaterOrEqual(    const    UCollator    *coll,
            const    UChar        *source,
            int32_t        sourceLength,
            const    UChar        *target,
            int32_t        targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength)
      != UCOL_LESS);
}

/* convenience function for comparing strings */
U_CAPI UBool
ucol_equal(        const    UCollator        *coll,
            const    UChar            *source,
            int32_t            sourceLength,
            const    UChar            *target,
            int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength)
      == UCOL_EQUAL);
}


int32_t ucol_getIncrementalCE(const UCollator *coll, incrementalContext *ctx, UErrorCode *status) {
    uint32_t order;
    if (ctx->CEpos > ctx->toReturn) {       /* Are there any CEs from previous expansions? */
      order = *(ctx->toReturn++);                         /* if so, return them */
      if(ctx->CEpos == ctx->toReturn) {
        ctx->CEpos = ctx->toReturn = ctx->CEs;
      }
    } else {          /* This is the real business now */
      if(ctx->lastChar == 0xFFFF) {
          ctx->currentChar = ctx->source(ctx->sourceContext);
          incctx_appendChar(ctx, ctx->currentChar);
          if(ctx->currentChar == 0xFFFF) {
              return UCOL_NO_MORE_CES;
          }
      } else {
          ctx->currentChar = ctx->lastChar;
          ctx->lastChar = 0xFFFF;
      }

      UChar ch = ctx->currentChar;
      if(ch <= 0xFF) {                                                 /* if it's Latin One, we'll try to fast track it */
        order = coll->latinOneMapping[ch];                            /* by looking in up in an array */
      } else {                                                        /* otherwise, */
        order = ucmp32_get(coll->mapping, ch);                        /* we'll go for slightly slower trie */
      }
      if(order >= UCOL_NOT_FOUND) {                                   /* if a CE is special */
        order = ucol_getIncrementalSpecialCE(coll, order, ctx, status);       /* and try to get the special CE */
        if(order == UCOL_NOT_FOUND) {   /* We couldn't find a good CE in the tailoring */
          order = ucol_getIncrementalUCA(ch, ctx, status);
        }
      }
    }
    /* This means that contraction should spit back the last codepoint eaten! */
    return order; /* return the CE */
}

/* This function tries to get a CE from UCA, which should be always around  */
/* UChar is passed in in order to speed things up                           */
/* here is also the generation of implicit CEs                              */
uint32_t ucol_getIncrementalUCA(UChar ch, incrementalContext *collationSource, UErrorCode *status) {
    uint32_t order;
    if(ch < 0xFF) {               /* so we'll try to find it in the UCA */
      order = UCA->latinOneMapping[ch];
    } else {
      order = ucmp32_get(UCA->mapping, ch);
    }
    if(order >= UCOL_NOT_FOUND) { /* UCA also gives us a special CE */
      order = ucol_getIncrementalSpecialCE(UCA, order, collationSource, status);
    }
    if(order == UCOL_NOT_FOUND) { /* This is where we have to resort to algorithmical generation */
      /* We have to check if ch is possibly a first surrogate - then we need to take the next code unit */
      /* and make a bigger CE */
      const uint32_t
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount;   // 11172
        //LLimit = LBase + LCount,    // 1113
        //VLimit = VBase + VCount,    // 1176
        //TLimit = TBase + TCount,    // 11C3
        //SLimit = SBase + SCount;    // D7A4

        // once we have failed to find a match for codepoint cp, and are in the implicit code.

        uint32_t L = ch - SBase;
        //if (ch < SLimit) { // since it is unsigned, catchs zero case too
        if (L < SCount) { // since it is unsigned, catchs zero case too

          // divide into pieces

          uint32_t T = L % TCount; // we do it in this order since some compilers can do % and / in one operation
          L /= TCount;
          uint32_t V = L % VCount;
          L /= VCount;

          // offset them

          L += LBase;
          V += VBase;
          T += TBase;

          // return the first CE, but first put the rest into the expansion buffer
          if (!collationSource->coll->image->jamoSpecial) { // FAST PATH

            *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, V);
            if (T != TBase) {
                *(collationSource->CEpos++) = ucmp32_get(UCA->mapping, T);
            }

            return ucmp32_get(UCA->mapping, L); // return first one

          } else { // Jamo is Special
            collIterate jamos;
            UChar jamoString[3];
            uint32_t CE = UCOL_NOT_FOUND;
            const UCollator *collator = collationSource->coll;
            jamoString[0] = (UChar)L;
            jamoString[1] = (UChar)V;
            if (T != TBase) {
              jamoString[2] = (UChar)T;
              IInit_collIterate(collator, jamoString, 3, &jamos);
            } else {
              IInit_collIterate(collator, jamoString, 2, &jamos);
            }

            CE = ucol_IGetNextCE(collator, &jamos, status);

            while(CE != UCOL_NO_MORE_CES) {
              *(collationSource->CEpos++) = CE;
              CE = ucol_IGetNextCE(collator, &jamos, status);
            }
            return *(collationSource->toReturn++);

/*
            ucol_getJamoCEs(collationSource->coll, L, &collationSource->CEpos);
            ucol_getJamoCEs(collationSource->coll, V, &collationSource->CEpos);
            if (T != TBase) {
              ucol_getJamoCEs(collationSource->coll, T, &collationSource->CEpos);
            }
            return *(collationSource->toReturn++);
*/

/*
            // do recursive processing of L, V, and T with fetchCE (but T only if not equal to TBase!!)
            // Since fetchCE returns a CE, and (potentially) stuffs items into the ce buffer,
            // this is how it is done.

            int firstCE = fetchCE(L, ...);
            int* lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
            *lastExpansion = fetchCE(V,...);
            if (T != TBase) {
              lastExpansion = expansionBufferEnd++; // set pointer, leave gap!
              *lastExpansion = fetchCE(T,...);
            }
*/
          }
      }

      collationSource->lastChar = collationSource->source(collationSource->sourceContext);
      incctx_appendChar(collationSource, collationSource->lastChar);

      if(UTF_IS_FIRST_SURROGATE(ch)) {
        if( (collationSource->lastChar != 0xFFFF) &&
          UTF_IS_SECOND_SURROGATE((collationSource->lastChar))) {
          uint32_t cp = (((ch)<<10UL)+(collationSource->lastChar)-((0xd800<<10UL)+0xdc00));
          collationSource->lastChar = 0xFFFF; /*used up*/
          if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
              return 0;  /* illegal code value, use completely ignoreable! */
          }
          /* This is a code point minus 0x10000, that's what algorithm requires */
          order = 0xE0010303 | (cp & 0xFFE00) << 8;

          *(collationSource->CEpos++) = 0x80200080 | (cp & 0x001FF) << 22;
        } else {
          return 0; /* completely ignorable */
        }
      } else {
        /* otherwise */
        if(UTF_IS_SECOND_SURROGATE((ch)) || (ch & 0xFFFE) == 0xFFFE) {
          return 0; /* completely ignorable */
        }
        /* Make up an artifical CE from code point as per UCA */
        order = 0xD0800303 | (ch & 0xF000) << 12 | (ch & 0x0FE0) << 11;
        *(collationSource->CEpos++) = 0x04000080 | (ch & 0x001F) << 27;
      }
    }
    return order; /* return the CE */
}


int32_t ucol_getIncrementalSpecialCE(const UCollator *coll, uint32_t CE, incrementalContext *source, UErrorCode *status) {
  uint32_t i = 0; /* general counter */

  if(U_FAILURE(*status)) return -1;

  for(;;) {
    const uint32_t *CEOffset = NULL;
    const UChar *UCharOffset = NULL;
    UChar schar, tchar;
    uint32_t size = 0;
    switch(getCETag(CE)) {
    case NOT_FOUND_TAG:
      /* This one is not found, and we'll let somebody else bother about it... no more games */
      return CE;
    case SURROGATE_TAG:
      /* pending surrogate discussion with Markus and Mark */
      return UCOL_NOT_FOUND;
    case THAI_TAG:
      /* Thai/Lao reordering */
      source->panic = TRUE;
      return UCOL_NO_MORE_CES;
    case CONTRACTION_TAG:
      /* This should handle contractions */
      for(;;) {
        /* First we position ourselves at the begining of contraction sequence */
        const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

        /* we need to convey the notion of having a backward search - most probably through the context object */
        /* if (backwardsSearch) offset += contractionUChars[(int16_t)offset]; else UCharOffset++;  */
        schar = source->lastChar = source->source(source->sourceContext);
        incctx_appendChar(source, source->lastChar);
        if (schar == 0xFFFF) { /* this is the end of string */
          CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex)); /* So we'll pick whatever we have at the point... */
//!          source->pos--; /* I think, since we'll advance in the getCE */
          break;
        }
        UCharOffset++; /* skip the backward offset, see above */
//!        schar = *(++source->pos);
        while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          UCharOffset++;
        }
        if(schar != tchar) { /* we didn't find the correct codepoint. We can use either the first or the last CE */
          if(tchar != 0xFFFF) {
            UCharOffset = ContractionStart; /* We're not at the end, bailed out in the middle. Better use starting CE */
          }
//!          source->pos--; /* Spit out the last char of the string, wasn't tasty enough */
        } else {
          source->lastChar = 0xFFFF;
        }
        CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
/*
        if(!isContraction(CE)) {
          break;
        }
*/
        if(isContraction(CE)) { /* fix for the bug. Other places need to be checked */
        /* this is contraction, and we will continue. However, we can fail along the */
        /* th road, which means that we have part of contraction correct */
          source->panic = TRUE;
          return UCOL_NO_MORE_CES;
        } else {
          break;
        }
      }
      break;
    case EXPANSION_TAG:
      /* This should handle expansion. */
      /* NOTE: we can encounter both continuations and expansions in an expansion! */
      /* I have to decide where continuations are going to be dealt with */
      CEOffset = (uint32_t *)coll->image+getExpansionOffset(CE); /* find the offset to expansion table */
      size = getExpansionCount(CE);
      CE = *CEOffset++;
      if(size != 0) { /* if there are less than 16 elements in expansion, we don't terminate */
        for(i = 1; i<size; i++) {
          *(source->CEpos++) = *CEOffset++;
        }
      } else { /* else, we do */
        while(*CEOffset != 0) {
          *(source->CEpos++) = *CEOffset++;
        }
      }
      /*source->toReturn++;*/
      return CE;
    case CHARSET_TAG:
      /* probably after 1.8 */
      return UCOL_NOT_FOUND;
    default:
      *status = U_INTERNAL_PROGRAM_ERROR;
      CE=0;
      break;
    }
    if (CE <= UCOL_NOT_FOUND) break;
  }
  return CE;

}

void incctx_cleanUpContext(incrementalContext *ctx) {
    if(ctx->stringP != ctx->stackString) {
        uprv_free(ctx->stringP);
    }
}

UChar incctx_appendChar(incrementalContext *ctx, UChar c) {
    if(ctx->len == ctx->capacity) { /* bother, said Pooh, we need to reallocate */
        UChar *newStuff;
        if(ctx->stringP == ctx->stackString) { /* we haven't allocated before, need to allocate */
            newStuff = (UChar *)uprv_malloc(2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
            uprv_memcpy(newStuff, ctx->stringP, (ctx->capacity - ctx->stringP)*sizeof(UChar));
        } else { /* we have already allocated, need to reallocate */
            newStuff = (UChar *)uprv_realloc(ctx->stringP, 2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
        }
        ctx->len=newStuff+(ctx->len - ctx->stringP);
        ctx->capacity = newStuff+2*(ctx->capacity - ctx->stringP);
        ctx->stringP = newStuff;
    }
    *(ctx->len++) = c;
    return c;
}



UCollationResult alternateIncrementalProcessing(const UCollator *coll, incrementalContext *srcCtx, incrementalContext *trgCtx) {
    if(srcCtx->stringP == srcCtx->len || *(srcCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(srcCtx, srcCtx->source(srcCtx->sourceContext)) != 0xFFFF);
    }
    if(trgCtx->stringP == trgCtx->len || *(trgCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(trgCtx, trgCtx->source(trgCtx->sourceContext)) != 0xFFFF);
    }
    UCollationResult result = ucol_strcoll(coll, srcCtx->stringP, srcCtx->len-srcCtx->stringP-1, trgCtx->stringP, trgCtx->len-trgCtx->stringP-1);
    incctx_cleanUpContext(srcCtx);
    incctx_cleanUpContext(trgCtx);
    return result;
}
