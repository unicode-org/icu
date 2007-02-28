/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;
import com.ibm.icu.util.*;

public class ICUFile {
    public ICUFile(File file, Logger logger) throws IOException {
        this.file = file;
        ICUFile.logger = logger;

        if (!file.isFile())
            throw new IOException("not a file");
        if (!isUpdatable())
            throw new IOException("not an updatable ICU4J jar");
        if (isSigned())
            throw new IOException("not a signed jar");

        tzVersion = findEntryTZVersion(file, insertEntry);
    }

    public File getFile() {
        return file;
    }

    public String getFilename() {
        return file.getName();
    }

    public String getPath() {
        String path = file.getPath();
        int pos = path.lastIndexOf(File.separator);
        path = (pos == -1) ? "" : path.substring(0, pos);
        return path;
    }

    public String toString() {
        return file.toString();
    }

    public String getICUVersion() {
        return icuVersion;
    }

    public String getTZVersion() {
        return tzVersion;
    }

    public boolean equals(Object other) {
        return (!(other instanceof ICUFile)) ? false : file
                .equals(((ICUFile) other).file);
    }

    public void updateJar(URL insertURL, File backupDir) throws IOException {
        if (!file.canRead() || !file.canWrite())
            throw new IOException("Missing permissions for " + file);
        File backupFile = null;
        if ((backupFile = createBackupFile(file, backupDir)) == null)
            throw new IOException("Failed to create a backup file.");
        if (!copyFile(file, backupFile))
            throw new IOException("Could not replace the original jar.");
        if (!createUpdatedJar(backupFile, file, insertEntry, insertURL))
            throw new IOException("Could not create an updated jar.");

        tzVersion = findEntryTZVersion(file, insertEntry);
    }

    private static File createBackupFile(File inputFile, File backupBase) {
        String filename = inputFile.getName();
        String suffix = ".jar";
        String prefix = filename.substring(0, filename.length()
                - ".jar".length());

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
            File backupDir = new File(backupBase.getPath() + File.separator
                    + prefix);
            PrintStream ostream = null;

            try {
                backupBase.mkdir();
                backupDir.mkdir();
                backupFile = File.createTempFile(prefix, suffix, backupDir);
                backupDesc = new File(backupDir.toString() + File.separator
                        + prefix + ".txt");
                backupDesc.createNewFile();
                ostream = new PrintStream(new FileOutputStream(backupDesc));
                ostream.println(inputFile.toString());
            } catch (IOException ex) {
                // ex.printStackTrace();
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

    private static boolean copyFile(File inputFile, File outputFile) {
        logger.println("Coping from " + inputFile + " to " + outputFile + ".",
                Logger.VERBOSE);
        logger.logln("Coping from " + inputFile + " to " + outputFile + ".");
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
            logger.println("Copy successful.", Logger.VERBOSE);
            logger.logln("Copy successful.");
        } catch (IOException ex) {
            outputFile.delete();
            logger.println("Copy failed.", Logger.VERBOSE);
            logger.logln("Copy failed.");
        } finally {
            // safely close the streams
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
        }
        return success;
    }

    private static boolean copyEntry(File inputFile, JarEntry inputEntry,
            File outputFile) {
        logger.println("Coping from " + inputFile + "!/" + inputEntry + " to "
                + outputFile + ".", Logger.VERBOSE);
        logger.logln("Coping from " + inputFile + "!/" + inputEntry + " to "
                + outputFile + ".");
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
            logger.println("Copy successful.", Logger.VERBOSE);
            logger.logln("Copy successful.");
        } catch (IOException ex) {
            // ex.printStackTrace();
            outputFile.delete();
            logger.println("Copy failed.", Logger.VERBOSE);
            logger.logln("Copy failed.");
        } finally {
            // safely close the streams
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
        }
        return success;
    }

