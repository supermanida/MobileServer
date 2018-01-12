package CombiEngine.Mobile;

import CombiEngine.Selector.Connector;
import CombiEngine.Codec.AmCodec;
import java.util.Map;
import java.util.Vector;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ParallelService extends Thread
{
	public ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap;
	//private ConcurrentHashMap<String, ArrayList<Connector>> userMap;
	
	private String ip;
	private int port;
	private String reservedMessagePath;
	
	private final String characterEncoding = "UTF-8";
	
	private ServerSocket ss = null;
	private Socket sc = null;
	private InputStreamReader ir = null;
	private OutputStreamWriter ow = null;
	private BufferedReader br = null;
	private BufferedWriter bw = null;
	
	private char paramDelemeter = '\t';
	private char commandDelemeter = '\f';
	
	private String command;
	private ArrayList<String> param = null;
	
	protected AmCodec codec = null;
	private boolean m_bDebug = true;
	private String backupSuccess = "SUCCESS_BACKUP";
	public boolean isConnected = false;
	
	//MobileService parent = null; //2017-10-20
	
	//2017-10-20
	//public ParallelService(String ip, int port, String reservedFolder, ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap, MobileService mobile)
	public ParallelService(String ip, int port, String reservedFolder, ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap)
	{
		this.userMap = userMap;
		this.reservedMessagePath = reservedFolder;
		this.backupSuccess = reservedFolder + File.separator + backupSuccess;
		
		//parent = mobile; //2017-10-20
		
		File reservedFolderFile = new File(reservedFolder);
		if ( !reservedFolderFile.exists() ) reservedFolderFile.mkdirs();
		
		this.ip = ip;
		this.port = port;
		
		this.codec = new AmCodec();
		
		this.command = "";
		this.param = new ArrayList<String>();
		
		this.start();
	}
	
	//2017-10-20
	public void sendUserInfoRelay(String command, String id, String phoneType, String data, String version, String rcvUserIds)
    {
			   try
               {
				   	   System.out.println("[ParallelService] sendUserInfoRelay ->" + "USERINFO_RELAY" + paramDelemeter + command + paramDelemeter + id + paramDelemeter + phoneType + paramDelemeter + data + paramDelemeter +  version + paramDelemeter + rcvUserIds);
                       send("USERINFO_RELAY" + paramDelemeter + command + paramDelemeter + id + paramDelemeter + phoneType + paramDelemeter + data + paramDelemeter +  version + paramDelemeter + rcvUserIds);
               }
               catch(Exception e)
               {
                       e.printStackTrace();
               }
    }
	//
	
	private void receive(String command, ArrayList<String> param)
	{
		if ( command.equals("USERCONNECTED") && param.size() == 2 && !param.get(0).equals("") )
		{
			onUserConnected(param.get(0), param.get(1));
		}
		else if ( command.equals("SUCCESS") && param.size() == 3 && !param.get(0).equals("") )
		{
			onSuccess(param.get(0), param.get(1), param.get(2));
		}
		else if ( command.equals("RELAY") && param.size() == 3 )
		{
			String id = param.get(0);
			String phoneType = param.get(1);
			
			ConcurrentLinkedQueue<Connector> userAr = userMap.get(id);
			//ArrayList<Connector> userAr = userMap.get(id);
			
			if ( userAr == null || userAr.size() == 0 ) return;
			
			for( Connector conn : userAr )
			{
			        if(conn.itemType.equals( phoneType ))
			                conn.send(param.get(2) + commandDelemeter);
			}
			
			/*for ( int i = 0 ; i < userAr.size() ; i++ )
			{
				if ( userAr.get(i).itemType.equals(phoneType) )
				{
					userAr.get(i).send(param.get(2) + commandDelemeter);
				}
			}*/
		}
		else if ( command.equals("SEND") && param.size() == 2 )
		{
			String id = param.get(0);
			
			ConcurrentLinkedQueue<Connector> userAr = userMap.get(id);
			//ArrayList<Connector> userAr = userMap.get(id);
			
			if ( userAr == null || userAr.size() == 0 ) return;
			
			for( Connector conn : userAr)
			{
			        conn.send( param.get( 1 ) + commandDelemeter );
			}
			
			/*for ( int i = 0 ; i < userAr.size() ; i++ )
			{
				userAr.get(i).send(param.get(1) + commandDelemeter);
			}*/
		}
		//2017-10-20
		else if(command.equals("USERINFO_RELAY"))
		{
			String parse = param.get(0);
			String userId = param.get(1);
		    String phoneType = param.get(2);
			String data = param.get(3);
			String dataVersion = param.get(4);
			String rcvIds = param.get(5);
			
			if(parse.startsWith("NAMEUPDATE"))
			{
				System.out.println("[ParallelService] parent.updateNameInfo call. param:"+param);
				//parent.updateNameInfo(userId, data, dataVersion, rcvIds);
				
				//UserObject data, version refresh
				/*UserObject user = hubs.dataBean.users.get(userId));
				if(user != null)
				{
					user.setName(data);
					user.setNameVersion(Integer.parseInt(dataVersion));
				}
				else return;*/

				//observer conn.send
				/*ConcurrentLinkedQueue<ConnectorWithList> obs = hubs.dataBean.getObservers(user.id);
		        if ( obs == null ) return;

		        for (ConnectorWithList connList : obs)
		        {
					connList.conn.send("NAMEUPDATE\t" + user.id + "\t" + user.getNameVersion() + "\t" + user.getNameField() + commandDelemeter);
					connList.conn.send("UPDATEEND\t0" + commandDelemeter);
		        }*/
			}
			else if(parse.startsWith("NICKUPDATE"))
			{
				//parent.updateNickInfo(userId, data, dataVersion, rcvIds);
				
				//UserObject data, version refresh
				/*UserObject user = hubs.dataBean.users.get(userId));
				if(user != null)
				{
					user.setNick(data);
					user.setNickVersion(Integer.parseInt(dataVersion));
				}
				else return;*/

				//observer conn.send
				/*ConcurrentLinkedQueue<ConnectorWithList> obs = hubs.dataBean.getObservers(user.id);
		        if ( obs == null ) return;

		        for (ConnectorWithList connList : obs)
		        {
					connList.conn.send("NICKUPDATE\t" + user.id + "\t" + user.getNickVersion() + "\t" + user.param2 + commandDelemeter);
					connList.conn.send("UPDATEEND\t0" + commandDelemeter);
		        }*/
			}
			else if(parse.startsWith("PROFILE_DELETE"))
			{
				//parent.updatePhotoDeleteInfo(userId, rcvIds);
				
				//UserObject user = hubs.dataBean.users.get(userId));
				//if(user == null) return;
				
				//observer conn.send
				/*ConcurrentLinkedQueue<ConnectorWithList> obs = hubs.dataBean.getObservers(user.id);
		        if ( obs == null ) return;

		        for (ConnectorWithList connList : obs)
		        {
					dataBean.sendMessage(connList.conn, "PHOTO_DELETE\t" + userId);
					connList.conn.send("PHOTO_DELETE\t" + userId + commandDelemeter);
		        }*/
			}
			else if(parse.startsWith("PROFILEUPDATE"))
			{
				//parent.updatePhotoInfo(userId, dataVersion, rcvIds);
				
				//UserObject data, version refresh
				/*UserObject user = hubs.dataBean.users.get(userId));
				if(user != null)
				{
					user.setPhotoVersion(Integer.parseInt(dataVersion));
				}
				else return;*/

				//observer conn.send
				/*ConcurrentLinkedQueue<ConnectorWithList> obs = hubs.dataBean.getObservers(user.id);
		        if ( obs == null ) return;

		        for (ConnectorWithList connList : obs)
		        {
					connList.conn.send("PHOTOUPDATE\t" + userId + "\t" + dataVersion + commandDelemeter);
		        }*/
			}
			else if(parse.startsWith("LOGIN"))
			{
				//parent.updateLoginInfo(userId);
				
				/*UserObject user = hubs.dataBean.users.get(userId));
				if(user != null)
					user.mobileOn = 1;*/
			}
		}
		//
	}
	
	// �궗�슜�옄媛� �뿰寃곕릺�뿀�쓬�쓣 �븣由�
	public void sendUserConnected(String id, String phoneType)
	{
		try
		{
			send("USERCONNECTED" + paramDelemeter + id + paramDelemeter + phoneType);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// �궗�슜�옄媛� �뿰寃곕릺�뿀�쓬�쓣 �닔�떊�뻽�쓣 �븣
	private void onUserConnected(String id, String phoneType)
	{
		if ( id == null || id.equals("") ) return;
		
		File f = getFilePath(reservedMessagePath, id, phoneType);
		
		if ( !f.exists() )
		{
			sendRelay(id, phoneType, "RESERVED_END");
			return;
		}
		
		arrangeReservedFile(f);
		
		FileReader fr = null;
		BufferedReader br = null;
		
		try
		{
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			String line = null;
			
			sendRelay(id, phoneType, "RESERVED_START");
			
			while ( ( line = br.readLine() ) != null )
			{
				if ( fieldCount(line) >= 6 )
				{
					sendMessage(id, phoneType, line.substring(0, line.indexOf("\t")), line.substring(line.indexOf("\t") + 1).replaceAll("<BR>", "\n"));
					
					if ( line.indexOf("[READ_COMPLETE]") >= 0 ) removeMessageFile(id, line.substring(0, line.indexOf("\t")), phoneType);
				}
			}
			
			sendRelay(id, phoneType, "RESERVED_END");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception ee) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception ee) {} }
		}
	}
	
	// ���옣 硫붿떆吏� �궘�젣 �슂泥�
	public void sendSuccess(String id, String phoneType, String msgId)
	{
		if ( sc != null && sc.isConnected() )
		{
			try
			{
				send("SUCCESS" + paramDelemeter + id + paramDelemeter + phoneType + paramDelemeter + msgId);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			FileOutputStream fo = null;
			OutputStreamWriter ow = null;
			
			try
			{
				fo = new FileOutputStream(backupSuccess, true);
				ow = new OutputStreamWriter(fo, characterEncoding);
				
				ow.write(id + paramDelemeter + phoneType + paramDelemeter + msgId + "\n");
				ow.flush();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if ( fo != null ) { try { fo.close(); fo = null; } catch(Exception e) {} }
				if ( ow != null ) { try { ow.close(); ow = null; } catch(Exception e) {} }
			}
		}
	}
	
	// ���옣 硫붿떆吏� �궘�젣 �슂泥� �닔�떊
	private void onSuccess(String id, String phoneType, String msgId)
	{
		removeMessageFile(id, msgId, phoneType);
	}
	
	// 蹂닿� �릺�뿀�뜕 硫붿떆吏� �궘�젣 �슂泥� �쟾遺� �쟾�넚
	public void clearSuccessedMessages()
	{
		File backupSuccessFile = new File(backupSuccess);
		
		if ( !backupSuccessFile.exists() ) return;
		
		FileInputStream fi = null;
		InputStreamReader ir = null;
		BufferedReader br = null;
		
		try
		{
			fi = new FileInputStream(backupSuccessFile);
			ir = new InputStreamReader(fi, characterEncoding);
			br = new BufferedReader(ir);
			
			String line = null;
			
			while ( ( line = br.readLine() ) != null )
			{
				send("SUCCESS" + paramDelemeter + line);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fi != null ) { try { fi.close(); fi = null; } catch(Exception e) {} }
			if ( ir != null ) { try { ir.close(); ir = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
		}
		
		backupSuccessFile.delete();
	}
	
	// 蹂묐젹 �꽌踰꾧� �뿰寃� �릺�뿀�쓣 �븣
	private void sendUserOnlineList()
	{
		for( Map.Entry<String, ConcurrentLinkedQueue<Connector>> element : userMap.entrySet() )
		//for( Map.Entry<String, ArrayList<Connector>> element : userMap.entrySet() )
		{
			String id = element.getKey();
			//ArrayList<Connector> userList = element.getValue();
			ConcurrentLinkedQueue<Connector> userList = element.getValue();
			
			for(Connector conn : userList)
			{
			        sendUserConnected(id, conn.itemType);
			}
			
			/*for ( int i = 0 ; i < userList.size() ; i++ )
			{
				sendUserConnected(id, userList.get(i).itemType);
			}*/
		}
	}
	
	public void sendMessage(String id, String msgEncoded)
	{
		try
		{
			send("SEND" + paramDelemeter + id + paramDelemeter + msgEncoded);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void run()
	{
		StringBuffer sb = new StringBuffer();
		
		try
		{
			System.out.println("Parall Service new Connect ip:"+ip + ", port:"+port);
			sc = new Socket();
			sc.connect(new InetSocketAddress(ip, port), 1000);
			System.out.println("Parall Connect Success!");
			isConnected = true;
			
			ir = new InputStreamReader(sc.getInputStream(), characterEncoding);
			ow = new OutputStreamWriter(sc.getOutputStream(), characterEncoding);
			
			br = new BufferedReader(ir);
			bw = new BufferedWriter(ow);
			
			if ( !sc.isConnected() )
			{
				throw new Exception("Parallel is not connected");
			}
			
			clearSuccessedMessages();
			
			char[] receiveBuffer = new char[4096];
			
			int receiveSize = -1;
			
			while ( ( receiveSize = br.read(receiveBuffer, 0, 4096) ) >= 0 )
			{
				sb.append(new String(receiveBuffer, 0, receiveSize));
				
				processStringBuffer(sb);
			}
		}
		catch(SocketException se) {}
		catch(SocketTimeoutException se) {}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( sc != null ) { try { sc.close(); sc = null; } catch(Exception e) {} }
			if ( ir != null ) { try { ir.close(); ir = null; } catch(Exception e) {} }
			if ( ow != null ) { try { ow.close(); ow = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
			if ( bw != null ) { try { bw.close(); bw = null; } catch(Exception e) {} }
			
			isConnected = false;
		}
		
		sb.delete(0, sb.length());
		
		try
		{
			ss = new ServerSocket(port);
			
			char[] receiveBuffer = new char[4096];
			
			while ( true )
			{
				sc = ss.accept();
				
				try
				{
					isConnected = true;
					
					ir = new InputStreamReader(sc.getInputStream(), characterEncoding);
					ow = new OutputStreamWriter(sc.getOutputStream(), characterEncoding);
					
					br = new BufferedReader(ir);
					bw = new BufferedWriter(ow);
					
					if ( !sc.isConnected() )
					{
						throw new Exception("Parallel is disconnected");
					}
					
					clearSuccessedMessages();
					
					sendUserOnlineList();
					
					int receiveSize = -1;
					
					while ( ( receiveSize = br.read(receiveBuffer, 0, 4096) ) >= 0 )
					{
						sb.append(new String(receiveBuffer, 0, receiveSize));
						
						processStringBuffer(sb);
					}
				}
				catch(SocketException se) {}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					if ( sc != null ) { try { sc.close(); sc = null; } catch(Exception e) {} }
					if ( ir != null ) { try { ir.close(); ir = null; } catch(Exception e) {} }
					if ( ow != null ) { try { ow.close(); ow = null; } catch(Exception e) {} }
					if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
					if ( bw != null ) { try { bw.close(); bw = null; } catch(Exception e) {} }
					
					isConnected = false;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			return;
		}
	}
	
	private void processStringBuffer(StringBuffer sb)
	{
		command = "";
		param.clear();
		
		String nowString = "";
		short type = 0;
		
		int deletePosition = -1;
		
		for ( int i = 0 ; i < sb.length() ; i++ )
		{
			if ( sb.charAt(i) == paramDelemeter )
			{
				if ( type > 0 )
				{
					param.add(nowString);
				}
				
				nowString = "";
				
				type++;
			}
			else if ( sb.charAt(i) == commandDelemeter )
			{
				if ( type > 0 )
				{
					param.add(nowString);
				}
				
				nowString = "";
				
				type = 0;
				
				deletePosition = i + 1;
				
				System.out.println("[ParallserService] processString command:"+command + ", param:"+param);
				receive(command, param);
				
				command = "";
				param.clear();
			}
			else if ( type == 0 )
			{
				command += sb.charAt(i);
			}
			else
			{
				nowString += sb.charAt(i);
			}
		}
		
		if ( deletePosition >= 0 )
		{
			sb.delete(0, deletePosition);
		}
	}
	
	private void send(String msg) throws Exception
	{
		if ( bw == null ) return;
		
		if ( msg.charAt(msg.length() - 1) != commandDelemeter )
		{
			msg += commandDelemeter;
		}
		
		bw.write(msg);
		bw.flush();
	}
	
	private void sendMessage(String id, String phoneType, String msgId, String other)
	{
		String sendMessage = "NOTIFY\t" + msgId + "\t" + other;
		
		sendRelay(id, phoneType, sendMessage);
	}
	
	private void sendRelay(String id, String phoneType, String msg)
	{
		try
		{
			send("RELAY" + paramDelemeter + id + paramDelemeter + phoneType + paramDelemeter + codec.EncryptSEED(msg));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public File getFilePath(String baseDir, String itemKey, String phoneType)
	{
		String pathName = baseDir + java.io.File.separator;
		
		if ( itemKey.length() > 6 )
		{
			pathName += itemKey.substring(0,2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2,4);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(4,6);
		}
		else if ( itemKey.length() > 4 )
		{
			pathName += itemKey.substring(0,2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2,4);
		}
		else if ( itemKey.length() > 2 )
		{
			pathName += itemKey.substring(0,2);
		}
		else
		{
			pathName += "{SHORTID}";
		}
		
		java.io.File f = new java.io.File(pathName);
		f.mkdirs();
		
		pathName += java.io.File.separator;
		pathName += itemKey;
		pathName += "." + phoneType;
		
		return new File(pathName);
	}
	
	public void removeMessageFile(String id, String msgId, String phoneType)
	{
		File f = getFilePath(reservedMessagePath, id, phoneType);
		
		if ( !f.exists() ) return;
		
		int len = msgId.length();
		
		FileReader fr = null;
		BufferedReader br = null;
		
		ArrayList<String> lines = new ArrayList<String>();
		
		try
		{
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			String line = null;
			
			while ( ( line = br.readLine() ) != null )
			{
				if ( line.length() < ( len + 1 ) || !line.substring(0,len + 1).equals(msgId + "\t") )
				{
					lines.add(line);
				}
				else
				{
					if ( m_bDebug ) System.out.println("Deleted : " + msgId);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception ee) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception ee) {} }
		}
		
		if ( lines.size() > 0 )
		{
			FileWriter fw = null;
			
			try
			{
				fw = new FileWriter(f);
				
				for ( int i = 0 ; i < lines.size() ; i++ )
				{
					if ( fieldCount(lines.get(i)) >= 6 )
					{
						fw.write(lines.get(i) + "\n");
						fw.flush();
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if ( fw != null ) { try { fw.close(); fw = null; } catch(Exception ee) {} }
			}
		}
		else
		{
			if ( m_bDebug ) System.out.println("DeleteNullFile");
			f.delete();
		}
	}
	
	private int fieldCount(String s)
	{
		int cnt = 0;
		
		for ( int i = 0 ; i < s.length() ; i++ )
		{
			if(i == (s.length()-1))
				cnt++;

			if ( s.charAt(i) == '\t')
				cnt++;
	
			/*
			if ( s.charAt(i) == '\t' || i == ( s.length() - 1 ) )
			{
				cnt++;
			}
			*/
		}
		
		return cnt;
	}
	
	public void arrangeReservedFile(File f)
	{
		FileReader fr = null;
		BufferedReader br = null;
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		Vector<String> lines = new Vector<String>();
		
		try
		{
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			String line = null;
			
			while ( ( line = br.readLine() ) != null )
			{
				StringTokenizer st = new StringTokenizer(line, "\t");
				
				String msgId = st.nextToken();
				
				if ( st.countTokens() >= 4 && !msgId.equals("") )
				{
					lines.add(line);
				}
			}
			
			if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
			
			fw = new FileWriter(f, false);
			bw = new BufferedWriter(fw);
			
			for ( int i = 0 ; i < lines.size() ; i++ )
			{
				bw.write(lines.get(i) + "\n");
				bw.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
			if ( fw != null ) { try { fw.close(); fw = null; } catch(Exception e) {} }
			if ( bw != null ) { try { bw.close(); bw = null; } catch(Exception e) {} }
		}
	}
}
