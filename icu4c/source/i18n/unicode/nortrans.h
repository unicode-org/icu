/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/03/01    aliu        Creation.
**********************************************************************
*/
#ifndef NORTRANS_H
#define NORTRANS_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/normlzr.h"

/**
 * A transliterator that performs normalization.
 * @author Alan Liu
 * @version $RCSfile: nortrans.h,v $ $Revision: 1.3 $ $Date: 2001/09/27 23:19:22 $
 */
class U_I18N_API NormalizationTransliterator : public Transliterator {

    /**
     * The normalization mode of this transliterator.
     */
    UNormalizationMode fMode;

    /**
     * Normalization options for this transliterator.
     */
    int32_t options;

 public:

    /**
     * Factory method.
     */
    static NormalizationTransliterator* createInstance(UNormalizationMode mode,
                                                       int32_t opt=0);

    /**
     * Destructor.
     */
    virtual ~NormalizationTransliterator();

    /**
     * Copy constructor.
     */
    NormalizationTransliterator(const NormalizationTransliterator&);

    /**
     * Assignment operator.
     */
    NormalizationTransliterator& operator=(const NormalizationTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

 protected:

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    void handleTransliterate(Replaceable& text, UTransPosition& offset,
                             UBool isIncremental) const;
 public:

    /**
     * System registration hook.  Public to Transliterator only.
     */
    static void registerIDs();

 private:

    // Transliterator::Factory methods
    static Transliterator* _createNFC();
    static Transliterator* _createNFKC();
    static Transliterator* _createNFD();
    static Transliterator* _createNFKD();

    /**
     * Constructs a transliterator.  This method is private.
     * Public users must use the factory method createInstance().
     */
    NormalizationTransliterator(const UnicodeString& id,
                                UNormalizationMode mode, int32_t opt);
};

#endif
