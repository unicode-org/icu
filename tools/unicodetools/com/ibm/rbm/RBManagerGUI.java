/*
 *****************************************************************************
 * Copyright (C) 2000-2002, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/rbm/Attic/RBManagerGUI.java,v $ 
 * $Date: 2002/05/20 18:53:09 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;

/**
 * The Graphical User Interface for working with and through a Resource Bundle Manager. The GUI has no public main
 * method. It is instead instantiated from running the main method in RBManager. For help with using this interface,
 * consult the documentation included in the project.
 * 
 * @author Jared Jackson - Email: <a href="mailto:jjared@almaden.ibm.com">jjared@almaden.ibm.com</a>
 * @see com.ibm.rbm.RBManager
 */
public class RBManagerGUI extends JFrame implements ActionListener, MouseListener, ChangeListener, TreeSelectionListener
{
	// CONSTANTS
	private static final int buffer = 20;
	private static final Dimension dimMain = new Dimension(750,550);
	private static final Dimension dimMainMax = new Dimension(2000,1500);
	private static final Dimension dimMainMin = new Dimension(550,350);
	private static final Dimension dimTop = new Dimension(dimMain.width - buffer,50);
	private static final Dimension dimTopMax = new Dimension(dimMainMax.width - buffer,50);
	private static final Dimension dimTopMin = new Dimension(dimMainMin.width - buffer,50);
	private static final Dimension dimBottom = new Dimension(dimMain.width - buffer,dimMain.height-dimTop.height - buffer);
	private static final Dimension dimBottomMax = new Dimension(dimMainMax.width - buffer,dimMainMax.height-dimTopMin.height - buffer);
	private static final Dimension dimBottomMin = new Dimension(dimMainMin.width - buffer,dimMainMin.height-dimTopMax.height - buffer);
	private static final Dimension dimLeft = new Dimension(175,dimBottom.height - buffer);
	private static final Dimension dimRight = new Dimension(dimMain.width-dimLeft.width - buffer,dimBottom.height - buffer);
	
	/**
	 * Used for toggling the debug mode
	 */
	public static final boolean debug = false;
	/**
	 * Used to count debug messages
	 */
	public static int debugcount = 0;
	
	// member declarations
	
	// ** DATA **
	RBManager rbm = null;
	String userName = Resources.getTranslation("unknown_user");
	
	DefaultMutableTreeNode activeNode = null;
	
	// ** MAIN MENU **
	RBManagerMenuBar         jMenuBarMain = null;

	// ** CONTENT PANES **
	Box          boxMain = new Box(BoxLayout.Y_AXIS);
	JPanel       jPanelTop = new JPanel();
	JPanel       jPanelBottom = new JPanel();
	JSplitPane   jSplitPaneMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	// ** TOP COMPONENTS **
	JLabel       jLabelTitle = new JLabel(new ImageIcon(this.getClass().getResource("images/" + 
																				Resources.getTranslation("logo_filename"))));
	//JLabel       jLabelTitle = new JLabel(new ImageIcon(this.getClass().getResource("images/TitleLogo_transparent.gif")));
	
