// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// ICU-22356 Test that client code can be built with UCHAR_TYPE redefined.
#undef UCHAR_TYPE
#define UCHAR_TYPE uint16_t
#include "unicode/ures.h"
