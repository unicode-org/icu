/*
*******************************************************************************
*
*   Copyright (C) 2008, International Business Machines
*   Corporation, Google and others.  All Rights Reserved.
*
*******************************************************************************
*/
// Author : eldawy@google.com (Mohamed Eldawy)
// ucnvsel.cpp
//
// Purpose: To generate a list of encodings capable of handling
// a given Unicode text
//
// Started 09-April-2008

/**
 * \file
 *
 * This is an implementation of an encoding selector.
 * The goal is, given a unicode string, find the encodings
 * this string can be mapped to. To make processing faster
 * a trie is built when you call ucnvsel_open() that
 * stores all encodings a codepoint can map to
 */

#include "unicode/ucnvsel.h"

#include <string.h>

#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"
#include "unicode/uchriter.h"
#include "utrie2.h"
#include "propsvec.h"
#include "uenumimp.h"
#include "cmemory.h"
#include "cstring.h"


U_NAMESPACE_USE

// maximum possible serialized trie that can ever be reached
// this was obtained by attempting to serialize a trie for all fallback mapping
// and for all roundtrip mappings and then selecting the maximum
// this value actually adds around 30KB of unneeded extra space (the actual
// maximum space is around 220000).
// the reasoning is to make it still work if lots of other converters were
// added to ICU
#define CAPACITY 250000


struct UConverterSelector {
  UTrie2 *trie;              // 16 bit trie containing offsets into pv
  uint32_t* pv;              // table of bits!
  int32_t pvCount;
  char** encodings;          // which encodings did user ask to use?
  int32_t encodingsCount;
};


/* internal function */
static void generateSelectorData(UConverterSelector* result,
                                 const USet* excludedCodePoints,
                                 const UConverterUnicodeSet whichSet,
                                 UErrorCode* status);


U_CAPI int32_t ucnvsel_swap(const UDataSwapper *ds,
                                 const void *inData,
                                 int32_t length,
                                 void *outData,
                                 UErrorCode *status);


/* open a selector. If converterList is NULL, build for all converters.
   If excludedCodePoints is NULL, don't exclude any codepoints */
