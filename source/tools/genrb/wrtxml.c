/*
*******************************************************************************
*
*   Copyright (C) 2002, International Business Machines
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
/*

<resourceBundle name="eo">
  <table>
    <int key="a" val="2"/>
    <str key="s" val="Vladimir"/>
    <str key="s2" val="Markus"/>
    <int key="i" val="0x22"/>
    <str key="emptyString" val=""/>
    <str key="anotherEmptyString"/>
    <intVector key="iv" val="20 21 -1 0x7f"/>
    <intVector key="emptyIntegerVector"/>
    <array key="array">
      <int val="20"/>
      <str val="Andy"/>
      <str val="Andy2"/>
      <bin val="fe ff 0a b5"/>
      <intVector val="20 21 -1 0x7f"/>
      <importBin filename="/other.jpeg"/>
      <str/><str val=""/>
      <bin/>
      <intVector/>
      <array/>
      <table/>
    </array>
    <array key="emptyArray"/>
    <bin key="b" val="fe ff 0a b5"/>
    <bin key="emptyBinary"/>
    <importBin key="bb" filename="/something.jpeg"/>
    <table key="t">
      <int key="t0" val="-21"/>
    </table>
    <table key="emptyTable"/>
  </table>
</resourceBundle>

*/


static int tabCount = 0;

static FileStream* out=NULL;
static struct SRBRoot* srBundle ;
static const char* outDir = NULL;
static const char* enc ="";
static UConverter* conv = NULL;

static void write_tabs(FileStream* os){
    int i=0;
    for(;i<=tabCount;i++){
        T_FileStream_write(os,"    ",4);
    }
}
static const char* xmlHeader = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        "<!DOCTYPE resourceBundle " 
                        "SYSTEM \"http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/resourceBundle.dtd\">\n";
                       
static const char* bundleStart = "<resourceBundle name=\"";
static const char* bundleEnd   = "</resourceBundle>\n";


void res_write_xml(struct SResource *res,UErrorCode *status);


static char* convertAndEscape(char** pDest, int32_t destCap, int32_t* destLength, 
							  const UChar* src, int32_t srcLen, UBool isRule, 
							  UErrorCode* status){
	int32_t i=0;
	char* dest=NULL;
	char* temp=NULL;
	int32_t destLen=0;
	
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
string_write_xml(struct SResource *res,UErrorCode *status) {

	char* buf = NULL;
	int32_t bufLen = 0;
	UBool isRule=FALSE;
	if(status==NULL || U_FAILURE(*status)){
		return;
	}

	if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
		const char* valStrStart ="<str val=\"";
		const char* valStrEnd   ="\"/>\n";
		write_tabs(out);
		T_FileStream_write(out,valStrStart, uprv_strlen(valStrStart));
		
		buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,isRule,status);

		if(U_FAILURE(*status)){
			return;
		}
		T_FileStream_write(out,buf,bufLen);
		T_FileStream_write(out,valStrEnd,uprv_strlen(valStrEnd));
	}else{

		const char* keyStrStart ="<str key=\"";
		const char* val			="\" val=\"";
		const char* keyStrEnd   ="\"/>\n";
		write_tabs(out);
		if(uprv_strstr(srBundle->fKeys+res->fKey,"Rule") || uprv_strstr(srBundle->fKeys+res->fKey,"RULES")){
			isRule=TRUE;
		}
		T_FileStream_write(out,keyStrStart, uprv_strlen(keyStrStart));
		T_FileStream_write(out,srBundle->fKeys+res->fKey, uprv_strlen(srBundle->fKeys+res->fKey));
		T_FileStream_write(out,val,uprv_strlen(val));

		buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,isRule,status);
		if(U_FAILURE(*status)){
			return;
		}
		T_FileStream_write(out,buf,bufLen);
		T_FileStream_write(out,keyStrEnd,uprv_strlen(keyStrEnd));
	
	}
	uprv_free(buf);

}

