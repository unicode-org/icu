/*
**********************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File uhash.c
*
* Modification History:
*
*   Date        Name        Description
*   03/12/99    stephen     Creation.
*******************************************************************************
*/

#include "uhash.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cmemory.h"

/* Private function prototypes */
void
uhash_initialize(UHashtable *hash,
         int32_t primeIndex,
         UErrorCode *status);

int32_t 
uhash_leastGreaterPrimeIndex(int32_t source);

void
uhash_rehash(UHashtable *hash,
         UErrorCode *status);

void
uhash_putInternal(UHashtable *hash,
          int32_t hashCode, 
          void *value);

int32_t 
uhash_find(const UHashtable *hash,
       int32_t hashCode);



/*
  INVARIANT: the size of the table MUST be prime for this algorithm to work!
  Prime table can be tuned for different performance/storage characteristics
  We avoid computing primes by precomputing a table that we use.
*/
int32_t UHASH_PRIMES [] =
{
  17, 37, 67, 131, 257,
  521, 1031, 2053, 4099, 8209, 16411, 32771, 65537,
  131101, 262147, 524309, 1048583, 2097169, 4194319, 8388617, 16777259,
  33554467, 67108879, 134217757, 268435459, 536870923, 1073741827, 2147483647
};

#define UHASH_PRIMES_LENGTH 28

#define UHASH_SLOT_DELETED      ((int32_t) 0x80000000)
#define UHASH_SLOT_EMPTY      ((int32_t) UHASH_SLOT_DELETED + 1)
#define UHASH_MAX_UNUSED      ((int32_t) UHASH_SLOT_EMPTY)

/*
  INVARIANTS:
  DELETED <= MAX_UNUSED
  EMPTY <= MAX_UNUSED
  Any hash > MAX_UNUSED*
  * hashcodes may not start out this way, but internally, 
  they are adjusted so that they are always positive, and this is always true.
  Note here that we are assuming 32-bit ints.
*/

U_CAPI UHashtable*
uhash_open(UHashFunction func,
       UErrorCode *status)
{
  UHashtable* myUHT =  uhash_openSize(func, 3, status);
  if (U_SUCCESS(*status)) myUHT->isGrowable = TRUE;

  return myUHT;
}

U_CAPI UHashtable*
uhash_openSize(UHashFunction func,
	       int32_t size,
	       UErrorCode *status)
{
  UHashtable *result;
  
  if(U_FAILURE(*status)) return NULL;
  
  result = (UHashtable*) uprv_malloc(sizeof(UHashtable));
  if(result == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }
  
  result->highWaterFactor     = 0.5F;
  result->lowWaterFactor     = 0.0F;
  result->hashFunction         = func;
  result->valueDelete        = NULL;
  result->toBeDeleted        = NULL;
  result->toBeDeletedCount    = 0;
  result->isGrowable    = FALSE;

  uhash_initialize(result, uhash_leastGreaterPrimeIndex(size), status);

  if(U_FAILURE(*status)) {
    uprv_free(result);
    return 0;
  }

  return result;
}

U_CAPI void
uhash_setValueDeleter(UHashtable *hash, ValueDeleter del )
{
    hash->valueDelete = del;
}

U_CAPI void
uhash_close(UHashtable *hash)
{
  if (hash->valueDelete)
  {
      ValueDeleter my_free = hash->valueDelete;
      void** vals = hash->values;
      void** toBeDeleted = hash->toBeDeleted;
      int32_t i;
      int32_t count = hash->count;
      int32_t toBeDeletedCount = hash->toBeDeletedCount;
      for (i = 0; i < count; i++)  my_free(vals[i]);
      while (toBeDeletedCount--)    my_free(toBeDeleted[toBeDeletedCount]);

  }
  uprv_free(hash->values);
  uprv_free(hash->hashes);
  uprv_free(hash->toBeDeleted);
}

