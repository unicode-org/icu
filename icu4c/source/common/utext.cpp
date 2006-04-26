/*
*******************************************************************************
*
*   Copyright (C) 2005-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utext.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005apr12
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/utext.h"
#include "ustr_imp.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"


#define I32_FLAG(bitIndex) ((int32_t)1<<(bitIndex))


static UBool
utext_access(UText *ut, int64_t index, UBool forward) {
    return ut->access(ut, index, forward);
}



U_DRAFT UBool U_EXPORT2
utext_moveIndex32(UText *ut, int32_t delta) {
    UChar32  c;
    if (delta > 0) {
        do {
            if(ut->chunkOffset>=ut->chunkLength && !utext_access(ut, ut->chunkNativeLimit, TRUE)) {
                return FALSE;
            }
            c = ut->chunkContents[ut->chunkOffset];
            if (U16_IS_SURROGATE(c)) {
                c = utext_next32(ut);
                if (c == U_SENTINEL) {
                    return FALSE;
                }
            } else {
                ut->chunkOffset++;
            }
        } while(--delta>0);

    } else if (delta<0) {
        do {
            if(ut->chunkOffset<=0 && !utext_access(ut, ut->chunkNativeStart, FALSE)) {
                return FALSE;
            }
            c = ut->chunkContents[ut->chunkOffset-1];
            if (U16_IS_SURROGATE(c)) {
                c = utext_previous32(ut);
                if (c == U_SENTINEL) {
                    return FALSE;
                }
            } else {
                ut->chunkOffset--;
            }
        } while(++delta<0);
    }   

    return TRUE;
}


U_DRAFT int64_t U_EXPORT2
utext_nativeLength(UText *ut) {
    return ut->nativeLength(ut);
}


U_DRAFT UBool U_EXPORT2
utext_isLengthExpensive(const UText *ut) {
    UBool r = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE)) != 0;
    return r;
}


U_DRAFT int64_t U_EXPORT2
utext_getNativeIndex(UText *ut) {
    if(!ut->nonUTF16Indexes || ut->chunkOffset==0) {
        return ut->chunkNativeStart+ut->chunkOffset;
    } else {
        return ut->mapOffsetToNative(ut, ut->chunkOffset);
    }
}



U_DRAFT void U_EXPORT2
utext_setNativeIndex(UText *ut, int64_t index) {
    if(index<ut->chunkNativeStart || index>=ut->chunkNativeLimit) {
        // The desired position is outside of the current chunk.  
        // Access the new position.  Assume a forward iteration from here,
        // which will also be optimimum for a single random access.
        // Reverse iterations may suffer slightly.
        ut->access(ut, index, TRUE);
    } else if(ut->nonUTF16Indexes) {
        ut->chunkOffset=ut->mapNativeIndexToUTF16(ut, index);
    } else {
        // utf-16 indexing.
        ut->chunkOffset=(int32_t)(index-ut->chunkNativeStart);
    }
    // The convention is that the index must always be on a code point boundary.
    // Adjust the index position if it is in the middle of a surrogate pair.
    if (ut->chunkOffset<ut->chunkLength) {  
        UChar c= ut->chunkContents[ut->chunkOffset];
        if (UTF16_IS_TRAIL(c)) {
            if (ut->chunkOffset==0) {
                ut->access(ut, ut->chunkNativeStart, FALSE);
            }
            if (ut->chunkOffset>0) {
                UChar lead = ut->chunkContents[ut->chunkOffset-1];
                if (UTF16_IS_LEAD(lead)) {
                    ut->chunkOffset--;
                }
            }
        }
    }
}



  
//
//  utext_current32.  Get the UChar32 at the current position.
//                    UText iteration position is always on a code point boundary,
//                    never on the trail half of a surrogate pair.
//
U_DRAFT UChar32 U_EXPORT2
utext_current32(UText *ut) {
    UChar32  c;
    if (ut->chunkOffset==ut->chunkLength) {
        // Current position is just off the end of the chunk.
        if (ut->access(ut, ut->chunkNativeLimit, TRUE) == FALSE) {
            // Off the end of the text.
            return U_SENTINEL;
        }
    }

    c = ut->chunkContents[ut->chunkOffset];
    if (U16_IS_LEAD(c) == FALSE) {
        // Normal, non-supplementary case.
        return c;
    }

    //
    //  Possible supplementary char.  
    //
    UChar32   trail = 0;
    UChar32   supplementaryC = c;
    if ((ut->chunkOffset+1) < ut->chunkLength) {
        // The trail surrogate is in the same chunk.
        trail = ut->chunkContents[ut->chunkOffset+1];
    } else {
        //  The trail surrogate is in a different chunk.
        //     Because we must maintain the iteration position, we need to switch forward
        //     into the new chunk, get the trail surrogate, then revert the chunk back to the
        //     original one. 
        //     An edge case to be careful of:  the entire text may end with an unpaired
        //        leading surrogate.  The attempt to access the trail will fail, but
        //        the original position before the unpaired lead still needs to be restored.
        int64_t  nativePosition = ut->chunkNativeLimit;
        int32_t  originalOffset = ut->chunkOffset;
        if (ut->access(ut, nativePosition, TRUE)) {
            trail = ut->chunkContents[ut->chunkOffset];
        }
        UBool r = ut->access(ut, nativePosition, FALSE);  // reverse iteration flag loads preceding chunk
        U_ASSERT(r==TRUE);
        ut->chunkOffset = originalOffset;
    }

    if (U16_IS_TRAIL(trail)) {
        supplementaryC = U16_GET_SUPPLEMENTARY(c, trail);
    }
    return supplementaryC;

}


U_DRAFT UChar32 U_EXPORT2
utext_char32At(UText *ut, int64_t nativeIndex) {
    UChar32 c = U_SENTINEL;

    // Fast path the common case.
    if (!ut->nonUTF16Indexes && nativeIndex>=ut->chunkNativeStart && nativeIndex<ut->chunkNativeLimit) {
        ut->chunkOffset = (int32_t)(nativeIndex - ut->chunkNativeStart);
        c = ut->chunkContents[ut->chunkOffset];
        if (U16_IS_SURROGATE(c) == FALSE) {
            return c;
        }
    }


    utext_setNativeIndex(ut, nativeIndex);
    if (nativeIndex>=ut->chunkNativeStart && ut->chunkOffset<ut->chunkLength) {
        c = ut->chunkContents[ut->chunkOffset];
        if (U16_IS_SURROGATE(c)) {
            // For surrogates, let current32() deal with the complications
            //    of supplementaries that may span chunk boundaries.
            c = utext_current32(ut);
        }
    }
    return c;
}


U_DRAFT UChar32 U_EXPORT2
utext_next32(UText *ut) {
    UChar32       c;

    if (ut->chunkOffset >= ut->chunkLength) {
        if (ut->access(ut, ut->chunkNativeLimit, TRUE) == FALSE) {
            return U_SENTINEL;
        }
    }
            
    c = ut->chunkContents[ut->chunkOffset++];
    if (U16_IS_LEAD(c) == FALSE) {
        // Normal case, not supplementary.
        //   (A trail surrogate seen here is just returned as is, as a surrogate value.
        //    It cannot be part of a pair.)
        return c;
    }

    if (ut->chunkOffset >= ut->chunkLength) {
        if (ut->access(ut, ut->chunkNativeLimit, TRUE) == FALSE) {
            // c is an unpaired lead surrogate at the end of the text.
            // return it as it is.
            return c;
        }
    }
    UChar32 trail = ut->chunkContents[ut->chunkOffset];
    if (U16_IS_TRAIL(trail) == FALSE) {
        // c was an unpaired lead surrogate, not at the end of the text.
        // return it as it is (unpaired).  Iteration position is on the
        // following character, possibly in the next chunk, where the
        //  trail surrogate would have been if it had existed.
        return c;
    }

    UChar32 supplementary = U16_GET_SUPPLEMENTARY(c, trail);
    ut->chunkOffset++;   // move iteration position over the trail surrogate.
    return supplementary;
    }


U_DRAFT UChar32 U_EXPORT2
utext_previous32(UText *ut) {
    UChar32       c;

    if (ut->chunkOffset <= 0) {
        if (ut->access(ut, ut->chunkNativeStart, FALSE) == FALSE) {
            return U_SENTINEL;
        }
    }
    ut->chunkOffset--;
    c = ut->chunkContents[ut->chunkOffset];
    if (U16_IS_TRAIL(c) == FALSE) {
        // Normal case, not supplementary.
        //   (A lead surrogate seen here is just returned as is, as a surrogate value.
        //    It cannot be part of a pair.)
        return c;
    }

    if (ut->chunkOffset <= 0) {
        if (ut->access(ut, ut->chunkNativeStart, FALSE) == FALSE) {
            // c is an unpaired trail surrogate at the start of the text.
            // return it as it is.
            return c;
        }
    }

    UChar32 lead = ut->chunkContents[ut->chunkOffset-1];
    if (U16_IS_LEAD(lead) == FALSE) {
        // c was an unpaired trail surrogate, not at the end of the text.
        // return it as it is (unpaired).  Iteration position is at c
        return c;
    }

    UChar32 supplementary = U16_GET_SUPPLEMENTARY(lead, c);
    ut->chunkOffset--;   // move iteration position over the lead surrogate.
    return supplementary;
}



U_DRAFT UChar32 U_EXPORT2
utext_next32From(UText *ut, int64_t index) {
    UChar32       c      = U_SENTINEL;

    if(index<ut->chunkNativeStart || index>=ut->chunkNativeLimit) {
        if(!ut->access(ut, index, TRUE)) {
            // no chunk available here
            return U_SENTINEL;
        }
    } else if(ut->nonUTF16Indexes) {
        ut->chunkOffset = ut->mapNativeIndexToUTF16(ut, index);
    } else {
        ut->chunkOffset = (int32_t)(index - ut->chunkNativeStart);
    }

    c = ut->chunkContents[ut->chunkOffset++];
    if (U16_IS_SURROGATE(c)) {
        // Surrogates.  Many edge cases.  Use other functions that already
        //              deal with the problems.
        utext_setNativeIndex(ut, index);
        c = utext_next32(ut);  
    }
    return c;
}


U_DRAFT UChar32 U_EXPORT2
utext_previous32From(UText *ut, int64_t index) {
    //
    //  Return the character preceding the specified index.
    //  Leave the iteration position at the start of the character that was returned.
    //
    UChar32     cPrev;    // The character preceding cCurr, which is what we will return.

    // Address the chunk containg the position preceding the incoming index
    // A tricky edge case:
    //   We try to test the requested native index against the chunkNativeStart to determine
    //    whether the character preceding the one at the index is in the current chunk.
    //    BUT, this test can fail with UTF-8 (or any other multibyte encoding), when the
    //    requested index is on something other than the first position of the first char.
    //
    if(index<=ut->chunkNativeStart || index>ut->chunkNativeLimit) {
        // Requested native index is outside of the current chunk.
        if(!ut->access(ut, index, FALSE)) {
            // no chunk available here
            return U_SENTINEL; 
        }
    } else if(ut->nonUTF16Indexes) {
        ut->chunkOffset=ut->mapNativeIndexToUTF16(ut, index);
        if (ut->chunkOffset==0 && !ut->access(ut, index, FALSE)) {
            // no chunk available here
            return U_SENTINEL;
        }
    } else {
        // This chunk uses UTF-16 indexing.  Index into it.
        ut->chunkOffset = (int32_t)(index - ut->chunkNativeStart);
    }

    // 
    // Simple case with no surrogates.  
    //
    ut->chunkOffset--;
    cPrev = ut->chunkContents[ut->chunkOffset];

    if (U16_IS_SURROGATE(cPrev)) {
        // Possible supplementary.  Many edge cases.
        // Let other functions do the heavy lifting.
        utext_setNativeIndex(ut, index);
        cPrev = utext_previous32(ut);
    }
    return cPrev;
}


U_DRAFT int32_t U_EXPORT2
utext_extract(UText *ut,
             int64_t start, int64_t limit,
             UChar *dest, int32_t destCapacity,
             UErrorCode *status) {
                 return ut->extract(ut, start, limit, dest, destCapacity, status);
             }



U_DRAFT UBool U_EXPORT2
utext_equals(const UText *a, const UText *b) {
    if (a==NULL || b==NULL ||
        a->magic != UTEXT_MAGIC ||
        b->magic != UTEXT_MAGIC) {
            // Null or invalid arguments don't compare equal to anything.
            return FALSE;
        }

    if (a->access != b->access) {
        // Different types of text providers.
        return FALSE;
    }

    if (a->context != b->context) {
        return FALSE;
    }
    if (a->chunkNativeStart != b->chunkNativeStart ||
        a->chunkOffset != b->chunkOffset) {
            // The iteration position is different.
            //  TODO:  this test could fail to work correctly if the provider
            //         makes non-deterministic chunks.
            return FALSE;
    }
    return TRUE;
}

U_DRAFT UBool U_EXPORT2
utext_isWritable(const UText *ut)
{
    UBool b = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_WRITABLE)) != 0;
    return b;
}


U_DRAFT void U_EXPORT2
utext_freeze(UText *ut) {
    // Zero out the WRITABLE flag.
    ut->providerProperties &= ~(I32_FLAG(UTEXT_PROVIDER_WRITABLE));
}


U_DRAFT UBool U_EXPORT2
utext_hasMetaData(const UText *ut)
{
    UBool b = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_HAS_META_DATA)) != 0;
    return b;
}



U_DRAFT int32_t U_EXPORT2
utext_replace(UText *ut,
             int64_t nativeStart, int64_t nativeLimit,
             const UChar *replacementText, int32_t replacementLength,
             UErrorCode *status) 
{
    if (U_FAILURE(*status)) {
        return 0;
    }
    if ((ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_WRITABLE)) == 0) {
        *status = U_NO_WRITE_PERMISSION;
        return 0;
    }
    int32_t i = ut->replace(ut, nativeStart, nativeLimit, replacementText, replacementLength, status);
    return i;
}

U_DRAFT void U_EXPORT2
utext_copy(UText *ut,
          int64_t nativeStart, int64_t nativeLimit,
          int64_t destIndex,
          UBool move,
          UErrorCode *status)
{
    if (U_FAILURE(*status)) {
        return;
    }
    if ((ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_WRITABLE)) == 0) {
        *status = U_NO_WRITE_PERMISSION;
        return;
    }
    ut->copy(ut, nativeStart, nativeLimit, destIndex, move, status);
}



U_DRAFT UText * U_EXPORT2
utext_clone(UText *dest, const UText *src, UBool deep, UBool readOnly, UErrorCode *status) {
    UText *result;
    result = src->clone(dest, src, deep, status);
    if (readOnly) {
        utext_freeze(result);
    }
    return result;
}



//------------------------------------------------------------------------------
//
//   UText common functions implementation
//
//------------------------------------------------------------------------------

//
//  UText.flags bit definitions
//
enum {
    UTEXT_HEAP_ALLOCATED  = 1,      //  1 if ICU has allocated this UText struct on the heap.
                                    //  0 if caller provided storage for the UText.

    UTEXT_EXTRA_HEAP_ALLOCATED = 2, //  1 if ICU has allocated extra storage as a separate
                                    //     heap block.
                                    //  0 if there is no separate allocation.  Either no extra
                                    //     storage was requested, or it is appended to the end
                                    //     of the main UText storage.

    UTEXT_OPEN = 4                  //  1 if this UText is currently open
                                    //  0 if this UText is not open.
};


//
//  Extended form of a UText.  The purpose is to aid in computing the total size required
//    when a provider asks for a UText to be allocated with extra storage.

struct ExtendedUText {
    UText          ut;
    UAlignedMemory extension;
};

static const UText emptyText = UTEXT_INITIALIZER;

U_DRAFT UText * U_EXPORT2
utext_setup(UText *ut, int32_t extraSpace, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return ut;
    }

    if (ut == NULL) {
        // We need to heap-allocate storage for the new UText
        int32_t spaceRequired = sizeof(UText);
        if (extraSpace > 0) {
            spaceRequired = sizeof(ExtendedUText) + extraSpace - sizeof(UAlignedMemory);
        }
        ut = (UText *)uprv_malloc(spaceRequired);
        if (ut == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
        } else {
            *ut = emptyText;
            ut->flags |= UTEXT_HEAP_ALLOCATED;
            if (spaceRequired>0) {
                ut->extraSize = extraSpace;
                ut->pExtra    = &((ExtendedUText *)ut)->extension;
                uprv_memset(ut->pExtra, 0, extraSpace);  // Purify whines about copying untouched extra [buffer]
                                                         //  space when cloning, so init it now.
            }
        }
    } else {
        // We have been supplied with an already existing UText.
        // Verify that it really appears to be a UText.
        if (ut->magic != UTEXT_MAGIC) {
            *status = U_ILLEGAL_ARGUMENT_ERROR;
            return ut;
        }
        // If the ut is already open and there's a provider supplied close
        //   function, call it.
        if ((ut->flags & UTEXT_OPEN) && ut->close != NULL)  {
            ut->close(ut);
        }
        ut->flags &= ~UTEXT_OPEN;

        // If extra space was requested by our caller, check whether
        //   sufficient already exists, and allocate new if needed.
        if (extraSpace > ut->extraSize) {
            // Need more space.  If there is existing separately allocated space,
            //   delete it first, then allocate new space.
            if (ut->flags & UTEXT_EXTRA_HEAP_ALLOCATED) {
                uprv_free(ut->pExtra);
                ut->extraSize = 0;
            }
            ut->pExtra = uprv_malloc(extraSpace);
            if (ut->pExtra == NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
            } else {
                ut->extraSize = extraSpace;
                ut->flags |= UTEXT_EXTRA_HEAP_ALLOCATED;
                uprv_memset(ut->pExtra, 0, extraSpace);
            }
        }
    }
    if (U_SUCCESS(*status)) {
        ut->flags |= UTEXT_OPEN;

        // Initialize all fields of the UText from ut->context to the end.
        //
        int32_t*  pi = (int32_t *)&ut->context;
        int32_t*  pEnd = (int32_t *)(((char *)ut)+ sizeof(UText));
        do {
            *pi++ = 0;
        } while (pi<pEnd);

        /*
        ut->context = NULL;
        ut->p       = NULL;
        ut->q       = NULL;
        ut->r       = NULL;
        ut->chunkContents = NULL;
        ut->chunkOffset = 0;
        // TODO:  double check that all opens initialize adequately.
        //        Maybe zero all fields here for safety.
        */
    }
    return ut;
}


