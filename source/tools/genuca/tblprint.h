/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genuca.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
*
*   Change history:
*
*   02/08/2001  Vladimir Weinstein      Created this program
*   02/23/2001  grhoten                 Made it into a tool
*/

#ifndef TBLPRINT_H
#define TBLPRINT_H

#include "unicode/utypes.h"
#include "genuca.h"

char *formatElementString(uint32_t CE, char *buffer);
void printExp(uint32_t CE, uint32_t oldCE, char* primb, char* secb, char *terb, UBool *printedCont);
void printOutTable(UCATableHeader *myData, UErrorCode *status);

#endif
