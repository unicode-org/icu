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

U_NAMESPACE_BEGIN

#define I32_FLAG(bitIndex) ((int32_t)1<<(bitIndex))

/*---------------------------------------------------------------------------
 *
 * UTextIterator implementation
 *
 * ---------------------------------------------------------------------------*/

UTextIterator::UTextIterator(UText *text) {
    t=text;
    chunk.sizeOfStruct=(uint16_t)sizeof(UTextChunk);
    chunk.padding=0;
    setChunkInvalid(0);
    providerProperties=t->properties(t);
}



//
//   setChunkInvalid()   This is called when the iterator position is set outside
//                       of the current range of the chunk.  The index position is
//                       kept, but chunk contents are set such that an attempt to
//                       access data will fail.
void
UTextIterator::setChunkInvalid(int32_t index) {
    chunk.contents=NULL;
    chunk.length=chunkOffset=0;
    chunk.start=chunk.limit=index;
    chunk.nonUTF16Indexes=FALSE;
}



UBool
UTextIterator::access(int32_t index, UBool forward) {
    chunkOffset=t->access(t, index, forward, &chunk);
    if(chunkOffset>=0) {
        return TRUE;
    } else {
        // no chunk available here
        //  TODO:  Possibly cleaner end-of-string bail-out.  
        setChunkInvalid(index);
        return FALSE;
    }
}



UBool
UTextIterator::moveIndex(int32_t delta) {
    UBool retval = TRUE;
    if(delta>0) {
        do {
            if(chunkOffset>=chunk.length && !access(chunk.limit, TRUE)) {
                retval = FALSE;
                break;
            }
            U16_FWD_1(chunk.contents, chunkOffset, chunk.length);
        } while(--delta>0);
    } else if (delta<0) {
        do {
            if(chunkOffset<=chunk.start && !access(chunk.start, FALSE)) {
                retval = FALSE;
                break;
            }
            U16_BACK_1(chunk.contents, chunk.start, chunkOffset);
        } while(++delta<0);
    } else {
        // Delta == 0.
        // Need to trim current postion to be within the bounds of the text.
        if (chunkOffset>=0 && chunkOffset<chunk.limit) {
            // Current position is within the current chunk.
            // No action needed.
        } else if (chunk.start<=0) {
            // Current position is <= 0, and outside of the current chunk.
            //   can only get negative if someone did a setIndex(negative value).
            //   Trim position back to zero.
            setChunkInvalid(0);
        } else {
            // Current postion is past the current chunk bounds.
            // Force trim to length of text by doing a text access.
            access(chunk.limit, FALSE);
        }
    }
    return retval;
}


int32_t
UTextIterator::length() {
    return t->length(t);
}



UChar32  
UTextIterator::getSupplementary() {
    UChar32  c;
    U16_GET(chunk.contents, 0, chunkOffset, chunk.length, c);
    if (U16_IS_TRAIL(chunk.contents[chunkOffset]) && U_IS_SUPPLEMENTARY(c)) {
        // Incoming position pointed to the trailing supplementary pair.
        // Move ourselves back to the lead.
        chunkOffset--;
    }
    return c;
}


UBool
UTextIterator::compare(const UChar *s, int32_t length, UBool codePointOrder) {
    int32_t segLength, result;

    if(length<0) {
        length=u_strlen(s);
    }
    if(length==0) {
        return 0;
    }
    for(;;) {
        // compare starting from the current position in the current chunk
        segLength=chunk.length-chunkOffset;
        if(segLength>length) {
            segLength=length;
        }
        result=u_strCompare(
            chunk.contents+chunkOffset, segLength,
            s, length,
            codePointOrder);
        chunkOffset+=segLength;
        if(result!=0) {
            return result;
        }

        // compare the next chunk
        s+=segLength;
        length-=segLength;
        if(length==0) {
            return 0;
        }

        if(!access(chunk.limit, TRUE)) {
            // the text ends before the string does
            return -1;
        }
    }
    return 0;
}
U_NAMESPACE_END




/* No-Op UText implementation for illegal input ----------------------------- */

