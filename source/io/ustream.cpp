/*
**********************************************************************
*   Copyright (C) 2001-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*  FILE NAME : ustream.cpp
*
*   Modification History:
*
*   Date        Name        Description
*   06/25/2001  grhoten     Move iostream from unistr.h to here
******************************************************************************
*/


#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/ustream.h"
#include "unicode/ucnv.h"
#include "unicode/uchar.h"
#include "ustr_cnv.h"
#include <string.h>

// console IO

#if U_IOSTREAM_SOURCE >= 198506

#if U_IOSTREAM_SOURCE >= 199711
#define STD_NAMESPACE std::
#else
#define STD_NAMESPACE
#endif

#define STD_OSTREAM STD_NAMESPACE ostream
#define STD_ISTREAM STD_NAMESPACE istream

U_NAMESPACE_BEGIN

U_IO_API STD_OSTREAM & U_EXPORT2
operator<<(STD_OSTREAM& stream, const UnicodeString& str)
{
    if(str.length() > 0) {
        char buffer[200];
        UConverter *converter;
        UErrorCode errorCode = U_ZERO_ERROR;

        // use the default converter to convert chunks of text
        converter = u_getDefaultConverter(&errorCode);
        if(U_SUCCESS(errorCode)) {
            const UChar *us = str.getBuffer();
            const UChar *uLimit = us + str.length();
            char *s, *sLimit = buffer + sizeof(buffer);
            do {
                errorCode = U_ZERO_ERROR;
                s = buffer;
                ucnv_fromUnicode(converter, &s, sLimit, &us, uLimit, 0, FALSE, &errorCode);

                // write this chunk
                if(s > buffer) {
                    stream.write(buffer, (int32_t)(s - buffer));
                }
            } while(errorCode == U_BUFFER_OVERFLOW_ERROR);
            u_releaseDefaultConverter(converter);
        }
    }

/*    stream.flush();*/
    return stream;
}

U_IO_API STD_ISTREAM & U_EXPORT2
operator>>(STD_ISTREAM& stream, UnicodeString& str)
{
    /* ipfx should eat whitespace when ios::skipws is set */
    UChar uBuffer[16];
    char buffer[16];
    int32_t idx = 0;
    UConverter *converter;
    UErrorCode errorCode = U_ZERO_ERROR;

    str.truncate(0);
    // use the default converter to convert chunks of text
    converter = u_getDefaultConverter(&errorCode);
    if(U_SUCCESS(errorCode)) {
        UChar *us = uBuffer;
        const UChar *uLimit = uBuffer + sizeof(uBuffer)/sizeof(*uBuffer);
        const char *s, *sLimit;
        char ch;
        UChar ch32;
        UBool intialWhitespace = TRUE;

        /* We need to consume one byte at a time to see what is considered whitespace. */
        while (!stream.eof()) {
            ch = stream.get();
            sLimit = &ch + 1;
            errorCode = U_ZERO_ERROR;
            us = uBuffer;
            s = &ch;
            ucnv_toUnicode(converter, &us, uLimit, &s, sLimit, 0, FALSE, &errorCode);
            if(U_FAILURE(errorCode)) {
                /* Something really bad happened */
                return stream;
            }
            /* Was the character consumed? */
            if (us != uBuffer) {
                /* Reminder: ibm-1390 & JISX0213 can output 2 Unicode code points */
                int32_t uBuffSize = us-uBuffer;
                int32_t uBuffIdx = 0;
                while (uBuffIdx < uBuffSize) {
                    U16_NEXT(uBuffer, uBuffIdx, uBuffSize, ch32);
                    if (u_isWhitespace(ch32)) {
                        if (!intialWhitespace) {
                            buffer[idx++] = ch;
                            while (idx > 0) {
                                stream.putback(buffer[--idx]);
                            }
                            goto STOP_READING;
                        }
                        /* else skip intialWhitespace */
                    }
                    else {
                        str.append(ch32);
                        intialWhitespace = FALSE;
                    }
                }
                idx = 0;
            }
            else {
                buffer[idx++] = ch;
            }
        }
STOP_READING:
        u_releaseDefaultConverter(converter);
    }

/*    stream.flush();*/
    return stream;
}

U_NAMESPACE_END

#endif

