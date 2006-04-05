package com.ibm.text.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.text.UCD.Default;
import com.ibm.text.UCD.GenerateData;
import com.ibm.text.UCD.MakeUnicodeFiles;
import com.ibm.text.UCD.UCD_Types;

public class UnicodeDataFile {
    public PrintWriter out;
    private String newFile;
    private String batName;
    private String mostRecent;
    private String filename;
    private UnicodeDataFile(){};
    private String fileType = ".txt";
    
    public static UnicodeDataFile openAndWriteHeader(String directory, String filename) throws IOException {
        return new UnicodeDataFile(directory, filename, false);
    }
    
    public static UnicodeDataFile openHTMLAndWriteHeader(String directory, String filename) throws IOException {
        return new UnicodeDataFile(directory, filename, true);
    }
    
    private UnicodeDataFile (String directory, String filename, boolean isHTML) throws IOException {
    	fileType = isHTML ? ".html" : ".txt";
    	String newSuffix = UnicodeDataFile.getFileSuffix(true, fileType);
        newFile = directory + filename + newSuffix;
        out = Utility.openPrintWriter(newFile, Utility.UTF8_UNIX);
        String[] batName2 = {""};
        mostRecent = UnicodeDataFile.generateBat(directory, filename, newSuffix, fileType, batName2);
        batName = batName2[0];
    	this.filename = filename;
        
    	if (!isHTML) {
	        out.println("# " + filename + UnicodeDataFile.getFileSuffix(false));
	        out.println(generateDateLine());
	        out.println("#");        
	        out.println("# Unicode Character Database");        
	        out.println("# Copyright (c) 1991-" + Default.getYear() + " Unicode, Inc.");
	        out.println(
	            "# For terms of use, see http://www.unicode.org/terms_of_use.html");
	        out.println("# For documentation, see UCD.html");
    	}
        try {
            Utility.appendFile("com/ibm/text/UCD/" + filename + "Header" + fileType, Utility.UTF8_UNIX, out);
        } catch (FileNotFoundException e) {
            /*
            out.println("# Unicode Character Database: Derived Property Data");
            out.println("# Generated algorithmically from the Unicode Character Database");
            out.println("# For documentation, see UCD.html");
            out.println("# Note: Unassigned and Noncharacter codepoints may be omitted");
            out.println("#       if they have default property values.");
            out.println("# ================================================");
            */
        }
    }
    
    public void close() throws IOException {
        try {
            Utility.appendFile(filename + "Footer" + fileType, Utility.UTF8_UNIX, out);
        } catch (FileNotFoundException e) {}
        out.close();           
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile), batName);
    }

    public static String generateDateLine() {
        return "# Date: " + Default.getDate() + " [MD]";
    }

    public static String getHTMLFileSuffix(boolean withDVersion) {
        return getFileSuffix(withDVersion, ".html");
    }

    public static String getFileSuffix(boolean withDVersion) {
    	return getFileSuffix(withDVersion, ".txt");
    }

    public static String getFileSuffix(boolean withDVersion, String suffix) {
        return "-"
            + Default.ucd().getVersion()
            + ((withDVersion && MakeUnicodeFiles.dVersion >= 0)
                ? ("d" + MakeUnicodeFiles.dVersion)
                : "")
            + suffix;
    }

    //Remove "d1" from DerivedJoiningGroup-3.1.0d1.txt type names
    
    public static String fixFile(String s) {
        int len = s.length();
        if (!s.endsWith(".txt")) return s;
        if (s.charAt(len-6) != 'd') return s;
        char c = s.charAt(len-5);
        if (c != 'X' && (c < '0' || '9' < c)) return s;
        s = s.substring(0,len-6) + s.substring(len-4);
        System.out.println("Fixing File Name: " + s);
        return s;
    }

    private static String generateBatAux(String batName, String oldName, String newName) throws IOException {
        String fullBatName = batName + ".bat";
        PrintWriter output = Utility.openPrintWriter(batName + ".bat", Utility.LATIN1_UNIX);
        
        newName = Utility.getOutputName(newName);
        System.out.println("Writing BAT to compare " + oldName + " and " + newName);
        
        File newFile = new File(newName);
        File oldFile = new File(oldName);
        output.println("\"C:\\Program Files\\Compare It!\\wincmp3.exe\" "
            // "\"C:\\Program Files\\wincmp.exe\" "
            + oldFile.getCanonicalFile()
            + " "
            + newFile.getCanonicalFile());
        output.close();
        return new File(Utility.getOutputName(fullBatName)).getCanonicalFile().toString();
    }

     /*   
        static String skeleton(String source) {
            StringBuffer result = new StringBuffer();
            source = source.toLowerCase();
            for (int i = 0; i < source.length(); ++i) {
                char c = source.charAt(i);
                if (c == ' ' || c == '_' || c == '-') continue;
                result.append(c);
            }
            return result.toString();
        }
        */
        // static final byte KEEP_SPECIAL = 0, SKIP_SPECIAL = 1;
        
    public static String generateBat(String directory, String fileRoot, String suffix, String fileType, String[] outputBatName) throws IOException {
        String mostRecent = Utility.getMostRecentUnicodeDataFile(UnicodeDataFile.fixFile(fileRoot), Default.ucd().getVersion(), true, true, fileType);
        if (mostRecent != null) {
            outputBatName[0] = UnicodeDataFile.generateBatAux(directory + "DIFF/Diff_" + fileRoot + suffix,
                mostRecent, directory + fileRoot + suffix);
        } else {
            System.out.println("No previous version of: " + fileRoot + ".txt");
            return null;
        }
    
        String lessRecent = Utility.getMostRecentUnicodeDataFile(UnicodeDataFile.fixFile(fileRoot), Default.ucd().getVersion(), false, true);
        if (lessRecent != null && !mostRecent.equals(lessRecent)) {
            UnicodeDataFile.generateBatAux(directory + "DIFF/OLDER-Diff_" + fileRoot + suffix,
                lessRecent, directory + fileRoot + suffix);
        }
        return mostRecent;
    }
}
    
