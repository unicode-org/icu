#include "utypes.h"
#include "usettest.h"
#include "uniset.h"

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break;

void
UnicodeSetTest::runIndexedTest(int32_t index, bool_t exec,
                               char* &name, char* par) {
    // if (exec) logln((UnicodeString)"TestSuite UnicodeSetTest");
    switch (index) {
        CASE(0,Test1)

        default: name = ""; break;
    }
}

void
UnicodeSetTest::Test1() {
	UErrorCode status = U_ZERO_ERROR;

    UnicodeSet set; // Construct empty set
    set.add('a', 'z');
    expect(set, "az");
    set.remove('m', 'p');
    expect(set, "alqz");
    set.remove('e', 'g');
    expect(set, "adhlqz");
    set.remove('d', 'i');
    expect(set, "acjlqz");
    set.remove('c', 'r');
    expect(set, "absz");
    set.add('f', 'q');
    expect(set, "abfqsz");
    set.remove('a', 'g');
    expect(set, "hqsz");
    set.remove('a', 'z');
    expect(set, "");

    set.applyPattern("[[a-m]&[d-z]&[k-y]]", status);
	if (U_FAILURE(status)) {
		errln("FAIL: Unexpected pattern parse failure");
		return;
	}
    expect(set, "km");

    set.applyPattern("[[a-z]-[m-y]-[d-r]]", status);
	if (U_FAILURE(status)) {
		errln("FAIL: Unexpected pattern parse failure");
		return;
	}
    expect(set, "aczz");

    set.applyPattern("[a\\-z]", status);
	if (U_FAILURE(status)) {
		errln("FAIL: Unexpected pattern parse failure");
		return;
	}
    expect(set, "--aazz");
    set.applyPattern("[-az]", status);
	if (U_FAILURE(status)) {
		errln("FAIL: Unexpected pattern parse failure");
		return;
	}
    expect(set, "--aazz");
    set.applyPattern("[az-]", status);
	if (U_FAILURE(status)) {
		errln("FAIL: Unexpected pattern parse failure");
		return;
	}
    expect(set, "--aazz");
}

void
UnicodeSetTest::expect(const UnicodeSet& set, const UnicodeString& expectedPairs) {
    if (set.getPairs() != expectedPairs) {
        errln(UnicodeString("FAIL: Expected pair list \"") +
              escape(expectedPairs) + "\", got \"" +
              escape(set.getPairs()) + '"');
    }
}

static char toHexString(int32_t i) { return i + (i < 10 ? '0' : ('A' - 10)); }

UnicodeString
UnicodeSetTest::escape(const UnicodeString& s) {
    UnicodeString buf;
    for (int32_t i=0; i<s.size(); ++i)
    {
        UChar c = s[(UTextOffset)i];
        if (c <= (UChar)0x7F) buf += c;
        else {
            buf += '\\'; buf += 'U';
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return buf;
}
