/*
**********************************************************************
* Copyright (c) 2013, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include <string.h>
#include "unicode/uperf.h"
#include "unicode/ucol.h"
#include "unicode/coll.h"
#include "unicode/uiter.h"
#include "unicode/sortkey.h"
#include "uoptions.h"

#define COMPATCT_ARRAY(CompactArrays, UNIT) \
struct CompactArrays{\
    CompactArrays(const CompactArrays & );\
    CompactArrays & operator=(const CompactArrays & );\
    int32_t   count;/*total number of the strings*/ \
    int32_t * index;/*relative offset in data*/ \
    UNIT    * data; /*the real space to hold strings*/ \
    \
    ~CompactArrays(){free(index);free(data);} \
    CompactArrays():data(NULL), index(NULL), count(0){ \
    index = (int32_t *) realloc(index, sizeof(int32_t)); \
    index[0] = 0; \
    } \
    void append_one(int32_t theLen){ /*include terminal NULL*/ \
    count++; \
    index = (int32_t *) realloc(index, sizeof(int32_t) * (count + 1)); \
    index[count] = index[count - 1] + theLen; \
    data = (UNIT *) realloc(data, sizeof(UNIT) * index[count]); \
    } \
    UNIT * last(){return data + index[count - 1];} \
    UNIT * dataOf(int32_t i){return data + index[i];} \
    int32_t lengthOf(int i){return index[i+1] - index[i] - 1; } /*exclude terminating NULL*/  \
};

COMPATCT_ARRAY(CA_uchar, UChar)
COMPATCT_ARRAY(CA_char, char)

#define MAX_TEST_STRINGS_FOR_PERMUTING 1000

// C API test cases

//
// Test case taking a single test data array, calling ucol_strcoll by permuting the test data
//
class Strcoll : public UPerfFunction
{
public:
    Strcoll(const UCollator* coll, CA_uchar* source, UBool useLen);
    ~Strcoll();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const UCollator *coll;
    CA_uchar *source;
    UBool useLen;
    int32_t maxTestStrings;
};

Strcoll::Strcoll(const UCollator* coll, CA_uchar* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
    maxTestStrings = source->count > MAX_TEST_STRINGS_FOR_PERMUTING ? MAX_TEST_STRINGS_FOR_PERMUTING : source->count;
}

Strcoll::~Strcoll()
{
}

void Strcoll::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    // call strcoll for permutation
    int32_t divisor = source->count / maxTestStrings;
    int32_t srcLen, tgtLen;
    int32_t cmp = 0;
    for (int32_t i = 0, numTestStringsI = 0; i < source->count && numTestStringsI < maxTestStrings; i++) {
        if (i % divisor) continue;
        numTestStringsI++;
        srcLen = useLen ? source->lengthOf(i) : -1;
        for (int32_t j = 0, numTestStringsJ = 0; j < source->count && numTestStringsJ < maxTestStrings; j++) {
            if (j % divisor) continue;
            numTestStringsJ++;
            tgtLen = useLen ? source->lengthOf(j) : -1;
            cmp += ucol_strcoll(coll, source->dataOf(i), srcLen, source->dataOf(j), tgtLen);
        }
    }
    // At the end, cmp must be 0
    if (cmp != 0) {
        *status = U_INTERNAL_PROGRAM_ERROR;
    }
}

long Strcoll::getOperationsPerIteration()
{
    return maxTestStrings * maxTestStrings;
}

//
// Test case taking two test data arrays, calling ucol_strcoll for strings at a same index
//
class Strcoll_2 : public UPerfFunction
{
public:
    Strcoll_2(const UCollator* coll, CA_uchar* source, CA_uchar* target, UBool useLen);
    ~Strcoll_2();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const UCollator *coll;
    CA_uchar *source;
    CA_uchar *target;
    UBool useLen;
};

Strcoll_2::Strcoll_2(const UCollator* coll, CA_uchar* source, CA_uchar* target, UBool useLen)
    :   coll(coll),
        source(source),
        target(target),
        useLen(useLen)
{
}

Strcoll_2::~Strcoll_2()
{
}

void Strcoll_2::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    // call strcoll for two strings at the same index
    if (source->count < target->count) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    } else {
        for (int32_t i = 0; i < source->count; i++) {
            int32_t srcLen = useLen ? source->lengthOf(i) : -1;
            int32_t tgtLen = useLen ? target->lengthOf(i) : -1;
            ucol_strcoll(coll, source->dataOf(i), srcLen, target->dataOf(i), tgtLen);
        }
    }
}

long Strcoll_2::getOperationsPerIteration()
{
    return source->count;
}


