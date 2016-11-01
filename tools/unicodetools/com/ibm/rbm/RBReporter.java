/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;

import com.ibm.rbm.gui.RBManagerGUI;

/**
 * RBReporter is a fully functional application that runs separate from RBManager.
 * The report produces statistically based reports on specified resource bundles,
 * and it allows the user to set time intervals at which those reports will be
 * generated. For more information on command line arguments and usage see the
 * comments for the main() method.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBReporter extends JFrame {
	
	// ** COMPONENTS **
	JLabel       statusLabel;                       // Indicates if the reported is running
	JButton      statusButton;                      // Button for toggling the reporter on/off
	JLabel       nextReportLabel;                   // Indicates date/time of next report
	JLabel       lastReportLabel;                   // Indicates date/time of last report
	JTextField   bundleField;                       // Indicates input base class file
	JTextField   directoryField;                    // Indicates output directory
	JCheckBox    textCheck;                         // Is text report generated?
	JCheckBox    htmlCheck;                         // Is HTML report generated?
	JCheckBox    xmlCheck;                          // Is XML report generated?
	JCheckBox    scanCheck;                         // Is code scan performed?
	JTextField   textField;                         // Text report file name
	JTextField   htmlField;                         // HTML report file name
	JTextField   xmlField;                          // XML report file name
	JTextField   scanField;                         // XML scanner file location
	JComboBox    textCombo;                         // Text report detail level
	JComboBox    htmlCombo;                         // HTML report detail level
	JComboBox    xmlCombo;                          // XML report detail level
	JRadioButton sequentialRadio;                   // Report at sequential interval?
	JRadioButton definedRadio;                      // Report at defined time?
	JComboBox    valueCombo;                        // Number of units to wait between reports
	JComboBox    unitCombo;                         // Units of time
	JComboBox    hourCombo;                         // Defined time to report -- hours
	JComboBox    minuteCombo;                       // Defined time to report -- minutes
	JComboBox    dayCombo;                          // Defined time to report -- day
	
	// ** File Chooser **
	JFileChooser bundleFileChooser = new JFileChooser();
	JFileChooser directoryFileChooser = new JFileChooser();
	JFileChooser scanFileChooser = new JFileChooser();
	
	// ** DATA **
	Date lastReport = null;
	Date nextReport = null;
	boolean running = false;
    /** For generating a report */
    RBManager rbm;
	
	private RBReporter(boolean makeVisible) {
		try {
			// Get the look and feel from preferences	
			try {
				String laf = Preferences.getPreference("lookandfeel");
				if (!laf.equals(""))
					UIManager.setLookAndFeel(laf);
			} 
			catch (Exception e) { 
			}
			// Get the locale from preferences
			if (!Preferences.getPreference("locale").equals("")) {
				String localeStr = Preferences.getPreference("locale");
				String language = Resources.getLanguage(localeStr);
				String country = Resources.getCountry(localeStr);
				String variant = Resources.getVariant(localeStr);
				if (language == null || language.equals("") || language.length() > 3) language = "en";
				if (country == null) country = new String();
				if (variant == null) Resources.setLocale(new Locale(language, country));
				else Resources.setLocale(new Locale(language, country, variant));
			}
			Resources.initBundle();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		initComponents();
		setVisible(makeVisible);
		Thread reportThread = new Thread(){
			public void run() {
				if (nextReport != null && (nextReport.compareTo(new Date()) <= 0)) {
					try { generateReports(); } catch (IOException ioe) {}
				}
				if (nextReport == null)
				    nextReport = generateNextReportDate();
				updateStatusComponents();
				updateDateFields();
				while (true) {
					if (running && (nextReport.compareTo(new Date()) < 0)) {
						try {generateReports();}
						catch (IOException ioe) {
							JOptionPane.showMessageDialog(null, ioe.getMessage(),
														  Resources.getTranslation("error"),
														  JOptionPane.ERROR_MESSAGE);
						}
					}
					try {
						sleep(1000);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}
				}
			}
		};
		reportThread.start();
	}
	
	// Called when a report should be generated. Does not check if it should be generated
	private void generateReports() throws IOException {
		File baseFile = new File(bundleField.getText());
		if (baseFile == null || !baseFile.isFile())
		    throw new IOException("Specified input file is unusable");
		File directory = new File(directoryField.getText());
		rbm = new RBManager(baseFile);
		
		if (rbm == null)
		    throw new IOException("Unable to load the resource bundle file");
		if (directory == null || !directory.isDirectory())
		    throw new IOException("Specified output directory is unusable");
		RBReporterScanner scanner = null;
		if (scanCheck.isSelected()) {
			scanner = new RBReporterScanner((Bundle)rbm.getBundles().elementAt(0),
										    new File(scanField.getText()));
			scanner.performScan();
		}
		if (textCheck.isSelected()) {
			File textFile = new File(directory, textField.getText());
			String textReport = getAllLanguageReports(textCombo.getSelectedIndex() == 0);
			if (scanCheck.isSelected()) {
				// Add file scan information
				StringBuffer buffer = new StringBuffer();
				buffer.append("\n\nCode Scan Results:\n\n");
				buffer.append("\n\tNumber of unique resources found: " + scanner.getNumberResourcesFound());
				buffer.append("\n\tNumber of resources missing from bundle: " + scanner.getNumberMissingResources());
				// Missing resources from the bundle
				buffer.append("\n\tMissing Resources: ");
				Vector v = scanner.getMissingResources();
				for (int i=0; i < v.size(); i++) {
					ScanResult result = (ScanResult)v.elementAt(i);
					if (textCombo.getSelectedIndex() == 0) {
						buffer.append("\n\t\t" + result.getName() + " (" + result.getOccurances().size() + " Occurances)");
						buffer.append("\n\t\t\t" + result.getOccurances());
					} else {
						buffer.append((i==0 ? "" : ", ") + result.getName() + " (" + result.getOccurances().size() + " Occurances)");
					}
				}
				// Bundle resources not found in the code
				buffer.append("\n\tNumber of potentially unused resources in bundle: " + scanner.getNumberUnusedResources());
				v = scanner.getUnusedResources();
				for (int i=0; i < v.size(); i++) {
					ScanResult result = (ScanResult)v.elementAt(i);
					if (textCombo.getSelectedIndex() == 0) {
						buffer.append("\n\t\t" + result.getName() + " (Group: " + result.getGroupName() + ")");
					} else {
						buffer.append((i==0 ? "" : ", ") + result.getName());
					}
				}
				
				textReport = textReport + buffer.toString();
			}
			FileWriter fw = new FileWriter(textFile);
			fw.write(textReport);
			fw.flush();
			fw.close();
		}
		if (htmlCheck.isSelected()) {
			File htmlFile = new File(directory, htmlField.getText());
			Document htmlReport = getHTMLReportz(htmlCombo.getSelectedIndex() == 0);
			if (scanCheck.isSelected()) {
				// Add file scan information
				Element html_elem = htmlReport.getDocumentElement();
				NodeList nl = html_elem.getElementsByTagName("BODY");
				Element body_elem = (Element)nl.item(0);
				Element h2_elem = htmlReport.createElement("H2");
				Text    h2_text = htmlReport.createTextNode("Code Scan Results");
				Element block_elem = htmlReport.createElement("BLOCKQUOTE");
				Element p1_elem = htmlReport.createElement("P");
				Element p2_elem = htmlReport.createElement("P");
				Element p3_elem = htmlReport.createElement("P");
				Text    p1_text = htmlReport.createTextNode("Number of unique resources found: " +
															scanner.getNumberMissingResources());
				Text    p2_text = htmlReport.createTextNode("Number of resources missing from bundle: " +
															scanner.getNumberMissingResources());
				Text    p3_text = htmlReport.createTextNode("Number of potentially unused resources in bundle: " +
															scanner.getNumberUnusedResources());
				
				h2_elem.appendChild(h2_text);
				p1_elem.appendChild(p1_text);
				p2_elem.appendChild(p2_text);
				p3_elem.appendChild(p3_text);
				block_elem.appendChild(p1_elem);
				block_elem.appendChild(p2_elem);
				block_elem.appendChild(p3_elem);
				body_elem.appendChild(h2_elem);
				body_elem.appendChild(block_elem);
				
				// Missing resources from the bundle
				Text   missing_text = null;
				Vector v = scanner.getMissingResources();
				if (htmlCombo.getSelectedIndex() == 0) {
					Element ul_elem = htmlReport.createElement("UL");
					missing_text = htmlReport.createTextNode("Missing Resources:");
					ul_elem.appendChild(missing_text);
					for (int i=0; i < v.size(); i++) {
						ScanResult result = (ScanResult)v.elementAt(i);
						Element li_elem = htmlReport.createElement("LI");
						Element br_elem = htmlReport.createElement("BR");
						Text    t1_text = htmlReport.createTextNode(result.getName() + " (" +
																				  result.getOccurances().size() + " Occurances)");
						Text    t2_text = htmlReport.createTextNode(result.getOccurances().toString());
						li_elem.appendChild(t1_text);
						li_elem.appendChild(br_elem);
						li_elem.appendChild(t2_text);
						ul_elem.appendChild(li_elem);
					}
					p2_elem.appendChild(ul_elem);
				} else {
					StringBuffer buffer = new StringBuffer();
					buffer.append("Missing Resources: ");
					for (int i=0; i < v.size(); i++) {
						ScanResult result = (ScanResult)v.elementAt(i);
						buffer.append((i==0 ? "" : ", ") + result.getName() + " (" + result.getOccurances().size() + " Occurances)");
					}
					missing_text = htmlReport.createTextNode(buffer.toString());
					Element br_elem = htmlReport.createElement("BR");
					p2_elem.appendChild(br_elem);
					p2_elem.appendChild(missing_text);
				}
				// Bundle resources not found in the code
				Text   unused_text = null;
				v = scanner.getUnusedResources();
				if (htmlCombo.getSelectedIndex() == 0) {
					Element ul_elem = htmlReport.createElement("UL");
					unused_text = htmlReport.createTextNode("Unused Resources:");
					ul_elem.appendChild(unused_text);
					for (int i=0; i < v.size(); i++) {
						ScanResult result = (ScanResult)v.elementAt(i);
						Element li_elem = htmlReport.createElement("LI");
						Text    t1_text = htmlReport.createTextNode(result.getName() + " (Group: " +
																				  result.getGroupName() + ")");
						li_elem.appendChild(t1_text);
						ul_elem.appendChild(li_elem);
					}
					p3_elem.appendChild(ul_elem);
				} else {
					StringBuffer buffer = new StringBuffer();
					buffer.append("Unused Resources: ");
					for (int i=0; i < v.size(); i++) {
						ScanResult result = (ScanResult)v.elementAt(i);
						buffer.append((i==0 ? "" : ", ") + result.getName());
					}
					unused_text = htmlReport.createTextNode(buffer.toString());
					Element br_elem = htmlReport.createElement("BR");
					p3_elem.appendChild(br_elem);
					p3_elem.appendChild(unused_text);
				}
			}
			FileWriter fw = new FileWriter(htmlFile);
			OutputFormat of = new OutputFormat(htmlReport);
			of.setIndenting(true);
			of.setEncoding("ISO-8859-1");
			HTMLSerializer serializer = new HTMLSerializer(fw, of);
			serializer.serialize(htmlReport);
		}
		if (xmlCheck.isSelected()) {
			File xmlFile = new File(directory, xmlField.getText());
			Document xmlReport = getXMLReportz(xmlCombo.getSelectedIndex() == 0);
			if (scanCheck.isSelected()) {
				// Add file scan information
				Element root = xmlReport.getDocumentElement();
				Element code_scan_elem = xmlReport.createElement("CODE_SCAN");
				Element unique_elem = xmlReport.createElement("UNIQUE_RESOURCES");
				Element missing_elem = xmlReport.createElement("MISSING_RESOURCES");
				Element unused_elem = xmlReport.createElement("UNUSED_RESOURCES");
				Element unique_total_elem = xmlReport.createElement("TOTAL");
				Element missing_total_elem = xmlReport.createElement("TOTAL");
				Element unused_total_elem = xmlReport.createElement("TOTAL");
				Text    unique_total_text = xmlReport.createTextNode(String.valueOf(scanner.getNumberMissingResources()));
				Text    missing_total_text = xmlReport.createTextNode(String.valueOf(scanner.getNumberMissingResources()));
				Text    unused_total_text = xmlReport.createTextNode(String.valueOf(scanner.getNumberUnusedResources()));
				
				unique_total_elem.appendChild(unique_total_text);
				missing_total_elem.appendChild(missing_total_text);
				unused_total_elem.appendChild(unused_total_text);
				unique_elem.appendChild(unique_total_elem);
				missing_elem.appendChild(missing_total_elem);
				unused_elem.appendChild(unused_total_elem);
				code_scan_elem.appendChild(unique_elem);
				code_scan_elem.appendChild(missing_elem);
				code_scan_elem.appendChild(unused_elem);
				root.appendChild(code_scan_elem);
				// Missing resources from the bundle
				Vector v = scanner.getMissingResources();
				for (int i=0; i < v.size(); i++) {
					ScanResult result = (ScanResult)v.elementAt(i);
					Element item_elem = xmlReport.createElement("RESOURCE");
					item_elem.setAttribute("NAME",result.getName());
					if (xmlCombo.getSelectedIndex() == 0) {
						Vector occ_v = result.getOccurances();
						for (int j=0; j < occ_v.size(); j++) {
							Occurance occ = (Occurance)occ_v.elementAt(j);
							Element occ_elem = xmlReport.createElement("OCCURANCE");
							occ_elem.setAttribute("FILE_NAME", occ.getFileName());
							occ_elem.setAttribute("FILE_PATH", occ.getFilePath());
							occ_elem.setAttribute("LINE_NUMBER", String.valueOf(occ.getLineNumber()));
							item_elem.appendChild(occ_elem);
						}
					}
					missing_elem.appendChild(item_elem);
				}
				// Bundle resources not found in the code
				v = scanner.getUnusedResources();
				for (int i=0; i < v.size(); i++) {
					ScanResult result = (ScanResult)v.elementAt(i);
					Element item_elem = xmlReport.createElement("RESOURCE");
					item_elem.setAttribute("NAME",result.getName());
					item_elem.setAttribute("GROUP",result.getGroupName());
					unused_elem.appendChild(item_elem);
				}
			}
			FileWriter fw = new FileWriter(xmlFile);
			OutputFormat of = new OutputFormat(xmlReport);
			of.setIndenting(true);
			of.setEncoding("ISO-8859-1");
			XMLSerializer serializer = new XMLSerializer(fw, of);
			serializer.serialize(xmlReport);
		}
		
		lastReport = new Date();
		nextReport = generateNextReportDate();
		updateDateFields();
		if (!isVisible()) {
			System.out.println("RBReporter: Generated report at " + lastReport.toString());
			System.out.println("RBReporter: Next report at " + nextReport.toString());
		}
	}
	
	// Assumes the last report was just generated, and computes the next report time accordingly
	private Date generateNextReportDate() {
		Date retDate = null;
		GregorianCalendar now = new GregorianCalendar();
		if (sequentialRadio.isSelected()) {
			int value = Integer.parseInt(valueCombo.getSelectedItem().toString());
			if (unitCombo.getSelectedIndex() == 0) now.add(Calendar.MINUTE, value);
			else if (unitCombo.getSelectedIndex() == 1) now.add(Calendar.HOUR, value);
			else if (unitCombo.getSelectedIndex() == 2) now.add(Calendar.DATE, value);
			retDate = now.getTime();
		} else if (definedRadio.isSelected()) {
			int hour = Integer.parseInt(hourCombo.getSelectedItem().toString());
			int minute = Integer.parseInt(minuteCombo.getSelectedItem().toString());
			int day = dayCombo.getSelectedIndex();
			
			GregorianCalendar then = new GregorianCalendar();
			then.set(Calendar.HOUR, hour);
			then.set(Calendar.MINUTE, minute);
			then.set(Calendar.SECOND, 0);
			
			if (then.getTime().compareTo(now.getTime()) <= 0) then.add(Calendar.DATE, 1);
			if (day > 0 && day <= 7) {
				// Make sure we are at the right day
				boolean rightDay = false;
				while (!rightDay) {
					int weekDay = then.get(Calendar.DAY_OF_WEEK);
					if ((day == 1 && weekDay == Calendar.MONDAY) ||
						(day == 2 && weekDay == Calendar.TUESDAY) ||
						(day == 3 && weekDay == Calendar.WEDNESDAY) ||
						(day == 4 && weekDay == Calendar.THURSDAY) ||
						(day == 5 && weekDay == Calendar.FRIDAY) ||
						(day == 6 && weekDay == Calendar.SATURDAY) ||
						(day == 7 && weekDay == Calendar.SUNDAY)) rightDay = true;
					else then.add(Calendar.DATE, 1);
				}
			}
			retDate = then.getTime();
		}
		RBManagerGUI.debugMsg("Next Date: " + retDate.toString());
		return retDate;
	}

    /**
     * Returns a string based text report about all of the language files on record
     */
    public String getAllLanguageReports(boolean detailed) {
        String retStr = new String();
        retStr =    "Resource Bundle Report: " + rbm.getBaseClass();
        retStr += "\nReport Generated:       " + (new Date()).toString() + "\n\n";
        Vector bundles = rbm.getBundles();
        for (int i=0; i < bundles.size(); i++) {
            retStr += getLanguageReport(detailed, (Bundle)bundles.elementAt(i));	
        }
        return retStr;
    }
	
    private String getLanguageReport(boolean detailed, Bundle dict) {
        if (dict == null) return "";
        String retStr = new String();
        retStr += "\nLanguage: " + (dict.language == null ? dict.encoding : dict.language);
        retStr += (dict.country == null ? "" : " - Country: " + dict.country);
        retStr += (dict.variant == null ? "" : " - Variant: " + dict.variant);
        retStr += "\n";
        retStr += "  Number of NLS items in the file: " + dict.allItems.size() + "\n";
		
        int untranslated = 0;
        String untransStr = new String();
        Enumeration items = dict.allItems.elements();
        while (items.hasMoreElements()) {
            BundleItem tempItem = (BundleItem)items.nextElement();
            if (tempItem.isTranslated()) continue;
            untranslated++;
            untransStr += " " + tempItem.getKey();
        }
        retStr += "  Number of NLS items not translated: " + untranslated;
        if (detailed) {
            retStr += "\n  Untranslated NLS keys: " + untransStr;	
        }
		
        return retStr;
    }
    
    /**
     * Returns an XHTML formatted report on the status of the currently opened resource bundle
     */
    public Document getHTMLReportz(boolean detailed) {
        Document html = new DocumentImpl();
        Element root = html.createElement("HTML");
        html.appendChild(root);
        Element head_elem = html.createElement("HEAD");
        Element title_elem = html.createElement("TITLE");
        Text    title_text = html.createTextNode("Resource Bundle Report - " + rbm.getBaseClass());
        Element body_elem = html.createElement("BODY");
        Element center1_elem = html.createElement("CENTER");
        Element h1_elem = html.createElement("H1");
        Element center2_elem = html.createElement("CENTER");
        Element h3_elem = html.createElement("H1");
        Text    title1_text = html.createTextNode("Resource Bundle Report: " + rbm.getBaseClass());
        Text    title2_text = html.createTextNode("Report Generated: " + (new Date()).toString());
        Vector bundles = rbm.getBundles();
		
        title_elem.appendChild(title_text);
        head_elem.appendChild(title_elem);
        h1_elem.appendChild(title1_text);
        h3_elem.appendChild(title2_text);
        center1_elem.appendChild(h1_elem);
        center2_elem.appendChild(h3_elem);
        body_elem.appendChild(center1_elem);
        body_elem.appendChild(center2_elem);
        root.appendChild(head_elem);
        root.appendChild(body_elem);
        
        for (int i=0; i < bundles.size(); i++) {
            getHTMLLanguageReportz(html, body_elem, detailed, (Bundle)bundles.elementAt(i));
        }
            
        return html;
    }
    
    /**
     * Returns a HTML report as a String object on the status of the currently opened resource bundle
     */
    public String getHTMLReport(boolean detailed) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<HTML>\n<HEAD><TITLE>Resource Bundle Report - " + rbm.getBaseClass() + "</TITLE></HEAD>\n<BODY>\n");
        buffer.append("<CENTER><H1>Resource Bundle Report: " + rbm.getBaseClass() + "</H1></CENTER>\n");
        buffer.append("<CENTER><H3>Report Generated: " + (new Date()).toString() + "</H3></CENTER>\n");
		
        Vector bundles = rbm.getBundles();
        for (int i=0; i < bundles.size(); i++) {
            buffer.append(getHTMLLanguageReport(detailed, (Bundle)bundles.elementAt(i)));	
        }
		
        buffer.append("</BODY>\n</HTML>");
        return buffer.toString();
    }
	
    private void getHTMLLanguageReportz(Document html, Element body_elem, boolean detailed, Bundle dict) {
        Element h2_elem = html.createElement("H2");
        Text    h2_text = html.createTextNode("Language: " + (dict.language == null ? dict.encoding : dict.language) +
                          (dict.country == null ? "" : " - Country: " + dict.country) +
                          (dict.variant == null ? "" : " - Variant: " + dict.variant));
        Element block_elem = html.createElement("BLOCKQUOTE");
        Element p_elem = html.createElement("P");
        Text    p_text = html.createTextNode("Number of NLS items in the file: " +
                         String.valueOf(dict.allItems.size()));
        Element ul_elem = html.createElement("UL");
        Text    ul_text = html.createTextNode("Untranslated NLS keys:");
		
        h2_elem.appendChild(h2_text);
        p_elem.appendChild(p_text);
        ul_elem.appendChild(ul_text);
        block_elem.appendChild(p_elem);
        body_elem.appendChild(h2_elem);
        body_elem.appendChild(block_elem);
		
        int untranslated = 0;
        Enumeration items = dict.allItems.elements();
        while (items.hasMoreElements()) {
            BundleItem tempItem = (BundleItem)items.nextElement();
            if (tempItem.isTranslated()) continue;
            untranslated++;
            if (detailed) {
                Element li_elem = html.createElement("LI");
                Text    li_text = html.createTextNode(tempItem.getKey());
                li_elem.appendChild(li_text);
                ul_elem.appendChild(li_elem);
            }
        }
        Element p2_elem = html.createElement("P");
        Text    p2_text = html.createTextNode("Number of NLS items not translated: " +
                          String.valueOf(untranslated));
        p2_elem.appendChild(p2_text);
        block_elem.appendChild(p2_elem);
        if (detailed) block_elem.appendChild(ul_elem);
    }
	
    private String getHTMLLanguageReport(boolean detailed, Bundle dict) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("\n<H2>Language: " + (dict.language == null ? dict.encoding : dict.language));
        buffer.append(dict.country == null ? "" : " - Country: " + dict.country);
        buffer.append(dict.variant == null ? "" : " - Variant: " + dict.variant);
        buffer.append("</H2>\n");
        buffer.append("<BLOCKQUOTE>\n");
        
        buffer.append("<P>Number of NLS items in the file: " + String.valueOf(dict.allItems.size()) + "</P>\n");
        int untranslated = 0;
        Enumeration items = dict.allItems.elements();
        StringBuffer innerBuffer = new StringBuffer();
        while (items.hasMoreElements()) {
            BundleItem tempItem = (BundleItem)items.nextElement();
            if (tempItem.isTranslated()) continue;
            untranslated++;
            innerBuffer.append("<LI>" + tempItem.getKey() + "</LI>\n");
        }
        buffer.append("<P>Number of NLS items not translated: " + String.valueOf(untranslated) + "</P>\n");
        if (detailed) {
            buffer.append("<UL>Untranslated NLS keys:\n");
            buffer.append(innerBuffer.toString());
            buffer.append("</UL>\n");
        }
		
        buffer.append("</BLOCKQUOTE>\n");
        return buffer.toString();
    }

    /**
     * Returns an XML formatted report on the status of the currently open resource bundle
     */ 
	
    public Document getXMLReportz(boolean detailed) {
        Document xml = new DocumentImpl();
        Element root = xml.createElement("REPORT");
        root.setAttribute("BASECLASS", rbm.getBaseClass());
        root.setAttribute("DATE", (new Date()).toString());
        xml.appendChild(root);

        Vector bundles = rbm.getBundles();
        for (int i=0; i < bundles.size(); i++) {
            root.appendChild(getXMLLanguageReportz(xml, detailed, (Bundle)bundles.elementAt(i)));
        }
        return xml;
    }
    
    /**
     * Returns an XML formatted report as a String object on the status of the currently open resource bundle
     */
    
    public String getXMLReport(boolean detailed) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\"?>\n");
        buffer.append("<REPORT BASECLASS=\"" + rbm.getBaseClass() + "\" DATE=\"" + (new Date()).toString() + "\">\n");

        Vector bundles = rbm.getBundles();
        for (int i=0; i < bundles.size(); i++) {
            buffer.append(getXMLLanguageReport(detailed, (Bundle)bundles.elementAt(i)));	
        }
        buffer.append("</REPORT>");
        return buffer.toString();
    }
	
    private Element getXMLLanguageReportz(Document xml, boolean detailed, Bundle dict) {
        Element lang_report_elem = xml.createElement("LANGUAGE_REPORT");
        Element locale_elem = xml.createElement("LOCALE");
        locale_elem.setAttribute("LANGUAGE", (dict.language == null ? dict.encoding : dict.language));
        locale_elem.setAttribute("COUNTRY", (dict.country == null ? "" : dict.country));
        locale_elem.setAttribute("VARIANT", (dict.variant == null ? "" : dict.variant));
        Element nls_total_elem = xml.createElement("NLS_TOTAL");
        Text    nls_total_text = xml.createTextNode(String.valueOf(dict.allItems.size()));
        Element untranslated_total_elem = xml.createElement("UNTRANSLATED_TOTAL");
        Element untranslated_elem = xml.createElement("UNTRANSLATED");
        
        nls_total_elem.appendChild(nls_total_text);
        lang_report_elem.appendChild(locale_elem);
        lang_report_elem.appendChild(nls_total_elem);
        lang_report_elem.appendChild(untranslated_total_elem);
        if (detailed) lang_report_elem.appendChild(untranslated_elem);
		
        int untranslated = 0;
        Enumeration items = dict.allItems.elements();
        while (items.hasMoreElements()) {
            BundleItem tempItem = (BundleItem)items.nextElement();
            if (tempItem.isTranslated()) continue;
            untranslated++;
            Element resource_elem = xml.createElement("RESOURCEKEY");
            Text    resource_text = xml.createTextNode(tempItem.getKey());
            resource_elem.appendChild(resource_text);
            untranslated_elem.appendChild(resource_elem);
        }
        Text untranslated_total_text = xml.createTextNode(String.valueOf(untranslated));
        untranslated_total_elem.appendChild(untranslated_total_text);
		
        return lang_report_elem;
    }
	
    private String getXMLLanguageReport(boolean detailed, Bundle dict) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<LANGUAGE_REPORT>\n");
        
        buffer.append("\n\t<LOCALE LANGUAGE=\"" + (dict.language == null ? dict.encoding : dict.language));
        buffer.append("\" COUNTRY=\"" + (dict.country == null ? "" : dict.country));
        buffer.append("\" VARIANT=\"" + (dict.variant == null ? "" : dict.variant) + "\"/>\n");
		
        buffer.append("\t<NLS_TOTAL>" + String.valueOf(dict.allItems.size()) + "</NLS_TOTAL>\n");
        int untranslated = 0;
        Enumeration items = dict.allItems.elements();
        StringBuffer innerBuffer = new StringBuffer();
        while (items.hasMoreElements()) {
            BundleItem tempItem = (BundleItem)items.nextElement();
            if (tempItem.isTranslated()) continue;
            untranslated++;
            innerBuffer.append("\t\t<RESOURCEKEY>" + tempItem.getKey() + "</RESOURCEKEY>\n");
        }
        buffer.append("\t<UNTRANSLATED_TOTAL>" + String.valueOf(untranslated) + "</UNTRANSLATED_TOTAL>\n");
        if (detailed) {
            buffer.append("\t<UNTRANSLATED>\n");
            buffer.append(innerBuffer.toString());
            buffer.append("\t</UNTRANSLATED>\n");
        }
		
        buffer.append("</LANGUAGE_REPORT>\n");
        return buffer.toString();
    }
	
	private void updateDateFields() {
		if (nextReport == null) nextReportLabel.setText(Resources.getTranslation("reporter_next_report", "--"));
		else nextReportLabel.setText(Resources.getTranslation("reporter_next_report", nextReport.toString()));
		if (lastReport == null) lastReportLabel.setText(Resources.getTranslation("reporter_last_report", "--"));
		else lastReportLabel.setText(Resources.getTranslation("reporter_last_report", lastReport.toString()));
	}
	
	private void updateStatusComponents() {
		if (running) {
			statusLabel.setText(Resources.getTranslation("reporter_status_running"));
			statusLabel.setForeground(Color.green);
			statusButton.setText(Resources.getTranslation("reporter_button_stop"));
		} else {
			statusLabel.setText(Resources.getTranslation("reporter_status_stopped"));
			statusLabel.setForeground(Color.red);
			statusButton.setText(Resources.getTranslation("reporter_button_start"));
		}
	}
	
	private void setComponentsToDefaults() {
		if ((running && Preferences.getPreference("reporter_enabled").equals("No")) ||
			(!running && Preferences.getPreference("reporter_enabled").equals("Yes"))) toggleStatus();
		if (Preferences.getPreference("reporter_format_text_enabled") != null)
			textCheck.setSelected(Preferences.getPreference("reporter_format_text_enabled").equals("Yes"));
		if (Preferences.getPreference("reporter_format_html_enabled") != null)
			htmlCheck.setSelected(Preferences.getPreference("reporter_format_html_enabled").equals("Yes"));
		if (Preferences.getPreference("reporter_format_xml_enabled") != null)
				xmlCheck.setSelected(Preferences.getPreference("reporter_format_xml_enabled").equals("Yes"));
		if (Preferences.getPreference("reporter_format_text_file") != null &&
			!Preferences.getPreference("reporter_format_text_file").equals(""))
			textField.setText(Preferences.getPreference("reporter_format_text_file"));
		if (Preferences.getPreference("reporter_format_html_file") != null &&
			!Preferences.getPreference("reporter_format_html_file").equals(""))
			htmlField.setText(Preferences.getPreference("reporter_format_html_file"));
		if (Preferences.getPreference("reporter_format_xml_file") != null &&
			!Preferences.getPreference("reporter_format_xml_file").equals(""))
			xmlField.setText(Preferences.getPreference("reporter_format_xml_file"));
		if (Preferences.getPreference("reporter_format_text_detail") != null &&
			!Preferences.getPreference("reporter_format_text_detail").equals(""))
			selectComboValue(textCombo, Preferences.getPreference("reporter_format_text_detail"));
		if (Preferences.getPreference("reporter_format_html_detail") != null &&
			!Preferences.getPreference("reporter_format_html_detail").equals(""))
			selectComboValue(htmlCombo, Preferences.getPreference("reporter_format_html_detail"));
		if (Preferences.getPreference("reporter_format_xml_detail") != null &&
			!Preferences.getPreference("reporter_format_xml_detail").equals(""))
			selectComboValue(xmlCombo, Preferences.getPreference("reporter_format_xml_detail"));
		if (Preferences.getPreference("reporter_interval").equals("Sequential"))
			sequentialRadio.setSelected(true);
		else definedRadio.setSelected(true);
		if (Preferences.getPreference("reporter_interval_sequential_value") != null &&
			!Preferences.getPreference("reporter_interval_sequential_value").equals(""))
			selectComboValue(valueCombo, Preferences.getPreference("reporter_interval_sequential_value"));
		if (Preferences.getPreference("reporter_interval_sequential_units") != null &&
			!Preferences.getPreference("reporter_interval_sequential_units").equals(""))
			selectComboValue(valueCombo, Preferences.getPreference("reporter_interval_sequential_units"));
		if (Preferences.getPreference("reporter_interval_defined_hour") != null &&
			!Preferences.getPreference("reporter_interval_defined_hour").equals(""))
			selectComboValue(hourCombo, Preferences.getPreference("reporter_interval_defined_hour"));
		if (Preferences.getPreference("reporter_interval_defined_day") != null &&
			!Preferences.getPreference("reporter_interval_defined_day").equals(""))
			selectComboValue(dayCombo, Preferences.getPreference("reporter_interval_defined_day"));
		if (Preferences.getPreference("reporter_interval_defined_minute") != null &&
			!Preferences.getPreference("reporter_interval_defined_minute").equals(""))
			selectComboValue(minuteCombo, Preferences.getPreference("reporter_interval_defined_minute"));
		if (Preferences.getPreference("reporter_scan_file") != null &&
			!Preferences.getPreference("reporter_scan_file").equals(""))
			scanField.setText(Preferences.getPreference("reporter_scan_file"));
		if (Preferences.getPreference("reporter_perform_scan") != null)
			scanCheck.setSelected(Preferences.getPreference("reporter_perform_scan").equals("Yes"));
	}
	
	private static void selectComboValue(JComboBox box, String value) {
		for (int i=0; i < box.getItemCount(); i++) {
			if (box.getItemAt(i).toString().equals(value)) {
				box.setSelectedIndex(i);
				break;
			}
		}
	}
	
	private void saveDefaults() {
		// Save format options
		Preferences.setPreference("reporter_format_text_enabled", (textCheck.isSelected() ? "Yes" : "No"));
		Preferences.setPreference("reporter_format_text_file", textField.getText());
		Preferences.setPreference("reporter_format_text_detail", textCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_format_html_enabled", (htmlCheck.isSelected() ? "Yes" : "No"));
		Preferences.setPreference("reporter_format_html_file", htmlField.getText());
		Preferences.setPreference("reporter_format_html_detail", htmlCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_format_xml_enabled", (xmlCheck.isSelected() ? "Yes" : "No"));
		Preferences.setPreference("reporter_format_xml_file", xmlField.getText());
		Preferences.setPreference("reporter_format_xml_detail", xmlCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_scan_file", scanField.getText());
		Preferences.setPreference("reporter_perform_scan", (scanCheck.isSelected() ? "Yes" : "No"));
		// Save interval options
		Preferences.setPreference("reporter_interval", (sequentialRadio.isSelected() ? "Sequential" : "Defined"));
		Preferences.setPreference("reporter_interval_sequential_value", valueCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_interval_sequential_units", unitCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_interval_defined_hour", hourCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_interval_defined_minute", minuteCombo.getSelectedItem().toString());
		Preferences.setPreference("reporter_interval_defined_day", dayCombo.getSelectedItem().toString());
		// Save system options
		Preferences.setPreference("reporter_enabled", (running ? "Yes" : "No"));
		// Write the preferences
		try {
			Preferences.savePreferences();
		} catch (IOException ioe) {
			// TODO: Warn of error through JOptionPane
			ioe.printStackTrace();
		}
	}
	
	private void toggleStatus() {
		if (running) {
			running = false;
		} else {
			running = true;
		}
		updateStatusComponents();
	}
	
	private void initComponents() {
		
		// File choosers
		bundleFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".properties"))) return false;
				if (name.indexOf("_") > 0) return false;
				return true;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_file_filter_description");
			}
		});
		bundleFileChooser.setSelectedFile(new File(Preferences.getPreference("reporter_base_class_file")));
		
		directoryFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				return false;
			}
	
			public String getDescription() {
				return Resources.getTranslation("directory");
			}
		});
		directoryFileChooser.setSelectedFile(new File(Preferences.getPreference("reporter_output_directory")));
		
		scanFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				if (f.getName().endsWith(".xml")) return true;
				return false;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_file_filter_description_scan");
			}
		});
		scanFileChooser.setSelectedFile(new File(Preferences.getPreference("reporter_scan_file")));
		
		// New top level components
		JPanel statusPanel = new JPanel();
		JPanel intervalPanel = new JPanel();
		JPanel optionsPanel = new JPanel();
		JPanel formatPanel = new JPanel();
		Box mainBox = new Box(BoxLayout.Y_AXIS);
		int width = 600;
		int height = 600;
		int compHeight = 20;
		Dimension mainDim = new Dimension(width,height);
		
		statusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															   Resources.getTranslation("reporter_panel_status")));
		intervalPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															   Resources.getTranslation("reporter_panel_interval")));
		optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															   Resources.getTranslation("reporter_panel_options")));
		formatPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															   Resources.getTranslation("reporter_panel_output_format")));
		
		// ** STATUS PANEL SETUP **
		JButton nowButton = new JButton(Resources.getTranslation("reporter_button_now"));
		Box     statusBox = new Box(BoxLayout.Y_AXIS);
		JPanel  statusPanel1 = new JPanel();
		JPanel  statusPanel2 = new JPanel();
		JPanel  statusPanel3 = new JPanel();
		JPanel  statusPanel4 = new JPanel();
		statusButton = new JButton(Resources.getTranslation("reporter_button_start"));
		statusLabel = new JLabel(Resources.getTranslation("reporter_status_stopped"));
		nextReportLabel = new JLabel(Resources.getTranslation("reporter_next_report", "--"));
		lastReportLabel = new JLabel(Resources.getTranslation("reporter_last_report", "--"));
		statusLabel.setFont(new Font("serif",Font.BOLD,14));
		statusLabel.setForeground(Color.red);
		statusPanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		statusPanel3.setLayout(new FlowLayout(FlowLayout.LEFT));
		statusPanel.setLayout(new BorderLayout());
		
		nowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					generateReports();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, e.getMessage(), Resources.getTranslation("error"),
												  JOptionPane.ERROR_MESSAGE);
					RBManagerGUI.debugMsg(e.toString());
					if (RBManagerGUI.debug) e.printStackTrace(System.err);
				}
			}
		});
		
		statusButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				toggleStatus();
			}
		});
		
		statusPanel1.add(statusLabel);
		statusPanel2.add(nextReportLabel);
		statusPanel3.add(lastReportLabel);
		statusPanel4.add(nowButton);
		statusPanel4.add(Box.createHorizontalStrut(7));
		statusPanel4.add(statusButton);
		statusBox.add(statusPanel1);
		statusBox.add(Box.createVerticalStrut(7));
		//statusBox.add(Box.createHorizontalGlue());
		statusBox.add(statusPanel2);
		//statusBox.add(Box.createHorizontalGlue());
		statusBox.add(statusPanel3);
		statusBox.add(Box.createVerticalStrut(7));
		statusBox.add(statusPanel4);
		statusPanel.add(statusBox, BorderLayout.CENTER);
		
		// ** OPTIONS PANEL SETUP **
		JLabel  inputLabel    = new JLabel(Resources.getTranslation("reporter_input_bundle"));
		JLabel  outputLabel   = new JLabel(Resources.getTranslation("reporter_output_directory"));
		JButton inputButton   = new JButton(Resources.getTranslation("reporter_button_choose"));
		JButton outputButton  = new JButton(Resources.getTranslation("reporter_button_choose"));
		JButton scanButton    = new JButton(Resources.getTranslation("reporter_button_choose"));
		JButton defaultButton = new JButton(Resources.getTranslation("reporter_button_save_defaults"));
		JLabel  textLabel     = new JLabel(Resources.getTranslation("reporter_output_file"));
		JLabel  htmlLabel     = new JLabel(Resources.getTranslation("reporter_output_file"));
		JLabel  xmlLabel      = new JLabel(Resources.getTranslation("reporter_output_file"));
		JLabel  textLabel2    = new JLabel(Resources.getTranslation("reporter_detail_level"));
		JLabel  htmlLabel2    = new JLabel(Resources.getTranslation("reporter_detail_level"));
		JLabel  xmlLabel2     = new JLabel(Resources.getTranslation("reporter_detail_level"));
		JPanel  optionsPanel1 = new JPanel();
		JPanel  optionsPanel2 = new JPanel();
		JPanel  optionsPanelA = new JPanel();
		JPanel  optionsPanel3 = new JPanel();
		JPanel  optionsPanel4 = new JPanel();
		JPanel  optionsPanel5 = new JPanel();
		JPanel  optionsPanel6 = new JPanel();
		Box     optionsBox    = new Box(BoxLayout.Y_AXIS);
		Box     outputBox     = new Box(BoxLayout.Y_AXIS);
		
		bundleField    = new JTextField(Preferences.getPreference("reporter_base_class_file"));
		directoryField = new JTextField(Preferences.getPreference("reporter_output_directory"));
		textCheck      = new JCheckBox(Resources.getTranslation("reporter_format_text"));
		htmlCheck      = new JCheckBox(Resources.getTranslation("reporter_format_html"));
		xmlCheck       = new JCheckBox(Resources.getTranslation("reporter_format_xml"));
		scanCheck      = new JCheckBox(Resources.getTranslation("reporter_perform_scan"), false);
		textField      = new JTextField("report.txt");
		htmlField      = new JTextField("report.html");
		xmlField       = new JTextField("report.xml");
		scanField      = new JTextField();
		String [] detailLevels = {Resources.getTranslation("reporter_detail_high"),
								  Resources.getTranslation("reporter_detail_normal")};
		textCombo      = new JComboBox(detailLevels);
		htmlCombo      = new JComboBox(detailLevels);
		xmlCombo       = new JComboBox(detailLevels);
		
		bundleField.setColumns(30);
		directoryField.setColumns(30);
		scanField.setColumns(30);
		textField.setColumns(15);
		htmlField.setColumns(15);
		xmlField.setColumns(15);
		Dimension checkDim = new Dimension(55,compHeight);
		textCheck.setPreferredSize(checkDim);
		htmlCheck.setPreferredSize(checkDim);
		xmlCheck.setPreferredSize(checkDim);
		optionsPanel1.setLayout(new FlowLayout(FlowLayout.RIGHT));
		optionsPanel2.setLayout(new FlowLayout(FlowLayout.RIGHT));
		optionsPanelA.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		inputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				setInputBundle();
			}
		});
		
		outputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				setOutputBundle();
			}
		});
		
		scanButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ev) {
				setScanFile();
			}
		});
		
		defaultButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				saveDefaults();
			}
		});
		
		optionsPanel6.add(defaultButton);
		optionsPanel3.add(textCheck);
		optionsPanel3.add(Box.createHorizontalStrut(5));
		optionsPanel3.add(textLabel);
		optionsPanel3.add(Box.createHorizontalStrut(5));
		optionsPanel3.add(textField);
		optionsPanel3.add(Box.createHorizontalStrut(5));
		optionsPanel3.add(textLabel2);
		optionsPanel3.add(Box.createHorizontalStrut(5));
		optionsPanel3.add(textCombo);
		optionsPanel4.add(htmlCheck);
		optionsPanel4.add(Box.createHorizontalStrut(5));
		optionsPanel4.add(htmlLabel);
		optionsPanel4.add(Box.createHorizontalStrut(5));
		optionsPanel4.add(htmlField);
		optionsPanel4.add(Box.createHorizontalStrut(5));
		optionsPanel4.add(htmlLabel2);
		optionsPanel4.add(Box.createHorizontalStrut(5));
		optionsPanel4.add(htmlCombo);
		optionsPanel5.add(xmlCheck);
		optionsPanel5.add(Box.createHorizontalStrut(5));
		optionsPanel5.add(xmlLabel);
		optionsPanel5.add(Box.createHorizontalStrut(5));
		optionsPanel5.add(xmlField);
		optionsPanel5.add(Box.createHorizontalStrut(5));
		optionsPanel5.add(xmlLabel2);
		optionsPanel5.add(Box.createHorizontalStrut(5));
		optionsPanel5.add(xmlCombo);
		outputBox.add(optionsPanel3);
		outputBox.add(optionsPanel4);
		outputBox.add(optionsPanel5);
		formatPanel.add(outputBox);
		optionsPanel1.add(inputLabel);
		optionsPanel1.add(Box.createHorizontalStrut(5));
		optionsPanel1.add(bundleField);
		optionsPanel1.add(Box.createHorizontalStrut(5));
		optionsPanel1.add(inputButton);
		optionsPanel2.add(outputLabel);
		optionsPanel2.add(Box.createHorizontalStrut(5));
		optionsPanel2.add(directoryField);
		optionsPanel2.add(Box.createHorizontalStrut(5));
		optionsPanel2.add(outputButton);
		optionsPanelA.add(scanCheck);
		optionsPanelA.add(Box.createHorizontalStrut(5));
		optionsPanelA.add(scanField);
		optionsPanelA.add(Box.createHorizontalStrut(5));
		optionsPanelA.add(scanButton);
		optionsBox.add(optionsPanel1);
		optionsBox.add(optionsPanel2);
		optionsBox.add(optionsPanelA);
		optionsBox.add(formatPanel);
		optionsBox.add(optionsPanel6);
		optionsPanel.add(optionsBox);
		
		// ** INTERVAL PANEL SETUP **
		String boxArray1[] = {"1","2","3","4","5","6","7","8","9","10","11","12","15","20","24","25","30"};
		String boxArray2[] = {Resources.getTranslation("reporter_time_minutes"),
							  Resources.getTranslation("reporter_time_hours"),
							  Resources.getTranslation("reporter_time_days")};
		String boxArray3[] = {"1","2","3","4","5","6","7","8","9","10","11","12",
							  "13","14","15","16","17","18","19","20","21","22","23","0"};
		String boxArray4[] = {"00","15","30","45"};
		String boxArray5[] = {Resources.getTranslation("reporter_time_everyday"),
							  Resources.getTranslation("reporter_time_monday"),
							  Resources.getTranslation("reporter_time_tuesday"),
							  Resources.getTranslation("reporter_time_wednesday"),
							  Resources.getTranslation("reporter_time_thursday"),
							  Resources.getTranslation("reporter_time_friday"),
							  Resources.getTranslation("reporter_time_saturday"),
							  Resources.getTranslation("reporter_time_sunday")};
		
		JLabel colonLabel = new JLabel(":");
		sequentialRadio = new JRadioButton(Resources.getTranslation("reporter_interval_sequential"));
		definedRadio = new JRadioButton(Resources.getTranslation("reporter_interval_defined"), true);
		valueCombo = new JComboBox(boxArray1);
		unitCombo = new JComboBox(boxArray2);
		hourCombo = new JComboBox(boxArray3);
		minuteCombo = new JComboBox(boxArray4);
		dayCombo = new JComboBox(boxArray5);
		JPanel intervalPanel1 = new JPanel();
		JPanel intervalPanel2 = new JPanel();
		intervalPanel1.setLayout(new FlowLayout(FlowLayout.LEFT));
		intervalPanel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		Box intervalBox = new Box(BoxLayout.Y_AXIS);
		intervalPanel.setLayout(new BorderLayout());
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(sequentialRadio);
		bg.add(definedRadio);
		
		intervalPanel1.add(sequentialRadio);
		intervalPanel1.add(Box.createHorizontalStrut(5));
		intervalPanel1.add(valueCombo);
		intervalPanel1.add(Box.createHorizontalStrut(5));
		intervalPanel1.add(unitCombo);
		intervalPanel2.add(definedRadio);
		intervalPanel2.add(Box.createHorizontalStrut(5));
		intervalPanel2.add(hourCombo);
		intervalPanel2.add(colonLabel);
		intervalPanel2.add(minuteCombo);
		intervalPanel2.add(Box.createHorizontalStrut(5));
		intervalPanel2.add(dayCombo);
		intervalBox.add(intervalPanel1);
		intervalBox.add(intervalPanel2);
		intervalPanel.add(intervalBox, BorderLayout.WEST);
		
		// ** MAINBOX SETUP **
		mainBox.removeAll();
		mainBox.add(statusPanel);
		mainBox.add(intervalPanel);
		mainBox.add(optionsPanel);
		
		// ** MAIN FRAME SETUP **
		setLocation(new java.awt.Point(25, 25));
		setSize(mainDim);
		//((JComponent)getContentPane()).setMaximumSize(dimMainMax);
		//((JComponent)getContentPane()).setMinimumSize(dimMainMin);
		//setJMenuBar(jMenuBarMain);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().removeAll();
		getContentPane().add(mainBox, BorderLayout.CENTER);
		setTitle(Resources.getTranslation("resource_bundle_reporter"));
		//validateTree();
		setComponentsToDefaults();
		nextReport = generateNextReportDate();
		updateDateFields();
		repaint();
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent ev) {
				thisWindowClosing(ev);
			}
		});
	}
	
	public void thisWindowClosing(WindowEvent ev) {
		setVisible(false);
		dispose();
		System.exit(0);
	}
	
	private void setInputBundle() {
		int result = bundleFileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = bundleFileChooser.getSelectedFile();
			if (f != null) {
				bundleField.setText(f.getAbsolutePath());
				Preferences.setPreference("reporter_base_class_file",f.getAbsolutePath());
				try {Preferences.savePreferences();} catch (IOException ioe) {}
			}
		}
	}
	
	private void setOutputBundle() {
		int result = directoryFileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = directoryFileChooser.getSelectedFile();
			if (!f.isDirectory()) f = new File(f.getParent());
			if (f != null) {
				directoryField.setText(f.getAbsolutePath());
				Preferences.setPreference("reporter_output_directory",f.getAbsolutePath());
				try {Preferences.savePreferences();} catch (IOException ioe) {}
			}
		}
	}
	
	private void setScanFile() {
		int result = scanFileChooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = scanFileChooser.getSelectedFile();
			if (f != null) {
				scanField.setText(f.getAbsolutePath());
				Preferences.setPreference("reporter_scan_file",f.getAbsolutePath());
				try {Preferences.savePreferences();} catch (IOException ioe) {}
			}
		}
	}
	
	private static String getUsage() {
		return "\nRBReporter Command Line Usage:\n\n" +
			   "Default Usage (GUI):   java com.ibm.rbm.RBReporter\n" +
			   "Options Usage:         java com.ibm.rbm.RBReporter [-gui | -now | -line]\n\n" + 
			   "Options:               -gui     Run the Graphical User Interface\n" +
			   "                       -now     Execute the Report Generation Immediately\n" +
			   "                       -line    Run the Reporter without the GUI";
	}
	
	public static void main(String args[]) {
	    RBReporter reporter;
		if (args.length == 1) {
			if (args[0].equals("-gui")) {
			    reporter = new RBReporter(true);	
			} else if (args[0].equals("-now")) {
				reporter = new RBReporter(false);
				try {
					reporter.generateReports();
					System.out.println("RBReporter: Generation of reports successful. " + new Date());
				} catch (IOException ioe) {
					System.out.println("There was an error generating the reports...\n\n\t" + ioe.getMessage());
				}
				reporter.thisWindowClosing(null);
			} else if (args[0].equals("-line")) {
				reporter = new RBReporter(false);
				if (!reporter.running)
				    reporter.toggleStatus();
				System.out.println("RBReporter: Next Report at " + reporter.nextReport.toString());
			} else {
				System.out.println(getUsage());
			}
		} else if (args.length == 0) {
			reporter = new RBReporter(true);
		} else {
			System.out.println(getUsage());
		}
	}
	
}