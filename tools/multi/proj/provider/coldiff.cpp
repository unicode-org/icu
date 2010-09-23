/*
*******************************************************************************
*
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include <unicode/coll.h>
#include <unicode/uclean.h>
#include <stdio.h>


/* String to use. */
const UChar stuff[] = { 0x30BB, 0x0d4c, 0x53, 0x74, 0x75, 0x66, 0x66, 0x00 }; /* Stuff */

#define VERS_COUNT 3
const char *vers[VERS_COUNT] = { NULL, "42", "38" }; /* List of ICU versions to test */

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
    fprintf(stderr, "# Collators now available: %d,\t%s\n", count, u_errorName(status));
}

int main(int argc, const char *argv[]) {
    UErrorCode status = U_ZERO_ERROR;
    setup(status);
    if(U_FAILURE(status)) return 1;
    for(int l=0;l<LOCALE_COUNT;l++) {
        printf("\n");
        uint8_t oldBytes[200];
        int32_t oldLen = -1;
        for(int v=0;v<VERS_COUNT;v++) {

            // Construct the locale ID
            char locID[200];
            strcpy(locID, locale[l]);
            if(vers[v]!=NULL) { // NULL = no version
                strcat(locID, "@provider=ICU");
                strcat(locID, vers[v]);
            }
            
            printf("%28s : ", locID);
            
            UErrorCode subStatus = U_ZERO_ERROR;
            uint8_t bytes[200];

            Collator *col = Collator::createInstance(Locale(locID),subStatus);
            if(U_FAILURE(subStatus)) {
                printf("ERR: %s\n", u_errorName(subStatus));
                continue;
            }
            int32_t len = col->getSortKey(stuff, -1, bytes, 200);

            for(int i=0;i<len;i++) {
                if(v>0&&i<oldLen&&bytes[i]!=oldBytes[i]) {
                    printf("*");
                } else {
                    printf(" ");
                }
                printf("%02X", (0xFF&bytes[i]));
            }
            printf("\n");

            delete col;

            oldLen = len;
            memcpy(oldBytes, bytes, len);
        }
    }
    return 0;
}