U_DRAFT UText * U_EXPORT2
utext_close(UText *ut) {
    if (ut==NULL ||
        ut->magic != UTEXT_MAGIC ||
        (ut->flags & UTEXT_OPEN) == 0)
    {
        // The supplied ut is not an open UText.
        // Do nothing.
        return ut;
    }

    // If the provider gave us a close function, call it now.
    // This will clean up anything allocated specifically by the provider.
    if (ut->close != NULL) {
        ut->close(ut);
    }
    ut->flags &= ~UTEXT_OPEN;

    // If we (the framework) allocated the UText or subsidiary storage,
    //   delete it.
    if (ut->flags & UTEXT_EXTRA_HEAP_ALLOCATED) {
        uprv_free(ut->pExtra);
        ut->pExtra = NULL;
        ut->flags &= ~UTEXT_EXTRA_HEAP_ALLOCATED;
        ut->extraSize = 0;
    }

    // Zero out fields of the closed UText.  This is a defensive move,
    //   inteded to cause applications that inadvertantly use a closed
    //   utext to crash with null pointer errors.
    ut->clone        = NULL;
    ut->nativeLength = NULL;
    ut->access       = NULL;
    ut->extract      = NULL;
    ut->replace      = NULL;
    ut->copy         = NULL;
    ut->close        = NULL;
    ut->chunkContents = NULL;

    if (ut->flags & UTEXT_HEAP_ALLOCATED) {
        // This UText was allocated by UText setup.  We need to free it.
        // Clear magic, so we can detect if the user messes up and immediately
        //  tries to reopen another UText using the deleted storage.
        ut->magic = 0;
        uprv_free(ut);
        ut = NULL;
    }
    return ut;
}



