/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_tok.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 02/22/2001
*   created by: Vladimir Weinstein
*
* This module reads a tailoring rule string and produces a list of 
* tokens that will be turned into collation elements
* 
*/

#include "unicode/ustring.h"
#include "unicode/uchar.h"
 
#include "cmemory.h"
#include "ucol_tok.h"

const UChar *rulesToParse;

void ucol_tok_initTokenList(UColTokenParser *src, const UChar *rules, const uint32_t rulesLength, UCollator *UCA, UErrorCode *status) {
  uint32_t nSize = 0;
  if(U_FAILURE(*status)) {
    return;
  }
  
  src->source = (UChar *)uprv_malloc((2*rulesLength+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
  nSize = unorm_normalize(rules, rulesLength, UNORM_NFD, 0, src->source, 2*rulesLength+UCOL_TOK_EXTRA_RULE_SPACE_SIZE, status);
  if(nSize > (uint32_t)(2*rulesLength+UCOL_TOK_EXTRA_RULE_SPACE_SIZE) || *status == U_BUFFER_OVERFLOW_ERROR) {
    *status = U_ZERO_ERROR;
    src->source = (UChar *)realloc(src->source, (nSize+UCOL_TOK_EXTRA_RULE_SPACE_SIZE)*sizeof(UChar));
    nSize = unorm_normalize(rules, rulesLength, UNORM_NFD, 0, src->source, nSize+UCOL_TOK_EXTRA_RULE_SPACE_SIZE, status);
  }
  src->current = src->source;
  src->end = src->source+nSize;
  src->sourceCurrent = src->source;
  src->extraCurrent = src->end;
  src->extraEnd = src->end+UCOL_TOK_EXTRA_RULE_SPACE_SIZE;
  src->UCA = UCA;
  src->invUCA = ucol_initInverseUCA(status);
  src->resultLen = 0;
  src->lh = 0;
  src->varTop = NULL;
  src->tailored = uhash_open(uhash_hashTokens, uhash_compareTokens, status);
  if(U_FAILURE(*status)) {
    return;
  }
  uhash_setValueDeleter(src->tailored, deleteToken);

  src->opts = (UColOptionSet *)uprv_malloc(sizeof(UColOptionSet));

  uprv_memcpy(src->opts, UCA->options, sizeof(UColOptionSet));

  rulesToParse = src->source;
  src->lh = (UColTokListHeader *)uprv_malloc(512*sizeof(UColTokListHeader));
  src->resultLen = 0;
}

U_INLINE void 
syntaxError(   const UChar* rules, 
               int32_t pos,
               int32_t rulesLen,
               UParseError* parseError){
    parseError->offset = pos;
    parseError->line = 0 ; /* we are not using line numbers */
    
    // for pre-context
    int32_t start = (pos <=U_PARSE_CONTEXT_LEN)? 0 : (pos - (U_PARSE_CONTEXT_LEN-1));
    int32_t stop  = pos;
    
    u_memcpy(parseError->preContext,rules+start,stop-start);
    //null terminate the buffer
    parseError->preContext[stop-start] = 0;
    
    //for post-context
    start = pos+1;
    stop  = ((pos+U_PARSE_CONTEXT_LEN)<= rulesLen )? (pos+(U_PARSE_CONTEXT_LEN-1)) : 
                                                            u_strlen(rules);

    u_memcpy(parseError->postContext,rules+start,stop-start);
    //null terminate the buffer
    parseError->postContext[stop-start]= 0;
}

void 
ucol_uprv_tok_setOptionInImage(UColOptionSet *opts, UColAttribute attrib, UColAttributeValue value) {
  switch(attrib) {
  case UCOL_FRENCH_COLLATION:
    opts->frenchCollation = value;
    break;
  case UCOL_ALTERNATE_HANDLING:
    opts->alternateHandling = value;
    break;
  case UCOL_CASE_FIRST:
    opts->caseFirst = value;
    break;
  case UCOL_CASE_LEVEL:
    opts->caseLevel = value;
    break;
  case UCOL_NORMALIZATION_MODE:
    opts->normalizationMode = value;
    break;
  case UCOL_STRENGTH:
    opts->strength = value;
    break;
  case UCOL_ATTRIBUTE_COUNT:
  default:
    break;
  }
}

#define UTOK_OPTION_COUNT 13

static UBool didInit = FALSE;
/* we can be strict, or we can be lenient */
/* I'd surely be lenient with the option arguments */
/* maybe even with options */
U_STRING_DECL(suboption_00, "non-ignorable", 13);
U_STRING_DECL(suboption_01, "shifted",        7);

U_STRING_DECL(suboption_02, "lower",          5);
U_STRING_DECL(suboption_03, "upper",          5);
U_STRING_DECL(suboption_04, "off",            3);
U_STRING_DECL(suboption_05, "on",             2);
U_STRING_DECL(suboption_06, "1",              1);
U_STRING_DECL(suboption_07, "2",              1);
U_STRING_DECL(suboption_08, "3",              1);



U_STRING_DECL(option_00,    "undefined",      9);
U_STRING_DECL(option_01,    "rearrange",      9);  
U_STRING_DECL(option_02,    "alternate",      9);
U_STRING_DECL(option_03,    "backwards",      9);  
U_STRING_DECL(option_04,    "variable top",  12); 
U_STRING_DECL(option_05,    "top",            3);  
U_STRING_DECL(option_06,    "normalization", 13); 
U_STRING_DECL(option_07,    "caseLevel",      9);  
U_STRING_DECL(option_08,    "caseFirst",      9); 
U_STRING_DECL(option_09,    "scriptOrder",   11);  
U_STRING_DECL(option_10,    "charsetname",   11); 
U_STRING_DECL(option_11,    "charset",        7);  
U_STRING_DECL(option_12,    "before",         6);  


ucolTokSuboption alternateSub[2] = {
  {suboption_00, 13, UCOL_NON_IGNORABLE},
  {suboption_01,  7, UCOL_SHIFTED}
};

ucolTokSuboption caseFirstSub[3] = {
  {suboption_02, 5, UCOL_LOWER_FIRST},
  {suboption_03,  5, UCOL_UPPER_FIRST},
  {suboption_04,  3, UCOL_OFF},
};

ucolTokSuboption onOffSub[2] = {
  {suboption_04, 3, UCOL_OFF},
  {suboption_05, 2, UCOL_ON}
};

ucolTokSuboption frenchSub[1] = {
  {suboption_07, 1, UCOL_ON}
};

ucolTokSuboption beforeSub[3] = {
  {suboption_06, 1, UCOL_PRIMARY},
  {suboption_07, 1, UCOL_SECONDARY},
  {suboption_08, 1, UCOL_TERTIARY}
};


ucolTokOption rulesOptions[UTOK_OPTION_COUNT] = {
 {option_02,  9, alternateSub, 2, UCOL_ALTERNATE_HANDLING}, /*"alternate" */
 {option_03,  9, frenchSub, 1, UCOL_FRENCH_COLLATION}, /*"backwards"      */
 {option_07,  9, onOffSub, 2, UCOL_CASE_LEVEL},  /*"caseLevel"      */
 {option_08,  9, caseFirstSub, 3, UCOL_CASE_FIRST}, /*"caseFirst"   */
 {option_06, 13, onOffSub, 2, UCOL_NORMALIZATION_MODE}, /*"normalization" */
 {option_04, 12, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"variable top"   */
 {option_01,  9, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"rearrange"      */
 {option_05,  3, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"top"            */
 {option_12,  6, beforeSub, 3, UCOL_ATTRIBUTE_COUNT}, /*"before"    */
 {option_00,  9, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"undefined"      */
 {option_09, 11, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"scriptOrder"    */
 {option_10, 11, NULL, 0, UCOL_ATTRIBUTE_COUNT}, /*"charsetname"    */
 {option_11,  7, NULL, 0, UCOL_ATTRIBUTE_COUNT}  /*"charset"        */
};

int32_t  
u_strncmpNoCase(const UChar     *s1, 
     const UChar     *s2, 
     int32_t     n) 
{
    if(n > 0) {
        int32_t rc;
        for(;;) {
            rc = (int32_t)u_tolower(*s1) - (int32_t)u_tolower(*s2);
            if(rc != 0 || *s1 == 0 || --n == 0) {
                return rc;
            }
            ++s1;
            ++s2;
        }
    }
    return 0;
}

uint8_t ucol_uprv_tok_readAndSetOption(UColOptionSet *opts, const UChar* start, const UChar *end, UErrorCode *status) {
  uint32_t i = 0;
  int32_t j=0;
  UBool foundOption = FALSE;
  const UChar *optionArg = NULL;
  if(!didInit) {
    U_STRING_INIT(suboption_00, "non-ignorable", 13);
    U_STRING_INIT(suboption_01, "shifted",        7);

    U_STRING_INIT(suboption_02, "lower",          5);
    U_STRING_INIT(suboption_03, "upper",          5);
    U_STRING_INIT(suboption_04, "off",            3);
    U_STRING_INIT(suboption_05, "on",             2);

    U_STRING_INIT(suboption_06, "1",              1);
    U_STRING_INIT(suboption_07, "2",              1);
    U_STRING_INIT(suboption_08, "3",              1);


    U_STRING_INIT(option_00, "undefined",      9);
    U_STRING_INIT(option_01, "rearrange",      9);  
    U_STRING_INIT(option_02, "alternate",      9);
    U_STRING_INIT(option_03, "backwards",      9);  
    U_STRING_INIT(option_04, "variable top",  12); 
    U_STRING_INIT(option_05, "top",            3);  
    U_STRING_INIT(option_06, "normalization", 13); 
    U_STRING_INIT(option_07, "caseLevel",      9);  
    U_STRING_INIT(option_08, "caseFirst",      9); 
    U_STRING_INIT(option_09, "scriptOrder",   11);  
    U_STRING_INIT(option_10, "charsetname",   11); 
    U_STRING_INIT(option_11, "charset",        7);  
    U_STRING_INIT(option_12, "before",         6);  
  }
  start++; /*skip opening '['*/
  while(i < UTOK_OPTION_COUNT) {
    if(u_strncmpNoCase(start, rulesOptions[i].optionName, rulesOptions[i].optionLen) == 0) {
      foundOption = TRUE;
      if(end - start > rulesOptions[i].optionLen) {
        optionArg = start+rulesOptions[i].optionLen+1; /* start of the options, skip space */
        while(u_isWhitespace(*optionArg)) { /* eat whitespace */
          optionArg++;
        }
      }     
      break;
    }
    i++;
  }

  if(!foundOption) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return FALSE;
  }

  if(i<5) {
    if(optionArg) {
      for(j = 0; j<rulesOptions[i].subSize; j++) {
        if(u_strncmpNoCase(optionArg, rulesOptions[i].subopts[j].subName, rulesOptions[i].subopts[j].subLen) == 0) {
          ucol_uprv_tok_setOptionInImage(opts, rulesOptions[i].attr, rulesOptions[i].subopts[j].attrVal);
          return UCOL_TOK_SUCCESS;
        }
      }
    }
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return FALSE;
  } else if(i == 5) { /* variable top */
    return UCOL_TOK_SUCCESS | UCOL_TOK_VARIABLE_TOP;
  } else if(i == 6) {  /*rearange */
    return UCOL_TOK_SUCCESS;
  } else if(i == 7) {  /*top */
    return UCOL_TOK_SUCCESS | UCOL_TOK_TOP;
  } else if(i == 8) {  /*before*/
    if(optionArg) {
      for(j = 0; j<rulesOptions[i].subSize; j++) {
        if(u_strncmpNoCase(optionArg, rulesOptions[i].subopts[j].subName, rulesOptions[i].subopts[j].subLen) == 0) {
        return UCOL_TOK_SUCCESS | rulesOptions[i].subopts[j].attrVal + 1;
        }
      }
    }
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return 0;
  } else {
    *status = U_UNSUPPORTED_ERROR;
    return 0;
  }
}

const UChar *ucol_tok_parseNextToken(UColTokenParser *src, 
                        uint32_t *strength, 
                        uint32_t *chOffset, uint32_t *chLen, 
                        uint32_t *exOffset, uint32_t *exLen,
                        uint8_t *specs,
                        UBool startOfRules,
                        UParseError *parseError,
                        UErrorCode *status) { 
/* parsing part */

  UBool variableTop = FALSE;
  UBool top = FALSE;
  UBool inChars = TRUE;
  UBool inQuote = FALSE;
  UBool wasInQuote = FALSE;
  UChar *optionEnd = NULL;
  uint8_t before = 0;

  uint32_t newCharsLen = 0, newExtensionLen = 0;
  uint32_t charsOffset = 0, extensionOffset = 0;
  uint32_t newStrength = UCOL_TOK_UNSET; 

  while (src->current < src->end) {
      UChar ch = *(src->current);

    if (inQuote) {
      if (ch == 0x0027/*'\''*/) {
          inQuote = FALSE;
      } else {
        if ((newCharsLen == 0) || inChars) {
          if(newCharsLen == 0) {
            charsOffset = src->extraCurrent - src->source;
          }
          newCharsLen++;
        } else {
          if(newExtensionLen == 0) {
            extensionOffset = src->extraCurrent - src->source;
          }
          newExtensionLen++;
        }
      }
    } else {
      /* Sets the strength for this entry */
      switch (ch) {
        case 0x003D/*'='*/ : 
          if (newStrength != UCOL_TOK_UNSET) {
            goto EndOfLoop;
          }

          /* if we start with strength, we'll reset to top */
          if(startOfRules == TRUE) {
            top = TRUE;
            newStrength = UCOL_TOK_RESET;
            goto EndOfLoop;
          }
          newStrength = UCOL_IDENTICAL;
          break;

        case 0x002C/*','*/:  
          if (newStrength != UCOL_TOK_UNSET) {
            goto EndOfLoop;
          }

          /* if we start with strength, we'll reset to top */
          if(startOfRules == TRUE) {
            top = TRUE;
            newStrength = UCOL_TOK_RESET;
            goto EndOfLoop;
          }
          newStrength = UCOL_TERTIARY;
          break;

        case  0x003B/*';'*/:
          if (newStrength != UCOL_TOK_UNSET) {
            goto EndOfLoop;
          }

          /* if we start with strength, we'll reset to top */
          if(startOfRules == TRUE) {
            top = TRUE;
            newStrength = UCOL_TOK_RESET;
            goto EndOfLoop;
          }
          newStrength = UCOL_SECONDARY;
          break;

        case 0x003C/*'<'*/:  
          if (newStrength != UCOL_TOK_UNSET) {
            goto EndOfLoop;
          }

          /* if we start with strength, we'll reset to top */
          if(startOfRules == TRUE) {
            top = TRUE;
            newStrength = UCOL_TOK_RESET;
            goto EndOfLoop;
          }
          /* before this, do a scan to verify whether this is */
          /* another strength */
          if(*(src->current+1) == 0x003C) {
            src->current++;
            if(*(src->current+1) == 0x003C) {
              src->current++; /* three in a row! */
              newStrength = UCOL_TERTIARY;
            } else { /* two in a row */
              newStrength = UCOL_SECONDARY;
            }
          } else { /* just one */
            newStrength = UCOL_PRIMARY;
          }
          break;

        case 0x0026/*'&'*/:  
          if (newStrength != UCOL_TOK_UNSET) {
            /**/
            goto EndOfLoop;
          }

          newStrength = UCOL_TOK_RESET; /* PatternEntry::RESET = 0 */
          break;

        case 0x005b/*'['*/:
          /* options - read an option, analyze it */
          if((optionEnd = u_strchr(src->current, 0x005d /*']'*/)) != NULL) {
            uint8_t result = ucol_uprv_tok_readAndSetOption(src->opts, src->current, optionEnd, status);
            src->current = optionEnd;
            if(U_SUCCESS(*status)) {
              if(result & UCOL_TOK_TOP) {
                if(newStrength == UCOL_TOK_RESET) { 
                  top = TRUE;
                  src->current++;
                  goto EndOfLoop;
                } else {
                  *status = U_INVALID_FORMAT_ERROR;
                }
              } else if(result & UCOL_TOK_VARIABLE_TOP) {
                if(newStrength != UCOL_TOK_RESET && newStrength != UCOL_TOK_UNSET) {
                  variableTop = TRUE;
                  charsOffset = src->extraCurrent - src->source;
                  newCharsLen = 1;
                  *src->extraCurrent++ = 0xFFFF;
                  src->current++;
                  goto EndOfLoop;
                } else {
                  *status = U_INVALID_FORMAT_ERROR;
                  syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);
                }
              } else if (result & UCOL_TOK_BEFORE){
                if(newStrength == UCOL_TOK_RESET) {
                  before = result & UCOL_TOK_BEFORE;
                } else {
                  *status = U_INVALID_FORMAT_ERROR;
                  syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);

                }
              } 
            } else {
              syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);
              return NULL;
            }
          }
          break;
        /* Ignore the white spaces */
        case 0x0009/*'\t'*/:
        case 0x000C/*'\f'*/:
        case 0x000D/*'\r'*/:
        case 0x000A/*'\n'*/:
        case 0x0020/*' '*/:  
          break; /* skip whitespace TODO use Unicode */
        case 0x002F/*'/'*/:
          wasInQuote = FALSE; /* if we were copying source characters, we want to stop now */
          inChars = FALSE; /* we're now processing expansion */
          break;

        /* found a quote, we're gonna start copying */
        case 0x0027/*'\''*/:
          if (newStrength == UCOL_TOK_UNSET) { /* quote is illegal until we have a strength */
            *status = U_INVALID_FORMAT_ERROR;
            syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);
            return NULL;
          }

          inQuote = TRUE;

          if(inChars) { /* we're doing characters */
            if(wasInQuote == FALSE) {
              charsOffset = src->extraCurrent - src->source;
            }
            if (newCharsLen != 0) {
                uprv_memcpy(src->extraCurrent, src->current - newCharsLen, newCharsLen*sizeof(UChar));
                src->extraCurrent += newCharsLen;
            }
            newCharsLen++;
          } else { /* we're doing an expansion */
            if(wasInQuote == FALSE) {
              extensionOffset = src->extraCurrent - src->source;
            }
            if (newExtensionLen != 0) {
              uprv_memcpy(src->extraCurrent, src->current - newExtensionLen, newExtensionLen*sizeof(UChar));
              src->extraCurrent += newExtensionLen;
            }
            newExtensionLen++;
          }

          wasInQuote = TRUE;

          ch = *(++(src->current)); 
          if(ch == 0x0027) { /* copy the double quote */
            *src->extraCurrent++ = ch;
            inQuote = FALSE;
          }
          break;

        /* '@' is french only if the strength is not currently set */
        /* if it is, it's just a regular character in collation rules */
        case 0x0040/*'@'*/:
          if (newStrength == UCOL_TOK_UNSET) {
            src->opts->frenchCollation = UCOL_ON;
            break;
          }

        default:
          if (newStrength == UCOL_TOK_UNSET) {
            *status = U_INVALID_FORMAT_ERROR;
            syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);
            return NULL;
          }

          if (ucol_tok_isSpecialChar(ch) && (inQuote == FALSE)) {
            *status = U_INVALID_FORMAT_ERROR;
            syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError);
            return NULL;
          }

          if(ch == 0x0000 && src->current+1 == src->end) {
            break;
          }

          if (inChars) {
            if(newCharsLen == 0) {
              charsOffset = src->current - src->source;
            }
            newCharsLen++;
          } else {
            if(newExtensionLen == 0) {
              extensionOffset = src->current - src->source;
            }
            newExtensionLen++;
          }

          break;
        }
    }

    if(wasInQuote) {
      if(ch != 0x27) {
        *src->extraCurrent++ = ch;
      }
      if(src->extraCurrent == src->extraEnd) {
        /* reallocate */
        UChar *newSrc = (UChar *)uprv_realloc(src->source, (src->extraEnd-src->source)*2*sizeof(UChar));
        if(newSrc != NULL) {
          src->current = newSrc + (src->current - src->source);
          src->extraCurrent = newSrc + (src->extraCurrent - src->source);
          src->end = newSrc + (src->end - src->source);
          src->extraEnd = newSrc + (src->extraEnd-src->source)*2;
          src->sourceCurrent = newSrc + (src->sourceCurrent-src->source);
          src->source = newSrc;
        } else {
          *status = U_MEMORY_ALLOCATION_ERROR;
          return NULL;
        }
      }
    }

      src->current++;
    }

 EndOfLoop:
    wasInQuote = FALSE;
  if (newStrength == UCOL_TOK_UNSET) {
    return NULL;
  }

  if (newCharsLen == 0 && top == FALSE) {
    syntaxError(src->source,(src->current-src->source),(src->end-src->source),parseError); 
    *status = U_INVALID_FORMAT_ERROR;
    return NULL;
  }

  *strength = newStrength; 

  *chOffset = charsOffset;
  *chLen = newCharsLen;
  *exOffset = extensionOffset;
  *exLen = newExtensionLen;
  *specs = (UCOL_TOK_VARIABLE_TOP * (variableTop?1:0)) | (UCOL_TOK_TOP * (top?1:0)) | before;

  return src->current;
}

