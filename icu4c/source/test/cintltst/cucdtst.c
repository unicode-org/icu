/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CUCDTST.C
*
* Modification History:
*        Name                     Description
*     Madhu Katragadda            Ported for C API, added tests for string functions
*********************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"
#include "unicode/putil.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"

#include "cintltst.h"
#include "cucdtst.h"
#include "uparse.h"
#include "unicode/uscript.h"
#include "usc_impl.h"
#include "uprops.h"

#include <string.h>
#include <math.h>
#include <stdlib.h>

/* prototypes --------------------------------------------------------------- */

static void setUpDataTable(void);
static void cleanUpDataTable(void);

static void TestUpperLower(void);
static void TestLetterNumber(void);
static void TestMisc(void);
static void TestControlPrint(void);
static void TestIdentifier(void);
static void TestUnicodeData(void);
static void TestCodeUnit(void);
static void TestCodePoint(void);
static void TestCharLength(void);
static void TestStringCopy(void);
static void TestStringFunctions(void);
static void TestStringSearching(void);
static void TestCharNames(void);
static void TestMirroring(void);
static void TestUnescape(void);
static void TestUScriptCodeAPI(void);
static void TestUScriptRunAPI(void);
static void TestAdditionalProperties(void);
static void TestNumericProperties(void);

/* internal methods used */
static int32_t MakeProp(char* str);
static int32_t MakeDir(char* str);

#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))

/* get the sign of an integer */
#define _SIGN(value) ((value)==0 ? 0 : ((int32_t)(value)>>31)|1)

/* test data ---------------------------------------------------------------- */

UChar*** dataTable = NULL;
const UChar  LAST_CHAR_CODE_IN_FILE = 0xFFFD;
const char tagStrings[] = "MnMcMeNdNlNoZsZlZpCcCfCsCoCnLuLlLtLmLoPcPdPsPePoSmScSkSoPiPf";
const int32_t tagValues[] =
    {
    /* Mn */ U_NON_SPACING_MARK,
    /* Mc */ U_COMBINING_SPACING_MARK,
    /* Me */ U_ENCLOSING_MARK,
    /* Nd */ U_DECIMAL_DIGIT_NUMBER,
    /* Nl */ U_LETTER_NUMBER,
    /* No */ U_OTHER_NUMBER,
    /* Zs */ U_SPACE_SEPARATOR,
    /* Zl */ U_LINE_SEPARATOR,
    /* Zp */ U_PARAGRAPH_SEPARATOR,
    /* Cc */ U_CONTROL_CHAR,
    /* Cf */ U_FORMAT_CHAR,
    /* Cs */ U_SURROGATE,
    /* Co */ U_PRIVATE_USE_CHAR,
    /* Cn */ U_UNASSIGNED,
    /* Lu */ U_UPPERCASE_LETTER,
    /* Ll */ U_LOWERCASE_LETTER,
    /* Lt */ U_TITLECASE_LETTER,
    /* Lm */ U_MODIFIER_LETTER,
    /* Lo */ U_OTHER_LETTER,
    /* Pc */ U_CONNECTOR_PUNCTUATION,
    /* Pd */ U_DASH_PUNCTUATION,
    /* Ps */ U_START_PUNCTUATION,
    /* Pe */ U_END_PUNCTUATION,
    /* Po */ U_OTHER_PUNCTUATION,
    /* Sm */ U_MATH_SYMBOL,
    /* Sc */ U_CURRENCY_SYMBOL,
    /* Sk */ U_MODIFIER_SYMBOL,
    /* So */ U_OTHER_SYMBOL,
    /* Pi */ U_INITIAL_PUNCTUATION,
    /* Pf */ U_FINAL_PUNCTUATION
    };

const char dirStrings[][5] = {
    "L",
    "R",
    "EN",
    "ES",
    "ET",
    "AN",
    "CS",
    "B",
    "S",
    "WS",
    "ON",
    "LRE",
    "LRO",
    "AL",
    "RLE",
    "RLO",
    "PDF",
    "NSM",
    "BN"
};

void addUnicodeTest(TestNode** root);

void addUnicodeTest(TestNode** root)
{
    addTest(root, &TestUnicodeData, "tsutil/cucdtst/TestUnicodeData");
    addTest(root, &TestCodeUnit, "tsutil/cucdtst/TestCodeUnit");
    addTest(root, &TestCodePoint, "tsutil/cucdtst/TestCodePoint");
    addTest(root, &TestCharLength, "tsutil/cucdtst/TestCharLength");
    addTest(root, &TestAdditionalProperties, "tsutil/cucdtst/TestAdditionalProperties");
    addTest(root, &TestNumericProperties, "tsutil/cucdtst/TestNumericProperties");
    addTest(root, &TestUpperLower, "tsutil/cucdtst/TestUpperLower");
    addTest(root, &TestLetterNumber, "tsutil/cucdtst/TestLetterNumber");
    addTest(root, &TestMisc, "tsutil/cucdtst/TestMisc");
    addTest(root, &TestControlPrint, "tsutil/cucdtst/TestControlPrint");
    addTest(root, &TestIdentifier, "tsutil/cucdtst/TestIdentifier");
    addTest(root, &TestStringCopy, "tsutil/cucdtst/TestStringCopy");
    addTest(root, &TestStringFunctions, "tsutil/cucdtst/TestStringFunctions");
    addTest(root, &TestStringSearching, "tsutil/cucdtst/TestStringSearching");
    addTest(root, &TestCharNames, "tsutil/cucdtst/TestCharNames");
    addTest(root, &TestMirroring, "tsutil/cucdtst/TestMirroring");
    addTest(root, &TestUnescape, "tsutil/cucdtst/TestUnescape");
    addTest(root, &TestCaseLower, "tsutil/cucdtst/TestCaseLower");
    addTest(root, &TestCaseUpper, "tsutil/cucdtst/TestCaseUpper");
    addTest(root, &TestCaseTitle, "tsutil/cucdtst/TestCaseTitle");
    addTest(root, &TestCaseFolding, "tsutil/cucdtst/TestCaseFolding");
    addTest(root, &TestCaseCompare, "tsutil/cucdtst/TestCaseCompare");
    addTest(root, &TestUScriptCodeAPI, "tsutil/cucdtst/TestUScriptCodeAPI");
    addTest(root, &TestUScriptRunAPI, "tsutil/cucdtst/TestUScriptRunAPI");
}

/*==================================================== */
/* test u_toupper() and u_tolower()                    */
/*==================================================== */
static void TestUpperLower()
{
    const UChar upper[] = {0x41, 0x42, 0x00b2, 0x01c4, 0x01c6, 0x01c9, 0x01c8, 0x01c9, 0x000c, 0x0000};
    const UChar lower[] = {0x61, 0x62, 0x00b2, 0x01c6, 0x01c6, 0x01c9, 0x01c9, 0x01c9, 0x000c, 0x0000};
    U_STRING_DECL(upperTest, "abcdefg123hij.?:klmno", 21);
    U_STRING_DECL(lowerTest, "ABCDEFG123HIJ.?:KLMNO", 21);
    int i;

    U_STRING_INIT(upperTest, "abcdefg123hij.?:klmno", 21);
    U_STRING_INIT(lowerTest, "ABCDEFG123HIJ.?:KLMNO", 21);

/*
Checks LetterLike Symbols which were previously a source of confusion
[Bertrand A. D. 02/04/98]
*/
    for (i=0x2100;i<0x2138;i++)
    {
        if(i!=0x2126 && i!=0x212a && i!=0x212b)
        {
            if (i != (int)u_tolower(i)) /* itself */
                log_err("Failed case conversion with itself: \\u%4X\n", i);
            if (i != (int)u_toupper(i))
                log_err("Failed case conversion with itself: \\u%4X\n", i);
        }
    }

    for(i=0; i < u_strlen(upper); i++){
        if(u_tolower(upper[i]) != lower[i]){
            log_err("FAILED u_tolower() for %lx Expected %lx Got %lx\n", upper[i], lower[i], u_tolower(upper[i]));
        }
    }
    log_verbose("testing upper lower\n");
    for (i = 0; i < 21; i++) {

        log_verbose("testing to upper to lower\n");
        if (u_isalpha(upperTest[i]) && !u_islower(upperTest[i]))
        {
            log_err("Failed isLowerCase test at  %c\n", upperTest[i]);
        }
        else if (u_isalpha(lowerTest[i]) && !u_isupper(lowerTest[i]))
         {
            log_err("Failed isUpperCase test at %c\n", lowerTest[i]);
        }
        else if (upperTest[i] != u_tolower(lowerTest[i]))
        {
            log_err("Failed case conversion from %c  To %c :\n", lowerTest[i], upperTest[i]);
        }
        else if (lowerTest[i] != u_toupper(upperTest[i]))
         {
            log_err("Failed case conversion : %c To %c \n", upperTest[i], lowerTest[i]);
        }
        else if (upperTest[i] != u_tolower(upperTest[i]))
        {
            log_err("Failed case conversion with itself: %c\n", upperTest[i]);
        }
        else if (lowerTest[i] != u_toupper(lowerTest[i]))
        {
            log_err("Failed case conversion with itself: %c\n", lowerTest[i]);
        }
    }
    log_verbose("done testing upper Lower\n");

}


/* test isLetter(u_isapha()) and isDigit(u_isdigit()) */
static void TestLetterNumber()
{
    UChar i = 0x0000;

    for (i = 0x0041; i < 0x005B; i++) {
        log_verbose("Testing for isalpha\n");
        if (!u_isalpha(i))
        {
            log_err("Failed isLetter test at  %.4X\n", i);
        }
    }
    for (i = 0x0660; i < 0x066A; i++) {
        log_verbose("Testing for isalpha\n");
        if (u_isalpha(i))
        {
            log_err("Failed isLetter test with numbers at %.4X\n", i);
        }
    }
    for (i = 0x0660; i < 0x066A; i++) {
        log_verbose("Testing for isdigit\n");
        if (!u_isdigit(i))
        {
            log_verbose("Failed isNumber test at %.4X\n", i);
        }
    }
    for (i = 0x0041; i < 0x005B; i++) {
        log_verbose("Testing for isalnum\n");
        if (!u_isalnum(i))
        {
            log_err("Failed isAlNum test at  %.4X\n", i);
        }
    }
    for (i = 0x0660; i < 0x066A; i++) {
        log_verbose("Testing for isalnum\n");
        if (!u_isalnum(i))
        {
            log_err("Failed isAlNum test at  %.4X\n", i);
        }
    }

}

/* Tests for isDefined(u_isdefined)(, isBaseForm(u_isbase()), isSpaceChar(u_isspace()), isWhiteSpace(), u_CharDigitValue(),u_CharCellWidth() */
static void TestMisc()
{
    const UChar sampleSpaces[] = {0x0020, 0x00a0, 0x2000, 0x2001, 0x2005};
    const UChar sampleNonSpaces[] = {0x61, 0x62, 0x63, 0x64, 0x74};
    const UChar sampleUndefined[] = {0xfff1, 0xfff7, 0xfa6b };
    const UChar sampleDefined[] = {0x523E, 0x4f88, 0xfffd};
    const UChar sampleBase[] = {0x0061, 0x0031, 0x03d2};
    const UChar sampleNonBase[] = {0x002B, 0x0020, 0x203B};
    const UChar sampleChars[] = {0x000a, 0x0045, 0x4e00, 0xDC00, 0xFFE8, 0xFFF0};
    const UChar sampleDigits[]= {0x0030, 0x0662, 0x0F23, 0x0ED5};
    const UChar sample2Digits[]= {0x3007, 0x4e00, 0x4e8c, 0x4e09, 0x56db, 0x4e94, 0x516d, 0x4e03, 0x516b, 0x4e5d}; /*sp characters not in the proptable*/
    const UChar sampleNonDigits[] = {0x0010, 0x0041, 0x0122, 0x68FE};
    const UChar sampleWhiteSpaces[] = {0x2008, 0x2009, 0x200a, 0x001c, 0x000c};
    const UChar sampleNonWhiteSpaces[] = {0x61, 0x62, 0x3c, 0x28, 0x3f};


    const int32_t sampleDigitValues[] = {0, 2, 3, 5};
    const int32_t sample2DigitValues[]= {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}; /*special characters not in the properties table*/

    uint32_t mask;

    enum ECellWidths         /* pasted in here from unicode.h */
    {
        ZERO_WIDTH              = 0,
        HALF_WIDTH              = 1,
        FULL_WIDTH              = 2,
        NEUTRAL                 = 3
    };

    const uint16_t sampleCellWidth[] = { ZERO_WIDTH, 
                                         HALF_WIDTH, 
                                         FULL_WIDTH, 
                                         NEUTRAL,
                                         U_HALF_WIDTH,
                                         U_ZERO_WIDTH};
    int i;
    char icuVersion[U_MAX_VERSION_STRING_LENGTH];
    UVersionInfo realVersion;

    memset(icuVersion, 0, U_MAX_VERSION_STRING_LENGTH);
    for (i = 0; i < 5; i++) {
      log_verbose("Testing for isspace and nonspaces\n");
        if (!(u_isspace(sampleSpaces[i])) ||
                (u_isspace(sampleNonSpaces[i])))
        {
            log_err("Space char test error : %d or %d \n", (int32_t)sampleSpaces[i], (int32_t)sampleNonSpaces[i]);
        }
    }
    for (i = 0; i < 5; i++) {
      log_verbose("Testing for isspace and nonspaces\n");
        if (!(u_isWhitespace(sampleWhiteSpaces[i])) ||
                (u_isWhitespace(sampleNonWhiteSpaces[i])))
        {
            log_err("White Space char test error : %lx or %lx \n", sampleWhiteSpaces[i], sampleNonWhiteSpaces[i]);
        }
    }
    for (i = 0; i < 3; i++) {
      log_verbose("Testing for isdefined\n");
        if ((u_isdefined(sampleUndefined[i])) ||
                !(u_isdefined(sampleDefined[i])))
        {
            log_err("Undefined char test error : U+%04x or U+%04x\n", (int32_t)sampleUndefined[i], (int32_t)sampleDefined[i]);
        }
    }
    for (i = 0; i < 3; i++) {
      log_verbose("Testing for isbase\n");
        if ((u_isbase(sampleNonBase[i])) ||
                !(u_isbase(sampleBase[i])))
        {
            log_err("Non-baseform char test error : %d or %d",(int32_t)sampleNonBase[i], (int32_t)sampleBase[i]);
        }
    }
    for (i = 0; i < 5; i++) {
      log_verbose("Testing for charcellwidth\n");
        if (u_charCellWidth(sampleChars[i]) != sampleCellWidth[i])
        {
            log_err("Cell width char test error : %d  \n", (int32_t)sampleChars[i]);
        }
    }
    for (i = 0; i < 4; i++) {
       log_verbose("Testing for isdigit \n");
        if ((u_isdigit(sampleDigits[i]) && 
            (u_charDigitValue(sampleDigits[i])!= sampleDigitValues[i])) ||
            (u_isdigit(sampleNonDigits[i]))) {
            log_err("Digit char test error : %lx   or   %lx\n", sampleDigits[i], sampleNonDigits[i]);
        }
    }
    for (i = 0; i < 10; i++) {
       log_verbose("Testing for u_charDigitValue for special values not existing in prop table %lx \n",  sample2Digits[i]);
        if (u_charDigitValue(sample2Digits[i])!= sample2DigitValues[i]) 
        {
            log_err("Digit char test error : %lx\n", sample2Digits[i]);
        }
    }
    /* Tests the ICU version #*/
    u_getVersion(realVersion);
    u_versionToString(realVersion, icuVersion);
    if (strncmp(icuVersion, U_ICU_VERSION, uprv_min(strlen(icuVersion), strlen(U_ICU_VERSION))) != 0)
    {
        log_err("ICU version test failed. Header says=%s, got=%s \n", U_ICU_VERSION, icuVersion);
    }
#if defined(ICU_VERSION)
    /* test only happens where we have configure.in with VERSION - sanity check. */
    if(strcmp(U_ICU_VERSION, ICU_VERSION))
    {
        log_err("ICU version mismatch: Header says %s, build environment says %s.\n",  U_ICU_VERSION, ICU_VERSION);
    }
#endif

    /* test U_GC_... */
    if(
        U_GET_GC_MASK(0x41)!=U_GC_LU_MASK ||
        U_GET_GC_MASK(0x662)!=U_GC_ND_MASK ||
        U_GET_GC_MASK(0xa0)!=U_GC_ZS_MASK ||
        U_GET_GC_MASK(0x28)!=U_GC_PS_MASK ||
        U_GET_GC_MASK(0x2044)!=U_GC_SM_MASK ||
        U_GET_GC_MASK(0xe0063)!=U_GC_CF_MASK
    ) {
        log_err("error: U_GET_GC_MASK does not work properly\n");
    }

    mask=0;
    mask=(mask&~U_GC_CN_MASK)|U_GC_CN_MASK;

    mask=(mask&~U_GC_LU_MASK)|U_GC_LU_MASK;
    mask=(mask&~U_GC_LL_MASK)|U_GC_LL_MASK;
    mask=(mask&~U_GC_LT_MASK)|U_GC_LT_MASK;
    mask=(mask&~U_GC_LM_MASK)|U_GC_LM_MASK;
    mask=(mask&~U_GC_LO_MASK)|U_GC_LO_MASK;

    mask=(mask&~U_GC_MN_MASK)|U_GC_MN_MASK;
    mask=(mask&~U_GC_ME_MASK)|U_GC_ME_MASK;
    mask=(mask&~U_GC_MC_MASK)|U_GC_MC_MASK;

    mask=(mask&~U_GC_ND_MASK)|U_GC_ND_MASK;
    mask=(mask&~U_GC_NL_MASK)|U_GC_NL_MASK;
    mask=(mask&~U_GC_NO_MASK)|U_GC_NO_MASK;

    mask=(mask&~U_GC_ZS_MASK)|U_GC_ZS_MASK;
    mask=(mask&~U_GC_ZL_MASK)|U_GC_ZL_MASK;
    mask=(mask&~U_GC_ZP_MASK)|U_GC_ZP_MASK;

    mask=(mask&~U_GC_CC_MASK)|U_GC_CC_MASK;
    mask=(mask&~U_GC_CF_MASK)|U_GC_CF_MASK;
    mask=(mask&~U_GC_CO_MASK)|U_GC_CO_MASK;
    mask=(mask&~U_GC_CS_MASK)|U_GC_CS_MASK;

    mask=(mask&~U_GC_PD_MASK)|U_GC_PD_MASK;
    mask=(mask&~U_GC_PS_MASK)|U_GC_PS_MASK;
    mask=(mask&~U_GC_PE_MASK)|U_GC_PE_MASK;
    mask=(mask&~U_GC_PC_MASK)|U_GC_PC_MASK;
    mask=(mask&~U_GC_PO_MASK)|U_GC_PO_MASK;

    mask=(mask&~U_GC_SM_MASK)|U_GC_SM_MASK;
    mask=(mask&~U_GC_SC_MASK)|U_GC_SC_MASK;
    mask=(mask&~U_GC_SK_MASK)|U_GC_SK_MASK;
    mask=(mask&~U_GC_SO_MASK)|U_GC_SO_MASK;

    mask=(mask&~U_GC_PI_MASK)|U_GC_PI_MASK;
    mask=(mask&~U_GC_PF_MASK)|U_GC_PF_MASK;

    if(mask!=(U_CHAR_CATEGORY_COUNT<32 ? U_MASK(U_CHAR_CATEGORY_COUNT)-1: 0xffffffff)) {
        log_err("error: problems with U_GC_XX_MASK constants\n");
    }

    mask=0;
    mask=(mask&~U_GC_C_MASK)|U_GC_C_MASK;
    mask=(mask&~U_GC_L_MASK)|U_GC_L_MASK;
    mask=(mask&~U_GC_M_MASK)|U_GC_M_MASK;
    mask=(mask&~U_GC_N_MASK)|U_GC_N_MASK;
    mask=(mask&~U_GC_Z_MASK)|U_GC_Z_MASK;
    mask=(mask&~U_GC_P_MASK)|U_GC_P_MASK;
    mask=(mask&~U_GC_S_MASK)|U_GC_S_MASK;

    if(mask!=(U_CHAR_CATEGORY_COUNT<32 ? U_MASK(U_CHAR_CATEGORY_COUNT)-1: 0xffffffff)) {
        log_err("error: problems with U_GC_Y_MASK constants\n");
    }
}

