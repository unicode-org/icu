/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_utf.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb03
*   created by: Markus W. Scherer
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "unicode/utypes.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* UTF-8 -------------------------------------------------------------------- */

/* UTF-8 Conversion DATA
 *   for more information see Unicode Strandard 2.0 , Transformation Formats Appendix A-9
 */
const uint32_t kReplacementCharacter = 0x0000FFFD;
const uint32_t kMaximumUCS2 = 0x0000FFFF;
const uint32_t kMaximumUTF16 = 0x0010FFFF;
const uint32_t kMaximumUCS4 = 0x7FFFFFFF;
const int8_t halfShift = 10;
const uint32_t halfBase = 0x0010000;
const uint32_t halfMask = 0x3FF;
const uint32_t kSurrogateHighStart = 0xD800;
const uint32_t kSurrogateHighEnd = 0xDBFF;
const uint32_t kSurrogateLowStart = 0xDC00;
const uint32_t kSurrogateLowEnd = 0xDFFF;

const uint32_t offsetsFromUTF8[7] = {0,
  (uint32_t) 0x00000000, (uint32_t) 0x00003080, (uint32_t) 0x000E2080,
  (uint32_t) 0x03C82080, (uint32_t) 0xFA082080, (uint32_t) 0x82082080
};

/* END OF UTF-8 Conversion DATA */

const int8_t bytesFromUTF8[256] = {
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
};

const unsigned char firstByteMark[7] = {0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC};

