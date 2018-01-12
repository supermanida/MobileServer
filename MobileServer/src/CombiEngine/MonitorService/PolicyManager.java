package CombiEngine.MonitorService;

//*************************************************************************************************
//
// CombiGate Service Main
//
//*************************************************************************************************
public class PolicyManager
{
	//*********************************************************************************************
	//
	//*********************************************************************************************
//	DataBean      dataBean      = null;
//	GateProcessor gateProcessor = null;
	PolicyServer    server        = null;
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public static void main(String[] args)
	{
		new PolicyManager(args[0]);
	}
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public PolicyManager(String strport)
	{
		System.out.println("CombiGateService Starting...");
		startService(strport);
	}
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	private void startService(String strport)
	{
		try
		{
//			dataBean = new DataBean(configFile);
//			gateProcessor = new GateProcessor(dataBean);
			server = new PolicyServer(Integer.parseInt(strport));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}