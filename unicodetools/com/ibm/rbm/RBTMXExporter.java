/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.*;
import javax.swing.*;
import java.util.*;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;

/**
 * This class is a plug-in to RBManager that allows the user to export Resource Bundles
 * along with some of the meta-data associated by RBManager to the TMX specification.
 * For more information on TMX visit the web site <a href="http://www.lisa.org/tmx/">http://www.lisa.org/tmx/</a>
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBTMXExporter extends RBExporter {
    private static final String VERSION = "0.5a";
	
    /**
     * Default constructor for the TMX exporter.
     */
        
    public RBTMXExporter() {
        super();
		
        // Initialize the file chooser if necessary
        if (chooser == null) {
            chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter(){
                public String getDescription() {
                    return "TMX Files";
                }
                public boolean accept(File f) {
                    if (f.isDirectory()) return true;
                    if (f.getName().endsWith(".tmx")) return true;
                    return false;
                }
            });
        } // end if
    }
	
    private String convertToISO(Date d) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        return convertToISO(gc);
    }
	
    private String convertToISO(GregorianCalendar gc) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.valueOf(gc.get(Calendar.YEAR)));
        int month = gc.get(Calendar.MONTH)+1;
        buffer.append(((month < 10) ? "0" : "") + String.valueOf(month));
        int day = gc.get(Calendar.DAY_OF_MONTH);
        buffer.append(((day < 10) ? "0" : "") + String.valueOf(day));
        buffer.append("T");
        int hour = gc.get(Calendar.HOUR_OF_DAY);
        buffer.append(((hour < 10) ? "0" : "") + String.valueOf(hour));
        int minute = gc.get(Calendar.MINUTE);
        buffer.append(((minute < 10) ? "0" : "") + String.valueOf(minute));
        int second = gc.get(Calendar.SECOND);
        buffer.append(((second < 10) ? "0" : "") + String.valueOf(second));
        buffer.append("Z");
        return buffer.toString();
    }
	
    private String convertEncoding(BundleItem item) {
        if (item != null && item.getParentGroup() != null && item.getParentGroup().getParentBundle() != null) {
            String language = item.getParentGroup().getParentBundle().getLanguageEncoding();
            String country = item.getParentGroup().getParentBundle().getCountryEncoding();
            String variant = item.getParentGroup().getParentBundle().getVariantEncoding();
            if (language != null && !language.equals("")) {
                //language = language.toUpperCase();
                if (country != null && !country.equals("")) {
                    //country = country.toUpperCase();
                    if (variant != null && !variant.equals("")) {
                        //variant = variant.toUpperCase();
                        return language + "-" + country + "-" + variant;
                    }
                    return language + "-" + country;
                }
                return language;
            }
        }
        return "";
    }
	
    private void appendTUV(Document xml, Element tu, BundleItem item) {
        Element tuv = xml.createElement("tuv");
        tuv.setAttribute("lang", convertEncoding(item));
        tuv.setAttribute("creationdate",convertToISO(item.getCreatedDate()));
        tuv.setAttribute("creationid",item.getCreator());
        tuv.setAttribute("changedate",convertToISO(item.getModifiedDate()));
        tuv.setAttribute("changeid",item.getModifier());
        item.getComment();
        item.isTranslated();
		
        Element comment_prop = xml.createElement("prop");
        comment_prop.appendChild(xml.createTextNode(item.getComment()));
        comment_prop.setAttribute("type","x-Comment");
        tuv.appendChild(comment_prop);
        
        Element translated_prop = xml.createElement("prop");
        translated_prop.appendChild(xml.createTextNode(String.valueOf(item.isTranslated())));
        translated_prop.setAttribute("type","x-Translated");
        tuv.appendChild(translated_prop);
		
        Hashtable lookups = item.getLookups();
        Enumeration keys = lookups.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = (String)lookups.get(key);
            Element lookup_prop = xml.createElement("prop");
            lookup_prop.appendChild(xml.createTextNode(key + "=" + value));
            lookup_prop.setAttribute("type","x-Lookup");
            tuv.appendChild(lookup_prop);
        }
		
        Element seg = xml.createElement("seg");
        seg.appendChild(xml.createTextNode(item.getTranslation()));
        tuv.appendChild(seg);
		
        tu.appendChild(tuv);
    }
	
    public void export(RBManager rbm) throws IOException {
        if (rbm == null) return;
        // Open the Save Dialog
        int ret_val = chooser.showSaveDialog(null);
        if (ret_val != JFileChooser.APPROVE_OPTION) return;
        // Retrieve basic file information
        File file = chooser.getSelectedFile();                  // The file(s) we will be working with
        File directory = new File(file.getParent());            // The directory we will be writing to
        String base_name = file.getName();                      // The base name of the files we will write
        if (base_name == null || base_name.equals("")) base_name = rbm.getBaseClass();
        if (base_name.endsWith(".tmx")) base_name = base_name.substring(0,base_name.length()-4);
		
        String file_name = base_name + ".tmx";
        
        Vector bundle_v = rbm.getBundles();
        Bundle main_bundle = (Bundle)bundle_v.elementAt(0);
        
        Document xml = new DocumentImpl();
        Element root = xml.createElement("tmx");
        root.setAttribute("version", "1.2");
        xml.appendChild(root);
		
        Element header = xml.createElement("header");
        Element note = xml.createElement("note");
        note.appendChild(xml.createTextNode("This document was created automatically by RBManager"));
        header.appendChild(note);
        header.setAttribute("creationtool", "RBManager");
        header.setAttribute("creationtoolversion", VERSION);
        header.setAttribute("datatype", "PlainText");
        header.setAttribute("segtype", "sentance");
        header.setAttribute("adminlang", "en-us");
        header.setAttribute("srclang", "EN");
        header.setAttribute("o-tmf", "none");
        header.setAttribute("creationdate", convertToISO(new Date()));
        root.appendChild(header);
		
        Element body = xml.createElement("body");
        root.appendChild(body);
		
        Vector group_v = main_bundle.getGroupsAsVector();
        // Loop through each bundle group in main_bundle
        for (int i=0; i < group_v.size(); i++) {
            BundleGroup main_group = (BundleGroup)group_v.elementAt(i);
            // Gather a group of groups of the same name as main_group
            Vector all_groups_v = new Vector();
            for (int j=1; j < bundle_v.size(); j++) {
                Bundle bundle = (Bundle)bundle_v.elementAt(j);
                if (bundle.hasGroup(main_group.getName())) {
                    Vector groups = bundle.getGroupsAsVector();
                    for (int k=0; k < groups.size(); k++) {
                        BundleGroup group = (BundleGroup)groups.elementAt(k);
                        if (group.getName().equals(main_group.getName())) all_groups_v.addElement(group);
                    }
                }
            } // end for - j
            // Loop through each item in main_group
            for (int j=0; j < main_group.getItemCount(); j++) {
                BundleItem main_item = main_group.getBundleItem(j);
                Element tu = xml.createElement("tu");
                tu.setAttribute("tuid",main_item.getKey());
                tu.setAttribute("datatype","Text");
                // Insert the group name for the item
                Element group_prop = xml.createElement("prop");
                group_prop.appendChild(xml.createTextNode(main_group.getName()));
                group_prop.setAttribute("type", "x-Group");
                tu.appendChild(group_prop);
                // Add the main_item to the xml
                appendTUV(xml, tu, main_item);
                // Loop through the rest of the groups of the same name as main_group
                for (int k=0; k < all_groups_v.size(); k++) {
                    BundleGroup group = (BundleGroup)all_groups_v.elementAt(k);
                    // Loop through the items in each group
                    for (int l=0; l < group.getItemCount(); l++) {
                        BundleItem item = group.getBundleItem(l);
                        if (item.getKey().equals(main_item.getKey())) {
                            appendTUV(xml, tu, item);
                            break;
                        }
                    } // end for - l
                } // end for - k
                body.appendChild(tu);
            } // end for - j
        } // end for - i
        FileWriter fw = new FileWriter(new File(directory,file_name));
        OutputFormat of = new OutputFormat(xml);
        of.setIndenting(true);
        of.setEncoding("ISO-8859-1");
        XMLSerializer serializer = new XMLSerializer(fw, of);
        serializer.serialize(xml);
    }
}