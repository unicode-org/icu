/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/perf/PerfTest.java,v $ 
**********************************************************************
*/
package com.ibm.icu.dev.test.perf;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.*;

/**
 * Base class for performance testing framework.  To use, subclass and
 * define one or more instance methods with names beginning with
 * "test" (case ignored).  Each such method must then call two
 * functions, setTestFunction(), and setEventsPerCall().  In addition,
 * the subclass should define a main() method that calls run() as
 * defined here.  If the subclasses uses any command line arguments
 * (beyond those handled automatically by this calss) then it should
 * override setup() to handle its arguments.
 * 
 * The command-line argument list should have the form:
 *   arg-list := options* meth-spec+ '-'? ...
 *   meth-spec := meth-name count+
 *   options := -gc | -nogc
 * where:
 *   meth-name a method name, case insensitive, optionally
 *             with the initial "test" omitted
 *   count     an integer, > 0, giving the number of time
 *             to call the method setup by meth-name.  If 'count' is
 *             an integer < 0, then iterations are varied to achieve a
 *             total duration of at least -count seconds.
 *   '-'       an optional separator; if seen, all subsequent
 *             arguments are passed to the subclass
 *   ...       extra arguments, not recognized as either a
 *             meth-name or a count, or following a '-',
 *             passed to the setup() method, which the
 *             subclass may override
 * options:
 *   -gc       calls Runtime.gc() repeatedly until heap
 *             stabilizes before doing any timing
 *   -nogc     disable -gc (default)
 *
 * Example invocation:
 * java -cp classes -verbose:gc com.ibm.icu.dev.test.perf.UnicodeSetPerf -gc UnicodeSetAdd 100 100 100 100 [[:l:][:c:]]
 * 
 * Example output:
 * [GC 511K->192K(1984K), 0.0086170 secs]
 * [GC 704K->353K(1984K), 0.0059619 secs]
 * [Full GC 618K->371K(1984K), 0.0242779 secs]
 * [Full GC 371K->371K(1984K), 0.0228649 secs]
 * = testUnicodeSetAdd begin 100
 * = testUnicodeSetAdd end 11977 1109044
 * = testUnicodeSetAdd begin 100
 * = testUnicodeSetAdd end 12047 1109044
 * = testUnicodeSetAdd begin 100
 * = testUnicodeSetAdd end 11987 1109044
 * = testUnicodeSetAdd begin 100
 * = testUnicodeSetAdd end 11978 1109044
 *
 * The [] lines are emitted by the JVM as a result of the -verbose:gc
 * switch.
 *
 * Lines beginning with '=' are emitted by PerfTest:
 *
 * = testUnicodeSetAdd begin 100
 * A 'begin' statement contains the name of the setup method, which
 * determines what test function is measures, and the number of
 * iterations that will be times.
 *
 * = testUnicodeSetAdd end 12047 1109044
 * An 'end' statement gives the name of the setup method again, and
 * then two integers.  The first is the total elapsed time in
 * milliseconds, and the second is the number of events per iteration.
 * In this example, the time per event is 12047 / (100 * 1109044) or
 * 108.6 ns/event.
 *
 * Raw times are given as integer ms, because this is what the system
 * measures.
 *
 * @author Alan Liu
 * @since ICU 2.4
 */
public abstract class PerfTest {

    /**
     * The number of subclass-defined "events" within on call to
     * Function.call().  Set by subclass using setEventsPerCall().
     */
    private int eventsPerCall;

    /**
     * The subclass-defined test function.  Set by subclass using
     * setTestFunction().
     */
    private Function testFunction;

    /**
     * A map of munged names to Method objects.  All available methods
     * in the current object beginning with "test" (case ignored).
     */
    private Map availableTests; // NOT static

    /**
     * A flag determining whether to call gc() once before the test
     * passes.
     */
    private boolean doPriorGC;

    /**
     * Subclasses of PerfTest will need to create subclasses of
     * Function that define a call() method which contains the code to
     * be timed.  They then call setTestFunction() in their "Test..."
     * method to establish this as the current test functor.
     */
    public abstract class Function {

        /**
         * Subclasses must implement this method to do the action to be
         * measured.
         */
        public abstract void call();

