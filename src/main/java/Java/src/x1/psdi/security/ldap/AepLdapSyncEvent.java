package x1.psdi.security.ldap;

import psdi.security.ldap.DataMap;
import psdi.security.ldap.LdapSyncEvent;
import psdi.security.ldap.SyncData;

public class AepLdapSyncEvent extends LdapSyncEvent {

	private int eventId = 0;
	
	/**
	 * Generates the event type 800 to Sync users based on group membership
	 * @param eventId
	 * @param dataMap
	 * @param syncData
	 */
	public AepLdapSyncEvent(int eventId, DataMap dataMap, SyncData syncData) {
		super(eventId, dataMap, syncData);
		this.eventId=800;
		
	}
	
	public int getEventType()
	   {
	     return this.eventId;
	   }

}
