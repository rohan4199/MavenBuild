package x1.psdi.iface.valgl;

import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

public interface ValGLConstants {
	
	//public static final String VALTRANLOGGERNAME = "maximo.service.FINANCIAL";
	public static final String VALTRANLOGGERNAME = "maximo.integration";
	public static final String VALTRANWSURL = "x1.psdi.valgl.url"; // JMDA
																	// 02/14/17
	public static final String ADDGLTOMAXIMO = "x1.psdi.valgl.autoAddGL"; // JMDA
																			// 02/14/17
	public static final String ORGIDLIST = "x1.psdi.valgl.orgIdList"; 
	
	public static final MXLogger LOGGER = MXLoggerFactory.getLogger(VALTRANLOGGERNAME);
	
	public static final boolean DEBUG = LOGGER.isDebugEnabled();
	
	public static final String PSVALIDATIONENABLED = "x1.psdi.valgl.psValidationEnabled"; //JMDA 09/16/17
	
	public static final String IGNOREERRORLIST = "x1.psdi.valgl.ignoreErrorList";
	
	public static final String IGNOREERRORSETLIST = "x1.psdi.valgl.ignoreErrorSetList";
}
