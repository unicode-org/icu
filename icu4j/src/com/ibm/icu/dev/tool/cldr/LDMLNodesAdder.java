/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */
package com.ibm.icu.dev.tool.cldr;

import com.ibm.icu.dev.tool.UOption;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.*;

/**
 * @author ram
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LDMLNodesAdder {
    /**
     * These must be kept in sync with getOptions().
     */
    private static final int HELP1 = 0;
    private static final int HELP2 = 1;
    private static final int MAINFILE= 2;
    private static final int EXTRACTFILE = 3;
    private static final int DESTFILE = 4;
    private static final int ADDONLY = 5;
    
    private static final UOption[] options = new UOption[] {
            UOption.HELP_H(),
            UOption.HELP_QUESTION_MARK(),
            UOption.create("main", 'm', UOption.REQUIRES_ARG),
            UOption.create("extract", 'e', UOption.REQUIRES_ARG),
            UOption.DESTDIR(),
            UOption.create("add-only", 'a', UOption.NO_ARG),
            
    };
    
    private String  mainfile     = null;
    private String  destfile     = null;
    private String  extractfile  = null;
    private boolean addonly      = false;   
    public static void main(String[] args) {
        LDMLNodesAdder cnv = new LDMLNodesAdder();
        cnv.processArgs(args);
    }
    
    private void usage() {
        System.out.println("\nUsage: LDMLNodesAdder [OPTIONS] [XPATH1] [XPATH2]\n\n"+
                "This program is used to extract nodes from extract LDML file and merge \n"+
                "the extracted nodes with the main LDML file\n"+
                "Please refer to the following options. Options are not case sensitive.\n"+
                "Options:\n"+
                "-m or --main               The LDML file to which the extracted nodes are merged\n" +
                "-o or --extract            The LDML file from which the nodes need to be extracted\n" +
                "-d or --destination        destination directory, followed by the path, default is current directory.\n"+
                "-a or --add-only           Don't replace existing nodes in the main document.\n"+
                "-h or -? or --help         this usage text.\n"+
                "example: com.ibm.icu.dev.tool.cldr.LDMLNodesAdder -m locale/common/main/en.xml -o locale/ibmjdk/main/en.xml //ldml/dates/calendars/calendar[@type='chinese'] //ldml/posix");
         System.exit(-1);
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

        if(options[MAINFILE].doesOccur) {
            mainfile = options[MAINFILE].value;
        }
        if(options[DESTFILE].doesOccur) {
            destfile = options[DESTFILE].value;
        }
        if(options[EXTRACTFILE].doesOccur) {
            extractfile = options[EXTRACTFILE].value;
        }
        if(options[ADDONLY].doesOccur) {
            addonly = true;
        }
        if(destfile==null){
           throw new RuntimeException("Destination not specified");
        }
        if(remainingArgc<1){
            usage();
            System.exit(-1);
        }
        try{
            Document maindoc = LDMLUtilities.parse(mainfile, false);
            Document extractdoc  = LDMLUtilities.parse(extractfile, false);
          
            System.out.println("INFO: Merging nodes and marking them draft");
            mergeNodes(maindoc, extractdoc, args, remainingArgc);
            System.out.println("INFO: Removing specials");
            removeSpecials(maindoc);
            System.out.println("INFO: Fixing eras");
            fixEras(maindoc);
            maindoc.normalize();
            OutputStreamWriter writer = new
            OutputStreamWriter(new FileOutputStream(destfile),"UTF-8");
            PrintWriter pw = new PrintWriter(writer);
            LDMLUtilities.printDOMTree(maindoc,pw);
            writer.flush(); 
         }catch( Exception e){ 
             e.printStackTrace();
             //System.exit(-1);
         }
    }
   // private static final String xmlheader = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    //private static final String doctype   = "<!DOCTYPE ldml SYSTEM \"http://oss.software.ibm.com/cvs/icu/~checkout~/locale/ldml.dtd\">\n";
    
    private void mergeNodes(Document maindoc, Document extractdoc, String[] args, int argc ){
        for(int i=0; i< argc; i++){
           Node extract = LDMLUtilities.getNode(extractdoc, args[i]);
           Node nodeInSource = LDMLUtilities.getNode(maindoc, args[i]);
           Node parentNode = null;
           StringBuffer xpath = new StringBuffer(args[i]);
           if(nodeInSource==null){
               int j=0;
               while(parentNode==null){
                   XPathTokenizer.deleteToken(xpath);
                   parentNode = LDMLUtilities.getNode(maindoc, xpath.toString());
                   if(j>0){
                       extract = extract.getParentNode();
                   }
                   j++;
               }
               extract = maindoc.importNode(extract, true);
               parentNode.appendChild(extract);
               Attr draft = maindoc.createAttribute("draft");
               draft.setValue("true");
               ((Element) extract).setAttributeNode(draft);
           }else if(!addonly){
               parentNode = nodeInSource.getParentNode();
               extract = maindoc.importNode(extract, true);
               parentNode.replaceChild(extract, nodeInSource);
               Attr draft = maindoc.createAttribute("draft");
               draft.setValue("true");
               ((Element) extract).setAttributeNode(draft);
           }
        }
    }
    private void removeSpecials(Document doc){
       Node[] nodes = LDMLUtilities.getElementsByTagName(doc, LDMLConstants.SPECIAL);
       if(nodes!=null){
           for(int i=0;i<nodes.length;i++){
               Node parent = nodes[i].getParentNode();
               parent.removeChild(nodes[i]);
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
