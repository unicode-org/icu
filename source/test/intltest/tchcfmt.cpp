
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


#include "unicode/utypes.h"

#include "intltest.h"
#include "tchcfmt.h"

#include "unicode/msgfmt.h"
#include "unicode/choicfmt.h"

#include <float.h>

// tests have obvious memory leaks!

static UBool chkstatus( UErrorCode &status, char* msg = NULL )
{
    UBool ok = U_SUCCESS(status);
    if (!ok) it_errln( msg );
    return ok;
}

void
TestChoiceFormat::TestSimpleExample( void )
{
    double limits[] = {1,2,3,4,5,6,7};
    UnicodeString monthNames[] = {"Sun","Mon","Tue","Wed","Thur","Fri","Sat"};
    ChoiceFormat* form = new ChoiceFormat(limits, monthNames, 7);
    ParsePosition parse_pos;
    // TODO Fix this ParsePosition stuff...
    UnicodeString str;
    UnicodeString res1, res2;
    UErrorCode status;
    FieldPosition fpos(0);
    Formattable f;
    //for (double i = 0.0; i <= 8.0; ++i) {
    for (int32_t ix = 0; ix <= 8; ++ix) {
        double i = ix; //nos
        status = U_ZERO_ERROR;
        fpos = 0;
        str = "";
        res1 = form->format(i, str, fpos, status );
        if (!chkstatus( status, "***  test_simple_example format" )) {
            delete form;
            return;
        }
        //form->parse(res1, f, parse_pos);
        res2 = " ??? ";
        it_out << ix << " -> " << res1 << " -> " << res2 << endl;
    }
    //Testing ==operator
    const double filelimits[] = {0,1,2};
    const UnicodeString filepart[] = {"are no files","is one file","are {2} files"};
    ChoiceFormat* formnew=new ChoiceFormat(filelimits, filepart, 3);
    ChoiceFormat* formequal=new ChoiceFormat(limits, monthNames, 7);
    if(*formnew == *form){
        errln("ERROR: ==operator failed\n");
    }
    if(!(*form == *formequal)){
        errln("ERROR: ==operator failed\n");
    }
    delete formequal; 
    
    //Testing adoptChoices() 
    formnew->adoptChoices(limits, monthNames, 7);
    if(!(*formnew == *form)){
        errln("ERROR: ==Operator or adoptChoices failed\n");
    }
      
    //Testing getLimits()
    double *gotLimits=0;
    int32_t count=0;
    gotLimits=(double*)form->getLimits(count);
    if(count != 7){
        errln("getLimits didn't update the count correctly\n");
    }
    for(ix=0; ix<count; ix++){
        if(gotLimits[ix] != limits[ix]){
            errln((UnicodeString)"getLimits didn't get the limits correctly.  Expected " + limits[ix] + " Got " + gotLimits[ix]);
        }
    }
    //Testing getFormat()
    count=0;
    UnicodeString *gotFormats=0;
    gotFormats=(UnicodeString*)form->getFormats(count);
    if(count != 7){
        errln("getFormats didn't update the count correctly\n");
    }
    for(ix=0; ix<count; ix++){
        if(gotFormats[ix] != monthNames[ix]){
            errln((UnicodeString)"getFormats didn't get the Formats correctly.  Expected " + monthNames[ix] + " Got " + gotFormats[ix]);
        }
    }
    
   
    delete form;
   
}

