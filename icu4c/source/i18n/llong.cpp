/*
******************************************************************************
*   Copyright (C) 1997-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*   file name:  llong.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
* Modification history
* Date        Name      Comments
* 10/11/2001  Doug      Ported from ICU4J (thanks to Mike Cowlishaw)
*/

#include "llong.h"
#include <float.h>

U_NAMESPACE_BEGIN

#if 0
/*
 * This should work, I think, but SOLARISCC -xO3 can't handle it.
 * Works with SOLARISGCC, SOLARISCC -g, Win32...
 *
 */
const llong& llong::kMaxValue = llong(0x7fffffff, 0xffffffff);
const llong& llong::kMinValue = llong(0x80000000, 0x0);
const llong& llong::kMinusOne = llong(0xffffffff, 0xffffffff);
const llong& llong::kZero = llong(0x0, 0x0);
const llong& llong::kOne = llong(0x0, 0x1);
const llong& llong::kTwo = llong(0x0, 0x2);
const llong& llong::kMaxDouble = llong(0x200000, 0x0);
const llong& llong::kMinDouble = -kMaxDouble;

static llong kMaxValueObj(0x7fffffff, 0xffffffff);
static llong kMinValueObj(0x80000000, 0x0);
static llong kMinusOneObj(0xffffffff, 0xffffffff);
static llong kZeroObj(0x0, 0x0);
static llong kOneObj(0x0, 0x1);
static llong kTwoObj(0x0, 0x2);
static llong kMaxDoubleObj(0x200000, 0x0);
static llong kMinDoubleObj(-kMaxDoubleObj);

const llong& llong::kMaxValue = kMaxValueObj;
const llong& llong::kMinValue = kMinValueObj;
const llong& llong::kMinusOne = kMinusOneObj;
const llong& llong::kZero = kZeroObj;
const llong& llong::kOne = kOneObj;
const llong& llong::kTwo = kTwoObj;
const llong& llong::kMaxDouble = kMaxDoubleObj;
const llong& llong::kMinDouble = kMinDoubleObj;

const double llong::kDMax = llong_asDouble(kMaxDouble);
const double llong::kDMin = -kDMax;
#endif

#define SQRT231 46340

const double llong::kD32 = ((double)(0xffffffffu)) + 1;

llong::llong(double d) { // avoid dependency on bit representation of double
    if (uprv_isNaN(d)) {
        hi = 0;
        lo = 0; /* zero */
    } else {
        double mant = uprv_maxMantissa();
        if (d < -mant) {
            d = -mant;
        } else if (d > mant) {
            d = mant;
        }
        UBool neg = d < 0; 
        if (neg) {
            d = -d;
        }
        d = uprv_floor(d);
        hi = (int32_t)uprv_floor(d / kD32);
        d -= kD32 * hi;
        lo = (uint32_t)d;
        if (neg) {
            negate();
        }
    }
}

llong& llong::operator*=(const llong& rhs)
{
    // optimize small positive multiplications
    if (hi == 0 && rhs.hi == 0 && lo < SQRT231 && rhs.lo < SQRT231) {
        lo *= rhs.lo;
    } else {
        int retry = 0;

        llong a(*this);
        if (a.isNegative()) {
            retry = 1;
            a.negate();
        }

        llong b(rhs);
        if (b.isNegative()) {
            retry = 1;
            b.negate();
        }

        llong r;
        // optimize small negative multiplications
        if (retry && a.hi == 0 && b.hi == 0 && a.lo < SQRT231 && b.lo < SQRT231) {
            r.lo = a.lo * b.lo;
        } else {
            if (a < b) {
                llong t = a;
                a = b;
                b = t;
            }
            while (b.notZero()) {
                if (b.lo & 0x1) {
                    r += a;
                }
                b >>= 1;
                a <<= 1;
            }
        }
        if (isNegative() != rhs.isNegative()) {
            r.negate();
        }
        *this = r;
    }
    return *this;
}

llong& llong::operator/=(const llong& rhs) 
{
    if (isZero()) {
        return *this;
    }
    int32_t sign = 1;
    llong a(*this);
    if (a.isNegative()) {
        sign = -1;
        a.negate();
    }
    llong b(rhs);
    if (b.isNegative()) {
        sign = -sign;
        b.negate();
    }

    if (b.isZero()) { // should throw div by zero error
        *this = sign < 0 ? -uprv_maxMantissa() : uprv_maxMantissa();
    } else if (a.hi == 0 && b.hi == 0) {
        *this = (int32_t)(sign * (a.lo / b.lo));
    } else if (b > a) {
        hi = 0;
        lo = 0; /* zero */
    } else if (b == a) {
        *this = sign;
    } else {
        llong r;
        llong m((int32_t)1);

        while (b.ule(a)) { // a positive so topmost bit is 0, this will always terminate
            m <<= 1;
            b <<= 1;
        }

        do {
            m.ushr(1); // don't sign-extend!
            if (m.isZero()) break;

            b.ushr(1);
            if (b <= a) {
                r |= m;
                a -= b;
            }
        } while (a >= rhs);

        if (sign < 0) {
            r.negate();
        }
        *this = r;
    }
    return *this;
}