static void 
alias_write_xml(struct SResource *res,UErrorCode *status) {
	static const char* startKey    = "<alias key=\"";
	static const char* val		   = " val=\"";
	static const char* endKey      = "\">";
	static const char* start    = "<alias";
	static const char* end			= "</alias>\n";
	char* buf = NULL;
	int32_t bufLen=0;
    write_tabs(out);
	if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
		T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}else{
		T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
		T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
		T_FileStream_write(out, "\"", 1);
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}
	
	buf = convertAndEscape(&buf,0,&bufLen,res->u.fString.fChars,res->u.fString.fLength,FALSE,status);
	if(U_FAILURE(*status)){
		return;
	}
	T_FileStream_write(out,buf,bufLen);
	T_FileStream_write(out, endKey, uprv_strlen(endKey));
    T_FileStream_write(out, end, uprv_strlen(end));
	uprv_free(buf);

}

static void 
array_write_xml( struct SResource *res, UErrorCode *status) {
	static const char* startKey    = "<array key=\"";
	static const char* endKey      = "\">\n";
	static const char* start    = "<array>\n";
	static const char* end			= "</array>\n";
	struct SResource *current = NULL;
    struct SResource *first =NULL;
	
	write_tabs(out);
	if(res->fKey==0xFFFF ||uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
		T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
	}else{
		T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
		T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
		T_FileStream_write(out, endKey, uprv_strlen(endKey));
	}
	current = res->u.fArray.fFirst;
	first=current;
	tabCount++;
    while (current != NULL) {
        res_write_xml(current, status);
        if(U_FAILURE(*status)){
            return;
        }
        current = current->fNext;
    }
	tabCount--;
	write_tabs(out);
	T_FileStream_write(out,end,uprv_strlen(end));

}

static void 
intvector_write_xml( struct SResource *res, UErrorCode *status) {
	static const char* startKey    = "<intVector key=\"";
	static const char* val		   = " val=\"";
	static const char* endKey      = "\">";
	static const char* start    = "<intVector";
	static const char* end			= "</intVector>\n";
	uint32_t i=0;
	uint32_t len=0;
	char buf[100]={0};
    write_tabs(out);
	if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
		T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}else{
		T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
		T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
		T_FileStream_write(out, "\"", 1);
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}
	/* write the value out */
    for(i = 0; i<res->u.fIntVector.fCount; i++) {
        len=itostr(buf,res->u.fIntVector.fArray[i],10,0);
        T_FileStream_write(out,buf,len);
        T_FileStream_write(out," ",1);
    }
	T_FileStream_write(out, endKey, uprv_strlen(endKey));
    T_FileStream_write(out, end, uprv_strlen(end));

}

static void 
int_write_xml(struct SResource *res,UErrorCode *status) {
	static const char* startKey    = "<int key=\"";
	static const char* val		   = " val=\"";
	static const char* endKey      = "\">";
	static const char* start    = "<int";
	static const char* end			= "</int>\n";
	uint32_t len=0;
	char buf[100]={0};
    write_tabs(out);
	if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
		T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}else{
		T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
		T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
		T_FileStream_write(out, "\"", 1);
		T_FileStream_write(out, val, (int32_t)uprv_strlen(val));
	}
	len=itostr(buf,res->u.fIntValue.fValue,10,0);
    T_FileStream_write(out,buf,len);
    T_FileStream_write(out," ",1);
	T_FileStream_write(out, endKey, uprv_strlen(endKey));
    T_FileStream_write(out, end, uprv_strlen(end));
	
}

static void 
bin_write_xml( struct SResource *res, UErrorCode *status) {
    
    static const char* start    = "<bin key=\"";
    static const char* val      = " val=\"";
    static const char* end      = "\"/>\n";
    static const char* importStart ="<importBin key=\"%s\" filename=\"%s\"/>\n";  
    char fileName[1024] ={0};
    char* fn =  (char*) uprv_malloc(sizeof(char) * (uprv_strlen(outDir)+1024 + 
                                                    (res->u.fBinaryValue.fFileName !=NULL ? 
													uprv_strlen(res->u.fBinaryValue.fFileName) :0)));
    char* buffer = NULL;
    const char* ext = NULL;
    fn[0]=0;

    if(res->u.fBinaryValue.fFileName!=NULL){
        
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
    }
    uprv_free(fn);
}



