/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestFmwk.java,v $
 * $Date: 2003/02/01 00:51:30 $
 * $Revision: 1.35 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

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
    }

    protected TestFmwk getObject(String name) throws Exception{
        Class cl = Class.forName(name);
        return (TestFmwk)cl.newInstance();
    }
    
    private Hashtable moduleHashtable= null;
    
    protected void setModuleHash(Hashtable hashtable){
        moduleHashtable = hashtable;
    }
    /**
     * Default is to create a hashmap containing all the test methods.
     * Data-driven tests can override to provide a different mapping
     * of test names to test methods, and a different list.  If this
     * test suite is invalid, subclassers should return an empty
     * collection.
     */
    protected Map getAvailableTests() {
        Map result = Collections.EMPTY_MAP;
        if (validate()) {
            result = new HashMap();
            Method[] methods = getClass().getDeclaredMethods();
            for( int i=0; i<methods.length; i++ ) {
                if( methods[i].getName().startsWith("Test")
                    || methods[i].getName().startsWith("test")) {
                    result.put(methods[i].getName(), methods[i] );
                }
            }
        }
        return result;
    }

    private Map getTestsToRun(Set testNames) {
        Map methodsToRun = getAvailableTests();
        if (!(testNames == null  || testNames.isEmpty())) {
            methodsToRun.keySet().retainAll(testNames);
        }
        return methodsToRun;
    }

    private Iterator getTestEntryIterator(Map testsToRun) {
        TreeMap sortedMethods = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        sortedMethods.putAll(testsToRun);
        return sortedMethods.entrySet().iterator();
    }

    private void _run() throws Exception {
        _run(getTestsToRun(null));
    }

    private void _run(Map testsToRun) throws Exception {
        writeTestName(getClass().getName());
        params.indentLevel++;
        int oldClassCount = params.errorCount;
        int oldClassInvalidCount = params.invalidCount;

        if (validate()) {
            Iterator iter = getTestEntryIterator(testsToRun);

            // Run the list of tests given in the test arguments
        final Object[] NO_ARGS = new Object[0];
            while (iter.hasNext()) {
                int oldCount = params.errorCount;
                int oldInvalidCount = params.invalidCount;

                Map.Entry entry = (Map.Entry)iter.next();
        String testName = (String)entry.getKey();
                Method testMethod = (Method)entry.getValue();

                writeTestName(testName);

        if (validateMethod(testName)) {
            try {
            testMethod.invoke(this, NO_ARGS);
            } catch( IllegalAccessException e ) {
            errln("Can't access test method " + testName);
            } catch( InvocationTargetException e ) {
            errln("Uncaught exception \"" + e
                              +"\" thrown in test method " + testMethod.getName()
                              +" accessed under name " + testName);
            e.getTargetException().printStackTrace(this.params.log);
            }
        } else {
            params.invalidCount++;
        }
                writeTestResult(params.errorCount - oldCount, params.invalidCount - oldInvalidCount);
            }
        } else {
            params.invalidCount++;
        }
        params.indentLevel--;

        writeTestResult(params.errorCount - oldClassCount, params.invalidCount - oldClassInvalidCount);
    }

    public void run(String[] args) throws Exception {
        if (params == null) params = new TestParams();

        // Parse the test arguments.  They can be either the flag
        // "-verbose" or names of test methods. Create a list of
        // tests to be run.
    boolean usageError = false;
        Set testNames = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-verbose") || args[i].equals("-v")) {
                params.verbose = true;
            }
            else if (args[i].equals("-prompt")) {
                params.prompt = true;
            } else if (args[i].equals("-nothrow") || args[i].equals("-n")) {
                params.nothrow = true;
    	    } else if (args[i].equals("-describe")) {
    		    params.describe = true;
            } else if (args[i].startsWith("-e")) {
                // see above
                params.inclusion = (args[i].length() == 2) ? 5 : Integer.parseInt(args[i].substring(2));
                if (params.inclusion < 0 || params.inclusion > 10) {
                    usageError = true;
                }
            } else if (args[i].toLowerCase().startsWith("-filter:")) {
                params.filter = args[i].substring(8);
            }else if(args[i].equals("-withoutdata")|| args[i].equals("-w")){
            	params.withoutData = true;
        	} else if ( args[i].equals("-module") || args[i].equals("-m")){
                if(moduleHashtable!= null && i+1 < args.length){
                    Object o = moduleHashtable.get(args[++i]);
                    if (testNames == null) {
                        testNames = new TreeSet();
                    }
                    if(o instanceof String){
                        testNames.add((String) o);
                    }else if(o instanceof String[]){
                        String[] arr = (String[]) o;
                        for(int j=0;j<arr.length;j++){
                            testNames.add(arr[j]);
                        }
                    }
                } 
            } else {
                if (testNames == null) {
                    testNames = new TreeSet();
                }
                testNames.add(args[i]);
            }
        }

        Map testsToRun = null;
        if (!usageError) {
            testsToRun = getTestsToRun(testNames);
        }
        if (usageError ||
            (testNames != null && testsToRun.size() != testNames.size())) {
        usage();
        return;
    }

        _run(testsToRun);

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

    /**
     * Return true if we can run this test (allows test to inspect jvm, environment, params before running)
     */
    protected boolean validate() {
        return true;
    }

    protected String getDescription() {
    return null;
    }

    protected boolean validateMethod(String name) {
    return true;
    }

    protected String getMethodDescription(String name) {
    return null;
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
     * 0 = fewest tests, 5 is normal build, 10 is most tests
     */
    public int getInclusion() {
        return params.inclusion;
    }

    public boolean isQuick() {
        return params.inclusion == 0;
    }

    public String getFilter() {
        return params.filter;
    }

    /**
     * Adds given string to the log if we are in verbose mode.
     */
    public void log( String message ) {
        log(message, true, false, false);
    }

    public void logln( String message ) {
        logln(message, true, false);
    }

    /**
     * Adds given string to the log.
     */
    public void info( String message ) {
        message = "INFO: " + message;
        log(message,false,false,true);
    }

    public void infoln( String message ) {
        message = " INFO: " + message;
        log(message + System.getProperty("line.separator"), false, false,true);
        params.suppressIndent = false; // time to indent again
    }

    /**
     * Add a given string to the log.
     * @param message text to add
     * @param pass if true and if in verbose mode, or if false, then add
     * the text; otherwise suppress it
     * @param incrementCount if pass if false and incrementCount is true,
     * then increment the failure count; if pass is true, then this param
     * is ignored
     */
    public void log( String message, boolean pass, boolean incrementCount,boolean isInfo ) {
        if(!isInfo){
            if (!pass && incrementCount) {
                if(params==null) {params = new TestParams();}
                params.errorCount++;
            }

            if (!pass || params.verbose) {
                if (!params.suppressIndent) indent(params.indentLevel + 1);
                params.log.print( message );
                params.log.flush();
            }

            if (!pass && !params.nothrow) {
                throw new RuntimeException(message);
            }
        }else{
            if (!params.suppressIndent) indent(params.indentLevel + 1);
            params.log.print( message );
            params.log.flush();
        }
        params.suppressIndent = true; // don't indent on successive calls to log()

    }

    public void logln( String message, boolean pass, boolean incrementCount ) {
        log(message + System.getProperty("line.separator"), pass, incrementCount,false);
        params.suppressIndent = false; // time to indent again
    }

    /**
     * Convenience overloads
     */
    public void log( String message, boolean pass ) {
        log(message, pass, true, false);
    }

    public void logln( String message, boolean pass ) {
        logln(message, pass, true);
    }

    /**
     * Report an error
     */
    public void err( String message ) {
        log(message, false, true, false);
    }

    public void errln( String message ) {
        logln(message, false, true);
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

    protected void writeTestResult(int failCount, int invalidCount) {
        if (!params.needLineFeed) {
            indent(params.indentLevel);
            params.log.print("}");
        }
        params.needLineFeed = false;

        if (failCount != 0) {
            params.log.println(" FAILED (" + failCount + " failures" +
                               ((invalidCount != 0) ?
                                ", " + invalidCount + " tests skipped)" :
                                ")"));
        } else if (invalidCount != 0) {
            params.log.println(" Qualified (" + invalidCount + " tests skipped)");
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
                           ": [-verbose] [-nothrow] [-prompt]\n"+
                           "                              [-describe] [-e<n>] [-filter:<str>]\n"+
                           "                              [-m <module name>] [-w[ithoutdata] [test names]");
        System.out.println("Options:");
        System.out.println(" -v[erbose] Show all output");
        System.out.println(" -n[othrow] Message on test failure rather than exception");
        System.out.println(" -prompt Prompt before exiting");
        System.out.println(" -describe ?");
        System.out.println(" -e<n> Set exhaustiveness from 0..10.  Default is 0, fewest tests.\n       To run all tests, specify -e10.  Giving -e with no <n> is\n       the same as -e5.");
        System.out.println(" -filter:<str> ?");
        System.out.println(" -m[odule] <module name>");
		System.out.println(" -w[ithoutdata] make tests pass even though data is missing");
        boolean valid = params.describe && validate();
        if (valid) {
            String testDescription = getDescription();
            if (testDescription != null) {
            System.out.println("-- " + testDescription);
            }
        }

        Iterator testEntries = getTestEntryIterator(getAvailableTests());

        System.out.println("Test names:");
        while(testEntries.hasNext() ) {
            Map.Entry e = (Map.Entry)testEntries.next();
	        String methodName = (String)e.getKey();


            System.out.print("\t" + methodName );
    	    if (valid) {
        		String methodDescription = getMethodDescription(methodName);
        		if (methodDescription != null) {
        		    System.out.print(" -- " + methodDescription);
        		}
    	    }
    	    System.out.println();
        }
        if(moduleHashtable != null){
            System.out.println("Module names:");
            Enumeration keys = moduleHashtable.keys();
            while(keys.hasMoreElements() ) {
                String moduleName = (String)keys.nextElement();
                System.out.print("\t" + moduleName );

                System.out.println();
            }

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

    public static String hex(int ch) {
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

	/**
     * Returns true if ICU is built modularly without locale data
     */
    public boolean isModularBuild(){
        return params.withoutData;
    }



    private static class ASCIIWriter extends PrintWriter {
        private Writer w;
        private StringBuffer buffer = new StringBuffer();

        // Characters that we think are printable but that escapeUnprintable
        // doesn't
        private static final String S ="\u0009"+(char)0x000A+(char)0x000D;

        public ASCIIWriter(Writer w, boolean autoFlush) {
            super(w, autoFlush);
        }

        public ASCIIWriter(OutputStream os, boolean autoFlush) {
            super(os, autoFlush);
        }

        public void write(int c) {
            synchronized(lock) {
                buffer.setLength(0);
                if (S.indexOf(c)<0 && TestUtil.escapeUnprintable(buffer, c)) {
                    super.write(buffer.toString());
                } else {
                    super.write(c);
                }
            }
        }

        public void write(char[] buf, int off, int len) {
            synchronized (lock) {
                buffer.setLength(0);
                int limit = off + len;
                while (off < limit) {
                    int c = UTF16Util.charAt(buf, 0, buf.length, off);
                    off += UTF16Util.getCharCount(c);
                    if (S.indexOf(c)<0 && TestUtil.escapeUnprintable(buffer, c)) {
                        super.write(buffer.toString());
                        buffer.setLength(0);
                    } else {
                        super.write(c);
                    }
                }
            }
        }

        public void write(String s, int off, int len) {
            write(s.substring(off, off + len).toCharArray(), 0, len);
        }
    }

    private static class TestParams {
        public boolean   prompt = false;
        public boolean   nothrow = false;
        public boolean   verbose = false;
    	public boolean   describe = false;
    	public boolean   withoutData = false; 
        public int       inclusion = 0;
        public String    filter = null;

        public PrintWriter log = new ASCIIWriter(System.out, true);
        public int         indentLevel = 0;
        public boolean     needLineFeed = false;
        public boolean     suppressIndent = false;
        public int         errorCount = 0;
        public int         invalidCount = 0;
    }

    private TestParams params = null;
    private Map testMethods;
    private Set testsToRun;
    private final String spaces = "                                          ";
}