//
// resetChunk   When an access fails for attempting to get text that is out-of-range
//              this function puts the chunk into a benign state with the index at the
//              at the requested position.
//
//              If there is a pre-existing chunk that is adjacent to the index
//              preserve the chunk, otherwise set up a dummy zero length chunk.
//
static void
resetChunk(UText *ut, int64_t index) {
    if (index==ut->chunkNativeLimit) {
        ut->chunkOffset = ut->chunkLength;
    } else if (index==ut->chunkNativeStart) {
        ut->chunkOffset = 0;
    } else {
        ut->chunkLength      = 0;
        ut->chunkNativeStart = index;
        ut->chunkNativeLimit = index;
        ut->chunkOffset      = 0;
    } 
}


//
// invalidateChunk   Reset a chunk to have no contents, so that the next call
//                   to access will cause new data to load.
//                   This is needed when copy/move/replace operate directly on the
//                   backing text, potentially putting it out of sync with the
//                   contents in the chunk.
//
static void
invalidateChunk(UText *ut) {
    ut->chunkLength = 0;
    ut->chunkNativeLimit = 0;
    ut->chunkNativeStart = 0;
    ut->chunkOffset = 0;
}
        
//
// pinIndex        Do range pinning on a native index parameter.
//                 64 bit pinning is done in place.
//                 32 bit truncated result is returned as a convenience for
//                        use in providers that don't need 64 bits.
static int32_t 
pinIndex(int64_t &index, int64_t limit) {
    if (index<0) {
        index = 0;
    } else if (index > limit) {
        index = limit;
    }
    return (int32_t)index;
}


U_CDECL_BEGIN

//
// Pointer relocation function,
//   a utility used by shallow clone.
//   Adjust a pointer that refers to something within one UText (the source)
//   to refer to the same relative offset within a another UText (the target)
//
static void adjustPointer(UText *dest, const void **destPtr, const UText *src) {
    // convert all pointers to (char *) so that byte address arithmetic will work.
    char  *dptr = (char *)*destPtr;
    char  *dUText = (char *)dest;
    char  *sUText = (char *)src;

    if (dptr >= (char *)src->pExtra && dptr < ((char*)src->pExtra)+src->extraSize) {
        // target ptr was to something within the src UText's pExtra storage.
        //   relocate it into the target UText's pExtra region.
        *destPtr = ((char *)dest->pExtra) + (dptr - (char *)src->pExtra);    
    } else if (dptr>=sUText && dptr < sUText+src->sizeOfStruct) {
        // target ptr was pointing to somewhere within the source UText itself.
        //   Move it to the same offset within the target UText.
        *destPtr = dUText + (dptr-sUText);
    }
}


//
//  Clone.  This is a generic copy-the-utext-by-value clone function that can be
//          used as-is with some utext types, and as a helper by other clones. 
//
static UText * U_CALLCONV
shallowTextClone(UText * dest, const UText * src, UErrorCode * status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }
    int32_t  srcExtraSize = src->extraSize;

    //
    // Use the generic text_setup to allocate storage if required.
    //
    dest = utext_setup(dest, srcExtraSize, status);
    if (U_FAILURE(*status)) {
        return dest;
    }

    //
    //  flags (how the UText was allocated) and the pointer to the
    //   extra storage must retain the values in the cloned utext that
    //   were set up by utext_setup.  Save them separately before
    //   copying the whole struct.
    //
    void *destExtra = dest->pExtra;
    int32_t flags   = dest->flags;


    //
    //  Copy the whole UText struct by value.
    //  Any "Extra" storage is copied also.
    //
    int sizeToCopy = src->sizeOfStruct;
    if (sizeToCopy > dest->sizeOfStruct) {
        sizeToCopy = dest->sizeOfStruct;
    }
    uprv_memcpy(dest, src, sizeToCopy);
    dest->pExtra = destExtra;
    dest->flags  = flags;
    if (srcExtraSize > 0) {
        uprv_memcpy(dest->pExtra, src->pExtra, srcExtraSize);
    }

    //
    // Relocate any pointers in the target that refer to the UText itself.
    //   to point to the cloned copy rather than the original source.
    //
    adjustPointer(dest, &dest->context, src);
    adjustPointer(dest, &dest->p, src);
    adjustPointer(dest, &dest->q, src);
    adjustPointer(dest, &dest->r, src);
    adjustPointer(dest, &dest->r, src);
    adjustPointer(dest, &dest->r, src);
    adjustPointer(dest, &dest->r, src);
    adjustPointer(dest, &dest->r, src);


    return dest;
}


U_CDECL_END



//------------------------------------------------------------------------------
//
//     UText implementation for UTF-8 strings (read-only) 
//
//         Use of UText data members:
//              context    pointer to UTF-8 string
//              utext.b  is the input string length (bytes).
//              utext.p  pointer to allocated utf-8 string if owned by this utext (after a clone)
//              utext.q  pointer to the filled part of the Map array.
//
//      TODO:  make creation of the index mapping array lazy.
//             Create it for a chunk the first time the user asks for an index.
//
//------------------------------------------------------------------------------

enum { UTF8_TEXT_CHUNK_SIZE=10 };

struct UTF8Extra {
    /*
     * Chunk UChars.
     * +1 to simplify filling with surrogate pair at the end.
     */
    UChar s[UTF8_TEXT_CHUNK_SIZE+1];
    /*
     * Index map, from UTF-16 indexes into s back to native indexes.
     * +2: length of s[] + one more for chunk limit index.
     *
     * When accessing preceding text, chunk.contents may point into the middle
     * of s[].
     */
    int32_t map[UTF8_TEXT_CHUNK_SIZE+2];
};

//     because backwards iteration fills the buffers starting at the end and
//     working towards the front, the filled part of the buffers may not begin
//     at the start of the available storage for the buffers.

U_CDECL_BEGIN


static int64_t U_CALLCONV
utf8TextLength(UText *ut) {
    return ut->b;
}

        
   

