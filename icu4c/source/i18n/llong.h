// thanks to Mike Cowlishaw

#ifndef LLONG_H
#define LLONG_H

// debug
#include <stdio.h>

#include "unicode/utypes.h"

// machine dependent value, need to move
#define __u_IntBits 32

class llong {
public:
	uint32_t lo;
	int32_t hi;
private:
	enum { 
		MASK32 = 0xffffffffu
	};

	static const double kD32;  // 2^^32 as a double
	static const double kDMin; // -(2^^54), minimum double with full integer precision
	static const double kDMax; // 2^^54, maximum double with full integer precision

	// private constructor
    // should be private, but we can't construct the way we want using SOLARISCC
    // so make public in order that file statics can access this constructor
 public:
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
#if __u_IntBits == 64
	llong(const int i) : lo(i & MASK32), hi(i >> 32) {}
#endif
	llong(uint16_t s) : lo(s), hi(0) {}
	llong(uint32_t l) : lo(l), hi(0) {}
#if __u_IntBits == 64
	llong(unsigned int i) : lo(i & MASK32), hi(i >> 32) {}
#endif
	llong(double d) { // avoid dependency on bit representation of double
		if (uprv_isNaN(d)) {
			*this = llong::kZero;
		} else if (d < kDMin) {
			*this = llong::kMinDouble;
		} else if (d > kDMax) {
			*this = llong::kMaxDouble;
		} else {
			int neg = d < 0; 
			if (neg) d = -d; 
			d = uprv_floor(d);
			hi = (int32_t)uprv_floor(d / kD32);
			d -= kD32 * hi;
			lo = (uint32_t)d;
			if (neg) negate();
		}
	}

 	llong(const llong& rhs) : lo(rhs.lo), hi(rhs.hi) {}

	// the following cause ambiguities in binary expressions, 
	// even if we overload all methods on all args!
	// so you have to use global functions
	// operator const int32_t() const;
	// operator const uint32_t() const;
	// operator const double() const;

	friend int32_t  llong_asInt(const llong& lhs);
	friend uint32_t llong_asUInt(const llong& lhs);
	friend double   llong_asDouble(const llong& lhs);

	llong& operator=(const llong& rhs) { lo = rhs.lo; hi = rhs.hi; return *this; }

	// left shift
	llong& operator<<=(int32_t shift) {
		shift &= 63; // like java spec
		if (shift < 32) {
			hi = (signed)(hi << shift | lo >> (32 - shift)); // no sign extension on lo since unsigned
			lo <<= shift;
		} else {
			hi = (signed)(lo << (shift - 32));
			lo = 0;
		}
		return *this;
	}
	llong operator<<(int32_t shift) const { llong r(*this); r <<= shift; return r; }

	// right shift with sign extension
	llong& operator>>=(int32_t shift) {
		shift &= 63; // like java spec
		if (shift < 32) {
			lo >>= shift; 
			lo |= (hi << (32 - shift)); 
			hi = hi >> shift; // note sign extension
		} else {
			lo = (unsigned)(hi >> (shift - 32)); // note sign extension
			hi = hi < 0 ? -1 : 0;
		}
		return *this; 
	}
	llong operator>>(int32_t shift) const { llong r(*this); r >>= shift; return r; }

	// unsigned right shift
	friend llong ushr(const llong& lhs, int32_t shift);

	// bit operations
 	friend llong operator&(const llong& lhs, const llong& rhs);
	friend llong operator|(const llong& lhs, const llong& rhs);
	friend llong operator^(const llong& lhs, const llong& rhs);

 	friend llong operator&(const llong& lhs, const uint32_t rhs);
	friend llong operator|(const llong& lhs, const uint32_t rhs);
	friend llong operator^(const llong& lhs, const uint32_t rhs);

	llong operator~() const { return llong(~hi, ~lo); }
	// is this useful?
	// UBool operator!() const { return !(hi | lo); }

	llong& operator&=(const llong& rhs) { hi &= rhs.hi; lo &= rhs.lo; return *this; }
	llong& operator|=(const llong& rhs) { hi |= rhs.hi; lo |= rhs.lo; return *this; }
	llong& operator^=(const llong& rhs) { hi ^= rhs.hi; lo ^= rhs.lo; return *this; }

	llong& operator&=(const uint32_t rhs) { hi = 0; lo &= rhs; return *this; }
	llong& operator|=(const uint32_t rhs) { lo |= rhs; return *this; }
	llong& operator^=(const uint32_t rhs) { lo ^= rhs; return *this; }

