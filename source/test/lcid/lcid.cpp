//**********************************************************************
//* Copyright (C) 2000, International Business Machines Corporation
//* and others.  All Rights Reserved.
//**********************************************************************
// lcid.cpp - Test for establishing conformance of data in resource bundles to 
// lcid <-> POSIX mapping
//  Created by: Vladimir Weinstein
//  Date:       08/09/2000

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "locmap.h"
#include "cstring.h"
#include "unicode/resbund.h"

static void
testLCID(const char *localeName);

static void
testLCID(const char *localeName,
         const UnicodeString &posixName,
         uint32_t *errors,
         uint32_t *warnings)
{
    UErrorCode status = U_ZERO_ERROR;
    uint32_t lcid;
    uint32_t expectedLCID;
    char lcidStringC[1024];
    char cLocaleName[256] =  {'\0'};
    
    u_UCharsToChars(posixName.getUChars(),cLocaleName,posixName.length());
    ResourceBundle posixLocale((char *)0, Locale(cLocaleName), status);
    if(status != U_ZERO_ERROR) {
        if(U_SUCCESS(status)) {
            printf("ERROR: Locale %-5s not installed, and it should be!\n", localeName);
        } else {
            printf("%%%%%%% Unexpected error %d %%%%%%%", u_errorName(status));
        }
        (*errors)++;
        return;
    }

    UnicodeString lcidString(posixLocale.getStringEx("LocaleID", status));

    if (U_FAILURE(status)) {
        printf("ERROR:   %s does not have a LocaleID (%s)\n", localeName, u_errorName(status));
        (*errors)++;
        return;
    }

    lcidString.extract(0, lcidString.length(), lcidStringC, "");
    lcidStringC[lcidString.length()] = '\0';
    expectedLCID = uprv_strtoul(lcidStringC, NULL, 16);

    lcid = T_convertToLCID(localeName, &status);
    if (U_FAILURE(status)) {
        if (expectedLCID == 0) {
            printf("INFO:    %-5s does not have any LCID mapping\n", localeName);
        }
        else {
            printf("ERROR:   %-5s does not have an LCID mapping to 0x%.4X\n", localeName, expectedLCID);
            (*errors)++;
        }
        return;
    }

    status = U_ZERO_ERROR;
    uprv_strcpy(lcidStringC, T_convertToPosix(expectedLCID, &status));
    if (U_FAILURE(status)) {
        printf("ERROR:   %.4x does not have a POSIX mapping due to %s\n", expectedLCID, u_errorName(status));
        (*errors)++;
    }

    if(lcid != expectedLCID) {
        printf("ERROR:   %-5s wrongfully has 0x%.4x instead of 0x%.4x for LCID\n", localeName, expectedLCID, lcid);
        (*errors)++;
    }
    if(strcmp(localeName, lcidStringC) != 0) {
        char langName[1024];
        char langLCID[1024];
        uloc_getLanguage(localeName, langName, sizeof(langName), &status);
        uloc_getLanguage(lcidStringC, langLCID, sizeof(langLCID), &status);

        if (expectedLCID == lcid && strcmp(langName, langLCID) == 0) {
            printf("WARNING: %-5s resolves to %s (0x%.4x)\n", localeName, lcidStringC, lcid);
            (*warnings)++;
        }
        else if (expectedLCID == lcid) {
            printf("ERROR:   %-5s has 0x%.4x and the number resolves wrongfully to %s\n", localeName, expectedLCID, lcidStringC);
            (*errors)++;
        }
        else {
            printf("ERROR:   %-5s has 0x%.4x and the number resolves wrongfully to %s. It should be 0x%x.\n", localeName, expectedLCID, lcidStringC, lcid);
            (*errors)++;
        }
    } else {
        //printf("0x%x is from %s and it resolves correctly to %s(0x%x)\n", expectedLCID, localeName, lcidStringC, lcid);
        //printf("%s: %x->%x\n", localeName, expectedLCID, lcid);
    }
}

int main() {
    UErrorCode status = U_ZERO_ERROR;
    ResourceBundle index((char *)0, Locale("index"), status);
    uint32_t errors = 0;
    uint32_t warnings = 0;

    if(U_SUCCESS(status)) {
        ResourceBundle installedLocales = index.get("InstalledLocales", status);
        if(U_SUCCESS(status)) {
            installedLocales.resetIterator();
            while(installedLocales.hasNext()) {
                char localeName[1024];
                UnicodeString posixName = installedLocales.getNextString(status);

                posixName.extract(0, posixName.length(), localeName, "");
                localeName[posixName.length()] = '\0';
                testLCID(localeName, posixName, &errors, &warnings);
            }
        }
        else {
            puts("Error getting the InstalledLocales\n");
            errors++;
        }
    }
    else {
        puts("Error getting the index\n");
        errors++;
    }
    if(errors > 0) {
        printf("There were %d error(s)\n", errors);
    } else {
        printf("There were no errors\n");
    }

    if(warnings > 0) {
        printf("There were %d warning(s)\n", warnings);
    }

//    char temp;
//    scanf("%c", &temp);

    return 0;
}