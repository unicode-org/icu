/*
 **********************************************************************
 * Copyright (c) 2006, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 * Created on 2006-7-24
 */
package com.ibm.icu.dev.tools.docs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.crimson.jaxp.DocumentBuilderFactoryImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 A utility to report the status change between two ICU releases

To use the utility
1. Generate the XML files
    (put the two ICU releases on your machine ^_^ )
    (generate 'Doxygen' file on Windows platform with Cygwin's help)
    Edit the generated 'Doxygen' file under ICU4C source directory
    a) GENERATE_XML           = YES
    b) Sync the ALIASES definiation
       (For example, copy the ALIASES defination from ICU 3.6
       Doxygen file to ICU 3.4 Doxygen file.)
    c) gerenate the XML files
2. Build the tool
    Download Apache Xerces Java Parser
    Build this file with the library
3. Edit the api-report-config.xml file & Change the file according your real configuration
4. Run the tool to generate the report.

 * @author Raymond Yang
 */
public class StableAPI {

    private String leftVer;
    private String leftDir;
//    private String leftStatus;
    
    private String rightVer;
    private String rightDir;
//    private String rightStatus;
    
    private String dumpCppXslt;
    private String dumpCXslt;
    private String reportXsl;
    private String resultFile;
    
    final private static String nul = "None"; 

    public static void main(String[] args) throws FileNotFoundException, TransformerException, ParserConfigurationException {
        
        StableAPI t = new StableAPI();
        t.parseArgs(args);
        Set full = new HashSet();

        Set setCpp = t.getFullList(t.dumpCppXslt);
        full.addAll(setCpp);
        
        Set setC = t.getFullList(t.dumpCXslt);
        full.addAll(setC);
        
        Node fullList = t.setToNode(full);
//        t.dumpNode(fullList,"");
        
        t.reportSelectedFun(fullList);
        System.out.println("Done. Please check " + t.resultFile);
    }


