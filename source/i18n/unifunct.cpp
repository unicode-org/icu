#include "unicode/unifunct.h"

const char UnicodeFunctor::fgClassID = 0;

UnicodeMatcher* UnicodeFunctor::toMatcher() const {
    return 0;
}

UnicodeReplacer* UnicodeFunctor::toReplacer() const {
    return 0;
}

//eof
