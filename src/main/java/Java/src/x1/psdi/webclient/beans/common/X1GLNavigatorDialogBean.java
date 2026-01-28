/**
 * 
 */
package x1.psdi.webclient.beans.common;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.ibm.tivoli.maximo.report.cognos.metadata.util.PrintWriter;

import java.io.StringWriter;
import java.lang.StringBuilder;

import psdi.app.financial.GLComponentsSetRemote;
import psdi.app.system.SystemServiceRemote;
import psdi.mbo.GLFormat;
import psdi.mbo.Mbo;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.mbo.MboValueData;
import psdi.security.ProfileRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.MXSession;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;
import x1.psdi.webclient.beans.common.X1GLNavigatorComboboxBean;
import x1.psdi.webclient.beans.common.X1GLNavigatorSelectOrgBean;
import psdi.webclient.system.beans.DataBean;
import psdi.webclient.system.beans.QbeBean;
import psdi.webclient.system.beans.ResultsBean;
import psdi.webclient.system.controller.AppInstance;
import psdi.webclient.system.controller.ControlInstance;
import psdi.webclient.system.controller.WebClientEvent;
import psdi.webclient.system.runtime.WebClientRuntime;
import psdi.webclient.system.session.WebClientSession;

/**
 * @author s279004 Updates by Aravindh Manickavasagam (s276983) 3/11/2018
 *         Updates by Aravindh Manickavasagam (s276983) 9/21/2018
 *         Updates by Aravindh Manickavasagam (s276983) 11/21/2018 ** ALM 4342 -- L work order validation
 */
public class X1GLNavigatorDialogBean extends DataBean {

	private String[] segmentValues = null;
	private int currentSegment = 0;

	private GLComponentsSetRemote msr = null;
	private GLFormat glFormat = null;
	private String originalGL = "";
	private String orgID = null;
	private String siteID = null;
	private String seg3Select = null;
	private int segmentCount = 0;
	private String appName = null;
	private UserInfo userInfo = null;
	private MXLogger log = null;
	private boolean fromCOA = false;
	private String origGL;
	private String prevWonum;

	ControlInstance sourceControl = null;
	DataBean sourceDataBean = null;

	public X1GLNavigatorComboboxBean comboboxBean = null;

	public String orgAttribute = "orgid";
	public boolean diffWonum = false;
	public boolean firstTime = true;
	public boolean changedWO = true; // Global variable to capture if complvalue on segment 3 has modified
										// (WORKORDER)
	public boolean updatedWO = false; // VK - global variable to capture if 4 segment WO (workorder) has been updated
	private String seg4;
	private String seg5;
	private String seg0;
	private String seg1;
	private String seg2;
	private String tseg0;
	private String tseg1;
	private String tseg2;
	private String tseg4;
	private String tseg5;
	private String tseg6;
	private String tseg7;
	private String tseg8;
	private String tseg9;
	private String tseg10;

	protected void initialize() throws MXException, RemoteException {

		super.initialize();
		this.orgID = null;
		log = MXLoggerFactory.getLogger("maximo.Customization.GLNavigator");
		if (this.sourceDataBean == null) {
			this.sourceControl = this.app.getWebClientSession().getCurrentEvent().getSourceControlInstance();
			if (this.sourceControl != null) {
				this.sourceDataBean = this.app.getWebClientSession()
						.getDataBean(this.sourceControl.getProperty("datasrc"));
				fetchOrgAndSiteIdFromSource();
			}

		}

		updatedWO = false; // VK - setting the variable to false in initialize
		log.debug("X1GLNavigator::updatedWO set = " + updatedWO);
		X1GLNavigatorSelectOrgBean db = null;
		String inputMode = this.sourceControl.getProperty("inputmode").toLowerCase();
		appName = app.getApp();
		log.debug("X1GLNavigator Application Name >>>>>>>>>>>>>" + appName);
		if (inputMode.equals("query")) {
			db = (X1GLNavigatorSelectOrgBean) this.app.getDataBean("query_lookup_glnavigator_org_datasrc");
		} else {
			db = (X1GLNavigatorSelectOrgBean) this.app.getDataBean("lookup_glnavigator_org_datasrc");
		}

		if ((this.orgID == null) || (this.orgID.equals(""))) {
			if (db != null) {
				this.orgID = db.getOrgId();
			}
		}

		if ((this.orgID == null) || (this.orgID.equals(""))) {
			clearData();
		} else {
			refreshGLData(db);
		}
		this.setOrigGL(this.originalGL);
		log.debug("This original GL >>>>" + this.originalGL);
		log.debug("Set Variable >>>>>" + origGL);

	}

