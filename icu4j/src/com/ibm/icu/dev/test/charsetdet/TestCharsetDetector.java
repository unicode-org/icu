//##header
/**
 *******************************************************************************
 * Copyright (C) 2005-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.charsetdet;

import java.io.ByteArrayInputStream;
import java.io.Reader;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
//#endif

import com.ibm.icu.charset.CharsetProviderICU;
import java.nio.charset.CharsetEncoder;
import java.nio.CharBuffer;

/**
 * @author andy
 */
public class TestCharsetDetector extends TestFmwk
{
    
    /**
     * Constructor
     */
    public TestCharsetDetector()
    {
    }

    public static void main(String[] args) {
        try
        {
            TestCharsetDetector test = new TestCharsetDetector();
            test.run(args);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void CheckAssert(boolean exp) {
        if (exp == false) {
            String msg;
            try {
                throw new Exception();
            }
            catch (Exception e) {
//#if defined(FOUNDATION10) || defined(J2SE13)
//##           msg = "Test failure  " + e.getMessage() ;
//#else
                StackTraceElement failPoint = e.getStackTrace()[1];
                msg = "Test failure in file " + failPoint.getFileName() +
                             " at line " + failPoint.getLineNumber();
//#endif
            }
            errln(msg);
        }
        
    }
    
    private String stringFromReader(Reader reader)
    {
        StringBuffer sb = new StringBuffer();
        char[] buffer   = new char[1024];
        int bytesRead   = 0;
        
        try {
            while ((bytesRead = reader.read(buffer, 0, 1024)) >= 0) {
                sb.append(buffer, 0, bytesRead);
            }
            
            return sb.toString();
        } catch (Exception e) {
            errln("stringFromReader() failed: " + e.toString());
            return null;
        }
    }
    
    public void TestConstruction() {
        int i;
        CharsetDetector  det = new CharsetDetector();
        if(det==null){
            errln("Could not construct a charset detector");
        }
        String [] charsetNames = CharsetDetector.getAllDetectableCharsets();
        CheckAssert(charsetNames.length != 0);
        for (i=0; i<charsetNames.length; i++) {
            CheckAssert(charsetNames[i].equals("") == false); 
            // System.out.println("\"" + charsetNames[i] + "\"");
        }
     }

    public void TestInputFilter() throws Exception
    {
        String s = "<a> <lot> <of> <English> <inside> <the> <markup> Un tr\u00E8s petit peu de Fran\u00E7ais. <to> <confuse> <the> <detector>";
        byte[] bytes = s.getBytes("ISO-8859-1");
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        
        det.enableInputFilter(true);
        if (!det.inputFilterEnabled()){
            errln("input filter should be enabled");
        }
        
        det.setText(bytes);
        m = det.detect();
        
        if (! m.getLanguage().equals("fr")) {
            errln("input filter did not strip markup!");
        }
        
        det.enableInputFilter(false);
        det.setText(bytes);
        m = det.detect();
        
        if (! m.getLanguage().equals("en")) {
            errln("unfiltered input did not detect as English!");
        }
    }
    
    public void TestUTF8() throws Exception {
        
        String  s = "This is a string with some non-ascii characters that will " +
                    "be converted to UTF-8, then shoved through the detection process.  " +
                    "\u0391\u0392\u0393\u0394\u0395" +
                    "Sure would be nice if our source could contain Unicode directly!";
        byte [] bytes = s.getBytes("UTF-8");
        CharsetDetector det = new CharsetDetector();
        String retrievedS;
        Reader reader;
        
        retrievedS = det.getString(bytes, "UTF-8");
        CheckAssert(s.equals(retrievedS));
        
        reader = det.getReader(new ByteArrayInputStream(bytes), "UTF-8");
        CheckAssert(s.equals(stringFromReader(reader)));
        det.setDeclaredEncoding("UTF-8"); // Jitterbug 4451, for coverage
    }
    
    public void TestUTF16() throws Exception
    {
        String source = 
                "u0623\u0648\u0631\u0648\u0628\u0627, \u0628\u0631\u0645\u062c\u064a\u0627\u062a " +
                "\u0627\u0644\u062d\u0627\u0633\u0648\u0628 \u002b\u0020\u0627\u0646\u062a\u0631\u0646\u064a\u062a";
        
        byte[] beBytes = source.getBytes("UnicodeBig");
        byte[] leBytes = source.getBytes("UnicodeLittle");
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        
        det.setText(beBytes);
        m = det.detect();
        
        if (! m.getName().equals("UTF-16BE")) {
            errln("Encoding detection failure: expected UTF-16BE, got " + m.getName());
        }
        
        det.setText(leBytes);
        m = det.detect();
        
        if (! m.getName().equals("UTF-16LE")) {
            errln("Encoding detection failure: expected UTF-16LE, got " + m.getName());
        }

        // Jitterbug 4451, for coverage
        int confidence = m.getConfidence(); 
        if(confidence != 100){
            errln("Did not get the expected confidence level " + confidence);
        }
        int matchType = m.getMatchType();
        if(matchType != 0){
            errln("Did not get the expected matchType level " + matchType);
        }
    }
    
    public void TestC1Bytes() throws Exception
    {
        String sISO =
            "This is a small sample of some English text. Just enough to be sure that it detects correctly.";
        
        String sWindows =
            "This is another small sample of some English text. Just enough to be sure that it detects correctly. It also includes some \u201CC1\u201D bytes.";

        byte[] bISO     = sISO.getBytes("ISO-8859-1");
        byte[] bWindows = sWindows.getBytes("windows-1252");
        
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        
        det.setText(bWindows);
        m = det.detect();
        
        if (m.getName() != "windows-1252") {
            errln("Text with C1 bytes not correctly detected as windows-1252.");
            return;
        }
        
        det.setText(bISO);
        m = det.detect();
        
        if (m.getName() != "ISO-8859-1") {
            errln("Text without C1 bytes not correctly detected as ISO-8859-1.");
        }
    }
    
    public void TestShortInput() {
        // Test that detection with very short byte strings does not crash and burn.
        // The shortest input that should produce positive detection result is two bytes, 
        //   a UTF-16 BOM.
        // TODO:  Detector confidence levels needs to be refined for very short input.
        //        Too high now, for some charsets that happen to be compatible with a few bytes of input.
        byte [][]  shortBytes = new byte [][] 
            {
                {},
                {(byte)0x0a},
                {(byte)'A', (byte)'B'},
                {(byte)'A', (byte)'B', (byte)'C'},
                {(byte)'A', (byte)'B', (byte)'C', (byte)'D'}
            };
        
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        for (int i=0; i<shortBytes.length; i++) {
            det.setText(shortBytes[i]);
            m = det.detect();
            logln("i=" + i + " -> " + m.getName());
        }
    }
    
    public void TestBufferOverflow()
    {
        byte testStrings[][] = {
            {(byte) 0x80, (byte) 0x20, (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x45, (byte) 0x6E, (byte) 0x67, (byte) 0x6C, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x1b}, /* A partial ISO-2022 shift state at the end */
            {(byte) 0x80, (byte) 0x20, (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x45, (byte) 0x6E, (byte) 0x67, (byte) 0x6C, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x1b, (byte) 0x24}, /* A partial ISO-2022 shift state at the end */
            {(byte) 0x80, (byte) 0x20, (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x45, (byte) 0x6E, (byte) 0x67, (byte) 0x6C, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x1b, (byte) 0x24, (byte) 0x28}, /* A partial ISO-2022 shift state at the end */
            {(byte) 0x80, (byte) 0x20, (byte) 0x54, (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x45, (byte) 0x6E, (byte) 0x67, (byte) 0x6C, (byte) 0x69, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x1b, (byte) 0x24, (byte) 0x28, (byte) 0x44}, /* A complete ISO-2022 shift state at the end with a bad one at the start */
            {(byte) 0x1b, (byte) 0x24, (byte) 0x28, (byte) 0x44}, /* A complete ISO-2022 shift state at the end */
            {(byte) 0xa1}, /* Could be a single byte shift-jis at the end */
            {(byte) 0x74, (byte) 0x68, (byte) 0xa1}, /* Could be a single byte shift-jis at the end */
            {(byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0xa1} /* Could be a single byte shift-jis at the end, but now we have English creeping in. */
        };
        
        String testResults[] = {
            "windows-1252",
            "windows-1252",
            "windows-1252",
            "windows-1252",
            "ISO-2022-JP",
            null,
            null,
            "ISO-8859-1"
        };
        
        CharsetDetector det = new CharsetDetector();
        CharsetMatch match;

        det.setDeclaredEncoding("ISO-2022-JP");

        for (int idx = 0; idx < testStrings.length; idx += 1) {
            det.setText(testStrings[idx]);
            match = det.detect();

            if (match == null) {
                if (testResults[idx] != null) {
                    errln("Unexpectedly got no results at index " + idx);
                }
                else {
                    logln("Got no result as expected at index " + idx);
                }
                continue;
            }

            if (testResults[idx] == null || ! testResults[idx].equals(match.getName())) {
                errln("Unexpectedly got " + match.getName() + " instead of " + testResults[idx] +
                      " at index " + idx + " with confidence " + match.getConfidence());
                return;
            }
        }
    }

//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
    public void TestDetection()
    {
        //
        //  Open and read the test data file.
        //
        //InputStreamReader isr = null;
        
        try {
            InputStream is = TestCharsetDetector.class.getResourceAsStream("CharsetDetectionTests.xml");
            if (is == null) {
                errln("Could not open test data file CharsetDetectionTests.xml");
                return;
            }
            
            //isr = new InputStreamReader(is, "UTF-8"); 

            // Set up an xml parser.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            factory.setIgnoringComments(true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Parse the xml content from the test case file.
            Document doc = builder.parse(is, null);
            Element root = doc.getDocumentElement();
            
            NodeList testCases = root.getElementsByTagName("test-case");
            
            // Process each test case
            for (int n = 0; n < testCases.getLength(); n += 1) {
                Node testCase = testCases.item(n);
                NamedNodeMap attrs = testCase.getAttributes();
                NodeList testData  = testCase.getChildNodes();
                StringBuffer testText = new StringBuffer();
                String id = attrs.getNamedItem("id").getNodeValue();
                String encodings = attrs.getNamedItem("encodings").getNodeValue();
                
                // Collect the test case text.
                for (int t = 0; t < testData.getLength(); t += 1) {
                    Node textNode = testData.item(t);
                    
                    testText.append(textNode.getNodeValue());                    
                }
                
                // Process test text with each encoding / language pair.
                String testString = testText.toString();
                String[] encodingList = encodings.split(" ");
                for (int e = 0; e < encodingList.length; e += 1) {
                    checkEncoding(testString, encodingList[e], id);
                }
            }
            
        } catch (Exception e) {
            errln("exception while processing test cases: " + e.toString());
        }
    }

    private void checkMatch(CharsetDetector det, String testString, String encoding, String language, String id) throws Exception
    {
        CharsetMatch m = det.detect();
        String decoded;
        
        if (! m.getName().equals(encoding)) {
            errln(id + ": encoding detection failure - expected " + encoding + ", got " + m.getName());
            return;
        }
        
        String charsetMatchLanguage = m.getLanguage();
        if ((language != null && !charsetMatchLanguage.equals(language))
            || (language == null && charsetMatchLanguage != null)
            || (language != null && charsetMatchLanguage == null))
        {
            errln(id + ", " + encoding + ": language detection failure - expected " + language + ", got " + m.getLanguage());
        }
        
        if (encoding.startsWith("UTF-32")) {
            return;
        }
        
        decoded = m.getString();
        
        if (! testString.equals(decoded)) {
            errln(id + ", " + encoding + ": getString() didn't return the original string!");
        }
        
        decoded = stringFromReader(m.getReader());
        
        if (! testString.equals(decoded)) {
            errln(id + ", " + encoding + ": getReader() didn't yield the original string!");
        }
    }
    
    private void checkEncoding(String testString, String encoding, String id)
    {
        String enc = null, lang = null;
        String[] split = encoding.split("/");
        
        enc = split[0];
        
        if (split.length > 1) {
            lang = split[1];
        }

        try {
            CharsetDetector det = new CharsetDetector();
            byte[] bytes;
            
            //if (enc.startsWith("UTF-32")) {
            //    UTF32 utf32 = UTF32.getInstance(enc);
                
            //    bytes = utf32.toBytes(testString);
            //} else {
                String from = enc;

                while (true) {
                    try {
                        bytes = testString.getBytes(from);
                    } catch (UnsupportedOperationException uoe) {
                         // In some runtimes, the ISO-2022-CN converter
                         // only converts *to* Unicode - we have to use
                         // x-ISO-2022-CN-GB to convert *from* Unicode.
                        if (from.equals("ISO-2022-CN")) {
                            from = "x-ISO-2022-CN-GB";
                            continue;
                        }
                        
                        // Ignore any other converters that can't
                        // convert from Unicode.
                        return;
                    } catch (UnsupportedEncodingException uee) {
                        // Ignore any encodings that this runtime
                        // doesn't support.
                        return;
                    }
                    
                    break;
                }
            //}
        
            det.setText(bytes);
            checkMatch(det, testString, enc, lang, id);
            
            det.setText(new ByteArrayInputStream(bytes));
            checkMatch(det, testString, enc, lang, id);
         } catch (Exception e) {
            errln(id + ": " + e.toString() + "enc=" + enc);
            e.printStackTrace();
        }
    }
//#endif
    
    public void TestArabic() throws Exception {
        String  s = "\u0648\u0636\u0639\u062A \u0648\u0646\u0641\u0630\u062A \u0628\u0631\u0627" +
        "\u0645\u062C \u062A\u0623\u0645\u064A\u0646 \u0639\u062F\u064A\u062F\u0629 \u0641\u064A " + 
        "\u0645\u0624\u0633\u0633\u0629 \u0627\u0644\u062A\u0623\u0645\u064A\u0646 \u0627\u0644"  + 
        "\u0648\u0637\u0646\u064A, \u0645\u0639 \u0645\u0644\u0627\u0626\u0645\u062A\u0647\u0627 " + 
        "\u062F\u0627\u0626\u0645\u0627 \u0644\u0644\u0627\u062D\u062A\u064A\u0627\u062C" + 
        "\u0627\u062A \u0627\u0644\u0645\u062A\u063A\u064A\u0631\u0629 \u0644\u0644\u0645\u062C" + 
        "\u062A\u0645\u0639 \u0648\u0644\u0644\u062F\u0648\u0644\u0629. \u062A\u0648\u0633\u0639" + 
        "\u062A \u0648\u062A\u0637\u0648\u0631\u062A \u0627\u0644\u0645\u0624\u0633\u0633\u0629 " + 
        "\u0628\u0647\u062F\u0641 \u0636\u0645\u0627\u0646 \u0634\u0628\u0643\u0629 \u0623\u0645" + 
        "\u0627\u0646 \u0644\u0633\u0643\u0627\u0646 \u062F\u0648\u0644\u0629 \u0627\u0633\u0631" + 
        "\u0627\u0626\u064A\u0644 \u0628\u0648\u062C\u0647 \u0627\u0644\u0645\u062E\u0627\u0637" + 
        "\u0631 \u0627\u0644\u0627\u0642\u062A\u0635\u0627\u062F\u064A\u0629 \u0648\u0627\u0644" + 
        "\u0627\u062C\u062A\u0645\u0627\u0639\u064A\u0629.";
        
        CharsetMatch m = _test1256(s);
        String charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("windows-1256"));
        
        /* Create an encoder to get the bytes.
         * Using String.getBytes("IBM420") can produce inconsistent results
         * between different versions of the JDK.
         */
        CharsetEncoder encoder = new CharsetProviderICU().charsetForName("IBM420").newEncoder();
        
        m = _testIBM420_ar_rtl(s, encoder);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM420_rtl"));
        
         m = _testIBM420_ar_ltr(s, encoder);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM420_ltr"));

    }
    
    private CharsetMatch _testIBM420_ar_rtl(String s, CharsetEncoder encoder) throws Exception {
        CharsetDetector det = new CharsetDetector();
        det.setText(encoder.encode(CharBuffer.wrap(s)).array());
        CharsetMatch m = det.detect();
        return m;
    }
    
    
    private CharsetMatch _testIBM420_ar_ltr(String s, CharsetEncoder encoder) throws Exception {
        /**
         * transformation of input string to CP420 left to right requires reversing the string
         */    
        
        StringBuffer ltrStrBuf = new StringBuffer(s);
        ltrStrBuf = ltrStrBuf.reverse();
        
        CharsetDetector det = new CharsetDetector();
        det.setText(encoder.encode(CharBuffer.wrap(ltrStrBuf.toString())).array());
        CharsetMatch m = det.detect();
        return m;
    }

    private CharsetMatch _test1256(String s) throws Exception {
        
        byte [] bytes = s.getBytes("windows-1256");
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        return m;
    }
    
    public void TestHebrew() throws Exception {
        String  s =  "\u05D4\u05E4\u05E8\u05E7\u05DC\u05D9\u05D8 \u05D4\u05E6\u05D1\u05D0\u05D9 \u05D4" +
            "\u05E8\u05D0\u05E9\u05D9, \u05EA\u05EA \u05D0\u05DC\u05D5\u05E3 \u05D0\u05D1\u05D9" + 
            "\u05D7\u05D9 \u05DE\u05E0\u05D3\u05DC\u05D1\u05DC\u05D9\u05D8, \u05D4\u05D5\u05E8" + 
            "\u05D4 \u05E2\u05DC \u05E4\u05EA\u05D9\u05D7\u05EA \u05D7\u05E7\u05D9\u05E8\u05EA " + 
            "\u05DE\u05E6\"\u05D7 \u05D1\u05E2\u05E7\u05D1\u05D5\u05EA \u05E2\u05D3\u05D5\u05D9" + 
            "\u05D5\u05EA \u05D7\u05D9\u05D9\u05DC\u05D9 \u05E6\u05D4\"\u05DC \u05DE\u05DE\u05D1" + 
            "\u05E6\u05E2 \u05E2\u05D5\u05E4\u05E8\u05EA \u05D9\u05E6\u05D5\u05E7\u05D4 \u05D1+ " +
            "\u05E8\u05E6\u05D5\u05E2\u05EA \u05E2\u05D6\u05D4. \u05DC\u05D3\u05D1\u05E8\u05D9 " + 
            "\u05D4\u05E4\u05E6\"\u05E8, \u05DE\u05D4\u05E2\u05D3\u05D5\u05D9\u05D5\u05EA \u05E2" +
            "\u05D5\u05DC\u05D4 \u05EA\u05DE\u05D5\u05E0\u05D4 \u05E9\u05DC \"\u05D4\u05EA\u05E0" + 
            "\u05D4\u05D2\u05D5\u05EA \u05E4\u05E1\u05D5\u05DC\u05D4 \u05DC\u05DB\u05D0\u05D5\u05E8" + 
            "\u05D4 \u05E9\u05DC \u05D7\u05D9\u05D9\u05DC\u05D9\u05DD \u05D1\u05DE\u05D4\u05DC\u05DA" + 
            " \u05DE\u05D1\u05E6\u05E2 \u05E2\u05D5\u05E4\u05E8\u05EA \u05D9\u05E6\u05D5\u05E7\u05D4\"." + 
            " \u05DE\u05E0\u05D3\u05DC\u05D1\u05DC\u05D9\u05D8 \u05E7\u05D9\u05D1\u05DC \u05D0\u05EA" +
            " \u05D4\u05D7\u05DC\u05D8\u05EA\u05D5 \u05DC\u05D0\u05D7\u05E8 \u05E9\u05E2\u05D9\u05D9" +
            "\u05DF \u05D1\u05EA\u05DE\u05DC\u05D9\u05DC \u05D4\u05E2\u05D3\u05D5\u05D9\u05D5\u05EA";
        
        CharsetMatch m = _test1255(s);
        String charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("ISO-8859-8"));
        
        m = _testIBM424_he_rtl(s);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM424_rtl"));
        
        m = _testIBM424_he_ltr(s);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM424_ltr"));
    }

    private CharsetMatch _test1255(String s) throws Exception {
        byte [] bytes = s.getBytes("ISO-8859-8");
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        return m;
    }
    
    private CharsetMatch _testIBM424_he_rtl(String s) throws Exception {
        byte [] bytes = s.getBytes("IBM424");        
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        return m;
    }
    
    private CharsetMatch _testIBM424_he_ltr(String s) throws Exception {
        /**
         * transformation of input string to CP420 left to right requires reversing the string
         */    
        
        StringBuffer ltrStrBuf = new StringBuffer(s);
        ltrStrBuf = ltrStrBuf.reverse();
        byte [] bytes = ltrStrBuf.toString().getBytes("IBM424");
        
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        return m;
    }
}