/* Tests for isControl(u_iscntrl()) and isPrintable(u_isprint()) */
static void TestControlPrint()
{
    const UChar sampleControl[] = {0x001b, 0x0097, 0x0082};
    const UChar sampleNonControl[] = {0x61, 0x0031, 0x00e2};
    const UChar samplePrintable[] = {0x0042, 0x005f, 0x2014};
    const UChar sampleNonPrintable[] = {0x200c, 0x009f, 0x001b};
    UChar32 c;
    int i;

    for (i = 0; i < 3; i++) {
        log_verbose("Testing for iscontrol\n");
        if (!u_iscntrl(sampleControl[i]))
        {
            log_err("Control char test error : %d should be control but is not\n", (int32_t)sampleControl[i]);
        }
        if (u_iscntrl(sampleNonControl[i]))
        {
            log_err("Control char test error : %d should not be control but is\n", (int32_t)sampleNonControl[i]);
        }
    }
    for (i = 0; i < 3; i++) {
        log_verbose("testing for isprintable\n");
        if (!u_isprint(samplePrintable[i]))
        {
            log_err("Printable char test error : %d should be printable but is not\n", (int32_t)samplePrintable[i]);
        }
        if (u_isprint(sampleNonPrintable[i]))
        {
            log_err("Printable char test error : %d should not be printable but is\n", (int32_t)sampleNonPrintable[i]);
        }
    }

    /* test all ISO 8 controls */
    for(c=0; c<=0x9f; ++c) {
        if(c==0x20) {
            /* skip ASCII graphic characters and continue with DEL */
            c=0x7f;
        }
        if(!u_iscntrl(c)) {
            log_err("error: u_iscntrl(ISO 8 control U+%04x)=FALSE\n", c);
        }
        if(u_isprint(c)) {
            log_err("error: u_isprint(ISO 8 control U+%04x)=TRUE\n", c);
        }
    }

    /* test all Latin-1 graphic characters */
    for(c=0x20; c<=0xff; ++c) {
        if(c==0x7f) {
            c=0xa0;
        }
        if(!u_isprint(c)) {
            log_err("error: u_isprint(Latin-1 graphic character U+%04x)=FALSE\n", c);
        }
    }
}

/* u_isJavaIDStart, u_isJavaIDPart, u_isIDStart(), u_isIDPart(), u_isIDIgnorable()*/
static void TestIdentifier()
{
    const UChar sampleJavaIDStart[] = {0x0071, 0x00e4, 0x005f};
    const UChar sampleNonJavaIDStart[] = {0x0020, 0x2030, 0x0082};
    const UChar sampleJavaIDPart[] = {0x005f, 0x0032, 0x0045};
    const UChar sampleNonJavaIDPart[] = {0x2030, 0x2020, 0x0020};
    const UChar sampleUnicodeIDStart[] = {0x0250, 0x00e2, 0x0061};
    const UChar sampleNonUnicodeIDStart[] = {0x2000, 0x000a, 0x2019};
    const UChar sampleUnicodeIDPart[] = {0x005f, 0x0032, 0x0045};
    const UChar sampleNonUnicodeIDPart[] = {0x2030, 0x00a3, 0x0020};
    const UChar sampleIDIgnore[] = {0x0006, 0x0010, 0x206b};
    const UChar sampleNonIDIgnore[] = {0x0075, 0x00a3, 0x0061};

    int i;
    for (i = 0; i < 3; i++) {
        log_verbose("Testing sampleJavaID start \n");
        if (!(u_isJavaIDStart(sampleJavaIDStart[i])) ||
                (u_isJavaIDStart(sampleNonJavaIDStart[i])))
            log_err("Java ID Start char test error : %lx or %lx\n",
            sampleJavaIDStart[i], sampleNonJavaIDStart[i]);
    }
    for (i = 0; i < 3; i++) {
        log_verbose("Testing sampleJavaID part \n");
        if (!(u_isJavaIDPart(sampleJavaIDPart[i])) ||
                (u_isJavaIDPart(sampleNonJavaIDPart[i])))
            log_err("Java ID Part char test error : %lx or %lx\n",
             sampleJavaIDPart[i], sampleNonJavaIDPart[i]);
    }
    for (i = 0; i < 3; i++) {
        log_verbose("Testing sampleUnicodeID start \n");
        /* T_test_logln_ustr((int32_t)i); */
        if (!(u_isIDStart(sampleUnicodeIDStart[i])) ||
                (u_isIDStart(sampleNonUnicodeIDStart[i])))
        {
            log_err("Unicode ID Start char test error : %lx  or  %lx\n", sampleUnicodeIDStart[i],
                                    sampleNonUnicodeIDStart[i]);
        }
    }
    for (i = 2; i < 3; i++) {   /* nos *** starts with 2 instead of 0, until clarified */
        log_verbose("Testing sample unicode ID part \n");
        /* T_test_logln_ustr((int32_t)i); */
        if (!(u_isIDPart(sampleUnicodeIDPart[i])) ||
                (u_isIDPart(sampleNonUnicodeIDPart[i])))
           {
            log_err("Unicode ID Part char test error : %lx  or  %lx", sampleUnicodeIDPart[i], sampleNonUnicodeIDPart[i]);
            }
    }
    for (i = 0; i < 3; i++) {
        log_verbose("Testing  sampleId ignore\n");
        /*T_test_logln_ustr((int32_t)i); */
        if (!(u_isIDIgnorable(sampleIDIgnore[i])) ||
                (u_isIDIgnorable(sampleNonIDIgnore[i])))
        {
            log_verbose("ID ignorable char test error : %d  or  %d\n", sampleIDIgnore[i], sampleNonIDIgnore[i]);
        }
    }
}

/* for each line of UnicodeData.txt, check some of the properties */
/*
 * ### TODO
 * This test fails incorrectly if the First or Last code point of a repetitive area
 * is overridden, which is allowed and is encouraged for the PUAs.
 * Currently, this means that both area First/Last and override lines are
 * tested against the properties from the API,
 * and the area boundary will not match and cause an error.
 *
 * This function should detect area boundaries and skip them for the test of individual
 * code points' properties.
 * Then it should check that the areas contain all the same properties except where overridden.
 * For this, it would have had to set a flag for which code points were listed explicitly.
 */
static void U_CALLCONV
unicodeDataLineFn(void *context,
                  char *fields[][2], int32_t fieldCount,
                  UErrorCode *pErrorCode)
{
    char buffer[100];
    char *end;
    uint32_t value;
    UChar32 c;
    int32_t i;
    int8_t type;

    /* get the character code, field 0 */
    c=strtoul(fields[0][0], &end, 16);
    if(end<=fields[0][0] || end!=fields[0][1]) {
        log_err("error: syntax error in field 0 at %s\n", fields[0][0]);
        return;
    }
    if((uint32_t)c>=UCHAR_MAX_VALUE + 1) {
        log_err("error in UnicodeData.txt: code point %lu out of range\n", c);
        return;
    }

    /* get general category, field 2 */
    *fields[2][1]=0;
    type = (int8_t)tagValues[MakeProp(fields[2][0])];
    if(u_charType(c)!=type) {
        log_err("error: u_charType(U+%04lx)==%u instead of %u\n", c, u_charType(c), type);
    }

    /* get canonical combining class, field 3 */
    value=strtoul(fields[3][0], &end, 10);
    if(end<=fields[3][0] || end!=fields[3][1]) {
        log_err("error: syntax error in field 3 at code 0x%lx\n", c);
        return;
    }
    if(value>255) {
        log_err("error in UnicodeData.txt: combining class %lu out of range\n", value);
        return;
    }
    if(value!=u_getCombiningClass(c) || value!=(uint32_t)u_getIntPropertyValue(c, UCHAR_CANONICAL_COMBINING_CLASS)) {
        log_err("error: u_getCombiningClass(U+%04lx)==%hu instead of %lu\n", c, u_getCombiningClass(c), value);
    }

    /* get BiDi category, field 4 */
    *fields[4][1]=0;
    i=MakeDir(fields[4][0]);
    if(i!=u_charDirection(c) || i!=u_getIntPropertyValue(c, UCHAR_BIDI_CLASS)) {
        log_err("error: u_charDirection(U+%04lx)==%u instead of %u (%s)\n", c, u_charDirection(c), MakeDir(fields[4][0]), fields[4][0]);
    }

    /* get ISO Comment, field 11 */
    *fields[11][1]=0;
    i=u_getISOComment(c, buffer, sizeof(buffer), pErrorCode);
    if(U_FAILURE(*pErrorCode) || 0!=strcmp(fields[11][0], buffer)) {
        log_err("error: u_getISOComment(U+%04lx) wrong (%s): \"%s\" should be \"%s\"\n",
            c, u_errorName(*pErrorCode),
            U_FAILURE(*pErrorCode) ? buffer : "[error]",
            fields[11][0]);
    }

    /* get uppercase mapping, field 12 */
    if(fields[12][0]!=fields[12][1]) {
        value=strtoul(fields[12][0], &end, 16);
        if(end!=fields[12][1]) {
            log_err("error: syntax error in field 12 at code 0x%lx\n", c);
            return;
        }
        if((UChar32)value!=u_toupper(c)) {
            log_err("error: u_toupper(U+%04lx)==U+%04lx instead of U+%04lx\n", c, u_toupper(c), value);
        }
    } else {
        /* no case mapping: the API must map the code point to itself */
        if(c!=u_toupper(c)) {
            log_err("error: U+%04lx does not have an uppercase mapping but u_toupper()==U+%04lx\n", c, u_toupper(c));
        }
    }

    /* get lowercase mapping, field 13 */
    if(fields[13][0]!=fields[13][1]) {
        value=strtoul(fields[13][0], &end, 16);
        if(end!=fields[13][1]) {
            log_err("error: syntax error in field 13 at code 0x%lx\n", c);
            return;
        }
        if((UChar32)value!=u_tolower(c)) {
            log_err("error: u_tolower(U+%04lx)==U+%04lx instead of U+%04lx\n", c, u_tolower(c), value);
        }
    } else {
        /* no case mapping: the API must map the code point to itself */
        if(c!=u_tolower(c)) {
            log_err("error: U+%04lx does not have a lowercase mapping but u_tolower()==U+%04lx\n", c, u_tolower(c));
        }
    }

    /* get titlecase mapping, field 14 */
    if(fields[14][0]!=fields[14][1]) {
        value=strtoul(fields[14][0], &end, 16);
        if(end!=fields[14][1]) {
            log_err("error: syntax error in field 14 at code 0x%lx\n", c);
            return;
        }
        if((UChar32)value!=u_totitle(c)) {
            log_err("error: u_totitle(U+%04lx)==U+%04lx instead of U+%04lx\n", c, u_totitle(c), value);
        }
    } else {
        /* no case mapping: the API must map the code point to itself */
        if(c!=u_totitle(c)) {
            log_err("error: U+%04lx does not have a titlecase mapping but u_totitle()==U+%04lx\n", c, u_totitle(c));
        }
    }
}

