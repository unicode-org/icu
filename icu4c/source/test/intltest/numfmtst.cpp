/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2009, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/* Modification History:
*   Date        Name        Description
*   07/15/99    helena      Ported to HPUX 10/11 CC.
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "numfmtst.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/ucurr.h"
#include "unicode/ustring.h"
#include "unicode/measfmt.h"
#include "unicode/curramt.h"
#include "digitlst.h"
#include "textfile.h"
#include "tokiter.h"
#include "charstr.h"
#include "putilimp.h"
#include "winnmtst.h"
#include <float.h>
#include <string.h>
#include <stdlib.h>
#include "cstring.h"

//#define NUMFMTST_CACHE_DEBUG 1
#ifdef NUMFMTST_CACHE_DEBUG 
#include "stdio.h"
#endif

//#define NUMFMTST_DEBUG 1
#ifdef NUMFMTST_DEBUG 
#include "stdio.h"
#endif


static const UChar EUR[] = {69,85,82,0}; // "EUR"
static const UChar ISO_CURRENCY_USD[] = {0x55, 0x53, 0x44, 0}; // "USD"
 
// *****************************************************************************
// class NumberFormatTest
// *****************************************************************************

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break

#define CHECK(status,str) if (U_FAILURE(status)) { errln(UnicodeString("FAIL: ") + str); return; }

void NumberFormatTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    // if (exec) logln((UnicodeString)"TestSuite DateFormatTest");
    switch (index) {
        CASE(0,TestCurrencySign);
        CASE(1,TestCurrency);
        CASE(2,TestParse);
        CASE(3,TestRounding487);
        CASE(4,TestQuotes);
        CASE(5,TestExponential);
        CASE(6,TestPatterns);

        // Upgrade to alphaWorks - liu 5/99
        CASE(7,TestExponent);
        CASE(8,TestScientific);
        CASE(9,TestPad);
        CASE(10,TestPatterns2);
        CASE(11,TestSecondaryGrouping);
        CASE(12,TestSurrogateSupport);
        CASE(13,TestAPI);

        CASE(14,TestCurrencyObject);
        CASE(15,TestCurrencyPatterns);
        //CASE(16,TestDigitList);
        CASE(16,TestWhiteSpaceParsing);
        CASE(17,TestComplexCurrency);
        CASE(18,TestRegCurrency);
        CASE(19,TestSymbolsWithBadLocale);
        CASE(20,TestAdoptDecimalFormatSymbols);

        CASE(21,TestScientific2);
        CASE(22,TestScientificGrouping);
        CASE(23,TestInt64);

        CASE(24,TestPerMill);
        CASE(25,TestIllegalPatterns);
        CASE(26,TestCases);

        CASE(27,TestCurrencyNames);
        CASE(28,TestCurrencyAmount);
        CASE(29,TestCurrencyUnit);
        CASE(30,TestCoverage);
        CASE(31,TestJB3832);
        CASE(32,TestHost);
        CASE(33,TestHostClone);
        CASE(34,TestCurrencyFormat);
        CASE(35,TestRounding);
        CASE(36,TestNonpositiveMultiplier);
        CASE(37,TestNumberingSystems);
        CASE(38,TestSpaceParsing);
        CASE(39,TestMultiCurrencySign);
        CASE(40,TestCurrencyFormatForMixParsing);
        CASE(41,TestDecimalFormatCurrencyParse);
        CASE(42,TestCurrencyIsoPluralFormat);
        CASE(43,TestCurrencyParsing);
        CASE(44,TestParseCurrencyInUCurr);
        default: name = ""; break;
    }
}
 
// -------------------------------------

// Test API (increase code coverage)
void
NumberFormatTest::TestAPI(void)
{
  logln("Test API");
  UErrorCode status = U_ZERO_ERROR;
  NumberFormat *test = NumberFormat::createInstance("root", status);
  if(U_FAILURE(status)) {
    errln("unable to create format object");
  }
  if(test != NULL) {
    test->setMinimumIntegerDigits(10);
    test->setMaximumIntegerDigits(2);

    test->setMinimumFractionDigits(10);
    test->setMaximumFractionDigits(2);

    UnicodeString result;
    FieldPosition pos;
    Formattable bla("Paja Patak"); // Donald Duck for non Serbian speakers
    test->format(bla, result, pos, status);
    if(U_SUCCESS(status)) {
      errln("Yuck... Formatted a duck... As a number!");
    } else {
      status = U_ZERO_ERROR;
    }

    result.remove();
    int64_t ll = 12;
    test->format(ll, result);
    if (result != "12.00"){
        errln("format int64_t error");
    }

    delete test;  
  }
}

class StubNumberForamt :public NumberFormat{
public:
    StubNumberForamt(){};
    virtual UnicodeString& format(double ,UnicodeString& appendTo,FieldPosition& ) const {
        return appendTo;
    }
    virtual UnicodeString& format(int32_t ,UnicodeString& appendTo,FieldPosition& ) const {
        return appendTo.append((UChar)0x0033);
    }
    virtual UnicodeString& format(int64_t number,UnicodeString& appendTo,FieldPosition& pos) const {
        return NumberFormat::format(number, appendTo, pos);
    }
    virtual UnicodeString& format(const Formattable& , UnicodeString& appendTo, FieldPosition& , UErrorCode& ) const {
        return appendTo;
    }
    virtual void parse(const UnicodeString& ,
                    Formattable& ,
                    ParsePosition& ) const {}
    virtual void parse( const UnicodeString& ,
                        Formattable& ,
                        UErrorCode& ) const {}
    virtual UClassID getDynamicClassID(void) const {
        static char classID = 0;
        return (UClassID)&classID; 
    }
    virtual Format* clone() const {return NULL;}
};

void 
NumberFormatTest::TestCoverage(void){
    StubNumberForamt stub;
    UnicodeString agent("agent");
    FieldPosition pos;
    int64_t num = 4;
    if (stub.format(num, agent, pos) != UnicodeString("agent3")){
        errln("NumberFormat::format(int64, UnicodString&, FieldPosition&) should delegate to (int32, ,)");
    };
}

// Test various patterns
void
NumberFormatTest::TestPatterns(void)
{
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols sym(Locale::getUS(), status);
    if (U_FAILURE(status)) { errln("FAIL: Could not construct DecimalFormatSymbols"); return; }

    const char* pat[]    = { "#.#", "#.", ".#", "#" };
    int32_t pat_length = (int32_t)(sizeof(pat) / sizeof(pat[0]));
    const char* newpat[] = { "#0.#", "#0.", "#.0", "#" };
    const char* num[]    = { "0",   "0.", ".0", "0" };
    for (int32_t i=0; i<pat_length; ++i)
    {
        status = U_ZERO_ERROR;
        DecimalFormat fmt(pat[i], sym, status);
        if (U_FAILURE(status)) { errln((UnicodeString)"FAIL: DecimalFormat constructor failed for " + pat[i]); continue; }
        UnicodeString newp; fmt.toPattern(newp);
        if (!(newp == newpat[i]))
            errln((UnicodeString)"FAIL: Pattern " + pat[i] + " should transmute to " + newpat[i] +
                  "; " + newp + " seen instead");

        UnicodeString s; (*(NumberFormat*)&fmt).format((int32_t)0, s);
        if (!(s == num[i]))
        {
            errln((UnicodeString)"FAIL: Pattern " + pat[i] + " should format zero as " + num[i] +
                  "; " + s + " seen instead");
            logln((UnicodeString)"Min integer digits = " + fmt.getMinimumIntegerDigits());
        }
    }
}

/*
icu_2_4::DigitList::operator== 0 0 2 icuuc24d.dll digitlst.cpp Doug  
icu_2_4::DigitList::append 0 0 4 icuin24d.dll digitlst.h Doug  
icu_2_4::DigitList::operator!= 0 0 1 icuuc24d.dll digitlst.h Doug 
*/
/*
void 
NumberFormatTest::TestDigitList(void)
{
  // API coverage for DigitList
  DigitList list1;
  list1.append('1');
  list1.fDecimalAt = 1;
  DigitList list2;
  list2.set((int32_t)1);
  if (list1 != list2) {
    errln("digitlist append, operator!= or set failed ");
  }
  if (!(list1 == list2)) {
    errln("digitlist append, operator== or set failed ");
  }
}
*/

// -------------------------------------

// Test exponential pattern
void
NumberFormatTest::TestExponential(void)
{
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols sym(Locale::getUS(), status);
    if (U_FAILURE(status)) { errln("FAIL: Bad status returned by DecimalFormatSymbols ct"); return; }
    const char* pat[] = { "0.####E0", "00.000E00", "##0.######E000", "0.###E0;[0.###E0]"  };
    int32_t pat_length = (int32_t)(sizeof(pat) / sizeof(pat[0]));

// The following #if statements allow this test to be built and run on
// platforms that do not have standard IEEE numerics.  For example,
// S/390 doubles have an exponent range of -78 to +75.  For the
// following #if statements to work, float.h must define
// DBL_MAX_10_EXP to be a compile-time constant.

// This section may be expanded as needed.

#if DBL_MAX_10_EXP > 300
    double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
    int32_t val_length = (int32_t)(sizeof(val) / sizeof(val[0]));
    const char* valFormat[] =
    {
        // 0.####E0
        "1.234E-2", "1.2346E8", "1.23E300", "-3.1416E-271",
        // 00.000E00
        "12.340E-03", "12.346E07", "12.300E299", "-31.416E-272",
        // ##0.######E000
        "12.34E-003", "123.4568E006", "1.23E300", "-314.1593E-273",
        // 0.###E0;[0.###E0]
        "1.234E-2", "1.235E8", "1.23E300", "[3.142E-271]"
    };
    double valParse[] =
    {
        0.01234, 123460000, 1.23E300, -3.1416E-271,
        0.01234, 123460000, 1.23E300, -3.1416E-271,
        0.01234, 123456800, 1.23E300, -3.141593E-271,
        0.01234, 123500000, 1.23E300, -3.142E-271,
    };
#elif DBL_MAX_10_EXP > 70
    double val[] = { 0.01234, 123456789, 1.23e70, -3.141592653e-71 };
    int32_t val_length = sizeof(val) / sizeof(val[0]);
    char* valFormat[] =
    {
        // 0.####E0
        "1.234E-2", "1.2346E8", "1.23E70", "-3.1416E-71",
        // 00.000E00
        "12.340E-03", "12.346E07", "12.300E69", "-31.416E-72",
        // ##0.######E000
        "12.34E-003", "123.4568E006", "12.3E069", "-31.41593E-072",
        // 0.###E0;[0.###E0]
        "1.234E-2", "1.235E8", "1.23E70", "[3.142E-71]"
    };
    double valParse[] =
    {
        0.01234, 123460000, 1.23E70, -3.1416E-71,
        0.01234, 123460000, 1.23E70, -3.1416E-71,
        0.01234, 123456800, 1.23E70, -3.141593E-71,
        0.01234, 123500000, 1.23E70, -3.142E-71,
    };
#else
    // Don't test double conversion
    double* val = 0;
    int32_t val_length = 0;
    char** valFormat = 0;
    double* valParse = 0;
    logln("Warning: Skipping double conversion tests");
#endif

    int32_t lval[] = { 0, -1, 1, 123456789 };
    int32_t lval_length = (int32_t)(sizeof(lval) / sizeof(lval[0]));
    const char* lvalFormat[] =
    {
        // 0.####E0
        "0E0", "-1E0", "1E0", "1.2346E8",
        // 00.000E00
        "00.000E00", "-10.000E-01", "10.000E-01", "12.346E07",
        // ##0.######E000
        "0E000", "-1E000", "1E000", "123.4568E006",
        // 0.###E0;[0.###E0]
        "0E0", "[1E0]", "1E0", "1.235E8"
    };
    int32_t lvalParse[] =
    {
        0, -1, 1, 123460000,
        0, -1, 1, 123460000,
        0, -1, 1, 123456800,
        0, -1, 1, 123500000,
    };
    int32_t ival = 0, ilval = 0;
    for (int32_t p=0; p<pat_length; ++p)
    {
        DecimalFormat fmt(pat[p], sym, status);
        if (U_FAILURE(status)) { errln("FAIL: Bad status returned by DecimalFormat ct"); continue; }
        UnicodeString pattern;
        logln((UnicodeString)"Pattern \"" + pat[p] + "\" -toPattern-> \"" +
          fmt.toPattern(pattern) + "\"");
        int32_t v;
        for (v=0; v<val_length; ++v)
        {
            UnicodeString s; (*(NumberFormat*)&fmt).format(val[v], s);
            logln((UnicodeString)" " + val[v] + " -format-> " + s);
            if (s != valFormat[v+ival])
                errln((UnicodeString)"FAIL: Expected " + valFormat[v+ival]);

            ParsePosition pos(0);
            Formattable af;
            fmt.parse(s, af, pos);
            double a;
            UBool useEpsilon = FALSE;
            if (af.getType() == Formattable::kLong)
                a = af.getLong();
            else if (af.getType() == Formattable::kDouble) {
                a = af.getDouble();
#if defined(OS390) || defined(OS400)
                // S/390 will show a failure like this:
                //| -3.141592652999999e-271 -format-> -3.1416E-271
                //|                          -parse-> -3.1416e-271
                //| FAIL: Expected -3.141599999999999e-271
                // To compensate, we use an epsilon-based equality
                // test on S/390 only.  We don't want to do this in
                // general because it's less exacting.
                useEpsilon = TRUE;
#endif
            }
            else {
                errln((UnicodeString)"FAIL: Non-numeric Formattable returned");
                continue;
            }
            if (pos.getIndex() == s.length())
            {
                logln((UnicodeString)"  -parse-> " + a);
                // Use epsilon comparison as necessary
                if ((useEpsilon &&
                    (uprv_fabs(a - valParse[v+ival]) / a > (2*DBL_EPSILON))) ||
                    (!useEpsilon && a != valParse[v+ival]))
                {
                    errln((UnicodeString)"FAIL: Expected " + valParse[v+ival]);
                }
            }
            else {
                errln((UnicodeString)"FAIL: Partial parse (" + pos.getIndex() + " chars) -> " + a);
                errln((UnicodeString)"  should be (" + s.length() + " chars) -> " + valParse[v+ival]);
            }
        }
        for (v=0; v<lval_length; ++v)
        {
            UnicodeString s;
            (*(NumberFormat*)&fmt).format(lval[v], s);
            logln((UnicodeString)" " + lval[v] + "L -format-> " + s);
            if (s != lvalFormat[v+ilval])
                errln((UnicodeString)"ERROR: Expected " + lvalFormat[v+ilval] + " Got: " + s);

            ParsePosition pos(0);
            Formattable af;
            fmt.parse(s, af, pos);
            if (af.getType() == Formattable::kLong ||
                af.getType() == Formattable::kInt64) {
                UErrorCode status = U_ZERO_ERROR;
                int32_t a = af.getLong(status);
                if (pos.getIndex() == s.length())
                {
                    logln((UnicodeString)"  -parse-> " + a);
                    if (a != lvalParse[v+ilval])
                        errln((UnicodeString)"FAIL: Expected " + lvalParse[v+ilval]);
                }
                else
                    errln((UnicodeString)"FAIL: Partial parse (" + pos.getIndex() + " chars) -> " + a);
            }
            else
                errln((UnicodeString)"FAIL: Non-long Formattable returned for " + s
                    + " Double: " + af.getDouble()
                    + ", Long: " + af.getLong());
        }
        ival += val_length;
        ilval += lval_length;
    }
}

void
NumberFormatTest::TestScientific2() {
    // jb 2552
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat* fmt = (DecimalFormat*)NumberFormat::createCurrencyInstance("en_US", status);
    if (U_SUCCESS(status)) {
        double num = 12.34;
        expect(*fmt, num, "$12.34");
        fmt->setScientificNotation(TRUE);
        expect(*fmt, num, "$1.23E1");
        fmt->setScientificNotation(FALSE);
        expect(*fmt, num, "$12.34");
    }
    delete fmt;
}

void 
NumberFormatTest::TestScientificGrouping() {
    // jb 2552
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat fmt("##0.00E0",status);
    if (U_SUCCESS(status)) {
        expect(fmt, .01234, "12.3E-3");
        expect(fmt, .1234, "123E-3");
        expect(fmt, 1.234, "1.23E0");
        expect(fmt, 12.34, "12.3E0");
        expect(fmt, 123.4, "123E0");
        expect(fmt, 1234., "1.23E3");
    }
}

/*static void setFromString(DigitList& dl, const char* str) {
    char c;
    UBool decimalSet = FALSE;
    dl.clear();
    while ((c = *str++)) {
        if (c == '-') {
            dl.fIsPositive = FALSE;
        } else if (c == '+') {
            dl.fIsPositive = TRUE;
        } else if (c == '.') {
            dl.fDecimalAt = dl.fCount;
            decimalSet = TRUE;
        } else {
            dl.append(c);
        }
    }
    if (!decimalSet) {
        dl.fDecimalAt = dl.fCount;
    }
}*/

void
NumberFormatTest::TestInt64() {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat fmt("#.#E0",status);
    fmt.setMaximumFractionDigits(20);
    if (U_SUCCESS(status)) {
        expect(fmt, (Formattable)(int64_t)0, "0E0");
        expect(fmt, (Formattable)(int64_t)-1, "-1E0");
        expect(fmt, (Formattable)(int64_t)1, "1E0");
        expect(fmt, (Formattable)(int64_t)2147483647, "2.147483647E9");
        expect(fmt, (Formattable)((int64_t)-2147483647-1), "-2.147483648E9");
        expect(fmt, (Formattable)(int64_t)U_INT64_MAX, "9.223372036854775807E18");
        expect(fmt, (Formattable)(int64_t)U_INT64_MIN, "-9.223372036854775808E18");
    }

    // also test digitlist
/*    int64_t int64max = U_INT64_MAX;
    int64_t int64min = U_INT64_MIN;
    const char* int64maxstr = "9223372036854775807";
    const char* int64minstr = "-9223372036854775808";
    UnicodeString fail("fail: ");

    // test max int64 value
    DigitList dl;
    setFromString(dl, int64maxstr);
    {
        if (!dl.fitsIntoInt64(FALSE)) {
            errln(fail + int64maxstr + " didn't fit");
        }
        int64_t int64Value = dl.getInt64();
        if (int64Value != int64max) {
            errln(fail + int64maxstr);
        }
        dl.set(int64Value);
        int64Value = dl.getInt64();
        if (int64Value != int64max) {
            errln(fail + int64maxstr);
        }
    }
    // test negative of max int64 value (1 shy of min int64 value)
    dl.fIsPositive = FALSE;
    {
        if (!dl.fitsIntoInt64(FALSE)) {
            errln(fail + "-" + int64maxstr + " didn't fit");
        }
        int64_t int64Value = dl.getInt64();
        if (int64Value != -int64max) {
            errln(fail + "-" + int64maxstr);
        }
        dl.set(int64Value);
        int64Value = dl.getInt64();
        if (int64Value != -int64max) {
            errln(fail + "-" + int64maxstr);
        }
    }
    // test min int64 value
    setFromString(dl, int64minstr);
    {
        if (!dl.fitsIntoInt64(FALSE)) {
            errln(fail + "-" + int64minstr + " didn't fit");
        }
        int64_t int64Value = dl.getInt64();
        if (int64Value != int64min) {
            errln(fail + int64minstr);
        }
        dl.set(int64Value);
        int64Value = dl.getInt64();
        if (int64Value != int64min) {
            errln(fail + int64minstr);
        }
    }
    // test negative of min int 64 value (1 more than max int64 value)
    dl.fIsPositive = TRUE; // won't fit
    {
        if (dl.fitsIntoInt64(FALSE)) {
            errln(fail + "-(" + int64minstr + ") didn't fit");
        }
    }*/
}

// -------------------------------------

// Test the handling of quotes
void
NumberFormatTest::TestQuotes(void)
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString *pat;
    DecimalFormatSymbols *sym = new DecimalFormatSymbols(Locale::getUS(), status);
    pat = new UnicodeString("a'fo''o'b#");
    DecimalFormat *fmt = new DecimalFormat(*pat, *sym, status);
    UnicodeString s; 
    ((NumberFormat*)fmt)->format((int32_t)123, s);
    logln((UnicodeString)"Pattern \"" + *pat + "\"");
    logln((UnicodeString)" Format 123 -> " + escape(s));
    if (!(s=="afo'ob123")) 
        errln((UnicodeString)"FAIL: Expected afo'ob123");
    
    s.truncate(0);
    delete fmt;
    delete pat;

    pat = new UnicodeString("a''b#");
    fmt = new DecimalFormat(*pat, *sym, status);
    ((NumberFormat*)fmt)->format((int32_t)123, s);
    logln((UnicodeString)"Pattern \"" + *pat + "\"");
    logln((UnicodeString)" Format 123 -> " + escape(s));
    if (!(s=="a'b123")) 
        errln((UnicodeString)"FAIL: Expected a'b123");
    delete fmt;
    delete pat;
    delete sym;
}

/**
 * Test the handling of the currency symbol in patterns.
 */
void
NumberFormatTest::TestCurrencySign(void)
{
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols* sym = new DecimalFormatSymbols(Locale::getUS(), status);
    UnicodeString pat;
    UChar currency = 0x00A4;
    // "\xA4#,##0.00;-\xA4#,##0.00"
    pat.append(currency).append("#,##0.00;-").
        append(currency).append("#,##0.00");
    DecimalFormat *fmt = new DecimalFormat(pat, *sym, status);
    UnicodeString s; ((NumberFormat*)fmt)->format(1234.56, s);
    pat.truncate(0);
    logln((UnicodeString)"Pattern \"" + fmt->toPattern(pat) + "\"");
    logln((UnicodeString)" Format " + 1234.56 + " -> " + escape(s));
    if (s != "$1,234.56") errln((UnicodeString)"FAIL: Expected $1,234.56");
    s.truncate(0);
    ((NumberFormat*)fmt)->format(- 1234.56, s);
    logln((UnicodeString)" Format " + (-1234.56) + " -> " + escape(s));
    if (s != "-$1,234.56") errln((UnicodeString)"FAIL: Expected -$1,234.56");
    delete fmt;
    pat.truncate(0);
    // "\xA4\xA4 #,##0.00;\xA4\xA4 -#,##0.00"
    pat.append(currency).append(currency).
        append(" #,##0.00;").
        append(currency).append(currency).
        append(" -#,##0.00");
    fmt = new DecimalFormat(pat, *sym, status);
    s.truncate(0);
    ((NumberFormat*)fmt)->format(1234.56, s);
    logln((UnicodeString)"Pattern \"" + fmt->toPattern(pat) + "\"");
    logln((UnicodeString)" Format " + 1234.56 + " -> " + escape(s));
    if (s != "USD 1,234.56") errln((UnicodeString)"FAIL: Expected USD 1,234.56");
    s.truncate(0);
    ((NumberFormat*)fmt)->format(-1234.56, s);
    logln((UnicodeString)" Format " + (-1234.56) + " -> " + escape(s));
    if (s != "USD -1,234.56") errln((UnicodeString)"FAIL: Expected USD -1,234.56");
    delete fmt;
    delete sym;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: Status " + (int32_t)status);
}
 
// -------------------------------------

static UChar toHexString(int32_t i) { return (UChar)(i + (i < 10 ? 0x30 : (0x41 - 10))); }

UnicodeString&
NumberFormatTest::escape(UnicodeString& s)
{
    UnicodeString buf;
    for (int32_t i=0; i<s.length(); ++i)
    {
        UChar c = s[(int32_t)i];
        if (c <= (UChar)0x7F) buf += c;
        else {
            buf += (UChar)0x5c; buf += (UChar)0x55;
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return (s = buf);
}

 
// -------------------------------------
static const char* testCases[][2]= {
     /* locale ID */  /* expected */
    {"ca_ES_PREEURO", "1.150\\u00A0\\u20A7" },
    {"de_LU_PREEURO", "1,150\\u00A0F" },
    {"el_GR_PREEURO", "1.150,50\\u00A0\\u0394\\u03C1\\u03C7" },
    {"en_BE_PREEURO", "1.150,50\\u00A0BF" },
    {"es_ES_PREEURO", "\\u20A7\\u00A01.150" },
    {"eu_ES_PREEURO", "1.150\\u00A0\\u20A7" }, 
    {"gl_ES_PREEURO", "1.150\\u00A0\\u20A7" },
    {"it_IT_PREEURO", "\\u20A4\\u00A01.150" },
    {"pt_PT_PREEURO", "1,150$50\\u00A0Esc."},
    {"en_US@currency=JPY", "\\u00A51,150"}
};
/**
 * Test localized currency patterns.
 */
void
NumberFormatTest::TestCurrency(void)
{
    UErrorCode status = U_ZERO_ERROR;
    NumberFormat* currencyFmt = NumberFormat::createCurrencyInstance(Locale::getCanadaFrench(), status);
    if (U_FAILURE(status)) {
        dataerrln("Error calling NumberFormat::createCurrencyInstance()");
        return;
    }

    UnicodeString s; currencyFmt->format(1.50, s);
    logln((UnicodeString)"Un pauvre ici a..........." + s);
    if (!(s==CharsToUnicodeString("1,50\\u00A0$")))
        errln((UnicodeString)"FAIL: Expected 1,50<nbsp>$");
    delete currencyFmt;
    s.truncate(0);
    char loc[256]={0};
    int len = uloc_canonicalize("de_DE_PREEURO", loc, 256, &status);
    currencyFmt = NumberFormat::createCurrencyInstance(Locale(loc),status);
    currencyFmt->format(1.50, s);
    logln((UnicodeString)"Un pauvre en Allemagne a.." + s);
    if (!(s==CharsToUnicodeString("1,50\\u00A0DM")))
        errln((UnicodeString)"FAIL: Expected 1,50<nbsp>DM");
    delete currencyFmt;
    s.truncate(0);
    len = uloc_canonicalize("fr_FR_PREEURO", loc, 256, &status);
    currencyFmt = NumberFormat::createCurrencyInstance(Locale(loc), status);
    currencyFmt->format(1.50, s);
    logln((UnicodeString)"Un pauvre en France a....." + s);
    if (!(s==CharsToUnicodeString("1,50\\u00A0F")))
        errln((UnicodeString)"FAIL: Expected 1,50<nbsp>F");
    delete currencyFmt;
    if (U_FAILURE(status))
        errln((UnicodeString)"FAIL: Status " + (int32_t)status);
    
    for(int i=0; i < (int)(sizeof(testCases)/sizeof(testCases[i])); i++){
        status = U_ZERO_ERROR;
        const char *localeID = testCases[i][0];
        UnicodeString expected(testCases[i][1], -1, US_INV);
        expected = expected.unescape();
        s.truncate(0);
        char loc[256]={0};
        uloc_canonicalize(localeID, loc, 256, &status);
        currencyFmt = NumberFormat::createCurrencyInstance(Locale(loc), status);
        if(U_FAILURE(status)){
            errln("Could not create currency formatter for locale %s",localeID);
            continue;
        }
        currencyFmt->format(1150.50, s);
        if(s!=expected){
            errln(UnicodeString("FAIL: Expected: ")+expected 
                    + UnicodeString(" Got: ") + s 
                    + UnicodeString( " for locale: ")+ UnicodeString(localeID) );
        }
        if (U_FAILURE(status)){
            errln((UnicodeString)"FAIL: Status " + (int32_t)status);
        }
        delete currencyFmt;
    }
}
 
// -------------------------------------

/**
 * Test the Currency object handling, new as of ICU 2.2.
 */
void NumberFormatTest::TestCurrencyObject() {
    UErrorCode ec = U_ZERO_ERROR;
    NumberFormat* fmt = 
        NumberFormat::createCurrencyInstance(Locale::getUS(), ec);

    if (U_FAILURE(ec)) {
        errln("FAIL: getCurrencyInstance(US)");
        delete fmt;
        return;
    }

    Locale null("", "", "");
        
    expectCurrency(*fmt, null, 1234.56, "$1,234.56");

    expectCurrency(*fmt, Locale::getFrance(),
                   1234.56, CharsToUnicodeString("\\u20AC1,234.56")); // Euro

    expectCurrency(*fmt, Locale::getJapan(),
                   1234.56, CharsToUnicodeString("\\u00A51,235")); // Yen

    expectCurrency(*fmt, Locale("fr", "CH", ""),
                   1234.56, "Fr.1,234.55"); // 0.05 rounding

    expectCurrency(*fmt, Locale::getUS(),
                   1234.56, "$1,234.56");

    delete fmt;
    fmt = NumberFormat::createCurrencyInstance(Locale::getFrance(), ec);

    if (U_FAILURE(ec)) {
        errln("FAIL: getCurrencyInstance(FRANCE)");
        delete fmt;
        return;
    }
        
    expectCurrency(*fmt, null, 1234.56, CharsToUnicodeString("1 234,56 \\u20AC"));

    expectCurrency(*fmt, Locale::getJapan(),
                   1234.56, CharsToUnicodeString("1 235 \\u00A5JP")); // Yen

    expectCurrency(*fmt, Locale("fr", "CH", ""),
                   1234.56, "1 234,55 sFr."); // 0.05 rounding

    expectCurrency(*fmt, Locale::getUS(),
                   1234.56, "1 234,56 $US");

    expectCurrency(*fmt, Locale::getFrance(),
                   1234.56, CharsToUnicodeString("1 234,56 \\u20AC")); // Euro

    delete fmt;
}
    
// -------------------------------------

/**
 * Do rudimentary testing of parsing.
 */
void
NumberFormatTest::TestParse(void)
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString arg("0");
    DecimalFormat* format = new DecimalFormat("00", status);
    //try {
        Formattable n; format->parse(arg, n, status);
        logln((UnicodeString)"parse(" + arg + ") = " + n.getLong());
        if (n.getType() != Formattable::kLong ||
            n.getLong() != 0) errln((UnicodeString)"FAIL: Expected 0");
    delete format;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: Status " + (int32_t)status);
    //}
    //catch(Exception e) {
    //    errln((UnicodeString)"Exception caught: " + e);
    //}
}
 
// -------------------------------------

/**
 * Test proper rounding by the format method.
 */
void
NumberFormatTest::TestRounding487(void)
{
    UErrorCode status = U_ZERO_ERROR;
    NumberFormat *nf = NumberFormat::createInstance(status);
    if (U_FAILURE(status)) {
        dataerrln("Error calling NumberFormat::createInstance()");
        return;
    }

    roundingTest(*nf, 0.00159999, 4, "0.0016");
    roundingTest(*nf, 0.00995, 4, "0.01");

    roundingTest(*nf, 12.3995, 3, "12.4");

    roundingTest(*nf, 12.4999, 0, "12");
    roundingTest(*nf, - 19.5, 0, "-20");
    delete nf;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: Status " + (int32_t)status);
}

/**
 * Test the functioning of the secondary grouping value.
 */
void NumberFormatTest::TestSecondaryGrouping(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols ct");

    DecimalFormat f("#,##,###", US, status);
    CHECK(status, "DecimalFormat ct");

    expect2(f, (int32_t)123456789L, "12,34,56,789");
    expectPat(f, "#,##,###");
    f.applyPattern("#,###", status);
    CHECK(status, "applyPattern");

    f.setSecondaryGroupingSize(4);
    expect2(f, (int32_t)123456789L, "12,3456,789");
    expectPat(f, "#,####,###");
    NumberFormat *g = NumberFormat::createInstance(Locale("hi", "IN"), status);
    CHECK(status, "createInstance(hi_IN)");

    UnicodeString out;
    int32_t l = (int32_t)1876543210L;
    g->format(l, out);
    delete g;
    // expect "1,87,65,43,210", but with Hindi digits
    //         01234567890123
    UBool ok = TRUE;
    if (out.length() != 14) {
        ok = FALSE;
    } else {
        for (int32_t i=0; i<out.length(); ++i) {
            UBool expectGroup = FALSE;
            switch (i) {
            case 1:
            case 4:
            case 7:
            case 10:
                expectGroup = TRUE;
                break;
            }
            // Later -- fix this to get the actual grouping
            // character from the resource bundle.
            UBool isGroup = (out.charAt(i) == 0x002C);
            if (isGroup != expectGroup) {
                ok = FALSE;
                break;
            }
        }
    }
    if (!ok) {
        errln((UnicodeString)"FAIL  Expected " + l +
              " x hi_IN -> \"1,87,65,43,210\" (with Hindi digits), got \"" +
              escape(out) + "\"");
    } else {
        logln((UnicodeString)"Ok    " + l +
              " x hi_IN -> \"" +
              escape(out) + "\"");
    }
}

void NumberFormatTest::TestWhiteSpaceParsing(void) {
    UErrorCode ec = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), ec);
    DecimalFormat fmt("a  b#0c  ", US, ec);
    if (U_FAILURE(ec)) {
        errln("FAIL: Constructor");
        return;
    }
    int32_t n = 1234;
    expect(fmt, "a b1234c ", n);
    expect(fmt, "a   b1234c   ", n);
}

