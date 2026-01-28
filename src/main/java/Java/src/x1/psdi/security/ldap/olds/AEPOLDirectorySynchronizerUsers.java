package x1.psdi.security.ldap.olds;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.naming.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import psdi.security.ldap.*;
import psdi.server.MXServer;
import psdi.util.MXSystemException;

import x1.psdi.security.ldap.AepLdapSyncEvent;

public class AEPOLDirectorySynchronizerUsers extends AbstractLdapSynchronizer
{
  public static final String MODIFYTIMESTAMP = "modifyTimeStamp";
  protected String currentModifyTimeStamp = null;

  // mckenzie -- added start
  // indicates that an orphaned user warning has occurred
  private static final String ORPHANED_USER_WARNING_MESSAGE = "orphaneduserwarningmessage";
  // mckenzie -- added end

  protected void initSync() throws LdapSyncException
  {
    super.initSync();
    this.currentModifyTimeStamp = getModifyTimeStamp("modifyTimeStamp");
    this.userSettings.addAttribute(getGroupMemberAsUserAttribute());

    this.groupSettings.addAttribute(getGroupMemberAsGroupAttribute());
    this.groupSettings.addAttribute(getGroupMembersAttribute());
    this.groupSettings.addAttribute(getGroupDNAttributeName());
  }

  protected void updateSyncParameters()
  {
    SyncParameters syncParams = getSyncParameters();
    syncParams.put("modifyTimeStamp", this.currentModifyTimeStamp);
  }

  protected String getUserSearchFilter()
  {
    String defaultFilter = super.getUserSearchFilter();

    String filter = defaultFilter;

    if (isFullSyncNeeded())
    {
      filter = "(&(" + defaultFilter + ")(modifyTimeStamp<=" + this.currentModifyTimeStamp + "))";
    }
    else
    {
      SyncParameters parameters = getSyncParameters();
      String lastModifyTimeStamp = parameters.get("modifyTimeStamp");

      filter = "(&(" + defaultFilter + ")" + "(modifyTimeStamp>=" + lastModifyTimeStamp + ")" + "(modifyTimeStamp<=" + this.currentModifyTimeStamp + "))";
    }

    return filter;
  }

  protected String getGroupSearchFilter() {
    String defaultFilter = super.getGroupSearchFilter();

    String filter = defaultFilter;
    if (isFullSyncNeeded())
    {
      filter = "(&(modifyTimeStamp<=" + this.currentModifyTimeStamp + ")" + defaultFilter + ")";
    }
    else
    {
      SyncParameters parameters = getSyncParameters();
      String lastModifyTimeStamp = parameters.get("modifyTimeStamp");

      filter = "(&(modifyTimeStamp>=" + lastModifyTimeStamp + ")" + "(modifyTimeStamp<=" + this.currentModifyTimeStamp + ")" + defaultFilter + ")";
    }

    return filter;
  }

  protected Set retrieveAllGroupMembers(String groupDN)
    throws LdapSyncException
  {
    Set memberUsers = new HashSet();
    Set memberGroups = new HashSet();

    Set processedMemberGroups = new HashSet();
    Set memberGroupsToProcess = new HashSet();

    String currentGroupDN = groupDN;
    do
    {
      retrieveGroupMembers(currentGroupDN, memberUsers, memberGroups);
      processedMemberGroups.add(currentGroupDN);
      memberGroupsToProcess.remove(currentGroupDN);

      Iterator memberGroupIterator = memberGroups.iterator();
      while (memberGroupIterator.hasNext())
      {
        String memberGroupDN = (String)memberGroupIterator.next();

        if (processedMemberGroups.contains(memberGroupDN))
          continue;
        memberGroupsToProcess.add(memberGroupDN);
      }

      if (memberGroupsToProcess.size() > 0)
      {
        Iterator iterator = memberGroupsToProcess.iterator();

        currentGroupDN = (String)iterator.next();
      }

      memberGroups = new HashSet();
    }
    while (
      memberGroupsToProcess.size() > 0);

    return memberUsers;
  }