//
// Test case taking a single test data array, calling ucol_strcollUTF8 by permuting the test data
//
class StrcollUTF8 : public UPerfFunction
{
public:
    StrcollUTF8(const UCollator* coll, CA_char* source, UBool useLen);
    ~StrcollUTF8();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const UCollator *coll;
    CA_char *source;
    UBool useLen;
    int32_t maxTestStrings;
};

StrcollUTF8::StrcollUTF8(const UCollator* coll, CA_char* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
    maxTestStrings = source->count > MAX_TEST_STRINGS_FOR_PERMUTING ? MAX_TEST_STRINGS_FOR_PERMUTING : source->count;
}

StrcollUTF8::~StrcollUTF8()
{
}

void StrcollUTF8::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    // call strcollUTF8 for permutation
    int32_t divisor = source->count / maxTestStrings;
    int32_t srcLen, tgtLen;
    int32_t cmp = 0;
    for (int32_t i = 0, numTestStringsI = 0; U_SUCCESS(*status) && i < source->count && numTestStringsI < maxTestStrings; i++) {
        if (i % divisor) continue;
        numTestStringsI++;
        srcLen = useLen ? source->lengthOf(i) : -1;
        for (int32_t j = 0, numTestStringsJ = 0; U_SUCCESS(*status) && j < source->count && numTestStringsJ < maxTestStrings; j++) {
            if (j % divisor) continue;
            numTestStringsJ++;
            tgtLen = useLen ? source->lengthOf(j) : -1;
            cmp += ucol_strcollUTF8(coll, source->dataOf(i), srcLen, source->dataOf(j), tgtLen, status);
        }
    }
    // At the end, cmp must be 0
    if (cmp != 0) {
        *status = U_INTERNAL_PROGRAM_ERROR;
    }
}

long StrcollUTF8::getOperationsPerIteration()
{
    return maxTestStrings * maxTestStrings;
}

//
// Test case taking two test data arrays, calling ucol_strcoll for strings at a same index
//
class StrcollUTF8_2 : public UPerfFunction
{
public:
    StrcollUTF8_2(const UCollator* coll, CA_char* source, CA_char* target, UBool useLen);
    ~StrcollUTF8_2();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const UCollator *coll;
    CA_char *source;
    CA_char *target;
    UBool useLen;
};

StrcollUTF8_2::StrcollUTF8_2(const UCollator* coll, CA_char* source, CA_char* target, UBool useLen)
    :   coll(coll),
        source(source),
        target(target),
        useLen(useLen)
{
}

StrcollUTF8_2::~StrcollUTF8_2()
{
}

void StrcollUTF8_2::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    // call strcoll for two strings at the same index
    if (source->count < target->count) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    } else {
        for (int32_t i = 0; U_SUCCESS(*status) && i < source->count; i++) {
            int32_t srcLen = useLen ? source->lengthOf(i) : -1;
            int32_t tgtLen = useLen ? target->lengthOf(i) : -1;
            ucol_strcollUTF8(coll, source->dataOf(i), srcLen, target->dataOf(i), tgtLen, status);
        }
    }
}

long StrcollUTF8_2::getOperationsPerIteration()
{
    return source->count;
}

//
// Test case taking a single test data array, calling ucol_getSortKey for each
//
class GetSortKey : public UPerfFunction
{
public:
    GetSortKey(const UCollator* coll, CA_uchar* source, UBool useLen);
    ~GetSortKey();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const UCollator *coll;
    CA_uchar *source;
    UBool useLen;
};

GetSortKey::GetSortKey(const UCollator* coll, CA_uchar* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
}

GetSortKey::~GetSortKey()
{
}

#define KEY_BUF_SIZE 512

void GetSortKey::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    uint8_t key[KEY_BUF_SIZE];
    int32_t len;

    if (useLen) {
        for (int32_t i = 0; i < source->count; i++) {
            len = ucol_getSortKey(coll, source->dataOf(i), source->lengthOf(i), key, KEY_BUF_SIZE);
        }
    } else {
        for (int32_t i = 0; i < source->count; i++) {
            len = ucol_getSortKey(coll, source->dataOf(i), -1, key, KEY_BUF_SIZE);
        }
    }
}

long GetSortKey::getOperationsPerIteration()
{
    return source->count;
}

//
// Test case taking a single test data array in UTF-16, calling ucol_nextSortKeyPart for each for the
// given buffer size
//
class NextSortKeyPart : public UPerfFunction
{
public:
    NextSortKeyPart(const UCollator* coll, CA_uchar* source, int32_t bufSize, int32_t maxIteration = -1);
    ~NextSortKeyPart();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();
    virtual long getEventsPerIteration();

private:
    const UCollator *coll;
    CA_uchar *source;
    int32_t bufSize;
    int32_t maxIteration;
    long events;
};

