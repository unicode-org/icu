package com.ibm.icu.dev.test;

import com.ibm.icu.dev.test.TestDataModule.TestData;
import com.ibm.icu.dev.test.TestDataModule.DataMap;

/**
 * A convenience extension of TestFmwk for use by data module-driven
 * tests.  Tests can implement this if they make extensive use of
 * information in a TestDataModule.  The module should be openable
 * using TestDataModule.open() passing in the class name of this test
 * + "Data".  For example, a test named MyTest would have a module
 * named MyTestData.  Each test method should also have a
 * corresponding test data in the module whose name matches the test
 * method name.
 *
 * Subclasses can allow for test methods that don't use data from the
 * modeul by overriding validateMethod to return true for these
 * methods.  Tests are also free to instantiate their own modules and
 * run from them, though care should be taken not to interfere with
 * the methods in this class.
 *
 * See ModuleTestSample for an example.  */
public class ModuleTest extends TestFmwk {
    private TestDataModule m;
    private TestData t;

    /**
     * Subclasses access this after calling nextSettings and getting
     * a true result.
     */
    protected DataMap settings;

    /**
     * Subclasses access this after calling nextCase and getting a
     * true result.
     */
    protected DataMap testcase;

    /**
     * TestFmwk calls this before trying to run a suite of tests.
     * The test suite if valid if a module whose name is the name of
     * this class + "Data" can be opened.  Subclasses can override
     * this if there are different or additional data required.  
     */
    protected boolean validate() {
	return openModule(getClass().getName()+"Data");
    }

    /**
     * TestFmwk calls this before trying to invoke a test method.
     * The method is valid if there is test data with the name of this
     * method in the module.  Subclasses can override this to allow
     * for tests that do not require test data from the module, or
     * if there are different or additional data required.
     */
    protected boolean validateMethod(String methodName) {
	return openTestData(methodName);
    }

    /**
     * Override of TestFmwk method to get the test suite description
     * from the DESCRIPTION field of the module info.  
     */
    protected String getDescription() {
	DataMap info = moduleInfo();
	if (info != null) {
	    return info.getString(TestDataModule.DESCRIPTION);
	}
	return null;
    }

    /**
     * Override of TestFmwk method to get the test method description
     * from the DESCRIPTION field of the test info.  
     */
    protected String getMethodDescription(String methodName) {
	if (openTestData(methodName)) {
	    DataMap info = testInfo();
	    if (info != null) {
		return info.getString(TestDataModule.DESCRIPTION);
	    }
	}
	return null;
    }

    /**
     * Opens the module with the given name, and return true if success.
     * All contents are reset.
     */
    protected boolean openModule(String name) {
	t = null;
	m = TestDataModule.open(name, this);
	return m != null;
    }

    /**
     * Open the test data in the module with the given name, and return
     * true if success.  The current test is reset.
     */
    protected boolean openTestData(String name) {
	t = m == null ? null : m.createTestData(name);
	return t != null;
    }

    /**
     * Get information on this module.  Returns null if no module
     * open or no info for the module.
     */
    protected DataMap moduleInfo() {
	return m == null ? null : m.getInfo();
    }

    /**
     * Get information on this test.  Returns null if no module
     * open or no test open or not info for this test.
     */
    protected DataMap testInfo() {
	return t == null ? null : t.getInfo();
    }

    /**
     * Advance test to the next settings, and return true if 
     * there are more settings.  The protected member variable
     * 'settings' holds the new settings data.
     */
    protected boolean nextSettings() {
	settings = t == null ? null : t.nextSettings();
	return settings != null;
    }

    /**
     * Advance test to the next case, and return true if there
     * is another case.  The protected member variable
     * 'testcase' holds the new case data.
     */
    protected boolean nextCase() {
	testcase = t == null ? null : t.nextCase();
	return testcase != null;
    }

    /**
     * Report an error, and stop iteration of the current test.
     */
    public void err(String message) {
	if (t != null) {
	    t.stopIteration();
	}
	super.err(message);
    }

    /**
     * Report an error, and stop iteration of the current test.
     */
    public void errln(String message) {
	if (t != null) {
	    t.stopIteration();
	}
	super.errln(message);
    }
}
