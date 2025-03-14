// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// ICU-22954 Test that client code can be built with U_SHOW_CPLUSPLUS_API=0.
#undef U_ALL_IMPLEMENTATION
#undef U_SHOW_CPLUSPLUS_API
#define U_SHOW_CPLUSPLUS_API 0
#include "unicode/char16ptr.h"
#include "unicode/ucol.h"
#include "unicode/uset.h"
#include "unicode/utypes.h"