// Note: maxIteration = -1 -> repeat until the end of collation key
NextSortKeyPart::NextSortKeyPart(const UCollator* coll, CA_uchar* source, int32_t bufSize, int32_t maxIteration /* = -1 */)
    :   coll(coll),
        source(source),
        bufSize(bufSize),
        maxIteration(maxIteration),
        events(0)
{
}

NextSortKeyPart::~NextSortKeyPart()
{
}

void NextSortKeyPart::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    uint8_t *part = (uint8_t *)malloc(bufSize);
    uint32_t state[2];
    UCharIterator iter;

    events = 0;
    for (int i = 0; i < source->count && U_SUCCESS(*status); i++) {
        uiter_setString(&iter, source->dataOf(i), source->lengthOf(i));
        state[0] = 0;
        state[1] = 0;
        int32_t partLen = bufSize;
        for (int32_t n = 0; U_SUCCESS(*status) && partLen == bufSize && (maxIteration < 0 || n < maxIteration); n++) {
            partLen = ucol_nextSortKeyPart(coll, &iter, state, part, bufSize, status);
            events++;
        }
        // Workaround for #10595
        if (U_FAILURE(*status)) {
            *status = U_ZERO_ERROR;
        }
    }
    free(part);
}

long NextSortKeyPart::getOperationsPerIteration()
{
    return source->count;
}

long NextSortKeyPart::getEventsPerIteration()
{
    return events;
}

//
// Test case taking a single test data array in UTF-8, calling ucol_nextSortKeyPart for each for the
// given buffer size
//
class NextSortKeyPartUTF8 : public UPerfFunction
{
public:
    NextSortKeyPartUTF8(const UCollator* coll, CA_char* source, int32_t bufSize, int32_t maxIteration = -1);
    ~NextSortKeyPartUTF8();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();
    virtual long getEventsPerIteration();

private:
    const UCollator *coll;
    CA_char *source;
    int32_t bufSize;
    int32_t maxIteration;
    long events;
};

// Note: maxIteration = -1 -> repeat until the end of collation key
NextSortKeyPartUTF8::NextSortKeyPartUTF8(const UCollator* coll, CA_char* source, int32_t bufSize, int32_t maxIteration /* = -1 */)
    :   coll(coll),
        source(source),
        bufSize(bufSize),
        maxIteration(maxIteration),
        events(0)
{
}

NextSortKeyPartUTF8::~NextSortKeyPartUTF8()
{
}

void NextSortKeyPartUTF8::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    uint8_t *part = (uint8_t *)malloc(bufSize);
    uint32_t state[2];
    UCharIterator iter;

    events = 0;
    for (int i = 0; i < source->count && U_SUCCESS(*status); i++) {
        uiter_setUTF8(&iter, source->dataOf(i), source->lengthOf(i));
        state[0] = 0;
        state[1] = 0;
        int32_t partLen = bufSize;
        for (int32_t n = 0; U_SUCCESS(*status) && partLen == bufSize && (maxIteration < 0 || n < maxIteration); n++) {
            partLen = ucol_nextSortKeyPart(coll, &iter, state, part, bufSize, status);
            events++;
        }
        // Workaround for #10595
        if (U_FAILURE(*status)) {
            *status = U_ZERO_ERROR;
        }
    }
    free(part);
}

long NextSortKeyPartUTF8::getOperationsPerIteration()
{
    return source->count;
}

long NextSortKeyPartUTF8::getEventsPerIteration()
{
    return events;
}

// CPP API test cases

//
// Test case taking a single test data array, calling Collator::compare by permuting the test data
//
class CppCompare : public UPerfFunction
{
public:
    CppCompare(const Collator* coll, CA_uchar* source, UBool useLen);
    ~CppCompare();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const Collator *coll;
    CA_uchar *source;
    UBool useLen;
    int32_t maxTestStrings;
};

CppCompare::CppCompare(const Collator* coll, CA_uchar* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
    maxTestStrings = source->count > MAX_TEST_STRINGS_FOR_PERMUTING ? MAX_TEST_STRINGS_FOR_PERMUTING : source->count;
}

CppCompare::~CppCompare()
{
}

void CppCompare::call(UErrorCode* status) {
    if (U_FAILURE(*status)) return;

    // call compare for permutation of test data
    int32_t divisor = source->count / maxTestStrings;
    int32_t srcLen, tgtLen;
    int32_t cmp = 0;
    for (int32_t i = 0, numTestStringsI = 0; i < source->count && numTestStringsI < maxTestStrings; i++) {
        if (i % divisor) continue;
        numTestStringsI++;
        srcLen = useLen ? source->lengthOf(i) : -1;
        for (int32_t j = 0, numTestStringsJ = 0; j < source->count && numTestStringsJ < maxTestStrings; j++) {
            if (j % divisor) continue;
            numTestStringsJ++;
            tgtLen = useLen ? source->lengthOf(j) : -1;
            cmp += coll->compare(source->dataOf(i), srcLen, source->dataOf(j), tgtLen);
        }
    }
    // At the end, cmp must be 0
    if (cmp != 0) {
        *status = U_INTERNAL_PROGRAM_ERROR;
    }
}

