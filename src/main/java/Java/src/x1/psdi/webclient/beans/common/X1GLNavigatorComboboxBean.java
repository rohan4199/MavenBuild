/**
 * 
 */
package x1.psdi.webclient.beans.common;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.controller.AppInstance;
import psdi.webclient.system.controller.BoundAttribute;
import psdi.webclient.system.controller.ControlInstance;

/**
 * @author s279004
 *
 */
public class X1GLNavigatorComboboxBean extends DataBean {

	private ControlInstance parentCtrl = null;
	  private X1GLNavigatorDialogBean parentBean = null;
	  private MXLogger log=null;
	  
	  public synchronized MboSetRemote getList(int row, String attribute)
	    throws MXException, RemoteException
	  {
		 log.info( row+" ROW <<<<< GLNavigator Combobox Bean >>>>>> attribute "+attribute); 
	    String orgId = null;
	    if (this.app.getDataBean("lookup_glnavigator_org_datasrc") != null) {
	      orgId = this.app.getDataBean("lookup_glnavigator_org_datasrc").getMbo(0).getString("orgid");
	    } else if (this.app.getDataBean("query_lookup_glnavigator_org_datasrc") != null) {
	      orgId = this.app.getDataBean("query_lookup_glnavigator_org_datasrc").getMbo(0).getString("orgid");
	    }
	    MboSetRemote glComponentsSet = super.getList(row, attribute);
	    glComponentsSet.setOrderBy("glorder asc");
	    glComponentsSet.setWhere("orgid = '" + orgId + "'");
	    glComponentsSet.reset();
	    log.info("GL ComponentsSet Count "+glComponentsSet.count());
	    return glComponentsSet;
	  }
	  
	  protected void initialize()
	    throws MXException, RemoteException
	  {
	    super.initialize();
	    log = MXLoggerFactory.getLogger("maximo.Customization.GLNavigator");
	    this.parentCtrl = ((ControlInstance)this.creator.getParentInstance());
	    if (this.parentCtrl != null)
	    {
	      this.parentBean = ((X1GLNavigatorDialogBean)this.app.getDataBean(this.parentCtrl.getProperty("datasrc")));
	      this.parentBean.comboboxBean = this;
	      this.parentBean.comboboxBean.setValue("SEGMENTNAME", "0");
	    }
	  }
	  
	  public int setvalue()
	    throws MXException, RemoteException
	  {
	    int returnValue = super.setvalue();
	    if (this.parentBean != null) {
	      returnValue = this.parentBean.setsegment();
	    }
	    return returnValue;
	  }
	  
	  public synchronized void structureChangedEvent(DataBean speaker)
	  {
	    super.structureChangedEvent(speaker);
	    try
	    {
	      if (this.parentBean != null)
	      {
	        this.parentBean.setCurrentSegment(0);
	        
	        Iterator attribsI = this.boundAttributes.values().iterator();
	        while (attribsI.hasNext())
	        {
	          BoundAttribute ba = (BoundAttribute)attribsI.next();
	          ba.setValue(null);
	        }
	      }
	    }
	    catch (Exception e)
	    {
	      e.printStackTrace();
	    }
	  }
}
