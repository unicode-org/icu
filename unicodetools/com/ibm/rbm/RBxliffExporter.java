/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.*;
import java.util.*;
import java.text.*;

import javax.swing.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

/**
 * This class is a plug-in to RBManager that allows the user to export Resource Bundles
 * along with some of the meta-data associated by RBManager to the XLIFF specification.
 * For more information on XLIFF visit the web site
 * <a href="http://www.lisa.org/xliff/">http://www.lisa.org/xliff/</a>
 * 
 * @author George Rhoten
 * @see com.ibm.rbm.RBManager
 */
public class RBxliffExporter extends RBExporter {
    private static final String VERSION = "0.7";
    private static final String XLIFF_DTD = "http://www.oasis-open.org/committees/xliff/documents/xliff.dtd";
    private static final String XLIFF_PUBLIC_NAME = "-//XLIFF//DTD XLIFF//EN";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
    /**
     * Default constructor for the XLIFF exporter.
     */
        
    public RBxliffExporter() {
        super();
		
        // Initialize the file chooser if necessary
        if (chooser == null) {
            chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
                public String getDescription() {
                    return "XLIFF Files";
                }
                public boolean accept(File f) {
                    return (f.isDirectory() || f.getName().endsWith(".xlf"));
                }
            });
        }
    }
	
    private String convertToISO(Date d) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        return convertToISO(gc);
    }
	
    private String convertToISO(GregorianCalendar gc) {
    	dateFormat.setCalendar(gc);
        return dateFormat.format(gc.getTime());
    }
	
    private String getLocale(Bundle item) {
        String language = item.getLanguageEncoding();
        if (language != null && !language.equals("")) {
            //language = language.toUpperCase();
            String country = item.getCountryEncoding();
            if (country != null && !country.equals("")) {
                //country = country.toUpperCase();
                String variant = item.getVariantEncoding();
                if (variant != null && !variant.equals("")) {
                    //variant = variant.toUpperCase();
                    return language + "-" + country + "-" + variant;
                }
                return language + "-" + country;
            }
            return language;
        }
        return "";
    }

    private String getParentLocale(String locale) {
    	
    	int truncIndex = locale.lastIndexOf('-');
    	if (truncIndex > 0) {
    		locale = locale.substring(0, truncIndex);
    	}
    	else {
    		locale = "";
    	}
    	return locale;
    }
	
    private void addTransUnit(Document xml, Element groupElem, BundleItem item, BundleItem parent_item) {
        Element transUnit = xml.createElement("trans-unit");
        transUnit.setAttribute("date",convertToISO(item.getModifiedDate()));
        transUnit.setAttribute("id",item.getKey());
		
        String sourceOrTarget = "target";
        if (parent_item == null) {
        	sourceOrTarget = "source";
        }
        else {
            Element source = xml.createElement("source");
            source.setAttribute("xml:space","preserve");
            source.appendChild(xml.createTextNode(parent_item.getTranslation()));
            transUnit.appendChild(source);
        }
        Element target = xml.createElement(sourceOrTarget);
        target.setAttribute("xml:space","preserve");
    	// This is different from the translate attribute
        if (item.isTranslated()) {
        	// TODO Handle the other states in the future.
        	transUnit.setAttribute("state", "translated");
        }
        target.appendChild(xml.createTextNode(item.getTranslation()));
        transUnit.appendChild(target);
		
        if (item.getComment() != null && item.getComment().length() > 1) {
	        Element comment_prop = xml.createElement("note");
	        comment_prop.setAttribute("xml:space","preserve");
	        comment_prop.appendChild(xml.createTextNode(item.getComment()));
	        transUnit.appendChild(comment_prop);
        }
        
        if ((item.getCreator() != null && item.getCreator().length() > 1)
        	|| (item.getModifier() != null && item.getModifier().length() > 1))
        {
            Element transUnit_prop_group_elem = xml.createElement("prop-group");

            if (item.getCreator() != null && item.getCreator().length() > 1) {
	            Element creator_prop = xml.createElement("prop");
	            creator_prop.setAttribute("prop-type","creator");
	            creator_prop.appendChild(xml.createTextNode(item.getCreator()));
		        transUnit_prop_group_elem.appendChild(creator_prop);
            }
	        
            if (item.getCreator() != null && item.getCreator().length() > 1) {
	            Element created_prop = xml.createElement("prop");
	            created_prop.setAttribute("prop-type","created");
	            created_prop.appendChild(xml.createTextNode(convertToISO(item.getCreatedDate())));
		        transUnit_prop_group_elem.appendChild(created_prop);
            }
	        
        	if (item.getModifier() != null && item.getModifier().length() > 1) {
		        Element modifier_prop = xml.createElement("prop");
		        modifier_prop.setAttribute("prop-type","modifier");
		        modifier_prop.appendChild(xml.createTextNode(item.getModifier()));
		        transUnit_prop_group_elem.appendChild(modifier_prop);
        	}
	        
	        transUnit.appendChild(transUnit_prop_group_elem);
        }

        groupElem.appendChild(transUnit);
    }
	
    public void export(RBManager rbm) throws IOException {
        if (rbm == null)
        	return;
        // Open the Save Dialog
        int ret_val = chooser.showSaveDialog(null);
        if (ret_val != JFileChooser.APPROVE_OPTION)
        	return;
        // Retrieve basic file information
        File file = chooser.getSelectedFile();              // The file(s) we will be working with
        File directory = new File(file.getParent());        // The directory we will be writing to
        String base_name = file.getName();                  // The base name of the files we will write
        if (base_name == null || base_name.equals(""))
        	base_name = rbm.getBaseClass();
        if (base_name.endsWith(".xlf"))
        	base_name = base_name.substring(0,base_name.length()-4);
		
        String file_name = base_name + ".xlf";
        
        Vector bundle_v = rbm.getBundles();
        Enumeration bundleIter = bundle_v.elements();
        while (bundleIter.hasMoreElements()) {
        	exportFile(rbm, directory, base_name, (Bundle)bundleIter.nextElement());
        }
    }
    
    private void addHeaderProperties(Document xml, Element header, Bundle main_bundle) {
        if (main_bundle.comment != null && main_bundle.comment.length() > 0) {
            Element note = xml.createElement("note");
        	header.appendChild(note);
            note.appendChild(xml.createTextNode(main_bundle.comment));
            note.setAttribute("xml:space","preserve");
        }
        if ((main_bundle.name != null && main_bundle.name.length() > 0)
    		|| (main_bundle.manager != null && main_bundle.manager.length() > 0)
        	|| (main_bundle.language != null && main_bundle.language.length() > 0)
			|| (main_bundle.country != null && main_bundle.country.length() > 0)
			|| (main_bundle.variant != null && main_bundle.variant.length() > 0))
        {
            Element prop_group = xml.createElement("prop-group");
        	header.appendChild(prop_group);
            if (main_bundle.name != null && main_bundle.name.length() > 0) {
                Element prop = xml.createElement("prop");
            	header.appendChild(prop);
            	prop.setAttribute("xml:space","preserve");
            	prop.setAttribute("prop-type","name");
            	prop.appendChild(xml.createTextNode(main_bundle.name));
            	prop_group.appendChild(prop);
            }
            if (main_bundle.manager != null && main_bundle.manager.length() > 0) {
                Element prop = xml.createElement("prop");
            	header.appendChild(prop);
            	prop.setAttribute("xml:space","preserve");
            	prop.setAttribute("prop-type","manager");
            	prop.appendChild(xml.createTextNode(main_bundle.manager));
            	prop_group.appendChild(prop);
            }
            if (main_bundle.language != null && main_bundle.language.length() > 0) {
                Element prop = xml.createElement("prop");
            	header.appendChild(prop);
            	prop.setAttribute("xml:space","preserve");
            	prop.setAttribute("prop-type","language");
            	prop.appendChild(xml.createTextNode(main_bundle.language));
            	prop_group.appendChild(prop);
            }
            if (main_bundle.country != null && main_bundle.country.length() > 0) {
                Element prop = xml.createElement("prop");
            	header.appendChild(prop);
            	prop.setAttribute("xml:space","preserve");
            	prop.setAttribute("prop-type","country");
            	prop.appendChild(xml.createTextNode(main_bundle.country));
            	prop_group.appendChild(prop);
            }
            if (main_bundle.variant != null && main_bundle.variant.length() > 0) {
                Element prop = xml.createElement("prop");
            	header.appendChild(prop);
            	prop.setAttribute("xml:space","preserve");
            	prop.setAttribute("prop-type","variant");
            	prop.appendChild(xml.createTextNode(main_bundle.variant));
            	prop_group.appendChild(prop);
            }
        }
    }
    
    private void exportFile(RBManager rbm, File directory, String base_name, Bundle main_bundle)
    	throws IOException
    {
        Bundle parent_bundle = null;
        String parent_bundle_name = null;
        if (!getLocale(main_bundle).equals("")) {
        	// If this isn't the root locale, find the parent
            parent_bundle_name = getParentLocale(getLocale(main_bundle));
	        do {
	        	parent_bundle = rbm.getBundle(parent_bundle_name);
	        	if (parent_bundle != null) {
	        		break;
	        	}
	            parent_bundle_name = getParentLocale(parent_bundle_name);
	        } while (!parent_bundle_name.equals(""));
        }

        
        // Find the implementation
        DocumentBuilder builder;
        try {
        	builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException pce) {
        	throw new IOException(pce.getMessage());
        }
        
        // Create the document
        Document xml = builder.getDOMImplementation().createDocument(null, "xliff", null);
         
        // Fill the document
        Element root = xml.getDocumentElement();
        root.setAttribute("version", "1.1");
        //root.appendChild(root);
        
        Element file_elem = xml.createElement("file");
        String mainLocale = getLocale(main_bundle);
        Bundle parentBundle = null;
        if (mainLocale.equals("")) {
        	file_elem.setAttribute("source-language", getLocale(main_bundle));
        }
        else {
        	file_elem.setAttribute("source-language", parent_bundle_name);
        	file_elem.setAttribute("target-language", getLocale(main_bundle));
        }
        file_elem.setAttribute("datatype", "plaintext");
        file_elem.setAttribute("date", convertToISO(new Date()));
        root.appendChild(file_elem);
		
        Element header = xml.createElement("header");
        Element tool = xml.createElement("tool");
        tool.setAttribute("tool-name", "RBManager");
        tool.setAttribute("tool-id", "RBManager");
        tool.setAttribute("tool-version", VERSION);
        // TODO Add file attribute
        //header.setAttribute("file", "");
        header.appendChild(tool);
        addHeaderProperties(xml, header, main_bundle);
        file_elem.appendChild(header);
		
        Element body = xml.createElement("body");
        file_elem.appendChild(body);
		
        Vector group_v = main_bundle.getGroupsAsVector();
        Vector parent_group_v = null;
        if (parent_bundle != null) {
        	parent_group_v = parent_bundle.getGroupsAsVector();
        }
        // Loop through each bundle group in main_bundle
        for (int i=0; i < group_v.size(); i++) {
            BundleGroup curr_group = (BundleGroup)group_v.elementAt(i);
            BundleGroup parent_group = null;
            if (parent_group_v != null) { 
	            Enumeration parentGroupIter = parent_group_v.elements();
	            
	            while (parentGroupIter.hasMoreElements()) {
	            	BundleGroup groupToFind = (BundleGroup)parentGroupIter.nextElement();
	            	if (groupToFind.getName().equals(curr_group.getName())) {
	            		parent_group = groupToFind;
	            		break;
	            	}
	            }
            }
            Element group_elem = xml.createElement("group");
            group_elem.setAttribute("id", curr_group.getName());
            if (curr_group.getComment() != null && curr_group.getComment().length() > 1) {
    	        Element comment_prop = xml.createElement("note");
    	        comment_prop.setAttribute("xml:space","preserve");
    	        comment_prop.appendChild(xml.createTextNode(curr_group.getComment()));
    	        group_elem.appendChild(comment_prop);
            }
            
            Vector group_items = curr_group.getItemsAsVector();
            for (int j=0; j < group_items.size(); j++) {
            	BundleItem main_item = (BundleItem)group_items.get(j);
            	BundleItem parent_item = null;
            	if (parent_group != null) {
	            	Enumeration parentIter = parent_group.getItemsAsVector().elements();
	            	BundleItem itemToFind = null;
	                while (parentIter.hasMoreElements()) {
	                	itemToFind = (BundleItem)parentIter.nextElement();
	                	if (itemToFind.getKey().equals(main_item.getKey())) {
	                		parent_item = itemToFind;
	                		break;
	                	}
	                }
            	}
                addTransUnit(xml, group_elem, main_item, parent_item);
                //group_elem.appendChild(tu);
            }
            body.appendChild(group_elem);
        } // end for - i
        String suffix = mainLocale;
        if (!suffix.equals("")) {
        	suffix = '_' + suffix;
        }
        char array[] = suffix.toCharArray();
        for (int k=0; k < array.length; k++) {
            if (array[k] == '-')
                array[k] = '_';
        }
        suffix = String.valueOf(array);
        
        // serialize document
        OutputStreamWriter osw = new OutputStreamWriter(
        		new FileOutputStream(new File(directory, base_name + suffix + ".xlf")), "UTF-8");
        try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, XLIFF_DTD);
			transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, XLIFF_PUBLIC_NAME);
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(new DOMSource(xml), new StreamResult(osw));
        }
        catch (TransformerException te) {
        	throw new IOException(te.getMessage());
        }
        
        osw.close();
    }
}
