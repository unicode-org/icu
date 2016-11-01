/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.io.*;
import javax.swing.*;

import com.ibm.rbm.gui.RBManagerGUI;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
 * <P>This is the super class for all importer plug-in classes.</P>
 * <P>
 * In terms of general functionality of this class or its children classes, the following steps should happen in order:
 * <OL>
 * <LI>A Dialog is shown from which the user may select options about the import, including the file from which to import.</LI>
 * <LI>The 'Import' button is pressed, closing the options dialog and opening a progress bar dialog box.</LI>
 * <LI>The class should resolve all conflicts with locale encodings existing in the import files, but not in the active resource bundle.</LI>
 * <LI>The class should parse resources one at a time and use the importResource() method to insert them into the resource bundle.</LI>
 * <LI>The class should report when all resources have been read and the import is complete.</LI>
 * </OL>
 * </P>
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class RBImporter extends JDialog {
    private final static int FILE_OPTION_POPULATE      = 0;            // Create a new locale file populated from base file
    private final static int FILE_OPTION_EMPTY         = 1;            // Create a new empty locale file
    private final static int FILE_OPTION_IGNORE        = 2;            // Ignore all resources from this encoding
    private final static int FILE_OPTION_PROMPT        = 3;            // Prompt for each conflict
    private final static int RESOURCE_OPTION_OVERWRITE = 0;            // Use the value from the source import file
    private final static int RESOURCE_OPTION_IGNORE    = 1;            // Ignore the import and use existing value
    private final static int RESOURCE_OPTION_PROMPT    = 2;            // Propmpt for each conflict
	
    protected static JFileChooser chooser;
    protected int    num_conflicts;
    protected int    num_extra_files;
    protected String title;
    protected RBManager rbm;
    protected RBManagerGUI gui;
    protected boolean pathSet = false;
	
    // Visual Components
    JRadioButton resourceOverwriteRadio    = new JRadioButton(Resources.getTranslation("import_resource_conflict_overwrite"), false);
    JRadioButton resourceIgnoreRadio       = new JRadioButton(Resources.getTranslation("import_resource_conflict_ignore"), false);
    JRadioButton resourcePromptRadio       = new JRadioButton(Resources.getTranslation("import_conflict_prompt"), true);
    JRadioButton fileGeneratePopulateRadio = new JRadioButton(Resources.getTranslation("import_file_conflict_generate_populate"), false);
    JRadioButton fileGenerateEmptyRadio    = new JRadioButton(Resources.getTranslation("import_file_conflict_generate_empty"), false);
    JRadioButton fileIgnoreRadio           = new JRadioButton(Resources.getTranslation("import_file_conflict_ignore"), false);
    JRadioButton filePromptRadio           = new JRadioButton(Resources.getTranslation("import_conflict_prompt"), true);
	
    JCheckBox markTranslatedCheck = new JCheckBox(Resources.getTranslation("import_default_translated"), true);
    JCheckBox createGroupsCheck = new JCheckBox(Resources.getTranslation("import_default_group_creation"), true);
    JComboBox groupComboBox = new JComboBox();
	
    JLabel sourceLabel;
	
    JDialog progressBarDialog;
    JProgressBar progressBar;
    
    /**
     * Constructor
     * @param title The title that appears in the Dialog box
     * @param rbm An RBManager instance
     * @param gui The RBManager GUI instance associated with the RBManager instance
     */
    
    public RBImporter(String title, RBManager rbm, RBManagerGUI gui) {
        super(new Frame(), title, true);
        this.title = title;
        this.rbm = rbm;
        this.gui = gui;
        init();
    }
	
    protected void init() {
        chooser = new JFileChooser();
        setupFileChooser();
        num_conflicts = 0;
        num_extra_files = 0;
        initComponents();
        setVisible(true);
    }
	
    protected void setupFileChooser() {
        // To be overwritten
    }
	
    protected void beginImport() throws IOException {
        // To be overwritten
        if (!pathSet)
            throw new IOException("Path not set yet");
    }
	
    protected void chooseFile() {
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            sourceLabel.setText(Resources.getTranslation("import_source_file",f.getAbsolutePath()));
            pathSet = true;
        }
    }
	
    protected File getChosenFile() {
        return chooser.getSelectedFile();
    }
	
    /**
     * A super class method intended for use of nearly all subclass importers, once a resource
     * is found by those subclasses. This method is called in order to create the new resource
     * and handle the various conflict errors that may result as a part of that import.
     */
	
    protected void importResource(BundleItem item, String encoding, String group_name) {
        Bundle bundle = null;
        BundleGroup group = null;
        BundleGroup backup_group = null;
		
        if (group_name == null)
        	group_name = getDefaultGroup();
        if (encoding == null)
        	return;
        // Get the bundle to which we will be adding this resource
        bundle = rbm.getBundle(encoding);
        // Skip this import if the bundle is non-existent (Should have been resolved if wanted)
        if (bundle == null)
        	return;
        // Find the group in the bundle, Ungrouped if non-existent
        Vector gv = bundle.getGroupsAsVector();
        for (int i=0; i < gv.size(); i++) {
            BundleGroup tempg = (BundleGroup)gv.elementAt(i);
            if (i==0) backup_group = tempg;
            if (tempg.getName().equals("Ungrouped Items")) backup_group = tempg;
            else if (tempg.getName().equals(group_name)) {
                group = tempg;
                break;
            }
        }
        if (group == null) {
            if (getDefaultGroupCreation()) {
                // Create a new group by this name
                bundle.addBundleGroup(group_name, "");
                gv = bundle.getGroupsAsVector();
                for (int i=0; i < gv.size(); i++) {
                    BundleGroup tempg = (BundleGroup)gv.elementAt(i);
                    if (tempg.getName().equals(group_name)) {
                        group = tempg;
                        break;
                    }
                }
            } else {
                // Use the backup_group
                group = backup_group;
            }
        }
        // If all group identification efforts fail, we fail
        if (group == null)
        	return;
        item.setParentGroup(group);
        // Check for and resolve conflicts
        if (bundle.allItems.containsKey(item.getKey())) {
            resolveResource(bundle,item); 
            RBManagerGUI.debugMsg("Resolve conflict");
        } else {
            // Insert the resource
            bundle.addBundleItem(item);
        }
    }
    
    /**
     * This method should be called when trying to import and item whose key all ready exists within the bundle.
     */
	
    protected void resolveResource(Bundle bundle, BundleItem item) {
        if (this.getResourceConflictOption() == RESOURCE_OPTION_IGNORE)
        	return;
        else if (this.getResourceConflictOption() == RESOURCE_OPTION_OVERWRITE) {
            bundle.removeItem(item.getKey());
            bundle.addBundleItem(item);
        } else if (this.getResourceConflictOption() == RESOURCE_OPTION_PROMPT) {
            BundleItem original = (BundleItem)bundle.allItems.get(item.getKey());
            if (original == null)
            	return;
            String trans = original.getTranslation();
            String options[] = { Resources.getTranslation("import_resource_conflict_overwrite"),
                                 Resources.getTranslation("import_resource_conflict_ignore")};
            String insert[] = {item.getKey(), (bundle.encoding.equals("") ? "(Base Class)" : bundle.encoding)};
            String result = (String)JOptionPane.showInputDialog(this,  Resources.getTranslation("import_resource_conflict_choose", insert) + 
                "\n" + Resources.getTranslation("import_resource_conflict_choose_source", item.getTranslation()) +
                "\n" + Resources.getTranslation("import_resource_conflict_choose_target", trans),
                Resources.getTranslation("import_file_conflicts"), JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
            if (result == null)
            	return;
            if (result.equals(Resources.getTranslation("import_resource_conflict_overwrite"))) {
                bundle.removeItem(item.getKey());
                bundle.addBundleItem(item);
            } else if (result.equals(Resources.getTranslation("import_resource_conflict_ignore")))
            	return;
        }
    }
	
    /**
     * Given a vector of strings containing locale encodings (e.g. {"en", "en_us", "de"}), attempts
     * to resolve those conflicts according to the preferences selected by the user.
     */
	
    protected void resolveEncodings(Vector v) {
        for (int i=0; i < v.size(); i++) {
            String encoding = (String)v.elementAt(i);
            if (encoding == null || encoding.equals("") || rbm.hasResource(encoding)) {
                continue;
            }

            // We need to resolve this conflict
            if (this.getFileConflictOption() == FILE_OPTION_IGNORE) continue;
            else if (this.getFileConflictOption() == FILE_OPTION_POPULATE) {
                rbm.createResource(null, null, null, encoding, null, null, null, true);
            } else if (this.getFileConflictOption() == FILE_OPTION_EMPTY) {
                rbm.createResource(null, null, null, encoding, null, null, null, true);
            } else if (this.getFileConflictOption() == FILE_OPTION_PROMPT) {
                String options[] = { Resources.getTranslation("import_file_conflict_generate_populate"),
                                     Resources.getTranslation("import_file_conflict_generate_empty"),
                                     Resources.getTranslation("import_file_conflict_ignore")};
				
                String result = (String)JOptionPane.showInputDialog(this, Resources.getTranslation("import_file_conflict_choose", encoding),
                    Resources.getTranslation("import_file_conflicts"), JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
                if (result == null) continue;
                if (result.equals(Resources.getTranslation("import_file_conflict_ignore"))) continue;
                else if (result.equals(Resources.getTranslation("import_file_conflict_generate_populate"))) {
                    rbm.createResource(null, null, null, encoding, null, null, null, true);
                } else if (result.equals(Resources.getTranslation("import_file_conflict_generate_empty"))) {
                    rbm.createResource(null, null, null, encoding, null, null, null, false);
                }
            }
        }
        gui.updateDisplayTree();
    }
	
    // Returns an integer mask describing the user's selection for file resolving missing file locale conflicts
	
    private int getFileConflictOption() {
        if (fileGeneratePopulateRadio.isSelected()) return FILE_OPTION_POPULATE;
        if (fileGenerateEmptyRadio.isSelected()) return FILE_OPTION_EMPTY;
        if (fileIgnoreRadio.isSelected()) return FILE_OPTION_IGNORE;
        if (filePromptRadio.isSelected()) return FILE_OPTION_PROMPT;
        return FILE_OPTION_PROMPT;
    }
	
    // Returns an integer mask describing the user's selection for duplicate resource key conflicts
	
    private int getResourceConflictOption() {
        if (resourceOverwriteRadio.isSelected()) return RESOURCE_OPTION_OVERWRITE;
        if (resourceIgnoreRadio.isSelected()) return RESOURCE_OPTION_IGNORE;
        if (resourcePromptRadio.isSelected()) return RESOURCE_OPTION_PROMPT;
        return RESOURCE_OPTION_PROMPT;
    }
	
    // Returns the group name for use when no group name is specified
	
    protected String getDefaultGroup() {
        return groupComboBox.getSelectedItem().toString();
    }
	
    // Returns the default translation value
	
    protected boolean getDefaultTranslated() {
        return markTranslatedCheck.isSelected();
    }
	
    // Returns whether or not a group of name non-existant in the active bundle is created
	
    protected boolean getDefaultGroupCreation() {
        return createGroupsCheck.isSelected();
    }
	
    protected void showProgressBar(int steps) {
        thisWindowClosing();
        JDialog progressBarDialog = new JDialog(this, Resources.getTranslation("dialog_title_import_progress"), false);
        JProgressBar progressBar = new JProgressBar(0, steps);
        progressBar.setValue(0);
        progressBarDialog.getContentPane().add(progressBar);
        progressBarDialog.pack();
        progressBarDialog.setVisible(true);
    }
	
    protected void incrementProgressBar() {
        if (progressBar == null) return;
        progressBar.setValue(progressBar.getValue()+1);
        if (progressBar.getValue() == progressBar.getMaximum()) hideProgressBar();
    }
	
    protected void hideProgressBar() {
        if (progressBarDialog != null) progressBarDialog.setVisible(false);
    }
	
    /**
     * Initialize the visual components for selecting an import file and setting the appropriate
     * options
     */
	
    protected void initComponents() {
        // Create Components
        JLabel titleLabel       = new JLabel(title);
        sourceLabel             = new JLabel(Resources.getTranslation("import_source_file","--"));
        JLabel insertGroupLabel = new JLabel(Resources.getTranslation("import_insert_group"));
        
        JButton fileChooseButton = new JButton(Resources.getTranslation("button_choose"));
        JButton cancelButton     = new JButton(Resources.getTranslation("button_cancel"));
        JButton importButton     = new JButton(Resources.getTranslation("button_import"));
        
        ButtonGroup resourceGroup = new ButtonGroup();
        ButtonGroup fileGroup = new ButtonGroup();
		
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel midPanel = new JPanel(new BorderLayout());
        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JPanel topInnerPanel = new JPanel(new BorderLayout());
        
        Box midBox = new Box(BoxLayout.Y_AXIS);
		
        JPanel resourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel filePanel     = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel defaultPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel defaultPanel2 = new JPanel(new BorderLayout());
        
        Box resourceBox = new Box(BoxLayout.Y_AXIS);
        Box fileBox     = new Box(BoxLayout.Y_AXIS);
        Box groupBox    = new Box(BoxLayout.X_AXIS);
		
        // Setup title
        titleLabel.setFont(new Font("Serif",Font.BOLD,16));
		
        // Setup panels
        midPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("import_options")));
        resourcePanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("import_resource_conflicts")));
        filePanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("import_file_conflicts")));
        defaultPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("import_default_values")));
		
        // Arrange button groups
        fileGroup.add(fileGeneratePopulateRadio);
        fileGroup.add(fileGenerateEmptyRadio);
        fileGroup.add(fileIgnoreRadio);
        fileGroup.add(filePromptRadio);
        resourceGroup.add(resourceOverwriteRadio);
        resourceGroup.add(resourceIgnoreRadio);
        resourceGroup.add(resourcePromptRadio);
		
        // Add action listeners
        cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                thisWindowClosing();
            }
        });
		
        importButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                try {
                    beginImport();
                    gui.updateProjectTree();
                    gui.updateDisplayTree();
                    thisWindowClosing();
                } catch (IOException ioe) {
                	ioe.printStackTrace(System.err);
                    JOptionPane.showMessageDialog(null,
                        Resources.getTranslation("error") + "\n" + ioe.getLocalizedMessage(),
                        Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);	
                }
            }
        });
		
        fileChooseButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev) {
                chooseFile();
            }
        });
		
        // Setup combo box
        Bundle baseBundle = ((Bundle)rbm.getBundles().elementAt(0));
        BundleGroup ungroupedGroup = baseBundle.getUngroupedGroup();
        groupComboBox = new JComboBox(baseBundle.getGroupsAsVector());
        int groupComboBoxCount = groupComboBox.getItemCount();
        for (int selectedIndex = 0; selectedIndex < groupComboBoxCount; selectedIndex++) {
        	BundleGroup bundGroup = ((BundleGroup)groupComboBox.getItemAt(selectedIndex));
        	if (bundGroup.getName().equals(ungroupedGroup.getName())) {
        		// By default, use the ungrouped group. Probably named 'Ungrouped Items'.
        		groupComboBox.setSelectedIndex(selectedIndex);
        		break;
        	}
        }
		
        // Arange components
        groupBox.add(Box.createHorizontalGlue());
        groupBox.add(insertGroupLabel);
        groupBox.add(Box.createHorizontalStrut(5));
        groupBox.add(groupComboBox);
		
        defaultPanel2.add(groupBox, BorderLayout.NORTH);
        defaultPanel2.add(markTranslatedCheck, BorderLayout.CENTER);
        defaultPanel2.add(createGroupsCheck, BorderLayout.SOUTH);
		
        fileBox.add(fileGeneratePopulateRadio);
        fileBox.add(fileGenerateEmptyRadio);
        fileBox.add(fileIgnoreRadio);
        fileBox.add(filePromptRadio);
		
        resourceBox.add(resourceOverwriteRadio);
        resourceBox.add(resourceIgnoreRadio);
        resourceBox.add(resourcePromptRadio);
		
        defaultPanel.add(defaultPanel2);
        filePanel.add(fileBox);
        resourcePanel.add(resourceBox);
		
        midBox.add(resourcePanel);
        midBox.add(filePanel);
        midBox.add(defaultPanel);
            
        midPanel.add(midBox, BorderLayout.CENTER);
		
        topInnerPanel.add(sourceLabel, BorderLayout.CENTER);
        topInnerPanel.add(fileChooseButton, BorderLayout.EAST);
		
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(topInnerPanel, BorderLayout.CENTER);
		
        botPanel.add(cancelButton);
        botPanel.add(importButton);
		
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(midPanel, BorderLayout.CENTER);
        getContentPane().add(botPanel, BorderLayout.SOUTH);
		
        pack();
    }
	
    protected void thisWindowClosing() {
        setVisible(false);
        dispose();
    }
}