/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CRESTST.C
*
* Modification History:
*        Name                    Date               Description
*     Madhu Katragadda           05/09/2000         Ported Tests for New ResourceBundle API
*********************************************************************************
*/


#include "unicode/utypes.h"
#include "cintltst.h"
#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "string.h"
#include "cstring.h"
#include <time.h>

#define RESTEST_HEAP_CHECK 0

#include "unicode/uloc.h"
#include "unicode/ures.h"
#include "creststn.h"
#include "unicode/ctest.h"
#include <stdio.h>

/*****************************************************************************/
/**
 * Return a random unsigned long l where 0N <= l <= ULONG_MAX.
 */

static uint32_t
randul()
{
	uint32_t l;
	int32_t i;
    static bool_t initialized = FALSE;
    if (!initialized)
    {
        srand((unsigned)time(NULL));
        initialized = TRUE;
    }
    /* Assume rand has at least 12 bits of precision */
    
    for (i=0; i<sizeof(l); ++i) ((char*)&l)[i] = (rand() & 0x0FF0) >> 4;
    return l;
}

/**
 * Return a random double x where 0.0 <= x < 1.0.
 */
static double
randd()
{
    return ((double)randul()) / ULONG_MAX;
}

/**
 * Return a random integer i where 0 <= i < n.
 */
static int32_t randi(int32_t n)
{
    return (int32_t)(randd() * n);
}
/***************************************************************************************/
/**
 * Convert an integer, positive or negative, to a character string radix 10.
 */
static char*
itoa1(int32_t i, char* buf)
{
  char *p = 0;
  char* result = buf;
  /* Handle negative */
  if(i < 0) {
    *buf++ = '-';
    i = -i;
  }

  /* Output digits in reverse order */
  p = buf;
  do {
    *p++ = '0' + (i % 10);
    i /= 10;
  }
  while(i);
  *p-- = 0;

  /* Reverse the string */
  while(buf < p) {
    char c = *buf;
    *buf++ = *p;
    *p-- = c;
  }

  return result;
}
static const int32_t kERROR_COUNT = -1234567;
static const UChar kERROR[] = { 0x0045 /*E*/, 0x0052 /*'R'*/, 0x0052 /*'R'*/,
             0x004F /*'O'*/, 0x0052/*'R'*/, 0x0000 /*'\0'*/};

/*****************************************************************************/

enum E_Where
{
  e_Root,
  e_te,
  e_te_IN,
  e_Where_count
};
typedef enum E_Where E_Where;
/*****************************************************************************/

#define CONFIRM_EQ(actual,expected) if (u_strcmp(expected,actual)==0){ record_pass(); } else { record_fail(); log_err("%s  returned  %s  instead of %s\n", action, austrdup(actual), austrdup(expected)); pass=FALSE; }
#define CONFIRM_INT_EQ(actual,expected) if ((expected)==(actual)) { record_pass(); } else { record_fail(); log_err("%s returned %d instead of %d\n",  action, actual, expected); pass=FALSE; }
#define CONFIRM_INT_GE(actual,expected) if ((actual)>=(expected)) { record_pass(); } else { record_fail(); log_err("%s returned %d instead of x >= %d\n",  action, actual, expected); pass=FALSE; }
#define CONFIRM_INT_NE(actual,expected) if ((expected)!=(actual)) { record_pass(); } else { record_fail(); log_err("%s returned %d instead of x != %d\n",  action, actual, expected); pass=FALSE; }
#define CONFIRM_ErrorCode(actual,expected) if ((expected)==(actual)) { record_pass(); } else { record_fail();  log_err("%s returned  %s  instead of %s\n", action, myErrorName(actual), myErrorName(expected)); pass=FALSE; }


/* Array of our test objects */

static struct
{
  const char* name;
  UErrorCode expected_constructor_status;
  E_Where where;
  bool_t like[e_Where_count];
  bool_t inherits[e_Where_count];
}
param[] =
{
  /* "te" means test */
  /* "IN" means inherits */
  /* "NE" or "ne" means "does not exist" */

  { "root",                U_ZERO_ERROR,             e_Root,      { TRUE, FALSE, FALSE }, { TRUE, FALSE, FALSE } },
  { "te",                  U_ZERO_ERROR,             e_te,        { FALSE, TRUE, FALSE }, { TRUE, TRUE, FALSE  } },
  { "te_IN",               U_ZERO_ERROR,             e_te_IN,     { FALSE, FALSE, TRUE }, { TRUE, TRUE, TRUE   } },
  { "te_NE",               U_USING_FALLBACK_ERROR,   e_te,        { FALSE, TRUE, FALSE }, { TRUE, TRUE, FALSE  } },
  { "te_IN_NE",            U_USING_FALLBACK_ERROR,   e_te_IN,     { FALSE, FALSE, TRUE }, { TRUE, TRUE, TRUE   } },
  { "ne",                  U_USING_DEFAULT_ERROR,    e_Root,      { TRUE, FALSE, FALSE }, { TRUE, FALSE, FALSE } }
};

