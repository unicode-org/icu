/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   03/22/00    aliu        Adapted from original C++ ICU Hashtable.
*******************************************************************************
*/

#include "uhash.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cmemory.h"

/* This hashtable is implemented as a double hash.  All elements are
 * stored in a single array with no secondary storage for collision
 * resolution (no linked list, etc.).  When there is a hash collision
 * (when two unequal keys have the same hashcode) we resolve this by
 * using a secondary hash.  The secondary hash is an increment
 * computed as a hash function (a different one) of the primary
 * hashcode.  This increment is added to the initial hash value to
 * obtain further slots assigned to the same hash code.  For this to
 * work, the length of the array and the increment must be relatively
 * prime.  The easiest way to achieve this is to have the length of
 * the array be prime, and the increment be any value from
 * 1..length-1.
 *
 * Hashcodes are 32-bit integers.  We make sure all hashcodes are
 * non-negative by masking off the top bit.  This has two effects: (1)
 * modulo arithmetic is simplified.  If we allowed negative hashcodes,
 * then when we computed hashcode % length, we could get a negative
 * result, which we would then have to adjust back into range.  It's
 * simpler to just make hashcodes non-negative. (2) It makes it easy
 * to check for empty vs. occupied slots in the table.  We just mark
 * empty or deleted slots with a negative hashcode.
 *
 * The central function is _uhash_find().  This function looks for a
 * slot matching the given key and hashcode.  If one is found, it
 * returns a pointer to that slot.  If the table is full, and no match
 * is found, it returns NULL -- in theory.  This would make the code
 * more complicated, since all callers of _uhash_find() would then
 * have to check for a NULL result.  To keep this from happening, we
 * don't allow the table to fill.  When there is only one
 * empty/deleted slot left, uhash_put() will refuse to increase the
 * count, and fail.  This simplifies the code.  In practice, one will
 * seldom encounter this using default UHashtables.  However, if a
 * hashtable is set to a U_FIXED resize policy, or if memory is
 * exhausted, then the table may fill.
 *
 * High and low water ratios control rehashing.  They establish levels
 * of fullness (from 0 to 1) outside of which the data array is
 * reallocated and repopulated.  Setting the low water ratio to zero
 * means the table will never shrink.  Setting the high water ratio to
 * one means the table will never grow.  The ratios should be
 * coordinated with the ratio between successive elements of the
 * PRIMES table, so that when the primeIndex is incremented or
 * decremented during rehashing, it brings the ratio of count / length
 * back into the desired range (between low and high water ratios).
 */

/********************************************************************
 * PRIVATE Constants, Macros
 ********************************************************************/

/* This is a list of non-consecutive primes chosen such that
 * PRIMES[i+1] ~ 2*PRIMES[i].  (Currently, the ratio ranges from 1.81
 * to 2.18; the inverse ratio ranges from 0.459 to 0.552.)  If this
 * ratio is changed, the low and high water ratios should also be
 * adjusted to suit.
 */
static int32_t PRIMES[] = {
    17, 37, 67, 131, 257, 521, 1031, 2053, 4099, 8209, 16411, 32771,
    65537, 131101, 262147, 524309, 1048583, 2097169, 4194319, 8388617,
    16777259, 33554467, 67108879, 134217757, 268435459, 536870923,
    1073741827, 2147483647
};

#define PRIMES_LENGTH (sizeof(PRIMES) / sizeof(PRIMES[0]))

/* These ratios are tuned to the PRIMES array such that a resize
 * places the table back into the zone of non-resizing.  That is,
 * after a call to _uhash_rehash(), a subsequent call to
 * _uhash_rehash() should do nothing (should not churn).  This is only
 * a potential problem with U_GROW_AND_SHRINK.
 */
static const float RESIZE_POLICY_RATIO_TABLE[6] = {
    /* low, high water ratio */
    0.0F, 0.5F, /* U_GROW: Grow on demand, do not shrink */
    0.1F, 0.5F, /* U_GROW_AND_SHRINK: Grow and shrink on demand */
    0.0F, 1.0F  /* U_FIXED: Never change size */
};

