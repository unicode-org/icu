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
    
    static final boolean EXTRA_TESTS = true;
    static final boolean PRINT_RULES = true;
    
    public static void main(String[] args) throws Exception {
        new RoundTripTest().run(args);
    }
    /*
    public void TestSingle() throws IOException, ParseException {
        Transliterator t = Transliterator.getInstance("Latin-Greek");
        String s = t.transliterate("\u0101\u0069");
    }
    */
    
    public void TestKana() throws IOException, ParseException {
        new Test("Katakana-Hiragana")
          .test("[[:katakana:]\u30A1-\u30FA\u30FC]", "[[:hiragana:]\u3040-\u3094\u30FC]",
            "[\u30FC\u309D\u309E\uFF66-\uFF9D]", this, new Legal());
    }

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
            .test("[a-zA-Z]", "[\u1100-\u1112 \u1161-\u1175 \u11A8-\u11C2]", "", this, new LegalJamo());
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
        t.test("[a-zA-Z]", "[\uAC00-\uD7A4]", "", this, new Legal());
    }

    public void TestGreek() throws IOException, ParseException {
        new Test("Latin-Greek")
        .test("[a-zA-Z]", "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
            "[\u00B5\u037A\u03D0-\u03F5]", /* roundtrip exclusions */
            this, new LegalGreek(true));
    }

    public void TestGreekUNGEGN() throws IOException, ParseException {
        new Test("Latin-Greek/UNGEGN")
          .test("[a-zA-Z]", "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
            "[\u00B5\u037A\u03D0-\uFFFF]", /* roundtrip exclusions */
            this, new LegalGreek(false));
    }

    public void Testel() throws IOException, ParseException {
        new Test("Latin-el")
          .test("[a-zA-Z]", "[\u003B\u00B7[:Greek:]-[\u03D7-\u03EF]]", 
            "[\u00B5\u037A\u03D0-\uFFFF]", /* roundtrip exclusions */
            this, new LegalGreek(false));
    }

    public void TestCyrillic() throws IOException, ParseException {
        new Test("Latin-Cyrillic")
          .test("[a-zA-Z\u0110\u0111]", "[\u0400-\u045F]", null, this, new Legal());
    }
    
    //----------------------------------
    // Inter-Indic Tests
    //----------------------------------
   public void TestDevanagariLatin() throws IOException, ParseException {
        new Test("Latin-DEVANAGARI")
          .test("[a-zA-Z]", "[:Devanagari:]", null, this, new Legal());
    }
    private static final String [][] array= new String[][]{
        new String [] {  "BENGALI-DEVANAGARI",
            "[:BENGALI:]", "[:Devanagari:]", 
                "[\u0951-\u0954\u0943-\u0949\u094a\u0962\u0963\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-BENGALI",
           "[:Devanagari:]", "[:BENGALI:]",
                  "[\u0951-\u0954\u09D7\u090D\u090e\u0911\u0912\u0929\u0933\u0934\u0935\u093d\u0950\u0958\u0959\u095a\u095b\u095e\u09f0\u09f1]", /*roundtrip exclusions*/
                  },

        new String [] {  "GURMUKHI-DEVANAGARI",
          "[:GURMUKHI:]", "[:Devanagari:]", 
                "[\u0936\u0933\u0951-\u0954\u0902\u0903\u0943-\u0949\u094a\u0962\u0963\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-GURMUKHI",
           "[:Devanagari:]", "[:GURMUKHI:]",
                  "[\u0946\u0A5C\u0951-\u0954\u0A70\u0A71\u090B\u090C\u090D\u090e\u0911\u0912\u0934\u0937\u093D\u0950\u0960\u0961\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                  },

        new String [] {  "GUJARATI-DEVANAGARI",
          "[:GUJARATI:]", "[:Devanagari:]", 
                "[\u0946\u094A\u0962\u0963\u0951-\u0954\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-GUJARATI",
           "[:Devanagari:]", "[:GUJARATI:]",
                  "[\u0951-\u0954\u0961\u090c\u090e\u0912]", /*roundtrip exclusions*/
                  },

        new String [] {  "ORIYA-DEVANAGARI",
          "[:ORIYA:]", "[:Devanagari:]", 
                "[\u0943-\u094a\u0962\u0963\u0951-\u0954\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-ORIYA",
           "[:Devanagari:]", "[:ORIYA:]",
                  "[\u0b5f\u0b56\u0b57\u0950\u090D\u090e\u0912\u0911\u0931\u0935]", /*roundtrip exclusions*/
                  },

        new String [] {  "Tamil-DEVANAGARI",
          "[:tamil:]", "[:Devanagari:]", 
                  "[\u093c\u0943-\u094a\u0951-\u0954\u0962\u0963\u090B\u090C\u090D\u0911\u0916\u0917\u0918\u091B\u091D\u0920\u0921\u0922\u0925\u0926\u0927\u092B\u092C\u092D\u0936\u093d\u0950[\u0958-\u0961]]", /*roundtrip exclusions*/
                  },
        new String [] {  "DEVANAGARI-Tamil",
           "[:Devanagari:]", "[:tamil:]",
                  "[\u0bd7]", /*roundtrip exclusions*/
                  },

        new String [] {  "Telugu-DEVANAGARI",
          "[:telugu:]", "[:Devanagari:]", 
                "[\u093c\u0950\u0945\u0949\u0951-\u0954\u0962\u0963\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-TELUGU",
           "[:Devanagari:]", "[:TELUGU:]",
                  "[\u0c55\u0c56\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-DEVANAGARI",
          "[:KANNADA:]", "[:Devanagari:]", 
                "[\u0946\u093c\u0950\u0945\u0949\u0951-\u0954\u0962\u0963\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-KANNADA",
           "[:Devanagari:]", "[:KANNADA:]",
                  "[\u0cde\u0cd5\u0cd6\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/ 
                  },

        new String [] {  "MALAYALAM-DEVANAGARI",
          "[:MALAYALAM:]", "[:Devanagari:]", 
                "[\u094a\u094b\u094c\u093c\u0950\u0944\u0945\u0949\u0951-\u0954\u0962\u0963\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                },
        new String [] {  "DEVANAGARI-MALAYALAM",
           "[:Devanagari:]", "[:MALAYALAM:]",
                  "[\u0d4c\u0d57\u0950\u090D\u0911\u093d\u0929\u0934[\u0958-\u095f]]", /*roundtrip exclusions*/
                  },

        new String [] {  "GURMUKHI-BENGALI",
          "[:GURMUKHI:]", "[:BENGALI:]",  
                "[\u09b6\u09e2\u09e3\u09c3\u09c4\u09d7\u098B\u098C\u09B7\u09E0\u09E1\u09F0\u09F1]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-GURMUKHI",
           "[:BENGALI:]", "[:GURMUKHI:]",
                  "[\u0a5c\u0a47\u0a70\u0a71\u0A33\u0A35\u0A59\u0A5A\u0A5B\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                  },

        new String [] {  "GUJARATI-BENGALI",
          "[:GUJARATI:]", "[:BENGALI:]", 
                "[\u09d7\u09e2\u09e3\u098c\u09e1\u09f0\u09f1]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-GUJARATI",
           "[:BENGALI:]", "[:GUJARATI:]",
                  "[\u0A82\u0a83\u0Ac9\u0Ac5\u0ac7\u0A8D\u0A91\u0AB3\u0AB5\u0ABD\u0AD0]", /*roundtrip exclusions*/
                  },
 
        new String [] {  "ORIYA-BENGALI",
          "[:ORIYA:]", "[:BENGALI:]", 
                "[\u09c4\u09e2\u09e3\u09f0\u09f1]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-ORIYA",
           "[:BENGALI:]", "[:ORIYA:]",
                  "[\u0b5f\u0b56\u0b33\u0b3d]", /*roundtrip exclusions*/
                  },

        new String [] {  "Tamil-BENGALI",
          "[:tamil:]", "[:BENGALI:]", 
                  "[\u09bc\u09c3\u09c4\u09e2\u09e3\u09f0\u09f1\u098B\u098C\u0996\u0997\u0998\u099B\u099D\u09A0\u09A1\u09A2\u09A5\u09A6\u09A7\u09AB\u09AC\u09AD\u09B6\u09DC\u09DD\u09DF\u09E0\u09E1]", /*roundtrip exclusions*/
                  },
        new String [] {  "BENGALI-Tamil",
           "[:BENGALI:]", "[:tamil:]",
                  "[\u0bc6\u0bc7\u0bca\u0B8E\u0B92\u0BA9\u0BB1\u0BB3\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  },

        new String [] {  "Telugu-BENGALI",
          "[:telugu:]", "[:BENGALI:]", 
                "[\u09e2\u09e3\u09bc\u09d7\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-TELUGU",
           "[:BENGALI:]", "[:TELUGU:]",
                  "[\u0c55\u0c56\u0c47\u0c46\u0c4a\u0C0E\u0C12\u0C31\u0C33\u0C35]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-BENGALI",
          "[:KANNADA:]", "[:BENGALI:]", 
                "[\u09e2\u09e3\u09bc\u09d7\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-KANNADA",
           "[:BENGALI:]", "[:KANNADA:]",
                  "[\u0cc6\u0cca\u0cd5\u0cd6\u0cc7\u0C8E\u0C92\u0CB1\u0cb3\u0cb5\u0cde]", /*roundtrip exclusions*/ 
                  },

        new String [] {  "MALAYALAM-BENGALI",
          "[:MALAYALAM:]", "[:BENGALI:]", 
                "[\u09e2\u09e3\u09bc\u09c4\u09f0\u09f1\u09dc\u09dd\u09df]", /*roundtrip exclusions*/
                },
        new String [] {  "BENGALI-MALAYALAM",
           "[:BENGALI:]", "[:MALAYALAM:]",
                  "[\u0d46\u0d4a\u0d47\u0d31-\u0d35\u0d0e\u0d12]", /*roundtrip exclusions*/
                  },

        new String [] {  "GUJARATI-GURMUKHI",
          "[:GUJARATI:]", "[:GURMUKHI:]", 
                "[\u0ab3\u0ab6\u0A70\u0a71\u0a82\u0a83\u0ac3\u0ac4\u0ac5\u0ac9\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0abd]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-GUJARATI",
           "[:GURMUKHI:]", "[:GUJARATI:]",
                  "[\u0ab3\u0ab6\u0A70\u0a71\u0a82\u0a83\u0ac3\u0ac4\u0ac5\u0ac9\u0a5c\u0a72\u0a73\u0a74\u0a8b\u0a8d\u0a91\u0ab7\u0abd\u0ad0\u0ae0]", /*roundtrip exclusions*/
                  },

        new String [] {  "ORIYA-GURMUKHI",
          "[:ORIYA:]", "[:GURMUKHI:]", 
                "[\u0a21\u0a47\u0a71\u0b02\u0b03\u0b33\u0b36\u0b43\u0b56\u0b57\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61\u0a35\u0a72\u0a73\u0a74]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-ORIYA",
           "[:GURMUKHI:]", "[:ORIYA:]",
                  "[\u0a71\u0b02\u0b03\u0b33\u0b36\u0b43\u0b56\u0b57\u0B0B\u0B0C\u0B37\u0B3D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                  },

        new String [] {  "TAMIL-GURMUKHI",
          "[:TAMIL:]", "[:GURMUKHI:]", 
                "[\u0a33\u0a36\u0a3c\u0a70\u0a71\u0a47\u0A16\u0A17\u0A18\u0A1B\u0A1D\u0A20\u0A21\u0A22\u0A25\u0A26\u0A27\u0A2B\u0A2C\u0A2D\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-TAMIL",
           "[:GURMUKHI:]", "[:TAMIL:]",
                  "[\u0bc6\u0bca\u0bd7\u0bb7\u0bb3\u0b83\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  },

        new String [] {  "TELUGU-GURMUKHI",
          "[:TELUGU:]", "[:GURMUKHI:]", 
                "[\u0a33\u0a36\u0a3c\u0a70\u0a71\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-TELUGU",
           "[:GURMUKHI:]", "[:TELUGU:]",
                  "[\u0c02\u0c03\u0c33\u0c36\u0c44\u0c43\u0c46\u0c4a\u0c56\u0c55\u0C0B\u0C0C\u0C0E\u0C12\u0C31\u0C37\u0C60\u0C61]", /*roundtrip exclusions*/
                  },
        new String [] {  "KANNADA-GURMUKHI",
          "[:KANNADA:]", "[:GURMUKHI:]", 
                "[\u0a33\u0a36\u0a3c\u0a70\u0a71\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-KANNADA",
           "[:GURMUKHI:]", "[:KANNADA:]",
                  "[\u0c83\u0cb3\u0cb6\u0cc4\u0cc3\u0cc6\u0cca\u0cd5\u0cd6\u0C8B\u0C8C\u0C8E\u0C92\u0CB1\u0CB7\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-GURMUKHI",
          "[:MALAYALAM:]", "[:GURMUKHI:]", 
                "[\u0a4b\u0a4c\u0a33\u0a36\u0a3c\u0a70\u0a71\u0A59\u0A5A\u0A5B\u0A5C\u0A5E\u0A72\u0A73\u0A74]", /*roundtrip exclusions*/
                },
        new String [] {  "GURMUKHI-MALAYALAM",
           "[:GURMUKHI:]", "[:MALAYALAM:]",
                  "[\u0d03\u0d33\u0d36\u0d43\u0d46\u0d4a\u0d4c\u0d57\u0D0B\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D37\u0D60\u0D61]", /*roundtrip exclusions*/
                  },

        new String [] {  "GUJARATI-ORIYA",
          "[:GUJARATI:]", "[:ORIYA:]", 
                "[\u0b56\u0b57\u0B0C\u0B5F\u0B61]", /*roundtrip exclusions*/
                },
        new String [] {  "ORIYA-GUJARATI",
           "[:ORIYA:]", "[:GUJARATI:]",
                  "[\u0Ac4\u0Ac5\u0Ac9\u0Ac7\u0A8D\u0A91\u0AB5\u0Ad0]", /*roundtrip exclusions*/
                  },

        new String [] {  "TAMIL-GUJARATI",
          "[:TAMIL:]", "[:GUJARATI:]", 
                "[\u0abc\u0ac3\u0Ac4\u0Ac5\u0Ac9\u0Ac7\u0A8B\u0A8D\u0A91\u0A96\u0A97\u0A98\u0A9B\u0A9D\u0AA0\u0AA1\u0AA2\u0AA5\u0AA6\u0AA7\u0AAB\u0AAC\u0AAD\u0AB6\u0ABD\u0AD0\u0AE0]", /*roundtrip exclusions*/
                },
        new String [] {  "GUJARATI-TAMIL",
           "[:GUJARATI:]", "[:TAMIL:]",
                  "[\u0Bc6\u0Bca\u0Bd7\u0B8E\u0B92\u0BA9\u0BB1\u0BB4]", /*roundtrip exclusions*/
                  },

        new String [] {  "TELUGU-GUJARATI",
          "[:TELUGU:]", "[:GUJARATI:]", 
                "[\u0abc\u0Ac5\u0Ac9\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                },
        new String [] {  "GUJARATI-TELUGU",
           "[:GUJARATI:]", "[:TELUGU:]",
                  "[\u0c46\u0c4a\u0c55\u0c56\u0C0C\u0C0E\u0C12\u0C31\u0C61]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-GUJARATI",
          "[:KANNADA:]", "[:GUJARATI:]", 
                "[\u0abc\u0Ac5\u0Ac9\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                },
        new String [] {  "GUJARATI-KANNADA",
           "[:GUJARATI:]", "[:KANNADA:]",
                  "[\u0cc6\u0cca\u0cd5\u0cd6\u0C8C\u0C8E\u0C92\u0CB1\u0CDE\u0CE1]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-GUJARATI",
          "[:MALAYALAM:]", "[:GUJARATI:]", 
                "[\u0ac4\u0acb\u0acc\u0abc\u0Ac5\u0Ac9\u0A8D\u0A91\u0ABD\u0Ad0]", /*roundtrip exclusions*/
                },
        new String [] {  "GUJARATI-MALAYALAM",
           "[:GUJARATI:]", "[:MALAYALAM:]",
                  "[\u0d46\u0d4a\u0d4c\u0d55\u0d57\u0D0C\u0D0E\u0D12\u0D31\u0D34\u0D61]", /*roundtrip exclusions*/
                  },

        new String [] {  "TAMIL-ORIYA",
          "[:TAMIL:]", "[:ORIYA:]", 
                "[\u0b3c\u0b43\u0b56\u0B0B\u0B0C\u0B16\u0B17\u0B18\u0B1B\u0B1D\u0B20\u0B21\u0B22\u0B25\u0B26\u0B27\u0B2B\u0B2C\u0B2D\u0B36\u0B3D\u0B5C\u0B5D\u0B5F\u0B60\u0B61]", /*roundtrip exclusions*/
                },
        new String [] {  "ORIYA-TAMIL",
           "[:ORIYA:]", "[:TAMIL:]",
                  "[\u0bc6\u0bca\u0bc7\u0B8E\u0B92\u0BA9\u0BB1\u0BB4\u0BB5]", /*roundtrip exclusions*/
                  },

        new String [] {  "TELUGU-ORIYA",
          "[:TELUGU:]", "[:ORIYA:]", 
                "[\u0b3c\u0b57\u0b56\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                },
        new String [] {  "ORIYA-TELUGU",
           "[:ORIYA:]", "[:TELUGU:]",
                  "[\u0c44\u0c46\u0c4a\u0c55\u0c47\u0C0E\u0C12\u0C31\u0C35]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-ORIYA",
          "[:KANNADA:]", "[:ORIYA:]", 
                "[\u0b3c\u0b57\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                },
        new String [] {  "ORIYA-KANNADA",
           "[:ORIYA:]", "[:KANNADA:]",
                  "[\u0cc4\u0cc6\u0cca\u0cd5\u0cc7\u0C8E\u0C92\u0CB1\u0CB5\u0CDE]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-ORIYA",
          "[:MALAYALAM:]", "[:ORIYA:]", 
                "[\u0b3c\u0b56\u0B3D\u0B5C\u0B5D\u0B5F]", /*roundtrip exclusions*/
                },
        new String [] {  "ORIYA-MALAYALAM",
           "[:ORIYA:]", "[:MALAYALAM:]",
                  "[\u0D47\u0D46\u0D4a\u0D0E\u0D12\u0D31\u0D34\u0D35]", /*roundtrip exclusions*/
                  },

        new String [] {  "TELUGU-TAMIL",
          "[:TELUGU:]", "[:TAMIL:]", 
                "[\u0bd7\u0ba9\u0bb4]", /*roundtrip exclusions*/
                },
        new String [] {  "TAMIL-TELUGU",
           "[:TAMIL:]", "[:TELUGU:]",
                  "[\u0c43\u0c44\u0c46\u0c47\u0c55\u0c56\u0c66\u0C0B\u0C0C\u0C16\u0C17\u0C18\u0C1B\u0C1D\u0C20\u0C21\u0C22\u0C25\u0C26\u0C27\u0C2B\u0C2C\u0C2D\u0C36\u0C60\u0C61]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-TAMIL",
          "[:KANNADA:]", "[:TAMIL:]", 
                "[\u0bd7\u0bc6\u0ba9\u0bb4]", /*roundtrip exclusions*/
                },
        new String [] {  "TAMIL-KANNADA",
           "[:TAMIL:]", "[:KANNADA:]",
                  "[\u0cc3\u0cc4\u0cc6\u0cc7\u0cd5\u0cd6\u0C8B\u0C8C\u0C96\u0C97\u0C98\u0C9B\u0C9D\u0CA0\u0CA1\u0CA2\u0CA5\u0CA6\u0CA7\u0CAB\u0CAC\u0CAD\u0CB6\u0CDE\u0CE0\u0CE1]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-TAMIL",
          "[:MALAYALAM:]", "[:TAMIL:]", 
                "[\u0ba9]", /*roundtrip exclusions*/
                },
        new String [] {  "TAMIL-MALAYALAM",
           "[:TAMIL:]", "[:MALAYALAM:]",
                  "[\u0d43\u0d12\u0D0B\u0D0C\u0D16\u0D17\u0D18\u0D1B\u0D1D\u0D20\u0D21\u0D22\u0D25\u0D26\u0D27\u0D2B\u0D2C\u0D2D\u0D36\u0D60\u0D61]", /*roundtrip exclusions*/
                  },

        new String [] {  "KANNADA-TELUGU",
          "[:KANNADA:]", "[:TELUGU:]", 
                "[\u0c3f\u0c46\u0c48\u0c4a]", /*roundtrip exclusions*/
                },
        new String [] {  "TELUGU-KANNADA",
           "[:TELUGU:]", "[:KANNADA:]",
                  "[\u0cc8\u0cd5\u0cd6\u0CDE]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-TELUGU",
          "[:MALAYALAM:]", "[:TELUGU:]", 
                "[\u0c44\u0c4a\u0c4c\u0c4b\u0c55\u0c56]", /*roundtrip exclusions*/
                },
        new String [] {  "TELUGU-MALAYALAM",
           "[:TELUGU:]", "[:MALAYALAM:]",
                  "[\u0d4c\u0d57\u0D34]", /*roundtrip exclusions*/
                  },

        new String [] {  "MALAYALAM-KANNADA",
          "[:MALAYALAM:]", "[:KANNADA:]", 
                "[\u0cc4\u0cc6\u0cca\u0ccc\u0ccb\u0cd5\u0cd6\u0cDe]", /*roundtrip exclusions*/
                },
        new String [] {  "KANNADA-MALAYALAM",
           "[:KANNADA:]", "[:MALAYALAM:]",
                  "[\u0d4c\u0d57\u0d46\u0D34]", /*roundtrip exclusions*/
                  },
    };

    public void TestInterIndic() throws Exception{
        int num = array.length;
        if (isQuick()) {
            logln("Testing only 5 of "+ array.length+" Skipping rest (use -e for exhaustive)");
            num = 5;
        }
        for(int i=0; i<num;i++){
           logln("Testing " + array[i][0] );
           new Test(array[i][0])
                .test(array[i][1], array[i][2], 
                array[i][3],
                this, new Legal());
        }
    }
    //---------------
    // End Indic
    //---------------
    
    public static class Legal {
        public boolean is(String sourceString) {return true;}
    }
    
    public static class LegalJamo extends Legal {
        // any initial must be followed by a medial (or initial)
        // any medial must follow an initial (or medial)
        // any final must follow a medial (or final)
        
        public boolean is(String sourceString) {
            try {
                int t;
                String decomp = Normalizer.normalize(sourceString, Normalizer.DECOMP, 0);
                for (int i = 0; i < decomp.length(); ++i) { // don't worry about surrogates
                    switch (getType(decomp.charAt(i))) {
                    case 0:
                        t = getType(decomp.charAt(i+1));
                        if (t != 0 && t != 1) return false;
                        break;
                    case 1:
                        t = getType(decomp.charAt(i-1));
                        if (t != 0 && t != 1) return false;
                        break;
                    case 2:
                        t = getType(decomp.charAt(i-1));
                        if (t != 1 && t != 2) return false;
                        break;
                    }
                }
                return true;
            } catch (StringIndexOutOfBoundsException e) {
                return false;
            }
        }
        
        public int getType(char c) {
            if ('\u1100' <= c && c <= '\u1112') return 0;
            else if ('\u1161' <= c && c  <= '\u1175') return 1;
            else if ('\u11A8' <= c && c  <= '\u11C2') return 2;
            return -1; // other
        }
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
        
        RoundTripTest log;
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
                         RoundTripTest log, Legal legalSource) 
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
            if (log.isQuick()) log.logln("Abbreviated Test");
            
            badCharacters = new UnicodeSet("[:other:]");

            // make a UTF-8 output file we can read with a browser

            // note: check that every transliterator transliterates the null string correctly!

            String logFileName = "test_" + transliteratorID.replace('/', '_') + ".html";

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
        
        // ok if at least one is not equal
        public boolean checkIrrelevants(Transliterator t, String irrelevants) {
            for (int i = 0; i < irrelevants.length(); ++i) {
                char c = irrelevants.charAt(i);
                String cs = UTF16.valueOf(c);
                String targ = t.transliterate(cs);
                if (cs.equals(targ)) return true;
            }
            return false;
        }

        public void test2() {

            Transliterator sourceToTarget = Transliterator.getInstance(transliteratorID);
            Transliterator targetToSource = sourceToTarget.getInverse();
            UnicodeSetIterator usi = new UnicodeSetIterator();
            UnicodeSetIterator usi2 = new UnicodeSetIterator();
            
            log.logln("Checking that at least one irrevant characters is not NFC'ed");
                
            String irrelevants = "\u2000\u2001\u2126\u212A\u212B\u2329"; // string is from NFC_NO in the UCD
                
            if (!checkIrrelevants(sourceToTarget, irrelevants)) {
                logFails("Source-Target, irrelevants");
            }
            if (!checkIrrelevants(targetToSource, irrelevants)) {
                logFails("Target-Source, irrelevants");
            }
            
            if (EXTRA_TESTS) {
                log.logln("Checking that toRules works");
                String rules = "";
                Transliterator sourceToTarget2;
                Transliterator targetToSource2;
                try {
                    rules = sourceToTarget.toRules(false);
                    sourceToTarget2 = Transliterator.createFromRules("s2t2", rules, Transliterator.FORWARD);
                    if (PRINT_RULES) {
                        out.println("<h3>Forward Rules:</h3><p>");
                        out.println(TestUtility.replace(rules, "\n", "<br>\n"));
                        out.println("</p>");
                    }
                    rules = targetToSource.toRules(false);
                    targetToSource2 = Transliterator.createFromRules("t2s2", rules, Transliterator.FORWARD);
                    if (PRINT_RULES) {
                        out.println("<h3>Backward Rules:</h3><p>");
                        out.println(TestUtility.replace(rules, "\n", "<br>\n"));
                        out.println("</p>");
                    }
                } catch (RuntimeException e) {
                    out.println("<h3>Broken Rules:</h3><p>");
                    out.println(TestUtility.replace(rules, "\n", "<br>\n"));
                    out.println("</p>");
                    out.flush();
                    throw e;
                }
                
                usi.reset(sourceRange);
                while (true) {
                    int c = usi.next();
                    if (c < 0) break;
                    
                    String cs = UTF16.valueOf(c);
                    String targ = sourceToTarget.transliterate(cs);
                    String targ2 = sourceToTarget2.transliterate(cs);
                    if (!targ.equals(targ2)) {
                        logToRulesFails("Source-Target, toRules", cs, targ, targ2);
                    }
                }
                
                usi.reset(targetRange);
                while (true) {
                    int c = usi.next();
                    if (c < 0) break;
                    
                    String cs = UTF16.valueOf(c);
                    String targ = targetToSource.transliterate(cs);
                    String targ2 = targetToSource2.transliterate(cs);
                    if (!targ.equals(targ2)) {
                        logToRulesFails("Target-Source, toRules", cs, targ, targ2);
                    }
                }
            }
            

            log.logln("Checking that source characters convert to target - Singles");
            
            UnicodeSet failSourceTarg = new UnicodeSet();

            /*
            for (char c = 0; c < 0xFFFF; ++c) {
                if (!sourceRange.contains(c)) continue;
                */
            usi.reset(sourceRange);
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
            
            usi.reset(sourceRangeMinusFailures, log.isQuick());
            while (true) {
                int c = usi.next();
                if (c < 0) break;
             
                /*
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !sourceRange.contains(d)) continue;
                    if (failSourceTarg.get(d)) continue;
                */
                usi2.reset(sourceRangeMinusFailures, log.isQuick());
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
            
            usi.reset(targetRangeMinusFailures, log.isQuick());

            while (true) {
                int c = usi.next();
                if (c < 0) break;
                
                if (++count > pairLimit) {
                    throw new TestTruncated("Test truncated at " + pairLimit + " x 64k pairs");
                }
                log.log(TestUtility.hex(c));
                
                /*
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !targetRange.contains(d)) continue;
                        */
                usi2.reset(targetRangeMinusFailures, log.isQuick());
                while (true) {
                    int d = usi2.next();
                    if (d < 0) break;
                    
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
        
        final void logFails(String label) {
            if (++errorCount > errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
            out.println("<br>Fail (can.equiv)" + label);
        }

        final void logToRulesFails(String label, String from, String to, String toCan) {
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
