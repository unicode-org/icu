/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File memstrts.c (Tests the API in umemstrm)
*
* Modification History:
*
*   Date          Name        Description
*   07/19/2000    Madhu       Creation 
*******************************************************************************
*/

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cintltst.h"
#include "umemstrm.h"

static void TestMemoryStreamAPI(void);
static void printUSeqErr(const uint8_t *a, int len);

void addMemoryStreamTest(TestNode** root);

static void printUSeqErr(const uint8_t *a, int len)
{
    int i=0;
    fprintf(stderr, "{U+ ");
    while (i<len)
        fprintf(stderr, "0x%02x ", a[i++]);
    fprintf(stderr,"}\n");
}

void
addMemoryStreamTest(TestNode** root)
{
    addTest(root, &TestMemoryStreamAPI,       "/tsutil/mstrmtst/TestMemoryStreamAPI");

}

static void TestMemoryStreamAPI(){
    UMemoryStream *memStream=NULL;
    int32_t size=999, x=0;
    const uint8_t *gotBuffer=0;
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
    if(uprv_memcmp(buffer, gotBuffer, sizeof(buffer)/sizeof(buffer[0])) != 0){
        log_err("uprv_mstrm_getBuffer() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, sizeof(buffer)/sizeof(buffer[0]));
        printf("\nExpected:");
        printUSeqErr(buffer, sizeof(buffer)/sizeof(buffer[0]));
    }

    log_verbose("Testing the function uprv_mstrm_read()\n");
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 1);
    if(uprv_memcmp(buffer, gotBuffer, 1) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 1);
        printf("\nExpected:");
        printUSeqErr(buffer, 1);

    } 
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 5);
    if(uprv_memcmp(buffer+1, gotBuffer, 5) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 5);
        printf("\nExpected:");
        printUSeqErr(buffer+1, 5);

    } 
    uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 8);
    if(uprv_memcmp(buffer+6, gotBuffer, 8) != 0){
        log_err("uprv_mstrm_read() failed\n");
        printf("\nGot:");
        printUSeqErr(gotBuffer, 8);
        printf("\nExpected:");
        printUSeqErr(buffer+6, 8);


    } 
    /*try to read outside the limit*/
    /*It just reads untill the limit and sets the error and eof flags*/
    x=uprv_mstrm_read(memStream, (uint8_t*)gotBuffer, 5);
    if(uprv_memcmp(buffer+14, gotBuffer, 2) != 0){
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
    if(uprv_memcmp(buffer, gotBuffer, sizeof(buffer)/sizeof(buffer[0])) != 0){
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

/*
Test the following APIs:
uprv_mstrm_write8
uprv_mstrm_write16
uprv_mstrm_write32
uprv_mstrm_writeString
uprv_mstrm_writeUString
uprv_mstrm_writePadding
uprv_mstrm_writeBlock
uprv_mstrm_getCurrentBuffer
uprv_mstrm_jump
uprv_mstrm_skip
*/    
    {
      uint8_t byteValue = 0x12;
      uint16_t wordValue = 0x2112;
      uint32_t wydeValue = 0x12211221;
      uint32_t wydeRead = 0;
      const char* stringVal = "This is a string";
      UChar UCharBuff[256];
      const UChar* ucharVal = UCharBuff;
      const uint8_t *data = NULL;
      int32_t bufLen = 0;

      u_unescape("This is an Unicode String", UCharBuff, 256);

      memStream=uprv_mstrm_openNew(size);
      if(memStream == NULL){
          log_err("uprv_mstrm_openNew() failed\n");
      }
      uprv_mstrm_write8(memStream, byteValue);
      uprv_mstrm_writePadding(memStream, 3);
      uprv_mstrm_write16(memStream, wordValue);
      uprv_mstrm_writePadding(memStream, 2);
      uprv_mstrm_write32(memStream, wydeValue);
      uprv_mstrm_writeBlock(memStream, &wydeValue, 4);

      uprv_mstrm_writeString(memStream, stringVal, -1);
      uprv_mstrm_writeString(memStream, stringVal, strlen(stringVal));
      uprv_mstrm_writeUString(memStream, ucharVal, -1);
      uprv_mstrm_writeUString(memStream, ucharVal, u_strlen(ucharVal));

      /* Now, lets get the values back */
      data = uprv_mstrm_getBuffer(memStream, &bufLen);

      if(data == NULL || bufLen == 0) {
        log_err("get Buffer failed!\n");
      } else {
        if(byteValue != *(uint8_t *)data) {
          log_err("Failed getting byte value\n");
        }
        data += 4; /* skip byte and 3 padding */
        if(wordValue != *(uint16_t *)data) {
          log_err("Failed getting word value\n");
        }
        data += 4; /* skip word and 2 padding */

        if(wydeValue != *(uint32_t *)data) {
          log_err("Failed getting word value\n");
        }
        data += 4; /* skip wyde */

        if(wydeValue != *(uint32_t *)data) {
          log_err("Failed getting word value\n");
        }
        data += 4; /* skip wyde */

        if(strncmp(stringVal, (char *)data, strlen(stringVal)) != 0) {
          log_err("String was not written correctly\n");
        }
        data += strlen(stringVal);

        if(strncmp(stringVal, (char *)data, strlen(stringVal)) != 0) {
          log_err("String was not written correctly\n");
        }
        data += strlen(stringVal);

        if(u_strncmp(ucharVal, (UChar *)data, u_strlen(ucharVal)) != 0) {
          log_err("UString was not written correctly\n");
        }
        data += u_strlen(ucharVal)*2;

        if(u_strncmp(ucharVal, (UChar *)data, u_strlen(ucharVal)) != 0) {
          log_err("UString was not written correctly\n");
        }
        data += u_strlen(ucharVal)*2;

        uprv_mstrm_skip(memStream, 8); /* skip to first wyde */
        bufLen = uprv_mstrm_read(memStream, &wydeRead, 4);
        if(bufLen != 4 || wydeValue != wydeRead) {
          log_err("Reading after skip failed\n");
        }

        /* this should get us to the second wyde */
        data = uprv_mstrm_getCurrentBuffer(memStream, &bufLen);
        if(wydeValue != *(uint32_t *)data) {
          log_err("Failed getting wyde value after getCurrentBuffer\n");
        }

        uprv_mstrm_skip(memStream, -8);
        data = uprv_mstrm_getCurrentBuffer(memStream, &bufLen);
        uprv_mstrm_jump(memStream, data+4);

        data = uprv_mstrm_getCurrentBuffer(memStream, &bufLen);
        if(wydeValue != *(uint32_t *)data) {
          log_err("Failed getting wyde value after getCurrentBuffer\n");
        }






      }


      uprv_mstrm_close(memStream);


    }
   
}
