/*
*******************************************************************************
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  utrace.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*/

#include "unicode/utrace.h"
#include "cstring.h"


static UTraceEntry     *pTraceEntryFunc = NULL;
static UTraceExit      *pTraceExitFunc  = NULL;
static UTraceData      *pTraceDataFunc  = NULL;
static void            *gTraceContext   = NULL;

U_CAPI void U_EXPORT2
utrace_entry(int32_t fnNumber) {
    if (pTraceEntryFunc != NULL) {
        (*pTraceEntryFunc)(gTraceContext, fnNumber);
    }
}



U_CAPI void U_EXPORT2
utrace_exit(int32_t fnNumber, UErrorCode status) {
    if (pTraceExitFunc != NULL) {
        (*pTraceExitFunc)(gTraceContext, fnNumber, status);
    }
}
 

U_CAPI void U_EXPORT2 
utrace_data(int32_t fnNumber, int32_t level, const char *fmt, ...) {
    if (pTraceDataFunc != NULL) {
           va_list args;
           va_start(args, fmt ); 
           (*pTraceDataFunc)(gTraceContext, fnNumber, level, fmt, args);
           va_end(args);
    }
}


U_CAPI int32_t U_EXPORT2
utrace_format(char *outBuf, int32_t capacity, const char *fmt, va_list args) {
    int32_t outIx  = 0;
    int32_t fmtIx  = 0;
    int32_t tbufIx = 0;
    char    fmtC;
    int32_t intArg;
    char    *ptrArg;
    char    tbuf[32];     /* Small buffer to hold numeric conversions, which may
                           *  not fit in output buffer without overflow.   */

    /*   Loop runs once for each character in the format string.
     *   Break out when format is exhausted or the output buffer fills, whichever comes first.
     */
    for (;;) {
        if (outIx >= capacity) {
            break;
        }
        fmtC = fmt[fmtIx++];
        if (fmtC != '%') {
            outBuf[outIx++] = fmtC;
            if (fmtC == 0) {
                break;
            }
            continue;
        }

        fmtC = fmt[fmtIx++];
        if (fmtC == '%' || fmtC == 0) {
            outBuf[outIx++] = '%';
            if (fmtC == 0) {
                /* Single '%' at end of fmt string.  Treat as literal.   */
                break;
            } else {
                /* %% in string, outputs a single %.   */
                continue;
            }
        }

        if (fmtC == 'v') {
            /* TODO:  vector handling... */
        }

        switch (fmtC) {
        case 'c':
            outBuf[outIx++] = (char)va_arg(args, int32_t);
            break;

        case 's':
            ptrArg = va_arg(args, char *);
            if (ptrArg == NULL) {
                if (capacity - outIx > 6) {
                    uprv_strcpy(outBuf+outIx, "*NULL*");
                    outIx += 6;
                } else {
                    outBuf[outIx++] = '0';
                }
                break;
            }

            while (*ptrArg != 0 && outIx < capacity) {
                outBuf[outIx++] = *ptrArg++;
            }
            break;

        case 'b':
        case 'h':
        case 'd':
            /*  8, 16, 32 bit ints.  Not in a vector, so these all are passed
             *  in the same way, as a plain in32_t
             */
            intArg = va_arg(args, int32_t);
            tbufIx = 0;
            if (intArg < 0) {
                tbuf[0] = '-';
                tbufIx = 1;
                intArg = -intArg;
            }
            T_CString_integerToString(tbuf + tbufIx, intArg, 10);
            for (tbufIx = 0; tbuf[tbufIx] != 0 && outIx < capacity; tbufIx++) {
                outBuf[outIx++] = tbuf[tbufIx];
            }
            break;

        case 'p':
            /*  Pointers  */
            ptrArg = va_arg(args, char *);
            // TODO:  handle 64 bit ptrs.
            intArg = (int)ptrArg;
            T_CString_integerToString(tbuf, intArg, 16);
            for (tbufIx = 0; tbuf[tbufIx] != 0 && outIx < capacity; tbufIx++) {
                outBuf[outIx++] = tbuf[tbufIx];
            }
            break;

        case 'l':
            /*  TODO:  64 bit longs  */
            outBuf[outIx++] = 'X';
            break;
            
        default:
            // %? in format string, where ? is some character not in the set
            //    of recognized format chars.  Just output it as if % wasn't there.
            outBuf[outIx++] = fmtC;
        }
    }
    return outIx;
}



U_CAPI void U_EXPORT2
utrace_setFunctions(const void *context,
                    UTraceEntry *e, UTraceExit *x, UTraceData *d,
                    int32_t traceLevel,
                    UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return;
    }

    if (traceLevel < UTRACE_OFF || traceLevel > UTRACE_VERBOSE) {
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    pTraceEntryFunc = e;
    pTraceExitFunc  = x;
    pTraceDataFunc  = d;
    utrace_level    = traceLevel;
}
