/*
******************************************************************************
* Copyright (C) 1999-2001, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* File unistr.cpp
*
* Modification History:
*
*   Date        Name        Description
*   09/25/98    stephen     Creation.
*   04/20/99    stephen     Overhauled per 4/16 code review.
*   07/09/99    stephen     Renamed {hi,lo},{byte,word} to icu_X for HP/UX
*   11/18/99    aliu        Added handleReplaceBetween() to make inherit from
*                           Replaceable.
******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "unicode/locid.h"
#include "cstring.h"
#include "cmemory.h"
#include "unicode/ustring.h"
#include "mutex.h"
#include "unicode/unistr.h"
#include "unicode/unicode.h"
#include "unicode/ucnv.h"
#include "uhash.h"
#include "ustr_imp.h"

#if U_IOSTREAM_SOURCE >= 199711
#include <iostream>
using namespace std;
#elif U_IOSTREAM_SOURCE >= 198506
#include <iostream.h>
#endif

#if 0
//DEBUGGING
void
print(const UnicodeString& s,
      const char *name)
{
  UChar c;
  cout << name << ":|";
  for(int i = 0; i < s.length(); ++i) {
    c = s[i];
    if(c>= 0x007E || c < 0x0020)
      cout << "[0x" << hex << s[i] << "]";
    else
      cout << (char) s[i];
  }
  cout << '|' << endl;
}

void
print(const UChar *s,
      int32_t len,
      const char *name)
{
  UChar c;
  cout << name << ":|";
  for(int i = 0; i < len; ++i) {
    c = s[i];
    if(c>= 0x007E || c < 0x0020)
      cout << "[0x" << hex << s[i] << "]";
    else
      cout << (char) s[i];
  }
  cout << '|' << endl;
}
// END DEBUGGING
#endif

// Local function definitions for now

// need to copy areas that may overlap
inline void
us_arrayCopy(const UChar *src, int32_t srcStart,
         UChar *dst, int32_t dstStart, int32_t count)
{
  if(count>0) {
    uprv_memmove(dst+dstStart, src+srcStart, (size_t)(count*sizeof(*src)));
  }
}

UConverter* UnicodeString::fgDefaultConverter  = 0;

//========================================
// Constructors
//========================================
UnicodeString::UnicodeString()
  : fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{}

UnicodeString::UnicodeString(int32_t capacity, UChar32 c, int32_t count)
  : fCapacity(US_STACKBUF_SIZE),
    fArray(0),
    fFlags(0)
{
  if(count <= 0) {
    // just allocate and do not do anything else
    allocate(capacity);
  } else {
    // count > 0, allocate and fill the new string with count c's
    int32_t unitCount = UTF_CHAR_LENGTH(c), length = count * unitCount;
    if(capacity < length) {
      capacity = length;
    }
    if(allocate(capacity)) {
      int32_t i = 0;

      // fill the new string with c
      if(unitCount == 1) {
        // fill with length UChars
        while(i < length) {
          fArray[i++] = (UChar)c;
        }
      } else {
        // get the code units for c
        UChar units[UTF_MAX_CHAR_LENGTH];
        UTF_APPEND_CHAR_UNSAFE(units, i, c);

        // now it must be i==unitCount
        i = 0;

        // for Unicode, unitCount can only be 1, 2, 3, or 4
        // 1 is handled above
        switch(unitCount) {
        case 2:
          while(i < length) {
            fArray[i++]=units[0];
            fArray[i++]=units[1];
          }
          break;
        case 3:
          while(i < length) {
            fArray[i++]=units[0];
            fArray[i++]=units[1];
            fArray[i++]=units[2];
          }
          break;
        case 4:
          while(i < length) {
            fArray[i++]=units[0];
            fArray[i++]=units[1];
            fArray[i++]=units[2];
            fArray[i++]=units[3];
          }
          break;
        default:
          break;
        }
      }
    }
    fLength = length;
  }
}

UnicodeString::UnicodeString(UChar ch)
  : Replaceable(1),
    fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  fStackBuffer[0] = ch;
}

UnicodeString::UnicodeString(UChar32 ch)
  : Replaceable(1),
    fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  UTextOffset i = 0;
  UTF_APPEND_CHAR(fStackBuffer, i, US_STACKBUF_SIZE, ch);
  fLength = i;
}

UnicodeString::UnicodeString(const UChar *text)
  : fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  doReplace(0, 0, text, 0, u_strlen(text));
}

UnicodeString::UnicodeString(const UChar *text,
                             int32_t textLength)
  : fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  doReplace(0, 0, text, 0, textLength);
}

UnicodeString::UnicodeString(UBool isTerminated,
                             const UChar *text,
                             int32_t textLength)
  : Replaceable(textLength),
    fCapacity(isTerminated ? textLength + 1 : textLength),
    fArray((UChar *)text),
    fFlags(kReadonlyAlias)
{
  if(text == 0 || textLength < -1 || textLength == -1 && !isTerminated) {
    setToBogus();
  } else if(textLength == -1) {
    // text is terminated, or else it would have failed the above test
    fLength = u_strlen(text);
    fCapacity = fLength + 1;
  }
}

UnicodeString::UnicodeString(UChar *buff,
                             int32_t bufLength,
                             int32_t buffCapacity)
  : Replaceable(bufLength),
    fCapacity(buffCapacity),
    fArray(buff),
    fFlags(kWriteableAlias)
{
  if(buff == 0 || bufLength < 0 || bufLength > buffCapacity) {
    setToBogus();
  }
}

UnicodeString::UnicodeString(const char *codepageData,
                             const char *codepage)
  : fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  if(codepageData != 0) {
    doCodepageCreate(codepageData, uprv_strlen(codepageData), codepage);
  }
}


UnicodeString::UnicodeString(const char *codepageData,
                             int32_t dataLength,
                             const char *codepage)
  : fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  if(codepageData != 0) {
    doCodepageCreate(codepageData, dataLength, codepage);
  }
}

UnicodeString::UnicodeString(const UnicodeString& that)
  : Replaceable(),
    fCapacity(US_STACKBUF_SIZE),
    fArray(fStackBuffer),
    fFlags(kShortString)
{
  *this = that;
}

//========================================
// array allocation
//========================================

UBool
UnicodeString::allocate(int32_t capacity) {
  if(capacity <= US_STACKBUF_SIZE) {
    fArray = fStackBuffer;
    fCapacity = US_STACKBUF_SIZE;
    fFlags = kShortString;
  } else {
    // count bytes for the refCounter and the string capacity, and
    // round up to a multiple of 16; then divide by 4 and allocate int32_t's
    // to be safely aligned for the refCount
    int32_t words = ((sizeof(int32_t) + capacity * U_SIZEOF_UCHAR + 15) & ~15) >> 2;
    int32_t *array = new int32_t[words];
    if(array != 0) {
      // set initial refCount and point behind the refCount
      *array++ = 1;

      // have fArray point to the first UChar
      fArray = (UChar *)array;
      fCapacity = (words - 1) * (sizeof(int32_t) / U_SIZEOF_UCHAR);
      fFlags = kLongString;
    } else {
      fLength = 0;
      fCapacity = 0;
      fFlags = kIsBogus;
      return FALSE;
    }
  }
  return TRUE;
}

//========================================
// Destructor
//========================================
UnicodeString::~UnicodeString()
{
  releaseArray();
}

//========================================
// Assignment
//========================================
UnicodeString&
UnicodeString::operator= (const UnicodeString& src)
{
  // if assigning to ourselves, do nothing
  if(this == 0 || this == &src) {
    return *this;
  }

  // is the right side bogus?
  if(&src == 0 || src.isBogus()) {
    setToBogus();
    return *this;
  }

  // delete the current contents
  releaseArray();

  // we always copy the length and the hash code
  fLength = src.fLength;

  switch(src.fFlags) {
  case kShortString:
    // short string using the stack buffer, do the same
    fArray = fStackBuffer;
    fCapacity = US_STACKBUF_SIZE;
    fFlags = kShortString;
    if(fLength > 0) {
      uprv_memcpy(fStackBuffer, src.fArray, fLength * U_SIZEOF_UCHAR);
    }
    break;
  case kLongString:
    // src uses a refCounted string buffer, use that buffer with refCount
    // src is const, use a cast - we don't really change it
    ((UnicodeString &)src).addRef();
    // fall through to readonly alias copying: copy all fields
  case kReadonlyAlias:
    // src is a readonly alias, do the same
    fArray = src.fArray;
    fCapacity = src.fCapacity;
    fFlags = src.fFlags;
    break;
  case kWriteableAlias:
    // src is a writeable alias; we make a copy of that instead
    if(allocate(fLength)) {
      if(fLength > 0) {
        uprv_memcpy(fArray, src.fArray, fLength * U_SIZEOF_UCHAR);
      }
      break;
    }
    // if there is not enough memory, then fall through to setting to bogus
  default:
    // if src is bogus, set ourselves to bogus
    // do not call setToBogus() here because fArray and fFlags are not consistent here
    fArray = 0;
    fLength = 0;
    fCapacity = 0;
    fFlags = kIsBogus;
    break;
  }

  return *this;
}

//========================================
// Miscellaneous operations
//========================================
int32_t
UnicodeString::numDisplayCells( UTextOffset start,
                int32_t length,
                UBool asian) const
{
  // pin indices to legal values
  pinIndices(start, length);

  UChar32 c;
  int32_t result = 0;
  UTextOffset limit = start + length;

  while(start < limit) {
    UTF_NEXT_CHAR(fArray, start, limit, c);
    switch(Unicode::getCellWidth(c)) {
    case Unicode::ZERO_WIDTH:
      break;

    case Unicode::HALF_WIDTH:
      result += 1;
      break;

    case Unicode::FULL_WIDTH:
      result += 2;
      break;

    case Unicode::NEUTRAL:
      result += (asian ? 2 : 1);
      break;
    }
  }

  return result;
}

UCharReference
UnicodeString::operator[] (UTextOffset pos)
{
  return UCharReference(this, pos);
}

UnicodeString UnicodeString::unescape() const {
    UnicodeString result;
    for (int32_t i=0; i<length(); ) {
        UChar32 c = charAt(i++);
        if (c == 0x005C /*'\\'*/) {
            c = unescapeAt(i); // advances i
            if (c == (UChar32)0xFFFFFFFF) {
                result.remove(); // return empty string
                break; // invalid escape sequence
            }
        }
        result.append(c);
    }
    return result;
}

