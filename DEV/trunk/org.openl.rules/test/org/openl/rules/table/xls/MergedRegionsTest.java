package org.openl.rules.table.xls;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.Test;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.actions.IUndoableGridTableAction;
import org.openl.rules.table.xls.XlsCell;
import org.openl.source.impl.FileSourceCodeModule;

import static org.junit.Assert.*;

/**
 * Tests correctness of resizing and moving merged regions during
 * removing/inserting of columns and rows.
 * 
 * Test format: see in "MergedRegions.xls"
 * 
 * @author PUdalau
 */
public class MergedRegionsTest {

    /**
     * Description of test case: region to do action(insert or remove), region
     * with expected result, first row/column to do action, number of
     * rows/columns
     */
    private static class TestDesctiption {
        private static final String testDescriptionFormat = "test=.+&result=.+&original=.+&from=\\d+&count=\\d+";
        private static Pattern testDescriptionPattern = Pattern.compile(testDescriptionFormat);

        private IGridRegion testRegion;
        private IGridRegion expectedResultRegion;
        private IGridRegion originalTableRegion;
        private int from;
        private int count;

        public IGridRegion getTestRegion() {
            return testRegion;
        }

        public IGridRegion getExpectedResultRegion() {
            return expectedResultRegion;
        }

        public IGridRegion getOriginalTableRegion() {
            return originalTableRegion;
        }

        public int getFrom() {
            return from;
        }

        public int getCount() {
            return count;
        }

        private static boolean isTestDescriptionString(String descriptionString) {

            return descriptionString != null && testDescriptionPattern.matcher(descriptionString).matches();
        }

