/**
 *******************************************************************************
 * Copyright (C) 2001-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.lang.reflect.Method;
import java.util.Iterator;

import com.ibm.icu.dev.test.TestDataModule.DataMap;
import com.ibm.icu.dev.test.TestDataModule.DataModuleFormatError;
import com.ibm.icu.dev.test.TestDataModule.Factory;
import com.ibm.icu.dev.test.TestDataModule.TestData;

/**
 * Ray: An adapter class for TestDataMoule to make it like TestFmwk
 * 
 * A convenience extension of TestFmwk for use by data module-driven tests.
 * 
 * Tests can implement this if they make extensive use of information in a
 * TestDataModule.
 * 
 * The module should be openable using TestDataModule.open() passing in the
 * class name of this test + "Data". For example, a test named MyTest would have
 * a module named MyTestData. Each test method should also have a corresponding
 * test data in the module whose name matches the test method name.
 * 
 * Subclasses can allow for test methods that don't use data from the modeul by
 * overriding validateMethod to return true for these methods. Tests are also
 * free to instantiate their own modules and run from them, though care should
 * be taken not to interfere with the methods in this class.
 * 
 * See ModuleTestSample for an example.
 */
public abstract class ModuleTest extends TestFmwk {
    private TestDataModule m;
    protected TestData t;
    
    abstract protected void processModules();
    
    protected Target getTargets(String targetName) {
      if (params.doMethods()) {
            Target target = null;
            if (!validate()) {
                return null;
            }
            Iterator testData = m.getTestDataIterator();
            if (testData != null) {
                try {
                    Method method = getClass().getMethod("processModules", null);
                    while (testData.hasNext()) {
                        target = new MethodTarget(((TestData)testData.next()).getName(), method).setNext(target);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException(e.getMessage());
                }
            }
            return target;
        } else {
            return null;
        }
    }

    /**
     * 
     * TestFmwk calls this before trying to run a suite of tests.
     * The test suite if valid if a module whose name is the name of
     * this class + "Data" can be opened.  Subclasses can override
     * this if there are different or additional data required.  
     */
    protected boolean validate() {
        t = null;
        String n = getClass().getName();
        String localename = "DataDriven" + n.substring(n.lastIndexOf('.') + 1);
        String baseName = "com/ibm/icu/dev/data/testdata/"; 
        
        try {
            //m = Factory.get(getClass().getName()+"Data", this);
            m = Factory.get(baseName, localename);
        } catch (DataModuleFormatError e) {
            e.printStackTrace();
            m = null;
        }
        return m != null;
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
//        return info.getString(TestDataModule.DESCRIPTION);
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
//        return info.getString(TestDataModule.DESCRIPTION);
        }
    }
    return null;
    }

    /**
     * Open the test data in the module with the given name, and return
     * true if success.  The current test is reset.
     * @throws DataModuleFormatError
     */
    protected boolean openTestData(String name){
    try {
        t = m == null ? null : m.getTestData(name);
    } catch (DataModuleFormatError e) {
        return false;
    }
    return t != null;
    }
    
    /**
     * Get information on this module.  Returns null if no module
     * open or no info for the module.
     */
    private DataMap moduleInfo() {
    return m == null ? null : m.getInfo();
    }

    /**
     * Get information on this test.  Returns null if no module
     * open or no test open or not info for this test.
     */
    private DataMap testInfo() {
    return t == null ? null : t.getInfo();
    }

    public void msg(String message, int level, boolean incCount, boolean newln) {
        if (level == ERR && t != null) {
//            t.stopIteration();
        }
        super.msg(message, level, incCount, newln);
    }
    
}
