/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Adapted from original C++ ICU Hashtable.
*******************************************************************************
*/

#ifndef UHASH_H
#define UHASH_H

#include "unicode/utypes.h"

/**
 * UHashtable stores key-value pairs and does moderately fast lookup
 * based on keys.  It provides a good tradeoff between access time and
 * storage space.  As elements are added to it, it grows to accomodate
 * them.  In its current implementation, the table never shrinks, even
 * if all elements are removed from it.
 *
 * Keys and values are stored as void* pointers.  These void* pointers
 * may be actual pointers to strings, objects, or any other structure
 * in memory, or they may simply be integral values cast to void*.
 * UHashtable doesn't care and manipulates them via user-supplied
 * functions.  These functions hash keys, compare keys, delete keys,
 * and delete values.  Some function pointers are optional (may be
 * NULL); others must be supplied.  Several prebuilt functions exist
 * to handle common key types.
 *
 * UHashtable ownership of keys and values is flexible, and controlled
 * by whether or not the key deleter and value deleter functions are
 * set.  If a void* key is actually a pointer to a deletable object,
 * then UHashtable can be made to delete that object by setting the
 * key deleter function pointer to a non-NULL value.  If this is done,
 * then keys passed to uhash_put() are owned by the hashtable and will
 * be deleted by it at some point, either as keys are replaced, or
 * when uhash_close() is finally called.  The same is true of values
 * and the value deleter function pointer.  Keys passed to methods
 * other than uhash_put() are never owned by the hashtable.
 *
 * NULL values are not allowed.  uhash_get() returns NULL to indicate
 * a key that is not in the table, and having a NULL value in the
 * table would generate an ambiguous result.  If a key and a NULL
 * value is passed to uhash_put(), this has the effect of doing a
 * uhash_remove() on that key.  This keeps uhash_get(), uhash_count(),
 * and uhash_nextElement() consistent with one another.
 *
 * To see everything in a hashtable, use uhash_nextElement() to
 * iterate through its contents.  Each call to this function returns a
 * UHashElement pointer.  A hash element contains a key, value, and
 * hashcode.  During iteration an element may be deleted by calling
 * uhash_removeElement(); iteration may safely continue thereafter.
 * The uhash_remove() function may also be safely called in
 * mid-iteration.  However, if uhash_put() is called during iteration
 * then the iteration will be out of sync.  Under no circumstances
 * should the UHashElement returned by uhash_nextElement be modified
 * directly.
 *
 * By default, the hashtable grows when necessary, but never shrinks,
 * even if all items are removed.  For most applications this is
 * optimal.  However, in a highly dynamic usage where memory is at a
 * premium, the table can be set to both grow and shrink by calling
 * uhash_setResizePolicy() with the policy U_GROW_AND_SHRINK.  In a
 * situation where memory is critical and the client wants a table
 * that does not grow at all, the constant U_FIXED can be used.
 */

/********************************************************************
 * Data Structures
 ********************************************************************/

U_CDECL_BEGIN

/**
 * A hashing function.
 * @param key A key stored in a hashtable
 * @return A NON-NEGATIVE hash code for parm.
 */
typedef int32_t (* U_CALLCONV UHashFunction)(const void* key);

/**
 * A key comparison function.
 * @param key1 A key stored in a hashtable
 * @param key2 A key stored in a hashtable
 * @return TRUE if the two keys are equal.
 */
typedef UBool (* U_CALLCONV UKeyComparator)(const void* key1,
                                             const void* key2);

/**
 * A function called by <TT>uhash_remove</TT>,
 * <TT>uhash_close</TT>, or <TT>uhash_put</TT> to delete
 * an existing key or value.
 * @param obj A key or value stored in a hashtable
 */
typedef void (* U_CALLCONV UObjectDeleter)(void* obj);

/**
 * This is a single hash element.  These should pack nicely
 * into exactly 24 bytes.  If this is not true, then split
 * the elements array into 3 separate arrays for the hash,
 * key, and value.
 */
struct UHashElement {
    int32_t  hashcode;
    void*    key;
    void*    value;
};
typedef struct UHashElement UHashElement;

/**
 * This specifies whether or not, and how, the hastable resizes itself.
 * See uhash_setResizePolicy().
 */
enum UHashResizePolicy {
    U_GROW,            /* Grow on demand, do not shrink */
    U_GROW_AND_SHRINK, /* Grow and shrink on demand */
    U_FIXED            /* Never change size */
};

/**
 * The UHashtable struct.  Clients should treat this as an opaque data
 * type and manipulate it only through the uhash_... API.
 */
struct UHashtable {

    /* Main key-value pair storage array */

    UHashElement *elements;

    /* Size parameters */
  
