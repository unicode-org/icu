/*
*******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//===============================================================================
//
// File tables.cpp
//
// Contains:
//  EntryPair               - Represents a contracting-character string
//  PointerToPatternEntry   - a smart pointer to a PatternEntry
//
//  VectorOfInt             - Dynamic array classes, in lieu of templates
//  VectorOfPointer 
//  VectorOfPToExpandTable
//  VectorOfPToContractElement
//  VectorOfPointersToPatternEntry
//              
// All of these classes are fairly small and self-explanatory, so they don't
// contain too many internal comments.
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
//  2/5/97      aliu        Added streamIn and streamOut methods to EntryPair,
//                          VectorOfInt, VectorOfPToExpandTable, VectorOfPToContractElement,
//                          VectorOfPToContractTable.  These are used by TableCollation
//                          streamIn and streamOut methods.
//  2/11/97     aliu        Moved declarations out of for loop initializer.
//  3/5/97      aliu        Made VectorOfPointersToPatternEntry::at() inline.
//  6/18/97     helena      Added VectorOfPointer for MergeCollation.
//  6/23/97     helena      Added comments to make code more readable.  Since 
//                          this is converted from a templatized version, the comment
//                          is only added to one class.
//  8/04/98        erm            Added EntryPair::fwd.
//===============================================================================
#ifndef _TABLES
#include "tables.h"
#endif

#ifndef _PTNENTRY
#include "ptnentry.h"
#endif

#ifndef _FILESTRM
#include "filestrm.h"
#endif

#ifndef _UNISTRM
#include "unistrm.h"
#endif

//=======================================================================================
// METHODS ON EntryPair
//=======================================================================================

void EntryPair::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        UnicodeStringStreamer::streamOut(&entryName, os);
        T_FileStream_write(os, &value, sizeof(value));
        T_FileStream_write(os, &fwd, sizeof(fwd));
    }
}

void EntryPair::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        UnicodeStringStreamer::streamIn(&entryName, is);
        T_FileStream_read(is, &value, sizeof(value));
        T_FileStream_read(is, &fwd, sizeof(fwd));
    }
}

//=======================================================================================
// METHODS ON VectorOfInt
//=======================================================================================

VectorOfInt::VectorOfInt(int32_t    initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

// Copy constructor
VectorOfInt::VectorOfInt(const VectorOfInt& that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new int32_t[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    int32_t*    to = fElements;
    int32_t*    from = that.fElements;
    int32_t*    end = &(fElements[fCapacity]);

    while (to < end)
        *to++ = *from++;
}

VectorOfInt::~VectorOfInt()
{
    delete [] fElements;
}

// assignment operator
const VectorOfInt&
VectorOfInt::operator=(const VectorOfInt&   that)
{
    if (this != &that) {
        // resize if necessary
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new int32_t[that.fCapacity];
            if (!fElements) { 
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        int32_t*    to = fElements;
        int32_t*    from = that.fElements;
        int32_t*    cutover = &(fElements[that.fCapacity]);
        int32_t*    end = &(fElements[fCapacity]);

        while (to < cutover)
            *to++ = *from++;
        while (to < end)
            *to++ = 0;

        fSize = that.fSize;
    }
    return *this;
}

bool_t
VectorOfInt::isBogus() const
{
    return fBogus;
}

bool_t
VectorOfInt::operator==(const VectorOfInt&  that)
{
    if (this == &that) return TRUE;
    if (fSize != that.fSize) return FALSE;
    for (int32_t i = 0; i < fSize; i++) {
        if (fElements[i] != that.fElements[i])
            return FALSE;
    }
    return TRUE;
}

bool_t
VectorOfInt::operator!=(const VectorOfInt& that)
{
    return !(*this == that);
}

// replace the element at the index
void
VectorOfInt::atPut( int32_t     index,
                const int32_t&  value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }
    fElements[index] = value;
}

// insert the element at the index, shift down the following elements
void
VectorOfInt::atInsert(  int32_t         index,
                        const   int32_t&    value)
{
    if (fSize + 1 >= fCapacity) {
        resize(fSize + 1);
        if (fBogus) return;
    } else {
        fSize++;
    }
    int32_t i;
    for (i = fSize - 2 ; i >= index; i--)
    {
        fElements[i+1] = fElements[i];
    }
    fElements[index] = value;
}

// Resize the element array.  Create a new array and copy the elements over 
// then discard the old array.
void
VectorOfInt::resize(int32_t newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    int32_t*    newArray = 0;
    newArray    = new int32_t[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }

    int32_t*    iter = newArray;
    int32_t*    cutover = &(newArray[fCapacity]);
    int32_t*    end = &(newArray[newCapacity]);
    int32_t*    from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}

// Do not detect the out of bounds error, try to do the right thing
// by resizing the array.
int32_t&
VectorOfInt::operator[](int32_t index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0]; // HSYS : Is this correct?
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}

void
VectorOfInt::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        T_FileStream_write(os, &fSize, sizeof(fSize));
        T_FileStream_write(os, fElements, sizeof(*fElements) * fSize);
    }
}

void
VectorOfInt::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        int32_t newSize;
        T_FileStream_read(is, &newSize, sizeof(newSize));
        resize(newSize);
        if (fBogus) return;
        T_FileStream_read(is, fElements, sizeof(*fElements) * newSize);
    }
}

//=======================================================================================
// METHODS ON VectorOfPointer
//=======================================================================================

VectorOfPointer::VectorOfPointer(int32_t    initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

VectorOfPointer::VectorOfPointer(const VectorOfPointer& that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new void*[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    void**  to = fElements;
    void**  from = that.fElements;
    void**  end = &(fElements[fCapacity]);

    while (to < end)
        *to++ = *from++;
}

VectorOfPointer::~VectorOfPointer()
{
    delete [] fElements;
}

const VectorOfPointer&
VectorOfPointer::operator=(const VectorOfPointer&   that)
{
    if (this != &that) {
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new void*[that.fCapacity];
            if (!fElements) { 
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        void**  to = fElements;
        void**  from = that.fElements;
        void**  cutover = &(fElements[that.fCapacity]);
        void**  end = &(fElements[fCapacity]);

        while (to < cutover)
            *to++ = *from++;
        while (to < end)
            *to++ = 0;

        fSize = that.fSize;
    }
    return *this;
}

bool_t

VectorOfPointer::isBogus() const
{
    return fBogus;
}

bool_t
VectorOfPointer::operator==(const VectorOfPointer&  that)
{
    if (this == &that) return TRUE;
    if (fSize != that.fSize) return FALSE;
    for (int32_t i = 0; i < fSize; i++) {
        if (fElements[i] != that.fElements[i])
            return FALSE;
    }
    return TRUE;
}

bool_t
VectorOfPointer::operator!=(const VectorOfPointer& that)
{
    return !(*this == that);
}
void
VectorOfPointer::atPut( int32_t     index,
                        const void*&    value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }
    fElements[index] = (void*)value;
}

void
VectorOfPointer::atInsert(  int32_t         index,
                            const   void*&  value)
{
    if (fSize + 1 >= fCapacity) {
        resize(fSize + 1);
        if (fBogus) return;
    } else {
        fSize++;
    }
    int32_t i;
    for (i = fSize - 2 ; i >= index; i--)
    {
        fElements[i+1] = fElements[i];
    }
    fElements[index] = (void*)value;
}

void
VectorOfPointer::resize(int32_t newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    void**  newArray = 0;
    newArray    = new void*[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }

    void**  iter = newArray;
    void**  cutover = &(newArray[fCapacity]);
    void**  end = &(newArray[newCapacity]);
    void**  from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}

void*&
VectorOfPointer::operator[](int32_t index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0]; // HSYS : Is this correct?
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}
//=======================================================================================
// METHODS ON VectorOfPToExpandTable
//=======================================================================================

VectorOfPToExpandTable::VectorOfPToExpandTable(int32_t  initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

VectorOfPToExpandTable::VectorOfPToExpandTable(const VectorOfPToExpandTable& that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new VectorOfInt*[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    VectorOfInt**       to = fElements;
    VectorOfInt**       from = that.fElements;
    VectorOfInt**       end = &(fElements[fCapacity]);

    while (to < end) {
        if (*from == 0)
            *to++ = *from++;
        else
            // We actually DUPLICATE the items pointed to by "that"
            *to = new VectorOfInt(*(*from++));
            if ((*to)->isBogus()) {
                delete [] fElements;
                fElements = 0;
                return;
            }
            to++;
    }
}

VectorOfPToExpandTable::~VectorOfPToExpandTable()
{
    VectorOfInt**       iter = fElements;
    VectorOfInt**       end = &(fElements[fSize]);

    while (iter < end)
        delete *iter++;

    delete [] fElements;
}

bool_t
VectorOfPToExpandTable::isBogus() const
{
    return fBogus;
}

const VectorOfPToExpandTable&
VectorOfPToExpandTable::operator=(const VectorOfPToExpandTable& that)
{
    if (this != &that) {
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new VectorOfInt*[that.fCapacity];
            if (!fElements) {
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        VectorOfInt**       to = fElements;
        VectorOfInt**       from = that.fElements;
        VectorOfInt**       cutover = &(fElements[that.fCapacity]);
        VectorOfInt**       end = &(fElements[fCapacity]);

        while (to < cutover) {
            delete *to;
            if (*from == 0)
                *to++ = *from++;
            else {
                *to = new VectorOfInt(*(*from++));
                if ((*to)->isBogus()) {
                    delete [] fElements;
                    fElements = 0;
                    return *this;
                }
                to++;
            }
        }
        while (to < end) {
            delete *to;
            *to++ = 0;
        }

        fSize = that.fSize;
    }
    return *this;
}

PToExpandTable
VectorOfPToExpandTable::operator[](int32_t  index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0];  // Always return the first element
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}

void
VectorOfPToExpandTable::atPut(  int32_t         index,
                                VectorOfInt*    value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }

    delete fElements[index];
    fElements[index] = value;
}

VectorOfInt*
VectorOfPToExpandTable::orphanAt(int32_t    index)
{
    if (index > fSize)
        return 0;
    else {
        VectorOfInt*        returnVal = fElements[index];
        fElements[index] = 0;
        return returnVal;
    }
}

void
VectorOfPToExpandTable::resize(int32_t  newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    VectorOfInt**       newArray = 0;
    newArray = new VectorOfInt*[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }
    VectorOfInt**       iter = newArray;
    VectorOfInt**       cutover = &(newArray[fCapacity]);
    VectorOfInt**       end = &(newArray[newCapacity]);
    VectorOfInt**       from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}

void
VectorOfPToExpandTable::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        T_FileStream_write(os, &fSize, sizeof(fSize));
        int32_t i;
        for (i=0; i<fSize; ++i)
        {
            char isNull = (fElements[i] == 0);
            T_FileStream_write(os, &isNull, sizeof(isNull));
            if (!isNull) fElements[i]->streamOut(os);
        }
    }
}

void
VectorOfPToExpandTable::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        int32_t newSize;
        T_FileStream_read(is, &newSize, sizeof(newSize));
        resize(newSize);
        if (fBogus) return;
        int32_t i;
        for (i=0; i<newSize; ++i)
        {
            char isNull;
            T_FileStream_read(is, &isNull, sizeof(isNull));
            if (isNull)
            {
                delete fElements[i];
                fElements[i] = 0;
            }
            else
            {
                if (fElements[i] == 0) fElements[i] = new VectorOfInt;
                fElements[i]->streamIn(is);
                if (fElements[i]->isBogus()) {
                    fBogus = TRUE;
                    return;
                }
            }
        }
    }
}

//=======================================================================================
// METHODS ON VectorOfPToContractElement
//=======================================================================================

VectorOfPToContractElement::VectorOfPToContractElement(int32_t  initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

VectorOfPToContractElement::VectorOfPToContractElement(const VectorOfPToContractElement&    that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new EntryPair*[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    EntryPair**     to = fElements;
    EntryPair**     from = that.fElements;
    EntryPair**     end = &(fElements[fCapacity]);

    while (to < end) {
        if (*from == 0)
            *to++ = *from++;
        else
            // We actually DUPLICATE the items pointed to by "that"
            *to++ = new EntryPair(*(*from++));
    }
}

VectorOfPToContractElement::~VectorOfPToContractElement()
{
    EntryPair**     iter = fElements;
    EntryPair**     end = &(fElements[fSize]);

    while (iter < end)
        delete *iter++;

    delete [] fElements;
}

bool_t
VectorOfPToContractElement::isBogus() const
{
    return fBogus;
}

const VectorOfPToContractElement&
VectorOfPToContractElement::operator=(const VectorOfPToContractElement& that)
{
    if (this != &that) {
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new EntryPair*[that.fCapacity];
            if (!fElements) {
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        EntryPair**     to = fElements;
        EntryPair**     from = that.fElements;
        EntryPair**     cutover = &(fElements[that.fCapacity]);
        EntryPair**     end = &(fElements[fCapacity]);

        while (to < cutover) {
            delete *to;
            if (*from == 0)
                *to++ = *from++;
            else
                *to++ = new EntryPair(*(*from++));
        }
        while (to < end) {
            delete *to;
            *to++ = 0;
        }

        fSize = that.fSize;
    }
    return *this;
}

PToContractElement
VectorOfPToContractElement::operator[](int32_t  index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0];
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}

void
VectorOfPToContractElement::atPut(  int32_t     index,
                                    EntryPair*      value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1); 
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }

    delete fElements[index];
    fElements[index] = value;
}

void
VectorOfPToContractElement::atInsert(   int32_t     index,
                                        EntryPair*  value)
{
    if (fSize + 1 >= fCapacity) {
        resize(fSize + 1);
        if (fBogus) return;
    } else {
        fSize++;
    }
    int32_t i;
    for (i = fSize - 2 ; i >= index; i--)
    {
        fElements[i+1] = fElements[i];
    }
    fElements[index] = value;
}

EntryPair*
VectorOfPToContractElement::orphanAt(int32_t    index)
{
    if (index > fSize)
        return 0;
    else {
        EntryPair*      returnVal = fElements[index];
        fElements[index] = 0;
        return returnVal;
    }
}

void
VectorOfPToContractElement::resize(int32_t  newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    EntryPair**     newArray = 0;
    newArray = new EntryPair*[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }
    EntryPair**     iter = newArray;
    EntryPair**     cutover = &(newArray[fCapacity]);
    EntryPair**     end = &(newArray[newCapacity]);
    EntryPair**     from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}

void
VectorOfPToContractElement::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        T_FileStream_write(os, &fSize, sizeof(fSize));
        int32_t i;
        for (i=0; i<fSize; ++i)
        {
            char isNull = (fElements[i] == 0);
            T_FileStream_write(os, &isNull, sizeof(isNull));
            if (!isNull) fElements[i]->streamOut(os);
        }
    }
}

void
VectorOfPToContractElement::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        int32_t newSize;
        T_FileStream_read(is, &newSize, sizeof(newSize));
        resize(newSize);
        if (fBogus) return;
        int32_t i;
        for (i=0; i<newSize; ++i)
        {
            char isNull;
            T_FileStream_read(is, &isNull, sizeof(isNull));
            if (isNull)
            {
                delete fElements[i];
                fElements[i] = 0;
            }
            else
            {
                if (fElements[i] == 0) fElements[i] = new EntryPair;
                fElements[i]->streamIn(is);
            }
        }
    }
}

//=======================================================================================
// METHODS ON VectorOfPToContractTable
//=======================================================================================

VectorOfPToContractTable::VectorOfPToContractTable(int32_t  initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

VectorOfPToContractTable::VectorOfPToContractTable(const VectorOfPToContractTable&  that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new VectorOfPToContractElement*[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    VectorOfPToContractElement**        to = fElements;
    VectorOfPToContractElement**        from = that.fElements;
    VectorOfPToContractElement**        end = &(fElements[fCapacity]);

    while (to < end) {
        if (*from == 0)
            *to++ = *from++;
        else {
            // We actually DUPLICATE the items pointed to by "that"
            *to = new VectorOfPToContractElement(*(*from++));
            if ((*to)->isBogus()) {
                delete [] fElements;
                fElements = 0;
                return;
            }
            to++;
        }
    }
}

VectorOfPToContractTable::~VectorOfPToContractTable()
{
    VectorOfPToContractElement**        iter = fElements;
    VectorOfPToContractElement**        end = &(fElements[fSize]);

    while (iter < end)
        delete *iter++;

    delete [] fElements;
}

bool_t
VectorOfPToContractTable::isBogus() const
{
    return fBogus;
}

const VectorOfPToContractTable&
VectorOfPToContractTable::operator=(const VectorOfPToContractTable& that)
{
    if (this != &that) {
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new VectorOfPToContractElement*[that.fCapacity];
            if (!fElements) {
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        VectorOfPToContractElement**        to = fElements;
        VectorOfPToContractElement**        from = that.fElements;
        VectorOfPToContractElement**        cutover = &(fElements[that.fCapacity]);
        VectorOfPToContractElement**        end = &(fElements[fCapacity]);

        while (to < cutover) {
            delete *to;
            if (*from == 0)
                *to++ = *from++;
            else {
                *to = new VectorOfPToContractElement(*(*from++));
                if ((*to)->isBogus()) {
                    delete [] fElements;
                    fElements = 0;
                    return *this;
                }
                to++;
            }
        }
        while (to < end) {
            delete *to;
            *to++ = 0;
        }

        fSize = that.fSize;
    }
    return *this;
}

void
VectorOfPToContractTable::atPut(    int32_t     index,
                                    VectorOfPToContractElement*     value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }

    delete fElements[index];
    fElements[index] = value;
}

PToContractTable
VectorOfPToContractTable::operator[](int32_t    index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0];
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}

VectorOfPToContractElement*
VectorOfPToContractTable::orphanAt(int32_t  index)
{
    if (index > fSize)
        return 0;
    else {
        VectorOfPToContractElement*     returnVal = fElements[index];
        fElements[index] = 0;
        return returnVal;
    }
}

void
VectorOfPToContractTable::resize(int32_t    newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    VectorOfPToContractElement**        newArray = 0;
    newArray = new VectorOfPToContractElement*[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }
    VectorOfPToContractElement**        iter = newArray;
    VectorOfPToContractElement**        cutover = &(newArray[fCapacity]);
    VectorOfPToContractElement**        end = &(newArray[newCapacity]);
    VectorOfPToContractElement**        from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}

void
VectorOfPToContractTable::streamOut(FileStream* os) const
{
    if (!T_FileStream_error(os))
    {
        T_FileStream_write(os, &fSize, sizeof(fSize));
        int32_t i;
        for (i=0; i<fSize; ++i)
        {
            char isNull = (fElements[i] == 0);
            T_FileStream_write(os, &isNull, sizeof(isNull));
            if (!isNull) fElements[i]->streamOut(os);
        }
    }
}

void
VectorOfPToContractTable::streamIn(FileStream* is)
{
    if (!T_FileStream_error(is))
    {
        int32_t newSize;
        T_FileStream_read(is, &newSize, sizeof(newSize));
        resize(newSize);
        if (fBogus) return;
        int32_t i;
        for (i=0; i<newSize; ++i)
        {
            char isNull;
            T_FileStream_read(is, &isNull, sizeof(isNull));
            if (isNull)
            {
                delete fElements[i];
                fElements[i] = 0;
            }
            else
            {
                if (fElements[i] == 0) fElements[i] = new VectorOfPToContractElement;
                fElements[i]->streamIn(is);
            }
        }
    }
}

//=======================================================================================
// METHODS ON PointerToPatternEntry
//=======================================================================================

PointerToPatternEntry::PointerToPatternEntry(PatternEntry*& value)
: fValue(value)
{
}

PointerToPatternEntry::PointerToPatternEntry(const PointerToPatternEntry&   that)
: fValue(that.fValue)
{
}

PointerToPatternEntry::~PointerToPatternEntry()
{
}

const PointerToPatternEntry&
PointerToPatternEntry::operator=(PatternEntry*  newValue)
{
    delete fValue;
    fValue = newValue;
    return *this;
}

const PointerToPatternEntry&
PointerToPatternEntry::operator=(const PointerToPatternEntry&   pointerToNewValue)
{
    delete fValue;
    fValue = (PatternEntry*)(pointerToNewValue);
    return *this;
}

PointerToPatternEntry::operator PatternEntry*() const
{
    return fValue;
}

//=======================================================================================
// METHODS ON VectorOfPointersToPatternEntry
//=======================================================================================
VectorOfPointersToPatternEntry::VectorOfPointersToPatternEntry(int32_t  initialSize)
: fSize(0),
  fCapacity(0),
  fElements(0),
  fBogus(FALSE)
{
    if (initialSize != 0) {
        resize(initialSize);
        if (fBogus) return;
    }
}

VectorOfPointersToPatternEntry::VectorOfPointersToPatternEntry(const VectorOfPointersToPatternEntry& that)
: fSize(that.fSize),
  fCapacity(that.fCapacity),
  fElements(0),
  fBogus(FALSE)
{
    fElements = new PatternEntry*[fCapacity];
    if (!fElements) {
        fBogus = TRUE;
        return;
    }
    PatternEntry**      to = fElements;
    PatternEntry**      from = that.fElements;
    PatternEntry**      end = &(fElements[fCapacity]);

    while (to < end) {
        if (*from == 0)
            *to++ = *from++;
        else
            // We actually DUPLICATE the items pointed to by "that"
            *to++ = new PatternEntry(*(*from++));
    }
}

VectorOfPointersToPatternEntry::~VectorOfPointersToPatternEntry()
{
    PatternEntry**      iter = fElements;
    PatternEntry**      end = &(fElements[fSize]);

    while (iter < end)
        delete *iter++;

    delete [] fElements;
}

bool_t
VectorOfPointersToPatternEntry::isBogus() const
{
    return fBogus;
}

const VectorOfPointersToPatternEntry&
VectorOfPointersToPatternEntry::operator=(const VectorOfPointersToPatternEntry& that)
{
    if (this != &that) {
        if (fCapacity < that.fSize) {
            delete [] fElements;
            fElements = 0;
            fElements = new PatternEntry*[that.fCapacity];
            if (!fElements) {
                fBogus = TRUE;
                return *this;
            }
            fCapacity = that.fCapacity;
        }

        PatternEntry**      to = fElements;
        PatternEntry**      from = that.fElements;
        PatternEntry**      cutover = &(fElements[that.fCapacity]);
        PatternEntry**      end = &(fElements[fCapacity]);

        while (to < cutover) {
            delete *to;
            if (*from == 0)
                *to++ = *from++;
            else
                *to++ = new PatternEntry(*(*from++));
        }
        while (to < end) {
            delete *to;
            *to++ = 0;
        }

        fSize = that.fSize;
    }
    return *this;
}

PatternEntry*
VectorOfPointersToPatternEntry::operator[](int32_t  index) const
{
    return (index < fCapacity) ? fElements[index] : 0;
}

PointerToPatternEntry
VectorOfPointersToPatternEntry::operator[](int32_t  index)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return fElements[0];
        }
        else
            fSize = index + 1;
    }
    return fElements[index];
}

void
VectorOfPointersToPatternEntry::atPut(  int32_t         index,
                                        PatternEntry*   value)
{
    if (index >= fSize) {
        if (index >= fCapacity) {
            resize(index + 1);
            if (fBogus) return;
        }
        else
            fSize = index + 1;
    }

    delete fElements[index];
    fElements[index] = value;
}

void
VectorOfPointersToPatternEntry::atInsert(   int32_t         index,
                                            PatternEntry*   value)
{
    if (fSize + 1 >= fCapacity) {
        resize(fSize + 1);
        if (fBogus) return;
    } else {
        fSize++;
    }
    int32_t i;
    for (i = fSize - 2 ; i >= index; i--)
    {
        fElements[i+1] = fElements[i];
    }
    fElements[index] = value;
}

PatternEntry*
VectorOfPointersToPatternEntry::orphanAt(int32_t    index)
{
    if (index > fSize)
        return 0;
    else {
        PatternEntry*   returnVal = fElements[index];
        fElements[index] = 0;
        return returnVal;
    }
}

void
VectorOfPointersToPatternEntry::clear()
{
    int32_t i;

    for (i = 0; i < fSize; i += 1)
    {
        delete fElements[i];
        fElements[i] = NULL;
    }

    fSize = 0;
}

int32_t
VectorOfPointersToPatternEntry::size() const
{
    return fSize;
}

int32_t
VectorOfPointersToPatternEntry::indexOf(const PatternEntry* value) const
{
    int32_t i;

    if (value == NULL)
    {
        for (i = 0; i < fSize; i += 1)
        {
            if (fElements[i] == NULL)
            {
                return i;
            }
        }
    }
    else
    {
        for (i = 0; i < fSize; i += 1)
        {
            if (fElements[i] != NULL && value->equals(*fElements[i]))
            {
                return i;
            }
        }
    }

    return -1;
}

int32_t
VectorOfPointersToPatternEntry::lastIndexOf(const PatternEntry* value) const
{
    int32_t i;

    if (value == NULL)
    {
        for (i = fSize - 1; i >= 0; i -= 1)
        {
            if (fElements[i] == NULL)
            {
                return i;
            }
        }
    }
    else
    {
        for (i = fSize - 1; i >= 0; i -= 1)
        {
            if (fElements[i] != NULL && value->equals(*fElements[i]))
            {
                return i;
            }
        }
    }

    return -1;
}

void
VectorOfPointersToPatternEntry::resize(int32_t  newSize)
{
    int32_t     newCapacity;

    newCapacity = newSize / GROWTH_RATE;
    if (newCapacity < 10)
        newCapacity = 10;
    newCapacity += newSize;

    PatternEntry**      newArray = 0;
    newArray = new PatternEntry*[newCapacity];
    if (!newArray) {
        fBogus = TRUE;
        return;
    }
    PatternEntry**      iter = newArray;
    PatternEntry**      cutover = &(newArray[fCapacity]);
    PatternEntry**      end = &(newArray[newCapacity]);
    PatternEntry**      from = fElements;

    while (iter < cutover)
        *iter++ = *from++;
    while (iter < end)
        *iter++ = 0;

    delete [] fElements;
    fElements = newArray;
    fSize = newSize;
    fCapacity = newCapacity;
}
