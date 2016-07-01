// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 1996-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.serializable.SerializableTestUtility.Handler;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * @author sgill
 * @author emader
 */
@RunWith(JUnitParamsRunner.class)
public class CompatibilityTest extends TestFmwk
{
    //TODO(junit) - requires code to read the serialized classes from a jar
    @Ignore
    @Test
    @Parameters(method="generateClassList")
    public void testCompatibility(String testFileName) throws ClassNotFoundException, IOException {
        File testFile = new File(testFileName);
        Object[] oldObjects = SerializableTestUtility.getSerializedObjects(testFile);
        int start = testFileName.lastIndexOf('/') + 1;
        int end = testFileName.lastIndexOf('.');
        String className = testFileName.substring(start, end);
        Handler classHandler = SerializableTestUtility.getHandler(className);
        
        Object[] testObjects = classHandler.getTestObjects();
        for (int i = 0; i < testObjects.length; i++) {
            if (!classHandler.hasSameBehavior(oldObjects[i], testObjects[i])) {
                errln("Input object " + i + " failed behavior test.");
            }            
        }
    }
    
    @SuppressWarnings("unused")
    private List<String> generateClassList() {
        List<String> classList = new ArrayList();

        URL dataURL = getClass().getResource("data");

        File topDir = new File(dataURL.getPath());
        File dataDirs[] = topDir.listFiles(new FileFilter() { 
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }});

        File dataDirs2[] = topDir.listFiles();
        System.out.println("topDir = " + topDir);
        //System.out.println("dataDirs = " + dataDirs);
        System.out.println("dataDirs2 = " + dataDirs2);
        for (File dataDir : dataDirs2) {
            System.out.println("\t" + dataDir);
        }
        for (File dataDir : dataDirs) {
            File files[] = dataDir.listFiles(new FileFilter() { 
                public boolean accept(File pathname) {
                    return pathname.isFile() && pathname.getName().endsWith(".dat");
                }});

            String dataDirName = dataDir.getName();

            element_loop:
            for (File file : files) {
                String filename = file.getName();
                String className = filename.substring(0, filename.lastIndexOf("."));

                // Skip some cases which do not work well
                for (int skip = 0; skip < SKIP_CASES.length; skip++) {
                    if (dataDirName.equals(SKIP_CASES[skip][0]) && filename.equals(SKIP_CASES[skip][1])) {
                        logln("Skipping test case - " + dataDirName + "/" + className);
                        continue element_loop;
                    }
                }
                classList.add(file.getAbsolutePath());
            }
        }
        // TODO(junit): add randomization support on the list based on the params object
        
        return classList;
    }

    private static final String[][] SKIP_CASES = {
            // ICU 52+ PluralRules/PluralFormat/CurrencyPluralInfo are not
            // serialization-compatible with previous versions. 
            {"ICU_50.1", "com.ibm.icu.text.CurrencyPluralInfo.dat"},
            {"ICU_51.1", "com.ibm.icu.text.CurrencyPluralInfo.dat"},

            {"ICU_50.1", "com.ibm.icu.text.PluralFormat.dat"},
            {"ICU_51.1", "com.ibm.icu.text.PluralFormat.dat"},

            {"ICU_50.1", "com.ibm.icu.text.PluralRules.dat"},
            {"ICU_51.1", "com.ibm.icu.text.PluralRules.dat"},

            // GeneralMeasureFormat was in technical preview, but is going away after ICU 52.1.
            {"ICU_52.1", "com.ibm.icu.text.GeneralMeasureFormat.dat"},

            // RuleBasedNumberFormat
            {"ICU_3.6",     "com.ibm.icu.text.RuleBasedNumberFormat.dat"},

            // ICU 4.8+ MessageFormat is not serialization-compatible with previous versions.
            {"ICU_3.6",     "com.ibm.icu.text.MessageFormat.dat"},
    };
}
