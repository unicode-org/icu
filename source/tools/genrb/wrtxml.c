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
/*added by Jing*/
#include <time.h>

static int tabCount = 0;

static FileStream* out=NULL;
static struct SRBRoot* srBundle ;
static const char* outDir = NULL;
static const char* enc ="";
static UConverter* conv = NULL;

/*added by Jing*/
const char* const* ISOLanguages;
const char* const* ISOCountries;

/*write indentation for formatting*/
static void write_tabs(FileStream* os){
    int i=0;
    for(;i<=tabCount;i++){
        T_FileStream_write(os,"    ",4);
    }
}

/*added by Jing*/
/*get ID for each element. ID is globally unique.*/
static char* getID(char* id, char* curKey, char* result) {
    if(curKey == NULL) { 
        result = uprv_malloc(sizeof(char)*uprv_strlen(id) + 1);
        uprv_memset(result, 0, sizeof(char)*uprv_strlen(id) + 1);
        uprv_strcpy(result, id);
    } else {
        result = uprv_malloc(sizeof(char)*(uprv_strlen(id) + 1 + uprv_strlen(curKey)) + 1);
        uprv_memset(result, 0, sizeof(char)*(uprv_strlen(id) + 1 + uprv_strlen(curKey)) + 1);
        uprv_strcpy(result, id);
        uprv_strcat(result, "_");
        uprv_strcat(result, curKey);
    }
    return result;
}

