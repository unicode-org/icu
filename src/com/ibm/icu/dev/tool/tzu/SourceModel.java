/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * Represents a map of timezone version names to urls where they can be found that is usable by any
 * class that uses AbstractListModels (such as a JList or JCombobox in swing). Also contains methods
 * to begin a search for sources on the ICU Timezone Repository.
 */
class SourceModel extends AbstractListModel implements ComboBoxModel {
    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1339;

    /**
     * The URL of the ICU Timezone Repository. In order to catch a MalformedURLException, this field
     * must be initialized by the constructor.
     */
    public static URL TZ_BASE_URL = null;

    /**
     * The end of a URL string to any timezone resource in the ICU Timezone Repository meant for
     * ICU4J.
     */
    public static final String TZ_BASE_URLSTRING_END = "/be/zoneinfo.res";

    /**
     * The URL string of the ICU Timezone Repository.
     */
    public static final String TZ_BASE_URLSTRING_START = "http://icu-project.org/tzdata/";

    /**
     * The readable name of the local timezone resource file, ie. "Local Copy" or "Local Copy
     * (2007c)". Since the version is determined by a <code>ICUFile.findFileTZVersion</code>,
     * this field must be initialized by the constructor.
     */
    public static String TZ_LOCAL_CHOICE = null;

    /**
     * The URL of the local timezone resource file. In order to catch a MalformedURLException, this
     * field must be initialized by the constructor.
     */
    public static URL TZ_LOCAL_URL = null;

    /**
     * The version of the local timezone resource file, ie. "2007c". Since the version is determined
     * by a <code>ICUFile.findFileTZVersion</code>, this field must be initialized by the
     * constructor.
     */
    public static String TZ_LOCAL_VERSION = null;

    /**
     * The local timezone resource file.
     */
    public static File tzLocalFile = null;

    /**
     * The current logger.
     */
    private Logger logger;

    /**
     * The currently selected timezone resource name. Initially set to <code>TZ_LOCAL_CHOICE</code>.
     */
    private Object selected = TZ_LOCAL_CHOICE;

    /**
     * The map of timezone resource names to their respective URL locations.
     */
    private TreeMap urlMap = new TreeMap();

