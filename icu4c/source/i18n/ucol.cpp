/*
*******************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#include "unicode/ucol.h"

#include "unicode/uloc.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ustring.h"
#include "unicode/normlzr.h"
#include "cpputils.h"

#define UCOL_LEVELTERMINATOR 0
#define UCOL_IGNORABLE 0x0000
#define UCOL_CHARINDEX 0x70000000             // need look up in .commit()
#define UCOL_EXPANDCHARINDEX 0x7E000000       // Expand index follows
#define UCOL_CONTRACTCHARINDEX 0x7F000000     // contract indexes follows
#define UCOL_UNMAPPED 0xFFFFFFFF              // unmapped character values
#define UCOL_PRIMARYORDERINCREMENT 0x00010000 // primary strength increment
#define UCOL_SECONDARYORDERINCREMENT 0x00000100 // secondary strength increment
#define UCOL_TERTIARYORDERINCREMENT 0x00000001 // tertiary strength increment
#define UCOL_MAXIGNORABLE 0x00010000          // maximum ignorable char order value
#define UCOL_PRIMARYORDERMASK 0xffff0000      // mask off anything but primary order
#define UCOL_SECONDARYORDERMASK 0x0000ff00    // mask off anything but secondary order
#define UCOL_TERTIARYORDERMASK 0x000000ff     // mask off anything but tertiary order
#define UCOL_SECONDARYRESETMASK 0x0000ffff    // mask off secondary and tertiary order
#define UCOL_IGNORABLEMASK 0x0000ffff         // mask off ignorable char order
#define UCOL_PRIMARYDIFFERENCEONLY 0xffff0000 // use only the primary difference
#define UCOL_SECONDARYDIFFERENCEONLY 0xffffff00  // use only the primary and secondary difference
#define UCOL_PRIMARYORDERSHIFT 16             // primary order shift
#define UCOL_SECONDARYORDERSHIFT 8            // secondary order shift
#define UCOL_SORTKEYOFFSET 1                  // minimum sort key offset
#define UCOL_CONTRACTCHAROVERFLOW 0x7FFFFFFF  // Indicates the char is a contract char

U_CAPI int32_t
u_normalize(const UChar*            source,
        int32_t                 sourceLength, 
        UNormalizationMode      mode, 
        int32_t                 option,
        UChar*                  result,
        int32_t                 resultLength,
        UErrorCode*             status)
{
  if(U_FAILURE(*status)) return -1;

  Normalizer::EMode normMode;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    normMode = Normalizer::NO_OP;
    break;
  case UCOL_DECOMP_CAN:
    normMode = Normalizer::DECOMP;
    break;
  case UCOL_DECOMP_COMPAT:
    normMode = Normalizer::DECOMP_COMPAT;
    break;
  case UCOL_DECOMP_CAN_COMP_COMPAT:
    normMode = Normalizer::COMPOSE;
    break;
  case UCOL_DECOMP_COMPAT_COMP_CAN:
    normMode = Normalizer::COMPOSE_COMPAT;
    break;
  default:
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return -1;
  }

  int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);
  const UnicodeString src((UChar*)source, len, len);
  UnicodeString dst(result, 0, resultLength);
  Normalizer::normalize(src, normMode, option, dst, *status);
  int32_t actualLen;
  T_fillOutputParams(&dst, result, resultLength, &actualLen, status);
  return actualLen;
}

U_CAPI UCollator*
ucol_open(    const    char         *loc,
        UErrorCode      *status)
{
  if(U_FAILURE(*status)) return 0;

  Collator *col = 0;

  if(loc == 0) 
    col = Collator::createInstance(*status);
  else
    col = Collator::createInstance(Locale(loc), *status);

  if(col == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UCollator*)col;
}

U_CAPI UCollator*
ucol_openRules(    const    UChar                  *rules,
        int32_t                 rulesLength,
        UNormalizationMode      mode,
        UCollationStrength      strength,
        UErrorCode              *status)
{
  if(U_FAILURE(*status)) return 0;

  int32_t len = (rulesLength == -1 ? u_strlen(rules) : rulesLength);
  const UnicodeString ruleString((UChar*)rules, len, len);

  Normalizer::EMode normMode;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    normMode = Normalizer::NO_OP;
    break;
  case UCOL_DECOMP_CAN:
    normMode = Normalizer::DECOMP;
    break;
  case UCOL_DECOMP_COMPAT:
    normMode = Normalizer::DECOMP_COMPAT;
    break;
  case UCOL_DECOMP_CAN_COMP_COMPAT:
    normMode = Normalizer::COMPOSE;
    break;
  case UCOL_DECOMP_COMPAT_COMP_CAN:
    normMode = Normalizer::COMPOSE_COMPAT;
    break;
  default:
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  }

  RuleBasedCollator *col = 0;
  col = new RuleBasedCollator(ruleString, 
                  (Collator::ECollationStrength) strength, 
                  normMode, 
                  *status);

  if(col == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UCollator*) col;
}

U_CAPI void
ucol_close(UCollator *coll)
{
  delete (Collator*)coll;
}

U_CAPI UCollationResult
ucol_strcoll(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    if (coll == NULL) return UCOL_EQUAL;
    if (sourceLength == -1) sourceLength = u_strlen(source);
    if (targetLength == -1) targetLength = u_strlen(target);
        return (UCollationResult) ((Collator*)coll)->compare(source,sourceLength,target,targetLength);
}

U_CAPI UCollationResult
ucol_strcollEx(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
	return UCOL_EQUAL;
}


U_CAPI UBool
ucol_greater(    const    UCollator        *coll,
        const    UChar            *source,
        int32_t            sourceLength,
        const    UChar            *target,
        int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      == UCOL_GREATER);
}

U_CAPI UBool
ucol_greaterOrEqual(    const    UCollator    *coll,
            const    UChar        *source,
            int32_t        sourceLength,
            const    UChar        *target,
            int32_t        targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      != UCOL_LESS);
}

U_CAPI UBool
ucol_equal(        const    UCollator        *coll,
            const    UChar            *source,
            int32_t            sourceLength,
            const    UChar            *target,
            int32_t            targetLength)
{
  return (ucol_strcoll(coll, source, sourceLength, target, targetLength) 
      == UCOL_EQUAL);
}

U_CAPI UCollationStrength
ucol_getStrength(const UCollator *coll)
{
  return (UCollationStrength) ((Collator*)coll)->getStrength();
}

U_CAPI void
ucol_setStrength(    UCollator                *coll,
            UCollationStrength        strength)
{
  ((Collator*)coll)->setStrength((Collator::ECollationStrength)strength);
}

U_CAPI UNormalizationMode
ucol_getNormalization(const UCollator* coll)
{
  switch(((Collator*)coll)->getDecomposition()) {
  case Normalizer::NO_OP:
    return UCOL_NO_NORMALIZATION;

  case Normalizer::COMPOSE:
    return UCOL_DECOMP_COMPAT_COMP_CAN;

  case Normalizer::COMPOSE_COMPAT:
    return UCOL_DECOMP_CAN_COMP_COMPAT;

  case Normalizer::DECOMP:
    return UCOL_DECOMP_COMPAT;

  case Normalizer::DECOMP_COMPAT:
    return UCOL_DECOMP_COMPAT;

  }
  return UCOL_NO_NORMALIZATION;
}

U_CAPI void
ucol_setNormalization(  UCollator            *coll,
            UNormalizationMode    mode)
{
  Normalizer::EMode normMode;
  switch(mode) {
  case UCOL_NO_NORMALIZATION:
    normMode = Normalizer::NO_OP;
    break;
  case UCOL_DECOMP_CAN:
    normMode = Normalizer::DECOMP;
    break;
  case UCOL_DECOMP_COMPAT:
    normMode = Normalizer::DECOMP_COMPAT;
    break;
  case UCOL_DECOMP_COMPAT_COMP_CAN:
    normMode = Normalizer::COMPOSE;
    break;
  case UCOL_DECOMP_CAN_COMP_COMPAT:
    normMode = Normalizer::COMPOSE_COMPAT;
    break;
  default:
    /* Shouldn't get here. */
    /* *status = U_ILLEGAL_ARGUMENT_ERROR; */
    return;
  }

  ((Collator*)coll)->setDecomposition(normMode);
}

