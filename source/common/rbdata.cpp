/*
 **********************************************************************
 *   Copyright (C) 1998-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*
* File rbdata.cpp
*
* Modification History:
*
*   Date        Name        Description
*   06/11/99    stephen     Creation. (Moved here from resbund.cpp)
*******************************************************************************
*/

#include "rbdata.h"

UClassID StringList::fgClassID = 0; // Value is irrelevant
UClassID String2dList::fgClassID = 0; // Value is irrelevant
UClassID TaggedList::fgClassID = 0; // Value is irrelevant

//-----------------------------------------------------------------------------

StringList::StringList()
  : fStrings(0), fCount(0) 
{}

StringList::StringList(UnicodeString *adopted, 
		       int32_t count) 
  : fStrings(adopted), fCount(count) 
{}

StringList::~StringList() 
{ delete [] fStrings; }
  
const UnicodeString& 
StringList::operator[](int32_t i) const 
{ return fStrings[i]; }

UClassID 
StringList::getDynamicClassID() const 
{ return getStaticClassID(); }

UClassID 
StringList::getStaticClassID() 
{ return (UClassID)&fgClassID; }

//-----------------------------------------------------------------------------

String2dList::String2dList() 
  : fStrings(0), fRowCount(0), fColCount(0) 
{}
  
String2dList::String2dList(UnicodeString **adopted, 
			   int32_t rowCount, 
			   int32_t colCount) 
  : fStrings(adopted), fRowCount(rowCount), fColCount(colCount) 
{}

String2dList::~String2dList() 
{ 
  for(int32_t i = 0; i < fRowCount; ++i) {
    delete[] fStrings[i]; 
  }
}
  
const UnicodeString& 
String2dList::getString(int32_t rowIndex, 
			int32_t colIndex) 
{ return fStrings[rowIndex][colIndex]; }
  
UClassID 
String2dList::getDynamicClassID() const 
{ return getStaticClassID(); }

UClassID 
String2dList::getStaticClassID() 
{ return (UClassID)&fgClassID; }

//-----------------------------------------------------------------------------

TaggedList::TaggedList() {
    UErrorCode status = U_ZERO_ERROR;
    hash = new Hashtable(status);
    hash->setValueDeleter(uhash_deleteUnicodeString);
}
  
TaggedList::~TaggedList() {
    delete hash;
}

int32_t TaggedList::count() const {
    return hash->count();
}

void 
TaggedList::put(const UnicodeString& tag, 
		const UnicodeString& data) {
    UErrorCode status = U_ZERO_ERROR;
    hash->put(tag, new UnicodeString(data), status);
}

const UnicodeString* 
TaggedList::get(const UnicodeString& tag) const {
    return (const UnicodeString*) hash->get(tag);
}

UBool TaggedList::nextElement(const UnicodeString*& key,
                               const UnicodeString*& value,
                               int32_t& pos) const {
    const UHashElement *e = hash->nextElement(pos);
    if (e != NULL) {
        key   = (const UnicodeString*) e->key;
        value = (const UnicodeString*) e->value;
        return TRUE;
    } else {
        return FALSE;
    }
}

UClassID 
TaggedList::getDynamicClassID() const 
{ return getStaticClassID(); }

UClassID 
TaggedList::getStaticClassID() 
{ return (UClassID)&fgClassID; }
