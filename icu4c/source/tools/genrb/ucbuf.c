/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ucbuf.c
*
* Modification History:
*
*   Date        Name        Description
*   05/10/01    Ram         Creation.
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "filestrm.h"
#include "cmemory.h"
#include "unicode/utrans.h"
#include "unicode/ustring.h"
#include "ucbuf.h"
#define MAX_BUF 1000

UBool 
ucbuf_autodetect(FileStream* in,const char** cp){
  UBool autodetect = FALSE;
  char start[3];
  int cap =T_FileStream_size(in);
  T_FileStream_read(in, start, 3);
  if(start[0] == '\xFE' && start[1] == '\xFF') {
      *cp = "UTF16_BigEndian";
      autodetect = TRUE;
  } else if(start[0] == '\xFF' && start[1] == '\xFE') {
      *cp = "UTF16_LittleEndian";
      autodetect = TRUE;
  } else if(start[0] == '\xEF' && start[1] == '\xBB' && start[2] == '\xBF') {
      *cp = "UTF8";
      autodetect = TRUE;
  }
  if(!autodetect){
      T_FileStream_rewind(in);
  }
  return autodetect;
}

UCHARBUF* 
ucbuf_fillucbuf( UCHARBUF* buf,UErrorCode* err){
    UChar* pTarget=NULL;
    UChar* target=NULL;
    const char* source=NULL;
    char* cbuf =NULL;
    int32_t numConverted =0;
    int32_t limit=0;
    int numRead=0;
    int numWritten=0;
    int offset=0;

    cbuf =(char*)uprv_malloc(sizeof(char) * MAX_BUF);
    if(buf->buffer==NULL){
        /* allocate buffers */
        pTarget = (UChar*) uprv_malloc(sizeof(UChar)* MAX_BUF);
    }else{
        pTarget = buf->buffer;
        /* check if we arrived here without exhausting the buffer*/
        if(buf->currentPos<buf->bufLimit){
            offset= buf->bufLimit-buf->currentPos;
            memmove(buf->buffer,buf->currentPos,offset* sizeof(UChar));
            memset(pTarget+offset,0xff,sizeof(UChar)*(MAX_BUF-offset));
        }else{
            memset(pTarget,0xff,sizeof(UChar)*MAX_BUF);
        }

    }
    
    /* read the file */
    numRead=T_FileStream_read(buf->in,cbuf,MAX_BUF-offset);
    buf->remaining-=numRead;

    target=pTarget;
    /* convert the bytes */
    if(buf->conv){
        /* since state is saved in the converter we add offset to source*/
        target = pTarget+offset;
        source = cbuf;
        ucnv_toUnicode(buf->conv,&target,target+numRead,&source,source+numRead,NULL,FALSE,err);
        numRead= target-pTarget;
        if(U_FAILURE(*err)){
            return NULL;
        }
    }else{
        u_charsToUChars(cbuf,target+offset,numRead);
        numRead=((buf->remaining>MAX_BUF)? MAX_BUF:numRead+offset);
    }
    buf->buffer= pTarget;
    buf->currentPos = pTarget;
    buf->bufLimit=pTarget+numRead;
    return buf;
}

UChar32 
ucbuf_getc(UCHARBUF* buf,UErrorCode* err){
    UChar32 c =0;
    if(buf->currentPos<buf->bufLimit){
         c = *(buf->currentPos);
        buf->currentPos++;
        return c;
    }else{
        if(buf->remaining==0){
            return U_EOF;
        }
        buf=ucbuf_fillucbuf(buf,err);
        if(U_FAILURE(*err)){
            return U_EOF;
        }
        c = *(buf->currentPos);
        buf->currentPos++;
        return c;   
    }

    return 0;
}


/* u_unescapeAt() callback to return a UChar*/
static UChar 
_charAt(int32_t offset, void *context) {
    return ((UCHARBUF*) context)->currentPos[offset];
}

UChar32
ucbuf_getcx(UCHARBUF* buf,UErrorCode* err) {
    int32_t length;
    int32_t offset;
    UChar32 c32;
    UChar c16;
    
    /* Fill the buffer if it is empty */
    if (buf->currentPos >=buf->bufLimit) {
        ucbuf_fillucbuf(buf,err);        
    }

    /* Get the next character in the buffer */
    if (buf->currentPos < buf->bufLimit) {
        c16 = *(buf->currentPos)++;
    } else {
        c16 = U_EOF;
    }

    /* If it isn't a backslash, return it */
    if (c16 != 0x005C /*'\\'*/) {
        return c16;
    }

    /* Determine the amount of data in the buffer */
    length = buf->bufLimit-buf->currentPos;
    
    /* The longest escape sequence is \Uhhhhhhhh; make sure
       we have at least that many characters */
    if (length < 10) {

        /* fill the buffer */
        ucbuf_fillucbuf(buf,err);
        length = buf->bufLimit-buf->buffer;
    }
    
    /* Process the escape */
    offset = 0;
    c32 = u_unescapeAt(_charAt, &offset, length, (void*)buf);

    /* Update the current buffer position */
    buf->currentPos += offset;

    return c32;
}


UCHARBUF* 
ucbuf_open(FileStream* in, const char* cp,UErrorCode* err){

    UCHARBUF* buf =(UCHARBUF*) uprv_malloc(sizeof(UCHARBUF));
    if(U_FAILURE(*err)){
        return NULL;
    }
    if(buf){
        buf->in=in;
        buf->fileLen = T_FileStream_size(in);
        buf->remaining=buf->fileLen;
        buf->buffer=NULL;
        buf->currentPos=NULL;
        buf->bufLimit=NULL;
        if(*cp!='\0'){
            buf->conv=ucnv_open(cp,err);
        }else{
            buf->conv=NULL;
        }
        if(U_FAILURE(*err)){
            return NULL;
        }
        buf=ucbuf_fillucbuf(buf,err);
        return buf;
    }else{
        *err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
}

void 
ucbuf_closebuf(UCHARBUF* buf){
    uprv_free(buf->buffer);
}

void 
ucbuf_ungetc(UChar32 c,UCHARBUF* buf){
    if(buf->currentPos!=buf->buffer){
        buf->currentPos--;
    }

}

void 
ucbuf_close(UCHARBUF* buf){
    if(buf->conv){
        ucnv_close(buf->conv);
    }
    buf->in=NULL;
    buf->currentPos=NULL;
    buf->bufLimit=NULL;
    ucbuf_closebuf(buf);
    uprv_free(buf);
}
