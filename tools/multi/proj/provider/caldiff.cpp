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

int err=0;
#include "provider_version.h"


#define LOCALE_COUNT 4
const char *locale[LOCALE_COUNT] = { "es_GU", "fr_AD", "et_AM", "en_IE" }; /* List of locales to test */

/**
 * Set up ICU, print # of available collators
 */
void setup(UErrorCode &status) {
    u_init(&status);
  
    fprintf(stderr, "ICU %s init: %s\n", U_ICU_VERSION, u_errorName(status));

     int32_t count;
     const Locale *se = Calendar::getAvailableLocales(count);
     fprintf(stderr, "# Calendars now available: %d,\t%s - %d providers expected.\n", count, u_errorName(status), (int32_t)PROVIDER_COUNT);
}

int main(int /* argc*/ , const char * /*argv*/ []) {
#if 0
  // fprintf(stderr, "Warning: ICU %s doesn't support date providers. Need at least 49.\n",  U_ICU_VERSION );
  // return 0;
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
        char oldChars[200];
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
          char outchars[200];
          
          LocalPointer<Calendar> cal(Calendar::createInstance(Locale(locID), subStatus));
          
          if(U_FAILURE(subStatus)) {
            printf("ERR: %s\n", u_errorName(subStatus));
            err++;
            continue;
          }
          
          // int32_t len = udat_format(dat, stuff, outchars, 200, NULL, &subStatus); 
          
          // //printf("\n");
          //char utf8[200];
          // u_strToUTF8(utf8, 200, NULL, outchars, len, &subStatus);
          
          sprintf(outchars, " cal: mindays=%d firstday=%d ", (int)cal->getMinimalDaysInFirstWeek(), (int)cal->getFirstDayOfWeek());
          int32_t len = strlen(outchars);

          if(oldLen!=len || memcmp(outchars,oldChars,len*sizeof(outchars[0]))) {
            if(v==0) {
              putchar(' ');
            } else {
              putchar ('!');
              diffs++;
            }
          } else {
            putchar ('=');
          }
          printf(" %s ", outchars); 
          
          for(int i=0;i<len;i++) {
               if((i<oldLen)&&(outchars[i]!=oldChars[i])) {
                        diffs++;
                        printf("*", oldChars[i]);
               } else {
                 printf(" ");
               }
          //   //                printf("U+%04X", (outchars[i]));
          }
          putchar('\n');
          
          oldLen = len;
          memcpy(oldChars, outchars, len*sizeof(oldChars[0]));
        }
      }
    }
      
    if(diffs==0) {
      printf("ERROR: 0 differences found between platforms.. are the platforms installed? Try 'icuinfo -L'\n");
      return 1;
    } else {
      printf("%d differences found among provider versions! Provider is working!\n", diffs);
    }

    // if(gbaddiffs>0) {
    //   printf("ERROR: %d diffs found between a collator and it's reopened (from shortstring) variant.\n", gbaddiffs);
    //   return 2;
    // } else {
    //   printf("Collator and reopened (shortstring) are OK.\n");
    // }

    if(err) {
      printf("%d errors - FAIL!\n", err);
      return 1;
    }

    printf("Success!\n");
    
    return 0;
#endif
}
