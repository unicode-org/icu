#include "unicode/unistr.h"
#include "unicode/numfmt.h"
#include "unicode/locid.h"
#include "util.h"
#include <stdio.h>
#include <stdlib.h>

extern "C" void capi();
void cppapi();

int main(int argc, char **argv) {
    printf("C++ API\n");
    cppapi();

    printf("C API\n");
    capi();

    printf("Exiting successfully\n");
    return 0;
}

/**
 * Sample code for the C++ API to NumberFormat.
 */
void cppapi() {
    Locale us("en", "US");
    UErrorCode status = U_ZERO_ERROR;
    
    // Create a number formatter for the US locale
    NumberFormat *fmt = NumberFormat::createInstance(us, status);
    check(status, "NumberFormat::createInstance");

    // Parse a string.  The string uses the digits '0' through '9'
    // and the decimal separator '.', standard in the US locale
    UnicodeString str("9876543210.123");
    Formattable result;
    fmt->parse(str, result, status);
    check(status, "NumberFormat::parse");

    printf("NumberFormat::parse(\""); // Display the result
    uprintf(str);
    printf("\") => ");
    uprintf(formattableToString(result));
    printf("\n");

    // Take the number parsed above, and use the formatter to
    // format it.
    str.remove(); // format() will APPEND to this string
    fmt->format(result, str, status);
    check(status, "NumberFormat::format");

    printf("NumberFormat::format("); // Display the result
    uprintf(formattableToString(result));
    printf(") => \"");
    uprintf(str);
    printf("\"\n");

    delete fmt; // Release the storage used by the formatter
}
