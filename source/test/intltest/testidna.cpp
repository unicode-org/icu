/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  testidna.cpp
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_IDNA && !UCONFIG_NO_TRANSLITERATION

#include "unicode/uidna.h"
#include <time.h>
#include <limits.h>
#include "unicode/ustring.h"
#include "sprpimpl.h"
#include "unicode/putil.h"
#include "cstring.h"
#include "unicode/uniset.h"
#include "unicode/ures.h"
#include "cmemory.h"
#include "testidna.h"
#include "idnaref.h"
#include "nptrans.h"

static UChar unicodeIn[][41] ={
    { 
        0x0644, 0x064A, 0x0647, 0x0645, 0x0627, 0x0628, 0x062A, 0x0643, 0x0644,
        0x0645, 0x0648, 0x0634, 0x0639, 0x0631, 0x0628, 0x064A, 0x061F, 0x0000
    },
    {
        0x4ED6, 0x4EEC, 0x4E3A, 0x4EC0, 0x4E48, 0x4E0D, 0x8BF4, 0x4E2D, 0x6587, 
        0x0000
    },
    {
        0x0050, 0x0072, 0x006F, 0x010D, 0x0070, 0x0072, 0x006F, 0x0073, 0x0074,
        0x011B, 0x006E, 0x0065, 0x006D, 0x006C, 0x0075, 0x0076, 0x00ED, 0x010D,
        0x0065, 0x0073, 0x006B, 0x0079, 0x0000
    },
    {
        0x05DC, 0x05DE, 0x05D4, 0x05D4, 0x05DD, 0x05E4, 0x05E9, 0x05D5, 0x05D8,
        0x05DC, 0x05D0, 0x05DE, 0x05D3, 0x05D1, 0x05E8, 0x05D9, 0x05DD, 0x05E2,
        0x05D1, 0x05E8, 0x05D9, 0x05EA, 0x0000
    },
    {
        0x092F, 0x0939, 0x0932, 0x094B, 0x0917, 0x0939, 0x093F, 0x0928, 0x094D,
        0x0926, 0x0940, 0x0915, 0x094D, 0x092F, 0x094B, 0x0902, 0x0928, 0x0939,
        0x0940, 0x0902, 0x092C, 0x094B, 0x0932, 0x0938, 0x0915, 0x0924, 0x0947,
        0x0939, 0x0948, 0x0902, 0x0000
    },
    {
        0x306A, 0x305C, 0x307F, 0x3093, 0x306A, 0x65E5, 0x672C, 0x8A9E, 0x3092,
        0x8A71, 0x3057, 0x3066, 0x304F, 0x308C, 0x306A, 0x3044, 0x306E, 0x304B,
        0x0000
    },
/*  
    {
        0xC138, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
        0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
        0xC5BC, 0xB9C8, 0xB098, 0xC88B, 0xC744, 0xAE4C, 0x0000
    },
*/
    {   
        0x043F, 0x043E, 0x0447, 0x0435, 0x043C, 0x0443, 0x0436, 0x0435, 0x043E,
        0x043D, 0x0438, 0x043D, 0x0435, 0x0433, 0x043E, 0x0432, 0x043E, 0x0440,
        0x044F, 0x0442, 0x043F, 0x043E, 0x0440, 0x0443, 0x0441, 0x0441, 0x043A,
        0x0438, 0x0000
    },
    {
        0x0050, 0x006F, 0x0072, 0x0071, 0x0075, 0x00E9, 0x006E, 0x006F, 0x0070,
        0x0075, 0x0065, 0x0064, 0x0065, 0x006E, 0x0073, 0x0069, 0x006D, 0x0070,
        0x006C, 0x0065, 0x006D, 0x0065, 0x006E, 0x0074, 0x0065, 0x0068, 0x0061,
        0x0062, 0x006C, 0x0061, 0x0072, 0x0065, 0x006E, 0x0045, 0x0073, 0x0070,
        0x0061, 0x00F1, 0x006F, 0x006C, 0x0000
    },
    {
        0x4ED6, 0x5011, 0x7232, 0x4EC0, 0x9EBD, 0x4E0D, 0x8AAA, 0x4E2D, 0x6587,
        0x0000
    },
    {
        0x0054, 0x1EA1, 0x0069, 0x0073, 0x0061, 0x006F, 0x0068, 0x1ECD, 0x006B,
        0x0068, 0x00F4, 0x006E, 0x0067, 0x0074, 0x0068, 0x1EC3, 0x0063, 0x0068,
        0x1EC9, 0x006E, 0x00F3, 0x0069, 0x0074, 0x0069, 0x1EBF, 0x006E, 0x0067,
        0x0056, 0x0069, 0x1EC7, 0x0074, 0x0000
    },
    {
        0x0033, 0x5E74, 0x0042, 0x7D44, 0x91D1, 0x516B, 0x5148, 0x751F, 0x0000
    },
    {
        0x5B89, 0x5BA4, 0x5948, 0x7F8E, 0x6075, 0x002D, 0x0077, 0x0069, 0x0074,
        0x0068, 0x002D, 0x0053, 0x0055, 0x0050, 0x0045, 0x0052, 0x002D, 0x004D,
        0x004F, 0x004E, 0x004B, 0x0045, 0x0059, 0x0053, 0x0000
    },
    {
        0x0048, 0x0065, 0x006C, 0x006C, 0x006F, 0x002D, 0x0041, 0x006E, 0x006F,
        0x0074, 0x0068, 0x0065, 0x0072, 0x002D, 0x0057, 0x0061, 0x0079, 0x002D,
        0x305D, 0x308C, 0x305E, 0x308C, 0x306E, 0x5834, 0x6240, 0x0000
    },
    {
        0x3072, 0x3068, 0x3064, 0x5C4B, 0x6839, 0x306E, 0x4E0B, 0x0032, 0x0000
    },
    {
        0x004D, 0x0061, 0x006A, 0x0069, 0x3067, 0x004B, 0x006F, 0x0069, 0x3059,
        0x308B, 0x0035, 0x79D2, 0x524D, 0x0000
    },
    {
        0x30D1, 0x30D5, 0x30A3, 0x30FC, 0x0064, 0x0065, 0x30EB, 0x30F3, 0x30D0,
        0x0000
    },
    {
        0x305D, 0x306E, 0x30B9, 0x30D4, 0x30FC, 0x30C9, 0x3067, 0x0000
    },
    // test non-BMP code points
    {    
        0xD800, 0xDF00, 0xD800, 0xDF01, 0xD800, 0xDF02, 0xD800, 0xDF03, 0xD800, 0xDF05,
        0xD800, 0xDF06, 0xD800, 0xDF07, 0xD800, 0xDF09, 0xD800, 0xDF0A, 0xD800, 0xDF0B,
        0x0000
    },
    {
        0xD800, 0xDF0D, 0xD800, 0xDF0C, 0xD800, 0xDF1E, 0xD800, 0xDF0F, 0xD800, 0xDF16,
        0xD800, 0xDF15, 0xD800, 0xDF14, 0xD800, 0xDF12, 0xD800, 0xDF10, 0xD800, 0xDF20,
        0xD800, 0xDF21,
        0x0000
    },
    // Greek
    {
        0x03b5, 0x03bb, 0x03bb, 0x03b7, 0x03bd, 0x03b9, 0x03ba, 0x03ac
    },
    // Maltese
    {
        0x0062, 0x006f, 0x006e, 0x0121, 0x0075, 0x0073, 0x0061, 0x0127,
        0x0127, 0x0061
    },
    // Russian
    {
        0x043f, 0x043e, 0x0447, 0x0435, 0x043c, 0x0443, 0x0436, 0x0435,
        0x043e, 0x043d, 0x0438, 0x043d, 0x0435, 0x0433, 0x043e, 0x0432,
        0x043e, 0x0440, 0x044f, 0x0442, 0x043f, 0x043e, 0x0440, 0x0443,
        0x0441, 0x0441, 0x043a, 0x0438
    }
   
};

