// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// pnames_impl.h
// created: 2026aug17 Markus W. Scherer
// (pulled out of pnamesbuilder.cpp)

#ifndef __PNAMES_IMPL_H__
#define __PNAMES_IMPL_H__

#include "unicode/utypes.h"

// Dilemma: We want to use MAX_ALIASES to define fields in the Value class.
// However, we need to define the class before including the data header
// and we can use MAX_ALIASES only after including it.
// So we define a second constant and at runtime check that it's >=MAX_ALIASES.
static const int32_t VALUE_MAX_ALIASES=4;

static const int32_t JOINED_ALIASES_CAPACITY=100;

class Value {
public:
    Value(int32_t enumValue, const char *apiName, const char *joinedAliases);

    /**
     * Writes at most MAX_ALIASES pointers for unique normalized aliases
     * (no empty strings) to dest and returns how many there are.
     */
    int32_t getUniqueNormalizedAliases(const char *dest[]) const;

    int32_t enumValue;
    const char *apiName;
    const char *joinedAliases;
    char aliasesBuffer[JOINED_ALIASES_CAPACITY];
    char normalizedBuffer[JOINED_ALIASES_CAPACITY];
    const char *aliases[VALUE_MAX_ALIASES];
    const char *normalized[VALUE_MAX_ALIASES];
    int32_t count;
};

class Property : public Value {
public:
    // A property with a values array.
    Property(int32_t enumValue, const char *apiName, const char *joinedAliases,
             const Value *values, int32_t valueCount);
    // A binary property (enumValue<UCHAR_BINARY_LIMIT), or one without values.
    Property(int32_t enumValue, const char *apiName, const char *joinedAliases);

    const Value *values;
    int32_t valueCount;
};

const Property *getPNamesProperties(int32_t &length);

#endif  // __PNAMES_IMPL_H__