        public static TestDesctiption parse(String descriptionString) {
            TestDesctiption test = new TestDesctiption();
            StringTokenizer tokenizer = new StringTokenizer(descriptionString, "&");
            while (tokenizer.hasMoreElements()) {
                String param = (String) tokenizer.nextElement();
                int index = param.indexOf('=');
                String key = param.substring(0, index);
                String value = param.substring(index + 1);
                if ("test".equals(key)) {
                    test.testRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("result".equals(key)) {
                    test.expectedResultRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("original".equals(key)) {
                    test.originalTableRegion = new XlsGridRegion(CellRangeAddress.valueOf(value));
                } else if ("from".equals(key)) {
                    test.from = Integer.valueOf(value);
                } else if ("count".equals(key)) {
                    test.count = Integer.valueOf(value);
                }
            }
            return test;
        }
    }

    /**
     * Service exception.
     * 
     * Signals that difference between result cell and expected cell has been
     * detected.
     * 
     */
    private static class DifferentCellsException extends Exception {
        private static final long serialVersionUID = 1L;
        private ICell resultCell;
        private ICell expectedCell;

        public ICell getResultCell() {
            return resultCell;
        }

        public ICell getExpectedCell() {
            return expectedCell;
        }

        public DifferentCellsException(ICell resultCell, ICell expectedCell) {
            this.resultCell = resultCell;
            this.expectedCell = expectedCell;
        }

    }

    private static String __src = "test/rules/MergedRegions.xls";
    private static boolean saveAfterFailure = false;

    private List<TestDesctiption> findAllTests(IWritableGrid grid) {
        List<TestDesctiption> result = new ArrayList<TestDesctiption>();
        for (int row = 0; row <= grid.getMaxRowIndex(); row++) {
            for (int column = 0; column <= grid.getMaxColumnIndex(row); column++) {
                ICell cell = grid.getCell(column, row);
                if (cell != null) {
                    String descriptionString = cell.getStringValue();
                    if (TestDesctiption.isTestDescriptionString(descriptionString)) {
                        result.add(TestDesctiption.parse(descriptionString));
                    }
                }
            }
        }
        return result;
    }

    private void compareTablesByCell(IGridRegion testRegion, IGridRegion expectedRegion, XlsSheetGridModel grid)
            throws DifferentCellsException {
        int height = Math.max(IGridRegion.Tool.height(testRegion), IGridRegion.Tool.height(expectedRegion));
        int width = Math.max(IGridRegion.Tool.width(testRegion), IGridRegion.Tool.width(expectedRegion));
        for (int row = 0; row <= height; row++) {
            for (int column = 0; column <= width; column++) {
                XlsCell resultCell = (XlsCell) grid.getCell(testRegion.getLeft() + column, testRegion.getTop() + row);
                XlsCell expectedCell = (XlsCell) grid.getCell(expectedRegion.getLeft() + column, expectedRegion
                        .getTop()
                        + row);
                Cell resultXLSCell = PoiExcelHelper.getOrCreateCell(testRegion.getLeft() + column, testRegion.getTop()
                        + row, grid.getSheetSource().getSheet());
                Cell expectedXLSCell = PoiExcelHelper.getOrCreateCell(expectedRegion.getLeft() + column, expectedRegion
                        .getTop()
                        + row, grid.getSheetSource().getSheet());
                if (resultCell != expectedCell
                        && !StringUtils.equals(resultCell.getStringValue(), expectedCell.getStringValue())) {
                    // non top left cells of merged regions will be skipped in
                    // comparing by POI due to the second check
                    // TODO:remove the second check when the bug with non empty
                    // cells in merged regions will be resolved
                    if (!isEqualCells(resultCell, expectedCell, grid)
                            || !isEqualCellsInPOI(resultXLSCell, expectedXLSCell)) {
                        throw new DifferentCellsException(resultCell, expectedCell);
                    }
                }
            }
        }
        assertTrue(true);
    }

    private boolean isEqualCells(ICell first, ICell second, XlsSheetGridModel grid) {
        if (grid.isPartOfTheMergedRegion(first.getAbsoluteColumn(), first.getAbsoluteRow()) != grid
                .isPartOfTheMergedRegion(second.getAbsoluteColumn(), second.getAbsoluteRow())) {
            return false;
        }
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        String firstValue = first.getStringValue();
        String secondValue = second.getStringValue();
        if (firstValue != null) {
            return firstValue.equals(secondValue);
        } else if (secondValue != null) {
            return secondValue.equals(firstValue);
        }
        return firstValue == secondValue;
    }

    private boolean isEqualCellsInPOI(Cell first, Cell second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        if (first.getCellType() != second.getCellType()) {
            return false;
        }
        Object firstValue = extractCellValue(first);
        Object secondValue = extractCellValue(second);
        if (firstValue != null) {
            return firstValue.equals(secondValue);
        } else if (secondValue != null) {
            return secondValue.equals(firstValue);
        }
        return firstValue == secondValue;
    }

    private Object extractCellValue(Cell cell) {
        int type = cell.getCellType();
        switch (type) {
            case Cell.CELL_TYPE_BLANK:
                return null;
            case Cell.CELL_TYPE_BOOLEAN:
                return Boolean.valueOf(cell.getBooleanCellValue());
            case Cell.CELL_TYPE_NUMERIC:
                return cell.getNumericCellValue();
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case Cell.CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            default:
                return "unknown type: " + cell.getCellType();
        }
    }

    private void testActions(XlsWorkbookSourceCodeModule workbook, XlsSheetGridModel grid, IGridTable table,
            TestDesctiption test, IUndoableGridTableAction removeRowsActions) {
        try {
            removeRowsActions.doAction(table);
            compareTablesByCell(test.getTestRegion(), test.getExpectedResultRegion(), grid);
            removeRowsActions.undoAction(table);
            compareTablesByCell(test.getTestRegion(), test.getOriginalTableRegion(), grid);
        } catch (DifferentCellsException e) {
            if (saveAfterFailure) {
                try {
                    workbook.saveAs(String.format("test/rules/MergedRegionsAfter%s.xls", grid.getSheetSource()
                            .getSheetName()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            assertFalse("Different cells:\n" + e.getResultCell().getUri() + "\n and \n" + e.getExpectedCell().getUri(),
                    true);
        }
    }

    @Test
    public void testDeleteRows() {
        XlsWorkbookSourceCodeModule workbook = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(__src, null));
        XlsSheetSourceCodeModule sheet = new XlsSheetSourceCodeModule(workbook.getWorkbook().getSheet("DeleteRows"),
                "DeleteRows", workbook);
        XlsSheetGridModel grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(7, tests.size());
        IGridTable table = grid.getTables()[0];
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction removeRowsAction = IWritableGrid.Tool.removeRows(test.getCount(), test.getFrom(),
                    test.getTestRegion(), table);
            testActions(workbook, grid, table, test, removeRowsAction);
        }
    }

    @Test
    public void testInsertRows() {
        XlsWorkbookSourceCodeModule workbook = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(__src, null));
        XlsSheetSourceCodeModule sheet = new XlsSheetSourceCodeModule(workbook.getWorkbook().getSheet("InsertRows"),
                "InsertRows", workbook);
        XlsSheetGridModel grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(7, tests.size());
        IGridTable table = grid.getTables()[0];
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction insertRowsAction = IWritableGrid.Tool.insertRows(test.getCount(), test.getFrom(),
                    test.getTestRegion(), table);
            testActions(workbook, grid, table, test, insertRowsAction);
        }
    }

    @Test
    public void testDeleteColumns() {
        XlsWorkbookSourceCodeModule workbook = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(__src, null));
        XlsSheetSourceCodeModule sheet = new XlsSheetSourceCodeModule(workbook.getWorkbook().getSheet("DeleteColumns"),
                "DeleteColumns", workbook);
        XlsSheetGridModel grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(6, tests.size());
        IGridTable table = grid.getTables()[0];
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction removeColumnsAction = IWritableGrid.Tool.removeColumns(test.getCount(), test
                    .getFrom(), test.getTestRegion(), table);
            testActions(workbook, grid, table, test, removeColumnsAction);
        }
    }

    @Test
    public void testInsertColumn() {
        XlsWorkbookSourceCodeModule workbook = new XlsWorkbookSourceCodeModule(new FileSourceCodeModule(__src, null));
        XlsSheetSourceCodeModule sheet = new XlsSheetSourceCodeModule(workbook.getWorkbook().getSheet("InsertColumns"),
                "InsertColumns", workbook);
        XlsSheetGridModel grid = new XlsSheetGridModel(sheet);
        List<TestDesctiption> tests = findAllTests(grid);
        assertEquals(7, tests.size());
        IGridTable table = grid.getTables()[0];
        for (TestDesctiption test : tests) {
            IUndoableGridTableAction insertColumnsAction = IWritableGrid.Tool.insertColumns(test.getCount(), test
                    .getFrom(), test.getTestRegion(), table);
            testActions(workbook, grid, table, test, insertColumnsAction);
        }
    }
}