U_CAPI UConverterSelector* ucnvsel_open(const char* const*  converterList,
                                      int32_t converterListSize,
                                      const USet* excludedCodePoints,
                                      const UConverterUnicodeSet whichSet,
                                      UErrorCode* status ) {
  // allocate a new converter
  UConverterSelector* newSelector;
  int32_t i;  // for loop counter

  // the compiler should realize the tail recursion here and optimize 
  // accordingly. This call is to get around the constness of
  // converterList by smallest amount of code modification
  if(converterListSize == 0 && converterList != NULL) {
    return ucnvsel_open(NULL, 0, excludedCodePoints, whichSet, status);
  }

  // check if already failed
  if (U_FAILURE(*status)) {
    return NULL;
  }
  // ensure args make sense!
  if (converterListSize < 0 || (converterList == NULL && converterListSize != 0)) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }



  newSelector = (UConverterSelector*)uprv_malloc(sizeof(UConverterSelector));
  if (!newSelector) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  uprv_memset(newSelector, 0, sizeof(UConverterSelector));

  // make a backup copy of the list of converters
  if (converterList != NULL && converterListSize > 0) {
    // TODO: reduce code duplication, combine the explicit-list and all-converters handling
    newSelector->encodings =
      (char**)uprv_malloc(converterListSize*sizeof(char*));
    // out of memory. Give user back the 100 bytes or so
    // we allocated earlier, and wish them good luck ;)
    if (!newSelector->encodings) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      uprv_free(newSelector);
      return NULL;
    }

    char* allStrings = NULL;
    int32_t totalSize = 0;
    for (i = 0 ; i < converterListSize ; i++) {
      totalSize += uprv_strlen(converterList[i])+1;
    }
    // 4-align the totalSize to 4-align the trie in the serialized form
    int32_t encodingStrPadding = totalSize & 3;
    if (encodingStrPadding != 0) {
      encodingStrPadding = 4 - encodingStrPadding;
    }
    totalSize += encodingStrPadding;
    allStrings = (char*) uprv_malloc(totalSize);
    //out of memory :(
    if (!allStrings) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      uprv_free(newSelector->encodings);
      uprv_free(newSelector);
      return NULL;
    }

    for (i = 0 ; i < converterListSize ; i++) {
      newSelector->encodings[i] = allStrings;
      uprv_strcpy(newSelector->encodings[i], converterList[i]);
      allStrings += uprv_strlen(newSelector->encodings[i]) + 1;  // calling strlen
        // twice per string is probably faster than allocating memory to
        // cache the lengths!
    }
    while (encodingStrPadding > 0) {
      *allStrings++ = (char)0xff;
      --encodingStrPadding;
    }
  } else {
    int32_t count = ucnv_countAvailable();
    newSelector->encodings =
      (char**)uprv_malloc(ucnv_countAvailable()*sizeof(char*));
    // out of memory. Give user back the 100 bytes or so
    // we allocated earlier, and wish them good luck ;)
    if (!newSelector->encodings) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      uprv_free(newSelector);
      return NULL;
    }
    char* allStrings = NULL;
    int32_t totalSize = 0;
    for (i = 0 ; i < count ; i++) {
      const char* conv_moniker = ucnv_getAvailableName(i);
      totalSize += uprv_strlen(conv_moniker)+1;
    }
    // 4-align the totalSize to 4-align the trie in the serialized form
    int32_t encodingStrPadding = totalSize & 3;
    if (encodingStrPadding != 0) {
      encodingStrPadding = 4 - encodingStrPadding;
    }
    totalSize += encodingStrPadding;
    allStrings = (char*) uprv_malloc(totalSize);
    //out of memory :(
    if (!allStrings) {
      *status = U_MEMORY_ALLOCATION_ERROR;
      uprv_free(newSelector->encodings);
      uprv_free(newSelector);
      return NULL;
    }
    for (i = 0 ; i < count ; i++) {
      const char* conv_moniker = ucnv_getAvailableName(i);
      newSelector->encodings[i] = allStrings;
      uprv_strcpy(newSelector->encodings[i], conv_moniker);
      allStrings += uprv_strlen(conv_moniker) + 1;  // calling strlen twice per
        // string is probably faster than allocating memory to cache the
        // lengths!
    }
    while (encodingStrPadding > 0) {
      *allStrings++ = (char)0xff;
      --encodingStrPadding;
    }
    converterListSize = ucnv_countAvailable();
  }

  newSelector->encodingsCount = converterListSize;
  generateSelectorData(newSelector, excludedCodePoints, whichSet, status);

  if (U_FAILURE(*status)) {
    // at this point, we know pv and encodings have been allocated. No harm in
    // calling ucnv_closeSelector()
    ucnvsel_close(newSelector);
    return NULL;
  }

  return newSelector;
}


/* close opened selector */
U_CAPI void ucnvsel_close(UConverterSelector *sel) {
  if (!sel) {
    return;
  }
  uprv_free(sel->encodings[0]);
  uprv_free(sel->encodings);
  upvec_close(sel->pv);
  utrie2_close(sel->trie);
  uprv_free(sel);
}

