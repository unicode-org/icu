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

#ifndef _NUMBERFORMATREGRESSIONTEST_
#define _NUMBERFORMATREGRESSIONTEST_
 
#include "utypes.h"
#include "numfmt.h"

#include "intltest.h"

#include "numfmt.h"

class DecimalFormat;

/** 
 * Performs regression test for MessageFormat
 **/
class NumberFormatRegressionTest: public IntlTest {    
    
    // IntlTest override
    void runIndexedTest( int32_t index, bool_t exec, char* &name, char* par );
public:

    void Test4075713();
    void Test4074620() ;
    void Test4088161 ();
    void Test4087245 ();
    void Test4087535 ();
    void Test4088503 ();
    void Test4066646 () ;
    float assignFloatValue(float returnfloat);
    void Test4059870() ;
    void Test4083018 ();
    void Test4071492 ();
    void Test4086575() ;
    void Test4068693();
    void Test4069754();
    void Test4087251 ();
    void Test4090489 ();
    void Test4090504 ();
    void Test4095713 ();
    void Test4092561 ();
    void Test4092480 ();
    void Test4087244 () ;
    void Test4070798 () ;
    void Test4071005 () ;
    void Test4071014 () ;
    void Test4071859 () ;
    void Test4093610();
    void roundingTest(DecimalFormat *df, double x, UnicodeString& expected);
    void Test4098741();
    void Test4074454();
    void Test4099404();
    void Test4101481();
    void Test4052223();
    void Test4061302();
    void Test4062486();
    void Test4108738();
    void Test4106658();
    void Test4106662();
    void Test4114639();
    void Test4106664();
    void Test4106667();
    void Test4110936();
    void Test4122840();
    void Test4125885();
    void Test4134034() ;
    void Test4134300() ;
    void Test4140009() ;
    void Test4141750() ;
    void Test4145457() ;
    void Test4147295() ;
    void Test4147706() ;

    void Test4162198() ;
    void Test4162852() ;

    void Test4167494();
    void Test4170798();
    void Test4176114();
    void Test4179818();
    void Test4212072();
    void Test4216742();
    void Test4217661();
    void Test4161100();
    void Test4243011();
    void Test4243108();

protected:
    bool_t failure(UErrorCode status, const UnicodeString& msg);
};

class MyNumberFormatTest : public NumberFormat 
{
public:

    //public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) 
  //{ return new StringBuffer(""); }
  //public StringBuffer format(long number,StringBuffer toAppendTo, FieldPosition pos) 
  //{ return new StringBuffer(""); }
  
  virtual UnicodeString& format(    double            number, 
                    UnicodeString&        toAppendTo, 
                    FieldPosition&        pos) const
    {
      toAppendTo = "";
      return toAppendTo;
    }
  
  
  //public Number parse(String text, ParsePosition parsePosition) 
  //{ return new Integer(0); }
  
  virtual void parse(    const UnicodeString&    text, 
            Formattable&            result, 
            ParsePosition&            parsePosition) const
    {
      result.setLong(0L);
    }
  
  virtual Format* clone() const 
    { return NULL; }
  
  virtual UnicodeString& format(int32_t, 
                UnicodeString& foo, 
                FieldPosition&) const
    { return foo.remove(); }
};

#endif // _NUMBERFORMATREGRESSIONTEST_
//eof
