
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/


#include "unicode/utypes.h"

#include "intltest.h"
#include "tfsmalls.h"

#include "unicode/msgfmt.h"
#include "unicode/choicfmt.h"

#include "unicode/parsepos.h"
#include "unicode/fieldpos.h"
#include "unicode/fmtable.h"


static bool_t chkstatus( UErrorCode &status, char* msg = NULL )
{
    bool_t ok = (status == U_ZERO_ERROR);
    if (!ok) it_errln( msg );
    return ok;
}

void test_ParsePosition( void )
{
    ParsePosition* pp1 = new ParsePosition();
    if (pp1 && (pp1->getIndex() == 0)) {
        it_out << "PP constructor() tested." << endl;
    }else{
        it_errln("*** PP getIndex or constructor() result");
    }
    delete pp1;

    
    {
        UTextOffset to = 5;
        ParsePosition pp2( to );
        if (pp2.getIndex() == 5) {
            it_out << "PP getIndex and constructor(UTextOffset) tested." << endl;
        }else{
            it_errln("*** PP getIndex or constructor(UTextOffset) result");
        }
        pp2.setIndex( 3 );
        if (pp2.getIndex() == 3) {
            it_out << "PP setIndex tested." << endl;
        }else{
            it_errln("*** PP getIndex or setIndex result");
        }
    }

    ParsePosition pp2, pp3;
    pp2 = 3;
    pp3 = 5;
    ParsePosition pp4( pp3 );
    if ((pp2 != pp3) && (pp3 == pp4)) {
        it_out << "PP copy contructor, operator== and operator != tested." << endl;
    }else{
        it_errln("*** PP operator== or operator != result");
    }

    ParsePosition pp5;
    pp5 = pp4;
    if ((pp4 == pp5) && (!(pp4 != pp5))) {
        it_out << "PP operator= tested." << endl;
    }else{
        it_errln("*** PP operator= operator== or operator != result");
    }


}

#include "unicode/decimfmt.h"

void test_FieldPosition_example( void )
{
    //***** no error detection yet !!!!!!!
    //***** this test is for compiler checks and visual verification only.
    double doubleNum[] = { 123456789.0, -12345678.9, 1234567.89, -123456.789,
        12345.6789, -1234.56789, 123.456789, -12.3456789, 1.23456789};
    int32_t dNumSize = (int32_t)(sizeof(doubleNum)/sizeof(double));

    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat* fmt = (DecimalFormat*) NumberFormat::createInstance(status);
    fmt->setDecimalSeparatorAlwaysShown(TRUE);
    
    const int32_t tempLen = 20;
    char temp[tempLen];
    
    for (int32_t i=0; i<dNumSize; i++) {
        FieldPosition pos(NumberFormat::INTEGER_FIELD);
        UnicodeString buf;
        //char fmtText[tempLen];
        //ToCharString(fmt->format(doubleNum[i], buf, pos), fmtText);
        UnicodeString res = fmt->format(doubleNum[i], buf, pos);
        for (int32_t j=0; j<tempLen; j++) temp[j] = '='; // clear with spaces
        UTextOffset tempOffset = (tempLen <= (tempLen - pos.getEndIndex())) ? 
            tempLen : (tempLen - pos.getEndIndex());
        temp[tempOffset] = '\0';
        //cout << temp << fmtText   << endl;
        it_out << "FP " << temp << res << endl;
    }
    delete fmt;
    
    it_out << endl;

}

