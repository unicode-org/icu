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
#include "bocsu.h"

#include "unicode/uloc.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/unorm.h"
#include "unicode/normlzr.h"
#include "unicode/udata.h"

#include "unormimp.h"
#include "cpputils.h"
#include "cstring.h"
#include "umutex.h"
#include "uhash.h"
#include "ucln_in.h" 

#ifdef UCOL_DEBUG
#include <stdio.h>
#endif

U_NAMESPACE_USE

/* added by synwee for trie manipulation*/
#define STAGE_1_SHIFT_            10
#define STAGE_2_SHIFT_            4
#define STAGE_2_MASK_AFTER_SHIFT_ 0x3F
#define STAGE_3_MASK_             0xF
#define LAST_BYTE_MASK_           0xFF
#define SECOND_LAST_BYTE_SHIFT_   8

#define ZERO_CC_LIMIT_            0xC0

static UCollator* UCA = NULL;
static UDataMemory* UCA_DATA_MEM = NULL;


U_CDECL_BEGIN
static UBool U_CALLCONV
isAcceptableUCA(void * /*context*/,
             const char * /*type*/, const char * /*name*/,
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
U_CDECL_END

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
        (s)->flags |= UCOL_ITER_NORM; 
    }
    if(collator->hiraganaQ == UCOL_ON) {
      (s)->flags |= UCOL_HIRAGANA_Q;
    }
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


