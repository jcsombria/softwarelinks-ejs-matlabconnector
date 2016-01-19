package es.uned.dia.jcsombria.softwarelinks.utils;

import java.util.Vector;

public class SimulinkCodeBuilder {
	private static String prefix = "es.uned.dia.jcsombria.softwarelinks.transport.";
	private String name;
	private ClassBuilder builder = new ClassBuilder();
	private ConfigurationModel model;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setModel(ConfigurationModel model) {
		this.model = model;
	}

	public String getCode() {
		createClass();
		switch(model.getMode()) {
		default:
		case "local":
			return "new es.uned.dia.jcsombria.softwarelinks.matlab.client.SimulinkConnector()" + builder.toString() + ";";
		case "remote":
			return "try {" +
				prefix + "Transport transport = new "+ prefix + getTransport() + "(\"" + model.getURL() + "\");" +
				"es.uned.dia.jcsombria.softwarelinks.matlab.client.RemoteMatlabConnectorClient __matlab__ = new es.uned.dia.jcsombria.softwarelinks.matlab.client.RemoteMatlabConnectorClient(transport);" +
				name + " = new es.uned.dia.jcsombria.softwarelinks.matlab.client.RemoteSimulinkConnectorV2(__matlab__)" + builder.toString() + ";" +
			"} catch (Exception e) { e.printStackTrace(); }";
		}
	}

	private String getTransport() {
		switch(model.getProtocol()) {
		default:
		case "tcp":
			return "TcpTransport";
		case "http":
			return "HttpTransport";
		}
	}

	private void createClass() {
		MethodGetBuilder mgb = new MethodGetBuilder();
		MethodSetBuilder msb = new MethodSetBuilder();
		Vector<Vector<Object>> data = model.getDataVector();
		for(Vector<Object> row : data) {
			String matlabVariable = (String)row.get(0);
			String ejsVariable = (String)row.get(1);
			boolean shouldAddToGet = (Boolean)row.get(2);
			boolean shouldAddToSet = (Boolean)row.get(3);
			if(isValid(matlabVariable, ejsVariable)) {
				if(shouldAddToGet) {
					mgb.addLink(matlabVariable, ejsVariable, "Double");
				}
				if(shouldAddToSet) {
					msb.addLink(matlabVariable, ejsVariable);
				}
			}
		}
		builder.addMethod("get", mgb);
		builder.addMethod("set", msb);
	}

	private boolean isValid(String matlab, String ejs) {
		return matlab != "" && matlab != null && ejs != "" && ejs != null;
	}
}