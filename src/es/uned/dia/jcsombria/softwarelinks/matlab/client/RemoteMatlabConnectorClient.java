package es.uned.dia.jcsombria.softwarelinks.matlab.client;

import java.net.MalformedURLException;

import es.uned.dia.jcsombria.softwarelinks.matlab.RemoteControlProtocol;
import es.uned.dia.jcsombria.softwarelinks.rpc.JsonRpcClient;
import es.uned.dia.jcsombria.softwarelinks.rpc.param.RpcParam;
import es.uned.dia.jcsombria.softwarelinks.rpc.param.RpcParamFactory;
import es.uned.dia.jcsombria.softwarelinks.transport.TcpTransport;
import es.uned.dia.jcsombria.softwarelinks.transport.Transport;

/**
 * Class to implement the RPC communication protocol with MATLAB
 * @author <a href="mailto:gfarias@bec.uned.es">Gonzalo Farias</a>
 * @author <a href="mailto:jchacon@bec.uned.es">Jesús Chacón</a> 
 *
 */
public class RemoteMatlabConnectorClient extends JsonRpcClient implements RemoteControlProtocol {

	public RemoteMatlabConnectorClient(Transport transport) throws Exception {
		super(transport);
	}

	public RemoteMatlabConnectorClient(String url) throws MalformedURLException, Exception {
		super(new TcpTransport(url));
	}

	@Override
	public boolean connect() {
		RpcParam[] response = (RpcParam[])execute("connect", null);
		RpcParam<Boolean> result = (RpcParam<Boolean>)response[0];
		boolean isConnected = result.get().booleanValue();
		return isConnected;
	}

	@Override
	public boolean disconnect() {
		Object[] response = (Object[])execute("disconnect", null);
		RpcParam<Boolean> result = (RpcParam<Boolean>)response[0];
		boolean isConnected = result.get().booleanValue();
		return isConnected;
	}

	@Override
	public Object get(String name) {
		RpcParam[] args = new RpcParam[] {
			RpcParamFactory.create("name", name)
		};
		RpcParam[] result = (RpcParam[])execute("get", args);
		return result[0].get();
	}
	
	@Override
	public void set(String name, Object value) {
		set(RpcParamFactory.create("name", name), RpcParamFactory.create("value", value));
	}

	private void set(RpcParam<String> name, RpcParam value) {
		RpcParam[] args = new RpcParam[] {
			RpcParamFactory.create("name", name.get()),
			value
		};
		execute("set", args);
	}

	@Override
	public Object eval(String command) {
		RpcParam[] args = new RpcParam[] {
			RpcParamFactory.create("command", command)
		};
		RpcParam[] result = (RpcParam[])execute("eval", args);
		return result[0];
	}

	
	public boolean getBoolean(String name) {
		return (boolean)get(name);
	}

	public int getInt(String name) {
		Double[] response = (Double[])get(name);
		Double result = response[0];
		return (result != null) ? result.intValue() : 0;
	}

	public double getDouble(String name) {
		Double[] response = (Double[])get(name);
		Double result = response[0];
		return result.doubleValue();
	}

	public String getString(String name) {
		return (String)get(name);
	}

	public double[] getDoubleArray(String name) {
		Double[] result = (Double[])get(name);
		double[] values = new double[result.length];
		for(int i=0; i<result.length; i++) {
			values[i] = result[i].doubleValue();
		}
		return values;
	}
	
}

//protected class Context {
//	private Object client = null;
//
//	public Context(Object client) {
//		this.client = client;
//	}
//	
//	public boolean hasField(String name) {
//		Field field = getField(name);
//		return (field != null);
//	}
//
//	public Field getField(String name) {
//		try {
//			Field field = client.getClass().getField(name);
//			return field;
//		} catch(SecurityException e) {
//			System.err.println("Access denied to field "+name);
//		} catch(NoSuchFieldException e) {
//			System.err.println("No such field: "+name);
//		}
//		return null;
//	}
//	
//	public Object get(String variable) {
//		try {
//			Field field = getField(variable);
//			return field.get(client);
//		} catch(IllegalAccessException e) {
//			System.err.println("Access denied to field "+variable);
//		}
//		return null;
//	}
//	
//	public void set(String variable, Object value) {
//		try {
//			Field field = getField(variable);
//			field.set(client, value);
//		} catch(IllegalAccessException e) {
//			System.err.println("Access denied to field "+variable);
//		}
//	}
//
//
//
//
//
//
//	protected Context context = null; // The client of the external application
//	protected java.util.Map<String, Link> links = null;
//
//	 
//	public addLink() {
//		
//	}
//}
//
//protected class Context {
//	private Object client = null;
//
//	public Context(Object client) {
//		this.client = client;
//	}
//	
//	public boolean hasField(String name) {
//		Field field = getField(name);
//		return (field != null);
//	}
//
//	public Field getField(String name) {
//		try {
//			Field field = client.getClass().getField(name);
//			return field;
//		} catch(SecurityException e) {
//			System.err.println("Access denied to field "+name);
//		} catch(NoSuchFieldException e) {
//			System.err.println("No such field: "+name);
//		}
//		return null;
//	}
//	
//	public Object get(String variable) {
//		try {
//			Field field = getField(variable);
//			return field.get(client);
//		} catch(IllegalAccessException e) {
//			System.err.println("Access denied to field "+variable);
//		}
//		return null;
//	}
//	
//	public void set(String variable, Object value) {
//		try {
//			Field field = getField(variable);
//			field.set(client, value);
//		} catch(IllegalAccessException e) {
//			System.err.println("Access denied to field "+variable);
//		}
//	}
//}
//
//protected interface Link {
//	public String getNameOfClientVariable();
//	public String getNameOfExternalVariable();
//}
//
//protected class MatlabLink implements Link {
//	private String variable;
//	private String external;
//
//	public MatlabLink(String variable, String external) {
//		this.variable = variable;
//		this.external = external;
//	}
//	
//	public String getNameOfClientVariable() {
//		return this.variable;
//	}
//
//	public String getNameOfExternalVariable() {
//		return this.external;
//	}
//}
