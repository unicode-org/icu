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
 
#include "miscdtfm.h"

#include "format.h"
#include "decimfmt.h"
#include "datefmt.h"
#include "smpdtfmt.h"
#include "dtfmtsym.h"
#include "locid.h"
#include "msgfmt.h"
#include "numfmt.h"
#include "choicfmt.h"
#include "gregocal.h"

// *****************************************************************************
// class DateFormatMiscTests
// *****************************************************************************

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void 
DateFormatMiscTests::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    // if (exec) logln((UnicodeString)"TestSuite DateFormatMiscTests");
    switch (index) {
        CASE(0, test4097450)
        CASE(1, test4099975)
        CASE(2, test4117335)

        default: name = ""; break;
    }
}

const char* 
DateFormatMiscTests::errorName(UErrorCode code)
{
    switch (code) {
        case ZERO_ERROR:                return "ZERO_ERROR";
        case ILLEGAL_ARGUMENT_ERROR:    return "ILLEGAL_ARGUMENT_ERROR";
        case MISSING_RESOURCE_ERROR:    return "MISSING_RESOURCE_ERROR";
        case INVALID_FORMAT_ERROR:      return "INVALID_FORMAT_ERROR";
        case FILE_ACCESS_ERROR:         return "FILE_ACCESS_ERROR";
        case INTERNAL_PROGRAM_ERROR:    return "INTERNAL_PROGRAM_ERROR";
        case MESSAGE_PARSE_ERROR:       return "MESSAGE_PARSE_ERROR";
        case MEMORY_ALLOCATION_ERROR:   return "MEMORY_ALLOCATION_ERROR";
        case USING_FALLBACK_ERROR:      return "USING_FALLBACK_ERROR";
        case USING_DEFAULT_ERROR:       return "USING_DEFAULT_ERROR";
        default:                        return "[BOGUS UErrorCode]";
    }
}

bool_t 
DateFormatMiscTests::failure(UErrorCode status, const char* msg)
{
    if(FAILURE(status)) {
        errln(UnicodeString("FAIL: ") + msg + " failed, error " + errorName(status));
        return TRUE;
    }

    return FALSE;
}

/*
 * @test @(#)$RCSfile: miscdtfm.cpp,v $ $Revision: 1.1 $ $Date: 1999/08/16 21:51:30 $
 *
 * @bug 4097450
 */
void
DateFormatMiscTests::test4097450()
{
    //
    // Date parse requiring 4 digit year.
    //
    UnicodeString  dstring [] = {
        UnicodeString("97"),
        UnicodeString("1997"),  
        UnicodeString("97"),
        UnicodeString("1997"),
        UnicodeString("01"),
        UnicodeString("2001"),  
        UnicodeString("01"),
        UnicodeString("2001"),  
        UnicodeString("1"),
        UnicodeString("1"),
        UnicodeString("11"),  
        UnicodeString("11"),
        UnicodeString("111"), 
        UnicodeString("111")
    };
    
    UnicodeString dformat [] = {
        UnicodeString("yy"),  
        UnicodeString("yy"),
        UnicodeString("yyyy"),
        UnicodeString("yyyy"),
        UnicodeString("yy"),  
        UnicodeString("yy"),
        UnicodeString("yyyy"),
        UnicodeString("yyyy"),
        UnicodeString("yy"),
        UnicodeString("yyyy"),
        UnicodeString("yy"),
        UnicodeString("yyyy"), 
        UnicodeString("yy"),
        UnicodeString("yyyy")
    };
    
    bool_t dresult [] = {
        TRUE, 
        FALSE, 
        FALSE,  
        TRUE,
        TRUE, 
        FALSE, 
        FALSE,  
        TRUE,
        FALSE,
        FALSE,
        TRUE, 
        FALSE,
        FALSE, 
        FALSE
    };

    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *formatter;
    SimpleDateFormat *resultFormatter = new SimpleDateFormat("yyyy", status);
    failure(status, "new SimpleDateFormat");

    logln("Format\tSource\tResult");
    logln("-------\t-------\t-------");
    for (int i = 0; i < 14/*dstring.length*/; i++)
    {
        log(dformat[i] + "\t" + dstring[i] + "\t");
        formatter = new SimpleDateFormat(dformat[i], status);
        failure(status, "new SimpleDateFormat");
        //try {
        UnicodeString str;
        FieldPosition pos(FieldPosition::DONT_CARE);
        logln(resultFormatter->format(formatter->parse(dstring[i], status), str, pos));
        failure(status, "resultFormatter->format");
            //if ( !dresult[i] ) System.out.print("   <-- error!");
        /*}
        catch (ParseException exception) {
            //if ( dresult[i] ) System.out.print("   <-- error!");
            System.out.print("exception --> " + exception);
        }*/
        delete formatter;
        logln();
    }

    delete resultFormatter;
}

