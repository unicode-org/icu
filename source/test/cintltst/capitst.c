/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CAPITEST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda             Ported for C API
*********************************************************************************
*//* C API TEST For COLLATOR */
#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "ucolimp.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "capitst.h"
#include "unicode/ustring.h"
#include "unicode/ures.h"
#include "cmemory.h"



void addCollAPITest(TestNode** root)
{
    /* WEIVTODO: return tests here */
    addTest(root, &TestProperty,      "tscoll/capitst/TestProperty");
    addTest(root, &TestRuleBasedColl, "tscoll/capitst/TestRuleBasedColl");
    addTest(root, &TestCompare,       "tscoll/capitst/TestCompare");
    addTest(root, &TestSortKey,       "tscoll/capitst/TestSortKey");
    addTest(root, &TestHashCode,      "tscoll/capitst/TestHashCode");
    /*addTest(root, &TestElemIter,      "tscoll/capitst/TestElemIter");*/
    addTest(root, &TestGetAll,        "tscoll/capitst/TestGetAll");
    /*addTest(root, &TestGetDefaultRules, "tscoll/capitst/TestGetDefaultRules");*/
    addTest(root, &TestDecomposition, "tscoll/capitst/TestDecomposition");
    addTest(root, &TestSafeClone, "tscoll/capitst/TestSafeClone");
    
}

static void doAssert(int condition, const char *message)
{
    if (condition==0) {
        log_err("ERROR :  %s\n", message);
    }
}

#if 0
/* We don't have default rules, at least not in the previous sense */
void TestGetDefaultRules(){
    uint32_t size=0;
    UErrorCode status=U_ZERO_ERROR;
    UCollator *coll=NULL;
    int32_t len1 = 0, len2=0;
    uint8_t *binColData = NULL;

    UResourceBundle *res = NULL;
    UResourceBundle *binColl = NULL;
    uint8_t *binResult = NULL;
    
    
    const UChar * defaultRulesArray=ucol_getDefaultRulesArray(&size);
    log_verbose("Test the function ucol_getDefaultRulesArray()\n");

    coll = ucol_openRules(defaultRulesArray, size, UCOL_DECOMP_CAN, UCOL_PRIMARY, &status);
    ucol_setNormalization(coll, UCOL_DEFAULT_NORMALIZATION);
    if(U_SUCCESS(status) && coll !=NULL) {
        binColData = (uint8_t*)ucol_cloneRuleData(coll, &len1, &status);
        
    }

     
    status=U_ZERO_ERROR;
    res=ures_open(NULL, "root", &status);
    if(U_FAILURE(status)){
        log_err("ERROR: Failed to get resource for \"root Locale\" with %s", myErrorName(status));
        return;
    }
    binColl=ures_getByKey(res, "%%Collation", binColl, &status);  
    if(U_SUCCESS(status)){
        binResult=(uint8_t*)ures_getBinary(binColl,  &len2, &status);
        if(U_FAILURE(status)){
            log_err("ERROR: ures_getBinary() failed\n");
        }
    }else{
        log_err("ERROR: ures_getByKey(locale(default), %%Collation) failed");
    }


    if(len1 != len2){
        log_err("Error: ucol_getDefaultRulesArray() failed to return the correct length.\n");
    }
    if(memcmp(binColData, binResult, len1) != 0){
        log_err("Error: ucol_getDefaultRulesArray() failed\n");
    }

    uprv_free(binColData);
    ures_close(binColl);
    ures_close(res);
    ucol_close(coll);
  
}
#endif

/* Collator Properties
 ucol_open, ucol_strcoll,  getStrength/setStrength
 getDecomposition/setDecomposition, getDisplayName*/