	public int execute() throws MXException, RemoteException {

		log.debug(" execute method >>>>> ");
		String[] gl = null;
		String seg3 = null;
		if (this.sourceDataBean != null) {

			String glaccount = new GLFormat(this.segmentValues, this.orgID).toStorageString();

			log.debug(" X1GLNavigator DialogBean >>>>>>>>> " + glaccount);

			ArrayList segments = getSegmentValues(glaccount);

			if (segments.size() >= 4) {
				gl = getGLUpdate(glaccount);
				if (gl != null)
					glaccount = new GLFormat(gl, this.orgID).toStorageString();
			}
			if (segments.size() >= 3) {
				seg3 = (String) segments.get(2);
				log.debug("AM# seg3 is " + seg3);
			}
			if (seg3 != null && seg3 != "") {
				log.debug("Inside seg3 not null check " + seg3);
				if (this.appName != null && !(appName.equalsIgnoreCase("PLUSPR") || appName.equalsIgnoreCase("PLUSPO")
						|| appName.equalsIgnoreCase("PLUSDRECPT"))) {
					log.debug("Inside APP check");
					if (seg3.equalsIgnoreCase("1011001")) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				}
				else if (this.appName != null && (appName.equalsIgnoreCase("PLUSPR") || appName.equalsIgnoreCase("PLUSPO")
						|| appName.equalsIgnoreCase("PLUSDRECPT"))) 
				{
					log.debug("Inside PO/PR/RECEIPTS APP check");
					if (segments.size() == 3 && seg3.equalsIgnoreCase("1011001")) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				}				
			}

			if (this.sourceDataBean.getString(this.orgAttribute).equals("")) {
				this.sourceDataBean.setValue(this.orgAttribute, this.orgID);
			}

			if ((this.app.onListTab()) && (!(this.sourceDataBean instanceof QbeBean))) {

				if ((this.app.getResultsBean() != null) && (this.app.getBoundComponent() != null))

					this.app.getResultsBean().returnLookupValue(WebClientRuntime.unFormatString(glaccount));

				else {
					this.sourceDataBean.returnLookupValue(WebClientRuntime.unFormatString(glaccount));
				}

			} else {

				this.sourceDataBean.returnLookupValue(WebClientRuntime.unFormatString(glaccount));
			}
		}
		return 1;
	}

	public int cancelDialog() throws MXException, RemoteException {

		return 1;
	}

	public int setsegment() throws MXException, RemoteException {

		int returnValue = 1;
		String seg = this.app.getWebClientSession().getCurrentEvent().getValueString();
		if (!WebClientRuntime.isNull(seg)) {
			if (Character.isDigit(seg.charAt(0))) {
				this.currentSegment = Integer.parseInt(seg);
			}

			if (this.currentSegment >= this.segmentCount) {
				if (this.currentSegment == this.segmentCount) {
					this.currentSegment = (this.segmentCount - 1);
				}

				return returnValue;
			}

			this.comboboxBean.setValue("SEGMENTNAME", seg);
			/*
			 * Custom code added to exclude certain workorders which exist in the X1WOFIN
			 * table WHERE X1SYSSOURCE not in ( 'MAX', 'WMS', 'CKP')
			 * 
			 */
			this.msr.findValidComponents(this.currentSegment, this.segmentValues, false, this.fromCOA, this.orgID);
			log.debug("Segment Name###" + seg);
			log.debug("Before loop for setwhere am");
			if (this.orgID.equalsIgnoreCase("AEP") && this.appName != null && (appName.equalsIgnoreCase("PLUSWOTR")
					|| appName.equalsIgnoreCase("X1PLUSWOTR") || appName.equalsIgnoreCase("PLUSDWOTRK"))) {
				log.debug("Inside loop for setwhere --->" + appName + this.currentSegment);
				if (this.currentSegment == 3) {
					log.debug("setwhere is being set --->");
					this.msr.setWhere(
							"COMPVALUE IN (SELECT x1workorder FROM  X1WOFIN WHERE X1SYSSOURCE IN ( 'MAX', 'WMS')  AND X1STATUS='ACTIVE' )");
				}
			}
			setOrderBy("compvalue asc");

			reset();
		}
		return returnValue;
	}

