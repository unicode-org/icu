/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
*
* File CBIAPTS.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Creation
*********************************************************************************
/*C API TEST FOR BREAKITERATOR */
/**
* This is an API test.  It doesn't test very many cases, and doesn't
* try to test the full functionality.  It just calls each function in the class and
* verifies that it works on a basic level.
**/
#include "uloc.h"
#include "ubrk.h"
#include "utypes.h"
#include "ustring.h"
#include "cintltst.h"
#include "cbiapts.h"
#include<stdio.h>
#include<string.h>


void addBrkIterAPITest(TestNode** root)
{
    addTest(root, &TestBreakIteratorCAPI, "tstxtbd/capitst/TestBreakIteratorCAPI");

}

void TestBreakIteratorCAPI()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *word, *sentence, *line, *character, *b;
    UChar text[50];
    UTextOffset start,pos,end,to;
    int32_t count = 0;
    u_uastrcpy(text, "He's from Africa. ""Mr. Livingston, I presume?"" Yeah");
    status  = U_ZERO_ERROR;

/*test ubrk_open()*/
    log_verbose("\nTesting BreakIterator open functions\n");
                                            
    /* Use french for fun */
    word         = ubrk_open(UBRK_WORD, "en_US", text, u_strlen(text), &status);
    if(FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for word breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  word breakiterator\n");
    }
    
    sentence     = ubrk_open(UBRK_SENTENCE, "en_US", text, u_strlen(text), &status);
    if(FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for sentence breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  sentence breakiterator\n");
    }
    
    line         = ubrk_open(UBRK_SENTENCE, "en_US", text, u_strlen(text), &status);
    if(FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for line breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  line breakiterator\n");
    }
    
    character     = ubrk_open(UBRK_SENTENCE, "en_US", text, u_strlen(text), &status);
    if(FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for character breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  character breakiterator\n");
    }

/* ======= Test ubrk_countAvialable() and ubrk_getAvilable() */

    log_verbose("\nTesting ubrk_countAvailable() and ubrk_getAvailable()\n");
    count=ubrk_countAvailable();
    /* use something sensible w/o hardcoding the count */
    if(count < 0){
        log_err("FAIL: Error in ubrk_countAvialable() returned %d\n", count);
    }
    else{
        log_verbose("PASS: ubrk_countAvialable() successful returned %d\n", count);
    }


/*========Test ubrk_first(), ubrk_last()...... and other functions*/
  
   log_verbose("\nTesting the functions for word\n");
    start = ubrk_first(word);
    if(start!=0)
        log_err("error ubrk_start(word) did not return 0\n");
    log_verbose("first (word = %d\n", (int32_t)start);
       pos=ubrk_next(word);
    if(pos!=4)
        log_err("error ubrk_next(word) did not return 4\n");
    log_verbose("next (word = %d\n", (int32_t)pos);
    pos=ubrk_following(word, 4);
    if(pos!=5)
        log_err("error ubrl_following(word,4) did not return 6\n");
    log_verbose("next (word = %d\n", (int32_t)pos);
    end=ubrk_last(word);
    if(end!=49)
        log_err("error ubrk_last(word) did not return 49\n");
    log_verbose("last (word = %d\n", (int32_t)end);
    
    pos=ubrk_previous(word);
    log_verbose("%d   %d\n", end, pos);
     
    pos=ubrk_previous(word);
    log_verbose("%d \n", pos);




    
    log_verbose("\nTesting the functions for character\n");
    ubrk_first(character);
    pos = ubrk_following(character, 5);
    if(pos!=18)
       log_err("error ubrk_following(character,5) did not return 18\n");
    log_verbose("Following (character,5) = %d\n", (int32_t)pos);
    pos=ubrk_following(character, 18);
    if(pos!=22)
       log_err("error ubrk_following(character,18) did not return 22\n");
    log_verbose("Followingcharacter,18) = %d\n", (int32_t)pos);
    pos=ubrk_preceding(character, 22);
    if(pos!=18)
       log_err("error ubrk_preceding(character,22) did not return 18\n");
    log_verbose("preceding(character,22) = %d\n", (int32_t)pos);
    

    log_verbose("\nTesting the functions for line\n");
    ubrk_first(line);
    pos = ubrk_next(line);
    if(pos!=18)
        log_err("error ubrk_next(line) did not return 18\n");
    log_verbose("Next (line) = %d\n", (int32_t)pos);
    pos=ubrk_following(line, 18);
    if(pos!=22)
        log_err("error ubrk_following(line) did not return 22\n");
    log_verbose("following (line) = %d\n", (int32_t)pos);

    
    log_verbose("\nTesting the functions for sentence\n");
    ubrk_first(sentence);
    pos = ubrk_current(sentence);
    log_verbose("Current(sentence) = %d\n", (int32_t)pos);
       pos = ubrk_last(sentence);
    if(pos!=49)
        log_err("error ubrk_last for sentence did not return 49\n");
    log_verbose("Last (sentence) = %d\n", (int32_t)pos);
    ubrk_first(sentence);
    to = ubrk_following( sentence, 0 );
    if (to == 0) log_err("ubrk_following returned 0\n");
    to = ubrk_preceding( sentence, to );
    if (to != 0) log_err("ubrk_preceding didn't return 0\n");
    if (ubrk_first(sentence)!=ubrk_current(sentence)) {
        log_err("error in ubrk_first() or ubrk_current()\n");
    }
    
 
    /*---- */
/*Testing ubrk_open and ubrk_close()*/
   log_verbose("\nTesting open and close for us locale\n");
    b = ubrk_open(UBRK_WORD, "fr_FR", text, u_strlen(text), &status);
    if (FAILURE(status)) {
        log_err("ubrk_open for word returned NULL: %s\n", myErrorName(status));
    }
    ubrk_close(b);

  
    
    ubrk_close(word);
    ubrk_close(sentence);
    ubrk_close(line);
    ubrk_close(character);


}
