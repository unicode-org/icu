// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 *******************************************************************************
 * Copyright (C) 2006-2016, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */
#include <utility>

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include <stdlib.h>
#include "cjmlbe.h"
#include "brkeng.h"
#include "dictbe.h"
#include "unicode/uniset.h"
#include "unicode/chariter.h"
#include "unicode/resbund.h"
#include "unicode/ubrk.h"
#include "unicode/usetiter.h"
#include "ubrkimpl.h"
#include "utracimp.h"
#include "uvectr32.h"
#include "uvector.h"
#include "uassert.h"
#include "unicode/normlzr.h"
#include "cmemory.h"
#include "dictionarydata.h"

U_NAMESPACE_BEGIN

#if !UCONFIG_NO_NORMALIZATION

static inline int32_t utext_i32_flag(int32_t bitIndex) {
    return (int32_t)1 << bitIndex;
}

class Element {
public:
    Element(){};
    Element(UChar32 ch, UnicodeString str) {
        this->ch = ch;
        this->str = str;
    };

    UChar32 ch;
    UnicodeString str;
};


CjMLBreakEngine::CjMLBreakEngine(UnicodeSet& fDigitOrOpenPunctuationOrAlphabetSet,
    UnicodeSet& fClosePunctuationSet, UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    this->fDigitOrOpenPunctuationOrAlphabetSet = fDigitOrOpenPunctuationOrAlphabetSet;
    this->fClosePunctuationSet = fClosePunctuationSet;
    fUnicodeBlock = new UVector32(status);
    loadMLparams(status);
}

CjMLBreakEngine::~CjMLBreakEngine() {
}

static const UChar32 INVALID = '▔';
static const UChar32 UNKNOWN = 'U';
static const UChar32 POSITIVE = 'B';
static const UChar32 NEGATIVE = 'O';
static const int32_t THRESHOLD = 1000;

static UnicodeString* concatChar(const char* str, UChar32 arr[], UErrorCode& status) {
    UnicodeString* temp = new UnicodeString(str);
    int32_t idx = 0;
    while (arr[idx] != -1) {
        temp->append(arr[idx++]);
    }
    LocalPointer<UnicodeString> result(temp, status);
    return result.orphan();
}

static UnicodeString* concatUnicodeString(const char* str1, UnicodeString &str2, UErrorCode& status) {
    UnicodeString* temp = new UnicodeString(str1);
    temp->append(str2);
    LocalPointer<UnicodeString> result(temp, status);
    return result.orphan();
}

