/*
*****************************************************************
* Copyright (c) 2002, International Business Machines Corporation
* and others.  All Rights Reserved.
*****************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/i18n/anytrans.h,v $ 
* $Revision: 1.1 $
*****************************************************************
* Date        Name        Description
* 06/06/2002  aliu        Creation.
*****************************************************************
*/
#ifndef _ANYTRANS_H_
#define _ANYTRANS_H_

#include "unicode/translit.h"
#include "unicode/uscript.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator named Any-X, where X is the target, that contains
 * multiple transliterators, all going to X, all with script sources.
 * The target need not be a script.  It uses the script run API
 * (uscript.h) to partition text into runs of the same script, and
 * then based on the script of each run, transliterates from that
 * script to the given target.
 *
 * <p>For example, "Any-Latin" might contain two transliterators,
 * "Greek-Latin" and "Hiragana-Latin".  It would then transliterate
 * runs of Greek with Greek-Latin, runs of Hiragana with
 * Hirgana-Latin, and pass other runs through unchanged.
 *
 * <p>There is no inverse of an Any-X transliterator.  Although it
 * would be possible to tag the output text with script markers to
 * make inversion possible, this is not currently implemented.
 *
 * @author Alan Liu
 */
class U_I18N_API AnyTransliterator : public Transliterator {

    /**
     * A script code and associated transliterator.  It does _not_ own
     * the transliterator.
     */
    class Elem {
    public:
        UScriptCode script;
        Transliterator* translit;
        Elem(UScriptCode s=(UScriptCode)0, Transliterator* t=NULL) {
            script = s;
            translit = t;
        }
        Elem& operator=(const Elem& o) {
            script = o.script;
            translit = o.translit;
            return *this;
        }
    };

    /**
     * Array of script codes and associated transliterators.  We
     * own the transliterators.
     */
    Elem* elems;

    /**
     * Length of elems, always at least 2.
     */
    int32_t count;

public:

    /**
     * Factory method to create an Any-X transliterator.  Relies on
     * registered transliterators at the time of the call to build the
     * Any-X transliterator.  If there are no registered transliterators
     * of the form Y-X, then the logical result is Any-Null.  If there is
     * exactly one transliterator of the form Y-X, then the logical result
     * is Y-X, a degenerate result.  If there are 2 or more
     * transliterators of the form Y-X, then an AnyTransliterator is
     * instantiated and returned. 
     * @param target the target, which need not be a script.  This
     * be a string such as "Latin", <em>not</em> "Any-Latin".
     * @param allowNull if true, then return Any-Null if there are no
     * transliterator to the given script; otherwise return NULL
     * @param allowDegenerate if true, then return a transliterator of the
     * form X-Y if there is only one such transliterator
     * the given script; otherwise return NULL
     * @return a new Transliterator, or NULL.  If allowNull or
     * allowDegenerate is TRUE, the result may not be an
     * AnyTransliterator.  If they are both false, the result will be
     * an AnyTransliterator.
     */
    static Transliterator* createInstance(const UnicodeString& target,
                                          UBool allowNull,
                                          UBool allowDegenerate);
    
//|    /**
//|     * Factory method to create an Any-X transliterator.  Convenience
//|     * function that takes a script code.
//|     */
//|    static Transliterator* createInstance(UScriptCode target,
//|                                          UBool allowNull,
//|                                          UBool allowDegenerate);
    
    /**
     * Destructor.
     */
    virtual ~AnyTransliterator();

    /**
     * Copy constructor.
     */
    AnyTransliterator(const AnyTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone() const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& index,
                                     UBool incremental) const;
    
private:

    /**
     * Private constructor for Transliterator.
     */
    AnyTransliterator(const UnicodeString& id, UVector& vec);

    /**
     * Try to create a transliterator with the given ID, which should
     * be of the form "Any-X".
     */
    static Transliterator* _create(const UnicodeString& ID, Token /*context*/);
    
    /**
     * Registers standard variants with the system.  Called by
     * Transliterator during initialization.
     */
    static void registerIDs();

    friend class Transliterator; // for registerIDs()
    
    /**
     * Return the script code for a given name, or -1 if not found.
     */
    static int32_t scriptNameToCode(const UnicodeString& name);
};

U_NAMESPACE_END

#endif
