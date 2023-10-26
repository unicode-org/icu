// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.format;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.DontCareFieldPosition;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class MeasureUnitThreadTest extends CoreTestFmwk {

    @Test
    public void MUThreadTest() {
        // Ticket #12034 deadlock on multi-threaded static init of MeasureUnit.
        // The code below reliably deadlocks with ICU 56.
        // The test is here in its own file so it can be made to run independent of anything else.
        Thread thread = new Thread()  {
            @Override
            public void run() {
                MeasureUnit.getAvailableTypes();
            }
        };
        thread.start();
        Currency.getInstance(ULocale.ENGLISH);
        try {thread.join();} catch(InterruptedException e) {};
    }

    static class NumericMeasureThread extends Thread {
        final MeasureFormat mf;
        final Measure[] arg;
        final String expected;
        volatile boolean running = true;
        AssertionError error;

        NumericMeasureThread(Measure[] arg, String expected) {
            this.mf = MeasureFormat.getInstance(ULocale.ENGLISH, MeasureFormat.FormatWidth.NUMERIC);
            this.arg = arg;
            this.expected = expected;
            this.error = null;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    StringBuilder sb = new StringBuilder();
                    mf.formatMeasures(sb, DontCareFieldPosition.INSTANCE, arg);
                    org.junit.Assert.assertEquals(expected, sb.toString());
                } catch (AssertionError e) {
                    error = e;
                    break;
                }
            }
        }
    }

    // Race in formatMeasures with width NUMERIC:
    // https://unicode-org.atlassian.net/browse/ICU-13606
    @Test
    public void NumericRaceTest() throws InterruptedException {
        NumericMeasureThread t1 = new NumericMeasureThread(new Measure[] {
          new Measure(3, MeasureUnit.MINUTE),
          new Measure(4, MeasureUnit.SECOND)
        }, "3:04");
        NumericMeasureThread t2 = new NumericMeasureThread(new Measure[] {
          new Measure(5, MeasureUnit.MINUTE),
          new Measure(6, MeasureUnit.SECOND)
        }, "5:06");
        t1.start();
        t2.start();
        Thread.sleep(5);
        t1.running = false;
        t2.running = false;
        t1.join();
        t2.join();
        if (t1.error != null) {
            AssertionError error = new AssertionError("Failure in thread 1");
            error.initCause(t1.error);
            throw error;
        }
        if (t2.error != null) {
            AssertionError error = new AssertionError("Failure in thread 2");
            error.initCause(t2.error);
            throw error;
        }
    }
}
