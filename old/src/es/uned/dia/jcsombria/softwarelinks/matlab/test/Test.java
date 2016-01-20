package es.uned.dia.jcsombria.softwarelinks.matlab.test;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import es.uned.dia.interoperate.matlab.jimc.MatlabExternalApp;
import es.uned.dia.interoperate.matlab.jimc.SimulinkExternalApp;
import es.uned.dia.jcsombria.softwarelinks.matlab.client.RemoteMatlabConnectorClient;
import es.uned.dia.jcsombria.softwarelinks.transport.TcpTransport;
import es.uned.dia.jcsombria.softwarelinks.transport.Transport;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws MatlabConnectionException, MatlabInvocationException
	{
		Test test = new Test();

		test.testRemoteMatlab();
//		test.testMatlab();
//		test.testSimulink();
	}

	public void testRemoteMatlab() {
		RemoteMatlabConnectorClient rmcc = null;
		try {
			Transport transport = new TcpTransport("tcp://localhost:2055");
			rmcc = new RemoteMatlabConnectorClient(transport);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int time = 10, expected = 10;
		if(rmcc.connect()) {
			try{ Thread.sleep(10000); } catch(InterruptedException e){}
			rmcc.set("t", 10.0);
			rmcc.set("u", new double[]{0.0, 1.0, 2.0});
			rmcc.set("x", "Prueba");
			rmcc.set("y", 1);
			
			String xx = rmcc.getString("x");
			int yy = rmcc.getInt("y");
			double tt = rmcc.getDouble("t");
			double[] uu = rmcc.getDoubleArray("u");
			
			String uu_string = "[";
			for(int i=0; i<uu.length; i++) {
				if(i==0) {
					uu_string += uu[i];
				} else {
					uu_string += "," + uu[i];  
				}
			}
			uu_string += "]";

			System.out.println("t: "+tt);
			System.out.println("u: "+uu_string);
			System.out.println("x: "+xx);
			System.out.println("y: "+yy);
		}
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		rmcc.disconnect();
	}

	public double t;
	public double dt;
	public double w;
	public double y;
	// Declare local variables
	public double time=0, position=10, velocity=15;    

	public void testMatlab() {
		t = 0.0; 
		dt = 0.01; 
		w = 2*Math.PI; 
		y = 0.0;

		MatlabExternalApp mc = new MatlabExternalApp(false);
		mc.connect();
		mc.setClient(this);

		String cmd = "y=sin(w*t);";
		mc.setCommand(cmd);
		mc.eval("t="+t);
		mc.eval("w="+w);
		mc.eval("y="+y);
		mc.linkVariables("t", "t");
		mc.linkVariables("w", "w");
		mc.linkVariables("y", "y" +	"");
		
		System.out.println("[" + t + ", " + y + "]");
		for(int i=0; i<100; i++) {
			t = t + dt;
			mc.eval(cmd);
			mc.step(1);
			System.out.println("[" + t + ", " + y + "]");
		}

		mc.disconnect();
	}

	public void testSimulink() {
		SimulinkExternalApp mc = new SimulinkExternalApp("./bounce.mdl");

	    mc.setClient(this);
	    //Link Java and Simulink variables
	    mc.linkVariables("position", "bounce/Position", "out", "1");
	    mc.linkVariables("velocity", "bounce/Velocity", "out", "1");
	    mc.linkVariables("time", "bounce", "param", "time");
		mc.connect();	

	    boolean firstReset=true;
	    //Perform the simulation
	    do{
	    	mc.step(1);
	    	//reset at time=10
	    	if (time>=10 && firstReset){
	    		position=10; velocity=0;
	    		mc.synchronize();
	    		firstReset=false;
	    	}
	    	System.out.println("time:"+time+" position:"+position+" velocity:"+velocity);
	    } while (time < 13);     
//	    for(long i=0; i<100000; i++) {
//	    	System.out.println(i);
//	    }
	    mc.disconnect();
	}
}
