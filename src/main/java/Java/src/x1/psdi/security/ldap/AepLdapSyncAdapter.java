package x1.psdi.security.ldap;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import psdi.app.person.Person;
import psdi.app.signature.GroupUser;
import psdi.app.signature.MaxGroup;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueInfo;
import psdi.mbo.SqlFormat;
import psdi.security.ldap.*;
import psdi.server.MXServer;
import psdi.util.MXException;
import psdi.util.MXSystemException;

public class AepLdapSyncAdapter extends LdapSyncAdapter
{
  String everyoneGroup = null;

  private String currentObjname = null;
  private String currentColname = null;
  private boolean createUser = false;  
  protected HashMap cachedPreparedStatements = new HashMap();
  
  /**
   * 
   */
  
  public void syncUser(LdapSyncEvent event)
		  throws LdapSyncException {
	  
	  if(event.getEventType()==200)
	  {
		  syncPerson(event);
	  }
	  else if(event.getEventType()==800)
	  {  
		  syncMemUser(event);
	  }
	  else{
		  System.out.println(" *************** event type is not 200 or 800 ********************");
	  }
	  
  }

  public void syncPerson(LdapSyncEvent event)
		    throws LdapSyncException
		  {
		    MboRemote userMbo = null;
		    MboRemote personMbo = null;
		    this.currentObjname = null;
		    this.currentColname = null;
		    String tableId = null;
		    try
		    {
		      Connection con = getConnection();
		      int phoneTypeAsInt = MXServer.getMXServer().getMaximoDD().getMboSetInfo("PHONE").getMboValueInfo("TYPE").getTypeAsInt();

		      DataMap userDataMap = event.getUserDataMap();
		      SyncData syncData = event.getUserSyncData();
		      if (getLogger().isDebugEnabled()) {
		        getLogger().debug("syncPerson, syncData = " + syncData.toString());
		      }
		      
		     
		      tableId = userDataMap.getTableId("PERSON");
		      MboSetRemote personSet = getPersonSet(con, userDataMap, tableId, syncData);
		      
		      if (personSet.isEmpty())
		      {
		        personMbo = insertRecord(con, tableId, personSet, syncData, userDataMap);
		      }
		      else
		      {
		        personMbo = personSet.getMbo(0);
		        updateRecord(con, tableId, personMbo, syncData, userDataMap);
		      }

		      if (personMbo == null) {
		        return;
		      }
		      Iterator tableIdIterator = userDataMap.getTableIds();
		      while (tableIdIterator.hasNext())
		      {
		        tableId = (String)tableIdIterator.next();
		        String objectName = userDataMap.getObjectName(tableId, con);
		        if (objectName.equalsIgnoreCase("EMAIL"))
		        {
		          MboSetRemote emailSet = personMbo.getMboSet("PRIMARYEMAIL");
		          MboRemote emailMbo = emailSet.getMbo(0);

		          if (emailMbo == null)
		            emailMbo = insertRecord(con, tableId, emailSet, syncData, userDataMap);
		          else
		            updateRecord(con, tableId, emailMbo, syncData, userDataMap);
		        }
		        else if (objectName.equalsIgnoreCase("PHONE"))
		        {
		          MboSetRemote phoneSet = personMbo.getMboSet("PHONE");

		          Object type = getDataMapValue(con, tableId, "TYPE", syncData, userDataMap);
		          if (phoneTypeAsInt == 1)
		            type = ((String)type).toUpperCase();
		          else if (phoneTypeAsInt == 2)
		            type = ((String)type).toLowerCase();
		          Object phonenum = getDataMapValue(con, tableId, "PHONENUM", syncData, userDataMap);
		          if (((phonenum instanceof String)) && (phonenum != null) && (((String)phonenum).trim().length() == 0)) {
		            phonenum = null;
		          }
		          if (phonenum == null)
		            continue;
		          if ((phonenum instanceof String))
		            phonenum = ((String)phonenum).trim();
		          MboRemote test = null;
		          MboRemote phoneMbo = null;
		          for (int xx = 0; (test = phoneSet.getMbo(xx)) != null; xx++)
		          {
		            if ((!test.getString("type").equals((String)type)) || (!test.getString("phonenum").equals((String)phonenum)))
		              continue;
		            phoneMbo = test;
		            break;
		          }

		          if (phoneMbo == null)
		            phoneMbo = insertRecord(con, tableId, phoneSet, syncData, userDataMap);
		          else
		            updateRecord(con, tableId, phoneMbo, syncData, userDataMap);
		        }
		        else {
		          if ((objectName.equalsIgnoreCase("MAXUSER")) || (objectName.equalsIgnoreCase("PERSON")) ||(objectName.equalsIgnoreCase("LABOR")) ||(objectName.equalsIgnoreCase("LABORCRAFT")))
		        	  continue;
		          //System.out.println("*****Other Object name ******** :"+objectName);
		          MboSetRemote otherSet = personMbo.getMboSet(objectName);
		          MboRemote otherMbo = otherSet.getMbo(0);

		          if (otherMbo == null)
		            otherMbo = insertRecord(con, tableId, otherSet, syncData, userDataMap);
		          else {
		            updateRecord(con, tableId, otherMbo, syncData, userDataMap);
		          }
		        }
		      }
		      personMbo.getThisMboSet().save();
		    }
		    catch (MXException e2)
		    {
		      String msg_userid = "";
		      try
		      {
		        if (personMbo != null)
		          msg_userid = " personid = " + personMbo.getString("personid");
		      } catch (Exception localException) {
		      }
		      String temp = "";
		      if (this.currentObjname != null)
		      {
		        temp = temp + " " + this.currentObjname;
		        if (this.currentColname != null)
		          temp = temp + "." + this.currentColname;
		      }
		      this.currentObjname = null;
		      this.currentColname = null;

		      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter01").getMessage() + msg_userid + " " + e2.getMessage() + temp, e2);
		    }
		    catch (RemoteException e3)
		    {
		      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter01").getMessage() + " " + e3.getMessage(), e3);
		    }
		    this.currentObjname = null;
		    this.currentColname = null;
		  }
// Person records ended


