<%@page import="org.openl.rules.webtools.WebTool"%>
<%@page import="org.openl.rules.webstudio.web.util.WebStudioUtils"%>
<%@page import="org.openl.rules.ui.WebStudio"%>
<%@page import="org.openl.util.StringTool"%>
<%@page import="org.apache.commons.lang.StringUtils"%>

<%
    final String contextPath = request.getContextPath();

    final String TREE_PAGE = contextPath + "/faces/facelets/tree.xhtml";
    final String TABLE_PAGE = contextPath + "/faces/facelets/tableeditor/showTable.xhtml";
    final String INTRO_PAGE = contextPath + "/html/ws-intro.html";

    String leftFramePageToOpen = TREE_PAGE;
    String mainFramePageToOpen = INTRO_PAGE;

    WebStudio studio = WebStudioUtils.getWebStudio(session);
    String reload = request.getParameter("reload");
    if ("true".equals(reload)) {
        studio.rebuildModel();
    }
    String tableUri = null;
    String reopenCurrentTable = request.getParameter("reopen");
    if ("true".equals(reopenCurrentTable)) {
        tableUri = studio.getTableUri();
    } else {
        tableUri = request.getParameter("uri");
    }
    if (StringUtils.isNotBlank(tableUri)) {
        String nodeToOpen = studio.getModel().getTreeNodeId(tableUri);
        if (StringUtils.isNotBlank(nodeToOpen)) {
            leftFramePageToOpen += "?nodeToOpen=" + nodeToOpen;
        }
        String tableParams = WebTool.listRequestParams(request, new String[]{ "uri" });
        tableParams += ((tableParams.isEmpty() ? "" : "&") + "uri=" + StringTool.encodeURL(tableUri));
        mainFramePageToOpen = TABLE_PAGE + "?" + tableParams;
    }
%>    

<script type="text/javascript">
    window.parent.frames.leftFrame.location.href = "<%=leftFramePageToOpen%>";
    window.parent.frames.mainFrame.location.href = "<%=mainFramePageToOpen%>";
</script>
