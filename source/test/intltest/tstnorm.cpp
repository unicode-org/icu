/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _TSTNORM
#include "tstnorm.h"
#endif


#define ARRAY_LENGTH(array) (sizeof (array) / sizeof (*array))

static UErrorCode status = U_ZERO_ERROR;

/**
 * Convert Java-style strings with \u Unicode escapes into UnicodeString objects
 */
static UnicodeString str(const char *input)
{
  static const UnicodeString digitString1("0123456789ABCDEF");
  static const UnicodeString digitString2("0123456789abcdef");
  
  UnicodeString result(input);
  int index = 0;
  
  while ((index = result.indexOf("\\u")) != -1)
    {
      if (index + 6 <= result.length())
    {
      UChar c = 0;
      for (int i = index + 2; i < index + 6; i++) {
        UTextOffset value = digitString1.indexOf(result[i]);
        
        if (value == -1) {
          value = digitString2.indexOf(result[i]);
        }
        c = (UChar)(c * 16 + value);
      }
      UnicodeString replace;
      replace += c;
      result.replace(index, 6, replace);
    }
      index += 1;
    }

  return result;
}


UnicodeString BasicNormalizerTest::canonTests[23][3];

UnicodeString BasicNormalizerTest::compatTests[11][3];

BasicNormalizerTest::BasicNormalizerTest()
{
  // canonTest
  // Input                    Decomposed                    Composed

    canonTests[0][0] = str("cat");  canonTests[0][1] = str("cat"); canonTests[0][2] =  str("cat");

    canonTests[1][0] = str("\\u00e0ardvark");    canonTests[1][1] = str("a\\u0300ardvark");  canonTests[1][2] = str("\\u00e0ardvark"); 

    canonTests[2][0] = str("\\u1e0a"); canonTests[2][1] = str("D\\u0307"); canonTests[2][2] = str("\\u1e0a");                 // D-dot_above

    canonTests[3][0] = str("D\\u0307");  canonTests[3][1] = str("D\\u0307"); canonTests[3][2] = str("\\u1e0a");            // D dot_above

    canonTests[4][0] = str("\\u1e0c\\u0307"); canonTests[4][1] = str("D\\u0323\\u0307");  canonTests[4][2] = str("\\u1e0c\\u0307");         // D-dot_below dot_above

    canonTests[5][0] = str("\\u1e0a\\u0323"); canonTests[5][1] = str("D\\u0323\\u0307");  canonTests[5][2] = str("\\u1e0c\\u0307");        // D-dot_above dot_below 

    canonTests[6][0] = str("D\\u0307\\u0323"); canonTests[6][1] = str("D\\u0323\\u0307");  canonTests[6][2] = str("\\u1e0c\\u0307");         // D dot_below dot_above 

    canonTests[7][0] = str("\\u1e10\\u0307\\u0323");  canonTests[7][1] = str("D\\u0327\\u0323\\u0307"); canonTests[7][2] = str("\\u1e10\\u0323\\u0307");     // D dot_below cedilla dot_above

    canonTests[8][0] = str("D\\u0307\\u0328\\u0323"); canonTests[8][1] = str("D\\u0328\\u0323\\u0307"); canonTests[8][2] = str("\\u1e0c\\u0328\\u0307");     // D dot_above ogonek dot_below

    canonTests[9][0] = str("\\u1E14"); canonTests[9][1] = str("E\\u0304\\u0300"); canonTests[9][2] = str("\\u1E14");         // E-macron-grave

    canonTests[10][0] = str("\\u0112\\u0300"); canonTests[10][1] = str("E\\u0304\\u0300");  canonTests[10][2] = str("\\u1E14");            // E-macron + grave

    canonTests[11][0] = str("\\u00c8\\u0304"); canonTests[11][1] = str("E\\u0300\\u0304");  canonTests[11][2] = str("\\u00c8\\u0304");         // E-grave + macron
  
    canonTests[12][0] = str("\\u212b"); canonTests[12][1] = str("A\\u030a"); canonTests[12][2] = str("\\u00c5");             // angstrom_sign

    canonTests[13][0] = str("\\u00c5");      canonTests[13][1] = str("A\\u030a");  canonTests[13][2] = str("\\u00c5");            // A-ring
  
    canonTests[14][0] = str("Äffin");  canonTests[14][1] = str("A\\u0308ffin");  canonTests[14][2] = str("Äffin");

    canonTests[15][0] = str("Ä\\uFB03n"); canonTests[15][1] = str("A\\u0308\\uFB03n"); canonTests[15][2] = str("Ä\\uFB03n");
  
    canonTests[16][0] = str("Henry IV"); canonTests[16][1] = str("Henry IV"); canonTests[16][2] = str("Henry IV");

    canonTests[17][0] = str("Henry \\u2163");  canonTests[17][1] = str("Henry \\u2163");  canonTests[17][2] = str("Henry \\u2163");
  
    canonTests[18][0] = str("\\u30AC");  canonTests[18][1] = str("\\u30AB\\u3099");  canonTests[18][2] = str("\\u30AC");              // ga (Katakana)

    canonTests[19][0] = str("\\u30AB\\u3099"); canonTests[19][1] = str("\\u30AB\\u3099");  canonTests[19][2] = str("\\u30AC");            // ka + ten

    canonTests[20][0] = str("\\uFF76\\uFF9E"); canonTests[20][1] = str("\\uFF76\\uFF9E");  canonTests[20][2] = str("\\uFF76\\uFF9E");       // hw_ka + hw_ten

    canonTests[21][0] = str("\\u30AB\\uFF9E"); canonTests[21][1] = str("\\u30AB\\uFF9E");  canonTests[21][2] = str("\\u30AB\\uFF9E");         // ka + hw_ten

    canonTests[22][0] = str("\\uFF76\\u3099"); canonTests[22][1] = str("\\uFF76\\u3099");  canonTests[22][2] = str("\\uFF76\\u3099");         // hw_ka + ten

    /* compatTest */
  // Input                        Decomposed                        Composed
  compatTests[0][0] = str("cat"); compatTests[0][1] = str("cat"); compatTests[0][2] = str("cat") ;
  
  compatTests[1][0] = str("\\uFB4f");  compatTests[1][1] = str("\\u05D0\\u05DC"); compatTests[1][2] = str("\\u05D0\\u05DC");  // Alef-Lamed vs. Alef, Lamed
  
  compatTests[2][0] = str("Äffin"); compatTests[2][1] = str("A\\u0308ffin"); compatTests[2][2] = str("Äffin") ;

  compatTests[3][0] = str("Ä\\uFB03n"); compatTests[3][1] = str("A\\u0308ffin"); compatTests[3][2] = str("Äffin") ; // ffi ligature -> f + f + i
  
  compatTests[4][0] = str("Henry IV"); compatTests[4][1] = str("Henry IV"); compatTests[4][2] = str("Henry IV") ;

  compatTests[5][0] = str("Henry \\u2163"); compatTests[5][1] = str("Henry IV");  compatTests[5][2] = str("Henry IV") ;
  
  compatTests[6][0] = str("\\u30AC"); compatTests[6][1] = str("\\u30AB\\u3099"); compatTests[6][2] = str("\\u30AC") ; // ga (Katakana)

  compatTests[7][0] = str("\\u30AB\\u3099"); compatTests[7][1] = str("\\u30AB\\u3099"); compatTests[7][2] = str("\\u30AC") ; // ka + ten
  
  compatTests[8][0] = str("\\uFF76\\u3099"); compatTests[8][1] = str("\\u30AB\\u3099"); compatTests[8][2] = str("\\u30AC") ; // hw_ka + ten
  
  /* These two are broken in Unicode 2.1.2 but fixed in 2.1.5 and later */
  compatTests[9][0] = str("\\uFF76\\uFF9E"); compatTests[9][1] = str("\\u30AB\\u3099"); compatTests[9][2] = str("\\u30AC") ; // hw_ka + hw_ten

  compatTests[10][0] = str("\\u30AB\\uFF9E"); compatTests[10][1] = str("\\u30AB\\u3099"); compatTests[10][2] = str("\\u30AC") ; // ka + hw_ten

  /* Hangul Canonical */
  // Input                        Decomposed                        Composed
  hangulCanon[0][0] = str("\\ud4db"); hangulCanon[0][1] = str("\\u1111\\u1171\\u11b6"); hangulCanon[0][2] = str("\\ud4db") ;

  hangulCanon[1][0] = str("\\u1111\\u1171\\u11b6"), hangulCanon[1][1] = str("\\u1111\\u1171\\u11b6"),   hangulCanon[1][2] = str("\\ud4db");

  /* Hangul Compatible */
  // Input            Decomposed                                    Composed
  hangulCompat[0][0] = str("\\ud4db"); hangulCompat[0][1] = str("\\u1111\\u116e\\u1175\\u11af\\u11c2"); hangulCompat[0][2] = str("\\ud478\\u1175\\u11af\\u11c2");
}