static int32_t bundles_count = sizeof(param) / sizeof(param[0]);


static void printUChars(UChar*);
/***************************************************************************************/

/* Array of our test objects */

void addNEWResourceBundleTest(TestNode** root)
{
  addTest(root, &TestConstruction1,   "tsutil/creststn/TestConstruction1");
  addTest(root, &TestConstruction2,   "tsutil/creststn/TestConstruction2");
  addTest(root, &TestResourceBundles, "tsutil/creststn/TestResourceBundle");
  addTest(root, &TestFallback,        "tsutil/creststn/TestFallback");
  addTest(root, &TestAliasConflict,   "tsutil/creststn/TestAlias");

}


/***************************************************************************************/
void TestAliasConflict(void) {
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *he = NULL;
    UResourceBundle *iw = NULL;
    const UChar *result = NULL;
    
    he = ures_open(NULL, "he", &status);
    iw = ures_open(NULL, "iw", &status);
    if(U_FAILURE(status)) { 
        log_err("Failed to get resource with %s", myErrorName(status));
    }
    ures_close(iw);
    result = ures_get(he, "ShortLanguage", &status);
    if(U_FAILURE(status) || result == NULL) { 
        log_err("Failed to get resource with %s", myErrorName(status));
    }
    ures_close(he);
}


void TestResourceBundles()
{

  testTag("only_in_Root", TRUE, FALSE, FALSE);
  testTag("in_Root_te", TRUE, TRUE, FALSE);
  testTag("in_Root_te_te_IN", TRUE, TRUE, TRUE);
  testTag("in_Root_te_IN", TRUE, FALSE, TRUE);
  testTag("only_in_te", FALSE, TRUE, FALSE);
  testTag("only_in_te_IN", FALSE, FALSE, TRUE);
  testTag("in_te_te_IN", FALSE, TRUE, TRUE);
  testTag("nonexistent", FALSE, FALSE, FALSE);

  log_verbose("Passed:=  %d   Failed=   %d \n", pass, fail);

}


void TestConstruction1()
{
  UResourceBundle *test1 = 0, *test2 = 0;
  const UChar *result1, *result2;
  UErrorCode status= U_ZERO_ERROR;
  UErrorCode   err = U_ZERO_ERROR;
  const char*        directory=NULL;
  const char*      locale="te_IN";
  char testdatapath[256];
  int32_t len1=0;
  int32_t len2=0;

  U_STRING_DECL(rootVal, "ROOT", 4);
  U_STRING_DECL(te_inVal, "TE_IN", 5);

  U_STRING_INIT(rootVal, "ROOT", 4);
  U_STRING_INIT(te_inVal, "TE_IN", 5);


  directory= ctest_getTestDirectory();
  uprv_strcpy(testdatapath, directory);
  uprv_strcat(testdatapath, "testdata");
  log_verbose("Testing ures_open()......\n");

  test1=ures_open(testdatapath, NULL, &err);
  test2=ures_open(testdatapath, locale, &err);

  if(U_FAILURE(err))
    {
      log_err("construction did not succeed :  %s \n", myErrorName(status));
      return;
    }

  result1= ures_getStringByKey(test1, "string_in_Root_te_te_IN", &len1, &err);
  result2= ures_getStringByKey(test2, "string_in_Root_te_te_IN", &len2, &err);
  if (U_FAILURE(err) || len1==0 || len2==0) {
    log_err("Something threw an error in TestConstruction(): %s\n", myErrorName(status));
    return;
  }
  log_verbose("for string_in_Root_te_te_IN, default.txt had  %s\n", austrdup(result1));
  log_verbose("for string_in_Root_te_te_IN, te_IN.txt had %s\n", austrdup(result2));
  if(u_strcmp(result1, rootVal) !=0  || u_strcmp(result2, te_inVal) !=0 ){
	  log_err("construction test failed. Run Verbose for more information");
  }


  /* Test getVersionNumber*/
  log_verbose("Testing version number\n");
  log_verbose("for getVersionNumber :  %s\n", ures_getVersionNumber(test1));

  ures_close(test1);
  ures_close(test2);
}

