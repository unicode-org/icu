// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2014, International Business Machines Corporation   *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */

#ifndef CJMLBREAKENGINE_H
#define CJMLBREAKENGINE_H

#include "unicode/utypes.h"
#include "unicode/uniset.h"
#include "unicode/utext.h"

#include "brkeng.h"
#include "dictbe.h"
#include "hash.h"
#include "uvectr32.h"


U_NAMESPACE_BEGIN

#if !UCONFIG_NO_NORMALIZATION

class CjMLBreakEngine : public UMemory {

public:
    /**
     * <p>Constructor.</p>
     *
     * @param fDigitOrOpenPunctuationOrAlphabetSet A UnicodeSet with Digit, open punctuation and alphabet.
     * @param fClosePunctuationSet A UnicodeSet with close punctuation.
     * @param status Information on any errors encountered.
     */
    CjMLBreakEngine(UnicodeSet &fDigitOrOpenPunctuationOrAlphabetSet, UnicodeSet &fClosePunctuationSet, UErrorCode &status);

    /**
     * <p>Virtual destructor.</p>
     */
    virtual ~CjMLBreakEngine();

public:
    /**
     * <p>Divide up a range of known dictionary characters handled by this break engine.</p>
     *
     * @param inText A UText representing the text
     * @param rangeStart The start of the range of dictionary characters
     * @param rangeEnd The end of the range of dictionary characters
     * @param foundBreaks Output of C array of int32_t break positions, or 0
     * @param inString The normalized string of text ranging from rangeStart to rangeEnd
     * @param inputMap The vector storing the native index of inText
     * @param status Information on any errors encountered.
     * @return The number of breaks found
     */
    int32_t divideUpDictionaryRange(UText *inText,
                                            int32_t rangeStart,
                                            int32_t rangeEnd,
                                            UVector32 &foundBreaks,
                                            UnicodeString &inString,
                                            LocalPointer<UVector32> &inputMap,
                                            UErrorCode& status) const;

private:
    // load ML model parameter
    void loadMLparams(UErrorCode& error);
    // generates a feature from the elementList's characters around (w1-w6) and past results (p1-p3).
    void getFeature(UVector* elementList, UChar32 p1, UChar32 p2, UChar32 p3, UVector* list, UErrorCode& status) const;
    // initiate elementList by inString's first four characters.
    void initElementList(UnicodeString &inString, UVector *elementList, UErrorCode& status) const;
    // returns the index of the Unicode block that the character belongs to.
    UnicodeString getUnicodeBlockIndex(UChar32 ch, UErrorCode& status) const;

    UnicodeSet fDigitOrOpenPunctuationOrAlphabetSet;
    UnicodeSet fClosePunctuationSet;
    Hashtable fModel;
    UVector32 *fUnicodeBlock;
};

#endif

U_NAMESPACE_END

    /* CJKMLBREAKENGINE_H */
#endif
