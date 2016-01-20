package es.uned.dia.model_elements.matlab;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.Reader;
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
import org.opensourcephysics.tools.ResourceLoader;
import es.uned.dia.model_elements.Utilities;

public class MatlabElement implements ModelElement {

	static private final String BEGIN_FILE_HEADER = "<Filename><![CDATA["; // Used to delimit my XML information
	static private final String END_FILE_HEADER = "]]></Filename>";        // Used to delimit my XML information

	private JTextField field=new JTextField();;  
	static ImageIcon ELEMENT_ICON = ResourceLoader.getIcon("es/uned/dia/model_elements/matlab/matlab.png"); // This icon is included in this jar
	//static ImageIcon LINK_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/link.gif");      // This icon is bundled with EJS
	//static ImageIcon FILE_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/openSmall.gif"); // This icon is bundled with EJS

	private JDialog helpDialog; // The dialog for help
	private JDialog  editorDialog; // The dialog for edition

	private ModelElementsCollection elementsCollection; // A provider of services for edition under EJS
	private static final long serialVersionUID = 1L;

	private DefaultTableModel matlabTableModel;
	private JTable table;
	private JScrollPane scrollPane;
	private JPopupMenu popupMenu;  
	private JTextField addressText;
	private JLabel addressLabel;	
	private JLabel modeLabel;
	private JTextField portText;
	private JLabel portLabel;
	private JComboBox modeComboBox;
	private JCheckBox asynchronousButton;
	private JCheckBox automaticButton;
	private JCheckBox waitForeverButton;
	
	//Default options of Matlab Element
	private boolean localMode=true;
	private boolean remoteAsynchronous=false;
    private String remoteIP="";
    private String remotePort="";
    private boolean waitForEver=false;
    private boolean automaticConnection=true;
    //private boolean linkedVariables=false;
    private ArrayList<String> variablesTable=new ArrayList<String>();
    //private String jmatlinkFile=null;
	
	public MatlabElement(){		
		variablesTable.clear();
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
	}
	
	public String getGenericName() { return "matlab"; }

	public ImageIcon getImageIcon() { return ELEMENT_ICON; }

	public String getConstructorName() {
		//Local Matlab
		if (localMode) return "es.uned.dia.model_elements.matlab.MatlabWrapper";
		else return "es.uned.dia.interoperate.matlab.jimc.RMatlabExternalApp";		             
	}

	public String getInitializationCode(String _name) {	
		String _code;
		//Create Matlab constructor	only once				
		if (localMode) {
			_code= "if ("+_name+"==null) {"+_name + " = new " + getConstructorName() + "();\n";			
		}else {			
			if (remoteAsynchronous) _code= "if ("+_name+"==null) {"+_name + " = new "+ getConstructorName()+"(\"<matlabas:"+remoteIP+":"+remotePort+">\");\n";
			else _code="if ("+_name+"==null) {"+_name + " = new "+getConstructorName()+"(\"<matlab:"+remoteIP+":"+remotePort+">\");\n";
		}
				
		//Check Linked Variables
		  String[] variables;
		  String matlabVariable="";
		  String ejsVariable="";
		  boolean firstLink=true;	
		  for (Iterator<String> e = variablesTable.listIterator(); e.hasNext(); ) {
			  variables=e.next().split(",");
			  if (variables.length<2) continue;
			  matlabVariable=variables[0];
			  ejsVariable=variables[1];
			  if ((matlabVariable.trim()!="" || matlabVariable.trim()!=null) && (ejsVariable.trim()!="" || ejsVariable.trim()!=null)) {
					if (firstLink) {
						_code=_code+_name+".setClient(_model);\n";
						firstLink=false;
					}
					_code=_code+_name+".linkVariables(\""+ejsVariable.trim()+"\",\""+matlabVariable.trim()+"\");\n";						 
			  }		
          }	
				  
		//advanced Options
		_code=_code+_name+".setWaitForEver("+waitForEver+");\n";
		if (automaticConnection) _code=_code+_name+".connect();\n";
		_code=_code+"}\n";
		return _code;
	}

	public String getDestructionCode(String _name) {
		String code="";
		if (automaticConnection) return code=code+_name+".disconnect();\n";		
		return code;
	}

	public String getResourcesRequired() {
		   String path=this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		   path=path.substring(6,path.lastIndexOf("/"));
		   path=path+"/../../jimc.jar";
		   
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
		   org.colos.ejs.library.utils.TemporaryFilesManager.extractToDirectory("jimc.jar", new File(wsp+"source/"), true);		   
		   return "jimc.jar";
	}

	public String getPackageList() {
		return null;
	}