inline void getVirginBefore(UColTokenParser *src, UColToken *sourceToken, uint32_t strength, uint32_t *charsOffset, uint32_t *newCharsLen, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
      /* this is a virgin before - we need to fish the anchor from the UCA */
  collIterate s;
  uint32_t baseCE = UCOL_NOT_FOUND, baseContCE = UCOL_NOT_FOUND;
  uint32_t CE, SecondCE;
  uint32_t invPos;
  if(sourceToken != NULL) {
    init_collIterate(src->UCA, src->source+((sourceToken->source)&0xFFFFFF), 1, &s); 
  } else {
    init_collIterate(src->UCA, src->source+*charsOffset, 1, &s); 
  }

  baseCE = ucol_getNextCE(src->UCA, &s, status) & 0xFFFFFF3F;
  baseContCE = ucol_getNextCE(src->UCA, &s, status);
  if(baseContCE == UCOL_NO_MORE_CES) {
    baseContCE = 0;
  }

  invPos = ucol_inv_getPrevCE(baseCE, baseContCE, &CE, &SecondCE, strength);

  uint32_t *CETable = (uint32_t *)((uint8_t *)src->invUCA+src->invUCA->table);
  uint32_t ch = CETable[3*invPos+2];

  if((ch &  UCOL_INV_SIZEMASK) != 0) {
    uint32_t *conts = (uint32_t *)((uint8_t *)src->invUCA+src->invUCA->conts);
    uint32_t offset = (ch & UCOL_INV_OFFSETMASK);
    ch = conts[offset];
  }      
  *src->extraCurrent++ = (UChar)ch;        
  *charsOffset = src->extraCurrent - src->source - 1;
  *newCharsLen = 1;
}

