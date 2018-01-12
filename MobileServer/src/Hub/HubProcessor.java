package Hub;

import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Calendar;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileWriter;
import java.io.IOException;
import CombiEngine.Mobile.MobileService;
import CombiEngine.Selector.Connector;
import DBEmulator.DataBean;
import DBEmulator.PartObject;
import DBEmulator.UltariLinkedQueue;
import DBEmulator.UserObject;
import java.io.RandomAccessFile;
import CombiEngine.Codec.AmCodec;
import CombiEngine.Aria.AriaAlgorithm;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

//2016-11-18
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
//

//2017-08-14
import java.nio.channels.FileChannel;

public class HubProcessor extends Thread {
	class InvokeObject {
		String command;
		ArrayList<String> param;
	}

	HubClient client;
	public DataBean dataBean;

	ConcurrentHashMap<String, Connector> m_RequestUsers;
	ConcurrentHashMap<Connector, ConnectorWithList> observers;

	boolean m_bDebug;

	ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap;
	public static ConcurrentHashMap<String, String> blockUserMap;
	ConcurrentLinkedQueue<InvokeObject> InvokeQueue;
	public static boolean isOrgEnd = false;
	MobileService parent;

	private static final int idLen = 64;
	private static final int icLen = 2;
	private static final int nmLen = 256;
	private static final int mfLen = 386;
	private AmCodec codec;

