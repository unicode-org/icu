/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "uobjtest.h"
#include <string.h>

#define TESTCLASSID_FACTORY(c, f) { delete testClass(f, #c, #f, c ::getStaticClassID()); if(U_FAILURE(status)) { errln(UnicodeString(#c " - " #f " - got err status ") + UnicodeString(u_errorName(status))); status = U_ZERO_ERROR; } }
#define TESTCLASSID_DEFAULT(c) delete testClass(new c, #c, "new " #c , c::getStaticClassID())
#define TESTCLASSID_ABSTRACT(c) testClass(NULL, #c, NULL, c::getStaticClassID())

#define MAX_CLASS_ID 100

UClassID    ids[MAX_CLASS_ID];
const char *ids_factory[MAX_CLASS_ID];
const char *ids_class[MAX_CLASS_ID];
uint32_t    ids_count = 0;

UObject *UObjectTest::testClass(UObject *obj,
				const char *className, const char *factory, 
				UClassID staticID)
{
  uint32_t i;
  UnicodeString what = UnicodeString(className) + " * x= " + UnicodeString(factory?factory:" ABSTRACT ") + "; ";
  UClassID dynamicID = NULL;

  if(ids_count >= MAX_CLASS_ID) {
    char count[100];
    sprintf(count, " (currently %d) ", MAX_CLASS_ID);
    errln(what + "FAIL: Fatal: Ran out of IDs! Increase MAX_CLASS_ID." + UnicodeString(count));
    return obj;
  }

  {
    char tmp[500];
    sprintf(tmp, " [static=0x%p] ", staticID);
    logln(what + tmp);
  }

  if(staticID == NULL) {
    errln( what + "FAIL: staticID == NULL!");
  }

  if(factory != NULL) {  /* NULL factory means: abstract */
    if(!obj) {
      errln( what + "FAIL: ==NULL!");
      return obj;
    }
    
    dynamicID = obj->getDynamicClassID();
    
    if(dynamicID == NULL) {
      errln(what + "FAIL: dynamicID == NULL!");
    }
    
    if(dynamicID != staticID) {
      errln(what + "FAIL: dynamicID != staticID!");
    }
  }

  // Bail out if static ID is null
  if(staticID == NULL) {
    return obj;
  }

  for(i=0;i<ids_count;i++) {
    if(staticID == ids[i]) {
      if(!strcmp(ids_class[i], className)) {
	logln(what + "OK: ID found is the same as " + UnicodeString(ids_class[i]) + UnicodeString(" *y= ") + ids_factory[i]);
	return obj; 
      } else {
	errln(what + "FAIL: ID is the same as " + UnicodeString(ids_class[i]) + UnicodeString(" *y= ") + ids_factory[i]);
	return obj;
      }
    }
  }

  ids[ids_count] = staticID;
  ids_factory[ids_count] = factory;
  ids_class[ids_count] = className;
  ids_count++;

  return obj;
}


// begin actual #includes for things to be tested
// 
// The following script will generate the #includes needed here:
//
//    find common i18n -name '*.h' -print | xargs fgrep ClassID | cut -d: -f1 | cut -d\/ -f2-  | sort | uniq | sed -e 's%.*%#include "&"%'

#include "anytrans.h"
#include "digitlst.h"
#include "esctrn.h"
#include "funcrepl.h"
#include "iculserv.h"
#include "icunotif.h"
#include "icuserv.h"
#include "name2uni.h"
#include "nfsubs.h"
#include "nortrans.h"
#include "quant.h"
#include "remtrans.h"
#include "strmatch.h"
#include "strrepl.h"
#include "titletrn.h"
#include "tolowtrn.h"
#include "toupptrn.h"
#include "unesctrn.h"
#include "uni2name.h"
//#include "unicode/bidi.h"
#include "unicode/brkiter.h"
#include "unicode/calendar.h"
#include "unicode/caniter.h"
#include "unicode/chariter.h"
#include "unicode/choicfmt.h"
#include "unicode/coleitr.h"
#include "unicode/coll.h"
//#include "unicode/convert.h"
#include "unicode/cpdtrans.h"
#include "unicode/datefmt.h"
#include "unicode/dbbi.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/fieldpos.h"
#include "unicode/fmtable.h"
#include "unicode/format.h"
#include "unicode/gregocal.h"
//#include "unicode/hextouni.h"
#include "unicode/locid.h"
#include "unicode/msgfmt.h"
#include "unicode/normlzr.h"
#include "unicode/nultrans.h"
#include "unicode/numfmt.h"
#include "unicode/parsepos.h"
#include "unicode/rbbi.h"
#include "unicode/rbnf.h"
#include "unicode/rbt.h"
#include "unicode/regex.h"
#include "unicode/resbund.h"
#include "unicode/schriter.h"
#include "unicode/simpletz.h"
#include "unicode/smpdtfmt.h"
#include "unicode/sortkey.h"
#include "unicode/stsearch.h"
#include "unicode/tblcoll.h"
#include "unicode/timezone.h"
#include "unicode/translit.h"
#include "unicode/uchriter.h"
#include "unicode/unifilt.h"
#include "unicode/unifunct.h"
#include "unicode/uniset.h"
#include "unicode/unistr.h"
//#include "unicode/unitohex.h"
#include "unicode/uobject.h"
#include "unicode/usetiter.h"
#include "unicode/utypes.h"
#include "uvector.h"