  protected void retrieveGroupMembers(String groupDN, Set memberUsers, Set memberGroups)
    throws LdapSyncException
  {
    LdapContext ctx = null;
    try
    {
      ctx = createSearchLdapContext();

      String memberAttrName = getGroupMembersAttribute();

      String searchMemberAttrName = memberAttrName;

      boolean moreMembersToRetrieve = false;
      do
      {
        Name name = new CompositeName().add(groupDN);
        Attributes attrs = ctx.getAttributes(name, new String[] { searchMemberAttrName });

        NamingEnumeration enumit = attrs.getAll();
        while (enumit.hasMore())
        {
          Attribute attr = (Attribute)enumit.next();
          String attrName = attr.getID();
          if (!attrName.equalsIgnoreCase(memberAttrName))
            continue;
          try
          {
            Object obj = attr.get();
            if (attr.get() == null)
              continue;
            NamingEnumeration memberDNEnum = attr.getAll();
            while (memberDNEnum.hasMore())
            {
              String memberDN = (String)memberDNEnum.next();
              String memberUserAccountName = getMemberUserAccountName(memberDN);

              // mckenzie -- added start
              // if orphaned user record, then skip normal processing
              if (memberUserAccountName != ORPHANED_USER_WARNING_MESSAGE) {
              // mckenzie -- added end
                if (memberUserAccountName != null) {
                  memberUsers.add(memberUserAccountName);
                } else {
                  memberGroups.add(memberDN);
                }
              }
            }
          }
          catch (NoSuchElementException localNoSuchElementException)
          {
          }
        }

      }

      while (moreMembersToRetrieve);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();

      throw new LdapSyncException(new MXSystemException("system", "genericdirectorysynchronizer01").getMessage(), ex);
    }
    finally
    {
      closeContext(ctx);
    }
  }

  protected String getMemberUserAccountName(String memberDN)
    throws LdapSyncException
  {
    String memberUserAccountName = null;

    String[] attributes = { getGroupMemberAsUserAttribute(), getGroupMemberAsGroupAttribute(), "objectclass", getUserDNAttributeName(), getGroupDNAttributeName() };

    LdapContext memberLdapContext = null;
    try
    {
      memberLdapContext = createSearchLdapContext();

      String jndi_dn = new CompositeName().add(memberDN).toString();
      Attributes attrs = memberLdapContext.getAttributes(jndi_dn, attributes);

      Attribute objClassAttr = attrs.get("objectClass");

      String userObjectClass = getUserObjectClass();
      String groupObjectClass = getGroupObjectClass();

      NamingEnumeration enumit = objClassAttr.getAll();
      while (enumit.hasMore())
      {
        Object obj = enumit.next();
        String objClassName = obj.toString();
        if (objClassName.equalsIgnoreCase(userObjectClass))
        {
          Attribute userAccountAttr = attrs.get(getGroupMemberAsUserAttribute());
          memberUserAccountName = userAccountAttr.get().toString();
          break;
        }
        if (objClassName.equalsIgnoreCase(groupObjectClass))
        {
          break;
        }

      }

    }
    // mckenzie -- added start
    // log warning for orphan user error rather than raise exception
    catch (NameNotFoundException e1)
    {
      if(this.logger.isWarnEnabled()) {
        this.logger.warn(new MXSystemException("system", "genericdirectorysynchronizer02").getMessage(), e1);
      }
      // set special message to return value so caller knows the warning occurred
      memberUserAccountName = ORPHANED_USER_WARNING_MESSAGE;
    }
    // mckenzie -- added end
    catch (NamingException e)
    {
      throw new LdapSyncException(new MXSystemException("system", "genericdirectorysynchronizer02").getMessage(), e);
    }
    finally
    {
      closeContext(memberLdapContext);
    }

    return memberUserAccountName;
  }

