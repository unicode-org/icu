package com.ibm.test.translit;
import com.ibm.test.*;
import com.ibm.text.*;
import com.ibm.util.Utility;
import java.io.*;
import java.text.ParseException;

/**
 * @test
 * @summary Round trip test of Transliterator
 */
public class RoundTripTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new RoundTripTest().run(args);
    }

    public void TestHiragana() throws IOException, ParseException {
        new Test("Latin-Kana", 
          TestUtility.LATIN_SCRIPT, TestUtility.HIRAGANA_SCRIPT)
          .test("[a-z]", "[\u3040-\u3094]", this);
    }

    public void TestKatakana() throws IOException, ParseException {
        new Test("Latin-Kana", 
          TestUtility.LATIN_SCRIPT, TestUtility.KATAKANA_SCRIPT)
          .test("[A-Z]", "[\u30A1-\u30FA]", this);
    }

    public void TestArabic() throws IOException, ParseException {
        new Test("Latin-Arabic", 
          TestUtility.LATIN_SCRIPT, TestUtility.ARABIC_SCRIPT)
          .test("[a-z]", "[\u0620-\u065F-[\u0640]]", this);
    }

    public void TestHebrew() throws IOException, ParseException {
        new Test("Latin-Hebrew", 
          TestUtility.LATIN_SCRIPT, TestUtility.HEBREW_SCRIPT)
          .test(null, "[\u05D0-\u05EF]", this);
    }

    public void TestHangul() throws IOException, ParseException {
        Test t = new TestHangul();
        t.setPairLimit(30); // Don't run full test -- too long
        t.test(null, null, this);
    }

    public void TestJamo() throws IOException, ParseException {
        Test t = new Test("Latin-Jamo", 
          TestUtility.LATIN_SCRIPT, TestUtility.JAMO_SCRIPT);
        t.setErrorLimit(200); // Don't run full test -- too long
        //t.test("[[a-z]-[fqvxz]]", null, this);
        t.test("[a-z]", null, this);
    }

    public void TestJamoHangul() throws IOException, ParseException {
        Test t = new Test("Latin-Hangul", 
          TestUtility.LATIN_SCRIPT, TestUtility.HANGUL_SCRIPT);
        t.setErrorLimit(50); // Don't run full test -- too long
        t.test("[a-z]", null, this);
    }

    public void TestGreek() throws IOException, ParseException {
        new Test("Latin-Greek", 
          TestUtility.LATIN_SCRIPT, TestUtility.GREEK_SCRIPT)
          .test(null, "[\u0380-\u03CF]", this);
    }

    public void TestCyrillic() throws IOException, ParseException {
        new Test("Latin-Cyrillic", 
          TestUtility.LATIN_SCRIPT, TestUtility.CYRILLIC_SCRIPT)
          .test(null, "[\u0401\u0410-\u044F\u0451]", this);
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
        TestLog log;
    
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
      
        public void test(String sourceRange, String targetRange, TestLog log) 
            throws java.io.IOException, java.text.ParseException {
      
            if (sourceRange != null && sourceRange.length() > 0) {
                this.sourceRange = new UnicodeSet(sourceRange);
            }
            if (targetRange != null && targetRange.length() > 0) {
                this.targetRange = new UnicodeSet(targetRange);
            }

            if (this.sourceRange == null) this.sourceRange = new UnicodeSet("[a-zA-Z]");

            this.log = log;

            log.logln(Utility.escape("Source: " + this.sourceRange));
            log.logln(Utility.escape("Target: " + this.targetRange));

            // make a UTF-8 output file we can read with a browser

            // note: check that every transliterator transliterates the null string correctly!

            String logFileName = "test_" + transliteratorID + "_"
                + sourceScript + "_" + targetScript + ".html";

            log.logln("Creating log file " + logFileName);

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                      new FileOutputStream(logFileName), "UTF8"), 4*1024));
            //out.write('\uFFEF');    // BOM
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<HTML><HEAD>");
            out.println("<META content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
            out.println("<BODY>");
            out.println("<TABLE>");
            try {
                test2();
                out.println("</TABLE>");
            } catch (TestTruncated e) {
                out.println("</TABLE>" + e.getMessage());
            }
            out.println("</BODY></HTML>");
            out.close();

            if (errorCount > 0) {
                log.errln(transliteratorID + " errors: " + errorCount + ", see " + logFileName);
            } else {
                log.logln(transliteratorID + " ok");
                new File(logFileName).delete();
            }
        }

        public void test2() {

            Transliterator sourceToTarget = Transliterator.getInstance(transliteratorID);
            Transliterator targetToSource = sourceToTarget.getInverse();

            log.logln("Checking that source characters convert to target - Singles");

            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !isSource(c)) continue;
                String cs = String.valueOf(c);
                String targ = sourceToTarget.transliterate(String.valueOf(cs));
                if (!isReceivingTarget(targ)) {
                    logWrongScript("Source-Target", cs, targ);
                }
            }

            log.logln("Checking that source characters convert to target - Doubles");

            for (char c = 0; c < 0xFFFF; ++c) { 
                if (TestUtility.isUnassigned(c) ||
                    !isSource(c)) continue;
                for (char d = 0; d < 0xFFFF; ++d) {
                    if (TestUtility.isUnassigned(d) ||
                        !isSource(d)) continue;
                    String cs = String.valueOf(c) + d;
                    String targ = sourceToTarget.transliterate(cs);
                    if (!isReceivingTarget(targ)) {
                        logWrongScript("Source-Target", cs, targ);
                    }
                }
            }

            log.logln("Checking that target characters convert to source and back - Singles");

            for (char c = 0; c < 0xFFFF; ++c) {
                if (TestUtility.isUnassigned(c) ||
                    !isTarget(c)) continue;
                String cs = String.valueOf(c);
                String targ = targetToSource.transliterate(cs);
                String reverse = sourceToTarget.transliterate(targ);
                if (!isReceivingSource(targ)) {
                    logWrongScript("Target-Source", cs, targ);
                } else if (!cs.equals(reverse)) {
                    logRoundTripFailure(cs, targ, reverse);
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
                    if (!isReceivingSource(targ)) {
                        logWrongScript("Target-Source", cs, targ);
                    } else if (!cs.equals(reverse)) {
                        logRoundTripFailure(cs, targ, reverse);
                    }
                }
            }
            log.logln("");
        }

        final void logWrongScript(String label, String from, String to) {
            out.println("<TR><TD>Fail " + label + ":</TD><TD><FONT SIZE=\"6\">" +
                        from + "</FONT></TD><TD>(" +
                        TestUtility.hex(from) + ") =></TD><TD><FONT SIZE=\"6\">" +
                        to + "</FONT></TD><TD>(" +
                        TestUtility.hex(to) + ")</TD></TR>" );
            if (++errorCount >= errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
        }

        final void logRoundTripFailure(String from, String to, String back) {
            out.println("<TR><TD>Fail Roundtrip:</TD><TD><FONT SIZE=\"6\">" +
                        from + "</FONT></TD><TD>(" +
                        TestUtility.hex(from) + ") =></TD><TD>" +
                        to + "</TD><TD>(" +
                        TestUtility.hex(to) + ") =></TD><TD><FONT SIZE=\"6\">" +
                        back + "</TD><TD>(" +
                        TestUtility.hex(back) + ")</TD></TR>" );
            if (++errorCount >= errorLimit) {
                throw new TestTruncated("Test truncated; too many failures");
            }
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

    static class TestHangul extends Test {
        TestHangul () {
            super("Jamo-Hangul", TestUtility.JAMO_SCRIPT, TestUtility.HANGUL_SCRIPT);
        }
  
        public boolean isSource(char c) {
            if (0x1113 <= c && c <= 0x1160) return false;
            if (0x1176 <= c && c <= 0x11F9) return false;
            if (0x3131 <= c && c <= 0x318E) return false;
            return super.isSource(c);
        }
    }
}
