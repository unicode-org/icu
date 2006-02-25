/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Represents a collection of test data described in a file.
 */
public abstract class TestDataModule {
    protected final String name;
    protected final TestLog log;

    /**
     * Looks for file with corresponding name, either xml or resource
     * bundle.  Returns module or null if not present.  
     */
    static final TestDataModule open(String name, TestLog log) {
    ResourceBundle bundle = ResourceBundle.getBundle(name);
    if (bundle != null) {
        return new ResourceModule(name, log, bundle);
    }
    return null;
    }

    /**
     * Subclass constructors call this.
     */
    protected TestDataModule(String name, TestLog log) {
    this.name = name;
    this.log = log;
    }
    
    /**
     * Return the name of this test module.
     */
    public final String getName() {
    return name;
    }

    /**
     * Return the log used by this module.  Errors logged to this
     * log will terminate settings and cases iteration for the
     * current test.
     */
    public final TestLog getLog() {
    return log;
    }

    /**
     * Get additional data related to the module, e.g. DESCRIPTION,
     * global settings.  Might be null.
     */
    public abstract DataMap getInfo();

    /**
     * A standard datum that can be found in the DataMap returned by getInfo.
     * DESCRIPTION provides a one-line description.  The corresponding type
     * is a String.
     */
    public static final String DESCRIPTION = "Description";

    /**
     * A standard datum that can be found in the DataMap returned by
     * getInfo.  LONG_DESCRIPTION provides an extended description.
     * The corresponding type is a String.  
     */
    public static final String LONG_DESCRIPTION = "Long_Description";

    /**
     * The names of the columns of test data.  This can be found in
     * either the module INFO or the test INFO.  If the test INFO
     * does not define the headers then they are taken from the
     * module.  If test cases are used by the test, then the headers
     * must be defined.  Most clients will not need this information,
     * since it is handled by the framework.
     */
    public static final String HEADERS = "Headers";

    /**
     * Returns the Test object corresponding to index, or null if
     * index is out of range for this module.  No error logged if
     * index is out of bounds, the assumption is that iteration is
     * being used.  
     */
    public abstract TestData createTestData(int index);

    /**
     * Returns the TestData corresponding to name, or null if name not
     * found in this module.  Logs error if name is not found.  
     */
    public abstract TestData createTestData(String name);

    /**
     * Return an unmodifiable list of the test data names, in index order.
     */
    public abstract List getTestDataNames();

    /**
     * Represents a single test in the module.
     */
    public abstract static class TestData {
    String name;

    protected TestData(String name) {
        this.name = name;
    }

    public final String name() {
        return name;
    }

    /**
     * Get additional data related to the test, e.g. DESCRIPTION,
     * LONG_DESCRIPTION, HEADERS, or other test-specific
     * information.  
     */
    public abstract DataMap getInfo();

    /**
     * Returns new DataMap for next settings, and resets test case
     * iteration.  Returns null if no more settings.  
     */
    public abstract DataMap nextSettings();

    /**
     * Returns new DataMap for next cases.  
     * Returns null if no more cases.
     */
    public abstract DataMap nextCase();

    /**
     * Stops iteration of the test.  Usually called if some
     * condition detects an error.
     */
    public abstract void stopIteration();
    }

    /**
     * DataMap provides named access to typed data.  Lookup data by
     * key and attempt to cast to indicated type.  If data not found
     * or cast fails, log error and return null.  */
    public abstract static class DataMap {
    public abstract boolean    isDefined(String key);

    public abstract Object     getObject(String key);
    public abstract String     getString(String key);
    public abstract char       getChar(String key);
    public abstract int        getInt(String key);
    public abstract byte       getByte(String key);
    public abstract boolean    getBoolean(String key);

    public abstract Object[]   getObjectArray(String key);
    public abstract String[]   getStringArray(String key);
    public abstract char[]     getCharArray(String key);
    public abstract int[]      getIntArray(String key);
    public abstract byte[]     getByteArray(String key);
    public abstract boolean[]  getBooleanArray(String key);
    }
}
    
