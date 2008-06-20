/*
 **********************************************************************
 *   Copyright (C) 2005-2008, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */


#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/unistr.h"
#include "unicode/putil.h"
#include "unicode/usearch.h"

#include "cmemory.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ucoleitr.h"

#include "unicode/regex.h"        // TODO: make conditional on regexp being built.

#include "unicode/uniset.h"
#include "unicode/uset.h"
#include "unicode/ustring.h"
#include "hash.h"
#include "uhash.h"
#include "ucol_imp.h"

#include "intltest.h"
#include "ssearch.h"

#include "xmlparser.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

char testId[100];

#define TEST_ASSERT(x) {if (!(x)) { \
    errln("Failure in file %s, line %d, test ID = \"%s\"", __FILE__, __LINE__, testId);}}

#define TEST_ASSERT_M(x, m) {if (!(x)) { \
    errln("Failure in file %s, line %d.   \"%s\"", __FILE__, __LINE__, m);return;}}

#define TEST_ASSERT_SUCCESS(errcode) {if (U_FAILURE(errcode)) { \
    errln("Failure in file %s, line %d, test ID \"%s\", status = \"%s\"", \
          __FILE__, __LINE__, testId, u_errorName(errcode));}}

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])

//---------------------------------------------------------------------------
//
//  Test class boilerplate
//
//---------------------------------------------------------------------------
SSearchTest::SSearchTest()
{
}

SSearchTest::~SSearchTest()
{
}

void SSearchTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char *params )
{
    if (exec) logln("TestSuite SSearchTest: ");
    switch (index) {
#if !UCONFIG_NO_BREAK_ITERATION
       case 0: name = "searchTest";
            if (exec) searchTest();
            break;

        case 1: name = "offsetTest";
            if (exec) offsetTest();
            break;

        case 2: name = "monkeyTest";
            if (exec) monkeyTest(params);
            break;
#endif
        default: name = "";
            break; //needed to end loop
    }
}


#if !UCONFIG_NO_BREAK_ITERATION

#define PATH_BUFFER_SIZE 2048
const char *SSearchTest::getPath(char buffer[2048], const char *filename) {
    UErrorCode status = U_ZERO_ERROR;
    const char *testDataDirectory = IntlTest::getSourceTestData(status);

    if (U_FAILURE(status) || strlen(testDataDirectory) + strlen(filename) + 1 >= PATH_BUFFER_SIZE) {
        errln("ERROR: getPath() failed - %s", u_errorName(status));
        return NULL;
    }

    strcpy(buffer, testDataDirectory);
    strcat(buffer, filename);
    return buffer;
}


void SSearchTest::searchTest()
{
#if !UCONFIG_NO_REGULAR_EXPRESSIONS
    UErrorCode status = U_ZERO_ERROR;
    char path[PATH_BUFFER_SIZE];
    const char *testFilePath = getPath(path, "ssearch.xml");

    if (testFilePath == NULL) {
        return; /* Couldn't get path: error message already output. */
    }

    UXMLParser  *parser = UXMLParser::createParser(status);
    TEST_ASSERT_SUCCESS(status);
    UXMLElement *root   = parser->parseFile(testFilePath, status);
    TEST_ASSERT_SUCCESS(status);
    if (U_FAILURE(status)) {
        return;
    }

    const UnicodeString *debugTestCase = root->getAttribute("debug");
    if (debugTestCase != NULL) {
//       setenv("USEARCH_DEBUG", "1", 1);
    }


    const UXMLElement *testCase;
    int32_t tc = 0;

    while((testCase = root->nextChildElement(tc)) != NULL) {

        if (testCase->getTagName().compare("test-case") != 0) {
            errln("ssearch, unrecognized XML Element in test file");
            continue;
        }
        const UnicodeString *id       = testCase->getAttribute("id");
        *testId = 0;
        if (id != NULL) {
            id->extract(0, id->length(), testId,  sizeof(testId), US_INV);
        }

        // If debugging test case has been specified and this is not it, skip to next.
        if (id!=NULL && debugTestCase!=NULL && *id != *debugTestCase) {
            continue;
        }
        //
        //  Get the requested collation strength.
        //    Default is tertiary if the XML attribute is missing from the test case.
        //
        const UnicodeString *strength = testCase->getAttribute("strength");
        UColAttributeValue collatorStrength;
        if      (strength==NULL)          { collatorStrength = UCOL_TERTIARY;}
        else if (*strength=="PRIMARY")    { collatorStrength = UCOL_PRIMARY;}
        else if (*strength=="SECONDARY")  { collatorStrength = UCOL_SECONDARY;}
        else if (*strength=="TERTIARY")   { collatorStrength = UCOL_TERTIARY;}
        else if (*strength=="QUATERNARY") { collatorStrength = UCOL_QUATERNARY;}
        else if (*strength=="IDENTICAL")  { collatorStrength = UCOL_IDENTICAL;}
        else {
            // Bogus value supplied for strength.  Shouldn't happen, even from
            //  typos, if the  XML source has been validated.
            //  This assert is a little deceiving in that strength can be
            //   any of the allowed values, not just TERTIARY, but it will
            //   do the job of getting the error output.
            TEST_ASSERT(*strength=="TERTIARY")
        }

        //
        // Get the collator normalization flag.  Default is UCOL_OFF.
        //
        UColAttributeValue normalize = UCOL_OFF;
        const UnicodeString *norm = testCase->getAttribute("norm");
        TEST_ASSERT (norm==NULL || *norm=="ON" || *norm=="OFF");
        if (norm!=NULL && *norm=="ON") {
            normalize = UCOL_ON;
        }

        const UnicodeString defLocale("en");
        char  clocale[100];
        const UnicodeString *locale   = testCase->getAttribute("locale");
        if (locale == NULL || locale->length()==0) {
            locale = &defLocale;
        };
        locale->extract(0, locale->length(), clocale, sizeof(clocale), NULL);


        UnicodeString  text;
        UnicodeString  target;
        UnicodeString  pattern;
        int32_t        expectedMatchStart = -1;
        int32_t        expectedMatchLimit = -1;
        const UXMLElement  *n;
        int                nodeCount = 0;

        n = testCase->getChildElement("pattern");
        TEST_ASSERT(n != NULL);
        if (n==NULL) {
            continue;
        }
        text = n->getText(FALSE);
        text = text.unescape();
        pattern.append(text);
        nodeCount++;

        n = testCase->getChildElement("pre");
        if (n!=NULL) {
            text = n->getText(FALSE);
            text = text.unescape();
            target.append(text);
            nodeCount++;
        }
        
        n = testCase->getChildElement("m");
        if (n!=NULL) {
            expectedMatchStart = target.length();
            text = n->getText(FALSE);
            text = text.unescape();
            target.append(text);
            expectedMatchLimit = target.length();
            nodeCount++;
        }

        n = testCase->getChildElement("post");
        if (n!=NULL) {
            text = n->getText(FALSE);
            text = text.unescape();
            target.append(text);
            nodeCount++;
        }

        //  Check that there weren't extra things in the XML
        TEST_ASSERT(nodeCount == testCase->countChildren());

        // Open a collotor and StringSearch based on the parameters
        //   obtained from the XML.
        //
        status = U_ZERO_ERROR;
        UCollator *collator = ucol_open(clocale, &status);
        ucol_setStrength(collator, collatorStrength);
        ucol_setAttribute(collator, UCOL_NORMALIZATION_MODE, normalize, &status);
        UStringSearch *uss = usearch_openFromCollator(pattern.getBuffer(), pattern.length(),
                                         target.getBuffer(), target.length(),
                                         collator,
                                         NULL,     // the break iterator
                                         &status);
                                         
        TEST_ASSERT_SUCCESS(status);
        if (U_FAILURE(status)) {
            usearch_close(uss);
            ucol_close(collator);
            continue;
        }

        int32_t foundStart = 0;
        int32_t foundLimit = 0;
        UBool   foundMatch;

        //
        // Do the search, check the match result against the expected results.
        //
        foundMatch= usearch_search(uss, 0, &foundStart, &foundLimit, &status);
        TEST_ASSERT_SUCCESS(status);
        if (foundMatch && expectedMatchStart<0 ||
            foundStart != expectedMatchStart   ||
            foundLimit != expectedMatchLimit) {
                TEST_ASSERT(FALSE);   //  ouput generic error position
                infoln("Found, expected match start = %d, %d \n"
                       "Found, expected match limit = %d, %d",
                foundStart, expectedMatchStart, foundLimit, expectedMatchLimit);
        }

        // In case there are other matches...
        // (should we only do this if the test case passed?)
        while (foundMatch) {
            expectedMatchStart = foundStart;
            expectedMatchLimit = foundLimit;

            foundMatch = usearch_search(uss, foundLimit, &foundStart, &foundLimit, &status);
        }

        usearch_close(uss);
       
        uss = usearch_openFromCollator(pattern.getBuffer(), pattern.length(),
            target.getBuffer(), target.length(),
            collator,
            NULL,
            &status);

        //
        // Do the backwards search, check the match result against the expected results.
        //
        foundMatch= usearch_searchBackwards(uss, target.length(), &foundStart, &foundLimit, &status);
        TEST_ASSERT_SUCCESS(status);
        if (foundMatch && expectedMatchStart<0 ||
            foundStart != expectedMatchStart   ||
            foundLimit != expectedMatchLimit) {
                TEST_ASSERT(FALSE);   //  ouput generic error position
                infoln("Found, expected backwards match start = %d, %d \n"
                       "Found, expected backwards match limit = %d, %d",
                foundStart, expectedMatchStart, foundLimit, expectedMatchLimit);
        }

        usearch_close(uss);
        ucol_close(collator);
    }

    delete root;
    delete parser;
#endif
}