/**
 * Test currencies whose display name is a ChoiceFormat.
 */
void NumberFormatTest::TestComplexCurrency() {
    UErrorCode ec = U_ZERO_ERROR;
    Locale loc("kn", "IN", "");
    NumberFormat* fmt = NumberFormat::createCurrencyInstance(loc, ec);
    if (U_SUCCESS(ec)) {
        expect2(*fmt, 1.0, CharsToUnicodeString("Re.\\u00A01.00"));
        // Use .00392625 because that's 2^-8.  Any value less than 0.005 is fine.
        expect(*fmt, 1.00390625, CharsToUnicodeString("Re.\\u00A01.00")); // tricky
        expect2(*fmt, 12345678.0, CharsToUnicodeString("Rs.\\u00A01,23,45,678.00"));
        expect2(*fmt, 0.5, CharsToUnicodeString("Rs.\\u00A00.50"));
        expect2(*fmt, -1.0, CharsToUnicodeString("-Re.\\u00A01.00"));
        expect2(*fmt, -10.0, CharsToUnicodeString("-Rs.\\u00A010.00"));
    } else {
        errln("FAIL: getCurrencyInstance(kn_IN)");
    }
    delete fmt;
}
    
// -------------------------------------
 
void
NumberFormatTest::roundingTest(NumberFormat& nf, double x, int32_t maxFractionDigits, const char* expected)
{
    nf.setMaximumFractionDigits(maxFractionDigits);
    UnicodeString out; nf.format(x, out);
    logln((UnicodeString)"" + x + " formats with " + maxFractionDigits + " fractional digits to " + out);
    if (!(out==expected)) errln((UnicodeString)"FAIL: Expected " + expected);
}

/**
 * Upgrade to alphaWorks
 */
void NumberFormatTest::TestExponent(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");
    DecimalFormat fmt1(UnicodeString("0.###E0"), US, status);
    CHECK(status, "DecimalFormat(0.###E0)");
    DecimalFormat fmt2(UnicodeString("0.###E+0"), US, status);
    CHECK(status, "DecimalFormat(0.###E+0)");
    int32_t n = 1234;
    expect2(fmt1, n, "1.234E3");
    expect2(fmt2, n, "1.234E+3");
    expect(fmt1, "1.234E+3", n); // Either format should parse "E+3"
}

/**
 * Upgrade to alphaWorks
 */
void NumberFormatTest::TestScientific(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");

    // Test pattern round-trip
    const char* PAT[] = { "#E0", "0.####E0", "00.000E00", "##0.####E000",
                          "0.###E0;[0.###E0]" };
    int32_t PAT_length = (int32_t)(sizeof(PAT) / sizeof(PAT[0]));
    int32_t DIGITS[] = {
        // min int, max int, min frac, max frac
        0, 1, 0, 0, // "#E0"
        1, 1, 0, 4, // "0.####E0"
        2, 2, 3, 3, // "00.000E00"
        1, 3, 0, 4, // "##0.####E000"
        1, 1, 0, 3, // "0.###E0;[0.###E0]"
    };
    for (int32_t i=0; i<PAT_length; ++i) {
        UnicodeString pat(PAT[i]);
        DecimalFormat df(pat, US, status);
        CHECK(status, "DecimalFormat constructor");
        UnicodeString pat2;
        df.toPattern(pat2);
        if (pat == pat2) {
            logln(UnicodeString("Ok   Pattern rt \"") +
                  pat + "\" -> \"" +
                  pat2 + "\"");
        } else {
            errln(UnicodeString("FAIL Pattern rt \"") +
                  pat + "\" -> \"" +
                  pat2 + "\"");
        }
        // Make sure digit counts match what we expect
        if (df.getMinimumIntegerDigits() != DIGITS[4*i] ||
            df.getMaximumIntegerDigits() != DIGITS[4*i+1] ||
            df.getMinimumFractionDigits() != DIGITS[4*i+2] ||
            df.getMaximumFractionDigits() != DIGITS[4*i+3]) {
            errln(UnicodeString("FAIL \"" + pat +
                                "\" min/max int; min/max frac = ") +
                  df.getMinimumIntegerDigits() + "/" +
                  df.getMaximumIntegerDigits() + ";" +
                  df.getMinimumFractionDigits() + "/" +
                  df.getMaximumFractionDigits() + ", expect " +
                  DIGITS[4*i] + "/" +
                  DIGITS[4*i+1] + ";" +
                  DIGITS[4*i+2] + "/" +
                  DIGITS[4*i+3]);
        }
    }


    // Test the constructor for default locale. We have to
    // manually set the default locale, as there is no 
    // guarantee that the default locale has the same 
    // scientific format.
    Locale def = Locale::getDefault();
    Locale::setDefault(Locale::getUS(), status);
    expect2(NumberFormat::createScientificInstance(status),
           12345.678901,
           "1.2345678901E4", status);
    Locale::setDefault(def, status);

    expect2(new DecimalFormat("#E0", US, status),
           12345.0,
           "1.2345E4", status);
    expect(new DecimalFormat("0E0", US, status),
           12345.0,
           "1E4", status);
    expect2(NumberFormat::createScientificInstance(Locale::getUS(), status),
           12345.678901,
           "1.2345678901E4", status);
    expect(new DecimalFormat("##0.###E0", US, status),
           12345.0,
           "12.34E3", status);
    expect(new DecimalFormat("##0.###E0", US, status),
           12345.00001,
           "12.35E3", status);
    expect2(new DecimalFormat("##0.####E0", US, status),
           (int32_t) 12345,
           "12.345E3", status);
    expect2(NumberFormat::createScientificInstance(Locale::getFrance(), status),
           12345.678901,
           "1,2345678901E4", status);
    expect(new DecimalFormat("##0.####E0", US, status),
           789.12345e-9,
           "789.12E-9", status);
    expect2(new DecimalFormat("##0.####E0", US, status),
           780.e-9,
           "780E-9", status);
    expect(new DecimalFormat(".###E0", US, status),
           45678.0,
           ".457E5", status);
    expect2(new DecimalFormat(".###E0", US, status),
           (int32_t) 0,
           ".0E0", status);
    /*
    expect(new DecimalFormat[] { new DecimalFormat("#E0", US),
                                 new DecimalFormat("##E0", US),
                                 new DecimalFormat("####E0", US),
                                 new DecimalFormat("0E0", US),    
                                 new DecimalFormat("00E0", US),   
                                 new DecimalFormat("000E0", US), 
                               },
           new Long(45678000),
           new String[] { "4.5678E7",
                          "45.678E6",
                          "4567.8E4",
                          "5E7",
                          "46E6",  
                          "457E5",
                        }
           );
    !
    ! Unroll this test into individual tests below...
    !
    */
    expect2(new DecimalFormat("#E0", US, status),
           (int32_t) 45678000, "4.5678E7", status);
    expect2(new DecimalFormat("##E0", US, status),
           (int32_t) 45678000, "45.678E6", status);
    expect2(new DecimalFormat("####E0", US, status),
           (int32_t) 45678000, "4567.8E4", status);
    expect(new DecimalFormat("0E0", US, status),
           (int32_t) 45678000, "5E7", status);
    expect(new DecimalFormat("00E0", US, status),
           (int32_t) 45678000, "46E6", status);
    expect(new DecimalFormat("000E0", US, status),
           (int32_t) 45678000, "457E5", status);
    /*
    expect(new DecimalFormat("###E0", US, status),
           new Object[] { new Double(0.0000123), "12.3E-6",
                          new Double(0.000123), "123E-6",
                          new Double(0.00123), "1.23E-3",
                          new Double(0.0123), "12.3E-3",
                          new Double(0.123), "123E-3",
                          new Double(1.23), "1.23E0",
                          new Double(12.3), "12.3E0",
                          new Double(123), "123E0",
                          new Double(1230), "1.23E3",
                         });
    !
    ! Unroll this test into individual tests below...
    !
    */
    expect2(new DecimalFormat("###E0", US, status),
           0.0000123, "12.3E-6", status);
    expect2(new DecimalFormat("###E0", US, status),
           0.000123, "123E-6", status);
    expect2(new DecimalFormat("###E0", US, status),
           0.00123, "1.23E-3", status);
    expect2(new DecimalFormat("###E0", US, status),
           0.0123, "12.3E-3", status);
    expect2(new DecimalFormat("###E0", US, status),
           0.123, "123E-3", status);
    expect2(new DecimalFormat("###E0", US, status),
           1.23, "1.23E0", status);
    expect2(new DecimalFormat("###E0", US, status),
           12.3, "12.3E0", status);
    expect2(new DecimalFormat("###E0", US, status),
           123.0, "123E0", status);
    expect2(new DecimalFormat("###E0", US, status),
           1230.0, "1.23E3", status);
    /*
    expect(new DecimalFormat("0.#E+00", US, status),
           new Object[] { new Double(0.00012), "1.2E-04",
                          new Long(12000),     "1.2E+04",
                         });
    !
    ! Unroll this test into individual tests below...
    !
    */
    expect2(new DecimalFormat("0.#E+00", US, status),
           0.00012, "1.2E-04", status);
    expect2(new DecimalFormat("0.#E+00", US, status),
           (int32_t) 12000, "1.2E+04", status);
}

/**
 * Upgrade to alphaWorks
 */
void NumberFormatTest::TestPad(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");

    expect2(new DecimalFormat("*^##.##", US, status),
           int32_t(0), "^^^^0", status);
    expect2(new DecimalFormat("*^##.##", US, status),
           -1.3, "^-1.3", status);
    expect2(new DecimalFormat("##0.0####E0*_ 'g-m/s^2'", US, status),
           int32_t(0), "0.0E0______ g-m/s^2", status);
    expect(new DecimalFormat("##0.0####E0*_ 'g-m/s^2'", US, status),
           1.0/3, "333.333E-3_ g-m/s^2", status);
    expect2(new DecimalFormat("##0.0####*_ 'g-m/s^2'", US, status),
           int32_t(0), "0.0______ g-m/s^2", status);
    expect(new DecimalFormat("##0.0####*_ 'g-m/s^2'", US, status),
           1.0/3, "0.33333__ g-m/s^2", status);

    // Test padding before a sign
    const char *formatStr = "*x#,###,###,##0.0#;*x(###,###,##0.0#)";
    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(-10),  "xxxxxxxxxx(10.0)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(-1000),"xxxxxxx(1,000.0)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(-1000000),"xxx(1,000,000.0)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           -100.37,       "xxxxxxxx(100.37)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           -10456.37,     "xxxxx(10,456.37)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           -1120456.37,   "xx(1,120,456.37)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           -112045600.37, "(112,045,600.37)", status);
    expect2(new DecimalFormat(formatStr, US, status),
           -1252045600.37,"(1,252,045,600.37)", status);

    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(10),  "xxxxxxxxxxxx10.0", status);
    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(1000),"xxxxxxxxx1,000.0", status);
    expect2(new DecimalFormat(formatStr, US, status),
           int32_t(1000000),"xxxxx1,000,000.0", status);
    expect2(new DecimalFormat(formatStr, US, status),
           100.37,       "xxxxxxxxxx100.37", status);
    expect2(new DecimalFormat(formatStr, US, status),
           10456.37,     "xxxxxxx10,456.37", status);
    expect2(new DecimalFormat(formatStr, US, status),
           1120456.37,   "xxxx1,120,456.37", status);
    expect2(new DecimalFormat(formatStr, US, status),
           112045600.37, "xx112,045,600.37", status);
    expect2(new DecimalFormat(formatStr, US, status),
           10252045600.37,"10,252,045,600.37", status);


    // Test padding between a sign and a number
    const char *formatStr2 = "#,###,###,##0.0#*x;(###,###,##0.0#*x)";
    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(-10),  "(10.0xxxxxxxxxx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(-1000),"(1,000.0xxxxxxx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(-1000000),"(1,000,000.0xxx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           -100.37,       "(100.37xxxxxxxx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           -10456.37,     "(10,456.37xxxxx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           -1120456.37,   "(1,120,456.37xx)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           -112045600.37, "(112,045,600.37)", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           -1252045600.37,"(1,252,045,600.37)", status);

    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(10),  "10.0xxxxxxxxxxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(1000),"1,000.0xxxxxxxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           int32_t(1000000),"1,000,000.0xxxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           100.37,       "100.37xxxxxxxxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           10456.37,     "10,456.37xxxxxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           1120456.37,   "1,120,456.37xxxx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           112045600.37, "112,045,600.37xx", status);
    expect2(new DecimalFormat(formatStr2, US, status),
           10252045600.37,"10,252,045,600.37", status);

    //testing the setPadCharacter(UnicodeString) and getPadCharacterString()
    DecimalFormat fmt("#", US, status);
    CHECK(status, "DecimalFormat constructor");
    UnicodeString padString("P");
    fmt.setPadCharacter(padString);
    expectPad(fmt, "*P##.##", DecimalFormat::kPadBeforePrefix, 5, padString);
    fmt.setPadCharacter((UnicodeString)"^");
    expectPad(fmt, "*^#", DecimalFormat::kPadBeforePrefix, 1, (UnicodeString)"^");
    //commented untill implementation is complete
  /*  fmt.setPadCharacter((UnicodeString)"^^^");
    expectPad(fmt, "*^^^#", DecimalFormat::kPadBeforePrefix, 3, (UnicodeString)"^^^");
    padString.remove();
    padString.append((UChar)0x0061);
    padString.append((UChar)0x0302);
    fmt.setPadCharacter(padString);
    UChar patternChars[]={0x002a, 0x0061, 0x0302, 0x0061, 0x0302, 0x0023, 0x0000};
    UnicodeString pattern(patternChars);
    expectPad(fmt, pattern , DecimalFormat::kPadBeforePrefix, 4, padString);
 */

}

/**
 * Upgrade to alphaWorks
 */
void NumberFormatTest::TestPatterns2(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");

    DecimalFormat fmt("#", US, status);
    CHECK(status, "DecimalFormat constructor");
    
    UChar hat = 0x005E; /*^*/

    expectPad(fmt, "*^#", DecimalFormat::kPadBeforePrefix, 1, hat);
    expectPad(fmt, "$*^#", DecimalFormat::kPadAfterPrefix, 2, hat);
    expectPad(fmt, "#*^", DecimalFormat::kPadBeforeSuffix, 1, hat);
    expectPad(fmt, "#$*^", DecimalFormat::kPadAfterSuffix, 2, hat);
    expectPad(fmt, "$*^$#", ILLEGAL);
    expectPad(fmt, "#$*^$", ILLEGAL);
    expectPad(fmt, "'pre'#,##0*x'post'", DecimalFormat::kPadBeforeSuffix,
              12, (UChar)0x0078 /*x*/);
    expectPad(fmt, "''#0*x", DecimalFormat::kPadBeforeSuffix,
              3, (UChar)0x0078 /*x*/);
    expectPad(fmt, "'I''ll'*a###.##", DecimalFormat::kPadAfterPrefix,
              10, (UChar)0x0061 /*a*/);

    fmt.applyPattern("AA#,##0.00ZZ", status);
    CHECK(status, "applyPattern");
    fmt.setPadCharacter(hat);

    fmt.setFormatWidth(10);

    fmt.setPadPosition(DecimalFormat::kPadBeforePrefix);
    expectPat(fmt, "*^AA#,##0.00ZZ");

    fmt.setPadPosition(DecimalFormat::kPadBeforeSuffix);
    expectPat(fmt, "AA#,##0.00*^ZZ");

    fmt.setPadPosition(DecimalFormat::kPadAfterSuffix);
    expectPat(fmt, "AA#,##0.00ZZ*^");

    //            12  3456789012
    UnicodeString exp("AA*^#,##0.00ZZ", "");
    fmt.setFormatWidth(12);
    fmt.setPadPosition(DecimalFormat::kPadAfterPrefix);
    expectPat(fmt, exp);

    fmt.setFormatWidth(13);
    //              12  34567890123
    expectPat(fmt, "AA*^##,##0.00ZZ");

    fmt.setFormatWidth(14);
    //              12  345678901234
    expectPat(fmt, "AA*^###,##0.00ZZ");

    fmt.setFormatWidth(15);
    //              12  3456789012345
    expectPat(fmt, "AA*^####,##0.00ZZ"); // This is the interesting case

    fmt.setFormatWidth(16);
    //              12  34567890123456
    expectPat(fmt, "AA*^#,###,##0.00ZZ");
}

void NumberFormatTest::TestSurrogateSupport(void) {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols custom(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");

    custom.setSymbol(DecimalFormatSymbols::kDecimalSeparatorSymbol, "decimal");
    custom.setSymbol(DecimalFormatSymbols::kPlusSignSymbol, "plus");
    custom.setSymbol(DecimalFormatSymbols::kMinusSignSymbol, " minus ");
    custom.setSymbol(DecimalFormatSymbols::kExponentialSymbol, "exponent");

    UnicodeString patternStr("*\\U00010000##.##", "");
    patternStr = patternStr.unescape();
    UnicodeString expStr("\\U00010000\\U00010000\\U00010000\\U000100000", "");
    expStr = expStr.unescape();
    expect2(new DecimalFormat(patternStr, custom, status),
           int32_t(0), expStr, status);

    status = U_ZERO_ERROR;
    expect2(new DecimalFormat("*^##.##", custom, status),
           int32_t(0), "^^^^0", status);
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat("##.##", custom, status),
           -1.3, " minus 1decimal3", status);
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat("##0.0####E0 'g-m/s^2'", custom, status),
           int32_t(0), "0decimal0exponent0 g-m/s^2", status);
    status = U_ZERO_ERROR;
    expect(new DecimalFormat("##0.0####E0 'g-m/s^2'", custom, status),
           1.0/3, "333decimal333exponent minus 3 g-m/s^2", status);
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat("##0.0#### 'g-m/s^2'", custom, status),
           int32_t(0), "0decimal0 g-m/s^2", status);
    status = U_ZERO_ERROR;
    expect(new DecimalFormat("##0.0#### 'g-m/s^2'", custom, status),
           1.0/3, "0decimal33333 g-m/s^2", status);

    UnicodeString zero((UChar32)0x10000);
    custom.setSymbol(DecimalFormatSymbols::kZeroDigitSymbol, zero);
    expStr = UnicodeString("\\U00010001decimal\\U00010002\\U00010005\\U00010000", "");
    expStr = expStr.unescape();
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat("##0.000", custom, status),
           1.25, expStr, status);

    custom.setSymbol(DecimalFormatSymbols::kZeroDigitSymbol, (UChar)0x30);
    custom.setSymbol(DecimalFormatSymbols::kCurrencySymbol, "units of money");
    custom.setSymbol(DecimalFormatSymbols::kMonetarySeparatorSymbol, "money separator");
    patternStr = UNICODE_STRING_SIMPLE("0.00 \\u00A4' in your bank account'");
    patternStr = patternStr.unescape();
    expStr = UnicodeString(" minus 20money separator00 units of money in your bank account", "");
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat(patternStr, custom, status),
           int32_t(-20), expStr, status);

    custom.setSymbol(DecimalFormatSymbols::kPercentSymbol, "percent");
    patternStr = "'You''ve lost ' -0.00 %' of your money today'";
    patternStr = patternStr.unescape();
    expStr = UnicodeString(" minus You've lost   minus 2000decimal00 percent of your money today", "");
    status = U_ZERO_ERROR;
    expect2(new DecimalFormat(patternStr, custom, status),
           int32_t(-20), expStr, status);
}

void NumberFormatTest::TestCurrencyPatterns(void) {
    int32_t i, locCount;
    const Locale* locs = NumberFormat::getAvailableLocales(locCount);
    for (i=0; i<locCount; ++i) {
        UErrorCode ec = U_ZERO_ERROR;
        NumberFormat* nf = NumberFormat::createCurrencyInstance(locs[i], ec);
        if (U_FAILURE(ec)) {
            errln("FAIL: Can't create NumberFormat(%s) - %s", locs[i].getName(), u_errorName(ec));
        } else {
            // Make sure currency formats do not have a variable number
            // of fraction digits
            int32_t min = nf->getMinimumFractionDigits();
            int32_t max = nf->getMaximumFractionDigits();
            if (min != max) {
                UnicodeString a, b;
                nf->format(1.0, a);
                nf->format(1.125, b);
                errln((UnicodeString)"FAIL: " + locs[i].getName() +
                      " min fraction digits != max fraction digits; "
                      "x 1.0 => " + escape(a) +
                      "; x 1.125 => " + escape(b));
            }

            // Make sure EURO currency formats have exactly 2 fraction digits
            if (nf->getDynamicClassID() == DecimalFormat::getStaticClassID()) {
                DecimalFormat* df = (DecimalFormat*) nf;
                if (u_strcmp(EUR, df->getCurrency()) == 0) {
                    if (min != 2 || max != 2) {
                        UnicodeString a;
                        nf->format(1.0, a);
                        errln((UnicodeString)"FAIL: " + locs[i].getName() +
                              " is a EURO format but it does not have 2 fraction digits; "
                              "x 1.0 => " +
                              escape(a));
                    }
                }
            }
        }
        delete nf;
    }
}

void NumberFormatTest::TestRegCurrency(void) {
#if !UCONFIG_NO_SERVICE
    UErrorCode status = U_ZERO_ERROR;
    UChar USD[4];
    ucurr_forLocale("en_US", USD, 4, &status);
    UChar YEN[4];
    ucurr_forLocale("ja_JP", YEN, 4, &status);
    UChar TMP[4];
    static const UChar QQQ[] = {0x51, 0x51, 0x51, 0};
    if(U_FAILURE(status)) {
        errln("Unable to get currency for locale, error %s", u_errorName(status));
        return;
    }
    
    UCurrRegistryKey enkey = ucurr_register(YEN, "en_US", &status);
    UCurrRegistryKey enUSEUROkey = ucurr_register(QQQ, "en_US_EURO", &status);
    
    ucurr_forLocale("en_US", TMP, 4, &status);
    if (u_strcmp(YEN, TMP) != 0) {
        errln("FAIL: didn't return YEN registered for en_US");
    }

    ucurr_forLocale("en_US_EURO", TMP, 4, &status);
    if (u_strcmp(QQQ, TMP) != 0) {
        errln("FAIL: didn't return QQQ for en_US_EURO");
    }
    
    int32_t fallbackLen = ucurr_forLocale("en_XX_BAR", TMP, 4, &status);
    if (fallbackLen) {
        errln("FAIL: tried to fallback en_XX_BAR");
    }
    status = U_ZERO_ERROR; // reset
    
    if (!ucurr_unregister(enkey, &status)) {
        errln("FAIL: couldn't unregister enkey");
    }

    ucurr_forLocale("en_US", TMP, 4, &status);        
    if (u_strcmp(USD, TMP) != 0) {
        errln("FAIL: didn't return USD for en_US after unregister of en_US");
    }
    status = U_ZERO_ERROR; // reset
    
    ucurr_forLocale("en_US_EURO", TMP, 4, &status);
    if (u_strcmp(QQQ, TMP) != 0) {
        errln("FAIL: didn't return QQQ for en_US_EURO after unregister of en_US");
    }
    
    ucurr_forLocale("en_US_BLAH", TMP, 4, &status);
    if (u_strcmp(USD, TMP) != 0) {
        errln("FAIL: could not find USD for en_US_BLAH after unregister of en");
    }
    status = U_ZERO_ERROR; // reset
    
    if (!ucurr_unregister(enUSEUROkey, &status)) {
        errln("FAIL: couldn't unregister enUSEUROkey");
    }
    
    ucurr_forLocale("en_US_EURO", TMP, 4, &status);
    if (u_strcmp(EUR, TMP) != 0) {
        errln("FAIL: didn't return EUR for en_US_EURO after unregister of en_US_EURO");
    }
    status = U_ZERO_ERROR; // reset
#endif
}

