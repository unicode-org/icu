/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;
import com.ibm.icu.text.*;
import java.util.HashSet;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * A class for testing UnicodeSet performance.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public class RBBIPerf extends PerfTest {

    String dataFileName;
    RuleBasedBreakIterator bi;
    String testString;

    public static void main(String[] args) throws Exception {
        new RBBIPerf().run(args);
    }

    protected void setup(String[] args) {
        // We only take one argument, the pattern
        if (args.length != 1) {
            throw new RuntimeException("Please supply utf-8 encoded text data file");
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
        
        bi = (RuleBasedBreakIterator)BreakIterator.getLineInstance();
        bi.setText(testString);
        
    }

    
    
    PerfTest.Function testRBBINext() {
        return new PerfTest.Function() {
            
        	public void call() {
                int n;
        		n = bi.first();
        		for (; n != BreakIterator.DONE; n=bi.next()) {
        		}   
        	}
        	
        	
            public long getOperationsPerIteration() {
                int n;
                int count = 0;
                for (n=bi.first(); n != BreakIterator.DONE; n=bi.next()) {
                    count++;
                }   
                System.out.println(count);
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
                System.out.println(count);
                return count;
            }
        };
    }


 
}
