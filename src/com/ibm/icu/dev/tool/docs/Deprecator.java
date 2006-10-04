/**
*******************************************************************************
* Copyright (C) 2005, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.tool.docs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.regex.*;

public final class Deprecator {
    private boolean undep;
    private int log;

    Deprecator(boolean undep, int log) {
        this.undep = undep;
        this.log = log;
    }

    public static void main(String[] args) {
        String srcPath = null;
        String dstPath = null;
        boolean undep = false;

        int log = 1;
        boolean help = false;
        StringBuffer err = new StringBuffer();

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("-src")) {
                srcPath = args[++i];
            } else if (arg.equals("-dst")) {
                dstPath = args[++i];
            } else if (arg.equals("-undep")) {
                undep = true;
            } else if (arg.equals("-help")) {
                help = true;
            } else if (arg.equals("-silent")) {
                log = 0;
            } else if (arg.equals("-log")) {
                log = 2;
            } else if (arg.equals("-logfiles")) {
                log = 3;
            } else if (arg.equals("-verbose")) {
                log = 4;
            } else {
                err.append("\nunrecognized argument: " + arg);
            }
        }

        File srcDir = null;
        File dstDir = null;

        if (srcPath == null) {
            err.append("\nsrc must be defined");
        } else {
            srcDir = new File(srcPath);
            if (!(srcDir.exists() && srcDir.isDirectory())) {
                err.append("\nsrc must be an existing directory: '" + srcPath + "'");
            }
        }
        if (dstPath == null) {
            err.append("\ndst must be defined");
        } else {
            dstDir = new File(dstPath);
            if (!dstDir.exists()) {
                if (!dstDir.mkdirs()) {
                    err.append("\nunable to create dst: '" + dstPath + "'");
                }
            } else if (!dstDir.isDirectory()) {
                err.append("\ndst exists but is not directory: '" + dstPath + "'");
            }
        }

        if (help || err.length() > 0) {
            if (!help) {
                System.err.println("Error: " + err.toString());
            }
            usage();
            return;
        }

        try {
            if (log > 0) {
                System.out.println("src: " + srcDir.getCanonicalPath());
                System.out.println("dst: " + dstDir.getCanonicalPath());
                System.out.println("undep: " + undep);
                System.out.flush();
            }

            new Deprecator(undep, log).process(srcDir, dstDir);

            if (log > 0) {
                System.out.println("done");
                System.out.flush();
            }
        }
        catch(Exception e) {
            System.err.println("Unexpected error: " + e);
        }
    }

    static void usage() {
        PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: Deprecator -src path -dst path [-help]");
        pw.println("  -src path : the root of the tree of files to work on");
        pw.println("  -dst path : the root of the tree to put the resulting files");
        pw.println("  -help     : print this usage message and exit, doing nothing");
        pw.println("  -undep    : remove deprecation tags if present (default false)");
        pw.println();
        pw.println("  Add or remove warning deprecations for ICU @draft and @internal APIs");
        pw.flush();
    }

    static final String stoplist = "!CVS";
    static final FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".java")) return true;
                if (new File(dir, name).isDirectory()) {
                    if (stoplist.indexOf("!"+name) == -1) {
                        return true;
                    }
                }
                return false;
            }
        };
            
    void process(File srcDir, File dstDir) {
        File[] files = srcDir.listFiles(ff);
        for (int i = 0; i < files.length; ++i) {
            File f = files[i];
            File d = new File(dstDir, f.getName());
            if (f.isDirectory()) {
                if (!d.exists()) {
                    if (!d.mkdir()) {
                        System.err.println("cannot create directory: " + d.getPath());
                        continue;
                    }
                } else if (!d.isDirectory()) {
                    System.err.println("file already exists but is not directory: " + d.getPath());
                    continue;
                }
                if (log > 1) {
                    System.out.println("process dir: " + f.getPath());
                }
                process(f, d);
            } else {
                processFile(f, d);
            }
        }
    }

    /*
 @ deprecated
 *** @deprecated
 ** ** ** @deprecated
    */
    static final Pattern pat = Pattern.compile("^[\\s*]*@\\s*deprecated.*");

    void processFile(File srcFile, File dstFile) {
        if (log > 2) {
            System.out.println("process '" + srcFile.getPath() + "'");
        }

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile)));
            int n = 0;
            String line = null;
            while (null != (line = r.readLine())) {
                ++n;
                Matcher m = pat.matcher(line);
                if (m.matches()) {
                    if (log > 3) {
                        System.out.println(String.valueOf(n) + ": " + line);
                    }
                }
            }
            r.close();
        }
        catch (Exception e) {
            System.out.flush();
            System.err.println("caught exception: " + e);
        }
    }
}
