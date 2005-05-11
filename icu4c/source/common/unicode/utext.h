/*
*******************************************************************************
*
*   Copyright (C) 2004-2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utext.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004oct06
*   created by: Markus W. Scherer
*/

#ifndef __UTEXT_H__
#define __UTEXT_H__

/**
 * \file
 * \brief C API: Abstract Unicode Text API
 *
 * TBD
 *
 * Text chunks must begin and end on Unicode code point boundaries.
 * That is, a chunk boundary must not fall between a leading and a trailing
 * surrogate.
 *
 * If an input index points not at a code point boundary, then the API behaves as if
 * the index is first adjusted to the immediately preceding code point boundary.
 *
 * Valid indexes into the text must begin at 0 (start of the text) and
 * must strictly increase going forward through the text.
 * (No reordering, and valid indexes must be non-negative.)
 *
 * Issues:
 * - Code point boundary adjustment if index points not already to a boundary
 *   + Currently, see above: always adjusts to the immediately preceding code point boundary.
 *     (For example, using U8_SET_CP_START().)
 *   + Alternatively: Could adjust to preceding boundary when going forward (U8_SET_CP_START())
 *     and to the following boundary when going backward (U8_SET_CP_LIMIT()).
 *     Result: next32From(index) and previous32From(index) would return the same
 *     character.
 * - Error handling - add UErrorCode parameters? Add UBool return values to void functions?
 *   + Add UErrorCode to extract()?
 * - This version does not expose NUL-termination to the caller.
 * - This version assumes option 2 (index mapping done by provider functions).
 * - This version uses one API for read-only as well as read-write access,
 *   with a way to find out whether the text object is writable or not.
 * - This version does not support absolute UTF-16 indexes when native indexes are used.
 * - Should the copy() function have a UBool for whether to copy or move the text?
 * - replace() needs a way to indicate that the current chunk
 *   (which would need to be passed in) became invalid during the operation.
 *   Same for copy().
 *
 * @see UText
 */

#include "unicode/utypes.h"
#include "unicode/rep.h"
#include "unicode/unistr.h"

#ifndef U_HIDE_DRAFT_API

U_CDECL_BEGIN

struct UText;
typedef struct UText UText; /**< C typedef for struct UText. @draft ICU 3.4 */

struct UTextChunk;
typedef struct UTextChunk UTextChunk; /**< C typedef for struct UTextChunk. @draft ICU 3.4 */

struct UTextChunk {
    /** Pointer to contents of text chunk. */
    const UChar *contents;
    /** Number of UChars in the chunk. */
    int32_t length;
    /** (Native) text index corresponding to the start of the chunk. */
    int32_t start;
    /** (Native) text index corresponding to the end of the chunk (contents+length). */
    int32_t limit;
    /** If TRUE, then non-UTF-16 indexes are used in this chunk. */
    UBool nonUTF16Indexes;
    /** Unused. */
    UBool padding;
    /** Contains sizeof(UTextChunk) and allows the future addition of fields. */
    uint16_t sizeOfStruct;
};

/**
 * UText caller properties (bit field indexes).
 *
 * @see UText
 * @draft ICU 3.4
 */
enum {
    /**
     * The caller uses more random access than iteration.
     * @draft ICU 3.4
     */
    UTEXT_CALLER_RANDOM_ACCESS,
    /**
     * The caller requires UTF-16 index semantics.
     * @draft ICU 3.4
     */
    UTEXT_CALLER_REQUIRES_UTF16,
    /**
     * The caller provides a suggested chunk size in bits 31..16.
     * @draft ICU 3.4
     */
    UTEXT_CALLER_CHUNK_SIZE_SHIFT=16
};

/**
 * UText provider properties (bit field indexes).
 *
 * @see UText
 * @draft ICU 3.4
 */
