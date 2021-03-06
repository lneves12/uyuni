<%@ taglib uri="http://rhn.redhat.com/rhn" prefix="rhn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://rhn.redhat.com/tags/list" prefix="rl" %>


<html>
<head>
</head>
<body>
<rhn:toolbar base="h1" icon="header-system" imgAlt="system.common.systemAlt"
 helpUrl="/rhn/help/reference/en-US/ref.webui.systems.systems.jsp#ref.webui.systems.systems.ood">
  <bean:message key="outofdatelist.jsp.header"/>
</rhn:toolbar>

<rl:listset name="outOfDateListSet" legend="system">
    <rhn:csrf />
    <rhn:submitted />
    <c:set var="noAddToSsm" value="1" />
    <%@ include file="/WEB-INF/pages/common/fragments/systems/system_listdisplay.jspf" %>
</rl:listset>


</form>
</body>
</html>
