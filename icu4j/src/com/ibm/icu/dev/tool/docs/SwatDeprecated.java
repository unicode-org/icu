/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.docs;

import java.io.*;

public class SwatDeprecated {
    private File srcFile;
    private File dstFile;
    private int maxLength = 85;
    private String srcPrefix;
    private String dstPrefix;
    private String srcTag;
    private String trgTag;
    private boolean overwrite;
    private int verbosity;
    private int cc; // changed file count

    private PrintWriter pw = new PrintWriter(System.out);

    private static FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new File(dir, name).isDirectory() && !"CVS".equals(name)) ||
                    name.endsWith(".java");
            }
        };

    public static void main(String[] args) {
        String src = System.getProperty("user.dir");
        String dst = src;
        boolean dep = true;
        boolean ovr = false;
        int vrb = 1;

        for (int i = 0; i < args.length; ++i) {
            String arg = args[i].toLowerCase();
            if (arg.charAt(0) == '-') {
                if (arg.equals("-src")) {
                    src = args[++i];
                }
                else if (arg.equals("-dst")) {
                    dst = args[++i];
                }
                else if (arg.equals("-dep")) {
                    dep = true;
                } 
                else if (arg.equals("-prov")) {
                    dep = false;
                }
                else if (arg.equals("-overwrite")) {
                    ovr = true;
                }
                else if (arg.equals("-silent")) { // no output
                    vrb = 0;
                }
                else if (arg.equals("-quiet")) { // output parameters and count of changed files (default)
                    vrb = 1;
                } 
                else if (arg.equals("-verbose")) { // output names of modified files
                    vrb = 2;
                } 
                else if (arg.equals("-noisy")) { // output names of files not modified
                    vrb = 3;
                } 
                else if (arg.equals("-debug")) { // output copyright debugging
                    vrb = 4;
                }
            }
        }

        new SwatDeprecated(src, dst, dep, ovr, vrb).run();
    }

    public SwatDeprecated(String src, String dst, boolean dep, boolean overwrite, int verbosity) {
        this.srcFile = new File(src);
        this.dstFile = new File(dst);
        this.overwrite = overwrite;
        this.verbosity = verbosity;

        this.srcTag = "@deprecated This is a draft API and might change in a future release of ICU.";
        this.trgTag = "@provisional";
        if (!dep) {
            String temp = srcTag;
            srcTag = trgTag;
            trgTag = temp;
        }
        try {
            this.srcPrefix = srcFile.getCanonicalPath();
            this.dstPrefix = dstFile.getCanonicalPath();
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }

        this.cc = 0;

        if (verbosity >= 1) {
            pw.println("replacing '" + srcTag + "'");
            pw.println("     with '" + trgTag + "'");
            pw.println();
            pw.println("     source: '" + srcPrefix + "'");
            pw.println("destination: '" + dstPrefix + "'");
            pw.println("  overwrite: " + overwrite);
            pw.println("  verbosity: " + verbosity);
            pw.flush();
        }
    }

    public void run() {
        doList(srcFile);
        if (verbosity >= 1) {
            pw.println("changed " + cc + " file(s)");
            pw.flush();
        }
    }

    public void doList(File file) {
        File[] files = file.listFiles(ff);
        for (int i = 0; i < files.length; ++i) {
            File f = files[i];
            if (f.isDirectory()) {
                doList(f);
            } else {
                processFile(f);
            }
        }
    }

    public void processFile(File inFile) {
        try {
            String name = dstPrefix + inFile.getCanonicalPath().substring(srcPrefix.length());
            File outFile = new File(name + ".tmp");
            outFile.getParentFile().mkdirs();

            FileWriter fw = new FileWriter(outFile);
            BufferedWriter bw = new BufferedWriter(fw);

            FileReader fr = new FileReader(inFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            int n = 0;
            int tc = 0;
            boolean debug = false;
            while (null != (line = br.readLine())) {
                int temp = line.indexOf("@deprecated");
                int ix = line.indexOf(srcTag);
                if (temp != -1 && ix == -1) {
                    if (debug == false) {
                        debug = true;
                        pw.println("file: " + name);
                    }
                    pw.println("[" + n + "] " + line);
                    pw.flush();
                }
                if (ix != -1) {
                    line = line.substring(0,ix) + trgTag;
                    ++tc;
                } else if (n < 20) {
                    ix = line.indexOf("opyright");
                    if (ix != -1) {
                        String nline = null;
                        do {
                            if (verbosity == 4) {
                                pw.println("[" + n + "] " + line);
                            }
                            ix = line.indexOf("-200");
                            if (ix != -1) {
                                nline = line.substring(0, ix) + "-2006" + line.substring(ix+5);
                                break;
                            }
                            ix = line.indexOf("-199");
                            if (ix != -1) {
                                nline = line.substring(0, ix) + "-2006" + line.substring(ix+5);
                                break;
                            }
                            ix = line.indexOf("200");
                            if (ix != -1) {
                                nline = line.substring(0, ix) + "2006" + line.substring(ix+4);
                                break;
                            }
                            ix = line.indexOf("199");
                            if (ix != -1) {
                                nline = line.substring(0, ix) + "2006" + line.substring(ix+4);
                                break;
                            }
                        } while (false);

                        if (nline != null) {
                            if (verbosity >= 4) {
                                pw.println("  --> " + nline);
                            }
                            line = nline;
                        }
                    }
                }
                bw.write(line);
                bw.newLine();
                ++n;
            }
            bw.flush();
            bw.close();

            if (tc == 0) { // nothing changed, forget this file
                if (verbosity >= 3) {
                    pw.println("no changes in file: " + name);
                }
                if (!outFile.delete()) {
                    throw new RuntimeException("unable to delete unneeded temporary file: " + name + ".tmp");
                }
                return;
            }

            outFile.setLastModified(inFile.lastModified());
            File newFile = new File(name);
            if (newFile.exists()) {
                if (!overwrite) {
                    throw new RuntimeException("file " + name + " already exists");
                } else {
                    if (!newFile.delete()) {
                        throw new RuntimeException("could not delete existing file: " + name);
                    }
                }
            }
            if (!outFile.renameTo(newFile)) {
                 throw new RuntimeException("could not rename file to " + name);
            }
            if (verbosity >= 2) {
                pw.println(name);
                pw.flush();
            }
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }

        ++cc;
    }

    public void dumpList(String[] names) {
        if (names == null) {
            pw.print("null");
        } else {
            pw.print("{");
            int lc = 0;
            if (names.length > 0) {
                pw.println();
                pw.print("    ");
                lc = 4;
            }
            for (int i = 0; i < names.length; ++i) {
                String name = names[i];
                int nl = name.length();
                if (lc + nl > maxLength) {
                    pw.println();
                    pw.print("    ");
                    lc = 4;
                }
                pw.print(name);
                pw.print(", ");
                lc += nl + 2;
            }
            if (names.length > 0) {
                pw.println();
            }
            pw.print("} ");
        }
    }
}
