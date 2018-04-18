// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

// Allow implicit conversion from char16_t* to UnicodeString for this file:
// Helpful in toString methods and elsewhere.
#define UNISTR_FROM_STRING_EXPLICIT

#include "numparse_unisets.h"
#include "umutex.h"
#include "ucln_cmn.h"
#include "unicode/uniset.h"
#include "uresimp.h"
#include "cstring.h"
#include "uassert.h"

using namespace icu;
using namespace icu::numparse;
using namespace icu::numparse::impl;
using namespace icu::numparse::impl::unisets;


namespace {

static UnicodeSet* gUnicodeSets[COUNT] = {};

UnicodeSet* computeUnion(Key k1, Key k2) {
    UnicodeSet* result = new UnicodeSet();
    if (result == nullptr) {
        return nullptr;
    }
    result->addAll(*gUnicodeSets[k1]);
    result->addAll(*gUnicodeSets[k2]);
    result->freeze();
    return result;
}

UnicodeSet* computeUnion(Key k1, Key k2, Key k3) {
    UnicodeSet* result = new UnicodeSet();
    if (result == nullptr) {
        return nullptr;
    }
    result->addAll(*gUnicodeSets[k1]);
    result->addAll(*gUnicodeSets[k2]);
    result->addAll(*gUnicodeSets[k3]);
    result->freeze();
    return result;
}


void saveSet(Key key, const UnicodeString& unicodeSetPattern, UErrorCode& status) {
    // assert unicodeSets.get(key) == null;
    gUnicodeSets[key] = new UnicodeSet(unicodeSetPattern, status);
}

class ParseDataSink : public ResourceSink {
  public:
    void put(const char* key, ResourceValue& value, UBool /*noFallback*/, UErrorCode& status) U_OVERRIDE {
        ResourceTable contextsTable = value.getTable(status);
        if (U_FAILURE(status)) { return; }
        for (int i = 0; contextsTable.getKeyAndValue(i, key, value); i++) {
            if (uprv_strcmp(key, "date") == 0) {
                // ignore
            } else {
                ResourceTable strictnessTable = value.getTable(status);
                if (U_FAILURE(status)) { return; }
                for (int j = 0; strictnessTable.getKeyAndValue(j, key, value); j++) {
                    bool isLenient = (uprv_strcmp(key, "lenient") == 0);
                    ResourceArray array = value.getArray(status);
                    if (U_FAILURE(status)) { return; }
                    for (int k = 0; k < array.getSize(); k++) {
                        array.getValue(k, value);
                        UnicodeString str = value.getUnicodeString(status);
                        if (U_FAILURE(status)) { return; }
                        // There is both lenient and strict data for comma/period,
                        // but not for any of the other symbols.
                        if (str.indexOf(u'.') != -1) {
                            saveSet(isLenient ? PERIOD : STRICT_PERIOD, str, status);
                        } else if (str.indexOf(u',') != -1) {
                            saveSet(isLenient ? COMMA : STRICT_COMMA, str, status);
                        } else if (str.indexOf(u'+') != -1) {
                            saveSet(PLUS_SIGN, str, status);
                        } else if (str.indexOf(u'‒') != -1) {
                            saveSet(MINUS_SIGN, str, status);
                        } else if (str.indexOf(u'$') != -1) {
                            saveSet(DOLLAR_SIGN, str, status);
                        } else if (str.indexOf(u'£') != -1) {
                            saveSet(POUND_SIGN, str, status);
                        } else if (str.indexOf(u'₨') != -1) {
                            saveSet(RUPEE_SIGN, str, status);
                        }
                        if (U_FAILURE(status)) { return; }
                    }
                }
            }
        }
    }
};


icu::UInitOnce gNumberParseUniSetsInitOnce = U_INITONCE_INITIALIZER;

UBool U_CALLCONV cleanupNumberParseUniSets() {
    for (int32_t i = 0; i < COUNT; i++) {
        delete gUnicodeSets[i];
        gUnicodeSets[i] = nullptr;
    }
    gNumberParseUniSetsInitOnce.reset();
    return TRUE;
}

void U_CALLCONV initNumberParseUniSets(UErrorCode& status) {
    ucln_common_registerCleanup(UCLN_COMMON_NUMPARSE_UNISETS, cleanupNumberParseUniSets);

    gUnicodeSets[EMPTY] = new UnicodeSet();

    // These sets were decided after discussion with icu-design@. See tickets #13084 and #13309.
    // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
    gUnicodeSets[DEFAULT_IGNORABLES] = new UnicodeSet(
            u"[[:Zs:][\\u0009][:Bidi_Control:][:Variation_Selector:]]", status);
    gUnicodeSets[STRICT_IGNORABLES] = new UnicodeSet(u"[[:Bidi_Control:]]", status);

    LocalUResourceBundlePointer rb(ures_open(nullptr, "root", &status));
    if (U_FAILURE(status)) { return; }
    ParseDataSink sink;
    ures_getAllItemsWithFallback(rb.getAlias(), "parse", sink, status);
    if (U_FAILURE(status)) { return; }

    // TODO: Should there be fallback behavior if for some reason these sets didn't get populated?
    U_ASSERT(gUnicodeSets[COMMA] != nullptr);
    U_ASSERT(gUnicodeSets[STRICT_COMMA] != nullptr);
    U_ASSERT(gUnicodeSets[PERIOD] != nullptr);
    U_ASSERT(gUnicodeSets[STRICT_PERIOD] != nullptr);

    gUnicodeSets[OTHER_GROUPING_SEPARATORS] = new UnicodeSet(
            u"['٬‘’＇\\u0020\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]", status);
    gUnicodeSets[ALL_SEPARATORS] = computeUnion(COMMA, PERIOD, OTHER_GROUPING_SEPARATORS);
    gUnicodeSets[STRICT_ALL_SEPARATORS] = computeUnion(
            STRICT_COMMA, STRICT_PERIOD, OTHER_GROUPING_SEPARATORS);

    U_ASSERT(gUnicodeSets[MINUS_SIGN] != nullptr);
    U_ASSERT(gUnicodeSets[PLUS_SIGN] != nullptr);

    gUnicodeSets[PERCENT_SIGN] = new UnicodeSet(u"[%٪]", status);
    gUnicodeSets[PERMILLE_SIGN] = new UnicodeSet(u"[‰؉]", status);
    gUnicodeSets[INFINITY_KEY] = new UnicodeSet(u"[∞]", status);

    U_ASSERT(gUnicodeSets[DOLLAR_SIGN] != nullptr);
    U_ASSERT(gUnicodeSets[POUND_SIGN] != nullptr);
    U_ASSERT(gUnicodeSets[RUPEE_SIGN] != nullptr);
    gUnicodeSets[YEN_SIGN] = new UnicodeSet(u"[¥\\uffe5]", status);

    gUnicodeSets[DIGITS] = new UnicodeSet(u"[:digit:]", status);

    gUnicodeSets[DIGITS_OR_ALL_SEPARATORS] = computeUnion(DIGITS, ALL_SEPARATORS);
    gUnicodeSets[DIGITS_OR_STRICT_ALL_SEPARATORS] = computeUnion(DIGITS, STRICT_ALL_SEPARATORS);

    for (int32_t i = 0; i < COUNT; i++) {
        gUnicodeSets[i]->freeze();
    }
}

}

