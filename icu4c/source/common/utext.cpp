/*
*******************************************************************************
*
*   Copyright (C) 2005, International Business Machines
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
#include "unicode/utext.h"
#include "ustr_imp.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"


#define I32_FLAG(bitIndex) ((int32_t)1<<(bitIndex))


static UBool
utext_access(UText *ut, int32_t index, UBool forward) {
    return ut->access(ut, index, forward, &ut->chunk);
}



U_DRAFT UBool U_EXPORT2
utext_moveIndex(UText *ut, int32_t delta) {
    UBool retval = TRUE;
    if(delta>0) {
        do {
            if(ut->chunk.offset>=ut->chunk.length && !utext_access(ut, ut->chunk.nativeLimit, TRUE)) {
                retval = FALSE;
                break;
            }
            U16_FWD_1(ut->chunk.contents, ut->chunk.offset, ut->chunk.length);
        } while(--delta>0);
    } else if (delta<0) {
        do {
            if(ut->chunk.offset<=0 && !utext_access(ut, ut->chunk.nativeStart, FALSE)) {
                retval = FALSE;
                break;
            }
            U16_BACK_1(ut->chunk.contents, 0, ut->chunk.offset);
        } while(++delta<0);
    }   

    return retval;
}


U_DRAFT int32_t U_EXPORT2
utext_length(UText *ut) {
    return ut->length(ut);
}


U_DRAFT UBool U_EXPORT2
utext_isLengthExpensive(const UText *ut) {
    UBool r = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE)) != 0;
    return r;
}


U_DRAFT int32_t U_EXPORT2
utext_getIndex(UText *ut) {
    if(!ut->chunk.nonUTF16Indexes || ut->chunk.offset==0) {
        return ut->chunk.nativeStart+ut->chunk.offset;
    } else {
        return ut->mapOffsetToNative(ut, ut->chunk.offset);
    }
}



U_DRAFT void U_EXPORT2
utext_setIndex(UText *ut, int32_t index) {
    if(index<ut->chunk.nativeStart || ut->chunk.nativeLimit<index) {
        // The desired position is outside of the current chunk.  
        // Access the new position.  Assume a forward iteration from here,
        // which will also be optimimum for a single random access.
        // Reverse iterations may suffer slightly.
        ut->access(ut, index, TRUE, &ut->chunk);
    } else if(ut->chunk.nonUTF16Indexes) {
        ut->chunk.offset=ut->mapIndexToUTF16(ut, index);
    } else {
        ut->chunk.offset=index-ut->chunk.nativeStart;
        // Our convention is that the index must always be on a code point boundary.
        //  If we are somewhere in the middle of a utf-16 buffer, check that new index
        //  is not in the middle of a surrogate pair.
        if (index>ut->chunk.nativeStart && index < ut->chunk.nativeLimit) {
            UChar c = ut->chunk.contents[ut->chunk.offset];
            if (U16_TRAIL(c)) {
                utext_current32(ut);  // force index to the start of the curent code point.
            }
        }
    }
}



  
U_DRAFT UChar32 U_EXPORT2
utext_current32(UText *ut) {
    UChar32  c = U_SENTINEL;
    if (ut->chunk.offset < ut->chunk.length) {
        c = ut->chunk.contents[ut->chunk.offset];
        if (U16_IS_SURROGATE(c)) {
            // looking at a surrogate.  Could be unpaired, need to be careful.
            // Speed doesn't matter, will be very rare.
            UChar32  char16AtIndex = c;
            U16_GET(ut->chunk.contents, 0, ut->chunk.offset, ut->chunk.length, c);
            if (U16_IS_TRAIL(char16AtIndex) && U_IS_SUPPLEMENTARY(c)) {
                // Incoming position pointed to the trailing part of a  supplementary pair.
                // Move offset to point to the lead surrogate.  This is needed because utext_current()
                // is used internally to force code point alignment.  When called from
                // the outside we should always be pre-aligned, but this check doesn't hurt.
                ut->chunk.offset--;
            }
        }
    }
    return c;
}


U_DRAFT UChar32 U_EXPORT2
utext_char32At(UText *ut, int32_t nativeIndex) {
    UChar32 c = U_SENTINEL;
    utext_setIndex(ut, nativeIndex);
    if (nativeIndex >= 0 && nativeIndex < ut->chunk.nativeLimit) {
        c = ut->chunk.contents[ut->chunk.offset];
    }
    return c;
}


U_DRAFT UChar32 U_EXPORT2
utext_next32(UText *ut) {
    UTextChunk   *chunk  = &ut->chunk;
    int32_t       offset = chunk->offset;
    UChar32       c      = U_SENTINEL;

    if (offset >= chunk->length) {
        if (ut->access(ut, chunk->nativeLimit, TRUE, chunk) == FALSE) {
            goto next32_return;
        }
        offset = chunk->offset;
    }
            
    c = chunk->contents[offset++];
    if (U16_IS_SURROGATE(c)) {
        // looking at a surrogate.  Could be unpaired, need to be careful.
        // Speed doesn't matter, will be very rare.
        c =  utext_current32(ut);
        if (U_IS_SUPPLEMENTARY(c)) {
            offset++;
        }
    }
    chunk->offset = offset;

next32_return:
    return c;
}



U_DRAFT UChar32 U_EXPORT2
utext_previous32(UText *ut) {
    UTextChunk   *chunk  = &ut->chunk;
    int32_t       offset = chunk->offset;
    UChar32       c      = U_SENTINEL;

    if (offset <= 0) {
        if (ut->access(ut, chunk->nativeStart, FALSE, chunk) == FALSE) {
            goto prev32_return;
        }
        offset = chunk->offset;
    }
            
    c = chunk->contents[--offset];
    chunk->offset = offset;
    if (U16_IS_SURROGATE(c)) {
        // Note that utext_current() will move the chunk offset to the lead surrogate
        // if we come in referring to trail half of a surrogate pair.
        c =  utext_current32(ut);
    } 

prev32_return:
    return c;
}



U_DRAFT UChar32 U_EXPORT2
utext_next32From(UText *ut, int32_t index) {
    int32_t       offset;   // index into the chunk buffer containing the desired char.
    UTextChunk   *chunk  = &ut->chunk;
    UChar32       c      = U_SENTINEL;

    if(index<chunk->nativeStart || index>=chunk->nativeLimit) {
        if(!ut->access(ut, index, TRUE, chunk)) {
            // no chunk available here
            goto next32return;
        }
        offset = chunk->offset;
    } else if(chunk->nonUTF16Indexes) {
        offset=ut->mapIndexToUTF16(ut, index);
    } else {
        offset = index - chunk->nativeStart;
    }

    c = chunk->contents[offset++];
    if (U16_IS_SURROGATE(c)) {
        // Surrogate code unit.  Could be pointing at either half of a pair, or at
        //   an unpaired surrogate.  Let utext_current() do the work.  Speed doesn't matter.
        chunk->offset = offset;
        c = utext_current32(ut);  
        if (U_IS_SUPPLEMENTARY(c)) {
            offset++;
        }
    }
    chunk->offset = offset;
next32return:
    return c;
}


U_DRAFT UChar32 U_EXPORT2
utext_previous32From(UText *ut, int32_t index) {
    int32_t     offset;   // index into the chunk buffer containing the desired char.
    UTextChunk *chunk = &ut->chunk;
    UChar32     c     = U_SENTINEL;

    if(index<=chunk->nativeStart || index>chunk->nativeLimit) {
        if(!ut->access(ut, index, FALSE, chunk)) {
            // no chunk available here
            goto prev32return;
        }
        offset = chunk->offset;
    } else if(chunk->nonUTF16Indexes) {
        offset=ut->mapIndexToUTF16(ut, index);
    } else {
        offset = index - chunk->nativeStart;
    }

    offset--;
    c = chunk->contents[offset];
    chunk->offset = offset;
    if (U16_IS_SURROGATE(c)) {
        c = utext_current32(ut);  // get supplementary char if not unpaired surrogate,
                                  //  and adjust offset to start.
    }
prev32return:
    return c;
}


U_DRAFT int32_t U_EXPORT2
utext_extract(UText *ut,
             int32_t start, int32_t limit,
             UChar *dest, int32_t destCapacity,
             UErrorCode *status) {
                 return ut->extract(ut, start, limit, dest, destCapacity, status);
             }




U_DRAFT UBool U_EXPORT2
utext_isWriteble(const UText *ut)
{
    UBool b = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_WRITABLE)) != 0;
    return b;
}


U_DRAFT UBool U_EXPORT2
utext_hasMetaData(const UText *ut)
{
    UBool b = (ut->providerProperties & I32_FLAG(UTEXT_PROVIDER_HAS_META_DATA)) != 0;
    return b;
}



U_DRAFT int32_t U_EXPORT2
utext_replace(UText *ut,
             int32_t nativeStart, int32_t nativeLimit,
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
          int32_t nativeStart, int32_t nativeLimit,
          int32_t destIndex,
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
utext_clone(UText *dest, const UText *src, UBool deep, UErrorCode *status) {
    return src->clone(dest, src, deep, status);
}

U_DRAFT UBool U_EXPORT2
utext_compare(UText *ut, const UChar *s, int32_t length, UBool codePointOrder) {
    int32_t segLength, result;

    if(length<0) {
        length=u_strlen(s);
    }
    if(length==0) {
        return 0;
    }
    for(;;) {
        // compare starting from the current position in the current chunk
        segLength=ut->chunk.length-ut->chunk.offset;
        if(segLength>length) {
            segLength=length;
        }
        result=u_strCompare(
            ut->chunk.contents+ut->chunk.offset, segLength,
            s, length,
            codePointOrder);
        ut->chunk.offset+=segLength;
        if(result!=0) {
            return result;
        }

        // compare the next chunk
        s+=segLength;
        length-=segLength;
        if(length==0) {
            return 0;
        }

        if(!ut->access(ut, ut->chunk.nativeLimit, TRUE, &ut->chunk)) {
            // the text ends before the string does
            return -1;
        }
    }
    return 0;
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
                ut->extraSize = spaceRequired;
                ut->pExtra    = &((ExtendedUText *)ut)->extension;
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
            }
        }
    }
    if (U_SUCCESS(*status)) {
        ut->flags |= UTEXT_OPEN;
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

    // If we (the famework) allocated the UText or subsidiary storage,
    //   delete it.
    if (ut->flags & UTEXT_EXTRA_HEAP_ALLOCATED) {
        uprv_free(ut->pExtra);
        ut->pExtra = NULL;
    }
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
resetChunk(UTextChunk *chunk, int32_t index) {
    if (index==chunk->nativeLimit) {
        chunk->offset = chunk->length;
    } else if (index==chunk->nativeStart) {
        chunk->offset = 0;
    } else {
        chunk->length      = 0;
        chunk->nativeStart = index;
        chunk->nativeLimit = index;
        chunk->offset      = 0;
    } 
}

        


//------------------------------------------------------------------------------
//
// No-Op UText implementation for illegal input 
//
//------------------------------------------------------------------------------
U_CDECL_BEGIN

static UText * U_CALLCONV
//
//  Clone.  This is a generic copy-the-utext-by-value clone function that can be
//          used as-is with some utext types, and as helper by other clones. 
//
noopTextClone(UText * dest, const UText * src, UBool deep, UErrorCode * status) {
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

    return dest;
}


static int32_t U_CALLCONV
noopTextLength(UText * /* t */) {
    return 0;
}

