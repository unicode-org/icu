/*
 *****************************************************************************
 * Copyright (C) 2000-2007, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

import com.ibm.rbm.*;

/**
 * The class used to display untranslated items
 */
class RBSearchPanel extends JPanel {
	RBManager rbm;
	Bundle bundle;
	RBManagerGUI listener;
	
	// Components
	JLabel titleLabel       = new JLabel();
	JLabel findLabel        = new JLabel(Resources.getTranslation("search_find"));
	JLabel replaceLabel     = new JLabel(Resources.getTranslation("search_replace"));
	
	JTextField findField    = new JTextField();
	JTextField replaceField = new JTextField();
	
	JCheckBox keysCheck     = new JCheckBox(Resources.getTranslation("search_keys"), false);
	JCheckBox transCheck    = new JCheckBox(Resources.getTranslation("search_values"), true);
	JCheckBox commentsCheck = new JCheckBox(Resources.getTranslation("search_comments"), false);
	JCheckBox caseCheck     = new JCheckBox(Resources.getTranslation("search_case_sensitive"), false);
	
	JButton findButton      = new JButton(Resources.getTranslation("button_search_find_all"));
	JButton replaceButton   = new JButton(Resources.getTranslation("button_search_replace_all"));
	
	SearchItemsTableModel     model;
	JTable                    table;
	JScrollPane               tableScroll;
	
	public RBSearchPanel(RBManagerGUI gui) {
		super();
		listener = gui;
	}
	
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
	
	protected void performSearch() {
		String search_term = findField.getText().trim();
		if (search_term.length() < 1) return;
		if (bundle != null) {
			performSearch(search_term, bundle, caseCheck.isSelected());
		} else if (rbm != null) {
			performSearch(search_term, (Bundle)rbm.getBundles().elementAt(0), caseCheck.isSelected());
		}
	}
	
