/*
**********************************************************************
*   Copyright (C) 1999-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/

//////////////////////////////////////////////////////////////
//
// NOTICE - Do not use
//
// This entire file has been deprecated as of ICU 2.4.
//
//////////////////////////////////////////////////////////////

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/unifltlg.h"
#include "unicode/unifilt.h"

U_NAMESPACE_BEGIN

/**
 * This class stubs out UnicodeMatcher API that we don't implement.
 */
class _UF: public UnicodeFilter {

    // Stubs
    virtual UnicodeString& toPattern(UnicodeString& result,
                                     UBool escapeUnprintable) const {
        return result;
    }
    virtual UBool matchesIndexValue(uint8_t v) const {
        return FALSE;
    }
    virtual void addMatchSetTo(UnicodeSet& toUnionTo) const {}
};

/**
 * A NullFilter always returns a fixed value, either TRUE or FALSE.
 * A filter value of 0 (that is, a UnicodeFilter* f, where f == 0)
 * is equivalent to a NullFilter(TRUE).
 */
static const char gNullFilterClassID = 0;
class NullFilter : public _UF {
    UBool result;
public:
    virtual UClassID getDynamicClassID() const { return getStaticClassID(); }
    static inline UClassID getStaticClassID() { return (UClassID)&gNullFilterClassID; }
    NullFilter(UBool r) { result = r; }
    NullFilter(const NullFilter& f) : _UF(f) { result = f.result; }
    virtual ~NullFilter() {}
    virtual UBool contains(UChar32 /*c*/) const { return result; }
    virtual UnicodeFunctor* clone() const { return new NullFilter(*this); }
};

static const char gUnicodeNotFilterClassID = 0;
class UnicodeNotFilter : public _UF {
    UnicodeFilter* filt;
public:
    virtual UClassID getDynamicClassID() const { return getStaticClassID(); }
    static inline UClassID getStaticClassID() { return (UClassID)&gUnicodeNotFilterClassID; }
    UnicodeNotFilter(UnicodeFilter* adopted);
    UnicodeNotFilter(const UnicodeNotFilter&);
    virtual ~UnicodeNotFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeFunctor* clone() const;
};

UnicodeNotFilter::UnicodeNotFilter(UnicodeFilter* adopted) : filt(adopted) {}
UnicodeNotFilter::UnicodeNotFilter(const UnicodeNotFilter& f)
 : _UF(f), filt((UnicodeFilter*) f.filt->clone()) {}
UnicodeNotFilter::~UnicodeNotFilter() { delete filt; }
UBool UnicodeNotFilter::contains(UChar32 c) const { return !filt->contains(c); }
UnicodeFunctor* UnicodeNotFilter::clone() const { return new UnicodeNotFilter(*this); }

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

static const char gUnicodeAndFilterClassID = 0;
class UnicodeAndFilter : public _UF {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    virtual UClassID getDynamicClassID() const { return getStaticClassID(); }
    static inline UClassID getStaticClassID() { return (UClassID)&gUnicodeAndFilterClassID; }
    UnicodeAndFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeAndFilter(const UnicodeAndFilter&);
    virtual ~UnicodeAndFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeFunctor* clone() const;
};

UnicodeAndFilter::UnicodeAndFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeAndFilter::UnicodeAndFilter(const UnicodeAndFilter& f)
 : _UF(f), filt1((UnicodeFilter*)f.filt1->clone()), filt2((UnicodeFilter*)f.filt2->clone()) {}
UnicodeAndFilter::~UnicodeAndFilter() { delete filt1; delete filt2; }
UBool UnicodeAndFilter::contains(UChar32 c) const { return filt1->contains(c) && filt2->contains(c); }
UnicodeFunctor* UnicodeAndFilter::clone() const { return new UnicodeAndFilter(*this); }

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

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit AND of the result of the two given filters.  That is,
 * if <tt>f.contains()</tt> is <tt>false</tt>, then <tt>g.contains()</tt>
 * is not called, and <tt>contains()</tt> returns <tt>false</tt>.
 *
 * ADOPTS both arguments.
 */
UnicodeFilter* UnicodeFilterLogic::createAdoptingAnd(UnicodeFilter* f,
                                                     UnicodeFilter* g) {
    if (f == 0) {
        if (g == 0) {
            return NULL;
        }
        return g;
    }
    if (g == 0) {
        return f;
    }
    return new UnicodeAndFilter(f, g);
}

static const char gUnicodeOrFilterClassID = 0;
class UnicodeOrFilter : public _UF {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    virtual UClassID getDynamicClassID() const { return getStaticClassID(); }
    static inline UClassID getStaticClassID() { return (UClassID)&gUnicodeOrFilterClassID; }
    UnicodeOrFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeOrFilter(const UnicodeOrFilter&);
    virtual ~UnicodeOrFilter();
    virtual UBool contains(UChar32 c) const;
    virtual UnicodeFunctor* clone() const;
};

UnicodeOrFilter::UnicodeOrFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeOrFilter::UnicodeOrFilter(const UnicodeOrFilter& f)
 : _UF(f), filt1((UnicodeFilter*)f.filt1->clone()), filt2((UnicodeFilter*)f.filt2->clone()) {}
UnicodeOrFilter::~UnicodeOrFilter() { delete filt1; delete filt2; }
UBool UnicodeOrFilter::contains(UChar32 c) const { return filt1->contains(c) || filt2->contains(c); }
UnicodeFunctor* UnicodeOrFilter::clone() const { return new UnicodeOrFilter(*this); }

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

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
