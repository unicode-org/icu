/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CBIAPTS.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Creation
*********************************************************************************/
/*C API TEST FOR BREAKITERATOR */
/**
* This is an API test.  It doesn't test very many cases, and doesn't
* try to test the full functionality.  It just calls each function in the class and
* verifies that it works on a basic level.
**/
#include "unicode/uloc.h"
#include "unicode/ubrk.h"
#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cbiapts.h"
#include<stdio.h>
#include<string.h>


void addBrkIterAPITest(TestNode** root)
{
    addTest(root, &TestBreakIteratorCAPI, "tstxtbd/capitst/TestBreakIteratorCAPI");

}

#define CLONETEST_ITERATOR_COUNT 2

static void TestBreakIteratorCAPI()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *word, *sentence, *line, *character, *b, *bogus;
    UChar text[50];
    UTextOffset start,pos,end,to;
    int32_t i;
    int32_t count = 0;

   	UBreakIterator * someIterators [CLONETEST_ITERATOR_COUNT];
	UBreakIterator * someClonedIterators [CLONETEST_ITERATOR_COUNT];
	UBreakIterator * brk;
	uint8_t buffer [CLONETEST_ITERATOR_COUNT] [U_BRK_SAFECLONE_BUFFERSIZE];
	int32_t bufferSize = U_BRK_SAFECLONE_BUFFERSIZE;

    u_uastrcpy(text, "He's from Africa. ""Mr. Livingston, I presume?"" Yeah");
    status  = U_ZERO_ERROR;


/*test ubrk_open()*/
    log_verbose("\nTesting BreakIterator open functions\n");
                                            
    /* Use french for fun */
    word         = ubrk_open(UBRK_WORD, "en_US", text, u_strlen(text), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for word breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  word breakiterator\n");
    }
    
    sentence     = ubrk_open(UBRK_SENTENCE, "en_US", text, u_strlen(text), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for sentence breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  sentence breakiterator\n");
    }
    
    line         = ubrk_open(UBRK_LINE, "en_US", text, u_strlen(text), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for line breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  line breakiterator\n");
    }
    
    character     = ubrk_open(UBRK_CHARACTER, "en_US", text, u_strlen(text), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for character breakiterator: %s\n", myErrorName(status));
    }
    else{
        log_verbose("PASS: Successfully opened  character breakiterator\n");
    }
    /*trying to open an illegal iterator*/
    bogus     = ubrk_open((UBreakIteratorType)4, "en_US", text, u_strlen(text), &status);
    if(U_SUCCESS(status)){
        log_err("FAIL: Error in ubrk_open() for BOGUS breakiterator. Expected U_MEMORY_ALLOCATION_ERROR");
    }
    if(U_FAILURE(status)){
        if(status != U_MEMORY_ALLOCATION_ERROR){
            log_err("FAIL: Error in ubrk_open() for BOGUS breakiterator. Expected U_MEMORY_ALLOCATION_ERROR\n Got %s\n", myErrorName(status));
        }
    }
    status=U_ZERO_ERROR;


