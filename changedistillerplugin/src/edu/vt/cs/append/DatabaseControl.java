package edu.vt.cs.append;

import java.sql.*;

public class DatabaseControl {
	boolean openornot = false;
//	String databasename = "1111pldi/commonsio.db";
//	static String databasename2 = "1111pldi/commonsio.db";
	//String databasename = "1030pldi/springsec.db";
	//static String databasename2 = "1030pldi/springsec.db";
//	String databasename = "1030pldi/mockito.db";
//	static String databasename2 = "1030pldi/mockito.db";
	//String databasename = "1030pldi/guava.db";
	//static String databasename2 = "1030pldi/guava.db";
	
//	String databasename = "1114pldi/1114lucene.db";
//	static String databasename2 = "1114pldi/1114lucene.db";
//	String databasename = "1114pldi/1114bukkit.db";
//	static String databasename2 = "1114pldi/1114bukkit.db";
	
//	String databasename = "1114pldi/1114android.db";
//	static String databasename2 = "1114pldi/1114android.db";
	
	String databasename = CommonValue.database_path;
	static String databasename2 = CommonValue.database_path;
	
	
	// to compare whether the two patterns are same, including fuzzy toleration, shengzhe, 18Apr20
	public boolean compare_same(String x, String y) {
		int fuzzy = 0;
		if (x.length() != y.length()) return false;
		for (int i = 0, j = 0; i<x.length() && j<y.length();) {
			if (i>0 && x.charAt(i-1)=='_' && '0'<=x.charAt(i) && x.charAt(i)<='9') {
				while (i<x.length() && '0'<=x.charAt(i) && x.charAt(i)<='9') i++;
			}
			if (j>0 && y.charAt(j-1)=='_' && '0'<=y.charAt(j) && y.charAt(j)<='9') {
				while (j<y.length() && '0'<=y.charAt(j) && y.charAt(j)<='9') j++;
			}
			if (i<x.length() && j<y.length() && x.charAt(i) != y.charAt(j)) {			
				return false;
			}
			i++; j++;
		}
		return true;
	}
	
	public int insertpattern(String oldp, String newp, String oldv, String newv, String type, String level) {
		if (openornot == false)
			return -1;
		int nowID;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename);   
			Statement stat = conn.createStatement();   
			ResultSet rs = stat.executeQuery("select * from Pattern;");
	        nowID = 0;
			while (rs.next()) {
	        	nowID++;
//				if (rs.getString("old").equals(oldp) && rs.getString("new").equals(newp)) {
//					insertversion(nowID, oldv, newv);
//					return nowID;
//				}
	        	if (compare_same(rs.getString("old"), oldp) && compare_same(rs.getString("new"),newp)) {
	        		conn.close();
	        		insertversion(nowID, oldv, newv);
					return nowID;
	        	}
	        }
			conn.close();
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename);   
			PreparedStatement prep = conn.prepareStatement(
					"insert into Pattern values (?, ?, ?, ?, ?);");
			nowID++;
			prep.setString(1, String.valueOf(nowID));
			prep.setString(2, oldp);
			prep.setString(3, newp);
			prep.setString(4, type);
			prep.setString(5, level);
			prep.addBatch();
			
	        conn.setAutoCommit(false);   
	        prep.executeBatch();   
	        conn.setAutoCommit(true);   	
	        conn.close();
	        insertversion(nowID, oldv, newv);
		}
		catch (Exception e) {
			e.printStackTrace();
			nowID = -1;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {
				}
			}
		}
        return nowID;
	}
	
	public void insertversion(int nowID, String oldv, String newv) {
		if (openornot == false)
			return;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename);   
			Statement stat = conn.createStatement();   
			ResultSet rs = stat.executeQuery("select * from Version;");	      
			while (rs.next()) {	        		        	
				if (rs.getInt("patternid") == nowID 
						&& rs.getString("oversion").equals(oldv) && rs.getString("nversion").equals(newv)) {
					return;
				}
	        }			
			conn.close();
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename);
	        PreparedStatement prep = conn.prepareStatement(
					"insert into Version values (?, ?, ?);");			
			prep.setString(1, String.valueOf(nowID));	
			prep.setString(2, oldv);
			prep.setString(3, newv);
			prep.addBatch();
			
	        //conn.setAutoCommit(false);   
	        prep.executeBatch();   
	        //conn.setAutoCommit(true);   
	        conn.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			nowID = -1;
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {
				}
			}
		}
        return;
	}
	
	public void insertsnippet(String old_code, String new_code, String project, String commitnum, String patternid, String vc) {
//		if (patternid.equals("-1")) {
//			return;
//		}
		if (openornot == false) {
			return;
		}
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename);   
			Statement stat = conn.createStatement();   
			ResultSet rs = stat.executeQuery("select * from Snippet;"); 
			while (rs.next()) {
				try {
					if (rs.getString("old_code").equals(old_code) && rs.getString("new_code").equals(new_code)
							&& rs.getString("project").equals(project)
							&& rs.getString("commitnum").equals(commitnum) && rs.getString("patternid").equals(patternid)) {
						return;
					}
				}
				finally {
				}
			}
			conn.close();
			conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename); 
			String ovc = vc.split("-")[0];
			String nvc = vc.split("-")[1];
			PreparedStatement prep = conn.prepareStatement(
					"insert into Snippet values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			prep.setString(1, old_code);
			prep.setString(2, new_code);
			prep.setString(3, project);
			prep.setString(4, commitnum);
			prep.setString(5, CommonValue.now_ofile);
			prep.setString(6, CommonValue.now_nfile);
			prep.setString(7, patternid);
			prep.setString(8, vc);
			prep.setString(9, ovc);
			prep.setString(10, nvc);
			prep.addBatch();
			
	       // conn.setAutoCommit(false);   
	        prep.executeBatch();   
	        //conn.setAutoCommit(true);   
	        conn.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (conn != null) {
				try {
					conn.close();
				}
				catch (Exception e) {
				}
			}
		}
	}
	
	public static void init_tables() throws SQLException, ClassNotFoundException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + CommonValue.workspace + databasename2);   
        Statement stat = conn.createStatement();   
        try {
        	stat.executeUpdate("create table Pattern(ID integer primary key, old text, new text, type text, level integer);");   
        }
        catch (SQLException e) {
        	System.out.println(e);
        }
        try {
        	stat.executeUpdate("create table Version(patternid integer, oversion text, nversion text);");   
        }
        catch (SQLException e) {
        	System.out.println(e);
        }
        try {
        	stat.executeUpdate("create table Snippet(old_code text, new_code text, project text, commitnum text, ofile text, nfile text, patternid integer, verchange text, over text, nver text);");
		}
		catch (SQLException e) {
			System.out.println(e);
		}
        finally {
        	stat.close();
        	conn.close();
        }
	}
	public static void main(String[] args) throws Exception {
		init_tables();
		return;
	}
}