	public HubProcessor(String gateIp, int gatePort, DataBean dataBean, boolean m_bDebug,
			ConcurrentHashMap<String, ConcurrentLinkedQueue<Connector>> userMap, MobileService parent) {
		this.dataBean = dataBean;
		this.m_RequestUsers = new ConcurrentHashMap<String, Connector>();
		this.m_bDebug = m_bDebug;
		this.userMap = userMap;
		this.parent = parent;
		this.InvokeQueue = new ConcurrentLinkedQueue<InvokeObject>();

		codec = new AmCodec(); 

		client = new HubClient(this, gateIp, gatePort, "MOBILE");
		new HubOrganizationSynchronizer(this, gateIp, gatePort, "MOBILE");

		this.observers = new ConcurrentHashMap<Connector, ConnectorWithList>();
		blockUserMap = new ConcurrentHashMap<String, String>();

		this.start();
		Thread thread = new Thread() {
			public void run() {
				while (true) {
					while (InvokeQueue.size() > 0) {
						InvokeObject obj = InvokeQueue.poll();
						if (obj != null) {
							ProcessMsg(obj.command, obj.param);
						}
					}
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}

	public void ProcessCommand(String receiveStr) {
		if (receiveStr == null)
			return;

		String command = "";
		ArrayList<String> param = new ArrayList<String>();
		param.clear();
		int type = 0;
		String nowStr = "";

		for (int i = 0; i < receiveStr.length(); i++) {
			if (receiveStr.charAt(i) == '\t') {
				if (type != 0)
					param.add(nowStr.trim());
				type++;
				nowStr = "";
			} else if (receiveStr.charAt(i) == '\f') {
				if (command.length() > 2) {
					if (type > 0)
						param.add(nowStr.trim());

					ProcessMsgByThread(command.trim(), param);
				}

				command = "";
				param = null;
				param = new ArrayList<String>();
				param.clear();
				nowStr = "";
				type = 0;
			} else if (type == 0)
				command += receiveStr.charAt(i);
			else
				nowStr += receiveStr.charAt(i);
		}
	}

	public void sendMyFolderEdit(String command, ArrayList<String> param, Connector conn) {

		if (command.equals("MYFOLDER_USER_ADD")) {
			AddMyFolder(param.get(2), param.get(3), param.get(4), param.get(5), param.get(1));
		} else if (command.equals("MYFOLDER_USER_DEL")) {
			RemoveMyFolder(param.get(2), param.get(3), param.get(1));
		}
	}

	private void ProcessMsgByThread(String command, ArrayList<String> param) {
		InvokeObject obj = new InvokeObject();
		obj.command = command;
		obj.param = param;
		InvokeQueue.add(obj);
	}

	private void ProcessMsg(String command, ArrayList<String> param) {
		try {
			if (command.equals("ORG") && param.size() > 0) {

				if (param.get(0).equals("Add")) {
					if (param.size() <= 6) {
						while (param.size() < 6)
							param.add("");

						if (dataBean.parts.get(param.get(1)) == null) {
							if (dataBean.addPart(param.get(1), param.get(2), param.get(3), param.get(4),
									param.get(5))) {
								broadcastPartAdded(param.get(1));
							}
						} else {
							if (dataBean.modPart(param.get(1), param.get(2), param.get(3), param.get(4),
									param.get(5))) {
								broadcastPartModified(param.get(1));
							}
						}
					} else {
						while (param.size() < 11)
							param.add("");

						if (dataBean.users.get(param.get(1)) == null) {
							if (dataBean.addUser(param.get(1), param.get(2), param.get(3), param.get(4), param.get(5),
									param.get(6), param.get(7), param.get(8), param.get(9), param.get(10))) {
								broadcastUserAdded(param.get(1));
							}
						} else {
							if (dataBean.modUser(param.get(1), param.get(2), param.get(5), param.get(6), param.get(7),
									param.get(8), param.get(9), param.get(10))) {
								broadcastUserModified(param.get(1));
							}
						}
					}
				} else if (param.get(0).equals("Mod")) {
					if (param.size() <= 6) {
						while (param.size() < 6)
							param.add("");

						if (dataBean.parts.get(param.get(1)) == null) {
							if (dataBean.addPart(param.get(1), param.get(2), param.get(3), param.get(4),
									param.get(5))) {
								broadcastPartAdded(param.get(1));
							}
						} else {
							if (dataBean.modPart(param.get(1), param.get(2), param.get(3), param.get(4),
									param.get(5))) {
								broadcastPartModified(param.get(1));
							}
						}
					} else {
						while (param.size() < 9)
							param.add("");

						if (dataBean.users.get(param.get(1)) == null) {
							if (dataBean.addUser(param.get(1), param.get(2), "0.0.0.0", "0", param.get(3), param.get(4),
									param.get(5), param.get(6), param.get(7), param.get(8))) {
								broadcastUserAdded(param.get(1));
							}
						} else {
							if (dataBean.modUser(param.get(1), param.get(2), param.get(3), param.get(4), param.get(5),
									param.get(6), param.get(7), param.get(8))) {
								broadcastUserModified(param.get(1));
							}
						}

						if (dataBean.modUser(param.get(1), param.get(2), param.get(3), param.get(4), param.get(5),
								param.get(6), param.get(7), param.get(8))) {
							broadcastUserModified(param.get(1));
						}
					}
				} else if (param.get(0).equals("Del")) {
					PartObject part = dataBean.delPart(param.get(1));
					UserObject user = dataBean.delUser(param.get(1));

					if (part != null) {
						broadcastPartRemoved(part);
					}

					if (user != null) {
						broadcastUserRemoved(user);
					}
				}
			} else if (command.equals("OrganizationComplete")) {
				dataBean.addSubPart();
				dataBean.addSubUser();

				isOrgEnd = true;
				writeOrgFile();
			} else if (command.equals("CNT") && param.size() >= 2) {
				dataBean.SetIcon((String) param.get(0), "1");

				broadcastUserIcon((String) param.get(0));
			} else if (command.equals("ICN") && param.size() == 2) {
				dataBean.SetIcon((String) param.get(0), (String) param.get(1));

				broadcastUserIcon((String) param.get(0));
			} else if (command.equals("NickName") && param.size() == 1) {
				dataBean.SetNickName((String) param.get(0), "");

				broadcastUserNick((String) param.get(0));
			} else if (command.equals("NickName") && param.size() == 2) {
				dataBean.SetNickName((String) param.get(0), (String) param.get(1));

				broadcastUserNick((String) param.get(0));
			} else if (command.equals("MyFolder") && param.size() == 5) {
				Connector conn = null;

				conn = m_RequestUsers.get(param.get(0));

				if (conn != null) {
					UserObject usr = dataBean.SendMyFolder(param.get(1), param.get(2), param.get(3), param.get(4),
							conn);

					if (param.get(4).equals("0")) {
						UserObject nowUsr = dataBean.users.get(param.get(1));

						if (nowUsr != null) {
							PartObject nowPrt = dataBean.parts.get(nowUsr.high);

							if (nowPrt != null) {
								dataBean.sendMessage(conn,
										"MyFolder\t" + nowUsr.id + "\t" + nowPrt.name + "\t" + nowUsr.mobileOn);
							}
						}
					}

					if (usr != null) {
						addObserver(conn, usr);
					}
				}
			} else if (command.equals("MyFolderEnd") && param.size() == 1) {
				Connector conn = null;

				conn = m_RequestUsers.remove(param.get(0));

				if (m_bDebug)
					System.out.println("m_RequestUsers : " + m_RequestUsers.size());

				if (conn != null) {
					sendMyPart(param.get(0), conn);

					dataBean.sendMessage(conn, "MyFolderEnd");
				}
			}
			else if (command.equals("ADMINPW") && param.size() > 0) {
				UserObject admin = dataBean.users.get(parent.config.get("SUPERADMIN"));
				String base = parent.config.get("MYFOLDERDIRECTORY");
				String oldKey = param.get(0);
				String newKey = param.get(1);

				if (admin != null) {
					new ConvertMyFolder(oldKey, newKey, base);
					admin.password = newKey;
				}
			}
			else if (command.equals("SYSMSG") && param.size() > 0) {
				String msg = (String) param.get(param.size() - 2);
				String script = (String) param.get(param.size() - 1);
				msg.trim();
				script.trim();

				if (msg.indexOf("<NOW>") >= 0 || script.indexOf("<NOW>") >= 0) {
					Calendar cal = Calendar.getInstance();
					long mil = cal.getTimeInMillis();
					msg = msg.replaceAll("<NOW>", mil + "");
					script = script.replaceAll("<NOW>", mil + "");
				}

				String[] data = parseSystemMessage(msg);

				String title = "";
				String message = "";
				String url = "";
				String sendTime = getNowTime();
				String sender = "";

				if (data.length >= 4)
					url = data[3];
				if (data.length >= 7)
					sender = data[6];
				if (data.length > 15)
					message = data[15];

				UserObject usr = dataBean.users.get(sender);
				if (usr != null) {
					sender = usr.getUserInfo(0);
				}

				if (data.length >= 15)
					title = data[14];
				else if (!sender.equals(""))
					title = sender + "";
				else
					title = message;

				System.out.println("TITLE : " + title);
				System.out.println("message : " + message);
				System.out.println("url : " + url);
				System.out.println("sender : " + sender);

				for (int x = 0; x <= (param.size() - 3); x++) {
					parent.send(param.get(x), sender, sendTime, title, message, "");
				}

				UserObject admin = dataBean.users.get(parent.config.get("SUPERADMIN"));
				String base = parent.config.get("MYFOLDERDIRECTORY");
				if (admin != null)
					new ConvertMyFolder(admin.password, "b59c67bf196a4758191e42f76670ceba", base);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String[] parseSystemMessage(String str) {
		ArrayList<String> ar = new ArrayList<String>();

		String nowStr = "";

		final char DOMMI = 15;

		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == DOMMI) {
				ar.add(nowStr);
				nowStr = "";
			} else
				nowStr += str.charAt(i);
		}

		ar.add(nowStr);

		String[] retAr = new String[ar.size()];

		for (int i = 0; i < ar.size(); i++) {
			retAr[i] = ar.get(i);
		}

		return retAr;
	}

	private String getNowTime() {
		Calendar cal = Calendar.getInstance();

		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int d = cal.get(Calendar.DAY_OF_MONTH);
		int H = cal.get(Calendar.HOUR_OF_DAY);
		int M = cal.get(Calendar.MINUTE);
		int S = cal.get(Calendar.SECOND);

		String ret = y + "";

		if (m < 10)
			ret += "0" + m;
		else
			ret += "" + m;

		if (d < 10)
			ret += "0" + d;
		else
			ret += "" + d;

		if (H < 10)
			ret += "0" + H;
		else
			ret += "" + H;

		if (M < 10)
			ret += "0" + M;
		else
			ret += "" + M;

		if (S < 10)
			ret += "0" + S;
		else
			ret += "" + S;

		return ret;
	}

	public void myFolderRequest(String userId, Connector conn) {
		File tFile = getMyFolderFilePath(userId);

		try {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(tFile.getPath()), "UTF-8"));

			String lines = "", line = "";
			while ((line = in.readLine()) != null) {
				lines += line;
			}

			lines = codec.DecryptSEED(lines);
			in.close();

			dataBean.sendMessage(conn, "MyFolder\t" + lines);
			dataBean.sendMessage(conn, "MyFolderEnd");
		} catch (Exception e) {
			dataBean.sendMessage(conn, "MyFolderEnd");
			e.printStackTrace();
		} finally {
		}
	}

	private boolean compare(String strVal, byte[] bytVal) {
		for (int i = 0; i < strVal.getBytes().length; i++) {
			if (strVal.getBytes()[i] != bytVal[i]) {
				return false;
			}
		}

		for (int j = strVal.getBytes().length; j < bytVal.length; j++) {
			if (bytVal[j] != 0x00) {
				return false;
			}
		}

		return true;
	}

	private String getStr(byte[] byteChar) {
		String ret = new String(byteChar);
		String retStr = "";
		for (int i = 0; i < ret.length(); i++) {
			if ((ret.charAt(i) != 0x00)) {
				retStr += ret.charAt(i);
			}
		}
		return retStr;
	}

	private File myFolderEncToDec(File f, String usrPassword) {
		FileOutputStream fo = null;
		OutputStreamWriter ow = null;
		FileInputStream fi = null;
		File decFile = null;

		try {
			String data = "";
			String key = usrPassword;
			System.out.println("myFOlderEncToDec pw:" + usrPassword);

			byte[] by = key.getBytes();
			byte[] arr = new byte[256];
			for (int i = 0; i < by.length; i++)
				arr[i] = by[i];
			for (int j = by.length; j < arr.length; j++)
				arr[j] = 0;

			fi = new FileInputStream(f);
			fo = new FileOutputStream(f + ".dec");
			ow = new OutputStreamWriter(fo, "UTF-8");
			byte[] buf = new byte[2048];
			int rcv = 0;
			while ((rcv = fi.read(buf, 0, 2048)) >= 0) {
				data = AriaAlgorithm.decrypt(new String(buf, 0, rcv), arr, 256, null);
				ow.write(data);
				ow.flush();
			}

			decFile = new File(f.getCanonicalPath() + ".dec");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fi != null) {
				try {
					fi.close();
					fi = null;
				} catch (Exception e) {
				}
			}
			if (fo != null) {
				try {
					fo.close();
					fo = null;
				} catch (Exception e) {
				}
			}
			if (ow != null) {
				try {
					ow.close();
					ow = null;
				} catch (Exception e) {
				}
			}
		}

		System.out.println("EncToDec decFile path:" + decFile.getPath());
		return decFile;
	}

	private File myFolderDecToEnc(File f, String usrPassword) {
		FileOutputStream fo = null;
		OutputStreamWriter ow = null;
		FileInputStream fi = null;
		File encFile = null;

		try {
			String data = "";
			String key = usrPassword;

			System.out.println("myFolderDecToEnc:" + usrPassword);

			byte[] by = key.getBytes();
			byte[] arr = new byte[256];
			for (int i = 0; i < by.length; i++)
				arr[i] = by[i];
			for (int j = by.length; j < arr.length; j++)
				arr[j] = 0;

			String parse = f.getCanonicalPath().substring(0, f.getCanonicalPath().indexOf("."));
			fi = new FileInputStream(f);
			fo = new FileOutputStream(parse);
			ow = new OutputStreamWriter(fo, "UTF-8");
			byte[] buf = new byte[2048];
			int rcv = 0;
			while ((rcv = fi.read(buf, 0, 2048)) >= 0) {
				data = AriaAlgorithm.encrypt(new String(buf, 0, rcv), arr, 256, null);
				ow.write(data);
				ow.flush();
			}

			encFile = new File(parse);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fi != null) {
				try {
					fi.close();
					fi = null;
				} catch (Exception e) {
				}
			}
			if (fo != null) {
				try {
					fo.close();
					fo = null;
				} catch (Exception e) {
				}
			}
			if (ow != null) {
				try {
					ow.close();
					ow = null;
				} catch (Exception e) {
				}
			}
		}

		System.out.println("DecToEnc encFile path:" + encFile.getPath());
		return encFile;
	}

