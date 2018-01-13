
import java.util.ArrayList;
import java.util.HashMap;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Iterator;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import CombiEngine.Mobile.MobileService;
import CombiEngine.Selector.Connector;
import DBEmulator.DataBean;
import DBEmulator.UserObject;
import Hub.HubProcessor;

import java.util.GregorianCalendar;
import java.util.Calendar;

public class MobileServer extends MobileService {
	private static final Logger logger = LogManager.getLogger(MobileServer.class);
	
	HubProcessor hub;

	public MobileServer(ConcurrentHashMap<String, String> config) {
		super(config);

		boolean m_bDebug = false;
		if (config.get("DEBUG") != null && config.get("DEBUG").equals("Y"))
			m_bDebug = true;

		hub = new HubProcessor(config.get("GATE_IP"), Integer.parseInt(config.get("GATE_PORT")), new DataBean(m_bDebug),
				m_bDebug, userMap, this);
	}

	public void mobileKeyUpdated(String id, String serial, String phoneType) {
		hub.mobileKeyUpdated(id, serial, phoneType);
	}

	public static void main(String[] args) {
		String confPath = null;

		if (args.length == 1) {
			confPath = args[0];
		} else {
			confPath = "Config2.dat";
		}

		ConcurrentHashMap<String, String> config = new ConcurrentHashMap<String, String>();
		MobileServer.resetConfig(confPath, config);

		new MobileServer(config);
		logger.info("Service started with port : " + config.get("SERVICE_PORT"));

	}

	private static void resetConfig(String confFile, ConcurrentHashMap<String, String> config) {
		FileReader cf = null;
		BufferedReader br = null;

		try {
			cf = new FileReader(confFile);
			br = new BufferedReader(cf);

			String line = "";

			while ((line = br.readLine()) != null) {
				line.trim();
				if (line.length() > 0 && line.indexOf(":") > 0 && line.charAt(0) != '#')
					config.put(line.substring(0, line.indexOf(":")), line.substring(line.indexOf(":") + 1));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				cf.close();
				cf = null;
			} catch (Exception ee) {
			}
			try {
				br.close();
				br = null;
			} catch (Exception ee) {
			}
		}
	}

