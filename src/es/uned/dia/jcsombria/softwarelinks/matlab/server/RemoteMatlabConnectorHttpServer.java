package es.uned.dia.jcsombria.softwarelinks.matlab.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/*
 * a simple http server
*/
public class RemoteMatlabConnectorHttpServer {
	HttpServer server;

	public RemoteMatlabConnectorHttpServer(int port) {
		try {
			server = HttpServer.create(new InetSocketAddress(port), 0);
			server.createContext("/", new MyHandler());
			server.setExecutor(null);
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public void start() {
		server.start();
	}
	
	static class MyHandler implements HttpHandler {
		private RemoteMatlabConnectorServer rmcs = new RemoteMatlabConnectorServer();    

		public void handle(HttpExchange t) throws IOException {			
			String request = extract(t.getRequestBody());
			System.out.println("request: "+request);
			String response = rmcs.parse(request);
			System.out.println("response: "+response);
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			if(response != null) {
				os.write(response.getBytes());
			}
			os.close();
		}

		private String extract(InputStream message) {
			StringBuilder extracted = new StringBuilder(); 
			try {
				byte[] buffer = new byte[1024];
				int length = message.read(buffer);
				while(length != -1) {
					extracted.append(new String(buffer, 0, length));
					length = message.read(buffer);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return extracted.toString();
		}
	}
}
