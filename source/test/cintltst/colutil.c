/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "ucol_imp.h"
int TestBufferSize(void); /* keep gcc happy */

int TestBufferSize(void)
{
    return (U_COL_SAFECLONE_BUFFERSIZE < sizeof(UCollator));
}