	public String getDisplayInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public String savetoXML() {
		StringBuffer data=new StringBuffer();
		data.append(BEGIN_FILE_HEADER);
		
		//Matlab Configuration
		data.append("<localMode>");data.append(localMode);data.append("<\\localMode>");
		data.append("<remoteAsynchronous>");data.append(remoteAsynchronous);data.append("<\\remoteAsynchronous>");
		data.append("<remoteIP>");data.append(remoteIP);data.append("<\\remoteIP>");
		data.append("<remotePort>");data.append(remotePort);data.append("<\\remotePort>");
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
			
			//***check***
			field.getDocument().addDocumentListener (new DocumentListener() {
				public void changedUpdate(DocumentEvent e) { elementsCollection.reportChange(MatlabElement.this); }
				public void insertUpdate(DocumentEvent e)  { elementsCollection.reportChange(MatlabElement.this); }
				public void removeUpdate(DocumentEvent e)  { elementsCollection.reportChange(MatlabElement.this); }
			});
			
			//Panels of the Dialog
			JPanel topPanel = new JPanel(new BorderLayout());
			JPanel bottomPanel = new JPanel(new BorderLayout());
			
			//Location of Matlab Engine			  
			modeLabel=new JLabel("Mode ");								
			modeComboBox = new JComboBox(new String[]{"Local","Remote"});		
			addressLabel = new JLabel("Server Address ");			
			addressText=new JTextField(10);	
			portLabel = new JLabel("Port ");
			portText=new JTextField(4);	
        	asynchronousButton=new JCheckBox("Asynchronous",false);        				
			modeComboBox.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {					
					JComboBox cb = (JComboBox)e.getSource();
			        if (cb.getSelectedIndex()==0){
			        	addressText.setEnabled(false);
			        	addressLabel.setEnabled(false);
			        	portText.setEnabled(false);
			        	portLabel.setEnabled(false);
			        	asynchronousButton.setEnabled(false);
			        }else{
			        	addressText.setEnabled(true);
			        	addressLabel.setEnabled(true);
			        	portText.setEnabled(true);
			        	portLabel.setEnabled(true);
			        	asynchronousButton.setEnabled(true);			        	
			        }			      
				}				
			}				
			);											 
			JPanel urlPanel= new JPanel(new FlowLayout());
			urlPanel.add(modeLabel);
			urlPanel.add(modeComboBox);
			urlPanel.add(addressLabel);
			urlPanel.add(addressText);
			urlPanel.add(portLabel);
			urlPanel.add(portText);
			urlPanel.add(asynchronousButton);			
			TitledBorder titleUrlPanel;
			titleUrlPanel = BorderFactory.createTitledBorder("Location of Matlab Engine");
			urlPanel.setBorder(titleUrlPanel);
			topPanel.add(urlPanel,BorderLayout.NORTH);

			//Table of Linked Variables		
			matlabTableModel = new DefaultTableModel();
			matlabTableModel.addColumn("MATLAB Variable");
			matlabTableModel.addColumn("EJS Variable");
			table=new JTable(matlabTableModel);  			
			TableModelListener ta=new TableModelListener (){ //Add a row to the table when last row is edited
				public void tableChanged(TableModelEvent e){
					if (TableModelEvent.UPDATE==e.getType() && table.getRowCount()==e.getLastRow()+1){
						matlabTableModel.addRow(new String[]{"",""});
					}    		
				}    	  
			};
			table.getModel().addTableModelListener(ta);
			popupMenu = new JPopupMenu(); // The popup menu for the table
			popupMenu.add(new AbstractAction("Insert"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount())
						matlabTableModel.insertRow(table.getSelectedRow()+1,new String[]{"",""}); 
				}
			});
			popupMenu.add(new AbstractAction("Delete"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount() && table.getRowCount()>1)
						matlabTableModel.removeRow(table.getSelectedRow()); 
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
	        	if (modeComboBox.getSelectedIndex()==0) localMode=true;
	        	else localMode=false;
	        	if (asynchronousButton.isSelected()) remoteAsynchronous=true;
	        	else remoteAsynchronous=false;
	        	remoteIP=addressText.getText();
	        	remotePort=portText.getText();
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
			java.net.URL htmlURL = ResourceLoader.getResource("es/uned/dia/model_elements/matlab/Matlab.html").getURL();
			htmlArea.setPage(htmlURL);
		} catch(Exception exc) { exc.printStackTrace(); }
		return helpComponent;
	}

	
	private void processXMLData(String dataFromXML){
		//Load Data from XML
		if (dataFromXML!=null){
			if (getXMLData(dataFromXML,"<localMode>","<\\localMode>").equalsIgnoreCase("true")) localMode=true;
			else localMode=false;
			if (getXMLData(dataFromXML,"<remoteAsynchronous>","<\\remoteAsynchronous>").equalsIgnoreCase("true")) remoteAsynchronous=true;
			else remoteAsynchronous=false;
			remoteIP=getXMLData(dataFromXML,"<remoteIP>","<\\remoteIP>");
			remotePort=getXMLData(dataFromXML,"<remotePort>","<\\remotePort>");
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
		addressText.setText(remoteIP);
		portText.setText(remotePort);						
		if (remoteAsynchronous) asynchronousButton.setSelected(true);		    
		else asynchronousButton.setSelected(false); 		
		if (localMode) {
			modeComboBox.setSelectedIndex(0);
	    	addressText.setEnabled(false);
	    	addressLabel.setEnabled(false);
	    	portText.setEnabled(false);
	    	portLabel.setEnabled(false);
	    	asynchronousButton.setEnabled(false);	    	
		}
		else {
			modeComboBox.setSelectedIndex(1);
	    	addressText.setEnabled(true);
	    	addressLabel.setEnabled(true);
	    	portText.setEnabled(true);
	    	portLabel.setEnabled(true);
	    	asynchronousButton.setEnabled(true);
		}								
			String[] variables;
			for (Iterator<String> e = variablesTable.listIterator(); e.hasNext(); ) {
				variables=e.next().split(",");
				matlabTableModel.addRow(variables);  
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

	public String getTooltip() {
		return "encapsulates an object that connects EJS to Matlab";
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

