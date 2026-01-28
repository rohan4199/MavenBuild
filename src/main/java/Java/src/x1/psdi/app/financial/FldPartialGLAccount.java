/**
 * Description: This class extends psdi.app.financial.FldFullGLAccount to use validate() method.
 * When a GL component is deactivated or does not exist, the PeopleSoft validation will be invoked 
 * using the GL String and Org ID. The Deactivate GL Validations? checkbox for the selected 
 * organization should be unchecked in the Chart of Accounts application. 
 * 
 * List of attributes to be validated are:
 * TAX.EXCLUSIVEGL, TAX.INCLUSIVEGL
 * COMPANIES.APCONTROLACC, COMPANIES.APSUSPENSEACC, COMPANIES.RBNIACC
 * INVCOST.CONTROLACC, INVCOST.GLACCOUNT, INVCOST.INVCOSTADJACC
 * INVRESERVE.GLACCOUNT
 * INVOICE.APCONTROLACCT, INVOICE.APSUSPENSEACCT, INVOICE.TAX1GL, INVOICE.TAX2GL, INVOICE.TAX3GL, 
 * INVOICE.TAX4GL, INVOICE.TAX5GL
 * INVOICECOST.GLCREDITACCT, INVOICECOST.GLDEBITACCT
 * INVOICETRANS.GLCREDITACCT, INVOICETRANS.GLDEBITACCT
 * LOCATIONS.CONTROLACC, LOCATIONS.CURVARACC, LOCATIONS.INVCOSTADJACC, LOCATIONS.INVOICEVARACC
 * INVUSELINE.GLCREDITACCT
 */
package x1.psdi.app.financial;

import java.rmi.RemoteException;

import psdi.mbo.Mbo;
import psdi.mbo.MboValue;
import psdi.util.MXException;
import x1.psdi.iface.valgl.ValGL;
import x1.psdi.iface.valgl.ValGLConstants;

public class FldPartialGLAccount extends psdi.app.financial.FldPartialGLAccount implements ValGLConstants{

	private String CLASS_NAME = this.getClass().getName();

	public FldPartialGLAccount(MboValue mbv) throws RemoteException {
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