U_CAPI int32_t
uhash_size(const UHashtable *hash)
{
  return hash->count;
}
U_CAPI int32_t
uhash_putKey(UHashtable *hash,
         int32_t valueKey,
         void *value,
         UErrorCode *status)
{
  /* Put finds the position in the table for the new value. */
  
  int32_t hashCode;
  int32_t index;
  
  if(U_FAILURE(*status)) return UHASH_INVALID;

  if(hash->count > hash->highWaterMark) {
    if (hash->isGrowable)    uhash_rehash(hash, status);
    else  {
      *status = U_INDEX_OUTOFBOUNDS_ERROR;
      return UHASH_INVALID;
    }
  }

  hashCode     = valueKey;
  index     = uhash_find(hash, hashCode);
  
  /* deleted or empty */
  if(hash->hashes[index] <= UHASH_MAX_UNUSED) {
    /* make new object */
    hash->hashes[index] = hashCode;
    ++(hash->count);
  }
  
  /* delete old value? */
  if (hash->valueDelete) 
    {
      void * result = hash->values[index];
    if (result != value) /*Make sure the same object isn't scheduled for a double deletion*/
      {
        hash->toBeDeleted = (void**) uprv_realloc(hash->toBeDeleted, sizeof(void*)*(++(hash->toBeDeletedCount)));
        hash->toBeDeleted[(hash->toBeDeletedCount)-1] = result;
      }
      hash->values[index] = 0;
    }
  
  
  /* store value */
  hash->values[index] = value;
  
  /* return the hash code to the user */
  return hashCode;
}

U_CAPI int32_t
uhash_put(UHashtable *hash,
      void *value,
      UErrorCode *status)
{
  /* Put finds the position in the table for the new value. */
  
  int32_t hashCode;
  int32_t index;
  
  if(U_FAILURE(*status)) return UHASH_INVALID;

  if(hash->count > hash->highWaterMark) {
    if (hash->isGrowable)    uhash_rehash(hash, status);
    else  {
      *status = U_INDEX_OUTOFBOUNDS_ERROR;
      return UHASH_INVALID;
    }
  }

  hashCode     = (hash->hashFunction)(value);
  index     = uhash_find(hash, hashCode);
  
  /* deleted or empty */
  if(hash->hashes[index] <= UHASH_MAX_UNUSED) {
    /* make new object */
    hash->hashes[index] = hashCode;
    ++(hash->count);
  }
  
  /* delete old value? */
  if (hash->valueDelete) 
    {
      void* result = hash->values[index];
    if (result != value) /*Make sure the same object isn't scheduled for a double deletion*/
      {
        hash->toBeDeleted = (void**) uprv_realloc(hash->toBeDeleted,
                         sizeof(void*)*(++(hash->toBeDeletedCount)));
        hash->toBeDeleted[(hash->toBeDeletedCount)-1] = result;
      }
      hash->values[index] = 0;
    }
  
    
  /* store value */
  hash->values[index] = value;

  /* return the hash code to the user */
  return hashCode;
}

U_CAPI void*
uhash_get(const UHashtable *hash, 
      int32_t key)
{
	/* srl: Shouldn't we check to see if hash->hashes[uhash_find(hash, key)] == UHASH_SLOT_DELETED ?
		Perhaps in theory hash->values[...] should have been set to 0, but can we depend
		on this?
	 */

  void *result         = hash->values[uhash_find(hash, key)];
  return result;  
}

U_CAPI void*
uhash_remove(UHashtable *hash,
         int32_t key,
         UErrorCode *status)
{
  /*
    First find the position of the key in the table
    If the object has not be removed already, remove it.
    We have to put a special value in that position that means that
    something has been deleted, since when we do a find,
    we have to continue PAST any deleted values
  */
  int32_t index     = uhash_find(hash, key);
  void *result         = 0;

  /* neither deleted nor empty */
  if(hash->hashes[index] > UHASH_MAX_UNUSED) {
    /* set to deleted */
    hash->hashes[index] = UHASH_SLOT_DELETED;
    result = hash->values[index];

    /* delete old value? */
    if (hash->valueDelete) 
    {
        hash->valueDelete(result);
    }
    hash->values[index] = 0; /* srl .. always null out the value even if there's no deletor!! */

    --(hash->count);

    if(hash->count < hash->lowWaterMark) {
      uhash_rehash(hash, status);
    }
  }
  
  return result;
}

