/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*
* File FILESTRM.H
*
* Contains FileStream interface
*
* @author       Glenn Marcy
*
* Modification History:
*
*   Date        Name        Description
*   5/8/98      gm          Created.
*  03/02/99     stephen     Reordered params in ungetc to match stdio
*                           Added wopen
*
*****************************************************************************************
*/

#ifndef FILESTRM_H
#define FILESTRM_H

#ifndef _UTYPES
#include "utypes.h"
#endif

#include <wchar.h>

typedef struct _FileStream FileStream;

CAPI FileStream* U_EXPORT2
T_FileStream_open(const char* filename, const char* mode);

CAPI FileStream* U_EXPORT2
T_FileStream_wopen(const wchar_t* filename, const wchar_t* mode);

CAPI void U_EXPORT2
T_FileStream_close(FileStream* fileStream);

CAPI bool_t U_EXPORT2
T_FileStream_file_exists(const char* filename);


CAPI FileStream* U_EXPORT2
T_FileStream_tmpfile(void);

CAPI int32_t U_EXPORT2
T_FileStream_read(FileStream* fileStream, void* addr, int32_t len);

CAPI int32_t U_EXPORT2
T_FileStream_write(FileStream* fileStream, const void* addr, int32_t len);

CAPI void U_EXPORT2
T_FileStream_rewind(FileStream* fileStream);

/*Added by Bertrand A. D. */
CAPI char * U_EXPORT2
T_FileStream_readLine(FileStream* fileStream, char* buffer, int32_t length);

CAPI int32_t U_EXPORT2
T_FileStream_writeLine(FileStream* fileStream, const char* buffer);

CAPI int32_t U_EXPORT2
T_FileStream_putc(FileStream* fileStream, int32_t ch);

CAPI int U_EXPORT2
T_FileStream_getc(FileStream* fileStream);

CAPI int32_t U_EXPORT2
T_FileStream_ungetc(int32_t ch, FileStream *fileStream);

CAPI int32_t U_EXPORT2
T_FileStream_peek(FileStream* fileStream);

CAPI int32_t U_EXPORT2
T_FileStream_size(FileStream* fileStream);

CAPI int U_EXPORT2
T_FileStream_eof(FileStream* fileStream);

CAPI int U_EXPORT2
T_FileStream_error(FileStream* fileStream);

CAPI void U_EXPORT2
T_FileStream_setError(FileStream* fileStream);

CAPI FileStream* U_EXPORT2
T_FileStream_stdin(void);

CAPI FileStream* U_EXPORT2
T_FileStream_stdout(void);

CAPI FileStream* U_EXPORT2
T_FileStream_stderr(void);

#endif /* _FILESTRM*/