/*added by Jing*/
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
    int32_t crc_ta[256] = {
        0x00000000, 0x77073096, 0xEE0E612C, 0x990951BA,
        0x076DC419, 0x706AF48F, 0xE963A535, 0x9E6495A3,
        0x0EDB8832, 0x79DCB8A4, 0xE0D5E91E, 0x97D2D988,
        0x09B64C2B, 0x7EB17CBD, 0xE7B82D07, 0x90BF1D91,
        0x1DB71064, 0x6AB020F2, 0xF3B97148, 0x84BE41DE,
        0x1ADAD47D, 0x6DDDE4EB, 0xF4D4B551, 0x83D385C7,
        0x136C9856, 0x646BA8C0, 0xFD62F97A, 0x8A65C9EC,
        0x14015C4F, 0x63066CD9, 0xFA0F3D63, 0x8D080DF5,
        0x3B6E20C8, 0x4C69105E, 0xD56041E4, 0xA2677172,
        0x3C03E4D1, 0x4B04D447, 0xD20D85FD, 0xA50AB56B,
        0x35B5A8FA, 0x42B2986C, 0xDBBBC9D6, 0xACBCF940,
        0x32D86CE3, 0x45DF5C75, 0xDCD60DCF, 0xABD13D59,
        0x26D930AC, 0x51DE003A, 0xC8D75180, 0xBFD06116,
        0x21B4F4B5, 0x56B3C423, 0xCFBA9599, 0xB8BDA50F,
        0x2802B89E, 0x5F058808, 0xC60CD9B2, 0xB10BE924,
        0x2F6F7C87, 0x58684C11, 0xC1611DAB, 0xB6662D3D,

        0x76DC4190, 0x01DB7106, 0x98D220BC, 0xEFD5102A,
        0x71B18589, 0x06B6B51F, 0x9FBFE4A5, 0xE8B8D433,
        0x7807C9A2, 0x0F00F934, 0x9609A88E, 0xE10E9818,
        0x7F6A0DBB, 0x086D3D2D, 0x91646C97, 0xE6635C01,
        0x6B6B51F4, 0x1C6C6162, 0x856530D8, 0xF262004E,
        0x6C0695ED, 0x1B01A57B, 0x8208F4C1, 0xF50FC457,
        0x65B0D9C6, 0x12B7E950, 0x8BBEB8EA, 0xFCB9887C,
        0x62DD1DDF, 0x15DA2D49, 0x8CD37CF3, 0xFBD44C65,
        0x4DB26158, 0x3AB551CE, 0xA3BC0074, 0xD4BB30E2,
        0x4ADFA541, 0x3DD895D7, 0xA4D1C46D, 0xD3D6F4FB,
        0x4369E96A, 0x346ED9FC, 0xAD678846, 0xDA60B8D0,
        0x44042D73, 0x33031DE5, 0xAA0A4C5F, 0xDD0D7CC9,
        0x5005713C, 0x270241AA, 0xBE0B1010, 0xC90C2086,
        0x5768B525, 0x206F85B3, 0xB966D409, 0xCE61E49F,
        0x5EDEF90E, 0x29D9C998, 0xB0D09822, 0xC7D7A8B4,
        0x59B33D17, 0x2EB40D81, 0xB7BD5C3B, 0xC0BA6CAD,

        0xEDB88320, 0x9ABFB3B6, 0x03B6E20C, 0x74B1D29A,
        0xEAD54739, 0x9DD277AF, 0x04DB2615, 0x73DC1683,
        0xE3630B12, 0x94643B84, 0x0D6D6A3E, 0x7A6A5AA8,
        0xE40ECF0B, 0x9309FF9D, 0x0A00AE27, 0x7D079EB1,
        0xF00F9344, 0x8708A3D2, 0x1E01F268, 0x6906C2FE,
        0xF762575D, 0x806567CB, 0x196C3671, 0x6E6B06E7,
        0xFED41B76, 0x89D32BE0, 0x10DA7A5A, 0x67DD4ACC,
        0xF9B9DF6F, 0x8EBEEFF9, 0x17B7BE43, 0x60B08ED5,
        0xD6D6A3E8, 0xA1D1937E, 0x38D8C2C4, 0x4FDFF252,
        0xD1BB67F1, 0xA6BC5767, 0x3FB506DD, 0x48B2364B,
        0xD80D2BDA, 0xAF0A1B4C, 0x36034AF6, 0x41047A60,
        0xDF60EFC3, 0xA867DF55, 0x316E8EEF, 0x4669BE79,
        0xCB61B38C, 0xBC66831A, 0x256FD2A0, 0x5268E236,
        0xCC0C7795, 0xBB0B4703, 0x220216B9, 0x5505262F,
        0xC5BA3BBE, 0xB2BD0B28, 0x2BB45A92, 0x5CB36A04,
        0xC2D7FFA7, 0xB5D0CF31, 0x2CD99E8B, 0x5BDEAE1D,

        0x9B64C2B0, 0xEC63F226, 0x756AA39C, 0x026D930A,
        0x9C0906A9, 0xEB0E363F, 0x72076785, 0x05005713,
        0x95BF4A82, 0xE2B87A14, 0x7BB12BAE, 0x0CB61B38,
        0x92D28E9B, 0xE5D5BE0D, 0x7CDCEFB7, 0x0BDBDF21,
        0x86D3D2D4, 0xF1D4E242, 0x68DDB3F8, 0x1FDA836E,
        0x81BE16CD, 0xF6B9265B, 0x6FB077E1, 0x18B74777,
        0x88085AE6, 0xFF0F6A70, 0x66063BCA, 0x11010B5C,
        0x8F659EFF, 0xF862AE69, 0x616BFFD3, 0x166CCF45,
        0xA00AE278, 0xD70DD2EE, 0x4E048354, 0x3903B3C2,
        0xA7672661, 0xD06016F7, 0x4969474D, 0x3E6E77DB,
        0xAED16A4A, 0xD9D65ADC, 0x40DF0B66, 0x37D83BF0,
        0xA9BCAE53, 0xDEBB9EC5, 0x47B2CF7F, 0x30B5FFE9,
        0xBDBDF21C, 0xCABAC28A, 0x53B39330, 0x24B4A3A6,
        0xBAD03605, 0xCDD70693, 0x54DE5729, 0x23D967BF,
        0xB3667A2E, 0xC4614AB8, 0x5D681B02, 0x2A6F2B94,
        0xB40BBE37, 0xC30C8EA1, 0x5A05DF1B, 0x2D02EF8D,
    };

    int32_t CRC32_POLYNOMIAL = 0xEDB88320;
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
    char *pos;
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

    /*cut the path and file extension information, pick out the file name only*/
    pos = uprv_strrchr(fileName, '\\');
    if(pos == NULL) {
        index = uprv_strlen(fileName) - 4;
        str0 = uprv_malloc(sizeof(char) * index + 1);
        uprv_memset(str0, 0, sizeof(char) * index + 1);
        uprv_strncpy(str0, fileName, index);
    } else{
        first = pos - fileName + 1;
        index = uprv_strlen(fileName) - first - 4;
        str0 = uprv_malloc(sizeof(char)*index+1);
        uprv_memset(str0, 0, sizeof(char)*index+1);
        uprv_strncpy(str0, fileName + first, index);
    }

    pos = uprv_strchr( str0, '_' );
    first = pos - str0;
    if (pos == NULL) {
        /*"xx, str0 = xx*/
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
            /*myResources_xx_YY, str1 = myResources, str2 = xx, str3 = YY*/
            index = second - first-1;
            str2 = uprv_malloc(sizeof(char)*index+1);
            uprv_memset(str2, 0, sizeof(char)*index+1);
            uprv_strncpy(str2, str0 + first + 1, index );
            index = uprv_strlen(str0) - second -1;
            str3 = uprv_malloc(sizeof(char)*index+1);
            uprv_memset(str3, 0, sizeof(char)*index+1);
            uprv_strncpy(str3, str0 + second + 1, (uprv_strlen(str0) - second -1));
        } else if(first == second) {
            /*myResource_xx or xx_YY*/
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

/*commented by Jing*/
/*static const char* xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        "<!DOCTYPE resourceBundle " 
                        "SYSTEM \"http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/resourceBundle.dtd\">\n";*/
/*added by Jing*/
static const char* xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                               "<!DOCTYPE xliff " 
							   "SYSTEM \"http://www.oasis-open.org/committees/xliff/documents/xliff.dtd\">\n";
/*commented by Jing*/                       
/*static const char* bundleStart = "<resourceBundle name=\"";
static const char* bundleEnd   = "</resourceBundle>\n";*/

/*added by Jing*/
static const char* bundleStart = "<xliff version = \"1.0\">\n";
static const char* bundleEnd   = "</xliff>\n";

/*commented by Jing*/
/*void res_write_xml(struct SResource *res,UErrorCode *status);*/
/*added by Jing*/
void res_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status);

