// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Fuzzer for ICU bidi.

#include <cstring>

#include "fuzzer_utils.h"

#include "unicode/ubidi.h"

#define MAXLEN 200

static void sub2(UBiDi *bidi) {
  UErrorCode errorCode = U_ZERO_ERROR;
  int32_t i;
  int32_t runCount;
  ubidi_getDirection(bidi);
  ubidi_getParaLevel(bidi);
  ubidi_getReorderingMode(bidi);
  ubidi_getReorderingOptions(bidi);
  runCount = ubidi_countRuns(bidi, &errorCode);
  if (U_FAILURE(errorCode)) {
  } else {
    for (i = 0; i < runCount; i++) {
      int32_t start, len;
      ubidi_getVisualRun(bidi, i, &start, &len);
    }
  }
}

static void matchingPair(UBiDi *bidi, int32_t i) {
  UBiDiLevel level;
  ubidi_getLogicalRun(bidi, i, NULL, &level);
}

static bool sub1(UBiDi *bidi) {
  int32_t i, idx, logLimit, visLimit;
  bool testOK, errMap, errDst;
  UErrorCode errorCode = U_ZERO_ERROR;
  int32_t visMap[MAXLEN];
  int32_t logMap[MAXLEN];
  ubidi_getVisualMap(bidi, visMap, &errorCode);
  ubidi_getLogicalMap(bidi, logMap, &errorCode);
  if (U_FAILURE(errorCode)) {
    return false;
  }

  testOK = true;
  errMap = errDst = false;
  logLimit = ubidi_getProcessedLength(bidi);
  visLimit = ubidi_getResultLength(bidi);
  for (i = 0; i < logLimit; i++) {
    idx = ubidi_getVisualIndex(bidi, i, &errorCode);
    if (idx != logMap[i]) {
      errMap = true;
    }
    if (idx == UBIDI_MAP_NOWHERE) {
      continue;
    }
    if (idx >= visLimit) {
      continue;
    }
    matchingPair(bidi, i);
  }

  if (U_FAILURE(errorCode)) {
    return false;
  }
  if (errMap) {
    if (testOK) {
      sub2(bidi);
      testOK = false;
    }
    for (i = 0; i < logLimit; i++) {
      ubidi_getVisualIndex(bidi, i, &errorCode);
    }
  }

  if (errDst) {
    if (testOK) {
      sub2(bidi);
      testOK = false;
    }
  }

  errMap = errDst = false;
  for (i = 0; i < visLimit; i++) {
    idx = ubidi_getLogicalIndex(bidi, i, &errorCode);
    if (idx != visMap[i]) {
      errMap = true;
    }
    if (idx == UBIDI_MAP_NOWHERE) {
      continue;
    }
    if (idx >= logLimit) {
      continue;
    }
    matchingPair(bidi, idx);
  }
  if (U_FAILURE(errorCode)) {
    return false;
  }
  if (errMap) {
    if (testOK) {
      sub2(bidi);
      testOK = false;
    }
  }
  if (errDst) {
    if (testOK) {
      sub2(bidi);
      testOK = false;
    }
  }
  return testOK;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  icu::StringPiece fuzzData(reinterpret_cast<const char *>(data), size);

  UErrorCode ec = U_ZERO_ERROR;
  UBiDi* bidi = ubidi_open();
  UChar input1[MAXLEN];
  UChar dest[MAXLEN];
  int32_t destSize;
  unsigned char input1size;

  std::memcpy(&input1size, fuzzData.data(), sizeof(input1size));
  fuzzData.remove_prefix(sizeof(input1size));
  if (input1size > MAXLEN) input1size = MAXLEN;

  std::memcpy(&input1, fuzzData.data(), sizeof(UChar)*input1size);
  fuzzData.remove_prefix(sizeof(UChar)*input1size);

  UBiDiLevel input2;
  std::memcpy(&input2, fuzzData.data(), sizeof(input2));
  fuzzData.remove_prefix(sizeof(input2));

  int16_t input3;
  std::memcpy(&input3, fuzzData.data(), sizeof(input3));
  fuzzData.remove_prefix(sizeof(input3));

  unsigned char input4;
  std::memcpy(&input4, fuzzData.data(), sizeof(input4));
  fuzzData.remove_prefix(sizeof(input4));
  if (input4 > MAXLEN) input4 = MAXLEN;

  bool flow1, flow2;
  flow1 = (*(fuzzData.data()) & 0x01) == 0;
  fuzzData.remove_prefix(sizeof(flow1));

  flow2 = (*(fuzzData.data()) & 0x01) == 0;
  fuzzData.remove_prefix(sizeof(flow2));

  if (flow1) {
    bool input1_1_1;
    std::memcpy(&input1_1_1, fuzzData.data(), sizeof(input1_1_1));
    fuzzData.remove_prefix(sizeof(input1_1_1));
    ubidi_setInverse(bidi, input1_1_1);
  }
  ubidi_setPara(bidi, input1, input1size, input2, NULL, &ec);
  if (U_FAILURE(ec)) goto out;

  destSize = ubidi_writeReordered(bidi, dest, input4, input3, &ec);
  if (U_FAILURE(ec)) goto out;

  if (flow2) {
    sub1(bidi);
  } else {
    int16_t input2_2_1;
    std::memcpy(&input2_2_1, fuzzData.data(), sizeof(input2_2_1));
    fuzzData.remove_prefix(sizeof(input2_2_1));
    ubidi_writeReordered(bidi, dest, destSize+1, input2_2_1, &ec);
  }

out:
  ubidi_close(bidi);

  return EXIT_SUCCESS;
}