/*
  Invariants for hashcode values:

  * DELETED < 0
  * EMPTY < 0
  * Real hashes >= 0

  Hashcodes may not start out this way, but internally they are
  adjusted so that they are always positive.  We assume 32-bit
  hashcodes; adjust these constants for other hashcode sizes.
*/
#define HASH_DELETED    ((int32_t) 0x80000000)
#define HASH_EMPTY      ((int32_t) HASH_DELETED + 1)

#define IS_EMPTY_OR_DELETED(x) ((x) < 0)

#define HASH_DELETE_KEY_VALUE(hash, key, value) \
            if (hash->keyDeleter != NULL && key != NULL) { \
                (*hash->keyDeleter)(key); \
            } \
            if (hash->valueDeleter != NULL && value != NULL) { \
                (*hash->valueDeleter)(value); \
            }

/********************************************************************
 * Debugging
 ********************************************************************/

/* Enable this section to compile in runtime assertion checking. */

/* #define HASH_DEBUG */
#ifdef HASH_DEBUG

  #include <stdio.h>

  #define assert(exp) (void)( (exp) || (_assert(#exp, __FILE__, __LINE__), 0) )

  static void _assert(const char* exp, const char* file, int line) {
      printf("ERROR: assert(%s) failed: %s, line %d\n",
             exp, file, line);
  }

#else

  #define assert(exp)

#endif

/********************************************************************
 * PRIVATE Prototypes
 ********************************************************************/

static UHashtable* _uhash_create(UHashFunction keyHash, UKeyComparator keyComp,
                                 int32_t primeIndex, UErrorCode *status);

static void _uhash_allocate(UHashtable *hash, int32_t primeIndex,
                            UErrorCode *status);

static void _uhash_rehash(UHashtable *hash);

static UHashElement* _uhash_find(const UHashtable *hash, const void* key,
                                 int32_t hashcode);

static void* _uhash_internalRemoveElement(UHashtable *hash, UHashElement* e);

static void* _uhash_setElement(UHashtable* hash, UHashElement* e,
                               int32_t hashcode, void* key, void* value);

static void _uhash_internalSetResizePolicy(UHashtable *hash, enum UHashResizePolicy policy);

/********************************************************************
 * PUBLIC API
 ********************************************************************/

U_CAPI UHashtable*
uhash_open(UHashFunction keyHash, UKeyComparator keyComp,
           UErrorCode *status) {

    return _uhash_create(keyHash, keyComp, 3, status);
}

U_CAPI UHashtable*
uhash_openSize(UHashFunction keyHash, UKeyComparator keyComp,
               int32_t size,
               UErrorCode *status) {

    /* Find the smallest index i for which PRIMES[i] >= size. */
    int32_t i = 0;
    while (i<(PRIMES_LENGTH-1) && PRIMES[i]<size) {
        ++i;
    }

    return _uhash_create(keyHash, keyComp, i, status);
}

U_CAPI void
uhash_close(UHashtable *hash) {
    assert(hash != NULL);
    if (hash->elements != NULL) {
        if (hash->keyDeleter != NULL || hash->valueDeleter != NULL) {
            int32_t pos=-1;
            UHashElement *e;
            while ((e = (UHashElement*) uhash_nextElement(hash, &pos)) != NULL) {
                HASH_DELETE_KEY_VALUE(hash, e->key, e->value);
            }
        }
        uprv_free(hash->elements);
        hash->elements = NULL;
    }
    uprv_free(hash);
}

U_CAPI UHashFunction
uhash_setKeyHasher(UHashtable *hash, UHashFunction fn) {
    UHashFunction result = hash->keyHasher;
    hash->keyHasher = fn;
    return result;
}

U_CAPI UKeyComparator
uhash_setKeyComparator(UHashtable *hash, UKeyComparator fn) {
    UKeyComparator result = hash->keyComparator;
    hash->keyComparator = fn;
    return result;
}

U_CAPI UObjectDeleter
uhash_setKeyDeleter(UHashtable *hash, UObjectDeleter fn) {
    UObjectDeleter result = hash->keyDeleter;
    hash->keyDeleter = fn;
    return result;
}