struct Order
{
    int32_t order;
    int32_t lowOffset;
    int32_t highOffset;
};

class OrderList
{
public:
    OrderList();
    OrderList(UCollator *coll, const UnicodeString &string, int32_t stringOffset = 0);
    ~OrderList();

    int32_t size(void) const;
    void add(int32_t order, int32_t low, int32_t high);
    const Order *get(int32_t index) const;
    int32_t getLowOffset(int32_t index) const;
    int32_t getHighOffset(int32_t index) const;
    int32_t getOrder(int32_t index) const;
    void reverse(void);
    UBool compare(const OrderList &other) const;
    UBool matchesAt(int32_t offset, const OrderList &other) const;

private:
    Order *list;
    int32_t listMax;
    int32_t listSize;
};

OrderList::OrderList()
  : list(NULL), listSize(0), listMax(16)
{
    list = new Order[listMax];
}

OrderList::OrderList(UCollator *coll, const UnicodeString &string, int32_t stringOffset)
    : list(NULL), listMax(16), listSize(0)
{
    UErrorCode status = U_ZERO_ERROR;
    UCollationElements *elems = ucol_openElements(coll, string.getBuffer(), string.length(), &status);
    uint32_t strengthMask = 0;
    int32_t order, low, high;

    switch (ucol_getStrength(coll)) 
    {
    default:
        strengthMask |= UCOL_TERTIARYORDERMASK;
        /* fall through */

    case UCOL_SECONDARY:
        strengthMask |= UCOL_SECONDARYORDERMASK;
        /* fall through */

    case UCOL_PRIMARY:
        strengthMask |= UCOL_PRIMARYORDERMASK;
    }

    list = new Order[listMax];

    ucol_setOffset(elems, stringOffset, &status);

    do {
        low   = ucol_getOffset(elems);
        order = ucol_next(elems, &status);
        high  = ucol_getOffset(elems);

        if (order != UCOL_NULLORDER) {
            order &= strengthMask;
        }

        if (order != UCOL_IGNORABLE) {
            add(order, low, high);
        }
    } while (order != UCOL_NULLORDER);

    ucol_closeElements(elems);
}

OrderList::~OrderList()
{
    delete[] list;
}

void OrderList::add(int32_t order, int32_t low, int32_t high)
{
    if (listSize >= listMax) {
        listMax *= 2;

        Order *newList = new Order[listMax];

        uprv_memcpy(newList, list, listSize * sizeof(Order));
        delete[] list;
        list = newList;
    }

    list[listSize].order      = order;
    list[listSize].lowOffset  = low;
    list[listSize].highOffset = high;

    listSize += 1;
}

const Order *OrderList::get(int32_t index) const
{
    if (index >= listSize) {
        return NULL;
    }

    return &list[index];
}

int32_t OrderList::getLowOffset(int32_t index) const
{
    const Order *order = get(index);

    if (order != NULL) {
        return order->lowOffset;
    }

    return -1;
}

int32_t OrderList::getHighOffset(int32_t index) const
{
    const Order *order = get(index);

    if (order != NULL) {
        return order->highOffset;
    }

    return -1;
}

