package com.ibm.test.translit;
import com.ibm.test.*;
import com.ibm.text.*;
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

    public void TestRoundTrip() throws IOException, ParseException {
        Test t;

        // Test Hiragana
        new Test("Latin-Kana", 
          TestUtility.LATIN_SCRIPT, TestUtility.HIRAGANA_SCRIPT)
          .test("[a-z]", "[\u3040-\u3094]", this);

        // Test Katakana
        new Test("Latin-Kana", 
          TestUtility.LATIN_SCRIPT, TestUtility.KATAKANA_SCRIPT)
          .test("[A-Z]", "[\u30A1-\u30FA]", this);

        // Test Arabic
        new Test("Latin-Arabic", 
          TestUtility.LATIN_SCRIPT, TestUtility.ARABIC_SCRIPT)
          .test(null, "[\u0620-\u065F-[\u0640]]", this);

        // Test Hebrew
        new Test("Latin-Hebrew", 
          TestUtility.LATIN_SCRIPT, TestUtility.HEBREW_SCRIPT)
          .test(null, "[\u05D0-\u05EF]", this);

        // Test Hangul
        t = new TestHangul();
        t.setPairLimit(30); // Don't run full test -- too long
        t.test(null, null, this);
                                                                    
        // Test Jamo
        t = new Test("Latin-Jamo", 
          TestUtility.LATIN_SCRIPT, TestUtility.JAMO_SCRIPT);
        t.setErrorLimit(100);
        t.test(null, null, this);
                                                                    
        // Test JamoHangul
        t = new Test("Latin-Jamo;Jamo-Hangul", 
          TestUtility.LATIN_SCRIPT, TestUtility.HANGUL_SCRIPT);
        t.setErrorLimit(100);
        t.test(null, null, this);

        // Test Greek
        new Test("Latin-Greek", 
          TestUtility.LATIN_SCRIPT, TestUtility.GREEK_SCRIPT)
          .test(null, "[\u0380-\u03CF]", this);

        // Test Cyrillic
        new Test("Latin-Cyrillic", 
          TestUtility.LATIN_SCRIPT, TestUtility.CYRILLIC_SCRIPT)
          .test(null, "[\u0401\u0410-\u0451]", this);

        // Test Utility
        // TestUtility.test(); // dump blocks and scripts for debugging
    }

    static class Test {
    
    PrintWriter out;
    
    private String transliteratorID; 
    private byte sourceScript;
    private byte targetScript;
    private boolean showProgress = true;
    private boolean showSuccess = false;
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

          if (this.sourceRange == null) this.sourceRange = new UnicodeSet("[a-Z]");

          this.log = log;

            // make a UTF-8 output file we can read with a browser

            // note: check that every transliterator transliterates the null string correctly!

            String logFileName = "test_" + transliteratorID + "_"
            + sourceScript + "_" + targetScript + ".html";

            log.logln("Creating log file " + logFileName);

            out = new PrintWriter(
                new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(logFileName),
                    "UTF8"),
                4*1024));
            //out.write('\uFFEF');    // BOM
            out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
            out.println("<HTML><HEAD>");
            out.println("<META content=\"text/html; charset=utf-8\" http-equiv=Content-Type></HEAD>");
            out.println("<BODY>");
            out.println("<TABLE>");
            test2();
            out.println("</TABLE></BODY></HTML>");
            out.close();

            if (errorCount > 0) {
                log.errln(transliteratorID + " errors: " + errorCount);
            } else {
                log.logln(transliteratorID + " ok");
                new File(logFileName).delete();
            }
       }

        public void test2() {
            int count = 0;

            Transliterator sourceToTarget = Transliterator.getInstance(transliteratorID);
            Transliterator targetToSource = sourceToTarget.getInverse();

            log.logln("Checking that all source characters convert to target - Singles");

            // check single letters
            for (char c = 0; c < 0xFFFF; ++c) {
              if (Character.getType(c) == Character.UNASSIGNED) continue;
              if (!isSource(c)) continue;
              //if (showProgress && (count++ % 100) == 0) {
              //  log.logln(count + ": " + TestUtility.hex(c));
              //}
              String cs = String.valueOf(c);
              String targ = sourceToTarget.transliterate(String.valueOf(cs));
              if (!isReceivingTarget(targ)) {
                out.println("<TR><TD>Fail Source-Target: " + cs + "(" + TestUtility.hex(cs) + ")" 
                  + " => " + targ + "(" + TestUtility.hex(targ) + ")</TD></TR>");
                if (++errorCount > errorLimit) return;
              } else if (showSuccess) {
                out.println(c + "(" + TestUtility.hex(cs) + ")" 
                  + " => " + targ + "(" + TestUtility.hex(targ) + ")");
              }
            }

            log.logln("Checking that all source characters convert to target - Doubles");
            count = 0;

            for (char c = 0; c < 0xFFFF; ++c) {
              if (Character.getType(c) == Character.UNASSIGNED) continue;
              if (!isSource(c)) continue;
              for (char d = 0; d < 0xFFFF; ++d) {
                if (Character.getType(d) == Character.UNASSIGNED) continue;
                if (!isSource(d)) continue;
                String cs = String.valueOf(c) + d;
                //if (showProgress && (count++ % 1000) == 0) {
                //  log.logln(count + ": " + TestUtility.hex(cs));
                //}
                String targ = sourceToTarget.transliterate(cs);
                if (!isReceivingTarget(targ)) {
                  out.println("<TR><TD>Fail Source-Target: " + cs + "(" + TestUtility.hex(cs) + ")" 
                    + " => " + targ + "(" + TestUtility.hex(targ) + ")</TR></TD>");
                  if (++errorCount > errorLimit) return;
                } else if (showSuccess) {
                  out.println(c + "(" + TestUtility.hex(cs) + ")" 
                    + " => " + targ + "(" + TestUtility.hex(targ) + ")");
                }
              }
            }

            log.logln("Checking that target characters convert to source and back - Singles");
            count = 0;

            for (char c = 0; c < 0xFFFF; ++c) {
              if (Character.getType(c) == Character.UNASSIGNED) continue;
              if (!isTarget(c)) continue;
              //if (showProgress && (count++ % 100) == 0) {
              //  log.logln(count + ": " + TestUtility.hex(c));
              //}
              String cs = String.valueOf(c);
              if (c > 0x0400) {
                cs = cs + "";
              }
              String targ = targetToSource.transliterate(cs);
              String reverse = sourceToTarget.transliterate(targ);
              if (!isReceivingSource(targ)) {
                out.println("<TR><TD>Fail Target-Source: " + cs + "(" + TestUtility.hex(cs) + ")" 
                + " => " + targ + "(" + TestUtility.hex(targ) + ")</TR></TD>" );
                if (++errorCount > errorLimit) return;
              } else if (!cs.equals(reverse)) {
                  out.println("<TR><TD>Fail Roundtrip:</TD><TD><FONT SIZE=\"6\">" +
                              cs + "</FONT></TD><TD>(" +
                              TestUtility.hex(cs) + ") =></TD><TD>" +
                              targ + "</TD><TD>(" +
                              TestUtility.hex(targ) + ") =></TD><TD><FONT SIZE=\"6\">" +
                              reverse + "</TD><TD>(" +
                              TestUtility.hex(reverse) + ")</TD></TR>" );
                if (++errorCount > errorLimit) return;
              } else if (showSuccess) {
                out.println(cs + "(" + TestUtility.hex(cs) + ")" 
                + " => " + targ + "(" + TestUtility.hex(targ) + ")" 
                + " => " + reverse + "(" + TestUtility.hex(reverse) + ")" );
              }
            }

            log.logln("Checking that target characters convert to source and back - Doubles");
            count = 0;

            StringBuffer buf = new StringBuffer("aa");
            for (char c = 0; c < 0xFFFF; ++c) {
              if (Character.getType(c) == Character.UNASSIGNED) continue;
              if (!isTarget(c)) continue;
              if (++count > pairLimit) {
                  out.println("<TR><TD>Test truncated at " + pairLimit + " x 64k pairs</TR></TD>");
                  break;
              }
              buf.setCharAt(0, c);
              if (showProgress) { // && (count++ % 10000) == 0) {
                  log.log(TestUtility.hex(c));
                  // count + ": " + TestUtility.hex(cs));
              }
              for (char d = 0; d < 0xFFFF; ++d) {
                if (Character.getType(d) == Character.UNASSIGNED) continue;
                if (!isTarget(d)) continue;
                buf.setCharAt(1, d);
                String cs = buf.toString();
                String targ = targetToSource.transliterate(cs);
                String reverse = sourceToTarget.transliterate(targ);
                if (!isReceivingSource(targ)) {
                  out.println("<TR><TD>Fail Target-Source: " + cs + "(" + TestUtility.hex(cs) + ")" 
                  + " => " + targ + "(" + TestUtility.hex(targ) + ")</TR></TD>" );
                  if (++errorCount > errorLimit) return;
                } else if (!cs.equals(reverse)) {
                  out.println("<TR><TD>Fail Roundtrip:</TD><TD><FONT SIZE=\"6\">" +
                              cs + "</FONT></TD><TD>(" +
                              TestUtility.hex(cs) + ") =></TD><TD>" +
                              targ + "</TD><TD>(" +
                              TestUtility.hex(targ) + ") =></TD><TD><FONT SIZE=\"6\">" +
                              reverse + "</TD><TD>(" +
                              TestUtility.hex(reverse) + ")</TD></TR>" );
                  if (++errorCount > errorLimit) return;
                } else if (showSuccess) {
                  out.println(cs + "(" + TestUtility.hex(cs) + ")" 
                  + " => " + targ + "(" + TestUtility.hex(targ) + ")" 
                  + " => " + reverse + "(" + TestUtility.hex(reverse) + ")" );
                }
              }
            }
            if (showProgress) log.logln("");
        }

      /*
       * Characters to filter for source-target mapping completeness
       * Typically is base alphabet, minus extended characters
       * Default is ASCII letters for Latin
       */
      public boolean isSource(char c) {
        byte script = TestUtility.getScript(c);
        if (script != sourceScript) return false;
        if (!Character.isLetter(c)) return false;
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
        if (!Character.isLetter(c)) return false;
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
