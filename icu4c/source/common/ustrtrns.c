/*
******************************************************************************
*
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File ustrtrns.c
*
* Modification History:
*
*   Date        Name        Description
*   9/10/2001    Ram    Creation.
******************************************************************************
*/

/*******************************************************************************
 *
 * u_strTo* and u_strFrom* APIs
 *
 *******************************************************************************
 */


#include "unicode/putil.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cwchar.h"
#include "cmemory.h"
#include "ustr_imp.h"


U_CAPI UBool /* U_CALLCONV U_EXPORT2 */
u_growAnyBufferFromStatic(void *context,
                       void **pBuffer, int32_t *pCapacity, int32_t reqCapacity,
                       int32_t length, int32_t size) {

    void *newBuffer=uprv_malloc(reqCapacity*size);
    if(newBuffer!=NULL) {
        if(length>0) {
            uprv_memcpy(newBuffer, *pBuffer, length*size);
        }
        *pCapacity=reqCapacity;
    } else {
        *pCapacity=0;
    }

    /* release the old pBuffer if it was not statically allocated */
    if(*pBuffer!=(void *)context) {
        uprv_free(*pBuffer);
    }

    *pBuffer=newBuffer;
    return (UBool)(newBuffer!=NULL);
}

#define _STACK_BUFFER_CAPACITY 1000

