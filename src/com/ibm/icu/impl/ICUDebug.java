package com.ibm.icu.impl;

import com.ibm.icu.util.VersionInfo;

import java.util.ResourceBundle;

public final class ICUDebug {
    private static String params;
    static {
	try {
	    params = System.getProperty("ICUDebug");
	}
	catch (SecurityException e) {
	}
    }
    private static boolean debug = params != null;
    private static boolean help = debug && (params.equals("") || params.indexOf("help") != -1);

    static {
	if (debug) {
	    System.out.println("\nICUDebug=" + params);
	}
    }

    public static final String javaVersionString = System.getProperty("java.version");

    public static final VersionInfo javaVersion;
    static {
	String vs = javaVersionString;
	int index = vs.indexOf('-');
	if (index != -1) {
	    vs = vs.substring(0, index);
	}
	index = vs.indexOf('b'); // 'beta'
	if (index != -1) {
	    vs = vs.substring(0, index);
	}

	vs = vs.replace('_', '.');
	javaVersion = VersionInfo.getInstance(vs);
    }

    public static boolean enabled() {
	return debug;
    }

    public static boolean enabled(String arg) {
	if (debug) {
	    boolean result = params.indexOf(arg) != -1;
	    if (help) System.out.println("\nICUDebug.enabled(" + arg + ") = " + result);
	    return result;
	}
	return false;
    }

    public static String value(String arg) {
	String result = "false";
	if (debug) {
	    int index = params.indexOf(arg);
	    if (index != -1) {
		index += arg.length();
		if (params.length() > index && params.charAt(index) == '=') {
		    index += 1;
		    int limit = params.indexOf(",", index);
		    result = params.substring(index, limit == -1 ? params.length() : limit);
		}
		result = "true";
	    }

	    if (help) System.out.println("\nICUDebug.value(" + arg + ") = " + result);
	}
	return result;
    }
}
