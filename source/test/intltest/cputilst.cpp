/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
#include "unicode/utypes.h"
#include "cputilst.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


/*Debugging functions, DO NOT USE*/
;

void  printUChar(const UChar *uniString)
{
    uint32_t i=0;
    
    putchar('{');
    while (uniString[i]) 
    {
        printf( "0x%.4X\t", (UChar)uniString[i++]);
        if (!(i%8)) putchar('\n');
    }
    putchar('}');
    printf("(%d)", i);
    
}

void  printChar(const char *charString)
{
    uint32_t i=0;

    putchar('{');
    while (charString[i]) 

    {
        printf( "0x%.2X\t", (unsigned char)charString[i++]);
        if (!(i%8)) putchar('\n');
    }
    putchar('}');
    printf("(%d)", i);

}