	// ** SPLIT PANE COMPONENTS **
	JTree        jTreeDisplay = new JTree();
	JTabbedPane  jTabbedPaneMain = new JTabbedPane();
	RBStatisticsPanel        jPanelStats = new RBStatisticsPanel();
	RBUntranslatedPanel      jPanelUntrans = new RBUntranslatedPanel(this);
	RBGroupPanel             jPanelGroups = new RBGroupPanel(this);
	RBSearchPanel            jPanelSearch = new RBSearchPanel(this);
	JScrollPane  jScrollPaneTree = new JScrollPane(jTreeDisplay,
																 		   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
																		   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	// ** PROJECT VIEW SPLIT PANE COMPONENTS
	JTabbedPane              treeTabbedPane = new JTabbedPane();
	JTree                    projectTree = new JTree();
	JScrollPane              projectScrollPane = new JScrollPane(projectTree, 
																 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
																 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	RBProjectItemPanel       projectPanel = new RBProjectItemPanel(this);
	RBProject                project = null;
	
	// ** File Chooser **
	JFileChooser openFileChooser = new JFileChooser();
	JFileChooser saveFileChooser = new JFileChooser();
	JFileChooser projectFileChooser = new JFileChooser();
	
	/**
	 * Creation of the GUI should be immediately followed by the method calls to initComponents() and setVisible(true).
	 * These methods were not called by default for programming discretion
	 */
	
	public RBManagerGUI()
	{
	}
	
	/**
	 * Inherits from JFrame.validate(), with some component updates
	 */
	
	public void validate() {
		super.validate();
		updateDisplayPanels();
	}
	
	/**
	 * Initial construction of all of the GUI components. This method should be called immediately following the
	 * construction of the GUI object.
	 */
	
	public void initComponents() throws Exception
	{
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// the following code sets the frame's initial state
		
		openFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
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
		
		saveFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
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
		
		projectFileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".rbproject"))) return false;
				return true;
			}
			
			public String getDescription() {
				return Resources.getTranslation("dialog_project_filter_description");	
			}
		});

        // ** The Main program icon **
        setIconImage((new ImageIcon(getClass().getResource("images/tree_icon_bundle.gif"))).getImage());
		
		// ** MAIN MENU BAR ITEMS **
		jMenuBarMain = new RBManagerMenuBar(this);
		
		// ** DISPLAY TREE **
		//jScrollPaneTree.setSize(dimLeft);
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		
		jPanelStats.setSize(dimRight);
		jPanelUntrans.setSize(dimRight);
		jPanelGroups.setSize(dimRight);
		jPanelSearch.setSize(dimRight);
		
		// ** MAIN TABBED PANE **
		jTabbedPaneMain.setSize(dimRight);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_statistics"), jPanelStats);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_untranslated"), jPanelUntrans);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_groups"), jPanelGroups);
		jTabbedPaneMain.addTab(Resources.getTranslation("tab_search"), jPanelSearch);
		
		// ** LEFT TABBED PANE **
		treeTabbedPane.setSize(dimLeft);
		treeTabbedPane.setPreferredSize(dimLeft);
		treeTabbedPane.addTab(Resources.getTranslation("tab_bundle"), jScrollPaneTree);
		treeTabbedPane.addTab(Resources.getTranslation("tab_project"), projectScrollPane);
		treeTabbedPane.addChangeListener(this);
		
		// ** MAIN SPLIT PANE **
		//jSplitPaneMain.setSize(dimBottom);
		//jSplitPaneMain.setLeftComponent(jScrollPaneTree);
		jSplitPaneMain.setLeftComponent(treeTabbedPane);
		jSplitPaneMain.setRightComponent(jTabbedPaneMain);
		jSplitPaneMain.setContinuousLayout(true);
		
		// ** TOP PANEL **
		//jPanelTop.setPreferredSize(dimTop);
		jPanelTop.setMaximumSize(dimTopMax);
		jPanelTop.setMinimumSize(dimTopMin);
		jPanelTop.setBorder(BorderFactory.createLineBorder(Color.black));
		jPanelTop.add(jLabelTitle);
		
		// ** BOTTOM PANEL **
		//jPanelBottom.setPreferredSize(dimBottom);
		jPanelBottom.setMaximumSize(dimBottomMax);
		jPanelBottom.setMinimumSize(dimBottomMin);
		jPanelBottom.setBorder(BorderFactory.createLineBorder(Color.black));
		jPanelBottom.setLayout(new BorderLayout(1,1));
		jPanelBottom.removeAll();
		jPanelBottom.add(jSplitPaneMain, BorderLayout.CENTER);
		
		// ** MAIN FRAME SETUP **
		setLocation(new java.awt.Point(0, 0));
		dimMain.height += jMenuBarMain.getPreferredSize().height;
		setSize(dimMain);
		((JComponent)getContentPane()).setMaximumSize(dimMainMax);
		((JComponent)getContentPane()).setMinimumSize(dimMainMin);
		setJMenuBar(jMenuBarMain);
		getContentPane().removeAll();
		getContentPane().add(jPanelTop,BorderLayout.NORTH);
		getContentPane().add(jPanelBottom, BorderLayout.CENTER);
		setTitle(Resources.getTranslation("resource_bundle_manager"));
		validateTree();
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});
	}
  
  	private boolean mShown = false;
	
	/**
	 * Reveals the private method of JFrame.validateTree()
	 */
	
	public void validateMyTree() {
		validateTree();
	}
	
	/**
	 * Creates a new Resource File (i.e. English, English Canada, Finnish, etc.)
	 */
	
	public void createResourceFile() {
		new ResourceCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_file"), true);
	}
	
	/**
	 * Creates a new group for grouping BundleItems
	 */
	
	public void createBundleGroup() {
		new BundleGroupCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_group"), true);
		updateDisplayPanels();
	}
	
	/**
	 * Creates a new BundleItem
	 */
	
	public void createBundleItem() {
		new BundleItemCreationDialog(rbm, this, Resources.getTranslation("dialog_title_new_item"), true);
	}
	
	/**
	 * Handles events generated
	 */
	
	public void valueChanged(TreeSelectionEvent ev) {
		if (ev.getSource() == projectTree) updateProjectPanels();
		else if (ev.getSource() == jTreeDisplay) {
			TreePath selPath = jTreeDisplay.getSelectionPath();
			activeNode = (DefaultMutableTreeNode)selPath.getLastPathComponent();
			updateDisplayPanels();
			/*
			int selRow = jTreeDisplay.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = jTreeDisplay.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1) {
            if (ev.getClickCount() == 1 && ev.getSource() == jTreeDisplay) {
				
				System.out.println("Other tree");
			} else if (ev.getClickCount() == 1 && ev.getSource() == projectTree) {
				System.out.println("Mouse pressed");
				updateProjectPanels();	
			} else System.out.println(String.valueOf(ev.getClickCount()) + " " + ev.getSource().toString());
			
			*/
		}
	}
	
	public void stateChanged(ChangeEvent ev) {
		if (ev.getSource() == treeTabbedPane) {
			int index = treeTabbedPane.getSelectedIndex();	
			String title = treeTabbedPane.getTitleAt(index);
			if (title.equals(Resources.getTranslation("tab_bundle"))) {
				jSplitPaneMain.setRightComponent(jTabbedPaneMain);
				updateDisplayPanels();
			} else if (title.equals(Resources.getTranslation("tab_project"))) {
				jSplitPaneMain.setRightComponent(projectPanel);
				updateProjectPanels();
			}
		}
	}
	
	public void actionPerformed(ActionEvent ev) {
		if (ev.getID() == ActionEvent.ACTION_PERFORMED) {
			if (ev.getSource() instanceof JMenuItem && ((JMenuItem)ev.getSource()).getName() != null &&
				((JMenuItem)ev.getSource()).getName().startsWith("__")) {               // Menu -> File -> __Recent File
				// This item is a recent file selection. We need to open that file
				String fileLocation = ((JMenuItem)ev.getSource()).getName();
				fileLocation = fileLocation.substring(2,fileLocation.length());
				try {
					rbm = new RBManager(new File(fileLocation));
					updateDisplayTree();
					updateProjectTree();
					updateProjectPanels();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,Resources.getTranslation("error_opening_file", ev.getActionCommand()),
												  Resources.getTranslation("dialog_title_error_opening_file"),
												  JOptionPane.ERROR_MESSAGE);
					rbm = null;
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_save")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Save
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				saveResources(selectedEncoding);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_hide")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Hide
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				// Should I prompt for this?
				hideResources(selectedEncoding);
			}  else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_delete")) &&
					   ((JMenuItem)ev.getSource()).getName() != null) {                // Popup Tree Menu -> Delete
				String selectedEncoding = ((JMenuItem)ev.getSource()).getName();
				int response = JOptionPane.showConfirmDialog(this,
					Resources.getTranslation("dialog_delete_warning"),
					Resources.getTranslation("dialog_title_quit"), JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE);
				if (response == JOptionPane.YES_OPTION) {
					deleteResources(selectedEncoding);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_new_project"))) {
				String response = JOptionPane.showInputDialog(this,                   // Popup Project Menu -> New Project
					Resources.getTranslation("dialog_new_project"), Resources.getTranslation("dialog_title_new_project"),
					JOptionPane.QUESTION_MESSAGE);
				if (response == null || response.trim().equals("")) return;
				project = new RBProject(response);
				updateProjectTree();
				updateProjectPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_open_project"))) {
				int result = projectFileChooser.showOpenDialog(this);                // Popup Project Menu -> Open Project
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = projectFileChooser.getSelectedFile();
					try {
						project = new RBProject(f);
						updateProjectTree();
						updateProjectPanels();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_creating_project"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_save_project"))) {
				int result = projectFileChooser.showSaveDialog(this);                // Popup Project Menu -> Save Project
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = projectFileChooser.getSelectedFile();
					try {
						project.write(f);
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_saving_project"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_add_project_bundle"))) {
				int result = openFileChooser.showOpenDialog(this);                   // Popup Project Menu -> Add Bundle
				if (result == JFileChooser.APPROVE_OPTION) {
					File f = openFileChooser.getSelectedFile();
					try {
						project.addBundle(f.getAbsolutePath());
						updateProjectTree();
						updateProjectPanels();
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_adding_project_bundle"),
							Resources.getTranslation("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_remove_project_bundle"))) {
				String bundleName = ((JMenuItem)ev.getSource()).getName();           // Popup Project Menu -> Remove Bundle
				project.removeBundle(bundleName);
				updateProjectTree();
				updateProjectPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_tree_select_project_bundle"))) {
				String bundleName = ((JMenuItem)ev.getSource()).getName();           // Popup Project Menu -> Select Bundle
				RBManager bundle = project.getBundle(bundleName);
				rbm = bundle;
				updateDisplayTree();
				updateDisplayPanels();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_quit"))) {
																					   // Menu -> File -> Quit
				thisWindowClosing(null);
				return;
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_new"))) {
																					   // Menu -> File -> New Resource Bundle
				promptForSave(null);
				String oldUser = getUser();
				if (rbm != null && rbm.getUser() != null && !(rbm.getUser().equals(Resources.getTranslation("unknown_user"))))
					oldUser = rbm.getUser();
				String response = JOptionPane.showInputDialog(this,
					Resources.getTranslation("dialog_new_baseclass"), Resources.getTranslation("dialog_title_new_bundle"),
					JOptionPane.QUESTION_MESSAGE);
				if (response != null) {
					// Test the response for white space
					if (response.indexOf(" ") > 0 || response.indexOf("\t") > 0 || response.indexOf("\n") > 0) {
						JOptionPane.showMessageDialog(this,
							Resources.getTranslation("error_baseclass_whitespace") + "\n" + Resources.getTranslation("error_bundle_not_created"),
							Resources.getTranslation("dialog_title_error_creating_bundle"), JOptionPane.ERROR_MESSAGE);
					} else {
						rbm = new RBManager(response);
						updateDisplayTree();
						updateProjectTree();
						updateProjectPanels();
						updateDisplayPanels();
					}
				}
				// Update the user information
				if (oldUser.equals(Resources.getTranslation("unknown_user"))) {
					String user = JOptionPane.showInputDialog(this,
						Resources.getTranslation("dialog_user_name"), Resources.getTranslation("dialog_title_user_name"),
						JOptionPane.QUESTION_MESSAGE);
					if (user != null && !(user.equals(""))) setUser(user);
				} else rbm.setUser(oldUser);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_open"))) {
																					   // Menu -> File -> Open Resource Bundle
				promptForSave(null);
				String oldUser = getUser();
				if (rbm != null && rbm.getUser() != null && !(rbm.getUser().equals(Resources.getTranslation("unknown_user"))))
					oldUser = rbm.getUser();
				openFileChooser.setSelectedFile(new File("Resources" + File.separator + "RBManager.properties"));
				int status = openFileChooser.showOpenDialog(this);
				if (status == JFileChooser.CANCEL_OPTION) {
					// File opening canceled
				} else if (status == JFileChooser.ERROR_OPTION) {
					// Error in file open
				} else {
					// A file has been selected
					try {
						rbm = new RBManager(openFileChooser.getSelectedFile());
						updateDisplayTree();
						updateProjectTree();
						updateProjectPanels();
					} catch (IOException ioe) {
						// Should provide some alert here
						System.err.println("Could not open the file " + openFileChooser.getSelectedFile().getAbsolutePath() +
										   ": " + ioe.getMessage());
						rbm = null;
					}
				}
				if (rbm == null) return;
				// Update the user information
				if (oldUser.equals(Resources.getTranslation("unknown_user"))) {
					String user = JOptionPane.showInputDialog(this,
						Resources.getTranslation("dialog_user_name"), Resources.getTranslation("dialog_title_user_name"),
						JOptionPane.QUESTION_MESSAGE);
					if (user != null && !(user.equals(""))) setUser(user);
				} else rbm.setUser(oldUser);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_save"))) {
																					   // Menu -> File -> Save Resource Bundle
				saveResources();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_saveas"))) {
																					   // Menu -> File -> Save Resource Bundle As
				saveResourcesAs();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_properties"))) {
																					   // Menu -> File -> Import -> Properties
				if (rbm == null || rbm.getBundles() == null) return;
				RBPropertiesImporter importer = new RBPropertiesImporter(Resources.getTranslation("import_properties_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_java"))) {
																					   // Menu -> File -> Import -> Java
				if (rbm == null || rbm.getBundles() == null) return;
				RBJavaImporter importer = new RBJavaImporter(Resources.getTranslation("import_java_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_import_TMX"))) {
																					   // Menu -> File -> Import -> TMX
				if (rbm == null || rbm.getBundles() == null) return;
				RBTMXImporter importer = new RBTMXImporter(Resources.getTranslation("import_TMX_title"), rbm, this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_properties"))) {
																					   // Menu -> File -> Export -> Properties
				RBPropertiesExporter exp = new RBPropertiesExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_java"))) {
																					   // Menu -> File -> Export -> Java
				RBJavaExporter exp = new RBJavaExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_ICU"))) {
																					   // Menu -> File -> Export -> Java
				RBICUExporter exp = new RBICUExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_file_export_TMX"))) {
																					   // Menu -> File -> Export -> TMX
				RBTMXExporter exp = new RBTMXExporter();
				try {
					if (rbm != null && rbm.getBundles() != null) exp.export(rbm);
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this, Resources.getTranslation("error_export"),
												  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				}
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addfile"))) {
																					   // Menu -> Options -> Add New Resource
				createResourceFile();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addgroup")) ||
					   ev.getActionCommand().equals(Resources.getTranslation("button_create_group"))) {
																					   // Menu -> Options -> Add New Group
				createBundleGroup();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_addentry"))) {
																					   // Menu -> Options -> Add New Entry
				createBundleItem();
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_options_preferences"))) {
																					   // Menu -> Options -> Preferences
				PreferencesDialog pd = new PreferencesDialog(this);
			} else if (ev.getActionCommand().equals(Resources.getTranslation("menu_help_about"))) { 
																					   // Menu -> Help -> About RBManager
				AboutDialog.showDialog(this);
			} else RBManagerGUI.debugMsg("Missed Action Command: " + ev.getActionCommand());
			
		}
		
	}
	
	/**
	 * Handles events generated
	 */
	
	public void mousePopup(MouseEvent ev) {
		if (ev.getSource() == jTreeDisplay) {
			int selRow = jTreeDisplay.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = jTreeDisplay.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1) {
			    if (ev.getClickCount() == 1) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
					Object obj = node.getUserObject();
					if (obj == null || !(obj instanceof Bundle)) return;
					Bundle bundle = (Bundle)obj;
					String encoding = bundle.encoding;
					if (encoding == null) encoding = new String();
							
					// Create the menu to display
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save"));
					JMenuItem hideItem = new JMenuItem(Resources.getTranslation("menu_tree_hide"));
					JMenuItem deleteItem = new JMenuItem(Resources.getTranslation("menu_tree_delete"));
							
					saveItem.setName(encoding); saveItem.addActionListener(this);
					hideItem.setName(encoding); hideItem.addActionListener(this);
					deleteItem.setName(encoding); deleteItem.addActionListener(this);
							
					popupMenu.add(saveItem);
					if (node.getLevel() != 1) {
						popupMenu.add(hideItem);
						popupMenu.add(deleteItem);
					}
							
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
			    }
			}
		} else if (ev.getSource() == projectTree) {
			int selRow = projectTree.getRowForLocation(ev.getX(), ev.getY());
			TreePath selPath = projectTree.getPathForLocation(ev.getX(), ev.getY());
			if (selRow != -1 && ev.getClickCount() == 1) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
				Object obj = node.getUserObject();
				if (obj == null) return;
				else if (obj instanceof String) {
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem newItem = new JMenuItem(Resources.getTranslation("menu_tree_new_project"));
					JMenuItem openItem = new JMenuItem(Resources.getTranslation("menu_tree_open_project"));
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save_project"));
					newItem.addActionListener(this);
					openItem.addActionListener(this);
					saveItem.addActionListener(this);
					popupMenu.add(newItem);
					popupMenu.add(openItem);
					popupMenu.add(saveItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				} else if (obj instanceof RBProject) {
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem newItem = new JMenuItem(Resources.getTranslation("menu_tree_new_project"));
					JMenuItem openItem = new JMenuItem(Resources.getTranslation("menu_tree_open_project"));
					JMenuItem saveItem = new JMenuItem(Resources.getTranslation("menu_tree_save_project"));
					JMenuItem addItem = new JMenuItem(Resources.getTranslation("menu_tree_add_project_bundle"));
					newItem.addActionListener(this);
					openItem.addActionListener(this);
					saveItem.addActionListener(this);
					addItem.addActionListener(this);
					popupMenu.add(newItem);
					popupMenu.add(openItem);
					popupMenu.add(saveItem);
					popupMenu.add(addItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				} else if (obj instanceof RBManager) {
					RBManager rbm = (RBManager)obj;
					JPopupMenu popupMenu = new JPopupMenu();
					JMenuItem selectItem = new JMenuItem(Resources.getTranslation("menu_tree_select_project_bundle"));
					JMenuItem removeItem = new JMenuItem(Resources.getTranslation("menu_tree_remove_project_bundle"));
					selectItem.setName(rbm.getBaseClass());
					removeItem.setName(rbm.getBaseClass());
					selectItem.addActionListener(this);
					removeItem.addActionListener(this);
					popupMenu.add(selectItem);
					popupMenu.add(removeItem);
					popupMenu.show(ev.getComponent(), ev.getX(), ev.getY());
				}
			}
		}
	}
	
	public void mousePressed(MouseEvent ev)  {
		if (ev.isPopupTrigger()) {
			mousePopup(ev);
		}
	}
	
	public void mouseReleased(MouseEvent ev) {
		if (ev.isPopupTrigger()) {
			mousePopup(ev);
			return;
		}
		// Not the popup trigger
	}
	
	public void mouseEntered(MouseEvent ev)  { }
	
	public void mouseExited(MouseEvent ev)   { }
	
	public void mouseClicked(MouseEvent ev) {
		if (ev.getClickCount() == 2 && ev.getSource() instanceof JTable) {
			// We are going to display the edit frame for the item selected
			BundleItem item = null;
			JTable table = (JTable) ev.getSource();
			if (table.getModel() instanceof UntranslatedItemsTableModel) {
				int row = table.getSelectedRow();
				UntranslatedItemsTableModel model = (UntranslatedItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			} else if (table.getModel() instanceof SearchItemsTableModel) {
				int row = table.getSelectedRow();
				SearchItemsTableModel model = (SearchItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			} else if (table.getModel() instanceof GroupItemsTableModel) {
				int row = table.getSelectedRow();
				GroupItemsTableModel model = (GroupItemsTableModel)table.getModel();
				item = model.getBundleItem(row);
				BundleItemDialog biDialog = new BundleItemDialog(rbm, item, (rbm == null ? "" : rbm.getUser()),
															 this, Resources.getTranslation("dialog_title_edit_item"), true);
				model.update();
			}
		}
	}
	
	protected void updateProjectPanels() {
		projectPanel.updateComponents();
	}
	
	// Update the display of the main panels (stats, untrans, groups). Should be called after a new tree selection
	protected void updateDisplayPanels() {
		debugMsg("Updating Display Panels");
		
		Bundle bundle = null;
		if (activeNode == null) return;
		Object o = activeNode.getUserObject();
		if (o == null) return;
		if (o instanceof String) {
			// A node that is not a root was selected.... I need to do something here
			String str = (String)o;
			if (rbm == null) return;
			if (str.equals(rbm.getBaseClass())) {
				// The base class node was selected
				jPanelStats.setManager(rbm);
				jPanelUntrans.setManager(rbm);
				jPanelGroups.setManager(rbm);
				jPanelSearch.setManager(rbm);
			} else {
				jPanelStats.removeElements();
				jPanelUntrans.removeElements();
				jPanelGroups.removeElements();
				jPanelSearch.removeElements();
			}
			//return;
		} else if (o instanceof Bundle) {
			bundle = (Bundle) activeNode.getUserObject();
			jPanelStats.setBundle(bundle);
			jPanelUntrans.setBundle(bundle);
			jPanelGroups.setBundle(bundle);
			jPanelSearch.setBundle(bundle);
		} else RBManagerGUI.debugMsg(o.toString());
		
		jPanelStats.updateComponents();
		jPanelUntrans.updateComponents();
		jPanelGroups.updateComponents();
		jPanelSearch.updateComponents();
		
		validateTree();
	}
	
	protected void updateProjectTree() {
		debugMsg("Updating Project Trees");
		
		DefaultMutableTreeNode root = null;
		
		if (project != null) {
			root = new DefaultMutableTreeNode(project);
			for (int i=0; i < project.getSize(); i++) {
				RBManager rbm = project.getBundle(i);
				DefaultMutableTreeNode bundleNode = new DefaultMutableTreeNode(rbm);
				root.add(bundleNode);
				Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
				Vector groups = mainBundle.getGroupsAsVector();
				for (int j=0; j < groups.size(); j++) {
					BundleGroup group = (BundleGroup)groups.elementAt(j);
					DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
					bundleNode.add(groupNode);
					Vector items = group.getItemsAsVector();
					for (int k=0; k < items.size(); k++) {
						BundleItem item = (BundleItem)items.elementAt(k);
						DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
						groupNode.add(itemNode);
					}
				}
			}
		} else if (rbm != null) {
			// There is a resource bundle open, but no project
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_project"));
			Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
			DefaultMutableTreeNode bundleNode = new DefaultMutableTreeNode(rbm);//(rbm.getBaseClass());
			root.add(bundleNode);
			Vector groups = mainBundle.getGroupsAsVector();
			for (int i=0; i < groups.size(); i++) {
				BundleGroup group = (BundleGroup)groups.elementAt(i);
				DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group);
				bundleNode.add(groupNode);
				Vector items = group.getItemsAsVector();
				for (int j=0; j < items.size(); j++) {
					BundleItem item = (BundleItem)items.elementAt(j);
					DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
					groupNode.add(itemNode);
				}
			}
		} else {
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_project_bundle"));
		}
		
		// Create the tree from the roots
		projectTree = new JTree(root);
		projectTree.addMouseListener(this);
		projectTree.addTreeSelectionListener(this);
		projectTree.setCellRenderer(RBTreeCellRenderer.getInstance());
		projectScrollPane.getViewport().removeAll();
		projectScrollPane.getViewport().add(projectTree);
		repaint();
		validateTree();
		return;
	}
	
	// Update the display of the tree file map. Should be called when the tree is changed/updated
	protected void updateDisplayTree() {
		debugMsg("Updating Display Trees");
		
		DefaultMutableTreeNode root = null;
		
		if (rbm == null || rbm.getBundles() == null) {
			root = new DefaultMutableTreeNode(Resources.getTranslation("no_resource_bundle"));
		} else {
			// From here on out, there is a defined resource bundle manager
			Bundle mainBundle = (Bundle)rbm.getBundles().firstElement();
			root = new DefaultMutableTreeNode(rbm.getBaseClass());
			// Add the base class
			root.add(new DefaultMutableTreeNode(mainBundle));
			
			DefaultMutableTreeNode currNode = root;
			for (int i = 1; i < rbm.getBundles().size(); i++) {
				Bundle currBundle = (Bundle)rbm.getBundles().elementAt(i);
				String variant = currBundle.getVariantEncoding();
				String country = currBundle.getCountryEncoding();
				String language = currBundle.getLanguageEncoding();
				DefaultMutableTreeNode languageNode = null;
				// Look for a node representing this language
				if (language == null || language.equals("")) continue;
				boolean languageNodeFound = false;
				for (int j=0; j < root.getChildCount(); j++) {
					DefaultMutableTreeNode langNode = (DefaultMutableTreeNode)root.getChildAt(j);
					Object o = langNode.getUserObject();
					if (o == null || !(o instanceof String)) continue;
					String str = (String)o;
					if (str.equals(Resources.getTranslation("tree_language_node", language))) {
						// There is a non-leaf node with this language
						languageNodeFound = true;
						if (country == null || country.equals(""))
							 langNode.add(new DefaultMutableTreeNode(currBundle));
						else {
							// We need to look at country, variant
							boolean countryNodeFound = false;
							for (int k=0; k < langNode.getChildCount(); k++) {
								DefaultMutableTreeNode countryNode = (DefaultMutableTreeNode)langNode.getChildAt(k);
								Object o2 = countryNode.getUserObject();
								if (o2 == null || !(o2 instanceof String)) continue;
								String str2 = (String)o2;
								if (str2.equals(Resources.getTranslation("tree_country_node", country))) {
									// There is a non-leaf node for this country
									countryNodeFound = true;
									if (variant == null || variant.equals("")) {
										countryNode.add(new DefaultMutableTreeNode(currBundle));
									} else {
										// We need to look at variant
										boolean variantNodeFound = false;
										for (int l=0; l < countryNode.getChildCount(); l++) {
											DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
											Object o3 = variantNode.getUserObject();
											if (o3 == null || !(o3 instanceof String)) continue;
											String str3 = (String)o3;
											if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
												variantNodeFound = true;
												variantNode.add(new DefaultMutableTreeNode(currBundle));
											} 
										} // end for - country node loop
										if (!variantNodeFound) {
											DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
											countryNode.add(variantNode);
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										}
									}
								}
							} // end for - language node loop
							if (!countryNodeFound) {
								DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_country_node", country));
								langNode.add(countryNode);
								if (variant == null || variant.equals("")) {
									countryNode.add(new DefaultMutableTreeNode(currBundle));
								} else {
									// We need to look at the variant	
									boolean variantNodeFound = false;
									for (int l=0; l < countryNode.getChildCount(); l++) {
										DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
										Object o3 = variantNode.getUserObject();
										if (o3 == null || !(o3 instanceof String)) continue;
										String str3 = (String)o3;
										if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
											variantNodeFound = true;
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										} 
									} // end for - country node loop
									if (!variantNodeFound) {
										DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
										countryNode.add(variantNode);
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									}
								}
							}
						}
					}
				}
				if (!languageNodeFound) {
					// We need to create a node for this country
					DefaultMutableTreeNode langNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_language_node", language));
					root.add(langNode);
					if (country == null || country.equals("")) {
						langNode.add(new DefaultMutableTreeNode(currBundle));
					} else {
						// We need to look at the country, variant
						boolean countryNodeFound = false;
						for (int k=0; k < langNode.getChildCount(); k++) {
							DefaultMutableTreeNode countryNode = (DefaultMutableTreeNode)langNode.getChildAt(k);
							Object o2 = countryNode.getUserObject();
							if (o2 == null || !(o2 instanceof String)) continue;
							String str2 = (String)o2;
							if (str2.equals(Resources.getTranslation("tree_country_node", country))) {
								// There is a non-leaf node for this country
								countryNodeFound = true;
								if (variant == null || variant.equals("")) {
									countryNode.add(new DefaultMutableTreeNode(currBundle));
								} else {
									// We need to look at variant
									boolean variantNodeFound = false;
									for (int l=0; l < countryNode.getChildCount(); l++) {
										DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
										Object o3 = variantNode.getUserObject();
										if (o3 == null || !(o3 instanceof String)) continue;
										String str3 = (String)o3;
										if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
											variantNodeFound = true;
											variantNode.add(new DefaultMutableTreeNode(currBundle));
										} 
									} // end for - country node loop
									if (!variantNodeFound) {
										DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node"));
										countryNode.add(variantNode);
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									}
								}
							}
						} // end for - language node loop
						if (!countryNodeFound) {
							DefaultMutableTreeNode countryNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_country_node", country));
							langNode.add(countryNode);
							if (variant == null || variant.equals("")) {
								countryNode.add(new DefaultMutableTreeNode(currBundle));
							} else {
								// We need to look at the variant	
								boolean variantNodeFound = false;
								for (int l=0; l < countryNode.getChildCount(); l++) {
									DefaultMutableTreeNode variantNode = (DefaultMutableTreeNode)countryNode.getChildAt(l);
									Object o3 = variantNode.getUserObject();
									if (o3 == null || !(o3 instanceof String)) continue;
									String str3 = (String)o3;
									if (str3.equals(Resources.getTranslation("tree_variant_node"))) {
										variantNodeFound = true;
										variantNode.add(new DefaultMutableTreeNode(currBundle));
									} 
								} // end for - country node loop
								if (!variantNodeFound) {
									DefaultMutableTreeNode variantNode = new DefaultMutableTreeNode(Resources.getTranslation("tree_variant_node", variant));
									countryNode.add(variantNode);
									variantNode.add(new DefaultMutableTreeNode(currBundle));
								}
							}
						}
					}
				}
			}
		}
		
		// Create the tree from the roots
		jTreeDisplay = new JTree(root);
		jTreeDisplay.addMouseListener(this);
		jTreeDisplay.addTreeSelectionListener(this);
		jTreeDisplay.setCellRenderer(RBTreeCellRenderer.getInstance());
		jScrollPaneTree.getViewport().removeAll();
		jScrollPaneTree.getViewport().add(jTreeDisplay);
		repaint();
		validateTree();
		return;
	}
	
	/**
	 * Inherits from JFrame.addNotify(), but also inserts the menu bar
	 */
	
	public void addNotify() 
	{
		super.addNotify();
		
		if (mShown)
			return;
			
		// resize frame to account for menubar
		JMenuBar jMenuBar = getJMenuBar();
		if (jMenuBar != null) {
			int jMenuBarHeight = jMenuBar.getPreferredSize().height;
			Dimension dimension = getSize();
			dimension.height += jMenuBarHeight;
			setSize(dimension);
		}

		mShown = true;
	}

	/**
	 * Called when it may be appropriate to check with the user if they want to save the file
	 */
	
	boolean promptForSave(String message) {
		if (rbm != null) {
			int response = JOptionPane.showConfirmDialog(this,
				(message == null ? Resources.getTranslation("dialog_save") : message),
				Resources.getTranslation("dialog_title_quit"), JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.CANCEL_OPTION) return true;
			if (response == JOptionPane.YES_OPTION) {
				return saveResources();
			}
		}
		return true;
	}
	
	public boolean deleteResources(String encoding) {
		if (rbm == null) return false;  // This should never happen
		try {
			rbm.eraseFile(encoding);	
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_deleting", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
		return true;
	}
	
	public void hideResources(String encoding) {
		rbm.hideResource(encoding);
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
	}
	
	/**
	 * Save a particular resources file within the bundle.
	 */
	
	public boolean saveResources(String encoding) {
		if (rbm == null) return false;  // This should never happen
		return saveResources(rbm, encoding);
	}
	
	public boolean saveResources(RBManager bundle, String encoding) {
		try {
			bundle.writeToFile(encoding);	
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		return true;
	}
	
	/**
	 * Called when the resources are to be saved
	 */
	
	public boolean saveResources() {
		if (rbm == null) return true;
		return saveResources(rbm);
	}
	
	public boolean saveResources(RBManager bundle) {
		try {
			bundle.writeToFile();
		} catch (IOException ioe) {
			JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			if (RBManagerGUI.debug) System.err.println(ioe);
			return false;
		}
		return true;
	}
	
	/**
	 * Called when the resource bundle is to be saved, but displays a window to the user allowing them
	 * to selecte the file destination of the folder in which to save the bundle as well as the base
	 * class name for the bundle.
	 */
	
	public boolean saveResourcesAs() {
		if (rbm == null) return true;
		int result = saveFileChooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			try {
				File newFile = saveFileChooser.getSelectedFile();
				String fileName = newFile.getName();
				String baseName = fileName;
				if (fileName.toLowerCase().endsWith(".properties"))
					baseName = baseName.substring(0,baseName.length()-11);
				rbm.setBaseClass(baseName);
				rbm.setFileDirectory(newFile.getParentFile());
				rbm.writeToFile();
			} catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, Resources.getTranslation("error_saving", ioe.getMessage()),
											  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				if (RBManagerGUI.debug) System.err.println(ioe);
				return false;
			}
		}
		return true;
	}
	
	void updateLocale(Locale l) {
		// Update the menubars
		jMenuBarMain.updateLocale();
		
		updateLocale(getContentPane(), l);
		updateLocale(openFileChooser, l);
		updateLocale(saveFileChooser, l);
		// Redraw the panes
		updateDisplayTree();
		updateProjectTree();
		updateProjectPanels();
		updateDisplayPanels();
		// update the tab titles
		jTabbedPaneMain.setTitleAt(0,Resources.getTranslation("tab_statistics"));
		jTabbedPaneMain.setTitleAt(1,Resources.getTranslation("tab_untranslated"));
		jTabbedPaneMain.setTitleAt(2,Resources.getTranslation("tab_groups"));
		setTitle(Resources.getTranslation("resource_bundle_manager"));
	}
	
	static void updateLocale(Container c, Locale l) {
		Component comp[] = c.getComponents();
		for (int i=0; i < comp.length; i++) {
			if (comp[i] instanceof JComponent) {
				((JComponent)comp[i]).setLocale(l);
			}
			if (comp[i] instanceof Container) {
				updateLocale((Container)comp[i],l);
			}
		}
		if (c instanceof JMenu) {
			comp = ((JMenu)c).getMenuComponents();
			for (int i=0; i < comp.length; i++) {
				if (comp[i] instanceof JComponent) {
					((JComponent)comp[i]).setLocale(l);
				}
				if (comp[i] instanceof Container) {
					updateLocale((Container)comp[i],l);
				}
			}
		}
	}
			
	void updateUI() {
		updateUI(getContentPane());
		jMenuBarMain.updateUI();
		updateUI(jMenuBarMain);
		updateUI(openFileChooser);
		updateUI(saveFileChooser);
	}
	
	static void updateUI(Container c) {
		Component comp[] = c.getComponents();
		for (int i=0; i < comp.length; i++) {
			if (comp[i] instanceof JComponent) {
				((JComponent)comp[i]).updateUI();
			}
			if (comp[i] instanceof Container) {
				updateUI((Container)comp[i]);
			}
		}
		if (c instanceof JMenu) {
			comp = ((JMenu)c).getMenuComponents();
			for (int i=0; i < comp.length; i++) {
				if (comp[i] instanceof JComponent) {
					((JComponent)comp[i]).updateUI();
				}
				if (comp[i] instanceof Container) {
					updateUI((Container)comp[i]);
				}
			}
		}
	}
	
	// Close the window when the close box is clicked
	void thisWindowClosing(java.awt.event.WindowEvent e)
	{
		if (promptForSave(Resources.getTranslation("dialog_quit_save"))) {
			setVisible(false);
			dispose();
			System.exit(0);
		}
	}
	
	public void setUser(String userName) {
		this.userName = userName;
		if (rbm != null) rbm.setUser(userName);
	}
	
	public String getUser() {
		return userName;
	}
	
	public BundleItem getSelectedProjectBundleItem() {
		TreePath path = projectTree.getSelectionPath();
		if (path == null) return null;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object obj = node.getUserObject();
		if (obj == null || !(obj instanceof BundleItem)) return null;
		return (BundleItem)obj;
	}
	
	public RBManager getSelectedProjectBundle() {
		TreePath path = projectTree.getSelectionPath();
		if (path == null) return null;
		for (int i=0; i < path.getPathCount(); i++) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getPathComponent(i);
			Object obj = node.getUserObject();
			if (obj != null && obj instanceof RBManager) return (RBManager)obj;
		}
		return null;
	}
	
	public static void debugMsg(String msg) {
		if (debug) System.out.println("Debug Message [" + debugcount++ + "]: " + msg);
	}
}

