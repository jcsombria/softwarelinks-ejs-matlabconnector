package es.uned.dia.model_elements.simulink;
import java.awt.BorderLayout;
import java.awt.Color;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

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
import org.colos.ejs.osejs.utils.InterfaceUtils;
import org.opensourcephysics.desktop.OSPDesktop;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.ResourceLoader;
import es.uned.dia.model_elements.Utilities;

public class SimulinkElement implements ModelElement, ActionListener {

	static private final String BEGIN_FILE_HEADER = "<Filename><![CDATA["; // Used to delimit my XML information
	static private final String END_FILE_HEADER = "]]></Filename>";        // Used to delimit my XML information

	private JTextField field=new JTextField(); 
	static ImageIcon ELEMENT_ICON = ResourceLoader.getIcon("es/uned/dia/model_elements/simulink/simulink.png"); // This icon is included in this jar
	static ImageIcon LINK_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/link.gif");      // This icon is bundled with EJS
	static ImageIcon FILE_ICON = org.opensourcephysics.tools.ResourceLoader.getIcon("data/icons/openSmall.gif"); // This icon is bundled with EJS

	private JDialog helpDialog; // The dialog for help
	private JDialog  editorDialog; // The dialog for edition

	private ModelElementsCollection elementsCollection; // A provider of services for edition under EJS
	private static final long serialVersionUID = 1L;

	private DefaultTableModel matlabTableModel;
	private JTable table;
	private JScrollPane scrollPane;
	private JPopupMenu popupMenuEJS;  
	private JPopupMenu popupMenuSMK;
	private JTextField addressText;
	private JLabel addressLabel;	
	private JLabel modeLabel;
	private JTextField portText;
	private JLabel portLabel;
	private JComboBox modeComboBox;
	private JCheckBox asynchronousButton;
	private JCheckBox automaticButton;
	private JCheckBox waitForeverButton;
	JTextField smkModelText;
	
	//Default options of Matlab Element
	private boolean localMode=true;
	private boolean remoteAsynchronous=false;
    private String remoteIP="";
    private String remotePort="";
    private boolean waitForEver=false;
    private boolean automaticConnection=true;
    private String smkModel="";    
    //private boolean linkedVariables=false;
    private ArrayList<String> variablesTable=new ArrayList<String>();
    private es.uned.dia.interoperate.ExternalApp matlab;
    
    
    static private final String SEPARATOR="_%_";
    static private final String INTEGRATOR_BLOCK="-IB-";
    static private final String MAIN_BLOCK="_Global_Parameters_";
    static private final String MAIN_PARAMETERS="Parameters=time,InitialStep,FixedStep,MaxStep;";
    
    //static private EjsJMatLink matlab=null;
    private boolean blockComboActive = false, currentBlockIsIntegrator=false;
    //private long id;   // The integer id for the Matlab engine opened
    private double currentBlockHandle=Double.NaN;
    private String userDir;
    private String returnValue=null, currentBlockName=null, originalBlockName=null;
    private javax.swing.Timer timer;
    private Color blockComboDefColor;
    
    static private JLabel blockLabel, variableLabel;
    static private JButton blockDeleteButton, blockUndeleteButton;
    static private JCheckBox blockCB;
    static private JComboBox blockCombo;
    static private JDialog dialog;
    private DefaultListModel modelInput = new DefaultListModel();
    private DefaultListModel modelOutput= new DefaultListModel();
    private DefaultListModel modelParameters= new DefaultListModel();
    private JList listBlockInput,listBlockOutput,listBlockParameters;
    private JList listVariableInput,listVariableOutput,listVariableParameters;
    
    
    
    private Vector<String> blocksInModel=null;
    private Vector<String> blocksToDelete=new Vector<String>();
   // private boolean hasChanged=false;
    private boolean fileExists=true;
    public String mdlFile, theMdlFile;
    static String theMdlFileStatic; //Gonzalo 060629
    
    private URL userDirURL=null;
       
	
	public SimulinkElement(){		
		variablesTable.clear();
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
		variablesTable.add(",");
	}
	
	public String getGenericName() { return "simulink"; }

	public ImageIcon getImageIcon() { return ELEMENT_ICON; }

	public String getConstructorName() {						
		//Local Matlab
		if (localMode) return "es.uned.dia.model_elements.simulink.SimulinkWrapper";
		else return "es.uned.dia.model_elements.simulink.RSimulinkWrapper";		             
	}

