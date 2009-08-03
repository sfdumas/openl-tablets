package org.openl.rules.tableeditor.renderkit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ITable;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.model.ui.ActionLink;
import org.openl.rules.tableeditor.model.ui.CellModel;
import org.openl.rules.tableeditor.model.ui.ICellModel;
import org.openl.rules.tableeditor.model.ui.TableModel;
import org.openl.rules.tableeditor.util.Constants;
import org.openl.rules.tableeditor.util.WebUtil;
import org.openl.rules.web.jsf.util.FacesUtils;

/**
 * Render TableEditor HTML.
 *
 * @author Andrei Astrouski
 */
public class HTMLRenderer {

    /**
     * Render HTML table by table model.
     *
     * @author Andrey Naumenko
     */
    public static class TableRenderer {
        private final TableModel tableModel;
        private String cellIdPrefix;

        public TableRenderer(TableModel tableModel) {
            this.tableModel = tableModel;
        }

        public String render() {
            return render(null, false);
        }

        public String render(String extraTDText, boolean embedCellURI) {
            String tdPrefix = "<td";
            if (extraTDText != null) {
                tdPrefix += " ";
                tdPrefix += extraTDText;
            }
            final String prefix = cellIdPrefix != null ? cellIdPrefix : Constants.CELL_ID_POSTFIX;

            IGridTable table = tableModel.getGridTable();
            StringBuffer s = new StringBuffer();
            s.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\">\n");

            for (int i = 0; i < tableModel.getCells().length; i++) {
                s.append("<tr>\n");
                for (int j = 0; j < tableModel.getCells()[i].length; j++) {
                    ICellModel cell = tableModel.getCells()[i][j];
                    if ((cell == null) || !cell.isReal()) {
                        continue;
                    }

                    s.append(tdPrefix);
                    if (cell instanceof CellModel) {
                        ((CellModel) (cell)).atttributesToHtml(s, tableModel);
                    }

                    StringBuilder id = new StringBuilder();
                    id.append(prefix).append(String.valueOf(i + 1)).append(":").append(j + 1);

                    s.append(" id=\"").append(id).append("\">");
                    if (embedCellURI) {
                        s.append("<input name=\"uri\" type=\"hidden\" value=\"").append(table.getUri(j, i)).append(
                                "\"></input>");
                    }
                    String content = cell.getContent();
                    if (content != null) {
                        content.replaceAll("", "");
                    }
                    s.append(cell.getContent()).append("</td>\n");
                }
                s.append("</tr>\n");
            }
            s.append("</table>");
            return s.toString();
        }

        public String renderWithMenu(String menuId) {
            menuId = menuId == null ? "" : menuId;
            return render("onmouseover=\"openMenu('" + menuId + "',this,event)\" onmouseout=\"closeMenu(this)\"", true);
        }