	public boolean AddMyFolder(String id, String high, String name, String icon, String connId) {
		System.out.println("AddMyFolder->" + id + "\t" + high + "\t" + name + "\t" + icon + "\t" + connId);
		File tFile = getMyFolderFilePath(connId);

		RandomAccessFile uf = null;

		UserObject usr = dataBean.users.get(connId);
		if (usr == null)
			return false;

		UserObject admin = dataBean.users.get(parent.config.get("SUPERADMIN"));
		if (admin == null)
			return false;

		File decFile = null;
		if (tFile.exists()) {
			decFile = myFolderEncToDec(tFile, admin.password);
			if (!decFile.exists()) {
				System.out.println("AddMyFolder decFile null.. return");
				return false;
			}
		} else {
			decFile = new File(tFile + ".dec");
		}

		synchronized (usr) {
			try {
				uf = new RandomAccessFile(decFile, "rw"); 

				uf.seek(uf.length());
				uf.setLength(uf.length() + mfLen);

				if (id.getBytes().length > idLen) {
					return false;
				}
				if (high.getBytes().length > idLen) {
					return false;
				}
				if (name.getBytes().length > nmLen) {
					name = name.substring(0, 12);
				}
				if (icon.getBytes().length > icLen) {
					return false;
				}

				uf.write(id.getBytes());
				for (int i = 0; (i < idLen - id.getBytes().length); i++) {
					uf.write(0x00);
				}

				uf.write(high.getBytes());
				for (int i = 0; (i < idLen - high.getBytes().length); i++) {
					uf.write(0x00);
				}

				uf.write(name.getBytes());
				for (int i = 0; (i < nmLen - name.getBytes().length); i++) {
					uf.write(0x00);
				}

				uf.write(icon.getBytes());
				for (int i = 0; (i < icLen - icon.getBytes().length); i++) {
					uf.write(0x00);
				}

				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			} finally {
				try {
					uf.close();
				} catch (IOException ie) {
				}

				myFolderDecToEnc(decFile, admin.password);
				if (decFile.exists()) {
					decFile.delete();
					System.out.println("AddFolder decFile delete..");
				}
			}
		}
	}