// u_unescapeAt() callback to get a UChar from a UnicodeString
U_CFUNC UChar _charAt(int32_t offset, void *context) {
    return ((UnicodeString*) context)->charAt(offset);
}

UChar32 UnicodeString::unescapeAt(int32_t &offset) const {
    return u_unescapeAt(_charAt, &offset, length(), (void*)this);
}

//========================================
// Read-only implementation
//========================================
int8_t
UnicodeString::doCompare( UTextOffset start,
              int32_t length,
              const UChar *srcChars,
              UTextOffset srcStart,
              int32_t srcLength) const
{
  // compare illegal string values
  if(isBogus()) {
    if(srcChars==0) {
      return 0;
    } else {
      return -1;
    }
  } else if(srcChars==0) {
    return 1;
  }

  // pin indices to legal values
  pinIndices(start, length);

  // get the correct pointer
  const UChar *chars = getArrayStart();

  // are we comparing the same buffer contents?
  chars += start;
  srcChars += srcStart;
  if(chars == srcChars) {
    return 0;
  }

  UTextOffset minLength;
  int8_t lengthResult;

  // are we comparing different lengths?
  if(length != srcLength) {
    if(length < srcLength) {
      minLength = length;
      lengthResult = -1;
    } else {
      minLength = srcLength;
      lengthResult = 1;
    }
  } else {
    minLength = length;
    lengthResult = 0;
  }

  /*
   * note that uprv_memcmp() returns an int but we return an int8_t;
   * we need to take care not to truncate the result -
   * one way to do this is to right-shift the value to
   * move the sign bit into the lower 8 bits and making sure that this
   * does not become 0 itself
   */

  if(minLength > 0) {
    int32_t result;

#   if U_IS_BIG_ENDIAN 
      // big-endian: byte comparison works
      result = uprv_memcmp(chars, srcChars, minLength * sizeof(UChar));
      if(result != 0) {
        return (int8_t)(result >> 15 | 1);
      }
#   else
      // little-endian: compare UChar units
      do {
        result = ((int32_t)*(chars++) - (int32_t)*(srcChars++));
        if(result != 0) {
          return (int8_t)(result >> 15 | 1);
        }
      } while(--minLength > 0);
#   endif
  }
  return lengthResult;
}

