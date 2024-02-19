// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009-2012, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include <unicode/datefmt.h>
#include <unicode/udat.h>
#include <unicode/uclean.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>


/* String to use. */
const UDate stuff = 1299977771000.0L;

#include "provider_version.h"


#define LOCALE_COUNT 4
const char *locale[LOCALE_COUNT] = { "fi", "en_US", "ja", "ml" }; /* List of locales to test */

/**
 * Set up ICU, print # of available collators
 */
void setup(UErrorCode &status) {
    u_init(&status);
  
    fprintf(stderr, "ICU %s init: %s\n", U_ICU_VERSION, u_errorName(status));

    // int32_t count;
    // StringEnumeration *se = DateFormat::getAvailableLocales();
    // count = se->count(status);
    // fprintf(stderr, "# DateFormats now available: %d,\t%s - %d providers expected.\n", count, u_errorName(status), (int32_t)PROVIDER_COUNT);
}

int main(int /* argc*/ , const char * /*argv*/ []) {
#if (U_ICU_VERSION_MAJOR_NUM < 49)
  fprintf(stderr, "Warning: ICU %s doesn't support date providers. Need at least 49.\n",  U_ICU_VERSION );
  return 0;
#else
    UErrorCode status = U_ZERO_ERROR;
    int diffs = 0;
    int gbaddiffs =0;
    UDateFormatStyle styles[] = { UDAT_FULL, UDAT_SHORT };
    setup(status);
    if(U_FAILURE(status)) return 1;

    int expected = PROVIDER_COUNT;

    for(uint32_t s=0;s<sizeof(styles)/sizeof(styles[0]);s++) {
      for(int l=0;l<LOCALE_COUNT;l++) {
        printf("\n");
        char16_t oldChars[200];
        int32_t oldLen = -1;
        for(int v=0;v<=expected;v++) {
          
          // Construct the locale ID
          char locID[200];
          strcpy(locID, locale[l]);
          if((v!=expected)) { // -1 = no version
            strcat(locID, "@sp=icu");
            strcat(locID, provider_version[v]);
          }
          
          printf("%18s : ", locID);
          
          UErrorCode subStatus = U_ZERO_ERROR;
          char16_t outchars[200];
          
          UDateFormat *dat = udat_open(styles[s],styles[s], locID, nullptr, -1, nullptr, 0, &subStatus);
          
          if(U_FAILURE(subStatus)) {
            printf("ERR: %s\n", u_errorName(subStatus));
            continue;
          }
          
          int32_t len = udat_format(dat, stuff, outchars, 200, nullptr, &subStatus); 
          
          //printf("\n");
          char utf8[200];
          u_strToUTF8(utf8, 200, nullptr, outchars, len, &subStatus);
          if(oldLen!=len || memcmp(outchars,oldChars,len*sizeof(outchars[0]))) {
            putchar ('!');
            diffs++;
          } else {
            putchar ('=');
          }
          printf(" %s ", utf8); 
          
          // for(int i=0;i<len;i++) {
          //   if((i<oldLen)&&(outchars[i]!=oldChars[i])) {
          //       diffs++;
          //       printf("*", oldChars[i]);
          //     } else {
          //       printf(" ");
          //     }
          //   //                printf("U+%04X", (outchars[i]));
          // }
          putchar('\n');
          udat_close(dat);
          
          oldLen = len;
          memcpy(oldChars, outchars, len*sizeof(oldChars[0]));
        }
      }
    }
      
    if(diffs==0) {
      printf("ERROR: 0 differences found between platforms.. are the platforms installed? Try 'icuinfo -L'\n");
      return 1;
    } else {
      printf("%d differences found among provider versions!\n", diffs);
    }

    // if(gbaddiffs>0) {
    //   printf("ERROR: %d diffs found between a collator and it's reopened (from shortstring) variant.\n", gbaddiffs);
    //   return 2;
    // } else {
    //   printf("Collator and reopened (shortstring) are OK.\n");
    // }

    printf("Success!\n");
    
    return 0;
#endif
}
