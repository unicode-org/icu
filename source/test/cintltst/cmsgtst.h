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
* File CMSGTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Creation
*********************************************************************************
*/
/* C API TEST FOR MESSAGE FORMAT */
#ifndef _CMSGFRMTST
#define _CMSGFRMTST

#include "cintltst.h"


/* The function used to test the Message format API*/

    /**
     * Test u_formatMessage() with various test patterns
     **/
    static void MessageFormatTest(void);
    /**
     * Test u_formatMessage() with sample test Patterns 
     **/
    static void TestSampleMessageFormat(void);
    /**
     * Test format and parse sequence and roundtrip
     **/
    static void TestSampleFormatAndParse(void);
    /**
     * Test u_formatMessage() with choice option
     **/
    static void TestMsgFormatChoice(void);
    /**
     * Test u_parseMessage() with various test patterns()
     **/
    static void TestParseMessage(void);
    /**
     * function used to set up various patterns used for testing u_formatMessage()
     **/
    static void InitStrings( void );

#endif
