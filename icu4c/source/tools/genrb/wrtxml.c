/*
*******************************************************************************
*
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File wrtxml.c
*
* Modification History:
*
*   Date        Name        Description
*   10/01/02    Ram        Creation.
*******************************************************************************
*/
#include "reslist.h"
#include "unewdata.h"
#include "unicode/ures.h"
#include "errmsg.h"
#include "filestrm.h"
#include "cstring.h"
#include "unicode/ucnv.h"
#include "genrb.h"
#include "rle.h"
#include "ucol_tok.h"
#include "uhash.h"
#include "uresimp.h"
#include "unicode/ustring.h"
#include "unicode/uchar.h"
#include <time.h>

static int tabCount = 0;

static FileStream* out=NULL;
static struct SRBRoot* srBundle ;
static const char* outDir = NULL;
static const char* enc ="";
static UConverter* conv = NULL;

const char* const* ISOLanguages;
const char* const* ISOCountries;
const char* textExt = ".txt";
const char* xliffExt = ".xlf";

/*write indentation for formatting*/
static void write_tabs(FileStream* os){
    int i=0;
    for(;i<=tabCount;i++){
        T_FileStream_write(os,"    ",4);
    }
}

/*get ID for each element. ID is globally unique.*/
static char* getID(const char* id, char* curKey, char* result) {
    if(curKey == NULL) { 
        result = uprv_malloc(sizeof(char)*uprv_strlen(id) + 1);
        uprv_memset(result, 0, sizeof(char)*uprv_strlen(id) + 1);
        uprv_strcpy(result, id);
    } else {
        result = uprv_malloc(sizeof(char)*(uprv_strlen(id) + 1 + uprv_strlen(curKey)) + 1);
        uprv_memset(result, 0, sizeof(char)*(uprv_strlen(id) + 1 + uprv_strlen(curKey)) + 1);
        if(id[0]!='\0'){
            uprv_strcpy(result, id);
            uprv_strcat(result, "_");
        }
        uprv_strcat(result, curKey);
    }
    return result;
}

/*compute CRC for binary code*/
/* The code is from  http://www.theorem.com/java/CRC32.java
 * Calculates the CRC32 - 32 bit Cyclical Redundancy Check
 * <P> This check is used in numerous systems to verify the integrity
 * of information.  It's also used as a hashing function.  Unlike a regular
 * checksum, it's sensitive to the order of the characters.
 * It produces a 32 bit
 * 
 * @author Michael Lecuyer (mjl@theorem.com)
 * @version 1.1 August 11, 1998
 */
 
/* ICU is not endian portable, because ICU data generated on big endian machines can be
 * ported to big endian machines but not to little endian machines and vice versa. The 
 * conversion is not portable across platforms with different endianess. 
 */
 
static uint32_t computeCRC(char *ptr, uint32_t len, uint32_t lastcrc){
    int32_t crc;
    uint32_t temp1;
    uint32_t temp2;

    int32_t crc_ta[256];
    int i = 0;
    int j = 0;
    uint32_t crc2 = 0;

#define CRC32_POLYNOMIAL 0xEDB88320

    /*build crc table*/
    for (i = 0; i <= 255; i++) {
        crc2 = i;
        for (j = 8; j > 0; j--) {
            if ((crc2 & 1) == 1) {
                crc2 = (crc2 >> 1) ^ CRC32_POLYNOMIAL;
            } else {
                crc2 >>= 1;
            }
        }
        crc_ta[i] = crc2;
    }

    crc = lastcrc;
    while(len--!=0) {
        temp1 = (uint32_t)crc>>8; 
        temp2 = crc_ta[(crc^*ptr) & 0xFF]; 
        crc = temp1^temp2;
        ptr++;
    }
    return(crc);
}

/*check the language with ISO 639 standard*/
static UBool checkISOLanguage(char* language) {
    int i = 0;
    int result = -1;

    while(ISOLanguages[i] != '\0') {
        result = uprv_strcmp(language, ISOLanguages[i]);
        if(result == 0) {
            return TRUE;
        }
        i++;
    }
    return FALSE;
}

/*check the language with ISO 639 standard*/
static UBool checkISOCountry(char* country) {
    int i = 0;
    int result = -1;

    while(ISOCountries[i]!='\0') {
        result = uprv_strcmp(country, ISOCountries[i]);
        if(result == 0) {
            return TRUE;
        }
        i++;
    }
    return FALSE;
}

/* Parse the filename, and get its language information. 
 * If it fails to get the language information from the filename, 
 * use "en" as the default value for language
 */
