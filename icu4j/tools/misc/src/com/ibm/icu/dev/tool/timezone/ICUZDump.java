/*
 ***********************************************************************
 * Copyright (C) 2007, International Business Machines                 *
 * Corporation and others. All Rights Reserved.                        *
 ***********************************************************************
 *
 */

package com.ibm.icu.dev.tool.timezone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.ULocale;

/**
 * TimeZone transition dump tool.
 */
public class ICUZDump {
    private static final String DEFAULT_LINE_SEP;

    static {
        String sep = System.getProperty("line.separator");
        DEFAULT_LINE_SEP = (sep == null) ? "\n" : sep;
    }

    private TimeZoneImpl tz = null;

    private int loyear = 1900;
    private int hiyear = 2050;
    private long tick = 1000;
    private DumpFormatter formatter;
    private String linesep = DEFAULT_LINE_SEP;

    public ICUZDump() {
    }
    
    public void setLowYear(int loyear) {
        this.loyear = loyear;
    }
    
    public void setHighYear(int hiyear) {
        this.hiyear = hiyear;
    }

    public void setTick(int tick) {
        if (tick <= 0) {
            throw new IllegalArgumentException("tick must be positive");
        }
        this.tick = tick;
    }
    
    public void setTimeZone(Object tzimpl) {
        this.tz = new TimeZoneImpl(tzimpl);
    }

    public void setDumpFormatter(DumpFormatter formatter) {
        this.formatter = formatter;
    }
    
    public void setLineSeparator(String sep) {
        this.linesep = sep;
    }
    
    public void dump(Writer w) throws IOException {
        if (tz == null) {
            throw new IllegalStateException("timezone is not initialized");
        }

        if (formatter == null) {
            formatter = new DumpFormatter();
        }
        
        final long SEARCH_INCREMENT = 12 * 60 * 60 * 1000; // half day
        long cutovers[] = getCutOverTimes();
        long t = cutovers[0];
        int offset = tz.getOffset(t);
        boolean inDst = tz.inDaylightTime(t);
        while (t < cutovers[1]) {
            long newt = t + SEARCH_INCREMENT;
            int newOffset = tz.getOffset(newt);
            boolean newInDst = tz.inDaylightTime(newt);
            if (offset != newOffset || inDst != newInDst) {
                // find the boundary
                long lot = t;
                long hit = newt;
                while (true) {
                    long diff = hit - lot;
                    if (diff <= tick) {
                        break;
                    }
                    long medt = lot + ((diff / 2) / tick) * tick;
                    int medOffset = tz.getOffset(medt);
                    boolean medInDst = tz.inDaylightTime(medt);
                    if (medOffset != offset || medInDst != inDst) {
                        hit = medt;
                    } else {
                        lot = medt;
                    }
                }
                w.write(formatter.format(lot, offset, tz.inDaylightTime(lot)));
                w.write(" > ");
                w.write(formatter.format(hit, newOffset, tz.inDaylightTime(hit)));
                w.write(linesep);
                offset = newOffset;
                inDst = newInDst;
            }
            t = newt;
        }

    }
    
    private long[] getCutOverTimes() {
        long[] cutovers = new long[2];
        cutovers[0] = tz.getTime(loyear, 0, 1, 0, 0, 0);
        cutovers[1] = tz.getTime(hiyear, 0, 1, 0, 0, 0);
        return cutovers;
    }

    private class TimeZoneImpl {
        private Object tzobj;
        
        public TimeZoneImpl(Object tzobj) {
            this.tzobj = tzobj;
        }

