/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/Attic/TestFmwk.java,v $ 
 * $Date: 2001/02/28 20:37:24 $ 
 * $Revision: 1.14 $
 *
 *****************************************************************************************
 */
package com.ibm.test;

import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;
import java.text.*;



/**
 * TestFmwk is a base class for tests that can be run conveniently from
 * the command line as well as under the Java test harness.
 * <p>
 * Sub-classes implement a set of methods named Test<something>. Each
 * of these methods performs some test. Test methods should indicate
 * errors by calling either err or errln.  This will increment the
 * errorCount field and may optionally print a message to the log.
 * Debugging information may also be added to the log via the log
 * and logln methods.  These methods will add their arguments to the
 * log only if the test is being run in verbose mode.
 */

public class TestFmwk implements TestLog {

    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    //------------------------------------------------------------------------
    // Everything below here is boilerplate code that makes it possible
    // to add a new test by simply adding a function to an existing class
    //------------------------------------------------------------------------

	protected TestFmwk() {
        // Create a hashtable containing all the test methods.
        testMethods = new Hashtable();
        Method[] methods = getClass().getDeclaredMethods();
        for( int i=0; i<methods.length; i++ ) {
            if( methods[i].getName().startsWith("Test") 
					|| methods[i].getName().startsWith("test")) {
                testMethods.put( methods[i].getName(), methods[i] );
            }
        }
    }

    private void _run() throws Exception {
        writeTestName(getClass().getName());
        params.indentLevel++;
		Enumeration methodsToRun;
		
		if (testsToRun != null && testsToRun.size() >= 1) {
			methodsToRun = testsToRun.elements();
		} else {
			methodsToRun = testMethods.elements();
		}

        int oldClassCount = params.errorCount;

        // Run the list of tests given in the test arguments
        while (methodsToRun.hasMoreElements()) {
            int oldCount = params.errorCount;

           	Method testMethod = (Method)methodsToRun.nextElement();
            writeTestName(testMethod.getName());

            try {
                testMethod.invoke(this, new Object[0]);
            } catch( IllegalAccessException e ) {
                errln("Can't access test method " + testMethod.getName());
            } catch( InvocationTargetException e ) {
                errln("Uncaught exception \""+e+"\" thrown in test method "
                        + testMethod.getName());
                e.getTargetException().printStackTrace(this.params.log);
            }
            writeTestResult(params.errorCount - oldCount);
        }
        params.indentLevel--;
        writeTestResult(params.errorCount - oldClassCount);
    }
    
    public void run(String[] args) throws Exception {
    	if (params == null) params = new TestParams();
        // Parse the test arguments.  They can be either the flag
        // "-verbose" or names of test methods. Create a list of
        // tests to be run.
        testsToRun = new Vector(args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-verbose") || args[i].equals("-v")) {
                params.verbose = true;
            }
            else if (args[i].equals("-prompt")) {
                params.prompt = true;
            } else if (args[i].equals("-nothrow")) {
                params.nothrow = true;
            } else {
                Object m = testMethods.get(args[i]);
                if (m != null) {
                    testsToRun.addElement(m);
                } else {
                    usage();
                    return;
                }
            }
        }

    	if (params == null) params = new TestParams();
        _run();

        if (params.prompt) {
            System.out.println("Hit RETURN to exit...");
            try {
                System.in.read();
            } catch (IOException e) {
                System.out.println("Exception: " + e.toString() + e.getMessage());
            }
        }
        if (params.nothrow) {
            System.exit(params.errorCount);
        }
    }

	protected void run(TestFmwk childTest) throws Exception {
        run(new TestFmwk[] { childTest });
	}

	protected void run(TestFmwk[] tests) throws Exception {
        for (int i=0; i<tests.length; ++i) {
            tests[i].params = this.params;
            params.indentLevel++;
            tests[i]._run();
            params.indentLevel--;
        }
	}

    protected boolean isVerbose() {
        return params.verbose;
    }

    /**
     * Adds given string to the log if we are in verbose mode.
     */
    public void log( String message ) {
        if( params.verbose ) {
            indent(params.indentLevel + 1);
            params.log.print( message );
            params.log.flush();
        }
    }

    public void logln( String message ) {
        log(message + System.getProperty("line.separator"));
    }

    /**
     * Report an error
     */
    public void err( String message ) {
        params.errorCount++;
        indent(params.indentLevel + 1);
        params.log.print( message );
        params.log.flush();

        if (!params.nothrow) {
            throw new RuntimeException(message);
        }
    }

    public void errln( String message ) {
        err(message + System.getProperty("line.separator"));
    }

    protected int getErrorCount() {
        return params.errorCount;
    }

    protected void writeTestName(String testName) {
        indent(params.indentLevel);
        params.log.print(testName);
        params.log.flush();
        params.needLineFeed = true;
    }

    protected void writeTestResult(int count) {
        if (!params.needLineFeed) {
            indent(params.indentLevel);
            params.log.print("}");
        }
        params.needLineFeed = false;

        if (count != 0) {
            params.log.println(" FAILED (" + count + " failures)");
        } else {
            params.log.println(" Passed");
        }
    }

    private final void indent(int distance) {
        if (params.needLineFeed) {
            params.log.println(" {");
            params.needLineFeed = false;
        }
        params.log.print(spaces.substring(0, distance * 2));
    }

    /**
     * Print a usage message for this test class.
     */
    void usage() {
        System.out.println(getClass().getName() +
                ": [-verbose] [-nothrow] [-prompt] [test names]");

        System.out.println("test names:");
        Enumeration methodNames = testMethods.keys();
        while( methodNames.hasMoreElements() ) {
            System.out.println("\t" + methodNames.nextElement() );
        }
    }

    public static String hex(char ch) {
        StringBuffer result = new StringBuffer();
        String foo = Integer.toString(ch,16).toUpperCase();
        for (int i = foo.length(); i < 4; ++i) {
            result.append('0');
        }
        return result + foo;
    }

    public static String hex(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            if (i != 0) result.append(',');
            result.append(hex(s.charAt(i)));
        }
        return result.toString();
    }

    public static String hex(StringBuffer s) {
        return hex(s.toString());
    }

	private static class TestParams {
    	public boolean   prompt = false;
    	public boolean   nothrow = false;
    	public boolean   verbose = false;

    	public PrintWriter log = new PrintWriter(System.out,true);
    	public int         indentLevel = 0;
    	public boolean     needLineFeed = false;
    	public int         errorCount = 0;
    }

	private TestParams params = null;
    private Hashtable testMethods;
	private Vector testsToRun;
    private final String spaces = "                                          ";
}