void
TestChoiceFormat::TestComplexExample( void )
{
    UErrorCode status = U_ZERO_ERROR;
    const double filelimits[] = {0,1,2};
    const UnicodeString filepart[] = {"are no files","is one file","are {2} files"};

    ChoiceFormat* fileform = new ChoiceFormat( filelimits, filepart, 3);

    if (!fileform) { 
        it_errln("***  test_complex_example fileform"); 
        return; 
    }

    Format* filenumform = NumberFormat::createInstance( status );
    if (!filenumform) { 
        it_errln("***  test_complex_example filenumform"); 
        delete fileform;
        return; 
    }
    if (!chkstatus( status, "***  test_simple_example filenumform" )) {
        delete fileform;
        delete filenumform;
        return;
    }

    //const Format* testFormats[] = { fileform, NULL, filenumform };
    //pattform->setFormats( testFormats, 3 );

    MessageFormat* pattform = new MessageFormat("There {0} on {1}", status );
    if (!pattform) { 
        it_errln("***  test_complex_example pattform"); 
        delete fileform;
        delete filenumform;
        return; 
    }
    if (!chkstatus( status, "***  test_complex_example pattform" )) {
        delete fileform;
        delete filenumform;
        delete pattform;
        return;
    }

    pattform->setFormat( 0, *fileform );
    pattform->setFormat( 2, *filenumform );


    Formattable testArgs[] = {(int32_t)0, "Disk_A", (int32_t)0};
    UnicodeString str;
    UnicodeString res1, res2;
    pattform->toPattern( res1 );
    it_out << "MessageFormat toPattern: " << res1 << endl;
    fileform->toPattern( res1 );
    it_out << "ChoiceFormat toPattern: " << res1 << endl;
    if (res1 == "0.0#are no files|1.0#is one file|2.0#are {2} files") {
        it_out << "toPattern tested!" << endl;
    }else{
        it_errln("***  ChoiceFormat to Pattern result!");
    }

    FieldPosition fpos(0);

    UnicodeString checkstr[] = { 
        "There are no files on Disk_A",
        "There is one file on Disk_A",
        "There are 2 files on Disk_A",
        "There are 3 files on Disk_A"
    };

    if (status != U_ZERO_ERROR) return;

    int32_t i;
    for (i = 0; i < 4; ++i) {
        str = "";
        status = U_ZERO_ERROR;
        testArgs[0] = Formattable((int32_t)i);
        testArgs[2] = testArgs[0];
        res2 = pattform->format(testArgs, 3, str, fpos, status );
        if (!chkstatus( status, "***  test_complex_example format" )) {
            delete fileform;
            delete filenumform;
            delete pattform;
            return;
        }
        it_out << i << " -> " << res2 << endl;
        if (res2 != checkstr[i]) {
            it_errln("***  test_complex_example res string");
            it_out << "*** " << i << " -> '" << res2 << "' unlike '" << checkstr[i] << "' ! " << endl;
        }
    }
    it_out << endl;

    it_out << "------ additional testing in complex test ------" << endl << endl;
    //
    int32_t retCount;
    const double* retLimits = fileform->getLimits( retCount );
    if ((retCount == 3) && (retLimits)
    && (retLimits[0] == 0.0)
    && (retLimits[1] == 1.0)
    && (retLimits[2] == 2.0)) {
        it_out << "getLimits tested!" << endl;
    }else{
        it_errln("***  getLimits unexpected result!");
    }

    const UnicodeString* retFormats = fileform->getFormats( retCount );
    if ((retCount == 3) && (retFormats)
    && (retFormats[0] == "are no files") 
    && (retFormats[1] == "is one file")
    && (retFormats[2] == "are {2} files")) {
        it_out << "getFormats tested!" << endl;
    }else{
        it_errln("***  getFormats unexpected result!");
    }

    UnicodeString checkstr2[] = { 
        "There is no folder on Disk_A",
        "There is one folder on Disk_A",
        "There are many folders on Disk_A",
        "There are many folders on Disk_A"
    };

    fileform->applyPattern("0#is no folder|1#is one folder|2#are many folders", status );
    if (status == U_ZERO_ERROR) it_out << "status applyPattern OK!" << endl;
    if (!chkstatus( status, "***  test_complex_example pattform" )) {
        delete fileform;
        delete filenumform;
        delete pattform;
        return;
    }
    pattform->setFormat( 0, *fileform );
    fpos = 0;
    for (i = 0; i < 4; ++i) {
        str = "";
        status = U_ZERO_ERROR;
        testArgs[0] = Formattable((int32_t)i);
        testArgs[2] = testArgs[0];
        res2 = pattform->format(testArgs, 3, str, fpos, status );
        if (!chkstatus( status, "***  test_complex_example format 2" )) {
            delete fileform;
            delete filenumform;
            delete pattform;
            return;
        }
        it_out << i << " -> " << res2 << endl;
        if (res2 != checkstr2[i]) {
            it_errln("***  test_complex_example res string");
            it_out << "*** " << i << " -> '" << res2 << "' unlike '" << checkstr2[i] << "' ! " << endl;
        }
    }

    double nd = ChoiceFormat::nextDouble( 1.0 );
    double pd = ChoiceFormat::previousDouble( 1.0 );
    if ((ChoiceFormat::nextDouble( 1.0, TRUE ) == nd)
     && (ChoiceFormat::nextDouble( 1.0, FALSE ) == pd)) {
        it_out << "nextDouble(x, TRUE) and nextDouble(x, FALSE) tested" << endl;
    }else{
        it_errln("***  nextDouble( x, BOOL )");
    }
    if ((nd > 1.0) && (nd < 1.0001)) {
        it_out << "nextDouble(x) tested" << endl;
    }else{
        it_errln("***  nextDouble");
    }
    if ((pd < 1.0) && (pd > 0.9999)) {
        it_out << "prevDouble(x) tested" << endl;
    }else{
        it_errln("***  prevDouble");
    }


    const double limits_A[] = {1,2,3,4,5,6,7};
    const UnicodeString monthNames_A[] = {"Sun","Mon","Tue","Wed","Thur","Fri","Sat"};
    ChoiceFormat* form_A = new ChoiceFormat(limits_A, monthNames_A, 7);
    ChoiceFormat* form_A2 = new ChoiceFormat(limits_A, monthNames_A, 7);
    const double limits_B[] = {1,2,3,4,5,6,7};
    const UnicodeString monthNames_B[] = {"Sun","Mon","Tue","Wed","Thur","Fri","Sat_BBB"};
    ChoiceFormat* form_B = new ChoiceFormat(limits_B, monthNames_B, 7);
    if (!form_A || !form_B || !form_A2) {
        it_errln("***  test-choiceFormat not allocatable!");
    }else{
        if (*form_A == *form_A2) {
            it_out << "operator== tested." << endl;
        }else{
            it_errln("***  operator==");
        }

        if (*form_A != *form_B) {
            it_out << "operator!= tested." << endl;
        }else{
            it_errln("***  operator!=");
        }

        ChoiceFormat* form_A3 = (ChoiceFormat*) form_A->clone();
        if (!form_A3) {
            it_errln("***  ChoiceFormat->clone is nil.");
        }else{
            if ((*form_A3 == *form_A) && (*form_A3 != *form_B)) {
                it_out << "method clone tested." << endl;
            }else{
                it_errln("***  ChoiceFormat clone or operator==, or operator!= .");
            }
        }

        ChoiceFormat form_Assigned( *form_A );
        UBool ok = (form_Assigned == *form_A) && (form_Assigned != *form_B);
        form_Assigned = *form_B;
        ok = ok && (form_Assigned != *form_A) && (form_Assigned == *form_B);
        if (ok) {
            it_out << "copy constructor and operator= tested." << endl;
        }else{
            it_errln("***  copy constructor or operator= or operator == or operator != .");
        }
        delete form_A3;
    }
    

    delete form_A; delete form_A2; delete form_B; 

    char* testPattern = "0#none|1#one|2#many";
    ChoiceFormat form_pat( testPattern, status );
    if (!chkstatus( status, "***  ChoiceFormat contructor( newPattern, status)" )) {
        delete fileform;
        delete filenumform;
        delete pattform;
        return;
    }

    form_pat.toPattern( res1 );
    if (res1 == "0.0#none|1.0#one|2.0#many") {
        it_out << "ChoiceFormat contructor( newPattern, status) tested" << endl;
    }else{
        it_errln("***  ChoiceFormat contructor( newPattern, status) or toPattern result!");
    }

    double* d_a = new double[2];
    if (!d_a) { it_errln("*** allocation error."); return; }
    d_a[0] = 1.0; d_a[1] = 2.0;

    UnicodeString* s_a = new UnicodeString[2];
    if (!s_a) { it_errln("*** allocation error."); return; }
    s_a[0] = "first"; s_a[1] = "second";

    form_pat.adoptChoices( d_a, s_a, 2 );
    form_pat.toPattern( res1 );
    it_out << "ChoiceFormat adoptChoices toPattern: " << res1 << endl;
    if (res1 == "1.0#first|2.0#second") {
        it_out << "ChoiceFormat adoptChoices tested" << endl;
    }else{
        it_errln("***  ChoiceFormat adoptChoices result!");
    }

    double d_a2[] = { 3.0, 4.0 };
    UnicodeString s_a2[] = { "third", "forth" };

    form_pat.setChoices( d_a2, s_a2, 2 );
    form_pat.toPattern( res1 );
    it_out << "ChoiceFormat adoptChoices toPattern: " << res1 << endl;
    if (res1 == "3.0#third|4.0#forth") {
        it_out << "ChoiceFormat adoptChoices tested" << endl;
    }else{
        it_errln("***  ChoiceFormat adoptChoices result!");
    }

    str = "";
    fpos = 0;
    status = U_ZERO_ERROR;
    double arg_double = 3.0;
    res1 = form_pat.format( arg_double, str, fpos );
    it_out << "ChoiceFormat format:" << res1 << endl;
    if (res1 != "third") it_errln("***  ChoiceFormat format (double, ...) result!");

    str = "";
    fpos = 0;
    status = U_ZERO_ERROR;
    int32_t arg_long = 3;
    res1 = form_pat.format( arg_long, str, fpos );
    it_out << "ChoiceFormat format:" << res1 << endl;
    if (res1 != "third") it_errln("***  ChoiceFormat format (int32_t, ...) result!");

    Formattable ft( (int32_t)3 );
    str = "";
    fpos = 0;
    status = U_ZERO_ERROR;
    res1 = form_pat.format( ft, str, fpos, status );
    if (!chkstatus( status, "***  test_complex_example format (int32_t, ...)" )) {
        delete fileform;
        delete filenumform;
        delete pattform;
        return;
    }
    it_out << "ChoiceFormat format:" << res1 << endl;
    if (res1 != "third") it_errln("***  ChoiceFormat format (Formattable, ...) result!");

    Formattable fta[] = { (int32_t)3 };
    str = "";
    fpos = 0;
    status = U_ZERO_ERROR;
    res1 = form_pat.format( fta, 1, str, fpos, status );
    if (!chkstatus( status, "***  test_complex_example format (int32_t, ...)" )) {
        delete fileform;
        delete filenumform;
        delete pattform;
        return;
    }
    it_out << "ChoiceFormat format:" << res1 << endl;
    if (res1 != "third") it_errln("***  ChoiceFormat format (Formattable[], cnt, ...) result!");

    ParsePosition parse_pos = 0;
    Formattable result;
    UnicodeString parsetext("third");
    form_pat.parse( parsetext, result, parse_pos );
    double rd = (result.getType() == Formattable::kLong) ? result.getLong() : result.getDouble();
    if (rd == 3.0) {
        it_out << "parse( ..., ParsePos ) tested." << endl;
    }else{
        it_errln("*** ChoiceFormat parse( ..., ParsePos )!");
    }

    form_pat.parse( parsetext, result, status );
    rd = (result.getType() == Formattable::kLong) ? result.getLong() : result.getDouble();
    if (rd == 3.0) {
        it_out << "parse( ..., UErrorCode ) tested." << endl;
    }else{
        it_errln("*** ChoiceFormat parse( ..., UErrorCode )!");
    }

    /*
    UClassID classID = ChoiceFormat::getStaticClassID();
    if (classID == form_pat.getDynamicClassID()) {
        it_out << "getStaticClassID and getDynamicClassID tested." << endl;
    }else{
        it_errln("*** getStaticClassID and getDynamicClassID!");
    }
    */

    it_out << endl;

    delete fileform; 
    delete filenumform;
    delete pattform;
}

