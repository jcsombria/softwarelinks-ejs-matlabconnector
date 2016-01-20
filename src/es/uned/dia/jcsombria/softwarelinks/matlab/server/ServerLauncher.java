package es.uned.dia.jcsombria.softwarelinks.matlab.server;

public class ServerLauncher {

	public static void main(String args[]) {
		String type = "tcp";
		int port = 2055;
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-t")) {
				String param = ""; 
				try {
					param = args[i+1];
					if(!param.matches("tcp|http")) {
						System.err.println("Invalid transport: "+param);
						System.exit(1);
					}
					type = param;
					i++;
				} catch(ArrayIndexOutOfBoundsException | NullPointerException e) {
					System.out.println("Invalid option: '-t' requires an argument (tcp | http).");
					System.exit(1);
				}
			} else if(args[i].equals("-p")) {
				String param = "";
				try {
					param = args[i+1];
					port = Integer.valueOf(param);
				} catch(NumberFormatException e) {
					System.err.println("Invalid port: "+param);
					System.exit(1);
				} catch(ArrayIndexOutOfBoundsException | NullPointerException e) {
					System.out.println("Invalid option: '-p' requires an argument (tcp | http).");
					System.exit(1);
				}
				i++;
			} else {
				showHelp();
				System.exit(1);
			}
		}
		if(type.equals("tcp")) {
			new RemoteMatlabConnectorTcpServer(port).start();
			System.out.println("Server listening to tcp://localhost:"+port);
		} else if(type.equals("http")) {
			new RemoteMatlabConnectorHttpServer(port).start();
			System.out.println("Server listening to http://localhost:"+port);
		} else {
			showHelp();
		}
	}

	public static void showHelp() {
		System.out.println("usage: java -jar RcpMatlabServer.jar [-t [tcp|http]] [- p port]");
		System.out.println("	Start the RPC Matlab Server listening to the specified");
		System.out.println("	ip and port (default: 2055).");
		System.out.println("options:");
		System.out.println("	-p [tcp|http] Use tcp or http transport (default: tcp).");
	}

}