static UBool U_CALLCONV
utf8TextAccess(UText *ut, int64_t index, UBool forward) {
    const uint8_t *s8=(const uint8_t *)ut->context;
    UChar32  c;
    int32_t  i;
    int32_t  length = ut->b;              // Length of original utf-8

    UTF8Extra  *ut8e   = (UTF8Extra *)ut->pExtra;
    UChar      *u16buf = ut8e->s;
    int32_t    *map    = ut8e->map;

    int32_t  index32 = pinIndex(index, length);

    if(forward) {
        if(index32 >= length) {
            resetChunk(ut, length);
            return FALSE;
        }

        c=s8[index32];
        if(c<=0x7f) {
            // get a run of ASCII characters.
            // Even if we don't fill the buffer, we will stop with the first
            //   non-ascii char, so that the buffer can use utf-16 indexing.
            ut->chunkNativeStart=index32;
            u16buf[0]=(UChar)c;
            for(i=1, ++index32;
                i<UTF8_TEXT_CHUNK_SIZE && index32<length && (c=s8[index32])<=0x7f;
                ++i, ++index32
            ) {
                u16buf[i]=(UChar)c;
            }
            ut->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            U8_SET_CP_START(s8, 0, index32);  // put utf-8 index at first byte of char, if not there already.
            ut->chunkNativeStart=index32;
            for(i=0;  i<UTF8_TEXT_CHUNK_SIZE && index32<length;  ) {
                //  i     is utf-16 index into chunk buffer.
                //  index is utf-8 index into original string
                map[i]=(int32_t)index32;
                map[i+1]=index32; // in case there is a trail surrogate
                U8_NEXT(s8, index32, length, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                U16_APPEND_UNSAFE(u16buf, i, c);    // post-increments i.
            }
            map[i]=index32;
            ut->nonUTF16Indexes=TRUE;
        }
        ut->chunkContents    = u16buf;
        ut->chunkLength      = i;
        ut->chunkNativeLimit = index32;
        ut->q                = map;  
        ut->chunkOffset      = 0;      // chunkOffset corresponding to index
        return TRUE; 
    } else {
        // Reverse Access.  The chunk buffer must be filled so as to contain the
        //                  character preceding the specified index.
        U8_SET_CP_START(s8, 0, index32);
        if(index32<=0) {
            resetChunk(ut, 0);
            return FALSE;
        }

        c=s8[index32-1];
        if(c<=0x7f) {
            // get a chunk of ASCII characters.  Don't build the index map
            ut->chunkNativeLimit=index32;
            i=UTF8_TEXT_CHUNK_SIZE;
            do {
                u16buf[--i]=(UChar)c;
                --index32;
            } while(i>0 && index32>0 && (c=s8[index32-1])<=0x7f);
            ut->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            if(index32<length) {
                U8_SET_CP_START(s8, 0, index32);
            }
            ut->chunkNativeLimit=index32;
            i=UTF8_TEXT_CHUNK_SIZE;
            map[i]=index32;    // map position for char following the last one in the buffer.
            do {
                //  i     is utf-16 index into chunk buffer.
                //  index is utf-8 index into original string
                U8_PREV(s8, 0, index32, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                if(c<=0xffff) {
                    u16buf[--i]=(UChar)c;
                    map[i]=index32;
                } else {
                    // We've got a supplementary char
                    if (i<2) {
                        // Both halves of the surrogate pair wont fit in the chunk buffer.
                        // Stop without putting either half in.
                        U8_NEXT(s8, index32, length, c);  // restore index.
                        break;
                    }
                    u16buf[--i]=U16_TRAIL(c);
                    map[i]=index32;
                    u16buf[--i]=U16_LEAD(c);
                    map[i]=index32;
                }
            } while(i>0 && index32>0);

            // Because we have filled the map & chunk buffers from back to front,
            //   the start position for accesses may not be at the start of the
            //   available storage.
            ut->q = map+i;
            ut->nonUTF16Indexes=TRUE;
        }
        // Common reverse iteration, for both UTF16 and non-UTIF16 indexes.
        ut->chunkContents    = u16buf+i;
        ut->chunkLength      = (UTF8_TEXT_CHUNK_SIZE)-i;
        ut->chunkNativeStart = index32;
        ut->chunkOffset      = ut->chunkLength; // chunkOffset corresponding to index
        return TRUE;
    }
}


//
//  This is a slightly modified copy of u_strFromUTF8,
//     Inserts a Replacement Char rather than failing on invalid UTF-8
//     Removes unnecessary features.
//
static UChar* 
utext_strFromUTF8(UChar *dest,             
              int32_t destCapacity,
              int32_t *pDestLength,
              const char* src, 
              int32_t srcLength,        // required.  NUL terminated not supported.
              UErrorCode *pErrorCode
              )
{

    UChar *pDest = dest;
    UChar *pDestLimit = dest+destCapacity;
    UChar32 ch=0;
    int32_t index = 0;
    int32_t reqLength = 0;
    uint8_t* pSrc = (uint8_t*) src;

         
    while((index < srcLength)&&(pDest<pDestLimit)){
        ch = pSrc[index++];
        if(ch <=0x7f){
            *pDest++=(UChar)ch;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                ch = 0xfffd;
            }
            if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else{
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }
        }
    }
    /* donot fill the dest buffer just count the UChars needed */
    while(index < srcLength){
        ch = pSrc[index++];
        if(ch <= 0x7f){
            reqLength++;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                ch = 0xfffd;
            }
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    reqLength+=(int32_t)(pDest - dest);

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode);

    return dest;
}



static int32_t U_CALLCONV
utf8TextExtract(UText *ut,
                int64_t start, int64_t limit,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    int32_t  length  = ut->b;
    int32_t  start32 = pinIndex(start, length);
    int32_t  limit32 = pinIndex(limit, length);

    if(start32>limit32) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }


    // adjust the incoming indexes to land on code point boundaries if needed.
    //    adjust by no more than three, because that is the largest number of trail bytes
    //    in a well formed UTF8 character.
    const uint8_t *buf = (const uint8_t *)ut->context;
    int i;
    if (start32 < ut->chunkNativeLimit) {
        for (i=0; i<3; i++) {
            if (U8_IS_LEAD(buf[start32]) || start32==0) {
                break;
            }
            start32--;
        }
    }

    if (limit32 < ut->chunkNativeLimit) {
        for (i=0; i<3; i++) {
            if (U8_IS_LEAD(buf[limit32]) || limit32==0) {
                break;
            }
            limit32--;
        }
    }

    // Do the actual extract.
    int32_t destLength=0;
    utext_strFromUTF8(dest, destCapacity, &destLength,
                    (const char *)ut->context+start32, limit32-start32,
                    pErrorCode);
    return destLength;
}

// Assume nonUTF16Indexes and 0<=offset<=ut->chunkLength
static int64_t U_CALLCONV
utf8TextMapOffsetToNative(UText *ut, int32_t offset) {
    // UText.q points to the index mapping array that is allocated in the extra storage area.
    U_ASSERT(offset>=0 && offset<=ut->chunkLength);
    int32_t *map=(int32_t *)(ut->q);
    return map[offset];
}

// Assume nonUTF16Indexes and ut->chunkstart<=index<=ut->chunklimit
static int32_t U_CALLCONV
utf8TextMapIndexToUTF16(UText *ut, int64_t index) {
    int32_t *map=(int32_t *)(ut->q);
    int32_t offset=0;

    U_ASSERT(index>=ut->chunkNativeStart && index<=ut->chunkNativeLimit);
    U_ASSERT(index <= UINT32_MAX);
    int32_t index32 = (int32_t) index;
    while(index32>map[offset]) {
        ++offset;
    }
    if (index32<map[offset]) {
        // index was to a trail byte of a multi-byte utf-8 char.
        // The loop above advanced offset to the start of the following char, now
        //  offset must be backed up to the start of the utf-16 char into which
        //  the utf-8 index pointed.
        offset--;
        if (offset>0 && map[offset] == map[offset-1]) {
            // index was to a utf-8 trail byte of a supplemenary char.
            //   Offset now points to the trail surrogate (one in back of the following char)
            //   Back offset up one more time to get to the utf-16 lead surrogate.
            offset--;
        }
    }
    return offset;
}

static UText * U_CALLCONV
utf8TextClone(UText *dest, const UText *src, UBool deep, UErrorCode *status) 
{
    // First do a generic shallow clone.  Does everything needed for the UText struct itself.
    dest = shallowTextClone(dest, src, status);

    // For deep clones, make a copy of the string.
    //  The copied storage is owned by the newly created clone.
    //  A non-NULL pointer in UText.p is the signal to the close() function to delete
    //    it.
    //
    if (deep && U_SUCCESS(*status)) {
        int32_t  len = src->b;
        char *copyStr = (char *)uprv_malloc(len+1);
        if (copyStr == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
        } else {
            uprv_memcpy(copyStr, src->context, len+1);
            dest->context = copyStr;
            dest->p       = copyStr;
        }
    }
    return dest;
}


static void U_CALLCONV
utf8TextClose(UText *ut) {
    // Most of the work of close is done by the generic UText framework close.
    // All that needs to be done here is to delete the UTF8 string if the UText
    //  owns it.  This occurs if the UText was created by cloning.
    char *s = (char *)ut->p;
    uprv_free(s);
    ut->p = NULL;
}




U_DRAFT UText * U_EXPORT2
utext_openUTF8(UText *ut, const char *s, int64_t length, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return NULL;
    }
    if(s==NULL || length<-1 || length>INT32_MAX) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    ut = utext_setup(ut, sizeof(UTF8Extra), status);
    if (U_FAILURE(*status)) {
        return ut;
    }
    ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_NON_UTF16_INDEXES);

    ut->clone         = utf8TextClone;
    ut->nativeLength  = utf8TextLength;
    ut->access        = utf8TextAccess;
    ut->extract       = utf8TextExtract;
    ut->mapOffsetToNative     = utf8TextMapOffsetToNative;
    ut->mapNativeIndexToUTF16 = utf8TextMapIndexToUTF16;
    ut->close         = utf8TextClose;

    ut->context=s;
    if(length>=0) {
        ut->b=(int32_t)length;
    } else {
        // TODO:  really undesirable to do this scan upfront.
        ut->b=(int32_t)uprv_strlen(s);
    }

    return ut;
}

