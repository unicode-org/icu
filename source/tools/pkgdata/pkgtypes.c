/**************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  pkgdata.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000may16
*   created by: Steven \u24C7 Loomis
*
*  common types for pkgdata
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "pkgtypes.h"


const char *pkg_writeCharListWrap(FileStream *s, CharList *l, const char *delim, const char *brk, int32_t quote)
{
    int32_t ln = 0;
    char buffer[1024];
	const CharList *ol = NULL;
    while(l != NULL)
    {
        if(l->str)
        {
            uprv_strncpy(buffer, l->str, 1020);
            buffer[1019]=0;

            if(quote < 0) { /* remove quotes */
                if(buffer[uprv_strlen(buffer)-1] == '"') {
                    buffer[uprv_strlen(buffer)-1] = '\0';
                }
                if(buffer[0] == '"') {
                    uprv_strcpy(buffer, buffer+1);
                }
            } else if(quote > 0) { /* add quotes */
                if(l->str[0] != '"') {
                    uprv_strcpy(buffer, "\"");
                    uprv_strncat(buffer, l->str,1020);
                }
                if(l->str[uprv_strlen(l->str)-1] != '"') {
                    uprv_strcat(buffer, "\"");
                }
            }
            T_FileStream_write(s, buffer, uprv_strlen(buffer));
        }

        ln  += uprv_strlen(l->str);

		ol = l;

        if(l->next && delim)
        {
            if(ln > 60 && brk) {
                ln  = 0;
                T_FileStream_write(s, brk, uprv_strlen(brk));
            }
            T_FileStream_write(s, delim, uprv_strlen(delim));
        }
        l = l->next;
    }
    return NULL;
}


const char *pkg_writeCharList(FileStream *s, CharList *l, const char *delim, int32_t quote)
{
    char buffer[1024];
    while(l != NULL)
    {
        if(l->str)
        {
            uprv_strncpy(buffer, l->str, 1023);
            buffer[1023]=0;
            if(uprv_strlen(l->str) >= 1023)
            {
                fprintf(stderr, "%s:%d: Internal error, line too long (greater than 1023 chars)\n",
                        __FILE__, __LINE__);
                exit(0);
            }
            if(quote < 0) { /* remove quotes */
                if(buffer[uprv_strlen(buffer)-1] == '"') {
                    buffer[uprv_strlen(buffer)-1] = '\0';
                }
                if(buffer[0] == '"') {
                    uprv_strcpy(buffer, buffer+1);
                }
            } else if(quote > 0) { /* add quotes */
                if(l->str[0] != '"') {
                    uprv_strcpy(buffer, "\"");
                    uprv_strcat(buffer, l->str);
                }
                if(l->str[uprv_strlen(l->str)-1] != '"') {
                    uprv_strcat(buffer, "\"");
                }
            }
            T_FileStream_write(s, buffer, uprv_strlen(buffer));
        }
        
        if(l->next && delim)
        {
            T_FileStream_write(s, delim, uprv_strlen(delim));
        }
        l = l->next;
    }
    return NULL;
}


/*
 * Count items . 0 if null
 */
uint32_t pkg_countCharList(CharList *l)
{
  uint32_t c = 0;
  while(l != NULL)
  {
    c++;
    l = l->next;
  }

  return c;
}

/* 
 * Prepend string to CharList
 */
CharList *pkg_prependToList(CharList *l, const char *str)
{
  CharList *newList;
  newList = uprv_malloc(sizeof(CharList));

  /* test for NULL */
  if(newList == NULL) {
    return NULL;
  }

  newList->str = str;
  newList->next = l;
  return newList;
}

/* 
 * append string to CharList. *end or even end can be null if you don't 
 * know it.[slow]
 * Str is adopted!
 */
CharList *pkg_appendToList(CharList *l, CharList** end, const char *str)
{
  CharList *endptr = NULL, *tmp;

  if(end == NULL)
  {
    end = &endptr;
  }
  
  /* FIND the end */
  if((*end == NULL) && (l != NULL))
  {
    tmp = l;
    while(tmp->next)
    {
      tmp = tmp->next;
    }

    *end = tmp;
  }

  /* Create a new empty list and append it */
  if(l == NULL)
    {
      l = pkg_prependToList(NULL, str);
    }
  else
    {
      (*end)->next = pkg_prependToList(NULL, str);
    }

  /* Move the end pointer. */
  if(*end)
    {
      (*end) = (*end)->next;
    }
  else
    {
      *end = l;
    }

  return l;
}

/*
 * Delete list 
 */
void pkg_deleteList(CharList *l)
{
  
  while(l != NULL)
  {
    uprv_free((void*)l->str);
    l = l->next;
  }
}

UBool  pkg_listContains(CharList *l, const char *str)
{
  for(;l;l=l->next){
    if(!uprv_strcmp(l->str, str)) {
      return TRUE;
    }
  }

  return FALSE;
}
