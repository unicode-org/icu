/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2012, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/* Modification History:
*/

#include <stdlib.h>

#include "intltest.h"
#include "unicode/gender.h"
#include "unicode/unum.h"

#define LENGTHOF(array) (int32_t)(sizeof(array) / sizeof((array)[0]))

UGender kSingleFemale[] = {UGENDER_FEMALE};
UGender kSingleMale[] = {UGENDER_MALE};
UGender kSingleOther[] = {UGENDER_OTHER};

UGender kAllFemale[] = {UGENDER_FEMALE, UGENDER_FEMALE};
UGender kAllMale[] = {UGENDER_MALE, UGENDER_MALE};
UGender kAllOther[] = {UGENDER_OTHER, UGENDER_OTHER};

UGender kFemaleMale[] = {UGENDER_FEMALE, UGENDER_MALE};
UGender kFemaleOther[] = {UGENDER_FEMALE, UGENDER_OTHER};
UGender kMaleOther[] = {UGENDER_MALE, UGENDER_OTHER};


class GenderInfoTest : public IntlTest {
public:
    GenderInfoTest() {
    }

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par=0);
private:
    void TestGetListGender();
    void TestFallback();
    void TestCApi();
    void check(UGender expected_neutral, UGender expected_mixed, UGender expected_taints, const UGender* genderList, int32_t listSize);
    void checkLocale(const Locale& locale, UGender expected, const UGender* genderList, int32_t listSize);
};

void GenderInfoTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char *par) {
  switch(index) {
    case 0:
      name = "TestGetListGender";
      if (exec) {
        TestGetListGender();
      }
      break;
    case 1:
      name = "TestFallback";
      if (exec) {
        TestFallback();
      }
      break;
    case 2:
      name = "TestCApi";
      if (exec) {
        TestCApi();
      }
      break;
    default: name = ""; break;
  }
}

void GenderInfoTest::TestGetListGender() {
    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_OTHER, NULL, 0);
    // JAVA version always returns OTHER if gender style is NEUTRAL. Is this
    // really correct?
    check(UGENDER_OTHER, UGENDER_FEMALE, UGENDER_FEMALE, kSingleFemale, LENGTHOF(kSingleFemale));
    check(UGENDER_OTHER, UGENDER_MALE, UGENDER_MALE, kSingleMale, LENGTHOF(kSingleMale));
    // JAVA version has MALE_TAINTS return OTHER for {OTHER}. Is this really correct?
    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_OTHER, kSingleOther, LENGTHOF(kSingleOther));

    check(UGENDER_OTHER, UGENDER_FEMALE, UGENDER_FEMALE, kAllFemale, LENGTHOF(kAllFemale));
    check(UGENDER_OTHER, UGENDER_MALE, UGENDER_MALE, kAllMale, LENGTHOF(kAllMale));
    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_MALE, kAllOther, LENGTHOF(kAllOther));

    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_MALE, kFemaleMale, LENGTHOF(kFemaleMale));
    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_MALE, kFemaleOther, LENGTHOF(kFemaleOther));
    check(UGENDER_OTHER, UGENDER_OTHER, UGENDER_MALE, kMaleOther, LENGTHOF(kMaleOther));
}

void GenderInfoTest::TestFallback() {
  UErrorCode status = U_ZERO_ERROR;
  const GenderInfo* actual = GenderInfo::getInstance(Locale::createFromName("xx"), status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to create GenderInfo - %s", u_errorName(status));
    return;
  }
  const GenderInfo* expected = GenderInfo::getNeutralInstance();
  if (expected != actual) {
    errln("For Neutral, expected %d got %d", expected, actual);
  }
  actual = GenderInfo::getInstance(Locale::createFromName("fr_CA"), status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to create GenderInfo - %s", u_errorName(status));
    return;
  }
  expected = GenderInfo::getMaleTaintsInstance();
  if (expected != actual) {
    errln("For Male Taints, Expected %d got %d", expected, actual);
  }
}

void GenderInfoTest::TestCApi() {
  UErrorCode status = U_ZERO_ERROR;
  const UGenderInfo* actual_gi = ugender_getInstance("fr_CA", &status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to create UGenderInfo - %s", u_errorName(status));
    return;
  }
  const UGenderInfo* expected_gi = (const UGenderInfo*) GenderInfo::getMaleTaintsInstance();
  if (expected_gi != actual_gi) {
    errln("Expected UGenderInfo %d got %d", expected_gi, actual_gi);
    return;
  }
  UGender actual = ugender_getListGender(actual_gi, kAllFemale, LENGTHOF(kAllFemale), &status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to create UGenderInfo - %s", u_errorName(status));
    return;
  }
  if (actual != UGENDER_FEMALE) {
    errln("Expected UGENDER_FEMALE got %d", actual);
  }
}

void GenderInfoTest::check(
    UGender expected_neutral, UGender expected_mixed, UGender expected_taints, const UGender* genderList, int32_t listSize) {
  checkLocale(Locale::getUS(), expected_neutral, genderList, listSize);
  checkLocale(Locale::createFromName("is"), expected_mixed, genderList, listSize);
  checkLocale(Locale::getFrench(), expected_taints, genderList, listSize);
}

void GenderInfoTest::checkLocale(
    const Locale& locale, UGender expected, const UGender* genderList, int32_t listSize) {
  UErrorCode status = U_ZERO_ERROR;
  const GenderInfo* gi = GenderInfo::getInstance(locale, status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to create GenderInfo - %s", u_errorName(status));
    return;
  }
  UGender actual = gi->getListGender(genderList, listSize, status);
  if (U_FAILURE(status)) {
    errcheckln(status, "Fail to get gender of list - %s", u_errorName(status));
    return;
  }
  if (actual != expected) {
    errln("For locale: %s expected: %d got %d", locale.getName(), expected, actual);
  }
}

extern IntlTest *createGenderInfoTest() {
  return new GenderInfoTest();
}
