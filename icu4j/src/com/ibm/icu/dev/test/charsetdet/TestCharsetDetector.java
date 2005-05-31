/**
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.charsetdet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;


/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestCharsetDetector extends TestFmwk {

    
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
    
    private void checkMatch(CharsetDetector det, String testString, String encoding, String language, String id) throws Exception
    {
        CharsetMatch m = det.detect();
        String decoded;
        
        if (! m.getName().equals(encoding)) {
            errln(id + ": encoding detection failure - expected " + encoding + ", got " + m.getName());
            return;
        }
        
        if (! (language == null || m.getLanguage().equals(language))) {
            errln(id + ", " + encoding + ": language detection failure - expected " + language + ", got " + m.getLanguage());
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
            byte[] bytes = testString.getBytes(enc);
            CharsetDetector det = new CharsetDetector();
            
            det.setText(bytes);
            checkMatch(det, testString, enc, lang, id);
            
            det.setText(new ByteArrayInputStream(bytes));
            checkMatch(det, testString, enc, lang, id);
         } catch (Exception e) {
            errln(id + ": " + e.toString());
        }
        
    }
    
    public void TestConstruction() {
        int i;
        CharsetDetector  det = new CharsetDetector();
        
        String [] charsetNames = CharsetDetector.getAllDetectableCharsets();
        CheckAssert(charsetNames.length != 0);
        for (i=0; i<charsetNames.length; i++) {
            CheckAssert(charsetNames[i].equals("") == false); 
            // System.out.println("\"" + charsetNames[i] + "\"");
        }
     }

    public void TestInputFilter() throws Exception
    {
        String s = "<a> <lot> <of> <English> <inside> <the> <markup> Une peut de Fran\u00E7ais. <to> <confuse> <the> <detector>";
        byte[] bytes = s.getBytes("ISO-8859-1");
        CharsetDetector det = new CharsetDetector();
        CharsetMatch m;
        
        det.enableInputFilter(true);
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
        CharsetMatch m;
        String retrievedS;
        Reader reader;
        
        det.setText(bytes);
        m = det.detect();
        CheckAssert(m.getName().equals("UTF-8"));
        retrievedS = m.getString();
        CheckAssert(s.equals(retrievedS));
        
        reader = m.getReader();
        CheckAssert(s.equals(stringFromReader(reader)));
        
        det.setText(new ByteArrayInputStream(bytes));
        m = det.detect();
        CheckAssert(m.getName().equals("UTF-8"));
        retrievedS = m.getString();
        CheckAssert(s.equals(retrievedS));
        
        reader = m.getReader();
        CheckAssert(s.equals(stringFromReader(reader)));
    }
    
    public void TestDetection()
    {
        //
        //  Open and read the test data file.
        //
        InputStreamReader isr = null;
        
        try {
            InputStream is = TestCharsetDetector.class.getResourceAsStream("CharsetDetectionTests.xml");
            if (is == null) {
                errln("Could not open test data file CharsetDetectionTests.xml");
                return;
            }
            
            isr = new InputStreamReader(is, "UTF-8"); 

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
}