/* unserialize a selector */
U_CAPI UConverterSelector* ucnvsel_unserialize(const char* buffer,
                                             int32_t length,
                                             UErrorCode* status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return NULL;
  }
  // ensure args make sense!
  if (buffer == NULL || length <= 0) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }

  // TODO: check and document data alignment as usual
  // TODO: require that the input memory remains valid while this selector
  //       is in use, and don't copy the memory?
  // TODO: propose renaming to ucnvsel_openFromSerialized()
  UConverterSelector* sel;
  int32_t i = 0;  // for the for loop
  // check length!
  if (length < sizeof(int32_t) * 3) {
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }
  uint32_t sig, ASCIIness;

  memcpy(&sig, buffer, sizeof(int32_t));
  buffer += sizeof(uint32_t);
  memcpy(&ASCIIness, buffer, sizeof(int32_t));
  buffer += sizeof(uint32_t);
  // at this point, we don't know what the endianness or Asciiness of
  // our system or data is. Detect everything!
  // notice that a little trick is used here to save work. We don't actually
  // detect endianness of the machine or of the data. We simply detect
  // if the 2 are reversed. If they are, we send flags to udata_openSwapper()
  // to indicate we need endian swapping. Those params are not REALLY
  // the machine and data endianness
  UBool dataEndianness = FALSE;
  //if endianness need to be reversed
  if (sig == 0x99887766) {
    dataEndianness = TRUE;
  } else if (sig != 0x66778899) {
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }

  int32_t dataASCIIness = ASCIIness;
  if(dataEndianness) {
    //need to convert ASCIIness before using it!
    dataASCIIness = ((char*)&ASCIIness)[3];
  }
  int32_t machineASCIIness = U_CHARSET_FAMILY;

  //now, we have everything!!
  if(dataEndianness ||
     dataASCIIness != machineASCIIness) {
    //construct a data swapper!
    UDataSwapper *ds;

    ds=udata_openSwapper(dataEndianness, dataASCIIness, FALSE, machineASCIIness, status);
    char* newBuffer = (char*)uprv_malloc(length);
    if(!newBuffer) {
      udata_closeSwapper(ds);
      *status = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
    //can we pass buffer twice to swap in place?
    ucnvsel_swap(ds, buffer, length, newBuffer, status);
    buffer = newBuffer;
    udata_closeSwapper(ds);
  }

  length -= 3 * sizeof(int32_t); //sig, Asciiness, and pvCount
  // end of check length!
  // TODO: fix screwed-up length counting and checking --
  //       at the end, the trie size should be exactly the remaining length

  sel = (UConverterSelector*)uprv_malloc(sizeof(UConverterSelector));
  //out of memory :(
  if (!sel) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  uprv_memset(sel, 0, sizeof(UConverterSelector));

  memcpy(&sel->pvCount, buffer, sizeof(int32_t));
  buffer+=sizeof(int32_t);

  // check length
  if (length < (int32_t)((sel->pvCount+1)*sizeof(uint32_t))) {
    uprv_free(sel);
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }
  length -= (sel->pvCount+1)*sizeof(uint32_t);
  // end of check length

  sel->pv = (uint32_t*)uprv_malloc(sel->pvCount*sizeof(uint32_t));
  if(!sel->pv) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(sel);
    return NULL;
  }

  memcpy(sel->pv, buffer, sel->pvCount*sizeof(uint32_t));
  buffer += sel->pvCount*sizeof(uint32_t);

  int32_t encodingsLength;
  memcpy(&encodingsLength, buffer, sizeof(int32_t));
  buffer += sizeof(int32_t);
  char* tempEncodings = (char*) uprv_malloc(encodingsLength);
  if(!tempEncodings) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(sel);
    uprv_free(sel->pv);
    return NULL;
  }

  memcpy(tempEncodings, buffer, encodingsLength);
  buffer += encodingsLength;
  // count how many strings are there!
  int32_t numStrings = 0;
  for (int32_t i = 0 ; i < encodingsLength; i++) {
    if (tempEncodings[i] == 0) {
      numStrings++;
    }
  }
  sel->encodingsCount = numStrings;
  sel->encodings = (char**) uprv_malloc(numStrings * sizeof(char*));
  if(!sel->encodings) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(sel);
    uprv_free(sel->pv);
    uprv_free(tempEncodings);
    return NULL;
  }

  sel->encodings[0] = tempEncodings;
  int32_t curString = 1;
  for (i = 0; curString < numStrings; i++) {
    if (tempEncodings[i] == 0) {
      sel->encodings[curString++] = tempEncodings+i+1;
    }
  }

  // check length
  if (length < sizeof(uint32_t)) {
    uprv_free(sel->pv);
    uprv_free(tempEncodings);
    uprv_free(sel->encodings);
    uprv_free(sel);
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }
  length -= sizeof(uint32_t);
  // end of check length

  // the trie
  int32_t serializedTrieSize;
  memcpy(&serializedTrieSize, buffer, sizeof(int32_t));
  buffer += sizeof(int32_t);

  // check length
  if (length < serializedTrieSize) {
    uprv_free(sel->pv);
    uprv_free(tempEncodings);
    uprv_free(sel->encodings);
    uprv_free(sel);
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }
  length -= sizeof(uint32_t);
  // end of check length

  // unserialize!
  sel->trie = utrie2_openFromSerialized(UTRIE2_16_VALUE_BITS,
                                        buffer, serializedTrieSize, NULL,
                                        status);
  if (U_FAILURE(*status)) {
    ucnvsel_close(sel);
    sel = NULL;
  }
  return sel;
}

