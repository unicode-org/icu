/**
*******************************************************************************
* Copyright (C) 2004-2009, International Business Machines Corporation and    *
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


/**
 * A simple facility for adding C-like preprocessing to .java files.
 * This only understands a subset of the C preprocessing syntax.
 * Its used to manage files that with only small differences can be
 * compiled for different JVMs.  This changes files in place, 
 * commenting out lines based on the current flag settings.
 */
public class CodeMangler {
    private File indir;        // root of input
    private File outdir;       // root of output
    private String suffix;     // suffix to process, default '.jpp'
    private boolean recurse;   // true if recurse on directories
    private boolean force;     // true if force reprocess of files
    private boolean clean;     // true if output is to be cleaned
    private boolean timestamp; // true if we read/write timestamp
    private boolean nonames;   // true if no names in header
    private HashMap map;       // defines
    private ArrayList names;   // files/directories to process
    private String header;     // sorted list of defines passed in

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
        "-t[imestamp]           - expect/write timestamp in header\n" +
        "-dNAME[=VALUE]         - define NAME with optional value VALUE\n" +
        "  (or -d NAME[=VALUE])\n" +
        "-n                     - do not put NAME/VALUE in header\n" +
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
        timestamp = false;

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
                    } else if (arg.startsWith("-t")) {
                        timestamp = true;
                    } else if (arg.startsWith("-h")) {
                        System.out.print(usage);
                        break; // stop before processing arguments, so we will do nothing
                    } else if (arg.startsWith("-v")) {
                        verbose = true;
                    } else if (arg.startsWith("-n")) {
                        nonames = true;
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
        if (!nonames) {
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
        }
          
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
                        String headerline = HEADER_PREFIX;
                        if (header.length() > 0) {
                            headerline += " ";
                            headerline += header;
                        }
                        if (timestamp) {
                            headerline += " ";
                            headerline += String.valueOf(expectLastModified);
                        }
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
                    String headerline = HEADER_PREFIX;
                    if (header.length() > 0) {
                        headerline += " ";
                        headerline += header;
                    }
                    if (timestamp) {
                        headerline += " ";
                        headerline += String.valueOf(outModTime);
                    }
                    outstream.println(headerline);
                    if (verbose) System.out.println("header: " + headerline);

                    // discard the old header if we had one, otherwise match this line like any other
                    if (hasHeader) {
                        ++lc; // mark as having read a line so we never reexecute this block
                        continue;
                    }
                }
                
                String[] res = new String[3];
                if (patMatch(line, res)) {
                    String lead = res[0];
                    String key = res[1];
                    String val = res[2];

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
                    } else if (key.equals("define")) {
                        if (pat2Match(val, res)) {
                            String key2 = res[0];
                            String val2 = res[2];

                            if (verbose) System.out.println("val2: '" + val2 
                                                            + "' key2: '" + key2 
                                                            + "'");
                            if (state.emit) {
                                if (oldMap == null) {
                                    oldMap = (HashMap)map.clone();
                                }
                                map.put(key2, val2);
                            }
                        }
                    } else { // #if, #elif
                        // only top level OR (||) operator is supported for now
                        int count = 1;
                        int index = 0;
                        while ((index = val.indexOf("||", index)) > 0) {
                            count++;
                            index++;
                        }
                        String[] expressions = new String[count];
                        if (count == 1) {
                            expressions[0] = val;
                        } else {
                            int start = 0;
                            index = 0;
                            count = 0;
                            while (true) {
                                index = val.indexOf("||", start);
                                if (index > 0) {
                                    expressions[count++] = val.substring(start, index);
                                    start = index + 2;
                                } else {
                                    expressions[count++] = val.substring(start);
                                    break;
                                }
                            }
                        }
                        boolean eval = false;
                        for (count = 0; count < expressions.length && !eval; count++) {
                            if (pat2Match(expressions[count], res)) {
                                String key2 = res[0];
                                String val2 = res[2];

                                if (key2.equals("defined")) {
                                    // defined command
                                    if (verbose) System.out.println(
                                            "index: '" + count
                                            + "' val2: '" + val2 
                                            + "' key2: '" + key2 
                                            + "'");
                                    eval = map.containsKey(val2);
                                } else {
                                    boolean neq = false;
                                    if (res[1].equals("!=")) {
                                        neq = true;
                                    } else if (!res[1].equals("==")) {
                                        System.err.println("Invalid expression: '" + val);
                                    }
                                    if (verbose) System.out.println(
                                            "index: '" + count
                                            + "' val2: '" + val2 
                                            + "' neq: '" + neq 
                                            + "' key2: '" + key2 
                                            + "'");
                                    eval = (val2.equals(map.get(key2)) != neq);
                                }
                            }
                        }
                        if (key.equals("if")) {
                            state = state.push(lc, line, eval);
                        } else if (key.equals("elif")) {
                            state.trip(eval);
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
                String found = pat3Match(line);
                boolean hasIgnore = found != null;
                if (state.emit == hasIgnore) {
                    if (state.emit) {
                        line = line.substring(found.length());
                    } else {
                        line = IGNORE_PREFIX + line;
                    }
                } else if (hasIgnore && !found.equals(IGNORE_PREFIX)) {
                    line = IGNORE_PREFIX + line.substring(found.length());
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

            if (timestamp) {
                outfile.setLastModified(outModTime); // synch with timestamp
            }

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


    /**
     * Perform same operation as matching on pat.  on exit
     * leadKeyValue contains the three strings lead, key, and value.
     * 'lead' is the portion before the #ifdef directive.  'key' is
     * the directive.  'value' is the portion after the directive.  if
     * there is a match, return true, else return false.
     */
    static boolean patMatch(String line, String[] leadKeyValue) {
        if (line.length() == 0) {
            return false;
        }
        if (!line.endsWith("\n")) {
            line = line + '\n';
        }
        int mark = 0;
        int state = 0;
        loop: for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            switch (state) {
            case 0: // at start of line, haven't seen anything but whitespace yet
                if (c == ' ' || c == '\t' || c == '\r') continue;
                if (c == '/') { state = 1; continue; }
                if (c == '#') { state = 4; continue; }
                return false;
            case 1: // have seen a single slash after start of line
                if (c == '/') { state = 2; continue; }
                return false;
            case 2: // have seen two or more slashes
                if (c == '/') continue;
                if (c == ' ' || c == '\t' || c == '\r') { state = 3; continue; }
                if (c == '#') { state = 4; continue; }
                return false;
            case 3: // have seen a space after two or more slashes
                if (c == ' ' || c == '\t' || c == '\r') continue;
                if (c == '#') { state = 4; continue; }
                return false;
            case 4: // have seen a '#' 
                leadKeyValue[0] = line.substring(mark, i-1);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) { mark = i; state = 5; continue; }
                return false;
            case 5: // an ascii char followed the '#'
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) continue;
                if (c == ' ' || c == '\t' || c == '\n') {
                    String key = line.substring(mark, i).toLowerCase();
                    if (key.equals("ifdef") ||
                        key.equals("ifndef") ||
                        key.equals("else") ||
                        key.equals("endif") ||
                        key.equals("undef") ||
                        key.equals("define") ||
                        key.equals("if") ||
                        key.equals("elif")) {
                        leadKeyValue[1] = key;
                        mark = i;
                        state = 6;
                        break loop;
                    }
                }
                return false;
            default:
                throw new IllegalStateException();
            }
        }
        if (state == 6) {
            leadKeyValue[2] = line.substring(mark, line.length()).trim();
            return true;
        }
        return false; // never reached, does the compiler know this?
    }

    /**
     * Perform same operation as matching on pat2.  on exit
     * keyRelValue contains the three strings key, rel, and value.
     * 'key' is the portion before the relation (or final word).  'rel' is
     * the relation, if present, either == or !=.  'value' is the final
     * word.  if there is a match, return true, else return false.
     */
    static boolean pat2Match(String line, String[] keyRelVal) {

        if (line.length() == 0) {
            return false;
        }
        keyRelVal[0] = keyRelVal[1] = keyRelVal[2] = "";
        int mark = 0;
        int state = 0;
        String command = null;
        loop: for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            switch (state) {
            case 0: // saw beginning or space, no rel yet
                if (c == ' ' || c == '\t' || c == '\n') {
                    continue;
                }
                if ((c == '!' || c == '=')) {
                    return false;
                }
                state = 1;
                continue;
            case 1: // saw start of a word
                if (c == ' ' || c == '\t') {
                    state = 2;
                }
                else if (c == '(') {
                    command = line.substring(0, i).trim();
                    if (!command.equals("defined")) {
                        return false;
                    }
                    keyRelVal[0] = command;
                    state = 2;
                }
                else if (c == '!' || c == '=') {
                    state = 3;
                }
                continue;
            case 2: // saw end of word, and space
                if (c == ' ' || c == '\t') {
                    continue;
                }
                else if (command == null && c == '(') {
                    continue;
                }
                else if (c == '!' || c == '=') {
                    state = 3;
                    continue;
                }
                keyRelVal[0] = line.substring(0, i-1).trim();
                mark = i;
                state = 4;
                break loop;
            case 3: // saw end of word, and '!' or '='
                if (c == '=') {
                    keyRelVal[0] = line.substring(0, i-1).trim();
                    keyRelVal[1] = line.substring(i-1, i+1);
                    mark = i+1;
                    state = 4;
                    break loop;
                }
                return false;
            default:
                break;
            }
        }
        switch (state) {
        case 0: 
            return false; // found nothing
        case 1: 
        case 2:
            keyRelVal[0] = line.trim(); break; // found only a word
        case 3:
            return false; // found a word and '!' or '=" then end of line, incomplete
        case 4:
            keyRelVal[2] = line.substring(mark).trim(); // found a word, possible rel, and who knows what
            if (command != null) {
                int len = keyRelVal[2].length();
                if (keyRelVal[2].charAt(len - 1) != ')') {
                    // closing parenthesis is missing
                    return false;
                }
                keyRelVal[2] = keyRelVal[2].substring(0, len - 1).trim();
            }
            break;
        default: 
            throw new IllegalStateException();
        }
        return true;
    }

    static String pat3Match(String line) {
        int state = 0;
        loop: for (int i = 0; i < line.length(); ++i) {
            char c = line.charAt(i);
            switch(state) {
            case 0: if (c == ' ' || c == '\t') continue;
                if (c == '/') { state = 1; continue; }
                break loop;
            case 1:
                if (c == '/') { state = 2; continue; }
                break loop;
            case 2:
                if (c == '#') { state = 3; continue; }
                break loop;
            case 3:
                if (c == '#') return line.substring(0, i+1);
                break loop;
            default:
                break loop;
            }
        }
        return null;
    }
}
