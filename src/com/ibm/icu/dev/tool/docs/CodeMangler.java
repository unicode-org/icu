/**
*******************************************************************************
* Copyright (C) 2004, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple facility for adding C-like preprocessing to .java files.
 * This only understands a subset of the C preprocessing syntax.
 * Its used to manage files that with only small differences can be
 * compiled for different JVMs.  Input is generally a '.jpp' file,
 * output is a '.java' file with the same name, that can then be
 * compiled.
 */
public class CodeMangler {
    private String infile;
    private String outfile;
    private Map map;

    private boolean verbose;

    public static void main(String[] args) {
	new CodeMangler(args).run();
    }

    private static final String usage = "Usage:\n" +
	"-in[file] path         - read from file, otherwise read stdin\n" +
	"-out[file] path        - write to output file, otherwise write stdout\n" +
	"-d[efine] NAME[=VALUE] - define NAME with optional value VALUE\n" +
	"\n" +
	"directives in the file are one of the following:\n" +
	"  #ifdef, #ifndef, #else, #endif, #if, #elif, #define, #undef\n" +
	"these may optionally be preceeded by whitespace or //.\n" +
	"#if, #elif args are of the form 'key == value' or 'key != value'\n" +
	"only exact character match key with value is performed.\n" +
	"#define args are 'key [==] value', the '==' is optional\n";

    CodeMangler(String[] args) {
	map = new HashMap();

	String arg = null;
	try {
	    for (int i = 0; i < args.length; ++i) {
		arg = args[i];
		if (arg.startsWith("-in")) {
		    infile = args[++i];
		} else if (arg.startsWith("-out")) {
		    outfile = args[++i];
		} else if (arg.startsWith("-d")) {
		    String id = args[++i];
		    String val = "";
		    int ix = id.indexOf('=');
		    if (ix >= 0) {
			val = id.substring(ix+1);
			id = id.substring(0,ix);
		    }
		    map.put(id, val);
		} else if (arg.startsWith("-help")) {
		    System.out.print(usage);
		    System.exit(0);
		} else if (arg.startsWith("-v")) {
		    verbose = true;
		} else {
		    System.err.println("Error: unrecognized argument '" + arg + "'");
		    System.err.println(usage);
		    System.exit(1);
		}
	    }
	} catch (IndexOutOfBoundsException e) {
	    System.err.println("Error: argument '" + arg + "' missing value");
	    System.err.println(usage);
	    System.exit(1);
	}
    }

    public void run() {
	Pattern pat = Pattern.compile(
	    "(?i)^\\s*(?://+)??\\s*#(ifdef\\s|ifndef\\s|else|endif|undef\\s|define\\s|if\\s|elif\\s)\\s*(.*)$");
 	Pattern pat2 = Pattern.compile("([^=!]+)\\s*([!=]=)??\\s*(\\w+)");
	try {
	    PrintStream outstream = System.out;
	    if (outfile != null) {
		outstream = new PrintStream(new FileOutputStream(outfile));
	    }

	    InputStream instream = System.in;
	    if (infile != null) {
		instream = new FileInputStream(infile);
	    }

	    class State {
		int lc;
		String line;
		boolean emit = true;
		boolean tripped;
		private State next;

		public String toString() {
		    return "line " + lc 
			+ ": '" + line 
			+ "' (emit: " + emit 
			+ " tripped: " + tripped 
			+ ")";
		}

		void trip(boolean trip) {
		    if (!tripped & trip) {
			tripped = true;
			emit = next != null ? next.emit : true;
		    } else {
			emit = false;
		    }
		}
			
		State push(int lc, String line, boolean trip) {
		    this.lc = lc;
		    this.line = line;
		    State ret = new State();
		    ret.next = this;
		    ret.emit = this.emit & trip;
		    ret.tripped = trip;
		    return ret;
		}

		State pop() {
		    return next;
		}
	    }

	    BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
	    String line = null;
	    int lc = 0;
	    State state = new State();
	    while ((line = reader.readLine()) != null) {
		Matcher m = pat.matcher(line);
		if (m.find()) {
		    String key = m.group(1).toLowerCase().trim();
		    String val = m.group(2).trim();
		    if (verbose) outstream.println("directive: " + line
						   + " key: '" + key
						   + "' val: '" + val 
						   + "' " + state);
		    if (key.equals("ifdef")) {
			state = state.push(lc, line, map.get(val) != null);
		    } else if (key.equals("ifndef")) {
			state = state.push(lc, line, map.get(val) == null);
		    } else if (key.equals("else")) {
			state.trip(true);
		    } else if (key.equals("endif")) {
			state = state.pop();
		    } else if (key.equals("undef")) {
			if (state.emit) {
			    map.remove(val);
			}
		    } else {
			Matcher m2 = pat2.matcher(val);
			if (m2.find()) {
			    String key2 = m2.group(1).trim();
			    boolean neq = "!=".equals(m2.group(2)); // optional
			    String val2 = m2.group(3).trim();
			    if (verbose) outstream.println("val2: '" + val2 
							   + "' neq: '" + neq 
							   + "' key2: '" + key2 
							   + "'");
			    if (key.equals("if")) {
				state = state.push(lc, line, val2.equals(map.get(key2)) != neq);
			    } else if (key.equals("elif")) {
				state.trip(val2.equals(map.get(key2)) != neq);
			    } else if (key.equals("define")) {
				if (state.emit) {
				    map.put(key2, val2);
				}
			    }
			}
		    }
		    continue;
		}

		lc++;
		if (state.emit) {
		    outstream.println(line);
		} else {
		    if (verbose) outstream.println("skipping: " + line);
		}
	    }

	    state = state.pop();
	    if (state != null) {
		System.out.println("Error: unclosed directive(s):");
		do {
		    System.err.println(state);
		} while ((state = state.pop()) != null);
	    }
		
	    outstream.close();
	    instream.close();
	}
	catch (IOException e) {
	    System.out.println(e);
	}
    }
}
