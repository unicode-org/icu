/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File parse.c
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*   02/25/00    weiv        Overhaul to write udata
*   5/10/01     Ram         removed ustdio dependency
*   06/10/2001  Dominic Ludlam <dom@recoil.org> Rewritten
*******************************************************************************
*/

#include "ucol_imp.h"
#include "parse.h"
#include "error.h"
#include "uhash.h"
#include "cmemory.h"
#include "read.h"
#include "ustr.h"
#include "reslist.h"
#include "unicode/ustring.h"
#include "unicode/putil.h"

/* Number of tokens to read ahead of the current stream position */
#define MAX_LOOKAHEAD   2

#define U_ICU_UNIDATA   "unidata"
#define CR              0x000D
#define LF              0x000A
#define SPACE           0x0020
#define ESCAPE          0x005C

U_STRING_DECL(k_type_string,    "string",    6);
U_STRING_DECL(k_type_binary,    "binary",    6);
U_STRING_DECL(k_type_bin,       "bin",       3);
U_STRING_DECL(k_type_table,     "table",     5);
U_STRING_DECL(k_type_int,       "int",       3);
U_STRING_DECL(k_type_integer,   "integer",   7);
U_STRING_DECL(k_type_array,     "array",     5);
U_STRING_DECL(k_type_intvector, "intvector", 9);
U_STRING_DECL(k_type_import,    "import",    6);
U_STRING_DECL(k_type_reserved,  "reserved",  8);

enum EResourceType
{
    RT_UNKNOWN,
    RT_STRING,
    RT_BINARY,
    RT_TABLE,
    RT_INTEGER,
    RT_ARRAY,
    RT_INTVECTOR,
    RT_IMPORT,
    RT_RESERVED
};

/* only used for debugging */
const char *resourceNames[] =
{
    "Unknown",
    "String",
    "Binary",
    "Table",
    "Integer",
    "Array",
    "Int vector",
    "Import",
    "Reserved",
};

struct Lookahead
{
    enum   ETokenType type;
    struct UString    value;
    uint32_t          line;
};

/* keep in sync with token defines in read.h */
const char *tokenNames[] =
{
    "string",           /* A string token, such as "MonthNames" */
    "'{'",              /* An opening brace character */
    "'}'",              /* A closing brace character */
    "','",              /* A comma */
    "':'",              /* A colon */

    "<end of file>",    /* End of the file has been reached successfully */
    "<error>",          /* An error, such an unterminated quoted string */
};

/* Just to store "TRUE" */
static const UChar trueValue[] = {0x0054, 0x0052, 0x0055, 0x0045, 0x0000};

static struct Lookahead  lookahead[MAX_LOOKAHEAD + 1];
static uint32_t          lookaheadPosition;
static UCHARBUF         *buffer;

static struct SRBRoot *bundle;
static const char     *inputdir;
static uint32_t        inputdirLength;

static struct SResource *parseResource(char *tag, UErrorCode *status);

void initParser(void)
{
    uint32_t i;

    U_STRING_INIT(k_type_string,    "string",    6);
    U_STRING_INIT(k_type_binary,    "binary",    6);
    U_STRING_INIT(k_type_bin,       "bin",       3);
    U_STRING_INIT(k_type_table,     "table",     5);
    U_STRING_INIT(k_type_int,       "int",       3);
    U_STRING_INIT(k_type_integer,   "integer",   7);
    U_STRING_INIT(k_type_array,     "array",     5);
    U_STRING_INIT(k_type_intvector, "intvector", 9);
    U_STRING_INIT(k_type_import,    "import",    6);
    U_STRING_INIT(k_type_reserved,  "reserved",  8);

    for (i = 0; i < MAX_LOOKAHEAD + 1; i++)
    {
        ustr_init(&lookahead[i].value);
    }
}