static char* parseFilename(const char* fileName, char* lang) {
    char* pos;
    char* pos2;
    int first;
    int second;
    char* str0 = NULL;
    char* str1 = NULL;
    char* str2 = NULL;
    char* str3 = NULL;
    int index = 0;
    UBool ISO_tag = TRUE;    

    ISOLanguages = uloc_getISOLanguages();
    ISOCountries = uloc_getISOCountries();

    pos = uprv_strrchr(fileName, U_FILE_SEP_CHAR);
    pos2 = uprv_strrchr(fileName, '.');
    
    if(pos == NULL) {
        first = -1;
    }else {
        first = pos - fileName;
    }
    if(pos2 == NULL) {
        second = uprv_strlen(fileName);
    } else {
        second = pos2 - fileName;    
    }
    index = second - first - 1;
    str0 = uprv_malloc(sizeof(char) * index + 1);
    uprv_memset(str0, 0, sizeof(char) * index + 1);
    uprv_strncpy(str0, fileName + first + 1, index);
    
    pos = uprv_strchr( str0, '_' );
    first = pos - str0;
    if (pos == NULL) {
        str1 = uprv_malloc(sizeof(char)*uprv_strlen(str0)+1);
        uprv_memset(str1, 0, sizeof(char)*uprv_strlen(str0)+1);
        uprv_strcpy(str1, str0);
    } else {
        str1 = uprv_malloc(sizeof(char)*first+1);
        uprv_memset(str1, 0, sizeof(char)*first+1);
        uprv_strncpy(str1, str0, first);
        pos = uprv_strrchr( str0, '_' );
        second = pos - str0;
        if(first != second && second-first != 1) {
            index = second - first-1;
            str2 = uprv_malloc(sizeof(char)*index+1);
            uprv_memset(str2, 0, sizeof(char)*index+1);
            uprv_strncpy(str2, str0 + first + 1, index );
            index = uprv_strlen(str0) - second -1;
            str3 = uprv_malloc(sizeof(char)*index+1);
            uprv_memset(str3, 0, sizeof(char)*index+1);
            uprv_strncpy(str3, str0 + second + 1, index);
        } else if(first == second) {
            index = first;
            str1 = uprv_malloc(sizeof(char)*first+1);
            uprv_memset(str1, 0, sizeof(char)*first+1);
            uprv_strncpy(str1, str0, index );
            index = uprv_strlen(str0) - second -1;
            str2 = uprv_malloc(sizeof(char)*index+1);
            uprv_memset(str2, 0, sizeof(char)*index+1);
            uprv_strncpy(str2, str0 + second + 1, index );
        }
    }

    if (str2 == NULL && str3 == NULL) {
        ISO_tag = checkISOLanguage(str1);
        if(ISO_tag) {
            lang = uprv_malloc(sizeof(char)*uprv_strlen(str1)+1);
            uprv_memset(lang, 0, sizeof(char)*uprv_strlen(str1)+1);
            uprv_strcpy(lang, str1);
        }
    } else if(str3 == NULL){
        ISO_tag = checkISOLanguage(str1);
        if (ISO_tag) {
            ISO_tag = checkISOCountry(str2);
            if (ISO_tag) {
                lang = uprv_malloc(sizeof(char)*uprv_strlen(str1)+1);
                uprv_memset(lang, 0, sizeof(char)*uprv_strlen(str1)+1);
                uprv_strcpy(lang, str1);
            }
        } else {
            ISO_tag = checkISOLanguage(str2);
            if (ISO_tag) {
                lang = uprv_malloc(sizeof(char)*uprv_strlen(str2)+1);
                uprv_memset(lang, 0, sizeof(char)*uprv_strlen(str2)+1);
                uprv_strcpy(lang, str2);
            }
        }
    } else {
        ISO_tag = checkISOLanguage(str1);
        if(ISO_tag) {
            ISO_tag = checkISOCountry(str2);
            if (ISO_tag) {
                lang = uprv_malloc(sizeof(char)*uprv_strlen(str1)+1);
                uprv_memset(lang, 0, sizeof(char)*uprv_strlen(str1)+1);
                uprv_strcpy(lang, str1);
            }
        } else {
            ISO_tag = checkISOLanguage(str2);
            if(ISO_tag) {
                ISO_tag = checkISOCountry(str3);
                if (ISO_tag) {
                    lang = uprv_malloc(sizeof(char)*uprv_strlen(str2)+1);
                    uprv_memset(lang, 0, sizeof(char)*uprv_strlen(str2)+1);
                    uprv_strcpy(lang, str2);
                }
            }
        }
    }

    if(str0 != NULL){
        uprv_free(str0);
    }

    if(str1 != NULL){
        uprv_free(str1);
    }
    if(str2 != NULL){
        uprv_free(str2);
    }
    if(str3 != NULL){
        uprv_free(str3);
    }
    return lang;
}

