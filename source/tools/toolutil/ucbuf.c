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
#include "unicode/ustring.h"
#include "ucbuf.h"

#define MAX_IN_BUF 1000
#define MAX_U_BUF 1500

static UBool ucbuf_autodetect_nrw(FileStream* in, const char** cp,int* numRead){
  
  char start[4]={'\0'};
  int cap =T_FileStream_size(in);
  UBool autodetect =FALSE;
  int i=4;
  *numRead=4;
  *cp="";
  if(cap>0){
       T_FileStream_read(in, start, 4);
       if(start[0] == '\xFE' && start[1] == '\xFF') {
           *cp = "UTF16_BigEndian";
           autodetect = TRUE;
       } else if(start[0] == '\xFF' && start[1] == '\xFE') {
           *cp = "UTF16_LittleEndian";
           *numRead-=2;
           if(start[2] == '\x00' && start[3] =='\0x00'){
               *cp="UTF32_LittleEndian";
               *numRead+=2;
           }
           autodetect = TRUE;
       } else if(start[0] == '\xEF' && start[1] == '\xBB' && start[2] == '\xBF') {
           *cp = "UTF8";
           *numRead-=1;
           autodetect = TRUE;
       }else if(start[0] == '\x0E' && start[1] == '\xFE' && start[2] == '\xFF'){
           *cp ="SCSU";
           *numRead-=1;
           autodetect = TRUE;
       }else if(start[0] == '\x00' && start[1] == '\x00' && start[2] == '\xFF' && start[3]=='\xFE'){
            *cp = "UTF32_BigEndian";
            autodetect =TRUE;
       }else{
           *numRead =0;
       }
  }
  while(i> *numRead){
    T_FileStream_ungetc(start[i-1],in);
    i--;
  }
  return autodetect;
}

/* Autodetects UTF8, UTF-16-BigEndian and UTF-16-LittleEndian BOMs*/
U_CAPI UBool U_EXPORT2
ucbuf_autodetect(FileStream* in,const char** cp){
  UBool autodetect = FALSE;
  int numRead =0;
  char* tcp ="";
  autodetect=ucbuf_autodetect_nrw(in,&tcp, &numRead);
  *cp =tcp;
  /* rewind the file Stream */
  T_FileStream_rewind(in);
  return autodetect;
}

/* fill the uchar buffer */
static UCHARBUF* 
ucbuf_fillucbuf( UCHARBUF* buf,UErrorCode* err){
    UChar* pTarget=NULL;
    UChar* target=NULL;
    const char* source=NULL;
    char  cbuf[MAX_IN_BUF] = {'\0'};
    int numRead=0;
    int offset=0;

    pTarget = buf->buffer;
    /* check if we arrived here without exhausting the buffer*/
    if(buf->currentPos<buf->bufLimit){
        offset= buf->bufLimit-buf->currentPos;
        memmove(buf->buffer,buf->currentPos,offset* sizeof(UChar));
    }

#if DEBUG
    memset(pTarget+offset,0xff,sizeof(UChar)*(MAX_IN_BUF-offset));
#endif
    
    /* read the file */
    numRead=T_FileStream_read(buf->in,cbuf,MAX_IN_BUF-offset);
    buf->remaining-=numRead;

    target=pTarget;
    /* convert the bytes */
    if(buf->conv){
        /* since state is saved in the converter we add offset to source*/
        target = pTarget+offset;
        source = cbuf;
        ucnv_toUnicode(buf->conv,&target,target+(MAX_U_BUF-offset),&source,source+numRead,NULL,(UBool)(buf->remaining==0),err);
        numRead= target-pTarget;
        if(U_FAILURE(*err)){
            return NULL;
        }
    }else{
        u_charsToUChars(cbuf,target+offset,numRead);
        numRead=((buf->remaining>MAX_IN_BUF)? MAX_IN_BUF:numRead+offset);
    }
    buf->currentPos = pTarget;
    buf->bufLimit=pTarget+numRead;
    return buf;
}

