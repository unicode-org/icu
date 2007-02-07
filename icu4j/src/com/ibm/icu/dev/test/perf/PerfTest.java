/*
**********************************************************************
* Copyright (c) 2002-2007, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.dev.tool.UOption;
import com.ibm.icu.impl.LocaleUtility;

/**
 * Base class for performance testing framework. To use, the subclass can simply
 * define one or more instance methods with names beginning with "test" (case
 * ignored). The prototype of the method is
 * 
 * PerfTest.Function testTheName()
 * 
 * The actual performance test will execute on the returned Commond object
 * (refer to Command Pattern). To call a test from command line, the 'test'
 * prefix of the test method name can be ignored/removed.
 * 
 * In addition, the subclass should define a main() method that calls
 * PerfTest.run() as defined here.
 * 
 * If the subclasses uses any command line arguments (beyond those handled
 * automatically by this calss) then it should override PerfTest.setup() to
 * handle its arguments. If the subclasse needs more sophisticated management
 * for controlling finding/calling test method, it can replace the default
 * implementation for PerfTest.testProvider before calling PerfTest.run().
 * 
 * Example invocation: java -cp classes -verbose:gc
 * com.ibm.icu.dev.test.perf.UnicodeSetPerf --gc --passes 4 --iterations 100
 * UnicodeSetAdd [[:l:][:c:]]
 * 
 * Example output: [GC 511K->192K(1984K), 0.0086170 secs] [GC 704K->353K(1984K),
 * 0.0059619 secs] [Full GC 618K->371K(1984K), 0.0242779 secs] [Full GC
 * 371K->371K(1984K), 0.0228649 secs] = testUnicodeSetAdd begin 100 =
 * testUnicodeSetAdd end 11977 1109044 = testUnicodeSetAdd begin 100 =
 * testUnicodeSetAdd end 12047 1109044 = testUnicodeSetAdd begin 100 =
 * testUnicodeSetAdd end 11987 1109044 = testUnicodeSetAdd begin 100 =
 * testUnicodeSetAdd end 11978 1109044
 * 
 * The [] lines are emitted by the JVM as a result of the -verbose:gc switch.
 * 
 * Lines beginning with '=' are emitted by PerfTest: = testUnicodeSetAdd begin
 * 100 A 'begin' statement contains the name of the setup method, which
 * determines what test function is measures, and the number of iterations that
 * will be times. = testUnicodeSetAdd end 12047 1109044 An 'end' statement gives
 * the name of the setup method again, and then two integers. The first is the
 * total elapsed time in milliseconds, and the second is the number of events
 * per iteration. In this example, the time per event is 12047 / (100 * 1109044)
 * or 108.6 ns/event.
 * 
 * Raw times are given as integer ms, because this is what the system measures.
 * 
 * @author Alan Liu
 * @since ICU 2.4
 */
public abstract class PerfTest{
    // Command-line options set these:
    protected boolean        verbose;
    protected String         sourceDir;
    protected String         fileName;
    //protected String         resolvedFileName;
    protected String         encoding;
    protected boolean        uselen;
    protected int            iterations;
    protected int            passes;
    protected int            time;
    protected boolean        line_mode;
    protected boolean        bulk_mode;
    protected Locale         locale;
    protected boolean        doPriorGC;
    protected TestCmdProvider testProvider = new TestPrefixProvider(this);
    
    static interface TestCmdProvider {
        /**
         * @return The names for all available test.
         */
        public Set getAllTestCmdNames();
        
        /**
         * @param name
         * @return Whether the given name is a test name. The implementation may
         *         have more sophisticated naming control here.
         *         TestCmdProvider.isTestCmd() != Set.contains()
         */
        public boolean isTestCmd(String name);
        
        /**
         * @param name
         * @return the test Command or null
         */
        public PerfTest.Function getTestCmd(String name);
    }
    
    /**
     * Treat all method beginning with 'test' prefix (ignoring case)
     * for given object as the test methods.
     */
    static class TestPrefixProvider implements TestCmdProvider{
        private Map theTests = null; // Map<string(no case), string(with case)> 
        private Set orgNames = null; // shadow reference, ==theTests, for better output
        private Object refer;
        
        TestPrefixProvider(Object theProvider){
            refer = theProvider;
        }
        
