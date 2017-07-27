// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
*   Copyright (C) 2001-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/23/00    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/edits.h"
#include "unicode/unistr.h"
#include "testutil.h"
#include "intltest.h"

static const UChar HEX[] = u"0123456789ABCDEF";

UnicodeString &TestUtility::appendHex(UnicodeString &buf, UChar32 ch) {
    if (ch >= 0x10000) {
        if (ch >= 0x100000) {
            buf.append(HEX[0xF&(ch>>20)]);
        }
        buf.append(HEX[0xF&(ch>>16)]);
    }
    buf.append(HEX[0xF&(ch>>12)]);
    buf.append(HEX[0xF&(ch>>8)]);
    buf.append(HEX[0xF&(ch>>4)]);
    buf.append(HEX[0xF&ch]);
    return buf;
}

UnicodeString TestUtility::hex(UChar32 ch) {
    UnicodeString buf;
    appendHex(buf, ch);
    return buf;
}

UnicodeString TestUtility::hex(const UnicodeString& s) {
    return hex(s, u',');
}

UnicodeString TestUtility::hex(const UnicodeString& s, UChar sep) {
    UnicodeString result;
    if (s.isEmpty()) return result;
    UChar32 c;
    for (int32_t i = 0; i < s.length(); i += U16_LENGTH(c)) {
        c = s.char32At(i);
        if (i > 0) {
            result.append(sep);
        }
        appendHex(result, c);
    }
    return result;
}

UnicodeString TestUtility::hex(const uint8_t* bytes, int32_t len) {
    UnicodeString buf;
    for (int32_t i = 0; i < len; ++i) {
        buf.append(HEX[0x0F & (bytes[i] >> 4)]);
        buf.append(HEX[0x0F & bytes[i]]);
    }
    return buf;
}

