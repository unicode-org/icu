/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
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
    /**
     * Constructs a blank ICUFile. Used internally.
     * 
     * @param logger
     *            The current logger.
     */
    private ICUFile(Logger logger) {
        this.logger = logger;
    }

    /**
     * Constructs an ICUFile around a file. See <code>initialize</code> for
     * details.
     * 
     * @param filename
     *            The file to wrap this ICUFile around.
     * @param logger
     *            The current logger.
     * @throws IOException
     */
    public ICUFile(String filename, Logger logger) throws IOException {
        if (filename == null || filename.trim().length() == 0)
            throw new IOException("cannot be blank");

        initialize(new File(filename), logger);
    }

    /**
     * Constructs an ICUFile around a file. See <code>initialize</code> for
     * details.
     * 
     * @param file
     *            The file to wrap this ICUFile around.
     * @param logger
     *            The current logger.
     * @throws IOException
     */
    public ICUFile(File file, Logger logger) throws IOException {
        initialize(file, logger);
    }

    /**
     * Performs the shared work of the constructors. Throws an IOException if
     * <code>file</code>...
     * <ul>
     * <li>does not exist</li>
     * <li>is not a file</li>
     * <li>ends with .ear or .war (these file types are unsupported)</li>
     * <li>does not end with .jar</li>
     * <li>is not updatable according the <code>isUpdatable</code></li>
     * <li>is signed.</li>
     * </ul>
     * If an exception is not thrown, the ICUFile is fully initialized.
     * 
     * @param file
     *            The file to wrap this ICUFile around.
     * @param logger
     *            The current logger.
     * @throws IOException
     */
    private void initialize(File file, Logger logger) throws IOException {
        this.icuFile = file;
        this.logger = logger;
        String message = null;

        if (!file.exists())
            message = "Skipped " + file.getPath() + " (does not exist).";
        else if (!file.isFile())
            message = "Skipped " + file.getPath() + " (not a file).";
        else if (file.getName().endsWith(".ear") || file.getName().endsWith(".war")) {
            message = "Skipped " + file.getPath() + " (this tool does not support .ear and .war files).";
            logger.loglnToBoth(message);
        } else if (!file.getName().endsWith(".jar"))
            message = "Skipped " + file.getPath() + " (not a jar file).";
        else if (!isUpdatable())
            message = "Skipped " + file.getPath() + " (not an updatable ICU4J jar).";
        else if (isSigned()) {
            message = "Skipped " + file.getPath() + " (signed jar).";
            logger.loglnToBoth(message);
        }

        if (message != null)
            throw new IOException(message);

        tzVersion = findEntryTZVersion(file, tzEntry);
    }

    /**
     * Returns the File object represented by this ICUFile object.
     * 
     * @return The File object represented by this ICUFile object.
     */
    public File getFile() {
        return icuFile;
    }

    /**
     * Returns the filename of this ICUFile object, without the path.
     * 
     * @return The filename of this ICUFile object, without the path.
     */
    public String getFilename() {
        return icuFile.getName();
    }

    /**
     * Returns the path of this ICUFile object, without the filename.
     * 
     * @return The path of this ICUFile object, without the filename.
     */
    public String getPath() {
        String path = icuFile.getPath();
        int pos = path.lastIndexOf(File.separator);
        path = (pos == -1) ? "" : path.substring(0, pos);
        return path;
    }

    /**
     * Returns the result of getFile().toString().
     * 
     * @return The result of getFile().toString().
     */
    public String toString() {
        return getFile().toString();
    }

    /**
     * Returns the ICU version of this ICU4J jar.
     * 
     * @return The ICU version of this ICU4J jar.
     */
    public String getICUVersion() {
        return icuVersion;
    }

    /**
     * Returns the timezone resource version.
     * 
     * @return The timezone resource version.
     */
    public String getTZVersion() {
        return tzVersion;
    }

    public boolean equals(Object other) {
        return (!(other instanceof ICUFile)) ? false : icuFile.equals(((ICUFile) other).icuFile);
    }

    /**
     * Updates the timezone resource in this ICUFile using
     * <code>insertURL</code> as the source of the new timezone resource and
     * the backup directory <code>backupDir</code> to store a copy of the
     * ICUFile.
     * 
     * @param insertURL
     *            The url location of the timezone resource to use.
     * @param backupDir
     *            The directory to store a backup for this ICUFile, or null if
     *            no backup.
     * @throws IOException
     */
    public void update(URL insertURL, File backupDir) throws IOException {
        if (!icuFile.canRead() || !icuFile.canWrite())
            throw new IOException("Missing permissions for " + icuFile);
        File backupFile = null;
        if ((backupFile = createBackupFile(icuFile, backupDir)) == null)
            throw new IOException("Failed to create a backup file.");
        if (!copyFile(icuFile, backupFile))
            throw new IOException("Could not replace the original jar.");
        if (!createUpdatedJar(backupFile, icuFile, tzEntry, insertURL))
            throw new IOException("Could not create an updated jar.");

        // get the new timezone resource version
        tzVersion = findEntryTZVersion(icuFile, tzEntry);
    }

    /**
     * Creates a temporary file for the jar file <code>inputFile</code> under
     * the directory <code>backupBase</code> and returns it, or returns null
     * if a temporary file could not be created. Does not put any data in the
     * newly created file yet.
     * 
     * @param inputFile
     *            The file to backup.
     * @param backupBase
     *            The directory where backups are to be stored.
     * @return The temporary file that was created.
     */
    private File createBackupFile(File inputFile, File backupBase) {
        logger.loglnToBoth("Creating backup file for + " + inputFile + " at " + backupBase + ".");
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
        }

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
            logger.loglnToBoth("Successfully created backup file at " + backupFile + ".");
        } catch (IOException ex) {
            logger.loglnToBoth("Failed to create backup file.");
            if (backupFile != null)
                backupFile.delete();
            if (backupDesc != null)
                backupDesc.delete();
            backupDir.delete();
            backupFile = null;
        } finally {
            if (ostream != null)
                ostream.close();
        }

        return backupFile;
    }

    /**
     * Copies <code>inputFile</code> to <code>outputFile</code>.
     * 
     * @param inputFile
     *            The input file.
     * @param outputFile
     *            The output file.
     * @return Whether the operation was successful.
     */
    private boolean copyFile(File inputFile, File outputFile) {
        logger.loglnToBoth("Copying from " + inputFile + " to " + outputFile + ".");
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
            logger.loglnToBoth("Copy successful.");
        } catch (IOException ex) {
            outputFile.delete();
            logger.loglnToBoth("Copy failed.");
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

    /**
     * Copies the jar entry <code>insertEntry</code> in <code>inputFile</code>
     * to <code>outputFile</code>.
     * 
     * @param inputFile
     *            The jar file containing <code>insertEntry</code>.
     * @param inputEntry
     *            The entry to copy.
     * @param outputFile
     *            The output file.
     * @return Whether the operation was successful.
     */
    private boolean copyEntry(File inputFile, JarEntry inputEntry, File outputFile) {
        logger.loglnToBoth("Copying from " + inputFile + "!/" + inputEntry + " to " + outputFile + ".");
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
            logger.loglnToBoth("Copy successful.");
        } catch (IOException ex) {
            outputFile.delete();
            logger.loglnToBoth("Copy failed.");
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

    /**
     * Copies <code>inputFile</code> to <code>outputFile</code>, replacing
     * <code>insertEntry</code> with <code>inputURL</code>.
     * 
     * @param inputFile
     *            The input jar file.
     * @param outputFile
     *            The output jar file.
     * @param insertEntry
     *            The entry to be replaced.
     * @param inputURL
     *            The URL to use in replacing the entry.
     * @return Whether the operation was successful.
     */
    private boolean createUpdatedJar(File inputFile, File outputFile, JarEntry insertEntry, URL inputURL) {
        logger.loglnToBoth("Copying " + inputFile + " to " + outputFile + ", replacing " + insertEntry + " with "
                + inputURL + ".");
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
            logger.loglnToBoth("Copy successful.");
        } catch (IOException ex) {
            outputFile.delete();
            logger.loglnToBoth("Copy failed.");
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

    /**
     * Gathers information on the jar file represented by this ICUFile object
     * and returns whether it is an updatable ICU4J jar file.
     * 
     * @return Whether the jar file represented by this ICUFile object is an
     *         updatable ICU4J jar file.
     */
    private boolean isUpdatable() {
        JarFile jar = null;
        boolean success = false;

        try {
            // open icuFile as a jar file
            jar = new JarFile(icuFile);

            // get its manifest to determine the ICU version
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

            // if the jar's directory structure contains TZ_ENTRY_DIR and there
            // is a timezone resource in the jar, then the jar is updatable
            success = (jar.getJarEntry(TZ_ENTRY_DIR) != null) && ((this.tzEntry = getTZEntry(jar)) != null);
        } catch (IOException ex) {
            // unable to create the JarFile or unable to get the Manifest
            // log the unexplained i/o error, but we must drudge on
            logger.loglnToBoth("Error reading " + icuFile.getPath() + ".");
        } finally {
            // close the jar gracefully
            if (jar != null)
                try {
                    jar.close();
                } catch (IOException ex) {
                }
        }

        // return whether the jar is updatable or not
        return success;
    }

    /**
     * Finds the jar entry in the jar file that represents a timezone resource
     * and returns it, or null if none is found.
     * 
     * @param jar
     *            The jar file to search.
     * @return The jar entry representing the timezone resource in the jar file,
     *         or null if none is found.
     */
    private static JarEntry getTZEntry(JarFile jar) {
        JarEntry tzEntry = null;
        Enumeration e = jar.entries();
        while (e.hasMoreElements()) {
            tzEntry = (JarEntry) e.nextElement();
            if (tzEntry.getName().endsWith(TZ_ENTRY_FILENAME))
                return tzEntry;
        }
        return null;
    }

    /**
     * Determines whether a timezone resource in a jar file is signed.
     * 
     * @return Whether a timezone resource in a jar file is signed.
     */
    private boolean isSigned() {
        return tzEntry.getCertificates() != null;
    }

    /**
     * Determines the version of a timezone resource in a jar file without
     * locking the jar file.
     * 
     * @param icuFile
     *            The jar file containing the timezone resource.
     * @param tzEntry
     *            The jar entry in the given jar file representing the timezone
     *            resource.
     * @return The version of the timezone resource.
     */
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

    /**
     * Determines the version of a timezone resource as a standard file without
     * locking the file.
     * 
     * @param tzFile
     *            The file representing the timezone resource.
     * @param logger
     *            The current logger.
     * @return The version of the timezone resource.
     */
    public static String findFileTZVersion(File tzFile, Logger logger) {
        ICUFile rawTZFile = new ICUFile(logger);

        try {
            File temp = File.createTempFile("zoneinfo", ".res");
            temp.deleteOnExit();
            rawTZFile.copyFile(tzFile, temp);
            return findTZVersion(temp);
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
            return null;
        }
    }

    /**
     * Determines the version of a timezone resource as a standard file, but
     * locks the file for the duration of the program.
     * 
     * @param tzFile
     *            The file representing the timezone resource.
     * @return The version of the timezone resource.
     */
    private static String findTZVersion(File tzFile) {
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

    // public static String findURLTZVersion(File tzFile) {
    // try {
    // File temp = File.createTempFile("zoneinfo", ".res");
    // temp.deleteOnExit();
    // copyFile(tzFile, temp);
    // return findTZVersion(temp);
    // } catch (IOException ex) {
    // ex.printStackTrace();
    // return null;
    // }
    // }

    public static final int BUFFER_SIZE = 1024;

    public static final String ICU_VERSION_UNKNOWN = "Unknown";

    public static final String TZ_VERSION_UNKNOWN = "Unknown";

    public static final String TZ_VERSION_KEY = "TZVersion";

    public static final String TZ_ENTRY_DIR = "com/ibm/icu/impl";

    public static final String TZ_ENTRY_FILENAME_PREFIX = "zoneinfo";

    public static final String TZ_ENTRY_FILENAME_EXTENSION = ".res";

    public static final String TZ_ENTRY_FILENAME = TZ_ENTRY_FILENAME_PREFIX + TZ_ENTRY_FILENAME_EXTENSION;

    private File icuFile;

    private String icuVersion;

    private String tzVersion;

    private JarEntry tzEntry;

    private Logger logger;
}
