/*
**********************************************************************
*   Copyright (C) 1999 Alan Liu and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.  This is an internal header.
*                           It should not be exported.
**********************************************************************
*/

#ifndef UVECTOR_H
#define UVECTOR_H

#include "utypes.h"

/**
 * <p>Ultralightweight C++ implementation of a <tt>void*</tt> vector
 * that is (mostly) compatible with java.util.Vector.
 *
 * <p>This is a very simple implementation, written to satisfy an
 * immediate porting need.  As such, it is not completely fleshed out,
 * and it aims for simplicity and conformity.  Nonetheless, it serves
 * its purpose (porting code from java that uses java.util.Vector)
 * well, and it could be easily made into a more robust vector class.
 *
 * <p><b>Design notes</b>
 *
 * <p>There is index bounds checking, but little is done about it.  If
 * indices are out of bounds, either nothing happens, or zero is
 * returned.  We <em>do</em> avoid indexing off into the weeds.
 *
 * <p>There is detection of out of memory, but the handling is very
 * coarse-grained -- similar to UnicodeString's protocol, but even
 * coarser.  The class contains <em>one static flag</em> that is set
 * when any call to <tt>new</tt> returns zero.  This allows the caller
 * to use several vectors and make just one check at the end to see if
 * a memory failure occurred.  This is more efficient than making a
 * check after each call on each vector when doing many operations on
 * multiple vectors.  The single static flag works best when memory
 * failures are infrequent, and when recovery options are limited or
 * nonexistent.
 *
 * <p>Since we don't have garbage collection, UVector was given the
 * option to <em>own</em>its contents.  To employ this, set a deleter
 * function.  The deleter is called on a void* pointer when that
 * pointer is released by the vector, either when the vector itself is
 * destructed, or when a call to setElementAt() overwrites an element,
 * or when a call to remove() or one of its variants explicitly
 * removes an element.  If no deleter is set, or the deleter is set to
 * zero, then it is assumed that the caller will delete elements as
 * needed.
 *
 * <p>In order to implement methods such as contains() and indexOf(),
 * UVector needs a way to compare objects for equality.  To do so, it
 * uses a comparison frunction, or "comparer."  If the comparer is not
 * set, or is set to zero, then all such methods will act as if the
 * vector contains no element.  That is, indexOf() will always return
 * -1, contains() will always return FALSE, etc.
 *
 * <p><b>To do</b>
 *
 * <p>Improve the handling of index out of bounds errors.
 *
 * @author Alan Liu
 */
class UVector {
public:
    typedef void (*Deleter)(void*);
    typedef bool_t (*Comparer)(void*, void*);

private:
    int32_t count;

    int32_t capacity;

    void** elements;

    Deleter deleter;

    Comparer comparer;

    static bool_t outOfMemory;

public:
    UVector(int32_t initialCapacity = 8);

    UVector(Deleter d, Comparer c, int32_t initialCapacity = 8);

    ~UVector();

    void addElement(void* obj);

    void setElementAt(void* obj, int32_t index);

    void insertElementAt(void* obj, int32_t index);

    void* elementAt(int32_t index) const;

    void* firstElement() const;

    void* lastElement() const;

    int32_t indexOf(void* obj, int32_t startIndex = 0) const;

    bool_t contains(void* obj) const;

    void removeElementAt(int32_t index);

    bool_t removeElement(void* obj);

    void removeAllElements();

    int32_t size() const;

    bool_t isEmpty() const;

    bool_t ensureCapacity(int32_t minimumCapacity);

    // New API
    Deleter setDeleter(Deleter d);

    Comparer setComparer(Comparer c);

    static bool_t isOutOfMemory();

    void* operator[](int32_t index) const;

private:
    void _init(int32_t initialCapacity);

    // Disallow
    UVector(const UVector&);

    // Disallow
    UVector& operator=(const UVector&);
};


/**
 * <p>Ultralightweight C++ implementation of a <tt>void*</tt> stack
 * that is (mostly) compatible with java.util.Stack.  As in java, this
 * is merely a paper thin layer around UVector.  See the UVector
 * documentation for further information.
 *
 * <p><b>Design notes</b>
 *
 * <p>The element at index <tt>n-1</tt> is (of course) the top of the
 * stack.
 *
 * <p>The poorly named <tt>empty()</tt> method doesn't empty the
 * stack; it determines if the stack is empty.
 *
 * @author Alan Liu
 */
class UStack : public UVector {
public:
    UStack(int32_t initialCapacity = 8);

    UStack(Deleter d, Comparer c, int32_t initialCapacity = 8);

    // It's okay not to have a virtual destructor (in UVector)
    // because UStack has no special cleanup to do.

    bool_t empty() const;

    void* peek() const;
    
    void* pop();
    
    void* push(void* obj);

    int32_t search(void* obj) const;

private:
    // Disallow
    UStack(const UStack&);

    // Disallow
    UStack& operator=(const UStack&);
};


// UVector inlines

inline int32_t UVector::size() const {
    return count;
}

inline bool_t UVector::isEmpty() const {
    return count == 0;
}

inline bool_t UVector::contains(void* obj) const {
    return indexOf(obj) >= 0;
}

inline void* UVector::firstElement() const {
    return elementAt(0);
}

inline void* UVector::lastElement() const {
    return elementAt(count-1);
}

inline void* UVector::operator[](int32_t index) const {
    return elementAt(index);
}

// Dummy implementation - disallowed method
UVector::UVector(const UVector&) {}

// Dummy implementation - disallowed method
UVector& UVector::operator=(const UVector&) {
    return *this;
}


// UStack inlines

inline bool_t UStack::empty() const {
    return isEmpty();
}

inline void* UStack::peek() const {
    return lastElement();
}

inline void* UStack::push(void* obj) {
    addElement(obj);
    return obj;
}

// Dummy implementation - disallowed method
UStack::UStack(const UStack&) {}

// Dummy implementation - disallowed method
UStack& UStack::operator=(const UStack&) {
    return *this;
}

#endif