static UBool U_CALLCONV
noopTextAccess(UText * /* t */, int32_t /* index */, UBool /* forward*/,
               UTextChunk * /* chunk */) {
    return FALSE;
}

static int32_t U_CALLCONV
noopTextExtract(UText * /* t */,
                int32_t /* start */, int32_t /* limit */,
                UChar * /* dest */, int32_t /* destCapacity */,
                UErrorCode * /* pErrorCode */) {
    return 0;
}

static int32_t U_CALLCONV
noopTextMapOffsetToNative(UText * /* t */, int32_t /* offset */) {
    return 0;
}

static int32_t U_CALLCONV
noopTextMapIndexToUTF16(UText * /* t */, int32_t /* index */) {
    return 0;
}

U_CDECL_END


static const UText noopText={
    UTEXT_INITIALZIER_HEAD,
    noopTextClone,
    noopTextLength,
    noopTextAccess,
    noopTextExtract,
    NULL, // replace
    NULL, // copy
    noopTextMapOffsetToNative,
    noopTextMapIndexToUTF16,
    NULL  // close
};



//------------------------------------------------------------------------------
//
//     UText implementation for UTF-8 strings (read-only) 
//
//         Use of UText data members:
//            context    pointer to UTF-8 string
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

//  utext.b  is the input string length (bytes).
//  utext.q  pointer to the filled part of the Map array.
//
//     because backwards iteration fills the buffers starting at the end and
//     working towards the front, the filled part of the buffers may not begin
//     at the start of the available storage for the buffers.