int32_t OrderList::getOrder(int32_t index) const
{
    const Order *order = get(index);

    if (order != NULL) {
        return order->order;
    }

    return UCOL_NULLORDER;
}

int32_t OrderList::size() const
{
    return listSize;
}

void OrderList::reverse()
{
    for(int32_t f = 0, b = listSize - 1; f < b; f += 1, b -= 1) {
        Order swap = list[b];

        list[b] = list[f];
        list[f] = swap;
    }
}

UBool OrderList::compare(const OrderList &other) const
{
    if (listSize != other.listSize) {
        return FALSE;
    }

    for(int32_t i = 0; i < listSize; i += 1) {
        if (list[i].order  != other.list[i].order ||
            list[i].lowOffset != other.list[i].lowOffset ||
            list[i].highOffset != other.list[i].highOffset) {
                return FALSE;
        }
    }

    return TRUE;
}

UBool OrderList::matchesAt(int32_t offset, const OrderList &other) const
{
    // NOTE: sizes include the NULLORDER, which we don't want to compare.
    int32_t otherSize = other.size() - 1;

    if (listSize - 1 - offset < otherSize) {
        return FALSE;
    }

    for (int32_t i = offset, j = 0; j < otherSize; i += 1, j += 1) {
        if (getOrder(i) != other.getOrder(j)) {
            return FALSE;
        }
    }

    return TRUE;
}

static char *printOffsets(char *buffer, OrderList &list)
{
    int32_t size = list.size();
    char *s = buffer;

    for(int32_t i = 0; i < size; i += 1) {
        const Order *order = list.get(i);

        if (i != 0) {
            s += sprintf(s, ", ");
        }

        s += sprintf(s, "(%d, %d)", order->lowOffset, order->highOffset);
    }

    return buffer;
}

static char *printOrders(char *buffer, OrderList &list)
{
    int32_t size = list.size();
    char *s = buffer;

    for(int32_t i = 0; i < size; i += 1) {
        const Order *order = list.get(i);

        if (i != 0) {
            s += sprintf(s, ", ");
        }

        s += sprintf(s, "%8.8X", order->order);
    }

    return buffer;
}

void SSearchTest::offsetTest()
{
    const char *test[] = {
        "\\ua191\\u16ef\\u2036\\u017a",

#if 0
        // This results in a complex interaction between contraction,
        // expansion and normalization that confuses the backwards offset fixups.
        "\\u0F7F\\u0F80\\u0F81\\u0F82\\u0F83\\u0F84\\u0F85",
#endif

        "\\u0F80\\u0F81\\u0F82\\u0F83\\u0F84\\u0F85",
        "\\u07E9\\u07EA\\u07F1\\u07F2\\u07F3",

        "\\u02FE\\u02FF"
        "\\u0300\\u0301\\u0302\\u0303\\u0304\\u0305\\u0306\\u0307\\u0308\\u0309\\u030A\\u030B\\u030C\\u030D\\u030E\\u030F"
        "\\u0310\\u0311\\u0312\\u0313\\u0314\\u0315\\u0316\\u0317\\u0318\\u0319\\u031A\\u031B\\u031C\\u031D\\u031E\\u031F"
        "\\u0320\\u0321\\u0322\\u0323\\u0324\\u0325\\u0326\\u0327\\u0328\\u0329\\u032A\\u032B\\u032C\\u032D\\u032E\\u032F"
        "\\u0330\\u0331\\u0332\\u0333\\u0334\\u0335\\u0336\\u0337\\u0338\\u0339\\u033A\\u033B\\u033C\\u033D\\u033E\\u033F"
        "\\u0340\\u0341\\u0342\\u0343\\u0344\\u0345\\u0346\\u0347\\u0348\\u0349\\u034A\\u034B\\u034C\\u034D\\u034E",

        "\\u02FE\\u02FF\\u0300\\u0301\\u0302\\u0303\\u0316\\u0317\\u0318",
        "abc\\u0E41\\u0301\\u0316",
		"abc\\u0E41\\u0316\\u0301",
		"\\u0E41\\u0301\\u0316",
		"\\u0E41\\u0316\\u0301",
		"a\\u0301\\u0316",
		"a\\u0316\\u0301",
		"\\uAC52\\uAC53",
		"\\u34CA\\u34CB",
		"\\u11ED\\u11EE",
		"\\u30C3\\u30D0",
		"p\\u00E9ch\\u00E9",
        "a\\u0301\\u0325",
        "a\\u0300\\u0325",
        "a\\u0325\\u0300",
        "A\\u0323\\u0300B",
        "A\\u0300\\u0323B",
        "A\\u0301\\u0323B",
        "A\\u0302\\u0301\\u0323B",
        "abc",
        "ab\\u0300c",
        "ab\\u0300\\u0323c",
        " \\uD800\\uDC00\\uDC00",
        "a\\uD800\\uDC00\\uDC00",
        "A\\u0301\\u0301",
        "A\\u0301\\u0323",
        "A\\u0301\\u0323B",
        "B\\u0301\\u0323C",
        "A\\u0300\\u0323B",
        "\\u0301A\\u0301\\u0301",
        "abcd\\r\\u0301",
        "p\\u00EAche",
        "pe\\u0302che",
    };

    int32_t testCount = ARRAY_SIZE(test);
    UErrorCode status = U_ZERO_ERROR;
    RuleBasedCollator *col = (RuleBasedCollator *) Collator::createInstance(Locale::getEnglish(), status);
    if (U_FAILURE(status)) {
        errln("Failed to create collator in offsetTest!");
        return;
    }
    char buffer[4096];  // A bit of a hack... just happens to be long enough for all the test cases...
                        // We could allocate one that's the right size by (CE_count * 10) + 2
                        // 10 chars is enough room for 8 hex digits plus ", ". 2 extra chars for "[" and "]"

    col->setAttribute(UCOL_NORMALIZATION_MODE, UCOL_ON, status);

    for(int32_t i = 0; i < testCount; i += 1) {
        UnicodeString ts = CharsToUnicodeString(test[i]);
        CollationElementIterator *iter = col->createCollationElementIterator(ts);
        OrderList forwardList;
        OrderList backwardList;
        int32_t order, low, high;

        do {
            low   = iter->getOffset();
            order = iter->next(status);
            high  = iter->getOffset();

            forwardList.add(order, low, high);
        } while (order != CollationElementIterator::NULLORDER);

        iter->reset();
        iter->setOffset(ts.length(), status);

        backwardList.add(CollationElementIterator::NULLORDER, iter->getOffset(), iter->getOffset());

        do {
            high  = iter->getOffset();
            order = iter->previous(status);
            low   = iter->getOffset();

            if (order == CollationElementIterator::NULLORDER) {
                break;
            }

            backwardList.add(order, low, high);
        } while (TRUE);

        backwardList.reverse();

        if (forwardList.compare(backwardList)) {
            logln("Works with \"%s\"", test[i]);
            logln("Forward offsets:  [%s]", printOffsets(buffer, forwardList));
//          logln("Backward offsets: [%s]", printOffsets(buffer, backwardList));

            logln("Forward CEs:  [%s]", printOrders(buffer, forwardList));
//          logln("Backward CEs: [%s]", printOrders(buffer, backwardList));

            logln();
        } else {
            errln("Fails with \"%s\"", test[i]);
            infoln("Forward offsets:  [%s]", printOffsets(buffer, forwardList));
            infoln("Backward offsets: [%s]", printOffsets(buffer, backwardList));

            infoln("Forward CEs:  [%s]", printOrders(buffer, forwardList));
            infoln("Backward CEs: [%s]", printOrders(buffer, backwardList));

            infoln();
        }
        delete iter;
    }
    delete col;
}

