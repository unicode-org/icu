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
    UnicodeString index;
    // The ML model groups six char to evaluate if the 4th char is a breakpoint.
    // Like a sliding window, the elementList removes the first char and appends the new char from
    // inString in each iteration so that its size always remains at six.
    UChar32 elementList[6];

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
        uprv_memmove(elementList, elementList + 1, 5 * sizeof(UChar32));
        elementList[5] = inString.countChar32(0, codeUts) < length ? inString.char32At(codeUts) : INVALID;
        if (elementList[5] != INVALID) {
            codeUts += U16_LENGTH(elementList[5]);
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

void MlBreakEngine::evaluateBreakpoint(UChar32* elementList, int32_t index, int32_t &numBreaks,
                                         UVector32 &boundary, UErrorCode &status) const {
    if (U_FAILURE(status)) {
        return;
    }

    UnicodeString feature;
    int32_t score = fNegativeSum;

    if (elementList[0] != INVALID) {
        // When the key doesn't exist, Hashtable.geti(key) returns 0  and 2 * 0 = 0.
        // So, we can skip to check whether fModel includes key featureList[j] or not.
        score += (2 * fModel.geti(feature.setTo(u"UW1:", 4).append(elementList[0])));
    }
    if (elementList[1] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"UW2:", 4).append(elementList[1])));
    }
    if (elementList[2] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"UW3:", 4).append(elementList[2])));
    }
    if (elementList[3] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"UW4:", 4).append(elementList[3])));
    }
    if (elementList[4] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"UW5:", 4).append(elementList[4])));
    }
    if (elementList[5] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"UW6:", 4).append(elementList[5])));
    }
    if (elementList[1] != INVALID && elementList[2] != INVALID) {
        score += (2 * fModel.geti(
                          feature.setTo(u"BW1:", 4).append(elementList[1]).append(elementList[2])));
    }
    if (elementList[2] != INVALID && elementList[3] != INVALID) {
        score += (2 * fModel.geti(
                          feature.setTo(u"BW2:", 4).append(elementList[2]).append(elementList[3])));
    }
    if (elementList[3] != INVALID && elementList[4] != INVALID) {
        score += (2 * fModel.geti(
                          feature.setTo(u"BW3:", 4).append(elementList[3]).append(elementList[4])));
    }
    if (elementList[0] != INVALID && elementList[1] != INVALID && elementList[2] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"TW1:", 4)
                                      .append(elementList[0])
                                      .append(elementList[1])
                                      .append(elementList[2])));
    }
    if (elementList[1] != INVALID && elementList[2] != INVALID && elementList[3] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"TW2:", 4)
                                      .append(elementList[1])
                                      .append(elementList[2])
                                      .append(elementList[3])));
    }
    if (elementList[2] != INVALID && elementList[3] != INVALID && elementList[4] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"TW3:", 4)
                                      .append(elementList[2])
                                      .append(elementList[3])
                                      .append(elementList[4])));
    }
    if (elementList[3] != INVALID && elementList[4] != INVALID && elementList[5] != INVALID) {
        score += (2 * fModel.geti(feature.setTo(u"TW4:", 4)
                                      .append(elementList[3])
                                      .append(elementList[4])
                                      .append(elementList[5])));
    }
    if (score > 0) {
        boundary.addElement(index, status);
        numBreaks++;
    }
}

int32_t MlBreakEngine::initElementList(const UnicodeString &inString, UChar32* elementList,
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
        if (length > 1) {
            w4 = inString.char32At(index);
            index += U16_LENGTH(w4);
            if (length > 2) {
                w5 = inString.char32At(index);
                index += U16_LENGTH(w5);
                if (length > 3) {
                    w6 = inString.char32At(index);
                    index += U16_LENGTH(w6);
                }
            }
        }
    }
    elementList[0] = w1;
    elementList[1] = w2;
    elementList[2] = w3;
    elementList[3] = w4;
    elementList[4] = w5;
    elementList[5] = w6;

    return index;
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