U_CAPI void*
uhash_nextElement(const UHashtable *hash,
          int32_t *pos)
{
  /*
    Walk through the array until you find an element that is not EMPTY and 
    not DELETED
  */
  
  int32_t i;
  void *value;
  
  for(i = *pos + 1; i < hash->length; ++i) {
    if(hash->hashes[i] > UHASH_MAX_UNUSED) {
      *pos = i;
      value = hash->values[i];
      return value;
    }
  }
  
  /* No more elements */
  return 0; 
}

/* ================================================== */
/* Private functions */
/* ================================================== */
void
uhash_initialize(UHashtable *hash,
         int32_t primeIndex,
         UErrorCode *status)
{
  int32_t i;
  
  if(U_FAILURE(*status)) return;

  if(primeIndex < 0) {
    primeIndex = 0;
  }
  else if(primeIndex >= UHASH_PRIMES_LENGTH) {
    primeIndex = UHASH_PRIMES_LENGTH - 1;
  }
  
  hash->primeIndex     = primeIndex;
  hash->length         = UHASH_PRIMES[primeIndex];

  hash->values         = (void**) uprv_malloc(sizeof(void*) * hash->length);
  if(hash->values == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }

  hash->hashes         = (int32_t*) uprv_malloc(sizeof(int32_t) * hash->length);
  if(hash->values == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    uprv_free(hash->values);
    return;
  }

  for(i = 0; i < hash->length; ++i) {
    hash->hashes[i]     = UHASH_SLOT_EMPTY;
    hash->values[i]     = 0;
  }
  
  hash->count         = 0;
  hash->lowWaterMark     = (int32_t)(hash->length * hash->lowWaterFactor);
  hash->highWaterMark     = (int32_t)(hash->length * hash->highWaterFactor);
}

int32_t 
uhash_leastGreaterPrimeIndex(int32_t source)
{
  int32_t i;
  for(i = 0; i < UHASH_PRIMES_LENGTH; ++i) {
    if(source < UHASH_PRIMES[i]) {
      break;
    }
  }
  return (i == 0 ? 0 : (i - 1));
}

void
uhash_rehash(UHashtable *hash,
         UErrorCode *status)
{
  /*
    Rebuild the table from the start. This clears out deadwood, in case
    we have a lot of deleted values. See Find
    It is also used when the table grows or shrinks a lot.
    INVARIANT: The size of the table MUST be prime for this algorithm to work!
  */
  
  void         **oldValues     = hash->values;
  int32_t     *oldHashList     = hash->hashes;
  int32_t     old_length     = hash->length;
  int32_t     newPrimeIndex     = hash->primeIndex;
  int32_t     i;

  if(U_FAILURE(*status)) return;

  if(hash->count > hash->highWaterMark) {
    ++newPrimeIndex;
  }
  else if(hash->count < hash->lowWaterMark) {
    newPrimeIndex -= 2;
  }

  uhash_initialize(hash, newPrimeIndex, status);
  for(i = old_length - 1; i >= 0; --i) {
    void *value = oldValues[i];
    if(value != 0) {
      uhash_putInternal(hash, oldHashList[i], value);
    }
  }
  
  uprv_free(oldValues);
  uprv_free(oldHashList);
}

void
uhash_putInternal(UHashtable *hash,
          int32_t hashCode, 
          void *value)
{
  int32_t index = uhash_find(hash, hashCode);
  if(hash->hashes[index] <= UHASH_MAX_UNUSED) {  
    /* deleted or empty */
    hash->hashes[index] = hashCode;               
    ++(hash->count);
  }
  
  /* reset value */
  hash->values[index] = value; 
}

