/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "uobjtest.h"
#include "cmemory.h" // UAlignedMemory
#include <string.h>
#include <stdio.h>

/**
 * 
 * Test for UObject, currently only the classID.
 *
 * Usage
 *   TESTCLASSID_ABSTRACT(Bar)
 *      --  Bar is expected to be abstract. Only the static ID will be tested.
 *
 *   TESTCLASSID_DEFAULT(Foo)
 *      --  Foo will be default-constructed.
 *
 *   TESTCLASSID_CTOR(Foo, (1, 2, 3, status))
 *      -- Second argument is (parenthesized) constructor argument.
 *          Will be called as:   new Foo ( 1, 2, 3, status)  [status is tested]
 *
 *   TESTCLASSID_FACTORY(Foo, fooCreateFunction(status) ) 
 *      -- call fooCreateFunction.  'status' will be tested & reset
 */


#define TESTCLASSID_FACTORY(c, f) { delete testClass(f, #c, #f, c ::getStaticClassID()); if(U_FAILURE(status)) { errln(UnicodeString(#c " - " #f " - got err status ") + UnicodeString(u_errorName(status))); status = U_ZERO_ERROR; } }
#define TESTCLASSID_TRANSLIT(c, t) { delete testClass(Transliterator::createInstance(UnicodeString(t), UTRANS_FORWARD,parseError,status), #c, "Transliterator: " #t, c ::getStaticClassID()); if(U_FAILURE(status)) { errln(UnicodeString(#c " - Transliterator: " #t " - got err status ") + UnicodeString(u_errorName(status))); status = U_ZERO_ERROR; } }
#define TESTCLASSID_CTOR(c, x) { delete testClass(new c x, #c, "new " #c #x, c ::getStaticClassID()); if(U_FAILURE(status)) { errln(UnicodeString(#c " - new " #x " - got err status ") + UnicodeString(u_errorName(status))); status = U_ZERO_ERROR; } }
#define TESTCLASSID_DEFAULT(c) delete testClass(new c, #c, "new " #c , c::getStaticClassID())
#define TESTCLASSID_ABSTRACT(c) testClass(NULL, #c, NULL, c::getStaticClassID())

