package es.uned.dia.jcsombria.softwarelinks.matlab.server;

import es.uned.dia.jcsombria.softwarelinks.matlab.client.MatlabConnector;
import es.uned.dia.jcsombria.softwarelinks.rpc.JsonRpcServer;
import es.uned.dia.jcsombria.softwarelinks.rpc.param.RpcParam;
import es.uned.dia.jcsombria.softwarelinks.rpc.param.RpcParamFactory;

public class RemoteMatlabConnectorServer extends JsonRpcServer {
	private MatlabConnector matlab = new MatlabConnector(false);

	public RemoteMatlabConnectorServer() {
		try {
			this.on("connect", RemoteMatlabConnectorServer.class.getMethod("connect"));
			this.on("disconnect", RemoteMatlabConnectorServer.class.getMethod("disconnect"));
			this.on("set", RemoteMatlabConnectorServer.class.getMethod("set", RpcParam.class, RpcParam.class));
			this.on("get", RemoteMatlabConnectorServer.class.getMethod("get", RpcParam.class));
			this.on("eval", RemoteMatlabConnectorServer.class.getMethod("eval", RpcParam.class));
		} catch (NoSuchMethodException | SecurityException e) {
			System.err.println("Error Registering RPC Methods");
			e.printStackTrace();
		}
	}
	
	public String echo(String message) {
		return message;
	}

	public RpcParam<Boolean> connect() {
		boolean isConnected = matlab.connect();
		return RpcParamFactory.create("result", isConnected);
	}

	public RpcParam<Boolean> disconnect() {
		if(!matlab.isConnected()) {
			System.err.println("matlab not connected");
			return RpcParamFactory.create("result", false);
		}
		matlab.disconnect();
		return RpcParamFactory.create("result", true);
	}

	public RpcParam get(RpcParam<String> name) {
		if(!matlab.isConnected()) {
			return RpcParamFactory.createNull("result");
		}
		String variable = name.get();
		Object result = matlab.get(variable);
		return RpcParamFactory.create("result", result);
	}

	public void set(RpcParam<String> name, RpcParam value) {
		if(!matlab.isConnected()) {
			System.err.println("matlab not connected");
			return;
		}
		String variable = name.get();
		Object valueAsObject = value.get();
		if(valueAsObject.getClass() != Double[].class) {
			matlab.set(variable, valueAsObject);
		} else {
			Object valueAsArray = (Object)asPrimitiveArray((Double[])valueAsObject);
			matlab.set(variable, valueAsArray);	
		}
	}
	
	private double[] asPrimitiveArray(Double[] array) {
		if(array == null) {
			return null;
		}
		double[] result = new double[array.length];
		for(int i=0; i<array.length; i++) { 
			result[i] = array[i].doubleValue();
		}
		return result;
	}

	public void eval(RpcParam<String> command) {
		if(!matlab.isConnected()) {
			System.err.println("matlab not connected");
			return;
		}
		String commandToEval = command.get();
		matlab.eval(commandToEval);
	}
}