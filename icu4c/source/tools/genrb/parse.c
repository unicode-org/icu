/*
*******************************************************************************
*
*   Copyright (C) 1998-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File parse.c
*
* Modification History:
*
*   Date        Name        Description
*   05/26/99    stephen     Creation.
*   02/25/00    weiv        Overhaul to write udata
*******************************************************************************
*/

#include "parse.h"
#include "error.h"
#include "uhash.h"
#include "cmemory.h"
#include "read.h"
#include "unicode/ustdio.h"
#include "ustr.h"
#include "reslist.h"
#include "unicode/ustring.h"
#include "unicode/ucol.h"

U_CAPI const UChar * U_EXPORT2 ucol_getDefaultRulesArray(uint32_t *size);

U_STRING_DECL(k_start_string, "string", 6);
U_STRING_DECL(k_start_binary, "binary", 6);
U_STRING_DECL(k_start_table, "table", 5);
U_STRING_DECL(k_start_int, "int", 3);
U_STRING_DECL(k_start_array, "array", 5);
U_STRING_DECL(k_start_intvector, "intvector", 9);
U_STRING_DECL(k_start_reserved, "reserved", 8);

static UBool didInit=FALSE;

/* Node IDs for the state transition table. */
enum ENode {
  eError,
  eInitial,   /* Next: Locale name */
  eGotLoc,    /* Next: { */
  eIdle,      /* Next: Tag name | } */
  eGotTag,    /* Next: { | : */
  eNode5,     /* Next: Data | Subtag */
  eNode6,     /* Next: } | { | , */
  eList,      /* Next: List data */
  eNode8,     /* Next: , */
  eTagList,   /* Next: Subtag data */
  eNode10,    /* Next: } */
  eNode11,    /* Next: Subtag */
  eNode12,    /* Next: { */
  e2dArray,   /* Next: Data | } */
  eNode14,    /* Next: , | } */
  eNode15,    /* Next: , | } */
  eNode16,     /* Next: { | } */
  eTypeStart, /* Next: Type name */
  eGotType	  /* Next: { */
};

/* Action codes for the state transtiion table. */
enum EAction {
  /* Generic actions */
  eNOP       = 0x0100, /* Do nothing */
  eOpen      = 0x0200, /* Open a new locale data block with the data
			  string as the locale name */
  eClose     = 0x0300, /* Close a locale data block */
  eSetTag    = 0x0400, /* Record the last string as the tag name */
  
  /* Comma-delimited lists */
  eBegList   = 0x1100, /* Start a new string list with the last string
			  as the first element */
  eEndList   = 0x1200, /* Close a string list being built */
  eListStr   = 0x1300, /* Record the last string as a data string and
			  increment the index */
  eStr       = 0x1400, /* Record the last string as a singleton string */
  
  /* 2-d lists */
  eBeg2dList = 0x2100, /* Start a new 2d string list with no elements as yet */
  eEnd2dList = 0x2200, /* Close a 2d string list being built */
  e2dStr     = 0x2300, /* Record the last string as a 2d string */
  eNewRow    = 0x2400, /* Start a new row */
  
  /* Tagged lists */
  eBegTagged = 0x3100, /* Start a new tagged list with the last
			  string as the first subtag */
  eEndTagged = 0x3200, /* Close a tagged list being build */
  eSubtag    = 0x3300, /* Record the last string as the subtag */
  eTaggedStr = 0x3400,  /* Record the last string as a tagged string */

  /* Type support */
  eBegType = 0x4100, /* Start getting a type */
  eSetType = 0x4200 /* Record and init type */
};

/* A struct which encapsulates a node ID and an action. */
struct STransition {
  enum ENode fNext;
  enum EAction fAction;
};

/* This table describes an ATM (state machine) which parses resource
   bundle text files rather strictly. Each row represents a node. The
   columns of that row represent transitions into other nodes. Most
   transitions are "eError" because most transitions are
   disallowed. For example, if the parser has just seen a tag name, it
   enters node 4 ("eGotTag"). The state table then marks only one
   valid transition, which is into node 5, upon seeing an eOpenBrace
   token. We allow an extra comma after the last element in a
   comma-delimited list (transition from eList to eIdle on
   kCloseBrace). */
static struct STransition gTransitionTable [] = {
  /*                kString           kOpenBrace            kCloseBrace         kComma            
  /*eError*/    {eError,eNOP},       {eError,eNOP},        {eError,eNOP},      {eError,eNOP},	
  