static void 
table_write_xml(struct SResource *res, UErrorCode *status) {

    uint32_t  i         = 0;

    struct SResource *current = NULL;
    struct SResource *save = NULL;
    const char* start = "<table>\n";
    const char* end   = "</table>\n";
    const char* startKey= "<table key=\"";
    const char* endKey  = "\">\n";

    if (U_FAILURE(*status)) {
        return ;
    }
    
    if (res->u.fTable.fCount > 0) {
        write_tabs(out);
		if(res->fKey==0xFFFF || uprv_strcmp(srBundle->fKeys+res->fKey ,"")==0){
			T_FileStream_write(out, start, (int32_t)uprv_strlen(start));
		}else{
			T_FileStream_write(out, startKey, (int32_t)uprv_strlen(startKey));
			T_FileStream_write(out, srBundle->fKeys+res->fKey, (int32_t) uprv_strlen(srBundle->fKeys+res->fKey));
			T_FileStream_write(out, endKey, uprv_strlen(endKey));
		}
        tabCount++;
        
        save = current = res->u.fTable.fFirst;
        i       = 0;
        while (current != NULL) {
            res_write_xml(current, status);
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
        T_FileStream_write(out,start,uprv_strlen(start));
        write_tabs(out);
        T_FileStream_write(out,end,uprv_strlen(end));

    }
}

void 
res_write_xml(struct SResource *res,UErrorCode *status) {
    
    if (U_FAILURE(*status)) {
        return ;
    }

    if (res != NULL) {
        switch (res->fType) {
        case RES_STRING:
             string_write_xml    (res, status);
             return;
        case RES_ALIAS:
             alias_write_xml     (res, status);
             return;
        case RES_INT_VECTOR:
             intvector_write_xml (res, status);
             return;
        case RES_BINARY:
             bin_write_xml       (res, status);
             return;
        case RES_INT:
             int_write_xml       (res, status);
             return;
        case RES_ARRAY:
             array_write_xml     (res, status);
             return;
        case RES_TABLE:
             table_write_xml     (res, status);
             return;

        default:
            break;
        }
    }

    *status = U_INTERNAL_PROGRAM_ERROR;
}

void 
bundle_write_xml(struct SRBRoot *bundle, const char *outputDir,const char* outputEnc, 
                  char *writtenFilename, int writtenFilenameLen, 
                  UErrorCode *status) {

    char fileName[256] = {'\0'};
    outDir = outputDir;

    srBundle = bundle;

    if(outputDir){
        uprv_strcpy(fileName, outputDir);
        if(outputDir[uprv_strlen(outputDir)-1] !=U_FILE_SEP_CHAR){
            uprv_strcat(fileName,U_FILE_SEP_STRING);
        }
        uprv_strcat(fileName,srBundle->fLocale);
        uprv_strcat(fileName,".xml");
    }else{
        uprv_strcat(fileName,srBundle->fLocale);
        uprv_strcat(fileName,".xml");
    }

    if (writtenFilename) {
        uprv_strncpy(writtenFilename, fileName, writtenFilenameLen);
    }

    if (U_FAILURE(*status)) {
        return;
    }
    
    out= T_FileStream_open(fileName,"w");

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
    T_FileStream_write(out,srBundle->fLocale,uprv_strlen(srBundle->fLocale));
    T_FileStream_write(out,"\">\n",3);
    res_write_xml(bundle->fRoot, status);    
    T_FileStream_write(out,bundleEnd,uprv_strlen(bundleEnd));
    T_FileStream_close(out);

    ucnv_close(conv);

}

