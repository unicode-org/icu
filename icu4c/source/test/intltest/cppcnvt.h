/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "intltest.h"

/**
 * Test for APIs of CPPUnicodeConverter
 **/
class ConvertTest: public IntlTest {
public:
    ConvertTest() {};
    ~ConvertTest() {};
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /**
     * Test everything
     **/
    void TestConvert(void);
    void TestAmbiguous(void);
};



