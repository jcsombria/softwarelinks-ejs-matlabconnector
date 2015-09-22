/**
 * RemoteControlProtocolL1
 * author: Jesús Chacón <jcsombria@gmail.com>
 *
 * Copyright (C) 2014 Jesús Chacón
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.uned.dia.jcsombria.softwarelinks.matlab.client;

import es.uned.dia.jcsombria.softwarelinks.matlab.Context;
import es.uned.dia.jcsombria.softwarelinks.matlab.RemoteControlProtocolL1;
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
public class MatlabConnector implements RemoteControlProtocolL1 {
	private MatlabProxyFactory proxyFactory;
	protected MatlabProxy proxy = null;
	private boolean hideMatlab = true;
	protected Context context;
	
	/**
	 * Default Constructor  
	 */    
	public MatlabConnector () {
		this(true);
	}

	public Context getContext() {
		return context;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}

	public void setContext(Object context) {
		this.context = new Context(this, context);
	}

	public void getValues() {
		if(context != null) {
			context.getValues();
		}
	}
	
	public void setValues() {
		if(context != null) {
			context.setValues();
		}
	}

	/**
	 * @param hide if true Matlab is shown, otherwise is ran in background  
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
	@Override
	public boolean connect(){
		try {
			if(!isConnected()) {
				proxy = proxyFactory.getProxy();
			}
		} catch(MatlabConnectionException e) {
			System.err.println("Error connecting to Matlab.");
			System.err.println(e.getMessage());
		}
		return isConnected();
	}
	
	/**
	 * Checks if the connection was successful
	 * @return boolean true if the connection was successful
	 */
	public boolean isConnected(){
		return (proxy != null && proxy.isConnected());
	} 
	
	/**
	 * Finishes the connection with the external application     
	 * @return 
	 */         
	@Override
	public boolean disconnect() {
		if(!proxy.isConnected()) {
			return true;
		}
		try {
			proxy.eval("bdclose ('all')");
			proxy.exit();
			return true;
		} catch (MatlabInvocationException e) {
			System.err.println("Error disconnecting from Matlab.");
			System.err.println(e.getMessage());
		}
		return false;
	}
	
	/**
	 * Gets the value of a variable of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	@Override
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
	@Override
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
	@Override
	public boolean eval(String command){
		if(!proxy.isConnected()) {
			return false;
		}
		try {
			proxy.eval(command);
			return true;
		} catch (MatlabInvocationException e) {
			System.err.println("Error evaluating Matlab command: "+command);
			System.err.println(e.getMessage());
		}
		return false;
	}
}