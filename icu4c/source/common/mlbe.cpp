// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "cmemory.h"
#include "mlbe.h"
#include "uassert.h"
#include "ubrkimpl.h"
#include "unicode/resbund.h"
#include "unicode/udata.h"
#include "unicode/utf16.h"
#include "uresimp.h"
#include "util.h"
#include "uvectr32.h"

U_NAMESPACE_BEGIN

Element::Element() : length(0) {}

void Element::setCharAndUblock(UChar32 ch, const UnicodeString &idx) {
    character = ch;
    U_ASSERT(idx.length() <= 3);
    length = idx.length();
    idx.extract(0, length, ublock);
    ublock[length] = '\0';
}

UChar32 Element::getCharacter() const {
    return character;
}

char16_t* Element::getUblock() const {
    return (char16_t*)ublock;
}

uint16_t Element::getLength() const {
    return length;
}

MlBreakEngine::MlBreakEngine(const UnicodeSet &digitOrOpenPunctuationOrAlphabetSet,
                                 const UnicodeSet &closePunctuationSet, UErrorCode &status)
    : fDigitOrOpenPunctuationOrAlphabetSet(digitOrOpenPunctuationOrAlphabetSet),
      fClosePunctuationSet(closePunctuationSet),
      fModel(status),
      fNegativeSum(0) {
    if (U_FAILURE(status)) {
        return;
    }
    loadMLModel(status);
}

MlBreakEngine::~MlBreakEngine() {}

namespace {
    const char16_t INVALID = u'|';
    const int32_t MAX_FEATURE = 26;
    const int32_t MAX_FEATURE_LENGTH = 14;

    bool isValid(const Element& element) {
        return element.getLength() != 1 || element.getUblock()[0] != INVALID;
    }

    void concatChar(const char16_t *str, const UChar32 *arr, int32_t length, char16_t *feature, UErrorCode &status) {
        if (U_FAILURE(status)) {
            return;
        }
        UnicodeString result(str);
        for (int i = 0; i < length; i++) {
            result.append(arr[i]);
        }
        U_ASSERT(result.length() < MAX_FEATURE_LENGTH);
        result.extract(feature, MAX_FEATURE_LENGTH, status);  // NUL-terminates
    }

    void writeString(const UnicodeString &str, char16_t *feature, UErrorCode &status) {
        U_ASSERT(str.length() < MAX_FEATURE_LENGTH);
        str.extract(feature, MAX_FEATURE_LENGTH, status);  // NUL-terminates
    }
}

