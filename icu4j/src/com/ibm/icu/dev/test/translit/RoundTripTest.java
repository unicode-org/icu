package com.ibm.test.translit;
import com.ibm.test.*;
import com.ibm.text.*;
import com.ibm.util.Utility;
import java.io.*;
import java.util.BitSet;
import java.text.ParseException;

/**
 * @test
 * @summary Round trip test of Transliterator
 */
public class RoundTripTest extends TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new RoundTripTest().run(args);
    }
    /*
    public void TestSingle() throws IOException, ParseException {
        Transliterator t = Transliterator.getInstance("Latin-Greek");
        String s = t.transliterate("\u0101\u0069");
    }
    */
    
    public void TestHiragana() throws IOException, ParseException {
        new Test("Latin-Hiragana", 
          TestUtility.LATIN_SCRIPT, TestUtility.HIRAGANA_SCRIPT)
          .test("[a-z]", "[\u3040-\u3094]", null, this, new Legal());
    }

    public void TestKatakana() throws IOException, ParseException {
        new Test("Latin-Katakana", 
          TestUtility.LATIN_SCRIPT, TestUtility.KATAKANA_SCRIPT)
          .test("[a-z]", "[\u30A1-\u30FA\u30FC]", null, this, new Legal());
    }

// Some transliterators removed for 2.0

//  public void TestArabic() throws IOException, ParseException {
//      new Test("Latin-Arabic", 
//        TestUtility.LATIN_SCRIPT, TestUtility.ARABIC_SCRIPT)
//        .test("[a-z]", "[\u0620-\u065F-[\u0640]]", this);
//  }

//  public void TestHebrew() throws IOException, ParseException {
//      new Test("Latin-Hebrew", 
//        TestUtility.LATIN_SCRIPT, TestUtility.HEBREW_SCRIPT)
//        .test(null, "[\u05D0-\u05EF]", this);
//  }

