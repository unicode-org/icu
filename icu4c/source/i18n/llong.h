/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   file name:  llong.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 10/11/2001  Doug      Ported from ICU4J (thanks to Mike Cowlishaw)
*/

#ifndef LLONG_H
#define LLONG_H

#include "unicode/utypes.h"

U_NAMESPACE_BEGIN

// machine dependent value, need to move
//#define __u_IntBits 32

class llong {
private:
    uint32_t lo;
    int32_t hi;
private:

    // private constructor
    // should be private, but we can't construct the way we want using SOLARISCC
    // so make public in order that file statics can access this constructor
public:
    static const double kD32;  // 2^^32 as a double
    static const double kDMin; // -(2^^54), minimum double with full integer precision
    static const double kDMax; // 2^^54, maximum double with full integer precision

    llong(int32_t h, uint32_t l) : lo(l), hi(h) {}
private:
    // convenience, size reduction in inline code
    llong& nnot() { hi = ~hi; lo = ~lo; return *this; }
    llong& negate() { hi = ~hi; lo = ~lo; if (!++lo) ++hi; return *this; }
    llong& abs() { if (hi < 0) negate(); return *this; }
    UBool notZero() const { return (hi | lo) != 0; }
    UBool isZero() const { return (hi | lo) == 0; }
    UBool isNegative() const { return hi < 0; }

public:
    llong() : lo(0), hi(0) {}
    llong(const int32_t l) : lo((unsigned)l), hi(l < 0 ? -1 : 0) {}
    llong(const int16_t l) : lo((unsigned)l), hi(l < 0 ? -1 : 0) {}
    llong(const int8_t l) : lo((unsigned)l), hi(l < 0 ? -1 : 0) {}
//#if __u_IntBits == 64
//    llong(const int i) : lo(i & MASK32), hi(i >> 32) {}
//#endif
    llong(uint16_t s) : lo(s), hi(0) {}
    llong(uint32_t l) : lo(l), hi(0) {}
//#if __u_IntBits == 64
//    llong(unsigned int i) : lo(i & MASK32), hi(i >> 32) {}
//#endif
    llong(double d);

    llong(const llong& rhs) : lo(rhs.lo), hi(rhs.hi) {}

    // the following cause ambiguities in binary expressions, 
    // even if we overload all methods on all args!
    // so you have to use global functions
    // operator const int32_t() const;
    // operator const uint32_t() const;
    // operator const double() const;

    inline int32_t  asInt() const;
    inline uint32_t asUInt() const;
    inline double   asDouble() const;

    inline llong& operator=(const llong& rhs) { lo = rhs.lo; hi = rhs.hi; return *this; }

    // left shift
    inline llong& operator<<=(int32_t shift) {
        shift &= 63; // like java spec
        if (shift < 32) {
            hi = (int32_t)(hi << shift | lo >> (32 - shift)); // no sign extension on lo since unsigned
            lo <<= shift;
        } else {
            hi = (int32_t)(lo << (shift - 32));
            lo = 0;
        }
        return *this;
    }
    inline llong operator<<(int32_t shift) const { llong r(*this); r <<= shift; return r; }

    // right shift with sign extension
    inline llong& operator>>=(int32_t shift) {
        shift &= 63; // like java spec
        if (shift < 32) {
            lo >>= shift; 
            lo |= (hi << (32 - shift)); 
            hi = hi >> shift; // note sign extension
        } else {
            lo = (uint32_t)(hi >> (shift - 32)); // note sign extension
            hi = hi < 0 ? -1 : 0;
        }
        return *this; 
    }
    inline llong operator>>(int32_t shift) const { llong r(*this); r >>= shift; return r; }

    // unsigned right shift
    inline llong ushr(int32_t shift) const;

    // bit operations
    inline llong operator&(const llong& rhs) const;
    inline llong operator|(const llong& rhs) const;
    inline llong operator^(const llong& rhs) const;

    inline llong operator&(const uint32_t rhs) const;
    inline llong operator|(const uint32_t rhs) const;
    inline llong operator^(const uint32_t rhs) const;

    llong operator~() const { return llong(~hi, ~lo); }
    // is this useful?
    // UBool operator!() const { return !(hi | lo); }

    inline llong& operator&=(const llong& rhs) { hi &= rhs.hi; lo &= rhs.lo; return *this; }
    inline llong& operator|=(const llong& rhs) { hi |= rhs.hi; lo |= rhs.lo; return *this; }
    inline llong& operator^=(const llong& rhs) { hi ^= rhs.hi; lo ^= rhs.lo; return *this; }

