package CombiEngine.Mobile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.JSONObject;
import CombiEngine.Codec.AmCodec;
import CombiEngine.Processor.CombiService;
import CombiEngine.Selector.Connector;
import CombiEngine.Selector.SelectService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.BufferedWriter;
import java.util.Vector;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.sql.DriverManager;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//2017-12-13
import kr.co.ultari.dbhandler.DBCPConnectionMgr;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
//

public class MobileService extends CombiService implements Runnable {
	private final String charset;
	public ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap;
	public ConcurrentHashMap<String, String> config;
	public DBCPConnectionMgr dbcpMsger, dbcpCert; // 2017-12-13
	public ConcurrentHashMap<String, String> userSerialMap;
	protected AmCodec codec;
	public String serialPath;
	String reservedMessagePath;
	protected String query = ""; // 2017-12-13
	long messageIndexBase;
	long messageIndex;
	boolean m_bDebug;
	String iphoneAuthToken;
	// Thread thread; 2018-01-11
	private String m_SmsReceive;
	private String m_Sms;
	private AndroidManager androidManager;
	public ConcurrentHashMap<String, PushItem> sb;
	private int retryCount = 0;
	public ExecutorService executorsPool; //2017-12-13 
	private ParallelService parallel = null;
	private int availableDataCount = 100; //2018-01-11

