package es.uned.dia.jcsombria.softwarelinks.matlab.common;

public interface RemoteControlProtocolL1 {
	public boolean connect();
	public boolean disconnect();
	public boolean eval(String command);
	public void set(String name, Object value);
	public Object get(String name);
	public void set(String[] name, Object[] value);
	public Object[] get(String[] name);
}
