// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 **********************************************************************
 * Copyright (c) 2002-2008, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package com.ibm.icu.dev.test.perf;

import com.ibm.icu.impl.LocaleUtility;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Base class for performance testing framework. To use, the subclass can simply define one or more
 * instance methods with names beginning with "test" (case ignored). The prototype of the method is
 * {@code PerfTest.Function testTheName()}
 *
 * <p>The actual performance test will execute on the returned Command object (refer to Command
 * Pattern). To call a test from command line, the 'test' prefix of the test method name can be
 * ignored/removed.
 *
 * <p>In addition, the subclass should define a main() method that calls PerfTest.run() as defined
 * here.
 *
 * <p>If the subclasses use any command line arguments (beyond those handled automatically by this
 * class) then it should override PerfTest.setup() to handle its arguments. If the subclasses need
 * more sophisticated management for controlling finding/calling test method, it can replace the
 * default implementation for PerfTest.testProvider before calling PerfTest.run().
 *
 * <p>Example invocation: java -cp classes -verbose:gc com.ibm.icu.dev.test.perf.UnicodeSetPerf --gc
 * --passes 4 --iterations 100 UnicodeSetAdd [[:l:][:c:]]
 *
 * <p>Example output: [GC 511K->192K(1984K), 0.0086170 secs] [GC 704K->353K(1984K), 0.0059619 secs]
 * [Full GC 618K->371K(1984K), 0.0242779 secs] [Full GC 371K->371K(1984K), 0.0228649 secs] =
 * testUnicodeSetAdd begin 100 = testUnicodeSetAdd end 11977 1109044 = testUnicodeSetAdd begin 100 =
 * testUnicodeSetAdd end 12047 1109044 = testUnicodeSetAdd begin 100 = testUnicodeSetAdd end 11987
 * 1109044 = testUnicodeSetAdd begin 100 = testUnicodeSetAdd end 11978 1109044
 *
 * <p>The [] lines are emitted by the JVM as a result of the -verbose:gc switch.
 *
 * <p>Lines beginning with '=' are emitted by PerfTest: = testUnicodeSetAdd begin 100 A 'begin'
 * statement contains the name of the setup method, which determines what test function is measures,
 * and the number of iterations that will be times. = testUnicodeSetAdd end 12047 1109044 An 'end'
 * statement gives the name of the setup method again, and then two integers. The first is the total
 * elapsed time in milliseconds, and the second is the number of events per iteration. In this
 * example, the time per event is 12047 / (100 * 1109044) or 108.6 ns/event.
 *
 * <p>Raw times are given as integer ms, because this is what the system measures.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public abstract class PerfTest {
    // Command-line options set these:
    protected boolean verbose;
    protected String sourceDir;
    protected String fileName;

    // protected String resolvedFileName;
    protected String encoding;
    protected String testName;
    protected boolean uselen;
    protected int iterations;
    protected int passes;
    protected int time;
    protected boolean line_mode;
    protected boolean bulk_mode;
    protected Locale locale;
    protected boolean doPriorGC;
    protected int threads;
    protected int duration;
    protected boolean action;

    protected TestCmdProvider testProvider = new TestPrefixProvider(this);

    interface TestCmdProvider {
        /**
         * @return The names for all available test.
         */
        Set<String> getAllTestCmdNames();

        /**
         * @param name
         * @return Whether the given name is a test name. The implementation may have more
         *     sophisticated naming control here. TestCmdProvider.isTestCmd() != Set.contains()
         */
        boolean isTestCmd(String name);

        /**
         * @param name
         * @return the test Command or null
         */
        PerfTest.Function getTestCmd(String name);
    }

    /**
     * Treat all method beginning with 'test' prefix (ignoring case) for given object as the test
     * methods.
     */
    static class TestPrefixProvider implements TestCmdProvider {
        private Map<String, String> theTests = null; // Map<string(no case), string(with case)>
        private Set<String> orgNames = null; // shadow reference, ==theTests, for better output
        private Object refer;

        TestPrefixProvider(Object theProvider) {
            refer = theProvider;
        }

        @Override
        public Set<String> getAllTestCmdNames() {
            if (theTests == null) {
                theTests = new HashMap<>();
                orgNames = new HashSet<>();
                for (Method method : refer.getClass().getDeclaredMethods()) {
                    String org = method.getName();
                    String name = org.toLowerCase(); // ignoring case
                    // beginning with 'test'
                    // Note: methods named 'test()' are ignored
                    if (name.length() > 4 && name.startsWith("test")) {
                        if (theTests.containsKey(name)) {
                            throw new Error("Duplicate method name ignoring case: " + name);
                        }
                        theTests.put(name, org);
                        orgNames.add(org);
                    }
                }
            }
            return orgNames; // beginning with 'test', keeping case
        }

        /**
         * The given name will map to a method of the same name, or a method named "test" + name.
         * Case is ignored.
         */
        private String isTestCmd_impl(String name) {
            getAllTestCmdNames();
            String tn1 = name.toLowerCase();
            String tn2 = "test" + tn1;
            if (theTests.containsKey(tn1)) {
                return tn1;
            } else if (theTests.containsKey(tn2)) {
                return tn2;
            }
            return null;
        }

        @Override
        public boolean isTestCmd(String name) {
            return isTestCmd_impl(name) != null;
        }

        @Override
        public Function getTestCmd(String aname) {
            String name = theTests.get(isTestCmd_impl(aname));
            if (name == null) {
                return null;
            }

            try {
                Method m = refer.getClass().getDeclaredMethod(name, (Class<?>[]) null);
                return (Function) m.invoke(refer, new Object[] {});
            } catch (Exception e) {
                throw new Error("TestPrefixProvider implementation error. Finding: " + name, e);
            }
        }
    }

    /**
     * Subclasses of PerfTest will need to create subclasses of Function that define a call() method
     * which contains the code to be timed. They then call setTestFunction() in their "Test..."
     * method to establish this as the current test functor.
     */
    public abstract static class Function {

        /**
         * Subclasses should implement this method to do the action to be measured if the action is
         * thread-safe
         */
        public void call() {
            call(0);
        }

        /** Subclasses should implement this method if the action is not thread-safe */
        public void call(int i) {
            call();
        }

        /**
         * Subclasses may implement this method to return positive integer indicating the number of
         * operations in a single call to this object's call() method. If subclasses do not override
         * this method, the default implementation returns 1.
         */
        public long getOperationsPerIteration() {
            return 1;
        }

        /**
         * Subclasses may implement this method to return either positive or negative integer
         * indicating the number of events in a single call to this object's call() method. If
         * subclasses do not override this method, the default implementation returns -1, indicating
         * that events are not applicable to this test. e.g: Number of breaks / iterations for break
         * iterator
         */
        public long getEventsPerIteration() {
            return -1;
        }

        /**
         * Call call() n times in a tight loop and return the elapsed milliseconds. If n is small
         * and call() is fast the return result may be zero. Small return values have limited
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

        /** init is called each time before looping through call */
        public void init() {}

        public final int getID() {
            return id;
        }

        public final void setID(int id) {
            this.id = id;
        }

        private int id;
    }

    private class FunctionRunner implements Runnable {
        private final Function f;
        private final long loops;
        private final int id;

        public FunctionRunner(Function f, long loops, int id) {
            this.f = f;
            this.loops = loops;
            this.id = id;
        }

        @Override
        public void run() {
            long n = loops;
            while (n-- > 0) f.call(id);
        }
    }

    /** Exception indicating a usage error. */
    public static class UsageException extends Exception {
        /** For serialization */
        private static final long serialVersionUID = -1201256240606806242L;

        public UsageException(String message) {
            super(message);
        }

        public UsageException() {
            super();
        }
    }

    /** Constructor. */
    protected PerfTest() {}

    /**
     * Framework method. Default implementation does not parse any extra arguments. Subclasses may
     * override this to parse extra arguments. Subclass implementations should NOT call the base
     * class implementation.
     */
    protected void setup(String[] args) {
        if (args.length > 0) {
            throw new RuntimeException("Extra arguments received");
        }
    }

    /** These are to be used in Options. */
    static final String HELP = "help";

    static final String VERBOSE = "verbose";
    static final String SOURCEDIR = "sourcedir";
    static final String ENCODING = "encoding";
    static final String USELEN = "uselen";
    static final String FILE_NAME = "filename";
    static final String PASSES = "passes";
    static final String ITERATIONS = "iterations";
    static final String TIME = "time";
    static final String LINE_MODE = "line-mode";
    static final String BULK_MODE = "bulk-mode";
    static final String LOCALE = "locale";
    static final String TEST_NAME = "testname";
    static final String THREADS = "threads";
    static final String DURATION = "duration";
    static final String ACTION = "action";
    // Options above here are identical to those in C; keep in sync with C
    // Options below here are unique to Java; shift down as necessary
    static final String GARBAGE_COLLECT = "gc";
    static final String LIST = "list";

    static final Options OPTIONS =
            new Options()
                    .addOption(Option.builder("h").longOpt(HELP).desc("Print this message").build())
                    .addOption(Option.builder("?").longOpt(HELP).desc("Print this message").build())
                    .addOption(
                            Option.builder("v")
                                    .longOpt(VERBOSE)
                                    .desc("Enable verbose output")
                                    .build())
                    .addOption(
                            Option.builder("s")
                                    .longOpt(SOURCEDIR)
                                    .hasArg()
                                    .argName("source_dir")
                                    .desc("Source directory for files followed by path")
                                    .build())
                    .addOption(
                            Option.builder("e")
                                    .longOpt(ENCODING)
                                    .hasArg()
                                    .argName("encoding")
                                    .desc("Encoding of source files")
                                    .build())
                    .addOption(
                            Option.builder("u")
                                    .longOpt(USELEN)
                                    .desc(
                                            "Use API with string lengths. Default is null-terminated strings.")
                                    .build())
                    .addOption(
                            Option.builder("f")
                                    .longOpt(FILE_NAME)
                                    .hasArg()
                                    .argName("file_name")
                                    .desc("File to be used as test data")
                                    .build())
                    .addOption(
                            Option.builder("p")
                                    .longOpt(PASSES)
                                    .hasArg()
                                    .argName("passes")
                                    .type(Integer.class)
                                    .desc("Number of passes")
                                    .build())
                    .addOption(
                            Option.builder("i")
                                    .longOpt(ITERATIONS)
                                    .hasArg()
                                    .argName("iterations")
                                    .type(Integer.class)
                                    .desc("Number of iterations")
                                    .build())
                    .addOption(
                            Option.builder("t")
                                    .longOpt(TIME)
                                    .hasArg()
                                    .argName("seconds")
                                    .type(Integer.class)
                                    .desc("Run tests for a certain time (?)")
                                    .build())
                    .addOption(
                            Option.builder("l")
                                    .longOpt(LINE_MODE)
                                    .desc("Normalize file one line at a time")
                                    .build())
                    .addOption(
                            Option.builder("b")
                                    .longOpt(BULK_MODE)
                                    .desc("Normalize whole file at once")
                                    .build())
                    .addOption(
                            Option.builder("L")
                                    .longOpt(LOCALE)
                                    .hasArg()
                                    .argName("locale")
                                    .desc("ICU locale to use. Default is en_US.")
                                    .build())
                    .addOption(
                            Option.builder("T")
                                    .longOpt(TEST_NAME)
                                    .hasArg()
                                    .argName("test_name")
                                    .desc("Test to run")
                                    .build())
                    .addOption(
                            Option.builder("r")
                                    .longOpt(THREADS)
                                    .hasArg()
                                    .argName("threads")
                                    .type(Integer.class)
                                    .desc("Number of threads")
                                    .build())
                    .addOption(
                            Option.builder("d")
                                    .longOpt(DURATION)
                                    .hasArg()
                                    .argName("duration")
                                    .type(Integer.class)
                                    .desc("Run tests for a certain duration (?)")
                                    .build())
                    .addOption(
                            Option.builder("a")
                                    .longOpt(ACTION)
                                    .desc(
                                            "If test is invoked on command line, includes GitHub Action")
                                    .build())
                    // Options above here are identical to those in C; keep in sync
                    // Options below here are unique to Java
                    .addOption(
                            Option.builder("g")
                                    .longOpt(GARBAGE_COLLECT)
                                    .desc("Garbage collect")
                                    .build())
                    .addOption(
                            Option.builder("ls")
                                    .longOpt(LIST)
                                    .desc("List available tests")
                                    .build());

    Options getOptions() {
        return OPTIONS;
    }

    /**
     * Subclasses should call this method in their main(). run() will in turn call setup() with any
     * arguments it does not parse. This method parses the command line and runs the tests given on
     * the command line, with the given parameters. See the class description for details.
     */
    protected final void run(String[] args) throws Exception {
        Set<String> testList = parseOptions(args);

        // Run the tests
        for (String meth : testList) {
            // Call meth to set up the test
            // long eventsPerCall = -1;
            Function testFunction = testProvider.getTestCmd(meth);
            if (testFunction == null) {
                throw new RuntimeException(meth + " failed to return a test function");
            }
            long ops = testFunction.getOperationsPerIteration();
            if (ops < 1) {
                throw new RuntimeException(meth + " returned an illegal operations/iteration()");
            }

            long min_t = 1000000;
            long t;
            // long b = System.currentTimeMillis();
            long calibration_iter = getIteration(meth, testFunction);
            // System.out.println("The guess cost: " + (System.currentTimeMillis() - b)/1000. + "
            // s.");

            // Calculate iterations for the specified duration/pass.
            double timePerIter =
                    performLoops(testFunction, calibration_iter) / 1000. / calibration_iter;
            long iterationCount = (long) (duration / timePerIter + 0.5);

            for (int j = 0; j < passes; ++j) {
                if (verbose) {
                    if (iterations > 0) {
                        System.out.println("= " + meth + " begin " + iterations);
                    } else {
                        System.out.println("= " + meth + " begin " + time + " seconds");
                    }
                } else if (!action) {
                    System.out.println("= " + meth + " begin ");
                }

                t = performLoops(testFunction, iterationCount);
                if (t < min_t) {
                    min_t = t;
                }
                long events = testFunction.getEventsPerIteration();

                if (verbose) {
                    if (events == -1) {
                        System.out.println(
                                "= "
                                        + meth
                                        + " end "
                                        + (t / 1000.0)
                                        + " loops: "
                                        + iterationCount
                                        + " operations: "
                                        + ops);
                    } else {
                        System.out.println(
                                "= "
                                        + meth
                                        + " end "
                                        + (t / 1000.0)
                                        + " loops: "
                                        + iterationCount
                                        + " operations: "
                                        + ops
                                        + " events: "
                                        + events);
                    }
                } else if (!action) {
                    if (events == -1) {
                        System.out.println(
                                "= "
                                        + meth
                                        + " end "
                                        + (t / 1000.0)
                                        + " "
                                        + iterationCount
                                        + " "
                                        + ops);
                    } else {
                        System.out.println(
                                "= "
                                        + meth
                                        + " end "
                                        + (t / 1000.0)
                                        + " "
                                        + iterationCount
                                        + " "
                                        + ops
                                        + " "
                                        + events);
                    }
                }
            }
            if (action) {
                // Print results in ndjson format for GHA Benchmark to process.
                System.out.println(
                        "{\"biggerIsBetter\":false,\"name\":\""
                                + meth
                                + "\",\"unit\":\"ns/iter\",\"value\":"
                                + (min_t * 1E6) / (iterationCount * ops)
                                + "}");
            }
        }
    }

    /**
     * @param args command line arguments to parse
     * @return the method list to call
     * @throws UsageException parameters parsed, but semantically incorrect or conflicting
     * @throws ParseException parameters cannot be parsed
     */
    private Set<String> parseOptions(String[] args) throws UsageException, ParseException {

        doPriorGC = false;
        encoding = "";
        uselen = false;
        fileName = null;
        sourceDir = null;
        line_mode = false;
        verbose = false;
        bulk_mode = false;
        passes = iterations = time = -1;
        locale = null;
        testName = null;
        threads = 1;
        duration = 10; // Default used by Perl scripts
        action = false; // If test is invoked on command line, includes GitHub Action

        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(getOptions(), args);

        if (args.length == 0 || cli.hasOption(HELP)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(this.getClass().getSimpleName(), OPTIONS);
            System.exit(0);
        }

        if (cli.hasOption(LIST)) {
            System.err.println("Available tests:");
            Set<String> testNames = testProvider.getAllTestCmdNames();
            for (String name : testNames) {
                System.err.println(" " + name);
            }
            System.exit(0);
        }

        if (cli.hasOption(TIME) && cli.hasOption(ITERATIONS)) {
            throw new UsageException("Cannot specify both '-t <seconds>' and '-i <iterations>'");
        } else if (!cli.hasOption(TIME) && !cli.hasOption(ITERATIONS)) {
            throw new UsageException(
                    "Either '-t <seconds>' or '-i <iterations>' must be specified");
        } else if (cli.hasOption(ITERATIONS)) {
            iterations = cli.getParsedOptionValue(ITERATIONS, iterations);
        } else { // if (cli.hasOption(TIME))
            time = cli.getParsedOptionValue(TIME, time);
        }

        if (!cli.hasOption(PASSES)) throw new UsageException("'-p <passes>' must be specified");
        else passes = cli.getParsedOptionValue(PASSES, passes);

        if (cli.hasOption(LINE_MODE) && cli.hasOption(BULK_MODE))
            throw new UsageException("Cannot specify both '-l' (line mode) and '-b' (bulk mode)");

        threads = cli.getParsedOptionValue(THREADS, threads);
        if (threads <= 0)
            throw new UsageException("'-r <threads>' requires a number of threads greater than 0");
        duration = cli.getParsedOptionValue(DURATION, duration);

        line_mode = cli.hasOption(LINE_MODE);
        bulk_mode = cli.hasOption(BULK_MODE);
        verbose = cli.hasOption(VERBOSE);
        uselen = cli.hasOption(USELEN);
        doPriorGC = cli.hasOption(GARBAGE_COLLECT);
        action = cli.hasOption(ACTION);

        sourceDir = cli.getOptionValue(SOURCEDIR, sourceDir);
        encoding = cli.getOptionValue(ENCODING, encoding);
        fileName = cli.getOptionValue(FILE_NAME, fileName);
        testName = cli.getOptionValue(TEST_NAME, testName);
        if (cli.hasOption(LOCALE)) {
            locale = LocaleUtility.getLocaleFromName(cli.getOptionValue(LOCALE));
        }

        // Left-over arguments
        String[] remainingArgv = cli.getArgs();
        int remainingArgc = remainingArgv.length;

        // build the test list, preserving order
        Set<String> testList = new LinkedHashSet<>();
        int i, j;
        for (i = 0; i < remainingArgc; ++i) {
            // is remainingArgv[i] a method name?
            if (testProvider.isTestCmd(remainingArgv[i])) {
                testList.add(remainingArgv[i]);
            } else {
                // remainingArgv[i] is neither a method name nor a number. Pass
                // everything from here on through to the subclass via
                // setup().
                break;
            }
        }

        // if no tests were specified, put all the tests in the test list
        if (testList.isEmpty()) {
            testList.addAll(testProvider.getAllTestCmdNames());
        }

        // pass remaining arguments, if any, through to the subclass via setup() method.
        String[] subclassArgs = new String[remainingArgc - i];
        for (j = 0; i < remainingArgc; j++) subclassArgs[j] = remainingArgv[i++];
        setup(subclassArgs);

        // Put the heap in a consistent state
        if (doPriorGC) gc();

        return testList;
    }

    /** Translate '-t time' to iterations (or just return '-i iteration') */
    private long getIteration(String methName, Function fn) throws InterruptedException {
        long iter = 0;
        if (iterations > 0) {
            iter = iterations;
        } else { // iterations not in input, calibrate iterations for given time.
            // Translate time to iteration
            // Assuming there is a linear relation between time and iterations

            if (verbose) {
                System.out.println("= " + methName + " calibrating " + time + " seconds");
            }

            long base = time * 1000L;
            // System.out.println("base :" + base);
            long seed = 1;
            long t = 0;
            while (t < base * 0.9 || base * 1.1 < t) { // + - 10%
                if (iter == 0 || t == 0) {
                    iter = seed; // start up from 1
                    seed *= 100; // if the method is too fast (t == 0),
                    // multiply 100 times
                    // 100 is rational because 'base' is always larger than 1000
                } else {
                    // If 't' is large enough, use linear function to calculate
                    // new iteration
                    //
                    // new iter(base) old iter
                    // -------------- = -------- = k
                    // new time old time
                    //
                    // System.out.println("before guess t: " + t);
                    // System.out.println("before guess iter: " + iter);
                    iter = (long) ((double) iter / t * base); // avoid long
                    // cut, eg. 1/10
                    // == 0
                    if (iter == 0) {
                        throw new RuntimeException("Unable to converge on desired duration");
                    }
                }
                t = performLoops(fn, iter);
            }
            // System.out.println("final t : " + t);
            // System.out.println("final i : " + iter);
        }
        return iter;
    }

    private long performLoops(Function function, long loops) throws InterruptedException {
        function.init();
        if (threads > 1) {
            Thread[] threadList = new Thread[threads];
            for (int i = 0; i < threads; i++)
                threadList[i] = new Thread(new FunctionRunner(function, loops, i));

            long start = System.currentTimeMillis();
            for (int i = 0; i < threads; i++) threadList[i].start();
            for (int i = 0; i < threads; i++) threadList[i].join();
            return System.currentTimeMillis() - start;

        } else {
            return function.time(loops); // ms
        }
    }

    /**
     * Invoke the runtime's garbage collection procedure repeatedly until the amount of free memory
     * stabilizes to within 10%.
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
            } while (((double) Math.abs(free - last)) / free > 0.1);
            // Wait for the change in free memory to drop under 10%
            // between successive calls.
        }

        // From "Java Platform Performance". This is the procedure
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
        } catch (InterruptedException e) {
        }
    }

    public static char[] readToEOS(Reader reader) {
        ArrayList<char[]> vec = new ArrayList<>();
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
                    int n = reader.read(buffer, pos, length - pos);
                    if (n == -1) {
                        break;
                    }
                    pos += n;
                } while (pos < length);
            } catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);

        char[] data = new char[count];
        pos = 0;
        for (char[] buf : vec) {
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
    }

    public static byte[] readToEOS(InputStream stream) {

        ArrayList<byte[]> vec = new ArrayList<>();
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
            } catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);

        byte[] data = new byte[count];
        pos = 0;
        for (byte[] buf : vec) {
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
    }

    protected String[] readLines(String filename, String srcEncoding, boolean bulkMode) {
        try (FileInputStream fis = new FileInputStream(filename);
                InputStreamReader isr = new InputStreamReader(fis, srcEncoding);
                BufferedReader br = new BufferedReader(isr)) {
            ArrayList<String> list = new ArrayList<>();
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

            String[] lines = null;

            if (bulkMode) {
                lines = new String[1];
                StringBuilder buffer = new StringBuilder();
                for (String s : list) {
                    buffer.append(s);
                    /*if (i < (size - 1)) {
                        buffer.append("\r\n");
                    }*/
                }
                lines[0] = buffer.toString();
            } else {
                int size = list.size();
                lines = new String[size];
                for (int i = 0; i < size; ++i) {
                    lines[i] = list.get(i);
                }
            }

            return lines;
        } catch (Exception e) {
            System.err.println("Error: File access exception: " + e.getMessage() + "!");
            System.exit(1);
        }

        return null;
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
            throw new Exception("Line \"{0}\",  \"{1}\"" + originalLine + " " + line + " " + e);
        }
        return line;
    }

    public static class BOMFreeReader extends Reader {
        InputStreamReader reader;
        String encoding;
        int MAX_BOM_LENGTH = 5;

        /**
         * Creates a new reader, skipping a BOM associated with the given encoding. If encoding is
         * null, attempts to detect the encoding by the BOM.
         *
         * @param in The input stream.
         * @param encoding The encoding to use. Can be null.
         * @throws IOException Thrown if reading for a BOM causes an IOException.
         */
        public BOMFreeReader(InputStream in, String encoding) throws IOException {
            if (encoding == null) {
                throw new IllegalArgumentException("The encoding cannot be null");
            }
            if (!Charset.isSupported(encoding)) {
                throw new IllegalArgumentException("Unsupported encoding '" + encoding + "'");
            }
            PushbackInputStream pushback = new PushbackInputStream(in, MAX_BOM_LENGTH);
            this.encoding = encoding;

            byte[] start = new byte[MAX_BOM_LENGTH];
            Arrays.fill(start, (byte) 0xa5);

            int amountRead = pushback.read(start, 0, MAX_BOM_LENGTH);
            int bomLength = detectBOMLength(start);
            if (amountRead > bomLength) pushback.unread(start, bomLength, amountRead - bomLength);

            reader = new InputStreamReader(pushback, encoding);
        }

        /**
         * Determines the length of a BOM in the beginning of start. Assumes start is at least a
         * length 5 array. If encoding is null, the check will not be encoding specific and it will
         * set the encoding of this BOMFreeReader.
         *
         * @param start The starting bytes.
         * @return The length of a detected BOM.
         */
        private int detectBOMLength(byte[] start) {
            if ((encoding == null || "UTF-16BE".equals(encoding))
                    && start[0] == (byte) 0xFE
                    && start[1] == (byte) 0xFF) {
                if (encoding == null) this.encoding = "UTF-16BE";
                return 2; // "UTF-16BE";
            } else if (start[0] == (byte) 0xFF && start[1] == (byte) 0xFE) {
                if ((encoding == null || "UTF-32LE".equals(encoding))
                        && start[2] == (byte) 0x00
                        && start[3] == (byte) 0x00) {
                    if (encoding == null) this.encoding = "UTF-32LE";
                    return 4; // "UTF-32LE";
                } else if ((encoding == null || "UTF-16LE".equals(encoding))) {
                    if (encoding == null) this.encoding = "UTF-16LE";
                    return 2; // "UTF-16LE";
                }
            } else if ((encoding == null || "UTF-8".equals(encoding))
                    && start[0] == (byte) 0xEF
                    && start[1] == (byte) 0xBB
                    && start[2] == (byte) 0xBF) {
                if (encoding == null) this.encoding = "UTF-8";
                return 3; // "UTF-8";
            } else if ((encoding == null || "UTF-32BE".equals(encoding))
                    && start[0] == (byte) 0x00
                    && start[1] == (byte) 0x00
                    && start[2] == (byte) 0xFE
                    && start[3] == (byte) 0xFF) {
                if (encoding == null) this.encoding = "UTF-32BE";
                return 4; // "UTF-32BE";
            } else if ((encoding == null || "SCSU".equals(encoding))
                    && start[0] == (byte) 0x0E
                    && start[1] == (byte) 0xFE
                    && start[2] == (byte) 0xFF) {
                if (encoding == null) this.encoding = "SCSU";
                return 3; // "SCSU";
            } else if ((encoding == null || "BOCU-1".equals(encoding))
                    && start[0] == (byte) 0xFB
                    && start[1] == (byte) 0xEE
                    && start[2] == (byte) 0x28) {
                if (encoding == null) this.encoding = "BOCU-1";
                return 3; // "BOCU-1";
            } else if ((encoding == null || "UTF-7".equals(encoding))
                    && start[0] == (byte) 0x2B
                    && start[1] == (byte) 0x2F
                    && start[2] == (byte) 0x76) {
                if (start[3] == (byte) 0x38 && start[4] == (byte) 0x2D) {
                    if (encoding == null) this.encoding = "UTF-7";
                    return 5; // "UTF-7";
                } else if (start[3] == (byte) 0x38
                        || start[3] == (byte) 0x39
                        || start[3] == (byte) 0x2B
                        || start[3] == (byte) 0x2F) {
                    if (encoding == null) this.encoding = "UTF-7";
                    return 4; // "UTF-7";
                }
            } else if ((encoding == null || "UTF-EBCDIC".equals(encoding))
                    && start[0] == (byte) 0xDD
                    && start[2] == (byte) 0x73
                    && start[2] == (byte) 0x66
                    && start[3] == (byte) 0x73) {
                if (encoding == null) this.encoding = "UTF-EBCDIC";
                return 4; // "UTF-EBCDIC";
            }

            /* no known Unicode signature byte sequence recognized */
            return 0;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return reader.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}

// eof
