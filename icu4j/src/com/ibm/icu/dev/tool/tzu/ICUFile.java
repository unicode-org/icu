/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.ibm.icu.util.UResourceBundle;

public class ICUFile {
    private ICUFile(Logger logger) {
        this.logger = logger;
    }

    public ICUFile(File file, Logger logger) throws IOException {
        this.icuFile = file;
        this.logger = logger;

        if (!file.isFile())
            throw new IOException("not a file");
        if (!isUpdatable())
            throw new IOException("not an updatable ICU4J jar");
        if (isSigned())
            throw new IOException("not a signed jar");

        tzVersion = findEntryTZVersion(file, tzEntry);
    }

    public File getFile() {
        return icuFile;
    }

    public String getFilename() {
        return icuFile.getName();
    }

    public String getPath() {
        String path = icuFile.getPath();
        int pos = path.lastIndexOf(File.separator);
        path = (pos == -1) ? "" : path.substring(0, pos);
        return path;
    }

    public String toString() {
        return icuFile.toString();
    }

    public String getICUVersion() {
        return icuVersion;
    }

    public String getTZVersion() {
        return tzVersion;
    }

    public boolean equals(Object other) {
        return (!(other instanceof ICUFile)) ? false : icuFile.getPath().equalsIgnoreCase(
                ((ICUFile) other).icuFile.getPath());
    }

    public void updateJar(URL insertURL, File backupDir) throws IOException {
        if (!icuFile.canRead() || !icuFile.canWrite())
            throw new IOException("Missing permissions for " + icuFile);
        File backupFile = null;
        if ((backupFile = createBackupFile(icuFile, backupDir)) == null)
            throw new IOException("Failed to create a backup file.");
        if (!copyFile(icuFile, backupFile))
            throw new IOException("Could not replace the original jar.");
        if (!createUpdatedJar(backupFile, icuFile, tzEntry, insertURL))
            throw new IOException("Could not create an updated jar.");

        tzVersion = findEntryTZVersion(icuFile, tzEntry);
    }

    private File createBackupFile(File inputFile, File backupBase) {
        logger.logln("Creating backup file for + " + inputFile + " at " + backupBase + ".", Logger.VERBOSE);
        String filename = inputFile.getName();
        String suffix = ".jar";
        String prefix = filename.substring(0, filename.length() - ".jar".length());

        if (backupBase == null) {
            try {
                File backupFile = File.createTempFile(prefix, suffix);
                backupFile.deleteOnExit();
                return backupFile;
            } catch (IOException ex) {
                return null;
            }
        } else {
            File backupFile = null;
            File backupDesc = null;
            File backupDir = new File(backupBase.getPath() + File.separator + prefix);
            PrintStream ostream = null;

            try {
                backupBase.mkdir();
                backupDir.mkdir();
                backupFile = File.createTempFile(prefix, suffix, backupDir);
                backupDesc = new File(backupDir.toString() + File.separator + prefix + ".txt");
                backupDesc.createNewFile();
                ostream = new PrintStream(new FileOutputStream(backupDesc));
                ostream.println(inputFile.toString());
                logger.logln("Successfully created backup file at " + backupFile + ".", Logger.VERBOSE);
            } catch (IOException ex) {
                logger.logln("Failed to create backup file.", Logger.VERBOSE);
                backupFile.delete();
                backupDesc.delete();
                backupDir.delete();
                backupFile = null;
            } finally {
                ostream.close();
            }
            return backupFile;
        }
    }