static const char *asciiIn[] = {
    "xn--egbpdaj6bu4bxfgehfvwxn",
    "xn--ihqwcrb4cv8a8dqg056pqjye",
    "xn--Proprostnemluvesky-uyb24dma41a",
    "xn--4dbcagdahymbxekheh6e0a7fei0b",
    "xn--i1baa7eci9glrd9b2ae1bj0hfcgg6iyaf8o0a1dig0cd",
    "xn--n8jok5ay5dzabd5bym9f0cm5685rrjetr6pdxa",
/*  "xn--989aomsvi5e83db1d2a355cv1e0vak1dwrv93d5xbh15a0dt30a5jpsd879ccm6fea98c",*/
    "xn--b1abfaaepdrnnbgefbaDotcwatmq2g4l",
    "xn--PorqunopuedensimplementehablarenEspaol-fmd56a",
    "xn--ihqwctvzc91f659drss3x8bo0yb",
    "xn--TisaohkhngthchnitingVit-kjcr8268qyxafd2f1b9g",
    "xn--3B-ww4c5e180e575a65lsy2b",
    "xn---with-SUPER-MONKEYS-pc58ag80a8qai00g7n9n",
    "xn--Hello-Another-Way--fc4qua05auwb3674vfr0b",
    "xn--2-u9tlzr9756bt3uc0v",
    "xn--MajiKoi5-783gue6qz075azm5e",
    "xn--de-jg4avhby1noc0d",
    "xn--d9juau41awczczp",
    "XN--097CCDEKGHQJK",
    "XN--db8CBHEJLGH4E0AL",
    "xn--hxargifdar",                       // Greek
    "xn--bonusaa-5bb1da",                   // Maltese
    "xn--b1abfaaepdrnnbgefbadotcwatmq2g4l", // Russian (Cyrillic)

};

static const char *domainNames[] = {
    "slip129-37-118-146.nc.us.ibm.net",
    "saratoga.pe.utexas.edu",
    "dial-120-45.ots.utexas.edu",
    "woo-085.dorms.waller.net",
    "hd30-049.hil.compuserve.com",
    "pem203-31.pe.ttu.edu",
    "56K-227.MaxTNT3.pdq.net",
    "dial-36-2.ots.utexas.edu",
    "slip129-37-23-152.ga.us.ibm.net",
    "ts45ip119.cadvision.com",
    "sdn-ts-004txaustP05.dialsprint.net",
    "bar-tnt1s66.erols.com",
    "101.st-louis-15.mo.dial-access.att.net",
    "h92-245.Arco.COM",
    "dial-13-2.ots.utexas.edu",
    "net-redynet29.datamarkets.com.ar",
    "ccs-shiva28.reacciun.net.ve",
    "7.houston-11.tx.dial-access.att.net",
    "ingw129-37-120-26.mo.us.ibm.net",
    "dialup6.austintx.com",
    "dns2.tpao.gov.tr",
    "slip129-37-119-194.nc.us.ibm.net",
    "cs7.dillons.co.uk.203.119.193.in-addr.arpa",
    "swprd1.innovplace.saskatoon.sk.ca",
    "bikini.bologna.maraut.it",
    "node91.subnet159-198-79.baxter.com",
    "cust19.max5.new-york.ny.ms.uu.net",
    "balexander.slip.andrew.cmu.edu",
    "pool029.max2.denver.co.dynip.alter.net",
    "cust49.max9.new-york.ny.ms.uu.net",
    "s61.abq-dialin2.hollyberry.com",
    "http://\\u0917\\u0928\\u0947\\u0936.sanjose.ibm.com",
    "www.xn--vea.com",
    "www.\\u00E0\\u00B3\\u00AF.com",
    "www.\\u00C2\\u00A4.com",
    "www.\\u00C2\\u00A3.com",
    "\\u0025",
    "\\u005C\\u005C",
    "@",
    "\\u002F",
    "www.\\u0021.com",
    "www.\\u0024.com",
    "\\u003f",
    // These yeild U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
    //"\\u00CF\\u0082.com",
    //"\\u00CE\\u00B2\\u00C3\\u009Fss.com",
    //"\\u00E2\\u0098\\u00BA.com",
    "\\u00C3\\u00BC.com",

};

typedef struct ErrorCases ErrorCases;

static struct ErrorCases{

    UChar unicode[100];
    const char *ascii;
    UErrorCode expected;
    UBool useSTD3ASCIIRules;
    UBool testToUnicode;
    UBool testLabel;
} errorCases[] = {
      {
        
        { 
            0x0077, 0x0077, 0x0077, 0x002e, /* www. */
            0xC138, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
            0x2060,/*prohibited*/
            0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
            0x002e, 0x0063, 0x006f, 0x006d, /* com. */
            0x0000
        },
        "www.XN--fxG2146CsoA28OruCyA378BqrE2tCwOp06C5qBw82A1rFfmAE0361DeA96B.com",
        U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR,
        FALSE, TRUE, TRUE
    },

    {
        { 
            0x0077, 0x0077, 0x0077, 0x002e, /* www. */
            0xC138, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
            0x0221, 0x0234/*Unassigned code points*/,
            0x002e, 0x0063, 0x006f, 0x006d, /* com. */
            0x0000
        },
        "www.XN--6lA2Bz548Fj1GuA391Bf1Gb1N59Ab29A7iA.com",

        U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR,
        FALSE, TRUE, TRUE
    },
    {
        { 
            0x0077, 0x0077, 0x0077, 0x002e, /* www. */
            0xC138, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
            0x0644, 0x064A, 0x0647,/*Arabic code points. Cannot mix RTL with LTR*/
            0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
            0x002e, 0x0063, 0x006f, 0x006d, /* com. */
            0x0000
        },
        "www.xn--ghBGI4851OiyA33VqrD6Az86C4qF83CtRv93D5xBk15AzfG0nAgA0578DeA71C.com",
        U_IDNA_CHECK_BIDI_ERROR,
        FALSE, TRUE, TRUE
    },
    {
        { 
            0x0077, 0x0077, 0x0077, 0x002e, /* www. */
            /* labels cannot begin with an HYPHEN */
            0x002D, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
            0x002E, 
            0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
            0x002e, 0x0063, 0x006f, 0x006d, /* com. */
            0x0000
            
        },
        "www.xn----b95Ew8SqA315Ao5FbuMlnNmhA.com",
        U_IDNA_STD3_ASCII_RULES_ERROR,
        TRUE, TRUE, FALSE
    },
    {
        { 
            /* correct ACE-prefix followed by unicode */
            0x0077, 0x0077, 0x0077, 0x002e, /* www. */
            0x0078, 0x006e, 0x002d,0x002d,  /* ACE Prefix */
            0x002D, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
            0x002D, 
            0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
            0x002e, 0x0063, 0x006f, 0x006d, /* com. */
            0x0000
            
        },
        /* wrong ACE-prefix followed by valid ACE-encoded ASCII */ 
        "www.XY-----b91I0V65S96C2A355Cw1E5yCeQr19CsnP1mFfmAE0361DeA96B.com",
        U_IDNA_ACE_PREFIX_ERROR,
        FALSE, FALSE, FALSE
    },
    /* cannot verify U_IDNA_VERIFICATION_ERROR */

    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, /* www. */
        0xC138, 0xACC4, 0xC758, 0xBAA8, 0xB4E0, 0xC0AC, 0xB78C, 0xB4E4, 0xC774,
        0xD55C, 0xAD6D, 0xC5B4, 0xB97C, 0xC774, 0xD574, 0xD55C, 0xB2E4, 0xBA74,
        0xC5BC, 0xB9C8, 0xB098, 0xC88B, 0xC744, 0xAE4C, 
        0x002e, 0x0063, 0x006f, 0x006d, /* com. */
        0x0000
      },
      "www.xn--989AoMsVi5E83Db1D2A355Cv1E0vAk1DwRv93D5xBh15A0Dt30A5JpSD879Ccm6FeA98C.com",
      U_IDNA_LABEL_TOO_LONG_ERROR,
      FALSE, TRUE, TRUE
    },  
    
    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, /* www. */
        0x0030, 0x0644, 0x064A, 0x0647, 0x0031, /* Arabic code points squashed between EN codepoints */
        0x002e, 0x0063, 0x006f, 0x006d, /* com. */
        0x0000
      },
      "www.xn--01-tvdmo.com",
      U_IDNA_CHECK_BIDI_ERROR,
      FALSE, TRUE, TRUE
    },  
    
    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, // www. 
        0x206C, 0x0644, 0x064A, 0x0647, 0x206D, // Arabic code points squashed between BN codepoints 
        0x002e, 0x0063, 0x006f, 0x006d, // com. 
        0x0000
      },
      "www.XN--ghbgi278xia.com",
      U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR,
      FALSE, TRUE, TRUE
    },
    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, // www. 
        0x002D, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, // HYPHEN at the start of label 
        0x002e, 0x0063, 0x006f, 0x006d, // com. 
        0x0000
      },
      "www.-abcde.com",
      U_IDNA_STD3_ASCII_RULES_ERROR,
      TRUE, TRUE, FALSE
    },
    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, // www. 
        0x0041, 0x0042, 0x0043, 0x0044, 0x0045,0x002D, // HYPHEN at the end of the label
        0x002e, 0x0063, 0x006f, 0x006d, // com. 
        0x0000
      },
      "www.abcde-.com",
      U_IDNA_STD3_ASCII_RULES_ERROR,
      TRUE, TRUE, FALSE
    },
    { 
      {
        0x0077, 0x0077, 0x0077, 0x002e, // www. 
        0x0041, 0x0042, 0x0043, 0x0044, 0x0045,0x0040, // Containing non LDH code point
        0x002e, 0x0063, 0x006f, 0x006d, // com. 
        0x0000
      },
      "www.abcde@.com",
      U_IDNA_STD3_ASCII_RULES_ERROR,
      TRUE, TRUE, FALSE
    },
    { 
      {0},
      NULL,
      U_ILLEGAL_ARGUMENT_ERROR,
      TRUE, TRUE, FALSE
    }
};