/* The nature of the lookahead buffer:
   There are MAX_LOOKAHEAD + 1 slots, used as a circular buffer.  This provides
   MAX_LOOKAHEAD lookahead tokens and a slot for the current token and value.
   When getToken is called, the current pointer is moved to the next slot and the
   old slot is filled with the next token from the reader by calling getNextToken.
   The token values are stored in the slot, which means that token values don't
   survive a call to getToken, ie.

   UString *value;

   getToken(&value, NULL, status);
   getToken(NULL,   NULL, status);      bad - value is now a different string
*/
static void
initLookahead(UCHARBUF *buf, UErrorCode *status)
{
    static uint32_t initTypeStrings = 0;
    uint32_t i;

    if (!initTypeStrings)
    {
        initTypeStrings = 1;
    }

    lookaheadPosition = 0;
    buffer            = buf;

    resetLineNumber();

    for (i = 0; i < MAX_LOOKAHEAD; i++)
    {
        lookahead[i].type = getNextToken(buffer, &lookahead[i].value, &lookahead[i].line, status);

        if (U_FAILURE(*status))
        {
            return;
        }
    }

    *status = U_ZERO_ERROR;
}

static enum ETokenType
getToken(struct UString **tokenValue, uint32_t *linenumber, UErrorCode *status)
{
    enum ETokenType result;
    uint32_t        i;

    result = lookahead[lookaheadPosition].type;

    if (tokenValue != NULL)
    {
        *tokenValue = &lookahead[lookaheadPosition].value;
    }

    if (linenumber != NULL)
    {
        *linenumber = lookahead[lookaheadPosition].line;
    }

    i = (lookaheadPosition + MAX_LOOKAHEAD) % (MAX_LOOKAHEAD + 1);
    lookaheadPosition = (lookaheadPosition + 1) % (MAX_LOOKAHEAD + 1);
    lookahead[i].type = getNextToken(buffer, &lookahead[i].value, &lookahead[i].line, status);

    /* printf("getToken, returning %s\n", tokenNames[result]); */

    return result;
}

static enum ETokenType
peekToken(uint32_t lookaheadCount, struct UString **tokenValue, uint32_t *linenumber, UErrorCode *status)
{
    uint32_t i = (lookaheadPosition + lookaheadCount) % (MAX_LOOKAHEAD + 1);

    if (U_FAILURE(*status))
    {
        return TOK_ERROR;
    }

    if (lookaheadCount >= MAX_LOOKAHEAD)
    {
        *status = U_INTERNAL_PROGRAM_ERROR;
        return TOK_ERROR;
    }

    if (tokenValue != NULL)
    {
        *tokenValue = &lookahead[i].value;
    }

    if (linenumber != NULL)
    {
        *linenumber = lookahead[i].line;
    }

    return lookahead[i].type;
}

static void
expect(enum ETokenType expectedToken, struct UString **tokenValue, uint32_t *linenumber, UErrorCode *status)
{
    uint32_t        line;
    enum ETokenType token = getToken(tokenValue, &line, status);

    if (U_FAILURE(*status))
    {
        return;
    }

    if (linenumber != NULL)
    {
        *linenumber = line;
    }

    if (token != expectedToken)
    {
        *status = U_INVALID_FORMAT_ERROR;
        error(line, "expecting %s, got %s", tokenNames[expectedToken], tokenNames[token]);
    }

    *status = U_ZERO_ERROR;
}

