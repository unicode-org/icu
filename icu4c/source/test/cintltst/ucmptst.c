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
#include "ucmp8.h"
#include "ucmpe32.h"
#include "cmemory.h"
#include "cintltst.h"
#include "ucol_imp.h"


static void TestUCMP8API(void);
static void TestUCMPE32API(void);

void addCompactArrayTest(TestNode** root);


void
addCompactArrayTest(TestNode** root)
{
    addTest(root, &TestUCMP8API,   "ucmptst/TestUCMP8API");
    addTest(root, &TestUCMPE32API,  "ucmptst/TestUCMPE32API");
}

static void TestUCMP8API(){
    CompactByteArray* ucmp8Array=NULL;
    CompactByteArray ucmp8Array1;
    CompactByteArray ucmp8Array2;
    int32_t i=0;
    uint8_t *values;
    uint8_t *valuesSet;
    uint8_t const TEST_DEFAULT_VALUE = (uint8_t)0xFF;
    

    /*ucmp8_open*/
    log_verbose("Testing ucmp8_open()\n");
    ucmp8Array=ucmp8_open(TEST_DEFAULT_VALUE);
    if(ucmp8Array == NULL){
        log_err("ERROR: ucmp8_open() failed\n");
    }
    if( (int32_t)ucmp8_getCount(ucmp8Array) != (int32_t)ucmp8_getkUnicodeCount()) {
        log_err("ERROR: ucmp8_open failed\n");
    }

    /*ucmp8_init*/
    log_verbose("Testing ucmp8_init()\n");
    ucmp8_init(&ucmp8Array1, TEST_DEFAULT_VALUE);
    if( (int32_t)ucmp8_getCount(&ucmp8Array1) != (int32_t)ucmp8_getkUnicodeCount() || 
        ucmp8_getIndex(&ucmp8Array1) == NULL ||
        ucmp8_getArray(&ucmp8Array1) == NULL || 
        ucmp8Array1.fBogus != FALSE){
        log_err("Error: ucmp8_init() failed\n");
    }
    /*ucmp8_initBogus*/
    log_verbose("Testing ucmp8_initBogus()\n");
    ucmp8_initBogus(&ucmp8Array2);
    if((int32_t)ucmp8_getCount(&ucmp8Array2) != ucmp8Array2.fCount ||
        ucmp8_getIndex(&ucmp8Array2) != NULL ||
        ucmp8_getArray(&ucmp8Array2) != NULL || 
        ucmp8Array2.fBogus != TRUE){
        log_err("Error: ucmp8_initBogus() failed\n");
    }
    /*ucmp8_getkBlockCount*/
    if(ucmp8_getkBlockCount() != 128 ){
        log_err("Error in ucmp8_getkBlockCount()\n");
    } 
    values=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    if(values[0] !=TEST_DEFAULT_VALUE){
        log_err("Error: getArray() or init failed\n");
    }
    
    /*ucmp8_compact*/
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_open failed Got compact for expanded data\n");
    } 
    ucmp8_compact(&ucmp8Array1, 1);
    if(ucmp8Array1.fCompact != TRUE){
        log_err("Error: ucmp8_compact failed\n");
    } 
    /*ucmp8_set*/
    ucmp8_set(&ucmp8Array1, 0, (uint8_t)0xFE);
    valuesSet=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    if(valuesSet[0] != (uint8_t)0xFE ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    } 

    /*ucmp8_set*/
    ucmp8_compact(&ucmp8Array1, 1);
    ucmp8_set(&ucmp8Array1, 0, (uint8_t)0xFD);
    valuesSet=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    if(valuesSet[0] != (uint8_t)0xFD ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1.fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    }
    /*ucmp8_setRange*/
    ucmp8_compact(&ucmp8Array1, 1);
    ucmp8_setRange(&ucmp8Array1, 0,  10, (uint8_t)0xFD);
    valuesSet=(uint8_t*)ucmp8_getArray(&ucmp8Array1);
    for(i =0 ; i< 10; i++ ){
        if(valuesSet[0] != (uint8_t)0xFD ){
             log_err("ERROR: ucmp8_set() failed\n");
             break;
        }
    }

    ucmp8_close(ucmp8Array);
    ucmp8_close(&ucmp8Array1);
    ucmp8_close(&ucmp8Array2);
}



void addCompactArrayTest(TestNode** root);

struct {
  UChar lead;
  UChar trail;
  int32_t value;
} testCmpe32array[] = {
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


static void fillup(CompactEIntArray *a) {
  int32_t i = 0;
  for(i = 0; i<sizeof(testCmpe32array)/sizeof(testCmpe32array[0]); i++) {
    if(testCmpe32array[i].lead == 0) {
      ucmpe32_set32(a, testCmpe32array[i].trail, testCmpe32array[i].value);
    } else {
      ucmpe32_setSurrogate(a, testCmpe32array[i].lead, testCmpe32array[i].trail, testCmpe32array[i].value);
    }
  }
}

static void query(CompactEIntArray *a) {
  int32_t i = 0;      
  int32_t result = 0;
  for(i = 0; i<sizeof(testCmpe32array)/sizeof(testCmpe32array[0]); i++) {
    if(testCmpe32array[i].lead == 0) {
      result = ucmpe32_get(a, testCmpe32array[i].trail);
      if(result != testCmpe32array[i].value) {
        log_err("Wrong value for %04X, expected %08X, got %08X\n", 
          testCmpe32array[i].trail, testCmpe32array[i].value, result);
      }
    } else {
      if(a->fCompact == TRUE) {
        result = ucmpe32_get(a, testCmpe32array[i].lead);
        result = ucmpe32_getSurrogate(a, result, testCmpe32array[i].trail);
        if(result != ucmpe32_getSurrogateEx(a, testCmpe32array[i].lead, testCmpe32array[i].trail)) {
          log_err("results for getsurrogate and getsurrogateex do not match in compacted array\n");
        }
      } else {
        result = ucmpe32_getSurrogateEx(a, testCmpe32array[i].lead, testCmpe32array[i].trail);
      }
      if(result != testCmpe32array[i].value) {
        log_err("Wrong value for %04X %04X, expected %08X, got %08X\n", 
          testCmpe32array[i].lead, testCmpe32array[i].trail, testCmpe32array[i].value, result);
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
