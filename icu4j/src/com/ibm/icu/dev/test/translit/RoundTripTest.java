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
        new Test("Latin-Hiragana")
          .test("[a-zA-Z]", "[[:hiragana:]\u3040-\u3094]", "[\u309D\u309E]", this, new Legal());
    }

    public void TestKatakana() throws IOException, ParseException {
        new Test("Latin-Katakana")
          .test("[a-zA-Z]", "[[:katakana:]\u30A1-\u30FA\u30FC]", "[\u30FD\u30FE\uFF66-\uFF9D]", this, new Legal());
    }

// Some transliterators removed for 2.0

//  public void TestArabic() throws IOException, ParseException {
//      new Test("Latin-Arabic", 
//        TestUtility.LATIN_SCRIPT, TestUtility.ARABIC_SCRIPT)
//        .test("[a-zA-Z]", "[\u0620-\u065F-[\u0640]]", this);
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
        new Test("Latin-Jamo")
            .test("[a-zA-Z]", "[\u1100-\u1113 \u1161-\u1176 \u11A8-\u11C2]", "", this, new Legal());
    }

/*
        SBase = 0xAC00, LBase = 0x1100, VBase = 0x1161, TBase = 0x11A7,
        LCount = 19, VCount = 21, TCount = 28,
        NCount = VCount * TCount,   // 588
        SCount = LCount * NCount,   // 11172
        LLimit = LBase + LCount,    // 1113
        VLimit = VBase + VCount,    // 1176
        TLimit = TBase + TCount,    // 11C3
        SLimit = SBase + SCount;    // D7A4
*/

    public void TestHangul() throws IOException, ParseException {
        Test t = new Test("Latin-Hangul");
        t.setDoublePercentage(0.02);
        t.test("[a-zA-Z]", "[\uAC00-\uD7A4]", "", this, new Legal());
    }

    public void TestGreek() throws IOException, ParseException {
        try {
            Legal lt = new LegalGreek(true);
            new Test("Latin-Greek")
            .test("[a-zA-Z]", "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
                "[\u00B5\u037A\u03D0-\u03F5]", /* roundtrip exclusions */
                this, lt);
        } catch (RuntimeException e) {
            System.out.println(e.getClass().getName() + ", " + e.getMessage());
            throw e;
        }
    }

    public void Testel() throws IOException, ParseException {
        new Test("Latin-el")
          .test("[a-zA-Z]", "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
            "[\u00B5\u037A\u03D0-\uFFFF]", /* roundtrip exclusions */
            this, new LegalGreek(false));
    }

    public void TestCyrillic() throws IOException, ParseException {
        new Test("Latin-Cyrillic")
          .test("[a-zA-Z]", "[\u0400-\u045F]", null, this, new Legal());
    }
    
    //----------------------------------
    // Inter-Indic Tests
    //----------------------------------
   public void TestDevanagariLatin() throws IOException, ParseException {
        new Test("Latin-DEVANAGARI")
          .test("[a-zA-Z]", "[:Devanagari:]", null, this, new Legal());
    }
    public void TestDevanagariBengali() throws IOException, ParseException {
        new Test("BENGALI-DEVANAGARI")
          .test("[:BENGALI:]", "[:Devanagari:]", 
                "[\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-BENGALI")
          .test( "[:Devanagari:]", "[:BENGALI:]",
                  "[\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestDevanagariGurmukhi() throws IOException, ParseException {
        new Test("GURMUKHI-DEVANAGARI")
          .test("[:GURMUKHI:]", "[:Devanagari:]", 
                "[\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-GURMUKHI")
          .test( "[:Devanagari:]", "[:GURMUKHI:]",
                  "[\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                  this, new Legal());
    } 
    public void TestDevanagariGujarati() throws IOException, ParseException {
        new Test("GUJARATI-DEVANAGARI")
          .test("[:GUJARATI:]", "[:Devanagari:]", 
                "[\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-GUJARATI")
          .test( "[:Devanagari:]", "[:GUJARATI:]",
                  "[\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariOriya() throws IOException, ParseException {
        new Test("ORIYA-DEVANAGARI")
          .test("[:ORIYA:]", "[:Devanagari:]", 
                "[\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-ORIYA")
          .test( "[:Devanagari:]", "[:ORIYA:]",
                  "[\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariTamil() throws IOException, ParseException {
        new Test("Tamil-DEVANAGARI")
          .test("[:tamil:]", "[:Devanagari:]", 
                  "[\u090B\u090C\u090D\u0911\u0916\u0917\u0918\u091B\u091D\u0920\u0921\u0922\u0925\u0926\u0927\u092B\u092C\u092D\u0936\u093d\u0950[\u0958-\u0961]]", /*roundtrip exclusions*/
                  this, new Legal());
        new Test("DEVANAGARI-Tamil")
          .test( "[:Devanagari:]", "[:tamil:]",
                  "", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestDevanagariTelugu() throws IOException, ParseException {
        new Test("Telugu-DEVANAGARI")
          .test("[:telugu:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-TELUGU")
          .test( "[:Devanagari:]", "[:TELUGU:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestDevanagariKannada() throws IOException, ParseException {
        new Test("KANNADA-DEVANAGARI")
          .test("[:KANNADA:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-KANNADA")
          .test( "[:Devanagari:]", "[:KANNADA:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/ 
                  this, new Legal());
    }
    public void TestDevanagariMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-DEVANAGARI")
          .test("[:MALAYALAM:]", "[:Devanagari:]", 
                "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("DEVANAGARI-MALAYALAM")
          .test( "[:Devanagari:]", "[:MALAYALAM:]",
                  "[\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestBengaliGurmukhi() throws IOException, ParseException {
        new Test("GURMUKHI-BENGALI")
          .test("[:GURMUKHI:]", "[:BENGALI:]",  
                "[\u098B\u098C\u09B7\u09E0\u09E1\u09F0\u09F1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-GURMUKHI")
          .test( "[:BENGALI:]", "[:GURMUKHI:]",
                  "[\u0A33\u0A35\u0A59\u0A5A\u0A5B\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                  this, new Legal());
    } 
    public void TestBengaliGujarati() throws IOException, ParseException {
        new Test("GUJARATI-BENGALI")
          .test("[:GUJARATI:]", "[:BENGALI:]", 
                "[\u098c\u09e1\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-GUJARATI" )
          .test( "[:BENGALI:]", "[:GUJARATI:]",
                  "[\u0A8D\u0A91\u0AB3\u0AB5\u0ABD\u0AD0]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliOriya() throws IOException, ParseException {
        new Test("ORIYA-BENGALI")
          .test("[:ORIYA:]", "[:BENGALI:]", 
                "[\u09f0\u09f1]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-ORIYA")
          .test( "[:BENGALI:]", "[:ORIYA:]",
                  "[\u0b33\u0b3d]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliTamil() throws IOException, ParseException {
        new Test("Tamil-BENGALI")
          .test("[:tamil:]", "[:BENGALI:]", 
                  "[\u09f0\u09f1\u098B\u098C\u0996\u0997\u0998\u099B\u099D\u09A0\u09A1\u09A2\u09A5\u09A6\u09A7\u09AB\u09AC\u09AD\u09B6\u09DC\u09DD\u09DF\u09E0\u09E1]", /*roundtrip exclusions*/
                  this, new Legal());
        new Test("BENGALI-Tamil")
          .test( "[:BENGALI:]", "[:tamil:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB3\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  this, new Legal());
   }
   public void TestBengaliTelugu() throws IOException, ParseException {
        new Test("Telugu-BENGALI")
          .test("[:telugu:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-TELUGU")
          .test( "[:BENGALI:]", "[:TELUGU:]",
                  "[\u0C0E\u0C12\u0C31\u0C33\u0C35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestBengaliKannada() throws IOException, ParseException {
        new Test("KANNADA-BENGALI")
          .test("[:KANNADA:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-KANNADA")
          .test( "[:BENGALI:]", "[:KANNADA:]",
                  "[\u0C8E\u0C92\u0CB1\u0cb3\u0cb5\u0cde]", /*roundtrip exclusions*/ 
                  this, new Legal());
    }
    public void TestBengaliMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-BENGALI")
          .test("[:MALAYALAM:]", "[:BENGALI:]", 
                "[\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("BENGALI-MALAYALAM")
          .test( "[:BENGALI:]", "[:MALAYALAM:]",
                  "[\u0d31-\u0d35\u0d0e\u0d12]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiGujarati() throws IOException, ParseException {
        new Test("GUJARATI-GURMUKHI")
          .test("[:GUJARATI:]", "[:GURMUKHI:]", 
                "[\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0abd]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-GUJARATI")
          .test( "[:GURMUKHI:]", "[:GUJARATI:]",
                  "[\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0ab7\u0abd\u0ad0\u0ae0]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiOriya() throws IOException, ParseException {
        new Test("ORIYA-GURMUKHI")
          .test("[:ORIYA:]", "[:GURMUKHI:]", 
                "[\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61\u0a35\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-ORIYA")
          .test( "[:GURMUKHI:]", "[:ORIYA:]",
                  "[\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiTamil() throws IOException, ParseException {
        new Test("TAMIL-GURMUKHI")
          .test("[:TAMIL:]", "[:GURMUKHI:]", 
                "[\u0A16\u0A17\u0A18\u0A1B\u0A1D\u0A20\u0A21\u0A22\u0A25\u0A26\u0A27\u0A2B\u0A2C\u0A2D\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-TAMIL")
          .test( "[:GURMUKHI:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiTelugu() throws IOException, ParseException {
        new Test("TELUGU-GURMUKHI")
          .test("[:TELUGU:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-TELUGU")
          .test( "[:GURMUKHI:]", "[:TELUGU:]",
                  "[\u0C0B\u0C0C\u0C0E\u0C12\u0C31\u0C37\u0C60\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiKannada() throws IOException, ParseException {
        new Test("KANNADA-GURMUKHI")
          .test("[:KANNADA:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-KANNADA")
          .test( "[:GURMUKHI:]", "[:KANNADA:]",
                  "[\u0C8B\u0C8C\u0C8E\u0C92\u0CB1\u0CB7\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGurmukhiMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-GURMUKHI")
          .test("[:MALAYALAM:]", "[:GURMUKHI:]", 
                "[\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GURMUKHI-MALAYALAM")
          .test( "[:GURMUKHI:]", "[:MALAYALAM:]",
                  "[\u0D0B\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D37\u0D60\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
     public void TestGujaratiOriya() throws IOException, ParseException {
        new Test("GUJARATI-ORIYA")
          .test("[:GUJARATI:]", "[:ORIYA:]", 
                "[\u0B0C\u0B5F\u0B61]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-GUJARATI")
          .test( "[:ORIYA:]", "[:GUJARATI:]",
                  "[\u0A8D\u0A91\u0AB5\u0Ad0]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiTamil() throws IOException, ParseException {
        new Test("TAMIL-GUJARATI")
          .test("[:TAMIL:]", "[:GUJARATI:]", 
                "[\u0A8B\u0A8D\u0A91\u0A96\u0A97\u0A98\u0A9B\u0A9D\u0AA0\u0AA1\u0AA2\u0AA5\u0AA6\u0AA7\u0AAB\u0AAC\u0AAD\u0AB6\u0ABD\u0AD0\u0AE0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-TAMIL")
          .test( "[:GUJARATI:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiTelugu() throws IOException, ParseException {
        new Test("TELUGU-GUJARATI")
          .test("[:TELUGU:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-TELUGU")
          .test( "[:GUJARATI:]", "[:TELUGU:]",
                  "[\u0C0C\u0C0E\u0C12\u0C31\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiKannada() throws IOException, ParseException {
        new Test("KANNADA-GUJARATI")
          .test("[:KANNADA:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-KANNADA")
          .test( "[:GUJARATI:]", "[:KANNADA:]",
                  "[\u0C8C\u0C8E\u0C92\u0CB1\u0CDE\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestGujaratiMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-GUJARATI")
          .test("[:MALAYALAM:]", "[:GUJARATI:]", 
                "[\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("GUJARATI-MALAYALAM")
          .test( "[:GUJARATI:]", "[:MALAYALAM:]",
                  "[\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
  public void TestOriyaTamil() throws IOException, ParseException {
        new Test("TAMIL-ORIYA")
          .test("[:TAMIL:]", "[:ORIYA:]", 
                "[\u0B0B\u0B0C\u0B16\u0B17\u0B18\u0B1B\u0B1D\u0B20\u0B21\u0B22\u0B25\u0B26\u0B27\u0B2B\u0B2C\u0B2D\u0B36\u0B3D\u0B5C\u0B5D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-TAMIL")
          .test( "[:ORIYA:]", "[:TAMIL:]",
                  "[\u0B8E\u0B92\u0BA9\u0BB1\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaTelugu() throws IOException, ParseException {
        new Test("TELUGU-ORIYA")
          .test("[:TELUGU:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-TELUGU")
          .test( "[:ORIYA:]", "[:TELUGU:]",
                  "[\u0C0E\u0C12\u0C31\u0C35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaKannada() throws IOException, ParseException {
        new Test("KANNADA-ORIYA")
          .test("[:KANNADA:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-KANNADA")
          .test( "[:ORIYA:]", "[:KANNADA:]",
                  "[\u0C8E\u0C92\u0CB1\u0CB5\u0CDE]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestOriyaMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-ORIYA")
          .test("[:MALAYALAM:]", "[:ORIYA:]", 
                "[\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("ORIYA-MALAYALAM" )
          .test( "[:ORIYA:]", "[:MALAYALAM:]",
                  "[\u0D0E\u0D12\u0D31\u0D34\u0D35]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
    public void TestTamilTelugu() throws IOException, ParseException {
        new Test("TELUGU-TAMIL")
          .test("[:TELUGU:]", "[:TAMIL:]", 
                "[\u0ba9\u0bb4]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-TELUGU" )
          .test( "[:TAMIL:]", "[:TELUGU:]",
                  "[\u0C0B\u0C0C\u0C16\u0C17\u0C18\u0C1B\u0C1D\u0C20\u0C21\u0C22\u0C25\u0C26\u0C27\u0C2B\u0C2C\u0C2D\u0C36\u0C60\u0C61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTamilKannada() throws IOException, ParseException {
        new Test("KANNADA-TAMIL")
          .test("[:KANNADA:]", "[:TAMIL:]", 
                "[\u0ba9\u0bb4]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-KANNADA" )
          .test( "[:TAMIL:]", "[:KANNADA:]",
                  "[\u0C8B\u0C8C\u0C96\u0C97\u0C98\u0C9B\u0C9D\u0CA0\u0CA1\u0CA2\u0CA5\u0CA6\u0CA7\u0CAB\u0CAC\u0CAD\u0CB6\u0CDE\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTamilMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-TAMIL")
          .test("[:MALAYALAM:]", "[:TAMIL:]", 
                "[\u0ba9]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TAMIL-MALAYALAM")
          .test( "[:TAMIL:]", "[:MALAYALAM:]",
                  "[\u0D0B\u0D0C\u0D16\u0D17\u0D18\u0D1B\u0D1D\u0D20\u0D21\u0D22\u0D25\u0D26\u0D27\u0D2B\u0D2C\u0D2D\u0D36\u0D60\u0D61]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTeluguKannada() throws IOException, ParseException {
        new Test("KANNADA-TELUGU")
          .test("[:KANNADA:]", "[:TELUGU:]", 
                "[]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TELUGU-KANNADA")
          .test( "[:TELUGU:]", "[:KANNADA:]",
                  "[\u0CDE]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    public void TestTeluguMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-TELUGU")
          .test("[:MALAYALAM:]", "[:TELUGU:]", 
                "[]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("TELUGU-MALAYALAM" )
          .test( "[:TELUGU:]", "[:MALAYALAM:]",
                  "[\u0D34]", /*roundtrip exclusions*/
                  this, new Legal());
    }
    
    public void TestKannadaMalayalam() throws IOException, ParseException {
        new Test("MALAYALAM-KANNADA")
          .test("[:MALAYALAM:]", "[:KANNADA:]", 
                "[\u0cDe]", /*roundtrip exclusions*/
                this, new Legal());
        new Test("KANNADA-MALAYALAM")
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
        private int errorLimit = 500;
        private int errorCount = 0;
        private int pairLimit  = 0x10000;
        UnicodeSet sourceRange;
        UnicodeSet targetRange;
        UnicodeSet toSource;
        UnicodeSet toTarget;
        UnicodeSet roundtripExclusions;
        
        double doublePercentage = 1.0;
        
        TestLog log;
        Legal legalSource;
        UnicodeSet badCharacters;
    
        /*
         * create a test for the given script transliterator.
         */
        Test(String transliteratorID) {
            this.transliteratorID = transliteratorID;
        }
    
        public void setErrorLimit(int limit) {
            errorLimit = limit;
        }
    
        public void setPairLimit(int limit) {
            pairLimit = limit;
        }
        
        public void setDoublePercentage(double newval) {
            doublePercentage = newval;
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
        
        /*
        public boolean includesSome(UnicodeSet set, String a) {
            int cp;
            for (int i = 0; i < a.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(a, i);
                if (set.contains(cp)) return true;
            }
            return false;
        }
        */
        
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
        
        static final UnicodeSet okAnyway = new UnicodeSet("[^[:Letter:]]");
        static final UnicodeSet neverOk = new UnicodeSet("[:Other:]");
      
        public void test(String sourceRange, String targetRange, String roundtripExclusions,
                         TestLog log, Legal legalSource) 
          throws java.io.IOException, java.text.ParseException {
            
            this.legalSource = legalSource;
            this.sourceRange = new UnicodeSet(sourceRange);
            this.sourceRange.removeAll(neverOk);
            
            this.targetRange = new UnicodeSet(targetRange);
            this.targetRange.removeAll(neverOk);
            
            this.toSource = new UnicodeSet(sourceRange);
            this.toSource.addAll(okAnyway);
            
            this.toTarget = new UnicodeSet(targetRange);
            this.toTarget.addAll(okAnyway);
            
            if (roundtripExclusions != null && roundtripExclusions.length() > 0) {
                this.roundtripExclusions = new UnicodeSet(roundtripExclusions);
            }else{
                this.roundtripExclusions = new UnicodeSet(); // empty
            }

            this.log = log;

            log.logln(Utility.escape("Source:  " + this.sourceRange));
            log.logln(Utility.escape("Target:  " + this.targetRange));
            log.logln(Utility.escape("Exclude: " + this.roundtripExclusions));
            if (doublePercentage < 1.0) log.logln("Double Percentage: " + doublePercentage);
            
            badCharacters = new UnicodeSet("[:other:]");

            // make a UTF-8 output file we can read with a browser

            // note: check that every transliterator transliterates the null string correctly!

            String logFileName = "test_" + transliteratorID + ".html";

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
                log.errln(transliteratorID + " errors: " 
                    + errorCount + (errorCount > errorLimit ? " (at least!)" : "")
                    + ", see " + lf.getAbsoluteFile());
            } else {
                log.logln(transliteratorID + " ok");
                new File(logFileName).delete();
            }
        }

        public void test2() {

            Transliterator sourceToTarget = Transliterator.getInstance(transliteratorID);
            Transliterator targetToSource = sourceToTarget.getInverse();

            log.logln("Checking that source characters convert to target - Singles");
            
            UnicodeSet failSourceTarg = new UnicodeSet();

            /*
            for (char c = 0; c < 0xFFFF; ++c) {
                if (!sourceRange.contains(c)) continue;
                */
            UnicodeSetIterator usi = new UnicodeSetIterator(sourceRange);
            while (true) {
                int c = usi.next();
                if (c < 0) break;
                
                String cs = UTF16.valueOf(c);
                String targ = sourceToTarget.transliterate(cs);
                if (!UnicodeSetIterator.containsAll(toTarget, targ) 
                        || UnicodeSetIterator.containsSome(badCharacters, targ)) {
                    String targD = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                    if (!UnicodeSetIterator.containsAll(toTarget, targD) 
                            || UnicodeSetIterator.containsSome(badCharacters, targD)) {
                        logWrongScript("Source-Target", cs, targ);
                        failSourceTarg.add(c);
                        continue;
                    }
                }
                
                String cs2 = Normalizer.normalize(cs, Normalizer.DECOMP, 0);
                String targ2 = sourceToTarget.transliterate(cs2);
                if (!targ.equals(targ2)) {
                    logNotCanonical("Source-Target", cs, targ, targ2);
                }
            }

            log.logln("Checking that source characters convert to target - Doubles");
            
            /*
            for (char c = 0; c < 0xFFFF; ++c) { 
                if (TestUtility.isUnassigned(c) ||
                    !sourceRange.contains(c)) continue;
                if (failSourceTarg.get(c)) continue;
                
            */
            
            UnicodeSet sourceRangeMinusFailures = new UnicodeSet(sourceRange);
            sourceRangeMinusFailures.removeAll(failSourceTarg);
            
            UnicodeSetIterator usi2 = new UnicodeSetIterator();

            usi.reset(sourceRangeMinusFailures);
            while (true) {
                int c = usi.next();
                if (c < 0) break;
             
                /*
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !sourceRange.contains(d)) continue;
                    if (failSourceTarg.get(d)) continue;
                */
                usi2.reset(sourceRangeMinusFailures);
                while (true) {
                    int d = usi2.next();
                    if (d < 0) break;
                    
                    String cs = UTF16.valueOf(c) + UTF16.valueOf(d);
                    String targ = sourceToTarget.transliterate(cs);
                    if (!UnicodeSetIterator.containsAll(toTarget,targ) 
                            || UnicodeSetIterator.containsSome(badCharacters, targ)) {
                        String targD = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                        if (!UnicodeSetIterator.containsAll(toTarget,targD) 
                                || UnicodeSetIterator.containsSome(badCharacters, targD)) {
                            logWrongScript("Source-Target", cs, targ);
                            continue;
                        }
                    }
                    String cs2 = Normalizer.normalize(cs, Normalizer.DECOMP, 0);
                    String targ2 = sourceToTarget.transliterate(cs2);
                    if (!targ.equals(targ2)) {
                        logNotCanonical("Source-Target", cs, targ, targ2);
                    }
                }
            }

            log.logln("Checking that target characters convert to source and back - Singles");
            
            UnicodeSet failTargSource = new UnicodeSet();
            UnicodeSet failRound = new UnicodeSet();

            /*for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !targetRange.contains(c)) continue;
                    */
                    
            usi.reset(targetRange);
            while (true) {
                int c = usi.next();
                if (c < 0) break;
                    
                String cs = UTF16.valueOf(c);
                String targ = targetToSource.transliterate(cs);
                String reverse = sourceToTarget.transliterate(targ);
                
                if (!UnicodeSetIterator.containsAll(toSource, targ) 
                        || UnicodeSetIterator.containsSome(badCharacters, targ)) {
                    String targD = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                    if (!UnicodeSetIterator.containsAll(toSource, targD) 
                            || UnicodeSetIterator.containsSome(badCharacters, targD)) {
                        logWrongScript("Target-Source", cs, targ);
                        failTargSource.add(c);
                        continue;
                    }
                }
                if (!isSame(cs, reverse) && !roundtripExclusions.contains(c)) {
                    logRoundTripFailure(cs, targ, reverse);
                    failRound.add(c);
                    continue;
                }
                String targ2 = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                String reverse2 = sourceToTarget.transliterate(targ2);
                if (!reverse.equals(reverse2)) {
                    logNotCanonical("Target-Source", cs, targ, targ2);
                }
            }

            log.logln("Checking that target characters convert to source and back - Doubles");
            int count = 0;
            
            UnicodeSet targetRangeMinusFailures = new UnicodeSet(targetRange);
            targetRangeMinusFailures.removeAll(failTargSource);
            targetRangeMinusFailures.removeAll(failRound);
            
            //char[] buf = new char[4]; // maximum we can have with 2 code points
            /*
            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !targetRange.contains(c)) continue;
                    */
            
            usi.reset(targetRangeMinusFailures);
            while (true) {
                int c = usi.next();
                if (c < 0) break;
                
                if (doublePercentage != 1.0) {
                    double rand = Math.random();
                    if (rand > doublePercentage) {
                        //log.log(".");
                        continue;
                    }
                }
                    
                if (++count > pairLimit) {
                    throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
                }
                log.log(TestUtility.hex(c));
                
                /*
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !targetRange.contains(d)) continue;
                        */
                usi2.reset(targetRangeMinusFailures);
                while (true) {
                    int d = usi2.next();
                    if (d < 0) break;
                    
                    if (doublePercentage != 1.0) {
                        if (Math.random() > doublePercentage) continue;
                    }
                                            
                    String cs = UTF16.valueOf(c) + UTF16.valueOf(d);
                    String targ = targetToSource.transliterate(cs);
                    String reverse = sourceToTarget.transliterate(targ);
                    
                    if (!UnicodeSetIterator.containsAll(toSource, targ) /*&& !failTargSource.contains(c) && !failTargSource.contains(d)*/
                            || UnicodeSetIterator.containsSome(badCharacters, targ)) {
                        String targD = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                        if (!UnicodeSetIterator.containsAll(toSource, targD) /*&& !failTargSource.contains(c) && !failTargSource.contains(d)*/
                                || UnicodeSetIterator.containsSome(badCharacters, targD)) {
                            logWrongScript("Target-Source", cs, targ);
                            continue;
                        }
                    }
                    if (!isSame(cs, reverse) /*&& !failRound.contains(c) && !failRound.contains(d)*/
                         && !roundtripExclusions.contains(c) && !roundtripExclusions.contains(d)) {
                        logRoundTripFailure(cs, targ, reverse);
                        continue;
                    }
                    String targ2 = Normalizer.normalize(targ, Normalizer.DECOMP, 0);
                    String reverse2 = sourceToTarget.transliterate(targ2);
                    if (!reverse.equals(reverse2)) {
                        logNotCanonical("Target-Source", cs, targ, targ2);
                    }
                }
            }
            log.logln("");
        }

        final void logWrongScript(String label, String from, String to) {
            if (++errorCount > errorLimit) {
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
            if (++errorCount > errorLimit) {
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
            
            if (++errorCount > errorLimit) {
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
         /*
        public boolean isSource(char c) {
            if (!sourceRange.contains(c)) return false;
            return true;
        }
        */

        /*
         * Characters to check for target back to source mapping.
         * Typically the same as the target script, plus punctuation
         */
         /*
        public boolean isReceivingSource(char c) {
            if (!targetRange.contains(c)) return false;
            return true;
        }
        */
        /*
         * Characters to filter for target-source mapping
         * Typically is base alphabet, minus extended characters
         */
         /*
        public boolean isTarget(char c) {
            byte script = TestUtility.getScript(c);
            if (script != targetScript) return false;
            if (!TestUtility.isLetter(c)) return false;
            if (targetRange != null && !targetRange.contains(c)) return false;
            return true;
        }
        */
        
        /*
         * Characters to check for target-source mapping
         * Typically the same as the source script, plus punctuation
         */
        /*
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
        */

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
