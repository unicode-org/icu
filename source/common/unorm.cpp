/*
******************************************************************************
* Copyright (c) 1996-2001, International Business Machines
* Corporation and others. All Rights Reserved.
******************************************************************************
* File unorm.cpp
*
* Created by: Vladimir Weinstein 12052000
*
* Modification history :
*
* Date        Name        Description
* 02/01/01    synwee      Added normalization quickcheck enum and method.
* 02/12/01    synwee      Commented out quickcheck util api has been approved
*                         Added private method for doing FCD checks
* 02/23/01    synwee      Modified quickcheck and checkFCE to run through 
*                         string for codepoints < 0x300 for the normalization 
*                         mode NFC.
*/

#include "unicode/unorm.h"
#include "unicode/normlzr.h"
#include "unicode/ustring.h"
#include "unicode/udata.h"
#include "cpputils.h"
#include "ustr_imp.h"
#include "umutex.h"

/* added by synwee */
#include "unicode/uchar.h"
#include "unicode/utf16.h"

/* added by synwee for trie manipulation*/
#define STAGE_1_SHIFT_            10
#define STAGE_2_SHIFT_            4
#define STAGE_2_MASK_AFTER_SHIFT_ 0x3F
#define STAGE_3_MASK_             0xF
#define LAST_BYTE_MASK_           0xFF
#define SECOND_LAST_BYTE_SHIFT_   8

/* added by synwee for fast route in quickcheck and fcd */
#define NFC_ZERO_CC_BLOCK_LIMIT_  0x300

/*
 * for a description of the file format, 
 * see icu/source/tools/genqchk/genqchk.c
 */
#define QCHK_DATA_NAME "qchk"
#define FCHK_DATA_NAME "fchk"
#define DATA_TYPE "dat"

static UDataMemory *quickcheckData = NULL;
static UDataMemory *fcdcheckData   = NULL;

/**
* Authentication values
*/
static const uint8_t QCHK_DATA_FORMAT_[]    = {0x71, 0x63, 0x68, 0x6b};
static const uint8_t FCHK_DATA_FORMAT_[]    = {0x66, 0x63, 0x68, 0x6b};
static const uint8_t QCHK_FORMAT_VERSION_[] = {1, 0, 0, 0};
static const uint8_t FCHK_FORMAT_VERSION_[] = {1, 0, 0, 0};

/** 
* index values loaded from qchk.dat.
* static uint16_t indexes[8]; 
*/
enum {
    QCHK_INDEX_STAGE_2_BITS,
    QCHK_INDEX_STAGE_3_BITS,
    QCHK_INDEX_MIN_VALUES_SIZE,
    QCHK_INDEX_STAGE_1_INDEX,
    QCHK_INDEX_STAGE_2_INDEX,
    QCHK_INDEX_STAGE_3_INDEX
};

/** 
* index values loaded from qchk.dat.
* static uint16_t indexes[8]; 
*/
enum {
    FCHK_INDEX_STAGE_2_BITS,
    FCHK_INDEX_STAGE_3_BITS,
    FCHK_INDEX_STAGE_1_INDEX,
    FCHK_INDEX_STAGE_2_INDEX,
    FCHK_INDEX_STAGE_3_INDEX
};

/**
* Array of mask for determining normalization quick check values.
* Indexes follows the values in UNormalizationMode
*/
static const uint8_t QCHK_MASK_[] = {0, 0, 0x11, 0x22, 0x44, 0x88};
/** 
* Array of minimum codepoints that has UNORM_MAYBE or UNORM_NO quick check
* values. Indexes follows the values in UNormalizationMode.
* Generated values! Edit at your own risk.
*/
static const UChar32 *QCHK_MIN_VALUES_;

/**
* Flag to indicate if data has been loaded 
*/
static UBool isQuickCheckLoaded = FALSE;
static UBool isFCDCheckLoaded   = FALSE;

/**
* Minimum value to determine if quickcheck value contains a MAYBE
*/
static const uint8_t MIN_UNORM_MAYBE_ = 0x10;

/**
* Array of normalization form corresponding to the index code point.
* Hence codepoint 0xABCD will have normalization form QUICK_CHECK_DATA[0xABCD].
* UQUICK_CHECK_DATA[0xABCD] is a byte containing 2 sets of 4 bits information
* representing UNORM_MAYBE and UNORM_YES.<br>
* bits 1 2 3 4                        5678<br>
*      NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
* ie if UQUICK_CHECK_DATA[0xABCD] = 10000001, this means that 0xABCD is in 
* NFD form and maybe in NFKC form
*/
static const uint16_t *QCHK_STAGE_1_;
static const uint16_t *QCHK_STAGE_2_;
static const uint8_t  *QCHK_STAGE_3_;

