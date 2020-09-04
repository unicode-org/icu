// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.BreakIterator;

import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UTF16;

/**
 * A class for testing UnicodeSet performance.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public class RBBIPerf extends PerfTest {

    String                  dataFileName;
    RuleBasedBreakIterator  bi;
    BreakIterator           jdkbi;
    String                  testString;

    public static void main(String[] args) throws Exception {
        new RBBIPerf().run(args);
    }

    protected void setup(String[] args) {
        // We only take one argument, the pattern
        if (args.length != 2) {
            throw new RuntimeException("RBBITest params:  data_file_name break_iterator_type ");
        }

        try {
            dataFileName = args[0];
            StringBuffer  testFileBuf = new StringBuffer();
            InputStream is = new FileInputStream(dataFileName);
            InputStreamReader isr = new InputStreamReader(is, "UTF-8");           
            int c;
            for (;;) {
                c = isr.read();
                if (c < 0) {
                    break;
                }
                UTF16.append(testFileBuf, c);
            }
            testString = testFileBuf.toString();
        }
        catch (IOException e) {
            throw new RuntimeException(e.toString());   
        }
        
        if (args.length >= 2) {
            if (args[1].equals("char")) {
                bi  = (RuleBasedBreakIterator)com.ibm.icu.text.BreakIterator.getCharacterInstance();  
            } else if (args[1].equals("word")) {
                bi  = (RuleBasedBreakIterator)com.ibm.icu.text.BreakIterator.getWordInstance();
            } else if (args[1].equals("line")) {
                bi  = (RuleBasedBreakIterator)com.ibm.icu.text.BreakIterator.getLineInstance();
            } else if (args[1].equals("jdkline")) {
                jdkbi  = BreakIterator.getLineInstance();
            }
        }
        if (bi!=null ) {
            bi.setText(testString);
        }
        if (jdkbi != null) {
            jdkbi.setText(testString);   
        }
        
    }

    
    
    PerfTest.Function testRBBINext() {
        return new PerfTest.Function() {
            
            public void call() {
                int n;
                if (bi != null) {
                    n = bi.first();
                    for (; n != BreakIterator.DONE; n=bi.next()) {
                    }   
                } else {
                    n = jdkbi.first();
                    for (; n != BreakIterator.DONE; n=jdkbi.next()) {
                    }   
                }
            }
        
            
            public long getOperationsPerIteration() {
                int n;
                int count = 0;
                if (bi != null) {
                    for (n=bi.first(); n != BreakIterator.DONE; n=bi.next()) {
                        count++;
                    }
                } else {                  
                    for (n=jdkbi.first(); n != BreakIterator.DONE; n=jdkbi.next()) {
                        count++;
                    }
                }
                return count;
            }
        };
    }
    
    
    PerfTest.Function testRBBIPrevious() {
        return new PerfTest.Function() {
            
            public void call() {
                bi.first();
                int n=0;
                for (n=bi.last(); n != BreakIterator.DONE; n=bi.previous()) {
                }   
            }
            
            
            public long getOperationsPerIteration() {
                int n;
                int count = 0;
                for (n=bi.last(); n != BreakIterator.DONE; n=bi.previous()) {
                    count++;
                }   
                return count;
            }
        };
    }


    PerfTest.Function testRBBIIsBoundary() {
        return new PerfTest.Function() {
            
            public void call() {
                int n=testString.length();
                int i;
                for (i=0; i<n; i++) {
                    bi.isBoundary(i);
                }   
            }
            
            public long getOperationsPerIteration() {
                int n = testString.length();
                return n;
            }
        };
    }


 
}