const UnicodeSet* unisets::get(Key key) {
    UErrorCode localStatus = U_ZERO_ERROR;
    umtx_initOnce(gNumberParseUniSetsInitOnce, &initNumberParseUniSets, localStatus);
    if (U_FAILURE(localStatus)) {
        // TODO: This returns non-null in Java, and callers assume that.
        return nullptr;
    }
    return gUnicodeSets[key];
}

Key unisets::chooseFrom(UnicodeString str, Key key1) {
    return get(key1)->contains(str) ? key1 : COUNT;
}

Key unisets::chooseFrom(UnicodeString str, Key key1, Key key2) {
    return get(key1)->contains(str) ? key1 : chooseFrom(str, key2);
}

//Key unisets::chooseCurrency(UnicodeString str) {
//    if (get(DOLLAR_SIGN)->contains(str)) {
//        return DOLLAR_SIGN;
//    } else if (get(POUND_SIGN)->contains(str)) {
//        return POUND_SIGN;
//    } else if (get(RUPEE_SIGN)->contains(str)) {
//        return RUPEE_SIGN;
//    } else if (get(YEN_SIGN)->contains(str)) {
//        return YEN_SIGN;
//    } else {
//        return COUNT;
//    }
//}


#endif /* #if !UCONFIG_NO_FORMATTING */
