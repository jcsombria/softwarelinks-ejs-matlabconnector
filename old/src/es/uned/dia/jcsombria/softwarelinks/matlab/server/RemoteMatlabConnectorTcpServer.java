package es.uned.dia.jcsombria.softwarelinks.matlab.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class RemoteMatlabConnectorTcpServer extends Thread {
	private static final int DEFAULT_PORT = 2055;
	private int port;
	private boolean running = true;
	private List<Handler> clients = new ArrayList<>();

	public RemoteMatlabConnectorTcpServer() {
		this.port = DEFAULT_PORT;
	}

	public RemoteMatlabConnectorTcpServer(int port) {
		this.port = port;
	}

	public void run() {
		try ( 
			ServerSocket serverSocket = new ServerSocket(port);
		) {
			while(running) {
				Socket clientSocket = serverSocket.accept();
				System.out.println("New client connected.");
				Handler client = new Handler(clientSocket); 
				clients.add(client);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void quit() {
		for(Handler handler : clients) {
			handler.quit();
		}
		running = false;
	}

}

class Handler extends Thread {
	private RemoteMatlabConnectorServer rmcs = new RemoteMatlabConnectorServer();    
	private Socket socket;
	private boolean running = true;

	public Handler(Socket socket) {
		this.socket = socket;
		this.start();
	}
	
	public void run() {
		try (
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		) {
			while(running) {
				String request = in.readLine();
				if(request != null) {
					String response = rmcs.parse(request);
					out.println(response);
				}
			}
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void quit() {
		running = false;
	}
}