static UText * U_CALLCONV
noopTextClone(const UText *t) {
    return NULL; // not supported
}

static int32_t U_CALLCONV
noopTextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE)|
        I32_FLAG(UTEXT_PROVIDER_STABLE_CHUNKS);
}

static int32_t U_CALLCONV
noopTextLength(UText *t) {
    return 0;
}

static int32_t U_CALLCONV
noopTextAccess(UText *t, int32_t index, UBool forward, UTextChunk *chunk) {
    return -1;
}

static int32_t U_CALLCONV
noopTextExtract(UText *t,
                int32_t start, int32_t limit,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    return 0;
}

static int32_t U_CALLCONV
noopTextMapOffsetToNative(UText *t, UTextChunk *chunk, int32_t offset) {
    return 0;
}

static int32_t U_CALLCONV
noopTextMapIndexToUTF16(UText *t, UTextChunk *chunk, int32_t index) {
    return 0;
}

static const UText noopText={
    NULL, NULL, NULL, NULL,
    (int32_t)sizeof(UText), 0, 0, 0,
    noopTextClone,
    noopTextGetProperties,
    noopTextLength,
    noopTextAccess,
    noopTextExtract,
    NULL, // replace
    NULL, // copy
    noopTextMapOffsetToNative,
    noopTextMapIndexToUTF16
};



//------------------------------------------------------------------------------
//
//     UText implementation for UTF-8 strings (read-only) 
//
//         Use of UText data members:
//            context    pointer to UTF-8 string
//
//------------------------------------------------------------------------------

enum { UTF8_TEXT_CHUNK_SIZE=10 };

struct UTF8Text : public UText {
    /* length of UTF-8 string (in bytes) */
    int32_t length;
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
    /* points into map[] corresponding to where chunk.contents starts in s[] */
    int32_t *chunkMap;
};

static int32_t U_CALLCONV
utf8TextGetProperties(UText * /*t*/) {
    return
        I32_FLAG(UTEXT_PROVIDER_NON_UTF16_INDEXES)|
        I32_FLAG(UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE);
        // not UTEXT_PROVIDER_STABLE_CHUNKS because chunk-related data is kept
        // in UTF8Text, so only one at a time can be active
}

static int32_t U_CALLCONV
utf8TextLength(UText *t) {
    return ((UTF8Text *)t)->length;
}

static int32_t U_CALLCONV
utf8TextAccess(UText *t, int32_t index, UBool forward, UTextChunk *chunk) {
    UTF8Text *t8=(UTF8Text *)t;
    const uint8_t *s8=(const uint8_t *)t8->context;
    UChar32 c;
    int32_t i, length=t8->length;

    if(forward) {
        if(length<=index) {
            return -1;
        }

        chunk->start=index;
        c=s8[index];
        if(c<=0x7f) {
            // get a chunk of ASCII characters
            t8->s[0]=(UChar)c;
            for(i=1, ++index;
                i<UTF8_TEXT_CHUNK_SIZE && index<length && (c=s8[index])<=0x7f;
                ++i, ++index
            ) {
                t8->s[i]=(UChar)c;
            }
            chunk->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            U8_SET_CP_START(s8, 0, index);
            for(i=0;
                i<UTF8_TEXT_CHUNK_SIZE && index<length;
                ++i
            ) {
                t8->map[i]=index;
                t8->map[i+1]=index; // in case there is a trail surrogate
                U8_NEXT(s8, index, length, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                U16_APPEND_UNSAFE(t8->s, i, c);
            }
            t8->map[i]=index;
            t8->chunkMap=t8->map;
            chunk->nonUTF16Indexes=TRUE;
        }
        chunk->contents=t8->s;
        chunk->length=i;
        chunk->limit=index;
        return 0; // chunkOffset corresponding to index
    } else {
        if(index<=0) {
            return -1;
        }

        chunk->limit=index;
        c=s8[index-1];
        if(c<=0x7f) {
            // get a chunk of ASCII characters
            i=UTF8_TEXT_CHUNK_SIZE;
            t8->map[i]=index;
            do {
                t8->s[--i]=(UChar)c;
                --index;
            } while(i>0 && index>0 && (c=s8[index-1])<=0x7f);
            chunk->nonUTF16Indexes=FALSE;
        } else {
            // get a chunk of characters starting with a non-ASCII one
            if(index<length) {
                U8_SET_CP_START(s8, 0, index);
            }
            i=UTF8_TEXT_CHUNK_SIZE+1;
            t8->map[i]=index;
            do {
                U8_PREV(s8, 0, index, c);
                if(c<0) {
                    c=0xfffd; // use SUB for illegal sequences
                }
                if(c<=0xffff) {
                    t8->s[--i]=(UChar)c;
                    t8->map[i]=index;
                } else {
                    t8->s[--i]=U16_TRAIL(c);
                    t8->map[i]=index;
                    t8->s[--i]=U16_LEAD(c);
                    t8->map[i]=index;
                }
            } while(i>1 && index>0);
            t8->chunkMap=t8->map+i;
            chunk->nonUTF16Indexes=TRUE;
        }
        chunk->contents=t8->s+i;
        chunk->length=(UTF8_TEXT_CHUNK_SIZE+1)-i;
        chunk->start=index;
        return chunk->length; // chunkOffset corresponding to index
    }
}

static int32_t U_CALLCONV
utf8TextExtract(UText *t,
                int32_t start, int32_t limit,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    UTF8Text *t8=(UTF8Text *)t;
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
    if(start<0 || start>limit || t8->length<limit) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }
    int32_t destLength=0;
    u_strFromUTF8(dest, destCapacity, &destLength,
                    (const char *)t8->context+start, limit-start,
                    pErrorCode);
    return destLength;
    // TODO: if U_INVALID|ILLEGAL_CHAR_FOUND, extract text anyway and use SUB for illegal sequences?
}