/*
class ProjectFrame extends JFrame implements ActionListener {
	private RBManagerGUI parent;
	private JTable table;
	private JButton addButton;
	private JButton selectButton;
	private JButton removeButton;
	private JButton newButton;
	private JButton openButton;
	private JButton closeButton;
	
	private JFileChooser openFileChooser;
	private JFileChooser saveFileChooser;
	
	private File activeFile = null;
	
	public ProjectFrame(RBManagerGUI parent) {
		super(Resources.getTranslation("dialog_project_title",Resources.getTranslation("dialog_project_none_selected")));
		this.parent = parent;
		initComponents();
	}
	
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() == openButton) {
			int result = openFileChooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File file = openFileChooser.getSelectedFile();
				try {
					table.setModel(new ProjectTableModel(file));
					activeFile = file;
					String fileName = file.getName().substring(0,file.getName().length()-10);
					setTitle(Resources.getTranslation("dialog_project_title",fileName));
				} catch (IOException ex) {
					String alert = Resources.getTranslation("error_bad_project_file");
					JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"),
												JOptionPane.ERROR_MESSAGE);
					activeFile = null;
				}
			}
		} else if (ev.getSource() == selectButton) {
			if (table.getSelectedRow() >= 0) {
				int rowIndex = table.getSelectedRow();
				// This item is a recent file selection. We need to open that file
				String fileLocation = (String)table.getValueAt(rowIndex,1);
				try {
					parent.rbm = new RBManager(new File(fileLocation));
					parent.updateDisplayTree();
					parent.updateProjectTree();
					parent.updateDisplayPanels();
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(this,Resources.getTranslation("error_opening_file", ev.getActionCommand()),
												  Resources.getTranslation("dialog_title_error_opening_file"),
												  JOptionPane.ERROR_MESSAGE);
				}
			}
		} else if (ev.getSource() == closeButton) {
			thisWindowClosing(null);
		} else if (ev.getSource() == newButton) {
			int result = saveFileChooser.showSaveDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				File file = saveFileChooser.getSelectedFile();
				try {
					table.setModel(new ProjectTableModel());
					activeFile = file;
					FileWriter fw = new FileWriter(file);
					fw.write("\n");
					fw.flush();
					fw.close();
				} catch (IOException ex) {
					String alert = Resources.getTranslation("error_bad_project_file");
					JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"),
												JOptionPane.ERROR_MESSAGE);
					activeFile = null;
				}
			}
		} else if (ev.getSource() == removeButton) {
			if (table.getSelectedRow() >= 0) {
				int rowIndex = table.getSelectedRow();
				ProjectTableModel ptm = (ProjectTableModel)table.getModel();
				ptm.removeBundle(ptm.getValueAt(rowIndex,1).toString());
				try { ptm.write(activeFile); } catch (IOException ex) {}
			}
		} else if (ev.getSource() == addButton) {
			try {
				File rbFile = parent.rbm.getBaseFile();
				String filePath = rbFile.getAbsolutePath();
				ProjectTableModel ptm = (ProjectTableModel)table.getModel();
				// Check for duplicates
				for (int i=0; i < ptm.getRowCount(); i++) {
					if (ptm.getValueAt(i,1).toString().equals(filePath)) return;
				}
				ptm.addBundle(parent.rbm.getBaseClass(),rbFile.getAbsolutePath());
				ptm.write(activeFile);
			} catch (Exception ex) {}
		}
		updateComponents();
		this.validate();
		this.repaint();
	}
	
	private void thisWindowClosing(WindowEvent ev) {
		setVisible(false);
	}
	
	private void updateComponents() {
		newButton.setEnabled(true);
		openButton.setEnabled(true);
		closeButton.setEnabled(true);
		if (activeFile == null) {
			addButton.setEnabled(false);
			removeButton.setEnabled(false);
			selectButton.setEnabled(false);
		} else {
			addButton.setEnabled(true);
			selectButton.setEnabled(true);
			removeButton.setEnabled(true);
		}
	}

	private void initComponents() {
		setSize(new Dimension(500,300));
		setLocation(200,200);
		getContentPane().setLayout(new BorderLayout());
		
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// the following code sets the frame's initial state
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				thisWindowClosing(e);
			}
		});
		
		openFileChooser = new JFileChooser(".");
		saveFileChooser = new JFileChooser(".");
		
		openFileChooser.setFileFilter(new filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".rbproject"))) return false;
				return true;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_project_file_filter_description");
			}
		});
		
		saveFileChooser.setFileFilter(new filechooser.FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				
				String name = f.getName();
				if (!(name.toLowerCase().endsWith(".rbproject"))) return false;
				return true;
			}
	
			public String getDescription() {
				return Resources.getTranslation("dialog_project_file_filter_description");
			}
		});
		
		table = new JTable();
		initTable();
		
		openButton = new JButton(Resources.getTranslation("button_project_open"));
		selectButton = new JButton(Resources.getTranslation("button_project_select"));
		closeButton = new JButton(Resources.getTranslation("button_project_close"));
		newButton = new JButton(Resources.getTranslation("button_project_new"));
		addButton = new JButton(Resources.getTranslation("button_project_add"));
		removeButton = new JButton(Resources.getTranslation("button_project_remove"));
		openButton.addActionListener(this);
		selectButton.addActionListener(this);
		closeButton.addActionListener(this);
		newButton.addActionListener(this);
		addButton.addActionListener(this);
		removeButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel(new GridLayout(2,3));
		buttonPanel.setBorder(BorderFactory.createEtchedBorder());
		buttonPanel.add(selectButton);
		buttonPanel.add(addButton);
		buttonPanel.add(removeButton);
		buttonPanel.add(newButton);
		buttonPanel.add(openButton);
		buttonPanel.add(closeButton);
		
		getContentPane().add(table, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		updateComponents();
		validate();
	}
	
	private void initTable() {
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setModel(new ProjectTableModel());
	}
}
*/

class RBTreeCellRenderer extends DefaultTreeCellRenderer {
	private static RBTreeCellRenderer cellRend = null;
	private static ImageIcon bundleIcon   = null;
	private static ImageIcon languageIcon = null;
	private static ImageIcon countryIcon  = null;
	private static ImageIcon variantIcon  = null;
	private static ImageIcon fileIcon     = null;
	private static ImageIcon groupIcon    = null;
	private static ImageIcon itemIcon     = null;
	private static ImageIcon projectIcon  = null;
	
	private RBTreeCellRenderer() {
		
	}
	
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
												  boolean leaf, int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
		int level = node.getLevel();
		Object obj = node.getUserObject();
		
		if (obj instanceof BundleGroup) {
			setIcon(groupIcon);
		} else if (obj instanceof BundleItem) {
			setIcon(itemIcon);
		} else if (obj instanceof RBManager) {
			setIcon(bundleIcon);
		} else if (obj instanceof RBProject) {
			setIcon(projectIcon);
		} else if (leaf) {
			if (level != 0) setIcon(fileIcon);	
		} else {	
			if (level == 0) {
				if (obj instanceof String && ((String)obj).equals(Resources.getTranslation("no_project"))) 
					setIcon(projectIcon);
				else setIcon(bundleIcon);
			}
			else if (level == 1) setIcon(languageIcon);
			else if (level == 2) setIcon(countryIcon);
			else if (level == 3) setIcon(variantIcon);
		}
		
		return this;
	}
	
	public static RBTreeCellRenderer getInstance() {
		if (cellRend == null) {
            try {
                Class thisClass = Class.forName("com.ibm.rbm.RBManagerGUI");
			    // Create instances of the icons
                Image scaledImage = (new ImageIcon(thisClass.getResource("images/tree_icon_bundle.gif"))).getImage().getScaledInstance(16, 16, Image.SCALE_DEFAULT);
			    bundleIcon   = new ImageIcon(scaledImage);
			    languageIcon = new ImageIcon(thisClass.getResource("images/tree_icon_language.gif"));
			    countryIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_country.gif"));
			    variantIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_variant.gif"));
			    fileIcon     = new ImageIcon(thisClass.getResource("images/tree_icon_file.gif"));
			    groupIcon    = new ImageIcon(thisClass.getResource("images/tree_icon_group.gif"));
			    itemIcon     = new ImageIcon(thisClass.getResource("images/tree_icon_item.gif"));
			    projectIcon  = new ImageIcon(thisClass.getResource("images/tree_icon_project.gif"));
            } catch (ClassNotFoundException e) {
                RBManagerGUI.debugMsg(e.toString());
            }
			// Create the instance of the renderer
			cellRend = new RBTreeCellRenderer();
		}
		return cellRend;
	}
}

// A dialog which allows the user to create a new Bundle Item

class BundleItemCreationDialog extends JDialog {
	RBManager rbm;
	String groupName;
	BundleItem item;
	boolean firstInit = true;
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 275;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	Box box4 = new Box(BoxLayout.X_AXIS);
	Box box5 = new Box(BoxLayout.X_AXIS);
	Box box6 = new Box(BoxLayout.X_AXIS);
	