/**
 * test the use of next_Double with ChoiceFormat
 **/
void
TestChoiceFormat::TestChoiceNextDouble()
{

    double limit[] = {0.0, 1.0, 2.0};
    const UnicodeString formats[] = {"0.0<=Arg<=1.0",
                               "1.0<Arg<2.0",
                               "2.0<Arg"};
    limit[1] = ChoiceFormat::nextDouble( limit[1] );
    ChoiceFormat *cf = new ChoiceFormat(limit, formats, 3);
    FieldPosition status(0);
    UnicodeString toAppendTo;
    cf->format((int32_t)1, toAppendTo, status);
    if (toAppendTo != "0.0<=Arg<=1.0") {
        it_errln("ChoiceFormat cmp in testBug1");
    }
    it_out <<  toAppendTo << endl;
    delete cf;
}


/*
 * Return a random double (---Copied here from tsnmfmt.h---)
 **/
static double randDouble()
{
    // Assume 8-bit (or larger) rand values.  Also assume
    // that the system rand() function is very poor, which it always is.
    double d;
    int32_t i;
    for (i=0; i < sizeof(double); ++i)
    {
        char* poke = (char*)&d;
        poke[i] = (rand() & 0xFF);
    }
    return d;
}

/** 
 * test the numerical results of next_Double and previous_Double
 **/