	public int setsegmentvalue() throws MXException, RemoteException {

		if (this.currentSegment >= this.segmentCount) {
			return 1;
		}

		if ((this.orgID == null) || (this.orgID.equals(""))) {
			throw new MXApplicationException("financial", "GLNavOrgId");
		}

		WebClientEvent currentEvent = this.clientSession.getCurrentEvent();
		WebClientEvent additionalSelectRecordEvent = new WebClientEvent("selectrecord", currentEvent.getTargetId(),
				currentEvent.getValue(), Integer.toString(currentEvent.getRow()), this.clientSession);

		this.clientSession.handleEvent(additionalSelectRecordEvent);

		String select = getString("COMPVALUE");

		log.debug(" set segment value method >>>>> COMPVALUE " + select);

		if (!WebClientRuntime.isNull(select)) {
			boolean hasAccess = getMXSession().getProfile().getGLAuth(this.currentSegment, this.orgID);
			if (!hasAccess) {
				if ((this.app == null) || (!this.app.getProperty("id").equalsIgnoreCase("USER"))
						|| (!getMXSession().getProfile().getGLAuthForUserApp(this.currentSegment))) {
					Object[] params = { getSegmentName(this.currentSegment) };
					throw new MXApplicationException("financial", "SegmentAccessDenied", params);
				}
			}

			if (!validateComponent(WebClientRuntime.unFormatString(select))) {
				String[] msgArray = new String[2];
				msgArray[0] = WebClientRuntime.unFormatString(select);
				msgArray[1] = getSegmentName(getCurrentSegment());
			}

			if (this.segmentValues == null) {
				return 1;
			}
			log.debug("Current segment = " + this.currentSegment);

			if (this.currentSegment == 3) {
				log.debug("Work Order Segment 3 is being set");
				String seg3Select = select;
				log.debug("Work Order Segment 3 has been set to " + select);
			}

			this.segmentValues[this.currentSegment] = select;
			resetQbe();
			this.clientSession.queueEvent(new WebClientEvent("setsegment", this.clientSession.getCurrentPageId(),
					new Integer(this.currentSegment + 1).toString(), this.clientSession));
		}
		return 1;
	}

	public int refreshGL() throws MXException, RemoteException {

		resetQbe();
		for (int i = this.currentSegment; i < this.segmentCount; i++) {
			this.segmentValues[i] = "";
		}
		return 1;
	}

	public String getPlaceHolder() {
		try {
			if (getMboSet() != null) {
				return this.glFormat.getPlaceHolder();
			}
			return null;
		} catch (Throwable t) {
		}
		return null;
	}

	public GLFormat getCurrentGLFormat() throws MXException {
		return new GLFormat(this.segmentValues);
	}

	public int getCurrentSegment() {
		return this.currentSegment;
	}

	public String getSegmentValue(int segment) {
		try {
			if (getMboSet() != null) {
				if ((this.segmentValues[segment] == null) || (this.segmentValues[segment].equals(""))) {
					return this.glFormat.segmentPlaceHolder(segment);
				}
				return this.segmentValues[segment];
			}

			return null;
		} catch (Throwable t) {

		}
		return null;
	}

	public String getSegmentName(int segment) {

		try {
			if (getMboSet() != null) {
				return this.glFormat.getSegmentName(segment);
			}
			return null;
		} catch (Throwable t) {
		}
		return null;
	}

	public int getSegmentSize(int segment) {
		return this.glFormat.getSegmentLength(segment);
	}

	public char getSegmentDelimiter(int segment) {
		return this.glFormat.getScreenDelimiter(segment);
	}

	public int getSegmentCount() {
		try {
			if (getMboSet() != null) {
				return this.glFormat.getSegmentCount();
			}
			return 0;
		} catch (Throwable t) {
		}
		return 0;
	}

	public String getSegmentPlaceHolder() {

		try {

			if (getMboSet() != null) {
				return this.glFormat.getPlaceHolder();
			}
			return null;
		} catch (Throwable t) {
		}
		return null;
	}

	public boolean validateComponent(String compVal) throws MXException, RemoteException {

		boolean valid = false;
		String qbe = getQbe();
		resetQbe();
		setQbeExactMatch(true);
		setQbe(compVal);
		reset();
		if (count() > 0)
			valid = true;
		resetQbe();
		setQbeExactMatch(false);
		setQbe(qbe);
		reset();
		return valid;
	}

	public void refreshGLData(X1GLNavigatorSelectOrgBean orgBean) throws MXException, RemoteException {

		if ((this.orgID == null) || (this.orgID.equals(""))) {
			clearData();
			return;
		}

		if (this.sourceDataBean != null) {
			MboValueData mvd = null;
			mvd = this.sourceDataBean.getMboValueData(this.sourceControl.getProperty("dataattribute"));
			if (mvd != null) {
				this.originalGL = mvd.getData();
			}

		}
		this.msr = ((GLComponentsSetRemote) getMboSet());
		this.userInfo = msr.getUserInfo();
		if (this.orgID.equals("")) {
			return;
		}
		if (this.glFormat == null) {
			this.glFormat = this.msr.getGLFormat(this.orgID);
		}
		if ((orgBean != null) && (this.app != null) && (this.app.getProperty("id").equalsIgnoreCase("USER"))) {
			MboSetRemote orgSet = orgBean.getMboSet();
			if ((orgSet != null) && (!orgSet.isEmpty()) && (orgSet.count() == 1)) {
				this.msr.setOwner(orgSet.getMbo(0));
			}
		}
		log.debug(" Original GL " + this.originalGL);
		log.debug(" Org ID " + this.orgID);
		GLFormat glformatOfTarget = new GLFormat(this.originalGL, this.orgID);
		this.segmentValues = glformatOfTarget.getSegments();
		this.segmentCount = glformatOfTarget.getSegmentCount();

		log.debug(" Segment Count " + this.segmentCount);

		this.currentSegment = 0;
		for (int i = 0; i < this.segmentValues.length; i++) {
			if ((this.segmentValues[i] != null) && (!this.segmentValues[i].equals(""))) {
				log.debug(" Segment Values >>>>> " + this.segmentValues[i]);
				continue;
			}
			this.currentSegment = i;
			break;
		}

		if (this.sourceDataBean.getMboSet().getName().equalsIgnoreCase("CHARTOFACCOUNTS")) {
			this.fromCOA = true;
		}

		if (this.firstTime) {
			this.msr.findValidComponents(this.currentSegment, this.segmentValues, false, this.fromCOA, this.orgID);
			this.firstTime = false;
		}
		setOrderBy("compvalue asc");
		reset();
		setCurrentRow(0);
	}

