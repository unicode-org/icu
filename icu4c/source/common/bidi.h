/*
*******************************************************************************
*
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
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

#include "utypes.h"
#include "ubidi.h"

#ifndef XP_CPLUSPLUS
#   error This is a C++ header file.
#endif

/**
 * BiDi is a C++ wrapper class for UBiDi.
 * You need one BiDi object in place of one UBiDi object.
 * For details on the API and implementation of the
 * Unicode BiDi algorithm, see ubidi.h.
 *
 * @see UBiDi
 */
class U_COMMON_API BiDi {
public:
    /** @memo Default constructor, calls ubidi_open(). */
    BiDi();

    /** @memo Constructor, calls ubidi_open(). */
    BiDi(UErrorCode &rErrorCode);

    /** @memo Preallocating constructor, calls ubidi_openSized(). */
    BiDi(UTextOffset maxLength, UTextOffset maxRunCount, UErrorCode &rErrorCode);

    /** @memo Destructor, calls ubidi_close(). */
    ~BiDi();

    /** @memo Set this object for one paragraph's text. */
    BiDi &
    setPara(const UChar *text, UTextOffset length,
            UBiDiLevel paraLevel, UBiDiLevel *embeddingLevels,
            UErrorCode &rErrorCode);


    /** @memo Set this object for one line of the paragraph object's text. */
    BiDi &
    setLine(const BiDi &rParaBiDi,
            UTextOffset start, UTextOffset limit,
            UErrorCode &rErrorCode);

    /** @memo Get the directionality of the text. */
    UBiDiDirection
    getDirection() const;

    /** @memo Get the length of the text. */
    UTextOffset
    getLength() const;

    /** @memo Get the paragraph level of the text. */
    UBiDiLevel
    getParaLevel() const;

    /** @memo Get the level for one character. */
    UBiDiLevel
    getLevelAt(UTextOffset charIndex) const;

    /** @memo Get an array of levels for each character. */
    const UBiDiLevel *
    getLevels(UErrorCode &rErrorCode);

    /** @memo Get a logical run. */
    void
    getLogicalRun(UTextOffset logicalStart,
                  UTextOffset &rLogicalLimit, UBiDiLevel &rLevel) const;

    /** @memo Get the number of runs. */
    UTextOffset
    countRuns(UErrorCode &rErrorCode);

    /**
     * @memo Get one run's logical start, length, and directionality,
     *       which can be 0 for LTR or 1 for RTL.
     */
    UBiDiDirection
    getVisualRun(UTextOffset runIndex, UTextOffset &rLogicalStart, UTextOffset &rLength);

    /** @memo Get the visual position from a logical text position. */
    UTextOffset
    getVisualIndex(UTextOffset logicalIndex, UErrorCode &rErrorCode);

    /** @memo Get the logical text position from a visual position. */
    UTextOffset
    getLogicalIndex(UTextOffset visualIndex, UErrorCode &rErrorCode);

    /**
     * @memo Get a logical-to-visual index map (array) for the characters in the UBiDi
     *       (paragraph or line) object.
     */
    void
    getLogicalMap(UTextOffset *indexMap, UErrorCode &rErrorCode);

    /**
     * @memo Get a visual-to-logical index map (array) for the characters in the UBiDi
     *       (paragraph or line) object.
     */
    void
    getVisualMap(UTextOffset *indexMap, UErrorCode &rErrorCode);

    /** @memo Same as ubidi_reorderLogical(). */
    static void
    reorderLogical(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap);

    /** @memo Same as ubidi_reorderVisual(). */
    static void
    reorderVisual(const UBiDiLevel *levels, UTextOffset length, UTextOffset *indexMap);

    /** @memo Same as ubidi_invertMap(). */
    static void
    invertMap(const UTextOffset *srcMap, UTextOffset *destMap, UTextOffset length);

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

#endif