void
TestChoiceFormat::TestGapNextDouble()
{

    double val;
    int32_t i;
    
    //test area between -15 and 15
    logln("TestChoiceFormat::TestGapNextDouble: ----- testing area between -15 and 15...");
    val = -15.0;
    while (val < 15.0) {
        testValue( val );
        val += 0.31;
    }
    
    //test closely +/- n values around zero
    logln("TestChoiceFormat::TestGapNextDouble: ----- testing closely +/- n values around zero...");
    int32_t test_n;
    if (quick) {
        test_n = 25;
    } else {
        test_n = 1000;
    }
    val = 0.0;
    for (i = 0; i < test_n; i++ ) {
        testValue( val );
        val = ChoiceFormat::nextDouble( val );
    }
    for (i = 0; i < (test_n + test_n); i++ ) {
        testValue( val );
        val = ChoiceFormat::previousDouble( val );
    }
    for (i = 0; i < test_n; i++ ) {
        testValue( val );
        val = ChoiceFormat::nextDouble( val );
    }
    if (val != 0.0) {
        errln("*** TestMessageFormat::TestGapNextDouble didn't come back to zero!");
    }

    // random numbers
    logln("TestChoiceFormat::TestGapNextDouble: ----- testing random numbers...");
    if (quick) {
        test_n = 25;
    } else {
        test_n = 5000;
    }
    srand(0);           // use common starting point to make test reproducable
    double negTestLimit = -DBL_MAX / 2.0; // has to be larger than this (not larger or  equal)
    double posTestLimit = DBL_MAX / 2.0; // has to be smaller than this (not smaller or equal)
    for (i = 0; i < test_n; i++) {
        val = randDouble();
        if ((val > negTestLimit) && (val < posTestLimit)) {
            testValue( val );
        }
    }

    // extreme positive values
    logln("TestChoiceFormat::TestGapNextDouble: ----- testing extreme positive values...");
    val = ChoiceFormat::previousDouble( posTestLimit );
    testValue( val );
    val = ChoiceFormat::nextDouble( DBL_MIN );
    testValue( val );
    val = ChoiceFormat::previousDouble( DBL_MIN );
    //logln((UnicodeString) "prev MIN: " + val );
    testValue( val );
    val = DBL_MIN;
    testValue( val );


    // extreme negative values
    logln("TestChoiceFormat::TestGapNextDouble: ----- testing extreme negative values...");
    val = ChoiceFormat::nextDouble( negTestLimit );
    testValue( val );
    val = ChoiceFormat::previousDouble( -DBL_MIN );
    testValue( val );
    val = ChoiceFormat::nextDouble( -DBL_MIN );
    //logln((UnicodeString) "next -MIN: " + val );
    testValue( val );
    val = -DBL_MIN;
    testValue( val );

    it_out << "MSG: nextDouble & previousDouble tested." << endl;
}