U_CDECL_END






//------------------------------------------------------------------------------
//
//     UText implementation wrapper for Replaceable (read/write) 
//
//         Use of UText data members:
//            context    pointer to Replaceable.
//            p          pointer to Replaceable if it is owned by the UText.
//
//------------------------------------------------------------------------------



// minimum chunk size for this implementation: 3
// to allow for possible trimming for code point boundaries
enum { REP_TEXT_CHUNK_SIZE=10 };

struct ReplExtra {
    /*
     * Chunk UChars.
     * +1 to simplify filling with surrogate pair at the end.
     */
    UChar s[REP_TEXT_CHUNK_SIZE+1];
};


U_CDECL_BEGIN

static UText * U_CALLCONV
repTextClone(UText *dest, const UText *src, UBool deep, UErrorCode *status) {
    // First do a generic shallow clone.  Does everything needed for the UText struct itself.
    dest = shallowTextClone(dest, src, status);

    // For deep clones, make a copy of the Replaceable.
    //  The copied Replaceable storage is owned by the newly created UText clone.
    //  A non-NULL pointer in UText.p is the signal to the close() function to delete
    //    it.
    //
    if (deep && U_SUCCESS(*status)) {
        const Replaceable *replSrc = (const Replaceable *)src->context;
        dest->context = replSrc->clone();
        dest->p       = dest->context;
        // with deep clone, the copy is writable, even when the source is not.
        dest->providerProperties |= I32_FLAG(UTEXT_PROVIDER_WRITABLE);
    }
    return dest;
}


static void U_CALLCONV
repTextClose(UText *ut) {
    // Most of the work of close is done by the generic UText framework close.
    // All that needs to be done here is delete the Replaceable if the UText
    //  owns it.  This occurs if the UText was created by cloning.
    Replaceable *rep = (Replaceable *)ut->p;
    delete rep;
    ut->p = NULL;
}


static int64_t U_CALLCONV
repTextLength(UText *ut) {
    const Replaceable *replSrc = (const Replaceable *)ut->context;
    int32_t  len = replSrc->length();
    return len;
}


static UBool U_CALLCONV
repTextAccess(UText *ut, int64_t index, UBool forward) {
    const Replaceable *rep=(const Replaceable *)ut->context;
    int32_t length=rep->length();   // Full length of the input text (bigger than a chunk)

    // clip the requested index to the limits of the text.
    int32_t index32 = pinIndex(index, length);
    U_ASSERT(index<=INT32_MAX);

    // TODO:  check if requested location is in chunk already.


    /*
     * Compute start/limit boundaries around index, for a segment of text
     * to be extracted.
     * To allow for the possibility that our user gave an index to the trailing
     * half of a surrogate pair, we must request one extra preceding UChar when
     * going in the forward direction.  This will ensure that the buffer has the
     * entire code point at the specified index.
     */
    if(forward) {

        if (index32>=ut->chunkNativeStart && index32<ut->chunkNativeLimit) {
            // Buffer already contains the requested position.
            ut->chunkOffset = (int32_t)(index - ut->chunkNativeStart);
            return TRUE;
        }
        if (index32>=length && ut->chunkNativeLimit==length) {
            // Request for end of string, and buffer already extends up to it.
            // Can't get the data, but don't change the buffer.
            ut->chunkOffset = length - (int32_t)ut->chunkNativeStart;
            return FALSE;
        }

        ut->chunkNativeLimit = index + REP_TEXT_CHUNK_SIZE - 1;
        // Going forward, so we want to have the buffer with stuff at and beyond
        //   the requested index.  The -1 gets us one code point before the
        //   requested index also, to handle the case of the index being on
        //   a trail surrogate of a surrogate pair.
        if(ut->chunkNativeLimit > length) {
            ut->chunkNativeLimit = length;
        }
        // unless buffer ran off end, start is index-1.
        ut->chunkNativeStart = ut->chunkNativeLimit - REP_TEXT_CHUNK_SIZE;   
        if(ut->chunkNativeStart < 0) {
            ut->chunkNativeStart = 0;
        }
    } else {
        // Reverse iteration.  Fill buffer with data preceding the requested index.
        if (index32>ut->chunkNativeStart && index32<=ut->chunkNativeLimit) {
            // Requested position already in buffer.
            ut->chunkOffset = index32 - (int32_t)ut->chunkNativeStart;
            return TRUE;
        }
        if (index32==0 && ut->chunkNativeStart==0) {
            // Request for start, buffer already begins at start.
            //  No data, but keep the buffer as is.
            ut->chunkOffset = 0;
            return FALSE;
        }

        // Figure out the bounds of the chunk to extract for reverse iteration.
        // Need to worry about chunk not splitting surrogate pairs, and while still 
        // containing the data we need.
        // Fix by requesting a chunk that includes an extra UChar at the end.
        // If this turns out to be a lead surrogate, we can lop it off and still have
        //   the data we wanted.
        ut->chunkNativeStart = index32 + 1 - REP_TEXT_CHUNK_SIZE;
        if (ut->chunkNativeStart < 0) {
            ut->chunkNativeStart = 0;
        }

        ut->chunkNativeLimit = index32 + 1;
        if (ut->chunkNativeLimit > length) {
            ut->chunkNativeLimit = length;
        }
    }

    // Extract the new chunk of text from the Replaceable source.
    ReplExtra *ex = (ReplExtra *)ut->pExtra;
    // UnicodeString with its buffer a writable alias to the chunk buffer
    UnicodeString buffer(ex->s, 0 /*buffer length*/, REP_TEXT_CHUNK_SIZE /*buffer capacity*/); 
    rep->extractBetween((int32_t)ut->chunkNativeStart, (int32_t)ut->chunkNativeLimit, buffer);

    ut->chunkContents  = ex->s;
    ut->chunkLength    = (int32_t)(ut->chunkNativeLimit - ut->chunkNativeStart);
    ut->chunkOffset    = (int32_t)(index32 - ut->chunkNativeStart);

    // Surrogate pairs from the input text must not span chunk boundaries.
    // If end of chunk could be the start of a surrogate, trim it off.
    if (ut->chunkNativeLimit < length &&
        U16_IS_LEAD(ex->s[ut->chunkLength-1])) {
            ut->chunkLength--;
            ut->chunkNativeLimit--;
            if (ut->chunkOffset > ut->chunkLength) {
                ut->chunkOffset = ut->chunkLength;
            }
        }

    // if the first UChar in the chunk could be the trailing half of a surrogate pair,
    // trim it off.
    if(ut->chunkNativeStart>0 && U16_IS_TRAIL(ex->s[0])) {
        ++(ut->chunkContents);
        ++(ut->chunkNativeStart);
        --(ut->chunkLength);
        --(ut->chunkOffset);
    }

    // adjust the index/chunkOffset to a code point boundary
    U16_SET_CP_START(ut->chunkContents, 0, ut->chunkOffset);

    return TRUE; 
}



