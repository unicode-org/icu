/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CFORMTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/

/* FormatTest is a medium top level test for everything in the  C FORMAT API */

#include "cintltst.h"
#include "cformtst.h"

void addCalTest(TestNode**);
void addDateForTest(TestNode**);
void addNumForTest(TestNode**);
void addMsgForTest(TestNode**);
void addDateForRgrTest(TestNode**);
void addNumFrDepTest(TestNode**);
void addDtFrDepTest(TestNode**);


void checkHeap()
{
  int i;
  void *n[2048];

  log_info("checkHeap...");

  
  for(i=0;i<1;i++)
    {
            n[i]= malloc(i+15); 
    }
  
  /*  for(i=0;i<2048;i++)
    {
      free(n[i]);
      }*/
}

void addFormatTest(TestNode** root)
{
    addCalTest(root);

    /*    addTest(root, &checkHeap, "/tsformat/mallocTest2" ); */

    addDateForTest(root);

    /*    addTest(root, &checkHeap, "/tsformat/mallocTest3" ); */

    addNumForTest(root);

    /*    addTest(root, &checkHeap, "/tsformat/mallocTest4" );  */

    addNumFrDepTest(root);

    /* addTest(root, &checkHeap, "/tsformat/mallocTest5" );  */

    addMsgForTest(root);
    addDateForRgrTest(root);
    addDtFrDepTest(root);
    
    
}
/*INternal functions used*/

UChar* myDateFormat(UDateFormat* dat, UDate d1)
{
    UChar *result1;
    int32_t resultlength, resultlengthneeded;
    UFieldPosition pos;
    UErrorCode status = U_ZERO_ERROR;
    

    resultlength=0;
    resultlengthneeded=udat_format(dat, d1, NULL, resultlength, &pos, &status);
    if(status==U_BUFFER_OVERFLOW_ERROR)
    {
        status=U_ZERO_ERROR;
        resultlength=resultlengthneeded+1;
        result1=(UChar*)malloc(sizeof(UChar) * resultlength);
        udat_format(dat, d1, result1, resultlength, &pos, &status);
    }
    if(U_FAILURE(status))
    {
        log_err("Error in formatting using udat_format(.....): %s\n", myErrorName(status));
        return 0;
    }
    return result1;

}
