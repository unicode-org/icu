/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
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
