package x1.psdi.iface.valgl;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.soap.SOAPException;

import psdi.app.financial.AccountRemote;
import psdi.app.financial.GLComponents;
import psdi.app.financial.GLComponentsRemote;
import psdi.iface.mic.MicUtil;
import psdi.mbo.GLFormat;
import psdi.mbo.MboConstants;
import psdi.mbo.MboRemote;
import psdi.mbo.MboSetRemote;
import psdi.security.UserInfo;
import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;

public class ValGL implements ValGLConstants {
	private String CLASS_NAME = this.getClass().getName();
	private String ADD_GL_TO_MAXIMO_STR = null;
	private boolean ADD_GL_TO_MAXIMO = false;
	/**
	 * 
	 * @throws RemoteException
	 */
	public ValGL() throws RemoteException {
		ADD_GL_TO_MAXIMO_STR = MXServer.getMXServer().getProperty(ValGLConstants.ADDGLTOMAXIMO);
		ADD_GL_TO_MAXIMO = ADD_GL_TO_MAXIMO_STR != null && !ADD_GL_TO_MAXIMO_STR.equalsIgnoreCase("0");
	}

	/**
	 * 
	 * @param glString
	 * @param orgId
	 * @throws RemoteException
	 * @throws MXException
	 * @throws SOAPException
	 * @throws UnsupportedOperationException
	 */
	public void validatePartialGL(String glString, String orgId, MXException ex) throws RemoteException, MXException {
		ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".validatePartial()");
		GLFormat account = null;
		ValGLRequest vtgr = null;
		try {
			try {		
				// See if account is fully specified.  
				account = new GLFormat(glString, true, orgId);
			}
			catch (MXException zz) {
			//If any error occurs during the creation of account, rethrow the original error
				throw ex;
			}
			if (account != null && !account.isFullySpecified()) {
					throw ex;
			}
			// Validate GL String from Web Service
			vtgr = new ValGLRequest();
			vtgr.validateGL(glString, orgId);
		}
		finally {
			vtgr = null;
			ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".validatePartial()");
			
		}
	}
	
	/**
	 * @param glString
	 * @param orgId
	 * @throws RemoteException
	 * @throws MXException
	 * @throws  
	 * @throws SOAPException
	 * @throws UnsupportedOperationException
	 */
	
	public void validateGL(String glString, String orgId) throws RemoteException, MXException {
    ValGL.printDebug(">>>>> GLSTRING >>>>> : " + glString + " >>>>> ORGID >>>>> "+orgId);
		validateGL(glString, orgId, false,null);
	}
	
	/**
	 * @param glString
	 * @param orgId
	 * @param ignoreWorkOrderErrors
	 * @throws RemoteException
	 * @throws MXException
	 * @throws  
	 * @throws SOAPException
	 * @throws UnsupportedOperationException
	 */
	public void validateGL(String glString, String orgId, boolean ignoreWorkOrderErrors) throws RemoteException, MXException{
		ValGL.printDebug(">>>>> GLSTRING >>>>> : " + glString + " >>>>> ORGID >>>>> "+orgId+" >>>>> IGNOREWOERRORS >>>>> "+ignoreWorkOrderErrors);
        validateGL(glString, orgId, ignoreWorkOrderErrors,null);
	}
	
	/**
	 * @param glString
	 * @param orgId
	 * @param ignoreWorkOrderErrors
	 * @param affiliate
	 * @throws RemoteException
	 * @throws MXException
	 * @throws  
	 * @throws SOAPException
	 * @throws UnsupportedOperationException
	 */
	public void validateGL(String glString, String orgId, boolean ignoreWorkOrderErrors, String affiliate) throws RemoteException, MXException{
      ValGL.printDebug(">>>>> GLSTRING >>>>> : " + glString + " >>>>> ORGID >>>>> "+orgId+" >>>>> IGNOREWOERRORS >>>>> "+ignoreWorkOrderErrors+"  >>>>> AFFILIATE >>>>>  "+affiliate);
	  try {
      	boolean noGLAccountValidation = getMaxVars("DISABLEGLSWITCH", orgId);
	    if (!noGLAccountValidation) 
        {
			if (ValGL.isOrgPSEnabled(orgId))
			{
				if (ValGL.psValidationEnabled()){
					ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".validateGL()");
					if (DEBUG) {
						ValGL.printDebug(">>>>> Validating GL " + glString);
					}
					/*
					 * Check if GL Components Exist
					 */
					GLFormat account = null;
					try {
						// See if account is fully specified.
						account = new GLFormat(glString, true, orgId);
					} catch (MXException ex) {

						String msgResp = "";
						msgResp = "\nGL Account " + glString + " for ORGID " + orgId + " could not be inserted in to Maximo COA table.\n";
						msgResp = msgResp + "This means that the one or more of the components doesn't exist in Maximo's GLCOMPONENTS table.\n";
						msgResp = msgResp + "\n\nMaximo message = " + ex.getMessage();
						ex.printStackTrace();
						String[] params = { glString, msgResp };
						throw new MXApplicationException("custom", "glnotvalid", params);
					}
					/*
					 * Check if the all the GL COMPONENTS are ACTIVE in maximo
					 */
					String erroredComponents = isGLComponentsValid(glString, orgId);
					if(erroredComponents!="")
					{
						String[] params = { erroredComponents,glString };
						throw new MXApplicationException("custom", "invalidGLComponent", params);
					}
					
					/*
					 * Check if the glString is already in Chart of accounts
					 */
					
					if (this.isGLAccountValid(glString, orgId))
					{
						// If the GLAccout is in COA and is Valid, then do not call PS.
						return;
					}
					// Validate GL String from Web Service
					int ignored_ERR_CNT;
					ValGLRequest vtgr = new ValGLRequest();
					ignored_ERR_CNT = vtgr.validateGL(glString, orgId, ignoreWorkOrderErrors, affiliate );
		
					/*
					 * If we got here, then the validation succeeded - add to
					 * ChartOfAccounts.
					 * Add only if the GL String does not contain the "?" character and if NO errors were IGNORED.
					 */
					 //Ignore adding to COA if Affiliate value is passed to PS Validation routine.
					if ((ADD_GL_TO_MAXIMO) && (glString.indexOf("?") < 0) && ignored_ERR_CNT==0 && affiliate == null)
					{
						ValGL.printDebug(">>>>> Adding GL " + glString + " to Maximo");
						try {
							addCOAWithComponents(glString, orgId, false);
						}
						catch (MXException|RemoteException e)
						{
							String msgResp = "";
							msgResp = "\nGL Account " + glString + " for ORGID " + orgId + " could not be inserted in to Maximo COA table.\n";
							msgResp =msgResp + "This means that the one or more of the components doesn't exist in Maximo's GLCOMPONENTS table.\n";
							msgResp = msgResp + "\n\nMaximo message = " + e.getMessage();	
							
							e.printStackTrace();
							
							String[] params = { glString, msgResp };
							throw new MXApplicationException("custom", "glnotvalid", params);
						}
					}				
				}
			}
		}
      }
      catch (ParseException e) {
		e.printStackTrace();		
      }
      finally {
		ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".validateGL()");
      }
	}
	
	/*
	 * Method that adds GL Components to GLCOMPONENENTS table - not used 
	 */
	
	

	public void addGLComponents(String[] segs, String orgId) throws RemoteException, MXException {
		ValGL.printDebug(">>>>> In :: " + CLASS_NAME + ".addGLComponents()");
		if (segs == null) {
			return;
		}

		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote glCompSet = null;
		GLComponentsRemote glComp = null;

		try {
			int len = segs.length;

			for (int i = 0; i < len; i++) {

				if ((segs[i] != null) && (segs[i].trim().length() > 0)) {

					/*
					 * Don't create a components set unless needed
					 */
					if (glCompSet == null) {
						server = MXServer.getMXServer();
						userInfo = server.getUserInfo(getMEAUser());
						glCompSet = server.getMboSet("GLCOMPONENTS", userInfo);
					}
					glCompSet.reset();
					String where = "compvalue = '" + segs[i] + "' and glorder = '" + i + "' and orgid = '" + orgId
							+ "'";
					glCompSet.setWhere(where);
					glCompSet.reset();

					if (glCompSet.isEmpty()) {
						// Component Doesn't exist - add
						ValGL.printDebug(">>>>> Adding  GL Components :: " + segs[i]);

						glComp = (GLComponentsRemote) glCompSet.add();
						glComp.setValue("compvalue", segs[i],
								MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
						glComp.setValue("comptext", segs[i],
								MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
						glComp.setValue("glorder", i,
								MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
						glComp.setValue("orgid", orgId,
								MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
					} else {
						// Component exist - activate
						ValGL.printDebug(">>>>> Updating  GL Components :: " + segs[i]);
						glComp = (GLComponentsRemote) glCompSet.getMbo(0);
					}
					glCompSet.setInsertOrg(orgId);
					glComp.setValue("active", true,
							MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
					glCompSet.save();
					glCompSet.reset();
				}
			}
		} finally {
			glCompSet = null;
			glComp = null;
			server = null;
			userInfo = null;
			ValGL.printDebug(">>>>> Leaving :: " + CLASS_NAME + ".addGLComponents()");
		}
	}
	
	/*
	 * 
	 * Adds or updates the COA table.  Sets EXPIREDATE to the current day, 11:59:59 PM
	 */

	public void addCOAWithComponents(String glAccount, String orgId, boolean addComponents) throws RemoteException, MXException, ParseException {
		// Segment names
		ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".addCOAWithComponents()");
		
		String[] segs;

		// Check that the code has been fully specified
		GLFormat account = new GLFormat(glAccount, true, orgId);

		// Check if the GL Components used are valid
		// First get the segments
		segs = account.getSegments();

		int numComponents = segs.length;

		// String [] compDesc = segs.clone();
		boolean[] CompDescAsNameArray = new boolean[numComponents];
		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote coaSet = null;
		AccountRemote coa = null;
		String glDesc = "";
		String glcompValue;
		int segNo;

		ValGL.printDebug(">>>>> Adding  GL Code Combination to Maximo :: " + glAccount);

		try {
			glDesc = this.getGLSegmentDescriptions(segs, orgId);
			if (glDesc == null || glDesc.trim().length() <= 0) {
				for (int i = 0; i < numComponents; i++) {
					CompDescAsNameArray[i] = false;
					if (i == 0) {
						glDesc = segs[i];
					} else {
						glDesc = glDesc + "+" + segs[i];
					}
					ValGL.printDebug("i=" + i + "  Component=" + segs[i]);
				}
			}
			
			
			if (glDesc!= null && glDesc.length() >= 100) glDesc = glDesc.substring(0,99);
			
			/* Add components if requested */
			if (addComponents) 
			{
				addGLComponents(segs, orgId);
			}
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			coaSet = server.getMboSet("CHARTOFACCOUNTS", userInfo);

			String where = "orgid = '" + orgId + "' and glaccount = '" + glAccount + "'";
			coaSet.setWhere(where);
			coaSet.reset();

			ValGL.printDebug(">>>>> Check coaSet.isEmpty()...");
			if (coaSet.isEmpty()) {
				ValGL.printDebug(">>>>> Adding  COA Components :: " + glAccount);
				coa = (AccountRemote) coaSet.add();
				ValGL.printDebug(">>>>> glAccount: " + glAccount);
				coa.setValue("glaccount", glAccount,
						MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				ValGL.printDebug(">>>>> orgId: " + orgId);
				coa.setValue("orgid", orgId, MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				ValGL.printDebug(">>>>> glDesc: " + glDesc);
				coa.setValue("accountname", glDesc,
						MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				for (int i = 0; i < account.getSegmentCount(); i++) {
					// Get the value of the component
					if ((segs[i] != null) && (segs[i].trim().length() > 0)) {
						glcompValue = segs[i].toString();
						segNo = i;

						// Generate the segment column name
						String glcomp = GLComponents.generateGLCompColumn(segNo);

						// Set the individual components to their respective
						// columns
						ValGL.printDebug(">>>>> " + glcomp + ": " + glDesc);
						coa.setValue(glcomp, glcompValue,
								MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
					}
				}
				//Set expiration date
				Date dateToday = new Date();
		        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		        String dateString = dateFormat.format(dateToday);
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date expireDate = sdf.parse(dateString + " " + "23:59:59");
				ValGL.printDebug(">>>>> expiredate:" + expireDate);
				coa.setValue("expiredate", expireDate, MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				coa.setValue("sourcesysid", "PEOPLESOFT", MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
			} else {
				coa = (AccountRemote) coaSet.getMbo(0);
				ValGL.printDebug(">>>>> Updating  COA Components :: " + glAccount);
				/*
				 * Copy "expiredate" to "oldexpiredate" and set "expiredate" to
				 * end of today
				 */
				ValGL.printDebug(">>>>> oldexpiredate: " + coa.getDate("expiredate"));
				coa.setValue("oldexpiredate", coa.getDate("expiredate"),
						MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);				
				//Set expiration date
				Date dateToday = new Date();
		        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		        String dateString = dateFormat.format(dateToday);
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				Date expireDate = sdf.parse(dateString + " " + "23:59:59");
				ValGL.printDebug(">>>>> expiredate:" + expireDate);
				coa.setValue("expiredate", expireDate, MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				coa.setValue("sourcesysid", "PEOPLESOFT", MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
			}

			/*
			 * Set active and activedate
			 */
			
			ValGL.printDebug(">>>>> orgId: " + orgId);
			coaSet.setInsertOrg(orgId); // JMDA 2/14/17
			ValGL.printDebug(">>>>> active: " + true);
			coa.setValue("active", true, MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
			ValGL.printDebug(">>>>> activedate: " + server.getDate());
			coa.setValue("activedate", server.getDate(),
					MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
			ValGL.printDebug(">>>>> Calling save() in  " + CLASS_NAME + ".addCOAWithComponents()");
			coaSet.save();
			coaSet.close();
		} finally {
			server = null;
			coaSet = null;
			coa = null;
			userInfo = null;
			// compDesc = null;
			CompDescAsNameArray = null;
			segs = null;
		}
		ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".addCOAWithComponents()");
	}
	/**
	 * 
	 * @return
	 * @throws RemoteException
	 * @throws MXException
	 */
	public String getMEAUser() throws RemoteException, MXException {
		// default should be mxintadm, configurable in System Properties
		String meaUser = MicUtil.getMEAProperty("mxe.int.dfltuser");
		if (meaUser == null || meaUser.length() == 0)
			meaUser = "MAXADMIN";
		return meaUser;
	}

	/**
	 * 
	 * @param glAccount
	 * @throws RemoteException
	 * @throws MXException
	 */
	public void setCOAActive(String glAccount, String orgId, boolean active) throws RemoteException, MXException {
		// Segment names
		ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".setCOAActive()");

		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote coaSet = null;
		AccountRemote coa = null;
		ValGL.printDebug(">>>>> Checking  if GL Code Combination exists Maximo :: " + glAccount);

		try {
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			coaSet = server.getMboSet("CHARTOFACCOUNTS", userInfo);

			String where = "orgid = '" + orgId + "' and glaccount = '" + glAccount + "'";
			coaSet.setWhere(where);
			coaSet.reset();

			if (!coaSet.isEmpty()) {
				coa = (AccountRemote) coaSet.getMbo(0);
				ValGL.printDebug(">>>>> Updating  COA Components :: " + glAccount);
				/*
				 * Copy "expiredate" to "oldexpiredate" and set "expiredate" to
				 * null
				 */
				coa.setValue("active", active, MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				coa.setValue("expiredate", server.getDate(),
						MboConstants.NOACCESSCHECK | MboConstants.NOVALIDATION_AND_NOACTION);
				coaSet.setInsertOrg(orgId);
			}
			// JMDA 2/14/17
			ValGL.printDebug(">>>>> Calling save() in  " + CLASS_NAME + ".setCOAActive()");
			coaSet.save();
			coaSet.close();
		} finally {
			server = null;
			coaSet = null;
			coa = null;
			userInfo = null;
		}
		ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".setCOAActive()");
	}
	
	/*
	 * Print debug messages
	 */
	public static void printDebug(String msg){
		MXLogger logger = ValGLConstants.LOGGER;
		boolean debug = ValGLConstants.DEBUG;
		if (debug)
			logger.debug(msg);
	}
	
	/*
	 * Has Maximo enable GL Validation
	 */
	public  boolean isGLValidationEnabled(String orgId) throws RemoteException, MXException
	{	
		boolean myVal = false;
		myVal =  !getMaxVars("DISABLEGLSWITCH",orgId);
		return myVal;
	}
	
	/*
	 * DO we need to call PS Validation 
	 */
	public static boolean psValidationEnabled() throws RemoteException{
		String psEnabled = MXServer.getMXServer().getProperty(ValGLConstants.PSVALIDATIONENABLED);
		boolean enabled =  psEnabled != null && !psEnabled.equalsIgnoreCase("0");
		return enabled;
	}
	
	public boolean getMaxVars(String varName, String orgId) throws RemoteException, MXException{
		// Segment names
		ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".getMaxVars()");

		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote maxVarsSet = null;
		MboRemote maxVars = null;
		//ValGL.printDebug(">>>>> Checking  GL Code Combination to Maximo :: " + glAccount);

		try {
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			maxVarsSet = server.getMboSet("MAXVARS", userInfo);

			String where = "orgid = '" + orgId + "' AND varname = '" + varName + "'";
			maxVarsSet.setWhere(where);
			maxVarsSet.reset();

			if (!maxVarsSet.isEmpty()) {
				maxVars = maxVarsSet.moveFirst();
				String varValue = maxVars.getString("VARVALUE");
				if (varValue.equalsIgnoreCase("1"))
					return true;
				else
					return false;
			}
			maxVarsSet.close();
		} finally {
			server = null;
			maxVarsSet = null;
			maxVars = null;
			userInfo = null;
		}		
		ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".getMaxVars()");
		return false;
	}
	
	/*
	 * Is orgId in the list of ORGS for which PS Validation needs to be called?
	 */
	public static boolean  isOrgPSEnabled (String orgId)
	{
		boolean validOrgId = false;
		try {
			String orgIdList = MXServer.getMXServer().getProperty(ValGLConstants.ORGIDLIST);
			String[] orgIdListArr = orgIdList.split(",");
			validOrgId = java.util.Arrays.asList(orgIdListArr).contains(orgId);
		} 
		catch (RemoteException e){
			
		}
		
		return validOrgId;
	}
    /*
	 * Check if ALL GL Components are valid and ACTIVE. 
	 * 
	 */
    private String isGLComponentsValid (String glAccount, String orgId)
	{
		
		String glComps = "";
		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote glCompSet = null;
		try 
		{
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			String[] glCompListArr = glAccount.split("-");
			for (int i = 0; i < glCompListArr.length; i++) 
			{
				if (glCompListArr[i].contains("?"))
				{
					continue;
				}
				else
				{
					glCompSet = server.getMboSet("GLCOMPONENTS", userInfo);
					String where = "ORGID = '" + orgId + "' and COMPVALUE  = '" + glCompListArr[i] + "' and GLORDER = '" + i + "' and active = '1'" ;
					glCompSet.setWhere(where);
					glCompSet.reset();
					if (glCompSet.isEmpty())
					{
						if(glComps == "")
							glComps = glCompListArr[i];
						else
							glComps = glComps +" , "+ glCompListArr[i];
					}
				}
			}
		}
		catch (Exception e)
		{
			//DO nothing
		}
		finally 
		{
			try 
			{
				glCompSet.close();
			} 
			catch (RemoteException | MXException e) 
			{
				//do nothing
			}
			server = null;
			glCompSet = null;
			userInfo = null;
		}

		return glComps;
	}
	
	/*
	 * Check if GL Account is in COA and is Active
	 */
	private boolean isGLAccountValid (String glAccount, String orgId)
	{
		boolean isValid = false;
		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote coaSet = null;
		try 
		{
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			coaSet = server.getMboSet("CHARTOFACCOUNTS", userInfo);

			String where = "orgid = '" + orgId + "' and glaccount = '" + glAccount + "' and active = '1'" ;
			coaSet.setWhere(where);
			coaSet.reset();
			if (!coaSet.isEmpty())
			{
				AccountRemote coa = (AccountRemote) coaSet.getMbo(0);
				isValid = coa.isActive();
			}
		}
		catch (Exception e)
		{
			//DO nothing
		}
		finally 
		{
			try 
			{
				coaSet.close();
			} 
			catch (RemoteException | MXException e) 
			{
				//do nothing
			}
			server = null;
			coaSet = null;
			userInfo = null;
		}

		return isValid;
	}
	
	private String getGLSegmentDescriptions (String [] segs, String orgId)
	{
		ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".getGLSegmentDescriptions()");

		UserInfo userInfo = null;
		MXServer server = null;
		MboSetRemote glCompSet = null;
		String glDesc = "";
		try 
		{
			server = MXServer.getMXServer();
			userInfo = server.getUserInfo(getMEAUser());
			glCompSet = server.getMboSet("GLCOMPONENTS", userInfo);
			
			ValGL.printDebug(">>>>> before for loop in " + CLASS_NAME + ".getGLSegmentDescriptions()");
			for (int i = 0 ; i < segs.length; i++){
				String where = "ORGID = '" + orgId + "' and COMPVALUE  = '" + segs[i] + "' and GLORDER = '" + i + "'" ;
				ValGL.printDebug(">>>>> Where Clause is :: )" + where);

				glCompSet.setWhere(where);
				glCompSet.reset();
				if (!glCompSet.isEmpty())
				{
					GLComponentsRemote glComp = (GLComponentsRemote) glCompSet.getMbo(0);
					if (i != 0) glDesc += "+";
					glDesc = glDesc + glComp.getString("COMPTEXT");
				}
			}
			ValGL.printDebug(">>>>> after for loop in " + CLASS_NAME + ".getGLSegmentDescriptions()");

		}
		catch (Exception e)
		{
			//DO nothing
			ValGL.printDebug(">>>>> Caught exception in try block : error is  )" + e.getMessage() );
			if (ValGLConstants.DEBUG) e.printStackTrace();
		}
		finally 
		{
			try {
				glCompSet.close();
			} catch (RemoteException | MXException e) {
				// Do nothing
			}
			glCompSet = null;
			userInfo = null;
			server = null;
			ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".getGLSegmentDescriptions()");

		}
		return glDesc;
	}

}