        public Set getAllTestCmdNames() {
            if (theTests == null) {
                theTests = new HashMap();
                orgNames = new HashSet();
                Method[] methods = refer.getClass().getDeclaredMethods();
                for (int i=0; i<methods.length; i++) {
                    String org = methods[i].getName();
                    String name = org.toLowerCase();    // ignoring case
                    // beginning with 'test'
                    // Note: methods named 'test()' are ignored
                    if (name.length() > 4 && name.startsWith("test")) { 
                        if (theTests.containsKey(name)) {
                            throw new Error("Duplicate method name ignoring case: " + name);
                        }
                        theTests.put(name,org);
                        orgNames.add(org);
                    }
                }
            }
            return orgNames; // beggining with 'test', keeping case
        }
        
        /**
         * The given name will map to a method of the same name, or a method
         * named "test" + name. Case is ignored.
         */
        private String isTestCmd_impl(String name){
            getAllTestCmdNames();
            String tn1 = name.toLowerCase();
            String tn2 = "test" + tn1;
            if (theTests.containsKey(tn1)){
                return tn1;
            } else if (theTests.containsKey(tn2)){
                return tn2;
            }
            return null;
        }
        
        public boolean isTestCmd(String name){
            return isTestCmd_impl(name)!=null;
        }
        
        public Function getTestCmd(String aname){
            String name = (String) theTests.get(isTestCmd_impl(aname));
            if (name == null){
                return null;
            }
            
            try {
                Method m = refer.getClass().getDeclaredMethod(name,null);
                return (Function) m.invoke(refer, new Object[]{});
            } catch (Exception e) {
                throw new Error("TestPrefixProvider implementation error. Finding: " + name,e);
            }
        }
    };
    

    /**
     * Subclasses of PerfTest will need to create subclasses of
     * Function that define a call() method which contains the code to
     * be timed.  They then call setTestFunction() in their "Test..."
     * method to establish this as the current test functor.
     */
    public abstract static class Function {

        /**
         * Subclasses must implement this method to do the action to be
         * measured.
         */
        public abstract void call();

        /**
         * Subclasses may implement this method to return positive
         * integer indicating the number of operations in a single
         * call to this object's call() method.  If subclasses do not
         * override this method, the default implementation returns 1.
         */
        public long getOperationsPerIteration() {
            return 1;
        }

        /**
         * Subclasses may implement this method to return either positive
         * or negative integer indicating the number of events in a single
         * call to this object's call() method.  If subclasses do not
         * override this method, the default implementation returns -1,
         * indicating that events are not applicable to this test.
         * e.g: Number of breaks / iterations for break iterator
         */
        public long getEventsPerIteration() {
            return -1;
        }

        /**
         * Call call() n times in a tight loop and return the elapsed
         * milliseconds.  If n is small and call() is fast the return
         * result may be zero.  Small return values have limited
         * meaningfulness, depending on the underlying VM and OS.
         */
        public final long time(long n) {
            long start, stop;
            start = System.currentTimeMillis();
            while (n-- > 0) {
                call();
            }
            stop = System.currentTimeMillis();
            return stop - start; // ms
        }
    }

    /**
     * Exception indicating a usage error.
     */
    public static class UsageException extends Exception {
        public UsageException(String message) {
            super(message);
        }

        public UsageException() {
            super();
        }
    }

    /**
     * Constructor.
     */
    protected PerfTest() {
    }

    /**
     * Framework method.  Default implementation does not parse any
     * extra arguments.  Subclasses may override this to parse extra
     * arguments.  Subclass implementations should NOT call the base
     * class implementation.
     */
    protected void setup(String[] args) {
        if (args.length > 0) {
            throw new RuntimeException("Extra arguments received");
        }
    }

    /**
     * These must be kept in sync with getOptions().
     */
    static final int HELP1 = 0;
    static final int HELP2 = 1;
    static final int VERBOSE = 2;
    static final int SOURCEDIR = 3;
    static final int ENCODING = 4;
    static final int USELEN = 5;
    static final int FILE_NAME = 6;
    static final int PASSES = 7;
    static final int ITERATIONS = 8;
    static final int TIME = 9;
    static final int LINE_MODE = 10;
    static final int BULK_MODE = 11;
    static final int LOCALE = 12;
    // Options above here are identical to those in C; keep in sync with C
    // Options below here are unique to Java; shift down as necessary
    static final int GARBAGE_COLLECT = 13;
    static final int LIST = 14;

