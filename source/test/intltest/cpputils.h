/* Wraps C++ internal utilities  needed in C
Bertrand A. D.*/

#include "utypes.h"
CAPI void  printUChar(const UChar*      uniString);
CAPI void  printChar(   const char*         charString);

CAPI void T_PlatformUtilities_pathnameInContext( char *fullname, int32_t maxsize, const char * relPath);
CAPI const char *T_PlatformUtilities_getDefaultDataDirectory(void);