  public void syncMemUser(LdapSyncEvent event)
    throws LdapSyncException
  {
    MboRemote userMbo = null;
    MboRemote personMbo = null;
    this.currentObjname = null;
    this.currentColname = null;
    try
    {
      Connection con = getConnection();
      // mckenzie changed
      MboValueInfo phoneTypeValueInfo = MXServer.getMXServer().getMaximoDD().getMboSetInfo("PHONE").getMboValueInfo("TYPE");
      int phoneTypeAsInt = phoneTypeValueInfo.getTypeAsInt();
      // mckenzie changed

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("syncMemUser, phoneTypeAsInt = " + phoneTypeAsInt);
      //  getLogger().debug("syncMemUser, phoneTypeValueInfo.getType() = " + phoneTypeValueInfo.getType());
      //}
      // mckenzie added - end

      DataMap userDataMap = event.getUserDataMap();
      SyncData syncData = event.getUserSyncData();
      if (getLogger().isDebugEnabled()) {
        // mckenzie added - start
        getLogger().debug("syncMemUser, syncData = " + syncData.toString());
        getLogger().debug("syncMemUser, userDataMap = " + userDataMap.toString());
        // mckenzie added - end
      }

      String tableId = userDataMap.getTableId("MAXUSER");
      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("syncMemUser, tableId = " + tableId);
      //}
      // mckenzie added - end

      // mckenzie changed -- start
      // if person does not exist or is inactive, exit sync and do not create user record
      String personTableId = userDataMap.getTableId("PERSON");
      MboSetRemote personSet = getPersonSet(con, userDataMap, personTableId, syncData);
      if (personSet.isEmpty()) {
        return;
      }
      else
      {
        personMbo = personSet.getMbo(0);
        if (personMbo == null) {
          return;
        }
        if (!((Person)personMbo).isActive()){
          return;
        }
      }
      // mckenzie changed -- end

      MboSetRemote userSet = getMaxUserSet(con, userDataMap, tableId, syncData);
      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("syncMemUser, userSet.isEmpty() = " + userSet.isEmpty());
      //}
      // mckenzie added - end

      if (userSet.isEmpty())
      {
        // mckenzie added - start
        // if the user is not in a group already within Maximo, then do not add
        Object userId = getDataMapValue(con, tableId, "userid", syncData, userDataMap);
        if (!(((userId instanceof String)) && (userId != null) && (((String)userId).trim().length() == 0))) {
          if(!isUserInExistingGroup((String)userId)) {
            return;
          }
        }
        // mckenzie added - end
        userMbo = insertRecord(con, tableId, userSet, syncData, userDataMap);
      }
      else
      {
        userMbo = userSet.getMbo(0);
        updateRecord(con, tableId, userMbo, syncData, userDataMap);
      }

      if (userMbo == null)
        return;

      // mckenzie changed -- start
      // moved this above to check for person record before creating
      // tableId = userDataMap.getTableId("PERSON");
      // MboSetRemote personSet = userMbo.getMboSet("PERSON");
      // if (personSet.isEmpty())
      // {
      //   personMbo = insertRecord(con, tableId, personSet, syncData, userDataMap);
      // }
      // else
      // {
      //   personMbo = personSet.getMbo(0);
      //   updateRecord(con, tableId, personMbo, syncData, userDataMap);
      // }
      //
      // if (personMbo == null) {
      //   return;
      // }
      // mckenzie changed -- end
      Iterator tableIdIterator = userDataMap.getTableIds();
      while (tableIdIterator.hasNext())
      {
        tableId = (String)tableIdIterator.next();
        String objectName = userDataMap.getObjectName(tableId, con);
        if (objectName.equalsIgnoreCase("EMAIL"))
        {
          // mckenzie removed -- start
          // do not want to sync this info
          // MboSetRemote emailSet = personMbo.getMboSet("PRIMARYEMAIL");
          // MboRemote emailMbo = emailSet.getMbo(0);

          // if (emailMbo == null)
          //   emailMbo = insertRecord(con, tableId, emailSet, syncData, userDataMap);
          // else
          //   updateRecord(con, tableId, emailMbo, syncData, userDataMap);
          // mckenzie removed -- end
        }
        else if (objectName.equalsIgnoreCase("PHONE"))
        {
          // mckenzie removed -- start
          // do not want to sync this info
          /*
          MboSetRemote phoneSet = personMbo.getMboSet("PHONE");

          Object type = getDataMapValue(con, tableId, "TYPE", syncData, userDataMap);
          if (phoneTypeAsInt == 1)
            type = ((String)type).toUpperCase();
          else if (phoneTypeAsInt == 2)
            type = ((String)type).toLowerCase();
          Object phonenum = getDataMapValue(con, tableId, "PHONENUM", syncData, userDataMap);
          if (((phonenum instanceof String)) && (phonenum != null) && (((String)phonenum).trim().length() == 0)) {
            phonenum = null;
          }
          if (phonenum == null)
            continue;
          if ((phonenum instanceof String))
            phonenum = ((String)phonenum).trim();
          MboRemote test = null;
          MboRemote phoneMbo = null;
          for (int xx = 0; (test = phoneSet.getMbo(xx)) != null; xx++)
          {
            if ((!test.getString("type").equals((String)type)) || (!test.getString("phonenum").equals((String)phonenum)))
              continue;
            phoneMbo = test;
            break;
          }

          if (phoneMbo == null)
            phoneMbo = insertRecord(con, tableId, phoneSet, syncData, userDataMap);
          else
            updateRecord(con, tableId, phoneMbo, syncData, userDataMap);
          */
        }
        else {
          if ((objectName.equalsIgnoreCase("MAXUSER")) || (objectName.equalsIgnoreCase("PERSON")))
            continue;
          // mckenzie removed -- start
          // do not want to sync this info
          /*
          MboSetRemote otherSet = userMbo.getMboSet(objectName);
          MboRemote otherMbo = otherSet.getMbo(0);

          if (otherMbo == null)
            otherMbo = insertRecord(con, tableId, otherSet, syncData, userDataMap);
          else {
            updateRecord(con, tableId, otherMbo, syncData, userDataMap);
          }
          */
          // mckenzie removed -- end
        }
      }
      userMbo.getThisMboSet().save();
    }
    catch (MXException e2)
    {
      String msg_userid = "";
      try
      {
        if (userMbo != null)
          msg_userid = " userid = " + userMbo.getString("userid");
      } catch (Exception localException) {
      }
      String temp = "";
      if (this.currentObjname != null)
      {
        temp = temp + " " + this.currentObjname;
        if (this.currentColname != null)
          temp = temp + "." + this.currentColname;
      }
      this.currentObjname = null;
      this.currentColname = null;

      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter01").getMessage() + msg_userid + " " + e2.getMessage() + temp, e2);
    }
    catch (RemoteException e3)
    {
      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter01").getMessage() + " " + e3.getMessage(), e3);
    }
    this.currentObjname = null;
    this.currentColname = null;
  }

