package es.uned.dia.jcsombria.softwarelinks.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class ModelPackager {
	private List<String> files = new ArrayList<>();

	public void extractTo(String path) {
		for(String fileToExtract : files) {
			try {
				InputStream in = this.getClass().getResourceAsStream(fileToExtract);
				String whereToPut = getPathToExtract(path, fileToExtract);
				File extractedFile = createFile(whereToPut);
				if(in != null && extractedFile != null) {
					OutputStream out = new FileOutputStream(extractedFile);
					copyStream(in, out);
					out.close();
				}
			} catch (IOException | NullPointerException e) {
				System.err.println("Error: couldn't extract files from jar");
				e.printStackTrace();
			}
		}
	}

	private String getPathToExtract(String path, String file) {
		return path + File.separator + file;
	}

	private File createFile(String path) {
		try {
			String dirs = getDirs(path);
			File dir = new File(dirs);
			dir.mkdirs();
			boolean isFile = !path.endsWith("/");
			File file = null;
			if(isFile) {
				file = new File(path);
				file.createNewFile();
			}
			return file;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getDirs(String path) {
		int slash = path.lastIndexOf("/")+1;
		if(slash > 0) {
			return path.substring(0, slash);
		} else {
			return "";
		}
	}

	private void copyStream(InputStream in, OutputStream out) {
		byte[] buffer = new byte[1048];
		try {
			while(in.available() > 0) {
				int read = in.read(buffer);
				out.write(buffer, 0, read);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public void add(String file) {
		files.add(file);
	}
}
