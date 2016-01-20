package es.uned.dia.model_elements.octave;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.ResourceLoader;

public class OctaveWrapper extends es.uned.dia.interoperate.octave.OctaveExternalApp{

	public OctaveWrapper() {
		super ();
		/*
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
		*/		
	}
	
	public void addJars(){
      System.out.println("directorio principal:"+jarsDir);
		
	try{
		  addURL(new File(jarsDir+"/commons-logging-1.1.1.jar").toURL());
		  addURL(new File(jarsDir+"/log4j-1.2.15.jar").toURL());
		  addURL(new File(jarsDir+"/javaoctave-0.6.1.jar").toURL());
		}catch(java.net.MalformedURLException e){
		System.out.println("e:"+e);
		}
	}
	
	public void addURL(URL url) {
		  URLClassLoader classLoader
		         = (URLClassLoader) ClassLoader.getSystemClassLoader();
		  Class clazz= URLClassLoader.class;

		  // Use reflection
		    try{
		  java.lang.reflect.Method method= clazz.getDeclaredMethod("addURL", new Class[] { URL.class });
		  method.setAccessible(true);

		    method.invoke(classLoader, new Object[] { url });
		  }catch (java.lang.Exception e) {
		    System.out.println(e);
		  }
		}	
}