/* ======= Test ubrk_countAvialable() and ubrk_getAvialable() */

    log_verbose("\nTesting ubrk_countAvailable() and ubrk_getAvailable()\n");
    count=ubrk_countAvailable();
    /* use something sensible w/o hardcoding the count */
    if(count < 0){
        log_err("FAIL: Error in ubrk_countAvialable() returned %d\n", count);
    }
    else{
        log_verbose("PASS: ubrk_countAvialable() successful returned %d\n", count);
    }
    for(i=0;i<count;i++)
    {
        log_verbose("%s\n", ubrk_getAvailable(i)); 
        if (ubrk_getAvailable(i) == 0)
            log_err("No locale for which breakiterator is applicable\n");
        else 
            log_verbose("A locale %s for which breakiterator is applicable\n",ubrk_getAvailable(i));
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
    if(pos!=6)
       log_err("error ubrk_following(character,5) did not return 6\n");
    log_verbose("Following (character,5) = %d\n", (int32_t)pos);
    pos=ubrk_following(character, 18);
    if(pos!=19)
       log_err("error ubrk_following(character,18) did not return 19\n");
    log_verbose("Followingcharacter,18) = %d\n", (int32_t)pos);
    pos=ubrk_preceding(character, 22);
    if(pos!=21)
       log_err("error ubrk_preceding(character,22) did not return 21\n");
    log_verbose("preceding(character,22) = %d\n", (int32_t)pos);
    

    log_verbose("\nTesting the functions for line\n");
    pos=ubrk_first(line);
    if(pos != 0)
        log_err("error ubrk_first(line) returned %d, expected 0\n", (int32_t)pos);
    pos = ubrk_next(line);
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
    if (U_FAILURE(status)) {
        log_err("ubrk_open for word returned NULL: %s\n", myErrorName(status));
    }
    ubrk_close(b);

    ubrk_close(word);
    ubrk_close(sentence);
    ubrk_close(line);
    ubrk_close(character);

    /*Testing ubrk_safeClone */

	/* US & Thai - rule-based & dictionary based */
	someIterators[0] = ubrk_open(UBRK_WORD, "en_US", text, u_strlen(text), &status);
	someIterators[1] = ubrk_open(UBRK_WORD, "th_TH", text, u_strlen(text), &status);
	
    /* test each type of iterator */
    for (i = 0; i < CLONETEST_ITERATOR_COUNT; i++)
	{

	    /* Check the various error & informational states */

	    /* Null status - just returns NULL */
	    if (0 != ubrk_safeClone(someIterators[i], buffer[i], &bufferSize, 0))
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with null status\n");
	    }
	    /* error status - should return 0 & keep error the same */
	    status = U_MEMORY_ALLOCATION_ERROR;
	    if (0 != ubrk_safeClone(someIterators[i], buffer[i], &bufferSize, &status) || status != U_MEMORY_ALLOCATION_ERROR)
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with incoming error status\n");
	    }
	    status = U_ZERO_ERROR;

	    /* Null buffer size pointer - just returns NULL & set error to U_ILLEGAL_ARGUMENT_ERROR*/
	    if (0 != ubrk_safeClone(someIterators[i], buffer[i], 0, &status) || status != U_ILLEGAL_ARGUMENT_ERROR)
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with null bufferSize pointer\n");
	    }
	    status = U_ZERO_ERROR;
	
	    /* buffer size pointer is 0 - fill in pbufferSize with a size */
	    bufferSize = 0;
	    if (0 != ubrk_safeClone(someIterators[i], buffer[i], &bufferSize, &status) || U_FAILURE(status) || bufferSize <= 0)
	    {
		    log_err("FAIL: Cloned Iterator failed a sizing request ('preflighting')\n");
	    }
	    /* Verify our define is large enough  */
	    if (U_BRK_SAFECLONE_BUFFERSIZE < bufferSize)
	    {
		    log_err("FAIL: Pre-calculated buffer size is too small\n");
	    }
	    /* Verify we can use this run-time calculated size */
	    if (0 == (brk = ubrk_safeClone(someIterators[i], buffer[i], &bufferSize, &status)) || U_FAILURE(status))
	    {
		    log_err("FAIL: Iterator can't be cloned with run-time size\n");
	    }
	    if (brk) ubrk_close(brk);
	    /* size one byte too small - should allocate & let us know */
	    --bufferSize;
	    if (0 == (brk = ubrk_safeClone(someIterators[i], 0, &bufferSize, &status)) || status != U_SAFECLONE_ALLOCATED_ERROR)
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with too-small buffer size\n");
	    }
	    if (brk) ubrk_close(brk);
	    status = U_ZERO_ERROR;
	    bufferSize = U_BRK_SAFECLONE_BUFFERSIZE;

    	/* Null buffer pointer - return Iterator & set error to U_SAFECLONE_ALLOCATED_ERROR */
	    if (0 == (brk = ubrk_safeClone(someIterators[i], 0, &bufferSize, &status)) || status != U_SAFECLONE_ALLOCATED_ERROR)
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with null buffer pointer\n");
	    }
	    if (brk) ubrk_close(brk);
	    status = U_ZERO_ERROR;

	    /* Null Iterator - return NULL & set U_ILLEGAL_ARGUMENT_ERROR */
	    if (0 != ubrk_safeClone(0, buffer[i], &bufferSize, &status) || status != U_ILLEGAL_ARGUMENT_ERROR)
	    {
		    log_err("FAIL: Cloned Iterator failed to deal correctly with null Iterator pointer\n");
	    }
	    status = U_ZERO_ERROR;
	
    	/* Do these cloned Iterators work at all - make a first & next call */
		bufferSize = U_BRK_SAFECLONE_BUFFERSIZE;
		someClonedIterators[i] = ubrk_safeClone(someIterators[i], buffer[i], &bufferSize, &status);

	    start = ubrk_first(someClonedIterators[i]);
        if(start!=0)
            log_err("error ubrk_start(clone) did not return 0\n");
        pos=ubrk_next(someClonedIterators[i]);
        if(pos!=4)
            log_err("error ubrk_next(clone) did not return 4\n");
    		
	    ubrk_close(someClonedIterators[i]);
		ubrk_close(someIterators[i]);
	}
}