U_CAPI UObjectDeleter
uhash_setValueDeleter(UHashtable *hash, UObjectDeleter fn) {
    UObjectDeleter result = hash->valueDeleter;
    hash->valueDeleter = fn;
    return result;
}

U_CAPI void
uhash_setResizePolicy(UHashtable *hash, enum UHashResizePolicy policy) {
    _uhash_internalSetResizePolicy(hash, policy);
    hash->lowWaterMark  = (int32_t)(hash->length * hash->lowWaterRatio);
    hash->highWaterMark = (int32_t)(hash->length * hash->highWaterRatio);    
    _uhash_rehash(hash);
}

U_CAPI int32_t
uhash_count(const UHashtable *hash) {
    return hash->count;
}

U_CAPI void*
uhash_get(const UHashtable *hash,
          const void* key) {
    return _uhash_find(hash, key, hash->keyHasher(key))->value;
}

U_CAPI void*
uhash_put(UHashtable *hash,
          void* key,
          void* value,
          UErrorCode *status) {

    /* Put finds the position in the table for the new value.  If the
     * key is already in the table, it is deleted, if there is a
     * non-NULL keyDeleter.  Then the key, the hash and the value are
     * all put at the position in their respective arrays.
     */
    int32_t hashcode;
    UHashElement* e;

    if (U_FAILURE(*status)) {
        goto err;
    }
    assert(hash != NULL);
    if (value == NULL) {
        /* Disallow storage of NULL values, since NULL is returned by
         * get() to indicate an absent key.  Storing NULL == removing.
         */
        return uhash_remove(hash, key);
    }
    if (hash->count > hash->highWaterMark) {
        _uhash_rehash(hash);
    }

    hashcode = (*hash->keyHasher)(key);
    e = _uhash_find(hash, key, hashcode);
    assert(e != NULL);

    if (IS_EMPTY_OR_DELETED(e->hashcode)) {
        /* Important: We must never actually fill the table up.  If we
         * do so, then _uhash_find() will return NULL, and we'll have
         * to check for NULL after every call to _uhash_find().  To
         * avoid this we make sure there is always at least one empty
         * or deleted slot in the table.  This only is a problem if we
         * are out of memory and rehash isn't working.
         */
        ++hash->count;
        if (hash->count == hash->length) {
            /* Don't allow count to reach length */
            --hash->count;
            *status = U_MEMORY_ALLOCATION_ERROR;
            goto err;
        }
    }

    /* We must in all cases handle storage properly.  If there was an
     * old key, then it must be deleted (if the deleter != NULL).
     * Make hashcodes stored in table positive.
     */
    return _uhash_setElement(hash, e, hashcode & 0x7FFFFFFF, key, value);

 err:
    /* If the deleters are non-NULL, this method adopts its key and/or
     * value arguments, and we must be sure to delete the key and/or
     * value in all cases, even upon failure.
     */
    HASH_DELETE_KEY_VALUE(hash, key, value);
    return NULL;
}

U_CAPI void*
uhash_remove(UHashtable *hash,
             const void* key) {
    /* First find the position of the key in the table.  If the object
     * has not been removed already, remove it.  If the user wanted
     * keys deleted, then delete it also.  We have to put a special
     * hashcode in that position that means that something has been
     * deleted, since when we do a find, we have to continue PAST any
     * deleted values.
     */
    void* result = NULL;
    UHashElement* e = _uhash_find(hash, key, hash->keyHasher(key));
    assert(e != NULL);
    if (!IS_EMPTY_OR_DELETED(e->hashcode)) {
        result = _uhash_internalRemoveElement(hash, e);
        if (hash->count < hash->lowWaterMark) {
            _uhash_rehash(hash);
        }
    }
    return result;
}

U_CAPI void
uhash_removeAll(UHashtable *hash) {
    int32_t pos = -1;
    const UHashElement *e;
    assert(hash != NULL);
    if (hash->count != 0) {
        while ((e = uhash_nextElement(hash, &pos)) != NULL) {
            uhash_removeElement(hash, e);
        }
    }
    assert(hash->count == 0);
}