static char* convertAndEscape(char** pDest, int32_t destCap, int32_t* destLength, 
                              const UChar* src, int32_t srcLen, UBool isRule, 
                              UErrorCode* status){
    int32_t i=0;
    char* dest=NULL;
    char* temp=NULL;
    int32_t destLen=0;
    char buf[4] = {'\0'};
    
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

    while(i<srcLen){
        if((destLen+UTF8_CHAR_LENGTH(src[i])) < destCap){
            /* ASCII Range */
            if((uint16_t)src[i] <=0x007F){
                switch(src[i]){
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
                case '\0':
                    uprv_strcpy(dest+(destLen),"\\u0000;");
                    destLen+=uprv_strlen("&apos;");
                    break;
                 /* escape the C0 controls */
                //case 0x00:
                case 0x01:
				case 0x02:
                case 0x03:
                case 0x04:
                case 0x05:
                case 0x06:
                case 0x07:
                case 0x08:
                /*case 0x09:
                case 0x0A:*/
                case 0x0B:
                case 0x0C:
                case 0x0D:
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
                    uprv_strcpy(dest+destLen,"\\u");
                    destLen+=2;
                    itostr(dest+destLen,src[i],16,4);
                    destLen+=4;
                    break;
                default:
                    dest[destLen++]=(char)src[i];
                }
            }else{
                if(isRule){
                    if(destLen+6 > destCap){
                        goto REALLOC;
                    }
                    uprv_strcpy(dest+destLen,"\\u");
                    destLen+=2;
                    itostr(dest+destLen,src[i],16,4);
                    destLen+=4;
                    
                }else{
                    int32_t len=0;
                    if(UTF_IS_SURROGATE(src[i])){
                        u_strToUTF8(dest+destLen,destCap-destLen,&len,src+i,2,status);
                        i++;
                    }else{
                        u_strToUTF8(dest+destLen,destCap-destLen,&len,src+i,1,status);
                    }
                    destLen+=len;
                    if(U_FAILURE(*status)){
                        break;
                    }
                }
            }
            i++;
        }else{
REALLOC:

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
string_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status) {

    char* buf = NULL;
    int32_t bufLen = 0;
    UBool isRule=FALSE;
    
    /*added by Jing*/
    char* sid = NULL;
    const char* strStart = "<trans-unit xml:space = \"preserve\" id = \"";
    const char* valStrStart = "<source xml:lang = \"";
    const char* valStrEnd = "</source>\n";
    const char* strEnd = "</trans-unit>\n";
    
    if(status==NULL || U_FAILURE(*status)){
        return;
    }

    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        /*commented by Jing*/
        /*const char* valStrStart ="<str val=\"";
        const char* valStrEnd   ="\"/>\n";*/
        write_tabs(out);
        /*T_FileStream_write(out,valStrStart, uprv_strlen(valStrStart));*/
        
        /*added by Jing*/
        T_FileStream_write(out,strStart, uprv_strlen(strStart));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valStrStart, uprv_strlen(valStrStart));
        T_FileStream_write(out,language, uprv_strlen(language));
        T_FileStream_write(out,"\">", 2);
        
        buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,isRule,status);

        if(U_FAILURE(*status)){
            return;
        }
        T_FileStream_write(out,buf,bufLen);
        T_FileStream_write(out,valStrEnd,uprv_strlen(valStrEnd));
        
        /*added by Jing*/
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out,strEnd,uprv_strlen(strEnd));
    }else{
        /*commented by Jing*/
        /*const char* keyStrStart ="<str key=\"";
        const char* val            ="\" val=\"";
        const char* keyStrEnd   ="\"/>\n";*/
        
        /*added by Jing*/
        const char* keyStrStart = "resname = \"";
        
        write_tabs(out);
        if(uprv_strstr(srBundle->fKeys+res->fKey,"Rule") || uprv_strstr(srBundle->fKeys+res->fKey,"RULES")){
            isRule=TRUE;
        }
        /*commented by Jing*/
        /*T_FileStream_write(out,keyStrStart, uprv_strlen(keyStrStart));*/
        
        /*added by Jing*/
        
        T_FileStream_write(out, strStart, uprv_strlen(strStart));
        sid = getID(id, srBundle->fKeys+res->fKey,sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
        T_FileStream_write(out,keyStrStart, uprv_strlen(keyStrStart));
        
        T_FileStream_write(out,srBundle->fKeys+res->fKey, uprv_strlen(srBundle->fKeys+res->fKey));
        
        /*commented by Jing*/
        /*T_FileStream_write(out,val,uprv_strlen(val));*/
        
        /*added by Jing*/
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valStrStart,uprv_strlen(valStrStart));

        T_FileStream_write(out,language, uprv_strlen(language));
        T_FileStream_write(out,"\">", 2);

        /*Special way to handle %%UCARULES*/
        if(uprv_strcmp(srBundle->fKeys+res->fKey, "%%UCARULES") == 0) {
             T_FileStream_write(out,"UCARules.txt",uprv_strlen("UCARules.txt"));
        } else {
            buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,isRule,status);
            if(U_FAILURE(*status)){
                return;
            }
            T_FileStream_write(out,buf,bufLen);
        }
        /*commented by Jing*/
        /*T_FileStream_write(out,keyStrEnd,uprv_strlen(keyStrEnd));*/
        /*added by Jing*/
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
alias_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status) {
    /*commented by Jing*/
    /*static const char* startKey    = "<alias key=\"";
    static const char* val           = " val=\"";
    static const char* endKey      = "\">";
    static const char* start    = "<alias";
    static const char* end            = "</alias>\n";*/
    
    /*added by Jing*/
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
        
        /*added by Jing*/
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);        
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
    }else{
        /*added by Jing*/
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\" ", 2);
        T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        
        /*commented by Jing*/
        /*T_FileStream_write(out, "\"", 1);*/
        
        /*added by Jing*/
        T_FileStream_write(out, "\">\n", 3);
        tabCount++;
        write_tabs(out);
        
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
    }
    
    buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,FALSE,status);
    if(U_FAILURE(*status)){
        return;
    }
    T_FileStream_write(out,buf,bufLen);
    T_FileStream_write(out, endKey, uprv_strlen(endKey));
    
    /*added by Jing*/
    tabCount--;
    write_tabs(out);
    
    T_FileStream_write(out, end, uprv_strlen(end));
    uprv_free(buf);
    uprv_free(sid);
}

