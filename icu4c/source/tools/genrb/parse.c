/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
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
*******************************************************************************
*/

#include "parse.h"
#include "error.h"
#include "uhash.h"
#include "cmemory.h"
#include "read.h"
#include "unicode/ustdio.h"
#include "ustr.h"
#include "list.h"
#include "rblist.h"
#include "unicode/ustring.h"

/* Node IDs for the state transition table. */
enum ENode {
  eError,
  eInitial,   /* Next: Locale name */
  eGotLoc,    /* Next: { */
  eIdle,      /* Next: Tag name | } */
  eGotTag,    /* Next: { */
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
  eNode16     /* Next: { | } */
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
  eTaggedStr = 0x3400  /* Record the last string as a tagged string */
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
  /* kString           kOpenBrace            kCloseBrace         kComma*/
  {eError,eNOP},       {eError,eNOP},        {eError,eNOP},      {eError,eNOP},
  
  {eGotLoc,eOpen},     {eError,eNOP},        {eError,eNOP},      {eError,eNOP},
  {eError,eNOP},       {eIdle,eNOP},         {eError,eNOP},      {eError,eNOP},
  
  {eGotTag,eSetTag},   {eError,eNOP},        {eInitial,eClose},  {eError,eNOP},
  {eError,eNOP},       {eNode5,eNOP},        {eError,eNOP},      {eError,eNOP},
  {eNode6,eNOP},       {e2dArray,eBeg2dList},{eError,eNOP},      {eError,eNOP},
  {eError,eNOP},       {eTagList,eBegTagged},{eIdle,eStr},       {eList,eBegList},
  
  {eNode8,eListStr},   {eError,eNOP},         {eIdle,eEndList},  {eError,eNOP},
  {eError,eNOP},       {eError,eNOP},         {eIdle,eEndList},  {eList,eNOP},
  
  {eNode10,eTaggedStr},{eError,eNOP},         {eError,eNOP},     {eError,eNOP},
  {eError,eNOP},       {eError,eNOP},         {eNode11,eNOP},    {eError,eNOP},
  {eNode12,eNOP},      {eError,eNOP},         {eIdle,eEndTagged},{eError,eNOP},
  {eError,eNOP},       {eTagList,eSubtag},    {eError,eNOP},     {eError,eNOP},

  {eNode14,e2dStr},    {eError,eNOP},         {eNode15,eNOP},    {eError,eNOP},
  {eError,eNOP},       {eError,eNOP},         {eNode15,eNOP},    {e2dArray,eNOP},
  {eError,eNOP},       {e2dArray,eNewRow},    {eIdle,eEnd2dList},{eNode16,eNOP},
  {eError,eNOP},       {e2dArray,eNewRow},    {eIdle,eEnd2dList},{eError,eNOP} 
};

/* Row length is 4 */
#define GETTRANSITION(row,col) (gTransitionTable[col + (row<<2)])

/*********************************************************************
 * Hashtable glue
 ********************************************************************/

static bool_t get(UHashtable *hash, const struct UString* tag) {
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

static bool_t compareUString(const void* ustr1, const void* ustr2) {
    return uhash_compareUChars(((struct UString*)ustr1)->fChars,
                               ((struct UString*)ustr2)->fChars);
}

/*********************************************************************
 * parse
 ********************************************************************/

struct SRBItemList*
parse(FileStream *f, const char *cp,
      UErrorCode *status)
{
  struct UFILE *file;
  enum ETokenType type;
  enum ENode node;
  struct STransition t;

  struct UString token;
  struct UString tag;
  struct UString subtag;
  struct UString localeName;
  struct UString keyname;

  struct SRBItem *item;
  struct SRBItemList *list;
  struct SList *current;

  /* Hashtable for keeping track of seen tag names */
  struct UHashtable *data;


  if(U_FAILURE(*status)) return 0;

  /* setup */

  ustr_init(&token);
  ustr_init(&tag);
  ustr_init(&subtag);
  ustr_init(&localeName);
  ustr_init(&keyname);

  node = eInitial;
  data = 0;
  current = 0;
  item = 0;

  file = u_finit((FILE *)f, 0, cp);
/*  file = u_finit(f, cp, status); */
  list = rblist_open(status);
  if(U_FAILURE(*status) || file == NULL) goto finish;
  
  /* iterate through the stream */
  for(;;) {

    /* get next token from stream */
    type = getNextToken(file, &token, status);
    if(U_FAILURE(*status)) goto finish;

    switch(type) {
    case tok_EOF:
      *status = (node == eInitial) ? U_ZERO_ERROR : U_INVALID_FORMAT_ERROR;
      setErrorText("Unexpected EOF encountered");
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
      if(U_FAILURE(*status)) goto finish;
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
      if(current != 0) {
	*status = U_INTERNAL_PROGRAM_ERROR;
	goto finish;
      }
      current = strlist_open(status);
      strlist_add(current, token.fChars, status);
      item = make_rbitem(tag.fChars, current, status);
      rblist_add(list, item, status);
      put(data, &tag, status);
      if(U_FAILURE(*status)) goto finish;
      current = 0;
      item = 0;
      break;

      /* Begin a string list */
    case eBegList:
      if(current != 0) {
	*status = U_INTERNAL_PROGRAM_ERROR;
	goto finish;
      }
      current = strlist_open(status);
      strlist_add(current, token.fChars, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
      /* Record a comma-delimited list string */      
    case eListStr:
      strlist_add(current, token.fChars, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
      /* End a string list */
    case eEndList:
      put(data, &tag, status);
      item = make_rbitem(tag.fChars, current, status);
      rblist_add(list, item, status);
      if(U_FAILURE(*status)) goto finish;
      current = 0;
      item = 0;
      break;
      
    case eBeg2dList:
      if(current != 0) {
	*status = U_INTERNAL_PROGRAM_ERROR;
	goto finish;
      }
      current = strlist2d_open(status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
    case eEnd2dList:
      put(data, &tag, status);
      item = make_rbitem(tag.fChars, current, status);
      rblist_add(list, item, status);
      if(U_FAILURE(*status)) goto finish;
      current = 0;
      item = 0;
      break;
      
    case e2dStr:
      strlist2d_add(current, token.fChars, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
    case eNewRow:
      strlist2d_newRow(current, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
    case eBegTagged:
      if(current != 0) {
	*status = U_INTERNAL_PROGRAM_ERROR;
	goto finish;
      }
      current = taglist_open(status);
      ustr_cpy(&subtag, &token, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
    case eEndTagged:
      put(data, &tag, status);
      item = make_rbitem(tag.fChars, current, status);
      rblist_add(list, item, status);
      if(U_FAILURE(*status)) goto finish;
      current = 0;
      item = 0;
      break;
      
    case eTaggedStr:
      taglist_add(current, subtag.fChars, token.fChars, status);
      if(U_FAILURE(*status)) goto finish;
      break;
      
      /* Record the last string as the subtag */
    case eSubtag:
      ustr_cpy(&subtag, &token, status);
      if(U_FAILURE(*status)) goto finish;
      if(taglist_get(current, subtag.fChars, status) != 0) {
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
      ustr_cpy(&localeName, &token, status);
      rblist_setlocale(list, localeName.fChars, status);
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
    }
  }

 finish:

  /* clean  up */
  
  if(data != 0)
    uhash_close(data);

  if(item != 0)
    uprv_free(item);

  ustr_deinit(&token);
  ustr_deinit(&tag);
  ustr_deinit(&subtag);
  ustr_deinit(&localeName);
  ustr_deinit(&keyname);

  if(file != 0)
    u_fclose(file);

  return list;
}
