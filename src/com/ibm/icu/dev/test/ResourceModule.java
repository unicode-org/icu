/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Resource-based module.
 */
public class ResourceModule extends TestDataModule {
    private NamedArray tests;
    private RBDataMap info;
    private String[] defaultHeaders;

    /**
     * For internal use.
     */
    static final String TESTS = "TestData";

    /**
     * For internal use.
     */
    static final String INFO = "Info";

    /**
     * For internal use.
     */
    static final String SETTINGS = "Settings";

    /**
     * For internal use.
     */
    static final String CASES = "Cases";

    public ResourceModule(String name, TestLog log, ResourceBundle b) {
    super(name, log);

    if (b == null) {
        log.errln("ResourceModule must have non-null bundle");
        return;
    }

    Object[][] data = (Object[][])b.getObject(TESTS);
    if (data == null) {
        log.errln("ResourceModule does not contain tests!");
        return;
    }
    this.tests = NamedArray.create(data);

    Object[][] temp = (Object[][])b.getObject(INFO);
    if (temp != null) {
        NamedArray na = NamedArray.create(temp);
        if (na.count() < 0) {
        log.errln("Bad data for " + INFO);
        } else {
        this.info = new RBDataMap(log, na, false);    
        this.defaultHeaders = info.getStringArray(HEADERS);    
        }
    }
    }

    /**
     * Get additional data related to the module.
     * Returns true if successfully got info.
     */
    public DataMap getInfo() {
    return info;
    }

    /**
     * Returns the Test object corresponding to name, 
     * or null if name not found in this module.
     */
    public TestData createTestData(String name) {
    if (tests != null) {
        Object[] td = tests.getEntry(name);
        if (td != null) {
        return createTestData(td);
        }
    }

    return null;
    }

    /**
     * Returns the Test object corresponding to index,
     * or null if index is out of range for this module.
     * No error logged if index is out of bounds, the assumption is that
     * iteration is being used and proceeds until a null return.
     */
    public TestData createTestData(int index) {
    if (tests != null) {
        Object[] td = tests.getEntry(index);
        if (td != null) {
        return createTestData(td);
        }
    }
    return null;
    }

    /**
     * Return an unmodifiable list of the test data names, in index order.
     */
    public List getTestDataNames() {
        if (tests != null) {
            return Collections.unmodifiableList(Arrays.asList(tests.names()));
        }
        return null;
    }

    private TestData createTestData(Object[] d) {
    return new RBTestData(this, (String)d[0], (Object[][])d[1], defaultHeaders);
    }

    /**
     * Provides access to values either by index or by case-insensitive search on 
     * the name.  Poor man's Map with index.
     */
    static abstract class NamedArray {
    int txcache;
    int count;

    static NamedArray create(Object[][] pairs) {
        return new NamedArrayOfPairs(pairs);
    }

    static NamedArray create(String[] names, Object[] values) {
        return new NamedArrayTwoLists(names, values);
    }

    protected NamedArray(int count) {
        this.count = count;
    }

    public final int count() {
        return count;
    }

    public final boolean isDefined(String name) {
        int ix = nameToIndex(name);
        if (ix != -1) {
        txcache = (ix == 0 ? count : ix) - 1;
        return true;
        }
        return false;
    }

    public final Object[] getEntry(String name) {
        return getEntry(nameToIndex(name));
    }

    public final Object getValue(String name) {
        return getValue(nameToIndex(name));
    }
    
    public final Object[] getEntry(int index) {
        if (index >= 0 && index < count) {
        return entryAtIndex(index);
        }
        return null;
    }

    public final Object getValue(int index) {
        if (index >= 0 && index < count) {
        return valueAtIndex(index);
        }
        return null;
    }

    public String[] names() {
        if (count > 0) {
        String[] result = new String[count];
        for (int i = 0; i < count; ++i) {
            result[i] = nameAtIndex(i);
        }
        return result;
        }
        return null;
    }

    public Object[] values() {
        if (count > 0) {
        Object[] result = new Object[count];
        for (int i = 0; i < count; ++i) {
            result[i] = valueAtIndex(i);
        }
        return result;
        }
        return null;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("count: " + count + "{");
        for (int i = 0; i < count; ++i) {
        buf.append("\n  { " + nameAtIndex(i) + ", " + valueAtIndex(i) + " }");
        }
        buf.append("}");
        return buf.toString();
    }