long CppCompare::getOperationsPerIteration()
{
    return maxTestStrings * maxTestStrings;
}

//
// Test case taking two test data arrays, calling Collator::compare for strings at a same index
//
class CppCompare_2 : public UPerfFunction
{
public:
    CppCompare_2(const Collator* coll, CA_uchar* source, CA_uchar* target, UBool useLen);
    ~CppCompare_2();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const Collator *coll;
    CA_uchar *source;
    CA_uchar *target;
    UBool useLen;
};

CppCompare_2::CppCompare_2(const Collator* coll, CA_uchar* source, CA_uchar* target, UBool useLen)
    :   coll(coll),
        source(source),
        target(target),
        useLen(useLen)
{
}

CppCompare_2::~CppCompare_2()
{
}

void CppCompare_2::call(UErrorCode* status) {
    if (U_FAILURE(*status)) return;

    // call strcoll for two strings at the same index
    if (source->count < target->count) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    } else {
        for (int32_t i = 0; i < source->count; i++) {
            int32_t srcLen = useLen ? source->lengthOf(i) : -1;
            int32_t tgtLen = useLen ? target->lengthOf(i) : -1;
            coll->compare(source->dataOf(i), srcLen, target->dataOf(i), tgtLen);
        }
    }
}

long CppCompare_2::getOperationsPerIteration()
{
    return source->count;
}


//
// Test case taking a single test data array, calling Collator::compareUTF8 by permuting the test data
//
class CppCompareUTF8 : public UPerfFunction
{
public:
    CppCompareUTF8(const Collator* coll, CA_char* source, UBool useLen);
    ~CppCompareUTF8();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const Collator *coll;
    CA_char *source;
    UBool useLen;
    int32_t maxTestStrings;
};

CppCompareUTF8::CppCompareUTF8(const Collator* coll, CA_char* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
    maxTestStrings = source->count > MAX_TEST_STRINGS_FOR_PERMUTING ? MAX_TEST_STRINGS_FOR_PERMUTING : source->count;
}

CppCompareUTF8::~CppCompareUTF8()
{
}

void CppCompareUTF8::call(UErrorCode* status) {
    if (U_FAILURE(*status)) return;

    // call compareUTF8 for all permutations
    int32_t divisor = source->count / maxTestStrings;
    StringPiece src, tgt;
    int32_t cmp = 0;
    for (int32_t i = 0, numTestStringsI = 0; U_SUCCESS(*status) && i < source->count && numTestStringsI < maxTestStrings; i++) {
        if (i % divisor) continue;
        numTestStringsI++;

        if (useLen) {
            src.set(source->dataOf(i), source->lengthOf(i));
        } else {
            src.set(source->dataOf(i));
        }
        for (int32_t j = 0, numTestStringsJ = 0; U_SUCCESS(*status) && j < source->count && numTestStringsJ < maxTestStrings; j++) {
            if (j % divisor) continue;
            numTestStringsJ++;

            if (useLen) {
                tgt.set(source->dataOf(i), source->lengthOf(i));
            } else {
                tgt.set(source->dataOf(i));
            }
            cmp += coll->compareUTF8(src, tgt, *status);
        }
    }
    // At the end, cmp must be 0
    if (cmp != 0) {
        *status = U_INTERNAL_PROGRAM_ERROR;
    }
}

long CppCompareUTF8::getOperationsPerIteration()
{
    return maxTestStrings * maxTestStrings;
}


//
// Test case taking two test data arrays, calling Collator::compareUTF8 for strings at a same index
//
class CppCompareUTF8_2 : public UPerfFunction
{
public:
    CppCompareUTF8_2(const Collator* coll, CA_char* source, CA_char* target, UBool useLen);
    ~CppCompareUTF8_2();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const Collator *coll;
    CA_char *source;
    CA_char *target;
    UBool useLen;
};

CppCompareUTF8_2::CppCompareUTF8_2(const Collator* coll, CA_char* source, CA_char* target, UBool useLen)
    :   coll(coll),
        source(source),
        target(target),
        useLen(useLen)
{
}

CppCompareUTF8_2::~CppCompareUTF8_2()
{
}

void CppCompareUTF8_2::call(UErrorCode* status) {
    if (U_FAILURE(*status)) return;

    // call strcoll for two strings at the same index
    StringPiece src, tgt;
    if (source->count < target->count) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
    } else {
        for (int32_t i = 0; U_SUCCESS(*status) && i < source->count; i++) {
            if (useLen) {
                src.set(source->dataOf(i), source->lengthOf(i));
                tgt.set(target->dataOf(i), target->lengthOf(i));
            } else {
                src.set(source->dataOf(i));
                tgt.set(target->dataOf(i));
            }
            coll->compareUTF8(src, tgt, *status);
        }
    }
}

