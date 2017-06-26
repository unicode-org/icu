// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

package com.ibm.icu.dev.test.format;

import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public class MeasureUnitThreadTest extends TestFmwk {

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
}