BasicNormalizerTest::~BasicNormalizerTest()
{
}

void BasicNormalizerTest::TestPrevious() 
{
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP, 0);
  
  logln("testing decomp...");
  int i;
  for (i = 0; i < ARRAY_LENGTH(canonTests); i++) {
    backAndForth(norm, canonTests[i][0]);
  }
  
  logln("testing compose...");
  norm->setMode(Normalizer::COMPOSE);
  for (i = 0; i < ARRAY_LENGTH(canonTests); i++) {
    backAndForth(norm, canonTests[i][0]);
  }
}

void BasicNormalizerTest::TestDecomp() 
{
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP, 0);
  iterateTest(norm, canonTests, ARRAY_LENGTH(canonTests), 1);
  
  staticTest(Normalizer::DECOMP, 0, canonTests, ARRAY_LENGTH(canonTests), 1);
}

void BasicNormalizerTest::TestCompatDecomp() 
{
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP_COMPAT, 0);
  iterateTest(norm, compatTests, ARRAY_LENGTH(compatTests), 1);
  
  staticTest(Normalizer::DECOMP_COMPAT, 0, 
         compatTests, ARRAY_LENGTH(compatTests), 1);
}

void BasicNormalizerTest::TestCanonCompose() 
{
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE, 0);
  iterateTest(norm, canonTests, ARRAY_LENGTH(canonTests), 2);
  
  staticTest(Normalizer::COMPOSE, 0, canonTests,
         ARRAY_LENGTH(canonTests), 2);
}

