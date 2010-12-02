/*
 **********************************************************************
 * Copyright (c) 2008-2010, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package com.ibm.icu.dev.tools.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

public class NonAsciiFileDetector
{
	
	private static boolean VERBOSE = false;
	
    public static class ICUSourceFileFilter implements FilenameFilter
    {
    	int matched = 0;
    	int skipped = 0;
        public boolean accept(File dir, String name) 
        {
            boolean doAccept = name.endsWith(".cpp") || name.endsWith(".c") || name.endsWith(".h") || name.endsWith(".java");
            if(doAccept) {
            	matched++;
            } else {
            	skipped++;
            }
            return doAccept;
        }
        public String stats() {
        	return "Checked " + matched + " files and skipped " + skipped + " files";
        }
    }

    public static int isNonAscii(File file) throws IOException
    {
        BufferedReader in = new BufferedReader(new FileReader(file));
        int line = 0;
        while (true) {
            String str = in.readLine();
            if (str == null) {
                in.close();
                return -1;
            }
            for (int i = 0; i < str.length(); i ++) {
                if (str.charAt(i) > 0x7f || str.charAt(i)==0x07) {
                    System.out.println("Ascii test failed in " 
                                       + file.getAbsolutePath() + " line "
                                       + line + " string\n" + str);
                    // non-latin1
                    in.close();
                    return line;
                }
            }
            line ++;
        }
    }
    
    public static void listFiles(File file, 
                                 FilenameFilter filter,
                                 Vector<File> list) throws IOException 
    {
    	if(VERBOSE) System.err.println(" .. checking " + file.getPath() );
        File files[] = file.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i ++) {
                if (files[i].isDirectory()) {
                	if(files[i].getAbsolutePath().equals(files[i].getCanonicalPath())) {
                		if(!files[i].getName().equals(".svn")) { // skip .svn dirs
                			listFiles(files[i], filter, list);          
                		}
                	} else {
                		if(VERBOSE) {
                			System.err.println("..    skipping symlink " + files[i].getPath());
                		}
                	}
                }
                else {
                    if (filter.accept(file, files[i].getName())) {
                        list.add(files[i]);
                    }
                }
            }
        }
    }

    public static Map<String, Integer> getNonAsciiFiles(File directory, 
                                            FilenameFilter filter)
                                            throws IOException
    {
        Vector<File> files = new Vector<File>();
        Map<String,Integer> result = new TreeMap<String,Integer>();
        listFiles(directory, filter, files);
        int filecount = files.size();
        if (filecount == 0) {
            return null;
        }
        for (int i = 0; i < filecount; i ++) {
             int isnonascii = isNonAscii(files.elementAt(i));
             if (isnonascii != -1) {
            	 result.put(files.elementAt(i).getAbsolutePath(),new Integer(isnonascii));
             }
        }
        return result;
    }

    public static void main(String arg[])
    {
    	int nextArg = 0;
    	for(nextArg = 0;nextArg<arg.length && arg[nextArg].startsWith("-");nextArg++) {
    		if(arg[nextArg].equals("-v")) {
    			VERBOSE=true;
    			System.err.println(" .. verbose mode.");
    		}
    	}
    	if(nextArg == arg.length) {
    		System.err.println(NonAsciiFileDetector.class.getSimpleName()+": error, no directories specified!");
    	}
    	int bad=0;
    	for(;nextArg<arg.length;nextArg++){
	        try {
	        	ICUSourceFileFilter isff = new ICUSourceFileFilter();
	        	File dir = new File(arg[nextArg]);
	            if(!dir.isDirectory()) {
	            	throw new FileNotFoundException("Not a directory: " + dir.getAbsolutePath());
	            }
	            Map<String, Integer> nonascii = getNonAsciiFiles(dir, isff);
	            boolean noised = false;
	            System.out.println();
	            if (nonascii != null && nonascii.size() > 0) {
	                for (Entry<String, Integer> e : nonascii.entrySet()) {
	                     if(!noised) {
	                    	 System.out.println("Non ascii files in  " + arg[nextArg] + ": ");
	                    	 noised = true;
	                     }
	                     System.out.println("" 
	                            +e.getKey() + ":" 
	                            +e.getValue());
	                     bad++;
	                }
	            } else {
//	            	if (VERBOSE==true) {
	            		System.out.println("No non ascii files in " + arg[nextArg]);
//	            	}
	            }
//	            if(VERBOSE==true) {
	            	System.out.println( isff.stats());
//	            }
	        } catch (IOException e) {
	        	System.err.println("Error processing " + arg[nextArg]);
	        	e.printStackTrace();
	        }
    	}
    	if(bad>0) {
    		System.err.println(bad+" non-ascii files found in total.");
    	}	
    }
}
             