class CEList
{
public:
    CEList(UCollator *coll, const UnicodeString &string);
    ~CEList();

    int32_t size() const;
    int32_t get(int32_t index) const;
    UBool matchesAt(int32_t offset, const CEList *other) const; 

private:
    void add(int32_t ce);

    int32_t *ces;
    int32_t listMax;
    int32_t listSize;
};

CEList::CEList(UCollator *coll, const UnicodeString &string)
    : ces(NULL), listMax(8), listSize(0)
{
    UErrorCode status = U_ZERO_ERROR;
    UCollationElements *elems = ucol_openElements(coll, string.getBuffer(), string.length(), &status);
    uint32_t strengthMask = 0;
    int32_t order;

#if 0
    switch (ucol_getStrength(coll)) 
    {
    default:
        strengthMask |= UCOL_TERTIARYORDERMASK;
        /* fall through */

    case UCOL_SECONDARY:
        strengthMask |= UCOL_SECONDARYORDERMASK;
        /* fall through */

    case UCOL_PRIMARY:
        strengthMask |= UCOL_PRIMARYORDERMASK;
    }
#else
    strengthMask = UCOL_PRIMARYORDERMASK;
#endif

    ces = new int32_t[listMax];

    while ((order = ucol_next(elems, &status)) != UCOL_NULLORDER) {
        order &= strengthMask;

        if (order == UCOL_IGNORABLE) {
            continue;
        }

        add(order);
    }

    ucol_closeElements(elems);
}

CEList::~CEList()
{
    delete[] ces;
}

void CEList::add(int32_t ce)
{
    if (listSize >= listMax) {
        listMax *= 2;

        int32_t *newCEs = new int32_t[listMax];

        uprv_memcpy(newCEs, ces, listSize * sizeof(int32_t));
        delete[] ces;
        ces = newCEs;
    }

    ces[listSize++] = ce;
}

int32_t CEList::get(int32_t index) const
{
    if (index >= 0 && index < listSize) {
        return ces[index];
    }

    return -1;
}

UBool CEList::matchesAt(int32_t offset, const CEList *other) const
{
    if (listSize - offset < other->size()) {
        return FALSE;
    }

    for (int32_t i = offset, j = 0; j < other->size(); i += 1, j += 1) {
        if (ces[i] != other->get(j)) {
            return FALSE;
        }
    }

    return TRUE;
}

int32_t CEList::size() const
{
    return listSize;
}

class StringList
{
public:
    StringList();
    ~StringList();

    void add(const UnicodeString *string);
    void add(const UChar *chars, int32_t count);
    const UnicodeString *get(int32_t index) const;
    int32_t size() const;

private:
    UnicodeString *strings;
    int32_t listMax;
    int32_t listSize;
};

StringList::StringList()
    : strings(NULL), listMax(16), listSize(0)
{
    strings = new UnicodeString [listMax];
}

StringList::~StringList()
{
    delete[] strings;
}

void StringList::add(const UnicodeString *string)
{
    if (listSize >= listMax) {
        listMax *= 2;

        UnicodeString *newStrings = new UnicodeString[listMax];

        uprv_memcpy(newStrings, strings, listSize * sizeof(UnicodeString));

        delete[] strings;
        strings = newStrings;
    }

    // The ctor initialized all the strings in
    // the array to empty strings, so this
    // is the same as copying the source string.
    strings[listSize++].append(*string);
}

void StringList::add(const UChar *chars, int32_t count)
{
    const UnicodeString string(chars, count);

    add(&string);
}

const UnicodeString *StringList::get(int32_t index) const
{
    if (index >= 0 && index < listSize) {
        return &strings[index];
    }

    return NULL;
}

int32_t StringList::size() const
{
    return listSize;
}

class CEToStringsMap
{
public:

    CEToStringsMap();
    ~CEToStringsMap();

    void put(int32_t ce, UnicodeString *string);
    StringList *getStringList(int32_t ce) const;

private:
 
    static void deleteStringList(void *obj);
    void putStringList(int32_t ce, StringList *stringList);
    UHashtable *map;
};

CEToStringsMap::CEToStringsMap()
{
    UErrorCode status = U_ZERO_ERROR;

    map = uhash_open(uhash_hashLong, uhash_compareLong,
                     uhash_compareCaselessUnicodeString,
                     &status);

    uhash_setValueDeleter(map, deleteStringList);
}

CEToStringsMap::~CEToStringsMap()
{
    uhash_close(map);
}

void CEToStringsMap::put(int32_t ce, UnicodeString *string)
{
    StringList *strings = getStringList(ce);

    if (strings == NULL) {
        strings = new StringList();
        putStringList(ce, strings);
    }

    strings->add(string);
}

StringList *CEToStringsMap::getStringList(int32_t ce) const
{
    return (StringList *) uhash_iget(map, ce);
}

void CEToStringsMap::putStringList(int32_t ce, StringList *stringList)
{
    UErrorCode status = U_ZERO_ERROR;

    uhash_iput(map, ce, (void *) stringList, &status);
}

void CEToStringsMap::deleteStringList(void *obj)
{
    StringList *strings = (StringList *) obj;

    delete strings;
}

class StringToCEsMap
{
public:
    StringToCEsMap();
    ~StringToCEsMap();