void NumberFormatTest::TestCurrencyNames(void) {
    // Do a basic check of getName()
    // USD { "US$", "US Dollar"            } // 04/04/1792-
    UErrorCode ec = U_ZERO_ERROR;
    static const UChar USD[] = {0x55, 0x53, 0x44, 0}; /*USD*/
    static const UChar USX[] = {0x55, 0x53, 0x58, 0}; /*USX*/
    static const UChar CAD[] = {0x43, 0x41, 0x44, 0}; /*CAD*/
    static const UChar ITL[] = {0x49, 0x54, 0x4C, 0}; /*ITL*/
    UBool isChoiceFormat;
    int32_t len;
    // Warning: HARD-CODED LOCALE DATA in this test.  If it fails, CHECK
    // THE LOCALE DATA before diving into the code.
    assertEquals("USD.getName(SYMBOL_NAME)",
                 UnicodeString("$"),
                 UnicodeString(ucurr_getName(USD, "en",
                                             UCURR_SYMBOL_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("USD.getName(LONG_NAME)",
                 UnicodeString("US Dollar"),
                 UnicodeString(ucurr_getName(USD, "en",
                                             UCURR_LONG_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("CAD.getName(SYMBOL_NAME)",
                 UnicodeString("CA$"),
                 UnicodeString(ucurr_getName(CAD, "en",
                                             UCURR_SYMBOL_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("CAD.getName(SYMBOL_NAME)",
                 UnicodeString("$"),
                 UnicodeString(ucurr_getName(CAD, "en_CA",
                                             UCURR_SYMBOL_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("USD.getName(SYMBOL_NAME)",
                 UnicodeString("US$"),
                 UnicodeString(ucurr_getName(USD, "en_AU",
                                             UCURR_SYMBOL_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("CAD.getName(SYMBOL_NAME)",
                 UnicodeString("CA$"),
                 UnicodeString(ucurr_getName(CAD, "en_AU",
                                             UCURR_SYMBOL_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertEquals("USX.getName(LONG_NAME)",
                 UnicodeString("USX"),
                 UnicodeString(ucurr_getName(USX, "en_US",
                                             UCURR_LONG_NAME,
                                             &isChoiceFormat, &len, &ec)));
    assertSuccess("ucurr_getName", ec);
    
    ec = U_ZERO_ERROR;

    // Test that a default or fallback warning is being returned. JB 4239.
    ucurr_getName(CAD, "es_ES", UCURR_LONG_NAME, &isChoiceFormat,
                            &len, &ec);
    assertTrue("ucurr_getName (fallback)",
                    U_USING_FALLBACK_WARNING == ec, TRUE);

    ucurr_getName(CAD, "zh_TW", UCURR_LONG_NAME, &isChoiceFormat,
                            &len, &ec);
    assertTrue("ucurr_getName (fallback)",
                    U_USING_FALLBACK_WARNING == ec, TRUE);

    ucurr_getName(CAD, "en_US", UCURR_LONG_NAME, &isChoiceFormat,
                            &len, &ec);
    assertTrue("ucurr_getName (default)",
                    U_USING_DEFAULT_WARNING == ec, TRUE);
    
    ucurr_getName(CAD, "vi", UCURR_LONG_NAME, &isChoiceFormat,
                            &len, &ec);
    assertTrue("ucurr_getName (default)",
                    U_USING_DEFAULT_WARNING == ec, TRUE);
    
    // Test that a default warning is being returned when falling back to root. JB 4536.
    ucurr_getName(ITL, "cy", UCURR_LONG_NAME, &isChoiceFormat,
                            &len, &ec);
    assertTrue("ucurr_getName (default to root)",
                    U_USING_DEFAULT_WARNING == ec, TRUE);
    
    // TODO add more tests later
}

void NumberFormatTest::TestCurrencyUnit(void){
    UErrorCode ec = U_ZERO_ERROR;
    static const UChar USD[] = {85, 83, 68, 0}; /*USD*/
    CurrencyUnit cu(USD, ec);
    assertSuccess("CurrencyUnit", ec);

    const UChar * r = cu.getISOCurrency(); // who is the buffer owner ?
    assertEquals("getISOCurrency()", USD, r);

    CurrencyUnit cu2(cu);
    if (!(cu2 == cu)){
        errln("CurrencyUnit copy constructed object should be same");
    }

    CurrencyUnit * cu3 = (CurrencyUnit *)cu.clone();
    if (!(*cu3 == cu)){
        errln("CurrencyUnit cloned object should be same");
    }
    delete cu3;
}

void NumberFormatTest::TestCurrencyAmount(void){
    UErrorCode ec = U_ZERO_ERROR;
    static const UChar USD[] = {85, 83, 68, 0}; /*USD*/
    CurrencyAmount ca(9, USD, ec);
    assertSuccess("CurrencyAmount", ec);

    CurrencyAmount ca2(ca);
    if (!(ca2 == ca)){
        errln("CurrencyAmount copy constructed object should be same");
    }

    ca2=ca;
    if (!(ca2 == ca)){
        errln("CurrencyAmount assigned object should be same");
    }
    
    CurrencyAmount *ca3 = (CurrencyAmount *)ca.clone();
    if (!(*ca3 == ca)){
        errln("CurrencyAmount cloned object should be same");
    }
    delete ca3;
}

void NumberFormatTest::TestSymbolsWithBadLocale(void) {
    Locale locDefault;
    Locale locBad("x-crazy_ZZ_MY_SPECIAL_ADMINISTRATION_REGION_NEEDS_A_SPECIAL_VARIANT_WITH_A_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_REALLY_LONG_NAME");
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString intlCurrencySymbol((UChar)0xa4);

    intlCurrencySymbol.append((UChar)0xa4);

    logln("Current locale is %s", Locale::getDefault().getName());
    Locale::setDefault(locBad, status);
    logln("Current locale is %s", Locale::getDefault().getName());
    DecimalFormatSymbols mySymbols(status);
    if (status != U_USING_FALLBACK_WARNING) {
        errln("DecimalFormatSymbols should returned U_USING_FALLBACK_WARNING.");
    }
    if (strcmp(mySymbols.getLocale().getName(), locBad.getName()) != 0) {
        errln("DecimalFormatSymbols does not have the right locale.");
    }
    int symbolEnum = (int)DecimalFormatSymbols::kDecimalSeparatorSymbol;
    for (; symbolEnum < (int)DecimalFormatSymbols::kFormatSymbolCount; symbolEnum++) {
        logln(UnicodeString("DecimalFormatSymbols[") + symbolEnum + UnicodeString("] = ")
            + prettify(mySymbols.getSymbol((DecimalFormatSymbols::ENumberFormatSymbol)symbolEnum)));

        if (mySymbols.getSymbol((DecimalFormatSymbols::ENumberFormatSymbol)symbolEnum).length() == 0
            && symbolEnum != (int)DecimalFormatSymbols::kGroupingSeparatorSymbol
            && symbolEnum != (int)DecimalFormatSymbols::kMonetaryGroupingSeparatorSymbol)
        {
            errln("DecimalFormatSymbols has an empty string at index %d.", symbolEnum);
        }
    }
    status = U_ZERO_ERROR;
    Locale::setDefault(locDefault, status);
    logln("Current locale is %s", Locale::getDefault().getName());
}

/**
 * Check that adoptDecimalFormatSymbols and setDecimalFormatSymbols
 * behave the same, except for memory ownership semantics. (No
 * version of this test on Java, since Java has only one method.)
 */
void NumberFormatTest::TestAdoptDecimalFormatSymbols(void) {
    UErrorCode ec = U_ZERO_ERROR;
    DecimalFormatSymbols *sym = new DecimalFormatSymbols(Locale::getUS(), ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormatSymbols constructor");
        delete sym;
        return;
    }
    UnicodeString pat(" #,##0.00");
    pat.insert(0, (UChar)0x00A4);
    DecimalFormat fmt(pat, sym, ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormat constructor");
        return;
    }

    UnicodeString str;
    fmt.format(2350.75, str);
    if (str == "$ 2,350.75") {
        logln(str);
    } else {
        errln("Fail: " + str + ", expected $ 2,350.75");
    }

    sym = new DecimalFormatSymbols(Locale::getUS(), ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormatSymbols constructor");
        delete sym;
        return;
    }
    sym->setSymbol(DecimalFormatSymbols::kCurrencySymbol, "Q");
    fmt.adoptDecimalFormatSymbols(sym);

    str.truncate(0);
    fmt.format(2350.75, str);
    if (str == "Q 2,350.75") {
        logln(str);
    } else {
        errln("Fail: adoptDecimalFormatSymbols -> " + str + ", expected Q 2,350.75");
    }

    sym = new DecimalFormatSymbols(Locale::getUS(), ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormatSymbols constructor");
        delete sym;
        return;
    }
    DecimalFormat fmt2(pat, sym, ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormat constructor");
        return;
    }
    
    DecimalFormatSymbols sym2(Locale::getUS(), ec);
    if (U_FAILURE(ec)) {
        errln("Fail: DecimalFormatSymbols constructor");
        return;
    }
    sym2.setSymbol(DecimalFormatSymbols::kCurrencySymbol, "Q");
    fmt2.setDecimalFormatSymbols(sym2);

    str.truncate(0);
    fmt2.format(2350.75, str);
    if (str == "Q 2,350.75") {
        logln(str);
    } else {
        errln("Fail: setDecimalFormatSymbols -> " + str + ", expected Q 2,350.75");
    }
}

void NumberFormatTest::TestPerMill() {
    UErrorCode ec = U_ZERO_ERROR;
    UnicodeString str;
    DecimalFormat fmt(ctou("###.###\\u2030"), ec);
    if (!assertSuccess("DecimalFormat ct", ec)) return;
    assertEquals("0.4857 x ###.###\\u2030",
                 ctou("485.7\\u2030"), fmt.format(0.4857, str));
    
    DecimalFormatSymbols sym(Locale::getUS(), ec);
    sym.setSymbol(DecimalFormatSymbols::kPerMillSymbol, ctou("m"));
    DecimalFormat fmt2("", sym, ec);
    fmt2.applyLocalizedPattern("###.###m", ec);
    if (!assertSuccess("setup", ec)) return;
    str.truncate(0);
    assertEquals("0.4857 x ###.###m",
                 "485.7m", fmt2.format(0.4857, str));
}

/**
 * Generic test for patterns that should be legal/illegal.
 */
void NumberFormatTest::TestIllegalPatterns() {
    // Test cases:
    // Prefix with "-:" for illegal patterns
    // Prefix with "+:" for legal patterns
    const char* DATA[] = {
        // Unquoted special characters in the suffix are illegal
        "-:000.000|###",
        "+:000.000'|###'",
        0
    };
    for (int32_t i=0; DATA[i]; ++i) {
        const char* pat=DATA[i];
        UBool valid = (*pat) == '+';
        pat += 2;
        UErrorCode ec = U_ZERO_ERROR;
        DecimalFormat fmt(pat, ec); // locale doesn't matter here
        if (U_SUCCESS(ec) == valid) {
            logln("Ok: pattern \"%s\": %s",
                  pat, u_errorName(ec));
        } else {
            errln("FAIL: pattern \"%s\" should have %s; got %s",
                  pat, (valid?"succeeded":"failed"),
                  u_errorName(ec));
        }
    }
}

//----------------------------------------------------------------------

static const char* KEYWORDS[] = {
    /*0*/ "ref=", // <reference pattern to parse numbers>
    /*1*/ "loc=", // <locale for formats>
    /*2*/ "f:",   // <pattern or '-'> <number> <exp. string>
    /*3*/ "fp:",  // <pattern or '-'> <number> <exp. string> <exp. number>
    /*4*/ "rt:",  // <pattern or '-'> <(exp.) number> <(exp.) string>
    /*5*/ "p:",   // <pattern or '-'> <string> <exp. number>
    /*6*/ "perr:", // <pattern or '-'> <invalid string>
    /*7*/ "pat:", // <pattern or '-'> <exp. toPattern or '-' or 'err'>
    /*8*/ "fpc:", // <pattern or '-'> <curr.amt> <exp. string> <exp. curr.amt>
    0
};

/**
 * Return an integer representing the next token from this
 * iterator.  The integer will be an index into the given list, or
 * -1 if there are no more tokens, or -2 if the token is not on
 * the list.
 */
static int32_t keywordIndex(const UnicodeString& tok) {
    for (int32_t i=0; KEYWORDS[i]!=0; ++i) {
        if (tok==KEYWORDS[i]) {
            return i;
        }
    }
    return -1;
}

/**
 * Parse a CurrencyAmount using the given NumberFormat, with
 * the 'delim' character separating the number and the currency.
 */
static void parseCurrencyAmount(const UnicodeString& str,
                                const NumberFormat& fmt,
                                UChar delim,
                                Formattable& result,
                                UErrorCode& ec) {
    UnicodeString num, cur;
    int32_t i = str.indexOf(delim);
    str.extractBetween(0, i, num);
    str.extractBetween(i+1, INT32_MAX, cur);
    Formattable n;
    fmt.parse(num, n, ec);
    result.adoptObject(new CurrencyAmount(n, cur.getTerminatedBuffer(), ec));
}

void NumberFormatTest::TestCases() {
    UErrorCode ec = U_ZERO_ERROR;
    TextFile reader("NumberFormatTestCases.txt", "UTF8", ec);
    if (U_FAILURE(ec)) {
        dataerrln("[DATA] Couldn't open NumberFormatTestCases.txt");
        return;
    }
    TokenIterator tokens(&reader);

    Locale loc("en", "US", "");
    DecimalFormat *ref = 0, *fmt = 0;
    MeasureFormat *mfmt = 0;
    UnicodeString pat, tok, mloc, str, out, where, currAmt;
    Formattable n;

    for (;;) {
        ec = U_ZERO_ERROR;
        if (!tokens.next(tok, ec)) {
            break;
        }
        where = UnicodeString("(") + tokens.getLineNumber() + ") ";
        int32_t cmd = keywordIndex(tok);
        switch (cmd) {
        case 0:
            // ref= <reference pattern>
            if (!tokens.next(tok, ec)) goto error;
            delete ref;
            ref = new DecimalFormat(tok,
                      new DecimalFormatSymbols(Locale::getUS(), ec), ec);
            if (U_FAILURE(ec)) {
                dataerrln("Error constructing DecimalFormat");
                goto error;
            }
            break;
        case 1:
            // loc= <locale>
            if (!tokens.next(tok, ec)) goto error;
            loc = Locale::createFromName(CharString(tok));
            break;
        case 2: // f:
        case 3: // fp:
        case 4: // rt:
        case 5: // p:
            if (!tokens.next(tok, ec)) goto error;
            if (tok != "-") {
                pat = tok;
                delete fmt;
                fmt = new DecimalFormat(pat, new DecimalFormatSymbols(loc, ec), ec);
                if (U_FAILURE(ec)) {
                    errln("FAIL: " + where + "Pattern \"" + pat + "\": " + u_errorName(ec));
                    ec = U_ZERO_ERROR;
                    if (!tokens.next(tok, ec)) goto error;
                    if (!tokens.next(tok, ec)) goto error;
                    if (cmd == 3) {
                        if (!tokens.next(tok, ec)) goto error;
                    }
                    continue;
                }
            }
            if (cmd == 2 || cmd == 3 || cmd == 4) {
                // f: <pattern or '-'> <number> <exp. string>
                // fp: <pattern or '-'> <number> <exp. string> <exp. number>
                // rt: <pattern or '-'> <number> <string>
                UnicodeString num;
                if (!tokens.next(num, ec)) goto error;
                if (!tokens.next(str, ec)) goto error;
                ref->parse(num, n, ec);
                assertSuccess("parse", ec);
                assertEquals(where + "\"" + pat + "\".format(" + num + ")",
                             str, fmt->format(n, out.remove(), ec));
                assertSuccess("format", ec);
                if (cmd == 3) { // fp:
                    if (!tokens.next(num, ec)) goto error;
                    ref->parse(num, n, ec);
                    assertSuccess("parse", ec);
                }
                if (cmd != 2) { // != f:
                    Formattable m;
                    fmt->parse(str, m, ec);
                    assertSuccess("parse", ec);
                    assertEquals(where + "\"" + pat + "\".parse(\"" + str + "\")",
                                 n, m);
                } 
            }
            // p: <pattern or '-'> <string to parse> <exp. number>
            else {
                UnicodeString expstr;
                if (!tokens.next(str, ec)) goto error;
                if (!tokens.next(expstr, ec)) goto error;
                Formattable exp, n;
                ref->parse(expstr, exp, ec);
                assertSuccess("parse", ec);
                fmt->parse(str, n, ec);
                assertSuccess("parse", ec);
                assertEquals(where + "\"" + pat + "\".parse(\"" + str + "\")",
                             exp, n);
            }
            break;
        case 8: // fpc:
            if (!tokens.next(tok, ec)) goto error;
            if (tok != "-") {
                mloc = tok;
                delete mfmt;
                mfmt = MeasureFormat::createCurrencyFormat(
                           Locale::createFromName(CharString(mloc)), ec);
                if (U_FAILURE(ec)) {
                    errln("FAIL: " + where + "Loc \"" + mloc + "\": " + u_errorName(ec));
                    ec = U_ZERO_ERROR;
                    if (!tokens.next(tok, ec)) goto error;
                    if (!tokens.next(tok, ec)) goto error;
                    if (!tokens.next(tok, ec)) goto error;
                    continue;
                }
            }
            // fpc: <loc or '-'> <curr.amt> <exp. string> <exp. curr.amt>
            if (!tokens.next(currAmt, ec)) goto error;
            if (!tokens.next(str, ec)) goto error;
            parseCurrencyAmount(currAmt, *ref, (UChar)0x2F/*'/'*/, n, ec);
            if (assertSuccess("parseCurrencyAmount", ec)) {
                assertEquals(where + "getCurrencyFormat(" + mloc + ").format(" + currAmt + ")",
                             str, mfmt->format(n, out.remove(), ec));
                assertSuccess("format", ec);
            }
            if (!tokens.next(currAmt, ec)) goto error;
            parseCurrencyAmount(currAmt, *ref, (UChar)0x2F/*'/'*/, n, ec);
            if (assertSuccess("parseCurrencyAmount", ec)) {
                Formattable m;

                mfmt->parseObject(str, m, ec);
                if (assertSuccess("parseCurrency", ec)) {
                    assertEquals(where + "getCurrencyFormat(" + mloc + ").parse(\"" + str + "\")",
                                 n, m);
                } else {
                    errln("FAIL: source " + str);
                }
            }
            break;
        case 6:
            // perr: <pattern or '-'> <invalid string>
            errln("FAIL: Under construction");
            goto done;
        case 7: {
            // pat: <pattern> <exp. toPattern, or '-' or 'err'>
            UnicodeString testpat;
            UnicodeString exppat;
            if (!tokens.next(testpat, ec)) goto error;
            if (!tokens.next(exppat, ec)) goto error;
            UBool err = exppat == "err";
            UBool existingPat = FALSE;
            if (testpat == "-") {
                if (err) {
                    errln("FAIL: " + where + "Invalid command \"pat: - err\"");
                    continue;
                }
                existingPat = TRUE;
                testpat = pat;
            }
            if (exppat == "-") exppat = testpat;
            DecimalFormat* f = 0;
            UErrorCode ec2 = U_ZERO_ERROR;
            if (existingPat) {
                f = fmt;
            } else {
                f = new DecimalFormat(testpat, ec2);
            }
            if (U_SUCCESS(ec2)) {
                if (err) {
                    errln("FAIL: " + where + "Invalid pattern \"" + testpat +
                          "\" was accepted");
                } else {
                    UnicodeString pat2;
                    assertEquals(where + "\"" + testpat + "\".toPattern()",
                                 exppat, f->toPattern(pat2));
                }
            } else {
                if (err) {
                    logln("Ok: " + where + "Invalid pattern \"" + testpat +
                          "\" failed: " + u_errorName(ec2));
                } else {
                    errln("FAIL: " + where + "Valid pattern \"" + testpat +
                          "\" failed: " + u_errorName(ec2));
                }
            }
            if (!existingPat) delete f;
            } break;
        case -1:
            errln("FAIL: " + where + "Unknown command \"" + tok + "\"");
            goto done;
        }
    }
    goto done;

 error:
    if (U_SUCCESS(ec)) {
        errln("FAIL: Unexpected EOF");
    } else {
        errln("FAIL: " + where + "Unexpected " + u_errorName(ec));
    }

 done:
    delete mfmt;
    delete fmt;
    delete ref;
}


//----------------------------------------------------------------------
// Support methods
//----------------------------------------------------------------------

UBool NumberFormatTest::equalValue(const Formattable& a, const Formattable& b) {
    if (a.getType() == b.getType()) {
        return a == b;
    }

    if (a.getType() == Formattable::kLong) {
        if (b.getType() == Formattable::kInt64) {
            return a.getLong() == b.getLong();
        } else if (b.getType() == Formattable::kDouble) {
            return (double) a.getLong() == b.getDouble(); // TODO check use of double instead of long 
        }
    } else if (a.getType() == Formattable::kDouble) {
        if (b.getType() == Formattable::kLong) {
            return a.getDouble() == (double) b.getLong();
        } else if (b.getType() == Formattable::kInt64) {
            return a.getDouble() == (double)b.getInt64();
        }
    } else if (a.getType() == Formattable::kInt64) {
        if (b.getType() == Formattable::kLong) {
                return a.getInt64() == (int64_t)b.getLong();
        } else if (b.getType() == Formattable::kDouble) {
            return a.getInt64() == (int64_t)b.getDouble();
        }
    }
    return FALSE;
}

void NumberFormatTest::expect3(NumberFormat& fmt, const Formattable& n, const UnicodeString& str) {
    // Don't round-trip format test, since we explicitly do it
    expect_rbnf(fmt, n, str, FALSE);
    expect_rbnf(fmt, str, n);
}

void NumberFormatTest::expect2(NumberFormat& fmt, const Formattable& n, const UnicodeString& str) {
    // Don't round-trip format test, since we explicitly do it
    expect(fmt, n, str, FALSE);
    expect(fmt, str, n);
}

void NumberFormatTest::expect2(NumberFormat* fmt, const Formattable& n,
                               const UnicodeString& exp,
                               UErrorCode status) {
    if (U_FAILURE(status)) {
        errln("FAIL: NumberFormat constructor");
    } else {
        expect2(*fmt, n, exp);
    }
    delete fmt;
}

void NumberFormatTest::expect(NumberFormat& fmt, const UnicodeString& str, const Formattable& n) {
    UErrorCode status = U_ZERO_ERROR;
    Formattable num;
    fmt.parse(str, num, status);
    if (U_FAILURE(status)) {
        errln(UnicodeString("FAIL: Parse failed for \"") + str + "\"");
        return;
    }
    UnicodeString pat;
    ((DecimalFormat*) &fmt)->toPattern(pat);
    if (equalValue(num, n)) {
        logln(UnicodeString("Ok   \"") + str + "\" x " +
              pat + " = " +
              toString(num));
    } else {
        errln(UnicodeString("FAIL \"") + str + "\" x " +
              pat + " = " +
              toString(num) + ", expected " + toString(n));
    }
}

void NumberFormatTest::expect_rbnf(NumberFormat& fmt, const UnicodeString& str, const Formattable& n) {
    UErrorCode status = U_ZERO_ERROR;
    Formattable num;
    fmt.parse(str, num, status);
    if (U_FAILURE(status)) {
        errln(UnicodeString("FAIL: Parse failed for \"") + str + "\"");
        return;
    }
    if (equalValue(num, n)) {
        logln(UnicodeString("Ok   \"") + str + " = " +
              toString(num));
    } else {
        errln(UnicodeString("FAIL \"") + str + " = " +
              toString(num) + ", expected " + toString(n));
    }
}

void NumberFormatTest::expect_rbnf(NumberFormat& fmt, const Formattable& n,
                              const UnicodeString& exp, UBool rt) {
    UnicodeString saw;
    FieldPosition pos;
    UErrorCode status = U_ZERO_ERROR;
    fmt.format(n, saw, pos, status);
    CHECK(status, "NumberFormat::format");
    if (saw == exp) {
        logln(UnicodeString("Ok   ") + toString(n) + 
              " = \"" +
              escape(saw) + "\"");
        // We should be able to round-trip the formatted string =>
        // number => string (but not the other way around: number
        // => string => number2, might have number2 != number):
        if (rt) {
            Formattable n2;
            fmt.parse(exp, n2, status);
            if (U_FAILURE(status)) {
                errln(UnicodeString("FAIL: Parse failed for \"") + exp + "\"");
                return;
            }
            UnicodeString saw2;
            fmt.format(n2, saw2, pos, status);
            CHECK(status, "NumberFormat::format");
            if (saw2 != exp) {
                errln((UnicodeString)"FAIL \"" + exp + "\" => " + toString(n2) +
                      " => \"" + saw2 + "\"");
            }
        }
    } else {
        errln(UnicodeString("FAIL ") + toString(n) + 
              " = \"" +
              escape(saw) + "\", expected \"" + exp + "\"");
    }
}

void NumberFormatTest::expect(NumberFormat& fmt, const Formattable& n,
                              const UnicodeString& exp, UBool rt) {
    UnicodeString saw;
    FieldPosition pos;
    UErrorCode status = U_ZERO_ERROR;
    fmt.format(n, saw, pos, status);
    CHECK(status, "NumberFormat::format");
    UnicodeString pat;
    ((DecimalFormat*) &fmt)->toPattern(pat);
    if (saw == exp) {
        logln(UnicodeString("Ok   ") + toString(n) + " x " +
              escape(pat) + " = \"" +
              escape(saw) + "\"");
        // We should be able to round-trip the formatted string =>
        // number => string (but not the other way around: number
        // => string => number2, might have number2 != number):
        if (rt) {
            Formattable n2;
            fmt.parse(exp, n2, status);
            if (U_FAILURE(status)) {
                errln(UnicodeString("FAIL: Parse failed for \"") + exp + "\"");
                return;
            }
            UnicodeString saw2;
            fmt.format(n2, saw2, pos, status);
            CHECK(status, "NumberFormat::format");
            if (saw2 != exp) {
                errln((UnicodeString)"FAIL \"" + exp + "\" => " + toString(n2) +
                      " => \"" + saw2 + "\"");
            }
        }
    } else {
        errln(UnicodeString("FAIL ") + toString(n) + " x " +
              escape(pat) + " = \"" +
              escape(saw) + "\", expected \"" + exp + "\"");
    }
}

void NumberFormatTest::expect(NumberFormat* fmt, const Formattable& n,
                              const UnicodeString& exp,
                              UErrorCode status) {
    if (U_FAILURE(status)) {
        errln("FAIL: NumberFormat constructor");
    } else {
        expect(*fmt, n, exp);
    }
    delete fmt;
}

void NumberFormatTest::expectCurrency(NumberFormat& nf, const Locale& locale,
                                      double value, const UnicodeString& string) {
    UErrorCode ec = U_ZERO_ERROR;
    DecimalFormat& fmt = * (DecimalFormat*) &nf;
    const UChar DEFAULT_CURR[] = {45/*-*/,0};
    UChar curr[4];
    u_strcpy(curr, DEFAULT_CURR);
    if (*locale.getLanguage() != 0) {
        ucurr_forLocale(locale.getName(), curr, 4, &ec);
        assertSuccess("ucurr_forLocale", ec);
        fmt.setCurrency(curr, ec);
        assertSuccess("DecimalFormat::setCurrency", ec);
        fmt.setCurrency(curr); //Deprecated variant, for coverage only
    }
    UnicodeString s;
    fmt.format(value, s);
    s.findAndReplace((UChar32)0x00A0, (UChar32)0x0020);

    // Default display of the number yields "1234.5599999999999"
    // instead of "1234.56".  Use a formatter to fix this.
    NumberFormat* f = 
        NumberFormat::createInstance(Locale::getUS(), ec);
    UnicodeString v;
    if (U_FAILURE(ec)) {
        // Oops; bad formatter.  Use default op+= display.
        v = (UnicodeString)"" + value;
    } else {
        f->setMaximumFractionDigits(4);
        f->setGroupingUsed(FALSE);
        f->format(value, v);
    }
    delete f;

    if (s == string) {
        logln((UnicodeString)"Ok: " + v + " x " + curr + " => " + prettify(s));
    } else {
        errln((UnicodeString)"FAIL: " + v + " x " + curr + " => " + prettify(s) +
              ", expected " + prettify(string));
    }
}

void NumberFormatTest::expectPat(DecimalFormat& fmt, const UnicodeString& exp) {
    UnicodeString pat;
    fmt.toPattern(pat);
    if (pat == exp) {
        logln(UnicodeString("Ok   \"") + pat + "\"");
    } else {
        errln(UnicodeString("FAIL \"") + pat + "\", expected \"" + exp + "\"");
    }
}

void NumberFormatTest::expectPad(DecimalFormat& fmt, const UnicodeString& pat,
                                 int32_t pos) {
    expectPad(fmt, pat, pos, 0, (UnicodeString)"");
}
void NumberFormatTest::expectPad(DecimalFormat& fmt, const UnicodeString& pat,
                                 int32_t pos, int32_t width, UChar pad) {
    expectPad(fmt, pat, pos, width, UnicodeString(pad));
}
void NumberFormatTest::expectPad(DecimalFormat& fmt, const UnicodeString& pat,
                                 int32_t pos, int32_t width, const UnicodeString& pad) {
    int32_t apos = 0, awidth = 0;
    UnicodeString apadStr;
    UErrorCode status = U_ZERO_ERROR;
    fmt.applyPattern(pat, status);
    if (U_SUCCESS(status)) {
        apos = fmt.getPadPosition();
        awidth = fmt.getFormatWidth();
        apadStr=fmt.getPadCharacterString();
    } else {
        apos = -1;
        awidth = width;
        apadStr = pad;
    }
    if (apos == pos && awidth == width && apadStr == pad) {
        UnicodeString infoStr;
        if (pos == ILLEGAL) {
            infoStr = UnicodeString(" width=", "") + awidth + UnicodeString(" pad=", "") + apadStr;
        }
        logln(UnicodeString("Ok   \"") + pat + "\" pos=" + apos + infoStr);
    } else {
        errln(UnicodeString("FAIL \"") + pat + "\" pos=" + apos +
              " width=" + awidth + " pad=" + apadStr +
              ", expected " + pos + " " + width + " " + pad);
    }
}
void NumberFormatTest::TestJB3832(){
    const char* localeID = "pt_PT@currency=PTE";
    Locale loc(localeID);
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString expected(CharsToUnicodeString("1,150$50\\u00A0Esc."));
    UnicodeString s;
    NumberFormat* currencyFmt = NumberFormat::createCurrencyInstance(loc, status);
    if(U_FAILURE(status)){
        errln("Could not create currency formatter for locale %s", localeID);
        return;
    }
    currencyFmt->format(1150.50, s);
    if(s!=expected){
        errln(UnicodeString("FAIL: Expected: ")+expected 
                + UnicodeString(" Got: ") + s 
                + UnicodeString( " for locale: ")+ UnicodeString(localeID) );
    }
    if (U_FAILURE(status)){
        errln("FAIL: Status %s", u_errorName(status));
    }
    delete currencyFmt;
}

void NumberFormatTest::TestHost()
{
#ifdef U_WINDOWS
    Win32NumberTest::testLocales(this);
#endif
    for (NumberFormat::EStyles k = NumberFormat::kNumberStyle;
         k < NumberFormat::kStyleCount; k = (NumberFormat::EStyles)(k+1)) {
        UErrorCode status = U_ZERO_ERROR;
        Locale loc("en_US@compat=host");
        NumberFormat *full = NumberFormat::createInstance(loc, status);
        if (full == NULL || U_FAILURE(status)) {
            errln("FAIL: Can't create number instance for host");
            return;
        }
        UnicodeString result1;
        Formattable number(10.00);
        full->format(number, result1, status);
        if (U_FAILURE(status)) {
            errln("FAIL: Can't format for host");
            return;
        }
        Formattable formattable;
        full->parse(result1, formattable, status);
        if (U_FAILURE(status)) {
            errln("FAIL: Can't parse for host");
            return;
        }
    }
}

void NumberFormatTest::TestHostClone()
{
    /*
    Verify that a cloned formatter gives the same results
    and is useable after the original has been deleted.
    */
    // This is mainly important on Windows.
    UErrorCode status = U_ZERO_ERROR;
    Locale loc("en_US@compat=host");
    UDate now = Calendar::getNow();
    NumberFormat *full = NumberFormat::createInstance(loc, status);
    if (full == NULL || U_FAILURE(status)) {
        errln("FAIL: Can't create Relative date instance");
        return;
    }
    UnicodeString result1;
    full->format(now, result1, status);
    Format *fullClone = full->clone();
    delete full;
    full = NULL;

    UnicodeString result2;
    fullClone->format(now, result2, status);
    if (U_FAILURE(status)) {
        errln("FAIL: format failure.");
    }
    if (result1 != result2) {
        errln("FAIL: Clone returned different result from non-clone.");
    }
    delete fullClone;
}

void NumberFormatTest::TestCurrencyFormat()
{
    // This test is here to increase code coverage.
    UErrorCode status = U_ZERO_ERROR;
    MeasureFormat *cloneObj;
    UnicodeString str;
    Formattable toFormat, result;
    static const UChar ISO_CODE[4] = {0x0047, 0x0042, 0x0050, 0};

    Locale  saveDefaultLocale = Locale::getDefault();
    Locale::setDefault( Locale::getUK(), status );
    if (U_FAILURE(status)) {
        errln("couldn't set default Locale!");
        return;
    }

    MeasureFormat *measureObj = MeasureFormat::createCurrencyFormat(status);
    Locale::setDefault( saveDefaultLocale, status );
    if (U_FAILURE(status)){
        errln("FAIL: Status %s", u_errorName(status));
        return;
    }
    cloneObj = (MeasureFormat *)measureObj->clone();
    if (cloneObj == NULL) {
        errln("Clone doesn't work");
        return;
    }
    toFormat.adoptObject(new CurrencyAmount(1234.56, ISO_CODE, status));
    measureObj->format(toFormat, str, status);
    measureObj->parseObject(str, result, status);
    if (U_FAILURE(status)){
        errln("FAIL: Status %s", u_errorName(status));
    }
    if (result != toFormat) {
        errln("measureObj does not round trip. Formatted string was \"" + str + "\" Got: " + toString(result) + " Expected: " + toString(toFormat));
    }
    status = U_ZERO_ERROR;
    str.truncate(0);
    cloneObj->format(toFormat, str, status);
    cloneObj->parseObject(str, result, status);
    if (U_FAILURE(status)){
        errln("FAIL: Status %s", u_errorName(status));
    }
    if (result != toFormat) {
        errln("Clone does not round trip. Formatted string was \"" + str + "\" Got: " + toString(result) + " Expected: " + toString(toFormat));
    }
    if (*measureObj != *cloneObj) {
        errln("Cloned object is not equal to the original object");
    }
    delete measureObj;
    delete cloneObj;

    status = U_USELESS_COLLATOR_ERROR;
    if (MeasureFormat::createCurrencyFormat(status) != NULL) {
        errln("createCurrencyFormat should have returned NULL.");
    }
}

/* Port of ICU4J rounding test. */
void NumberFormatTest::TestRounding() {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat *df = (DecimalFormat*)NumberFormat::createCurrencyInstance(Locale::getEnglish(), status);

    if (U_FAILURE(status)) {
        errln("Unable to create decimal formatter.");
        return;
    }

    int roundingIncrements[]={1, 2, 5, 20, 50, 100};
    int testValues[]={0, 300};

    for (int j=0; j<2; j++) {
        for (int mode=DecimalFormat::kRoundUp;mode<DecimalFormat::kRoundHalfEven;mode++) {
            df->setRoundingMode((DecimalFormat::ERoundingMode)mode);
            for (int increment=0; increment<6; increment++) {
                double base=testValues[j];
                double rInc=roundingIncrements[increment];
                checkRounding(df, base, 20, rInc);
                rInc=1.000000000/rInc;
                checkRounding(df, base, 20, rInc);
            }
        }
    }
    delete df;
}

void NumberFormatTest::checkRounding(DecimalFormat* df, double base, int iterations, double increment) {
    df->setRoundingIncrement(increment);
    double lastParsed=INT32_MIN; //Intger.MIN_VALUE
    for (int i=-iterations; i<=iterations;i++) {
        double iValue=base+(increment*(i*0.1));
        double smallIncrement=0.00000001;
        if (iValue!=0) {
            smallIncrement*=iValue;
        }
        //we not only test the value, but some values in a small range around it
        lastParsed=checkRound(df, iValue-smallIncrement, lastParsed);
        lastParsed=checkRound(df, iValue, lastParsed);
        lastParsed=checkRound(df, iValue+smallIncrement, lastParsed);
    }
}

double NumberFormatTest::checkRound(DecimalFormat* df, double iValue, double lastParsed) {
    UErrorCode status=U_ZERO_ERROR;
    UnicodeString formattedDecimal;
    double parsed;
    Formattable result;
    df->format(iValue, formattedDecimal, status);

    if (U_FAILURE(status)) {
        errln("Error formatting number.");
    }

    df->parse(formattedDecimal, result, status);

    if (U_FAILURE(status)) {
        errln("Error parsing number.");
    }

    parsed=result.getDouble();

    if (lastParsed>parsed) {
        errln("Rounding wrong direction! %d > %d", lastParsed, parsed);
    }

    return lastParsed;
}

void NumberFormatTest::TestNonpositiveMultiplier() {
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols US(Locale::getUS(), status);
    CHECK(status, "DecimalFormatSymbols constructor");
    DecimalFormat df(UnicodeString("0"), US, status);
    CHECK(status, "DecimalFormat(0)");
    
    // test zero multiplier

    int32_t mult = df.getMultiplier();
    df.setMultiplier(0);
    if (df.getMultiplier() != mult) {
        errln("DecimalFormat.setMultiplier(0) did not ignore its zero input");
    }
    
    // test negative multiplier
    
    df.setMultiplier(-1);
    if (df.getMultiplier() != -1) {
        errln("DecimalFormat.setMultiplier(-1) ignored its negative input");
        return;
    }
    
    expect(df, "1122.123", -1122.123);
    expect(df, "-1122.123", 1122.123);
    expect(df, "1.2", -1.2);
    expect(df, "-1.2", 1.2);

    // TODO: change all the following int64_t tests once BigInteger is ported
    // (right now the big numbers get turned into doubles and lose tons of accuracy)
    static const char* posOutOfRange = "9223372036854780000";
    static const char* negOutOfRange = "-9223372036854780000";

    expect(df, U_INT64_MIN, posOutOfRange);
    expect(df, U_INT64_MIN+1, "9223372036854775807");
    expect(df, (int64_t)-123, "123");
    expect(df, (int64_t)123, "-123");
    expect(df, U_INT64_MAX-1, "-9223372036854775806");
    expect(df, U_INT64_MAX, "-9223372036854775807");

    df.setMultiplier(-2);
    expect(df, -(U_INT64_MIN/2)-1, "-9223372036854775806");
    expect(df, -(U_INT64_MIN/2), "-9223372036854775808");
    expect(df, -(U_INT64_MIN/2)+1, negOutOfRange);

    df.setMultiplier(-7);
    expect(df, -(U_INT64_MAX/7)-1, posOutOfRange);
    expect(df, -(U_INT64_MAX/7), "9223372036854775807");
    expect(df, -(U_INT64_MAX/7)+1, "9223372036854775800");

    // TODO: uncomment (and fix up) all the following int64_t tests once BigInteger is ported
    // (right now the big numbers get turned into doubles and lose tons of accuracy)
    //expect2(df, U_INT64_MAX, Int64ToUnicodeString(-U_INT64_MAX));
    //expect2(df, U_INT64_MIN, UnicodeString(Int64ToUnicodeString(U_INT64_MIN), 1));
    //expect2(df, U_INT64_MAX / 2, Int64ToUnicodeString(-(U_INT64_MAX / 2)));
    //expect2(df, U_INT64_MIN / 2, Int64ToUnicodeString(-(U_INT64_MIN / 2)));

    // TODO: uncomment (and fix up) once BigDecimal is ported and DecimalFormat can handle it
    //expect2(df, BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal.valueOf(Long.MAX_VALUE).negate().toString());
    //expect2(df, BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MIN_VALUE).negate().toString());
    //expect2(df, java.math.BigDecimal.valueOf(Long.MAX_VALUE), java.math.BigDecimal.valueOf(Long.MAX_VALUE).negate().toString());
    //expect2(df, java.math.BigDecimal.valueOf(Long.MIN_VALUE), java.math.BigDecimal.valueOf(Long.MIN_VALUE).negate().toString());
}


void 
NumberFormatTest::TestSpaceParsing() {
    // the data are:
    // the string to be parsed, parsed position, parsed error index
    const char* DATA[][3] = {
        {"$124", "4", "-1"},
        {"$124 $124", "4", "-1"},
        {"$124 ", "4", "-1"},
        //{"$ 124 ", "5", "-1"}, // TODO: need to handle space correctly
        //{"$\\u00A0124 ", "5", "-1"}, // TODO: need to handle space correctly
        {"$ 124 ", "0", "0"}, 
        {"$\\u00A0124 ", "0", "0"},
        {" $ 124 ", "0", "0"}, // TODO: need to handle space correctly
        {"124$", "0", "3"}, // TODO: need to handle space correctly
        {"124 $", "5", "-1"},
    };
    UErrorCode status = U_ZERO_ERROR;
    NumberFormat* foo = NumberFormat::createCurrencyInstance(status);
    if (U_FAILURE(status)) {
        delete foo;
        return;
    }
    for (uint32_t i = 0; i < sizeof(DATA)/sizeof(DATA[0]); ++i) {
        ParsePosition parsePosition(0);
        UnicodeString stringToBeParsed = ctou(DATA[i][0]);
        int parsedPosition = atoi(DATA[i][1]);
        int errorIndex = atoi(DATA[i][2]);
        Formattable result;
        foo->parse(stringToBeParsed, result, parsePosition);
        if (parsePosition.getIndex() != parsedPosition ||
            parsePosition.getErrorIndex() != errorIndex) {
            errln("FAILED parse " + stringToBeParsed + "; wrong position, expected: (" + parsedPosition + ", " + errorIndex + "); got (" + parsePosition.getIndex() + ", " + parsePosition.getErrorIndex() + ")");
        }
        if (parsePosition.getErrorIndex() == -1 &&
            result.getType() == Formattable::kLong && 
            result.getLong() != 124) {
            errln("FAILED parse " + stringToBeParsed + "; wrong number, expect: 124, got " + result.getLong());
        }
    }
    delete foo;
}

/**
 * Test using various numbering systems and numbering system keyword.
 */
void NumberFormatTest::TestNumberingSystems() {
    UErrorCode ec = U_ZERO_ERROR;

    Locale loc1("en", "US", "", "numbers=thai");
    Locale loc2("en", "US", "", "numbers=hebrew");
    Locale loc3("en", "US", "", "numbers=persian");
    Locale loc4("en", "US", "", "numbers=foobar");

    NumberFormat* fmt1= NumberFormat::createInstance(loc1, ec);
    if (U_FAILURE(ec)) {
        errln("FAIL: getInstance(en_US@numbers=thai)");
    }
    NumberFormat* fmt2= NumberFormat::createInstance(loc2, ec);
    if (U_FAILURE(ec)) {
        errln("FAIL: getInstance(en_US@numbers=hebrew)");
    }
    NumberFormat* fmt3= NumberFormat::createInstance(loc3, ec);
    if (U_FAILURE(ec)) {
        errln("FAIL: getInstance(en_US@numbers=persian)");
    }

    expect2(*fmt1, 1234.567, CharsToUnicodeString("\\u0E51,\\u0E52\\u0E53\\u0E54.\\u0E55\\u0E56\\u0E57"));
    expect3(*fmt2, 5678.0, CharsToUnicodeString("\\u05D4\\u05F3\\u05EA\\u05E8\\u05E2\\u05F4\\u05D7"));
    expect2(*fmt3, 1234.567, CharsToUnicodeString("\\u06F1,\\u06F2\\u06F3\\u06F4.\\u06F5\\u06F6\\u06F7"));

    // Test bogus keyword value
    NumberFormat* fmt4= NumberFormat::createInstance(loc4, ec);
    if ( ec != U_UNSUPPORTED_ERROR ) {
        errln("FAIL: getInstance(en_US@numbers=foobar) should have returned U_UNSUPPORTED_ERROR");
    }

    delete fmt1;
    delete fmt2;
    delete fmt3;
}


void 
NumberFormatTest::TestMultiCurrencySign() {
    const char* DATA[][6] = {
        // the fields in the following test are:
        // locale, 
        // currency pattern (with negative pattern), 
        // currency number to be formatted,
        // currency format using currency symbol name, such as "$" for USD,
        // currency format using currency ISO name, such as "USD",
        // currency format using plural name, such as "US dollars".
        // for US locale
        {"en_US", "\\u00A4#,##0.00;-\\u00A4#,##0.00", "1234.56", "$1,234.56", "USD1,234.56", "US dollars1,234.56"}, 
        {"en_US", "\\u00A4#,##0.00;-\\u00A4#,##0.00", "-1234.56", "-$1,234.56", "-USD1,234.56", "-US dollars1,234.56"}, 
        {"en_US", "\\u00A4#,##0.00;-\\u00A4#,##0.00", "1", "$1.00", "USD1.00", "US dollar1.00"}, 
        // for CHINA locale
        {"zh_CN", "\\u00A4#,##0.00;(\\u00A4#,##0.00)", "1234.56", "\\uFFE51,234.56", "CNY1,234.56", "\\u4EBA\\u6C11\\u5E011,234.56"},
        {"zh_CN", "\\u00A4#,##0.00;(\\u00A4#,##0.00)", "-1234.56", "(\\uFFE51,234.56)", "(CNY1,234.56)", "(\\u4EBA\\u6C11\\u5E011,234.56)"},
        {"zh_CN", "\\u00A4#,##0.00;(\\u00A4#,##0.00)", "1", "\\uFFE51.00", "CNY1.00", "\\u4EBA\\u6C11\\u5E011.00"}
    };

    const UChar doubleCurrencySign[] = {0xA4, 0xA4, 0};
    UnicodeString doubleCurrencyStr(doubleCurrencySign);
    const UChar tripleCurrencySign[] = {0xA4, 0xA4, 0xA4, 0};
    UnicodeString tripleCurrencyStr(tripleCurrencySign);

    for (uint32_t i=0; i<sizeof(DATA)/sizeof(DATA[0]); ++i) {
        const char* locale = DATA[i][0];
        UnicodeString pat = ctou(DATA[i][1]);
        double numberToBeFormat = atof(DATA[i][2]);
        UErrorCode status = U_ZERO_ERROR;
        DecimalFormatSymbols* sym = new DecimalFormatSymbols(Locale(locale), status);
        if (U_FAILURE(status)) {
            delete sym;
            continue;
        }
        for (int j=1; j<=3; ++j) {
            // j represents the number of currency sign in the pattern.
            if (j == 2) {
                pat = pat.findAndReplace(ctou("\\u00A4"), doubleCurrencyStr);
            } else if (j == 3) {
                pat = pat.findAndReplace(ctou("\\u00A4\\u00A4"), tripleCurrencyStr);
            }

            DecimalFormat* fmt = new DecimalFormat(pat, new DecimalFormatSymbols(*sym), status);
            if (U_FAILURE(status)) {
                errln("FAILED init DecimalFormat ");
                delete fmt;
                continue;
            }
            UnicodeString s;
            ((NumberFormat*) fmt)->format(numberToBeFormat, s);
            // DATA[i][3] is the currency format result using a
            // single currency sign.
            // DATA[i][4] is the currency format result using
            // double currency sign.
            // DATA[i][5] is the currency format result using
            // triple currency sign.
            // DATA[i][j+2] is the currency format result using
            // 'j' number of currency sign.
            UnicodeString currencyFormatResult = ctou(DATA[i][2+j]);
            if (s.compare(currencyFormatResult)) {
                errln("FAIL format: Expected " + currencyFormatResult + "; Got " + s);
            }
            // mix style parsing
            for (int k=3; k<=5; ++k) {
              // DATA[i][3] is the currency format result using a
              // single currency sign.
              // DATA[i][4] is the currency format result using
              // double currency sign.
              // DATA[i][5] is the currency format result using
              // triple currency sign.
              UnicodeString oneCurrencyFormat = ctou(DATA[i][k]);
              UErrorCode status = U_ZERO_ERROR;
              Formattable parseRes;
              fmt->parse(oneCurrencyFormat, parseRes, status);
              if (U_FAILURE(status) ||
                  (parseRes.getType() == Formattable::kDouble &&
                   parseRes.getDouble() != numberToBeFormat) || 
                  (parseRes.getType() == Formattable::kLong &&
                   parseRes.getLong() != numberToBeFormat)) {
                  errln("FAILED parse " + oneCurrencyFormat + "; (i, j, k): " +
                        i + ", " + j + ", " + k);
              }
            }
            delete fmt;
        }
        delete sym;
    }
}


void 
NumberFormatTest::TestCurrencyFormatForMixParsing() {
    UErrorCode status = U_ZERO_ERROR;
    MeasureFormat* curFmt = MeasureFormat::createCurrencyFormat(Locale("en_US"), status);
    if (U_FAILURE(status)) {
        delete curFmt;
        return;
    }
    const char* formats[] = {
        "$1,234.56",  // string to be parsed
        "USD1,234.56",
        "US dollars1,234.56",
        "1,234.56 US dollars"
    };
    for (uint32_t i = 0; i < sizeof(formats)/sizeof(formats[0]); ++i) {
        UnicodeString stringToBeParsed = ctou(formats[i]);
        Formattable result;
        UErrorCode status = U_ZERO_ERROR;
        curFmt->parseObject(stringToBeParsed, result, status);
        if (U_FAILURE(status)) {
            errln("FAIL: measure format parsing");
        } 
        if (result.getType() != Formattable::kObject || 
            result.getObject()->getDynamicClassID() != CurrencyAmount::getStaticClassID() ||
            ((CurrencyAmount*)result.getObject())->getNumber().getDouble() != 1234.56 ||
            UnicodeString(((CurrencyAmount*)result.getObject())->getISOCurrency()).compare(ISO_CURRENCY_USD)) {
            errln("FAIL: getCurrencyFormat of default locale (en_US) failed roundtripping the number ");
            if (((CurrencyAmount*)result.getObject())->getNumber().getDouble() != 1234.56) {
                errln((UnicodeString)"wong number, expect: 1234.56" + ", got: " + ((CurrencyAmount*)result.getObject())->getNumber().getDouble());
            }
            if (((CurrencyAmount*)result.getObject())->getISOCurrency() != ISO_CURRENCY_USD) {
                errln((UnicodeString)"wong currency, expect: USD" + ", got: " + ((CurrencyAmount*)result.getObject())->getISOCurrency());
            }
        }
    }
    delete curFmt;
}


void 
NumberFormatTest::TestDecimalFormatCurrencyParse() {
    // Locale.US
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormatSymbols* sym = new DecimalFormatSymbols(Locale("en_US"), status);
    if (U_FAILURE(status)) {
        delete sym;
        return;
    }
    UnicodeString pat;
    UChar currency = 0x00A4;
    // "\xA4#,##0.00;-\xA4#,##0.00"
    pat.append(currency).append(currency).append(currency).append("#,##0.00;-").append(currency).append(currency).append(currency).append("#,##0.00");
    DecimalFormat* fmt = new DecimalFormat(pat, sym, status);
    if (U_FAILURE(status)) {
        delete fmt;
        errln("failed to new DecimalFormat in TestDecimalFormatCurrencyParse");
        return;
    }
    const char* DATA[][2] = {
        // the data are:
        // string to be parsed, the parsed result (number)
        {"$1.00", "1"},    
        {"USD1.00", "1"},    
        {"1.00 US dollar", "1"},    
        {"$1,234.56", "1234.56"},    
        {"USD1,234.56", "1234.56"},    
        {"1,234.56 US dollar", "1234.56"},    
    };
    for (uint32_t i = 0; i < sizeof(DATA)/sizeof(DATA[0]); ++i) {
        UnicodeString stringToBeParsed = ctou(DATA[i][0]);
        double parsedResult = atof(DATA[i][1]);
        UErrorCode status = U_ZERO_ERROR;
        Formattable result;
        fmt->parse(stringToBeParsed, result, status);
        if (U_FAILURE(status) ||
            (result.getType() == Formattable::kDouble &&
            result.getDouble() != parsedResult) ||
            (result.getType() == Formattable::kLong &&
            result.getLong() != parsedResult)) {
            errln((UnicodeString)"FAIL parse: Expected " + parsedResult);
        }
    }
    delete fmt;
} 


void 
NumberFormatTest::TestCurrencyIsoPluralFormat() {
    const char* DATA[][6] = {
        // the data are:
        // locale, 
        // currency amount to be formatted,
        // currency ISO code to be formatted,
        // format result using CURRENCYSTYLE,
        // format result using ISOCURRENCYSTYLE,
        // format result using PLURALCURRENCYSTYLE,
        {"en_US", "1", "USD", "$1.00", "USD1.00", "1.00 US dollar"},
        {"en_US", "1234.56", "USD", "$1,234.56", "USD1,234.56", "1,234.56 US dollars"},
        {"en_US", "-1234.56", "USD", "($1,234.56)", "(USD1,234.56)", "-1,234.56 US dollars"},
        {"zh_CN", "1", "USD", "US$1.00", "USD1.00", "1.00 \\u7F8E\\u5143"}, 
        {"zh_CN", "1234.56", "USD", "US$1,234.56", "USD1,234.56", "1,234.56 \\u7F8E\\u5143"},
        // wrong ISO code {"zh_CN", "1", "CHY", "CHY1.00", "CHY1.00", "1.00 CHY"},
        // wrong ISO code {"zh_CN", "1234.56", "CHY", "CHY1,234.56", "CHY1,234.56", "1,234.56 CHY"},
        {"zh_CN", "1", "CNY", "\\uFFE51.00", "CNY1.00", "1.00 \\u4EBA\\u6C11\\u5E01"},
        {"zh_CN", "1234.56", "CNY", "\\uFFE51,234.56", "CNY1,234.56", "1,234.56 \\u4EBA\\u6C11\\u5E01"}, 
        {"ru_RU", "1", "RUB", "1,00\\u00A0\\u0440\\u0443\\u0431.", "1,00\\u00A0RUB", "1,00 \\u0420\\u043E\\u0441\\u0441\\u0438\\u0439\\u0441\\u043A\\u0438\\u0439 \\u0440\\u0443\\u0431\\u043B\\u044C"},
        {"ru_RU", "2", "RUB", "2,00\\u00A0\\u0440\\u0443\\u0431.", "2,00\\u00A0RUB", "2,00 \\u0420\\u043E\\u0441\\u0441\\u0438\\u0439\\u0441\\u043A\\u0438\\u0445 \\u0440\\u0443\\u0431\\u043B\\u044F"},
        {"ru_RU", "5", "RUB", "5,00\\u00A0\\u0440\\u0443\\u0431.", "5,00\\u00A0RUB", "5,00 \\u0420\\u043E\\u0441\\u0441\\u0438\\u0439\\u0441\\u043A\\u0438\\u0445 \\u0440\\u0443\\u0431\\u043B\\u0435\\u0439"},
        // test locale without currency information
        {"ti_ET", "-1.23", "USD", "-US$1.23", "-USD1.23", "-1.23 USD"},
        // test choice format
        {"es_AR", "1", "INR", "Re.\\u00A01,00", "INR\\u00A01,00", "1,00 rupia india"},
    };
    
    for (uint32_t i=0; i<sizeof(DATA)/sizeof(DATA[0]); ++i) {
      for (NumberFormat::EStyles k = NumberFormat::kCurrencyStyle;
           k <= NumberFormat::kPluralCurrencyStyle;
           k = (NumberFormat::EStyles)(k+1)) {
        // k represents currency format style.
        if ( k != NumberFormat::kCurrencyStyle &&
             k != NumberFormat::kIsoCurrencyStyle &&
             k != NumberFormat::kPluralCurrencyStyle ) {
            continue;
        }
        const char* localeString = DATA[i][0];
        double numberToBeFormat = atof(DATA[i][1]);
        const char* currencyISOCode = DATA[i][2];
        Locale locale(localeString);
        UErrorCode status = U_ZERO_ERROR;
        NumberFormat* numFmt = NumberFormat::createInstance(locale, k, status);
        if (U_FAILURE(status)) {
            delete numFmt;
            errln((UnicodeString)"can not create instance, locale:" + localeString + ", style: " + k);
            continue;
        }
        // TODO: need to be UChar*
        UChar currencyCode[4];
        currencyCode[0] = currencyISOCode[0];
        currencyCode[1] = currencyISOCode[1];
        currencyCode[2] = currencyISOCode[2];
        currencyCode[3] = currencyISOCode[3];
        numFmt->setCurrency(currencyCode, status);
        if (U_FAILURE(status)) {
            delete numFmt;
            errln((UnicodeString)"can not set currency:" + currencyISOCode);
            continue;
        }
       
        UnicodeString strBuf;
        numFmt->format(numberToBeFormat, strBuf);
        int resultDataIndex = k;
        if ( k == NumberFormat::kCurrencyStyle ) {
            resultDataIndex = k+2;
        }
        // DATA[i][resultDataIndex] is the currency format result
        // using 'k' currency style.
        UnicodeString formatResult = ctou(DATA[i][resultDataIndex]);
        if (strBuf.compare(formatResult)) {
            errln("FAIL: Expected " + formatResult + " actual: " + strBuf);
        }
        // test parsing, and test parsing for all currency formats.
        for (int j = 3; j < 6; ++j) {
            // DATA[i][3] is the currency format result using 
            // CURRENCYSTYLE formatter.
            // DATA[i][4] is the currency format result using
            // ISOCURRENCYSTYLE formatter.
            // DATA[i][5] is the currency format result using
            // PLURALCURRENCYSTYLE formatter.
            UnicodeString oneCurrencyFormatResult = ctou(DATA[i][j]);
            UErrorCode status = U_ZERO_ERROR;
            Formattable parseResult;
            numFmt->parse(oneCurrencyFormatResult, parseResult, status);
            if (U_FAILURE(status) ||
                (parseResult.getType() == Formattable::kDouble &&
                 parseResult.getDouble() != numberToBeFormat) ||
                (parseResult.getType() == Formattable::kLong &&
                 parseResult.getLong() != numberToBeFormat)) {
                errln((UnicodeString)"FAIL: getCurrencyFormat of locale " + 
                      localeString + " failed roundtripping the number");
                if (parseResult.getType() == Formattable::kDouble) {
                    errln((UnicodeString)"expected: " + numberToBeFormat + "; actual: " +parseResult.getDouble());
                } else {
                    errln((UnicodeString)"expected: " + numberToBeFormat + "; actual: " +parseResult.getLong());
                }
            }
        }
        delete numFmt;
      }  
    }
}

void
NumberFormatTest::TestCurrencyParsing() {
    const char* DATA[][6] = {
        // the data are:
        // locale, 
        // currency amount to be formatted,
        // currency ISO code to be formatted,
        // format result using CURRENCYSTYLE,
        // format result using ISOCURRENCYSTYLE,
        // format result using PLURALCURRENCYSTYLE,
        {"en_US", "1", "USD", "$1.00", "USD1.00", "1.00 US dollar"},
        {"pa_PK", "1", "USD", "US$\\u00a0\\u0a67.\\u0a66\\u0a66", "USD\\u00a0\\u0a67.\\u0a66\\u0a66", "\\u0a67.\\u0a66\\u0a66 USD"},
        {"es_AR", "1", "USD", "US$\\u00a01,00", "USD\\u00a01,00", "1,00 d\\u00f3lar estadounidense"},
        {"ar_EG", "1", "USD", "US$\\u00a0\\u0661\\u066b\\u0660\\u0660", "USD\\u00a0\\u0661\\u066b\\u0660\\u0660", "\\u0661\\u066b\\u0660\\u0660 \\u062f\\u0648\\u0644\\u0627\\u0631 \\u0623\\u0645\\u0631\\u064a\\u0643\\u064a"},
        {"fa_CA", "1", "USD", "\\u06f1\\u066b\\u06f0\\u06f0\\u00a0US$", "\\u06f1\\u066b\\u06f0\\u06f0\\u00a0USD", "\\u06f1\\u066b\\u06f0\\u06f0\\u0020\\u062f\\u0644\\u0627\\u0631\\u0020\\u0627\\u0645\\u0631\\u06cc\\u06a9\\u0627"},
        {"he_IL", "1", "USD", "1.00\\u00a0US$", "1.00\\u00a0USD", "1.00 \\u05d3\\u05d5\\u05dc\\u05e8 \\u05d0\\u05de\\u05e8\\u05d9\\u05e7\\u05d0\\u05d9"},
        {"hr_HR", "1", "USD", "1,00\\u00a0US$", "1,00\\u00a0USD", "1,00 Ameri\\u010dki dolar"},
        {"id_ID", "1", "USD", "US$1,00", "USD1,00", "1,00 USD"},
        {"it_IT", "1", "USD", "US$\\u00a01,00", "USD\\u00a01,00", "1,00 Dollaro Statunitense"},
        {"ko_KR", "1", "USD", "US$1.00", "USD1.00", "1.00 \\ubbf8\\uad6d \\ub2ec\\ub7ec"},
        {"ja_JP", "1", "USD", "US$1.00", "USD1.00", "1.00 \\u7c73\\u30c9\\u30eb"},
        {"zh_CN", "1", "CNY", "\\uFFE51.00", "CNY1.00", "1.00 \\u4EBA\\u6C11\\u5E01"},
        {"zh_TW", "1", "CNY", "\\uFFE51.00", "CNY1.00", "1.00 \\u4eba\\u6c11\\u5e63"},
        {"ru_RU", "1", "RUB", "1,00\\u00A0\\u0440\\u0443\\u0431.", "1,00\\u00A0RUB", "1,00 \\u0420\\u043E\\u0441\\u0441\\u0438\\u0439\\u0441\\u043A\\u0438\\u0439 \\u0440\\u0443\\u0431\\u043B\\u044C"},
    };
    
#ifdef NUMFMTST_CACHE_DEBUG
int deadloop = 0;
for (;;) {
    printf("loop: %d\n", deadloop++);
#endif
    for (uint32_t i=0; i<sizeof(DATA)/sizeof(DATA[0]); ++i) {
      for (NumberFormat::EStyles k = NumberFormat::kCurrencyStyle;
           k <= NumberFormat::kPluralCurrencyStyle;
           k = (NumberFormat::EStyles)(k+1)) {
        // k represents currency format style.
        if ( k != NumberFormat::kCurrencyStyle &&
             k != NumberFormat::kIsoCurrencyStyle &&
             k != NumberFormat::kPluralCurrencyStyle ) {
            continue;
        }
        const char* localeString = DATA[i][0];
        double numberToBeFormat = atof(DATA[i][1]);
        const char* currencyISOCode = DATA[i][2];
        Locale locale(localeString);
        UErrorCode status = U_ZERO_ERROR;
        NumberFormat* numFmt = NumberFormat::createInstance(locale, k, status);
        if (U_FAILURE(status)) {
            delete numFmt;
            errln((UnicodeString)"can not create instance, locale:" + localeString + ", style: " + k);
            continue;
        }
        // TODO: need to be UChar*
        UChar currencyCode[4];
        currencyCode[0] = currencyISOCode[0];
        currencyCode[1] = currencyISOCode[1];
        currencyCode[2] = currencyISOCode[2];
        currencyCode[3] = currencyISOCode[3];
        numFmt->setCurrency(currencyCode, status);
        if (U_FAILURE(status)) {
            delete numFmt;
            errln((UnicodeString)"can not set currency:" + currencyISOCode);
            continue;
        }
   
        /*    
        UnicodeString strBuf;
        numFmt->format(numberToBeFormat, strBuf);
        int resultDataIndex = k;
        if ( k == NumberFormat::kCurrencyStyle ) {
            resultDataIndex = k+2;
        }
        // DATA[i][resultDataIndex] is the currency format result
        // using 'k' currency style.
        UnicodeString formatResult = ctou(DATA[i][resultDataIndex]);
        if (strBuf.compare(formatResult)) {
            errln("FAIL: Expected " + formatResult + " actual: " + strBuf);
        }
        */
        // test parsing, and test parsing for all currency formats.
        for (int j = 3; j < 6; ++j) {
            // DATA[i][3] is the currency format result using 
            // CURRENCYSTYLE formatter.
            // DATA[i][4] is the currency format result using
            // ISOCURRENCYSTYLE formatter.
            // DATA[i][5] is the currency format result using
            // PLURALCURRENCYSTYLE formatter.
            UnicodeString oneCurrencyFormatResult = ctou(DATA[i][j]);
            UErrorCode status = U_ZERO_ERROR;
            Formattable parseResult;
            numFmt->parse(oneCurrencyFormatResult, parseResult, status);
            if (U_FAILURE(status) ||
                (parseResult.getType() == Formattable::kDouble &&
                 parseResult.getDouble() != numberToBeFormat) ||
                (parseResult.getType() == Formattable::kLong &&
                 parseResult.getLong() != numberToBeFormat)) {
                errln((UnicodeString)"FAIL: getCurrencyFormat of locale " + 
                      localeString + " failed roundtripping the number" + 
                      "(i,k,j): " + i + ", " + k + ", " + j);
                if (parseResult.getType() == Formattable::kDouble) {
                    errln((UnicodeString)"expected: " + numberToBeFormat + "; actual: " +parseResult.getDouble());
                } else {
                    errln((UnicodeString)"expected: " + numberToBeFormat + "; actual: " +parseResult.getLong());
                }
            }
        }
        delete numFmt;
      }  
    }
#ifdef NUMFMTST_CACHE_DEBUG
}
#endif
}


void
NumberFormatTest::TestParseCurrencyInUCurr() {
    const char* DATA[] = {
        "$1.00",
        "USD1.00",
        "1.00 US dollar",
        "1.00 US dollars",
        "1.00 $",
        "1.00 A$",
        "1.00 ADP",
        "1.00 ADP",
        "1.00 AED",
        "1.00 AED",
        "1.00 AFA",
        "1.00 AFA",
        "1.00 AFN",
        "1.00 ALL",
        "1.00 AMD",
        "1.00 ANG",
        "1.00 AOA",
        "1.00 AOK",
        "1.00 AOK",
        "1.00 AON",
        "1.00 AON",
        "1.00 AOR",
        "1.00 AOR",
        "1.00 AR$",
        "1.00 ARA",
        "1.00 ARA",
        "1.00 ARP",
        "1.00 ARP",
        "1.00 ARS",
        "1.00 ATS",
        "1.00 ATS",
        "1.00 AUD",
        "1.00 AWG",
        "1.00 AZM",
        "1.00 AZM",
        "1.00 AZN",
        "1.00 Af",
        "1.00 Afghani (1927-2002)",
        "1.00 Afghani (AFA)",
        "1.00 Afghani",
        "1.00 Afghani",
        "1.00 Afghanis (AFA)",
        "1.00 Afghanis",
        "1.00 Afl.",
        "1.00 Albanian Lek",
        "1.00 Albanian lek",
        "1.00 Albanian lekë",
        "1.00 Algerian Dinar",
        "1.00 Algerian dinar",
        "1.00 Algerian dinars",
        "1.00 Andorran Peseta",
        "1.00 Andorran peseta",
        "1.00 Andorran pesetas",
        "1.00 Angolan Kwanza (1977-1990)",
        "1.00 Angolan Kwanza Reajustado (1995-1999)",
        "1.00 Angolan Kwanza",
        "1.00 Angolan New Kwanza (1990-2000)",
        "1.00 Angolan kwanza (AOK)",
        "1.00 Angolan kwanza reajustado (AOR)",
        "1.00 Angolan kwanza",
        "1.00 Angolan kwanzas (AOK)",
        "1.00 Angolan kwanzas reajustado (AOR)",
        "1.00 Angolan kwanzas",
        "1.00 Angolan new kwanza (AON)",
        "1.00 Angolan new kwanzas (AON)",
        "1.00 Argentine Austral",
        "1.00 Argentine Peso (1983-1985)",
        "1.00 Argentine Peso",
        "1.00 Argentine austral",
        "1.00 Argentine australs",
        "1.00 Argentine peso (ARP)",
        "1.00 Argentine peso",
        "1.00 Argentine pesos (ARP)",
        "1.00 Argentine pesos",
        "1.00 Armenian Dram",
        "1.00 Armenian dram",
        "1.00 Armenian drams",
        "1.00 Aruban Florin",
        "1.00 Aruban florin",
        "1.00 Australian Dollar",
        "1.00 Australian dollar",
        "1.00 Australian dollars",
        "1.00 Austrian Schilling",
        "1.00 Austrian schilling",
        "1.00 Austrian schillings",
        "1.00 Azerbaijanian Manat (1993-2006)",
        "1.00 Azerbaijanian Manat",
        "1.00 Azerbaijanian manat (AZM)",
        "1.00 Azerbaijanian manat",
        "1.00 Azerbaijanian manats (AZM)",
        "1.00 Azerbaijanian manats",
        "1.00 B$",
        "1.00 BAD",
        "1.00 BAD",
        "1.00 BAM",
        "1.00 BBD",
        "1.00 BD",
        "1.00 BD$",
        "1.00 BDT",
        "1.00 BEC",
        "1.00 BEC",
        "1.00 BEF",
        "1.00 BEL",
        "1.00 BEL",
        "1.00 BF",
        "1.00 BGL",
        "1.00 BGN",
        "1.00 BGN",
        "1.00 BHD",
        "1.00 BIF",
        "1.00 BMD",
        "1.00 BND",
        "1.00 BOB",
        "1.00 BOP",
        "1.00 BOP",
        "1.00 BOV",
        "1.00 BOV",
        "1.00 BRB",
        "1.00 BRB",
        "1.00 BRC",
        "1.00 BRC",
        "1.00 BRE",
        "1.00 BRE",
        "1.00 BRL",
        "1.00 BRN",
        "1.00 BRN",
        "1.00 BRR",
        "1.00 BRR",
        "1.00 BSD",
        "1.00 BSD",
        "1.00 BTN",
        "1.00 BUK",
        "1.00 BUK",
        "1.00 BWP",
        "1.00 BYB",
        "1.00 BYB",
        "1.00 BYR",
        "1.00 BZ$",
        "1.00 BZD",
        "1.00 Bahamian Dollar",
        "1.00 Bahamian dollar",
        "1.00 Bahamian dollars",
        "1.00 Bahraini Dinar",
        "1.00 Bahraini dinar",
        "1.00 Bahraini dinars",
        "1.00 Bangladeshi Taka",
        "1.00 Bangladeshi taka",
        "1.00 Bangladeshi takas",
        "1.00 Barbados Dollar",
        "1.00 Barbados dollar",
        "1.00 Barbados dollars",
        "1.00 Bds$",
        "1.00 Be",
        "1.00 Belarussian New Ruble (1994-1999)",
        "1.00 Belarussian Ruble",
        "1.00 Belarussian new ruble (BYB)",
        "1.00 Belarussian new rubles (BYB)",
        "1.00 Belarussian ruble",
        "1.00 Belarussian rubles",
        "1.00 Belgian Franc (convertible)",
        "1.00 Belgian Franc (financial)",
        "1.00 Belgian Franc",
        "1.00 Belgian franc (convertible)",
        "1.00 Belgian franc (financial)",
        "1.00 Belgian franc",
        "1.00 Belgian francs (convertible)",
        "1.00 Belgian francs (financial)",
        "1.00 Belgian francs",
        "1.00 Belize Dollar",
        "1.00 Belize dollar",
        "1.00 Belize dollars",
        "1.00 Bermudan Dollar",
        "1.00 Bermudan dollar",
        "1.00 Bermudan dollars",
        "1.00 Bhutan Ngultrum",
        "1.00 Bhutan ngultrum",
        "1.00 Bhutan ngultrums",
        "1.00 Bolivian Mvdol",
        "1.00 Bolivian Peso",
        "1.00 Bolivian mvdol",
        "1.00 Bolivian mvdols",
        "1.00 Bolivian peso",
        "1.00 Bolivian pesos",
        "1.00 Boliviano",
        "1.00 Boliviano",
        "1.00 Bolivianos",
        "1.00 Bosnia-Herzegovina Convertible Mark",
        "1.00 Bosnia-Herzegovina Dinar",
        "1.00 Bosnia-Herzegovina convertible mark",
        "1.00 Bosnia-Herzegovina convertible marks",
        "1.00 Bosnia-Herzegovina dinar",
        "1.00 Bosnia-Herzegovina dinars",
        "1.00 Botswanan Pula",
        "1.00 Botswanan pula",
        "1.00 Botswanan pulas",
        "1.00 Br",
        "1.00 Brazilian Cruzado Novo",
        "1.00 Brazilian Cruzado",
        "1.00 Brazilian Cruzeiro (1990-1993)",
        "1.00 Brazilian Cruzeiro Novo (1967-1986)",
        "1.00 Brazilian Cruzeiro",
        "1.00 Brazilian Real",
        "1.00 Brazilian cruzado novo",
        "1.00 Brazilian cruzado novos",
        "1.00 Brazilian cruzado",
        "1.00 Brazilian cruzados",
        "1.00 Brazilian cruzeiro (BRE)",
        "1.00 Brazilian cruzeiro novo (BRB)",
        "1.00 Brazilian cruzeiro",
        "1.00 Brazilian cruzeiros (BRE)",
        "1.00 Brazilian cruzeiros novo (BRB)",
        "1.00 Brazilian cruzeiros",
        "1.00 Brazilian real",
        "1.00 Brazilian reals",
        "1.00 British Pound Sterling",
        "1.00 British pound sterling",
        "1.00 British pound sterlings",
        "1.00 Brunei Dollar",
        "1.00 Brunei dollar",
        "1.00 Brunei dollars",
        "1.00 Bs",
        "1.00 BsF",
        "1.00 Bulgarian Hard Lev",
        "1.00 Bulgarian Lev",
        "1.00 Bulgarian Levs",
        "1.00 Bulgarian hard lev",
        "1.00 Bulgarian hard levs",
        "1.00 Bulgarian lev",
        "1.00 Burmese Kyat",
        "1.00 Burmese kyat",
        "1.00 Burmese kyats",
        "1.00 Burundi Franc",
        "1.00 Burundi franc",
        "1.00 Burundi francs",
        "1.00 C$",
        "1.00 CA$",
        "1.00 CAD",
        "1.00 CDF",
        "1.00 CDF",
        "1.00 CF",
        "1.00 CFA Franc BCEAO",
        "1.00 CFA Franc BEAC",
        "1.00 CFA franc BCEAO",
        "1.00 CFA franc BEAC",
        "1.00 CFA francs BCEAO",
        "1.00 CFA francs BEAC",
        "1.00 CFP Franc",
        "1.00 CFP franc",
        "1.00 CFP francs",
        "1.00 CFPF",
        "1.00 CHE",
        "1.00 CHE",
        "1.00 CHF",
        "1.00 CHW",
        "1.00 CHW",
        "1.00 CL$",
        "1.00 CLF",
        "1.00 CLF",
        "1.00 CLP",
        "1.00 CNY",
        "1.00 CO$",
        "1.00 COP",
        "1.00 COU",
        "1.00 COU",
        "1.00 CR",
        "1.00 CRC",
        "1.00 CSD",
        "1.00 CSD",
        "1.00 CSK",
        "1.00 CSK",
        "1.00 CUP",
        "1.00 CUP",
        "1.00 CVE",
        "1.00 CYP",
        "1.00 CZK",
        "1.00 Cambodian Riel",
        "1.00 Cambodian riel",
        "1.00 Cambodian riels",
        "1.00 Canadian Dollar",
        "1.00 Canadian dollar",
        "1.00 Canadian dollars",
        "1.00 Cape Verde Escudo",
        "1.00 Cape Verde escudo",
        "1.00 Cape Verde escudos",
        "1.00 Cayman Islands Dollar",
        "1.00 Cayman Islands dollar",
        "1.00 Cayman Islands dollars",
        "1.00 Chilean Peso",
        "1.00 Chilean Unidades de Fomento",
        "1.00 Chilean peso",
        "1.00 Chilean pesos",
        "1.00 Chilean unidades de fomento",
        "1.00 Chilean unidades de fomentos",
        "1.00 Chinese Yuan Renminbi",
        "1.00 Chinese yuan",
        "1.00 Colombian Peso",
        "1.00 Colombian peso",
        "1.00 Colombian pesos",
        "1.00 Comoro Franc",
        "1.00 Comoro franc",
        "1.00 Comoro francs",
        "1.00 Congolese Franc Congolais",
        "1.00 Congolese franc Congolais",
        "1.00 Congolese francs Congolais",
        "1.00 Costa Rican Colon",
        "1.00 Costa Rican colon",
        "1.00 Costa Rican colons",
        "1.00 Croatian Dinar",
        "1.00 Croatian Kuna",
        "1.00 Croatian dinar",
        "1.00 Croatian dinars",
        "1.00 Croatian kuna",
        "1.00 Croatian kunas",
        "1.00 Cuban Peso",
        "1.00 Cuban peso",
        "1.00 Cuban pesos",
        "1.00 Cyprus Pound",
        "1.00 Cyprus pound",
        "1.00 Cyprus pounds",
        "1.00 Czech Republic Koruna",
        "1.00 Czech Republic koruna",
        "1.00 Czech Republic korunas",
        "1.00 Czechoslovak Hard Koruna",
        "1.00 Czechoslovak hard koruna",
        "1.00 Czechoslovak hard korunas",
        "1.00 DA",
        "1.00 DDM",
        "1.00 DDM",
        "1.00 DEM",
        "1.00 DEM",
        "1.00 DJF",
        "1.00 DKK",
        "1.00 DOP",
        "1.00 DZD",
        "1.00 Danish Krone",
        "1.00 Danish krone",
        "1.00 Danish kroner",
        "1.00 Db",
        "1.00 Deutsche Mark",
        "1.00 Deutsche mark",
        "1.00 Deutsche marks",
        "1.00 Djibouti Franc",
        "1.00 Djibouti franc",
        "1.00 Djibouti francs",
        "1.00 Dkr",
        "1.00 Dominican Peso",
        "1.00 Dominican peso",
        "1.00 Dominican pesos",
        "1.00 E",
        "1.00 EC$",
        "1.00 ECS",
        "1.00 ECS",
        "1.00 ECV",
        "1.00 ECV",
        "1.00 EEK",
        "1.00 EEK",
        "1.00 EGP",
        "1.00 EGP",
        "1.00 EQE",
        "1.00 EQE",
        "1.00 ERN",
        "1.00 ERN",
        "1.00 ESA",
        "1.00 ESA",
        "1.00 ESB",
        "1.00 ESB",
        "1.00 ESP",
        "1.00 ETB",
        "1.00 EUR",
        "1.00 East Caribbean Dollar",
        "1.00 East Caribbean dollar",
        "1.00 East Caribbean dollars",
        "1.00 East German Ostmark",
        "1.00 East German ostmark",
        "1.00 East German ostmarks",
        "1.00 Ecuador Sucre",
        "1.00 Ecuador Unidad de Valor Constante (UVC)",
        "1.00 Ecuador sucre",
        "1.00 Ecuador sucres",
        "1.00 Ecuador unidad de valor Constante (UVC)",
        "1.00 Ecuador unidads de valor Constante (UVC)",
        "1.00 Egyptian Pound",
        "1.00 Egyptian pound",
        "1.00 Egyptian pounds",
        "1.00 Ekwele",
        "1.00 El Salvador Colon",
        "1.00 El Salvador colon",
        "1.00 El Salvador colons",
        "1.00 Equatorial Guinea Ekwele Guineana",
        "1.00 Equatorial Guinea ekwele",
        "1.00 Eritrean Nakfa",
        "1.00 Eritrean nakfa",
        "1.00 Eritrean nakfas",
        "1.00 Esc",
        "1.00 Estonian Kroon",
        "1.00 Estonian kroon",
        "1.00 Estonian kroons",
        "1.00 Ethiopian Birr",
        "1.00 Ethiopian birr",
        "1.00 Ethiopian birrs",
        "1.00 Euro",
        "1.00 European Composite Unit",
        "1.00 European Currency Unit",
        "1.00 European Monetary Unit",
        "1.00 European Unit of Account (XBC)",
        "1.00 European Unit of Account (XBD)",
        "1.00 European composite unit",
        "1.00 European composite units",
        "1.00 European currency unit",
        "1.00 European currency units",
        "1.00 European monetary unit",
        "1.00 European monetary units",
        "1.00 European unit of account (XBC)",
        "1.00 European unit of account (XBD)",
        "1.00 European units of account (XBC)",
        "1.00 European units of account (XBD)",
        "1.00 F$",
        "1.00 FBu",
        "1.00 FIM",
        "1.00 FIM",
        "1.00 FJD",
        "1.00 FKP",
        "1.00 FKP",
        "1.00 FRF",
        "1.00 FRF",
        "1.00 Falkland Islands Pound",
        "1.00 Falkland Islands pound",
        "1.00 Falkland Islands pounds",
        "1.00 Fdj",
        "1.00 Fiji Dollar",
        "1.00 Fiji dollar",
        "1.00 Fiji dollars",
        "1.00 Finnish Markka",
        "1.00 Finnish markka",
        "1.00 Finnish markkas",
        "1.00 Fr.",
        "1.00 French Franc",
        "1.00 French Gold Franc",
        "1.00 French UIC-Franc",
        "1.00 French UIC-franc",
        "1.00 French UIC-francs",
        "1.00 French franc",
        "1.00 French francs",
        "1.00 French gold franc",
        "1.00 French gold francs",
        "1.00 Ft",
        "1.00 G$",
        "1.00 GBP",
        "1.00 GEK",
        "1.00 GEK",
        "1.00 GEL",
        "1.00 GF",
        "1.00 GHC",
        "1.00 GHC",
        "1.00 GHS",
        "1.00 GIP",
        "1.00 GIP",
        "1.00 GMD",
        "1.00 GMD",
        "1.00 GNF",
        "1.00 GNS",
        "1.00 GNS",
        "1.00 GQE",
        "1.00 GQE",
        "1.00 GRD",
        "1.00 GRD",
        "1.00 GTQ",
        "1.00 GWE",
        "1.00 GWE",
        "1.00 GWP",
        "1.00 GWP",
        "1.00 GYD",
        "1.00 Gambia Dalasi",
        "1.00 Gambia dalasi",
        "1.00 Gambia dalasis",
        "1.00 Georgian Kupon Larit",
        "1.00 Georgian Lari",
        "1.00 Georgian kupon larit",
        "1.00 Georgian kupon larits",
        "1.00 Georgian lari",
        "1.00 Georgian laris",
        "1.00 Ghana Cedi (1979-2007)",
        "1.00 Ghana Cedi",
        "1.00 Ghana cedi (GHC)",
        "1.00 Ghana cedi",
        "1.00 Ghana cedis (GHC)",
        "1.00 Ghana cedis",
        "1.00 Gibraltar Pound",
        "1.00 Gibraltar pound",
        "1.00 Gibraltar pounds",
        "1.00 Gold",
        "1.00 Gold",
        "1.00 Greek Drachma",
        "1.00 Greek drachma",
        "1.00 Greek drachmas",
        "1.00 Guatemala Quetzal",
        "1.00 Guatemala quetzal",
        "1.00 Guatemala quetzals",
        "1.00 Guinea Franc",
        "1.00 Guinea Syli",
        "1.00 Guinea franc",
        "1.00 Guinea francs",
        "1.00 Guinea syli",
        "1.00 Guinea sylis",
        "1.00 Guinea-Bissau Peso",
        "1.00 Guinea-Bissau peso",
        "1.00 Guinea-Bissau pesos",
        "1.00 Guyana Dollar",
        "1.00 Guyana dollar",
        "1.00 Guyana dollars",
        "1.00 HK$",
        "1.00 HKD",
        "1.00 HNL",
        "1.00 HRD",
        "1.00 HRD",
        "1.00 HRK",
        "1.00 HRK",
        "1.00 HTG",
        "1.00 HTG",
        "1.00 HUF",
        "1.00 Haitian Gourde",
        "1.00 Haitian gourde",
        "1.00 Haitian gourdes",
        "1.00 Honduras Lempira",
        "1.00 Honduras lempira",
        "1.00 Honduras lempiras",
        "1.00 Hong Kong Dollar",
        "1.00 Hong Kong dollar",
        "1.00 Hong Kong dollars",
        "1.00 Hungarian Forint",
        "1.00 Hungarian forint",
        "1.00 Hungarian forints",
        "1.00 ID",
        "1.00 IDR",
        "1.00 IEP",
        "1.00 ILP",
        "1.00 ILP",
        "1.00 ILS",
        "1.00 INR",
        "1.00 IQD",
        "1.00 IRR",
        "1.00 IR\\u00a3",
        "1.00 ISK",
        "1.00 ISK",
        "1.00 ITL",
        "1.00 Icelandic Krona",
        "1.00 Icelandic krona",
        "1.00 Icelandic kronas",
        "1.00 Indian Rupee",
        "1.00 Indian rupee",
        "1.00 Indian rupees",
        "1.00 Indonesian Rupiah",
        "1.00 Indonesian rupiah",
        "1.00 Indonesian rupiahs",
        "1.00 Iranian Rial",
        "1.00 Iranian rial",
        "1.00 Iranian rials",
        "1.00 Iraqi Dinar",
        "1.00 Iraqi dinar",
        "1.00 Iraqi dinars",
        "1.00 Irish Pound",
        "1.00 Irish pound",
        "1.00 Irish pounds",
        "1.00 Israeli Pound",
        "1.00 Israeli new sheqel",
        "1.00 Israeli pound",
        "1.00 Israeli pounds",
        "1.00 Italian Lira",
        "1.00 Italian lira",
        "1.00 Italian liras",
        "1.00 J$",
        "1.00 JD",
        "1.00 JMD",
        "1.00 JOD",
        "1.00 JPY",
        "1.00 Jamaican Dollar",
        "1.00 Jamaican dollar",
        "1.00 Jamaican dollars",
        "1.00 Japanese Yen",
        "1.00 Japanese yen",
        "1.00 Jordanian Dinar",
        "1.00 Jordanian dinar",
        "1.00 Jordanian dinars",
        "1.00 K Sh",
        "1.00 KD",
        "1.00 KES",
        "1.00 KGS",
        "1.00 KHR",
        "1.00 KM",
        "1.00 KMF",
        "1.00 KPW",
        "1.00 KPW",
        "1.00 KRW",
        "1.00 KWD",
        "1.00 KYD",
        "1.00 KYD",
        "1.00 KZT",
        "1.00 Kazakhstan Tenge",
        "1.00 Kazakhstan tenge",
        "1.00 Kazakhstan tenges",
        "1.00 Kenyan Shilling",
        "1.00 Kenyan shilling",
        "1.00 Kenyan shillings",
        "1.00 Kuwaiti Dinar",
        "1.00 Kuwaiti dinar",
        "1.00 Kuwaiti dinars",
        "1.00 Kyrgystan Som",
        "1.00 Kyrgystan som",
        "1.00 Kyrgystan soms",
        "1.00 Kz",
        "1.00 K\\u010d",
        "1.00 L",
        "1.00 LAK",
        "1.00 LAK",
        "1.00 LBP",
        "1.00 LD",
        "1.00 LKR",
        "1.00 LL",
        "1.00 LRD",
        "1.00 LRD",
        "1.00 LS",
        "1.00 LSL",
        "1.00 LSM",
        "1.00 LSM",
        "1.00 LTL",
        "1.00 LTL",
        "1.00 LTT",
        "1.00 LTT",
        "1.00 LUC",
        "1.00 LUC",
        "1.00 LUF",
        "1.00 LUF",
        "1.00 LUL",
        "1.00 LUL",
        "1.00 LVL",
        "1.00 LVL",
        "1.00 LVR",
        "1.00 LVR",
        "1.00 LYD",
        "1.00 Laotian Kip",
        "1.00 Laotian kip",
        "1.00 Laotian kips",
        "1.00 Latvian Lats",
        "1.00 Latvian Ruble",
        "1.00 Latvian lats",
        "1.00 Latvian latses",
        "1.00 Latvian ruble",
        "1.00 Latvian rubles",
        "1.00 Lebanese Pound",
        "1.00 Lebanese pound",
        "1.00 Lebanese pounds",
        "1.00 Lesotho Loti",
        "1.00 Lesotho loti",
        "1.00 Lesotho lotis",
        "1.00 Liberian Dollar",
        "1.00 Liberian dollar",
        "1.00 Liberian dollars",
        "1.00 Libyan Dinar",
        "1.00 Libyan dinar",
        "1.00 Libyan dinars",
        "1.00 Lithuanian Lita",
        "1.00 Lithuanian Talonas",
        "1.00 Lithuanian lita",
        "1.00 Lithuanian litas",
        "1.00 Lithuanian talonas",
        "1.00 Lithuanian talonases",
        "1.00 Lm",
        "1.00 Luxembourg Convertible Franc",
        "1.00 Luxembourg Financial Franc",
        "1.00 Luxembourg Franc",
        "1.00 Luxembourg convertible franc",
        "1.00 Luxembourg convertible francs",
        "1.00 Luxembourg financial franc",
        "1.00 Luxembourg financial francs",
        "1.00 Luxembourg franc",
        "1.00 Luxembourg francs",
        "1.00 M",
        "1.00 MAD",
        "1.00 MAD",
        "1.00 MAF",
        "1.00 MAF",
        "1.00 MDL",
        "1.00 MDL",
        "1.00 MDen",
        "1.00 MEX$",
        "1.00 MGA",
        "1.00 MGA",
        "1.00 MGF",
        "1.00 MGF",
        "1.00 MK",
        "1.00 MKD",
        "1.00 MLF",
        "1.00 MLF",
        "1.00 MMK",
        "1.00 MMK",
        "1.00 MNT",
        "1.00 MOP",
        "1.00 MOP",
        "1.00 MRO",
        "1.00 MTL",
        "1.00 MTP",
        "1.00 MTP",
        "1.00 MTn",
        "1.00 MUR",
        "1.00 MUR",
        "1.00 MVR",
        "1.00 MVR",
        "1.00 MWK",
        "1.00 MXN",
        "1.00 MXP",
        "1.00 MXP",
        "1.00 MXV",
        "1.00 MXV",
        "1.00 MYR",
        "1.00 MZE",
        "1.00 MZE",
        "1.00 MZM",
        "1.00 MZN",
        "1.00 Macao Pataca",
        "1.00 Macao pataca",
        "1.00 Macao patacas",
        "1.00 Macedonian Denar",
        "1.00 Macedonian denar",
        "1.00 Macedonian denars",
        "1.00 Madagascar Ariaries",
        "1.00 Madagascar Ariary",
        "1.00 Madagascar Ariary",
        "1.00 Madagascar Franc",
        "1.00 Madagascar franc",
        "1.00 Madagascar francs",
        "1.00 Malawi Kwacha",
        "1.00 Malawi Kwacha",
        "1.00 Malawi Kwachas",
        "1.00 Malaysian Ringgit",
        "1.00 Malaysian ringgit",
        "1.00 Malaysian ringgits",
        "1.00 Maldive Islands Rufiyaa",
        "1.00 Maldive Islands rufiyaa",
        "1.00 Maldive Islands rufiyaas",
        "1.00 Mali Franc",
        "1.00 Mali franc",
        "1.00 Mali francs",
        "1.00 Maloti",
        "1.00 Maltese Lira",
        "1.00 Maltese Pound",
        "1.00 Maltese lira",
        "1.00 Maltese liras",
        "1.00 Maltese pound",
        "1.00 Maltese pounds",
        "1.00 Mauritania Ouguiya",
        "1.00 Mauritania ouguiya",
        "1.00 Mauritania ouguiyas",
        "1.00 Mauritius Rupee",
        "1.00 Mauritius rupee",
        "1.00 Mauritius rupees",
        "1.00 Mexican Peso",
        "1.00 Mexican Silver Peso (1861-1992)",
        "1.00 Mexican Unidad de Inversion (UDI)",
        "1.00 Mexican peso",
        "1.00 Mexican pesos",
        "1.00 Mexican silver peso (MXP)",
        "1.00 Mexican silver pesos (MXP)",
        "1.00 Mexican unidad de inversion (UDI)",
        "1.00 Mexican unidads de inversion (UDI)",
        "1.00 Moldovan Leu",
        "1.00 Moldovan leu",
        "1.00 Moldovan leus",
        "1.00 Mongolian Tugrik",
        "1.00 Mongolian tugrik",
        "1.00 Mongolian tugriks",
        "1.00 Moroccan Dirham",
        "1.00 Moroccan Franc",
        "1.00 Moroccan dirham",
        "1.00 Moroccan dirhams",
        "1.00 Moroccan franc",
        "1.00 Moroccan francs",
        "1.00 Mozambique Escudo",
        "1.00 Mozambique Metical",
        "1.00 Mozambique escudo",
        "1.00 Mozambique escudos",
        "1.00 Mozambique metical",
        "1.00 Mozambique meticals",
        "1.00 Mt",
        "1.00 Myanmar Kyat",
        "1.00 Myanmar kyat",
        "1.00 Myanmar kyats",
        "1.00 N$",
        "1.00 NAD",
        "1.00 NAf.",
        "1.00 NGN",
        "1.00 NIC",
        "1.00 NIO",
        "1.00 NIO",
        "1.00 NKr",
        "1.00 NLG",
        "1.00 NLG",
        "1.00 NOK",
        "1.00 NPR",
        "1.00 NT$",
        "1.00 NZ$",
        "1.00 NZD",
        "1.00 Namibia Dollar",
        "1.00 Namibia dollar",
        "1.00 Namibia dollars",
        "1.00 Nepalese Rupee",
        "1.00 Nepalese rupee",
        "1.00 Nepalese rupees",
        "1.00 Netherlands Antillan Guilder",
        "1.00 Netherlands Antillan guilder",
        "1.00 Netherlands Antillan guilders",
        "1.00 Netherlands Guilder",
        "1.00 Netherlands guilder",
        "1.00 Netherlands guilders",
        "1.00 New Israeli Sheqel",
        "1.00 New Israeli Sheqels",
        "1.00 New Zealand Dollar",
        "1.00 New Zealand dollar",
        "1.00 New Zealand dollars",
        "1.00 Nicaraguan Cordoba Oro",
        "1.00 Nicaraguan Cordoba",
        "1.00 Nicaraguan cordoba oro",
        "1.00 Nicaraguan cordoba oros",
        "1.00 Nicaraguan cordoba",
        "1.00 Nicaraguan cordobas",
        "1.00 Nigerian Naira",
        "1.00 Nigerian naira",
        "1.00 Nigerian nairas",
        "1.00 North Korean Won",
        "1.00 North Korean won",
        "1.00 North Korean wons",
        "1.00 Norwegian Krone",
        "1.00 Norwegian krone",
        "1.00 Norwegian krones",
        "1.00 Nrs",
        "1.00 Nu",
        "1.00 OMR",
        "1.00 Old Mozambique Metical",
        "1.00 Old Mozambique metical",
        "1.00 Old Mozambique meticals",
        "1.00 Old Romanian Lei",
        "1.00 Old Romanian Leu",
        "1.00 Old Romanian leu",
        "1.00 Old Serbian Dinar",
        "1.00 Old Serbian dinar",
        "1.00 Old Serbian dinars",
        "1.00 Old Sudanese Dinar",
        "1.00 Old Sudanese Pound",
        "1.00 Old Sudanese dinar",
        "1.00 Old Sudanese dinars",
        "1.00 Old Sudanese pound",
        "1.00 Old Sudanese pounds",
        "1.00 Old Turkish Lira",
        "1.00 Old Turkish Lira",
        "1.00 Oman Rial",
        "1.00 Oman rial",
        "1.00 Oman rials",
        "1.00 P",
        "1.00 PAB",
        "1.00 PAB",
        "1.00 PEI",
        "1.00 PEI",
        "1.00 PEN",
        "1.00 PEN",
        "1.00 PES",
        "1.00 PES",
        "1.00 PGK",
        "1.00 PGK",
        "1.00 PHP",
        "1.00 PKR",
        "1.00 PLN",
        "1.00 PLZ",
        "1.00 PLZ",
        "1.00 PTE",
        "1.00 PTE",
        "1.00 PYG",
        "1.00 Pakistan Rupee",
        "1.00 Pakistan rupee",
        "1.00 Pakistan rupees",
        "1.00 Palladium",
        "1.00 Palladium",
        "1.00 Panamanian Balboa",
        "1.00 Panamanian balboa",
        "1.00 Panamanian balboas",
        "1.00 Papua New Guinea Kina",
        "1.00 Papua New Guinea kina",
        "1.00 Papua New Guinea kinas",
        "1.00 Paraguay Guarani",
        "1.00 Paraguay guarani",
        "1.00 Paraguay guaranis",
        "1.00 Peruvian Inti",
        "1.00 Peruvian Sol Nuevo",
        "1.00 Peruvian Sol",
        "1.00 Peruvian inti",
        "1.00 Peruvian intis",
        "1.00 Peruvian sol nuevo",
        "1.00 Peruvian sol nuevos",
        "1.00 Peruvian sol",
        "1.00 Peruvian sols",
        "1.00 Philippine Peso",
        "1.00 Philippine peso",
        "1.00 Philippine pesos",
        "1.00 Platinum",
        "1.00 Platinum",
        "1.00 Polish Zloty (1950-1995)",
        "1.00 Polish Zloty",
        "1.00 Polish zloties",
        "1.00 Polish zloty (PLZ)",
        "1.00 Polish zloty",
        "1.00 Polish zlotys (PLZ)",
        "1.00 Portuguese Escudo",
        "1.00 Portuguese Guinea Escudo",
        "1.00 Portuguese Guinea escudo",
        "1.00 Portuguese Guinea escudos",
        "1.00 Portuguese escudo",
        "1.00 Portuguese escudos",
        "1.00 Pra",
        "1.00 Q",
        "1.00 QAR",
        "1.00 QR",
        "1.00 Qatari Rial",
        "1.00 Qatari rial",
        "1.00 Qatari rials",
        "1.00 R",
        "1.00 R$",
        "1.00 RD$",
        "1.00 RHD",
        "1.00 RHD",
        "1.00 RI",
        "1.00 RINET Funds",
        "1.00 RINET Funds",
        "1.00 RM",
        "1.00 RMB",
        "1.00 RO",
        "1.00 ROL",
        "1.00 ROL",
        "1.00 RON",
        "1.00 RON",
        "1.00 RSD",
        "1.00 RSD",
        "1.00 RUB",
        "1.00 RUB",
        "1.00 RUR",
        "1.00 RUR",
        "1.00 RWF",
        "1.00 RWF",
        "1.00 Rbl",
        "1.00 Rhodesian Dollar",
        "1.00 Rhodesian dollar",
        "1.00 Rhodesian dollars",
        "1.00 Romanian Leu",
        "1.00 Romanian lei",
        "1.00 Romanian leu",
        "1.00 Rp",
        "1.00 Russian Ruble (1991-1998)",
        "1.00 Russian Ruble",
        "1.00 Russian ruble (RUR)",
        "1.00 Russian ruble",
        "1.00 Russian rubles (RUR)",
        "1.00 Russian rubles",
        "1.00 Rwandan Franc",
        "1.00 Rwandan franc",
        "1.00 Rwandan francs",
        "1.00 S$",
        "1.00 SAR",
        "1.00 SBD",
        "1.00 SCR",
        "1.00 SDD",
        "1.00 SDD",
        "1.00 SDG",
        "1.00 SDG",
        "1.00 SDP",
        "1.00 SDP",
        "1.00 SEK",
        "1.00 SGD",
        "1.00 SHP",
        "1.00 SHP",
        "1.00 SI$",
        "1.00 SIT",
        "1.00 SIT",
        "1.00 SKK",
        "1.00 SKr",
        "1.00 SL Re",
        "1.00 SLL",
        "1.00 SLL",
        "1.00 SOS",
        "1.00 SR",
        "1.00 SRD",
        "1.00 SRD",
        "1.00 SRG",
        "1.00 SRl",
        "1.00 STD",
        "1.00 SUR",
        "1.00 SUR",
        "1.00 SVC",
        "1.00 SVC",
        "1.00 SYP",
        "1.00 SZL",
        "1.00 Saint Helena Pound",
        "1.00 Saint Helena pound",
        "1.00 Saint Helena pounds",
        "1.00 Sao Tome and Principe Dobra",
        "1.00 Sao Tome and Principe dobra",
        "1.00 Sao Tome and Principe dobras",
        "1.00 Saudi Riyal",
        "1.00 Saudi riyal",
        "1.00 Saudi riyals",
        "1.00 Serbian Dinar",
        "1.00 Serbian dinar",
        "1.00 Serbian dinars",
        "1.00 Seychelles Rupee",
        "1.00 Seychelles rupee",
        "1.00 Seychelles rupees",
        "1.00 Sf",
        "1.00 Sh.",
        "1.00 Sierra Leone Leone",
        "1.00 Sierra Leone leone",
        "1.00 Sierra Leone leones",
        "1.00 Silver",
        "1.00 Silver",
        "1.00 Singapore Dollar",
        "1.00 Singapore dollar",
        "1.00 Singapore dollars",
        "1.00 Sk",
        "1.00 Slovak Koruna",
        "1.00 Slovak koruna",
        "1.00 Slovak korunas",
        "1.00 Slovenia Tolar",
        "1.00 Slovenia tolar",
        "1.00 Slovenia tolars",
        "1.00 Solomon Islands Dollar",
        "1.00 Solomon Islands dollar",
        "1.00 Solomon Islands dollars",
        "1.00 Somali Shilling",
        "1.00 Somali shilling",
        "1.00 Somali shillings",
        "1.00 South African Rand (financial)",
        "1.00 South African Rand",
        "1.00 South African rand (financial)",
        "1.00 South African rand",
        "1.00 South African rands (financial)",
        "1.00 South African rands",
        "1.00 South Korean Won",
        "1.00 South Korean won",
        "1.00 South Korean wons",
        "1.00 Soviet Rouble",
        "1.00 Soviet rouble",
        "1.00 Soviet roubles",
        "1.00 Spanish Peseta (A account)",
        "1.00 Spanish Peseta (convertible account)",
        "1.00 Spanish Peseta",
        "1.00 Spanish peseta (A account)",
        "1.00 Spanish peseta (convertible account)",
        "1.00 Spanish peseta",
        "1.00 Spanish pesetas (A account)",
        "1.00 Spanish pesetas (convertible account)",
        "1.00 Spanish pesetas",
        "1.00 Special Drawing Rights",
        "1.00 Sri Lanka Rupee",
        "1.00 Sri Lanka rupee",
        "1.00 Sri Lanka rupees",
        "1.00 Sudanese Pound",
        "1.00 Sudanese pound",
        "1.00 Sudanese pounds",
        "1.00 Surinam Dollar",
        "1.00 Surinam dollar",
        "1.00 Surinam dollars",
        "1.00 Suriname Guilder",
        "1.00 Suriname guilder",
        "1.00 Suriname guilders",
        "1.00 Swaziland Lilangeni",
        "1.00 Swaziland lilangeni",
        "1.00 Swaziland lilangenis",
        "1.00 Swedish Krona",
        "1.00 Swedish krona",
        "1.00 Swedish kronas",
        "1.00 Swiss Franc",
        "1.00 Swiss franc",
        "1.00 Swiss francs",
        "1.00 Syrian Pound",
        "1.00 Syrian pound",
        "1.00 Syrian pounds",
        "1.00 T Sh",
        "1.00 T",
        "1.00 T$",
        "1.00 THB",
        "1.00 TJR",
        "1.00 TJR",
        "1.00 TJS",
        "1.00 TJS",
        "1.00 TL",
        "1.00 TMM",
        "1.00 TMM",
        "1.00 TND",
        "1.00 TND",
        "1.00 TOP",
        "1.00 TPE",
        "1.00 TPE",
        "1.00 TRL",
        "1.00 TRY",
        "1.00 TRY",
        "1.00 TT$",
        "1.00 TTD",
        "1.00 TWD",
        "1.00 TZS",
        "1.00 Taiwan New Dollar",
        "1.00 Taiwan dollar",
        "1.00 Taiwan dollars",
        "1.00 Tajikistan Ruble",
        "1.00 Tajikistan Somoni",
        "1.00 Tajikistan ruble",
        "1.00 Tajikistan rubles",
        "1.00 Tajikistan somoni",
        "1.00 Tajikistan somonis",
        "1.00 Tanzanian Shilling",
        "1.00 Tanzanian shilling",
        "1.00 Tanzanian shillings",
        "1.00 Testing Currency Code",
        "1.00 Testing Currency Code",
        "1.00 Thai Baht",
        "1.00 Thai baht",
        "1.00 Thai bahts",
        "1.00 Timor Escudo",
        "1.00 Timor escudo",
        "1.00 Timor escudos",
        "1.00 Tk",
        "1.00 Tonga Paʻanga",
        "1.00 Tonga paʻanga",
        "1.00 Tonga paʻangas",
        "1.00 Trinidad and Tobago Dollar",
        "1.00 Trinidad and Tobago dollar",
        "1.00 Trinidad and Tobago dollars",
        "1.00 Tunisian Dinar",
        "1.00 Tunisian dinar",
        "1.00 Tunisian dinars",
        "1.00 Turkish Lira",
        "1.00 Turkish Lira",
        "1.00 Turkish lira",
        "1.00 Turkmenistan Manat",
        "1.00 Turkmenistan manat",
        "1.00 Turkmenistan manats",
        "1.00 U Sh",
        "1.00 UAE dirham",
        "1.00 UAE dirhams",
        "1.00 UAH",
        "1.00 UAK",
        "1.00 UAK",
        "1.00 UGS",
        "1.00 UGS",
        "1.00 UGX",
        "1.00 UM",
        "1.00 US Dollar (Next day)",
        "1.00 US Dollar (Same day)",
        "1.00 US Dollar",
        "1.00 US dollar (next day)",
        "1.00 US dollar (same day)",
        "1.00 US dollar",
        "1.00 US dollars (next day)",
        "1.00 US dollars (same day)",
        "1.00 US dollars",
        "1.00 USD",
        "1.00 USN",
        "1.00 USN",
        "1.00 USS",
        "1.00 USS",
        "1.00 UYI",
        "1.00 UYI",
        "1.00 UYP",
        "1.00 UYP",
        "1.00 UYU",
        "1.00 UZS",
        "1.00 UZS",
        "1.00 Ugandan Shilling (1966-1987)",
        "1.00 Ugandan Shilling",
        "1.00 Ugandan shilling (UGS)",
        "1.00 Ugandan shilling",
        "1.00 Ugandan shillings (UGS)",
        "1.00 Ugandan shillings",
        "1.00 Ukrainian Hryvnia",
        "1.00 Ukrainian Karbovanetz",
        "1.00 Ukrainian hryvnia",
        "1.00 Ukrainian hryvnias",
        "1.00 Ukrainian karbovanetz",
        "1.00 Ukrainian karbovanetzs",
        "1.00 Unidad de Valor Real",
        "1.00 United Arab Emirates Dirham",
        "1.00 Unknown or Invalid Currency",
        "1.00 Ur$",
        "1.00 Uruguay Peso (1975-1993)",
        "1.00 Uruguay Peso Uruguayo",
        "1.00 Uruguay Peso en Unidades Indexadas",
        "1.00 Uruguay peso (UYP)",
        "1.00 Uruguay peso en unidades indexadas",
        "1.00 Uruguay peso",
        "1.00 Uruguay pesos (UYP)",
        "1.00 Uruguay pesos en unidades indexadas",
        "1.00 Uzbekistan Sum",
        "1.00 Uzbekistan sum",
        "1.00 Uzbekistan sums",
        "1.00 VEB",
        "1.00 VEF",
        "1.00 VND",
        "1.00 VT",
        "1.00 VUV",
        "1.00 Vanuatu Vatu",
        "1.00 Vanuatu vatu",
        "1.00 Vanuatu vatus",
        "1.00 Venezuelan Bolivar Fuerte",
        "1.00 Venezuelan Bolivar",
        "1.00 Venezuelan bolivar fuerte",
        "1.00 Venezuelan bolivar fuertes",
        "1.00 Venezuelan bolivar",
        "1.00 Venezuelan bolivars",
        "1.00 Vietnamese Dong",
        "1.00 Vietnamese dong",
        "1.00 Vietnamese dongs",
        "1.00 WIR Euro",
        "1.00 WIR Franc",
        "1.00 WIR euro",
        "1.00 WIR euros",
        "1.00 WIR franc",
        "1.00 WIR francs",
        "1.00 WST",
        "1.00 WST",
        "1.00 Western Samoa Tala",
        "1.00 Western Samoa tala",
        "1.00 Western Samoa talas",
        "1.00 XAF",
        "1.00 XAF",
        "1.00 XAG",
        "1.00 XAG",
        "1.00 XAU",
        "1.00 XAU",
        "1.00 XBA",
        "1.00 XBA",
        "1.00 XBB",
        "1.00 XBB",
        "1.00 XBC",
        "1.00 XBC",
        "1.00 XBD",
        "1.00 XBD",
        "1.00 XCD",
        "1.00 XDR",
        "1.00 XDR",
        "1.00 XEU",
        "1.00 XEU",
        "1.00 XFO",
        "1.00 XFO",
        "1.00 XFU",
        "1.00 XFU",
        "1.00 XOF",
        "1.00 XOF",
        "1.00 XPD",
        "1.00 XPD",
        "1.00 XPF",
        "1.00 XPT",
        "1.00 XPT",
        "1.00 XRE",
        "1.00 XRE",
        "1.00 XTS",
        "1.00 XTS",
        "1.00 XXX",
        "1.00 XXX",
        "1.00 YDD",
        "1.00 YDD",
        "1.00 YER",
        "1.00 YRl",
        "1.00 YUD",
        "1.00 YUD",
        "1.00 YUM",
        "1.00 YUM",
        "1.00 YUN",
        "1.00 YUN",
        "1.00 Yemeni Dinar",
        "1.00 Yemeni Rial",
        "1.00 Yemeni dinar",
        "1.00 Yemeni dinars",
        "1.00 Yemeni rial",
        "1.00 Yemeni rials",
        "1.00 Yugoslavian Convertible Dinar",
        "1.00 Yugoslavian Hard Dinar",
        "1.00 Yugoslavian Noviy Dinar",
        "1.00 Yugoslavian Noviy dinars",
        "1.00 Yugoslavian convertible dinar",
        "1.00 Yugoslavian convertible dinars",
        "1.00 Yugoslavian hard dinar",
        "1.00 Yugoslavian hard dinars",
        "1.00 Yugoslavian noviy dinar",
        "1.00 Z$",
        "1.00 ZAL",
        "1.00 ZAL",
        "1.00 ZAR",
        "1.00 ZMK",
        "1.00 ZMK",
        "1.00 ZRN",
        "1.00 ZRN",
        "1.00 ZRZ",
        "1.00 ZRZ",
        "1.00 ZWD",
        "1.00 Zairean New Zaire",
        "1.00 Zairean Zaire",
        "1.00 Zairean new zaire",
        "1.00 Zairean new zaires",
        "1.00 Zairean zaire",
        "1.00 Zairean zaires",
        "1.00 Zambian Kwacha",
        "1.00 Zambian kwacha",
        "1.00 Zambian kwachas",
        "1.00 Zimbabwe Dollar",
        "1.00 Zimbabwe dollar",
        "1.00 Zimbabwe dollars",
        "1.00 dram",
        "1.00 ekwele",
        "1.00 ekweles",
        "1.00 euro",
        "1.00 euros",
        "1.00 lari",
        "1.00 lek",
        "1.00 lev",
        "1.00 maloti",
        "1.00 malotis",
        "1.00 man.",
        "1.00 old Turkish lira",
        "1.00 som",
        "1.00 special drawing rights",
        "1.00 unidad de valor real",
        "1.00 unidad de valor reals",
        "1.00 unknown/invalid currency",
        "1.00 z\\u0142",
        "1.00 \\u00a3",
        "1.00 \\u00a3C",
        "1.00 \\u00a5",
        "1.00 \\u0e3f",
        "1.00 \\u20ab",
        "1.00 \\u20a1",
        "1.00 \\u20a7",
        "1.00 \\u20aa",
        "1.00 \\u20ac",
        "1.00 \\u20a8",
        "1.00 \\u20a6",
        "1.00 \\u20ae",
        "1.00 \\u20a4",
        // for GHS
        // for PHP
        // for PYG
        // for UAH
        //
        // Following has extra text, should be parsed correctly too
        "$1.00 random",
        "USD1.00 random",
        "1.00 US dollar random",
        "1.00 US dollars random",
        "1.00 $ random",
        "1.00 A$ random",
        "1.00 ADP random",
        "1.00 ADP random",
        "1.00 AED random",
        "1.00 AED random",
        "1.00 AFA random",
        "1.00 AFA random",
        "1.00 AFN random",
        "1.00 ALL random",
        "1.00 AMD random",
        "1.00 ANG random",
        "1.00 AOA random",
        "1.00 AOK random",
        "1.00 AOK random",
        "1.00 AON random",
        "1.00 AON random",
        "1.00 AOR random",
        "1.00 AOR random",
        "1.00 AR$ random",
        "1.00 ARA random",
        "1.00 ARA random",
        "1.00 ARP random",
        "1.00 ARP random",
        "1.00 ARS random",
        "1.00 ATS random",
        "1.00 ATS random",
        "1.00 AUD random",
        "1.00 AWG random",
        "1.00 AZM random",
        "1.00 AZM random",
        "1.00 AZN random",
        "1.00 Af random",
        "1.00 Afghani (1927-2002) random",
        "1.00 Afghani (AFA) random",
        "1.00 Afghani random",
        "1.00 Afghani random",
        "1.00 Afghanis (AFA) random",
        "1.00 Afghanis random",
        "1.00 Afl. random",
        "1.00 Albanian Lek random",
        "1.00 Albanian lek random",
        "1.00 Albanian lekë random",
        "1.00 Algerian Dinar random",
        "1.00 Algerian dinar random",
        "1.00 Algerian dinars random",
        "1.00 Andorran Peseta random",
        "1.00 Andorran peseta random",
        "1.00 Andorran pesetas random",
        "1.00 Angolan Kwanza (1977-1990) random",
        "1.00 Angolan Kwanza Reajustado (1995-1999) random",
        "1.00 Angolan Kwanza random",
        "1.00 Angolan New Kwanza (1990-2000) random",
        "1.00 Angolan kwanza (AOK) random",
        "1.00 Angolan kwanza reajustado (AOR) random",
        "1.00 Angolan kwanza random",
        "1.00 Angolan kwanzas (AOK) random",
        "1.00 Angolan kwanzas reajustado (AOR) random",
        "1.00 Angolan kwanzas random",
        "1.00 Angolan new kwanza (AON) random",
        "1.00 Angolan new kwanzas (AON) random",
        "1.00 Argentine Austral random",
        "1.00 Argentine Peso (1983-1985) random",
        "1.00 Argentine Peso random",
        "1.00 Argentine austral random",
        "1.00 Argentine australs random",
        "1.00 Argentine peso (ARP) random",
        "1.00 Argentine peso random",
        "1.00 Argentine pesos (ARP) random",
        "1.00 Argentine pesos random",
        "1.00 Armenian Dram random",
        "1.00 Armenian dram random",
        "1.00 Armenian drams random",
        "1.00 Aruban Florin random",
        "1.00 Aruban florin random",
        "1.00 Australian Dollar random",
        "1.00 Australian dollar random",
        "1.00 Australian dollars random",
        "1.00 Austrian Schilling random",
        "1.00 Austrian schilling random",
        "1.00 Austrian schillings random",
        "1.00 Azerbaijanian Manat (1993-2006) random",
        "1.00 Azerbaijanian Manat random",
        "1.00 Azerbaijanian manat (AZM) random",
        "1.00 Azerbaijanian manat random",
        "1.00 Azerbaijanian manats (AZM) random",
        "1.00 Azerbaijanian manats random",
        "1.00 B$ random",
        "1.00 BAD random",
        "1.00 BAD random",
        "1.00 BAM random",
        "1.00 BBD random",
        "1.00 BD random",
        "1.00 BD$ random",
        "1.00 BDT random",
        "1.00 BEC random",
        "1.00 BEC random",
        "1.00 BEF random",
        "1.00 BEL random",
        "1.00 BEL random",
        "1.00 BF random",
        "1.00 BGL random",
        "1.00 BGN random",
        "1.00 BGN random",
        "1.00 BHD random",
        "1.00 BIF random",
        "1.00 BMD random",
        "1.00 BND random",
        "1.00 BOB random",
        "1.00 BOP random",
        "1.00 BOP random",
        "1.00 BOV random",
        "1.00 BOV random",
        "1.00 BRB random",
        "1.00 BRB random",
        "1.00 BRC random",
        "1.00 BRC random",
        "1.00 BRE random",
        "1.00 BRE random",
        "1.00 BRL random",
        "1.00 BRN random",
        "1.00 BRN random",
        "1.00 BRR random",
        "1.00 BRR random",
        "1.00 BSD random",
        "1.00 BSD random",
        "1.00 BTN random",
        "1.00 BUK random",
        "1.00 BUK random",
        "1.00 BWP random",
        "1.00 BYB random",
        "1.00 BYB random",
        "1.00 BYR random",
        "1.00 BZ$ random",
        "1.00 BZD random",
        "1.00 Bahamian Dollar random",
        "1.00 Bahamian dollar random",
        "1.00 Bahamian dollars random",
        "1.00 Bahraini Dinar random",
        "1.00 Bahraini dinar random",
        "1.00 Bahraini dinars random",
        "1.00 Bangladeshi Taka random",
        "1.00 Bangladeshi taka random",
        "1.00 Bangladeshi takas random",
        "1.00 Barbados Dollar random",
        "1.00 Barbados dollar random",
        "1.00 Barbados dollars random",
        "1.00 Bds$ random",
        "1.00 Be random",
        "1.00 Belarussian New Ruble (1994-1999) random",
        "1.00 Belarussian Ruble random",
        "1.00 Belarussian new ruble (BYB) random",
        "1.00 Belarussian new rubles (BYB) random",
        "1.00 Belarussian ruble random",
        "1.00 Belarussian rubles random",
        "1.00 Belgian Franc (convertible) random",
        "1.00 Belgian Franc (financial) random",
        "1.00 Belgian Franc random",
        "1.00 Belgian franc (convertible) random",
        "1.00 Belgian franc (financial) random",
        "1.00 Belgian franc random",
        "1.00 Belgian francs (convertible) random",
        "1.00 Belgian francs (financial) random",
        "1.00 Belgian francs random",
        "1.00 Belize Dollar random",
        "1.00 Belize dollar random",
        "1.00 Belize dollars random",
        "1.00 Bermudan Dollar random",
        "1.00 Bermudan dollar random",
        "1.00 Bermudan dollars random",
        "1.00 Bhutan Ngultrum random",
        "1.00 Bhutan ngultrum random",
        "1.00 Bhutan ngultrums random",
        "1.00 Bolivian Mvdol random",
        "1.00 Bolivian Peso random",
        "1.00 Bolivian mvdol random",
        "1.00 Bolivian mvdols random",
        "1.00 Bolivian peso random",
        "1.00 Bolivian pesos random",
        "1.00 Boliviano random",
        "1.00 Boliviano random",
        "1.00 Bolivianos random",
        "1.00 Bosnia-Herzegovina Convertible Mark random",
        "1.00 Bosnia-Herzegovina Dinar random",
        "1.00 Bosnia-Herzegovina convertible mark random",
        "1.00 Bosnia-Herzegovina convertible marks random",
        "1.00 Bosnia-Herzegovina dinar random",
        "1.00 Bosnia-Herzegovina dinars random",
        "1.00 Botswanan Pula random",
        "1.00 Botswanan pula random",
        "1.00 Botswanan pulas random",
        "1.00 Br random",
        "1.00 Brazilian Cruzado Novo random",
        "1.00 Brazilian Cruzado random",
        "1.00 Brazilian Cruzeiro (1990-1993) random",
        "1.00 Brazilian Cruzeiro Novo (1967-1986) random",
        "1.00 Brazilian Cruzeiro random",
        "1.00 Brazilian Real random",
        "1.00 Brazilian cruzado novo random",
        "1.00 Brazilian cruzado novos random",
        "1.00 Brazilian cruzado random",
        "1.00 Brazilian cruzados random",
        "1.00 Brazilian cruzeiro (BRE) random",
        "1.00 Brazilian cruzeiro novo (BRB) random",
        "1.00 Brazilian cruzeiro random",
        "1.00 Brazilian cruzeiros (BRE) random",
        "1.00 Brazilian cruzeiros novo (BRB) random",
        "1.00 Brazilian cruzeiros random",
        "1.00 Brazilian real random",
        "1.00 Brazilian reals random",
        "1.00 British Pound Sterling random",
        "1.00 British pound sterling random",
        "1.00 British pound sterlings random",
        "1.00 Brunei Dollar random",
        "1.00 Brunei dollar random",
        "1.00 Brunei dollars random",
        "1.00 Bs random",
        "1.00 BsF random",
        "1.00 Bulgarian Hard Lev random",
        "1.00 Bulgarian Lev random",
        "1.00 Bulgarian Levs random",
        "1.00 Bulgarian hard lev random",
        "1.00 Bulgarian hard levs random",
        "1.00 Bulgarian lev random",
        "1.00 Burmese Kyat random",
        "1.00 Burmese kyat random",
        "1.00 Burmese kyats random",
        "1.00 Burundi Franc random",
        "1.00 Burundi franc random",
        "1.00 Burundi francs random",
        "1.00 C$ random",
        "1.00 CA$ random",
        "1.00 CAD random",
        "1.00 CDF random",
        "1.00 CDF random",
        "1.00 CF random",
        "1.00 CFA Franc BCEAO random",
        "1.00 CFA Franc BEAC random",
        "1.00 CFA franc BCEAO random",
        "1.00 CFA franc BEAC random",
        "1.00 CFA francs BCEAO random",
        "1.00 CFA francs BEAC random",
        "1.00 CFP Franc random",
        "1.00 CFP franc random",
        "1.00 CFP francs random",
        "1.00 CFPF random",
        "1.00 CHE random",
        "1.00 CHE random",
        "1.00 CHF random",
        "1.00 CHW random",
        "1.00 CHW random",
        "1.00 CL$ random",
        "1.00 CLF random",
        "1.00 CLF random",
        "1.00 CLP random",
        "1.00 CNY random",
        "1.00 CO$ random",
        "1.00 COP random",
        "1.00 COU random",
        "1.00 COU random",
        "1.00 CR random",
        "1.00 CRC random",
        "1.00 CSD random",
        "1.00 CSD random",
        "1.00 CSK random",
        "1.00 CSK random",
        "1.00 CUP random",
        "1.00 CUP random",
        "1.00 CVE random",
        "1.00 CYP random",
        "1.00 CZK random",
        "1.00 Cambodian Riel random",
        "1.00 Cambodian riel random",
        "1.00 Cambodian riels random",
        "1.00 Canadian Dollar random",
        "1.00 Canadian dollar random",
        "1.00 Canadian dollars random",
        "1.00 Cape Verde Escudo random",
        "1.00 Cape Verde escudo random",
        "1.00 Cape Verde escudos random",
        "1.00 Cayman Islands Dollar random",
        "1.00 Cayman Islands dollar random",
        "1.00 Cayman Islands dollars random",
        "1.00 Chilean Peso random",
        "1.00 Chilean Unidades de Fomento random",
        "1.00 Chilean peso random",
        "1.00 Chilean pesos random",
        "1.00 Chilean unidades de fomento random",
        "1.00 Chilean unidades de fomentos random",
        "1.00 Chinese Yuan Renminbi random",
        "1.00 Chinese yuan random",
        "1.00 Colombian Peso random",
        "1.00 Colombian peso random",
        "1.00 Colombian pesos random",
        "1.00 Comoro Franc random",
        "1.00 Comoro franc random",
        "1.00 Comoro francs random",
        "1.00 Congolese Franc Congolais random",
        "1.00 Congolese franc Congolais random",
        "1.00 Congolese francs Congolais random",
        "1.00 Costa Rican Colon random",
        "1.00 Costa Rican colon random",
        "1.00 Costa Rican colons random",
        "1.00 Croatian Dinar random",
        "1.00 Croatian Kuna random",
        "1.00 Croatian dinar random",
        "1.00 Croatian dinars random",
        "1.00 Croatian kuna random",
        "1.00 Croatian kunas random",
        "1.00 Cuban Peso random",
        "1.00 Cuban peso random",
        "1.00 Cuban pesos random",
        "1.00 Cyprus Pound random",
        "1.00 Cyprus pound random",
        "1.00 Cyprus pounds random",
        "1.00 Czech Republic Koruna random",
        "1.00 Czech Republic koruna random",
        "1.00 Czech Republic korunas random",
        "1.00 Czechoslovak Hard Koruna random",
        "1.00 Czechoslovak hard koruna random",
        "1.00 Czechoslovak hard korunas random",
        "1.00 DA random",
        "1.00 DDM random",
        "1.00 DDM random",
        "1.00 DEM random",
        "1.00 DEM random",
        "1.00 DJF random",
        "1.00 DKK random",
        "1.00 DOP random",
        "1.00 DZD random",
        "1.00 Danish Krone random",
        "1.00 Danish krone random",
        "1.00 Danish kroner random",
        "1.00 Db random",
        "1.00 Deutsche Mark random",
        "1.00 Deutsche mark random",
        "1.00 Deutsche marks random",
        "1.00 Djibouti Franc random",
        "1.00 Djibouti franc random",
        "1.00 Djibouti francs random",
        "1.00 Dkr random",
        "1.00 Dominican Peso random",
        "1.00 Dominican peso random",
        "1.00 Dominican pesos random",
        "1.00 E random",
        "1.00 EC$ random",
        "1.00 ECS random",
        "1.00 ECS random",
        "1.00 ECV random",
        "1.00 ECV random",
        "1.00 EEK random",
        "1.00 EEK random",
        "1.00 EGP random",
        "1.00 EGP random",
        "1.00 EQE random",
        "1.00 EQE random",
        "1.00 ERN random",
        "1.00 ERN random",
        "1.00 ESA random",
        "1.00 ESA random",
        "1.00 ESB random",
        "1.00 ESB random",
        "1.00 ESP random",
        "1.00 ETB random",
        "1.00 EUR random",
        "1.00 East Caribbean Dollar random",
        "1.00 East Caribbean dollar random",
        "1.00 East Caribbean dollars random",
        "1.00 East German Ostmark random",
        "1.00 East German ostmark random",
        "1.00 East German ostmarks random",
        "1.00 Ecuador Sucre random",
        "1.00 Ecuador Unidad de Valor Constante (UVC) random",
        "1.00 Ecuador sucre random",
        "1.00 Ecuador sucres random",
        "1.00 Ecuador unidad de valor Constante (UVC) random",
        "1.00 Ecuador unidads de valor Constante (UVC) random",
        "1.00 Egyptian Pound random",
        "1.00 Egyptian pound random",
        "1.00 Egyptian pounds random",
        "1.00 Ekwele random",
        "1.00 El Salvador Colon random",
        "1.00 El Salvador colon random",
        "1.00 El Salvador colons random",
        "1.00 Equatorial Guinea Ekwele Guineana random",
        "1.00 Equatorial Guinea ekwele random",
        "1.00 Eritrean Nakfa random",
        "1.00 Eritrean nakfa random",
        "1.00 Eritrean nakfas random",
        "1.00 Esc random",
        "1.00 Estonian Kroon random",
        "1.00 Estonian kroon random",
        "1.00 Estonian kroons random",
        "1.00 Ethiopian Birr random",
        "1.00 Ethiopian birr random",
        "1.00 Ethiopian birrs random",
        "1.00 Euro random",
        "1.00 European Composite Unit random",
        "1.00 European Currency Unit random",
        "1.00 European Monetary Unit random",
        "1.00 European Unit of Account (XBC) random",
        "1.00 European Unit of Account (XBD) random",
        "1.00 European composite unit random",
        "1.00 European composite units random",
        "1.00 European currency unit random",
        "1.00 European currency units random",
        "1.00 European monetary unit random",
        "1.00 European monetary units random",
        "1.00 European unit of account (XBC) random",
        "1.00 European unit of account (XBD) random",
        "1.00 European units of account (XBC) random",
        "1.00 European units of account (XBD) random",
        "1.00 F$ random",
        "1.00 FBu random",
        "1.00 FIM random",
        "1.00 FIM random",
        "1.00 FJD random",
        "1.00 FKP random",
        "1.00 FKP random",
        "1.00 FRF random",
        "1.00 FRF random",
        "1.00 Falkland Islands Pound random",
        "1.00 Falkland Islands pound random",
        "1.00 Falkland Islands pounds random",
        "1.00 Fdj random",
        "1.00 Fiji Dollar random",
        "1.00 Fiji dollar random",
        "1.00 Fiji dollars random",
        "1.00 Finnish Markka random",
        "1.00 Finnish markka random",
        "1.00 Finnish markkas random",
        "1.00 Fr. random",
        "1.00 French Franc random",
        "1.00 French Gold Franc random",
        "1.00 French UIC-Franc random",
        "1.00 French UIC-franc random",
        "1.00 French UIC-francs random",
        "1.00 French franc random",
        "1.00 French francs random",
        "1.00 French gold franc random",
        "1.00 French gold francs random",
        "1.00 Ft random",
        "1.00 G$ random",
        "1.00 GBP random",
        "1.00 GEK random",
        "1.00 GEK random",
        "1.00 GEL random",
        "1.00 GF random",
        "1.00 GHC random",
        "1.00 GHC random",
        "1.00 GHS random",
        "1.00 GIP random",
        "1.00 GIP random",
        "1.00 GMD random",
        "1.00 GMD random",
        "1.00 GNF random",
        "1.00 GNS random",
        "1.00 GNS random",
        "1.00 GQE random",
        "1.00 GQE random",
        "1.00 GRD random",
        "1.00 GRD random",
        "1.00 GTQ random",
        "1.00 GWE random",
        "1.00 GWE random",
        "1.00 GWP random",
        "1.00 GWP random",
        "1.00 GYD random",
        "1.00 Gambia Dalasi random",
        "1.00 Gambia dalasi random",
        "1.00 Gambia dalasis random",
        "1.00 Georgian Kupon Larit random",
        "1.00 Georgian Lari random",
        "1.00 Georgian kupon larit random",
        "1.00 Georgian kupon larits random",
        "1.00 Georgian lari random",
        "1.00 Georgian laris random",
        "1.00 Ghana Cedi (1979-2007) random",
        "1.00 Ghana Cedi random",
        "1.00 Ghana cedi (GHC) random",
        "1.00 Ghana cedi random",
        "1.00 Ghana cedis (GHC) random",
        "1.00 Ghana cedis random",
        "1.00 Gibraltar Pound random",
        "1.00 Gibraltar pound random",
        "1.00 Gibraltar pounds random",
        "1.00 Gold random",
        "1.00 Gold random",
        "1.00 Greek Drachma random",
        "1.00 Greek drachma random",
        "1.00 Greek drachmas random",
        "1.00 Guatemala Quetzal random",
        "1.00 Guatemala quetzal random",
        "1.00 Guatemala quetzals random",
        "1.00 Guinea Franc random",
        "1.00 Guinea Syli random",
        "1.00 Guinea franc random",
        "1.00 Guinea francs random",
        "1.00 Guinea syli random",
        "1.00 Guinea sylis random",
        "1.00 Guinea-Bissau Peso random",
        "1.00 Guinea-Bissau peso random",
        "1.00 Guinea-Bissau pesos random",
        "1.00 Guyana Dollar random",
        "1.00 Guyana dollar random",
        "1.00 Guyana dollars random",
        "1.00 HK$ random",
        "1.00 HKD random",
        "1.00 HNL random",
        "1.00 HRD random",
        "1.00 HRD random",
        "1.00 HRK random",
        "1.00 HRK random",
        "1.00 HTG random",
        "1.00 HTG random",
        "1.00 HUF random",
        "1.00 Haitian Gourde random",
        "1.00 Haitian gourde random",
        "1.00 Haitian gourdes random",
        "1.00 Honduras Lempira random",
        "1.00 Honduras lempira random",
        "1.00 Honduras lempiras random",
        "1.00 Hong Kong Dollar random",
        "1.00 Hong Kong dollar random",
        "1.00 Hong Kong dollars random",
        "1.00 Hungarian Forint random",
        "1.00 Hungarian forint random",
        "1.00 Hungarian forints random",
        "1.00 ID random",
        "1.00 IDR random",
        "1.00 IEP random",
        "1.00 ILP random",
        "1.00 ILP random",
        "1.00 ILS random",
        "1.00 INR random",
        "1.00 IQD random",
        "1.00 IRR random",
        "1.00 IR\\u00a3 random",
        "1.00 ISK random",
        "1.00 ISK random",
        "1.00 ITL random",
        "1.00 Icelandic Krona random",
        "1.00 Icelandic krona random",
        "1.00 Icelandic kronas random",
        "1.00 Indian Rupee random",
        "1.00 Indian rupee random",
        "1.00 Indian rupees random",
        "1.00 Indonesian Rupiah random",
        "1.00 Indonesian rupiah random",
        "1.00 Indonesian rupiahs random",
        "1.00 Iranian Rial random",
        "1.00 Iranian rial random",
        "1.00 Iranian rials random",
        "1.00 Iraqi Dinar random",
        "1.00 Iraqi dinar random",
        "1.00 Iraqi dinars random",
        "1.00 Irish Pound random",
        "1.00 Irish pound random",
        "1.00 Irish pounds random",
        "1.00 Israeli Pound random",
        "1.00 Israeli new sheqel random",
        "1.00 Israeli pound random",
        "1.00 Israeli pounds random",
        "1.00 Italian Lira random",
        "1.00 Italian lira random",
        "1.00 Italian liras random",
        "1.00 J$ random",
        "1.00 JD random",
        "1.00 JMD random",
        "1.00 JOD random",
        "1.00 JPY random",
        "1.00 Jamaican Dollar random",
        "1.00 Jamaican dollar random",
        "1.00 Jamaican dollars random",
        "1.00 Japanese Yen random",
        "1.00 Japanese yen random",
        "1.00 Jordanian Dinar random",
        "1.00 Jordanian dinar random",
        "1.00 Jordanian dinars random",
        "1.00 K Sh random",
        "1.00 KD random",
        "1.00 KES random",
        "1.00 KGS random",
        "1.00 KHR random",
        "1.00 KM random",
        "1.00 KMF random",
        "1.00 KPW random",
        "1.00 KPW random",
        "1.00 KRW random",
        "1.00 KWD random",
        "1.00 KYD random",
        "1.00 KYD random",
        "1.00 KZT random",
        "1.00 Kazakhstan Tenge random",
        "1.00 Kazakhstan tenge random",
        "1.00 Kazakhstan tenges random",
        "1.00 Kenyan Shilling random",
        "1.00 Kenyan shilling random",
        "1.00 Kenyan shillings random",
        "1.00 Kuwaiti Dinar random",
        "1.00 Kuwaiti dinar random",
        "1.00 Kuwaiti dinars random",
        "1.00 Kyrgystan Som random",
        "1.00 Kyrgystan som random",
        "1.00 Kyrgystan soms random",
        "1.00 Kz random",
        "1.00 K\\u010d random",
        "1.00 L random",
        "1.00 LAK random",
        "1.00 LAK random",
        "1.00 LBP random",
        "1.00 LD random",
        "1.00 LKR random",
        "1.00 LL random",
        "1.00 LRD random",
        "1.00 LRD random",
        "1.00 LS random",
        "1.00 LSL random",
        "1.00 LSM random",
        "1.00 LSM random",
        "1.00 LTL random",
        "1.00 LTL random",
        "1.00 LTT random",
        "1.00 LTT random",
        "1.00 LUC random",
        "1.00 LUC random",
        "1.00 LUF random",
        "1.00 LUF random",
        "1.00 LUL random",
        "1.00 LUL random",
        "1.00 LVL random",
        "1.00 LVL random",
        "1.00 LVR random",
        "1.00 LVR random",
        "1.00 LYD random",
        "1.00 Laotian Kip random",
        "1.00 Laotian kip random",
        "1.00 Laotian kips random",
        "1.00 Latvian Lats random",
        "1.00 Latvian Ruble random",
        "1.00 Latvian lats random",
        "1.00 Latvian latses random",
        "1.00 Latvian ruble random",
        "1.00 Latvian rubles random",
        "1.00 Lebanese Pound random",
        "1.00 Lebanese pound random",
        "1.00 Lebanese pounds random",
        "1.00 Lesotho Loti random",
        "1.00 Lesotho loti random",
        "1.00 Lesotho lotis random",
        "1.00 Liberian Dollar random",
        "1.00 Liberian dollar random",
        "1.00 Liberian dollars random",
        "1.00 Libyan Dinar random",
        "1.00 Libyan dinar random",
        "1.00 Libyan dinars random",
        "1.00 Lithuanian Lita random",
        "1.00 Lithuanian Talonas random",
        "1.00 Lithuanian lita random",
        "1.00 Lithuanian litas random",
        "1.00 Lithuanian talonas random",
        "1.00 Lithuanian talonases random",
        "1.00 Lm random",
        "1.00 Luxembourg Convertible Franc random",
        "1.00 Luxembourg Financial Franc random",
        "1.00 Luxembourg Franc random",
        "1.00 Luxembourg convertible franc random",
        "1.00 Luxembourg convertible francs random",
        "1.00 Luxembourg financial franc random",
        "1.00 Luxembourg financial francs random",
        "1.00 Luxembourg franc random",
        "1.00 Luxembourg francs random",
        "1.00 M random",
        "1.00 MAD random",
        "1.00 MAD random",
        "1.00 MAF random",
        "1.00 MAF random",
        "1.00 MDL random",
        "1.00 MDL random",
        "1.00 MDen random",
        "1.00 MEX$ random",
        "1.00 MGA random",
        "1.00 MGA random",
        "1.00 MGF random",
        "1.00 MGF random",
        "1.00 MK random",
        "1.00 MKD random",
        "1.00 MLF random",
        "1.00 MLF random",
        "1.00 MMK random",
        "1.00 MMK random",
        "1.00 MNT random",
        "1.00 MOP random",
        "1.00 MOP random",
        "1.00 MRO random",
        "1.00 MTL random",
        "1.00 MTP random",
        "1.00 MTP random",
        "1.00 MTn random",
        "1.00 MUR random",
        "1.00 MUR random",
        "1.00 MVR random",
        "1.00 MVR random",
        "1.00 MWK random",
        "1.00 MXN random",
        "1.00 MXP random",
        "1.00 MXP random",
        "1.00 MXV random",
        "1.00 MXV random",
        "1.00 MYR random",
        "1.00 MZE random",
        "1.00 MZE random",
        "1.00 MZM random",
        "1.00 MZN random",
        "1.00 Macao Pataca random",
        "1.00 Macao pataca random",
        "1.00 Macao patacas random",
        "1.00 Macedonian Denar random",
        "1.00 Macedonian denar random",
        "1.00 Macedonian denars random",
        "1.00 Madagascar Ariaries random",
        "1.00 Madagascar Ariary random",
        "1.00 Madagascar Ariary random",
        "1.00 Madagascar Franc random",
        "1.00 Madagascar franc random",
        "1.00 Madagascar francs random",
        "1.00 Malawi Kwacha random",
        "1.00 Malawi Kwacha random",
        "1.00 Malawi Kwachas random",
        "1.00 Malaysian Ringgit random",
        "1.00 Malaysian ringgit random",
        "1.00 Malaysian ringgits random",
        "1.00 Maldive Islands Rufiyaa random",
        "1.00 Maldive Islands rufiyaa random",
        "1.00 Maldive Islands rufiyaas random",
        "1.00 Mali Franc random",
        "1.00 Mali franc random",
        "1.00 Mali francs random",
        "1.00 Maloti random",
        "1.00 Maltese Lira random",
        "1.00 Maltese Pound random",
        "1.00 Maltese lira random",
        "1.00 Maltese liras random",
        "1.00 Maltese pound random",
        "1.00 Maltese pounds random",
        "1.00 Mauritania Ouguiya random",
        "1.00 Mauritania ouguiya random",
        "1.00 Mauritania ouguiyas random",
        "1.00 Mauritius Rupee random",
        "1.00 Mauritius rupee random",
        "1.00 Mauritius rupees random",
        "1.00 Mexican Peso random",
        "1.00 Mexican Silver Peso (1861-1992) random",
        "1.00 Mexican Unidad de Inversion (UDI) random",
        "1.00 Mexican peso random",
        "1.00 Mexican pesos random",
        "1.00 Mexican silver peso (MXP) random",
        "1.00 Mexican silver pesos (MXP) random",
        "1.00 Mexican unidad de inversion (UDI) random",
        "1.00 Mexican unidads de inversion (UDI) random",
        "1.00 Moldovan Leu random",
        "1.00 Moldovan leu random",
        "1.00 Moldovan leus random",
        "1.00 Mongolian Tugrik random",
        "1.00 Mongolian tugrik random",
        "1.00 Mongolian tugriks random",
        "1.00 Moroccan Dirham random",
        "1.00 Moroccan Franc random",
        "1.00 Moroccan dirham random",
        "1.00 Moroccan dirhams random",
        "1.00 Moroccan franc random",
        "1.00 Moroccan francs random",
        "1.00 Mozambique Escudo random",
        "1.00 Mozambique Metical random",
        "1.00 Mozambique escudo random",
        "1.00 Mozambique escudos random",
        "1.00 Mozambique metical random",
        "1.00 Mozambique meticals random",
        "1.00 Mt random",
        "1.00 Myanmar Kyat random",
        "1.00 Myanmar kyat random",
        "1.00 Myanmar kyats random",
        "1.00 N$ random",
        "1.00 NAD random",
        "1.00 NAf. random",
        "1.00 NGN random",
        "1.00 NIC random",
        "1.00 NIO random",
        "1.00 NIO random",
        "1.00 NKr random",
        "1.00 NLG random",
        "1.00 NLG random",
        "1.00 NOK random",
        "1.00 NPR random",
        "1.00 NT$ random",
        "1.00 NZ$ random",
        "1.00 NZD random",
        "1.00 Namibia Dollar random",
        "1.00 Namibia dollar random",
        "1.00 Namibia dollars random",
        "1.00 Nepalese Rupee random",
        "1.00 Nepalese rupee random",
        "1.00 Nepalese rupees random",
        "1.00 Netherlands Antillan Guilder random",
        "1.00 Netherlands Antillan guilder random",
        "1.00 Netherlands Antillan guilders random",
        "1.00 Netherlands Guilder random",
        "1.00 Netherlands guilder random",
        "1.00 Netherlands guilders random",
        "1.00 New Israeli Sheqel random",
        "1.00 New Israeli Sheqels random",
        "1.00 New Zealand Dollar random",
        "1.00 New Zealand dollar random",
        "1.00 New Zealand dollars random",
        "1.00 Nicaraguan Cordoba Oro random",
        "1.00 Nicaraguan Cordoba random",
        "1.00 Nicaraguan cordoba oro random",
        "1.00 Nicaraguan cordoba oros random",
        "1.00 Nicaraguan cordoba random",
        "1.00 Nicaraguan cordobas random",
        "1.00 Nigerian Naira random",
        "1.00 Nigerian naira random",
        "1.00 Nigerian nairas random",
        "1.00 North Korean Won random",
        "1.00 North Korean won random",
        "1.00 North Korean wons random",
        "1.00 Norwegian Krone random",
        "1.00 Norwegian krone random",
        "1.00 Norwegian krones random",
        "1.00 Nrs random",
        "1.00 Nu random",
        "1.00 OMR random",
        "1.00 Old Mozambique Metical random",
        "1.00 Old Mozambique metical random",
        "1.00 Old Mozambique meticals random",
        "1.00 Old Romanian Lei random",
        "1.00 Old Romanian Leu random",
        "1.00 Old Romanian leu random",
        "1.00 Old Serbian Dinar random",
        "1.00 Old Serbian dinar random",
        "1.00 Old Serbian dinars random",
        "1.00 Old Sudanese Dinar random",
        "1.00 Old Sudanese Pound random",
        "1.00 Old Sudanese dinar random",
        "1.00 Old Sudanese dinars random",
        "1.00 Old Sudanese pound random",
        "1.00 Old Sudanese pounds random",
        "1.00 Old Turkish Lira random",
        "1.00 Old Turkish Lira random",
        "1.00 Oman Rial random",
        "1.00 Oman rial random",
        "1.00 Oman rials random",
        "1.00 P random",
        "1.00 PAB random",
        "1.00 PAB random",
        "1.00 PEI random",
        "1.00 PEI random",
        "1.00 PEN random",
        "1.00 PEN random",
        "1.00 PES random",
        "1.00 PES random",
        "1.00 PGK random",
        "1.00 PGK random",
        "1.00 PHP random",
        "1.00 PKR random",
        "1.00 PLN random",
        "1.00 PLZ random",
        "1.00 PLZ random",
        "1.00 PTE random",
        "1.00 PTE random",
        "1.00 PYG random",
        "1.00 Pakistan Rupee random",
        "1.00 Pakistan rupee random",
        "1.00 Pakistan rupees random",
        "1.00 Palladium random",
        "1.00 Palladium random",
        "1.00 Panamanian Balboa random",
        "1.00 Panamanian balboa random",
        "1.00 Panamanian balboas random",
        "1.00 Papua New Guinea Kina random",
        "1.00 Papua New Guinea kina random",
        "1.00 Papua New Guinea kinas random",
        "1.00 Paraguay Guarani random",
        "1.00 Paraguay guarani random",
        "1.00 Paraguay guaranis random",
        "1.00 Peruvian Inti random",
        "1.00 Peruvian Sol Nuevo random",
        "1.00 Peruvian Sol random",
        "1.00 Peruvian inti random",
        "1.00 Peruvian intis random",
        "1.00 Peruvian sol nuevo random",
        "1.00 Peruvian sol nuevos random",
        "1.00 Peruvian sol random",
        "1.00 Peruvian sols random",
        "1.00 Philippine Peso random",
        "1.00 Philippine peso random",
        "1.00 Philippine pesos random",
        "1.00 Platinum random",
        "1.00 Platinum random",
        "1.00 Polish Zloty (1950-1995) random",
        "1.00 Polish Zloty random",
        "1.00 Polish zloties random",
        "1.00 Polish zloty (PLZ) random",
        "1.00 Polish zloty random",
        "1.00 Polish zlotys (PLZ) random",
        "1.00 Portuguese Escudo random",
        "1.00 Portuguese Guinea Escudo random",
        "1.00 Portuguese Guinea escudo random",
        "1.00 Portuguese Guinea escudos random",
        "1.00 Portuguese escudo random",
        "1.00 Portuguese escudos random",
        "1.00 Pra random",
        "1.00 Q random",
        "1.00 QAR random",
        "1.00 QR random",
        "1.00 Qatari Rial random",
        "1.00 Qatari rial random",
        "1.00 Qatari rials random",
        "1.00 R random",
        "1.00 R$ random",
        "1.00 RD$ random",
        "1.00 RHD random",
        "1.00 RHD random",
        "1.00 RI random",
        "1.00 RINET Funds random",
        "1.00 RINET Funds random",
        "1.00 RM random",
        "1.00 RMB random",
        "1.00 RO random",
        "1.00 ROL random",
        "1.00 ROL random",
        "1.00 RON random",
        "1.00 RON random",
        "1.00 RSD random",
        "1.00 RSD random",
        "1.00 RUB random",
        "1.00 RUB random",
        "1.00 RUR random",
        "1.00 RUR random",
        "1.00 RWF random",
        "1.00 RWF random",
        "1.00 Rbl random",
        "1.00 Rhodesian Dollar random",
        "1.00 Rhodesian dollar random",
        "1.00 Rhodesian dollars random",
        "1.00 Romanian Leu random",
        "1.00 Romanian lei random",
        "1.00 Romanian leu random",
        "1.00 Rp random",
        "1.00 Russian Ruble (1991-1998) random",
        "1.00 Russian Ruble random",
        "1.00 Russian ruble (RUR) random",
        "1.00 Russian ruble random",
        "1.00 Russian rubles (RUR) random",
        "1.00 Russian rubles random",
        "1.00 Rwandan Franc random",
        "1.00 Rwandan franc random",
        "1.00 Rwandan francs random",
        "1.00 S$ random",
        "1.00 SAR random",
        "1.00 SBD random",
        "1.00 SCR random",
        "1.00 SDD random",
        "1.00 SDD random",
        "1.00 SDG random",
        "1.00 SDG random",
        "1.00 SDP random",
        "1.00 SDP random",
        "1.00 SEK random",
        "1.00 SGD random",
        "1.00 SHP random",
        "1.00 SHP random",
        "1.00 SI$ random",
        "1.00 SIT random",
        "1.00 SIT random",
        "1.00 SKK random",
        "1.00 SKr random",
        "1.00 SL Re random",
        "1.00 SLL random",
        "1.00 SLL random",
        "1.00 SOS random",
        "1.00 SR random",
        "1.00 SRD random",
        "1.00 SRD random",
        "1.00 SRG random",
        "1.00 SRl random",
        "1.00 STD random",
        "1.00 SUR random",
        "1.00 SUR random",
        "1.00 SVC random",
        "1.00 SVC random",
        "1.00 SYP random",
        "1.00 SZL random",
        "1.00 Saint Helena Pound random",
        "1.00 Saint Helena pound random",
        "1.00 Saint Helena pounds random",
        "1.00 Sao Tome and Principe Dobra random",
        "1.00 Sao Tome and Principe dobra random",
        "1.00 Sao Tome and Principe dobras random",
        "1.00 Saudi Riyal random",
        "1.00 Saudi riyal random",
        "1.00 Saudi riyals random",
        "1.00 Serbian Dinar random",
        "1.00 Serbian dinar random",
        "1.00 Serbian dinars random",
        "1.00 Seychelles Rupee random",
        "1.00 Seychelles rupee random",
        "1.00 Seychelles rupees random",
        "1.00 Sf random",
        "1.00 Sh. random",
        "1.00 Sierra Leone Leone random",
        "1.00 Sierra Leone leone random",
        "1.00 Sierra Leone leones random",
        "1.00 Silver random",
        "1.00 Silver random",
        "1.00 Singapore Dollar random",
        "1.00 Singapore dollar random",
        "1.00 Singapore dollars random",
        "1.00 Sk random",
        "1.00 Slovak Koruna random",
        "1.00 Slovak koruna random",
        "1.00 Slovak korunas random",
        "1.00 Slovenia Tolar random",
        "1.00 Slovenia tolar random",
        "1.00 Slovenia tolars random",
        "1.00 Solomon Islands Dollar random",
        "1.00 Solomon Islands dollar random",
        "1.00 Solomon Islands dollars random",
        "1.00 Somali Shilling random",
        "1.00 Somali shilling random",
        "1.00 Somali shillings random",
        "1.00 South African Rand (financial) random",
        "1.00 South African Rand random",
        "1.00 South African rand (financial) random",
        "1.00 South African rand random",
        "1.00 South African rands (financial) random",
        "1.00 South African rands random",
        "1.00 South Korean Won random",
        "1.00 South Korean won random",
        "1.00 South Korean wons random",
        "1.00 Soviet Rouble random",
        "1.00 Soviet rouble random",
        "1.00 Soviet roubles random",
        "1.00 Spanish Peseta (A account) random",
        "1.00 Spanish Peseta (convertible account) random",
        "1.00 Spanish Peseta random",
        "1.00 Spanish peseta (A account) random",
        "1.00 Spanish peseta (convertible account) random",
        "1.00 Spanish peseta random",
        "1.00 Spanish pesetas (A account) random",
        "1.00 Spanish pesetas (convertible account) random",
        "1.00 Spanish pesetas random",
        "1.00 Special Drawing Rights random",
        "1.00 Sri Lanka Rupee random",
        "1.00 Sri Lanka rupee random",
        "1.00 Sri Lanka rupees random",
        "1.00 Sudanese Pound random",
        "1.00 Sudanese pound random",
        "1.00 Sudanese pounds random",
        "1.00 Surinam Dollar random",
        "1.00 Surinam dollar random",
        "1.00 Surinam dollars random",
        "1.00 Suriname Guilder random",
        "1.00 Suriname guilder random",
        "1.00 Suriname guilders random",
        "1.00 Swaziland Lilangeni random",
        "1.00 Swaziland lilangeni random",
        "1.00 Swaziland lilangenis random",
        "1.00 Swedish Krona random",
        "1.00 Swedish krona random",
        "1.00 Swedish kronas random",
        "1.00 Swiss Franc random",
        "1.00 Swiss franc random",
        "1.00 Swiss francs random",
        "1.00 Syrian Pound random",
        "1.00 Syrian pound random",
        "1.00 Syrian pounds random",
        "1.00 T Sh random",
        "1.00 T random",
        "1.00 T$ random",
        "1.00 THB random",
        "1.00 TJR random",
        "1.00 TJR random",
        "1.00 TJS random",
        "1.00 TJS random",
        "1.00 TL random",
        "1.00 TMM random",
        "1.00 TMM random",
        "1.00 TND random",
        "1.00 TND random",
        "1.00 TOP random",
        "1.00 TPE random",
        "1.00 TPE random",
        "1.00 TRL random",
        "1.00 TRY random",
        "1.00 TRY random",
        "1.00 TT$ random",
        "1.00 TTD random",
        "1.00 TWD random",
        "1.00 TZS random",
        "1.00 Taiwan New Dollar random",
        "1.00 Taiwan dollar random",
        "1.00 Taiwan dollars random",
        "1.00 Tajikistan Ruble random",
        "1.00 Tajikistan Somoni random",
        "1.00 Tajikistan ruble random",
        "1.00 Tajikistan rubles random",
        "1.00 Tajikistan somoni random",
        "1.00 Tajikistan somonis random",
        "1.00 Tanzanian Shilling random",
        "1.00 Tanzanian shilling random",
        "1.00 Tanzanian shillings random",
        "1.00 Testing Currency Code random",
        "1.00 Testing Currency Code random",
        "1.00 Thai Baht random",
        "1.00 Thai baht random",
        "1.00 Thai bahts random",
        "1.00 Timor Escudo random",
        "1.00 Timor escudo random",
        "1.00 Timor escudos random",
        "1.00 Tk random",
        "1.00 Tonga Paʻanga random",
        "1.00 Tonga paʻanga random",
        "1.00 Tonga paʻangas random",
        "1.00 Trinidad and Tobago Dollar random",
        "1.00 Trinidad and Tobago dollar random",
        "1.00 Trinidad and Tobago dollars random",
        "1.00 Tunisian Dinar random",
        "1.00 Tunisian dinar random",
        "1.00 Tunisian dinars random",
        "1.00 Turkish Lira random",
        "1.00 Turkish Lira random",
        "1.00 Turkish lira random",
        "1.00 Turkmenistan Manat random",
        "1.00 Turkmenistan manat random",
        "1.00 Turkmenistan manats random",
        "1.00 U Sh random",
        "1.00 UAE dirham random",
        "1.00 UAE dirhams random",
        "1.00 UAH random",
        "1.00 UAK random",
        "1.00 UAK random",
        "1.00 UGS random",
        "1.00 UGS random",
        "1.00 UGX random",
        "1.00 UM random",
        "1.00 US Dollar (Next day) random",
        "1.00 US Dollar (Same day) random",
        "1.00 US Dollar random",
        "1.00 US dollar (next day) random",
        "1.00 US dollar (same day) random",
        "1.00 US dollar random",
        "1.00 US dollars (next day) random",
        "1.00 US dollars (same day) random",
        "1.00 US dollars random",
        "1.00 USD random",
        "1.00 USN random",
        "1.00 USN random",
        "1.00 USS random",
        "1.00 USS random",
        "1.00 UYI random",
        "1.00 UYI random",
        "1.00 UYP random",
        "1.00 UYP random",
        "1.00 UYU random",
        "1.00 UZS random",
        "1.00 UZS random",
        "1.00 Ugandan Shilling (1966-1987) random",
        "1.00 Ugandan Shilling random",
        "1.00 Ugandan shilling (UGS) random",
        "1.00 Ugandan shilling random",
        "1.00 Ugandan shillings (UGS) random",
        "1.00 Ugandan shillings random",
        "1.00 Ukrainian Hryvnia random",
        "1.00 Ukrainian Karbovanetz random",
        "1.00 Ukrainian hryvnia random",
        "1.00 Ukrainian hryvnias random",
        "1.00 Ukrainian karbovanetz random",
        "1.00 Ukrainian karbovanetzs random",
        "1.00 Unidad de Valor Real random",
        "1.00 United Arab Emirates Dirham random",
        "1.00 Unknown or Invalid Currency random",
        "1.00 Ur$ random",
        "1.00 Uruguay Peso (1975-1993) random",
        "1.00 Uruguay Peso Uruguayo random",
        "1.00 Uruguay Peso en Unidades Indexadas random",
        "1.00 Uruguay peso (UYP) random",
        "1.00 Uruguay peso en unidades indexadas random",
        "1.00 Uruguay peso random",
        "1.00 Uruguay pesos (UYP) random",
        "1.00 Uruguay pesos en unidades indexadas random",
        "1.00 Uzbekistan Sum random",
        "1.00 Uzbekistan sum random",
        "1.00 Uzbekistan sums random",
        "1.00 VEB random",
        "1.00 VEF random",
        "1.00 VND random",
        "1.00 VT random",
        "1.00 VUV random",
        "1.00 Vanuatu Vatu random",
        "1.00 Vanuatu vatu random",
        "1.00 Vanuatu vatus random",
        "1.00 Venezuelan Bolivar Fuerte random",
        "1.00 Venezuelan Bolivar random",
        "1.00 Venezuelan bolivar fuerte random",
        "1.00 Venezuelan bolivar fuertes random",
        "1.00 Venezuelan bolivar random",
        "1.00 Venezuelan bolivars random",
        "1.00 Vietnamese Dong random",
        "1.00 Vietnamese dong random",
        "1.00 Vietnamese dongs random",
        "1.00 WIR Euro random",
        "1.00 WIR Franc random",
        "1.00 WIR euro random",
        "1.00 WIR euros random",
        "1.00 WIR franc random",
        "1.00 WIR francs random",
        "1.00 WST random",
        "1.00 WST random",
        "1.00 Western Samoa Tala random",
        "1.00 Western Samoa tala random",
        "1.00 Western Samoa talas random",
        "1.00 XAF random",
        "1.00 XAF random",
        "1.00 XAG random",
        "1.00 XAG random",
        "1.00 XAU random",
        "1.00 XAU random",
        "1.00 XBA random",
        "1.00 XBA random",
        "1.00 XBB random",
        "1.00 XBB random",
        "1.00 XBC random",
        "1.00 XBC random",
        "1.00 XBD random",
        "1.00 XBD random",
        "1.00 XCD random",
        "1.00 XDR random",
        "1.00 XDR random",
        "1.00 XEU random",
        "1.00 XEU random",
        "1.00 XFO random",
        "1.00 XFO random",
        "1.00 XFU random",
        "1.00 XFU random",
        "1.00 XOF random",
        "1.00 XOF random",
        "1.00 XPD random",
        "1.00 XPD random",
        "1.00 XPF random",
        "1.00 XPT random",
        "1.00 XPT random",
        "1.00 XRE random",
        "1.00 XRE random",
        "1.00 XTS random",
        "1.00 XTS random",
        "1.00 XXX random",
        "1.00 XXX random",
        "1.00 YDD random",
        "1.00 YDD random",
        "1.00 YER random",
        "1.00 YRl random",
        "1.00 YUD random",
        "1.00 YUD random",
        "1.00 YUM random",
        "1.00 YUM random",
        "1.00 YUN random",
        "1.00 YUN random",
        "1.00 Yemeni Dinar random",
        "1.00 Yemeni Rial random",
        "1.00 Yemeni dinar random",
        "1.00 Yemeni dinars random",
        "1.00 Yemeni rial random",
        "1.00 Yemeni rials random",
        "1.00 Yugoslavian Convertible Dinar random",
        "1.00 Yugoslavian Hard Dinar random",
        "1.00 Yugoslavian Noviy Dinar random",
        "1.00 Yugoslavian Noviy dinars random",
        "1.00 Yugoslavian convertible dinar random",
        "1.00 Yugoslavian convertible dinars random",
        "1.00 Yugoslavian hard dinar random",
        "1.00 Yugoslavian hard dinars random",
        "1.00 Yugoslavian noviy dinar random",
        "1.00 Z$ random",
        "1.00 ZAL random",
        "1.00 ZAL random",
        "1.00 ZAR random",
        "1.00 ZMK random",
        "1.00 ZMK random",
        "1.00 ZRN random",
        "1.00 ZRN random",
        "1.00 ZRZ random",
        "1.00 ZRZ random",
        "1.00 ZWD random",
        "1.00 Zairean New Zaire random",
        "1.00 Zairean Zaire random",
        "1.00 Zairean new zaire random",
        "1.00 Zairean new zaires random",
        "1.00 Zairean zaire random",
        "1.00 Zairean zaires random",
        "1.00 Zambian Kwacha random",
        "1.00 Zambian kwacha random",
        "1.00 Zambian kwachas random",
        "1.00 Zimbabwe Dollar random",
        "1.00 Zimbabwe dollar random",
        "1.00 Zimbabwe dollars random",
        "1.00 dram random",
        "1.00 ekwele random",
        "1.00 ekweles random",
        "1.00 euro random",
        "1.00 euros random",
        "1.00 lari random",
        "1.00 lek random",
        "1.00 lev random",
        "1.00 maloti random",
        "1.00 malotis random",
        "1.00 man. random",
        "1.00 old Turkish lira random",
        "1.00 som random",
        "1.00 special drawing rights random",
        "1.00 unidad de valor real random",
        "1.00 unidad de valor reals random",
        "1.00 unknown/invalid currency random",
        "1.00 z\\u0142 random",
        "1.00 \\u00a3 random",
        "1.00 \\u00a3C random",
        "1.00 \\u00a5 random",
        "1.00 \\u0e3f random",
        "1.00 \\u20ab random",
        "1.00 \\u20a1 random",
        "1.00 \\u20a7 random",
        "1.00 \\u20aa random",
        "1.00 \\u20ac random",
        "1.00 \\u20a8 random",
        "1.00 \\u20a6 random",
        "1.00 \\u20ae random",
        "1.00 \\u20a4 random",
        // for GHS
        // for PHP
        // for PYG
        // for UAH
    };

    const char* WRONG_DATA[] = {
        // Following are missing one last char in the currency name
         "1.00 US dolla",
         "1.00 ",
         "1.00 A",
         "1.00 AD",
         "1.00 AD",
         "1.00 AE",
         "1.00 AE",
         "1.00 AF",
         "1.00 AF",
         "1.00 AF",
         "1.00 AL",
         "1.00 AM",
         "1.00 AN",
         "1.00 AO",
         "1.00 AO",
         "1.00 AO",
         "1.00 AO",
         "1.00 AO",
         "1.00 AO",
         "1.00 AO",
         "1.00 AR",
         "1.00 AR",
         "1.00 AR",
         "1.00 AR",
         "1.00 AR",
         "1.00 AR",
         "1.00 AT",
         "1.00 AT",
         "1.00 AU",
         "1.00 AW",
         "1.00 AZ",
         "1.00 AZ",
         "1.00 AZ",
         "1.00 A",
         "1.00 Albanian Le",
         "1.00 Albanian le",
         "1.00 Algerian Dina",
         "1.00 Algerian dina",
         "1.00 Andorran Peset",
         "1.00 Andorran peset",
         "1.00 Angolan Kwanz",
         "1.00 Angolan kwanz",
         "1.00 Argentine Austra",
         "1.00 Argentine Pes",
         "1.00 Argentine austra",
         "1.00 Argentine pes",
         "1.00 Armenian Dra",
         "1.00 Armenian dra",
         "1.00 Aruban Flori",
         "1.00 Aruban flori",
         "1.00 Australian Dolla",
         "1.00 Australian dolla",
         "1.00 Austrian Schillin",
         "1.00 Austrian schillin",
         "1.00 Azerbaijanian Mana",
         "1.00 Azerbaijanian mana",
         "1.00 B",
         "1.00 BA",
         "1.00 BA",
         "1.00 BA",
         "1.00 BB",
         "1.00 B",
         "1.00 BE",
         "1.00 BE",
         "1.00 BE",
         "1.00 BE",
         "1.00 BE",
         "1.00 B",
         "1.00 BG",
         "1.00 BG",
         "1.00 BG",
         "1.00 BH",
         "1.00 BI",
         "1.00 BM",
         "1.00 BN",
         "1.00 BO",
         "1.00 BO",
         "1.00 BO",
         "1.00 BO",
         "1.00 BO",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BR",
         "1.00 BS",
         "1.00 BS",
         "1.00 BT",
         "1.00 BU",
         "1.00 BU",
         "1.00 BW",
         "1.00 BY",
         "1.00 BY",
         "1.00 BY",
         "1.00 BZ",
         "1.00 BZ",
         "1.00 Bahamian Dolla",
         "1.00 Bahamian dolla",
         "1.00 Bahraini Dina",
         "1.00 Bahraini dina",
         "1.00 Bangladeshi Tak",
         "1.00 Bangladeshi tak",
         "1.00 Barbados Dolla",
         "1.00 Barbados dolla",
         "1.00 Bds",
         "1.00 B",
         "1.00 Bhutan Ngultru",
         "1.00 Bhutan ngultru",
         "1.00 Bolivian Mvdo",
         "1.00 Bolivian Pes",
         "1.00 Bolivian mvdo",
         "1.00 Bolivian pes",
         "1.00 Bolivian",
         "1.00 Bolivian",
         "1.00 Bosnia-Herzegovina Convertible Mar",
         "1.00 Bosnia-Herzegovina Dina",
         "1.00 Bosnia-Herzegovina convertible mar",
         "1.00 Bosnia-Herzegovina dina",
         "1.00 Botswanan Pul",
         "1.00 Botswanan pul",
         "1.00 B",
         "1.00 Bulgarian Hard Le",
         "1.00 Bulgarian Le",
         "1.00 Bulgarian hard le",
         "1.00 Bulgarian le",
         "1.00 Burmese Kya",
         "1.00 Burmese kya",
         "1.00 Burundi Fran",
         "1.00 Burundi fran",
         "1.00 C",
         "1.00 CA",
         "1.00 CA",
         "1.00 CD",
         "1.00 CD",
         "1.00 C",
         "1.00 CH",
         "1.00 CH",
         "1.00 CH",
         "1.00 CH",
         "1.00 CH",
         "1.00 CL",
         "1.00 CL",
         "1.00 CL",
         "1.00 CL",
         "1.00 CN",
         "1.00 CO",
         "1.00 CO",
         "1.00 CO",
         "1.00 CO",
         "1.00 C",
         "1.00 CS",
         "1.00 CS",
         "1.00 CS",
         "1.00 CS",
         "1.00 CU",
         "1.00 CU",
         "1.00 CV",
         "1.00 CY",
         "1.00 CZ",
         "1.00 Cambodian Rie",
         "1.00 Cambodian rie",
         "1.00 Canadian Dolla",
         "1.00 Canadian dolla",
         "1.00 Cape Verde Escud",
         "1.00 Cape Verde escud",
         "1.00 Cayman Islands Dolla",
         "1.00 Cayman Islands dolla",
         "1.00 Chilean Pes",
         "1.00 Chilean Unidades de Foment",
         "1.00 Chilean pes",
         "1.00 Chilean unidades de foment",
         "1.00 Chinese Yuan Renminb",
         "1.00 Chinese yua",
         "1.00 Colombian Pes",
         "1.00 Colombian pes",
         "1.00 Comoro Fran",
         "1.00 Comoro fran",
         "1.00 Congolese Franc Congolai",
         "1.00 Congolese franc Congolai",
         "1.00 Congolese francs Congolai",
         "1.00 Costa Rican Colo",
         "1.00 Costa Rican colo",
         "1.00 Croatian Dina",
         "1.00 Croatian Kun",
         "1.00 Croatian dina",
         "1.00 Croatian kun",
         "1.00 Cuban Pes",
         "1.00 Cuban pes",
         "1.00 Cyprus Poun",
         "1.00 Cyprus poun",
         "1.00 Czech Republic Korun",
         "1.00 Czech Republic korun",
         "1.00 Czechoslovak Hard Korun",
         "1.00 Czechoslovak hard korun",
         "1.00 D",
         "1.00 DD",
         "1.00 DD",
         "1.00 DE",
         "1.00 DE",
         "1.00 DJ",
         "1.00 DK",
         "1.00 DO",
         "1.00 DZ",
         "1.00 Danish Kron",
         "1.00 Danish kron",
         "1.00 D",
         "1.00 Deutsche Mar",
         "1.00 Deutsche mar",
         "1.00 Djibouti Fran",
         "1.00 Djibouti fran",
         "1.00 Dk",
         "1.00 Dominican Pes",
         "1.00 Dominican pes",
         "1.00 F",
         "1.00 FB",
         "1.00 FI",
         "1.00 FI",
         "1.00 FJ",
         "1.00 FK",
         "1.00 FK",
         "1.00 FR",
         "1.00 FR",
         "1.00 Falkland Islands Poun",
         "1.00 Falkland Islands poun",
         "1.00 Fd",
         "1.00 Fiji Dolla",
         "1.00 Fiji dolla",
         "1.00 Finnish Markk",
         "1.00 Finnish markk",
         "1.00 Fr",
         "1.00 French Fran",
         "1.00 French Gold Fran",
         "1.00 French UIC-Fran",
         "1.00 French UIC-fran",
         "1.00 French fran",
         "1.00 French gold fran",
         "1.00 F",
         "1.00 G",
         "1.00 GB",
         "1.00 GE",
         "1.00 GE",
         "1.00 GE",
         "1.00 G",
         "1.00 GH",
         "1.00 GH",
         "1.00 GH",
         "1.00 GI",
         "1.00 GI",
         "1.00 GM",
         "1.00 GM",
         "1.00 GN",
         "1.00 GN",
         "1.00 GN",
         "1.00 GQ",
         "1.00 GQ",
         "1.00 GR",
         "1.00 GR",
         "1.00 GT",
         "1.00 GW",
         "1.00 GW",
         "1.00 GW",
         "1.00 GW",
         "1.00 GY",
         "1.00 Gambia Dalas",
         "1.00 Gambia dalas",
         "1.00 Georgian Kupon Lari",
         "1.00 Georgian Lar",
         "1.00 Georgian kupon lari",
         "1.00 Georgian lar",
         "1.00 Ghana Ced",
         "1.00 Ghana ced",
         "1.00 Gibraltar Poun",
         "1.00 Gibraltar poun",
         "1.00 Gol",
         "1.00 Gol",
         "1.00 Greek Drachm",
         "1.00 Greek drachm",
         "1.00 Guatemala Quetza",
         "1.00 Guatemala quetza",
         "1.00 Guinea Fran",
         "1.00 Guinea Syl",
         "1.00 Guinea fran",
         "1.00 Guinea syl",
         "1.00 Guinea-Bissau Pes",
         "1.00 Guinea-Bissau pes",
         "1.00 Guyana Dolla",
         "1.00 Guyana dolla",
         "1.00 HK",
         "1.00 HK",
         "1.00 HN",
         "1.00 HR",
         "1.00 HR",
         "1.00 HR",
         "1.00 HR",
         "1.00 HT",
         "1.00 HT",
         "1.00 HU",
         "1.00 Haitian Gourd",
         "1.00 Haitian gourd",
         "1.00 Honduras Lempir",
         "1.00 Honduras lempir",
         "1.00 Hong Kong Dolla",
         "1.00 Hong Kong dolla",
         "1.00 Hungarian Forin",
         "1.00 Hungarian forin",
         "1.00 I",
         "1.00 IE",
         "1.00 IL",
         "1.00 IL",
         "1.00 IL",
         "1.00 IN",
         "1.00 IQ",
         "1.00 IR",
         "1.00 IR\\u00a",
         "1.00 IS",
         "1.00 IS",
         "1.00 IT",
         "1.00 Icelandic Kron",
         "1.00 Icelandic kron",
         "1.00 Indian Rupe",
         "1.00 Indian rupe",
         "1.00 Indonesian Rupia",
         "1.00 Indonesian rupia",
         "1.00 Iranian Ria",
         "1.00 Iranian ria",
         "1.00 Iraqi Dina",
         "1.00 Iraqi dina",
         "1.00 Irish Poun",
         "1.00 Irish poun",
         "1.00 Israeli Poun",
         "1.00 Israeli new sheqe",
         "1.00 Israeli poun",
         "1.00 Italian Lir",
         "1.00 Italian lir",
         "1.00 J",
         "1.00 J",
         "1.00 JM",
         "1.00 JO",
         "1.00 JP",
         "1.00 Jamaican Dolla",
         "1.00 Jamaican dolla",
         "1.00 Japanese Ye",
         "1.00 Japanese ye",
         "1.00 Jordanian Dina",
         "1.00 Jordanian dina",
         "1.00 K S",
         "1.00 K",
         "1.00 KE",
         "1.00 KG",
         "1.00 KH",
         "1.00 K",
         "1.00 KP",
         "1.00 KP",
         "1.00 KR",
         "1.00 KW",
         "1.00 KY",
         "1.00 KY",
         "1.00 KZ",
         "1.00 Kazakhstan Teng",
         "1.00 Kazakhstan teng",
         "1.00 Kenyan Shillin",
         "1.00 Kenyan shillin",
         "1.00 Kuwaiti Dina",
         "1.00 Kuwaiti dina",
         "1.00 Kyrgystan So",
         "1.00 Kyrgystan so",
         "1.00 K",
         "1.00 K\\u010",
         "1.00 N",
         "1.00 NA",
         "1.00 NAf",
         "1.00 NG",
         "1.00 NI",
         "1.00 NI",
         "1.00 NI",
         "1.00 NK",
         "1.00 NL",
         "1.00 NL",
         "1.00 NO",
         "1.00 NP",
         "1.00 NT",
         "1.00 NZ",
         "1.00 NZ",
         "1.00 Namibia Dolla",
         "1.00 Namibia dolla",
         "1.00 Nepalese Rupe",
         "1.00 Nepalese rupe",
         "1.00 Netherlands Antillan Guilde",
         "1.00 Netherlands Antillan guilde",
         "1.00 Netherlands Guilde",
         "1.00 Netherlands guilde",
         "1.00 New Israeli Sheqe",
         "1.00 New Zealand Dolla",
         "1.00 New Zealand dolla",
         "1.00 Nicaraguan Cordob",
         "1.00 Nicaraguan cordob",
         "1.00 Nigerian Nair",
         "1.00 Nigerian nair",
         "1.00 North Korean Wo",
         "1.00 North Korean wo",
         "1.00 Norwegian Kron",
         "1.00 Norwegian kron",
         "1.00 Nr",
         "1.00 N",
         "1.00 OM",
         "1.00 Old Mozambique Metica",
         "1.00 Old Mozambique metica",
         "1.00 Old Romanian Le",
         "1.00 Old Romanian Le",
         "1.00 Old Romanian le",
         "1.00 Old Serbian Dina",
         "1.00 Old Serbian dina",
         "1.00 Old Sudanese Dina",
         "1.00 Old Sudanese Poun",
         "1.00 Old Sudanese dina",
         "1.00 Old Sudanese poun",
         "1.00 Old Turkish Lir",
         "1.00 Old Turkish Lir",
         "1.00 Oman Ria",
         "1.00 Oman ria",
         "1.00 S",
         "1.00 SA",
         "1.00 SB",
         "1.00 SC",
         "1.00 SD",
         "1.00 SD",
         "1.00 SD",
         "1.00 SD",
         "1.00 SD",
         "1.00 SD",
         "1.00 SE",
         "1.00 SG",
         "1.00 SH",
         "1.00 SH",
         "1.00 SI",
         "1.00 SI",
         "1.00 SI",
         "1.00 SK",
         "1.00 SK",
         "1.00 SL R",
         "1.00 SL",
         "1.00 SL",
         "1.00 SO",
         "1.00 S",
         "1.00 ST",
         "1.00 SU",
         "1.00 SV",
         "1.00 SY",
         "1.00 SZ",
         "1.00 Saint Helena Poun",
         "1.00 Saint Helena poun",
         "1.00 Sao Tome and Principe Dobr",
         "1.00 Sao Tome and Principe dobr",
         "1.00 Saudi Riya",
         "1.00 Saudi riya",
         "1.00 Serbian Dina",
         "1.00 Serbian dina",
         "1.00 Seychelles Rupe",
         "1.00 Seychelles rupe",
         "1.00 S",
         "1.00 Sh",
         "1.00 Sierra Leone Leon",
         "1.00 Sierra Leone leon",
         "1.00 Silve",
         "1.00 Silve",
         "1.00 Singapore Dolla",
         "1.00 Singapore dolla",
         "1.00 S",
         "1.00 Slovak Korun",
         "1.00 Slovak korun",
         "1.00 Slovenia Tola",
         "1.00 Slovenia tola",
         "1.00 Solomon Islands Dolla",
         "1.00 Solomon Islands dolla",
         "1.00 Somali Shillin",
         "1.00 Somali shillin",
         "1.00 South African Ran",
         "1.00 South African ran",
         "1.00 South Korean Wo",
         "1.00 South Korean wo",
         "1.00 Soviet Roubl",
         "1.00 Soviet roubl",
         "1.00 Spanish Peset",
         "1.00 Spanish peset",
         "1.00 Special Drawing Right",
         "1.00 Sri Lanka Rupe",
         "1.00 Sri Lanka rupe",
         "1.00 Sudanese Poun",
         "1.00 Sudanese poun",
         "1.00 Surinam Dolla",
         "1.00 Surinam dolla",
         "1.00 Suriname Guilde",
         "1.00 Suriname guilde",
         "1.00 Swaziland Lilangen",
         "1.00 Swaziland lilangen",
         "1.00 Swedish Kron",
         "1.00 Swedish kron",
         "1.00 Swiss Fran",
         "1.00 Swiss fran",
         "1.00 Syrian Poun",
         "1.00 Syrian poun",
         "1.00 U S",
         "1.00 UAE dirha",  
         "1.00 UA",
         "1.00 UA",
         "1.00 UA",
         "1.00 UG",
         "1.00 UG",
         "1.00 UG",
         "1.00 U",
         "1.00 US Dolla",
         "1.00 US dolla",
         "1.00 US",
         "1.00 US",
         "1.00 US",
         "1.00 US",
         "1.00 US",
         "1.00 UY",
         "1.00 UY",
         "1.00 UY",
         "1.00 UY",
         "1.00 UY",
         "1.00 UZ",
         "1.00 UZ",
         "1.00 Ugandan Shillin",
         "1.00 Ugandan shillin",
         "1.00 Ukrainian Hryvni",
         "1.00 Ukrainian Karbovanet",
         "1.00 Ukrainian hryvni",
         "1.00 Ukrainian karbovanet",
         "1.00 Unidad de Valor Rea",
         "1.00 United Arab Emirates Dirha",
         "1.00 Unknown or Invalid Currenc",
         "1.00 Ur",
         "1.00 Uruguay Peso Uruguay",
         "1.00 Uruguay pes",
         "1.00 Uzbekistan Su",
         "1.00 Uzbekistan su",
         "1.00 VE",
         "1.00 VE",
         "1.00 VN",
         "1.00 V",
         "1.00 VU",
         "1.00 Vanuatu Vat",
         "1.00 Vanuatu vat",
         "1.00 Venezuelan Boliva",
         "1.00 Venezuelan boliva",
         "1.00 Vietnamese Don",
         "1.00 Vietnamese don",
         "1.00 WIR Eur",
         "1.00 WIR Fran",
         "1.00 WIR eur",
         "1.00 WIR fran",
         "1.00 WS",
         "1.00 WS",
         "1.00 Western Samoa Tal",
         "1.00 Western Samoa tal",
         "1.00 XA",
         "1.00 XA",
         "1.00 XA",
         "1.00 XA",
         "1.00 XA",
         "1.00 XA",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XB",
         "1.00 XC",
         "1.00 XD",
         "1.00 XD",
         "1.00 XE",
         "1.00 XE",
         "1.00 XF",
         "1.00 XF",
         "1.00 XF",
         "1.00 XF",
         "1.00 XO",
         "1.00 XO",
         "1.00 XP",
         "1.00 XP",
         "1.00 XP",
         "1.00 XP",
         "1.00 XP",
         "1.00 XR",
         "1.00 XR",
         "1.00 XT",
         "1.00 XT",
         "1.00 XX",
         "1.00 XX",
         "1.00 YD",
         "1.00 YD",
         "1.00 YE",
         "1.00 YR",
         "1.00 YU",
         "1.00 YU",
         "1.00 YU",
         "1.00 YU",
         "1.00 YU",
         "1.00 YU",
         "1.00 Yemeni Dina",
         "1.00 Yemeni Ria",
         "1.00 Yemeni dina",
         "1.00 Yemeni ria",
         "1.00 Yugoslavian Convertible Dina",
         "1.00 Yugoslavian Hard Dina",
         "1.00 Yugoslavian Noviy Dina",
         "1.00 Yugoslavian Noviy dinar",
         "1.00 Yugoslavian convertible dina",
         "1.00 Yugoslavian hard dina",
         "1.00 Yugoslavian noviy dina",
         "1.00 Z",
         "1.00 ZA",
         "1.00 ZA",
         "1.00 ZA",
         "1.00 ZM",
         "1.00 ZM",
         "1.00 ZR",
         "1.00 ZR",
         "1.00 ZR",
         "1.00 ZR",
         "1.00 ZW",
         "1.00 Zairean New Zair",
         "1.00 Zairean Zair",
         "1.00 Zairean new zair",
         "1.00 Zairean zair",
         "1.00 Zambian Kwach",
         "1.00 Zambian kwach",
         "1.00 Zimbabwe Dolla",
         "1.00 Zimbabwe dolla",
         "1.00 dra",
         "1.00 ekwel",
         "1.00 eur",
         "1.00 lar",
         "1.00 le",
         "1.00 le",
         "1.00 malot",
         "1.00 man",
         "1.00 old Turkish lir",
         "1.00 so",
         "1.00 special drawing right",
         "1.00 unidad de valor rea",
         "1.00 unknown/invalid currenc",
         "1.00 z",
    };

    Locale locale("en_US");
    for (uint32_t i=0; i<sizeof(DATA)/sizeof(DATA[0]); ++i) {
      UnicodeString formatted = ctou(DATA[i]);
      UErrorCode status = U_ZERO_ERROR;
      NumberFormat* numFmt = NumberFormat::createInstance(locale, NumberFormat::kCurrencyStyle, status);
      Formattable parseResult;
      numFmt->parse(formatted, parseResult, status);
      if (U_FAILURE(status) ||
          (parseResult.getType() == Formattable::kDouble &&
           parseResult.getDouble() != 1.0)) {
          errln("wrong parsing, " + formatted);
          errln("data: " + formatted); 
      }
      delete numFmt;
    }

    for (uint32_t i=0; i<sizeof(WRONG_DATA)/sizeof(WRONG_DATA[0]); ++i) {
      UnicodeString formatted = ctou(WRONG_DATA[i]);
      UErrorCode status = U_ZERO_ERROR;
      NumberFormat* numFmt = NumberFormat::createInstance(locale, NumberFormat::kCurrencyStyle, status);
      Formattable parseResult;
      numFmt->parse(formatted, parseResult, status);
      if (!U_FAILURE(status) ||
          (parseResult.getType() == Formattable::kDouble &&
           parseResult.getDouble() == 1.0)) {
          errln("parsed but should not be: " + formatted); 
          errln("data: " + formatted);
      }
      delete numFmt;
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
