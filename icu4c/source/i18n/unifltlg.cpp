/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unifltlg.h"
#include "unifilt.h"

class UnicodeNotFilter : public UnicodeFilter {
    UnicodeFilter* filt;
public:
    UnicodeNotFilter(UnicodeFilter* adopted);
    UnicodeNotFilter(const UnicodeNotFilter&);
    virtual ~UnicodeNotFilter();
    virtual bool_t isIn(UChar c) const;
    virtual UnicodeFilter* clone() const;
};

UnicodeNotFilter::UnicodeNotFilter(UnicodeFilter* adopted) : filt(adopted) {}
UnicodeNotFilter::UnicodeNotFilter(const UnicodeNotFilter& f) : filt(f.filt->clone()) {}
UnicodeNotFilter::~UnicodeNotFilter() { delete filt; }
bool_t UnicodeNotFilter::isIn(UChar c) const { return !filt->isIn(c); }
UnicodeFilter* UnicodeNotFilter::clone() const { return new UnicodeNotFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements the inverse of
 * the given filter.
 */
UnicodeFilter* UnicodeFilterLogic::createNot(const UnicodeFilter& f) {
    return new UnicodeNotFilter(f.clone());
}

class UnicodeAndFilter : public UnicodeFilter {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    UnicodeAndFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeAndFilter(const UnicodeAndFilter&);
    virtual ~UnicodeAndFilter();
    virtual bool_t isIn(UChar c) const;
    virtual UnicodeFilter* clone() const;
};

UnicodeAndFilter::UnicodeAndFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeAndFilter::UnicodeAndFilter(const UnicodeAndFilter& f) :
    filt1(f.filt1->clone()), filt2(f.filt2->clone()) {}
UnicodeAndFilter::~UnicodeAndFilter() { delete filt1; delete filt2; }
bool_t UnicodeAndFilter::isIn(UChar c) const { return filt1->isIn(c) && filt2->isIn(c); }
UnicodeFilter* UnicodeAndFilter::clone() const { return new UnicodeAndFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit AND of the result of the two given filters.  That is,
 * if <tt>f.isIn()</tt> is <tt>false</tt>, then <tt>g.isIn()</tt>
 * is not called, and <tt>isIn()</tt> returns <tt>false</tt>.
 *
 * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
 */
UnicodeFilter* UnicodeFilterLogic::createAnd(const UnicodeFilter& f,
                                             const UnicodeFilter& g) {
    return new UnicodeAndFilter(f.clone(), g.clone());
}

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit AND of the result of the given filters.  That is, if
 * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
 * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
 * <tt>isIn()</tt> returns <tt>false</tt>.
 */
//!UnicodeFilter* UnicodeFilterLogic::and(const UnicodeFilter** f) {
//!    return new UnicodeFilter() {
//!        public bool_t isIn(UChar c) {
//!            for (int32_t i=0; i<f.length; ++i) {
//!                if (!f[i].isIn(c)) {
//!                    return FALSE;
//!                }
//!            }
//!            return TRUE;
//!        }
//!    };
//!}

class UnicodeOrFilter : public UnicodeFilter {
    UnicodeFilter* filt1;
    UnicodeFilter* filt2;
public:
    UnicodeOrFilter(UnicodeFilter* adopted1, UnicodeFilter* adopted2);
    UnicodeOrFilter(const UnicodeOrFilter&);
    virtual ~UnicodeOrFilter();
    virtual bool_t isIn(UChar c) const;
    virtual UnicodeFilter* clone() const;
};

UnicodeOrFilter::UnicodeOrFilter(UnicodeFilter* f1, UnicodeFilter* f2) : filt1(f1), filt2(f2) {}
UnicodeOrFilter::UnicodeOrFilter(const UnicodeOrFilter& f) :
    filt1(f.filt1->clone()), filt2(f.filt2->clone()) {}
UnicodeOrFilter::~UnicodeOrFilter() { delete filt1; delete filt2; }
bool_t UnicodeOrFilter::isIn(UChar c) const { return filt1->isIn(c) || filt2->isIn(c); }
UnicodeFilter* UnicodeOrFilter::clone() const { return new UnicodeOrFilter(*this); }

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit OR of the result of the two given filters.  That is, if
 * <tt>f.isIn()</tt> is <tt>true</tt>, then <tt>g.isIn()</tt> is
 * not called, and <tt>isIn()</tt> returns <tt>true</tt>.
 *
 * <p>Either <tt>f</tt> or <tt>g</tt> must be non-null.
 */
UnicodeFilter* UnicodeFilterLogic::createOr(const UnicodeFilter& f,
                                            const UnicodeFilter& g) {
    return new UnicodeOrFilter(f.clone(), g.clone());
}

/**
 * Returns a <tt>UnicodeFilter</tt> that implements a short
 * circuit OR of the result of the given filters.  That is, if
 * <tt>f[i].isIn()</tt> is <tt>false</tt>, then
 * <tt>f[j].isIn()</tt> is not called, where <tt>j > i</tt>, and
 * <tt>isIn()</tt> returns <tt>true</tt>.
 */
//!UnicodeFilter* UnicodeFilterLogic::or(const UnicodeFilter** f) {
//!    return new UnicodeFilter() {
//!        public bool_t isIn(UChar c) {
//!            for (int32_t i=0; i<f.length; ++i) {
//!                if (f[i].isIn(c)) {
//!                    return TRUE;
//!                }
//!            }
//!            return FALSE;
//!        }
//!    };
//!}

// TODO: Add nand() & nor() for convenience, if needed.