enum {
    /**
     * The provider works with non-UTF-16 ("native") text indexes.
     * For example, byte indexes into UTF-8 text or UTF-32 indexes into UTF-32 text.
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_NON_UTF16_INDEXES,
    /**
     * The provider can return the text length inexpensively.
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_LENGTH_IS_INEXPENSIVE,
    /**
     * Text chunks remain valid and usable until the text object is modified or
     * deleted, not just until the next time the access() function is called
     * (which is the default).
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_STABLE_CHUNKS,
    /**
     * The provider supports modifying the text via the replace() and copy()
     * functions.
     * @see Replaceable
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_WRITABLE,
    /**
     * There is meta data associated with the text.
     * @see Replaceable::hasMetaData()
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_HAS_META_DATA
};

/**
 * Function type declaration for UText.clone().
 *
 * TBD
 *
 * May return NULL if the object cannot be cloned.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef UText * U_CALLCONV
UTextClone(const UText *t);

/**
 * Function type declaration for UText.exchangeProperties().
 *
 * TBD
 *
 * @param callerProperties Bit field with caller properties.
 *        If negative, none are communicated, only the provider properties
 *        are requested.
 * @return Provider properties bit field.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextExchangeProperties(UText *t, int32_t callerProperties);

/**
 * Function type declaration for UText.length().
 *
 * TBD
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextLength(UText *t);

/**
 * Function type declaration for UText.access().
 *
 * @param index Requested (native) index.
 * @param forward If TRUE, then the returned chunk must contain text
 *        starting from the index, so that start<=index<limit.
 *        If FALSE, then the returned chunk must contain text
 *        before the index, so that start<index<=limit.
 * @return Chunk-relative UTF-16 offset corresponding to the requested index.
 *         Negative value if a chunk cannot be accessed
 *         (the requested index is out of bounds).
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextAccess(UText *t, int32_t index, UBool forward, UTextChunk *chunk);

/**
 * Function type declaration for UText.extract().
 *
 * TBD
 *
 * The extracted string must be NUL-terminated if possible.
 *
 * @return Number of UChars extracted.
 *         If U_BUFFER_OVERFLOW_ERROR: Returns number of UChars for
 *         preflighting.
 *         If U_INDEX_OUTOFBOUNDS_ERROR: Start and limit do not specify
 *         accessible text. Return value undefined.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextExtract(UText *t,
             int32_t start, int32_t limit,
             UChar *dest, int32_t destCapacity,
             UErrorCode *pErrorCode);

/**
 * Function type declaration for UText.replace().
 *
 * TBD
 *
 * If chunk is not NULL and the chunk contents outside of start..limit is
 * modified, other than moving text after limit,
 * then the chunk->contents pointer is set to NULL.
 *
 * @return Delta between the limit of the replacement text and the limit argument,
 *         that is, the signed number of (native) storage units by which
 *         the old and the new pieces of text differ.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextReplace(UText *t,
             int32_t start, int32_t limit,
             const UChar *src, int32_t length,
             UTextChunk *chunk,
             UErrorCode *pErrorCode);

/**
 * Function type declaration for UText.copy().
 *
 * Copies a substring of this object, retaining metadata.
 * This method is used to duplicate or reorder substrings.
 * The destination index must not overlap the source range.
 *
 * TBD
 *
 * If chunk is not NULL and the chunk contents outside of start..limit is
 * modified, other than moving text after limit,
 * then the chunk->contents pointer is set to NULL.
 *
 * @param move If TRUE, then the substring is moved, not copied/duplicated.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef void U_CALLCONV
UTextCopy(UText *t,
          int32_t start, int32_t limit,
          int32_t destIndex,
          UBool move,
          UTextChunk *chunk,
          UErrorCode *pErrorCode);

/**
 * Function type declaration for UText.mapOffsetToNative().
 *
 * TBD
 *
 * @param offset UTF-16 offset relative to the current text chunk,
 *               0<=offset<=chunk->length.
 * @return Absolute (native) index corresponding to the UTF-16 offset
 *         relative to the current text chunk.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextMapOffsetToNative(UText *t, UTextChunk *chunk, int32_t offset);

/**
 * Function type declaration for UText.mapIndexToUTF16().
 *
 * TBD
 *
 * @param index Absolute (native) text index, chunk->start<=index<=chunk->limit.
 * @return Chunk-relative UTF-16 offset corresponding to the absolute (native)
 *         index.
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextMapIndexToUTF16(UText *t, UTextChunk *chunk, int32_t index);

struct UText {
    /**
     * (protected) Pointer to string or wrapped object or similar.
     * Not used by caller.
     * @draft ICU 3.4
     */
    const void *context;

    /**
     * (protected) Pointer fields for use by text provider.
     * Not used by caller.
     * @draft ICU 3.4
     */
    const void *p, *q, *r;

    /**
     * (public) sizeOfStruct=sizeof(UText)
     * Allows possible backward compatible extension.
     *
     * @draft ICU 3.4
     */
    int32_t sizeOfStruct;

    /**
     * (protected) Integer fields for use by text provider.
     * Not used by caller.
     * @draft ICU 3.4
     */
    int32_t a, b, c;

    /**
     * (public) TBD
     *
     * @see UTextClone
     * @draft ICU 3.4
     */
    UTextClone *clone;

    /**
     * (public) TBD
     *
     * @see UTextExchangeProperties
     * @draft ICU 3.4
     */
    UTextExchangeProperties *exchangeProperties;

    /**
     * (public) Returns the length of the text.
     * May be expensive to compute!
     *
     * @see UTextLength
     * @draft ICU 3.4
     */
    UTextLength *length;

