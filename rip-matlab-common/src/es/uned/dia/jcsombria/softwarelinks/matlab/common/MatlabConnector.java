/**
 * MatlabConnector
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
package es.uned.dia.jcsombria.softwarelinks.matlab.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

/**
 * Class to implement the communication protocol with MATLAB
 * @author <a href="mailto:jchacon@bec.uned.es">Jesús Chacón</a> 
 *
 */
public class MatlabConnector implements RemoteControlProtocolL1, RemoteControlProtocolL2 {
	private MatlabProxyFactory proxyFactory;
	protected MatlabProxy proxy = null;
	private boolean hideMatlab = true;
	protected Context context = new Context(this);
	private Map<String, String> commands = new HashMap<>();

	/**
	 * Default Constructor  
	 */    
	public MatlabConnector () {
		this(true);
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
		setContext(this);
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setContext(Object context) {
		this.context = new Context(this, context);
	}

	public Context getContext() {
		return context;
	}

	/**
	 * Start the connection with the external application  
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
	 * Check if the connection was successful
	 * @return boolean true if the connection was successful
	 */
	public boolean isConnected(){
		return (proxy != null && proxy.isConnected());
	} 
	
	/**
	 * Finish the connection with the external application     
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
	 * Get the value of a variable of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	@Override
	public Object[] get(String[] variable) {
		try {
			int length = variable.length;
			Object[] result = new Object[length];
			for(int i=0; i<length; i++) {
				result[i] = proxy.getVariable(variable[i]);
			}
			return result;
		} catch(NullPointerException e) {
			System.err.println("Error invalid arguments");
		} catch (MatlabInvocationException e) {
			System.err.println("Error getting Matlab variable: "+variable);
			System.err.println(e.getMessage());
		}
		return null;
	}

	/**
	 * Get the value of several variables of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	public Object get(String variable, String type) {
		switch(type) {
		default:
		case "last":
			return get(variable);
		case "history":
			return getHistory(variable);
		}
	}

	/**
	 * Gets the value of a variable of the application
	 * @param variable String the variable name
	 * @return Object the value
	 */
	public Object[] get(String[] variable, String type) {
		switch(type) {
		default:
		case "last":
			return get(variable);
		case "history":
			ArrayList result = new ArrayList();
			for(int i=0; i<variable.length; i++) {
				result.add(getHistory(variable[i]));
			}
			return result.toArray();
		}
	}

	private Object[] getHistory(String variable) {
		List history = context.getHistory(variable);
		if(history != null) {
			Object[] values = history.toArray(); 
			history.clear();
			return values;
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
	 * Sets the value of a variable of the application
	 * @param variable String the variable name
	 * @param Object the value
	 */
	@Override
	public void set(String[] variable, Object[] value) {
		try {
			int length = variable.length;
			for(int i=0; i<length; i++) {
				proxy.setVariable(variable[i], value[i]);
			}
		} catch(NullPointerException e) {
			System.err.println("Error invalid arguments");
		} catch(MatlabInvocationException e) {
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

	@Override
	public void setCommand(String command, String type) {
		commands.put(type, command);
	}

	public void add(String[] variables) {
		int length = variables.length;
		Map<String, List> bufferedVariables = new HashMap<>();
		for(int i=0; i<length; i++) {
			String name = variables[i];
			if(!bufferedVariables.containsKey(name)) {
				List buffer = new ArrayList();
				bufferedVariables.put(name, buffer);
			}
		}
	}

	@Override
	public void step() {
		setValues();
		if(commands.containsKey("step")) {
			eval(commands.get("step"));
		}
		getValues();
	}

	public void step(int steps) {
		for(int i=0; i<steps; i++) {
			step();
		}
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
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
}