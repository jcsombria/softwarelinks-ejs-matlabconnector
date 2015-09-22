package es.uned.dia.jcsombria.softwarelinks.matlab.client;

import java.net.MalformedURLException;

import es.uned.dia.jcsombria.softwarelinks.matlab.RemoteControlProtocolL1;
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
public class RemoteMatlabConnectorClient extends JsonRpcClient implements RemoteControlProtocolL1 {

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
	public boolean eval(String command) {
		RpcParam[] args = new RpcParam[] {
			RpcParamFactory.create("command", command)
		};
		RpcParam[] result = (RpcParam[])execute("eval", args);
		/// TODO: convertir correctamente la respuesta
		return true;
	}

	
	public boolean getBoolean(String name) {
		Object value = get(name);
		return (boolean)value;
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