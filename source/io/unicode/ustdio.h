/*
******************************************************************************
*
*   Copyright (C) 1998-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
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
*   02/01/01    george      Added sprintf & sscanf with all of its variants
******************************************************************************
*/

#ifndef USTDIO_H
#define USTDIO_H

#include <stdio.h>
#include <stdarg.h>

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/utrans.h"

/*
    TODO
 The following is a small list as to what is currently wrong/suggestions for
 ustdio.

 * Make sure that * in the scanf format specification works for all formats.
 * Each UFILE takes up at least 2KB.
    Look into adding setvbuf() for configurable buffers.
 * This library does buffering. The OS should do this for us already. Check on
    this, and remove it from this library, if this is the case. Double buffering
    wastes a lot of time and space.
 * Test stdin and stdout with the u_f* functions
 * Testing should be done for reading and writing multi-byte encodings,
    and make sure that a character that is contained across buffer boundries
    works even for incomplete characters.
 * Make sure that the last character is flushed when the file/string is closed.
 * snprintf should follow the C99 standard for the return value, which is
    return the number of characters (excluding the trailing '\0')
    which would have been written to the destination string regardless
    of available space. This is like pre-flighting.
 * Everything that uses %s should do what operator>> does for UnicodeString.
    It should convert one byte at a time, and once a character is
    converted then check to see if it's whitespace or in the scanset.
    If it's whitespace or in the scanset, put all the bytes back (do nothing
    for sprintf/sscanf).
 * If bad string data is encountered, make sure that the function fails
    without memory leaks and the unconvertable characters are valid
    substitution or are escaped characters.
 * u_fungetc() can't unget a character when it's at the beginning of the
    internal conversion buffer. For example, read the buffer size # of
    characters, and then ungetc to get the previous character that was
    at the end of the last buffer.
 * u_fflush() and u_fclose should return an int32_t like C99 functions.
    0 is returned if the operation was successful and EOF otherwise.
 * u_fsettransliterator does not support U_READ side of transliteration.
 * The format specifier should limit the size of a format or honor it in
    order to prevent buffer overruns.  (e.g. %256.256d).
 * u_fread and u_fwrite don't exist. They're needed for reading and writing
    data structures without any conversion.
 * u_file_read and u_file_write are used for writing strings. u_fgets and
    u_fputs or u_fread and u_fwrite should be used to do this.
 * The width parameter for all scanf formats, including scanset, needs
    better testing. This prevents buffer overflows.
 * Figure out what is suppose to happen when a codepage is changed midstream.
    Maybe a flush or a rewind are good enough.
 * Make sure that a UFile opened with "rw" can be used after using
    u_fflush with a u_frewind.
 * scanf(%i) should detect what type of number to use.
 * Complete the file documentation with proper doxygen formatting.
    See http://oss.software.ibm.com/pipermail/icu/2003-July/005647.html
*/

/**
 * \file
 * \brief C API: Unicode stdio-like API
 *
 * <h2>Unicode stdio-like C API</h2>
printf
fmt type        Comment
%E  double      Scientific with an uppercase exponent
%e  double      Scientific with a lowercase exponent
%G  double      Use %E or %f for best format
%g  double      Use %e or %f for best format
%f  double      Simple floating point without the exponent
%X  int32_t     ustdio special uppercase hex radix formatting
%x  int32_t     ustdio special lowercase hex radix formatting
%d  int32_t     Decimal format
%i  int32_t     Same as %d
%n  int32_t     count (write the number of chars written)
%o  int32_t     octal ustdio special octal radix formatting
%u  uint32_t    Decimal format
%p  void *      Prints the pointer value
%s  char *      Use default converter or specified converter from fopen
%hs char *      (Unimplemented) Use invariant converter
%ls N/A         (Unimplemented) Reserved for future implementation
%c  char        Use default converter or specified converter from fopen
%hc char        (Unimplemented) Use invariant converter
%lc N/A         (Unimplemented) Reserved for future implementation
%S  UChar *     Null terminated UTF-16 string
%hS char *      (Unimplemented) Null terminated UTF-8 string
%lS UChar32 *   (Unimplemented) Null terminated UTF-32 string
%C  UChar       16-bit Unicode code unit
%hC char        (Unimplemented) 8-bit Unicode code unit
%lC UChar32     (Unimplemented) 32-bit Unicode code unit
%%  N/A         Show a percent sign

Format modifiers
%h    int16_t   short format for %d, %i, %o, %x
%l    int32_t   long format for %d, %i, %o, %x (no effect)
%ll   int64_t   long long format for %d, %i, %o, %x
%h    uint16_t  short format for %u
%l    uint32_t  long format for %u (no effect)
%ll   uint64_t  (Unimplemented) long long format for %u
%-    N/A       Left justify
%+    N/A       Always show the plus or minus sign. Needs data for plus sign.
%     N/A       Instead of a "+" output a blank character for positive numbers.
%#    N/A       Precede octal value with 0, hex with 0x and show the 
                decimal point for floats.
%num  N/A       Width of input/output. num is an actual number from 0 to 
                some large number.
%.num N/A       Significant digits precision. num is an actual number from
                0 to some large number. Currently can only specify precision
                before or after decimal, and not total precision.

printf modifier
%*  int32_t     Next argument after this one specifies the width

scanf modifier
%*  N/A         This field is scanned, but not stored

 */