long CppCompareUTF8_2::getOperationsPerIteration()
{
    return source->count;
}


//
// Test case taking a single test data array, calling Collator::getCollationKey for each
//
class CppGetCollationKey : public UPerfFunction
{
public:
    CppGetCollationKey(const Collator* coll, CA_uchar* source, UBool useLen);
    ~CppGetCollationKey();
    virtual void call(UErrorCode* status);
    virtual long getOperationsPerIteration();

private:
    const Collator *coll;
    CA_uchar *source;
    UBool useLen;
};

CppGetCollationKey::CppGetCollationKey(const Collator* coll, CA_uchar* source, UBool useLen)
    :   coll(coll),
        source(source),
        useLen(useLen)
{
}

CppGetCollationKey::~CppGetCollationKey()
{
}

void CppGetCollationKey::call(UErrorCode* status)
{
    if (U_FAILURE(*status)) return;

    CollationKey key;
    for (int32_t i = 0; U_SUCCESS(*status) && i < source->count; i++) {
        coll->getCollationKey(source->dataOf(i), source->lengthOf(i), key, *status);
    }
}

long CppGetCollationKey::getOperationsPerIteration() {
    return source->count;
}


class CollPerf2Test : public UPerfTest
{
public:
    CollPerf2Test(int32_t argc, const char *argv[], UErrorCode &status);
    ~CollPerf2Test();
    virtual UPerfFunction* runIndexedTest(
        int32_t index, UBool exec, const char *&name, char *par = NULL);

private:
    UCollator* coll;
    Collator* collObj;

    int32_t count;
    CA_uchar* data16;
    CA_char* data8;

    CA_uchar* modData16;
    CA_char* modData8;

    CA_uchar* getData16(UErrorCode &status);
    CA_char* getData8(UErrorCode &status);

    CA_uchar* getModData16(UErrorCode &status);
    CA_char* getModData8(UErrorCode &status);

    UPerfFunction* TestStrcoll();
    UPerfFunction* TestStrcollNull();
    UPerfFunction* TestStrcollSimilar();

    UPerfFunction* TestStrcollUTF8();
    UPerfFunction* TestStrcollUTF8Null();
    UPerfFunction* TestStrcollUTF8Similar();

    UPerfFunction* TestGetSortKey();
    UPerfFunction* TestGetSortKeyNull();

    UPerfFunction* TestNextSortKeyPart_4All();
    UPerfFunction* TestNextSortKeyPart_4x2();
    UPerfFunction* TestNextSortKeyPart_4x4();
    UPerfFunction* TestNextSortKeyPart_4x8();
    UPerfFunction* TestNextSortKeyPart_32All();
    UPerfFunction* TestNextSortKeyPart_32x2();

    UPerfFunction* TestNextSortKeyPartUTF8_4All();
    UPerfFunction* TestNextSortKeyPartUTF8_4x2();
    UPerfFunction* TestNextSortKeyPartUTF8_4x4();
    UPerfFunction* TestNextSortKeyPartUTF8_4x8();
    UPerfFunction* TestNextSortKeyPartUTF8_32All();
    UPerfFunction* TestNextSortKeyPartUTF8_32x2();

    UPerfFunction* TestCppCompare();
    UPerfFunction* TestCppCompareNull();
    UPerfFunction* TestCppCompareSimilar();

    UPerfFunction* TestCppCompareUTF8();
    UPerfFunction* TestCppCompareUTF8Null();
    UPerfFunction* TestCppCompareUTF8Similar();

    UPerfFunction* TestCppGetCollationKey();
    UPerfFunction* TestCppGetCollationKeyNull();

};

