/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ustdio.h
*
* Modification History:
*
*   Date        Name        Description
*   10/16/98    stephen     Creation.
*   11/06/98    stephen     Modified per code review.
*   03/12/99    stephen     Modified for new C API.
*   07/19/99    stephen     Minor doc update.
*******************************************************************************
*/

#ifndef USTDIO_H
#define USTDIO_H

#include <stdio.h>
#include <stdarg.h>

#include "unicode/utypes.h"
#include <unicode/ucnv.h>


#define U_EOF 0xFFFF

/** Forward declaration of a Unicode-aware file */
typedef struct UFILE UFILE;



/**
 * Open a UFILE.
 * A UFILE is a wrapper around a FILE* that is locale and codepage aware.
 * That is, data written to a UFILE will be formatted using the conventions
 * specified by that UFILE's Locale; this data will be in the character set
 * specified by that UFILE's codepage.
 * @param filename The name of the file to open.
 * @param perm The read/write permission for the UFILE; one of "r", "w", "rw"
 * @param locale The locale whose conventions will be used to format 
 * and parse output. If this parameter is NULL, the default locale will 
 * be used.
 * @param codepage The codepage in which data will be written to and
 * read from the file. If this paramter is NULL, data will be written and
 * read using the default codepage for <TT>locale</TT>, unless <TT>locale</TT>
 * is NULL, in which case the system default codepage will be used.
 * @return A new UFILE, or 0 if an error occurred.
 */
U_CAPI UFILE* U_EXPORT2
u_fopen(const char    *filename,
    const char    *perm,
    const char    *locale,
    const char    *codepage);

/**
 * Open a UFILE on top of an existing FILE* stream.
 * @param f The FILE* to which this UFILE will attach.
 * @param locale The locale whose conventions will be used to format 
 * and parse output. If this parameter is NULL, the default locale will 
 * be used.
 * @param codepage The codepage in which data will be written to and
 * read from the file. If this paramter is NULL, data will be written and
 * read using the default codepage for <TT>locale</TT>, unless <TT>locale</TT>
 * is NULL, in which case the system default codepage will be used.
 * @return A new UFILE, or 0 if an error occurred.
 */
U_CAPI UFILE* U_EXPORT2
u_finit(FILE        *f,
    const char    *locale,
    const char    *codepage);

/**
 * Close a UFILE.
 * @param file The UFILE to close.
 */
U_CAPI void U_EXPORT2
u_fclose(UFILE *file);

/**
 * Get the FILE* associated with a UFILE.
 * @param f The UFILE
 * @return A FILE*, owned by the UFILE.  The FILE <EM>must not</EM> be closed.
 */
U_CAPI FILE* U_EXPORT2
u_fgetfile(UFILE *f);

/**
 * Get the locale whose conventions are used to format and parse output.
 * This is the same locale passed in the preceding call to<TT>u_fsetlocale</TT>
 * or <TT>u_fopen</TT>.
 * @param file The UFILE to set.
 * @return The locale whose conventions are used to format and parse output.
 */
U_CAPI const char* U_EXPORT2
u_fgetlocale(UFILE *file);

/**
 * Set the locale whose conventions will be used to format and parse output.
 * @param locale The locale whose conventions will be used to format 
 * and parse output.
 * @param file The UFILE to query.
 * @return 0 if successful, otherwise a negative number.
 */
U_CAPI int32_t U_EXPORT2
u_fsetlocale(const char        *locale,
         UFILE        *file);

/**
 * Get the codepage in which data is written to and read from the UFILE.
 * This is the same codepage passed in the preceding call to 
 * <TT>u_fsetcodepage</TT> or <TT>u_fopen</TT>.
 * @param file The UFILE to query.
 * @return The codepage in which data is written to and read from the UFILE,
 * or 0 if an error occurred.
 */
U_CAPI const char* U_EXPORT2
u_fgetcodepage(UFILE *file);

/**
 * Set the codepage in which data will be written to and read from the UFILE.
 * All Unicode data written to the UFILE will be converted to this codepage
 * before it is written to the underlying FILE*.
 * @param codepage The codepage in which data will be written to 
 * and read from the file. For example <TT>"latin-1"</TT> or <TT>"ibm-943</TT>.
 * A value of NULL means the default codepage for the UFILE's current 
 * locale will be used.
 * @param file The UFILE to set.
 * @return 0 if successful, otherwise a negative number.
 */
U_CAPI int32_t U_EXPORT2
u_fsetcodepage(const char    *codepage,
           UFILE        *file);


/**
 * Returns an alias to the converter being used for this file.
 * @param file The UFILE to set.
 * @return alias to the converter
 */
U_CAPI UConverter U_EXPORT2 *u_fgetConverter(UFILE *f);

/* Output functions */

/**
 * Write formatted data to a UFILE.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>f</TT>.
 */