static int32_t U_CALLCONV
repTextExtract(UText *ut,
               int64_t start, int64_t limit,
               UChar *dest, int32_t destCapacity,
               UErrorCode *status) {
    const Replaceable *rep=(const Replaceable *)ut->context;
    int32_t  length=rep->length();

    if(U_FAILURE(*status)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start>limit) {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    int32_t  start32 = pinIndex(start, length);
    int32_t  limit32 = pinIndex(limit, length);

    // adjust start, limit if they point to trail half of surrogates
    if (start32<length && U16_IS_TRAIL(rep->charAt(start32)) &&
        U_IS_SUPPLEMENTARY(rep->char32At(start32))){
            start32--;
    }
    if (limit32<length && U16_IS_TRAIL(rep->charAt(limit32)) &&
        U_IS_SUPPLEMENTARY(rep->char32At(limit32))){
            limit32--;
    }

    length=limit32-start32;
    if(length>destCapacity) {
        limit32 = start32 + destCapacity;
    }
    UnicodeString buffer(dest, 0, destCapacity); // writable alias
    rep->extractBetween(start32, limit32, buffer);
    return u_terminateUChars(dest, destCapacity, length, status);
}

static int32_t U_CALLCONV
repTextReplace(UText *ut,
               int64_t start, int64_t limit,
               const UChar *src, int32_t length,
               UErrorCode *status) {
    Replaceable *rep=(Replaceable *)ut->context;
    int32_t oldLength;

    if(U_FAILURE(*status)) {
        return 0;
    }
    if(src==NULL && length!=0) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    oldLength=rep->length(); // will subtract from new length
    if(start>limit ) {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    int32_t start32 = pinIndex(start, oldLength);
    int32_t limit32 = pinIndex(limit, oldLength);

    // Snap start & limit to code point boundaries.
    if (start32<oldLength && U16_IS_TRAIL(rep->charAt(start32)) &&
        start32>0 && U16_IS_LEAD(rep->charAt(start32-1))) 
    {
            start32--;
    }
    if (limit32<oldLength && U16_IS_LEAD(rep->charAt(limit32-1)) &&
        U16_IS_TRAIL(rep->charAt(limit32))) 
    {
            limit32++;
    }

    // Do the actual replace operation using methods of the Replaceable class
    UnicodeString replStr((UBool)(length<0), src, length); // read-only alias
    rep->handleReplaceBetween(start32, limit32, replStr);
    int32_t newLength = rep->length();
    int32_t lengthDelta = newLength - oldLength;

    // Is the UText chunk buffer OK?
    if (ut->chunkNativeLimit > start32) {
        // this replace operation may have impacted the current chunk.
        // invalidate it, which will force a reload on the next access.
        invalidateChunk(ut);
    }

    // set the iteration position to the end of the newly inserted replacement text.
    int32_t newIndexPos = limit32 + lengthDelta;
    repTextAccess(ut, newIndexPos, TRUE);

    return lengthDelta;
}


static void U_CALLCONV
repTextCopy(UText *ut,
                int64_t start, int64_t limit,
                int64_t destIndex,
                UBool move,
                UErrorCode *status) 
{
    Replaceable *rep=(Replaceable *)ut->context;
    int32_t length=rep->length();

    if(U_FAILURE(*status)) {
        return;
    }
    if (start>limit || (start<destIndex && destIndex<limit)) 
    {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }

    int32_t start32     = pinIndex(start, length);
    int32_t limit32     = pinIndex(limit, length);
    int32_t destIndex32 = pinIndex(destIndex, length);

    // TODO:  snap everything to code point boundaries.

    if(move) {
        // move: copy to destIndex, then replace original with nothing
        int32_t segLength=limit32-start32;
        rep->copy(start32, limit32, destIndex32);
        if(destIndex32<start32) {
            start32+=segLength;
            limit32+=segLength;
        }
        rep->handleReplaceBetween(start32, limit32, UnicodeString());
    } else {
        // copy
        rep->copy(start32, limit32, destIndex32);
    }

    // If the change to the text touched the region in the chunk buffer,
    //  invalidate the buffer.
    int32_t firstAffectedIndex = destIndex32;
    if (move && start32<firstAffectedIndex) {
        firstAffectedIndex = start32;
    }
    if (firstAffectedIndex < ut->chunkNativeLimit) {
        // changes may have affected range covered by the chunk
        invalidateChunk(ut);
    }

    // Put iteration position at the newly inserted (moved) block,
    int32_t  nativeIterIndex = destIndex32 + limit32 - start32;
    if (move && destIndex32>start32) {
        // moved a block of text towards the end of the string.
        nativeIterIndex = destIndex32;
    }

    // Set position, reload chunk if needed.
    repTextAccess(ut, nativeIterIndex, TRUE);
}






U_DRAFT UText * U_EXPORT2
utext_openReplaceable(UText *ut, Replaceable *rep, UErrorCode *status) 
{
    if(U_FAILURE(*status)) {
        return NULL;
    }
    if(rep==NULL) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    ut = utext_setup(ut, sizeof(ReplExtra), status);
    
    ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_WRITABLE);
    if(rep->hasMetaData()) {
        ut->providerProperties |=I32_FLAG(UTEXT_PROVIDER_HAS_META_DATA);
    }

    ut->clone        = repTextClone;
    ut->nativeLength = repTextLength;
    ut->access       = repTextAccess;
    ut->extract      = repTextExtract;
    ut->replace      = repTextReplace;
    ut->copy         = repTextCopy;
    ut->close        = repTextClose;

    ut->context=rep;
    return ut;
}

U_CDECL_END








//------------------------------------------------------------------------------
//
//     UText implementation for UnicodeString (read/write)  and
//                    for const UnicodeString (read only)
//             (same implementation, only the flags are different)
//
//         Use of UText data members:
//            context    pointer to UnicodeString
//            p          pointer to UnicodeString IF this UText owns the string
//                       and it must be deleted on close().  NULL otherwise.
//
//------------------------------------------------------------------------------

U_CDECL_BEGIN


static UText * U_CALLCONV
unistrTextClone(UText *dest, const UText *src, UBool deep, UErrorCode *status) {
    // First do a generic shallow clone.  Does everything needed for the UText struct itself.
    dest = shallowTextClone(dest, src, status);

    // For deep clones, make a copy of the UnicodeSring.
    //  The copied UnicodeString storage is owned by the newly created UText clone.
    //  A non-NULL pointer in UText.p is the signal to the close() function to delete
    //    the UText.
    //
    if (deep && U_SUCCESS(*status)) {
        const UnicodeString *srcString = (const UnicodeString *)src->context;
        dest->context = new UnicodeString(*srcString);
        dest->p       = dest->context;

        // with deep clone, the copy is writable, even when the source is not.
        dest->providerProperties |= I32_FLAG(UTEXT_PROVIDER_WRITABLE);
    }
    return dest;
}
    
static void U_CALLCONV
unistrTextClose(UText *ut) {
    // Most of the work of close is done by the generic UText framework close.
    // All that needs to be done here is delete the UnicodeString if the UText
    //  owns it.  This occurs if the UText was created by cloning.
    UnicodeString *str = (UnicodeString *)ut->p;
    delete str;
    ut->p = NULL;
}


static int64_t U_CALLCONV
unistrTextLength(UText *t) {
    return ((const UnicodeString *)t->context)->length();
}


static UBool U_CALLCONV
unistrTextAccess(UText *ut, int64_t index, UBool  forward) {
    int32_t length  = ut->chunkLength;
    ut->chunkOffset = pinIndex(index, length);

    // Check whether request is at the start or end
    UBool retVal = (forward && index<length) || (!forward && index>0);
    return retVal; 
}



static int32_t U_CALLCONV
unistrTextExtract(UText *t,
                  int64_t start, int64_t limit,
                  UChar *dest, int32_t destCapacity,
                  UErrorCode *pErrorCode) {
    const UnicodeString *us=(const UnicodeString *)t->context;
    int32_t length=us->length();

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start<0 || start>limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    int32_t start32 = start<length ? us->getChar32Start((int32_t)start) : length;
    int32_t limit32 = limit<length ? us->getChar32Start((int32_t)limit) : length;

    length=limit32-start32;
    if (destCapacity>0 && dest!=NULL) {
        int32_t trimmedLength = length;
        if(trimmedLength>destCapacity) {
            trimmedLength=destCapacity;
        }
        us->extract(start32, trimmedLength, dest);
    }
    u_terminateUChars(dest, destCapacity, length, pErrorCode);
    return length;
}

static int32_t U_CALLCONV
unistrTextReplace(UText *ut,
                  int64_t start, int64_t limit,
                  const UChar *src, int32_t length,
                  UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)ut->context;
    int32_t oldLength;

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(src==NULL && length!=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start>limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    oldLength=us->length(); 
    int32_t start32 = pinIndex(start, oldLength);
    int32_t limit32 = pinIndex(limit, oldLength);
    if (start32 < oldLength) {
        start32 = us->getChar32Start(start32);
    }
    if (limit32 < oldLength) {
        limit32 = us->getChar32Start(limit32);
    }

    // replace
    us->replace(start32, limit32-start32, src, length);
    int32_t newLength = us->length();

    // Update the chunk description.
    ut->chunkContents    = us->getBuffer();
    ut->chunkLength      = newLength;
    ut->chunkNativeLimit = newLength;

    // Set iteration position to the point just following the newly inserted text.
    int32_t lengthDelta = newLength - oldLength;
    ut->chunkOffset = limit32 + lengthDelta;

    return lengthDelta;
}

static void U_CALLCONV
unistrTextCopy(UText *ut,
               int64_t start, int64_t limit,
               int64_t destIndex,
               UBool move,
               UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)ut->context;
    int32_t length=us->length();

    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    int32_t start32 = pinIndex(start, length);
    int32_t limit32 = pinIndex(limit, length);
    int32_t destIndex32 = pinIndex(destIndex, length);

    if( start32>limit32 || (start32<destIndex32 && destIndex32<limit32)) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }

    if(move) {
        // move: copy to destIndex, then replace original with nothing
        int32_t segLength=limit32-start32;
        us->copy(start32, limit32, destIndex32);
        if(destIndex32<start32) {
            start32+=segLength;
        }
        us->replace(start32, segLength, NULL, 0);
    } else {
        // copy
        us->copy(start32, limit32, destIndex32);
    }
    
    // update chunk description, set iteration position.
    ut->chunkContents = us->getBuffer();
    if (move==FALSE) {
        // copy operation, string length grows
        ut->chunkLength += limit32-start32;
        ut->chunkNativeLimit = ut->chunkLength;
    }

    // Iteration position to end of the newly inserted text.
    ut->chunkOffset = destIndex32+limit32-start32;
    if (move && destIndex32>start32) {  //TODO:  backwards? check.
        ut->chunkOffset = destIndex32;
    }

}

