/*
******************************************************************************
*
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*   file name:  ubidi.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999sep15
*   created by: Markus W. Scherer
*/

#ifndef BIDI_H
#define BIDI_H

#include "unicode/utypes.h"
#include "unicode/ubidi.h"

#ifndef XP_CPLUSPLUS
#   error This is a C++ header file.
#endif

U_NAMESPACE_BEGIN
/**
 * This class is deprecated and will be removed.
 * Use the C API with the UBiDi type and ubidi_... functions.
 * The BiDi class was just a pure 1:1 wrapper for the ubidi_ API.
 *
 * Old documentation:
 *
 * BiDi is a C++ wrapper class for UBiDi.
 * You need one BiDi object in place of one UBiDi object.
 * For details on the API and implementation of the
 * Unicode BiDi algorithm, see ubidi.h.
 *
 * @see UBiDi
 * @deprecated To be removed after 2002-sep-30; use the C API with UBiDi and ubidi_... functions.
 */
class U_COMMON_API BiDi {
public:
    /** @memo Default constructor, calls ubidi_open(). 
     * @deprecated
     */
    BiDi();

    /** @memo Constructor, calls ubidi_open(). 
     * @deprecated
     */
    BiDi(UErrorCode &rErrorCode);

    /** @memo Preallocating constructor, calls ubidi_openSized(). 
     * @deprecated
     */
    BiDi(UTextOffset maxLength, UTextOffset maxRunCount, UErrorCode &rErrorCode);

    /** @memo Destructor, calls ubidi_close(). 
     * @deprecated
     */
    ~BiDi();

    /**
     * Modify the operation of the BiDi algorithm such that it
     * approximates an "inverse BiDi" algorithm. This function
     * must be called before <code>setPara()</code>.
     *
     * @param isInverse specifies "forward" or "inverse" BiDi operation
     *
     * @see setPara
     * @see writeReordered
     * @deprecated
     */
    void
    setInverse(UBool isInverse);

    /**
     * Is this BiDi object set to perform the inverse BiDi algorithm?
     *
     * @see setInverse
     * @deprecated
     */
    UBool
    isInverse();

    /** @memo Set this object for one paragraph's text. 
     * @deprecated
     */
    BiDi &
    setPara(const UChar *text, UTextOffset length,
            UBiDiLevel paraLevel, UBiDiLevel *embeddingLevels,
            UErrorCode &rErrorCode);


    /** @memo Set this object for one line of the paragraph object's text. 
     * @deprecated
     */
    BiDi &
    setLine(const BiDi &rParaBiDi,
            UTextOffset start, UTextOffset limit,
            UErrorCode &rErrorCode);

    /** @memo Get the directionality of the text. 
     * @deprecated
     */
    UBiDiDirection
    getDirection() const;

    /** @memo Get the pointer to the text.
     * @deprecated
     */
    const UChar *
    getText() const;

    /** @memo Get the length of the text. 
     * @deprecated
     */
    UTextOffset
    getLength() const;

    /** @memo Get the paragraph level of the text. 
     * @deprecated
     */
    UBiDiLevel
    getParaLevel() const;

    /** @memo Get the level for one character. 
     * @deprecated
     */
    UBiDiLevel
    getLevelAt(UTextOffset charIndex) const;

    /** @memo Get an array of levels for each character. 
     * @deprecated
     */
    const UBiDiLevel *
    getLevels(UErrorCode &rErrorCode);

    /** @memo Get a logical run. 
     * @deprecated
     */
    void
    getLogicalRun(UTextOffset logicalStart,
                  UTextOffset &rLogicalLimit, UBiDiLevel &rLevel) const;

    /** @memo Get the number of runs. 
     * @deprecated
     */
    UTextOffset
    countRuns(UErrorCode &rErrorCode);

    /**
     * @memo Get one run's logical start, length, and directionality,
     *       which can be 0 for LTR or 1 for RTL.     
     * @deprecated
     */
    UBiDiDirection
    getVisualRun(UTextOffset runIndex, UTextOffset &rLogicalStart, UTextOffset &rLength);

    /** @memo Get the visual position from a logical text position. 
     * @deprecated
     */
    UTextOffset
    getVisualIndex(UTextOffset logicalIndex, UErrorCode &rErrorCode);

    /** @memo Get the logical text position from a visual position. 
     * @deprecated
     */
    UTextOffset
    getLogicalIndex(UTextOffset visualIndex, UErrorCode &rErrorCode);

    /**
     *  @memo Get a logical-to-visual index map (array) for the characters in the UBiDi
     *       (paragraph or line) object.
     * @deprecated
     */
    void
    getLogicalMap(UTextOffset *indexMap, UErrorCode &rErrorCode);

    /**
     * @memo Get a visual-to-logical index map (array) for the characters in the UBiDi
     *       (paragraph or line) object.
     * @deprecated
     */
    void
    getVisualMap(UTextOffset *indexMap, UErrorCode &rErrorCode);

    /** @memo Same as ubidi_reorderLogical(). 
     * @deprecated
     */
    static void
    reorderLogical(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap);

    /** @memo Same as ubidi_reorderVisual(). 
     * @deprecated
     */
    static void
    reorderVisual(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap);

    /** @memo Same as ubidi_invertMap(). 
     * @deprecated
     */
    static void
    invertMap(const UTextOffset *srcMap, UTextOffset *destMap, UTextOffset length);