int32_t
CjMLBreakEngine::divideUpDictionaryRange( UText *inText,
    int32_t rangeStart,
    int32_t rangeEnd,
    UVector32 &foundBreaks,
    UnicodeString &inString,
    LocalPointer<UVector32> &inputMap,
    UErrorCode& status) const {

    if (U_FAILURE(status)) return 0;
    if (rangeStart >= rangeEnd) {
        return 0;
    }

    UVector* featureList = new UVector(uprv_deleteUObject, uhash_compareUnicodeString, 42, status);
    UVector32 t_boundary(inString.countChar32() + 1, status);
    int32_t score = 0;
    int32_t numBreaks = 0;
    int32_t prevCPPos = -1;
    int32_t prevUTextPos = -1;
    int32_t correctedNumBreaks = 0;

    UChar32 p = UNKNOWN;
    UChar32 p1 = UNKNOWN;
    UChar32 p2 = UNKNOWN;
    UChar32 p3 = UNKNOWN;

    UChar32 w;
    UnicodeString b;
    UVector* elementList = new UVector(6, status);

    for (int32_t i = 1; i < inString.length(); i++) {
        if (elementList->size() > 0) {
            Element* element = (Element*)elementList->elementAt(0);
            elementList->removeElementAt(0);
            delete element;
            w = (i + 2) < inString.length() ? inString.char32At(i + 2) : INVALID;
            b = (w != INVALID) ? getUnicodeBlockIndex(w, status) : UnicodeString(INVALID);
            elementList->addElement(new Element(w, b), status);
        }
        else {
            initElementList(inString, elementList, status);
        }

        getFeature(elementList, p1, p2, p3, featureList, status);

        int32_t score = 0;
        for (int32_t j = 0; j < featureList->size(); j++) {
            UnicodeString* key = (UnicodeString*)featureList->elementAt(j);
            if (!fModel.containsKey(*key))
                continue;
            score += fModel.geti(*key);
        }

        if (score > THRESHOLD) {
            t_boundary.addElement(i, status);
            numBreaks++;
        }
        p = (score > 0) ? POSITIVE : NEGATIVE;
        p1 = p2;
        p2 = p3;
        p3 = p;
        featureList->removeAllElements();
    }
    delete featureList;
    for (int i = 5; i >= 0; i--) {
        Element* element = (Element*)elementList->elementAt(i);
        elementList->removeElementAt(i);
        delete element;
    }
    delete elementList;

    // Add a break for the start of the dictionary range if there is not one
    // there already.
    if (foundBreaks.size() == 0 || foundBreaks.peeki() < rangeStart) {
        t_boundary.insertElementAt(0, 0, status);
        numBreaks++;
    }
    if (t_boundary.lastElementi() != inString.length()) {
        t_boundary.addElement(inString.length(), status);
        numBreaks++;
    }

    for (int32_t i = 0; i < numBreaks; i++) {
        int32_t cpPos = t_boundary.elementAti(i);
        int32_t utextPos = inputMap.isValid() ? inputMap->elementAti(cpPos) : cpPos + rangeStart;
        U_ASSERT(cpPos > prevCPPos);
        U_ASSERT(utextPos >= prevUTextPos);

        if (utextPos > prevUTextPos) {
            if (utextPos != rangeStart
                || (utextPos > 0
                       && fClosePunctuationSet.contains(utext_char32At(inText, utextPos - 1)))) {
                foundBreaks.push(utextPos, status);
                correctedNumBreaks++;
            }
        }
        else {
            // Normalization expanded the input text, the dictionary found a boundary
            // within the expansion, giving two boundaries with the same index in the
            // original text. Ignore the second. See ticket #12918.
            --numBreaks;
        }
        prevCPPos = cpPos;
        prevUTextPos = utextPos;
    }
    UChar32 nextChar = utext_char32At(inText, rangeEnd);
    if (!foundBreaks.isEmpty() && foundBreaks.peeki() == rangeEnd) {
        // In phrase breaking, there has to be a breakpoint between Cj character and
        // the number/open punctuation.
        // E.g. る文字「そうだ、京都」->る▁文字▁「そうだ、▁京都」-> breakpoint between 字 and「
        // E.g. 乗車率９０％程度だろうか -> 乗車▁率▁９０％▁程度だろうか -> breakpoint between 率 and ９
        // E.g. しかもロゴがＵｎｉｃｏｄｅ！ -> しかも▁ロゴが▁Ｕｎｉｃｏｄｅ！-> breakpoint between が and Ｕ
        if (!fDigitOrOpenPunctuationOrAlphabetSet.contains(nextChar)) {
            foundBreaks.popi();
            correctedNumBreaks--;
        }
    }

    return correctedNumBreaks;
}

void CjMLBreakEngine::initElementList(UnicodeString& inString, UVector* elementList, UErrorCode& status) const {
    UChar32 w1, w2, w3, w4, w5, w6;
    UnicodeString b1, b2, b3, b4, b5, b6;
    w1 = INVALID;
    w2 = INVALID;
    w3 = inString.char32At(0);
    w4 = inString.char32At(1);
    w5 = 2 < inString.length() ? inString.char32At(2) : INVALID;
    w6 = 3 < inString.length() ? inString.char32At(3) : INVALID;

    b1 = UnicodeString(INVALID);
    b2 = UnicodeString(INVALID);
    b3 = getUnicodeBlockIndex(w3, status);
    b4 = getUnicodeBlockIndex(w4, status);
    b5 = getUnicodeBlockIndex(w5, status);
    b6 = getUnicodeBlockIndex(w6, status);

    elementList->addElement(new Element(w1, b1), status);
    elementList->addElement(new Element(w2, b2), status);
    elementList->addElement(new Element(w3, b3), status);
    elementList->addElement(new Element(w4, b4), status);
    elementList->addElement(new Element(w5, b5), status);
    elementList->addElement(new Element(w6, b6), status);
}

