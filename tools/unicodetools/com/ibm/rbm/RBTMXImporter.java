/*
 *****************************************************************************
 * Copyright (C) 2000-2002, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/rbm/RBTMXImporter.java,v $ 
 * $Date: 2002/05/20 18:53:09 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.util.*;

import org.apache.xerces.parsers.*;
import org.apache.xerces.dom.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import org.xml.sax.*;


/**
 * This is the super class for all importer plug-in classes. This class defines the methods
 * and functionality common to all importers. This includes setting up the options dialog and
 * displaying it to the user, performing the actual insertions into the resource bundle manager,
 * and managing any import conflicts.
 * 
 * @author Jared Jackson - Email: <a href="mailto:jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see com.ibm.rbm.RBManager
 */
public class RBTMXImporter extends RBImporter {
	
    DocumentImpl tmx_xml = null;

    /**
     * Basic constructor for the TMX importer from the parent RBManager data and a Dialog title.
     */
    
    public RBTMXImporter(String title, RBManager rbm, RBManagerGUI gui) {
        super(title, rbm, gui);
    }
	
    protected void setupFileChooser() {
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                if (f.getName().endsWith(".tmx")) return true;
                return false;
            }
        
            public String getDescription() {
                return Resources.getTranslation("import_TMX_file_description");
            }
        });
    }
	
    protected void beginImport() throws IOException {
        super.beginImport();
        File tmx_file = getChosenFile();
		
        try {
            InputSource is = new InputSource(new FileInputStream(tmx_file));
            //is.setEncoding("UTF-8");
            DOMParser parser = new DOMParser();
            parser.parse(is);
            tmx_xml = (DocumentImpl)parser.getDocument();
        } catch (Exception e) {
            RBManagerGUI.debugMsg(e.getMessage());
            e.printStackTrace(System.err);
        }
        if (tmx_xml == null) return;
        
        importDoc();
    }
    
    private void importDoc() {
        if (tmx_xml == null) return;
        ElementImpl root = (ElementImpl)tmx_xml.getDocumentElement();
        Node node = root.getFirstChild();
        while (node != null && (node.getNodeType() != Node.ELEMENT_NODE || !(node.getNodeName().equalsIgnoreCase("header")))) {
            node = node.getNextSibling();
        }
        ElementImpl header = (ElementImpl)node;
        node = root.getFirstChild();
        while (node != null && (node.getNodeType() != Node.ELEMENT_NODE || !(node.getNodeName().equalsIgnoreCase("body")))) {
            node = node.getNextSibling();
        }
        ElementImpl body = (ElementImpl)node;
        resolveEncodings(getEncodingsVector(body));
		
        // Now do the actual import resource by resource
        NodeList tu_list = body.getElementsByTagName("tu");
        for (int i=0; i < tu_list.getLength(); i++) {
            ElementImpl tu_elem = (ElementImpl)tu_list.item(i);
            // Get the key value
            String name = tu_elem.getAttribute("tuid");
            if (name == null || name.length() < 1) continue;
            // Get the group if it exists
            String group = null;
            NodeList prop_list = tu_elem.getElementsByTagName("prop");
            for (int j=0; j < prop_list.getLength(); j++) {
                ElementImpl prop_elem = (ElementImpl)prop_list.item(j);
                String type = prop_elem.getAttribute("type");
                if (type != null && type.equals("x-Group")) {
                    prop_elem.normalize();
                    NodeList text_list = prop_elem.getChildNodes();
                    if (text_list.getLength() < 1) continue;
                    TextImpl text_elem = (TextImpl)text_list.item(0);
                    group = text_elem.getNodeValue();
                }
            }
            if (group == null || group.length() < 1) group = getDefaultGroup();
            
            NodeList tuv_list = tu_elem.getElementsByTagName("tuv");
            // For each tuv element
            for (int j=0; j < tuv_list.getLength(); j++) {
                ElementImpl tuv_elem = (ElementImpl)tuv_list.item(j);
                String encoding = tuv_elem.getAttribute("lang");
                // Get the current encoding
                if (encoding == null) continue;
                char array[] = encoding.toCharArray();
                for (int k=0; k < array.length; k++) {
                    if (array[k] == '-') array[k] = '_';
                }
                encoding = String.valueOf(array);
                // Get the translation value
                NodeList seg_list = tuv_elem.getElementsByTagName("seg");
                if (seg_list.getLength() < 1) continue;
                ElementImpl seg_elem = (ElementImpl)seg_list.item(0);
                seg_elem.normalize();
                NodeList text_list = seg_elem.getChildNodes();
                if (text_list.getLength() < 1) continue;
                TextImpl text_elem = (TextImpl)text_list.item(0);
                String value = text_elem.getNodeValue();
                if (value == null || value.length() < 1) continue;
                // Create the bundle item
                BundleItem item = new BundleItem(null, name, value);
                // Get creation, modification values
                item.setCreatedDate(tuv_elem.getAttribute("creationdate"));
                item.setModifiedDate(tuv_elem.getAttribute("changedate"));
                if (tuv_elem.getAttribute("changeid") != null) item.setModifier(tuv_elem.getAttribute("changeid"));
                if (tuv_elem.getAttribute("creationid") != null) item.setCreator(tuv_elem.getAttribute("creationid"));
                // Get properties specified
                prop_list = tuv_elem.getElementsByTagName("prop");
                Hashtable lookups = null;
                for (int k=0; k < prop_list.getLength(); k++) {
                    ElementImpl prop_elem = (ElementImpl)prop_list.item(k);
                    String type = prop_elem.getAttribute("type");
                    if (type != null && type.equals("x-Comment")) {
                        // Get the comment
                        prop_elem.normalize();
                        text_list = prop_elem.getChildNodes();
                        if (text_list.getLength() < 1) continue;
                        text_elem = (TextImpl)text_list.item(0);
                        String comment = text_elem.getNodeValue();
                        if (comment != null && comment.length() > 0) item.setComment(comment);
                    } else if (type != null && type.equals("x-Translated")) {
                        // Get the translated flag value
                        prop_elem.normalize();
                        text_list = prop_elem.getChildNodes();
                        if (text_list.getLength() < 1) continue;
                        text_elem = (TextImpl)text_list.item(0);
                        if (text_elem.getNodeValue() != null) {
                            if (text_elem.getNodeValue().equalsIgnoreCase("true")) item.setTranslated(true);
                            else if (text_elem.getNodeValue().equalsIgnoreCase("false")) item.setTranslated(false);
                            else item.setTranslated(getDefaultTranslated());
                        } else item.setTranslated(getDefaultTranslated());
                    } else if (type != null && type.equals("x-Lookup")) {
                        // Get a lookup value
                        prop_elem.normalize();
                        text_list = prop_elem.getChildNodes();
                        if (text_list.getLength() < 1) continue;
                        text_elem = (TextImpl)text_list.item(0);
                        if (text_elem.getNodeValue() != null) {
                            String text = text_elem.getNodeValue();
                            if (text.indexOf("=") > 0) {
                                try {
                                    if (lookups == null) lookups = new Hashtable();
                                    String lkey = text.substring(0,text.indexOf("="));
                                    String lvalue = text.substring(text.indexOf("=")+1,text.length());
                                    lookups.put(lkey, lvalue);
                                } catch (Exception ex) { /* String out of bounds - Ignore and go on */ }
                            }
                        } else item.setTranslated(getDefaultTranslated());
                    }
                }
                if (lookups != null) item.setLookups(lookups);
                importResource(item, encoding, group);
            }
        }
    }
	
    private Vector getEncodingsVector(ElementImpl body) {
        String empty = "";
        if (body == null) return null;
        Hashtable hash = new Hashtable();
        NodeList tu_list = body.getElementsByTagName("tu");
        for (int i=0; i < tu_list.getLength(); i++) {
            ElementImpl tu_elem = (ElementImpl)tu_list.item(i);
            NodeList tuv_list = tu_elem.getElementsByTagName("tuv");
            for (int j=0; j < tuv_list.getLength(); j++) {
                ElementImpl tuv_elem = (ElementImpl)tuv_list.item(j);
                String encoding = tuv_elem.getAttribute("lang");
                if (encoding == null) continue;
                char array[] = encoding.toCharArray();
                for (int k=0; k < array.length; k++) {
                    if (array[k] == '-') array[k] = '_';
                }
                encoding = String.valueOf(array);
                if (!(hash.containsKey(encoding))) hash.put(encoding,empty);
            }
        }
        Vector v = new Vector();
        Enumeration enum = hash.keys();
        while (enum.hasMoreElements()) { v.addElement(enum.nextElement()); }
        return v;
    }
}