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
 * Test API and functionality of class Unicode
 **/
class UnicodeTest: public IntlTest {
public:
    UnicodeTest();
    ~UnicodeTest();
    
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par = NULL );

    /** 
     * Test methods toUpperCase() and toLowerCase()
     **/
    void TestUpperLower(void);
    /** 
     * Test methods isLetter() and isDigit() 
     **/
    void TestLetterNumber(void);
    /** 
     * Tests methods isControl() and isPrintable()
     **/
    void TestControlPrint(void);
    /** 
     * Tests methods isJavaIdentifierStart(), isJavaIdentifierPart(),
     * isUnicodeIdentifierStart() and isUnicodeIdentifierPart()
     **/
    void TestIdentifier(void);
    /** 
     * Tests methods isDefined(), isBaseForm(), isSpaceChar() and getCellWidth()
     **/
    void TestMisc(void);
    /** 
     * tests methods getType(), isTitleCase(), and toTitleCase() 
     * as well as characterDirection()
     **/
    void TestUnicodeData(void);

private:
    /**
     * internal utility used by TestUnicodeData
     **/
    int32_t MakeProp(char* str);
    /**
     * internal utility used by TestUnicodeData
     **/
    int32_t MakeDir(char* str);
};

