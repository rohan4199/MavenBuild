package x1.psdi.app.inventory;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboValue;
import psdi.util.MXException;
import x1.psdi.iface.valgl.ValGL;

public class FldMatRecTransGLAcct extends psdi.app.inventory.FldMatRecTransGLAcct {

	private String CLASS_NAME = this.getClass().getName();
	public FldMatRecTransGLAcct(MboValue mbv) throws MXException, RemoteException {
		super(mbv);
		// TODO Auto-generated constructor stub
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

		if (!ValGL.isOrgPSEnabled(orgId)) {
			ValGL.printDebug(">>>> Calling super.validate()");
			super.validate();
			ValGL.printDebug(">>>> super.validate() executed successfully - GL is valid.");
		} 
		else {
			ValGL.printDebug(">>>> Performing custom validation As ORG is PS Validation Enabled");
			// Validate GL String from Web Service
			Mbo mbo = getMboValue().getMbo();
			String objectName = mbo.getName();
			ValGL.printDebug(">>>> Object name is " + objectName);
			ValGL.printDebug(">>>> Field to be validated: " + objectName + "." + getMboValue().getAttributeName());
			ValGL vtg = new ValGL();
			vtg.validateGL(glString, orgId);
			}		
		ValGL.printDebug(">>>> Leaving " + CLASS_NAME + ".validate()");
	}
}
