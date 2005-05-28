/**
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.charsetdet;

import java.io.InputStream;
import java.io.InputStreamReader;

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
            
            CharsetMatch m = det.detect();
            
//          CheckAssert(m.getName().equals(enc));
            if (! m.getName().equals(enc)) {
                errln(id + ": detection failure - expected " + enc + " got " + m.getName());
            }
            
            if (lang != null) {
//              CheckAssert(m.getLanguage().equals(lang));
                if (! m.getLanguage().equals(lang)) {
                    errln(id + ": language detection failure - expected " + lang + " got " + m.getLanguage());
                }
            }
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
    
    public void TestUTF8() throws Exception {
        
        String  s = "This is a string with some non-ascii characters that will " +
                    "be converted to UTF-8, then shoved through the detection process.  " +
                    "\u0391\u0392\u0393\u0394\u0395" +
                    "Sure would be nice if our source could contain Unicode directly!";
        byte [] bytes = s.getBytes("UTF-8");
        CharsetDetector det = new CharsetDetector();
        det.setText(bytes);
        CharsetMatch m = det.detect();
        CheckAssert(m.getName().equals("UTF-8"));
        String retrievedS = m.getString();
        CheckAssert(s.equals(retrievedS));
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
