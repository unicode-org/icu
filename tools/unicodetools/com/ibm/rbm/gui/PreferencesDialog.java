/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Locale;

import javax.swing.*;

import com.ibm.rbm.*;

/**
 * Dialog to display to a user about their preferences. 
 */
class PreferencesDialog extends JDialog {
	String userName;
	Locale locale;
	LookAndFeel laf;
	RBManagerGUI gui;
	
	// ** COMPONENTS **
	JTextField   nameField;
	JRadioButton machineRadio;
	JRadioButton definedRadio;
	JRadioButton isoRadio;
	JComboBox    machineCombo;
	JComboBox    definedCombo;
	JComboBox    isoLangCombo;
	JComboBox    isoCounCombo;
	JComboBox    lafCombo;
	JButton      okButton;
	JButton      cancelButton;
	
	public PreferencesDialog(RBManagerGUI gui) {
		super(gui, Resources.getTranslation("dialog_title_preferences"), true);
		this.gui = gui;
		userName = gui.getUser();
		locale = Resources.getLocale();
		laf = UIManager.getLookAndFeel();
		
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			updatePreferences();
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			thisWindowClosing();
		}
	}
	
	private void initComponents() {
		UIManager.LookAndFeelInfo lafi[] = UIManager.getInstalledLookAndFeels();
		String lafn[] = new String[lafi.length];
		for (int i=0; i < lafi.length; i++) {
			lafn[i] = lafi[i].getName();
		}
		
		// COMPONENTS
		
		JPanel panel1 = new JPanel();
		JPanel panel2 = new JPanel();
		JPanel panel3 = new JPanel();
		JPanel panel4 = new JPanel();
		Box    mainBox = new Box(BoxLayout.Y_AXIS);
		Box    localeBox1 = new Box(BoxLayout.Y_AXIS);
		Box    localeBox2 = new Box(BoxLayout.Y_AXIS);
		JPanel localePanel = new JPanel();
		
		Dimension localeDim1 = new Dimension(200,25);
		Dimension localeDim2 = new Dimension(150,25);
		Dimension localeDim3 = new Dimension(50,25);
		
		JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_preferences_username"));
		JLabel lafLabel = new JLabel(Resources.getTranslation("dialog_preferences_lookandfeel"));
		JLabel warnLabel = new JLabel(Resources.getTranslation("dialog_preferences_locale_warning"));
		JLabel underscoreLabel = new JLabel("_");
		
		nameField = new JTextField(userName);
		machineRadio = new JRadioButton(Resources.getTranslation("dialog_preferences_locale_machine"), false);
		definedRadio = new JRadioButton(Resources.getTranslation("dialog_preferences_locale_defined"), true);
		isoRadio = new JRadioButton(Resources.getTranslation("dialog_preferences_locale_iso"), false);
		machineCombo = new JComboBox(Locale.getAvailableLocales());
		definedCombo = new JComboBox(Resources.getAvailableLocales());
		isoLangCombo = new JComboBox(Locale.getISOLanguages());
		isoCounCombo = new JComboBox(Locale.getISOCountries());
		lafCombo = new JComboBox(lafn);
		okButton = new JButton(Resources.getTranslation("button_update"));
		cancelButton = new JButton(Resources.getTranslation("button_cancel"));
		
		machineRadio.setPreferredSize(localeDim1);
		definedRadio.setPreferredSize(localeDim1);
		isoRadio.setPreferredSize(localeDim1);
		
		nameLabel.setPreferredSize(localeDim1);
		lafLabel.setPreferredSize(localeDim1);
		
		//localePanel.setPreferredSize(localeDim2);
		machineCombo.setPreferredSize(localeDim2);
		definedCombo.setPreferredSize(localeDim2);
		
		nameField.setPreferredSize(localeDim2);
		lafCombo.setPreferredSize(localeDim2);
		
		isoLangCombo.setPreferredSize(localeDim3);
		isoCounCombo.setPreferredSize(localeDim3);
		
		// Select the appropriate entries in the combo boxes
		String lafname = UIManager.getLookAndFeel().getName();
		for (int i = 0; i < lafCombo.getItemCount(); i++) {
			if (lafCombo.getItemAt(i).toString().equals(lafname)) {
				lafCombo.setSelectedIndex(i);
				break;
			}
		}
		String locname = Resources.getLocale().toString();
		String loclang = Resources.getLocale().getLanguage();
		String loccoun = Resources.getLocale().getCountry();
		for (int i = 0; i < machineCombo.getItemCount(); i++) {
			if (machineCombo.getItemAt(i).toString().equalsIgnoreCase(locname)) {
				machineCombo.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < definedCombo.getItemCount(); i++) {
			if (definedCombo.getItemAt(i).toString().equalsIgnoreCase(locname)) {
				definedCombo.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < isoLangCombo.getItemCount(); i++) {
			if (isoLangCombo.getItemAt(i).toString().equalsIgnoreCase(loclang)) {
				isoLangCombo.setSelectedIndex(i);
				break;
			}
		}
		for (int i = 0; i < isoCounCombo.getItemCount(); i++) {
			if (isoCounCombo.getItemAt(i).toString().equalsIgnoreCase(loccoun)) {
				isoCounCombo.setSelectedIndex(i);
				break;
			}
		}
		
		// Set the radio button group
		ButtonGroup group = new ButtonGroup();
		group.add(machineRadio);
		group.add(definedRadio);
		group.add(isoRadio);
		
		nameField.setColumns(15);
		
		// Add action listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				thisWindowClosing();
			}
		});
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				updatePreferences();
			}
		});
		getRootPane().setDefaultButton(okButton);
		
		panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
														  Resources.getTranslation("dialog_preferences_locale")));
		panel3.setLayout(new BorderLayout());
		
		localePanel.add(isoLangCombo);
		localePanel.add(underscoreLabel);
		localePanel.add(isoCounCombo);
		
		localeBox1.add(machineRadio);
		localeBox1.add(definedRadio);
		localeBox1.add(isoRadio);
		localeBox2.add(machineCombo);
		localeBox2.add(definedCombo);
		localeBox2.add(localePanel);
		localeBox1.add(Box.createVerticalStrut(5));
		localeBox2.add(Box.createVerticalStrut(5));
		
		panel1.add(nameLabel);
		panel1.add(nameField);
		panel2.add(lafLabel);
		panel2.add(lafCombo);
		panel3.add(localeBox1, BorderLayout.WEST);
		panel3.add(localeBox2, BorderLayout.EAST);
		panel3.add(warnLabel, BorderLayout.SOUTH);
		panel4.add(okButton);
		panel4.add(cancelButton);
		
		mainBox.add(panel1);
		mainBox.add(panel2);
		mainBox.add(panel3);
		mainBox.add(panel4);
		
		getContentPane().add(mainBox);
		//validate();
		pack();
		setVisible(true);
	}
	
	private void thisWindowClosing() {
		setVisible(false);
		dispose();
	}
	
	void updatePreferences() {
		// Set the user name
		gui.setUser(nameField.getText().trim());
		// Set the look and feel
		try {
			UIManager.LookAndFeelInfo lafi[] = UIManager.getInstalledLookAndFeels();
			for (int i=0; i < lafi.length; i++) {
				if (lafi[i].getName().equals(lafCombo.getSelectedItem().toString())) {
					UIManager.setLookAndFeel(lafi[i].getClassName());
					gui.updateUI();
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Could not change the look and feel");
			e.printStackTrace(System.err);
		}
		// Set the locale
		String language = null;
		String country = null;
		String variant = null;
		if (definedRadio.isSelected()) {
			String encoding = "";
			if (definedCombo.getSelectedItem() != null) {
				encoding = definedCombo.getSelectedItem().toString();
			}
			language = Resources.getLanguage(encoding);
			country = Resources.getCountry(encoding);
			variant = Resources.getVariant(encoding);
			RBManagerGUI.debugMsg("Before: " + language + "_" + country + "_" + variant);
			if (country == null) country = new String();
			if (variant == null) locale = new Locale(language, country);
			else locale = new Locale(language, country, variant);
			RBManagerGUI.debugMsg("After: " + locale.toString());
		} else if (machineRadio.isSelected()) {
			String encoding = machineCombo.getSelectedItem().toString();
			language = Resources.getLanguage(encoding);
			country = Resources.getCountry(encoding);
			variant = Resources.getVariant(encoding);
			if (country == null) country = new String();
			if (variant == null) locale = new Locale(language, country);
			else locale = new Locale(language, country, variant);
		} else if (isoRadio.isSelected()) {
			language = isoLangCombo.getSelectedItem().toString();
			country = isoCounCombo.getSelectedItem().toString();
			if (variant == null) locale = new Locale(language, country);
			else locale = new Locale(language, country, variant);
		}
		Resources.setLocale(locale);
		gui.updateLocale(locale);
			
		// Write the preferences
		Preferences.setPreference("username", gui.getUser());
		Preferences.setPreference("lookandfeel", UIManager.getLookAndFeel().getClass().getName());
		Preferences.setPreference("locale", locale.toString());
		try {
			Preferences.savePreferences();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_preferences_save"),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			ioe.printStackTrace(System.err);
		}
		
		// Close the window
		thisWindowClosing();
	}
}
