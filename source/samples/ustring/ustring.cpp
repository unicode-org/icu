/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ustring.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000aug15
*   created by: Markus W. Scherer
*
*   This file contains sample code that illustrates the use of Unicode strings
*   with ICU.
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/unistr.h"

// helper function --------------------------------------------------------- ***

static void
printUnicodeString(const UnicodeString &s) {
    static char out[200];
    int32_t i, length;

    // output the string, converted to the platform encoding
    out[s.extract(0, 99, out)]=0;
    printf("%s {", out);

    // output the code units
    length=s.length();
    for(i=0; i<length; ++i) {
        printf(" %04x", s.charAt(i));
    }
    printf(" }\n");
}

// sample code for storage models ------------------------------------------ ***

#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))

static const UChar readonly[]={
    0x61, 0x31, 0x20ac
};
static UChar writeable[]={
    0x62, 0x32, 0xdbc0, 0xdc01
};
static char out[100];

static void
demoStorage() {
    // These sample code lines illustrate how to use UnicodeString, and the
    // comments tell what happens internally. There are no APIs to observe
    // most of this programmatically, except for stepping into the code
    // with a debugger.
    // This is by design to hide such details from the user.

    // * UnicodeString with internally stored contents
    // instantiate a UnicodeString from a single code point
    // the few (2) UChars will be stored in the object itself
    UnicodeString one((UChar32)0x24001);
    // this copies the few UChars into the "two" object
    UnicodeString two=one;
    printf("length of short string copy: %d\n", two.length());
    // set "one" to contain the 3 UChars from readonly
    one.setTo(readonly, LENGTHOF(readonly));

    // * UnicodeString with allocated contents
    // build a longer string that will not fit into the object's buffer
    one+=UnicodeString(writeable, LENGTHOF(writeable));
    one+=one;
    one+=one;
    printf("length of longer string: %d\n", one.length());
    // copying will use the same allocated buffer and increment the reference
    // counter
    two=one;
    printf("length of longer string copy: %d\n", two.length());

    // * UnicodeString using readonly-alias to a const UChar array
    // construct a string that aliases a readonly buffer
    UnicodeString three(FALSE, readonly, LENGTHOF(readonly));
    UTextOffset i;
    for(i=0; i<three.length(); ++i) {
        printf("readonly-alias string[%d]=0x%lx\n", i, three.charAt(i));
    }
    // copy-on-write: any modification to the string results in
    // a copy to either the internal buffer or to a newly allocated one
    three.setCharAt(1, 0x39);
    out[three.extract(0, 99, out)]=0;
    printf("readonly-aliasing string after modification: %s\n", out);
    // the aliased array is not modified
    for(i=0; i<three.length(); ++i) {
        printf("readonly buffer[%d] after modifying its string: 0x%lx\n",
               i, readonly[i]);
    }
    // setTo() readonly alias
    one.setTo(FALSE, writeable, LENGTHOF(writeable));
    // copying the readonly-alias object will readonly-alias the same buffer
    two=one;
    out[two.extract(0, 99, out)]=0;
    printf("copy of readonly-alias string of \"writeable\" array: %s\n", out);

    // * UnicodeString using writeable-alias to a non-const UChar array
    UnicodeString four(writeable, LENGTHOF(writeable), LENGTHOF(writeable));
    for(i=0; i<four.length(); ++i) {
        printf("writeable-alias string[%d]=0x%lx\n", i, four.charAt(i));
    }
    // a modification writes through to the buffer
    four.setCharAt(1, 0x39);
    for(i=0; i<four.length(); ++i) {
        printf("writeable-alias backing buffer[%d]=0x%lx "
               "after modification\n", i, writeable[i]);
    }
    // a copy will not alias any more;
    // instead, it will get a copy of the contents into allocated memory
    two=four;
    two.setCharAt(1, 0x21);
    for(i=0; i<two.length(); ++i) {
        printf("writeable-alias backing buffer[%d]=0x%lx after "
               "modification of string copy\n", i, writeable[i]);
    }
    // setTo() writeable alias
    one.setTo(writeable, LENGTHOF(writeable), LENGTHOF(writeable));
    // grow the string - it will not fit into the backing buffer any more
    // and will get copied before modification
    one.append((UChar)0x40);
    // shrink it back so it would fit
    one.truncate(one.length()-1);
    // we still operate on the copy
    one.setCharAt(1, 0x25);
    printf("string after growing too much and then shrinking[1]=0x%lx\n"
           "                          backing store for this[1]=0x%lx\n",
           one.charAt(1), writeable[1]);
    // if we need it in the original buffer, then extract() to it
    // extract() does not do anything if the string aliases that same buffer
    // i=min(one.length(), length of array)
    if(one.length()<LENGTHOF(writeable)) {
        i=one.length();
    } else {
        i=LENGTHOF(writeable);
    }
    one.extract(0, i, writeable);
    for(i=0; i<LENGTHOF(writeable); ++i) {
        printf("writeable-alias backing buffer[%d]=0x%lx after re-extract\n",
               i, writeable[i]);
    }
}

// sample code for instantiating Unicode strings --------------------------- ***

static void
initString() {
    // the string literal is 32 chars long - this must be counted for the macro
    UnicodeString invariantOnly=UNICODE_STRING("such characters are safe 123 %-.", 32);

    /*
     * In C, we need two macros: one to declare the UChar[] array, and
     * one to populate it; the second one is a noop on platforms where
     * wchar_t is compatible with UChar and ASCII-based.
     * The length of the string literal must be counted for both macros.
     */
    /* declare the invString array for the string */
    U_STRING_DECL(invString, "such characters are safe 123 %-.", 32);
    /* populate it with the characters */
    U_STRING_INIT(invString, "such characters are safe 123 %-.", 32);

    // compare the C and C++ strings
    printf("C and C++ Unicode strings are equal: %d\n", invariantOnly==UnicodeString(TRUE, invString, 32));

    /*
     * convert between char * and UChar * strings that
     * contain only invariant characters
     */
    static const char *cs1="such characters are safe 123 %-.";
    static UChar us1[40];
    static char cs2[40];
    u_charsToUChars(cs1, us1, 33); /* include the terminating NUL */
    u_UCharsToChars(us1, cs2, 33);
    printf("char * -> UChar * -> char * with only "
           "invariant characters: \"%s\"\n",
           cs2);

    // initialize a UnicodeString from a string literal that contains
    // escape sequences written with invariant characters
    // do not forget to duplicate the backslashes for ICU to see them
    // then, count each double backslash only once!
    UnicodeString german=UNICODE_STRING(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n", 64).
        unescape();
    printf("german UnicodeString from unescaping:\n    ");
    printUnicodeString(german);

    /*
     * C: convert and unescape a char * string with only invariant
     * characters to fill a UChar * string
     */
    UChar buffer[200];
    int32_t length;
    length=u_unescape(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n",
        buffer, LENGTHOF(buffer));
    printf("german C Unicode string from char * unescaping: (length %d)\n    ", length);
    printUnicodeString(UnicodeString(buffer));
}

extern int
main(int argc, const char *argv[]) {
    demoStorage();
    initString();

    return 0;
}