	JLabel instructionsLabel = new JLabel("");
	JLabel groupLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_key"));
	JLabel transLabel = new JLabel(Resources.getTranslation("dialog_translation"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_comment"));
	JLabel lookupLabel = new JLabel(Resources.getTranslation("dialog_lookups"));
	
	JComboBox groupComboBox = new JComboBox();
	JTextField nameField = new JTextField("");
	JTextField transField = new JTextField("");
	JTextField commentField = new JTextField("");
	JTextField lookupFields[] = null;
	JLabel noLookupLabel = null;
	Box lookupBox = null;
	Box lookupBoxes[] = null;
	JLabel lookupLabels[] = null;
	
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton createMoreButton = new JButton(Resources.getTranslation("button_create_more"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	Hashtable lookups = new Hashtable();
	
	public BundleItemCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		groupName = null;
		item = null;
		initComponents();
	}
	
	public BundleItemCreationDialog(String groupName, RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		this.groupName = groupName;
		item = null;
		initComponents();
	}
	
	public BundleItemCreationDialog(BundleItem item, RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.item = item;
		this.rbm = rbm;
		groupName = item.getParentGroup().getName();
		initComponents();
	}
	
	boolean createItem() {
		if (rbm == null) return false;
		Hashtable lookupHash = new Hashtable();
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookupHash.put(name,value);
			}
		}
		return rbm.createItem(nameField.getText().trim(), transField.getText().trim(),
							  ((BundleGroup)groupComboBox.getSelectedItem()).getName(),
							  commentField.getText().trim(), lookupHash);
	}
	
	boolean editItem() {
		if (item == null) return false;
		Hashtable lookupHash = new Hashtable();
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookupHash.put(name,value);
			}
		}
		return rbm.editItem(item, nameField.getText().trim(),
							transField.getText().trim(), ((BundleGroup)groupComboBox.getSelectedItem()).getName(),
							commentField.getText().trim(), lookupHash); 
	}
	
	private void clearComponents() {
		nameField.setText("");
		transField.setText("");
		commentField.setText("");
		initComponents();
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.getID() == KeyEvent.KEY_RELEASED) {
			if (transField.hasFocus()) {
				// If we are in the translation field, then enter should create a new line character, not exit the dialog
				int caretPos = transField.getCaretPosition();
				String oldText = transField.getText();
				transField.setText(oldText.substring(0,caretPos) + "\n" + oldText.substring(caretPos,oldText.length()));
				transField.setCaretPosition(caretPos+1);
				validate();
				setSize(getPreferredSize());
				return;
			}
			
			BundleItemCreationDialog dialog = this;
			boolean success = false;
			if (dialog.item == null) success = dialog.createItem();
			else success = dialog.editItem();
			if (!success) {
				String alert = (item == null ? Resources.getTranslation("error_create_item") :
											   Resources.getTranslation("error_modify_item"));
				alert += " " + Resources.getTranslation("error_try_again_item");
				JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
											  JOptionPane.ERROR_MESSAGE);
			} else {
				((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
				((RBManagerGUI)dialog.getParent()).invalidate();
				//((RBManagerGUI)dialog.getParent()).validateMyTree();
				dialog.setVisible(false);
				dialog.dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_ESCAPE) {
			closeWindow();
		}
	}
	
	public void initComponents(){
		enableEvents(AWTEvent.KEY_EVENT_MASK);
		// Error check
		if (rbm == null || rbm.bundles == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_item");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		Bundle mainBundle = (Bundle)rbm.bundles.firstElement();
		if (firstInit) {
			groupComboBox = new JComboBox(mainBundle.getGroupsAsVector());
			if (groupName != null) {
				for (int i = 0; i < groupComboBox.getItemCount(); i++) {
					BundleGroup bg = (BundleGroup)groupComboBox.getItemAt(i);
					if (bg.getName().equals(groupName)) {
						groupComboBox.setSelectedIndex(i);
						break;
					}
				}
			}
		}
	
		if (firstInit && item != null) {
			// We are editing, not creating an item
			createButton.setText(Resources.getTranslation("button_edit"));
			createMoreButton.setText(Resources.getTranslation("button_edit_more"));
			if (item.getKey() != null) nameField.setText(item.getKey());
			if (item.getComment() != null) commentField.setText(item.getComment());
			if (item.getTranslation() != null) transField.setText(item.getTranslation());
			if (item.getLookups() != null) lookups = item.getLookups();
		}
		
		String currentTrans = transField.getText();
		// ** LOOKUPS **
		// Update the lookups if necessary
		if (lookupBoxes != null) {
			for (int i=0; i < lookupBoxes.length; i++) {
				String nameText = lookupLabels[i].getText().trim();
				String name = nameText.substring(nameText.indexOf("{")+1,nameText.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				lookups.put(name,value);
			}
		}
		// Remove old lookups if necessary
		Enumeration enum = lookups.keys();
		while (enum.hasMoreElements()) {
			String name = (String)enum.nextElement();
			if (currentTrans.indexOf("{" + name + "}") < 0) {
				lookups.remove(name);
			}
		}
		// Add new lookups if neccesary
		if (currentTrans != null && currentTrans.indexOf("{") >= 0) {
			while (currentTrans.indexOf("{") >= 0) {
				currentTrans = currentTrans.substring(currentTrans.indexOf("{")+1,currentTrans.length());
				String name = currentTrans.substring(0,currentTrans.indexOf("}"));
				if (!lookups.containsKey(name)) {
					lookups.put(name,"");
				}
			}
		}
		// Remove components
		box5.removeAll();
		
		// Now create the visual components for the lookups
		if (lookups.size() > 0) {
			noLookupLabel = null;
			lookupBox = new Box(BoxLayout.Y_AXIS);
			lookupBoxes = new Box[lookups.size()];
			lookupFields = new JTextField[lookups.size()];
			lookupLabels = new JLabel[lookups.size()];
			int count = 0;
			enum = lookups.keys();
			while (enum.hasMoreElements()) {
				String name = (String)enum.nextElement();
				String value = (String)lookups.get(name);
				RBManagerGUI.debugMsg("Lookup: " + name + " -> " + value);
				RBManagerGUI.debugMsg(lookups.toString());
				lookupBoxes[count] = new Box(BoxLayout.X_AXIS);
				lookupFields[count] = new JTextField((value == null ? "" : value));
				lookupLabels[count] = new JLabel("{" + name + "}");
				lookupBoxes[count].add(Box.createHorizontalGlue());
				lookupBoxes[count].add(lookupLabels[count]);
				lookupBoxes[count].add(Box.createHorizontalStrut(5));
				lookupBoxes[count].add(lookupFields[count]);
				lookupBox.add(lookupBoxes[count]);
				count++;
			}
		} else {
			lookupBox = null;
			lookupBoxes = null;
			lookupFields = null;
			lookupLabels = null;
			noLookupLabel = new JLabel(Resources.getTranslation("none"));
		}
		
		// Set up the components
		if (firstInit) {
			groupLabel.setPreferredSize(leftDim);
			groupComboBox.setPreferredSize(rightDim);
			nameLabel.setPreferredSize(leftDim);
			nameField.setColumns(30);
			commentLabel.setPreferredSize(leftDim);
			commentField.setColumns(30);
			transLabel.setPreferredSize(leftDim);
			transField.setColumns(30);
			lookupLabel.setPreferredSize(leftDim);
			
			box1.add(groupLabel); box1.add(groupComboBox);
			box2.add(nameLabel); box2.add(nameField);
			box4.add(commentLabel); box4.add(commentField);
			box3.add(transLabel); box3.add(transField);
			
			createButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_trigger")));
			createMoreButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_more_trigger")));
		}
		box5.add(Box.createHorizontalGlue()); box5.add(lookupLabel); box5.add(Box.createHorizontalStrut(5));
		if (noLookupLabel != null) {
			noLookupLabel.setPreferredSize(rightDim);
			box5.add(noLookupLabel);
		}
		else box5.add(lookupBox);
		if (firstInit) {
			box6.add(createButton);
			box6.add(Box.createHorizontalStrut(5));
			if (item == null) box6.add(createMoreButton);
			box6.add(Box.createHorizontalStrut(5));
			box6.add(cancelButton);
		}
		
		instructionsLabel.setBorder(BorderFactory.createEtchedBorder());
		
		// Add the appropriate listeners
		if (firstInit) {
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					dialog.setVisible(false);
					dialog.dispose();
				}
			});
		
			createButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					boolean success = false;
					if (dialog.item == null) success = dialog.createItem();
					else success = dialog.editItem();
					if (!success) {
						String alert = (item == null ? Resources.getTranslation("error_create_item") :
													   Resources.getTranslation("error_modify_item"));
						alert += " " + Resources.getTranslation("error_try_again_item");
						JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
													  JOptionPane.ERROR_MESSAGE);
					} else {
						((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
						((RBManagerGUI)dialog.getParent()).invalidate();
						//((RBManagerGUI)dialog.getParent()).validateMyTree();
						dialog.setVisible(false);
						dialog.dispose();
					}
				}
			});
			
			createMoreButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					boolean success = false;
					if (dialog.item == null) success = createItem();
					else success = dialog.editItem();
					if (!success) {
						String alert = (item == null ? Resources.getTranslation("error_create_item") :
													   Resources.getTranslation("error_modify_item"));
						alert += " " + Resources.getTranslation("error_try_again_item");
						JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"),
													  JOptionPane.ERROR_MESSAGE);
					} else {
						((RBManagerGUI)dialog.getParent()).updateDisplayPanels();
						((RBManagerGUI)dialog.getParent()).invalidate();
						//((RBManagerGUI)dialog.getParent()).validateMyTree();
						dialog.clearComponents();
					}
				}
			});
		
			transField.addFocusListener(new FocusListener() {
				public void focusGained(FocusEvent ev) {}
				public void focusLost(FocusEvent ev) {
					BundleItemCreationDialog dialog =
						(BundleItemCreationDialog)((JTextField)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					firstInit = false;
					dialog.initComponents();
				}
			});
		}
		
		// Complete the initialization of the frame
		if (firstInit) setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		//mainBox.add(instructionsLabel);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box1);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box2);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box3);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box4);
		mainBox.add(Box.createVerticalStrut(5));
		if (noLookupLabel == null) {
			mainBox.add(box5);
			mainBox.add(Box.createVerticalStrut(5));
		}
		mainBox.add(box6);
		getContentPane().add(mainBox, BorderLayout.CENTER);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
		firstInit = false;
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}


// A dialog which allows the user to create a new Bundle Group

