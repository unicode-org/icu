/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestUtil.java,v $
 * $Date: 2002/08/13 22:02:16 $
 * $Revision: 1.2 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class TestUtil {
	/**
	 * Standard path to the test data.
	 */
	public static final String DATA_PATH = "/src/com/ibm/icu/dev/data";

	/**
	 * Property for user-defined data path.
	 */
	public static final String DATA_PATH_PROPERTY = "ICUDataPath";

	/**
	 * Get path to test data.<p>
	 * 
	 * path is provided relative to the src path, however the user could 
	 * set a system property to change the directory path.<br>
	 */

	public static final File getDataPathRoot() {
		String s = System.getProperty(DATA_PATH_PROPERTY);
		if (s == null) {
			// assume user.dir is directly above src directory
			s = System.getProperty("user.dir");
			s = s + DATA_PATH;
		}
		
		File f = new File(s);
		if (!f.exists()) {
			throw new InternalError("cannot find ICU data root '" + f.getAbsolutePath() + "', try definining " + DATA_PATH_PROPERTY);
		}

		return f;
	}

	/**
	 * Return the data file at path 'name' rooted at the data path.
	 * For example, <pre>getDataFile("unicode/UnicodeData.txt");</pre>
	 */
	public static final File getDataFile(String name) {
		File f = new File(getDataPathRoot(), name);
		if (!f.exists()) {
			throw new InternalError("cannot find ICU data file '" + f.getAbsolutePath() + "'");
		}

		return f;
	}

	/**
	 * Return a buffered reader on the data file at path 'name' rooted at the data path
	 * with initial buffer size 'bufSize'.
	 */
	public static final BufferedReader getDataReader(String name, int bufSize) throws IOException {
		File f = getDataFile(name);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr, bufSize);
		return br;
	}

	/**
	 * Return a buffered reader on the data file at path 'name' rooted at the data path.
	 */
	public static final BufferedReader getDataReader(String name) throws IOException {
		return getDataReader(name, 1024);
	}

}