// END includes =============================================================


void UObjectTest::testIDs()
{
    ids_count = 0;
    UParseError parseError;
    UErrorCode status = U_ZERO_ERROR;
    
    
    //TESTCLASSID_DEFAULT(AbbreviatedUnicodeSetIterator);
    //TESTCLASSID_DEFAULT(AnonymousStringFactory);
    TESTCLASSID_FACTORY(AnyTransliterator, Transliterator::createInstance(UnicodeString("Any-Latin"), UTRANS_FORWARD, parseError, status));
    
    TESTCLASSID_FACTORY(CanonicalIterator, new CanonicalIterator(UnicodeString("abc"), status));
    //TESTCLASSID_DEFAULT(CollationElementIterator);
    TESTCLASSID_DEFAULT(CollationKey);
    //TESTCLASSID_FACTORY(CompoundTransliterator, Transliterator::createInstance(UnicodeString("Any-Jex;Hangul-Jamo"), UTRANS_FORWARD, parseError, status));
    
    //TESTCLASSID_DEFAULT(DateFormatSymbols);
    //TESTCLASSID_DEFAULT(DecimalFormatSymbols);
    //TESTCLASSID_DEFAULT(DictionaryBasedBreakIterator);
    //TESTCLASSID_DEFAULT(DigitList);
    
    //TESTCLASSID_DEFAULT(EscapeTransliterator);
    //TESTCLASSID_DEFAULT(EventListener);
    
    //TESTCLASSID_DEFAULT(FieldPosition);
    TESTCLASSID_DEFAULT(Formattable);
    //TESTCLASSID_DEFAULT(FunctionReplacer);
    
    //TESTCLASSID_DEFAULT(GregorianCalendar);
    
    TESTCLASSID_FACTORY(EscapeTransliterator, Transliterator::createInstance(UnicodeString("Any-Hex"), UTRANS_FORWARD, parseError, status));
        
    //TESTCLASSID_DEFAULT(ICUResourceBundleFactory);
    
    //TESTCLASSID_DEFAULT(Key); // does ont exist?
    
    TESTCLASSID_FACTORY(Locale, new Locale("123"));
    TESTCLASSID_ABSTRACT(LocaleKey);
    //TESTCLASSID_DEFAULT(LocaleKeyFactory);
    //TESTCLASSID_DEFAULT(LowercaseTransliterator);
    
    //TESTCLASSID_DEFAULT(NFSubstitution);
    //TESTCLASSID_DEFAULT(NameUnicodeTransliterator);
    //TESTCLASSID_DEFAULT(NormalizationTransliterator);
    //TESTCLASSID_DEFAULT(Normalizer);
    //TESTCLASSID_DEFAULT(NullTransliterator);
    TESTCLASSID_ABSTRACT(NumberFormat);
    //TESTCLASSID_DEFAULT(NumeratorSubstitution);
    
    TESTCLASSID_DEFAULT(ParsePosition);
    
    //TESTCLASSID_DEFAULT(Quantifier);
    
    //TESTCLASSID_DEFAULT(RegexCompile);
    //TESTCLASSID_DEFAULT(RegexMatcher);
    //TESTCLASSID_DEFAULT(RegexPattern);
    //TESTCLASSID_DEFAULT(RemoveTransliterator);
    //TESTCLASSID_DEFAULT(ReplaceableGlue);
    TESTCLASSID_FACTORY(ResourceBundle, new ResourceBundle(UnicodeString(), status) );
    //TESTCLASSID_DEFAULT(RuleBasedTransliterator);
    
    //TESTCLASSID_DEFAULT(SimpleFactory);
    //TESTCLASSID_DEFAULT(SimpleFwdCharIterator);
    //TESTCLASSID_DEFAULT(SimpleLocaleKeyFactory);
    //TESTCLASSID_DEFAULT(StringMatcher);
    //TESTCLASSID_DEFAULT(StringReplacer);
    //TESTCLASSID_DEFAULT(StringSearch);
    
    //TESTCLASSID_DEFAULT(TempSearch);
    //TESTCLASSID_DEFAULT(TestMultipleKeyStringFactory);
    //TESTCLASSID_DEFAULT(TestReplaceable);
    TESTCLASSID_ABSTRACT(TimeZone);
    TESTCLASSID_FACTORY(TitlecaseTransliterator,  Transliterator::createInstance(UnicodeString("Any-Title"), UTRANS_FORWARD, parseError, status));
    TESTCLASSID_ABSTRACT(Transliterator);
    
    TESTCLASSID_DEFAULT(UnicodeString);
    //TESTCLASSID_DEFAULT(UStack);
    //TESTCLASSID_DEFAULT(UVector);
    
#if 0
    int i;
    for(i=0;i<ids_count;i++) {
        char junk[800];
        sprintf(junk, " %4d:\t%p\t%s\t%s\n", 
            i, ids[i], ids_class[i], ids_factory[i]);
        logln(UnicodeString(junk));
    }
#endif
}

/* --------------- */

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;


void UObjectTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /* par */ )
{
    switch (index) {

    CASE(0, testIDs);

    default: name = ""; break; //needed to end loop
    }
}
