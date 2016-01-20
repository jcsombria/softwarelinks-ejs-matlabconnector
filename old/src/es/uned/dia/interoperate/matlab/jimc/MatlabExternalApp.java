package es.uned.dia.interoperate.matlab.jimc;

import es.uned.dia.interoperate.ExternalApp;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

/**
 * Class to implement the communication protocol with MATLAB
 * @author <a href="mailto:gfarias@bec.uned.es">Gonzalo Farias</a>
 * @author <a href="mailto:jchacon@bec.uned.es">Jesús Chacón</a> 
 *
 */
public class MatlabExternalApp implements ExternalApp {
	protected class Context {
		private Object client = null;

		public Context(Object client) {
			this.client = client;
		}
		
		public boolean hasField(String name) {
			Field field = getField(name);
			return (field != null);
		}

		public Field getField(String name) {
			try {
				Field field = client.getClass().getField(name);
				return field;
			} catch(SecurityException e) {
				System.err.println("Access denied to field "+name);
			} catch(NoSuchFieldException e) {
				System.err.println("No such field: "+name);
			}
			return null;
		}
		
		public Object get(String variable) {
			try {
				Field field = getField(variable);
				return field.get(client);
			} catch(IllegalAccessException e) {
				System.err.println("Access denied to field "+variable);
			}
			return null;
		}
		
		public void set(String variable, Object value) {
			try {
				Field field = getField(variable);
				field.set(client, value);
			} catch(IllegalAccessException e) {
				System.err.println("Access denied to field "+variable);
			}
		}
	}

	protected interface Link {
		public String getNameOfClientVariable();
		public String getNameOfExternalVariable();
	}
	
	protected class MatlabLink implements Link {
		private String variable;
		private String external;

		public MatlabLink(String variable, String external) {
			this.variable = variable;
			this.external = external;
		}
		
		public String getNameOfClientVariable() {
			return this.variable;
		}

		public String getNameOfExternalVariable() {
			return this.external;
		}
	}

	protected long id=-1;   // The integer id for the Matlab engine opened  
	protected String initCommand = null; // An optional initialization command required for the correct reset

	boolean startRequired = true; // Whether the simulation needs to be started
	boolean waitForEverFlag = false; //Flag to wait for a variable
	protected String userDir = null; // The users working directory for this instance
	static String pathOfJimc = null;
	static String pathOfJmatlink = null;
	static String temporalDirectory = null;
	public String commandUpdate = " "; 
	protected String resultOfLastAction = "";// A String with the result of the last method executed 

	protected Context context = null; // The client of the external application
	protected java.util.Map<String, Link> links = null;
	
	private MatlabProxyFactory proxyFactory;
	protected MatlabProxy proxy = null;
	private boolean hideMatlab = true;

	public MatlabExternalApp () {
		this(false);
	}

	public MatlabExternalApp (boolean hide) {
		hideMatlab = hide;
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
												.setHidden(hideMatlab)
												.build();
		proxyFactory = new MatlabProxyFactory(options);
	}

	/**
	 * Starts the connection with the external application  
	 * @return boolean true if the connection was successful
	 */    
	public boolean connect(){
		try {
			proxy = proxyFactory.getProxy();
			java.net.URI uribase = new java.net.URI(getBaseDirectory());
			//File efilebase = new File(uribase);
			String currentPath = System.getProperty("user.dir");
			setCurrentPathMatlab(currentPath);
		} catch(java.net.URISyntaxException | MatlabConnectionException e) {
			e.printStackTrace();
		}
		return proxy.isConnected();
	}
	
