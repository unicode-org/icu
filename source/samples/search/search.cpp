/**************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
***************************************************************************
*   file name:  colex.cpp
*
*   created on: 2001June8
*   created by: Helena Shih
*
*   Sample code for the ICU Search C++ routines.  
*/
#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/locid.h"

#include "strsrch.h"

int main()
{
   UErrorCode status = U_ZERO_ERROR;
   UnicodeString target("A quick fox jumped over the lazy dog.", "");
   UnicodeString easyPatterns[] = {"FoX", "CAT", "jump", "under" };
   int exactOffsets[] = { -1, -1, 12, -1 };
   int tertiaryOffsets[] = { 8, -1, 12, -1 };
   UnicodeString monkeyTarget("abcdefgh");
   UnicodeString monkeyTarget2("ijklmnop");

   int i, j;
   int pos = 0; 
   StringSearch *searchIter = new StringSearch(easyPatterns[0], target, status);
   fprintf(stdout, "\n");
   if (U_FAILURE(status))
   {
        fprintf(stderr, "Failed to create a StringSearch object for the default locale.\n");
   }
   fprintf(stdout, "Try with default normalization mode and strength.\n");
   i = 0;
   while (TRUE)
   {
       status = U_ZERO_ERROR;
       searchIter->reset();
       pos = searchIter->next();
       if ( pos != exactOffsets[i] )
          fprintf(stdout, "Exact match failed at the index %d pattern.\n", i);
       
       i ++;
       if (i == 4) {
           break;
       }

       searchIter->setPattern(easyPatterns[i], status);
       if (U_FAILURE(status))
       {
            fprintf(stderr, "Failed to set a pattern for %d element.\n", i);
            continue;
       }
   }
   fprintf(stdout, "Try now with strength == primary.\n");
   status = U_ZERO_ERROR;
   searchIter->setStrength(Collator::PRIMARY, status);
   if (U_FAILURE(status))
   {
        fprintf(stderr, "Failed to set strength of the string search object.\n");
   }
   searchIter->reset();
   searchIter->setPattern(easyPatterns[0], status);
   if (U_FAILURE(status))
   {
        fprintf(stderr, "Failed to set a pattern for the first element.\n");
   }
   pos = searchIter->first();
   if (pos != tertiaryOffsets[0])
       fprintf(stdout, "Tertiary match failed at the first pattern.\n");
   for (i = 1; i < 4; i++)
   {
       status = U_ZERO_ERROR;
       searchIter->setPattern(easyPatterns[i], status);
       searchIter->reset();
       pos = searchIter->next();
       if (pos != tertiaryOffsets[i])
           fprintf(stdout, "Tertiary match failed at index %d pattern.\n", i);
   }
   // Going backwards
   searchIter->reset();
   searchIter->setPattern(easyPatterns[--i], status);
   if (U_FAILURE(status))
   {
        fprintf(stderr, "Failed to set a pattern for the last element.\n");
   }
   pos = searchIter->last();
   if (pos != tertiaryOffsets[i])
       fprintf(stdout, "Tertiary match failed at the last pattern.\n");
   for (; i >= 1 ; --i)
   {
       status = U_ZERO_ERROR;       
       searchIter->setPattern(easyPatterns[i-1], status);
       searchIter->reset();
       pos = searchIter->previous();
       if (pos != tertiaryOffsets[i-1])
           fprintf(stdout, "Walking backwards: tertiary match failed at index %d pattern.\n", i);
   }
   status = U_ZERO_ERROR;
  searchIter->setTarget(monkeyTarget);
  if (U_FAILURE(status))
  {
      fprintf(stderr, "Failed to set a pattern for the monkey target.\n");
      goto cleanup;
  }
  searchIter->setStrength(Collator::TERTIARY, status);
  // change direction again 
   searchIter->reset();
   searchIter->setPattern(monkeyTarget, status);
   if (U_FAILURE(status))
   {
        fprintf(stderr, "Failed to set a pattern as monkey test itself.\n");
   }
   pos = searchIter->first();
   if (pos == -1)
       fprintf(stdout, "Matching monkey test itself failed.\n");
  for (i = 0; i < monkeyTarget.length() - 1; i++)
   {
       // will always find its substring
       for (j = i+1; j < monkeyTarget.length(); j++)
       {
            UnicodeString temp;
            status = U_ZERO_ERROR;
            searchIter->reset();
            monkeyTarget.extract(i, j, temp);
            searchIter->setPattern(temp, status);
            if (U_FAILURE(status))
            {
                fprintf(stderr, "Failed to set a pattern for the %d -th monkey pattern of length %d.\n", i, j);
                continue;
            }
            pos = searchIter->next();
            if (pos == -1)
               fprintf(stdout, "Monkey match failed at index %d in monkey pattern of length %d.\n", i, j);
       }
   }
  status = U_ZERO_ERROR;
  searchIter->setTarget(monkeyTarget2);
  if (U_FAILURE(status))
  {
      fprintf(stderr, "Failed to set a pattern for the monkey target2.\n");
      goto cleanup;
  }
  for (i = 0; i < monkeyTarget.length() - 1; i++)
   {
       // will never find the match
        UnicodeString temp;
        status = U_ZERO_ERROR;
        monkeyTarget.extract(i, monkeyTarget.length(), temp);
        searchIter->reset();
        searchIter->setPattern(temp, status);
        if (U_FAILURE(status))
        {
            fprintf(stderr, "Failed to set a pattern for the monkey pattern at offset index %d.\n", i);
            continue;
        }
        pos = searchIter->next();
        if (pos != -1)
           fprintf(stdout, "Monkey mismatch failed at index %d in monkey pattern.\n", i);
   }
   
cleanup:
    delete searchIter;
    return 0;
}

