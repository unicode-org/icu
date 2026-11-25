// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// safe32.h
// created: 2026nov23 Markus W. Scherer

#ifndef __SAFE32_H__
#define __SAFE32_H__

#include <type_traits>
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

/**
 * 32-bit non-negative length of a string or similar.
 * When the value overflows (does not fit into 0..INT32_MAX),
 * it is set to 0, and a failure error code is set.
 */
// NOTE: Some compilers (GCC, Clang) have primitives available,
// like __builtin_add_overflow() and __builtin_mul_overflow().
// This class could be optimized by calling those primitives.
class Length32 /* not : public UObject because this should only be used on the stack */ {
public:
    enum UseInternalErrorCode {
        /** Use an internal UErrorCode. */
        INTERNAL
    };

    struct Result {
        int32_t value;
        bool ok;
    };

    /** Constructor, records errors in the provided UErrorCode. */
    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Length32(T value, UErrorCode &errorCode) :
            value_(toInt32(value, errorCode)),
            errorCode_(errorCode) {}

    /** Constructor, records errors internally. */
    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Length32(T value, UseInternalErrorCode) :
            value_(toInt32(value, internalErrorCode_)),
            errorCode_(internalErrorCode_) {}

    Length32(const Length32 &other) :
            internalErrorCode_(other.internalErrorCode_), value_(other.value_),
            errorCode_(outErrorCode(other)) {}

    // no assignment operator (because of the reference member), only copy constructor
    Length32 &operator=(const Length32 &) = delete;

    // Explicit getter.
    // An operator int32_t() would allow unchecked default arithmetic.
    int32_t get() const { return value_; }

    Result result() const { return {value_, isSuccess()}; }

    bool isSuccess() const { return U_SUCCESS(errorCode_); }
    bool isFailure() const { return U_FAILURE(errorCode_); }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Length32 operator+(T b) const {
        // TODO: maybe allow negative b
        if (isSuccess() && b >= 0) {
            return {static_cast<int64_t>(value_) + static_cast<int64_t>(b), this};
        }
        return {this};
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Length32 operator*(T b) const {
        if (isSuccess() && b >= 0) {
            return {static_cast<int64_t>(value_) * static_cast<int64_t>(b), this};
        }
        return {this};
    }

private:
    // Internal constructor for returning a result object;
    // the constructor checks the value.
    Length32(int64_t value, const Length32 *other) :
            internalErrorCode_(other->internalErrorCode_),
            value_(toInt32(value, outErrorCode(*other))),
            errorCode_(outErrorCode(*other)) {}

    // Internal constructor for returning a failure result object.
    Length32(const Length32 *other) :
            internalErrorCode_(other->internalErrorCode_), value_(0),
            errorCode_(outErrorCode(*other)) {
        if (isSuccess()) {
            errorCode_ = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }

    // A Length32 return value must use its own internal error code
    // if the source object did, or else use the same external one.
    UErrorCode &outErrorCode(const Length32 &other) {
        return &other.errorCode_ == &other.internalErrorCode_ ?
                internalErrorCode_ : other.errorCode_;
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    static int32_t toInt32(T value, UErrorCode &errorCode) {
        if (U_SUCCESS(errorCode)) {
            int32_t v32 = static_cast<int32_t>(value);
            if (v32 >= 0 && v32 == value) {
                return v32;
            } else {
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
        }
        return 0;
    }

    // Error code before value, so that they are initialized in that order.
    UErrorCode internalErrorCode_ = U_ZERO_ERROR;
    int32_t value_;
    UErrorCode &errorCode_;
};

U_NAMESPACE_END

#endif  // __SAFE32_H__
