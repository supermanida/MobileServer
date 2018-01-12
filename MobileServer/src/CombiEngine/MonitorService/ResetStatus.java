package CombiEngine.MonitorService;

import CombiEngine.Selector.SelectService;
import CombiEngine.Selector.SelectProcessor;
import CombiEngine.Processor.ProcessorThread;
import CombiEngine.ItemManager.ItemQueue;

import java.util.LinkedList;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.io.File;
import java.util.Date;
import java.lang.management.ThreadMXBean;

public class ResetStatus extends Thread
{
	private SelectService selectService;
	
	private String queueStatus;
	private String processorStatus;
	private String selectorStatus;
	private String cpuStatus;
	
	private String partition;
	
	public ResetStatus(SelectService selectService, String partition)
	{
		this.selectService = selectService;
		
		this.queueStatus = "";
		this.processorStatus = "";
		this.selectorStatus = "";
		this.cpuStatus = "";
		
		this.partition = partition;
		
		new CheckCpuStatus(this);
		
		start();
	}
	
	public void run()
	{
		StringBuffer statusStr = new StringBuffer();
		
		while ( true )
		{
			statusStr.delete(0, statusStr.length());
			
			LinkedList<SelectProcessor> selectors = selectService.selectors;
			
			for ( int i = 0 ; i < selectors.size() ; i++ )
			{
				SelectProcessor sp = selectors.get(i);
				statusStr.append("SelectProcessor" + i + ":" + sp.getStatus() + "\n");
			}
			
			synchronized(selectorStatus)
			{
				selectorStatus = statusStr.toString();
			}
			
			statusStr.delete(0, statusStr.length());
			
			for ( int i = 0 ; i < selectors.size() ; i++ )
			{
				ProcessorThread pt = selectors.get(i).processor;
				statusStr.append(pt.getStatus() + "\n");
			}
			
			synchronized(processorStatus)
			{
				processorStatus = statusStr.toString();
			}
			
			synchronized(queueStatus)
			{
				int rcvPut = 0;
				int clsPut = 0;
				int rcvGet = 0;
				int clsGet = 0;
				int ttlQue = 0;
				
				for ( int i = 0 ; i < selectService.selectors.size(); i++ )
				{
					ItemQueue iq = selectService.selectors.get(i).queue;
					int[] val = iq.getStatus();
					
					rcvPut += val[0];
					clsPut += val[1];
					rcvGet += val[2];
					clsGet += val[3];
					ttlQue += val[4];
				}
				queueStatus = "ItemQueue:" + rcvPut + ":" + clsPut + ":" + rcvGet + ":" + clsGet + ":" + ttlQue;
			}
			
			try
			{
				sleep(1000);
			}
			catch(Exception e) {}
		}
	}
	
	public String getProcessorStatus()
	{
		synchronized(processorStatus)
		{
			return processorStatus;
		}
	}
	
	public String getSelectorStatus()
	{
		synchronized(selectorStatus)
		{
			return selectorStatus;
		}
	}
	
	public String getQueueStatus()
	{
		synchronized(queueStatus)
		{
			return queueStatus;
		}
	}
	
	public String getCpuStatus()
	{
		synchronized(cpuStatus)
		{
			return "CPU:" + cpuStatus;
		}
	}
	
	public String getMemoryStatus()
	{
		OperatingSystemMXBean bean =
		ManagementFactory.getOperatingSystemMXBean();
		if ( ! (bean instanceof com.sun.management.OperatingSystemMXBean) ) return "0/0";
		String ret = ((com.sun.management.OperatingSystemMXBean)bean).getFreePhysicalMemorySize() + "/" + ((com.sun.management.OperatingSystemMXBean)bean).getTotalPhysicalMemorySize();
		return ret;
	}
	
	public String getStorageStatus()
	{
		File f = new File(partition);
		String ret = "DISK:" + f.getTotalSpace() + ":" + f.getUsableSpace() + ":" + f.getFreeSpace();
		return ret;
	}
	
	class CheckCpuStatus extends Thread
	{
		ResetStatus parent;
		
		public CheckCpuStatus(ResetStatus parent)
		{
			this.parent = parent;
			
			start();
		}
		
		public void run()
		{
			while ( true )
			{
				ThreadMXBean TMB = ManagementFactory.getThreadMXBean();
				long time = new Date().getTime() * 1000000;
				long cput = 0;
				double cpuperc = -1;
				
				while ( true )
				{
					if( TMB.isThreadCpuTimeSupported() )
					{
						if(new Date().getTime() * 1000000 - time > 1000000000)
						{
							time = new Date().getTime() * 1000000;
							cput = TMB.getCurrentThreadCpuTime();
						}
						
						if(!TMB.isThreadCpuTimeEnabled())
						{       
							TMB.setThreadCpuTimeEnabled(true);
						}
						
						if(new Date().getTime() * 1000000 - time != 0)
						{
							cpuperc = (TMB.getCurrentThreadCpuTime() - cput) / (new Date().getTime() * 1000000.0 - time) * 100.0;        
						}
					}
					else
					{   
						cpuperc = -2;
					}
					
					parent.cpuStatus = (int)cpuperc + "";
					
					try { sleep(100); } catch(Exception e) {}
				}
			}
		}
	}
}