        public int getOffset(long time) {
            try {
                Method method = tzobj.getClass().getMethod("getOffset", new Class[] {long.class});
                Object result = method.invoke(tzobj, new Object[] {new Long(time)});
                return ((Integer)result).intValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        public boolean inDaylightTime(long time) {
            try {
                Method method = tzobj.getClass().getMethod("inDaylightTime", new Class[] {Date.class});
                Object result = method.invoke(tzobj, new Object[] {new Date(time)});
                return ((Boolean)result).booleanValue();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public long getTime(int year, int month, int dayOfMonth, int hour, int minute, int second) {
            long time;
            if (tzobj instanceof com.ibm.icu.util.TimeZone) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeZone((com.ibm.icu.util.TimeZone)tzobj);
                cal.clear();
                cal.set(year, month, dayOfMonth, hour, minute, second);
                time = cal.getTimeInMillis();
            } else if (tzobj instanceof java.util.TimeZone) {
                java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
                cal.setTimeZone((java.util.TimeZone)tzobj);
                cal.clear();
                cal.set(year, month, dayOfMonth, hour, minute, second);
                time = cal.getTimeInMillis();
            } else {
                throw new IllegalStateException("Unsupported TimeZone implementation");
            }
            return time;
        }
    }

    public class DumpFormatter {
        private SimpleTimeZone stz = new SimpleTimeZone(0, "");
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd EEE HH:mm:ss", ULocale.US);
        private DecimalFormat decf;

        public DumpFormatter() {
            DecimalFormatSymbols decfs = new DecimalFormatSymbols(ULocale.US);
            decf = new DecimalFormat("00", decfs);
        }

        public String format(long time, int offset, boolean isDst) {
            StringBuffer buf = new StringBuffer();
            stz.setRawOffset(offset);
            sdf.setTimeZone(stz);
            buf.append(sdf.format(new Date(time)));
            if (offset < 0) {
                buf.append("-");
                offset = -offset;
            } else {
                buf.append("+");
            }

            int hour, min, sec;

            offset /= 1000;
            sec = offset % 60;
            offset = (offset - sec) / 60;
            min = offset % 60;
            hour = offset / 60;

            buf.append(decf.format(hour));
            buf.append(decf.format(min));
            buf.append(decf.format(sec));

            buf.append("[DST=");
            buf.append(isDst ? "1" : "0");
            buf.append("]");
            return buf.toString();
        }
    }

    /*
     * Usage:
     * 
     * java -cp icu4j.jar com.ibm.icu.dev.tool.timezone [-j] [-a] [-c[<low_year>,]<high_year>] [-d<dir>] [-l<sep>] [<zone_name> [<zone_name>]]
     * 
     * Options:
     *      -j      : Use JDK TimeZone.  By default, ICU TimeZone is used.
     *      -a      : Dump all available zones.
     *      -c[<low_year>,]<high_year>
     *              : When specified, dump transitions starting <low_year> (inclusive) up to
     *                <high_year> (exclusive).  The default values are 1902(low) and 2038(high).
     *      -d<dir> : When specified, write transitions in a file under the directory for each zone.
     *      -l<sep> : New line code type CR/LF/CRLF.
     */
    public static void main(String[] args) {
        boolean jdk = false;
        int low = 1902;
        int high = 2038;
        List idlist = new ArrayList();
        boolean all = false;
        String dir = null;
        String newLineMode = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-j")) {
                jdk = true;
            } else if (args[i].startsWith("-c")) {
                String val = args[i].substring(2);
                String[] years = val.split(",");
                if (years.length == 1) {
                    high = Integer.parseInt(years[0]);
                } else if (years.length == 2) {
                    low = Integer.parseInt(years[0]);
                    high = Integer.parseInt(years[1]);
                }
            } else if (args[i].equals("-a")) {
                all = true;
            } else if (args[i].startsWith("-d")) {
                dir = args[i].substring(2);
            } else if (args[i].startsWith("-l")) {
                newLineMode = args[i].substring(2);
            } else if (!args[i].startsWith("-")){
                idlist.add(args[i].trim());
            }
        }

        String lineSep = System.getProperty("line.separator");
        if (newLineMode != null && newLineMode.length() > 0) {
            if (newLineMode.equalsIgnoreCase("CR")) {
                lineSep = "\r";
            } else if (newLineMode.equalsIgnoreCase("LF")) {
                lineSep = "\n";
            } else if (newLineMode.equalsIgnoreCase("CRLF")) {
                lineSep = "\r\n";            
            }
        }
        
        String[] tzids = null;

        if (all) {
            if (jdk) {
                tzids = java.util.TimeZone.getAvailableIDs();
            } else {
                tzids = com.ibm.icu.util.TimeZone.getAvailableIDs();
            }

            // sort tzids
            TreeSet set = new TreeSet();
            for (int i = 0; i < tzids.length; i++) {
                set.add(tzids[i]);
            }
            Iterator it = set.iterator();
            int i = 0;
            while (it.hasNext()) {
                tzids[i++] = (String)it.next();
            }
        } else {
            int len = idlist.size();
            if (len == 0) {
                tzids = new String[1];
                tzids[0] = java.util.TimeZone.getDefault().getID();
            } else {
                tzids = new String[idlist.size()];
                idlist.toArray(tzids);
            }            
        }

        File dirfile = null;
        if (dir == null || dir.length() == 0) {
            PrintWriter pw = new PrintWriter(System.out);
            try {
                for (int i = 0; i < tzids.length; i++) {
                    if (i != 0) {
                        pw.println();
                    }
                    pw.write("ZONE: ");
                    pw.write(tzids[i]);
                    pw.println();
                    dumpZone(pw, lineSep, tzids[i], low, high, jdk);
                }
                pw.flush();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            dirfile = new File(dir);
            dirfile.mkdirs();

            try {
                for (int i = 0; i < tzids.length; i++) {
                    FileOutputStream fos = new FileOutputStream(new File(dirfile, tzids[i].replace('/', '-')));
                    Writer w = new BufferedWriter(new OutputStreamWriter(fos));
                    dumpZone(w, lineSep, tzids[i], low, high, jdk);
                    w.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static void dumpZone(Writer w, String lineSep, String tzid, int low, int high, boolean isJdk) throws IOException {
        ICUZDump dumper = new ICUZDump();
        Object tzimpl;
        if (isJdk) {
            tzimpl = java.util.TimeZone.getTimeZone(tzid);
        } else {
            tzimpl = com.ibm.icu.util.TimeZone.getTimeZone(tzid);
        }
        dumper.setTimeZone(tzimpl);
        dumper.setLowYear(low);
        dumper.setHighYear(high);
        dumper.setLineSeparator(lineSep);
        dumper.dump(w);
    }
}