static const char* xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                               "<!DOCTYPE xliff " 
                               "SYSTEM \"http://www.oasis-open.org/committees/xliff/documents/xliff.dtd\">\n";
static const char* bundleStart = "<xliff version = \"1.0\">\n";
static const char* bundleEnd   = "</xliff>\n";

void res_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status);

static char* convertAndEscape(char** pDest, int32_t destCap, int32_t* destLength, 
                              const UChar* src, int32_t srcLen, UErrorCode* status){
    int32_t srcIndex=0;
    char* dest=NULL;
    char* temp=NULL;
    int32_t destLen=0;
    UChar32 c = 0;

    if(status==NULL || U_FAILURE(*status) || pDest==NULL  || srcLen==0 || src == NULL){
        return NULL;
    }
    dest =*pDest;
    if(dest==NULL || destCap <=0){
        destCap = srcLen * 8;
        dest = (char*) uprv_malloc(sizeof(char) * destCap);
        if(dest==NULL){
            *status=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
    }

    dest[0]=0;

    while(srcIndex<srcLen){
        U16_NEXT(src, srcIndex, srcLen, c);

        if (U16_IS_LEAD(c) || U16_IS_TRAIL(c)) {
            *status = U_ILLEGAL_CHAR_FOUND;
            fprintf(stderr, "Illegal Surrogate! \n");
            uprv_free(dest);
            return NULL;
        } 

        if((destLen+UTF8_CHAR_LENGTH(c)) < destCap){

            /* ASCII Range */
            if(c <=0x007F){
                switch(c) {
                case '&':
                    uprv_strcpy(dest+( destLen),"&amp;");
                    destLen+=uprv_strlen("&amp;");
                    break;
                case '<':
                    uprv_strcpy(dest+(destLen),"&lt;");
                    destLen+=uprv_strlen("&lt;");
                    break;
                case '>':
                    uprv_strcpy(dest+(destLen),"&gt;");
                    destLen+=uprv_strlen("&gt;");
                    break;
                case '"':
                    uprv_strcpy(dest+(destLen),"&quot;");
                    destLen+=uprv_strlen("&quot;");
                    break;
                case '\'':
                    uprv_strcpy(dest+(destLen),"&apos;");
                    destLen+=uprv_strlen("&apos;");
                    break;

                 /* Disallow C0 controls except TAB, CR, LF*/
                case 0x00:
                case 0x01:
                case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                /*case 0x09:*/
                /*case 0x0A: */
                case 0x0B:
                case 0x0C:
                /*case 0x0D:*/
                case 0x0E:
                case 0x0F:
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                case 0x14:
                case 0x15:
                case 0x16:
                case 0x17:
                case 0x18:
                case 0x19:
                case 0x1A:
                case 0x1B:
                case 0x1C:
                case 0x1D:
                case 0x1E:
                case 0x1F:
                    *status = U_ILLEGAL_CHAR_FOUND;
                    fprintf(stderr, "Illegal Character \\u%04X! \n",c);
                    uprv_free(dest);
                    return NULL;
                default:
                    dest[destLen++]=(char)c;
                }
            }else{
                UBool isError = FALSE;
                U8_APPEND((unsigned char*)dest,destLen,destCap,c,isError);
                if(isError){
                    *status = U_ILLEGAL_CHAR_FOUND;
                    fprintf(stderr, "Illegal Character \\U%08X! \n",c);
                    uprv_free(dest);
                    return NULL;
                }            
            }
        }else{
            destCap += destLen;
            
            temp = (char*) uprv_malloc(sizeof(char)*destCap);
            if(temp==NULL){
                *status=U_MEMORY_ALLOCATION_ERROR;
                return NULL;
            }
            uprv_memmove(temp,dest,destLen);
            destLen=0;
            uprv_free(dest);
            dest=temp;
            temp=NULL;
        }

    }
    *destLength = destLen;
    return dest;
}

    
/* Writing Functions */
static void 
string_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status) {

    char* buf = NULL;
    int32_t bufLen = 0;
    
    char* sid = NULL;
    const char* strStart = "<trans-unit xml:space = \"preserve\" id = \"";
    const char* valStrStart = "<source xml:lang = \"";
    const char* valStrEnd = "</source>\n";
    const char* strEnd = "</trans-unit>\n";
    
    if(status==NULL || U_FAILURE(*status)){
        return;
    }

    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        write_tabs(out);
        T_FileStream_write(out,strStart, uprv_strlen(strStart));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valStrStart, uprv_strlen(valStrStart));
        T_FileStream_write(out,language, uprv_strlen(language));
        T_FileStream_write(out,"\">", 2);
        
        buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,status);

        if(U_FAILURE(*status)){
            return;
        }
        T_FileStream_write(out,buf,bufLen);
        T_FileStream_write(out,valStrEnd,uprv_strlen(valStrEnd));
        
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,strEnd,uprv_strlen(strEnd));
    }else{
        const char* keyStrStart = "resname = \"";
        
        write_tabs(out);
        
        T_FileStream_write(out, strStart, uprv_strlen(strStart));
        sid = getID(id, srBundle->fKeys+res->fKey,sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
        T_FileStream_write(out,keyStrStart, uprv_strlen(keyStrStart));
        
        T_FileStream_write(out,srBundle->fKeys+res->fKey, uprv_strlen(srBundle->fKeys+res->fKey));
        
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valStrStart,uprv_strlen(valStrStart));

        T_FileStream_write(out,language, uprv_strlen(language));
        T_FileStream_write(out,"\">", 2);

        buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,status);
        if(U_FAILURE(*status)){
            return;
        }
        T_FileStream_write(out,buf,bufLen);

        T_FileStream_write(out,valStrEnd,uprv_strlen(valStrEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,strEnd,uprv_strlen(strEnd));
    }
    uprv_free(sid);
    sid = NULL;    
    
    uprv_free(buf);
    buf = NULL;
}