  /*eInitial*/  {eGotLoc,eOpen},     {eError,eNOP},        {eError,eNOP},      {eError,eNOP},	
  /*eGotLoc*/   {eError,eNOP},       {eIdle,eNOP},         {eError,eNOP},      {eError,eNOP},	
 
  /*eIdle*/     {eGotTag,eSetTag},   {eError,eNOP},        {eInitial,eClose},  {eError,eNOP},	
  /*eGotTag*/   {eError,eNOP},       {eNode5,eNOP},        {eError,eNOP},      {eError,eNOP},	
  /*eNode5*/    {eNode6,eNOP},       {e2dArray,eBeg2dList},{eError,eNOP},      {eError,eNOP},	
  /*eNode6*/    {eError,eNOP},       {eTagList,eBegTagged},{eIdle,eStr},       {eList,eBegList},
  
  /*eList*/     {eNode8,eListStr},   {eError,eNOP},         {eIdle,eEndList},  {eError,eNOP},	
  /*eNode8*/    {eError,eNOP},       {eError,eNOP},         {eIdle,eEndList},  {eList,eNOP},	
 
  /*eTagList*/  {eNode10,eTaggedStr},{eError,eNOP},         {eError,eNOP},     {eError,eNOP},	
  /*eNode10*/   {eError,eNOP},       {eError,eNOP},         {eNode11,eNOP},    {eError,eNOP},	
  /*eNode11*/   {eNode12,eNOP},      {eError,eNOP},         {eIdle,eEndTagged},{eError,eNOP},	
  /*eNode12*/   {eError,eNOP},       {eTagList,eSubtag},    {eError,eNOP},     {eError,eNOP},	

  /*e2dArray*/  {eNode14,e2dStr},    {eError,eNOP},         {eNode15,eNOP},    {eError,eNOP},	
  /*eNode14*/   {eError,eNOP},       {eError,eNOP},         {eNode15,eNOP},    {e2dArray,eNOP},	
  /*eNode15*/   {eError,eNOP},       {e2dArray,eNewRow},    {eIdle,eEnd2dList},{eNode16,eNOP},	
  /*eNode16*/   {eError,eNOP},       {e2dArray,eNewRow},    {eIdle,eEnd2dList},{eError,eNOP},	
  /*eTypeStart*/{eGotType,eSetType}, {eError,eNOP},         {eError,eNOP},     {eError,eNOP},	
  /*eGotType*/	{eError,eNOP},       {eError,eNOP},         {eError,eNOP},     {eError,eNOP}	
};

/* Row length is 4 */
#define GETTRANSITION(row,col) (gTransitionTable[col + (row<<2)])
/* Not anymore, it is 5 now */
/*#define GETTRANSITION(row,col) (gTransitionTable[col + (row*5)])*/

/*********************************************************************
 * Hashtable glue
 ********************************************************************/

static UBool get(UHashtable *hash, const struct UString* tag) {
    return uhash_get(hash, tag) != NULL;
}

static void put(UHashtable *hash, const struct UString *tag,
                UErrorCode* status) {
    struct UString* key = uprv_malloc(sizeof(struct UString));
    ustr_init(key);
    ustr_cpy(key, tag, status);
    uhash_put(hash, key, (void*)1, status);
}

static void freeUString(void* ustr) {
    ustr_deinit(ustr);
    uprv_free(ustr);
}

static int32_t hashUString(const void* ustr) {
    return uhash_hashUChars(((struct UString*)ustr)->fChars);
}

static UBool compareUString(const void* ustr1, const void* ustr2) {
    return uhash_compareUChars(((struct UString*)ustr1)->fChars,
                               ((struct UString*)ustr2)->fChars);
}

/*********************************************************************
 * parse
 ********************************************************************/

