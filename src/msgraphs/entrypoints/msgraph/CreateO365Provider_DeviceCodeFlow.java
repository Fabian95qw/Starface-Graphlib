package si.module.msgraphs.entrypoints.msgraph;

import java.util.List;

import org.apache.logging.log4j.Logger;

import de.starface.core.component.StarfaceComponentProvider;
import de.vertico.starface.module.core.model.VariableType;
import de.vertico.starface.module.core.model.Visibility;
import de.vertico.starface.module.core.runtime.IBaseExecutable;
import de.vertico.starface.module.core.runtime.IRuntimeEnvironment;
import de.vertico.starface.module.core.runtime.annotations.Function;
import de.vertico.starface.module.core.runtime.annotations.InputVar;
import de.vertico.starface.module.core.runtime.annotations.OutputVar;
import si.module.msgraphs.o365provider.DeviceCodeFlowProvider;
import si.module.msgraphs.o365provider.O365Provider;

@Function(name="Create O365 Provider (DeviceCode)", visibility=Visibility.Public, rookieFunction=false, description="")
public class CreateO365Provider_DeviceCodeFlow implements IBaseExecutable 
{
	//##########################################################################################
	
	@InputVar(label="ClientID", description="Microsoft Azure APP ID",type=VariableType.STRING)
	public String ClientID="";
	
	@InputVar(label="Scopes", description="List of Ms Graph Scopes",type=VariableType.LIST)
	public List<List<String>> ScopesRaw= null;
	
	@InputVar(label="Branch", description="Change the Branch Provider. DO NOT CHANGE IF YOU DON'T KNOW WHAT YOU'RE DOING",type=VariableType.STRING)
	public String Branch="https://graph.microsoft.com";
	
	@InputVar(label="DeviceCodeFLowProvider", description="Use the DeviceAuthprovider from CreateCodeAuthorization",type=VariableType.OBJECT)
	public Object ODeviceCodeFLowProvider = null;
	
	@InputVar(label="Timeout", description="Timeout to wait for Code Authorization",type=VariableType.NUMBER)
	public Integer Timeout=120;
	
	@OutputVar(label="O365Provider", description="Return an Office365 Provider",type=VariableType.OBJECT)
	public Object O365Provider = null;
	
    StarfaceComponentProvider componentProvider = StarfaceComponentProvider.getInstance(); 
    //##########################################################################################
	
	//###################			Code Execution			############################	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(IRuntimeEnvironment context) throws Exception 
	{
		Logger log = context.getLog();
		log.debug("Creating Provider...");
		
		List<String> Scopes = null;
		
		//For some Reason STARFACE Provides a List[List[]] instead of a List[]
		try
		{
			Scopes = ScopesRaw.get(0);
		}
		catch(Exception e)
		{
			Object O= ScopesRaw;
			Scopes = (List<String>) O;
		}
		
		log.debug(ClientID);
		log.debug(Scopes);
		
		DeviceCodeFlowProvider DCP = (DeviceCodeFlowProvider) ODeviceCodeFLowProvider;

		if(!DCP.hasToken())
		{
			DCP.poll(120);
		}

		//DCP.poll(Timeout);
		log.debug("Has Token: " + DCP.hasToken());
		
		if(DCP.hasToken())
		{
			O365Provider Provider = new O365Provider(DCP, log);
			Provider.setBranch(Branch);
			O365Provider = Provider;
					
			log.debug("Provider Sucessfully created!");		
		}
		else
		{
			log.debug("No Token Provided. O365 wont work.");
		}
		

		
	}//END OF EXECUTION

	
}