void CjMLBreakEngine::getFeature(UVector* elementList, UChar32 p1, UChar32 p2, UChar32 p3,
    UVector* featureList, UErrorCode& status) const{

    UChar32 w1, w2, w3, w4, w5, w6;
    UnicodeString b1, b2, b3, b4, b5, b6;

    w1 = ((Element*)elementList->elementAt(0))->ch;
    b1 = ((Element*)elementList->elementAt(0))->str;
    w2 = ((Element*)elementList->elementAt(1))->ch;
    b2 = ((Element*)elementList->elementAt(1))->str;
    w3 = ((Element*)elementList->elementAt(2))->ch;
    b3 = ((Element*)elementList->elementAt(2))->str;
    w4 = ((Element*)elementList->elementAt(3))->ch;
    b4 = ((Element*)elementList->elementAt(3))->str;
    w5 = ((Element*)elementList->elementAt(4))->ch;
    b5 = ((Element*)elementList->elementAt(4))->str;
    w6 = ((Element*)elementList->elementAt(5))->ch;
    b6 = ((Element*)elementList->elementAt(5))->str;

    UChar32 arr[4] = { p1, -1, -1, -1 };
    featureList->adoptElement(concatChar("UP1:", arr, status), status);

    arr[0] = p2;
    featureList->adoptElement(concatChar("UP2:", arr, status), status);

    arr[0] = p3;
    featureList->adoptElement(concatChar("UP3:", arr, status), status);

    arr[0] = p1;
    arr[1] = p2;
    featureList->adoptElement(concatChar("BP1:", arr, status), status);

    arr[0] = p2;
    arr[1] = p3;
    featureList->adoptElement(concatChar("BP2:", arr, status), status);

    if (w1 != INVALID) {
        arr[0] = w1;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW1:", arr, status), status);
    }
    if (w2 != INVALID) {
        arr[0] = w2;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW2:", arr, status), status);
    }
    if (w3 != INVALID) {
        arr[0] = w3;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW3:", arr, status), status);
    }
    if (w4 != INVALID) {
        arr[0] = w4;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW4:", arr, status), status);
    }
    if (w5 != INVALID) {
        arr[0] = w5;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW5:", arr, status), status);
    }
    if (w6 != INVALID) {
        arr[0] = w6;
        arr[1] = -1;
        featureList->adoptElement(concatChar("UW6:", arr, status), status);
    }
    if (w2 != INVALID && w3 != INVALID) {
        arr[0] = w2;
        arr[1] = w3;
        arr[2] = -1;
        featureList->adoptElement(concatChar("BW1:", arr, status), status);
    }
    if (w3 != INVALID && w4 != INVALID) {
        arr[0] = w3;
        arr[1] = w4;
        arr[2] = -1;
        featureList->adoptElement(concatChar("BW2:", arr, status), status);
    }
    if (w4 != INVALID && w5 != INVALID) {
        arr[0] = w4;
        arr[1] = w5;
        arr[2] = -1;
        featureList->adoptElement(concatChar("BW3:", arr, status), status);
    }
    if (w1 != INVALID && w2 != INVALID && w3 != INVALID) {
        arr[0] = w1;
        arr[1] = w2;
        arr[2] = w3;
        arr[3] = -1;
        featureList->adoptElement(concatChar("TW1:", arr, status), status);
    }
    if (w2 != INVALID && w3 != INVALID && w4 != INVALID) {
        arr[0] = w2;
        arr[1] = w3;
        arr[2] = w4;
        arr[3] = -1;
        featureList->adoptElement(concatChar("TW2:", arr, status), status);
    }
    if (w3 != INVALID && w4 != INVALID && w5 != INVALID) {
        arr[0] = w3;
        arr[1] = w4;
        arr[2] = w5;
        arr[3] = -1;
        featureList->adoptElement(concatChar("TW3:", arr, status), status);
    }
    if (w4 != INVALID && w5 != INVALID && w6 != INVALID) {
        arr[0] = w4;
        arr[1] = w5;
        arr[2] = w6;
        arr[3] = -1;
        featureList->adoptElement(concatChar("TW4:", arr, status), status);
    }
    if (b1 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB1:", b1, status), status);
    }
    if (b2 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB2:", b2, status), status);
    }
    if (b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB3:", b3, status), status);
    }
    if (b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB4:", b4, status), status);
    }
    if (b5 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB5:", b5, status), status);
    }
    if (b6 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UB6:", b6, status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BB1:", UnicodeString(b2).append(b3), status), status);
    }
    if (b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BB2:", UnicodeString(b3).append(b4), status), status);
    }
    if (b4 != UnicodeString(INVALID) && b5 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BB3:", UnicodeString(b4).append(b5), status), status);
    }
    if (b1 != UnicodeString(INVALID) && b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TB1:", UnicodeString(b1).append(b2).append(b3), status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TB2:", UnicodeString(b2).append(b3).append(b4), status), status);
    }
    if (b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID) && b5 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TB3:", UnicodeString(b3).append(b4).append(b5), status), status);
    }
    if (b4 != UnicodeString(INVALID) && b5 != UnicodeString(INVALID) && b6 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TB4:", UnicodeString(b4).append(b5).append(b6), status), status);
    }
    if (b1 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UQ1:", UnicodeString(p1).append(b1), status), status);
    }
    if (b2 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UQ2:", UnicodeString(p2).append(b2), status), status);
    }
    if (b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("UQ3:", UnicodeString(p3).append(b3), status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BQ1:", UnicodeString(p2).append(b2).append(b3), status), status);
    }
    if (b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BQ2:", UnicodeString(p2).append(b3).append(b4), status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BQ3:", UnicodeString(p3).append(b2).append(b3), status), status);
    }
    if (b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("BQ4:", UnicodeString(p3).append(b3).append(b4), status), status);
    }
    if (b1 != UnicodeString(INVALID) && b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TQ1:", UnicodeString(p2).append(b1).append(b2).append(b3), status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TQ2:", UnicodeString(p2).append(b2).append(b3).append(b4), status), status);
    }
    if (b1 != UnicodeString(INVALID) && b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TQ3:", UnicodeString(p3).append(b1).append(b2).append(b3), status), status);
    }
    if (b2 != UnicodeString(INVALID) && b3 != UnicodeString(INVALID) && b4 != UnicodeString(INVALID)) {
        featureList->adoptElement(concatUnicodeString("TQ4:", UnicodeString(p3).append(b2).append(b3).append(b4), status), status);
    }
}

