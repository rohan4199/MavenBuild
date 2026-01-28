package x1.psdi.iface.valgl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import psdi.server.MXServer;
import psdi.util.MXApplicationException;
import psdi.util.MXException;
import psdi.util.logging.MXLogger;
import psdi.util.logging.MXLoggerFactory;

/*
<ns0:ValidationRequest version="1" sequence="1" origin="PSHR" onBehalfOf="PSHR" definitive="0" correlator="" QOS="">
	<n2:generalLedgerAccount>{GLCOMP2}</n2:generalLedgerAccount>
	<n2:accountingDate>2007-06-01</n2:accountingDate>
	<n2:affiliate/>
	<n2:analysisType>GLE</n2:analysisType>
	<n2:generalLedgerBusinessUnit>{GLCOMP0}</n2:generalLedgerBusinessUnit>
	<n2:currencyCode/>
	<n2:department>{GLCOMP1}</n2:department>
	<n2:ledger>ACTUALS</n2:ledger>
	<n2:ledgerGroup>ACTUALS</n2:ledgerGroup>
	<n2:St-Jurisdiction>{GLCOMP8}</n2:St-Jurisdiction>
	<n2:productCode>{GLCOMP9}</n2:productCode>
	<n2:statisticsCode> </n2:statisticsCode>
	<n2:projectInfo>
		<n2:workOrderTask>{GLCOMP3}</n2:workOrderTask>
		<n2:projectBusinessUnit>{GLCOMP4}<n2:projectBusinessUnit>
		<n2:project>{GLCOMP5}<n2:project>
		<n2:ABMActivity>{GLCOMP6}</n2:ABMActivity>
		<n2:resourceSubCategory>{GLCOMP10}</n2:resourceSubCategory>
		<n2:costComponent>{GLCOMP7}</n2:costComponent>
	<n2:projectInfo/>
</ns0:ValidationRequest>
*/

public class ValGLRequest {
	public MXLogger LOGGER = null;
	public boolean DEBUG = false ;
	public String SERVER_URL = null;
	public String ORGID_LIST = null;
	public String IGNOREERROR_LIST = null;
	public String IGNOREERRORSET_LIST = null;
	private String CLASS_NAME = "x1.psdi.iface.valtran.ValTranGLRequest";
	//GL Segments
	public int GLBU = 0;
    public int DEPT = 1;
    public int ACCT = 2;
    public int ACT = 3;
    public int PCBU = 4;
    public int PROJECT = 5;
    public int ABM = 6;
    public int CC = 7;
    public int OU = 8;
    public int PROD = 9;
    public int RESESUB = 10;
    public int ignored_PS_ERR_CNT = 0;
    
	public ValGLRequest() throws RemoteException {
		LOGGER = MXLoggerFactory.getLogger(ValGLConstants.VALTRANLOGGERNAME);
		DEBUG = LOGGER.isDebugEnabled();
		SERVER_URL = MXServer.getMXServer().getProperty(ValGLConstants.VALTRANWSURL);
		ORGID_LIST = MXServer.getMXServer().getProperty(ValGLConstants.ORGIDLIST);
		IGNOREERROR_LIST = MXServer.getMXServer().getProperty(ValGLConstants.IGNOREERRORLIST);
		IGNOREERRORSET_LIST = MXServer.getMXServer().getProperty(ValGLConstants.IGNOREERRORSETLIST);

	}

	public int validateGL (String glString, String orgId) throws MXApplicationException {
		return (validateGL	 (glString, orgId, false,null));
	}