/*
Processing Description
  1 Build a ListList. Each list has a header, which contains two lists (positive 
  and negative), a reset token, a baseCE, nextCE, and previousCE. The lists and 
  reset may be null. 
  2 As you process, you keep a LAST pointer that points to the last token you 
  handled. 
*/

uint32_t ucol_tok_assembleTokenList(UColTokenParser *src, UParseError *parseError,UErrorCode *status) {
  UColToken *lastToken = NULL;
  const UChar *parseEnd = NULL;
  uint32_t expandNext = 0;
  UBool variableTop = FALSE;
  UBool top = FALSE;
  uint8_t specs = 0;

  UColTokListHeader *ListList = NULL;

  uint32_t newCharsLen = 0, newExtensionsLen = 0;
  uint32_t charsOffset = 0, extensionOffset = 0;
  uint32_t newStrength = UCOL_TOK_UNSET; 

  UHashtable *uchars2tokens = src->tailored;
  ListList = src->lh;

  while(src->current < src->end) {
  
    parseEnd = ucol_tok_parseNextToken(src, 
                        &newStrength, 
                        &charsOffset, &newCharsLen, 
                        &extensionOffset, &newExtensionsLen,
                        &specs,
                        (UBool)(lastToken == NULL),
                        parseError,
                        status);

    variableTop = ((specs & UCOL_TOK_VARIABLE_TOP) != 0);
    top = ((specs & UCOL_TOK_TOP) != 0);

    if(U_SUCCESS(*status) && parseEnd != NULL) {
      UColToken *sourceToken = NULL;
      uint32_t key = 0;
      uint32_t lastStrength = UCOL_TOK_UNSET;
      
      if(lastToken != NULL ) {
        lastStrength = lastToken->strength;
      }

      key = newCharsLen << 24 | charsOffset;

      /*  4 Lookup each source in the CharsToToken map, and find a sourceToken */
      sourceToken = (UColToken *)uhash_geti(uchars2tokens, (int32_t)key);

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

          sourceToken->debugSource = *(src->source + charsOffset);

          sourceToken->polarity = UCOL_TOK_POLARITY_POSITIVE; /* TODO: this should also handle reverse */
          sourceToken->next = NULL;
          sourceToken->previous = NULL;
          sourceToken->noOfCEs = 0;
          sourceToken->noOfExpCEs = 0;
          uhash_puti(uchars2tokens, (int32_t)sourceToken->source, sourceToken, status);
        } else {
          /* we could have fished out a reset here */
          if(sourceToken->strength != UCOL_TOK_RESET && lastToken != sourceToken) {
            /* otherwise remove sourceToken from where it was. */
            if(sourceToken->next != NULL) {
              if(sourceToken->next->strength > sourceToken->strength) {
                sourceToken->next->strength = sourceToken->strength;
              }
              sourceToken->next->previous = sourceToken->previous;
            } else {
              sourceToken->listHeader->last = sourceToken->previous;
            }

            if(sourceToken->previous != NULL) {
              sourceToken->previous->next = sourceToken->next;
            } else {
              sourceToken->listHeader->first = sourceToken->next;
            }
            sourceToken->next = NULL;
            sourceToken->previous = NULL;
          }
        }

        sourceToken->strength = newStrength;
        sourceToken->listHeader = lastToken->listHeader;

        /*
        1.	Find the strongest strength in each list, and set strongestP and strongestN 
        accordingly in the headers. 
        */
        if(lastStrength == UCOL_TOK_RESET 
          || sourceToken->listHeader->first == 0) {
        /* If LAST is a reset 
              insert sourceToken in the list. */
          if(sourceToken->listHeader->first == 0) {
            sourceToken->listHeader->first = sourceToken;
            sourceToken->listHeader->last = sourceToken;
          } else { /* we need to find a place for us */
            /* and we'll get in front of the same strength */
            if(sourceToken->listHeader->first->strength <= sourceToken->strength) {
              sourceToken->next = sourceToken->listHeader->first;
              sourceToken->next->previous = sourceToken;
              sourceToken->listHeader->first = sourceToken;
              sourceToken->previous = NULL;
            } else {
              lastToken = sourceToken->listHeader->first;
              while(lastToken->next != NULL && lastToken->next->strength > sourceToken->strength) {
                lastToken = lastToken->next;
              }
              if(lastToken->next != NULL) {
                lastToken->next->previous = sourceToken;
              } else {
                sourceToken->listHeader->last = sourceToken;
              }
              sourceToken->previous = lastToken;
              sourceToken->next = lastToken->next;
              lastToken->next = sourceToken;
            }
          }
        } else {
        /* Otherwise (when LAST is not a reset) 
              if polarity (LAST) == polarity(relation), insert sourceToken after LAST, 
              otherwise insert before. 
              when inserting after or before, search to the next position with the same 
              strength in that direction. (This is called postpone insertion).         */
          if(sourceToken != lastToken) { 
            if(lastToken->polarity == sourceToken->polarity) {
              while(lastToken->next != NULL && lastToken->next->strength > sourceToken->strength) {
                lastToken = lastToken->next;
              }
              sourceToken->previous = lastToken;
              if(lastToken->next != NULL) {
                lastToken->next->previous = sourceToken;
              } else {
                sourceToken->listHeader->last = sourceToken;
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
                sourceToken->listHeader->first = sourceToken;
              }
              sourceToken->previous = lastToken->previous;
              lastToken->previous = sourceToken;
            }
          } else { /* repeated one thing twice in rules, stay with the stronger strength */
            if(lastStrength < sourceToken->strength) {
              sourceToken->strength = lastStrength;
            }
          }
        }

        /* if the token was a variable top, we're gonna put it in */
        if(variableTop == TRUE && src->varTop == NULL) {
          variableTop = FALSE;
          src->varTop = sourceToken;
        }

       // Treat the expansions.
       // There are two types of expansions: explicit (x / y) and reset based propagating expansions 
       // (&abc * d * e <=> &ab * d / c * e / c) 
       // if both of them are in effect for a token, they are combined.

        sourceToken->expansion = newExtensionsLen << 24 | extensionOffset;

        if(expandNext != 0) {
          if(sourceToken->strength == UCOL_PRIMARY) { /* primary strength kills off the implicit expansion */
            expandNext = 0;
          } else if(sourceToken->expansion == 0) { /* if there is no expansion, implicit is just added to the token */
            sourceToken->expansion = expandNext;
          } else { /* there is both explicit and implicit expansion. We need to make a combination */
            memcpy(src->extraCurrent, src->source + (expandNext & 0xFFFFFF), (expandNext >> 24)*sizeof(UChar));
            memcpy(src->extraCurrent+(expandNext >> 24), src->source + extensionOffset, newExtensionsLen*sizeof(UChar));
            sourceToken->expansion = ((expandNext >> 24) + newExtensionsLen)<<24 | (src->extraCurrent - src->source);
            src->extraCurrent += (expandNext >> 24) + newExtensionsLen;
          }
        }

        // This is just for debugging purposes
        if(sourceToken->expansion != 0) {
          sourceToken->debugExpansion = *(src->source + extensionOffset);
        } else {
          sourceToken->debugExpansion = 0;
        }
      } else {
        if(sourceToken == NULL) { /* this is a reset, but it might still be somewhere in the tailoring, in shorter form */
          uint32_t searchCharsLen = newCharsLen;
          while(searchCharsLen > 1 && sourceToken == NULL) {
            searchCharsLen--;
            key = searchCharsLen << 24 | charsOffset;
            sourceToken = (UColToken *)uhash_geti(uchars2tokens, (int32_t)key);
          }
          if(sourceToken != NULL) {
            expandNext = (newCharsLen - searchCharsLen) << 24 | (charsOffset + searchCharsLen);
          }
        }

        uint32_t CE = UCOL_NOT_FOUND, SecondCE = UCOL_NOT_FOUND;
        collIterate s;

        if((specs & UCOL_TOK_BEFORE) != 0) { /* we're doing before */
          uint8_t strength = (specs & UCOL_TOK_BEFORE) - 1;
          if(sourceToken != NULL && sourceToken->strength != UCOL_TOK_RESET) { 
            /* this is a before that is already ordered in the UCA - so we need to get the previous with good strength */
            while(sourceToken->strength > strength && sourceToken->previous != NULL) {
              sourceToken = sourceToken->previous;
            }
            /* here, either we hit the strength or NULL */
            if(sourceToken->strength == strength) {
              if(sourceToken->previous != NULL) {
                sourceToken = sourceToken->previous;
              } else { /* start of list */
                sourceToken = sourceToken->listHeader->reset;
              }              
            } else { /* we hit NULL */
              /* we should be doing the else part */
              sourceToken = sourceToken->listHeader->reset;
              getVirginBefore(src, sourceToken, strength, &charsOffset, &newCharsLen, status);
              sourceToken = NULL;
            }
          } else {
            getVirginBefore(src, sourceToken, strength, &charsOffset, &newCharsLen, status);
            sourceToken = NULL;
          }
        }


        if(lastToken != NULL && lastStrength == UCOL_TOK_RESET) {
          /* if the previous token was also a reset, */
          /*this means that we have two consecutive resets */
          /* and we want to remove the previous one if empty*/
          if(ListList[src->resultLen-1].first == NULL) {
            src->resultLen--;
          }
        }

      /*  5 If the relation is a reset: 
          If sourceToken is null 
            Create new list, create new sourceToken, make the baseCE from source, put 
            the sourceToken in ListHeader of the new list */
        if(sourceToken == NULL) {

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
          sourceToken->listHeader = &ListList[src->resultLen];
          /*
            3 Consider each item: relation, source, and expansion: e.g. ...< x / y ... 
              First convert all expansions into normal form. Examples: 
                If "xy" doesn't occur earlier in the list or in the UCA, convert &xy * c * 
                d * ... into &x * c/y * d * ... 
                Note: reset values can never have expansions, although they can cause the 
                very next item to have one. They may be contractions, if they are found 
                earlier in the list. 
          */
          if(top == FALSE) {
            uint32_t resetCharsOffset;

            init_collIterate(src->UCA, src->source+charsOffset, newCharsLen, &s);

            CE = ucol_getNextCE(src->UCA, &s, status);
            resetCharsOffset = s.pos - src->source;

            SecondCE = ucol_getNextCE(src->UCA, &s, status);
    
            ListList[src->resultLen].baseCE = CE & 0xFFFFFF3F;
            if(isContinuation(SecondCE)) {
              ListList[src->resultLen].baseContCE = SecondCE;
            } else {
              ListList[src->resultLen].baseContCE = 0;
            }
            if(newCharsLen > 1) {
              sourceToken->source = ((resetCharsOffset - charsOffset ) << 24) | charsOffset;
              expandNext = ((newCharsLen + charsOffset - resetCharsOffset)<<24) | (resetCharsOffset);
            } else {
              expandNext = 0;
            }
          } else { /* top == TRUE */
            top = FALSE;
            ListList[src->resultLen].baseCE = UCOL_RESET_TOP_VALUE;
            ListList[src->resultLen].baseContCE = 0;
          }


          ListList[src->resultLen].first = NULL;
          ListList[src->resultLen].last = NULL;
          ListList[src->resultLen].first = NULL;
          ListList[src->resultLen].last = NULL;

          ListList[src->resultLen].reset = sourceToken;

          src->resultLen++;
          uhash_puti(uchars2tokens, (int32_t)sourceToken->source, sourceToken, status);
        } else { /* reset to something already in rules */
          top = FALSE;
        }
      }
      /*  7 After all this, set LAST to point to sourceToken, and goto step 3. */  
      lastToken = sourceToken;
    } else {
      return 0;
    }
  }

  if(src->resultLen > 0 && ListList[src->resultLen-1].first == NULL) {
    src->resultLen--;
  }
  return src->resultLen;
}