	public boolean RemoveMyFolder(String id, String high, String connId) {
		System.out.println("RemoveMyFolder->" + id + "\t" + high + "\t" + connId);

		File tFile = getMyFolderFilePath(connId);

		RandomAccessFile uf = null;

		UserObject usr = dataBean.users.get(connId);
		if (usr == null)
			return false;

		UserObject admin = dataBean.users.get(parent.config.get("SUPERADMIN"));
		if (admin == null)
			return false;

		File decFile = null;
		if (tFile.exists()) {
			decFile = myFolderEncToDec(tFile, admin.password);
			if (!decFile.exists()) {
				System.out.println("RemoveMyFolder decFile null.. return");
				return false;
			}
		} else {
			decFile = new File(tFile + ".dec");
		}

		synchronized (usr) {
			try {
				uf = new RandomAccessFile(decFile, "rw"); 

				long totalSize = uf.length();
				long nowSize = 0L;
				byte[] idChar = new byte[idLen];
				byte[] hgChar = new byte[idLen];
				int readSize = uf.read(idChar);
				nowSize += readSize;
				readSize = uf.read(hgChar);
				nowSize += readSize;

				while (readSize > 0) {
					if ((compare(id, idChar) && compare(high, hgChar))) {
						uf.skipBytes((mfLen - idLen) - idLen);
						nowSize += (mfLen - idLen - idLen);
						final byte[] retChar = new byte[((int) (totalSize - nowSize))];
						uf.read(retChar);

						uf.seek(nowSize - mfLen);
						uf.write(retChar);
						uf.setLength(totalSize - mfLen);

						return true;
					} else {
						uf.skipBytes((mfLen - idLen) - idLen);
						nowSize += ((mfLen - idLen) - idLen);
					}

					readSize = uf.read(idChar);
					nowSize += readSize;
					readSize = uf.read(hgChar);
					nowSize += readSize;
				}

				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				try {
					uf.close();
				} catch (IOException ie) {
				}

				myFolderDecToEnc(decFile, admin.password);
				if (decFile.exists()) {
					decFile.delete();
				}
			}
		}
	}

	public java.io.File getMyFolderDecFilePath(String itemKey) {
		String pathName = parent.config.get("MYFOLDERDIRECTORY_BACKUP") + java.io.File.separator;

		if (itemKey.length() <= 2) {
			itemKey += "_ultari";
		}

		if (itemKey.length() > 4) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
		} else if (itemKey.length() > 2) {
			pathName += itemKey.substring(0, 2);
		}

		java.io.File f = new java.io.File(pathName);
		f.mkdirs();

		pathName += java.io.File.separator;
		pathName += itemKey;
		pathName += ".dec";