  protected void determineSynchronizationNeed() throws LdapSyncException
  {
    try
    {
      SyncParameters parameters = getSyncParameters();
      if (parameters == null)
      {
        setSyncNeeded(true);
        setFullSyncNeeded(true);
      }
      else
      {
        String currentModifyTimeStamp = getModifyTimeStamp("modifyTimeStamp");

        String lastModifyTimeStamp = parameters.get("modifyTimeStamp");
        if (this.logger.isDebugEnabled())
        {
          this.logger.debug("LDAP Synchronizer determining the need for fullsync based on:");
          this.logger.debug("     previous sync lastModifyTimeStamp = " + lastModifyTimeStamp);

          this.logger.debug("     current currentModifyTimeStamp = " + currentModifyTimeStamp);
        }

        if (lastModifyTimeStamp == null)
        {
          setSyncNeeded(true);
          setFullSyncNeeded(true);
        }
        else if (lastModifyTimeStamp.equals(currentModifyTimeStamp))
        {
          setSyncNeeded(false);
          setFullSyncNeeded(false);
        }
        else
        {
          setSyncNeeded(true);
          setFullSyncNeeded(false);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    // mckenzie changed -- start
    // force sync for debugging -- REMOVE WHEN COMPLETE
    //setSyncNeeded(true);
    //setFullSyncNeeded(true);
    // mckenzie changed -- end
  }

  protected String getUserObjectClass()
  {
    String userObjClass = null;
    try
    {
      userObjClass = MXServer.getMXServer().getProperty("mxe.ldap.UserObjectClass");
    }
    catch (Throwable t)
    {
      userObjClass = "person";
    }
    if ((userObjClass == null) || (userObjClass.length() == 0))
      userObjClass = "person";
    return userObjClass;
  }

  protected String getGroupObjectClass()
  {
    String groupObjClass = null;
    try
    {
      groupObjClass = MXServer.getMXServer().getProperty("mxe.ldap.GroupObjectClass");
    }
    catch (Throwable t)
    {
      groupObjClass = "group";
    }
    if ((groupObjClass == null) || (groupObjClass.length() == 0))
      groupObjClass = "group";
    return groupObjClass;
  }

  protected String getUserDNAttributeName()
  {
    return "cn";
  }

  protected String getGroupDNAttributeName()
  {
    return "cn";
  }

  protected String getModifyTimeStamp(String attributeName)
    throws LdapSyncException
  {
    String atributeValue = null;

    String searchBase = this.groupSettings.getBaseDN();
    String searchFilter = "(objectclass=*)";
    SearchControls constraints = new SearchControls();
    constraints.setSearchScope(2);

    constraints.setReturningAttributes(new String[] { attributeName });

    LdapContext ctx = null;
    try
    {
      ctx = createDefaultLdapContext();
      long maxTimeStamp = 0L;
      NamingEnumeration results = ctx.search(searchBase, searchFilter, constraints);
      while ((results != null) && (results.hasMore()))
      {
        SearchResult sr = (SearchResult)results.next();
        Attributes attrs = sr.getAttributes();
        if (attrs.size() <= 0)
          continue;
        Attribute attr = attrs.get(attributeName);
        if (attr == null)
          continue;
        atributeValue = attr.get().toString();
        if ((atributeValue == null) || (atributeValue.length() <= 0) || (atributeValue.indexOf('Z') <= -1))
          continue;
        atributeValue = atributeValue.substring(0, atributeValue.indexOf('Z'));
        long timeStamp = new Long(atributeValue).longValue();
        if (timeStamp > maxTimeStamp) {
          maxTimeStamp = timeStamp;
        }

      }

      if (maxTimeStamp > 0L) {
        atributeValue = new Long(maxTimeStamp).toString() + "Z";
      }

    }
    catch (NamingException e)
    {
      Object[] params_msg = { attributeName };
      throw new LdapSyncException(new MXSystemException("system", "novelldirectorysynchronizer03", params_msg).getMessage(), e);
    }
    finally {
      closeContext(ctx);
    }

    return atributeValue;
  }

  protected void syncGroups()
    throws LdapSyncException
  {
    LdapContext ctx = null;
    try
    {
      ctx = createSearchLdapContext();

      if (this.logger.isDebugEnabled())
      {
        StringBuffer msg = new StringBuffer();
        msg.append("Synchronizing groups from base: {");
        msg.append(getGroupSearchBase());
        msg.append("}, using filter: {");
        msg.append(getGroupSearchFilter());
        msg.append("}");

        this.logger.debug(msg.toString());
      }

      NamingEnumeration results = null;

      if (this.logger.isDebugEnabled())
      {
        this.logger.debug("Synchronizing Groups:");
      }

      results = ctx.search(getGroupSearchBase(), getGroupSearchFilter(), getGroupSearchControls());

      while ((results != null) && (results.hasMore()) && (this.errorCount <= maxErrors))
      {
        try
        {
          SearchResult si = (SearchResult)results.next();

          if (this.logger.isDebugEnabled())
          {
            this.logger.debug(" group : " + si.getName());
          }

          Attributes attrs = si.getAttributes();

          if (attrs == null)
            continue;
          SyncData groupSyncData = new SyncData();
          int sz = attrs.size();
          NamingEnumeration ae = attrs.getAll();
          while (ae.hasMoreElements())
          {
            Attribute attr = (Attribute)ae.next();
            String attrId = attr.getID();
            

            if (attr.size() == 1)
            {
              Object obj = attr.get();
              if (!(obj instanceof String))
                continue;
              groupSyncData.put(attrId, (String)attr.get());
            }
            else
            {
              Object obj = attr.get(0);
              if (!(obj instanceof String))
                continue;
              groupSyncData.put(attrId, (String)attr.get(0));
            }

          }

          LdapSyncListener listener = getLDAPSyncListener();
          if (listener != null)
          {
            LdapSyncEvent event = new LdapSyncEvent(300, this.groupSettings.getDataMap(), groupSyncData);

            listener.syncGroup(event);
            this.noGroupsSynchronized += 1;
          }

          String groupDN = si.getName() + "," + getGroupSearchBase();

          Set groupMembers = retrieveAllGroupMembers(groupDN);
          syncGroupMembers(groupSyncData, groupMembers);
          syncMemberUsers(groupMembers);
          

          commitGroupChanges(this.noGroupsSynchronized);
        }
        catch (Exception e)
        {
          this.errorCount += 1;

          Object[] params_msg = { e.getMessage(), this.errorCount };
          this.logger.error(new MXSystemException("system", "abstractldapsynchronizer13", params_msg).getMessage(), e);
        }

      }

      commitGroupChanges();
    }
    catch (Exception e)
    {
      throw new LdapSyncException(new MXSystemException("system", "genericdirectorysynchronizer03").getMessage(), e);
    }
    finally
    {
      closeContext(ctx);
    }
  }
  
  
  // Creation of users based on group membership
  protected void syncMemberUsers(Set groupMembers) 
		  throws LdapSyncException
  {
	  
	  String filter = "(&(objectClass=inetOrgPerson)(|";
	  Iterator iter = groupMembers.iterator();
	  while (iter.hasNext())
	  {
	      
	      filter = filter + "(uid="+iter.next()+")";
	  }
      filter = filter+"))";
      LdapContext ctx = null;
	  
	  
	    try
	    {
	      ctx = createSearchLdapContext();

	      if (this.logger.isDebugEnabled())
	      {
	        StringBuffer msg = new StringBuffer();
	        msg.append("Synchronizing member users from base: {");
	        msg.append(getUserSearchBase());
	        msg.append("}, using filter: {");
	        msg.append(filter);
	        msg.append("}");

	        this.logger.debug(msg.toString());
	      }

	      NamingEnumeration results = null;

	      if (this.logger.isDebugEnabled())
	      {
	        this.logger.debug("Synchronizing Users:");
	      }

	      results = ctx.search(getUserSearchBase(), filter, getUserSearchControls());

	      while ((results != null) && (results.hasMore()) && (this.errorCount <= maxErrors))
	      {
	        try
	        {
	          SearchResult si = (SearchResult)results.next();

	          if (this.logger.isDebugEnabled())
	          {
	            this.logger.debug("  user: " + si.getName());
	          }

	          Attributes attrs = si.getAttributes();

	          if (attrs != null)
	          {
	            SyncData userSyncData = new SyncData();
	            NamingEnumeration ae = attrs.getAll();
	            while (ae.hasMoreElements())
	            {
	              Attribute attr = (Attribute)ae.next();
	              String attrId = attr.getID();

	              if (attr.size() == 1)
	              {
	                Object obj = attr.get();
	                if (!(obj instanceof String))
	                  continue;
	                userSyncData.put(attrId, (String)attr.get());
	              }
	              else
	              {
	                Object obj = attr.get(0);
	                if (!(obj instanceof String))
	                  continue;
	                userSyncData.put(attrId, (String)attr.get(0));
	              }
	              
	              if(attrId.equalsIgnoreCase("manager") )
	              {
	            	  String manager = attrs.get("manager").get().toString();
	                  manager = manager.substring(manager.indexOf('=')+1, manager.indexOf(','));
	            	  userSyncData.put(attrId, manager);
	              }
	              

	            }

	            LdapSyncListener listener = getLDAPSyncListener();
	            if (listener != null)
	            {
	              LdapSyncEvent event = new AepLdapSyncEvent(200, this.userSettings.getDataMap(), userSyncData);
	                           
                  listener.syncUser(event);
	              this.noUsersSynchronized += 1;
	            }

	          }

	          commitUserChanges(this.noUsersSynchronized);
	        }
	        catch (Exception e)
	        {
	          this.errorCount += 1;

	          Object[] params_msg = { e.getMessage(), this.errorCount };
	          this.logger.error(new MXSystemException("system", "abstractldapsynchronizer07", params_msg).getMessage(), e);
	        }

	      }

	      commitUserChanges();
	    }
	    catch (NamingException e)
	    {
	      throw new LdapSyncException(new MXSystemException("system", "genericdirectorysynchronizer04").getMessage(), e);
	    }
	    finally
	    {
	      closeContext(ctx);
	    }

	
  }

protected void syncUsers()
    throws LdapSyncException
  {
    LdapContext ctx = null;
    try
    {
      ctx = createSearchLdapContext();

      if (this.logger.isDebugEnabled())
      {
        StringBuffer msg = new StringBuffer();
        msg.append("Synchronizing users from base: {");
        msg.append(getUserSearchBase());
        msg.append("}, using filter: {");
        msg.append(getUserSearchFilter());
        msg.append("}");

        this.logger.debug(msg.toString());
      }

      NamingEnumeration results = null;

      if (this.logger.isDebugEnabled())
      {
        this.logger.debug("Synchronizing Users:");
      }

      results = ctx.search(getUserSearchBase(), getUserSearchFilter(), getUserSearchControls());

      while ((results != null) && (results.hasMore()) && (this.errorCount <= maxErrors))
      {
        try
        {
          SearchResult si = (SearchResult)results.next();

          if (this.logger.isDebugEnabled())
          {
            this.logger.debug("  user: " + si.getName());
          }

          Attributes attrs = si.getAttributes();

          if (attrs != null)
          {
            SyncData userSyncData = new SyncData();
            NamingEnumeration ae = attrs.getAll();
            while (ae.hasMoreElements())
            {
              Attribute attr = (Attribute)ae.next();
              String attrId = attr.getID();
                           
              if (attr.size() == 1)
              {
                Object obj = attr.get();
                if (!(obj instanceof String))
                  continue;
                userSyncData.put(attrId, (String)attr.get());
              }
              else
              {
                Object obj = attr.get(0);
                if (!(obj instanceof String))
                  
                	continue;
                userSyncData.put(attrId, (String)attr.get(0));
              }
              
              if(attrId.equalsIgnoreCase("manager") )
              {
            	  String manager = attrs.get("manager").get().toString();
                  manager = manager.substring(manager.indexOf('=')+1, manager.indexOf(','));
            	  userSyncData.put(attrId, manager);
              }
              
              //System.out.println("Checking User Sync Data"+userSyncData.get(attrId));
              
            }

            LdapSyncListener listener = getLDAPSyncListener();
            if (listener != null)
            {
              LdapSyncEvent event = new LdapSyncEvent(200, this.userSettings.getDataMap(), userSyncData);

              listener.syncUser(event);
              this.noUsersSynchronized += 1;
            }

          }

          commitUserChanges(this.noUsersSynchronized);
        }
        catch (Exception e)
        {
          this.errorCount += 1;

          Object[] params_msg = { e.getMessage(), this.errorCount };
          this.logger.error(new MXSystemException("system", "abstractldapsynchronizer07", params_msg).getMessage(), e);
        }

      }

      commitUserChanges();
      
    }
    catch (NamingException e)
    {
      throw new LdapSyncException(new MXSystemException("system", "genericdirectorysynchronizer04").getMessage(), e);
    }
    finally
    {
      closeContext(ctx);
      
    }
  }
}