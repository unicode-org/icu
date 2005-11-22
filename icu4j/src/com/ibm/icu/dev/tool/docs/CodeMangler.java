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
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple facility for adding C-like preprocessing to .java files.
 * This only understands a subset of the C preprocessing syntax.
 * Its used to manage files that with only small differences can be
 * compiled for different JVMs.  This changes files in place, 
 * commenting out lines based on the current flag settings.
 */
public class CodeMangler {
    private File indir;      // root of input
    private File outdir;     // root of output
    private String suffix;   // suffix to process, default '.jpp'
    private boolean recurse; // true if recurse on directories
    private boolean force;   // true if force reprocess of files
    private boolean clean;   // true if output is to be cleaned
    private HashMap map;     // defines
    private ArrayList names; // files/directories to process
    private String header;   // sorted list of defines passed in

    private boolean verbose; // true if we emit debug output

    private static final String IGNORE_PREFIX = "//##";
    private static final String HEADER_PREFIX = "//##header";

    public static void main(String[] args) {
        new CodeMangler(args).run();
    }

    private static final String usage = "Usage:\n" +
        "    CodeMangler [flags] file... dir... @argfile... \n" +
        "-in[dir] path          - root directory of input files, otherwise use current directory\n" +
        "-out[dir] path         - root directory of output files, otherwise use input directory\n" +
        "-s[uffix] string       - suffix of inputfiles to process, otherwise use '.java' (directories only)\n" +
        "-c[lean]               - remove all control flags from code on output (does not proceed if overwriting)\n" +
        "-r[ecurse]             - if present, recursively process subdirectories\n" +
        "-f[orce]               - force reprocessing of files even if timestamp and headers match\n" +
        "-dNAME[=VALUE]         - define NAME with optional value VALUE\n" +
        "  (or -d NAME[=VALUE])\n" +
        "-help                  - print this usage message and exit.\n" +
        "\n" +
        "For file arguments, output '.java' files using the same path/name under the output directory.\n" +
        "For directory arguments, process all files with the defined suffix in the directory.\n" +
        "  (if recursing, do the same for all files recursively under each directory)\n" +
        "For @argfile arguments, read the specified text file (strip the '@'), and process each line of that file as \n" +
        "an argument.\n" +
        "\n" +
        "Directives are one of the following:\n" +
        "  #ifdef, #ifndef, #else, #endif, #if, #elif, #define, #undef\n" +
        "These may optionally be preceeded by whitespace or //.\n" +
        "#if, #elif args are of the form 'key == value' or 'key != value'.\n" +
        "Only exact character match key with value is performed.\n" +
        "#define args are 'key [==] value', the '==' is optional.\n";