		return new java.io.File(pathName);
	}

	public java.io.File getMyFolderBackupFilePath(String itemKey) {
		String pathName = parent.config.get("MYFOLDERDIRECTORY_BACKUP") + java.io.File.separator;

		if (itemKey.length() <= 2) {
			itemKey += "_ultari";
		}

		if (itemKey.length() > 4) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
		} else if (itemKey.length() > 2) {
			pathName += itemKey.substring(0, 2);
		}

		java.io.File f = new java.io.File(pathName);
		f.mkdirs();

		pathName += java.io.File.separator;
		pathName += itemKey;

		return new java.io.File(pathName);

	}

	public java.io.File getMyFolderFilePath(String itemKey) {
		String pathName = parent.config.get("MYFOLDERDIRECTORY") + java.io.File.separator;

		if (itemKey.length() <= 2) {
			itemKey += "_ultari";
		}

		if (itemKey.length() > 4) {
			pathName += itemKey.substring(0, 2);
			pathName += java.io.File.separator;
			pathName += itemKey.substring(2, 4);
		} else if (itemKey.length() > 2) {
			pathName += itemKey.substring(0, 2);
		}

		java.io.File f = new java.io.File(pathName);
		f.mkdirs();

		pathName += java.io.File.separator;
		pathName += itemKey;

		return new java.io.File(pathName);
	}

	public void sendMyPart(String id, Connector conn) {
		UserObject usr = dataBean.users.get(id);

		if (usr == null)
			return;

		PartObject part = dataBean.parts.get(usr.high);

		if (part == null)
			return;

		UltariLinkedQueue subUsers = part.subUsers;
		Iterator it = subUsers.iterator();
		while (it.hasNext()) {
			UserObject nowUser = (UserObject) it.next();
			dataBean.sendMessage(conn, "MyFolder\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.name + "\t"
					+ nowUser.icon + "\t" + nowUser.param2 + "\t" + nowUser.order + "\t" + nowUser.mobileOn);

			addObserver(conn, nowUser);
		}
	}

	public String GetTopPart(String id) {
		String StrRet = "";
		String Childid = id;

		while (true) {
			PartObject prt = dataBean.parts.get(Childid);

			if (prt != null) {
				Childid = prt.high;

				if (prt == null) {
					StrRet = Childid;
					break;
				}
				if (prt.high.equals("0")) {
					StrRet = prt.id;
					break;
				}
			} else
				break;
		}
		return StrRet;
	}

	public void newSubOrganizationRequest(String id, String userId, Connector conn) {
		String userTopPart = "";
		if (id.equals("0")) {
			String partHigh = "";

			Iterator<String> iterator = dataBean.users.keySet().iterator();
			while (iterator.hasNext()) {
				UserObject user = dataBean.users.get(iterator.next());
				if (user != null) {
					if (user.id.equals(userId))
						partHigh = user.high;
				}
			}

			userTopPart = GetTopPart(partHigh);
		}

		PartObject part = dataBean.parts.get(id);
		if (part == null) {
			return;
		}

		addObserver(conn, part);
		{
			Iterator it = part.subParts.iterator();
			while (it.hasNext()) {
				PartObject nowPart = (PartObject) it.next();

				if (id.equals("0")) {
					if (nowPart.id.equals(userTopPart)) {
						dataBean.sendMessage(conn, "Part\t" + nowPart.id + "\t" + nowPart.high + "\t" + nowPart.name
								+ "\t" + nowPart.order + "\t" + nowPart.param1);
					}
				} else {
					dataBean.sendMessage(conn, "Part\t" + nowPart.id + "\t" + nowPart.high + "\t" + nowPart.name + "\t"
							+ nowPart.order + "\t" + nowPart.param1);
				}

				addObserver(conn, nowPart);
			}
		}
		{
			Iterator it = part.subUsers.iterator();
			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				dataBean.sendMessage(conn,
						"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name + "\t"
								+ nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t" + nowUser.param3
								+ "\t" + nowUser.mobileOn);

				addObserver(conn, nowUser);
			}
		}
		dataBean.sendMessage(conn, "OrganizationEnd");
	}

	public void newSearchRequest(String type, String key, String userId, Connector conn) {
		String nowUserTopPart = "";
		String userTopPart = "";
		String partHigh = "";

		Iterator<String> iterator = dataBean.users.keySet().iterator();
		while (iterator.hasNext()) {
			UserObject user = dataBean.users.get(iterator.next());
			if (user != null) {
				if (user.id.equals(userId))
					partHigh = user.high;
			}
		}

		userTopPart = GetTopPart(partHigh);

		if (type.equals("0")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(0);
				if (info.toLowerCase().indexOf(key.toLowerCase()) >= 0) {
					PartObject part = dataBean.parts.get(nowUser.high);

					nowUserTopPart = GetTopPart(nowUser.high);
					System.out.println("nowUserTop:" + nowUserTopPart + ", userTop:" + userTopPart);

					if (nowUserTopPart.equals(userTopPart))
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("1")) {
			Iterator<PartObject> it = dataBean.parts.values().iterator();

			while (it.hasNext()) {
				PartObject nowPart = (PartObject) it.next();

				if (nowPart.name.indexOf(key) >= 0) {
					Iterator usrIt = nowPart.subParts.iterator();
					while (usrIt.hasNext()) {
						UserObject nowUser = (UserObject) usrIt.next();

						nowUserTopPart = GetTopPart(nowUser.high);

						if (nowUserTopPart.equals(userTopPart))
							dataBean.sendMessage(conn,
									"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t"
											+ nowUser.name + "\t" + nowUser.order + "\t" + nowUser.param1 + "\t"
											+ nowUser.param2 + "\t" + nowUser.param3 + "\t" + nowUser.mobileOn);

						addObserver(conn, nowUser);
					}
				}
			}
		} else if (type.equals("2")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				if (nowUser.id.indexOf(key) >= 0) {
					PartObject part = dataBean.parts.get(nowUser.high);

					nowUserTopPart = GetTopPart(nowUser.high);
					System.out.println("nowUserTop:" + nowUserTopPart + ", userTop:" + userTopPart);

					if (nowUserTopPart.equals(userTopPart))
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("3")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(1);
				if (info.indexOf(key) >= 0) {
					PartObject part = dataBean.parts.get(nowUser.high);

					nowUserTopPart = GetTopPart(nowUser.high);
					System.out.println("nowUserTop:" + nowUserTopPart + ", userTop:" + userTopPart);

					if (nowUserTopPart.equals(userTopPart))
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("4")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(3);
				if (info.indexOf(key) >= 0) {
					PartObject part = dataBean.parts.get(nowUser.high);

					nowUserTopPart = GetTopPart(nowUser.high);
					System.out.println("nowUserTop:" + nowUserTopPart + ", userTop:" + userTopPart);

					if (nowUserTopPart.equals(userTopPart))
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("5")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(5);
				if (info.indexOf(key) >= 0) {
					PartObject part = dataBean.parts.get(nowUser.high);

					nowUserTopPart = GetTopPart(nowUser.high);
					System.out.println("nowUserTop:" + nowUserTopPart + ", userTop:" + userTopPart);

					if (nowUserTopPart.equals(userTopPart))
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		}
		dataBean.sendMessage(conn, "SearchEnd");
	}

	public void subOrganizationRequest(String id, Connector conn) {
		PartObject part = dataBean.parts.get(id);

		if (part == null)
			return;

		addObserver(conn, part);

		Iterator<PartObject> subParts = part.subParts.iterator();
		Iterator<UserObject> subUsers = part.subUsers.iterator();

		while (subParts.hasNext()) {
			PartObject nowPart = subParts.next();

			dataBean.sendMessage(conn, "Part\t" + nowPart.id + "\t" + nowPart.high + "\t" + nowPart.name + "\t"
					+ nowPart.order + "\t" + nowPart.param1);

			addObserver(conn, nowPart);
		}

		while (subUsers.hasNext()) {
			UserObject nowUser = subUsers.next();

			dataBean.sendMessage(conn,
					"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name + "\t"
							+ nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t" + nowUser.param3
							+ "\t" + nowUser.mobileOn);

			addObserver(conn, nowUser);
		}

		dataBean.sendMessage(conn, "OrganizationEnd");
	}

	public void searchRequest(String type, String key, Connector conn) {
		if (type.equals("0")) {
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(0);
				if (info.indexOf(key) >= 0) {
					dataBean.sendMessage(conn,
							"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
									+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
									+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("1")) 
		{
			Iterator<PartObject> it = dataBean.parts.values().iterator();

			while (it.hasNext()) {
				PartObject nowPart = (PartObject) it.next();

				if (nowPart.name.indexOf(key) >= 0) {
					Iterator<UserObject> subUsers = nowPart.subUsers.iterator();
					while (subUsers.hasNext()) {
						UserObject nowUser = subUsers.next();
						dataBean.sendMessage(conn,
								"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
										+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
										+ nowUser.param3 + "\t" + nowUser.mobileOn);
						addObserver(conn, nowUser);
					}
				}
			}
		} else if (type.equals("2")) 
		{
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				if (nowUser.id.indexOf(key) >= 0) {
					dataBean.sendMessage(conn,
							"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
									+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
									+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("3"))
		{
			Iterator<UserObject> it = dataBean.users.values().iterator();

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(1);

				if (info.indexOf(key) >= 0) {
					dataBean.sendMessage(conn,
							"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
									+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
									+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("4")) 
		{
			Iterator<UserObject> it = dataBean.users.values().iterator();

			key = key.replaceAll("-", "");
			key = key.replaceAll(" ", "");

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(3);

				info = info.replaceAll("-", "");
				info = info.replaceAll(" ", "");

				if (info.indexOf(key) >= 0) {
					dataBean.sendMessage(conn,
							"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
									+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
									+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		} else if (type.equals("5")) 
		{
			Iterator<UserObject> it = dataBean.users.values().iterator();

			key = key.replaceAll("-", "");
			key = key.replaceAll(" ", "");

			while (it.hasNext()) {
				UserObject nowUser = (UserObject) it.next();

				String info = nowUser.getUserInfo(5);

				info = info.replaceAll("-", "");
				info = info.replaceAll(" ", "");

				if (info.indexOf(key) >= 0) {
					dataBean.sendMessage(conn,
							"User\t" + nowUser.id + "\t" + nowUser.high + "\t" + nowUser.icon + "\t" + nowUser.name
									+ "\t" + nowUser.order + "\t" + nowUser.param1 + "\t" + nowUser.param2 + "\t"
									+ nowUser.param3 + "\t" + nowUser.mobileOn);

					addObserver(conn, nowUser);
				}
			}
		}

		dataBean.sendMessage(conn, "SearchEnd");
	}

	public void userOnlineUpdated(String id) {
		Iterator<String> it = dataBean.users.keySet().iterator();
		while (it.hasNext()) {
			UserObject user = dataBean.users.get(it.next());
			if (user.id.equals(id)) {
				user.mobileOn = 1;
				broadcastUserMobileIcon(id);
				break;
			}
		}
	}

	public void userOfflineUpdated(String id) {
		Iterator<String> it = dataBean.users.keySet().iterator();
		while (it.hasNext()) {
			UserObject user = dataBean.users.get(it.next());
			if (user.id.equals(id)) {
				user.mobileOn = 0;
				broadcastUserMobileIcon(id);
				break;
			}
		}
	}

	private void broadcastUserMobileIcon(String id) {
		UserObject user = dataBean.users.get(id);
		if (user == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(user.id);
		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			dataBean.sendMessage(connList.conn, "MobileIcon\t" + user.id + "\t" + user.mobileOn);
		}
	}

	public void OnClose(Connector conn) {
		Connector nowConn = m_RequestUsers.get(conn.ip);

		if (nowConn != null && nowConn == conn) {
			m_RequestUsers.remove(conn.ip);

			if (m_bDebug)
				System.out.println("m_RequestUsers : " + m_RequestUsers.size());
		}

		clearObserver(conn);
	}

	public void addObserver(Connector conn, PartObject part) {
		ConnectorWithList connList = observers.get(conn);

		if (connList == null) {
			connList = new ConnectorWithList(conn, m_bDebug, dataBean);

			observers.put(conn, connList);
		}

		connList.addObserver(part);
	}

	public void addObserver(Connector conn, UserObject user) {
		ConnectorWithList connList = observers.get(conn);

		if (connList == null) {
			connList = new ConnectorWithList(conn, m_bDebug, dataBean);

			observers.put(conn, connList);
		}

		connList.addObserver(user);
	}

	private void clearObserver(Connector conn) {
		ConnectorWithList connList = observers.remove(conn);

		if (connList != null) {
			connList.close();
		}
	}

	private void broadcastPartAdded(String id) {
		PartObject part = dataBean.parts.get(id);

		if (part == null)
			return;

		PartObject parentPart = dataBean.parts.get(part.high);

		if (parentPart == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(parentPart.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			dataBean.sendMessage(connList.conn, "AddPart\t" + part.id + "\t" + part.high + "\t" + part.name + "\t"
					+ part.order + "\t" + part.param1);
			addObserver(connList.conn, part);
		}
	}

	private void broadcastUserAdded(String id) {
		UserObject user = dataBean.users.get(id);

		if (user == null)
			return;

		PartObject parentPart = dataBean.parts.get(user.high);

		if (parentPart == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(parentPart.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			dataBean.sendMessage(connList.conn, "AddUser\t" + user.id + "\t" + user.high + "\t" + user.name + "\t"
					+ user.order + "\t" + user.param1 + "\t" + user.param2 + "\t" + user.param3);

			addObserver(connList.conn, user);
		}
	}

	private void broadcastPartModified(String id) {
		PartObject part = dataBean.parts.get(id);

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(part.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {

			dataBean.sendMessage(connList.conn, "ModPart\t" + part.id + "\t" + part.high + "\t" + part.name + "\t"
					+ part.order + "\t" + part.param1);

			addObserver(connList.conn, part);
		}
	}

	private void broadcastUserModified(String id) {
		UserObject user = dataBean.users.get(id);

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(user.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {

			dataBean.sendMessage(connList.conn, "ModUser\t" + user.id + "\t" + user.high + "\t" + user.name + "\t"
					+ user.order + "\t" + user.param1 + "\t" + user.param2 + "\t" + user.param3);

			addObserver(connList.conn, user);
		}
	}

	private void broadcastPartRemoved(PartObject part) {

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(part.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {

			dataBean.sendMessage(connList.conn, "DelPart\t" + part.id);
		}
	}

	private void broadcastUserRemoved(UserObject user) {

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(user.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {

			dataBean.sendMessage(connList.conn, "DelUser\t" + user.id);
		}
	}

	private void broadcastUserIcon(String id) {
		UserObject user = dataBean.users.get(id);

		if (user == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(user.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			dataBean.sendMessage(connList.conn, "Icon\t" + user.id + "\t" + user.icon);
		}
	}

	private void broadcastUserNick(String id) {
		UserObject user = dataBean.users.get(id);

		if (user == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(user.id);

		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {

			dataBean.sendMessage(connList.conn, "Nick\t" + user.id + "\t" + user.param2);
		}

		ConcurrentLinkedQueue<Connector> conns = userMap.get(id);

		if (conns == null)
			return;

		for (Connector conn : conns) {
			if (conn != null) {
				dataBean.sendMessage(conn, "Nick\t" + user.id + "\t" + user.param2);
			} else {
				conns.remove(conn);
			}
		}
	}

	public void sendSynId(ArrayList<String> param, Connector conn) {
		for (int i = 0; i < param.size(); i++) {
			UserObject user = dataBean.users.get(param.get(i));
			dataBean.sendMessage(conn, "SYNID\t" + user.id + "\t" + user.mobileOn);
		}

		dataBean.sendMessage(conn, "SYNEND\t0");
	}

	public void sendSynIdNew(ArrayList<String> param, Connector conn) {
		for (int i = 0; i < param.size(); i++) {
			UserObject user = dataBean.users.get(param.get(i));
			if (user == null)
				dataBean.sendMessage(conn, "SYNID\t" + param.get(i) + "\t" + 0 + "\t" + 0);
			else
				dataBean.sendMessage(conn,
						"SYNID\t" + user.id + "\t" + user.mobileOn + "\t" + user.name + "\t" + user.param2);
		}

		dataBean.sendMessage(conn, "SYNEND\t0");
	}

	public void sendRoomSyn(ArrayList<String> param, Connector conn) {
		System.out.println("HUB sendRoomSyn!->" + param.size());
		try {
			for (int i = 0; i < param.size(); i++) {
				// ROOMID:USERID list
				String[] arr = param.get(i).split(":");
				if (arr != null) {
					String resultName = "";
					String[] ids = arr[1].split(",");
					if (ids != null) {
						for (int j = 0; j < ids.length; j++) {
							UserObject user = dataBean.users.get(ids[j]);
							if (user == null)
								resultName += " " + ",";
							else
								resultName += user.getName() + ",";
						}

						resultName = resultName.substring(0, resultName.lastIndexOf(","));
						dataBean.sendMessage(conn, "ROOMSYN\t" + arr[0] + "\t" + resultName);
						System.out.println("HUB send! ROOMSYN\t" + arr[0] + "\t" + resultName);
					}
				}
			}

			dataBean.sendMessage(conn, "ROOMSYNEND\t0");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendName(Connector conn) {
		dataBean.sendName(conn);
	}

	public void sendName(String id, Connector conn) {
		dataBean.sendName(id, conn);
	}

	public void sendNick(Connector conn) {
		dataBean.sendNick(conn);
	}

	public void setNick(String nick, Connector conn) {
		client.sendString("NickName\t" + conn.itemKey + "\t" + nick + "\f");
	}

	public boolean checkLogin(String id, String password, String mobileKey, Connector conn) {
		String encPassword = "";
		UserObject user = dataBean.users.get(id);

		if (user == null) {
			dataBean.sendMessage(conn, "NoUser");

			return false;
		}
		/*
		 * else if ( !user.password.equals(password) ) { dataBean.sendMessage(conn,
		 * "NoPassword");
		 * 
		 * return false; }
		 */
		else {
			dataBean.sendMessage(conn, "Passport\t" + user.name + "\t" + user.param2);

			return true;
		}
	}

	public void sendUserStatus(String ids, Connector conn) {
		System.out.println("GJ1:" + ids);

		String[] userIds = ids.split(",");
		String result = "USERSTATUS#";

		for (String id : userIds) {
			UserObject usr = dataBean.users.get(id);

			if (usr != null) {
				if (usr.mobileOn == 1)
					result += id + ":ON:" + usr.name + "$";
				else
					result += id + ":OFF:" + usr.name + "$";
			} else
				result += id + ":OFF:" + "NO USER" + "$";
		}

		System.out.println("GJ2:" + result);
		dataBean.sendMessage(conn, result);
	}

	public void mobileKeyUpdated(String id, String serial, String phoneType) {
		if (phoneType.equals("Android") || phoneType.equals("iPhone")) {
			client.sendString("MOBILEKEY\t" + id + "\t" + phoneType + ":" + serial + "\f");

			UserObject user = dataBean.users.get(id);
			if (user != null)
				user.mobileOn = 1;
		}
	}

	public void resetPhotoVersionToDatabase() {
		
		//2017-12-13
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection dbconn = null;
        		Statement stmt = null;
        		ResultSet rset = null;

        		String id = "", photoVer = "", nameVer = "", nickVer = "";

        		try {
        			dbconn = parent.dbcpCert.getConnection();
        			stmt = dbconn.createStatement();
        			dbconn.setAutoCommit(false);

        			try {
        				rset = stmt.executeQuery("select * FROM PHOTO_CNT");
        				while (rset.next()) {
        					id = rset.getString(1);
        					photoVer = rset.getString(2);
        					nameVer = rset.getString(3);
        					nickVer = rset.getString(4);

        					UserObject usr = dataBean.users.get(id);
        					if (usr != null) {
        						usr.setPhotoVersion(Integer.parseInt(photoVer));
        						usr.setNameVersion(Integer.parseInt(nameVer));
        						usr.setNickVersion(Integer.parseInt(nickVer));
        					}
        				}

        				dbconn.setAutoCommit(true);
        			} catch (Exception ee) {
        				ee.getStackTrace();
        				return;
        			}
        		} catch (Exception ee) {
        			ee.printStackTrace();
        			return;
        		} finally {
        			parent.dbcpCert.freeConnection(dbconn, stmt, rset);
        		}
            }
        };

        parent.executorsPool.submit(runnable);
        //
	}

	public void new_searchPhotoInfo(String list, Connector conn) {
		if (list == null)
			return;

		try {
			String[] parse = list.split("#");
			if (parse != null) {
				boolean isChange = false;

				for (int i = 0; i < parse.length; i++) {
					String[] data = parse[i].split(":");

					UserObject user = dataBean.users.get(data[0]);
					if (user != null) {
						System.out.println("user photoV:" + user.getPhotoVersion() + ", dataV:" + data[1]);

						if (user.getPhotoVersion() > Integer.parseInt(data[1])) {
							isChange = true;
							dataBean.sendMessage(conn, "PHOTOUPDATE\t" + data[0] + "\t" + user.getPhotoVersion());
						}

						if (user.getNameVersion() > Integer.parseInt(data[2])) {
							isChange = true;
							dataBean.sendMessage(conn, "NAMEUPDATE\t" + data[0] + "\t" + user.getNameVersion() + "\t"
									+ user.getNameField());
						}

						if (user.getNickVersion() > Integer.parseInt(data[3])) {
							isChange = true;
							dataBean.sendMessage(conn,
									"NICKUPDATE\t" + data[0] + "\t" + user.getNickVersion() + "\t" + user.param2);
						}
					}

					addObserver(conn, user);
				}

				if (isChange)
					dataBean.sendMessage(conn, "UPDATEEND\t0");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			while (true) {
				Iterator<String> it = dataBean.users.keySet().iterator();
				while (it.hasNext()) {
					UserObject user = dataBean.users.get(it.next());
					if (user != null)
						user.mobileOn = 0;
				}

				File f = new File(parent.serialPath);
				sendSerialToGate(f);

				resetPhotoVersionToDatabase();

				Thread.sleep(300000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendSerialToGate(File f) {
		File[] fl = f.listFiles();

		for (int i = 0; i < fl.length; i++) {
			if (fl[i].isDirectory())
				sendSerialToGate(fl[i]);
			else {
				FileReader fr = null;
				BufferedReader br = null;

				try {
					fr = new FileReader(fl[i]);
					br = new BufferedReader(fr);

					String line = br.readLine();
					if (line.indexOf("iPhone") == 0) {
						line = line.substring(line.indexOf("\t") + 1);
						client.sendString("MOBILEKEY\t" + fl[i].getName() + "\tiPhone:" + line + "\f");

						setMobileUserIcon(fl[i].getName(), fl[i].lastModified());
					} else if (line.indexOf("Android") == 0) {
						line = line.substring(line.indexOf("\t") + 1);
						client.sendString("MOBILEKEY\t" + fl[i].getName() + "\tAndroid:" + line + "\f");

						setMobileUserIcon(fl[i].getName(), fl[i].lastModified());
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

	public static long diffOfDate(String begin, String end) throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date beginDate = formatter.parse(begin);
		Date endDate = formatter.parse(end);

		long diff = endDate.getTime() - beginDate.getTime();
		long diffDays = diff / (24 * 60 * 60 * 1000);

		return diffDays;
	}

	public void setMobileUserIcon(String name, long modified) {
		try {
			UserObject user = dataBean.users.get(name.trim());
			if (user != null) {
				/*
				 * Long lastModified = modified; Date date = new Date(lastModified);
				 * SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.KOREA );
				 * String dateString = format.format(date);
				 * 
				 * SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat ( "yyyyMMdd",
				 * Locale.KOREA ); Date currentTime = new Date ( ); String mTime =
				 * mSimpleDateFormat.format ( currentTime );
				 */

				// if(diffOfDate(dateString,mTime) > 29)
				// user.mobileOn = 0;
				// else
				user.mobileOn = 1;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendModUser(UserObject user) {
		// ORG ModUser [id] [high] [name] [password] [order] [param1] [param2]
		if (user != null)
			client.sendString("ORG\tModUser\t" + user.id + "\t" + user.high + "\t" + user.name + "\t" + user.password
					+ "\t" + user.order + "\t" + user.param1 + "\t" + user.param2 + "\f");

		UserObject usr = dataBean.users.get(user.id);
		if (usr == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(usr.id);
		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			System.out.println("sendModUser -> observer Send! ID:" + connList.conn.itemKey);

			dataBean.sendMessage(connList.conn,
					"NAMEUPDATE\t" + usr.id + "\t" + usr.getNameVersion() + "\t" + usr.getNameField());
			dataBean.sendMessage(connList.conn,
					"NICKUPDATE\t" + usr.id + "\t" + usr.getNickVersion() + "\t" + usr.param2);
			dataBean.sendMessage(connList.conn, "UPDATEEND\t0");
		}
	}

	public void sendModPhotoVersion(String userId, String photoVersion) {
		UserObject usr = dataBean.users.get(userId);
		if (usr == null)
			return;

		ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(usr.id);
		if (obs == null)
			return;

		for (ConnectorWithList connList : obs) {
			dataBean.sendMessage(connList.conn, "PHOTOUPDATE\t" + userId + "\t" + photoVersion);
		}
	}

	public void sendDeletePhotoVersion(String userId) {
		try {
			UserObject usr = dataBean.users.get(userId);
			if (usr == null)
				return;

			ConcurrentLinkedQueue<ConnectorWithList> obs = dataBean.getObservers(usr.id);
			if (obs == null)
				return;

			for (ConnectorWithList connList : obs) {
				dataBean.sendMessage(connList.conn, "PHOTO_DELETE\t" + userId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void writeOrgFile() {
		try {
			FileWriter fw = new FileWriter("/home/center/Group.txt");
			Iterator<PartObject> it = dataBean.parts.values().iterator();
			while (it.hasNext()) {
				PartObject part = it.next();
				if (part != null)
					fw.write(part.id + "\t" + part.high + "\t" + part.name + "\t" + part.order + "\n");
			}

			FileWriter fw2 = new FileWriter("/home/center/User.txt");
			Iterator<UserObject> it2 = dataBean.users.values().iterator();
			while (it2.hasNext()) {
				UserObject user = it2.next();
				if (user != null)
					fw2.write(user.id + "\t" + user.high + "\t" + user.name + "\t" + user.password + "\t" + user.order
							+ "\n");
			}

			fw.flush();
			fw.close();
			fw2.flush();
			fw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class ConvertMyFolder extends Thread {
		String oldPassword;
		String newPassword;
		String baseDir;

		public ConvertMyFolder(String oldPw, String newPw, String baseDir) {
			this.oldPassword = oldPw;
			this.newPassword = newPw;
			this.baseDir = baseDir;

			System.out.println("ConverMyFolder start!");

			start();
		}

		public void run() {
			try {
				CheckData(new File(baseDir));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void CheckData(File dir) {
			if (!dir.exists())
				return;

			File[] fl = dir.listFiles();

			try {
				for (int i = (fl.length - 1); i >= 0; i--) {
					File f = fl[i];

					if (f.isDirectory())
						CheckData(f);
					else {
						File decFile = null;
						File encFile = null;

						if (f.exists()) {
							decFile = myFolderEncToDec(f, oldPassword);
							if (decFile.exists()) {
								System.out.println("ConvertMyFolder CheckData decFile ok..");
							}
						} else
							return;

						encFile = myFolderDecToEnc(decFile, newPassword);
						if (encFile.exists()) {
							System.out.println("ConvertMyFolder CheckData encFile ok..");
						} else
							return;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void gpsProvideConsent(String userId, String date, String result, Connector conn) {
		FileWriter fwlog = null;
		String logDir = parent.config.get("GPS_DIRECTORY");
		try {
			fwlog = new FileWriter(logDir, true);
			fwlog.write(result + " " + userId + " " + date + "\r\n");
			fwlog.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fwlog != null) {
				try {
					fwlog.close();
					fwlog = null;
				} catch (Exception eee) {
				}
			}
		}
	}

	public void requestMyFolderBackup(Connector conn, String senderId, String value) {
		try {
			File tFile = getMyFolderFilePath(senderId);
			if (tFile.exists()) {
				File oFile = getMyFolderBackupFilePath(senderId);
				if (oFile.exists())
					oFile.delete();
				oFile.createNewFile();

				System.out.println("[HubProcessor] requestMyFolderBackup file dec backup start inPath:"
						+ tFile.getPath() + ", outPath:" + oFile.getPath());

				FileInputStream inputStream = new FileInputStream(tFile);
				FileOutputStream outputStream = new FileOutputStream(oFile);

				FileChannel fcin = inputStream.getChannel();
				FileChannel fcout = outputStream.getChannel();

				long size = fcin.size();
				fcin.transferTo(0, size, fcout);

				fcout.close();
				fcin.close();

				outputStream.close();
				inputStream.close();

				System.out.println("[HubProcessor] requestMyFolderBackup file copy end.. dec file make start");
				BufferedReader in = new BufferedReader(
						new InputStreamReader(new FileInputStream(tFile.getPath()), "UTF-8"));

				String lines = "", line = "";
				while ((line = in.readLine()) != null)
					lines += line;

				lines = codec.DecryptSEED(lines);
				in.close();

				File decFile = getMyFolderDecFilePath(senderId);
				if (decFile.exists())
					decFile.delete();
				decFile.createNewFile();

				BufferedWriter output = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(decFile), "UTF-8"));
				output.write(lines);
				output.flush();
				output.close();

				System.out.println("[HubProcessor] requestMyFolderBackup dec file make end..");
				tFile.delete();
			}

			tFile.createNewFile();

			BufferedWriter output = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(tFile.getPath()), "UTF-8"));
			output.write(codec.EncryptSEED(value));
			output.flush();
			output.close();

			System.out.println("requestMyFolderBackup file path:" + tFile.getPath() + ", value:" + value);
			dataBean.sendMessage(conn, "MyFolderBackup\tY");
		} catch (Exception e) {
			dataBean.sendMessage(conn, "MyFolderBackup\tN");
			e.printStackTrace();
		}
	}

	public void deleteMyFolder(String userId) {
		try {
			File tFile = getMyFolderFilePath(userId);
			if (tFile.exists()) {
				System.out.println("DROPUSER deleteMyFolder delete path:" + tFile.getPath());
				tFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