  private MboSetRemote getMaxUserSet(Connection con, DataMap userDataMap, String tableId, SyncData syncData)
    throws LdapSyncException, MXException, RemoteException
  {
    Object userid = getDataMapValue(con, tableId, "USERID", syncData, userDataMap);
    MboSetRemote userSet = MXServer.getMXServer().getMboSet("MAXUSER", getUserInfo());
    SqlFormat sqf = new SqlFormat(getUserInfo(), "userid = :1");
    if ((userid instanceof String))
      userid = ((String)userid).trim();
    sqf.setObject(1, "MAXUSER", "USERID", (String)userid);
    userSet.setWhere(sqf.format());
    return userSet;
  }

  // mckenzie added -- start
  // determine if the given userId (sID) has a person record that is active
  private boolean hasActivePersonRecord(String userId)
    throws LdapSyncException
  {
    try {
      MboSetRemote personSet = MXServer.getMXServer().getMboSet("PERSON", getUserInfo());
      SqlFormat sqf = new SqlFormat(getUserInfo(), "personid = :1");
      sqf.setObject(1, "PERSON", "PERSONID", userId.trim());
      personSet.setWhere(sqf.format());
      if (personSet.isEmpty()) {
        return false;
      } else {
        MboRemote personMbo = personSet.getMbo(0);
        if (personMbo == null) {
          return false;
        }
        if (!((Person) personMbo).isActive()) {
          return false;
        }
      }
      return true;
    }
    catch (Exception ex)
    {
      throw new LdapSyncException(ex);
    }
  }
  // mckenzie added -- end

  // mckenzie added -- start
  // determine if the given group already exists in maximo
  private boolean groupExists(String groupName)
    throws LdapSyncException
  {
    try {
      MboSetRemote groupSet = MXServer.getMXServer().getMboSet("MAXGROUP", getUserInfo());
      SqlFormat sqf = new SqlFormat(getUserInfo(), "groupname = :1");
      sqf.setObject(1, "MAXGROUP", "GROUPNAME", groupName.trim());
      groupSet.setWhere(sqf.format());
      if (groupSet.isEmpty()) {
        return false;
      } else {
        MboRemote groupMbo = groupSet.getMbo(0);
        if (groupMbo == null) {
          return false;
        }
      }
      return true;
    }
    catch (Exception ex)
    {
      throw new LdapSyncException(ex);
    }
  }
  // mckenzie added -- end

  // mckenzie added -- start
  // determine if the given userId (sID) exists in a group already in Maximo
  private boolean isUserInExistingGroup(String userId)
    throws LdapSyncException
  {
    try {
      // get set of groups the given user belongs to
      MboSetRemote userGroupSet = MXServer.getMXServer().getMboSet("GROUPUSER", getUserInfo());
      SqlFormat sqf = new SqlFormat(getUserInfo(), "userid = :1");
      sqf.setObject(1, "GROUPUSER", "USERID", userId.trim());
      userGroupSet.setWhere(sqf.format());

      if (!userGroupSet.isEmpty()) {
        String userGroupName;
        // check all groups the user belongs to
        for(MboRemote userGroupMbo = userGroupSet.moveFirst();
            userGroupMbo != null;
            userGroupMbo = userGroupSet.moveNext())
        {
          userGroupName = userGroupMbo.getString("GROUPNAME");
          // check if the user's group is in Maximo --
          //  if it is, then user is in an existing group
          if(groupExists(userGroupName)) {
            return true;
          }
        }
      }
      // if the user appears in no existing groups, user is not in an existing group
      return false;
    }
    catch (Exception ex)
    {
      throw new LdapSyncException(ex);
    }
  }
  // mckenzie added -- end

