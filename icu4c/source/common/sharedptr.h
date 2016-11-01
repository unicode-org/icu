/*
*******************************************************************************
* Copyright (C) 2014, International Business Machines Corporation and         
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
struct AtomicInt : public UMemory {
    u_atomic_int32_t value;
};

/**
 * SharedPtr are shared pointers that support copy-on-write sematics.
 * SharedPtr makes the act of copying large objects cheap by deferring the
 * cost of the copy to the first write operation after the copy.
 *
 * A SharedPtr<T> instance can refer to no object or an object of type T.
 * T must have a clone() method that copies
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

// TODO (Travis Keep): Leave interface the same, but find a more efficient
// implementation that is easier to understand.
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
            refPtr = new AtomicInt();
            if (refPtr == NULL) {
                delete ptr;
                ptr = NULL;
            } else {
                refPtr->value = 1;
            }
        }
    }

    /**
     * Copy constructor.
     */
    SharedPtr(const SharedPtr<T> &other) :
            ptr(other.ptr), refPtr(other.refPtr) {
        if (refPtr != NULL) {
            umtx_atomic_inc(&refPtr->value);
        }
    }

    /**
     * assignment operator.
     */
    SharedPtr<T> &operator=(const SharedPtr<T> &other) {
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
                delete ptr;
                delete refPtr;
            }
        }
    }

    /**
     * reset adopts a new pointer. On success, returns TRUE.
     * On memory allocation error creating reference counter for adopted
     * pointer, returns FALSE while leaving this instance unchanged.
     */
    bool reset(T *adopted) {
        SharedPtr<T> newValue(adopted);
        if (adopted != NULL && newValue.ptr == NULL) {
            // We couldn't allocate ref counter.
            return FALSE;
        }
        swap(newValue);
        return TRUE;
    }

    /**
     * reset makes this instance refer to no object.
     */
    void reset() {
        reset(NULL);
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
     * Swaps this instance with other.
     */
    void swap(SharedPtr<T> &other) {
        T *tempPtr = other.ptr;
        AtomicInt *tempRefPtr = other.refPtr;
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
     * this method returns NULL leaving this instance unchanged.
     * <p>
     * If readWrite() returns a non NULL pointer, it guarantees that this
     * object holds the only reference to its T object enabling the caller to
     * perform mutations using the returned pointer without affecting other
     * SharedPtr objects. However, the non-constness of readWrite continues as
     * long as the returned pointer is in scope. Therefore it is an API
     * violation to call readWrite() on A; perform B = A; and then proceed to
     * mutate A via its writeable pointer as that would be the same as setting
     * B = A while A is changing. The returned pointer is guaranteed to be
     * valid only while this object is in scope because this object maintains
     * ownership of its T object. Therefore, callers must never attempt to
     * delete the returned writeable pointer. The best practice with readWrite
     * is this: callers should use the returned pointer from readWrite() only
     * within the same scope as that call to readWrite, and that scope should
     * be made as small as possible avoiding overlap with other operatios on
     * this object.
     */
    T *readWrite() {
        int32_t refCount = count();
        if (refCount <= 1) {
            return ptr;
        }
        T *result = (T *) ptr->clone();
        if (result == NULL) {
            // Memory allocation error
            return NULL;
        }
        if (!reset(result)) {
            return NULL;
        }
        return ptr;
    }
private:
    T *ptr;
    AtomicInt *refPtr;
    // No heap allocation. Use only stack.
    static void * U_EXPORT2 operator new(size_t size);
    static void * U_EXPORT2 operator new[](size_t size);
#if U_HAVE_PLACEMENT_NEW
    static void * U_EXPORT2 operator new(size_t, void *ptr);
#endif
};

U_NAMESPACE_END

#endif