static void 
alias_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    static const char* startKey    = "resname=\"";
    static const char* val           = "<source>";
    static const char* endKey      = "</source>\n";
    static const char* start       = "<trans-unit restype = \"alias\" xml:space = \"preserve\" id = \"";
    static const char* end           = "</trans-unit>\n";
    char* sid = NULL;
        
    char* buf = NULL;
    int32_t bufLen=0;
    write_tabs(out);
    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);        
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
    }else{
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\" ", 2);
        T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);
        
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
    }
    
    buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,/*FALSE,*/status);
    if(U_FAILURE(*status)){
        return;
    }
    T_FileStream_write(out,buf,bufLen);
    T_FileStream_write(out, endKey, uprv_strlen(endKey));
    
    tabCount--;
    write_tabs(out);
    
    T_FileStream_write(out, end, uprv_strlen(end));
    uprv_free(buf);
    uprv_free(sid);
}

static void 
array_write_xml( struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    const char* start = "<group restype = \"array\" xml:space = \"preserve\" id = \"";
    const char* end   = "</group>\n";
    const char* startKey= "resname=\"";
    const char* endKey  = "\">\n";
    char* sid = NULL;
    int index = 0;
    
    struct SResource *current = NULL;
    struct SResource *first =NULL;
    
    write_tabs(out);
    if(res->fKey==0xFFFF ||uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\">\n", 3);
    }else{
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\" ", 2);
        T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        T_FileStream_write(out, endKey, uprv_strlen(endKey));
    }
    current = res->u.fArray.fFirst;
    first=current;
    tabCount++;
    while (current != NULL) {
        char c[256] = {0};
        char* subId = NULL;
        itostr(c, index,10,0);
        index++;
        subId = getID(sid, c, subId);
        
        res_write_xml(current, subId, language, status);
        uprv_free(subId);
        subId = NULL;
        if(U_FAILURE(*status)){
            return;
        }
        current = current->fNext;
    }
    tabCount--;
    write_tabs(out);
    T_FileStream_write(out,end,uprv_strlen(end));
    uprv_free(sid);
    sid = NULL;
}