    UOption[] getOptions() {
        return new UOption[] {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.VERBOSE(),
            UOption.SOURCEDIR(),
            UOption.ENCODING(),
            UOption.DEF( "uselen",        'u', UOption.NO_ARG),
            UOption.DEF( "file-name",     'f', UOption.REQUIRES_ARG),
            UOption.DEF( "passes",        'p', UOption.REQUIRES_ARG),
            UOption.DEF( "iterations",    'i', UOption.REQUIRES_ARG),
            UOption.DEF( "time",          't', UOption.REQUIRES_ARG),
            UOption.DEF( "line-mode",     'l', UOption.NO_ARG),
            UOption.DEF( "bulk-mode",     'b', UOption.NO_ARG),
            UOption.DEF( "locale",        'L', UOption.REQUIRES_ARG),

            // Options above here are identical to those in C; keep in sync
            // Options below here are unique to Java

            UOption.DEF( "gc",            'g', UOption.NO_ARG),
            UOption.DEF( "list",     (char)-1, UOption.NO_ARG),
        };
    }

    /**
     * Subclasses should call this method in their main().  run() will
     * in turn call setup() with any arguments it does not parse.
     * This method parses the command line and runs the tests given on
     * the command line, with the given parameters.  See the class
     * description for details.
     */
    protected final void run(String[] args) throws Exception {
        Set testList = parseOptions(args);

        // Run the tests
        for (Iterator iter = testList.iterator(); iter.hasNext();) {
            String meth = (String) iter.next();

            // Call meth to set up the test
           // long eventsPerCall = -1;
            Function testFunction = testProvider.getTestCmd(meth);
            if (testFunction == null) {
                throw new RuntimeException(meth + " failed to return a test function");
            }
            if (testFunction.getOperationsPerIteration() < 1) {
                throw new RuntimeException(meth + " returned an illegal operations/iteration()");
            }

            long t;
            //long b = System.currentTimeMillis();
            long loops = getIteration(meth, testFunction);
            //System.out.println("The guess cost: " + (System.currentTimeMillis() - b)/1000. + " s.");
            
            for (int j=0; j<passes; ++j) {
                long events = -1;
                if (verbose) {
                    if (iterations > 0) {
                        System.out.println("= " + meth + " begin " + iterations);
                    } else {
                        System.out.println("= " + meth + " begin " + time + " seconds");
                    }
                } else {
                    System.out.println("= " + meth + " begin " );
                }

                t = testFunction.time(loops);   //ms
                events = testFunction.getEventsPerIteration();

                if (verbose) {
                    if (events == -1){
                        System.out.println("= " + meth + " end " + (t/1000.0) + " loops: " + loops +
                                                " operations: " + testFunction.getOperationsPerIteration());
                    } else {
                        System.out.println("= " + meth + " end " + (t/1000.0) + " loops: " + loops +
                                                " operations: " + testFunction.getOperationsPerIteration() +" events: " + events);
                    }
                } else {
                    if (events == -1){
                        System.out.println("= " + meth + " end " + (t/1000.0) + " " + loops +
                                                " " + testFunction.getOperationsPerIteration());
                    } else {
                        System.out.println("= " + meth + " end " + (t/1000.0) + " " + loops +
                                                " " + testFunction.getOperationsPerIteration() + " " + events);
                    }
                }

            }
        }
    }

