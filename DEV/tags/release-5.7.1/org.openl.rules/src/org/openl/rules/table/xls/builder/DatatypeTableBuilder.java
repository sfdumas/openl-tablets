package org.openl.rules.table.xls.builder;

import org.apache.commons.lang.StringUtils;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;

/**
 * The class is responsible for creating Datatype tables.
 *
 * @author Andrei Astrouski
 */
public class DatatypeTableBuilder extends TableBuilder {

    /**
     * Creates new instance.
     *
     * @param gridModel represents interface for operations with excel sheets
     */
    public DatatypeTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    public void beginTable(int height) throws CreateTableException {
        super.beginTable(2, height);
    }

    @Override
    public void writeHeader(String tableName, ICellStyle style) {
        String header = IXlsTableNames.DATATYPE_TABLE;
        if (StringUtils.isNotBlank(tableName)) {
            header += (" " + tableName);
        }
        super.writeHeader(header, style);
    }

    public void writeHeader(String tableName) {
        writeHeader(tableName, null);
    }

    public void writeParameter(String paramType, String paramName, ICellStyle cellStyle) {
        if (StringUtils.isBlank(paramType) || StringUtils.isBlank(paramName)) {
            throw new IllegalArgumentException("Parameter type and name must be not empty");
        }
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }

        writeCell(0, getCurrentRow(), 1, 1, paramType, cellStyle);
        writeCell(1, getCurrentRow(), 1, 1, paramName, cellStyle);

        incCurrentRow();
     }

     public void writeParameter(String paramType, String paramName) {
        writeParameter(paramType, paramName, null);
     }

}
