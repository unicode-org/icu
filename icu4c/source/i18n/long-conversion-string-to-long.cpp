// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
//
// From the long-conversion library. Original license:
//
// Copyright 2010 the V8 project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
//       copyright notice, this list of conditions and the following
//       disclaimer in the documentation and/or other materials provided
//       with the distribution.
//     * Neither the name of Google Inc. nor the names of its
//       contributors may be used to endorse or promote products derived
//       from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// ICU PATCH: ifdef around UCONFIG_NO_FORMATTING
#include "unicode/utypes.h"
#if !UCONFIG_NO_FORMATTING

// ICU PATCH: Do not include std::locale.

#include <climits>
// #include <locale>
#include <cmath>

// ICU PATCH: Customize header file paths for ICU.

#include "long-conversion-string-to-long.h"
#include "unicode/uchar.h"

// ICU PATCH: Wrap in ICU namespace
U_NAMESPACE_BEGIN

#ifdef _MSC_VER
#if _MSC_VER >= 1900
// Fix MSVC >= 2015 (_MSC_VER == 1900) warning
// C4244: 'argument': conversion from 'const uc16' to 'char', possible loss of data
// against Advance and friends, when instantiated with **it as char, not uc16.
__pragma(warning(disable : 4244))
#endif
#if _MSC_VER <= 1700 // VS2012, see IsDecimalDigitForRadix warning fix, below
#define VS2012_RADIXWARN
#endif
#endif

    namespace long_conversion {
    namespace {

    // Converts a string (that contains only digits) to a long.
    // For example:
    //   stringToLongPlain("12345") -> 12345
    //   stringToLongPlain("0") -> 0
    //   stringToLongPlain("+1") -> 1
    //   stringToLongPlain("9223372036854775807") -> 9223372036854775807
    //   stringToLongPlain("9223372036854775808" /* more than maximum */) ->  Error
    //   stringToLongPlain("-9223372036854775808") -> -9223372036854775808
    //   stringToLongPlain("-9223372036854775809" /* less than minimum */) -> Error
    //  stringToLongPlain("++1234560" /* representation error */) -> Error
    int64_t stringToLongPlain(StringPiece number, UErrorCode &status) {
        if (U_FAILURE(status)) {
            return 0;
        }

        uint64_t absolute = 0;
        bool negative = false;
        uint64_t absolute_max = LONG_MAX + 1; // 9223372036854775808
        for (int32_t i = 0; i < number.length(); i++) {
            // Handle the sign.
            if (i == 0 && number.data()[i] == '+') {
                continue;
            }
            if (i == 0 && number.data()[i] == '-') {
                negative = true;
                continue;
            }

            // Handle the digits.
            if (u_isdigit(number.data()[i])) {
                int32_t digit = number.data()[i] - '0';
                if (absolute > (absolute_max - digit) /
                                   10.0) { // TODO: the check can be more accurate than using double.
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return 0;
                }

                absolute = absolute * 10 + digit;
            }

            // Handle the junk.
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }

        if (negative) {
            return -absolute;
        }

        if (absolute > LONG_MAX) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }

        return absolute;
    }

    int64_t power10(int64_t exponent, UErrorCode &status) {
        if (U_FAILURE(status)) {
            return 0;
        }

        if (exponent < -18 || exponent > 18) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }

        int64_t result = exponent < 0 ? -1 : 1;
        int64_t base = 10;
        while (exponent != 0) {
            if (exponent % 2 == 1) {
                result *= base;
            }

            exponent /= 2;
            base *= base;
        }

        return result;
    }

    } // namespace

    int64_t StringToLongConverter::stringToLong(StringPiece number, UErrorCode & status) const {
        if (number.empty()) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }

        int32_t exponent_index = 0;
        while (exponent_index < number.length() && number.data()[exponent_index] != 'e' &&
               number.data()[exponent_index] != 'E') {
            exponent_index++;
        }

        auto baseString = number.substr(0, exponent_index);
        auto exponentString = number.substr(exponent_index + 1);

        int64_t base = stringToLongPlain(baseString, status);
        int64_t exponent = stringToLongPlain(exponentString, status);
        int64_t power = power10(exponent, status);

        if (status != U_ZERO_ERROR) {
            return 0;
        }

        if ((base > 0 && power > 0 && base > LONG_MAX / power) ||
            (base > 0 && power < 0 && base > LONG_MIN / power) ||
            (base < 0 && power > 0 && base < LONG_MIN / power) ||
            (base < 0 && power < 0 && base < LONG_MAX / power)) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }

        return base * power;
    }
} // namespace long_conversion

// ICU PATCH: Close ICU namespace
U_NAMESPACE_END
#endif // ICU PATCH: close #if !UCONFIG_NO_FORMATTING