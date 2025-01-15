// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <iostream>

#include "unicode/utypes.h"
#include "unicode/calendar.h"
#include "unicode/errorcode.h"
#include "unicode/locid.h"
#include "unicode/messageformat2.h"

using namespace icu;

int main() {
    ErrorCode errorCode;
    UParseError parseError;

    icu::Calendar* cal(Calendar::createInstance(errorCode));
    cal->set(2025, Calendar::JANUARY, 28);
    UDate date = cal->getTime(errorCode);

    message2::MessageFormatter::Builder builder(errorCode);
    message2::MessageFormatter mf = builder
            .setPattern("Hello {$user}, today is {$now :date style=long}!", parseError, errorCode)
            .setLocale(Locale("en_US"))
            .build(errorCode);

    std::map<UnicodeString, message2::Formattable> argsBuilder;
    argsBuilder["user"] = message2::Formattable("John");
    argsBuilder["now"] = message2::Formattable::forDate(date);
    message2::MessageArguments arguments(argsBuilder, errorCode);

    icu::UnicodeString result = mf.formatToString(arguments, errorCode);
    std::string strResult;
    result.toUTF8String(strResult);
    std::cout << strResult << std::endl;
}