U_CAPI UChar* U_EXPORT2 
u_strFromUTF32(UChar   *dest,
               int32_t destCapacity, 
               int32_t *pDestLength,
               const uint32_t *pSrc,
               int32_t srcLength,
               UErrorCode *pErrorCode){
    
    int32_t reqLength = 0;
    uint32_t ch =0;
    UChar *pDestLimit =dest+destCapacity;
    UChar *pDest = dest;
    
    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

     /* Check if the source is null terminated */
    if(srcLength == -1 ){
        while((ch=*pSrc)!=0){
            if(pDest < pDestLimit){
                ++pSrc;
                if(ch<=0xFFFF){
                    *(pDest++)=(UChar)ch;
                }else{
                    *(pDest++)=(uint16_t)((ch>>10)+0xd7c0);   /*UTF_FIRST_SURROGATE(ch);*/
                    if(pDest<pDestLimit){
                        *(pDest++)=(uint16_t)((ch&0x3ff)|0xdc00); /*UTF_SECOND_SURROGATE(ch);*/
                    }else{
                        reqLength++;
                        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }else{
                *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
        while((ch=*pSrc++) != 0){
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }else{
        const uint32_t* pSrcLimit = pSrc + srcLength;
        while(pSrc<pSrcLimit){
            if(pDest < pDestLimit){
                ch = *pSrc++;
                if(ch<=0xFFFF){
                    *(pDest++)=(UChar)ch;
                }else{
                    *(pDest++)=(uint16_t)((ch>>10)+0xd7c0);   /*UTF_FIRST_SURROGATE(ch);*/
                    if(pDest<pDestLimit){
                        *(pDest++)=(uint16_t)((ch&0x3ff)|0xdc00); /*UTF_SECOND_SURROGATE(ch);*/
                    }else{
                        reqLength++;
                        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }else{
                *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
        while(pSrc <pSrcLimit){
            ch = *pSrc++;
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    /* return the required length */
    reqLength+=(pDest - dest);

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode); 
    
    return dest;
}


U_CAPI uint32_t* U_EXPORT2 
u_strToUTF32(uint32_t *dest, 
             int32_t  destCapacity,
             int32_t  *pDestLength,
             const UChar *src, 
             int32_t  srcLength,
             UErrorCode *pErrorCode){

    const UChar* pSrc = src;
    int32_t reqLength=0;
    uint32_t ch=0;
    int32_t index=0;
    uint32_t *pDest = dest;
    uint32_t *pDestLimit = dest + destCapacity;

    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength == -1){
        srcLength = u_strlen(pSrc);
    }

    while(index < srcLength){
        if(pDest <pDestLimit){
            UTF16_NEXT_CHAR_SAFE(src, index, srcLength, ch, TRUE);
            if(ch!=UTF_ERROR_VALUE){
                *(pDest++)= ch;
            }else{
                *pErrorCode = U_ILLEGAL_CHAR_FOUND;
                return NULL;
            }
        }else{
            *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    /* donot fill the dest buffer just count the UChars needed */
    while(index < srcLength){
        UTF16_NEXT_CHAR_SAFE(src, index, srcLength, ch, TRUE);
        if(ch!=UTF_ERROR_VALUE){
            reqLength++;
        }else{
            *pErrorCode = U_ILLEGAL_CHAR_FOUND;
            return NULL;
        }
    }

    reqLength+=(pDest - dest);
    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChar32s(dest,destCapacity,reqLength,pErrorCode);

    return dest;
}

U_CAPI UChar* U_EXPORT2
u_strFromUTF8(UChar *dest,             
              int32_t destCapacity,
              int32_t *pDestLength,
              const char* src, 
              int32_t srcLength,
              UErrorCode *pErrorCode){

    UChar *pDest = dest;
    UChar *pDestLimit = dest+destCapacity;
    UChar32 ch=0;
    int32_t index = 0;
    int32_t reqLength = 0;
    uint8_t* pSrc = (uint8_t*) src;
    
    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength == -1){
       srcLength = uprv_strlen((char*)pSrc);
    }
    
    while(index < srcLength){
        if(pDest <pDestLimit){
            ch = pSrc[index++];
            if(ch <=0x7f){
                *pDest++=(UChar)ch;
            }else{
                ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, FALSE);
                if(ch<=0xFFFF){
                    *(pDest++)=(UChar)ch;
                }else{
                    *(pDest++)=(uint16_t)((ch>>10)+0xd7c0);   /*UTF_FIRST_SURROGATE(ch);*/
                    if(pDest<pDestLimit){
                        *(pDest++)=(uint16_t)((ch&0x3ff)|0xdc00); /*UTF_SECOND_SURROGATE(ch);*/
                    }else{
                        reqLength++;
                        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }
        }else{
            *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
    /* donot fill the dest buffer just count the UChars needed */
    while(index < srcLength){
        ch = pSrc[index++];
        if(ch <= 0x7f){
            reqLength++;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, FALSE);
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    reqLength+=(pDest - dest);

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode);

    return dest;
}
    
U_CAPI char* U_EXPORT2 
u_strToUTF8(char *dest,           
            int32_t destCapacity,
            int32_t *pDestLength,
            const UChar *pSrc, 
            int32_t srcLength,
            UErrorCode *pErrorCode){

    int32_t reqLength=0;
    const UChar *pSrcLimit;
    uint32_t ch;

    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength == -1){
        while(*pSrc!=0){
            if(reqLength < destCapacity){
                ch = *pSrc++;
                if(ch<=0x7f) { 
                    dest[reqLength++]=(uint8_t)ch; 
                } else { 
                    int num = utf8_appendCharSafeBody((uint8_t*)dest, reqLength, destCapacity, ch); 
                    if(num==reqLength){
                        *pErrorCode =U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    reqLength = num;
                }
            }else{
                *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
        while(*pSrc!=0){
            ch = *pSrc++;
            reqLength+=UTF8_CHAR_LENGTH(ch);
        }
    }else{
        pSrcLimit = pSrc + srcLength;
        while(pSrc < pSrcLimit){
            if(reqLength  < destCapacity){
                ch = *pSrc++;
                if(ch<=0x7f) { 
                    dest[reqLength++]=(uint8_t)ch; 
                } else { 
                    int num = utf8_appendCharSafeBody((uint8_t*)dest, reqLength, destCapacity, ch); 
                    if(num==reqLength){
                        *pErrorCode =U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                    reqLength = num;
                }
            }else{
                *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
        while(pSrc < pSrcLimit){
            ch = *pSrc++;
            reqLength+=UTF8_CHAR_LENGTH(ch);
        }
    }
    if(destCapacity==0){
        reqLength+=destCapacity;
    }
    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateChars((char*)dest,destCapacity,reqLength,pErrorCode);

    return (char*)dest;
}

/* helper function */
static wchar_t* 
_strToWCS(wchar_t *dest, 
           int32_t destCapacity,
           int32_t *pDestLength,
           const UChar *src, 
           int32_t srcLength,
           UErrorCode *pErrorCode){

    char stackBuffer [_STACK_BUFFER_CAPACITY];
    char* tempBuf = stackBuffer;
    int32_t tempBufCapacity = _STACK_BUFFER_CAPACITY;
    char* tempBufLimit = stackBuffer + tempBufCapacity;
    UConverter* conv = NULL;
    char* saveBuf = tempBuf;
    wchar_t* intTarget=NULL;
    int count=0,retVal=0;
    
    const UChar *pSrcLimit =NULL;
    const UChar *pSrc = src;
    pSrcLimit = pSrc + srcLength;
    conv = u_getDefaultConverter(pErrorCode);
    do{
        /* convert to chars using default converter */
        ucnv_fromUnicode(conv,&tempBuf,tempBufLimit,&pSrc,pSrcLimit,NULL,FALSE,pErrorCode);
        
        /* This should rarely occur */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR){
            int32_t count = tempBuf - saveBuf;
            *pErrorCode=U_ZERO_ERROR;
            tempBuf = saveBuf;
            /* we dont have enough room on the stack grow the buffer */
            u_growAnyBufferFromStatic(stackBuffer,(void**) &tempBuf, &tempBufCapacity, 
                    (tempBufCapacity+_STACK_BUFFER_CAPACITY), count,sizeof(char));
           
           saveBuf = tempBuf;
           tempBufLimit = tempBuf + tempBufCapacity;
           tempBuf = tempBuf + count;
           /* to flush the converters internal state when pSrc<pSrcLimit */
           continue;

        }

    }while (pSrc<pSrcLimit);
    
    count =(tempBuf - saveBuf);

    /* allocate more space than required 
     * here we assume that every char requires 
     * no more than 2 wchar_ts
     */
    intTarget = (wchar_t*)uprv_malloc( ((count * 2)+1/* for null termination */) * sizeof(wchar_t) );

    if(intTarget){

        int nulLen = 0;
        wchar_t* pIntTarget=intTarget;

        /* null terminate the UChar buffer */
        u_terminateChars((char*)saveBuf,(tempBufLimit-saveBuf),count,pErrorCode);
        
        tempBuf = saveBuf;
        
        /* now convert the mbs to wcs */
        for(;;){
            
            /* we can call the system API since we are sure that
             * there is atleast 1 null in the input
             */
            retVal = uprv_mbstowcs(pIntTarget,(tempBuf+nulLen),(count+1));
            
            if(retVal==-1){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                break;
            }

            /*scan for nulls */
            /* we donot check for limit since tempBuf is null terminated */
            while(tempBuf[nulLen++] != 0){
            }
            
            /* check if we have reached the source limit*/
            if(nulLen>=(count)){
                break;
            }
            pIntTarget = pIntTarget + retVal+1/* for terminating null*/;
        }
        
        if(count < destCapacity){
            uprv_memcpy(dest,intTarget,count*sizeof(wchar_t));

        }else{
            *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        }  

        if(pDestLength){
            *pDestLength = count;
        }

        /* free the allocated memory */
        uprv_free(intTarget);

    }else{
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
    }
    
    /* are we still using stack buffer */
    if(stackBuffer != saveBuf){
        uprv_free(saveBuf);
    }
    u_terminateWChars(dest,destCapacity,count,pErrorCode);

    u_releaseDefaultConverter(conv);

    return dest;
}

U_CAPI wchar_t* U_EXPORT2
u_strToWCS(wchar_t *dest, 
           int32_t destCapacity,
           int32_t *pDestLength,
           const UChar *src, 
           int32_t srcLength,
           UErrorCode *pErrorCode){
    
    const UChar *pSrc = src;

    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    
    if(srcLength == -1){
        srcLength = u_strlen(pSrc);
    }

#ifdef U_WCHAR_IS_UTF16
    /* wchar_t is UTF-16 just do a memcpy */
    if(srcLength==-1){
        srcLength =0;
        while(pSrc[srcLength++]!=0){
        }
    }
    if(srcLength <= destCapacity){
        uprv_memcpy(dest,src,srcLength*U_SIZEOF_UCHAR);
    }else{
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
    }
    if(pDestLength){
       *pDestLength = srcLength;
    }

    u_terminateUChars(dest,destCapacity,srcLength,pErrorCode);

    return dest;;
#elif defined U_WCHAR_IS_UTF32
    
    return u_strToUTF32(dest,destCapacity,pDestLength,src,srcLength,pErrorCode);

#else
    
    return _strToWCS(dest,destCapacity,pDestLength,src,srcLength, pErrorCode);
    
#endif
}

/* helper function */
static UChar* 
_strFromWCS( UChar   *dest,
             int32_t destCapacity, 
             int32_t *pDestLength,
             const wchar_t *src,
             int32_t srcLength,
             UErrorCode *pErrorCode){

    int32_t retVal =0, count =0 ;
    UConverter* conv = NULL;
    UChar* pTarget = NULL;
    UChar* pTargetLimit = NULL;
    
    UChar uStack [_STACK_BUFFER_CAPACITY];

    wchar_t wStack[_STACK_BUFFER_CAPACITY];
    wchar_t* pWStack = wStack;


    char cStack[_STACK_BUFFER_CAPACITY];
    int32_t cStackCap = _STACK_BUFFER_CAPACITY;
    char* pCSrc=cStack;
    char* pCSave=pCSrc;
    char* pCSrcLimit=NULL;

    const wchar_t* pSrc = src;
    const wchar_t* pSrcLimit = NULL;

    if(srcLength ==-1){
        /* if the wchar_t source is null terminated we can safely
         * assume that there are no embedded nulls, this is a fast
         * path for null terminated strings.
         */
        for(;;){
            /* convert wchars  to chars */
            retVal = uprv_wcstombs(pCSrc,src, cStackCap);
    
            if(retVal == -1){
                *pErrorCode = U_ILLEGAL_CHAR_FOUND;
                goto cleanup;
            }else if(retVal == cStackCap){
                /* Should rarely occur */
                u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                    cStackCap*2,0,sizeof(char));
                pCSave = pCSrc;
            }else{
                /* converted every thing */
                pCSrc = pCSrc+retVal;
                break;
            }
        }
        
    }else{
        /* here the source is not null terminated 
         * so it may have nulls embeded and we need to
         * do some extra processing 
         */
        int32_t remaining =cStackCap;
        
        pSrcLimit = src + srcLength;

        for(;;){
            int32_t nulLen = 0;
            
            /* find nulls in the string */
            while((pSrc+nulLen)<pSrcLimit && pSrc[nulLen++]!=0){
            }


            if((pSrc+nulLen != pSrcLimit)){
                /* check if we have enough room in pCSrc */
                if(remaining < (nulLen * MB_CUR_MAX)){
                    /* should rarely occur */
                    int32_t len = (pCSrc-pCSave);
                    pCSrc = pCSave;
                    /* we do not have enough room so grow the buffer*/
                    u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                           2*cStackCap+(nulLen*MB_CUR_MAX),len,sizeof(char));

                    pCSave = pCSrc;
                    pCSrc = pCSave+len;
                    remaining = cStackCap-(pCSrc - pCSave);
                }

                /* we have found a null  so convert the 
                 * chunk from begining of non-null char to null
                 */
                retVal = uprv_wcstombs(pCSrc,pSrc,remaining);

                if(retVal==-1){
                    /* an error occurred bail out */
                    *pErrorCode = U_ILLEGAL_CHAR_FOUND;
                    goto cleanup;
                }

                pCSrc = pCSrc + retVal+1 /* already null terminated */;

                pSrc = pSrc+nulLen; /* skip past the null */
            
                remaining -= (pCSrc-pCSave);


            }else{
                /* the source is not null terminated and we are 
                 * end of source so we copy the source to a temp buffer
                 * null terminate it and convert wchar_ts to chars
                 */
                if(nulLen > _STACK_BUFFER_CAPACITY){
                    /* Should rarely occcur */
                    /* allocate new buffer buffer */
                    pWStack =(wchar_t*) uprv_malloc(sizeof(wchar_t) * nulLen);
                }
                /* copy the contents to tempStack */
                uprv_memcpy(pWStack,pSrc,nulLen*sizeof(wchar_t));
            
                /* null terminate the tempBuffer */
                pWStack[nulLen] =0 ;
            
                if(remaining < (nulLen * MB_CUR_MAX)){
                    /* Should rarely occur */
                    int32_t len = (pCSrc-pCSave);
                    pCSrc = pCSave;
                    /* we do not have enough room so grow the buffer*/
                    u_growAnyBufferFromStatic(cStack,(void**)&pCSrc,&cStackCap,
                           cStackCap+(nulLen*MB_CUR_MAX),len,sizeof(char));

                    pCSave = pCSrc;
                    pCSrc = pCSave+len;
                    remaining = cStackCap-(pCSrc - pCSave);
                }
                /* convert to chars */
                retVal = uprv_wcstombs(pCSrc,pWStack,remaining);
            
                pCSrc = pCSrc+retVal +1;
                pSrc  = pSrc + nulLen;
            }

            /* ran out of input, break */
            if(pSrc >= pSrcLimit){
                break;
            }

        }

    }

    /* OK..now we have converted from wchar_ts to chars now 
     * convert chars to UChars 
     */
    pCSrcLimit = pCSrc;
    pCSrc = pCSave;
    pTarget = dest;
    pTargetLimit = dest + destCapacity;    
    
    conv= u_getDefaultConverter(pErrorCode);
    
    /* convert and write to the target */
    ucnv_toUnicode(conv,&pTarget,pTargetLimit,(const char**)&pCSrc,pCSrcLimit,NULL,FALSE,pErrorCode);
    /* count the number converted */
    count=pTarget - dest;

    while(*pErrorCode ==U_BUFFER_OVERFLOW_ERROR){
        
        *pErrorCode = U_ZERO_ERROR;

        pTarget = uStack;
        pTargetLimit = uStack + _STACK_BUFFER_CAPACITY;
        
        /* convert to stack buffer*/
        ucnv_toUnicode(conv,&pTarget,pTargetLimit,(const char**)&pCSrc,pCSrcLimit,NULL,FALSE,pErrorCode);
        
        /* increment count to number written to stack */
        count+= pTarget - uStack;
    }  
    
    if(pDestLength){
        *pDestLength =count;
    }

    u_terminateUChars(dest,destCapacity,count,pErrorCode);
    
cleanup:
 
    if(cStack != pCSave){
        uprv_free(pCSave);
    }

    if(wStack != pWStack){
        uprv_free(pWStack);
    }
    
    u_releaseDefaultConverter(conv);

    return dest;
}

U_CAPI UChar* U_EXPORT2
u_strFromWCS(UChar   *dest,
             int32_t destCapacity, 
             int32_t *pDestLength,
             const wchar_t *src,
             int32_t srcLength,
             UErrorCode *pErrorCode){
    
    const wchar_t* pSrc = src;


    /* args check */
    if(pErrorCode && U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

#ifdef U_WCHAR_IS_UTF16
    /* wchar_t is UTF-16 just do a memcpy */
    if(srcLength==-1){
        srcLength =0;
        while(pSrc[srcLength++]!=0){
        }
    }
    if(srcLength <= destCapacity){
        uprv_memcpy(dest,src,srcLength*U_SIZEOF_UCHAR);
    }else{
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
    }
    if(pDestLength){
       *pDestLength = srcLength;
    }

    u_terminateUChars(dest,destCapacity,srcLength,pErrorCode);

    return dest;

#elif defined U_WCHAR_IS_UTF32
    
    return u_strFromUTF32(dest,destCapacity,pDestLength,src,srcLength,pErrorCode);

#else

    return _strFromWCS(dest,destCapacity,pDestLength,src,srcLength,pErrorCode);  

#endif

}
