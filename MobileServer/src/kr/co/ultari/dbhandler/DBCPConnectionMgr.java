package kr.co.ultari.dbhandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDriver;
import org.apache.commons.pool.impl.GenericObjectPool;
 
public class DBCPConnectionMgr 
{
    String Driver=""; 
    String Url="";
    String Id="";
    String Pwd = "";
    int MaxCnt = 0;
    int IdleCnt = 0;
    String PoolName = "";
    PoolingDriver driver = null;
    
    public DBCPConnectionMgr() 
    {
        try{
            setupDriver(Driver, Url, Id, Pwd);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
 
    public DBCPConnectionMgr(String Driver, String Url, String Id, String Pwd, String MaxCnt, String IdleCnt, String PoolName)
    {
    	this.Driver = Driver;
    	this.Url = Url;
    	this.Id = Id;
    	this.Pwd = Pwd;
    	this.MaxCnt = Integer.parseInt(MaxCnt);
    	this.IdleCnt = Integer.parseInt(IdleCnt);
    	this.PoolName = PoolName;
        try{
            setupDriver(Driver, Url, Id, Pwd);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    public Connection getConnection()
    {
        Connection con = null;
        try {
        	
            con = DriverManager.getConnection("jdbc:apache:commons:dbcp:" + PoolName);
            if(con==null||con.isClosed())
            {
            	setupDriver(Driver,Url,Id,Pwd);
            	con = DriverManager.getConnection("jdbc:apache:commons:dbcp:" + PoolName);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
        }
        return con;
    }
        
    public void setupDriver(String jdbcDriver, 
                                   String jdbcURL,
                                   String user,
                                   String password) throws Exception {
        Class.forName(jdbcDriver);
        
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        connectionPool.setMaxActive(MaxCnt);
        connectionPool.setMaxIdle(IdleCnt);
        
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            jdbcURL, // JDBC URL
            user, 
            password);
        
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
            connectionFactory,
            connectionPool,
            null, // statement pool
            null, // .
            false, // read only
            true); // auto commit 
        
        driver = new PoolingDriver();
        driver.registerPool(PoolName, connectionPool);
        
    }
    
    public void RemovePool()
    {
    	try
    	{
	    	GenericObjectPool connectionPool = (GenericObjectPool)driver.getConnectionPool(PoolName);
	    	connectionPool.clear();
    	}
    	catch(Exception e){}
    }
    
    public void freeConnection(Connection con, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
 
    public void freeConnection(Connection con, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void freeConnection(Connection con, PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    

    public void freeConnection(Connection con, Statement stmt) {
        try {
            if (stmt != null) stmt.close();
            freeConnection(con);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void freeConnection(Connection con) {
        try {
            if (con != null) con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void freeConnection(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
 
    
    public void freeConnection(PreparedStatement pstmt) {
        try {
            if (pstmt != null) pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
 
    public void freeConnection(ResultSet rs) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }    
    
}