CollPerf2Test::CollPerf2Test(int32_t argc, const char *argv[], UErrorCode &status) :
    UPerfTest(argc, argv, status),
    coll(NULL),
    collObj(NULL),
    count(0),
    data16(NULL),
    data8(NULL),
    modData16(NULL),
    modData8(NULL)
{
    if (U_FAILURE(status)) {
        return;
    }

    if (locale == NULL){
        locale = "en_US";   // set default locale
    }

    //  Set up an ICU collator
    coll = ucol_open(locale, &status);
    collObj = Collator::createInstance(locale, status);

    // Keyword support should be actually a part of ICU collator
    char keyBuffer[256];
    UColAttributeValue val;
    if (uloc_getKeywordValue(locale, "strength", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "primary") == 0) {
            val = UCOL_PRIMARY;
        } else if (strcmp(keyBuffer, "secondary") == 0) {
            val = UCOL_SECONDARY;
        } else if (strcmp(keyBuffer, "tertiary") == 0) {
            val = UCOL_TERTIARY;
        } else if (strcmp(keyBuffer, "quaternary") == 0) {
            val = UCOL_QUATERNARY;
        } else if (strcmp(keyBuffer, "identical") == 0) {
            val = UCOL_IDENTICAL;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_STRENGTH, val, &status);
            collObj->setAttribute(UCOL_STRENGTH, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "alternate", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "non-ignorable") == 0) {
            val = UCOL_NON_IGNORABLE;
        } else if (strcmp(keyBuffer, "shifted") == 0) {
            val = UCOL_SHIFTED;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, val, &status);
            collObj->setAttribute(UCOL_ALTERNATE_HANDLING, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "backwards", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "on") == 0) {
            val = UCOL_ON;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_FRENCH_COLLATION, val, &status);
            collObj->setAttribute(UCOL_FRENCH_COLLATION, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "normalization", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "on") == 0) {
            val = UCOL_ON;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, val, &status);
            collObj->setAttribute(UCOL_NORMALIZATION_MODE, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "caseLevel", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "on") == 0) {
            val = UCOL_ON;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_CASE_LEVEL, val, &status);
            collObj->setAttribute(UCOL_CASE_LEVEL, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "caseFirst", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "upper") == 0) {
            val = UCOL_UPPER_FIRST;
        } else if (strcmp(keyBuffer, "lower") == 0) {
            val = UCOL_LOWER_FIRST;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_CASE_FIRST, val, &status);
            collObj->setAttribute(UCOL_CASE_FIRST, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "hiraganaQuaternary", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "on") == 0) {
            val = UCOL_ON;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_HIRAGANA_QUATERNARY_MODE, val, &status);
            collObj->setAttribute(UCOL_HIRAGANA_QUATERNARY_MODE, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "numeric", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        if (strcmp(keyBuffer, "on") == 0) {
            val = UCOL_ON;
        } else if (strcmp(keyBuffer, "off") == 0) {
            val = UCOL_OFF;
        } else {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        }
        if (U_SUCCESS(status)) {
            ucol_setAttribute(coll, UCOL_NUMERIC_COLLATION, val, &status);
            collObj->setAttribute(UCOL_NUMERIC_COLLATION, val, status);
        }
    }
    if (uloc_getKeywordValue(locale, "variableTop", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        // no support for now
        status = U_UNSUPPORTED_ERROR;
    }
    if (uloc_getKeywordValue(locale, "reorder", keyBuffer, sizeof(keyBuffer)/sizeof(keyBuffer[0]), &status)) {
        // no support for now
        status = U_UNSUPPORTED_ERROR;
    }
}

CollPerf2Test::~CollPerf2Test()
{
    ucol_close(coll);
    delete collObj;

    delete data16;
    delete data8;
    delete modData16;
    delete modData8;
}

#define MAX_NUM_DATA 10000

CA_uchar* CollPerf2Test::getData16(UErrorCode &status)
{
    if (U_FAILURE(status)) return NULL;
    if (data16) return data16;

    CA_uchar* d16 = new CA_uchar();
    const UChar *line = NULL;
    int32_t len = 0;
    int32_t numData = 0;

    for (;;) {
        line = ucbuf_readline(ucharBuf, &len, &status);
        if (line == NULL || U_FAILURE(status)) break;

        // Refer to the source code of ucbuf_readline()
        // 1. 'len' includes the line terminal symbols
        // 2. The length of the line terminal symbols is only one character
        // 3. The Windows CR LF line terminal symbols will be converted to CR

        if (len == 1 || line[0] == 0x23 /* '#' */) {
            continue; // skip empty/comment line
        } else {
            d16->append_one(len);
            memcpy(d16->last(), line, len * sizeof(UChar));
            d16->last()[len - 1] = NULL;

            numData++;
            if (numData >= MAX_NUM_DATA) break;
        }
    }

    if (U_SUCCESS(status)) {
        data16 = d16;
    } else {
        delete d16;
    }

    return data16;
}

CA_char* CollPerf2Test::getData8(UErrorCode &status)
{
    if (U_FAILURE(status)) return NULL;
    if (data8) return data8;

    // UTF-16 -> UTF-8 conversion
    CA_uchar* d16 = getData16(status);
    UConverter *conv = ucnv_open("utf-8", &status);
    if (U_FAILURE(status)) return NULL;

    CA_char* d8 = new CA_char();
    for (int32_t i = 0; i < d16->count; i++) {
        int32_t s, t;

        // get length in UTF-8
        s = ucnv_fromUChars(conv, NULL, 0, d16->dataOf(i), d16->lengthOf(i), &status);
        if (status == U_BUFFER_OVERFLOW_ERROR || status == U_ZERO_ERROR){
            status = U_ZERO_ERROR;
        } else {
            break;
        }
        d8->append_one(s + 1); // plus terminal NULL

        // convert to UTF-8
        t = ucnv_fromUChars(conv, d8->last(), s, d16->dataOf(i), d16->lengthOf(i), &status);
        if (U_FAILURE(status)) break;
        if (t != s) {
            status = U_INVALID_FORMAT_ERROR;
            break;
        }
        d8->last()[s] = 0;
    }
    ucnv_close(conv);

    if (U_SUCCESS(status)) {
        data8 = d8;
    } else {
        delete d8;
    }

    return data8;
}

