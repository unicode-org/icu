/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File uhash.h
*
* Modification History:
*
*   Date        Name        Description
*   03/12/99    stephen     Creation.
*******************************************************************************
*/

#ifndef UHASH_H
#define UHASH_H

#include "utypes.h"

/*
 * Hashtable stores key-value pairs and does efficient lookup based on keys.
 * It also provides a protocol for enumerating through the key-value pairs
 * (although it does so in no particular order). 
 * Values are stored as void* pointers.
 */

/**
 * A hashing function.
 * @param parm A pointer to the data to be hashed.
 * @return A NON-NEGATIVE hash code for parm.
 */
typedef int32_t (*UHashFunction)(const void*);
/**
 * A function called when performing a <TT>uhash_remove</TT> or a <TT>uhash_close</TT>
 * and <TT>uhash_put</TT>
 * @param parm A pointer to the data to be hashed.
 */
typedef void (* U_CALLCONV ValueDeleter)(void* valuePtr);
/** The UHashtable struct */
struct UHashtable {

  /* Internals - DO NOT TOUCH! */
  
  int32_t     primeIndex;     /* Index into our prime table for length */
  int32_t     highWaterMark;     /* Used for determiningg rehashing time */
  int32_t     lowWaterMark;
  float     highWaterFactor;
  float     lowWaterFactor;

  int32_t     count;      /* The number of items in this table */
  
  int32_t     *hashes;    /* Hash codes associated with values */
  void         **values;      /* The stored values */
  int32_t     length;     /* The physical size of hashes and values */

  ValueDeleter valueDelete; /*Function deletes values when required, if NULL won't do anything*/
  UHashFunction hashFunction;      /* Hashing function */

  int32_t    toBeDeletedCount; 
  void**    toBeDeleted;
  bool_t isGrowable;
};
typedef struct UHashtable UHashtable;

/**
 * Initialize a new UHashtable.
 * @param func A pointer to the hashing function to be used by this hash table.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return A pointer to a UHashtable, or 0 if an error occurred.
 * @see uhash_openSize
 */
U_CAPI UHashtable*
uhash_open(UHashFunction func,
       UErrorCode *status);

/**
 * Initialize a new UHashtable with a given size. If after a sequence of uhash_put the table runs out of space
 * An error will be signalled by uhash_put.
 * @param hash A pointer to the UHashtable to be initialized.
 * @param func A pointer to the hashing function to be used by this hash table.
 * @param size The maximal capacity of this hash table.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return A pointer to a UHashtable, or 0 if an error occurred.
 * @see uhash_open
 */
U_CAPI UHashtable*
uhash_openSize(UHashFunction func,
           int32_t size,
           UErrorCode *status);

/**
 * Close a UHashtable, releasing the memory used.
 * @param hash The UHashtable to close.
 */
U_CAPI void
uhash_close(UHashtable *hash);


U_CAPI void
uhash_setValueDeleter(UHashtable *hash, ValueDeleter del);

/**
 * Get the number of items stored in a UHashtable.
 * @param hash The UHashtable to query.
 * @return The number of items stored in hash.
 */
U_CAPI int32_t
uhash_size(const UHashtable *hash);

/**
 * Put an item in a UHashtable.
 * @param hash The target UHashtable.
 * @param value The value to store.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The hash code associated with value.
 * @see uhash_get
 */
U_CAPI int32_t
uhash_put(UHashtable *hash,
      void *value,
      UErrorCode *status);

/**
 * Put an item in a UHashtable.
 * @param hash The target UHashtable.
 * @param value The value to store.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The hash code associated with value.
 * @see uhash_get
 */
U_CAPI int32_t
uhash_putKey(UHashtable *hash,
         int32_t valueKey,
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
      int32_t key);

/**
 * Remove an item from a UHashtable.
 * @param hash The target UHashtable.
 * @param key The hash code of the value to be removed.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The item removed, or 0 if not found.
 */
U_CAPI void*
uhash_remove(UHashtable *hash,
         int32_t key,
         UErrorCode *status);

/**
 * Iterate through the elements of a UHashtable.
 * @param hash The target UHashtable.
 * @param pos A pointer to an integer.  This should be set to -1 to retrieve
 * the first value, and should subsequently not be changed by the caller.
 * @return The next item in the hash table, or 0 if no items remain.
 */
U_CAPI void*
uhash_nextElement(const UHashtable *hash,
          int32_t *pos);


/* Predefined hashing functions */

/** Indicates an invalid hash code */
#define UHASH_INVALID 0

/** Indicates a value is empty or 0 */
#define UHASH_EMPTY 1

/**
 * Generate a hash code for a null-terminated ustring.
 * If the string is not null-terminated the behavior of this
 * function is undefined.
 * @param parm The ustring (const UChar*) to hash.
 * @return A hash code for parm.
 */
U_CAPI int32_t
uhash_hashUString(const void *parm);

/**
 * Generate a hash code for a null-terminated string.
 * If the string is not null-terminated the behavior of this
 * function is undefined.
 * @param parm The string (const char*) to hash.
 * @return A hash code for parm.
 */
U_CAPI int32_t
uhash_hashString(const void *parm);

/**
 * Generate a hash code for long integer.
 * @param parm The long (cast to void*) to hash.
 * @return A hash code for parm.
 */
U_CAPI int32_t
uhash_hashLong(const void *parm);

#endif