void test_FieldPosition( void )
{

    FieldPosition fp( 7 );

    if (fp.getField() == 7) {
        it_out << "FP constructor(int32_t) and getField tested." << endl;
    }else{
        it_errln("*** FP constructor(int32_t) or getField");
    }

    FieldPosition* fph = new FieldPosition( 3 );
    if ( fph->getField() != 3) it_errln("*** FP getField or heap constr.");
    delete fph;

    bool_t err1 = FALSE;
    bool_t err2 = FALSE;
    bool_t err3 = FALSE;
    for (int32_t i = -50; i < 50; i++ ) {
        fp.setField( i+8 );
        fp.setBeginIndex( i+6 );
        fp.setEndIndex( i+7 );
        if (fp.getField() != i+8)  err1 = TRUE;
        if (fp.getBeginIndex() != i+6) err2 = TRUE;
        if (fp.getEndIndex() != i+7) err3 = TRUE;
    }
    if (!err1) {
        it_out << "FP setField and getField tested." << endl;
    }else{
        it_errln("*** FP setField or getField");
    }
    if (!err2) {
        it_out << "FP setBeginIndex and getBeginIndex tested." << endl;
    }else{
        it_errln("*** FP setBeginIndex or getBeginIndex");
    }
    if (!err3) {
        it_out << "FP setEndIndex and getEndIndex tested." << endl;
    }else{
        it_errln("*** FP setEndIndex or getEndIndex");
    }

    it_out << endl;

}

