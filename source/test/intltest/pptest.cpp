/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation, 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#include "pptest.h"

#include "numfmt.h"
#include "decimfmt.h"
 
// *****************************************************************************
// class ParsePositionTest
// *****************************************************************************

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void ParsePositionTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    // if (exec) logln((UnicodeString)"TestSuite ParsePositionTest");
    switch (index) {
        CASE(0, TestParsePosition)
        CASE(1, TestFieldPosition)
        CASE(2, TestFieldPosition_example)
        CASE(3, Test4109023)

        default: name = ""; break;
    }
}

bool_t 
ParsePositionTest::failure(UErrorCode status, const char* msg)
{
    if(U_FAILURE(status)) {
        errln(UnicodeString("FAIL: ") + msg + " failed, error " + u_errorName(status));
        return TRUE;
    }

    return FALSE;
}
 
void ParsePositionTest::TestParsePosition() 
{
    ParsePosition pp1(0);
    if (pp1.getIndex() == 0) {
        logln("PP constructor() tested.");
    }else{
        errln("*** PP getIndex or constructor() result");
    }

    {
        int to = 5;
        ParsePosition pp2( to );
        if (pp2.getIndex() == 5) {
            logln("PP getIndex and constructor(UTextOffset) tested.");
        }else{
            errln("*** PP getIndex or constructor(UTextOffset) result");
        }
        pp2.setIndex( 3 );
        if (pp2.getIndex() == 3) {
            logln("PP setIndex tested.");
        }else{
            errln("*** PP getIndex or setIndex result");
        }
    }

    ParsePosition pp2(3), pp3(5);
    //pp2 = new ParsePosition( 3 );
    //pp3 = new ParsePosition( 5 );
    ParsePosition pp4(5);
    if ( pp2 != pp3) {
        logln("PP not equals tested.");
    }else{
        errln("*** PP not equals fails");
    }
    if (pp3 == pp4) {
        logln("PP equals tested.");
    }else{
        errln(UnicodeString("*** PP equals fails (") + pp3.getIndex() + " != " + pp4.getIndex() + ")");
    }

    ParsePosition pp5;
    pp5 = pp4;
    if (pp4 == pp5) {
        logln("PP operator= tested.");
    }else{
        errln("*** PP operator= operator== or operator != result");
    }

}

void ParsePositionTest::TestFieldPosition() 
{
    FieldPosition fp( 7 );

    if (fp.getField() == 7) {
        logln("FP constructor(int) and getField tested.");
    }else{
        errln("*** FP constructor(int) or getField");
    }

    FieldPosition fph( 3 );
    if ( fph.getField() != 3) 
        errln("*** FP getField or heap constr.");

    bool_t err1 = FALSE;
    bool_t err2 = FALSE;
    bool_t err3 = FALSE;
//        for (long i = -50; i < 50; i++ ) {
//            fp.setField( i+8 );
//            fp.setBeginIndex( i+6 );
//            fp.setEndIndex( i+7 );
//            if (fp.getField() != i+8)  err1 = TRUE;
//            if (fp.getBeginIndex() != i+6) err2 = TRUE;
//            if (fp.getEndIndex() != i+7) err3 = TRUE;
//        }
    if (!err1) {
        logln("FP setField and getField tested.");
    }else{
        errln("*** FP setField or getField");
    }
    if (!err2) {
        logln("FP setBeginIndex and getBeginIndex tested.");
    }else{
        errln("*** FP setBeginIndex or getBeginIndex");
    }
    if (!err3) {
        logln("FP setEndIndex and getEndIndex tested.");
    }else{
        errln("*** FP setEndIndex or getEndIndex");
    }

    logln("");
}

void ParsePositionTest::TestFieldPosition_example() 
{
    //***** no error detection yet !!!!!!!
    //***** this test is for compiler checks and visual verification only.
    double doubleNum[] = { 
        123456789.0, 
        -12345678.9, 
        1234567.89, 
        -123456.789,
        12345.6789, 
        -1234.56789, 
        123.456789, 
        -12.3456789, 
        1.23456789};
    int dNumSize = 9;

    UErrorCode status = U_ZERO_ERROR;
    NumberFormat *nf = NumberFormat::createInstance(status);
    failure(status, "NumberFormat::createInstance");

    if(nf->getDynamicClassID() != DecimalFormat::getStaticClassID()) {
        errln("NumberFormat::createInstance returned unexpected class type");
        return;
    }
    DecimalFormat *fmt = (DecimalFormat*) nf;
    fmt->setDecimalSeparatorAlwaysShown(TRUE);

    const int tempLen = 20;
    UnicodeString temp;

    for (int i=0; i < dNumSize; i++) {
        temp.remove();
        //temp = new StringBuffer(); // Get new buffer

        FieldPosition pos(NumberFormat::INTEGER_FIELD);
        UnicodeString buf;// = new StringBuffer();
        //char fmtText[tempLen];
        //ToCharString(fmt->format(doubleNum[i], buf, pos), fmtText);
        UnicodeString res;
        res = fmt->format(doubleNum[i], buf, pos);
        int tempOffset = (tempLen <= (tempLen - pos.getEndIndex())) ?
            tempLen : (tempLen - pos.getEndIndex());
        for (int j=0; j<tempOffset; j++) 
            temp += UnicodeString("="/*'='*/); // initialize
        //cout << temp << fmtText   << endl;
        logln("FP " + temp + res);
    }

    logln("");
    delete nf;
}

/* @bug 4109023
 * Need to override ParsePosition.equals and FieldPosition.equals.
 */
void ParsePositionTest::Test4109023()
{
    ParsePosition p(3);
    ParsePosition p2(3);
    if (p != p2)
        errln("Error : ParsePosition.equals() failed");
    FieldPosition fp(2);
    FieldPosition fp2(2);
    if (fp != fp2)
        errln("Error : FieldPosition.equals() failed");
}