int32_t 
uhash_find(const UHashtable *hash,
       int32_t hashCode)
{
  /*
    This is the key routine. It looks for a particular key in the following 
    way. First find the start position, which is basically the key modulo the
    length. Test it to see if it is 
    a. Identical (same hash values)
    b. Deleted
    c. Empty
    Stop if it is identical or empty, otherwise continue by adding a "jump"
    value (moduloing by the length again to keep it within range) and
    retesting. For efficiency, it needs enough empty values so that the
    searches stop within a reasonable amount of time. This can be changed by
    changing the high/low water marks.
    INVARIANT: the size of the table MUST be prime for this algorithm to work!
  */

  int32_t firstDeleted     = -1; 
  int32_t index     = (hashCode ^ 0x4000000) % hash->length;
  int32_t jump         = 0;

  while(TRUE) {
    int32_t tableHash = hash->hashes[index];

    /* Compare hash codes */
    if(tableHash == hashCode) {     
      return index;
    } 
    
    /* neither correct nor unused */
    else if(tableHash > UHASH_MAX_UNUSED) {   
      /* ignore */
    }

    /* empty, end o' the line */
    else if(tableHash == UHASH_SLOT_EMPTY) {   
      if(firstDeleted >= 0) {
    /* reset if had deleted slot */
    index = firstDeleted;   
      }
      return index;
    }
    
    /* remember first deleted */
    else if(firstDeleted < 0) { 
      firstDeleted = index;
    }
    
    /* lazy compute jump */
    if(jump == 0) {
      jump = (hashCode % (hash->length - 1)) + 1;
    }
    
    index = (index + jump) % hash->length;
  }

  /* This never happens -- just make the compiler happy */
  return -1;
}

/* Predefined hash functions */

U_CAPI int32_t
uhash_hashUString(const void *parm)
{
  if(parm != NULL) {
    const UChar *key     = (const UChar*) parm;
    int32_t len         = u_strlen(key);
    int32_t hash         = UHASH_INVALID;
    const UChar *limit     = key + len;

    /*
      We compute the hash by iterating sparsely over 64 (at most) characters
      spaced evenly through the string.  For each character, we multiply the
      previous hash value by a prime number and add the new character in,
      in the manner of a additive linear congruential random number generator,
      thus producing a pseudorandom deterministic value which should be well
      distributed over the output range. [LIU]
    */

    if(len<=64) {
      while(key < limit) {
        hash = (hash * 37) + *key++;
      }
    } else {
      int32_t inc = (len+63)/64;

      while(key < limit) {
        hash = (hash * 37) + *key;
        key += inc;
      }
    }
  
    hash &= 0x7FFFFFFF;
    if(hash == UHASH_INVALID) {
      hash = UHASH_EMPTY;
    }
    return hash;
  } else {
    return UHASH_INVALID;
  }
}

U_CAPI int32_t
uhash_hashString(const void *parm)
{
  if(parm != NULL) {
    const char *key     = (const char*) parm;
    int32_t len         = uprv_strlen(key);
    int32_t hash         = UHASH_INVALID;
    const char *limit     = key + len;

    /*
      We compute the hash by iterating sparsely over 64 (at most) characters
      spaced evenly through the string.  For each character, we multiply the
      previous hash value by a prime number and add the new character in,
      in the manner of a additive linear congruential random number generator,
      thus producing a pseudorandom deterministic value which should be well
      distributed over the output range. [LIU]
    */

    if(len<=64) {
      while(key < limit) {
        hash = (hash * 37) + *key++;
      }
    } else {
      int32_t inc = (len+63)/64;

      while(key < limit) {
        hash = (hash * 37) + *key;
        key += inc;
      }
    }
  
    hash &= 0x7FFFFFFF;
    if(hash == UHASH_INVALID) {
      hash = UHASH_EMPTY;
    }
    return hash;
  } else {
    return UHASH_INVALID;
  }
}

U_CAPI int32_t
uhash_hashIString(const void *parm)
{
  if(parm != NULL) {
    const char *key     = (const char*) parm;
    int32_t len         = uprv_strlen(key);
    int32_t hash         = UHASH_INVALID;
    const char *limit     = key + len;

    /* same as uhash_hashString(), but uses uprv_tolower(characters) */

    if(len<=64) {
      while(key < limit) {
        hash = (hash * 37) + uprv_tolower(*key++);
      }
    } else {
      int32_t inc = (len+63)/64;

      while(key < limit) {
        hash = (hash * 37) + uprv_tolower(*key);
        key += inc;
      }
    }

    hash &= 0x7FFFFFFF;
    if(hash == UHASH_INVALID) {
      hash = UHASH_EMPTY;
    }
    return hash;
  } else {
    return UHASH_INVALID;
  }
}

U_CAPI int32_t
uhash_hashLong(const void *parm)
{
  int32_t hash = (int32_t) parm & 0x7FFFFFFF;
  if(hash == UHASH_INVALID) {
    hash = UHASH_EMPTY;
  }
  return hash;
}