    private static boolean createUpdatedJar(File inputFile, File outputFile,
            JarEntry insertEntry, URL inputURL) {
        logger.println("Inserting " + inputURL + " into " + inputFile + "/"
                + insertEntry + ".", Logger.VERBOSE);
        logger.logln("Inserting " + inputURL + " into " + inputFile + "/"
                + insertEntry + ".");
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
            logger.println("Insert successful.", Logger.VERBOSE);
            logger.logln("Insert successful.");
        } catch (IOException ex) {
            // ex.printStackTrace();
            outputFile.delete();
            logger.println("Insert failed.", Logger.VERBOSE);
            logger.logln("Insert failed.");
        } finally {
            // safely close the streams
            if (istream != null)
                try {
                    istream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (ostream != null)
                try {
                    ostream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (jstream != null)
                try {
                    jstream.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
        }
        return success;
    }

    private boolean isUpdatable() {
        JarFile jar = null;
        boolean success = false;

        try {
            jar = new JarFile(file);
            Manifest manifest = jar.getManifest();
            if (manifest == null)
                return hasFile(jar);
            Iterator iter = manifest.getEntries().values().iterator();
            while (iter.hasNext()) {
                Attributes attr = (Attributes) iter.next();
                icuTitle = attr.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                icuVersion = attr
                        .getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                if (!("ICU for Java".equals(icuTitle) || "Modularized ICU for Java"
                        .equals(icuTitle)))
                    continue;

                // since it's an ICU file, we will search inside for the
                // intended file
                success = hasFile(jar);
                break;
            }
        } catch (IOException ex) {
            // unable to create the JarFile,
            // unable to get the Manifest
            // log the unexplained i/o error, but we must drudge on
            logger.println("I/O Error with " + file.getPath(), Logger.VERBOSE);
            logger.logln("I/O Error with " + file.getPath());
        } catch (Exception ex) {
            // ex.printStackTrace();
        } finally {
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                    // ex.printStackTrace();
                }
        }
        return success;
    }

    private boolean hasFile(JarFile jar) {
        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            insertEntry = (JarEntry) e.nextElement();
            if (insertEntry.getName().endsWith(UPDATE_FILENAME))
                return true;
        }
        return false;
    }

    private boolean isSigned() {
        return insertEntry.getCertificates() != null;
    }

    public static String findEntryTZVersion(File icuFile, JarEntry tzEntry) {
        try {
            File temp = File.createTempFile("zoneinfo", ".res");
            temp.deleteOnExit();
            copyEntry(icuFile, tzEntry, temp);
            return findTZVersion(temp);
        } catch (IOException ex) {
            logger.logln(ex.getMessage());
            return null;
        }
    }

    public static String findFileTZVersion(File tzFile) {
        try {
            File temp = File.createTempFile("zoneinfo", ".res");
            temp.deleteOnExit();
            copyFile(tzFile, temp);
            return findTZVersion(temp);
        } catch (IOException ex) {
            logger.logln(ex.getMessage());
            return null;
        }
    }

    /*
     * public static String findURLTZVersion(File tzFile) { try { File temp =
     * File.createTempFile("zoneinfo", ".res"); temp.deleteOnExit();
     * copyFile(tzFile, temp); return findTZVersion(temp); } catch (IOException
     * ex) { ex.printStackTrace(); return null; } }
     */

    private static String findTZVersion(File tzFile) {
        try {
            String filename = tzFile.getName();
            String entryname = filename.substring(0, filename.length()
                    - ".res".length());

            URL url = new URL(tzFile.getParentFile().toURL().toString());
            ClassLoader loader = new URLClassLoader(new URL[] { url });

            UResourceBundle bundle = UResourceBundle.getBundleInstance("",
                    entryname, loader);

            String tzVersion;
            if (bundle != null
                    && (tzVersion = bundle.getString(TZ_VERSION_KEY)) != null)
                return tzVersion;
        } catch (MissingResourceException ex) {
            // not an error -- some zoneinfo files do not have a version number
        } catch (MalformedURLException ex) {
            // this should never happen
            ex.printStackTrace();
        }

        return UNKNOWN_VERSION;
    }

    public static final String UPDATE_FILENAME = "zoneinfo.res";

    public static final int BUFFER_SIZE = 1024;

    public static final String UNKNOWN_VERSION = "Unknown";

    public static final String TZ_VERSION_KEY = "TZVersion";

    private File file;

    private JarEntry insertEntry;

    private String icuTitle;

    private String icuVersion;

    private String tzVersion;

    private static Logger logger;
}
