/*
*******************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/

#include "ucolimp.h"

#include "unicode/uloc.h"
#include "unicode/coll.h"
#include "unicode/tblcoll.h"
#include "unicode/coleitr.h"
#include "unicode/ustring.h"
#include "unicode/normlzr.h"
#include "cpputils.h"
#include "cstring.h"

#include <stdio.h>

#include "ucmp32.h"
#include "tcoldata.h"
#include "tables.h"


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
    return UCOL_DECOMP_CAN;

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

inline void *ucol_getABuffer(const UCollator *coll, uint32_t size) {
    return ((RuleBasedCollator *)coll)->getSomeMemory(size);
}

U_CAPI const UChar*
ucol_getRules(    const    UCollator        *coll, 
        int32_t            *length)
{
  const UnicodeString& rules = ((RuleBasedCollator*)coll)->getRules();
  *length = rules.length();
  return rules.getUChars();
}

UCollationResult ucol_compareUsingSortKeys(const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    uint8_t sourceKey[UCOL_MAX_BUFFER], targetKey[UCOL_MAX_BUFFER];
    uint8_t *sourceKeyP = sourceKey;
    uint8_t *targetKeyP = targetKey;
    int32_t sourceKeyLen = UCOL_MAX_BUFFER, targetKeyLen = UCOL_MAX_BUFFER;

    sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    if(sourceKeyLen > UCOL_MAX_BUFFER) {
        sourceKeyP = (uint8_t*)uprv_malloc(sourceKeyLen*sizeof(uint8_t));
        sourceKeyLen = ucol_getSortKey(coll, source, sourceLength, sourceKeyP, sourceKeyLen);
    }

    targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    if(targetKeyLen > UCOL_MAX_BUFFER) {
        targetKeyP = (uint8_t*)uprv_malloc(targetKeyLen*sizeof(uint8_t));
        targetKeyLen = ucol_getSortKey(coll, target, targetLength, targetKeyP, targetKeyLen);
    }

    int32_t result = uprv_strcmp((const char*)sourceKeyP, (const char*)targetKeyP);

    if(sourceKeyP != sourceKey) {
        uprv_free(sourceKeyP);
    }

    if(targetKeyP != targetKey) {
        uprv_free(targetKeyP);
    }

    if(result<0) {
        return UCOL_LESS;
    } else if(result>0) {
        return UCOL_GREATER;
    } else {
        return UCOL_EQUAL;
    }
}


int32_t getComplicatedCE(const UCollator *coll, collIterate *source, UErrorCode *status) {
  if (*(source->CEpos) == UCOL_UNMAPPED) {
      // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (*(source->pos) == 0x0000) return *(source->pos++); // \u0000 is not valid in C++'s UnicodeString
    	*(source->CEpos++) = UCOL_UNMAPPEDCHARVALUE;
	    *(source->CEpos++) = *(source->pos)<<16;
    } else {
        // Contraction sequence start...
        if (*(source->CEpos) >= UCOL_CONTRACTCHARINDEX) {
			UChar key[1024];
			uint32_t posKey = 0;

            VectorOfPToContractElement* list = ((RuleBasedCollator *)coll)->data->contractTable->at(*(source->CEpos)-UCOL_CONTRACTCHARINDEX);
            // The upper line obtained a list of contracting sequences.
            if (list != NULL) {
				EntryPair *pair = (EntryPair *)list->at(0); // Taking out the first one.
				int32_t order = pair->value; // This got us mapping for just the first element - the one that signalled a contraction.

				key[posKey++] = *(source->pos++);
				// This tries to find the longes common match for the data in contraction table...
				// and needs to be rewritten, especially the test down there!
				int32_t i;
                int32_t listSize = list->size();
				UBool foundSmaller = TRUE;
				while(source->pos<source->len && foundSmaller) {
					key[posKey++] = *source->pos;

					foundSmaller = FALSE;
					i = 0;
					while(i<listSize && !foundSmaller) {
						pair = list->at(i);
                        if ((pair != NULL) && (pair->fwd == TRUE /*fwd*/) && (pair->equalTo(key, posKey))) { 
                            /* Found a matching contraction sequence */
                            order = pair->value; /* change the CE value */
                            source->pos++;       /* consume another char from the source */
							foundSmaller = TRUE; 
						}
						i++;

					}
				}
				source->pos--; /* spit back the last char - it wasn't part of the sequence */
				*(source->CEpos) = order;
			}
    }
	// Expansion sequence start...
        if (*(source->CEpos) >= UCOL_EXPANDCHARINDEX) {
            VectorOfInt *v = ((RuleBasedCollator *)coll)->data->expandTable->at(*(source->CEpos)-UCOL_EXPANDCHARINDEX);
            if(v != NULL) {
                int32_t expandindex=0;
                int32_t vSize = v->size();
                while(expandindex < vSize) {
                    *(source->CEpos++) = v->at(expandindex++);
                }
            }
        }

     // Thai/Lao reordering
        if (UCOL_ISTHAIPREVOWEL(*(source->pos)) && 
			UCOL_ISTHAIBASECONSONANT(*(source->pos+1))) {
			if(source->isThai == TRUE) {
				source->isThai = FALSE;
				if((source->len - source->pos) > UCOL_WRITABLE_BUFFER_SIZE) {
					// allocate a new buffer
                    source->writableBuffer = (UChar *)ucol_getABuffer(coll, (source->len - source->pos)*sizeof(UChar));
				} 
				UChar *sourceCopy = source->pos;
				UChar *targetCopy = source->writableBuffer;
				while(sourceCopy < source->len) {
					if(UCOL_ISTHAIPREVOWEL(*(sourceCopy)) && 
						UCOL_ISTHAIBASECONSONANT(*(sourceCopy+1))) {
						*(targetCopy) = *(sourceCopy+1);
						*(targetCopy+1) = *(sourceCopy);
						targetCopy+=2;
						sourceCopy+=2;
					} else {
						*(targetCopy++) = *(sourceCopy++);
					}
				}
				source->pos = source->writableBuffer;
				source->len = targetCopy;
				source->CEpos = source->toReturn = source->CEs;
                return UCOL_IGNORABLE;
            }
        }
    }
    source->pos++;
    return (*(source->toReturn++));
}


/* This is the original function */
U_CAPI UCollationResult
ucol_strcollEx(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
    if (coll == NULL) return UCOL_EQUAL;
    if (sourceLength == -1) sourceLength = u_strlen(source);
    if (targetLength == -1) targetLength = u_strlen(target);
        return (UCollationResult) ((RuleBasedCollator*)coll)->compareEx(source,sourceLength,target,targetLength);
}

void init_incrementalContext(UCharForwardIterator *source, void *sourceContext, incrementalContext *s) {
    s->len = s->stringP = s->stackString ;
    s->capacity = s->stackString+UCOL_MAX_BUFFER;
    s->CEpos = s->toReturn = s->CEs;
    s->source = source;
    s->sourceContext = sourceContext;
    s->currentChar = 0xFFFF;
    s->lastChar = 0xFFFF;
    s->panic = FALSE;
}

