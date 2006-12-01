/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import com.ibm.rbm.*;

/**
 * The class used to display statistics
 */
class RBStatisticsPanel extends JPanel {
	RBManager rbm;
	Bundle bundle;
	
	// Components - Bundle
	JLabel     jLabelStatsTitle;
		
	JLabel     jLabelStatsName;
	JLabel     jLabelStatsComment;
	JLabel     jLabelStatsManager;
	JLabel     jLabelStatsLanguage;
	JLabel     jLabelStatsCountry;
	JLabel     jLabelStatsVariant;
	JLabel     jLabelStatsNumTrans;
	JLabel     jLabelStatsNumUntrans;
		
	JTextField jTextFieldStatsName;
	JTextField jTextFieldStatsComment;
	JTextField jTextFieldStatsManager;
	JTextField jTextFieldStatsLanguage;
	JTextField jTextFieldStatsCountry;
	JTextField jTextFieldStatsVariant;
	
	JButton    updateButton;
	
	Box        boxStatsLeftRight1;
	Box        boxStatsLeftRight2;
	
	// Components - bundle manager
	JLabel      titleLabel;
	JLabel      numFileLabel;
	JLabel      numDupLabel;
	JLabel      numGroupLabel;
	JLabel      numItemLabel;
	
	JList       groupList;
	JList       fileList;
	JList       dupList;
	
	JScrollPane groupScroll;
	JScrollPane dupScroll;
	JScrollPane fileScroll;
	
	JPanel      filePanel;
	JPanel      itemPanel;
	JPanel      groupPanel;
	
	JButton     fileButton;
	JButton     groupButton;
	JButton     itemButton;
	
	Box         mainBox;
	Box         dupBox;
	
	
	public void setBundle(Bundle b) {
		rbm = null;
		if (bundle == null) {
			bundle = b;
			initComponents();
		} else if (bundle != b) {
			bundle = b;
			updateComponents();
		}
	}
	
	public void setManager(RBManager m) {
		bundle = null;
		if (rbm == null) {
			rbm = m;
			initComponents();
		} else if (rbm != m) {
			rbm = m;
			updateComponents();
		}
	}
	
	public void removeElements() {
		if (rbm != null || bundle != null) {
			rbm = null;
			bundle = null;
			initComponents();
		}
	}	
		