	public MobileService(ConcurrentHashMap<String, String> config) {
		this.config = config;
		this.charset = config.get("ENCODING");
		this.serialPath = config.get("SERIAL_PATH");
		this.reservedMessagePath = config.get("RESERVED_PATH");
		this.query = config.get("QUERY_GETID");
		this.userMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>>();
		this.codec = new AmCodec();
		this.userSerialMap = new ConcurrentHashMap<String, String>();

		if (config.get("PARALLEL_IP") != null && !config.get("PARALLEL_IP").equals("")
				&& config.get("PARALLEL_PORT") != null && !config.get("PARALLEL_PORT").equals(""))
			parallel = new ParallelService(config.get("PARALLEL_IP"), Integer.parseInt(config.get("PARALLEL_PORT")),
					reservedMessagePath, userMap);

		this.sb = new ConcurrentHashMap<String, PushItem>();
		retryCount = Integer.parseInt(config.get("PUSH_RETRY_COUNT"));
		boolean m_bDebug = false;
		if (config.get("DEBUG") != null && config.get("DEBUG").equals("Y"))
			m_bDebug = true;
		this.m_Sms = config.get("SMSG");
		this.m_SmsReceive = config.get("SMSG_RECEIVE");
		this.m_bDebug = m_bDebug;
		this.iphoneAuthToken = config.get("IPHONE_AUTH_FILE");
		this.messageIndexBase = Calendar.getInstance().getTimeInMillis();
		this.messageIndex = 0;

		//2018-01-12 count 10->100 pool
		new SelectService(Integer.parseInt(config.get("SERVICE_PORT")), this, 100, 3100, "ultari.keystore",
				"ultari".toCharArray());

		new ManagementFiles(reservedMessagePath, Integer.parseInt(config.get("MAX_LINES")),
				Integer.parseInt(config.get("TERMINATE")));
		this.androidManager = new AndroidManager();

		// 2017-12-13
		try {
			this.dbcpMsger = new DBCPConnectionMgr(config.get("MSGER_DB_DRIVER"), config.get("MSGER_DB_URL"), config.get("MSGER_DB_USER"),
					config.get("MSGER_DB_PASSWORD"), config.get("MSGER_DB_MAX_CNT"), config.get("MSGER_DB_IDLE_CNT"),
					config.get("MSGER_POOL_NAME"));
			
			this.dbcpCert = new DBCPConnectionMgr(config.get("CERT_DB_DRIVER"), config.get("CERT_DB_URL"), config.get("CERT_DB_USER"),
					config.get("CERT_DB_PASSWORD"), config.get("CERT_DB_MAX_CNT"), config.get("CERT_DB_IDLE_CNT"),
					config.get("CERT_POOL_NAME"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//

		executorsPool = Executors.newFixedThreadPool( Integer.parseInt(config.get("THREAD_POOL_COUNT") ));

		// 2018-01-11
		// thread = new Thread(this);
		// thread.start();
		availableDataCount = Integer.parseInt(config.get("MAX_LINES"));
	}

	public void run() {
		// 2018-01-11
		/*
		 * try { while (true) { if (sb.size() > 0) { Iterator<PushItem> it =
		 * sb.values().iterator(); while (it.hasNext()) { PushItem item = it.next(); //
		 * retry count max:3 if (item.getPushRetryCount() > retryCount) { it.remove();
		 * continue; } item.setPushRetryCount(); // count++ List<String> list = new
		 * ArrayList<String>(); list.add(item.getDeviceToken());
		 * 
		 * String msg = ""; if (item.getChatRoomId().indexOf("REQUESTMAPINFO") >= 0) msg
		 * = codec.EncryptSEED(item.getChatRoomId()); else msg =
		 * codec.EncryptSEED(item.getUserId() + "#" + item.getChatRoomId() + "#" +
		 * item.getUserName() + "#" + item.getSendTime());
		 * 
		 * JSONObject obj = new JSONObject(); obj.put("message", msg); exec.execute(new
		 * SendPush(obj.toString(), list)); } } Thread.sleep(30000); } } catch
		 * (Exception e) { e.printStackTrace(); } finally { exec.shutdown(); try {
		 * exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		 * 
		 * } catch (InterruptedException e) { e.printStackTrace(); } }
		 */
	}

	public void OnConnect(Connector conn) {
		conn.item = (Object) new StringBuffer();
	}

	public void OnClose(Connector conn) {
		if (conn.itemKey != null) {
			removeUser(conn.itemKey, conn);
		}

		OnCloseCustom(conn);
	}

	public String enc(int type, String s) {
		try {
			if (type == 0)
				return new String(s.getBytes(), "UTF-8");
			else if (type == 1)
				return new String(s.getBytes(), "KSC5601");
			else if (type == 2)
				return new String(s.getBytes(), "ISO-8859-1");
			else if (type == 3)
				return new String(s.getBytes("ISO-8859-1"), "KSC5601");
			else if (type == 4)
				return new String(s.getBytes("ISO-8859-1"), "UTF-8");
			else if (type == 5)
				return new String(s.getBytes("UTF-8"), "KSC5601");
			else if (type == 6)
				return new String(s.getBytes("UTF-8"), "ISO-8859-1");
			else if (type == 7)
				return new String(s.getBytes("KSC5601"), "UTF-8");
			else
				return new String(s.getBytes("KSC5601"), "ISO-8859-1");
		} catch (Exception e) {
			return "";
		}
	}

	public String onReport(Connector conn) {
		StringBuffer sb = new StringBuffer();
		sb.append("Total connected users : " + userMap.size() + "\n");
		sb.append("\n");
		java.util.Iterator<String> it = userMap.keySet().iterator();
		while (it.hasNext()) {
			String id = it.next();
			ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
			sb.append(id + " : " + conns.size());
			Iterator<Connector> it2 = conns.iterator();
			while (it2.hasNext()) {
				Connector con = it2.next();
				sb.append(", " + con.getIP());
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public void OnReceive(Connector conn, byte[] buf, int size) {
		StringBuffer sb = (StringBuffer) conn.item;

		if (sb == null) {
			conn.close();
			return;
		}
		try {
			String m = new String(buf, 0, size);
			if (m.indexOf("REPORT") == 0) {
				conn.send(onReport(conn));
				conn.setAutoClose(true);
				return;
			}
			sb.append(new String(buf, 0, size, charset));
		} catch (UnsupportedEncodingException ue) {
			ue.printStackTrace();
			return;
		}
		while (sb.indexOf("\f") >= 0) {
			String substr = sb.substring(0, sb.indexOf("\f"));
			sb.delete(0, sb.indexOf("\f") + 1);
			substr = codec.DecryptSEED(substr);

			System.out.println("[MobileService] OnRecieve RCV : " + substr);

			int lastI = 0;
			String command;
			ArrayList<String> param = new ArrayList<String>();
			int type = 0;
			String nowStr = "";
			command = "";
			param.clear();
			for (int i = 0; i < substr.length(); i++) {
				if (substr.charAt(i) == '\t') {
					if (type == 0) {
						command = nowStr;
					} else {
						param.add(nowStr);
					}
					type++;
					nowStr = "";
				} else if (i == (substr.length() - 1)) {
					nowStr += substr.charAt(i);
					if (type == 0) {
						command = nowStr;
					} else {
						param.add(nowStr);
					}
					try {
						Process(command, param, conn);
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				} else {
					nowStr += substr.charAt(i);
				}
			}
			param = null;
		}
	}

	public static String getYYYYMMDDHHNNSS(String type) {
		SimpleDateFormat sdf = new SimpleDateFormat(type);
		Date date = new Date();
		String now = sdf.format(date);
		return now;
	}

	public void Process(String command, ArrayList<String> param, Connector conn) {

		if (command.equals("HI") && param.size() == 3) {
			String id = param.get(0);
			String phoneType = param.get(1);
			String serial = param.get(2);
			conn.itemType = phoneType;
			if (m_bDebug)
				System.out.println(id + ":" + phoneType + ":" + serial);
			appendUser(id, conn);
			if (phoneType != null && !phoneType.equals("PC")) {
				if (userSerialMap.get(id) == null) {
					updatePhoneSerial(id, serial, phoneType);
					userSerialMap.put(id, serial);
				} else {
					if (userSerialMap.get(id).equals(serial)) {
					} else {
						updatePhoneSerial(id, serial, phoneType);
						userSerialMap.put(id, serial);
					}
				}
			}
			sendReservedMessage(conn, phoneType);
			onConnected(conn);
		} else if (command.equals("GCM_RECEIVE") && param.size() == 1) {
			if (param != null)
				sb.remove(param.get(0));
		} else if (command.equals("CHECK_RESERVED")) {
			boolean isCheck = checkNewMessage(param.get(0), "Android");
			if (isCheck)
				conn.send(codec.EncryptSEED("NEW_MESSAGE") + "\f");
			else
				conn.send(codec.EncryptSEED("EMPTY") + "\f");
		} else if (command.equals("NOPUSH") && param.size() == 1) {
			updatePhoneSerial(param.get(0), "NoAlarm", "iPhone");
		} else if (command.equals("SETPUSH") && param.size() == 3) {
			updatePhoneSerial(param.get(0), param.get(2), param.get(1));
		} else if (command.equals("SUCCESS") && param.size() == 1) {
			String msgId = param.get(0);
			removeMessageFile(conn.itemKey, msgId, conn.itemType);

			if (parallel != null)
				parallel.sendSuccess(conn.itemKey, conn.itemType, msgId);
		} else if (command.equals("noop")) {
			conn.send(codec.EncryptSEED("noop") + "\f");
		} else if (command.equals("CHAT") && param.size() == 7) {
			String fromId = param.get(0);
			String fromName = param.get(1);
			String fromNickName = param.get(2);
			String toId = param.get(3);
			String roomId = param.get(4);
			String date = param.get(5);
			String message = param.get(6);
			fromId = fromId.replaceAll("\r", "");
			fromName = fromName.replaceAll("\r", "");
			fromNickName = fromNickName.replaceAll("\r", "");
			toId = toId.replaceAll("\r", "");
			roomId = roomId.replaceAll("\r", "");
			date = date.replaceAll("\r", "");
			message = message.replaceAll("\r", "");
			ProcessChat(fromId, fromName, fromNickName, toId, roomId, date, message, conn);
		} else if (command.equals("MESSAGE") && param.size() == 6) {
			String rcvId = param.get(0);
			String senderId = param.get(1);
			String senderName = param.get(2);
			String senderNickName = param.get(3);
			String title = param.get(4);
			String content = param.get(5);
			String date = null;
			date = getYYYYMMDDHHNNSS("yyyyMMddHHmmss");
			send(rcvId, senderId + "<BR>" + senderName + "<BR>" + senderNickName, date, title, content, m_Sms, null);
		} else if (command.equals("MESSAGE") && param.size() == 7) {
			String rcvId = param.get(0);
			String senderId = param.get(1);
			String senderName = param.get(2);
			String senderNickName = param.get(3);
			String title = param.get(4);
			String content = param.get(5);
			String date = param.get(6);
			File f = getFilePath(serialPath, rcvId);
			if (f.exists()) {
				send(rcvId, senderId + "<BR>" + senderName + "<BR>" + senderNickName, date, title, content, m_Sms,
						null);
			}
		} else if (command.equals("NOTIFY") && param.size() == 8) {
			String msgId = param.get(0);
			String fromId = param.get(1);
			String fromName = param.get(2);
			String fromNickName = param.get(3);
			String toId = param.get(4);
			String roomId = param.get(5);
			String date = param.get(6);
			String message = param.get(7);
			fromId = fromId.replaceAll("\n", "<BR>");
			fromName = fromName.replaceAll("\n", "<BR>");
			fromNickName = fromNickName.replaceAll("\n", "<BR>");
			toId = toId.replaceAll("\n", "<BR>");
			roomId = roomId.replaceAll("\n", "<BR>");
			date = date.replaceAll("\n", "<BR>");
			message = message.replaceAll("\n", "<BR>");
			fromId = fromId.replaceAll("\r", "");
			fromName = fromName.replaceAll("\r", "");
			fromNickName = fromNickName.replaceAll("\r", "");
			toId = toId.replaceAll("\r", "");
			roomId = roomId.replaceAll("\r", "");
			date = date.replaceAll("\r", "");
			message = message.replaceAll("\r", "");

			conn.send(codec.EncryptSEED("SUCCESS\t" + msgId) + "\f");
			ProcessChat(fromId, fromName, fromNickName, toId, roomId, date, message, conn);
		} else if (command.equals("REQUESTMAPINFO")) {
			try {
				String fromId = param.get(0);
				String targetId = param.get(1);
				String reqTime = param.get(2);
				String roomId = param.get(3);

				String[] targetArray = targetId.split(",");
				HashMap<String, String> serialMap = new HashMap<String, String>();
				for (String tId : targetArray) {
					if (tId.equals(fromId))
						continue;
					String tmpSerial = getSerial(tId);
					if (tmpSerial != null && tmpSerial.contains("Android"))
						serialMap.put(tId, tmpSerial.substring(tmpSerial.indexOf("\t") + 1));
				}

				List<String> list = new ArrayList<String>();
				for (String tId : serialMap.keySet()) {
					String token = serialMap.get(tId);
					if (token == null || token.equals("NoC2dm") || token.equals(""))
						continue;
					sb.put(tId, new PushItem(fromId, token, "REQUESTMAPINFO#" + fromId + "#" + reqTime + "#" + roomId,
							"", 1, ""));
					list.add(token);
				}

				// multi gcm send
				String msg = codec.EncryptSEED("REQUESTMAPINFO#" + fromId + "#" + reqTime + "#" + roomId);

				JSONObject obj = new JSONObject();
				obj.put("message", msg);

				if (list.size() > 1000) {
					List<List<String>> arrList = listSplit(list, 1000);
					for (int i = 0; i < arrList.size(); i++)
						executorsPool.execute(new SendPush(obj.toString(), arrList.get(i)));
				} else
					executorsPool.execute(new SendPush(obj.toString(), list));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (command.equals("GPSINFO")) {
			String msgId = param.get(0);
			String fromId = param.get(1);
			String fromName = param.get(2);
			String fromNickName = param.get(3);
			String toId = param.get(4);
			String roomId = param.get(5);
			String date = param.get(6);
			String message = param.get(7);
			fromId = fromId.replaceAll("\n", "<BR>");
			fromName = fromName.replaceAll("\n", "<BR>");
			fromNickName = fromNickName.replaceAll("\n", "<BR>");
			toId = toId.replaceAll("\n", "<BR>");
			roomId = roomId.replaceAll("\n", "<BR>");
			date = date.replaceAll("\n", "<BR>");
			message = message.replaceAll("\n", "<BR>");
			fromId = fromId.replaceAll("\r", "");
			fromName = fromName.replaceAll("\r", "");
			fromNickName = fromNickName.replaceAll("\r", "");
			toId = toId.replaceAll("\r", "");
			roomId = roomId.replaceAll("\r", "");
			date = date.replaceAll("\r", "");
			message = message.replaceAll("\r", "");

			conn.itemType = "Android";

			ProcessChat(fromId, fromName, fromNickName, toId, roomId, date, message, conn);
			appendMessageFile(fromId, msgId, fromName, date, "CHAT", "[SUCCESS]", roomId, "Android");
		} else {
			ProcessCustom(command, param, conn);
		}
	}

	public void onConnected(Connector conn) {
	}

	public void ProcessChat(String fromId, String fromName, String fromNickName, String toId, String roomId,
			String date, String message, Connector conn) {
		if (message.indexOf("[READ_COMPLETE]") < 0 && message.indexOf("[ROOM_KEY]") < 0
				&& message.indexOf("[ROOM_IN]") < 0 && message.indexOf("[ROOM_OUT]") < 0)
			checkAndroid(toId, fromId, conn, roomId, fromName);

		send(toId, fromId + "\n" + fromName + "\n" + fromNickName, date, "CHAT", message, roomId, fromId, conn);
	}

	public static List<List<String>> listSplit(List<String> list, int i) {
		List<List<String>> out = new ArrayList<List<String>>();
		int size = list.size();
		int number = size / i;
		int remain = size % i;
		if (remain != 0) {
			number++;
		}
		for (int j = 0; j < number; j++) {
			int start = j * i;
			int end = start + i;
			if (end > list.size()) {
				end = list.size();
			}
			out.add(list.subList(start, end));
		}
		return out;
	}

	private void checkAndroid(String targetId, String fromId, Connector conn, String roomId, String name) {
		try {
			if (roomId.startsWith("SendMapData")) {
				return;
			}

			if (roomId.indexOf("<BR>") >= 0)
				roomId = roomId.substring(0, roomId.indexOf("<BR>"));
			String[] targetArray = targetId.split(",");
			HashMap<String, String> serialMap = new HashMap<String, String>();
			for (String tId : targetArray) {
				if (tId.equals(fromId))
					continue;
				String tmpSerial = getSerial(tId);
				if (tmpSerial != null && tmpSerial.contains("Android")) {
					serialMap.put(tId, tmpSerial.substring(tmpSerial.indexOf("\t") + 1));
				}
			}
			String retStr = null;
			retStr = getYYYYMMDDHHNNSS("yyyyMMddHHmmssSS");
			List<String> list = new ArrayList<String>();
			for (String tId : serialMap.keySet()) {

				String token = serialMap.get(tId);
				if (token == null || token.equals("NoC2dm") || token.equals(""))
					continue;

				sb.put(tId, new PushItem(fromId, token, roomId, name, 1, retStr));
				list.add(token);
			}
			// multi gcm send
			String msg = codec.EncryptSEED(fromId + "#" + roomId + "#" + name + "#" + retStr);

			JSONObject obj = new JSONObject();
			obj.put("message", msg);
			if (list.size() > 1000) {
				List<List<String>> arrList = listSplit(list, 1000);
				for (int i = 0; i < arrList.size(); i++)
					executorsPool.execute(new SendPush(obj.toString(), arrList.get(i)));
			} else
				executorsPool.execute(new SendPush(obj.toString(), list));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void ProcessCustom(String command, ArrayList<String> param, Connector conn) {
	}

	public void OnCloseCustom(Connector conn) {
	}

	public void send(String idListString, String senderName, String date, String title, String message, String url) {
		send(idListString, senderName, date, title, message, url, "", null);
	}

	public void send(String idListString, String senderName, String date, String title, String message, String url,
			Connector conn) {
		send(idListString, senderName, date, title, message, url, "", conn);
	}

	public void sendiPhone(String msgId, String id, String senderName, String date, String title, String message,
			String url, String myId, String serial) {

		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
		message = message.replaceAll("\n", "<BR>");
		message = message.replaceAll("\r", "");
		message = message.replaceAll("\t", "<TAB>");
		appendMessageFile(id, msgId, senderName, date, title, message, url, "iPhone");
		boolean sended2iPhone = false;
		if (conns != null) {
			Iterator<Connector> it = conns.iterator();
			while (it.hasNext()) {
				Connector conn = it.next();
				if (conn.itemType != null && conn.itemType.equals("iPhone")) {
					sended2iPhone = true;
					sendMessage(conn, msgId, senderName, date, title, message, url);
					if (message.indexOf("[READ_COMPLETE]") >= 0)
						removeMessageFile(id, msgId, "iPhone");
					break;
				}
			}
		}
		if (message.indexOf("[READ_COMPLETE]") >= 0)
			return;
		if (!sended2iPhone && !myId.equals(id)) {
			String tSenderName = senderName;
			if (tSenderName.indexOf("\n") > 0)
				tSenderName = tSenderName.substring(tSenderName.indexOf("\n") + 1);
			if (tSenderName.indexOf("\n") > 0)
				tSenderName = tSenderName.substring(0, tSenderName.indexOf("\n"));
			if (!message.equals(""))
				new iPhoneAlertSender(id, iphoneAuthToken, serial, tSenderName + ":" + message.replaceAll("<BR>", " "));
		}
	}

	public void sendAndroid(String msgId, String id, String senderName, String date, String title, String message,
			String url, String myId) {

		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
		message = message.replaceAll("\n", "<BR>");
		message = message.replaceAll("\r", "");
		message = message.replaceAll("\t", "<TAB>");
		if (message.indexOf("[ROOM_KEY]") >= 0) {
			String[] parse = message.split("#");
			sendSkey(conns, myId, id, msgId, senderName, date, title, url, "Android", parse[0], parse[1]);
		} else {

			appendMessageFile(id, msgId, senderName, date, title, message, url, "Android");
			if (conns == null)
				return;

			for (Connector conn : conns) {
				if (conn.itemType != null && conn.itemType.equals("Android")) {

					sendMessage(conn, msgId, senderName, date, title, message, url);
					if (message.indexOf("[READ_COMPLETE]") >= 0)
						removeMessageFile(id, msgId, "Android");
					return;
				}
			}
		}
	}

	private void sendSkey(final ConcurrentLinkedQueue<Connector> conns, final String myId, final String userId,
			final String msgId, final String senderName, final String date, final String title, final String url,
			final String type, final String data1, final String data2) {
		
		//2017-12-13
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection dbconn = null;
				Statement stmt = null;
				ResultSet rset = null;
				try {
					dbconn = dbcpMsger.getConnection();
					stmt = dbconn.createStatement();
					try {
						String myToken = "";
						String toToken = "";
						rset = stmt.executeQuery(
								"select USER_ID, TOKEN from MSG_USERDATA_CENTER where USER_ID like '" + myId + "'");
						if (rset.next()) {
							String value = rset.getString(1);
							myToken = rset.getString(2);
						}
						rset = stmt.executeQuery(
								"select USER_ID, TOKEN from MSG_USERDATA_CENTER where USER_ID like '" + userId + "'");
						if (rset.next()) {
							String value3 = rset.getString(1);
							toToken = rset.getString(2);
						}
						System.out.println("[TEST] myToken:" + myToken + ", toTken:" + toToken);
						String dec = AriaWrapper.decryptSkeyWrapper(myToken, data2);
						String enc = data1 + "#";
						enc += AriaWrapper.encryptSkeyWrapper(toToken, dec);

						appendMessageFile(userId, msgId, senderName, date, title, enc, url, type);
						if (conns == null)
							return;
						for (Connector conn : conns) {
							if (conn.itemType != null && conn.itemType.equals("Android")) {
								sendMessage(conn, msgId, senderName, date, title, enc, url);
								if (enc.indexOf("[READ_COMPLETE]") >= 0)
									removeMessageFile(userId, msgId, "Android");
								return;
							}
						}
					} catch (Exception ee) {
						ee.getStackTrace();
						return;
					}
				} catch (Exception ee) {
					ee.printStackTrace();
					return;
				} finally {
					
					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
            }
        };

        executorsPool.submit(runnable);
        //
		
		/*Thread th = new Thread() {
			public synchronized void start() {
				super.start();
			}

			public void run() {
				Connection dbconn = null;
				Statement stmt = null;
				ResultSet rset = null;
				try {
					dbconn = dbcpMsger.getConnection();
					stmt = dbconn.createStatement();
					try {
						String myToken = "";
						String toToken = "";
						rset = stmt.executeQuery(
								"select USER_ID, TOKEN from MSG_USERDATA_CENTER where USER_ID like '" + myId + "'");
						if (rset.next()) {
							String value = rset.getString(1);
							myToken = rset.getString(2);
						}
						rset = stmt.executeQuery(
								"select USER_ID, TOKEN from MSG_USERDATA_CENTER where USER_ID like '" + userId + "'");
						if (rset.next()) {
							String value3 = rset.getString(1);
							toToken = rset.getString(2);
						}
						System.out.println("[TEST] myToken:" + myToken + ", toTken:" + toToken);
						String dec = AriaWrapper.decryptSkeyWrapper(myToken, data2);
						String enc = data1 + "#";
						enc += AriaWrapper.encryptSkeyWrapper(toToken, dec);

						appendMessageFile(userId, msgId, senderName, date, title, enc, url, type);
						if (conns == null)
							return;
						for (Connector conn : conns) {
							if (conn.itemType != null && conn.itemType.equals("Android")) {
								sendMessage(conn, msgId, senderName, date, title, enc, url);
								if (enc.indexOf("[READ_COMPLETE]") >= 0)
									removeMessageFile(userId, msgId, "Android");
								return;
							}
						}
					} catch (Exception ee) {
						ee.getStackTrace();
						return;
					}
				} catch (Exception ee) {
					ee.printStackTrace();
					return;
				} finally {
					
					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
			}
		};
		th.start();*/
	}

	public void sendPushMessageAndroid(String id, String regId) {
		try {
			androidManager.sendAndroidPushMessage(id, regId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendPC(String msgId, String id, String senderName, String date, String title, String message,
			String url, String myId) {
		if (url.equals("SendMapData")) {
			return;
		}

		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
		message = message.replaceAll("\n", "<BR>");
		message = message.replaceAll("\r", "");
		message = message.replaceAll("\t", "<TAB>");
		appendMessageFile(id, msgId, senderName, date, title, message, url, "PC");
		if (conns == null)
			return;
		for (Connector conn : conns) {
			if (conn.itemType != null && conn.itemType.equals("PC")) {
				sendMessage(conn, msgId, senderName, date, title, message, url);
				if (message.indexOf("[READ_COMPLETE]") >= 0)
					removeMessageFile(id, msgId, "PC");
				return;
			}
		}
	}

	public boolean checkDuplicate(String id, String msgId, String phoneType) {
		File f = getFilePath(reservedMessagePath, id, phoneType);
		if (!f.exists())
			return false;
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
				line = codec.DecryptSEED(line);

				if (line.indexOf(msgId + "\t") >= 0)
					return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}
		}
		return false;
	}

	public void send(String idListString, String senderName, String date, String title, String message, String url,
			String myId, Connector conn) {

		StringTokenizer st = new StringTokenizer(idListString, ",");
		while (st.hasMoreTokens()) {
			String id = st.nextToken();
			String targetPhone = getSerial(id);
			String myPhone = getSerial(myId);
			String mySerial = "";
			String targetSerial = "";

			String msgId = "AM_" + messageIndexBase + "_" + messageIndex++;

			if (id.equals(myId)) {
				if (targetPhone == null) {
					targetPhone = "";
				} else {
					int ind = targetPhone.indexOf("\t");
					if (ind > 0) {
						targetSerial = targetPhone.substring(ind + 1);
						targetPhone = targetPhone.substring(0, ind);
					}
				}
				if (myPhone == null) {
					myPhone = "";
				} else {
					int ind = myPhone.indexOf("\t");
					if (ind >= 0) {
						mySerial = myPhone.substring(ind + 1);
						myPhone = myPhone.substring(0, ind);
					}
				}
				if (conn != null && conn.itemType != null) {
					if (conn.itemType.equals("PC")) {
						if (message.indexOf(m_SmsReceive) < 0) {
							if (myPhone.equals("iPhone")) {
								sendiPhone(msgId, myId, senderName, date, title, message, url, myId, mySerial);
							} else if (myPhone.equals("Android")) {
								sendAndroid(msgId, myId, senderName, date, title, message, url, myId);
							}
						}
					} else if (conn.itemType.equals("Android") || conn.itemType.equals("iPhone"))
						sendPC(msgId, myId, senderName, date, title, message, url, myId);
				}
			} else {
				if (targetPhone == null) {
					targetPhone = "";
				} else {
					int ind = targetPhone.indexOf("\t");
					if (ind >= 0) {
						targetSerial = targetPhone.substring(ind + 1);
						targetPhone = targetPhone.substring(0, ind);
					}
				}
				if (myPhone == null) {
					myPhone = "";
				} else {
					int ind = myPhone.indexOf("\t");
					if (ind >= 0) {
						myPhone = myPhone.substring(0, ind);
					}
				}
				if (conn != null) {
					System.out.println("conn.itemType : " + conn.itemType);
				}
				if (conn == null || conn.itemType == null) {
					if (targetPhone.equals("Android")) {
						sendAndroid(msgId, id, senderName, date, title, message, url, myId);
					} else if (targetPhone.equals("iPhone")) {
						sendiPhone(msgId, id, senderName, date, title, message, url, myId, targetSerial);
					}
					sendPC(msgId, id, senderName, date, title, message, url, myId);
				} else if (conn.itemType.equals("PC")) {
					if (targetPhone.equals("Android")) {
						sendAndroid(msgId, id, senderName, date, title, message, url, myId);
					} else if (targetPhone.equals("iPhone")) {
						sendiPhone(msgId, id, senderName, date, title, message, url, myId, targetSerial);
					}
					if (message.indexOf(m_SmsReceive) < 0) {
						sendPC(msgId, id, senderName, date, title, message, url, myId);
					}
				} else if (conn.itemType.equals("Android")) {
					if (targetPhone.equals("Android")) {
						sendAndroid(msgId, id, senderName, date, title, message, url, myId);
					} else if (targetPhone.equals("iPhone")) {
						sendiPhone(msgId, id, senderName, date, title, message, url, myId, targetSerial);
					}
					sendPC(msgId, id, senderName, date, title, message, url, myId);
				} else if (conn.itemType.equals("iPhone")) {
					if (targetPhone.equals("Android")) {
						sendAndroid(msgId, id, senderName, date, title, message, url, myId);
					} else if (targetPhone.equals("iPhone")) {
						sendiPhone(msgId, id, senderName, date, title, message, url, myId, targetSerial);
					}
					sendPC(msgId, id, senderName, date, title, message, url, myId);
				}
			}

			if (parallel != null)
				parallel.sendMessage(id, getEncodedMessage(msgId, senderName, date, title, message, url));
		}
	}

	public String getSerial(String id) {
		File sf = getFilePath(serialPath, id);
		if (sf == null || !sf.exists() || sf.length() == 0)
			return null;
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(sf);
			if (fr == null)
				return null;
			br = new BufferedReader(fr);
			return br.readLine();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}
		}
	}

	public void sendMessage(Connector conn, String msgId, String other) {
		String sendMessage = "NOTIFY\t" + msgId + "\t" + other;
		if (m_bDebug)
			System.out.println("SendMessage1 : " + sendMessage);
		conn.send(codec.EncryptSEED(sendMessage) + "\f");
	}

	public void sendMessage(Connector conn, String msgId, String senderName, String date, String title, String message,
			String url) {
		String sendMessage = "NOTIFY\t" + msgId + "\t" + senderName + "\t" + date + "\t" + title + "\t" + message + "\t"
				+ url;
		if (m_bDebug)
			System.out.println("SendMessage2 : " + sendMessage);
		conn.send(codec.EncryptSEED(sendMessage) + "\f");
	}

	public String getEncodedMessage(String msgId, String senderName, String date, String title, String message,
			String url) {
		String sendMessage = "NOTIFY\t" + msgId + "\t" + senderName + "\t" + date + "\t" + title + "\t" + message + "\t"
				+ url;

		return codec.EncryptSEED(sendMessage) + "\f";
	}

	//2018-01-11
	public File getFolderPath(String baseDir, String itemKey, String phoneType)
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
            pathName += "_" + phoneType;

            f = new File(pathName);
            f.mkdirs();

            return f;
    }
	//
	
	//2018-01-11
	private String getNowTime()
    {
            Calendar cal = UCalendar.getInstance();

            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH) + 1;
            int d = cal.get(Calendar.DAY_OF_MONTH);
            int H = cal.get(Calendar.HOUR_OF_DAY);
            int M = cal.get(Calendar.MINUTE);
            int S = cal.get(Calendar.SECOND);

            String ret = y + "";

            if ( m < 10 ) ret += "0" + m;
            else ret += "" + m;

            if ( d < 10 ) ret += "0" + d;
            else ret += "" + d;

            if ( H < 10 ) ret += "0" + H;
            else ret += "" + H;

            if ( M < 10 ) ret += "0" + M;
            else ret += "" + M;

            if ( S < 10 ) ret += "0" + S;
            else ret += "" + S;

            return ret;
    }
	//
	
	//2018-01-11
	class SendReservedThread extends Thread
    {
            private Connector conn;
            private String phoneType;

            public SendReservedThread(Connector conn, String phoneType)
            {
                    this.conn = conn;
                    this.phoneType = phoneType;

                    this.start();
            }

            public void run()
            {
                    File folder = getFolderPath(reservedMessagePath, conn.itemKey, phoneType);
                    File[] fl = folder.listFiles();
                    ArrayList<File> fileAr = new ArrayList<File>();

                    for ( int i = 0 ; i < fl.length ; i++ ) 
                    {
                            if ( fl[i].isDirectory() ) continue;

                            boolean m_bAdded = false;

                            String tName = fl[i].getName();
                            if ( tName.indexOf("_") < 0 || tName.length() < 14 ) continue;

                            tName = tName.substring(0, tName.indexOf("_"));

                            for ( int j = 0 ; j < fileAr.size() ; j++ )
                            {
                                    if ( tName.compareTo(fileAr.get(j).getName().substring(0, 14)) < 0 )
                                    {
                                            fileAr.add(j, fl[i]);
                                            m_bAdded = true;
                                            break;
                                    }
                            }

                            if ( !m_bAdded )
                            {
                                    fileAr.add(fl[i]);
                            }
                    }

                    if ( fileAr.size() == 0 )
                    {
                            if ( parallel != null && parallel.isConnected )
                            {
                                    parallel.sendUserConnected(conn.itemKey, phoneType);
                            }
                            else
                            {
                                    conn.send(codec.EncryptSEED("RESERVED_END") + "\f");
                            }

                            return;
                    }

                    ArrayList<String> ar = new ArrayList<String>();

                    int removedFileCount = 0;

                    if ( fileAr.size() > availableDataCount )  
                    {
                            for ( int i = 0 ; i < ( fileAr.size() - availableDataCount ) ; i++ )
                            {
                                    File f = fileAr.get(i);

                                    f.delete();

                                    removedFileCount++;
                            }
                    }

                    for ( int i = removedFileCount ; i < fileAr.size() ; i++ )
                    {
                                    FileReader fr = null;
                                    BufferedReader br = null;

                                    try
                                    {
                                            fr = new FileReader(fileAr.get(i));
                                            br = new BufferedReader(fr);

                                            String line = null;

                                            while ( ( line = br.readLine() ) != null )
                                            {
                                                    ar.add(line);
                                            }
                                    }
                                    catch(Exception e)
                                    {
                                            e.printStackTrace();
                                    }
                                    finally
                                    {
                                            if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception ee) { } }
                                            if ( br != null ) { try { br.close(); br = null; } catch(Exception ee) { } }
                                    }
                    }

                    conn.send(codec.EncryptSEED("RESERVED_START") + "\f");

                    for ( int i = 0 ; i < ar.size() ; i++ )
                    {
                            String line = ar.get(i);
                            line = codec.DecryptSEED(line);

                            if ( fieldCount(line) >= 6 )
                            {
                                    sendMessage(conn, line.substring(0, line.indexOf("\t")), line.substring(line.indexOf("\t") + 1).replaceAll("<BR>", "\n"));

                                    if ( line.indexOf("[READ_COMPLETE]") >= 0 ) removeMessageFile(conn.itemKey, line.substring(0, line.indexOf("\t")), phoneType);
                            }
                    }

                    if ( parallel != null && parallel.isConnected )
                    {
                            parallel.sendUserConnected(conn.itemKey, phoneType);
                    }
                    else
                    {
                            conn.send(codec.EncryptSEED("RESERVED_END") + "\f");
                    }
            }
    }
	//
	
	public void appendMessageFile(String id, String msgId, String senderName, String date, String title, String message,
			String url, String phoneType) {
		
		System.out.println("[MobileService] appendMsg id:"+id +", msgId:"+msgId +", message:"+message);
		//return;
		
		//map
		/*
		 * System.out.println("[MobileService] TEST appendMessageFile id:"+id +
		 * ", title:"+title + ", message:"+message); if(!phoneType.equals("Android"))
		 * return;
		 * 
		 * String msg = codec.EncryptSEED(msgId + "\t" + senderName + "\t" + date + "\t"
		 * + title + "\t" + message + "\t" + url);
		 * 
		 * ConcurrentHashMap<String,String> messageList = contentMaps.get(id);
		 * if(messageList == null) { messageList = new
		 * ConcurrentHashMap<String,String>(); messageList.put(msgId, msg);
		 * contentMaps.put(id, messageList); } else messageList.put(msgId, msg);
		 * 
		 * System.out.println("[MobileService] TEST appendMessageFile id:"+id +
		 * ", messageList size:"+ messageList.size());
		 */

		//2018-01-11
		File folder = getFolderPath(reservedMessagePath, id, phoneType);
		String fileName = getNowTime() + "_" + msgId;
		File f = new File(folder, fileName);
		
		if (checkDuplicate(id, msgId, phoneType))
			return;

		FileOutputStream fo = null;
		OutputStreamWriter ow = null;

		try {
			fo = new FileOutputStream(f);
			ow = new OutputStreamWriter(fo, charset);

			senderName = senderName.replaceAll("\n", "<BR>");
			date = date.replaceAll("\n", "<BR>");
			title = title.replaceAll("\n", "<BR>");
			message = message.replaceAll("\n", "<BR>");
			url = url.replaceAll("\n", "<BR>");

			String msg = codec
					.EncryptSEED(msgId + "\t" + senderName + "\t" + date + "\t" + title + "\t" + message + "\t" + url);
			ow.write(msg + "\n");
			ow.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fo != null) {
				try {
					fo.close();
					fo = null;
				} catch (Exception ee) {
				}
			}
			if (ow != null) {
				try {
					ow.close();
					ow = null;
				} catch (Exception ee) {
				}
			}
		}
	}
	
	public void removeMessageFile(String id, String msgId, String phoneType) {
		
		System.out.println("[MobileService] removeMsg id:"+id + ", msgId:"+msgId + ", type:"+phoneType);
		//return;
		
		//map
		/*
		 * System.out.println( "[MobileService] TEST removeMsg  id:" + id + ", msgid:" +
		 * msgId + ", type:" + phoneType); if(!phoneType.equals("Android")) return;
		 * 
		 * ConcurrentHashMap<String,String> messageList = contentMaps.get(id);
		 * if(messageList != null) { System.out.println(
		 * "[MobileService] TEST removeMsg  id:" + id +
		 * ", messageList size1:"+messageList.size()); messageList.remove(msgId);
		 * System.out.println( "[MobileService] TEST removeMsg  id:" + id +
		 * ", messageList size2:"+messageList.size()); } else System.out.println(
		 * "[MobileService] TEST removeMsg  id:" + id + ", messageList null");
		 */
		
		//2018-01-11
		if ( id == null ) return;
        File folder = getFolderPath(reservedMessagePath, id, phoneType);
        File[] f = folder.listFiles();

        for ( int i = 0 ; i < f.length ; i++ )
        {
                String name = f[i].getName();

                if ( name.indexOf("_") > 0 )
                {
                        name = name.substring(name.indexOf("_") + 1);

                        if ( name.equals(msgId) )
                        {
                                f[i].delete();
                        }
                }
        }
	}

	public boolean checkNewMessage(String userId, String phoneType) {
		File f = getFilePath(reservedMessagePath, userId, phoneType);
		if (!f.exists())
			return false;

		boolean isExists = false;

		FileReader fr = null;
		BufferedReader br = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		Vector<String> lines = new Vector<String>();
		try {
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.indexOf("CHAT") >= 0 && line.indexOf("[READ_COMPLETE]") < 0 && line.indexOf("[SUCCESS]") < 0) {
					isExists = true;
					break;
				}
			}

			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}

			if (isExists)
				return true;
		}

		return false;
	}

	public void sendReservedMessage(Connector conn, String phoneType) {
		
		System.out.println("[MobileService] sendReservedMsg send id:"+conn.itemKey +", type:"+phoneType);
		//return;
		
		//map
		/*
		 * if(!phoneType.equals("Android")) return;
		 * 
		 * System.out.println("[MobileService] TEST sendReserved");
		 * ConcurrentHashMap<String,String> messageList = contentMaps.get(conn.itemKey);
		 * conn.send( codec.EncryptSEED( "RESERVED_START" ) + "\f" );
		 * 
		 * if(messageList != null) {
		 * System.out.println("[MobileService] TEST sendReserved messageList size:"
		 * +messageList.size()); for( String value : messageList.values() ) { value =
		 * codec.DecryptSEED(value);
		 * System.out.println("[MobileService] TEST sendReserved str:" + value);
		 * 
		 * sendMessage( conn, value.substring( 0, value.indexOf( "\t" ) ),
		 * value.substring( value.indexOf( "\t" ) + 1 ) ); if ( value.indexOf(
		 * "[READ_COMPLETE]" ) >= 0 ) removeMessageFile( conn.itemKey, value.substring(
		 * 0, value.indexOf( "\t" ) ), phoneType ); } } else
		 * System.out.println("[MobileService] TEST sendReserved messageList null id:"
		 * +conn.itemKey);
		 * 
		 * conn.send( codec.EncryptSEED( "RESERVED_END" ) + "\f" );
		 */

		//2018-01-11
		if ( conn.itemKey == null ) return;
        new SendReservedThread(conn, phoneType);
	}

	private int fieldCount(String s) {
		if (s == null)
			return 0;

		int cnt = 0;
		for (int i = 0; i < s.length(); i++) {
			if (i == (s.length() - 1))
				cnt++;
			if (s.charAt(i) == '\t')
				cnt++;
		}
		return cnt;
	}

	public void arrangeReservedFile(File f) {
		FileReader fr = null;
		BufferedReader br = null;
		FileWriter fw = null;
		BufferedWriter bw = null;
		Vector<String> lines = new Vector<String>();
		try {
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				String msgId = st.nextToken();
				if (st.countTokens() >= 4 && !msgId.equals("")) {
					lines.add(line);
				} else {
					System.out.println("Deleted : " + st.countTokens() + "/" + msgId);
				}
			}
			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}
			fw = new FileWriter(f, false);
			bw = new BufferedWriter(fw);
			for (int i = 0; i < lines.size(); i++) {
				bw.write(lines.get(i) + "\n");
				bw.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fr != null) {
				try {
					fr.close();
					fr = null;
				} catch (Exception e) {
				}
			}
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (Exception e) {
				}
			}
			if (fw != null) {
				try {
					fw.close();
					fw = null;
				} catch (Exception e) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
					bw = null;
				} catch (Exception e) {
				}
			}
		}
	}

	public void appendUser(String id, Connector conn) {
		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
		System.out.println("[AppendUser] id:" + id + ", conn.itemType:" + conn.itemType + ", conns:" + conns);

		if (conns == null) {
			conns = new ConcurrentLinkedQueue<Connector>();
			userMap.put(id, conns);
		}

		if (conn.itemType != null && conn.itemType.equals("Android")) {
			for (Connector con : conns) {
				if (con.itemType != null && con.itemType.equals("Android")) {
					System.out.println();
					System.out.println("[Append_USER] remove Conn id:" + id + ", type:" + con.itemType + ", sc info:"
							+ con.sc.socket().getRemoteSocketAddress().toString());
					System.out.println();

					con.itemKey = null;
					con.itemType = null;
					con.item = null;

					con.close();
					conns.remove(con);
				}
			}
		}

		conns.add(conn);
		conn.itemKey = id;
		for (Connector con : conns) {
			System.out.println("====================================================");
			System.out.println(
					"[Append_USER] id:" + con.itemKey + ", type:" + con.itemType + ", conns size:" + conns.size());
			System.out.println("====================================================");
		}
	}

	public void removeUser(String id, Connector conn) {
		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);
		if (conns == null)
			return;
		conns.remove(conn);
		if (conns.size() == 0)
			userMap.remove(id);

		if (conns != null)
			System.out.println("RemoveUser => ID:" + id + ", conns size:" + conns.size() + ", type:" + conn.itemType);
	}

	public void mobileKeyUpdated(String id, String serial, String phoneType) {
	}

	public void updatePhoneSerial(String id, String serial, String phoneType) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(getFilePath(serialPath, id));
			System.out.println(getFilePath(serialPath, id).getCanonicalPath());
			fw.write(phoneType + "\t" + serial);
			fw.flush();

			mobileKeyUpdated(id, serial, phoneType);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
					fw = null;
				} catch (Exception ee) {
				}
			}
		}
	}

	public void ClearOldKey(String id, String serial) {
		if (serial.indexOf("Android") >= 0 || serial.indexOf("iPhone") >= 0)
			new ClearOldKey(id, serial, serialPath);
	}

	public File getFilePath(String baseDir, String itemKey, String phoneType) {
		String pathName = baseDir + java.io.File.separator;
		if (itemKey.length() > 6) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(4, 6);
		} else if (itemKey.length() > 4) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
		} else if (itemKey.length() > 2) {
			pathName += itemKey.substring(0, 2);
		} else {
			pathName += "{SHORTID}";
		}
		java.io.File f = new java.io.File(pathName);
		f.mkdirs();
		pathName += java.io.File.separator;
		pathName += itemKey;
		pathName += "." + phoneType;
		return new File(pathName);
	}

	public File getFilePath(String baseDir, String itemKey) {
		String pathName = baseDir + java.io.File.separator;
		if (itemKey.length() > 6) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(4, 6);
		} else if (itemKey.length() > 4) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
		} else if (itemKey.length() > 2) {
			pathName += itemKey.substring(0, 2);
		} else {
			pathName += "{SHORTID}";
			return null;
		}
		java.io.File f = new java.io.File(pathName);
		f.mkdirs();
		pathName += java.io.File.separator;
		pathName += itemKey;
		return new File(pathName);
	}

	class SendPush implements Runnable {
		String msg;
		List<String> list;

		public SendPush(String _msg, List<String> _list) {
			// sender = new Sender("AIzaSyDVPHu-UXZ3gpNfIEl_0bkgb3wKCOVnRjA");
			this.msg = _msg;
			this.list = _list;
		}

		public void run() {
			/*
			 * Message.Builder messageBuilder = new Message.Builder();
			 * messageBuilder.delayWhileIdle(false); messageBuilder.timeToLive(0);
			 * messageBuilder.collapseKey("collapseKey" + System.currentTimeMillis());
			 * messageBuilder.priority(Message.Priority.HIGH);
			 * messageBuilder.addData("msg",msg);
			 */

			try {
				/*
				 * System.out.println("[MobileService] SendPush send:"+msg); MulticastResult
				 * multiResult = sender.send(messageBuilder.build(), list, 5);
				 * System.out.println("[MobileService] SendPush Success. msg:"+msg);
				 * 
				 * if (multiResult != null) { List<Result> resultList = multiResult.getResults
				 * (); for (Result result : resultList) { System.out.println("Message ID : " +
				 * result.getMessageId()); System.out.println("ErrorCode  : " +
				 * result.getErrorCodeName()); System.out.println("canonicalID: " +
				 * result.getCanonicalRegistrationId()); } }
				 */

				/*
				 * 2017.03.23 test String apiKey = "AIzaSyDVPHu-UXZ3gpNfIEl_0bkgb3wKCOVnRjA";
				 * Map<String, Object> pushDatas = new HashMap<String, Object>();
				 * pushDatas.put("registration_ids", list); pushDatas.put("collapse_key",
				 * "atsmart_"+System.currentTimeMillis()); pushDatas.put("time_to_live", 180);
				 * pushDatas.put("priority", "high"); pushDatas.put("delay_while_idle", false);
				 * 
				 * Map<String, Object> pushMessages = new HashMap<String, Object>();
				 * pushMessages.put("msg", msg); pushDatas.put("data", pushMessages); String
				 * json = JSON.encode(pushDatas);
				 * 
				 * System.out.println("GCM->data:"+json);
				 * 
				 * URL url = new URL("https://gcm-http.googleapis.com/gcm/send "); //URL url =
				 * new URL("https://android.googleapis.com/gcm/send"); HttpsURLConnection
				 * urlConn = (HttpsURLConnection) url.openConnection();
				 * urlConn.setRequestMethod("POST"); urlConn.addRequestProperty("Authorization",
				 * "key=" + apiKey); urlConn.addRequestProperty("Content-Type",
				 * "application/json"); urlConn.setDoInput(true); urlConn.setDoOutput(true);
				 * 
				 * OutputStream os = urlConn.getOutputStream(); os.write(json.getBytes());
				 * os.close();
				 * 
				 * System.out.println("GCM Send->(" + urlConn.getResponseCode() + "):" +
				 * urlConn.getResponseMessage());
				 * 
				 * InputStream is = urlConn.getInputStream(); byte[] buf = new byte[1024];
				 * while(is.available() > 0) { int readCount = is.read(buf); if(readCount > 0)
				 * System.out.println("GCM-> readCount:" + new String(buf, 0, readCount)); }
				 */
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class ClearOldKey extends Thread {
		String id;
		String key;
		String path;

		public ClearOldKey(String id, String key, String path) {
			this.id = id;
			this.key = key;
			this.path = path;
			this.start();
		}

		public void run() {
			clearOldSerial(new File(path));
		}

		public void clearOldSerial(File f) {
			File[] fl = f.listFiles();
			for (int i = 0; i < fl.length; i++) {
				if (fl[i].isDirectory())
					clearOldSerial(fl[i]);
				else {
					FileReader fr = null;
					BufferedReader br = null;
					try {
						fr = new FileReader(fl[i]);
						br = new BufferedReader(fr);
						String line = br.readLine();
						if (line != null && line.equals(key) && !fl[i].getName().equals(id)) {
							if (fr != null) {
								try {
									fr.close();
									fr = null;
								} catch (Exception ee) {
								}
							}
							if (br != null) {
								try {
									br.close();
									br = null;
								} catch (Exception ee) {
								}
							}
							fl[i].delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (fr != null) {
							try {
								fr.close();
								fr = null;
							} catch (Exception ee) {
							}
						}
						if (br != null) {
							try {
								br.close();
								br = null;
							} catch (Exception ee) {
							}
						}
					}
				}
			}
		}
	}
}
