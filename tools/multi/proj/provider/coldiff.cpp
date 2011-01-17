/*
*******************************************************************************
*
*   Copyright (C) 2009-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include <unicode/coll.h>
#include <unicode/uclean.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>


/* String to use. */
const UChar stuff[] = { 0x30BB, 0x0d4c, 0x53, 0x74, 0x75, 0x66, 0x66, 0x00 }; /* Stuff */

#include "provider_version.h"


#define LOCALE_COUNT 4
const char *locale[LOCALE_COUNT] = { "fi", "en_US", "ja", "ml" }; /* List of locales to test */

/**
 * Set up ICU, print # of available collators
 */
void setup(UErrorCode &status) {
    u_init(&status);
  
    fprintf(stderr, "ICU %s init: %s\n", U_ICU_VERSION, u_errorName(status));

    int32_t count;
    StringEnumeration *se = Collator::getAvailableLocales();
    count = se->count(status);
    fprintf(stderr, "# Collators now available: %d,\t%s - %d providers expected.\n", count, u_errorName(status), PROVIDER_COUNT);
}

int main(int /* argc*/ , const char * /*argv*/ []) {
    UErrorCode status = U_ZERO_ERROR;
    int diffs = 0;
    setup(status);
    if(U_FAILURE(status)) return 1;

    int expected = PROVIDER_COUNT;

    for(int l=0;l<LOCALE_COUNT;l++) {
        printf("\n");
        uint8_t oldBytes[200];
        int32_t oldLen = -1;
        for(int v=0;v<=expected;v++) {

            // Construct the locale ID
            char locID[200];
            strcpy(locID, locale[l]);
            if((v!=expected)) { // -1 = no version
                strcat(locID, "@sp=icu");
                strcat(locID, provider_version[v]);
            }
            
            printf("%28s : ", locID);
            
            UErrorCode subStatus = U_ZERO_ERROR;
            uint8_t bytes[200];
#define USE_CXX 0

#if USE_CXX
            Collator *col = Collator::createInstance(Locale(locID),subStatus);
            if(U_FAILURE(subStatus)) {
                printf("ERR: %s\n", u_errorName(subStatus));
                continue;
            }
            int32_t len = col->getSortKey(stuff, -1, bytes, 200);
#else
#if 1
            char xbuf2[200];
            strcpy(xbuf2,"X/");
            strcat(xbuf2,locID);
            strcat(xbuf2,"/");
            printf(" -> %s\n", xbuf2);
            UCollator *col = ucol_openFromShortString(xbuf2, FALSE,NULL, &subStatus);
#else
            UCollator *col = ucol_open(locID, &subStatus);
#endif
            if(U_FAILURE(subStatus)) {
                printf("ERR: %s\n", u_errorName(subStatus));
                continue;
            }
            int32_t len = ucol_getSortKey(col, stuff, -1, bytes, 200);
#endif

            for(int i=0;i<len;i++) {
	      if(i<oldLen&&bytes[i]!=oldBytes[i]) {
                  diffs++;
                  printf("*");
                } else {
                  printf(" ");
                }
                printf("%02X", (0xFF&bytes[i]));
            }
            printf("\n");
#if USE_CXX
            delete col;
#else
            ucol_close(col);
#endif

            oldLen = len;
            memcpy(oldBytes, bytes, len);
        }
    }

    if(diffs==0) {
      printf("ERROR: 0 differences found between platforms.. are the platforms installed? Try 'icuinfo -L'\n");
    } else {
      printf("%d differences found among provider versions. Success!\n", diffs);
    }
    
    return 0;
}
