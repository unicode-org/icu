#include "unicode/ustring.h"
 
#include "cmemory.h"
#include "ucoltok.h"
#include "uhash.h"
#include "ucmp32.h"

static UHashtable *uchars2tokens;
static UColTokListHeader ListList[256];
static uint32_t listPosition = 0;

static const UChar *rulesToParse = 0;

/* will use a small structure, tokHash */

int32_t
uhash_hashTokens(const void *k) {
  int32_t hash = 0;
  if (k != NULL) {
      const UColToken *key = (const UColToken *)k;
      int32_t len = (key->source & 0xFF000000)>>24;
      int32_t inc = ((len - 32) / 32) + 1;

      const UChar *p = (key->source & 0x00FFFFFF) + rulesToParse;
      const UChar *limit = p + len;    

      while (p<limit) {
          hash = (hash * 37) + *p;
          p += inc;
      }

      if((len = ((key->expansion & 0xFF000000)>>24)) != 0) {
        p = (key->expansion & 0x00FFFFFF) + rulesToParse;
        limit = p + len;    
        while (p<limit) {
            hash = (hash * 37) + *p;
            p += inc;
        }
      }
  }
  return hash;
}

UBool uhash_compareTokens(const void *key1, const void *key2) {
    const UColToken *p1 = (const UColToken*) key1;
    const UColToken *p2 = (const UColToken*) key2;
    const UChar *s1 = (p1->source & 0x00FFFFFF) + rulesToParse;
    const UChar *s2 = (p2->source & 0x00FFFFFF) + rulesToParse;
    uint32_t s1L = ((p1->source & 0xFF000000) >> 24);
    uint32_t s2L = ((p2->source & 0xFF000000) >> 24);

    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == NULL || p2 == NULL) {
        return FALSE;
    }
    if(p1->source == p2->source && p1->expansion == p2->expansion) {
      return TRUE;
    }
    if(s1L != s2L) {
      return FALSE;
    }
    while(s1 < s1+s1L-1 && *s1 == *s2) {
      ++s1;
      ++s2;
    }
    if(*s1 == *s2) {
      s1 = (p1->expansion & 0x00FFFFFF) + rulesToParse;
      s2 = (p2->expansion & 0x00FFFFFF) + rulesToParse;
      s1L = ((p1->expansion & 0xFF000000) >> 24);
      s2L = ((p2->expansion & 0xFF000000) >> 24);
      if(s1L != s2L) {
        return FALSE;
      }
      if(s1L != 0) {
        while(s1 < s1+s1L-1 && *s1 == *s2) {
          ++s1;
          ++s2;
        }
        return (UBool)(*s1 == *s2);
      } else {
        return TRUE;
      }
    } else {
      return FALSE;
    }
}

/*
void deleteElement(void *element) {
    UCAElements *el = (UCAElements *)element;

    int32_t i = 0;
    for(i = 0; i < el->noOfCEs; i++) {
        free(el->primary[i]);
        free(el->secondary[i]);
        free(el->tertiary[i]);
    }
    free(el);
}
*/

void ucol_tok_initTokenList(UColTokenParser *src, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
  rulesToParse = src->source;
  uchars2tokens = uhash_open(uhash_hashTokens, uhash_compareTokens, status);
  /*uhash_setValueDeleter(uchars2tokens, deleteElement);*/
}

void ucol_tok_closeTokenList(void) {
    uhash_close(uchars2tokens);
}

#define UCOL_TOK_UNSET 0xFFFFFFFF
#define UCOL_TOK_RESET 0xDEADBEEF

/*
Processing Description
  1 Build a ListList. Each list has a header, which contains two lists (positive 
  and negative), a reset token, a baseCE, nextCE, and previousCE. The lists and 
  reset may be null. 
  2 As you process, you keep a LAST pointer that points to the last token you 
  handled. 
*/