static UBool U_CALLCONV
enumTypeRange(const void *context, UChar32 start, UChar32 limit, UCharCategory type) {
    static const UChar32 test[][2]={
        {0x41, U_UPPERCASE_LETTER},
        {0x308, U_NON_SPACING_MARK},
        {0xfffe, U_GENERAL_OTHER_TYPES},
        {0xe0041, U_FORMAT_CHAR},
        {0xeffff, U_UNASSIGNED}
    };

    /* default Bidi classes for unassigned code points */
    static const int32_t defaultBidi[][2]={ /* { limit, class } */
        { 0x0590, U_LEFT_TO_RIGHT },
        { 0x0600, U_RIGHT_TO_LEFT },
        { 0x07C0, U_RIGHT_TO_LEFT_ARABIC },
        { 0xFB1D, U_LEFT_TO_RIGHT },
        { 0xFB50, U_RIGHT_TO_LEFT },
        { 0xFE00, U_RIGHT_TO_LEFT_ARABIC },
        { 0xFE70, U_LEFT_TO_RIGHT },
        { 0xFF00, U_RIGHT_TO_LEFT_ARABIC },
        { 0x110000, U_LEFT_TO_RIGHT }
    };

    UChar32 c;
    int i, count;

    if(0!=strcmp((const char *)context, "a1")) {
        log_err("error: u_enumCharTypes() passes on an incorrect context pointer\n");
        return FALSE;
    }

    count=sizeof(test)/sizeof(test[0]);
    for(i=0; i<count; ++i) {
        if(start<=test[i][0] && test[i][0]<limit) {
            if(type!=(UCharCategory)test[i][1]) {
                log_err("error: u_enumCharTypes() has range [U+%04lx, U+%04lx[ with %ld instead of U+%04lx with %ld\n",
                        start, limit, (long)type, test[i][0], test[i][1]);
            }
            /* stop at the range that includes the last test code point */
            return i==(count-1) ? FALSE : TRUE;
        }
    }

    if(start>test[count-1][0]) {
        log_err("error: u_enumCharTypes() has range [U+%04lx, U+%04lx[ with %ld after it should have stopped\n",
                start, limit, (long)type);
        return FALSE;
    }

    /*
     * LineBreak.txt specifies:
     *   #  - Assigned characters that are not listed explicitly are given the value
     *   #    "AL".
     *   #  - Unassigned characters are given the value "XX".
     *
     * PUA characters are listed explicitly with "XX".
     * Verify that no assigned character has "XX".
     */
    if(type!=U_UNASSIGNED && type!=U_PRIVATE_USE_CHAR) {
        c=start;
        while(c<limit) {
            if(0==u_getIntPropertyValue(c, UCHAR_LINE_BREAK)) {
                log_err("error UCHAR_LINE_BREAK(assigned U+%04lx)=XX\n", c);
            }
            ++c;
        }
    }

    /*
     * Verify default Bidi classes.
     * See table 3-7 "Bidirectional Character Types" in UAX #9.
     * http://www.unicode.org/reports/tr9/
     */
    if(type==U_UNASSIGNED || type==U_PRIVATE_USE_CHAR) {
        /* enumerate the intersections of defaultBidi ranges with [start..limit[ */
        c=start;
        for(i=0; i<LENGTHOF(defaultBidi) && c<limit; ++i) {
            if((int32_t)c<defaultBidi[i][0]) {
                while(c<limit && (int32_t)c<defaultBidi[i][0]) {
                    if( u_charDirection(c)!=(UCharDirection)defaultBidi[i][1] ||
                        u_getIntPropertyValue(c, UCHAR_BIDI_CLASS)!=defaultBidi[i][1]
                    ) {
                        log_err("error: u_charDirection(unassigned/PUA U+%04lx)=%s should be %s\n",
                            c, dirStrings[u_charDirection(c)], dirStrings[defaultBidi[i][1]]);
                    }
                    ++c;
                }
            }
        }
    }

    return TRUE;
}