    inline llong& operator&=(const uint32_t rhs) { hi = 0; lo &= rhs; return *this; }
    inline llong& operator|=(const uint32_t rhs) { lo |= rhs; return *this; }
    inline llong& operator^=(const uint32_t rhs) { lo ^= rhs; return *this; }

    // no logical ops since we can't enforce order of evaluation, not much use anyway?

    // comparison
    inline UBool operator==(const llong& rhs) const;
    inline UBool operator!=(const llong& rhs) const;
    inline UBool operator> (const llong& rhs) const;
    inline UBool operator< (const llong& rhs) const;
    inline UBool operator>=(const llong& rhs) const;
    inline UBool operator<=(const llong& rhs) const;

    // overload comparison to native int to avoid conversion to llong for common comparisons
    inline UBool operator==(const int32_t rhs) const;
    inline UBool operator!=(const int32_t rhs) const;
    inline UBool operator> (const int32_t rhs) const;
    inline UBool operator< (const int32_t rhs) const;
    inline UBool operator>=(const int32_t rhs) const;
    inline UBool operator<=(const int32_t rhs) const;

    // unsigned comparison
    inline UBool ugt(const llong& rhs) const;
    inline UBool ult(const llong& rhs) const;
    inline UBool uge(const llong& rhs) const;
    inline UBool ule(const llong& rhs) const;

    // prefix inc/dec
    llong& operator++() { if (!++lo) ++hi; return *this; }
    llong& operator--() { if (!lo--) --hi; return *this; }

    // postfix inc/dec
    llong operator++(int) { llong r(*this); if (!++lo) ++hi; return r; }
    llong operator--(int) { llong r(*this); if (!lo--) --hi; return r; }
    
    // unary minus
    llong operator-() const { uint32_t l = ~lo + 1; return llong(l ? ~hi : ~hi + 1, l); }

    // addition and subtraction
    llong& operator-=(const llong& rhs) { hi -= rhs.hi; if (lo < rhs.lo) --hi; lo -= rhs.lo; return *this; }
    inline llong operator-(const llong& rhs) const { return llong(lo < rhs.lo ? hi - rhs.hi - 1 : hi - rhs.hi, lo - rhs.lo); }

    inline llong& operator+=(const llong& rhs) { return *this -= -rhs; }
    inline llong operator+(const llong& rhs) const { return *this - -rhs; }

    // pluttification and fizzen'
    llong& operator*=(const llong& rhs);
    inline llong operator*(const llong& rhs) const { llong r(*this); r *= rhs; return r; }

    llong& operator/=(const llong& rhs);
    inline llong operator/(const llong& rhs) const { llong r(*this); r /= rhs; return r; }

    llong& operator%=(const llong& rhs) { return operator-=((*this / rhs) * rhs); }
    inline llong operator%(const llong& rhs) const { llong r(*this); r %= rhs; return r; }

    // power function, positive integral powers only
    inline llong pow(uint32_t n) const;

    // absolute value
    llong abs() const;

    // simple construction from ASCII and Unicode strings
    static llong atoll(const char* str, uint32_t radix = 10);
    static llong u_atoll(const UChar* str, uint32_t radix = 10);

    // output as ASCII or Unicode strings or as raw values, preceeding '-' if signed
    uint32_t lltoa(char* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = FALSE) const;
    uint32_t u_lltoa(UChar* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = FALSE) const;

    // useful public constants - perhaps should not have class statics
//    static const llong getZero();
//    static const llong getOne();

private:
    static const llong getMaxDouble();
    static const llong getMinDouble();

    // right shift without sign extension
    llong& ushr(int32_t shift) {
        shift &= 0x63;
        if (shift < 32) {
            lo >>= shift;
            lo |= (hi << (32 - shift));
            hi = (int32_t)(((unsigned)hi) >> shift);
        } else {
            lo = (uint32_t)(((unsigned)hi) >> (shift - 32));
            hi = 0;
        }
        return *this;
    }

    // back door for test
    friend void llong_test();
};

inline llong llong::operator& (const llong& rhs) const { return llong(hi & rhs.hi, lo & rhs.lo); }
inline llong llong::operator| (const llong& rhs) const { return llong(hi | rhs.hi, lo | rhs.lo); }
inline llong llong::operator^ (const llong& rhs) const { return llong(hi ^ rhs.hi, lo ^ rhs.lo); }

inline llong llong::operator& (const uint32_t rhs) const { return llong(0, lo & rhs); }
inline llong llong::operator| (const uint32_t rhs) const { return llong(hi, lo | rhs); }
inline llong llong::operator^ (const uint32_t rhs) const { return llong(hi, lo ^ rhs); }

