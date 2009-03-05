package org.openl.rules.calc;

import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class SCellField extends ASpreadsheetField {

    public SCellField(IOpenClass declaringClass, String name, SCell cell) {
        super(declaringClass, name, cell.getType());
        this.cell = cell;
    }

    SCell cell;

    @Override
    public Object calculate(SpreadsheetResult spreadsheetResult, Object targetModule, Object[] params, IRuntimeEnv env) {
        return cell.calculate(spreadsheetResult, targetModule, params, env);
    }

    @Override
    public IOpenClass getType() {
        return cell.getType();
    }

    public SCell getCell() {
        return cell;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        SpreadsheetResult res = (SpreadsheetResult) target;

        return res.getValue(cell.row, cell.column);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        throw new UnsupportedOperationException("Can not write to spreadsheet cell");
    }

    @Override
    public boolean isWritable() {
        return false;
    }

}