struct SRBRoot*
parse(FileStream *f, const char *cp,
      UErrorCode *status)
{
  struct UFILE *file;
  enum ETokenType type;
  enum ENode node;
  struct STransition t;

  struct UString token;
  struct UString tag;

    char cTag[1024];
    char cSubTag[1024];
    struct SRBRoot *bundle = NULL;
    struct SResource *rootTable = NULL;
    struct SResource *temp = NULL;
    struct SResource *temp1 = NULL;
    struct SResource *temp2 = NULL;
	UBool colEl = FALSE;

    /* Hashtable for keeping track of seen tag names */
    struct UHashtable *data;


    if(U_FAILURE(*status)) return NULL;

    /* setup */

  ustr_init(&token);
  ustr_init(&tag);
/*  
    cTag = uprv_malloc(1024);
    if(cTag == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    cSubTag = uprv_malloc(1024);
    if(cSubTag == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
*/

  node = eInitial;
  data = 0;

  file = u_finit((FILE *)f, 0, cp);
/*  file = u_finit(f, cp, status); */

    bundle = bundle_open(status);
    rootTable = bundle -> fRoot;

  if(U_FAILURE(*status) || file == NULL) goto finish;
  
  /* iterate through the stream */
  for(;;) {

    /* get next token from stream */
    type = getNextToken(file, &token, status);
    if(U_FAILURE(*status)) goto finish;

    switch(type) {
    case tok_EOF:
      *status = (node == eInitial) ? U_ZERO_ERROR : U_INVALID_FORMAT_ERROR;
      if(U_FAILURE(*status)) {
        setErrorText("Unexpected EOF encountered");
      }
      goto finish;
      /*break;*/

    case tok_error:
      *status = U_INVALID_FORMAT_ERROR;
      goto finish;
      /*break;*/
      
    default:
      break;
    }
    
    t = GETTRANSITION(node, type);
    node = t.fNext;
    
    if(node == eError) {
        *status = U_INVALID_FORMAT_ERROR;
        goto finish;
    }
    
    switch(t.fAction) {
    case eNOP:
        break;
      
      /* Record the last string as the tag name */
    case eSetTag:
        ustr_cpy(&tag, &token, status);
        u_UCharsToChars(tag.fChars, cTag, u_strlen(tag.fChars)+1);
        if(U_FAILURE(*status)) goto finish;
		if(uprv_strchr(cTag, ':')) {
			/* type modificator - do the type modification*/
		} else if(uprv_strcmp(cTag, "CollationElements") == 0) {
			colEl = TRUE;
		}
        /*if(uhash_get(data, uhash_hashUString(tag.fChars)) != 0) {*/
        if(get(data, &tag)) {
	        char *s;
	        *status = U_INVALID_FORMAT_ERROR;
            s = uprv_malloc(1024);
            strcpy(s, "Duplicate tag name detected: ");
            u_austrcpy(s+strlen(s), tag.fChars);
            setErrorText(s);
	        goto finish;
        }

        break;

      /* Record a singleton string */
    case eStr:
        if(temp != NULL) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        temp = string_open(bundle, cTag, token.fChars, token.fLength, status);
        table_add(rootTable, temp, status);
		if(colEl == TRUE) {
			const UChar * defaultRulesArray;
			uint32_t defaultRulesArrayLength = 0;
			/* do the collation elements */
			int32_t len = 0;
			uint8_t *data = NULL;
			UCollator *coll = NULL; 
			UChar *rules = NULL;
			defaultRulesArray = ucol_getDefaultRulesArray(&defaultRulesArrayLength);
			rules = uprv_malloc(sizeof(defaultRulesArray[0])*(defaultRulesArrayLength + token.fLength));
			uprv_memcpy(rules, defaultRulesArray, defaultRulesArrayLength*sizeof(defaultRulesArray[0]));
			uprv_memcpy(rules + defaultRulesArrayLength, token.fChars, token.fLength*sizeof(token.fChars[0]));
			
			coll = ucol_openRules(rules, defaultRulesArrayLength + token.fLength, 0, 0, status);
			if(U_SUCCESS(*status) && coll !=NULL) {
                /* This is just for testing & should be removed
				temp1 = bin_open(bundle, "%%Collation", sizeof(defaultRulesArray[0])*(defaultRulesArrayLength + token.fLength), (uint8_t *) rules, status);
				table_add(rootTable, temp1, status);
                */
/*
				data = ucol_cloneRuleData(coll, &len, status);
				if(U_SUCCESS(*status) && data != NULL) {
					temp1 = bin_open(bundle, "%%Collation", len, data, status);
					table_add(rootTable, temp1, status);
					uprv_free(data);
				}
*/
				ucol_close(coll);
			}
			uprv_free(rules);
			colEl = FALSE;
		}
        /*uhash_put(data, tag.fChars, status);*/
        put(data, &tag, status);
        if(U_FAILURE(*status)) goto finish;
        temp = NULL;
        break;

      /* Begin a string list */
    case eBegList:
        if(temp != NULL) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        temp = array_open(bundle, cTag, status);
        temp1 = string_open(bundle, NULL, token.fChars, token.fLength, status);
        array_add(temp, temp1, status);
        temp1 = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
      /* Record a comma-delimited list string */      
    case eListStr:
        temp1 = string_open(bundle, NULL, token.fChars, token.fLength, status);
        array_add(temp, temp1, status);
        temp1 = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
      /* End a string list */
    case eEndList:
        /*uhash_put(data, tag.fChars, status);*/
        put(data, &tag, status);
        table_add(rootTable, temp, status);
        temp = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eBeg2dList:
        if(temp != NULL) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        temp = array_open(bundle, cTag, status);
        temp1 = array_open(bundle, NULL, status);
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eEnd2dList:
        /*uhash_put(data, tag.fChars, status);*/
        put(data, &tag, status);
        array_add(temp, temp1, status);
        table_add(rootTable, temp, status);
        temp1 = NULL;
        temp = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case e2dStr:
        temp2 = string_open(bundle, NULL, token.fChars, token.fLength, status);
        array_add(temp1, temp2, status);
        temp2 = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eNewRow:
        array_add(temp, temp1, status);
        temp1 = array_open(bundle, NULL, status);
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eBegTagged:
        if(temp != NULL) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        temp = table_open(bundle, cTag, status);
        u_UCharsToChars(token.fChars, cSubTag, u_strlen(token.fChars)+1);
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eEndTagged:
        /*uhash_put(data, tag.fChars, status);*/
        put(data, &tag, status);
        table_add(rootTable, temp, status);
        temp = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
    case eTaggedStr:
        temp1 = string_open(bundle, cSubTag, token.fChars, token.fLength, status);
        table_add(temp, temp1, status);
        temp1 = NULL;
        if(U_FAILURE(*status)) goto finish;
        break;
      
      /* Record the last string as the subtag */
    case eSubtag:
        u_UCharsToChars(token.fChars, cSubTag, u_strlen(token.fChars)+1);
        if(U_FAILURE(*status)) goto finish;
        if(table_get(temp, cSubTag, status) != 0) {
	        *status = U_INVALID_FORMAT_ERROR;
	        setErrorText("Duplicate subtag found in tagged list");
	        goto finish;
        }
        break;
      
    case eOpen:
      if(data != 0) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        bundle_setlocale(bundle, token.fChars, status);
        if(U_FAILURE(*status)) goto finish;
        data = uhash_open(hashUString, compareUString, status);
        uhash_setKeyDeleter(data, freeUString);
        break;
      
    case eClose:
        if(data == 0) {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
        }
        break;
	case eSetType:
		/* type recognition */
		if(!didInit) {
			U_STRING_INIT(k_start_string, "string", 6);
			U_STRING_INIT(k_start_binary, "binary", 6);
			U_STRING_INIT(k_start_table, "table", 5);
			U_STRING_INIT(k_start_int, "int", 3);
			U_STRING_INIT(k_start_array, "array", 5);
			U_STRING_INIT(k_start_intvector, "intvector", 9);
			U_STRING_INIT(k_start_reserved, "reserved", 8);
			didInit=TRUE;
		}
		if(u_strcmp(token.fChars, k_start_string) == 0) {
			node = eGotTag;
		} else if(u_strcmp(token.fChars, k_start_array) == 0) {
			node = eGotTag;
		} else if(u_strcmp(token.fChars, k_start_table) == 0) {
			node = eGotTag;
		} else if(u_strcmp(token.fChars, k_start_binary) == 0) {
			/* start of binary */
		} else if(u_strcmp(token.fChars, k_start_int) == 0) {
			/* start of integer */
		} else if(u_strcmp(token.fChars, k_start_intvector) == 0) {
			/* start of intvector */
		} else if(u_strcmp(token.fChars, k_start_reserved) == 0) {
			/* start of reserved */
		} else {
	        *status = U_INTERNAL_PROGRAM_ERROR;
	        goto finish;
		}
		break;
    }
  }

 finish:

  /* clean  up */
  
  if(data != 0)
    uhash_close(data);

  ustr_deinit(&token);
  ustr_deinit(&tag);

    /*uprv_free(cTag);*/
    /*uprv_free(cSubTag);*/

  if(file != 0)
    u_fclose(file);

  return bundle;
}