class BundleGroupCreationDialog extends JDialog {
	RBManager rbm;
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 275;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	
	JTextArea instructionsArea = new JTextArea("");
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_group_comment"));
	JTextField nameField = new JTextField("");
	JTextField commentField = new JTextField("");
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	
	public BundleGroupCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	boolean createGroup() {
		if (rbm == null) return false;
		return rbm.createGroup(nameField.getText().trim(), commentField.getText().trim());
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = createGroup();
			if (!success) {
				String alert = Resources.getTranslation("error_create_group") + " " +
							   Resources.getTranslation("error_try_again_group");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	public void initComponents(){
		// Error check
		if (rbm == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_group");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		
		// Set up the components
		nameLabel.setPreferredSize(leftDim);
		nameField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		commentField.setColumns(30);
		
		box1.add(nameLabel); box1.add(nameField);
		box2.add(commentLabel); box2.add(commentField);
		box3.add(createButton);
		box3.add(Box.createHorizontalStrut(5));
		box3.add(cancelButton);
		
		instructionsArea.setBorder(BorderFactory.createEtchedBorder());
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				BundleGroupCreationDialog dialog =
					(BundleGroupCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.createGroup();
				if (!success) {
					String alert = Resources.getTranslation("error_create_group") + " " +
								   Resources.getTranslation("error_try_again_group");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				} else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		
		// Complete the initialization of the frame
		setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		mainBox.add(box1);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box2);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().removeAll();
		//getContentPane().add(instructionsArea, BorderLayout.NORTH);
		getContentPane().add(mainBox, BorderLayout.CENTER);
		getContentPane().add(box3, BorderLayout.SOUTH);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}

// A dialog which allows the user to create a new Bundle Group

class BundleGroupEditDialog extends JDialog {
	BundleGroup group;
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 275;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	
	JLabel nameLabel = new JLabel(Resources.getTranslation("dialog_group"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_group_comment"));
	JTextField nameField = new JTextField("");
	JTextField commentField = new JTextField("");
	JButton editButton = new JButton(Resources.getTranslation("button_edit"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	
	public BundleGroupEditDialog(BundleGroup group, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.group = group;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = editGroup();
			if (!success) {
				String alert = Resources.getTranslation("error_modify_group");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error_internal"),
											  JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	boolean editGroup() {
		if (group == null) return false;
		group.setName(nameField.getText().trim());
		group.setComment(commentField.getText().trim());
		return true;
	}
	
	public void initComponents(){
		// Error check
		if (group == null) {
			String alert = Resources.getTranslation("error_modify_group");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error_internal"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		
		// Set up the components
		nameLabel.setPreferredSize(leftDim);
		nameField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		commentField.setColumns(30);
		
		nameField.setText(group.getName());
		commentField.setText(group.getComment());
		
		box1.add(nameLabel); box1.add(nameField);
		box2.add(commentLabel); box2.add(commentField);
		box3.add(editButton);
		box3.add(Box.createHorizontalStrut(5));
		box3.add(cancelButton);
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				BundleGroupEditDialog dialog =
					(BundleGroupEditDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.editGroup();
				if (!success) {
					String alert = Resources.getTranslation("error_modify_group");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error_internal"),
												  JOptionPane.ERROR_MESSAGE);
				} else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		
		// Complete the initialization of the frame
		setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		mainBox.add(box1);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box2);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().removeAll();
		getContentPane().add(mainBox, BorderLayout.CENTER);
		getContentPane().add(box3, BorderLayout.SOUTH);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}

// A dialog for creating new Resource Files

class ResourceCreationDialog extends JDialog {
	RBManager rbm;
	RBManagerGUI gui;
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box infoBox = new Box(BoxLayout.Y_AXIS);
	JPanel infoPanel = new JPanel();
	JPanel infoTitlePanel = new JPanel();
	JPanel infoCommentPanel = new JPanel();
	JPanel infoManagerPanel = new JPanel();
	JPanel langPanel = new JPanel();
	JPanel counPanel = new JPanel();
	JPanel variPanel = new JPanel();
	JPanel buttPanel = new JPanel();
	
	JLabel instructionsLabel = new JLabel("");
	JLabel titleLabel = new JLabel(Resources.getTranslation("dialog_file_title"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_file_comment"));
	JLabel managerLabel = new JLabel(Resources.getTranslation("dialog_file_manager"));
	JLabel enc1Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel enc2Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel enc3Label = new JLabel(Resources.getTranslation("dialog_encoding"));
	JLabel nam1Label = new JLabel(Resources.getTranslation("dialog_name"));
	JLabel nam2Label = new JLabel(Resources.getTranslation("dialog_name"));
	JLabel nam3Label = new JLabel(Resources.getTranslation("dialog_name"));
	
	JTextField titleField = new JTextField("");
	JTextField commentField = new JTextField("");
	JTextField managerField = new JTextField("");
	JTextField enc1Field = new JTextField("");
	JTextField enc2Field = new JTextField("");
	JTextField enc3Field = new JTextField("");
	JTextField nam1Field = new JTextField("");
	JTextField nam2Field = new JTextField("");
	JTextField nam3Field = new JTextField("");
	
	JCheckBox copyCheckBox = new JCheckBox(Resources.getTranslation("dialog_checkbox_copy_elements"), true);
	
	JButton createButton = new JButton(Resources.getTranslation("button_create"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	
	public ResourceCreationDialog(RBManager rbm, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.gui = (RBManagerGUI)frame;
		this.rbm = rbm;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER) {
			boolean success = createResource();
			if (!success) {
				String alert = Resources.getTranslation("error_create_file") + " " +
							   Resources.getTranslation("error_try_again_file");
				JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			} else {
				setVisible(false);
				dispose();
			}
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	boolean createResource() {
		if (rbm == null) return false;
		String encoding = enc1Field.getText().trim();
		String enc2 = enc2Field.getText().trim();
		if (enc2 != null && !enc2.equals("")) encoding += "_" + enc2;
		String enc3 = enc3Field.getText().trim();
		if (enc3 != null && !enc3.equals("")) encoding += "_" + enc3;
		boolean ret = rbm.createResource(titleField.getText().trim(), commentField.getText().trim(), managerField.getText().trim(),
							encoding, nam1Field.getText().trim(), nam2Field.getText().trim(),
							nam3Field.getText().trim(), copyCheckBox.isSelected());
		if (ret) { 
			gui.updateDisplayTree();
			gui.updateProjectTree();
			gui.updateProjectPanels();
		}
		return ret;
	}
	
	public void initComponents(){
		// Error check
		if (rbm == null) {
			String alert = Resources.getTranslation("error_no_bundle_for_file");
			JOptionPane.showMessageDialog(this, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			closeWindow();
			return;
		}
		
		// Initialize values
		int tempWidth = 175;
		Dimension labelDim = new Dimension(tempWidth, 30);
		titleLabel.setPreferredSize(labelDim);
		commentLabel.setPreferredSize(labelDim);
		managerLabel.setPreferredSize(labelDim);
		
		infoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_file_info")));
		langPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_language")));
		counPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_country")));
		variPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
															 Resources.getTranslation("dialog_variant")));
		
		Dimension encDim = new Dimension(50,20);
		Dimension namDim = new Dimension(350,20);
		Dimension othDim = new Dimension(400,20);
		
		titleField.setColumns(30);
		commentField.setColumns(30);
		managerField.setColumns(30);
		
		enc1Field.setColumns(3);
		enc2Field.setColumns(3);
		enc3Field.setColumns(3);
		nam1Field.setColumns(20);
		nam2Field.setColumns(20);
		nam3Field.setColumns(20);
		
		// Set up the components
		infoTitlePanel.add(titleLabel); infoTitlePanel.add(titleField);
		infoCommentPanel.add(commentLabel); infoCommentPanel.add(commentField);
		infoManagerPanel.add(managerLabel); infoManagerPanel.add(managerField);
		infoBox.add(infoTitlePanel);
		infoBox.add(infoCommentPanel);
		infoBox.add(infoManagerPanel);
		infoPanel.add(infoBox);
		
		langPanel.add(enc1Label); langPanel.add(enc1Field); langPanel.add(nam1Label); langPanel.add(nam1Field);
		counPanel.add(enc2Label); counPanel.add(enc2Field); counPanel.add(nam2Label); counPanel.add(nam2Field);
		variPanel.add(enc3Label); variPanel.add(enc3Field); variPanel.add(nam3Label); variPanel.add(nam3Field);
		
		buttPanel.add(createButton); buttPanel.add(cancelButton);
		
		// Add the appropriate listeners
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
		
		createButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				ResourceCreationDialog dialog =
					(ResourceCreationDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
				boolean success = dialog.createResource();
				if (!success) {
					String alert = Resources.getTranslation("error_create_file") + " " +
								   Resources.getTranslation("error_try_again_file");
					JOptionPane.showMessageDialog(dialog, alert, Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
				} else {
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		});
		
		// Complete the component layout
		mainBox.removeAll();
		//mainBox.add(instructionsLabel);
		mainBox.add(infoPanel);
		mainBox.add(langPanel);
		mainBox.add(counPanel);
		mainBox.add(variPanel);
		mainBox.add(copyCheckBox);
		mainBox.add(buttPanel);
		
		setLocation(new java.awt.Point(50, 50));
		getContentPane().add(mainBox, BorderLayout.CENTER);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
}

// A dialog which displays the properties of a Bundle Item in an editable way

class BundleItemDialog extends JDialog implements ActionListener {
	RBManager rbm;
	BundleItem item;
	String user;
	boolean firstInit = true;	
	
	// Helper data
	int left_col_width = 125;
	int right_col_width = 375;
	int row_height = 25;
	Dimension leftDim = new Dimension(left_col_width, row_height);
	Dimension rightDim = new Dimension(right_col_width, row_height);
	
	// Components
	Box mainBox = new Box(BoxLayout.Y_AXIS);
	Box box0 = new Box(BoxLayout.X_AXIS);
	Box box1 = new Box(BoxLayout.X_AXIS);
	Box box2 = new Box(BoxLayout.X_AXIS);
	Box box3 = new Box(BoxLayout.X_AXIS);
	Box box4 = new Box(BoxLayout.X_AXIS);
	Box box5 = new Box(BoxLayout.X_AXIS);
	Box box6 = new Box(BoxLayout.X_AXIS);
	Box box7 = new Box(BoxLayout.X_AXIS);
	Box box8 = new Box(BoxLayout.X_AXIS);
	
	JLabel groupLabel = new JLabel(Resources.getTranslation("dialog_group")); 
	JLabel keyLabel = new JLabel(Resources.getTranslation("dialog_key"));
	JLabel defTransLabel = new JLabel(Resources.getTranslation("dialog_default_translation"));
	JLabel transLabel = new JLabel(Resources.getTranslation("dialog_translation"));
	JLabel commentLabel = new JLabel(Resources.getTranslation("dialog_comment"));
	JLabel lookupLabel = new JLabel(Resources.getTranslation("dialog_lookups"));
	JLabel createdLabel = new JLabel(Resources.getTranslation("dialog_created"));
	JLabel modifiedLabel = new JLabel(Resources.getTranslation("dialog_modified"));
	
	JComboBox groupComboBox;
	JTextField keyField;
	JTextField transField;
	JTextField defTransField;
	JTextField commentField;
	JLabel createdLabel2;
	JLabel modifiedLabel2;
	JLabel lookupLabel2 = null;
	JCheckBox transCheckBox;
	JButton saveButton = new JButton(Resources.getTranslation("button_edit"));
	JButton cancelButton = new JButton(Resources.getTranslation("button_cancel"));
	Box lookupBox = null;
	Box lookups[] = null;
	JLabel lookupLabels[] = null;
	JTextField lookupFields[] = null;
	
	public BundleItemDialog(RBManager rbm, BundleItem item, String user, JFrame frame, String title, boolean modal) {
		super(frame, title, modal);
		this.rbm = rbm;
		this.user = user;
		this.item = item;
		initComponents();
		enableEvents(AWTEvent.KEY_EVENT_MASK);
	}
	
	protected void processKeyEvent(KeyEvent ev) {
		if (ev.getKeyCode() == KeyEvent.VK_ENTER && ev.getID() == KeyEvent.KEY_RELEASED) {
			actionPerformed(null);
		} else if (ev.getKeyCode() == KeyEvent.VK_CANCEL) {
			closeWindow();
		}
	}
	
	public void initComponents(){
		// Error check
		if (item == null) closeWindow();
		if (!firstInit) closeWindow();
		
		// Initialize values
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		Bundle bundle = item.getParentGroup().getParentBundle();
		
		// Lookup the default translation
		String defTrans = new String();
		Object o = ((Bundle)rbm.bundles.firstElement()).allItems.get(item.getKey());
		if (o != null) defTrans = ((BundleItem)o).getTranslation();
		
		keyField = new JTextField(item.getKey());
		keyField.setEnabled(false);
		defTransField = new JTextField(defTrans);
		defTransField.setEnabled(false);
		transField = new JTextField(item.getTranslation());
		commentField = new JTextField(item.getComment());
		String created[] = {df.format(item.getCreatedDate()), item.getCreator()};
		String modified[] = {df.format(item.getModifiedDate()), item.getModifier()};
		String createdString = Resources.getTranslation("dialog_date_person", created);
		String modifiedString = Resources.getTranslation("dialog_date_person", modified);
		createdLabel2 = new JLabel(item.getCreator() == null ? df.format(item.getCreatedDate()) : createdString);
		modifiedLabel2 = new JLabel(item.getModifier() == null ? df.format(item.getModifiedDate()) : modifiedString);
		transCheckBox = new JCheckBox(Resources.getTranslation("dialog_checkbox_translated"),item.isTranslated());
		
		groupComboBox = new JComboBox(bundle.getGroupsAsVector());
		for (int i=0; i < groupComboBox.getItemCount(); i++) {
			BundleGroup bg = (BundleGroup)groupComboBox.getItemAt(i);
			if (bg.getName().equals(item.getParentGroup().getName())) {
				groupComboBox.setSelectedIndex(i);
				break;
			}
		}
		groupComboBox.setEnabled(false);
		
		// Set up the components
		groupLabel.setPreferredSize(leftDim);
		groupComboBox.setPreferredSize(rightDim);
		keyLabel.setPreferredSize(leftDim);
		//keyField.setPreferredSize(rightDim);
		keyField.setColumns(30);
		defTransLabel.setPreferredSize(leftDim);
		//defTransField.setPreferredSize(rightDim);
		defTransField.setColumns(30);
		transLabel.setPreferredSize(leftDim);
		//transField.setPreferredSize(rightDim);
		transField.setColumns(30);
		commentLabel.setPreferredSize(leftDim);
		//commentField.setPreferredSize(rightDim);
		commentField.setColumns(30);
		lookupLabel.setPreferredSize(leftDim);
		createdLabel.setPreferredSize(leftDim);
		createdLabel2.setPreferredSize(rightDim);
		modifiedLabel.setPreferredSize(leftDim);
		modifiedLabel2.setPreferredSize(rightDim);
		// Special setup for the lookup items if they exist
		if (item.getLookups().size() < 1) {
			lookupLabel2 = new JLabel(Resources.getTranslation("none"));
			lookupLabel2.setPreferredSize(rightDim);
		} else {
			lookupBox = new Box(BoxLayout.Y_AXIS);
			lookups = new Box[item.getLookups().size()];
			lookupLabels = new JLabel[item.getLookups().size()];
			lookupFields = new JTextField[item.getLookups().size()];
			Enumeration enum = item.getLookups().keys();
			for (int i = 0; i < item.getLookups().size(); i++) {
				String name = (String)enum.nextElement();
				String value = (String)item.getLookups().get(name);
				RBManagerGUI.debugMsg("X - Lookup: " + name + " -> " + value);
				lookups[i] = new Box(BoxLayout.X_AXIS);
				lookupLabels[i] = new JLabel("{" + name + "}");
				lookupLabels[i].setPreferredSize(new Dimension(30,row_height));
				lookupFields[i] = new JTextField(value);
				lookupFields[i].setPreferredSize(new Dimension(right_col_width-35,row_height));
				lookups[i].add(Box.createHorizontalGlue());
				lookups[i].add(lookupLabels[i]);
				lookups[i].add(Box.createHorizontalStrut(5));
				lookups[i].add(lookupFields[i]);
				lookupBox.add(lookups[i]);
			}
		}
		
		// Add the appropriate listeners
		if (firstInit) {
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					JDialog dialog = (JDialog)((JButton)ev.getSource()).getParent().getParent().getParent().getParent().getParent().getParent();
					dialog.setVisible(false);
					dialog.dispose();
				}
			});
		
			saveButton.addActionListener(this);
		
			transField.addFocusListener(new TranslationFocusListener(item.getTranslation(),transCheckBox));
		}
		
		box0.add(groupLabel); box0.add(groupComboBox);
		box1.add(keyLabel); box1.add(keyField);
		box8.add(defTransLabel); box8.add(defTransField);
		box2.add(transLabel); box2.add(transField);
		box3.add(commentLabel); box3.add(commentField);
		box4.add(Box.createHorizontalGlue()); box4.add(lookupLabel);
		if (lookupLabel2 != null) {
			box4.add(Box.createHorizontalStrut(5));
			box4.add(lookupLabel2);
		} else if (lookupBox != null) {
			box4.add(Box.createHorizontalStrut(5));
			box4.add(lookupBox);
		}
		box5.add(Box.createHorizontalGlue()); box5.add(createdLabel);
		box5.add(Box.createHorizontalStrut(5)); box5.add(createdLabel2);
		box6.add(Box.createHorizontalGlue()); box6.add(modifiedLabel);
		box6.add(Box.createHorizontalStrut(5)); box6.add(modifiedLabel2);
		box7.add(transCheckBox); box7.add(saveButton); box7.add(cancelButton);
		
		// Complete the initialization of the frame
		setLocation(new java.awt.Point(50, 50));
		mainBox.removeAll();
		mainBox.add(box0);
		mainBox.add(box1);
		mainBox.add(box8);
		mainBox.add(box2);
		mainBox.add(box3);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box4);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box5);
		mainBox.add(box6);
		mainBox.add(Box.createVerticalStrut(5));
		mainBox.add(box7);
		getContentPane().add(mainBox, BorderLayout.CENTER);
		validateTree();
		pack();
		setVisible(true);
		//setResizable(false);
		
		firstInit = false;
	}
	
	void closeWindow() {
		setVisible(false);
		dispose();
	}
	
	public void actionPerformed(ActionEvent ev) {
		if (ev == null && transField.hasFocus()) {
			// If we are in the translation field, then enter should create a new line character, not exit the dialog
			int caretPos = transField.getCaretPosition();
			String oldText = transField.getText();
			transField.setText(oldText.substring(0,caretPos) + "\n" + oldText.substring(caretPos,oldText.length()));
			transField.setCaretPosition(caretPos+1);
			validate();
			setSize(getPreferredSize());
			return;
		}
		
		// This action is called when the 'Edit' button is pressed
		item.setTranslation(transField.getText().trim());
		if (!item.getKey().equals(keyField.getText())) item.setKey(keyField.getText().trim());
		item.setComment(commentField.getText());
		item.setModifiedDate(new Date());
		item.setModifier(user);
		item.setTranslated(transCheckBox.isSelected());
		if (transCheckBox.isSelected()) {
			// Remove this item from the untranslated items, if it is there
			item.getParentGroup().getParentBundle().removeUntranslatedItem(item.getKey());
		} else {
			item.getParentGroup().getParentBundle().addUntranslatedItem(item);
		}
		if (lookups != null) {
			item.setLookups(new Hashtable());
			for (int i=0; i < lookups.length; i++) {
				String name = lookupLabels[i].getText().trim();
				if (name.indexOf("{") >= 0) name = name.substring(name.indexOf("{")+1,name.length());
				if (name.indexOf("}") >= 0) name = name.substring(0, name.indexOf("}"));
				String value = lookupFields[i].getText().trim();
				item.getLookups().put(name,value);
			}
		}
		closeWindow();
	}
}
 
// The action listener which monitors changes in the group to display

class GroupComboActionListener implements ActionListener {
	JTable table;
	JList list;
	
	protected GroupComboActionListener(JTable table) {
		list = null;
		this.table = table;
	}
	
	protected GroupComboActionListener(JList list) {
		table = null;
		this.list = list;
	}
	
	public void actionPerformed(ActionEvent ev) {
		JComboBox cbox = (JComboBox)ev.getSource();
		
		if (table != null) {
			BundleGroup bg = (BundleGroup)cbox.getSelectedItem();
			((GroupItemsTableModel)table.getModel()).setGroup((BundleGroup)cbox.getSelectedItem());
			//table.validate();
			Container c = table.getParent();
			while (!(c instanceof RBGroupPanel)) { c = c.getParent(); } 
			((RBGroupPanel)c).updateComponents();
		} else if (list != null) {
			list.setListData(((BundleGroup)cbox.getSelectedItem()).getItemsAsVector());
			//list.validate();
			Container c = list.getParent();
			while (!(c instanceof RBGroupPanel)) { c = c.getParent(); } 
			((RBGroupPanel)c).updateComponents();
		} else RBManagerGUI.debugMsg("Selection changed, but no active components");
	}
}

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
	
	public void thisWindowClosing() {
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
			String encoding = definedCombo.getSelectedItem().toString();
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

// A dialog displaying information about this application

class AboutDialog {
	public static JDialog dialog = null;
	
	public static void showDialog(Frame parent) {
		if (dialog == null) {
			dialog = new JDialog(parent, Resources.getTranslation("dialog_title_about_rbmanager"), false);
			initComponents();
		}
		dialog.setVisible(true);
	}
	
	private static void initComponents() {
		dialog.getContentPane().setLayout(new BorderLayout());
        JLabel logoLabel = null;
		JLabel titleLabel = new JLabel(Resources.getTranslation("rbmanager"));
		JLabel versionLabel = new JLabel(Resources.getTranslation("version", Package.getPackage("com.ibm.rbm").getImplementationVersion()));
		JLabel copyrightLabel = new JLabel(Resources.getTranslation("copyright"));
		JLabel contactLabel = new JLabel(Resources.getTranslation("rbmanager_contact"));
		JPanel panel = new JPanel();
		Box box = new Box(BoxLayout.Y_AXIS);
		
        try {
            Class thisClass = Class.forName("com.ibm.rbm.AboutDialog");
		    logoLabel = new JLabel(new ImageIcon(thisClass.getResource("images/" +
									      Resources.getTranslation("logo_filename"))));
        } catch (ClassNotFoundException e) {
            RBManagerGUI.debugMsg(e.toString());
        }

		box.add(titleLabel);
		box.add(versionLabel);
		box.add(Box.createVerticalStrut(10));
		box.add(copyrightLabel);
		box.add(Box.createVerticalStrut(5));
		box.add(contactLabel);
		
		panel.add(box);
		dialog.getContentPane().add(logoLabel, BorderLayout.WEST);
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		
		dialog.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent ev) { }
			public void mouseReleased(MouseEvent ev) {
				hideDialog();
			}
			public void mouseEntered(MouseEvent ev) { }
			public void mouseExited(MouseEvent ev) { }
			public void mouseClicked(MouseEvent ev) { }
		});
		
		//dialog.validate();
		dialog.pack();
		dialog.setLocation(new Point(50,50));
		//dialog.setResizable(false);
	}
	
	private static void hideDialog() {
		dialog.setVisible(false);
	}
}

// The list model for groups

class GroupItemsListModel extends AbstractListModel {
	BundleGroup group;
	
	public void setGroup(BundleGroup group) {
		this.group = group;
	}
	
	public GroupItemsListModel(BundleGroup group) {
		this.group = group;
	}
	
	public int getSize() {
		if (group == null) return 0;
		return group.getItemCount();
	}
	
	public Object getElementAt(int index) {
		return group.getBundleItem(index);
	}
	
	public void update() {
		fireContentsChanged(this, 0, getSize()-1);
	}
}

// Table model for resource bundle projects

class RBProject {
	java.util.List bundleNames;
	java.util.List bundleFileNames;
	java.util.List bundles;
	String projectName;
	
	public RBProject(String projectName) {
		this.projectName = projectName;
		bundleNames = new java.util.LinkedList();
		bundleFileNames = new java.util.LinkedList();
		bundles = new java.util.LinkedList();
	}
	
	public RBProject(File inputFile) throws IOException {
		this(inputFile.getName());
		
		if (projectName.indexOf(".") > 0) {
			projectName = projectName.substring(0,projectName.lastIndexOf("."));
		}
		
		FileReader fr = new FileReader(inputFile);
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		int linecount = 0;
		while ((line = br.readLine()) != null) {
			if (linecount % 2 == 0) {
				bundleNames.add(line.trim());
			} else {
				bundleFileNames.add(line.trim());
			}
			linecount++;
		}
		fr.close();
		try {
			for (int i=0; i < bundleFileNames.size(); i++) {
				RBManager rbm = new RBManager(new File((String)bundleFileNames.get(i)));
				bundles.add(rbm);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(new JFrame(), Resources.getTranslation("error_load_project"),
										  Resources.getTranslation("error"), JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
			bundleNames.clear();
			bundleFileNames.clear();
		}
	}
	
	public String toString() { return projectName; }
	
	public int getSize() { return bundleNames.size(); }
	
	public String getBundleName(int index) {
		return (String)bundleNames.get(index);	
	}
	
	public String getFileName(int index) {
		return (String)bundleFileNames.get(index);	
	}
	
	public RBManager getBundle(int index) {
		return (RBManager)bundles.get(index);
	}
	
	public RBManager getBundle(String bundleName) {
		int index = bundleNames.indexOf(bundleName);
		if (index >= 0) return getBundle(index);
		return null;
	}
	
	public void write(File outputFile) throws IOException {
		FileWriter fw = new FileWriter(outputFile);
		for (int i=0; i < bundleNames.size(); i++) {
			fw.write((String)bundleNames.get(i));
			fw.write("\n");
			fw.write((String)bundleFileNames.get(i));
			if (i != bundleNames.size()-1) fw.write("\n");
		}
		fw.flush();
		fw.close();
	}
	
	public void addBundle(String bundleFileName) throws IOException {
		RBManager bundle = new RBManager(new File(bundleFileName));
		bundles.add(bundle);
		bundleNames.add(bundle.getBaseClass());
		bundleFileNames.add(bundleFileName);
	}
	
	public void removeBundle(String bundleName) {
		int index = bundleNames.indexOf(bundleName);
		if (index >= 0) {
			bundleNames.remove(index);
			bundleFileNames.remove(index);
			bundles.remove(index);
		}
	}
}

// The table model for bundle groups

class GroupItemsTableModel extends AbstractTableModel {
	BundleGroup group;
	
	public GroupItemsTableModel(BundleGroup group) {
		this.group = group;
	}
	
	public int getColumnCount() { return 3; }
		    
	public int getRowCount() {
		return group.getItemCount();
	}
	
	public void setGroup(BundleGroup bg) {
		group = bg;
		fireTableChanged(new TableModelEvent(this));
	}
			
	public Object getValueAt(int row, int col) {
		BundleItem item = group.getBundleItem(row);
				
		String retStr = null;
				
		switch(col) {
		case 0:
			retStr = item.getKey();
			break;
		case 1:
			retStr = item.getTranslation();
			break;
		case 2:
			retStr = (item.getComment() == null ? "" : item.getComment());
			break;
		default:
			retStr = Resources.getTranslation("table_cell_error");
		}
				
		return retStr;
	}
	
	public String getColumnName(int col) {
		if (col == 0) return Resources.getTranslation("languagegroup_column_key");
		else if (col == 1) return Resources.getTranslation("languagegroup_column_translation");
		else if (col == 2) return Resources.getTranslation("languagegroup_column_comment");
		else return Resources.getTranslation("table_column_error");
	}
	
	public BundleItem getBundleItem(int row) {
		if (row >= group.getItemCount()) return null;
		return (BundleItem)group.getBundleItem(row);
	}
	
	public void update() {
		fireTableDataChanged();
	}
}

// The table model for untranslated Items

class UntranslatedItemsTableModel extends AbstractTableModel {
	Bundle bundle;
	
	public UntranslatedItemsTableModel(Bundle bundle) {
		this.bundle = bundle;
	}
	
	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
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

// The table model for search and replace Items

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

// A listener which checks a translation box to see if it changes, if it does, it marks the word as translated in a check box
class TranslationFocusListener implements FocusListener {
	String original;
	JCheckBox cbox;
	boolean selected;
	
	public TranslationFocusListener(String original, JCheckBox cbox) {
		this.original = original;
		this.cbox = cbox;
		selected = cbox.isSelected();
	}
	
	public void focusGained(FocusEvent ev) {}
	
	public void focusLost(FocusEvent ev) {
		JTextField field = (JTextField)ev.getSource();
		if (field.getText().equals(original)) {
			cbox.setSelected(selected);
			return;
		}
		cbox.setSelected(true);
	}
}


// Combo box model for display all groups of a bundle

class GroupComboBoxModel extends DefaultComboBoxModel {
	Bundle bundle;
	
	public GroupComboBoxModel (Bundle bundle) {
		this.bundle = bundle;
		setSelectedItem(bundle.getBundleGroup(0));
	}
	
	public int getSize() {
		return bundle.getGroupCount();
	}
	
	public Object getElementAt(int index) {
		return bundle.getBundleGroup(index);
	}
	
	public Object getSelectedItem() {
		return super.getSelectedItem();
		//return getElementAt(0);
	}
	
	public void update() {
		fireContentsChanged(this, 0, getSize()-1);
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

// The class used to display groups

class RBGroupPanel extends JPanel {
	RBManager rbm;
	Bundle bundle;
	RBManagerGUI listener;
	
	// Components
	JLabel      jLabelGroupTitle;
	JLabel      jLabelGroupNameTitle;
	JLabel      jLabelGroupCommentTitle;
	JLabel      jLabelGroupComment;
	JComboBox   jComboBoxGroup;
	JTable      jTableGroupTable;
	JScrollPane jScrollPaneGroupTable;
	Box         boxGroupInfo;
	Box         box1;
	Box         box2;
	Box         box3;
	Box         box4;
	Dimension   topDim = new Dimension();
	Dimension   leftDim = new Dimension();
	Dimension   rightDim = new Dimension();
	
	// Components - Manager
	JList       jListGroup;
	JButton     createItemButton;
	JButton     createGroupButton;
	JButton     editItemButton;
	JButton     editGroupButton;
	JButton     deleteItemButton;
	JButton     deleteGroupButton;
	JPanel      itemPanel;
	JPanel      groupPanel;
	
	public RBGroupPanel(RBManagerGUI gui) {
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
		if (jTableGroupTable.getSelectedRow() < 0) return;
		if (jTableGroupTable.getModel() instanceof GroupItemsTableModel) {
			int row = jTableGroupTable.getSelectedRow();
			GroupItemsTableModel model = (GroupItemsTableModel)jTableGroupTable.getModel();
			BundleItem item = model.getBundleItem(row);
			item.setTranslated(true);
			model.update();
		}
	}
	
	// Removes the selected resource from the resource file
	private void deleteSelectedResource() {
		if (bundle == null) return;
		if (jTableGroupTable.getSelectedRow() < 0) return;
		if (jTableGroupTable.getModel() instanceof GroupItemsTableModel) {
			int row = jTableGroupTable.getSelectedRow();
			GroupItemsTableModel model = (GroupItemsTableModel)jTableGroupTable.getModel();
			BundleItem item = model.getBundleItem(row);
			if (item.getParentGroup() != null && item.getParentGroup().getParentBundle() != null) {
				Bundle bundle = item.getParentGroup().getParentBundle();
				bundle.removeItem(item.getKey());
			}
			model.update();
		}
	}
		
	public void initComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelGroupTitle          = new JLabel(bundle.name);
			jComboBoxGroup            = new JComboBox(new GroupComboBoxModel(bundle));//bundle.getGroupsAsVector());
			
			jTableGroupTable          = new JTable(new GroupItemsTableModel((BundleGroup)jComboBoxGroup.getSelectedItem()));
			jScrollPaneGroupTable     = new JScrollPane(jTableGroupTable);
			jLabelGroupNameTitle      = new JLabel(Resources.getTranslation("basegroup_group_name"));
			jLabelGroupCommentTitle   = new JLabel(Resources.getTranslation("basegroup_group_comment"));
			jLabelGroupComment        = new JLabel(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			boxGroupInfo              = new Box(BoxLayout.Y_AXIS);
			box1                      = new Box(BoxLayout.X_AXIS);
			box2                      = new Box(BoxLayout.X_AXIS);
			box3                      = new Box(BoxLayout.X_AXIS);
			
			// Lower panel components
			JPanel  lowerPanel = new JPanel();
			JButton deleteButton = new JButton(Resources.getTranslation("button_delete_resource"));
			JButton translateButton = new JButton(Resources.getTranslation("button_mark_translated"));
			
			deleteButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			translateButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_mark_translated_trigger")));
			lowerPanel.setBorder(BorderFactory.createTitledBorder(Resources.getTranslation("languageuntrans_selected_resources_options")));
			lowerPanel.setLayout(new GridLayout(1,2));
			
			topDim   = new Dimension(getSize().width, 35);
			leftDim  = new Dimension(150,25);
			rightDim = new Dimension(getSize().width - leftDim.width, leftDim.height);
			
			jLabelGroupNameTitle.setPreferredSize(leftDim);
			jLabelGroupNameTitle.setHorizontalAlignment(SwingConstants.LEFT);
			jLabelGroupCommentTitle.setPreferredSize(leftDim);
			jComboBoxGroup.setPreferredSize(rightDim);
			jLabelGroupTitle.setPreferredSize(topDim);
			jLabelGroupComment.setPreferredSize(rightDim);
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTableGroupTable.addMouseListener(listener);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jListGroup));
			
			jLabelGroupTitle.setFont(new Font("SansSerif",Font.PLAIN,18));
			
			// Add action listeners
			deleteButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					deleteSelectedResource();
				}
			});
			
			translateButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					markSelectedResourceAsTranslated();
				}
			});
			
			// Update the display
			setLayout(new BorderLayout());
			removeAll();
			lowerPanel.add(deleteButton);
			lowerPanel.add(translateButton);
			box1.add(Box.createHorizontalGlue());
			box1.add(jLabelGroupTitle);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			boxGroupInfo.add(box1);
			boxGroupInfo.add(box2);
			boxGroupInfo.add(box3);
			boxGroupInfo.add(jScrollPaneGroupTable);
			add(boxGroupInfo, BorderLayout.CENTER);
			add(lowerPanel, BorderLayout.SOUTH);
		} else if (rbm != null) {
			Bundle mainBundle = (Bundle)rbm.bundles.firstElement();
			jLabelGroupTitle          = new JLabel(rbm.getBaseClass() + " - " + Resources.getTranslation("groups"));
			jComboBoxGroup            = new JComboBox(new GroupComboBoxModel(mainBundle));//mainBundle.getGroupsAsVector());
			
			jListGroup                = new JList(new GroupItemsListModel((BundleGroup)jComboBoxGroup.getSelectedItem()));
			jScrollPaneGroupTable     = new JScrollPane(jListGroup);
			jLabelGroupNameTitle      = new JLabel(Resources.getTranslation("basegroup_group_name"));
			jLabelGroupCommentTitle   = new JLabel(Resources.getTranslation("basegroup_group_comment"));
			try {
				jLabelGroupComment    = new JLabel(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			} catch (NullPointerException npe) {
				jLabelGroupComment    = new JLabel("");
			}
			
			boxGroupInfo              = new Box(BoxLayout.Y_AXIS);
			box1                      = new Box(BoxLayout.X_AXIS);
			box2                      = new Box(BoxLayout.X_AXIS);
			box3                      = new Box(BoxLayout.X_AXIS);
			box4                      = new Box(BoxLayout.Y_AXIS);
			
			createItemButton          = new JButton(Resources.getTranslation("button_create_resource"));
			createGroupButton         = new JButton(Resources.getTranslation("button_create_group"));
			deleteItemButton          = new JButton(Resources.getTranslation("button_delete_resource"));
			deleteGroupButton         = new JButton(Resources.getTranslation("button_delete_group"));
			editItemButton            = new JButton(Resources.getTranslation("button_edit_resource"));
			editGroupButton           = new JButton(Resources.getTranslation("button_edit_group"));
			
			itemPanel                 = new JPanel();
			groupPanel                = new JPanel();
			
			itemPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																  Resources.getTranslation("basegroup_item_options")));
			groupPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
																  Resources.getTranslation("basegroup_group_options")));
			itemPanel.setLayout(new GridLayout(1,3));
			groupPanel.setLayout(new GridLayout(1,3));
			itemPanel.setMaximumSize(new Dimension(20000,50));
			groupPanel.setMaximumSize(new Dimension(20000,50));
			
			topDim   = new Dimension(getSize().width, 35);
			leftDim  = new Dimension(150,25);
			rightDim = new Dimension(getSize().width - leftDim.width, leftDim.height);
			
			createItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_resource_trigger")));
			editItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_edit_resource_trigger")));
			deleteItemButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_delete_resource_trigger")));
			createGroupButton.setMnemonic(RBManagerMenuBar.getKeyEventKey(Resources.getTranslation("button_create_group_trigger")));
			
			jLabelGroupNameTitle.setPreferredSize(leftDim);
			jLabelGroupCommentTitle.setPreferredSize(leftDim);
			jComboBoxGroup.setPreferredSize(rightDim);
			jLabelGroupTitle.setPreferredSize(topDim);
			jLabelGroupComment.setPreferredSize(rightDim);
			
			jListGroup.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jListGroup));
			
			jLabelGroupTitle.setFont(new Font("SansSerif",Font.PLAIN,18));

			// Add the listeners
			jListGroup.addMouseListener(new MouseListener() {
				public void mousePressed(MouseEvent ev) { }
				public void mouseClicked(MouseEvent ev) { 
					if(ev.getClickCount() == 2 && ev.getSource() instanceof JList) {
						// A double click means they want to edit a bundle item
						RBGroupPanel panel = (RBGroupPanel)
							((JList)ev.getSource()).getParent().getParent().getParent().getParent();
						
						if (((JList)ev.getSource()).getSelectedValue() != null)
							new BundleItemCreationDialog((BundleItem)((JList)ev.getSource()).getSelectedValue(),
								panel.listener.rbm, panel.listener, Resources.getTranslation("dialog_title_edit_item"), true);
					}
				}
				public void mouseReleased(MouseEvent ev) { }
				public void mouseEntered(MouseEvent ev) { }
				public void mouseExited(MouseEvent ev) { }
			});
			
			createItemButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					new BundleItemCreationDialog(((BundleGroup)panel.jComboBoxGroup.getSelectedItem()).getName(),
												 panel.listener.rbm, panel.listener,
												 Resources.getTranslation("dialog_title_new_item"), true);
					panel.updateComponents();
				}
			});
			createGroupButton.addActionListener(listener);
			editItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					if (panel.jListGroup.getSelectedValue() != null)
						new BundleItemCreationDialog((BundleItem)panel.jListGroup.getSelectedValue(),
							panel.listener.rbm, panel.listener, Resources.getTranslation("dialog_title_edit_item"), true);
					panel.updateComponents();
				}
			});
			editGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					new BundleGroupEditDialog((BundleGroup)panel.jComboBoxGroup.getSelectedItem(),
											  panel.listener, Resources.getTranslation("dialog_title_edit_group"), true);
				panel.updateComponents();
				}
			});
			deleteGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)
						((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					int response = JOptionPane.showConfirmDialog(panel.listener,
						Resources.getTranslation("dialog_warning_delete_group"),
						Resources.getTranslation("dialog_title_delete_group"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						// Delete the group 
						int index = panel.jComboBoxGroup.getSelectedIndex();
						BundleGroup group = (BundleGroup)panel.jComboBoxGroup.getSelectedItem();
						if (group.getName().equals("Ungrouped Items")) return;
						if (index < panel.jComboBoxGroup.getItemCount()-1) panel.jComboBoxGroup.setSelectedIndex(index+1);
						else panel.jComboBoxGroup.setSelectedIndex(index-1);
						panel.rbm.deleteGroup(group.getName());
					}
					panel.updateComponents();
				}
			});
			
			deleteItemButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ev) {
					RBGroupPanel panel = (RBGroupPanel)((JButton)ev.getSource()).getParent().getParent().getParent().getParent();
					int response = JOptionPane.showConfirmDialog(panel.listener,
						Resources.getTranslation("dialog_warning_delete_item"),
						Resources.getTranslation("dialog_title_delete_item"), JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE);
					if (response == JOptionPane.OK_OPTION) {
						Object o = panel.jListGroup.getSelectedValue();
						if (o != null) {
							BundleItem item = (BundleItem) o;
							handleDeleteItem(item.getKey());
							//panel.rbm.deleteItem(item.getKey());
						}
					}
					panel.updateComponents();
				}
			});
			
			// Update the display
			setLayout(new BorderLayout());
			removeAll();
			itemPanel.add(createItemButton, BorderLayout.WEST);
			itemPanel.add(editItemButton, BorderLayout.CENTER);
			itemPanel.add(deleteItemButton, BorderLayout.EAST);
			groupPanel.add(createGroupButton, BorderLayout.WEST);
			groupPanel.add(editGroupButton, BorderLayout.CENTER);
			groupPanel.add(deleteGroupButton, BorderLayout.EAST);
			box1.add(Box.createHorizontalGlue());
			box1.add(jLabelGroupTitle);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			box4.add(Box.createVerticalStrut(5));
			box4.add(groupPanel);
			box4.add(Box.createVerticalStrut(10));
			box4.add(itemPanel);
			box4.add(Box.createVerticalStrut(5));
			boxGroupInfo.add(box1);
			boxGroupInfo.add(box2);
			boxGroupInfo.add(box3);
			boxGroupInfo.add(jScrollPaneGroupTable);
			boxGroupInfo.add(box4);
			add(boxGroupInfo, BorderLayout.CENTER);
		} else {
			removeAll();
		}
	}
	
	public void updateComponents() {
		// Initialize components
		if (bundle != null) {
			jLabelGroupTitle.setText(bundle.name);
			
			topDim.width = getSize().width;
			rightDim.width = getSize().width - leftDim.width;
			
			box2.removeAll();
			box3.removeAll();
			boxGroupInfo.remove(jScrollPaneGroupTable);
			
			String selName = null;
			try {
				selName = ((BundleGroup)jComboBoxGroup.getSelectedItem()).getName();
			} catch (Exception e) {}
			jComboBoxGroup = new JComboBox(new GroupComboBoxModel(bundle));//bundle.getGroupsAsVector());
			for (int i = 0; i < jComboBoxGroup.getItemCount(); i++) {
				BundleGroup bg = (BundleGroup)jComboBoxGroup.getItemAt(i);
				if (bg.getName().equals(selName)) jComboBoxGroup.setSelectedIndex(i);
			}
			
			((GroupItemsTableModel)jTableGroupTable.getModel()).setGroup((BundleGroup)jComboBoxGroup.getSelectedItem());
			jScrollPaneGroupTable = new JScrollPane(jTableGroupTable);
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			jTableGroupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jComboBoxGroup.addActionListener(new GroupComboActionListener(jTableGroupTable));
			
			// Update the group comment
			jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			
			// Update the display
			jComboBoxGroup.setPreferredSize(rightDim);
			box2.add(Box.createHorizontalGlue());
			box2.add(jLabelGroupNameTitle);
			box2.add(jComboBoxGroup);
			box3.add(Box.createHorizontalGlue());
			box3.add(jLabelGroupCommentTitle);
			box3.add(jLabelGroupComment);
			boxGroupInfo.add(jScrollPaneGroupTable);
			
		} else if (rbm != null) {
			
			// Update the list of groups
			try {((GroupComboBoxModel)jComboBoxGroup.getModel()).update();}
			catch (Exception e) {}
			// Update the group comment
			if ((BundleGroup)jComboBoxGroup.getSelectedItem() != null)
				jLabelGroupComment.setText(((BundleGroup)jComboBoxGroup.getSelectedItem()).getComment());
			else jLabelGroupComment.setText("");
			// Update the list of resources
			ListModel lmodel = jListGroup.getModel();
			if (lmodel instanceof GroupItemsListModel)
				((GroupItemsListModel)lmodel).update();
			else {
				GroupItemsListModel newModel = new GroupItemsListModel((BundleGroup)jComboBoxGroup.getSelectedItem());
				RBManagerGUI.debugMsg("List Model not as anticipated: " + lmodel.getClass().getName());
				jListGroup.setModel(newModel);
				newModel.update();
			}
			/*
			try {GroupItemsListModel mod = (GroupItemsListModel) lmodel; }
			catch (Exception e) {
				e.printStackTrace(System.err);
			}
			*/
			if (lmodel instanceof AbstractListModel) {
				RBManagerGUI.debugMsg("List Model is an AbstractListModel");
			}  else {
				RBManagerGUI.debugMsg("List Model is not an AbstractListModel");
			}
		} else {
			removeAll();
		}
		//validate();
	}
	
	private void handleDeleteItem(String key) {
		if (rbm != null) rbm.deleteItem(key);
	}
}