U_CDECL_BEGIN


static int32_t U_CALLCONV
utf8TextLength(UText *ut) {
    return ut->b;
}

        
   

static UBool U_CALLCONV
utf8TextAccess(UText *ut, int32_t index, UBool forward, UTextChunk *chunk) {
    const uint8_t *s8=(const uint8_t *)ut->context;
    UChar32  c;
    int32_t  i;
    int32_t  length = ut->b;              // Length of original utf-8

    UTF8Extra  *ut8e   = (UTF8Extra *)ut->pExtra;
    UChar      *u16buf = ut8e->s;
    int32_t    *map    = ut8e->map;

    if (index<0) {
        index = 0;
    } else if (index>length) {
        index = length;
    }

    if(forward) {
        if(index >= length) {
            resetChunk(chunk, length);
            return FALSE;
        }

        chunk->nativeStart=index;
        c=s8[index];
        if(c<=0x7f) {
            // get a run of ASCII characters.
            // Even if we don't fill the buffer, we will stop with the first
            //   non-ascii char, so that the buffer can use utf-16 indexing.
            u16buf[0]=(UChar)c;
            for(i=1, ++index;
                i<UTF8_TEXT_CHUNK_SIZE && index<length && (c=s8[index])<=0x7f;
                ++i, ++index
            ) {
                u16buf[i]=(UChar)c;
            }
            chunk->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            U8_SET_CP_START(s8, 0, index);  // put utf-8 index at first byte of char, if not there already.
            for(i=0;  i<UTF8_TEXT_CHUNK_SIZE && index<length;  ) {
                //  i     is utf-16 index into chunk buffer.
                //  index is utf-8 index into original string
                map[i]=index;
                map[i+1]=index; // in case there is a trail surrogate
                U8_NEXT(s8, index, length, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                U16_APPEND_UNSAFE(u16buf, i, c);    // post-increments i.
            }
            map[i]=index;
            chunk->nonUTF16Indexes=TRUE;
        }
        chunk->contents    = u16buf;
        chunk->length      = i;
        chunk->nativeLimit = index;
        ut->q              = map;  
        chunk->offset      = 0;      // chunkOffset corresponding to index
        return TRUE; 
    } else {
        // Reverse Access.  The chunk buffer must be filled so as to contain the
        //                  character preceding the specified index.
        if(index<=0) {
            resetChunk(chunk, 0);
            return FALSE;
        }

        chunk->nativeLimit=index;
        c=s8[index-1];
        if(c<=0x7f) {
            // get a chunk of ASCII characters.  Don't build the index map
            i=UTF8_TEXT_CHUNK_SIZE;
            do {
                u16buf[--i]=(UChar)c;
                --index;
            } while(i>0 && index>0 && (c=s8[index-1])<=0x7f);
            chunk->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            if(index<length) {
                U8_SET_CP_START(s8, 0, index);
            }
            i=UTF8_TEXT_CHUNK_SIZE;
            map[i]=index;    // map position for char following the last one in the buffer.
            do {
                //  i     is utf-16 index into chunk buffer.
                //  index is utf-8 index into original string
                U8_PREV(s8, 0, index, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                if(c<=0xffff) {
                    u16buf[--i]=(UChar)c;
                    map[i]=index;
                } else {
                    // We've got a supplementary char
                    if (i<2) {
                        // Both halves of the surrogate pair wont fit in the chunk buffer.
                        // Stop without putting either half in.
                        U8_NEXT(s8, index, length, c);  // restore index.
                        break;
                    }
                    u16buf[--i]=U16_TRAIL(c);
                    map[i]=index;
                    u16buf[--i]=U16_LEAD(c);
                    map[i]=index;
                }
            } while(i>0 && index>0);

            // Because we have filled the map & chunk buffers from back to front,
            //   the start position for accesses may not be at the start of the
            //   available storage.
            ut->q = map+i;
            chunk->nonUTF16Indexes=TRUE;
        }
        // Common reverse iteration, for both UTF16 and non-UTIF16 indexes.
        chunk->contents    = u16buf+i;
        chunk->length      = (UTF8_TEXT_CHUNK_SIZE)-i;
        chunk->nativeStart = index;
        chunk->offset      = chunk->length; // chunkOffset corresponding to index
        return TRUE;
    }
}

static int32_t U_CALLCONV
utf8TextExtract(UText *ut,
                int32_t start, int32_t limit,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start<0 || start>limit || ut->b<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    int32_t destLength=0;
    u_strFromUTF8(dest, destCapacity, &destLength,
                    (const char *)ut->context+start, limit-start,
                    pErrorCode);
    return destLength;
    // TODO: if U_INVALID|ILLEGAL_CHAR_FOUND, extract text anyway and use SUB for illegal sequences?
}

// Assume nonUTF16Indexes and 0<=offset<=chunk->length
static int32_t U_CALLCONV
utf8TextMapOffsetToNative(UText *ut, int32_t offset) {
    // UText.q points to the index mapping array that is allocated in the extra storage area.
    U_ASSERT(offset>=0 && offset<=ut->chunk.length);
    int32_t *map=(int32_t *)(ut->q);
    return map[offset];
}

// Assume nonUTF16Indexes and chunk->start<=index<=chunk->limit
static int32_t U_CALLCONV
utf8TextMapIndexToUTF16(UText *ut, int32_t index) {
    int32_t *map=(int32_t *)(ut->q);
    int32_t offset=0;

    U_ASSERT(index>=ut->chunk.nativeStart && index<=ut->chunk.nativeLimit);
    while(index>map[offset]) {
        ++offset;
    }
    return offset;
}




U_DRAFT UText * U_EXPORT2
utext_openUTF8(UText *ut, const uint8_t *s, int32_t length, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return NULL;
    }
    if(s==NULL || length<-1) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    ut = utext_setup(ut, sizeof(UTF8Extra), status);
    if (U_FAILURE(*status)) {
        return ut;
    }
    ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_NON_UTF16_INDEXES);

    ut->clone      = noopTextClone;
    ut->length     = utf8TextLength;
    ut->access     = utf8TextAccess;
    ut->extract    = utf8TextExtract;
    ut->mapOffsetToNative = utf8TextMapOffsetToNative;
    ut->mapIndexToUTF16   = utf8TextMapIndexToUTF16;

    ut->context=s;
    if(length>=0) {
        ut->b=length;
    } else {
        // TODO:  really undesirable to do this scan upfront.
        ut->b=(int32_t)uprv_strlen((const char *)s);
    }

    return ut;
}