void BasicNormalizerTest::TestCompatCompose() 
{
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE_COMPAT, 0);
  iterateTest(norm, compatTests, ARRAY_LENGTH(compatTests), 2);
  
  staticTest(Normalizer::COMPOSE_COMPAT, 0, 
         compatTests, ARRAY_LENGTH(compatTests), 2);
}


//-------------------------------------------------------------------------------

UnicodeString BasicNormalizerTest::hangulCanon[2][3];

UnicodeString BasicNormalizerTest::hangulCompat[1][3];

void BasicNormalizerTest::TestHangulCompose() 
{
  // Make sure that the static composition methods work
  logln("Canonical composition...");
  staticTest(Normalizer::COMPOSE, 0,                    hangulCanon,  ARRAY_LENGTH(hangulCanon),  2);
  logln("Compatibility composition...");
  staticTest(Normalizer::COMPOSE_COMPAT, 0,         hangulCompat, ARRAY_LENGTH(hangulCompat), 2);
  
  // Now try iterative composition....
  logln("Static composition...");
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE, 0);
  iterateTest(norm, hangulCanon, ARRAY_LENGTH(hangulCanon), 2);
  norm->setMode(Normalizer::COMPOSE_COMPAT);
  iterateTest(norm, hangulCompat, ARRAY_LENGTH(hangulCompat), 2);
  
  // And finally, make sure you can do it in reverse too
  logln("Reverse iteration...");
  norm->setMode(Normalizer::COMPOSE);
  for (int i = 0; i < ARRAY_LENGTH(hangulCanon); i++) {
    backAndForth(norm, hangulCanon[i][0]);
  }
}

void BasicNormalizerTest::TestHangulDecomp() 
{
  // Make sure that the static decomposition methods work
  logln("Canonical decomposition...");
  staticTest(Normalizer::DECOMP, 0,                     hangulCanon,  ARRAY_LENGTH(hangulCanon),  1);
  logln("Compatibility decomposition...");
  staticTest(Normalizer::DECOMP_COMPAT, 0,         hangulCompat, ARRAY_LENGTH(hangulCompat), 1);
  
  // Now the iterative decomposition methods...
  logln("Iterative decomposition...");
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP, 0);
  iterateTest(norm, hangulCanon, ARRAY_LENGTH(hangulCanon), 1);
  norm->setMode(Normalizer::DECOMP_COMPAT);
  iterateTest(norm, hangulCompat, ARRAY_LENGTH(hangulCompat), 1);
  
  // And finally, make sure you can do it in reverse too
  logln("Reverse iteration...");
  norm->setMode(Normalizer::DECOMP);
  for (int i = 0; i < ARRAY_LENGTH(hangulCanon); i++) {
    backAndForth(norm, hangulCanon[i][0]);
  }
}


