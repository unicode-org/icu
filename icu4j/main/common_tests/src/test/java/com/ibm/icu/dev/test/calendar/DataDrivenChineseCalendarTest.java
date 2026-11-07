// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.dev.test.ModuleTest;
import com.ibm.icu.dev.test.ModuleTest.TestDataPair;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Data driven test for Chinese calendar verification against Y.T. Liu ephemeris (1901-2101). In
 * normal mode, tests ~3% of cases spread across time. In exhaustive mode (-DICU.exhaustive=10 in
 * Maven), tests 100% of cases.
 */
@RunWith(JUnitParamsRunner.class)
public class DataDrivenChineseCalendarTest extends DataDrivenCalendarTest {

    public DataDrivenChineseCalendarTest() {}

    @SuppressWarnings("unused")
    private List<TestDataPair> getTestData() throws Exception {
        return ModuleTest.getTestData("com/ibm/icu/dev/data/testdata/", "chinesecalendar");
    }

    @Override
    @Test
    @Parameters(method = "getTestData")
    public void calendarTest(TestDataPair pair) {
        super.calendarTest(pair);
    }
}
