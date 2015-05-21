package es.uned.dia.model_elements.sysquake;

import java.io.File;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

public class SysquakeWrapper extends es.uned.dia.interoperate.sysquake.SysquakeExternalApp{

	public SysquakeWrapper() {		
		String sqlinkFile=null;
		Resource auxresource=ResourceLoader.getResource("SysquakeLink.dll");
		if (auxresource==null) return;
		File temporalDirectory;
		try{		
			File userHome=new File(System.getProperty("user.home"));
			if (OSPRuntime.isWindows()) {				
				temporalDirectory=org.colos.ejs.library.utils.TemporaryFilesManager.createTemporaryDirectory("sysquakeWrapper", userHome);
				temporalDirectory.deleteOnExit();				
				sqlinkFile=org.colos.ejs.library.utils.TemporaryFilesManager.extractToDirectory("SysquakeLink.dll", temporalDirectory, true).toString().replace('\\','/');
			}
		}catch(Exception e){}	
		if (sqlinkFile!=null){
			new es.uned.dia.interoperate.sysquake.SysquakeExternalApp(new File(sqlinkFile));			
		}		
	}
	
}