//  public void TestHangul() throws IOException, ParseException {
//      Test t = new TestHangul();
//      t.setPairLimit(30); // Don't run full test -- too long
//      t.test(null, null, this);
//  }

    public void TestJamo() throws IOException, ParseException {
        Test t = new Test("Latin-Jamo", 
          TestUtility.LATIN_SCRIPT, TestUtility.JAMO_SCRIPT);
        t.setErrorLimit(200); // Don't run full test -- too long
        //t.test("[[a-z]-[fqvxz]]", null, this);
        t.test("[a-z]", null, null, this, new Legal());
    }

    public void TestJamoHangul() throws IOException, ParseException {
        Test t = new Test("Latin-Hangul", 
          TestUtility.LATIN_SCRIPT, TestUtility.HANGUL_SCRIPT);
        t.setErrorLimit(50); // Don't run full test -- too long
        t.test("[a-z]", null, null, this, new Legal());
    }

    public void TestGreek() throws IOException, ParseException {
        try {
            Legal lt = new LegalGreek(true);
            new Test("Latin-Greek", 
            TestUtility.LATIN_SCRIPT, TestUtility.GREEK_SCRIPT)
            .test(null, "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
                "[\u037A\u03D0-\u03F5]", /* exclusions */
                this, lt);
        } catch (RuntimeException e) {
            System.out.println(e.getClass().getName() + ", " + e.getMessage());
            throw e;
        }
    }

    public void Testel() throws IOException, ParseException {
        new Test("Latin-el", 
          TestUtility.LATIN_SCRIPT, TestUtility.GREEK_SCRIPT)
          .test(null, "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
            "[\u037A\u03D0-\u03F5]", /* exclusions */
            this, new LegalGreek(false));
    }

    public void TestCyrillic() throws IOException, ParseException {
        new Test("Latin-Cyrillic", 
          TestUtility.LATIN_SCRIPT, TestUtility.CYRILLIC_SCRIPT)
          .test(null, "[\u0400-\u045F]", null, this, new Legal());
    }
    
    //----------------------------------
    // Inter-Indic Tests
    //----------------------------------
   public void TestDevanagariLatin() throws IOException, ParseException {
        new Test("Latin-DEVANAGARI", 
          TestUtility.LATIN_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test(null, "[:Devanagari:]", null, this, new Legal());
    }
    public void TestDevanagariBengali() throws IOException, ParseException {
        new Test("BENGALI-DEVANAGARI", 
          TestUtility.BENGALI_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:BENGALI:]", "[:Devanagari:]", 
                "[\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-BENGALI", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.BENGALI_SCRIPT )
          .test("[:Devanagari:]", "[:BENGALI:]",
                "[\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestDevanagariGurmukhi() throws IOException, ParseException {
        new Test("GURMUKHI-DEVANAGARI", 
          TestUtility.GURMUKHI_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:GURMUKHI:]", "[:Devanagari:]",  
                "[\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-GURMUKHI", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.GURMUKHI_SCRIPT )
          .test( "[:Devanagari:]", "[:GURMUKHI:]",
                  "[\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                  this, new Legal());
    } 
    public void TestDevanagariGujarati() throws IOException, ParseException {
        new Test("GUJARATI-DEVANAGARI", 
          TestUtility.GUJARATI_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:GUJARATI:]", "[:Devanagari:]", 
                "[\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-GUJARATI", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.GUJARATI_SCRIPT )
          .test( "[:Devanagari:]", "[:GUJARATI:]",
                  "[\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariOriya() throws IOException, ParseException {
        new Test("ORIYA-DEVANAGARI", 
          TestUtility.ORIYA_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:ORIYA:]", "[:Devanagari:]", 
                "[\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-ORIYA", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.ORIYA_SCRIPT )
          .test( "[:Devanagari:]", "[:ORIYA:]",
                  "[\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariTamil() throws IOException, ParseException {
        new Test("Tamil-DEVANAGARI", 
          TestUtility.TAMIL_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:tamil:]", "[:Devanagari:]", 
                  "[\u090B\u090C\u090D\u0911\u0916\u0917\u0918\u091B\u091D\u0920\u0921\u0922\u0925\u0926\u0927\u092B\u092C\u092D\u0936\u093d\u0950[\u0958-\u0961]]", /*roundtrip exclusions*/
                  this, new Legal());
        new Test("DEVANAGARI-Tamil", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.TAMIL_SCRIPT )
          .test( "[:Devanagari:]", "[:tamil:]",
                  "", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariTelugu() throws IOException, ParseException {
        new Test("Telugu-DEVANAGARI", 
          TestUtility.TELUGU_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:telugu:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-TELUGU", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:Devanagari:]", "[:TELUGU:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestDevanagariKannada() throws IOException, ParseException {
        new Test("KANNADA-DEVANAGARI", 
          TestUtility.KANNADA_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:KANNADA:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-KANNADA", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:Devanagari:]", "[:KANNADA:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/ 
                  this, new Legal());
    }
    public void TestDevanagariMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-DEVANAGARI", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.DEVANAGARI_SCRIPT)
          .test("[:MALAYALAM:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-MALAYALAM", 
            TestUtility.DEVANAGARI_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:Devanagari:]", "[:MALAYALAM:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestBengaliGurmukhi() throws IOException, ParseException {
        new Test("GURMUKHI-BENGALI", 
          TestUtility.GURMUKHI_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:GURMUKHI:]", "[:BENGALI:]",  
                "[\u098B\u098C\u09B7\u09E0\u09E1\u09F0\u09F1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-GURMUKHI", 
            TestUtility.BENGALI_SCRIPT, TestUtility.GURMUKHI_SCRIPT )
          .test( "[:BENGALI:]", "[:GURMUKHI:]",
                  "[\u0A33\u0A35\u0A59\u0A5A\u0A5B\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                  this, new Legal());
    } 
    public void TestBengaliGujarati() throws IOException, ParseException {
        new Test("GUJARATI-BENGALI", 
          TestUtility.GUJARATI_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:GUJARATI:]", "[:BENGALI:]", 
                "[\u098c\u09e1\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-GUJARATI", 
            TestUtility.BENGALI_SCRIPT, TestUtility.GUJARATI_SCRIPT )
          .test( "[:BENGALI:]", "[:GUJARATI:]",
                  "[\u0A8D\u0A91\u0AB3\u0AB5\u0ABD\u0AD0]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliOriya() throws IOException, ParseException {
        new Test("ORIYA-BENGALI", 
          TestUtility.ORIYA_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:ORIYA:]", "[:BENGALI:]", 
                "[\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-ORIYA", 
            TestUtility.BENGALI_SCRIPT, TestUtility.ORIYA_SCRIPT )
          .test( "[:BENGALI:]", "[:ORIYA:]",
                  "[\u0b33\u0b3d]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliTamil() throws IOException, ParseException {
        new Test("Tamil-BENGALI", 
          TestUtility.TAMIL_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:tamil:]", "[:BENGALI:]", 
                  "[\u09f0\u09f1\u098B\u098C\u0996\u0997\u0998\u099B\u099D\u09A0\u09A1\u09A2\u09A5\u09A6\u09A7\u09AB\u09AC\u09AD\u09B6\u09DC\u09DD\u09DF\u09E0\u09E1]", /*roundtrip exclusions*/
                  this, new Legal());
        new Test("BENGALI-Tamil", 
            TestUtility.BENGALI_SCRIPT, TestUtility.TAMIL_SCRIPT )
          .test( "[:BENGALI:]", "[:tamil:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB3\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliTelugu() throws IOException, ParseException {
        new Test("Telugu-BENGALI", 
          TestUtility.TELUGU_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:telugu:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-TELUGU", 
            TestUtility.BENGALI_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:BENGALI:]", "[:TELUGU:]",
                  "[\u0C0E\u0C12\u0C31\u0C33\u0C35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestBengaliKannada() throws IOException, ParseException {
        new Test("KANNADA-BENGALI", 
          TestUtility.KANNADA_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:KANNADA:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-KANNADA", 
            TestUtility.BENGALI_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:BENGALI:]", "[:KANNADA:]",
                  "[\u0C8E\u0C92\u0CB1\u0cb3\u0cb5\u0cde]", /*roundtrip exclusions*/ 
                  this, new Legal());
    }
    public void TestBengaliMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-BENGALI", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.BENGALI_SCRIPT)
          .test("[:MALAYALAM:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-MALAYALAM", 
            TestUtility.BENGALI_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:BENGALI:]", "[:MALAYALAM:]",
                  "[\u0d31-\u0d35\u0d0e\u0d12]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiGujarati() throws IOException, ParseException {
        new Test("GUJARATI-GURMUKHI", 
          TestUtility.GUJARATI_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:GUJARATI:]", "[:GURMUKHI:]", 
                "[\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0abd]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-GUJARATI", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.GUJARATI_SCRIPT )
          .test( "[:GURMUKHI:]", "[:GUJARATI:]",
                  "[\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0ab7\u0abd\u0ad0\u0ae0]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiOriya() throws IOException, ParseException {
        new Test("ORIYA-GURMUKHI", 
          TestUtility.ORIYA_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:ORIYA:]", "[:GURMUKHI:]", 
                "[\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61\u0a35\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-ORIYA", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.ORIYA_SCRIPT )
          .test( "[:GURMUKHI:]", "[:ORIYA:]",
                  "[\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiTamil() throws IOException, ParseException {
        new Test("TAMIL-GURMUKHI", 
          TestUtility.TAMIL_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:TAMIL:]", "[:GURMUKHI:]", 
                "[\u0A16\u0A17\u0A18\u0A1B\u0A1D\u0A20\u0A21\u0A22\u0A25\u0A26\u0A27\u0A2B\u0A2C\u0A2D\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-TAMIL", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.TAMIL_SCRIPT )
          .test( "[:GURMUKHI:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiTelugu() throws IOException, ParseException {
        new Test("TELUGU-GURMUKHI", 
          TestUtility.TELUGU_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:TELUGU:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-TELUGU", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:GURMUKHI:]", "[:TELUGU:]",
                  "[\u0C0B\u0C0C\u0C0E\u0C12\u0C31\u0C37\u0C60\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiKannada() throws IOException, ParseException {
        new Test("KANNADA-GURMUKHI", 
          TestUtility.KANNADA_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:KANNADA:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-KANNADA", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:GURMUKHI:]", "[:KANNADA:]",
                  "[\u0C8B\u0C8C\u0C8E\u0C92\u0CB1\u0CB7\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-GURMUKHI", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.GURMUKHI_SCRIPT)
          .test("[:MALAYALAM:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-MALAYALAM", 
            TestUtility.GURMUKHI_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:GURMUKHI:]", "[:MALAYALAM:]",
                  "[\u0D0B\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D37\u0D60\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
     public void TestGujaratiOriya() throws IOException, ParseException {
        new Test("GUJARATI-ORIYA", 
          TestUtility.GUJARATI_SCRIPT, TestUtility.ORIYA_SCRIPT)
          .test("[:GUJARATI:]", "[:ORIYA:]", 
                "[\u0B0C\u0B5F\u0B61]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-GUJARATI", 
            TestUtility.ORIYA_SCRIPT, TestUtility.GUJARATI_SCRIPT )
          .test( "[:ORIYA:]", "[:GUJARATI:]",
                  "[\u0A8D\u0A91\u0AB5\u0Ad0]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiTamil() throws IOException, ParseException {
        new Test("TAMIL-GUJARATI", 
          TestUtility.TAMIL_SCRIPT, TestUtility.GUJARATI_SCRIPT)
          .test("[:TAMIL:]", "[:GUJARATI:]", 
                "[\u0A8B\u0A8D\u0A91\u0A96\u0A97\u0A98\u0A9B\u0A9D\u0AA0\u0AA1\u0AA2\u0AA5\u0AA6\u0AA7\u0AAB\u0AAC\u0AAD\u0AB6\u0ABD\u0AD0\u0AE0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-TAMIL", 
            TestUtility.GUJARATI_SCRIPT, TestUtility.TAMIL_SCRIPT )
          .test( "[:GUJARATI:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiTelugu() throws IOException, ParseException {
        new Test("TELUGU-GUJARATI", 
          TestUtility.TELUGU_SCRIPT, TestUtility.GUJARATI_SCRIPT)
          .test("[:TELUGU:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-TELUGU", 
            TestUtility.GUJARATI_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:GUJARATI:]", "[:TELUGU:]",
                  "[\u0C0C\u0C0E\u0C12\u0C31\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiKannada() throws IOException, ParseException {
        new Test("KANNADA-GUJARATI", 
          TestUtility.KANNADA_SCRIPT, TestUtility.GUJARATI_SCRIPT)
          .test("[:KANNADA:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-KANNADA", 
            TestUtility.GUJARATI_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:GUJARATI:]", "[:KANNADA:]",
                  "[\u0C8C\u0C8E\u0C92\u0CB1\u0CDE\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-GUJARATI", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.GUJARATI_SCRIPT)
          .test("[:MALAYALAM:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-MALAYALAM", 
            TestUtility.GUJARATI_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:GUJARATI:]", "[:MALAYALAM:]",
                  "[\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
  public void TestOriyaTamil() throws IOException, ParseException {
        new Test("TAMIL-ORIYA", 
          TestUtility.TAMIL_SCRIPT, TestUtility.ORIYA_SCRIPT)
          .test("[:TAMIL:]", "[:ORIYA:]", 
                "[\u0B0B\u0B0C\u0B16\u0B17\u0B18\u0B1B\u0B1D\u0B20\u0B21\u0B22\u0B25\u0B26\u0B27\u0B2B\u0B2C\u0B2D\u0B36\u0B3D\u0B5C\u0B5D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-TAMIL", 
            TestUtility.ORIYA_SCRIPT, TestUtility.TAMIL_SCRIPT )
          .test( "[:ORIYA:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaTelugu() throws IOException, ParseException {
        new Test("TELUGU-ORIYA", 
          TestUtility.TELUGU_SCRIPT, TestUtility.ORIYA_SCRIPT)
          .test("[:TELUGU:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-TELUGU", 
            TestUtility.ORIYA_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:ORIYA:]", "[:TELUGU:]",
                  "[\u0C0E\u0C12\u0C31\u0C35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaKannada() throws IOException, ParseException {
        new Test("KANNADA-ORIYA", 
          TestUtility.KANNADA_SCRIPT, TestUtility.ORIYA_SCRIPT)
          .test("[:KANNADA:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-KANNADA", 
            TestUtility.ORIYA_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:ORIYA:]", "[:KANNADA:]",
                  "[\u0C8E\u0C92\u0CB1\u0CB5\u0CDE]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-ORIYA", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.ORIYA_SCRIPT)
          .test("[:MALAYALAM:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-MALAYALAM", 
            TestUtility.ORIYA_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:ORIYA:]", "[:MALAYALAM:]",
                  "[\u0D0E\u0D12\u0D31\u0D34\u0D35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
    public void TestTamilTelugu() throws IOException, ParseException {
        new Test("TELUGU-TAMIL", 
          TestUtility.TELUGU_SCRIPT, TestUtility.TAMIL_SCRIPT)
          .test("[:TELUGU:]", "[:TAMIL:]", 
                "[\u0ba9\u0bb4]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-TELUGU", 
            TestUtility.TAMIL_SCRIPT, TestUtility.TELUGU_SCRIPT )
          .test( "[:TAMIL:]", "[:TELUGU:]",
                  "[\u0C0B\u0C0C\u0C16\u0C17\u0C18\u0C1B\u0C1D\u0C20\u0C21\u0C22\u0C25\u0C26\u0C27\u0C2B\u0C2C\u0C2D\u0C36\u0C60\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTamilKannada() throws IOException, ParseException {
        new Test("KANNADA-TAMIL", 
          TestUtility.KANNADA_SCRIPT, TestUtility.TAMIL_SCRIPT)
          .test("[:KANNADA:]", "[:TAMIL:]", 
                "[\u0ba9\u0bb4]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-KANNADA", 
            TestUtility.TAMIL_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:TAMIL:]", "[:KANNADA:]",
                  "[\u0C8B\u0C8C\u0C96\u0C97\u0C98\u0C9B\u0C9D\u0CA0\u0CA1\u0CA2\u0CA5\u0CA6\u0CA7\u0CAB\u0CAC\u0CAD\u0CB6\u0CDE\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTamilMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-TAMIL", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.TAMIL_SCRIPT)
          .test("[:MALAYALAM:]", "[:TAMIL:]", 
                "[\u0ba9]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-MALAYALAM", 
            TestUtility.TAMIL_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:TAMIL:]", "[:MALAYALAM:]",
                  "[\u0D0B\u0D0C\u0D16\u0D17\u0D18\u0D1B\u0D1D\u0D20\u0D21\u0D22\u0D25\u0D26\u0D27\u0D2B\u0D2C\u0D2D\u0D36\u0D60\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTeluguKannada() throws IOException, ParseException {
        new Test("KANNADA-TELUGU", 
          TestUtility.KANNADA_SCRIPT, TestUtility.TELUGU_SCRIPT)
          .test("[:KANNADA:]", "[:TELUGU:]", 
                "[]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TELUGU-KANNADA", 
            TestUtility.TELUGU_SCRIPT, TestUtility.KANNADA_SCRIPT )
          .test( "[:TELUGU:]", "[:KANNADA:]",
                  "[\u0CDE]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTeluguMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-TELUGU", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.TELUGU_SCRIPT)
          .test("[:MALAYALAM:]", "[:TELUGU:]", 
                "[]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TELUGU-MALAYALAM", 
            TestUtility.TELUGU_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:TELUGU:]", "[:MALAYALAM:]",
                  "[\u0D34]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
    public void TestKannadaMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-KANNADA", 
          TestUtility.MALAYALAM_SCRIPT, TestUtility.KANNADA_SCRIPT)
          .test("[:MALAYALAM:]", "[:KANNADA:]", 
                "[\u0cDe]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("KANNADA-MALAYALAM", 
            TestUtility.KANNADA_SCRIPT, TestUtility.MALAYALAM_SCRIPT )
          .test( "[:KANNADA:]", "[:MALAYALAM:]",
                  "[\u0D34]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
    //---------------
    // End Indic
    //---------------
    public static class Legal {
        public boolean is(String sourceString) {return true;}
    }
    
    public static class LegalGreek extends Legal {
        
        boolean full;
        
        public LegalGreek(boolean full) {
            this.full = full;
        }
        
        public static boolean isVowel(char c) {
            return "\u03B1\u03B5\u03B7\u03B9\u03BF\u03C5\u03C9\u0391\u0395\u0397\u0399\u039F\u03A5\u03A9".indexOf(c) >= 0;
        }
        
        public static boolean isRho(char c) {
            return "\u03C1\u03A1".indexOf(c) >= 0;
        }
        
        public boolean is(String sourceString) { 
            try {
                String decomp = Normalizer.normalize(sourceString, Normalizer.DECOMP, 0);
                
                // modern is simpler: don't care about anything but a grave
                if (!full) {
                    if (sourceString.equals("\u039C\u03C0")) return false;
                    for (int i = 0; i < decomp.length(); ++i) {
                        char c = decomp.charAt(i);
                        // exclude all the accents
                        if (c == '\u0313' || c == '\u0314' || c == '\u0300' || c == '\u0302'
                            || c == '\u0342' || c == '\u0345'
                            ) return false;
                    }
                    return true;
                }
                
                // Legal full Greek has breathing marks IFF there is a vowel or RHO at the start
                // IF it has them, it has exactly one.
                // IF it starts with a RHO, then the breathing mark must come before the second letter.
                // Since there are no surrogates in greek, don't worry about them

                boolean firstIsVowel = false;
                boolean firstIsRho = false;
                boolean noLetterYet = true;
                int breathingCount = 0;
                int letterCount = 0;
                for (int i = 0; i < decomp.length(); ++i) {
                    char c = decomp.charAt(i);
                    if (UCharacter.isLetter(c)) {
                        ++letterCount;
                        if (noLetterYet) {
                            noLetterYet = false;
                            firstIsVowel = isVowel(c);
                            firstIsRho = isRho(c);
                        }
                        if (firstIsRho && letterCount == 2 && breathingCount == 0) return false;
                    }
                    if (c == '\u0313' || c == '\u0314') {
                        ++breathingCount;
                    }
                }
                
                if (firstIsVowel || firstIsRho) return breathingCount == 1;
                return breathingCount == 0;
            } catch (Throwable t) {
                System.out.println(t.getClass().getName() + " " + t.getMessage());
                return true;
            }
        }
    }
    
    static class Test {
    
        PrintWriter out;
    
        private String transliteratorID; 
        private byte sourceScript;
        private byte targetScript;
        private int errorLimit = Integer.MAX_VALUE;
        private int errorCount = 0;
        private int pairLimit  = 0x10000;
        UnicodeSet sourceRange;
        UnicodeSet targetRange;
        UnicodeSet roundtripExclusions;
        TestLog log;
        Legal legalSource;
        UnicodeSet badCharacters;
    
        /*
         * create a test for the given script transliterator.
         */
        Test(String transliteratorID, 
             byte sourceScript, byte targetScript) {
            this.transliteratorID = transliteratorID;
            this.sourceScript = sourceScript;
            this.targetScript = targetScript;
        }
    
        public void setErrorLimit(int limit) {
            errorLimit = limit;
        }
    
        public void setPairLimit(int limit) {
            pairLimit = limit;
        }
        
        // Added to do better equality check.
        
        public static boolean isSame(String a, String b) {
            if (a.equals(b)) return true;
            if (a.equalsIgnoreCase(b) && isCamel(a)) return true;
            a = Normalizer.normalize(a, Normalizer.DECOMP, 0);
            b = Normalizer.normalize(b, Normalizer.DECOMP, 0);
            if (a.equals(b)) return true;
            if (a.equalsIgnoreCase(b) && isCamel(a)) return true;
            return false;
        }
        
        public boolean includesSome(UnicodeSet set, String a) {
            int cp;
            for (int i = 0; i < a.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(a, i);
                if (set.contains(cp)) return true;
            }
            return false;
        }
        
        public static boolean isCamel(String a) {
            //System.out.println("CamelTest");
            // see if string is of the form aB; e.g. lower, then upper or title
            int cp;
            boolean haveLower = false;
            for (int i = 0; i < a.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(a, i);
                int t = UCharacter.getType(cp);
                //System.out.println("\t" + t + " " + Integer.toString(cp,16) + " " + UCharacter.getName(cp));
                switch (t) {
                    case Character.UPPERCASE_LETTER:
                        if (haveLower) return true;
                        break;
                    case Character.TITLECASE_LETTER:
                        if (haveLower) return true;
                        // drop through, since second letter is lower.
                    case Character.LOWERCASE_LETTER:
                        haveLower = true;
                        break;
                }
            }
            //System.out.println("FALSE");
            return false;
        }
      
        public void test(String sourceRange, String targetRange, String roundtripExclusions,
                         TestLog log, Legal legalSource) 
          throws java.io.IOException, java.text.ParseException {
            
            this.legalSource = legalSource;
      
            if (sourceRange != null && sourceRange.length() > 0) {
                this.sourceRange = new UnicodeSet(sourceRange);
            }else{
                this.sourceRange = new UnicodeSet("[a-zA-Z]");
            }
            if (targetRange != null && targetRange.length() > 0) {
                this.targetRange = new UnicodeSet(targetRange);
            }
            if (roundtripExclusions != null && roundtripExclusions.length() > 0) {
                this.roundtripExclusions = new UnicodeSet(roundtripExclusions);
            }else{
                this.roundtripExclusions = new UnicodeSet(); // empty
            }

            this.log = log;

            log.logln(Utility.escape("Source:  " + this.sourceRange));
            log.logln(Utility.escape("Target:  " + this.targetRange));
            log.logln(Utility.escape("Exclude: " + this.roundtripExclusions));
            
            badCharacters = new UnicodeSet("[:other:]");

            // make a UTF-8 output file we can read with a browser

            // note: check that every transliterator transliterates the null string correctly!

            String logFileName = "test_" + transliteratorID + "_"
                + sourceScript + "_" + targetScript + ".html";

            File lf = new File(logFileName); 
            log.logln("Creating log file " + lf.getAbsoluteFile());

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                      new FileOutputStream(logFileName), "UTF8"), 4*1024));
            //out.write('\uFFEF');    // BOM
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<HTML><HEAD>");
            out.println("<META content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
            out.println("<BODY>");
            try {
                test2();
            } catch (TestTruncated e) {
                out.println(e.getMessage());
            }
            out.println("</BODY></HTML>");
            out.close();

            if (errorCount > 0) {
                log.errln(transliteratorID + " errors: " + errorCount + ", see " + lf.getAbsoluteFile());
            } else {
                log.logln(transliteratorID + " ok");
                new File(logFileName).delete();
            }
        }

        public void test2() {

            Transliterator sourceToTarget = Transliterator.getInstance(transliteratorID);
            Transliterator targetToSource = sourceToTarget.getInverse();

            log.logln("Checking that source characters convert to target - Singles");
            
            BitSet failSourceTarg = new BitSet();

            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !isSource(c)) continue;
                String cs = String.valueOf(c);
                String targ = sourceToTarget.transliterate(cs);
                if (!isReceivingTarget(targ) || includesSome(badCharacters, targ)) {
                    logWrongScript("Source-Target", cs, targ);
                    failSourceTarg.set(c);
                } else {
                    String cs2 = Normalizer.normalize(cs, Normalizer.DECOMP, 0);
                    String targ2 = sourceToTarget.transliterate(cs2);
                    if (!targ.equals(targ2)) {
                        logNotCanonical("Source-Target", cs, targ, targ2);
                    }
                }
            }

            log.logln("Checking that source characters convert to target - Doubles");

            for (char c = 0; c < 0xFFFF; ++c) { 
                if (TestUtility.isUnassigned(c) ||
                    !isSource(c)) continue;
                if (failSourceTarg.get(c)) continue;
                
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !isSource(d)) continue;
                    if (failSourceTarg.get(d)) continue;
                    
                    String cs = String.valueOf(c) + d;
                    String targ = sourceToTarget.transliterate(cs);
                    if (!isReceivingTarget(targ) || includesSome(badCharacters, targ)) {
                        logWrongScript("Source-Target", cs, targ);
                } else {
                    String cs2 = Normalizer.normalize(cs, Normalizer.DECOMP, 0);
                    String targ2 = sourceToTarget.transliterate(cs2);
                    if (!targ.equals(targ2)) {
                        logNotCanonical("Source-Target", cs, targ, targ2);
                    }
                }
                }
            }

            log.logln("Checking that target characters convert to source and back - Singles");
            
            BitSet failTargSource = new BitSet();
            BitSet failRound = new BitSet();

            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !isTarget(c)) continue;
                String cs = String.valueOf(c);
                String targ = targetToSource.transliterate(cs);
                String reverse = sourceToTarget.transliterate(targ);
                if (!isReceivingSource(targ) || includesSome(badCharacters, targ)) {
                    logWrongScript("Target-Source", cs, targ);
                    failTargSource.set(c);
                } else if (!isSame(cs, reverse) && !roundtripExclusions.contains(c)) {
                    logRoundTripFailure(cs, targ, reverse);
                    failRound.set(c);
                } else {
                    String targ2 = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                    String reverse2 = sourceToTarget.transliterate(targ2);
                    if (!reverse.equals(reverse2)) {
                        logNotCanonical("Target-Source", cs, targ, targ2);
                    }
                }
            }

            log.logln("Checking that target characters convert to source and back - Doubles");
            int count = 0;
            StringBuffer buf = new StringBuffer("aa");
            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !isTarget(c)) continue;
                if (++count > pairLimit) {
                    throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
                }
                buf.setCharAt(0, c);
                log.log(TestUtility.hex(c));
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !isTarget(d)) continue;
                    buf.setCharAt(1, d);
                    String cs = buf.toString();
                    String targ = targetToSource.transliterate(cs);
                    String reverse = sourceToTarget.transliterate(targ);
                    if (!isReceivingSource(targ) && !failTargSource.get(c) && !failTargSource.get(d)
                         || includesSome(badCharacters, targ)) {
                        logWrongScript("Target-Source", cs, targ);
                    } else if (!isSame(cs, reverse) && !failRound.get(c) && !failRound.get(d)
                         && !roundtripExclusions.contains(c) && !roundtripExclusions.contains(d)) {
                        logRoundTripFailure(cs, targ, reverse);
                    } else {
                        String targ2 = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                        String reverse2 = sourceToTarget.transliterate(targ2);
                        if (!reverse.equals(reverse2)) {
                            logNotCanonical("Target-Source", cs, targ, targ2);
                        }
                    }
                }
            }
            log.logln("");
        }

        final void logWrongScript(String label, String from, String to) {
            if (++errorCount >= errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
            out.println("<br>Fail " + label + ": " +
                        from + " (" +
                        TestUtility.hex(from) + ") => " +
                        to + " (" +
                        TestUtility.hex(to) + ")"
                        );
        }

        final void logNotCanonical(String label, String from, String to, String toCan) {
            if (++errorCount >= errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
            out.println("<br>Fail (can.equiv)" + label + ": " +
                        from + " (" +
                        TestUtility.hex(from) + ") => " +
                        to + " (" +
                        TestUtility.hex(to) + ")" +
                        toCan + " (" +
                        TestUtility.hex(to) + ")"
                        );
        }

        final void logRoundTripFailure(String from, String to, String back) {
            if (!legalSource.is(from)) return; // skip illegals
            
            if (++errorCount >= errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
            out.println("<br>Fail Roundtrip: " +
                        from + " (" +
                        TestUtility.hex(from) + ") => " +
                        to + " (" +
                        TestUtility.hex(to) + ") => " +
                        back + " (" +
                        TestUtility.hex(back) + ")" 
                        );
        }

        /*
         * Characters to filter for source-target mapping completeness
         * Typically is base alphabet, minus extended characters
         * Default is ASCII letters for Latin
         */
        public boolean isSource(char c) {
            byte script = TestUtility.getScript(c);
            if (script != sourceScript) return false;
            if (!TestUtility.isLetter(c)) return false;
            if (!sourceRange.contains(c)) return false;
            return true;
        }

        /*
         * Characters to check for target back to source mapping.
         * Typically the same as the target script, plus punctuation
         */
        public boolean isReceivingSource(char c) {
            byte script = TestUtility.getScript(c);
            return (script == sourceScript || script == TestUtility.COMMON_SCRIPT);
        }

        /*
         * Characters to filter for target-source mapping
         * Typically is base alphabet, minus extended characters
         */
        public boolean isTarget(char c) {
            byte script = TestUtility.getScript(c);
            if (script != targetScript) return false;
            if (!TestUtility.isLetter(c)) return false;
            if (targetRange != null && !targetRange.contains(c)) return false;
            return true;
        }

        /*
         * Characters to check for target-source mapping
         * Typically the same as the source script, plus punctuation
         */
        public boolean isReceivingTarget(char c) {
            byte script = TestUtility.getScript(c);
            return (script == targetScript || script == TestUtility.COMMON_SCRIPT);
        }

        final boolean isSource(String s) {
            for (int i = 0; i < s.length(); ++i) {
                if (!isSource(s.charAt(i))) return false;
            }
            return true;
        }

        final boolean isTarget(String s) {
            for (int i = 0; i < s.length(); ++i) {
                if (!isTarget(s.charAt(i))) return false;
            }
            return true;
        }

        final boolean isReceivingSource(String s) {
            for (int i = 0; i < s.length(); ++i) {
                if (!isReceivingSource(s.charAt(i))) return false;
            }
            return true;
        }

        final boolean isReceivingTarget(String s) {
            for (int i = 0; i < s.length(); ++i) {
                if (!isReceivingTarget(s.charAt(i))) return false;
            }
            return true;
        }

        static class TestTruncated extends RuntimeException {
            TestTruncated(String msg) {
                super(msg);
            }
        }
    }

//  static class TestHangul extends Test {
//      TestHangul () {
//          super("Jamo-Hangul", TestUtility.JAMO_SCRIPT, TestUtility.HANGUL_SCRIPT);
//      }
//
//      public boolean isSource(char c) {
//          if (0x1113 <= c && c <= 0x1160) return false;
//          if (0x1176 <= c && c <= 0x11F9) return false;
//          if (0x3131 <= c && c <= 0x318E) return false;
//          return super.isSource(c);
//      }
//  }
}
