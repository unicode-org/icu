/*
**********************************************************************
*   Copyright (C) 1999-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File USC_IMPL.C
*
* Modification History:
*
*   Date        Name        Description
*   07/08/2002  Eric Mader  Creation.
******************************************************************************
*/

#include "unicode/uscript.h"
#include "usc_impl.h"
#include "cmemory.h"

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

#define PAREN_STACK_DEPTH 128

struct ParenStackEntry
{
    int32_t pairIndex;
    UScriptCode scriptCode;
};

struct UScriptRun
{
    int32_t textLength;
    const UChar *textArray;

    int32_t scriptStart;
    int32_t scriptLimit;
    UScriptCode scriptCode;

    struct ParenStackEntry parenStack[PAREN_STACK_DEPTH];
    int32_t parenSP;
};

static int8_t highBit(int32_t value);

static const UChar32 pairedChars[] = {
    0x0028, 0x0029, /* ascii paired punctuation */
    0x003c, 0x003e,
    0x005b, 0x005d,
    0x007b, 0x007d,
    0x00ab, 0x00bb, /* guillemets */
    0x2018, 0x2019, /* general punctuation */
    0x201c, 0x201d,
    0x2039, 0x203a,
    0x3008, 0x3009, /* chinese paired punctuation */
    0x300a, 0x300b,
    0x300c, 0x300d,
    0x300e, 0x300f,
    0x3010, 0x3011,
    0x3014, 0x3015,
    0x3016, 0x3017,
    0x3018, 0x3019,
    0x301a, 0x301b
};

static int8_t
highBit(int32_t value)
{
    int8_t bit = 0;

    if (value <= 0) {
        return -32;
    }

    if (value >= 1 << 16) {
        value >>= 16;
        bit += 16;
    }

    if (value >= 1 << 8) {
        value >>= 8;
        bit += 8;
    }

    if (value >= 1 << 4) {
        value >>= 4;
        bit += 4;
    }

    if (value >= 1 << 2) {
        value >>= 2;
        bit += 2;
    }

    if (value >= 1 << 1) {
        value >>= 1;
        bit += 1;
    }

    return bit;
}

static int32_t
getPairIndex(UChar32 ch)
{
    int32_t pairedCharCount = ARRAY_SIZE(pairedChars);
    int32_t pairedCharPower = 1 << highBit(pairedCharCount);
    int32_t pairedCharExtra = pairedCharCount - pairedCharPower;

    int32_t probe = pairedCharPower;
    int32_t index = 0;

    if (ch >= pairedChars[pairedCharExtra]) {
        index = pairedCharExtra;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (ch >= pairedChars[index + probe]) {
            index += probe;
        }
    }

    if (pairedChars[index] != ch) {
        index = -1;
    }

    return index;
}

static UBool
sameScript(UScriptCode scriptOne, UScriptCode scriptTwo)
{
    return scriptOne <= USCRIPT_INHERITED || scriptTwo <= USCRIPT_INHERITED || scriptOne == scriptTwo;
}

