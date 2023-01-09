// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef MLBREAKENGINE_H
#define MLBREAKENGINE_H

#include "hash.h"
#include "unicode/uniset.h"
#include "unicode/utext.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

#if !UCONFIG_NO_BREAK_ITERATION

/**
 * A machine learning break engine for the phrase breaking in Japanese.
 */
class MlBreakEngine : public UMemory {
   public:
    /**
     * Constructor.
     *
     * @param digitOrOpenPunctuationOrAlphabetSet An UnicodeSet with the digit, open punctuation and
     * alphabet.
     * @param closePunctuationSet An UnicodeSet with close punctuation.
     * @param status Information on any errors encountered.
     */
    MlBreakEngine(const UnicodeSet &digitOrOpenPunctuationOrAlphabetSet,
                    const UnicodeSet &closePunctuationSet, UErrorCode &status);

    /**
     * Virtual destructor.
     */
    virtual ~MlBreakEngine();

   public:
    /**
     * Divide up a range of characters handled by this break engine.
     *
     * @param inText A UText representing the text
     * @param rangeStart The start of the range of the characters
     * @param rangeEnd The end of the range of the characters
     * @param foundBreaks Output of C array of int32_t break positions, or 0
     * @param inString The normalized string of text ranging from rangeStart to rangeEnd
     * @param inputMap The vector storing the native index of inText
     * @param status Information on any errors encountered.
     * @return The number of breaks found
     */
    int32_t divideUpRange(UText *inText, int32_t rangeStart, int32_t rangeEnd,
                          UVector32 &foundBreaks, const UnicodeString &inString,
                          const LocalPointer<UVector32> &inputMap, UErrorCode &status) const;

   private:
    /**
     * Load the machine learning's model file.
     *
     * @param error Information on any errors encountered.
     */
    void loadMLModel(UErrorCode &error);

    /**
     * Initialize the element list from the input string.
     *
     * @param inString A input string to be segmented.
     * @param elementList A list to store the first six characters.
     * @param status Information on any errors encountered.
     * @return The number of code units of the first six characters in inString.
     */
    int32_t initElementList(const UnicodeString &inString, UChar32* elementList,
                            UErrorCode &status) const;

    /**
     * Evaluate whether the index is a potential breakpoint.
     *
     * @param elementList A list including six elements for the breakpoint evaluation.
     * @param index The breakpoint index to be evaluated.
     * @param numBreaks The accumulated number of breakpoints.
     * @param boundary A vector including the index of the breakpoint.
     * @param status Information on any errors encountered.
     */
    void evaluateBreakpoint(UChar32* elementList, int32_t index, int32_t &numBreaks,
                            UVector32 &boundary, UErrorCode &status) const;

    UnicodeSet fDigitOrOpenPunctuationOrAlphabetSet;
    UnicodeSet fClosePunctuationSet;
    Hashtable fModel;
    int32_t fNegativeSum;
};

#endif

U_NAMESPACE_END

/* MLBREAKENGINE_H */
#endif