/**
* Trie data for FCD.
* Each index corresponds to each code point. 
* Trie value is the combining class of the first and the last character of the
* NFD of the codepoint.
* size uint16_t for the first 2 stages instead of uint32_t to reduce size.
*/
static const uint16_t *FCHK_STAGE_1_;
static const uint16_t *FCHK_STAGE_2_;
static const uint16_t *FCHK_STAGE_3_;

U_CAPI int32_t
unorm_normalize(const UChar*            source,
        int32_t                 sourceLength, 
        UNormalizationMode      mode, 
        int32_t                 option,
        UChar*                  result,
        int32_t                 resultLength,
        UErrorCode*             status)
{
  if(U_FAILURE(*status)) return -1;

  /* synwee : removed hard coded conversion */
  Normalizer::EMode normMode = Normalizer::getNormalizerEMode(mode, *status);
  if (U_FAILURE(*status))
    return -1;

  int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);
  const UnicodeString src(sourceLength == -1, source, len);
  UnicodeString dst(result, 0, resultLength);
  /* synwee : note quickcheck is added in C ++ normalize method */
  if ((option & UNORM_IGNORE_HANGUL) != 0)
    option = Normalizer::IGNORE_HANGUL;
  Normalizer::normalize(src, normMode, option, dst, *status);
  return uprv_fillOutputString(dst, result, resultLength, status);
}

static UBool
isQuickCheckAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    if (pInfo->size >= 20 &&
        pInfo->isBigEndian == U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily == U_CHARSET_FAMILY &&
        (uprv_memcmp(pInfo->dataFormat, QCHK_DATA_FORMAT_, 
                     sizeof(QCHK_DATA_FORMAT_)) == 0) && 
        /*
        pInfo->dataFormat[0] == 0x71 && 
        pInfo->dataFormat[1] == 0x63 &&
        pInfo->dataFormat[2] == 0x68 &&
        pInfo->dataFormat[3] == 0x6b &&
        pInfo->formatVersion[0] == 1
        */
        (uprv_memcmp(pInfo->formatVersion, QCHK_FORMAT_VERSION_, 
                     sizeof(QCHK_FORMAT_VERSION_)) == 0)) {
        return TRUE;
    } else {
        context = NULL;
        type    = NULL;
        name    = NULL;
        return FALSE;
    }
}