U_CDECL_END


U_DRAFT UText * U_EXPORT2
utext_openUnicodeString(UText *ut, UnicodeString *s, UErrorCode *status) {
    // TODO:  use openConstUnicodeString, then add in the differences.
    ut = utext_setup(ut, 0, status);
    if (U_SUCCESS(*status)) {
        ut->clone        = unistrTextClone;
        ut->nativeLength = unistrTextLength;
        ut->access       = unistrTextAccess;
        ut->extract      = unistrTextExtract;
        ut->replace      = unistrTextReplace;
        ut->copy         = unistrTextCopy;
        ut->close        = unistrTextClose;

        ut->context      = s;
        ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS)|
                                 I32_FLAG(UTEXT_PROVIDER_WRITABLE);

        ut->chunkContents      = s->getBuffer();
        ut->chunkLength        = s->length();
        ut->chunkNativeStart   = 0;
        ut->chunkNativeLimit   = ut->chunkLength;
        ut->nonUTF16Indexes    = FALSE;
    }
    return ut;
}



U_DRAFT UText * U_EXPORT2
utext_openConstUnicodeString(UText *ut, const UnicodeString *s, UErrorCode *status) {
    ut = utext_setup(ut, 0, status);
    if (U_SUCCESS(*status)) {
        ut->clone        = unistrTextClone;
        ut->nativeLength = unistrTextLength;
        ut->access       = unistrTextAccess;
        ut->extract      = unistrTextExtract;
        ut->close        = unistrTextClose;

        ut->context      = s;
        ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
        ut->chunkContents      = s->getBuffer();
        ut->chunkLength        = s->length();
        ut->chunkNativeStart   = 0;
        ut->chunkNativeLimit   = ut->chunkLength;
        ut->nonUTF16Indexes    = FALSE;

    }
    return ut;
}

//------------------------------------------------------------------------------
//
//     UText implementation for const UChar * strings 
//
//         Use of UText data members:
//            context    pointer to UnicodeString
//            a          length.  -1 if not yet known.
//
//         TODO:  support 64 bit lengths.
//
//------------------------------------------------------------------------------

U_CDECL_BEGIN


static UText * U_CALLCONV
ucstrTextClone(UText *dest, const UText * src, UBool deep, UErrorCode * status) {
    // First do a generic shallow clone.  
    dest = shallowTextClone(dest, src, status);

    // For deep clones, make a copy of the string.
    //  The copied storage is owned by the newly created clone.
    //  A non-NULL pointer in UText.p is the signal to the close() function to delete
    //    it.
    //
    if (deep && U_SUCCESS(*status)) {
        U_ASSERT(utext_nativeLength(dest) < INT32_MAX);
        int32_t  len = (int32_t)utext_nativeLength(dest);

        // The cloned string IS going to be NUL terminated, whether or not the orginal was.
        const UChar *srcStr = (const UChar *)src->context;
        UChar *copyStr = (UChar *)uprv_malloc((len+1) * sizeof(UChar));
        if (copyStr == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
        } else {
            int64_t i;
            for (i=0; i<len; i++) {
                copyStr[i] = srcStr[i];
            }
            copyStr[len] = 0;
            dest->context = copyStr;
            dest->p       = copyStr;
        }
    }
    return dest;
}


static void U_CALLCONV
ucstrTextClose(UText *ut) {
    // Most of the work of close is done by the generic UText framework close.
    // All that needs to be done here is delete the string if the UText
    //  owns it.  This occurs if the UText was created by cloning.
    UChar *s = (UChar *)ut->p;
    uprv_free(s);
    ut->p = NULL;
}



static int64_t U_CALLCONV
ucstrTextLength(UText *ut) {
    if (ut->a < 0) {
        // null terminated, we don't yet know the length.  Scan for it.
        //    Access is not convenient for doing this  
        //    because the current interation postion can't be changed.
        const UChar  *str = (const UChar *)ut->context;
        for (;;) {
            if (str[ut->chunkNativeLimit] == 0) {
                break;
            }
            ut->chunkNativeLimit++;
        }
        ut->a = ut->chunkNativeLimit;
        ut->chunkLength = (int32_t)ut->chunkNativeLimit;
        ut->providerProperties &= ~I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
    }
    return ut->a;
}


static UBool U_CALLCONV
ucstrTextAccess(UText *ut, int64_t index, UBool  forward) {
    const UChar *str   = (const UChar *)ut->context;
        
    // pin the requested index to the bounds of the string,
    //  and set current iteration position.
    if (index<0) {
        index = 0;
    } else if (index < ut->chunkNativeLimit) {
        // The request data is within the chunk as it is known so far.
        // Put index on a code point boundary.
        U16_SET_CP_START(str, 0, index);
    } else if (ut->a >= 0) {
        // We know the length of this string, and the user is requesting something
        // at or beyond the length.  Pin the requested index to the length.
        index = ut->a;
    } else {
        // Null terminated string, length not yet known, and the requested index
        //  is beyond where we have scanned so far.
        //  Scan to 32 UChars beyond the requested index.  The strategy here is
        //  to avoid fully scanning a long string when the caller only wants to
        //  see a few characters at its beginning.
        int32_t scanLimit = (int32_t)index + 32;
        if ((index + 32)>INT32_MAX || (index + 32)<0 ) {   // note: int64 expression
            scanLimit = INT32_MAX;
        }

        int32_t chunkLimit = (int32_t)ut->chunkNativeLimit;
        for (; chunkLimit<scanLimit; chunkLimit++) {
            if (str[chunkLimit] == 0) {
                // We found the end of the string.  Remember it, pin the requested index to it,
                //  and bail out of here.
                ut->a = chunkLimit;
                ut->chunkLength = chunkLimit;
                if (index >= chunkLimit) {
                    index = chunkLimit;
                } else {
                    U16_SET_CP_START(str, 0, index);
                }

                ut->chunkNativeLimit = chunkLimit;
                ut->providerProperties &= ~I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
                goto breakout;
            }
        }
        // We scanned through the next batch of UChars without finding the end.
        U16_SET_CP_START(str, 0, index);
        if (chunkLimit == INT32_MAX) {
            // Scanned to the limit of a 32 bit length.
            // Forceably trim the overlength string back so length fits in int32
            //  TODO:  add support for longer strings.
            ut->a = chunkLimit;
            ut->chunkLength = chunkLimit;
            if (index > chunkLimit) {
                index = chunkLimit;
            }
            ut->chunkNativeLimit = chunkLimit;
            ut->providerProperties &= ~I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
        } else {
            // The endpoint of a chunk must not be left in the middle of a surrogate pair.
            // If the current end is on a lead surrogate, back the end up by one.
            // It doesn't matter if the end char happens to be an unpaired surrogate,
            //    and it's simpler not to worry about it.
            if (U16_IS_LEAD(str[chunkLimit-1])) {
                --chunkLimit;
            }
            ut->chunkNativeLimit = chunkLimit;
        }

    }
breakout:
    U_ASSERT(index<=INT32_MAX);
    ut->chunkOffset = (int32_t)index;

    // Check whether request is at the start or end
    UBool retVal = (forward && index<ut->chunkNativeLimit) || (!forward && index>0);
    return retVal; 
}



static int32_t U_CALLCONV
ucstrTextExtract(UText *ut,
                  int64_t start, int64_t limit,
                  UChar *dest, int32_t destCapacity,
                  UErrorCode *pErrorCode) 
{
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0) || start>limit) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    const UChar *s=(const UChar *)ut->context;
    int32_t si, di;

    int32_t start32;
    int32_t limit32;

    // Access the start.  Does two things we need:
    //   Pins 'start' to the length of the string, if it came in out-of-bounds.
    //   Snaps 'start' to the beginning of a code point.
    ucstrTextAccess(ut, start, TRUE);
    U_ASSERT(start <= INT32_MAX);
    start32 = (int32_t)start;

    int32_t strLength=(int32_t)ut->a;
    if (strLength >= 0) {
        limit32 = pinIndex(limit, strLength);
    } else {
        limit32 = pinIndex(limit, INT32_MAX);
    }

    di = 0;
    for (si=start32; si<limit32; si++) {
        if (strLength<0 && s[si]==0) {
            // Just hit the end of a null-terminated string.
            ut->a = si;               // set string length for this UText
            ut->chunkNativeLimit = si;
            ut->chunkLength      = si;
            strLength            = si;
            break;
        }
        if (di<destCapacity) {
            // only store if there is space.
            dest[di] = s[si];
        } else {
            if (strLength>=0) {
                // We have filled the destination buffer, and the string length is known.
                //  Cut the loop short.  There is no need to scan string termination.
                di = strLength;
                si = limit32;
                break;
            }
        }
        di++;
    }

    // If the limit index points to a lead surrogate of a pair,
    //   add the corresponding trail surrogate to the destination.
    if (si>0 && U16_IS_LEAD(s[si-1]) &&
        ((si<strLength || strLength<0)  && U16_IS_TRAIL(s[si])))
    {
        if (di<destCapacity) {
            // store only if there is space in the output buffer.
            dest[di++] = s[si++];
        }
    }

    // Put iteration position at the point just following the extracted text
    ut->chunkOffset = si;

    // Add a terminating NUL if space in the buffer permits,
    // and set the error status as required.
    u_terminateUChars(dest, destCapacity, di, pErrorCode);
    return di;
}



