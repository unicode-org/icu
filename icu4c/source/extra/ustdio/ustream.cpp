/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*  FILE NAME : ustream.cpp
*
*   Modification History:
*
*   Date        Name        Description
*   06/25/2001  grhoten     Move iostream from unistr.h to here
******************************************************************************
*/


#include "unicode/utypes.h"
#include "unicode/ustream.h"
#include "unicode/ucnv.h"
#include "ustr_imp.h"
#include <string.h>

// console IO

#if U_IOSTREAM_SOURCE >= 198506

#if U_IOSTREAM_SOURCE >= 199711
#define STD_NAMESPACE std::
#include <strstream>
#else
#define STD_NAMESPACE
#include <strstrea.h>
#endif

#define STD_OSTREAM STD_NAMESPACE ostream
#define STD_ISTREAM STD_NAMESPACE istream

U_USTDIO_API STD_OSTREAM &
operator<<(STD_OSTREAM& stream, const UnicodeString& str)
{
    if(str.length() > 0) {
        char buffer[200];
        UConverter *converter;
        UErrorCode errorCode = U_ZERO_ERROR;

        // use the default converter to convert chunks of text
        converter = u_getDefaultConverter(&errorCode);
        if(U_SUCCESS(errorCode)) {
            const UChar *us = str.getBuffer();
            const UChar *uLimit = us + str.length();
            char *s, *sLimit = buffer + sizeof(buffer);
            do {
                errorCode = U_ZERO_ERROR;
                s = buffer;
                ucnv_fromUnicode(converter, &s, sLimit, &us, uLimit, 0, FALSE, &errorCode);

                // write this chunk
                if(s > buffer) {
                    stream.write(buffer, s - buffer);
                }
            } while(errorCode == U_BUFFER_OVERFLOW_ERROR);
            u_releaseDefaultConverter(converter);
        }
    }

/*    stream.flush();*/
    return stream;
}

U_USTDIO_API STD_OSTREAM &
operator<<(STD_OSTREAM& stream, const UChar *s)
{
    UnicodeString localStr(s);
    return operator<<(stream, localStr);
}


U_USTDIO_API STD_ISTREAM &
operator>>(STD_ISTREAM& stream, UnicodeString& str)
{
    /* ipfx should eat whitespace when ios::skipws is set */
    char buffer[200];
    UChar uBuffer[sizeof(buffer) * 2];
    UConverter *converter;
    UErrorCode errorCode = U_ZERO_ERROR;

    str.truncate(0);
    // use the default converter to convert chunks of text
    converter = u_getDefaultConverter(&errorCode);
    if(U_SUCCESS(errorCode)) {
        UChar *us = uBuffer;
        const UChar *uLimit = uBuffer + sizeof(uBuffer)/sizeof(*uBuffer);
        const char *s, *sLimit;
        char ch;
        uint32_t idx;
        UBool strDone = FALSE;

        /* stream.eatwhite() doesn't seem to be available. Do it manually.*/
        while (!stream.eof()) {
            if (!isspace(ch = stream.get())) {
                stream.putback(ch);
                break;
            }
        }
        do {
            idx = 0;
            while (!stream.eof() && idx < sizeof(buffer)) {
                if (isspace(ch = stream.get())) {
                    stream.putback(ch);
                    strDone = TRUE;
                    break;
                }
                else {
                    buffer[idx++] = ch;
                }
            }
            s = buffer;
            sLimit = buffer + idx;
            do {
                errorCode = U_ZERO_ERROR;
                us = uBuffer;
                ucnv_toUnicode(converter, &us, uLimit, &s, sLimit, 0, FALSE, &errorCode);
                str.append(uBuffer, (int32_t)(us - uBuffer));
            } while(errorCode == U_BUFFER_OVERFLOW_ERROR);
        } while (!strDone);
        u_releaseDefaultConverter(converter);
    }

/*    stream.flush();*/
    return stream;
}


#endif

#if 0
/* UnicodeStringStreamer insternal API may be useful for future reference */
#ifndef UNISTRM_H
#define UNISTRM_H

#include "filestrm.h"
#include "umemstrm.h"
#include "unicode/unistr.h"


class U_COMMON_API UnicodeStringStreamer
{
public:
    static void streamIn(UnicodeString* string, FileStream* is);
    static void streamOut(const UnicodeString* string, FileStream* os);
    static void streamIn(UnicodeString* string, UMemoryStream* is);
    static void streamOut(const UnicodeString* string, UMemoryStream* os);
};


#endif

//========================================
// Streaming (to be removed)
//========================================

#include "unistrm.h"
#include "filestrm.h"