	// no logical ops since we can't enforce order of evaluation, not much use anyway?

	// comparison
    friend UBool operator==(const llong& lhs, const llong& rhs);
	friend UBool operator!=(const llong& lhs, const llong& rhs);
	friend UBool operator> (const llong& lhs, const llong& rhs);
	friend UBool operator< (const llong& lhs, const llong& rhs);
	friend UBool operator>=(const llong& lhs, const llong& rhs);
	friend UBool operator<=(const llong& lhs, const llong& rhs);

	// overload comparison to native int to avoid conversion to llong for common comparisons
    friend UBool operator==(const llong& lhs, const int32_t rhs);
	friend UBool operator!=(const llong& lhs, const int32_t rhs);
	friend UBool operator> (const llong& lhs, const int32_t rhs);
	friend UBool operator< (const llong& lhs, const int32_t rhs);
	friend UBool operator>=(const llong& lhs, const int32_t rhs);
	friend UBool operator<=(const llong& lhs, const int32_t rhs);

	// unsigned comparison
	friend UBool ugt(const llong& lhs, const llong& rhs);
	friend UBool ult(const llong& lhs, const llong& rhs);
	friend UBool uge(const llong& lhs, const llong& rhs);
	friend UBool ule(const llong& lhs, const llong& rhs);

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
	friend llong operator-(const llong& lhs, const llong& rhs);

	llong& operator+=(const llong& rhs) { return *this -= -rhs; }
	friend llong operator+(const llong& lhs, const llong& rhs);

	// pluttification and fizzen'
	llong& operator*=(const llong& rhs);
	friend llong operator*(const llong& lhs, const llong& rhs);

	llong& operator/=(const llong& rhs);
	friend llong operator/(const llong& lhs, const llong& rhs);

	llong& operator%=(const llong& rhs) { return operator-=((*this / rhs) * rhs); }
	friend llong operator%(const llong& lhs, const llong& rhs);

	// power function, positive integral powers only
	friend llong llong_pow(const llong& lhs, uint32_t n);

	// absolute value
	friend llong llong_abs(const llong& lhs);

	// simple construction from ASCII and Unicode strings
	friend llong atoll(const char* str, uint32_t radix = 10);
	friend llong u_atoll(const UChar* str, uint32_t radix = 10);

	// output as ASCII or Unicode strings or as raw values, preceeding '-' if signed
	friend uint32_t lltoa(const llong& lhs, char* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = FALSE);
	friend uint32_t u_lltoa(const llong& lhs, UChar* buffer, uint32_t buflen, uint32_t radix = 10, UBool raw = FALSE);

	// useful public constants - perhaps should not have class statics
	static const llong& kMaxValue;
	static const llong& kMinValue;
	static const llong& kMinusOne;
	static const llong& kZero;
	static const llong& kOne;
	static const llong& kTwo;

private:
	static const llong& kMaxDouble;
	static const llong& kMinDouble;

	// right shift without sign extension
	llong& ushr(int32_t shift) {
		shift &= 0x63;
		if (shift < 32) {
			lo >>= shift;
			lo |= (hi << (32 - shift));
			hi = (signed)(((unsigned)hi) >> shift);
		} else {
			lo = (unsigned)(((unsigned)hi) >> (shift - 32));
			hi = 0;
		}
		return *this;
	}

	// back door for test
	friend void llong_test();
};

inline llong operator& (const llong& lhs, const llong& rhs) { return llong(lhs.hi & rhs.hi, lhs.lo & rhs.lo); }
inline llong operator| (const llong& lhs, const llong& rhs) { return llong(lhs.hi | rhs.hi, lhs.lo | rhs.lo); }
inline llong operator^ (const llong& lhs, const llong& rhs) { return llong(lhs.hi ^ rhs.hi, lhs.lo ^ rhs.lo); }

inline llong operator& (const llong& lhs, const uint32_t rhs) { return llong(0, lhs.lo & rhs); }
inline llong operator| (const llong& lhs, const uint32_t rhs) { return llong(lhs.hi, lhs.lo | rhs); }
inline llong operator^ (const llong& lhs, const uint32_t rhs) { return llong(lhs.hi, lhs.lo ^ rhs); }