/* serialize a selector */
U_CAPI int32_t ucnvsel_serialize(const UConverterSelector* sel,
                               char* buffer,
                               int32_t bufferCapacity,
                               UErrorCode* status) {
  // compute size and make sure it fits
  int32_t totalSize;
  int32_t encodingStrLength = 0;

  // check if already failed
  if (U_FAILURE(*status)) {
    return 0;
  }
  // ensure args make sense!
  if (sel == NULL || bufferCapacity < 0) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }
  // TODO: call utrie2_swap()?
  // TODO: rethink this serialization; use UDataHeader?! require alignment as usual
  UErrorCode localStatus = U_ZERO_ERROR;
  int32_t serializedTrieSize = utrie2_serialize(sel->trie, NULL, 0, &localStatus);
  totalSize = sizeof(uint32_t) /*signature*/+sizeof(uint32_t) /*ASCIIness*/+
    sizeof(uint32_t)*sel->pvCount /*pv*/+ sizeof(uint32_t) /*pvCount*/+
    sizeof(uint32_t) /*serializedTrieSize*/+ serializedTrieSize /*trie*/;

  // this is a multi-string! strlen() will stop at the first one
  encodingStrLength =
    uprv_strlen(sel->encodings[sel->encodingsCount-1]) + 1 +
    (sel->encodings[sel->encodingsCount-1] - sel->encodings[0]);
  encodingStrLength = (encodingStrLength + 3) & ~3;  // round up to 4-alignment
  totalSize += encodingStrLength + sizeof(uint32_t);

  if (totalSize > bufferCapacity) {
    *status = U_BUFFER_OVERFLOW_ERROR;
    return totalSize;
  }
  // ok, save!
  // 0a. the signature
  uint32_t sig = 0x66778899;
  memcpy(buffer, &sig, sizeof(uint32_t));
  buffer+=sizeof(uint32_t);
  // 0b. ASCIIness
  uint32_t ASCIIness = U_CHARSET_FAMILY;
  memcpy(buffer, &ASCIIness, sizeof(uint32_t));
  buffer+=sizeof(uint32_t);

  // 1. the array
  memcpy(buffer, &sel->pvCount, sizeof(int32_t));
  buffer+=sizeof(int32_t);
  memcpy(buffer, sel->pv, sel->pvCount*sizeof(int32_t));
  buffer+=sel->pvCount*sizeof(int32_t);
  memcpy(buffer, &encodingStrLength, sizeof(int32_t));
  buffer+=sizeof(int32_t);
  memcpy(buffer, sel->encodings[0], encodingStrLength);
  buffer += encodingStrLength;

  // the trie
  memcpy(buffer, &serializedTrieSize, sizeof(uint32_t));
  buffer+=sizeof(uint32_t);
  utrie2_serialize(sel->trie, buffer, serializedTrieSize, status);
  return totalSize;
}