// The class used to display untranslated items

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
			for (int i=0; i < rbm.bundles.size(); i++) {
				Bundle bundle = (Bundle)rbm.bundles.elementAt(i);
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
			for (int i=0; i < rbm.bundles.size(); i++) {
				Bundle bundle = (Bundle)rbm.bundles.elementAt(i);
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

// The class used to display statistics

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
			
			updateButton.setEnabled(false);
			
			// Update the display
			if (mainBox != null){
				mainBox.removeAll();
			} else {
				mainBox = new Box(BoxLayout.Y_AXIS);
			}
			if (dupBox != null) dupBox.removeAll();
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
			
		} else if (rbm != null) {
			
		} else {
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

// The class used to display untranslated items

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
		Enumeration enum = bundle.allItems.keys();
		while (enum.hasMoreElements()) {
			String key = (String)enum.nextElement();
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

class RBProjectItemPanel extends JPanel implements ActionListener {
	RBManagerGUI gui;
	
	// Visual Components
	Box mainBox;
	JTextField itemFields[];
	JLabel     itemLabels[];
	JButton    commitButtons[];
	JButton    commitButton;
	JLabel     titleLabel;
	JLabel     keyLabel;
	JLabel     commentLabel;
	
	public RBProjectItemPanel(RBManagerGUI gui) {
		super();
		this.gui = gui;
		initComponents();
	}
	
	public void actionPerformed(ActionEvent ev) {
		JButton button = (JButton)ev.getSource();
		String buttonName = button.getName();
		if (buttonName == null) {
			// Save all components
			RBManager bundle = gui.getSelectedProjectBundle();
			Vector bundles = bundle.getBundles();
			for (int i=0; i < itemFields.length; i++) {
				String encoding = commitButtons[i].getName();
				String translation = itemFields[i].getText();
				String key = itemFields[i].getName();
				for (int j=0; j < bundles.size(); j++) {
					Bundle rbundle = (Bundle)bundles.elementAt(j);
					if (rbundle.encoding.equals(encoding)) {
						BundleItem item = rbundle.getBundleItem(key);
						if (item != null) item.setTranslation(translation);
						break;
					}
				}
			}
			gui.saveResources(bundle);
		} else {
			// Save a particular encoding
			String encoding = buttonName;
			RBManager bundle = gui.getSelectedProjectBundle();
			int index = -1;
			for (int i=0; i < commitButtons.length; i++)
				if (commitButtons[i] == button) { index = i; break; }
			String translation = itemFields[index].getText();
			String key = itemFields[index].getName();
			Vector bundles = bundle.getBundles();
			for (int i=0; i < bundles.size(); i++) {
				Bundle rbundle = (Bundle)bundles.elementAt(i);
				if (rbundle.encoding.equals(encoding)) {
					BundleItem item = rbundle.getBundleItem(key);
					if (item != null) {item.setTranslation(translation); System.out.println("Set translation to : " + translation); }
					else System.out.println("Item was null");
					break;
				} else System.out.println("Compared " + rbundle.encoding + " with " + encoding);
			}
			gui.saveResources(bundle, encoding);
		}
		updateComponents();
	}
	
	private void initComponents() {
		setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new GridLayout(2,1));
		titleLabel = new JLabel(Resources.getTranslation("project_panel_default_title"), SwingConstants.CENTER);
		titleLabel.setFont(new Font("serif",Font.BOLD,18));
		JPanel commentPanel = new JPanel(new GridLayout(2,1));
		JLabel commentLabel2 = new JLabel(Resources.getTranslation("project_panel_comment"), SwingConstants.LEFT);
		commentLabel = new JLabel(Resources.getTranslation("project_panel_comment_none"), SwingConstants.LEFT);
		commentPanel.add(commentLabel2);
		commentPanel.add(commentLabel);
		topPanel.add(titleLabel);
		topPanel.add(commentPanel);
		JPanel centerPanel = new JPanel(new BorderLayout());
		mainBox = new Box(BoxLayout.Y_AXIS);
		JScrollPane scrollPane = new JScrollPane(mainBox,
												 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
												 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		centerPanel.add(scrollPane, BorderLayout.NORTH);
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		commitButton = new JButton(Resources.getTranslation("project_panel_commit_button_all"));
		commitButton.addActionListener(this);
		botPanel.add(commitButton);
		add(topPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(botPanel, BorderLayout.SOUTH);
		
		updateComponents();
	}
	
	public void updateComponents() {
		BundleItem item = gui.getSelectedProjectBundleItem();
		
		if (item == null) {
			commentLabel.setText(Resources.getTranslation("project_panel_comment_none"));
			titleLabel.setText(Resources.getTranslation("project_panel_default_title"));
			itemFields = null;
			itemLabels = null;
			commitButtons = null;
			commitButton.setEnabled(false);
		} else {
			String comment = item.getComment();
			String key = item.getKey();
			commentLabel.setText(comment);
			titleLabel.setText(Resources.getTranslation("project_panel_title", key));
			
			RBManager manager = gui.getSelectedProjectBundle();
			Vector bundles = manager.getBundles();
			itemFields = new JTextField[bundles.size()];
			itemLabels = new JLabel[bundles.size()];
			commitButtons = new JButton[bundles.size()];
			for (int i=0; i < bundles.size(); i++) {
				Bundle bundle = (Bundle)bundles.elementAt(i);
				BundleItem bundleItem = bundle.getBundleItem(key);
				boolean translated = bundleItem.isTranslated();
				JLabel encodingLabel = new JLabel(Resources.getTranslation("project_panel_bundle", bundle.toString()),
												  SwingConstants.LEFT);
				if (!translated) encodingLabel.setText(Resources.getTranslation("project_panel_bundle_untranslated",
																				bundle.toString()));
				String fieldText = (bundleItem == null ? Resources.getTranslation("project_panel_item_inherits") :
														 bundleItem.getTranslation());
				JTextField itemField = new JTextField(fieldText);
				itemField.setMaximumSize(new Dimension(this.getSize().width-150, 200));
				itemField.setName(key);
				JButton commitItemButton = new JButton(Resources.getTranslation("project_panel_commit_button"));
				commitItemButton.addActionListener(this);
				commitItemButton.setName(bundle.encoding);
				itemFields[i] = itemField;
				itemLabels[i] = encodingLabel;
				commitButtons[i] = commitItemButton;
			}
			commitButton.setEnabled(true);
		}
		
		mainBox.removeAll();
		if (itemFields != null) {
			for (int i=0; i < itemFields.length; i++) {
				JPanel bundlePanel = new JPanel(new BorderLayout());
				bundlePanel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
				bundlePanel.add(itemLabels[i], BorderLayout.NORTH);
				bundlePanel.add(itemFields[i], BorderLayout.CENTER);
				bundlePanel.add(commitButtons[i], BorderLayout.EAST);
				mainBox.add(bundlePanel);
			}
		}
		
		revalidate();
	}
}

// The main menu bar for the main frame

class RBManagerMenuBar extends JMenuBar {
	RBManagerGUI             listener;
	
	JMenu        jMenuFile = new JMenu();                                     // Menu -> File
	JMenuItem    jMenuFileNewResourceBundle = new JMenuItem();                
	JMenuItem    jMenuFileOpenResourceBundle = new JMenuItem();
	JMenuItem    jMenuFileSaveResourceBundle = new JMenuItem();
	JMenuItem    jMenuFileSaveResourceBundleAs = new JMenuItem();
	JMenu        jMenuFileImportResourceBundle = new JMenu();                 // Menu -> File -> Import
	JMenuItem    jMenuFileImportJava = new JMenuItem();
	JMenuItem    jMenuFileImportProperties = new JMenuItem();
	JMenuItem    jMenuFileImportTMX = new JMenuItem();
	JMenu        jMenuFileExportResourceBundle = new JMenu();                 // Menu -> File -> Export
	JMenuItem    jMenuFileExportJava = new JMenuItem();
	JMenuItem    jMenuFileExportICU = new JMenuItem();
	JMenuItem    jMenuFileExportProperties = new JMenuItem();
	JMenuItem    jMenuFileExportTMX = new JMenuItem();
	JMenuItem    jMenuFileExit = new JMenuItem();
	JMenu        jMenuEdit = new JMenu();                                     // Menu -> Edit
	JMenuItem    jMenuEditCut = new JMenuItem();
	JMenuItem    jMenuEditCopy = new JMenuItem();
	JMenuItem    jMenuEditPaste = new JMenuItem();
	JMenuItem    jMenuEditDelete = new JMenuItem();
	JMenu        jMenuOptions = new JMenu();                                  // Menu -> Options
	JMenuItem    jMenuOptionsAddNewEntry = new JMenuItem();
	JMenuItem    jMenuOptionsAddNewGroup = new JMenuItem();
	JMenuItem    jMenuOptionsAddNewResourceFile = new JMenuItem();
	//JMenuItem    jMenuOptionsProjectViewer = new JMenuItem();
	JMenuItem    jMenuOptionsPreferences = new JMenuItem();
	JMenu        jMenuView = new JMenu();                                     // Menu -> View
	JMenuItem    jMenuViewViewStatistics = new JMenuItem();
	JMenu        jMenuHelp = new JMenu();                                     // Menu -> Help
	JMenuItem    jMenuHelpAboutResourceBundleManager = new JMenuItem();
	
	void updateLocale() {
		//FILE
		jMenuFile.setText(Resources.getTranslation("menu_file"));
		jMenuFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_trigger")));
		jMenuFileNewResourceBundle.setText(Resources.getTranslation("menu_file_new"));
		jMenuFileNewResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_new_trigger")));
		jMenuFileOpenResourceBundle.setText(Resources.getTranslation("menu_file_open"));
		jMenuFileOpenResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_open_trigger")));
		jMenuFileSaveResourceBundle.setText(Resources.getTranslation("menu_file_save"));
		jMenuFileSaveResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_save_trigger")));
		jMenuFileSaveResourceBundleAs.setText(Resources.getTranslation("menu_file_saveas"));
		jMenuFileSaveResourceBundleAs.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_saveas_trigger")));
		jMenuFileImportResourceBundle.setText(Resources.getTranslation("menu_file_import"));
		jMenuFileImportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_trigger")));
		jMenuFileImportJava.setText(Resources.getTranslation("menu_file_import_java"));
		jMenuFileImportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_java_trigger")));
		jMenuFileImportProperties.setText(Resources.getTranslation("menu_file_import_properties"));
		jMenuFileImportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_properties_trigger")));
		jMenuFileImportTMX.setText(Resources.getTranslation("menu_file_import_TMX"));
		jMenuFileImportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_TMX_trigger")));
		jMenuFileExportResourceBundle.setText(Resources.getTranslation("menu_file_export"));
		jMenuFileExportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_trigger")));
		jMenuFileExportJava.setText(Resources.getTranslation("menu_file_export_java"));
		jMenuFileExportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_java_trigger")));
		jMenuFileExportICU.setText(Resources.getTranslation("menu_file_export_ICU"));
		jMenuFileExportICU.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_ICU_trigger")));
		jMenuFileExportProperties.setText(Resources.getTranslation("menu_file_export_properties"));
		jMenuFileExportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_properties_trigger")));
		jMenuFileExportTMX.setText(Resources.getTranslation("menu_file_export_TMX"));
		jMenuFileExportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_TMX_trigger")));
		jMenuFileExit.setText(Resources.getTranslation("menu_file_quit"));
		jMenuFileExit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_quit_trigger")));
		//EDIT
		jMenuEdit.setText(Resources.getTranslation("menu_edit"));
		jMenuEdit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_trigger")));
		jMenuEditCut.setText(Resources.getTranslation("menu_edit_cut"));
		jMenuEditCut.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_cut_trigger")));
		jMenuEditCopy.setText(Resources.getTranslation("menu_edit_copy"));
		jMenuEditCopy.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_copy_trigger")));
		jMenuEditPaste.setText(Resources.getTranslation("menu_edit_paste"));
		jMenuEditPaste.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_paste_trigger")));
		jMenuEditDelete.setText(Resources.getTranslation("menu_edit_delete"));
		jMenuEditDelete.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_delete_trigger")));
		//OPTIONS
		jMenuOptions.setText(Resources.getTranslation("menu_options"));
		jMenuOptions.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuOptionsAddNewEntry.setText(Resources.getTranslation("menu_options_addentry"));
		jMenuOptionsAddNewEntry.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addentry_trigger")));
		jMenuOptionsAddNewGroup.setText(Resources.getTranslation("menu_options_addgroup"));
		jMenuOptionsAddNewGroup.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addgroup_trigger")));
		jMenuOptionsAddNewResourceFile.setText(Resources.getTranslation("menu_options_addfile"));
		jMenuOptionsAddNewResourceFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addfile_trigger")));
		//jMenuOptionsProjectViewer.setText(Resources.getTranslation("menu_options_project_viewer"));
		//jMenuOptionsProjectViewer.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_project_viewer_trigger")));
		jMenuOptionsPreferences.setText(Resources.getTranslation("menu_options_preferences"));
		jMenuOptionsPreferences.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_preferences_trigger")));
		//VIEW
		jMenuView.setText(Resources.getTranslation("menu_view"));
		jMenuView.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_trigger")));
		jMenuViewViewStatistics.setText(Resources.getTranslation("menu_view_statistics"));
		jMenuViewViewStatistics.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_statistics_trigger")));
		//HELP
		jMenuHelp.setText(Resources.getTranslation("menu_help"));
		jMenuHelp.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuHelpAboutResourceBundleManager.setText(Resources.getTranslation("menu_help_about"));
		jMenuHelpAboutResourceBundleManager.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_about_trigger")));
	}
	
	public RBManagerMenuBar(RBManagerGUI gui) {
		super();	
		
        boolean xmlAvailable;
		try {
            Class cl = Class.forName("org.apache.xerces.parsers.DOMParser");
            xmlAvailable = true;
        } catch (ClassNotFoundException e) {
            xmlAvailable = false;
        }
		listener = gui;
		
		// Add the menus to the menu bar 
		setVisible(true);
		add(jMenuFile);
		//add(jMenuEdit);
		add(jMenuOptions);
		//add(jMenuView);
		add(jMenuHelp);

		// Add File Menu Items to the File Menu
		jMenuFile.setVisible(true);
		jMenuFile.setText(Resources.getTranslation("menu_file"));
		jMenuFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_trigger")));
		jMenuFile.add(jMenuFileNewResourceBundle);
		jMenuFile.add(jMenuFileOpenResourceBundle);
		jMenuFile.add(jMenuFileSaveResourceBundle);
		jMenuFile.add(jMenuFileSaveResourceBundleAs);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileImportResourceBundle);
		jMenuFile.add(jMenuFileExportResourceBundle);
		jMenuFile.addSeparator();
		// Add the recent files to the file menu
		Vector recentFiles = Preferences.getRecentFilesPreferences();
		if (recentFiles.size() > 0) {
			for (int i=0; i < recentFiles.size(); i+=2) {
				String name = (String)recentFiles.elementAt(i);
				String location = (String)recentFiles.elementAt(i+1);
				JMenuItem recentMenuItem = new JMenuItem();
				recentMenuItem.setVisible(true);
				recentMenuItem.setText(name);
				recentMenuItem.setName("__" + location.trim());
				recentMenuItem.addActionListener(listener);
				jMenuFile.add(recentMenuItem);
			}
			jMenuFile.addSeparator();
		}
		jMenuFile.add(jMenuFileExit);
		
		//jMenuFileImportResourceBundle.add(jMenuFileImportJava);
		jMenuFileImportResourceBundle.add(jMenuFileImportProperties);
        jMenuFileImportTMX.setEnabled(xmlAvailable);
		jMenuFileImportResourceBundle.add(jMenuFileImportTMX);
		jMenuFileExportResourceBundle.add(jMenuFileExportJava);
		jMenuFileExportResourceBundle.add(jMenuFileExportICU);
		jMenuFileExportResourceBundle.add(jMenuFileExportProperties);
        jMenuFileExportTMX.setEnabled(xmlAvailable);
		jMenuFileExportResourceBundle.add(jMenuFileExportTMX);

		jMenuFileNewResourceBundle.setVisible(true);
		jMenuFileNewResourceBundle.setText(Resources.getTranslation("menu_file_new"));
		jMenuFileNewResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_new_trigger")));
		jMenuFileNewResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		jMenuFileNewResourceBundle.addActionListener(listener);
		
		jMenuFileOpenResourceBundle.setVisible(true);
		jMenuFileOpenResourceBundle.setText(Resources.getTranslation("menu_file_open"));
		jMenuFileOpenResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_open_trigger")));
		jMenuFileOpenResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		jMenuFileOpenResourceBundle.addActionListener(listener);

		jMenuFileSaveResourceBundle.setVisible(true);
		jMenuFileSaveResourceBundle.setText(Resources.getTranslation("menu_file_save"));
		jMenuFileSaveResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_save_trigger")));
		jMenuFileSaveResourceBundle.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		jMenuFileSaveResourceBundle.addActionListener(listener);

		jMenuFileSaveResourceBundleAs.setVisible(true);
		jMenuFileSaveResourceBundleAs.setText(Resources.getTranslation("menu_file_saveas"));
		jMenuFileSaveResourceBundleAs.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_saveas_trigger")));
		jMenuFileSaveResourceBundleAs.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		jMenuFileSaveResourceBundleAs.addActionListener(listener);
		
		jMenuFileImportResourceBundle.setVisible(true);
		jMenuFileImportResourceBundle.setText(Resources.getTranslation("menu_file_import"));
		jMenuFileImportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_trigger")));
		jMenuFileImportResourceBundle.addActionListener(listener);
		
		jMenuFileImportJava.setVisible(true);
		jMenuFileImportJava.setText(Resources.getTranslation("menu_file_import_java"));
		jMenuFileImportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_java_trigger")));
		jMenuFileImportJava.addActionListener(listener);
		
		jMenuFileImportProperties.setVisible(true);
		jMenuFileImportProperties.setText(Resources.getTranslation("menu_file_import_properties"));
		jMenuFileImportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_properties_trigger")));
		jMenuFileImportProperties.addActionListener(listener);
		
		jMenuFileImportTMX.setVisible(true);
		jMenuFileImportTMX.setText(Resources.getTranslation("menu_file_import_TMX"));
		jMenuFileImportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_import_TMX_trigger")));
		jMenuFileImportTMX.addActionListener(listener);
		
		jMenuFileExportResourceBundle.setVisible(true);
		jMenuFileExportResourceBundle.setText(Resources.getTranslation("menu_file_export"));
		jMenuFileExportResourceBundle.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_trigger")));
		jMenuFileExportResourceBundle.addActionListener(listener);
		
		jMenuFileExportJava.setVisible(true);
		jMenuFileExportJava.setText(Resources.getTranslation("menu_file_export_java"));
		jMenuFileExportJava.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_java_trigger")));
		jMenuFileExportJava.addActionListener(listener);
		
		jMenuFileExportICU.setVisible(true);
		jMenuFileExportICU.setText(Resources.getTranslation("menu_file_export_ICU"));
		jMenuFileExportICU.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_ICU_trigger")));
		jMenuFileExportICU.addActionListener(listener);

		jMenuFileExportProperties.setVisible(true);
		jMenuFileExportProperties.setText(Resources.getTranslation("menu_file_export_properties"));
		jMenuFileExportProperties.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_properties_trigger")));
		jMenuFileExportProperties.addActionListener(listener);
		
		jMenuFileExportTMX.setVisible(true);
		jMenuFileExportTMX.setText(Resources.getTranslation("menu_file_export_TMX"));
		jMenuFileExportTMX.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_export_TMX_trigger")));
		jMenuFileExportTMX.addActionListener(listener);
		
		jMenuFileExit.setVisible(true);
		jMenuFileExit.setText(Resources.getTranslation("menu_file_quit"));
		jMenuFileExit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_file_quit_trigger")));
		jMenuFileExit.setAccelerator(KeyStroke.getKeyStroke(
                  KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		jMenuFileExit.addActionListener(listener);

		// Add Edit Menu Items to the Edit Menu
		jMenuEdit.setVisible(true);
		jMenuEdit.setText(Resources.getTranslation("menu_edit"));
		jMenuEdit.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_trigger")));
		jMenuEdit.add(jMenuEditCut);
		jMenuEdit.add(jMenuEditCopy);
		jMenuEdit.add(jMenuEditPaste);
		jMenuEdit.add(jMenuEditDelete);

		jMenuEditCut.setVisible(true);
		jMenuEditCut.setText(Resources.getTranslation("menu_edit_cut"));
		jMenuEditCut.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_cut_trigger")));
		jMenuEditCut.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.CTRL_MASK));

		jMenuEditCopy.setVisible(true);
		jMenuEditCopy.setText(Resources.getTranslation("menu_edit_copy"));
		jMenuEditCopy.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_copy_trigger")));
		jMenuEditCopy.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_C, ActionEvent.CTRL_MASK));

		jMenuEditPaste.setVisible(true);
		jMenuEditPaste.setText(Resources.getTranslation("menu_edit_paste"));
		jMenuEditPaste.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_paste_trigger")));
		jMenuEditPaste.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		
		jMenuEditDelete.setVisible(true);
		jMenuEditDelete.setText(Resources.getTranslation("menu_edit_delete"));
		jMenuEditDelete.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_edit_delete_trigger")));

		// Add Options Menu Items to the Options Menu
		jMenuOptions.setVisible(true);
		jMenuOptions.setText(Resources.getTranslation("menu_options"));
		jMenuOptions.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_trigger")));
		jMenuOptions.add(jMenuOptionsAddNewEntry);
		jMenuOptions.add(jMenuOptionsAddNewGroup);
		jMenuOptions.add(jMenuOptionsAddNewResourceFile);
		//jMenuOptions.addSeparator();
		//jMenuOptions.add(jMenuOptionsProjectViewer);
		jMenuOptions.addSeparator();
		jMenuOptions.add(jMenuOptionsPreferences);

		jMenuOptionsAddNewEntry.setVisible(true);
		jMenuOptionsAddNewEntry.setText(Resources.getTranslation("menu_options_addentry"));
		jMenuOptionsAddNewEntry.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addentry_trigger")));
		jMenuOptionsAddNewEntry.addActionListener(listener);
		
		jMenuOptionsAddNewGroup.setVisible(true);
		jMenuOptionsAddNewGroup.setText(Resources.getTranslation("menu_options_addgroup"));
		jMenuOptionsAddNewGroup.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addgroup_trigger")));
		jMenuOptionsAddNewGroup.addActionListener(listener);

		jMenuOptionsAddNewResourceFile.setVisible(true);
		jMenuOptionsAddNewResourceFile.setText(Resources.getTranslation("menu_options_addfile"));
		jMenuOptionsAddNewResourceFile.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_addfile_trigger")));
		jMenuOptionsAddNewResourceFile.addActionListener(listener);
		
		/*
		jMenuOptionsProjectViewer.setVisible(true);
		jMenuOptionsProjectViewer.setText(Resources.getTranslation("menu_options_project_viewer"));
		jMenuOptionsProjectViewer.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_project_viewer_trigger")));
		jMenuOptionsProjectViewer.addActionListener(listener);
		*/
		
		jMenuOptionsPreferences.setVisible(true);
		jMenuOptionsPreferences.setText(Resources.getTranslation("menu_options_preferences"));
		jMenuOptionsPreferences.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_options_preferences_trigger")));
		jMenuOptionsPreferences.addActionListener(listener);

		// Add View Menu Items to the View Menu
		jMenuView.setVisible(true);
		jMenuView.setText(Resources.getTranslation("menu_view"));
		jMenuView.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_trigger")));
		jMenuView.add(jMenuViewViewStatistics);

		jMenuViewViewStatistics.setVisible(true);
		jMenuViewViewStatistics.setText(Resources.getTranslation("menu_view_statistics"));
		jMenuViewViewStatistics.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_view_statistics_trigger")));

		// Add Help Menu Items to the Help Menu
		jMenuHelp.setVisible(true);
		jMenuHelp.setText(Resources.getTranslation("menu_help"));
		jMenuHelp.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_trigger")));
		jMenuHelp.add(jMenuHelpAboutResourceBundleManager);

		jMenuHelpAboutResourceBundleManager.setVisible(true);
		jMenuHelpAboutResourceBundleManager.setText(Resources.getTranslation("menu_help_about"));
		jMenuHelpAboutResourceBundleManager.setMnemonic(getKeyEventKey(Resources.getTranslation("menu_help_about_trigger")));
		jMenuHelpAboutResourceBundleManager.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_H, ActionEvent.CTRL_MASK));
		jMenuHelpAboutResourceBundleManager.addActionListener(listener);
	}
	
	public static int getKeyEventKey(String character) {
		if (character == null) return KeyEvent.VK_A;
		character = character.toUpperCase();
	
		if (character.startsWith("A")) return KeyEvent.VK_A;
		else if (character.startsWith("B")) return KeyEvent.VK_B;
		else if (character.startsWith("C")) return KeyEvent.VK_C;
		else if (character.startsWith("D")) return KeyEvent.VK_D;
		else if (character.startsWith("E")) return KeyEvent.VK_E;
		else if (character.startsWith("F")) return KeyEvent.VK_F;
		else if (character.startsWith("G")) return KeyEvent.VK_G;
		else if (character.startsWith("H")) return KeyEvent.VK_H;
		else if (character.startsWith("I")) return KeyEvent.VK_I;
		else if (character.startsWith("J")) return KeyEvent.VK_J;
		else if (character.startsWith("K")) return KeyEvent.VK_K;
		else if (character.startsWith("L")) return KeyEvent.VK_L;
		else if (character.startsWith("M")) return KeyEvent.VK_M;
		else if (character.startsWith("N")) return KeyEvent.VK_N;
		else if (character.startsWith("O")) return KeyEvent.VK_O;
		else if (character.startsWith("P")) return KeyEvent.VK_P;
		else if (character.startsWith("Q")) return KeyEvent.VK_Q;
		else if (character.startsWith("R")) return KeyEvent.VK_R;
		else if (character.startsWith("S")) return KeyEvent.VK_S;
		else if (character.startsWith("T")) return KeyEvent.VK_T;
		else if (character.startsWith("U")) return KeyEvent.VK_U;
		else if (character.startsWith("V")) return KeyEvent.VK_V;
		else if (character.startsWith("W")) return KeyEvent.VK_W;
		else if (character.startsWith("X")) return KeyEvent.VK_X;
		else if (character.startsWith("Y")) return KeyEvent.VK_Y;
		else if (character.startsWith("Z")) return KeyEvent.VK_Z;
		else if (character.startsWith("0")) return KeyEvent.VK_0;
		else if (character.startsWith("1")) return KeyEvent.VK_1;
		else if (character.startsWith("2")) return KeyEvent.VK_2;
		else if (character.startsWith("3")) return KeyEvent.VK_3;
		else if (character.startsWith("4")) return KeyEvent.VK_4;
		else if (character.startsWith("5")) return KeyEvent.VK_5;
		else if (character.startsWith("6")) return KeyEvent.VK_6;
		else if (character.startsWith("7")) return KeyEvent.VK_7;
		else if (character.startsWith("8")) return KeyEvent.VK_8;
		else if (character.startsWith("9")) return KeyEvent.VK_9;

		return KeyEvent.VK_A;
	}
	
}