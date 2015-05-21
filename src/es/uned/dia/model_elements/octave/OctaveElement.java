package es.uned.dia.model_elements.octave;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.colos.ejs.library.control.EjsControl;
import org.colos.ejs.model_elements.ModelElement;
import org.colos.ejs.model_elements.ModelElementSearch;
import org.colos.ejs.model_elements.ModelElementsCollection;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;
import es.uned.dia.model_elements.Utilities;

public class OctaveElement implements ModelElement {

	static private final String BEGIN_FILE_HEADER = "<Filename><![CDATA["; // Used to delimit my XML information
	static private final String END_FILE_HEADER = "]]></Filename>";        // Used to delimit my XML information

	private JTextField field=new JTextField();;  
	static ImageIcon ELEMENT_ICON = ResourceLoader.getIcon("es/uned/dia/model_elements/octave/octave.png"); // This icon is included in this jar
	//static ImageIcon LINK_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/link.gif");      // This icon is bundled with EJS
	//static ImageIcon FILE_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/openSmall.gif"); // This icon is bundled with EJS

	private JDialog helpDialog; // The dialog for help
	private JDialog  editorDialog; // The dialog for edition

	private ModelElementsCollection elementsCollection; // A provider of services for edition under EJS
	private static final long serialVersionUID = 1L;

	private DefaultTableModel octaveTableModel;
	private JTable table;
	private JScrollPane scrollPane;
	private JPopupMenu popupMenu;  
	private JCheckBox automaticButton;
	private JCheckBox waitForeverButton;
	
	//Default options of Octave Element
    private boolean waitForEver=false;
    private boolean automaticConnection=true;
    //private boolean linkedVariables=false;
    private ArrayList<String> variablesTable=new ArrayList<String>();
	
	public OctaveElement(){		
		variablesTable.clear();
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
	}
	
	public String getGenericName() { return "octave"; }

	public ImageIcon getImageIcon() { return ELEMENT_ICON; }

	public String getConstructorName() {						
		 return "es.uned.dia.model_elements.octave.OctaveWrapper";
		//return "es.uned.dia.interoperate.octave.OctaveExternalApp";
	}

	public String getInitializationCode(String _name) {	
		String _code;
		//Create octave constructor only once						
	    _code= "if ("+_name+"==null) "+_name + " = new " + getConstructorName() + "();\n";

		//Check Linked Variables
		  String[] variables;
		  String octaveVariable="";
		  String ejsVariable="";
		  boolean firstLink=true;	
		  for (Iterator<String> e = variablesTable.listIterator(); e.hasNext(); ) {
			  variables=e.next().split(",");
			  if (variables.length<2) continue;
			  octaveVariable=variables[0];
			  ejsVariable=variables[1];
			  if ((octaveVariable.trim()!="" || octaveVariable.trim()!=null) && (ejsVariable.trim()!="" || ejsVariable.trim()!=null)) {
					if (firstLink) {
						_code=_code+_name+".setClient(_model);\n";
						firstLink=false;
					}
					_code=_code+_name+".linkVariables(\""+ejsVariable.trim()+"\",\""+octaveVariable.trim()+"\");\n";						 
			  }		
          }	
		
		  //class path
		  _code=_code+_name+".addJars();\n";
		  
		  //advanced Options
		  _code=_code+_name+".setWaitForEver("+waitForEver+");\n";
		  if (automaticConnection) _code=_code+_name+".connect();\n";
	 	  return _code;
	}

	public String getDestructionCode(String _name) {
		String code="";
		if (automaticConnection) return code=code+_name+".disconnect();\n";		
		return null;
	}

	public String getResourcesRequired() {		
						
		//Copy resource from /extensions to /workspace/source			
		org.colos.ejs.library.utils.TemporaryFilesManager.extractToDirectory("commons-logging-1.1.1.jar", new File(getWorkSpacePath()+"source/"), true);	
		org.colos.ejs.library.utils.TemporaryFilesManager.extractToDirectory("javaoctave-0.6.1.jar", new File(getWorkSpacePath()+"source/"), true);
		org.colos.ejs.library.utils.TemporaryFilesManager.extractToDirectory("log4j-1.2.15.jar", new File(getWorkSpacePath()+"source/"), true);		   
		return "commons-logging-1.1.1.jar;javaoctave-0.6.1.jar;log4j-1.2.15.jar";				
	}

