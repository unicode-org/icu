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
    private boolean inPlace;

    private PrintWriter pw = new PrintWriter(System.out);

    private static FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new File(dir, name).isDirectory() && !"CVS".equals(name)) ||
                    (!name.equals("SwatDeprecated.java") && name.endsWith(".java"));
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
                else if (arg.equals("-copydebug")) { // output copyright debugging
                    vrb = 4;
                }
                else if (arg.equals("-debug")) { // output all debugging
                    vrb = 5;
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

        this.inPlace = srcPrefix.equals(dstPrefix);
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
        if (!srcFile.exists()) {
            throw new RuntimeException("file " + srcFile.getPath() + " does not exist.");
        }
        doList(srcFile);
        if (verbosity >= 1) {
            pw.println("changed " + cc + " file(s)");
            pw.flush();
        }
    }

    public void doList(File file) {
        String[] filenames = file.list(ff);
        for (int i = 0; i < filenames.length; ++i) {
            File f = new File(file, filenames[i]);
            if (f.isDirectory()) {
                doList(f);
            } else {
                processFile(f);
            }
        }
    }

    /*
      if infile != outfile
      - if not overwrite and outfile exists, exit with error
      - if outfile.tmp exists, delete it
      --  exit with error if fail to delete
      - create new outfile.tmp
      - if don't need it, 
      --  delete outfile.tmp
      --  exit with nothing done
      - if outfile.old exists, delete it
      --  if fail to delete
      ---   delete outfile.tmp
      ---   exit with error (extra error if outfile doesn't exist)
<     - if outfile exists, 
<     --  rename it to outfile.old
      - rename outfile.tmp to outfile
      - return success

      if infile == outfile
      - if not overwrite (we know outfile exists) exit with error
      - if outfile.tmp exists, delete it
      --  exit with error if fail to delete
>     - get infile out of the way for creation
>     --  if infile.bak exists, delete it
>     ---   exit with error if fail to delete
>     --  rename infile to infile.bak
      - create new outfile.tmp
      - if don't need it,
      --  delete outfile.tmp
>     --  rename infile.bak to infile
      --  exit with nothing done
      - if outfile.old exists, delete it
      --  if fail to delete
      ---   delete outfile.tmp
>     ---   rename infile.bak to infile
      ---   exit with error
>     - rename infile.bak to outfile.old
      - rename outfile.tmp to outfile
      - exit with success

     */
    public void processFile(File inFile) {
        File bakFile = null;
        File oldFile = null;
        try {
            String inPath = inFile.getCanonicalPath();
            if (verbosity >= 5) {
                pw.println("processFile: " + inPath);
            }

            String outPath = dstPrefix + inPath.substring(srcPrefix.length());
            File outFile = new File(outPath);
            if (!overwrite && outFile.exists()) {
                throw new RuntimeException("no permission to overwrite file: " + outPath);
            }
            if (verbosity >= 5) {
                pw.println("outFile: " + outPath);
            }

            String tmpPath = outPath + ".tmp";
            File tmpFile = new File(tmpPath);
            if (tmpFile.exists()) {
                if (!tmpFile.delete()) {
                    tmpFile = null;
                    throw new RuntimeException("could not delete existing temporary file: " + tmpPath);
                }
            }
            if (verbosity >= 5) {
                pw.println("tmpFile: " + tmpPath);
            }

            FileReader fr = null;
            String bakPath = inPath + ".bak";
            if (inPlace) {
                bakFile = new File(bakPath);
                if (bakFile.exists()) {
                    if (!bakFile.delete()) {
                        bakFile = null;
                        throw new RuntimeException("could not delete existing backup file: " + bakPath);
                    }
                }
                if (!inFile.renameTo(bakFile)) {
                    bakFile = null;
                    throw new InternalError("can't rename: " + inPath + " to: " + bakPath);
                }
                if (verbosity >= 5) {
                    pw.println("bakFile: " + bakPath);
                    pw.println("inFile.exists(): " + inFile.exists());
                    pw.println("bakFile.exists(): " + bakFile.exists());
                }
                fr = new FileReader(bakFile);
            } else {
                fr = new FileReader(inFile);
            }

            // ensure parent directory tree exists
            tmpFile.getParentFile().mkdirs();

            // operate on backup and temporary files
            FileWriter fw = new FileWriter(tmpFile);
            BufferedWriter bw = new BufferedWriter(fw);

            BufferedReader br = new BufferedReader(fr);
            String line;
            int n = 0;
            int tc = 0;
            boolean debug = false;
            while (null != (line = br.readLine())) {
                // int temp = line.indexOf("@deprecated");
                int ix = line.indexOf(srcTag);
//                 if (temp != -1 && ix == -1) {
//                     if (debug == false) {
//                         debug = true;
//                         pw.println("file: " + name);
//                     }
//                     pw.println("[" + n + "] " + line);
//                     pw.flush();
//                 }
                if (ix != -1) {
//                     pw.println("[" + n + "] " + line); // debug

                    line = line.substring(0,ix) + trgTag;
                    
                    ++tc;
                } else if (n < 20) {
                    // swat copyrights while we're at it
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
                            ix = line.indexOf("- 200");
                            if (ix != -1) {
                                nline = line.substring(0, ix) + "-2006" + line.substring(ix+6);
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
                    pw.println("no changes in file: " + inPath);
                }
                if (!tmpFile.delete()) {
                    throw new RuntimeException("unable to delete unneeded temporary file: " + tmpPath);
                }
                if (bakFile != null ) {
                    if (verbosity >= 5) {
                        pw.println("trying to rename: " + bakFile.getPath() + " to: " + inFile.getPath());
                        pw.println("bakFile.exists(): " + bakFile.exists());
                        pw.println("inFile.exists(): " + inFile.exists());
                    }

                    if (!bakFile.renameTo(inFile)) {
                        throw new RuntimeException("could not restore backup file: " + bakPath);
                    }
                }
                return;
            }

            String oldPath = outPath + ".old";
            oldFile = new File(oldPath);
            if (oldFile.exists()) {
                if (!oldFile.delete()) {
                    tmpFile.delete(); // ignore deletion error
                    if (bakFile != null) {
                        if (!bakFile.renameTo(inFile)) {
                            throw new RuntimeException("could not restore backup file: " + bakPath);
                        }
                    }
                    throw new RuntimeException("cannot remove existing old file: " + oldPath);
                }
            }

            if (inPlace) {
                if (!bakFile.renameTo(oldFile)) {
                    throw new RuntimeException("cannot create old file from backup: " + bakPath);
                }
            } else if (outFile.exists()) {
                if (!outFile.renameTo(oldFile)) {
                    // todo: error recovery
                    throw new RuntimeException("cannot create old file from original: " + outPath);
                }
            }

            if (!tmpFile.renameTo(outFile)) {
                throw new RuntimeException("could not rename temporary file to output file: " + outPath);
            }

            outFile.setLastModified(inFile.lastModified());

            if (verbosity >= 2) {
                pw.println(inPath);
                pw.flush();
            }
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }
        finally {
            pw.flush();
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