void TestConstruction2()
{
  
  UChar temp[7];
  UResourceBundle *test4 = 0;
  const UChar*   result4;
  UErrorCode   err = U_ZERO_ERROR;
  const char*     directory;
  const char*    locale="te_IN";
  wchar_t widedirectory[256];
  char testdatapath[256];
  int32_t len=0;

  directory= ctest_getTestDirectory();
  uprv_strcpy(testdatapath, directory);
  uprv_strcat(testdatapath, "testdata");
  mbstowcs(widedirectory, testdatapath, 256);

  log_verbose("Testing ures_openW().......\n");

  test4=ures_openW(widedirectory, locale, &err);
  if(U_FAILURE(err)){
    log_err("Error in the construction using ures_openW():  %s\n", myErrorName(err));
    return;
  }

  result4=ures_getStringByKey(test4, "string_in_Root_te_te_IN", &len, &err);
  if (U_FAILURE(err) || len==0) {
    log_err("Something threw an error in TestConstruction()  %s\n", myErrorName(err));
    return;
  }

  log_verbose("for string_in_Root_te_te_IN, te_IN.txt had  %s\n", austrdup(result4));
  u_uastrcpy(temp, "TE_IN");

  if(u_strcmp(result4, temp)!=0)
  {

    log_err("Construction test failed for ures_openW();\n");
    if(!VERBOSITY)
         log_info("(run verbose for more information)\n");

      log_verbose("\nGot->");
      printUChars((UChar*)result4);
	  log_verbose(" Want->");
      printUChars(temp);
	  log_verbose("\n");
  }
     
  
  ures_close(test4);
}

/*****************************************************************************/
/*****************************************************************************/

