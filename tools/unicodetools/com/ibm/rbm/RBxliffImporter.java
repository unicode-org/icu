/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.*;

import com.ibm.rbm.gui.RBManagerGUI;

import java.util.*;

import org.apache.xerces.dom.ElementImpl;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * This imports XLIFF files into RBManager.
 * For more information see <a href="http://www.oasis-open.org/committees/xliff/documents/xliff-specification.htm">
 * http://www.oasis-open.org/committees/xliff/documents/xliff-specification.htm</a>
 * 
 * @author George Rhoten
 * @see com.ibm.rbm.RBManager
 */
public class RBxliffImporter extends RBImporter {
	
    Document xlf_xml = null;

    /**
     * Basic constructor for the XLIFF importer from the parent RBManager data and a Dialog title.
     */
    public RBxliffImporter(String title, RBManager rbm, RBManagerGUI gui) {
        super(title, rbm, gui);
    }
	
    protected void setupFileChooser() {
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().endsWith(".xlf"));
            }
        
            public String getDescription() {
                return Resources.getTranslation("import_XLF_file_description");
            }
        });
    }
	
    protected void beginImport() throws IOException {
        super.beginImport();
        File xlf_file = getChosenFile();
        FileInputStream fis;
		
        try {
        	fis = new FileInputStream(xlf_file);
            InputSource is = new InputSource(fis);
            //is.setEncoding("UTF-8");
            DOMParser parser = new DOMParser();
            parser.parse(is);
            xlf_xml = parser.getDocument();
            fis.close();
        } catch (SAXException e) {
            RBManagerGUI.debugMsg(e.getMessage());
            e.printStackTrace(System.err);
            return;
        }
        if (xlf_xml == null) return;
        
        importDoc();
        fis.close();
        
    }
    
    private void importDoc() {
        if (xlf_xml == null)
            return;
        String language = "";
        String bundleNote = null;
        Element root = xlf_xml.getDocumentElement();
        Node fileNode = root.getFirstChild();
        Node header = null;
        Node node = null;
        while (fileNode != null && !(fileNode.getNodeType() == Node.ELEMENT_NODE && fileNode.getNodeName().equalsIgnoreCase("file"))) {
            fileNode = fileNode.getNextSibling();
        }
        header = fileNode.getFirstChild();
        while (header != null
        		&& !(header.getNodeType() == Node.ELEMENT_NODE
        		&& (header.getNodeName().equalsIgnoreCase("header")
				|| header.getNodeName().equalsIgnoreCase("body")))) {
        	header = header.getNextSibling();
        }
        if (header.getNodeName().equalsIgnoreCase("header")) {
        	// Get the notes if from the header if they exist.
            NodeList header_note_list = ((Element)header).getElementsByTagName("note");
            if (header_note_list.getLength() > 0) {
	            Text text_elem = (Text)header_note_list.item(0).getChildNodes().item(0);
	            if (text_elem != null) {
		            String value = text_elem.getNodeValue();
		            if (value != null && value.length() > 0) {
		            	bundleNote = value;
		            }
	            }
            }
        }
        node = header.getNextSibling();
        while (node != null && !(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equalsIgnoreCase("body"))) {
            node = node.getNextSibling();
        }
        
        Element body = (Element)node;
        //resolveEncodings(getEncodingsVector(body));
        
        String sourceLocale = ((Element)fileNode).getAttribute("source-language");
        String targetLocale = ((Element)fileNode).getAttribute("target-language");
        if (!sourceLocale.equals("")) {
            language = sourceLocale;
        }
        if (!targetLocale.equals("")) {
            // The target language is the real data. The source is only for reference.
            // We could do verification that all the data is translated the same though.
            language = targetLocale;
        }
		
        // Now do the actual import resource by resource
        NodeList tu_list = body.getElementsByTagName("group");
        int body_nodes_length = body.getChildNodes().getLength();
        NodeList body_list = body.getChildNodes();
        int groupCount = 0, elementCount = 0;
        Node last_group_node = null;
        for (int i=0; i < body_nodes_length; i++) {
            Node body_elem = body_list.item(i);
	        if (body_elem.getNodeType() == Node.ELEMENT_NODE) {
	            if (body_elem.getNodeName().equalsIgnoreCase("group")) {
		            groupCount++;
		            last_group_node = body_elem;
		        }
	            elementCount++;
	        }
        }
        if (elementCount == 1 && groupCount == 1) {
	        // ICU style group where the top group is just the locale.
	        Element localeNode = (Element)last_group_node; 
	        tu_list = last_group_node.getChildNodes();
            String rootGroupName = localeNode.getAttribute("id");
            if (rootGroupName != null && rootGroupName.equals("root")) {
                rootGroupName = "";
            }
            // It's done this way because ICU handles rfc3066bis (the successor of rfc3066)
            // XLIFF requires rfc3066, which doesn't handle scripts.
            language = rootGroupName;
        }
        
        // Add the locale if needed, and normalize it to the correct format.
        Vector localeNames = new Vector();
        char array[] = language.toCharArray();
        for (int k=0; k < array.length; k++) {
            if (array[k] == '-')
                array[k] = '_';
        }
        language = String.valueOf(array);
        localeNames.add(language);
        resolveEncodings(localeNames);
        rbm.getBundle(language).comment = bundleNote;

        for (int i=0; i < tu_list.getLength(); i++) {
            if (!(tu_list.item(i) instanceof Element)) {
                continue;
            }
            Element tu_elem = (Element)tu_list.item(i);
            
            // Get the key value
            String name = tu_elem.getAttribute("id");
            if (name == null || name.length() < 1)
                continue;
            // Get the group if it exists
            String group = null;
            if (tu_elem.getNodeName().equalsIgnoreCase("group")) {
                group = name;
                String groupComment = "";
                NodeList notes_list = tu_elem.getElementsByTagName("note");
                if (notes_list.getLength() > 0) {
    	            Text text_elem = (Text)notes_list.item(0).getChildNodes().item(0);
    	            String value = text_elem.getNodeValue();
    	            if (value != null && value.length() > 0) {
    	            	groupComment = value;
    	            }
                }
                rbm.createGroup(group, groupComment);
                //NodeList group_list = tu_elem.getElementsByTagName("group");
            }
            
            if (group == null || group.length() < 1) {
                group = getDefaultGroup();
                parseTranslationUnit(language, group, tu_elem);
            }
            
            NodeList trans_unit_list = tu_elem.getElementsByTagName("trans-unit");
            // For each trans-unit element
            for (int j=0; j < trans_unit_list.getLength(); j++) {
                parseTranslationUnit(language, group, (Element)trans_unit_list.item(j));
            }
        }
    }

    private void parseTranslationUnit(String language, String group, Element trans_unit_elem) {
        // Get the translation value
    	ElementImpl target_elem = (ElementImpl)trans_unit_elem.getElementsByTagName("target").item(0);
        if (target_elem == null) {
            // This is a template, or a skeleton
            target_elem = (ElementImpl)trans_unit_elem.getElementsByTagName("source").item(0);
        }
        if (target_elem.getLength() < 1)
            return;
        target_elem.normalize();
        NodeList text_list = target_elem.getChildNodes();
        if (text_list.getLength() < 1)
            return;
        Text text_elem = (Text)text_list.item(0);
        String transValue = text_elem.getNodeValue();
        if (transValue == null || transValue.length() < 1)
            return;
        /*NamedNodeMap attribMap = trans_unit_elem.getAttributes();
        for (int k = 0; k < attribMap.getLength(); k++) {
            String attribMapName = attribMap.item(k).getNodeName();
            System.out.println(attribMapName);
        }*/
        String name = trans_unit_elem.getAttribute("id");
        if (name == null || name.length() < 1)
            return;
        // Create the bundle item
        BundleItem item = new BundleItem(null, name, transValue);
        // Get creation, modification values

        String state = trans_unit_elem.getAttribute("state");
        if (state != null && state.length() > 0) {
            item.setTranslated(state.equalsIgnoreCase("translated"));
        }

        String date = trans_unit_elem.getAttribute("date");
        if (date != null && date.length() > 0) {
            item.setModifiedDate(date);
        }

        Element note_elem = (Element)trans_unit_elem.getElementsByTagName("note").item(0);
        if (note_elem != null) {
            NodeList note_list = note_elem.getChildNodes();
            if (note_list.getLength() > 0) {
	            Text note_text_elem = (Text)note_list.item(0);
	            String comment = note_text_elem.getNodeValue();
	            if (comment != null && comment.length() > 0) {
	            	item.setComment(comment);
	            }
            }
        }

        Element prop_group_elem = (Element)trans_unit_elem.getElementsByTagName("prop-group").item(0);
        if (prop_group_elem != null) {
            NodeList prop_list = prop_group_elem.getChildNodes();
        	int propertyLen = prop_list.getLength();
            for (int prop = 0; prop < propertyLen; prop++) {
            	if (prop_list.item(prop) instanceof Element) {
	            	Element property_elem = (Element)prop_list.item(prop);
	            	String propertyType = property_elem.getAttribute("prop-type");
	            	if (propertyType != null) {
			            String value = property_elem.getChildNodes().item(0).getNodeValue();
			            if (value != null && value.length() > 0) {
			            	if (propertyType.equals("creator")) {
				            	item.setCreator(value);
			            	}
			            	else if (propertyType.equals("modifier")) {
				            	item.setModifier(value);
			            	}
			            }
	            	}
            	}
            }
        }

        /*item.setCreatedDate(tuv_elem.getAttribute("creationdate"));
        item.setModifiedDate(tuv_elem.getAttribute("changedate"));
        if (tuv_elem.getAttribute("changeid") != null) item.setModifier(tuv_elem.getAttribute("changeid"));
        if (tuv_elem.getAttribute("creationid") != null) item.setCreator(tuv_elem.getAttribute("creationid"));*/
        // Get properties specified
/*                NodeList prop_list = tuv_elem.getElementsByTagName("prop");
        Hashtable lookups = null;
        for (int k=0; k < prop_list.getLength(); k++) {
            ElementImpl prop_elem = (ElementImpl)prop_list.item(k);
            String type = prop_elem.getAttribute("type");
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
                            if (lookups == null)
                                lookups = new Hashtable();
                            String lkey = text.substring(0,text.indexOf("="));
                            String lvalue = text.substring(text.indexOf("=")+1,text.length());
                            lookups.put(lkey, lvalue);
                        } catch (Exception ex) {
                         	//String out of bounds - Ignore and go on
                        }
                    }
                } else item.setTranslated(getDefaultTranslated());
            }
        }*/
//        if (lookups != null)
//            item.setLookups(lookups);
        importResource(item, language, group);
    }
/*    private Vector getEncodingsVector(ElementImpl body) {
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
    }*/
}