    private boolean copyFile(File inputFile, File outputFile) {
        logger.logln("Copying from " + inputFile + " to " + outputFile + ".", Logger.VERBOSE);
        InputStream istream = null;
        OutputStream ostream = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        boolean success = false;

        try {
            istream = new FileInputStream(inputFile);
            ostream = new FileOutputStream(outputFile);

            while ((bytesRead = istream.read(buffer)) != -1)
                ostream.write(buffer, 0, bytesRead);

            success = true;
            logger.logln("Copy successful.", Logger.VERBOSE);
        } catch (IOException ex) {
            outputFile.delete();
            logger.logln("Copy failed.", Logger.VERBOSE);
        } finally {
            // safely close the streams
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                }
        }
        return success;
    }

    private boolean copyEntry(File inputFile, JarEntry inputEntry, File outputFile) {
        logger.logln("Copying from " + inputFile + "!/" + inputEntry + " to " + outputFile + ".", Logger.VERBOSE);
        JarFile jar = null;
        InputStream istream = null;
        OutputStream ostream = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        boolean success = false;

        try {
            jar = new JarFile(inputFile);
            istream = jar.getInputStream(inputEntry);
            ostream = new FileOutputStream(outputFile);

            while ((bytesRead = istream.read(buffer)) != -1)
                ostream.write(buffer, 0, bytesRead);

            success = true;
            logger.logln("Copy successful.", Logger.VERBOSE);
        } catch (IOException ex) {
            outputFile.delete();
            logger.logln("Copy failed.", Logger.VERBOSE);
        } finally {
            // safely close the streams
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                }
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                }
        }
        return success;
    }

    private boolean createUpdatedJar(File inputFile, File outputFile, JarEntry insertEntry, URL inputURL) {
        JarFile jar = null;
        JarOutputStream ostream = null;
        InputStream istream = null;
        InputStream jstream = null;
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        boolean success = false;

        try {
            jar = new JarFile(inputFile);
            ostream = new JarOutputStream(new FileOutputStream(outputFile));
            istream = inputURL.openStream();

            Enumeration e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry currentEntry = (JarEntry) e.nextElement();

                if (!currentEntry.getName().equals(insertEntry.getName())) {
                    // if the current entry isn't the one that needs updating
                    // write a copy of the old entry from the old file
                    ostream.putNextEntry(new JarEntry(currentEntry.getName()));

                    jstream = jar.getInputStream(currentEntry);
                    while ((bytesRead = jstream.read(buffer)) != -1)
                        ostream.write(buffer, 0, bytesRead);
                    jstream.close();
                } else {
                    // if the current entry *is* the one that needs updating
                    // write a new entry based on the input stream (from the
                    // URL)
                    // currentEntry.setTime(System.currentTimeMillis());
                    ostream.putNextEntry(new JarEntry(currentEntry.getName()));

                    while ((bytesRead = istream.read(buffer)) != -1)
                        ostream.write(buffer, 0, bytesRead);
                }
            }

            success = true;
            logger.logln("Insert successful.", Logger.VERBOSE);
        } catch (IOException ex) {
            outputFile.delete();
            logger.logln("Insert failed.", Logger.VERBOSE);
        } finally {
            // safely close the streams
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                }
            if (jstream != null)
                try {
                    jstream.close();
                } catch (IOException ex) {
                }
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                }
        }
        return success;
    }

    private boolean isUpdatable() {
        JarFile jar = null;
        boolean success = false;

        try {
            jar = new JarFile(icuFile);
            Manifest manifest = jar.getManifest();
            icuVersion = ICU_VERSION_UNKNOWN;
            if (manifest != null) {
                Iterator iter = manifest.getEntries().values().iterator();
                while (iter.hasNext()) {
                    Attributes attr = (Attributes) iter.next();
                    String ver = attr.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    if (ver != null) {
                        icuVersion = ver;
                        break;
                    }
                }
            }
            success = (jar.getJarEntry(TZ_ENTRY_DIR) != null) && hasFile(jar);
        } catch (IOException ex) {
            // unable to create the JarFile or unable to get the Manifest
            // log the unexplained i/o error, but we must drudge on
            logger.logln("I/O Error with " + icuFile.getPath(), Logger.VERBOSE);
        } finally {
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                }
        }
        return success;
    }

    private boolean hasFile(JarFile jar) {
        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            tzEntry = (JarEntry) e.nextElement();
            if (tzEntry.getName().endsWith(TZ_ENTRY_FILENAME))
                return true;
        }
        return false;
    }

    private boolean isSigned() {
        return tzEntry.getCertificates() != null;
    }

    public String findEntryTZVersion(File icuFile, JarEntry tzEntry) {
        try {
            File temp = File.createTempFile("zoneinfo", ".res");
            temp.deleteOnExit();
            copyEntry(icuFile, tzEntry, temp);
            return findTZVersion(temp);
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
            return null;
        }
    }

    public static String findFileTZVersion(File tzFile, Logger logger) {
        ICUFile rawTZFile = new ICUFile(logger);

        try {
            File temp = File.createTempFile("zoneinfo", ".res");
            temp.deleteOnExit();
            rawTZFile.copyFile(tzFile, temp);
            return rawTZFile.findTZVersion(temp);
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
            return null;
        }
    }

    /*
     * public static String findURLTZVersion(File tzFile) { try { File temp =
     * File.createTempFile("zoneinfo", ".res"); temp.deleteOnExit();
     * copyFile(tzFile, temp); return findTZVersion(temp); } catch (IOException
     * ex) { ex.printStackTrace(); return null; } }
     */

    private String findTZVersion(File tzFile) {
        try {
            String filename = tzFile.getName();
            String entryname = filename.substring(0, filename.length() - ".res".length());

            URL url = new URL(tzFile.getParentFile().toURL().toString());
            ClassLoader loader = new URLClassLoader(new URL[] { url });

            UResourceBundle bundle = UResourceBundle.getBundleInstance("", entryname, loader);

            String tzVersion;
            if (bundle != null && (tzVersion = bundle.getString(TZ_VERSION_KEY)) != null)
                return tzVersion;
        } catch (MissingResourceException ex) {
            // not an error -- some zoneinfo files do not have a version number
        } catch (MalformedURLException ex) {
            // this should never happen
            ex.printStackTrace();
        }

        return TZ_VERSION_UNKNOWN;
    }

    public static final int BUFFER_SIZE = 1024;

    public static final String ICU_VERSION_UNKNOWN = "Unknown";

    public static final String TZ_VERSION_UNKNOWN = "Unknown";

    public static final String TZ_VERSION_KEY = "TZVersion";

    public static final String TZ_ENTRY_DIR = "com/ibm/icu/impl";

    public static final String TZ_ENTRY_FILENAME = "zoneinfo.res";

    private File icuFile;

    private String icuVersion;

    private String tzVersion;

    private JarEntry tzEntry;

    private Logger logger;
}
