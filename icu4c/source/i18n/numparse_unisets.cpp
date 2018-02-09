// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT

#include "numparse_unisets.h"
#include "numparse_types.h"
#include "umutex.h"
#include "ucln_in.h"
#include "unicode/uniset.h"

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

icu::UInitOnce gNumberParseUniSetsInitOnce = U_INITONCE_INITIALIZER;

UBool U_CALLCONV cleanupNumberParseUniSets() {
    for (int32_t i = 0; i < COUNT; i++) {
        delete gUnicodeSets[i];
        gUnicodeSets[i] = nullptr;
    }
    return TRUE;
}

void U_CALLCONV initNumberParseUniSets(UErrorCode& status) {
    ucln_i18n_registerCleanup(UCLN_I18N_NUMPARSE_UNISETS, cleanupNumberParseUniSets);
#define NEW_UNISET(pattern, status) new UnicodeSet(UnicodeString(pattern), status)

    gUnicodeSets[EMPTY] = new UnicodeSet();

    // BiDi characters are skipped over and ignored at any point in the string, even in strict mode.
    gUnicodeSets[BIDI] = NEW_UNISET(u"[[\\u200E\\u200F\\u061C]]", status);

    // This set was decided after discussion with icu-design@. See ticket #13309.
    // Zs+TAB is "horizontal whitespace" according to UTS #18 (blank property).
    gUnicodeSets[WHITESPACE] = NEW_UNISET(u"[[:Zs:][\\u0009]]", status);

    gUnicodeSets[DEFAULT_IGNORABLES] = computeUnion(BIDI, WHITESPACE);
    gUnicodeSets[STRICT_IGNORABLES] = new UnicodeSet(*gUnicodeSets[BIDI]);

    // TODO: Re-generate these sets from the UCD. They probably haven't been updated in a while.
    gUnicodeSets[COMMA] = NEW_UNISET(u"[,،٫、︐︑﹐﹑，､]", status);
    gUnicodeSets[STRICT_COMMA] = NEW_UNISET(u"[,٫︐﹐，]", status);
    gUnicodeSets[PERIOD] = NEW_UNISET(u"[.․。︒﹒．｡]", status);
    gUnicodeSets[STRICT_PERIOD] = NEW_UNISET(u"[.․﹒．｡]", status);
    gUnicodeSets[OTHER_GROUPING_SEPARATORS] = NEW_UNISET(
            u"['٬‘’＇\\u0020\\u00A0\\u2000-\\u200A\\u202F\\u205F\\u3000]",
            status);
    gUnicodeSets[ALL_SEPARATORS] = computeUnion(COMMA, PERIOD, OTHER_GROUPING_SEPARATORS);
    gUnicodeSets[STRICT_ALL_SEPARATORS] = computeUnion(
            STRICT_COMMA, STRICT_PERIOD, OTHER_GROUPING_SEPARATORS);

    gUnicodeSets[MINUS_SIGN] = NEW_UNISET(u"[-⁻₋−➖﹣－]", status);
    gUnicodeSets[PLUS_SIGN] = NEW_UNISET(u"[+⁺₊➕﬩﹢＋]", status);

    gUnicodeSets[PERCENT_SIGN] = NEW_UNISET(u"[%٪]", status);
    gUnicodeSets[PERMILLE_SIGN] = NEW_UNISET(u"[‰؉]", status);
    gUnicodeSets[INFINITY] = NEW_UNISET(u"[∞]", status);

    gUnicodeSets[DIGITS] = NEW_UNISET(u"[:digit:]", status);
    gUnicodeSets[NAN_LEAD] = NEW_UNISET(u"[NnТтmeՈոс¤НнчTtsҳ\u975e\u1002\u0e9a\u10d0\u0f68\u0644\u0646]",
            status);
    gUnicodeSets[SCIENTIFIC_LEAD] = NEW_UNISET(u"[Ee×·е\u0627]", status);
    gUnicodeSets[CWCF] = NEW_UNISET(u"[:CWCF:]", status);

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


#endif /* #if !UCONFIG_NO_FORMATTING */