void ucol_tok_closeTokenList(UColTokenParser *src) {
  if(src->tailored != NULL) {
    uhash_close(src->tailored);
  }
  if(src->lh != NULL) {
    uprv_free(src->lh);
  }
  if(src->source != NULL) {
    uprv_free(src->source);
  }
  if(src->opts != NULL) {
    uprv_free(src->opts);
  }
}

int32_t
uhash_hashTokens(const UHashKey k) {
  int32_t hash = 0;
  uint32_t key = (uint32_t)k.integer;
  if (key != 0) {
      int32_t len = (key & 0xFF000000)>>24;
      int32_t inc = ((len - 32) / 32) + 1;

      const UChar *p = (key & 0x00FFFFFF) + rulesToParse;
      const UChar *limit = p + len;    

      while (p<limit) {
          hash = (hash * 37) + *p;
          p += inc;
      }
  }
  return hash;
}

UBool uhash_compareTokens(const UHashKey key1, const UHashKey key2) {
    uint32_t p1 = (uint32_t) key1.integer;
    uint32_t p2 = (uint32_t) key2.integer;
    const UChar *s1 = (p1 & 0x00FFFFFF) + rulesToParse;
    const UChar *s2 = (p2 & 0x00FFFFFF) + rulesToParse;
    uint32_t s1L = ((p1 & 0xFF000000) >> 24);
    uint32_t s2L = ((p2 & 0xFF000000) >> 24);
    const UChar *end = s1+s1L-1;

    if (p1 == p2) {
        return TRUE;
    }
    if (p1 == 0 || p2 == 0) {
        return FALSE;
    }
    if(s1L != s2L) {
      return FALSE;
    }
    if(p1 == p2) {
      return TRUE;
    }
    while((s1 < end) && *s1 == *s2) {
      ++s1;
      ++s2;
    }
    if(*s1 == *s2) {
      return TRUE;
    } else {
      return FALSE;
    }
}

void deleteToken(void *token) {
    UColToken *tok = (UColToken *)token;
    uprv_free(tok);
}