    /**
     * Use the <code>BiDi</code> object containing the reordering
     * information for one paragraph or line of text as set by
     * <code>setPara()</code> or <code>setLine()</code> and
     * write a reordered string to the destination buffer.
     *
     * @see ubidi_writeReordered
     * @deprecated
     */
    UTextOffset
    writeReordered(UChar *dest, int32_t destSize,
                   uint16_t options,
                   UErrorCode &rErrorCode);

    /**
     * Reverse a Right-To-Left run of Unicode text.
     *
     * @see ubidi_writeReverse
     * @deprecated
     */
    static UTextOffset
    writeReverse(const UChar *src, int32_t srcLength,
                 UChar *dest, int32_t destSize,
                 uint16_t options,
                 UErrorCode &rErrorCode);

protected:
    UBiDi *pBiDi;
};

/* Inline implementations. -------------------------------------------------- */

inline BiDi::BiDi() {
    pBiDi=ubidi_open();
}

inline BiDi::BiDi(UErrorCode &rErrorCode) {
    if(U_SUCCESS(rErrorCode)) {
        pBiDi=ubidi_open();
        if(pBiDi==0) {
            rErrorCode=U_MEMORY_ALLOCATION_ERROR;
        }
    } else {
        pBiDi=0;
    }
}

inline BiDi::BiDi(UTextOffset maxLength, UTextOffset maxRunCount, UErrorCode &rErrorCode) {
    pBiDi=ubidi_openSized(maxLength, maxRunCount, &rErrorCode);
}

inline BiDi::~BiDi() {
    ubidi_close(pBiDi);
    pBiDi=0;
}

inline void
BiDi::setInverse(UBool isInverse) {
    ubidi_setInverse(pBiDi, isInverse);
}

inline UBool
BiDi::isInverse() {
    return ubidi_isInverse(pBiDi);
}

inline BiDi &
BiDi::setPara(const UChar *text, UTextOffset length,
        UBiDiLevel paraLevel, UBiDiLevel *embeddingLevels,
        UErrorCode &rErrorCode) {
    ubidi_setPara(pBiDi, text, length, paraLevel, embeddingLevels, &rErrorCode);
    return *this;
}


inline BiDi &
BiDi::setLine(const BiDi &rParaBiDi,
        UTextOffset start, UTextOffset limit,
        UErrorCode &rErrorCode) {
    ubidi_setLine(rParaBiDi.pBiDi, start, limit, pBiDi, &rErrorCode);
    return *this;
}

inline UBiDiDirection
BiDi::getDirection() const {
    return ubidi_getDirection(pBiDi);
}

inline const UChar *
BiDi::getText() const {
    return ubidi_getText(pBiDi);
}

inline UTextOffset
BiDi::getLength() const {
    return ubidi_getLength(pBiDi);
}

inline UBiDiLevel
BiDi::getParaLevel() const {
    return ubidi_getParaLevel(pBiDi);
}

inline UBiDiLevel
BiDi::getLevelAt(UTextOffset charIndex) const {
    return ubidi_getLevelAt(pBiDi, charIndex);
}

inline const UBiDiLevel *
BiDi::getLevels(UErrorCode &rErrorCode) {
    return ubidi_getLevels(pBiDi, &rErrorCode);
}

inline void
BiDi::getLogicalRun(UTextOffset logicalStart,
              UTextOffset &rLogicalLimit, UBiDiLevel &rLevel) const {
    ubidi_getLogicalRun(pBiDi, logicalStart, &rLogicalLimit, &rLevel);
}

inline UTextOffset
BiDi::countRuns(UErrorCode &rErrorCode) {
    return ubidi_countRuns(pBiDi, &rErrorCode);
}

inline UBiDiDirection
BiDi::getVisualRun(UTextOffset runIndex, UTextOffset &rLogicalStart, UTextOffset &rLength) {
    return ubidi_getVisualRun(pBiDi, runIndex, &rLogicalStart, &rLength);
}

inline UTextOffset
BiDi::getVisualIndex(UTextOffset logicalIndex, UErrorCode &rErrorCode) {
    return ubidi_getVisualIndex(pBiDi, logicalIndex, &rErrorCode);
}

inline UTextOffset
BiDi::getLogicalIndex(UTextOffset visualIndex, UErrorCode &rErrorCode) {
    return ubidi_getLogicalIndex(pBiDi, visualIndex, &rErrorCode);
}

inline void
BiDi::getLogicalMap(UTextOffset *indexMap, UErrorCode &rErrorCode) {
    ubidi_getLogicalMap(pBiDi, indexMap, &rErrorCode);
}

inline void
BiDi::getVisualMap(UTextOffset *indexMap, UErrorCode &rErrorCode) {
    ubidi_getVisualMap(pBiDi, indexMap, &rErrorCode);
}

inline void
BiDi::reorderLogical(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap) {
    ubidi_reorderLogical(levels, length, indexMap);
}

inline void
BiDi::reorderVisual(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap) {
    ubidi_reorderVisual(levels, length, indexMap);
}

inline void
BiDi::invertMap(const UTextOffset *srcMap, UTextOffset *destMap, UTextOffset length) {
    ubidi_invertMap(srcMap, destMap, length);
}

inline UTextOffset
BiDi::writeReordered(UChar *dest, int32_t destSize,
                     uint16_t options,
                     UErrorCode &rErrorCode) {
    return ubidi_writeReordered(pBiDi, dest, destSize, options, &rErrorCode);
}

inline UTextOffset
BiDi::writeReverse(const UChar *src, int32_t srcLength,
                   UChar *dest, int32_t destSize,
                   uint16_t options,
                   UErrorCode &rErrorCode) {
    return ubidi_writeReverse(src, srcLength, dest, destSize, options, &rErrorCode);
}

U_NAMESPACE_END

#endif