//------------------------------------------------------------------------
// Internal utilities
//

UnicodeString BasicNormalizerTest::hex(UChar ch) {
    UnicodeString result;
    return appendHex(ch, 4, result);
}

UnicodeString BasicNormalizerTest::hex(const UnicodeString& s) {
    UnicodeString result;
    for (int i = 0; i < s.length(); ++i) {
        if (i != 0) result += ',';
        appendHex(s[i], 4, result);
    }
    return result;
}


inline static void insert(UnicodeString& dest, int pos, UChar ch)
{
    dest.replace(pos, 0, &ch, 1);
}

void BasicNormalizerTest::backAndForth(Normalizer* iter, const UnicodeString& input)
{
    UChar ch;
    iter->setText(input, status);

    // Run through the iterator forwards and stick it into a StringBuffer
    UnicodeString forward;
    for (ch = iter->first(); ch != iter->DONE; ch = iter->next()) {
        forward += ch;
    }

    // Now do it backwards
    UnicodeString reverse;
    for (ch = iter->last(); ch != iter->DONE; ch = iter->previous()) {
        insert(reverse, 0, ch);
    }
    
    if (forward != reverse) {
        errln("Forward/reverse mismatch for input " + hex(input)
              + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
    }
}

void BasicNormalizerTest::staticTest(Normalizer::EMode mode, int options,
                     UnicodeString tests[][3], int length,
                     int outCol)
{
    for (int i = 0; i < length; i++)
    {
        UnicodeString& input = tests[i][0];
        UnicodeString& expect = tests[i][outCol];
        
        logln("Normalizing '" + input + "' (" + hex(input) + ")" );
        
        UnicodeString output;
        Normalizer::normalize(input, mode, options, output, status);
        
        if (output != expect) {
            errln(UnicodeString("ERROR: case ") + i + " normalized " + hex(input) + "\n"
                + "                expected " + hex(expect) + "\n"
                + "              static got " + hex(output) );
        }
    }
}

void BasicNormalizerTest::iterateTest(Normalizer* iter,
                                      UnicodeString tests[][3], int length,
                                      int outCol)
{
    for (int i = 0; i < length; i++)
    {
        UnicodeString& input = tests[i][0];
        UnicodeString& expect = tests[i][outCol];
        
        logln("Normalizing '" + input + "' (" + hex(input) + ")" );
        
        iter->setText(input, status);
        assertEqual(input, expect, iter, UnicodeString("ERROR: case ") + i + " ");
    }
}

void BasicNormalizerTest::assertEqual(const UnicodeString&    input,
                      const UnicodeString&    expected,
                      Normalizer*        iter,
                      const UnicodeString&    errPrefix)
{
    int index = 0;
    UnicodeString result;

    for (UChar ch = iter->first(); ch != iter->DONE; ch = iter->next()) {
        result += ch;
    }
    if (result != expected) {
        errln(errPrefix + "normalized " + hex(input) + "\n"
            + "                expected " + hex(expected) + "\n"
            + "             iterate got " + hex(result) );
    }
}

void BasicNormalizerTest::runIndexedTest(int32_t index, bool_t exec, char* &name, char* par)
{
    if (exec)
    {
        logln("Collation Regression Tests: ");
    }

    switch (index)
    {
        case  0: name = "TestDecomp";            if (exec) TestDecomp(); break;
        case  1: name = "TestCompatDecomp";        if (exec) TestCompatDecomp(); break;
        case  2: name = "TestCanonCompose";        if (exec) TestCanonCompose(); break;
        case  3: name = "TestCompatCompose";    if (exec) TestCompatCompose(); break;
        case  4: name = "TestPrevious";            if (exec) TestPrevious(); break;
        case  5: name = "TestHangulDecomp";        if (exec) TestHangulDecomp(); break;
        case  6: name = "TestHangulCompose";    if (exec) TestHangulCompose(); break;
        default: name = ""; break;
    }
}
