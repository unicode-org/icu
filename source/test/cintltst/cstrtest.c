/*
**********************************************************************
* Copyright (C) 1998-2003, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*
* File cstrtest.c
*
* Modification History:
*
*   Date        Name        Description
*   07/13/2000  Madhu         created
*******************************************************************************
*/


#include "cstring.h"
#include "cintltst.h"
#include "cmemory.h"

static void TestAPI(void);
void addCStringTest(TestNode** root);

void addCStringTest(TestNode** root) {
   
    addTest(root, &TestAPI,   "tsutil/cstrtest/TestAPI");
  
}

static void TestAPI(void)
{
    int32_t intValue=0;
    char src[30]="HELLO THERE", dest[30];
    static const char *const abc="abcdefghijklmnopqrstuvwxyz", *const ABC="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    const char *temp;
    int32_t i;

    log_verbose("Testing uprv_tolower() and uprv_toupper()\n");
    for(i=0; i<=26; ++i) {
        dest[i]=uprv_tolower(abc[i]);
    }
    if(0!=strcmp(abc, dest)) {
        log_err("uprv_tolower(abc) failed\n");
    }

    for(i=0; i<=26; ++i) {
        dest[i]=uprv_tolower(ABC[i]);
    }
    if(0!=strcmp(abc, dest)) {
        log_err("uprv_tolower(ABC) failed\n");
    }

    for(i=0; i<=26; ++i) {
        dest[i]=uprv_toupper(abc[i]);
    }
    if(0!=strcmp(ABC, dest)) {
        log_err("uprv_toupper(abc) failed\n");
    }

    for(i=0; i<=26; ++i) {
        dest[i]=uprv_toupper(ABC[i]);
    }
    if(0!=strcmp(ABC, dest)) {
        log_err("uprv_toupper(ABC) failed\n");
    }

    log_verbose("Testing the API in cstring\n");
    T_CString_toLowerCase(src);
    if(uprv_strcmp(src, "hello there") != 0){
        log_err("FAIL: *** T_CString_toLowerCase() failed. Expected: \"hello there\", Got: \"%s\"\n", src);
    }
    T_CString_toUpperCase(src);
    if(uprv_strcmp(src, "HELLO THERE") != 0){
        log_err("FAIL: *** T_CString_toUpperCase() failed. Expected: \"HELLO THERE\", Got: \"%s\"\n", src);
    }
     
    intValue=T_CString_stringToInteger("34556", 10);
    if(intValue != 34556){
        log_err("FAIL: ****T_CString_stringToInteger(\"34556\", 10) failed. Expected: 34556, Got: %d\n", intValue);
    }
    intValue=T_CString_stringToInteger("100", 16);
    if(intValue != 256){
        log_err("FAIL: ****T_CString_stringToInteger(\"100\", 16) failed. Expected: 256, Got: %d\n", intValue);
    }
    i = T_CString_integerToString(src, 34556, 10);
    if(uprv_strcmp(src, "34556") != 0 || i != 5){
        log_err("FAIL: ****integerToString(src, 34566, 10); failed. Expected: \"34556\", Got: %s\n", src);
    }
    i = T_CString_integerToString(src, 431, 16);
    if(uprv_stricmp(src, "1AF") != 0 || i != 3){
        log_err("FAIL: ****integerToString(src, 431, 16); failed. Expected: \"1AF\", Got: %s\n", src);
    }

    uprv_strcpy(src, "this is lower case");
    if(T_CString_stricmp(src, "THIS is lower CASE") != 0){
        log_err("FAIL: *****T_CString_stricmp() failed.");
    }
    if((intValue=T_CString_stricmp(NULL, "first string is null") )!= -1){
        log_err("FAIL: T_CString_stricmp() where the first string is null failed. Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp("second string is null", NULL)) != 1){
        log_err("FAIL: T_CString_stricmp() where the second string is null failed. Expected: 1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp(NULL, NULL)) != 0){
        log_err("FAIL: T_CString_stricmp(NULL, NULL) failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_stricmp("", "")) != 0){
        log_err("FAIL: T_CString_stricmp(\"\", \"\") failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_stricmp("", "abc")) != -1){
        log_err("FAIL: T_CString_stricmp(\"\", \"abc\") failed.  Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_stricmp("abc", "")) != 1){
        log_err("FAIL: T_CString_stricmp(\"abc\", \"\") failed.  Expected: 1, returned %d\n", intValue);
    }

    temp=uprv_strdup("strdup");
    if(uprv_strcmp(temp, "strdup") !=0 ){
        log_err("FAIL: uprv_strdup() failed. Expected: \"strdup\", Got: %s\n", temp);
    }
    uprv_free((char *)temp);
  
    uprv_strcpy(src, "this is lower case");
    if(T_CString_strnicmp(src, "THIS", 4 ) != 0){
        log_err("FAIL: *****T_CString_strnicmp() failed.");
    }
    if((intValue=T_CString_strnicmp(NULL, "first string is null", 10) )!= -1){
        log_err("FAIL: T_CString_strnicmp() where the first string is null failed. Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_strnicmp("second string is null", NULL, 10)) != 1){
        log_err("FAIL: T_CString_strnicmp() where the second string is null failed. Expected: 1, returned %d\n", intValue);
    }
    if((intValue=T_CString_strnicmp(NULL, NULL, 10)) != 0){
        log_err("FAIL: T_CString_strnicmp(NULL, NULL, 10) failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_strnicmp("", "", 10)) != 0){
        log_err("FAIL: T_CString_strnicmp(\"\", \"\") failed.  Expected:  0, returned %d\n", intValue);;
    }
    if((intValue=T_CString_strnicmp("", "abc", 10)) != -1){
        log_err("FAIL: T_CString_stricmp(\"\", \"abc\", 10) failed.  Expected: -1, returned %d\n", intValue);
    }
    if((intValue=T_CString_strnicmp("abc", "", 10)) != 1){
        log_err("FAIL: T_CString_strnicmp(\"abc\", \"\", 10) failed.  Expected: 1, returned %d\n", intValue);
    }
    
}
