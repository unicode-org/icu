/**
*******************************************************************************
* Copyright (C) 2004, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UTF16;

/**
 * Test JDK 1.5 cover APIs.
 */
public final class UCharacterSurrogateTest extends TestFmwk {

    public static void main(String[] args) {
	new UCharacterSurrogateTest().run(args);
    }

    public void TestUnicodeBlockForName() {
	String[] names = {
	    "Optical Character Recognition",
	    "CJK Unified Ideographs Extension A",
	    "Supplemental Arrows-B",
	    "Supplementary Private Use Area-B",
	    "supplementary_Private_Use_Area-b",
	    "supplementary_PRIVATE_Use_Area_b"
	};
	for (int i = 0; i < names.length; ++i) {
	    try {
		UCharacter.UnicodeBlock b = UCharacter.UnicodeBlock.forName(names[i]);
		logln("found: " + b + " for name: " + names[i]);
	    }
	    catch (Exception e) {
		errln("could not find block for name: " + names[i]);
		break;
	    }
	}
    }

    public void TestIsValidCodePoint() {
	if (UCharacter.isValidCodePoint(-1)) errln("-1");
	if (!UCharacter.isValidCodePoint(0)) errln("0");
	if (!UCharacter.isValidCodePoint(UCharacter.MAX_CODE_POINT)) errln("0x10ffff");
	if (UCharacter.isValidCodePoint(UCharacter.MAX_CODE_POINT+1)) errln("0x110000");
    }

    public void TestIsSupplementaryCodePoint() {
	if (UCharacter.isSupplementaryCodePoint(-1)) errln("-1");
	if (UCharacter.isSupplementaryCodePoint(0)) errln("0");
	if (UCharacter.isSupplementaryCodePoint(UCharacter.MIN_SUPPLEMENTARY_CODE_POINT-1)) errln("0xffff");
	if (!UCharacter.isSupplementaryCodePoint(UCharacter.MIN_SUPPLEMENTARY_CODE_POINT)) errln("0x10000");
	if (!UCharacter.isSupplementaryCodePoint(UCharacter.MAX_CODE_POINT)) errln("0x10ffff");
	if (UCharacter.isSupplementaryCodePoint(UCharacter.MAX_CODE_POINT+1)) errln("0x110000");
    }

    public void TestIsHighSurrogate() {
	if (UCharacter.isHighSurrogate((char)(UCharacter.MIN_HIGH_SURROGATE-1))) errln("0xd7ff");
	if (!UCharacter.isHighSurrogate(UCharacter.MIN_HIGH_SURROGATE)) errln("0xd800");
	if (!UCharacter.isHighSurrogate(UCharacter.MAX_HIGH_SURROGATE)) errln("0xdbff");
	if (UCharacter.isHighSurrogate((char)(UCharacter.MAX_HIGH_SURROGATE+1))) errln("0xdc00");
    }

    public void TestIsLowSurrogate() {
	if (UCharacter.isLowSurrogate((char)(UCharacter.MIN_LOW_SURROGATE-1))) errln("0xdbff");
	if (!UCharacter.isLowSurrogate(UCharacter.MIN_LOW_SURROGATE)) errln("0xdc00");
	if (!UCharacter.isLowSurrogate(UCharacter.MAX_LOW_SURROGATE)) errln("0xdfff");
	if (UCharacter.isLowSurrogate((char)(UCharacter.MAX_LOW_SURROGATE+1))) errln("0xe000");
    }

    public void TestIsSurrogatePair() {
	if (UCharacter.isSurrogatePair((char)(UCharacter.MIN_HIGH_SURROGATE-1), UCharacter.MIN_LOW_SURROGATE)) errln("0xd7ff,0xdc00");
	if (UCharacter.isSurrogatePair((char)(UCharacter.MAX_HIGH_SURROGATE+1), UCharacter.MIN_LOW_SURROGATE)) errln("0xd800,0xdc00");
	if (UCharacter.isSurrogatePair(UCharacter.MIN_HIGH_SURROGATE, (char)(UCharacter.MIN_LOW_SURROGATE-1))) errln("0xd800,0xdbff");
	if (UCharacter.isSurrogatePair(UCharacter.MIN_HIGH_SURROGATE, (char)(UCharacter.MAX_LOW_SURROGATE+1))) errln("0xd800,0xe000");
	if (!UCharacter.isSurrogatePair(UCharacter.MIN_HIGH_SURROGATE, UCharacter.MIN_LOW_SURROGATE)) errln("0xd800,0xdc00");
    }

