/*
******************************************************************************
*
*   Copyright (C) 2001-2004, International Business Machines
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
 * WCS functions moved to ustr_wcs.c for better modularization
 *
 *******************************************************************************
 */


#include "unicode/putil.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cmemory.h"
#include "ustr_imp.h"

U_CAPI UChar* U_EXPORT2 
u_strFromUTF32(UChar   *dest,
               int32_t destCapacity, 
               int32_t *pDestLength,
               const UChar32 *src,
               int32_t srcLength,
               UErrorCode *pErrorCode)
{
    int32_t reqLength = 0;
    uint32_t ch =0;
    UChar *pDestLimit =dest+destCapacity;
    UChar *pDest = dest;
    const uint32_t *pSrc = (const uint32_t *)src;

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

     /* Check if the source is null terminated */
    if(srcLength == -1 ){
        while(((ch=*pSrc)!=0) && (pDest < pDestLimit)){
            ++pSrc;
            if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else if(ch<=0x10ffff){
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }else{
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
        while((ch=*pSrc++) != 0){
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }else{
        const uint32_t* pSrcLimit = ((const uint32_t*)pSrc) + srcLength;
        while((pSrc < pSrcLimit) && (pDest < pDestLimit)){
            ch = *pSrc++;
            if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else if(ch<=0x10FFFF){
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }else{
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
        while(pSrc <pSrcLimit){
            ch = *pSrc++;
            reqLength+=UTF_CHAR_LENGTH(ch);
        }
    }

    reqLength += pDest - dest;
    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateUChars(dest,destCapacity,reqLength,pErrorCode); 
    
    return dest;
}


U_CAPI UChar32* U_EXPORT2 
u_strToUTF32(UChar32 *dest, 
             int32_t  destCapacity,
             int32_t  *pDestLength,
             const UChar *src, 
             int32_t  srcLength,
             UErrorCode *pErrorCode)
{
    const UChar* pSrc = src;
    const UChar* pSrcLimit;
    int32_t reqLength=0;
    uint32_t ch=0;
    uint32_t *pDest = (uint32_t *)dest;
    uint32_t *pDestLimit = pDest + destCapacity;
    UChar ch2=0;

    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength==-1) {
        while((ch=*pSrc)!=0 && pDest!=pDestLimit) {
            ++pSrc;
            /*need not check for NUL because NUL fails UTF_IS_TRAIL() anyway*/
            if(UTF_IS_LEAD(ch) && UTF_IS_TRAIL(ch2=*pSrc)) { 
                ++pSrc;
                ch=UTF16_GET_PAIR_VALUE(ch, ch2);
            }
            *(pDest++)= ch;
        }
        while((ch=*pSrc++)!=0) {
            if(UTF_IS_LEAD(ch) && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
            }
            ++reqLength; 
        }
    } else {
        pSrcLimit = pSrc+srcLength;
        while(pSrc<pSrcLimit && pDest<pDestLimit) {
            ch=*pSrc++;
            if(UTF_IS_LEAD(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                ch=UTF16_GET_PAIR_VALUE(ch, ch2);
            }
            *(pDest++)= ch;
        }
        while(pSrc!=pSrcLimit) {
            ch=*pSrc++;
            if(UTF_IS_LEAD(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
            }
            ++reqLength;
        }
    }

    reqLength+=(pDest - (uint32_t *)dest);
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
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((src==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength == -1){
       srcLength = uprv_strlen((char*)pSrc);
    }
    
    while((index < srcLength)&&(pDest<pDestLimit)){
        ch = pSrc[index++];
        if(ch <=0x7f){
            *pDest++=(UChar)ch;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }else if(ch<=0xFFFF){
                *(pDest++)=(UChar)ch;
            }else{
                *(pDest++)=UTF16_LEAD(ch);
                if(pDest<pDestLimit){
                    *(pDest++)=UTF16_TRAIL(ch);
                }else{
                    reqLength++;
                    break;
                }
            }
        }
    }
    /* donot fill the dest buffer just count the UChars needed */
    while(index < srcLength){
        ch = pSrc[index++];
        if(ch <= 0x7f){
            reqLength++;
        }else{
            ch=utf8_nextCharSafeBody(pSrc, &index, srcLength, ch, -1);
            if(ch<0){
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
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

static U_INLINE uint8_t *
_appendUTF8(uint8_t *pDest, UChar32 c) {
    /* c<=0x7f is handled by the caller, here it is 0x80<=c<=0x10ffff */
    if((c)<=0x7ff) {
        *pDest++=(uint8_t)((c>>6)|0xc0);
        *pDest++=(uint8_t)((c&0x3f)|0x80);
    } else if((uint32_t)(c)<=0xffff) {
        *pDest++=(uint8_t)((c>>12)|0xe0);
        *pDest++=(uint8_t)(((c>>6)&0x3f)|0x80);
        *pDest++=(uint8_t)(((c)&0x3f)|0x80);
    } else /* if((uint32_t)(c)<=0x10ffff) */ {
        *pDest++=(uint8_t)(((c)>>18)|0xf0);
        *pDest++=(uint8_t)((((c)>>12)&0x3f)|0x80);
        *pDest++=(uint8_t)((((c)>>6)&0x3f)|0x80);
        *pDest++=(uint8_t)(((c)&0x3f)|0x80);
    }
    return pDest;
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
    uint32_t ch=0,ch2=0;
    uint8_t *pDest = (uint8_t *)dest;
    uint8_t *pDestLimit = pDest + destCapacity;


    /* args check */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)){
        return NULL;
    }
        
    if((pSrc==NULL) || (srcLength < -1) || (destCapacity<0) || (!dest && destCapacity > 0)){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(srcLength==-1) {
        while((ch=*pSrc)!=0 && pDest!=pDestLimit) {
            ++pSrc;
            if(ch <= 0x7f) {
                *pDest++ = (char)ch;
                ++reqLength;
                continue;
            }

            /*need not check for NUL because NUL fails UTF_IS_TRAIL() anyway*/
            if(UTF_IS_SURROGATE(ch)) {
                if(UTF_IS_SURROGATE_FIRST(ch) && UTF_IS_TRAIL(ch2=*pSrc)) { 
                    ++pSrc;
                    ch=UTF16_GET_PAIR_VALUE(ch, ch2);
                } else {
                    /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                    *pErrorCode = U_INVALID_CHAR_FOUND;
                    return NULL;
                }
            }
            reqLength += UTF8_CHAR_LENGTH(ch);
            /* do we have enough room in destination? */
            if(destCapacity< reqLength){
                break;
            }
            /* convert and append*/
            pDest=_appendUTF8(pDest, ch);
        }
        while((ch=*pSrc++)!=0) {
            if(ch<=0x7f) {
                ++reqLength;
            } else if(ch<=0x7ff) {
                reqLength+=2;
            } else if(!UTF_IS_SURROGATE(ch)) {
                reqLength+=3;
            } else if(UTF_IS_SURROGATE_FIRST(ch) && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                reqLength+=4;
            } else {
                /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
    } else {
        pSrcLimit = pSrc+srcLength;
        while(pSrc<pSrcLimit && pDest<pDestLimit) {
            ch=*pSrc++;
            if(ch <= 0x7f) {
                *pDest++ = (char)ch;
                ++reqLength;
                continue;
            }

            if(UTF_IS_SURROGATE(ch)) {
                if(UTF_IS_SURROGATE_FIRST(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) { 
                    ++pSrc;
                    ch=UTF16_GET_PAIR_VALUE(ch, ch2);
                } else {
                    /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                    *pErrorCode = U_INVALID_CHAR_FOUND;
                    return NULL;
                }
            }
            reqLength += UTF8_CHAR_LENGTH(ch);
            /* do we have enough room in destination? */
            if(destCapacity< reqLength){
                break;
            }
            /* convert and append*/
            pDest=_appendUTF8(pDest, ch);
        }
        while(pSrc<pSrcLimit) {
            ch=*pSrc++;
            if(ch<=0x7f) {
                ++reqLength;
            } else if(ch<=0x7ff) {
                reqLength+=2;
            } else if(!UTF_IS_SURROGATE(ch)) {
                reqLength+=3;
            } else if(UTF_IS_SURROGATE_FIRST(ch) && pSrc<pSrcLimit && UTF_IS_TRAIL(ch2=*pSrc)) {
                ++pSrc;
                reqLength+=4;
            } else {
                /* Unicode 3.2 forbids surrogate code points in UTF-8 */
                *pErrorCode = U_INVALID_CHAR_FOUND;
                return NULL;
            }
        }
    }

    if(pDestLength){
        *pDestLength = reqLength;
    }

    /* Terminate the buffer */
    u_terminateChars((char*)dest,destCapacity,reqLength,pErrorCode);

    return (char*)dest;
}
