/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-1999, International Business Machines Corporation and
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
#include "ucmp32.h"
#include "cmemory.h"
#include "cintltst.h"
#include <stdio.h>



static void TestUCMP16API();
static void TestUCMP8API();
static void TestUCMP32API();



void 
addCompactArrayTest(TestNode** root)
{
  addTest(root, &TestUCMP16API,  "ucmptst/TestUCMP16API");
  addTest(root, &TestUCMP8API,   "ucmptst/TestUCMP8API");
  addTest(root, &TestUCMP32API,  "ucmptst/TestUCMP32API");
 
}

void TestUCMP16API(){
     uint16_t newValues[]={
        0x00, 0x01, 0x02, 0x03, 0x04, 
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04, 
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04,
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04, 
        0x05, 0x06, 0x07, 0x08, 0x09, 
        0x00, 0x01, 0x02, 0x03, 0x04, 
        0x05, 0x06, 0x07, 0x08, 0x09, 
    };
     int16_t indexArray[]={
        0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65,
    };
       
    uint16_t *values;
   
    CompactShortArray* ucmp16Array=NULL;
    CompactShortArray* ucmp16Array1=NULL;
    CompactShortArray* ucmp16Array2=NULL;
    UErrorCode status=U_ZERO_ERROR;
    int32_t count=0, i=0;
    int16_t const TEST_DEFAULT_VALUE = 0xff;

    /*ucmp16_open*/
    log_verbose("Testing ucmp16_open()\n");
    ucmp16Array=ucmp16_open(TEST_DEFAULT_VALUE);
    if(ucmp16Array == NULL){
        log_err("ERROR: ucmp16_open() failed\n");
    }
    if(ucmp16_getDefaultValue(ucmp16Array) != TEST_DEFAULT_VALUE ||
        (int32_t)ucmp16_getCount(ucmp16Array) != (int32_t)ucmp16_getkUnicodeCount()) {
        log_err("ERROR: ucmp16_open failed\n");
    }

    ucmp16Array1=ucmp16_open(0x0000);
    ucmp16Array2=ucmp16_open(0x0000);

    /*ucmp16_init*/
    log_verbose("Testing ucmp16_init()\n");
    ucmp16_init(ucmp16Array1, TEST_DEFAULT_VALUE);
    if(ucmp16_getDefaultValue(ucmp16Array1) == 0x0000 ||
        (int32_t)ucmp16_getCount(ucmp16Array1) != (int32_t)ucmp16_getkUnicodeCount() || 
        ucmp16_getIndex(ucmp16Array1) == NULL ||
        ucmp16_getArray(ucmp16Array1) == NULL || 
        ucmp16Array->fBogus != FALSE){
        log_err("Error: ucmp16_init() failed\n");
    }
    /*ucmp16_initBogus*/
    log_verbose("Testing ucmp16_initBogus()\n");
    ucmp16_initBogus(ucmp16Array2);
    if(ucmp16_getDefaultValue(ucmp16Array2) != 0x0000 ||
        (int32_t)ucmp16_getCount(ucmp16Array2) != ucmp16Array2->fCount ||
        ucmp16_getIndex(ucmp16Array2) != NULL ||
        ucmp16_getArray(ucmp16Array2) != NULL || 
        ucmp16Array2->fBogus != TRUE){
        log_err("Error: ucmp16_initBogus() failed\n");
    }
    /*ucmp16_getDefaultValue*/
    if(ucmp16_getDefaultValue(ucmp16Array) != ucmp16_getDefaultValue(ucmp16Array1) ||
        ucmp16_getDefaultValue(ucmp16Array) == ucmp16_getDefaultValue(ucmp16Array2) ){
        log_err("Error in ucmp16_getDefaultValue()\n");
    }
     /*ucmp16_getkBlockCount*/
    if(ucmp16_getkBlockCount() != 128 ){
        log_err("Error in ucmp16_getkBlockCount()\n");
    } 


    ucmp16_close(ucmp16Array);
    ucmp16_close(ucmp16Array1);
    ucmp16_close(ucmp16Array2);

    /*ucmp_compact, ucmp16_set*/
    log_verbose("Testing ucmp16_set\n");
    ucmp16Array=ucmp16_open(TEST_DEFAULT_VALUE);
    if(ucmp16Array->fCompact == TRUE){
        log_err("Error: ucmp16_open failed Got compact for expanded data\n");
    } 
     
    ucmp16_compact(ucmp16Array);
    if(ucmp16Array->fCompact != TRUE){
        log_err("Error: ucmp16_compact failed\n");
    }
   
    /* ucmp16_set*/
    ucmp16_set(ucmp16Array, 0,  TEST_DEFAULT_VALUE);
    values=(int16_t*)ucmp16_getArray(ucmp16Array);
    if(values[0] != TEST_DEFAULT_VALUE){
        log_err("ERROR: ucmp16_set() failed\n");
    }
    if(ucmp16Array->fCompact == TRUE){
        log_err("Error: ucmp16_set didn't expand the compact data\n");
    } 
    /*ucmp16_set where the value != defaultValue*/
    /*ucmp16_compact(ucmp16Array);
    ucmp16_set(ucmp16Array, 0,  0xfe);
    values=(int16_t*)ucmp16_getArray(ucmp16Array);
    if(values[0] != 0xfe){
        log_err("ERROR: ucmp16_set() failed\n");
    }*/

    /*ucmp16_setRange*/
    /*ucmp16_compact(ucmp16Array);*/
    ucmp16_setRange(ucmp16Array, 0,  10, 0xff);
    values=(int16_t*)ucmp16_getArray(ucmp16Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xff){
            log_err("ERROR: ucmp16_set() failed\n");
            break;
        }
    }
    /*ucmp16_setRange where the value != defaultValue*/
    /*ucmp16_compact(ucmp16Array);
    ucmp16_setRange(ucmp16Array, 0,  10, 0xfe);
    values=(int16_t*)ucmp16_getArray(ucmp16Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xfe){
            log_err("ERROR: ucmp16_set() failed\n");
            break;
        }
    }
    */

   ucmp16_close(ucmp16Array);
  
    /*ucmp16_openAlias*/
    log_verbose("Testing ucmp16_openAlias()\n");
    count=sizeof(newValues)/sizeof(newValues[0]);
    ucmp16Array1=ucmp16_openAlias(indexArray, newValues, count, 0xFE);
    if(ucmp16_getDefaultValue(ucmp16Array1) != 0x00fe ||
        (int32_t)ucmp16_getCount(ucmp16Array1) != count ||
        ucmp16Array1->fAlias != TRUE ||
        memcmp(ucmp16_getArray(ucmp16Array1), newValues, count) != 0  ||
        memcmp(ucmp16_getIndex(ucmp16Array1), indexArray, count) != 0 ) {
         log_err("Error: ucmp16_openAlias() failed\n");
    }
    ucmp16_close(ucmp16Array1);


}