    void put(const UnicodeString *string, const CEList *ces);
    const CEList *get(const UnicodeString *string);

private:

    static void deleteCEList(void *obj);
    static void deleteUnicodeStringKey(void *obj);

    UHashtable *map;
};

StringToCEsMap::StringToCEsMap()
{
    UErrorCode status = U_ZERO_ERROR;

    map = uhash_open(uhash_hashCaselessUnicodeString,
                     uhash_compareCaselessUnicodeString,
                     uhash_compareLong,
                     &status);

    uhash_setValueDeleter(map, deleteCEList);
    uhash_setKeyDeleter(map, deleteUnicodeStringKey);
}

StringToCEsMap::~StringToCEsMap()
{
    uhash_close(map);
}

void StringToCEsMap::put(const UnicodeString *string, const CEList *ces)
{
    UErrorCode status = U_ZERO_ERROR;

    uhash_put(map, (void *) string, (void *) ces, &status);
}

const CEList *StringToCEsMap::get(const UnicodeString *string)
{
    return (const CEList *) uhash_get(map, string);
}

void StringToCEsMap::deleteCEList(void *obj)
{
    CEList *list = (CEList *) obj;

    delete list;
}

void StringToCEsMap::deleteUnicodeStringKey(void *obj)
{
    UnicodeString *key = (UnicodeString *) obj;

    delete key;
}

static void buildData(UCollator *coll, USet *charsToTest, StringToCEsMap *charsToCEList, CEToStringsMap *ceToCharsStartingWith)
{
    int32_t itemCount = uset_getItemCount(charsToTest);
    UErrorCode status = U_ZERO_ERROR;

    for(int32_t item = 0; item < itemCount; item += 1) {
        UChar32 start = 0, end = 0;
        UChar buffer[16];
        int32_t len = uset_getItem(charsToTest, item, &start, &end,
                                   buffer, 16, &status);

        if (len == 0) {
            for (UChar32 ch = start; ch <= end; ch += 1) {
                UnicodeString *st = new UnicodeString(ch);
                CEList *ceList = new CEList(coll, *st);

                charsToCEList->put(st, ceList);
                ceToCharsStartingWith->put(ceList->get(0), st);
            }
        } else if (len > 0) {
            UnicodeString *st = new UnicodeString(buffer, len);
            CEList *ceList = new CEList(coll, *st);

            charsToCEList->put(st, ceList);
            ceToCharsStartingWith->put(ceList->get(0), st);
        } else {
            // shouldn't happen...
        }
    }
}

static UnicodeString &escape(const UnicodeString &string, UnicodeString &buffer)
{
    for(int32_t i = 0; i < string.length(); i += 1) {
        UChar32 ch = string.char32At(i);

        if (ch >= 0x0020 && ch <= 0x007F) {
            if (ch == 0x005C) {
                buffer.append("\\\\");
            } else {
                buffer.append(ch);
            }
        } else {
            char cbuffer[12];

            if (ch <= 0xFFFFL) {
                sprintf(cbuffer, "\\u%4.4X", ch);
            } else {
                sprintf(cbuffer, "\\U%8.8X", ch);
            }

            buffer.append(cbuffer);
        }

        if (ch >= 0x10000L) {
            i += 1;
        }
    }

    return buffer;
}

static int32_t minLengthInChars(const CEList *ceList, int32_t offset, StringToCEsMap *charsToCEList, CEToStringsMap *ceToCharsStartingWith,
                                UnicodeString &debug)
{
    // find out shortest string for the longest sequence of ces.
    // needs to be refined to use dynamic programming, but will be roughly right
	int32_t totalStringLength = 0;
	
    while (offset < ceList->size()) {
        int32_t ce = ceList->get(offset);
        int32_t bestLength = INT32_MIN;
        const UnicodeString *bestString = NULL;
        int32_t bestCeLength = 0;
        const StringList *strings = ceToCharsStartingWith->getStringList(ce);
        int32_t stringCount = strings->size();
      
        for (int32_t s = 0; s < stringCount; s += 1) {
            const UnicodeString *string = strings->get(s);
            const CEList *ceList2 = charsToCEList->get(string);

            if (ceList->matchesAt(offset, ceList2)) {
                int32_t length = ceList2->size() - string->length();

                if (bestLength < length) {
                    bestLength = length;
                    bestCeLength = ceList2->size();
                    bestString = string;
                }
            }
        }
      
        totalStringLength += bestString->length();
        escape(*bestString, debug).append("/");
        offset += bestCeLength;
    }

    debug.append((UChar)0x0000);
    return totalStringLength;
}

static void minLengthTest(UCollator *coll, StringToCEsMap *charsToCEList, CEToStringsMap *ceToCharsStartingWith)
{
    UnicodeString examples[] = {"fuss", "fiss", "affliss", "VII"};
    UnicodeString debug;
    int32_t nExamples = sizeof(examples) / sizeof(examples[0]);

    for (int32_t s = 0; s < nExamples; s += 1) {
        CEList *ceList = new CEList(coll, examples[s]);

      //infoln("%S:", examples[s].getTerminatedBuffer());

        for(int32_t i = 0; i < examples[s].length(); i += 1) {
            debug.remove();

            int32_t minLength = minLengthInChars(ceList, i, charsToCEList, ceToCharsStartingWith, debug);
          //infoln("\t%d\t%S", minLength, debug.getTerminatedBuffer());
        }

      //infoln();
        delete ceList;
    }
}

//----------------------------------------------------------------------------------------
//
//   Random Numbers.  Similar to standard lib rand() and srand()
//                    Not using library to
//                      1.  Get same results on all platforms.
//                      2.  Get access to current seed, to more easily reproduce failures.
//
//---------------------------------------------------------------------------------------
static uint32_t m_seed = 1;

static uint32_t m_rand()
{
    m_seed = m_seed * 1103515245 + 12345;
    return (uint32_t)(m_seed/65536) % 32768;
}

class Monkey
{
public:
    virtual void append(UnicodeString &test, UnicodeString &alternate) = 0;

protected:
    Monkey();
    virtual ~Monkey();
};

Monkey::Monkey()
{
    // ook?
}

Monkey::~Monkey()
{
    // ook?
}

class SetMonkey : public Monkey
{
public:
    SetMonkey(const USet *theSet);
    ~SetMonkey();

    virtual void append(UnicodeString &test, UnicodeString &alternate);

private:
    const USet *set;
};