U_CAPI const UHashElement*
uhash_nextElement(const UHashtable *hash, int32_t *pos) {
    /* Walk through the array until we find an element that is not
     * EMPTY and not DELETED.
     */
    int32_t i;
    assert(hash != NULL);
    for (i = *pos + 1; i < hash->length; ++i) {
        if (!IS_EMPTY_OR_DELETED(hash->elements[i].hashcode)) {
            *pos = i;
            return &(hash->elements[i]);
        }
    }

    /* No more elements */
    return NULL;
}

U_CAPI void*
uhash_removeElement(UHashtable *hash, const UHashElement* e) {
    assert(hash != NULL);
    assert(e != NULL);
    if (!IS_EMPTY_OR_DELETED(e->hashcode)) {
        return _uhash_internalRemoveElement(hash, (UHashElement*) e);
    }
    return NULL;
}

/********************************************************************
 * PUBLIC Key Hash Functions
 ********************************************************************/

/*
  Compute the hash by iterating sparsely over about 32 (up to 63)
  characters spaced evenly through the string.  For each character,
  multiply the previous hash value by a prime number and add the new
  character in, like a linear congruential random number generator,
  producing a pseudorandom deterministic value well distributed over
  the output range. [LIU]
*/

#define STRING_HASH(TYPE, STRLEN, DEREF)      \
    int32_t hash = 0;                         \
    if (key != NULL) {                        \
        const TYPE *p = (const TYPE*) key;    \
        int32_t len = STRLEN;                 \
        int32_t inc = ((len - 32) / 32) + 1;  \
        const TYPE *limit = p + len;          \
        while (p<limit) {                     \
            hash = (hash * 37) + DEREF;       \
            p += inc;                         \
        }                                     \
    }                                         \
    return hash

U_CAPI int32_t
uhash_hashUChars(const void *key) {
    STRING_HASH(UChar, u_strlen(p), *p);
}

/* Used by UnicodeString to compute its hashcode - Not public API. */
U_CAPI int32_t
uhash_hashUCharsN(const UChar *key, int32_t length) {
    STRING_HASH(UChar, length, *p);
}

U_CAPI int32_t
uhash_hashChars(const void *key) {
    STRING_HASH(uint8_t, uprv_strlen((char*)p), *p);
}

U_CAPI int32_t
uhash_hashIChars(const void *key) {
    STRING_HASH(uint8_t, uprv_strlen((char*)p), uprv_tolower(*p));
}

/********************************************************************
 * PUBLIC Comparator Functions
 ********************************************************************/

U_CAPI UBool
uhash_compareUChars(const void *key1, const void *key2) {
    const UChar *p1 = (const UChar*) key1;
    const UChar *p2 = (const UChar*) key2;
    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == NULL || p2 == NULL) {
        return FALSE;
    }
    while (*p1 != 0 && *p1 == *p2) {
        ++p1;
        ++p2;
    }
    return (UBool)(*p1 == *p2);
}

U_CAPI UBool
uhash_compareChars(const void *key1, const void *key2) {
    const char *p1 = (const char*) key1;
    const char *p2 = (const char*) key2;
    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == NULL || p2 == NULL) {
        return FALSE;
    }
    while (*p1 != 0 && *p1 == *p2) {
        ++p1;
        ++p2;
    }
    return (UBool)(*p1 == *p2);
}

U_CAPI UBool
uhash_compareIChars(const void *key1, const void *key2) {
    const char *p1 = (const char*) key1;
    const char *p2 = (const char*) key2;
    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == NULL || p2 == NULL) {
        return FALSE;
    }
    while (*p1 != 0 && uprv_tolower(*p1) == uprv_tolower(*p2)) {
        ++p1;
        ++p2;
    }
    return (UBool)(*p1 == *p2);
}

/********************************************************************
 * PUBLIC int32_t Support Functions
 ********************************************************************/

U_CAPI int32_t
uhash_hashLong(const void *key) {
    return (int32_t) key;
}

U_CAPI UBool
uhash_compareLong(const void *key1, const void *key2) {
    return (UBool)(key1 == key2);
}

/********************************************************************
 * PUBLIC Deleter Functions
 ********************************************************************/

U_CAPI void
uhash_freeBlock(void *obj) {
    uprv_free(obj);
}

