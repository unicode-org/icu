/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   08/10/2001  aliu        Creation.
**********************************************************************
*/
#ifndef _TRANSREG_H
#define _TRANSREG_H

#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "hash.h"
#include "uvector.h"

class Entry;
class Spec;
class UnicodeString;

//------------------------------------------------------------------
// TransliteratorAlias
//------------------------------------------------------------------

/**
 * A TransliteratorAlias object is returned by get() if the given ID
 * actually translates into something else.  The caller then invokes
 * the create() method on the alias to create the actual
 * transliterator, and deletes the alias.
 *
 * Why all the shenanigans?  To prevent circular calls between
 * the registry code and the transliterator code that deadlocks.
 */
class TransliteratorAlias {
 public:
    /**
     * Construct a simple alias.
     */
    TransliteratorAlias(const UnicodeString& aliasID);
    
    /**
     * Construct a compound RBT alias.
     */
    TransliteratorAlias(const UnicodeString& ID, const UnicodeString& idBlock,
                        Transliterator* adopted, int32_t idSplitPoint);

    ~TransliteratorAlias();
    
    /**
     * The whole point of create() is that the caller must invoke
     * it when the registry mutex is NOT held, to prevent deadlock.
     * It may only be called once.
     */
    Transliterator* create(UParseError&, UErrorCode&);
    
 private:
    // We actually come in two flavors:
    // 1. Simple alias
    //    Here aliasID is the alias string.  Everything else is
    //    null, zero, empty.
    // 2. CompoundRBT
    //    Here ID is the ID, aliasID is the idBlock, trans is the
    //    contained RBT, and idSplitPoint is the offet in aliasID
    //    where the contained RBT goes.
    UnicodeString ID;
    UnicodeString aliasID;
    Transliterator* trans; // owned
    int32_t idSplitPoint;
};


/**
 * A registry of system transliterators.  This is the data structure
 * that implements the mapping between transliterator IDs and the data
 * or function pointers used to create the corresponding
 * transliterators.  There is one instance of the registry that is
 * created statically.
 *
 * The registry consists of a dynamic component -- a hashtable -- and
 * a static component -- locale resource bundles.  The dynamic store
 * is semantically overlaid on the static store, so the static mapping
 * can be dynamically overridden.
 *
 * This is an internal class that is only used by Transliterator.
 * Transliterator maintains one static instance of this class and
 * delegates all registry-related operations to it.
 *
 * @author Alan Liu
 */
class TransliteratorRegistry {

 public:

    TransliteratorRegistry(UErrorCode& status);

    /**
     * Nonvirtual destructor -- this class is not subclassable.
     */
    ~TransliteratorRegistry();

    //------------------------------------------------------------------
    // Basic public API
    //------------------------------------------------------------------

    /**
     * Given a simple ID (forward direction, no inline filter, not
     * compound) attempt to instantiate it from the registry.  Return
     * 0 on failure.
     *
     * Return a non-NULL aliasReturn value if the ID points to an alias.
     * We cannot instantiate it ourselves because the alias may contain
     * filters or compounds, which we do not understand.  Caller should
     * make aliasReturn NULL before calling.
     */
    Transliterator* get(const UnicodeString& ID,
                        TransliteratorAlias*& aliasReturn,
                        UParseError& parseError,
                        UErrorCode& status);

    /**
     * Register a prototype (adopted).  This adds an entry to the
     * dynamic store, or replaces an existing entry.  Any entry in the
     * underlying static locale resource store is masked.
     */
    void put(Transliterator* adoptedProto,
             UBool visible);

    /**
     * Register an ID and a factory function pointer.  This adds an
     * entry to the dynamic store, or replaces an existing entry.  Any
     * entry in the underlying static locale resource store is masked.
     */
    void put(const UnicodeString& ID,
             Transliterator::Factory factory,
             UBool visible);

    /**
     * Register an ID and a resource name.  This adds an entry to the
     * dynamic store, or replaces an existing entry.  Any entry in the
     * underlying static locale resource store is masked.
     */
    void put(const UnicodeString& ID,
             const UnicodeString& resourceName,
             UTransDirection dir,
             UBool visible);

    /**
     * Register an ID and an alias ID.  This adds an entry to the
     * dynamic store, or replaces an existing entry.  Any entry in the
     * underlying static locale resource store is masked.
     */
    void put(const UnicodeString& ID,
             const UnicodeString& alias,
             UBool visible);

    /**
     * Unregister an ID.  This removes an entry from the dynamic store
     * if there is one.  The static locale resource store is
     * unaffected.
     */
    void remove(const UnicodeString& ID);

    //------------------------------------------------------------------
    // Public ID and spec management
    //------------------------------------------------------------------

    /**
     * Return the number of IDs currently registered with the system.
     * To retrieve the actual IDs, call getAvailableID(i) with
     * i from 0 to countAvailableIDs() - 1.
     * @draft
     */
    int32_t countAvailableIDs(void);

