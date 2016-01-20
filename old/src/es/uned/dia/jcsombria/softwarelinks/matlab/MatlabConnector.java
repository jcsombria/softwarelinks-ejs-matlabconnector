package es.uned.dia.jcsombria.softwarelinks.matlab;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * Class to implement the communication protocol with MATLAB
 * @author <a href="mailto:gfarias@bec.uned.es">Gonzalo Farias</a>
 * @author <a href="mailto:jchacon@bec.uned.es">Jesús Chacón</a> 
 *
 */
public class MatlabConnector {
	private MatlabProxyFactory proxyFactory;
	protected MatlabProxy proxy = null;
	private boolean hideMatlab = true;
	
	/**
	 * Default Constructor  
	 */    
	public MatlabConnector () {
		this(true);
	}

	/**
	 * @param hide if true Matlab is shown, otherwise is runned in background  
	 */    
	public MatlabConnector (boolean hide) {
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
		} catch(MatlabConnectionException e) {
			e.printStackTrace();
		}
		return proxy.isConnected();
	}
	
	/**
	 * Checks if the connection was successful
	 * @return boolean true if the connection was successful
	 */
	public boolean isConnected(){
		return proxy.isConnected();
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
	 * Gets the value of a variable of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	public Object get(String variable) {
		try {
			return proxy.getVariable(variable);
		} catch (MatlabInvocationException e) {
			System.err.println("Error getting Matlab variable: "+variable);
			System.err.println(e.getMessage());
		}
		return null;
	}


	/**
	 * Sets the value of a variable of the application
	 * @param variable String the variable name
	 * @param Object the value
	 */
	public void set(String variable, Object value) {
		try {
			proxy.setVariable(variable, value);
		} catch (MatlabInvocationException e) {
			System.err.println("Error setting Matlab variable: "+variable);
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Evaluates a given command in the external application
	 * @param command String to be executed
	 */
	public void eval(String command){
		if(!proxy.isConnected()) return;
		try {
			proxy.eval(command);
		} catch (MatlabInvocationException e) {
			e.printStackTrace();
		}
	}

}