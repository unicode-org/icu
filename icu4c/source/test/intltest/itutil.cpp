/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


/**
 * IntlTestUtilities is the medium level test class for everything in the directory "utility".
 */

#include "unicode/utypes.h"
#include "itutil.h"
#include "strtest.h"
#include "loctest.h"
#include "citrtest.h"
#include "ustrtest.h"
#include "ucdtest.h"
#include "restest.h"
#include "restsnew.h"
#include "tsmthred.h"
#include "tsputil.h"
#include "uobjtest.h"
#include "v32test.h"
//#include "custrtest.h"
//#include "ccitrtst.h"
//#include "cloctest.h"
//#include "ctres.h"
//#include "ctucd.h"

void IntlTestUtilities::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
{
    if (exec) logln("TestSuite Utilities: ");
    switch (index) {
        case 0:
            name = "MultithreadTest"; 
            if (exec) {
                logln("MultithreadTest---"); logln("");
                MultithreadTest test;
                callTest( test, par );
            }
            break;

        case 1:
            name = "StringTest"; 
            if (exec) {
                logln("StringTest---"); logln("");
                StringTest test;
                callTest( test, par );
            }
            break;

        case 2:
            name = "UnicodeStringTest"; 
            if (exec) {
                logln("UnicodeStringTest---"); logln("");
                UnicodeStringTest test;
                callTest( test, par );
            }
            break;

        case 3:
            name = "LocaleTest"; 
            if (exec) {
                logln("LocaleTest---"); logln("");
                LocaleTest test;
                callTest( test, par );
            }
            break;

        case 4:
            name = "CharIterTest"; 
            if (exec) {
                logln("CharIterTest---"); logln("");
                CharIterTest test;
                callTest( test, par );
            }
            break;

        case 5:
            name = "UnicodeTest"; 
            if (exec) {
                logln("UnicodeTest---"); logln("");
                UnicodeTest test;
                callTest( test, par );
            }
            break;

        case 6:
            name = "ResourceBundleTest"; 
            if (exec) {
                logln("ResourceBundleTest---"); logln("");
                ResourceBundleTest test;
                callTest( test, par );
            }
            break;
        case 7:
            name = "NewResourceBundleTest"; 
            if (exec) {
                logln("NewResourceBundleTest---"); logln("");
                NewResourceBundleTest test;
                callTest( test, par );
            }
            break;

        case 8:
            name = "PUtilTest"; 
            if (exec) {
                logln("PUtilTest---"); logln("");
                PUtilTest test;
                callTest( test, par );
            }
            break;
            
        case 9:
            name = "UObjectTest";
            if(exec) {
                logln ("UObjectTest---"); logln("");
                UObjectTest test;
                callTest( test, par );
            }
            break;;

        case 10:
            name = "UVector32Test";
            if(exec) {
                logln ("UVector32Test---"); logln("");
                UVector32Test test;
                callTest( test, par );
            }
            break;;

        /*
        case 8:
            name = "LocaleTest"; 
            if (exec) {
                logln("LocaleTest---"); logln("");
                CLocaleTest test;
                callTest( test, par );
            }
            break;
        */
        default: name = ""; break; //needed to end loop
    }
}