void T_UConverter_toUnicode_UTF8 (UConverter * _this,
                                  UChar ** target,
                                  const UChar * targetLimit,
                                  const char **source,
                                  const char *sourceLimit,
                                  int32_t *offsets,
                                  UBool flush,
                                  UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  uint32_t ch = 0 ,
           ch2 =0 ,
           i =0;            /* Index into the current # of bytes consumed in the current sequence */
  uint32_t inBytes = 0;  /* Total number of bytes in the current UTF8 sequence */
  UConverterToUnicodeArgs args;
  
  if (_this->toUnicodeStatus)
    {
      i = _this->invalidCharLength;   /* restore # of bytes consumed */
      inBytes = _this->toUnicodeStatus; /* Restore size of current sequence */

      ch = _this->mode; /*Stores the previously calculated ch from a previous call*/
      _this->toUnicodeStatus = 0;
      _this->invalidCharLength = 0;
      goto morebytes;
    }


  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          ch = 0;
          ch = ((uint32_t)mySource[mySourceIndex++]) & 0x000000FF;
          if (ch < 0x80)        /* Simple case */
            {
              myTarget[myTargetIndex++] = (UChar) ch;
            }
          else
            {
              /* store the first char */

              inBytes = bytesFromUTF8[ch]; /* lookup current sequence length */
              _this->invalidCharBuffer[0] = (char)ch;
              i = 1;

            morebytes:
              for (; i < inBytes; i++)
                {
                  {
                    if (mySourceIndex >= sourceLength)
                      {
                        if (flush)
                          {
                            if (U_SUCCESS(*err)) 
                              {
                                *err = U_TRUNCATED_CHAR_FOUND;
                                _this->toUnicodeStatus = 0x00;
                              }
                          }
                        else
                          {
                            _this->toUnicodeStatus = inBytes;
                            _this->invalidCharLength = (int8_t)i;
                          }
                        goto donefornow;
                      }
                    _this->invalidCharBuffer[i] = (char) (ch2 = (((uint32_t)mySource[mySourceIndex++]) & 0x000000FF));
                    if ((ch2 & 0xC0) != 0x80)   /* Invalid trailing byte */
                      break;
                  }
                  ch <<= 6;
                  ch += ch2;
                }


              ch -= offsetsFromUTF8[inBytes];

              if (i == inBytes && ch <= kMaximumUTF16) 
                {
                  if (ch <= kMaximumUCS2) 
                    {
                      myTarget[myTargetIndex++] = (UChar) ch;
                    }
                  else
                    {
                      ch -= halfBase;
                      myTarget[myTargetIndex++] = (UChar) ((ch >> halfShift) + kSurrogateHighStart);
                      ch = (ch & halfMask) + kSurrogateLowStart;
                      if (myTargetIndex < targetLength)
                        {
                          myTarget[myTargetIndex++] = (char)ch;
                        }
                      else
                        {
                          _this->invalidUCharBuffer[0] = (UChar) ch;
                          _this->invalidUCharLength = 1;
                          *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
                }
              else
                {
                  *err = U_ILLEGAL_CHAR_FOUND;
                  _this->invalidCharLength = (int8_t)i;
                  
#ifdef Debug
                  printf("inbytes %d\n, _this->invalidCharLength = %d,\n mySource[mySourceIndex]=%X\n", inBytes, _this->invalidCharLength, mySource[mySourceIndex]);
#endif
/* Needed explicit cast for mySource on MVS to make compiler happy - JJD */
                  args.converter = _this;
                  args.target = myTarget + myTargetIndex;
                  args.targetLimit = targetLimit;
                  args.source = (const char*) mySource + mySourceIndex;
                  args.sourceLimit = sourceLimit;
                  args.flush = flush;
                  args.offsets = offsets;
                  args.size = sizeof(args);
                  ToU_CALLBACK_MACRO(_this->toUContext,
                                     args,
                                     _this->invalidCharBuffer,
                                     _this->invalidCharLength,
                                     UCNV_UNASSIGNED,
                                     err);
                  if (U_FAILURE (*err))   break;
                  _this->invalidCharLength = 0;
                }
            }
        }
      else
        /* End of target buffer */
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

donefornow:
  *target += myTargetIndex;
  *source += mySourceIndex;
  _this->mode = ch; /*stores a partially calculated target*/
}

void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC (UConverter * _this,
                                                UChar ** target,
                                                const UChar * targetLimit,
                                                const char **source,
                                                const char *sourceLimit,
                                                int32_t *offsets,
                                                UBool flush,
                                                UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  uint32_t ch = 0, ch2 = 0, i = 0;
  uint32_t inBytes = 0;
  UConverterToUnicodeArgs args;

  if (_this->toUnicodeStatus)
    {
      i = _this->invalidCharLength;
      inBytes = _this->toUnicodeStatus;
      _this->toUnicodeStatus = 0;
      ch = _this->mode;
      goto morebytes;
    }

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          ch = mySource[mySourceIndex++];
          if (ch < 0x80)        /* Simple case */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (UChar) ch;
            }
          else
            {
              inBytes = bytesFromUTF8[ch];
              _this->invalidCharBuffer[0] = (char)ch;
              i = 1;

            morebytes:
              for (; i < inBytes; i++)
                {
                  {
                    if (mySourceIndex >= sourceLength)
                      {
                        if (flush)
                          {
                            if (U_SUCCESS(*err)) 
                              {
                                *err = U_TRUNCATED_CHAR_FOUND;
                                _this->toUnicodeStatus = 0x00;
                              }
                          }
                        else
                          {
                            _this->toUnicodeStatus = inBytes;
                            _this->invalidCharLength = (int8_t)i;
                          }
                        goto donefornow;
                      }
                    _this->invalidCharBuffer[i] = (char) (ch2 = mySource[mySourceIndex++]);
                    if ((ch2 & 0xC0) != 0x80)   /* Invalid trailing byte */
                      break;
                  }
                  ch <<= 6;
                  ch += ch2;
                }

              ch -= offsetsFromUTF8[inBytes];
              if (i == inBytes && ch <= kMaximumUTF16)
                {
                  if (ch <= kMaximumUCS2) {

                     offsets[myTargetIndex] = mySourceIndex-3;
                    myTarget[myTargetIndex++] = (UChar) ch;

                  }
                  else
                    {
                      ch -= halfBase;
                       offsets[myTargetIndex] = mySourceIndex-4;
                      myTarget[myTargetIndex++] = (UChar) ((ch >> halfShift) + kSurrogateHighStart);
                      ch = (ch & halfMask) + kSurrogateLowStart;
                      if (myTargetIndex < targetLength)
                        {
                           offsets[myTargetIndex] = mySourceIndex-4;
                          myTarget[myTargetIndex++] = (char)ch;
                        }
                      else
                        {
                          _this->invalidUCharBuffer[0] = (UChar) ch;
                          _this->invalidUCharLength = 1;
                          *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
                }
              else
                {
                  int32_t currentOffset = offsets[myTargetIndex-1];
                  int32_t My_i = myTargetIndex;

                  *err = U_ILLEGAL_CHAR_FOUND;
                  _this->invalidCharLength = (int8_t)i;
                  
                  args.converter = _this;
                  args.target = myTarget + myTargetIndex;
                  args.targetLimit = targetLimit;
                  args.source = (const char*)mySource + mySourceIndex;
                  args.sourceLimit = sourceLimit;
                  args.flush = flush;
                  args.offsets = offsets?offsets+myTargetIndex:0;
                  args.size = sizeof(args);
                  /* To do HSYS: more smarts here, including offsets */
                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(_this->toUContext,
                                     args,
                                     _this->invalidCharBuffer,
                                     _this->invalidCharLength,
                                     UCNV_UNASSIGNED,
                                     err);
/* Needed explicit cast for mySource on MVS to make compiler happy - JJD */

                  
                  if (U_FAILURE (*err))   break;
                  _this->invalidCharLength = 0;
              }
            }
        }
      else
        /* End of target buffer */
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

donefornow:
  *target += myTargetIndex;
  *source += mySourceIndex;
  _this->mode = ch;

}

void T_UConverter_fromUnicode_UTF8 (UConverter * _this,
                                    char **target,
                                    const char *targetLimit,
                                    const UChar ** source,
                                    const UChar * sourceLimit,
                                    int32_t *offsets,
                                    UBool flush,
                                    UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  uint32_t ch;
  int16_t i, bytesToWrite = 0;
  uint32_t ch2;
  char temp[4];

  if (_this->fromUnicodeStatus)
    {
      ch = _this->fromUnicodeStatus;
      _this->fromUnicodeStatus = 0;
      goto lowsurogate;
    }
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          bytesToWrite = 0;
          ch = mySource[mySourceIndex++];

          if (ch < 0x80)        /* Single byte */
            {
              myTarget[myTargetIndex++] = (char) ch;
            }
          else if (ch < 0x800)  /* Double byte */
            {
              myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
              if (myTargetIndex < targetLength)
                {
                  myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
              else
                {
                  _this->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                  _this->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else
            /* Check for surogates */
            {
              if ((ch >= kSurrogateHighStart) && (ch <= kSurrogateHighEnd))
                {
                lowsurogate:
                  if (mySourceIndex < sourceLength && !flush)
                    {
                      ch2 = mySource[mySourceIndex];
                      if ((ch2 >= kSurrogateLowStart) && (ch2 <= kSurrogateLowEnd))
                        {
                          ch = ((ch - kSurrogateHighStart) << halfShift) + (ch2 - kSurrogateLowStart) + halfBase;
                          ++mySourceIndex;
                        }
                    }
                }
              if (ch < 0x10000)
                {
                  bytesToWrite = 3;
                  temp[0] = (char) ((ch >> 12) | 0xe0);
                  temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[2] = (char) (ch & 0x3f | 0x80);
                }
              else
                {
                  bytesToWrite = 4;
                  temp[0] = (char) ((ch >> 18) | 0xf0);
                  temp[1] = (char) ((ch >> 12) & 0x3f | 0xe0);
                  temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[3] = (char) (ch & 0x3f | 0x80);
                }
              for (i = 0; i < bytesToWrite; i++)
                {
                  if (myTargetIndex < targetLength)
                    {
                      myTarget[myTargetIndex++] = temp[i];
                    }
                  else
                    {
                      _this->charErrorBuffer[_this->charErrorBufferLength++] = temp[i];
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
                }
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }

    }

  *target += myTargetIndex;
  *source += mySourceIndex;

  return;
}

void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverter * _this,
                                                  char **target,
                                                  const char *targetLimit,
                                                  const UChar ** source,
                                                  const UChar * sourceLimit,
                                                  int32_t *offsets,
                                                  UBool flush,
                                                  UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  uint32_t ch;
  int16_t i, bytesToWrite = 0;
  uint32_t ch2;
  char temp[4];

  if (_this->fromUnicodeStatus)
    {
      ch = _this->fromUnicodeStatus;
      _this->fromUnicodeStatus = 0;
      goto lowsurogate;
    }
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          bytesToWrite = 0;
          ch = mySource[mySourceIndex++];

          if (ch < 0x80)        /* Single byte */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (char) ch;
            }
          else if (ch < 0x800)  /* Double byte */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
              if (myTargetIndex < targetLength)
                {
                   offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
              else
                {
                  _this->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                  _this->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else
            /* Check for surogates */
            {
              if ((ch >= kSurrogateHighStart) && (ch <= kSurrogateHighEnd))
                {
                lowsurogate:
                  if (mySourceIndex < sourceLength && !flush)
                    {
                      ch2 = mySource[mySourceIndex];
                      if ((ch2 >= kSurrogateLowStart) && (ch2 <= kSurrogateLowEnd))
                        {
                          ch = ((ch - kSurrogateHighStart) << halfShift) + (ch2 - kSurrogateLowStart) + halfBase;
                          ++mySourceIndex;
                        }
                    }
                }
              if (ch < 0x10000)
                {
                  bytesToWrite = 3;
                  temp[0] = (char) ((ch >> 12) | 0xe0);
                  temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[2] = (char) (ch & 0x3f | 0x80);
                }
              else
                {
                  bytesToWrite = 4;
                  temp[0] = (char) ((ch >> 18) | 0xf0);
                  temp[1] = (char) ((ch >> 12) & 0x3f | 0xe0);
                  temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[3] = (char) (ch & 0x3f | 0x80);
                }
              for (i = 0; i < bytesToWrite; i++)
                {
                  if (myTargetIndex < targetLength)
                    {
                       offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = temp[i];
                    }
                  else
                    {
                      _this->charErrorBuffer[_this->charErrorBufferLength++] = temp[i];
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
                }
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }

    }

  *target += myTargetIndex;
  *source += mySourceIndex;

  return;
}

UChar32 T_UConverter_getNextUChar_UTF8(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  /*safe keeps a ptr to the beginning in case we need to step back*/
  char const *sourceInitial = *source;
  uint16_t extraBytesToWrite;
  uint8_t myByte;
  UChar32 ch;
  int8_t isLegalSequence = 1;
  UConverterToUnicodeArgs args;

  /*Input boundary check*/
  if ((*source) >= sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  myByte = (uint8_t)*((*source)++);
  if(myByte < 0x80) {
    return (UChar32)myByte;
  }
  extraBytesToWrite = (uint16_t)bytesFromUTF8[myByte];
  if (extraBytesToWrite == 0 || extraBytesToWrite > 4) {
    goto CALL_ERROR_FUNCTION;
  }
  

  /*The byte sequence is longer than the buffer area passed*/

  if ((*source + extraBytesToWrite - 1) > sourceLimit)
    {
      *err = U_TRUNCATED_CHAR_FOUND;
      return 0xFFFD;
    }
  else
    {
      ch = myByte << 6;
      switch(extraBytesToWrite)
        {     
          /* note: code falls through cases! (sic)*/ 
        case 6: ch += (myByte = (uint8_t)*((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 5: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 4: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 3: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 2: ch += (myByte = *((*source)++));
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
            }
        };
    }
  ch -= offsetsFromUTF8[extraBytesToWrite];

  
  if (isLegalSequence == 0) goto CALL_ERROR_FUNCTION;

  return ch; /* return the code point */

 CALL_ERROR_FUNCTION:
  {      
    /*rewinds source*/
    const char* sourceFinal = *source;
    UChar myUChar = (UChar)ch; /* ### TODO: this is a hack until we prepare the callbacks for code points */
    UChar* myUCharPtr = &myUChar;
    
    *err = U_ILLEGAL_CHAR_FOUND;
    *source = sourceInitial;
    
    /*It is very likely that the ErrorFunctor will write to the
     *internal buffers */
    args.converter = converter;
    args.target = myUCharPtr;
    args.targetLimit = myUCharPtr + 1;
    args.source = sourceFinal;
    args.sourceLimit = sourceLimit;
    args.flush = TRUE;
    args.offsets = NULL;  
    args.size = sizeof(args);
    converter->fromCharErrorBehaviour(converter->toUContext,
                                    &args,
                                    sourceFinal,
                                    sourceLimit-sourceFinal,
                                    UCNV_UNASSIGNED,
                                    err);

    
    /*makes the internal caching transparent to the user*/
    if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_ZERO_ERROR;
    
    return (UChar32)myUChar;
  }
} 

static const UConverterImpl _UTF8Impl={
    UCNV_UTF8,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF8,
    T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_UTF8,
    T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_UTF8,

    NULL
};


const UConverterStaticData _UTF8StaticData={
  sizeof(UConverterStaticData),
"UTF8",
    1208, UCNV_IBM, UCNV_UTF8, 1, 4,
    3, { 0xef, 0xbf, 0xbd, 0 }
};


const UConverterSharedData _UTF8Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF8StaticData, FALSE, &_UTF8Impl, 
    0
};

/* UTF-16BE ----------------------------------------------------------------- */

void T_UConverter_toUnicode_UTF16_BE (UConverter * _this,
                                      UChar ** target,
                                      const UChar * targetLimit,
                                      const char **source,
                                      const char *sourceLimit,
                                      int32_t *offsets,
                                      UBool flush,
                                      UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  UChar mySourceChar = 0x0000;
  UChar oldmySourceChar = 0x0000;


  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];
           oldmySourceChar = mySourceChar;
          if (_this->toUnicodeStatus == 0)
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar == 0x00 ? 0xFFFF : mySourceChar;
            }
          else
            {
              if (_this->toUnicodeStatus != 0xFFFF)
                mySourceChar = (UChar) ((_this->toUnicodeStatus << 8) | mySourceChar);
              _this->toUnicodeStatus = 0;



              myTarget[myTargetIndex++] = mySourceChar;

            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  if (U_SUCCESS(*err) && flush
      && (mySourceIndex == sourceLength)
      && (_this->toUnicodeStatus != 0x00))
    {
      if (U_SUCCESS(*err)) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          _this->toUnicodeStatus = 0x00;
        }
    }
  
  *target += myTargetIndex;
  *source += mySourceIndex;

  return;
}

void  T_UConverter_fromUnicode_UTF16_BE (UConverter * _this,
                                         char **target,
                                         const char *targetLimit,
                                         const UChar ** source,
                                         const UChar * sourceLimit,
                                         int32_t *offsets,
                                         UBool flush,
                                         UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  UChar mySourceChar;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
          if (myTargetIndex < targetLength)
            {
              myTarget[myTargetIndex++] = (char) mySourceChar;
            }
          else
            {
              _this->charErrorBuffer[0] = (char) mySourceChar;
              _this->charErrorBufferLength = 1;
              *err = U_INDEX_OUTOFBOUNDS_ERROR;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  *target += myTargetIndex;
  *source += mySourceIndex;;

  return;
}

UChar32 T_UConverter_getNextUChar_UTF16_BE(UConverter* converter,
                                                   const char** source,
                                                   const char* sourceLimit,
                                                   UErrorCode* err)
{
  UChar32 myUChar;
  /*Checks boundaries and set appropriate error codes*/
  if ((*source)+2 > sourceLimit) 
    {
      if ((*source) >= sourceLimit)
        {
          /*Either caller has reached the end of the byte stream*/
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
      else
        {
          /* a character was cut in half*/
          *err = U_TRUNCATED_CHAR_FOUND;
        }
      
      return 0xFFFD;
    }
  
  
  /*Gets the corresponding codepoint*/

  myUChar = ((uint16_t)((**source)) << 8) |((uint8_t)*((*source)+1));
  *source += 2;

  if(UTF_IS_FIRST_SURROGATE(myUChar)) {
    uint16_t second;

    if ((*source)+2 > sourceLimit) {
      *err = U_TRUNCATED_CHAR_FOUND;
      return 0xFFFD;
    }

    /* get the second surrogate and assemble the code point */
    second = ((uint16_t)((**source)) << 8) |((uint8_t)*((*source)+1));

    /* ignore unmatched surrogates and just deliver the first one in such a case */
    if(UTF_IS_SECOND_SURROGATE(second)) {
      /* matched pair, get pair value */
      myUChar = UTF16_GET_PAIR_VALUE(myUChar, second);
      *source += 2;
    }
  }

  return myUChar;
} 

static const UConverterImpl _UTF16BEImpl={
    UCNV_UTF16_BigEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF16_BE,
    NULL,
    T_UConverter_fromUnicode_UTF16_BE,
    NULL,
    T_UConverter_getNextUChar_UTF16_BE,

    NULL
};

const UConverterStaticData _UTF16BEStaticData={
  sizeof(UConverterStaticData),
"UTF16_BigEndian",
    1200, UCNV_IBM, UCNV_UTF16_BigEndian, 2, 2,
    2, { 0xff, 0xfd, 0, 0 }
};


const UConverterSharedData _UTF16BEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16BEStaticData, FALSE, &_UTF16BEImpl, 
    0
};

/* UTF-16LE ----------------------------------------------------------------- */

void  T_UConverter_toUnicode_UTF16_LE (UConverter * _this,
                                       UChar ** target,
                                       const UChar * targetLimit,
                                       const char **source,
                                       const char *sourceLimit,
                                       int32_t *offsets,
                                       UBool flush,
                                       UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  UChar mySourceChar = 0x0000;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];

          if (_this->toUnicodeStatus == 0x00)
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar == 0x00 ? 0xFFFF : mySourceChar;
            }
          else
            {
              if (_this->toUnicodeStatus == 0xFFFF)
                mySourceChar = (UChar) (mySourceChar << 8);
              else
                {
                  mySourceChar <<= 8;
                  mySourceChar |= (UChar) (_this->toUnicodeStatus);
                }
              _this->toUnicodeStatus = 0x00;
              myTarget[myTargetIndex++] = mySourceChar;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }


  if (U_SUCCESS(*err) && flush
      && (mySourceIndex == sourceLength)
      && (_this->toUnicodeStatus != 0x00))
    {
      if (U_SUCCESS(*err)) 
        {
          *err = U_TRUNCATED_CHAR_FOUND; 
          _this->toUnicodeStatus = 0x00;
        }
    }
  
  *target += myTargetIndex;
  *source += mySourceIndex;
  

  return;
}

void   T_UConverter_fromUnicode_UTF16_LE (UConverter * _this,
                                          char **target,
                                          const char *targetLimit,
                                          const UChar ** source,
                                          const UChar * sourceLimit,
                                          int32_t *offsets,
                                          UBool flush,
                                          UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  UChar mySourceChar;


  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          myTarget[myTargetIndex++] = (char) mySourceChar;
          if (myTargetIndex < targetLength)
            {
              myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
            }
          else
            {
              _this->charErrorBuffer[0] = (char) (mySourceChar >> 8);
              _this->charErrorBufferLength = 1;
              *err = U_INDEX_OUTOFBOUNDS_ERROR;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  *target += myTargetIndex;
  *source += mySourceIndex;;

  return;
}

UChar32 T_UConverter_getNextUChar_UTF16_LE(UConverter* converter,
                                                   const char** source,
                                                   const char* sourceLimit,
                                                   UErrorCode* err)
{
  UChar32 myUChar;
  /*Checks boundaries and set appropriate error codes*/
  if ((*source)+2 > sourceLimit) 
    {
      if ((*source) >= sourceLimit)
        {
          /*Either caller has reached the end of the byte stream*/
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
      else
        {
          /* a character was cut in half*/
          *err = U_TRUNCATED_CHAR_FOUND;
        }
      
      return 0xFFFD;
    }
  

  /*Gets the corresponding codepoint*/
  myUChar =  ((uint16_t)*((*source)+1) << 8) |((uint8_t)((**source)));
  /*updates the source*/
  *source += 2;  

  if(UTF_IS_FIRST_SURROGATE(myUChar)) {
    uint16_t second;

    if ((*source)+2 > sourceLimit) {
      *err = U_TRUNCATED_CHAR_FOUND;
      return 0xFFFD;
    }

    /* get the second surrogate and assemble the code point */
    second = ((uint16_t)*((*source)+1) << 8) |((uint8_t)((**source)));

    /* ignore unmatched surrogates and just deliver the first one in such a case */
    if(UTF_IS_SECOND_SURROGATE(second)) {
      /* matched pair, get pair value */
      myUChar = UTF16_GET_PAIR_VALUE(myUChar, second);
      *source += 2;
    }
  }

  return myUChar;
} 

static const UConverterImpl _UTF16LEImpl={
    UCNV_UTF16_LittleEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF16_LE,
    NULL,
    T_UConverter_fromUnicode_UTF16_LE,
    NULL,
    T_UConverter_getNextUChar_UTF16_LE,

    NULL
};


const UConverterStaticData _UTF16LEStaticData={
  sizeof(UConverterStaticData),
  "UTF16_LittleEndian",
  1200, UCNV_IBM, UCNV_UTF16_LittleEndian, 2, 2,
  2, { 0xfd, 0xff, 0, 0 }
};


const UConverterSharedData _UTF16LEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16LEStaticData, FALSE, &_UTF16LEImpl, 
    0
};