    CodeMangler(String[] args) {
        map = new HashMap();
        names = new ArrayList();
        suffix = ".java";
        clean = false;

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
                        String id = arg.substring(2);
                        if (id.length() == 0) {
                            id = args[++i];
                        }
                        String val = "";
                        int ix = id.indexOf('=');
                        if (ix >= 0) {
                            val = id.substring(ix+1);
                            id = id.substring(0,ix);
                        }
                        map.put(id, val);
                    } else if (arg.startsWith("-s")) {
                        suffix = args[++i];
                    } else if (arg.startsWith("-r")) {
                        recurse = true;
                    } else if (arg.startsWith("-f")) {
                        force = true;
                    } else if (arg.startsWith("-c")) {
                        clean = true;
                    } else if (arg.startsWith("-h")) {
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
                    if (arg.charAt(0) == '@') {
                        File argfile = new File(arg.substring(1));
                        if (argfile.exists() && !argfile.isDirectory()) {
                            try {
                                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(argfile)));
                                ArrayList list = new ArrayList();
                                for (int x = 0; x < args.length; ++x) {
                                    list.add(args[x]);
                                }
                                String line;
                                while (null != (line = br.readLine())) {
                                    line = line.trim();
                                    if (line.length() > 0 && line.charAt(0) != '#') {
                                        if (verbose) System.out.println("adding argument: " + line);
                                        list.add(line);
                                    }
                                }
                                args = (String[])list.toArray(new String[list.size()]);
                            }
                            catch (IOException e) {
                                System.err.println("error reading arg file: " + e);
                            }
                        }
                    } else {
                        names.add(arg);
                    }
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
        if (verbose) System.out.println("indir: " + indir.getAbsolutePath());

        if (outname == null) {
            outname = inname;
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
        if (verbose) System.out.println("outdir: " + outdir.getAbsolutePath());

        
        if (clean && suffix.equals(".java")) {
            try {
                if (outdir.getCanonicalPath().equals(indir.getCanonicalPath())) {
                    throw new IllegalArgumentException("Cannot use 'clean' to overwrite .java files in same directory tree");
                }
            }
            catch (IOException e) {
                System.err.println("possible overwrite, error: " + e.getMessage());
                throw new IllegalArgumentException("Cannot use 'clean' to overrwrite .java files");
            }
        }

        if (names.isEmpty()) {
            names.add(".");
        }

        TreeMap sort = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        sort.putAll(map);
        Iterator iter = sort.entrySet().iterator();
        StringBuffer buf = new StringBuffer();
        while (iter.hasNext()) {
            Map.Entry e = (Map.Entry)iter.next();
            if (buf.length() > 0) {
                buf.append(", ");
            }
            buf.append(e.getKey());
            String v = (String)e.getValue();
            if (v != null && v.length() > 0) {
                buf.append('=');
                buf.append(v);
            }
        }
        header = buf.toString();
    }

    public int run() {
        return process("", (String[])names.toArray(new String[names.size()]));
    }

    public int process(String path, String[] filenames) {
        if (verbose) System.out.println("path: '" + path + "'");
        int count = 0;
        for (int i = 0; i < filenames.length; ++i) {
            if (verbose) System.out.println("name " + i + " of " + filenames.length + ": '" + filenames[i] + "'");
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
                if (verbose) System.out.println("processing file: '" + fin.getAbsolutePath() + "'");
                String oname;
                int ix = name.lastIndexOf(".");
                if (ix != -1) {
                    oname = name.substring(0, ix);
                } else {
                    oname = name;
                }
                oname += ".java";
                File fout = new File(outdir, oname);
                if (processFile(fin, fout)) {
                    ++count;
                }
            } else if (fin.isDirectory()) {
                if (verbose) System.out.println("recursing on directory '" + fin.getAbsolutePath() + "'");
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
        File backup = null;

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
        };

        final Pattern pat = Pattern.compile(
            "(?i)^(\\s*(?://+)??\\s*)#(ifdef\\s|ifndef\\s|else|endif|undef\\s|define\\s|if\\s|elif\\s)\\s*(.*)$");
        final Pattern pat2 = Pattern.compile("([^=!]+)\\s*([!=]?=)??\\s*(\\w+)");
        final Pattern pat3 = Pattern.compile("^(\\s*//##).*");
        HashMap oldMap = null;
        
        long outModTime = 0;

        try {
            PrintStream outstream = null;
            InputStream instream = new FileInputStream(infile);

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
            int lc = 0;
            State state = new State();
            String line;
            while ((line = reader.readLine()) != null) {
                if (lc == 0) { // check and write header for output file if needed
                    boolean hasHeader = line.startsWith(HEADER_PREFIX);
                    if (hasHeader && !force) {
                        long expectLastModified = ((infile.lastModified() + 999)/1000)*1000;
                        String headerline = HEADER_PREFIX + ' ' + String.valueOf(expectLastModified) + ' ' + header;
                        if (line.equals(headerline)) {
                            if (verbose) System.out.println("no changes necessary to " + infile.getCanonicalPath());
                            instream.close();
                            return false; // nothing to do
                        }
                        if (verbose) {
                            System.out.println("  old header:  " + line);
                            System.out.println("  != expected: " + headerline);
                        }
                    }

                    // create output file directory structure
                    String outpname = outfile.getParent();
                    if (outpname != null) {
                        File outp = new File(outpname);
                        if (!(outp.exists() || outp.mkdirs())) {
                            System.err.println("could not create directory: '" + outpname + "'");
                            return false;
                        }
                    }

                    // if we're overwriting, use a temporary file
                    if (suffix.equals(".java")) {
                        backup = outfile;
                        try {
                            outfile = File.createTempFile(outfile.getName(), null, outfile.getParentFile());
                        }
                        catch (IOException ex) {
                            System.err.println(ex.getMessage());
                            return false;
                        }
                    }
            
                    outModTime = ((outfile.lastModified()+999)/1000)*1000; // round up
                    outstream = new PrintStream(new FileOutputStream(outfile));
                    String headerline = HEADER_PREFIX + ' ' + String.valueOf(outModTime) + ' ' + header;
                    outstream.println(headerline);
                    if (verbose) System.out.println("header: " + headerline);

                    // discard the old header if we had one, otherwise match this line like any other
                    if (hasHeader) {
                        ++lc; // mark as having read a line so we never reexecute this block
                        continue;
                    }
                }
                        
                Matcher m = pat.matcher(line);
                if (m.find()) {
                    String lead = m.group(1);
                    String key = m.group(2).toLowerCase().trim();
                    String val = m.group(3).trim();
                    if (verbose) System.out.println("directive: " + line
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
                            if (oldMap == null) {
                                oldMap = (HashMap)map.clone();
                            }
                            map.remove(val);
                        }
                    } else { // #define, #if, #elif
                        Matcher m2 = pat2.matcher(val);
                        if (m2.find()) {
                            String key2 = m2.group(1).trim();
                            boolean neq = "!=".equals(m2.group(2)); // optional
                            String val2 = m2.group(3).trim();
                            if (verbose) System.out.println("val2: '" + val2 
                                                           + "' neq: '" + neq 
                                                           + "' key2: '" + key2 
                                                           + "'");
                            if (key.equals("if")) {
                                state = state.push(lc, line, val2.equals(map.get(key2)) != neq);
                            } else if (key.equals("elif")) {
                                state.trip(val2.equals(map.get(key2)) != neq);
                            } else if (key.equals("define")) {
                                if (state.emit) {
                                    if (oldMap == null) {
                                        oldMap = (HashMap)map.clone();
                                    }
                                    map.put(key2, val2);
                                }
                            }
                        }
                    }
                    if (!clean) {
                        lc++;
                        if (!lead.equals("//")) {
                            outstream.print("//");
                            line = line.substring(lead.length());
                        }
                        outstream.println(line);
                    }
                    continue;
                }

                lc++;
                m = pat3.matcher(line);
                boolean hasIgnore = m.find();
                if (state.emit == hasIgnore) {
                    if (state.emit) {
                        line = line.substring(m.group(1).length());
                    } else {
                        line = IGNORE_PREFIX + line;
                    }
                } else if (hasIgnore && !m.group(1).equals(IGNORE_PREFIX)) {
                    line = IGNORE_PREFIX + line.substring(m.group(1).length());
                }
                if (!clean || state.emit) {
                    outstream.println(line);
                }
            }

            state = state.pop();
            if (state != null) {
                System.err.println("Error: unclosed directive(s):");
                do {
                    System.err.println(state);
                } while ((state = state.pop()) != null);
                System.err.println(" in file: " + outfile.getCanonicalPath());
                if (oldMap != null) {
                    map = oldMap;
                }
                outstream.close();
                return false;
            }
                
            outstream.close();
            instream.close();

            if (backup != null) {
                if (backup.exists()) {
                    backup.delete();
                }
                outfile.renameTo(backup);
            }

            outfile.setLastModified(outModTime); // synch with timestamp

            if (oldMap != null) {
                map = oldMap;
            }
        }
        catch (IOException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }
}
