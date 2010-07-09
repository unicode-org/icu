/*
**********************************************************************
* Copyright (c) 2002-2010,International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/

#include "DateFmtPerf.h"
#include "uoptions.h"
#include <stdio.h>

#include <iostream>
using namespace std;

DateFormatPerfTest::DateFormatPerfTest(int32_t argc, const char* argv[], UErrorCode& status)
: UPerfTest(argc,argv,status) {

}

DateFormatPerfTest::~DateFormatPerfTest()
{
}

UPerfFunction* DateFormatPerfTest::runIndexedTest(int32_t index, UBool exec,const char* &name, char* par) {

	//exec = true;

    switch (index) {
        TESTCASE(0,DateFmt250);
        TESTCASE(1,DateFmt10000);
		TESTCASE(2,DateFmt100000);
        TESTCASE(3,BreakItWord250);
		TESTCASE(4,BreakItWord10000);
		TESTCASE(5,BreakItChar250);
		TESTCASE(6,BreakItChar10000);

        default: 
            name = ""; 
            return NULL;
    }
    return NULL;
}


UPerfFunction* DateFormatPerfTest::DateFmt250(){
    DateFmtFunction* func= new DateFmtFunction(1);
    return func;
}

UPerfFunction* DateFormatPerfTest::DateFmt10000(){
    DateFmtFunction* func= new DateFmtFunction(40);
    return func;
}

UPerfFunction* DateFormatPerfTest::DateFmt100000(){
    DateFmtFunction* func= new DateFmtFunction(400);
    return func;
}

UPerfFunction* DateFormatPerfTest::BreakItWord250(){
    BreakItFunction* func= new BreakItFunction(250, true);
    return func;
}

UPerfFunction* DateFormatPerfTest::BreakItWord10000(){
    BreakItFunction* func= new BreakItFunction(10000, true);
    return func;
}
 
UPerfFunction* DateFormatPerfTest::BreakItChar250(){
    BreakItFunction* func= new BreakItFunction(250, false);
    return func;
}

UPerfFunction* DateFormatPerfTest::BreakItChar10000(){
    BreakItFunction* func= new BreakItFunction(10000, false);
    return func;
}


int main(int argc, const char* argv[]){

	cout << "ICU version - " << U_ICU_VERSION << endl;

    UErrorCode status = U_ZERO_ERROR;
    DateFormatPerfTest test(argc, argv, status);
    if(U_FAILURE(status)){   // ERROR HERE!!!
		cout << "initialize failed! " << status << endl;
        return status;
    }
	//cout << "Done initializing!\n" << endl;
    if(test.run()==FALSE){
		cout << "run failed!" << endl;
        fprintf(stderr,"FAILED: Tests could not be run please check the arguments.\n");
        return -1;
    }
	cout << "done!" << endl;
    return 0;
}