/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef _TSTNORM
#include "tstnorm.h"
#endif


#define ARRAY_LENGTH(array) (sizeof (array) / sizeof (*array))

#define CASE(id,test) case id:                          \
                          name = #test;                 \
                          if (exec) {                   \
                              logln(#test "---");       \
                              logln((UnicodeString)""); \
                              test();                   \
                          }                             \
                          break

static UErrorCode status = U_ZERO_ERROR;

void BasicNormalizerTest::runIndexedTest(int32_t index, UBool exec,
                                         char* &name, char* par) {
    switch (index) {
        CASE(0,TestDecomp);
        CASE(1,TestCompatDecomp);
        CASE(2,TestCanonCompose);
        CASE(3,TestCompatCompose);
        CASE(4,TestPrevious);
        CASE(5,TestHangulDecomp);
        CASE(6,TestHangulCompose);
        CASE(7,TestTibetan);
        CASE(8,TestCompositionExclusion);
        default: name = ""; break;
    }
}

/**
 * Convert Java-style strings with \u Unicode escapes into UnicodeString objects
 */
static UnicodeString str(const char *input)
{
    UnicodeString str(input, ""); // Invariant conversion
    return str.unescape();
}


UnicodeString BasicNormalizerTest::canonTests[24][3];

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
  
    canonTests[14][0] = str("\\u00C4ffin");  canonTests[14][1] = str("A\\u0308ffin");  canonTests[14][2] = str("\\u00C4ffin");

    canonTests[15][0] = str("\\u00C4\\uFB03n"); canonTests[15][1] = str("A\\u0308\\uFB03n"); canonTests[15][2] = str("\\u00C4\\uFB03n");
  
    canonTests[16][0] = str("Henry IV"); canonTests[16][1] = str("Henry IV"); canonTests[16][2] = str("Henry IV");

    canonTests[17][0] = str("Henry \\u2163");  canonTests[17][1] = str("Henry \\u2163");  canonTests[17][2] = str("Henry \\u2163");
  
    canonTests[18][0] = str("\\u30AC");  canonTests[18][1] = str("\\u30AB\\u3099");  canonTests[18][2] = str("\\u30AC");              // ga (Katakana)

    canonTests[19][0] = str("\\u30AB\\u3099"); canonTests[19][1] = str("\\u30AB\\u3099");  canonTests[19][2] = str("\\u30AC");            // ka + ten

    canonTests[20][0] = str("\\uFF76\\uFF9E"); canonTests[20][1] = str("\\uFF76\\uFF9E");  canonTests[20][2] = str("\\uFF76\\uFF9E");       // hw_ka + hw_ten

    canonTests[21][0] = str("\\u30AB\\uFF9E"); canonTests[21][1] = str("\\u30AB\\uFF9E");  canonTests[21][2] = str("\\u30AB\\uFF9E");         // ka + hw_ten

    canonTests[22][0] = str("\\uFF76\\u3099"); canonTests[22][1] = str("\\uFF76\\u3099");  canonTests[22][2] = str("\\uFF76\\u3099");         // hw_ka + ten

    canonTests[23][0] = str("A\\u0300\\u0316"); canonTests[23][1] = str("A\\u0316\\u0300");  canonTests[23][2] = str("\\u00C0\\u0316");     

    /* compatTest */
  // Input                        Decomposed                        Composed
  compatTests[0][0] = str("cat"); compatTests[0][1] = str("cat"); compatTests[0][2] = str("cat") ;
  
  compatTests[1][0] = str("\\uFB4f");  compatTests[1][1] = str("\\u05D0\\u05DC"); compatTests[1][2] = str("\\u05D0\\u05DC");  // Alef-Lamed vs. Alef, Lamed
  
  compatTests[2][0] = str("\\u00C4ffin"); compatTests[2][1] = str("A\\u0308ffin"); compatTests[2][2] = str("\\u00C4ffin") ;

  compatTests[3][0] = str("\\u00C4\\uFB03n"); compatTests[3][1] = str("A\\u0308ffin"); compatTests[3][2] = str("\\u00C4ffin") ; // ffi ligature -> f + f + i
  
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
  // THIS IS NO LONGER TRUE IN UNICODE v2.1.9, SO THIS TEST IS OBSOLETE
//-obsolete-  hangulCompat[0][0] = str("\\ud4db"); hangulCompat[0][1] = str("\\u1111\\u116e\\u1175\\u11af\\u11c2"); hangulCompat[0][2] = str("\\ud478\\u1175\\u11af\\u11c2");
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

  delete norm;
}