static UBool
loadQuickCheckData() {
    /* load quickcheck data from file if necessary */
    if (!isQuickCheckLoaded) {
        UErrorCode error = U_ZERO_ERROR;
        UDataMemory *data;

        /* open the data outside the mutex block */
        data = udata_openChoice(NULL, DATA_TYPE, QCHK_DATA_NAME, 
                                isQuickCheckAcceptable, NULL, &error);
        if (U_FAILURE(error)) {
            return isQuickCheckLoaded = FALSE;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if (quickcheckData == NULL) {
            const uint16_t *temp = (const uint16_t *)udata_getMemory(data);
            const uint16_t *indexes = temp;
    
            quickcheckData = data;

            temp += 8;
            QCHK_MIN_VALUES_ = (const UChar32 *)temp;
            QCHK_STAGE_1_    = temp + indexes[QCHK_INDEX_STAGE_1_INDEX];
            QCHK_STAGE_2_    = temp + indexes[QCHK_INDEX_STAGE_2_INDEX];
            QCHK_STAGE_3_    = (const uint8_t *)(temp + 
                                           indexes[QCHK_INDEX_STAGE_3_INDEX]);
            data = NULL;
        }
        umtx_unlock(NULL);

        isQuickCheckLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if (data != NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return isQuickCheckLoaded;
}

/**
 * Performing quick check on a string, to quickly determine if the string is 
 * in a particular normalization format.
 * Three types of result can be returned UNORM_YES, UNORM_NO or
 * UNORM_MAYBE. Result UNORM_YES indicates that the argument
 * string is in the desired normalized format, UNORM_NO determines that
 * argument string is not in the desired normalized format. A 
 * UNORM_MAYBE result indicates that a more thorough check is required, 
 * the user may have to put the string in its normalized form and compare the 
 * results.
 * @param source       string for determining if it is in a normalized format
 * @param sourcelength length of source to test
 * @param mode         normalization format from the enum UNormalizationMode
 * @param status A pointer to an UErrorCode to receive any errors
 * @return UNORM_YES, UNORM_NO or UNORM_MAYBE
 */
U_CAPI UNormalizationCheckResult
unorm_quickCheck(const UChar             *source,
                       int32_t            sourcelength, 
                       UNormalizationMode mode, 
                       UErrorCode*        status)
{
  uint8_t                    oldcombiningclass = 0;
  uint8_t                    combiningclass;
  uint8_t                    quickcheckvalue;
  uint8_t                    mask              = QCHK_MASK_[mode];
  UChar32                    min;
  UChar32                    codepoint;
  UNormalizationCheckResult  result            = UNORM_YES;
  const UChar                *psource;
  const UChar                *pend             = 0;

  if (!loadQuickCheckData() || U_FAILURE(*status)) {
    return UNORM_MAYBE;
  }

  min = QCHK_MIN_VALUES_[mode];
  
  /* checking argument*/
  if (mode >= UNORM_MODE_COUNT || mode < UNORM_NONE) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return UNORM_MAYBE;
  }

  if (sourcelength >= 0) {
    psource = source;
    pend    = source + sourcelength;
    for (;;) {
      if (psource >= pend) {
        return UNORM_YES;
      }
      /* fast route : since codepoints < min has combining class 0 and YES
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= min) {
        break;
      }
      psource ++;
    }
  }
  else {
    psource = source;
    for (;;) {
      if (*psource == 0) {
        return UNORM_YES;
      }
      /* fast route : since codepoints < min has combining class 0 and YES 
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= min) {
        break;
      }
      psource ++;
    }
  }

  if (sourcelength >= 0) {
    for (;;) {
      int count = 0;

      if (psource >= pend) {
        break;
      }
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);      
      combiningclass = u_getCombiningClass(codepoint);
      /* not in canonical order */

      if (oldcombiningclass > combiningclass && combiningclass != 0) {
        return UNORM_NO;
      }

      oldcombiningclass = combiningclass;

      /* trie access */
      quickcheckvalue = (uint8_t)(QCHK_STAGE_3_[
          QCHK_STAGE_2_[QCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
          ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
          (codepoint & STAGE_3_MASK_)] & mask);
      /* value is a byte containing 2 sets of 4 bits information.
         bits 1 2 3 4                        5678<br>
         NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
         ie if quick[0xABCD] = 10000001, this means that 0xABCD is in NFD form 
         and maybe in NFKC form. */
      if (quickcheckvalue == 0) {
        return UNORM_NO;
      }
      if (quickcheckvalue >= MIN_UNORM_MAYBE_) {
        result = UNORM_MAYBE;
      }
      psource += count;
    }
  }
  else {
    for (;;) {
      int count = 0;
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);      
      if (codepoint == 0) {
        break;
      }
      
      combiningclass = u_getCombiningClass(codepoint);
      /* not in canonical order */

      if (oldcombiningclass > combiningclass && combiningclass != 0) {
        return UNORM_NO;
      }

      oldcombiningclass = combiningclass;

      /* trie access */
      quickcheckvalue = (uint8_t)(QCHK_STAGE_3_[
          QCHK_STAGE_2_[QCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
          ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
          (codepoint & STAGE_3_MASK_)] & mask);
      /* value is a byte containing 2 sets of 4 bits information.
         bits 1 2 3 4                        5678<br>
         NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
         ie if quick[0xABCD] = 10000001, this means that 0xABCD is in NFD form 
         and maybe in NFKC form. */
      if (quickcheckvalue == 0) {
        return UNORM_NO;
      }
      if (quickcheckvalue >= MIN_UNORM_MAYBE_) {
        result = UNORM_MAYBE;
      }
      psource += count;
    }
  }
  
  return result;
}

/* private methods ---------------------------------------------------------- */

static UBool
isFCDCheckAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    if(
        pInfo->size >= 20 &&
        pInfo->isBigEndian == U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily == U_CHARSET_FAMILY &&
        (uprv_memcmp(pInfo->dataFormat, FCHK_DATA_FORMAT_, 
                     sizeof(FCHK_DATA_FORMAT_)) == 0) && 
        /*
        pInfo->dataFormat[0] == 0x71 && 
        pInfo->dataFormat[1] == 0x63 &&
        pInfo->dataFormat[2] == 0x68 &&
        pInfo->dataFormat[3] == 0x6b &&
        pInfo->formatVersion[0] == 1
        */
        (uprv_memcmp(pInfo->formatVersion, FCHK_FORMAT_VERSION_, 
                     sizeof(FCHK_FORMAT_VERSION_)) == 0)) {
        return TRUE;
    } else {
        context = NULL;
        type    = NULL;
        name    = NULL;
        return FALSE;
    }
}

static UBool
loadFCDCheckData() {
    /* load fcdcheck data from file if necessary */
    if (!isFCDCheckLoaded) {
        UErrorCode error = U_ZERO_ERROR;
        UDataMemory *data;

        /* open the data outside the mutex block */
        data = udata_openChoice(NULL, DATA_TYPE, FCHK_DATA_NAME, 
                                isFCDCheckAcceptable, NULL, &error);
        if (U_FAILURE(error)) {
            return isFCDCheckLoaded = FALSE;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if (fcdcheckData == NULL) {
            const uint16_t *temp = (const uint16_t *)udata_getMemory(data);
            const uint16_t *indexes = temp;
    
            fcdcheckData = data;

            temp += 8;
            FCHK_STAGE_1_    = temp + indexes[FCHK_INDEX_STAGE_1_INDEX];
            FCHK_STAGE_2_    = temp + indexes[FCHK_INDEX_STAGE_2_INDEX];
            FCHK_STAGE_3_    = (const uint16_t *)(temp + 
                                           indexes[FCHK_INDEX_STAGE_3_INDEX]);
            data = NULL;
        }
        umtx_unlock(NULL);

        isFCDCheckLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if (data != NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return isFCDCheckLoaded;
}

/**
* Private method which performs a quick FCD check on a string, to quickly 
* determine if a string is in a required FCD format.
* FCD is the set of strings such that for each character in the string, 
* decomposition without any canonical reordering will produce a NFD.
* @param source       string for determining if it is in a normalized format
* @param sourcelength length of source to test
* @paran mode         normalization format from the enum UNormalizationMode
* @param status       A pointer to an UErrorCode to receive any errors
* @return TRUE if source is in FCD format, FALSE otherwise
*/
U_CAPI UBool 
checkFCD(const UChar* source, int32_t sourcelength, UErrorCode* status)
{
        UChar32  codepoint;
  const UChar   *psource;
  const UChar   *pend = 0;
        uint8_t  oldfcdtrail = 0;
        uint16_t fcd = 0;
  
  if (!loadFCDCheckData() || U_FAILURE(*status)) {
    return FALSE;
        }

  if (sourcelength >= 0) {
    psource = source;
    pend    = source + sourcelength;
    for (;;) {
      if (psource >= pend) {
        return TRUE;
      }
      /* fast route : since codepoints < NFC_ZER_CC_BLOCK_LIMIT_ has 
         combining class 0.
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= NFC_ZERO_CC_BLOCK_LIMIT_) {
        break;
      }
      psource ++;
    }
  }
  else {
    psource = source;
    for (;;) {
      if (*psource == 0) {
        return TRUE;
      }
      /* fast route : since codepoints < min has combining class 0 and YES 
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= NFC_ZERO_CC_BLOCK_LIMIT_) {
        break;
      }
      psource ++;
    }
  }

  /* not end of string and yet failed simple compare 
     safe to shift back one char because the previous char has to be < 0x300 or the
     start of a string */
  if (psource == source) {
    oldfcdtrail = 0;
  }
  else {
    codepoint = *(psource - 1);
    oldfcdtrail = (uint8_t)(FCHK_STAGE_3_[
                  FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
                  ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] 
                  + (codepoint & STAGE_3_MASK_)] & LAST_BYTE_MASK_);
  }

  if (sourcelength >= 0) {
    for (;;) {
      int count = 0;
      uint8_t lead;

      if (psource >= pend) {
        return TRUE;
      }
      
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);

      /* trie access */
      fcd = FCHK_STAGE_3_[
            FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
            (codepoint & STAGE_3_MASK_)];
      lead = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
    
      if (lead != 0 && oldfcdtrail > lead) {
        return FALSE;
      }
      oldfcdtrail = (uint8_t)(fcd & LAST_BYTE_MASK_);
    
      psource += count;
    }
  }
  else {
    for (;;) {
      int count = 0;
      uint8_t lead;

      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);
      if (codepoint == 0) {
        return TRUE;
      }
      /* trie access */
      fcd = FCHK_STAGE_3_[
            FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
            (codepoint & STAGE_3_MASK_)];
    
      lead = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
    
      if (lead != 0 && oldfcdtrail > lead) {
        return FALSE;
      }
      oldfcdtrail = (uint8_t)(fcd & LAST_BYTE_MASK_);
      psource += count;
    }
  }
  return TRUE;
}
