/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unicode/unifltlg.h"
#include "unicode/unifilt.h"

/**
 * A NullFilter always returns a fixed value, either TRUE or FALSE.
 * A filter value of 0 (that is, a UnicodeFilter* f, where f == 0)
 * is equivalent to a NullFilter(TRUE).
 */
class NullFilter : public UnicodeFilter {
    UBool result;
public:
    NullFilter(UBool r) { result = r; }
    NullFilter(const NullFilter& f) : UnicodeFilter(f) { result = f.result; }
    virtual ~NullFilter() {}
    virtual UBool contains(UChar32 /*c*/) const { return result; }
    virtual UnicodeMatcher* clone() const { return new NullFilter(*this); }
};

class UnicodeNotFilter : public UnicodeFilter {
    UnicodeFilter* filt;
public:
    UnicodeNotFilter(UnicodeFilter* adopted);
    UnicodeNotFilter(const UnicodeNotFilter&);
    virtual ~UnicodeNotFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeMatcher* clone() const;
};

UnicodeNotFilter::UnicodeNotFilter(UnicodeFilter* adopted) : filt(adopted) {}
UnicodeNotFilter::UnicodeNotFilter(const UnicodeNotFilter& f)
 : UnicodeFilter(f), filt((UnicodeFilter*) f.filt->clone()) {}
UnicodeNotFilter::~UnicodeNotFilter() { delete filt; }
UBool UnicodeNotFilter::contains(UChar32 c) const { return !filt->contains(c); }
UnicodeMatcher* UnicodeNotFilter::clone() const { return new UnicodeNotFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
 * the given filter.
 */
UnicodeFilter* UnicodeFilterLogic::createNot(const UnicodeFilter* f) {
    if (f == 0) {
        return new NullFilter(FALSE);
    } else {
        return new UnicodeNotFilter((UnicodeFilter*)f->clone());
    }
}

class UnicodeAndFilter : public UnicodeFilter {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    UnicodeAndFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeAndFilter(const UnicodeAndFilter&);
    virtual ~UnicodeAndFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeMatcher* clone() const;
};

UnicodeAndFilter::UnicodeAndFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeAndFilter::UnicodeAndFilter(const UnicodeAndFilter& f)
 : UnicodeFilter(f), filt1((UnicodeFilter*)f.filt1->clone()), filt2((UnicodeFilter*)f.filt2->clone()) {}
UnicodeAndFilter::~UnicodeAndFilter() { delete filt1; delete filt2; }
UBool UnicodeAndFilter::contains(UChar32 c) const { return filt1->contains(c) && filt2->contains(c); }
UnicodeMatcher* UnicodeAndFilter::clone() const { return new UnicodeAndFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit AND of the result of the two given filters.  That is,
 * if <tt>f.contains()</tt> is <tt>false</tt>, then <tt>g.contains()</tt>
 * is not called, and <tt>contains()</tt> returns <tt>false</tt>.
 */
UnicodeFilter* UnicodeFilterLogic::createAnd(const UnicodeFilter* f,
                                             const UnicodeFilter* g) {
    if (f == 0) {
        if (g == 0) {
            return NULL;
        }
        return (UnicodeFilter*)g->clone();
    }
    if (g == 0) {
        return (UnicodeFilter*)f->clone();
    }
    return new UnicodeAndFilter((UnicodeFilter*)f->clone(), (UnicodeFilter*)g->clone());
}

class UnicodeOrFilter : public UnicodeFilter {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    UnicodeOrFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeOrFilter(const UnicodeOrFilter&);
    virtual ~UnicodeOrFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeMatcher* clone() const;
};

UnicodeOrFilter::UnicodeOrFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeOrFilter::UnicodeOrFilter(const UnicodeOrFilter& f)
 : UnicodeFilter(f), filt1((UnicodeFilter*)f.filt1->clone()), filt2((UnicodeFilter*)f.filt2->clone()) {}
UnicodeOrFilter::~UnicodeOrFilter() { delete filt1; delete filt2; }
UBool UnicodeOrFilter::contains(UChar32 c) const { return filt1->contains(c) || filt2->contains(c); }
UnicodeMatcher* UnicodeOrFilter::clone() const { return new UnicodeOrFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit OR of the result of the two given filters.  That is, if
 * <tt>f.contains()</tt> is <tt>true</tt>, then <tt>g.contains()</tt> is
 * not called, and <tt>contains()</tt> returns <tt>true</tt>.
 */
UnicodeFilter* UnicodeFilterLogic::createOr(const UnicodeFilter* f,
                                            const UnicodeFilter* g) {
    if (f == 0) {
        if (g == 0) {
            return NULL;
        }
        return (UnicodeFilter*)g->clone();
    }
    if (g == 0) {
        return (UnicodeFilter*)f->clone();
    }
    return new UnicodeOrFilter((UnicodeFilter*)f->clone(), (UnicodeFilter*)g->clone());
}