void TestUtility::checkEditsIter(
        IntlTest &test,
        const UnicodeString &name,
        Edits::Iterator ei1, Edits::Iterator ei2,  // two equal iterators
        const EditChange expected[], int32_t expLength, UBool withUnchanged,
        UErrorCode &errorCode) {
    test.assertFalse(name + u":" + __LINE__, ei2.findSourceIndex(-1, errorCode));
    test.assertFalse(name + u":" + __LINE__, ei2.findDestinationIndex(-1, errorCode));

    int32_t expSrcIndex = 0;
    int32_t expDestIndex = 0;
    int32_t expReplIndex = 0;
    int32_t expSrcIndexFromDest = 0;  // for sourceIndexFromDestinationIndex()
    int32_t expDestIndexFromSrc = 0;  // for destinationIndexFromSourceIndex()
    for (int32_t expIndex = 0; expIndex < expLength; ++expIndex) {
        const EditChange &expect = expected[expIndex];
        UnicodeString msg = UnicodeString(name).append(u' ') + expIndex;
        if (withUnchanged || expect.change) {
            test.assertTrue(msg + u":" + __LINE__, ei1.next(errorCode));
            test.assertEquals(msg + u":" + __LINE__, expect.change, ei1.hasChange());
            test.assertEquals(msg + u":" + __LINE__, expect.oldLength, ei1.oldLength());
            test.assertEquals(msg + u":" + __LINE__, expect.newLength, ei1.newLength());
            test.assertEquals(msg + u":" + __LINE__, expSrcIndex, ei1.sourceIndex());
            test.assertEquals(msg + u":" + __LINE__, expDestIndex, ei1.destinationIndex());
            test.assertEquals(msg + u":" + __LINE__, expReplIndex, ei1.replacementIndex());
        }

        if (expect.oldLength > 0 && expDestIndex == expDestIndexFromSrc) {
            test.assertTrue(msg + u":" + __LINE__, ei2.findSourceIndex(expSrcIndex, errorCode));
            test.assertEquals(msg + u":" + __LINE__, expect.change, ei2.hasChange());
            test.assertEquals(msg + u":" + __LINE__, expect.oldLength, ei2.oldLength());
            test.assertEquals(msg + u":" + __LINE__, expect.newLength, ei2.newLength());
            test.assertEquals(msg + u":" + __LINE__, expSrcIndex, ei2.sourceIndex());
            test.assertEquals(msg + u":" + __LINE__, expDestIndex, ei2.destinationIndex());
            test.assertEquals(msg + u":" + __LINE__, expReplIndex, ei2.replacementIndex());
            if (!withUnchanged) {
                // For some iterators, move past the current range
                // so that findSourceIndex() has to look before the current index.
                ei2.next(errorCode);
                ei2.next(errorCode);
            }
        }

        if (expect.newLength > 0 && expSrcIndex == expSrcIndexFromDest) {
            test.assertTrue(msg + u":" + __LINE__, ei2.findDestinationIndex(expDestIndex, errorCode));
            test.assertEquals(msg + u":" + __LINE__, expect.change, ei2.hasChange());
            test.assertEquals(msg + u":" + __LINE__, expect.oldLength, ei2.oldLength());
            test.assertEquals(msg + u":" + __LINE__, expect.newLength, ei2.newLength());
            test.assertEquals(msg + u":" + __LINE__, expSrcIndex, ei2.sourceIndex());
            test.assertEquals(msg + u":" + __LINE__, expDestIndex, ei2.destinationIndex());
            test.assertEquals(msg + u":" + __LINE__, expReplIndex, ei2.replacementIndex());
            if (!withUnchanged) {
                // For some iterators, move past the current range
                // so that findSourceIndex() has to look before the current index.
                ei2.next(errorCode);
                ei2.next(errorCode);
            }
        }

        // Span starts.
        test.assertEquals(name + u":" + __LINE__, expDestIndexFromSrc,
                          ei2.destinationIndexFromSourceIndex(expSrcIndex, errorCode));
        test.assertEquals(name + u":" + __LINE__, expSrcIndexFromDest,
                          ei2.sourceIndexFromDestinationIndex(expDestIndex, errorCode));

        // Inside unchanged span map offsets 1:1.
        if (!expect.change && expect.oldLength >= 2) {
            test.assertEquals(name + u":" + __LINE__, expDestIndex + 1,
                              ei2.destinationIndexFromSourceIndex(expSrcIndex + 1, errorCode));
            test.assertEquals(name + u":" + __LINE__, expSrcIndex + 1,
                              ei2.sourceIndexFromDestinationIndex(expDestIndex + 1, errorCode));
        }

        // Inside change span map to the span limit.
        int32_t expSrcLimit = expSrcIndex + expect.oldLength;
        int32_t expDestLimit = expDestIndex + expect.newLength;
        if (expect.change) {
            if (expect.oldLength >= 2) {
                test.assertEquals(name + u":" + __LINE__, expDestLimit,
                                  ei2.destinationIndexFromSourceIndex(expSrcIndex + 1, errorCode));
            }
            if (expect.newLength >= 2) {
                test.assertEquals(name + u":" + __LINE__, expSrcLimit,
                                  ei2.sourceIndexFromDestinationIndex(expDestIndex + 1, errorCode));
            }
        }

        expSrcIndex = expSrcLimit;
        expDestIndex = expDestLimit;
        if (expect.change) {
            expReplIndex += expect.newLength;
        }
        if (expect.newLength > 0) {
            expSrcIndexFromDest = expSrcIndex;
        }
        if (expect.oldLength > 0) {
            expDestIndexFromSrc = expDestIndex;
        }
    }
    UnicodeString msg = UnicodeString(name).append(u" end");
    test.assertFalse(msg + u":" + __LINE__, ei1.next(errorCode));
    test.assertFalse(msg + u":" + __LINE__, ei1.hasChange());
    test.assertEquals(msg + u":" + __LINE__, 0, ei1.oldLength());
    test.assertEquals(msg + u":" + __LINE__, 0, ei1.newLength());
    test.assertEquals(msg + u":" + __LINE__, expSrcIndex, ei1.sourceIndex());
    test.assertEquals(msg + u":" + __LINE__, expDestIndex, ei1.destinationIndex());
    test.assertEquals(msg + u":" + __LINE__, expReplIndex, ei1.replacementIndex());

    test.assertFalse(name + u":" + __LINE__, ei2.findSourceIndex(expSrcIndex, errorCode));
    test.assertFalse(name + u":" + __LINE__, ei2.findDestinationIndex(expDestIndex, errorCode));
    test.assertEquals(name + u":" + __LINE__, expDestIndex,
                      ei2.destinationIndexFromSourceIndex(expSrcIndex, errorCode));
    test.assertEquals(name + u":" + __LINE__, expSrcIndex,
                      ei2.sourceIndexFromDestinationIndex(expDestIndex, errorCode));
}
