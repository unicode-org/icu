// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// safe32.h
// created: 2026nov23 Markus W. Scherer

#ifndef __SAFE32_H__
#define __SAFE32_H__

#include <type_traits>
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

// Value types for safe operations on int32_t with bounds checking.
// These are not full numeric types; they define operators that we need.
// We can add more over time.
// NOTE: Some compilers (GCC, Clang) have primitives available,
// like __builtin_add_overflow() and __builtin_mul_overflow().
// The code below may be able to be optimized by calling those primitives.

struct UseInternalErrorCode {};
/** Use an internal UErrorCode. */
constexpr UseInternalErrorCode INTERNAL_ERROR;

typedef enum Bounds32 {
    NON_NEG, FULL
} Bounds32;

/**
 * Do not use this directly. Use NonNeg32 & Int32.
 *
 * 32-bit integer with bounds checking.
 * When the value is outside the supported range,
 * it is set to 0, and a failure error code is set.
 */
template<Bounds32 bounds>
class Safe32 {
public:
    struct Result {
        int32_t value;
        bool ok;
    };

    /** Constructor, records errors in the external UErrorCode. */
    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Safe32(T value, UErrorCode &errorCode) :
            value_(toInt32(value, errorCode)),
            errorCode_(errorCode) {}

    /** Constructor, records errors internally. */
    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Safe32(T value, UseInternalErrorCode) :
            value_(toInt32(value, internalErrorCode_)),
            errorCode_(internalErrorCode_) {}

    /** Copy constructor. */
    Safe32(const Safe32 &other) :
            internalErrorCode_(other.internalErrorCode_), value_(other.value_),
            errorCode_(outErrorCode(other)) {}

    // no assignment operator (because of the reference member), only copy constructor
    Safe32 &operator=(const Safe32 &) = delete;

    // Explicit getter.
    // An operator int32_t() would allow unchecked default arithmetic.
    int32_t get() const { return value_; }

    Result result() const { return {value_, isSuccess()}; }

    bool isSuccess() const { return U_SUCCESS(errorCode_); }
    bool isFailure() const { return U_FAILURE(errorCode_); }

private:
    // Internal constructor for returning a result object;
    // this constructor checks the value.
    Safe32(int64_t value, const Safe32 *other) :
            internalErrorCode_(other->internalErrorCode_),
            value_(toInt32(value, outErrorCode(*other))),
            errorCode_(outErrorCode(*other)) {}

    // Internal constructor for returning a failure result object.
    Safe32(const Safe32 *other) :
            internalErrorCode_(other->internalErrorCode_), value_(0),
            errorCode_(outErrorCode(*other)) {
        if (isSuccess()) {
            errorCode_ = U_ILLEGAL_ARGUMENT_ERROR;
        }
    }

    // A Safe32 return value must use its own internal error code
    // if the source object did, or else use the same external one.
    UErrorCode &outErrorCode(const Safe32 &other) {
        return &other.errorCode_ == &other.internalErrorCode_ ?
                internalErrorCode_ : other.errorCode_;
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    static int32_t toInt32(T value, UErrorCode &errorCode) {
        if (U_SUCCESS(errorCode)) {
            int32_t v32 = static_cast<int32_t>(value);
            if (v32 == value && (bounds == FULL || v32 >= 0)) {
                return v32;
            } else {
                errorCode = U_ILLEGAL_ARGUMENT_ERROR;
            }
        }
        return 0;
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Safe32 add(T b) const {
        // TODO: Non_NEG maybe allow negative b.
        if (isSuccess() && (bounds == FULL || b >= 0)) {
            return {static_cast<int64_t>(value_) + static_cast<int64_t>(b), this};
        }
        return {this};
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    friend Safe32 operator+(const Safe32 &a, T b) { return a.add(b); }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    friend Safe32 operator+(T a, const Safe32 &b) { return b.add(a); }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    Safe32 mul(T b) const {
        if (isSuccess() && (bounds == FULL || b >= 0)) {
            return {static_cast<int64_t>(value_) * static_cast<int64_t>(b), this};
        }
        return {this};
    }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    friend Safe32 operator*(const Safe32 &a, T b) { return a.mul(b); }

    template<typename T, typename = std::enable_if_t<std::is_integral_v<T>>>
    friend Safe32 operator*(T a, const Safe32 &b) { return b.mul(a); }

    // Error code before value, so that they are initialized in that order.
    UErrorCode internalErrorCode_ = U_ZERO_ERROR;
    int32_t value_;
    UErrorCode &errorCode_;
};

/**
 * 32-bit non-negative integer (0..INT32_MAX).
 * Usually used for the length or capacity of a string or buffer or similar.
 * When the value is outside the supported range,
 * it is set to 0, and a failure error code is set.
 */
using NonNeg32 = Safe32<NON_NEG>;

/**
 * Full-range 32-bit integer (INT32_MIN..INT32_MAX).
 * When the value does not fit into an int32_t,
 * it is set to 0, and a failure error code is set.
 */
using Int32 = Safe32<FULL>;

U_NAMESPACE_END

#endif  // __SAFE32_H__
