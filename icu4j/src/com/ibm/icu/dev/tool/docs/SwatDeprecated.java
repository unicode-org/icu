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
    private PrintWriter pw = new PrintWriter(System.out);
    private int maxLength = 85;
    private String srcPrefix;
    private String dstPrefix;
    private String srcTag;
    private String trgTag;

    private static FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (new File(dir, name).isDirectory() && !"CVS".equals(name)) ||
                    name.endsWith(".java");
            }
        };

    public static void main(String[] args) {
        String src = System.getProperty("user.dir");
        String dst = src;
        boolean dep = false;

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
                } else if (arg.equals("-prov")) {
                    dep = false;
                }
            }
        }

        new SwatDeprecated(src, dst, dep).run();
    }

    public SwatDeprecated(String src, String dst, boolean dep) {
        this.srcFile = new File(src);
        this.dstFile = new File(dst);
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
        pw.println("src: '" + srcPrefix + "'");
        pw.println("dst: '" + dstPrefix + "'");
        pw.println("name: '" + srcFile.getName() + "'");
        pw.println("isDir: " + srcFile.isDirectory());
    }

    public void run() {
        doList(srcFile);
        pw.println();
        pw.flush();
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

    public void ensureDirectories(File file) {
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
            while (null != (line = br.readLine())) {
                int ix = line.indexOf(srcTag);
                if (ix != -1) {
                    line = line.substring(0,ix) + trgTag;
                } else if (n < 20) {
                    ix = line.indexOf("opyright");
                    if (ix != -1) {
                        // pw.println("[" + n + "] " + line);
                        ix = line.indexOf("-200");
                        if (ix != -1) {
                            line = line.substring(0, ix) + "-2006" + line.substring(ix+5);
                            // pw.println("  --> " + line);
                        } else {
                            ix = line.indexOf("200");
                            if (ix != -1) {
                                line = line.substring(0, ix) + "2006" + line.substring(ix+4);
                                // pw.println("  --> " + line);
                            }
                        }
                    }
                }
                bw.write(line);
                bw.newLine();
                ++n;
            }
            bw.flush();
            bw.close();
            outFile.setLastModified(inFile.lastModified());
            if (!outFile.renameTo(new File(name))) {
                 throw new RuntimeException("could not rename file to " + name);
            }
            pw.println(name);
            pw.flush();
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }
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