/* internal function! */
static void generateSelectorData(UConverterSelector* result,
                                 const USet* excludedCodePoints,
                                 const UConverterUnicodeSet   whichSet,
                                 UErrorCode* status) {
  int32_t columns = (result->encodingsCount+31)/32;

  // 66000 as suggested by Markus [I suggest something like 66000 which
  // exceeds the number of BMP code points. There will be fewer ranges of
  // combinations of encodings. (I believe there are no encodings that have
  // interesting mappings for supplementary code points. All encodings either
  // support all of them or none of them.)]
  result->pv = upvec_open(columns, 66000);  // create for all
     // unicode codepoints, and have space for all those bits needed!
  // set errorValue to all-ones
  for (int32_t col = 0 ; col < columns; col++) {
    upvec_setValue(result->pv, UPVEC_ERROR_VALUE_CP, UPVEC_ERROR_VALUE_CP,
                   col, ~0, ~0, status);
  }

  for (int32_t i = 0; i < result->encodingsCount; ++i) {
    uint32_t mask;
    uint32_t column;
    int32_t item_count;
    int32_t j;
    UConverter* test_converter = ucnv_open(result->encodings[i], status);
    if (U_FAILURE(*status)) {
      // status will propagate back to user
      return;
    }
    USet* unicode_point_set;
    unicode_point_set = uset_open(1, 0);  // empty set

    ucnv_getUnicodeSet(test_converter, unicode_point_set,
                       whichSet, status);

    column = i / 32;
    mask = 1 << (i%32);
    // now iterate over intervals on set i!
    item_count = uset_getItemCount(unicode_point_set);

    for (j = 0; j < item_count; ++j) {
      UChar32 start_char;
      UChar32 end_char;
      UErrorCode smallStatus = U_ZERO_ERROR;
      uset_getItem(unicode_point_set, j, &start_char, &end_char, NULL, 0,
                   &smallStatus);
      if (U_FAILURE(smallStatus)) {
        // this will be reached for the converters that fill the set with
        // strings. Those should be ignored by our system
      } else {
        upvec_setValue(result->pv, start_char, end_char, column, ~0, mask,
                       status);
        if (U_FAILURE(*status)) {
           return;
        }
      }
    }
    ucnv_close(test_converter);
    uset_close(unicode_point_set);
  }


  // handle excluded encodings! Simply set their values to all 1's in the upvec
  if (excludedCodePoints) {
    int32_t item_count = uset_getItemCount(excludedCodePoints);
    for (int32_t j = 0; j < item_count; ++j) {
      UChar32 start_char;
      UChar32 end_char;

      uset_getItem(excludedCodePoints, j, &start_char, &end_char, NULL, 0,
                   status);
      if (U_FAILURE(*status)) {
        return;
      } else {
        for (int32_t col = 0 ; col < columns; col++) {
          upvec_setValue(result->pv, start_char, end_char, col, ~0, ~0,
                        status);
        }
      }
    }
  }

  // alright. Now, let's put things in the same exact form you'd get when you
  // unserialize things.
  UPVecToUTrie2Context toUTrie2={ NULL };
  result->pvCount = upvec_compact(result->pv, upvec_compactToUTrie2Handler,
                                  &toUTrie2, status);
  if (U_SUCCESS(*status)) {
    result->trie = toUTrie2.trie;
    utrie2_freeze(result->trie, UTRIE2_16_VALUE_BITS, status);
  }
}



// a bunch of functions for the enumeration thingie! Nothing fancy here. Just
// iterate over the selected encodings
struct Enumerator {
  int16_t* index;
  int16_t length;
  int16_t cur;
  const UConverterSelector* sel;
};

U_CDECL_BEGIN

static void U_CALLCONV
ucnvsel_close_selector_iterator(UEnumeration *enumerator) {
  uprv_free(((Enumerator*)(enumerator->context))->index);
  uprv_free(enumerator->context);
  uprv_free(enumerator);
}


static int32_t U_CALLCONV
ucnvsel_count_encodings(UEnumeration *enumerator, UErrorCode *status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return 0;
  }
  return ((Enumerator*)(enumerator->context))->length;
}


static const char* U_CALLCONV ucnvsel_next_encoding(UEnumeration* enumerator,
                                                 int32_t* resultLength,
                                                 UErrorCode* status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return NULL;
  }

  int16_t cur = ((Enumerator*)(enumerator->context))->cur;
  const UConverterSelector* sel;
  const char* result;
  if (cur >= ((Enumerator*)(enumerator->context))->length) {
    return NULL;
  }
  sel = ((Enumerator*)(enumerator->context))->sel;
  result = sel->encodings[((Enumerator*)(enumerator->context))->index[cur] ];
  ((Enumerator*)(enumerator->context))->cur++;
  if (resultLength) {
    *resultLength = uprv_strlen(result);
  }
  return result;
}

