/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.util.*;
import java.io.*;
import javax.swing.UIManager;
import javax.swing.JOptionPane;

import com.ibm.rbm.gui.RBManagerGUI;

/**
 * A utility class to aid in the process of updating the Natural Language Support of Tempus Fugit.
 * This class scans the directory containing NLS files and checks the various languages found there
 * for completeness, duplication of entry, and status of translation. The class can be instantiated
 * through a constructor, or it can be run from the command line. For additional information on the
 * command line results, see the <CODE>main</CODE> method.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBManager {
	
    // *** DATA ***
    private Vector allBundleKeys;                               // A Vector of Strings with all defined NLS properties
    private Vector bundles;                                   // A Vector of NLSbundles, one for each language
    private String currentUser;                                 // The name of the person currently using the editor
    private String baseClass;                                   // The name of the base class of the active resource bundle
    private File   currentDirectory;
	
    // *** CONSTRUCTORS ***
	
    // The default constructor is not publicly available
    private RBManager() {
        try {
            // Look and Feel check
            try {
                String laf = Preferences.getPreference("lookandfeel");
                if (!laf.equals("")) UIManager.setLookAndFeel(laf);
            } catch (Exception e) { 
                // Ignored
            }
			
            Resources.initBundle();
            RBManagerGUI guiFrame = new RBManagerGUI();
            if (!Preferences.getPreference("username").equals(""))
                guiFrame.setUser(Preferences.getPreference("username"));
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
            guiFrame.initComponents();
            guiFrame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    /**
     * This constructor creates an entirely blank RBManager and base Bundle. Only the base class name is defined.
     * All other properties need to be defined.
     */
	
    public RBManager(String baseClassName) {
        allBundleKeys = new Vector();
        bundles = new Vector();
        currentUser = "Unknown";
        baseClass = baseClassName;
        currentDirectory = new File("");
		
        Bundle mainBundle = new Bundle("");
        // Create a default group
        mainBundle.addBundleGroup("Ungrouped Items", "These are resource items that have not been assigned a group");
        bundles.addElement(mainBundle);
    }
	
    /**
     * This is the standard constructor for RBManager. It is constructed from the root of a resource bundle.
     * In the current implementation, each file is parsed separately starting with the base class file (root).
     * In this implementation, the lookup keys are represented to the user as they appear in the files. The
     * translation values however are translated according to the basic rules defined in java.util.Properties.
     * Thus in the key, the user may see '\"' when in the value it would have been converted to '"'. This
     * translation is reversed when saving the resource bundle.
     * @param mainFile The base class file of the resource bundle to be read
     */
	
    public RBManager(File mainFile) throws FileNotFoundException, IOException {
        init();
		
        currentDirectory = new File(mainFile.getParent());
		
        String[] encodings;
		
        // Initiailize the readers to the main NLS file
        FileReader fr = new FileReader(mainFile);
        BufferedReader br = new BufferedReader(fr);
		
        // Load the java readable values from the main NLS file;
        Properties p = new Properties();
        p.load(new FileInputStream(mainFile));
		
        // Count the number of language files and set up the encoding and dictionary data
        int numLanguages = 1;
        String NLSbaseClass = null;
        String NLSpostfix   = null;
		
        if (mainFile.getName().indexOf(".") >= 0) {
            NLSbaseClass = mainFile.getName().substring(0,mainFile.getName().indexOf("."));
            NLSpostfix = ".properties";
        } else {
            NLSbaseClass = mainFile.getName();
            NLSpostfix = "";
        }
		
        baseClass = NLSbaseClass;
        
        String filePrefix = mainFile.getName().substring(0,mainFile.getName().lastIndexOf("."));
        String filePostfix = mainFile.getName().substring(mainFile.getName().lastIndexOf("."),mainFile.getName().length());
        File resDir = currentDirectory;
        if (resDir != null && resDir.isDirectory()) {
            String[] temp = resDir.list();
            numLanguages = 0;
            // Count the number of language files
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].startsWith(NLSbaseClass) && (temp[i].endsWith(NLSpostfix) 
                    || temp[i].endsWith(NLSpostfix.toUpperCase()) || NLSpostfix.equals(""))) {
                    // Starts with the base class name and ends in proper suffix (above)
                    // Base name is followed by . or _ (below)
                    RBManagerGUI.debugMsg("Character is: " + temp[i].charAt(NLSbaseClass.length()));
                    if (temp[i].charAt(NLSbaseClass.length()) == '.' || temp[i].charAt(NLSbaseClass.length()) == '_')
                        numLanguages++;
                }
            }
            // Initialize the bundles and encodings
            encodings = new String[numLanguages];
			
            int count = 1;
            for (int i = 0; i < temp.length; i++) {
                if (temp[i].equals(mainFile.getName())) {
                    encodings[0] = "";
                } else if (temp[i].startsWith(NLSbaseClass) && (temp[i].endsWith(NLSpostfix) 
                    || temp[i].endsWith(NLSpostfix.toUpperCase()) || NLSpostfix.equals(""))) {
                    if (temp[i].charAt(NLSbaseClass.length()) == '.' || temp[i].charAt(NLSbaseClass.length()) == '_') {
                        encodings[count] = new String(temp[i].substring(filePrefix.length()+1,temp[i].indexOf(filePostfix)));							count++;
                    }
                }
            }
        } else {
            // Initialize the bundles and encodings in case the directory information is not available
            // In this case, only the main NLS file will be handled
            encodings = new String[numLanguages];
            encodings[0] = new String("");
        } // end the count and initialization
		
        // Read in the entries from the main file
        String line;
        // Set the dictionary for the main file
        Bundle dict = new Bundle(encodings[0]);
        bundles.addElement(dict);
        // Set up the first group in case there are NLS items which were not assigned to a group
        BundleGroup group = new BundleGroup(dict, "Ungrouped Items");
        group.setComment("NLS Items which were not initially assigned to a group");
        dict.addBundleGroup(group);
        BundleItem item = new BundleItem(group,null,null);
        int count = 0;
        while ((line = br.readLine()) != null) {
            // Test to make sure this is a file that was generated by RBManager
            if (!line.trim().equals("")) count++;
            if (count == 1 && !line.startsWith("# @file")) {
                // Not generated by RBManager
                JOptionPane.showMessageDialog(null,
                    Resources.getTranslation("error_not_rbmanager_format") + "\n" + Resources.getTranslation("error_suggest_import_properties"),
                    Resources.getTranslation("dialog_title_error_not_rbmanager_format"), JOptionPane.ERROR_MESSAGE);
                throw new FileNotFoundException("Improper format for file: " + mainFile.getName());
            }
            String commentLine = null;
            // Grab text following the # sign
            if (line.indexOf("#") >= 0) {
                commentLine = line.substring(line.indexOf("#")+1,line.length());
                line = line.substring(0,line.indexOf("#"));
            }
            if (commentLine != null && commentLine.trim().length() > 0) {
                // Process any information made available in comment '@' information
                Hashtable descriptors = getDescriptors(null,commentLine);
                if (descriptors != null) {
                    Object o;
                    // File tags
                    o = descriptors.get("file"); if (o != null) dict.name = ((String) o);
                    o = descriptors.get("fileComment");  if (o != null) dict.comment  = ((String) o);
                    o = descriptors.get("fileLanguage"); if (o != null) dict.language = ((String) o);
                    o = descriptors.get("fileCountry");  if (o != null) dict.country  = ((String) o);
                    o = descriptors.get("fileVariant");  if (o != null) dict.variant  = ((String) o);
                    o = descriptors.get("fileManager");  if (o != null) dict.manager  = ((String) o);
                    
                    // Group tags
                    o = descriptors.get("group");
                    if (o != null) {
                        group = new BundleGroup(dict, (String)o);
                        item.setParentGroup(group);
                        dict.addBundleGroup(group);
                    }
                    o = descriptors.get("groupComment"); if (o != null) group.setComment((String) o);
					
                    // Item tags
                    o = descriptors.get("comment");    if (o != null) item.setComment((String) o);
                    o = descriptors.get("translated"); if (o != null) item.setTranslated(((String) o).equalsIgnoreCase("true"));
                    o = descriptors.get("creator");    if (o != null) item.setCreator((String) o);
                    o = descriptors.get("modifier");   if (o != null) item.setModifier((String) o);
                    o = descriptors.get("created");    if (o != null) item.setCreatedDate((String) o);
                    o = descriptors.get("modified");   if (o != null) item.setModifiedDate((String) o);
					
                    // Lookup tags (e.g. {_#_} _description_)
                    Enumeration keys = descriptors.keys();
                    while (keys.hasMoreElements()) {
                        String tag = (String)keys.nextElement();
                        if (tag.startsWith("{")) {
                            if (tag.indexOf("}") < 0) continue;
                            String lookup = tag.substring(1,tag.indexOf("}"));
                            item.getLookups().put(lookup, descriptors.get(tag));
                        }
                    }
                } 
            } // end check of comment line
            if (line.trim().length() < 1) continue;
			
            // Grab the name and value (translation) from the line
            int breakpoint = 0;
            boolean started = false;
            char array[] = line.toCharArray();
            for (int i=0; i < array.length; i++) {
                if (!started && array[i] != ' ' && array[i] != '\t') started = true;
                if (started && (array[i] == '=' || array[i] == ':' || array[i] == ' ' || array[i] == '\t')) {
                    breakpoint = i;
                    break;
                }
            }
            String key = String.valueOf(array,0,breakpoint);
			
            item.setKey(key);
            String translation = p.getProperty(key);
            if (translation == null || translation.equals(""))
                item.setTranslation(line.substring(line.indexOf("=")+1,line.length()).trim());
            else item.setTranslation(translation);
			
            dict.addBundleItem(item);
            item = new BundleItem(group,null,null);
        } // end while - main NLS file
		
        // Now that we have parsed the entire main language file, populate the allNLSKey set with the dictionary keys
        allBundleKeys = new Vector();
        Enumeration keys = ((Bundle)bundles.elementAt(0)).allItems.keys();
        while (keys.hasMoreElements()) {
            allBundleKeys.addElement(keys.nextElement());
        }
		
        // Now go through all of the other languages
        for (int i = 1; i < encodings.length; i++) {
            if (encodings[i].equals("kr")) continue; // I can't handle double byte character sets yet
            // Try to obtain the new file
            File tempFile = new File(resDir, NLSbaseClass + "_" + encodings[i] + NLSpostfix);
            fr = new FileReader(tempFile);
            br = new BufferedReader(fr);
			
            // Try to obtain the java readable properties for the file
            p = new Properties();
            p.load(new FileInputStream(tempFile));
            
            // Set the dictionary for the main file
            dict = new Bundle(encodings[i]);
            bundles.addElement(dict);
            // Set up the first group in case there are NLS items which were not assigned to a group
            group = new BundleGroup(dict, "Ungrouped Items");
            dict.addBundleGroup(group);
            group.setComment("NLS Items which were not initially assigned to a group");
            item = new BundleItem(group,null,null);
            // Create the rest of the groups
            while ((line = br.readLine()) != null) {
                String commentLine = null;
                // Grab the text following the # sign
                if (line.indexOf("#") >= 0) {
                    commentLine = line.substring(line.indexOf("#")+1,line.length());
                    line = line.substring(0,line.indexOf("#"));
                }
                if (commentLine != null && commentLine.trim().length() > 0) {
                    // Process any information made available in comment '@' information
                    Hashtable descriptors = getDescriptors(null,commentLine);
                    if (descriptors != null) {
                        Object o;
                        // File tags
                        o = descriptors.get("file"); if (o != null) dict.name = ((String) o);
                        o = descriptors.get("fileComment");  if (o != null) dict.comment  = ((String) o);
                        o = descriptors.get("fileLanguage"); if (o != null) dict.language = ((String) o);
                        o = descriptors.get("fileCountry");  if (o != null) dict.country  = ((String) o);
                        o = descriptors.get("fileVariant");  if (o != null) dict.variant  = ((String) o);
                        o = descriptors.get("fileManager");  if (o != null) dict.manager  = ((String) o);
						
                        // Group tags
                        o = descriptors.get("group");
                        if (o != null) {
                            group = new BundleGroup(dict, (String)o);
                            item.setParentGroup(group);
                            dict.addBundleGroup(group);
                        }
                        o = descriptors.get("groupComment"); if (o != null) group.setComment((String) o);
                        
                        // Item tags
                        o = descriptors.get("comment");    if (o != null) item.setComment((String) o);
                        o = descriptors.get("translated"); if (o != null) item.setTranslated(((String) o).equalsIgnoreCase("true"));
                        o = descriptors.get("creator");    if (o != null) item.setCreator((String) o);
                        o = descriptors.get("modifier");   if (o != null) item.setModifier((String) o);
                        o = descriptors.get("created");    if (o != null) item.setCreatedDate((String) o);
                        o = descriptors.get("modified");   if (o != null) item.setModifiedDate((String) o);
                        
                        // Lookup tags (e.g. {_#_} _description_)
                        Enumeration descKeys = descriptors.keys();
                        while (descKeys.hasMoreElements()) {
                            String tag = (String)descKeys.nextElement();
                            if (tag.startsWith("{")) {
                                if (tag.indexOf("}") < 0) continue;
                                String lookup = tag.substring(1,tag.indexOf("}"));
                                item.getLookups().put(lookup, descriptors.get(tag));
                            }
                        }
                    }	
                } // end check of comment line
                if (line.trim().length() < 1) continue;
			
                // Grab the name and value (translation) from the line
                int breakpoint = 0;
                boolean started = false;
                char array[] = line.toCharArray();
                for (int j=0; j < array.length; j++) {
                    if (!started && array[j] != ' ' && array[j] != '\t') started = true;
                    if (started && (array[j] == '=' || array[j] == ':' || array[j] == ' ' || array[j] == '\t')) {
                        breakpoint = j;
                        break;
                    }
                }
                String key = String.valueOf(array,0,breakpoint);
                item.setKey(key);
                String translation = p.getProperty(key);
                if (translation == null || translation.equals(""))
                    item.setTranslation(line.substring(line.indexOf("=")+1,line.length()).trim());
                else item.setTranslation(translation);
				
                dict.addBundleItem(item);
                item = new BundleItem(group,null,null);
            } // end while - next line
        } // end for looop through languages
        // Add this opened file to our recent files
        Preferences.addRecentFilePreference(mainFile.getName(), mainFile.getAbsolutePath());
    } // end RBManager()
	
    // *** METHODS ***
	
    /**
     * Main
     */
	
    public static void main(String args[]) {
        // Make sure the user specified a path
        if (args.length < 1) { 
            new RBManager();
            return;
        }
    } // main
	
    public String toString() { return baseClass; }
	
    /**
     * Write the contents of the file to the output stream
     */
	
    public void writeToFile() throws IOException {
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            File outputFile = new File(currentDirectory, baseClass + 
                                       ((bundle.encoding == null || bundle.encoding.equals("")) ? "" : "_" + bundle.encoding) +
                                        ".properties");
            FileWriter fw = new FileWriter(outputFile);
            bundle.writeContents(fw);
            fw.flush();
            fw.close();
        }
        // In case this is a newly created bundle or the location has changed recently, update the recent files, preference
        Preferences.addRecentFilePreference(baseClass + ".properties", currentDirectory.getAbsolutePath() + File.separator +
                                            baseClass + ".properties");
    }
	
    /**
     * Calling this method removes a resource from the resource bundle. This method does not permanently
     * erase the file containing the resources at this encoding, however any changes or saves that take
     * place once this file has been removed will not be reflected in this hidden file. To restore the resource, 
     * the bundle will have to be recreated. (This last point may change)
     */
	
    public void hideResource(String encoding) {
        for (int i=0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            if (bundle.encoding.equals(encoding)) {
                bundles.removeElement(bundle);
                break;
            }
        }
    }
	
    /**
     * Erases permanently one of the resource files. Be careful about calling this method there is nothing you can do
     * once a file is erased.
     */
	
    public void eraseFile(String encoding) throws IOException {
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            if (!(bundle.encoding.equals(encoding))) continue;
            File outputFile = new File(currentDirectory, baseClass + 
                                       ((bundle.encoding == null || bundle.encoding.equals("")) ? "" : "_" + bundle.encoding) +
                                        ".properties");
            boolean success = outputFile.delete();
            if (!success) throw new IOException(Resources.getTranslation("error_deletion_not_possible"));
            hideResource(encoding);
            break;
        }
    }
	
    /**
     * Writes only one of the resource files to the file system. This file is specified by the encoding parameter
     */
	
    public void writeToFile(String encoding) throws IOException {
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            if (bundle.encoding.equals(encoding) || (i==0 && encoding.equals(""))) {
                File outputFile = new File(currentDirectory, baseClass + 
                                           ((bundle.encoding == null || bundle.encoding.equals("")) ? "" : "_" + bundle.encoding) +
                                            ".properties");
                FileWriter fw = new FileWriter(outputFile);
                bundle.writeContents(fw);
                fw.flush();
                fw.close();
                break;
            }
        }
        // In case this is a newly created bundle or the location has changed recently, update the recent files, preference
        Preferences.addRecentFilePreference(baseClass + ".properties", currentDirectory.getAbsolutePath() + File.separator +
                                            baseClass + ".properties");
    }

    /**
     * Given a BundleItem and some properties to change for that item, this method first checks to make sure the passed
     * item is valid and if it is, the properties of that item are changed to reflect those passed in as parameters to this
     * method.
     * @return true if the BundleItem was valid and updateable, false if otherwise (in this case no changes were made).
     */
     
    public boolean editItem(BundleItem item, String name, String value, String groupName, String comment, Hashtable lookups) {
        if (name == null || name.equals("") || groupName == null || groupName.equals("") || item == null) return false;
        String oldName = item.getKey();
        String oldComment = item.getComment();
        String oldValue = item.getTranslation();
        //String oldGroupName = item.getParentGroup().getName();
        // Loop through the bundles
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            BundleItem oldItem = (BundleItem)bundle.allItems.get(oldName);
            if (oldItem == null) break;
            if (!oldName.equals(name)) {
                // A new key
                oldItem.setKey(name);
                bundle.allItems.remove(oldItem);
                bundle.allItems.put(oldItem.getKey(), oldItem);
            }
            if (oldItem.getComment() == null || oldItem.getComment().equals(oldComment)) oldItem.setComment(comment);
            if (oldItem.getTranslation().equals(oldValue)) oldItem.setTranslation(value);
            oldItem.setLookups(lookups);
            if (!oldItem.getParentGroup().getName().equals(groupName)) {
                // A new group
                oldItem.getParentGroup().removeBundleItem(oldItem.getKey());
                BundleGroup bg = bundle.getBundleGroup(groupName);
                if (bg == null) bg = bundle.getUngroupedGroup();
                oldItem.setParentGroup(bg);
                bg.addBundleItem(oldItem);
            }
        }
        return true;
    }
	
    /**
     * Attempts to create a new item in each of the language files. The method first checks the base Resource Bundle
     * to make sure that the item name does not all ready exist. If it does exist the item is not created.
     * @param name The unique key of the item
     * @param value The translation of the item for the base class
     * @param groupName The group name, should all ready exist in the base class
     * @param comment An optional comment to be added to the item, can be <CODE>null</CODE>
     * @return An error response. If the creation was successful <CODE>true</CODE> is returned, if there was an error <CODE>false</CODE> is returned.
     */
	
    public boolean createItem(String name, String value, String groupName, String comment, Hashtable lookups) {
        if (name == null || name.equals("") || groupName == null || groupName.equals("")) return false;
        Bundle mainBundle = (Bundle)bundles.firstElement();
        BundleGroup mainGroup = null;
        if (mainBundle.allItems.containsKey(name)) return false;
        for (int i=0; i < mainBundle.getGroupCount(); i++) {
            BundleGroup bg = mainBundle.getBundleGroup(i);
            if (bg.getName().equals(groupName)) {mainGroup = bg; break;}
        }
        if (mainGroup == null) return false;
        // Add to the base class
        BundleItem mainItem = new BundleItem(mainGroup, name, value);
        mainItem.setTranslated(true);
        mainItem.setCreator(currentUser);
        mainItem.setModifier(currentUser);
        mainItem.setComment(comment);
        mainBundle.allItems.put(name, mainItem);
        mainGroup.addBundleItem(mainItem);
        if (lookups != null) mainItem.setLookups(lookups);
        // Add to the rest of the bundles
        for (int i=1; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            // Find the group
            BundleGroup group = null;
            for (int j=0; j < bundle.getGroupCount(); j++) {
                BundleGroup bg = bundle.getBundleGroup(j);
                if (bg.getName().equals(groupName)) {group = bg; break;}
            }
            if (group == null) {
                group = new BundleGroup(bundle, groupName);
                bundle.addBundleGroup(group);
            }
            BundleItem item = new BundleItem(group, name, value);
            item.setCreator(currentUser);
            item.setModifier(currentUser);
            item.setComment(comment);
            if (lookups != null) item.setLookups(lookups);
            bundle.allItems.put(name, item);
            bundle.addUntranslatedItem(item);
            group.addBundleItem(item);
        }
        return true;
    }
    
    /**
     * Attempts to create a new group in each of the language files. The method first checks the base Resource Bundle
     * to make sure that the group name does not all ready exist. If it does exist the group is not created.
     * @param groupName The unique group name to be created
     * @param groupComment An optional comment to be added to the group, can be <CODE>null</CODE>
     * @return An error response. If the creation was successful <CODE>true</CODE> is returned, if there was an error <CODE>false</CODE> is returned.
     */
    public boolean createGroup(String groupName, String groupComment) {
        if (groupName == null || groupName.equals(""))
        	return false;
        // Check to see if the group exists
        Bundle mainBundle = (Bundle)bundles.firstElement();
        if (mainBundle.hasGroup(groupName))
        	return false;
		
        // Create the group
        for (int i=0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            BundleGroup bg = new BundleGroup(bundle, groupName);
            if (groupComment != null)
            	bg.setComment(groupComment);
            bundle.addBundleGroup(bg);
        }
        return true;
    }
	
    /**
     * Removes a group and all of the items within that group from the various
     * Resource Bundles known to the system. This method removes the group from
     * the protected vector of groups, then removes all items in that group from
     * the protected vector of untranslated items, and the protected hashtable of
     * all items.
     */
	
    public void deleteGroup(String groupName) {
        if (groupName == null) return;
        // Loop through all of the bundles;
        for (int i=0; i < bundles.size(); i++) {
            Bundle bundle = (Bundle)bundles.elementAt(i);
            bundle.removeGroup(groupName);
        }
    }
	
    /**
     * Remove resource items of the given name from each of the resource bundles that the system
     * knows about. This works by first removing the item from the protected vector of translated
     * items, if it is there, and then removing it from the the hashtable of all items, and then
     * removing it from its respective group. 
     */
	
    public void deleteItem(String itemName) {
        if (itemName == null) return;
        // Loop through all of the bundles;
        for (int i=0; i < bundles.size(); i++) {
            // Loop through untranslated items
            Bundle bundle = (Bundle)bundles.elementAt(i);
            bundle.removeUntranslatedItem(itemName);
                    
            // Loop through all Items
            Enumeration items = bundle.allItems.elements();
            while(items.hasMoreElements()) {
                BundleItem item = (BundleItem)items.nextElement();
                if (item.getKey().equals(itemName)) {
                    bundle.allItems.remove(item);
                    item.getParentGroup().removeBundleItem(item.getKey());
                }
            }
        }
    }
	
    /**
     * Looks through the resources contained in the bundle for a resource of the given encoding. Note that this
     * search is case sensitive.
     * @return True if the encoding exists as one of the resource files, false otherwise
     */
	
    public boolean hasResource(String encoding) {
        // Check to see if the encoding exists
        for (int i=0; i < bundles.size(); i++) {
            Bundle b = (Bundle)bundles.elementAt(i);
            if (b.encoding.equals(encoding)) return true;
        }
        return false;
    }
	
    /**
     * Attempts to create a new resource file with the given encoding. The method first checks the base Resource Bundle
     * to make sure that encoding does not all ready exist. If it does exist the resource file is not created.
     * @param title An optional, quick title for the file, can be <CODE>null</CODE>
     * @param comment An optional comment to be added to the resource, can be <CODE>null</CODE>
     * @param manager The name of the person responsible for this resource, can be <CODE>null</CODE>
     * @param encoding The proper encoding for the resource. Must be of form 'language', 'language_country', or 'language_country_variant'
     * @param language A more formal name for the language (e.g. 'English', 'Deutsch', etc.), can be <CODE>null</CODE>
     * @param country A more formal name for the country described by the resource, can be <CODE>null</CODE>
     * @param variant A more formal name for the variant described by the resource, can be <CODE>null</CODE>
     * @param copyValues An indication of wether or not to populate the resource with the items in the base class
     * @return An error response. If the creation was successful <CODE>true</CODE> is returned, if there was an error <CODE>false</CODE> is returned.
     */
	
    public boolean createResource(String title, String comment, String manager, String encoding, 
                                  String language, String country, String variant, boolean copyValues) {
        if (encoding == null || encoding.equals("") || encoding.startsWith("_")) return false;
        // Check to see if the encoding exists
        if (hasResource(encoding)) return false;
        // Create the resource
        Bundle bundle = new Bundle(encoding);
        bundle.name = title;
        bundle.comment = comment;
        bundle.manager = manager;
        bundle.language = language;
        bundle.country = country;
        bundle.variant = variant;
		
        // Create a default group
        bundle.addBundleGroup("Ungrouped Items", "These are resource items that have not been assigned a group");
        
        if (copyValues) {
            Bundle mainBundle = (Bundle)bundles.firstElement();
            for (int i=0; i < mainBundle.getGroupCount(); i++) {
                BundleGroup mainGroup = mainBundle.getBundleGroup(i);
                BundleGroup bg = new BundleGroup(bundle,mainGroup.getName());
                bg.setComment(mainGroup.getComment());
                bundle.addBundleGroup(bg);
                for (int j=0; j < mainGroup.getItemCount(); j++) {
                    BundleItem mainItem = mainGroup.getBundleItem(j);
                    BundleItem item = new BundleItem(bg, mainItem.getKey(), mainItem.getTranslation());
                    item.setComment(mainItem.getComment());
                    item.setCreator(mainItem.getCreator());
                    item.setModifier(mainItem.getModifier());
                    item.setLookups(new Hashtable());
                    // TODO: This should be done in the Bundle class
                    Enumeration keys = mainItem.getLookups().keys();
                    while (keys.hasMoreElements()) {
                        String name = (String)keys.nextElement();
                        String value = (String)mainItem.getLookups().get(name);
                        item.getLookups().put(new String(name), new String(value));
                    }
                    bg.addBundleItem(item);
                    bundle.addUntranslatedItem(item);
                }
            }
        }
		
        bundles.addElement(bundle);
        
        return true;
    }

    /**
     * Returns the number of duplicate NLS entries
     */
	
    public int getNumberDuplicates() {
        return ((Bundle)bundles.firstElement()).duplicates.size();
    }
	
    /**
     * Returns a single string with a comma delimited listing of all duplicate entries found in the NLS resources
     */
	
    public String getDuplicatesListing() {
        return listStrings(getDuplicatesListingVector());
    }

    /**
     * Returns a Vector collection of duplicate BundleItems found in the bundle
     */
	
    public Vector getDuplicatesListingVector() {
        return ((Bundle)bundles.firstElement()).duplicates;
    }

    /**
     * A useful debugging method that lists the various BundleGroup names in a String.
     */
	
    public String getGroupListing() {
        return listStrings(getGroupListingVector());
    }
            
    /**
     * Returns a vector collection of all of the BundleGroup items founds int the bundle.
     */
    
    public Vector getGroupListingVector() {
        Vector v = new Vector();
        Bundle bundle = (Bundle)bundles.firstElement();
        for (int i=0; i < bundle.getGroupCount(); i++) {
            String name = bundle.getBundleGroup(i).getName();
            v.addElement(name);
        }
        return v;
    }
	
    /**
     * Returns the total number of languages that the system seems to support
     */
	
    public int getNumberLanguages() {
        return bundles.size();
    }
	
    /**
     * Returns a single string comprised of a comma delimited listing of all languages the system seems to support
     */
	
    public String getLanguageListing() {
        return listStrings(getLanguageListingVector());
    }
	
    /**
     * Returns a vector of strings comprising a list of all languages in the system
     */
	
    public Vector getLanguageListingVector() {
        Vector v = new Vector();
		
        for (int i = 0; i < bundles.size(); i++) {
            Bundle dict = (Bundle)bundles.elementAt(i);
            String dictStr = new String();
            if (dict.language != null) dictStr += dict.language;
            if (dict.country != null) dictStr += " " + dict.country;
            if (dict.variant != null) dictStr += " " + dict.variant;
            if (dictStr.trim().equals("")) dictStr = (dict.encoding.trim().equals("") ? "Base Resource Bundle" : dict.encoding);
            v.addElement(dictStr);
        }
		
        return v;
    }
	
    /**
     * Returns the number of translations contained across all language files
     */
	
    public int getNumberTotalTranslations() {
        return allBundleKeys.size();
    }

    /**
     * Returns the number of BundleGroups in  the bundle.
     */
	
    public int getNumberGroups() {
        return ((Bundle)bundles.firstElement()).getGroupCount();
    }
	
    /**
     * Returns the name of the user currently using the editor
     */
	
    public String getUser() {
        return currentUser;	
    }
	
    /**
     * Sets the name of the user currently using the editor
     */
	
    public void setUser(String user) {
        currentUser = user;	
    }
    
    /**
     * Sets the name of the base class associated with this resource bundle
     */
    
    public void setBaseClass(String baseClassName) {
        baseClass = baseClassName;
    }
    
    /**
     * Sets the directory in the file system in which this resource bundle is to be
     * saved and retrieved.
     */
    
    public void setFileDirectory(File directory) {
        if (directory.isDirectory()) currentDirectory = directory;
    }

    /**
     * Returns the base class name if known, or "Unknown Base Class" otherwise.
     */
    public String toSring() {
        return (baseClass == null ? "Unknown Base Class" : baseClass);
    }

    /**
     * Returns the base class name or null if it does not exist.
     */
	
    public String getBaseClass() {
        return baseClass;
    }
	
    /**
     * A Vector of NLSbundles, one for each language
     */
    public Vector getBundles() {
        return bundles;
    }
    
    /**
     * Return a bundle from a locale
     * @return The requested resource bundle
     */
    public Bundle getBundle(String locale) {
    	Bundle bundle = null;
        if (hasResource(locale)) {
            for (int i = 0; i < bundles.size(); i++) {
                Bundle tempb = (Bundle)bundles.elementAt(i);
                if (tempb.encoding.equals(locale)) {
                    bundle = tempb;
                    break;
                }
            }
        }
        return bundle;
    }

    /**
     * Returns the name of the file that is the base class file for the resource bundle.
     */
    
    public File getBaseFile() {
        return new File(currentDirectory,baseClass + ".properties");
    }
	
    // Return a single comma delimited string made from a vector of strings
    private String listStrings(Vector v) {
        String retStr = new String();
        for (int i = 0; i < v.size(); i++) {
            Object o = v.elementAt(i);
            if (!(o instanceof String)) continue;
            String s = (String)o;
            if (i > 0) retStr += ", ";
            retStr += s;
        }
        return retStr;
    }
	
    // Init - called before ant construction
    private void init() {
        allBundleKeys = new Vector();
        bundles = new Vector();
        currentUser = "Unknown";
    }
	
    // Return a hashtable of the tags in a comment line (i.e. the text after each '@' character) and their values
    private Hashtable getDescriptors(Hashtable result, String line) {
        // Recursion terminating condition
        if (line == null || line.length() <= 0 || line.indexOf("@") < 0) return result;
        // Otherwise generate what information we can and recurse
        if (result == null) result = new Hashtable();
        // Strip off any information before and including a '@'
        line = line.substring(line.indexOf("@")+1, line.length());
        // There should be a space after the '@_tag_' and the value of this property
        if (line.indexOf(" ") < 0) return result;        // This shouldn't happen if things are formatted right
        // Add the text after the '@' character up to the first whitespace (has to be a space, not tab or other whitespace)
        String name = line.substring(0,line.indexOf(" ")).trim();
        // Now strip off the tag name
        line = line.substring(line.indexOf(" "), line.length());
        // If there is another '@' character we take the value up until that character
        if (line.indexOf("@") >= 0) {
            result.put(name,line.substring(0,line.indexOf("@")).trim());
        }
        // Otherwise we take the rest of the characters in the line
        else {
            result.put(name,line.trim());
            return result;
        }
        // Recurse
        return getDescriptors(result, line.substring(line.indexOf("@"), line.length()));
    }

    // Checks an array of strings to see if it contains a particular string
/*    private static boolean arrayContains(String[] array, String match) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(match)) return true;
        }
        return false;
    }*/
	
    // Prints the usage of the program when called from main
/*    private static void printUsage() {
        String usage = new String();
        usage += "Usage:\n\njava com.ibm.almaden.TempusFugit.Tools.RBManager fileName ((-r | -d) encoding?)?";
        usage += "\n\n  fileName -> The file (and path?) representing the main NLS resource\n\t\t(i.e. TempusFugit.resources)\n";
        usage += "  encoding -> Returns results for only the language encoding specified\n";
        usage += "  flag -r  -> Gives only a status report on the state of the translations\n"; 
        System.out.println(usage);
    }*/
    
}

