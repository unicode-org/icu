/*
*******************************************************************************
*
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include <iostream.h>
#include <unicode/brkiter.h>
#include <stdlib.h>

U_CFUNC int c_main(void);


void printUnicodeString(const UnicodeString &s) {
  char charBuf[1000];
  s.extract(0, s.length(), charBuf, sizeof(charBuf)-1, 0);   
  charBuf[sizeof(charBuf)-1] = 0;          
  cout << charBuf;
}



void printTextRange( BreakIterator& iterator, 
		     int32_t start, int32_t end )
{
  CharacterIterator *strIter = iterator.getText().clone();
  UnicodeString  s;
  strIter->getText(s);

  cout << " " << start << " " << end << " |" ;
  printUnicodeString(s);
  cout << "|" << '\n';
  delete strIter;
}


/* Print each element in order: */
void printEachForward( BreakIterator& boundary)
{
  int32_t start = boundary.first();
  for (int32_t end = boundary.next();
       end != BreakIterator::DONE;
       start = end, end = boundary.next())
    {
      printTextRange( boundary, start, end );
    }
}

/* Print each element in reverse order: */
void printEachBackward( BreakIterator& boundary)
{
  int32_t end = boundary.last();
  for (int32_t start = boundary.previous();
       start != BreakIterator::DONE;
       end = start, start = boundary.previous())
    {
      printTextRange( boundary, start, end );
    }
}

/* Print the first element */
void printFirst(BreakIterator& boundary)
{
  int32_t start = boundary.first();
  int32_t end = boundary.next();
  printTextRange( boundary, start, end );
}

/* Print the last element */
void printLast(BreakIterator& boundary)
{
  int32_t end = boundary.last();
  int32_t start = boundary.previous();
  printTextRange( boundary, start, end );
}

/* Print the element at a specified position */
void printAt(BreakIterator &boundary, int32_t pos )
{
  int32_t end = boundary.following(pos);
  int32_t start = boundary.previous();
  printTextRange( boundary, start, end );
}

/* Creating and using text boundaries */
int main( void )
{
  cout << "ICU Break Iterator Sample Program\n\n";
  cout << "C++ Break Iteration\n\n";
  BreakIterator* boundary;
  UnicodeString stringToExamine("Aaa bbb ccc. Ddd eee fff.");
  cout << "Examining: ";
  printUnicodeString(stringToExamine);
  cout << endl;

  //print each sentence in forward and reverse order
  UErrorCode status = U_ZERO_ERROR;
  boundary = BreakIterator::createSentenceInstance(
						   Locale::getUS(), status );
  if (U_FAILURE(status)) {
    cout << 
      "failed to create sentence break iterator.  status = " 
	 << u_errorName(status);
    exit(1);
  }

  boundary->setText(stringToExamine);
  cout << "\n Sentence Boundaries... \n";
  cout << "----- forward: -----------" << '\n';
  printEachForward(*boundary);
  cout << "----- backward: ----------" << '\n';
  printEachBackward(*boundary);
  delete boundary;

  //print each word in order
  cout << "\n Word Boundaries... \n";
  boundary = BreakIterator::createWordInstance(
					       Locale::getUS(), status);
  boundary->setText(stringToExamine);
  cout << "----- forward: -----------" << '\n';
  printEachForward(*boundary);
  //print first element
  cout << "----- first: -------------" << '\n';
  printFirst(*boundary);
  //print last element
  cout << "----- last: --------------" << '\n';
  printLast(*boundary);
  //print word at charpos 10
  cout << "----- at pos 10: ---------" << '\n';
  printAt(*boundary, 10 );

  delete boundary;
  cout.flush();

  // Call the C version
  return c_main();
}