    int32_t     count;      /* The number of key-value pairs in this table.
                             * 0 <= count <= length.  In practice we
                             * never let count == length (see code). */
    int32_t     length;     /* The physical size of the arrays hashes, keys
                             * and values.  Must be prime. */
    int32_t     primeIndex;     /* Index into our prime table for length.
                                 * length == PRIMES[primeIndex] */

    /* Rehashing thresholds */
    
    int32_t     highWaterMark;  /* If count > highWaterMark, rehash */
    int32_t     lowWaterMark;   /* If count < lowWaterMark, rehash */
    float       highWaterRatio; /* 0..1; high water as a fraction of length */
    float       lowWaterRatio;  /* 0..1; low water as a fraction of length */
    
    /* Function pointers */

    UHashFunction keyHasher;      /* Computes hash from key.
                                   * Never null. */
    UKeyComparator keyComparator; /* Compares keys for equality.
                                   * Never null. */
    UObjectDeleter keyDeleter;    /* Deletes keys when required.
                                   * If NULL won't do anything */
    UObjectDeleter valueDeleter;  /* Deletes values when required.
                                   * If NULL won't do anything */
};
typedef struct UHashtable UHashtable;

U_CDECL_END

/********************************************************************
 * API
 ********************************************************************/

/**
 * Initialize a new UHashtable.
 * @param keyHash A pointer to the key hashing function.  Must not be
 * NULL.
 * @param keyComp A pointer to the function that compares keys.  Must
 * not be NULL.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return A pointer to a UHashtable, or 0 if an error occurred.
 * @see uhash_openSize
 */
U_CAPI UHashtable*
uhash_open(UHashFunction keyHash,
           UKeyComparator keyComp,
           UErrorCode *status);

/**
 * Initialize a new UHashtable with a given initial size.
 * @param keyHash A pointer to the key hashing function.  Must not be
 * NULL.
 * @param keyComp A pointer to the function that compares keys.  Must
 * not be NULL.
 * @param size The initial capacity of this hash table.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return A pointer to a UHashtable, or 0 if an error occurred.
 * @see uhash_open
 */
U_CAPI UHashtable*
uhash_openSize(UHashFunction keyHash,
               UKeyComparator keyComp,
               int32_t size,
               UErrorCode *status);

/**
 * Close a UHashtable, releasing the memory used.
 * @param hash The UHashtable to close.
 */
U_CAPI void
uhash_close(UHashtable *hash);



/**
 * Set the function used to hash keys.
 * @param fn the function to be used hash keys; must not be NULL
 * @return the previous key hasher; non-NULL
 */
U_CAPI UHashFunction
uhash_setKeyHasher(UHashtable *hash, UHashFunction fn);

/**
 * Set the function used to compare keys.  The default comparison is a
 * void* pointer comparison.
 * @param fn the function to be used compare keys; must not be NULL
 * @return the previous key comparator; non-NULL
 */
U_CAPI UKeyComparator
uhash_setKeyComparator(UHashtable *hash, UKeyComparator fn);

/**
 * Set the function used to delete keys.  If this function pointer is
 * NULL, this hashtable does not delete keys.  If it is non-NULL, this
 * hashtable does delete keys.  This function should be set once
 * before any elements are added to the hashtable and should not be
 * changed thereafter.
 * @param fn the function to be used delete keys, or NULL
 * @return the previous key deleter; may be NULL
 */
U_CAPI UObjectDeleter
uhash_setKeyDeleter(UHashtable *hash, UObjectDeleter fn);

/**
 * Set the function used to delete values.  If this function pointer
 * is NULL, this hashtable does not delete values.  If it is non-NULL,
 * this hashtable does delete values.  This function should be set
 * once before any elements are added to the hashtable and should not
 * be changed thereafter.
 * @param fn the function to be used delete values, or NULL
 * @return the previous value deleter; may be NULL
 */
U_CAPI UObjectDeleter
uhash_setValueDeleter(UHashtable *hash, UObjectDeleter fn);

/**
 * Specify whether or not, and how, the hastable resizes itself.
 * By default, tables grow but do not shrink (policy U_GROW).
 * See enum UHashResizePolicy.
 */
U_CAPI void
uhash_setResizePolicy(UHashtable *hash, enum UHashResizePolicy policy);

/**
 * Get the number of key-value pairs stored in a UHashtable.
 * @param hash The UHashtable to query.
 * @return The number of key-value pairs stored in hash.
 */
U_CAPI int32_t
uhash_count(const UHashtable *hash);

/**
 * Put an item in a UHashtable.  If the keyDeleter is non-NULL, then
 * the hashtable owns 'key' after this call.  If the valueDeleter is
 * non-NULL, then the hashtable owns 'value' after this call.
 * Storing a NULL value is the same as calling uhash_remove().
 * @param hash The target UHashtable.
 * @param key The key to store.
 * @param value The value to store, may be NULL (see above).
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The previous value, or NULL if none.
 * @see uhash_get
 */