U_CDECL_END





/* UText implementation wrapper for Replaceable (read/write) ---------------- */





//------------------------------------------------------------------------------
//
//     UText implementation wrapper for Replaceable (read/write) 
//
//         Use of UText data members:
//            context    pointer to Replaceable
//
//------------------------------------------------------------------------------


 /*
 * TODO: use a flag in RepText to support readonly strings?
 *       -> omit UTEXT_PROVIDER_WRITABLE
 */

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
    dest = noopTextClone(dest, src, deep, status);

    if (deep && U_SUCCESS(*status)) {
        const Replaceable *replSrc = (const Replaceable *)src->context;
        dest->context = replSrc->clone();
    }
    return dest;
}



static int32_t U_CALLCONV
repTextLength(UText *ut) {
    const Replaceable *replSrc = (const Replaceable *)ut->context;
    int32_t  len = replSrc->length();
    return len;
}


static UBool U_CALLCONV
repTextAccess(UText *ut, int32_t index, UBool forward, UTextChunk *chunk) {
    const Replaceable *rep=(const Replaceable *)ut->context;
    int32_t start;          // index of the start of the chunk to be loaded
    int32_t limit;          // index of the end+1 of the chunk to be loaded.
    int32_t length=rep->length();   // Full length of the input text (bigger than a chunk)


    /*
     * Compute start/limit boundaries around index, for a segment of text
     * to be extracted.
     * To allow for the possibility that our user gave an index to the trailing
     * half of a surrogate pair, we must request one extra preceding UChar when
     * going in the forward direction.  This will ensure that the buffer has the
     * entire code point at the specified index.
     */
    if(forward) {

        if (index>=ut->chunk.nativeStart && index<ut->chunk.nativeLimit) {
            // Buffer already contains the requested position.
            ut->chunk.offset = index - ut->chunk.nativeStart;
            return TRUE;
        }
        if (index>=length && ut->chunk.nativeLimit==length) {
            // Request for end of string, and buffer already extends up to it.
            // Can't get the data, but don't change the buffer.
            ut->chunk.offset = length - ut->chunk.nativeStart;
            return FALSE;
        }

        if (index<0) {
            index = 0;
        }
        ut->chunk.nativeLimit = index + REP_TEXT_CHUNK_SIZE - 1;
        // Going forward, so we want to have the buffer with stuff at and beyond
        //   the requested index.  The -1 gets us one code point before the
        //   requested index also, to handle the case of the index being on
        //   a trail surrogate of a surrogate pair.
        if(ut->chunk.nativeLimit > length) {
            ut->chunk.nativeLimit = length;
        }
        // unless buffer ran off end, start is index-1.
        ut->chunk.nativeStart = ut->chunk.nativeLimit - REP_TEXT_CHUNK_SIZE;   
        if(ut->chunk.nativeStart < 0) {
            ut->chunk.nativeStart = 0;
        }
    } else {
        // Reverse iteration.  Fill buffer with data preceding the requested index.
        if(index<0) {
            index = 0;
        }
        if (index>ut->chunk.nativeStart && index<=ut->chunk.nativeLimit) {
            // Requested position already in buffer.
            ut->chunk.offset = index - ut->chunk.nativeStart;
            return TRUE;
        }
        if (index==0 && ut->chunk.nativeStart==0) {
            // Request for start, buffer already begins at start.
            //  No data, but keep the buffer as is.
            ut->chunk.offset = 0;
            return FALSE;
        }
        limit = index;
        if (limit>length) {
            limit = length;
        }
        start=limit-REP_TEXT_CHUNK_SIZE;
        if(start<0) {
            start=0;
        }
    }
    ReplExtra *ex = (ReplExtra *)ut->pExtra;
    // UnicodeString with its buffer a writable alias to the chunk buffer
    UnicodeString buffer(ex->s, 0 /*buffer length*/, REP_TEXT_CHUNK_SIZE /*buffer capacity*/); 
    rep->extractBetween(ut->chunk.nativeStart, ut->chunk.nativeLimit, buffer);

    ut->chunk.contents = ex->s;
    ut->chunk.length    = ut->chunk.nativeLimit - ut->chunk.nativeStart;
    ut->chunk.offset    = index - ut->chunk.nativeStart;

    // Surrogate pairs from the input text must not span chunk boundaries.
    // If end of chunk could be the start of a surrogate, trim it off.
    if (ut->chunk.nativeLimit < length &&
        U16_IS_LEAD(ex->s[ut->chunk.length-1])) {
            ut->chunk.length--;
        }


    // if the first UChar in the chunk could be the trailing half of a surrogate pair,
    // trim it off.
    if(ut->chunk.nativeStart>0 && U16_IS_TRAIL(ex->s[0])) {
        ++(ut->chunk.contents);
        --(ut->chunk.length);
        --(ut->chunk.offset);
    }

    // adjust the index/chunkOffset to a code point boundary
    U16_SET_CP_START(ut->chunk.contents, 0, ut->chunk.offset);

    return TRUE; 
}



