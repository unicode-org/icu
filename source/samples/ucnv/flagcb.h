/* Copyright (c) 2000 IBM, Inc. and Others. 
   FLAGCB.H - interface to 'flagging' callback which 
   simply marks the fact that the callback was called. 
*/

#ifndef _FLAGCB
#define _FLAGCB

#include "unicode/utypes.h"
#include "unicode/ucnv.h"

/* The structure of a FromU Flag context. 
   (conceivably there could be a ToU Flag Context) */

typedef struct
{
  UConverterFromUCallback  subCallback;
  void                    *subContext;
  UBool                    flag;
} FromUFLAGContext;

U_CAPI void U_EXPORT2 UCNV_FROM_U_CALLBACK_FLAG (
                  void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
				  UErrorCode * err);

#endif