    /**
     * (public) Access to a chunk of text.
     * Does not copy text but instead gives access to a portion of it.
     *
     * The intention is that for discontiguous storage the chunk would be an actual
     * storage block used for storing the text.
     * For contiguously stored text with known length, the whole text would be returned.
     * For NUL-terminated text, the implementation may scan forward in exponentially
     * larger chunks instead of finding the NUL right away.
     *
     * In: Text index; the returned chunk of text must contain the index.
     * Out:
     * - Pointer to chunk start
     * - Start and limit indexes corresponding to the chunk;
     *   it must be start<=input index<limit
     * - Indication of success: If the input index is negative or >=length then
     *   failure needs to be indicated, probably by returning a NULL pointer
     *
     * @see UTextAccess
     * @draft ICU 3.4
     */
    UTextAccess *access;

    /**
     * (public) Copy a chunk of text into a buffer.
     * Does it need a return value indicating success/failure?
     * The signature shown here is the same as in UReplaceable.
     * Not strictly minimally necessary; Replaceable has it.
     *
     * @see UTextExtract
     * @draft ICU 3.4
     */
    UTextExtract *extract;

    /**
     * (public) TBD
     *
     * @see UTextReplace
     * @draft ICU 3.4
     */
    UTextReplace *replace;

    /**
     * (public) TBD
     *
     * @see UTextCopy
     * @draft ICU 3.4
     */
    UTextCopy *copy;

    /**
     * (public) TBD
     *
     * @see UTextMapOffsetToNative
     * @draft ICU 3.4
     */
    UTextMapOffsetToNative *mapOffsetToNative;

    /**
     * (public) TBD
     *
     * @see UTextMapIndexToUTF16
     * @draft ICU 3.4
     */
    UTextMapIndexToUTF16 *mapIndexToUTF16;
};

/**
 * Open a read-only UText implementation for UTF-8 strings.
 */
U_DRAFT UText * U_EXPORT2
utext_openUTF8(const uint8_t *s, int32_t length, UErrorCode *pErrorCode);

U_DRAFT void U_EXPORT2
utext_closeUTF8(UText *t);

U_DRAFT void U_EXPORT2
utext_resetUTF8(UText *t, const uint8_t *s, int32_t length, UErrorCode *pErrorCode);

/**
 * Open a read-only UText implementation for SBCS strings.
 * The implementation converts 1:1 according to the provided mapping table.
 * Supplementary code points are not supported.
 *
 * @param toU Mapping table for conversion from SBCS to Unicode (BMP only).
 *            The mapping table must be available during the lifetime of the
 *            UText object.
 */
U_DRAFT UText * U_EXPORT2
utext_openSBCS(const UChar toU[256],
               const char *s, int32_t length,
               UErrorCode *pErrorCode);

U_DRAFT void U_EXPORT2
utext_closeSBCS(UText *t);

U_DRAFT void U_EXPORT2
utext_resetSBCS(UText *t, const char *s, int32_t length, UErrorCode *pErrorCode);

U_CDECL_END

#ifdef XP_CPLUSPLUS

U_NAMESPACE_BEGIN

class UTextIterator {
public:
    // all-inline, and stack-allocatable
    // constructors, get/set UText, etc.
    // needs to have state besides the current chunk: at least the current index
    // for performance, may use a current-position pointer and chunk start/limit
    // pointers and translate back into indexes only when necessary

    UTextIterator(UText *t, int32_t callerProperties=0);

    /**
     * Returns the code point at the requested index,
     * or U_SENTINEL (-1) if it is out of bounds.
     */
    // see next32From() -- inline UChar32 char32At(int32_t index);

    // U_SENTINEL (-1) if out of bounds
    inline UChar32 next32();
    inline UChar32 previous32();

    /**
     * Set the iteration index for a following next32() or previous32().
     * Does not immediately access text from the provider.
     * next32From(index) is more efficient than setIndex()+next32().
     * previous32From(index) is more efficient than setIndex()+previous32().
     */
    inline void setIndex(int32_t index);

    /**
     * Set the iteration index, access the text for forward iteration,
     * and return the code point starting at or before that index.
     *
     * @param index Iteration index.
     * @return Code point which starts at or before index,
     *         or U_SENTINEL (-1) if it is out of bounds.
     */
    inline UChar32 next32From(int32_t index);
    /**
     * Set the iteration index, access the text for backward iteration,
     * and return the code point ending at or before that index.
     *
     * @param index Iteration index.
     * @return Code point which ends at or before index,
     *         or U_SENTINEL (-1) if it is out of bounds.
     */
    inline UChar32 previous32From(int32_t index);

    // see next32From() -- inline UBool setIndex(int32_t index);   // use origin enum as in UCharIterator?
    inline int32_t getIndex();              // use origin enum as in UCharIterator?