U_CDECL_END


U_DRAFT UText * U_EXPORT2
utext_openUChars(UText *ut, const UChar *s, int64_t length, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }
    if (length < -1 || length>INT32_MAX) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    ut = utext_setup(ut, 0, status);
    if (U_SUCCESS(*status)) {
        ut->clone        = ucstrTextClone;
        ut->nativeLength = ucstrTextLength;
        ut->access       = ucstrTextAccess;
        ut->extract      = ucstrTextExtract;
        ut->replace      = NULL;
        ut->copy         = NULL;
        ut->close        = ucstrTextClose;

        ut->context              = s;
        ut->providerProperties   = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
        if (length==-1) {
            ut->providerProperties |= I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
        }
        ut->a                    = length;
        ut->chunkContents        = s;
        ut->chunkNativeStart     = 0;
        ut->chunkNativeLimit     = length>=0? length : 0;
        ut->chunkLength          = (int32_t)ut->chunkNativeLimit;
        ut->chunkOffset          = 0;
        ut->nonUTF16Indexes      = FALSE;
    }
    return ut;
}


//------------------------------------------------------------------------------
//
//     UText implementation for text from ICU CharacterIterators 
//
//         Use of UText data members:
//            context    pointer to the CharacterIterator
//            a          length of the full text.  
//            p          pointer to  buffer 1
//            b          start index of local buffer 1 contents
//            q          pointer to buffer 2
//            c          start index of local buffer 2 contents
//            r          pointer to the character iterator if the UText owns it.
//                       Null otherwise.
//            
//------------------------------------------------------------------------------
#define CIBufSize 16

U_CDECL_BEGIN
static void U_CALLCONV
charIterTextClose(UText *ut) {
    // Most of the work of close is done by the generic UText framework close.
    // All that needs to be done here is delete the CharacterIterator if the UText
    //  owns it.  This occurs if the UText was created by cloning.
    CharacterIterator *ci = (CharacterIterator *)ut->r;
    delete ci;
    ut->r = NULL;
}

static int64_t U_CALLCONV
charIterTextLength(UText *ut) {
    return (int32_t)ut->a;
}

static UBool U_CALLCONV
charIterTextAccess(UText *ut, int64_t index, UBool  forward) {
    CharacterIterator *ci   = (CharacterIterator *)ut->context;

    int32_t clippedIndex = (int32_t)index;
    if (clippedIndex<0) {
        clippedIndex=0;
    } else if (clippedIndex>=ut->a) {
        clippedIndex=(int32_t)ut->a;
    }
    int32_t neededIndex = clippedIndex;
    if (!forward && neededIndex>0) {
        // reverse iteration, want the position just before what was asked for.
        neededIndex--;
    } else if (forward && neededIndex==ut->a && neededIndex>0) {
        // Forward iteration, don't ask for something past the end of the text.
        neededIndex--;
    }

    // Find the native index of the start of the buffer containing what we want.
    neededIndex -= neededIndex % CIBufSize;

    UChar *buf = NULL;
    UBool  needChunkSetup = TRUE;
    int    i;
    if (ut->chunkNativeStart == neededIndex) {
        // The buffer we want is already the current chunk.
        needChunkSetup = FALSE;
    } else if (ut->b == neededIndex) {
        // The first buffer (buffer p) has what we need.
        buf = (UChar *)ut->p;
    } else if (ut->c == neededIndex) {
        // The second buffer (buffer q) has what we need.
        buf = (UChar *)ut->q;
    } else {
        // Neither buffer already has what we need.
        // Load new data from the character iterator.
        // Use the buf that is not the current buffer.
        buf = (UChar *)ut->p;
        if (ut->p == ut->chunkContents) {
            buf = (UChar *)ut->q;
        }
        ci->setIndex(neededIndex);
        for (i=0; i<CIBufSize; i++) {
            buf[i] = ci->nextPostInc();
            if (i+neededIndex > ut->a) {
                break;
            }
        }
    }

    // We have a buffer with the data we need.
    // Set it up as the current chunk, if it wasn't already.
    if (needChunkSetup) {
        ut->chunkContents = buf;
        ut->chunkLength   = CIBufSize;
        ut->chunkNativeStart = neededIndex;
        ut->chunkNativeLimit = neededIndex + CIBufSize;
        if (ut->chunkNativeLimit > ut->a) {
            ut->chunkNativeLimit = ut->a;
            ut->chunkLength  = (int32_t)(ut->chunkNativeLimit)-(int32_t)(ut->chunkNativeStart);
        }
        ut->chunkOffset = clippedIndex - (int32_t)ut->chunkNativeStart;
        U_ASSERT(ut->chunkOffset>=0 && ut->chunkOffset<=CIBufSize);
    }
    UBool success = (forward? ut->chunkOffset<ut->chunkLength : ut->chunkOffset>0);
    return success;
}

static UText * U_CALLCONV
charIterTextClone(UText *dest, const UText *src, UBool deep, UErrorCode * status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }


    // For deep clones, make a copy of the string.
    //  The copied storage is owned by the newly created clone.
    //  A non-NULL pointer in UText.p is the signal to the close() function to delete
    //    it.
    //
    if (deep) {
        // TODO
        U_ASSERT(FALSE);
        return NULL;
    } else {
        CharacterIterator *srcCI =(CharacterIterator *)src->context;
        srcCI = srcCI->clone();
        dest = utext_openCharacterIterator(dest, srcCI, status);
        // cast off const on getNativeIndex.
        //   For CharacterIterator based UTexts, this is safe, the operation is const.
        int64_t  ix = utext_getNativeIndex((UText *)src);
        utext_setNativeIndex(dest, ix);
        dest->r = srcCI;    // flags that this UText owns the CharacterIterator
    }
    return dest;
}

static int32_t U_CALLCONV
charIterTextExtract(UText *ut,
                  int64_t start, int64_t limit,
                  UChar *dest, int32_t destCapacity,
                  UErrorCode *status) 
{
    if(U_FAILURE(*status)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0) || start>limit) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    int32_t  length  = (int32_t)ut->a;
    int32_t  start32 = pinIndex(start, length);
    int32_t  limit32 = pinIndex(limit, length);
    int32_t  desti   = 0;
    int32_t  srci;

    CharacterIterator *ci = (CharacterIterator *)ut->context;
    ci->setIndex32(start32);   // Moves ix to lead of surrogate pair, if needed.
    srci = ci->getIndex();
    while (srci<limit32) {
        UChar32 c = ci->next32PostInc();
        int32_t  len = U16_LENGTH(c);
        if (desti+len <= destCapacity) {
            U16_APPEND_UNSAFE(dest, desti, c);
        } else {
            desti += len;
            *status = U_BUFFER_OVERFLOW_ERROR;
        }
        srci += len;
    }
    if (desti<destCapacity) {
        dest[desti] = 0;
    }
    return desti;
}
U_CDECL_END



U_DRAFT UText * U_EXPORT2
utext_openCharacterIterator(UText *ut, CharacterIterator *ci, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }

    if (ci->startIndex() > 0) {
        // No support for CharacterIterators that do not start indexing from zero.
        *status = U_UNSUPPORTED_ERROR;
        return NULL;
    }

    // Extra space in UText for 2 buffers of CIBufSize UChars each.
    int32_t  extraSpace = 2 * CIBufSize * sizeof(UChar);
    ut = utext_setup(ut, extraSpace, status);
    if (U_SUCCESS(*status)) {
        ut->clone        = charIterTextClone;
        ut->nativeLength = charIterTextLength;
        ut->access       = charIterTextAccess;
        ut->extract      = charIterTextExtract;
        ut->replace      = NULL;
        ut->copy         = NULL;
        ut->close        = charIterTextClose;

        ut->context              = ci;
        ut->providerProperties   = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
        ut->a                    = ci->endIndex();        // Length of text
        ut->p                    = ut->pExtra;            // First buffer
        ut->b                    = -1;                    // Native index of first buffer contents
        ut->q                    = (UChar*)ut->pExtra+CIBufSize;  // Second buffer
        ut->c                    = -1;                    // Native index of second buffer contents

        // Initialize current chunk contents to be empty.
        //   First access will fault something in.
        //   Note:  The initial nativeStart and chunkOffset must sum to zero
        //          so that getNativeIndex() will correctly compute to zero
        //          if no call to Access() has ever been made.  They can't be both
        //          zero without Access() thinking that the chunk is valid.
        ut->chunkContents        = (UChar *)ut->p;
        ut->chunkNativeStart     = -1;
        ut->chunkOffset          = 1;
        ut->chunkNativeLimit     = 0;
        ut->chunkLength          = 0;
        ut->nonUTF16Indexes      = FALSE;
    }
    return ut;
}





