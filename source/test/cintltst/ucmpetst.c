/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File test.c
*
* Modification History:
*
*   Date          Name        Description
*   07/28/2000    Madhu       Creation 
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "umemstrm.h"
#include "ucmpe32.h"
#include "cmemory.h"
#include "cintltst.h"
#include "ucol_imp.h"


static void TestUCMPE32API(void);

void addCompactArrayTest(TestNode** root);

struct {
  UChar lead;
  UChar trail;
  int32_t value;
} testarray[] = {
  { 0x0, 0x0020, 0x00000020 },
  { 0x0, 0x0040, 0x00000040 },
  { 0x0, 0x004B, 0x0000004B },
  { 0x0, 0x00AC, 0x000000AC },
  { 0x0, 0x0400, 0x00000400 },
  { 0x0, 0xa123, 0x0000a123 },
  { 0x0, 0xeeee, 0x0000eeee },
  { 0xd800, 0xdc00, 0x0001000 },
  { 0xd900, 0xdc00, 0x0005000 }
};


void
addCompactArrayExTest(TestNode** root)
{
  addTest(root, &TestUCMPE32API,  "ucmpetst/TestUCMPE32API");
 
}

static void fillup(CompactEIntArray *a) {
  int32_t i = 0;
  for(i = 0; i<sizeof(testarray)/sizeof(testarray[0]); i++) {
    if(testarray[i].lead == 0) {
      ucmpe32_set32(a, testarray[i].trail, testarray[i].value);
    } else {
      ucmpe32_setSurrogate(a, testarray[i].lead, testarray[i].trail, testarray[i].value);
    }
  }
}

static void query(CompactEIntArray *a) {
  int32_t i = 0;      
  int32_t result = 0;
  for(i = 0; i<sizeof(testarray)/sizeof(testarray[0]); i++) {
    if(testarray[i].lead == 0) {
      result = ucmpe32_get(a, testarray[i].trail);
      if(result != testarray[i].value) {
        log_err("Wrong value for %04X, expected %08X, got %08X\n", 
          testarray[i].trail, testarray[i].value, result);
      }
    } else {
      if(a->fCompact == TRUE) {
        result = ucmpe32_get(a, testarray[i].lead);
        result = ucmpe32_getSurrogate(a, result, testarray[i].trail);
        if(result != ucmpe32_getSurrogateEx(a, testarray[i].lead, testarray[i].trail)) {
          log_err("results for getsurrogate and getsurrogateex do not match in compacted array\n");
        }
      } else {
        result = ucmpe32_getSurrogateEx(a, testarray[i].lead, testarray[i].trail);
      }
      if(result != testarray[i].value) {
        log_err("Wrong value for %04X %04X, expected %08X, got %08X\n", 
          testarray[i].lead, testarray[i].trail, testarray[i].value, result);
      }
    }
  }
}

static void TestUCMPE32API(){
    CompactEIntArray *ucmpe32Array=NULL, *ucmpe32Clone = NULL;
   
    int32_t i=0;
    /*int32_t *values;*/
    int32_t const TEST_DEFAULT_VALUE = 0xFFFF;
    UErrorCode status = U_ZERO_ERROR;

    /*ucmpe32_open*/
    log_verbose("Testing ucmpe32_open()\n");
    ucmpe32Array=ucmpe32_open(UCOL_NOT_FOUND, UCOL_SPECIAL_FLAG | (SURROGATE_TAG<<24), &status);
    if(U_FAILURE(status) || ucmpe32Array == NULL){
        log_err("ERROR: ucmpe32_open() failed\n");
        status = U_ZERO_ERROR;
    } else {
      fillup(ucmpe32Array);
      query(ucmpe32Array);

      log_verbose("Testing ucmpe32_clone()\n");
      ucmpe32Clone=ucmpe32_clone(ucmpe32Array, &status);
      if(U_FAILURE(status) || ucmpe32Clone == NULL){
          log_err("ERROR: ucmpe32_clone() failed\n");
          status = U_ZERO_ERROR;
      } else {
        query(ucmpe32Clone);
        ucmpe32_close(ucmpe32Clone);
        ucmpe32Clone = NULL;
      }

      log_verbose("Testing ucmpe32_flattenMem()\n");
      {
        UMemoryStream *MS = uprv_mstrm_openNew(65536);
        int32_t size = ucmpe32_flattenMem(ucmpe32Array, MS);
        int32_t len = 0;
        const uint8_t *buff = NULL; 
        if(size > 0) {
          log_err("Managed to flatten uncompacted array\n");
        }
        ucmpe32_compact(ucmpe32Array);
        query(ucmpe32Array);

        /* try after compacting */
        size = ucmpe32_flattenMem(ucmpe32Array, MS);
        buff = uprv_mstrm_getBuffer(MS, &len);

        if(size == 0 || len == 0 || buff == NULL) {
          log_err("Unable to flatten!\n");
        } else {
          log_verbose("Testing ucmpe32_openFromData()\n");
          ucmpe32Clone = ucmpe32_openFromData(&buff, &status);
          if(U_FAILURE(status) || ucmpe32Clone == NULL){
              log_err("ERROR: ucmpe32_openFromData() failed\n");
              status = U_ZERO_ERROR;
          } else {
            query(ucmpe32Clone);
            ucmpe32_close(ucmpe32Clone);
            ucmpe32Clone = NULL;
          }
        }

      }
      ucmpe32_close(ucmpe32Array);
    }
}
