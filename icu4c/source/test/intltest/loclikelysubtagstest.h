// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "intltest.h"

#include "unicode/utypes.h"
#include "lsr.h"

U_NAMESPACE_BEGIN
class XLikelySubtags;
U_NAMESPACE_END


/**
 * Tests for the XLikelySubtags class
 **/
class XLikelySubtagsTest: public IntlTest {
public:
    XLikelySubtagsTest();
    virtual ~XLikelySubtagsTest();

    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL ) override;

    void TestBasic(void);

    void assertLSR(UnicodeString msg, const LSR& expected, const LSR& actual);

private:
    const XLikelySubtags* service;
};