SetMonkey::SetMonkey(const USet *theSet)
    : Monkey(), set(theSet)
{
    // ook?
}

SetMonkey::~SetMonkey()
{
    //ook...
}

void SetMonkey::append(UnicodeString &test, UnicodeString &alternate)
{
    int32_t size = uset_size(set);
    int32_t index = m_rand() % size;
    UChar32 ch = uset_charAt(set, index);
    UnicodeString str(ch);

    test.append(str);
    alternate.append(str); // flip case, or some junk?
}

class StringSetMonkey : public Monkey
{
public:
    StringSetMonkey(const USet *theSet, UCollator *theCollator, StringToCEsMap *theCharsToCEList, CEToStringsMap *theCeToCharsStartingWith);
    ~StringSetMonkey();

    void append(UnicodeString &testCase, UnicodeString &alternate);

private:
    UnicodeString &generateAlternative(const UnicodeString &testCase, UnicodeString &alternate);

    const USet *set;
    UCollator      *coll;
    StringToCEsMap *charsToCEList;
    CEToStringsMap *ceToCharsStartingWith;
};

StringSetMonkey::StringSetMonkey(const USet *theSet, UCollator *theCollator, StringToCEsMap *theCharsToCEList, CEToStringsMap *theCeToCharsStartingWith)
: Monkey(), set(theSet), coll(theCollator), charsToCEList(theCharsToCEList), ceToCharsStartingWith(theCeToCharsStartingWith)
{
    // ook.
}

StringSetMonkey::~StringSetMonkey()
{
    // ook?
}

void StringSetMonkey::append(UnicodeString &testCase, UnicodeString &alternate)
{
    int32_t itemCount = uset_getItemCount(set), len = 0;
    int32_t index = m_rand() % itemCount;
    UChar32 rangeStart = 0, rangeEnd = 0;
    UChar buffer[16];
    UErrorCode err = U_ZERO_ERROR;

    len = uset_getItem(set, index, &rangeStart, &rangeEnd, buffer, 16, &err);

    if (len == 0) {
        int32_t offset = m_rand() % (rangeEnd - rangeStart + 1);
        UChar32 ch = rangeStart + offset;
        UnicodeString str(ch);

        testCase.append(str);
        generateAlternative(str, alternate);
    } else if (len > 0) {
        // should check that len < 16...
        UnicodeString str(buffer, len);

        testCase.append(str);
        generateAlternative(str, alternate);
    } else {
        // shouldn't happen...
    }
}

UnicodeString &StringSetMonkey::generateAlternative(const UnicodeString &testCase, UnicodeString &alternate)
{
    // find out shortest string for the longest sequence of ces.
    // needs to be refined to use dynamic programming, but will be roughly right
    CEList ceList(coll, testCase);
    UnicodeString alt;
    int32_t offset = 0;

    if (ceList.size() == 0) {
        return alternate.append(testCase);
    }

    while (offset < ceList.size()) {
        int32_t ce = ceList.get(offset);
        const StringList *strings = ceToCharsStartingWith->getStringList(ce);

        if (strings == NULL) {
            return alternate.append(testCase);
        }

        int32_t stringCount = strings->size();
        int32_t tries = 0;
      
        // find random string that generates the same CEList
        const CEList *ceList2;
        const UnicodeString *string;

        do {
            int32_t s = m_rand() % stringCount;

            if (tries++ > stringCount) {
                alternate.append(testCase);
                return alternate;
            }

            string = strings->get(s);
            ceList2 = charsToCEList->get(string);
        } while (! ceList.matchesAt(offset, ceList2));

        alt.append(*string);
        offset += ceList2->size();
    }

    const CEList altCEs(coll, alt);

    if (ceList.matchesAt(0, &altCEs)) {
        return alternate.append(alt);
    }

    return alternate.append(testCase);
}

static void generateTestCase(UCollator *coll, Monkey *monkeys[], int32_t monkeyCount, UnicodeString &testCase, UnicodeString &alternate)
{
    int32_t pieces = (m_rand() % 4) + 1;
    UBool matches;

    do {
        testCase.remove();
        alternate.remove();
        monkeys[0]->append(testCase, alternate);

        for(int32_t piece = 0; piece < pieces; piece += 1) {
            int32_t monkey = m_rand() % monkeyCount;

            monkeys[monkey]->append(testCase, alternate);
        }

        const CEList ceTest(coll, testCase);
        const CEList ceAlt(coll, alternate);

        matches = ceTest.matchesAt(0, &ceAlt);
    } while (! matches);
}

static inline USet *uset_openEmpty()
{
    return uset_open(1, 0);
}

//
//  Find the next acceptable boundary following the specified starting index
//     in the target text being searched.
//      TODO:  refine what is an acceptable boundary.  For the moment,
//             choose the next position not within a combining sequence.
//
static int32_t nextBoundaryAfter(const UnicodeString &string, int32_t startIndex) {
    const UChar *text = string.getBuffer();
    int32_t textLen   = string.length();
    
    if (startIndex >= textLen) {
        return startIndex;
    }

    UChar32  c;
    int32_t  i = startIndex;

    U16_NEXT(text, i, textLen, c);
    
    // If we are on a control character, stop without looking for combining marks.
    //    Control characters do not combine.
    int32_t gcProperty = u_getIntPropertyValue(c, UCHAR_GRAPHEME_CLUSTER_BREAK);
    if (gcProperty==U_GCB_CONTROL || gcProperty==U_GCB_LF || gcProperty==U_GCB_CR) {
        return i;
    }
    
    // The initial character was not a control, and can thus accept trailing
    //   combining characters.  Advance over however many of them there are.
    int32_t  indexOfLastCharChecked;

    for (;;) {
        indexOfLastCharChecked = i;

        if (i>=textLen) {
            break;
        }

        U16_NEXT(text, i, textLen, c);
        gcProperty = u_getIntPropertyValue(c, UCHAR_GRAPHEME_CLUSTER_BREAK);

        if (gcProperty != U_GCB_EXTEND && gcProperty != U_GCB_SPACING_MARK) {
            break;
        }
    }

    return indexOfLastCharChecked;
}
 