void foo (double* bar) {}

/** 
 * test a value for TestGapNextDouble
 **/
void
TestChoiceFormat::testValue( double val )
{
    double valnext = ChoiceFormat::nextDouble( val );
    double valprev = ChoiceFormat::previousDouble( val );

    if (val >= valnext) {
        errln( (UnicodeString)
            "*** TestChoiceFormat::testValue #1 nextDouble returns same or smaller value for:" + val );
        return;
    }

    if (val <= valprev) {
        errln( (UnicodeString)
            "*** TestChoiceFormat::testValue #2 PreviousDouble returns same or larger value for:" + val );
        return;
    }

    double middle;    
    double *middlePtr = &middle;
    *middlePtr = (val + valnext) / 2.0;
    foo(middlePtr); 
    if ((*middlePtr != val) && (*middlePtr != valnext)) {
        errln( (UnicodeString)
            "*** TestChoiceFormat::testValue #3 WARNING: There seems to be a gap for:" + val );
        return;
    }

    *middlePtr = (val + valprev) / 2.0;
    foo(middlePtr);

    if ((*middlePtr != val) && (*middlePtr != valprev)) {
         errln( (UnicodeString)
            "*** TestChoiceFormat::testValue #4 WARNING: There seems to be a gap for:" + val );
        return;
    }
}


void TestChoiceFormat::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    switch (index) {
        case 0: name = "TestSimpleExample"; 
                if (exec) logln("TestSuite Format/ChoiceFormat/Simple (f/chc/simple): ");
                if (exec) TestSimpleExample(); 
                break;
        case 1: name = "TestComplexExample"; 
                if (exec) logln("TestSuite Format/ChoiceFormat/Complex (f/chc/complex): ");
                if (exec) TestComplexExample(); 
                break;
        case 2: name = "TestChoiceNextDouble"; if (exec) TestChoiceNextDouble(); break;
        case 3: name = "TestGapNextDouble"; if (exec) TestGapNextDouble(); break;
        default: name = ""; break; //needed to end loop
    }
}
