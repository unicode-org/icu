package com.ibm.icu.test.format;

/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and    
 * others. All Rights Reserved.                                                
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/format/IntlTestDateFormat.java,v $ 
 * $Date: 2001/07/23 09:32:46 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
 
 // Porting Notice1: from ICU4C v1.8.1
 // Porting Notice2: from format -> DateFormat -> IntlTestDateFormat

import com.ibm.text.*;
import com.ibm.test.TestFmwk;
import com.ibm.util.*;

public class IntlTestDateFormat extends com.ibm.test.TestFmwk {
	public DateFormat fDateFormat;
	public int fLimit;
	public java.lang.String fTestName;
/**
 * IntlTestDateFormat constructor comment.
 */
protected IntlTestDateFormat() {
	super();
	fDateFormat = DateFormat.getInstance();
	fLimit = 3;
	fTestName = "Generic test (Default Locale)";
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:29:05 AM)
 */
public void describeTest() {
	SimpleDateFormat s = (SimpleDateFormat)	fDateFormat;
	logln(fTestName + s.toPattern());	
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:36:52 AM)
 * @param locale java.util.Locale
 * @param localeName java.lang.String
 */
public void localeTest(final java.util.Locale locale, final String localeName) {
	//an array of Style option
	int[] stylePattern = {DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};
		
	fLimit = 3;	
	for (int i = 0; i < 4; i++) {
		fTestName = "Time Test" +  i + " (" + localeName + ")";
		fDateFormat = DateFormat.getTimeInstance(stylePattern[i], locale);
		testFormat();
	}

	fLimit = 2;	
	for (int i = 0; i < 4; i++) {
		fTestName = "Date Test" +  i + " (" + localeName + ")";
		fDateFormat = DateFormat.getDateInstance(stylePattern[i], locale);
		testFormat();
	}

	fLimit = 1;	
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			fTestName = "Date Test" +  i + "/" + j + " (" + localeName + ")";
			fDateFormat = DateFormat.getDateTimeInstance(stylePattern[i], stylePattern[j], locale);
			testFormat();
		}
	}	
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:30:06 AM)
 * @param args java.lang.String[]
 */
public static void main(String[] args) throws Exception {
	new IntlTestDateFormat().run(args);
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:39:20 AM)
 */
public void testAvailableLocales() {
	final java.util.Locale[] locale = DateFormat.getAvailableLocales();
	int count = locale.length;
	logln(count + " available locales");

	if (count != 0) {
		String all = "";
		for (int i = 0; i < count; i++) {
			if (i != 0) {
				all += ", ";
			}
			all += locale[i].getDisplayName();
		}
		logln(all);	
	} else {
		 errln("**** FAIL: Zero available locales or null array pointer");
	}
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:31:18 AM)
 */
public void testFormat() {
	if (fDateFormat == null) {
		errln("FAIL: DateForamt create failed, DateForamt.getInstance()");
		return;	
	}
	
	describeTest();

	Calendar calendar = Calendar.getInstance();
	java.util.Date now = calendar.getTime();

	tryDate(new java.util.Date(0));
	tryDate(new java.util.Date((long)1278161801778.0));
	tryDate(new java.util.Date((long)5264498352317.0)); // Sunday, October 28, 2136 8:39:12 AM PST
	tryDate(now);
	//System.out.println(new java.util.Date((long)5264498352317.0));	
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:33:54 AM)
 */
public void testLocale() {
//an array of Style option
	int[] stylePattern = {DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};
	
	final java.util.Locale locale = java.util.Locale.getDefault();
	final String localeName = "Default Locale";
	
	fLimit = 3;	
	for (int i = 0; i < 4; i++) {
		fTestName = "Time Test" +  i + " (" + localeName + ")";
		fDateFormat = DateFormat.getTimeInstance(stylePattern[i], locale);
		testFormat();
	}

	fLimit = 2;	
	for (int i = 0; i < 4; i++) {
		fTestName = "Date Test" +  i + " (" + localeName + ")";
		fDateFormat = DateFormat.getDateInstance(stylePattern[i], locale);
		testFormat();
	}

	fLimit = 1;	
	for (int i = 0; i < 4; i++) {
		for (int j = 0; j < 4; j++) {
			fTestName = "Date Test" +  i + "/" + j + " (" + localeName + ")";
			fDateFormat = DateFormat.getDateTimeInstance(stylePattern[i], stylePattern[j], locale);
			testFormat();
		}
	}		
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:34:31 AM)
 */
public void testMonster() {
	final java.util.Locale[] locale = DateFormat.getAvailableLocales();
	int count = locale.length;
	logln(count + " available locales");

	if (count != 0) {
		String all = "";
		
		count = 3;
		
		for (int i = 0; i < count; i++) {
			String name = locale[i].getDisplayName();
			logln("Testing " + name + "...");
			localeTest(locale[i], name);
		}		
	} else {
		 errln("**** FAIL: Zero available locales or null array pointer");
	}
}
/**
 * Insert the method's description here.
 * Creation date: (7/23/2001 11:32:56 AM)
 * @param theDate java.util.Date
 */
public void tryDate(java.util.Date theDate) {
	final int DEPTH = 10;
	java.util.Date[] date = new java.util.Date[DEPTH];
	String[] str = new String[DEPTH];

	int dateMatch = 0;
	int stringMatch = 0;
	boolean dump = false;
	int i;

	date[0] = theDate;
	str[0] = fDateFormat.format(date[0]);

	for (i = 1; i < DEPTH; i++) {
		try {
			date[i] = fDateFormat.parse(str[i - 1]);
		} catch (java.text.ParseException pe) {
			describeTest();
			errln("**** FAIL: Parse of " + str[i-1] + " failed.");
			dump = true;
			break;
		}
		str[i] = fDateFormat.format(date[i]);
		
		if (dateMatch == 0 && date[i] == date[i - 1]) {
			dateMatch = i;
		}else if (dateMatch > 0 && date[i] != date[i - 1]) {
			describeTest();
			errln("**** FAIL: Date mismatch after match for " + str[i]);
			dump = true;
			break;
		}
		
		if (stringMatch == 0 && str[i] == str[i - 1]) {
			stringMatch = i;
		} else if (stringMatch > 0 && str[i] != str[i - 1]) {
			describeTest();
			errln("**** FAIL: String mismatch after match for " + str[i]);
			dump = true;
			break;
		}
		if (dateMatch > 0 && stringMatch > 0) {
			break;
		}
	}
	
	if (i == DEPTH) {
		--i;
	}

	if (stringMatch > fLimit || dateMatch > fLimit) {
		describeTest();
		errln("**** FAIL: No string and/or date match within " + fLimit
			+ " iterations for the Date " + str[0] + "\t(" + theDate + ").");
		dump = true;
	}

	if (dump)
	{
		for (int k=0; k<=i; ++k)
		{
			logln("" + k + ": " + date[k] + " F> " +
				  str[k] + " P> ");
		}
	}

}
}