	public void setOrgId(String newOrgId) {
		if (!this.orgID.equalsIgnoreCase(newOrgId))
			this.orgID = newOrgId;
	}

	public void clearData() throws MXException {
		reset();
		this.fetchData = false;
	}

	public void fetchOrgAndSiteIdFromSource() {
		try {
			DataBean sourceDataBeanOld = this.sourceDataBean;
			if (this.sourceDataBean.getMbo() == null) {
				if (this.sourceDataBean.getMboOrZombie() == null) {
					this.sourceDataBean = this.sourceDataBean.getParent();
				}
				if (this.sourceDataBean != null) {
					if ((!this.sourceDataBean.getMboOrZombie().isZombie()) && (this.sourceDataBean.getMbo() == null)) {
						this.sourceDataBean = sourceDataBeanOld;
						return;
					}

					MboRemote zombie = this.sourceDataBean.getMboOrZombie();
					this.siteID = ((Mbo) zombie).getProfile().getDefaultSite();
					this.orgID = ((Mbo) zombie).getProfile().getDefaultOrg();
					this.sourceDataBean = sourceDataBeanOld;
					return;
				}

				this.sourceDataBean = this.app.getAppBean();
			}

			if (this.sourceDataBean.getMbo() != null) {

				if (this.sourceDataBean.getMbo().getMboValueInfoStatic("siteid") != null) {
					this.siteID = this.sourceDataBean.getMbo().getString("siteid");
				}

				SystemServiceRemote sys = (SystemServiceRemote) getMXSession().lookup("SYSTEM");
				Object[] map = sys.getLookupKeyMap(this.sourceDataBean.getMbo().getName(), "GLACCOUNT",
						getMXSession().getUserInfo());

				if ((map != null) && (map.length != 0)) {
					for (int i = 0; i < map.length; i++) {
						Object[] arr = (Object[]) (Object[]) map[i];

						if (!arr[1].toString().equalsIgnoreCase("orgid"))
							continue;
						this.orgAttribute = arr[0].toString();
						this.orgID = this.sourceDataBean.getMbo().getString(this.orgAttribute);
					}
				} else {
					this.orgID = this.sourceDataBean.getMbo().getString("orgid");
				}
				this.sourceDataBean = sourceDataBeanOld;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public synchronized void structureChangedEvent(DataBean speaker) {

		try {
			reset();

			this.firstTime = false;

			initialize();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MXException e) {
			e.printStackTrace();
		}
	}

	public void setCurrentSegment(int i) {
		this.currentSegment = i;
	}

	/*************
	 * This method will return ArryList object from GLACCOUNT String
	 * 
	 * @param gl
	 * @return segments
	 */

	public ArrayList getSegmentValues(String gl) {

		StringTokenizer stk = new StringTokenizer(gl, "-");
		ArrayList<String> segments = new ArrayList<String>();
		int count = 0;
		while (stk.hasMoreTokens()) {
			// log.debug(count+" <<<"+stk.nextToken());
			segments.add(count, stk.nextToken());
			count++;
		}
		return segments;
	}

	/*************
	 * Custom method to tokenize work order GL String - AM
	 * 
	 * @param gl
	 * @return segments
	 */

	public ArrayList tokenizeGL(String wogl) {

		StringTokenizer stk1 = new StringTokenizer(wogl, "-");
		ArrayList<String> wosegments = new ArrayList<String>();
		int wosegcount = 0;
		while (stk1.hasMoreTokens()) {
			// log.debug(count+" <<<"+stk.nextToken());
			wosegments.add(wosegcount, stk1.nextToken());
			wosegcount++;
		}
		return wosegments;
	}

	/*************
	 * GLACCOUNT will be updated based on 4th Segment selected in GLNavigator
	 * 
	 * @param glaccount
	 * @return
	 * @throws RemoteException
	 * @throws MXException
	 *             validate here for GL string L
	 */

	public String[] getGLUpdate(String glaccount) throws RemoteException, MXException {

		String wonum = null;
		String x1seg3 = null;
		String[] newGL = null;
		MboSetRemote x1wofinSet = null;
		MboSetRemote x1projpsSet = null;
		MboSetRemote x1workorderSet = null;
		ArrayList segments = getSegmentValues(glaccount);
		ArrayList<String> tempSeg = new ArrayList<String>();
		ArrayList previousSeg = null;
		ArrayList currentSeg = null;

		log.debug("getGLUpdate >>>>>> GL update started for  " + glaccount);
		log.debug("getGLUpdate >>>>>> segment size is " + Integer.toString(segments.size()));
		if (segments.size() >= 4) {
			wonum = (String) segments.get(3);
			log.debug("getGLUpdate >>>>>> wonum is " + wonum);
		}
		if (segments.size() >= 3) {
			x1seg3 = (String) segments.get(2);
			log.debug("getGLUpdate >>>>>> seg3 is " + x1seg3);
		}
		log.debug("getGLUpdate >>>>>>Work Order Segment is being validated");
		String seg3CurrValue = (String) segments.get(3);
		if (seg3CurrValue != null && seg3Select != seg3CurrValue) {
			updatedWO = true;
			log.debug("getGLUpdate >>>>>> Work Order Segment is updateWO == true");
		}
		try {
			if (x1seg3 != null && x1seg3 != "") {
				log.debug("Inside seg3 not null check " + x1seg3);
				if (this.appName != null && !(appName.equalsIgnoreCase("CHRTACCT"))
						&& !(appName.equalsIgnoreCase("PLUSPR") || appName.equalsIgnoreCase("PLUSPO")
								|| appName.equalsIgnoreCase("PLUSDRECPT"))) {
					log.debug("Inside APP check");
					if (x1seg3.equalsIgnoreCase("1011001")) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				}
			}
			// ALM 4342 -- L work order validation
			if (wonum == null || wonum.trim().length() == 0 || wonum.contains("?")) {
				if (this.appName != null && !(appName.equalsIgnoreCase("CHRTACCT"))) {
					if (("1011001".equalsIgnoreCase(x1seg3) && !(wonum.startsWith("L")))
							|| (!"1011001".equalsIgnoreCase(x1seg3) && (wonum.startsWith("L")))) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				}
			}

			if (wonum != null && wonum != "") {
				log.debug("getGLUpdate >>>>>> Inside wonum not null" + wonum);
				if (this.appName != null && !(appName.equalsIgnoreCase("CHRTACCT"))
						&& !(appName.equalsIgnoreCase("PLUSPR") || appName.equalsIgnoreCase("PLUSPO")
								|| appName.equalsIgnoreCase("PLUSDRECPT"))) {
					if (wonum.startsWith("L")) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					} else if (x1seg3.equalsIgnoreCase("1011001")) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				}
				// ALM 4342 -- L work order validation
				log.debug("getGLUpdate >>>>>> Before L work order validation " + x1seg3 + " " + wonum);
				if ((x1seg3.equalsIgnoreCase("1011001") && !(wonum.startsWith("L")))
						|| (!x1seg3.equalsIgnoreCase("1011001") && (wonum.startsWith("L")))) {
					log.debug("getGLUpdate >>>>>> Inside invalid wonum/seg3");
					if (this.appName != null && !(appName.equalsIgnoreCase("CHRTACCT"))) {
						throw new MXApplicationException("WORKORDER", "lworkOrder");
					}
				} else if (this.appName != null && !(appName.equalsIgnoreCase("CHRTACCT"))) {
					previousSeg = getSegmentValues(this.originalGL);
					if (previousSeg.size() >= 4) {
						prevWonum = (String) previousSeg.get(3);
						log.debug("previous segment is >>>>>> prevWonum is " + prevWonum);
						log.debug("current segment is >>>>>> currWonum is " + wonum);
						if (prevWonum != "" && prevWonum != null && prevWonum.length() != 0) {
							if (!prevWonum.equals(wonum)) {
								diffWonum = true;
								log.debug("Setting diffWonum flag to true >>>>>> currWonum is " + wonum);
								log.debug("Setting diffWonum flag to true >>>>>> prevWonum is " + prevWonum);

							}
						} else if (prevWonum == "" | prevWonum == null) {
							log.debug("previous segment is blank >>>>>> prevWonum is " + prevWonum);
							log.debug("Setting diffWonum to false since prevwonum is blank >>>>>> " + prevWonum);
							diffWonum = true;

						}
					} else {
						log.debug("AM# Old segment was never set so diffWonum will be set to True");
						diffWonum = true;
					}

					log.debug("AM This from inside master else original GL >>>>" + this.originalGL);
					log.debug("AM# This is the current selected wonum" + wonum);
					log.debug("AM# This is the old wonum" + prevWonum);
					log.debug("AM# diffwonum flag" + diffWonum);

					x1wofinSet = MXServer.getMXServer().getMboSet("X1WOFIN", userInfo);
					x1wofinSet.setWhere("X1WORKORDER='" + wonum + "'");
					log.debug(x1wofinSet.count() + " X1WOFIN Set Count >>>>>>>>> ");
					if (!x1wofinSet.isEmpty()) {
						MboRemote wofin = x1wofinSet.getMbo(0);
						if (wofin.getString("X1SYSSOURCE") != null
								&& (wofin.getString("X1SYSSOURCE").equalsIgnoreCase("MAX"))
								|| (wofin.getString("X1SYSSOURCE").equalsIgnoreCase("WMS"))) {
							log.debug("SYSSOURCE in MAX,WMS");
							// Updated as part of defect 3195 to exclude CKP from source validation
							log.debug("getGLUpdate >>>>>> SYSSOURCE in MAX,WMS");
							x1workorderSet = MXServer.getMXServer().getMboSet("WORKORDER", userInfo);
							x1workorderSet.setWhere("WONUM='" + wonum + "'");
							if (x1workorderSet.count() > 0) {
								log.debug("getGLUpdate >>>>>> x1workorderSet count>0");
								MboRemote workorder = x1workorderSet.getMbo(0);
								String glaccountStr = workorder.getString("GLACCOUNT");
								log.debug("getGLUpdate >>>>>> Work Order GLAccount >>>> " + glaccountStr);
								// AM Don't update the segments directly, check if segment 4 has been updated
								// first
								log.debug("getGLUpdate >>>>>> AM# Maximo work order test >>>> " + diffWonum);
								log.debug("getGLUpdate >>>>>> AM# GLAccount to be set >>>> " + glaccountStr);
								log.debug("getGLUpdate >>>>>> AM# calling custom tokenizer to tokenize >>>> "
										+ glaccountStr);
								try {
									log.debug("inside try block >>>> ");

									tempSeg = tokenizeGL(glaccountStr);
								} catch (Exception ex) {
									log.debug("yes exceptions 1");
									ex.printStackTrace();
									throw(ex);

								}
								log.debug("no exceptions 1");
								try {
									if (tempSeg.size() >= 1) {
										tseg0 = (String) tempSeg.get(0);
									}
									if (tempSeg.size() >= 2) {
										tseg1 = (String) tempSeg.get(1);
									}
									if (tempSeg.size() >= 3) {
										tseg2 = (String) tempSeg.get(2);
									}
									if (tempSeg.size() >= 5) {
										tseg4 = (String) tempSeg.get(4);
									}
									if (tempSeg.size() >= 6) {
										tseg5 = (String) tempSeg.get(5);
									}
									if (tempSeg.size() >= 7) {
										tseg6 = (String) tempSeg.get(6);
									}
									if (tempSeg.size() >= 8) {
										if (this.appName != null && appName.equalsIgnoreCase("X1UVL")) {
											tseg7 = "9AA";
										}
										else {
											tseg7 = (String) tempSeg.get(7);
										}
									}
									if (tempSeg.size() >= 9) {
										tseg8 = (String) tempSeg.get(8);
									}
									if (tempSeg.size() >= 10) {
										tseg9 = (String) tempSeg.get(9);
									}
									if (tempSeg.size() >= 11) {
										tseg10 = (String) tempSeg.get(10);
									}
								} catch (Exception ex) {
									log.debug("yes exceptions 2");
									ex.printStackTrace();
									throw(ex);
								}
								log.debug("no exceptions 2 ");

								log.debug("getGLUpdate >>>>>> tokenized 4th seg value >>>> " + tseg4);
								log.debug("getGLUpdate >>>>>> tokenized 5th seg value >>>> " + tseg5);

								log.debug("AM# before if diffWonum >>>> " + diffWonum);
								if (diffWonum) {
									log.debug("Inside Maximo work order diffwonum is true" + diffWonum);

									// GLBU
									if (tseg0 != "" && tseg0 != null && tseg0.length() != 0) {
										if (segments.size() >= 1)
											segments.set(0, tseg0);
										else
											segments.add(0, tseg0);
										log.debug("getGLUpdate >>>>>> segment 0 >>>> " + (String) tempSeg.get(0));
									}

									// DEPARTMENT
									if (tseg1 != "" && tseg1 != null && tseg1.length() != 0) {
										if (segments.size() >= 2)
											segments.set(1, tseg1);
										else
											segments.add(1, tseg1);
										log.debug("getGLUpdate >>>>>> segment 1 >>>> " + (String) tempSeg.get(1));
									}

									// GL ACCOUNT
									if (tseg2 != "" && tseg2 != null && tseg2.length() != 0) {
										if (segments.size() >= 3)
											segments.set(2, tseg2);
										else
											segments.add(2, tseg2);
										log.debug("getGLUpdate >>>>>> segment 2 >>>> " + (String) tempSeg.get(2));
									}

									// PCBU
									if (tseg4 != "" && tseg4 != null && tseg4.length() != 0) {
										if (segments.size() >= 5)
											segments.set(4, tseg4);
										else
											segments.add(4, tseg4);
										log.debug("getGLUpdate >>>>>> segment 4 >>>> " + (String) tempSeg.get(4));
									}

									// PROJECTID
									if (tseg5 != "" && tseg5 != null && tseg5.length() != 0) {
										if (segments.size() >= 6)
											segments.set(5, tseg5);
										else
											segments.add(5, tseg5);
										log.debug("getGLUpdate >>>>>> segment 5 >>>> " + (String) tempSeg.get(5));
									}
									// Add rest of the segments

									// ABM 6
									if (tseg6 != "" && tseg6 != null && tseg6.length() != 0) {
										if (segments.size() >= 7)
											segments.set(6, tseg6);
										else
											segments.add(6, tseg6);
										log.debug("getGLUpdate >>>>>> segment 6 >>>> " + (String) tempSeg.get(6));
									}

									// CC 7
									if (tseg7 != "" && tseg7 != null && tseg7.length() != 0) {
										if (segments.size() >= 8)
											segments.set(7, tseg7);
										else
											segments.add(7, tseg7);
										log.debug("getGLUpdate >>>>>> segment 7 >>>> " + (String) tempSeg.get(7));
									}
									// STATE/JURIS 8
									if (tseg8 != "" && tseg8 != null && tseg8.length() != 0) {
										if (segments.size() >= 9)
											segments.set(8, tseg8);
										else
											segments.add(8, tseg8);
										log.debug("getGLUpdate >>>>>> segment 8 >>>> " + (String) tempSeg.get(8));
									}

									// PRODUCT 9

									if (tseg9 != "" && tseg9 != null && tseg9.length() != 0) {
										if (segments.size() >= 10)
											segments.set(9, tseg9);
										else
											segments.add(9, tseg9);
										log.debug("getGLUpdate >>>>>> segment 9 >>>> " + (String) tempSeg.get(9));
									}

									// RES-SUBCAT 10
									if (tseg10 != "" && tseg10 != null && tseg10.length() != 0) {
										if (segments.size() >= 11)
											segments.set(10, tseg10);
										else
											segments.add(10, tseg10);
										log.debug("getGLUpdate >>>>>> segment 10 >>>> " + (String) tempSeg.get(10));
									}
								} else {
									log.debug("Inside Maximo work order diffwonum is false" + diffWonum);

									// PCBU
									if (tseg4 != "" && tseg4 != null && tseg4.length() != 0) {
										if (segments.size() >= 5)
											segments.set(4, tseg4);
										else
											segments.add(4, tseg4);
										log.debug("getGLUpdate >>>>>> segment 4 >>>> " + (String) tempSeg.get(4));
									}

									// PROJECTID
									if (tseg5 != "" && tseg5 != null && tseg5.length() != 0) {
										if (segments.size() >= 6)
											segments.set(5, tseg5);
										else
											segments.add(5, tseg5);
										log.debug("getGLUpdate >>>>>> segment 5 >>>> " + (String) tempSeg.get(5));
									}
								}

							}
						} else {
							log.debug("BEFORE PP WORK ORDER SCENARIO" + diffWonum);

							if (diffWonum) {
								log.debug("Inside PP work order diffwonum is TRUE" + diffWonum);

								// Code added as part of defect 3195, to satisfy the 3rd requirement. lines 714-
								// 778
								log.debug("getGLUpdate >>>>>> SYSSOURCE NOT in MAX,WMS");
								log.debug("getGLUpdate >>>>>>Work Order >>>> " + wonum);
								// AM Don't update the segments directly, check if segment 4 has been updated
								// first
								log.debug(" AM# Before check for updatedwo >>>> " + updatedWO);

								log.debug("getGLUpdate >>>>>> inside not updated WO setting all segments" + wonum);
								String seg0 = wofin.getString("X1GLBU");
								if (seg0 != null && seg0 != "" && seg0.length() != 0) {
									if (segments.size() >= 1)
										segments.set(0, seg0);
									else
										segments.add(0, seg0);
									log.debug("getGLUpdate >>>>>> segment 0 >>>> " + seg0);
								}
								String seg1 = wofin.getString("X1DEPARTMENT");
								if (seg1 != null && seg1 != "" && seg1.length() != 0) {
									if (segments.size() >= 2)
										segments.set(1, seg1);
									else
										segments.add(1, seg1);
									log.debug("getGLUpdate >>>>>> segment 1 >>>> " + seg1);
								}
								String seg2 = wofin.getString("X1ACCOUNT");
								if (seg2 != null && seg2 != "" && seg2.length() != 0) {
									if (segments.size() >= 3)
										segments.set(2, seg2);
									else
										segments.add(2, seg2);
									log.debug("getGLUpdate >>>>>> segment 2 >>>> " + seg2);
								}
								String seg3 = wofin.getString("X1WORKORDER");
								if (seg3 != null && seg3 != "" && seg3.length() != 0) {
									if (segments.size() >= 4)
										segments.set(3, seg3);
									else
										segments.add(3, seg3);
									log.debug("getGLUpdate >>>>>> segment 3 >>>> " + seg3);
								}
								String seg4 = wofin.getString("X1PCBU");
								if (seg4 != null && seg4 != "" && seg4.length() != 0) {
									if (segments.size() >= 5)
										segments.set(4, seg4);
									else
										segments.add(4, seg4);
									log.debug("getGLUpdate >>>>>> segment 4 >>>> " + seg4);
								}
								String seg5 = wofin.getString("X1PROJECTID");
								if (seg5 != null && seg5 != "" && seg5.length() != 0) {
									if (segments.size() >= 6)
										segments.set(5, seg5);
									else
										segments.add(5, seg5);
									log.debug("getGLUpdate >>>>>> segment 5 >>>> " + seg5);
								}
							} else {
								log.debug("Inside PP work order diffwonum is FALSE" + diffWonum);

								String seg4 = wofin.getString("X1PCBU");
								if (seg4 != null && seg4 != "" && seg4.length() != 0) {
									if (segments.size() >= 5)
										segments.set(4, seg4);
									else
										segments.add(4, seg4);
									log.debug("getGLUpdate >>>>>> segment 4 >>>> " + seg4);
								}
								String seg5 = wofin.getString("X1PROJECTID");
								if (seg5 != null && seg5 != "" && seg5.length() != 0) {
									if (segments.size() >= 6)
										segments.set(5, seg5);
									else
										segments.add(5, seg5);
									log.debug("getGLUpdate >>>>>> segment 5 >>>> " + seg5);
								}
							}

						}
					}
				}
			}

			if (segments.size() >= 6) {
				String projectid = (String) segments.get(5);

				x1projpsSet = MXServer.getMXServer().getMboSet("X1PROJPS", userInfo);
				x1projpsSet.setWhere("X1FINPROJECTID='" + projectid + "'");
				if (!x1projpsSet.isEmpty()) {
					MboRemote projps = x1projpsSet.getMbo(0);
					String x1pcbu = projps.getString("X1PCBU");
					if (x1pcbu != null && !x1pcbu.trim().equalsIgnoreCase("")) {
						log.debug(" New X1PCBU >>>>>" + x1pcbu);
						segments.set(4, x1pcbu);
					}
				}
			}

			newGL = new String[segments.size()];
			log.debug("New GL String value length >>>>>>>>>>>>" + newGL.length);
			for (int i = 0; i < segments.size(); i++) {
				if (this.appName != null
						&& (appName.equalsIgnoreCase("X1TIMESHEETS") || appName.equalsIgnoreCase("X1CREWTS"))) {
					log.debug("application name>>" + appName);
					if (i == 0) {
						log.debug("Time reporting skip glbu set");
						newGL[i] = (String) previousSeg.get(0);
						log.debug("original TSGLBU value>>" + (String) previousSeg.get(0));
					}
					if (i == 1) {
						log.debug("Time reporting skip dept set ");
						newGL[i] = (String) previousSeg.get(1);
						log.debug("original TSDEPT value>>" + (String) previousSeg.get(1));
					}
					if (i == 7) {
						log.debug("Time reporting skip cc set ");
						newGL[i] = (String) previousSeg.get(7);
						log.debug("original TSCC value>>" + (String) previousSeg.get(7));

					} else if (!(i == 0 || i == 1 || i == 7)) {
						log.debug("Time reporting set others ");
						newGL[i] = (String) segments.get(i);
						log.debug("original TS setting a value>>" + (String) segments.get(i));

					}
				} else {
					newGL[i] = (String) segments.get(i);
				}
				log.debug(" String Array Value " + newGL[i]);
			}

		} catch (Exception e) {
			e.printStackTrace();
            throw(e);
		} finally {
			if (x1wofinSet != null)
				x1wofinSet.close();
			if (x1projpsSet != null)
				x1projpsSet.close();
		}
		return newGL;

	}

	private void setOrigGL(String glvalue) {
		this.origGL = glvalue;
	}

	private String getOrigGL() {
		return this.origGL;
	}

}