/**
 * When an end of file is encountered, this value can be returned.
 * @see u_fgetc
 * @draft 3.0
 */
#define U_EOF 0xFFFF

/** Forward declaration of a Unicode-aware file @draft 3.0 */
typedef struct UFILE UFILE;

/**
 * Enum for which direction of stream a transliterator applies to.
 * @see u_fsettransliterator
 * @draft 3.0
 */
typedef enum { 
   U_READ = 1,
   U_WRITE = 2, 
   U_READWRITE =3  /* == (U_READ | U_WRITE) */ 
} UFileDirection;

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
 * @return A new UFILE, or NULL if an error occurred.
 * @draft 3.0
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
 * @return A new UFILE, or NULL if an error occurred.
 * @draft 3.0
 */
U_CAPI UFILE* U_EXPORT2
u_finit(FILE        *f,
    const char    *locale,
    const char    *codepage);

/**
 * Create a UFILE that can be used for localized formatting or parsing.
 * The u_sprintf and u_sscanf functions do not read or write numbers for a
 * specific locale. The ustdio.h file functions can be used on this UFILE.
 * The string is usable once u_fclose or u_fflush has been called on the
 * returned UFILE.
 * @param stringBuf The string used for reading or writing.
 * @param count The number of code units available for use in stringBuf
 * @param locale The locale whose conventions will be used to format 
 * and parse output. If this parameter is NULL, the default locale will 
 * be used.
 * @return A new UFILE, or NULL if an error occurred.
 * @draft 3.0
 */
U_CAPI UFILE* U_EXPORT2
u_fstropen(UChar      *stringBuf,
           int32_t     capacity,
           const char *locale);

/**
 * Close a UFILE.
 * @param file The UFILE to close.
 * @draft 3.0
 */
U_CAPI void U_EXPORT2
u_fclose(UFILE *file);

/**
 * Tests if the UFILE is at the end of the file stream.
 * @param f The UFILE from which to read.
 * @return Returns TRUE after the first read operation that attempts to
 * read past the end of the file. It returns FALSE if the current position is
 * not end of file.
 * @draft 3.0
*/
U_CAPI UBool U_EXPORT2
u_feof(UFILE  *f);

/**
 * Flush output of a UFILE. Implies a flush of
 * converter/transliterator state. (That is, a logical break is
 * made in the output stream - for example if a different type of
 * output is desired.)  The underlying OS level file is also flushed.
 * @param file The UFILE to flush.
 * @draft 3.0
 */
U_CAPI void U_EXPORT2
u_fflush(UFILE *file);

/**
 * Rewind the file pointer to the beginning of the file.
 * @param file The UFILE to rewind.
 * @draft 3.0
 */
U_CAPI void
u_frewind(UFILE *file);

/**
 * Get the FILE* associated with a UFILE.
 * @param f The UFILE
 * @return A FILE*, owned by the UFILE.  The FILE <EM>must not</EM> be closed.
 * @draft 3.0
 */
U_CAPI FILE* U_EXPORT2
u_fgetfile(UFILE *f);

#if !UCONFIG_NO_FORMATTING

/**
 * Get the locale whose conventions are used to format and parse output.
 * This is the same locale passed in the preceding call to<TT>u_fsetlocale</TT>
 * or <TT>u_fopen</TT>.
 * @param file The UFILE to set.
 * @return The locale whose conventions are used to format and parse output.
 * @draft 3.0
 */
U_CAPI const char* U_EXPORT2
u_fgetlocale(UFILE *file);