int32_t ucol_getIncrementalCE(const UCollator *coll, incrementalContext *ctx, UErrorCode *status) {

  uint32_t order;

  if (U_FAILURE(*status) /*|| (ctx->CEpos <= ctx->toReturn)*/) {
    return UCOL_NULLORDER;
  }
  
  if (ctx->CEpos > ctx->toReturn) {
      return(*(ctx->toReturn++));
  }
 
  ctx->CEpos = ctx->toReturn = ctx->CEs;

  if(ctx->lastChar == 0xFFFF) {
      ctx->currentChar = ctx->source(ctx->sourceContext);
      incctx_appendChar(ctx, ctx->currentChar);
      //*(ctx->len++) = ctx->currentChar;
      if(ctx->currentChar == 0xFFFF) {
          return UCOL_NULLORDER;
      }
  } else {
      ctx->currentChar = ctx->lastChar;
      ctx->lastChar = 0xFFFF;
  }
 
  order  = ucmp32_get(((RuleBasedCollator *)coll)->data->mapping, ctx->currentChar);

  // this should benefit from reordering of the clauses, so that the cleanest case is returned the first.

  if(order < UCOL_EXPANDCHARINDEX && !(UCOL_ISTHAIPREVOWEL(ctx->currentChar))) {     
    return (order);
  }
  if (order == UCOL_UNMAPPED) {
      // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (ctx->currentChar == 0x0000) return ctx->currentChar; // \u0000 is not valid in C++'s UnicodeString
    	//*(ctx->CEpos++) = UCOL_UNMAPPEDCHARVALUE;
        order = UCOL_UNMAPPEDCHARVALUE;
	    *(ctx->CEpos++) = ctx->currentChar<<16;
    } else {
        // Contraction sequence start...
        if (order >= UCOL_CONTRACTCHARINDEX) {
			UChar key[1024];
			uint32_t posKey = 0;

            VectorOfPToContractElement* list = ((RuleBasedCollator *)coll)->data->contractTable->at(order-UCOL_CONTRACTCHARINDEX);
            // The upper line obtained a list of contracting sequences.
            if (list != NULL) {
				EntryPair *pair = (EntryPair *)list->at(0); // Taking out the first one.
				order = pair->value; // This got us mapping for just the first element - the one that signalled a contraction.

				key[posKey++] = ctx->currentChar;
				// This tries to find the longes common match for the data in contraction table...
				// and needs to be rewritten, especially the test down there!
				int32_t i;
                int32_t listSize = list->size();
				UBool foundSmaller = TRUE;
                UBool endOfString = FALSE;
                //*(ctx->len++) = ctx->lastChar;
                incctx_appendChar(ctx, ctx->lastChar);
				while(!endOfString && foundSmaller) {
                    endOfString = ((ctx->lastChar = ctx->source(ctx->sourceContext)) == 0xFFFF);
					key[posKey++] = ctx->lastChar;

					foundSmaller = FALSE;
					i = 0;
					while(i<listSize && !foundSmaller) {
						pair = list->at(i);
						if ((pair != NULL) && (pair->fwd == TRUE /*fwd*/) && (pair->equalTo(key, posKey))) {
							order = pair->value;
                            //*(ctx->len++) = ctx->lastChar;
                            incctx_appendChar(ctx, ctx->lastChar);
							foundSmaller = TRUE;
						}
						i++;

					}
				}
			}
    }
	// Expansion sequence start...
        if (order >= UCOL_EXPANDCHARINDEX) {
            VectorOfInt *v = ((RuleBasedCollator *)coll)->data->expandTable->at(order-UCOL_EXPANDCHARINDEX);
            if(v != NULL) {
                int32_t expandindex=0;
                int32_t vSize = v->size();
                order = v->at(expandindex++); // first character....
                while(expandindex < vSize) {
                    *(ctx->CEpos++) = v->at(expandindex++);
                }
            }
        }

     // Thai/Lao reordering
        // This is gonna be way too goofy - so we're gonna bail out and let others do the work...
        if (UCOL_ISTHAIPREVOWEL(ctx->currentChar)) {
                ctx->panic = TRUE;
                return UCOL_NULLORDER;
        }
    }
    return order;
}

void incctx_cleanUpContext(incrementalContext *ctx) {
    if(ctx->stringP != ctx->stackString) {
        uprv_free(ctx->stringP);
    }
}