static void 
array_write_xml( struct SResource *res, char* id, const char* language, UErrorCode *status) {
    /*commented by Jing*/
    /*static const char* startKey    = "<array key=\"";
    static const char* endKey      = "\">\n";
    static const char* start    = "<array>\n";
    static const char* end            = "</array>\n";*/
    
    /*added by Jing*/
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
        /*added by Jing*/
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
        T_FileStream_write(out, "\">\n", 3);
    }else{
        /*added by Jing*/
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
        /* added by Jing*/        
        /* A 256 char array is large enough to store the integer value "index". 
         * "index" is used to count the number of elements in the array
         */
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
intvector_write_xml( struct SResource *res, char* id, const char* language, UErrorCode *status) {
    /*commented by Jing*/
    /*static const char* startKey    = "<intVector key=\"";
    static const char* val           = " val=\"";
    static const char* endKey      = "\">";
    static const char* start    = "<intVector";
    static const char* end            = "</intVector>\n";*/
    
    /*added by Jing*/
    const char* start = "<group restype = \"array\" xml:space = \"preserve\" id = \"";
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
    /*256 is large enough to store the integer value*/
    char buf[256] = {'0'};
    write_tabs(out);
    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        /*commented by Jing*/
        /*T_FileStream_write(out, val, (int32_t)uprv_strlen(val));*/
        
        /*added by Jing*/
        sid = getID(id, NULL, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
        
    }else{
        /*added by JIng*/
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out,sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
    
        T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        /*commented by Jing*/
        /*T_FileStream_write(out, "\"", 1);
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));*/
        
        /*added by Jing*/
        T_FileStream_write(out, endKey, uprv_strlen(endKey));        
    }
    /* write the value out */
    /*added by Jing*/
    tabCount++;
    
    for(i = 0; i<res->u.fIntVector.fCount; i++) {
        /*added by Jing*/
        int m = 0;
        /* A 256 char array is large enough to store the integer value "i".
         * "i" is used to count the number of integers in an intvector.
         */
        char c[256] = {0};
        itostr(c, i,10,0);
        ivd = getID(sid, c, ivd);
        len=itostr(buf,res->u.fIntVector.fArray[i],10,0);
        
        /*added by Jing*/
        write_tabs(out);
        T_FileStream_write(out, intStart, (int32_t)uprv_strlen(intStart));
        T_FileStream_write(out, ivd, uprv_strlen(ivd));
        T_FileStream_write(out,"\">\n", 3);
        tabCount++;
        write_tabs(out);
        T_FileStream_write(out,valIntStart, uprv_strlen(valIntStart));
        
        T_FileStream_write(out,buf,len);
        /*commented by Jing*/
        /*T_FileStream_write(out," ",1);*/
        
        /*added by Jing*/
        T_FileStream_write(out,valIntEnd, uprv_strlen(valIntEnd));
        tabCount--;
        write_tabs(out);
        T_FileStream_write(out, intEnd, (int32_t)uprv_strlen(intEnd));
        
        uprv_free(ivd);
        ivd = NULL;
    }
    /*commented by Jing*/
    /*T_FileStream_write(out, endKey, uprv_strlen(endKey));*/
    
    /*added by Jing*/
    tabCount--;
    write_tabs(out);
    
    T_FileStream_write(out, end, uprv_strlen(end));
    uprv_free(sid);
    sid = NULL;
}

static void 
int_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status) {
    /*commented by Jing*/
    /*static const char* startKey    = "<int key=\"";
    static const char* val           = " val=\"";
    static const char* endKey      = "\">";
    static const char* start    = "<int";
    static const char* end            = "</int>\n";*/
    
    /*added by Jing*/
    const char* intStart = "<trans-unit restype = \"int\" translate = \"no\" xml:space = \"preserve\" id = \"";
    const char* valIntStart = "<source>";
    const char* valIntEnd = "</source>\n";
    const char* intEnd = "</trans-unit>\n";
    const char* keyIntStart = "resname = \"";
    char* sid = NULL;
    /*A 256 char array is large enough to store the integer value*/
    char buf[256] = {0};
    
    uint32_t len=0;
    write_tabs(out);
    
    /*added by Jing*/
    tabCount++;
    
    if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
        /*commented by Jing*/
        /*T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));*/
        
        /*added by Jing*/
        T_FileStream_write(out, intStart, (int32_t)uprv_strlen(intStart));
        sid = getID(id, NULL, sid);
        T_FileStream_write(out, sid, uprv_strlen(sid));
        T_FileStream_write(out,"\">\n", 3);
        write_tabs(out);
        T_FileStream_write(out,valIntStart, uprv_strlen(valIntStart));
        
    }else{
        
        /*T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));*/
        
        /*added by Jing*/
        T_FileStream_write(out, intStart, uprv_strlen(intStart));
        sid = getID(id, srBundle->fKeys+res->fKey, sid);
        T_FileStream_write(out, sid, uprv_strlen(sid));
        T_FileStream_write(out,"\" ", 2);
        T_FileStream_write(out,keyIntStart, uprv_strlen(keyIntStart));
        
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        /*commented by Jing*/
        /*T_FileStream_write(out, "\"", 1);
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));*/
        
        /*added by Jing*/
        T_FileStream_write(out, "\">\n", 3);
        write_tabs(out);
        T_FileStream_write(out, valIntStart, (int32_t)uprv_strlen(valIntStart));
        
    }
    len=itostr(buf,res->u.fIntValue.fValue,10,0);
    T_FileStream_write(out,buf,len);
    /*commented by Jing*/
    /*T_FileStream_write(out," ",1);
    T_FileStream_write(out, endKey, uprv_strlen(endKey));
    T_FileStream_write(out, end, uprv_strlen(end));*/
    
    /*added by Jing*/
    T_FileStream_write(out, valIntEnd, uprv_strlen(valIntEnd));
    tabCount--;
    write_tabs(out);
    T_FileStream_write(out, intEnd, uprv_strlen(intEnd));
    uprv_free(sid);
    sid = NULL;
}

