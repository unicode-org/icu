
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation, 1998
* Copyright (C) 1999 Alan Liu and others. All rights reserved. 
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#ifndef _DATEFORMATREGRESSIONTEST_
#define _DATEFORMATREGRESSIONTEST_
 
#include "utypes.h"
#include "caltztst.h"

class SimpleDateFormat;

/** 
 * Performs regression test for DateFormat
 **/
class DateFormatRegressionTest: public CalendarTimeZoneTest {
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

    void Test4029195(void);
    void Test4052408(void);
    void Test4056591(void);
    void Test4059917(void);
    void aux917( SimpleDateFormat *fmt, UnicodeString& str );
    void Test4060212(void);
    void Test4061287(void);
    void Test4065240(void);
    void Test4071441(void);
    void Test4073003(void);
    void Test4089106(void);
    void Test4100302(void);
    void Test4101483(void);
    void Test4103340(void);
    void Test4103341(void);
    void Test4104136(void);
    void Test4104522(void);
    void Test4106807(void);
    void Test4108407(void); 
    void Test4134203(void);
    void Test4151631(void);
    void Test4151706(void);
    void Test4162071(void);
    void Test4182066(void);
    void Test4210209(void);
 };
 
#endif // _DATEFORMATREGRESSIONTEST_
//eof