	public String getPackageList() {
	    /*
		String pkg="C:/Documents and Settings/Gonzalo/Escritorio/papers/EJSELEMENTOS/ProyectoEclipseUltimo/EJS_4.3.2_workspace/Ejs/distribution/bin/extensions/commons-logging-1.1.1.jar;";
		pkg=pkg+"C:/Documents and Settings/Gonzalo/Escritorio/papers/EJSELEMENTOS/ProyectoEclipseUltimo/EJS_4.3.2_workspace/Ejs/distribution/bin/extensions/javaoctave-0.6.1.jar;";
		return pkg+"C:/Documents and Settings/Gonzalo/Escritorio/papers/EJSELEMENTOS/ProyectoEclipseUltimo/EJS_4.3.2_workspace/Ejs/distribution/bin/extensions/log4j-1.2.15.jar";
		*/
		return null;
	}

	public String getDisplayInfo() {
		return null;
	}

	public String savetoXML() {
		StringBuffer data=new StringBuffer();
		data.append(BEGIN_FILE_HEADER);
		
		//Octave Configuration
		data.append("<automaticConnection>");data.append(automaticConnection);data.append("<\\automaticConnection>");
		data.append("<waitForEver>");data.append(waitForEver);data.append("<\\waitForEver>");				
		
		//Linked variables
		    String variables;			 
			int rowIndex=0;
			for (Iterator<String> e = variablesTable.listIterator(); e.hasNext(); ) {
				  variables=e.next();				  
				  data.append("<tablerow"+rowIndex+">");
				  data.append(variables);
				  data.append("<\\tablerow"+rowIndex+">");
				  rowIndex++;
			  } 	
			
		data.append(END_FILE_HEADER);
		return data.toString();
	}

	public void readfromXML(String _inputXML) {
		int begin = _inputXML.indexOf(BEGIN_FILE_HEADER);
		if (begin<0) return; // A syntax error
		int end = _inputXML.indexOf(END_FILE_HEADER,begin);
		if (end<0) return; // Another syntax error		   		
		//dataFromXML=_inputXML.substring(begin+BEGIN_FILE_HEADER.length(),end).split("<row>=");
		processXMLData(_inputXML.substring(begin+BEGIN_FILE_HEADER.length(),end));
	}

	public void refreshEditor(String arg0) {
		// TODO Auto-generated method stub
	}

	// -------------------------------
	// Help and edition
	// -------------------------------

	public void showHelp(Component _parentComponent) {
		if (helpDialog==null) { // create the dialog
			helpDialog = new JDialog((JFrame) null,"External Application: "+ getGenericName());
			helpDialog.getContentPane().setLayout(new BorderLayout());
			helpDialog.getContentPane().add(createHelpComponent(),BorderLayout.CENTER);
			helpDialog.setModal(false);
			helpDialog.pack();
		}
		java.awt.Rectangle bounds = EjsControl.getDefaultScreenBounds();
		if (_parentComponent==null) helpDialog.setLocation(bounds.x + (bounds.width - helpDialog.getWidth())/2, bounds.y + (bounds.height - helpDialog.getHeight())/2);
		else helpDialog.setLocationRelativeTo(_parentComponent);
		helpDialog.setVisible(true);
	}

