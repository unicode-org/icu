/*
*******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  uparse.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000apr18
*   created by: Markus W. Scherer
*
*   This file provides a parser for files that are delimited by one single
*   character like ';' or TAB. Example: the Unicode Character Properties files
*   like UnicodeData.txt are semicolon-delimited.
*/

#include "unicode/utypes.h"
#include "cstring.h"
#include "filestrm.h"
#include "uparse.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "ustr_imp.h"

#include <stdio.h>

U_CAPI const char * U_EXPORT2
u_skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

U_CAPI void U_EXPORT2
u_parseDelimitedFile(const char *filename, char delimiter,
                     char *fields[][2], int32_t fieldCount,
                     UParseLineFn *lineFn, void *context,
                     UErrorCode *pErrorCode) {
    FileStream *file;
    char line[300];
    char *start, *limit;
    int32_t i, length;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if(fields==NULL || lineFn==NULL || fieldCount<=0) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(filename==NULL || *filename==0 || (*filename=='-' && filename[1]==0)) {
        filename=NULL;
        file=T_FileStream_stdin();
    } else {
        file=T_FileStream_open(filename, "r");
    }
    if(file==NULL) {
        *pErrorCode=U_FILE_ACCESS_ERROR;
        return;
    }

    while(T_FileStream_readLine(file, line, sizeof(line))!=NULL) {
        length=(int32_t)uprv_strlen(line);

        /* remove trailing newline characters */
        while(length>0 && (line[length-1]=='\r' || line[length-1]=='\n')) {
            line[--length]=0;
        }

        /* skip this line if it is empty or a comment */
        if(line[0]==0 || line[0]=='#') {
            continue;
        }

        /* remove in-line comments */
        limit=uprv_strchr(line, '#');
        if(limit!=NULL) {
            /* get white space before the pound sign */
            while(limit>line && (*(limit-1)==' ' || *(limit-1)=='\t')) {
                --limit;
            }

            /* truncate the line */
            *limit=0;
        }

        /* skip lines with only whitespace */
        if(u_skipWhitespace(line)[0]==0) {
            continue;
        }

        /* for each field, call the corresponding field function */
        start=line;
        for(i=0; i<fieldCount; ++i) {
            /* set the limit pointer of this field */
            limit=start;
            while(*limit!=delimiter && *limit!=0) {
                ++limit;
            }

            /* set the field start and limit in the fields array */
            fields[i][0]=start;
            fields[i][1]=limit;

            /* set start to the beginning of the next field, if any */
            start=limit;
            if(*start!=0) {
                ++start;
            } else if(i+1<fieldCount) {
                *pErrorCode=U_PARSE_ERROR;
                limit=line+length;
                i=fieldCount;
                break;
            }
        }

        /* error in a field function? */
        if(U_FAILURE(*pErrorCode)) {
            break;
        }

        /* call the field function */
        lineFn(context, fields, fieldCount, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            break;
        }
    }

    if(filename!=NULL) {
        T_FileStream_close(file);
    }
}

/*
 * parse a list of code points
 * store them as a UTF-32 string in dest[destCapacity]
 * return the number of code points
 */
U_CAPI int32_t U_EXPORT2
u_parseCodePoints(const char *s,
                  uint32_t *dest, int32_t destCapacity,
                  UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;
    int32_t count;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(s==NULL || destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }

    count=0;
    for(;;) {
        s=u_skipWhitespace(s);
        if(*s==';' || *s==0) {
            return count;
        }

        /* read one code point */
        value=(uint32_t)uprv_strtoul(s, &end, 16);
        if(end<=s || (*end!=' ' && *end!='\t' && *end!=';' && *end!=0) || value>=0x110000) {
            *pErrorCode=U_PARSE_ERROR;
            return 0;
        }

        /* append it to the destination array */
        if(count<destCapacity) {
            dest[count++]=value;
        } else {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }

        /* go to the following characters */
        s=end;
    }
}

/*
 * parse a list of code points
 * store them as a string in dest[destCapacity]
 * set the first code point in *pFirst
 * @return The length of the string in numbers of UChars.
 */
