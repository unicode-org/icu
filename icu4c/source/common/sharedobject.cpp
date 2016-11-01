/*
******************************************************************************
* Copyright (C) 2014, International Business Machines
* Corporation and others.  All Rights Reserved.
******************************************************************************
* sharedobject.cpp
*/
#include "sharedobject.h"

U_NAMESPACE_BEGIN
SharedObject::~SharedObject() {}

void
SharedObject::addRef() const {
    umtx_atomic_inc(&refCount);
}

void
SharedObject::removeRef() const {
    if(umtx_atomic_dec(&refCount) == 0) {
        delete this;
    }
}

int32_t
SharedObject::getRefCount() const {
    return umtx_loadAcquire(refCount);
}

void
SharedObject::deleteIfZeroRefCount() const {
    if(getRefCount() == 0) {
        delete this;
    }
}

U_NAMESPACE_END
