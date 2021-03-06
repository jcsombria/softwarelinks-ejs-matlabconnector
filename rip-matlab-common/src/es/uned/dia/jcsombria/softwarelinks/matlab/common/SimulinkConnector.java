/**
 * SimulinkConnector
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

import java.io.File;
import java.io.IOException;

import es.uned.dia.jcsombria.softwarelinks.matlab.common.MatlabConnector;

public class SimulinkConnector extends MatlabConnector implements RemoteControlProtocolL2 {

	public static final String EJS_PREFIX = "Ejs_";
	protected String initCommand = null;
	private String path;
	private String model;
	private boolean startRequired = true;
	private boolean waitForEverFlag;

	public SimulinkConnector() {
		super(false);
	}

	public SimulinkConnector(String model) {
		super(false);
		setModel(model);
	}

	/**
	 * Set the Simulink model associated to the connection.
	 * @param file
	 */
	public void setModel(String file) {
		String thePath = path, theModel = model;
		try {
			path = getModelPath(file);
			model = getModelName(file);
		} catch(IOException e) {
			System.err.println("Invalid model file: "+file);
			path = thePath;
			model = theModel;
		}
	}

	private String getModelPath(String path) throws IOException {
		File file = new File(path);
		String thePath = file.getCanonicalPath();
		int lastSlash = thePath.lastIndexOf("/");
		return (lastSlash > 0) ? thePath.substring(0, lastSlash) : "";
	}

	private String getModelName(String path) {
		boolean isValidPath = path.toLowerCase().endsWith(".mdl");
		if (!isValidPath) {
			return "";
		}
		String model = path.trim();
		int begin = Math.max(0, model.lastIndexOf('/')+1),
				end = model.lastIndexOf('.');
		String name = model.substring(begin, end);
		return name;
	}

	/**
	 * Starts the connection with the external application  
	 * @return boolean true if the connection was successful
	 */
	@Override
	public boolean connect() {
		boolean isConnected = super.connect();
		System.out.println("Model: " + model);
//		if(isConnected && model != null) {
//			open(model);
//		}
		return isConnected;
	}

	private void loadModel() {
		open(path, model);
		addEjsSubsystemTo(model);
		initialize();
	}

	/**
	 * Opens the Simulink model 
	 */
	private void open(String path, String model) {
		String command = CommandBuilder.cd(path) +
						 CommandBuilder.load_system(model) +
						 CommandBuilder.set_param(model, "SimulationCommand", "stop") +
						 CommandBuilder.set_param(model, "StopTime", "inf");
		eval(command);
	}

	/**
	 * Open a Simulink model.  
	 * @return boolean true if the load was successful
	 */
	public void open(String model) {
		setModel(model);
		loadModel();
	}

	/**
	 * Creates the EJS subsystem in the Simulink model
	 */
	private void addEjsSubsystemTo(String model) {
		eval(
			CommandBuilder.addEjsSubblock(model) +
			CommandBuilder.addPauseBlock(model) + 
		// Set the start time to zero and stop time to infinity
			CommandBuilder.set_param(model, "StartTime", "0") +
			CommandBuilder.set_param(model, "StopTime", "inf")
		);
	}
	
	/**
	 * Initializes a Matlab/Simulink session. 
	 */
	protected void initialize() {
		eval("clear all;variables.path={};variables.name={};variables.fromto={};variables.port={};");
		if(initCommand != null) {
			eval(initCommand.substring(0,initCommand.lastIndexOf(";")));
		}
		eval("Ejs__ResetIC = 0");
		eval("sistema='"+model+"'");
		eval(CommandBuilder.convertSimulinkModel());
		startRequired = true;
	}

	/**
	 * Finishes the connection with the external application
	 */
	@Override
	public boolean disconnect() {
		eval("bdclose");
		return super.disconnect();
	}

	/**
	 * Links a client variable with a variable of the Simulink model (long format)
	 * @param variable String the client variable
	 * @param path String the path of the external variable
	 * @param type String the type of the external variable
	 * @param port String the port of the external variable
	 */
	public boolean linkVariables(String variable, String path, String type, String port) {
		if(context == null) {
			return false;
		}
		if(isTime(path, type, port)) {
			context.linkVariables(variable, EJS_PREFIX+"time");
		} else {
			context.linkVariables(variable, path, type, port);
			addToInitCommand(variable, path, type, port);
		}
		return true;
	}
	
	private boolean isTime(String path, String type, String port) {
		return path.equals(model)
			&& type.equalsIgnoreCase("param")
			&& port.equalsIgnoreCase("time");
	}

	private void addToInitCommand(String variable, String path, String type, String port) {
		String initValue = getInitializationString(variable);
		if (initCommand == null) {
			initCommand = "";
		}
		initCommand += initValue + ";"
			  		+ "variables.path{end+1,1} = '" + path + "';"
			  		+ "variables.name{end+1,1} = '" + EJS_PREFIX + variable + "';"
			  		+ "variables.fromto{end+1,1} = '" + type.toLowerCase() + "';"
			  		+ "variables.port{end+1,1} = '" + port + "';";
	}

	private String getInitializationString(String variable) {
		Object value = context.getClientValue(variable);
		String evarInitValue = EJS_PREFIX + variable + "=";
		String type = value.getClass().getSimpleName();
		switch (type) {
		case "Double":
			evarInitValue += value;
			break;
		case "double[]":
			double[] aux_value = (double[])value;
			String aux_valueString = "[";
			for (int w=0;w<aux_value.length;w++) { 
				aux_valueString = aux_valueString + aux_value[w] + ",";
			}
			aux_valueString = aux_valueString + "]";
			evarInitValue += aux_valueString;
			break;                                                                            
		case "double[][]":
			double[][] aux_value2D = (double[][])value;
			String aux_valueString2D = "[";
			for (int w=0;w<aux_value2D.length;w++) {
				for (int y=0;y<aux_value2D[0].length;y++) {                          
					aux_valueString2D = aux_valueString2D + aux_value2D[w][y] + ",";                  
				}
				aux_valueString2D = aux_valueString2D + ";";                 
			}
			aux_valueString2D = aux_valueString2D + "]";
			evarInitValue += aux_valueString2D;
			break;
		default:
		case "String":
			evarInitValue += "'" + (String)value+"'";
			break; 
		}
		return evarInitValue;
	}

	/**
	 * Advance the Simulink model exactly one step.
	 */
	@Override
	public void step() {
		if (model == null) {
			return;
		}
		setValues();
		if (startRequired) {
			String startCommand = CommandBuilder.set_param(model, "SimulationCommand", "start");			
			eval(startCommand);
			waitForPauseSimulink(10);
			startRequired = false;
		}
		String updateCommand = CommandBuilder.set_param(model, "SimulationCommand", "update");
		eval(updateCommand);
		String continueCommand = CommandBuilder.set_param(model, "SimulationCommand", "continue");
		eval(continueCommand);
		eval("EjsSimulationStatus='unknown';"); 
		if (waitForEverFlag){
			waitForPauseSimulink();
		} else {
			waitForPauseSimulink(10);
		}
		getValues();
	}

	/**
	 * Steps the Simulink model a number of times.
	 * @param dt double indicates the number of times to step the Simulink model.
	 */
	public void step (double dt) {
		if (model == null) {
			return;
		}
		setValues();
		if (startRequired) {
			String startCommand = CommandBuilder.set_param(model, "SimulationCommand", "start");			
			eval(startCommand);
			waitForPauseSimulink(10);
			startRequired = false;
		}
		for (int i=0, times=(int)dt; i<times; i++) {
			String updateCommand = CommandBuilder.set_param(model, "SimulationCommand", "update");
			eval(updateCommand);
			String continueCommand = CommandBuilder.set_param(model, "SimulationCommand", "continue");
			eval(continueCommand);
			eval("EjsSimulationStatus='unknown';"); 
			if (waitForEverFlag){
				waitForPauseSimulink();
			} else {
				waitForPauseSimulink(10);
			}
		}
		getValues();
	}

	private boolean waitForPauseSimulink() {
		return waitForPauseSimulink(0);
	}

	private boolean waitForPauseSimulink(int maxTrials) {
		int trial = maxTrials;
		boolean isPaused;
		do {
			isPaused = isSimulinkPaused();
			trial --;
		} while (!isPaused && trial > 0);
		return isPaused;
	}

	private boolean isSimulinkPaused() {
		String isModelPausedCommand = String.format("smlkstatus = %s", 
				CommandBuilder.isModelPaused(model));
		eval(isModelPausedCommand);
		boolean isPaused = ((boolean[])get("smlkstatus"))[0];
		return isPaused;
	}

	@Override
	public void setCommand(String command, String type) {
		// TODO Auto-generated method stub
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}
}