U_CAPI int32_t
ucol_getDisplayName(    const    char        *objLoc,
            const    char        *dispLoc,
            UChar             *result,
            int32_t         resultLength,
            UErrorCode        *status)
{
  if(U_FAILURE(*status)) return -1;

  UnicodeString dst(result, resultLength, resultLength);
  Collator::getDisplayName(Locale(objLoc), Locale(dispLoc), dst);
  int32_t actLen;
  T_fillOutputParams(&dst, result, resultLength, &actLen, status);
  return actLen;
}

U_CAPI const char*
ucol_getAvailable(int32_t index)
{
  return uloc_getAvailable(index);
}

U_CAPI int32_t
ucol_countAvailable()
{
  return uloc_countAvailable();
}

U_CAPI const UChar*
ucol_getRules(    const    UCollator        *coll, 
        int32_t            *length)
{
  const UnicodeString& rules = ((RuleBasedCollator*)coll)->getRules();
  *length = rules.length();
  return rules.getUChars();
}

#include "unicode/normlzr.h"
#include "ucmp32.h"
#include "tcoldata.h"
#include "tables.h"

struct collIterate {
    // We might need a place to point to normalized string. Normalization should be done before the processing
  UChar *string; // Original string
  uint32_t len;   // Original string length
  uint32_t pos; // This is position in the string
  uint32_t toReturn; // This is the CE from CEs buffer that should be returned
  uint32_t CEpos; // This is the position to which we have stored processed CEs
  uint32_t orderMask; // This is the mask for different weights
  uint32_t CEs[1024]; // This is where we store CEs
};