U_CAPI void*
uhash_put(UHashtable *hash,
          void *key,
          void *value,
          UErrorCode *status);

/**
 * Get an item from a UHashtable.
 * @param hash The target UHashtable.
 * @param key The hash code of the desired value.
 * @return The requested item, or 0 if not found.
 */
U_CAPI void*
uhash_get(const UHashtable *hash, 
          const void *key);

/**
 * Remove an item from a UHashtable.
 * @param hash The target UHashtable.
 * @param key The hash code of the value to be removed.
 * @return The item removed, or 0 if not found.
 */
U_CAPI void*
uhash_remove(UHashtable *hash,
             const void *key);

/**
 * Remove all items from a UHashtable.
 * @param hash The target UHashtable.
 */
U_CAPI void
uhash_removeAll(UHashtable *hash);

/**
 * Iterate through the elements of a UHashtable.  The caller must not
 * modify the returned object.  However, uhash_removeElement() may be
 * called during iteration to remove an element from the table.
 * Iteration may safely be resumed afterwards.  If uhash_put() is
 * called during iteration the iteration will then be out of sync and
 * should be restarted.
 * @param hash The target UHashtable.
 * @param pos This should be set to -1 initially, and left untouched
 * thereafter.
 * @return a hash element, or NULL if no further key-value pairs
 * exist in the table.
 */
U_CAPI const UHashElement*
uhash_nextElement(const UHashtable *hash,
                  int32_t *pos);

/**
 * Remove an element, returned by uhash_nextElement(), from the table.
 * Iteration may be safely continued afterwards.
 * @param hash The hashtable
 * @param e The element, returned by uhash_nextElement(), to remove.
 * Must not be NULL.  Must not be an empty or deleted element (as long
 * as this was returned by uhash_nextElement() it will not be empty or
 * deleted).  Note: Although this parameter is const, it will be
 * modified.
 * @return the value that was removed.
 */
U_CAPI void*
uhash_removeElement(UHashtable *hash, const UHashElement* e);

/********************************************************************
 * UChar* and char* Support Functions
 ********************************************************************/

/**
 * Generate a hash code for a null-terminated UChar* string.  If the
 * string is not null-terminated do not use this function.  Use
 * together with uhash_compareUChars.
 * @param key The string (const UChar*) to hash.
 * @return A hash code for the key.
 */
U_CAPI int32_t
uhash_hashUChars(const void *key);

/**
 * Generate a hash code for a null-terminated char* string.  If the
 * string is not null-terminated do not use this function.  Use
 * together with uhash_compareChars.
 * @param key The string (const char*) to hash.
 * @return A hash code for the key.
 */
U_CAPI int32_t
uhash_hashChars(const void *key);

/* Used by UnicodeString to compute its hashcode - Not public API. */
U_CAPI int32_t
uhash_hashUCharsN(const UChar *key, int32_t length);

/**
 * Generate a case-insensitive hash code for a null-terminated char*
 * string.  If the string is not null-terminated do not use this
 * function.  Use together with uhash_compareIChars.
 * @param key The string (const char*) to hash.
 * @return A hash code for the key.
 */
U_CAPI int32_t
uhash_hashIChars(const void *key);

/**
 * Comparator for null-terminated UChar* strings.  Use together with
 * uhash_hashUChars.
 */
U_CAPI UBool
uhash_compareUChars(const void *key1, const void *key2);

/**
 * Comparator for null-terminated char* strings.  Use together with
 * uhash_hashChars.
 */
U_CAPI UBool
uhash_compareChars(const void *key1, const void *key2);

/**
 * Case-insensitive comparator for null-terminated char* strings.  Use
 * together with uhash_hashIChars.
 */
U_CAPI UBool
uhash_compareIChars(const void *key1, const void *key2);

/********************************************************************
 * UnicodeString Support Functions
 ********************************************************************/

/**
 * Hash function for UnicodeString* keys.
 */
U_CAPI int32_t
uhash_hashUnicodeString(const void *key);

/**
 * Comparator function for UnicodeString* keys.
 */
U_CAPI UBool
uhash_compareUnicodeString(const void *key1, const void *key2);

/**
 * Deleter function for UnicodeString* keys or values.
 */
U_CAPI void
uhash_deleteUnicodeString(void *obj);

/********************************************************************
 * int32_t Support Functions
 ********************************************************************/

/**
 * Hash function for 32-bit integer keys.
 */
U_CAPI int32_t
uhash_hashLong(const void *key);

/**
 * Comparator function for 32-bit integer keys.
 */
U_CAPI UBool
uhash_compareLong(const void *key1, const void *key2);

/********************************************************************
 * Other Support Functions
 ********************************************************************/

/**
 * Deleter for any key or value allocated using uprv_malloc.  Calls
 * uprv_free.
 */
U_CAPI void
uhash_freeBlock(void *obj);

#endif