void BasicNormalizerTest::TestDecomp() 
{
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP, 0);
  iterateTest(norm, canonTests, ARRAY_LENGTH(canonTests), 1);
  
  staticTest(Normalizer::DECOMP, 0, canonTests, ARRAY_LENGTH(canonTests), 1);
  delete norm;
}

void BasicNormalizerTest::TestCompatDecomp() 
{
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP_COMPAT, 0);
  iterateTest(norm, compatTests, ARRAY_LENGTH(compatTests), 1);
  
  staticTest(Normalizer::DECOMP_COMPAT, 0, 
         compatTests, ARRAY_LENGTH(compatTests), 1);
  delete norm;
}

void BasicNormalizerTest::TestCanonCompose() 
{
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE, 0);
  iterateTest(norm, canonTests, ARRAY_LENGTH(canonTests), 2);
  
  staticTest(Normalizer::COMPOSE, 0, canonTests,
         ARRAY_LENGTH(canonTests), 2);
  delete norm;
}

void BasicNormalizerTest::TestCompatCompose() 
{
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE_COMPAT, 0);
  iterateTest(norm, compatTests, ARRAY_LENGTH(compatTests), 2);
  
  staticTest(Normalizer::COMPOSE_COMPAT, 0, 
         compatTests, ARRAY_LENGTH(compatTests), 2);
  delete norm;
}


//-------------------------------------------------------------------------------

UnicodeString BasicNormalizerTest::hangulCanon[2][3];

void BasicNormalizerTest::TestHangulCompose() 
{
  // Make sure that the static composition methods work
  logln("Canonical composition...");
  staticTest(Normalizer::COMPOSE, 0,                    hangulCanon,  ARRAY_LENGTH(hangulCanon),  2);
  logln("Compatibility composition...");
  
  // Now try iterative composition....
  logln("Static composition...");
  Normalizer* norm = new Normalizer("", Normalizer::COMPOSE, 0);
  iterateTest(norm, hangulCanon, ARRAY_LENGTH(hangulCanon), 2);
  norm->setMode(Normalizer::COMPOSE_COMPAT);
  
  // And finally, make sure you can do it in reverse too
  logln("Reverse iteration...");
  norm->setMode(Normalizer::COMPOSE);
  for (int i = 0; i < ARRAY_LENGTH(hangulCanon); i++) {
    backAndForth(norm, hangulCanon[i][0]);
  }
  delete norm;
}

void BasicNormalizerTest::TestHangulDecomp() 
{
  // Make sure that the static decomposition methods work
  logln("Canonical decomposition...");
  staticTest(Normalizer::DECOMP, 0,                     hangulCanon,  ARRAY_LENGTH(hangulCanon),  1);
  logln("Compatibility decomposition...");
  
  // Now the iterative decomposition methods...
  logln("Iterative decomposition...");
  Normalizer* norm = new Normalizer("", Normalizer::DECOMP, 0);
  iterateTest(norm, hangulCanon, ARRAY_LENGTH(hangulCanon), 1);
  norm->setMode(Normalizer::DECOMP_COMPAT);
  
  // And finally, make sure you can do it in reverse too
  logln("Reverse iteration...");
  norm->setMode(Normalizer::DECOMP);
  for (int i = 0; i < ARRAY_LENGTH(hangulCanon); i++) {
    backAndForth(norm, hangulCanon[i][0]);
  }
  delete norm;
}

/**
 * The Tibetan vowel sign AA, 0f71, was messed up prior to Unicode version 2.1.9.
 * Once 2.1.9 or 3.0 is released, uncomment this test.
 */