    /**
     * Constructs a source model.
     * 
     * @param logger
     *            The current logger.
     * @param tzLocalFile
     *            The local timezone resource file.
     */
    public SourceModel(Logger logger, File tzLocalFile) {
        this.logger = logger;

        // if all constants are not yet initialized
        // (this is where they get initialized)
        if (TZ_BASE_URL == null) {
            try {
                TZ_BASE_URL = new URL(TZ_BASE_URLSTRING_START);

                SourceModel.tzLocalFile = tzLocalFile;
                if (!tzLocalFile.exists()) {
                    // not a critical error, but we won't be able to use the
                    // local tz file
                    logger.errorln("Local copy (zoneinfo.res) does not exist (perhaps you are not running icutzu from the correct directory?)");
                } else {
                    TZ_LOCAL_URL = tzLocalFile.toURL();
                    TZ_LOCAL_VERSION = ICUFile.findFileTZVersion(tzLocalFile, logger);
                    if (TZ_LOCAL_VERSION == null) {
                        logger.errorln("Failed to determine version of local copy");
                        TZ_LOCAL_CHOICE = "Local Copy";
                    } else {
                        TZ_LOCAL_CHOICE = "Local Copy (" + TZ_LOCAL_VERSION + ")";
                    }
                    selected = TZ_LOCAL_CHOICE;
                }
            } catch (MalformedURLException ex) {
                // this shouldn't happen
                logger.errorln("Internal program error.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Gathers all the names and urls of timezone resources available on the ICU Timezone
     * Repository. Also sets the selected item in this list to be the best timezone version
     * available.
     */
    public void findSources() {
        BufferedReader reader = null;
        try {
            URLConnection con = TZ_BASE_URL.openConnection();
            con.setRequestProperty("user-agent", System.getProperty("http.agent"));
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            // create an html callback function to parse through every list item
            // (every list item
            HTMLEditorKit.ParserCallback htmlCallback = new HTMLEditorKit.ParserCallback() {
                private boolean listItem = false;

                public void handleEndTag(HTML.Tag tag, int pos) {
                    if (tag == HTML.Tag.LI)
                        listItem = false;
                }

                public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos) {
                    if (tag == HTML.Tag.LI)
                        listItem = true;
                }

                public void handleText(char[] data, int pos) {
                    if (listItem) {
                        String str = new String(data);
                        if (str.charAt(str.length() - 1) == '/')
                            str = str.substring(0, str.length() - 1);
                        if (!"..".equals(str))
                            try {
                                // add the new item to the map
                                urlMap.put(str, new URL(TZ_BASE_URLSTRING_START + str
                                        + TZ_BASE_URLSTRING_END));

                                // update the selected item and fire off an
                                // event
                                selected = urlMap.lastKey();
                                int index = 0;
                                for (Iterator iter = urlMap.keySet().iterator(); iter.hasNext();) {
                                    if (iter.next().equals(str))
                                        index++;
                                }
                                fireIntervalAdded(this, index, index);
                            } catch (MalformedURLException ex) {
                                logger.errorln("Internal program error.");
                                ex.printStackTrace();
                            }
                    }
                }
            };

            new ParserDelegator().parse(reader, htmlCallback, false);
        } catch (IOException ex) {
            // cannot connect to the repository -- use local version only.
            String message = "Failed to connect to the ICU Timezone Repository at "
                    + TZ_BASE_URL.toString()
                    + " .\n\n"
                    + "Check your connection and re-run ICUTZU or continue using the local copy of the timezone update (version "
                    + TZ_LOCAL_VERSION + ").";
            logger.showInformationDialog(message);
        } finally {
            // close the reader gracefully
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException ex) {
                }
        }
    }

    /**
     * Returns the name of the timezone resource at the given index.
     * 
     * @param index
     *            The given index.
     * @return The name of the timezone resource at the given index.
     */
    public Object getElementAt(int index) {
        if (index == 0)
            return TZ_LOCAL_CHOICE;
        else if (index < 0 || index > urlMap.size())
            return null;
        else {
            Iterator iter = urlMap.keySet().iterator();
            for (int i = 1; i < index; i++)
                iter.next();
            return iter.next();
        }
    }

    /**
     * Returns the selected timezone resource name. If <code>setSelectedItem</code> is never
     * called externally, then this is also the best available timezone resource (the most recent
     * version).
     * 
     * @return The selected timezone resource name.
     */
    public Object getSelectedItem() {
        return selected;
    }

    /**
     * Gets the number of timezone resources currently stored by the source model. There will always
     * be at least one.
     * 
     * @return The number of timezone resources.
     */
    public int getSize() {
        // the added size (+1) is due to the local copy not being inside the map
        return urlMap.size() + 1;
    }

    /**
     * Returns the URL mapped to by the given timezone resource name.
     * 
     * @param choice
     *            The version name, which should be a String.
     * @return The URL mapped to by the given timezone resource name.
     */
    public URL getURL(Object choice) {
        if (choice == null || !(choice instanceof String))
            return null;
        else if (TZ_LOCAL_CHOICE.equalsIgnoreCase((String) choice))
            return TZ_LOCAL_URL;
        else
            return (URL) urlMap.get(choice);
    }

    /**
     * Returns the version of a timezone resource with the given name. Usually this is exactly the
     * same as the name, but if the name is <code>TZ_LOCAL_CHOICE</code> (ignoring case), then the
     * version is <code>TZ_LOCAL_VERSION</code>.
     * 
     * @param choice
     *            The version name, which should be a String.
     * @return The URL mapped to by the given version name.
     */
    public String getVersion(Object choice) {
        if (choice == null || !(choice instanceof String))
            return null;
        else if (TZ_LOCAL_CHOICE.equalsIgnoreCase((String) choice))
            return TZ_LOCAL_VERSION;
        else
            return (String) choice;
    }

    /**
     * Returns an iterator on the entry set of the url map. Each item iterated will be of type
     * Map.Entry, whos key is a String and value is a URL.
     * 
     * @return An iterator as described above.
     */
    public Iterator iterator() {
        return urlMap.entrySet().iterator();
    }

    /**
     * Sets the selected timezone resource name.
     * 
     * @param selected
     *            The timezone resource name to be selected.
     */
    public void setSelectedItem(Object selected) {
        this.selected = selected;
    }
}