inline UBool operator==(const llong& lhs, const llong& rhs) { return lhs.lo == rhs.lo && lhs.hi == rhs.hi; }
inline UBool operator!=(const llong& lhs, const llong& rhs) { return lhs.lo != rhs.lo || lhs.hi != rhs.hi; }
inline UBool operator> (const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo > rhs.lo : lhs.hi > rhs.hi; }
inline UBool operator< (const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo < rhs.lo : lhs.hi < rhs.hi; }
inline UBool operator>=(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo >= rhs.lo : lhs.hi >= rhs.hi; }
inline UBool operator<=(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo <= rhs.lo : lhs.hi <= rhs.hi; }

inline UBool operator==(const llong& lhs, const int32_t rhs) { return lhs.lo == (unsigned)rhs && lhs.hi == (rhs < 0 ? -1 : 0); }
inline UBool operator!=(const llong& lhs, const int32_t rhs) { return lhs.lo != (unsigned)rhs || lhs.hi != (rhs < 0 ? -1 : 0); }
inline UBool operator> (const llong& lhs, const int32_t rhs) { return rhs < 0 ? (lhs.hi == -1 ? lhs.lo > (unsigned)rhs : lhs.hi > -1)
                                                                              : (lhs.hi ==  0 ? lhs.lo > (unsigned)rhs : lhs.hi >  0); }
inline UBool operator< (const llong& lhs, const int32_t rhs) { return rhs < 0 ? (lhs.hi == -1 ? lhs.lo < (unsigned)rhs : lhs.hi < -1)
                                                                              : (lhs.hi ==  0 ? lhs.lo < (unsigned)rhs : lhs.hi <  0); }
inline UBool operator>=(const llong& lhs, const int32_t rhs) { return rhs < 0 ? (lhs.hi == -1 ? lhs.lo >= (unsigned)rhs : lhs.hi > -1)
                                                                              : (lhs.hi ==  0 ? lhs.lo >= (unsigned)rhs : lhs.hi >  0); }
inline UBool operator<=(const llong& lhs, const int32_t rhs) { return rhs < 0 ? (lhs.hi == -1 ? lhs.lo <= (unsigned)rhs : lhs.hi < -1)
                                                                              : (lhs.hi ==  0 ? lhs.lo <= (unsigned)rhs : lhs.hi <  0); }

inline UBool ugt(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo > rhs.lo : (unsigned)lhs.hi > (unsigned)rhs.hi; }
inline UBool ult(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo < rhs.lo : (unsigned)lhs.hi < (unsigned)rhs.hi; }
inline UBool uge(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo >= rhs.lo : (unsigned)lhs.hi >= (unsigned)rhs.hi; }
inline UBool ule(const llong& lhs, const llong& rhs) { return lhs.hi == rhs.hi ? lhs.lo <= rhs.lo : (unsigned)lhs.hi <= (unsigned)rhs.hi; }

inline llong ushr(const llong& lhs, int32_t shift) { llong r(lhs); r.ushr(shift); return r; }

inline llong operator-(const llong& lhs, const llong& rhs) { return llong(lhs.lo < rhs.lo ? lhs.hi - rhs.hi - 1 : lhs.hi - rhs.hi, lhs.lo - rhs.lo); }
inline llong operator+(const llong& lhs, const llong& rhs) { return lhs - -rhs; }

inline llong operator*(const llong& lhs, const llong& rhs) { llong r(lhs); r *= rhs; return r; }
inline llong operator/(const llong& lhs, const llong& rhs) { llong r(lhs); r /= rhs; return r; }
inline llong operator%(const llong& lhs, const llong& rhs) { llong r(lhs); r %= rhs; return r; }

inline int32_t  llong_asInt(const llong& lhs) { return (int32_t)(lhs.lo | (lhs.hi < 0 ? 0x80000000 : 0)); }
inline uint32_t llong_asUInt(const llong& lhs) { return lhs.lo; }
inline double   llong_asDouble(const llong& lhs) { return llong::kD32 * lhs.hi + lhs.lo; }

inline llong llong_pow(const llong& lhs, uint32_t n) { 
	if (lhs.isZero()) {
		return llong::kZero;
	} else if (n == 0) {
		return llong::kOne;
	} else {
		llong r(lhs);
		while (--n > 0) {
			r *= lhs;
		}
		return r;
	}
}

inline llong llong_abs(const llong& lhs) { return lhs.isNegative() ? -lhs : lhs; }

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

// LLONG_H
#endif
