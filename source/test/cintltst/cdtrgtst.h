/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CDTRGTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
*/
/* REGRESSION TEST FOR DATE FORMAT */
#ifndef _CDTFRRGSTST
#define _CDTFRRGSTST

#include "cintltst.h"

    /**
     * DateFormat Regresstion tests
     **/

    void Test4029195(void); 
    void Test4056591(void); 
    void Test4059917(void);
    void Test4060212(void); 
    void Test4061287(void); 
    void Test4073003(void); 
    void Test4162071(void); 

    /**
     * test subroutine
     **/
    void aux917(UDateFormat *fmt, UChar* str );

    /**
     * test subroutine used by the testing functions
     **/
    UChar* myFormatit(UDateFormat* datdef, UDate d1);

#endif