U_CAPI UScriptRun * U_EXPORT2
uscript_openRun(const UChar *src, int32_t length, UErrorCode *pErrorCode)
{
    UScriptRun *result = NULL;

    if (pErrorCode == NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    }

    result = uprv_malloc(sizeof (UScriptRun));

    if (result == NULL) {
        *pErrorCode = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    uscript_setRunText(result, src, length, pErrorCode);

    /* Release the UScriptRun if uscript_setRunText() returns an error */
    if (U_FAILURE(*pErrorCode)) {
        uprv_free(result);
        result = NULL;
    }

    return result;
}

U_CAPI void U_EXPORT2
uscript_closeRun(UScriptRun *scriptRun)
{
    if (scriptRun != NULL) {
        uprv_free(scriptRun);
    }
}

U_CAPI void U_EXPORT2
uscript_resetRun(UScriptRun *scriptRun)
{
    if (scriptRun != NULL) {
        scriptRun->scriptStart = 0;
        scriptRun->scriptLimit = 0;
        scriptRun->scriptCode  = USCRIPT_INVALID_CODE;
        scriptRun->parenSP     = -1;
    }
}

U_CAPI void U_EXPORT2
uscript_setRunText(UScriptRun *scriptRun, const UChar *src, int32_t length, UErrorCode *pErrorCode)
{
    if (pErrorCode == NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if (scriptRun == NULL || length < 0 || ((src == NULL) != (length == 0))) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    scriptRun->textArray  = src;
    scriptRun->textLength = length;

    uscript_resetRun(scriptRun);
}

U_CAPI UBool U_EXPORT2
uscript_nextRun(UScriptRun *scriptRun, int32_t *pRunStart, int32_t *pRunLimit, UScriptCode *pRunScript)
{
    int32_t startSP  = -1;  /* used to find the first new open character */
    UErrorCode error = U_ZERO_ERROR;

    /* if we've fallen off the end of the text, we're done */
    if (scriptRun == NULL || scriptRun->scriptLimit >= scriptRun->textLength) {
        return FALSE;
    }
    
    startSP = scriptRun->parenSP;
    scriptRun->scriptCode = USCRIPT_COMMON;

    for (scriptRun->scriptStart = scriptRun->scriptLimit; scriptRun->scriptLimit < scriptRun->textLength; scriptRun->scriptLimit += 1) {
        UChar   high = scriptRun->textArray[scriptRun->scriptLimit];
        UChar32 ch   = high;
        UScriptCode sc;
        int32_t pairIndex;

        /*
         * if the character is a high surrogate and it's not the last one
         * in the text, see if it's followed by a low surrogate
         */
        if (high >= 0xD800 && high <= 0xDBFF && scriptRun->scriptLimit < scriptRun->textLength - 1) {
            UChar low = scriptRun->textArray[scriptRun->scriptLimit + 1];

            /*
             * if it is followed by a low surrogate,
             * consume it and form the full character
             */
            if (low >= 0xDC00 && low <= 0xDFFF) {
                ch = (high - 0xD800) * 0x0400 + low - 0xDC00 + 0x10000;
                scriptRun->scriptLimit += 1;
            }
        }

        sc = uscript_getScript(ch, &error);
        pairIndex = getPairIndex(ch);

        /*
         * Paired character handling:
         *
         * if it's an open character, push it onto the stack.
         * if it's a close character, find the matching open on the
         * stack, and use that script code. Any non-matching open
         * characters above it on the stack will be poped.
         */
        if (pairIndex >= 0) {
            if ((pairIndex & 1) == 0) {

                /*
                 * If the paren stack is full, empty it. This
                 * means that deeply nested paired punctuation
                 * characters will be ignored, but that's an unusual
                 * case, and it's better to ignore them than to
                 * write off the end of the stack...
                 */
                if (++scriptRun->parenSP >= PAREN_STACK_DEPTH) {
                    scriptRun->parenSP = 0;
                }

                scriptRun->parenStack[scriptRun->parenSP].pairIndex = pairIndex;
                scriptRun->parenStack[scriptRun->parenSP].scriptCode  = scriptRun->scriptCode;
            } else if (scriptRun->parenSP >= 0) {
                int32_t pi = pairIndex & ~1;

                while (scriptRun->parenSP >= 0 && scriptRun->parenStack[scriptRun->parenSP].pairIndex != pi) {
                    scriptRun->parenSP -= 1;
                }

                if (scriptRun->parenSP < startSP) {
                    startSP = scriptRun->parenSP;
                }

                if (scriptRun->parenSP >= 0) {
                    sc = scriptRun->parenStack[scriptRun->parenSP].scriptCode;
                }
            }
        }

        if (sameScript(scriptRun->scriptCode, sc)) {
            if (scriptRun->scriptCode <= USCRIPT_INHERITED && sc > USCRIPT_INHERITED) {
                scriptRun->scriptCode = sc;

                /*
                 * now that we have a final script code, fix any open
                 * characters we pushed before we knew the script code.
                 */
                while (startSP < scriptRun->parenSP) {
                    scriptRun->parenStack[++startSP].scriptCode = scriptRun->scriptCode;
                }
            }

            /*
             * if this character is a close paired character,
             * pop it from the stack
             */
            if (pairIndex >= 0 && (pairIndex & 1) != 0 && scriptRun->parenSP >= 0) {
                scriptRun->parenSP -= 1;
                startSP -= 1;
            }
        } else {
            /*
             * if the run broke on a surrogate pair,
             * end it before the high surrogate
             */
            if (ch >= 0x10000) {
                scriptRun->scriptLimit -= 1;
            }

            break;
        }
    }


    if (pRunStart != NULL) {
        *pRunStart = scriptRun->scriptStart;
    }

    if (pRunLimit != NULL) {
        *pRunLimit = scriptRun->scriptLimit;
    }

    if (pRunScript != NULL) {
        *pRunScript = scriptRun->scriptCode;
    }

    return TRUE;
}