/*
 * @test @(#)$RCSfile: miscdtfm.cpp,v $ $Revision: 1.1 $ $Date: 1999/08/16 21:51:30 $
 *
 * @bug 4099975
 */

void
DateFormatMiscTests::test4099975()
{
    UErrorCode status = ZERO_ERROR;
    DateFormatSymbols *symbols = new DateFormatSymbols(status);
    failure(status, "new DateFormatSymbols");
    SimpleDateFormat *df = new SimpleDateFormat(UnicodeString("E hh:mm"), symbols, status);
    failure(status, "new SimpleDateFormat");
    UnicodeString res;
    logln(df->toLocalizedPattern(res, status));
    failure(status, "df->toLocalizedPattern");
    symbols->setLocalPatternChars(UnicodeString("abcdefghijklmonpqr")); // change value of field
    logln(df->toLocalizedPattern(res, status));
    failure(status, "df->toLocalizedPattern");

    delete df;
}

/*
 * @test @(#)bug4117335.java    1.1 3/5/98
 *
 * @bug 4117335
 */
void
DateFormatMiscTests::test4117335()
{
    //UnicodeString bc = "\u7d00\u5143\u524d";
    UChar bcC [] = {
        0x7D00,
        0x5143,
        0x524D
    };
    UnicodeString bc(bcC, 3, 3);

    //UnicodeString ad = "\u897f\u66a6";
    UChar adC [] = {
        0x897F,
        0x66A6
    };
    UnicodeString ad(adC, 2, 2);
    
    //UnicodeString jstLong = "\u65e5\u672c\u6a19\u6e96\u6642";
    UChar jstLongC [] = {
        0x65e5,
        0x672c,
        0x6a19,
        0x6e96,
        0x6642
    };
    UnicodeString jstLong(jstLongC, 5, 5);

    UnicodeString jstShort = "JST";

    
    UErrorCode status = ZERO_ERROR;
    DateFormatSymbols *symbols = new DateFormatSymbols(Locale::JAPAN, status);
    failure(status, "new DateFormatSymbols");
    int32_t eraCount = 0;
    const UnicodeString *eras = symbols->getEras(eraCount);
    
    logln(UnicodeString("BC = ") + eras[0]);
    if (eras[0] != bc) {
        errln("*** Should have been " + bc);
        //throw new Exception("Error in BC");
    }

    logln(UnicodeString("AD = ") + eras[1]);
    if (eras[1] != ad) {
        errln("*** Should have been " + ad);
        //throw new Exception("Error in AD");
    }

    int32_t rowCount, colCount;
    const UnicodeString **zones = symbols->getZoneStrings(rowCount, colCount);
    logln(UnicodeString("Long zone name = ") + zones[0][1]);
    if (zones[0][1] != jstLong) {
        errln("*** Should have been " + jstLong);
        //throw new Exception("Error in long TZ name");
    }
    logln(UnicodeString("Short zone name = ") + zones[0][2]);
    if (zones[0][2] != jstShort) {
        errln("*** Should have been " + jstShort);
        //throw new Exception("Error in short TZ name");
    }
    logln(UnicodeString("Long zone name = ") + zones[0][3]);
    if (zones[0][3] != jstLong) {
        errln("*** Should have been " + jstLong);
        //throw new Exception("Error in long TZ name");
    }
    logln(UnicodeString("SHORT zone name = ") + zones[0][4]);
    if (zones[0][4] != jstShort) {
        errln("*** Should have been " + jstShort);
        //throw new Exception("Error in short TZ name");
    }

}