static char *getInvariantString(uint32_t *line, UErrorCode *status)
{
    struct UString *tokenValue;
    char           *result;
    uint32_t        count;

    expect(TOK_STRING, &tokenValue, line, status);

    if (U_FAILURE(*status))
    {
        return NULL;
    }

    count  = u_strlen(tokenValue->fChars) + 1;
    result = uprv_malloc(count);

    if (result == NULL)
    {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    u_UCharsToChars(tokenValue->fChars, result, count);
    return result;
}

static enum EResourceType
parseResourceType(UErrorCode *status)
{
    struct UString       *tokenValue;
    enum   EResourceType  result = RT_UNKNOWN;
    uint32_t              line;

    expect(TOK_STRING, &tokenValue, &line, status);

    if (U_FAILURE(*status))
    {
        return RT_UNKNOWN;
    }

    *status = U_ZERO_ERROR;

    if (u_strcmp(tokenValue->fChars, k_type_string) == 0) {
        result = RT_STRING;
    } else if (u_strcmp(tokenValue->fChars, k_type_array) == 0) {
        result = RT_ARRAY;
    } else if (u_strcmp(tokenValue->fChars, k_type_table) == 0) {
        result = RT_TABLE;
    } else if (u_strcmp(tokenValue->fChars, k_type_binary) == 0) {
        result = RT_BINARY;
    } else if (u_strcmp(tokenValue->fChars, k_type_bin) == 0) {
        result = RT_BINARY;
    } else if (u_strcmp(tokenValue->fChars, k_type_int) == 0) {
        result = RT_INTEGER;
    } else if (u_strcmp(tokenValue->fChars, k_type_integer) == 0) {
        result = RT_INTEGER;
    } else if (u_strcmp(tokenValue->fChars, k_type_intvector) == 0) {
        result = RT_INTVECTOR;
    } else if (u_strcmp(tokenValue->fChars, k_type_import) == 0) {
        result = RT_IMPORT;
    } else if (u_strcmp(tokenValue->fChars, k_type_reserved) == 0) {
        result = RT_RESERVED;
    } else {
        char tokenBuffer[1024];
        u_austrncpy(tokenBuffer, tokenValue->fChars, sizeof(tokenBuffer));
        tokenBuffer[sizeof(tokenBuffer)] = 0;
        *status = U_INVALID_FORMAT_ERROR;
        error(line, "unknown resource type '%s'", tokenBuffer);
    }

    return result;
}

static struct SResource *
parseUCARules(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource *result = NULL;
    struct UString   *tokenValue;
    FileStream       *file          = NULL;
    char              filename[256] = { '\0' };
    char              cs[128]       = { '\0' };
    uint32_t          line;
    int               len=0;
    expect(TOK_STRING, &tokenValue, &line, status);

    /* make the filename including the directory */
    if (inputdir != NULL)
    {
        uprv_strcat(filename, inputdir);

        if (inputdir[inputdirLength - 1] != U_FILE_SEP_CHAR)
        {
            uprv_strcat(filename, U_FILE_SEP_STRING);
        }
    }

    u_UCharsToChars(tokenValue->fChars, cs, tokenValue->fLength);

    expect(TOK_CLOSE_BRACE, NULL, NULL, status);

    if (U_FAILURE(*status))
    {
        return NULL;
    }

    uprv_strcat(filename, U_ICU_UNIDATA);
    uprv_strcat(filename, U_FILE_SEP_STRING);
    uprv_strcat(filename, cs);

    /* open the file */
    file = T_FileStream_open(filename, "rb");

    if (file != NULL)
    {
        UCHARBUF *ucbuf;
        UChar32   c    = 0;
        uint32_t  size = T_FileStream_size(file);

        /* We allocate more space than actually required
        * since the actual size needed for storing UChars 
        * is not known in UTF-8 byte stream
        */
        UChar *pTarget     = (UChar *) uprv_malloc(sizeof(UChar) * size);
        UChar *target      = pTarget;
        UChar *targetLimit = pTarget + size;

        ucbuf = ucbuf_open(file, status);

        /* read the rules into the buffer */
        while (target < targetLimit)
        {
            c = ucbuf_getc(ucbuf, status);

            if (c == ESCAPE)
            {
                c = unescape(ucbuf, status);

                if (c == U_ERR)
                {
                    uprv_free(pTarget);
                    T_FileStream_close(file);
                    return NULL;
                }
            }
            else if (c == SPACE || c == CR || c == LF)
            {
                /* ignore spaces carriage returns 
                * and line feed unless in the form \uXXXX
                */
                continue;
            }

            /* Append UChar * after dissembling if c > 0xffff*/
            if (c != U_EOF)
            {
                U_APPEND_CHAR32(c, target,len);
            }
            else
            {
                break;
            }
        }

        result = string_open(bundle, tag, pTarget, target - pTarget, status);

        uprv_free(pTarget);
        T_FileStream_close(file);

        return result;
    }
    else
    {
        error(line, "couldn't open input file %s\n", filename);
        *status = U_FILE_ACCESS_ERROR;
        return NULL;
    }
}

static struct SResource *
parseString(char *tag, uint32_t startline, UErrorCode *status)
{
    struct UString   *tokenValue;
    struct SResource *result = NULL;

    if (tag != NULL && uprv_strcmp(tag, "%%UCARULES") == 0)
    {
    return parseUCARules(tag, startline, status);
    }

    expect(TOK_STRING, &tokenValue, NULL, status);

    if (U_SUCCESS(*status))
    {
    /* create the string now - tokenValue doesn't survive a call to getToken (and therefore
       doesn't survive expect either) */

        result = string_open(bundle, tag, tokenValue->fChars, tokenValue->fLength, status);

        expect(TOK_CLOSE_BRACE, NULL, NULL, status);

        if (U_FAILURE(*status))
        {
        string_close(result, status);
            return NULL;
        }
    }

    return result;
}

static struct SResource *
parseCollationElements(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource  *result = NULL;
    struct SResource  *member = NULL;
    struct UString    *tokenValue;
    enum   ETokenType  token;
    char               subtag[1024];
    UVersionInfo       version;
    UBool              override = FALSE;
    uint32_t           line;

    result = table_open(bundle, tag, status);

    if (result == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    /* '{' . (name resource)* '}' */
    for (;;)
    {
        token = getToken(&tokenValue, &line, status);

        if (token == TOK_CLOSE_BRACE)
        {
            return result;
        }

        if (token != TOK_STRING)
        {
            table_close(result, status);
            *status = U_INVALID_FORMAT_ERROR;

            if (token == TOK_EOF)
            {
                error(startline, "unterminated table");
            }
            else
            {
                error(line, "enexpected token %s", tokenNames[token]);
            }

            return NULL;
        }

        u_UCharsToChars(tokenValue->fChars, subtag, u_strlen(tokenValue->fChars) + 1);

        if (U_FAILURE(*status))
        {
            table_close(result, status);
            return NULL;
        }

        expect(TOK_OPEN_BRACE, NULL,        NULL,  status);
        expect(TOK_STRING,     &tokenValue, &line, status);

        if (U_FAILURE(*status))
        {
            table_close(result, status);
            return NULL;
        }

        if (uprv_strcmp(subtag, "Version") == 0)
        {
            char    ver[40];
            int32_t length = u_strlen(tokenValue->fChars);

            if (length >= (int32_t) sizeof(ver))
            {
                length = (int32_t) sizeof(ver) - 1;
            }

            u_UCharsToChars(tokenValue->fChars, ver, length);
            u_versionFromString(version, ver);
        }
        else if (uprv_strcmp(subtag, "Override") == 0)
        {
            override = FALSE;

            if (u_strncmp(tokenValue->fChars, trueValue, u_strlen(trueValue)) == 0)
            {
                override = TRUE;
            }
        }
        else if (uprv_strcmp(subtag, "Sequence") == 0)
        {
            UErrorCode intStatus = U_ZERO_ERROR;

            /* do the collation elements */
            int32_t    len   = 0;
            uint8_t   *data  = NULL;
            UCollator *coll  = NULL;
            UChar     *rules = NULL;
            UParseError parseError;
            coll = ucol_openRules(tokenValue->fChars, tokenValue->fLength, 
                UNORM_NONE, UCOL_DEFAULT_STRENGTH,&parseError, &intStatus);

            if (U_SUCCESS(intStatus) && coll != NULL)
            {
                data = ucol_cloneRuleData(coll, &len, &intStatus);

                /* tailoring rules version */
                coll->dataInfo.dataVersion[1] = version[0];

                if (U_SUCCESS(intStatus) && data != NULL)
                {
                    member = bin_open(bundle, "%%CollationNew", len, data, status);
                    table_add(bundle->fRoot, member, line, status);
                    uprv_free(data);
                }
                else
                {
                    warning(line, "could not obtain rules from collator");
                }

                ucol_close(coll);
            }
            else
            {
                warning(line, "%%Collation could not be constructed from CollationElements - check context!");
            }

            uprv_free(rules);
        }

        member = string_open(bundle, subtag, tokenValue->fChars, tokenValue->fLength, status);
        table_add(result, member, line, status);

        expect(TOK_CLOSE_BRACE, NULL, NULL, status);

        if (U_FAILURE(*status))
        {
            table_close(result, status);
            return NULL;
        }
    }

    /* not reached */
    *status = U_INTERNAL_PROGRAM_ERROR;
    return NULL;
}

/* Necessary, because CollationElements requires the bundle->fRoot member to be present which,
   if this weren't special-cased, wouldn't be set until the entire file had been processed. */
static struct SResource *
realParseTable(struct SResource *table, char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource  *member = NULL;
    struct UString    *tokenValue;
    enum   ETokenType  token;
    char               subtag[1024];
    uint32_t           line;
    UBool              readToken = FALSE;

    /* '{' . (name resource)* '}' */
    for (;;)
    {
        token = getToken(&tokenValue, &line, status);

        if (token == TOK_CLOSE_BRACE)
        {
            if (!readToken) {
                warning(startline, "Encountered empty table");
            }
            return table;
        }

        if (token != TOK_STRING)
        {
            table_close(table, status);
            *status = U_INVALID_FORMAT_ERROR;

            if (token == TOK_EOF)
            {
                error(startline, "unterminated table");
            }
            else
            {
                error(line, "enexpected token %s", tokenNames[token]);
            }

            return NULL;
        }

        u_UCharsToChars(tokenValue->fChars, subtag, u_strlen(tokenValue->fChars) + 1);

        if (U_FAILURE(*status))
        {
            error(line, "parse error. Stopped parsing with %s", u_errorName(*status));
            table_close(table, status);
            return NULL;
        }

        member = parseResource(subtag, status);

        if (member == NULL || U_FAILURE(*status))
        {
            error(line, "parse error. Stopped parsing with %s", u_errorName(*status));
            table_close(table, status);
            return NULL;
        }

        table_add(table, member, line, status);

        if (U_FAILURE(*status))
        {
            error(line, "parse error. Stopped parsing with %s", u_errorName(*status));
            table_close(table, status);
            return NULL;
        }
        readToken = TRUE;
    }

    /* not reached */
    *status = U_INTERNAL_PROGRAM_ERROR;
    return NULL;
}

static struct SResource *
parseTable(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource *result;

    if (tag != NULL && uprv_strcmp(tag, "CollationElements") == 0)
    {
        return parseCollationElements(tag, startline, status);
    }

    result = table_open(bundle, tag, status);

    if (result == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    return realParseTable(result, tag, startline, status);
}

static struct SResource *
parseArray(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource  *result = NULL;
    struct SResource  *member = NULL;
    struct UString    *tokenValue;
    enum   ETokenType  token;
    UBool              readToken = FALSE;

    result = array_open(bundle, tag, status);

    if (result == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    /* '{' . resource [','] '}' */
    for (;;)
    {
        /* check for end of array, but don't consume next token unless it really is the end */
        token = peekToken(0, &tokenValue, NULL, status);

        if (token == TOK_CLOSE_BRACE)
        {
            getToken(NULL, NULL, status);
            if (!readToken) {
                warning(startline, "Encountered empty array");
            }
            break;
        }

        if (token == TOK_EOF)
        {
            array_close(result, status);
            *status = U_INVALID_FORMAT_ERROR;
            error(startline, "unterminated array");
            return NULL;
        }

        /* string arrays are a special case */
        if (token == TOK_STRING)
        {
            getToken(&tokenValue, NULL, status);
            member = string_open(bundle, NULL, tokenValue->fChars, tokenValue->fLength, status);
        }
        else
        {
            member = parseResource(NULL, status);
        }

        if (member == NULL || U_FAILURE(*status))
        {
            array_close(result, status);
            return NULL;
        }

        array_add(result, member, status);

        if (U_FAILURE(*status))
        {
            array_close(result, status);
            return NULL;
        }

        /* eat optional comma if present */
        token = peekToken(0, NULL, NULL, status);

        if (token == TOK_COMMA)
        {
            getToken(NULL, NULL, status);
        }

        if (U_FAILURE(*status))
        {
            array_close(result, status);
            return NULL;
        }
        readToken = TRUE;
    }

    return result;
}

static struct SResource *
parseIntVector(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource  *result = NULL;
    enum   ETokenType  token;
    char              *string;
    int32_t            value;
    UBool              readToken = FALSE;

    result = intvector_open(bundle, tag, status);

    if (result == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    /* '{' . string [','] '}' */
    for (;;)
    {
        /* check for end of array, but don't consume next token unless it really is the end */
        token = peekToken(0, NULL, NULL, status);

        if (token == TOK_CLOSE_BRACE)
        {
            /* it's the end, consume the close brace */
            getToken(NULL, NULL, status);
            if (!readToken) {
                warning(startline, "Encountered empty int vector");
            }
            return result;
        }

        string = getInvariantString(NULL, status);

        if (U_FAILURE(*status))
        {
            intvector_close(result, status);
            return NULL;
        }

        value = uprv_strtol(string, NULL, 10);
        intvector_add(result, value, status);

        uprv_free(string);

        token = peekToken(0, NULL, NULL, status);

        if (U_FAILURE(*status))
        {
            intvector_close(result, status);
            return NULL;
        }

        /* the comma is optional (even though it is required to prevent the reader from concatenating
           consecutive entries) so that a missing comma on the last entry isn't an error */
        if (token == TOK_COMMA)
        {
            getToken(NULL, NULL, status);
        }
        readToken = TRUE;
    }

    /* not reached */
    intvector_close(result, status);
    *status = U_INTERNAL_PROGRAM_ERROR;
    return NULL;
}

static struct SResource *
parseBinary(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource *result = NULL;
    uint8_t          *value;
    char             *string;
    char              toConv[3] = {'\0', '\0', '\0'};
    uint32_t          count;
    uint32_t          i;
    uint32_t          line;
    UBool             readToken = FALSE;

    string = getInvariantString(&line, status);

    if (string == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    expect(TOK_CLOSE_BRACE, NULL, NULL, status);

    if (U_FAILURE(*status))
    {
        uprv_free(string);
        return NULL;
    }

    count = uprv_strlen(string);
    if (count > 0)
    {
        value = uprv_malloc(sizeof(uint8_t) * count);

        if (value == NULL)
        {
            uprv_free(string);
            *status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }

        for (i = 0; i < count; i += 2)
        {
            toConv[0] = string[i];
            toConv[1] = string[i + 1];

            value[i >> 1] = (uint8_t) uprv_strtoul(toConv, NULL, 16);
        }

        result = bin_open(bundle, tag, (i >> 1), value, status);

        uprv_free(value);
    }
    else {
        result = bin_open(bundle, tag, 0, NULL, status);
        warning(startline, "Encountered empty binary tag");
    }

    uprv_free(string);

    return result;
}

static struct SResource *
parseInteger(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource *result = NULL;
    int32_t           value;
    char             *string;

    string = getInvariantString(NULL, status);

    if (string == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    expect(TOK_CLOSE_BRACE, NULL, NULL, status);

    if (U_FAILURE(*status))
    {
        uprv_free(string);
        return NULL;
    }

    if (uprv_strlen(string) <= 0) {
        warning(startline, "Encountered empty integer. Default value is 0.");
    }

    value  = uprv_strtol(string, NULL, 10);
    result = int_open(bundle, tag, value, status);

    uprv_free(string);

    return result;
}

static struct SResource *
parseImport(char *tag, uint32_t startline, UErrorCode *status)
{
    struct SResource *result;
    FileStream       *file;
    int32_t           len;
    uint8_t          *data;
    char             *filename;
    uint32_t          line;

    filename = getInvariantString(&line, status);

    if (U_FAILURE(*status))
    {
        return NULL;
    }

    expect(TOK_CLOSE_BRACE, NULL, NULL, status);

    if (U_FAILURE(*status))
    {
        uprv_free(filename);
        return NULL;
    }

    /* Open the input file for reading */
    if (inputdir == NULL)
    {
        file = T_FileStream_open(filename, "rb");
    }
    else
    {
        char    *fullname = NULL;
        int32_t  count    = uprv_strlen(filename);

        if (inputdir[inputdirLength - 1] != U_FILE_SEP_CHAR)
        {
            fullname = (char *) uprv_malloc(inputdirLength + count + 2);

            uprv_strcpy(fullname, inputdir);

            fullname[inputdirLength]     = U_FILE_SEP_CHAR;
            fullname[inputdirLength + 1] = '\0';

            uprv_strcat(fullname, filename);
        }
        else
        {
            fullname = (char *) uprv_malloc(inputdirLength + count + 1);

            uprv_strcpy(fullname, inputdir);
            uprv_strcat(fullname, filename);
        }

        file = T_FileStream_open(fullname, "rb");
        uprv_free(fullname);
    }

    if (file == NULL)
    {
        error(line, "couldn't open input file %s", filename);
        *status = U_FILE_ACCESS_ERROR;
        return NULL;
    }

    len  = T_FileStream_size(file);
    data = uprv_malloc(len);

    T_FileStream_read  (file, data, len);
    T_FileStream_close (file);

    result = bin_open(bundle, tag, len, data, status);

    uprv_free(data);
    uprv_free(filename);

    return result;
}

static struct SResource *
parseResource(char *tag, UErrorCode *status)
{
    enum   ETokenType     token;
    enum   EResourceType  resType = RT_UNKNOWN;
    struct UString       *tokenValue;
    uint32_t              startline;
    uint32_t              line;

    token = getToken(&tokenValue, &startline, status);

    /* name . [ ':' type ] '{' resource '}' */
    /* This function parses from the colon onwards.  If the colon is present, parse the
       type then try to parse a resource of that type.  If there is no explicit type,
       work it out using the lookahead tokens. */
    switch (token)
    {
    case TOK_EOF:
        *status = U_INVALID_FORMAT_ERROR;
        error(startline, "Unexpected EOF encountered");
        return NULL;

    case TOK_ERROR:
        *status = U_INVALID_FORMAT_ERROR;
        return NULL;

    case TOK_COLON:
        resType = parseResourceType(status);
        expect(TOK_OPEN_BRACE, &tokenValue, &startline, status);

        if (U_FAILURE(*status))
        {
            return NULL;
        }

        break;

    case TOK_OPEN_BRACE:
        break;

    default:
        *status = U_INVALID_FORMAT_ERROR;
        error(startline, "syntax error while reading a resource, expected '{' or ':'");
        return NULL;
    }

    if (resType == RT_UNKNOWN)
    {
        /* No explicit type, so try to work it out.  At this point, we've read the first '{'.
           We could have any of the following:
              { {          => array (nested)
              { :          => array
              { string ,   => string array
              { string {   => table
              { string }   => string
        */

        token = peekToken(0, NULL, &line, status);

        if (U_FAILURE(*status))
        {
            return NULL;
        }

        if (token == TOK_OPEN_BRACE || token == TOK_COLON)
        {
            resType = RT_ARRAY;
        }
        else if (token == TOK_STRING)
        {
            token = peekToken(1, NULL, &line, status);

            if (U_FAILURE(*status))
            {
                return NULL;
            }

            switch (token)
            {
            case TOK_COMMA:       resType = RT_ARRAY;  break;
            case TOK_OPEN_BRACE:  resType = RT_TABLE;  break;
            case TOK_CLOSE_BRACE: resType = RT_STRING; break;
            default:
                *status = U_INVALID_FORMAT_ERROR;
                error(line, "Unexpected token after string, expected ',', '{' or '}'");
                return NULL;
            }
        }
        else
        {
            *status = U_INVALID_FORMAT_ERROR;
            error(line, "Unexpected token after '{'");
            return NULL;
        }

    /* printf("Type guessed as %s\n", resourceNames[resType]); */
    }

    /* We should now know what we need to parse next, so call the appropriate parser
       function and return. */
    switch (resType)
    {
    case RT_STRING:     return parseString    (tag, startline, status);
    case RT_TABLE:      return parseTable     (tag, startline, status);
    case RT_ARRAY:      return parseArray     (tag, startline, status);
    case RT_BINARY:     return parseBinary    (tag, startline, status);
    case RT_INTEGER:    return parseInteger   (tag, startline, status);
    case RT_IMPORT:     return parseImport    (tag, startline, status);
    case RT_INTVECTOR:  return parseIntVector (tag, startline, status);

    default:
        *status = U_INTERNAL_PROGRAM_ERROR;
        error(startline, "internal error: unknown resource type found and not handled");
    }

    return NULL;
}

struct SRBRoot *
parse(UCHARBUF *buf, const char *currentInputDir, UErrorCode *status)
{
    struct UString *tokenValue;
    uint32_t        line;

    initLookahead(buf, status);

    inputdir       = currentInputDir;
    inputdirLength = (inputdir != NULL) ? uprv_strlen(inputdir) : 0;

    bundle = bundle_open(status);

    if (bundle == NULL || U_FAILURE(*status))
    {
        return NULL;
    }

    expect(TOK_STRING, &tokenValue, NULL, status);
    bundle_setlocale(bundle, tokenValue->fChars, status);
    expect(TOK_OPEN_BRACE, NULL, &line, status);

    if (U_FAILURE(*status))
    {
        bundle_close(bundle, status);
        return NULL;
    }

    realParseTable(bundle->fRoot, NULL, line, status);

    if (U_FAILURE(*status))
    {
        /* realParseTable has already closed the table */
        bundle->fRoot = NULL;
        bundle_close(bundle, status);
        return NULL;
    }

    if (getToken(NULL, &line, status) != TOK_EOF)
    {
        warning(line, "extraneous text after resource bundle (perhaps unmatched braces)");
    }

    return bundle;
}
