/*
*******************************************************************************
* Copyright (C) 2013, International Business Machines Corporation and         
* others. All Rights Reserved.                                                
*******************************************************************************
*                                                                             
* File SHAREDPTR.H                                                             
*******************************************************************************
*/

#ifndef __SHARED_PTR_H__
#define __SHARED_PTR_H__

#include "unicode/uobject.h"
#include "umutex.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

// Wrap u_atomic_int32_t in a UMemory so that we allocate them in the same
// way we allocate all other ICU objects.
struct _AtomicInt : public UMemory {
    u_atomic_int32_t value;
};

/**
 * SharedPtr are shared pointers that support copy-on-write sematics.
 * SharedPtr makes the act of copying large objects cheap by deferring the
 * cost of the copy to the first write operation after the copy.
 *
 * A SharedPtr<T> instance can refer to no object or an object of type T where
 * T is a subclass of UObject. T must also have a clone() method that copies
 * the object and returns a pointer to the copy. Copy and assignment of
 * SharedPtr instances are cheap because they only involve copying or
 * assigning the SharedPtr instance, not the T object which could be large.
 * Although many SharedPtr<T> instances may refer to the same T object,
 * clients can still assume that each SharedPtr<T> instance has its own
 * private instance of T because each SharedPtr<T> instance offers only a
 * const view of its T object through normal pointer operations. If a caller
 * must change a T object through its SharedPtr<T>, it can do so by calling
 * readWrite() on the SharedPtr instance. readWrite() ensures that the
 * SharedPtr<T> really does have its own private T object by cloning it if
 * it is shared by using its clone() method. SharedPtr<T> instances handle
 * management by reference counting their T objects. T objects that are
 * referenced by no SharedPtr<T> instances get deleted automatically.
 */
template<typename T>
class SharedPtr {
public:
    /**
     * Constructor. If there is a memory allocation error creating
     * reference counter then this object will contain NULL, and adopted
     * pointer will be freed. Note that when passing NULL or no argument to
     * constructor, no memory allocation error can happen as NULL pointers
     * are never reference counted.
     */
    explicit SharedPtr(T *adopted=NULL) : ptr(adopted), refPtr(NULL) {
        if (ptr != NULL) {
            refPtr = new _AtomicInt();
            if (refPtr == NULL) {
                delete ptr;
                ptr = NULL;
            } else {
                umtx_storeRelease(refPtr->value, 1);
            }
        }
    }

    /**
     * Non-templated copy costructor. Needed to keep compiler from
     * creating its own.
     */
    SharedPtr(const SharedPtr<T> &other) :
            ptr(other.ptr), refPtr(other.refPtr) {
        if (refPtr != NULL) {
            umtx_atomic_inc(&refPtr->value);
        }
    }

    /**
     * Templated copy constructor.
     */
    template<typename U>
    SharedPtr(const SharedPtr<U> &other) :
            ptr((T *) other.ptr), refPtr(other.refPtr) {
        if (refPtr != NULL) {
            umtx_atomic_inc(&refPtr->value);
        }
    }

    /**
     * Non-templated assignment operator. Needed to keep compiler
     * from creating its own.
     */
    SharedPtr<T> &operator=(const SharedPtr<T> &other) {
        if (ptr != other.ptr) {
            SharedPtr<T> newValue(other);
            swap(newValue);
        }
        return *this;
    }

    /**
     * Templated assignment operator.
     */
    template<typename U>
    SharedPtr<T> &operator=(const SharedPtr<U> &other) {
        if (ptr != other.ptr) {
            SharedPtr<T> newValue(other);
            swap(newValue);
        }
        return *this;
    }

    /**
     * Destructor.
     */
    ~SharedPtr() {
        if (refPtr != NULL) {
            if (umtx_atomic_dec(&refPtr->value) == 0) {
                // Cast to UObject to avoid compiler warnings about incomplete
                // type T.
                delete (UObject *) ptr;
                delete refPtr;
            }
        }
    }

    /**
     * adoptInstead adopts a new pointer. On success, returns TRUE.
     * On memory allocation error creating reference counter for adopted
     * pointer, returns FALSE while leaving this instance unchanged.
     */
    bool adoptInstead(T *adopted) {
        SharedPtr<T> newValue(adopted);
        if (adopted != NULL && newValue.ptr == NULL) {
            // We couldn't allocate ref counter.
            return FALSE;
        }
        swap(newValue);
        return TRUE;
    }

    /**
     * clear makes this instance refer to no object.
     */
    void clear() {
        adoptInstead(NULL);
    }

    /**
     * count returns how many SharedPtr instances, including this one,
     * refer to the T object. Used for testing. Clients need not use in
     * practice.
     */
    int32_t count() const {
        if (refPtr == NULL) {
            return 0;
        }
        return umtx_loadAcquire(refPtr->value);
    }

    /**
     * Swaps this instance with other. a.swap(b) is equivalent to the
     * following though more efficient: temp = a; a = b; b = temp.
     */
    void swap(SharedPtr<T> &other) {
        T *tempPtr = other.ptr;
        _AtomicInt *tempRefPtr = other.refPtr;
        other.ptr = ptr;
        other.refPtr = refPtr;
        ptr = tempPtr;
        refPtr = tempRefPtr;
    }

    const T *operator->() const {
        return ptr;
    }

    const T &operator*() const {
        return *ptr;
    }

    bool operator==(const T *other) const {
        return ptr == other;
    }

    bool operator!=(const T *other) const {
        return ptr != other;
    }

    /**
     * readOnly gives const access to this instance's T object. If this
     * instance refers to no object, returns NULL.
     */
    const T *readOnly() const {
        return ptr;
    }

    /**
     * readWrite returns a writable pointer to its T object copying it first
     * using its clone() method if it is shared.
     * On memory allocation error or if this instance refers to no object,
     * returns NULL leaving this instance unchanged.
     */
    T *readWrite() {
        int32_t refCount = count();
        if (refCount == 0 || refCount == 1) {
            return ptr;
        }
        T *result = (T *) ptr->clone();
        if (result == NULL) {
            // Memory allocation error
            return NULL;
        }
        if (!adoptInstead(result)) {
            return NULL;
        }
        return ptr;
    }
private:
    T *ptr;
    _AtomicInt *refPtr;
    // No heap allocation. Use only stack.
    static void * U_EXPORT2 operator new(size_t size);
    static void * U_EXPORT2 operator new[](size_t size);
#if U_HAVE_PLACEMENT_NEW
    static void * U_EXPORT2 operator new(size_t, void *ptr);
#endif
    template<typename U> friend class SharedPtr;
};

U_NAMESPACE_END

#endif