static uint8_t utf16fixup[32] = {
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0x20, 0xf8, 0xf8, 0xf8, 0xf8
};

// This will get the next CE(s)?
// Should be part macro, part function
int32_t ucol_getNextCE(const UCollator *coll, collIterate *source, UErrorCode *status) {

  if (U_FAILURE(*status) || (source->pos>=source->len && source->CEpos <= source->toReturn)) {
    return CollationElementIterator::NULLORDER;
  }
  
  if (source->CEpos > source->toReturn) {
    return (source->CEs[source->toReturn++] & source->orderMask);
  }

  source->CEs[source->CEpos]  = ucmp32_get(((RuleBasedCollator *)coll)->data->mapping, source->string[source->pos]);

  // this should benefit from reordering of the clauses, so that the cleanest case is returned the first.

  if(source->CEs[source->CEpos] < UCOL_EXPANDCHARINDEX) {
      
    source->CEpos++;
    source->pos++;

    return (source->CEs[source->toReturn++] & source->orderMask);
  }

  if (source->CEs[source->CEpos] == UCOL_UNMAPPED) {
      // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (source->string[source->pos] == 0x0000) return source->string[source->pos++]; // \u0000 is not valid in C++'s UnicodeString
	source->CEs[source->CEpos++] = CollationElementIterator::UNMAPPEDCHARVALUE;
	source->CEs[source->CEpos] = (source->string[source->pos])<<16;
    } else {
        // Contraction sequence start...
        if (source->CEs[source->CEpos] >= UCOL_CONTRACTCHARINDEX) {
	      // in place of: value = nextContractChar(cursor, ch, status);
            VectorOfPToContractElement* list = ((RuleBasedCollator *)coll)->data->contractTable->at(source->CEs[source->CEpos]-UCOL_CONTRACTCHARINDEX);
            // The upper line obtained a list of contracting sequences.
            EntryPair *pair = (EntryPair *)list->at(0); // Taking out the first one.
            int32_t order = pair->value; // This got us mapping for just the first element - the one that signalled a contraction.

            UChar key[1024];
            uint32_t posKey = 0;

            key[posKey++] = source->string[source->pos++];
            int32_t getEntryValue = RuleBasedCollator::UNMAPPED;

            while(source->pos<source->len) {

                key[posKey++] = source->string[source->pos];

                // in place of: int32_t n = getEntry(list, key, TRUE);
                {
                    int32_t i;
                    if (list != NULL)
                    {
                        for (i = 0; i < list->size(); i++)
                        {
                            EntryPair *pair = list->at(i);

                            if ((pair != NULL) && (pair->fwd == TRUE /*fwd*/) && (pair->entryName == UnicodeString(key, posKey)))
                            {
                                getEntryValue  = i;
                                goto done;
                                // break or something
                            }
                        }
                    }
                    getEntryValue = RuleBasedCollator::UNMAPPED;
                }
    done:
                // end of getEntry

                if (getEntryValue  == RuleBasedCollator::UNMAPPED)
                {
                    break;
                }

                source->pos++;
                pair = (EntryPair *)list->at(getEntryValue);
                order = pair->value;
        }
        source->CEs[source->CEpos++] = order;
        return (source->CEs[source->toReturn++] & source->orderMask);
    }
	// Expansion sequence start...
        if (source->CEs[source->CEpos] >= UCOL_EXPANDCHARINDEX) {
            VectorOfInt *v = ((RuleBasedCollator *)coll)->data->expandTable->at(source->CEs[source->CEpos]-UCOL_EXPANDCHARINDEX);
            if(v != NULL) {
                int32_t expandindex=0;
                while(expandindex < v->size()) {
                    source->CEs[source->CEpos++] = v->at(expandindex++);
                }
                source->pos++;
                return (source->CEs[source->toReturn++] & source->orderMask);
            }
        }
	// Thai/Lao reordering
        if (CollationElementIterator::isThaiPreVowel(source->string[source->pos])) {
            UChar consonant = source->string[source->pos+1];
            if (CollationElementIterator::isThaiBaseConsonant(consonant)) {
	      source->pos++;
	      // find the element for consonant
	      // and reorder them
            }
        }
    }

    source->CEpos++;
    source->pos++;

    return (source->CEs[source->toReturn++] & source->orderMask);
}