static void 
intvector_write_xml( struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    const char* start = "<group restype = \"intvector\" xml:space = \"preserve\" id = \"";
    const char* end   = "</group>\n";
    const char* startKey= "resname=\"";
    const char* endKey  = "\">\n";

    const char* intStart = "<trans-unit restype = \"int\" translate = \"no\" xml:space = \"preserve\" id = \"";
    const char* valIntStart = "<source>";
    const char* valIntEnd = "</source>\n";
    const char* intEnd = "</trans-unit>\n";
    char* sid = NULL;
    char* ivd = NULL;
    
    uint32_t i=0;
    uint32_t len=0;
    char buf[256] = {'0'};
    write_tabs(out);
    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
    }else{
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
    
        T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        T_FileStream_write(out, endKey, uprv_strlen(endKey));        
    }
    tabCount++;
    
    for(i = 0; i<res->u.fIntVector.fCount; i++) {
        char c[256] = {0};
        itostr(c, i,10,0);
        ivd = getID(sid, c, ivd);
        len=itostr(buf,res->u.fIntVector.fArray[i],10,0);
        
        write_tabs(out);
        T_FileStream_write(out, intStart, (int32_t)uprv_strlen(intStart));
        T_FileStream_write(out, ivd, uprv_strlen(ivd));
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valIntStart, uprv_strlen(valIntStart));
        
        T_FileStream_write(out,buf,len);
        
        T_FileStream_write(out,valIntEnd, uprv_strlen(valIntEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out, intEnd, (int32_t)uprv_strlen(intEnd));
        
        uprv_free(ivd);
        ivd = NULL;
    }
    
    tabCount--;
    write_tabs(out);
    
    T_FileStream_write(out, end, uprv_strlen(end));
    uprv_free(sid);
    sid = NULL;
}

static void 
int_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    const char* intStart = "<trans-unit restype = \"int\" translate = \"no\" xml:space = \"preserve\" id = \"";
    const char* valIntStart = "<source>";
    const char* valIntEnd = "</source>\n";
    const char* intEnd = "</trans-unit>\n";
    const char* keyIntStart = "resname = \"";
    char* sid = NULL;
    char buf[256] = {0};
    
    uint32_t len=0;
    write_tabs(out);
    
    tabCount++;
    
    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        T_FileStream_write(out, intStart, (int32_t)uprv_strlen(intStart));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
        write_tabs(out);
        T_FileStream_write(out,valIntStart, uprv_strlen(valIntStart));
    }else{
        T_FileStream_write(out, intStart, uprv_strlen(intStart));
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out, sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
        T_FileStream_write(out,keyIntStart, uprv_strlen(keyIntStart));
        
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        T_FileStream_write(out, "\">\n", 3);
        write_tabs(out);
        T_FileStream_write(out, valIntStart, (int32_t)uprv_strlen(valIntStart));
        
    }
    len=itostr(buf,res->u.fIntValue.fValue,10,0);
    T_FileStream_write(out,buf,len);
    
    T_FileStream_write(out, valIntEnd, uprv_strlen(valIntEnd));
    tabCount--;
    write_tabs(out);
    T_FileStream_write(out, intEnd, uprv_strlen(intEnd));
    uprv_free(sid);
    sid = NULL;
}