    protected int nameToIndex(String name) {
        if (name != null && count > 0) {
        int i = txcache;
        do {
            if (++i == count) {
            i = 0;
            }
            if (name.equalsIgnoreCase(nameAtIndex(i))) {
            txcache = i;
            return i;
            }
        } while (i != txcache);
        }
        return -1;
    }

    protected Object[] entryAtIndex(int index) {
        return new Object[] { nameAtIndex(index), valueAtIndex(index) };
    }

    protected abstract String nameAtIndex(int index);
    protected abstract Object valueAtIndex(int index);
    }

    /**
     * NamedArray implemented using an array of name/value
     * pairs, represented by an Object[][].
     */
    static final class NamedArrayOfPairs extends NamedArray {
    private Object[][] data;

    public NamedArrayOfPairs(Object[][] data) {
        super(data == null ? -1 : data.length);
        this.data = data;
    }

    protected Object[] entryAtIndex(int index) {
        return data[index];
    }

    protected String nameAtIndex(int index) {
        return (String)data[index][0];
    }

    protected Object valueAtIndex(int index) {
        return data[index][1];
    }
    }

    /**
     * NamedArray implemented using two arrays, one of String and one of Object.
     */
    static final class NamedArrayTwoLists extends NamedArray {
    String[] names;
    Object[] values;

    public NamedArrayTwoLists(String[] names, Object[] values) {
        super (values != null && names != null && names.length == values.length ?
           names.length :
           -1);
        this.names = names;
        this.values = values;
    }
        
    public String[] names() {
        return names;
    }

    public Object[] values() {
        return values;
    }

    protected String nameAtIndex(int index) {
        return names[index];
    }

    protected Object valueAtIndex(int index) {
        return values[index];
    }
    }

    static class RBTestData extends TestData {
    ResourceModule m;
    RBDataMap info;
    Object[] settings;
    /** 
     * changed to fit the c genrb format. this is actually Object[] {Object[]}
     */
    Object[] cases; 
    String[] headers;
    int sx;
    int cx;

    RBTestData(ResourceModule m, String name, Object[][] data, String[] defaultHeaders) {
        super(name);
        this.m = m;

        NamedArray namedData = NamedArray.create(data);

        try {
        Object[][] temp = (Object[][])namedData.getValue(INFO);
        if (temp != null) {
            NamedArray na = NamedArray.create(temp);
            if (na.count() < 1) {
            m.log.errln("Bad data for " + INFO);
            } else {
            this.info = new RBDataMap(m.log, na, false);
            }
        }
        } 
        catch (ClassCastException e) {
        m.log.errln("Test " + name + " in module " + m.getName() + " has bad type for " + INFO);
        }

        try {
        this.settings = (Object[])namedData.getValue(SETTINGS);
        }
        catch (ClassCastException e) {
        m.log.errln("Test " + name + " in module " + m.getName() + " has bad type for " + SETTINGS);
        }

        try {
        this.cases = (Object[])namedData.getValue(CASES);
        }
        catch (ClassCastException e) {
        m.log.errln("Test " + name + " in module " + m.getName() + " has bad type for " + CASES);
        }

        if (info != null) {
        this.headers = info.getStringArray(HEADERS);
        }
        if (this.headers == null) {
        this.headers = defaultHeaders;
        }
    }

    /**
     * Get additional data related to the test.
     */
    public DataMap getInfo() {
        return info;
    }

    /**
     * Returns DataMap with next settings and resets case iteration.
     * If no more settings, terminates iteration and returns null.
     */
    public DataMap nextSettings() {
        if (settings != null && sx < settings.length) {
        cx = 0;
        NamedArray na = NamedArray.create((Object[][])settings[sx++]);
        if (na.count() < 0) {
            m.log.errln("Bad settings data for settings " + (sx-1));
            return null;
        } else {
            return new RBDataMap(m.log, na, false);
        }
        }
        stopIteration();

        return null;
    }

    /**
     * Returns DataMap with next case.  If no next case, returns null.
     */
    public DataMap nextCase() {
        if (cases != null && cx < cases.length) {
        NamedArray na = NamedArray.create(headers, (Object[])cases[cx++]);
        if (na.count() < 0) {
            m.log.errln("Bad cases data for case " + (cx-1));
        } else {
            return new RBDataMap(m.log, na, true);
        }
        }
        return null;
    }

    /**
     * Stops iteration.
     */
    public void stopIteration() {
        sx = cx = Integer.MAX_VALUE;
    }
    }
    