  private MboSetRemote getPersonSet(Connection con, DataMap userDataMap, String tableId, SyncData syncData)
		    throws LdapSyncException, MXException, RemoteException
		  {
		    Object personid = getDataMapValue(con, tableId, "PERSONID", syncData, userDataMap);
		    MboSetRemote personSet = MXServer.getMXServer().getMboSet("PERSON", getUserInfo());
		     SqlFormat sqf = new SqlFormat(getUserInfo(), "personid = :1");
		    if ((personid instanceof String))
		    	personid = ((String)personid).trim();
		    sqf.setObject(1, "PERSON", "PERSONID", (String)personid);
		    personSet.setWhere(sqf.format());
		    return personSet;
		  }

  public void syncGroup(LdapSyncEvent event)
    throws LdapSyncException
  {
    MboRemote groupMbo = null;
    this.currentObjname = null;
    this.currentColname = null;
    try
    {
      Connection con = getConnection();

      DataMap groupDataMap = event.getGroupDataMap();
      SyncData syncData = event.getGroupSyncData();

      if (getLogger().isDebugEnabled()) {
        getLogger().debug("syncGroup, syncData = " + syncData.toString());
        // mckenzie added - start
        getLogger().debug("syncGroup, groupDataMap = " + groupDataMap.toString());
        // mckenzie added - end
      }
      Iterator tableIdIterator = groupDataMap.getTableIds();
      while (tableIdIterator.hasNext())
      {
        String tableId = (String)tableIdIterator.next();
        String objectName = groupDataMap.getObjectName(tableId, con);

        // mckenzie added - start
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("syncGroup, tableId = " + tableId);
        //  getLogger().debug("syncGroup, objectName = " + objectName);
        //}
        // mckenzie added - end

        if (objectName.equalsIgnoreCase("MAXGROUP"))
        {
          Object groupname = getDataMapValue(con, tableId, "GROUPNAME", syncData, groupDataMap);
          MboSetRemote groupSet = MXServer.getMXServer().getMboSet("MAXGROUP", getUserInfo());
          SqlFormat sqf = new SqlFormat(getUserInfo(), "groupname = :1");
          sqf.setObject(1, "MAXGROUP", "GROUPNAME", (String)groupname);
          groupSet.setWhere(sqf.format());
          groupMbo = groupSet.getMbo(0);

          // mckenzie added - start
          //if (getLogger().isDebugEnabled()) {
          //  getLogger().debug("syncGroup, sqf = " + sqf.format());
          //}
          // mckenzie added - end

          if (groupMbo == null) {
            // mckenzie -- changed start
            // commented out to avoid inserting groups not already in Maximo
            // groupMbo = insertRecord(con, tableId, groupSet, syncData, groupDataMap);
            // mckenzie -- changed end
          }
          else {
            updateRecord(con, tableId, groupMbo, syncData, groupDataMap);
          }
        }
        else
        {
          MboSetRemote otherSet = groupMbo.getMboSet(objectName);
          MboRemote otherMbo = otherSet.getMbo(0);

          if (otherMbo == null)
            otherMbo = insertRecord(con, tableId, otherSet, syncData, groupDataMap);
          else {
            updateRecord(con, tableId, otherMbo, syncData, groupDataMap);
          }
        }
      }
      // mckenzie -- changed start
      // do not save groupMbo if not inserted and still null
      if(groupMbo != null) {
        groupMbo.getThisMboSet().save();
      }
      // mckenzie -- changed end
    }
    catch (MXException e2)
    {
      String msg_groupname = "";
      try
      {
        if (groupMbo != null)
          msg_groupname = " groupname = " + groupMbo.getString("groupname");
      }
      catch (Exception localException) {
      }
      String temp = "";
      if (this.currentObjname != null)
      {
        temp = temp + " " + this.currentObjname;
        if (this.currentColname != null)
          temp = temp + "." + this.currentColname;
      }
      this.currentObjname = null;
      this.currentColname = null;

      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter02").getMessage() + msg_groupname + " " + e2.getMessage() + temp, e2);
    }
    catch (RemoteException e3)
    {
      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter02").getMessage() + " " + e3.getMessage(), e3);
    }
    this.currentObjname = null;
    this.currentColname = null;
  }