/* String compare in code point order - doCompare() compares in code unit order. */
int8_t
UnicodeString::doCompareCodePointOrder(UTextOffset start,
                                       int32_t length,
                                       const UChar *srcChars,
                                       UTextOffset srcStart,
                                       int32_t srcLength) const
{
  // compare illegal string values
  if(isBogus()) {
    if(srcChars==0) {
      return 0;
    } else {
      return -1;
    }
  } else if(srcChars==0) {
    return 1;
  }

  // pin indices to legal values
  pinIndices(start, length);

  // get the correct pointer
  const UChar *chars = getArrayStart();

  // are we comparing the same buffer contents?
  chars += start;
  srcChars += srcStart;
  if(chars == srcChars) {
    return 0;
  }

  UTextOffset minLength;
  int8_t lengthResult;

  // are we comparing different lengths?
  if(length != srcLength) {
    if(length < srcLength) {
      minLength = length;
      lengthResult = -1;
    } else {
      minLength = srcLength;
      lengthResult = 1;
    }
  } else {
    minLength = length;
    lengthResult = 0;
  }

  static const UChar utf16Fixup[32]={
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0x2000, 0xf800, 0xf800, 0xf800, 0xf800
  };
  UChar c1, c2;
  int32_t diff;

  /* rotate each code unit's value so that surrogates get the highest values */
  while(minLength>0) {
    c1=*chars;
    c1+=utf16Fixup[c1>>11]; /* additional "fix-up" line */
    c2=*srcChars;
    c2+=utf16Fixup[c2>>11]; /* additional "fix-up" line */

    /* now c1 and c2 are in UTF-32-compatible order */
    diff=(int32_t)c1-(int32_t)c2;
    if(diff!=0) {
      return (int8_t)(diff >> 15 | 1);
    }
    ++chars;
    ++srcChars;
    --minLength;
  }
  return lengthResult;
}

int8_t
UnicodeString::doCaseCompare(UTextOffset start,
                             int32_t length,
                             const UChar *srcChars,
                             UTextOffset srcStart,
                             int32_t srcLength,
                             uint32_t options) const
{
  // compare illegal string values
  if(isBogus()) {
    if(srcChars==0) {
      return 0;
    } else {
      return -1;
    }
  } else if(srcChars==0) {
    return 1;
  }

  // pin indices to legal values
  pinIndices(start, length);

  // get the correct pointer
  const UChar *chars = getArrayStart();

  // are we comparing the same buffer contents?
  chars += start;
  srcChars += srcStart;
  if(chars == srcChars) {
    return 0;
  }

  int32_t result=u_internalStrcasecmp(chars, length, srcChars, srcLength, options);
  if(result!=0) {
    return (int8_t)(result >> 24 | 1);
  } else {
    return 0;
  }
}

UChar
UnicodeString::getCharAt(UTextOffset offset) const {
  return charAt(offset);
}

UChar32
UnicodeString::getChar32At(UTextOffset offset) const {
  return char32At(offset);
}