    static final class RBDataMap extends DataMap {
    TestLog log;
    NamedArray na;
    boolean required;

    RBDataMap(TestLog log, NamedArray na, boolean required) {
        this.log = log;
        this.na = na;
        this.required = required;
    }

    public boolean isDefined(String key) {
        return na.isDefined(key);
    }

    public Object getObject(String key) { 
        Object result = na.getValue(key);
        if (required && result == null) {
        reportNullError(key);
        }
        return result;
    }

    public String getString(String key) { 
        String result = null;
        try {
        result = (String)getObject(key);
        }
        catch (ClassCastException e) {
        reportTypeError("String", key);
        }
        return result;
    }

    public char getChar(String key) { 
        try {
        String s = getString(key);
        if (s != null) {
            return s.charAt(0);
        }
        }
        catch (IndexOutOfBoundsException e) {
        reportTypeError("char", key);
        }
        return 0xffff;
    }

    public int getInt(String key) { 
        try {
        String s = getString(key);
        if (s != null) {
            return Integer.parseInt(s);
        }
        }
        catch (NumberFormatException e) {
        reportTypeError("int", key);
        }
        return -1;
    }

    public byte getByte(String key) { 
        try {
        String s = getString(key);
        if (s != null) {
            return Byte.parseByte(s);
        }
        }
        catch (NumberFormatException e) {
        reportTypeError("byte", key);
        }
        return (byte)-1;
    }

    public boolean getBoolean(String key) { 
        String s = getString(key);
        if (s != null) {
        if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (!s.equalsIgnoreCase("false")) {
            reportTypeError("boolean", key);
        }
        }
        return false;
    }

    public Object[] getObjectArray(String key) {
        try {
        Object[] result = (Object[])na.getValue(key);
        if (result == null && required) {
            reportNullError(key);
        }
        } catch (ClassCastException e) {
        reportTypeError("Object[]", key);
        }
        return null;
    }

    public String[] getStringArray(String key) { 
        try {
        String[] result = (String[])na.getValue(key);
        if (result == null && required) {
            reportNullError(key);
        }
        return result;
        }
        catch (ClassCastException e) {
        reportTypeError("String[]", key);
        }
        return null;
    }

    public char[] getCharArray(String key) { 
        try {
                return (char[])na.getValue(key);
            }
            catch (ClassCastException e) {
            }
            try{ 
        String temp = (String)na.getValue(key);
        if (temp == null) {
            if (required) {
            reportNullError(key);
            }
        } else {
            return temp.toCharArray();
        }
        }
        catch (ClassCastException e) {
        reportTypeError("char[]", key);
        }
        return null;
    }

    public int[] getIntArray(String key) {
        try {
                return (int[])na.getValue(key);
            }
            catch (ClassCastException e) {
            }
        String[] data = getStringArray(key);
        if (data != null) {
        try {
            int[] result = new int[data.length];
            for (int i = 0; i < data.length; ++i) {
            result[i] = Integer.parseInt(data[i]);
            }
            return result;
        }
        catch (NumberFormatException e) {
            reportTypeError("int[]", key);
        }
        }
        return null;
    }

    public byte[] getByteArray(String key) { 
        try {
                return (byte[])na.getValue(key);
            }
            catch (ClassCastException e) {
            }
        String[] data = getStringArray(key);
        if (data != null) {
        try {
            byte[] result = new byte[data.length];
            for (int i = 0; i < result.length; ++i) {
            result[i] = Byte.parseByte(data[i]);
            }
            return result;
        }
        catch (NumberFormatException e) {
            reportTypeError("byte[]", key);
        }
        }
        return null;
    }

    public boolean[] getBooleanArray(String key) {
        try {
                return (boolean[])na.getValue(key);
            }
            catch (ClassCastException e) {
            }
        String[] data = getStringArray(key);
        if (data != null) {
        boolean[] result = new boolean[data.length];
        for (int i = 0; i < result.length; ++i) {
            String s = data[i];
            if (s.equalsIgnoreCase("true")) {
            result[i] = true;
            } else if (s.equalsIgnoreCase("false")) {
            result[i] = false;
            } else {
            reportTypeError("boolean[]", key);
            return null;
            }
        }
        return result;
        }
        return null;
    }

    private void reportNullError(String key) {
        log.errln("Missing required value for '" + key + "'");
    }

    private void reportTypeError(String typeName, String key) {
        log.errln("Could not return value of '" + key + "' (" + getObject(key) + ") as type '" + typeName + "'");
    }
    }
}