    /**
     * @param args
     * @return                  the method list to call
     * @throws UsageException
     */
    private Set parseOptions(String[] args) throws UsageException {
        
        doPriorGC = false;
        encoding = "";
        uselen = false;
        fileName = null;
        sourceDir = null;
        //lines = null;
        line_mode = false;
        //buffer = null;
        //bufferLen = 0;
        verbose = false;
        bulk_mode = false;
        passes = iterations = time = -1;
        locale = null;

        UOption[] options = getOptions();
        int remainingArgc = UOption.parseArgs(args, options);

        if(args.length==0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            throw new UsageException();
        }

        if(options[VERBOSE].doesOccur) {
            verbose = true;
        }

        if(options[SOURCEDIR].doesOccur) {
            sourceDir = options[SOURCEDIR].value;
        }

        if(options[ENCODING].doesOccur) {
            encoding = options[ENCODING].value;
        }

        if(options[USELEN].doesOccur) {
            uselen = true;
        }

        if(options[FILE_NAME].doesOccur){
            fileName = options[FILE_NAME].value;
        }

        if (options[TIME].doesOccur && options[ITERATIONS].doesOccur) {
            throw new UsageException("Cannot specify both '-t time' and '-i iterations'");
        }

        if (!options[TIME].doesOccur && !options[ITERATIONS].doesOccur) {
            throw new UsageException("Either '-t time' or '-i iteration' must be specified");
        }
        
        if(options[PASSES].doesOccur) {
            passes = Integer.parseInt(options[PASSES].value);
        } else {
            throw new UsageException("'-p pass' must be specified");
        }

        if(options[ITERATIONS].doesOccur) {
            iterations = Integer.parseInt(options[ITERATIONS].value);
        }

        if(options[TIME].doesOccur) {
            time = Integer.parseInt(options[TIME].value);
        }

        if (options[LINE_MODE].doesOccur && options[BULK_MODE].doesOccur) {
            throw new UsageException("Cannot specify both line mode and bulk mode");
        }

        if(options[LINE_MODE].doesOccur) {
            line_mode = true;
            bulk_mode = false;
        }

        if(options[BULK_MODE].doesOccur) {
            bulk_mode = true;
            line_mode = false;
        }

        if(options[LOCALE].doesOccur) {
            locale = LocaleUtility.getLocaleFromName(options[LOCALE].value);
        }

        if(options[GARBAGE_COLLECT].doesOccur) {
            doPriorGC = true;
        }

        if (options[LIST].doesOccur){
            System.err.println("Available tests:");
            Set testNames = testProvider.getAllTestCmdNames();
            for (Iterator iter = testNames.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                System.err.println(" " + name);
            }
            System.exit(0);
        }

        Set testList = new HashSet();
        int i, j;
        for (i=0; i<remainingArgc; ++i) {

            // is args[i] a method name?
            if (testProvider.isTestCmd(args[i])){
                testList.add(args[i]);
            } else {
                // args[i] is neither a method name nor a number.  Pass
                // everything from here on through to the subclass via
                // setup().
                break;
            }
        }

        if (testList.size() < 1) { // run all tests
            Set testNames = testProvider.getAllTestCmdNames();
            for (Iterator iter = testNames.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                testList.add(name);
            }
        }

        // Pass remaining arguments, if any, through to the subclass
        // via setup() method.
        String[] subclassArgs = new String[remainingArgc - i];
        for (j=0; i<remainingArgc; ++j) {
            subclassArgs[j] = args[i++];
        }
        setup(subclassArgs);

        if (doPriorGC) {
            // Put the heap in a consistent state
            gc();
        }
        return testList;
    }

    /**
     * Translate '-t time' to iterations (or just return '-i iteration')
     * 
     * @param meth
     * @param fn
     * @return
     */
    private long getIteration(String methName, Function fn) {
        long iter = 0;
        if(time < 0) { // && iterations > 0
            iter = iterations;
        } else {  // && iterations < 0
            // Translate time to iteration
            // Assuming there is a linear relation between time and iterations

            if(verbose){
                System.out.println("= " + methName + " calibrating " + time + " seconds" );
            }

            long base = time * 1000;
//            System.out.println("base :" + base);
            long seed = 1;               
            long t=0;
            while (t < base * 0.9 || base * 1.1 < t) { // + - 10%  
                if (iter == 0 || t == 0) {
                    iter = seed;    // start up from 1
                    seed *= 100;    // if the method is too fast (t == 0), multiply 100 times
                    // 100 is rational because 'base' is always larger than 1000
                } else {
                    // If 't' is large enough, use linear function to calculate new iteration
                    //
                    // new iter(base)    old iter
                    // -------------- = -------- = k
                    // new time          old time
                    //
//                    System.out.println("before guess t: " + t);
//                    System.out.println("before guess iter: " + iter);
                    iter = (long) ((double)iter / t * base) ;  // avoid long cut, eg. 1/10 == 0
                    if (iter == 0) {
                        throw new RuntimeException("Unable to converge on desired duration");
                    }
                }
                t = fn.time(iter);
            }
//            System.out.println("final t : " + t);
//            System.out.println("final i : " + iter);
        }
        return iter;    
    }