	public void initComponents() {
		// Initialize components
		if (bundle != null) {
			RBManagerGUI.debugMsg("Initializing components for Resource File");
			int untranslated = bundle.getUntranslatedItemsSize();
			
			jLabelStatsTitle          = new JLabel(bundle.name);
		
			jLabelStatsName           = new JLabel(Resources.getTranslation("languagestats_title"));
			jLabelStatsComment        = new JLabel(Resources.getTranslation("languagestats_comment"));
			jLabelStatsManager        = new JLabel(Resources.getTranslation("languagestats_manager"));
			jLabelStatsLanguage       = new JLabel(Resources.getTranslation("languagestats_language"));
			jLabelStatsCountry        = new JLabel(Resources.getTranslation("languagestats_country"));
			jLabelStatsVariant        = new JLabel(Resources.getTranslation("languagestats_variant"));
			jLabelStatsNumTrans       = new JLabel(Resources.getTranslation("languagestats_item_count") + " " +
												   String.valueOf(bundle.allItems.size()));
			jLabelStatsNumUntrans     = new JLabel(Resources.getTranslation("languagestats_translation_count") + 
												   String.valueOf(untranslated));
		
			jTextFieldStatsName       = new JTextField((bundle.name == null ? Resources.getTranslation("untitled") : bundle.name));
			jTextFieldStatsComment    = new JTextField((bundle.comment == null ? "" : bundle.comment));
			jTextFieldStatsManager    = new JTextField((bundle.manager == null ? "" : bundle.manager));
			jTextFieldStatsLanguage   = new JTextField((bundle.language == null ? "" : bundle.language),25);
			jTextFieldStatsCountry    = new JTextField((bundle.country == null ? "" : bundle.country),25);
			jTextFieldStatsVariant    = new JTextField((bundle.variant == null ? "" : bundle.variant),25);
		
			boxStatsLeftRight1        = new Box(BoxLayout.X_AXIS);
			boxStatsLeftRight2        = new Box(BoxLayout.X_AXIS);	
		
			updateButton              = new JButton(Resources.getTranslation("button_update"));
			updateButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_update_trigger")));
			
			// Set up the components
			jLabelStatsTitle.setFont(new Font("SansSerif",Font.PLAIN,18));
			
			ButtonEnablerFocusListener befl = new ButtonEnablerFocusListener(updateButton);
			
			// Add listeners
			updateButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					updateBundleInfo();
				}
			});
			
			jTextFieldStatsName.addFocusListener(befl);
			jTextFieldStatsComment.addFocusListener(befl);
			jTextFieldStatsManager.addFocusListener(befl);
			jTextFieldStatsLanguage.addFocusListener(befl);
			jTextFieldStatsCountry.addFocusListener(befl);
			jTextFieldStatsVariant.addFocusListener(befl);
			
			jTextFieldStatsName.setColumns(35);
			jTextFieldStatsComment.setColumns(35);
			jTextFieldStatsManager.setColumns(35);
			jTextFieldStatsLanguage.setColumns(25);
			jTextFieldStatsCountry.setColumns(25);
			jTextFieldStatsVariant.setColumns(25);
			
			//updateButton.setEnabled(false);
			
			// Update the display
			if (mainBox != null){
				mainBox.removeAll();
			} else {
				mainBox = new Box(BoxLayout.Y_AXIS);
			}
			if (dupBox != null)
				dupBox.removeAll();
			removeAll();
			mainBox.add(jLabelStatsTitle);
			mainBox.add(Box.createVerticalStrut(10));
			mainBox.add(jLabelStatsName);
			mainBox.add(jTextFieldStatsName);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsComment);
			mainBox.add(jTextFieldStatsComment);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsManager);
			mainBox.add(jTextFieldStatsManager);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsLanguage);
			mainBox.add(jTextFieldStatsLanguage);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsCountry);
			mainBox.add(jTextFieldStatsCountry);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsVariant);
			mainBox.add(jTextFieldStatsVariant);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsNumTrans);
			mainBox.add(Box.createVerticalStrut(5));
			mainBox.add(jLabelStatsNumUntrans);
			mainBox.add(Box.createVerticalStrut(10));
			mainBox.add(updateButton);
			mainBox.add(Box.createHorizontalGlue());
			if (!(getLayout() instanceof FlowLayout)) {
				setLayout(new FlowLayout());
			}
			add(mainBox);
		} else if (rbm != null) {
			RBManagerGUI.debugMsg("Initializing components for Resource Bundle");
			titleLabel          = new JLabel(rbm.getBaseClass() + " - " + Resources.getTranslation("baseclass"));
		
			numFileLabel        = new JLabel(Resources.getTranslation("basestats_file_count") + " " + rbm.getNumberLanguages());
			numGroupLabel       = new JLabel(Resources.getTranslation("basestats_group_count") + " " + rbm.getNumberGroups());
			numItemLabel        = new JLabel(Resources.getTranslation("basestats_item_count") + " " + rbm.getNumberTotalTranslations());
			numDupLabel         = new JLabel(Resources.getTranslation("basestats_duplicates_count") + " " + rbm.getNumberDuplicates());
			
			fileList            = new JList(rbm.getLanguageListingVector());
			groupList           = new JList(rbm.getGroupListingVector());
			dupList             = new JList(rbm.getDuplicatesListingVector());
			
			fileButton          = new JButton(Resources.getTranslation("button_add_file"));
			groupButton         = new JButton(Resources.getTranslation("button_add_group"));
			itemButton          = new JButton(Resources.getTranslation("button_add_resource"));
			
			filePanel           = new JPanel();
			groupPanel          = new JPanel();
			itemPanel           = new JPanel();
			
			fileScroll          = new JScrollPane(fileList);
			groupScroll         = new JScrollPane(groupList);
			dupScroll           = new JScrollPane(dupList);
			
			if (mainBox == null) {
				mainBox         = new Box(BoxLayout.Y_AXIS);
			} else {
				mainBox.removeAll();
			}
			dupBox              = new Box(BoxLayout.Y_AXIS);
			
			// Set up the components
			filePanel.setLayout(new BorderLayout());
			groupPanel.setLayout(new BorderLayout());
			itemPanel.setLayout(new BorderLayout());
			
			filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																 Resources.getTranslation("basestats_file_group")));
			groupPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																 Resources.getTranslation("basestats_group_group")));
			itemPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																 Resources.getTranslation("basestats_item_group")));
			
			titleLabel.setFont(new Font("SansSerif",Font.PLAIN,18));
			
			fileButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_add_file_trigger")));
			groupButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_add_group_trigger")));
			itemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_add_resource_trigger")));
			
			// Add listeners
			fileButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					Container c = ((JButton)ev.getSource()).getParent();
					RBManagerGUI gui = null;
					while (!(c.getParent() instanceof RBManagerGUI)) c = c.getParent();
					gui = (RBManagerGUI)c.getParent();
					gui.createResourceFile();
				}
			});
			
			groupButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					Container c = ((JButton)ev.getSource()).getParent();
					RBManagerGUI gui = null;
					while (!(c.getParent() instanceof RBManagerGUI)) c = c.getParent();
					gui = (RBManagerGUI)c.getParent();
					gui.createBundleGroup();
				}
			});
			
			itemButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					Container c = ((JButton)ev.getSource()).getParent();
					RBManagerGUI gui = null;
					while (!(c.getParent() instanceof RBManagerGUI)) c = c.getParent();
					gui = (RBManagerGUI)c.getParent();
					gui.createBundleItem();
				}
			});
			
			// Update the display
			filePanel.removeAll();
			filePanel.add(numFileLabel, BorderLayout.NORTH);
			filePanel.add(fileScroll, BorderLayout.CENTER);
			filePanel.add(fileButton, BorderLayout.SOUTH);
			
			groupPanel.removeAll();
			groupPanel.add(numGroupLabel, BorderLayout.NORTH);
			groupPanel.add(groupScroll, BorderLayout.CENTER);
			groupPanel.add(groupButton, BorderLayout.SOUTH);
			
			dupBox.removeAll();
			dupBox.add(numDupLabel);
			dupBox.add(dupScroll);
			
			itemPanel.removeAll();
			itemPanel.add(numItemLabel, BorderLayout.NORTH);
			itemPanel.add(dupBox, BorderLayout.CENTER);
			itemPanel.add(itemButton, BorderLayout.SOUTH);
			
			mainBox.removeAll();
			mainBox.add(titleLabel);
			mainBox.add(Box.createVerticalStrut(10));
			mainBox.add(filePanel);
			mainBox.add(Box.createVerticalStrut(10));
			mainBox.add(groupPanel);
			mainBox.add(Box.createVerticalStrut(10));
			mainBox.add(itemPanel);
			
			removeAll();
			if (!(getLayout() instanceof BorderLayout))
				setLayout(new BorderLayout());
			add(mainBox, BorderLayout.CENTER);
			updateComponents();
		} else {
			removeAll();	
		}
		repaint();
	}
	
	public void updateComponents() {
		if (bundle != null) {
			int untranslated = bundle.getUntranslatedItemsSize();
			
			jLabelStatsTitle.setText(bundle.name);
		
			jTextFieldStatsName.setText(bundle.name == null ? Resources.getTranslation("untitled") : bundle.name);
			jTextFieldStatsComment.setText(bundle.comment == null ? "" : bundle.comment);
			jTextFieldStatsManager.setText(bundle.manager == null ? "" : bundle.manager);
			jTextFieldStatsLanguage.setText(bundle.language == null ? "" : bundle.language);
			jTextFieldStatsCountry.setText(bundle.country == null ? "" : bundle.country);
			jTextFieldStatsVariant.setText(bundle.variant == null ? "" : bundle.variant);
			jLabelStatsNumTrans.setText(Resources.getTranslation("languagestats_item_count") + " " +
					   String.valueOf(bundle.allItems.size()));
			jLabelStatsNumUntrans.setText(Resources.getTranslation("languagestats_translation_count") + 
					   String.valueOf(untranslated));
		} else if (rbm == null) {
			removeAll();
		}
		
	}
	
	void updateBundleInfo() {
		bundle.name     = jTextFieldStatsName.getText().trim();
		bundle.comment  = jTextFieldStatsComment.getText().trim();
		bundle.manager  = jTextFieldStatsManager.getText().trim();
		bundle.language = jTextFieldStatsLanguage.getText().trim();
		bundle.country  = jTextFieldStatsCountry.getText().trim();
		bundle.variant  = jTextFieldStatsVariant.getText().trim();
		updateButton.setEnabled(false);
	}
	
	public RBStatisticsPanel() {
		super();
		bundle = null;
		rbm = null;
	}
	
}

class ButtonEnablerFocusListener implements FocusListener {
	JButton button;
	String beforeText = null;
	
	public ButtonEnablerFocusListener(JButton button) {
		super();
		this.button = button;
	}
	
	public void focusGained(FocusEvent ev) {
		Object o = ev.getSource();
		if (o instanceof JTextComponent) {
			JTextComponent jtc = (JTextComponent)o;
			beforeText = jtc.getText();
		}
	}
	
	public void focusLost(FocusEvent ev) {
		Object o = ev.getSource();
		if (o instanceof JTextComponent) {
			JTextComponent jtc = (JTextComponent)o;
			String afterText = jtc.getText();
			if (!afterText.equals(beforeText)) button.setEnabled(true);
		} else button.setEnabled(true);
	}
}