inline UBool llong::operator==(const llong& rhs) const { return lo == rhs.lo && hi == rhs.hi; }
inline UBool llong::operator!=(const llong& rhs) const { return lo != rhs.lo || hi != rhs.hi; }
inline UBool llong::operator> (const llong& rhs) const { return hi == rhs.hi ? lo > rhs.lo : hi > rhs.hi; }
inline UBool llong::operator< (const llong& rhs) const { return hi == rhs.hi ? lo < rhs.lo : hi < rhs.hi; }
inline UBool llong::operator>=(const llong& rhs) const { return hi == rhs.hi ? lo >= rhs.lo : hi >= rhs.hi; }
inline UBool llong::operator<=(const llong& rhs) const { return hi == rhs.hi ? lo <= rhs.lo : hi <= rhs.hi; }

inline UBool llong::operator==(const int32_t rhs) const { return lo == (unsigned)rhs && hi == (rhs < 0 ? -1 : 0); }
inline UBool llong::operator!=(const int32_t rhs) const { return lo != (unsigned)rhs || hi != (rhs < 0 ? -1 : 0); }
inline UBool llong::operator> (const int32_t rhs) const { return rhs < 0 ? (hi == -1 ? lo > (unsigned)rhs : hi > -1)
                                                                              : (hi ==  0 ? lo > (unsigned)rhs : hi >  0); }
inline UBool llong::operator< (const int32_t rhs) const { return rhs < 0 ? (hi == -1 ? lo < (unsigned)rhs : hi < -1)
                                                                              : (hi ==  0 ? lo < (unsigned)rhs : hi <  0); }
inline UBool llong::operator>=(const int32_t rhs) const { return rhs < 0 ? (hi == -1 ? lo >= (unsigned)rhs : hi > -1)
                                                                              : (hi ==  0 ? lo >= (unsigned)rhs : hi >  0); }
inline UBool llong::operator<=(const int32_t rhs) const { return rhs < 0 ? (hi == -1 ? lo <= (unsigned)rhs : hi < -1)
                                                                              : (hi ==  0 ? lo <= (unsigned)rhs : hi <  0); }

inline UBool llong::ugt(const llong& rhs) const { return hi == rhs.hi ? lo > rhs.lo : (unsigned)hi > (unsigned)rhs.hi; }
inline UBool llong::ult(const llong& rhs) const { return hi == rhs.hi ? lo < rhs.lo : (unsigned)hi < (unsigned)rhs.hi; }
inline UBool llong::uge(const llong& rhs) const { return hi == rhs.hi ? lo >= rhs.lo : (unsigned)hi >= (unsigned)rhs.hi; }
inline UBool llong::ule(const llong& rhs) const { return hi == rhs.hi ? lo <= rhs.lo : (unsigned)hi <= (unsigned)rhs.hi; }

inline llong llong::ushr(int32_t shift) const { llong r(*this); r.ushr(shift); return r; }

inline int32_t  llong::asInt() const { return (int32_t)(lo | (hi < 0 ? 0x80000000 : 0)); }
inline uint32_t llong::asUInt() const { return lo; }
inline double   llong::asDouble() const { return llong::kD32 * hi + lo; }

inline llong llong::pow(uint32_t n) const { 
    if (isZero()) {
        return llong(0, 0); /* zero */
    } else if (n == 0) {
        return llong(0, 1); /* one */
    } else {
        llong r(*this);
        while (--n > 0) {
            r *= *this;
        }
        return r;
    }
}

inline llong llong::abs() const { return isNegative() ? -(*this) : *this; }

// Originally, I thought that overloading on int32 was too complex or to large to get inlined, and 
// since I mainly wanted to optimize comparisons to zero, I overloaded on uint32_t instead
// since it has a simpler implementation.
// But this means that llong(-1) != -1 (since the comparison treats the rhs as unsigned, but 
// the constructor does not).  So I am using the signed versions after all.

#if 0
inline UBool operator==(const llong& lhs, const uint32_t rhs) { return lhs.lo == rhs && lhs.hi == 0; }
inline UBool operator!=(const llong& lhs, const uint32_t rhs) { return lhs.lo != rhs || lhs.hi != 0; }
inline UBool operator> (const llong& lhs, const uint32_t rhs) { return lhs.hi == 0 ? lhs.lo > rhs : lhs.hi > 0; }
inline UBool operator< (const llong& lhs, const uint32_t rhs) { return lhs.hi == 0 ? lhs.lo < rhs : lhs.hi < 0; }
inline UBool operator>=(const llong& lhs, const uint32_t rhs) { return lhs.hi == 0 ? lhs.lo >= rhs : lhs.hi >= 0; }
inline UBool operator<=(const llong& lhs, const uint32_t rhs) { return lhs.hi == 0 ? lhs.lo <= rhs : lhs.hi <= 0; }
#endif

U_NAMESPACE_END

// LLONG_H
#endif
