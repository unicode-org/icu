#include "llong.h"

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
#endif

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

#define SQRT231 46340

const double llong::kD32 = ((double)(0xffffffffu)) + 1;
const double llong::kDMax = llong_asDouble(kMaxDouble);
const double llong::kDMin = -kDMax;

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
		*this = sign < 0 ? kMinValue : kMaxValue;
	} else if (a.hi == 0 && b.hi == 0) {
		*this = (int32_t)(sign * (a.lo / b.lo));
	} else if (b > a) {
		*this = kZero;
	} else if (b == a) {
		*this = sign;
	} else {
		llong r;
		llong m((int32_t)1);

		while (ule(b, a)) { // a positive so topmost bit is 0, this will always terminate
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

static uint8_t asciiDigits[] = { 
	(char)0x30, (char)0x31, (char)0x32, (char)0x33, (char)0x34, (char)0x35, (char)0x36, (char)0x37,
	(char)0x38, (char)0x39, (char)0x61, (char)0x62, (char)0x63, (char)0x64, (char)0x65, (char)0x66,
	(char)0x67, (char)0x68, (char)0x69, (char)0x6a, (char)0x6b, (char)0x6c, (char)0x6d, (char)0x6e,
	(char)0x6f, (char)0x70, (char)0x71, (char)0x72, (char)0x73, (char)0x74, (char)0x75, (char)0x76,
	(char)0x77, (char)0x78, (char)0x79, (char)0x7a,  
};

static UChar kUMinus = (UChar)0x002d;
static char kMinus = (char)0x2d;

static uint8_t digitInfo[] = {
	   0,    0,    0,    0,    0,    0,    0,    0,      0,    0,    0,    0,    0,    0,    0,    0,
	   0,    0,    0,    0,    0,    0,    0,    0,      0,    0,    0,    0,    0,    0,    0,    0,
	   0,    0,    0,    0,    0,    0,    0,    0,      0,    0,    0,    0,    0,    0,    0,    0,
	0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,   0x88, 0x89,    0,    0,    0,    0,    0,    0,
	   0, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f, 0x90,   0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
	0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, 0xa0,   0xa1, 0xa2, 0xa3,    0,    0,    0,    0,    0,
	   0, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f, 0x90,   0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
	0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, 0xa0,   0xa1, 0xa2, 0xa3,    0,    0,    0,    0,    0,
};

llong atoll(const char* str, uint32_t radix)
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

llong u_atoll(const UChar* str, uint32_t radix)
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

uint32_t lltoa(const llong& val, char* buf, uint32_t len, uint32_t radix, UBool raw)
{	
	if (radix > 36) {
		radix = 36;
	} else if (radix < 2) {
		radix = 2;
	}
	llong base(radix);

	char* p = buf;
	llong w(val);
	if (len && w.isNegative()) {
		w.negate();
		*p++ = kMinus;
		--len;
	}

	while (len && w.notZero()) {
		llong n = w / base;
		llong m = n * base;
		int32_t d = llong_asInt(w-m);
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

uint32_t u_lltoa(const llong& val, UChar* buf, uint32_t len, uint32_t radix, UBool raw)
{	
	if (radix > 36) {
		radix = 36;
	} else if (radix < 2) {
		radix = 2;
	}
	llong base(radix);

	UChar* p = buf;
	llong w(val);
	if (len && w.isNegative()) {
		w.negate();
		*p++ = kUMinus;
		--len;
	}

	while (len && w.notZero()) {
		llong n = w / base;
		llong m = n * base;
		int32_t d = llong_asInt(w-m);
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