	public String getInitializationCode(String _name) {					
		String _code;
		//Create Matlab constructor	only once				
		if (localMode) {
			//_code= "if ("+_name+"==null) {"+_name + " = new " + getConstructorName() + "(\""+userDir+"\",\"<matlab>"+smkModel+"\");\n";
			_code= "if ("+_name+"==null) {"+_name + " = new " + getConstructorName() + "(\"<matlab>"+smkModel+"\");\n";
			_code=_code+ _name+".setSmkMdlDir(\""+userDir+"\");\n";
		}else {			
			if (remoteAsynchronous) _code= "if ("+_name+"==null) {"+_name + " = new "+ getConstructorName()+"(\"<matlabas:"+remoteIP+":"+remotePort+">"+smkModel+"\");\n";
			else _code="if ("+_name+"==null) {"+_name + " = new "+getConstructorName()+"(\"<matlab:"+remoteIP+":"+remotePort+">"+smkModel+"\");\n";
			_code=_code+ _name+".setSmkMdlDir(\""+userDir+"\");\n";
		}
				
		//Add code to link variables
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
		        	Vector<String> line=new Vector<String>();
		  		   	line=addToInitCode(ejsVariable.trim(), matlabVariable, 0);
		  		    for (int j=0;j<line.size();j++){
		  		      	_code=_code+_name+"."+line.get(j)+"\n";
			  		}	  		    		  		    
					//_code=_code+_name+".linkVariables(\""+ejsVariable.trim()+"\",\""+matlabVariable.trim()+"\");\n";						 
			  }		
          }		
		  
		  //Add code to delete blocks
		  for (Enumeration<String> e = blocksToDelete.elements(); e.hasMoreElements();) {
  				String blockName = e.nextElement();		 
  				_code=_code+_name+".deleteBlock(\""+theMdlFile+"/"+blockName+"\");\n"; 
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
		return null;
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
		return "jimc.jar;"+smkModel;
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
		data.append("<smkModel>");data.append(smkModel);data.append("<\\smkModel>");		
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
		//Deleted blocks
		 String blockName;			 
		 rowIndex=0;
		 for (Enumeration<String> e = blocksToDelete.elements(); e.hasMoreElements();) {
			 blockName= e.nextElement();				  
				  data.append("<blocksToDelete"+rowIndex+">");
				  data.append(blockName);
				  data.append("<\\blocksToDelete"+rowIndex+">");
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
	
	public String getTooltip() {
		return "encapsulates an object that connects EJS to Simulink";
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
				public void changedUpdate(DocumentEvent e) { elementsCollection.reportChange(SimulinkElement.this); }
				public void insertUpdate(DocumentEvent e)  { elementsCollection.reportChange(SimulinkElement.this); }
				public void removeUpdate(DocumentEvent e)  { elementsCollection.reportChange(SimulinkElement.this); }
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
			
			//Simulink Model
			JLabel smkModelLabel=new JLabel("Simulink Model ");
			smkModelText=new JTextField(10);						 		   
		    JButton smkModelButton = new JButton(FILE_ICON);
		    smkModelButton.addActionListener(new ActionListener(){
		        public void actionPerformed(ActionEvent e) {
		          String filename = elementsCollection.chooseFilename(smkModelText, "Simulink model chooser", "Simulink files", "mdl");
		          if (filename!=null) smkModelText.setText(filename);
		        }
		    });
            JPanel modelPanel= new JPanel(new BorderLayout());
            modelPanel.add(smkModelLabel,BorderLayout.WEST);
            modelPanel.add(smkModelText,BorderLayout.CENTER);
            modelPanel.add(smkModelButton,BorderLayout.EAST);
			
			TitledBorder titleUrlPanel;
			titleUrlPanel = BorderFactory.createTitledBorder("Location of Matlab Engine and Simulink Model");
			topPanel.add(urlPanel,BorderLayout.NORTH);
			topPanel.add(modelPanel,BorderLayout.SOUTH);
			topPanel.setBorder(titleUrlPanel);
			
			//Table of Linked Variables		
			matlabTableModel = new DefaultTableModel();
			matlabTableModel.addColumn("Simulink Variable");
			matlabTableModel.addColumn("Model Variable");
			table=new JTable(matlabTableModel);  			
			TableModelListener ta=new TableModelListener (){ //Add a row to the table when last row is edited
				public void tableChanged(TableModelEvent e){
					if (TableModelEvent.UPDATE==e.getType() && table.getRowCount()==e.getLastRow()+1){
						matlabTableModel.addRow(new String[]{"",""});
					}    		
				}    	  
			};
			table.getModel().addTableModelListener(ta);
			popupMenuEJS = new JPopupMenu(); // The popup menu for the table
			popupMenuSMK = new JPopupMenu(); // The popup menu for the table
			AbstractAction insert=new AbstractAction("Insert New Link"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount())
						matlabTableModel.insertRow(table.getSelectedRow()+1,new String[]{"",""}); 
				}
			};
			AbstractAction delete= new AbstractAction("Delete This Link"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) {
					if (table.getSelectedRow()>=0 && table.getSelectedRow()<table.getRowCount() && table.getRowCount()>1)
						matlabTableModel.removeRow(table.getSelectedRow()); 
				}
			};
			AbstractAction connectEJS = new AbstractAction("Get Model Variable"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) { 				
					String value = (String)table.getValueAt(table.getSelectedRow(), 0);
					if (!Utilities.isLinkedToVariable(value)) value = "";
					else value = Utilities.getPureValue(value);
					String variable = elementsCollection.chooseVariable(table,"String|double|double[]|double[][]", value);
					if (variable!=null) table.setValueAt(variable,table.getSelectedRow(), 1);
				}
			};
			AbstractAction connectSMK = new AbstractAction("Get External Variable"){
				private static final long serialVersionUID = 1L;
				public void actionPerformed(ActionEvent e) { 
					String auxSMKFile=smkModelText.getText();
					//checkFile (auxSMKFile.substring(1, auxSMKFile.length()-1)); // delete quotes
					checkFile (auxSMKFile); // delete quotes
					String SMKVariable="";
					if (table.getValueAt(table.getSelectedRow(),0)!=null) SMKVariable = (String)table.getValueAt(table.getSelectedRow(),0);
					String EJSVariable="";
					if (table.getValueAt(table.getSelectedRow(),1)!=null) EJSVariable = (String)table.getValueAt(table.getSelectedRow(),1);					
					String variable=edit ("My property", EJSVariable, table, SMKVariable);
					table.setValueAt(variable,table.getSelectedRow(), 0);										
				}
			};				
			popupMenuEJS.add(insert);			
			popupMenuEJS.add(delete);
			popupMenuEJS.add(connectEJS);			
			popupMenuSMK.add(insert);
			popupMenuSMK.add(delete);			
			popupMenuSMK.add(connectSMK);					
			MouseListener ma = new MouseAdapter() {
				public void mousePressed (MouseEvent _evt) {
					if (OSPRuntime.isPopupTrigger(_evt) && table.isEnabled ()) {
						if (table.columnAtPoint(_evt.getPoint())==0) popupMenuSMK.show(_evt.getComponent(), _evt.getX(), _evt.getY());        	
						else popupMenuEJS.show(_evt.getComponent(), _evt.getX(), _evt.getY());
					}
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
	        	smkModel=smkModelText.getText();
	        	
	        	if (waitForeverButton.isSelected()) waitForEver=true;
	        	else waitForEver=false;
	        	if (automaticButton.isSelected()) automaticConnection=true;
	        	else automaticConnection=false;
	        	variablesTable.clear();
	        	String ejsVariable,matlabVariable;
	        	//Vector<String> code=new Vector<String>();
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
			java.net.URL htmlURL = ResourceLoader.getResource("es/uned/dia/model_elements/simulink/Simulink.html").getURL();
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
			smkModel=getXMLData(dataFromXML,"<smkModel>","<\\smkModel>");
			if (smkModel!=null) checkFile (smkModel); 
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
								
				//Deleted blocks				
				rowIndex=0;
				String blockName="";	
				blocksToDelete.clear();
				while((blockName=getXMLData(dataFromXML,"<blocksToDelete"+rowIndex+">","<\\blocksToDelete"+rowIndex+">"))!=null){
					blocksToDelete.add(blockName);
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
			
	    smkModelText.setText(smkModel);
			
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
	
	
	 /**************************************************************   
    Implementation of the Action Listener
**************************************************************/

	public synchronized void actionPerformed (java.awt.event.ActionEvent event) {
		if (!systemIsOpen()) {  // In case the user closes the system directly
			blockCB.setSelected(false);
			blockCombo.setEnabled(true);
			timer.stop();
			return;
		}
		matlab.eval("EjsCurrentBlock=gcbh"); // ; EjsCurrentBlockName=get_param(EjsCurrentBlock,'name');");
		double blockHandle = matlab.getDouble("EjsCurrentBlock");
		if (currentBlockHandle==blockHandle) { // Check if the user has changed the name of the block
			matlab.eval("EjsCurrentBlockName=[get_param(EjsCurrentBlock,'parent'),'/',strrep(get_param(EjsCurrentBlock,'name'),'/','//')]");
			String blockName =matlab.getString("EjsCurrentBlockName");

			//String blockName = EjsMatlab.waitForString (matlab,id,"EjsCurrentBlockName",
			//                                                 "EjsCurrentBlockName=[get_param(EjsCurrentBlock,'parent'),'/',strrep(get_param(EjsCurrentBlock,'name'),'/','//')]"); // Gonzalo 060703
			matlab.eval("clear EjsCurrentBlock; clear EjsCurrentBlockName");
			if (blockName==null) return;
			String originalBlockNameR=theMdlFile.toLowerCase()+'/'+originalBlockName; 
			if (!blockName.equalsIgnoreCase(originalBlockNameR)) 
				JOptionPane.showMessageDialog(dialog, "Changing the name of a block in the model\nwill confuse EJS and is therefore not allowed here.\nPlease undo the change or close Simulink's window\n(without saving) to start again.",
						"Warning", JOptionPane.YES_NO_OPTION);
			else{ 
				matlab.eval("Ejs_Subsystem=find_system('dirty','on')");
				matlab.eval("for Ejs_index=1:length(Ejs_Subsystem), try, set_param(Ejs_Subsystem{Ejs_index},'dirty','off'), catch, end, end ");
			}
			return;
		}
		matlab.eval("clear EjsCurrentBlock;");
		// a new block has been selected
		currentBlockHandle = blockHandle;
		if (blockHandle!=-1) getBlockVariables ("block=gcb;\n"); // Query the block variables
	}

	/**
	 * Checks if has changed
	 * @return boolean
	 */
	/*
	private boolean isChanged () {
		return hasChanged; 
	}

*/
	/**
	 * Sets hasChanged variable
	 * @param _change
	 */
	/*
	private void setChanged (boolean _change) {
		hasChanged = _change; 
	}
*/

	/**************************************************************   
    Extending BrowserForExternal
	 **************************************************************/

	/**
	 * Constructor of the editor
	 * @param _mdlFile The file with which the application was opened
	 * @param _codebase String
	 */
	private void checkFile (String _mdlFile) {
		_mdlFile = _mdlFile.trim().replace('\\','/');
		if (_mdlFile.toLowerCase().startsWith("<matlab")) {
			int index = _mdlFile.indexOf('>');
			if (index>0) _mdlFile = _mdlFile.substring(index+1);
			else _mdlFile = "";
		}
		mdlFile = _mdlFile.trim();
		if (!mdlFile.toLowerCase().endsWith(".mdl")) mdlFile += ".mdl";
		if (mdlFile.lastIndexOf('/')>=0) theMdlFile = mdlFile.substring (mdlFile.lastIndexOf("/")+1, mdlFile.length()-4);
		else theMdlFile = mdlFile.substring (0, mdlFile.length()-4);
		try {      
			userDirURL = ResourceLoader.getResource(mdlFile).getURL();//new URL(_codebase, mdlFile);
			userDirURL.openStream(); // Check if the file exists
			userDir=userDirURL.getPath().substring(1, userDirURL.getPath().lastIndexOf("/"));
		}
		catch (Exception exc) { fileExists = false; }
		blocksToDelete = new Vector<String>(); // Prepare information about deleted blocks
		blocksInModel = null;
		theMdlFileStatic=theMdlFile; 		
	}


	/**
	 * Checks if the file is a Simulink application
	 * @param filename the file
	 * @return boolean
	 */
	/*
	private boolean isSimulinkFile (String filename) {
		filename = filename.toLowerCase().trim();
		if (filename.endsWith(".mdl")) return true;
		return false;
	}
	*/

	/**
	 * Generates code of the client application
	 * @param _type type of code to be added
	 * @return StringBuffer the code
	 */
	/*
public StringBuffer generateCode (int _type) {
//if (_type==Editor.GENERATE_JARS_NEEDED) return new StringBuffer("ejsExternal.jar;");
if (_type==Editor.GENERATE_DECLARATION) return new StringBuffer("org.colos.ejs.externalapps.matlab.lsimulink.EjsSimulink.class");
//if (_type==Editor.GENERATE_RESOURCES_NEEDED) return new StringBuffer(mdlFile+";_library/EjsIcon.jpg;"); //Gonzalo 090617
return new StringBuffer();
}
	 */
	/**
	 * Checks if the application was lunched
	 * @return boolean
	 */
	//private boolean fileExists () { return fileExists; }



	/**
	 * Process the editor
	 */
	/*
	private String saveString () {
		String code = "";
		for (Enumeration<String> e = blocksToDelete.elements(); e.hasMoreElements();) {
			String blockName = e.nextElement();
			if (blocksInModel==null) code += blockName + SEPARATOR;
			else if (blocksInModel.contains(blockName)) code += blockName + SEPARATOR;
		}
		return code;
	}
*/
	/**
	 * Process the editor
	 */
	/*
	private void readString (String _code) {
		int pos = _code.indexOf(SEPARATOR);
		while (pos>=0) {
			String block = _code.substring(0,pos);
			_code = _code.substring(pos+SEPARATOR.length());
			blocksToDelete.add(block);
			pos = _code.indexOf(SEPARATOR);
		}
	}
*/


	/**
	 * Process the editor
	 */
	private String edit (String _property, String _name, JComponent _target, String _value) {
		if (!fileExists) return null;
		if (!loadMatlab()) return null;
		timer = new javax.swing.Timer (20,this);
		createTheInterface();
		//currentBrowser = this;
		queryModel();
		processList(null);
		currentBlockHandle=Double.NaN;
		setCurrentValue (_value);
		dialog.setLocationRelativeTo (_target);
		// dialog.setTitle (res.getString("EditorForVariables.Title"+_property));
		dialog.setTitle ("List of external variables");	
		variableLabel.setText("Connected to model variable ="+" "+_name);
		//variableLabel.setText(res.getString("SimulinkBrowser.VariableLabel")+" "+_name);
		dialog.setVisible (true);
		timer.stop();
		if (systemIsOpen()) {
			matlab.eval("Ejs_Subsystem=find_system('open','on')");
			matlab.eval("for Ejs_index=1:length(Ejs_Subsystem), try, set_param(Ejs_Subsystem{Ejs_index},'open','off'), catch, end, end ");
		}
		// timer.start();
		blockCB.setSelected(false);
		blockCombo.setEnabled(true);
		return returnValue;
	}



	/**
	 * Adds intitial commands to start the external application
	 * @return Vector<String>
	 */
	/*
	private Vector<String> prepareInitCode(){    
		createTheInterface(); // actually it is addToInitCode which needs it
		Vector<String> lines = new Vector<String>();
		// Add the block deletion commands for this model
		for (Enumeration<String> e = blocksToDelete.elements(); e.hasMoreElements();) {
			String blockName = e.nextElement();
			if (blocksInModel!=null && !blocksInModel.contains(blockName)) continue;			 
			lines.add("deleteBlock(\""+theMdlFile+"/"+blockName+"\")"); 
			//lines.add("setInitCommand(new String[]{\"delete\",\""+theMdlFile+"/"+blockName+"\"})"); 
		}
		return lines;
	}
*/

	/**
	 * Adds code to the client to initiate the external application
	 * @param _name String client variable
	 * @param _connected String external variable
	 * @param _value double initial value of the client variable
	 * @return Vector<String>
	 */
	private Vector<String> addToInitCode (String _name, String _connected, double _value) {
		setCurrentValue(_connected);
		Vector<String> lines = new Vector<String>();    
		for (int i=0,n=modelInput.getSize(); i<n; i++) {
			String oneConnection = ((String) modelInput.get(i)).trim();
			int begin = oneConnection.indexOf('('), end = oneConnection.lastIndexOf(')');
			if (begin<0) ; // It is an only output var from the model (actually there is no such variable)
			else {
				String block = oneConnection.substring(begin+1,end), position = oneConnection.substring(5,begin); // 5 = "input"
				if (block.endsWith(INTEGRATOR_BLOCK)) block = block.substring(0,block.length()-INTEGRATOR_BLOCK.length());
				if (blocksInModel!=null && !blocksInModel.contains(block)) continue;
				//lines.add("linkVariables(new String[]{\""+_name+"\",\""+theMdlFile+"/"+block+"\",\"in\",\""+position+"\"})");
				lines.add("linkVariables(\""+_name+"\",\""+theMdlFile+"/"+block+"@in@"+position+"\");");
			}
		}
		for (int i=0,n=modelOutput.getSize(); i<n; i++) {
			String oneConnection = ((String) modelOutput.get(i)).trim();
			int begin = oneConnection.indexOf('('), end = oneConnection.lastIndexOf(')');
			if (begin<0) ; // It is an only output var from the model
			else {
				String block = oneConnection.substring(begin+1,end), position = oneConnection.substring(6,begin); // 6 = "output"
				if (block.endsWith(INTEGRATOR_BLOCK)) block = block.substring(0,block.length()-INTEGRATOR_BLOCK.length());
				if (blocksInModel!=null && !blocksInModel.contains(block)) continue;
				//lines.add("linkVariables(new String[]{\""+_name+"\",\""+theMdlFile+"/"+block+"\",\"out\",\""+position+"\"})");
				lines.add("linkVariables(\""+_name+"\",\""+theMdlFile+"/"+block+"@out@"+position+"\");");
			}
		}
		for (int i=0,n=modelParameters.getSize(); i<n; i++) {
			String auxLine="";
			String oneConnection = ((String) modelParameters.get(i)).trim();
			int begin = oneConnection.indexOf('('), end = oneConnection.lastIndexOf(')');
			String parameter;
			if (begin<0) {
				parameter = oneConnection;                
				//auxLine="linkVariables(new String[]{\""+_name+"\",\""+theMdlFile+"\",";
				auxLine="linkVariables(\""+_name+"\",\""+theMdlFile+"@";
				//lines.add("variables.path{end+1,1}='"+theMdlFile+"';");
			}
			else {
				String block = oneConnection.substring(begin+1,end);
				if (block.endsWith(INTEGRATOR_BLOCK)) block = block.substring(0,block.length()-INTEGRATOR_BLOCK.length());
				if (blocksInModel!=null && !blocksInModel.contains(block)) continue;
				parameter = oneConnection.substring(0,begin);
				//auxLine="linkVariables(new String[]{\""+_name+"\",\""+theMdlFile+"/"+block+"\",";
				auxLine="linkVariables(\""+_name+"\",\""+theMdlFile+"/"+block+"@";
			}
			//lines.add(auxLine+"\"param\",\""+parameter+"\"})");
			lines.add(auxLine+"param@"+parameter+"\");");
		}
		return lines;
	}


/*
	private boolean isInputVariable (String _connected) {
		int pos;
		do {
			pos = _connected.indexOf(SEPARATOR);
			String variable;
			if (pos<0) variable = _connected;
			else { variable = _connected.substring(0,pos); _connected = _connected.substring(pos+SEPARATOR.length()); }
			if (variable.startsWith("input")) return true;
			if (variable.startsWith("output")) continue;
			if (variable.equals("time")) continue;
			return true; // a parameter
		} while (pos>=0);
		return false;
	}
*/
	
	/*
	private boolean isOutputVariable (String _connected) {
		int pos;
		do {
			pos = _connected.indexOf(SEPARATOR);
			String variable;
			if (pos<0) variable = _connected;
			else { variable = _connected.substring(0,pos); _connected = _connected.substring(pos+SEPARATOR.length()); }
			if (variable.startsWith("output")) return true;
			if (variable.equals("time")) return true;
			if (variable.startsWith("input")) continue;
			// continue; // a parameter
		} while (pos>=0);
		return false;
	}

*/

	/**
	 * Extracts the connections for the variable from the
	 * String and fills in the corresponding lists
	 * @param _value String
	 */
	private void setCurrentValue (String _value) {
		modelInput.removeAllElements();
		modelOutput.removeAllElements();
		modelParameters.removeAllElements();
		_value = _value.trim();
		if (_value.length()<=0) return;
		int pos;
		do {
			pos = _value.indexOf(SEPARATOR);
			String variable;
			if (pos<0) variable = _value;
			else { variable = _value.substring(0,pos); _value = _value.substring(pos+SEPARATOR.length()); }
			if (variable.startsWith("input")) modelInput.addElement(variable);
			else if (variable.startsWith("output")) modelOutput.addElement(variable);
			// 'time' or a parameter
			//  else if (variable.equals("time")) modelOutput.addElement(variable); //Gonzalo 090616
			else modelParameters.addElement(variable);
		} while (pos>=0);
	}

	/**
	 * Constructs a single string with the info of the connections established
	 * @return String
	 */
	private String finalValue () {
		String txt = "";
		int n;
		if ((n=modelInput.getSize())>0)      for (int i=0; i<n; i++) txt += (String) modelInput.get(i) + SEPARATOR;
		if ((n=modelOutput.getSize())>0)     for (int i=0; i<n; i++) txt += (String) modelOutput.get(i) + SEPARATOR;
		if ((n=modelParameters.getSize())>0) for (int i=0; i<n; i++) txt += (String) modelParameters.get(i) + SEPARATOR;
		if (txt.endsWith(SEPARATOR)) txt = txt.substring(0,txt.length()-SEPARATOR.length());
		return txt;
	}

	/**
	 * Constructs the interface to process the editor
	 */
	private void createTheInterface () {
		if (dialog!=null) return;
		java.awt.event.ActionListener comboListener = new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				String command = evt.getActionCommand();
				if (command.equals("blockCombo")) {
					String blockSelected = (String) blockCombo.getSelectedItem();
					if (blockComboActive) {
						if (blockSelected.equals(MAIN_BLOCK)) processList(null);
						else {
							getBlockVariables("block='" + theMdlFile + "/" + blockSelected + "';\n");
						}
						currentBlockHandle=Double.NaN;
					}
				}
				else if (command.equals("showHide")) {
					if (blockCB.isSelected()) {
						if (!systemIsOpen()) openSystem();
						//matlab.engEvalString(id, "set_param (gcs, 'Lock','Off')");

						//Gonzalo 060629
						matlab.eval("set_param ('"+theMdlFile+"' , 'Open','On')");

						//matlab.engEvalString(id, "set_param (gcs, 'Lock','On')");

						blockCombo.setEnabled(false);
						timer.start();
					}
					else {
						timer.stop();
						blockCombo.setEnabled(true);
						matlab.eval("Ejs_Subsystem=find_system('open','on')");
						matlab.eval("for Ejs_index=1:length(Ejs_Subsystem), try, set_param(Ejs_Subsystem{Ejs_index},'open','off'), catch, end, end ");
					}
				}
			}
		};

		java.awt.event.MouseAdapter mouseListener = new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				if (evt.getSource() instanceof JList) {
					if (evt.getClickCount() > 1) {
						JList list = (JList) evt.getSource();
						String selection = (String) list.getSelectedValue();
						if (selection==null) return;
						String block;
						if (MAIN_BLOCK.equals(currentBlockName)) block = "";
						else {
							block = currentBlockName;
							if (currentBlockIsIntegrator) block += INTEGRATOR_BLOCK;
							block = "("+block+")";
						}
						// Depends on which list was chosen
						if (list == listBlockInput && !modelInput.contains(selection)) modelInput.addElement(selection+block);
						else if (list == listBlockOutput) { modelOutput.removeAllElements(); modelOutput.addElement(selection+block); }
						else if (list == listBlockParameters && !modelParameters.contains(selection)) modelParameters.addElement(selection+block);
						else if (list == listVariableInput && modelInput.contains(selection)) modelInput.removeElement(selection);
						else if (list == listVariableOutput) modelOutput.removeAllElements();
						else if (list == listVariableParameters && modelParameters.contains(selection)) modelParameters.removeElement(selection);
						//hasChanged = true;
					}
					return;
				}
				AbstractButton button = (AbstractButton) (evt.getSource());
				String aCmd = button.getActionCommand();
				if (aCmd.equals("ok")) {
					returnValue = finalValue();
					dialog.setVisible(false);
				}
				else if (aCmd.equals("cancel")) {
					returnValue = null;
					dialog.setVisible(false);
				}
				else if (aCmd.equals("deleteBlock")) {
					if (!MAIN_BLOCK.equals(currentBlockName)) {
						if (!blocksToDelete.contains(currentBlockName)) blocksToDelete.add(currentBlockName);
						blockDeleteButton.setEnabled(false);
						blockUndeleteButton.setEnabled(true);
						blockCombo.setForeground(Color.gray);
						listBlockInput.setEnabled(false); listBlockOutput.setEnabled(false); listBlockParameters.setEnabled(false);
						//hasChanged = true;
					}
				}
				else if (aCmd.equals("undeleteBlock")) {
					if (!MAIN_BLOCK.equals(currentBlockName)) {
						if (blocksToDelete.contains(currentBlockName)) blocksToDelete.remove(currentBlockName);
						blockDeleteButton.setEnabled(true);
						blockUndeleteButton.setEnabled(false);
						blockCombo.setForeground(Color.black);
						listBlockInput.setEnabled(true); listBlockOutput.setEnabled(true); listBlockParameters.setEnabled(true);
						//hasChanged = true;
					}
				}
			}
		};

		JButton okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addMouseListener(mouseListener);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addMouseListener(mouseListener);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		blockCB = new JCheckBox("Block =");
		blockCB.setActionCommand("showHide");
		blockCB.addActionListener(comboListener);

		blockLabel = new JLabel("Block =");
		blockCombo = new JComboBox();
		blockCombo.setActionCommand("blockCombo");
		blockCombo.addActionListener(comboListener);
		blockComboDefColor = blockCombo.getForeground();
		blockCombo.setRenderer(new BrowserForSimulinkCR());
		blockDeleteButton = new JButton("Delete");
		blockDeleteButton.setActionCommand("deleteBlock");
		blockDeleteButton.addMouseListener(mouseListener);
		blockUndeleteButton = new JButton("Undelete");
		blockUndeleteButton.setActionCommand("undeleteBlock");
		blockUndeleteButton.addMouseListener(mouseListener);

		JPanel blocksLabelPanel = new JPanel(new java.awt.BorderLayout());
		blocksLabelPanel.add(blockCB, java.awt.BorderLayout.WEST);
		blocksLabelPanel.add(blockLabel, java.awt.BorderLayout.CENTER);

		JPanel blocksDeletePanel = new JPanel(new java.awt.GridLayout(1,2));
		blocksDeletePanel.add(blockDeleteButton);
		blocksDeletePanel.add(blockUndeleteButton);

		JPanel blockPanelUp = new JPanel(new java.awt.BorderLayout());
		blockPanelUp.add(blockCB, java.awt.BorderLayout.WEST);
		blockPanelUp.add(blockCombo, java.awt.BorderLayout.CENTER);
		blockPanelUp.add(blocksDeletePanel, java.awt.BorderLayout.EAST);

		listBlockInput = new JList();
		listBlockInput.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listBlockInput.addMouseListener(mouseListener);
		listBlockInput.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		JPanel panelBlockInput = new JPanel(new java.awt.BorderLayout());
		panelBlockInput.add(new JLabel ("Input",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelBlockInput.add(new JScrollPane(listBlockInput),java.awt.BorderLayout.CENTER);

		listBlockOutput = new JList();
		listBlockOutput.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listBlockOutput.addMouseListener(mouseListener);
		listBlockOutput.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		JPanel panelBlockOutput = new JPanel(new java.awt.BorderLayout());
		panelBlockOutput.add(new JLabel ("Output",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelBlockOutput.add(new JScrollPane(listBlockOutput),java.awt.BorderLayout.CENTER);

		listBlockParameters = new JList();
		listBlockParameters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listBlockParameters.addMouseListener(mouseListener);
		listBlockParameters.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		JPanel panelBlockParameters = new JPanel(new java.awt.BorderLayout());
		panelBlockParameters.add(new JLabel ("Parameters",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelBlockParameters.add(new JScrollPane(listBlockParameters),java.awt.BorderLayout.CENTER);

		JPanel blockPanelCenter = new JPanel(new java.awt.GridLayout(1,3));
		blockPanelCenter.add(panelBlockInput);
		blockPanelCenter.add(panelBlockOutput);
		blockPanelCenter.add(panelBlockParameters);

		JPanel blockPanel = new JPanel (new java.awt.BorderLayout());
		blockPanel.add (blockPanelUp,java.awt.BorderLayout.NORTH);
		blockPanel.add (blockPanelCenter,java.awt.BorderLayout.CENTER);

		listVariableInput = new JList();
		listVariableInput.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listVariableInput.addMouseListener(mouseListener);
		listVariableInput.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		listVariableInput.setModel(modelInput);
		JPanel panelVariableInput = new JPanel(new java.awt.BorderLayout());
		panelVariableInput.add(new JLabel ("Input",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelVariableInput.add(new JScrollPane(listVariableInput),java.awt.BorderLayout.CENTER);

		listVariableOutput = new JList();
		listVariableOutput.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listVariableOutput.addMouseListener(mouseListener);
		listVariableOutput.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		listVariableOutput.setModel(modelOutput);
		JPanel panelVariableOutput = new JPanel(new java.awt.BorderLayout());
		panelVariableOutput.add(new JLabel ("Output",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelVariableOutput.add(new JScrollPane(listVariableOutput),java.awt.BorderLayout.CENTER);

		listVariableParameters = new JList();
		listVariableParameters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listVariableParameters.addMouseListener(mouseListener);
		listVariableParameters.setFont(InterfaceUtils.font(null,"Editor.DefaultFont"));
		listVariableParameters.setModel(modelParameters);
		JPanel panelVariableParameters = new JPanel(new java.awt.BorderLayout());
		panelVariableParameters.add(new JLabel ("Parameters",SwingConstants.CENTER),java.awt.BorderLayout.NORTH);
		panelVariableParameters.add(new JScrollPane(listVariableParameters),java.awt.BorderLayout.CENTER);

		variableLabel = new JLabel("Connected to model variable =");

		JPanel variablePanelCenter = new JPanel(new java.awt.GridLayout(1,3));
		variablePanelCenter.add(panelVariableInput);
		variablePanelCenter.add(panelVariableOutput);
		variablePanelCenter.add(panelVariableParameters);

		JPanel variablePanel = new JPanel (new java.awt.BorderLayout());
		variablePanel.add (variableLabel,java.awt.BorderLayout.NORTH);
		variablePanel.add (variablePanelCenter,java.awt.BorderLayout.CENTER);

		JPanel centerPanel = new JPanel (new java.awt.GridLayout(2,1));
		centerPanel.add (blockPanel);
		centerPanel.add (variablePanel);

		JSeparator sep1 = new JSeparator (SwingConstants.HORIZONTAL);

		JPanel bottomPanel = new JPanel (new java.awt.BorderLayout());
		bottomPanel.add (sep1,java.awt.BorderLayout.NORTH);
		bottomPanel.add (buttonPanel,java.awt.BorderLayout.SOUTH);

		dialog = new JDialog();
		dialog.getContentPane().setLayout (new java.awt.BorderLayout(0,0));
		dialog.getContentPane().add (centerPanel,java.awt.BorderLayout.CENTER);
		dialog.getContentPane().add (bottomPanel,java.awt.BorderLayout.SOUTH);

		dialog.addWindowListener (
				new java.awt.event.WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent event) {
						timer.stop();
						if (systemIsOpen()) {
							matlab.eval("Ejs_Subsystem=find_system('open','on')");
							matlab.eval("for Ejs_index=1:length(Ejs_Subsystem), try, set_param(Ejs_Subsystem{Ejs_index},'open','off'), catch, end, end ");
						}
						returnValue = null;
					}
				}
		);

		dialog.setSize(500,350);
		dialog.validate();
		dialog.setModal(true);
	}



	private class BrowserForSimulinkCR extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
		public BrowserForSimulinkCR() { setOpaque(true); }
		public Component getListCellRendererComponent(JList list,Object value,int index,
				boolean isSelected,boolean cellHasFocus) {
			setText(value.toString());
			setForeground(blocksToDelete.contains(value) ? Color.gray : blockComboDefColor);
			if (isSelected) setBackground(list.getSelectionBackground());
			else setBackground(list.getBackground());
			return this;
		}
	}


	/**
	 * Processes a String obtained from Matlab with the variables of a given block
	 * @param _vars String
	 */
	private void processList (String _vars) {
		Vector<String> inputList = new Vector<String>(), outputList = new Vector<String>(), parameterList = new Vector<String>();
		if (_vars==null || _vars.trim().length()<=0) {
			_vars = MAIN_BLOCK + "_ejs_" + MAIN_PARAMETERS;
			currentBlockHandle = Double.NaN;
		}
		originalBlockName = _vars.substring(0,_vars.indexOf("_ejs_"));
		String blockName = correctBlockName(originalBlockName);
		_vars = _vars.substring(blockName.length()+5);
		currentBlockName = blockName;
		blockComboActive=false;
		blockCombo.setSelectedItem(blockName);
		blockComboActive=true;
		if (blockName.equals(MAIN_BLOCK)) {
			blockDeleteButton.setEnabled(false);
			blockUndeleteButton.setEnabled(false);
			blockCombo.setForeground(Color.black);
			listBlockInput.setEnabled(true); listBlockOutput.setEnabled(true); listBlockParameters.setEnabled(true);
		}
		else {
			boolean deleted = blocksToDelete.contains(blockName);
			blockDeleteButton.setEnabled(!deleted);
			blockUndeleteButton.setEnabled(deleted);
			blockCombo.setForeground(deleted ? Color.gray : Color.black);
			listBlockInput.setEnabled(!deleted); listBlockOutput.setEnabled(!deleted); listBlockParameters.setEnabled(!deleted);
		}
		StringTokenizer tkn = new StringTokenizer (_vars,";");
		int type = 0; // Input
		while (tkn.hasMoreTokens()) {
			String listStr = tkn.nextToken();
			if (listStr.startsWith("Output")) type = 1;
			else if (listStr.startsWith("Parameters")) type = 2;
			else type = 0;
			listStr = listStr.substring(listStr.indexOf('=')+1);
			StringTokenizer tkn2 = new StringTokenizer (listStr,",");
			while (tkn2.hasMoreTokens()) {
				switch (type) {
				case 1 : outputList.add(tkn2.nextToken()); break;
				case 2 : parameterList.add(tkn2.nextToken()); break;
				default :
				case 0 : inputList.add(tkn2.nextToken()); break;
				}
			}
		}
		listBlockInput.setListData(inputList);
		listBlockOutput.setListData(outputList);
		listBlockParameters.setListData(parameterList);
	}


	private boolean loadMatlab () {
		if (matlab!=null) {	
//			try{
				matlab.eval("");
				return true;
//			}catch(jmatlink.JMatLinkException e){
//				System.out.println("Open Matlab again");
//			}		    			    	
		}

		JFrame frame = new JFrame("Loading Matlab. Please wait");
		java.awt.Image image = org.opensourcephysics.tools.ResourceLoader.getImage("data/icons/EjsIcon.gif");    
		if (image!=null) frame.setIconImage(image);
		frame.getContentPane().setLayout (new java.awt.BorderLayout ());
		frame.getContentPane().add(new JLabel("Loading Matlab. Please wait",SwingConstants.CENTER), BorderLayout.CENTER);

		//Dimension size = res.getDimension("Osejs.StartDialogSize");

		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//frame.setSize(size);
		frame.validate();
		//frame.setLocation((screenSize.width-size.width)/2,(screenSize.height-size.width)/2);
		frame.setVisible(true);
		String dir = System.getProperty("user.home").replace('\\','/');
		if (!dir.endsWith("/")) dir += "/";
		if (org.opensourcephysics.display.OSPRuntime.isWindows()) {   
			matlab = new es.uned.dia.interoperate.matlab.jimc.MatlabExternalApp();  // create a JMatLink in Windows
			matlab.connect();
		}
		/*
		//if (matlab==null) JOptionPane.showMessageDialog(null, res.getString("Osejs.File.Error"),res.getString("BrowserForSimulink.ConnectionError"), JOptionPane.INFORMATION_MESSAGE);    		
		if (1==2) ;
		else {
			matlab.eval ("cd ('" + userDir + "')");
		}
		*/
		matlab.eval ("cd ('" + userDir + "')");
		frame.dispose();
		return matlab!=null;
	}

	/*
	private void exitMatlab () {
		if (matlab!=null) {
			matlab.eval("Ejs_Subsystem=find_system('type','block_diagram')"); //Gonzalo 060704 close systems without saving
			matlab.eval("for Ejs_index=1:length(Ejs_Subsystem), try, close_system(Ejs_Subsystem{Ejs_index},0), catch, end, end");
			matlab.disconnect();
		}
		matlab = null;
	}
	*/
	

	private void openSystem () {
		matlab.eval("open_system ('" + mdlFile+ "')");
		matlab.eval( "set_param (gcs, 'SimulationCommand','Stop')");
		matlab.eval("set_param (gcs, 'StopTime','inf')");
		matlab.eval( "Ejs_Subsystem=find_system('open','on')");
		matlab.eval( "for Ejs_index=1:length(Ejs_Subsystem), try, set_param(Ejs_Subsystem{Ejs_index},'open','off'), catch, end, end ");
		matlab.eval("variables=['']; blockList=[''];");
	}


	private void queryModel () {	
		if (!systemIsOpen()) openSystem();
		blockComboActive = false;
		blockCombo.removeAllItems();
		blockCombo.addItem(MAIN_BLOCK);

		matlab.eval("handle='"+theMdlFileStatic+"'");		    
		//EjsMatlab.waitForString (matlab,id,"handle","handle='"+theMdlFileStatic+"'");  // Gonzalo 060629

		matlab.eval(blockListCommand);
		String blockList = matlab.getString("blockList");		    
		//String blockList = EjsMatlab.waitForString (matlab,id,"blockList",blockListCommand);
		blocksInModel = new Vector<String>();
		if (blockList==null) { processList(null); return; }
		int pos = blockList.indexOf("_ejs_");
		while (pos>=0) {
			String piece = correctBlockName(blockList.substring(0,pos));
			blockCombo.addItem(piece);
			blocksInModel.add(piece);
			blockList = blockList.substring(pos+5);
			pos = blockList.indexOf("_ejs_");
		}
		blockComboActive = true;
	}   

	private boolean systemIsOpen() {
		matlab.eval("clear currentSystem;");			    
		matlab.eval ("currentSystem=gcs");
		//EjsMatlab.waitForString (matlab,id,"currentSystem","currentSystem=gcs");			    		    
		matlab.eval("clear isOPEN;");			   
		matlab.eval("try, isOPEN=get_param('"+theMdlFile +"','open'),catch,isOPEN='off',end");		    
		String isOpen = matlab.getString("isOPEN");		    
		//String isOpen = EjsMatlab.waitForString (matlab,id,"isOPEN","try, isOPEN=get_param('"+currentBrowser.theMdlFile +"','open'),catch,isOPEN='off',end");			    
		if (isOpen.equalsIgnoreCase("on")) return true;
		return false;
	}


	private void getBlockVariables (String prefix) {
		if (!systemIsOpen()) {
			openSystem();
			if (!blockCB.isSelected()) {
				matlab.eval("set_param (gcs, 'Open','Off')");
			}
		}
		matlab.eval(prefix+updateCommand);
		processList(matlab.getString("variables"));
		//processList(EjsMatlab.waitForString (matlab,id,"variables",prefix+updateCommand));			    
		currentBlockIsIntegrator = (matlab.getDouble("isIntegrator")!=0);
	}


	private String correctBlockName (String _name) {
		String correctName = "";
		StringTokenizer tkn = new StringTokenizer (_name.replace('\n',' '), "/",true);
		while (tkn.hasMoreTokens()) {
			String piece = tkn.nextToken();
			//		    if (piece.equals("/")) correctName += "//";
			if (piece.equals("/")) correctName += "/";  // Gonzalo 060420
			else correctName += piece;
		}
		return correctName;
	}

	// --------------------------
	// Elaborate Matlab commands
	// --------------------------

	private final String blockListCommand =
		// "handle=gcs;\n" // Gonzalo 060629
		//    + "blocks=get_param(handle,'blocks');\n"
		"blocks=find_system(handle,'type','block');\n" // Gonzalo 060420
		+ "for ik=1:length(blocks) aux=blocks{ik}; blocks{ik}=aux(length(handle)+2:end); end;\n" // Gonzalo 060420
		+ "blockList=[''];\n"
		+ "for i=1:size(blocks,1) blockList=[blockList,char(blocks(i,:)),'_ejs_']; end;\n";

	private final String updateCommand =
		// "name=get_param(block,'name');\n"
		"name=block;\n" // Gonzalo 060420
		+ "inportnumbers=size(get_param(block,'InputPorts'),1);\n"
		+ "in_mess='Input=';\n"
		+ "for i=1:inportnumbers in_mess=[in_mess,'input',num2str(i),',']; end;\n"
		+ "if strcmp(in_mess(end),',') in_mess(end)=''; end;\n"
		+ "outportnumbers=size(get_param(block,'OutputPorts'),1);\n"
		+ "out_mess='Output=';\n"
		+ "for i=1:outportnumbers out_mess=[out_mess,'output',num2str(i),',']; end;\n"
		+ "if strcmp(out_mess(end),',') out_mess(end)=''; end;\n"
		+ "parameters=get_param(block,'DialogParameters');\n"
		+ "nameparams=char(fieldnames(parameters));\n"
		+ "param_mess=['Parameters='];\n"
		+ "for i=1:size(nameparams,1) param_mess=[param_mess,deblank(nameparams(i,:)),',']; end;\n"
		+ "if strcmp(param_mess(end),',') param_mess(end)=''; end;\n"
		+ "name=name(length(handle)+2:end);\n"
		+ "variables=[name,'_ejs_',in_mess,';',out_mess,';',param_mess];"
		+ "blocktype=get_param(block,'BlockType'); \n"
		+ "isIntegrator = strcmp(deblank(blocktype),'Integrator');";


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