static void U_CALLCONV ucnvsel_reset_iterator(UEnumeration* enumerator,
                                           UErrorCode* status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return ;
  }
  ((Enumerator*)(enumerator->context))->cur = 0;
}

U_CDECL_END


static const UEnumeration defaultEncodings = {
  NULL,
    NULL,
    ucnvsel_close_selector_iterator,
    ucnvsel_count_encodings,
    uenum_unextDefault,
    ucnvsel_next_encoding, 
    ucnvsel_reset_iterator
};


// internal fn to intersect two sets of masks
// returns whether the mask has reduced to all zeros
UBool intersectMasks(uint32_t* dest, const uint32_t* source1, int32_t len) {
  int32_t i;
  uint32_t oredDest = 0;
  for (i = 0 ; i < len ; ++i) {
    oredDest |= (dest[i] &= source1[i]);
  }
  return oredDest == 0;
}

// internal fn to count how many 1's are there in a mask
// algorithm taken from  http://graphics.stanford.edu/~seander/bithacks.html
int16_t countOnes(uint32_t* mask, int32_t len) {
  int32_t i, totalOnes = 0;
  for (i = 0 ; i < len ; ++i) {
    uint32_t ent = mask[i];
    for (; ent; totalOnes++)
    {
      ent &= ent - 1; // clear the least significant bit set
    }
  }
  return totalOnes;
}


/* internal function! */
static UEnumeration *selectForMask(const UConverterSelector* sel,
                                   uint32_t *mask, UErrorCode *status) {
  // this is the context we will use. Store a table of indices to which
  // encodings are legit.
  struct Enumerator* result = (Enumerator*)uprv_malloc(sizeof(Enumerator));
  if (result == NULL) {
    uprv_free(mask);
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  result->index = NULL;  // this will be allocated later!
  result->length = result->cur = 0;
  result->sel = sel;

  UEnumeration *en = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
  if (en == NULL) {
    // TODO(markus): Combine Enumerator and UEnumeration into one struct.
    uprv_free(mask);
    uprv_free(result);
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  memcpy(en, &defaultEncodings, sizeof(UEnumeration));
  en->context = result;

  int32_t columns = (sel->encodingsCount+31)/32;
  int16_t numOnes = countOnes(mask, columns);
  // now, we know the exact space we need for index
  if (numOnes > 0) {
    result->index = (int16_t*) uprv_malloc(numOnes * sizeof(int16_t));

    int32_t i, j;
    int16_t k = 0;
    for (j = 0 ; j < columns; j++) {
      uint32_t v = mask[j];
      for (i = 0 ; i < 32 && k < sel->encodingsCount; i++, k++) {
        if ((v & 1) != 0) {
          result->index[result->length++] = k;
        }
        v >>= 1;
      }
    }
  } //otherwise, index will remain NULL (and will never be touched by
    //the enumerator code anyway)
  uprv_free(mask);
  return en;
}

/* check a string against the selector - UTF16 version */
U_CAPI UEnumeration *ucnvsel_selectForString(const UConverterSelector* sel,
                                   const UChar *s,
                                   int32_t length,
                                   UErrorCode *status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return NULL;
  }
  // ensure args make sense!
  if (sel == NULL || (s == NULL && length != 0)) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }

  int32_t columns = (sel->encodingsCount+31)/32;
  uint32_t* mask = (uint32_t*) uprv_malloc(columns * 4);
  if (mask == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  uprv_memset(mask, ~0, columns *4);

  const UChar *limit;
  if (length >= 0) {
    limit = s + length;
  } else {
    limit = NULL;
  }

  while (limit == NULL ? *s != 0 : s != limit) {
    UChar32 c;
    uint16_t pvIndex;
    UTRIE2_U16_NEXT16(sel->trie, s, limit, c, pvIndex);
    if (intersectMasks(mask, sel->pv+pvIndex, columns)) {
      break;
    }
  }
  return selectForMask(sel, mask, status);
}

/* check a string against the selector - UTF8 version */
U_CAPI UEnumeration *ucnvsel_selectForUTF8(const UConverterSelector* sel,
                                 const char *s,
                                 int32_t length,
                                 UErrorCode *status) {
  // check if already failed
  if (U_FAILURE(*status)) {
    return NULL;
  }
  // ensure args make sense!
  if (sel == NULL || (s == NULL && length != 0)) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return NULL;
  }

  int32_t columns = (sel->encodingsCount+31)/32;
  uint32_t* mask = (uint32_t*) uprv_malloc(columns * 4);
  if (mask == NULL) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }
  uprv_memset(mask, ~0, columns *4);

  if (length < 0) {
    length = uprv_strlen(s);
  }
  const char *limit = s + length;

  while (s != limit) {
    uint16_t pvIndex;
    UTRIE2_U8_NEXT16(sel->trie, s, limit, pvIndex);
    if (intersectMasks(mask, sel->pv+pvIndex, columns)) {
      break;
    }
  }
  return selectForMask(sel, mask, status);
}




