/*
 *
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * 5724-U18
 *
 * (C) COPYRIGHT IBM CORP. 2006,2011
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *
 */

package x1.psdi.iface.app.common;

import java.rmi.RemoteException;
import psdi.iface.mic.MaximoEventFilter;
import psdi.iface.mic.MicUtil;
import psdi.iface.mic.PublishInfo;
import psdi.mbo.MboRemote;
import psdi.server.MXServer;
import psdi.util.MXException;

public class ConditionalEventFilter extends MaximoEventFilter
{
    public ConditionalEventFilter(PublishInfo pubInfo)throws MXException
    {
    	super(pubInfo);
    }
    
    /**
     * This method stops recurrence, i.e it checks if this is incoming 
     * transaction and if the MetaDataProperties.SOURCETYPE is specified stops the event
     * from going out.
     * This method can be overriden by individual event filter classes to allow
     * recurrence.
     * 
     * @return - true for stopping the event and false otherwise.
     * @throws MXException
     *             MAXIMO exception
     * @throws RemoteException
     *             Remote exception
     */
    protected boolean stopRecurrence(MboRemote mbo) throws MXException, RemoteException
    {
    	String conditionName = pubInfo.getPublishChannelName().toUpperCase();
    	int maxLength = MXServer.getMXServer().getMaximoDD().getMboSetInfo("CONDITION").getMboValueInfo("CONDITIONNUM").getLength();
    	if (conditionName.length() > maxLength)
    	{
    		conditionName = conditionName.substring(0, maxLength);
    	}
    	System.out.println("Condition name " + conditionName);
        boolean skipMbo = MicUtil.parseCondition(conditionName,mbo);
    	if (skipMbo)
    	{
            return false;
    	}
        return super.stopRecurrence(mbo);
    }


}

