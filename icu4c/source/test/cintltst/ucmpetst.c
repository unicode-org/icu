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
#include "ucmp16.h"
#include "ucmp8.h"
#include "ucmpe32.h"
#include "cmemory.h"
#include "cintltst.h"
#include "ucol_imp.h"


static void TestUCMPE32API(void);

void addCompactArrayTest(TestNode** root);


void
addCompactArrayExTest(TestNode** root)
{
  addTest(root, &TestUCMPE32API,  "ucmpetst/TestUCMPE32API");
 
}

static void query(CompactEIntArray *a) {
      uint32_t CP = 0;
      int32_t value;
      int32_t result;
      UChar lead = 0;
      UChar trail = 0;
      for(value = 0; value < 0xd800; value++) {
        result = ucmpe32_get(a, (UChar)value);
        if(value%128 == 0) {
          if(result != value) {
            log_err("Expected %04X, got %04X\n", value, result);
          }
        } else {
          if(result != 0xF0000000) {
            log_err("For %04X expected 0, got %04X\n", value, result);
          }
        }
      }
      /* skip the surrogate space */
      for(value = 0xe000; value < 0x10000; value++) {
        result = ucmpe32_get(a, (UChar)value);
        if(value%128 == 0) {
          if(result != value) {
            log_err("Expected %04X, got %04X\n", value, result);
          }
        } else {
          if(result != 0xF0000000) {
            log_err("For %04X expected 0x0, got %04X\n", value, result);
          }
        }
      }

      for(value = 0x10000; value < 0x110000; value++) {
        lead = (UChar)((value>>10) + 0xd7c0);
        trail = (UChar)((value & 0x3ff) + 0xdc00);

        result = ucmpe32_getSurrogate(a, lead, trail);
        if(value%1024 == 0) {
          if(result != value) {
            log_err("Expected %04X, got %04X\n", value, result);
          }
        } else {
          if(result != 0xF0000000) {
            log_err("For %04X expected 0x0, got %04X\n", value, result);
          }
        }
      }   
}

static void TestUCMPE32API(){
    CompactEIntArray* ucmpe32Array=NULL;
   
    int32_t i=0;
    /*int32_t *values;*/
    int32_t const TEST_DEFAULT_VALUE = 0xFFFF;

    return;

    /*ucmpe32_open*/
    log_verbose("Testing ucmpe32_open()\n");
    ucmpe32Array=ucmpe32_open(UCOL_NOT_FOUND);
    if(ucmpe32Array == NULL){
        log_err("ERROR: ucmpe32_open() failed\n");
    }
    if( (int32_t)ucmpe32_getCount(ucmpe32Array) != (int32_t)ucmpe32_getkUnicodeCount()) {
        log_err("ERROR: ucmpe32_open or ucmp_getCount() failed failed\n");
    }
    /*ucmpe32_getkBlockCount*/
    if(ucmpe32_getkBlockCount() != 128 ){
        log_err("Error in ucmpe32_getkBlockCount()\n");
    }
    if(ucmpe32_getArray(ucmpe32Array) == NULL ||
        ucmpe32_getIndex(ucmpe32Array) == NULL ){
        log_err("Error in ucmpe32_open of ucmpe32_getArray() or ucmpe32_getIndex()\n");
    }

    {
      uint32_t CP = 0;
      int32_t value;
      UChar lead = 0;
      UChar trail = 0;
      for(value = 0; value < 0x10000; value+=128) {
        ucmpe32_set(ucmpe32Array, (UChar)value, value);
      }

      for(value = 0x10000; value < 0x110000; value+=1024) {
        lead = (UChar)((value>>10) + 0xd7c0);
        trail = (UChar)((value & 0x3ff) + 0xdc00);
        ucmpe32_setSurrogate(ucmpe32Array, lead, trail, value);
      }

      query(ucmpe32Array);
      ucmpe32_compact(ucmpe32Array, 1);  
      query(ucmpe32Array);
    }



#if 0
     /*ucmpe32_compact*/
    if(ucmpe32Array->fCompact == TRUE){
        log_err("Error: ucmpe32_open failed Got compact for expanded data\n");
    } 
    
    ucmpe32_compact(ucmpe32Array, 1);
    if(ucmpe32Array->fCompact != TRUE){
        log_err("Error: ucmpe32_compact failed\n");
    } 
   
    /* ucmpe32_set*/
    ucmpe32_set(ucmpe32Array, 0,  TEST_DEFAULT_VALUE);
    values=(int32_t*)ucmpe32_getArray(ucmpe32Array);
    if(values[0] != TEST_DEFAULT_VALUE){
        log_err("ERROR: ucmpe32_set() failed\n");
    }
    if(ucmpe32Array->fCompact == TRUE){
        log_err("Error: ucmpe32_set didn't expand\n");
    } 
   
    /*ucmpe32_set where the value != defaultValue*/
    ucmpe32_compact(ucmpe32Array, 1);
    ucmpe32_set(ucmpe32Array, 0,  0xFFFE);
    values=(int32_t*)ucmpe32_getArray(ucmpe32Array);
    if(values[0] != 0xFFFE){
        log_err("ERROR: ucmpe32_set() failed\n");
    }

    /*ucmpe32_setRange*/
    ucmpe32_compact(ucmpe32Array, 1);
    ucmpe32_setRange(ucmpe32Array, 0,  10, 0xFFFF);
    values=(int32_t*)ucmpe32_getArray(ucmpe32Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xFFFF){
            log_err("ERROR: ucmpe32_set() failed\n");
            break;
        }
    }
    if(ucmpe32Array->fCompact == TRUE){
        log_err("Error: ucmpe32_setRange didn't expand\n");
    } 
    /*ucmpe32_setRange where the value != defaultValue*/
    ucmpe32_compact(ucmpe32Array, 1);
    ucmpe32_setRange(ucmpe32Array, 0,  10, 0xFFFE);
    values=(int32_t*)ucmpe32_getArray(ucmpe32Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xFFFE){
            log_err("ERROR: ucmpe32_set() failed\n");
            break;
        }
    }
#endif
    
    ucmpe32_close(ucmpe32Array);
}