/**
 * swap a selector into the desired Endianness and Asciiness of
 * the system. Just as FYI, selectors are always saved in the format
 * of the system that created them. They are only converted if used
 * on another system. In other words, selectors created on different
 * system can be different even if the params are identical (endianness
 * and Asciiness differences only)
 *
 * @param ds pointer to data swapper containing swapping info
 * @param inData pointer to incoming data
 * @param length length of inData in bytes
 * @param outData pointer to output data. Capacity should
 *                be at least equal to capacity of inData
 * @param status an in/out ICU UErrorCode
 * @return 0 on failure, number of bytes swapped on success
 *         number of bytes swapped can be smaller than length
 *
 */
U_CAPI int32_t ucnvsel_swap(const UDataSwapper *ds,
                                 const void *inData,
                                 int32_t length,
                                 void *outData,
                                 UErrorCode *status) {
  const char* inDataC = (const char*) inData;
  char * outDataC = (char*) outData;
  int32_t passedLength = length;
  //args check
  if(U_FAILURE(*status)) {
    return 0;
  }
  if(ds==NULL || inData==NULL || length<-1 || (length>0 && outData==NULL)) {
    *status=U_ILLEGAL_ARGUMENT_ERROR;
      return 0;
  }

  if(length < 3 * sizeof(uint32_t)) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }

  ds->swapArray32(ds, inDataC, 3, outDataC, status);
  int32_t pvCount = ((int32_t*)outData)[2];

  if(((int32_t*)outData)[0] != 0x66778899)
    return 0;

  length -= 3 * sizeof(uint32_t);
  inDataC += 3 * sizeof(uint32_t);
  outDataC += 3 * sizeof(uint32_t);


  if(length < (int32_t)(pvCount * sizeof(uint32_t))) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }
  ds->swapArray32(ds, inDataC, pvCount, outDataC, status);
  length -= pvCount * sizeof(uint32_t);
  inDataC += pvCount * sizeof(uint32_t);
  outDataC += pvCount * sizeof(uint32_t);

  if(length < 1 * sizeof(uint32_t)) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }
  ds->swapArray32(ds, inDataC, 1, outDataC, status);
  int32_t encodingStrLength = ((int32_t*)outData)[0];
  length -= sizeof(uint32_t);
  inDataC += sizeof(uint32_t);
  outDataC += sizeof(uint32_t);

  if(length < encodingStrLength) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }
  ds->swapInvChars(ds, inDataC, encodingStrLength, outDataC, status);
  length -= encodingStrLength;
  inDataC += encodingStrLength;
  outDataC += encodingStrLength;

  if(length <  1 * sizeof(uint32_t)) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }
  ds->swapArray32(ds, inDataC, 1, outDataC, status);
  int32_t trieSize = ((int32_t*)outData)[0];
  length -= sizeof(uint32_t);
  inDataC += sizeof(uint32_t);
  outDataC += sizeof(uint32_t);

  if(length <  trieSize) {
    * status = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0;
  }
  utrie2_swap(ds, inDataC, trieSize, outDataC, status);
  length -= trieSize;
  return passedLength - length;
}
