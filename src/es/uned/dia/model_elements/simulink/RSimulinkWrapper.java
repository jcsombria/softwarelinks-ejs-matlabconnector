package es.uned.dia.model_elements.simulink;

public class RSimulinkWrapper extends es.uned.dia.interoperate.matlab.jimc.RSimulinkExternalApp{
		
	public RSimulinkWrapper(String mdlSmlkFile) {		 		    
		super(mdlSmlkFile);
	}
			  	 
	public void setSmkMdlDir(String dir){    
		//modelDir=dir;
    }
}