    /**
     * @return TRUE if successful.
     */
    UBool moveIndex(int32_t delta);  // signed delta code points

    /**
     * Compare the text starting from the current index with the string
     * argument. The index is modified. In case of a match (zero result),
     * the index is left exactly after the matching segment.
     * Otherwise, the index position is undefined.
     *
     * Negative/positive results mean that the text segment compares
     * lower/higher than the string. A zero result means that the text
     * segment compares equal, even if there is following text after the
     * matching segment.
     * Test for the end of the text using next32()>=0 if necessary.
     *
     * @param codePointOrder Choose between code unit order (FALSE)
     *                       and code point order (TRUE).
     *
     * @return negative/0/positive as comparison result.
     *
     *  TODO:  this function seems a little out of place in this class.
     *         Probably should be removed to some collection of TextIterator based 
     *         string utiltity functions.
     * @internal
     */
    UBool compare(const UChar *s, int32_t length, UBool codePointOrder);

    // convenience wrappers for length(), access(), extract()?
    // needed at least for extract()/copy() for chunk invalidation
    // getChunkStart(), getChunkLimit() for the current chunk?
    // const UTextChunk *getChunk()?

private:
    UText *t;
    UTextChunk chunk;
    int32_t chunkOffset;
    int32_t providerProperties; // -1 if not known yet

    void setChunkInvalid(int32_t index);

    /** Call chunkOffset=t->access() and return TRUE if a chunk is returned. */
    UBool access(int32_t index, UBool forward);
};

U_NAMESPACE_END

#if 0 // initially commented out to reduce testing

/**
 * Open a writable UText implementation for Replaceable objects.
 */
U_DRAFT UText * U_EXPORT2
utext_openReplaceable(Replaceable *rep, UErrorCode *pErrorCode);

U_DRAFT void U_EXPORT2
utext_closeReplaceable(UText *t);

U_DRAFT void U_EXPORT2
utext_resetReplaceable(UText *t, Replaceable *rep, UErrorCode *pErrorCode);

#endif

/**
 * Set the UText object to handle a writable UnicodeString.
 */
U_DRAFT void U_EXPORT2
utext_setUnicodeString(UText *t, UnicodeString *s);

// UTextIterator inline implementations ------------------------------------ ***

U_NAMESPACE_BEGIN

UChar32
UTextIterator::next32() {
    if(chunkOffset>=chunk.length && !access(chunk.limit, TRUE)) {
        // no chunk available here
        return U_SENTINEL;
    }

    UChar32 c;
    U16_NEXT(chunk.contents, chunkOffset, chunk.length, c);
    return c;
}

UChar32
UTextIterator::previous32() {
    if(chunkOffset<=0 && !access(chunk.start, FALSE)) {
        // no chunk available here
        return U_SENTINEL;
    }

    UChar32 c;
    U16_PREV(chunk.contents, 0, chunkOffset, c);
    return c;
}

void
UTextIterator::setIndex(int32_t index) {
    if(index<chunk.start || chunk.limit<index) {
        // leave it to next32() or previous32() to access the text
        // in the desired direction
        setChunkInvalid(index);
    } else if(chunk.nonUTF16Indexes) {
        chunkOffset=t->mapIndexToUTF16(t, &chunk, index);
    } else {
        chunkOffset=index-chunk.start;
    }
}

UChar32
UTextIterator::next32From(int32_t index) {
    if(index<chunk.start || chunk.limit<=index) {
        if(!access(index, TRUE)) {
            // no chunk available here
            return U_SENTINEL;
        }
    } else if(chunk.nonUTF16Indexes) {
        chunkOffset=t->mapIndexToUTF16(t, &chunk, index);
    } else {
        chunkOffset=index-chunk.start;
    }

    UChar32 c;
    U16_NEXT(chunk.contents, chunkOffset, chunk.length, c);
}

UChar32 UTextIterator::previous32From(int32_t index) {
    if(index<=chunk.start || chunk.limit<index) {
        if(!access(index, FALSE)) {
            // no chunk available here
            return U_SENTINEL;
        }
    } else if(chunk.nonUTF16Indexes) {
        chunkOffset=t->mapIndexToUTF16(t, &chunk, index);
    } else {
        chunkOffset=index-chunk.start;
    }

    UChar32 c;
    U16_PREV(chunk.contents, 0, chunkOffset, c);
}

int32_t UTextIterator::getIndex() {
    if(!chunk.nonUTF16Indexes) {
        return chunk.start+chunkOffset;
    } else {
        return t->mapOffsetToNative(t, &chunk, chunkOffset);
    }
}

U_NAMESPACE_END

#endif /* C++ */

#endif /* U_HIDE_DRAFT_API */

#endif
