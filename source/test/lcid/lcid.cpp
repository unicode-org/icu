//**********************************************************************
//* Copyright (C) 2000, International Business Machines Corporation
//* and others.  All Rights Reserved.
//**********************************************************************
// lcid.cpp - Test for establishing conformance of data in resource bundles to 
// lcid <-> POSIX mapping
//  Created by: Vladimir Weinstein
//  Date:       08/09/2000

#include <stdio.h>
#include "unicode/utypes.h"
#include "locmap.h"
#include "cstring.h"
#include "unicode/resbund.h"

int main() {
    UErrorCode status = U_ZERO_ERROR;
    ResourceBundle index((char *)0, Locale("index"), status);
    uint32_t errors = 0;

    if(U_SUCCESS(status)) {
        ResourceBundle installedLocales = index.get("InstalledLocales", status);
        if(U_SUCCESS(status)) {
            installedLocales.resetIterator();
            while(installedLocales.hasNext()) {
                char localeName[1024];
                uint32_t lcid;
                UnicodeString posixName = installedLocales.getNextString(status);
                posixName.extract(0, posixName.length(), localeName, "");
                localeName[posixName.length()] = '\0';
                ResourceBundle posixLocale((char *)0, Locale(posixName), status);
                if(status == U_ZERO_ERROR) {
                    UnicodeString lcidString = posixLocale.getStringEx("LocaleID", status);
                    char lcidStringC[1024];
                    lcidString.extract(0, lcidString.length(), lcidStringC, "");
                    lcidStringC[lcidString.length()] = '\0';
                    uint32_t expectedLCID = uprv_strtoul(lcidStringC, NULL, 16);
                    lcid = IGlobalLocales::convertToLCID(localeName);
                    uprv_strcpy(lcidStringC, IGlobalLocales::convertToPosix(expectedLCID));
                    if(strcmp(localeName, lcidStringC) == 0) {
                        printf("0x%x is from %s and it resolves correctly to %s(0x%x)\n", expectedLCID, localeName, lcidStringC, lcid);
                        //printf("%s: %x->%x\n", localeName, expectedLCID, lcid);
                    } else {
                        printf("ERROR: 0x%x is from %s and it resolves wrongfully to %s, it shoud have (0x%x)\n", expectedLCID, localeName, lcidStringC, lcid);
                        //printf("Name mismatch: %s vs. %s: %x->%x\n", localeName, lcidStringC, expectedLCID, lcid);
                        errors++;
                    }
                    if(lcid != expectedLCID) {
                        printf("ERROR: Locale %s wrongfully has 0x%x instead of 0x%x for LCID\n", localeName, expectedLCID, lcid);
                        //printf("LCID mismatch: %s: %x->%x\n", localeName, expectedLCID, lcid);
                        errors++;
                    }
                } else if(U_SUCCESS(status)) {
                    printf("ERROR: Locale %s not installed, and it should be!\n", localeName);
                    errors++;
                } else {
                    printf("%%%%%%% Unexpected error %d %%%%%%%", status);
                }

            }
        }
    }
    if(errors > 0) {
        printf("There were %d error(s)\n", errors);
    } else {
        printf("There were no errors\n");
    }

    return 0;
}