  public void syncGroupMembers(LdapSyncEvent event)
    throws LdapSyncException
  {
    try
    {
      Connection con = getConnection();

      MemberDataMap memberDataMap = event.getGroupMemberDataMap();
      SyncData syncData = event.getGroupSyncData();
      Set groupMembers = event.getGroupMembers();
      //System.out.println("******** Group Members************"+groupMembers);
      // mckenzie added - start
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("syncGroupMembers, memberDataMap = " + memberDataMap.toString());
        getLogger().debug("syncGroupMembers, syncData = " + syncData.toString());
        getLogger().debug("syncGroupMembers, groupMembers = " + groupMembers.toString());
      }
      // mckenzie added - end

      // mckenzie added - start
      // get the group name and check if the group already exists in maximo;
      //   if the group does not already exist, do not delete/insert group members
      Object groupName = getMemberDataMapKeyColumnValue("groupname", syncData, memberDataMap);
      if(!groupExists((String)groupName)) {
        return;
      }
      // mckenzie added - end

      deleteMemberRecords(con, syncData, memberDataMap);
      addMemberRecords(con, syncData, memberDataMap, groupMembers);
    }
    catch (SQLException e)
    {
      throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter03").getMessage() + " " + e.getMessage(), e);
    }
  }

  protected void deleteMemberRecords(Connection con, SyncData syncData, MemberDataMap memberDataMap)
    throws SQLException
  {
    StringBuffer deleteStmt = new StringBuffer();

    String tableName = memberDataMap.getTableName();
    deleteStmt.append("delete from ");
    deleteStmt.append(tableName);
    deleteStmt.append(" where ");

    Iterator keyIterator = memberDataMap.getKeyColumnNames();
    while (keyIterator.hasNext())
    {
      String keyColumn = (String)keyIterator.next();

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("deleteMemberRecords, keyColumn = " + keyColumn);
      //}
      // mckenzie added - end

      deleteStmt.append(keyColumn);
      deleteStmt.append("=?");
      if (!keyIterator.hasNext())
        continue;
      deleteStmt.append(" and ");
    }

    PreparedStatement stmt = null;

    String query = deleteStmt.toString();

    // mckenzie added - start
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("deleteMemberRecords, query = " + query);
    //}
    // mckenzie added - end

    stmt = getCachedPreparedStatement(query);
    if (stmt == null)
    {
      stmt = con.prepareStatement(query);
      cachePreparedStatement(query, stmt);
    }

    if (this.sqlLogger.isInfoEnabled())
    {
      this.sqlLogger.info(query);
    }

    stmt.clearParameters();

    int bindIndex = 1;
    keyIterator = memberDataMap.getKeyColumnNames();
    while (keyIterator.hasNext())
    {
      String keyColumn = (String)keyIterator.next();

      Object value = getMemberDataMapKeyColumnValue(keyColumn, syncData, memberDataMap);
      logBindValue(keyColumn, value);
      stmt.setObject(bindIndex, value);
      bindIndex++;
    }

    int updateRowCount = stmt.executeUpdate();
  }

  protected void addMemberRecords(Connection con, SyncData syncData, MemberDataMap memberDataMap, Set members)
    throws LdapSyncException, SQLException
  {
    StringBuffer insertStmt = new StringBuffer();

    String tableName = memberDataMap.getTableName();
    insertStmt.append("insert into ");
    insertStmt.append(tableName);
    insertStmt.append(" (");

    String memberColumn = memberDataMap.getMemberColumn();
    insertStmt.append(memberColumn);
    insertStmt.append(",");

    StringBuffer valuesStmt = new StringBuffer();
    valuesStmt.append(" values (");
    valuesStmt.append("?");
    valuesStmt.append(",");

    Iterator colIterator = memberDataMap.getColumnNames();
    while (colIterator.hasNext())
    {
      String column = (String)colIterator.next();

      insertStmt.append(column);
      valuesStmt.append("?");
      if (!colIterator.hasNext())
        continue;
      insertStmt.append(",");
      valuesStmt.append(",");
    }

    insertStmt.append(" ) ");
    insertStmt.append(valuesStmt.toString());
    insertStmt.append(") ");

    PreparedStatement stmt = null;

    String query = insertStmt.toString();

    stmt = getCachedPreparedStatement(query);
    if (stmt == null)
    {
      stmt = con.prepareStatement(query);
      cachePreparedStatement(query, stmt);
    }

    if (this.sqlLogger.isInfoEnabled())
    {
      this.sqlLogger.info(query);
    }

    stmt.clearParameters();
    HashSet typeAdjustedUserIDs = new HashSet();

    Iterator memberIterator = members.iterator();

    while (memberIterator.hasNext())
    {
      String memberAccountName = (String)memberIterator.next();

      int bindIndex = 1;

      int memberColumnType = memberDataMap.getMemberColumnTypeAsInt();

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("addMemberRecords, memberAccountName = " + memberAccountName);
      //  getLogger().debug("addMemberRecords, memberColumnType = " + memberColumnType);
      //}
      // mckenzie added - end

      Object memberAccName = convertToTypeSpecificValue(memberColumnType, memberAccountName);

      if (typeAdjustedUserIDs.contains(memberAccName)) {
        continue;
      }
      // mckenzie added -- start
      // if there is no active person record for group member,
      // then do not add to the list
      if(!hasActivePersonRecord(memberAccountName))
      {
        continue;
      }
      // mckenzie added -- end
      typeAdjustedUserIDs.add(memberAccName);

      logBindValue(memberColumn, memberAccName);
      stmt.setObject(bindIndex, memberAccName);
      bindIndex++;

      colIterator = memberDataMap.getColumnNames();
      while (colIterator.hasNext())
      {
        String column = (String)colIterator.next();
        // mckenzie added - start
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("addMemberRecords, column = " + column);
        //}
        // mckenzie added - end
        Object value = getMemberDataMapColumnValue(con, column, syncData, memberDataMap);
        logBindValue(column, value);
        stmt.setObject(bindIndex, value);
        bindIndex++;
      }

      int i = stmt.executeUpdate();
    }
    // mckenzie added - start
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("addMemberRecords, query = " + query);
    //  getLogger().debug("addMemberRecords, typeAdjustedUserIDs = " + typeAdjustedUserIDs.toString());
    //}
    // mckenzie added - end
  }

  protected Object getMemberDataMapKeyColumnValue(String keyColumn, SyncData syncData, MemberDataMap memberDataMap)
  {
    String attribute = memberDataMap.getKeyAttribute(keyColumn);
    String value = syncData.get(attribute);
    int type = memberDataMap.getTypeAsInt(keyColumn);
    // mckenzie added - start
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("getMemberDataMapKeyColumnValue, attribute = " + attribute);
    //  getLogger().debug("getMemberDataMapKeyColumnValue, value = " + value);
    //  getLogger().debug("getMemberDataMapKeyColumnValue, type = " + type);
    //}
    // mckenzie added - end
    return convertToTypeSpecificValue(type, value);
  }

  protected Object getMemberDataMapColumnValue(Connection con, String column, SyncData syncData, MemberDataMap memberDataMap)
    throws LdapSyncException
  {
    String attribute = memberDataMap.getAttribute(column);

    int type = memberDataMap.getTypeAsInt(column);

    // mckenzie added - start
    boolean isUniqueIdColumn = memberDataMap.isUniqueIdColumn(column);
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("getMemberDataMapColumnValue, attribute = " + attribute);
    //  getLogger().debug("getMemberDataMapColumnValue, type = " + type);
    //  getLogger().debug("getMemberDataMapColumnValue, isUniqueIdColumn = " + isUniqueIdColumn);
    //}

    if (isUniqueIdColumn)
    // mckenzie added - end
    {
      String uniqueId = "";
      try
      {
        String tableName = memberDataMap.getTableName();
        UniqueIdProvider idProvider = new UniqueIdProvider();
        uniqueId = idProvider.getUniqueId(con, tableName, column);
      }
      catch (MXException e)
      {
        throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter04").getMessage() + " " + e.getMessage(), e);
      }

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("getMemberDataMapColumnValue, uniqueId = " + uniqueId);
      //}
      // mckenzie added - end

      return convertToTypeSpecificValue(type, uniqueId);
    }
    if (memberDataMap.isSysDateColumn(column))
    {
      return new Date(System.currentTimeMillis());
    }

    if ((attribute.startsWith("{")) && (attribute.endsWith("}")))
    {
      String substitutionValue = attribute.substring(1, attribute.length() - 1);

      substitutionValue = convertToExternalValue(con, memberDataMap.getTableName(), column, substitutionValue);

      return convertToTypeSpecificValue(type, substitutionValue);
    }

    String value = syncData.get(attribute);
    return convertToTypeSpecificValue(type, value);
  }

  private String convertToExternalValue(Connection con, String tableName, String column, String value)
  {
    Statement s = null;
    try
    {
      MboValueInfo mvi = MXServer.getMXServer().getMaximoDD().getMboSetInfo(tableName).getMboValueInfo(column);
      int type = mvi.getTypeAsInt();

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("convertToExternalValue, type = " + type);
      //  getLogger().debug("convertToExternalValue, value = " + value);
      //}
      // mckenzie added - end

      if ((type == 1) || (type == 0) || (type == 2))
      {
        String domainid = mvi.getDomainId();

        // mckenzie added - start
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("convertToExternalValue, domainid = " + domainid);
        //}
        // mckenzie added - end

        if ((domainid != null) && (!domainid.equals("")))
        {
          SqlFormat sqf = new SqlFormat("select value from synonymdomain where defaults = 1 and (maxvalue = :1 or value = :2) and domainid = :3");
          sqf.setObject(1, "SYNONYMDOMAIN", "MAXVALUE", value);
          sqf.setObject(2, "SYNONYMDOMAIN", "VALUE", value);
          sqf.setObject(3, "SYNONYMDOMAIN", "DOMAINID", domainid);

          s = con.createStatement();
          ResultSet rs = s.executeQuery(sqf.format());
          if (rs.next())
            value = rs.getString(1);
          rs.close();

          // mckenzie added - start
          //if (getLogger().isDebugEnabled()) {
          //  getLogger().debug("convertToExternalValue, sqf = " + sqf.format());
          //  getLogger().debug("convertToExternalValue, value = " + value);
          //}
          // mckenzie added - end
        }
      }

    }
    catch (RemoteException e1)
    {
      getLogger().error(e1);

      if (s != null)
        try
        {
          s.close();
        }
        catch (Exception e)
        {
          getLogger().error(e);
        }
    }
    catch (SQLException e2)
    {
      getLogger().error(e2);

      if (s != null)
        try
        {
          s.close();
        }
        catch (Exception e)
        {
          getLogger().error(e);
        }
    }
    catch (MXException e3)
    {
      getLogger().error(e3);

      if (s != null)
        try
        {
          s.close();
        }
        catch (Exception e)
        {
          getLogger().error(e);
        }
    }
    finally
    {
      if (s != null)
        try
        {
          s.close();
        }
        catch (Exception e)
        {
          getLogger().error(e);
        }
    }
    return value;
  }

  public void syncEnded(LdapSyncEvent event)
    throws LdapSyncException
  {
    if (this.cachedPreparedStatements != null)
    {
      closeCachedPreparedStatements();
    }

    this.cachedPreparedStatements = new HashMap();
  }

  protected void cachePreparedStatement(String query, PreparedStatement stmt)
  {
    this.cachedPreparedStatements.put(query, stmt);
  }

  protected PreparedStatement getCachedPreparedStatement(String query)
  {
    return (PreparedStatement)this.cachedPreparedStatements.get(query);
  }

  protected void closeCachedPreparedStatements()
  {
    Iterator iterator = this.cachedPreparedStatements.values().iterator();
    while (iterator.hasNext())
    {
      PreparedStatement stmt = (PreparedStatement)iterator.next();
      try {
        if (stmt == null) continue; stmt.close();
      }
      catch (Exception localException) {
      }
    }
    this.cachedPreparedStatements.clear();
  }

  protected Object getDataMapValue(Connection con, String tableId, String columnName, SyncData syncData, DataMap dataMap)
    throws LdapSyncException
  {
    String attribute = dataMap.getAttribute(tableId, columnName);
    int type = dataMap.getTypeAsInt(tableId, columnName);

    // mckenzie added - start
    boolean isUniqueIdColumn = dataMap.isUniqueIdColumn(tableId, columnName);
    String typeString = dataMap.getType(tableId, columnName);
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("getDataMapValue, attribute = " + attribute);
    //  getLogger().debug("getDataMapValue, type = " + type);
    //  getLogger().debug("getDataMapValue, typeString = " + typeString);
    //  getLogger().debug("getDataMapValue, isUniqueIdColumn " + isUniqueIdColumn);
    //}
    if (isUniqueIdColumn)
    // mckenzie added - end
    {
      String uniqueId = "";
      try
      {
        String tableName = dataMap.getTableName(tableId);
        UniqueIdProvider idProvider = new UniqueIdProvider();
        uniqueId = idProvider.getUniqueId(con, tableName, columnName);
        // mckenzie added - start
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("getDataMapValue, tableName = " + tableName);
        //  getLogger().debug("getDataMapValue, uniqueId = " + uniqueId);
        //}
        // mckenzie added - end
      }
      catch (MXException e)
      {
        throw new LdapSyncException(new MXSystemException("system", "defaultldapsyncadapter04").getMessage() + " " + e.getMessage(), e);
      }

      return convertToTypeSpecificValue(type, uniqueId);
    }
    // mckenzie added - start
    boolean isSysDateColumn = dataMap.isSysDateColumn(tableId, columnName);
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("getDataMapValue, isSysDateColumn = " + isSysDateColumn);
    //}
    if (isSysDateColumn)
    // mckenzie added - end
    {
      return new Date(System.currentTimeMillis());
    }

    if ((attribute.startsWith("{")) && (attribute.endsWith("}")))
    {
      String substitutionValue = attribute.substring(1, attribute.length() - 1);

      substitutionValue = convertToExternalValue(con, dataMap.getTableName(tableId), columnName, substitutionValue);

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("getDataMapValue, substitutionValue = " + substitutionValue);
      //}
      // mckenzie added - end

      return convertToTypeSpecificValue(type, substitutionValue);
    }

    String value = syncData.get(attribute);
    // mckenzie added - start
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("getDataMapValue, value = " + value);
    //}
    // mckenzie added - end
    return convertToTypeSpecificValue(type, value);
  }

  private Object convertToTypeSpecificValue(int type, String value)
  {
    Object convertedValue = value;

    if (value == null)
      return convertedValue;
    switch (type)
    {
    case 1:
    case 6:
      convertedValue = value;
      break;
    case 2:
      convertedValue = value.toUpperCase();
      break;
    case 3:
      convertedValue = value.toLowerCase();
      break;
    case 4:
      convertedValue = new Long(value);
      break;
    case 5:
      convertedValue = new Integer(value);
    }

    return convertedValue;
  }

  protected MboRemote insertRecord(Connection con, String tableId, MboSetRemote mboSet, SyncData syncData, DataMap dataMap)
    throws LdapSyncException, MXException, RemoteException
  {
    String objectname = mboSet.getName();
    this.currentObjname = objectname;
    MboRemote mbo = mboSet.add(2L);

    Iterator colIterator = dataMap.getColumnNames(tableId);
    while (colIterator.hasNext())
    {
      String columnName = (String)colIterator.next();

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("insertRecord, columnName = " + columnName);
      //}
      // mckenzie added - end

      this.currentColname = columnName;
      Object value = getDataMapValue(con, tableId, columnName, syncData, dataMap);
      boolean mviRequired = MXServer.getMXServer().getMaximoDD().getMboSetInfo(objectname).getMboValueInfo(columnName).isRequired();
      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("insertRecord, mviRequired = " + mviRequired);
      //}
      // mckenzie added - end

      if (((value instanceof String)) && (value != null) && (((String)value).trim().length() == 0)) {
        value = null;
      }
      // mckenzie added - start
      boolean isUniqueIdColumn = dataMap.isUniqueIdColumn(tableId, columnName);
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("insertRecord, isUniqueIdColumn = " + isUniqueIdColumn);
      //}
      if (isUniqueIdColumn) {
      // mckenzie added - end
        continue;
      }
      if (value == null)
      {
        // mckenzie added - start
        boolean isRequired = dataMap.isRequired(tableId, columnName);
        boolean isMboNull = mbo.isNull(columnName);
        //if (getLogger().isDebugEnabled()) {
        //  getLogger().debug("insertRecord, isRequired = " + isRequired);
        //  getLogger().debug("insertRecord, isMboNull = " + isMboNull);
        //}
        if ((mviRequired) || (isRequired))
        {
          if (!isMboNull)
        // mckenzie added - end
            continue;
          mbo.delete(2L);
          this.currentObjname = null;
          this.currentColname = null;
          return mbo;
        }

        mbo.setValueNull(columnName, 38L);
      } else {
        if (columnName.equalsIgnoreCase("password"))
          continue;
        if (dataMap.getTypeAsInt(tableId, columnName) == 6)
          mbo.setValue(columnName, (String)value, 38L);
        else if (dataMap.getTypeAsInt(tableId, columnName) == 5)
        {
          if ((value instanceof Integer))
          {
            if (((Integer)value).intValue() == 0)
              mbo.setValue(columnName, false, 38L);
            else if (((Integer)value).intValue() == 1)
              mbo.setValue(columnName, true, 38L);
          } else {
            if (!(value instanceof String))
              continue;
            if (((String)value).equalsIgnoreCase("false"))
              mbo.setValue(columnName, false, 38L);
            else if (((String)value).equalsIgnoreCase("true"))
              mbo.setValue(columnName, true, 38L);
          }
        }
        else if ((value instanceof String))
          mbo.setValue(columnName, (String)value, 38L);
        else if ((value instanceof Integer))
          mbo.setValue(columnName, ((Integer)value).intValue(), 38L);
        else if ((value instanceof Long))
          mbo.setValue(columnName, ((Long)value).longValue(), 38L);
        else if ((value instanceof Date)) {
          mbo.setValue(columnName, (Date)value, 38L);
        }
      }
    }

    this.currentObjname = null;
    this.currentColname = null;

    return mbo;
  }

  protected void updateRecord(Connection con, String tableId, MboRemote mbo, SyncData syncData, DataMap dataMap)
    throws LdapSyncException, MXException, RemoteException
  {
    String objectname = mbo.getThisMboSet().getName();

    // mckenzie added - start
    //if (getLogger().isDebugEnabled()) {
    //  getLogger().debug("updateRecord, objectname = " + objectname);
    //}
    // mckenzie added - end

    this.currentObjname = objectname;
    Iterator colIterator = dataMap.getColumnNames(tableId);
    while (colIterator.hasNext())
    {
      String columnName = (String)colIterator.next();
      this.currentColname = columnName;

      // mckenzie added - start
      boolean isUniqueIdColumn = dataMap.isUniqueIdColumn(tableId, columnName);
      boolean isMappedToLdapAttribute = dataMap.isMappedToLdapAttribute(tableId, columnName);
      boolean isRequired = dataMap.isRequired(tableId, columnName);
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("updateRecord, columnName = " + columnName);
      //  getLogger().debug("updateRecord, isUniqueIdColumn = " + isUniqueIdColumn);
      //  getLogger().debug("updateRecord, isMappedToLdapAttribute = " + isMappedToLdapAttribute);
      //  getLogger().debug("updateRecord, isRequired = " + isRequired);
      //}

      if ((isUniqueIdColumn) || (
        (!isMappedToLdapAttribute) && (!isRequired)))
      // mckenzie added - end
      {
        continue;
      }
      Object value = getDataMapValue(con, tableId, columnName, syncData, dataMap);
      boolean mviRequired = MXServer.getMXServer().getMaximoDD().getMboSetInfo(objectname).getMboValueInfo(columnName).isRequired();
      // mckenzie added - start
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("updateRecord, mviRequired = " + mviRequired);
      }
      // mckenzie added - end
      if (((value instanceof String)) && (value != null) && (((String)value).trim().length() == 0)) {
        value = null;
      }
      if (value == null)
      {
        // mckenzie added - start
        if ((mviRequired) || (isRequired)) {
        // mckenzie added - end
          continue;
        }
        mbo.setValueNull(columnName, 38L);
      } else {
        if (columnName.equalsIgnoreCase("password"))
          continue;
        if (dataMap.getTypeAsInt(tableId, columnName) == 6)
          mbo.setValue(columnName, (String)value, 38L);
        else if (dataMap.getTypeAsInt(tableId, columnName) == 5)
        {
          if ((value instanceof Integer))
          {
            if (((Integer)value).intValue() == 0)
              mbo.setValue(columnName, false, 38L);
            else if (((Integer)value).intValue() == 1)
              mbo.setValue(columnName, true, 38L);
          } else {
            if (!(value instanceof String))
              continue;
            if (((String)value).equalsIgnoreCase("false"))
              mbo.setValue(columnName, false, 38L);
            else if (((String)value).equalsIgnoreCase("true"))
              mbo.setValue(columnName, true, 38L);
          }
        }
        else if ((value instanceof String))
          mbo.setValue(columnName, (String)value, 38L);
        else if ((value instanceof Integer))
          mbo.setValue(columnName, ((Integer)value).intValue(), 38L);
        else if ((value instanceof Long))
          mbo.setValue(columnName, ((Long)value).longValue(), 38L);
        else if ((value instanceof Date)) {
          mbo.setValue(columnName, (Date)value, 38L);
        }
      }
    }

    this.currentObjname = null;
    this.currentColname = null;
  }

  protected void deleteRecord(MboRemote mbo)
    throws MXException, RemoteException
  {
    mbo.delete(2L);
  }

  protected void logBindValue(String bindColumnName, Object bindValue)
  {
    if (getSqlLogger().isInfoEnabled())
    {
      String bindVal = "";
      if (bindValue == null)
      {
        bindVal = "null";
      }
      else
      {
        bindVal = bindValue.toString();
      }

      Object[] params_msg = { bindColumnName, bindVal };
      getSqlLogger().info(new MXSystemException("system", "defaultldapsyncadapter05", params_msg).getMessage());

      // mckenzie added - start
      //if (getLogger().isDebugEnabled()) {
      //  getLogger().debug("logBindValue, bindColumnName = " + bindColumnName);
      //  getLogger().debug("logBindValue, bindVal = " + bindVal);
      //}
      // mckenzie added - end
    }
  }
}