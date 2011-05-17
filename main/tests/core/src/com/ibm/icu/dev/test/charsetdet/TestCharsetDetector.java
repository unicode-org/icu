/**
 *******************************************************************************
 * Copyright (C) 2005-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.charsetdet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;


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
                StackTraceElement failPoint = e.getStackTrace()[1];
                msg = "Test failure in file " + failPoint.getFileName() +
                             " at line " + failPoint.getLineNumber();
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
    
    public void TestJapanese() throws Exception {
        String s = "\u3000\u3001\u3002\u3003\u3005\u3006\u3007\u3008\u3009\u300A\u300B\u300C\u300D\u300E\u300F\u3010\u3011\u3012\u3013\u3014" + 
        "\u3015\u301C\u3041\u3042\u3043\u3044\u3045\u3046\u3047\u3048\u3049\u304A\u304B\u304C\u304D\u304E\u304F\u3050\u3051\u3052" + 
        "\u3053\u3054\u3055\u3056\u3057\u3058\u3059\u305A\u305B\u305C\u305D\u305E\u305F\u3060\u3061\u3062\u3063\u3064\u3065\u3066" + 
        "\u3067\u3068\u3069\u306A\u306B\u306C\u306D\u306E\u306F\u3070\u3071\u3072\u3073\u3074\u3075\u3076\u3077\u3078\u3079\u307A" + 
        "\u307B\u307C\u307D\u307E\u307F\u3080\u3081\u3082\u3083\u3084\u3085\u3086\u3087\u3088\u3089\u308A\u308B\u308C\u308D\u308E" + 
        "\u308F\u3090\u3091\u3092\u3093\u309B\u309C\u309D\u309E\u30A1\u30A2\u30A3\u30A4\u30A5\u30A6\u30A7\u30A8\u30A9\u30AA\u30AB" + 
        "\u30AC\u30AD\u30AE\u30AF\u30B0\u30B1\u30B2\u30B3\u30B4\u30B5\u30B6\u30B7\u30B8\u30B9\u30BA\u30BB\u30BC\u30BD\u30BE\u30BF" + 
        "\u30C0\u30C1\u30C2\u30C3\u30C4\u30C5\u30C6\u30C7\u30C8\u30C9\u30CA\u30CB\u30CC\u30CD\u30CE\u30CF\u30D0\u30D1\u30D2\u30D3" + 
        "\u30D4\u30D5\u30D6\u30D7\u30D8\u30D9\u30DA\u30DB\u30DC\u30DD\u30DE\u30DF\u30E0\u30E1\u30E2\u30E3\u30E4\u30E5\u30E6\u30E7" + 
        "\u30E8\u30E9\u30EA\u30EB\u30EC\u30ED\u30EE\u30EF\u30F0\u30F1\u30F2\u30F3\u30F4\u30F5\u30F6\u30FB\u30FC\u30FD\u30FE\u4E00" + 
        "\u4E01\u4E02\u4E03\u4E04\u4E05\u4E07\u4E08\u4E09\u4E0A\u4E0B\u4E0C\u4E0D\u4E0E\u4E10\u4E11\u4E12\u4E14\u4E15\u4E16\u4E17" + 
        "\u4E18\u4E19\u4E1E\u4E1F\u4E21\u4E23\u4E24\u4E26\u4E28\u4E2A\u4E2B\u4E2D\u4E2E\u4E2F\u4E30\u4E31\u4E32\u4E35\u4E36\u4E38" + 
        "\u4E39\u4E3B\u4E3C\u4E3F\u4E40\u4E41\u4E42\u4E43\u4E44\u4E45\u4E47\u4E4B\u4E4D\u4E4E\u4E4F\u4E51\u4E55\u4E56\u4E57\u4E58" + 
        "\u4E59\u4E5A\u4E5C\u4E5D\u4E5E\u4E5F\u4E62\u4E63\u4E68\u4E69\u4E71\u4E73\u4E74\u4E75\u4E79\u4E7E\u4E7F\u4E80\u4E82\u4E85" + 
        "\u4E86\u4E88\u4E89\u4E8A\u4E8B\u4E8C";
        
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        String charsetMatch;
        byte[] bytes;
        {
            bytes = s.getBytes("EUC-JP");
            det.setText(bytes);
            m = det.detect();
            charsetMatch = m.getName();
            CheckAssert(charsetMatch.equals("EUC-JP"));
            
            // Tests "public String getLanguage()"
            CheckAssert(m.getLanguage().equals("ja"));
        }
    }

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

        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        String charsetMatch;
        byte[] bytes;
        {
            bytes = s.getBytes("windows-1256");
            det.setText(bytes);
            m = det.detect();
            charsetMatch = m.getName();
            CheckAssert(charsetMatch.equals("windows-1256"));
            
            // Tests "public String getLanguage()"
            CheckAssert(m.getLanguage().endsWith("ar"));
        }

        {
            // We cannot rely on IBM420 converter in Sun Java
            /*
            bytes = s.getBytes("IBM420");
            */
            bytes = new byte[] {
                (byte)0xCF, (byte)0x8D, (byte)0x9A, (byte)0x63, (byte)0x40, (byte)0xCF, (byte)0xBD, (byte)0xAB,
                (byte)0x74, (byte)0x63, (byte)0x40, (byte)0x58, (byte)0x75, (byte)0x56, (byte)0xBB, (byte)0x67,
                (byte)0x40, (byte)0x63, (byte)0x49, (byte)0xBB, (byte)0xDC, (byte)0xBD, (byte)0x40, (byte)0x9A,
                (byte)0x73, (byte)0xDC, (byte)0x73, (byte)0x62, (byte)0x40, (byte)0xAB, (byte)0xDC, (byte)0x40,
                (byte)0xBB, (byte)0x52, (byte)0x77, (byte)0x77, (byte)0x62, (byte)0x40, (byte)0x56, (byte)0xB1,
                (byte)0x63, (byte)0x49, (byte)0xBB, (byte)0xDC, (byte)0xBD, (byte)0x40, (byte)0x56, (byte)0xB1,
                (byte)0xCF, (byte)0x8F, (byte)0xBD, (byte)0xDC, (byte)0x6B, (byte)0x40, (byte)0xBB, (byte)0x9A,
                (byte)0x40, (byte)0xBB, (byte)0xB1, (byte)0x56, (byte)0x55, (byte)0xBB, (byte)0x63, (byte)0xBF,
                (byte)0x56, (byte)0x40, (byte)0x73, (byte)0x56, (byte)0x55, (byte)0xBB, (byte)0x56, (byte)0x40,
                (byte)0xB1, (byte)0xB1, (byte)0x56, (byte)0x69, (byte)0x63, (byte)0xDC, (byte)0x56, (byte)0x67,
                (byte)0x56, (byte)0x63, (byte)0x40, (byte)0x56, (byte)0xB1, (byte)0xBB, (byte)0x63, (byte)0x9E,
                (byte)0xDC, (byte)0x75, (byte)0x62, (byte)0x40, (byte)0xB1, (byte)0xB1, (byte)0xBB, (byte)0x67,
                (byte)0x63, (byte)0xBB, (byte)0x9A, (byte)0x40, (byte)0xCF, (byte)0xB1, (byte)0xB1, (byte)0x73,
                (byte)0xCF, (byte)0xB1, (byte)0x62, (byte)0x4B, (byte)0x40, (byte)0x63, (byte)0xCF, (byte)0x77,
                (byte)0x9A, (byte)0x63, (byte)0x40, (byte)0xCF, (byte)0x63, (byte)0x8F, (byte)0xCF, (byte)0x75,
                (byte)0x63, (byte)0x40, (byte)0x56, (byte)0xB1, (byte)0xBB, (byte)0x52, (byte)0x77, (byte)0x77,
                (byte)0x62, (byte)0x40, (byte)0x58, (byte)0xBF, (byte)0x73, (byte)0xAB, (byte)0x40, (byte)0x8D,
                (byte)0xBB, (byte)0x56, (byte)0xBD, (byte)0x40, (byte)0x80, (byte)0x58, (byte)0xAF, (byte)0x62,
                (byte)0x40, (byte)0x49, (byte)0xBB, (byte)0x56, (byte)0xBD, (byte)0x40, (byte)0xB1, (byte)0x77,
                (byte)0xAF, (byte)0x56, (byte)0xBD, (byte)0x40, (byte)0x73, (byte)0xCF, (byte)0xB1, (byte)0x62,
                (byte)0x40, (byte)0x56, (byte)0x77, (byte)0x75, (byte)0x56, (byte)0x55, (byte)0xDC, (byte)0xB1,
                (byte)0x40, (byte)0x58, (byte)0xCF, (byte)0x67, (byte)0xBF, (byte)0x40, (byte)0x56, (byte)0xB1,
                (byte)0xBB, (byte)0x71, (byte)0x56, (byte)0x8F, (byte)0x75, (byte)0x40, (byte)0x56, (byte)0xB1,
                (byte)0x56, (byte)0xAD, (byte)0x63, (byte)0x8B, (byte)0x56, (byte)0x73, (byte)0xDC, (byte)0x62,
                (byte)0x40, (byte)0xCF, (byte)0x56, (byte)0xB1, (byte)0x56, (byte)0x67, (byte)0x63, (byte)0xBB,
                (byte)0x56, (byte)0x9A, (byte)0xDC, (byte)0x62, (byte)0x4B,
            };
            det.setText(bytes);
            m = det.detect();
            charsetMatch = m.getName();
            CheckAssert(charsetMatch.equals("IBM420_rtl"));
            
         // Tests "public String getLanguage()"
            CheckAssert(m.getLanguage().endsWith("ar"));
        }

        {
            // We cannot rely on IBM420 converter in Sun Java
            /*
            StringBuffer ltrStrBuf = new StringBuffer(s);
            ltrStrBuf = ltrStrBuf.reverse();
            bytes = ltrStrBuf.toString().getBytes("IBM420");
            */
            bytes = new byte[] {
                (byte)0x4B, (byte)0x62, (byte)0xDC, (byte)0x9A, (byte)0x56, (byte)0xBB, (byte)0x63, (byte)0x67,
                (byte)0x56, (byte)0xB1, (byte)0x56, (byte)0xCF, (byte)0x40, (byte)0x62, (byte)0xDC, (byte)0x73,
                (byte)0x56, (byte)0x8B, (byte)0x63, (byte)0xAD, (byte)0x56, (byte)0xB1, (byte)0x56, (byte)0x40,
                (byte)0x75, (byte)0x8F, (byte)0x56, (byte)0x71, (byte)0xBB, (byte)0xB1, (byte)0x56, (byte)0x40,
                (byte)0xBF, (byte)0x67, (byte)0xCF, (byte)0x58, (byte)0x40, (byte)0xB1, (byte)0xDC, (byte)0x55,
                (byte)0x56, (byte)0x75, (byte)0x77, (byte)0x56, (byte)0x40, (byte)0x62, (byte)0xB1, (byte)0xCF,
                (byte)0x73, (byte)0x40, (byte)0xBD, (byte)0x56, (byte)0xAF, (byte)0x77, (byte)0xB1, (byte)0x40,
                (byte)0xBD, (byte)0x56, (byte)0xBB, (byte)0x49, (byte)0x40, (byte)0x62, (byte)0xAF, (byte)0x58,
                (byte)0x80, (byte)0x40, (byte)0xBD, (byte)0x56, (byte)0xBB, (byte)0x8D, (byte)0x40, (byte)0xAB,
                (byte)0x73, (byte)0xBF, (byte)0x58, (byte)0x40, (byte)0x62, (byte)0x77, (byte)0x77, (byte)0x52,
                (byte)0xBB, (byte)0xB1, (byte)0x56, (byte)0x40, (byte)0x63, (byte)0x75, (byte)0xCF, (byte)0x8F,
                (byte)0x63, (byte)0xCF, (byte)0x40, (byte)0x63, (byte)0x9A, (byte)0x77, (byte)0xCF, (byte)0x63,
                (byte)0x40, (byte)0x4B, (byte)0x62, (byte)0xB1, (byte)0xCF, (byte)0x73, (byte)0xB1, (byte)0xB1,
                (byte)0xCF, (byte)0x40, (byte)0x9A, (byte)0xBB, (byte)0x63, (byte)0x67, (byte)0xBB, (byte)0xB1,
                (byte)0xB1, (byte)0x40, (byte)0x62, (byte)0x75, (byte)0xDC, (byte)0x9E, (byte)0x63, (byte)0xBB,
                (byte)0xB1, (byte)0x56, (byte)0x40, (byte)0x63, (byte)0x56, (byte)0x67, (byte)0x56, (byte)0xDC,
                (byte)0x63, (byte)0x69, (byte)0x56, (byte)0xB1, (byte)0xB1, (byte)0x40, (byte)0x56, (byte)0xBB,
                (byte)0x55, (byte)0x56, (byte)0x73, (byte)0x40, (byte)0x56, (byte)0xBF, (byte)0x63, (byte)0xBB,
                (byte)0x55, (byte)0x56, (byte)0xB1, (byte)0xBB, (byte)0x40, (byte)0x9A, (byte)0xBB, (byte)0x40,
                (byte)0x6B, (byte)0xDC, (byte)0xBD, (byte)0x8F, (byte)0xCF, (byte)0xB1, (byte)0x56, (byte)0x40,
                (byte)0xBD, (byte)0xDC, (byte)0xBB, (byte)0x49, (byte)0x63, (byte)0xB1, (byte)0x56, (byte)0x40,
                (byte)0x62, (byte)0x77, (byte)0x77, (byte)0x52, (byte)0xBB, (byte)0x40, (byte)0xDC, (byte)0xAB,
                (byte)0x40, (byte)0x62, (byte)0x73, (byte)0xDC, (byte)0x73, (byte)0x9A, (byte)0x40, (byte)0xBD,
                (byte)0xDC, (byte)0xBB, (byte)0x49, (byte)0x63, (byte)0x40, (byte)0x67, (byte)0xBB, (byte)0x56,
                (byte)0x75, (byte)0x58, (byte)0x40, (byte)0x63, (byte)0x74, (byte)0xAB, (byte)0xBD, (byte)0xCF,
                (byte)0x40, (byte)0x63, (byte)0x9A, (byte)0x8D, (byte)0xCF,
            };

            det.setText(bytes);
            m = det.detect();
            charsetMatch = m.getName();
            CheckAssert(charsetMatch.equals("IBM420_ltr"));
        }
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
        CheckAssert(m.getLanguage().equals("he"));
        
        m = _test1255_reverse(s);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("ISO-8859-8"));
        CheckAssert(m.getLanguage().equals("he"));
        
        m = _testIBM424_he_rtl(s);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM424_rtl"));
        CheckAssert(m.getLanguage().equals("he"));
        
        m = _testIBM424_he_ltr(s);
        charsetMatch = m.getName();
        CheckAssert(charsetMatch.equals("IBM424_ltr"));
        CheckAssert(m.getLanguage().equals("he"));
    }
    
    private CharsetMatch _test1255(String s) throws Exception {
        byte [] bytes = s.getBytes("ISO-8859-8");
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        return m;
    }
    
    private CharsetMatch _test1255_reverse(String s) throws Exception {
        StringBuffer reverseStrBuf = new StringBuffer(s);
        reverseStrBuf = reverseStrBuf.reverse();
        byte [] bytes = reverseStrBuf.toString().getBytes("ISO-8859-8");
        
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
    
    /*
     * Test the method int match(CharsetDetector det) in CharsetRecog_UTF_16_LE
     */
    public void TestCharsetRecog_UTF_16_LE_Match() {
        byte[] in = { Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE, Byte.MIN_VALUE };
        CharsetDetector cd = new CharsetDetector();
        // Tests when if (input.length>=4 && input[2] == 0x00 && input[3] == 0x00) is true inside the
        // match(CharsetDetector) method of CharsetRecog_UTF_16_LE
        try {
            cd.setText(in);
        } catch (Exception e) {
            errln("CharsetRecog_UTF_16_LE.match(CharsetDetector) was not suppose to return an exception.");
        }
    }
}