bool_t testTag(const char* frag,
           bool_t in_Root,
           bool_t in_te,
           bool_t in_te_IN)
{
  bool_t pass=TRUE;

  /* Make array from input params */

  bool_t is_in[3];
  const char *NAME[] = { "ROOT", "TE", "TE_IN" };

  /* Now try to load the desired items */
  UResourceBundle* theBundle = NULL;
  char tag[99];
  char action[256];
  UErrorCode expected_status,status = U_ZERO_ERROR,expected_resource_status = U_ZERO_ERROR;
  UChar* base = NULL;
  UChar* expected_string = NULL;
  const UChar* string = NULL;
  char buf[5];
  char item_tag[10];
  int32_t i,j,row,col, len;
  int32_t actual_bundle;
  int32_t count = 0;
  int32_t row_count=0;
  int32_t column_count=0;
  int32_t index = 0;
  int32_t tag_count= 0;
  char testdatapath[256];
  UResourceBundle* array=NULL;
  UResourceBundle* array2d=NULL;
  UResourceBundle* tags=NULL;
  UResourceBundle* arrayItem1=NULL;

  const char*    directory =  ctest_getTestDirectory();

  uprv_strcpy(testdatapath, directory);
  uprv_strcat(testdatapath, "testdata");

  is_in[0] = in_Root;
  is_in[1] = in_te;
  is_in[2] = in_te_IN;

  strcpy(item_tag, "tag");

  for (i=0; i<bundles_count; ++i)
    {
      strcpy(action,"construction for");
      strcat(action, param[i].name);


      status = U_ZERO_ERROR;

      theBundle = ures_open(testdatapath, param[i].name, &status);
      CONFIRM_ErrorCode(status,param[i].expected_constructor_status);

      if(i == 5)
	actual_bundle = 0; /* ne -> default */
      else if(i == 3)
	actual_bundle = 1; /* te_NE -> te */
      else if(i == 4)
	actual_bundle = 2; /* te_IN_NE -> te_IN */
      else
	actual_bundle = i;

      expected_resource_status = U_MISSING_RESOURCE_ERROR;
      for (j=e_te_IN; j>=e_Root; --j)
        {
	  if (is_in[j] && param[i].inherits[j])
            {
	      
	      if(j == actual_bundle) /* it's in the same bundle OR it's a nonexistent=default bundle (5) */
		expected_resource_status = U_ZERO_ERROR;
	      else if(j == 0)
		expected_resource_status = U_USING_DEFAULT_ERROR;
	      else
		expected_resource_status = U_USING_FALLBACK_ERROR;

	      log_verbose("%s[%d]::%s: in<%d:%s> inherits<%d:%s>.  actual_bundle=%s\n",
			  param[i].name, 
			  i,
			  frag,
			  j,
			  is_in[j]?"Yes":"No",
			  j,
			  param[i].inherits[j]?"Yes":"No",
			  param[actual_bundle].name);

	      break;
            }
        }

      for (j=param[i].where; j>=0; --j)
        {
      if (is_in[j])
        {
          base=(UChar*)malloc(sizeof(UChar)*(strlen(NAME[j]) + 1));
          u_uastrcpy(base,NAME[j]);

          break;
            }
      else {
        base = (UChar*) malloc(sizeof(UChar) * 1);
        *base = 0x0000;
      }
        }

      /*----string---------------------------------------------------------------- */
      
      strcpy(tag,"string_");
      strcat(tag,frag);

      strcpy(action,param[i].name);
      strcat(action, ".ures_getStringByKey(" );
      strcat(action,tag);
      strcat(action, ")");

    
      status = U_ZERO_ERROR;
	  len=0;

      string=ures_getStringByKey(theBundle, tag, &len, &status);
      if(U_SUCCESS(status)) {
		  expected_string=(UChar*)malloc(sizeof(UChar)*(u_strlen(base) + 4));
	      u_strcpy(expected_string,base);
		  CONFIRM_INT_EQ(len, u_strlen(expected_string));
	  }else{
          expected_string = (UChar*)malloc(sizeof(UChar)*(u_strlen(kERROR) + 1));
          u_strcpy(expected_string,kERROR);
		  string=kERROR;
	  }
	  log_verbose("%s got %d, expected %d\n", action, status, expected_resource_status);
	 
      CONFIRM_ErrorCode(status, expected_resource_status);
      CONFIRM_EQ(string, expected_string);
      
 

      /*--------------array------------------------------------------------- */
	  
      strcpy(tag,"array_");
      strcat(tag,frag);

      strcpy(action,param[i].name);
      strcat(action, ".ures_getByKey(" );
      strcat(action,tag);
      strcat(action, ")");

	  len=0;

      count = kERROR_COUNT;
      status = U_ZERO_ERROR;
      array=ures_getByKey(theBundle, tag, array, &status);
	  CONFIRM_ErrorCode(status,expected_resource_status);
	  if (U_SUCCESS(status)) {
	    /*confirm the resource type is an array*/
		    CONFIRM_INT_EQ(ures_getType(array), RES_ARRAY);
		    /*confirm the size*/
			count=ures_getSize(array);
            CONFIRM_INT_GE(count,1);
            for (j=0; j<count; ++j) {
				UChar element[3];
                u_strcpy(expected_string, base);
                u_uastrcpy(element, itoa1(j,buf));
				u_strcat(expected_string, element);
				arrayItem1=ures_getNextResource(array, arrayItem1, &status);
				if(U_SUCCESS(status)){
					CONFIRM_EQ(ures_getString(arrayItem1, &len, &status),expected_string);
				}
            }
		
      }
      else {
            CONFIRM_INT_EQ(count,kERROR_COUNT);
			CONFIRM_INT_EQ((int32_t)(unsigned long)array,(int32_t)0);
            count = 0;
      }
       
      /*--------------arrayItem------------------------------------------------- */
   
      strcpy(tag,"array_");
      strcat(tag,frag);

      strcpy(action,param[i].name);
      strcat(action, ".ures_getStringByIndex(");
      strcat(action, tag);
      strcat(action, ")");

      
	 for (j=0; j<10; ++j){
		  index = count ? (randi(count * 3) - count) : (randi(200) - 100);
          status = U_ZERO_ERROR;
		  string=kERROR;
		  array=ures_getByKey(theBundle, tag, array, &status);
		  if(!U_FAILURE(status)){
			  UChar *t=NULL;
			  t=(UChar*)ures_getStringByIndex(array, index, &len, &status);
		      if(!U_FAILURE(status)){
                UChar element[3];
			    string=t;
				u_strcpy(expected_string, base);
                u_uastrcpy(element, itoa1(index,buf));
		        u_strcat(expected_string, element);
			  } else {
                u_strcpy(expected_string, kERROR);
			  }

		  }
		  expected_status = (index >= 0 && index < count) ? expected_resource_status : U_MISSING_RESOURCE_ERROR;
          CONFIRM_ErrorCode(status,expected_status);
          CONFIRM_EQ(string,expected_string);
		  		
      }
	   
     
      /*--------------2dArray------------------------------------------------- */  
	  
	    strcpy(tag,"array_2d_");
        strcat(tag,frag);

        strcpy(action,param[i].name);
        strcat(action, ".ures_getByKey(" );
        strcat(action,tag);
        strcat(action, ")");



        row_count = kERROR_COUNT, column_count = kERROR_COUNT;
        status = U_ZERO_ERROR;
		array2d=ures_getByKey(theBundle, tag, array2d, &status);

        CONFIRM_ErrorCode(status,expected_resource_status);
        if (U_SUCCESS(status))
        {
	  /*confirm the resource type is an 2darray*/
            CONFIRM_INT_EQ(ures_getType(array2d), RES_ARRAY);
		    row_count=ures_getSize(array2d);
          	CONFIRM_INT_GE(row_count,1);
 
			for(row=0; row<row_count; ++row){
				UResourceBundle *tableRow=NULL;
				tableRow=ures_getByIndex(array2d, row, tableRow, &status);
				CONFIRM_ErrorCode(status, expected_resource_status);
                if(U_SUCCESS(status)){
		  /*confirm the resourcetype of each table row is an array*/
			        CONFIRM_INT_EQ(ures_getType(tableRow), RES_ARRAY);
		            column_count=ures_getSize(tableRow);
                    CONFIRM_INT_GE(column_count,1);
           
                    for (col=0; j<column_count; ++j) {
						   UChar element[3];
                           u_strcpy(expected_string, base);
						   u_uastrcpy(element, itoa1(row, buf));
						   u_strcat(expected_string, element);
						   u_uastrcpy(element, itoa1(col, buf));
						   u_strcat(expected_string, element);
                           arrayItem1=ures_getNextResource(tableRow, arrayItem1, &status);
				           if(U_SUCCESS(status)){
							   const UChar *stringValue=ures_getString(arrayItem1, &len, &status);
					           CONFIRM_EQ(stringValue, expected_string);
                           }
					}
				} 
			}
		}else{
			CONFIRM_INT_EQ(row_count,kERROR_COUNT);
            CONFIRM_INT_EQ(column_count,kERROR_COUNT);
            row_count=column_count=0;
		}
		

      /*------2dArrayItem-------------------------------------------------------------- */
	/* 2dArrayItem*/
      for (j=0; j<10; ++j)
	   {
           row = row_count ? (randi(row_count * 3) - row_count) : (randi(200) - 100);
           col = column_count ? (randi(column_count * 3) - column_count) : (randi(200) - 100);
           status = U_ZERO_ERROR;
           string = kERROR;
		   len=0;
		   array2d=ures_getByKey(theBundle, tag, array2d, &status);
	       if(U_SUCCESS(status)){
                UResourceBundle *tableRow=NULL;
				tableRow=ures_getByIndex(array2d, row, tableRow, &status);
                if(U_SUCCESS(status)) {
                    UChar *t=NULL;
					t=(UChar*)ures_getStringByIndex(tableRow, col, &len, &status);
				    if(U_SUCCESS(status)){
				       string=t;
					}
				}
		   }
		   expected_status = (row >= 0 && row < row_count && col >= 0 && col < column_count) ?
                                  expected_resource_status: U_MISSING_RESOURCE_ERROR;
           CONFIRM_ErrorCode(status,expected_status);
           
		   if (U_SUCCESS(status)){
               UChar element[3];
               u_strcpy(expected_string, base);
     		   u_uastrcpy(element, itoa1(row, buf));
			   u_strcat(expected_string, element);
			   u_uastrcpy(element, itoa1(col, buf));
			   u_strcat(expected_string, element);
		   } else {
                u_strcpy(expected_string,kERROR);
		   }
           CONFIRM_EQ(string,expected_string);
				   
        }

      /*--------------taggedArray----------------------------------------------- */
      strcpy(tag,"tagged_array_");
      strcat(tag,frag);

      strcpy(action,param[i].name);
      strcat(action,".ures_getByKey(");
      strcat(action, tag);
      strcat(action,")");
      
	 
      status = U_ZERO_ERROR;
	  tag_count=0;
      tags=ures_getByKey(theBundle, tag, tags, &status);
	  CONFIRM_ErrorCode(status, expected_resource_status);
      if (U_SUCCESS(status)) {
            UResType bundleType=ures_getType(tags);
		    CONFIRM_INT_EQ(bundleType, RES_TABLE);
			
			tag_count=ures_getSize(tags);
            CONFIRM_INT_GE((int32_t)tag_count, (int32_t)0); 

			for(index=0; index <tag_count; index++){
				UResourceBundle *tagelement=NULL;
				char *key=NULL;
				UChar* value=NULL;
				tagelement=ures_getByIndex(tags, index, tagelement, &status);
				key=(char*)ures_getKey(tagelement);
				value=(UChar*)ures_getNextString(tagelement, &len, &key, &status);
				log_verbose("tag = %s, value = %s\n", key, austrdup(value));
				if(strncmp(key, "tag", 3) == 0 && u_strncmp(value, base, u_strlen(base)) == 0){
					record_pass();
				}else{
					record_fail();
				}
			 	
			}
		}else{
			tag_count=0;
		}
    
      /*---------taggedArrayItem----------------------------------------------*/
      count = 0;
        for (index=-20; index<20; ++index)
        {

           	status = U_ZERO_ERROR;
            string = kERROR;
		    strcpy(item_tag, "tag");
            strcat(item_tag, itoa1(index,buf));
		    tags=ures_getByKey(theBundle, tag, tags, &status);
			if(U_SUCCESS(status)){
			    UResourceBundle *tagelement=NULL;
				UChar *t=NULL;
				tagelement=ures_getByKey(tags, item_tag, tagelement, &status);
			    if(!U_FAILURE(status)){
				    UResType elementType=ures_getType(tagelement);
				    CONFIRM_INT_EQ(elementType, RES_STRING);
					if(strcmp(ures_getKey(tagelement), item_tag) == 0){
						record_pass();
					}else{
						record_fail();
					}
				    t=(UChar*)ures_getString(tagelement, &len, &status);
				    if(!U_FAILURE(status)){
				    	string=t;
					}
				}
		        if (index < 0) {
				    CONFIRM_ErrorCode(status,U_MISSING_RESOURCE_ERROR);
				}
                else{
			       if (status != U_MISSING_RESOURCE_ERROR) {
				       UChar element[3];
                       u_strcpy(expected_string, base);
                       u_uastrcpy(element, itoa1(index,buf));
				       u_strcat(expected_string, element);
				       CONFIRM_EQ(string,expected_string);
					   count++;
				   }
				}
			}
		}
      	CONFIRM_INT_EQ(count, tag_count);		
       
      free(expected_string);
      free(base);
      ures_close(theBundle);
    }
  return pass;
}