void TestUCMP8API(){
    CompactByteArray* ucmp8Array=NULL;
    CompactByteArray* ucmp8Array1=NULL;
    CompactByteArray* ucmp8Array2=NULL;
    UErrorCode status=U_ZERO_ERROR;
    int32_t i=0;
    int8_t *values;
    int8_t *valuesSet;
    int8_t const TEST_DEFAULT_VALUE = (char)0xFF;
    

    /*ucmp8_open*/
    log_verbose("Testing ucmp8_open()\n");
    ucmp8Array=ucmp8_open(TEST_DEFAULT_VALUE);
    if(ucmp8Array == NULL){
        log_err("ERROR: ucmp8_open() failed\n");
    }
    if( (int32_t)ucmp8_getCount(ucmp8Array) != (int32_t)ucmp8_getkUnicodeCount()) {
        log_err("ERROR: ucmp8_open failed\n");
    }

    ucmp8Array1=ucmp8_open(0x0000);
    ucmp8Array2=ucmp8_open(0x0000);

    /*ucmp8_init*/
    log_verbose("Testing ucmp8_init()\n");
    ucmp8_init(ucmp8Array1, TEST_DEFAULT_VALUE);
    if( (int32_t)ucmp8_getCount(ucmp8Array1) != (int32_t)ucmp8_getkUnicodeCount() || 
        ucmp8_getIndex(ucmp8Array1) == NULL ||
        ucmp8_getArray(ucmp8Array1) == NULL || 
        ucmp8Array1->fBogus != FALSE){
        log_err("Error: ucmp8_init() failed\n");
    }
    /*ucmp8_initBogus*/
    log_verbose("Testing ucmp8_initBogus()\n");
    ucmp8_initBogus(ucmp8Array2);
    if((int32_t)ucmp8_getCount(ucmp8Array2) != ucmp8Array2->fCount ||
        ucmp8_getIndex(ucmp8Array2) != NULL ||
        ucmp8_getArray(ucmp8Array2) != NULL || 
        ucmp8Array2->fBogus != TRUE){
        log_err("Error: ucmp8_initBogus() failed\n");
    }
    /*ucmp8_getkBlockCount*/
    if(ucmp8_getkBlockCount() != 128 ){
        log_err("Error in ucmp8_getkBlockCount()\n");
    } 
    values=(int8_t*)ucmp8_getArray(ucmp8Array1);
    if(values[0] !=TEST_DEFAULT_VALUE){
        log_err("Error: getArray() or init failed\n");
    }
    
    /*ucmp8_compact*/
    if(ucmp8Array1->fCompact == TRUE){
        log_err("Error: ucmp8_open failed Got compact for expanded data\n");
    } 
    ucmp8_compact(ucmp8Array1, 1);
    if(ucmp8Array1->fCompact != TRUE){
        log_err("Error: ucmp8_compact failed\n");
    } 
    /*ucmp8_set*/
    ucmp8_set(ucmp8Array1, 0,  (char)0xFE);
    valuesSet=(int8_t*)ucmp8_getArray(ucmp8Array1);
    if(valuesSet[0] != (char)0xFE ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1->fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    } 

    /*ucmp8_set*/
    ucmp8_compact(ucmp8Array1, 1);
    ucmp8_set(ucmp8Array1, 0,  (char)0xFD);
    valuesSet=(int8_t*)ucmp8_getArray(ucmp8Array1);
    if(valuesSet[0] != (char)0xFD ){
        log_err("ERROR: ucmp8_set() failed\n");
    }
    if(ucmp8Array1->fCompact == TRUE){
        log_err("Error: ucmp8_set didn't expand the compact data \n");
    }
    /*ucmp8_setRange*/
    ucmp8_compact(ucmp8Array1, 1);
    ucmp8_setRange(ucmp8Array1, 0,  10, (char)0xFD);
    valuesSet=(int8_t*)ucmp8_getArray(ucmp8Array1);
    for(i =0 ; i< 10; i++ ){
        if(valuesSet[0] != (char)0xFD ){
             log_err("ERROR: ucmp8_set() failed\n");
             break;
        }
    }

    ucmp8_close(ucmp8Array);
    ucmp8_close(ucmp8Array1);
    ucmp8_close(ucmp8Array2);
}

