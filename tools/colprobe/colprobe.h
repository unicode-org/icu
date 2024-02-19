// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#ifndef COLPROBE_H
#define COLPROBE_H

#include "unicode/uniset.h"
#include "unicode/normlzr.h"

typedef int (*CompareFn) (const void *elem1, const void *elem2);
typedef int (*GetSortKeyFn) (const char16_t *string, int32_t len, uint8_t *buffer, int32_t buffCapacity);
//typedef int (__cdecl *CompareFn)(const void *elem1, const void *elem2);
void generateRepertoire(const char *locale, UnicodeSet &rep, UBool &hanAppears, UErrorCode &status);
UnicodeSet flatten(const UnicodeSet &source, UErrorCode &status);

//UnicodeSet generateRepertoire(const char *locale);

#endif