#define UCOL_MAX_BUFFER 1

U_CAPI int32_t
ucol_getSortKeyEx(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength)
{

    uprv_memset(result, 0xAA, resultLength); // for debug purposes


    /* 
    Still problems in:
    SUMMARY:
        ******* [Total error count:     213]
         Errors in
           [tscoll/capitst/TestSortKey]  // this is normal, since we are changing binary keys
           [tscoll/cfrtst/TestSecondary] // this is also OK, ICU original implementation was messed up
           [tscoll/cfrtst/TestTertiary]  // probably the same as above
           [tscoll/cjacoll/TestTertiary] // most probably due to normalization...
           [tscoll/cg7coll/TestDemo4]    // need to check
         Total errors: 213
    */

    uint32_t i = 0; // general purpose counter

	UErrorCode status = U_ZERO_ERROR;

    uint8_t prim[2*UCOL_MAX_BUFFER], second[UCOL_MAX_BUFFER], tert[UCOL_MAX_BUFFER];

    uint8_t *primaries = prim, *secondaries = second, *tertiaries = tert;

    UChar normBuffer[2*UCOL_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = 2048;

	int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);

    UBool  compareSec   = (((RuleBasedCollator *)coll)->getStrength() >= Collator::SECONDARY);
    UBool  compareTer   = (((RuleBasedCollator *)coll)->getStrength() >= Collator::TERTIARY);
    UBool  compareIdent = (((RuleBasedCollator *)coll)->getStrength() == Collator::IDENTICAL);

    if(len > UCOL_MAX_BUFFER) {
        primaries = (uint8_t *)uprv_malloc(4*len*sizeof(uint8_t));
        if(compareSec) {
            secondaries = (uint8_t *)uprv_malloc(2*len*sizeof(uint8_t));
        }
        if(compareTer) {
            tertiaries = (uint8_t *)uprv_malloc(2*len*sizeof(uint8_t));
        }
    }

    uint8_t *primstart = primaries;
    uint8_t *secstart = secondaries;
    uint8_t *terstart = tertiaries;

	collIterate s = { (UChar *)source, len, 0, 0, 0, 0xFFFFFFFF };

    // If we need to normalize, we'll do it all at once at the beggining!
    if(((RuleBasedCollator *)coll)->getDecomposition() != Normalizer::NO_OP) {
		UnicodeString normalized;
		Normalizer::normalize(UnicodeString(source, sourceLength), ((RuleBasedCollator *)coll)->getDecomposition(),
			0, normalized, status);
		normSourceLen = normalized.length();

        if(normSourceLen > UCOL_MAX_BUFFER) {
            normSource = (UChar *) uprv_malloc(normSourceLen*sizeof(UChar));
        }

		normalized.extract(0, normSourceLen, normSource);
		s.string = normSource;
		s.len = normSourceLen;
	}

    int32_t order = 0;

    uint16_t primary = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;

    while((order = ucol_getNextCE(coll, &s, &status)) !=
    CollationElementIterator::NULLORDER) {
        primary = ((order & UCOL_PRIMARYORDERMASK)>> UCOL_PRIMARYORDERSHIFT);
        secondary = ((order & UCOL_SECONDARYORDERMASK)>> UCOL_SECONDARYORDERSHIFT);
        tertiary = (order & UCOL_TERTIARYORDERMASK);

        if(primary != UCOL_IGNORABLE) {
            *(primaries++) = (primary+UCOL_SORTKEYOFFSET)>>8;
            *(primaries++) = (primary+UCOL_SORTKEYOFFSET)&0xFF;
            if(compareSec) {
                *(secondaries++) = secondary+UCOL_SORTKEYOFFSET;
            }
            if(compareTer) {
                *(tertiaries++) = tertiary+UCOL_SORTKEYOFFSET;
            }
        } else {
            if(compareSec && secondary != 0) {
                *(secondaries++) = secondary+UCOL_SORTKEYOFFSET;
            }
            if(compareTer && tertiary != 0) {
                *(tertiaries++) = tertiary+UCOL_SORTKEYOFFSET;
            }
        }
    }

    *(primaries++) = UCOL_LEVELTERMINATOR;
    *(primaries++) = UCOL_LEVELTERMINATOR;


    if(compareSec) {
      uint32_t secsize = secondaries-secstart;
      if(((RuleBasedCollator *)coll)->data->isFrenchSec) { // do the reverse copy
          for(i = 0; i<secsize; i++) {
              *(primaries++) = *(secondaries-i-1);
          }
        } else { 
            uprv_memcpy(primaries, secstart, secsize); 
            primaries += secsize;
        }

        *(primaries++) = UCOL_LEVELTERMINATOR;
    }

    if(compareTer) {
      uint32_t tersize = tertiaries - terstart;
      uprv_memcpy(primaries, terstart, tersize);
      primaries += tersize;
      *(primaries++) = UCOL_LEVELTERMINATOR;
    }


    if(compareIdent) {
      for(i = 0; i<s.len; i++) {
          *(primaries++) = (s.string[i] >> 8) + utf16fixup[s.string[i] >> 11];
          *(primaries++) = (s.string[i] & 0xFF);
      }
      *(primaries++) = UCOL_LEVELTERMINATOR;
    }

    uprv_memcpy(result, primstart, uprv_min(resultLength, (primaries-primstart)));

    if(terstart != tert) {
        uprv_free(terstart);
    }
    if(secstart != second) {
        uprv_free(secstart);
    }
    if(primstart != prim) {
        uprv_free(primstart);
    }
    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    return primaries-primstart;
}