	public void ProcessCustom(String command, ArrayList<String> param, Connector conn) {
		if (command.equals("MyFolderRequest") && param.size() == 1) {
			hub.myFolderRequest(param.get(0), conn);
		} else if (command.equals("NAMEUPDATE")) {
			updateName(conn, param.get(0), param.get(1));
		} else if (command.equals("PROFILEUPDATE")) {
			if (param != null) {
				photoVersionUpdate(param.get(0));
			}
		} else if (command.equals("PROFILE_DELETE")) {
			if (param != null) {
				UserObject user = hub.dataBean.users.get(param.get(0));
				if (user != null) {
					user.setPhotoVersion(0);

					hub.sendDeletePhotoVersion(param.get(0));
				}
			}
		} else if (command.equals("SYNCHRONIZATION")) {
			hub.sendSynId(param, conn);
		} else if (command.equals("SYNCHRONIZATIONNEW")) {
			hub.sendSynIdNew(param, conn);
		} else if (command.equals("SubOrganizationRequest") && param.size() == 1) {
			hub.subOrganizationRequest(param.get(0), conn);
		} else if (command.equals("SearchRequest") && param.size() == 2) {
			logger.info("[MobileServer] SearchRequest! ");
			hub.searchRequest(param.get(0), param.get(1), conn);
		} else if (command.equals("ROOM_SYNCH")) {
			hub.sendRoomSyn(param, conn);
		} else if (command.equals("SubOrganizationRequest") && param.size() == 2) {
			hub.newSubOrganizationRequest(param.get(0), param.get(1), conn);
		} else if (command.equals("SearchRequest") && param.size() == 3) {
			hub.newSearchRequest(param.get(0), param.get(1), param.get(2), conn);
		}
		// 2017-12-13
		else if (command.equals("GetUserAuthority")) {
			getUserAuthority(conn, param.get(0));
		} else if (command.equals("IncrementTransferCount")) {
			updateTransferCount(conn, param.get(0));
		}
		//
		else if (command.equals("MyFolderSave")) {
			hub.requestMyFolderBackup(conn, param.get(0), param.get(1));
		} else if (command.equals("MYFOLDER_USER_ADD") || command.equals("MYFOLDER_USER_DEL")
				|| command.equals("MYFOLDER_GROUP_ADD") || command.equals("MYFOLDER_GROUP_MOD")
				|| command.equals("MYFOLDER_GROUP_DEL") || command.equals("MYFOLDER_SUB_GROUP_ADD")) {
			hub.sendMyFolderEdit(command, param, conn);
		} else if (command.equals("Nick") && param.size() == 1) {
			if (param == null || conn == null)
				return;
			updateNickUserObject(conn, conn.itemKey, param.get(0));

			hub.setNick(param.get(0), conn);
		} else if (command.equals("NEW_REQUESTPHOTO")) {
			hub.new_searchPhotoInfo(param.get(0), conn);
		} else if (command.equals("GpsProvideConsent")) {
			hub.gpsProvideConsent(param.get(0), param.get(1), param.get(2), conn);
		} else if (command.equals("Login") && param.size() == 5) {
			String id = param.get(0);
			String password = param.get(1);
			String mobileKey = param.get(2);
			String phoneType = param.get(3);
			String m_bRetain = param.get(4);

			if (hub.checkLogin(id, password, mobileKey, conn)) {
				if (m_bRetain.equals("0")) {
					updatePhoneSerial(id, "", "");
				} else
					updatePhoneSerial(id, mobileKey, phoneType);
			}
		} else if (command.equals("NameLogin") && param.size() == 5) {
			String name = param.get(0);
			String password = param.get(1);
			String mobileKey = param.get(2);
			String phoneType = param.get(3);
			String m_bRetain = param.get(4);

			int count = 0;
			String result = "";

			Iterator it = hub.dataBean.users.values().iterator();
			while (it.hasNext()) {
				UserObject user = (UserObject) it.next();

				if (user.name.indexOf(name + "#") == 0) {
					logger.info("NameLogin Find:" + user.name + ", str:" + name);
					count++;

					// id, name, email
					result += user.id + "," + user.getUserInfo(0) + "," + user.getUserInfo(6) + "\t";
				}
			}

			if (count == 0)
				conn.send(codec.EncryptSEED("NoUser") + "\f");
			else if (count == 1) {
				String id = result.substring(0, result.indexOf(","));
				logger.info("count 1 ID=>" + id);
				conn.send(codec.EncryptSEED("Login\t" + id) + "\f");
			} else if (count > 1) {
				conn.send(codec.EncryptSEED("MultiUser\t" + result) + "\f");
			}
		} else if (command.equals("DROPUSER")) {
			conn.send(codec.EncryptSEED("DROP_SUCCESS\t0") + "\f");

			hub.deleteMyFolder(param.get(0));
		} else if (command.equals("USERSTATUS")) {
			hub.sendUserStatus(param.get(0), conn);
		} else if (command.equals("UserNameRequest") && param.size() == 1) {
			hub.sendName(param.get(0), conn);
		} else if (command.equals("GETID") && param.size() > 0) {
			if (hub != null) {
				for (int i = 0; i < param.size(); i++) {
					String userId = hub.dataBean.getIdFromPhone(param.get(i));

					if (userId != null) {
						conn.send(codec.EncryptSEED("SETID\t" + param.get(i) + "\t" + userId) + "\f");
					}
				}
			}

			conn.send(codec.EncryptSEED("SETIDEND") + "\f");
		} else if (command.equals("CompareTokenToServer")) {
			logger.info("command:" + command + ", p size:" + param.size() + ", param:" + param);
			compareTokenRequest(conn, param.get(0), param.get(1), param.get(2), param.get(3));
		} else if (command.equals("UpdatePinToServer")) {
			logger.info("command:" + command + ", p size:" + param.size() + ", param:" + param);
			pinUpdateRequest(conn, param.get(0), param.get(1));
		} else if (command.equals("TokenCompare")) {
			// compareToken(conn, param.get(0), param.get(1), param.get(2));
			// System.out.println("[MobileServer] command:"+command + ", p
			// size:"+param.size() + ", param:"+param);
			// compareToken(conn, param.get(0), param.get(1), param.get(2), param.get(3));
		} else if (command.equals("VOICELOG")) {
		} else if (command.equals("EVENTLOG")) {
			System.out.println("[EVENT_LOG] 0:" + param.get(0) + ", 1:" + param.get(1) + ", 2:" + param.get(2) + ", 3:"
					+ param.get(3) + ", 4:" + param.get(4) + ", 5:" + param.get(5) + ", 6:" + param.get(6));
		} else if (command.equals("PinUpdate")) {
			// System.out.println("[MobileServer] command:"+command + ", p
			// size:"+param.size() + ", param:"+param);
			// pinUpdate(conn, param.get(0), param.get(1));
		} else if (command.equals("CallRequest") && param.size() == 4) {
			System.out.println("CallRequest");
			ConcurrentLinkedQueue<Connector> conns = userMap.get(param.get(0));

			Connector tConn = null;

			if (conns != null) {
				for (Connector con : conns) {
					if (con.itemType.startsWith("Android")) {
						tConn = con;
						break;
					}
				}
			}

			if (tConn == null) {
				System.out.println("NoReply\t" + param.get(1) + "\t" + param.get(2));
				conn.send(codec.EncryptSEED("NoReply\t" + param.get(1) + "\t" + param.get(2)) + "\f");
			} else {
				System.out.println("CallRequest\t" + param.get(1) + "\t" + param.get(2) + "\t" + param.get(3));
				tConn.send(codec.EncryptSEED("CallRequest\t" + param.get(1) + "\t" + param.get(2) + "\t" + param.get(3))
						+ "\f");
			}
		} else if (command.equals("CallDecline") && param.size() == 4)

		{
			ConcurrentLinkedQueue<Connector> conns = userMap.get(param.get(0));

			Connector tConn = null;

			if (conns != null) {
				for (Connector con : conns) {
					if (con.itemType.startsWith("Android")) {
						tConn = con;
						break;
					}
				}
			}

			if (tConn != null) {
				tConn.send(codec.EncryptSEED("CallDecline\t" + param.get(1) + "\t" + param.get(2) + "\t" + param.get(3))
						+ "\f");
			}
		} else if (command.equals("CallAccept") && param.size() == 4)

		{
			ConcurrentLinkedQueue<Connector> conns = userMap.get(param.get(0));

			Connector tConn = null;

			if (conns != null) {
				for (Connector con : conns) {
					if (con.itemType.startsWith("Android")) {
						tConn = con;
						break;
					}
				}
			}

			if (tConn != null) {
				tConn.send(codec.EncryptSEED("CallAccept\t" + param.get(1) + "\t" + param.get(2) + "\t" + param.get(3))
						+ "\f");
			}
		} else if (command.equals("CallFinish") && param.size() == 4)

		{
			ConcurrentLinkedQueue<Connector> conns = userMap.get(param.get(0));

			Connector tConn = null;

			if (conns != null) {
				for (Connector con : conns) {
					if (con.itemType.startsWith("Android")) {
						tConn = con;
						break;
					}
				}
			}

			if (tConn != null) {
				tConn.send(codec.EncryptSEED("CallFinish\t" + param.get(1) + "\t" + param.get(2) + "\t" + param.get(3))
						+ "\f");
			}
		}
	}