static void 
bin_write_xml( struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    const char* start = "<bin-unit restype = \"bin\" translate = \"no\" id = \"";
    const char* importStart = "<bin-unit restype = \"import\" translate = \"no\" id = \"";
    const char* mime = " mime-type = ";
    const char* key = "\" resname = \"";
    const char* valStart = "<bin-source>\n";
    const char* fileStart = "<internal-file form = \"application\" crc = \"";
    const char* fileEnd = "</internal-file>\n";
    const char* valEnd = "</bin-source>\n";
    const char* end = "</bin-unit>\n";
    const char* externalFileStart = "<external-file href = \"";
    const char* externalFileEnd = "\"/>\n";
    const char* m_type = "\"application";
    char* sid = NULL;
    uint32_t crc = 0xFFFFFFFF;
     
    char fileName[1024] ={0};
    int32_t tLen = ( outDir == NULL) ? 0 :uprv_strlen(outDir);
    char* fn =  (char*) uprv_malloc(sizeof(char) * (tLen+1024 + 
                                                    (res->u.fBinaryValue.fFileName !=NULL ? 
                                                    uprv_strlen(res->u.fBinaryValue.fFileName) :0)));
    const char* ext = NULL;
    
    char* f = NULL;    
    
    fn[0]=0;

    if(res->u.fBinaryValue.fFileName!=NULL){
        uprv_strcpy(fileName, res->u.fBinaryValue.fFileName);
        f = uprv_strrchr(fileName, '\\');
        f++;
        ext = uprv_strrchr(fileName, '.');

        if(uprv_strcmp(ext, ".jpg")==0 || uprv_strcmp(ext, ".jpeg")==0 || uprv_strcmp(ext, ".gif")==0 ){
            m_type = "\"image";
        } else if(uprv_strcmp(ext, ".wav")==0 || uprv_strcmp(ext, ".au")==0 ){
            m_type = "\"audio";
        } else if(uprv_strcmp(ext, ".avi")==0 || uprv_strcmp(ext, ".mpg")==0 || uprv_strcmp(ext, ".mpeg")==0){
            m_type = "\"video";
        } else if(uprv_strcmp(ext, ".txt")==0 || uprv_strcmp(ext, ".text")==0){
            m_type = "\"text";
        }

        write_tabs(out);
        T_FileStream_write(out, importStart, (int32_t)uprv_strlen(importStart));
        if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
            sid = getID(id, NULL, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        } else {
            sid = getID(id, srBundle->fKeys+res->fKey, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        }
        T_FileStream_write(out, "\" ", 2);
        T_FileStream_write(out, mime, (int32_t)uprv_strlen(mime));
        T_FileStream_write(out, m_type, (int32_t)uprv_strlen(m_type));
        if(!(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0)){
            T_FileStream_write(out, key, (int32_t)uprv_strlen(key));
            T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        } 
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out, valStart, uprv_strlen(valStart));
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out, externalFileStart, uprv_strlen(externalFileStart));
        T_FileStream_write(out, f, (int32_t)uprv_strlen(f));
        T_FileStream_write(out, externalFileEnd, uprv_strlen(externalFileEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out, valEnd, uprv_strlen(valEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,end,uprv_strlen(end));
    } else {
        char temp[256] = {0};
        uint32_t i = 0;
        int32_t len=0;

        write_tabs(out);
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
            sid = getID(id, NULL, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        } else {
            sid = getID(id, srBundle->fKeys+res->fKey, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        }
    
        T_FileStream_write(out, "\" ", 2);
        T_FileStream_write(out, mime, (int32_t)uprv_strlen(mime));
        T_FileStream_write(out, m_type, (int32_t)uprv_strlen(m_type));
        if(!(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0)){
            T_FileStream_write(out, key, (int32_t)uprv_strlen(key));
            T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        }
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out, valStart, uprv_strlen(valStart));
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out, fileStart, uprv_strlen(fileStart));
    
        while(i <res->u.fBinaryValue.fLength){
            len = itostr(temp,res->u.fBinaryValue.fData[i],16,2);
            crc = computeCRC(temp, len, crc);
            i++;
        }
    
        len = itostr(temp, crc, 10, 0);
        T_FileStream_write(out,temp,len);
        T_FileStream_write(out,"\">",2);
    
        i = 0;
        while(i <res->u.fBinaryValue.fLength){
            len = itostr(temp,res->u.fBinaryValue.fData[i],16,2);
            T_FileStream_write(out,temp,len);
            i++;
        }
        T_FileStream_write(out, fileEnd, uprv_strlen(fileEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out, valEnd, uprv_strlen(valEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,end,uprv_strlen(end));
    
        uprv_free(fn);
        uprv_free(sid);
        sid = NULL;
    }
}



static void 
table_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status) {

    uint32_t  i         = 0;

    struct SResource *current = NULL;
    struct SResource *save = NULL;

    char* sid = NULL;
    const char* start = "<group restype = \"table\" xml:space = \"preserve\"";
    const char* idstr   = " id = \"";
    const char* end   = "</group>\n";
    const char* startKey= "resname=\"";
    const char* endKey  = ">\n";

    if (U_FAILURE(*status)) {
        return ;
    }
    
    if (res->u.fTable.fCount > 0) {
        write_tabs(out);
        if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            
            sid = getID(id, NULL, sid);
            /* only write the id if the sid!="" */
            if(sid[0]!='\0'){
                T_FileStream_write(out, idstr, (int32_t)uprv_strlen(idstr));
                T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
                T_FileStream_write(out, "\" ", 2);

            }
            T_FileStream_write(out, endKey, (int32_t)uprv_strlen(endKey));
            
        }else{
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            sid = getID(id, srBundle->fKeys+res->fKey, sid);

            /* only write the id if the sid!="" */
            if(sid[0]!='\0'){
                T_FileStream_write(out, idstr, (int32_t)uprv_strlen(idstr));
                T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
                T_FileStream_write(out, "\" ", 2);
            }
                    
            T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
            T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
            T_FileStream_write(out, "\" ", 2);
            T_FileStream_write(out, endKey, uprv_strlen(endKey));
        }
        tabCount++;
        
        save = current = res->u.fTable.fFirst;
        i       = 0;
        while (current != NULL) {
            res_write_xml(current, sid, language, status);
            
            if(U_FAILURE(*status)){
                return;
            }
            i++;
            current = current->fNext;
        }
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,end,uprv_strlen(end));
    } else {
        write_tabs(out);
        if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            sid = getID(id, NULL, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
            T_FileStream_write(out, endKey, (int32_t)uprv_strlen(endKey));
        }else{
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            sid = getID(id, srBundle->fKeys+res->fKey, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
            T_FileStream_write(out, "\" ", 2);
            T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
            T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
            T_FileStream_write(out, endKey, uprv_strlen(endKey));
        }
        
        write_tabs(out);
        T_FileStream_write(out,end,uprv_strlen(end));
    }
    uprv_free(sid);
    sid = NULL;
}

