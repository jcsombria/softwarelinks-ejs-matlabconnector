package es.uned.dia.model_elements.simulink;

public class SimulinkWrapper extends es.uned.dia.interoperate.matlab.jimc.SimulinkExternalApp{
		
	public SimulinkWrapper(String mdlSmlkFile) {		 		    
		super(mdlSmlkFile);
	}
			  	 
	public void setSmkMdlDir(String dir){    
		//modelDir=dir;
    }
}