static UBool isInCombiningSequence(const UnicodeString &string, int32_t index) {
    const UChar *text = string.getBuffer();
    int32_t textLen   = string.length();
    
    if (index>=textLen || index<=0) {
        return FALSE;
    }
  
    // If the character at the current index is not a GRAPHEME_EXTEND
    //    then we can not be within a combining sequence.
    UChar32  c;
    U16_GET(text, 0, index, textLen, c);
    int32_t gcProperty = u_getIntPropertyValue(c, UCHAR_GRAPHEME_CLUSTER_BREAK);
    if (gcProperty != U_GCB_EXTEND && gcProperty != U_GCB_SPACING_MARK) {
        return FALSE;
    }
    
    // We are at a combining mark.  If the preceding character is anything
    //   except a CONTROL, CR or LF, we are in a combining sequence.
    U16_PREV(text, 0, index, c);    
    gcProperty = u_getIntPropertyValue(c, UCHAR_GRAPHEME_CLUSTER_BREAK);

    return !(gcProperty==U_GCB_CONTROL || gcProperty==U_GCB_LF || gcProperty==U_GCB_CR);
}      
        
static UBool simpleSearch(UCollator *coll, const UnicodeString &target, int32_t offset, const UnicodeString &pattern, int32_t &matchStart, int32_t &matchEnd)
{
    UErrorCode      status = U_ZERO_ERROR;
    OrderList       targetOrders(coll, target, offset);
    OrderList       patternOrders(coll, pattern);
    int32_t         targetSize  = targetOrders.size() - 1;
    int32_t         patternSize = patternOrders.size() - 1;
    UBreakIterator *charBreakIterator = ubrk_open(UBRK_CHARACTER, ucol_getLocale(coll, ULOC_VALID_LOCALE, &status), 
        									      target.getBuffer(), target.length(), &status);

    if (patternSize == 0) {
        matchStart = matchEnd = 0;
        return FALSE;
    }

    matchStart = matchEnd = -1;

    for(int32_t i = 0; i < targetSize; i += 1) {
        if (targetOrders.matchesAt(i, patternOrders)) {
            int32_t start    = targetOrders.getLowOffset(i);
            int32_t maxLimit = targetOrders.getLowOffset(i + patternSize);
            int32_t minLimit = targetOrders.getLowOffset(i + patternSize - 1);

            // if the low and high offsets of the first CE in
            // the match are the same, it means that the match
            // starts in the middle of an expansion - all but
            // the first CE of the expansion will have the offset
            // of the following character.
            if (start == targetOrders.getHighOffset(i)) {
                continue;
            }

            // Make sure match starts on a grapheme boundary
            if (! ubrk_isBoundary(charBreakIterator, start)) {
                continue;
            }

            // If the low and high offsets of the CE after the match
            // are the same, it means that the match ends in the middle
            // of an expansion sequence.
            if (maxLimit == targetOrders.getHighOffset(i + patternSize) &&
                targetOrders.getOrder(i + patternSize) != UCOL_NULLORDER) {
                continue;
            }

            int32_t mend = maxLimit;

            // Find the first grapheme break after the character index
            // of the last CE in the match. If it's after character index
            // that's after the last CE in the match, use that index
            // as the end of the match.
            if (minLimit < maxLimit) {
                int32_t nba = ubrk_following(charBreakIterator, minLimit);

                if (nba >= targetOrders.getHighOffset(i + patternSize - 1)) {
                    mend = nba;
                }
            }

            if (mend > maxLimit) {
                continue;
            }

            if (! ubrk_isBoundary(charBreakIterator, mend)) {
                continue;
            }

            matchStart = start;
            matchEnd   = mend;

            ubrk_close(charBreakIterator);
            return TRUE;
        }
    }

    ubrk_close(charBreakIterator);
    return FALSE;
}

#if !UCONFIG_NO_REGULAR_EXPRESSIONS
static int32_t  getIntParam(UnicodeString name, UnicodeString &params, int32_t defaultVal) {
    int32_t val = defaultVal;

    name.append(" *= *(-?\\d+)");

    UErrorCode status = U_ZERO_ERROR;
    RegexMatcher m(name, params, 0, status);

    if (m.find()) {
        // The param exists.  Convert the string to an int.
        char valString[100];
        int32_t paramLength = m.end(1, status) - m.start(1, status);

        if (paramLength >= (int32_t)(sizeof(valString)-1)) {
            paramLength = (int32_t)(sizeof(valString)-2);
        }

        params.extract(m.start(1, status), paramLength, valString, sizeof(valString));
        val = strtol(valString,  NULL, 10);

        // Delete this parameter from the params string.
        m.reset();
        params = m.replaceFirst("", status);
    }

  //U_ASSERT(U_SUCCESS(status));
    if (! U_SUCCESS(status)) {
        val = defaultVal;
    }

    return val;
}
#endif

#if !UCONFIG_NO_COLLATION
int32_t SSearchTest::monkeyTestCase(UCollator *coll, const UnicodeString &testCase, const UnicodeString &pattern, const UnicodeString &altPattern,
                                    const char *name, const char *strength, uint32_t seed)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t actualStart = -1, actualEnd = -1;
  //int32_t expectedStart = prefix.length(), expectedEnd = prefix.length() + altPattern.length();
    int32_t expectedStart = -1, expectedEnd = -1;
    int32_t notFoundCount = 0;
    UStringSearch *uss = usearch_openFromCollator(pattern.getBuffer(), pattern.length(),
                                testCase.getBuffer(), testCase.length(),
                                coll,
                                NULL,     // the break iterator
                                &status);

    // **** TODO: find *all* matches, not just first one ****
    simpleSearch(coll, testCase, 0, pattern, expectedStart, expectedEnd);

#if 0
    usearch_search(uss, 0, &actualStart, &actualEnd, &status);
#else
    actualStart = usearch_next(uss, &status);
    actualEnd   = actualStart + usearch_getMatchedLength(uss);
#endif

    if (actualStart != expectedStart || actualEnd != expectedEnd) {
        errln("Search for <pattern> in <%s> failed: expected [%d, %d], got [%d, %d]\n"
              "    strength=%s seed=%d",
              name, expectedStart, expectedEnd, actualStart, actualEnd, strength, seed);
    }

    if (expectedStart == -1 && actualStart == -1) {
        notFoundCount += 1;
    }

    // **** TODO: find *all* matches, not just first one ****
    simpleSearch(coll, testCase, 0, altPattern, expectedStart, expectedEnd);

    usearch_setPattern(uss, altPattern.getBuffer(), altPattern.length(), &status);

#if 0
    usearch_search(uss, 0, &actualStart, &actualEnd, &status);
#else
    usearch_reset(uss);
    actualStart = usearch_next(uss, &status);
    actualEnd   = actualStart + usearch_getMatchedLength(uss);
