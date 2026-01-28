/**
 * 
 */
package x1.psdi.webclient.beans.common;

import java.rmi.RemoteException;
import psdi.app.financial.virtual.GLNavTempOrgSetRemote;
import psdi.app.system.SystemServiceRemote;
import psdi.mbo.Mbo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.security.ProfileRemote;
import psdi.util.MXException;
import psdi.util.MXSession;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.controller.AppInstance;
import psdi.webclient.system.controller.ComponentInstance;
import psdi.webclient.system.controller.ControlInstance;
import psdi.webclient.system.controller.SessionContext;
import psdi.webclient.system.controller.WebClientEvent;
import psdi.webclient.system.session.WebClientSession;

/**
 * @author s279004
 *
 */
public class X1GLNavigatorSelectOrgBean extends DataBean {
	private X1GLNavigatorDialogBean dialogBean = null;
	  private ControlInstance sourceControl = null;
	  private DataBean sourceDataBean = null;
	  private MXLogger log=null;
	  
	  protected void initialize()
	    throws MXException, RemoteException
	  {
	    super.initialize();
	    log = MXLoggerFactory.getLogger("maximo.Customization.GLNavigator");
	    this.sourceControl = this.app.getWebClientSession().getCurrentEvent().getSourceControlInstance();
	    if (this.sourceControl != null)
	    {
	      this.sourceDataBean = this.app.getWebClientSession().getDataBean(this.sourceControl.getProperty("datasrc"));
	      if (this.sourceDataBean != null) {
	        fetchOrgAndSiteIdFromSource();
	      }
	    }
	  }
	  
	  public void setupBean(WebClientSession sc)
	  {
	    super.setupBean(sc);
	    
	    this.dialogBean = ((X1GLNavigatorDialogBean)this.app.getDataBean(this.app.getCurrentPageId()));
	  }
	  
	  protected MboSetRemote getMboSetRemote()
	    throws MXException, RemoteException
	  {
	    this.sourceControl = this.app.getWebClientSession().getCurrentEvent().getSourceControlInstance();
	    
	    GLNavTempOrgSetRemote glNav = (GLNavTempOrgSetRemote)super.getMboSetRemote();
	    
	    this.sourceDataBean = this.app.getWebClientSession().getDataBean(this.sourceControl.getProperty("datasrc"));
	    if (this.sourceDataBean.getMboSet().getName().equalsIgnoreCase("CHARTOFACCOUNTS")) {
	      glNav.setForCOA(true);
	    }
	    return glNav;
	  }
	  
	  public void fetchOrgAndSiteIdFromSource()
	  {
	    String orgID = "";
	    String siteID = "";
	    try
	    {
	      DataBean sourceDataBeanOld = this.sourceDataBean;
	      if (this.sourceDataBean.getMbo() == null)
	      {
	        if (this.sourceDataBean.getMboOrZombie() == null) {
	          this.sourceDataBean = this.sourceDataBean.getParent();
	        }
	        if ((!this.sourceDataBean.getMboOrZombie().isZombie()) && (this.sourceDataBean.getMbo() == null))
	        {
	          this.sourceDataBean = sourceDataBeanOld;
	          return;
	        }
	        MboRemote zombie = this.sourceDataBean.getMboOrZombie();
	        siteID = ((Mbo)zombie).getProfile().getDefaultSite();
	        orgID = ((Mbo)zombie).getProfile().getDefaultOrg();
	        setValue("orgid", orgID);
	        setValue("siteid", siteID);
	        getMbo().setFieldFlag("orgid", 7L, true);
	        getMbo().setFieldFlag("siteid", 7L, true);
	        this.sourceDataBean = sourceDataBeanOld;
	        return;
	      }
	      String attributeName = this.creatingEvent.getSourceComponentInstance().getProperty("dataattribute");
	      
	      SystemServiceRemote sys = (SystemServiceRemote)this.sessionContext.getMXSession().lookup("SYSTEM");
	      
	      Object[] map = null;
	      map = sys.getLookupKeyMap(this.sourceDataBean.getMbo().getName(), attributeName.toUpperCase(), getMXSession().getUserInfo());
	      if (map == null) {
	        map = sys.getLookupKeyMap(this.sourceDataBean.getMbo().getName(), "GLACCOUNT", getMXSession().getUserInfo());
	      }
	      if ((map != null) && (map.length != 0))
	      {
	        for (int i = 0; i < map.length; i++)
	        {
	          Object[] arr = (Object[])map[i];
	          if (arr[1].toString().equalsIgnoreCase("orgid")) {
	            orgID = this.sourceDataBean.getMbo().getString(arr[0].toString());
	          }
	          if (arr[1].toString().equalsIgnoreCase("siteid")) {
	            siteID = this.sourceDataBean.getMbo().getString(arr[0].toString());
	          }
	        }
	      }
	      else
	      {
	        orgID = this.sourceDataBean.getMbo().getString("orgid");
	        try
	        {
	          siteID = this.sourceDataBean.getMbo().getString("siteid");
	        }
	        catch (Exception ex) {}
	      }
	      if (!orgID.equals(""))
	      {
	        setValue("orgid", orgID);
	        if (!siteID.equals("")) {
	          setValue("siteid", siteID);
	        }
	        getMbo().setFieldFlag("orgid", 7L, true);
	        getMbo().setFieldFlag("siteid", 7L, true);
	      }
	      this.sourceDataBean = sourceDataBeanOld;
	    }
	    catch (Throwable e) {}
	  }
	  
	  public String getOrgId()
	  {
	    return getString("orgid");
	  }
	  
	  private ControlInstance parentCtrl = null;
	  private X1GLNavigatorDialogBean parentBean = null;
	  
	  public synchronized void setValue(String attribute, MboRemote mboRemote)
	    throws MXException
	  {
	    try
	    {
	      if (attribute.equals("orgid"))
	      {
	        setValue(attribute, mboRemote.getString("orgid"));
	        
	        this.parentCtrl = ((ControlInstance)this.creator.getParentInstance());
	        if (this.parentCtrl != null)
	        {
	          this.parentBean = ((X1GLNavigatorDialogBean)this.app.getDataBean(this.parentCtrl.getProperty("datasrc")));
	          this.parentBean.structureChangedEvent(this.parentBean);
	        }
	      }
	      if (attribute.equals("siteid"))
	      {
	        setValue(attribute, mboRemote.getString("siteid"));
	        setValue("orgid", mboRemote);
	        return;
	      }
	    }
	    catch (RemoteException e) {}
	    X1GLNavigatorComboboxBean db = null;
	    String inputMode = this.parentCtrl.getProperty("inputmode").toLowerCase();
	    if (inputMode.equals("query")) {
	      db = (X1GLNavigatorComboboxBean)this.app.getDataBean("query_lookup_glnavigator_combobox_datasrc");
	    } else {
	      db = (X1GLNavigatorComboboxBean)this.app.getDataBean("lookup_glnavigator_combobox_datasrc");
	    }
	    if (db == null) {
	      return;
	    }
	    db.structureChangedEvent(db);
	  }
	  
	  public synchronized void setValue(int row, String attribute, MboRemote mboRemote)
	    throws MXException
	  {
		  log.info(" GLNavigator Select Org Bean >>>>>>>>> "+attribute);
	    super.setValue(row, attribute, mboRemote);
	    if ((attribute.equalsIgnoreCase("orgid")) || (attribute.equalsIgnoreCase("siteid"))) {
	      setValue(attribute, mboRemote);
	    }
	  }
}
