/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*   file name:  unewdata.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct25
*   created by: Markus W. Scherer
*/

#include "utypes.h"
#include "ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "udata.h"
#include "unewdata.h"

struct UNewDataMemory {
    FileStream *file;
    uint16_t headerSize;
    uint8_t magic1, magic2;
};

U_CAPI UNewDataMemory * U_EXPORT2
udata_create(const char *type, const char *name,
             const UDataInfo *pInfo,
             const char *comment,
             UErrorCode *pErrorCode) {
    UNewDataMemory *pData;
    uint16_t headerSize, commentLength;
    const char *path;
    char filename[512];
    uint8_t bytes[16];

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    } else if(name==NULL || *name==0 || pInfo==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    /* allocate the data structure */
    pData=(UNewDataMemory *)icu_malloc(sizeof(UNewDataMemory));
    if(pData==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    /* open the output file */
    path=u_getDataDirectory();
    if(path!=NULL) {
        icu_strcpy(filename, path);
    } else {
        filename[0]=0;
    }
    icu_strcat(filename, name);
    if(type!=NULL && *type!=0) {
        icu_strcat(filename, ".");
        icu_strcat(filename, type);
    }
    pData->file=T_FileStream_open(filename, "wb");
    if(pData->file==NULL) {
        icu_free(pData);
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return NULL;
    }

    /* write the header information */
    headerSize=pInfo->size+4;
    if(comment!=NULL && *comment!=0) {
        commentLength=icu_strlen(comment)+1;
        headerSize+=commentLength;
    } else {
        commentLength=0;
    }

    /* write the size of the header, take padding into account */
    pData->headerSize=(headerSize+15)&~0xf;
    pData->magic1=0xda;
    pData->magic2=0x27;
    T_FileStream_write(pData->file, &pData->headerSize, 4);

    /* write the information data */
    T_FileStream_write(pData->file, pInfo, pInfo->size);

    /* write the comment */
    if(commentLength>0) {
        T_FileStream_write(pData->file, comment, commentLength);
    }

    /* write padding bytes to align the data section to 16 bytes */
    headerSize&=0xf;
    if(headerSize!=0) {
        headerSize=16-headerSize;
        icu_memset(bytes, 0, headerSize);
        T_FileStream_write(pData->file, bytes, headerSize);
    }

    return pData;
}

U_CAPI uint32_t U_EXPORT2
udata_finish(UNewDataMemory *pData, UErrorCode *pErrorCode) {
    uint32_t fileLength=0;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(pData!=NULL) {
        if(pData->file!=NULL) {
            /* fflush(pData->file);*/
            fileLength=T_FileStream_size(pData->file);
            if(T_FileStream_error(pData->file)) {
                *pErrorCode=U_FILE_ACCESS_ERROR;
            } else {
                fileLength-=pData->headerSize;
            }
            T_FileStream_close(pData->file);
        }
        icu_free(pData);
    }

    return fileLength;
}

U_CAPI void U_EXPORT2
udata_write8(UNewDataMemory *pData, uint8_t byte) {
    if(pData!=NULL && pData->file!=NULL) {
        T_FileStream_write(pData->file, &byte, 1);
    }
}

U_CAPI void U_EXPORT2
udata_write16(UNewDataMemory *pData, uint16_t word) {
    if(pData!=NULL && pData->file!=NULL) {
        T_FileStream_write(pData->file, &word, 2);
    }
}

U_CAPI void U_EXPORT2
udata_write32(UNewDataMemory *pData, uint32_t wyde) {
    if(pData!=NULL && pData->file!=NULL) {
        T_FileStream_write(pData->file, &wyde, 4);
    }
}

U_CAPI void U_EXPORT2
udata_writeBlock(UNewDataMemory *pData, const void *s, UTextOffset length) {
    if(pData!=NULL && pData->file!=NULL) {
        if(length>0) {
            T_FileStream_write(pData->file, s, length);
        }
    }
}

U_CAPI void U_EXPORT2
udata_writeString(UNewDataMemory *pData, const char *s, UTextOffset length) {
    if(pData!=NULL && pData->file!=NULL) {
        if(length==-1) {
            length=icu_strlen(s);
        }
        if(length>0) {
            T_FileStream_write(pData->file, s, length);
        }
    }
}

U_CAPI void U_EXPORT2
udata_writeUString(UNewDataMemory *pData, const UChar *s, UTextOffset length) {
    if(pData!=NULL && pData->file!=NULL) {
        if(length==-1) {
            length=u_strlen(s);
        }
        if(length>0) {
            T_FileStream_write(pData->file, s, length*sizeof(UChar));
        }
    }
}