void TestProperty()
{    
    UCollator *col;
    UChar *disName;
    int32_t len, i;
    UChar *source, *target;
    int32_t tempLength;
    UErrorCode status = U_ZERO_ERROR;
    /* In general, we don't want to hard-code data from resource files
       etc. into tests.  To make things somewhat flexible we encode
       a min and max version that seem reasonable at this time.  This
       still will have to be updated if we go beyond 32.128.207.207
    */
    UVersionInfo minVersionArray = {0x08, 0x40, 0x01, 0xc8};
    UVersionInfo maxVersionArray = {0x20, 0x80, 0xcf, 0xcf};
    UVersionInfo versionArray;
    
    log_verbose("The property tests begin : \n");
    log_verbose("Test ucol_strcoll : \n");
    col = ucol_open(NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Default Collator creation failed.: %s\n", myErrorName(status));
        return;
    }

    ucol_getVersion(col, versionArray);
    for (i=0; i<4; ++i) {
      if (versionArray[i] < minVersionArray[i] ||
          versionArray[i] > maxVersionArray[i]) {
        log_err("Testing ucol_getVersion() - unexpected result: %d.%d.%d.%d\n", 
            versionArray[0], versionArray[1], versionArray[2], versionArray[3]);
        break;
      }
    }
    source=(UChar*)malloc(sizeof(UChar) * 12);
    target=(UChar*)malloc(sizeof(UChar) * 12);
    

    u_uastrcpy(source, "ab");
    u_uastrcpy(target, "abc");
    
    doAssert((ucol_strcoll(col, source, u_strlen(source), target, u_strlen(target)) == UCOL_LESS), "ab < abc comparison failed");

    u_uastrcpy(source, "ab");
    u_uastrcpy(target, "AB");

    doAssert((ucol_strcoll(col, source, u_strlen(source), target, u_strlen(target)) == UCOL_LESS), "ab < AB comparison failed");
    u_uastrcpy(source, "black-bird");
    u_uastrcpy(target, "blackbird");
    doAssert((ucol_strcoll(col, source, u_strlen(source), target, u_strlen(target)) == UCOL_GREATER), 
        "black-bird > blackbird comparison failed");
    u_uastrcpy(source, "black bird");
    u_uastrcpy(target, "black-bird");
    doAssert((ucol_strcoll(col, source, u_strlen(source), target, u_strlen(target)) == UCOL_LESS), 
        "black bird < black-bird comparison failed");
    u_uastrcpy(source, "Hello");
    u_uastrcpy(target, "hello");

    doAssert((ucol_strcoll(col, source, u_strlen(source), target, u_strlen(target)) == UCOL_GREATER), 
        "Hello > hello comparison failed");
    free(source);
    free(target);
    log_verbose("Test ucol_strcoll ends.\n");

    log_verbose("testing ucol_getStrength() method ...\n");
    doAssert( (ucol_getStrength(col) == UCOL_TERTIARY), "collation object has the wrong strength");
    doAssert( (ucol_getStrength(col) != UCOL_PRIMARY), "collation object's strength is primary difference");
        
    log_verbose("testing ucol_setStrength() method ...\n");
    ucol_setStrength(col, UCOL_SECONDARY);
    doAssert( (ucol_getStrength(col) != UCOL_TERTIARY), "collation object's strength is secondary difference");
    doAssert( (ucol_getStrength(col) != UCOL_PRIMARY), "collation object's strength is primary difference");
    doAssert( (ucol_getStrength(col) == UCOL_SECONDARY), "collation object has the wrong strength");

    log_verbose("testing ucol_setDecomposition() method ...\n");
    ucol_setNormalization(col, UCOL_NO_NORMALIZATION);
    doAssert( (ucol_getNormalization(col) != UCOL_DECOMP_CAN_COMP_COMPAT), "collation object's normalization mode is Canonical decomposition followed by canonical composition");
    doAssert( (ucol_getNormalization(col) != UCOL_DECOMP_CAN), "collation object's normalization mode is canonical decomposition");
    doAssert( (ucol_getNormalization(col) == UCOL_NO_NORMALIZATION), "collation object has the wrong normalization mode");

    
    log_verbose("Get display name for the default collation in German : \n");

    len=ucol_getDisplayName("en_US", "de_DE", NULL, 0,  &status);
    if(status==U_BUFFER_OVERFLOW_ERROR){
        status=U_ZERO_ERROR;
        disName=(UChar*)malloc(sizeof(UChar) * (len+1));
        ucol_getDisplayName("en_US", "de_DE", disName, len+1,  &status);
        log_verbose("the display name for default collation in german: %s\n", austrdup(disName) );
        free(disName);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: in getDisplayName: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Default collation getDisplayName ended.\n");

    log_verbose("ucol_getRules() testing ...\n");
    ucol_getRules(col, &tempLength);
    doAssert( tempLength != 0, "getRules() result incorrect" );
    log_verbose("getRules tests end.\n");

    ucol_close(col);

    log_verbose("open an collator for french locale");
    col = ucol_open("fr_FR", &status);
    if (U_FAILURE(status)) {
       log_err("ERROR: Creating French collation failed.: %s\n", myErrorName(status));
        return;
    }
    ucol_setStrength(col, UCOL_PRIMARY);
    log_verbose("testing ucol_getStrength() method again ...\n");
    doAssert( (ucol_getStrength(col) != UCOL_TERTIARY), "collation object has the wrong strength");
    doAssert( (ucol_getStrength(col) == UCOL_PRIMARY), "collation object's strength is not primary difference");
        
    log_verbose("testing French ucol_setStrength() method ...\n");
    ucol_setStrength(col, UCOL_TERTIARY);
    doAssert( (ucol_getStrength(col) == UCOL_TERTIARY), "collation object's strength is not tertiary difference");
    doAssert( (ucol_getStrength(col) != UCOL_PRIMARY), "collation object's strength is primary difference");
    doAssert( (ucol_getStrength(col) != UCOL_SECONDARY), "collation object's strength is secondary difference");
    ucol_close(col);
    
    log_verbose("Get display name for the french collation in english : \n");
    len=ucol_getDisplayName("fr_FR", "en_US", NULL, 0,  &status);
    if(status==U_BUFFER_OVERFLOW_ERROR){
        status=U_ZERO_ERROR;
        disName=(UChar*)malloc(sizeof(UChar) * (len+1));
        ucol_getDisplayName("fr_FR", "en_US", disName, len+1,  &status);
        log_verbose("the display name for french collation in english: %s\n", austrdup(disName) );
        free(disName);
    }
    if(U_FAILURE(status)){
        log_err("ERROR: in getDisplayName: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Default collation getDisplayName ended.\n");
    

       
    

}
/* Test RuleBasedCollator and getRules*/
void TestRuleBasedColl()
{
    UCollator *col1, *col2, *col3, *col4;
    UChar ruleset1[60];
    UChar ruleset2[50];
    const UChar *rule1, *rule2, *rule3, *rule4;
    int32_t tempLength;
    UErrorCode status = U_ZERO_ERROR;
    
    u_uastrcpy(ruleset1, "< a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E");
    u_uastrcpy(ruleset2, "< a, A < b, B < c, C < d, D, e, E");
    
    col1 = ucol_openRules(ruleset1, u_strlen(ruleset1), UCOL_DEFAULT_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if (U_FAILURE(status)) {
        log_err("RuleBased Collator creation failed.: %s\n", myErrorName(status));
        return;
    }
    else
        log_verbose("PASS: RuleBased Collator creation passed\n");
    
    status = U_ZERO_ERROR;
    col2 = ucol_openRules(ruleset2, u_strlen(ruleset2),  UCOL_DEFAULT_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if (U_FAILURE(status)) {
        log_err("RuleBased Collator creation failed.: %s\n", myErrorName(status));
        return;
    }
    else
        log_verbose("PASS: RuleBased Collator creation passed\n");
    
    
    status = U_ZERO_ERROR;
    col3= ucol_open(NULL, &status);
    if (U_FAILURE(status)) {
        log_err("Default Collator creation failed.: %s\n", myErrorName(status));
        return;
    }
    else
        log_verbose("PASS: Default Collator creation passed\n");
    
    rule1 = ucol_getRules(col1, &tempLength);
    rule2 = ucol_getRules(col2, &tempLength);
    rule3 = ucol_getRules(col3, &tempLength);

    doAssert((u_strcmp(rule1, rule2) != 0), "Default collator getRules failed");
    doAssert((u_strcmp(rule2, rule3) != 0), "Default collator getRules failed");
    doAssert((u_strcmp(rule1, rule3) != 0), "Default collator getRules failed");
    
    col4=ucol_openRules(rule2, u_strlen(rule2), UCOL_DEFAULT_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if (U_FAILURE(status)) {
        log_err("RuleBased Collator creation failed.: %s\n", myErrorName(status));
        return;
    }
    rule4= ucol_getRules(col4, &tempLength);
    doAssert((u_strcmp(rule2, rule4) == 0), "Default collator getRules failed");

    ucol_close(col1);
    ucol_close(col2);
    ucol_close(col3);
    ucol_close(col4);
        
}

void TestCompare()
{
    UErrorCode status = U_ZERO_ERROR;
    UCollator *col;
    UChar* test1;
    UChar* test2;
    
    log_verbose("The compare tests begin : \n");
    status=U_ZERO_ERROR;
    col = ucol_open("en_US", &status);
    if(U_FAILURE(status)) {
        log_err("ucal_open() collation creation failed.: %s\n", myErrorName(status));
        return;
    }
    test1=(UChar*)malloc(sizeof(UChar) * 6);
    test2=(UChar*)malloc(sizeof(UChar) * 6);
    u_uastrcpy(test1, "Abcda");
    u_uastrcpy(test2, "abcda");
    
    log_verbose("Use tertiary comparison level testing ....\n");
                
    doAssert( (!ucol_equal(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" != \"abcda\" ");
    doAssert( (ucol_greater(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" >>> \"abcda\" ");
    doAssert( (ucol_greaterOrEqual(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" >>> \"abcda\""); 

    ucol_setStrength(col, UCOL_SECONDARY);
    log_verbose("Use secondary comparison level testing ....\n");
                
    doAssert( (ucol_equal(col, test1, u_strlen(test1), test2, u_strlen(test2) )), "Result should be \"Abcda\" == \"abcda\"");
    doAssert( (!ucol_greater(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" == \"abcda\"");
    doAssert( (ucol_greaterOrEqual(col, test1, u_strlen(test1), test2, u_strlen(test2) )), "Result should be \"Abcda\" == \"abcda\"");  

    ucol_setStrength(col, UCOL_PRIMARY);
    log_verbose("Use primary comparison level testing ....\n");
    
    doAssert( (ucol_equal(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" == \"abcda\"");
    doAssert( (!ucol_greater(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" == \"abcda\"");
    doAssert( (ucol_greaterOrEqual(col, test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"Abcda\" == \"abcda\"");  

      
    log_verbose("The compare tests end.\n");
    ucol_close(col);
    free(test1);
    free(test2);
   
}
/*
---------------------------------------------
 tests decomposition setting
*/
void TestDecomposition() {
    UErrorCode status = U_ZERO_ERROR;
    UCollator *en_US, *el_GR, *vi_VN;
    en_US = ucol_open("en_US", &status);
    el_GR = ucol_open("el_GR", &status);
    vi_VN = ucol_open("vi_VN", &status);

    if (U_FAILURE(status)) {
        log_err("ERROR: collation creation failed.: %s\n", myErrorName(status));
        return;
    }

    /* there is no reason to have canonical decomposition in en_US OR default locale */
    if(ucol_getNormalization(vi_VN) != UCOL_DECOMP_CAN)
      {
        log_err("ERROR: vi_VN collation did not have cannonical decomposition for normalization!\n");
      }

    if(ucol_getNormalization(el_GR) != UCOL_DECOMP_CAN)
      {
        log_err("ERROR: el_GR collation did not have cannonical decomposition for normalization!\n");
      }

    if(ucol_getNormalization(en_US) != UNORM_NONE)
      {
        log_err("ERROR: en_US collation had cannonical decomposition for normalization!\n");
      }

    ucol_close(en_US);
    ucol_close(el_GR);
    ucol_close(vi_VN);

}

#define CLONETEST_COLLATOR_COUNT 3

void TestSafeClone() {
    UChar* test1;
    UChar* test2;
    UCollator * someCollators [CLONETEST_COLLATOR_COUNT];
	UCollator * someClonedCollators [CLONETEST_COLLATOR_COUNT];
	UCollator * col;
    UErrorCode err = U_ZERO_ERROR;
	uint8_t buffer [CLONETEST_COLLATOR_COUNT] [U_COL_SAFECLONE_BUFFERSIZE];
	int32_t bufferSize = U_COL_SAFECLONE_BUFFERSIZE;
    int index;
    
    test1=(UChar*)malloc(sizeof(UChar) * 6);
    test2=(UChar*)malloc(sizeof(UChar) * 6);
    u_uastrcpy(test1, "abCda");
    u_uastrcpy(test2, "abcda");
    
    /* one default collator & two complex ones */
	someCollators[0] = ucol_open(NULL, &err);
	someCollators[1] = ucol_open("ko", &err);
	someCollators[2] = ucol_open("ja_JP", &err);

	/* Check the various error & informational states: */

	/* Null status - just returns NULL */
	if (0 != ucol_safeClone(someCollators[0], buffer[0], &bufferSize, 0))
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with null status\n");
	}
	/* error status - should return 0 & keep error the same */
	err = U_MEMORY_ALLOCATION_ERROR;
	if (0 != ucol_safeClone(someCollators[0], buffer[0], &bufferSize, &err) || err != U_MEMORY_ALLOCATION_ERROR)
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with incoming error status\n");
	}
	err = U_ZERO_ERROR;

	/* Null buffer size pointer - just returns NULL & set error to U_ILLEGAL_ARGUMENT_ERROR*/
	if (0 != ucol_safeClone(someCollators[0], buffer[0], 0, &err) || err != U_ILLEGAL_ARGUMENT_ERROR)
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with null bufferSize pointer\n");
	}
	err = U_ZERO_ERROR;
	
	/* buffer size pointer is 0 - fill in pbufferSize with a size */
	bufferSize = 0;
	if (0 != ucol_safeClone(someCollators[0], buffer[0], &bufferSize, &err) || U_FAILURE(err) || bufferSize <= 0)
	{
		log_err("FAIL: Cloned Collator failed a sizing request ('preflighting')\n");
	}
	/* Verify our define is large enough  */
	if (U_COL_SAFECLONE_BUFFERSIZE < bufferSize)
	{
		log_err("FAIL: Pre-calculated buffer size is too small\n");
	}
	/* Verify we can use this run-time calculated size */
	if (0 == (col = ucol_safeClone(someCollators[0], buffer[0], &bufferSize, &err)) || U_FAILURE(err))
	{
		log_err("FAIL: Collator can't be cloned with run-time size\n");
	}
	if (col) ucol_close(col);
	/* size one byte too small - should allocate & let us know */
	--bufferSize;
	if (0 == (col = ucol_safeClone(someCollators[0], 0, &bufferSize, &err)) || err != U_SAFECLONE_ALLOCATED_ERROR)
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with too-small buffer size\n");
	}
	if (col) ucol_close(col);
	err = U_ZERO_ERROR;
	bufferSize = U_COL_SAFECLONE_BUFFERSIZE;


	/* Null buffer pointer - return Collator & set error to U_SAFECLONE_ALLOCATED_ERROR */
	if (0 == (col = ucol_safeClone(someCollators[0], 0, &bufferSize, &err)) || err != U_SAFECLONE_ALLOCATED_ERROR)
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with null buffer pointer\n");
	}
	if (col) ucol_close(col);
	err = U_ZERO_ERROR;

	/* Null Collator - return NULL & set U_ILLEGAL_ARGUMENT_ERROR */
	if (0 != ucol_safeClone(0, buffer[0], &bufferSize, &err) || err != U_ILLEGAL_ARGUMENT_ERROR)
	{
		log_err("FAIL: Cloned Collator failed to deal correctly with null Collator pointer\n");
	}

	err = U_ZERO_ERROR;
	

	/* change orig & clone & make sure they are independent */

	for (index = 0; index < CLONETEST_COLLATOR_COUNT; index++)
	{
		bufferSize = U_COL_SAFECLONE_BUFFERSIZE;
		someClonedCollators[index] = ucol_safeClone(someCollators[index], buffer[index], &bufferSize, &err);

        ucol_setStrength(someClonedCollators[index], UCOL_TERTIARY);
        ucol_setStrength(someCollators[index], UCOL_PRIMARY);
        
        doAssert( (ucol_greater(someClonedCollators[index], test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"abCda\" >>> \"abcda\" ");
        doAssert( (ucol_equal(someCollators[index], test1, u_strlen(test1), test2, u_strlen(test2))), "Result should be \"abcda\" == \"abCda\"");
        
        ucol_close(someClonedCollators[index]);
		ucol_close(someCollators[index]);
	}
}

/*
----------------------------------------------------------------------------
 ctor -- Tests the getSortKey
*/
void TestSortKey()
{   
    uint8_t *sortk1 = NULL, *sortk2 = NULL, *sortk3 = NULL;
    uint8_t sortk2_compat[] = { 
        0x02, 0x54, 0x02, 0x55, 0x02, 0x56, 0x02, 0x57, 0x02, 0x54, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02, 0x00
        /* this is a 1.6 sortkey */
      /*0x00, 0x53, 0x00, 0x54, 0x00, 0x55, 0x00, 0x56, 0x00, 0x53, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00*/
    };

    int32_t sortklen, osortklen;
    UCollator *col;
    UChar *test1, *test2, *test3;
    UErrorCode status = U_ZERO_ERROR;


    uint8_t s1[] = { 0x9f, 0x00 };
    uint8_t s2[] = { 0x61, 0x00 };
    int  strcmpResult;

    strcmpResult = strcmp((const char *)s1, (const char *)s2);
    log_verbose("strcmp(0x9f..., 0x61...) = %d\n", strcmpResult);
    
    if(strcmpResult <= 0) {
      log_err("ERR: expected strcmp(\"9f 00\", \"61 00\") to be >=0 (GREATER).. got %d. Calling strcmp() for sortkeys may not work! \n",
              strcmpResult);
    }


    log_verbose("testing SortKey begins...\n");
    /* this is supposed to open default date format, but later on it treats it like it is "en_US" 
       - very bad if you try to run the tests on machine where default locale is NOT "en_US" */
    /* col = ucol_open(NULL, &status); */
    col = ucol_open("en_US", &status);
    if (U_FAILURE(status)) {
        log_err("ERROR: Default collation creation failed.: %s\n", myErrorName(status));
        return;
    }


    if(ucol_getStrength(col) != UCOL_DEFAULT_STRENGTH)
      {
        log_err("ERROR: default collation did not have UCOL_DEFAULT_STRENGTH !\n");
      }

    test1=(UChar*)malloc(sizeof(UChar) * 6);
    test2=(UChar*)malloc(sizeof(UChar) * 6);
    test3=(UChar*)malloc(sizeof(UChar) * 6);
    
    memset(test1,0xFE, sizeof(UChar)*6);
    memset(test2,0xFE, sizeof(UChar)*6);
    memset(test3,0xFE, sizeof(UChar)*6);


    u_uastrcpy(test1, "Abcda");
    u_uastrcpy(test2, "abcda");
    u_uastrcpy(test3, "abcda");

    log_verbose("Use tertiary comparison level testing ....\n");

    sortklen=ucol_getSortKey(col, test1, u_strlen(test1),  NULL, 0);
    sortk1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    memset(sortk1,0xFE, sortklen);
    ucol_getSortKey(col, test1, u_strlen(test1), sortk1, sortklen+1);

    sortklen=ucol_getSortKey(col, test2, u_strlen(test2),  NULL, 0);
    sortk2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    memset(sortk2,0xFE, sortklen);
    ucol_getSortKey(col, test2, u_strlen(test2), sortk2, sortklen+1);

    osortklen = sortklen;
    sortklen=ucol_getSortKey(col, test2, u_strlen(test3),  NULL, 0);
    sortk3=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    memset(sortk3,0xFE, sortklen);
    ucol_getSortKey(col, test2, u_strlen(test2), sortk3, sortklen+1);

    doAssert( (sortklen == osortklen), "Sortkey length should be the same (abcda, abcda)");

    doAssert( (memcmp(sortk1, sortk2, sortklen) > 0), "Result should be \"Abcda\" > \"abcda\"");
    doAssert( (memcmp(sortk2, sortk1, sortklen) < 0), "Result should be \"abcda\" < \"Abcda\"");
    doAssert( (memcmp(sortk2, sortk3, sortklen) == 0), "Result should be \"abcda\" ==  \"abcda\"");

    doAssert( (memcmp(sortk2, sortk2_compat, sortklen) == 0), "Binary format for 'abcda' sortkey different!");

#if 1 /* verobse log of sortkeys */
    {
      char junk2[1000];
      char junk3[1000];
      int i;

      strcpy(junk2, "abcda[2] ");
      strcpy(junk3, " abcda[3] ");

      for(i=0;i<sortklen;i++)
        {
          sprintf(junk2+strlen(junk2), "%02X ",(int)( 0xFF & sortk2[i]));
          sprintf(junk3+strlen(junk3), "%02X ",(int)( 0xFF & sortk3[i]));
        }
      
      log_verbose("%s\n", junk2);
      log_verbose("%s\n", junk3);
    }
#endif

    free(sortk1);
    free(sortk2);
    free(sortk3);

    log_verbose("Use secondary comparision level testing ...\n");
    ucol_setStrength(col, UCOL_SECONDARY);
    sortklen=ucol_getSortKey(col, test1, u_strlen(test1),  NULL, 0);
    sortk1=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(col, test1, u_strlen(test1), sortk1, sortklen+1);
    sortklen=ucol_getSortKey(col, test2, u_strlen(test2),  NULL, 0);
    sortk2=(uint8_t*)malloc(sizeof(uint8_t) * (sortklen+1));
    ucol_getSortKey(col, test2, u_strlen(test2), sortk2, sortklen+1);
    
    doAssert( !(memcmp(sortk1, sortk2, sortklen) > 0), "Result should be \"Abcda\" == \"abcda\"");
    doAssert( !(memcmp(sortk2, sortk1, sortklen) < 0), "Result should be \"abcda\" == \"Abcda\"");
    doAssert( (memcmp(sortk1, sortk2, sortklen) == 0), "Result should be \"abcda\" ==  \"abcda\"");
    
 
    log_verbose("testing sortkey ends...\n");
    ucol_close(col);
    free(test1);
    free(test2);
    free(test3);
    free(sortk1);
    free(sortk2);
     
}
void TestHashCode()
{
    uint8_t *sortk1, *sortk2, *sortk3;
    int32_t sortk1len, sortk2len, sortk3len;
    UCollator *col;
    UChar *test1, *test2, *test3;
    UErrorCode status = U_ZERO_ERROR;
    log_verbose("testing getHashCode begins...\n");
    col = ucol_open("en_US", &status);
    if (U_FAILURE(status)) {
        log_err("ERROR: Default collation creation failed.: %s\n", myErrorName(status));
        return;
    }
    test1=(UChar*)malloc(sizeof(UChar) * 6);
    test2=(UChar*)malloc(sizeof(UChar) * 6);
    test3=(UChar*)malloc(sizeof(UChar) * 6);
    u_uastrcpy(test1, "Abcda");
    u_uastrcpy(test2, "abcda");
    u_uastrcpy(test3, "abcda");

    log_verbose("Use tertiary comparison level testing ....\n");
    sortk1len=ucol_getSortKey(col, test1, u_strlen(test1),  NULL, 0);
    sortk1=(uint8_t*)malloc(sizeof(uint8_t) * (sortk1len+1));
    ucol_getSortKey(col, test1, u_strlen(test1), sortk1, sortk1len+1);
    sortk2len=ucol_getSortKey(col, test2, u_strlen(test2),  NULL, 0);
    sortk2=(uint8_t*)malloc(sizeof(uint8_t) * (sortk2len+1));
    ucol_getSortKey(col, test2, u_strlen(test2), sortk2, sortk2len+1);
    sortk3len=ucol_getSortKey(col, test2, u_strlen(test3),  NULL, 0);
    sortk3=(uint8_t*)malloc(sizeof(uint8_t) * (sortk3len+1));
    ucol_getSortKey(col, test2, u_strlen(test2), sortk3, sortk3len+1);
        
    
    log_verbose("ucol_hashCode() testing ...\n");
    
    doAssert( ucol_keyHashCode(sortk1, sortk1len) != ucol_keyHashCode(sortk2, sortk2len), "Hash test1 result incorrect" );               
    doAssert( !(ucol_keyHashCode(sortk1, sortk1len) == ucol_keyHashCode(sortk2, sortk2len)), "Hash test2 result incorrect" );
    doAssert( ucol_keyHashCode(sortk2, sortk2len) == ucol_keyHashCode(sortk3, sortk3len), "Hash result not equal" );
    
    log_verbose("hashCode tests end.\n");
    ucol_close(col);
    free(sortk1);
    free(sortk2);
    free(sortk3);
    free(test1);
    free(test2);
    free(test3);


}
/*
 *----------------------------------------------------------------------------
 * Tests the UCollatorElements API.
 * 
 */ 
void TestElemIter()
{
    UTextOffset offset;
    int32_t order1, order2, order3;
    UChar *testString1, *testString2;
    UCollator *col;
    UCollationElements *iterator1, *iterator2, *iterator3;
    UErrorCode status = U_ZERO_ERROR;
    log_verbose("testing UCollatorElements begins...\n");
    col = ucol_open(NULL, &status);
    ucol_setNormalization(col, UCOL_NO_NORMALIZATION);
    if (U_FAILURE(status)) {
        log_err("ERROR: Default collation creation failed.: %s\n", myErrorName(status));
        return;
    }

    testString1=(UChar*)malloc(sizeof(UChar) * 150);
    testString2=(UChar*)malloc(sizeof(UChar) * 150);
    u_uastrcpy(testString1, "XFILE What subset of all possible test cases has the highest probability of detecting the most errors?");
    u_uastrcpy(testString2, "Xf ile What subset of all possible test cases has the lowest probability of detecting the least errors?");
    
    log_verbose("Constructors and comparison testing....\n");
    
    iterator1 = ucol_openElements(col, testString1, u_strlen(testString1), &status);
    if(U_FAILURE(status)) {
        log_err("ERROR: Default collationElement iterator creation failed.: %s\n", myErrorName(status));
        ucol_close(col);
        return;
    }
    else{ log_verbose("PASS: Default collationElement iterator1 creation passed\n");}

    iterator2 = ucol_openElements(col, testString1, u_strlen(testString1), &status);
    if(U_FAILURE(status)) {
        log_err("ERROR: Default collationElement iterator creation failed.: %s\n", myErrorName(status));
        ucol_close(col);
        return;
    }
    else{ log_verbose("PASS: Default collationElement iterator2 creation passed\n");}

    iterator3 = ucol_openElements(col, testString2, u_strlen(testString2), &status);
    if(U_FAILURE(status)) {
        log_err("ERROR: Default collationElement iterator creation failed.: %s\n", myErrorName(status));
        ucol_close(col);
        return;
    }
    else{ log_verbose("PASS: Default collationElement iterator3 creation passed\n");}

    offset=ucol_getOffset(iterator1);
    ucol_setOffset(iterator1, 6, &status);
    if (U_FAILURE(status)) {
        log_err("Error in setOffset for UCollatorElements iterator.: %s\n", myErrorName(status));
        return;
    }
    if(ucol_getOffset(iterator1)==6)
        log_verbose("setOffset and getOffset working fine\n");
    else{
        log_err("error in set and get Offset got %d instead of 6\n", ucol_getOffset(iterator1));
    }

    ucol_setOffset(iterator1, 0, &status);
    order1 = ucol_next(iterator1, &status);
    if (U_FAILURE(status)) {
        log_err("Somehow ran out of memory stepping through the iterator1.: %s\n", myErrorName(status));
        return;
    }
    order2=ucol_getOffset(iterator2);
    doAssert((order1 != order2), "The first iterator advance failed");
    order2 = ucol_next(iterator2, &status);
    if (U_FAILURE(status)) {
        log_err("Somehow ran out of memory stepping through the iterator2.: %s\n", myErrorName(status));
        return;
    }
    order3 = ucol_next(iterator3, &status);
    if (U_FAILURE(status)) {
        log_err("Somehow ran out of memory stepping through the iterator3.: %s\n", myErrorName(status));
        return;
    }
    
    doAssert((order1 == order2), "The second iterator advance failed should be the same as first one");
    
    doAssert( ((order1 & UCOL_PRIMARYMASK) == (order3 & UCOL_PRIMARYMASK)), "The primary orders should be identical");
    doAssert( ((order1 & UCOL_SECONDARYMASK) == (order3 & UCOL_SECONDARYMASK)), "The secondary orders should be identical");
    doAssert( ((order1 & UCOL_TERTIARYMASK) == (order3 & UCOL_TERTIARYMASK)), "The tertiary orders should be identical");
    
    order1=ucol_next(iterator1, &status);
    order3=ucol_next(iterator3, &status);
    doAssert( ((order1 & UCOL_PRIMARYMASK) == (order3 & UCOL_PRIMARYMASK)), "The primary orders should be identical");
    doAssert( ((order1 & UCOL_TERTIARYMASK) != (order3 & UCOL_TERTIARYMASK)), "The tertiary orders should be different");
    
    order1=ucol_next(iterator1, &status);
    order3=ucol_next(iterator3, &status);
    doAssert( ((order1 & UCOL_SECONDARYMASK) != (order3 & UCOL_SECONDARYMASK)), "The secondary orders should be different");
    doAssert( (order1 != UCOL_NULLORDER), "Unexpected end of iterator reached");

    ucol_reset(iterator1);
    ucol_reset(iterator2);
    ucol_reset(iterator3);

    free(testString1);
    free(testString2);
    ucol_closeElements(iterator1);
    ucol_closeElements(iterator2);
    ucol_closeElements(iterator3);
    ucol_close(col);
    
    log_verbose("testing CollationElementIterator ends...\n");
}

void TestGetAll()
{
    int32_t i, count;
    count=ucol_countAvailable();
    /* use something sensible w/o hardcoding the count */
    if(count < 0){
        log_err("Error in countAvailable(), it returned %d\n", count);
    }
    else{
        log_verbose("PASS: countAvailable() successful, it returned %d\n", count);
    }
    for(i=0;i<count;i++)
        log_verbose("%s\n", ucol_getAvailable(i));


}
