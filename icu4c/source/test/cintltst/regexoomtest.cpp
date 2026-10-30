// © 2026 Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_REGULAR_EXPRESSIONS
#include "cintltst.h"
#include "unicode/putil.h"
#include "unicode/regex.h"
#include "unicode/uclean.h"
#include <cstdlib>
#include <string>

namespace {

struct RegexOOMContext {
    bool failAllocations = false;
    mutable int32_t failedAllocations = 0;
};

void *U_CALLCONV regexOOMAlloc(const void *context, size_t size) {
    const RegexOOMContext *state = static_cast<const RegexOOMContext *>(context);
    if (state->failAllocations) {
        ++state->failedAllocations;
        return nullptr;
    }
    return std::malloc(size);
}

void *U_CALLCONV regexOOMRealloc(const void *context, void *ptr, size_t size) {
    const RegexOOMContext *state = static_cast<const RegexOOMContext *>(context);
    if (state->failAllocations) {
        ++state->failedAllocations;
        return nullptr;
    }
    return std::realloc(ptr, size);
}

void U_CALLCONV regexOOMFree(const void *, void *ptr) {
    std::free(ptr);
}

}  // namespace

U_CFUNC void TestRegexPatternTextOOM(void) {
    const char *currentDataDir = u_getDataDirectory();
    std::string dataDir = currentDataDir == nullptr ? "" : currentDataDir;
    if (!ctest_resetICU()) {
        return;
    }
    u_cleanup();

    RegexOOMContext oom;
    UErrorCode status = U_ZERO_ERROR;
    UText *text = nullptr;
    u_setMemoryFunctions(&oom, regexOOMAlloc, regexOOMRealloc, regexOOMFree, &status);
    if (U_SUCCESS(status)) {
        u_setDataDirectory(dataDir.c_str());
        icu::RegexPattern pattern;
        oom.failAllocations = true;
        text = pattern.patternText(status);
        oom.failAllocations = false;
    }

    if (!ctest_resetICU()) {
        return;
    }
    if (oom.failedAllocations == 0 || status != U_MEMORY_ALLOCATION_ERROR || text != nullptr) {
        log_err("RegexPattern::patternText returned %p with %s after %d failed allocations.\n",
                static_cast<void *>(text), u_errorName(status), oom.failedAllocations);
    }
}
#endif