#endif

    if (actualStart != expectedStart || actualEnd != expectedEnd) {
        errln("Search for <alt_pattern> in <%s> failed: expected [%d, %d], got [%d, %d]\n"
              "    strength=%s seed=%d",
              name, expectedStart, expectedEnd, actualStart, actualEnd, strength, seed);
    }

    if (expectedStart == -1 && actualStart == -1) {
        notFoundCount += 1;
    }

    usearch_close(uss);

    return notFoundCount;
}
#endif

void SSearchTest::monkeyTest(char *params)
{
    // ook!
    UErrorCode status = U_ZERO_ERROR;
    U_STRING_DECL(test_pattern, "[[:assigned:]-[:ideographic:]-[:hangul:]-[:c:]]", 47);
    U_STRING_INIT(test_pattern, "[[:assigned:]-[:ideographic:]-[:hangul:]-[:c:]]", 47);
    UCollator *coll = ucol_open(NULL, &status);
    if (U_FAILURE(status)) {
        errln("Failed to create collator in MonkeyTest!");
        return;
    }
    USet *charsToTest  = uset_openPattern(test_pattern, 47, &status);
    USet *expansions   = uset_openEmpty();
    USet *contractions = uset_openEmpty();
    StringToCEsMap *charsToCEList = new StringToCEsMap();
    CEToStringsMap *ceToCharsStartingWith = new CEToStringsMap();

    ucol_getContractionsAndExpansions(coll, contractions, expansions, FALSE, &status);

    uset_addAll(charsToTest, contractions);
    uset_addAll(charsToTest, expansions);

    // TODO: set strength to UCOL_PRIMARY, change CEList to use strength?
    buildData(coll, charsToTest, charsToCEList, ceToCharsStartingWith);

    U_STRING_DECL(letter_pattern, "[[:letter:]-[:ideographic:]-[:hangul:]]", 39);
    U_STRING_INIT(letter_pattern, "[[:letter:]-[:ideographic:]-[:hangul:]]", 39);
    USet *letters = uset_openPattern(letter_pattern, 39, &status);
    SetMonkey letterMonkey(letters);
    StringSetMonkey contractionMonkey(contractions, coll, charsToCEList, ceToCharsStartingWith);
    StringSetMonkey expansionMonkey(expansions, coll, charsToCEList, ceToCharsStartingWith);
    UnicodeString testCase;
    UnicodeString alternate;
    UnicodeString pattern, altPattern;
    UnicodeString prefix, altPrefix;
    UnicodeString suffix, altSuffix;

    Monkey *monkeys[] = {
        &letterMonkey,
        &contractionMonkey,
        &expansionMonkey,
        &contractionMonkey,
        &expansionMonkey,
        &contractionMonkey,
        &expansionMonkey,
        &contractionMonkey,
        &expansionMonkey};
    int32_t monkeyCount = sizeof(monkeys) / sizeof(monkeys[0]);
    int32_t nonMatchCount = 0;

    UCollationStrength strengths[] = {UCOL_PRIMARY, UCOL_SECONDARY, UCOL_TERTIARY};
    const char *strengthNames[] = {"primary", "secondary", "tertiary"};
    int32_t strengthCount = sizeof(strengths) / sizeof(strengths[0]);
    int32_t loopCount = quick? 1000 : 10000;
    int32_t firstStrength = 0;
    int32_t lastStrength  = strengthCount - 1;

    if (params != NULL) {
#if !UCONFIG_NO_REGULAR_EXPRESSIONS
        UnicodeString p(params);

        loopCount = getIntParam("loop", p, loopCount);
        m_seed    = getIntParam("seed", p, m_seed);

        RegexMatcher m(" *strength *= *(primary|secondary|tertiary) *", p, 0, status);
        if (m.find()) {
            UnicodeString breakType = m.group(1, status);

            for (int32_t s = 0; s < strengthCount; s += 1) {
                if (breakType == strengthNames[s]) {
                    firstStrength = lastStrength = s;
                    break;
                }
            }

            m.reset();
            p = m.replaceFirst("", status);
        }

        if (RegexMatcher("\\S", p, 0, status).find()) {
            // Each option is stripped out of the option string as it is processed.
            // All options have been checked.  The option string should have been completely emptied..
            char buf[100];
            p.extract(buf, sizeof(buf), NULL, status);
            buf[sizeof(buf)-1] = 0;
            errln("Unrecognized or extra parameter:  %s\n", buf);
            return;
        }
#else
        infoln("SSearchTest built with UCONFIG_NO_REGULAR_EXPRESSIONS: ignoring parameters.");
#endif
    }

    for(int32_t s = firstStrength; s <= lastStrength; s += 1) {
        int32_t notFoundCount = 0;

        ucol_setStrength(coll, strengths[s]);

        // TODO: try alternate prefix and suffix too?
        // TODO: alterntaes are only equal at primary strength. Is this OK?
        for(int32_t t = 0; t < 10000; t += 1) {
            uint32_t seed = m_seed;
            int32_t  nmc = 0;

            generateTestCase(coll, monkeys, monkeyCount, pattern, altPattern);
            generateTestCase(coll, monkeys, monkeyCount, prefix,  altPrefix);
            generateTestCase(coll, monkeys, monkeyCount, suffix,  altSuffix);

            // pattern
            notFoundCount += monkeyTestCase(coll, pattern, pattern, altPattern, "pattern", strengthNames[s], seed);

            testCase.remove();
            testCase.append(prefix);
            testCase.append(/*alt*/pattern);

            // prefix + pattern
            notFoundCount += monkeyTestCase(coll, testCase, pattern, altPattern, "prefix + pattern", strengthNames[s], seed);

            testCase.append(suffix);

            // prefix + pattern + suffix
            notFoundCount += monkeyTestCase(coll, testCase, pattern, altPattern, "prefix + pattern + suffix", strengthNames[s], seed);

            testCase.remove();
            testCase.append(pattern);
            testCase.append(suffix);
            
            // pattern + suffix
            notFoundCount += monkeyTestCase(coll, testCase, pattern, altPattern, "pattern + suffix", strengthNames[s], seed);
        }

        logln("For strength %s the not found count is %d.", strengthNames[s], notFoundCount);
    }

    delete ceToCharsStartingWith;
    delete charsToCEList;

    uset_close(contractions);
    uset_close(expansions);
    uset_close(charsToTest);
    uset_close(letters);
    
    ucol_close(coll);
}

#endif        
        
#endif
