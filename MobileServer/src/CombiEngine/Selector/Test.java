package CombiEngine.Selector;

public class Test extends Thread
{
	static int sslProcessorCount = 0;
	static int sslProcessorRequestCount = 0;
	static int handshakeCount = 0;
	static int encryptCount = 0;
	static int decryptCount = 0;
	static int cntItem = 0;
	static int rcvItem = 0;
	static int dscItem = 0;
	static int sslSendItem = 0;
	static int sslSender = 0;
	
	public Test()
	{
		this.start();
	}
	
	public static synchronized void addSSLRequest()
	{
		sslProcessorRequestCount++;
	}
	
	public static synchronized void removeSSLRequest()
	{
		sslProcessorRequestCount--;
	}
	
	public static synchronized void addSSL()
	{
		sslProcessorCount++;
	}
	
	public static synchronized void removeSSL()
	{
		sslProcessorCount--;
	}
	
	public static synchronized void addHandshake()
	{
		handshakeCount++;
	}
	
	public static synchronized void removeHandshake()
	{
		handshakeCount--;
	}
	
	public static synchronized void addEncrypt()
	{
		encryptCount++;
	}
	
	public static synchronized void removeEncrypt()
	{
		encryptCount--;
	}
	
	public static synchronized void addDecrypt()
	{
		decryptCount++;
	}
	
	public static synchronized void removeDecrypt()
	{
		decryptCount--;
	}
	
	public static synchronized void addConnectItem()
	{
		cntItem++;
	}
	
	public static synchronized void removeConnectItem()
	{
		cntItem--;
	}
	
	public static synchronized void addReceiveItem()
	{
		rcvItem++;
	}
	
	public static synchronized void removeReceiveItem()
	{
		rcvItem--;
	}
	
	public static synchronized void addDisconnectItem()
	{
		dscItem++;
	}
	
	public static synchronized void removeDisconnectItem()
	{
		dscItem--;
	}
	
	public static synchronized void addSSLSendItem()
	{
		sslSendItem++;
	}
	
	public static synchronized void removeSSLSendItem()
	{
		sslSendItem--;
	}
	
	public static synchronized void addSSLSender()
	{
		sslSender++;
	}
	
	public static synchronized void removeSSLSender()
	{
		sslSender--;
	}
	
	public void run()
	{
		while ( true )
		{
			try
			{
				sleep(3000);
			}
			catch(Exception e) {}
			
			System.out.println("SSLCount : " + sslProcessorCount);
			System.out.println("HandshakeCount : " + handshakeCount);
			System.out.println("EncryptCount : " + encryptCount);
			System.out.println("DecryptCount : " + decryptCount);
			System.out.println("sslProcessorRequestCount : " + sslProcessorRequestCount);
			
			if ( ByteBufferPool.appBuffer != null )
			{
				System.out.println("appBuffer : " + ByteBufferPool.appBuffer.size());
				System.out.println("netBuffer : " + ByteBufferPool.netBuffer.size());
			}
			
			System.out.println("ConnectItem : " + cntItem);
			System.out.println("ReceiveItem : " + rcvItem);
			System.out.println("DisconnectItem : " + dscItem);
			System.out.println("SSLSendItem : " + sslSendItem);
			System.out.println("SSLSender : " + sslSender);
		}
	}
}
