#ifndef TBLPRINT_H
#define TBLPRINT_H

#include "unicode/utypes.h"
#include "genuca.h"

char *formatElementString(uint32_t CE, char *buffer);
void printExp(uint32_t CE, uint32_t oldCE, char* primb, char* secb, char *terb, UBool *printedCont);
void printOutTable(UCATableHeader *myData, UErrorCode *status);

#endif