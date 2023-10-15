// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.serializable.SerializableTestUtility.Handler;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * @author sgill
 * @author emader
 */
@RunWith(JUnitParamsRunner.class)
public class CompatibilityTest extends CoreTestFmwk
{
    private static final class FileHolder {
        String className;
        String icuVersion;
        byte[] b;
        boolean skip;

        FileHolder(String fileName, byte[] b) {
            this.b = b;

            // Replace '\' with '/' to normalize fileName before extracting
            // substrings. This is required if serialization test data is
            // loaded from Windows file system.
            String tmpPath = fileName.replaceAll("\\\\", "/");

            int fileBreak = tmpPath.lastIndexOf('/');
            this.className = fileName.substring(fileBreak + 1, tmpPath.lastIndexOf('.'));
            int finalDirBreak = tmpPath.lastIndexOf("/ICU");
            this.icuVersion = tmpPath.substring(finalDirBreak + 1, fileBreak);
            className = className.substring(className.lastIndexOf('/') + 1);

            this.skip = skipFile(this.icuVersion, this.className);
        }

        private static boolean skipFile(String icuVersion, String className) {
            for (int skip = 0; skip < SKIP_CASES.length; skip++) {
                if (icuVersion.equals(SKIP_CASES[skip][0]) && className.equals(SKIP_CASES[skip][1])) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return icuVersion + "[" + className + "]";
        }
    }

    @Test
    @Parameters(method="generateClassList")
    public void testCompatibility(FileHolder holder) throws ClassNotFoundException, IOException {
        if (holder.skip) {
            logln("Skipping File = " + holder);
            return;
        }

        Object[] oldObjects = SerializableTestUtility.getSerializedObjects(holder.b);
        Handler classHandler = SerializableTestUtility.getHandler(holder.className);

        Object[] testObjects = classHandler.getTestObjects();
        for (int i = 0; i < testObjects.length; i++) {
            if (!classHandler.hasSameBehavior(oldObjects[i], testObjects[i])) {
                errln("Input object " + i + ", className " + holder.className + ": failed behavior test.");
            }
        }
    }

    /**
     * The path to an actual data resource file in the JAR. This is needed because when the
     * code is packaged for Android the resulting archive does not have entries for directories
     * and so only actual resources can be found.
     */
    private static final String ACTUAL_RESOURCE = "/ICU_3.6/com.ibm.icu.impl.OlsonTimeZone.dat";

    @SuppressWarnings("unused")
    private List<FileHolder> generateClassList() throws IOException {
        // Get the URL to an actual resource and then compute the URL to the directory just in
        // case the resources are in a JAR file that doesn't have entries for directories.
        URL dataURL = getClass().getResource("data" + ACTUAL_RESOURCE);
        try {
            dataURL = new URL(dataURL.toExternalForm().replace(ACTUAL_RESOURCE, ""));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String protocol = dataURL.getProtocol();

        if (protocol.equals("jar")) {
            return getJarList(dataURL);
        } else if (protocol.equals("file")) {
            return getFileList(dataURL);
        } else {
            errln("Don't know how to test " + dataURL);
            return null;
        }
    }

    private List<FileHolder> getFileList(URL dataURL) throws IOException {
        List<FileHolder> classList = new ArrayList();

        File topDir = new File(dataURL.getPath());
        File dataDirs[] = topDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }});

        for (File dataDir : dataDirs) {
            File files[] = dataDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".dat");
                }});

                for (File file : files) {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] fileBytes;
                    try {
                        fileBytes = SerializableTestUtility.copyStreamBytes(fis);
                    } finally {
                        fis.close();
                    }
                    classList.add(new FileHolder(file.getAbsolutePath(), fileBytes));
                }
        }
        return classList;
    }

    private List<FileHolder> getJarList(URL jarURL) throws IOException {
        List<FileHolder> classList = new ArrayList();

        String prefix = jarURL.getPath();
        int ix = prefix.indexOf("!/");
        if (ix >= 0) {
            prefix = prefix.substring(ix + 2);
        }

        JarFile jarFile = null;
        try {
            // Need to trim the directory off the JAR entry before opening the connection otherwise
            // it could fail as it will try and find the entry within the JAR which may not exist.
            String urlAsString = jarURL.toExternalForm();
            ix = urlAsString.indexOf("!/");
            jarURL = new URL(urlAsString.substring(0, ix + 2));

            JarURLConnection conn = (JarURLConnection) jarURL.openConnection();
            jarFile = conn.getJarFile();
            Enumeration entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = (JarEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();

                    if (entryName.startsWith(prefix) && entryName.endsWith(".dat")) {
                        FileHolder holder = new FileHolder(entryName,
                                SerializableTestUtility.copyStreamBytes(jarFile.getInputStream(entry)));
                        classList.add(holder);

                    }
                }
            }
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
        return classList;
    }

    private static final String[][] SKIP_CASES = {
            // ICU 52+ PluralRules/PluralFormat/CurrencyPluralInfo are not
            // serialization-compatible with previous versions.
            {"ICU_50.1", "com.ibm.icu.text.CurrencyPluralInfo"},
            {"ICU_51.1", "com.ibm.icu.text.CurrencyPluralInfo"},

            {"ICU_50.1", "com.ibm.icu.text.PluralFormat"},
            {"ICU_51.1", "com.ibm.icu.text.PluralFormat"},

            {"ICU_50.1", "com.ibm.icu.text.PluralRules"},
            {"ICU_51.1", "com.ibm.icu.text.PluralRules"},

            // GeneralMeasureFormat was in technical preview, but is going away after ICU 52.1.
            {"ICU_52.1", "com.ibm.icu.text.GeneralMeasureFormat"},

            // RuleBasedNumberFormat
            {"ICU_3.6",     "com.ibm.icu.text.RuleBasedNumberFormat"},

            // ICU 4.8+ MessageFormat is not serialization-compatible with previous versions.
            {"ICU_3.6",     "com.ibm.icu.text.MessageFormat"},
    };
}