// Assume nonUTF16Indexes and 0<=offset<=chunk->length
static int32_t U_CALLCONV
utf8TextMapOffsetToNative(UText *t, UTextChunk *chunk, int32_t offset) {
    UTF8Text *t8=(UTF8Text *)t;
    return t8->chunkMap[offset];
}

// Assume nonUTF16Indexes and chunk->start<=index<=chunk->limit
static int32_t U_CALLCONV
utf8TextMapIndexToUTF16(UText *t, UTextChunk *chunk, int32_t index) {
    UTF8Text *t8=(UTF8Text *)t;
    int32_t *map=t8->chunkMap;
    int32_t offset=0;

    while(index>map[offset]) {
        ++offset;
    }
    return offset;
}

static const UText utf8Text={
    NULL, NULL, NULL, NULL,
    (int32_t)sizeof(UText), 0, 0, 0,
    noopTextClone,
    utf8TextGetProperties,
    utf8TextLength,
    utf8TextAccess,
    utf8TextExtract,
    NULL, // replace
    NULL, // copy
    utf8TextMapOffsetToNative,
    utf8TextMapIndexToUTF16
};

U_DRAFT UText * U_EXPORT2
utext_openUTF8(const uint8_t *s, int32_t length, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(s==NULL || length<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    UTF8Text *t8=(UTF8Text *)uprv_malloc(sizeof(UTF8Text));
    if(t8==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    *((UText *)t8)=utf8Text;
    t8->context=s;
    if(length>=0) {
        t8->length=length;
    } else {
        t8->length=(int32_t)uprv_strlen((const char *)s);
    }
    return t8;
}

U_DRAFT void U_EXPORT2
utext_closeUTF8(UText *t) {
    if(t!=NULL) {
        uprv_free((UTF8Text *)t);
    }
}

U_DRAFT void U_EXPORT2
utext_resetUTF8(UText *t, const uint8_t *s, int32_t length, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    if(s==NULL || length<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    UTF8Text *t8=(UTF8Text *)t;
    t8->context=s;
    if(length>=0) {
        t8->length=length;
    } else {
        t8->length=(int32_t)uprv_strlen((const char *)s);
    }
}




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

static int32_t U_CALLCONV
sbcsTextAccess(UText *t, int32_t index, UBool forward, UTextChunk *chunk) {
    SBCSText *ts=(SBCSText *)t;
    const uint8_t *s8=(const uint8_t *)ts->context;
    int32_t i, count, length=ts->length;

    chunk->nonUTF16Indexes=FALSE;
    if(forward) {
        if(length<=index) {
            return -1;
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
        return 0; // chunkOffset corresponding to index
    } else {
        if(index<=0) {
            return -1;
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
        return count; // chunkOffset corresponding to index
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
    NULL, NULL, NULL, NULL,
    (int32_t)sizeof(UText), 0, 0, 0,
    noopTextClone,
    sbcsTextGetProperties,
    sbcsTextLength,
    sbcsTextAccess,
    sbcsTextExtract,
    NULL, // replace
    NULL, // copy
    NULL, // mapOffsetToNative
    NULL  // mapIndexToUTF16
};

U_DRAFT UText * U_EXPORT2
utext_openSBCS(const UChar toU[256],
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

#endif







//------------------------------------------------------------------------------
//
//     UText implementation for UnicodeString (read/write) 
//
//         Use of UText data members:
//            context    pointer to UnicodeString
//
//------------------------------------------------------------------------------

 /*
 * TODO: use a flag in UText to support readonly strings?
 *       -> omit UTEXT_PROVIDER_WRITABLE
 */

static UText * U_CALLCONV
unistrTextClone(const UText *t) {
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

static int32_t U_CALLCONV
unistrTextAccess(UText *t, int32_t index, UBool  forward, UTextChunk *chunk) {
    const UnicodeString *us=(const UnicodeString *)t->context;
    int32_t length=us->length();

    if (forward) {
        if (index<0 || index>=length) {
            // Forward iteration.  Character after index position must exist.
            return -1;
        }
    } else {
        if (index<=0 || index>length) {
            // Reverse iteration.  Character before index position must exist.
            return -1;
        }
    }

    chunk->contents=us->getBuffer();
    chunk->length=length;
    chunk->start=0;
    chunk->limit=length;
    chunk->nonUTF16Indexes=FALSE;
    return index; // chunkOffset corresponding to index
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
    if(length>destCapacity) {
        length=destCapacity;
    }
    us->extract(start, length, dest);
    return u_terminateUChars(dest, destCapacity, length, pErrorCode);
}

static int32_t U_CALLCONV
unistrTextReplace(UText *t,
                  int32_t start, int32_t limit,
                  const UChar *src, int32_t length,
                  UTextChunk *chunk,
                  UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)t->context;
    const UChar *oldBuffer;
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
    // prepare
    if(chunk!=NULL) {
        oldBuffer=us->getBuffer(); // for chunk invalidation
    }
    // replace
    us->replace(start, limit-start, src, length);
    // post-processing
    if(chunk!=NULL && oldBuffer!=us->getBuffer()) {
        chunk->contents=NULL;
    }
    return us->length()-oldLength;
}

static void U_CALLCONV
unistrTextCopy(UText *t,
               int32_t start, int32_t limit,
               int32_t destIndex,
               UBool move,
               UTextChunk *chunk,
               UErrorCode *pErrorCode) {
    UnicodeString *us=(UnicodeString *)t->context;
    const UChar *oldBuffer;
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
    if(chunk!=NULL) {
        oldBuffer=us->getBuffer(); // for chunk invalidation
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
    if(chunk!=NULL && oldBuffer!=us->getBuffer()) {
        chunk->contents=NULL;
    }
};

//
//  Statically initialized utext object, pre-setup
//   for UnicodeStrings.
//  
static const UText unistrText={
    NULL, NULL, NULL, NULL,
    (int32_t)sizeof(UText), 0, 0, 0,
    unistrTextClone,
    unistrTextGetProperties,
    unistrTextLength,
    unistrTextAccess,
    unistrTextExtract,
    unistrTextReplace,
    unistrTextCopy,
    NULL, // mapOffsetToNative
    NULL  // mapIndexToUTF16
};

U_DRAFT void U_EXPORT2
utext_setUnicodeString(UText *t, UnicodeString *s) {
    *t=unistrText;
    t->context=s;
}