CA_uchar* CollPerf2Test::getModData16(UErrorCode &status)
{
    if (U_FAILURE(status)) return NULL;
    if (modData16) return modData16;

    CA_uchar* d16 = getData16(status);
    if (U_FAILURE(status)) return NULL;

    CA_uchar* modData16 = new CA_uchar();

    for (int32_t i = 0; i < d16->count; i++) {
        UChar *s = d16->dataOf(i);
        int32_t len = d16->lengthOf(i) + 1; // including NULL terminator

        modData16->append_one(len);
        memcpy(modData16->last(), s, len * sizeof(UChar));
        modData16->last()[len - 1] = NULL;

        // replacing the last character with a different character
        UChar *lastChar = &modData16->last()[len -2];
        for (int32_t j = i + 1; j != i; j++) {
            if (j >= d16->count) {
                j = 0;
            }
            UChar *s1 = d16->dataOf(j);
            UChar lastChar1 = s1[d16->lengthOf(j) - 1];
            if (*lastChar != lastChar1) {
                *lastChar = lastChar1;
                break;
            }
        }
    }

    return modData16;
}

CA_char* CollPerf2Test::getModData8(UErrorCode &status)
{
    if (U_FAILURE(status)) return NULL;
    if (modData8) return modData8;

    // UTF-16 -> UTF-8 conversion
    CA_uchar* md16 = getModData16(status);
    UConverter *conv = ucnv_open("utf-8", &status);
    if (U_FAILURE(status)) return NULL;

    CA_char* md8 = new CA_char();
    for (int32_t i = 0; i < md16->count; i++) {
        int32_t s, t;

        // get length in UTF-8
        s = ucnv_fromUChars(conv, NULL, 0, md16->dataOf(i), md16->lengthOf(i), &status);
        if (status == U_BUFFER_OVERFLOW_ERROR || status == U_ZERO_ERROR){
            status = U_ZERO_ERROR;
        } else {
            break;
        }
        md8->append_one(s + 1); // plus terminal NULL

        // convert to UTF-8
        t = ucnv_fromUChars(conv, md8->last(), s, md16->dataOf(i), md16->lengthOf(i), &status);
        if (U_FAILURE(status)) break;
        if (t != s) {
            status = U_INVALID_FORMAT_ERROR;
            break;
        }
        md8->last()[s] = 0;
    }
    ucnv_close(conv);

    if (U_SUCCESS(status)) {
        modData8 = md8;
    } else {
        delete md8;
    }

    return modData8;
}

UPerfFunction*
CollPerf2Test::runIndexedTest(int32_t index, UBool exec, const char *&name, char *par /*= NULL*/)
{
    switch (index) {
        TESTCASE(0, TestStrcoll);
        TESTCASE(1, TestStrcollNull);
        TESTCASE(2, TestStrcollSimilar);

        TESTCASE(3, TestStrcollUTF8);
        TESTCASE(4, TestStrcollUTF8Null);
        TESTCASE(5, TestStrcollUTF8Similar);

        TESTCASE(6, TestGetSortKey);
        TESTCASE(7, TestGetSortKeyNull);

        TESTCASE(8, TestNextSortKeyPart_4All);
        TESTCASE(9, TestNextSortKeyPart_4x4);
        TESTCASE(10, TestNextSortKeyPart_4x8);
        TESTCASE(11, TestNextSortKeyPart_32All);
        TESTCASE(12, TestNextSortKeyPart_32x2);

        TESTCASE(13, TestNextSortKeyPartUTF8_4All);
        TESTCASE(14, TestNextSortKeyPartUTF8_4x4);
        TESTCASE(15, TestNextSortKeyPartUTF8_4x8);
        TESTCASE(16, TestNextSortKeyPartUTF8_32All);
        TESTCASE(17, TestNextSortKeyPartUTF8_32x2);

        TESTCASE(18, TestCppCompare);
        TESTCASE(19, TestCppCompareNull);
        TESTCASE(20, TestCppCompareSimilar);

        TESTCASE(21, TestCppCompareUTF8);
        TESTCASE(22, TestCppCompareUTF8Null);
        TESTCASE(23, TestCppCompareUTF8Similar);

        TESTCASE(24, TestCppGetCollationKey);
        TESTCASE(25, TestCppGetCollationKeyNull);

    default:
            name = ""; 
            return NULL;
    }
    return NULL;
}



