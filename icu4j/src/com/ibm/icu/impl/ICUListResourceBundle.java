/**
 * A hacked list resource bundle that does redirection 
 * because otherwise some of our resource class files
 * are too big for the java runtime to handle.
 */

package com.ibm.icu.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class ICUListResourceBundle extends ListResourceBundle {

    protected ICUListResourceBundle() {
    }

    //    protected static final Integer COMPRESSED_BINARY 		= new Integer(1);
    protected static final Integer RESOURCE_BINARY   		= new Integer(2);
    //    protected static final Integer COMPRESSED_STRING	 	= new Integer(3);
    //    protected static final Integer COMPRESSED_BINARY_STRING = new Integer(4);
    protected static final Integer RESOURCE_UNICODE = new Integer(5);

    /**
     * Subclassers must statically initialize this
     */
    protected Object[][] contents;

    /**
     * This is our cache
     */
    private Object[][] realContents;

    /**
     * See base class description
     */
    protected Object[][] getContents(){
	// we replace any redirected values with real values in a cloned array

	if (realContents == null) {
	    realContents = contents;
	    for (int i = 0; i < contents.length; ++i) {
		Object newValue = getRedirectedValue(contents[i][1]);
		if (newValue != null) {
		    if (realContents == contents) {
			realContents = (Object[][])contents.clone();
		    }
		    realContents[i] = new Object[] { contents[i][0], newValue };
		}
	    }
	}

	return realContents;
    }

    /**
     * Return null if value is already in existing contents array, otherwise fetch the
     * real value and return it.
     */
    private Object getRedirectedValue(Object value) {

	// what we really want is:
	// if (value instanceof ICUListResourceBundle.Redirect) {
	//    return ((ICUListResourcBundle.Redirect)value).getValue();
	// }
	// return null;
	// if we really want to support multiple encoding types, then we just have
	// different instances of ICUListResourceBundle.Redirect.

	// value is always an array of one object which is an array of two objects,
	// encoding type and resource name.
	// encoding type is always RESOURCE_BINARY or RESOURCE_UNICODE

	if (value instanceof Object[][]) {
	    Object[][] aValue = (Object[][])value;
	    if (aValue.length == 1) {
		Object[] bValue = (Object[])aValue[0];
		String resName = (String)bValue[1];
		try {
		    if (bValue[0] == RESOURCE_BINARY) {
			// this code would be in RedirectByteArray.getValue, for example, and the resource
			// value would be an instance of this class
			// sigh, isn't there a better way to read the whole stream?
			// could get url instead, I suppose, and get the length of the data
			InputStream stream = this.getClass().getResourceAsStream(resName);
			byte[] result = readToEOS(stream);
			return result;
		    }
		    else if (bValue[0] == RESOURCE_UNICODE) {
			// temporarily disable because of BreakDictionaryData_th.problem
			// LocaleElements_th is the only resource currently using this.

			return null; 
			/*
			InputStream stream = this.getClass().getResourceAsStream(resName);
			InputStreamReader reader = new InputStreamReader(stream, "UTF-16LE");
			char[] result = readToEOS(reader);
			return result;
			*/
		    }
		} 
		catch (Exception e) {
		    return null;
		}
	    }
	}
		
	return null;
    }

    private static byte[] readToEOS(InputStream stream) {

	ArrayList vec = new ArrayList();
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
	    }
	    catch (IOException e) {
	    }
	    vec.add(buffer);
	    count += pos;
	} while (pos == length);
						
						
	byte[] data = new byte[count];
	pos = 0;
	for (int i = 0; i < vec.size(); ++i) {
	    byte[] buf = (byte[])vec.get(i);
	    int len = Math.min(buf.length, count - pos);
	    System.arraycopy(buf, 0, data, pos, len);
	    pos += len;
	}
	return data;
    }

    private static char[] readToEOS(InputStreamReader stream) {
	ArrayList vec = new ArrayList();
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
		    int n = stream.read(buffer, pos, length - pos);
		    if (n == -1) {
			break;
		    }
		    pos += n;
		} while (pos < length);
	    }
	    catch (IOException e) {
	    }
	    vec.add(buffer);
	    count += pos;
	} while (pos == length);
						
	char[] data = new char[count];
	pos = 0;
	for (int i = 0; i < vec.size(); ++i) {
	    char[] buf = (char[])vec.get(i);
	    int len = Math.min(buf.length, count - pos);
	    System.arraycopy(buf, 0, data, pos, len);
	    pos += len;
	}
	return data;
    }
}