U_CAPI int32_t U_EXPORT2
u_parseString(const char *s,
              UChar *dest, int32_t destCapacity,
              uint32_t *pFirst,
              UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;
    int32_t destLength;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(s==NULL || destCapacity<0 || (destCapacity>0 && dest==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }

    if(pFirst!=NULL) {
        *pFirst=0xffffffff;
    }

    destLength=0;
    for(;;) {
        s=u_skipWhitespace(s);
        if(*s==';' || *s==0) {
            if(destLength<destCapacity) {
                dest[destLength]=0;
            } else if(destLength==destCapacity) {
                *pErrorCode=U_STRING_NOT_TERMINATED_WARNING;
            } else {
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            }
            return destLength;
        }

        /* read one code point */
        value=(uint32_t)uprv_strtoul(s, &end, 16);
        if(end<=s || (*end!=' ' && *end!='\t' && *end!=';' && *end!=0) || value>=0x110000) {
            *pErrorCode=U_PARSE_ERROR;
            return 0;
        }

        /* store the first code point */
        if(destLength==0 && pFirst!=NULL) {
            *pFirst=value;
        }

        /* append it to the destination array */
        if((destLength+UTF_CHAR_LENGTH(value))<=destCapacity) {
            UTF_APPEND_CHAR_UNSAFE(dest, destLength, value);
        } else {
            destLength+=UTF_CHAR_LENGTH(value);
        }

        /* go to the following characters */
        s=end;
    }
}

/* read a range like start or start..end */
U_CAPI int32_t U_EXPORT2
u_parseCodePointRange(const char *s,
                      uint32_t *pStart, uint32_t *pEnd,
                      UErrorCode *pErrorCode) {
    char *end;
    uint32_t value;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(s==NULL || pStart==NULL || pEnd==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }

    s=u_skipWhitespace(s);
    if(*s==';' || *s==0) {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }

    /* read the start code point */
    value=(uint32_t)uprv_strtoul(s, &end, 16);
    if(end<=s || (*end!=' ' && *end!='\t' && *end!='.' && *end!=';') || value>=0x110000) {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }
    *pStart=*pEnd=value;

    /* is there a "..end"? */
    s=u_skipWhitespace(end);
    if(*s==';' || *s==0) {
        return 1;
    }

    if(*s!='.' || s[1]!='.') {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }
    s+=2;

    /* read the end code point */
    value=(uint32_t)uprv_strtoul(s, &end, 16);
    if(end<=s || (*end!=' ' && *end!='\t' && *end!=';') || value>=0x110000) {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }
    *pEnd=value;

    /* is this a valid range? */
    if(value<*pStart) {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }

    /* no garbage after that? */
    s=u_skipWhitespace(end);
    if(*s==';' || *s==0) {
        return value-*pStart+1;
    } else {
        *pErrorCode=U_PARSE_ERROR;
        return 0;
    }
}


U_CAPI const UChar * U_EXPORT2
u_strSkipWhiteSpace(const UChar *s, int32_t length) {
    int32_t i = 0, toReturn = 0;
    UChar32 c = 0;
    if(s == NULL) {
        return NULL;
    }
    if(length == 0) {
        return s;
    }
    if(length > 0) {
        for(;;) {
            if(i >= length) {
                break;
            }
            toReturn = i;
            U16_NEXT(s, i, length, c);
            if(!(c == 0x20 || u_isUWhiteSpace(c))) {
                break;
            }
        }
    } else {
        for(;;) {
            toReturn = i;
            U16_NEXT(s, i, length, c);
            if(!(c == 0x20 || u_isUWhiteSpace(c)) || c == 0) {
                break;
            }
        }
    }
    return s+toReturn;
}


U_CAPI const UChar * U_EXPORT2
u_strTrailingWhiteSpaceStart(const UChar *s, int32_t length) {
    int32_t i = 0, toReturn = 0;
    UChar32 c = 0;
    
    if(s == NULL) {
        return NULL;
    }
    if(length == 0) {
        return s;
    }
    
    if(length < 0) {
        length = u_strlen(s);
    }
    
    i = length;
    for(;;) {
        toReturn = i;
        if(i <= 0) {
            break;
        }
        U16_PREV(s, 0, i, c);
        if(!(c == 0x20 || u_isUWhiteSpace(c))) {
            break;
        }
    }
    
    return s+toReturn;
}

U_CAPI int32_t U_EXPORT2
u_parseUTF8(const char *source, int32_t sLen, char *dest, int32_t destCapacity, UErrorCode *status) {
    const char *read = source;
    int32_t i = 0;
    unsigned int value = 0;
    if(sLen == -1) {
        sLen = (int32_t)strlen(source);
    }
    
    while(read < source+sLen) {
        sscanf(read, "%2x", &value);
        if(i < destCapacity) {
            dest[i] = (char)value;
        }
        i++;
        read += 2;
    }
    return u_terminateChars(dest, destCapacity, i, status);
}