/*
* collIter_eos()
*     Checks for a collIterate being positioned at the end of
*     its source string.
*
*/
inline UBool collIter_eos(collIterate *s) {
    if ((s->flags & UCOL_ITER_HASLEN) == 0 && *s->pos != 0) {
        // Null terminated string, but not at null, so not at end.
        //   Whether in main or normalization buffer doesn't matter.
        return FALSE;
    }

    // String with length.  Can't be in normalization buffer, which is always
    //  null termintated.
    if (s->flags & UCOL_ITER_HASLEN) {
        return (s->pos == s->endp);
    }

    // We are at a null termination, could be either normalization buffer or main string.
    if ((s->flags & UCOL_ITER_INNORMBUF) == 0) {
        // At null at end of main string.
        return TRUE;
    }

    // At null at end of normalization buffer.  Need to check whether there there are
    //   any characters left in the main buffer.

    if ((s->origFlags & UCOL_ITER_HASLEN) == 0) {
        // Null terminated main string.  fcdPosition is the 'return' position into main buf.
        return (*s->fcdPosition == 0);
    }
    else {
        // Main string with an end pointer.
        return s->fcdPosition == s->endp;
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
      ucmpe32_close(coll->mapping);
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
ucol_openRules( const UChar        *rules,
                int32_t            rulesLength,
                UColAttributeValue normalizationMode,
                UCollationStrength strength,
                UParseError        *parseError,
                UErrorCode         *status)
{
  uint32_t listLen = 0;
  UColTokenParser src;
  UColAttributeValue norm;
  UParseError tErr;
  
  if(status == NULL || U_FAILURE(*status)){
    return 0;
  }

  if(rulesLength < -1 || (rules == NULL && rulesLength != 0)) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  if(rulesLength == -1) {
    rulesLength = u_strlen(rules);
  }

  if(parseError == NULL){
    parseError = &tErr;
  }
  
  switch((int)normalizationMode) { // TODO friendly deprecation helper, remove the (int) cast >2002-sep-30
  case UCOL_OFF:
  case UNORM_NONE:      // TODO friendly deprecation helper, remove >2002-sep-30
    norm = UCOL_OFF;
    break;
  case UCOL_ON:
  case UNORM_NFD:       // TODO friendly deprecation helper, remove >2002-sep-30
    norm = UCOL_ON;
    break;
  case UCOL_DEFAULT_NORMALIZATION: // TODO friendly deprecation helper, remove >2002-sep-30
  case UCOL_DEFAULT:
    norm = UCOL_DEFAULT;
    break;
  default:
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  ucol_initUCA(status);

  if(U_FAILURE(*status)){
    return 0;
  }

  ucol_tok_initTokenList(&src, rules, rulesLength, UCA, status);
  listLen = ucol_tok_assembleTokenList(&src,parseError, status);

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
    // must be only options
    // We will init the collator from UCA   
    result = ucol_initCollator(UCA->image,0,status);
    // And set only the options
    UColOptionSet *opts = (UColOptionSet *)uprv_malloc(sizeof(UColOptionSet));
    uprv_memcpy(opts, src.opts, sizeof(UColOptionSet));
    ucol_setOptionsFromHeader(result, opts, status);
    result->freeOptionsOnClose = TRUE;
    result->hasRealData = FALSE;
  }

  if(U_SUCCESS(*status)) {
    UChar *newRules;
    result->dataInfo.dataVersion[0] = UCOL_BUILDER_VERSION;
    newRules = (UChar *)uprv_malloc((rulesLength+1)*U_SIZEOF_UCHAR);
    if(rulesLength > 0) {
      uprv_memcpy(newRules, rules, rulesLength*U_SIZEOF_UCHAR);
    }
    newRules[rulesLength]=0;
    result->rules = newRules;
    result->rulesLength = rulesLength;
    result->freeRulesOnClose = TRUE;
    result->rb = 0;
    ucol_setAttribute(result, UCOL_STRENGTH, strength, status);
    ucol_setAttribute(result, UCOL_NORMALIZATION_MODE, norm, status);
  } else {
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
ucol_cloneRuleData(const UCollator *coll, int32_t *length, UErrorCode *status)
{
  uint8_t *result = NULL;
  if(U_FAILURE(*status)) {
    return NULL;
  }
  if(coll->hasRealData == TRUE) {
    *length = coll->image->size;
    result = (uint8_t *)uprv_malloc(*length);
    uprv_memcpy(result, coll->image, *length);
  } else {
    *length = (uint8_t)paddedsize(sizeof(UCATableHeader))+paddedsize(sizeof(UColOptionSet));
    result = (uint8_t *)uprv_malloc(*length);
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
    result->hiraganaQ = opts->hiraganaQ;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;
    result->hiraganaQisDefault = TRUE;

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
    opts->hiraganaQ = opts->hiraganaQ;
}

static const uint16_t *fcdTrieIndex=NULL;


/**
* Approximate determination if a character is at a contraction end.
* Guaranteed to be TRUE if a character is at the end of a contraction,
* otherwise it is not deterministic.
* @param c character to be determined
* @param coll collator
*/
inline UBool ucol_contractionEndCP(UChar c, const UCollator *coll) {
    if (UTF_IS_TRAIL(c)) {
      return TRUE;
    }

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



/*
*   i_getCombiningClass()
*        A fast, at least partly inline version of u_getCombiningClass()
*        This is a candidate for further optimization.  Used heavily
*        in contraction processing.
*/
inline uint8_t i_getCombiningClass(UChar c, const UCollator *coll) {
    uint8_t sCC = 0;
    if (c >= 0x300 && ucol_unsafeCP(c, coll)) {
        sCC = u_getCombiningClass(c);
    }
    return sCC;
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
    CompactEIntArray *newUCAmapping = ucmpe32_openFromData(&mapping, status);
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
    result->hiraganaQ = result->options->hiraganaQ;

    result->caseFirstisDefault = TRUE;
    result->caseLevelisDefault = TRUE;
    result->frenchCollationisDefault = TRUE;
    result->normalizationModeisDefault = TRUE;
    result->strengthisDefault = TRUE;
    result->variableTopValueisDefault = TRUE;
    result->alternateHandlingisDefault = TRUE;
    result->hiraganaQisDefault = TRUE;

    result->scriptOrder = NULL;

    result->rules = NULL;
    result->rulesLength = 0;

    /* get the version info from UCATableHeader and populate the Collator struct*/
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

    if (fcdTrieIndex == NULL) {
        fcdTrieIndex = unorm_getFCDTrie(status);
    }

    result->errorCode = *status;
    ucol_updateInternalState(result);

    return result;
}

U_CFUNC UBool
ucol_cleanup(void)
{
    if (UCA_DATA_MEM) {
        udata_close(UCA_DATA_MEM);
        UCA_DATA_MEM = NULL;
    }
    if (UCA) {
        /* Since UCA was opened with ucol_initCollator, ucol_close won't work. */
        ucmpe32_close(UCA->mapping);
        uprv_free(UCA);
        UCA = NULL;
    }
    return TRUE;
}

void ucol_initUCA(UErrorCode *status) {
    if(U_FAILURE(*status))
        return;
    
    if(UCA == NULL) {
        UCollator *newUCA = (UCollator *)uprv_malloc(sizeof(UCollator));
        if (newUCA == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return;
        }
        
        UDataMemory *result = udata_openChoice(NULL, UCA_DATA_TYPE, UCA_DATA_NAME, isAcceptableUCA, NULL, status);
        
        if(U_FAILURE(*status)) {
            if (result) {
                udata_close(result);
            }
            uprv_free(newUCA);
        }
        
        if(result != NULL) { /* It looks like sometimes we can fail to find the data file */
            newUCA = ucol_initCollator((const UCATableHeader *)udata_getMemory(result), newUCA, status);
            if(U_SUCCESS(*status)){
                newUCA->rb = NULL;
                umtx_lock(NULL);
                if(UCA == NULL) {
                    UCA = newUCA;
                    UCA_DATA_MEM = result;
                    result = NULL;
                    newUCA = NULL;
                }
                umtx_unlock(NULL);
                
                if(newUCA != NULL) {
                    udata_close(result);
                    uprv_free(newUCA);
                }
                else {
                    ucln_i18n_registerCleanup();
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
static
void collIterNormalize(collIterate *collationSource)
{
    UErrorCode  status = U_ZERO_ERROR;
    UChar      *srcP = collationSource->pos - 1;      /*  Start of chars to normalize    */
    UChar      *endP = collationSource->fcdPosition;  /* End of region to normalize+1    */
    int32_t    normLen;

    normLen = unorm_decompose(collationSource->writableBuffer, (int32_t)collationSource->writableBufSize,
                              srcP, (int32_t)(endP - srcP),
                              FALSE, FALSE,
                              &status);
    if(status == U_BUFFER_OVERFLOW_ERROR || status == U_STRING_NOT_TERMINATED_WARNING) {
        // reallocate and terminate
        if(!u_growBufferFromStatic(collationSource->stackWritableBuffer,
                                   &collationSource->writableBuffer,
                                   (int32_t *)&collationSource->writableBufSize, normLen + 1,
                                   0)
        ) {
#ifdef UCOL_DEBUG
            fprintf(stderr, "collIterNormalize(), out of memory\n");
#endif
            return;
        }
        status = U_ZERO_ERROR;
        normLen = unorm_decompose(collationSource->writableBuffer, (int32_t)collationSource->writableBufSize,
                                  srcP, (int32_t)(endP - srcP),
                                  FALSE, FALSE,
                                  &status);
    }
    if (U_FAILURE(status)) {
#ifdef UCOL_DEBUG
        fprintf(stderr, "collIterNormalize(), unorm_decompose() failed, status = %s\n", u_errorName(status));
#endif
        return;
    }

    if(collationSource->writableBuffer != collationSource->stackWritableBuffer) {
        collationSource->flags |= UCOL_ITER_ALLOCATED;
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
    UChar       c, c2;
    const UChar *srcP, *endP;
    uint8_t     leadingCC;
    uint8_t     prevTrailingCC = 0;
    uint16_t    fcd;
    UBool       needNormalize = FALSE;

    srcP = collationSource->pos-1;

    if (collationSource->flags & UCOL_ITER_HASLEN) {
        endP = collationSource->endp;
    } else {
        endP = NULL;
    }

    // Get the trailing combining class of the current character.  If it's zero,
    //   we are OK.
    c = *srcP++;
    /* trie access */
    fcd = unorm_getFCD16(fcdTrieIndex, c);
    if (fcd != 0) {
        if (UTF_IS_FIRST_SURROGATE(c)) {
            if ((endP == NULL || srcP != endP) && UTF_IS_SECOND_SURROGATE(c2=*srcP)) {
                ++srcP;
                fcd = unorm_getFCD16FromSurrogatePair(fcdTrieIndex, fcd, c2);
            } else {
                fcd = 0;
            }
        }

        prevTrailingCC = (uint8_t)(fcd & LAST_BYTE_MASK_);

        if (prevTrailingCC != 0) {
            // The current char has a non-zero trailing CC.  Scan forward until we find
            //   a char with a leading cc of zero.
            while (endP == NULL || srcP != endP)
            {
                const UChar *savedSrcP = srcP;

                c = *srcP++;
                /* trie access */
                fcd = unorm_getFCD16(fcdTrieIndex, c);
                if (fcd != 0 && UTF_IS_FIRST_SURROGATE(c)) {
                    if ((endP == NULL || srcP != endP) && UTF_IS_SECOND_SURROGATE(c2=*srcP)) {
                        ++srcP;
                        fcd = unorm_getFCD16FromSurrogatePair(fcdTrieIndex, fcd, c2);
                    } else {
                        fcd = 0;
                    }
                }
                leadingCC = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
                if (leadingCC == 0) {
                    srcP = savedSrcP;      // Hit char that is not part of combining sequence.
                                           //   back up over it.  (Could be surrogate pair!)
                    break;
                }

                if (leadingCC < prevTrailingCC) {
                    needNormalize = TRUE;
                }

                prevTrailingCC = (uint8_t)(fcd & LAST_BYTE_MASK_);
            }
        }
    }

    collationSource->fcdPosition = (UChar *)srcP;

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

        if ((collationSource->flags & (UCOL_ITER_HASLEN | UCOL_ITER_INNORMBUF | UCOL_ITER_NORM | UCOL_HIRAGANA_Q)) == 0)
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
                    // Ran off end of main string. backing up one character.
                    collationSource->pos--;
                    return UCOL_NO_MORE_CES;
                }
                else
                {
                    // Hit null in the normalize side buffer.
                    // Usually this means the end of the normalized data,
                    // except for one odd case: a null followed by combining chars,
                    //   which is the case if we are at the start of the buffer.
                    if (collationSource->pos == collationSource->writableBuffer+1) {
                        break;
                    }

                    //  Null marked end of side buffer.
                    //   Revert to the main string and
                    //   loop back to top to try again to get a character.
                    collationSource->pos   = collationSource->fcdPosition;
                    collationSource->flags = collationSource->origFlags;
                    continue;
                }
            }
        }

        if(collationSource->flags&UCOL_HIRAGANA_Q) {
          if(ch>=0x3040 && ch<=0x309f) {
            collationSource->flags |= UCOL_WAS_HIRAGANA;
          } else {
            collationSource->flags &= ~UCOL_WAS_HIRAGANA;
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
              order = ucol_prv_getSpecialCE(coll, ch, order, collationSource, status);
          }
      }
      else
      {
          order = ucmpe32_get(coll->mapping, ch);                             /* we'll go for slightly slower trie */
          if(order > UCOL_NOT_FOUND) {                                       /* if a CE is special                */
              order = ucol_prv_getSpecialCE(coll, ch, order, collationSource, status);    /* and try to get the special CE     */
          }
          if(order == UCOL_NOT_FOUND) {   /* We couldn't find a good CE in the tailoring */
            /* if we got here, the codepoint MUST be over 0xFF - so we look directly in the trie */
            order = ucmpe32_get(UCA->mapping, ch);

            if(order > UCOL_NOT_FOUND) { /* UCA also gives us a special CE */
              order = ucol_prv_getSpecialCE(UCA, ch, order, collationSource, status);
            }
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
static
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
            data->flags |= UCOL_ITER_ALLOCATED;
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
    const UChar *src, *start;
    UChar       c, c2;
    uint8_t     leadingCC;
    uint8_t     trailingCC = 0;
    uint16_t    fcd;
    UBool       result = FALSE;

    start = data->string;
    src = data->pos + 1;

    /* Get the trailing combining class of the current character. */
    c = *--src;
    if (!UTF_IS_SURROGATE(c)) {
        fcd = unorm_getFCD16(fcdTrieIndex, c);
    } else if (UTF_IS_SECOND_SURROGATE(c) && start < src && UTF_IS_FIRST_SURROGATE(c2 = *(src - 1))) {
        --src;
        fcd = unorm_getFCD16(fcdTrieIndex, c2);
        if (fcd != 0) {
            fcd = unorm_getFCD16FromSurrogatePair(fcdTrieIndex, fcd, c);
        }
    } else /* unpaired surrogate */ {
        fcd = 0;
    }

    leadingCC = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);

    if (leadingCC != 0) {
        /*
        The current char has a non-zero leading combining class.
        Scan backward until we find a char with a trailing cc of zero.
        */
        for (;;)
        {
            if (start == src) {
                data->fcdPosition = NULL;
                return result;
            }

            c = *--src;
            if (!UTF_IS_SURROGATE(c)) {
                fcd = unorm_getFCD16(fcdTrieIndex, c);
            } else if (UTF_IS_SECOND_SURROGATE(c) && start < src && UTF_IS_FIRST_SURROGATE(c2 = *(src - 1))) {
                --src;
                fcd = unorm_getFCD16(fcdTrieIndex, c2);
                if (fcd != 0) {
                    fcd = unorm_getFCD16FromSurrogatePair(fcdTrieIndex, fcd, c);
                }
            } else /* unpaired surrogate */ {
                fcd = 0;
            }

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

    data->fcdPosition = (UChar *)src;

    return result;
}

/**
* Determines if we are at the start of the data string in the backwards
* collation iterator
* @param data collation iterator
* @return TRUE if we are at the start
*/
inline UBool isAtStartPrevIterate(collIterate *data) {
    return (data->pos == data->string) ||
            ((data->flags & UCOL_ITER_INNORMBUF) &&
            *(data->pos - 1) == 0 && data->fcdPosition == NULL);
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
            data->CEpos = data->toReturn;
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
            if (data->flags & UCOL_ITER_HASLEN) {
                /*
                Normal path for strings when length is specified.
                Not in side buffer because it is always null terminated.
                */
                if (data->pos <= data->string) {
                    /* End of the main source string */
                    return UCOL_NO_MORE_CES;
                }
                data->pos --;
                ch = *data->pos;
            }
            else {
                data->pos --;
                ch = *data->pos;
                /* we are in the side buffer. */
                if (ch == 0) {
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

            if(data->flags&UCOL_HIRAGANA_Q) {
              if(ch>=0x3040 && ch<=0x309f) {
                data->flags |= UCOL_WAS_HIRAGANA;
              } else {
                data->flags &= ~UCOL_WAS_HIRAGANA;
              }
            }
            
            /*
            * got a character to determine if there's fcd and/or normalization 
            * stuff to do.
            * if the current character is not fcd.
            * if current character is at the start of the string
            * Trailing combining class == 0.
            * Note if pos is in the writablebuffer, norm is always 0
            */
            if (ch < ZERO_CC_LIMIT_ || 
                (data->flags & UCOL_ITER_NORM) == 0 ||
                (data->fcdPosition != NULL && data->fcdPosition <= data->pos) 
                || data->string == data->pos) {
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

        /* attempt to handle contractions, after removal of the backwards
        contraction
        */
        if (ucol_contractionEndCP(ch, coll) && !isAtStartPrevIterate(data)) {
            result = ucol_prv_getSpecialPrevCE(coll, ch, UCOL_CONTRACTION, data, status);
        }
        else {
            if (ch <= 0xFF) {
              result = coll->latinOneMapping[ch];
              if (result > UCOL_NOT_FOUND) {
                    result = ucol_prv_getSpecialPrevCE(coll, ch, result, data, status);
              }
            }
            else {
                if ((data->flags & UCOL_ITER_INNORMBUF) == 0 &&
                    UCOL_ISTHAIBASECONSONANT(ch) && data->pos > data->string &&
                    UCOL_ISTHAIPREVOWEL(*(data->pos -1)))
                {
                    result = UCOL_THAI;
                }
                else {
                    result = ucmpe32_get(coll->mapping, ch);
                }
                if (result > UCOL_NOT_FOUND) {
                    result = ucol_prv_getSpecialPrevCE(coll, ch, result, data, status);
                }
                if (result == UCOL_NOT_FOUND) {
                  if (!isAtStartPrevIterate(data) &&
                      ucol_contractionEndCP(ch, data->coll)) {
                      result = UCOL_CONTRACTION;
                  }
                  else {
                        result = ucmpe32_get(UCA->mapping, ch);
                  }

                  if (result > UCOL_NOT_FOUND) {
                    result = ucol_prv_getSpecialPrevCE(UCA, ch, result, data, status);
                  }
                }
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
                               int32_t length)
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
        data->flags |= UCOL_ITER_ALLOCATED;
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
    if ((data->flags & (UCOL_ITER_NORM | UCOL_ITER_INNORMBUF)) == 0 ) {
         /* if no normalization and not in buffer. */
         return *(data->pos ++);
    }

    UChar  *pEndWritableBuffer = NULL;
    UBool  innormbuf = (UBool)(data->flags & UCOL_ITER_INNORMBUF);
    if ((innormbuf && *data->pos != 0) ||
        (data->fcdPosition != NULL && !innormbuf &&
        data->pos < data->fcdPosition)) {
        /*
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
            int32_t length = data->fcdPosition - data->pos + 1;
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
    well, so that the whole chunk of codes in the getNextCE, ucol_prv_getSpecialCE does
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
static
uint32_t getDiscontiguous(const UCollator *coll, collIterate *source,
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

inline uint32_t getImplicit(UChar32 cp, collIterate *collationSource, uint32_t hanFixup) {
  if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
      return 0;  /* illegal code value, use completely ignoreable! */
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
      r = 0xEC030300 - hanFixup + (last2 << 24) + (last1 << 16) + (last0 << 9);
  } else {
      int32_t last1 = last0 / IMPLICIT_LAST_COUNT2_;
      last0 %= IMPLICIT_LAST_COUNT2_;
      int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
      last1 %= IMPLICIT_OTHER_COUNT_;
      r = 0xEF030303 - hanFixup + (last2 << 16) + (last1 << 8) + (last0 * IMPLICIT_LAST2_MULTIPLIER_);
  } 
  *(collationSource->CEpos++) = ((r & 0x0000FFFF)<<16) | 0x000000C0;
  return (r & UCOL_PRIMARYMASK) | 0x00000505; // This was 'order'
}

static
inline UChar getPrevNormalizedChar(collIterate *data);

/* This function handles the special CEs like contractions, expansions, surrogates, Thai */
/* It is called by getNextCE */

uint32_t ucol_prv_getSpecialCE(const UCollator *coll, UChar ch, uint32_t CE, collIterate *source, UErrorCode *status) {
  collIterateState entryState;
  backupState(source, &entryState);

  //UChar *entryPos = source->pos;
  for (;;) {
    // This loop will repeat only in the case of contractions, and only when a contraction
    //   is found and the first CE resulting from that contraction is itself a special
    //   (an expansion, for example.)  All other special CE types are fully handled the
    //   first time through, and the loop exits.

    const uint32_t *CEOffset = NULL;
    switch(getCETag(CE)) {
    case NOT_FOUND_TAG:
      /* This one is not found, and we'll let somebody else bother about it... no more games */
      return CE;
    case SURROGATE_TAG:
      /* we encountered a leading surrogate. We shall get the CE by using the following code unit */
      /* two things can happen here: next code point can be a trailing surrogate - we will use it */
      /* to retrieve the CE, or it is not a trailing surrogate (or the string is done). In that case */
      /* we return 0 (completely ignorable - per UCA specification */
      {
        UChar trail;
        collIterateState state;
        backupState(source, &state);
        if (collIter_eos(source) || !(UTF16_IS_TRAIL((trail = getNextNormalizedChar(source))))) {
          return 0;
        } else {
          CE = ucmpe32_getSurrogate(coll->mapping, CE, trail);
          if(CE == UCOL_NOT_FOUND) { // there are tailored surrogates in this block, but not this one.
            // We need to backup
            loadState(source, &state, TRUE);
          }
        }
      }
      break;
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
    case SPEC_PROC_TAG:
      {
        // Special processing is getting a CE that is preceded by a certain prefix
        // Currently this is only needed for optimizing Japanese length and iteration marks.
        // When we encouter a special processing tag, we go backwards and try to see if 
        // we have a match. 
        // Contraction tables are used - so the whole process is not unlike contraction.
        // prefix data is stored backwards in the table.
        const UChar *UCharOffset;
        UChar schar, tchar;
        //UChar32 normOutput = 0;
        collIterateState prefixState;
        backupState(source, &prefixState);
        loadState(source, &entryState, TRUE);
        source->pos--;

        //UChar  *sourcePointer = --entryPos; //source->pos; // We want to look at the point where we entered - actually one
        // before that...

        for(;;) {
        // This loop will run once per source string character, for as long as we
        //  are matching a potential contraction sequence                  

          // First we position ourselves at the begining of contraction sequence 
          const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);
          if (source->pos == source->string ||
              ((source->flags & UCOL_ITER_INNORMBUF) &&
              *(source->pos - 1) == 0 && source->fcdPosition == NULL)) {
          // if(sourcePointer == source->string) {
            CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
            break;
          }
          schar = getPrevNormalizedChar(source);
          source->pos--;
          //schar = *(--sourcePointer);

          while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
            UCharOffset++;
          }

          if (schar == tchar) {
              // Found the source string char in the table.
              //  Pick up the corresponding CE from the table.
              CE = *(coll->contractionCEs +
                  (UCharOffset - coll->contractionIndex));
          }
          else
          {
              // Source string char was not in the table.
              //   We have not found the prefix.
              CE = *(coll->contractionCEs +
                  (ContractionStart - coll->contractionIndex));
          }

          if(!isPrefix(CE)) {
              // The source string char was in the contraction table, and the corresponding
              //   CE is not a prefix CE.  We found the prefix, break
              //   out of loop, this CE will end up being returned.  This is the normal
              //   way out of prefix handling when the source actually contained
              //   the prefix.
              break;
          }
        }

      loadState(source, &prefixState, TRUE);
      break;
      }
    case CONTRACTION_TAG:
      {
      /* This should handle contractions */
      collIterateState state;
      backupState(source, &state);
      uint32_t firstCE = UCOL_NOT_FOUND;
      const UChar *UCharOffset;
      UChar schar, tchar;

      for (;;) {
        /* This loop will run once per source string character, for as long as we     */
        /*  are matching a potential contraction sequence                  */

        /* First we position ourselves at the begining of contraction sequence */
        const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

        if (collIter_eos(source)) {
            // Ran off the end of the source string.
            CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
            // So we'll pick whatever we have at the point...
            if (CE == UCOL_NOT_FOUND) {
                // back up the source over all the chars we scanned going into this contraction.
                CE = firstCE;  
                loadState(source, &state, TRUE);
            }
            break;
        }

        uint8_t maxCC = (uint8_t)(*(UCharOffset)&0xFF); /*get the discontiguos stuff */ /* skip the backward offset, see above */
        uint8_t allSame = (uint8_t)(*(UCharOffset++)>>8);

        schar = getNextNormalizedChar(source);
        while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
          UCharOffset++;
        }

        if (schar == tchar) {
            // Found the source string char in the contraction table.
            //  Pick up the corresponding CE from the table.
            CE = *(coll->contractionCEs +
                (UCharOffset - coll->contractionIndex));
        }
        else
        {
            // Source string char was not in contraction table.
            //   Unless we have a discontiguous contraction, we have finished
            //   with this contraction.
            uint8_t sCC;
            if (schar < 0x300 ||   
                maxCC == 0 ||
                (sCC = i_getCombiningClass(schar, coll)) == 0 ||
                sCC>maxCC || 
                (allSame != 0 && sCC == maxCC) ||
                collIter_eos(source)) {
                    //  Contraction can not be discontiguous.  
                    source->pos --;     // back up the source string pointer by one, 
                                        //  because  the character we just looked at was
                                        //  not part of the contraction.   */
                    CE = *(coll->contractionCEs +
                        (ContractionStart - coll->contractionIndex));
            } else {
                //
                // Contraction is possibly discontiguous.
                //   Scan more of source string looking for a match
                //
                UChar tempchar;
                /* find the next character if schar is not a base character
                    and we are not yet at the end of the string */
                tempchar = getNextNormalizedChar(source);
                source->pos --;
                if (i_getCombiningClass(tempchar, coll) == 0) {
                    source->pos --;
                    /* Spit out the last char of the string, wasn't tasty enough */
                    CE = *(coll->contractionCEs +
                        (ContractionStart - coll->contractionIndex));
                } else {
                    CE = getDiscontiguous(coll, source, ContractionStart);
                }
            }
        }

        if(CE == UCOL_NOT_FOUND) {
            /* The Source string did not match the contraction that we were checking.  */
            /*  Back up the source position to undo the effects of having partially    */
            /*   scanned through what ultimately proved to not be a contraction.       */
          loadState(source, &state, TRUE);
          CE = firstCE;
          break;
        }
        
        if(!isContraction(CE)) {
            // The source string char was in the contraction table, and the corresponding
            //   CE is not a contraction CE.  We completed the contraction, break
            //   out of loop, this CE will end up being returned.  This is the normal
            //   way out of contraction handling when the source actually contained
            //   the contraction.
            break;
        }
        

        // The source string char was in the contraction table, and the corresponding
        //   CE is IS  a contraction CE.  We will continue looping to check the source
        //   string for the remaining chars in the contraction.
        uint32_t tempCE = *(coll->contractionCEs + (ContractionStart - coll->contractionIndex));
        if(tempCE != UCOL_NOT_FOUND) {
            // We have scanned a a section of source string for which there is a
            //  CE from the contraction table.  Remember the CE and scan position, so 
            //  that we can return to this point if further scanning fails to
            //  match a longer contraction sequence.
            firstCE = tempCE;
            backupState(source, &state);
            state.pos --;
        }
      }
      break;
      }
    case EXPANSION_TAG:
      {
      /* This should handle expansion. */
      /* NOTE: we can encounter both continuations and expansions in an expansion! */
      /* I have to decide where continuations are going to be dealt with */
      uint32_t size;
      uint32_t i;    /* general counter */
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
      }
    /* TODO: */
    /* various implicits optimization */
    /* need to fill out the collation table for them to work */
    case CJK_IMPLICIT_TAG:    /* 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D*/
      return getImplicit(ch, source, 0x04000000);
    case IMPLICIT_TAG:        /* everything that is not defined otherwise */
      /* UCA is filled with these. Tailorings are NOT_FOUND */
      return getImplicit(ch, source, 0);
    case TRAIL_SURROGATE_TAG: /* DC00-DFFF*/
      return 0; /* broken surrogate sequence */
    case LEAD_SURROGATE_TAG:  /* D800-DBFF*/
      UChar nextChar;
      if( (((source->flags & UCOL_ITER_HASLEN) == 0 ) || (source->pos<source->endp)) &&
        UTF_IS_SECOND_SURROGATE((nextChar=*source->pos))) {
        uint32_t cp = ((((uint32_t)ch)<<10UL)+(nextChar)-(((uint32_t)0xd800<<10UL)+0xdc00-0x10000));
        source->pos++;
        return getImplicit(cp, source, 0);
      } else {
        return 0; /* completely ignorable */
      }
    case HANGUL_SYLLABLE_TAG: /* AC00-D7AF*/
      {
        const uint32_t
          SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7;
        //const uint32_t LCount = 19;
        const uint32_t VCount = 21; 
        const uint32_t TCount = 28;
        //const uint32_t NCount = VCount * TCount;   // 588
        //const uint32_t SCount = LCount * NCount;   // 11172
        uint32_t L = ch - SBase;

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
        if (!source->coll->image->jamoSpecial) { // FAST PATH

          *(source->CEpos++) = ucmpe32_get(UCA->mapping, V);
          if (T != TBase) {
              *(source->CEpos++) = ucmpe32_get(UCA->mapping, T);
          }

          return ucmpe32_get(UCA->mapping, L); // return first one

        } else { // Jamo is Special
          // TODO: if we are already in the normalization buffer,
          // and we encounter a Hangul with Jamo special set to 
          // TRUE, we currently fallback to old implementation.
          // What needs to be done is to replace the Hangul by 
          // 2 or 3 Jamos and move the rest of the normalization
          // buffer accordingly. 
          // But Markus says it is guaranteed that we won't be in
          // the normalization buffer if something like this happens,
          // so I will remove the bail out case
          // Move Jamos into normalization buffer
          source->writableBuffer[0] = (UChar)L;
          source->writableBuffer[1] = (UChar)V;
          if (T != TBase) {
            source->writableBuffer[2] = (UChar)T;
            source->writableBuffer[3] = 0;
          } else {
            source->writableBuffer[2] = 0;
          }

          source->fcdPosition       = source->pos;   // Indicate where to continue in main input string
                                                         //   after exhausting the writableBuffer
          source->pos   = source->writableBuffer;
          source->origFlags         = source->flags;
          source->flags            |= UCOL_ITER_INNORMBUF;
          source->flags            &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);

          return(UCOL_IGNORABLE);
        }
      }
    case CHARSET_TAG:
    /* not yet implemented */
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
static
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

inline uint32_t getPrevImplicit(UChar32 cp, collIterate *collationSource, uint32_t hanFixup) {
      if ((cp & 0xFFFE) == 0xFFFE || (0xD800 <= cp && cp <= 0xDC00)) {
          return 0;  /* illegal code value, use completely ignoreable! */
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
          r = 0xEC030300 - hanFixup + (last2 << 24) + (last1 << 16) + (last0 << 9);
      } else {
          int32_t last1 = last0 / IMPLICIT_LAST_COUNT2_;
          last0 %= IMPLICIT_LAST_COUNT2_;
          int32_t last2 = last1 / IMPLICIT_OTHER_COUNT_;
          last1 %= IMPLICIT_OTHER_COUNT_;
          r = 0xEF030303 - hanFixup + (last2 << 16) + (last1 << 8) +
              (last0 * IMPLICIT_LAST2_MULTIPLIER_);
      }
      *(collationSource->CEpos++) = (r & UCOL_PRIMARYMASK) | 0x00000505;
      collationSource->toReturn = collationSource->CEpos;
      return ((r & 0x0000FFFF)<<16) | 0x000000C0;
}

/**
* This function handles the special CEs like contractions, expansions,
* surrogates, Thai.
* It is called by both getPrevCE
*/
uint32_t ucol_prv_getSpecialPrevCE(const UCollator *coll, UChar ch, uint32_t CE,
                          collIterate *source,
                          UErrorCode *status)
{
  const uint32_t *CEOffset    = NULL;
        UChar    *UCharOffset = NULL;
        UChar    schar;
  const UChar    *constart    = NULL;
        uint32_t size;
        UChar    buffer[UCOL_MAX_BUFFER];
        uint32_t *endCEBuffer;
        UChar   *strbuffer;

  for(;;)
  {
    /* the only ces that loops are thai and contractions */
    switch (getCETag(CE))
    {
    case NOT_FOUND_TAG:  /* this tag always returns */
      return CE;
    case SURROGATE_TAG:  /* This is a surrogate pair */
      /* essentialy an engaged lead surrogate. */
      /* if you have encountered it here, it means that a */
      /* broken sequence was encountered and this is an error */
      return 0;
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
          } else {
            source->fcdPosition       = source->pos-2;
          }

          source->pos               = tempbuffer;
          source->origFlags         = source->flags;
          source->flags            |= UCOL_ITER_INNORMBUF;
          source->flags            &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);

          //CE = UCOL_IGNORABLE;
          return(UCOL_IGNORABLE);
      }
      break;
    case SPEC_PROC_TAG:
      {
        // Special processing is getting a CE that is preceded by a certain prefix
        // Currently this is only needed for optimizing Japanese length and iteration marks.
        // When we encouter a special processing tag, we go backwards and try to see if 
        // we have a match. 
        // Contraction tables are used - so the whole process is not unlike contraction.
        // prefix data is stored backwards in the table.
        const UChar *UCharOffset;
        UChar schar, tchar;
        collIterateState prefixState;
        backupState(source, &prefixState);
        //UChar *sourcePointer = source->pos;
        //UChar32 normOutput = 0;
        for(;;) {
        // This loop will run once per source string character, for as long as we
        //  are matching a potential contraction sequence                  

          // First we position ourselves at the begining of contraction sequence 
          const UChar *ContractionStart = UCharOffset = (UChar *)coll->image+getContractOffset(CE);

          if (source->pos == source->string ||
              ((source->flags & UCOL_ITER_INNORMBUF) &&
              *(source->pos - 1) == 0 && source->fcdPosition == NULL)) {
          //if(sourcePointer == source->string) {
            CE = *(coll->contractionCEs + (UCharOffset - coll->contractionIndex));
            break;
          }
          schar = getPrevNormalizedChar(source);
          source->pos--;
          //schar = *(--sourcePointer);

          while(schar > (tchar = *UCharOffset)) { /* since the contraction codepoints should be ordered, we skip all that are smaller */
            UCharOffset++;
          }

          if (schar == tchar) {
              // Found the source string char in the table.
              //  Pick up the corresponding CE from the table.
              CE = *(coll->contractionCEs +
                  (UCharOffset - coll->contractionIndex));
          }
          else
          {
              // Source string char was not in the table.
              //   We have not found the prefix.
              CE = *(coll->contractionCEs +
                  (ContractionStart - coll->contractionIndex));
          }

          if(!isPrefix(CE)) {
              // The source string char was in the contraction table, and the corresponding
              //   CE is not a prefix CE.  We found the prefix, break
              //   out of loop, this CE will end up being returned.  This is the normal
              //   way out of prefix handling when the source actually contained
              //   the prefix.
              break;
          }
        }
      loadState(source, &prefixState, TRUE);
      break;
      }

    case CONTRACTION_TAG:
        /* to ensure that the backwards and forwards iteration matches, we
        take the current region of most possible match and pass it through
        the forward iteration. this will ensure that the obstinate problem of
        overlapping contractions will not occur.
        */
        schar = *(source->pos);
        constart = (UChar *)coll->image + getContractOffset(CE);
        if (isAtStartPrevIterate(source)
            /* commented away contraction end checks after adding the checks
            in getPrevCE  */) {
            /* start of string or this is not the end of any contraction */
            CE = *(coll->contractionCEs +
                     (constart - coll->contractionIndex));
            break;
        }
        strbuffer = buffer;
        UCharOffset = strbuffer + (UCOL_MAX_BUFFER - 1);
        *(UCharOffset --) = 0;
        while (ucol_unsafeCP(schar, coll)) {
            *(UCharOffset) = schar;
            UCharOffset --;
            schar = getPrevNormalizedChar(source);
            source->pos --;
            if (UCharOffset + 1 == buffer) {
                /* we have exhausted the buffer */
                int32_t newsize = source->pos - source->string + 1;
                strbuffer = (UChar *)uprv_malloc(sizeof(UChar) *
                                             (newsize + UCOL_MAX_BUFFER));
                UCharOffset = strbuffer + newsize;
                uprv_memcpy(UCharOffset, buffer,
                                             UCOL_MAX_BUFFER * sizeof(UChar));
                UCharOffset --;
            }
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
        endCEBuffer = source->CEs + UCOL_EXPAND_CE_BUFFER_SIZE;
        while (CE != UCOL_NO_MORE_CES) {
            *(source->CEpos ++) = CE;
            if (source->CEpos == endCEBuffer) {
                /* ran out of CE space, bail.
                there's no guarantee of the right character position after
                this bail*/
                *status = U_BUFFER_OVERFLOW_ERROR;
                source->CEpos = source->CEs;
                return UCOL_NULLORDER;
            }
            CE = ucol_IGetNextCE(coll, &temp, status);
        }
        freeHeapWritableBuffer(&temp);
        if (strbuffer != buffer) {
            uprv_free(strbuffer);
        }
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
    case HANGUL_SYLLABLE_TAG: /* AC00-D7AF*/
      {
        const uint32_t
          SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7;
        //const uint32_t LCount = 19; 
        const uint32_t VCount = 21;
        const uint32_t TCount = 28;
        //const uint32_t NCount = VCount * TCount;   /* 588 */
        //const uint32_t SCount = LCount * NCount;   /* 11172 */

        uint32_t L = ch - SBase;
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
        if (!source->coll->image->jamoSpecial)
        {
          *(source->CEpos ++) = ucmpe32_get(UCA->mapping, L);
          *(source->CEpos ++) = ucmpe32_get(UCA->mapping, V);
          if (T != TBase)
            *(source->CEpos ++) = ucmpe32_get(UCA->mapping, T);

          source->toReturn = source->CEpos - 1;
          return *(source->toReturn);
        } else {
          // TODO: if we are already in the normalization buffer,
          // and we encounter a Hangul with Jamo special set to 
          // TRUE, we currently fallback to old implementation.
          // What needs to be done is to replace the Hangul by 
          // 2 or 3 Jamos and move the rest of the normalization
          // buffer accordingly. 
          // But Markus says it is guaranteed that we won't be in
          // the normalization buffer if something like this happens,
          // so I will remove the bail out case
          // Move Jamos into normalization buffer
          /*
          Move the Jamos into the
          normalization buffer 
          */
          UChar *tempbuffer = source->writableBuffer +
                              (source->writableBufSize - 1);
          *(tempbuffer) = 0;
          if (T != TBase) {
            *(tempbuffer - 1) = (UChar)T;
            *(tempbuffer - 2) = (UChar)V;
            *(tempbuffer - 3) = (UChar)L;
            *(tempbuffer - 4) = 0;
          } else {
            *(tempbuffer - 1) = (UChar)V;
            *(tempbuffer - 2) = (UChar)L;
            *(tempbuffer - 3) = 0;
          }

          /*
          Indicate where to continue in main input string after exhausting
          the writableBuffer
          */
          if (source->pos  == source->string) {
            source->fcdPosition = NULL;
          } else {
            source->fcdPosition       = source->pos-1;
          }

          source->pos               = tempbuffer;
          source->origFlags         = source->flags;
          source->flags            |= UCOL_ITER_INNORMBUF;
          source->flags            &= ~(UCOL_ITER_NORM | UCOL_ITER_HASLEN);

          return(UCOL_IGNORABLE);
        }
      }
    case LEAD_SURROGATE_TAG:  /* D800-DBFF*/
      return 0; /* broken surrogate sequence */
    case TRAIL_SURROGATE_TAG: /* DC00-DFFF*/
    {
      UChar32 cp = 0;
      UChar  prevChar;
      UChar *prev;
      if (isAtStartPrevIterate(source)) {
          /* we are at the start of the string, wrong place to be at */
          return 0;
      }
      if (source->pos != source->writableBuffer) {
          prev     = source->pos - 1;
      } else {
          prev     = source->fcdPosition;
      }
      prevChar = *prev;

      /* Handles Han and Supplementary characters here.*/
      if (UTF_IS_FIRST_SURROGATE(prevChar)) {
        cp = ((((uint32_t)prevChar)<<10UL)+(ch)-(((uint32_t)0xd800<<10UL)+0xdc00-0x10000));
        source->pos = prev;
      } else {
        return 0; /* completely ignorable */
      }
      return getPrevImplicit(cp, source, 0);
    }
    case CJK_IMPLICIT_TAG:    /* 0x3400-0x4DB5, 0x4E00-0x9FA5, 0xF900-0xFA2D*/
      return getPrevImplicit(ch, source, 0x04000000);
    case IMPLICIT_TAG:        /* everything that is not defined otherwise */
      return getPrevImplicit(ch, source, 0);
      /* UCA is filled with these. Tailorings are NOT_FOUND */
    /* not yet implemented */
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
static
uint8_t *reallocateBuffer(uint8_t **secondaries, uint8_t *secStart, uint8_t *second, uint32_t *secSize, uint32_t newSize, UErrorCode *status) {
#ifdef UCOL_DEBUG
  fprintf(stderr, ".");
#endif
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

/**
 * Merge two sort keys.
 * This is useful, for example, to combine sort keys from first and last names
 * to sort such pairs.
 * Merged sort keys consider on each collation level the first part first entirely,
 * then the second one.
 * It is possible to merge multiple sort keys by consecutively merging
 * another one with the intermediate result.
 *
 * The length of the merge result is the sum of the lengths of the input sort keys
 * minus 1.
 *
 * @param src1 the first sort key
 * @param src1Length the length of the first sort key, including the zero byte at the end;
 *        can be -1 if the function is to find the length
 * @param src2 the second sort key
 * @param src2Length the length of the second sort key, including the zero byte at the end;
 *        can be -1 if the function is to find the length
 * @param dest the buffer where the merged sort key is written,
 *        can be NULL if destCapacity==0
 * @param destCapacity the number of bytes in the dest buffer
 * @return the length of the merged sort key, src1Length+src2Length-1;
 *         can be larger than destCapacity, or 0 if an error occurs (only for illegal arguments),
 *         in which cases the contents of dest is undefined
 *
 * @draft
 */
int32_t
ucol_mergeSortkeys(const uint8_t *src1, int32_t src1Length,
                   const uint8_t *src2, int32_t src2Length,
                   uint8_t *dest, int32_t destCapacity) {
    int32_t destLength;
    uint8_t b;

    /* check arguments */
    if( src1==NULL || src1Length<-2 || src1Length==0 || (src1Length>0 && src1[src1Length-1]!=0) ||
        src2==NULL || src2Length<-2 || src2Length==0 || (src2Length>0 && src2[src2Length-1]!=0) ||
        destCapacity<0 || (destCapacity>0 && dest==NULL)
    ) {
        /* error, attempt to write a zero byte and return 0 */
        if(dest!=NULL && destCapacity>0) {
            *dest=0;
        }
        return 0;
    }

    /* check lengths and capacity */
    if(src1Length<0) {
        src1Length=(int32_t)uprv_strlen((const char *)src1)+1;
    }
    if(src2Length<0) {
        src2Length=(int32_t)uprv_strlen((const char *)src2)+1;
    }

    destLength=src1Length+src2Length-1;
    if(destLength>destCapacity) {
        /* the merged sort key does not fit into the destination */
        return destLength;
    }

    /* merge the sort keys with the same number of levels */
    while(*src1!=0 && *src2!=0) { /* while both have another level */
        /* copy level from src1 not including 00 or 01 */
        while((b=*src1)>=2) {
            ++src1;
            *dest++=b;
        }

        /* add a 02 merge separator */
        *dest++=2;

        /* copy level from src2 not including 00 or 01 */
        while((b=*src2)>=2) {
            ++src2;
            *dest++=b;
        }

        /* if both sort keys have another level, then add a 01 level separator and continue */
        if(*src1==1 && *src2==1) {
            ++src1;
            ++src2;
            *dest++=1;
        }
    }

    /*
     * here, at least one sort key is finished now, but the other one
     * might have some contents left from containing more levels;
     * that contents is just appended to the result
     */
    if(*src1!=0) {
        /* src1 is not finished, therefore *src2==0, and src1 is appended */
        src2=src1;
    }
    /* append src2, "the other, unfinished sort key" */
    uprv_strcpy((char *)dest, (const char *)src2);

    /* trust that neither sort key contained illegally embedded zero bytes */
    return destLength;
}

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
U_CFUNC int32_t
ucol_getSortKeyWithAllocation(const UCollator *coll,
                              const UChar *source, int32_t sourceLength,
                              uint8_t **pResult,
                              UErrorCode *pErrorCode) {
    *pResult = 0;
    return coll->sortKeyGen(coll, source, sourceLength, pResult, 0, TRUE, pErrorCode);
}

#define UCOL_FSEC_BUF_SIZE 256

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
    //UBool  qShifted = shifted  && (compareQuad == 0);
    UBool  doHiragana = (coll->hiraganaQ == UCOL_ON) && (compareQuad == 0);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);
    uint8_t fSecsBuff[UCOL_FSEC_BUF_SIZE];
    uint8_t *fSecs = fSecsBuff;
    uint32_t fSecsLen = 0, fSecsMaxLen = UCOL_FSEC_BUF_SIZE;
    uint8_t *frenchStartPtr = NULL, *frenchEndPtr = NULL;

    uint32_t variableTopValue = coll->variableTopValue;
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)((coll->variableTopValue>>8)+1); 
    if(doHiragana) {
      UCOL_COMMON_BOT4++;
      /* allocate one more space for hiragana */
    }
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    uint32_t order = UCOL_NO_MORE_CES;
    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
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

          if(order == UCOL_NO_MORE_CES) {
              break;
          }

          if(order == 0) {
            continue;
          }

          notIsContinuation = !isContinuation(order);


          if(notIsContinuation) {
            tertiary = (uint8_t)((order & UCOL_BYTE_SIZE_MASK));
          } else {
            tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
          }
          secondary = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
          primary2 = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
          primary1 = (uint8_t)(order >> 8);


          if(shifted && ((notIsContinuation && order <= variableTopValue && primary1 > 0)
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
                fSecs[fSecsLen++] = secondary;
                if(fSecsLen == fSecsMaxLen) {
                  if(fSecs == fSecsBuff) {
                    fSecs = (uint8_t *)uprv_malloc(2*fSecsLen);
                  } else {
                    fSecs = (uint8_t *)uprv_realloc(fSecs, 2*fSecsLen);
                  }
                  fSecsMaxLen *= 2;
                }
                if(notIsContinuation) {
                  if (frenchStartPtr != NULL) {
                      /* reverse secondaries from frenchStartPtr up to frenchEndPtr */
                    uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
                    frenchStartPtr = NULL;
                  }
                } else {
                  if (frenchStartPtr == NULL) {
                    frenchStartPtr = fSecs+fSecsLen-2;
                  }
                  frenchEndPtr = fSecs+fSecsLen-1;
                }
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

            if(/*qShifted*/(compareQuad==0)  && notIsContinuation) {
              if(s->flags & UCOL_WAS_HIRAGANA) { // This was Hiragana and we need to note it
                if(c4>0) { // Close this part
                  currentSize += (c4/UCOL_BOT_COUNT4)+1;
                  c4 = 0;
                }
                currentSize++; // Add the Hiragana
              } else { // This wasn't Hiragana, so we can continue adding stuff
                c4++;
              }
            }

          }
    }

    if(!isFrenchSec){
      if(c2 > 0) {
        currentSize += (c2/(uint32_t)UCOL_BOT_COUNT2)+1;
      }
    } else {
      uint32_t i = 0;
      if(frenchStartPtr != NULL) {
        uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
      }
      for(i = 0; i<fSecsLen; i++) {
        secondary = *(fSecs+fSecsLen-i-1);
        /* This is compression code. */
        if (secondary == UCOL_COMMON2) {
          ++c2;
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
      }
      if(c2 > 0) {
        currentSize += (c2/(uint32_t)UCOL_BOT_COUNT2)+1;
      }
      if(fSecs != fSecsBuff) {
        uprv_free(fSecs);
      }
    }

    if(c3 > 0) {
      currentSize += (c3/(uint32_t)coll->tertiaryBottomCount)+1;
    }

    if(c4 > 0  && compareQuad == 0) {
      currentSize += (c4/UCOL_BOT_COUNT4)+1;
    }

    if(compareIdent) {
      currentSize += u_lengthOfIdenticalLevelRun(s->string, len);
    }
    return currentSize;

}

inline void doCaseShift(uint8_t **cases, uint32_t &caseShift) {
  if (caseShift  == 0) {
    *(*cases)++ = UCOL_CASE_BYTE_START;
    caseShift = UCOL_CASE_SHIFT_START;
  }
}

inline uint8_t *packFrench(uint8_t *primaries, uint8_t *secondaries, uint32_t *secsize, uint8_t *frenchStartPtr, uint8_t *frenchEndPtr) {
  uint8_t secondary;
  int32_t count2 = 0;
  uint32_t i = 0;
  uint8_t *primStart = primaries;
  /* If there are any unresolved continuation secondaries, reverse them here so that we can reverse the whole secondary thing */
  if(frenchStartPtr != NULL) {
    uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
  }
  for(i = 0; i<*secsize; i++) {
    secondary = *(secondaries-i-1);
    /* This is compression code. */
    if (secondary == UCOL_COMMON2) {
      ++count2;
    } else {
      if (count2 > 0) {
        if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
          while (count2 > UCOL_TOP_COUNT2) {
            *(primaries)++ = (uint8_t)(UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2);
            count2 -= (uint32_t)UCOL_TOP_COUNT2;
          }
          *(primaries)++ = (uint8_t)(UCOL_COMMON_TOP2 - (count2-1));
        } else {
          while (count2 > UCOL_BOT_COUNT2) {
            *(primaries)++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
            count2 -= (uint32_t)UCOL_BOT_COUNT2;
          }
          *(primaries)++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
        }
        count2 = 0;
      }
      *(primaries)++ = secondary;
      //*(primaries++) = *(secondaries-i-1);
    }
  }
  if (count2 > 0) {
    while (count2 > UCOL_BOT_COUNT2) {
      *(primaries)++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
      count2 -= (uint32_t)UCOL_BOT_COUNT2;
    }
    *(primaries)++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
  }
  *secsize = primaries - primStart;
  return primaries;
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

    UColAttributeValue strength = coll->strength;

    uint8_t compareSec   = (uint8_t)((strength >= UCOL_SECONDARY)?0:0xFF);
    uint8_t compareTer   = (uint8_t)((strength >= UCOL_TERTIARY)?0:0xFF);
    uint8_t compareQuad  = (uint8_t)((strength >= UCOL_QUATERNARY)?0:0xFF);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    UBool  doCase = (coll->caseLevel == UCOL_ON);
    UBool  isFrenchSec = (coll->frenchCollation == UCOL_ON) && (compareSec == 0);
    UBool  shifted = (coll->alternateHandling == UCOL_SHIFTED);
    //UBool  qShifted = shifted && (compareQuad == 0);
    UBool  doHiragana = (coll->hiraganaQ == UCOL_ON) && (compareQuad == 0);
    const uint8_t *scriptOrder = coll->scriptOrder;

    uint32_t variableTopValue = coll->variableTopValue;
    // TODO: UCOL_COMMON_BOT4 should be a function of qShifted. If we have no
    // qShifted, we don't need to set UCOL_COMMON_BOT4 so high.
    uint8_t UCOL_COMMON_BOT4 = (uint8_t)((coll->variableTopValue>>8)+1);
    uint8_t UCOL_HIRAGANA_QUAD = 0;
    if(doHiragana) {
      UCOL_HIRAGANA_QUAD=UCOL_COMMON_BOT4++;
      /* allocate one more space for hiragana, value for hiragana */
    }
    uint8_t UCOL_BOT_COUNT4 = (uint8_t)(0xFF - UCOL_COMMON_BOT4);

    /* support for special features like caselevel and funky secondaries */
    uint8_t *frenchStartPtr = NULL;
    uint8_t *frenchEndPtr = NULL;
    uint32_t caseShift = 0;

    sortKeySize += ((compareSec?0:1) + (compareTer?0:1) + (doCase?1:0) + /*(qShifted?1:0)*/(compareQuad?0:1) + (compareIdent?1:0));

    /* If we need to normalize, we'll do it all at once at the beginning! */
    UNormalizationMode normMode;
    if(compareIdent) {
        normMode = UNORM_NFD;
    } else if(coll->normalizationMode != UCOL_OFF) {
        normMode = UNORM_FCD;
    } else {
        normMode = UNORM_NONE;
    }

    if(normMode != UNORM_NONE && UNORM_YES != unorm_quickCheck(source, len, normMode, status)) {
        len = unorm_internalNormalize(normSource, normSourceLen,
                                      source, len,
                                      normMode, FALSE,
                                      status);
        if(*status == U_BUFFER_OVERFLOW_ERROR) {
            normSourceLen = len;
            normSource = (UChar *)uprv_malloc(len*U_SIZEOF_UCHAR);
            if(normSource == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
            *status = U_ZERO_ERROR;
            len = unorm_internalNormalize(normSource, normSourceLen,
                                          source, len,
                                          normMode, FALSE,
                                          status);
        }

        if(U_FAILURE(*status)) {
            return 0;
        }
        source = normSource;
    }

    collIterate s;
    IInit_collIterate(coll, (UChar *)source, len, &s);
    if(source == normSource) {
        s.flags &= ~UCOL_ITER_NORM;
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

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    int8_t tertiaryAddition = (int8_t)coll->tertiaryAddition;
    uint8_t tertiaryTop = coll->tertiaryTop;
    uint8_t tertiaryBottom = coll->tertiaryBottom;
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

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            if(order == 0) {
              continue;
            }

            notIsContinuation = !isContinuation(order);

            if(notIsContinuation) {
              tertiary = (uint8_t)(order & UCOL_BYTE_SIZE_MASK);
            } else {
              tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
            }

            secondary = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary2 = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary1 = (uint8_t)(order >> 8);

            if(notIsContinuation) {
              if(scriptOrder != NULL) {
                primary1 = scriptOrder[primary1];
              }
            }

            if(shifted && ((notIsContinuation && order <= variableTopValue && primary1 > 0)
              || (!notIsContinuation && wasShifted))) {
              if(count4 > 0) {
                while (count4 > UCOL_BOT_COUNT4) {
                  *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                  count4 -= UCOL_BOT_COUNT4;
                }
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + (count4-1));
                count4 = 0;
              }
              /* We are dealing with a variable and we're treating them as shifted */
              /* This is a shifted ignorable */
              if(primary1 != 0) { /* we need to check this since we could be in continuation */
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
              /* regular and simple sortkey calc */
              if(primary1 != UCOL_IGNORABLE) {
                if(notIsContinuation) {
                  if(leadPrimary == primary1) {
                    *primaries++ = primary2;
                  } else {
                    if(leadPrimary != 0) {
                      *primaries++ = (uint8_t)((primary1 > leadPrimary) ? UCOL_BYTE_UNSHIFTED_MAX : UCOL_BYTE_UNSHIFTED_MIN);
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

            if(secondary > compareSec) {
              if(!isFrenchSec) {
                /* This is compression code. */
                if (secondary == UCOL_COMMON2 && notIsContinuation) {
                  ++count2;
                } else {
                  if (count2 > 0) {
                    if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                      while (count2 > UCOL_TOP_COUNT2) {
                        *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2);
                        count2 -= (uint32_t)UCOL_TOP_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - (count2-1));
                    } else {
                      while (count2 > UCOL_BOT_COUNT2) {
                        *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
                        count2 -= (uint32_t)UCOL_BOT_COUNT2;
                      }
                      *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
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
                  if(notIsContinuation) {
                    if (frenchStartPtr != NULL) {
                        /* reverse secondaries from frenchStartPtr up to frenchEndPtr */
                      uprv_ucol_reverse_buffer(uint8_t, frenchStartPtr, frenchEndPtr);
                      frenchStartPtr = NULL;
                    }
                  } else {
                    if (frenchStartPtr == NULL) {
                      frenchStartPtr = secondaries - 2;
                    }
                    frenchEndPtr = secondaries-1;
                  }
                }
              }

              if(doCase) {
                doCaseShift(&cases, caseShift);
                if(notIsContinuation) {
                  caseBits = (uint8_t)(tertiary & 0xC0);

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
                    if ((tertiary > tertiaryCommon)) {
                      while (count3 > coll->tertiaryTopCount) {
                        *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
                        count3 -= (uint32_t)coll->tertiaryTopCount;
                      }
                      *tertiaries++ = (uint8_t)(tertiaryTop - (count3-1));
                    } else {
                      while (count3 > coll->tertiaryBottomCount) {
                        *tertiaries++ = (uint8_t)(tertiaryBottom + coll->tertiaryBottomCount);
                        count3 -= (uint32_t)coll->tertiaryBottomCount;
                      }
                      *tertiaries++ = (uint8_t)(tertiaryBottom + (count3-1));
                    }
                    count3 = 0;
                  }
                  *tertiaries++ = tertiary;
                }
              }

              if(/*qShifted*/(compareQuad==0)  && notIsContinuation) {
                if(s.flags & UCOL_WAS_HIRAGANA) { // This was Hiragana and we need to note it
                  if(count4>0) { // Close this part
                    while (count4 > UCOL_BOT_COUNT4) {
                      *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                      count4 -= UCOL_BOT_COUNT4;
                    }
                    *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + (count4-1));
                    count4 = 0;
                  }
                  *quads++ = UCOL_HIRAGANA_QUAD; // Add the Hiragana
                } else { // This wasn't Hiragana, so we can continue adding stuff
                  count4++;
                }
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
          while (count2 > UCOL_BOT_COUNT2) {
            *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
            count2 -= (uint32_t)UCOL_BOT_COUNT2;
          }
          *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
        }
        *(primaries++) = UCOL_LEVELTERMINATOR;
        uint32_t secsize = secondaries-secStart;
        if(sortKeySize <= resultLength) {
          if(isFrenchSec) { /* do the reverse copy */
            primaries = packFrench(primaries, secondaries, &secsize, frenchStartPtr, frenchEndPtr);
            sortKeySize += secsize;
          } else {
            sortKeySize += secsize;
            uprv_memcpy(primaries, secStart, secsize);
            primaries += secsize;
          }
        } else {
          if(allocatePrimary == TRUE) { /* need to save our butts if we cannot reallocate */
            primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, 2*sortKeySize, status);
            *result = primStart;
            if(isFrenchSec) { /* do the reverse copy */
              primaries = packFrench(primaries, secondaries, &secsize, frenchStartPtr, frenchEndPtr);
              sortKeySize += secsize;
            } else {
              sortKeySize += secsize;
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
            while (count3 > coll->tertiaryBottomCount) {
              *tertiaries++ = (uint8_t)(tertiaryBottom + coll->tertiaryBottomCount);
              count3 -= (uint32_t)coll->tertiaryBottomCount;
            }
            *tertiaries++ = (uint8_t)(tertiaryBottom + (count3-1));
          }
        }
        uint32_t tersize = tertiaries - terStart;
        sortKeySize += tersize;
        *(primaries++) = UCOL_LEVELTERMINATOR;
        if(sortKeySize <= resultLength) {
          uprv_memcpy(primaries, terStart, tersize);
          primaries += tersize;
          if(compareQuad == 0/*qShifted == TRUE*/) {
              if(count4 > 0) {
                while (count4 > UCOL_BOT_COUNT4) {
                  *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + UCOL_BOT_COUNT4);
                  count4 -= UCOL_BOT_COUNT4;
                }
                *quads++ = (uint8_t)(UCOL_COMMON_BOT4 + (count4-1));
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
          *(primaries++) = UCOL_LEVELTERMINATOR;
          sortKeySize += u_lengthOfIdenticalLevelRun(s.string, len);
          if(sortKeySize <= resultLength) {
            primaries += u_writeIdenticalLevelRun(s.string, len, primaries);
          } else {
            if(allocatePrimary == TRUE) {
              primStart = reallocateBuffer(&primaries, *result, prim, &resultLength, sortKeySize, status);
              *result = primStart;
              u_writeIdenticalLevelRun(s.string, len, primaries);            } else {
              *status = U_MEMORY_ALLOCATION_ERROR;
            }
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

    /* If we need to normalize, we'll do it all at once at the beginning! */
    if(coll->normalizationMode != UCOL_OFF && UNORM_YES != unorm_quickCheck(source, len, UNORM_FCD, status)) {
        len = unorm_internalNormalize(normSource, normSourceLen,
                                      source, len,
                                      UNORM_FCD, FALSE,
                                      status);
        if(*status == U_BUFFER_OVERFLOW_ERROR) {
            normSourceLen = len;
            normSource = (UChar *)uprv_malloc(len*U_SIZEOF_UCHAR);
            if(normSource == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
            *status = U_ZERO_ERROR;
            len = unorm_internalNormalize(normSource, normSourceLen,
                                          source, len,
                                          UNORM_FCD, FALSE,
                                          status);
        }

        if(U_FAILURE(*status)) {
            return 0;
        }
        source = normSource;
    }

    collIterate s;
    IInit_collIterate(coll, (UChar *)source, len, &s);
    if(source == normSource) {
        s.flags &= ~UCOL_ITER_NORM;
    }

    if(resultLength == 0 || primaries == NULL) {
        int32_t t = ucol_getSortKeySize(coll, &s, sortKeySize, coll->strength, len);
        if(normSource != normBuffer) {
            uprv_free(normSource);
    }
        return t;
    }

    uint8_t *primarySafeEnd = primaries + resultLength - 2;

    uint32_t minBufferSize = UCOL_MAX_BUFFER;

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;

    uint32_t order = 0;

    uint8_t primary1 = 0;
    uint8_t primary2 = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;
    int8_t tertiaryAddition = (int8_t)coll->tertiaryAddition;
    uint8_t tertiaryTop = coll->tertiaryTop;
    uint8_t tertiaryBottom = coll->tertiaryBottom;
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

            if(order == 0) {
              continue;
            }

            if(order == UCOL_NO_MORE_CES) {
                finished = TRUE;
                break;
            }

            notIsContinuation = !isContinuation(order);

            if(notIsContinuation) {
              tertiary = (uint8_t)((order & tertiaryMask));
            } else {
              tertiary = (uint8_t)((order & UCOL_REMOVE_CONTINUATION));
            }
            secondary = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary2 = (uint8_t)((order >>= 8) & UCOL_BYTE_SIZE_MASK);
            primary1 = (uint8_t)(order >> 8);

            /* Note: This code assumes that the table is well built i.e. not having 0 bytes where they are not supposed to be. */
            /* Usually, we'll have non-zero primary1 & primary2, except in cases of LatinOne and friends, when primary2 will   */
            /* be zero with non zero primary1. primary3 is different than 0 only for long primaries - see above.               */
            /* regular and simple sortkey calc */
            if(primary1 != UCOL_IGNORABLE) {
              if(notIsContinuation) {
                if(leadPrimary == primary1) {
                  *primaries++ = primary2;
                } else {
                  if(leadPrimary != 0) {
                    *primaries++ = (uint8_t)((primary1 > leadPrimary) ? UCOL_BYTE_UNSHIFTED_MAX : UCOL_BYTE_UNSHIFTED_MIN);
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

            if(secondary > 0) { /* I think that != 0 test should be != IGNORABLE */
              /* This is compression code. */
              if (secondary == UCOL_COMMON2 && notIsContinuation) {
                ++count2;
              } else {
                if (count2 > 0) {
                  if (secondary > UCOL_COMMON2) { // not necessary for 4th level.
                    while (count2 > UCOL_TOP_COUNT2) {
                      *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - UCOL_TOP_COUNT2);
                      count2 -= (uint32_t)UCOL_TOP_COUNT2;
                    }
                    *secondaries++ = (uint8_t)(UCOL_COMMON_TOP2 - (count2-1));
                  } else {
                    while (count2 > UCOL_BOT_COUNT2) {
                      *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
                      count2 -= (uint32_t)UCOL_BOT_COUNT2;
                    }
                    *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
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
                if(tertiary > tertiaryCommon && tertiaryCommon == UCOL_COMMON3_NORMAL) {
                  tertiary += tertiaryAddition;
                } else if (tertiary <= tertiaryCommon && tertiaryCommon == UCOL_COMMON3_UPPERFIRST) {
                  tertiary -= tertiaryAddition;
                }
                if (count3 > 0) {
                  if ((tertiary > tertiaryCommon)) {
                    while (count3 > coll->tertiaryTopCount) {
                      *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
                      count3 -= (uint32_t)coll->tertiaryTopCount;
                    }
                    *tertiaries++ = (uint8_t)(tertiaryTop - (count3-1));
                  } else {
                    while (count3 > coll->tertiaryBottomCount) {
                      *tertiaries++ = (uint8_t)(tertiaryBottom + coll->tertiaryBottomCount);
                      count3 -= (uint32_t)coll->tertiaryBottomCount;
                    }
                    *tertiaries++ = (uint8_t)(tertiaryBottom + (count3-1));
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
        while (count2 > UCOL_BOT_COUNT2) {
          *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + UCOL_BOT_COUNT2);
          count2 -= (uint32_t)UCOL_BOT_COUNT2;
        }
        *secondaries++ = (uint8_t)(UCOL_COMMON_BOT2 + (count2-1));
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
        if (coll->tertiaryCommon != UCOL_COMMON3_NORMAL) {
          while (count3 >= coll->tertiaryTopCount) {
            *tertiaries++ = (uint8_t)(tertiaryTop - coll->tertiaryTopCount);
            count3 -= (uint32_t)coll->tertiaryTopCount;
          }
          *tertiaries++ = (uint8_t)(tertiaryTop - count3);
        } else {
          while (count3 > coll->tertiaryBottomCount) {
            *tertiaries++ = (uint8_t)(tertiaryBottom + coll->tertiaryBottomCount);
            count3 -= (uint32_t)coll->tertiaryBottomCount;
          }
          *tertiaries++ = (uint8_t)(tertiaryBottom + (count3-1));
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

inline void uprv_appendByteToHexString(char *dst, uint8_t val) {
  uint32_t len = (uint32_t)uprv_strlen(dst);
  *(dst+len) = T_CString_itosOffset((val >> 4));
  *(dst+len+1) = T_CString_itosOffset((val & 0xF));
  *(dst+len+2) = 0;
}

/* this function makes a string with representation of a sortkey */
U_CAPI char U_EXPORT2 *ucol_sortKeyToString(const UCollator *coll, const uint8_t *sortkey, char *buffer, uint32_t *len) {
  int32_t strength = UCOL_PRIMARY;
  uint32_t res_size = 0;
  UBool doneCase = FALSE;

  char *current = buffer;
  const uint8_t *currentSk = sortkey;

  uprv_strcpy(current, "[");

  while(strength <= UCOL_QUATERNARY && strength <= coll->strength) {
    if(strength > UCOL_PRIMARY) {
      strcat(current, " . ");
    }
    while(*currentSk != 0x01 && *currentSk != 0x00) { /* print a level */
      uprv_appendByteToHexString(current, *currentSk++);
      uprv_strcat(current, " ");
    }
    if(coll->caseLevel == UCOL_ON && strength == UCOL_SECONDARY && doneCase == FALSE) {
        doneCase = TRUE;
    } else if(coll->caseLevel == UCOL_OFF || doneCase == TRUE || strength != UCOL_SECONDARY) {
      strength ++;
    }
    uprv_appendByteToHexString(current, *currentSk++); /* This should print '01' */
    if(strength == UCOL_QUATERNARY && coll->alternateHandling == UCOL_NON_IGNORABLE) {
      break;
    }
  }

  if(coll->strength == UCOL_IDENTICAL) {
    uprv_strcat(current, " . ");
    while(*currentSk != 0) {
      uprv_appendByteToHexString(current, *currentSk++);
      uprv_strcat(current, " ");
    }

    uprv_appendByteToHexString(current, *currentSk++);
  }
  uprv_strcat(current, "]");

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
        coll->tertiaryBottom = UCOL_COMMON_BOT3;
      } else {
        coll->tertiaryMask = UCOL_KEEP_CASE;
        coll->tertiaryAddition = UCOL_FLAG_BIT_MASK_CASE_SW_ON;
        if(coll->caseFirst == UCOL_UPPER_FIRST) {
          coll->tertiaryCommon = UCOL_COMMON3_UPPERFIRST;
          coll->tertiaryTop = UCOL_COMMON_TOP3_CASE_SW_UPPER;
          coll->tertiaryBottom = UCOL_COMMON_BOTTOM3_CASE_SW_UPPER;
        } else {
          coll->tertiaryCommon = UCOL_COMMON3_NORMAL;
          coll->tertiaryTop = UCOL_COMMON_TOP3_CASE_SW_LOWER;
          coll->tertiaryBottom = UCOL_COMMON_BOTTOM3_CASE_SW_LOWER;
        }
      }

      /* Set the compression values */
      uint8_t tertiaryTotal = (uint8_t)(coll->tertiaryTop - UCOL_COMMON_BOT3-1);
      coll->tertiaryTopCount = (uint8_t)(UCOL_PROPORTION3*tertiaryTotal); /* we multilply double with int, but need only int */
      coll->tertiaryBottomCount = (uint8_t)(tertiaryTotal - coll->tertiaryTopCount);

      if(coll->caseLevel == UCOL_OFF && coll->strength == UCOL_TERTIARY
        && coll->frenchCollation == UCOL_OFF && coll->alternateHandling == UCOL_NON_IGNORABLE) {
        coll->sortKeyGen = ucol_calcSortKeySimpleTertiary;
      } else {
        coll->sortKeyGen = ucol_calcSortKey;
      }

}

U_CAPI uint32_t ucol_setVariableTop(UCollator *coll, const UChar *varTop, int32_t len, UErrorCode *status) {
  if(U_FAILURE(*status) || coll == NULL) {
    return 0;
  }
  if(len == -1) {
    len = u_strlen(varTop);
  }
  if(len == 0) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  collIterate s;
  IInit_collIterate(coll, varTop, len, &s);

  uint32_t CE = ucol_IGetNextCE(coll, &s, status);

  if(s.pos != s.endp) {
    *status = U_CE_NOT_FOUND_ERROR;
    return 0;
  }

  uint32_t nextCE = ucol_IGetNextCE(coll, &s, status);

  if(isContinuation(nextCE) && (nextCE & UCOL_PRIMARYMASK) != 0) {
    *status = U_PRIMARY_TOO_LONG_ERROR;
    return 0;
  }

  coll->variableTopValue = (CE & UCOL_PRIMARYMASK)>>16;

  return CE & UCOL_PRIMARYMASK;
}

U_CAPI uint32_t ucol_getVariableTop(const UCollator *coll, UErrorCode *status) {
  if(U_FAILURE(*status) || coll == NULL) {
    return 0;
  }
  return coll->variableTopValue<<16;
}

U_CAPI void ucol_restoreVariableTop(UCollator *coll, const uint32_t varTop, UErrorCode *status) {
  if(U_FAILURE(*status) || coll == NULL) {
    return;
  }
  coll->variableTopValue = (varTop & UCOL_PRIMARYMASK)>>16;
}
/* Attribute setter API */
U_CAPI void ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status) {
    if(U_FAILURE(*status) || coll == NULL) {
      return;
    }
    switch(attr) {
    case UCOL_HIRAGANA_QUATERNARY_MODE: /* special quaternary values for Hiragana */
      if(value == UCOL_ON) {
        coll->hiraganaQ = UCOL_ON;
        coll->hiraganaQisDefault = FALSE;
      } else if (value == UCOL_OFF) {
        coll->hiraganaQ = UCOL_OFF;
        coll->hiraganaQisDefault = FALSE;
      } else if (value == UCOL_DEFAULT) {
        coll->hiraganaQisDefault = TRUE;
        coll->hiraganaQ = coll->options->hiraganaQ;
      } else {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
      }
      break;
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
    if(U_FAILURE(*status) || coll == NULL) {
      return UCOL_DEFAULT;
    }
    switch(attr) {
    case UCOL_HIRAGANA_QUATERNARY_MODE:
      return coll->hiraganaQ;
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

// deprecated
U_CAPI void
ucol_setNormalization(  UCollator            *coll,
            UNormalizationMode    mode)
{
  UErrorCode status = U_ZERO_ERROR;
  switch(mode) {
  case UNORM_NONE:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
    break;
  case UNORM_NFD:
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    break;
  default:
    /* Shouldn't get here. */
    /* This is quite a bad API */
    /* *status = U_ILLEGAL_ARGUMENT_ERROR; */
    return;
  }
}

// deprecated
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
    int32_t bufferSizeNeeded = (int32_t)sizeof(UCollator);
    char *stackBufferChars = (char *)stackBuffer;

    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }
    if (!pBufferSize || !coll){
       *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    /* Pointers on 64-bit platforms need to be aligned
     * on a 64-bit boundry in memory.
     */
    if (U_ALIGNMENT_OFFSET(stackBuffer) != 0) {
        int32_t offsetUp = (int32_t)U_ALIGNMENT_OFFSET_UP(stackBufferChars);
        *pBufferSize -= offsetUp;
        stackBufferChars += offsetUp;
    }
    stackBuffer = (void *)stackBufferChars;

    if (*pBufferSize <= 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize =  bufferSizeNeeded;
        return 0;
    }
    if (*pBufferSize < bufferSizeNeeded || stackBuffer == NULL) {
        /* allocate one here...*/
        int32_t length;
        const UChar * rules = ucol_getRules(coll, &length);

        localCollator = ucol_openRules(rules,
                                       length,
                                       ucol_getAttribute(coll, UCOL_NORMALIZATION_MODE, status),
                                       ucol_getStrength(coll),
                                       NULL,
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
  UErrorCode status = U_ZERO_ERROR;
  int32_t len = 0;
  int32_t UCAlen = 0;
  const UChar* ucaRules = 0;
  const UChar *rules = ucol_getRules(coll, &len);
  if(delta == UCOL_FULL_RULES) {
    /* take the UCA rules and append real rules at the end */
    /* UCA rules will be probably coming from the root RB */
    ucaRules = ures_getStringByKey(coll->rb,"%%UCARULES",&UCAlen,&status);
  }
  if(U_FAILURE(status)) {
    return 0;
  }
  if(buffer!=0 && bufferLen>0){
      *buffer=0;
      if(UCAlen > 0) {
        u_memcpy(buffer, ucaRules, uprv_min(UCAlen, bufferLen));
      }
      if(len > 0 && bufferLen > UCAlen) {
        u_memcpy(buffer+UCAlen, rules, uprv_min(len, bufferLen-UCAlen));
      }
  }
  return u_terminateUChars(buffer, bufferLen, len+UCAlen, &status);
}

static const UChar _NUL = 0;

U_CAPI const UChar*
ucol_getRules(    const    UCollator       *coll,
        int32_t            *length)
{
  if(coll->rules != NULL) {
    *length = coll->rulesLength;
    return coll->rules;
  } else {
    UErrorCode status = U_ZERO_ERROR;
    if(coll->rb != NULL) {
      UResourceBundle *collElem = ures_getByKey(coll->rb, "CollationElements", NULL, &status);
      if(U_SUCCESS(status)) {
        /*Semantic const */
        ((UCollator *)coll)->rules = ures_getStringByKey(collElem, "Sequence", length, &status);
        ((UCollator *)coll)->rulesLength = *length;
        ((UCollator *)coll)->freeRulesOnClose = FALSE;
        ures_close(collElem);
        return coll->rules;
      }
    }
    *length = 0;
    return &_NUL;
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
  return dst.extract(result, resultLength, *status);
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
      CE = ucmpe32_get(coll->mapping, u);
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

static
UCollationResult    ucol_checkIdent(collIterate *sColl, collIterate *tColl, UBool normalize)
{
    int32_t            comparison;
    int32_t          sLen        = (sColl->flags & UCOL_ITER_HASLEN) ? sColl->endp - sColl->string : -1;
    UChar            *sBuf        = sColl->string;

    int32_t          tLen        = (tColl->flags & UCOL_ITER_HASLEN) ? tColl->endp - tColl->string : -1;
    UChar            *tBuf        = tColl->string;

    if (normalize) {
        UErrorCode status;

        status = U_ZERO_ERROR;
        if (unorm_quickCheck(sColl->string, sLen, UNORM_NFD, &status) != UNORM_YES) {
            sLen = unorm_decompose(sColl->writableBuffer, (int32_t)sColl->writableBufSize,
                                   sBuf, sLen,
                                   FALSE, FALSE,
                                   &status);
            if(status == U_BUFFER_OVERFLOW_ERROR) {
                if(!u_growBufferFromStatic(sColl->stackWritableBuffer,
                                           &sColl->writableBuffer,
                                           (int32_t *)&sColl->writableBufSize, sLen,
                                           0)
                ) {
                    return UCOL_LESS; /* TODO set *status = U_MEMORY_ALLOCATION_ERROR; */
                }
                status = U_ZERO_ERROR;
                sLen = unorm_decompose(sColl->writableBuffer, (int32_t)sColl->writableBufSize,
                                       sBuf, sLen,
                                       FALSE, FALSE,
                                       &status);
            }
            sBuf = sColl->writableBuffer;
            if (sBuf != sColl->stackWritableBuffer) {
                sColl->flags |= UCOL_ITER_ALLOCATED;
            }
        }

        status = U_ZERO_ERROR;
        if (unorm_quickCheck(tColl->string, tLen, UNORM_NFD, &status) != UNORM_YES) {
            tLen = unorm_decompose(tColl->writableBuffer, (int32_t)tColl->writableBufSize,
                                   tBuf, tLen,
                                   FALSE, FALSE,
                                   &status);
            if(status == U_BUFFER_OVERFLOW_ERROR) {
                if(!u_growBufferFromStatic(tColl->stackWritableBuffer,
                                           &tColl->writableBuffer,
                                           (int32_t *)&tColl->writableBufSize, tLen,
                                           0)
                ) {
                    return UCOL_LESS; /* TODO set *status = U_MEMORY_ALLOCATION_ERROR; */
                }
                status = U_ZERO_ERROR;
                tLen = unorm_decompose(tColl->writableBuffer, (int32_t)tColl->writableBufSize,
                                       tBuf, tLen,
                                       FALSE, FALSE,
                                       &status);
            }
            tBuf = tColl->writableBuffer;
            if (tBuf != tColl->stackWritableBuffer) {
                tColl->flags |= UCOL_ITER_ALLOCATED;
            }
        }
    }

    if (sLen == -1 && tLen == -1) {
        comparison = u_strcmpCodePointOrder(sBuf, tBuf);
    } else {
        if (sLen == -1) {
            sLen = u_strlen(sBuf);
        }
        if (tLen == -1) {
            tLen = u_strlen(tBuf);
        }
        comparison = u_memcmpCodePointOrder(sBuf, tBuf, uprv_min(sLen, tLen));
        if (comparison == 0) {
            comparison = sLen - tLen;
        }
    }

    if (comparison < 0) {
        return UCOL_LESS;
    } else if (comparison == 0) {
        return UCOL_EQUAL;
    } else /* comparison > 0 */ {
        return UCOL_GREATER;
    }
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

static
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
        uprv_free(b->buf);
    }
    b->buf = newBuf;
    b->endp = b->buf + newSize;
    b->pos  = b->buf + oldSize;
}

inline void UCOL_CEBUF_PUT(ucol_CEBuf *b, uint32_t ce, collIterate *ci) {
    if (b->pos == b->endp) {
        ucol_CEBuf_Expand(b, ci);
}
    *(b)->pos++ = ce;
};

/* This is a trick string compare function that goes in and uses sortkeys to compare */
/* It is used when compare gets in trouble and needs to bail out                     */
static UCollationResult ucol_compareUsingSortKeys(const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    uint8_t sourceKey[UCOL_MAX_BUFFER], targetKey[UCOL_MAX_BUFFER];
    uint8_t *sourceKeyP = sourceKey;
    uint8_t *targetKeyP = targetKey;
    int32_t sourceKeyLen = UCOL_MAX_BUFFER, targetKeyLen = UCOL_MAX_BUFFER;

    sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    if(sourceKeyLen > UCOL_MAX_BUFFER) {
        sourceKeyP = (uint8_t*)uprv_malloc(sourceKeyLen*sizeof(uint8_t));
        sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    }

    targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    if(targetKeyLen > UCOL_MAX_BUFFER) {
        targetKeyP = (uint8_t*)uprv_malloc(targetKeyLen*sizeof(uint8_t));
        targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    }

    int32_t result = uprv_strcmp((const char*)sourceKeyP, (const char*)targetKeyP);

    if(sourceKeyP != sourceKey) {
        uprv_free(sourceKeyP);
    }

    if(targetKeyP != targetKey) {
        uprv_free(targetKeyP);
    }

    if(result<0) {
        return UCOL_LESS;
    } else if(result>0) {
        return UCOL_GREATER;
    } else {
        return UCOL_EQUAL;
    }
}


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
/*
      if (equalLength < sourceLength) {
        while (UTF_IS_TRAIL(source + equalLength)) {
          --equalLength;
        }
*/
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


    // setting up the collator parameters
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
    UBool doHiragana = (coll->hiraganaQ == UCOL_ON) && checkQuad;

    if(doHiragana && shifted) {
      return (ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength));
    }
    uint8_t caseSwitch = coll->caseSwitch;
    uint8_t tertiaryMask = coll->tertiaryMask;

    // This is the lowest primary value that will not be ignored if shifted
    uint32_t LVT = (shifted)?(coll->variableTopValue<<16):0;

    UCollationResult result = UCOL_EQUAL;
    UCollationResult hirResult = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    // Preparing the context objects for iterating over strings
    collIterate sColl, tColl;

    IInit_collIterate(coll, source, sourceLength, &sColl);
    IInit_collIterate(coll, target, targetLength, &tColl);

    // Preparing the CE buffers. They will be filled during the primary phase
    ucol_CEBuf   sCEs;
    ucol_CEBuf   tCEs;
    UCOL_INIT_CEBUF(&sCEs);
    UCOL_INIT_CEBUF(&tCEs);

    uint32_t secS = 0, secT = 0;
    uint32_t sOrder=0, tOrder=0;

    // Non shifted primary processing is quite simple
    if(!shifted) {
      for(;;) {

        // We fetch CEs until we hit a non ignorable primary or end.
        do {
          // We get the next CE
          sOrder = ucol_IGetNextCE(coll, &sColl, &status);
          // Stuff it in the buffer
          UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
          // And keep just the primary part.
          sOrder &= UCOL_PRIMARYMASK;
        } while(sOrder == 0);

        // see the comments on the above block
        do {
          tOrder = ucol_IGetNextCE(coll, &tColl, &status);
          UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
          tOrder &= UCOL_PRIMARYMASK;
        } while(tOrder == 0);

        // if both primaries are the same
        if(sOrder == tOrder) {
            if(doHiragana && hirResult == UCOL_EQUAL) {
              if((sColl.flags & UCOL_WAS_HIRAGANA) != (tColl.flags & UCOL_WAS_HIRAGANA)) {
                hirResult = ((sColl.flags & UCOL_WAS_HIRAGANA) > (tColl.flags & UCOL_WAS_HIRAGANA)) 
                  ? UCOL_LESS:UCOL_GREATER;
              }
            }
            // and there are no more CEs, we advance to the next level
            if(sOrder == UCOL_NO_MORE_CES_PRIMARY) {
              break;
            }
        } else {
            // if two primaries are different, we are done
            result = (sOrder < tOrder) ?  UCOL_LESS: UCOL_GREATER;
            goto commonReturn;
        }
      } // no primary difference... do the rest from the buffers
    } else { // shifted - do a slightly more complicated processing :)
      for(;;) {
        UBool sInShifted = FALSE;
        UBool tInShifted = FALSE;
        // This version of code can be refactored. However, it seems easier to understand this way.
        // Source loop. Sam as the target loop.
        for(;;) {
          sOrder = ucol_IGetNextCE(coll, &sColl, &status);
          if(sOrder == UCOL_NO_MORE_CES) {
            UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
            break;
          } else if(sOrder == 0) {
            continue;
          } else if(isContinuation(sOrder)) {
            if((sOrder & UCOL_PRIMARYMASK) > 0) { /* There is primary value */
              if(sInShifted) {
                sOrder = (sOrder & UCOL_PRIMARYMASK) | 0xC0; /* preserve interesting continuation */
                UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
                continue;
              } else {
                UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
                break;
              }
            } else { /* Just lower level values */
              if(sInShifted) {
                continue;
              } else {
                UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
                continue;
              }
            }
          } else { /* regular */
            if((sOrder & UCOL_PRIMARYMASK) > LVT) {
              UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
              break;
            } else {
              if((sOrder & UCOL_PRIMARYMASK) > 0) {
                sInShifted = TRUE;
                sOrder &= UCOL_PRIMARYMASK;
                UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
                continue;
              } else {
                UCOL_CEBUF_PUT(&sCEs, sOrder, &sColl);
                sInShifted = FALSE;
                continue;
              }
            }
          }
        }
        sOrder &= UCOL_PRIMARYMASK;
        sInShifted = FALSE;

        for(;;) {
          tOrder = ucol_IGetNextCE(coll, &tColl, &status);
          if(tOrder == UCOL_NO_MORE_CES) {
            UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
            break;
          } else if(tOrder == 0) {
            continue;
          } else if(isContinuation(tOrder)) {
            if((tOrder & UCOL_PRIMARYMASK) > 0) { /* There is primary value */
              if(tInShifted) {
                tOrder = (tOrder & UCOL_PRIMARYMASK) | 0xC0; /* preserve interesting continuation */
                UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
                continue;
              } else {
                UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
                break;
              }
            } else { /* Just lower level values */
              if(tInShifted) {
                continue;
              } else {
                UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
                continue;
              }
            }
          } else { /* regular */
            if((tOrder & UCOL_PRIMARYMASK) > LVT) {
              UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
              break;
            } else {
              if((tOrder & UCOL_PRIMARYMASK) > 0) {
                tInShifted = TRUE;
                tOrder &= UCOL_PRIMARYMASK;
                UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
                continue;
              } else {
                UCOL_CEBUF_PUT(&tCEs, tOrder, &tColl);
                tInShifted = FALSE;
                continue;
              }
            }
          }
        }
        tOrder &= UCOL_PRIMARYMASK;
        tInShifted = FALSE;

        if(sOrder == tOrder) {
          /*
            if(doHiragana && hirResult == UCOL_EQUAL) {
              if((sColl.flags & UCOL_WAS_HIRAGANA) != (tColl.flags & UCOL_WAS_HIRAGANA)) {
                hirResult = ((sColl.flags & UCOL_WAS_HIRAGANA) > (tColl.flags & UCOL_WAS_HIRAGANA)) 
                  ? UCOL_LESS:UCOL_GREATER;
              }
            }
          */
            if(sOrder == UCOL_NO_MORE_CES_PRIMARY) {
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

    /* This is the secondary level of comparison */
    if(checkSecTer) {
      if(!isFrenchSec) { /* normal */
        sCE = sCEs.buf;
        tCE = tCEs.buf;
        for(;;) {
          while (secS == 0) {
            secS = *(sCE++) & UCOL_SECONDARYMASK;
          }

          while(secT == 0) {
              secT = *(tCE++) & UCOL_SECONDARYMASK;
          }

          if(secS == secT) {
            if(secS == UCOL_NO_MORE_CES_SECONDARY) {
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
              secS = *(sCE--);
              if(isContinuation(secS)) {
                while(isContinuation(secS = *(sCE--)));
                /* after this, secS has the start of continuation, and sCEs points before that */
                sCESave = sCE; /* we save it, so that we know where to come back AND that we need to go forward */
                sCE+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secS = *(sCE++);
              if(!isContinuation(secS)) { /* This means we have finished with this cont */
                sCE = sCESave;            /* reset the pointer to before continuation */
                sCESave = 0;
                continue;
              }
            }
            secS &= UCOL_SECONDARYMASK; /* remove the continuation bit */
          }

          while(secT == 0 && tCE >= tCEs.buf) {
            if(tCESave == 0) {
              secT = *(tCE--);
              if(isContinuation(secT)) {
                while(isContinuation(secT = *(tCE--)));
                /* after this, secS has the start of continuation, and sCEs points before that */
                tCESave = tCE; /* we save it, so that we know where to come back AND that we need to go forward */
                tCE+=2;  /* need to point to the first continuation CP */
                /* However, now you can just continue doing stuff */
              }
            } else {
              secT = *(tCE++);
              if(!isContinuation(secT)) { /* This means we have finished with this cont */
                tCE = tCESave;          /* reset the pointer to before continuation */
                tCESave = 0;
                continue;
              }
            }
            secT &= UCOL_SECONDARYMASK; /* remove the continuation bit */
          }

          if(secS == secT) {
            if(secS == UCOL_NO_MORE_CES_SECONDARY || (sCE < sCEs.buf && tCE < tCEs.buf)) {
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

        if((secS & UCOL_REMOVE_CASE) == UCOL_NO_MORE_CES_TERTIARY || (secT & UCOL_REMOVE_CASE) == UCOL_NO_MORE_CES_TERTIARY ) {
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


    if(qShifted /*checkQuad*/) {
      UBool sInShifted = TRUE;
      UBool tInShifted = TRUE;
      secS = 0;
      secT = 0;
      sCE = sCEs.buf;
      tCE = tCEs.buf;
      for(;;) {
        while(secS == 0 && secS != UCOL_NO_MORE_CES || (isContinuation(secS) && !sInShifted)) {
          secS = *(sCE++);
          if(isContinuation(secS)) {
            if(!sInShifted) {
              continue;
            }
          } else if(secS > LVT || (secS & UCOL_PRIMARYMASK) == 0) { /* non continuation */
            secS = UCOL_PRIMARYMASK;
            sInShifted = FALSE;
          } else {
            sInShifted = TRUE;
          }
        }
        secS &= UCOL_PRIMARYMASK;


        while(secT == 0 && secT != UCOL_NO_MORE_CES || (isContinuation(secT) && !tInShifted)) {
          secT = *(tCE++);
          if(isContinuation(secT)) {
            if(!tInShifted) {
              continue;
            }
          } else if(secT > LVT || (secT & UCOL_PRIMARYMASK) == 0) {
            secT = UCOL_PRIMARYMASK;
            tInShifted = FALSE;
          } else {
            tInShifted = TRUE;
          }
        }
        secT &= UCOL_PRIMARYMASK;

        if(secS == secT) {
          if(secS == UCOL_NO_MORE_CES_PRIMARY) {
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
    } else if(doHiragana && hirResult != UCOL_EQUAL) {
      // If we're fine on quaternaries, we might be different
      // on Hiragana. This, however, might fail us in shifted.
      result = hirResult;
      goto commonReturn;
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