/* tests for several properties */
static void TestUnicodeData()
{
    char newPath[256];
    char backupPath[256];
    UVersionInfo expectVersionArray;
    UVersionInfo versionArray;
    char *fields[15][2];
    UErrorCode errorCode;
    UChar32 c;
    int8_t type;

    /* Look inside ICU_DATA first */
    strcpy(newPath, u_getDataDirectory());
    strcat(newPath, ".." U_FILE_SEP_STRING "unidata" U_FILE_SEP_STRING "UnicodeData.txt");

    /* As a fallback, try to guess where the source data was located
     *    at the time ICU was built, and look there.
     */
#if defined (U_TOPSRCDIR)
    strcpy(backupPath, U_TOPSRCDIR  U_FILE_SEP_STRING "data");
#else
    strcpy(backupPath, __FILE__);
    strrchr(backupPath, U_FILE_SEP_CHAR)[0] = 0; /* Remove the file name */
    strrchr(backupPath, U_FILE_SEP_CHAR)[0] = 0; /* Previous directory */
    strrchr(backupPath, U_FILE_SEP_CHAR)[0] = 0; /* Previous directory */
    strcat(backupPath, U_FILE_SEP_STRING "data");
#endif
    strcat(backupPath, U_FILE_SEP_STRING);
    strcat(backupPath, "unidata" U_FILE_SEP_STRING "UnicodeData.txt");

    u_versionFromString(expectVersionArray, U_UNICODE_VERSION);
    u_getUnicodeVersion(versionArray);
    if(memcmp(versionArray, expectVersionArray, U_MAX_VERSION_LENGTH) != 0)
    {
        log_err("Testing u_getUnicodeVersion() - expected " U_UNICODE_VERSION " got %d.%d.%d.%d\n",
        versionArray[0], versionArray[1], versionArray[2], versionArray[3]);
    }

#if defined(ICU_UNICODE_VERSION)
    /* test only happens where we have configure.in with UNICODE_VERSION - sanity check. */
    if(strcmp(U_UNICODE_VERSION, ICU_UNICODE_VERSION))
    {
         log_err("Testing configure.in's ICU_UNICODE_VERSION - expected " U_UNICODE_VERSION " got " ICU_UNICODE_VERSION "\n");
    }
#endif

    if (ublock_getCode((UChar)0x0041) != UBLOCK_BASIC_LATIN || u_getIntPropertyValue(0x41, UCHAR_BLOCK)!=(int32_t)UBLOCK_BASIC_LATIN) {
        log_err("ublock_getCode(U+0041) property failed! Expected : %i Got: %i \n", UBLOCK_BASIC_LATIN,ublock_getCode((UChar)0x0041));
    }

    errorCode=U_ZERO_ERROR;
    u_parseDelimitedFile(newPath, ';', fields, 15, unicodeDataLineFn, NULL, &errorCode);
    if(errorCode==U_FILE_ACCESS_ERROR) {
        errorCode=U_ZERO_ERROR;
        u_parseDelimitedFile(backupPath, ';', fields, 15, unicodeDataLineFn, NULL, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        log_err("error parsing UnicodeData.txt: %s\n", u_errorName(errorCode));
    }

    /* sanity check on repeated properties */
    for(c=0xfffe; c<=0x10ffff;) {
        if(u_charType(c)!=U_UNASSIGNED) {
            log_err("error: u_charType(U+%04lx)!=U_UNASSIGNED (returns %d)\n", c, u_charType(c));
        }
        if((c&0xffff)==0xfffe) {
            ++c;
        } else {
            c+=0xffff;
        }
    }

    /* test that PUA is not "unassigned" */
    for(c=0xe000; c<=0x10fffd;) {
        type=u_charType(c);
        if(type==U_UNASSIGNED) {
            log_err("error: u_charType(U+%04lx)==U_UNASSIGNED\n", c);
        } else if(type!=U_PRIVATE_USE_CHAR) {
            log_verbose("PUA override: u_charType(U+%04lx)=%d\n", c, type);
        }
        if(c==0xf8ff) {
            c=0xf0000;
        } else if(c==0xffffd) {
            c=0x100000;
        } else {
            ++c;
        }
    }

    /* test u_enumCharTypes() */
    u_enumCharTypes(enumTypeRange, "a1");
}

static void TestCodeUnit(){
    const UChar codeunit[]={0x0000,0xe065,0x20ac,0xd7ff,0xd800,0xd841,0xd905,0xdbff,0xdc00,0xdc02,0xddee,0xdfff,0};

    int32_t i;

    for(i=0; i<(int32_t)(sizeof(codeunit)/sizeof(codeunit[0])); i++){
        UChar c=codeunit[i];
        log_verbose("Testing code unit value of \\u%4X\n", c);
        if(i<4){
            if(!(UTF_IS_SINGLE(c)) || (UTF_IS_LEAD(c)) || (UTF_IS_TRAIL(c)) ||(UTF_IS_SURROGATE(c))){
                log_err("ERROR: \\u%4X is a single", c);
            }

        }
        if(i >= 4 && i< 8){
            if(!(UTF_IS_LEAD(c)) || UTF_IS_SINGLE(c) || UTF_IS_TRAIL(c) || !(UTF_IS_SURROGATE(c))){
                log_err("ERROR: \\u%4X is a first surrogate", c);
            }
        }
        if(i >= 8 && i< 12){
            if(!(UTF_IS_TRAIL(c)) || UTF_IS_SINGLE(c) || UTF_IS_LEAD(c) || !(UTF_IS_SURROGATE(c))){
                log_err("ERROR: \\u%4X is a second surrogate", c);
            }
        }
    }

}

static void TestCodePoint(){
    const UChar32 codePoint[]={
        /*surrogate, notvalid(codepoint), not a UnicodeChar, not Error */
        0xd800,
        0xdbff,
        0xdc00,
        0xdfff,
        0xdc04,
        0xd821,
        /*not a surrogate, valid, isUnicodeChar , not Error*/
        0x20ac,
        0xd7ff,
        0xe000,
        0xe123,
        0x0061,
        0xe065, 
        0x20402,
        0x24506,
        0x23456,
        0x20402,
        0x10402,
        0x23456,
        /*not a surrogate, not valid, isUnicodeChar, isError */
        0x0015,
        0x009f,
        /*not a surrogate, not valid, not isUnicodeChar, isError */
        0xffff,
        0xfffe,
    };
    int32_t i;
    for(i=0; i<(int32_t)(sizeof(codePoint)/sizeof(codePoint[0])); i++){
        UChar32 c=codePoint[i];
        log_verbose("Testing code unit value of \\u%4X\n", c);
        if(i<6){
            if(!UTF_IS_SURROGATE(c)){
                log_err("ERROR: isSurrogate() failed for \\u%4X\n", c);
            }
            if(UTF_IS_VALID(c)){
                log_err("ERROR: isValid() failed for \\u%4X\n", c);
            }
            if(UTF_IS_UNICODE_CHAR(c)){
                log_err("ERROR: isUnicodeChar() failed for \\u%4X\n", c);
            }
            if(UTF_IS_ERROR(c)){
                log_err("ERROR: isError() failed for \\u%4X\n", c);
            }
        }else if(i >=6 && i<18){
            if(UTF_IS_SURROGATE(c)){
                log_err("ERROR: isSurrogate() failed for \\u%4X\n", c);
            }
            if(!UTF_IS_VALID(c)){
                log_err("ERROR: isValid() failed for \\u%4X\n", c);
            }
            if(!UTF_IS_UNICODE_CHAR(c)){
                log_err("ERROR: isUnicodeChar() failed for \\u%4X\n", c);
            }
            if(UTF_IS_ERROR(c)){
                log_err("ERROR: isError() failed for \\u%4X\n", c);
            }
        }else if(i >=18 && i<20){
            if(UTF_IS_SURROGATE(c)){
                log_err("ERROR: isSurrogate() failed for \\u%4X\n", c);
            }
            if(UTF_IS_VALID(c)){
                log_err("ERROR: isValid() failed for \\u%4X\n", c);
            }
            if(!UTF_IS_UNICODE_CHAR(c)){
                log_err("ERROR: isUnicodeChar() failed for \\u%4X\n", c);
            }
            if(!UTF_IS_ERROR(c)){
                log_err("ERROR: isError() failed for \\u%4X\n", c);
            }
        }
        else if(i >=18 && i<(int32_t)(sizeof(codePoint)/sizeof(codePoint[0]))){
            if(UTF_IS_SURROGATE(c)){
                log_err("ERROR: isSurrogate() failed for \\u%4X\n", c);
            }
            if(UTF_IS_VALID(c)){
                log_err("ERROR: isValid() failed for \\u%4X\n", c);
            }
            if(UTF_IS_UNICODE_CHAR(c)){
                log_err("ERROR: isUnicodeChar() failed for \\u%4X\n", c);
            }
            if(!UTF_IS_ERROR(c)){
                log_err("ERROR: isError() failed for \\u%4X\n", c);
            }
        }
    }

}

static void TestCharLength()
{
    const int32_t codepoint[]={
        1, 0x0061,
        1, 0xe065,
        1, 0x20ac,
        2, 0x20402,
        2, 0x23456,
        2, 0x24506,
        2, 0x20402,
        2, 0x10402,
        1, 0xd7ff,
        1, 0xe000
    };

    int32_t i;
    UBool multiple;
    for(i=0; i<(int32_t)(sizeof(codepoint)/sizeof(codepoint[0])); i=(int16_t)(i+2)){
        UChar32 c=codepoint[i+1];
        if(UTF_CHAR_LENGTH(c) != codepoint[i]){
            log_err("The no: of code units for \\u%4X:- Expected: %d Got: %d", c, codepoint[i], UTF_CHAR_LENGTH(c));
        }else{
            log_verbose("The no: of code units for \\u%4X is %d", c, UTF_CHAR_LENGTH(c));
        }
        multiple=(UBool)(codepoint[i] == 1 ? FALSE : TRUE);
        if(UTF_NEED_MULTIPLE_UCHAR(c) != multiple){
            log_err("ERROR: Unicode::needMultipleUChar() failed for \\u%4X\n", c);
        }
    }
}

/*internal functions ----*/
static int32_t MakeProp(char* str) 
{
    int32_t result = 0;
    char* matchPosition =0;

    matchPosition = strstr(tagStrings, str);
    if (matchPosition == 0) 
    {
        log_err("unrecognized type letter ");
        log_err(str);
    }
    else result = ((matchPosition - tagStrings) / 2);
    return result;
}

static int32_t MakeDir(char* str) 
{
    int32_t pos = 0;
    for (pos = 0; pos < 19; pos++) {
        if (strcmp(str, dirStrings[pos]) == 0) {
            return pos;
        }
    }
    return -1;
}
/*----------------*/


static const char* raw[3][4] = {

    /* First String */
    {   "English_",  "French_",   "Croatian_", "English_"},
    /* Second String */
    {   "United States",    "France",   "Croatia",  "Unites States"},

   /* Concatenated string */
    {   "English_United States", "French_France", "Croatian_Croatia", "English_United States"}
};

static void setUpDataTable()
{
    int32_t i,j;
    if(dataTable == NULL) {
        dataTable = (UChar***)calloc(sizeof(UChar**),3);

            for (i = 0; i < 3; i++) {
              dataTable[i] = (UChar**)calloc(sizeof(UChar*),4);
                for (j = 0; j < 4; j++){
                    dataTable[i][j] = (UChar*) malloc(sizeof(UChar)*(strlen(raw[i][j])+1));
                    u_uastrcpy(dataTable[i][j],raw[i][j]);
                }
            }
    }
}

static void cleanUpDataTable()
{
    int32_t i,j;
    if(dataTable != NULL) {
        for (i=0; i<3; i++) {
            for(j = 0; j<4; j++) {
                free(dataTable[i][j]);
            }
            free(dataTable[i]);
        }
        free(dataTable);
    }
    dataTable = NULL;
}

/*Tests  for u_strcat(),u_strcmp(), u_strlen(), u_strcpy(),u_strncat(),u_strncmp(),u_strncpy, u_uastrcpy(),u_austrcpy(), u_uastrncpy(); */
static void TestStringFunctions()
{
    int32_t i,j,k;
    UChar temp[512];
    UChar nullTemp[512];
    char test[512];
    char tempOut[512];

    setUpDataTable();

    log_verbose("Testing u_strlen()\n");
    if( u_strlen(dataTable[0][0])!= u_strlen(dataTable[0][3]) || u_strlen(dataTable[0][0]) == u_strlen(dataTable[0][2]))
        log_err("There is an error in u_strlen()");

    log_verbose("Testing u_memcpy() and u_memcmp()\n");

    for(i=0;i<3;++i)
    {
        for(j=0;j<4;++j)
        {
            log_verbose("Testing  %s\n", u_austrcpy(tempOut, dataTable[i][j]));
            temp[0] = 0;
            temp[7] = 0xA4; /* Mark the end */
            u_memcpy(temp,dataTable[i][j], 7);

            if(temp[7] != 0xA4)
                log_err("an error occured in u_memcpy()\n");
            if(u_memcmp(temp, dataTable[i][j], 7)!=0)
                log_err("an error occured in u_memcpy() or u_memcmp()\n");
        }
    }
    if(u_memcmp(dataTable[0][0], dataTable[1][1], 7)==0)
        log_err("an error occured in u_memcmp()\n");

    log_verbose("Testing u_memset()\n");
    nullTemp[0] = 0;
    nullTemp[7] = 0;
    u_memset(nullTemp, 0xa4, 7);
    for (i = 0; i < 7; i++) {
        if(nullTemp[i] != 0xa4) {
            log_err("an error occured in u_memset()\n");
        }
    }
    if(nullTemp[7] != 0) {
        log_err("u_memset() went too far\n");
    }

    u_memset(nullTemp, 0, 7);
    nullTemp[7] = 0xa4;
    temp[7] = 0;
    u_memcpy(temp,nullTemp, 7);
    if(u_memcmp(temp, nullTemp, 7)!=0 || temp[7]!=0)
        log_err("an error occured in u_memcpy() or u_memcmp()\n");


    log_verbose("Testing u_memmove()\n");
    for (i = 0; i < 7; i++) {
        temp[i] = (UChar)i;
    }
    u_memmove(temp + 1, temp, 7);
    if(temp[0] != 0) {
        log_err("an error occured in u_memmove()\n");
    }
    for (i = 1; i <= 7; i++) {
        if(temp[i] != (i - 1)) {
            log_err("an error occured in u_memmove()\n");
        }
    }

    log_verbose("Testing u_strcpy() and u_strcmp()\n");

    for(i=0;i<3;++i)
    {
        for(j=0;j<4;++j)
        {
            log_verbose("Testing  %s\n", u_austrcpy(tempOut, dataTable[i][j]));
            temp[0] = 0;
            u_strcpy(temp,dataTable[i][j]);

            if(u_strcmp(temp,dataTable[i][j])!=0)
                log_err("something threw an error in u_strcpy() or u_strcmp()\n");
        }
    }
    if(u_strcmp(dataTable[0][0], dataTable[1][1])==0)
        log_err("an error occured in u_memcmp()\n");

    log_verbose("testing u_strcat()\n");
    i=0;
    for(j=0; j<2;++j)
    {
        u_uastrcpy(temp, "");
        u_strcpy(temp,dataTable[i][j]);
        u_strcat(temp,dataTable[i+1][j]);
        if(u_strcmp(temp,dataTable[i+2][j])!=0)
            log_err("something threw an error in u_strcat()\n");

    }
    log_verbose("Testing u_strncmp()\n");
    for(i=0,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);
        if(u_strncmp(dataTable[i][j],dataTable[i+2][j],k)!=0)
            log_err("Something threw an error in u_strncmp\n");
    }
    if(u_strncmp(dataTable[0][0], dataTable[1][1], 7)==0)
        log_err("an error occured in u_memcmp()\n");


    log_verbose("Testing u_strncat\n");
    for(i=0,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);

        u_uastrcpy(temp,"");

        if(u_strcmp(u_strncat(temp,dataTable[i+2][j],k),dataTable[i][j])!=0)
            log_err("something threw an error in u_strncat or u_uastrcpy()\n");

    }

    log_verbose("Testing u_strncpy() and u_uastrcpy()\n");
    for(i=2,j=0;j<4; ++j)
    {
        k=u_strlen(dataTable[i][j]);
        u_strncpy(temp, dataTable[i][j],k);
        temp[k] = 0xa4;

        if(u_strncmp(temp, dataTable[i][j],k)!=0)
            log_err("something threw an error in u_strncpy()\n");

        if(temp[k] != 0xa4)
            log_err("something threw an error in u_strncpy()\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k-1);
        if(u_strncmp(temp, dataTable[i][j],k-1)!=0)
            log_err("something threw an error in u_uastrncpy(k-1)\n");

        if(temp[k-1] != 0x3F)
            log_err("something threw an error in u_uastrncpy(k-1)\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k+1);
        if(u_strcmp(temp, dataTable[i][j])!=0)
            log_err("something threw an error in u_uastrncpy(k+1)\n");

        if(temp[k] != 0)
            log_err("something threw an error in u_uastrncpy(k+1)\n");

        u_memset(temp, 0x3F, (sizeof(temp) / sizeof(UChar)) - 1);
        u_uastrncpy(temp, raw[i][j], k);
        if(u_strncmp(temp, dataTable[i][j], k)!=0)
            log_err("something threw an error in u_uastrncpy(k)\n");

        if(temp[k] != 0x3F)
            log_err("something threw an error in u_uastrncpy(k)\n");
    }

    log_verbose("Testing u_strchr() and u_memchr()\n");

    for(i=2,j=0;j<4;j++)
    {
        UChar saveVal = dataTable[i][j][0];
        UChar *findPtr = u_strchr(dataTable[i][j], 0x005F);
        int32_t dataSize = (int32_t)(u_strlen(dataTable[i][j]) + 1);

        log_verbose("%s ", u_austrcpy(tempOut, findPtr));

        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_strchr can't find '_' in the string\n");
        }

        findPtr = u_strchr32(dataTable[i][j], 0x005F);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_strchr32 can't find '_' in the string\n");
        }

        findPtr = u_strchr(dataTable[i][j], 0);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_strchr can't find NULL in the string\n");
        }

        findPtr = u_strchr32(dataTable[i][j], 0);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_strchr32 can't find NULL in the string\n");
        }

        findPtr = u_memchr(dataTable[i][j], 0, dataSize);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_memchr can't find NULL in the string\n");
        }

        findPtr = u_memchr32(dataTable[i][j], 0, dataSize);
        if (findPtr != (&(dataTable[i][j][dataSize - 1]))) {
            log_err("u_memchr32 can't find NULL in the string\n");
        }

        dataTable[i][j][0] = 0;
        /* Make sure we skip over the NULL termination */
        findPtr = u_memchr(dataTable[i][j], 0x005F, dataSize);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_memchr can't find '_' in the string\n");
        }

        findPtr = u_memchr32(dataTable[i][j], 0x005F, dataSize);
        if (findPtr == NULL || *findPtr != 0x005F) {
            log_err("u_memchr32 can't find '_' in the string\n");
        }
        findPtr = u_memchr32(dataTable[i][j], 0xFFFD, dataSize);
        if (findPtr != NULL) {
            log_err("Should have found NULL when the character is not there.\n");
        }
        dataTable[i][j][0] = saveVal;   /* Put it back for the other tests */
    }

    /*
     * test that u_strchr32()
     * does not find surrogate code points when they are part of matched pairs
     * (= part of supplementary code points)
     * Jitterbug 1542
     */
    {
        static const UChar s[]={
            /*   0       1       2       3       4       5       6       7       8  9 */
            0x0061, 0xd841, 0xdc02, 0xd841, 0x0062, 0xdc02, 0xd841, 0xdc02, 0x0063, 0
        };

        if(u_strchr32(s, 0xd841)!=(s+3) || u_strchr32(s, 0xdc02)!=(s+5)) {
            log_err("error: u_strchr32(surrogate) finds a partial supplementary code point\n");
        }
        if(u_memchr32(s, 0xd841, 9)!=(s+3) || u_memchr32(s, 0xdc02, 9)!=(s+5)) {
            log_err("error: u_memchr32(surrogate) finds a partial supplementary code point\n");
        }
    }

    log_verbose("Testing u_austrcpy()");
    u_austrcpy(test,dataTable[0][0]);
    if(strcmp(test,raw[0][0])!=0)
        log_err("There is an error in u_austrcpy()");


    log_verbose("Testing u_strtok_r()");
    {
        const char tokString[] = "  ,  1 2 3  AHHHHH! 5.5 6 7    ,        8\n";
        const char *tokens[] = {",", "1", "2", "3", "AHHHHH!", "5.5", "6", "7", "8\n"};
        UChar delimBuf[sizeof(test)];
        UChar currTokenBuf[sizeof(tokString)];
        UChar *state;
        uint32_t currToken = 0;
        UChar *ptr;

        u_uastrcpy(temp, tokString);
        u_uastrcpy(delimBuf, " ");

        ptr = u_strtok_r(temp, delimBuf, &state);
        u_uastrcpy(delimBuf, " ,");
        while (ptr != NULL) {
            u_uastrcpy(currTokenBuf, tokens[currToken]);
            if (u_strcmp(ptr, currTokenBuf) != 0) {
                log_err("u_strtok_r mismatch at %d. Got: %s, Expected: %s\n", currToken, ptr, tokens[currToken]);
            }
            ptr = u_strtok_r(NULL, delimBuf, &state);
            currToken++;
        }

        if (currToken != sizeof(tokens)/sizeof(tokens[0])) {
            log_err("Didn't get correct number of tokens\n");
        }
        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, "");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for empty string\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for empty string\n");
        }
        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, ", ,");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for a string of delimiters\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for a string of delimiters\n");
        }

        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, "q, ,");
        if (u_strtok_r(currTokenBuf, delimBuf, &state) == NULL) {
            log_err("Got NULL for a string that does not begin with delimiters\n");
        }
        if (u_strtok_r(NULL, delimBuf, &state) != NULL) {
            log_err("Didn't get NULL for a string that ends in delimiters\n");
        }
        if (state != NULL) {
            log_err("State should be NULL for empty string\n");
        }

        state = delimBuf;       /* Give it an "invalid" saveState */
        u_uastrcpy(currTokenBuf, tokString);
        u_uastrcpy(temp, tokString);
        u_uastrcpy(delimBuf, "q");  /* Give it a delimiter that it can't find. */
        ptr = u_strtok_r(currTokenBuf, delimBuf, &state);
        if (ptr == NULL || u_strcmp(ptr, temp) != 0) {
            log_err("Should have recieved the same string when there are no delimiters\n");
        }
        if (u_strtok_r(NULL, delimBuf, &state) != NULL) {
            log_err("Should not have found another token in a one token string\n");
        }
    }

    /* test u_strcmpCodePointOrder() */
    {
        /* these strings are in ascending order */
        static const UChar strings[][4]={
            { 0x61, 0 },                    /* U+0061 */
            { 0x20ac, 0xd801, 0 },          /* U+20ac U+d801 */
            { 0x20ac, 0xd800, 0xdc00, 0 },  /* U+20ac U+10000 */
            { 0xd800, 0 },                  /* U+d800 */
            { 0xd800, 0xff61, 0 },          /* U+d800 U+ff61 */
            { 0xdfff, 0 },                  /* U+dfff */
            { 0xff61, 0xdfff, 0 },          /* U+ff61 U+dfff */
            { 0xff61, 0xd800, 0xdc02, 0 },  /* U+ff61 U+10002 */
            { 0xd800, 0xdc02, 0 },          /* U+10002 */
            { 0xd84d, 0xdc56, 0 }           /* U+23456 */
        };
        int32_t len1, len2;

        for(i=0; i<(sizeof(strings)/sizeof(strings[0])-1); ++i) {
            if(u_strcmpCodePointOrder(strings[i], strings[i+1])>=0) {
                log_err("error: u_strcmpCodePointOrder() fails for string %d and the following one\n", i);
            }

            /* test u_strCompare(TRUE) */
            len1=u_strlen(strings[i]);
            len2=u_strlen(strings[i+1]);
            if( u_strCompare(strings[i], -1, strings[i+1], -1, TRUE)>=0 ||
                u_strCompare(strings[i], -1, strings[i+1], len2, TRUE)>=0 ||
                u_strCompare(strings[i], len1, strings[i+1], -1, TRUE)>=0 ||
                u_strCompare(strings[i], len1, strings[i+1], len2, TRUE)>=0
            ) {
                log_err("error: u_strCompare(code point order) fails for string %d and the following one\n", i);
            }

            /* test u_strCompare(FALSE) */
            if(_SIGN(u_strCompare(strings[i], -1, strings[i+1], -1, FALSE))!=_SIGN(u_strcmp(strings[i], strings[i+1]))) {
                log_err("error: u_strCompare(code unit order)!=u_strcmp() for string %d and the following one\n", i);
            }
        }
    }

    cleanUpDataTable();
}