void 
res_write_xml(struct SResource *res, const char* id, const char* language, UErrorCode *status) {
    
    if (U_FAILURE(*status)) {
        return ;
    }

    if (res != NULL) {
        switch (res->fType) {
        case URES_STRING:
             string_write_xml    (res, id, language, status);
             return;
        case URES_ALIAS:
             alias_write_xml     (res, id, language, status);
             return;
        case URES_INT_VECTOR:
             intvector_write_xml (res, id, language, status);
             return;
        case URES_BINARY:
             bin_write_xml       (res, id, language, status);
             return;
        case URES_INT:
             int_write_xml       (res, id, language, status);
             return;
        case URES_ARRAY:
             array_write_xml     (res, id, language, status);
             return;
        case URES_TABLE:
             table_write_xml     (res, id, language, status);
             return;

        default:
            break;
        }
    }

    *status = U_INTERNAL_PROGRAM_ERROR;
}

void 
bundle_write_xml(struct SRBRoot *bundle, const char *outputDir,const char* outputEnc, const char* filename,
                  char *writtenFilename, int writtenFilenameLen, 
                  const char* language, const char* package, UErrorCode *status) {

    char* xmlfileName = NULL;
    char* outputFileName = NULL;
    char* originalFileName = NULL;
    const char* fileStart = "<file xml:space = \"preserve\" source-language = \"";
    const char* file1 = "\" datatype = \"text\" ";
    const char* file2 = "original = \"";
    const char* file3 = "\" tool = \"genrb\" ";
    const char* file4 = "date = \"";
    const char* file5 = "\" ts = \"";
    const char* fileEnd = "</file>\n";
    const char* headerStart = "<header>";
    const char* headerEnd = "</header>\n";
    const char* bodyStart = "<body>\n";
    const char* bodyEnd = "</body>\n";
    const char* defaultLang = "en";
    char* pid = NULL;
    char* temp = NULL;
    char* lang = NULL;
    char* pos;
    int first, index;
    time_t currTime;
    char timeBuf[128];
    
    outDir = outputDir;

    srBundle = bundle;
    
    pos = uprv_strrchr(filename, '\\');
    if(pos != NULL) {
        first = pos - filename + 1;
    } else {
        first = 0;
    }
    index = uprv_strlen(filename) - uprv_strlen(textExt) - first;
    originalFileName = uprv_malloc(sizeof(char)*index+1);
    uprv_memset(originalFileName, 0, sizeof(char)*index+1);
    uprv_strncpy(originalFileName, filename + first, index);
    
    if(uprv_strcmp(originalFileName, srBundle->fLocale) != 0) {
        fprintf(stdout, "warning! The file name is not same as the resource name!\n");
    }

    temp = originalFileName;
    originalFileName = uprv_malloc(sizeof(char)* (uprv_strlen(temp)+uprv_strlen(textExt)) + 1);
    uprv_memset(originalFileName, 0, sizeof(char)* (uprv_strlen(temp)+uprv_strlen(textExt)) + 1);
    uprv_strcat(originalFileName, temp);
    uprv_strcat(originalFileName, textExt);
    uprv_free(temp);
    temp = NULL;

    /*check file name*/
    if (language == NULL) {
        lang = parseFilename(filename, lang);
        if (lang == NULL) {
            lang = uprv_malloc(sizeof(char)*uprv_strlen(defaultLang) +1);
            uprv_memset(lang, 0, sizeof(char)*uprv_strlen(defaultLang) +1);
            uprv_strcpy(lang, defaultLang);
        }
    } else {
        lang = uprv_malloc(sizeof(char)*uprv_strlen(language) +1);
        uprv_memset(lang, 0, sizeof(char)*uprv_strlen(language) +1);
        uprv_strcpy(lang, language);
    }

    if(package) {
        outputFileName = uprv_malloc(sizeof(char)*uprv_strlen(package) + 1);
        uprv_memset(outputFileName, 0, sizeof(char)*uprv_strlen(package) + 1);
        uprv_strcpy(outputFileName,package);
    } else {
        outputFileName = uprv_malloc(sizeof(char)*uprv_strlen(srBundle->fLocale) + 1);
        uprv_memset(outputFileName, 0, sizeof(char)*uprv_strlen(srBundle->fLocale) + 1);
        uprv_strcpy(outputFileName,srBundle->fLocale);
    }
    
    if(outputDir) {
        xmlfileName = uprv_malloc(sizeof(char)*(uprv_strlen(outputDir) + uprv_strlen(outputFileName) + uprv_strlen(xliffExt) + 1) +1);
        uprv_memset(xmlfileName, 0, sizeof(char)*(uprv_strlen(outputDir)+ uprv_strlen(outputFileName) + uprv_strlen(xliffExt) + 1) +1);
    } else {
        xmlfileName = uprv_malloc(sizeof(char)*(uprv_strlen(outputFileName) + uprv_strlen(xliffExt)) +1);
        uprv_memset(xmlfileName, 0, sizeof(char)*(uprv_strlen(outputFileName) + uprv_strlen(xliffExt)) +1);
    }
    
    if(outputDir){
        uprv_strcpy(xmlfileName, outputDir);
        if(outputDir[uprv_strlen(outputDir)-1] !=U_FILE_SEP_CHAR){
            uprv_strcat(xmlfileName,U_FILE_SEP_STRING);
        }
    }
    uprv_strcat(xmlfileName,outputFileName);
    uprv_strcat(xmlfileName,xliffExt);

    if (writtenFilename) {
        uprv_strncpy(writtenFilename, xmlfileName, writtenFilenameLen);
    }

    if (U_FAILURE(*status)) {
        return;
    }
    
    out= T_FileStream_open(xmlfileName,"w");

    if(out==NULL){
        *status = U_FILE_ACCESS_ERROR;
        return;
    }
    T_FileStream_write(out,xmlHeader, uprv_strlen(xmlHeader));
    
    if(outputEnc && *outputEnc!='\0'){
        /* store the output encoding */
        enc = outputEnc;
        conv=ucnv_open(enc,status);
        if(U_FAILURE(*status)){
            return;
        }
    }
    T_FileStream_write(out,bundleStart,uprv_strlen(bundleStart));
    write_tabs(out);
    T_FileStream_write(out, fileStart, uprv_strlen(fileStart));
    T_FileStream_write(out,lang,uprv_strlen(lang));
    T_FileStream_write(out,file1, uprv_strlen(file1));
    T_FileStream_write(out,file2, uprv_strlen(file2));
    T_FileStream_write(out,originalFileName, uprv_strlen(originalFileName));
    T_FileStream_write(out,file3, uprv_strlen(file3));
    T_FileStream_write(out,file4, uprv_strlen(file4));

    time(&currTime);
    strftime(timeBuf, sizeof(timeBuf), "%Y-%m-%dT%H:%M:%SZ", gmtime(&currTime));
    T_FileStream_write(out,timeBuf, uprv_strlen(timeBuf));

    if(package) {
        T_FileStream_write(out,file5, uprv_strlen(file5));
        T_FileStream_write(out,package, uprv_strlen(package));    
    }
    T_FileStream_write(out,"\">\n", 3);

    tabCount++;
    write_tabs(out);
    T_FileStream_write(out,headerStart, uprv_strlen(headerStart));
    T_FileStream_write(out,headerEnd, uprv_strlen(headerEnd));
    write_tabs(out);
    tabCount++;
    T_FileStream_write(out,bodyStart, uprv_strlen(bodyStart));
    
    if(package) {
        pid = uprv_malloc(sizeof(char) * uprv_strlen(package)+1);
        uprv_memset(pid, 0, sizeof(char) * uprv_strlen(package)+1);
        uprv_strcpy(pid, package);
        res_write_xml(bundle->fRoot, pid, lang, status);
    } else {
        res_write_xml(bundle->fRoot, "", lang, status);
    }

    tabCount--;
    write_tabs(out);
    T_FileStream_write(out,bodyEnd, uprv_strlen(bodyEnd));
    tabCount--;
    write_tabs(out);
    T_FileStream_write(out,fileEnd, uprv_strlen(fileEnd));
    tabCount--;
    write_tabs(out);
    T_FileStream_write(out,bundleEnd,uprv_strlen(bundleEnd));
    T_FileStream_close(out);

    ucnv_close(conv);

    if(originalFileName!= NULL) {
        uprv_free(originalFileName);
        originalFileName = NULL;
    }
    if(lang != NULL) {
        uprv_free(lang);
        lang = NULL;
    }
    if(pid != NULL) {
        uprv_free(pid);
        pid = NULL;
    }
    if(xmlfileName != NULL) {
        uprv_free(xmlfileName);
        pid = NULL;
    }
    if(outputFileName != NULL){
        uprv_free(outputFileName);
        pid = NULL;    
    }    
}