void TestUCMP32API(){
    CompactIntArray* ucmp32Array=NULL;
   
    UErrorCode status=U_ZERO_ERROR;
    int32_t i=0;
    int32_t *values;
    int32_t const TEST_DEFAULT_VALUE = 0xFFFF;

    /*ucmp32_open*/
    log_verbose("Testing ucmp32_open()\n");
    ucmp32Array=ucmp32_open(TEST_DEFAULT_VALUE);
    if(ucmp32Array == NULL){
        log_err("ERROR: ucmp32_open() failed\n");
    }
    if( (int32_t)ucmp32_getCount(ucmp32Array) != (int32_t)ucmp32_getkUnicodeCount()) {
        log_err("ERROR: ucmp32_open or ucmp_getCount() failed failed\n");
    }
    /*ucmp32_getkBlockCount*/
    if(ucmp32_getkBlockCount() != 128 ){
        log_err("Error in ucmp32_getkBlockCount()\n");
    }
    if(ucmp32_getArray(ucmp32Array) == NULL ||
        ucmp32_getIndex(ucmp32Array) == NULL ){
        log_err("Error in ucmp32_open of ucmp32_getArray() or ucmp32_getIndex()\n");
    }
     /*ucmp32_compact*/
    if(ucmp32Array->fCompact == TRUE){
        log_err("Error: ucmp32_open failed Got compact for expanded data\n");
    } 
    
    ucmp32_compact(ucmp32Array, 1);
    if(ucmp32Array->fCompact != TRUE){
        log_err("Error: ucmp32_compact failed\n");
    } 
   
    /* ucmp32_set*/
    ucmp32_set(ucmp32Array, 0,  TEST_DEFAULT_VALUE);
    values=(int32_t*)ucmp32_getArray(ucmp32Array);
    if(values[0] != TEST_DEFAULT_VALUE){
        log_err("ERROR: ucmp32_set() failed\n");
    }
    if(ucmp32Array->fCompact == TRUE){
        log_err("Error: ucmp32_set didn't expand\n");
    } 
   
    /*ucmp32_set where the value != defaultValue*/
    ucmp32_compact(ucmp32Array, 1);
    ucmp32_set(ucmp32Array, 0,  0xFFFE);
    values=(int32_t*)ucmp32_getArray(ucmp32Array);
    if(values[0] != 0xFFFE){
        log_err("ERROR: ucmp32_set() failed\n");
    }

    /*ucmp32_setRange*/
    ucmp32_compact(ucmp32Array, 1);
    ucmp32_setRange(ucmp32Array, 0,  10, 0xFFFF);
    values=(int32_t*)ucmp32_getArray(ucmp32Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xFFFF){
            log_err("ERROR: ucmp32_set() failed\n");
            break;
        }
    }
    if(ucmp32Array->fCompact == TRUE){
        log_err("Error: ucmp32_setRange didn't expand\n");
    } 
    /*ucmp32_setRange where the value != defaultValue*/
    ucmp32_compact(ucmp32Array, 1);
    ucmp32_setRange(ucmp32Array, 0,  10, 0xFFFE);
    values=(int32_t*)ucmp32_getArray(ucmp32Array);
    for(i=0; i<10; i++){
        if(values[0] != 0xFFFE){
            log_err("ERROR: ucmp32_set() failed\n");
            break;
        }
    }
    
    ucmp32_close(ucmp32Array);
}