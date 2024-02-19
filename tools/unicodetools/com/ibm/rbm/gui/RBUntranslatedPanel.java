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
import javax.swing.table.*;

import com.ibm.rbm.*;

/**
 * The class used to display untranslated items
 */
class RBUntranslatedPanel extends JPanel {
	RBManager rbm;
	Bundle bundle;
	RBManagerGUI listener;
	
	// Components - Bundle
	JLabel                      jLabelUntransTitle;
	UntranslatedItemsTableModel untransTableModel;
	JTable                      jTableUntrans;
	JScrollPane                 jScrollPaneUntransTable;
	
	// Components - Bundle Manager
	Box                         mainBox;
	JPanel                      mainPanels[];
	JLabel                      numUntransLabels[];
	JScrollPane                 mainScroll;
	JScrollPane                 listScrolls[];
	JList                       untransLists[];
	
	public RBUntranslatedPanel(RBManagerGUI gui) {
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
	
	// Marks the selected resource as translated and removes from this view
	private void markSelectedResourceAsTranslated() {
		if (bundle == null) return;
		if (jTableUntrans.getSelectedRow() < 0) return;
		if (jTableUntrans.getModel() instanceof UntranslatedItemsTableModel) {
			int row = jTableUntrans.getSelectedRow();
			UntranslatedItemsTableModel model = (UntranslatedItemsTableModel)jTableUntrans.getModel();
			BundleItem item = model.getBundleItem(row);
			item.setTranslated(true);
			model.update();
		}
	}
	
	// Removes the selected resource from the resource file
	private void deleteSelectedResource() {
		if (bundle == null) return;
		if (jTableUntrans.getSelectedRow() < 0) return;
		if (jTableUntrans.getModel() instanceof UntranslatedItemsTableModel) {
			int row = jTableUntrans.getSelectedRow();
			UntranslatedItemsTableModel model = (UntranslatedItemsTableModel)jTableUntrans.getModel();
			BundleItem item = model.getBundleItem(row);
			if (item.getParentGroup() != null && item.getParentGroup().getParentBundle() != null) {
				Bundle bundle = item.getParentGroup().getParentBundle();
				bundle.removeItem(item.getKey());
			}
			model.update();
		}
	}
	
	private void printTable() {
		PrintJob pjob = getToolkit().getPrintJob(new Frame(),
                           "Printing Test", null);

        if (pjob != null) {          
            Graphics pg = pjob.getGraphics();

            if (pg != null) {
                //jTableUntrans.print(pg);
                Dimension page_dim = pjob.getPageDimension();
				pg.setColor(Color.black);
				int y_off = 50;
				int x_off = 30;
				TableModel model = jTableUntrans.getModel();
				pg.setFont(new Font("SansSerif", Font.BOLD, 14));
				pg.drawString("Untranslated Items:       Page 1", x_off, y_off);
				pg.setFont(new Font("SansSerif", Font.PLAIN, 10));
				
				for (int i=0 ; i < model.getRowCount(); i++) {
					if (y_off < page_dim.height - 50) {
						y_off += 15;
						String key = model.getValueAt(i, 0).toString();
						String translation = model.getValueAt(i,1).toString();
						pg.drawString(key + " -> " + translation, x_off, y_off);
					}
				}
				pg.dispose(); // flush page
            }
            pjob.end();

        }
	}
	
	public void initComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelUntransTitle        = new JLabel(bundle.name);
			untransTableModel         = new UntranslatedItemsTableModel(bundle);
			jTableUntrans             = new JTable(untransTableModel);
			jScrollPaneUntransTable   = new JScrollPane(jTableUntrans);
	
			// Lower panel components
			JPanel  lowerPanel = new JPanel();
			JButton deleteButton = new JButton(Resources.getTranslation("button_delete_resource"));
			JButton translateButton = new JButton(Resources.getTranslation("button_mark_translated"));
			JButton printButton = new JButton(Resources.getTranslation("button_print_table"));
			
			deleteButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			translateButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_mark_translated_trigger")));
			lowerPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("languageuntrans_selected_resources_options")));
			lowerPanel.setLayout(new GridLayout(1,2));
			
			jTableUntrans.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableUntrans.addMouseListener(listener);
			
			jLabelUntransTitle.setFont(new Font("SansSerif",Font.PLAIN,18));
			
			// Add action listeners
			deleteButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					deleteSelectedResource();
				}
			});
			translateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					markSelectedResourceAsTranslated();
				}
			});
			printButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					printTable();
				}
			});	
			
			removeAll();
			setLayout(new BorderLayout());
			lowerPanel.add(deleteButton);
			lowerPanel.add(translateButton);
			//lowerPanel.add(printButton);
			add(jLabelUntransTitle, BorderLayout.NORTH);
			add(jScrollPaneUntransTable, BorderLayout.CENTER);
			add(lowerPanel, BorderLayout.SOUTH);
		} else if (rbm != null) {
			
			int langCount = 0;       // The number of languages with untranslated Items
			for (int i=0; i < rbm.getBundles().size(); i++) {
				Bundle bundle = (Bundle)rbm.getBundles().elementAt(i);
				if (bundle.getUntranslatedItemsSize() > 0) langCount++;
			}
			
			// Initialize the components
			mainPanels               = new JPanel[langCount];
			numUntransLabels         = new JLabel[langCount];
			listScrolls              = new JScrollPane[langCount];
			untransLists             = new JList[langCount];
			
			mainBox                  = new Box(BoxLayout.Y_AXIS);
			mainScroll               = new JScrollPane(mainBox);
			jLabelUntransTitle       = new JLabel(rbm.getBaseClass() + " - " + Resources.getTranslation("untranslated_items"));
			
			// Set component properties
			jLabelUntransTitle.setFont(new Font("SansSerif",Font.PLAIN,18));
			mainBox.add(jLabelUntransTitle);
			
			int count = 0;
			for (int i=0; i < rbm.getBundles().size(); i++) {
				Bundle bundle = (Bundle)rbm.getBundles().elementAt(i);
				if (bundle.getUntranslatedItemsSize() > 0) {
					mainPanels[count] = new JPanel();
					mainPanels[count].setLayout(new BorderLayout());
					numUntransLabels[count] = new JLabel(Resources.getTranslation("baseuntrans_untrans_count") +
														 bundle.getUntranslatedItemsSize());
					// TODO: Implement a List Model for this list, remove use of vector
					untransLists[count] = new JList(bundle.getUntranslatedItemsAsVector());
					listScrolls[count] = new JScrollPane(untransLists[count]);
					
					mainPanels[count].setBorder(BorderFactory.createTitledBorder(
												BorderFactory.createEtchedBorder(),
												Resources.getTranslation("baseuntrans_file") + " " + bundle.toString()));
					mainPanels[count].removeAll();
					mainPanels[count].add(numUntransLabels[count], BorderLayout.NORTH);
					mainPanels[count].add(listScrolls[count], BorderLayout.CENTER);
					
					mainBox.add(Box.createVerticalStrut(5));
					mainBox.add(mainPanels[count]);
					
					count++;
				}
			}
			mainScroll.setPreferredSize(getSize());
			removeAll();
			add(mainScroll);
		} else {
			removeAll();
		}
	}
	
	public void updateComponents() {
		// Update components
		if (bundle != null) {
			jLabelUntransTitle.setText(bundle.name);
			untransTableModel.setBundle(bundle);
		} else if (rbm != null) {
			initComponents();
		} else {
			removeAll();
		}
	}
}

/**
 * The table model for untranslated Items
 */

class UntranslatedItemsTableModel extends AbstractTableModel {
	Bundle bundle;
	
	public UntranslatedItemsTableModel(Bundle bundle) {
		this.bundle = bundle;
	}
	
	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
		update();
	}
	
	public int getColumnCount() { return 3; }
		    
	public int getRowCount() {
		return bundle.getUntranslatedItemsSize();
	}
	
	public Object getValueAt(int row, int col) {
		BundleItem item = bundle.getUntranslatedItem(row);
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
		return bundle.getUntranslatedItem(row);
	}
	
	public void update() {
		fireTableDataChanged();
	}
}