static struct ConformanceTestCases
   {
     const char *comment;
     const char *in;
     const char *out;
     const char *profile;
     int32_t flags;
     UErrorCode expectedStatus;
   }
   conformanceTestCases[] =
   {
  
     {
       "Case folding ASCII U+0043 U+0041 U+0046 U+0045",
       "\x43\x41\x46\x45", "\x63\x61\x66\x65",
       "Nameprep", UIDNA_DEFAULT, 
       U_ZERO_ERROR

     },
     {
       "Case folding 8bit U+00DF (german sharp s)",
       "\xC3\x9F", "\x73\x73", 
       "Nameprep", UIDNA_DEFAULT, 
       U_ZERO_ERROR  
     },
     {
       "Non-ASCII multibyte space character U+1680",
       "\xE1\x9A\x80", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Non-ASCII 8bit control character U+0085",
       "\xC2\x85", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Non-ASCII multibyte control character U+180E",
       "\xE1\xA0\x8E", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Non-ASCII control character U+1D175",
       "\xF0\x9D\x85\xB5", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Plane 0 private use character U+F123",
       "\xEF\x84\xA3", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Plane 15 private use character U+F1234",
       "\xF3\xB1\x88\xB4", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Plane 16 private use character U+10F234",
       "\xF4\x8F\x88\xB4", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Non-character code point U+8FFFE",
       "\xF2\x8F\xBF\xBE", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Non-character code point U+10FFFF",
       "\xF4\x8F\xBF\xBF", NULL,
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR 
     },
 /* 
     {
       "Surrogate code U+DF42",
       "\xED\xBD\x82", NULL, "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
*/
     {
       "Non-plain text character U+FFFD",
       "\xEF\xBF\xBD", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Ideographic description character U+2FF5",
       "\xE2\xBF\xB5", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Display property character U+0341",
       "\xCD\x81", "\xCD\x81",
       "Nameprep", UIDNA_DEFAULT, U_ZERO_ERROR

     },

     {
       "Left-to-right mark U+200E",
       "\xE2\x80\x8E", "\xCC\x81", 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {

       "Deprecated U+202A",
       "\xE2\x80\xAA", "\xCC\x81", 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Language tagging character U+E0001",
       "\xF3\xA0\x80\x81", "\xCC\x81", 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Language tagging character U+E0042",
       "\xF3\xA0\x81\x82", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_PROHIBITED_CODEPOINT_FOUND_ERROR
     },
     {
       "Bidi: RandALCat character U+05BE and LCat characters",
       "\x66\x6F\x6F\xD6\xBE\x62\x61\x72", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_CHECK_BIDI_ERROR
     },
     {
       "Bidi: RandALCat character U+FD50 and LCat characters",
       "\x66\x6F\x6F\xEF\xB5\x90\x62\x61\x72", NULL,
       "Nameprep",UIDNA_DEFAULT ,
       U_IDNA_CHECK_BIDI_ERROR
     },
     {
       "Bidi: RandALCat character U+FB38 and LCat characters",
       "\x66\x6F\x6F\xEF\xB9\xB6\x62\x61\x72", "\x66\x6F\x6F \xd9\x8e\x62\x61\x72",
       "Nameprep", UIDNA_DEFAULT,
       U_ZERO_ERROR
     },
     { "Bidi: RandALCat without trailing RandALCat U+0627 U+0031",
       "\xD8\xA7\x31", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_CHECK_BIDI_ERROR
     },
     {
       "Bidi: RandALCat character U+0627 U+0031 U+0628",
       "\xD8\xA7\x31\xD8\xA8", "\xD8\xA7\x31\xD8\xA8",
       "Nameprep", UIDNA_DEFAULT,
       U_ZERO_ERROR
     },
     {
       "Unassigned code point U+E0002",
       "\xF3\xA0\x80\x82", NULL, 
       "Nameprep", UIDNA_DEFAULT,
       U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR
     },

/*  // Invalid UTF-8
     {
       "Larger test (shrinking)",
       "X\xC2\xAD\xC3\xDF\xC4\xB0\xE2\x84\xA1\x6a\xcc\x8c\xc2\xa0\xc2"
       "\xaa\xce\xb0\xe2\x80\x80", "xssi\xcc\x87""tel\xc7\xb0 a\xce\xb0 ",
       "Nameprep",
       UIDNA_DEFAULT, U_ZERO_ERROR
     },
    {

       "Larger test (expanding)",
       "X\xC3\xDF\xe3\x8c\x96\xC4\xB0\xE2\x84\xA1\xE2\x92\x9F\xE3\x8c\x80",
       "xss\xe3\x82\xad\xe3\x83\xad\xe3\x83\xa1\xe3\x83\xbc\xe3\x83\x88"
       "\xe3\x83\xab""i\xcc\x87""tel\x28""d\x29\xe3\x82\xa2\xe3\x83\x91"
       "\xe3\x83\xbc\xe3\x83\x88"
       "Nameprep",
       UIDNA_DEFAULT, U_ZERO_ERROR
     },
  */
};



#define MAX_DEST_SIZE 300



void TestIDNA::testAPI(const UChar* src, const UChar* expected, const char* testName, 
            UBool useSTD3ASCIIRules,UErrorCode expectedStatus,
            UBool doCompare, UBool testUnassigned,  TestFunc func){

    UErrorCode status = U_ZERO_ERROR;
    UChar destStack[MAX_DEST_SIZE];
    int32_t destLen = 0;
    UChar* dest = NULL;
    int32_t expectedLen = (expected != NULL) ? u_strlen(expected) : 0;
    int32_t options = (useSTD3ASCIIRules == TRUE) ? UIDNA_USE_STD3_RULES : UIDNA_DEFAULT;
    UParseError parseError;
    int32_t tSrcLen = 0; 
    UChar* tSrc = NULL; 

    if(src != NULL){
        tSrcLen = u_strlen(src);
        tSrc  =(UChar*) uprv_malloc( U_SIZEOF_UCHAR * tSrcLen );
        uprv_memcpy(tSrc,src,tSrcLen * U_SIZEOF_UCHAR);
    }

    // test null-terminated source and return value of number of UChars required
    if( expectedStatus != U_IDNA_STD3_ASCII_RULES_ERROR ){
        destLen = func(src,-1,dest,0,options, &parseError , &status);
        if(status == U_BUFFER_OVERFLOW_ERROR){
            status = U_ZERO_ERROR; // reset error code
            if(destLen+1 < MAX_DEST_SIZE){
                dest = destStack;
                destLen = func(src,-1,dest,destLen+1,options, &parseError, &status);
                // TODO : compare output with expected
                if(U_SUCCESS(status) && expectedStatus != U_IDNA_STD3_ASCII_RULES_ERROR&& (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                    errln("Did not get the expected result for "+UnicodeString(testName) +" null terminated source. Expected : " 
                           + prettify(UnicodeString(expected,expectedLen))
                           + " Got: " + prettify(UnicodeString(dest,destLen))
                        );
                }
            }else{
                errln( "%s null terminated source failed. Requires destCapacity > 300\n",testName);
            }
        }

        if(status != expectedStatus){
            errln( "Did not get the expected error for %s null terminated source failed. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
            uprv_free(tSrc);
            return;
        } 
        if(testUnassigned ){
            status = U_ZERO_ERROR;
            destLen = func(src,-1,dest,0,options | UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
            if(status == U_BUFFER_OVERFLOW_ERROR){
                status = U_ZERO_ERROR; // reset error code
                if(destLen+1 < MAX_DEST_SIZE){
                    dest = destStack;
                    destLen = func(src,-1,dest,destLen+1,options | UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
                    // TODO : compare output with expected
                    if(U_SUCCESS(status) && (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                        //errln("Did not get the expected result for %s null terminated source with both options set.\n",testName);
                        errln("Did not get the expected result for "+UnicodeString(testName) +" null terminated source with both options set. Expected: "+ prettify(UnicodeString(expected,expectedLen)));
                
                    }
                }else{
                    errln( "%s null terminated source failed. Requires destCapacity > 300\n",testName);
                }
            }
            //testing query string
            if(status != expectedStatus && expectedStatus != U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR){
                errln( "Did not get the expected error for %s null terminated source with options set. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
            }  
        }

        status = U_ZERO_ERROR;

        // test source with lengthand return value of number of UChars required
        destLen = func(tSrc, tSrcLen, dest,0,options, &parseError, &status);
        if(status == U_BUFFER_OVERFLOW_ERROR){
            status = U_ZERO_ERROR; // reset error code
            if(destLen+1 < MAX_DEST_SIZE){
                dest = destStack;
                destLen = func(src,u_strlen(src),dest,destLen+1,options, &parseError, &status);
                // TODO : compare output with expected
                if(U_SUCCESS(status) && (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                    errln("Did not get the expected result for %s with source length.\n",testName);
                }
            }else{
                errln( "%s with source length  failed. Requires destCapacity > 300\n",testName);
            }
        }

        if(status != expectedStatus){
            errln( "Did not get the expected error for %s with source length. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
        } 
        if(testUnassigned){
            status = U_ZERO_ERROR;

            destLen = func(tSrc,tSrcLen,dest,0,options | UIDNA_ALLOW_UNASSIGNED, &parseError, &status);

            if(status == U_BUFFER_OVERFLOW_ERROR){
                status = U_ZERO_ERROR; // reset error code
                if(destLen+1 < MAX_DEST_SIZE){
                    dest = destStack;
                    destLen = func(src,u_strlen(src),dest,destLen+1,options | UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
                    // TODO : compare output with expected
                    if(U_SUCCESS(status) && (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                        errln("Did not get the expected result for %s with source length and both options set.\n",testName);
                    }
                }else{
                    errln( "%s with source length  failed. Requires destCapacity > 300\n",testName);
                }
            }
            //testing query string
            if(status != expectedStatus && expectedStatus != U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR){
                errln( "Did not get the expected error for %s with source length and options set. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
            }
        }
    }else{

        status = U_ZERO_ERROR;
        destLen = func(src,-1,dest,0,options | UIDNA_USE_STD3_RULES, &parseError, &status);
        if(status == U_BUFFER_OVERFLOW_ERROR){
            status = U_ZERO_ERROR; // reset error code
            if(destLen+1 < MAX_DEST_SIZE){
                dest = destStack;
                destLen = func(src,-1,dest,destLen+1,options | UIDNA_USE_STD3_RULES, &parseError, &status);
                // TODO : compare output with expected
                if(U_SUCCESS(status) && (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                    //errln("Did not get the expected result for %s null terminated source with both options set.\n",testName);
                    errln("Did not get the expected result for "+UnicodeString(testName) +" null terminated source with both options set. Expected: "+ prettify(UnicodeString(expected,expectedLen)));
            
                }
            }else{
                errln( "%s null terminated source failed. Requires destCapacity > 300\n",testName);
            }
        }
        //testing query string
        if(status != expectedStatus){
            errln( "Did not get the expected error for %s null terminated source with options set. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
        } 

        status = U_ZERO_ERROR;

        destLen = func(tSrc,tSrcLen,dest,0,options | UIDNA_USE_STD3_RULES, &parseError, &status);

        if(status == U_BUFFER_OVERFLOW_ERROR){
            status = U_ZERO_ERROR; // reset error code
            if(destLen+1 < MAX_DEST_SIZE){
                dest = destStack;
                destLen = func(src,u_strlen(src),dest,destLen+1,options | UIDNA_USE_STD3_RULES, &parseError, &status);
                // TODO : compare output with expected
                if(U_SUCCESS(status) && (doCompare==TRUE) && u_strCaseCompare(dest,destLen, expected,expectedLen,0,&status)!=0){
                    errln("Did not get the expected result for %s with source length and both options set.\n",testName);
                }
            }else{
                errln( "%s with source length  failed. Requires destCapacity > 300\n",testName);
            }
        }
        //testing query string
        if(status != expectedStatus && expectedStatus != U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR){
            errln( "Did not get the expected error for %s with source length and options set. Expected: %s Got: %s\n",testName, u_errorName(expectedStatus), u_errorName(status));
        }
    }
    uprv_free(tSrc);
}

void TestIDNA::testCompare(const UChar* s1, int32_t s1Len,
                        const UChar* s2, int32_t s2Len,
                        const char* testName, CompareFunc func,
                        UBool isEqual){

    UErrorCode status = U_ZERO_ERROR;
    int32_t retVal = func(s1,-1,s2,-1,UIDNA_DEFAULT,&status);

    if(isEqual==TRUE &&  retVal !=0){
        errln("Did not get the expected result for %s with null termniated strings.\n",testName);
    }
    if(U_FAILURE(status)){
        errln( "%s null terminated source failed. Error: %s\n", testName,u_errorName(status));
    }

    status = U_ZERO_ERROR;
    retVal = func(s1,-1,s2,-1,UIDNA_ALLOW_UNASSIGNED,&status);

    if(isEqual==TRUE &&  retVal !=0){
        errln("Did not get the expected result for %s with null termniated strings with options set.\n", testName);
    }
    if(U_FAILURE(status)){
        errln( "%s null terminated source and options set failed. Error: %s\n",testName, u_errorName(status));
    }
    
    status = U_ZERO_ERROR;
    retVal = func(s1,s1Len,s2,s2Len,UIDNA_DEFAULT,&status);

    if(isEqual==TRUE &&  retVal !=0){
        errln("Did not get the expected result for %s with string length.\n",testName);
    }
    if(U_FAILURE(status)){
        errln( "%s with string length. Error: %s\n",testName, u_errorName(status));
    }
    
    status = U_ZERO_ERROR;
    retVal = func(s1,s1Len,s2,s2Len,UIDNA_ALLOW_UNASSIGNED,&status);

    if(isEqual==TRUE &&  retVal !=0){
        errln("Did not get the expected result for %s with string length and options set.\n",testName);
    }
    if(U_FAILURE(status)){
        errln( "%s with string length and options set. Error: %s\n", u_errorName(status), testName);
    }
}

void TestIDNA::testToASCII(const char* testName, TestFunc func){

    int32_t i;
    UChar buf[MAX_DEST_SIZE];

    for(i=0;i< (int32_t)(sizeof(unicodeIn)/sizeof(unicodeIn[0])); i++){
        u_charsToUChars(asciiIn[i],buf, uprv_strlen(asciiIn[i])+1);
        testAPI(unicodeIn[i], buf,testName, FALSE,U_ZERO_ERROR, TRUE, TRUE, func);
        
    }
}

void TestIDNA::testToUnicode(const char* testName, TestFunc func){

    int32_t i;
    UChar buf[MAX_DEST_SIZE];
    
    for(i=0;i< (int32_t)(sizeof(asciiIn)/sizeof(asciiIn[0])); i++){
        u_charsToUChars(asciiIn[i],buf, uprv_strlen(asciiIn[i])+1);
        testAPI(buf,unicodeIn[i],testName,FALSE,U_ZERO_ERROR, TRUE, TRUE, func);
    }
}


void TestIDNA::testIDNToUnicode(const char* testName, TestFunc func){
    int32_t i;
    UChar buf[MAX_DEST_SIZE];
    UChar expected[MAX_DEST_SIZE];
    UErrorCode status = U_ZERO_ERROR;
    int32_t bufLen = 0;
    UParseError parseError;
    for(i=0;i< (int32_t)(sizeof(domainNames)/sizeof(domainNames[0])); i++){
        bufLen = uprv_strlen(domainNames[i]);
        bufLen = u_unescape(domainNames[i],buf, bufLen+1);
        func(buf,bufLen,expected,MAX_DEST_SIZE, UIDNA_ALLOW_UNASSIGNED, &parseError,&status);
        if(U_FAILURE(status)){
            errln( "%s failed to convert domainNames[%i].Error: %s \n",testName, i, u_errorName(status));
            break;
        }
        testAPI(buf,expected,testName,FALSE,U_ZERO_ERROR, TRUE, TRUE, func);
         //test toUnicode with all labels in the string
        testAPI(buf,expected,testName, FALSE,U_ZERO_ERROR, TRUE, TRUE, func);
        if(U_FAILURE(status)){
            errln( "%s failed to convert domainNames[%i].Error: %s \n",testName,i, u_errorName(status));
            break;
        }
    }
    
}

void TestIDNA::testIDNToASCII(const char* testName, TestFunc func){
    int32_t i;
    UChar buf[MAX_DEST_SIZE];
    UChar expected[MAX_DEST_SIZE];
    UErrorCode status = U_ZERO_ERROR;
    int32_t bufLen = 0;
    UParseError parseError; 
    for(i=0;i< (int32_t)(sizeof(domainNames)/sizeof(domainNames[0])); i++){
        bufLen = uprv_strlen(domainNames[i]);
        bufLen = u_unescape(domainNames[i],buf, bufLen+1);
        func(buf,bufLen,expected,MAX_DEST_SIZE, UIDNA_ALLOW_UNASSIGNED, &parseError,&status);
        if(U_FAILURE(status)){
            errln( "%s failed to convert domainNames[%i].Error: %s \n",testName,i, u_errorName(status));
            break;
        }
        testAPI(buf,expected,testName, FALSE,U_ZERO_ERROR, TRUE, TRUE, func);
        //test toASCII with all labels in the string
        testAPI(buf,expected,testName, FALSE,U_ZERO_ERROR, FALSE, TRUE, func);
        if(U_FAILURE(status)){
            errln( "%s failed to convert domainNames[%i].Error: %s \n",testName,i, u_errorName(status));
            break;
        }
    }
    
}

void TestIDNA::testCompare(const char* testName, CompareFunc func){
    int32_t i;


    UChar www[] = {0x0057, 0x0057, 0x0057, 0x002E, 0x0000};
    UChar com[] = {0x002E, 0x0043, 0x004F, 0x004D, 0x0000};
    UChar buf[MAX_DEST_SIZE]={0x0057, 0x0057, 0x0057, 0x002E, 0x0000};

    UnicodeString source(www), uni0(www),uni1(www), ascii0(www), ascii1(www);

    uni0.append(unicodeIn[0]);
    uni0.append(com);
    uni0.append((UChar)0x0000);

    uni1.append(unicodeIn[1]);
    uni1.append(com);
    uni1.append((UChar)0x0000);

    ascii0.append(asciiIn[0]);
    ascii0.append(com);
    ascii0.append((UChar)0x0000);

    ascii1.append(asciiIn[1]);
    ascii1.append(com);
    ascii1.append((UChar)0x0000);

    for(i=0;i< (int32_t)(sizeof(unicodeIn)/sizeof(unicodeIn[0])); i++){

        u_charsToUChars(asciiIn[i],buf+4, uprv_strlen(asciiIn[i])+1);
        u_strcat(buf,com);

        // for every entry in unicodeIn array
        // prepend www. and append .com
        source.truncate(4);
        source.append(unicodeIn[i]);
        source.append(com);
        source.append((UChar)0x0000);
        // a) compare it with itself
        const UChar* src = source.getBuffer();
        int32_t srcLen = u_strlen(src); //subtract null

        testCompare(src,srcLen,src,srcLen,testName, func, TRUE);
        
        // b) compare it with asciiIn equivalent
        testCompare(src,srcLen,buf,u_strlen(buf),testName, func,TRUE);
        
        // c) compare it with unicodeIn not equivalent
        if(i==0){
            testCompare(src,srcLen,uni1.getBuffer(),uni1.length()-1,testName, func,FALSE);
            uni1.releaseBuffer();
        }else{
            testCompare(src,srcLen,uni0.getBuffer(),uni0.length()-1,testName, func,FALSE);
            uni0.releaseBuffer();
        }
        // d) compare it with asciiIn not equivalent
        if(i==0){
            testCompare(src,srcLen,ascii1.getBuffer(),ascii1.length()-1,testName, func,FALSE);
            ascii1.releaseBuffer();
        }else{
            testCompare(src,srcLen,ascii0.getBuffer(),ascii0.length()-1,testName, func,FALSE);
            ascii0.releaseBuffer();
        }

    }
}

#if 0

static int32_t
getNextSeperator(UChar *src,int32_t srcLength,
                 UChar **limit){
    if(srcLength == -1){
        int32_t i;
        for(i=0 ; ;i++){
            if(src[i] == 0){
                *limit = src + i; // point to null
                return i;
            }
            if(src[i]==0x002e){
                *limit = src + (i+1); // go past the delimiter
                return i;
            }
        }
        // we have not found the delimiter
        if(i==srcLength){
            *limit = src+srcLength;
        }
        return i;
    }else{
        int32_t i;
        for(i=0;i<srcLength;i++){
            if(src[i]==0x002e){
                *limit = src + (i+1); // go past the delimiter
                return i;
            }
        }
        // we have not found the delimiter
        if(i==srcLength){
            *limit = src+srcLength;
        }
        return i;
    }
}

void printPunycodeOutput(){

    UChar dest[MAX_DEST_SIZE];
    int32_t destCapacity=MAX_DEST_SIZE;
    UChar* start;
    UChar* limit;
    int32_t labelLen=0;
    UBool caseFlags[MAX_DEST_SIZE];
    
    for(int32_t i=0;i< sizeof(errorCases)/sizeof(errorCases[0]);i++){
        ErrorCases errorCase = errorCases[i];
        UErrorCode status = U_ZERO_ERROR;
        start = errorCase.unicode;
        int32_t srcLen = u_strlen(start);
        labelLen = getNextSeperator(start,srcLen,&limit);
        start = limit;
        labelLen=getNextSeperator(start,srcLen-labelLen,&limit);
        int32_t destLen = u_strToPunycode(dest,destCapacity,start,labelLen,caseFlags, &status);
        if(U_FAILURE(status)){
            printf("u_strToPunycode failed for index %i\n",i);
            continue;
        }
        for(int32_t j=0; j<destLen; j++){
            printf("%c",(char)dest[j]);
        }
        printf("\n");
    }
}
#endif

void TestIDNA::testErrorCases(const char* toASCIIName, TestFunc toASCII,
                    const char* IDNToASCIIName, TestFunc IDNToASCII,
                    const char* IDNToUnicodeName, TestFunc IDNToUnicode){
    UChar buf[MAX_DEST_SIZE];
    int32_t bufLen=0;

    for(int32_t i=0;i< (int32_t)(sizeof(errorCases)/sizeof(errorCases[0]));i++){
        ErrorCases errorCase = errorCases[i];
        UChar* src =NULL;
        if(errorCase.ascii != NULL){
            bufLen =  uprv_strlen(errorCase.ascii);
            u_charsToUChars(errorCase.ascii,buf, bufLen+1);
        }else{
            bufLen = 1 ;
            memset(buf,0,U_SIZEOF_UCHAR*MAX_DEST_SIZE);
        }
        
        if(errorCase.unicode[0]!=0){
            src = errorCase.unicode;
        }
        // test toASCII
        testAPI(src,buf,
                IDNToASCIIName, errorCase.useSTD3ASCIIRules,
                errorCase.expected, TRUE, TRUE, IDNToASCII);
        if(errorCase.testLabel ==TRUE){
            testAPI(src,buf,
                toASCIIName, errorCase.useSTD3ASCIIRules,
                errorCase.expected, FALSE,TRUE, toASCII);
        }
        if(errorCase.testToUnicode ==TRUE){
            testAPI((src==NULL)? NULL : buf,src,
                    IDNToUnicodeName, errorCase.useSTD3ASCIIRules,
                    errorCase.expected, TRUE, TRUE, IDNToUnicode);   
        }

    }
    
}

void TestIDNA::testConformance(const char* toASCIIName, TestFunc toASCII,
                               const char* IDNToASCIIName, TestFunc IDNToASCII,
                               const char* IDNToUnicodeName, TestFunc IDNToUnicode,
                               const char* toUnicodeName, TestFunc toUnicode){
    UChar src[MAX_DEST_SIZE];
    int32_t srcLen=0;
    UChar expected[MAX_DEST_SIZE];
    int32_t expectedLen = 0;
    for(int32_t i=0;i< (int32_t)(sizeof(conformanceTestCases)/sizeof(conformanceTestCases[0]));i++){
        const char* utf8Chars1 = conformanceTestCases[i].in;
        int32_t utf8Chars1Len = strlen(utf8Chars1);
        const char* utf8Chars2 = conformanceTestCases[i].out;
        int32_t utf8Chars2Len = (utf8Chars2 == NULL) ? 0 : strlen(utf8Chars2);

        UErrorCode status = U_ZERO_ERROR;
        u_strFromUTF8(src,MAX_DEST_SIZE,&srcLen,utf8Chars1,utf8Chars1Len,&status);
        if(U_FAILURE(status)){
            errln(UnicodeString("Conversion of UTF8 source in conformanceTestCases[") + i +UnicodeString( "].in ( ")+prettify(utf8Chars1) +UnicodeString(" ) failed. Error: ")+ UnicodeString(u_errorName(status)));
            continue;
        }
        if(utf8Chars2 != NULL){
            u_strFromUTF8(expected,MAX_DEST_SIZE,&expectedLen,utf8Chars2,utf8Chars2Len, &status);
            if(U_FAILURE(status)){
                errln(UnicodeString("Conversion of UTF8 source in conformanceTestCases[") + i +UnicodeString( "].in ( ")+prettify(utf8Chars1) +UnicodeString(" ) failed. Error: ")+ UnicodeString(u_errorName(status)));
                continue;
            }
        }
        
        if(conformanceTestCases[i].expectedStatus != U_ZERO_ERROR){
            // test toASCII
            testAPI(src,expected,
                    IDNToASCIIName, FALSE,
                    conformanceTestCases[i].expectedStatus, 
                    TRUE, 
                    (conformanceTestCases[i].expectedStatus != U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR),
                    IDNToASCII);

            testAPI(src,expected,
                    toASCIIName, FALSE,
                    conformanceTestCases[i].expectedStatus, TRUE, 
                    (conformanceTestCases[i].expectedStatus != U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR),
                    toASCII);
        }

        testAPI(src,src,
                IDNToUnicodeName, FALSE,
                conformanceTestCases[i].expectedStatus, TRUE, TRUE, IDNToUnicode);
        testAPI(src,src,
                toUnicodeName, FALSE,
                conformanceTestCases[i].expectedStatus, TRUE, TRUE, toUnicode);

    }
    
}

// test and ascertain
// func(func(func(src))) == func(src)
void TestIDNA::testChaining(UChar* src,int32_t numIterations,const char* testName,
                  UBool useSTD3ASCIIRules, UBool caseInsensitive, TestFunc func){
    UChar even[MAX_DEST_SIZE];
    UChar odd[MAX_DEST_SIZE];
    UChar expected[MAX_DEST_SIZE];
    int32_t i=0,evenLen=0,oddLen=0,expectedLen=0;
    UErrorCode status = U_ZERO_ERROR;
    int32_t srcLen = u_strlen(src);
    int32_t options = (useSTD3ASCIIRules == TRUE) ? UIDNA_USE_STD3_RULES : UIDNA_DEFAULT;
    UParseError parseError;

    // test null-terminated source 
    expectedLen = func(src,-1,expected,MAX_DEST_SIZE, options, &parseError, &status);
    if(U_FAILURE(status)){
        errln("%s null terminated source failed. Error: %s\n",testName, u_errorName(status));
    }
    uprv_memcpy(odd,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    uprv_memcpy(even,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    for(;i<=numIterations; i++){
        if((i%2) ==0){
            evenLen = func(odd,-1,even,MAX_DEST_SIZE,options, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s null terminated source failed\n",testName);
                break;
            }
        }else{
            oddLen = func(even,-1,odd,MAX_DEST_SIZE,options, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s null terminated source failed\n",testName);
                break;
            }
        }
    }
    if(caseInsensitive ==TRUE){
        if( u_strCaseCompare(even,evenLen, expected,expectedLen, 0, &status) !=0 ||
            u_strCaseCompare(odd,oddLen, expected,expectedLen, 0, &status) !=0 ){

            errln("Chaining for %s null terminated source failed\n",testName);
        }
    }else{
        if( u_strncmp(even,expected,expectedLen) != 0 ||
            u_strncmp(odd,expected,expectedLen) !=0 ){
        
            errln("Chaining for %s null terminated source failed\n",testName);
        }
    }

    // test null-terminated source 
    status = U_ZERO_ERROR;
    expectedLen = func(src,-1,expected,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
    if(U_FAILURE(status)){
        errln("%s null terminated source with options set failed. Error: %s\n",testName, u_errorName(status));
    }
    uprv_memcpy(odd,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    uprv_memcpy(even,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    for(;i<=numIterations; i++){
        if((i%2) ==0){
            evenLen = func(odd,-1,even,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s null terminated source with options set failed\n",testName);
                break;
            }
        }else{
            oddLen = func(even,-1,odd,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s null terminated source with options set failed\n",testName);
                break;
            }
        }
    }
    if(caseInsensitive ==TRUE){
        if( u_strCaseCompare(even,evenLen, expected,expectedLen, 0, &status) !=0 ||
            u_strCaseCompare(odd,oddLen, expected,expectedLen, 0, &status) !=0 ){

            errln("Chaining for %s null terminated source with options set failed\n",testName);
        }
    }else{
        if( u_strncmp(even,expected,expectedLen) != 0 ||
            u_strncmp(odd,expected,expectedLen) !=0 ){
        
            errln("Chaining for %s null terminated source with options set failed\n",testName);
        }
    }


    // test source with length 
    status = U_ZERO_ERROR;
    expectedLen = func(src,srcLen,expected,MAX_DEST_SIZE,options, &parseError, &status);
    if(U_FAILURE(status)){
        errln("%s null terminated source failed. Error: %s\n",testName, u_errorName(status));
    }
    uprv_memcpy(odd,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    uprv_memcpy(even,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    for(;i<=numIterations; i++){
        if((i%2) ==0){
            evenLen = func(odd,oddLen,even,MAX_DEST_SIZE,options, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s source with source length failed\n",testName);
                break;
            }
        }else{
            oddLen = func(even,evenLen,odd,MAX_DEST_SIZE,options, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s source with source length failed\n",testName);
                break;
            }
        }
    }
    if(caseInsensitive ==TRUE){
        if( u_strCaseCompare(even,evenLen, expected,expectedLen, 0, &status) !=0 ||
            u_strCaseCompare(odd,oddLen, expected,expectedLen, 0, &status) !=0 ){

            errln("Chaining for %s source with source length failed\n",testName);
        }
    }else{
        if( u_strncmp(even,expected,expectedLen) != 0 ||
            u_strncmp(odd,expected,expectedLen) !=0 ){
        
            errln("Chaining for %s source with source length failed\n",testName);
        }
    }
    status = U_ZERO_ERROR;
    expectedLen = func(src,srcLen,expected,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
    if(U_FAILURE(status)){
        errln("%s null terminated source with options set failed. Error: %s\n",testName, u_errorName(status));
    }
    uprv_memcpy(odd,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    uprv_memcpy(even,expected,(expectedLen+1) * U_SIZEOF_UCHAR);
    for(;i<=numIterations; i++){
        if((i%2) ==0){
            evenLen = func(odd,oddLen,even,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s source with source length and options set failed\n",testName);
                break;
            }
        }else{
            oddLen = func(even,evenLen,odd,MAX_DEST_SIZE,options|UIDNA_ALLOW_UNASSIGNED, &parseError, &status);
            if(U_FAILURE(status)){
                errln("%s  source with source length and options set failed\n",testName);
                break;
            }
        }
    }
    if(caseInsensitive ==TRUE){
        if( u_strCaseCompare(even,evenLen, expected,expectedLen, 0, &status) !=0 ||
            u_strCaseCompare(odd,oddLen, expected,expectedLen, 0, &status) !=0 ){

            errln("Chaining for %s  source with source length and options set failed\n",testName);
        }
    }else{
        if( u_strncmp(even,expected,expectedLen) != 0 ||
            u_strncmp(odd,expected,expectedLen) !=0 ){
        
            errln("Chaining for %s  source with source length and options set failed\n",testName);
        }
    }
}
void TestIDNA::testChaining(const char* toASCIIName, TestFunc toASCII,
                  const char* toUnicodeName, TestFunc toUnicode){
    int32_t i;
    UChar buf[MAX_DEST_SIZE];
    
    for(i=0;i< (int32_t)(sizeof(asciiIn)/sizeof(asciiIn[0])); i++){
        u_charsToUChars(asciiIn[i],buf, uprv_strlen(asciiIn[i])+1);
        testChaining(buf,5,toUnicodeName, FALSE, FALSE, toUnicode);
    }
    for(i=0;i< (int32_t)(sizeof(unicodeIn)/sizeof(unicodeIn[0])); i++){
        testChaining(unicodeIn[i], 5,toASCIIName, FALSE, TRUE, toASCII);
    }
}


void TestIDNA::testRootLabelSeparator(const char* testName, CompareFunc func, 
                            const char* IDNToASCIIName, TestFunc IDNToASCII,
                            const char* IDNToUnicodeName, TestFunc IDNToUnicode){
    int32_t i;


    UChar www[] = {0x0057, 0x0057, 0x0057, 0x002E, 0x0000};
    UChar com[] = {0x002E, 0x0043, 0x004F, 0x004D, 0x002E, /* root label separator */0x0000};
    UChar buf[MAX_DEST_SIZE]={0x0057, 0x0057, 0x0057, 0x002E, 0x0000};

    UnicodeString source(www), uni0(www),uni1(www), ascii0(www), ascii1(www);

    uni0.append(unicodeIn[0]);
    uni0.append(com);
    uni0.append((UChar)0x0000);

    uni1.append(unicodeIn[1]);
    uni1.append(com);
    uni1.append((UChar)0x0000);

    ascii0.append(asciiIn[0]);
    ascii0.append(com);
    ascii0.append((UChar)0x0000);

    ascii1.append(asciiIn[1]);
    ascii1.append(com);
    ascii1.append((UChar)0x0000);

    for(i=0;i< (int32_t)(sizeof(unicodeIn)/sizeof(unicodeIn[0])); i++){

        u_charsToUChars(asciiIn[i],buf+4, uprv_strlen(asciiIn[i])+1);
        u_strcat(buf,com);

        // for every entry in unicodeIn array
        // prepend www. and append .com
        source.truncate(4);
        source.append(unicodeIn[i]);
        source.append(com);
        source.append((UChar)0x0000);
        // a) compare it with itself
        const UChar* src = source.getBuffer();
        int32_t srcLen = u_strlen(src); //subtract null

        testCompare(src,srcLen,src,srcLen,testName, func,TRUE);
        
        // b) compare it with asciiIn equivalent
        testCompare(src,srcLen,buf,u_strlen(buf),testName, func,TRUE);
        
        // IDNToASCII comparison
        testAPI(src,buf,IDNToASCIIName,FALSE,U_ZERO_ERROR,TRUE, TRUE, IDNToASCII);
        // IDNToUnicode comparison
        testAPI(buf,src,IDNToUnicodeName, FALSE,U_ZERO_ERROR, TRUE, TRUE, IDNToUnicode);

        // c) compare it with unicodeIn not equivalent
        if(i==0){
            testCompare(src,srcLen,uni1.getBuffer(),uni1.length()-1,testName, func,FALSE);
            uni1.releaseBuffer();
        }else{
            testCompare(src,srcLen,uni0.getBuffer(),uni0.length()-1,testName, func,FALSE);
            uni0.releaseBuffer();
        }
        // d) compare it with asciiIn not equivalent
        if(i==0){
            testCompare(src,srcLen,ascii1.getBuffer(),ascii1.length()-1,testName, func,FALSE);
            ascii1.releaseBuffer();
        }else{
            testCompare(src,srcLen,ascii0.getBuffer(),ascii0.length()-1,testName, func,FALSE);
            ascii0.releaseBuffer();
        }
    }
}   

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void TestIDNA::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln((UnicodeString)"TestSuite IDNA API ");
    switch (index) {

        case 0: name = "TestToASCII"; if (exec) TestToASCII(); break;
        case 1: name = "TestToUnicode"; if (exec) TestToUnicode(); break;
        case 2: name = "TestIDNToASCII"; if (exec) TestIDNToASCII(); break;
        case 3: name = "TestIDNToUnicode"; if (exec) TestIDNToUnicode(); break;
        case 4: name = "TestCompare"; if (exec) TestCompare(); break;
        case 5: name = "TestErrorCases"; if (exec) TestErrorCases(); break;
        case 6: name = "TestChaining"; if (exec) TestChaining(); break;
        case 7: name = "TestRootLabelSeparator"; if(exec) TestRootLabelSeparator(); break;
        case 8: name = "TestCompareReferenceImpl"; if(exec) TestCompareReferenceImpl(); break;
        case 9: name = "TestDataFile"; if(exec) TestDataFile(); break;
        case 10: name = "TestRefIDNA"; if(exec) TestRefIDNA(); break;
        case 11: name = "TestIDNAMonkeyTest"; if(exec) TestIDNAMonkeyTest(); break;
        case 12: name = "TestConformance"; if(exec) TestConformance();break;
        default: name = ""; break; /*needed to end loop*/
    }
}
void TestIDNA::TestToASCII(){
    testToASCII("uidna_toASCII", uidna_toASCII);
}
void TestIDNA::TestToUnicode(){
    testToUnicode("uidna_toUnicode", uidna_toUnicode);
}
void TestIDNA::TestIDNToASCII(){
    testIDNToASCII("uidna_IDNToASCII", uidna_IDNToASCII);
}
void TestIDNA::TestIDNToUnicode(){
    testIDNToUnicode("uidna_IDNToUnicode", uidna_IDNToUnicode);
}
void TestIDNA::TestCompare(){
    testCompare("uidna_compare",uidna_compare);
}
void TestIDNA::TestErrorCases(){
    testErrorCases("uidna_toASCII",uidna_toASCII, "uidna_IDNToASCII",uidna_IDNToASCII,
                    "uidna_IDNToUnicode",uidna_IDNToUnicode);
}
void TestIDNA::TestRootLabelSeparator(){
    testRootLabelSeparator( "uidna_compare",uidna_compare,
                            "uidna_IDNToASCII", uidna_IDNToASCII,
                            "uidna_IDNToUnicode",uidna_IDNToUnicode
                            );
}
void TestIDNA::TestChaining(){
    testChaining("uidna_toASCII",uidna_toASCII, "uidna_toUnicode", uidna_toUnicode);
}
void TestIDNA::TestConformance(){
    testConformance("uidna_toASCII",uidna_toASCII,"uidna_IDNToASCII",uidna_IDNToASCII,
                    "uidna_IDNToUnicode",uidna_IDNToUnicode, "uidna_toUnicode", uidna_toUnicode);
}

static const int loopCount = 100;
static const int maxCharCount = 20;
static const int maxCodePoint = 0x10ffff;
static uint32_t
randul()
{
    static UBool initialized = FALSE;
    if (!initialized)
    {
        srand((unsigned)time(NULL));
        initialized = TRUE;
    }
    // Assume rand has at least 12 bits of precision
    uint32_t l = 0;
    for (uint32_t i=0; i<sizeof(l); ++i)
        ((char*)&l)[i] = (char)((rand() & 0x0FF0) >> 4);
    return l;
}

/**
 * Return a random integer i where 0 <= i < n.
 * A special function that gets random codepoints from planes 0,1,2 and 14
 */
static int32_t rand_uni()
{
   int32_t retVal = (int32_t)(randul()& 0x3FFFF);
   if(retVal >= 0x30000){
       retVal+=0xB0000;
   }
   return retVal;
}

static int32_t randi(int32_t n){
    return (int32_t) (randul() % (n+1));
}

void getTestSource(UnicodeString& fillIn) {
    int32_t i = 0;
    int32_t charCount = (randi(maxCharCount) + 1);
    while (i <charCount ) {
        int32_t codepoint = rand_uni();
        if(codepoint == 0x0000){
            continue;
        }
        fillIn.append((UChar32)codepoint);
        i++;
    }
       
}
void TestIDNA::testCompareReferenceImpl(const UChar* src, int32_t srcLen){
    
    UnicodeString label, idn("www.");
    
    label.append(src, srcLen);
    label.append((UChar)0x0000);

    {
        
        const UChar* labelUChars = label.getBuffer();
        UChar ascii[MAX_DEST_SIZE]={0};
        int32_t asciiCapacity = MAX_DEST_SIZE, asciiLen=0;
        UChar uni[MAX_DEST_SIZE]={0};
        int32_t uniCapacity = MAX_DEST_SIZE, uniLen=0;
        UErrorCode expectedStatus = U_ZERO_ERROR;
        UParseError parseError;
        //ascii[0] = uni[0] =0;
        logln("Comparing idnaref_toASCII with uidna_toASCII for input: " + prettify(label));
        
        asciiLen = idnaref_toASCII(labelUChars, label.length()-1,ascii,asciiCapacity,
                                      UIDNA_DEFAULT,&parseError,&expectedStatus);

        if(expectedStatus == U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR){
            expectedStatus = U_ZERO_ERROR;
            asciiLen = idnaref_toASCII(labelUChars, label.length()-1,ascii,asciiCapacity,
                            UIDNA_ALLOW_UNASSIGNED,&parseError,&expectedStatus);
            expectedStatus = U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR;
        }

        testAPI(labelUChars,ascii, "uidna_toASCII",FALSE,
                expectedStatus,TRUE, TRUE, uidna_toASCII);
        if(expectedStatus == U_ZERO_ERROR){
            logln("Comparing idnaref_toUnicode with uidna_toUnicode for input: " + prettify(label));
            expectedStatus = U_ZERO_ERROR;
            uniLen = idnaref_toUnicode(ascii, asciiLen, uni,uniCapacity,UIDNA_DEFAULT,
                                        &parseError,&expectedStatus);
            if(expectedStatus == U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR){
                expectedStatus = U_ZERO_ERROR;
                uniLen = idnaref_toUnicode(ascii, asciiLen, uni,uniCapacity,UIDNA_DEFAULT,
                                        &parseError,&expectedStatus);
                expectedStatus = U_IDNA_UNASSIGNED_CODEPOINT_FOUND_ERROR;
            }
            testAPI(ascii,uni,"uidna_toUnicode",FALSE,expectedStatus,TRUE, FALSE, uidna_toUnicode);
        }
    }

}
void TestIDNA::TestIDNAMonkeyTest(){
    UnicodeString source;
    UErrorCode status = U_ZERO_ERROR;

    getInstance(status);    // Init prep
    
    for(int i=0; i<loopCount; i++){
        source.truncate(0);
        getTestSource(source);
        source.append((UChar)0x0000);
        testCompareReferenceImpl(source.getBuffer(),source.length()-1);
        source.releaseBuffer();
    }
    /* for debugging
    source.append("\\U000E5BC8\\U00025112\\U00016846\\U0001B375\\U0002EDE4"
                  "\\U00016E18\\U00010B84\\U000E1639\\U0001C3BE\\u336B\\u5F66"
                  "\\u2AA6\\uD817\\u0000");
    source = source.unescape();
    testCompareReferenceImpl(source.getBuffer(),source.length()-1);
    source.releaseBuffer();
    */

    delete TestIDNA::prep;
    TestIDNA::prep = NULL;
}

void TestIDNA::TestCompareReferenceImpl(){
    
    UChar src [2] = {0,0};
    int32_t srcLen = 0;
    

    for(int32_t i = 0x40000 ; i< 0x10ffff; i++){
        if(quick==TRUE && i> 0x1FFFF){
            return;
        }
        if(i >= 0x30000){
           i+=0xB0000;
        }
        if(i>0xFFFF){
           src[0] = U16_LEAD(i);
           src[1] = U16_TRAIL(i);
           srcLen =2;
        }else{
            src[0] = (UChar)i;
            src[1] = 0;
            srcLen = 1;
        }
        testCompareReferenceImpl(src, srcLen);
    }
}

void TestIDNA::TestRefIDNA(){
    testToASCII("idnaref_toASCII", idnaref_toASCII);
    testToUnicode("idnaref_toUnicode", idnaref_toUnicode);
    testIDNToASCII("idnaref_IDNToASCII", idnaref_IDNToASCII);
    testIDNToUnicode("idnaref_IDNToUnicode", idnaref_IDNToUnicode);
    testCompare("idnaref_compare",idnaref_compare);
    testErrorCases("idnaref_toASCII",idnaref_toASCII, "idnaref_IDNToASCII",idnaref_IDNToASCII,
                    "idnaref_IDNToUnicode",idnaref_IDNToUnicode);
    testChaining("idnaref_toASCII",idnaref_toASCII, "idnaref_toUnicode", idnaref_toUnicode);

    testRootLabelSeparator( "idnaref_compare",idnaref_compare,
                            "idnaref_IDNToASCII", idnaref_IDNToASCII,
                            "idnaref_IDNToUnicode",idnaref_IDNToUnicode
                            );
    testChaining("idnaref_toASCII",idnaref_toASCII, "idnaref_toUnicode", idnaref_toUnicode);
    delete TestIDNA::prep;
    TestIDNA::prep = NULL;
}

void TestIDNA::TestDataFile(){
     testData(*this);
}

#endif /* #if !UCONFIG_NO_IDNA */
