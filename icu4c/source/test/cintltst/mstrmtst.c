/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File memstrts.c (Tests the API in umemstrm)
*
* Modification History:
*
*   Date          Name        Description
*   07/19/2000    Madhu	      Creation 
*******************************************************************************
*/

#include <stdio.h>
#include <memory.h>
#include "unicode/utypes.h"
#include "cintltst.h"
#include "umemstrm.h"

void TestMemoryStreamAPI();
static void printUSeqErr(const uint8_t *a, int len);

void printUSeqErr(const uint8_t *a, int len)
{
    int i=0;
    fprintf(stderr, "{U+ ");
    while (i<len) fprintf(stderr, "0x%02x ", a[i++]);
    fprintf(stderr,"}\n");
}
void
addMemoryStreamTest(TestNode** root)
{
    addTest(root, &TestMemoryStreamAPI,       "/tsutil/mstrmtst/TestMemoryStreamAPI");
    
  
}

void TestMemoryStreamAPI(){
    UMemoryStream *memStream=NULL;
    int32_t size=999, x=0;
    uint8_t *gotBuffer=0;
    uint8_t buffer[]={ 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 
                       0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 
    };
    
    log_verbose("Testing the function uprv_mstrm_openNew()\n");
    memStream=uprv_mstrm_openNew(size);
    if(memStream == NULL){
        log_err("uprv_mstrm_openNew() failed\n");
    }
    uprv_mstrm_close(memStream);
    
    log_verbose("Testing the function uprv_mstrm_openNew() with size=0\n");
    memStream=uprv_mstrm_openNew(0);
    if(memStream == NULL){
        log_err("uprv_mstrm_openNew() failed with size=0\n");
    }
    
    log_verbose("Testing the function uprv_mstrm_write()\n");
    x=uprv_mstrm_write(memStream, buffer, sizeof(buffer)/sizeof(buffer[0]) );
    if(x == -1){
        log_err("uprv_mstrm_write() failed\n");
    }
    if(x != sizeof(buffer)/sizeof(buffer[0])){
        log_err("uprv_mstrm_write() wrote %d characters instead of %d\n", x, sizeof(buffer)/sizeof(buffer[0]));
    }
    
    log_verbose("Testing the function uprv_mstrm_getBuffer())\n");
    x=0;
    gotBuffer=uprv_mstrm_getBuffer(memStream, &x);
    if(memcmp(buffer, gotBuffer, sizeof(buffer)/sizeof(buffer[0])) != 0){
        log_err("uprv_mstrm_getBuffer() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, sizeof(buffer)/sizeof(buffer[0]));
        printf("\nExpected:");
        printUSeqErr(buffer, sizeof(buffer)/sizeof(buffer[0]));
    }

    log_verbose("Testing the function uprv_mstrm_read()\n");
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 1);
    if(memcmp(buffer, gotBuffer, 1) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 1);
        printf("\nExpected:");
        printUSeqErr(buffer, 1);

    } 
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 5);
    if(memcmp(buffer+1, gotBuffer, 5) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 5);
        printf("\nExpected:");
        printUSeqErr(buffer+1, 5);

    } 
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 8);
    if(memcmp(buffer+6, gotBuffer, 8) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 8);
        printf("\nExpected:");
        printUSeqErr(buffer+6, 8);


    } 
    /*try to read outside the limit*/
    /*It just reads untill the limit and sets the error and eof flags*/
    x=uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 5);
    if(memcmp(buffer+14, gotBuffer, 2) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 2);
        printf("\nExpected:");
        printUSeqErr(buffer+14, 2);
    }
    if(uprv_mstrm_error(memStream) != TRUE || uprv_mstrm_eof(memStream) != TRUE){
        log_err("Trying to read outside the limit should set the error and eof to TRUE\n");
    }
       
    uprv_mstrm_close(memStream);

    log_verbose("Testing the function uprv_mstrm_openBuffer()\n");
    memStream=uprv_mstrm_openBuffer(buffer, size);
    if(memStream == NULL){
        log_err("uprv_mstrm_openBuffer() failed\n");
    }
    log_verbose("Testing the function uprv_mstrm_getBuffer())\n");
    x=0;
    gotBuffer=uprv_mstrm_getBuffer(memStream, &x);
    if(memcmp(buffer, gotBuffer, sizeof(buffer)/sizeof(buffer[0])) != 0){
        log_err("uprv_mstrm_getBuffer() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, sizeof(buffer)/sizeof(buffer[0]));
        printf("\nExpected:");
        printUSeqErr(buffer, sizeof(buffer)/sizeof(buffer[0]));
    }

    log_verbose("Test that function uprv_mstrm_openBuffer() opens it in the read only mode\n");
    x=uprv_mstrm_write(memStream, gotBuffer, 2);
    if(x !=0  || uprv_mstrm_error(memStream) != TRUE){
        log_err("trying to write into a read only buffer should fail\n");
    }
    uprv_mstrm_close(memStream);

   
    memStream=uprv_mstrm_openNew(1);
    if(memStream == NULL){
        log_err("uprv_mstrm_openNew() failed\n");
    }
    log_verbose("Testing the function uprv_mstrm_write() when position > size\n");
    x=uprv_mstrm_write(memStream, buffer, sizeof(buffer)/sizeof(buffer[0]) );
    if(x == -1){
        log_err("uprv_mstrm_write() failed\n");
    }
    if(x != sizeof(buffer)/sizeof(buffer[0])){
        log_err("uprv_mstrm_write() wrote %d characters instead of %d\n", x, sizeof(buffer)/sizeof(buffer[0]));
    }
    
    log_verbose("Testing how different functions behave when error is set to true using setError\n");
    uprv_mstrm_setError(memStream);
    gotBuffer=uprv_mstrm_getBuffer(memStream, &x);
    if(gotBuffer != NULL || x !=0 ){
        log_err("uprv_mstrm_getBuffer() should fail when the error is set to true using uprv_mstrm_setError()");
    }
    uprv_mstrm_close(memStream);
   
}