static void TestStringSearching()
{
    UChar ucharBuf[255];
    const UChar testString[] = {0x0061, 0x0062, 0x0063, 0x0064, 0x0064, 0x0061, 0};
    const UChar testSurrogateString[] = {0xdbff, 0x0061, 0x0062, 0xdbff, 0xdfff, 0x0063, 0x0064, 0x0064, 0xdbff, 0xdfff, 0xdb00, 0xdf00, 0x0061, 0};
    const UChar surrMatchSet1[] = {0xdbff, 0xdfff, 0};
    const UChar surrMatchSet2[] = {0x0061, 0x0062, 0xdbff, 0xdfff, 0};
    const UChar surrMatchSet3[] = {0xdb00, 0xdf00, 0xdbff, 0xdfff, 0};
    const UChar surrMatchSet4[] = {0x0000};
    const UChar surrMatchSetBad[] = {0xdbff, 0x0061, 0};
    const UChar surrMatchSetBad2[] = {0x0061, 0xdbff, 0};
    const UChar surrMatchSetBad3[] = {0xdbff, 0x0061, 0x0062, 0xdbff, 0xdfff, 0};   /* has partial surrogate */

    log_verbose("Testing u_strpbrk()");

    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "a")) != &testString[0]) {
        log_err("u_strpbrk couldn't find first letter a.\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "dc")) != &testString[2]) {
        log_err("u_strpbrk couldn't find d or c.\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "cd")) != &testString[2]) {
        log_err("u_strpbrk couldn't find c or d.\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "cdh")) != &testString[2]) {
        log_err("u_strpbrk couldn't find c, d or h.\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "f")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"f\".\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "fg")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"fg\".\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "gf")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"gf\".\n");
    }
    if (u_strpbrk(testString, u_uastrcpy(ucharBuf, "")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"\".\n");
    }

    log_verbose("Testing u_strpbrk() with surrogates");

    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "a")) != &testSurrogateString[1]) {
        log_err("u_strpbrk couldn't find first letter a.\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "dc")) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find d or c.\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "cd")) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find c or d.\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "cdh")) != &testSurrogateString[5]) {
        log_err("u_strpbrk couldn't find c, d or h.\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "f")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"f\".\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "fg")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"fg\".\n");
    }
    if (u_strpbrk(testSurrogateString, u_uastrcpy(ucharBuf, "gf")) != NULL) {
        log_err("u_strpbrk didn't return NULL for \"gf\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet1) != &testSurrogateString[3]) {
        log_err("u_strpbrk couldn't find \"0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet2) != &testSurrogateString[1]) {
        log_err("u_strpbrk couldn't find \"0xdbff, a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet3) != &testSurrogateString[3]) {
        log_err("u_strpbrk couldn't find \"0xdb00, 0xdf00, 0xdbff, 0xdfff\".\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSet4) != NULL) {
        log_err("u_strpbrk should have returned NULL for empty string.\n");
    }
    if (u_strpbrk(testSurrogateString, surrMatchSetBad) != &testSurrogateString[0]) {
        log_err("u_strpbrk should have found bad surrogate.\n");
    }

    log_verbose("Testing u_strcspn()");

    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "a")) != 0) {
        log_err("u_strcspn couldn't find first letter a.\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "dc")) != 2) {
        log_err("u_strcspn couldn't find d or c.\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "cd")) != 2) {
        log_err("u_strcspn couldn't find c or d.\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "cdh")) != 2) {
        log_err("u_strcspn couldn't find c, d or h.\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "f")) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"f\".\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "fg")) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"fg\".\n");
    }
    if (u_strcspn(testString, u_uastrcpy(ucharBuf, "gf")) != u_strlen(testString)) {
        log_err("u_strcspn didn't return NULL for \"gf\".\n");
    }

    log_verbose("Testing u_strcspn() with surrogates");

    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "a")) != 1) {
        log_err("u_strcspn couldn't find first letter a.\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "dc")) != 5) {
        log_err("u_strcspn couldn't find d or c.\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "cd")) != 5) {
        log_err("u_strcspn couldn't find c or d.\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "cdh")) != 5) {
        log_err("u_strcspn couldn't find c, d or h.\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "f")) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"f\".\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "fg")) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"fg\".\n");
    }
    if (u_strcspn(testSurrogateString, u_uastrcpy(ucharBuf, "gf")) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn didn't return NULL for \"gf\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet1) != 3) {
        log_err("u_strcspn couldn't find \"0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet2) != 1) {
        log_err("u_strcspn couldn't find \"a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet3) != 3) {
        log_err("u_strcspn couldn't find \"0xdb00, 0xdf00, 0xdbff, 0xdfff\".\n");
    }
    if (u_strcspn(testSurrogateString, surrMatchSet4) != u_strlen(testSurrogateString)) {
        log_err("u_strcspn should have returned strlen for empty string.\n");
    }


    log_verbose("Testing u_strspn()");

    if (u_strspn(testString, u_uastrcpy(ucharBuf, "a")) != 1) {
        log_err("u_strspn couldn't skip first letter a.\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "ab")) != 2) {
        log_err("u_strspn couldn't skip a or b.\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "ba")) != 2) {
        log_err("u_strspn couldn't skip a or b.\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "f")) != 0) {
        log_err("u_strspn didn't return 0 for \"f\".\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "dc")) != 0) {
        log_err("u_strspn couldn't find first letter a (skip d or c).\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "abcd")) != u_strlen(testString)) {
        log_err("u_strspn couldn't skip over the whole string.\n");
    }
    if (u_strspn(testString, u_uastrcpy(ucharBuf, "")) != 0) {
        log_err("u_strspn should have returned 0 for empty string.\n");
    }

    log_verbose("Testing u_strspn() with surrogates");
    if (u_strspn(testSurrogateString, surrMatchSetBad) != 2) {
        log_err("u_strspn couldn't skip 0xdbff or a.\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSetBad2) != 2) {
        log_err("u_strspn couldn't skip 0xdbff or a.\n");
    }
    if (u_strspn(testSurrogateString, u_uastrcpy(ucharBuf, "f")) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, u_uastrcpy(ucharBuf, "dc")) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, u_uastrcpy(ucharBuf, "cd")) != 0) {
        log_err("u_strspn couldn't skip d or c (skip first letter).\n");
    }
    if (u_strspn(testSurrogateString, testSurrogateString) != u_strlen(testSurrogateString)) {
        log_err("u_strspn couldn't skip whole string.\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSet1) != 0) {
        log_err("u_strspn couldn't skip \"0xdbff, 0xdfff\" (get first letter).\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSetBad3) != 5) {
        log_err("u_strspn couldn't skip \"0xdbff, a, b, 0xdbff, 0xdfff\".\n");
    }
    if (u_strspn(testSurrogateString, surrMatchSet4) != 0) {
        log_err("u_strspn should have returned 0 for empty string.\n");
    }
}

static void TestStringCopy()
{
    UChar temp[40];
    UChar *result=0;
    UChar subString[5];
    UChar uchars[]={0x61, 0x62, 0x63, 0x00};
    char  charOut[40];
    char  chars[]="abc";    /* needs default codepage */

    log_verbose("Testing u_uastrncpy() and u_uastrcpy()");

    u_uastrcpy(temp, "abc");
    if(u_strcmp(temp, uchars) != 0) {
        log_err("There is an error in u_uastrcpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }

    temp[0] = 0xFB; /* load garbage into it */
    temp[1] = 0xFB;
    temp[2] = 0xFB;
    temp[3] = 0xFB;

    u_uastrncpy(temp, "abcabcabc", 3);
    if(u_strncmp(uchars, temp, 3) != 0){
        log_err("There is an error in u_uastrncpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }
    if(temp[3] != 0xFB) {
        log_err("u_uastrncpy wrote past it's bounds. Expected undisturbed byte at 3\n");
    }

    charOut[0] = (char)0x7B; /* load garbage into it */
    charOut[1] = (char)0x7B;
    charOut[2] = (char)0x7B;
    charOut[3] = (char)0x7B;

    temp[0] = 0x0061;
    temp[1] = 0x0062;
    temp[2] = 0x0063;
    temp[3] = 0x0061;
    temp[4] = 0x0062;
    temp[5] = 0x0063;
    temp[6] = 0x0000;

    u_austrncpy(charOut, temp, 3);
    if(strncmp(chars, charOut, 3) != 0){
        log_err("There is an error in u_austrncpy() Expected %s Got %s\n", austrdup(uchars), austrdup(temp));
    }
    if(charOut[3] != (char)0x7B) {
        log_err("u_austrncpy wrote past it's bounds. Expected undisturbed byte at 3\n");
    }

    /*Testing u_strchr()*/
    log_verbose("Testing u_strchr\n");
    temp[0]=0x42;
    temp[1]=0x62;
    temp[2]=0x62;
    temp[3]=0x63;
    temp[4]=0xd841;
    temp[5]=0xd841;
    temp[6]=0xdc02;
    temp[7]=0;
    result=u_strchr(temp, (UChar)0x62);
    if(result != temp+1){
        log_err("There is an error in u_strchr() Expected match at position 1 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    /*Testing u_strstr()*/
    log_verbose("Testing u_strstr\n");
    subString[0]=0x62;
    subString[1]=0x63;
    subString[2]=0;
    result=u_strstr(temp, subString);
    if(result != temp+2){
        log_err("There is an error in u_strstr() Expected match at position 2 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strstr(temp, subString+2); /* subString+2 is an empty string */
    if(result != temp){
        log_err("There is an error in u_strstr() Expected match at position 0 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strstr(subString, temp);
    if(result != NULL){
        log_err("There is an error in u_strstr() Expected NULL \"not found\" Got non-NULL \"found\" result\n");
    }

    /*Testing u_strchr32*/
    log_verbose("Testing u_strchr32\n");
    result=u_strchr32(temp, (UChar32)0x62);
    if(result != temp+1){
        log_err("There is an error in u_strchr32() Expected match at position 1 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_strchr32(temp, (UChar32)0xfb);
    if(result != NULL){
        log_err("There is an error in u_strchr32() Expected NULL \"not found\" Got non-NULL \"found\" result\n");
    }
    result=u_strchr32(temp, (UChar32)0x20402);
    if(result != temp+5){
        log_err("There is an error in u_strchr32() Expected match at position 5 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }

    temp[7]=0xfc00;
    result=u_memchr32(temp, (UChar32)0x20402, 7);
    if(result != temp+5){
        log_err("There is an error in u_memchr32() Expected match at position 5 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0x20402, 6);
    if(result != NULL){
        log_err("There is an error in u_memchr32() Expected no match Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0x20402, 1);
    if(result != NULL){
        log_err("There is an error in u_memchr32() Expected no match Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
    result=u_memchr32(temp, (UChar32)0xfc00, 8);
    if(result != temp+7){
        log_err("There is an error in u_memchr32() Expected match at position 7 Got %ld (pointer 0x%lx)\n", result-temp, result);
    }
}


/* test u_charName() -------------------------------------------------------- */

static const struct {
    uint32_t code;
    const char *name, *oldName, *extName;
} names[]={
    {0x0061, "LATIN SMALL LETTER A", "", "LATIN SMALL LETTER A"},
    {0x0284, "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK", "LATIN SMALL LETTER DOTLESS J BAR HOOK", "LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK" },
    {0x3401, "CJK UNIFIED IDEOGRAPH-3401", "", "CJK UNIFIED IDEOGRAPH-3401" },
    {0x7fed, "CJK UNIFIED IDEOGRAPH-7FED", "", "CJK UNIFIED IDEOGRAPH-7FED" },
    {0xac00, "HANGUL SYLLABLE GA", "", "HANGUL SYLLABLE GA" },
    {0xd7a3, "HANGUL SYLLABLE HIH", "", "HANGUL SYLLABLE HIH" },
    {0xd800, "", "", "<lead surrogate-D800>" },
    {0xdc00, "", "", "<trail surrogate-DC00>" },
    {0xff08, "FULLWIDTH LEFT PARENTHESIS", "FULLWIDTH OPENING PARENTHESIS", "FULLWIDTH LEFT PARENTHESIS" },
    {0xffe5, "FULLWIDTH YEN SIGN", "", "FULLWIDTH YEN SIGN" },
    {0xffff, "", "", "<noncharacter-FFFF>" },
    {0x23456, "CJK UNIFIED IDEOGRAPH-23456", "", "CJK UNIFIED IDEOGRAPH-23456" }
};

static UBool
enumCharNamesFn(void *context,
                UChar32 code, UCharNameChoice nameChoice,
                const char *name, int32_t length) {
    int32_t *pCount=(int32_t *)context;
    int i;

    if(length<=0 || length!=(int32_t)strlen(name)) {
        /* should not be called with an empty string or invalid length */
        log_err("u_enumCharName(0x%lx)=%s but length=%ld\n", name, length);
        return TRUE;
    }

    ++*pCount;
    for(i=0; i<sizeof(names)/sizeof(names[0]); ++i) {
        if(code==names[i].code) {
            switch (nameChoice) {
                case U_EXTENDED_CHAR_NAME:
                    if(0!=strcmp(name, names[i].extName)) {
                        log_err("u_enumCharName(0x%lx - Extended)=%s instead of %s\n", code, name, names[i].extName);
                    }
                    break;
                case U_UNICODE_CHAR_NAME:
                    if(0!=strcmp(name, names[i].name)) {
                        log_err("u_enumCharName(0x%lx)=%s instead of %s\n", code, name, names[i].name);
                    }
                    break;
                case U_UNICODE_10_CHAR_NAME:
                    if(names[i].oldName[0]==0 || 0!=strcmp(name, names[i].oldName)) {
                        log_err("u_enumCharName(0x%lx - 1.0)=%s instead of %s\n", code, name, names[i].oldName);
                    }
                    break;
                case U_CHAR_NAME_CHOICE_COUNT:
                    break;
            }
            break;
        }
    }
    return TRUE;
}

struct enumExtCharNamesContext {
    uint32_t length;
    int32_t last;
};

static UBool
enumExtCharNamesFn(void *context,
                UChar32 code, UCharNameChoice nameChoice,
                const char *name, int32_t length) {
    struct enumExtCharNamesContext *ecncp = (struct enumExtCharNamesContext *) context;

    if (ecncp->last != (int32_t) code - 1) {
        if (ecncp->last < 0) {
            log_err("u_enumCharName(0x%lx - Ext) after u_enumCharName(0x%lx - Ext) instead of u_enumCharName(0x%lx - Ext)\n", code, ecncp->last, ecncp->last + 1);
        } else {
            log_err("u_enumCharName(0x%lx - Ext) instead of u_enumCharName(0x0 - Ext)\n", code);
        }
    }
    ecncp->last = (int32_t) code;

    if (!*name) {
        log_err("u_enumCharName(0x%lx - Ext) should not be an empty string\n", code);
    }

    return enumCharNamesFn(&ecncp->length, code, nameChoice, name, length);
}

/**
 * This can be made more efficient by moving it into putil.c and having
 * it directly access the ebcdic translation tables.
 * TODO: If we get this method in putil.c, then delete it from here.
 */
static UChar
u_charToUChar(char c) {
    UChar uc;
    u_charsToUChars(&c, &uc, 1);
    return uc;
}

static void
TestCharNames() {
    static char name[80];
    UErrorCode errorCode=U_ZERO_ERROR;
    struct enumExtCharNamesContext extContext;
    int32_t length;
    UChar32 c;
    int i;

    log_verbose("Testing u_charName()\n");
    for(i=0; i<sizeof(names)/sizeof(names[0]); ++i) {
        /* modern Unicode character name */
        length=u_charName(names[i].code, U_UNICODE_CHAR_NAME, name, sizeof(name), &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("u_charName(0x%lx) error %s\n", names[i].code, u_errorName(errorCode));
            return;
        }
        if(length<0 || 0!=strcmp(name, names[i].name) || length!=(uint16_t)strlen(name)) {
            log_err("u_charName(0x%lx) gets: %s (length %ld) instead of: %s\n", names[i].code, name, length, names[i].name);
        }

        /* find the modern name */
        if (*names[i].name) {
            c=u_charFromName(U_UNICODE_CHAR_NAME, names[i].name, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("u_charFromName(%s) error %s\n", names[i].name, u_errorName(errorCode));
                return;
            }
            if(c!=names[i].code) {
                log_err("u_charFromName(%s) gets 0x%lx instead of 0x%lx\n", names[i].name, c, names[i].code);
            }
        }

        /* Unicode 1.0 character name */
        length=u_charName(names[i].code, U_UNICODE_10_CHAR_NAME, name, sizeof(name), &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("u_charName(0x%lx - 1.0) error %s\n", names[i].code, u_errorName(errorCode));
            return;
        }
        if(length<0 || (length>0 && 0!=strcmp(name, names[i].oldName)) || length!=(uint16_t)strlen(name)) {
            log_err("u_charName(0x%lx - 1.0) gets %s length %ld instead of nothing or %s\n", names[i].code, name, length, names[i].oldName);
        }

        /* find the Unicode 1.0 name if it is stored (length>0 means that we could read it) */
        if(names[i].oldName[0]!=0 /* && length>0 */) {
            c=u_charFromName(U_UNICODE_10_CHAR_NAME, names[i].oldName, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("u_charFromName(%s - 1.0) error %s\n", names[i].oldName, u_errorName(errorCode));
                return;
            }
            if(c!=names[i].code) {
                log_err("u_charFromName(%s - 1.0) gets 0x%lx instead of 0x%lx\n", names[i].oldName, c, names[i].code);
            }
        }
    }

    /* test u_enumCharNames() */
    length=0;
    errorCode=U_ZERO_ERROR;
    u_enumCharNames(UCHAR_MIN_VALUE, UCHAR_MAX_VALUE + 1, enumCharNamesFn, &length, U_UNICODE_CHAR_NAME, &errorCode);
    if(U_FAILURE(errorCode) || length<94140) {
        log_err("u_enumCharNames(%ld..%lx) error %s names count=%ld\n", UCHAR_MIN_VALUE, UCHAR_MAX_VALUE, u_errorName(errorCode), length);
    }

    extContext.length = 0;
    extContext.last = -1;
    errorCode=U_ZERO_ERROR;
    u_enumCharNames(UCHAR_MIN_VALUE, UCHAR_MAX_VALUE + 1, enumExtCharNamesFn, &extContext, U_EXTENDED_CHAR_NAME, &errorCode);
    if(U_FAILURE(errorCode) || extContext.length<UCHAR_MAX_VALUE + 1) {
        log_err("u_enumCharNames(%ld..0x%lx - Extended) error %s names count=%ld\n", UCHAR_MIN_VALUE, UCHAR_MAX_VALUE + 1, u_errorName(errorCode), extContext.length);
    }

    /* test that u_charFromName() uppercases the input name, i.e., works with mixed-case names (new in 2.0) */
    if(0x61!=u_charFromName(U_UNICODE_CHAR_NAME, "LATin smALl letTER A", &errorCode)) {
        log_err("u_charFromName(U_UNICODE_CHAR_NAME, \"LATin smALl letTER A\") did not find U+0061 (%s)\n", u_errorName(errorCode));
    }

    /* Test getCharNameCharacters */
    {
        enum { BUFSIZE = 256 };
        UErrorCode ec = U_ZERO_ERROR;
        char buf[BUFSIZE];
        int32_t i;
        UChar32 cp;
        UChar pat[BUFSIZE], dumbPat[BUFSIZE];
        int32_t l1, l2;
        UBool map[256];

        USet* set = uset_open(1, 0); /* empty set */
        USet* dumb = uset_open(1, 0); /* empty set */
        uprv_getCharNameCharacters(set);

        /* build set the dumb (but sure-fire) way */
        for (i=0; i<256; ++i) map[i] = FALSE;

        for (cp=0; cp<0x110000; ++cp) {
            int32_t choice;
            for (choice=0; choice<3; ++choice) {
                int32_t len = u_charName(cp, (UCharNameChoice) choice,
                                         buf, BUFSIZE, &ec);
                if (U_FAILURE(ec)) {
                    log_err("FAIL: u_charName failed when it shouldn't\n");
                    uset_close(set);
                    uset_close(dumb);
                    return;
                }

                for (i=0; i<len; ++i) {
                    if (!map[(uint8_t) buf[i]]) {
                        uset_add(dumb, (UChar32)u_charToUChar(buf[i]));
                        map[(uint8_t) buf[i]] = TRUE;
                    }
                }
            }
        }

        /* add all lowers, uppers, and #s to the dumb set */
        for (i=u_charToUChar('A'); i<=u_charToUChar('Z'); ++i) {
            uset_add(dumb, (UChar32)i);
        }
        for (i=u_charToUChar('a'); i<=u_charToUChar('z'); ++i) {
            uset_add(dumb, (UChar32)i);
        }
        for (i=u_charToUChar('0'); i<=u_charToUChar('9'); ++i) {
            uset_add(dumb, (UChar32)i);
        }

        /* compare the sets.  Where is my uset_equals?!! */
        l1 = uset_toPattern(set, pat, BUFSIZE, TRUE, &ec);
        l2 = uset_toPattern(dumb, dumbPat, BUFSIZE, TRUE, &ec);
        if (U_FAILURE(ec)) {
            log_err("FAIL: uset_toPattern failed when it shouldn't\n");
            uset_close(set);
            uset_close(dumb);
            return;
        }

        if (l1 >= BUFSIZE) {
            l1 = BUFSIZE-1;
            pat[l1] = 0;
        }
        if (l2 >= BUFSIZE) {
            l2 = BUFSIZE-1;
            dumbPat[l2] = 0;
        }

        if (l1 != l2 ||
            0 != u_strcmp(pat, dumbPat)) {
            char c1[256], c2[256];
            u_UCharsToChars(pat, c1, l1);
            u_UCharsToChars(dumbPat, c2, l2);
            c1[l1] = c2[l2] = 0;
            log_err("FAIL: uprv_getCharNameCharacters() returned %s, expected %s\n",
                    c1, c2);
        } else {
            char c1[256];
            u_UCharsToChars(pat, c1, l1);
            c1[l1] = 0;
            log_verbose("Ok: uprv_getCharNameCharacters() returned %s\n",
                        c1);
        }

        uset_close(set);
        uset_close(dumb);
    }


    /* ### TODO: test error cases and other interesting things */
}

/* test u_isMirrored() and u_charMirror() ----------------------------------- */

static void
TestMirroring() {
    log_verbose("Testing u_isMirrored()\n");
    if(!(u_isMirrored(0x28) && u_isMirrored(0xbb) && u_isMirrored(0x2045) && u_isMirrored(0x232a) &&
         !u_isMirrored(0x27) && !u_isMirrored(0x61) && !u_isMirrored(0x284) && !u_isMirrored(0x3400)
        )
    ) {
        log_err("u_isMirrored() does not work correctly\n");
    }

    log_verbose("Testing u_charMirror()\n");
    if(!(u_charMirror(0x3c)==0x3e && u_charMirror(0x5d)==0x5b && u_charMirror(0x208d)==0x208e && u_charMirror(0x3017)==0x3016 &&
         u_charMirror(0x2e)==0x2e && u_charMirror(0x6f3)==0x6f3 && u_charMirror(0x301c)==0x301c && u_charMirror(0xa4ab)==0xa4ab 
         )
    ) {
        log_err("u_charMirror() does not work correctly\n");
    }
}

/* test u_unescape() and u_unescapeAt() ------------------------------------- */

static void
TestUnescape() {
    static UChar buffer[200];
    static const UChar expect[]={
        0x53, 0x63, 0x68, 0xf6, 0x6e, 0x65, 0x73, 0x20, 0x41, 0x75, 0x74, 0x6f, 0x3a, 0x20,
        0x20ac, 0x20, 0x31, 0x31, 0x32, 0x34, 0x30, 0x2e, 0x0c,
        0x50, 0x72, 0x69, 0x76, 0x61, 0x74, 0x65, 0x73, 0x20,
        0x5a, 0x65, 0x69, 0x63, 0x68, 0x65, 0x6e, 0x3a, 0x20, 0xdbc8, 0xdf45, 0x0a, 0
    };
    int32_t length;

    /* test u_unescape() */
    length=u_unescape(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n",
        buffer, sizeof(buffer)/sizeof(buffer[0]));
    if(length!=45 || u_strcmp(buffer, expect)!=0) {
        log_err("failure in u_unescape(): length %d!=45 and/or incorrect result string\n", length);
    }

    /* try preflighting */
    length=u_unescape(
        "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\n",
        NULL, sizeof(buffer)/sizeof(buffer[0]));
    if(length!=45 || u_strcmp(buffer, expect)!=0) {
        log_err("failure in u_unescape(preflighting): length %d!=45\n", length);
    }

    /* ### TODO: test u_unescapeAt() */
}

static void TestUScriptCodeAPI(){
    int i =0;
    int numErrors =0;
    {
        const char* testNames[]={
        /* test locale */
        "en", "en_US", "sr", "ta" , "te_IN",
        "hi", "he", "ar",
        /* test abbr */
        "Hani", "Hang","Hebr","Hira",
        "Knda","Kana","Khmr","Lao",
        "Latn",/*"Latf","Latg",*/ 
        "Mlym", "Mong",
    
        /* test names */
        "CYRILLIC","DESERET","DEVANAGARI","ETHIOPIC","GEORGIAN", 
        "GOTHIC",  "GREEK",  "GUJARATI", "COMMON", "INHERITED", 
        /* test lower case names */
        "malayalam", "mongolian", "myanmar", "ogham", "old-italic",
        "oriya",     "runic",     "sinhala", "syriac","tamil",     
        "telugu",    "thaana",    "thai",    "tibetan", 
        /* test the bounds*/
        "ucas", "arabic",
        /* test bogus */
        "asfdasd", "5464", "12235",
        /* test the last index */
        "zyyy", "YI",
        '\0'  
        };
        UScriptCode expected[] ={
            /* locales should return */
            USCRIPT_LATIN, USCRIPT_LATIN, USCRIPT_CYRILLIC, USCRIPT_TAMIL, USCRIPT_TELUGU, 
            USCRIPT_DEVANAGARI, USCRIPT_HEBREW, USCRIPT_ARABIC,
            /* abbr should return */
            USCRIPT_HAN, USCRIPT_HANGUL, USCRIPT_HEBREW, USCRIPT_HIRAGANA,
            USCRIPT_KANNADA, USCRIPT_KATAKANA, USCRIPT_KHMER, USCRIPT_LAO,
            USCRIPT_LATIN,/* USCRIPT_LATIN, USCRIPT_LATIN,*/ 
            USCRIPT_MALAYALAM, USCRIPT_MONGOLIAN,
            /* names should return */
            USCRIPT_CYRILLIC, USCRIPT_DESERET, USCRIPT_DEVANAGARI, USCRIPT_ETHIOPIC, USCRIPT_GEORGIAN,
            USCRIPT_GOTHIC, USCRIPT_GREEK, USCRIPT_GUJARATI, USCRIPT_COMMON, USCRIPT_INHERITED,
            /* lower case names should return */    
            USCRIPT_MALAYALAM, USCRIPT_MONGOLIAN, USCRIPT_MYANMAR, USCRIPT_OGHAM, USCRIPT_OLD_ITALIC,
            USCRIPT_ORIYA, USCRIPT_RUNIC, USCRIPT_SINHALA, USCRIPT_SYRIAC, USCRIPT_TAMIL,
            USCRIPT_TELUGU, USCRIPT_THAANA, USCRIPT_THAI, USCRIPT_TIBETAN,
            /* bounds */
            USCRIPT_UCAS, USCRIPT_ARABIC,
            /* bogus names should return invalid code */
            USCRIPT_INVALID_CODE, USCRIPT_INVALID_CODE, USCRIPT_INVALID_CODE,
            USCRIPT_COMMON, USCRIPT_YI,
        };

        UErrorCode err = U_ZERO_ERROR;

        const int32_t capacity = 10;

        for( ; testNames[i]!='\0'; i++){
            UScriptCode script[10]={USCRIPT_INVALID_CODE};
            uscript_getCode(testNames[i],script,capacity, &err);
            if( script[0] != expected[i]){
                   log_verbose("Error getting script code Got: %i  Expected: %i for name %s\n",
                       script[0],expected[i],testNames[i]);
                   numErrors++;
            }
        }
        if(numErrors >0 ){
            log_err("Errors uchar_getScriptCode() : %i \n",numErrors);
        }
    }
    {
        UErrorCode err = U_ZERO_ERROR;
        int32_t capacity=0;
        UScriptCode jaCode[]={ USCRIPT_KATAKANA,USCRIPT_HIRAGANA,USCRIPT_HAN};
        UScriptCode script[10]={USCRIPT_INVALID_CODE};
        int32_t num = uscript_getCode("ja",script,capacity, &err);
        /* preflight */
        if(err==U_BUFFER_OVERFLOW_ERROR){
            err = U_ZERO_ERROR;
            capacity = 10;
            num = uscript_getCode("ja",script,capacity, &err);
            if(num!=(sizeof(jaCode)/sizeof(UScriptCode)) || script[0]!=jaCode[0] || script[1]!=jaCode[1]){
                log_err("Errors uscript_getScriptCode() for Japaneese locale \n");
            }
        }else{
            log_err("Errors in uscript_getScriptCode() expected error : %s got: %s \n", 
                "U_BUFFER_OVERFLOW_ERROR",
                 u_errorName(err));
        }

    }

    {
        UScriptCode testAbbr[]={
            /* names should return */
            USCRIPT_CYRILLIC, USCRIPT_DESERET, USCRIPT_DEVANAGARI, USCRIPT_ETHIOPIC, USCRIPT_GEORGIAN,
            USCRIPT_GOTHIC, USCRIPT_GREEK, USCRIPT_GUJARATI,
        };

        const char* expectedNames[]={
              
            /* test names */
            "CYRILLIC","DESERET","DEVANAGARI","ETHIOPIC","GEORGIAN", 
            "GOTHIC",  "GREEK",  "GUJARATI", 
             '\0'
        };
        i=0;
        while(i<sizeof(testAbbr)/sizeof(UScriptCode)){
            const char* name = uscript_getName(testAbbr[i]);
            numErrors=0;
            if(strcmp(expectedNames[i],name)!=0){
                log_verbose("Error getting abbreviations Got: %s Expected: %s\n",name,expectedNames[i]);
                numErrors++;
            }
            if(numErrors > 0){
                if(numErrors >0 ){
                    log_err("Errors uchar_getScriptAbbr() : %i \n",numErrors);
                }
            }
            i++;
        }

    }
    {
        UScriptCode testAbbr[]={
            /* abbr should return */
            USCRIPT_HAN, USCRIPT_HANGUL, USCRIPT_HEBREW, USCRIPT_HIRAGANA,
            USCRIPT_KANNADA, USCRIPT_KATAKANA, USCRIPT_KHMER, USCRIPT_LAO,
            USCRIPT_LATIN, 
            USCRIPT_MALAYALAM, USCRIPT_MONGOLIAN,
        };

        const char* expectedAbbr[]={
              /* test abbr */
            "Hani", "Hang","Hebr","Hira",
            "Knda","Kana","Khmr","Lao",
            "Latn",
            "Mlym", "Mong",
             '\0'
        };
        i=0;
        while(i<sizeof(testAbbr)/sizeof(UScriptCode)){
            const char* name = uscript_getShortName(testAbbr[i]);
            numErrors=0;
            if(strcmp(expectedAbbr[i],name)!=0){
                log_verbose("Error getting abbreviations Got: %s Expected: %s\n",name,expectedAbbr[i]);
                numErrors++;
            }
            if(numErrors > 0){
                if(numErrors >0 ){
                    log_err("Errors uchar_getScriptAbbr() : %i \n",numErrors);
                }
            }
            i++;
        }

    }
    /* now test uscript_getScript() API */
    {
#define MAX_ARRAY_SIZE 23
        uint32_t codepoints[] = {
                0x0000FF9D, /* USCRIPT_KATAKANA*/
                0x0000FFBE, /* USCRIPT_HANGUL*/
                0x0000FFC7, /* USCRIPT_HANGUL*/
                0x0000FFCF, /* USCRIPT_HANGUL*/
                0x0000FFD7, /* USCRIPT_HANGUL*/
                0x0000FFDC, /* USCRIPT_HANGUL*/
                0x00010300, /* USCRIPT_OLD_ITALIC*/
                0x00010330, /* USCRIPT_GOTHIC*/
                0x0001034A, /* USCRIPT_GOTHIC*/
                0x00010400, /* USCRIPT_DESERET*/
                0x00010428, /* USCRIPT_DESERET*/
                0x0001D167, /* USCRIPT_INHERITED*/
                0x0001D17B, /* USCRIPT_INHERITED*/
                0x0001D185, /* USCRIPT_INHERITED*/
                0x0001D1AA, /* USCRIPT_INHERITED*/
                0x00020000, /* USCRIPT_HAN*/
                0x00000D02, /* USCRIPT_MALAYALAM*/
                0x00000D00, /* USCRIPT_COMMON */
                0x00000000, /* USCRIPT_COMMON*/
                0x0001D169, /* USCRIPT_INHERITED*/
                0x0001D182, /* USCRIPT_INHERITED*/
                0x0001D18B, /* USCRIPT_INHERITED*/
                0x0001D1AD, /* USCRIPT_INHERITED*/
                0x00110000, /* USCRIPT_INVALID_CODE */
        };

        UScriptCode expected[] = {
                USCRIPT_KATAKANA ,
                USCRIPT_HANGUL ,
                USCRIPT_HANGUL ,
                USCRIPT_HANGUL ,
                USCRIPT_HANGUL ,
                USCRIPT_HANGUL ,
                USCRIPT_OLD_ITALIC, 
                USCRIPT_GOTHIC ,
                USCRIPT_GOTHIC ,
                USCRIPT_DESERET ,
                USCRIPT_DESERET ,
                USCRIPT_INHERITED,
                USCRIPT_INHERITED,
                USCRIPT_INHERITED,
                USCRIPT_INHERITED,
                USCRIPT_HAN ,
                USCRIPT_MALAYALAM,
                USCRIPT_COMMON,
                USCRIPT_COMMON,
                USCRIPT_INHERITED ,
                USCRIPT_INHERITED ,
                USCRIPT_INHERITED ,
                USCRIPT_INHERITED ,
                USCRIPT_INVALID_CODE,
        };
        UScriptCode code = USCRIPT_INVALID_CODE;
        UErrorCode status = U_ZERO_ERROR;
        UBool passed = TRUE;

        i =0;
        while(i< MAX_ARRAY_SIZE){
            code = uscript_getScript(codepoints[i],&status);
            if(U_SUCCESS(status)){
                if( code != expected[i] ||
                    code != (UScriptCode)u_getIntPropertyValue(codepoints[i], UCHAR_SCRIPT)
                ) {
                    log_verbose("uscript_getScript for codepoint \\U%08X failed\n",codepoints[i]);
                    passed = FALSE;
                }
            }else{
                log_err("uscript_getScript for codepoint \\U%08X failed. Error: %s\n", 
                         codepoints[i],u_errorName(status));
                break;
            }
            i++;
        }
        
        if(passed==FALSE){
           log_err("uscript_getScript failed.\n");
        }      
    }
    {
        UScriptCode code= USCRIPT_INVALID_CODE;
        UErrorCode  status = U_ZERO_ERROR;
        code = uscript_getScript(0x001D169,&status);
        if(code != USCRIPT_INHERITED){
            log_err("\\U001D169 is not contained in USCRIPT_INHERITED");
        }
    }
    {
        UScriptCode code= USCRIPT_INVALID_CODE;
        UErrorCode  status = U_ZERO_ERROR;
        int32_t err = 0;

        for(i = 0; i<=0x10ffff; i++){
            code =  uscript_getScript(i,&status);
            if(code == USCRIPT_INVALID_CODE){
                err++;
                log_verbose("uscript_getScript for codepoint \\U%08X failed.\n", i);
            }
        }
        if(err>0){
            log_err("uscript_getScript failed for %d codepoints\n", err);
        }
    }
                

 }

struct RunTestData
{
    const char *runText;
    UScriptCode runCode;
};

typedef struct RunTestData RunTestData;

static void
CheckScriptRuns(UScriptRun *scriptRun, int32_t *runStarts, const RunTestData *testData, int32_t nRuns,
                const char *prefix)
{
    int32_t run, runStart, runLimit;
    UScriptCode runCode;

    /* iterate over all the runs */
    run = 0;
    while (uscript_nextRun(scriptRun, &runStart, &runLimit, &runCode)) {
        if (runStart != runStarts[run]) {
            log_err("%s: incorrect start offset for run %d: expected %d, got %d\n",
                prefix, run, runStarts[run], runStart);
        }

        if (runLimit != runStarts[run + 1]) {
            log_err("%s: incorrect limit offset for run %d: expected %d, got %d\n",
                prefix, run, runStarts[run + 1], runLimit);
        }

        if (runCode != testData[run].runCode) {
            log_err("%s: incorrect script for run %d: expected \"%s\", got \"%s\"\n",
                prefix, run, uscript_getName(testData[run].runCode), uscript_getName(runCode));
        }
        
        run += 1;

        /* stop when we've seen all the runs we expect to see */
        if (run >= nRuns) {
            break;
        }
    }

    /* Complain if we didn't see then number of runs we expected */
    if (run != nRuns) {
        log_err("%s: incorrect number of runs: expected %d, got %d\n", prefix, run, nRuns);
    }
}

static void
TestUScriptRunAPI()
{
    static const RunTestData testData[] = {
        {"\\u0020\\u0946\\u0939\\u093F\\u0928\\u094D\\u0926\\u0940\\u0020", USCRIPT_DEVANAGARI},
        {"\\u0627\\u0644\\u0639\\u0631\\u0628\\u064A\\u0629\\u0020", USCRIPT_ARABIC},
        {"\\u0420\\u0443\\u0441\\u0441\\u043A\\u0438\\u0439\\u0020", USCRIPT_CYRILLIC},
        {"English (", USCRIPT_LATIN},
        {"\\u0E44\\u0E17\\u0E22", USCRIPT_THAI},
        {") ", USCRIPT_LATIN},
        {"\\u6F22\\u5B75", USCRIPT_HAN},
        {"\\u3068\\u3072\\u3089\\u304C\\u306A\\u3068", USCRIPT_HIRAGANA},
        {"\\u30AB\\u30BF\\u30AB\\u30CA", USCRIPT_KATAKANA},
        {"\\U00010400\\U00010401\\U00010402\\U00010403", USCRIPT_DESERET}
    };

    int32_t nTestRuns = sizeof testData / sizeof testData[0];

    UChar testString[1024];
    int32_t runStarts[256];

    int32_t run, stringLimit;
    UScriptRun *scriptRun = NULL;
    UErrorCode err;

    /*
     * Fill in the test string and the runStarts array.
     */
    stringLimit = 0;
    for (run = 0; run < nTestRuns; run += 1) {
        runStarts[run] = stringLimit;
        stringLimit += u_unescape(testData[run].runText, &testString[stringLimit], 1024 - stringLimit);
        stringLimit -= 1;
    }

    /* The limit of the last run */ 
    runStarts[nTestRuns] = stringLimit;

    /*
     * Make sure that calling uscript_OpenRun with a NULL text pointer
     * and a non-zero text length returns the correct error.
     */
    err = U_ZERO_ERROR;
    scriptRun = uscript_openRun(NULL, stringLimit, &err);

    if (err != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("uscript_openRun(NULL, stringLimit, &err) returned %s instead of U_ILLEGAL_ARGUMENT_ERROR.\n", u_errorName(err));
    }

    if (scriptRun != NULL) {
        log_err("uscript_openRun(NULL, stringLimit, &err) returned a non-NULL result.\n");
        uscript_closeRun(scriptRun);
    }

    /*
     * Make sure that calling uscript_OpenRun with a non-NULL text pointer
     * and a zero text length returns the correct error.
     */
    err = U_ZERO_ERROR;
    scriptRun = uscript_openRun(testString, 0, &err);

    if (err != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("uscript_openRun(testString, 0, &err) returned %s instead of U_ILLEGAL_ARGUMENT_ERROR.\n", u_errorName(err));
    }

    if (scriptRun != NULL) {
        log_err("uscript_openRun(testString, 0, &err) returned a non-NULL result.\n");
        uscript_closeRun(scriptRun);
    }

    /*
     * Make sure that calling uscript_openRun with a NULL text pointer
     * and a zero text length doesn't return an error.
     */
    err = U_ZERO_ERROR;
    scriptRun = uscript_openRun(NULL, 0, &err);

    if (U_FAILURE(err)) {
        log_err("Got error %s from uscript_openRun(NULL, 0, &err)\n", u_errorName(err));
    }

    /* Make sure that the empty iterator doesn't find any runs */
    if (uscript_nextRun(scriptRun, NULL, NULL, NULL)) {
        log_err("uscript_nextRun(...) returned TRUE for an empty iterator.\n");
    }

    /*
     * Make sure that calling uscript_setRunText with a NULL text pointer
     * and a non-zero text length returns the correct error.
     */
    err = U_ZERO_ERROR;
    uscript_setRunText(scriptRun, NULL, stringLimit, &err);

    if (err != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("uscript_setRunText(scriptRun, NULL, stringLimit, &err) returned %s instead of U_ILLEGAL_ARGUMENT_ERROR.\n", u_errorName(err));
    }

    /*
     * Make sure that calling uscript_OpenRun with a non-NULL text pointer
     * and a zero text length returns the correct error.
     */
    err = U_ZERO_ERROR;
    uscript_setRunText(scriptRun, testString, 0, &err);

    if (err != U_ILLEGAL_ARGUMENT_ERROR) {
        log_err("uscript_setRunText(scriptRun, testString, 0, &err) returned %s instead of U_ILLEGAL_ARGUMENT_ERROR.\n", u_errorName(err));
    }

    /*
     * Now call uscript_setRunText on the empty iterator
     * and make sure that it works.
     */
    err = U_ZERO_ERROR;
    uscript_setRunText(scriptRun, testString, stringLimit, &err);

    if (U_FAILURE(err)) {
        log_err("Got error %s from uscript_setRunText(...)\n", u_errorName(err));
    } else {
        CheckScriptRuns(scriptRun, runStarts, testData, nTestRuns, "uscript_setRunText");
    }

    uscript_closeRun(scriptRun);

    /* 
     * Now open an interator over the testString
     * using uscript_openRun and make sure that it works
     */
    scriptRun = uscript_openRun(testString, stringLimit, &err);

    if (U_FAILURE(err)) {
        log_err("Got error %s from uscript_openRun(...)\n", u_errorName(err));
    } else {
        CheckScriptRuns(scriptRun, runStarts, testData, nTestRuns, "uscript_openRun");
    }

    /* Now reset the iterator, and make sure
     * that it still works.
     */
    uscript_resetRun(scriptRun);

    CheckScriptRuns(scriptRun, runStarts, testData, nTestRuns, "uscript_resetRun");

    /* Close the iterator */
    uscript_closeRun(scriptRun);
}

/* test additional, non-core properties */
static void
TestAdditionalProperties() {
    /* test data for u_charAge() */
    static const struct {
        UChar32 c;
        UVersionInfo version;
    } charAges[]={
        {0x41,    { 1, 1, 0, 0 }},
        {0xffff,  { 1, 1, 0, 0 }},
        {0x20ab,  { 2, 0, 0, 0 }},
        {0x2fffe, { 2, 0, 0, 0 }},
        {0x20ac,  { 2, 1, 0, 0 }},
        {0xfb1d,  { 3, 0, 0, 0 }},
        {0x3f4,   { 3, 1, 0, 0 }},
        {0x10300, { 3, 1, 0, 0 }},
        {0x220,   { 3, 2, 0, 0 }},
        {0xff60,  { 3, 2, 0, 0 }}
    };

    /* test data for u_hasBinaryProperty() */
    static int32_t
    props[][3]={ /* code point, property, value */
        { 0x0627, UCHAR_ALPHABETIC, TRUE },
        { 0x1034a, UCHAR_ALPHABETIC, TRUE },
        { 0x2028, UCHAR_ALPHABETIC, FALSE },

        { 0x0066, UCHAR_ASCII_HEX_DIGIT, TRUE },
        { 0x0067, UCHAR_ASCII_HEX_DIGIT, FALSE },

        { 0x202c, UCHAR_BIDI_CONTROL, TRUE },
        { 0x202f, UCHAR_BIDI_CONTROL, FALSE },

        { 0x003c, UCHAR_BIDI_MIRRORED, TRUE },
        { 0x003d, UCHAR_BIDI_MIRRORED, FALSE },

        { 0x058a, UCHAR_DASH, TRUE },
        { 0x007e, UCHAR_DASH, FALSE },

        { 0x0c4d, UCHAR_DIACRITIC, TRUE },
        { 0x3000, UCHAR_DIACRITIC, FALSE },

        { 0x0e46, UCHAR_EXTENDER, TRUE },
        { 0x0020, UCHAR_EXTENDER, FALSE },

        { 0xfb1d, UCHAR_FULL_COMPOSITION_EXCLUSION, TRUE },
        { 0x1d15f, UCHAR_FULL_COMPOSITION_EXCLUSION, TRUE },
        { 0xfb1e, UCHAR_FULL_COMPOSITION_EXCLUSION, FALSE },

        { 0x0044, UCHAR_HEX_DIGIT, TRUE },
        { 0xff46, UCHAR_HEX_DIGIT, TRUE },
        { 0x0047, UCHAR_HEX_DIGIT, FALSE },

        { 0x30fb, UCHAR_HYPHEN, TRUE },
        { 0xfe58, UCHAR_HYPHEN, FALSE },

        { 0x2172, UCHAR_ID_CONTINUE, TRUE },
        { 0x0307, UCHAR_ID_CONTINUE, TRUE },
        { 0x005c, UCHAR_ID_CONTINUE, FALSE },

        { 0x2172, UCHAR_ID_START, TRUE },
        { 0x007a, UCHAR_ID_START, TRUE },
        { 0x0039, UCHAR_ID_START, FALSE },

        { 0x4db5, UCHAR_IDEOGRAPHIC, TRUE },
        { 0x2f999, UCHAR_IDEOGRAPHIC, TRUE },
        { 0x2f99, UCHAR_IDEOGRAPHIC, FALSE },

        { 0x200c, UCHAR_JOIN_CONTROL, TRUE },
        { 0x2029, UCHAR_JOIN_CONTROL, FALSE },

        { 0x1d7bc, UCHAR_LOWERCASE, TRUE },
        { 0x0345, UCHAR_LOWERCASE, TRUE },
        { 0x0030, UCHAR_LOWERCASE, FALSE },

        { 0x1d7a9, UCHAR_MATH, TRUE },
        { 0x2135, UCHAR_MATH, TRUE },
        { 0x0062, UCHAR_MATH, FALSE },

        { 0xfde1, UCHAR_NONCHARACTER_CODE_POINT, TRUE },
        { 0x10ffff, UCHAR_NONCHARACTER_CODE_POINT, TRUE },
        { 0x10fffd, UCHAR_NONCHARACTER_CODE_POINT, FALSE },

        { 0x0022, UCHAR_QUOTATION_MARK, TRUE },
        { 0xff62, UCHAR_QUOTATION_MARK, TRUE },
        { 0xd840, UCHAR_QUOTATION_MARK, FALSE },

        { 0x061f, UCHAR_TERMINAL_PUNCTUATION, TRUE },
        { 0xe003f, UCHAR_TERMINAL_PUNCTUATION, FALSE },

        { 0x1d44a, UCHAR_UPPERCASE, TRUE },
        { 0x2162, UCHAR_UPPERCASE, TRUE },
        { 0x0345, UCHAR_UPPERCASE, FALSE },

        { 0x0020, UCHAR_WHITE_SPACE, TRUE },
        { 0x202f, UCHAR_WHITE_SPACE, TRUE },
        { 0x3001, UCHAR_WHITE_SPACE, FALSE },

        { 0x0711, UCHAR_XID_CONTINUE, TRUE },
        { 0x1d1aa, UCHAR_XID_CONTINUE, TRUE },
        { 0x007c, UCHAR_XID_CONTINUE, FALSE },

        { 0x16ee, UCHAR_XID_START, TRUE },
        { 0x23456, UCHAR_XID_START, TRUE },
        { 0x1d1aa, UCHAR_XID_START, FALSE },

        /*
         * Version break:
         * The following properties are only supported starting with the
         * Unicode version indicated in the second field.
         */
        { -1, 0x32, 0 },

        { 0x180c, UCHAR_DEFAULT_IGNORABLE_CODE_POINT, TRUE },
        { 0xfe02, UCHAR_DEFAULT_IGNORABLE_CODE_POINT, TRUE },
        { 0x1801, UCHAR_DEFAULT_IGNORABLE_CODE_POINT, FALSE },

        { 0x0341, UCHAR_DEPRECATED, TRUE },
        { 0xe0041, UCHAR_DEPRECATED, FALSE },

        { 0x00a0, UCHAR_GRAPHEME_BASE, TRUE },
        { 0x0a4d, UCHAR_GRAPHEME_BASE, FALSE },
        { 0xff9f, UCHAR_GRAPHEME_BASE, FALSE },

        { 0x0300, UCHAR_GRAPHEME_EXTEND, TRUE },
        { 0xff9f, UCHAR_GRAPHEME_EXTEND, TRUE },
        { 0x0a4d, UCHAR_GRAPHEME_EXTEND, FALSE },

        { 0x0a4d, UCHAR_GRAPHEME_LINK, TRUE },
        { 0xff9f, UCHAR_GRAPHEME_LINK, FALSE },

        { 0x2ff7, UCHAR_IDS_BINARY_OPERATOR, TRUE },
        { 0x2ff3, UCHAR_IDS_BINARY_OPERATOR, FALSE },

        { 0x2ff3, UCHAR_IDS_TRINARY_OPERATOR, TRUE },
        { 0x2f03, UCHAR_IDS_TRINARY_OPERATOR, FALSE },

        { 0x0ec1, UCHAR_LOGICAL_ORDER_EXCEPTION, TRUE },
        { 0xdcba, UCHAR_LOGICAL_ORDER_EXCEPTION, FALSE },

        { 0x2e9b, UCHAR_RADICAL, TRUE },
        { 0x4e00, UCHAR_RADICAL, FALSE },

        { 0x012f, UCHAR_SOFT_DOTTED, TRUE },
        { 0x0049, UCHAR_SOFT_DOTTED, FALSE },

        { 0xfa11, UCHAR_UNIFIED_IDEOGRAPH, TRUE },
        { 0xfa12, UCHAR_UNIFIED_IDEOGRAPH, FALSE },

        /* enum/integer type properties */

        /* UCHAR_BIDI_CLASS tested for assigned characters in TestUnicodeData() */
#if 0
        /* ### TODO test default Bidi classes for unassigned code points */
        { 0x, UCHAR_BIDI_CLASS,  },
        { 0x, UCHAR_BIDI_CLASS,  },
        { 0x, UCHAR_BIDI_CLASS,  },
#endif

        { 0x02AF, UCHAR_BLOCK, UBLOCK_IPA_EXTENSIONS },
        { 0x0C4E, UCHAR_BLOCK, UBLOCK_TELUGU },
        { 0x155A, UCHAR_BLOCK, UBLOCK_UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS },
        { 0x1717, UCHAR_BLOCK, UBLOCK_TAGALOG },
        { 0x1AFF, UCHAR_BLOCK, UBLOCK_INVALID_CODE },
        { 0x3040, UCHAR_BLOCK, UBLOCK_HIRAGANA },
        { 0x1D0FF, UCHAR_BLOCK, UBLOCK_BYZANTINE_MUSICAL_SYMBOLS },
        { 0x10D0FF, UCHAR_BLOCK, UBLOCK_SUPPLEMENTARY_PRIVATE_USE_AREA_B },
        { 0xEFFFF, UCHAR_BLOCK, UBLOCK_INVALID_CODE },

        /* UCHAR_CANONICAL_COMBINING_CLASS tested for assigned characters in TestUnicodeData() */
        { 0xd7d7, UCHAR_CANONICAL_COMBINING_CLASS, 0 },

        { 0x00A0, UCHAR_DECOMPOSITION_TYPE, U_DT_NOBREAK },
        { 0x00A8, UCHAR_DECOMPOSITION_TYPE, U_DT_COMPAT },
        { 0x00bf, UCHAR_DECOMPOSITION_TYPE, U_DT_NONE },
        { 0x00c0, UCHAR_DECOMPOSITION_TYPE, U_DT_CANONICAL },
        { 0x1E9B, UCHAR_DECOMPOSITION_TYPE, U_DT_CANONICAL },
        { 0xBCDE, UCHAR_DECOMPOSITION_TYPE, U_DT_CANONICAL },
        { 0xFB5D, UCHAR_DECOMPOSITION_TYPE, U_DT_MEDIAL },
        { 0x1D736, UCHAR_DECOMPOSITION_TYPE, U_DT_FONT },
        { 0xe0033, UCHAR_DECOMPOSITION_TYPE, U_DT_NONE },

        { 0x0009, UCHAR_EAST_ASIAN_WIDTH, U_EA_NEUTRAL },
        { 0x0020, UCHAR_EAST_ASIAN_WIDTH, U_EA_NARROW },
        { 0x00B1, UCHAR_EAST_ASIAN_WIDTH, U_EA_AMBIGUOUS },
        { 0x20A9, UCHAR_EAST_ASIAN_WIDTH, U_EA_HALFWIDTH },
        { 0x2FFB, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0x3000, UCHAR_EAST_ASIAN_WIDTH, U_EA_FULLWIDTH },
        { 0x35bb, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0x58bd, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0xD7A3, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0xEEEE, UCHAR_EAST_ASIAN_WIDTH, U_EA_AMBIGUOUS },
        { 0x1D198, UCHAR_EAST_ASIAN_WIDTH, U_EA_NEUTRAL },
        { 0x20000, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0x2F8C7, UCHAR_EAST_ASIAN_WIDTH, U_EA_WIDE },
        { 0x3a5bd, UCHAR_EAST_ASIAN_WIDTH, U_EA_NEUTRAL },
        { 0xFEEEE, UCHAR_EAST_ASIAN_WIDTH, U_EA_AMBIGUOUS },
        { 0x10EEEE, UCHAR_EAST_ASIAN_WIDTH, U_EA_AMBIGUOUS },

        /* UCHAR_GENERAL_CATEGORY tested for assigned characters in TestUnicodeData() */
        { 0xd7d7, UCHAR_GENERAL_CATEGORY, 0 },

        { 0x0444, UCHAR_JOINING_GROUP, U_JG_NO_JOINING_GROUP },
        { 0x0639, UCHAR_JOINING_GROUP, U_JG_AIN },
        { 0x072A, UCHAR_JOINING_GROUP, U_JG_DALATH_RISH },
        { 0x0647, UCHAR_JOINING_GROUP, U_JG_HEH },
        { 0x06C1, UCHAR_JOINING_GROUP, U_JG_HEH_GOAL },
        { 0x06C3, UCHAR_JOINING_GROUP, U_JG_HAMZA_ON_HEH_GOAL },

        { 0x200C, UCHAR_JOINING_TYPE, U_JT_NON_JOINING },
        { 0x200D, UCHAR_JOINING_TYPE, U_JT_JOIN_CAUSING },
        { 0x0639, UCHAR_JOINING_TYPE, U_JT_DUAL_JOINING },
        { 0x0640, UCHAR_JOINING_TYPE, U_JT_JOIN_CAUSING },
        { 0x06C3, UCHAR_JOINING_TYPE, U_JT_RIGHT_JOINING },
        { 0x0300, UCHAR_JOINING_TYPE, U_JT_TRANSPARENT },
        { 0x070F, UCHAR_JOINING_TYPE, U_JT_TRANSPARENT },
        { 0xe0033, UCHAR_JOINING_TYPE, U_JT_TRANSPARENT },

        /* TestUnicodeData() verifies that no assigned character has "XX" (unknown) */
        { 0xe7e7, UCHAR_LINE_BREAK, U_LB_UNKNOWN },
        { 0x10fffd, UCHAR_LINE_BREAK, U_LB_UNKNOWN },
        { 0x0028, UCHAR_LINE_BREAK, U_LB_OPEN_PUNCTUATION },
        { 0x232A, UCHAR_LINE_BREAK, U_LB_CLOSE_PUNCTUATION },
        { 0x3401, UCHAR_LINE_BREAK, U_LB_IDEOGRAPHIC },
        { 0x4e02, UCHAR_LINE_BREAK, U_LB_IDEOGRAPHIC },
        { 0xac03, UCHAR_LINE_BREAK, U_LB_IDEOGRAPHIC },
        { 0x20004, UCHAR_LINE_BREAK, U_LB_IDEOGRAPHIC },
        { 0xf905, UCHAR_LINE_BREAK, U_LB_IDEOGRAPHIC },
        { 0xdb7e, UCHAR_LINE_BREAK, U_LB_SURROGATE },
        { 0xdbfd, UCHAR_LINE_BREAK, U_LB_SURROGATE },
        { 0xdffc, UCHAR_LINE_BREAK, U_LB_SURROGATE },
        { 0x2762, UCHAR_LINE_BREAK, U_LB_EXCLAMATION },
        { 0x002F, UCHAR_LINE_BREAK, U_LB_BREAK_SYMBOLS },
        { 0x1D49C, UCHAR_LINE_BREAK, U_LB_ALPHABETIC },
        { 0x1731, UCHAR_LINE_BREAK, U_LB_ALPHABETIC },

        /* UCHAR_NUMERIC_TYPE tested in TestNumericProperties() */

        /* UCHAR_SCRIPT tested in TestUScriptCodeAPI() */

        /* undefined UProperty values */
        { 0x61, 0x4a7, 0 },
        { 0x234bc, 0x15ed, 0 }
    };

    UVersionInfo version;
    UChar32 c;
    int32_t i, result, uVersion;
    UProperty which;

    /* what is our Unicode version? */
    u_getUnicodeVersion(version);
    uVersion=(version[0]<<4)|version[1]; /* major/minor version numbers */

    u_charAge(0x20, version);
    if(version[0]==0) {
        /* no additional properties available */
        log_err("TestAdditionalProperties: no additional properties available, not tested\n");
        return;
    }

    /* test u_charAge() */
    for(i=0; i<sizeof(charAges)/sizeof(charAges[0]); ++i) {
        u_charAge(charAges[i].c, version);
        if(0!=memcmp(version, charAges[i].version, sizeof(UVersionInfo))) {
            log_err("error: u_charAge(U+%04lx)={ %u, %u, %u, %u } instead of { %u, %u, %u, %u }\n",
                charAges[i].c,
                version[0], version[1], version[2], version[3],
                charAges[i].version[0], charAges[i].version[1], charAges[i].version[2], charAges[i].version[3]);
        }
    }

    if( u_getIntPropertyMinValue(UCHAR_DASH)!=0 ||
        u_getIntPropertyMinValue(UCHAR_BIDI_CLASS)!=0 ||
        u_getIntPropertyMinValue(UCHAR_BLOCK)!=(int32_t)UBLOCK_INVALID_CODE ||
        u_getIntPropertyMinValue(UCHAR_SCRIPT)!=(int32_t)USCRIPT_INVALID_CODE ||
        u_getIntPropertyMinValue(0x2345)!=0
    ) {
        log_err("error: u_getIntPropertyMinValue() wrong\n");
    }

    if( u_getIntPropertyMaxValue(UCHAR_DASH)!=1 ||
        u_getIntPropertyMaxValue(UCHAR_ID_CONTINUE)!=1 ||
        u_getIntPropertyMaxValue(UCHAR_BINARY_LIMIT-1)!=1 ||
        u_getIntPropertyMaxValue(UCHAR_BIDI_CLASS)!=(int32_t)U_CHAR_DIRECTION_COUNT-1 ||
        u_getIntPropertyMaxValue(UCHAR_BLOCK)!=(int32_t)UBLOCK_COUNT-1 ||
        u_getIntPropertyMaxValue(UCHAR_LINE_BREAK)!=(int32_t)U_LB_COUNT-1 ||
        u_getIntPropertyMaxValue(UCHAR_SCRIPT)!=(int32_t)USCRIPT_CODE_LIMIT-1 ||
        u_getIntPropertyMaxValue(0x2345)!=0
    ) {
        log_err("error: u_getIntPropertyMaxValue() wrong\n");
    }

    /* test u_hasBinaryProperty() and u_getIntPropertyValue() */
    for(i=0; i<sizeof(props)/sizeof(props[0]); ++i) {
        if(props[i][0]<0) {
            /* Unicode version break */
            if(uVersion<props[i][1]) {
                break; /* do not test properties that are not yet supported */
            } else {
                continue; /* skip this row */
            }
        }

        c=(UChar32)props[i][0];
        which=(UProperty)props[i][1];

        if(which<UCHAR_INT_START) {
            result=u_hasBinaryProperty(c, which);
            if(result!=props[i][2]) {
                log_err("error: u_hasBinaryProperty(U+%04lx, %d)=%d is wrong (props[%d])\n",
                        c, which, result, i);
            }
        }

        result=u_getIntPropertyValue(c, which);
        if(result!=props[i][2]) {
            log_err("error: u_getIntPropertyValue(U+%04lx, 0x1000+%d)=%d is wrong, should be %d (props[%d])\n",
                    c, (int32_t)which-0x1000, result, props[i][2], i);
        }

        /* test separate functions, too */
        switch((UProperty)props[i][1]) {
        case UCHAR_ALPHABETIC:
            if(u_isUAlphabetic((UChar32)props[i][0])!=(UBool)props[i][2]) {
                log_err("error: u_isUAlphabetic(U+%04lx)=%d is wrong (props[%d])\n",
                        props[i][0], result, i);
            }
            break;
        case UCHAR_LOWERCASE:
            if(u_isULowercase((UChar32)props[i][0])!=(UBool)props[i][2]) {
                log_err("error: u_isULowercase(U+%04lx)=%d is wrong (props[%d])\n",
                        props[i][0], result, i);
            }
            break;
        case UCHAR_UPPERCASE:
            if(u_isUUppercase((UChar32)props[i][0])!=(UBool)props[i][2]) {
                log_err("error: u_isUUppercase(U+%04lx)=%d is wrong (props[%d])\n",
                        props[i][0], result, i);
            }
            break;
        case UCHAR_WHITE_SPACE:
            if(u_isUWhiteSpace((UChar32)props[i][0])!=(UBool)props[i][2]) {
                log_err("error: u_isUWhiteSpace(U+%04lx)=%d is wrong (props[%d])\n",
                        props[i][0], result, i);
            }
            break;
        default:
            break;
        }
    }
}

static void
TestNumericProperties(void) {
    /* see UnicodeData.txt, DerivedNumericValues.txt */
    static const struct {
        UChar32 c;
        int32_t type;
        double numValue;
    } values[]={
        { 0x0F33, U_NT_NUMERIC, -1./2. },
        { 0x0C66, U_NT_DECIMAL, 0 },
        { 0x2159, U_NT_NUMERIC, 1./6. },
        { 0x00BD, U_NT_NUMERIC, 1./2. },
        { 0x0031, U_NT_DECIMAL, 1. },
        { 0x10320, U_NT_NUMERIC, 1. },
        { 0x0F2B, U_NT_NUMERIC, 3./2. },
        { 0x00B2, U_NT_DECIMAL, 2. },
        { 0x1813, U_NT_DECIMAL, 3. },
        { 0x2173, U_NT_NUMERIC, 4. },
        { 0x278E, U_NT_DIGIT, 5. },
        { 0x1D7F2, U_NT_DECIMAL, 6. },
        { 0x247A, U_NT_DIGIT, 7. },
        { 0x1372, U_NT_NUMERIC, 10. },
        { 0x216B, U_NT_NUMERIC, 12. },
        { 0x16EE, U_NT_NUMERIC, 17. },
        { 0x249A, U_NT_NUMERIC, 19. },
        { 0x303A, U_NT_NUMERIC, 30. },
        { 0x32B2, U_NT_NUMERIC, 37. },
        { 0x1375, U_NT_NUMERIC, 40. },
        { 0x10323, U_NT_NUMERIC, 50. },
        { 0x0BF1, U_NT_NUMERIC, 100. },
        { 0x217E, U_NT_NUMERIC, 500. },
        { 0x2180, U_NT_NUMERIC, 1000. },
        { 0x2181, U_NT_NUMERIC, 5000. },
        { 0x137C, U_NT_NUMERIC, 10000. },
        { 0x61, U_NT_NONE, U_NO_NUMERIC_VALUE },
        { 0x3000, U_NT_NONE, U_NO_NUMERIC_VALUE },
        { 0xfffe, U_NT_NONE, U_NO_NUMERIC_VALUE },
        { 0x10301, U_NT_NONE, U_NO_NUMERIC_VALUE },
        { 0xe0033, U_NT_NONE, U_NO_NUMERIC_VALUE },
        { 0x10ffff, U_NT_NONE, U_NO_NUMERIC_VALUE }
    };

    double nv;
    UChar32 c;
    int32_t i, type;

    for(i=0; i<LENGTHOF(values); ++i) {
        c=values[i].c;
        type=u_getIntPropertyValue(c, UCHAR_NUMERIC_TYPE);
        nv=u_getNumericValue(c);

        if(type!=values[i].type) {
            log_err("UCHAR_NUMERIC_TYPE(U+%04lx)=%d should be %d\n", c, type, values[i].type);
        }
        if(0.000001 <= fabs(nv - values[i].numValue)) {
            log_err("u_getNumericValue(U+%04lx)=%g should be %g\n", c, nv, values[i].numValue);
        }
    }
}