    /**
     * Invoke the runtime's garbage collection procedure repeatedly
     * until the amount of free memory stabilizes to within 10%.
     */
    protected void gc() {
        if (false) {
            long last;
            long free = 1;
            Runtime runtime = Runtime.getRuntime();
            do {
                runtime.gc();
                last = free;
                free = runtime.freeMemory();
            } while (((double)Math.abs(free - last)) / free > 0.1);
            // Wait for the change in free memory to drop under 10%
            // between successive calls.
        }

        // From "Java Platform Performance".  This is the procedure
        // recommended by Javasoft.
        try {
            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);

            System.gc();
            Thread.sleep(100);
            System.runFinalization();
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }


    /*
     * Private utility to convert a List of Integer objects to int[].
     */
    /*private static int[] toIntArray(List list) {
        int[] result = new int[list.size()];
        for (int i=0; i<list.size(); ++i) {
            result[i] = ((Integer) list.get(i)).intValue();
        }
        return result;
    }*/
    
    public static char[] readToEOS(InputStreamReader stream) {
        ArrayList vec = new ArrayList();
        int count = 0;
        int pos = 0;
        final int MAXLENGTH = 0x8000; // max buffer size - 32K
        int length = 0x80; // start with small buffers and work up
        do {
            pos = 0;
            length = length >= MAXLENGTH ? MAXLENGTH : length * 2;
            char[] buffer = new char[length];
            try {
                do {
                    int n = stream.read(buffer, pos, length - pos);
                    if (n == -1) {
                    break;
                    }
                    pos += n;
                } while (pos < length);
            }
            catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);

        char[] data = new char[count];
        pos = 0;
        for (int i = 0; i < vec.size(); ++i) {
            char[] buf = (char[])vec.get(i);
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
    }
    public static byte[] readToEOS(InputStream stream) {

        ArrayList vec = new ArrayList();
        int count = 0;
        int pos = 0;
        final int MAXLENGTH = 0x8000; // max buffer size - 32K
        int length = 0x80; // start with small buffers and work up
        do {
            pos = 0;
            length = length >= MAXLENGTH ? MAXLENGTH : length * 2;
            byte[] buffer = new byte[length];
            try {
                do {
                    int n = stream.read(buffer, pos, length - pos);
                    if (n == -1) {
                    break;
                    }
                    pos += n;
                } while (pos < length);
            }
            catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);


        byte[] data = new byte[count];
        pos = 0;
        for (int i = 0; i < vec.size(); ++i) {
            byte[] buf = (byte[])vec.get(i);
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
    }

    public String[] readLines(String fileName, String encoding, boolean bulkMode) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(fileName);
            isr = new InputStreamReader(fis, encoding);
            br= new BufferedReader(isr);
        } catch (Exception e) {
            System.err.println("Error: File access exception: " + e.getMessage() + "!");
            System.exit(1);
        }
        ArrayList list = new ArrayList();
        while (true) {
            String line = null;
            try {
                line = readDataLine(br);
            } catch (Exception e) {
                System.err.println("Read File Error" + e.getMessage() + "!");
                System.exit(1);
            }
            if (line == null) break;
            if (line.length() == 0) continue;
            list.add(line);
        }

        int size = list.size();
        String[] lines = null;

        if (bulkMode) {
            lines = new String[1];
            StringBuffer buffer = new StringBuffer("");
            for (int i = 0; i < size; ++i) {
                buffer.append((String) list.get(i));
                /*if (i < (size - 1)) {
                    buffer.append("\r\n");
                }*/
            }
            lines[0] = buffer.toString();
        } else {
            lines = new String [size];
            for (int i = 0; i < size; ++i) {
                lines[i] = (String) list.get(i);
            }
        }

        return lines;
    }

    public String readDataLine(BufferedReader br) throws Exception {
        String originalLine = "";
        String line = "";
        try {
            line = originalLine = br.readLine();
            if (line == null) return null;
            if (line.length() > 0 && line.charAt(0) == 0xFEFF) line = line.substring(1);
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) line = line.substring(0, commentPos);
            line = line.trim();
        } catch (Exception e) {
            throw new Exception("Line \"{0}\",  \"{1}\"" + originalLine + " "
                                + line + " " + e.toString());
        }
        return line;
    }

}

//eof
