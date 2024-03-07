// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "unicode/unistr.h"
#include "unicode/locid.h"
#include "unicode/ucnv.h"
#include <stdio.h>

int main(int argc,
     char* argv[])
{
  UErrorCode status = U_ZERO_ERROR;
  const char *loc = argv[1];
  int32_t hasCountry;
  UConverter *conv = ucnv_open("utf8", &status);

  
  char16_t UBuffer[256];
  int32_t uBufLen = 0;
  char buffer[256];
  int32_t bufLen = 0;

  uBufLen = uloc_getDisplayLanguage(loc, "en", UBuffer, 256, &status);
  bufLen = ucnv_fromUChars(conv, buffer, 256, UBuffer, uBufLen, &status);
  //u_UCharsToChars(UBuffer,  buffer, uBufLen);
  buffer[bufLen] = 0;
  printf("%s", buffer);

  if(hasCountry = uloc_getCountry(loc, buffer, 256, &status)) {
    uBufLen = uloc_getDisplayCountry(loc, "en", UBuffer, 256, &status);
    bufLen = ucnv_fromUChars(conv, buffer, 256, UBuffer, uBufLen, &status);
    //u_UCharsToChars(UBuffer,  buffer, uBufLen);
    buffer[bufLen] = 0;
    printf("_%s", buffer);
  }

  if(uloc_getVariant(loc, buffer, 256, &status)) {
    uBufLen = uloc_getDisplayVariant(loc, "en", UBuffer, 256, &status);
    bufLen = ucnv_fromUChars(conv, buffer, 256, UBuffer, uBufLen, &status);
    //u_UCharsToChars(UBuffer,  buffer, uBufLen);
    buffer[bufLen] = 0;
    if(!hasCountry) {
      printf("_");
    }
    printf("_%s", buffer);
  }
  printf("\n");


  return 0;
}