	protected void setCurrentPathMatlab(String dir) {
		try {
			proxy.eval("jim___str='"+"cd (''"+dir+"'')';");
			proxy.eval("jim___bytes=unicode2native(jim___str, 'ISO-8859-1')");
			proxy.eval("jim___str=native2unicode(jim___bytes, 'UTF-8')");
			proxy.eval("eval(jim___str)");
			proxy.eval("clear jim___str jim___bytes");
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}

	protected String getBaseDirectory(){
		String pathOfJimc2 = this.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
		pathOfJimc2 = pathOfJimc2.substring(0, pathOfJimc2.lastIndexOf("/"));  
		return pathOfJimc2;
	}

	/**
	 * Finishes the connection with the external application     
	 */         
	public void disconnect() {
		if(!proxy.isConnected()) return;
		try {
			proxy.eval("bdclose ('all')");
			proxy.exit();
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}
	 
	/**
	 * Checks if the connection was successful
	 * @return boolean true if the connection was successful
	 */
	public boolean isConnected(){
		return proxy.isConnected();
	} 

	/**
	 * Accepts an initialization command to use whenever the system is reset
	 * @param command String
	 */
	public void setInitCommand (String command){
		initCommand = command.trim();
		initialize();
	}

	/**
	 * Gets the initialization command
	 * @return String the initial command
	 */ 
	public String getInitCommand(){
		return initCommand;
	}

	/**
	 * Sets the value of the given variable of the application
	 * @param variable String the variable name
	 * @param value Object the desired value
	 */
	public void set(String variable, Object value) {
		Link link = links.get(variable);
		boolean isValid = (link != null && context.hasField(variable));
		if(!isValid) {
			return; 
		}
		Field field = context.getField(variable);
		Class<?> type = field.getType();
		String external = link.getNameOfExternalVariable();		
		switch(type.getName()) {
		case "double":
			setValue(external, (double)value);
			break;
		case "[D":
			setValue(external, (double[])value);
			break;
		case "[[D":
			setValue(external, (double[][])value);
			break;
		case "string":
			setValue(external, (String)value);
			break;
		default:
		}
	}

	/**
	 * Gets the value of a variable of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	public Object get(String variable) {
		Link link = links.get(variable);
		boolean isValid = (link != null && context.hasField(variable));
		if(!isValid) {
			return null;
		}
		Field field = context.getField(variable);
		Class<?> type = field.getType();
		String external = link.getNameOfExternalVariable();		
		switch(type.getName()) {
		case "double":
			return getDouble(external);
		case "[D":
			return getDoubleArray(external);
		case "[[D":
			return getDoubleArray2D(external);
		case "string":
			return getString(external);
		default:
			return null;
		}
	}

	/**
	 * Sets the value of the given variable of the application
	 * @param variable String the variable name
	 * @param value String the desired value
	 */
	public void setValue(String variable, String value){
		if(!proxy.isConnected()) return;
		try {
			proxy.eval (variable + "= [" + value + "]");
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}
	 
	/**
	 * Gets the value of a String variable of the application
	 * @param variable String the variable name
	 * @return String the value
	 */
	public String getString (String variable) { 
		if(!proxy.isConnected()) return null;
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		MatlabNumericArray array;
		double[][] arrayD = null;
		try {
			proxy.eval("EjsengGetCharArrayD=double(" + variable +")" );
			array = processor.getNumericArray("EjsengGetCharArrayD");
			arrayD = array.getRealArray2D(); 
			proxy.eval("clear EjsengGetCharArrayD");
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}		
		if (arrayD == null) return null;
		String value[] = double2String(arrayD);
		if (value.length <= 0) return null;
		return value[0];
	}
	 
	/**
	 * Sets the value of the given variable of the application
	 * @param variable String the variable name
	 * @param value double the desired value
	 */ 
	public void setValue(String variable, double value){
		if(!proxy.isConnected()) return;
		try {
			proxy.setVariable(variable, value);
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}
	 
	/**
	 * Gets the value of a double variable of the application
	 * @param variable String the variable name
	 * @return double the value
	 */
	public double getDouble (String variable){
		if(!proxy.isConnected()) return 0.0;
		double doubleToReturn = 0.0;
		try {
			doubleToReturn  = ((double[])proxy.getVariable(variable))[0];
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			doubleToReturn = 0.0;
		}
		return doubleToReturn;
	}

	/**
	 * Sets the value of the given variable of the application
	 * @param variable String the variable name
	 * @param value double[] the desired value
	 */
	public void setValue(String variable, double[] value){   
		if(!proxy.isConnected()) return;
		try {
			proxy.setVariable(variable, value);
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the value of a double[] variable of the application
	 * @param variable String the variable name
	 * @return double the value
	 */
	public double[] getDoubleArray (String variable){
		if(!proxy.isConnected()) return null;
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		MatlabNumericArray array;
		double[] arrayToReturn = null;
		try {
			array = processor.getNumericArray(variable);
			arrayToReturn = array.getRealArray2D()[0]; 
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
		return arrayToReturn;
	}

	/**
	 * Sets the value of the given variable of the application
	 * @param variable String the variable name
	 * @param value double[][] the desired value
	 */
	public void setValue(String variable, double[][] value){
		if(!proxy.isConnected()) return;
		 //matlabEng.engPutArray (id,variable,value);
	}

	/**
	 * Gets the value of a double[][] variable of the application
	 * @param variable String the variable name
	 * @return double the value
	 */
	public double[][] getDoubleArray2D (String variable){
		if(!proxy.isConnected()) return null;
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		MatlabNumericArray array;
		double[][] arrayToReturn = null;
		try {
			array = processor.getNumericArray(variable);
			arrayToReturn = array.getRealArray2D();
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
		return arrayToReturn;
	} 
  
	/**
	 * Evaluates a given command in the external application
	 * @param command String to be executed
	 */
	public void eval (String command){
		if(!proxy.isConnected()) return;
		try {
			proxy.eval(command);
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Resets the application
	 */
	public void reset(){
		if(!proxy.isConnected()) return;
		try {
			proxy.eval("bdclose ('all')");
			proxy.eval("clear all");  
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
//		initialize();
	}

	/**
	 * The result of last action can be read using
	 * this method.   
	 * @return String the result of last action
	 */  
	public String getActionResult(){
		return resultOfLastAction;
	}

	/**
	 * Sets the client application
	 * @param clientApp Object the client application. Reflection is used to access the variables in the client.
	 */
	public boolean setClient(Object clientApp){
		if (clientApp==null) return (false);
		context = new Context(clientApp);
		links = new java.util.HashMap<>();
		return (true);
	 }

	/**
	 * Links a client variable with a variable of the external application
	 * @param clientVariable String the client variable
	 * @param externalVariable String the external variable
	 */
	public boolean linkVariables(String clientVariable, String externalVariable){
		if(context.hasField(clientVariable)) {
			Link link = new MatlabLink(clientVariable, externalVariable);
			links.put(clientVariable, link);
			return true;
		}
		return false;
	}

	/**
	 * Clears all linking between variables
	 */
	public void clearLinks(){
		context = null;
		links = null;
	}
 
	/**
	 * Some external applications, such as MATLAB, can continuously execute  
	 * a command after every step().
	 * @param command a String to be executed
	 */
	public void setCommand(String command){
		commandUpdate=command;
	}

	/**
	 * Gets the command to be executed by the external application.  
	 * @return String the command
	 */ 
	public String getCommand(){
		return commandUpdate;
	}

	/**
	 * Steps the application a given step or a number of times.
	 * If getCommand() is non-null the command is executed that number of times.
	 * If getCommand() is null, the dt parameter is passed down to the externall application,
	 * and the actual meaning of this parameter dt will depend on the implementing class
	 * @param dt double
	 */
	public void step (double dt){    
		setValues();
		//Do Step  
		int steps=(int)dt;
		for (int i=0;i<steps;i++)
			eval(commandUpdate);    
		getValues();        
	 }

	/**
	 * Synchronizes client and external applications
	 */
	public void synchronize(){       
	}
	 
	/**
	 * Sets the package size used to group values of the external variables
	 */
	public void setPackageSize(int size){
	}

	/**
	 * Empty the buffer
	 */
	public void flush(){
	}  
	 
	/**
	 * Forces or not to the client to wait until an external variable is read. 
	 * @param set boolean 
	 */
	public void setWaitForEver(boolean set){
		waitForEverFlag=set;
	}
	 
	/**
	 * sets all external variables with client values
	 */
	public void setValues() {
		for(Link link: links.values()) {
			Object value = context.get(link.getNameOfClientVariable());
			set(link.getNameOfClientVariable(), value);
		}
	}

	/**
	 * gets all external values
	 */
	public void getValues() {
		for(Link link: links.values()) {
			Object value = get(link.getNameOfClientVariable());
			context.set(link.getNameOfClientVariable(), value);
		}
	}

	/**
	 * Initializes a Matlab session. 
	 */
	protected void initialize () {
		if (initCommand!=null) {
			try {
				proxy.eval(initCommand);
			} catch (MatlabInvocationException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This queries Matlab until a given String is returned
	 * The string must exist (sooner or later) in Matlab or the computer will hang
	 * Strongly inspired in JMatLink
	 */
	static protected String[] double2String(double[][] d) {
		String encodeS[]=new String[d.length];

		for (int n=0; n<d.length; n++){
			byte b[] = new byte[d[n].length];
			for (int i=0; i<d[n].length ;i++) {
				b[i] = (byte)d[n][i];
			}

			// convert byte to String
			try {
				encodeS[n] = new String(b, "UTF8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return encodeS;
	}
}