void
UnicodeString::doExtract(UTextOffset start,
             int32_t length,
             UChar *dst,
             UTextOffset dstStart) const
{
  // do not copy anything if we alias dst itself
  if(fArray + start != dst + dstStart) {
    // pin indices to legal values
    pinIndices(start, length);
    us_arrayCopy(getArrayStart(), start, dst, dstStart, length);
  }
}

UTextOffset 
UnicodeString::indexOf(const UChar *srcChars,
               UTextOffset srcStart,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{
  if(isBogus() || srcChars == 0 || srcStart < 0 || srcLength <= 0) {
    return -1;
  }

  // now we will only work with srcLength-1
  --srcLength;

  // get the indices within bounds
  pinIndices(start, length);

  // set length for the last possible match start position
  // note the --srcLength above
  length -= srcLength;

  if(length <= 0) {
    return -1;
  }

  const UChar *array = getArrayStart();
  UTextOffset limit = start + length;

  // search for the first char, then compare the rest of the string
  // increment srcStart here for that, matching the --srcLength above
  UChar ch = srcChars[srcStart++];

  do {
    if(array[start] == ch && (srcLength == 0 || compare(start + 1, srcLength, srcChars, srcStart, srcLength) == 0)) {
      return start;
    }
  } while(++start < limit);

  return -1;
}

UTextOffset
UnicodeString::doIndexOf(UChar c,
             UTextOffset start,
             int32_t length) const
{
  // pin indices
  pinIndices(start, length);
  if(length == 0) {
    return -1;
  }

  // find the first occurrence of c
  const UChar *begin = getArrayStart() + start;
  const UChar *limit = begin + length;

  do {
    if(*begin == c) {
      return begin - getArrayStart();
    }
  } while(++begin < limit);

  return -1;
}

UTextOffset 
UnicodeString::lastIndexOf(const UChar *srcChars,
               UTextOffset srcStart,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{
  if(isBogus() || srcChars == 0 || srcStart < 0 || srcLength <= 0) {
    return -1;
  }

  // now we will only work with srcLength-1
  --srcLength;

  // get the indices within bounds
  pinIndices(start, length);

  // set length for the last possible match start position
  // note the --srcLength above
  length -= srcLength;

  if(length <= 0) {
    return -1;
  }

  const UChar *array = getArrayStart();
  UTextOffset pos;

  // search for the first char, then compare the rest of the string
  // increment srcStart here for that, matching the --srcLength above
  UChar ch = srcChars[srcStart++];

  pos = start + length;
  do {
    if(array[--pos] == ch && (srcLength == 0 || compare(pos + 1, srcLength, srcChars, srcStart, srcLength) == 0)) {
      return pos;
    }
  } while(pos > start);

  return -1;
}

UTextOffset
UnicodeString::doLastIndexOf(UChar c,
                 UTextOffset start,
                 int32_t length) const
{
  if(isBogus()) {
    return -1;
  }

  // pin indices
  pinIndices(start, length);
  if(length == 0) {
    return -1;
  }

  const UChar *begin = getArrayStart() + start;
  const UChar *limit = begin + length;

  do {
    if(*--limit == c) {
      return limit - getArrayStart();
    }
  } while(limit > begin);

  return -1;
}

UnicodeString& 
UnicodeString::findAndReplace(UTextOffset start,
                  int32_t length,
                  const UnicodeString& oldText,
                  UTextOffset oldStart,
                  int32_t oldLength,
                  const UnicodeString& newText,
                  UTextOffset newStart,
                  int32_t newLength)
{
  if(isBogus() || oldText.isBogus() || newText.isBogus()) {
    return *this;
  }

  pinIndices(start, length);
  oldText.pinIndices(oldStart, oldLength);
  newText.pinIndices(newStart, newLength);

  if(oldLength == 0) {
    return *this;
  }

  while(length > 0 && length >= oldLength) {
    UTextOffset pos = indexOf(oldText, oldStart, oldLength, start, length);
    if(pos < 0) {
      // no more oldText's here: done
      break;
    } else {
      // we found oldText, replace it by newText and go beyond it
      replace(pos, oldLength, newText, newStart, newLength);
      length -= pos + oldLength - start;
      start = pos + newLength;
    }
  }

  return *this;
}


//========================================
// Write implementation
//========================================

void
UnicodeString::setToBogus()
{
  releaseArray();

  fArray = 0;
  fCapacity = fLength = 0;
  fFlags = kIsBogus;
}

// setTo() analogous to the readonly-aliasing constructor with the same signature
UnicodeString &
UnicodeString::setTo(UBool isTerminated,
                     const UChar *text,
                     int32_t textLength)
{
  if(text == 0 || textLength < -1 || textLength == -1 && !isTerminated) {
    setToBogus();
    return *this;
  }

  releaseArray();

  fArray = (UChar *)text;
  if(textLength != -1) {
    fLength = textLength;
  } else {
    // text is terminated, or else it would have failed the above test
    fLength = u_strlen(text);
    fCapacity = fLength + 1;
  }

  fCapacity = isTerminated ? fLength + 1 : fLength;
  fFlags = kReadonlyAlias;
  return *this;
}

// setTo() analogous to the writeable-aliasing constructor with the same signature
UnicodeString &
UnicodeString::setTo(UChar *buffer,
                     int32_t buffLength,
                     int32_t buffCapacity) {
  if(buffer == 0 || buffLength < 0 || buffLength > buffCapacity) {
    setToBogus();
    return *this;
  }

  releaseArray();

  fArray = buffer;
  fLength = buffLength;
  fCapacity = buffCapacity;
  fFlags = kWriteableAlias;
  return *this;
}

UnicodeString&
UnicodeString::setCharAt(UTextOffset offset,
             UChar c)
{
  if(cloneArrayIfNeeded()) {
    if(offset < 0) {
      offset = 0;
    } else if(offset >= fLength) {
      offset = fLength - 1;
    }

    fArray[offset] = c;
  }
  return *this;
}

/*
 * Implement argument checking and buffer handling
 * for string case mapping as a common function.
 */
enum {
    TO_LOWER,
    TO_UPPER,
    FOLD_CASE
};

UnicodeString &
UnicodeString::toLower() {
  return caseMap(Locale::getDefault(), 0, TO_LOWER);
}

UnicodeString &
UnicodeString::toLower(const Locale &locale) {
  return caseMap(locale, 0, TO_LOWER);
}

UnicodeString &
UnicodeString::toUpper() {
  return caseMap(Locale::getDefault(), 0, TO_UPPER);
}

UnicodeString &
UnicodeString::toUpper(const Locale &locale) {
  return caseMap(locale, 0, TO_UPPER);
}

UnicodeString &
UnicodeString::foldCase(uint32_t options) {
  return caseMap(Locale(), options, TO_LOWER);
}

// static helper function for string case mapping
// called by u_internalStrToUpper/Lower()
UBool U_CALLCONV
UnicodeString::growBuffer(void *context,
                          UChar **buffer, int32_t *pCapacity, int32_t reqCapacity,
                          int32_t length) {
  UnicodeString *me = (UnicodeString *)context;
  me->fLength = length;
  if(me->cloneArrayIfNeeded(reqCapacity)) {
    *buffer = me->fArray;
    *pCapacity = me->fCapacity;
    return TRUE;
  } else {
    return FALSE;
  }
}

UnicodeString &
UnicodeString::caseMap(const Locale& locale,
                       uint32_t options,
                       int32_t toWhichCase) {
  if(fLength <= 0) {
    // nothing to do
    return *this;
  }

  // We need to allocate a new buffer for the internal string case mapping function.
  // This is very similar to how doReplace() below keeps the old array pointer
  // and deletes the old array itself after it is done.
  // In addition, we are forcing cloneArrayIfNeeded() to always allocate a new array.
  UChar *oldArray = fArray;
  int32_t oldLength = fLength;
  int32_t *bufferToDelete = 0;

  // Make sure that if the string is in fStackBuffer we do not overwrite it!
  int32_t capacity;
  if(fLength <= US_STACKBUF_SIZE) {
    if(fArray == fStackBuffer) {
      capacity = 2 * US_STACKBUF_SIZE; // make sure that cloneArrayIfNeeded() allocates a new buffer
    } else {
      capacity = US_STACKBUF_SIZE;
    }
  } else {
    capacity = fLength + 2;
  }
  if(!cloneArrayIfNeeded(capacity, capacity, FALSE, &bufferToDelete, TRUE)) {
    return *this;
  }

  UErrorCode errorCode = U_ZERO_ERROR;
  if(toWhichCase==TO_LOWER) {
    fLength = u_internalStrToLower(fArray, fCapacity,
                                   oldArray, oldLength,
                                   locale.getName(),
                                   growBuffer, this,
                                   &errorCode);
  } else if(toWhichCase==TO_UPPER) {
    fLength = u_internalStrToUpper(fArray, fCapacity,
                                   oldArray, oldLength,
                                   locale.getName(),
                                   growBuffer, this,
                                   &errorCode);
  } else {
    fLength = u_internalStrFoldCase(fArray, fCapacity,
                                    oldArray, oldLength,
                                    options,
                                    growBuffer, this,
                                    &errorCode);
  }

  delete [] bufferToDelete;
  if(U_FAILURE(errorCode)) {
    setToBogus();
  }
  return *this;
}

UnicodeString&
UnicodeString::doReplace( UTextOffset start,
              int32_t length,
              const UnicodeString& src,
              UTextOffset srcStart,
              int32_t srcLength)
{
  if(!src.isBogus()) {
    // pin the indices to legal values
    src.pinIndices(srcStart, srcLength);

    // get the characters from src
    // and replace the range in ourselves with them
    return doReplace(start, length, src.getArrayStart(), srcStart, srcLength);
  } else {
    // remove the range
    return doReplace(start, length, 0, 0, 0);
  }
}

UnicodeString&
UnicodeString::doReplace(UTextOffset start,
             int32_t length,
             const UChar *srcChars,
             UTextOffset srcStart,
             int32_t srcLength)
{
  // if we're bogus, set us to empty first
  if(isBogus()) {
    fArray = fStackBuffer;
    fLength = 0;
    fCapacity = US_STACKBUF_SIZE;
    fFlags = kShortString;
  }

  if(srcChars == 0) {
    srcStart = srcLength = 0;
  }

  int32_t *bufferToDelete = 0;

  // the following may change fArray but will not copy the current contents;
  // therefore we need to keep the current fArray
  UChar *oldArray = fArray;
  int32_t oldLength = fLength;

  // pin the indices to legal values
  pinIndices(start, length);

  // calculate the size of the string after the replace
  int32_t newSize = oldLength - length + srcLength;

  // clone our array and allocate a bigger array if needed
  if(!cloneArrayIfNeeded(newSize, newSize + (newSize >> 2) + kGrowSize,
                         FALSE, &bufferToDelete)
  ) {
    return *this;
  }

  // now do the replace

  if(fArray != oldArray) {
    // if fArray changed, then we need to copy everything except what will change
    us_arrayCopy(oldArray, 0, fArray, 0, start);
    us_arrayCopy(oldArray, start + length,
                 fArray, start + srcLength,
                 oldLength - (start + length));
  } else if(length != srcLength) {
    // fArray did not change; copy only the portion that isn't changing, leaving a hole
    us_arrayCopy(oldArray, start + length,
                 fArray, start + srcLength,
                 oldLength - (start + length));
  }

  // now fill in the hole with the new string
  us_arrayCopy(srcChars, srcStart, getArrayStart(), start, srcLength);

  fLength = newSize;

  // delayed delete in case srcChars == fArray when we started, and
  // to keep oldArray alive for the above operations
  delete [] bufferToDelete;

  return *this;
}

/**
 * Replaceable API
 */
void
UnicodeString::handleReplaceBetween(UTextOffset start,
                                    UTextOffset limit,
                                    const UnicodeString& text) {
    replaceBetween(start, limit, text);
}

/**
 * Replaceable API
 */
void 
UnicodeString::copy(int32_t start, int32_t limit, int32_t dest) {
    UChar* text = new UChar[limit - start];
    extractBetween(start, limit, text, 0);
    insert(dest, text, 0, limit - start);    
    delete[] text;
}

UnicodeString&
UnicodeString::doReverse(UTextOffset start,
             int32_t length)
{
  // if we're bogus, do nothing
  if(isBogus() || !cloneArrayIfNeeded()) {
    return *this;
  }

  // pin the indices to legal values
  pinIndices(start, length);

  UChar *left = getArrayStart() + start;
  UChar *right = getArrayStart() + start + length;
  UChar swap;

  while(left < --right) {
    swap = *left;
    *left++ = *right;
    *right = swap;
  }

  return *this;
}

UBool 
UnicodeString::padLeading(int32_t targetLength,
                          UChar padChar)
{
  if(isBogus() || fLength >= targetLength || !cloneArrayIfNeeded(targetLength)) {
    return FALSE;
  } else {
    // move contents up by padding width
    int32_t start = targetLength - fLength;
    us_arrayCopy(fArray, 0, fArray, start, fLength);

    // fill in padding character
    while(--start >= 0) {
      fArray[start] = padChar;
    }
    fLength = targetLength;
    return TRUE;
  }
}

UBool 
UnicodeString::padTrailing(int32_t targetLength,
                           UChar padChar)
{
  if(isBogus() || fLength >= targetLength || !cloneArrayIfNeeded(targetLength)) {
    return FALSE;
  } else {
    // fill in padding character
    int32_t length = targetLength;
    while(--length >= fLength) {
      fArray[length] = padChar;
    }
    fLength = targetLength;
    return TRUE;
  }
}

UnicodeString& 
UnicodeString::trim()
{
  if(isBogus()) {
    return *this;
  }

  UChar32 c;
  UTextOffset i = fLength, length;

  // first cut off trailing white space
  for(;;) {
    length = i;
    if(i <= 0) {
      break;
    }
    UTF_PREV_CHAR(fArray, 0, i, c);
    if(!(c == 0x20 || Unicode::isWhitespace(c))) {
      break;
    }
  }
  if(length < fLength) {
    fLength = length;
  }

  // find leading white space
  UTextOffset start;
  i = 0;
  for(;;) {
    start = i;
    if(i >= length) {
      break;
    }
    UTF_NEXT_CHAR(fArray, i, length, c);
    if(!(c == 0x20 || Unicode::isWhitespace(c))) {
      break;
    }
  }

  // move string forward over leading white space
  if(start > 0) {
    doReplace(0, start, 0, 0, 0);
  }

  return *this;
}

//========================================
// Hashing
//========================================
int32_t
UnicodeString::doHashCode() const
{
    /* Delegate hash computation to uhash.  This makes UnicodeString
     * hashing consistent with UChar* hashing.  */
    int32_t hashCode = uhash_hashUCharsN(getArrayStart(), fLength);
    if (hashCode == kInvalidHashCode) {
        hashCode = kEmptyHashCode;
    }
    return hashCode;
}

//========================================
// Codeset conversion
//========================================
int32_t
UnicodeString::extract(UTextOffset start,
                       int32_t length,
                       char *target,
                       uint32_t dstSize,
                       const char *codepage) const
{
  // if we're bogus or there's nothing to convert, do nothing
  if(isBogus() || length <= 0) {
    return 0;
  }

  // pin the indices to legal values
  pinIndices(start, length);

  // create the converter
  UConverter *converter;
  UErrorCode status = U_ZERO_ERROR;

  // if the codepage is the default, use our cache
  // if it is an empty string, then use the "invariant character" conversion
  if (codepage == 0) {
    converter = getDefaultConverter(status);
  } else if (*codepage == 0) {
    // use the "invariant characters" conversion
    if (length > fLength - start) {
      length = fLength - start;
    }
    if (target != NULL) {
      if (length > dstSize) {
        length = dstSize;
      }
      u_UCharsToChars(getArrayStart() + start, target, length);
    }
    return length;
  } else {
    converter = ucnv_open(codepage, &status);
  }

  // if we failed, set the appropriate flags and return
  if (U_FAILURE(status)) {
    return 0;
  }

  // perform the conversion
  const UChar *mySource = getArrayStart() + start;
  const UChar *mySourceLimit = mySource + length;
  const char *myTargetLimit;
  int32_t size;

  if (target != NULL) {
    // Pin the limit to U_MAX_PTR if the "magic" dstSize is used.
    if(dstSize == 0xffffffff) {
      myTargetLimit = (char*)U_MAX_PTR(target);
    } else {
      myTargetLimit = target + dstSize;
    }

    char *myTarget = target;
    ucnv_fromUnicode(converter, &myTarget, myTargetLimit,
             &mySource, mySourceLimit, 0, TRUE, &status);
    size = (int32_t)(myTarget - target);
  } else {
    /* Find out the size of the target needed for the current codepage */
    char targetBuffer[1024];
    size = 0;

    myTargetLimit = targetBuffer + sizeof(targetBuffer);
    status = U_BUFFER_OVERFLOW_ERROR;
    while (mySource < mySourceLimit && status == U_BUFFER_OVERFLOW_ERROR) {
        target = targetBuffer;
        status = U_ZERO_ERROR;
        ucnv_fromUnicode(converter, &target, myTargetLimit,
                 &mySource, mySourceLimit, 0, TRUE, &status);
        size += (int32_t)(target - targetBuffer);
    }
    /* Use the close at the end of the function */
  }

  // close the converter
  if (codepage == 0) {
    releaseDefaultConverter(converter);
  } else {
    ucnv_close(converter);
  }

  return size;
}

void
UnicodeString::doCodepageCreate(const char *codepageData,
                int32_t dataLength,
                const char *codepage)
{
  // if there's nothing to convert, do nothing
  if(codepageData == 0 || dataLength <= 0) {
    return;
  }

  UErrorCode status = U_ZERO_ERROR;

  // create the converter
  // if the codepage is the default, use our cache
  // if it is an empty string, then use the "invariant character" conversion
  UConverter *converter = (codepage == 0 ?
                             getDefaultConverter(status) :
                             *codepage == 0 ?
                               0 :
                               ucnv_open(codepage, &status));

  // if we failed, set the appropriate flags and return
  if(U_FAILURE(status)) {
    setToBogus();
    return;
  }

  // perform the conversion
  if(converter == 0) {
    // use the "invariant characters" conversion
    if(cloneArrayIfNeeded(dataLength, dataLength, FALSE)) {
      u_charsToUChars(codepageData, getArrayStart(), dataLength);
      fLength = dataLength;
    } else {
      setToBogus();
    }
    return;
  }

  // set up the conversion parameters
  const char *mySource     = codepageData;
  const char *mySourceEnd  = mySource + dataLength;
  UChar *myTarget;

  // estimate the size needed:
  // 1.25 UChar's per source byte should cover most cases
  int32_t arraySize = dataLength + (dataLength >> 2);

  // we do not care about the current contents
  UBool doCopyArray = FALSE;
  for(;;) {
    if(!cloneArrayIfNeeded(arraySize, arraySize, doCopyArray)) {
      setToBogus();
      break;
    }

    // perform the conversion
    myTarget = fArray + fLength;
    ucnv_toUnicode(converter, &myTarget,  fArray + fCapacity,
           &mySource, mySourceEnd, 0, FALSE, &status);

    // update the conversion parameters
    fLength = myTarget - fArray;

    // allocate more space and copy data, if needed
    if(status == U_BUFFER_OVERFLOW_ERROR) {
      // reset the error code
      status = U_ZERO_ERROR;

      // keep the previous conversion results
      doCopyArray = TRUE;

      // estimate the new size needed, larger than before
      // try 2 UChar's per remaining source byte
      arraySize = fLength + 2 * (mySourceEnd - mySource);
    } else {
      break;
    }
  }

  // close the converter
  if(codepage == 0) {
    releaseDefaultConverter(converter);
  } else {
    ucnv_close(converter);
  }
}

//========================================
// External Buffer
//========================================
// ### TODO:
// this is very, very dirty: we should not ever expose our array to the outside,
// and this also violates the const-ness of this object
// this must be removed when the resource bundle implementation does not need it any more!
const UChar*
UnicodeString::getUChars() const {
  // if we're bogus, do nothing
  if(isBogus()) {
    return 0;
  }

  if(fCapacity <= fLength || fArray[fLength] != 0) {
    if(((UnicodeString &)*this).cloneArrayIfNeeded(fLength + 1)) {
      fArray[fLength] = 0;
    }
  }
  return fArray;
}

//========================================
// Miscellaneous
//========================================
UBool
UnicodeString::cloneArrayIfNeeded(int32_t newCapacity,
                                  int32_t growCapacity,
                                  UBool doCopyArray,
                                  int32_t **pBufferToDelete,
                                  UBool forceClone) {
  // default parameters need to be static, therefore
  // the defaults are -1 to have convenience defaults
  if(newCapacity == -1) {
    newCapacity = fCapacity;
  }

  /*
   * We need to make a copy of the array if
   * the buffer is read-only, or
   * the buffer is refCounted (shared), and refCount>1, or
   * the buffer is too small.
   * Return FALSE if memory could not be allocated.
   */
  if(forceClone ||
     fFlags & kBufferIsReadonly ||
     fFlags & kRefCounted && refCount() > 1 ||
     newCapacity > fCapacity
  ) {
    // save old values
    UChar *array = fArray;
    uint16_t flags = fFlags;

    // check growCapacity for default value and use of the stack buffer
    if(growCapacity == -1) {
      growCapacity = newCapacity;
    } else if(newCapacity <= US_STACKBUF_SIZE && growCapacity > US_STACKBUF_SIZE) {
      growCapacity = US_STACKBUF_SIZE;
    }

    // allocate a new array
    if(allocate(growCapacity) ||
       newCapacity < growCapacity && allocate(newCapacity)
    ) {
      if(doCopyArray) {
        // copy the contents
        // do not copy more than what fits - it may be smaller than before
        if(fCapacity < fLength) {
          fLength = fCapacity;
        }
        us_arrayCopy(array, 0, fArray, 0, fLength);
      } else {
        fLength = 0;
      }

      // release the old array
      if(flags & kRefCounted) {
        // the array is refCounted; decrement and release if 0
        int32_t *pRefCount = ((int32_t *)array - 1);
        if(--*pRefCount == 0) {
          if(pBufferToDelete == 0) {
            delete [] pRefCount;
          } else {
            // the caller requested to delete it himself
            *pBufferToDelete = pRefCount;
          }
        }
      }
    } else {
      // not enough memory for growCapacity and not even for the smaller newCapacity
      // reset the old values for setToBogus() to release the array
      fArray = array;
      fFlags = flags;
      setToBogus();
      return FALSE;
    }
  }
  return TRUE;
}

// private function for C API
U_CFUNC int32_t
T_UnicodeString_length(const UnicodeString *s)
{
  return s->length();
}

// private function for C API
U_CFUNC int32_t
T_UnicodeString_extract(const UnicodeString *s, char *dst)
{
  return s->extract(0, s->length(), dst, "");
}


//========================================
// Default converter caching
//========================================

UConverter*
UnicodeString::getDefaultConverter(UErrorCode &status)
{
  UConverter *converter = 0;

  if(fgDefaultConverter != 0) {
    Mutex lock;

    // need to check to make sure it wasn't taken out from under us
    if(fgDefaultConverter != 0) {
      converter = fgDefaultConverter;
      fgDefaultConverter = 0;
    }
  }

  // if the cache was empty, create a converter
  if(converter == 0) {
    converter = ucnv_open(0, &status);
    if(U_FAILURE(status)) {
      return 0;
    }
  }

  return converter;
}

void
UnicodeString::releaseDefaultConverter(UConverter *converter)
{
  if(fgDefaultConverter == 0) {
    if (converter != 0) {
      ucnv_reset(converter);
    }

    Mutex lock;

    if(fgDefaultConverter == 0) {
      fgDefaultConverter = converter;
      converter = 0;
    }
  }

  // it's safe to close a 0 converter
  ucnv_close(converter);
}

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
  int32_t x = 0;
  uint16_t byte;

  byte = T_FileStream_getc(is);
  x |= byte;
  byte = T_FileStream_getc(is);
  x = (x << 8) | byte;
  byte = T_FileStream_getc(is);
  x = (x << 8) | byte;
  byte = T_FileStream_getc(is);
  x = (x << 8) | byte;

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
  UChar c = 0;
  uint16_t byte;

  byte = T_FileStream_getc(is);
  c |= byte;
  byte = T_FileStream_getc(is);
  c = (c << 8) | byte;

  return c;
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
    uprv_mstrm_write(os, (uint8_t*)&s->fLength, sizeof(s->fLength));
  }

  const UChar *c   = s->getArrayStart();
  const UChar *end = c + s->fLength;

  while(c != end && ! uprv_mstrm_error(os)) {
    uprv_mstrm_write(os, (uint8_t*)c, sizeof(*c));
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
  uprv_mstrm_read(is, (uint8_t *)&newSize, sizeof(int32_t));
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
    uprv_mstrm_read(is, (uint8_t *)c, sizeof(*c));
    c++;
  }

  // couldn't read all chars
  if(c < end) {
    s->setToBogus();
    return;
  }

  s->fLength = newSize;
}

// console IO

#if U_IOSTREAM_SOURCE >= 198506

#if U_IOSTREAM_SOURCE >= 199711

U_COMMON_API std::ostream &
operator<<(std::ostream& stream, const UnicodeString& s)

#else

U_COMMON_API ostream &
operator<<(ostream& stream, const UnicodeString& s)

#endif

{
  if(s.length() > 0) {
    char buffer[200];
    UConverter *converter;
    UErrorCode errorCode = U_ZERO_ERROR;

    // use the default converter to convert chunks of text
    converter = UnicodeString::getDefaultConverter(errorCode);
    if(U_SUCCESS(errorCode)) {
      const UChar *us = s.getArrayStart(), *uLimit = us + s.length();
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
      UnicodeString::releaseDefaultConverter(converter);
    }
  }

  stream.flush();
  return stream;
}

#endif