static const uint8_t asciiDigits[] = { 
    0x30u, 0x31u, 0x32u, 0x33u, 0x34u, 0x35u, 0x36u, 0x37u,
    0x38u, 0x39u, 0x61u, 0x62u, 0x63u, 0x64u, 0x65u, 0x66u,
    0x67u, 0x68u, 0x69u, 0x6au, 0x6bu, 0x6cu, 0x6du, 0x6eu,
    0x6fu, 0x70u, 0x71u, 0x72u, 0x73u, 0x74u, 0x75u, 0x76u,
    0x77u, 0x78u, 0x79u, 0x7au,  
};

static const UChar kUMinus = (UChar)0x002d;
static const char kMinus = 0x2d;

static const uint8_t digitInfo[] = {
        0,     0,     0,     0,     0,     0,     0,     0,
        0,     0,     0,     0,     0,     0,     0,     0,
        0,     0,     0,     0,     0,     0,     0,     0,
        0,     0,     0,     0,     0,     0,     0,     0,
        0,     0,     0,     0,     0,     0,     0,     0,
        0,     0,     0,     0,     0,     0,     0,     0,
    0x80u, 0x81u, 0x82u, 0x83u, 0x84u, 0x85u, 0x86u, 0x87u,
    0x88u, 0x89u,     0,     0,     0,     0,     0,     0,
        0, 0x8au, 0x8bu, 0x8cu, 0x8du, 0x8eu, 0x8fu, 0x90u,
    0x91u, 0x92u, 0x93u, 0x94u, 0x95u, 0x96u, 0x97u, 0x98u,
    0x99u, 0x9au, 0x9bu, 0x9cu, 0x9du, 0x9eu, 0x9fu, 0xa0u,
    0xa1u, 0xa2u, 0xa3u,     0,     0,     0,     0,     0,
        0, 0x8au, 0x8bu, 0x8cu, 0x8du, 0x8eu, 0x8fu, 0x90u,
    0x91u, 0x92u, 0x93u, 0x94u, 0x95u, 0x96u, 0x97u, 0x98u,
    0x99u, 0x9au, 0x9bu, 0x9cu, 0x9du, 0x9eu, 0x9fu, 0xa0u,
    0xa1u, 0xa2u, 0xa3u,     0,     0,     0,     0,     0,
};

llong llong::atoll(const char* str, uint32_t radix)
{
    if (radix > 36) {
        radix = 36;
    } else if (radix < 2) {
        radix = 2;
    }
    llong lradix(radix);

    int neg = 0;
    if (*str == kMinus) {
        ++str;
        neg = 1;
    }
    llong result;
    uint8_t b;
    while ((b = digitInfo[*str++]) && ((b &= 0x7f) < radix)) {
        result *= lradix;
        result += (int32_t)b;
    }
    if (neg) {
        result.negate();
    }
    return result;
}

llong llong::u_atoll(const UChar* str, uint32_t radix)
{
    if (radix > 36) {
        radix = 36;
    } else if (radix < 2) {
        radix = 2;
    }
    llong lradix(radix);

    int neg = 0;
    if (*str == kUMinus) {
        ++str;
        neg = 1;
    }
    llong result;
    UChar c;
    uint8_t b;
    while (((c = *str++) < 0x0080) && (b = digitInfo[c]) && ((b &= 0x7f) < radix)) {
        result *= lradix;
        result += (int32_t)b;
    }
    if (neg) {
        result.negate();
    }
    return result;
}

uint32_t llong::lltoa(char* buf, uint32_t len, uint32_t radix, UBool raw) const
{    
    if (radix > 36) {
        radix = 36;
    } else if (radix < 2) {
        radix = 2;
    }
    llong base(radix);

    char* p = buf;
    llong w(*this);
    if (len && w.isNegative()) {
        w.negate();
        *p++ = kMinus;
        --len;
    }

    while (len && w.notZero()) {
        llong n = w / base;
        llong m = n * base;
        int32_t d = (w-m).asInt();
        *p++ = raw ? (char)d : asciiDigits[d];
        w = n;
        --len;
    }
    if (len) {
        *p = 0; // null terminate if room for caller convenience
    }

    len = p - buf;
    if (*buf == kMinus) {
        ++buf;
    }
    while (--p > buf) {
        char c = *p;
        *p = *buf;
        *buf = c;
        ++buf;
    }

    return len;
}

uint32_t llong::u_lltoa(UChar* buf, uint32_t len, uint32_t radix, UBool raw) const
{    
    if (radix > 36) {
        radix = 36;
    } else if (radix < 2) {
        radix = 2;
    }
    llong base(radix);

    UChar* p = buf;
    llong w(*this);
    if (len && w.isNegative()) {
        w.negate();
        *p++ = kUMinus;
        --len;
    }

    while (len && w.notZero()) {
        llong n = w / base;
        llong m = n * base;
        int32_t d = (w-m).asInt();
        *p++ = (UChar)(raw ? d : asciiDigits[d]);
        w = n;
        --len;
    }
    if (len) {
        *p = 0; // null terminate if room for caller convenience
    }

    len = p - buf;
    if (*buf == kUMinus) {
        ++buf;
    }
    while (--p > buf) {
        UChar c = *p;
        *p = *buf;
        *buf = c;
        ++buf;
    }

    return len;
}

U_NAMESPACE_END
