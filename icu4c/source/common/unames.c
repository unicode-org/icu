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
*   file name:  unames.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct04
*   created by: Markus W. Scherer
*/

/* set import/export definitions */
#ifndef U_COMMON_IMPLEMENTATION
#   define U_COMMON_IMPLEMENTATION
#endif

#include "unicode/utypes.h"
#include "umutex.h"
#include "cmemory.h"
#include "unicode/uchar.h"
#include "unicode/udata.h"

/* prototypes --------------------------------------------------------------- */

#define DATA_NAME "unames"
#define DATA_TYPE "dat"

#define GROUP_SHIFT 5
#define LINES_PER_GROUP (1UL<<GROUP_SHIFT)
#define GROUP_MASK (LINES_PER_GROUP-1)

typedef struct {
    uint16_t groupMSB,
             offsetHigh, offsetLow; /* avoid padding */
} Group;

typedef struct {
    uint32_t start, end;
    uint8_t type, variant;
    uint16_t size;
} AlgorithmicRange;

typedef struct {
    uint32_t tokenStringOffset, groupsOffset, groupStringOffset, algNamesOffset;
} UCharNames;

static UDataMemory *uCharNamesData=NULL;
static UCharNames *uCharNames=NULL;

static bool_t
isAcceptable(void *context,
             const char *type, const char *name,
             UDataInfo *pInfo);

static uint16_t
getName(UCharNames *names, uint32_t code, UCharNameChoice nameChoice,
        char *buffer, uint16_t bufferLength);

static uint16_t
expandGroupName(UCharNames *names, Group *group,
                uint16_t lineNumber, UCharNameChoice nameChoice,
                char *buffer, uint16_t bufferLength);

static uint16_t
expandName(UCharNames *names,
           uint8_t *name, uint16_t nameLength, UCharNameChoice nameChoice,
           char *buffer, uint16_t bufferLength);

static uint16_t
getAlgName(AlgorithmicRange *range, uint32_t code, UCharNameChoice nameChoice,
        char *buffer, uint16_t bufferLength);

/* public API --------------------------------------------------------------- */

U_CAPI UTextOffset U_EXPORT2
u_charName(uint32_t code, UCharNameChoice nameChoice,
           char *buffer, UTextOffset bufferLength,
           UErrorCode *pErrorCode) {
    AlgorithmicRange *algRange;
    uint32_t *p;
    uint32_t i;

    /* check the argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    } else if(nameChoice>=U_CHAR_NAME_CHOICE_COUNT || buffer==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(code>0x10ffff) {
        return 0;
    }

    /* load UCharNames from file if necessary */
    if(uCharNames==NULL) {
        UCharNames *names;
        UDataMemory *data;

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return 0;
        }

        names=(UCharNames *)udata_getMemory(data);

        /* in the mutex block, set the data for this process */
        {
            umtx_lock(NULL);
            if(uCharNames==NULL) {
                uCharNames=names;
                uCharNamesData=data;
                data=NULL;
                names=NULL;
            }
            umtx_unlock(NULL);
        }

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    /* try algorithmic names first */
    p=(uint32_t *)((uint8_t *)uCharNames+uCharNames->algNamesOffset);
    i=*p;
    algRange=(AlgorithmicRange *)(p+1);
    while(i>0) {
        if(algRange->start<=code && code<=algRange->end) {
            return getAlgName(algRange, code, nameChoice, buffer, (uint16_t)bufferLength);
        }
        algRange=(AlgorithmicRange *)((uint8_t *)algRange+algRange->size);
        --i;
    }

    /* normal character name */
    return getName(uCharNames, code, nameChoice, buffer, (uint16_t)bufferLength);
}

/* implementation ----------------------------------------------------------- */

static bool_t
isAcceptable(void *context,
             const char *type, const char *name,
             UDataInfo *pInfo) {
    return
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x75 &&   /* dataFormat="unam" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x61 &&
        pInfo->dataFormat[3]==0x6d &&
        pInfo->formatVersion[0]==1;
}

static uint16_t
getName(UCharNames *names, uint32_t code, UCharNameChoice nameChoice,
        char *buffer, uint16_t bufferLength) {
    uint16_t groupMSB=(uint16_t)(code>>GROUP_SHIFT),
             start=0,
             limit=*(uint16_t *)((char *)names+names->groupsOffset),
             number;
    Group *groups=(Group *)((char *)names+names->groupsOffset+2);

    /* binary search for the group of names that contains the one for code */
    while(start<limit-1) {
        number=(start+limit)/2;
        if(groupMSB<groups[number].groupMSB) {
            limit=number;
        } else {
            start=number;
        }
    }

    if(groupMSB==groups[start].groupMSB) {
        return expandGroupName(names, groups+start, (uint16_t)(code&GROUP_MASK), nameChoice,
                               buffer, bufferLength);
    } else {
        /* group not found */
        /* zero-terminate */
        if(bufferLength>0) {
            *buffer=0;
        }
        return 0;
    }
}