	/**
	 * This editor chooses to include the help for the element (because it is just so small). 
	 * But this is not compulsory.
	 */
	public void showEditor(String _name, Component _parentComponent, ModelElementsCollection _collection) {
		this.elementsCollection = _collection;
		if (editorDialog==null) { // create the dialog
			
			//*******REVISAR
			field.getDocument().addDocumentListener (new DocumentListener() {
				public void changedUpdate(DocumentEvent e) { elementsCollection.reportChange(OctaveElement.this); }
				public void insertUpdate(DocumentEvent e)  { elementsCollection.reportChange(OctaveElement.this); }
				public void removeUpdate(DocumentEvent e)  { elementsCollection.reportChange(OctaveElement.this); }
			});
			
			//Panels of the Dialog
			JPanel topPanel = new JPanel(new BorderLayout());
			JPanel bottomPanel = new JPanel(new BorderLayout());
		
			//Table of Linked Variables		
			octaveTableModel = new DefaultTableModel();
			octaveTableModel.addColumn("Octave Variable");
			octaveTableModel.addColumn("EJS Variable");
			table=new JTable(octaveTableModel);  			
			TableModelListener ta=new TableModelListener (){ //Add a row to the table when last row is edited
				public void tableChanged(TableModelEvent e){
					if (TableModelEvent.UPDATE==e.getType() && table.getRowCount()==e.getLastRow()+1){
						octaveTableModel.addRow(new String[]{"",""});
					}    		
				}    	  
			};
			table.getModel().addTableModelListener(ta);
			popupMenu = new JPopupMenu(); // The popup menu for the table
			popupMenu.add(new AbstractAction("Insert"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount())
						octaveTableModel.insertRow(table.getSelectedRow()+1,new String[]{"",""}); 
				}
			});
			popupMenu.add(new AbstractAction("Delete"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount() && table.getRowCount()>1)
						octaveTableModel.removeRow(table.getSelectedRow()); 
				}
			});
			popupMenu.add(new AbstractAction("Connect"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) { 
					String value = (String)table.getValueAt(table.getSelectedRow(), 0);
					if (!Utilities.isLinkedToVariable(value)) value = "";
					else value = Utilities.getPureValue(value);
					String variable = elementsCollection.chooseVariable(table,"String|double|double[]|double[][]", value);
					if (variable!=null) table.setValueAt(variable,table.getSelectedRow(), 1);
				}
			});
			MouseListener ma = new MouseAdapter() {
				public void mousePressed (MouseEvent _evt) {        	  
					if (OSPRuntime.isPopupTrigger(_evt) && table.isEnabled ()) popupMenu.show(_evt.getComponent(), _evt.getX(), _evt.getY());        	
				}
			};
			table.addMouseListener (ma);
			scrollPane = new JScrollPane(table); //an scroll panel for the table			
			TitledBorder title;
			title = BorderFactory.createTitledBorder("Table of Linked Variables");
			scrollPane.setBorder(title);
			
			//Advanced Options		
			automaticButton=new JCheckBox("Automatic Connect & Disconnect");			
			waitForeverButton=new JCheckBox("Wait Forever");				
			JPanel advancedPanel=new JPanel(new FlowLayout());
			TitledBorder titleAdvancedPanel;
			titleAdvancedPanel = BorderFactory.createTitledBorder("Advanced Options");
			advancedPanel.setBorder(titleAdvancedPanel);			
			advancedPanel.add(automaticButton,BorderLayout.WEST);
			advancedPanel.add(waitForeverButton,BorderLayout.EAST);
			bottomPanel.add(advancedPanel,BorderLayout.NORTH);

			//Create and Configurate Dialog
			editorDialog = new JDialog((JFrame) null,"Configuration of External Application: "+ _name);
			editorDialog.getContentPane().setLayout(new BorderLayout());
			editorDialog.getContentPane().add(topPanel,BorderLayout.NORTH);
			editorDialog.getContentPane().add(scrollPane,BorderLayout.CENTER);
			editorDialog.getContentPane().add(bottomPanel,BorderLayout.SOUTH);		
			editorDialog.setSize(475, 350);
			editorDialog.setModal(false);										
			editorDialog.addWindowListener(new WindowAdapter(){	        
	          public void windowClosing(WindowEvent we){	 
	        	if (waitForeverButton.isSelected()) waitForEver=true;
	        	else waitForEver=false;
	        	if (automaticButton.isSelected()) automaticConnection=true;
	        	else automaticConnection=false;
	        	variablesTable.clear();
	        	String ejsVariable,matlabVariable;
	  		    for (int i=0; i<table.getRowCount();i++){	  		    	
	  		    	ejsVariable="";
	  		    	matlabVariable="";	  		    	
	  		    	if (((String)table.getValueAt(i, 0))!=null && ((String)table.getValueAt(i, 0))!="") matlabVariable=((String)table.getValueAt(i, 0)); 
	  		   	    if (((String)table.getValueAt(i, 1))!=null && ((String)table.getValueAt(i, 1))!="") ejsVariable=((String)table.getValueAt(i, 1));	  		    		
	  		   	    variablesTable.add(matlabVariable+","+ejsVariable);	  		   	    
	  			}	   	  		    	         
	          }
	        });
			
			//Initiate Dialog if any data was loaded
			initiateDialogData();
			editorDialog.pack();
			
		}
		java.awt.Rectangle bounds = EjsControl.getDefaultScreenBounds();
		if (_parentComponent==null) editorDialog.setLocation(bounds.x + (bounds.width - editorDialog.getWidth())/2, bounds.y + (bounds.height - editorDialog.getHeight())/2);
		else editorDialog.setLocationRelativeTo(_parentComponent);
		editorDialog.setVisible(true);		
	}

	// -------------------------------
	// Utilities
	// -------------------------------

	/**
	 * Creates an HTML viewer with information about the class
	 */
	static private Component createHelpComponent() {
		JEditorPane htmlArea = new JEditorPane ();
		htmlArea.setContentType ("text/html");
		htmlArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		htmlArea.setEditable(false);
		htmlArea.addHyperlinkListener(new HyperlinkListener() { // Make hyperlinks work
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType()==HyperlinkEvent.EventType.ACTIVATED)
					OSPDesktop.displayURL(e.getURL().toString());
			}
		});
		JScrollPane helpComponent = new JScrollPane(htmlArea);
		helpComponent.setPreferredSize(new Dimension(600,500));

		try { // read the help for this element
			java.net.URL htmlURL = ResourceLoader.getResource("es/uned/dia/model_elements/octave/Octave.html").getURL();
			htmlArea.setPage(htmlURL);
		} catch(Exception exc) { exc.printStackTrace(); }
		return helpComponent;
	}

	
	private void processXMLData(String dataFromXML){
		//Load Data from XML
		if (dataFromXML!=null){
			if (getXMLData(dataFromXML,"<waitForEver>","<\\waitForEver>").equalsIgnoreCase("true")) waitForEver=true;
			else waitForEver=false;
			if (getXMLData(dataFromXML,"<automaticConnection>","<\\automaticConnection>").equalsIgnoreCase("true")) automaticConnection=true;
			else automaticConnection=false;	
										
				int rowIndex=0;
				String variablesRow="";	
				variablesTable.clear();
				while((variablesRow=getXMLData(dataFromXML,"<tablerow"+rowIndex+">","<\\tablerow"+rowIndex+">"))!=null){
					variablesTable.add(variablesRow);
					rowIndex++;
				}								
		}		
	}

	private void initiateDialogData(){		
        //Update Dialog
			String[] variables;
			for (Iterator<String> e = variablesTable.listIterator(); e.hasNext(); ) {
				variables=e.next().split(",");
				octaveTableModel.addRow(variables);  
			} 				
		automaticButton.setSelected(automaticConnection);
		waitForeverButton.setSelected(waitForEver); 		    	
	}

	
	private String getXMLData(String xml, String beginTag, String endTag){		
		int begin = xml.indexOf(beginTag);
		if (begin<0) return null; // A syntax error
		int end = xml.indexOf(endTag,begin);
		if (end<0) return null; // Another syntax error		   				
		return xml.substring(begin+beginTag.length(),end);
	}

    private String getWorkSpacePath(){
		File optionsFile = new File (System.getProperty("user.home")+"/.EjsConsole.txt");
		String wsp="";
		try {
			Reader reader = new FileReader(optionsFile);
			LineNumberReader l = new LineNumberReader(reader);
			String sl = l.readLine();
			while (sl != null) {
				if (sl.startsWith("UserDir="))  wsp=sl.substring(8);
				sl = l.readLine();
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();		      
		}
		return wsp;
    }
    
    private boolean extractResourcesFromJar(String resourcePath,String resourceName){    	
		Resource auxresource=ResourceLoader.getResource(resourcePath+"/"+resourceName);
		String extensionsPath=this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		extensionsPath=extensionsPath.substring(0,extensionsPath.lastIndexOf("/"));
		extensionsPath=extensionsPath+"/../../"+resourceName;		
		try{	        
			File f2 = new File(new java.net.URI(extensionsPath));
			InputStream in =  auxresource.openInputStream();
			OutputStream out = new FileOutputStream(f2);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			f2.deleteOnExit();		        
		}
		catch(FileNotFoundException ex){
			System.out.println("error:"+ex);
			return false;
		}
		catch(IOException e){
			System.out.println("error:"+e);
			return false;
		} catch (URISyntaxException e) {
			System.out.println("error:"+e);
			return false;
		}
		return true;
    }
    
	public String getTooltip() {
		return "encapsulates an object that connects EJS to Octave";
	}

	@Override
	public String getImportStatements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFont(Font font) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ModelElementSearch> search(String info, String searchString,
			int mode, String elementName, ModelElementsCollection collection) {
		// TODO Auto-generated method stub
		return null;
	}

}