U_CAPI int32_t U_EXPORT2
u_fprintf(    UFILE        *f,
        const char    *patternSpecification,
        ... );

/**
 * Write formatted data to a UFILE.
 * This is identical to <TT>u_fprintf</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @see u_fprintf
 */
U_CAPI int32_t U_EXPORT2
u_vfprintf(    UFILE        *f,
        const char    *patternSpecification,
        va_list        ap);

/**
 * Write formatted data to a UFILE.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>f</TT>.
 */
U_CAPI int32_t U_EXPORT2
u_fprintf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        ... );

/**
 * Write formatted data to a UFILE.
 * This is identical to <TT>u_fprintf_u</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @see u_fprintf_u
 */
U_CAPI int32_t U_EXPORT2
u_vfprintf_u(    UFILE        *f,
        const UChar    *patternSpecification,
        va_list        ap);

/**
 * Write a Unicode to a UFILE.  The null (U+0000) terminated UChar*
 * <TT>s</TT> will be written to <TT>f</TT>, excluding the NULL terminator.
 * A newline will be added to <TT>f</TT>.
 * @param s The UChar* to write.
 * @param f The UFILE to which to write.
 * @return A non-negative number if successful, EOF otherwise.
 */
U_CAPI int32_t U_EXPORT2
u_fputs(const UChar    *s,
    UFILE        *f);

/**
 * Write a UChar to a UFILE.
 * @param uc The UChar to write.
 * @param f The UFILE to which to write.
 * @return The character written if successful, EOF otherwise.
 */
U_CAPI int32_t U_EXPORT2
u_fputc(UChar        uc,
    UFILE        *f);

/**
 * Write Unicode to a UFILE.
 * The ustring passed in will be converted to the UFILE's underlying
 * codepage before it is written.
 * @param chars A pointer to the Unicode data to write.
 * @param count The number of Unicode characters to write
 * @param f The UFILE to which to write.
 * @return The number of Unicode characters written.
 */
U_CAPI int32_t U_EXPORT2
u_file_write(const UChar     *chars, 
         int32_t        count, 
         UFILE         *f);


/* Input functions */

/**
 * Read formatted data from a UFILE.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 */
U_CAPI int32_t U_EXPORT2
u_fscanf(    UFILE        *f,
        const char     *patternSpecification,
        ... );

/**
 * Read formatted data from a UFILE.
 * This is identical to <TT>u_fscanf</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @param ap The argument list to use.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @see u_fscanf
 */
U_CAPI int32_t U_EXPORT2
u_vfscanf(    UFILE        *f,
        const char     *patternSpecification,
        va_list        ap);

/**
 * Read formatted data from a UFILE.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 */
U_CAPI int32_t U_EXPORT2
u_fscanf_u(    UFILE        *f,
        const UChar     *patternSpecification,
        ... );

/**
 * Read formatted data from a UFILE.
 * This is identical to <TT>u_fscanf_u</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @param ap The argument list to use.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @see u_fscanf_u
 */
U_CAPI int32_t U_EXPORT2
u_vfscanf_u(    UFILE        *f,
        const UChar     *patternSpecification,
        va_list        ap);

/**
 * Read a UChar* from a UFILE.
 * @param f The UFILE from which to read.
 * @param n The maximum number of characters - 1 to read.
 * @param s The UChar* to receive the read data.  Characters will be
 * stored successively in <TT>s</TT> until a newline or EOF is
 * reached. A NULL character (U+0000) will be appended to <TT>s</TT>.
 * @return A pointer to <TT>s</TT>, or 0 if no characters were available.
 */
U_CAPI UChar* U_EXPORT2
u_fgets(UFILE        *f,
    int32_t        n,
    UChar        *s);

/**
 * Read a UChar from a UFILE.
 * @param f The UFILE from which to read.
 * @return The UChar value read, or U+FFFF if no character was available.
 */
U_CAPI UChar U_EXPORT2
u_fgetc(UFILE        *f);

/**
 * Unget a UChar from a UFILE.
 * If this function is not the first to operate on <TT>f</TT> after a call
 * to <TT>u_fgetc</TT>, the results are undefined.
 * @param c The UChar to put back on the stream.
 * @param f The UFILE to receive <TT>c</TT>.
 * @return The UChar value put back if successful, U+FFFF otherwise.
 */
U_CAPI UChar U_EXPORT2
u_fungetc(UChar        c,
      UFILE        *f);

/**
 * Read Unicode from a UFILE.
 * Bytes will be converted from the UFILE's underlying codepage, with
 * subsequent conversion to Unicode.
 * @param chars A pointer to receive the Unicode data.
 * @param count The number of Unicode characters to read.
 * @param f The UFILE from which to read.
 * @return The number of Unicode characters read.
 */
U_CAPI int32_t U_EXPORT2
u_file_read(UChar        *chars, 
        int32_t        count, 
        UFILE         *f);

#endif