static uint16_t
expandGroupName(UCharNames *names, Group *group,
                uint16_t lineNumber, UCharNameChoice nameChoice,
                char *buffer, uint16_t bufferLength) {
    uint8_t *s=(uint8_t *)names+names->groupStringOffset+
                   (group->offsetHigh<<16|group->offsetLow);

    /* read the length of this string and get the group strings offset */
    uint16_t i=0, offset=0, length=0, nameOffset, nameLength;
    uint8_t lengthByte;

    /* all 32 lengths must be read to get the offset of the first group string */
    while(i<LINES_PER_GROUP) {
        lengthByte=*s++;

        /* read even nibble - MSBs of lengthByte */
        if(length>=12) {
            /* double-nibble length spread across two bytes */
            length=((length&0x3)<<4|lengthByte>>4)+12;
            lengthByte&=0xf;
        } else if((lengthByte&0xf0)>=0xc0) {
            /* double-nibble length spread across this one byte */
            length=(lengthByte&0x3f)+12;
        } else {
            /* single-nibble length in MSBs */
            length=lengthByte>>4;
            lengthByte&=0xf;
        }

        if(i==lineNumber) {
            nameOffset=offset;
            nameLength=length;
        }

        offset+=length;
        ++i;

        /* read odd nibble - LSBs of lengthByte */
        if((lengthByte&0xf0)==0) {
            /* this nibble was not consumed for a double-nibble length above */
            length=lengthByte;
            if(length<12) {
                /* single-nibble length in LSBs */
                if(i==lineNumber) {
                    nameOffset=offset;
                    nameLength=length;
                }

                offset+=length;
                ++i;
            }
        } else {
            length=0;   /* prevent double-nibble detection in the next iteration */
        }
    }

    return expandName(names, s+nameOffset, nameLength, nameChoice,
                      buffer, bufferLength);
}

#define WRITE_CHAR(buffer, bufferLength, bufferPos, c) { \
    if((bufferLength)>0) { \
        *(buffer)++=c; \
        --(bufferLength); \
    } \
    ++(bufferPos); \
}

static uint16_t
expandName(UCharNames *names,
           uint8_t *name, uint16_t nameLength, UCharNameChoice nameChoice,
           char *buffer, uint16_t bufferLength) {
    uint16_t *tokens=(uint16_t *)names+8;
    uint16_t token, tokenCount=*tokens++, bufferPos=0;
    uint8_t *tokenStrings=(uint8_t *)names+names->tokenStringOffset;
    uint8_t c;

    if(nameChoice!=U_UNICODE_CHAR_NAME) {
        /* skip the modern name */
        while(nameLength>0) {
            --nameLength;
            if(*name++==';') {
                break;
            }
        }
    }

    /* write each letter directly, and write a token word per token */
    while(nameLength>0) {
        --nameLength;
        c=*name++;

        if(c==';') {
            /* finished */
            break;
        }

        if(c>=tokenCount) {
            /* implicit letter */
            WRITE_CHAR(buffer, bufferLength, bufferPos, c);
        } else {
            token=tokens[c];
            if(token==(uint16_t)(-2)) {
                /* this is a lead byte for a double-byte token */
                token=tokens[c<<8|*name++];
                --nameLength;
            }
            if(token==(uint16_t)(-1)) {
                /* explicit letter */
                WRITE_CHAR(buffer, bufferLength, bufferPos, c);
            } else {
                /* write token word */
                uint8_t *tokenString=tokenStrings+token;
                while((c=*tokenString++)!=0) {
                    WRITE_CHAR(buffer, bufferLength, bufferPos, c);
                }
            }
        }
    }

    /* zero-terminate */
    if(bufferLength>0) {
        *buffer=0;
    }

    return bufferPos;
}

static uint16_t
getAlgName(AlgorithmicRange *range, uint32_t code, UCharNameChoice nameChoice,
        char *buffer, uint16_t bufferLength) {
    uint16_t bufferPos=0;

    switch(range->type) {
    case 0: {
        /* name = prefix hex-digits */
        char *s=(char *)(range+1);
        char c;

        uint16_t i, count;

        /* copy prefix */
        while((c=*s++)!=0) {
            WRITE_CHAR(buffer, bufferLength, bufferPos, c);
        }

        /* write hexadecimal code point value */
        count=range->variant;

        /* zero-terminate */
        if(count<bufferLength) {
            buffer[count]=0;
        }

        for(i=count; i>0;) {
            if(--i<bufferLength) {
                c=(char)code&0xf;
                if(c<10) {
                    c+='0';
                } else {
                    c+='A'-10;
                }
                buffer[i]=c;
            }
            code>>=4;
        }

        bufferPos+=count;
        break;
    }
    case 1: {
        /* name = prefix factorized-elements */
        uint16_t *factors=(uint16_t *)(range+1);
        char *s=(char *)(factors+range->variant);
        char c;

        uint16_t indeces[8];
        uint16_t i, count, factor;

        /* copy prefix */
        while((c=*s++)!=0) {
            WRITE_CHAR(buffer, bufferLength, bufferPos, c);
        }

        /* write elements according to the factors */
        code-=range->start;

        /*
         * the factorized elements are determined by modulo arithmetic
         * with the factors of this algorithm
         *
         * note that for fewer operations, count is decremented here
         */
        count=range->variant-1;
        for(i=count; i>0; --i) {
            factor=factors[i];
            indeces[i]=(uint16_t)(code%factor);
            code/=factor;
        }
        /*
         * we don't need to calculate the last modulus because start<=code<=end
         * guarantees here that code<=factors[0]
         */
        indeces[0]=(uint16_t)code;

        /* write each element */
        for(;;) {
            /* skip indeces[i] strings */
            factor=indeces[i];
            while(factor>0) {
                while(*s++!=0) {}
                --factor;
            }

            /* write element */
            while((c=*s++)!=0) {
                WRITE_CHAR(buffer, bufferLength, bufferPos, c);
            }

            /* we do not need to perform the rest of this loop for i==count - break here */
            if(i>=count) {
                break;
            }

            /* skip the rest of the strings for this factors[i] */
            factor=factors[i]-indeces[i]-1;
            while(factor>0) {
                while(*s++!=0) {}
                --factor;
            }

            ++i;
        }

        /* zero-terminate */
        if(bufferLength>0) {
            *buffer=0;
        }
        break;
    }
    default:
        /* undefined type */
        /* zero-terminate */
        if(bufferLength>0) {
            *buffer=0;
        }
        break;
    }

    return bufferPos;
}
