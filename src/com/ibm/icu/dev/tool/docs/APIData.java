/**
*******************************************************************************
* Copyright (C) 2004-2005, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/**
 * Represent a file of APIInfo records.
 */

package com.ibm.icu.dev.tool.docs;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class APIData {
    int version;
    String name;
    String base;
    TreeSet set;

    static APIData read(BufferedReader br) {
        try {
            APIData data = new APIData();

            data.version = Integer.parseInt(APIInfo.readToken(br)); // version
            data.name = APIInfo.readToken(br);
            data.base = APIInfo.readToken(br); // base
            br.readLine();

            data.set = new TreeSet(APIInfo.defaultComparator());
            for (APIInfo info = new APIInfo(); info.read(br); info = new APIInfo()) {
                data.set.add(info);
            }
            return data;
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException("error reading api data");
            re.initCause(e);
            throw re;
        }
    }

    static APIData read(File file) {
        String fileName = file.getName();
        try {
            InputStream is;
            if (fileName.endsWith(".zip")) {
                ZipFile zf = new ZipFile(file);
                Enumeration entryEnum = zf.entries();
                if (entryEnum.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry)entryEnum.nextElement();
                    is = zf.getInputStream(entry);
                    // we only handle one!!!
                } else {
                    throw new IOException("zip file is empty");
                }
            } else {
                is = new FileInputStream(file);
                if (fileName.endsWith(".gz")) {
                    is = new GZIPInputStream(is);
                }
            }
            InputStreamReader isr = new InputStreamReader(is);
            return read(new BufferedReader(isr));
        }
        catch (IOException e) {
            RuntimeException re = new RuntimeException("error getting info stream: " + fileName);
            re.initCause(e);
            throw re;
        }
    }

    static APIData read(String fileName) {
        return read(new File(fileName));
    }

    private static final String[] stanames = { "draft", "stable", "deprecated", "obsolete" };
    private static final String[] catnames = { "classes", "fields", "constructors", "methods" };

    public void printStats(PrintWriter pw) {
        // classes, methods, fields
        // draft, stable, other

        int[] stats = new int[16];

        Iterator iter = set.iterator();
        while (iter.hasNext()) {
            APIInfo info = (APIInfo)iter.next();

            if (info.isPublic() || info.isProtected()) {
                int sta = info.getVal(APIInfo.STA);
                int cat = info.getVal(APIInfo.CAT);
                stats[cat * 4 + sta] += 1;
            }
        }

        int tt = 0;
        for (int cat = 0; cat < 4; ++cat) {
            pw.println(catnames[cat]);
            int t = 0;
            for (int sta = 0; sta < 4; ++sta) {
                int v = stats[cat * 4 + sta];
                t += v;
                pw.println("   " + stanames[sta] + ": " + v);
            }
            tt += t;
            pw.println("total: " + t);
            pw.println();
        }
        pw.println("total apis: " + tt);
    }

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);

        String path = "src/com/ibm/icu/dev/tool/docs/";
        String fn = "icu4j341.api.gz";
        if (args.length == 0) {
            args = new String[] { "-file", fn };
        }
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (arg.equals("-path")) {
                path = args[++i];
            } else if (arg.equals("-file")) {
                fn = args[++i];

                File f = new File(path, fn);
                read(f).printStats(pw);
                pw.flush();
            }
        }
    }
}
