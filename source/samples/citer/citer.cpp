/*
*******************************************************************************
*
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#include "unicode/uchriter.h"
#include "unicode/schriter.h"
#include "unicode/ustring.h"
#include <stdio.h>
#include <iostream.h>
#include <unicode/brkiter.h>
#include <stdlib.h>

void printUnicodeString(const UnicodeString &s) {
  char charBuf[1000];
  s.extract(0, s.length(), charBuf, sizeof(charBuf)-1, 0);   
  charBuf[sizeof(charBuf)-1] = 0;          
  cout << charBuf;
}

void printUChar(UChar32 ch) {
  char charBuf[1000];
  charBuf[sizeof(charBuf)-1] = 0;          
  if(ch < 127) {
    cout << (char) ch;
  } else if (ch == CharacterIterator::DONE) {
    cout << "[CharacterIterator::DONE = 0xFFFF]";
  } else {
    cout << "[" << ch << "]";
  }
}

class Test
{
public:
  void TestUChariter();
  void TestStringiter();
};

void Test::TestUChariter() {
  const char testChars[] = "Now is the time for all good men to come "
    "to the aid of their country.";

  UnicodeString testString(testChars,"");
  const UChar *testText  = testString.getTerminatedBuffer();

  UCharCharacterIterator iter(testText, u_strlen(testText));
  UCharCharacterIterator* test2 = (UCharCharacterIterator*)iter.clone();
  
  cout << "testText = " << testChars;
  
  if (iter != *test2 ) {
    printf("clone() or equals() failed: Two clones tested unequal\n");
  }
  
  UnicodeString result1, result2;
  // getting and comparing the text within the iterators
  iter.getText(result1);
  test2->getText(result2);
  if (result1 != result2) {
    printf("iter.getText() != clone.getText()\n");
  } 
  
  cout << endl;
  // Demonstrates seeking forward using the iterator.
  cout << "Forward  = ";
  
  UChar c = iter.first();
  printUChar(c);  // The first char
  int32_t i = 0;
  
  if (iter.startIndex() != 0 || iter.endIndex() != u_strlen(testText)) {
    printf("startIndex() or endIndex() failed\n");
  }
  
  
  // Testing forward iteration...
  do {
    if (c == CharacterIterator::DONE && i != u_strlen(testText)) {
      printf("Iterator reached end prematurely");
    }
    else if (c != testText[i]) {
      printf("Character mismatch at position %d\n" + i);
    }
    if (iter.current() != c) {
      printf("current() isn't working right");
    }
    if (iter.getIndex() != i) {
            printf("getIndex() isn't working right\n");
    }
    if (c != CharacterIterator::DONE) {
      c = iter.next();
      i++;
    }
    
    cout << "|";
    printUChar(c);
        
  } while (c != CharacterIterator::DONE);    
  
  delete test2;
  cout << endl;
}


void Test::TestStringiter() {
  const char testChars[] = "Now is the time for all good men to come "
    "to the aid of their country.";

  UnicodeString testString(testChars,"");
  const UChar *testText  = testString.getTerminatedBuffer();
  
  StringCharacterIterator iter(testText, u_strlen(testText));
  StringCharacterIterator* test2 = (StringCharacterIterator*)iter.clone();
  
  if (iter != *test2 ) {
    printf("clone() or equals() failed: Two clones tested unequal\n");
  }
  
  UnicodeString result1, result2;
  // getting and comparing the text within the iterators
  iter.getText(result1);
  test2->getText(result2);
  if (result1 != result2) {
    printf("getText() failed\n");
  }

  cout << "Backwards: ";
  UChar c = iter.last();
  printUChar(c);
  int32_t i = iter.endIndex();
  i--; // already printed out the last char 
  if (iter.startIndex() != 0 || iter.endIndex() != u_strlen(testText)) {
    printf("startIndex() or endIndex() failed\n");
  }
  
  // Testing backward iteration over a range...
  do {
    if (c == CharacterIterator::DONE) {
      printf("Iterator reached end prematurely\n");
    }
    else if (c != testText[i]) {
      printf("Character mismatch at position %d\n" + i);
    }
    if (iter.current() != c) {
      printf("current() isn't working right\n");
    }
    if (iter.getIndex() != i) {
      printf("getIndex() isn't working right [%d should be %d]\n", iter.getIndex(), i);
    }
    if (c != CharacterIterator::DONE) {
      c = iter.previous();
      i--;
    }
    cout << "|";
    printUChar(c);
  } while (c != CharacterIterator::DONE);

  cout << endl;
  delete test2;
}

/* Creating and using text boundaries */
int main( void )
{
  cout << "ICU Iterator Sample Program (C++)\n\n";
  
  Test t;
  
  cout << endl;
  cout << "Test::TestUCharIter()" << endl;
  t.TestUChariter();
  cout << "-----" << endl;
  cout << "Test::TestStringchariter()" << endl;
  t.TestStringiter();
  cout << "-----" << endl;
  
  return 0;
}