static void 
bin_write_xml( struct SResource *res, char* id, const char* language, UErrorCode *status) {
    /*commented by Jing*/
    /*static const char* start    = "<bin key=\"";
    static const char* val      = " val=\"";
    static const char* end      = "\"/>\n";
    static const char* importStart ="<importBin key=\"%s\" filename=\"%s\"/>\n";*/ 
    
    /*added by Jing*/
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
    char* buffer = NULL;
    const char* ext = NULL;
    
    /*added by Jing*/
    char* f = NULL;    
    
    fn[0]=0;

    /*if(res->u.fBinaryValue.fFileName!=NULL){
        
        buffer = (char*) uprv_malloc(sizeof(char) * ( uprv_strlen(importStart) + 
                                                      uprv_strlen(srBundle->fKeys+res->fKey) +
                                                      uprv_strlen(res->u.fBinaryValue.fFileName)
                                                    ));
         sprintf(buffer,importStart,srBundle->fKeys+res->fKey,fileName);
         write_tabs(out);
         T_FileStream_write(out, buffer, (int32_t)uprv_strlen(buffer));

    }else{

        char temp[4] ={0};
        uint32_t i = 0;
        int32_t len=0;
        write_tabs(out);
        T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
        T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
        T_FileStream_write(out, "\"", 1);
        T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
        while(i <res->u.fBinaryValue.fLength){
            len = itostr(temp,res->u.fBinaryValue.fData[i],16,2);
            T_FileStream_write(out,temp,len);
            i++;
        }
    
        T_FileStream_write(out,end,uprv_strlen(end));
    }*/
    
    /*added by Jing*/
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
    }else{
        /*An 256 char array is large enough to store the integer value*/
        char temp[256] = {0};
        uint32_t i = 0;
        int32_t len=0;

        if(uprv_strcmp(srBundle->fKeys+res->fKey ,"%%CollationBin") == 0) {
        /*skip*/
        }else {
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
        }
    
    uprv_free(fn);
    uprv_free(sid);
    sid = NULL;
    }
}



