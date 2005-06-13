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
            if(ut->chunk.offset>=ut->chunk.length && !utext_access(ut, ut->chunk.limit, TRUE)) {
                retval = FALSE;
                break;
            }
            U16_FWD_1(ut->chunk.contents, ut->chunk.offset, ut->chunk.length);
        } while(--delta>0);
    } else if (delta<0) {
        do {
            if(ut->chunk.offset<=0 && !utext_access(ut, ut->chunk.start, FALSE)) {
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

U_DRAFT int32_t U_EXPORT2
utext_getIndex(UText *ut) {
    if(!ut->chunk.nonUTF16Indexes || ut->chunk.offset==0) {
        return ut->chunk.start+ut->chunk.offset;
    } else {
        return ut->mapOffsetToNative(ut, &ut->chunk, ut->chunk.offset);
    }
}



U_DRAFT void U_EXPORT2
utext_setIndex(UText *ut, int32_t index) {
    // TODO - revise for keeping index always valid.
    if(index<ut->chunk.start || ut->chunk.limit<index) {
        // The desired position is outside of the current chunk.  Invalidate it and
        // leave it to next32() or previous32() to access the text
        // in the desired direction.
        ut->access(ut, index, TRUE, &ut->chunk);
    } else if(ut->chunk.nonUTF16Indexes) {
        ut->chunk.offset=ut->mapIndexToUTF16(ut, &ut->chunk, index);
    } else {
        ut->chunk.offset=index-ut->chunk.start;
        // Our convention is that the index must always be on a code point boundary.
        //  If we are somewhere in the middle of a utf-16 buffer, check that new index
        //  is not in the middle of a surrogate pair.
        if (index>ut->chunk.start && index < ut->chunk.limit) {   // TODO:  clean up end-of-chunk / end of input handling.  Everywhere.
            UChar c = ut->chunk.contents[ut->chunk.offset];
            if (U16_TRAIL(c)) {
                utext_current(ut);  // force index onto a code point boundary.
            }
        }
    }
}



  
U_DRAFT UChar32 U_EXPORT2
utext_current(UText *ut) {
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
utext_next32(UText *ut) {
    UTextChunk   *chunk  = &ut->chunk;
    int32_t       offset = chunk->offset;
    UChar32       c      = U_SENTINEL;

    if (offset >= chunk->length) {
        if (ut->access(ut, chunk->limit, TRUE, chunk) == FALSE) {
            goto next32_return;
        }
        offset = chunk->offset;
    }
            
    c = chunk->contents[offset++];
    if (U16_IS_SURROGATE(c)) {
        // looking at a surrogate.  Could be unpaired, need to be careful.
        // Speed doesn't matter, will be very rare.
        c =  utext_current(ut);
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
        if (ut->access(ut, chunk->start, FALSE, chunk) == FALSE) {
            goto prev32_return;
        }
        offset = chunk->offset;
    }
            
    c = chunk->contents[--offset];
    chunk->offset = offset;
    if (U16_IS_SURROGATE(c)) {
        // Note that utext_current() will move the chunk offset to the lead surrogate
        // if we come in referring to trail half of a surrogate pair.
        c =  utext_current(ut);
    } 

prev32_return:
    return c;
}



U_DRAFT UChar32 U_EXPORT2
utext_next32From(UText *ut, int32_t index) {
    int32_t       offset;   // index into the chunk buffer containing the desired char.
    UTextChunk   *chunk  = &ut->chunk;
    UChar32       c      = U_SENTINEL;

    if(index<chunk->start || index>=chunk->limit) {
        if(!ut->access(ut, index, TRUE, chunk)) {
            // no chunk available here
            goto next32return;
        }
        offset = chunk->offset;
    } else if(chunk->nonUTF16Indexes) {
        offset=ut->mapIndexToUTF16(ut, chunk, index);
    } else {
        offset = index - chunk->start;
    }

    c = chunk->contents[offset++];
    if (U16_IS_SURROGATE(c)) {
        // Surrogate code unit.  Could be pointing at either half of a pair, or at
        //   an unpaired surrogate.  Let utext_current() do the work.  Speed doesn't matter.
        chunk->offset = offset;
        c = utext_current(ut);  
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

    if(index<=chunk->start || index>chunk->limit) {
        if(!ut->access(ut, index, FALSE, chunk)) {
            // no chunk available here
            goto prev32return;
        }
        offset = chunk->offset;
    } else if(chunk->nonUTF16Indexes) {
        offset=ut->mapIndexToUTF16(ut, chunk, index);
    } else {
        offset = index - chunk->start;
    }

    offset--;
    c = chunk->contents[offset];
    chunk->offset = offset;
    if (U16_IS_SURROGATE(c)) {
        c = utext_current(ut);  // get supplementary char if not unpaired surrogate,
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

        if(!ut->access(ut, ut->chunk.limit, TRUE, &ut->chunk)) {
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
//
struct ExtendedUText: public UText {
    void  *extension;
};

static const UText emptyText = UTEXT_INITIALIZER

U_DRAFT UText * U_EXPORT2
utext_setup(UText *ut, int32_t extraSpace, UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return ut;
    }

    if (ut == NULL) {
        // We need to heap-allocate storage for the new UText
        int32_t spaceRequired = sizeof(UText);
        if (extraSpace > 0) {
            spaceRequired = sizeof(ExtendedUText) + extraSpace - sizeof(void *);
        }
        ut = (UText *)uprv_malloc(spaceRequired);
        *ut = emptyText;
        ut->flags |= UTEXT_HEAP_ALLOCATED;
        if (spaceRequired>0) {
            ut->extraSize = spaceRequired;
            ut->pExtra    = &((ExtendedUText *)ut)->extension;
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
    if (index==chunk->limit) {
        chunk->offset = chunk->length;
    } else if (index==chunk->start) {
        chunk->offset = 0;
    } else {
        chunk->length  = 0;
        chunk->start   = index;
        chunk->limit   = index;
        chunk->offset  = 0;
    } 
}

        


//------------------------------------------------------------------------------
//
// No-Op UText implementation for illegal input 
//
//------------------------------------------------------------------------------
U_CDECL_BEGIN

static UText * U_CALLCONV
noopTextClone(UText * /* dest */, const UText * /*src*/, UBool /*deep*/, UErrorCode * /*status*/) {
    return NULL; // not supported
}

static int32_t U_CALLCONV
noopTextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE)|
        I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
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
noopTextMapOffsetToNative(UText * /* t */, UTextChunk * /* chunk */, int32_t /* offset */) {
    return 0;
}

static int32_t U_CALLCONV
noopTextMapIndexToUTF16(UText * /* t */, UTextChunk * /* chunk */, int32_t /* index */) {
    return 0;
}

U_CDECL_END


static const UText noopText={
    UTEXT_INITIALZIER_HEAD,
    noopTextClone,
    noopTextGetProperties,
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
utf8TextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_NON_UTF16_INDEXES)|
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE);
        // not UTEXT_PROVIDER_STABLE_CHUNKS because chunk-related data is kept
        // in UTF8Text, so only one at a time can be active
}

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

        chunk->start=index;
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
        chunk->contents = u16buf;
        chunk->length   = i;
        chunk->limit    = index;
        ut->q           = map;  
        chunk->offset   = 0;      // chunkOffset corresponding to index
        return TRUE; 
    } else {
        // Reverse Access.  The chunk buffer must be filled so as to contain the
        //                  character preceding the specified index.
        if(index<=0) {
            resetChunk(chunk, 0);
            return FALSE;
        }

        chunk->limit=index;
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
        chunk->contents = u16buf+i;
        chunk->length   = (UTF8_TEXT_CHUNK_SIZE)-i;
        chunk->start    = index;
        chunk->offset   = chunk->length; // chunkOffset corresponding to index
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
utf8TextMapOffsetToNative(UText *ut, UTextChunk * /* chunk */, int32_t offset) {
    // UText.q points to the index mapping array that is allocated in the extra storage area.
    int32_t *map=(int32_t *)(ut->q);
    return map[offset];
}

// Assume nonUTF16Indexes and chunk->start<=index<=chunk->limit
static int32_t U_CALLCONV
utf8TextMapIndexToUTF16(UText *ut, UTextChunk * /*chunk */, int32_t index) {
    int32_t *map=(int32_t *)(ut->q);
    int32_t offset=0;

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

    ut->clone      = noopTextClone;
    ut->properties = utf8TextGetProperties;
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




//------------------------------------------------------------------------------
//
//     UText implementation for SBCS strings (read-only) 
//
//         Use of UText data members:
//            context    pointer to SBCS string
//
//------------------------------------------------------------------------------


enum { SBCS_TEXT_CHUNK_SIZE=10 };

struct SBCSText : public UText {
    /* pointer to SBCS-to-BMP mapping table */
    const UChar *toU;
    /* length of UTF-8 string (in bytes) */
    int32_t length;
    /* chunk UChars */
    UChar s[SBCS_TEXT_CHUNK_SIZE];
};


U_CDECL_BEGIN

static int32_t U_CALLCONV
sbcsTextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE);
        // not UTEXT_PROVIDER_STABLE_CHUNKS because chunk-related data is kept
        // in SBCSText, so only one at a time can be active
}

static int32_t U_CALLCONV
sbcsTextLength(UText *t) {
    return ((SBCSText *)t)->length;
}

static UBool U_CALLCONV
sbcsTextAccess(UText *ut, int32_t index, UBool forward, UTextChunk *chunk) {
    SBCSText *ts=(SBCSText *)ut;
    const uint8_t *s8=(const uint8_t *)ts->context;
    int32_t i, count, length=ts->length;

    chunk->nonUTF16Indexes=FALSE;

    if(forward) {
        if(length<=index) {
            resetChunk(chunk, length);
            return FALSE;
        }

        count=length-index;
        if(count>SBCS_TEXT_CHUNK_SIZE) {
            count=SBCS_TEXT_CHUNK_SIZE;
        }
        chunk->start=index;
        for(i=0; i<count; ++index, ++i) {
            ts->s[i]=ts->toU[s8[index]];
        }
        chunk->contents=ts->s;
        chunk->length=i;
        chunk->limit=index;
        chunk->offset = 0;   // chunkOffset corresponding to index
        return TRUE; 
    } else {
        if(index<=0) {
            resetChunk(chunk, 0);
            return FALSE;
        }

        if(index<=SBCS_TEXT_CHUNK_SIZE) {
            count=index;
        } else {
            count=SBCS_TEXT_CHUNK_SIZE;
        }
        chunk->limit=index;
        for(i=count; i>0;) {
            ts->s[--i]=ts->toU[s8[--index]];
        }
        chunk->contents=ts->s;
        chunk->length=count;
        chunk->start=index;
        chunk->offset=count;    // chunkOffset corresponding to index
        return TRUE; 
    }
}

static int32_t U_CALLCONV
sbcsTextExtract(UText *t,
                int32_t start, int32_t limit,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    SBCSText *ts=(SBCSText *)t;
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start<0 || start>limit || ts->length<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    const uint8_t *s8=(const uint8_t *)ts->context+start;
    UChar *d=dest;
    const UChar *destLimit;
    int32_t destLength=limit-start;
    if(destLength>destCapacity) {
        destLength=destCapacity;
    }
    destLimit=dest+destLength;
    while(d<destLimit) {
        *d++=ts->toU[*s8++];
    }
    return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
}

static const UText sbcsText={
    UTEXT_INITIALZIER_HEAD,
    noopTextClone,
    sbcsTextGetProperties,
    sbcsTextLength,
    sbcsTextAccess,
    sbcsTextExtract,
    NULL, // replace
    NULL, // copy
    NULL, // mapOffsetToNative
    NULL, // mapIndexToUTF16
    NULL  // close
};

U_DRAFT UText * U_EXPORT2
utext_openSBCS(UText * /*ut */,
               const UChar /* toU*/[256] ,
               const char *s, int32_t length,
               UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(s==NULL || length<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    SBCSText *ts=(SBCSText *)uprv_malloc(sizeof(SBCSText));
    if(ts==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    *((UText *)ts)=sbcsText;
    ts->context=s;
    if(length>=0) {
        ts->length=length;
    } else {
        ts->length=(int32_t)uprv_strlen(s);
    }
    return ts;
}

U_DRAFT void U_EXPORT2
utext_closeSBCS(UText *t) {
    if(t!=NULL) {
        uprv_free((SBCSText *)t);
    }
}

U_DRAFT void U_EXPORT2
utext_resetSBCS(UText *t, const char *s, int32_t length, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if(s==NULL || length<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    SBCSText *ts=(SBCSText *)t;
    ts->context=s;
    if(length>=0) {
        ts->length=length;
    } else {
        ts->length=(int32_t)uprv_strlen(s);
    }
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

#if 0 // initially commented out to reduce testing

 /*
 * TODO: use a flag in RepText to support readonly strings?
 *       -> omit UTEXT_PROVIDER_WRITABLE
 */

// minimum chunk size for this implementation: 3
// to allow for possible trimming for code point boundaries
enum { REP_TEXT_CHUNK_SIZE=10 };

struct RepText : public UText {
    /* chunk UChars */
    UChar s[REP_TEXT_CHUNK_SIZE];
};

U_CDECL_BEGIN

static UText * U_CALLCONV
repTextClone(const UText *t) {
    RepText *t2=(RepText *)uprv_malloc(sizeof(RepText));
    if(t2!=NULL) {
        *t2=*(const RepText *)t;
        t2->context=((const Replaceable *)t->context)->clone();
        if(t2->context==NULL) {
            uprv_free(t2);
            t2=NULL;
        }
    }
    return t2;
}

static int32_t U_CALLCONV
repTextGetProperties(UText *t) {
    int32_t props=I32_FLAG(UTEXT_PROVIDER_WRITABLE);
    if(((const Replaceable *)((const RepText *)t)->context)->hasMetaData()) {
        props|=I32_FLAG(UTEXT_PROVIDER_HAS_META_DATA);
    }
    return props;
}

static int32_t U_CALLCONV
repTextLength(UText *t) {
    return ((const Replaceable *)((const RepText *)t)->context)->length();
}

static int32_t U_CALLCONV
repTextAccess(UText *t, int32_t index, UBool forward, UTextChunk *chunk) {
    RepText *rt=(RepText *)t;
    const Replaceable *rep=(const Replaceable *)rt->context;
    int32_t start, limit, length=rep->length();
    int32_t chunkStart, chunkLength, chunkOffset;

    /*
     * Compute start/limit boundaries around index, for a segment of text
     * to be extracted.
     * The segment will be trimmed to not include halves of surrogate pairs.
     */
    if(forward) {
        if(length<=index) {
            return -1;
        }
        limit=index+REP_TEXT_CHUNK_SIZE-1;
        if(limit>length) {
            limit=length;
        }
        start=limit-REP_TEXT_CHUNK_SIZE;
        if(start<0) {
            start=0;
        }
    } else {
        if(index<0) {
            return -1;
        }
        start=index-REP_TEXT_CHUNK_SIZE+1;
        if(start<0) {
            start=0;
        }
        limit=start+REP_TEXT_CHUNK_SIZE;
        if(length<limit) {
            limit=length;
        }
    }
    UnicodeString buffer(rt->s, 0, REP_TEXT_CHUNK_SIZE); // writable alias
    rep->extractBetween(start, limit, buffer);

    chunkStart=0;
    chunkLength=limit-start;
    chunkOffset=index-start;

    // trim contents for code point boundaries
    if(0<start && U16_IS_TRAIL(rt->s[chunkStart])) {
        ++chunkStart;
        --chunkLength;
        ++start;
    }
    if(limit<length && U16_IS_LEAD(rt->s[chunkStart+chunkLength-1])) {
        --chunkLength;
        --limit;
    }

    // adjust the index/chunkOffset to a code point boundary
    U16_SET_CP_START(rt->s, chunkStart, chunkOffset);

    chunk->contents=rt->s+chunkStart;
    chunk->length=chunkLength;
    chunk->start=start;
    chunk->limit=limit;
    chunk->nonUTF16Indexes=FALSE;
    return chunkOffset; // chunkOffset corresponding to index
}

static int32_t U_CALLCONV
repTextExtract(UText *t,
               int32_t start, int32_t limit,
               UChar *dest, int32_t destCapacity,
               UErrorCode *pErrorCode) {
    RepText *rt=(RepText *)t;
    const Replaceable *rep=(const Replaceable *)rt->context;
    int32_t length=rep->length();

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
    if(length>destCapacity) {
        length=destCapacity;
    }
    UnicodeString buffer(dest, 0, destCapacity); // writable alias
    rep->extractBetween(start, limit, buffer);
    return u_terminateUChars(dest, destCapacity, length, pErrorCode);
}

static int32_t U_CALLCONV
repTextReplace(UText *t,
               int32_t start, int32_t limit,
               const UChar *src, int32_t length,
               UTextChunk *chunk,
               UErrorCode *pErrorCode) {
    RepText *rt=(RepText *)t;
    Replaceable *rep=(Replaceable *)rt->context;
    int32_t oldLength;

    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(src==NULL && length!=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    oldLength=rep->length(); // will subtract from new length
    if(start<0 || start>limit || oldLength<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
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
repTextCopy(UText *t,
            int32_t start, int32_t limit,
            int32_t destIndex,
            UBool move,
            UTextChunk *chunk,
            UErrorCode *pErrorCode) {
    RepText *rt=(RepText *)t;
    Replaceable *rep=(Replaceable *)rt->context;
    int32_t length=rep->length();

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

static const UText repText={
    NULL, NULL, NULL, NULL,
    (int32_t)sizeof(UText), 0, 0, 0,
    repTextClone,
    repTextGetProperties,
    repTextLength,
    repTextAccess,
    repTextExtract,
    repTextReplace,
    repTextCopy,
    NULL, // mapOffsetToNative
    NULL  // mapIndexToUTF16
};

U_DRAFT UText * U_EXPORT2
utext_openReplaceable(Replaceable *rep, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(rep==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    RepText *rt=(RepText *)uprv_malloc(sizeof(RepText));
    if(rt==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    *((UText *)rt)=repText;
    rt->context=rep;
    return rt;
}

U_DRAFT void U_EXPORT2
utext_closeReplaceable(UText *t) {
    if(t!=NULL) {
        uprv_free((RepText *)t);
    }
}

U_DRAFT void U_EXPORT2
utext_resetReplaceable(UText *t, Replaceable *rep, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if(rep==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    RepText *rt=(RepText *)t;
    rt->context=rep;
}
U_CDECL_END

#endif







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
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE)|
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

    if (chunk->limit != length) {
        // This chunk is not yet set up.  Do it now.
        chunk->contents=us->getBuffer();
        chunk->length=length;
        chunk->start=0;
        chunk->limit=length;
        chunk->nonUTF16Indexes=FALSE;
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
        ut->properties = unistrTextGetProperties;
        ut->length     = unistrTextLength;
        ut->access     = unistrTextAccess;
        ut->extract    = unistrTextExtract;
        ut->replace    = unistrTextReplace;
        ut->copy       = unistrTextCopy;

        ut->context     = s;
    }
    return ut;
}