inline uint8_t
uprv_hibyte(uint16_t x)
{ return (uint8_t)(x >> 8); }

inline uint8_t
uprv_lobyte(uint16_t x)
{ return (uint8_t)(x & 0xff); }

inline uint16_t
uprv_hiword(uint32_t x)
{ return (uint16_t)(x >> 16); }

inline uint16_t
uprv_loword(uint32_t x)
{ return (uint16_t)(x & 0xffff); }

inline void
writeLong(FileStream *os,
      int32_t x)
{
  uint16_t word = uprv_hiword((uint32_t)x);
  T_FileStream_putc(os, uprv_hibyte(word));
  T_FileStream_putc(os, uprv_lobyte(word));
  word = uprv_loword((uint32_t)x);
  T_FileStream_putc(os, uprv_hibyte(word));
  T_FileStream_putc(os, uprv_lobyte(word));
}

inline int32_t
readLong(FileStream *is)
{
  int32_t x = T_FileStream_getc(is);

  x = (x << 8) | T_FileStream_getc(is);
  x = (x << 8) | T_FileStream_getc(is);
  x = (x << 8) | T_FileStream_getc(is);

  return x;
}

inline void
writeUChar(FileStream *os,
       UChar c)
{
  T_FileStream_putc(os, uprv_hibyte(c));
  T_FileStream_putc(os, uprv_lobyte(c));
}

inline UChar
readUChar(FileStream *is)
{
  UChar c = (UChar)T_FileStream_getc(is);

  return (UChar)((c << 8) | T_FileStream_getc(is));
}

void
UnicodeStringStreamer::streamOut(const UnicodeString *s,
                 FileStream *os)
{
  if(!T_FileStream_error(os)) {
    writeLong(os, s->fLength);
  }

  const UChar *c   = s->getArrayStart();
  const UChar *end = c + s->fLength;

  while(c != end && ! T_FileStream_error(os)) {
    writeUChar(os, *c++);
  }
}

void
UnicodeStringStreamer::streamIn(UnicodeString *s,
                FileStream *is)
{
  int32_t newSize;

  // handle error conditions
  if(T_FileStream_error(is) || T_FileStream_eof(is)) {
    s->setToBogus();
    return;
  }
  newSize = readLong(is);
  if((newSize < 0) || T_FileStream_error(is)
     || ((newSize > 0) && T_FileStream_eof(is))) {
    s->setToBogus(); //error condition
    return;
  }

  // clone s's array, if needed
  if(!s->cloneArrayIfNeeded(newSize, newSize, FALSE)) {
    return;
  }

  UChar *c = s->getArrayStart();
  UChar *end = c + newSize;

  while(c < end && ! (T_FileStream_error(is) || T_FileStream_eof(is))) {
    *c++ = readUChar(is);
  }

  // couldn't read all chars
  if(c < end) {
    s->setToBogus();
    return;
  }

  s->fLength = newSize;
}

void
UnicodeStringStreamer::streamOut(const UnicodeString *s,
                 UMemoryStream *os)
{
  if(!uprv_mstrm_error(os)) {
    uprv_mstrm_write(os, (uint8_t*)&s->fLength, (int32_t)sizeof(s->fLength));
  }

  const UChar *c   = s->getArrayStart();
  const UChar *end = c + s->fLength;

  while(c != end && ! uprv_mstrm_error(os)) {
    uprv_mstrm_write(os, (uint8_t*)c, (int32_t)sizeof(*c));
    c++;
  }
}

void
UnicodeStringStreamer::streamIn(UnicodeString *s,
                UMemoryStream *is)
{
  int32_t newSize;

  // handle error conditions
  if(uprv_mstrm_error(is) || uprv_mstrm_eof(is)) {
    s->setToBogus();
    return;
  }
  uprv_mstrm_read(is, (uint8_t *)&newSize, (int32_t)sizeof(int32_t));
  if((newSize < 0) || uprv_mstrm_error(is)
     || ((newSize > 0) && uprv_mstrm_eof(is))) {
    s->setToBogus(); //error condition
    return;
  }

  // clone s's array, if needed
  if(!s->cloneArrayIfNeeded(newSize, newSize, FALSE)) {
    return;
  }

  UChar *c = s->getArrayStart();
  UChar *end = c + newSize;

  while(c < end && ! (uprv_mstrm_error(is) || uprv_mstrm_eof(is))) {
    uprv_mstrm_read(is, (uint8_t *)c, (int32_t)sizeof(*c));
    c++;
  }

  // couldn't read all chars
  if(c < end) {
    s->setToBogus();
    return;
  }

  s->fLength = newSize;
}

#endif