UPerfFunction* CollPerf2Test::TestStrcoll()
{
    UErrorCode status = U_ZERO_ERROR;
    Strcoll *testCase = new Strcoll(coll, getData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestStrcollNull()
{
    UErrorCode status = U_ZERO_ERROR;
    Strcoll *testCase = new Strcoll(coll, getData16(status), FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestStrcollSimilar()
{
    UErrorCode status = U_ZERO_ERROR;
    Strcoll_2 *testCase = new Strcoll_2(coll, getData16(status), getModData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestStrcollUTF8()
{
    UErrorCode status = U_ZERO_ERROR;
    StrcollUTF8 *testCase = new StrcollUTF8(coll, getData8(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestStrcollUTF8Null()
{
    UErrorCode status = U_ZERO_ERROR;
    StrcollUTF8 *testCase = new StrcollUTF8(coll, getData8(status),FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestStrcollUTF8Similar()
{
    UErrorCode status = U_ZERO_ERROR;
    StrcollUTF8_2 *testCase = new StrcollUTF8_2(coll, getData8(status), getModData8(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestGetSortKey()
{
    UErrorCode status = U_ZERO_ERROR;
    GetSortKey *testCase = new GetSortKey(coll, getData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestGetSortKeyNull()
{
    UErrorCode status = U_ZERO_ERROR;
    GetSortKey *testCase = new GetSortKey(coll, getData16(status), FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPart_4All()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPart *testCase = new NextSortKeyPart(coll, getData16(status), 4 /* bufSize */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPart_4x4()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPart *testCase = new NextSortKeyPart(coll, getData16(status), 4 /* bufSize */, 4 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPart_4x8()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPart *testCase = new NextSortKeyPart(coll, getData16(status), 4 /* bufSize */, 8 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPart_32All()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPart *testCase = new NextSortKeyPart(coll, getData16(status), 32 /* bufSize */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPart_32x2()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPart *testCase = new NextSortKeyPart(coll, getData16(status), 32 /* bufSize */, 2 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPartUTF8_4All()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPartUTF8 *testCase = new NextSortKeyPartUTF8(coll, getData8(status), 4 /* bufSize */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPartUTF8_4x4()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPartUTF8 *testCase = new NextSortKeyPartUTF8(coll, getData8(status), 4 /* bufSize */, 4 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPartUTF8_4x8()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPartUTF8 *testCase = new NextSortKeyPartUTF8(coll, getData8(status), 4 /* bufSize */, 8 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPartUTF8_32All()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPartUTF8 *testCase = new NextSortKeyPartUTF8(coll, getData8(status), 32 /* bufSize */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestNextSortKeyPartUTF8_32x2()
{
    UErrorCode status = U_ZERO_ERROR;
    NextSortKeyPartUTF8 *testCase = new NextSortKeyPartUTF8(coll, getData8(status), 32 /* bufSize */, 2 /* maxIteration */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompare()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompare *testCase = new CppCompare(collObj, getData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompareNull()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompare *testCase = new CppCompare(collObj, getData16(status), FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompareSimilar()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompare_2 *testCase = new CppCompare_2(collObj, getData16(status), getModData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompareUTF8()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompareUTF8 *testCase = new CppCompareUTF8(collObj, getData8(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompareUTF8Null()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompareUTF8 *testCase = new CppCompareUTF8(collObj, getData8(status), FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppCompareUTF8Similar()
{
    UErrorCode status = U_ZERO_ERROR;
    CppCompareUTF8_2 *testCase = new CppCompareUTF8_2(collObj, getData8(status), getModData8(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppGetCollationKey()
{
    UErrorCode status = U_ZERO_ERROR;
    CppGetCollationKey *testCase = new CppGetCollationKey(collObj, getData16(status), TRUE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}

UPerfFunction* CollPerf2Test::TestCppGetCollationKeyNull()
{
    UErrorCode status = U_ZERO_ERROR;
    CppGetCollationKey *testCase = new CppGetCollationKey(collObj, getData16(status), FALSE /* useLen */);
    if (U_FAILURE(status)) {
        delete testCase;
        return NULL;
    }
    return testCase;
}


int main(int argc, const char *argv[])
{
    UErrorCode status = U_ZERO_ERROR;
    CollPerf2Test test(argc, argv, status);

    if (U_FAILURE(status)){
        printf("The error is %s\n", u_errorName(status));
        //TODO: print usage here
        return status;
    }

    if (test.run() == FALSE){
        fprintf(stderr, "FAILED: Tests could not be run please check the arguments.\n");
        return -1;
    }
    return 0;
}