/**
 * Set the locale whose conventions will be used to format and parse output.
 * @param locale The locale whose conventions will be used to format 
 * and parse output.
 * @param file The UFILE to query.
 * @return NULL if successful, otherwise a negative number.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fsetlocale(UFILE      *file,
             const char *locale);

#endif

/**
 * Get the codepage in which data is written to and read from the UFILE.
 * This is the same codepage passed in the preceding call to 
 * <TT>u_fsetcodepage</TT> or <TT>u_fopen</TT>.
 * @param file The UFILE to query.
 * @return The codepage in which data is written to and read from the UFILE,
 * or NULL if an error occurred.
 * @draft 3.0
 */
U_CAPI const char* U_EXPORT2
u_fgetcodepage(UFILE *file);

/**
 * Set the codepage in which data will be written to and read from the UFILE.
 * All Unicode data written to the UFILE will be converted to this codepage
 * before it is written to the underlying FILE*. It it generally a bad idea to
 * mix codepages within a file. This should only be called right
 * after opening the <TT>UFile</TT>, or after calling <TT>u_frewind</TT>.
 * @param codepage The codepage in which data will be written to 
 * and read from the file. For example <TT>"latin-1"</TT> or <TT>"ibm-943</TT>.
 * A value of NULL means the default codepage for the UFILE's current 
 * locale will be used.
 * @param file The UFILE to set.
 * @return 0 if successful, otherwise a negative number.
 * @see u_frewind
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fsetcodepage(const char   *codepage,
               UFILE        *file);


/**
 * Returns an alias to the converter being used for this file.
 * @param file The UFILE to set.
 * @return alias to the converter
 * @draft 3.0
 */
U_CAPI UConverter* U_EXPORT2 u_fgetConverter(UFILE *f);

/* Output functions */

/**
 * Write formatted data to a UFILE.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fprintf(UFILE         *f,
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
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vfprintf(UFILE        *f,
           const char   *patternSpecification,
           va_list      ap);

/**
 * Write formatted data to a UFILE.
 * @param f The UFILE to which to write.
 * @param patternSpecification A pattern specifying how <TT>u_fprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fprintf_u(UFILE       *f,
            const UChar *patternSpecification,
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
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vfprintf_u(UFILE      *f,
            const UChar *patternSpecification,
            va_list     ap);

/**
 * Write a Unicode to a UFILE.  The null (U+0000) terminated UChar*
 * <TT>s</TT> will be written to <TT>f</TT>, excluding the NULL terminator.
 * A newline will be added to <TT>f</TT>.
 * @param s The UChar* to write.
 * @param f The UFILE to which to write.
 * @return A non-negative number if successful, EOF otherwise.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fputs(const UChar *s,
        UFILE       *f);

/**
 * Write a UChar to a UFILE.
 * @param uc The UChar to write.
 * @param f The UFILE to which to write.
 * @return The character written if successful, EOF otherwise.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fputc(UChar   uc,
        UFILE  *f);

/**
 * Write Unicode to a UFILE.
 * The ustring passed in will be converted to the UFILE's underlying
 * codepage before it is written.
 * @param ustring A pointer to the Unicode data to write.
 * @param count The number of Unicode characters to write
 * @param f The UFILE to which to write.
 * @return The number of Unicode characters written.
 * @see u_fputs
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_file_write(const UChar    *ustring, 
             int32_t        count, 
             UFILE          *f);


/* Input functions */

/**
 * Read formatted data from a UFILE.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fscanf(UFILE      *f,
         const char *patternSpecification,
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
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vfscanf(UFILE         *f,
          const char    *patternSpecification,
          va_list        ap);

/**
 * Read formatted data from a UFILE.
 * @param f The UFILE from which to read.
 * @param patternSpecification A pattern specifying how <TT>u_fscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_fscanf_u(UFILE        *f,
           const UChar  *patternSpecification,
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
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vfscanf_u(UFILE       *f,
            const UChar *patternSpecification,
            va_list      ap);

/**
 * Read one line of text into a UChar* string from a UFILE. The newline
 * at the end of the line is read into the string. The string is always
 * null terminated
 * @param f The UFILE from which to read.
 * @param n The maximum number of characters - 1 to read.
 * @param s The UChar* to receive the read data.  Characters will be
 * stored successively in <TT>s</TT> until a newline or EOF is
 * reached. A null character (U+0000) will be appended to <TT>s</TT>.
 * @return A pointer to <TT>s</TT>, or NULL if no characters were available.
 * @draft 3.0
 */
