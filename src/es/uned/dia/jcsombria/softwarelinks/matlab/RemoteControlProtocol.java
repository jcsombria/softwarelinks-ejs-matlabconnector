package es.uned.dia.jcsombria.softwarelinks.matlab;

public interface RemoteControlProtocol {
	public boolean connect();
	public boolean disconnect();
	public void set(String name, Object value);
	public Object get(String name);
	public Object eval(String command);
}
