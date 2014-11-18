/*
******************************************************************************
* Copyright (C) 2014, International Business Machines
* Corporation and others.  All Rights Reserved.
******************************************************************************
* shareddateformat.h
*/

#ifndef __SHARED_DATEFORMAT_H__
#define __SHARED_DATEFORMAT_H__

#include "unicode/utypes.h"
#include "sharedobject.h"

U_NAMESPACE_BEGIN

class DateFormat;

class U_I18N_API SharedDateFormat : public SharedObject {
public:
    SharedDateFormat(DateFormat *dfToAdopt) : ptr(dfToAdopt) { }
    virtual ~SharedDateFormat();
    const DateFormat *get() const { return ptr; }
    const DateFormat *operator->() const { return ptr; }
    const DateFormat &operator*() const { return *ptr; }
private:
    DateFormat *ptr;
    SharedDateFormat(const SharedDateFormat &);
    SharedDateFormat &operator=(const SharedDateFormat &);
};

U_NAMESPACE_END

#endif
