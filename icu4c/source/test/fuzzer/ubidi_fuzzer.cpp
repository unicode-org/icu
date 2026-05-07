// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU bidi.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/ubidi.h"

#define MAXLEN 200

static void testGetVisualRun(UBiDi *bidi) {
  UErrorCode status = U_ZERO_ERROR;
  ubidi_getDirection(bidi);
  ubidi_getParaLevel(bidi);
  ubidi_getReorderingMode(bidi);
  ubidi_getReorderingOptions(bidi);
  int32_t runCount = ubidi_countRuns(bidi, &status);
  if (U_FAILURE(status)) {
    return;
  }
  for (int32_t runIndex = 0; runIndex < runCount; runIndex++) {
    int32_t start, len;
    ubidi_getVisualRun(bidi, runIndex, &start, &len);
  }
}


static void testVisual(UBiDi *bidi) {
  UErrorCode status = U_ZERO_ERROR;
  int32_t visualToLogical[MAXLEN];
  int32_t logicalToVisual[MAXLEN];
  int32_t logicalLimit = ubidi_getProcessedLength(bidi);
  int32_t visualLimit = ubidi_getResultLength(bidi);
  if (visualLimit > MAXLEN) {
    return;
  }
  ubidi_getVisualMap(bidi, visualToLogical, &status);
  ubidi_getLogicalMap(bidi, logicalToVisual, &status);
  if (U_FAILURE(status)) {
    return;
  }

  bool mapIsIncorrect = false;
  for (int32_t logicalIndex = 0; logicalIndex < logicalLimit; logicalIndex++) {
    int32_t visualIndex = ubidi_getVisualIndex(bidi, logicalIndex, &status);
    if (visualIndex != logicalToVisual[logicalIndex]) {
      mapIsIncorrect = true;
    }
    if (visualIndex != UBIDI_MAP_NOWHERE && visualIndex < visualLimit) {
        UBiDiLevel level;
        ubidi_getLogicalRun(bidi, logicalIndex, nullptr, &level);
    }
  }

  if (U_FAILURE(status)) {
    return;
  }
  bool checkGetVisualRun = true;
  if (mapIsIncorrect) {
    testGetVisualRun(bidi);
    checkGetVisualRun = false;
    for (int32_t logicalIndex = 0; logicalIndex < logicalLimit; logicalIndex++) {
      ubidi_getVisualIndex(bidi, logicalIndex, &status);
    }
  }

  mapIsIncorrect = false;
  for (int32_t visualIndex = 0; visualIndex < visualLimit; visualIndex++) {
    int32_t logicalIndex = ubidi_getLogicalIndex(bidi, visualIndex, &status);
    if (logicalIndex != visualToLogical[visualIndex]) {
      mapIsIncorrect = true;
    }
    if (logicalIndex != UBIDI_MAP_NOWHERE && logicalIndex < logicalLimit) {
      UBiDiLevel level;
      ubidi_getLogicalRun(bidi, logicalIndex, nullptr, &level);
    }
  }
  if (U_FAILURE(status)) {
    return;
  }
  if (mapIsIncorrect && checkGetVisualRun) {
    testGetVisualRun(bidi);
  }
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

  UErrorCode status = U_ZERO_ERROR;
  UChar inputText[MAXLEN];
  UChar reorderedText[MAXLEN];

  unsigned char inputTextLength;
  UBiDiLevel paraLevel;
  int16_t options, options2;
  unsigned char reorderedTextSize;
  bool checkSetInverse, checkVisual;
  bool isInverse;

  if (static_cast<size_t>(fuzzData.length()) <
      sizeof(inputTextLength) + sizeof(paraLevel) + sizeof(options) +
      sizeof(reorderedTextSize) + sizeof(checkSetInverse) + sizeof(checkVisual) +
      sizeof(isInverse) + sizeof(options2) + sizeof(UChar)) {
    return 0;
  }
  std::memcpy(&inputTextLength, fuzzData.data(), sizeof(inputTextLength));
  fuzzData.remove_prefix(sizeof(inputTextLength));

  std::memcpy(&paraLevel, fuzzData.data(), sizeof(paraLevel));
  fuzzData.remove_prefix(sizeof(paraLevel));

  std::memcpy(&options, fuzzData.data(), sizeof(options));
  fuzzData.remove_prefix(sizeof(options));

  std::memcpy(&reorderedTextSize, fuzzData.data(), sizeof(reorderedTextSize));
  fuzzData.remove_prefix(sizeof(reorderedTextSize));
  if (reorderedTextSize > MAXLEN) reorderedTextSize = MAXLEN;

  checkSetInverse = (*(fuzzData.data()) & 0x01) == 0;
  fuzzData.remove_prefix(sizeof(checkSetInverse));

  checkVisual = (*(fuzzData.data()) & 0x01) == 0;
  fuzzData.remove_prefix(sizeof(checkVisual));

  isInverse = (*(fuzzData.data()) & 0x01) != 0;
  fuzzData.remove_prefix(sizeof(isInverse));

  std::memcpy(&options2, fuzzData.data(), sizeof(options2));
  fuzzData.remove_prefix(sizeof(options2));

  if (inputTextLength > MAXLEN) inputTextLength = MAXLEN;
  if (inputTextLength > fuzzData.length() / sizeof(UChar)) {
      inputTextLength = fuzzData.length() / sizeof(UChar);
  }
  std::memcpy(&inputText, fuzzData.data(), sizeof(UChar)*inputTextLength);
  fuzzData.remove_prefix(sizeof(UChar)*inputTextLength);

  UBiDi* bidi = ubidi_open();
  if (checkSetInverse) {
    ubidi_setInverse(bidi, isInverse);
  }
  ubidi_setPara(bidi, inputText, inputTextLength, paraLevel, nullptr, &status);
  if (U_SUCCESS(status)) {
    int32_t reorderedTextLength = ubidi_writeReordered(bidi, reorderedText, reorderedTextSize, options, &status);
    if (U_SUCCESS(status)) {
      if (checkVisual) {
        testVisual(bidi);
      } else {
        ubidi_writeReordered(bidi, reorderedText, reorderedTextLength+1, options2, &status);
      }
    }
  }
  ubidi_close(bidi);

  return EXIT_SUCCESS;
}