	/*
	 * does the heavy lifting - Calls PS Parses the response and passes back PS Validation errors in an exception
	 */
    public int validateGL (String glString, String orgId, boolean ignoreWorkOrderError,String affiliate) throws MXApplicationException {
    	ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".validateGL()");
    	ValGL.printDebug(">>>>> GLSTRING >>>>> : " + glString + " >>>>> ORGID >>>>> "+orgId+" >>>>> IGNOREWOERRORS >>>>> "+ignoreWorkOrderError+"  >>>>> AFFILIATE >>>>>  "+affiliate);
  	  //BasicConfigurator.configure();
		if ((SERVER_URL == null) || (SERVER_URL.trim().length() <= 0)) {
			throw new MXApplicationException("iface", "novaltranURL");
		}
		String orgIdList = ORGID_LIST;
		String[] orgIdListArr = orgIdList.split(",");
		boolean validOrgId = java.util.Arrays.asList(orgIdListArr).contains(orgId);
		SOAPConnectionFactory soapConnectionFactory = null;
		SOAPConnection soapConnection = null;
        try {
        	ValGL.printDebug(">>>>> Check ORGID String " + orgId + " if valid...");
        	if (validOrgId) {
        		ValGL.printDebug(">>>>> Create SOAP Connection...");
                // Create SOAP Connection
                soapConnectionFactory = SOAPConnectionFactory.newInstance();
                soapConnection = soapConnectionFactory.createConnection();
                ValGL.printDebug(">>>>> Send SOAP Message to SOAP Server...");
                // Send SOAP Message to SOAP Server
                SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(glString,affiliate), SERVER_URL);           
                String messageSoapResponse = soapMessageToString(soapResponse);
               	ValGL.printDebug("Response Message" + messageSoapResponse);
                ValGL.printDebug(">>>>> Create document builder factory...");
            	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            	DocumentBuilder builder;
            	builder = factory.newDocumentBuilder();
            	Document document = builder.parse(new InputSource(new StringReader(messageSoapResponse)));
            	document.getDocumentElement().normalize();
            	String numberOfErrors = document.getElementsByTagName("fint:numberOfErrors").item(0).getTextContent();
            	ValGL.printDebug(">>>>> Check the request has no errors...");
            	ValGL.printDebug(">>>>> numberOfErrors: " + numberOfErrors);
            	int errCount = Integer.parseInt(numberOfErrors);
            	if (errCount!=0){
            		ValGL.printDebug(">>>>> Retrieve error details...");
            		NodeList errorElementNodeList = document.getElementsByTagName("fint:errorDetail");
            		String messageErrorResponse = getErrors(errorElementNodeList, ignoreWorkOrderError);
            		
					if (messageErrorResponse != null && messageErrorResponse.trim().length() > 0) {
						ValGL.printDebug(">>>>> Deactivate COA Record...");
						// Deactivate COA Record
						messageErrorResponse = "\nGL String " + glString + " for ORGID " + orgId + " failed validation in PeopleSoft.\n" + messageErrorResponse;
						ValGL vl;
						try {
							vl = new ValGL();
							vl.setCOAActive(glString, orgId, false);
						} catch (MXException e) {
							e.printStackTrace();
							String errorMessage = "\nGL String " + glString + " for ORGID " + orgId + " failed validation in PeopleSoft.\n";
							errorMessage = errorMessage + e.getMessage();
							String[] params = { errorMessage };
							throw new MXApplicationException("custom", "valtranglexception", params);
						}
						ValGL.printDebug(">>>>> messageErrorResponse: " + messageErrorResponse);
						ValGL.printDebug(">>>>> Throw Maximo Exception to display SOAP Error Response...");
						// Throw Maximo Exception to display SOAP Error Response
						String[] params = { glString, messageErrorResponse };
						throw new MXApplicationException("custom", "glnotvalid", params);
					}
				}     	
            	//Write response to a XML file
                //FileWriter fileWriter = new FileWriter("C:\\temp\\responseCOA.xml");
                //fileWriter.write(soapMessageToString(soapResponse));
                //fileWriter.close();
            	//Close SOAP Connection
 
                soapConnection.close();
        	}
        } catch (IOException e) {
        	ValGL.printDebug(">>>>> Throw IOException..."); 
        	e.printStackTrace();
        	String [] params = {e.getMessage()};
			throw new MXApplicationException ("custom", "valtranglexception", params);
		} catch (ParserConfigurationException e) {
			ValGL.printDebug(">>>>> Throw ParserConfigurationException..."); 
			e.printStackTrace();
        	String [] params = {e.getMessage()};
			throw new MXApplicationException ("custom", "valtranglexception", params);
		} catch (SAXException e) {
			ValGL.printDebug(">>>>> Throw SAXException..."); 			
			e.printStackTrace();
        	String [] params = {e.getMessage()};
			throw new MXApplicationException ("custom", "valtranglexception", params);		
		} catch (SOAPException e) {
			ValGL.printDebug(">>>>> Throw SOAPException..."); 			
			e.printStackTrace();
        	String [] params = {e.getMessage()};
			throw new MXApplicationException ("custom", "valtranglexception", params);
		}
        ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + "..validateGL()");
        return ignored_PS_ERR_CNT;
    }
    
    /*
     * Parses SOAP Response for errors
     */
    
    private String getErrors (NodeList errorElementNodeList, boolean ignoreWorkOrderErrors)
    {
    	String messageErrorResponse = "\n";
    	
    	String ignoreErrorSetList = IGNOREERRORSET_LIST;
    	String ignoreErrorList = IGNOREERROR_LIST;
				
   		for (int i=0; i < errorElementNodeList.getLength(); i++){
			Node errorElementNode = errorElementNodeList.item(i);
			if (errorElementNode.getNodeType() == Node.ELEMENT_NODE){
				Element errorElement = (Element) errorElementNode;
				String peopleSoftMessageSet = errorElement.getElementsByTagName("fint:peopleSoftMessageSet").item(0).getTextContent();
				String peopleSoftMessageNumber = errorElement.getElementsByTagName("fint:peopleSoftMessageNumber").item(0).getTextContent();
				String severity = errorElement.getElementsByTagName("fint:severity").item(0).getTextContent();
				String errorMessageText = errorElement.getElementsByTagName("fint:errorMessageText").item(0).getTextContent();
				String errorDescription = errorElement.getElementsByTagName("fint:errorDescription").item(0).getTextContent();
				//Check the error set
				boolean ignoreErrorSet = false;
				boolean ignoreError = false;
				if (ignoreWorkOrderErrors){
					if (ignoreErrorSetList != null && ignoreErrorSetList.trim().length() > 0) {
						String[] ignoreErrorSetListArr = ignoreErrorSetList.split(",");
						ignoreErrorSet = java.util.Arrays.asList(ignoreErrorSetListArr).contains(peopleSoftMessageSet);
					}
					//Check the error				
					if (ignoreErrorList != null && ignoreErrorList.trim().length() > 0) {
						String[] ignoreErrorListArr = ignoreErrorList.split(",");
						ignoreError = java.util.Arrays.asList(ignoreErrorListArr).contains(peopleSoftMessageNumber);
					}
				if(ignoreError || ignoreErrorSet)
				{
					ignored_PS_ERR_CNT = ignored_PS_ERR_CNT+1;
				}
				}
				if (!ignoreError || !ignoreErrorSet){
					messageErrorResponse = messageErrorResponse + "peopleSoftMessageSet:" + peopleSoftMessageSet + "\n";
					messageErrorResponse = messageErrorResponse + "peopleSoftMessageNumber:" + peopleSoftMessageNumber + "\n";
					messageErrorResponse = messageErrorResponse + "severity:" + severity + "\n";
					messageErrorResponse = messageErrorResponse + "errorMessageText:" + errorMessageText + "\n";
					messageErrorResponse = messageErrorResponse + "errorDescription:" + errorDescription + "\n\n"; 
				}			      				
			}
		}
    	return messageErrorResponse;
    }
    
    /*
     * Creates the actual SOAP Request
     */
    private SOAPMessage createSOAPRequest(String glString, String affiliate) throws SOAPException, MXApplicationException {
    	ValGL.printDebug(">>>>> Entering " + CLASS_NAME + ".createSOAPRequest()");
    	String[] glSplit = glString.split("-");
        int glComp = 0;
        String glBu = "";
        String dept = "";
        String acct = "";
        String act = "";
        String pcBu = "";
        String project = "";
        String abm = "";
        String cc = "";
        String ou = "";
        String prod = "";
        String reseSub = "";
       
        
        while (glComp < glSplit.length){
            switch(glComp){
                case 0:
                    glBu = glSplit[GLBU];
                    glBu = glCompPartial(glBu);
                    break;
                case 1:
                    dept = glSplit[DEPT];
                    dept = glCompPartial(dept);
                    break;
                case 2:
                    acct = glSplit[ACCT];
                    acct = glCompPartial(acct);
                    break;
                case 3:
                    act = glSplit[ACT];
                    act = glCompPartial(act);
                    break;
                case 4:
                    pcBu = glSplit[PCBU];
                    pcBu = glCompPartial(pcBu);
                    break;
                case 5:
                    project = glSplit[PROJECT];
                    project = glCompPartial(project);
                    break;
                case 6:
                    abm = glSplit[ABM];
                    abm = glCompPartial(abm);
                    break;
                case 7:
                    cc = glSplit[CC];
                    cc = glCompPartial(cc);
                    break;
                case 8:
                    ou = glSplit[OU];
                    ou = glCompPartial(ou);
                    break;
                case 9:
                    prod = glSplit[PROD];
                    prod = glCompPartial(prod);
                    break;
                case 10:
                    reseSub = glSplit[RESESUB];
                    reseSub = glCompPartial(reseSub);
                    break;
            }
            glComp ++;
        }	
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
		String accDate = dateFormat.format(date);
        MessageFactory messageFactory;
        SOAPMessage soapMessage = null;
        ValGL.printDebug(">>>>> Create MessageFactory...");
		messageFactory = MessageFactory.newInstance();
		soapMessage = messageFactory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();
		String serverURI = SERVER_URL;
		ValGL.printDebug(">>>>> Create SOAP Envelope...");
		// SOAP Envelope
		SOAPEnvelope envelope = soapPart.getEnvelope();
		ValGL.printDebug(">>>>> Create SOAP Body...");
		// SOAP Body
		SOAPBody soapBody = envelope.getBody();
		ValGL.printDebug(">>>>> Set xmlns:ct");	
		envelope.addNamespaceDeclaration("ct", "http://www.aep.com/types/common");
		ValGL.printDebug(">>>>> Set xmlns:n2");	
		envelope.addNamespaceDeclaration("n2", "http://www.aep.com/types/val");
		ValGL.printDebug(">>>>> Set xmlns:ns0");	
		envelope.addNamespaceDeclaration("ns0", "http://www.aep.com/messages/finance");
		ValGL.printDebug(">>>>> Set xmlns:fint");
		envelope.addNamespaceDeclaration("fint", "http://www.aep.com/types/aug");
		ValGL.printDebug(">>>>> Set xsi:schemaLocation");
		envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		envelope.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance","xsi:schemaLocation","http://schemas.xmlsoap.org/soap/envelope/");
		ValGL.printDebug(">>>>> Set ns0:ValidationRequest...");	        
		SOAPElement soapBodyElem = soapBody.addChildElement("ValidationRequest", "ns0");
		ValGL.printDebug(">>>>> Set QOS");
		QName attrQOS = new QName("QOS");
		soapBodyElem.addAttribute(attrQOS, "");
		ValGL.printDebug(">>>>> Set origin");
		QName attrOrigin = new QName("origin");
		soapBodyElem.addAttribute(attrOrigin, "PSHR");
		ValGL.printDebug(">>>>> Set version");
		QName attrVersion = new QName("version");
		soapBodyElem.addAttribute(attrVersion, "1");
		ValGL.printDebug(">>>>> Set sequence");
		QName attrSequence = new QName("sequence");
		soapBodyElem.addAttribute(attrSequence, "1");
		ValGL.printDebug(">>>>> Set correlator");
		QName attrCorrelator = new QName("correlator");
		soapBodyElem.addAttribute(attrCorrelator, "");
		ValGL.printDebug(">>>>> Set correlator");
		QName attrDefinitive = new QName("definitive");
		soapBodyElem.addAttribute(attrDefinitive, "0");
		ValGL.printDebug(">>>>> Set onBehalfOf");
		QName attrOnBehalfOf = new QName("onBehalfOf");
		soapBodyElem.addAttribute(attrOnBehalfOf, "PSHR");		        
		ValGL.printDebug(">>>>> Set GL Values...");	
		ValGL.printDebug(">>>>> Set generalLedgerAccount");
		soapBodyElem.addChildElement("generalLedgerAccount", "n2").addTextNode(acct);
		ValGL.printDebug(">>>>> Set accountingDate");
		soapBodyElem.addChildElement("accountingDate", "n2").addTextNode(accDate);
		ValGL.printDebug(">>>>> Set affiliate");
		if (affiliate != null && affiliate.trim().length() > 0)
		{
			ValGL.printDebug(">>>>> PASSING AFFILIATE TO PS :" + affiliate );
			soapBodyElem.addChildElement("affiliate", "n2").addTextNode(affiliate);
		}
		else
		{
			ValGL.printDebug(">>>>> NOT PASSING ANY AFFILIATE TO PS AS the Value is NULL .. :" + affiliate );
			soapBodyElem.addChildElement("affiliate", "n2");
		}
		ValGL.printDebug(">>>>> Set analysisType");
		soapBodyElem.addChildElement("analysisType", "n2").addTextNode("ACT");
		ValGL.printDebug(">>>>> Set generalLedgerBusinessUnit");
		soapBodyElem.addChildElement("generalLedgerBusinessUnit", "n2").addTextNode(glBu);
		ValGL.printDebug(">>>>> Set currencyCode");
		soapBodyElem.addChildElement("currencyCode", "n2");
		ValGL.printDebug(">>>>> Set department");
		soapBodyElem.addChildElement("department", "n2").addTextNode(dept);
		ValGL.printDebug(">>>>> Set ledger");
		soapBodyElem.addChildElement("ledger", "n2").addTextNode("ACTUALS");
		ValGL.printDebug(">>>>> Set ledgerGroup");
		soapBodyElem.addChildElement("ledgerGroup", "n2").addTextNode("ACTUALS");
		ValGL.printDebug(">>>>> Set St-Jurisdiction");
		soapBodyElem.addChildElement("St-Jurisdiction", "n2").addTextNode(ou);
		ValGL.printDebug(">>>>> Set productCode");
		soapBodyElem.addChildElement("productCode", "n2").addTextNode(prod);
		ValGL.printDebug(">>>>> Set statisticsCode");
		soapBodyElem.addChildElement("statisticsCode", "n2");
		ValGL.printDebug(">>>>> Set n2:projectInfo...");
		SOAPElement soapBodyElem12 = soapBodyElem.addChildElement("projectInfo", "n2");
		ValGL.printDebug(">>>>> Set workOrderTask");
		soapBodyElem12.addChildElement("workOrderTask", "n2").addTextNode(act);
		ValGL.printDebug(">>>>> Set projectBusinessUnit");
		soapBodyElem12.addChildElement("projectBusinessUnit", "n2").addTextNode(pcBu);
		ValGL.printDebug(">>>>> Set project");
		soapBodyElem12.addChildElement("project", "n2").addTextNode(project);
		ValGL.printDebug(">>>>> Set ABMActivity");
		soapBodyElem12.addChildElement("ABMActivity", "n2").addTextNode(abm);
		ValGL.printDebug(">>>>> Set resourceSubCategory");
		soapBodyElem12.addChildElement("resourceSubCategory", "n2").addTextNode(reseSub);
		ValGL.printDebug(">>>>> Set costComponent");
		soapBodyElem12.addChildElement("costComponent", "n2").addTextNode(cc);
		ValGL.printDebug(">>>>> Set MimeHeaders..");
		MimeHeaders headers = soapMessage.getMimeHeaders();
		headers.addHeader("SOAPAction", serverURI + "ValidationRequest");
		ValGL.printDebug(">>>>> Save Changes...");
		soapMessage.saveChanges();
		
		ValGL.printDebug("SOAP Request is :: " + soapMessageToString(soapMessage));
		ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".createSOAPRequest()");
        return soapMessage;
    }

    /*
     * Conversts a SOAPMessage to a String
     */
    public String soapMessageToString(SOAPMessage message) throws MXApplicationException {
    	ValGL.printDebug(">>>>> Entering " + CLASS_NAME + "..soapMessageToString()");
        String result = null;

        if (message != null) {
            ByteArrayOutputStream baos = null;
            try {
                baos = new ByteArrayOutputStream();
                message.writeTo(baos); 
                result = baos.toString();
           
            } 
            catch (Exception e) {} 
            finally {
                if (baos != null) {
                    try {
                        baos.close();
                    } 
                    catch (IOException ioe) {
                    	ioe.printStackTrace();
            			// TODO Auto-generated catch block
                    	ValGL.printDebug(">>>>> Throw IOException..."); 
                    	ioe.printStackTrace();
                    	//String [] params = {ioe.getMessage()};
            			//throw new MXApplicationException ("custom", "valtranglexception", params);
                    }
                }
            }
        }
        ValGL.printDebug(">>>>> Leaving " + CLASS_NAME + ".soapMessageToString()");
        return result;
    } 
    
    /*
     * Strips ? from a GLCOMP and returns an empty string
     */
    public String glCompPartial(String glComp){
    	if (glComp.contains("?")){
    		glComp = "";
    	}
    	return glComp;
    }
}