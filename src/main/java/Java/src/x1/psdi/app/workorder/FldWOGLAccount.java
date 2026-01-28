package x1.psdi.app.workorder;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboValue;
import psdi.util.MXException;
import x1.psdi.iface.valgl.ValGL;
import x1.psdi.iface.valgl.ValGLConstants;

public class FldWOGLAccount extends psdi.app.financial.FldPartialGLAccount implements ValGLConstants{

	private String CLASS_NAME = this.getClass().getName();

	public FldWOGLAccount(MboValue mbv) throws RemoteException, MXException {
		super(mbv);
	}

	public void validate() throws MXException, RemoteException {
		/* If value is null, no validations */
		ValGL.printDebug(">>>> Entering " + CLASS_NAME + ".validate() ");
		MboValue me = getMboValue();
	
		String objAttr = me.getMbo().getRecordMboName() + "." + me.getAttributeName();
		ValGL.printDebug(">>>> Object.Attribute is " + objAttr);
		if (me.isNull()) {
			ValGL.printDebug(">>>> Leaving " + CLASS_NAME + ".validate() - value is NULL");
			return;
		}
		String glString = getMboValue().getString();
		String orgId = getMboValue("ORGID").getString();
		ValGL.printDebug(">>>> Validating GLValue" + glString + " for " + objAttr + " in ORG: " + orgId);

		try {
			ValGL.printDebug(">>>> Calling super.validate()");
			super.validate();
			ValGL.printDebug(">>>> super.validate() executed successfully - GL is valid.");
		} 
		catch (MXException e) {
			if (!ValGL.isOrgPSEnabled(orgId)) {
				throw e;
			}
			else 
			{
				ValGL.printDebug(">>>> No validation called. Validation is thru script.  ORG is " + orgId);
			}
		}
		ValGL.printDebug(">>>> Leaving " + CLASS_NAME + ".validate()");
	}

}
