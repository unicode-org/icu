/*
*******************************************************************************
* Copyright (C) 1999, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.
**********************************************************************
*/

#include "uvector.h"
#include "cmemory.h"

bool_t UVector::outOfMemory = FALSE;

UVector::UVector(int32_t initialCapacity) :
    capacity(0),
    count(0),
    elements(0),
    deleter(0),
    comparer(0) {

    _init(initialCapacity);
}

UVector::UVector(Deleter d, Comparer c, int32_t initialCapacity) :
    capacity(0),
    count(0),
    elements(0),
    deleter(d),
    comparer(c) {
   
    _init(initialCapacity);
}

void UVector::_init(int32_t initialCapacity) {
    elements = new void*[initialCapacity];
    if (elements == 0) {
        outOfMemory = TRUE;
    } else {
        capacity = initialCapacity;
    }
}

UVector::~UVector() {
    removeAllElements();
    delete[] elements;
    elements = 0;
}

void UVector::addElement(void* obj) {
    if (ensureCapacity(count + 1)) {
        elements[count++] = obj;
    }
}

void UVector::setElementAt(void* obj, int32_t index) {
    if (0 <= index && index < count) {
        if (elements[index] != 0 && deleter != 0) {
            (*deleter)(elements[index]);
        }
        elements[index] = obj;
    }
    /* else index out of range */
}

void UVector::insertElementAt(void* obj, int32_t index) {
    // must have 0 <= index <= count
    if (0 <= index && index <= count && ensureCapacity(count + 1)) {
        for (int32_t i=count; i>index; --i) {
            elements[i] = elements[i-1];
        }
        elements[index] = obj;
    }
    /* else index out of range */
}

void* UVector::elementAt(int32_t index) const {
    return (0 <= index && index < count) ? elements[index] : 0;
}

void UVector::removeElementAt(int32_t index) {
    void* e = orphanElementAt(index);
    if (e != 0 && deleter != 0) {
        (*deleter)(e);
    }
}

bool_t UVector::removeElement(void* obj) {
    int32_t i = indexOf(obj);
    if (i >= 0) {
        removeElementAt(i);
        return TRUE;
    }
    return FALSE;
}

void UVector::removeAllElements(void) {
    if (deleter != 0) {
        for (int32_t i=0; i<count; ++i) {
            if (elements[i] != 0) {
                (*deleter)(elements[i]);
            }
        }
    }
    count = 0;
}

int32_t UVector::indexOf(void* obj, int32_t startIndex) const {
    if (comparer != 0) {
        for (int32_t i=startIndex; i<count; ++i) {
            if ((*comparer)(obj, elements[i])) {
                return i;
            }
        }
    }
    return -1;
}

bool_t UVector::ensureCapacity(int32_t minimumCapacity) {
    if (capacity >= minimumCapacity) {
        return TRUE;
    } else {
        int32_t newCap = capacity * 2;
        void** newElems = new void*[newCap];
        if (newElems == 0) {
            outOfMemory = TRUE;
            return FALSE;
        }
        uprv_memcpy(newElems, elements, sizeof(void*) * count);
        delete[] elements;
        elements = newElems;
        capacity = newCap;
        return TRUE;
    }
}

UVector::Deleter UVector::setDeleter(Deleter d) {
    Deleter old = deleter;
    deleter = d;
    return old;
}

UVector::Comparer UVector::setComparer(Comparer d) {
    Comparer old = comparer;
    comparer = d;
    return old;
}

bool_t UVector::isOutOfMemory(void) {
    return outOfMemory;
}

/**
 * Removes the element at the given index from this vector and
 * transfer ownership of it to the caller.  After this call, the
 * caller owns the result and must delete it and the vector entry
 * at 'index' is removed, shifting all subsequent entries back by
 * one index and shortening the size of the vector by one.  If the
 * index is out of range or if there is no item at the given index
 * then 0 is returned and the vector is unchanged.
 */
void* UVector::orphanElementAt(int32_t index) {
    void* e = 0;
    if (0 <= index && index < count) {
        e = elements[index];
        for (int32_t i=index; i<count; ++i) {
            elements[i] = elements[i+1];
        }
		--count;
    }
    /* else index out of range */
    return e;
}

UStack::UStack(int32_t initialCapacity) :
    UVector(initialCapacity) {
}

UStack::UStack(Deleter d, Comparer c, int32_t initialCapacity) :
    UVector(d, c, initialCapacity) {
}

void* UStack::pop(void) {
    void* obj = lastElement();
    if (obj != 0) {
        removeElementAt(size() - 1);
    }
    return obj;
}

int32_t UStack::search(void* obj) const {
    int32_t i = indexOf(obj);
    return (i >= 0) ? size() - i : i;
}
