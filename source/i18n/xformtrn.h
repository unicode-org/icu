/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef XFORMTRN_H
#define XFORMTRN_H

#include "unicode/translit.h"

U_NAMESPACE_BEGIN

/**
 * An abstract class for transliterators based on a transform
 * operation.  To create a transliterator that implements a
 * transformation, create a subclass of this class and implement the
 * abstract <code>transform()</code> and <code>hasTransform()</code>
 * methods.
 * @author Alan Liu
 */
class U_I18N_API TransformTransliterator : public Transliterator {

 protected:

    /**
     * Constructs a transliterator.  For use by subclasses.
     */
    TransformTransliterator(const UnicodeString& id,
                            UnicodeFilter* adoptedFilter);

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    void handleTransliterate(Replaceable& text, UTransPosition& offset,
                             UBool isIncremental) const;
    /**
     * Subclasses must implement this method to determine whether a
     * given character has a transform that is not equal to itself.
     * This is approximately equivalent to <code>c !=
     * transform(String.valueOf(c))</code>, where
     * <code>String.valueOf(c)</code> returns a String containing the
     * single character (not integer) <code>c</code>.  Subclasses that
     * transform all their input can simply return <code>true</code>.
     */
    virtual UBool hasTransform(UChar32 c) const = 0;

    /**
     * Subclasses must implement this method to transform a string.
     */
    virtual void transform(UnicodeString& s) const = 0;
};

U_NAMESPACE_END

#endif