        public void setCellIdPrefix(String prefix) {
            cellIdPrefix = prefix;
        }
    }

    /** New line */
    public static final String NL = "\n";

    @SuppressWarnings("unchecked")
    protected Set getResourcesWritten() {
        Map requestMap = FacesUtils.getRequestMap();
        Set resources = (Set) requestMap.get(Constants.TABLE_EDITOR_RESOURCES);
        if (resources == null) {
            resources = new HashSet();
            requestMap.put(Constants.TABLE_EDITOR_RESOURCES, resources);
        }
        return resources;
    }

    public String render(ITable table, String view, String editorId, IGridFilter filter) {
        return render(null, table, view, null, false, null, false, editorId, filter);
    }

    public String render(String mode, ITable table, String view, List<ActionLink> actionLinks, boolean editable,
            String cellToEdit, boolean inner, String editorId, IGridFilter filter) {
        StringBuilder result = new StringBuilder();
        result.append("<div>").append(renderCSS("css/common.css")).append(renderCSS("css/menu.css")).append(
                renderCSS("css/toolbar.css")).append(renderJS("js/prototype/prototype-1.5.1.js")).append(
                renderJS("js/ScriptLoader.js")).append(renderJS("js/AjaxHelper.js")).append(
                renderJS("js/IconManager.js")).append(renderJS("js/TableEditor.js")).append(
                renderJS("js/initTableEditor.js")).append(renderJS("js/BaseEditor.js")).append(
                renderJS("js/BaseTextEditor.js"));
        if (!inner) {
            result.append("<div id='").append(editorId).append("' class='te_'>");
        }
        if (mode == null || mode.equals(Constants.MODE_VIEW)) {
            result.append(renderViewer(table, view, actionLinks, editable, editorId, filter));
        } else if (mode.equals(Constants.MODE_EDIT)) {
            result.append(renderEditor(editorId, cellToEdit, table));
        }
        if (!inner) {
            result.append("</div>");
        }
        result.append("</div>");
        return result.toString();
    }

    protected String renderActionMenu(String menuId, boolean editable, List<ActionLink> actionLinks) {
        StringBuilder result = new StringBuilder();

        String editLinks = "<tr><td><a href=\"javascript:triggerEdit('"
                + menuId.replaceFirst(Constants.MENU_ID_POSTFIX, "") + "','" + WebUtil.internalPath("ajax/edit")
                + "')\">Edit</a></td></tr>" + "<tr><td><a href=\"javascript:triggerEditXls('"
                + WebUtil.internalPath("ajax/editXls") + "')\">Edit in Excel</a></td></tr>";
        String menuBegin = "<div id=\"" + menuId + "\" style=\"display:none;\">" + "<table cellpadding=\"1px\">"
                + (editable ? editLinks : "");
        String menuEnd = "</table>" + "</div>";

        result.append(menuBegin).append(actionLinks == null ? "" : renderAddActionLinks(actionLinks)).append(menuEnd);

        return result.toString();
    }

    protected String renderAddActionLinks(List<ActionLink> links) {
        if (links == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();

        for (ActionLink link : links) {
            result.append("<tr><td>").append("<a href=\"").append(link.getAction()).append("\">")
                    .append(link.getName()).append("</a>").append("</td></tr>");
        }

        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public String renderCSS(String cssPath) {
        Set resources = getResourcesWritten();
        if (resources.add(cssPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(WebUtil.internalPath(cssPath))
                    .append("\"></link>");
            return result.toString();
        }
        return "";
    }

    protected String renderEditor(String editorId, String cellToEdit, ITable table) {
        StringBuilder result = new StringBuilder();
        cellToEdit = cellToEdit == null ? "" : cellToEdit;
        final String tableId = editorId + Constants.TABLE_ID_POSTFIX;
        String editor = Constants.TABLE_EDITOR_PREFIX + editorId;
        result.append(renderJSBody("var " + editor + ";"))
        // .append(renderJSBody("var jsPath = \"" + WebUtil.internalPath("js/")
        // + "\""))
                .append(renderEditorToolbar(editorId, editor)).append(renderJS("js/TextEditor.js")).append(
                        renderJS("js/MultiLineEditor.js")).append(renderJS("js/NumericEditor.js")).append(
                        renderJS("js/DropdownEditor.js"))
                .append(renderPropsEditor(table, Constants.MODE_EDIT))
                // .append(renderJS("js/SuggestEditor.js"))
                // .append(renderJS("js/DateEditor.js"))
                // .append(renderJS("js/PriceEditor.js"))
                // .append(renderJS("js/MultipleChoiceEditor.js"))
                .append("<div id=\"").append(tableId).append("\"></div>").append(
                        renderJSBody(editor + " = initTableEditor(\"" + editorId + "\", \""
                                + WebUtil.internalPath("ajax/") + "\",\"" + cellToEdit + "\");"));
        return result.toString();
    }

    protected String renderEditorToolbar(String editorId, String editor) {
        StringBuilder result = new StringBuilder();

        final String toolbarItemSeparator = "<img src=" + WebUtil.internalPath("img/toolbarSeparator.gif")
                + " class=\"item_separator\"></img>";

        result.append("<div class=\"te_toolbar\">").append(
                renderEditorToolbarItem(editorId + "_save_all", editor, "img/Save.gif", "save()", "Save")).append(
                renderEditorToolbarItem(editorId + "_undo", editor, "img/Undo.gif", "undoredo()", "Undo")).append(
                renderEditorToolbarItem(editorId + "_redo", editor, "img/Redo.gif", "undoredo(true)", "Redo")).append(
                toolbarItemSeparator).append(
                renderEditorToolbarItem(editorId + "_add_row_before", editor, "img/insert_row.gif",
                        "doRowOperation(TableEditor.Constants.ADD_BEFORE)", "Add row")).append(
                renderEditorToolbarItem(editorId + "_remove_row", editor, "img/delete_row.gif",
                        "doRowOperation(TableEditor.Constants.REMOVE)", "Remove row"))
        // .append(renderEditorToolbarItem(editorId + "_move_row_down", editor,
        // "img/b_row_ins.gif",
                // "doRowOperation(TableEditor.Constants.MOVE_DOWN)", "Move row
                // down"))
                // .append(renderEditorToolbarItem(editorId + "_move_row_up",
                // editor, "img/b_row_ins.gif",
                // "doRowOperation(TableEditor.Constants.MOVE_UP)", "Move row
                // up"))
                .append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_add_column_before", editor, "img/insert_column.gif",
                                "doColOperation(TableEditor.Constants.ADD_BEFORE)", "Add column")).append(
                        renderEditorToolbarItem(editorId + "_remove_column", editor, "img/delete_column.gif",
                                "doColOperation(TableEditor.Constants.REMOVE)", "Remove column"))
                // .append(renderEditorToolbarItem(editorId +
                // "_move_column_right", editor, "img/b_row_ins.gif",
                // "doColOperation(TableEditor.Constants.MOVE_DOWN)", "Move
                // column right"))
                // .append(renderEditorToolbarItem(editorId +
                // "_move_column_left", editor, "img/b_row_ins.gif",
                // "doColOperation(TableEditor.Constants.MOVE_UP)", "Move column
                // left"))
                .append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_align_left", editor, "img/alLeft.gif",
                                "setAlignment('left')", "Align left")).append(
                        renderEditorToolbarItem(editorId + "_align_center", editor, "img/alCenter.gif",
                                "setAlignment('center')", "Align center")).append(
                        renderEditorToolbarItem(editorId + "_align_right", editor, "img/alRight.gif",
                                "setAlignment('right')", "Align right")).append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_decrease_indent", editor, "img/indent_left.gif",
                                "indent('-1')", "Decrease indent")).append(
                        renderEditorToolbarItem(editorId + "_increase_indent", editor, "img/indent_right.gif",
                                "indent('1')", "Increase indent")).append(toolbarItemSeparator).append(
                        renderEditorToolbarItem(editorId + "_help", null, "img/help.gif", "window.open('"
                                + WebUtil.internalPath("docs/help.html") + "');", "Help")).append("</div>");

        return result.toString();
    }

    protected String renderEditorToolbarItem(String itemId, String editor, String imgSrc, String action, String title) {
        editor = (editor == null || editor.equals("")) ? "" : editor + ".";
        StringBuilder result = new StringBuilder();
        result.append("<img id=\"").append(itemId).append("\" src=\"").append(WebUtil.internalPath(imgSrc)).append(
                "\" title=\"").append(title).append("\" onclick=\"").append(editor).append(action).append(
                "\" onmouseover=\"this.className='item_over'\"")
                .append(" onmouseout=\"this.className='item_enabled'\"").append("></img>");
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    public String renderJS(String jsPath) {
        Set resources = getResourcesWritten();
        if (resources.add(jsPath)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\" src=\"").append(WebUtil.internalPath(jsPath)).append(
                    "\"></script>");
            return result.toString();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public String renderJSBody(String jsBody) {
        Set resources = getResourcesWritten();
        if (resources.add(jsBody)) {
            StringBuilder result = new StringBuilder();
            result.append("<script type=\"text/javascript\">").append(jsBody).append("</script>");
            return result.toString();
        }
        return "";
    }

    protected String renderViewer(ITable table, String view, List<ActionLink> actionLinks, boolean editable,
            String editorId, IGridFilter filter) {
        StringBuilder result = new StringBuilder();
        if (table != null) {
            result.append(renderPropsEditor(table, Constants.MODE_VIEW));
        }
        if (table != null) {
            IGridFilter[] filters = (filter == null) ? null : new IGridFilter[] { filter };
            TableModel tableModel = TableModel.initializeTableModel(new TableEditorModel(
                    table, view).getUpdatedTable(), filters);
            if (tableModel != null) {
                String menuId = editorId + Constants.MENU_ID_POSTFIX;
                TableRenderer tableRenderer = new TableRenderer(tableModel);
                tableRenderer.setCellIdPrefix(editorId + Constants.CELL_ID_POSTFIX);
                if (editable || (actionLinks != null && !actionLinks.isEmpty())) {
                    result.append(renderJS("js/popup/popupmenu.js")).append(renderJS("js/tableEditorMenu.js")).append(
                            tableRenderer.renderWithMenu(menuId)).append(
                            renderActionMenu(menuId, editable, actionLinks));
                } else {
                    result.append(tableRenderer.render());
                }
            }
        }
        return result.toString();
    }

    protected String renderPropsEditor(ITable table, String mode) {
        TableProperties props = table.getProperties();        
        return new PropertyRenderer(props, mode).renderProperties();
    }   

    /**
     * Temporary class to render properties on edit view
     * @author DLiauchuk
     *
     */
    public class PropertyRenderer {
                
        private List<TableProperty> listProperties = new ArrayList<TableProperty>();
        
        private int numberOfCalendars = 0;
        
        private StringBuilder result;

        private String mode;

        private TableProperties props;

        public PropertyRenderer(TableProperties props, String view) {
            if (props == null) {
                throw new IllegalArgumentException();
            }
            this.props = props;
            this.mode = view;
            this.listProperties = initPropertiesList();
            this.result = new StringBuilder();
        }

        private List<TableProperty> initPropertiesList() {
            List<TableProperty> listProp = new ArrayList<TableProperty>();
            TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
            for (TablePropertyDefinition propDefinition : propDefinitions) {
                listProp.add(new TableProperty(propDefinition.getDisplayName(),
                        props.getPropertyValue(propDefinition.getName()),
                        propDefinition.getType() == null ? null : propDefinition.getType().getInstanceClass(),
                        propDefinition.getGroup(), propDefinition.getName()));
            }
            return listProp;
        }

        private Map<String, List<TableProperty>> groupProps(List<TableProperty> props) {
            Map<String, List<TableProperty>> groupProps = new LinkedHashMap<String, List<TableProperty>>();
            for (TableProperty prop : props) {
                String group = prop.getGroup();
                List<TableProperty> groupList = groupProps.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<TableProperty>();
                    groupProps.put(group, groupList);
                }
                if (!groupList.contains(prop)) {
                    groupList.add(prop);
                }
            }
            return groupProps;
        }
        
        /**
         * 
         * @return Construct table for properties
         */
        public String renderProperties() {
            result.append("<fieldset style='width:600px;margin-bottom:5px;'><legend>Properties</legend>"
                    + "<table style='font-size: 12px;font-family: Arial;' cellspacing='1' cellpadding='1'>");
            result.append(renderJS("js/calendar_us.js"));
            result.append(renderCSS("css/calendar.css"));

            buildPropsTable();

            result.append("</table></fieldset>");
            return result.toString();            
        }

        private void buildPropsTable() {
            Map<String, List<TableProperty>> groupProps = groupProps(listProperties);
            int groupSize = groupProps.size();
            int numToDivide = getNumToDivideColumns(groupSize);

            int i = 0;
            Set<String> groupKeys = groupProps.keySet();
            for (String group : groupKeys) {
                if (i == 0) {
                    result.append("<tr>");
                }
                if (i == numToDivide) {
                    result.append("</tr>");
                    result.append("<tr>");
                }
                result.append("<td valign='top'>");
                fillPropsGroup(group, mode);
                result.append("</td>");

                if (i == groupSize - 1) {
                    result.append("</tr>");
                }
                i++;
            }
        }

        private int getNumToDivideColumns(int groupSize) {
            int numToDivide = 0;
            if (groupSize %2 == 0) {
                numToDivide = groupSize / 2;
            } else {
                numToDivide = (groupSize + 1) / 2;
            }
            return numToDivide;
        }

        private void fillPropsGroup(String groupKey, String mode) {
            Map<String, List<TableProperty>> groupProps = groupProps(listProperties);
            List<TableProperty> groupList = groupProps.get(groupKey);
            String groupId = groupKey + "_group";
            result.append("<div style='margin-bottom:5px;'>");
            result.append("<b>" + groupKey + "</b>");
            renderHideButton(groupId);
            result.append("</div>");
            result.append("<div id='" + groupId + "' style='border:1px solid grey; margin: 0 4px 8px 0;'>");
            result.append("<table>");
            for (TableProperty prop : groupList) {
                if (mode.equals("edit")) {
                    fillEditProp(prop);
                } else {
                    fillViewProp(prop);
                }
            }
            result.append("</table></div>");
        }

        private void renderHideButton(String idToHide) {
            result.append("<input type='button' value='-' onclick=\"$('"
                    + idToHide + "').toggle();this.value=(this.value == '-' ? '+' : '-');"
                    + "this.title=(this.title == 'Hide group' ? 'Show group' : 'Hide group');\""
                    + " title='Hide group'"
                    + " style='width:20px;height:15px;border:0px;background:none;' />");
        }

        /**
         * Fills the row in edit table, depending on the type of property value
         * @param prop
         */
        private void fillEditProp(TableProperty prop) {
            result.append("<tr>");
            insertLabel(prop.getDisplayName());
            Object propValue = prop.getValue();
            String propId = "_prop_" + prop.getName();
            if (prop.getType() != null) {
                if (prop.isStringValue()) {
                    insertEdit((String) propValue, propId);
                } else if (prop.isDateValue()) {
                    insertCalendar((Date) propValue, propId);
                } else if (prop.isBooleanValue()) {
                    insertCheckbox((Boolean) propValue, propId);
                }
            }
            result.append("</tr>");
        }

        /**
         * Fills the row in view table
         * @param prop
         */
        private void fillViewProp(TableProperty prop) {
            result.append("<tr>");
            insertLabel(prop.getDisplayName());
            insertText(prop);
        }

        private void insertCalendar(Date value, String name) {
            numberOfCalendars++;
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            String resultStr;
            if (value == null) {
                resultStr = "";
            } else {
                resultStr = sdf.format(value); 
            }
            result.append("<td><input name='" + name + "' type='text' value='" + resultStr + "' id='datepicker"
                    + numberOfCalendars +"' />")
                .append(renderJSBody("new tcal ({'controlname': 'datepicker" + numberOfCalendars + "'});"))
                .append("</td>");            
        }

        private void insertSelect(List<String> listOfOptions) {                        
            result.append("<td><select>");
            result.append("<option></option>");
            for(String option : listOfOptions) {
                result.append("<option>"+option+"</option>");
            }
            result.append("</select></td>");
        }

        private void insertEdit(String value, String name) {
            if(value==null) {
                value="";
            }
            result.append("<td><input name='" + name + "' type='text' value='" + value + "' /></td>");
        }

        private void insertCheckbox(Boolean value, String name) {
            if (value == null) {
                value = false;
            }
            result.append("<td><input name='" + name + "' type='checkbox' ").append(value ? "checked='checked'" : "").append(" /></td>");
        }

        private void insertLabel(String displayName) {
            result.append("<td><b>"+displayName+":</b></td>");
        }
        
        /**
         * 
         * @param value
         */
        private void insertText(TableProperty prop) {
            if(String.class.equals(prop.getType())) {
                String resString;
                if(prop.getValue()==null){
                    resString = "";
                } else {
                    resString = (String)prop.getValue();
                }
                result.append("<td>"+resString+"</td>");                    
            } else {
                if(Date.class.equals(prop.getType())) {
                    numberOfCalendars++;            
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
                    String resString;
                    if(prop.getValue()==null) {
                        resString = "";
                    } else {
                        resString = sdf.format((Date)prop.getValue());
                    }
                    result.append("<td>"+resString+"</td>");
                } else {
                    if(List.class.equals(prop.getType())) {
                        //result.append("<td><b>"+((List<String>)prop.getValue()).get(0)+"</b></td>");
                    }
                }
            }
        }
    }
}
