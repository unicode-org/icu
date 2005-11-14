/**
*******************************************************************************
* Copyright (C) 2004-2005, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
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
    private File indir;      // root of input
    private File outdir;     // root of output
    private String suffix;   // suffix to process, default '.jpp'
    private boolean recurse; // true if recurse on directories
    private Map map;         // defines
    private ArrayList names; // files/directories to process

    private boolean verbose; // true if we emit debug output

    public static void main(String[] args) {
        new CodeMangler(args).run();
    }

    private static final String usage = "Usage:\n" +
        "    CodeMangler [flags] file... dir...\n" +
        "-in[dir] path          - root directory of input files, otherwise use current directory\n" +
        "-out[dir] path         - root directory of output files, otherwise use current directory\n" +
        "-suffix string         - suffix of files to process, otherwise use '.jpp' (directories only)\n" +
        "-r                     - if present, recursively process subdirectories\n" +
        "-d[efine] NAME[=VALUE] - define NAME with optional value VALUE\n" +
        "-help                  - print this usage message and exit.\n" +
        "\n" +
        "For file arguments, output '.java' files using the same path/name under the output directory.\n" +
        "For directory arguments, process all files with the defined suffix in the directory.\n" +
        "  (if recursing, do the same for all files recursively under each directory)\n" +
        "\n" +
        "Directives in the file are one of the following:\n" +
        "  #ifdef, #ifndef, #else, #endif, #if, #elif, #define, #undef\n" +
        "These may optionally be preceeded by whitespace or //.\n" +
        "#if, #elif args are of the form 'key == value' or 'key != value'.\n" +
        "Only exact character match key with value is performed.\n" +
        "#define args are 'key [==] value', the '==' is optional.\n";

    CodeMangler(String[] args) {
        map = new HashMap();
        names = new ArrayList();
        suffix = ".jpp";

        String inname = null;
        String outname = null;
        boolean processArgs = true;
        String arg = null;
        try {
            for (int i = 0; i < args.length; ++i) {
                arg = args[i];
                if ("--".equals(arg)) {
                    processArgs = false;
                } else if (processArgs && arg.charAt(0) == '-') {
                    if (arg.startsWith("-in")) {
                        inname = args[++i];
                    } else if (arg.startsWith("-out")) {
                        outname = args[++i];
                    } else if (arg.startsWith("-d")) {
                        String id = args[++i];
                        String val = "";
                        int ix = id.indexOf('=');
                        if (ix >= 0) {
                            val = id.substring(ix+1);
                            id = id.substring(0,ix);
                        }
                        map.put(id, val);
                    } else if ("-suffix".equals(arg)) {
                        suffix = args[++i];
                    } else if ("-r".equals(arg)) {
                        recurse = true;
                    } else if (arg.startsWith("-help")) {
                        System.out.print(usage);
                        break; // stop before processing arguments, so we will do nothing
                    } else if (arg.startsWith("-v")) {
                        verbose = true;
                    } else {
                        System.err.println("Error: unrecognized argument '" + arg + "'");
                        System.err.println(usage);
                        throw new IllegalArgumentException(arg);
                    }
                } else {
                    names.add(arg);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            String msg = "Error: argument '" + arg + "' missing value";
            System.err.println(msg);
            System.err.println(usage);
            throw new IllegalArgumentException(msg);
        }

        String username = System.getProperty("user.dir");
        if (inname == null) {
            inname = username;
        } else if (!(inname.startsWith("\\") || inname.startsWith("/"))) {
            inname = username + File.separator + inname;
        }
        indir = new File(inname);
        try {
            indir = indir.getCanonicalFile();
        }
        catch (IOException e) {
            // continue, but most likely we'll fail later
        }
        if (!indir.exists()) {
            throw new IllegalArgumentException("Input directory '" + indir.getAbsolutePath() + "' does not exist.");
        } else if (!indir.isDirectory()) {
            throw new IllegalArgumentException("Input path '" + indir.getAbsolutePath() + "' is not a directory.");
        }
        if (verbose) System.err.println("indir: " + indir.getAbsolutePath());

        if (outname == null) {
            outname = username;
        } else if (!(outname.startsWith("\\") || outname.startsWith("/"))) {
            outname = username + File.separator + outname;
        }
        outdir = new File(outname);
        try {
            outdir = outdir.getCanonicalFile();
        }
        catch (IOException e) {
            // continue, but most likely we'll fail later
        }
        if (!outdir.exists()) {
            throw new IllegalArgumentException("Output directory '" + outdir.getAbsolutePath() + "' does not exist.");
        } else if (!outdir.isDirectory()) {
            throw new IllegalArgumentException("Output path '" + outdir.getAbsolutePath() + "' is not a directory.");
        }
        if (verbose) System.err.println("outdir: " + outdir.getAbsolutePath());

        if (names.isEmpty()) {
            names.add(".");
        }
    }

    public int run() {
        return process("", (String[])names.toArray(new String[names.size()]));
    }

    public int process(String path, String[] filenames) {
        if (verbose) System.err.println("path: '" + path + "'");
        int count = 0;
        for (int i = 0; i < filenames.length; ++i) {
            if (verbose) System.err.println("name " + i + " of " + filenames.length + ": '" + filenames[i] + "'");
            String name = path + filenames[i];
            File fin = new File(indir, name);
            try {
                fin = fin.getCanonicalFile();
            }
            catch (IOException e) {
            }
            if (!fin.exists()) {
                System.err.println("File " + fin.getAbsolutePath() + " does not exist.");
                continue;
            }
            if (fin.isFile()) {
                if (verbose) System.err.println("processing file: '" + fin.getAbsolutePath() + "'");
                String oname;
                int ix = name.lastIndexOf(".");
                if (ix != -1) {
                    oname = name.substring(0, ix);
                } else {
                    oname = name;
                }
                oname += ".java";
                File fout = new File(outdir, oname);
                String foutpname = fout.getParent();
                if (foutpname != null) {
                    File foutp = new File(foutpname);
                    if (!(foutp.exists() || foutp.mkdirs())) {
                        System.err.println("could not create directory: '" + foutpname + "'");
                        continue;
                    }
                }
                if (processFile(fin, fout)) {
                    ++count;
                }
            } else if (fin.isDirectory()) {
                if (verbose) System.err.println("recursing on directory '" + fin.getAbsolutePath() + "'");
                String npath = ".".equals(name) ? path : path + fin.getName() + File.separator;
                count += process(npath, fin.list(filter)); // recursive call
            }
        }
        return count;
    }

                
    private final FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                File f = new File(dir, name);
                return (f.isFile() && name.endsWith(suffix)) || (f.isDirectory() && recurse);
            }
        };

    public boolean processFile(File infile, File outfile) {
        Pattern pat = Pattern.compile(
            "(?i)^\\s*(?://+)??\\s*#(ifdef\\s|ifndef\\s|else|endif|undef\\s|define\\s|if\\s|elif\\s)\\s*(.*)$");
        Pattern pat2 = Pattern.compile("([^=!]+)\\s*([!=]=)??\\s*(\\w+)");
        try {
            PrintStream outstream = new PrintStream(new FileOutputStream(outfile));
            InputStream instream = new FileInputStream(infile);

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
            System.err.println(e);
            return false;
        }
        return true;
    }
}