void test_Formattable( void )
{
    UErrorCode status = U_ZERO_ERROR;

    Formattable* ftp = new Formattable();
    if (!ftp || !(ftp->getType() == Formattable::kLong) || !(ftp->getLong() == 0)) {
        it_errln("*** Formattable constructor or getType or getLong");
    }
    delete ftp;

    Formattable fta, ftb;
    fta.setLong(1); ftb.setLong(2);
    if ((fta != ftb) || !(fta == ftb)) {
        it_out << "FT setLong, operator== and operator!= tested." << endl;
    }else{
        it_errln("*** Formattable setLong or operator== or !=");
    }
    fta = ftb;
    if ((fta == ftb) || !(fta != ftb)) {
        it_out << "FT operator= tested." << endl;
    }else{
        it_errln("*** FT operator= or operator== or operator!=");
    }
    
    fta.setDouble( 3.0 );
    if ((fta.getType() == Formattable::kDouble) && (fta.getDouble() == 3.0)) {
        it_out << "FT set- and getDouble tested." << endl;
    }else{
        it_errln("*** FT set- or getDouble");
    }

    fta.setDate( 4.0 );
    if ((fta.getType() == Formattable::kDate) && (fta.getDate() == 4.0)) {
        it_out << "FT set- and getDate tested." << endl;
    }else{
        it_errln("*** FT set- or getDate");
    }

    fta.setString("abc");
    UnicodeString res;
    if ((fta.getType() == Formattable::kString) && (fta.getString(res) == "abc")) {
        it_out << "FT set- and getString tested." << endl;
    }else{
        it_errln("*** FT set- or getString");
    }


    UnicodeString ucs = "unicode-string";
    UnicodeString* ucs_ptr = new UnicodeString("pointed-to-unicode-string");

    const Formattable ftarray[] = 
    {
        Formattable( 1.0, Formattable::kIsDate ),
        2.0,
        T_INT32(3),
        ucs,
        ucs_ptr
    };
    const int32_t ft_cnt = sizeof(ftarray) / sizeof(Formattable);
    Formattable ft_arr( ftarray, ft_cnt );
    UnicodeString temp;
    if ((ft_arr[0].getType() == Formattable::kDate)   && (ft_arr[0].getDate()   == 1.0)
     && (ft_arr[1].getType() == Formattable::kDouble) && (ft_arr[1].getDouble() == 2.0)
     && (ft_arr[2].getType() == Formattable::kLong)   && (ft_arr[2].getLong()   == T_INT32(3))
     && (ft_arr[3].getType() == Formattable::kString) && (ft_arr[3].getString(temp) == ucs)
     && (ft_arr[4].getType() == Formattable::kString) && (ft_arr[4].getString(temp) == *ucs_ptr) ) {
        it_out << "FT constr. for date, double, long, ustring, ustring* and array tested" << endl;
    }else{
        it_errln("*** FT constr. for date, double, long, ustring, ustring* or array");
    }

    int32_t res_cnt;
    const Formattable* res_array = ft_arr.getArray( res_cnt );
    if (res_cnt == ft_cnt) {
        bool_t same  = TRUE;
        for (int32_t i = 0; i < res_cnt; i++ ) {
            if (res_array[i] != ftarray[i]) {
                same = FALSE;
            }
        }
        if (same) {
            it_out << "FT getArray tested" << endl;
        }else{
            it_errln("*** FT getArray comparison");
        }
    }else{
        it_out << res_cnt << " " << ft_cnt << endl;
        it_errln("*** FT getArray count");
    }

    const Formattable ftarr1[] = { Formattable( T_INT32(1) ), Formattable( T_INT32(2) ) };
    const Formattable ftarr2[] = { Formattable( T_INT32(3) ), Formattable( T_INT32(4) ) };

    const int32_t ftarr1_cnt = sizeof(ftarr1) / sizeof(Formattable);
    const int32_t ftarr2_cnt = sizeof(ftarr2) / sizeof(Formattable);

    ft_arr.setArray( ftarr1, ftarr1_cnt );
    if ((ft_arr[0].getType() == Formattable::kLong) && (ft_arr[0].getLong() == T_INT32(1))) {
        it_out << "FT setArray tested" << endl;
    }else{
        it_errln("*** FT setArray");
    }

    Formattable* ft_dynarr = new Formattable[ftarr2_cnt];
    for (int32_t i = 0; i < ftarr2_cnt; i++ ) {
        ft_dynarr[i] = ftarr2[i];
    }
    if ((ft_dynarr[0].getType() == Formattable::kLong) && (ft_dynarr[0].getLong() == T_INT32(3))
     && (ft_dynarr[1].getType() == Formattable::kLong) && (ft_dynarr[1].getLong() == T_INT32(4))) {
        it_out << "FT operator= and array operations tested" << endl;
    }else{
        it_errln("*** FT operator= or array operations");
    }

    ft_arr.adoptArray( ft_dynarr, ftarr2_cnt );
    if ((ft_arr[0].getType() == Formattable::kLong) && (ft_arr[0].getLong() == T_INT32(3))
     && (ft_arr[1].getType() == Formattable::kLong) && (ft_arr[1].getLong() == T_INT32(4))) {
        it_out << "FT adoptArray tested" << endl;
    }else{
        it_errln("*** FT adoptArray or operator[]");
    }

    ft_arr.setLong(0);   // calls 'dispose' and deletes adopted array !

    UnicodeString* ucs_dyn = new UnicodeString("ttt");
    UnicodeString tmp2;

    fta.adoptString( ucs_dyn );
    if ((fta.getType() == Formattable::kString) && (fta.getString(tmp2) == "ttt")) {
        it_out << "FT adoptString tested" << endl;
    }else{
        it_errln("*** FT adoptString or getString");
    }
    fta.setLong(0);   // calls 'dispose' and deletes adopted string !

    it_out << endl;

}

void TestFormatSmallClasses::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    switch (index) {
        case 0: name = "pp"; 
                if (exec) logln("TestSuite Format/SmallClasses/ParsePosition (f/chc/sma/pp): ");
                if (exec) test_ParsePosition(); 
                break;
        case 1: name = "fp"; 
                if (exec) logln("TestSuite Format/SmallClasses/FieldPosition (f/chc/sma/fp): ");
                if (exec) test_FieldPosition(); 
                break;
        case 2: name = "fpe"; 
                if (exec) logln("TestSuite Format/SmallClasses/FieldPositionExample (f/chc/sma/fpe): ");
                if (exec) test_FieldPosition_example(); 
                break;
        case 3: name = "ft"; 
                if (exec) logln("TestSuite Format/SmallClasses/Formattable (f/chc/sma/ft): ");
                if (exec) test_Formattable(); 
                break;
        default: name = ""; break; //needed to end loop
    }
}