uint32_t ucol_uprv_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status) {
  UColToken *lastToken = NULL;
  uint32_t newCharsLen = 0, newExtensionsLen = 0;
  uint32_t charsOffset = 0, extensionOffset = 0;
  uint32_t expandNext = 0;

  uint32_t newStrength = UCOL_TOK_UNSET; 

  ucol_tok_initTokenList(src, status);

  while(src->current < src->end) {
    { /* parsing part */

      UBool inChars = TRUE;
      UBool inQuote = FALSE;

      newStrength = UCOL_TOK_UNSET; 
      newCharsLen = 0; newExtensionsLen = 0;
      charsOffset = 0; extensionOffset = 0;

      while (src->current < src->end) {
          UChar ch = *(src->current);

        if (inQuote) {
          if (ch == 0x0027/*'\''*/) {
              inQuote = FALSE;
          } else {
            if ((newCharsLen == 0) || inChars) {
              if(newCharsLen == 0) {
                charsOffset = src->current - src->source;
              }
              newCharsLen++;
            } else {
              if(newExtensionsLen == 0) {
                extensionOffset = src->current - src->source;
              }
              newExtensionsLen++;
            }
          }
        } else {
          /* Sets the strength for this entry */
          switch (ch) {
            case 0x003D/*'='*/ : 
              if (newStrength != UCOL_TOK_UNSET) {
                goto EndOfLoop;
              }

              newStrength = UCOL_IDENTICAL;
              break;

            case 0x002C/*','*/:  
              if (newStrength != UCOL_TOK_UNSET) {
                goto EndOfLoop;
              }

              newStrength = UCOL_TERTIARY;
              break;

            case  0x003B/*';'*/:
              if (newStrength != UCOL_TOK_UNSET) {
                goto EndOfLoop;
              }

              newStrength = UCOL_SECONDARY;
              break;

            case 0x003C/*'<'*/:  
              if (newStrength != UCOL_TOK_UNSET) {
                goto EndOfLoop;
              }

              newStrength = UCOL_PRIMARY;
              break;

            case 0x0026/*'&'*/:  
              if (newStrength != UCOL_TOK_UNSET) {
                goto EndOfLoop;
              }

              newStrength = UCOL_TOK_RESET; /* PatternEntry::RESET = 0 */
              break;

            /* Ignore the white spaces */
            case 0x0009/*'\t'*/:
            case 0x000C/*'\f'*/:
            case 0x000D/*'\r'*/:
            case 0x000A/*'\n'*/:
            case 0x0020/*' '*/:  
              break; /* skip whitespace TODO use Unicode */

            case 0x002F/*'/'*/:
                    /* This entry has an extension. */
              inChars = FALSE;
              break;

            case 0x0027/*'\''*/:
              inQuote = TRUE;
              ch = *(++(src->current)); /*pattern[++index]; */

              if (newCharsLen == 0) {
                charsOffset = src->current - src->source;
                newCharsLen++;
              } else if (inChars) {
                if(newCharsLen == 0) {
                  charsOffset = src->current - src->source;
                }
                newCharsLen++;
              } else {
                newExtensionsLen++;
              }

              break;

            /* '@' is french only if the strength is not currently set */
            /* if it is, it's just a regular character in collation rules */
            case 0x0040/*'@'*/:
              if (newStrength == UCOL_TOK_UNSET) {
                src->image->frenchCollation = UCOL_ON;
                break;
              }

            default:
              if (newStrength == UCOL_TOK_UNSET) {
                *status = U_INVALID_FORMAT_ERROR;
                return 0;
              }

              if (ucol_tok_isSpecialChar(ch) && (inQuote == FALSE)) {
                *status = U_INVALID_FORMAT_ERROR;
                return 0;
              }



              if (inChars) {
                if(newCharsLen == 0) {
                  charsOffset = src->current - src->source;
                }
                newCharsLen++;
              } else {
                if(newExtensionsLen == 0) {
                  extensionOffset = src->current - src->source;
                }
                newExtensionsLen++;
              }

              break;
            }
        }

          src->current++;
        }

     EndOfLoop:
      if (newStrength == UCOL_TOK_UNSET) {
        return 0;
      }

      if (newCharsLen == 0) {
        *status = U_INVALID_FORMAT_ERROR;
        return 0;
      }
    }

    {
      UColToken *sourceToken = NULL;
      UColToken key;

      key.source = newCharsLen << 24 | charsOffset;
      key.expansion = newExtensionsLen << 24 | extensionOffset;

      /*  4 Lookup each [source,  expansion] in the CharsToToken map, and find a sourceToken */
      sourceToken = (UColToken *)uhash_get(uchars2tokens, &key);

      if(newStrength != UCOL_TOK_RESET) {
        if(lastToken == NULL) { /* this means that rules haven't started properly */
          *status = U_INVALID_FORMAT_ERROR;
          return 0;
        }
      /*  6 Otherwise (when relation != reset) */
        if(sourceToken == NULL) {
          /* If sourceToken is null, create new one, */
          sourceToken = (UColToken *)uprv_malloc(sizeof(UColToken));
          sourceToken->source = newCharsLen << 24 | charsOffset;
          sourceToken->expansion = newExtensionsLen << 24 | extensionOffset;

          sourceToken->debugSource = *(src->source + charsOffset);
          if(newExtensionsLen > 0) {
            sourceToken->debugExpansion = *(src->source + extensionOffset);
          } else {
            sourceToken->debugExpansion = 0;
          }


          sourceToken->polarity = UCOL_TOK_POLARITY_POSITIVE; /* TODO: this should also handle reverse */
          sourceToken->next = NULL;
          sourceToken->previous = NULL;
          sourceToken->noOfCEs = 0;
          sourceToken->noOfExpCEs = 0;
          uhash_put(uchars2tokens, sourceToken, sourceToken, status);
        } else {
          /* we could have fished out a reset here */
          if(sourceToken->strength != UCOL_TOK_RESET) {
            /* otherwise remove sourceToken from where it was. */
            if(sourceToken->next != NULL) {
              sourceToken->next->previous = sourceToken->previous;
            } else {
              sourceToken->listHeader->last[sourceToken->polarity] = sourceToken->previous;
            }

            if(sourceToken->previous != NULL) {
              sourceToken->previous->next = sourceToken->next;
            } else {
              sourceToken->listHeader->first[sourceToken->polarity] = sourceToken->next;
            }
          }
        }

        sourceToken->strength = newStrength;
        sourceToken->listHeader = lastToken->listHeader;
        /*
        1.	Find the strongest strength in each list, and set strongestP and strongestN 
        accordingly in the headers. 
        */

        if(lastToken->strength == UCOL_TOK_RESET) {
        /* If LAST is a reset 
              insert sourceToken at the head of either the positive list or the negative 
              list, depending on the polarity of relation. 
              set the polarity of sourceToken to be the same as the list you put it in. */
          if(sourceToken->listHeader->first[sourceToken->polarity] == 0) {
            sourceToken->listHeader->first[sourceToken->polarity] = sourceToken;
            sourceToken->listHeader->last[sourceToken->polarity] = sourceToken;
          } else {
            sourceToken->listHeader->first[sourceToken->polarity]->previous = sourceToken;
            sourceToken->next = sourceToken->listHeader->first[sourceToken->polarity];
            sourceToken->listHeader->first[sourceToken->polarity] = sourceToken;
          }

          /*
            If "xy" doesn't occur earlier in the list or in the UCA, convert &xy * c * 
            d * ... into &x * c/y * d * ... 
          */
          if(expandNext != 0 && sourceToken->expansion == 0) {
            sourceToken->expansion = expandNext;
            sourceToken->debugExpansion = *(src->source + (expandNext & 0xFFFFFF));
            expandNext = 0;
          }

        } else {
        /* Otherwise (when LAST is not a reset) 
              if polarity (LAST) == polarity(relation), insert sourceToken after LAST, 
              otherwise insert before. 
              when inserting after or before, search to the next position with the same 
              strength in that direction. (This is called postpone insertion).         */
          if(lastToken->polarity == sourceToken->polarity) {
            while(lastToken->next != NULL && lastToken->next->strength > sourceToken->strength) {
              lastToken = lastToken->next;
            }
            sourceToken->previous = lastToken;
            if(lastToken->next != NULL) {
              lastToken->next->previous = sourceToken;
            } else {
              sourceToken->listHeader->last[sourceToken->polarity] = sourceToken;
            }

            sourceToken->next = lastToken->next;
            lastToken->next = sourceToken;
          } else {
            while(lastToken->previous != NULL && lastToken->previous->strength > sourceToken->strength) {
              lastToken = lastToken->previous;
            }
            sourceToken->next = lastToken;
            if(lastToken->previous != NULL) {
              lastToken->previous->next = sourceToken;
            } else {
              sourceToken->listHeader->first[sourceToken->polarity] = sourceToken;
            }
            sourceToken->previous = lastToken->previous;
            lastToken->previous = sourceToken;
          }
        }
      } else {
        uint32_t CE = UCOL_NOT_FOUND, SecondCE = UCOL_NOT_FOUND;
        collIterate s;

        if(newCharsLen > 1) {
          expandNext = ((newCharsLen-1)<<24) | (charsOffset + 1);
        } else {
          expandNext = 0;
        }

      /*  5 If the relation is a reset: 
          If sourceToken is null 
            Create new list, create new sourceToken, make the baseCE from source, put 
            the sourceToken in ListHeader of the new list */
        if(sourceToken == NULL) {

          /*
              3. The rule for "& abcdefg < xyz" is a bit tricky. What it turns into is:

              a. Find the longest sequence in "abcdefg" that is in UCA *OR* in the
              tailoring so far. Suppose that is "abcd".
              b. Then treat this rule as equivalent to:
              "& abcd < xyz / efg"
          */
          if(newCharsLen > 1) {
            key.source = 0x01000000 | charsOffset;
            sourceToken = (UColToken *)uhash_get(uchars2tokens, &key);
            if(sourceToken != NULL) {
              lastToken = sourceToken;
              continue;
            }
          }
          /* do the reset thing */
          sourceToken = (UColToken *)uprv_malloc(sizeof(UColToken));
          sourceToken->source = newCharsLen << 24 | charsOffset;
          sourceToken->expansion = newExtensionsLen << 24 | extensionOffset;
          
          sourceToken->debugSource = *(src->source + charsOffset);
          sourceToken->debugExpansion = *(src->source + extensionOffset);


          sourceToken->polarity = UCOL_TOK_POLARITY_POSITIVE; /* TODO: this should also handle reverse */
          sourceToken->strength = UCOL_TOK_RESET;
          sourceToken->next = NULL;
          sourceToken->previous = NULL;
          sourceToken->listHeader = &ListList[listPosition];
          /*
            3 Consider each item: relation, source, and expansion: e.g. ...< x / y ... 
              First convert all expansions into normal form. Examples: 
                If "xy" doesn't occur earlier in the list or in the UCA, convert &xy * c * 
                d * ... into &x * c/y * d * ... 
                Note: reset values can never have expansions, although they can cause the 
                very next item to have one. They may be contractions, if they are found 
                earlier in the list. 
          */
          if(newCharsLen > 1) {
            sourceToken->source = 0x01000000 | charsOffset;
          } 

 
          init_collIterate(src->source+charsOffset, 1, &s, FALSE); /* or newCharsLen instead of 1??? */

          CE = ucol_getNextCE(src->UCA, &s, status);
          /*UCOL_GETNEXTCE(CE, src->UCA, s, &status);*/

          SecondCE = ucol_getNextCE(src->UCA, &s, status);
          /*UCOL_GETNEXTCE(SecondCE, src->UCA, s, &status);*/
    
          ListList[listPosition].baseCE = CE;
          if(isContinuation(SecondCE)) {
            ListList[listPosition].baseContCE = SecondCE;
          } else {
            ListList[listPosition].baseContCE = 0;
          }


          ListList[listPosition].first[UCOL_TOK_POLARITY_NEGATIVE] = NULL;
          ListList[listPosition].last[UCOL_TOK_POLARITY_NEGATIVE] = NULL;
          ListList[listPosition].first[UCOL_TOK_POLARITY_POSITIVE] = NULL;
          ListList[listPosition].last[UCOL_TOK_POLARITY_POSITIVE] = NULL;

          ListList[listPosition].reset = sourceToken;

          listPosition++;
          uhash_put(uchars2tokens, sourceToken, sourceToken, status);
        } else { /* reset to something already in rules */
        }
      }
      /*  7 After all this, set LAST to point to sourceToken, and goto step 3. */  
      lastToken = sourceToken;
    }  
  }

  src->lh = ListList;
  src->resultLen = listPosition;

  return listPosition;
}

uint32_t ucol_tok_assembleTokenList(UColTokenParser *src, UErrorCode *status) {
  uint32_t res = ucol_uprv_tok_assembleTokenList(src, status);
  ucol_tok_closeTokenList();
  return res;
}