        /**
         * Call call() n times in a tight loop and return the elapsed
         * milliseconds.  If n is small and call() is fast the return
         * result may be zero.  Small return values have limited
         * meaningfulness, depending on the underlying VM and OS.
         */
        public final long time(int n) {
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
     * Constructor.
     */
    protected PerfTest() {
        availableTests = null;
        doPriorGC = false;
    }
    
    /**
     * Subclasses must call this method with a positive integer to
     * indicate the number of "events" in a sincle call to the current
     * test functor's call() method.
     */
    protected void setEventsPerCall(int n) {
        eventsPerCall = n;
    }

    /**
     * Subclasses must call this method with a test functor, whose
     * call() function will be timed.
     */
    protected void setTestFunction(PerfTest.Function f) {
        testFunction = f;
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
     * Subclasses should call this method in their main().  run() will
     * in turn call setup() with any arguments it does not parse.
     * This method parses the command line and runs the tests given on
     * the command line, with the given parameters.  See the class
     * description for details.
     */
    protected final void run(String[] args) throws Exception {
        ArrayList methodList = new ArrayList();
        ArrayList countList = new ArrayList();
        Method meth = null;
        ArrayList iterationCounts = new ArrayList();
        int i, j;
        for (i=0; i<args.length; ++i) {
            if (args[i].equals("-")) {
                // end of our options; the rest go to setup()
                ++i;
                break;
            }

            if (args[i].equalsIgnoreCase("-gc")) {
                doPriorGC = true;
                continue;
            }

            if (args[i].equalsIgnoreCase("-nogc")) {
                doPriorGC = false;
                continue;
            }

            // is args[i] a number?
            try {
                int n = Integer.parseInt(args[i]);
                // >0 and <0 are okay, but 0 is illegal
                if (n == 0) {
                    throw new RuntimeException("Iteration count(s) may not be zero");
                }
                if (meth == null) {
                    throw new RuntimeException("Iteration count(s) must follow method name");
                }
                iterationCounts.add(new Integer(n));
                continue;
            } catch (NumberFormatException e) {}

            // is args[i] a method name?
            Method m = getTestMethod(args[i]);
            if (m != null) {
                if (meth != null) {
                    // COPIED below
                    if (iterationCounts.size() < 1) {
                        throw new RuntimeException("Method name must be followed by iteration count(s)");
                    }
                    methodList.add(meth);
                    countList.add(toIntArray(iterationCounts));
                    iterationCounts.clear();
                }
                meth = m;
                continue;
            }

            // args[i] is neither a method name nor a number.  Pass
            // everything from here on through to the subclass via
            // setup().
            break;
        }

        if (meth != null) {
            // COPIED from above
            if (iterationCounts.size() < 1) {
                throw new RuntimeException("Method name must be followed by iteration count(s)");
            }
            methodList.add(meth);
            countList.add(toIntArray(iterationCounts));
        }

        if (methodList.size() < 1) {
            throw new RuntimeException("Must specify at least one method name and count(s)");
        }

        // Pass remaining arguments, if any, through to the subclass
        // via setup() method.
        String[] remainingArgs = new String[args.length - i];
        for (j=0; i<args.length; ++j) {
            remainingArgs[j] = args[i++];
        }
        setup(remainingArgs);

        if (doPriorGC) {
            // Put the heap in a consistent state
            gc();
        }

	    final Object[] NO_ARGS = new Object[0];

        // Run the tests
        for (i=0; i<methodList.size(); ++i) {
            meth = (Method) methodList.get(i);
            int[] counts = (int[]) countList.get(i);

            // Call meth to set up the test
            eventsPerCall = -1;
            testFunction = null;
            meth.invoke(this, NO_ARGS); // set up the test
            if (eventsPerCall < 1) {
                throw new RuntimeException(meth.getName() + " failed to call setEventsPerCall()");
            }
            if (testFunction == null) {
                throw new RuntimeException(meth.getName() + " failed to call setTestFunction()");
            }

            int n;
            long t;
            for (j=0; j<counts.length; ++j) {
                n = counts[j];
                if (n > 0) {
                    // Run specified number of iterations
                    System.out.println("= " + meth.getName() + " begin " + n);
                    t = testFunction.time(n);
                    System.out.println("= " + meth.getName() + " end " + t + " " + eventsPerCall);
                } else {
                    // Run for specified duration in seconds
                    System.out.println("= " + meth.getName() + " begin " + n);
                    n = -n * 1000; // s => ms
                    //System.out.println("# " + meth.getName() + " " + n + " sec");                            
                    int loops = 0;
                    int failsafe = 1; // last resort for very fast methods
                    t = 0;
                    while (t < (int)(n * 0.9)) { // 90% is close enough
                        if (loops == 0 || t == 0) {
                            loops = failsafe;
                            failsafe *= 10;
                        } else {
                            //System.out.println("# " + meth.getName() + " x " + loops + " = " + t);                            
                            loops = (int)((double)n / t * loops + 0.5);
                            if (loops == 0) {
                                throw new RuntimeException("Unable to converge on desired duration");
                            }
                        }
                        //System.out.println("# " + meth.getName() + " x " + loops);
                        t = testFunction.time(loops);
                    }
                    System.out.println("= " + meth.getName() + " end " + t + " " + eventsPerCall +
                                       " " + loops);
                }
            }
        }
    }

    /**
     * Invoke the runtime's garbage collection procedure repeatedly
     * until the amount of free memory stabilizes to within 10%.
     */
    protected void gc() {
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

    /**
     * Return a map of String to Method objects.  Each string is a
     * munged all-lowercase name.  For this reason, test methods
     * (beginning with "test", ignoring case) must not collide when
     * case folded.
     */
    private Map getAvailableTests() {
        if (availableTests == null) {
            availableTests = new HashMap();
            Method[] methods = getClass().getDeclaredMethods();
            for (int i=0; i<methods.length; i++) {
                String name = methods[i].getName().toLowerCase();
                // Note: methods named "test()" are ignored
                if (name.length() > 4 && name.startsWith("test")) {
                    if (availableTests.get(name) != null) {
                        throw new RuntimeException("Duplicate method name ignoring case: " + name);
                    }
                    availableTests.put(name, methods[i]);
                }
            }
        }
        return availableTests;
    }

    /**
     * Given a name, return the associated Method object, or null if
     * not found.  The given name will map to a method of the same
     * name, or a method named "test" + name.  Case is ignored.
     */
    private Method getTestMethod(String name) {
        Map avail = getAvailableTests();
        String key = name.toLowerCase();
        Method m = (Method) avail.get(key);
        if (m != null) {
            return m;
        }
        return (Method) avail.get("test" + key);
    }

    /**
     * Private utility to convert a List of Integer objects to int[].
     */
    private static int[] toIntArray(List list) {
        int[] result = new int[list.size()];
        for (int i=0; i<list.size(); ++i) {
            result[i] = ((Integer) list.get(i)).intValue();
        }
        return result;
    }
}

//eof
