/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
   *
   *   uconv_cnv.h:
   *   defines all the low level conversion functions
   *   T_UnicodeConverter_{to,from}Unicode_$ConversionType
 */

#ifndef UCNV_CNV_H
#define UCNV_CNV_H

#include "unicode/utypes.h"
#include "unicode/ucnv_bld.h"

bool_t CONVERSION_U_SUCCESS (UErrorCode err);

void T_UConverter_toUnicode_SBCS (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_SBCS (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_toUnicode_MBCS (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_MBCS (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);
void T_UConverter_toUnicode_MBCS_OFFSETS_LOGIC (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_MBCS_OFFSETS_LOGIC (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_toUnicode_DBCS (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_DBCS (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_fromUnicode_UTF16_BE (UConverter * converter,
					char **target,
					const char *targetLimit,
					const UChar ** source,
					const UChar * sourceLimit,
					int32_t* offsets,
					bool_t flush,
					UErrorCode * err);

void T_UConverter_toUnicode_UTF16_BE (UConverter * converter,
				      UChar ** target,
				      const UChar * targetLimit,
				      const char **source,
				      const char *sourceLimit,
				      int32_t* offsets,
				      bool_t flush,
				      UErrorCode * err);

void T_UConverter_fromUnicode_UTF16_LE (UConverter * converter,
					char **target,
					const char *targetLimit,
					const UChar ** source,
					const UChar * sourceLimit,
					int32_t* offsets,
					bool_t flush,
					UErrorCode * err);

void T_UConverter_toUnicode_EBCDIC_STATEFUL(UConverter * converter,
					    UChar ** target,
					    const UChar * targetLimit,
					    const char **source,
					    const char *sourceLimit,
					    int32_t* offsets,
					    bool_t flush,
					    UErrorCode * err);

void T_UConverter_fromUnicode_EBCDIC_STATEFUL(UConverter * converter,
					      char **target,
					      const char *targetLimit,
					      const UChar ** source,
					      const UChar * sourceLimit,
					      int32_t* offsets,
					      bool_t flush,
					      UErrorCode * err);

void T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC(UConverter * converter,
							  UChar ** target,
							  const UChar * targetLimit,
							  const char **source,
							  const char *sourceLimit,
							  int32_t* offsets,
							  bool_t flush,
							  UErrorCode * err);

void T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC(UConverter * converter,
							    char **target,
							    const char *targetLimit,
							    const UChar ** source,
							    const UChar * sourceLimit,
							    int32_t* offsets,
							    bool_t flush,
							    UErrorCode * err);

void T_UConverter_toUnicode_ISO_2022(UConverter * converter,
				     UChar ** target,
				     const UChar * targetLimit,
				     const char **source,
				     const char *sourceLimit,
				     int32_t* offsets,
				     bool_t flush,
				     UErrorCode * err);

void T_UConverter_fromUnicode_ISO_2022(UConverter * converter,
				       char **target,
				       const char *targetLimit,
				       const UChar ** source,
				       const UChar * sourceLimit,
				       int32_t* offsets,
				       bool_t flush,
				       UErrorCode * err);

void T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC(UConverter * converter,
				     UChar ** target,
				     const UChar * targetLimit,
				     const char **source,
				     const char *sourceLimit,
				     int32_t* offsets,
				     bool_t flush,
				     UErrorCode * err);

void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC(UConverter * converter,
				       char **target,
				       const char *targetLimit,
				       const UChar ** source,
				       const UChar * sourceLimit,
				       int32_t* offsets,
				       bool_t flush,
				       UErrorCode * err);


void T_UConverter_toUnicode_UTF16_LE (UConverter * converter,
				      UChar ** target,
				      const UChar * targetLimit,
				      const char **source,
				      const char *sourceLimit,
				      int32_t* offsets,
				      bool_t flush,
				      UErrorCode * err);

void T_UConverter_fromUnicode_UTF8 (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_toUnicode_UTF8 (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC (UConverter * converter,
				  UChar ** target,
				  const UChar * targetLimit,
				  const char **source,
				  const char *sourceLimit,
				  int32_t* offsets,
				  bool_t flush,
				  UErrorCode * err);

void T_UConverter_fromUnicode_LATIN_1 (UConverter * converter,
				       char **target,
				       const char *targetLimit,
				       const UChar ** source,
				       const UChar * sourceLimit,
				       int32_t* offsets,
				       bool_t flush,
				       UErrorCode * err);

void T_UConverter_toUnicode_LATIN_1 (UConverter * converter,
				     UChar ** target,
				     const UChar * targetLimit,
				     const char **source,
				     const char *sourceLimit,
				     int32_t* offsets,
				     bool_t flush,
				     UErrorCode * err);

UChar T_UConverter_getNextUChar_LATIN_1 (UConverter * converter,
					 const char **source,
					 const char *sourceLimit,
					 UErrorCode * err);

UChar T_UConverter_getNextUChar_SBCS (UConverter * converter,
				      const char **source,
				      const char *sourceLimit,
				      UErrorCode * err);

UChar T_UConverter_getNextUChar_DBCS (UConverter * converter,
				      const char **source,
				      const char *sourceLimit,
				      UErrorCode * err);

UChar T_UConverter_getNextUChar_MBCS (UConverter * converter,
				      const char **source,
				      const char *sourceLimit,
				      UErrorCode * err);

UChar T_UConverter_getNextUChar_UTF8 (UConverter * converter,
				      const char **source,
				      const char *sourceLimit,
				      UErrorCode * err);

UChar T_UConverter_getNextUChar_UTF16_BE (UConverter * converter,
					  const char **source,
					  const char *sourceLimit,
					  UErrorCode * err);

UChar T_UConverter_getNextUChar_UTF16_LE (UConverter * converter,
					  const char **source,
					  const char *sourceLimit,
					  UErrorCode * err);


UChar T_UConverter_getNextUChar_EBCDIC_STATEFUL (UConverter * converter,
						 const char **source,
						 const char *sourceLimit,
						 UErrorCode * err);

UChar T_UConverter_getNextUChar_ISO_2022 (UConverter * converter,
					  const char **source,
					  const char *sourceLimit,
					  UErrorCode * err);

#endif /* UCNV_CNV */