    private void parseArgs(String[] args){
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.length() == 0) {
                continue;
            }
            if (arg.equals("--help") ) {
                printUsage();
            } else if (arg.equals("--oldver") ) {
                leftVer = args[++i];
            } else if (arg.equals("--olddir") ) {
                leftDir = args[++i];
            } else if (arg.equals("--newver")) {
                rightVer = args[++i];
            } else if (arg.equals("--newdir")) {
                rightDir = args[++i];
            } else if (arg.equals("--cxslt") ) {
                dumpCXslt = args[++i];
            } else if (arg.equals("--cppxslt") ) {
                dumpCppXslt = args[++i];
            } else if (arg.equals("--reportxslt") ) {
                reportXsl = args[++i];
            } else if (arg.equals("--resultfile")) {
                resultFile = args[++i];
            } else {
                System.out.println("Unknown option: "+arg);
               printUsage();
            } 
        }
    }
    
    private static void printUsage(){
        System.out.println("Usage: StableAPI option* target*");
        System.out.println();
        System.out.println("Options:");
        System.out.println("    --help          Print this text");
        System.out.println("    --oldver        Version of old version of ICU");
        System.out.println("    --olddir        Directory that contains xml docs of old version");
        System.out.println("    --newver        Version of new version of ICU");
        System.out.println("    --newdir        Directory that contains xml docs of new version");
        System.out.println("    --cxslt         XSLT file for C docs");
        System.out.println("    --cppxslt       XSLT file for C++ docs");
        System.out.println("    --reportxslt    XSLT file for report docs");
        System.out.println("    --resultfile    Output file");
        System.exit(-1);
    }

    static String getAttr(Node node, String attrName){
        return node.getAttributes().getNamedItem(attrName).getNodeValue();
    }
    
    static String getAttr(NamedNodeMap attrList, String attrName){
        return attrList.getNamedItem(attrName).getNodeValue();
    }
    
    static class Fun {
        public String prototype;
        public String id;
        public String status;
        public String file;
        public boolean equals(Fun right){
            return this.prototype.equals(right.prototype);
        }
        static Fun fromXml(Node n){
            Fun f = new Fun();
            f.prototype = getAttr(n, "prototype");
            f.id = getAttr(n, "id");
            f.status = getAttr(n, "status");
            f.file = getAttr(n, "file");
            f.purifyPrototype();
            f.purifyFile();
            return f;
        }
        
        private void purifyFile(){
            int i = file.lastIndexOf("/");
            file = i == -1 ? file : file.substring(i+1);
        }
        
        /**
         * Special cases:
         * 
         * Remove the status attribute embedded in the C prototype
         * 
         * Remove the virtual keyword in Cpp prototype
         */
        private void purifyPrototype(){
            //refer to 'umachine.h'
            String statusList[] = {"U_CAPI", "U_STABLE", "U_DRAFT", "U_DEPRECATED", "U_OBSOLETE", "U_INTERNAL", "virtual"};
            for (int i = 0; i < statusList.length; i++) {
                String s = statusList[i];
                prototype = prototype.replaceAll(s,"");
                prototype = prototype.trim();
            }
            prototype = prototype.trim();
        }
//        private Element toXml(Document doc){
//            Element  ele = doc.createElement("func");
//            ele.setAttribute("prototype", prototype);
//            ele.setAttribute("id", id);
//            ele.setAttribute("status", status);
//            return ele;
//        }
    }
    
    static class JoinedFun {
        public String prototype;
        public String leftRefId;
        public String leftStatus;
        public String leftFile;
        public String rightRefId;
        public String rightStatus;
        public String rightFile;
        
        static JoinedFun fromLeftFun(Fun left){
            JoinedFun u = new JoinedFun();
            u.prototype = left.prototype;
            u.leftRefId = left.id;
            u.leftStatus = left.status;
            u.leftFile = left.file;
            u.rightRefId = nul;
            u.rightStatus = nul;
            u.rightFile = nul;
            return u;
        }
    
        static JoinedFun fromRightFun(Fun right){
            JoinedFun u = new JoinedFun();
            u.prototype = right.prototype;
            u.leftRefId = nul;
            u.leftStatus = nul;
            u.leftFile = nul;
            u.rightRefId = right.id;
            u.rightStatus = right.status;
            u.rightFile = right.file;
            return u;
        }
        
        static JoinedFun fromTwoFun(Fun left, Fun right){
            if (!left.equals(right)) throw new Error();
            JoinedFun u = new JoinedFun();
            u.prototype = left.prototype;
            u.leftRefId = left.id;
            u.leftStatus = left.status;
            u.leftFile = left.file;
            u.rightRefId = right.id;
            u.rightStatus = right.status;
            u.rightFile = right.file;
            return u;
        }

        Element toXml(Document doc){
            Element  ele = doc.createElement("func");
            ele.setAttribute("prototype", prototype);
//            ele.setAttribute("leftRefId", leftRefId);
            
            ele.setAttribute("leftStatus", leftStatus);
//            ele.setAttribute("rightRefId", rightRefId);
            ele.setAttribute("rightStatus", rightStatus);
            
            
//            String f = rightRefId.equals(nul) ? leftRefId : rightRefId;
//            int tail = f.indexOf("_");
//            f = tail != -1 ? f.substring(0, tail) : f;
//            f = f.startsWith("class") ? f.replaceFirst("class","") : f;
            String f = rightFile.equals(nul) ? leftFile : rightFile;
            ele.setAttribute("file", f);
            return ele;
        }
    }

    TransformerFactory transFac = TransformerFactory.newInstance();

    private void reportSelectedFun(Node joinedNode) throws FileNotFoundException, TransformerException{
        Transformer report = transFac.newTransformer(new DOMSource(getDocument(reportXsl)));
//        report.setParameter("leftStatus", leftStatus);
        report.setParameter("leftVer", leftVer);
//        report.setParameter("rightStatus", rightStatus);
        report.setParameter("rightVer", rightVer);
        report.setParameter("dateTime", new GregorianCalendar().getTime());
        report.setParameter("nul", nul);
        
        DOMSource src = new DOMSource(joinedNode);

        Result res = new StreamResult(new File(resultFile));
//        DOMResult res = new DOMResult();
        report.transform(src, res);
//        dumpNode(res.getNode(),"");
    }
    
    private Set getFullList(String dumpXsltFile) throws FileNotFoundException, TransformerException, ParserConfigurationException{
        // prepare transformer
        Transformer transformer = transFac.newTransformer(new DOMSource(getDocument(dumpXsltFile)));
//        Node joinedNode = null;

        DOMSource leftIndex = new DOMSource(getDocument(leftDir + "index.xml"));
        DOMResult leftResult = new DOMResult();
        transformer.setParameter("docFolder", leftDir);
        transformer.transform(leftIndex, leftResult);
        Node leftList = XPathAPI.selectSingleNode(leftResult.getNode(),"/list");
//        dumpNode(leftList,"");
        
        DOMSource rightIndex = new DOMSource(getDocument(rightDir + "index.xml"));
        DOMResult rightResutl = new DOMResult();
        transformer.setParameter("docFolder", rightDir);
        transformer.transform(rightIndex, rightResutl);
        Node rightList = XPathAPI.selectSingleNode(rightResutl.getNode(),"/list");
//        dumpNode(rightList,"");
        
        
        Set leftSet = nodeToSet(leftList);
        Set rightSet = nodeToSet(rightList);
        Set joined = fullJoin(leftSet, rightSet);
        return joined;
//        joinedNode = setToNode(joined);
//        dumpNode(joinedNode,"");
//        return joinedNode;
    }

    /**
     * @param node
     * @return      Set<Fun>
     */
    private Set nodeToSet(Node node){
        Set s = new HashSet();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            s.add(Fun.fromXml(n));
        }
        return s;
    }

    /**
     * @param set       Set<JoinedFun>
     * @return
     * @throws ParserConfigurationException
     */
    private Node setToNode(Set set) throws ParserConfigurationException{
        DocumentBuilderFactory dbf = DocumentBuilderFactoryImpl.newInstance();
        Document doc  = dbf.newDocumentBuilder().newDocument();
        Element root = doc.createElement("list");
        doc.appendChild(root);
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            JoinedFun fun = (JoinedFun) iter.next();
            root.appendChild(fun.toXml(doc));
        }
        return doc;
    }

    /**
     * full-join two Set on 'prototype' 
     * 
     * @param left     Set<Fun>
     * @param right    Set<Fun>
     * @return          Set<JoinedFun>
     */
    private static Set fullJoin(Set left, Set right){

        Set joined = new HashSet(); //Set<JoinedFun>
        Set common = new HashSet(); //Set<Fun>
        for (Iterator iter1 = left.iterator(); iter1.hasNext();) {
            Fun f1 = (Fun) iter1.next();
//            if (f1.prototype.matches(".*Transliterator::.*")){
//                System.err.println("left: " + f1.prototype);
//                System.err.println("left: " + f1.status);
//            }
            for (Iterator iter2 = right.iterator(); iter2.hasNext();) {
                Fun f2 = (Fun) iter2.next();
//                if ( f1.prototype.matches(".*filteredTransliterate.*")
//                  && f2.prototype.matches(".*filteredTransliterate.*")){
//                    System.err.println("right: " + f2.prototype);
//                    System.err.println("right: " + f2.status);
//                    System.err.println(f1.prototype.equals(f2.prototype));
//                    System.err.println(f1.prototype.getBytes()[0]);
//                    System.err.println(f2.prototype.getBytes()[0]);
//                }
                if (f1.equals(f2)) {
                    // should add left item to common set
                    // since we will remove common items with left set later
                    common.add(f1);
                    joined.add(JoinedFun.fromTwoFun(f1, f2));
                    right.remove(f2);
                    break;
                } 
            }
        }

        for (Iterator iter = common.iterator(); iter.hasNext();) {
            Fun f = (Fun) iter.next();
            left.remove(f);
        }
        
        for (Iterator iter = left.iterator(); iter.hasNext();) {
            Fun f = (Fun) iter.next();
            joined.add(JoinedFun.fromLeftFun(f));
        }
        
        for (Iterator iter = right.iterator(); iter.hasNext();) {
            Fun f = (Fun) iter.next();
            joined.add(JoinedFun.fromRightFun(f));
        }
        return joined;
    }
    
    private static void dumpNode(Node n, String pre){
        pre += " ";
        System.out.println(pre + "<" + n.getNodeName() + ">");
        //dump attribute
        NamedNodeMap attr = n.getAttributes();
        if (attr!=null){
        for (int i = 0; i < attr.getLength(); i++) {
            System.out.println(attr.item(i));
        }
        }
        
        // dump value
        String v = pre + n.getNodeValue();
//      if (n.getNodeType() == Node.TEXT_NODE) 
          System.out.println(v);
        
        // dump sub nodes
        NodeList nList = n.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            Node ln = nList.item(i);
            dumpNode(ln, pre + " ");
        }
        System.out.println(pre + "</" + n.getNodeName() + ">");
    }
    
    private static Document getDocument(String name) throws FileNotFoundException{
        FileInputStream fis = new FileInputStream(name);
        InputSource inputSource = new InputSource(fis);
        DOMParser parser = new DOMParser();
        //convert it into DOM
        try {
            parser.parse(inputSource);
          //  fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 
        Document doc = parser.getDocument();
        return doc;
    }

}
