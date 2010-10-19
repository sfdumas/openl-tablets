package org.openl.rules.dt;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.xls.XlsSheetGridHelper;

public class DecisionTableHelper {
    
    /**
     * Check if table is vertical.<br>
     * Vertical table is when conditions are represented from left to right, table is reading from top to bottom.</br> 
     * Example of vertical table:
     * 
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff"><b>Rule</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C2</b></td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">paramLocal1==paramInc</td>
     * <td align="center" bgcolor="#ccffff">paramLocal2==paramInc</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
     * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule</td>
     * <td align="center" bgcolor="#ffff99">Local Param 1</td>
     * <td align="center" bgcolor="#ffff99">Local Param 2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule1</td>
     * <td align="center" bgcolor="#ffff99">value11</td>
     * <td align="center" bgcolor="#ffff99">value21</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule2</td>
     * <td align="center" bgcolor="#ffff99">value12</td>
     * <td align="center" bgcolor="#ffff99">value22</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule3</td>
     * <td align="center" bgcolor="#ffff99">value13</td>
     * <td align="center" bgcolor="#ffff99">value23</td>
     * </tr>
     * </table>
     * 
     * @param table     
     * @return <code>TRUE</code> if table is vertical.
     */
    public static boolean looksLikeVertical(ILogicalTable table) {

        if (table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getHeight() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }

        return table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
    }
    
    public static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) 
            && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(DecisionTableColumnHeaders.RETURN.getHeaderKey())
            && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    public static boolean isValidCommentHeader(String s) {
        return s.startsWith("//");
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s);
    }

    public static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isActionHeader(value) ? 1 : 0;
            }
        }

        return count;
    }
    
    /**
     * Checks if given table contain any horizontal condition header.
     * 
     * @param table
     * @return true if there is is any horizontal condition header in the table.
     */
    public static boolean hasHConditions(ILogicalTable table) {

        int width = table.getWidth();

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidHConditionHeader(value)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character.isDigit(headerStr.charAt(2));
    }
    
    /**
     * Creates virtual headers for condition and return columns to load simple
     * Decision Table as an usual Decision Table
     * 
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @param numberOfHcondition
     * @return prepared usual Decision Table.
     */
    public static ILogicalTable preprocessSimpleDecisionTable(DecisionTable decisionTable, ILogicalTable originalTable,
            int numberOfHcondition) {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeVirtualHeadersForSimpleDecisionTable(virtualGrid, originalTable, decisionTable, numberOfHcondition);
        GridTable virtualGridTable = new GridTable(0, 0, IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1, originalTable.getSource().getWidth() - 1, virtualGrid);
        IGrid grid = new CompositeGrid(new IGridTable[] { virtualGridTable, originalTable.getSource() }, true);
        return LogicalTableHelper.logicalTable(new GridTable(0, 0, originalTable.getSource().getHeight()
                + IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1, originalTable.getSource().getWidth() - 1, grid));
    }
    
    private static void writeVirtualHeadersForSimpleDecisionTable(IWritableGrid grid, ILogicalTable originalTable, DecisionTable decisionTable,
            int numberOfHcondition) {
        int numberOfConditions = decisionTable.getSignature().getNumberOfParameters();
        int column = 0;
        for (int i = 0; i < numberOfConditions; i++) {
            //write headers
            if (i < numberOfConditions - numberOfHcondition) {
                grid.setCellValue(column, 0, "C" + (i + 1));
            } else {
                grid.setCellValue(column, 0, "HC" + (i + 1));
            }
            grid.setCellValue(column, 1, decisionTable.getSignature().getParameterName(i));
            
            //merge columns
            int mergedColumnsCounts = originalTable.getColumnWidth(i);
            if (mergedColumnsCounts > 1) {
                for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                    grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
                }
            }
            column += mergedColumnsCounts;
        }
        grid.setCellValue(column, 0, "RET1");
        int mergedColumnsCounts = originalTable.getColumnWidth(numberOfConditions);
        if (mergedColumnsCounts > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
            }
        }
    }
    /**
     * Creates not-existing virtual grid.
     * 
     * @return virtual {@link IWritableGrid}.
     */
    public static IWritableGrid createVirtualGrid() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        return XlsSheetGridHelper.createVirtualGrid(sheet);
    }

    public static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        if (IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        if (IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType)) {
            return true;
        } else {
            return false;
        }
    }
}