    public void TestCharCount() {
	UCharacter.charCount(-1);
	UCharacter.charCount(UCharacter.MAX_CODE_POINT+1);
	if (UCharacter.charCount(UCharacter.MIN_SUPPLEMENTARY_CODE_POINT-1) != 1) errln("0xffff");
	if (UCharacter.charCount(UCharacter.MIN_SUPPLEMENTARY_CODE_POINT) != 2) errln("0x010000");
    }

    public void TestToCodePoint() {
	final char[] pairs = {
	    (char)(UCharacter.MIN_HIGH_SURROGATE+0), (char)(UCharacter.MIN_LOW_SURROGATE+0),
	    (char)(UCharacter.MIN_HIGH_SURROGATE+1), (char)(UCharacter.MIN_LOW_SURROGATE+1),
	    (char)(UCharacter.MIN_HIGH_SURROGATE+2), (char)(UCharacter.MIN_LOW_SURROGATE+2),
	    (char)(UCharacter.MAX_HIGH_SURROGATE-2), (char)(UCharacter.MAX_LOW_SURROGATE-2),
	    (char)(UCharacter.MAX_HIGH_SURROGATE-1), (char)(UCharacter.MAX_LOW_SURROGATE-1),
	    (char)(UCharacter.MAX_HIGH_SURROGATE-0), (char)(UCharacter.MAX_LOW_SURROGATE-0),
	};
	for (int i = 0; i < pairs.length; i += 2) {
	    int cp = UCharacter.toCodePoint(pairs[i], pairs[i+1]);
	    if (pairs[i] != UTF16.getLeadSurrogate(cp) ||
		pairs[i+1] != UTF16.getTrailSurrogate(cp)) {

		errln(Integer.toHexString(pairs[i]) + ", " + pairs[i+1]);
		break;
	    }
	}
    }

    public void TestCodePointAtBefore() {
	String s = "" +
	    UCharacter.MIN_HIGH_SURROGATE + // isolated high
	    UCharacter.MIN_HIGH_SURROGATE + // pair
	    UCharacter.MIN_LOW_SURROGATE +
	    UCharacter.MIN_LOW_SURROGATE; // isolated low
	char[] c = s.toCharArray();
	int[] avalues = {
	    UCharacter.MIN_HIGH_SURROGATE,
	    UCharacter.toCodePoint(UCharacter.MIN_HIGH_SURROGATE, UCharacter.MIN_LOW_SURROGATE),
	    UCharacter.MIN_LOW_SURROGATE,
	    UCharacter.MIN_LOW_SURROGATE
	};
	int[] bvalues = {
	    UCharacter.MIN_HIGH_SURROGATE,
	    UCharacter.MIN_HIGH_SURROGATE,
	    UCharacter.toCodePoint(UCharacter.MIN_HIGH_SURROGATE, UCharacter.MIN_LOW_SURROGATE),
	    UCharacter.MIN_LOW_SURROGATE,
	};
	StringBuffer b = new StringBuffer(s);
	for (int i = 0; i < avalues.length; ++i) {
	    if (UCharacter.codePointAt(s, i) != avalues[i]) errln("string at: " + i);
	    if (UCharacter.codePointAt(c, i) != avalues[i]) errln("chars at: " + i);
	    if (UCharacter.codePointAt(b, i) != avalues[i]) errln("stringbuffer at: " + i);

	    if (UCharacter.codePointBefore(s, i+1) != bvalues[i]) errln("string before: " + i);
	    if (UCharacter.codePointBefore(c, i+1) != bvalues[i]) errln("chars before: " + i);
	    if (UCharacter.codePointBefore(b, i+1) != bvalues[i]) errln("stringbuffer before: " + i);
	}
    }

    public void TestToChars() {
	char[] chars = new char[3];
	int cp = UCharacter.toCodePoint(UCharacter.MIN_HIGH_SURROGATE, UCharacter.MIN_LOW_SURROGATE);
	UCharacter.toChars(cp, chars, 1);
	if (chars[1] != UCharacter.MIN_HIGH_SURROGATE ||
	    chars[2] != UCharacter.MIN_LOW_SURROGATE) {

	    errln("fail");
	}

	chars = UCharacter.toChars(cp);
	if (chars[0] != UCharacter.MIN_HIGH_SURROGATE ||
	    chars[1] != UCharacter.MIN_LOW_SURROGATE) {

	    errln("fail");
	}
    }

    
}