int32_t MlBreakEngine::divideUpRange(UText *inText, int32_t rangeStart, int32_t rangeEnd,
                                       UVector32 &foundBreaks, const UnicodeString &inString,
                                       const LocalPointer<UVector32> &inputMap,
                                       UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    if (rangeStart >= rangeEnd) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    UVector32 boundary(inString.countChar32() + 1, status);
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t numBreaks = 0;
    UChar32 ch;
    UnicodeString index;
    // The ML model groups six char to evaluate if the 4th char is a breakpoint.
    // Like a sliding window, the elementList removes the first char and appends the new char from
    // inString in each iteration so that its size always remains at six.
    Element elementList[6];

    int32_t codeUts = initElementList(inString, elementList, status);
    int32_t length = inString.countChar32();

    // Add a break for the start.
    boundary.addElement(0, status);
    numBreaks++;
    if (U_FAILURE(status)) return 0;

    for (int32_t i = 1; i < length && U_SUCCESS(status); i++) {
        evaluateBreakpoint(elementList, i, numBreaks, boundary, status);
        if (i + 1 >= inString.countChar32()) break;
        // Remove the first element and append a new element
        uprv_memmove(elementList, elementList + 1, 5 * sizeof(Element));
        ch = inString.countChar32(0, codeUts) < length ? inString.char32At(codeUts) : INVALID;
        index = (ch != INVALID) ? getUnicodeBlock(ch, status) : UnicodeString(INVALID);
        elementList[5].setCharAndUblock(ch, index);
        if (ch != INVALID) {
            codeUts += U16_LENGTH(ch);
        }
    }
    if (U_FAILURE(status)) return 0;

    // Add a break for the end if there is not one there already.
    if (boundary.lastElementi() != inString.countChar32()) {
        boundary.addElement(inString.countChar32(), status);
        numBreaks++;
    }

    int32_t prevCPPos = -1;
    int32_t prevUTextPos = -1;
    int32_t correctedNumBreaks = 0;
    for (int32_t i = 0; i < numBreaks; i++) {
        int32_t cpPos = boundary.elementAti(i);
        int32_t utextPos = inputMap.isValid() ? inputMap->elementAti(cpPos) : cpPos + rangeStart;
        U_ASSERT(cpPos > prevCPPos);
        U_ASSERT(utextPos >= prevUTextPos);

        if (utextPos > prevUTextPos) {
            if (utextPos != rangeStart ||
                (utextPos > 0 &&
                 fClosePunctuationSet.contains(utext_char32At(inText, utextPos - 1)))) {
                foundBreaks.push(utextPos, status);
                correctedNumBreaks++;
            }
        } else {
            // Normalization expanded the input text, the dictionary found a boundary
            // within the expansion, giving two boundaries with the same index in the
            // original text. Ignore the second. See ticket #12918.
            --numBreaks;
        }
        prevCPPos = cpPos;
        prevUTextPos = utextPos;
    }
    (void)prevCPPos;  // suppress compiler warnings about unused variable

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

void MlBreakEngine::evaluateBreakpoint(Element* elementList, int32_t index, int32_t &numBreaks,
                                         UVector32 &boundary, UErrorCode &status) const {
    char16_t featureList[MAX_FEATURE][MAX_FEATURE_LENGTH];
    if (U_FAILURE(status)) {
        return;
    }

    UChar32 arr[4] = {-1, -1, -1, -1};
    int32_t length = 0, listLength = 0;

    const UChar32 w1 = elementList[0].getCharacter();
    const UChar32 w2 = elementList[1].getCharacter();
    const UChar32 w3 = elementList[2].getCharacter();
    const UChar32 w4 = elementList[3].getCharacter();
    const UChar32 w5 = elementList[4].getCharacter();
    const UChar32 w6 = elementList[5].getCharacter();

    length = 1;
    if (w1 != INVALID) {
        arr[0] = w1;
        concatChar(u"UW1:", arr, length, featureList[listLength++], status);
    }
    if (w2 != INVALID) {
        arr[0] = w2;
        concatChar(u"UW2:", arr, length, featureList[listLength++], status);
    }
    if (w3 != INVALID) {
        arr[0] = w3;
        concatChar(u"UW3:", arr, length, featureList[listLength++], status);
    }
    if (w4 != INVALID) {
        arr[0] = w4;
        concatChar(u"UW4:", arr, length, featureList[listLength++], status);
    }
    if (w5 != INVALID) {
        arr[0] = w5;
        concatChar(u"UW5:", arr, length, featureList[listLength++], status);
    }
    if (w6 != INVALID) {
        arr[0] = w6;
        concatChar(u"UW6:", arr, length, featureList[listLength++], status);
    }
    length = 2;
    if (w2 != INVALID && w3 != INVALID) {
        arr[0] = w2;
        arr[1] = w3;
        concatChar(u"BW1:", arr, length, featureList[listLength++], status);
    }
    if (w3 != INVALID && w4 != INVALID) {
        arr[0] = w3;
        arr[1] = w4;
        concatChar(u"BW2:", arr, length, featureList[listLength++], status);
    }
    if (w4 != INVALID && w5 != INVALID) {
        arr[0] = w4;
        arr[1] = w5;
        concatChar(u"BW3:", arr, length, featureList[listLength++], status);
    }
    length = 3;
    if (w1 != INVALID && w2 != INVALID && w3 != INVALID) {
        arr[0] = w1;
        arr[1] = w2;
        arr[2] = w3;
        concatChar(u"TW1:", arr, length, featureList[listLength++], status);
    }
    if (w2 != INVALID && w3 != INVALID && w4 != INVALID) {
        arr[0] = w2;
        arr[1] = w3;
        arr[2] = w4;
        concatChar(u"TW2:", arr, length, featureList[listLength++], status);
    }
    if (w3 != INVALID && w4 != INVALID && w5 != INVALID) {
        arr[0] = w3;
        arr[1] = w4;
        arr[2] = w5;
        concatChar(u"TW3:", arr, length, featureList[listLength++], status);
    }
    if (w4 != INVALID && w5 != INVALID && w6 != INVALID) {
        arr[0] = w4;
        arr[1] = w5;
        arr[2] = w6;
        concatChar(u"TW4:", arr, length, featureList[listLength++], status);
    }
    if (isValid(elementList[0])) {
        writeString(UnicodeString(u"UB1:").append(elementList[0].getUblock(), 0,
                                                  elementList[0].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[1])) {
        writeString(UnicodeString(u"UB2:").append(elementList[1].getUblock(), 0,
                                                  elementList[1].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[2])) {
        writeString(UnicodeString(u"UB3:").append(elementList[2].getUblock(), 0,
                                                  elementList[2].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[3])) {
        writeString(UnicodeString(u"UB4:").append(elementList[3].getUblock(), 0,
                                                  elementList[3].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[4])) {
        writeString(UnicodeString(u"UB5:").append(elementList[4].getUblock(), 0,
                                                  elementList[4].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[5])) {
        writeString(UnicodeString(u"UB6:").append(elementList[5].getUblock(), 0,
                                                  elementList[5].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[1]) && isValid(elementList[2])) {
        writeString(UnicodeString(u"BB1:")
                        .append(elementList[1].getUblock(), 0, elementList[1].getLength())
                        .append(elementList[2].getUblock(), 0, elementList[2].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[2]) && isValid(elementList[3])) {
        writeString(UnicodeString(u"BB2:")
                        .append(elementList[2].getUblock(), 0, elementList[2].getLength())
                        .append(elementList[3].getUblock(), 0, elementList[3].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[3]) && isValid(elementList[4])) {
        writeString(UnicodeString(u"BB3:")
                        .append(elementList[3].getUblock(), 0, elementList[3].getLength())
                        .append(elementList[4].getUblock(), 0, elementList[4].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[0]) && isValid(elementList[1]) && isValid(elementList[2])) {
        writeString(UnicodeString(u"TB1:")
                        .append(elementList[0].getUblock(), 0, elementList[0].getLength())
                        .append(elementList[1].getUblock(), 0, elementList[1].getLength())
                        .append(elementList[2].getUblock(), 0, elementList[2].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[1]) && isValid(elementList[2]) && isValid(elementList[3])) {
        writeString(UnicodeString(u"TB2:")
                        .append(elementList[1].getUblock(), 0, elementList[1].getLength())
                        .append(elementList[2].getUblock(), 0, elementList[2].getLength())
                        .append(elementList[3].getUblock(), 0, elementList[3].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[2]) && isValid(elementList[3]) && isValid(elementList[4])) {
        writeString(UnicodeString(u"TB3:")
                        .append(elementList[2].getUblock(), 0, elementList[2].getLength())
                        .append(elementList[3].getUblock(), 0, elementList[3].getLength())
                        .append(elementList[4].getUblock(), 0, elementList[4].getLength()),
                    featureList[listLength++], status);
    }
    if (isValid(elementList[3]) && isValid(elementList[4]) && isValid(elementList[5])) {
        writeString(UnicodeString(u"TB4:")
                        .append(elementList[3].getUblock(), 0, elementList[3].getLength())
                        .append(elementList[4].getUblock(), 0, elementList[4].getLength())
                        .append(elementList[5].getUblock(), 0, elementList[5].getLength()),
                    featureList[listLength++], status);
    }
    if (U_FAILURE(status)) {
        return;
    }
    int32_t score = fNegativeSum;
    for (int32_t j = 0; j < listLength; j++) {
        UnicodeString key(featureList[j]);
        if (fModel.containsKey(key)) {
            score += (2 * fModel.geti(key));
        }
    }
    if (score > 0) {
        boundary.addElement(index, status);
        numBreaks++;
    }
}

int32_t MlBreakEngine::initElementList(const UnicodeString &inString, Element* elementList,
                                         UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return 0;
    }
    int32_t index = 0;
    int32_t length = inString.countChar32();
    UChar32 w1, w2, w3, w4, w5, w6;
    w1 = w2 = w3 = w4 = w5 = w6 = INVALID;
    if (length > 0) {
        w3 = inString.char32At(0);
        index += U16_LENGTH(w3);
    }
    if (length > 1) {
        w4 = inString.char32At(index);
        index += U16_LENGTH(w4);
    }
    if (length > 2) {
        w5 = inString.char32At(index);
        index += U16_LENGTH(w5);
    }
    if (length > 3) {
        w6 = inString.char32At(index);
        index += U16_LENGTH(w6);
    }

    const UnicodeString b1(INVALID);
    const UnicodeString b2(b1);
    const UnicodeString b3(getUnicodeBlock(w3, status));
    const UnicodeString b4(getUnicodeBlock(w4, status));
    const UnicodeString b5(getUnicodeBlock(w5, status));
    const UnicodeString b6(getUnicodeBlock(w6, status));

    elementList[0].setCharAndUblock(w1, b1);
    elementList[1].setCharAndUblock(w2, b2);
    elementList[2].setCharAndUblock(w3, b3);
    elementList[3].setCharAndUblock(w4, b4);
    elementList[4].setCharAndUblock(w5, b5);
    elementList[5].setCharAndUblock(w6, b6);

    return index;
}

UnicodeString MlBreakEngine::getUnicodeBlock(UChar32 ch, UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return UnicodeString(INVALID);
    }

    UBlockCode block = ublock_getCode(ch);
    if (block == UBLOCK_NO_BLOCK || block == UBLOCK_INVALID_CODE) {
        return UnicodeString(INVALID);
    } else {
        UnicodeString empty;
        // Same as sprintf("%03d", block)
        return ICU_Utility::appendNumber(empty, (int32_t)block, 10, 3);
    }
}

void MlBreakEngine::loadMLModel(UErrorCode &error) {
    // BudouX's model consists of pairs of the feature and its score.
    // As integrating it into jaml.txt, modelKeys denotes the ML feature; modelValues means the
    // corresponding feature's score.

    if (U_FAILURE(error)) return;

    int32_t keySize = 0;
    int32_t valueSize = 0;
    int32_t stringLength = 0;
    UnicodeString key;
    StackUResourceBundle stackTempBundle;
    ResourceDataValue modelKey;

    LocalUResourceBundlePointer rbp(ures_openDirect(U_ICUDATA_BRKITR, "jaml", &error));
    UResourceBundle* rb = rbp.orphan();
    // get modelValues
    LocalUResourceBundlePointer modelValue(ures_getByKey(rb, "modelValues", nullptr, &error));
    const int32_t* value = ures_getIntVector(modelValue.getAlias(), &valueSize, &error);
    if (U_FAILURE(error)) return;

    // get modelKeys
    ures_getValueWithFallback(rb, "modelKeys", stackTempBundle.getAlias(), modelKey, error);
    ResourceArray stringArray = modelKey.getArray(error);
    keySize = stringArray.getSize();
    if (U_FAILURE(error)) return;

    for (int32_t idx = 0; idx < keySize; idx++) {
        stringArray.getValue(idx, modelKey);
        key = UnicodeString(modelKey.getString(stringLength, error));
        if (U_SUCCESS(error)) {
            U_ASSERT(idx < valueSize);
            fNegativeSum -= value[idx];
            fModel.puti(key, value[idx], error);
        }
    }
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