void BasicNormalizerTest::TestTibetan(void) {
    UnicodeString decomp[1][3];
    decomp[0][0] = str("\\u0f77");
    decomp[0][1] = str("\\u0f77");
    decomp[0][2] = str("\\u0fb2\\u0f71\\u0f80");

    UnicodeString compose[1][3];
    compose[0][0] = str("\\u0fb2\\u0f71\\u0f80");
    compose[0][1] = str("\\u0fb2\\u0f71\\u0f80");
    compose[0][2] = str("\\u0fb2\\u0f71\\u0f80");

    staticTest(Normalizer::DECOMP,         0, decomp, ARRAY_LENGTH(decomp), 1);
    staticTest(Normalizer::DECOMP_COMPAT,  0, decomp, ARRAY_LENGTH(decomp), 2);
    staticTest(Normalizer::COMPOSE,        0, compose, ARRAY_LENGTH(compose), 1);
    staticTest(Normalizer::COMPOSE_COMPAT, 0, compose, ARRAY_LENGTH(compose), 2);
}

/**
 * Make sure characters in the CompositionExclusion.txt list do not get
 * composed to.
 */
void BasicNormalizerTest::TestCompositionExclusion(void) {
    // This list is generated from CompositionExclusion.txt.
    // Update whenever the normalizer tables are updated.  Note
    // that we test all characters listed, even those that can be
    // derived from the Unicode DB and are therefore commented
    // out.
    UnicodeString EXCLUDED = str(
        "\\u0340\\u0341\\u0343\\u0344\\u0374\\u037E\\u0387\\u0958"
        "\\u0959\\u095A\\u095B\\u095C\\u095D\\u095E\\u095F\\u09DC"
        "\\u09DD\\u09DF\\u0A33\\u0A36\\u0A59\\u0A5A\\u0A5B\\u0A5E"
        "\\u0B5C\\u0B5D\\u0F43\\u0F4D\\u0F52\\u0F57\\u0F5C\\u0F69"
        "\\u0F73\\u0F75\\u0F76\\u0F78\\u0F81\\u0F93\\u0F9D\\u0FA2"
        "\\u0FA7\\u0FAC\\u0FB9\\u1F71\\u1F73\\u1F75\\u1F77\\u1F79"
        "\\u1F7B\\u1F7D\\u1FBB\\u1FBE\\u1FC9\\u1FCB\\u1FD3\\u1FDB"
        "\\u1FE3\\u1FEB\\u1FEE\\u1FEF\\u1FF9\\u1FFB\\u1FFD\\u2000"
        "\\u2001\\u2126\\u212A\\u212B\\u2329\\u232A\\uF900\\uFA10"
        "\\uFA12\\uFA15\\uFA20\\uFA22\\uFA25\\uFA26\\uFA2A\\uFB1F"
        "\\uFB2A\\uFB2B\\uFB2C\\uFB2D\\uFB2E\\uFB2F\\uFB30\\uFB31"
        "\\uFB32\\uFB33\\uFB34\\uFB35\\uFB36\\uFB38\\uFB39\\uFB3A"
        "\\uFB3B\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46"
        "\\uFB47\\uFB48\\uFB49\\uFB4A\\uFB4B\\uFB4C\\uFB4D\\uFB4E"
        );
    for (int32_t i=0; i<EXCLUDED.length(); ++i) {
        UnicodeString a(EXCLUDED.charAt(i));
        UnicodeString b;
        UnicodeString c;
        Normalizer::normalize(a, Normalizer::DECOMP_COMPAT, 0, b, status);
        Normalizer::normalize(b, Normalizer::COMPOSE, 0, c, status);
        if (c == a) {
            errln("FAIL: " + hex(a) + " x DECOMP_COMPAT => " +
                  hex(b) + " x COMPOSE => " +
                  hex(c));
        } else if (verbose) {
            logln("Ok: " + hex(a) + " x DECOMP_COMPAT => " +
                  hex(b) + " x COMPOSE => " +
                  hex(c));                
        }
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
        if (i != 0) result += (UChar)0x2c/*,*/;
        appendHex(s[i], 4, result);
    }
    return result;
}


inline static void insert(UnicodeString& dest, int pos, UChar32 ch)
{
    dest.replace(pos, 0, ch);
}

void BasicNormalizerTest::backAndForth(Normalizer* iter, const UnicodeString& input)
{
    UChar32 ch;
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

    for (UChar32 ch = iter->first(); ch != iter->DONE; ch = iter->next()) {
        result += ch;
    }
    if (result != expected) {
        errln(errPrefix + "normalized " + hex(input) + "\n"
            + "                expected " + hex(expected) + "\n"
            + "             iterate got " + hex(result) );
    }
}