UChar incctx_appendChar(incrementalContext *ctx, UChar c) {
    if(ctx->len == ctx->capacity) { /* bother, said Pooh, we need to reallocate */
        UChar *newStuff;
        if(ctx->stringP == ctx->stackString) { /* we haven't allocated before, need to allocate */
            newStuff = (UChar *)uprv_malloc(2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
            uprv_memcpy(newStuff, ctx->stringP, (ctx->capacity - ctx->stringP)*sizeof(UChar));
        } else { /* we have already allocated, need to reallocate */
            newStuff = (UChar *)uprv_realloc(ctx->stringP, 2*(ctx->capacity - ctx->stringP)*sizeof(UChar));
            if(newStuff == NULL) {
                /*freak out*/
            }
        }
        ctx->len=newStuff+(ctx->len - ctx->stringP);
        ctx->capacity = newStuff+2*(ctx->capacity - ctx->stringP);
        ctx->stringP = newStuff;
    }
    *(ctx->len++) = c;
    return c;
}



UCollationResult alternateIncrementalProcessing(const UCollator *coll, incrementalContext *srcCtx, incrementalContext *trgCtx) {
    if(srcCtx->stringP == srcCtx->len || *(srcCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(srcCtx, srcCtx->source(srcCtx->sourceContext)) != 0xFFFF);
    }
    if(trgCtx->stringP == trgCtx->len || *(trgCtx->len-1) != 0xFFFF) {
        while(incctx_appendChar(trgCtx, trgCtx->source(trgCtx->sourceContext)) != 0xFFFF);
    }
    UCollationResult result = ucol_strcoll(coll, srcCtx->stringP, srcCtx->len-srcCtx->stringP-1, trgCtx->stringP, trgCtx->len-trgCtx->stringP-1);
    incctx_cleanUpContext(srcCtx);
    incctx_cleanUpContext(trgCtx);
    return result;
}

/* This is the incremental function */
U_CAPI UCollationResult ucol_strcollinc(const UCollator *coll, 
								 UCharForwardIterator *source, void *sourceContext,
								 UCharForwardIterator *target, void *targetContext)
{
	Collator *cppColl = (Collator*)coll;

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    incrementalContext sColl, tColl;

    init_incrementalContext(source, sourceContext, &sColl);
    init_incrementalContext(target, targetContext, &tColl);

    if(cppColl->getDecomposition() != Normalizer::NO_OP) { // run away screaming!!!!
        return alternateIncrementalProcessing(coll, &sColl, &tColl);
    }

    if (U_FAILURE(status))
    {
        return UCOL_EQUAL;
    }

    UColAttributeValue strength = ucol_getAttribute(coll, UCOL_STRENGTH, &status);
    uint32_t sOrder=UCOL_NULLORDER, tOrder=UCOL_NULLORDER;
    uint32_t pSOrder, pTOrder;
    UBool gets = TRUE, gett = TRUE;
    UBool initialCheckSecTer = strength  >= UCOL_SECONDARY;
    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = strength  >= UCOL_TERTIARY;
    UBool checkQuad = strength  >= UCOL_QUATERNARY;
    UBool isFrenchSec = (cppColl->getAttribute(UCOL_FRENCH_COLLATION, status) == UCOL_ON) && checkSecTer;

    if(!isFrenchSec) {
        for(;;)
        {
            // Get the next collation element in each of the strings, unless
            // we've been requested to skip it.
            if (gets)
            {
                sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
            }
            gets = TRUE;

            if (gett)
            {
                tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
            }       
            gett = TRUE;

            // If we've hit the end of one of the strings, jump out of the loop
            if ((sOrder == UCOL_NULLORDER)||
                (tOrder == UCOL_NULLORDER)) {
                if(sColl.panic == TRUE || tColl.panic == TRUE) {
                    return alternateIncrementalProcessing(coll, &sColl, &tColl);
                }
                break;
            }

            // If there's no difference at this position, we can skip to the
            // next one.
            if (sOrder == tOrder)
            {
                continue;
            }

            // Compare primary differences first.
            pSOrder = UCOL_PRIMARYORDER(sOrder);
            pTOrder = UCOL_PRIMARYORDER(tOrder);
            if (pSOrder != pTOrder)
            {
                if (sOrder == UCOL_IGNORABLE)
                {
                    // The entire source element is ignorable.
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                    continue;
                }

                if (tOrder == UCOL_IGNORABLE)
                {
                    gets = FALSE;
                    continue;
                }

                // The source and target elements aren't ignorable, but it's still possible
                // for the primary component of one of the elements to be ignorable....
                if (pSOrder == UCOL_PRIMIGNORABLE)  // primary order in source is ignorable
                {
                    // The source's primary is ignorable, but the target's isn't.  We treat ignorables
                    // as a secondary difference, so remember that we found one.
                    if (checkSecTer)
                    {
                        result = UCOL_GREATER;  // (strength is SECONDARY) - still need to check for tertiary or quad
                        checkSecTer = FALSE;
                    }
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                }
                else if (pTOrder == UCOL_PRIMIGNORABLE)
                {
                    // record differences - see the comment above.
                    if (checkSecTer)
                    {
                        result = UCOL_LESS;  // (strength is SECONDARY) - still need to check for tertiary or quad
                        checkSecTer = FALSE;
                    }
                    // Skip to the next target element, but don't fetch another source element.
                    gets = FALSE;
                }
                else
                {
                    // Neither of the orders is ignorable, and we already know that the primary
                    // orders are different because of the (pSOrder != pTOrder) test above.
                    // Record the difference and stop the comparison.
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    if (pSOrder < pTOrder)
                    {
                        return UCOL_LESS;  // (strength is PRIMARY)
                    }

                    return UCOL_GREATER;  // (strength is PRIMARY)
                }
            }
            else
            { // else of if ( pSOrder != pTOrder )
                // primary order is the same, but complete order is different. So there
                // are no base elements at this point, only ignorables (Since the strings are
                // normalized)

                if (checkSecTer)
                {
                    // a secondary or tertiary difference may still matter
                    uint32_t secSOrder = UCOL_SECONDARYORDER(sOrder);
                    uint32_t secTOrder = UCOL_SECONDARYORDER(tOrder);

                    if (secSOrder != secTOrder)
                    {
                        // there is a secondary difference
                        result = (secSOrder < secTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                    else
                    {
                        if (checkTertiary)
                        {
                            // a tertiary difference may still matter
                            uint32_t terSOrder = UCOL_TERTIARYORDER(sOrder);
                            uint32_t terTOrder = UCOL_TERTIARYORDER(tOrder);

                            if (terSOrder != terTOrder)
                            {
                                // there is a tertiary difference
                                result = (terSOrder < terTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is TERTIARY)
                                checkTertiary = FALSE;
                            }
                        }
                    }
                } // if (checkSecTer)

            }  // if ( pSOrder != pTOrder )
        } // while()

        if (sOrder != UCOL_NULLORDER)
        {
            // (tOrder must be CollationElementIterator::NULLORDER,
            //  since this point is only reached when sOrder or tOrder is NULLORDER.)
            // The source string has more elements, but the target string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(sOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the source string.
                    // This is a primary difference, so the source is greater
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    return UCOL_GREATER; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(sOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements mean the source string is greater
                    if (checkSecTer)
                    {
                        result = UCOL_GREATER;  // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                } 
             sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
            }
            //while ((sOrder = ucol_getIncrementalCE(coll, &sColl, &status)) != CollationElementIterator::NULLORDER);
            while (sOrder != UCOL_NULLORDER);
        }
        else if (tOrder != UCOL_NULLORDER)
        {
            // The target string has more elements, but the source string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(tOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the target string.
                    // This is a primary difference, so the source is less
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    return UCOL_LESS; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(tOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements in the target mean the source string is less
                    if (checkSecTer)
                    {
                        result = UCOL_LESS;  // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                } 
                tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
            }
            while ( tOrder != UCOL_NULLORDER);
            //while ((tOrder = ucol_getIncrementalCE(coll, &tColl, &status)) != CollationElementIterator::NULLORDER);
        }
    } else { //French

        // there is a bad situation with French when there is a different number of secondaries... 
        // If that situation arises (when one primary is ignorable with nonignorable secondary and the other primary is not
        // ignorable
        // TODO: if the buffer is not big enough, we should use sortkeys
        UBool bufferFrenchSec = FALSE;
        uint32_t sourceFrenchSec[UCOL_MAX_BUFFER], targetFrenchSec[UCOL_MAX_BUFFER];
        uint32_t *sFSBEnd = sourceFrenchSec+UCOL_MAX_BUFFER;
        uint32_t *tFSBEnd = targetFrenchSec+UCOL_MAX_BUFFER;

        for(;;)
        {
            // Get the next collation element in each of the strings, unless
            // we've been requested to skip it.
            if (gets)
            {
                sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
                *(--sFSBEnd) = UCOL_SECONDARYORDER(sOrder);
                if(sFSBEnd == sourceFrenchSec) { /* overflowing the buffer, bail out */
                    return alternateIncrementalProcessing(coll, &sColl, &tColl);
                }
            }
 
            gets = TRUE;

            if (gett)
            {
                tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
                *(--tFSBEnd) = UCOL_SECONDARYORDER(tOrder);
                if(tFSBEnd == targetFrenchSec) { /* overflowing the buffer, bail out */
                    return alternateIncrementalProcessing(coll, &sColl, &tColl);
                }
            }
        
            gett = TRUE;

            // If we've hit the end of one of the strings, jump out of the loop
            if ((sOrder == UCOL_NULLORDER)||
                (tOrder == UCOL_NULLORDER)) {
                break;
            }

            // If there's no difference at this position, we can skip to the
            // next one.
            if (sOrder == tOrder)
            {
                continue;
            }

            // Compare primary differences first.
            pSOrder = UCOL_PRIMARYORDER(sOrder);
            pTOrder = UCOL_PRIMARYORDER(tOrder);
            if (pSOrder != pTOrder)
            {
                if (sOrder == UCOL_IGNORABLE)
                {
                    // The entire source element is ignorable.
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                    continue;
                }

                if (tOrder == UCOL_IGNORABLE)
                {
                    gets = FALSE;
                    continue;
                }

                // The source and target elements aren't ignorable, but it's still possible
                // for the primary component of one of the elements to be ignorable....
                if (pSOrder == UCOL_PRIMIGNORABLE)  // primary order in source is ignorable
                {
                    // The source's primary is ignorable, but the target's isn't.  We treat ignorables
                    // as a secondary difference, so remember that we found one.
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                }
                else if (pTOrder == UCOL_PRIMIGNORABLE)
                {
                    // record differences - see the comment above.
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                    // Skip to the next target element, but don't fetch another source element.
                    gets = FALSE;
                }
                else
                {
                    // Neither of the orders is ignorable, and we already know that the primary
                    // orders are different because of the (pSOrder != pTOrder) test above.
                    // Record the difference and stop the comparison.
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    if (pSOrder < pTOrder)
                    {
                        return UCOL_LESS;  // (strength is PRIMARY)
                    }

                    return UCOL_GREATER;  // (strength is PRIMARY)
                }
            }
            else
            { // else of if ( pSOrder != pTOrder )
                // primary order is the same, but complete order is different. So there
                // are no base elements at this point, only ignorables (Since the strings are
                // normalized)

                if (checkSecTer)
                {
                    // a secondary or tertiary difference may still matter
                    uint32_t secSOrder = UCOL_SECONDARYORDER(sOrder);
                    uint32_t secTOrder = UCOL_SECONDARYORDER(tOrder);

                    if (secSOrder != secTOrder)
                    {
                        // there is a secondary difference
                        result = (secSOrder < secTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is SECONDARY)
                        checkSecTer = isFrenchSec; // We still want to track the French secondaries
                        //checkSecTer = FALSE;
                    }
                    else
                    {
                        if (checkTertiary)
                        {
                            // a tertiary difference may still matter
                            uint32_t terSOrder = UCOL_TERTIARYORDER(sOrder);
                            uint32_t terTOrder = UCOL_TERTIARYORDER(tOrder);

                            if (terSOrder != terTOrder)
                            {
                                // there is a tertiary difference
                                result = (terSOrder < terTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is TERTIARY)
                                checkTertiary = FALSE;
                            }
                        }
                    }
                } // if (checkSecTer)

            }  // if ( pSOrder != pTOrder )
        } // while()

        if (sOrder != UCOL_NULLORDER)
        {
            // (tOrder must be CollationElementIterator::NULLORDER,
            //  since this point is only reached when sOrder or tOrder is NULLORDER.)
            // The source string has more elements, but the target string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(sOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the source string.
                    // This is a primary difference, so the source is greater
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    return UCOL_GREATER; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(sOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements mean the source string is greater
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                } 
                sOrder = ucol_getIncrementalCE(coll, &sColl, &status);
                *(--sFSBEnd) = UCOL_SECONDARYORDER(sOrder);
                if(sFSBEnd == sourceFrenchSec) { /* overflowing the buffer, bail out */
                    return alternateIncrementalProcessing(coll, &sColl, &tColl);
                }
            }
            //while ((sOrder = ucol_getIncrementalCE(coll, &sColl, &status)) != CollationElementIterator::NULLORDER);
            while (sOrder != UCOL_NULLORDER);
        }
        else if (tOrder != UCOL_NULLORDER)
        {
            // The target string has more elements, but the source string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(tOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the target string.
                    // This is a primary difference, so the source is less
                    incctx_cleanUpContext(&sColl);
                    incctx_cleanUpContext(&tColl);
                    return UCOL_LESS; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(tOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements in the target mean the source string is less
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                } 
                tOrder = ucol_getIncrementalCE(coll, &tColl, &status);
                *(--tFSBEnd) = UCOL_SECONDARYORDER(tOrder);
                if(tFSBEnd == targetFrenchSec) { /* overflowing the buffer, bail out */
                    return alternateIncrementalProcessing(coll, &sColl, &tColl);
                }
            }
            while ( tOrder != UCOL_NULLORDER);
        }

        if(bufferFrenchSec) {
            while(sFSBEnd < sourceFrenchSec+UCOL_MAX_BUFFER && tFSBEnd < targetFrenchSec+UCOL_MAX_BUFFER) {
                if(*sFSBEnd == *tFSBEnd) {
                    sFSBEnd++;
                    tFSBEnd++;
                } else if(*sFSBEnd < *tFSBEnd) {
                    result = UCOL_LESS;
                    break;
                } else {
                    result = UCOL_GREATER;
                    break;
                }
            }
        }
    }

 
    // For IDENTICAL comparisons, we use a bitwise character comparison
    // as a tiebreaker if all else is equal
    // NOTE: The java code compares result with 0, and 
    // puts the result of the string comparison directly into result
    if (result == UCOL_EQUAL && strength == UCOL_IDENTICAL)
    {
        UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;
        
        Normalizer::normalize(UnicodeString(sColl.stringP, sColl.len-sColl.stringP-1), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, sourceDecomp,  status);

        Normalizer::normalize(UnicodeString(tColl.stringP, tColl.len-tColl.stringP-1), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, targetDecomp,  status);
        
        comparison = sourceDecomp.compare(targetDecomp);

        if (comparison < 0)
        {
            result = UCOL_LESS;
        }
        else if (comparison == 0)
        {
            result = UCOL_EQUAL;
        }
        else
        {
            result = UCOL_GREATER;
        }
    }

    incctx_cleanUpContext(&sColl);
    incctx_cleanUpContext(&tColl);
    return result;
}

/* This is the new function */
U_CAPI UCollationResult
ucol_strcoll(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength)
{
	Collator *cppColl = (Collator*)coll;

    // check if source and target are valid strings
    if (((source == 0) && (target == 0)) ||
        ((sourceLength == 0) && (targetLength == 0)))
    {
        return UCOL_EQUAL;
    }

    UCollationResult result = UCOL_EQUAL;
    UErrorCode status = U_ZERO_ERROR;

    UChar normSource[UCOL_MAX_BUFFER], normTarget[UCOL_MAX_BUFFER];
    UChar *normSourceP = normSource;
    UChar *normTargetP = normTarget;
    uint32_t normSourceLength = UCOL_MAX_BUFFER, normTargetLength = UCOL_MAX_BUFFER;

    collIterate sColl, tColl;

    if(cppColl->getDecomposition() == Normalizer::NO_OP) {
        init_collIterate(source, sourceLength == -1 ? u_strlen(source) : sourceLength, &sColl, FALSE);
        init_collIterate(target, targetLength == -1 ? u_strlen(target) : targetLength, &tColl, FALSE);
    } else { /* TODO: This is bad behaved if we're working with small buffers */
             /* We really need the normalization quick check here*/
	    UNormalizationMode normMode = ucol_getNormalization(coll);
        normSourceLength = u_normalize(source, sourceLength, normMode, 0, normSource, normSourceLength, &status);
        if(U_FAILURE(status)) { /* This would be buffer overflow */
            normSourceP = (UChar *)uprv_malloc((normSourceLength+1)*sizeof(UChar));
            status = U_ZERO_ERROR;
            normSourceLength = u_normalize(source, sourceLength, normMode, 0, normSourceP, normSourceLength+1, &status);
            normTargetLength = u_normalize(target, targetLength, normMode, 0, normTargetP, normTargetLength, &status);
            if(U_FAILURE(status)) { /* This would be buffer overflow */
                normTargetP = (UChar *)uprv_malloc((normTargetLength+1)*sizeof(UChar));
                status = U_ZERO_ERROR;
                normTargetLength = u_normalize(target, targetLength, normMode, 0, normTargetP, normTargetLength+1, &status);
            }
            Normalizer::EMode mode = cppColl->getDecomposition();
            cppColl->setDecomposition(Normalizer::NO_OP);
            UCollationResult result = ucol_strcoll(coll, normSourceP, normSourceLength, normTargetP, normTargetLength);
            cppColl->setDecomposition(mode);
            uprv_free(normSourceP);
            if(normTargetP != normTarget) {
                uprv_free(normTargetP);
            }
            return result;
        }
        normTargetLength = u_normalize(target, targetLength, normMode, 0, normTarget, normTargetLength, &status);
        if(U_FAILURE(status)) { /* This would be buffer overflow */
            normTargetP = (UChar *)uprv_malloc((normTargetLength+1)*sizeof(UChar));
            status = U_ZERO_ERROR;
            normTargetLength = u_normalize(target, targetLength, normMode, 0, normTargetP, normTargetLength+1, &status);
            Normalizer::EMode mode = cppColl->getDecomposition();
            cppColl->setDecomposition(Normalizer::NO_OP);
            UCollationResult result = ucol_strcoll(coll, normSourceP, normSourceLength, normTargetP, normTargetLength);
            cppColl->setDecomposition(mode);
            uprv_free(normTargetP);
            return result;
        }
        init_collIterate(normSource, normSourceLength, &sColl, TRUE);
        init_collIterate(normTarget, normTargetLength, &tColl, TRUE);
	}

    if (U_FAILURE(status))
    {
        return UCOL_EQUAL;
    }

    UColAttributeValue strength = ucol_getAttribute(coll, UCOL_STRENGTH, &status);
    uint32_t sOrder=UCOL_NULLORDER, tOrder=UCOL_NULLORDER;
    uint32_t pSOrder, pTOrder;
    UBool gets = TRUE, gett = TRUE;
    UBool initialCheckSecTer = strength  >= UCOL_SECONDARY;
    UBool checkSecTer = initialCheckSecTer;
    UBool checkTertiary = strength  >= UCOL_TERTIARY;
    UBool checkQuad = strength  >= UCOL_QUATERNARY;
    UBool isFrenchSec = (cppColl->getAttribute(UCOL_FRENCH_COLLATION, status) == UCOL_ON) && checkSecTer;

    if(!isFrenchSec) {
        for(;;)
        {
            // Get the next collation element in each of the strings, unless
            // we've been requested to skip it.
            if (gets)
            {
                UCOL_GETNEXTCE(sOrder, coll, sColl, status);
            }
            gets = TRUE;

            if (gett)
            {
                UCOL_GETNEXTCE(tOrder, coll, tColl, status);
            }       
            gett = TRUE;

            // If we've hit the end of one of the strings, jump out of the loop
            if ((sOrder == UCOL_NULLORDER)||
                (tOrder == UCOL_NULLORDER)) {
                break;
            }

            // If there's no difference at this position, we can skip to the
            // next one.
            if (sOrder == tOrder)
            {
                continue;
            }

            // Compare primary differences first.
            pSOrder = UCOL_PRIMARYORDER(sOrder);
            pTOrder = UCOL_PRIMARYORDER(tOrder);
            if (pSOrder != pTOrder)
            {
                if (sOrder == UCOL_IGNORABLE)
                {
                    // The entire source element is ignorable.
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                    continue;
                }

                if (tOrder == UCOL_IGNORABLE)
                {
                    gets = FALSE;
                    continue;
                }

                // The source and target elements aren't ignorable, but it's still possible
                // for the primary component of one of the elements to be ignorable....
                if (pSOrder == UCOL_PRIMIGNORABLE)  // primary order in source is ignorable
                {
                    // The source's primary is ignorable, but the target's isn't.  We treat ignorables
                    // as a secondary difference, so remember that we found one.
                    if (checkSecTer)
                    {
                        result = UCOL_GREATER;  // (strength is SECONDARY) - still need to check for tertiary or quad
                        checkSecTer = FALSE;
                    }
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                }
                else if (pTOrder == UCOL_PRIMIGNORABLE)
                {
                    // record differences - see the comment above.
                    if (checkSecTer)
                    {
                        result = UCOL_LESS;  // (strength is SECONDARY) - still need to check for tertiary or quad
                        checkSecTer = FALSE;
                    }
                    // Skip to the next target element, but don't fetch another source element.
                    gets = FALSE;
                }
                else
                {
                    // Neither of the orders is ignorable, and we already know that the primary
                    // orders are different because of the (pSOrder != pTOrder) test above.
                    // Record the difference and stop the comparison.
                    if (pSOrder < pTOrder)
                    {
                        return UCOL_LESS;  // (strength is PRIMARY)
                    }

                    return UCOL_GREATER;  // (strength is PRIMARY)
                }
            }
            else
            { // else of if ( pSOrder != pTOrder )
                // primary order is the same, but complete order is different. So there
                // are no base elements at this point, only ignorables (Since the strings are
                // normalized)

                if (checkSecTer)
                {
                    // a secondary or tertiary difference may still matter
                    uint32_t secSOrder = UCOL_SECONDARYORDER(sOrder);
                    uint32_t secTOrder = UCOL_SECONDARYORDER(tOrder);

                    if (secSOrder != secTOrder)
                    {
                        // there is a secondary difference
                        result = (secSOrder < secTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                    else
                    {
                        if (checkTertiary)
                        {
                            // a tertiary difference may still matter
                            uint32_t terSOrder = UCOL_TERTIARYORDER(sOrder);
                            uint32_t terTOrder = UCOL_TERTIARYORDER(tOrder);

                            if (terSOrder != terTOrder)
                            {
                                // there is a tertiary difference
                                result = (terSOrder < terTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is TERTIARY)
                                checkTertiary = FALSE;
                            }
                        }
                    }
                } // if (checkSecTer)

            }  // if ( pSOrder != pTOrder )
        } // while()

        if (sOrder != UCOL_NULLORDER)
        {
            // (tOrder must be CollationElementIterator::NULLORDER,
            //  since this point is only reached when sOrder or tOrder is NULLORDER.)
            // The source string has more elements, but the target string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(sOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the source string.
                    // This is a primary difference, so the source is greater
                    return UCOL_GREATER; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(sOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements mean the source string is greater
                    if (checkSecTer)
                    {
                        result = UCOL_GREATER;  // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                } 
             UCOL_GETNEXTCE(sOrder, coll, sColl, status);
            }
            //while ((sOrder = ucol_getNextCE(coll, &sColl, &status)) != CollationElementIterator::NULLORDER);
            while (sOrder != UCOL_NULLORDER);
        }
        else if (tOrder != UCOL_NULLORDER)
        {
            // The target string has more elements, but the source string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(tOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the target string.
                    // This is a primary difference, so the source is less
                    return UCOL_LESS; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(tOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements in the target mean the source string is less
                    if (checkSecTer)
                    {
                        result = UCOL_LESS;  // (strength is SECONDARY)
                        checkSecTer = FALSE;
                    }
                } 
                UCOL_GETNEXTCE(tOrder, coll, tColl, status);
            }
            while ( tOrder != UCOL_NULLORDER);
            //while ((tOrder = ucol_getNextCE(coll, &tColl, &status)) != CollationElementIterator::NULLORDER);
        }
    } else { //French

        // there is a bad situation with French when there is a different number of secondaries... 
        // If that situation arises (when one primary is ignorable with nonignorable secondary and the other primary is not
        // ignorable
        // TODO: if the buffer is not big enough, we should use sortkeys
        UBool bufferFrenchSec = FALSE;
        uint32_t sourceFrenchSec[UCOL_MAX_BUFFER], targetFrenchSec[UCOL_MAX_BUFFER];
        uint32_t *sFSBEnd = sourceFrenchSec+UCOL_MAX_BUFFER;
        uint32_t *tFSBEnd = targetFrenchSec+UCOL_MAX_BUFFER;

        for(;;)
        {
            // Get the next collation element in each of the strings, unless
            // we've been requested to skip it.
            if (gets)
            {
                UCOL_GETNEXTCE(sOrder, coll, sColl, status);
                *(--sFSBEnd) = UCOL_SECONDARYORDER(sOrder);
                if(sFSBEnd == sourceFrenchSec) { /* overflowing the buffer, bail out */
                    return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
                }
            }

            gets = TRUE;

            if (gett)
            {
                UCOL_GETNEXTCE(tOrder, coll, tColl, status);
                *(--tFSBEnd) = UCOL_SECONDARYORDER(tOrder);
                if(tFSBEnd == targetFrenchSec) { /* overflowing the buffer, bail out */
                    return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
                }
            }
        
            gett = TRUE;

            // If we've hit the end of one of the strings, jump out of the loop
            if ((sOrder == UCOL_NULLORDER)||
                (tOrder == UCOL_NULLORDER)) {
                break;
            }

            // If there's no difference at this position, we can skip to the
            // next one.
            if (sOrder == tOrder)
            {
                continue;
            }

            // Compare primary differences first.
            pSOrder = UCOL_PRIMARYORDER(sOrder);
            pTOrder = UCOL_PRIMARYORDER(tOrder);
            if (pSOrder != pTOrder)
            {
                if (sOrder == UCOL_IGNORABLE)
                {
                    // The entire source element is ignorable.
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                    continue;
                }

                if (tOrder == UCOL_IGNORABLE)
                {
                    gets = FALSE;
                    continue;
                }

                // The source and target elements aren't ignorable, but it's still possible
                // for the primary component of one of the elements to be ignorable....
                if (pSOrder == UCOL_PRIMIGNORABLE)  // primary order in source is ignorable
                {
                    // The source's primary is ignorable, but the target's isn't.  We treat ignorables
                    // as a secondary difference, so remember that we found one.
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                    // Skip to the next source element, but don't fetch another target element.
                    gett = FALSE;
                }
                else if (pTOrder == UCOL_PRIMIGNORABLE)
                {
                    // record differences - see the comment above.
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                    // Skip to the next target element, but don't fetch another source element.
                    gets = FALSE;
                }
                else
                {
                    // Neither of the orders is ignorable, and we already know that the primary
                    // orders are different because of the (pSOrder != pTOrder) test above.
                    // Record the difference and stop the comparison.
                    if (pSOrder < pTOrder)
                    {
                        return UCOL_LESS;  // (strength is PRIMARY)
                    }

                    return UCOL_GREATER;  // (strength is PRIMARY)
                }
            }
            else
            { // else of if ( pSOrder != pTOrder )
                // primary order is the same, but complete order is different. So there
                // are no base elements at this point, only ignorables (Since the strings are
                // normalized)

                if (checkSecTer)
                {
                    // a secondary or tertiary difference may still matter
                    uint32_t secSOrder = UCOL_SECONDARYORDER(sOrder);
                    uint32_t secTOrder = UCOL_SECONDARYORDER(tOrder);

                    if (secSOrder != secTOrder)
                    {
                        // there is a secondary difference
                        result = (secSOrder < secTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is SECONDARY)
                        checkSecTer = isFrenchSec; // We still want to track the French secondaries
                        //checkSecTer = FALSE;
                    }
                    else
                    {
                        if (checkTertiary)
                        {
                            // a tertiary difference may still matter
                            uint32_t terSOrder = UCOL_TERTIARYORDER(sOrder);
                            uint32_t terTOrder = UCOL_TERTIARYORDER(tOrder);

                            if (terSOrder != terTOrder)
                            {
                                // there is a tertiary difference
                                result = (terSOrder < terTOrder) ? UCOL_LESS : UCOL_GREATER;
                                                // (strength is TERTIARY)
                                checkTertiary = FALSE;
                            }
                        }
                    }
                } // if (checkSecTer)

            }  // if ( pSOrder != pTOrder )
        } // while()

        if (sOrder != UCOL_NULLORDER)
        {
            // (tOrder must be CollationElementIterator::NULLORDER,
            //  since this point is only reached when sOrder or tOrder is NULLORDER.)
            // The source string has more elements, but the target string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(sOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the source string.
                    // This is a primary difference, so the source is greater
                    return UCOL_GREATER; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(sOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements mean the source string is greater
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                } 
                UCOL_GETNEXTCE(sOrder, coll, sColl, status);
                *(--sFSBEnd) = UCOL_SECONDARYORDER(sOrder);
                if(sFSBEnd == sourceFrenchSec) { /* overflowing the buffer, bail out */
                    return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
                }
            }
            //while ((sOrder = ucol_getNextCE(coll, &sColl, &status)) != CollationElementIterator::NULLORDER);
            while (sOrder != UCOL_NULLORDER);
        }
        else if (tOrder != UCOL_NULLORDER)
        {
            // The target string has more elements, but the source string hasn't.
            do
            {
                if (UCOL_PRIMARYORDER(tOrder) != UCOL_PRIMIGNORABLE)
                {
                    // We found an additional non-ignorable base character in the target string.
                    // This is a primary difference, so the source is less
                    return UCOL_LESS; // (strength is PRIMARY)
                }

                if (UCOL_SECONDARYORDER(tOrder) != UCOL_SECIGNORABLE)
                {
                    // Additional secondary elements in the target mean the source string is less
                    if (checkSecTer)
                    {
                            bufferFrenchSec = TRUE;
                    }
                } 
                UCOL_GETNEXTCE(tOrder, coll, tColl, status);
                *(--tFSBEnd) = UCOL_SECONDARYORDER(tOrder);
                if(tFSBEnd == targetFrenchSec) { /* overflowing the buffer, bail out */
                    return ucol_compareUsingSortKeys(coll, source, sourceLength, target, targetLength);
                }
            }
            while ( tOrder != UCOL_NULLORDER);
        }

        if(bufferFrenchSec) {
            while(sFSBEnd < sourceFrenchSec+UCOL_MAX_BUFFER && tFSBEnd < targetFrenchSec+UCOL_MAX_BUFFER) {
                if(*sFSBEnd == *tFSBEnd) {
                    sFSBEnd++;
                    tFSBEnd++;
                } else if(*sFSBEnd < *tFSBEnd) {
                    result = UCOL_LESS;
                    break;
                } else {
                    result = UCOL_GREATER;
                    break;
                }
            }
        }
    }


    // For IDENTICAL comparisons, we use a bitwise character comparison
    // as a tiebreaker if all else is equal
    // NOTE: The java code compares result with 0, and 
    // puts the result of the string comparison directly into result
    if (result == UCOL_EQUAL && strength == UCOL_IDENTICAL)
    {
        UnicodeString sourceDecomp, targetDecomp;

        int8_t comparison;
        
        Normalizer::normalize(UnicodeString(source, sourceLength), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, sourceDecomp,  status);

        Normalizer::normalize(UnicodeString(target, targetLength), ((RuleBasedCollator *)coll)->getDecomposition(), 
                      0, targetDecomp,  status);
        
        comparison = sourceDecomp.compare(targetDecomp);

        if (comparison < 0)
        {
            result = UCOL_LESS;
        }
        else if (comparison == 0)
        {
            result = UCOL_EQUAL;
        }
        else
        {
            result = UCOL_GREATER;
        }
    }

    return result;
}


/* This is the original sort key function */
U_CAPI int32_t
ucol_getSortKeyEx(const    UCollator    *coll,
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

  ((RuleBasedCollator*)coll)->getCollationKeyEx(source, len, key, status);
  if(U_FAILURE(status)) 
    return 0;

  bytes = key.getByteArray(count);
  
  copyLen = uprv_min(count, resultLength);
  uprv_arrayCopy((const int8_t*)bytes, (int8_t*)result, copyLen);

  return count;
}

int32_t ucol_getSortKeySize(const UCollator *coll, collIterate *s, int32_t currentSize, UColAttributeValue strength, int32_t len) {
    UErrorCode status = U_ZERO_ERROR;
    UBool  compareSec   = (strength >= UCOL_SECONDARY);
    UBool  compareTer   = (strength >= UCOL_TERTIARY);
    UBool  compareQuad  = (strength >= UCOL_QUATERNARY);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);
    int32_t order = UCOL_NULLORDER;
    uint16_t primary = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;
    

    for(;;) {
        UCOL_GETNEXTCE(order, coll, *s, status);
        if(order == UCOL_NULLORDER) {
            break;
        }

        primary = ((order & UCOL_PRIMARYORDERMASK)>> UCOL_PRIMARYORDERSHIFT);
        secondary = ((order & UCOL_SECONDARYORDERMASK)>> UCOL_SECONDARYORDERSHIFT);
        tertiary = (order & UCOL_TERTIARYORDERMASK);

        if(primary != UCOL_PRIMIGNORABLE) {
            currentSize += 2;
            if(compareSec) {
                currentSize++;
            }
            if(compareTer) {
                currentSize++;
            }
        } else if(secondary != 0) {
            if(compareSec) {
                currentSize++;
            }
            if(compareTer) {
                currentSize++;
            }
        } else if(tertiary != 0) {
            if(compareTer) {
                currentSize++;
            }
        }
    }

    if(compareIdent) {
        currentSize += len*sizeof(UChar);
        UChar *ident = s->string;
        while(ident<s->len) {
            if((*(ident) >> 8) + utf16fixup[*(ident) >> 11]<0x02) {

                currentSize++;
            }
            if((*(ident) & 0xFF)<0x02) {
                currentSize++;
            }
        }

    }

    return currentSize;
    
}
    

int32_t
ucol_calcSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        **result,
        int32_t        resultLength,
        UBool allocatePrimary)
{

    uint32_t i = 0; // general purpose counter

	UErrorCode status = U_ZERO_ERROR;

    uint8_t second[UCOL_MAX_BUFFER], tert[UCOL_MAX_BUFFER];

    uint8_t *primaries = *result, *secondaries = second, *tertiaries = tert;

    if(primaries == NULL && allocatePrimary == TRUE) {
        primaries = *result = (uint8_t *)uprv_malloc(2*UCOL_MAX_BUFFER);
        resultLength = 2*UCOL_MAX_BUFFER;
    }

    int32_t primSize = resultLength, secSize = UCOL_MAX_BUFFER, terSize = UCOL_MAX_BUFFER;

    int32_t sortKeySize = 1; // it is always \0 terminated

    UChar normBuffer[UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER];
    UChar *normSource = normBuffer;
    int32_t normSourceLen = UCOL_NORMALIZATION_GROWTH*UCOL_MAX_BUFFER;

	int32_t len = (sourceLength == -1 ? u_strlen(source) : sourceLength);

    UColAttributeValue strength = ucol_getAttribute(coll, UCOL_STRENGTH, &status);

    UBool  compareSec   = (strength >= UCOL_SECONDARY);
    UBool  compareTer   = (strength >= UCOL_TERTIARY);
    UBool  compareQuad  = (strength >= UCOL_QUATERNARY);
    UBool  compareIdent = (strength == UCOL_IDENTICAL);

    sortKeySize += ((compareSec?1:0) + (compareTer?1:0) + (compareQuad?1:0) + (compareIdent?1:0));

    collIterate s;
    init_collIterate((UChar *)source, len, &s, FALSE);

    // If we need to normalize, we'll do it all at once at the beggining!
    UNormalizationMode normMode = ucol_getNormalization(coll);
    if(normMode != UNORM_NONE) {
        normSourceLen = u_normalize(source, sourceLength, normMode, 0, normSource, normSourceLen, &status);
        if(U_FAILURE(status)) {
            status=U_ZERO_ERROR;
            normSource = (UChar *) uprv_malloc((normSourceLen+1)*sizeof(UChar));
            normSourceLen = u_normalize(source, sourceLength, normMode, 0, normSource, (normSourceLen+1), &status);
        }
    	normSource[normSourceLen] = 0;
		s.string = normSource;
        s.pos = normSource;
		s.len = normSource+normSourceLen;
	}

    len = s.len-s.pos;

    if(resultLength == 0) {
        return ucol_getSortKeySize(coll, &s, sortKeySize, strength, len);
    }

    int32_t minBufferSize = uprv_min(secSize, terSize);

    uint8_t *primStart = primaries;
    uint8_t *secStart = secondaries;
    uint8_t *terStart = tertiaries;

    uint32_t order = 0;

    uint16_t primary = 0;
    uint8_t secondary = 0;
    uint8_t tertiary = 0;

    UBool finished = FALSE;
    UBool resultOverflow = FALSE;

    int32_t prevBuffSize = 0;

    for(;;) {
        for(i=prevBuffSize; i<minBufferSize; ++i) {

            UCOL_GETNEXTCE(order, coll, s, status);

            if(order == UCOL_NULLORDER) {
                finished = TRUE;
                break;
            }

            primary = ((order & UCOL_PRIMARYORDERMASK)>> UCOL_PRIMARYORDERSHIFT);
            secondary = ((order & UCOL_SECONDARYORDERMASK)>> UCOL_SECONDARYORDERSHIFT);
            tertiary = (order & UCOL_TERTIARYORDERMASK);

            if(primary != UCOL_PRIMIGNORABLE) {
                *(primaries++) = (primary>>8);
                *(primaries++) = (primary&0xFF);
                sortKeySize += 2;
                if(compareSec) {
                    *(secondaries++) = secondary;
                    sortKeySize++;
                }
                if(compareTer) {
                    *(tertiaries++) = tertiary;
                    sortKeySize++;
                }
            } else if(secondary != UCOL_SECIGNORABLE) {
                if(compareSec) {
                    *(secondaries++) = secondary;
                    sortKeySize++;
                }
                if(compareTer) {
                    *(tertiaries++) = tertiary;
                    sortKeySize++;
                }
            } else if(tertiary != UCOL_TERIGNORABLE) {
                if(compareTer) {
                    *(tertiaries++) = tertiary;
                    sortKeySize++;
                }
            }
            if(sortKeySize>resultLength) {
                if(allocatePrimary == FALSE) {
                    resultOverflow = TRUE;
                    sortKeySize = ucol_getSortKeySize(coll, &s, sortKeySize, strength, len);
                    goto cleanup;
                } else {
                    uint8_t *newStart;
                    newStart = (uint8_t *)uprv_realloc(primStart, 2*sortKeySize);
                    if(primStart == NULL) {
                        /*freak out*/
                    }
                    primaries=newStart+(primaries-primStart);
                    resultLength = 2*sortKeySize;
                    primStart = *result = newStart;
                }
            }
        }
        if(finished) {
            break;
        } else {
            prevBuffSize = minBufferSize;
            uint8_t *newStart;

            if(secStart==second) {
                newStart=(uint8_t*)uprv_malloc(2*secSize);
                if(newStart==NULL) {
                    /*freak out;*/
                }
                uprv_memcpy(newStart, secStart, secondaries-secStart);
            } else {
                newStart=(uint8_t*)uprv_realloc(secStart, 2*secSize);
                if(newStart==NULL) {
                    /*freak out;*/
                }
            }
            secondaries=newStart+(secondaries-secStart);
            secStart = newStart;
            secSize*=2;

            if(terStart==tert) {
                newStart=(uint8_t*)uprv_malloc(2*terSize);
                if(newStart==NULL) {
                    /*freak out;*/
                }
                uprv_memcpy(newStart, terStart, tertiaries-terStart);
            } else {
                newStart=(uint8_t*)uprv_realloc(terStart, 2*terSize);
                if(newStart==NULL) {
                    /*freak out;*/
                }
            }
            tertiaries=newStart+(tertiaries-terStart);
            terStart = newStart;
            terSize*=2;

            minBufferSize = uprv_min(secSize, terSize);
        }
    }

    if(compareSec) {
      *(primaries++) = UCOL_LEVELTERMINATOR;
      uint32_t secsize = secondaries-secStart;
      if(ucol_getAttribute(coll, UCOL_FRENCH_COLLATION, &status) == UCOL_ON) { // do the reverse copy
          for(i = 0; i<secsize; i++) {
              *(primaries++) = *(secondaries-i-1);
          }
        } else { 
            uprv_memcpy(primaries, secStart, secsize); 
            primaries += secsize;
        }

    }

    if(compareTer) {
      *(primaries++) = UCOL_LEVELTERMINATOR;
      uint32_t tersize = tertiaries - terStart;
      uprv_memcpy(primaries, terStart, tersize);
      primaries += tersize;
    }

    if(compareQuad) {
        *(primaries++) = UCOL_LEVELTERMINATOR;
    }

    if(compareIdent) {
		UChar *ident = s.string;
        uint8_t idByte = 0;
        sortKeySize += len * sizeof(UChar);
        *(primaries++) = UCOL_LEVELTERMINATOR;
        if(sortKeySize <= resultLength) {
		    while(ident < s.len) {
                idByte = (*(ident) >> 8) + utf16fixup[*(ident) >> 11];
                if(idByte < 0x02) {
                    if(sortKeySize < resultLength) {
                        *(primaries++) = 0x01;
                        sortKeySize++;
                        *(primaries++) = idByte + 1;
                    }
                } else {
                    *(primaries++) = idByte;
                }
                idByte = (*(ident) & 0xFF);
                if(idByte < 0x02) {
                    if(sortKeySize < resultLength) {
                        *(primaries++) = 0x01;
                        sortKeySize++;
                        *(primaries++) = idByte + 1;
                    }
                } else {
                    *(primaries++) = idByte;
                }

		      ident++;
          }
        } else {
		    while(ident < s.len) {
                idByte = (*(ident) >> 8) + utf16fixup[*(ident) >> 11];
                if(idByte < 0x02) {
                    sortKeySize++;
                }
                idByte = (*(ident) & 0xFF);
                if(idByte < 0x02) {
                    sortKeySize++;
                }
		      ident++;
            }
        }

    }

    *(primaries++) = '\0';

cleanup:
    if(terStart != tert) {
        uprv_free(terStart);
    }
    if(secStart != second) {
        uprv_free(secStart);
    }
    if(normSource != normBuffer) {
        uprv_free(normSource);
    }

    return sortKeySize;
}

U_CFUNC uint8_t *ucol_getSortKeyWithAllocation(const UCollator *coll, 
        const    UChar        *source,
        int32_t            sourceLength,
        int32_t *resultLen) {
    uint8_t *result = NULL;
    *resultLen = ucol_calcSortKey(coll, source, sourceLength, &result, 0, TRUE);
    return result;
}

U_CAPI int32_t
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength)
{
    return ucol_calcSortKey(coll, source, sourceLength, &result, resultLength, FALSE);
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

U_CAPI void ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status) {
	((RuleBasedCollator *)coll)->setAttribute(attr, value, *status);
}

U_CAPI UColAttributeValue ucol_getAttribute(const UCollator *coll, UColAttribute attr, UErrorCode *status) {
	return (((RuleBasedCollator *)coll)->getAttribute(attr, *status));
}

U_CAPI UCollator *ucol_safeClone(const UCollator *coll, void *stackBuffer, uint32_t bufferSize, UErrorCode *status) {
	return (UCollator *)(((RuleBasedCollator *)coll)->safeClone());
}

U_CAPI int32_t ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen) {
	return 0;
}
