package x1.psdi.app.invoice;

import java.rmi.RemoteException;

import psdi.mbo.MboValue;
import psdi.util.MXException;
import x1.psdi.iface.valgl.ValGL;
import x1.psdi.iface.valgl.ValGLConstants;

public class FldInvoiceCostGLCreditAcct extends psdi.app.invoice.FldInvoiceCostGLCreditAcct implements ValGLConstants{

	private String CLASS_NAME = this.getClass().getName();

	public FldInvoiceCostGLCreditAcct(MboValue mbv) throws RemoteException {
		super(mbv);
	}

	public void validate() throws MXException, RemoteException {
		ValGL.printDebug(">>>> Entering " + CLASS_NAME + ".validate() for Attribute " + getMboValue().getAttributeName());
		if (getMboValue().isNull()) {
			ValGL.printDebug(">>>> Leaving " + CLASS_NAME + ".validate() - Value is null");
			return;
		}
		String orgId = getMboValue("ORGID").getString();
		String glString = getMboValue().toString();
		if (!ValGL.isOrgPSEnabled(orgId)) 
		{
			ValGL.printDebug(">>>> Calling Maximo Internal Validation for ORG " + orgId + ". Value is " + glString);
			super.validate();
		}
		else 
		{
			ValGL.printDebug(">>>> No validation called. Validation is thru script.  ORG is " + orgId);
		}
		ValGL.printDebug(">>>> Leaving " + CLASS_NAME + ".validate()");

	}

}