	private void performSearch(String term, Bundle bundle, boolean case_sensitive) {
		Vector ret_v = new Vector();
		Enumeration keys = bundle.allItems.keys();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			BundleItem item = (BundleItem)bundle.allItems.get(key);
			if (case_sensitive) {
				if (keysCheck.isSelected() && key.indexOf(term) >= 0) {
					ret_v.addElement(item);
					continue;
				} // end if - keys
				if (transCheck.isSelected() && item.getTranslation().indexOf(term) >= 0) {
					ret_v.addElement(item);
					continue;
				} // end if - translations
				if (commentsCheck.isSelected()) {
					if (item.getComment().indexOf(term) >= 0) {
						ret_v.addElement(item);
						continue;
					}
					Hashtable lookups = item.getLookups();
					Enumeration enum2 = lookups.keys();
					while (enum2.hasMoreElements()) {
						String lookup_key = (String)enum2.nextElement();
						String lookup_value = (String)lookups.get(lookup_key);
						if (lookup_value.indexOf(term) >= 0) {
							ret_v.addElement(item);
							continue;
						}
					} // end while
				} // end if - comments
			} else {
				// Not case sensitive	
				if (keysCheck.isSelected() && key.toUpperCase().indexOf(term.toUpperCase()) >= 0) {
					ret_v.addElement(item);
					continue;
				} // end if - keys
				if (transCheck.isSelected() && item.getTranslation().toUpperCase().indexOf(term.toUpperCase()) >= 0) {
					ret_v.addElement(item);
					continue;
				} // end if - translations
				if (commentsCheck.isSelected()) {
					if (item.getComment().toUpperCase().indexOf(term.toUpperCase()) >= 0) {
						ret_v.addElement(item);
						continue;
					}
					Hashtable lookups = item.getLookups();
					Enumeration enum2 = lookups.keys();
					while (enum2.hasMoreElements()) {
						String lookup_key = (String)enum2.nextElement();
						String lookup_value = (String)lookups.get(lookup_key);
						if (lookup_value.toUpperCase().indexOf(term.toUpperCase()) >= 0) {
							ret_v.addElement(item);
							continue;
						}
					} // end while
				} // end if - comments
			}
		} // end while
		model.setItems(ret_v);
		model.update();
	}
	
	protected void performReplace() {
		String search_term = findField.getText().trim();
		String replace_term = replaceField.getText().trim();
		performSearch();
		if (search_term.length() < 1 || replace_term.length() < 1) return;
		if (keysCheck.isSelected()) {
			JOptionPane.showMessageDialog(this,
				Resources.getTranslation("error_no_key_replace"),
				Resources.getTranslation("warning"), JOptionPane.WARNING_MESSAGE);
		}
		Vector items = model.getBundleItems();
		for (int i=0; i < items.size(); i++) {
			BundleItem item = (BundleItem)items.elementAt(i);
			if (transCheck.isSelected()) {
				item.setTranslation(replace(item.getTranslation(), search_term, replace_term));
			}
			if (commentsCheck.isSelected()) {
				item.setComment(replace(item.getComment(), search_term, replace_term));
			}
		}
		model.update();
	}
	
	// Replaces all instances of match in original with replace
	
	private String replace(String original, String match, String replace) {
		int current_index = -1;
		while (original.indexOf(match,++current_index) >= 0) {
			current_index = original.indexOf(match, current_index);
			original = original.substring(0,current_index) + replace +
					   original.substring(current_index+match.length(), original.length());
		}
		return original;
	}
	
	public void initComponents() {
		// Initialize components
		if (bundle != null) {
			titleLabel.setText(bundle.name);
		}
		else if (rbm != null) {
			titleLabel.setText(rbm.getBaseClass() + " - " + Resources.getTranslation("search"));
		}
		model = new SearchItemsTableModel(new Vector());
		
		titleLabel.setFont(new Font("SansSerif",Font.PLAIN,18));
		
		removeAll();
		setLayout(new BorderLayout());
		table = new JTable(model);
		tableScroll = new JScrollPane(table);
		
		table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		table.addMouseListener(listener);
		
		Dimension dim = new Dimension(75,15);
		
		findField.setColumns(20);
		replaceField.setColumns(20);
		findLabel.setPreferredSize(dim);
		replaceLabel.setPreferredSize(dim);
		
		JPanel innerPanel = new JPanel(new BorderLayout());
		JPanel southPanel = new JPanel();
		JPanel westPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel westPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		Box rightBox = new Box(BoxLayout.Y_AXIS);
		Box leftBox = new Box(BoxLayout.Y_AXIS);
		
		// Add action listeners
		findButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ev) {
				performSearch();
			}
		});
		
		replaceButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ev) {
				performReplace();
			}
		});
		
		findButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_search_find_all_trigger")));
		replaceButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_search_replace_all_trigger")));
		
		// Place components
		westPanel1.add(findLabel);
		westPanel1.add(Box.createHorizontalStrut(5));
		westPanel1.add(findField);
		
		westPanel2.add(replaceLabel);
		westPanel2.add(Box.createHorizontalStrut(5));
		westPanel2.add(replaceField);
		
		leftBox.add(Box.createVerticalGlue());
		leftBox.add(westPanel1);
		leftBox.add(westPanel2);
		//leftBox.add(caseCheck);
		
		rightBox.add(keysCheck);
		rightBox.add(transCheck);
		rightBox.add(commentsCheck);
		
		southPanel.add(findButton);
		southPanel.add(Box.createHorizontalStrut(5));
		southPanel.add(replaceButton);
		southPanel.add(Box.createHorizontalStrut(10));
		southPanel.add(caseCheck);
		
		innerPanel.add(titleLabel, BorderLayout.NORTH);
		innerPanel.add(leftBox, BorderLayout.CENTER);
		innerPanel.add(rightBox, BorderLayout.EAST);
		innerPanel.add(southPanel, BorderLayout.SOUTH);
		
		add(innerPanel, BorderLayout.NORTH);
		add(tableScroll, BorderLayout.CENTER);
	
		if (rbm == null && bundle == null) {
			removeAll();
		}
	}
	
	public void updateComponents() {
		
	}
}

// The table model for searched Items

class SearchItemsTableModel extends AbstractTableModel {
	Vector items;
	
	public SearchItemsTableModel(Vector items) {
		this.items = items;
	}
	
	public void setItems(Vector items) {
		this.items = items;
	}
	
	public int getColumnCount() { return 3; }
		    
	public int getRowCount() {
		return items.size();
	}
	
	public Object getValueAt(int row, int col) {
		BundleItem item = (BundleItem)items.elementAt(row);
		String retStr = null;
				
		switch(col) {
		case 0:
			retStr = item.getKey();
			break;
		case 1:
			retStr = item.getTranslation();
			break;
		case 2:
			retStr = (item.getParentGroup() == null ? "" : item.getParentGroup().getName());
			break;
		default:
			retStr = Resources.getTranslation("table_cell_error");
		}
				
		return retStr;
	}
			
	public String getColumnName(int col) {
		if (col == 0) return Resources.getTranslation("languageuntrans_column_key");
		else if (col == 1) return Resources.getTranslation("languageuntrans_column_translation");
		else if (col == 2) return Resources.getTranslation("languageuntrans_column_group");
		else return Resources.getTranslation("table_column_error");
	}
	
	public BundleItem getBundleItem(int row) {
		return (BundleItem)items.elementAt(row);
	}
	
	public Vector getBundleItems() {
		return items;
	}
	
	public void update() {
		fireTableDataChanged();
	}
}