/********************************************************************
 * PRIVATE Implementation
 ********************************************************************/

static UHashtable*
_uhash_create(UHashFunction keyHash, UKeyComparator keyComp,
              int32_t primeIndex,
              UErrorCode *status) {
    UHashtable *result;

    if (U_FAILURE(*status)) return NULL;
    assert(keyHash != NULL);
    assert(keyComp != NULL);

    result = (UHashtable*) uprv_malloc(sizeof(UHashtable));
    if (result == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    result->keyHasher      = keyHash;
    result->keyComparator  = keyComp;
    result->keyDeleter     = NULL;
    result->valueDeleter   = NULL;
    _uhash_internalSetResizePolicy(result, U_GROW);

    _uhash_allocate(result, primeIndex, status);

    if (U_FAILURE(*status)) {
        uprv_free(result);
        return NULL;
    }

    return result;
}

/**
 * Allocate internal data array of a size determined by the given
 * prime index.  If the index is out of range it is pinned into range.
 * If the allocation fails the status is set to
 * U_MEMORY_ALLOCATION_ERROR and all array storage is freed.  In
 * either case the previous array pointer is overwritten.
 *
 * Caller must ensure primeIndex is in range 0..PRIME_LENGTH-1.
 */
static void
_uhash_allocate(UHashtable *hash,
                int32_t primeIndex,
                UErrorCode *status) {

    UHashElement *p, *limit;

    if (U_FAILURE(*status)) return;

    assert(primeIndex >= 0 && primeIndex < PRIMES_LENGTH);

    hash->primeIndex = primeIndex;
    hash->length = PRIMES[primeIndex];

    p = hash->elements = (UHashElement*)
        uprv_malloc(sizeof(UHashElement) * hash->length);

    if (hash->elements == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }

    limit = p + hash->length;
    while (p < limit) {
        p->key = NULL;
        p->value = NULL;
        p->hashcode = HASH_EMPTY;
        ++p;
    }

    hash->count = 0;
    hash->lowWaterMark = (int32_t)(hash->length * hash->lowWaterRatio);
    hash->highWaterMark = (int32_t)(hash->length * hash->highWaterRatio);
}

/**
 * Attempt to grow or shrink the data arrays in order to make the
 * count fit between the high and low water marks.  hash_put() and
 * hash_remove() call this method when the count exceeds the high or
 * low water marks.  This method may do nothing, if memory allocation
 * fails, or if the count is already in range, or if the length is
 * already at the low or high limit.  In any case, upon return the
 * arrays will be valid.
 */
static void
_uhash_rehash(UHashtable *hash) {

    UHashElement *old = hash->elements;
    int32_t oldLength = hash->length;
    int32_t newPrimeIndex = hash->primeIndex;
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;

    if (hash->count > hash->highWaterMark) {
        if (++newPrimeIndex >= PRIMES_LENGTH) {
            return;
        }
    } else if (hash->count < hash->lowWaterMark) {
        if (--newPrimeIndex < 0) {
            return;
        }
    } else {
        return;
    }

    _uhash_allocate(hash, newPrimeIndex, &status);

    if (U_FAILURE(status)) {
        hash->elements = old;
        hash->length = oldLength;       
        return;
    }

    for (i = oldLength - 1; i >= 0; --i) {
        if (!IS_EMPTY_OR_DELETED(old[i].hashcode)) {
            UHashElement *e = _uhash_find(hash, old[i].key, old[i].hashcode);
            assert(e != NULL);
            assert(e->hashcode == HASH_EMPTY);
            e->key = old[i].key;
            e->value = old[i].value;
            e->hashcode = old[i].hashcode;
            ++hash->count;
        }
    }

    uprv_free(old);
}

/**
 * Look for a key in the table, or if no such key exists, the first
 * empty slot matching the given hashcode.  Keys are compared using
 * the keyComparator function.
 *
 * First find the start position, which is the hashcode modulo
 * the length.  Test it to see if it is:
 *
 * a. identical:  First check the hash values for a quick check,
 *    then compare keys for equality using keyComparator.
 * b. deleted
 * c. empty
 *
 * Stop if it is identical or empty, otherwise continue by adding a
 * "jump" value (moduloing by the length again to keep it within
 * range) and retesting.  For efficiency, there need enough empty
 * values so that the searchs stop within a reasonable amount of time.
 * This can be changed by changing the high/low water marks.
 *
 * In theory, this function can return NULL, if it is full (no empty
 * or deleted slots) and if no matching key is found.  In practice, we
 * prevent this elsewhere (in uhash_put) by making sure the last slot
 * in the table is never filled.
 *
 * The size of the table should be prime for this algorithm to work;
 * otherwise we are not guaranteed that the jump value (the secondary
 * hash) is relatively prime to the table length.
 */
static UHashElement*
_uhash_find(const UHashtable *hash, const void* key,
            int32_t hashcode) {

    int32_t firstDeleted = -1;  /* assume invalid index */
    int32_t theIndex, startIndex;
    int32_t jump = 0; /* lazy evaluate */
    int32_t tableHash;

    hashcode &= 0x7FFFFFFF; /* must be positive */
    startIndex = theIndex = (hashcode ^ 0x4000000) % hash->length;

    do {
        tableHash = hash->elements[theIndex].hashcode;
        if (tableHash == hashcode) {          /* quick check */
            if ((*hash->keyComparator)(key, hash->elements[theIndex].key)) {
                return &(hash->elements[theIndex]);
            }
        } else if (!IS_EMPTY_OR_DELETED(tableHash)) {
            /* We have hit a slot which contains a key-value pair,
             * but for which the hash code does not match.  Keep
             * looking.
             */
        } else if (tableHash == HASH_EMPTY) { /* empty, end o' the line */
            break;
        } else if (firstDeleted < 0) { /* remember first deleted */
            firstDeleted = theIndex;
        }
        if (jump == 0) { /* lazy compute jump */
            /* The jump value must be relatively prime to the table
             * length.  As long as the length is prime, then any value
             * 1..length-1 will be relatively prime to it.
             */
            jump = (hashcode % (hash->length - 1)) + 1;
        }
        theIndex = (theIndex + jump) % hash->length;
    } while (theIndex != startIndex);

    if (firstDeleted >= 0) {
        theIndex = firstDeleted; /* reset if had deleted slot */
    } else if (tableHash != HASH_EMPTY) {
        /* We get to this point if the hashtable is full (no empty or
         * deleted slots), and we've failed to find a match.  THIS
         * WILL NEVER HAPPEN as long as uhash_put() makes sure that
         * count is always < length.
         */
        assert(FALSE);
        return NULL; /* Never happens if uhash_put() behaves */
    }
    return &(hash->elements[theIndex]);
}

static void*
_uhash_setElement(UHashtable *hash, UHashElement* e,
                  int32_t hashcode, void* key, void* value) {

    void* oldKey = e->key;
    void* oldValue = e->value;
    if (hash->keyDeleter != NULL && oldKey != NULL &&
        oldKey != key) { /* Avoid double deletion */
        (*hash->keyDeleter)(oldKey);
    }
    if (oldValue == value) { /* Avoid double deletion */
        oldValue = NULL;
    }
    if (hash->valueDeleter != NULL && oldValue != NULL) {
        (*hash->valueDeleter)(oldValue);
        oldValue = NULL;
    }
    e->key = key;
    e->value = value;
    e->hashcode = hashcode;
    return oldValue;
}

/**
 * Assumes that the given element is not empty or deleted.
 */
static void*
_uhash_internalRemoveElement(UHashtable *hash, UHashElement* e) {
    assert(!IS_EMPTY_OR_DELETED(e->hashcode));
    --hash->count;
    return _uhash_setElement(hash, e, HASH_DELETED, NULL, NULL);
}

static void
_uhash_internalSetResizePolicy(UHashtable *hash, enum UHashResizePolicy policy) {
    assert(hash == 0);
    assert(((int32_t)policy) >= 0);
    assert(((int32_t)policy) < 3);
    hash->lowWaterRatio  = RESIZE_POLICY_RATIO_TABLE[policy * 2];
    hash->highWaterRatio = RESIZE_POLICY_RATIO_TABLE[policy * 2 + 1];
}