U_CAPI UChar* U_EXPORT2
u_fgets(UChar  *s,
        int32_t n,
        UFILE  *f);

/**
 * Read a UChar from a UFILE. It is recommended that <TT>u_fgetcx</TT>
 * used instead for proper parsing functions, but sometimes reading
 * code units is needed instead of codepoints.
 *
 * @param f The UFILE from which to read.
 * @return The UChar value read, or U+FFFF if no character was available.
 * @draft 3.0
 */
U_CAPI UChar U_EXPORT2
u_fgetc(UFILE   *f);

/**
 * Read a UChar32 from a UFILE.
 *
 * @param f The UFILE from which to read.
 * @return The UChar32 value read, or U_EOF if no character was
 * available, or U+FFFFFFFF if an ill-formed character was
 * encountered.
 * @see u_unescape()
 * @draft 3.0
 */
U_CAPI UChar32 U_EXPORT2
u_fgetcx(UFILE  *f);

/**
 * Unget a UChar from a UFILE.
 * If this function is not the first to operate on <TT>f</TT> after a call
 * to <TT>u_fgetc</TT>, the results are undefined.
 * If this function is passed a character that was not recieved from the
 * previous <TT>u_fgetc</TT> or <TT>u_fgetcx</TT> call, the results are undefined.
 * @param c The UChar to put back on the stream.
 * @param f The UFILE to receive <TT>c</TT>.
 * @return The UChar32 value put back if successful, U_EOF otherwise.
 * @draft 3.0
 */
U_CAPI UChar32 U_EXPORT2
u_fungetc(UChar32   c,
      UFILE        *f);

/**
 * Read Unicode from a UFILE.
 * Bytes will be converted from the UFILE's underlying codepage, with
 * subsequent conversion to Unicode. The data will not be NULL terminated.
 * @param chars A pointer to receive the Unicode data.
 * @param count The number of Unicode characters to read.
 * @param f The UFILE from which to read.
 * @return The number of Unicode characters read.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_file_read(UChar        *chars, 
        int32_t        count, 
        UFILE         *f);

#if !UCONFIG_NO_TRANSLITERATION

/**
 * Set a transliterator on the UFILE. The transliterator will be owned by the
 * UFILE. 
 * @param file The UFILE to set transliteration on
 * @param adopt The UTransliterator to set. Can be NULL, which will
 * mean that no transliteration is used.
 * @param direction either U_READ, U_WRITE, or U_READWRITE - sets
 *  which direction the transliterator is to be applied to. If
 * U_READWRITE, the "Read" transliteration will be in the inverse
 * direction.
 * @param status ICU error code.
 * @return The previously set transliterator, owned by the
 * caller. If U_READWRITE is specified, only the WRITE transliterator
 * is returned. In most cases, the caller should call utrans_close()
 * on the result of this function.
 * @draft 3.0
 */
U_CAPI UTransliterator* U_EXPORT2
u_fsettransliterator(UFILE *file, UFileDirection direction,
                     UTransliterator *adopt, UErrorCode *status);

#endif


/* Output string functions */