U_CAPI int32_t
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength)
{
  int32_t         count;
  const uint8_t*     bytes = NULL;
  CollationKey         key;
  int32_t         copyLen;
    int32_t         len = (sourceLength == -1 ? u_strlen(source) 
                   : sourceLength);
  //  UnicodeString     string((UChar*)source, len, len);
  UErrorCode         status = U_ZERO_ERROR;

  ((Collator*)coll)->getCollationKey(source, len, key, status);
  if(U_FAILURE(status)) 
    return 0;

  bytes = key.getByteArray(count);
  
  copyLen = uprv_min(count, resultLength);
  uprv_arrayCopy((const int8_t*)bytes, (int8_t*)result, copyLen);

  //  if(count > resultLength) {
  //    *status = U_BUFFER_OVERFLOW_ERROR;
  //  }

  return count;
}

U_CAPI int32_t
ucol_keyHashCode(    const    uint8_t*    key, 
            int32_t        length)
{
  CollationKey newKey(key, length);
  return newKey.hashCode();
}

UCollationElements*
ucol_openElements(    const    UCollator            *coll,
            const    UChar                *text,
            int32_t              textLength,
            UErrorCode *status)
{
  int32_t len = (textLength == -1 ? u_strlen(text) : textLength);
  const UnicodeString src((UChar*)text, len, len);

  CollationElementIterator *iter = 0;
  iter = ((RuleBasedCollator*)coll)->createCollationElementIterator(src);
  if(iter == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  return (UCollationElements*) iter;
}

U_CAPI void
ucol_closeElements(UCollationElements *elems)
{
  delete (CollationElementIterator*)elems;
}

U_CAPI void
ucol_reset(UCollationElements *elems)
{
  ((CollationElementIterator*)elems)->reset();
}

U_CAPI int32_t
ucol_next(    UCollationElements    *elems,
        UErrorCode            *status)
{
  if(U_FAILURE(*status)) return UCOL_NULLORDER;

  return ((CollationElementIterator*)elems)->next(*status);
}

U_CAPI int32_t
ucol_previous(    UCollationElements    *elems,
        UErrorCode            *status)
{
  if(U_FAILURE(*status)) return UCOL_NULLORDER;

  return ((CollationElementIterator*)elems)->previous(*status);
}

U_CAPI int32_t
ucol_getMaxExpansion(    const    UCollationElements    *elems,
            int32_t                order)
{
  return ((CollationElementIterator*)elems)->getMaxExpansion(order);
}

U_CAPI void
ucol_setText(UCollationElements        *elems,
         const    UChar                    *text,
         int32_t                    textLength,
         UErrorCode                *status)
{
  if(U_FAILURE(*status)) return;

  int32_t len = (textLength == -1 ? u_strlen(text) : textLength);
  const UnicodeString src((UChar*)text, len, len);

  ((CollationElementIterator*)elems)->setText(src, *status);
}

U_CAPI UTextOffset
ucol_getOffset(const UCollationElements *elems)
{
  return ((CollationElementIterator*)elems)->getOffset();
}

U_CAPI void
ucol_setOffset(    UCollationElements    *elems,
        UTextOffset            offset,
        UErrorCode            *status)
{
  if(U_FAILURE(*status)) return;
  
  ((CollationElementIterator*)elems)->setOffset(offset, *status);
}

U_CAPI void 
ucol_getVersion(const UCollator* coll, 
                UVersionInfo versionInfo) 
{
    ((Collator*)coll)->getVersion(versionInfo);
}

U_CAPI uint8_t *
ucol_cloneRuleData(UCollator *coll, int32_t *length, UErrorCode *status)
{
  return ((RuleBasedCollator*)coll)->cloneRuleData(*length,*status);
}

U_CAPI void ucol_setAttribute(const UCollator *coll, const UColAttribute attr, const UColAttributeValue value, UErrorCode *status) {
	*status = U_UNSUPPORTED_ERROR;
	return;
}

U_CAPI UColAttributeValue ucol_getAttribute(const UCollator *coll, const UColAttribute attr, UErrorCode *status) {
	*status = U_UNSUPPORTED_ERROR;
	return UCOL_ATTR_DEFAULT;
}

U_CAPI UCollator *ucol_safeClone(const UCollator *coll, void *stackBuffer, uint32_t bufferSize, UErrorCode *status) {
	*status = U_UNSUPPORTED_ERROR;
	return NULL;
}

U_CAPI UCollationResult ucol_strcollinc(const UCollator *coll, 
								 UCharForwardIterator *source, void *sourceContext,
								 UCharForwardIterator *target, void *targetContext) {
	return UCOL_EQUAL;
}

U_CAPI int32_t ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen) {
	return 0;
}