void record_pass()
{
  ++pass;
}

void record_fail()
{
  ++fail;
}

/**
 * Test to make sure that the U_USING_FALLBACK_ERROR and U_USING_DEFAULT_ERROR
 * are set correctly
 */

void TestFallback()
{
  UErrorCode status = U_ZERO_ERROR;
  UResourceBundle *fr_FR = NULL;
  const UChar *junk; /* ignored */
  
  log_verbose("Opening fr_FR..");
  fr_FR = ures_open(NULL, "fr_FR", &status);
  if(U_FAILURE(status))
    {
      log_err("Couldn't open fr_FR - %d\n", status);
      return;
    }

  status = U_ZERO_ERROR;


  /* clear it out..  just do some calls to get the gears turning */
  junk = ures_get(fr_FR, "LocaleID", &status);
  status = U_ZERO_ERROR;
  junk = ures_get(fr_FR, "LocaleString", &status);
  status = U_ZERO_ERROR;
  junk = ures_get(fr_FR, "LocaleID", &status);
  status = U_ZERO_ERROR;

  /* OK first one. This should be a Default value. */
  junk = ures_get(fr_FR, "Version", &status);
  if(status != U_USING_DEFAULT_ERROR)
    {
      log_err("Expected U_USING_DEFAULT_ERROR when trying to get Version from fr_FR, got %d\n", 
	      status);
    }
  
  status = U_ZERO_ERROR;

  /* and this is a Fallback, to fr */
  junk = ures_get(fr_FR, "ShortLanguage", &status);
  if(status != U_USING_FALLBACK_ERROR)
    {
      log_err("Expected U_USING_FALLBACK_ERROR when trying to get ShortLanguage from fr_FR, got %d\n", 
	      status);
    }
  
  status = U_ZERO_ERROR;
  
  ures_close(fr_FR);
}
void printUChars(UChar* uchars){
	int16_t i=0;
	for(i=0; i<u_strlen(uchars); i++){
		printf("%04X ", *(uchars+i));
	}
}