/**
 * Convert an integer, positive or negative, to a character string radix 10.
 */
static char* itoa(int32_t i, char* buf) {
    char* result = buf;
    // Handle negative
    if (i < 0) {
        *buf++ = '-';
        i = -i;
    }
    // Output digits in reverse order
    char* p = buf;
    do {
        *p++ = (char)('0' + (i % 10));
        i /= 10;
    } while (i);
    *p-- = 0;
    // Reverse the string
    while (buf < p) {
        char c = *buf;
        *buf++ = *p;
        *p-- = c;
    }

    return result;
}

UnicodeString CjMLBreakEngine::getUnicodeBlockIndex(UChar32 ch, UErrorCode& status) const{
    U_ASSERT(fUnicodeBlock->size() > 0);
    if (U_FAILURE(status)) {
        return UnicodeString();
    }
    if (ch == INVALID) {
        return UnicodeString(INVALID);
    }
    int32_t start = 0;
    int32_t end = fUnicodeBlock->size();
    while (start < end) {
        int32_t mid = (start + end) / 2;
        if (ch < fUnicodeBlock->elementAti(mid)) {
            end = mid;
        }
        else {
            start = mid + 1;
        }
    }
    //Add '0' to fill up three digits
    char buff[32];
    itoa(start, buff);
    UnicodeString result(buff);
    if (result.length() < 3) {
        int32_t preZeroCount = 3 - result.length();
        for (int i = 0; i < preZeroCount; i++) {
            result.insert(0, '0');
        }
    }
    return result;
}

void CjMLBreakEngine::loadMLparams(UErrorCode& error) {
    const char* tags[] = { "model", "unicode_blocks" };
    ResourceBundle ja(U_ICUDATA_BRKITR, "ja", error);
    UnicodeString key, value;
    UChar32 delimeter = ':';
    char charBuf[512];
    charBuf[sizeof(charBuf) - 1] = 0;
    if (U_SUCCESS(error)) {
        ResourceBundle bundle = ja.get(tags[0], error);
        while (U_SUCCESS(error) && bundle.hasNext()) {
            UnicodeString temp = bundle.getNextString(error);
            int32_t index = temp.lastIndexOf(delimeter);
            key = temp.tempSubString(0, index);
            value = temp.tempSubString(index + 1, temp.length() - index - 1);
            //transform to integer
            value.extract(0, value.length(), charBuf, sizeof(charBuf) - 1, 0);
            fModel.puti(key, atoi(charBuf), error);
        }
    }
    if (U_SUCCESS(error)) {
        ResourceBundle bundle = ja.get(tags[1], error);
        while (U_SUCCESS(error) && bundle.hasNext()) {
            UnicodeString value = bundle.getNextString(error);
            //transform to integer
            value.extract(0, value.length(), charBuf, sizeof(charBuf) - 1, 0);
            fUnicodeBlock->addElement(atoi(charBuf), error);
        }
    }
}

#endif

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
