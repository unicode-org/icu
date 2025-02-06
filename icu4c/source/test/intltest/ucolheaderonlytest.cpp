// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// Test header-only ICU C++ APIs. Do not use other ICU C++ APIs.
// Non-default configuration:
#define U_SHOW_CPLUSPLUS_API 0

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include <string_view>

#include "unicode/ucol.h"

#include "intltest.h"

namespace {

class UColHeaderOnlyTest : public IntlTest {
public:
    UColHeaderOnlyTest() = default;
    void runIndexedTest(int32_t index, UBool exec, const char*& name, char* par = nullptr) override;
    void TestPredicateTypes();
};

void UColHeaderOnlyTest::runIndexedTest(int32_t index, UBool exec, const char*& name, char* /*par*/) {
    if (exec) {
        logln("TestSuite UColHeaderOnlyTest: ");
    }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestPredicateTypes);
    TESTCASE_AUTO_END;
}

class UCollatorWrapper {
public:
    UCollatorWrapper(const char* loc, UErrorCode* status) : ucol(ucol_open(loc, status)) {}
    ~UCollatorWrapper() { ucol_close(ucol); }
    operator UCollator*() { return ucol; }

private:
    UCollator* ucol;
};

constexpr char16_t TEXT_CHAR16[] = u"char16";
#if !U_CHAR16_IS_TYPEDEF && (!defined(_LIBCPP_VERSION) || _LIBCPP_VERSION < 180000)
constexpr uint16_t TEXT_UINT16[] = {0x75, 0x69, 0x6e, 0x74, 0x31, 0x36, 0x00};
#endif
#if U_SIZEOF_WCHAR_T == 2
constexpr wchar_t TEXT_WCHAR[] = L"wchar";
#endif

constexpr char TEXT_CHAR[] = "char";
#if defined(__cpp_char8_t)
constexpr char8_t TEXT_CHAR8[] = u8"char8";
#endif

// Verify that the UCollator predicates handle all string types.
void UColHeaderOnlyTest::TestPredicateTypes() {
    using namespace U_HEADER_NESTED_NAMESPACE;
    IcuTestErrorCode status(*this, "TestPredicateTypes");
    UCollatorWrapper ucol("", status);
    status.assertSuccess();
    const auto equal_to = collator::equal_to(ucol);

    assertTrue("char16_t", equal_to(TEXT_CHAR16, TEXT_CHAR16));
    assertTrue("u16string_view", equal_to(std::u16string_view{TEXT_CHAR16}, TEXT_CHAR16));

#if !U_CHAR16_IS_TYPEDEF && (!defined(_LIBCPP_VERSION) || _LIBCPP_VERSION < 180000)
    assertTrue("uint16_t", equal_to(TEXT_UINT16, TEXT_UINT16));
    assertTrue("basic_string_view<uint16_t>",
               equal_to(std::basic_string_view<uint16_t>{TEXT_UINT16}, TEXT_UINT16));
#endif

#if U_SIZEOF_WCHAR_T == 2
    assertTrue("wchar_t", equal_to(TEXT_WCHAR, TEXT_WCHAR));
    assertTrue("wstring_view", equal_to(std::wstring_view{TEXT_WCHAR}, TEXT_WCHAR));
#endif

    assertTrue("char", equal_to(TEXT_CHAR, TEXT_CHAR));
    assertTrue("string_view", equal_to(std::string_view{TEXT_CHAR}, TEXT_CHAR));

#if defined(__cpp_char8_t)
    assertTrue("char8_t", equal_to(TEXT_CHAR8, TEXT_CHAR8));
    assertTrue("u8string_view", equal_to(std::u8string_view{TEXT_CHAR8}, TEXT_CHAR8));
#endif
}

} // namespace

IntlTest* createUColHeaderOnlyTest() {
    return new UColHeaderOnlyTest();
}

#endif  // !UCONFIG_NO_COLLATION
