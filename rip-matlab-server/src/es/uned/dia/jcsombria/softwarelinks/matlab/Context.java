/**
 * Context
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
package es.uned.dia.jcsombria.softwarelinks.matlab;

import java.lang.reflect.Field;

public class Context {
	private Object client = null;
	private RemoteControlProtocolL1 matlab;
	protected java.util.Map<String, Link> links = new java.util.HashMap<>();

	// A link between client and server variables
	protected interface Link {
		public String getNameOfClientVariable();
		public String getNameOfExternalVariable();
	}

	// A link between client and Matlab variables
	public class MatlabLink implements Link {
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
	
	// A link between client and Simulink variables
	public class SimulinkLink implements Link {
		public static final String EJS_PREFIX = "Ejs_";
		private String variable;
		private String path;
		private String type;
		private String port;
		
		public SimulinkLink(String variable, String path, String type, String port) {
			this.variable = variable;
			this.path = path;
			this.type = type;
			this.port = port;
		}

		@Override
		public String getNameOfClientVariable() {
			return this.variable;
		}

		@Override
		public String getNameOfExternalVariable() {
			return EJS_PREFIX + this.variable;
		}
		
		public String getPath() {
			return this.path;
		}
		
		public String getPort() {
			return this.port;
		}
		
		public String getType() {
			return this.type;
		}
		
	}

	public Context(RemoteControlProtocolL1 matlab) {
		this.client = this;
		this.matlab = matlab;
	}

	public Context(RemoteControlProtocolL1 matlab, Object client) {
		this.client = client;
		this.matlab = matlab;
	}
	
	/**
	 * Links a client variable with a variable of the external application
	 * @param clientVariable String the client variable
	 * @param externalVariable String the external variable
	 */
	public boolean linkVariables(String clientVariable, String externalVariable) {
		Link link = new MatlabLink(clientVariable, externalVariable);
		return addLink(link);
	}

	private boolean addLink(Link link) {
		String variable = link.getNameOfClientVariable();
		if(hasField(variable)) {
			links.put(variable, link);
			return true;
		}
		return false;
	}

	public boolean hasField(String name) {
		Field field = getField(name);
		return (field != null);
	}

	/**
	 * Links a client variable with a variable of the external application
	 * @param clientVariable String the client variable
	 * @param externalVariable String the external variable
	 */
	public boolean linkVariables(String variable, String path, String type, String port) {
		Link link = new SimulinkLink(variable, path, type, port);
		return addLink(link);		
	}

	/**
	 * sets all external variables with client values
	 */
	public void setValues() {
		for(Link link: links.values()) {
			Object value = getClientValue(link.getNameOfClientVariable());
			setServerValue(link.getNameOfClientVariable(), value);
		}
	}
	
	/**
	 * Get the value of a variable at the client side
	 * @param variable
	 * @return the value of the client variable
	 */
	public Object getClientValue(String variable) {
		try {
			Field field = getField(variable);
			return field.get(client);
		} catch(IllegalAccessException e) {
			System.err.println("Access denied to field "+variable);
		}
		return null;
	}
	
	private Field getField(String name) {
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

	private Object getServerValue(String variable) {
		Link link = links.get(variable);
		Field field = getField(variable);
		Class<?> type = field.getType();
		String external = link.getNameOfExternalVariable();		
		switch(type.getName()) {
		case "double":
			Object value = matlab.get(external);
			Class<?> theType = value.getClass();
			if(theType == double.class || theType == Double.class) {
				return value;
			} else if(theType == double[].class) {
				return ((double[])value)[0];
			} else if(theType == Double[].class) {
				return ((Double[])value)[0].doubleValue();
			} else {
				return 0.0;
			}
		case "[D":
			return (double[])matlab.get(external);
		case "[[D":
			return (double[][])matlab.get(external);
		case "string":
			return (String)matlab.get(external);
		default:
			return null;
		}
	}

	private void setServerValue(String variable, Object value) {
		Link link = links.get(variable);
		Field field = getField(variable);
		Class<?> type = field.getType();
		String external = link.getNameOfExternalVariable();		
		switch(type.getName()) {
		case "double":
			matlab.set(external, (double)value);
			break;
		case "[D":
			matlab.set(external, (double[])value);
			break;
		case "[[D":
			matlab.set(external, (double[][])value);
			break;
		case "string":
			matlab.set(external, (String)value);
			break;
		default:
		}
	}

	/**
	 * gets all external values
	 */
	public void getValues() {
		for(Link link: links.values()) {
			Object value = getServerValue(link.getNameOfClientVariable());
			setClientValue(link.getNameOfClientVariable(), value);
			System.out.println(link.getNameOfClientVariable()+": "+value);
		}
	}

	private void setClientValue(String variable, Object value) {
		try {
			Field field = getField(variable);
			field.set(client, value);
		} catch(IllegalAccessException e) {
			System.err.println("Access denied to field "+variable);
		}
	}

	/**
	 * Clears all linking between variables
	 */
	public void clearLinks(){
		links = new java.util.HashMap<String, Context.Link>();
	}	
}