/**
 * Write formatted data to a Unicode string.
 *
 * @param buffer The Unicode String to which to write.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode code units written to <TT>buffer</TT>. This
 * does not include the terminating null character.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_sprintf(UChar       *buffer,
        const char    *patternSpecification,
        ... );

/**
 * Write formatted data to a Unicode string. When the number of code units
 * required to store the data exceeds <TT>count</TT>, then <TT>count</TT> code
 * units of data are stored in <TT>buffer</TT> and a negative value is
 * returned. When the number of code units required to store the data equals
 * <TT>count</TT>, the string is not null terminated and <TT>count</TT> is
 * returned.
 *
 * @param buffer The Unicode String to which to write.
 * @param count The number of code units to read.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode code units written to <TT>buffer</TT>. This
 * does not include the terminating null character.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_snprintf(UChar      *buffer,
        int32_t       count,
        const char    *patternSpecification,
        ... );

/**
 * Write formatted data to a Unicode string.
 * This is identical to <TT>u_sprintf</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 *
 * @param buffer The Unicode string to which to write.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>buffer</TT>.
 * @see u_sprintf
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsprintf(UChar      *buffer,
        const char    *patternSpecification,
        va_list        ap);

/**
 * Write formatted data to a Unicode string.
 * This is identical to <TT>u_snprintf</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.<br><br>
 * When the number of code units required to store the data exceeds
 * <TT>count</TT>, then <TT>count</TT> code units of data are stored in
 * <TT>buffer</TT> and a negative value is returned. When the number of code
 * units required to store the data equals <TT>count</TT>, the string is not
 * null terminated and <TT>count</TT> is returned.
 *
 * @param buffer The Unicode string to which to write.
 * @param count The number of code units to read.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>buffer</TT>.
 * @see u_sprintf
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsnprintf(UChar     *buffer,
        int32_t       count,
        const char    *patternSpecification,
        va_list        ap);

/**
 * Write formatted data to a Unicode string.
 *
 * @param buffer The Unicode string to which to write.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>buffer</TT>.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_sprintf_u(UChar      *buffer,
        const UChar    *patternSpecification,
        ... );

/**
 * Write formatted data to a Unicode string. When the number of code units
 * required to store the data exceeds <TT>count</TT>, then <TT>count</TT> code
 * units of data are stored in <TT>buffer</TT> and a negative value is
 * returned. When the number of code units required to store the data equals
 * <TT>count</TT>, the string is not null terminated and <TT>count</TT> is
 * returned.
 *
 * @param buffer The Unicode string to which to write.
 * @param count The number of code units to read.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @return The number of Unicode characters written to <TT>buffer</TT>.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_snprintf_u(UChar     *buffer,
        int32_t        count,
        const UChar    *patternSpecification,
        ... );

/**
 * Write formatted data to a Unicode string.
 * This is identical to <TT>u_sprintf_u</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 *
 * @param buffer The Unicode string to which to write.
 * @param count The number of code units to read.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @see u_sprintf_u
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsprintf_u(UChar     *buffer,
        const UChar    *patternSpecification,
        va_list        ap);

/**
 * Write formatted data to a Unicode string.
 * This is identical to <TT>u_snprintf_u</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 * When the number of code units required to store the data exceeds
 * <TT>count</TT>, then <TT>count</TT> code units of data are stored in
 * <TT>buffer</TT> and a negative value is returned. When the number of code
 * units required to store the data equals <TT>count</TT>, the string is not
 * null terminated and <TT>count</TT> is returned.
 *
 * @param buffer The Unicode string to which to write.
 * @param locale The locale to use for formatting the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sprintf</TT> will
 * interpret the variable arguments received and format the data.
 * @param ap The argument list to use.
 * @return The number of Unicode characters written to <TT>f</TT>.
 * @see u_sprintf_u
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsnprintf_u(UChar *buffer,
        int32_t         count,
        const UChar     *patternSpecification,
        va_list         ap);

/* Input string functions */

/**
 * Read formatted data from a Unicode string.
 *
 * @param buffer The Unicode string from which to read.
 * @param locale The locale to use for parsing the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_sscanf(const UChar   *buffer,
        const char     *patternSpecification,
        ... );

/**
 * Read formatted data from a Unicode string.
 * This is identical to <TT>u_sscanf</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 *
 * @param buffer The Unicode string from which to read.
 * @param locale The locale to use for parsing the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @param ap The argument list to use.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @see u_sscanf
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsscanf(const UChar  *buffer,
        const char     *patternSpecification,
        va_list        ap);

/**
 * Read formatted data from a Unicode string.
 *
 * @param buffer The Unicode string from which to read.
 * @param locale The locale to use for parsing the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_sscanf_u(const UChar  *buffer,
        const UChar     *patternSpecification,
        ... );

/**
 * Read formatted data from a Unicode string.
 * This is identical to <TT>u_sscanf_u</TT>, except that it will
 * <EM>not</EM> call <TT>va_start/TT> and <TT>va_end</TT>.
 *
 * @param buffer The Unicode string from which to read.
 * @param locale The locale to use for parsing the numbers, dates and other
 * locale specific information.
 * @param patternSpecification A pattern specifying how <TT>u_sscanf</TT> will
 * interpret the variable arguments received and parse the data.
 * @param ap The argument list to use.
 * @return The number of items successfully converted and assigned, or EOF
 * if an error occurred.
 * @see u_sscanf_u
 * @draft 3.0
 */
U_CAPI int32_t U_EXPORT2
u_vsscanf_u(const UChar *buffer,
        const UChar     *patternSpecification,
        va_list         ap);


#endif


