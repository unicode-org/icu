package com.ibm.icu4jni.test;

public interface TestLog {

    /**
     * Adds given string to the log if we are in verbose mode.
     */
    void log(String message);

    void logln(String message);

    /**
     * Report an error
     */
    void err(String message);

    void errln(String message);
}