/* get a UChar from the stream*/
U_CAPI UChar32 U_EXPORT2
ucbuf_getc(UCHARBUF* buf,UErrorCode* err){
    if(buf->currentPos>=buf->bufLimit){
        if(buf->remaining==0){
            return U_EOF;
        }
        buf=ucbuf_fillucbuf(buf,err);
        if(U_FAILURE(*err)){
            return U_EOF;
        }
    }

    return *(buf->currentPos++);
}


/* u_unescapeAt() callback to return a UChar*/
static UChar 
_charAt(int32_t offset, void *context) {
    return ((UCHARBUF*) context)->currentPos[offset];
}

/* getc and escape it */
U_CAPI UChar32 U_EXPORT2
ucbuf_getcx(UCHARBUF* buf,UErrorCode* err) {
    int32_t length;
    int32_t offset;
    UChar32 c32,c1;
    
    /* Fill the buffer if it is empty */
    if (buf->currentPos >=buf->bufLimit) {
        ucbuf_fillucbuf(buf,err);
    }

    /* Get the next character in the buffer */
    if (buf->currentPos < buf->bufLimit) {
        c1 = *(buf->currentPos)++;
    } else {
        c1 = U_EOF;
    }

    /* If it isn't a backslash, return it */
    if (c1 != 0x005C /*|| *(buf->currentPos+1)==0x005C /*'\\'*/) {
        return c1;
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

/* open a UCHARBUF */
U_CAPI UCHARBUF* U_EXPORT2
ucbuf_open(FileStream* in, UErrorCode* err){

    UCHARBUF* buf =(UCHARBUF*) uprv_malloc(sizeof(UCHARBUF));
    char *cp ="";
    int numRead =0;
    if(U_FAILURE(*err)){
        return NULL;
    }
    if(buf){
        buf->in=in;
        ucbuf_autodetect_nrw(in,&cp,&numRead);
        buf->remaining=T_FileStream_size(in)-numRead;
        buf->buffer=(UChar*) uprv_malloc(sizeof(UChar)* MAX_U_BUF);
		if (buf->buffer == NULL) {
			*err = U_MEMORY_ALLOCATION_ERROR;
			return NULL;
		}
        buf->currentPos=buf->buffer;
        buf->bufLimit=buf->buffer;
        if(*cp!='\0'){
            buf->conv=ucnv_open(cp,err);
        }else{
            buf->conv=NULL;
        }
        if(U_FAILURE(*err)){
            fprintf(stderr, "Could not open codepage [%s]: %s\n", cp, u_errorName(*err));
            return NULL;
        }
        buf=ucbuf_fillucbuf(buf,err);
        return buf;
    }else{
        *err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
}

/* TODO: this method will fail if at the 
 * begining of buffer and the uchar to unget
 * is from the previous buffer. Need to implement
 * system to take care of that situation.
 */
U_CAPI void U_EXPORT2
ucbuf_ungetc(UChar32 c,UCHARBUF* buf){
    /* decrement currentPos pointer
     * if not at the begining of buffer
     */
    if(buf->currentPos!=buf->buffer){
        buf->currentPos--;
    }
}

/* frees the resources of UChar* buffer */
static void 
ucbuf_closebuf(UCHARBUF* buf){
    uprv_free(buf->buffer);
    buf->buffer = NULL;
}

/* close the buf and release resources*/
U_CAPI void U_EXPORT2
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

/* rewind the buf and file stream */
U_CAPI void U_EXPORT2
ucbuf_rewind(UCHARBUF* buf){
    if(buf){
        const char* cp="";
        buf->currentPos=buf->buffer;
        buf->bufLimit=buf->buffer;
        ucnv_reset(buf->conv);
        T_FileStream_rewind(buf->in);
        ucbuf_autodetect(buf->in,&cp);
        buf->remaining=T_FileStream_size(buf->in);
    }
}