	public void OnCloseCustom(Connector conn) {
		hub.OnClose(conn);
	}

	public void onConnected(Connector conn) {
		hub.sendName(conn);
		hub.sendNick(conn);
	}

	private void updateName(final Connector conn, final String userId, final String value) {
		
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
					dbconn.setAutoCommit(false);

					try {
						rset = stmt.executeQuery("UPDATE " + config.get("MSG_NAME_DB_TABLENAME") + " SET "
								+ config.get("MSG_NAME_DB_COLUMN") + "='" + value + "'" + " WHERE "
								+ config.get("MSG_NAME_DB_USERID_COLUMN") + "='" + userId + "'");
						dbconn.setAutoCommit(true);

					} catch (Exception ee) {
						conn.send(codec.EncryptSEED("NAME_FAILED") + "\f");

						ee.getStackTrace();
						return;
					}

					System.out.println("[MobileServer] updateName -> updateNameUserObject call");
					updateNameUserObject(conn, userId, value);
				} catch (Exception ee) {
					conn.send(codec.EncryptSEED("NAME_FAILED") + "\f");

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
					dbconn.setAutoCommit(false);

					try {
						rset = stmt.executeQuery("UPDATE " + config.get("MSG_NAME_DB_TABLENAME") + " SET "
								+ config.get("MSG_NAME_DB_COLUMN") + "='" + value + "'" + " WHERE "
								+ config.get("MSG_NAME_DB_USERID_COLUMN") + "='" + userId + "'");
						dbconn.setAutoCommit(true);

					} catch (Exception ee) {
						conn.send(codec.EncryptSEED("NAME_FAILED") + "\f");

						ee.getStackTrace();
						return;
					}

					System.out.println("[MobileServer] updateName -> updateNameUserObject call");
					updateNameUserObject(conn, userId, value);
				} catch (Exception ee) {
					conn.send(codec.EncryptSEED("NAME_FAILED") + "\f");

					ee.printStackTrace();
					return;
				} finally {

					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
			}
		};

		th.start();*/
	}

	private void updateNameUserObject(Connector conn, String userId, String name) {
		logger.info(userId);
		UserObject user = hub.dataBean.users.get(userId);
		if (user != null) {
			user.setName(name);
			conn.send(codec.EncryptSEED("NAME_CHANGED\t" + user.name) + "\f");

			hub.sendModUser(user);
			
			versionUpdate(conn, userId, 0, name);
		} else {
			logger.info("user is null [" + userId + "]");
		}
	}

	private void updateNickUserObject(Connector conn, String userId, String nick) {
		UserObject user = hub.dataBean.users.get(userId);
		if (user != null) {
			user.setNick(nick);

			hub.sendModUser(user);
			versionUpdate(conn, userId, 1, nick);
		}
	}

	private void photoVersionUpdate(final String userId) {

		//2018-01-13 leesh for Use PreparedStatement
		//2017-12-13
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection dbconn = null;
            	PreparedStatement ps = null;
				ResultSet rs = null;

				String sql = "SELECT CNT FROM PHOTO_CNT WHERE USER_ID=?";
				try {
					dbconn = dbcpCert.getConnection();
					ps = dbconn.prepareStatement(sql);
					
					try {
						ps.setString(1, userId);
						rs = ps.executeQuery();
						
						if (rs.next()) {
							String cnt = rs.getString(1);
							if (cnt != null) {
								UserObject user = hub.dataBean.users.get(userId);
								if (user != null) {
									user.setPhotoVersion(Integer.parseInt(cnt));
									hub.sendModPhotoVersion(userId, cnt);
								}
							}
						} else {
							UserObject user = hub.dataBean.users.get(userId);
							if (user != null) {
								user.setPhotoVersion(1);
								hub.sendModPhotoVersion(userId, Integer.toString(1));
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
					dbcpCert.freeConnection(dbconn, ps, rs);
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
					dbconn = dbcpCert.getConnection();
					stmt = dbconn.createStatement();
					dbconn.setAutoCommit(false);

					try {
						rset = stmt.executeQuery("select CNT from PHOTO_CNT where USER_ID like '" + userId + "'");
						if (rset.next()) {
							String cnt = rset.getString(1);
							if (cnt != null) {
								UserObject user = hub.dataBean.users.get(userId);
								if (user != null) {
									user.setPhotoVersion(Integer.parseInt(cnt));
									hub.sendModPhotoVersion(userId, cnt);
								}
							}
						} else {
							UserObject user = hub.dataBean.users.get(userId);
							if (user != null) {
								user.setPhotoVersion(1);
								hub.sendModPhotoVersion(userId, Integer.toString(1));
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

					dbcpCert.freeConnection(dbconn, stmt, rset);
				}
			}
		};

		th.start();*/
	}

	private void versionUpdate(Connector conn, final String userId, final int type, final String data) {
		final int NAME = 0;
		final int NICK = 1;
		
		//2017-12-13
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection dbconn = null;
				Statement stmt = null;
				ResultSet rset = null;

				try {
					dbconn = dbcpCert.getConnection();
					stmt = dbconn.createStatement();
					dbconn.setAutoCommit(false);

					try {
						rset = stmt.executeQuery("select USER_ID from PHOTO_CNT where USER_ID like '" + userId + "'");
						if (rset.next()) {
							if (type == NAME)
								stmt.execute("UPDATE PHOTO_CNT SET NAMEVERSION = ( SELECT nvl(NAMEVERSION,0)+1 FROM PHOTO_CNT WHERE USER_ID ='"
												+ userId + "') WHERE USER_ID = " + "'" + userId + "'");
							else if (type == NICK)
								stmt.execute("UPDATE PHOTO_CNT SET NICKVERSION = ( SELECT nvl(NICKVERSION,0)+1 FROM PHOTO_CNT WHERE USER_ID ='"
												+ userId + "') WHERE USER_ID = " + "'" + userId + "'");
						} else {
							stmt.execute("insert into PHOTO_CNT(USER_ID) values('" + userId + "')");
						}
							
						rset.close();
						dbconn.setAutoCommit(true);

						if (type == NAME) {
							rset = stmt.executeQuery("select NAMEVERSION from PHOTO_CNT where USER_ID like '" + userId + "'");
							
							
							String ver = "";
							if (rset.next()) {
								ver = rset.getString(1);
							} else
								return;

							UserObject user = hub.dataBean.users.get(userId);
							if (user != null)
								user.setNameVersion(Integer.parseInt(ver));
						} else if (type == NICK) {

							rset = stmt.executeQuery("select NICKVERSION from PHOTO_CNT where USER_ID like '" + userId + "'");

							// 2017-10-20 check logic rset.next no
							String ver = "";
							if (rset.next()) {
								ver = rset.getString(1);
							} else
								return;

							UserObject user = hub.dataBean.users.get(userId);
							if (user != null)
								user.setNickVersion(Integer.parseInt(ver));
						}

					} catch (Exception ee) {
						ee.printStackTrace();
						return;
					}
				} catch (Exception ee) {
					ee.printStackTrace();
					return;
				} finally {
					dbcpCert.freeConnection(dbconn, stmt, rset);
				}
            }
        };

        executorsPool.submit(runnable);
        //

		/*Thread th = new Thread() {
			public void start() {
				super.start();
			}

			public void run() {
				Connection dbconn = null;
				Statement stmt = null;
				ResultSet rset = null;

				try {
					dbconn = dbcpCert.getConnection();
					stmt = dbconn.createStatement();
					dbconn.setAutoCommit(false);

					try {
						rset = stmt.executeQuery("select USER_ID from PHOTO_CNT where USER_ID like '" + userId + "'");
						if (rset.next()) {
							if (type == NAME)
								stmt.execute(
										"UPDATE PHOTO_CNT SET NAMEVERSION = ( SELECT nvl(NAMEVERSION,0)+1 FROM PHOTO_CNT WHERE USER_ID ='"
												+ userId + "') WHERE USER_ID = " + "'" + userId + "'");
							else if (type == NICK)
								stmt.execute(
										"UPDATE PHOTO_CNT SET NICKVERSION = ( SELECT nvl(NICKVERSION,0)+1 FROM PHOTO_CNT WHERE USER_ID ='"
												+ userId + "') WHERE USER_ID = " + "'" + userId + "'");
						} else
							stmt.execute("insert into PHOTO_CNT(USER_ID) values('" + userId + "')");

						dbconn.setAutoCommit(true);

						if (type == NAME) {
							rset = stmt.executeQuery(
									"select NAMEVERSION from PHOTO_CNT where USER_ID like '" + userId + "'");

							String ver = "";
							if (rset.next()) {
								ver = rset.getString(1);
							} else
								return;

							UserObject user = hub.dataBean.users.get(userId);
							if (user != null)
								user.setNameVersion(Integer.parseInt(ver));
						} else if (type == NICK) {

							rset = stmt.executeQuery(
									"select NICKVERSION from PHOTO_CNT where USER_ID like '" + userId + "'");

							// 2017-10-20 check logic rset.next no
							String ver = "";
							if (rset.next()) {
								ver = rset.getString(1);
							} else
								return;

							UserObject user = hub.dataBean.users.get(userId);
							if (user != null)
								user.setNickVersion(Integer.parseInt(ver));
						}

					} catch (Exception ee) {
						ee.printStackTrace();
						return;
					}
				} catch (Exception ee) {
					ee.printStackTrace();
					return;
				} finally {
					dbcpCert.freeConnection(dbconn, stmt, rset);
				}
			}
		};

		th.start();*/
	}

	public String getNowDateTime() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);

		String retStr = year + "";
		if (month < 10)
			retStr += "0" + month;
		else
			retStr += month;
		if (day < 10)
			retStr += "0" + day;
		else
			retStr += day;
		if (hour < 10)
			retStr += "0" + hour;
		else
			retStr += hour;
		if (minute < 10)
			retStr += "0" + minute;
		else
			retStr += minute;

		return retStr;
	}

	private void compareTokenRequest(final Connector conn, final String userId, final String deviceToken,
			final String pin, final String pinDate) {
		
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

					String encPin = pin;

					// USER_ID -> TOKEN change
					rset = stmt.executeQuery(
							"select USER_ID, PIN, PIN_COUNT, PIN_SETDATE from MSG_USER_CENTER where USER_ID like '"
									+ userId + "'");
					if (rset.next()) {
						String rtnToken = rset.getString(1);
						String rtnPin = rset.getString(2);
						String rtnCount = rset.getString(3);
						String rtnDate = rset.getString(4);

						System.out.println("[MobileServer] compareToken userId:" + userId + ", rtnToken:" + rtnToken
								+ ", rtnPin:" + rtnPin + ", rtnCount:" + rtnCount + ", rtnDate:" + rtnDate + ", encPin:"
								+ encPin + ", pin:" + pin);

						// token & pin check
						if (rtnToken.equals(userId) && rtnPin.equals(encPin)) {
							// 30days over check
							String nowTime = pinDate;
							long MpDay = 24 * 60 * 60 * 1000;
							int startYear = Integer.parseInt(rtnDate.substring(0, 4));
							int startMonth = Integer.parseInt(rtnDate.substring(4, 6));
							int startDay = Integer.parseInt(rtnDate.substring(6, 8));
							int startHour = Integer.parseInt(rtnDate.substring(8, 10));
							int startMinute = Integer.parseInt(rtnDate.substring(10, 12));
							int nowYear = Integer.parseInt(nowTime.substring(0, 4));
							int nowMonth = Integer.parseInt(nowTime.substring(4, 6));
							int nowDay = Integer.parseInt(nowTime.substring(6, 8));
							int nowHour = Integer.parseInt(nowTime.substring(8, 10));
							int nowMinute = Integer.parseInt(nowTime.substring(10, 12));
							Calendar before = new GregorianCalendar(startYear, startMonth, startDay, startHour,
									startMinute);
							Calendar after = new GregorianCalendar(nowYear, nowMonth, nowDay, nowHour, nowMinute);
							long millis = after.getTime().getTime() - before.getTime().getTime();
							long days = millis / MpDay;

							System.out.println("[MobileServer] compareToken pinDate:" + pinDate + ", sY:" + startYear
									+ ", sM:" + startMonth + ", sD:" + startDay + ", sH:" + startHour + ", sMn:"
									+ startMinute + ", nY:" + nowYear + ", nM:" + nowMonth + ", nD:" + nowDay + ", nMn:"
									+ nowMinute + ", days:" + days);

							// 30
							if (days > 1) {
								System.out.println("[MobileServer] compareToken days 30 over. send REST_PIN");
								conn.send(codec.EncryptSEED("RESET_PIN") + "\f");
							} else {
								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT ='0'" + " WHERE USER_ID ='"
										+ userId + "'");
								dbconn.setAutoCommit(true);

								conn.send(codec.EncryptSEED("TOKEN_COMPARE_SUCCESS") + "\f");
								System.out.println("[MobileServer] compareToken send COMPARE_SUCCESS");
							}
						} else if (rtnToken.equals(userId) && !rtnPin.equals(encPin)) {
							// count check
							int totalCount = Integer.parseInt(rtnCount);
							++totalCount;
							if (totalCount >= 10) {
								// token delete
								// stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN =" + "''" + " WHERE
								// USER_ID ='" + userId + "'");

								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT =" + "'"
										+ Integer.toString(totalCount) + "'" + " WHERE USER_ID ='" + userId + "'");
								dbconn.setAutoCommit(true);
								conn.send(codec.EncryptSEED("BLOCK_PIN") + "\f");
								System.out.println("[MobileServer] compareToken wrong_pin 10 over block");
							} else {
								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT =" + "'"
										+ Integer.toString(totalCount) + "'" + " WHERE USER_ID ='" + userId + "'");
								dbconn.setAutoCommit(true);
								conn.send(codec.EncryptSEED("TOKEN_COMPARE_WRONG\t" + Integer.toString(totalCount))
										+ "\f");
								System.out.println("[MobileServer] compareToken wrong_pin count:" + totalCount);
							}
						} else
							conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
					} else
						conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
				} catch (Exception e) {
					conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
					e.printStackTrace();
				} finally {
					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
				
            }
        };
		
        executorsPool.submit(runnable);
        //
        
        /*Future future = executorsPool.submit(runnable);
		
        try{
            future.get();
        }catch(Exception e){
             e.getMessage());
        }*/
		
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

					String encPin = pin;

					// USER_ID -> TOKEN change
					rset = stmt.executeQuery(
							"select USER_ID, PIN, PIN_COUNT, PIN_SETDATE from MSG_USER_CENTER where USER_ID like '"
									+ userId + "'");
					if (rset.next()) {
						String rtnToken = rset.getString(1);
						String rtnPin = rset.getString(2);
						String rtnCount = rset.getString(3);
						String rtnDate = rset.getString(4);

						System.out.println("[MobileServer] compareToken userId:" + userId + ", rtnToken:" + rtnToken
								+ ", rtnPin:" + rtnPin + ", rtnCount:" + rtnCount + ", rtnDate:" + rtnDate + ", encPin:"
								+ encPin + ", pin:" + pin);

						// token & pin check
						if (rtnToken.equals(userId) && rtnPin.equals(encPin)) {
							// 30days over check
							String nowTime = pinDate;
							long MpDay = 24 * 60 * 60 * 1000;
							int startYear = Integer.parseInt(rtnDate.substring(0, 4));
							int startMonth = Integer.parseInt(rtnDate.substring(4, 6));
							int startDay = Integer.parseInt(rtnDate.substring(6, 8));
							int startHour = Integer.parseInt(rtnDate.substring(8, 10));
							int startMinute = Integer.parseInt(rtnDate.substring(10, 12));
							int nowYear = Integer.parseInt(nowTime.substring(0, 4));
							int nowMonth = Integer.parseInt(nowTime.substring(4, 6));
							int nowDay = Integer.parseInt(nowTime.substring(6, 8));
							int nowHour = Integer.parseInt(nowTime.substring(8, 10));
							int nowMinute = Integer.parseInt(nowTime.substring(10, 12));
							Calendar before = new GregorianCalendar(startYear, startMonth, startDay, startHour,
									startMinute);
							Calendar after = new GregorianCalendar(nowYear, nowMonth, nowDay, nowHour, nowMinute);
							long millis = after.getTime().getTime() - before.getTime().getTime();
							long days = millis / MpDay;

							System.out.println("[MobileServer] compareToken pinDate:" + pinDate + ", sY:" + startYear
									+ ", sM:" + startMonth + ", sD:" + startDay + ", sH:" + startHour + ", sMn:"
									+ startMinute + ", nY:" + nowYear + ", nM:" + nowMonth + ", nD:" + nowDay + ", nMn:"
									+ nowMinute + ", days:" + days);

							// 30
							if (days > 1) {
								System.out.println("[MobileServer] compareToken days 30 over. send REST_PIN");
								conn.send(codec.EncryptSEED("RESET_PIN") + "\f");
							} else {
								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT ='0'" + " WHERE USER_ID ='"
										+ userId + "'");
								dbconn.setAutoCommit(true);

								conn.send(codec.EncryptSEED("TOKEN_COMPARE_SUCCESS") + "\f");
								System.out.println("[MobileServer] compareToken send COMPARE_SUCCESS");
							}
						} else if (rtnToken.equals(userId) && !rtnPin.equals(encPin)) {
							// count check
							int totalCount = Integer.parseInt(rtnCount);
							++totalCount;
							if (totalCount >= 10) {
								// token delete
								// stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN =" + "''" + " WHERE
								// USER_ID ='" + userId + "'");

								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT =" + "'"
										+ Integer.toString(totalCount) + "'" + " WHERE USER_ID ='" + userId + "'");
								dbconn.setAutoCommit(true);
								conn.send(codec.EncryptSEED("BLOCK_PIN") + "\f");
								System.out.println("[MobileServer] compareToken wrong_pin 10 over block");
							} else {
								stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN_COUNT =" + "'"
										+ Integer.toString(totalCount) + "'" + " WHERE USER_ID ='" + userId + "'");
								dbconn.setAutoCommit(true);
								conn.send(codec.EncryptSEED("TOKEN_COMPARE_WRONG\t" + Integer.toString(totalCount))
										+ "\f");
								System.out.println("[MobileServer] compareToken wrong_pin count:" + totalCount);
							}
						} else
							conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
					} else
						conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
				} catch (Exception e) {
					conn.send(codec.EncryptSEED("CERTIFICATION_FAIL") + "\f");
					e.printStackTrace();
				} finally {
					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
			}
		};

		th.start();*/
	}

	private void pinUpdateRequest(final Connector conn, final String userId, final String newPin) {
		
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
					dbconn.setAutoCommit(false);

					rset = stmt.executeQuery("select PIN from MSG_USER_CENTER where USER_ID like '" + userId + "'");
					String oldPin = "";

					System.out.println("[MobileServer] updatePin start userId:" + userId);
					if (rset.next()) {
						oldPin = rset.getString(1);

						System.out.println("[MobileServer] updatePin oldPin:" + oldPin + ", newPin:" + newPin);
						if (oldPin.equals(newPin)) {
							conn.send(codec.EncryptSEED("REQUIRE_OTHER_PIN") + "\f");
							System.out.println("[MobileServer] updatePin exists pin.. send require other pin");
						} else {
							stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN ='" + newPin + "',  PIN_COUNT = '0',"
									+ " PIN_SETDATE ='" + getNowDateTime() + "'" + " WHERE USER_ID ='" + userId + "'");
							dbconn.setAutoCommit(true);
							conn.send(codec.EncryptSEED("PIN_UPDATE") + "\f");
							System.out.println("[MobileServer] updatePin success");
						}
					} else {
						stmt.execute("INSERT INTO MSG_USER_CENTER(USER_ID, PIN, PIN_COUNT, PIN_SETDATE) VALUES('"
								+ userId + "', '" + newPin + "', '0', '" + getNowDateTime() + "'");
						dbconn.setAutoCommit(true);
						conn.send(codec.EncryptSEED("PIN_UPDATE") + "\f");
						System.out.println("[MobileServer] updatePin not found id.. insert data");
					}
				} catch (Exception e) {
					conn.send(codec.EncryptSEED("PIN_FAILED") + "\f");
					System.out.println("[MobileServer] updatePin exception fail:" + e.toString());
					e.printStackTrace();
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
					dbconn.setAutoCommit(false);

					rset = stmt.executeQuery("select PIN from MSG_USER_CENTER where USER_ID like '" + userId + "'");
					String oldPin = "";

					System.out.println("[MobileServer] updatePin start userId:" + userId);
					if (rset.next()) {
						oldPin = rset.getString(1);

						System.out.println("[MobileServer] updatePin oldPin:" + oldPin + ", newPin:" + newPin);
						if (oldPin.equals(newPin)) {
							conn.send(codec.EncryptSEED("REQUIRE_OTHER_PIN") + "\f");
							System.out.println("[MobileServer] updatePin exists pin.. send require other pin");
						} else {
							stmt.execute("UPDATE " + "MSG_USER_CENTER" + " SET PIN ='" + newPin + "',  PIN_COUNT = '0',"
									+ " PIN_SETDATE ='" + getNowDateTime() + "'" + " WHERE USER_ID ='" + userId + "'");
							dbconn.setAutoCommit(true);
							conn.send(codec.EncryptSEED("PIN_UPDATE") + "\f");
							System.out.println("[MobileServer] updatePin success");
						}
					} else {
						stmt.execute("INSERT INTO MSG_USER_CENTER(USER_ID, PIN, PIN_COUNT, PIN_SETDATE) VALUES('"
								+ userId + "', '" + newPin + "', '0', '" + getNowDateTime() + "'");
						dbconn.setAutoCommit(true);
						conn.send(codec.EncryptSEED("PIN_UPDATE") + "\f");
						System.out.println("[MobileServer] updatePin not found id.. insert data");
					}
				} catch (Exception e) {
					conn.send(codec.EncryptSEED("PIN_FAILED") + "\f");
					System.out.println("[MobileServer] updatePin exception fail:" + e.toString());
					e.printStackTrace();
				} finally {
					dbcpMsger.freeConnection(dbconn, stmt, rset);
				}
			}
		};

		th.start();*/
	}

	// 2017-12-13
	private void getUserAuthority(Connector connector, String userId) {
		
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection conn = null;
        		PreparedStatement ps = null;
        		ResultSet rs = null;
        		String authCount = null, authStartDate = null, authEndDate = null, nowDate = null;

        		try {
        			conn = dbcpMsger.getConnection();

        			ps = conn.prepareStatement(query);
        			ps.setString(1, userId);
        			rs = ps.executeQuery();
        			if (rs.next()) {
        				authCount = rs.getString(1);
        				authStartDate = rs.getString(2);
        				authEndDate = rs.getString(3);
        				nowDate = getYYYYMMDDHHNNSS("yyyyMMdd");

        				System.out.println("[MobileServer] getUserAuthority rs next. count:" + authCount + ", startDate:"
        						+ authStartDate + ", endDate:" + authEndDate + ", ServerNowDate:" + nowDate);
        				int checkCount = Integer.parseInt(authCount);
        				if (checkCount >= 5) {
        					System.out.println("[MobileServer] getUserAuthority response:5");
        					connector.send(codec.EncryptSEED("MESSAGE_USER_AUTH\t5") + "\f");
        				} else {
        					int startYear = Integer.parseInt(authStartDate.substring(0, 4));
        					int startMonth = Integer.parseInt(authStartDate.substring(4, 6));
        					int startDay = Integer.parseInt(authStartDate.substring(6, 8));

        					int endYear = Integer.parseInt(authEndDate.substring(0, 4));
        					int endMonth = Integer.parseInt(authEndDate.substring(4, 6));
        					int endDay = Integer.parseInt(authEndDate.substring(6, 8));

        					int nowYear = Integer.parseInt(nowDate.substring(0, 4));
        					int nowMonth = Integer.parseInt(nowDate.substring(4, 6));
        					int nowDay = Integer.parseInt(nowDate.substring(6, 8));

        					if ((startYear <= nowYear && nowYear <= endYear) && (startMonth <= nowMonth && nowMonth <= endMonth)
        							&& (startDay <= nowDay && nowDay <= endDay)) {
        						System.out.println("[MobileServer] getUserAuthority response:1");
        						connector.send(codec.EncryptSEED("MESSAGE_USER_AUTH\t1\t" + checkCount) + "\f"); // 1:auth done.
        					} else {
        						System.out.println("[MobileServer] getUserAuthority response:4");
        						connector.send(codec.EncryptSEED("MESSAGE_USER_AUTH\t4") + "\f"); // 4:date miss.
        					}
        				}
        			} else {
        				System.out.println("[MobileServer] getUserAuthority response:3");
        				connector.send(codec.EncryptSEED("MESSAGE_USER_AUTH\t3") + "\f"); // 3:No Data
        			}
        		} catch (Exception e) {
        			e.getMessage();
        			System.out.println("[MobileServer] getUserAuthority response:2");
        			connector.send(codec.EncryptSEED("MESSAGE_USER_AUTH\t2") + "\f"); // 2:connect error
        		} finally {
        			dbcpMsger.freeConnection(conn, ps, rs);
        		}
            }
        };

        executorsPool.submit(runnable);
	}
	//

	// 2017-12-13
	private void updateTransferCount(Connector connector, String userId) {
		
		Runnable runnable = new Runnable(){
            @Override
            public void run(){
            	Connection conn = null;
        		PreparedStatement ps = null;
        		ResultSet rs = null;

        		try {
        			conn = dbcpMsger.getConnection();

        			ps = conn.prepareStatement("SELECT MSG_SEND_COUNT FROM MSG_USER_CENTER WHERE USER_ID = ?");
        			ps.setString(1, userId);
        			rs = ps.executeQuery();

        			if (rs.next()) {
        				ps.execute(
        						"UPDATE MSG_USER_CENTER SET MSG_SEND_COUNT = ( SELECT nvl(MSG_SEND_COUNT,0)+1 FROM MSG_USER_CENTER WHERE USER_ID ='"
        								+ userId + "') WHERE USER_ID = " + "'" + userId + "'");
        			}

        			conn.setAutoCommit(true);
        			System.out.println("[MobileServer] updateTransferCount done.");
        		} catch (Exception e) {
        			e.printStackTrace();
        		} finally {
        			dbcpMsger.freeConnection(conn, ps, rs);
        		}
            }
        };

        executorsPool.submit(runnable);
	}
	//

}