    /**
     * Return the index-th available ID.  index must be between 0
     * and countAvailableIDs() - 1, inclusive.  If index is out of
     * range, the result of getAvailableID(0) is returned.
     * @draft
     */
    const UnicodeString& getAvailableID(int32_t index);

    /**
     * Return the number of registered source specifiers.
     */
    int32_t countAvailableSources(void);
    
    /**
     * Return a registered source specifier.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableSources()
     * @param result fill-in paramter to receive the source specifier.
     * If index is out of range, result will be empty.
     * @return reference to result
     */
    UnicodeString& getAvailableSource(int32_t index,
                                      UnicodeString& result);
    
    /**
     * Return the number of registered target specifiers for a given
     * source specifier.
     */
    int32_t countAvailableTargets(const UnicodeString& source);
    
    /**
     * Return a registered target specifier for a given source.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableTargets(source)
     * @param source the source specifier
     * @param result fill-in paramter to receive the target specifier.
     * If source is invalid or if index is out of range, result will
     * be empty.
     * @return reference to result
     */
    UnicodeString& getAvailableTarget(int32_t index,
                                      const UnicodeString& source,
                                      UnicodeString& result);
    
    /**
     * Return the number of registered variant specifiers for a given
     * source-target pair.  There is always at least one variant: If
     * just source-target is registered, then the single variant
     * NO_VARIANT is returned.  If source-target/variant is registered
     * then that variant is returned.
     */
    int32_t countAvailableVariants(const UnicodeString& source,
                                   const UnicodeString& target);
    
    /**
     * Return a registered variant specifier for a given source-target
     * pair.  If NO_VARIANT is one of the variants, then it will be
     * at index 0.
     * @param index which specifier to return, from 0 to n-1, where
     * n = countAvailableVariants(source, target)
     * @param source the source specifier
     * @param target the target specifier
     * @param result fill-in paramter to receive the variant
     * specifier.  If source is invalid or if target is invalid or if
     * index is out of range, result will be empty.
     * @return reference to result
     */
    UnicodeString& getAvailableVariant(int32_t index,
                                       const UnicodeString& source,
                                       const UnicodeString& target,
                                       UnicodeString& result);

 private:

    //----------------------------------------------------------------
    // Private implementation
    //----------------------------------------------------------------

    Entry* find(const UnicodeString& ID);
    
    Entry* find(UnicodeString& source,
                UnicodeString& target,
                UnicodeString& variant);

    Entry* findInDynamicStore(const Spec& src,
                              const Spec& trg,
                              const UnicodeString& variant);

    Entry* findInStaticStore(const Spec& src,
                             const Spec& trg,
                             const UnicodeString& variant);

    static Entry* findInBundle(const Spec& specToOpen,
                                    const Spec& specToFind,
                                    const UnicodeString& variant,
                                    const char* tagPrefix);

    void registerEntry(const UnicodeString& source,
                       const UnicodeString& target,
                       const UnicodeString& variant,
                       Entry* adopted,
                       UBool visible);

    void registerEntry(const UnicodeString& ID,
                       Entry* adopted,
                       UBool visible);

   void registerEntry(const UnicodeString& ID,
                       const UnicodeString& source,
                       const UnicodeString& target,
                       const UnicodeString& variant,
                       Entry* adopted,
                       UBool visible);

    void registerSTV(const UnicodeString& source,
                     const UnicodeString& target,
                     const UnicodeString& variant);

    void removeSTV(const UnicodeString& source,
                   const UnicodeString& target,
                   const UnicodeString& variant);

    Transliterator* instantiateEntry(const UnicodeString& ID,
                                     Entry *entry,
                                     TransliteratorAlias*& aliasReturn,
                                     UParseError& parseError,
                                     UErrorCode& status);

    static void IDtoSTV(const UnicodeString& id,
                        UnicodeString& source,
                        UnicodeString& target,
                        UnicodeString& variant);

    static void STVtoID(const UnicodeString& source,
                        const UnicodeString& target,
                        const UnicodeString& variant,
                        UnicodeString& id);

 private:

    /**
     * Dynamic registry mapping full IDs to Entry objects.  This
     * contains both public and internal entities.  The visibility is
     * controlled by whether an entry is listed in availableIDs and
     * specDAG or not.
     */
    Hashtable registry;
    
    /**
     * DAG of visible IDs by spec.  Hashtable: source => (Hashtable:
     * target => (UVector: variant)) The UVector of variants is never
     * empty.  For a source-target with no variant, the special
     * variant NO_VARIANT (the empty string) is stored in slot zero of
     * the UVector.  This NO_VARIANT variant is invisible to
     * countAvailableVariants() and getAvailableVariant().
     */
    Hashtable specDAG;
    
    /**
     * Vector of public full IDs.
     */
    UVector availableIDs;
};

#endif
//eof