static void 
table_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status) {

    uint32_t  i         = 0;

    struct SResource *current = NULL;
    struct SResource *save = NULL;
    /*commented by Jing*/
    /*const char* start = "<table>\n";
    const char* end   = "</table>\n";*/
    
    /*added by Jing*/
    char* sid = NULL;
    const char* start = "<group restype = \"table\" xml:space = \"preserve\" id = \"";
    const char* end   = "</group>\n";
    const char* startKey= "resname=\"";
    const char* endKey  = "\">\n";
    /*commented by Jing*/
    /*const char* startKey= "<table key=\"";
    const char* endKey  = "\">\n";*/

    if (U_FAILURE(*status)) {
        return ;
    }
    
    if (res->u.fTable.fCount > 0) {
        write_tabs(out);
        if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            
            /*added by Jing*/
            sid = getID(id, NULL, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
            T_FileStream_write(out, endKey, (int32_t)uprv_strlen(endKey));
            
        }else{
            /*added by Jing*/
            T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
            sid = getID(id, srBundle->fKeys+res->fKey, sid);
            T_FileStream_write(out, sid, (int32_t)uprv_strlen(sid));
            T_FileStream_write(out, "\" ", 2);
                    
            T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
            T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
            T_FileStream_write(out, endKey, uprv_strlen(endKey));
        }
        tabCount++;
        
        save = current = res->u.fTable.fFirst;
        i       = 0;
        while (current != NULL) {
            /*commented by Jing*/
            /*res_write_xml(current, status);*/
            
            /*added by Jing*/
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
        /*commented by Jing*/
        /*T_FileStream_write(out,start,uprv_strlen(start));*/
        
        /*added by Jing*/
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
res_write_xml(struct SResource *res, char* id, const char* language, UErrorCode *status) {
    
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

    /*added by Jing*/
    char xmlfileName[256] = {'\0'};
    char outputFileName[256] = {'\0'};
    /*An 256 char array is large enough to store the time*/
    char date[256] = {0};
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
    char* defaultLang = "en";
    char* pid = NULL;
    char* temp = NULL;
    char* lang = NULL;
    char* pos;
    int first, index;
  
    outDir = outputDir;

    srBundle = bundle;
    
    /*compare the file name and the resource name*/
    pos = uprv_strrchr(filename, '\\');
    if(pos != NULL) {
        first = pos - filename + 1;
    } else {
        first = 0;
    }
    index = uprv_strlen(filename) -4 - first;
    originalFileName = uprv_malloc(sizeof(char)*index+1);
    uprv_memset(originalFileName, 0, sizeof(char)*index+1);
    uprv_strncpy(originalFileName, filename + first, index);
    
    /*warning if filename is not same as the resource name*/
    if(uprv_strcmp(originalFileName, srBundle->fLocale) != 0) {
        fprintf(stdout, "warning! The file name is not same as the resource name!\n");
    }

    temp = originalFileName;
    originalFileName = uprv_malloc(sizeof(char)* (uprv_strlen(temp)+4) + 1);
    uprv_memset(originalFileName, 0, sizeof(char)* (uprv_strlen(temp)+4) + 1);
    uprv_strcat(originalFileName, temp);
    uprv_strcat(originalFileName, ".txt");
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
        uprv_strcat(outputFileName,package);
    } else {
        uprv_strcat(outputFileName,srBundle->fLocale);
    }
    
    if(outputDir){
        uprv_strcpy(xmlfileName, outputDir);
        if(outputDir[uprv_strlen(outputDir)-1] !=U_FILE_SEP_CHAR){
            uprv_strcat(xmlfileName,U_FILE_SEP_STRING);
        }
    }
    uprv_strcat(xmlfileName,outputFileName);
    uprv_strcat(xmlfileName,".xml");

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
    /*commented by Jing*/
    /*T_FileStream_write(out,srBundle->fLocale,uprv_strlen(srBundle->fLocale));
    T_FileStream_write(out,"\">\n",3);
    res_write_xml(bundle->fRoot, status);    
    T_FileStream_write(out,bundleEnd,uprv_strlen(bundleEnd));
    T_FileStream_close(out);*/
    
    /*added by Jing*/
    write_tabs(out);
    T_FileStream_write(out, fileStart, uprv_strlen(fileStart));
    T_FileStream_write(out,lang,uprv_strlen(lang));
    T_FileStream_write(out,file1, uprv_strlen(file1));
    T_FileStream_write(out,file2, uprv_strlen(file2));
    T_FileStream_write(out,originalFileName, uprv_strlen(originalFileName));
    T_FileStream_write(out,file3, uprv_strlen(file3));
    T_FileStream_write(out,file4, uprv_strlen(file4));
    _strdate( date );
    T_FileStream_write(out,date, uprv_strlen(date));

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
        res_write_xml(bundle->fRoot, srBundle->fLocale, lang, status);
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
    }
    if(lang != NULL) {
        uprv_free(lang);
    }
    if(pid != NULL) {
        uprv_free(pid);
    }
}