static int32_t U_CALLCONV
repTextExtract(UText *ut,
               int32_t start, int32_t limit,
               UChar *dest, int32_t destCapacity,
               UErrorCode *status) {
    const Replaceable *rep=(const Replaceable *)ut->context;
    int32_t length=rep->length();

    if(U_FAILURE(*status)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *status=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start<0 || start>limit || length<limit) {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    length=limit-start;
    if(length>destCapacity) {
        length=destCapacity;
    }
    UnicodeString buffer(dest, 0, destCapacity); // writable alias
    rep->extractBetween(start, limit, buffer);
    return u_terminateUChars(dest, destCapacity, length, status);
}

static int32_t U_CALLCONV
repTextReplace(UText *ut,
               int32_t start, int32_t limit,
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
    if(start<0 || start>limit || oldLength<limit) {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    // prepare
    UnicodeString buffer((UBool)(length<0), src, length); // read-only alias
    // replace
    rep->handleReplaceBetween(start, limit, buffer);
    // post-processing
    return rep->length()-oldLength;
    // never invalidate the chunk because we have a copy of the characters
}

static void U_CALLCONV
repTextCopy(UText *ut,
            int32_t start, int32_t limit,
            int32_t destIndex,
            UBool move,
            UErrorCode *status) {
    Replaceable *rep=(Replaceable *)ut->context;
    int32_t length=rep->length();

    if(U_FAILURE(*status)) {
        return;
    }
    if( start<0 || start>limit || length<limit ||
        destIndex<0 || length<destIndex ||
        (start<destIndex && destIndex<limit)
    ) {
        *status=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    if(move) {
        // move: copy to destIndex, then replace original with nothing
        int32_t segLength=limit-start;
        rep->copy(start, limit, destIndex);
        if(destIndex<start) {
            start+=segLength;
            limit+=segLength;
        }
        rep->handleReplaceBetween(start, limit, UnicodeString());
    } else {
        // copy
        rep->copy(start, limit, destIndex);
    }
    // never invalidate the chunk because we have a copy of the characters
}



U_DRAFT UText * U_EXPORT2
utext_openReplaceable(UText *ut, Replaceable *rep, UErrorCode *status) {
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

    ut->clone      = noopTextClone;
    ut->length     = repTextLength;
    ut->access     = repTextAccess;
    ut->extract    = repTextExtract;
    ut->replace    = repTextReplace;
    ut->copy       = repTextCopy;

    ut->context=rep;
    return ut;
}

U_CDECL_END








//------------------------------------------------------------------------------
//
//     UText implementation for UnicodeString (read/write) 
//
//         Use of UText data members:
//            context    pointer to UnicodeString
//
//------------------------------------------------------------------------------

U_CDECL_BEGIN

 /*
 * TODO: use a flag in UText to support readonly strings?
 *       -> omit UTEXT_PROVIDER_WRITABLE
 */

static UText * U_CALLCONV
unistrTextClone(UText * /* dest */, const UText * /*src*/, UBool /*deep*/, UErrorCode * /*status*/) {
// TODO:  fix this.
#if 0
    UText *t2=(UText *)uprv_malloc(sizeof(UText));
    if(t2!=NULL) {
        *t2=*t;
        t2->context=((const UnicodeString *)t->context)->clone();
        if(t2->context==NULL) {
            uprv_free(t2);
            t2=NULL;
        }
    }
    return t2;
#endif
    return NULL;
}

static int32_t U_CALLCONV
unistrTextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS)|
        I32_FLAG(UTEXT_PROVIDER_WRITABLE);
}

static int32_t U_CALLCONV
unistrTextLength(UText *t) {
    return ((const UnicodeString *)t->context)->length();
}


static UBool U_CALLCONV
unistrTextAccess(UText *ut, int32_t index, UBool  forward, UTextChunk *chunk) {
    const UnicodeString *us   = (const UnicodeString *)ut->context;
    int32_t length = us->length();

    if (chunk->nativeLimit != length) {
        // This chunk is not yet set up.  Do it now.
        chunk->contents        = us->getBuffer();
        chunk->length          = length;
        chunk->nativeStart     = 0;
        chunk->nativeLimit     = length;
        chunk->nonUTF16Indexes = FALSE;
    }
        
    // pin the requested index to the bounds of the string,
    //  and set current iteration position.
    if (index<0) {
        index = 0;
    } else if (index>length) {
        index = length;
    }
    chunk->offset = index;

    // Check whether request is at the start or end
    UBool retVal = (forward && index<length) || (!forward && index>0);
    return retVal; 
}



static int32_t U_CALLCONV
unistrTextExtract(UText *t,
                  int32_t start, int32_t limit,
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
    if(start<0 || start>limit || length<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    length=limit-start;
    if (destCapacity>0 && dest!=NULL) {
        int32_t trimmedLength = length;
        if(trimmedLength>destCapacity) {
            trimmedLength=destCapacity;
        }
        us->extract(start, trimmedLength, dest);
    }
    u_terminateUChars(dest, destCapacity, length, pErrorCode);
    return length;
}

static int32_t U_CALLCONV
unistrTextReplace(UText *t,
                  int32_t start, int32_t limit,
                  const UChar *src, int32_t length,
                  UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)t->context;
    int32_t oldLength;

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(src==NULL && length!=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    oldLength=us->length(); // will subtract from new length
    if(start<0 || start>limit || oldLength<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    // replace
    us->replace(start, limit-start, src, length);

    // TODO: update chunk, set our iteration position.
    return us->length()-oldLength;
}

static void U_CALLCONV
unistrTextCopy(UText *t,
               int32_t start, int32_t limit,
               int32_t destIndex,
               UBool move,
               UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)t->context;
    int32_t length=us->length();

    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if( start<0 || start>limit || length<limit ||
        destIndex<0 || length<destIndex ||
        (start<destIndex && destIndex<limit)
    ) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    if(move) {
        // move: copy to destIndex, then replace original with nothing
        int32_t segLength=limit-start;
        us->copy(start, limit, destIndex);
        if(destIndex<start) {
            start+=segLength;
        }
        us->replace(start, segLength, NULL, 0);
    } else {
        // copy
        us->copy(start, limit, destIndex);
    }
    // TODO: update chunk description, set iteration position.
}

U_CDECL_END


U_DRAFT UText * U_EXPORT2
utext_openUnicodeString(UText *ut, UnicodeString *s, UErrorCode *status) {
    ut = utext_setup(ut, 0, status);
    if (U_SUCCESS(*status)) {
        ut->clone      = unistrTextClone;
        ut->length     = unistrTextLength;
        ut->access     = unistrTextAccess;
        ut->extract    = unistrTextExtract;
        ut->replace    = unistrTextReplace;
        ut->copy       = unistrTextCopy;

        ut->context     = s;
        ut->providerProperties = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS)|
                                 I32_FLAG(UTEXT_PROVIDER_WRITABLE);
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
//------------------------------------------------------------------------------

U_CDECL_BEGIN


static UText * U_CALLCONV
ucstrTextClone(UText * /* dest */, const UText * /*src*/, UBool /*deep*/, UErrorCode * /*status*/) {
// TODO:  fix this.
    return NULL;
}


static int32_t U_CALLCONV
ucstrTextLength(UText *ut) {
    if (ut->a < 0) {
        // null terminated, we don't yet know the length.  Scan for it.
        //    Access is not convenient for doing this  
        //    because the current interation postion can't be changed.
        const UChar  *str = (const UChar *)ut->context;
        for (;;) {
            if (str[ut->chunk.nativeLimit] == 0) {
                break;
            }
            ut->chunk.nativeLimit++;
        }
        ut->a = ut->chunk.nativeLimit;
        ut->chunk.length = ut->chunk.nativeLimit;
        ut->providerProperties &= ~I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
    }
    return ut->a;
}


static UBool U_CALLCONV
ucstrTextAccess(UText *ut, int32_t index, UBool  forward, UTextChunk *chunk) {
    const UChar *str   = (const UChar *)ut->context;
        
    // pin the requested index to the bounds of the string,
    //  and set current iteration position.
    if (index<0) {
        index = 0;
    } else if (index < ut->chunk.nativeLimit) {
        // The request data is within the chunk as it is known so far.
        // There is nothing more that needs to be done within this access function.
    } else if (ut->a >= 0) {
        // We know the length of this string, and the user is requesting something
        // at or beyond the length.  Trim the requested index to the length.
            index = ut->a;
    } else {
        // Null terminated string, length not yet known.
        // Scan down another 32 UChars or to the requested index, whichever is further
        int scanLimit = ut->chunk.nativeLimit + 32;
        if (scanLimit <= index) {
            scanLimit = index+1;         // TODO:  beware int overflow
        }
        for (; ut->chunk.nativeLimit<scanLimit; ut->chunk.nativeLimit++) {
            if (str[ut->chunk.nativeLimit] == 0) {
                // We found the end of the string.  Remember it, trim the index to it,
                //  and bail out of here.
                ut->a = ut->chunk.nativeLimit;
                ut->chunk.length = ut->chunk.nativeLimit;
                if (index > ut->chunk.nativeLimit) {
                    index = ut->chunk.nativeLimit;
                }
                ut->providerProperties &= ~I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
                goto breakout;
            }
        }
        // We scanned through the next batch of UChars without finding the end.
        // The endpoint of a chunk must not be left in the middle of a surrogate pair.
        // If the current end is on a lead surrogate, back the end up by one.
        // It doesn't matter if the end char happens to be an unpaired surrogate,
        //    and it's simpler not to worry about it.
        if (U16_IS_LEAD(str[ut->chunk.nativeLimit-1])) {
            --ut->chunk.nativeLimit;
        }
    }
breakout:
    chunk->offset = index;

    // Check whether request is at the start or end
    UBool retVal = (forward && index<ut->chunk.nativeLimit) || (!forward && index>0);
    return retVal; 
}



static int32_t U_CALLCONV
ucstrTextExtract(UText *ut,
                  int32_t start, int32_t limit,
                  UChar *dest, int32_t destCapacity,
                  UErrorCode *pErrorCode) {


    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    const UChar *s=(const UChar *)ut->context;
    int32_t strLength=ut->a;
    int32_t si, di;

    // If text is null terminated and we haven't yet scanned down as far as the starting
    //   position of the extract, do it now.
    if (strLength<0 && limit>=ut->chunk.nativeLimit) {
        ucstrTextAccess(ut, start, TRUE, &ut->chunk);
    }

    // Raise an error if starting position is outside of the string.
    if(start<0 || start>limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    if (strLength >= 0 && limit > strLength) {
        // String length is known.  Trim requested limit to be no more than the length
        limit = strLength;
    }

    di = 0;
    for (si=start; si<limit; si++) {
        if (strLength<0 && s[si]==0) {
            // Just hit the end of a null-terminated string.
            ut->a = si;               // set string length for this UText
            ut->chunk.nativeLimit = si;
            ut->chunk.length      = si;
            // 
            break;
        }
        if (di<destCapacity) {
            // only store if there is space.
            dest[di] = s[si];
        } else {
            if (strLength>=0) {
                // We have filled the destination buffer, and the string is known.
                //  Cut the loop short.  There is no need to scan string termination.
                di = strLength;
                break;
            }
        }
        di++;
    }

    u_terminateUChars(dest, destCapacity, di, pErrorCode);
    return di;
                  }



U_CDECL_END


U_DRAFT UText * U_EXPORT2
utext_openUChars(UText *ut, const UChar *s, int32_t length, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return NULL;
    }
    if (length < -1) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    ut = utext_setup(ut, 0, status);
    if (U_SUCCESS(*status)) {
        ut->clone      = noopTextClone;
        ut->length     = ucstrTextLength;
        ut->access     = ucstrTextAccess;
        ut->extract    = ucstrTextExtract;
        ut->replace    = NULL;
        ut->copy       = NULL;

        ut->context               = s;
        ut->providerProperties    = I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
        if (length==-1) {
            ut->providerProperties |= I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE);
        }
        ut->a                     = length;
        ut->chunk.contents        = s;
        ut->chunk.nativeStart     = 0;
        ut->chunk.nativeLimit     = length>=0? length : 0;
        ut->chunk.nonUTF16Indexes = FALSE;
    }
    return ut;
}


