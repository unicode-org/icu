/*
 * Copyright (c) 2004, International Business Machines
 * Corporation and others.  All Rights Reserved.
 *
 * Created on Sep 17, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.dev.tool.cldr;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ibm.icu.dev.tool.UOption;

/**
 * @author ram
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FixEras {
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int DESTDIR = 2;
    private static final int SOURCEDIR = 3;
    private static final UOption[] options = new UOption[] {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.DESTDIR(),
            UOption.SOURCEDIR(),
            
    };
    private String  destdir     = null;
    private String  sourcedir   = null;
    
    public static void main(String[] args) {
        FixEras cnv = new FixEras();
        cnv.processArgs(args);
    }
    
    private void usage() {
        System.out.println("\nUsage: FixEras [OPTIONS] [XPATH1] [XPATH2]\n\n"+
                "This program is used to extract nodes from extract LDML file and merge \n"+
                "the extracted nodes with the main LDML file\n"+
                "Please refer to the following options. Options are not case sensitive.\n"+
                "Options:\n"+
                "-s or --sourcedir          source directory followed by the path.\n"+
                "-d or --destination        destination directory, followed by the path, default is current directory.\n"+
                "-h or -? or --help         this usage text.\n"+
                "example: com.ibm.icu.dev.tool.cldr.FixErs ar.xml\n"
                );        System.exit(-1);
    }
    
    private void processArgs(String[] args) {
        int remainingArgc = 0;
        try{
            remainingArgc = UOption.parseArgs(args, options);
        }catch (Exception e){
            System.err.println("ERROR: "+ e.toString());
            e.printStackTrace();
            usage();
        }
        if(args.length==0 || options[HELP1].doesOccur || options[HELP2].doesOccur) {
            usage();
        }

        if(options[DESTDIR].doesOccur) {
            destdir = options[DESTDIR].value;
        }
        if(options[SOURCEDIR].doesOccur) {
            sourcedir = options[SOURCEDIR].value;
        }
        if(destdir==null){
           throw new RuntimeException("Destination not specified");
        }
        if(remainingArgc<1){
            usage();
            System.exit(-1);
        }
        for(int i=0; i<remainingArgc;i++){
            try{
                String sourcefile = null; 
                String file = args[i];
                if(sourcedir!= null){
                    sourcefile= sourcedir+"/"+file;
                }else{
                    sourcefile = file;
                }
                Document maindoc = LDMLUtilities.parse(sourcefile, false);
                System.out.println("INFO: Fixing eras of "+file);
                fixEras(maindoc);
                maindoc.normalize();
                String destfile = destdir+"/"+file;
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(destfile),"UTF-8");
                PrintWriter pw = new PrintWriter(writer);
                LDMLUtilities.printDOMTree(maindoc,pw);
                writer.flush(); 
                writer.close();
             }catch( Exception e){ 
                 e.printStackTrace();
                 System.exit(-1);
             }
        }
    }
    private void fixEras(Document doc){
        Node[] nodes = LDMLUtilities.getElementsByTagName(doc, LDMLConstants.ERA);
        if(nodes!=null){
            for(int i=0; i<nodes.length; i++){
                NamedNodeMap attr = nodes[i].getAttributes();
                Node type = attr.getNamedItem(LDMLConstants.TYPE);
                if(type!=null){
                    String val = type.getNodeValue();
                    int j = Integer.parseInt(val);
                    if(j>0){
                        j--;
                    }
                    type.setNodeValue(Integer.toString(j));
                }
            }
        }
    }
}