#define MAX_CLASS_ID 200

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
    errln("FAIL: Fatal: Ran out of IDs! Increase MAX_CLASS_ID." + UnicodeString(count) + what);
    return obj;
  }

  if(obj) {
    dynamicID = obj->getDynamicClassID();
  }    

  {
    char tmp[500];
    sprintf(tmp, " [static=%p, dynamic=%p] ", staticID, dynamicID);
    logln(what + tmp);
  }

  if(staticID == NULL) {
    errln(  "FAIL: staticID == NULL!" + what);
  }

  if(factory != NULL) {  /* NULL factory means: abstract */
    if(!obj) {
      errln( "FAIL: ==NULL!" + what);
      return obj;
    }

    if(dynamicID == NULL) {
      errln("FAIL: dynamicID == NULL!" + what);
    }
    
    if(dynamicID != staticID) {
      errln("FAIL: dynamicID != staticID!" + what );
    }
  }

  // Bail out if static ID is null
  if(staticID == NULL) {
    return obj;
  }

  for(i=0;i<ids_count;i++) {
    if(staticID == ids[i]) {
      if(!strcmp(ids_class[i], className)) {
	logln("OK: ID found is the same as " + UnicodeString(ids_class[i]) + UnicodeString(" *y= ") + ids_factory[i] + what);
	return obj; 
      } else {
	errln("FAIL: ID is the same as " + UnicodeString(ids_class[i]) + UnicodeString(" *y= ") + ids_factory[i] + what);
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


#include "unicode/utypes.h"

// Things we Patch
#define protected public   /* to access private factory function */
#include "iculserv.h"
#undef protected

// Internal Things (woo)
#include "cpdtrans.h"
#include "rbt.h"
#include "rbt_data.h"
#include "nultrans.h"
#include "anytrans.h"
#include "digitlst.h"
#include "esctrn.h"
#include "funcrepl.h"
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
#include "uvector.h"
#include "islamcal.h"

// External Things
#include "unicode/brkiter.h"
#include "unicode/calendar.h"
#include "unicode/caniter.h"
#include "unicode/chariter.h"
#include "unicode/choicfmt.h"
#include "unicode/coleitr.h"
#include "unicode/coll.h"
#include "unicode/datefmt.h"
#include "unicode/dbbi.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/dtfmtsym.h"
#include "unicode/fieldpos.h"
#include "unicode/fmtable.h"
#include "unicode/format.h"
#include "unicode/gregocal.h"
#include "unicode/locid.h"
#include "unicode/msgfmt.h"
#include "unicode/normlzr.h"
#include "unicode/numfmt.h"
#include "unicode/parsepos.h"
#include "unicode/rbbi.h"
#include "unicode/rbnf.h"
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
#include "unicode/uobject.h"
#include "unicode/usetiter.h"
//#include "unicode/bidi.h"
//#include "unicode/convert.h"

// END includes =============================================================

#define UOBJTEST_TEST_INTERNALS 0   /* do NOT test Internal things - their functions aren't exported on Win32 */

void UObjectTest::testIDs()
{
    ids_count = 0;

#if !UCONFIG_NO_TRANSLITERATION
    UParseError parseError;
#endif
    UErrorCode status = U_ZERO_ERROR;
   

    
    //TESTCLASSID_DEFAULT(AbbreviatedUnicodeSetIterator);
    //TESTCLASSID_DEFAULT(AnonymousStringFactory);

    
#if !UCONFIG_NO_NORMALIZATION
    TESTCLASSID_FACTORY(CanonicalIterator, new CanonicalIterator(UnicodeString("abc"), status));
#endif
    //TESTCLASSID_DEFAULT(CollationElementIterator);
#if !UCONFIG_NO_COLLATION
    TESTCLASSID_DEFAULT(CollationKey);
#endif
    //TESTCLASSID_FACTORY(CompoundTransliterator, Transliterator::createInstance(UnicodeString("Any-Jex;Hangul-Jamo"), UTRANS_FORWARD, parseError, status));
    
#if !UCONFIG_NO_FORMATTING
    /* TESTCLASSID_FACTORY(NFSubstitution,  NFSubstitution::makeSubstitution(8, */
    /* TESTCLASSID_DEFAULT(DigitList);  UMemory but not UObject*/
    TESTCLASSID_ABSTRACT(NumberFormat);
    TESTCLASSID_CTOR(DateFormatSymbols, (status));
    TESTCLASSID_CTOR(DecimalFormatSymbols, (status));
#if UOBJTEST_TEST_INTERNALS
    TESTCLASSID_CTOR(FunctionReplacer, (NULL,NULL) ); /* don't care */
#endif
    TESTCLASSID_DEFAULT(FieldPosition);
    TESTCLASSID_DEFAULT(Formattable);
    TESTCLASSID_CTOR(GregorianCalendar, (status));
    TESTCLASSID_CTOR(IslamicCalendar, (Locale::getUS(), status));
#endif

#if !UCONFIG_NO_BREAK_ITERATION
    /* TESTCLASSID_ABSTRACT(BreakIterator); No staticID!  */
    TESTCLASSID_FACTORY(RuleBasedBreakIterator, BreakIterator::createLineInstance("mt",status));
    TESTCLASSID_FACTORY(DictionaryBasedBreakIterator, BreakIterator::createLineInstance("th",status));
#endif
    
    //TESTCLASSID_DEFAULT(EscapeTransliterator);
        
    //TESTCLASSID_DEFAULT(GregorianCalendar);
    
#if !UCONFIG_NO_TRANSLITERATION

    
    TESTCLASSID_TRANSLIT(AnyTransliterator, "Any-Latin");
    TESTCLASSID_TRANSLIT(CompoundTransliterator, "Latin-Greek");
    TESTCLASSID_TRANSLIT(EscapeTransliterator, "Any-Hex");
    TESTCLASSID_TRANSLIT(LowercaseTransliterator, "Lower");
    TESTCLASSID_TRANSLIT(NameUnicodeTransliterator, "Name-Any");
    TESTCLASSID_TRANSLIT(NormalizationTransliterator, "NFD");
    TESTCLASSID_TRANSLIT(NullTransliterator, "Null");
    TESTCLASSID_TRANSLIT(RemoveTransliterator, "Remove");
    TESTCLASSID_CTOR(RuleBasedTransliterator, (UnicodeString("abcd"), UnicodeString("a>b;"), status));
    TESTCLASSID_TRANSLIT(TitlecaseTransliterator, "Title");
    TESTCLASSID_TRANSLIT(UnescapeTransliterator, "Hex-Any");
    TESTCLASSID_TRANSLIT(UnicodeNameTransliterator, "Any-Name");
    TESTCLASSID_TRANSLIT(UppercaseTransliterator, "Upper");
#endif
        
    TESTCLASSID_FACTORY(Locale, new Locale("123"));
    
    //TESTCLASSID_DEFAULT(Normalizer);

    //TESTCLASSID_DEFAULT(NumeratorSubstitution);
    
#if !UCONFIG_NO_TRANSLITERATION
    TESTCLASSID_DEFAULT(ParsePosition);
    //TESTCLASSID_DEFAULT(Quantifier);
#endif
    

// NO_REG_EX
    //TESTCLASSID_DEFAULT(RegexCompile);
    //TESTCLASSID_DEFAULT(RegexMatcher);
    //TESTCLASSID_DEFAULT(RegexPattern);

    //TESTCLASSID_DEFAULT(ReplaceableGlue);
    TESTCLASSID_FACTORY(ResourceBundle, new ResourceBundle(UnicodeString(), status) );
    //TESTCLASSID_DEFAULT(RuleBasedTransliterator);
    
    //TESTCLASSID_DEFAULT(SimpleFwdCharIterator);
    //TESTCLASSID_DEFAULT(StringReplacer);
    //TESTCLASSID_DEFAULT(StringSearch);
    
    //TESTCLASSID_DEFAULT(TempSearch);
    //TESTCLASSID_DEFAULT(TestMultipleKeyStringFactory);
    //TESTCLASSID_DEFAULT(TestReplaceable);

#if !UCONFIG_NO_FORMATTING
    TESTCLASSID_ABSTRACT(TimeZone);
#endif

#if !UCONFIG_NO_TRANSLITERATION
    TESTCLASSID_FACTORY(TitlecaseTransliterator,  Transliterator::createInstance(UnicodeString("Any-Title"), UTRANS_FORWARD, parseError, status));
    TESTCLASSID_ABSTRACT(Transliterator);

#if UOBJTEST_TEST_INTERNALS
    TESTCLASSID_CTOR(StringMatcher, (UnicodeString("x"), 0,0,0,TransliterationRuleData(status)));
    TESTCLASSID_CTOR(StringReplacer,(UnicodeString(),new TransliterationRuleData(status)));
#endif
#endif
    
    TESTCLASSID_DEFAULT(UnicodeString);
    TESTCLASSID_CTOR(UnicodeSet, (0, 1));
    TESTCLASSID_ABSTRACT(UnicodeFilter);
    TESTCLASSID_ABSTRACT(UnicodeFunctor);
    TESTCLASSID_CTOR(UnicodeSetIterator,(UnicodeSet(0,1)));
    TESTCLASSID_CTOR(UStack, (status));
    TESTCLASSID_CTOR(UVector, (status));

#if !UCONFIG_NO_SERVICE
    TESTCLASSID_CTOR(SimpleFactory, (NULL, UnicodeString("foo")));
    TESTCLASSID_DEFAULT(EventListener);
#if UOBJTEST_TEST_INTERNALS
    TESTCLASSID_DEFAULT(ICUResourceBundleFactory);
    //TESTCLASSID_DEFAULT(Key); // does ont exist?
    TESTCLASSID_CTOR(LocaleKey, (UnicodeString("baz"), UnicodeString("bat"), NULL, 92));
    TESTCLASSID_CTOR(LocaleKeyFactory, (42));
    TESTCLASSID_CTOR(SimpleLocaleKeyFactory, (NULL, UnicodeString("bar"), 8, 12) );
#endif
#endif

#if UOBJTEST_DUMP_IDS
    int i;
    for(i=0;i<ids_count;i++) {
        char junk[800];
        sprintf(junk, " %4d:\t%p\t%s\t%s\n", 
            i, ids[i], ids_class[i], ids_factory[i]);
        logln(UnicodeString(junk));
    }
#endif
}

void UObjectTest::testUMemory() {
    // additional tests for code coverage
#if U_OVERRIDE_CXX_ALLOCATION && U_HAVE_PLACEMENT_NEW
    UAlignedMemory stackMemory[sizeof(UnicodeString)/sizeof(UAlignedMemory)+1];
    UnicodeString *p;
    enum { len=20 };

    p=new(stackMemory) UnicodeString(len, (UChar32)0x20ac, len);
    if((void *)p!=(void *)stackMemory) {
        errln("placement new did not place the object at the expected address");
    }
    if(p->length()!=len || p->charAt(0)!=0x20ac || p->charAt(len-1)!=0x20ac) {
        errln("constructor used with placement new did not work right");
    }

    /*
     * It is not possible to simply say
     *     delete(p, stackMemory);
     * which results in a call to the normal, non-placement delete operator.
     *
     * Via a search on google.com for "c++ placement delete" I found
     * http://cpptips.hyperformix.com/cpptips/placement_del3
     * which says:
     *
     * TITLE: using placement delete
     *
     * (Newsgroups: comp.std.c++, 27 Aug 97)
     *
     * ISJ: isj@image.dk
     *
     * > I do not completely understand how placement works on operator delete.
     * > ...
     * There is no delete-expression which will invoke a placement
     * form of operator delete. You can still call the function
     * explicitly. Example:
     * ...
     *     // destroy object and delete space manually
     *     p->~T();
     *     operator delete(p, 12);
     *
     * ... so that's what I am doing here.
     * markus 20031216
     */
    // destroy object and delete space manually
    p->~UnicodeString(); 
    UnicodeString::operator delete(p, stackMemory); 
#endif

    // try to call the compiler-generated UMemory::operator=(class UMemory const &)
    UMemory m, n;
    m=n;
}

/* --------------- */

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;


void UObjectTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /* par */ )
{
    switch (index) {

    CASE(0, testIDs);
    CASE(1, testUMemory);

    default: name = ""; break; //needed to end loop
    }
}
