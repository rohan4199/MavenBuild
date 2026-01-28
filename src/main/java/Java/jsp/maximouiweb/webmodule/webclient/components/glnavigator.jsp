<%--
* Licensed Materials - Property of IBM
* 
* 5724-U18
* 
* (C) COPYRIGHT IBM CORP. 2006, 2011 All Rights Reserved.
* 
* US Government Users Restricted Rights - Use, duplication or
* disclosure restricted by GSA ADP Schedule Contract with
* IBM Corp.
--%><%--
This JSP renders the GL Account number.

CREATED ON: Feb 21, 2006
--%>
<%@ include file="../common/simpleheader.jsp" %><%

String dialogId = component.getProperty("datasrc");
x1.psdi.webclient.beans.common.X1GLNavigatorDialogBean glNavBean = null;

if(dialogId != null)
	glNavBean = (x1.psdi.webclient.beans.common.X1GLNavigatorDialogBean)app.getDataBean(dialogId);

String events = "onmouseout=\"return noStatus();\" "+
				"onfocus=\"return noStatus();\" "+
				"onmouseover=\"return noStatus();\" ";

if(glNavBean!=null)
{

String spanAutoId="";
if(automation)
	spanAutoId="automationid=\""+realId+"_segments\"";
if(component.needsRender()){ %>
<div aria-live="polite" id="<%=id%>_holder" class="bc">
<%}%>
	<span id="<%=id%>_segments" <%=spanAutoId%> style="padding-<%=defaultAlign%>:5px;text-align:center">
<%	int currentSegment = glNavBean.getCurrentSegment();
	// Build link for segment each segment glNavBean.getSegmentCount()
	for (int segmentNo = 0; segmentNo < glNavBean.getSegmentCount(); segmentNo++)
	{
		//glNavBean.getSegmentValue(x); 
		String sendEvent = "sendEvent('click','"+id+"','"+segmentNo+"')";
        String segId = id+segmentNo;	%>		
		<a id="<%=segId%>" tabindex="0" title="<%= glNavBean.getSegmentValue(segmentNo)%>" href="javascript:<%=sendEvent %>" onkeypress="if(hasKeyCode(event,'KEYCODE_ENTER')) {<%=sendEvent %>}" 
			class="<%= (segmentNo==currentSegment)?textcss+" glsegment_selected":textcss+" glsegment" %>" <%=events %> 
			glsegmentvalue="<%= glNavBean.getSegmentValue(segmentNo)%>"><%= glNavBean.getSegmentValue(segmentNo)%></a>
		<span class="glsegment_delimiter"><%= glNavBean.getSegmentDelimiter(segmentNo)%></span><%
 	} %>		
	</span>
<%	if(component.needsRender()){%>
</div>
<% }
}%><%@ include file="../common/componentfooter.jsp" %>