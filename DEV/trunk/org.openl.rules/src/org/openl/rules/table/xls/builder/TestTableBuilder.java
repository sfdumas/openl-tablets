package org.openl.rules.table.xls.builder;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.testmethod.TestMethodHelper;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenMember;

/**
 * The class is responsible for creating test method tables in excel sheets.
 * Given all necessary data (parameter names and titles, result column
 * description, table being tested and testmethod name) it just creates a new
 * table in the given sheet.
 * 
 * @author Aliaksandr Antonik
 * @author Andrei Astrouski
 */
public class TestTableBuilder extends TableBuilder {

    /** Default result parameter title. */
    private static final String RESULT_PARAM_TITLE = "Result";

    /** Test method name postfix */
    private static final String TESTMETHOD_NAME_POSTFIX = "Test";

    /**
     * Creates new instance.
     * 
     * @param gridModel represents interface for operations with excel sheets
     */
    public TestTableBuilder(XlsSheetGridModel gridModel) {
        super(gridModel);
    }

    /**
     * Writes test table parameters.
     * 
     * @param params test table parameters
     * @param resultTitle result parameter title
     * 
     * @throws IllegalArgumentException if params is null
     * @throws IllegalStateException if method is called without prior
     *             <code>beginTable()</code> call
     */
    public void writeParams(Map<String, String> params, String resultTitle) {
        if (params == null) {
            throw new IllegalArgumentException("params must be not null");
        }
        if (getTableRegion() == null) {
            throw new IllegalStateException("beginTable() has to be called");
        }
        if (!params.containsKey(TestMethodHelper.EXPECTED_RESULT_NAME)) {
            params.put(TestMethodHelper.EXPECTED_RESULT_NAME, StringUtils.isBlank(resultTitle) ? RESULT_PARAM_TITLE
                    : resultTitle);
        }
        int column = 0;
        Set<String> names = params.keySet();
        for (Iterator<String> iterator = names.iterator(); iterator.hasNext();) {
            String name = iterator.next();
            writeCell(column, getCurrentRow(), 1, 1, name);
            String title = params.get(name);
            writeCell(column, getCurrentRow() + 1, 1, 1, title);
            column++;
        }
        if (!params.isEmpty()) {
            incCurrentRow(2);
        }
    }

    /**
     * Returns table header.
     * 
     * @param decisionTableNode Decision table node
     * @return table header
     */
    public static String getHeader(TableSyntaxNode decisionTableNode) {
        DecisionTable decisionTable = getDecisionTable(decisionTableNode);
        if (decisionTable != null) {
            String tableName = decisionTable.getName();
            return IXlsTableNames.TEST_METHOD_TABLE + " " + tableName + " " + tableName + TESTMETHOD_NAME_POSTFIX;
        }
        return null;
    }

    /**
     * Returns table parameters.
     * 
     * @param decisionTableNode Decision table node
     * @return table parameters
     */
    public static Map<String, String> getParams(TableSyntaxNode decisionTableNode) {
        DecisionTable decisionTable = getDecisionTable(decisionTableNode);
        if (decisionTable != null) {
            Map<String, String> params = new LinkedHashMap<String, String>();
            IMethodSignature tableHeaderSignature = decisionTable.getHeader().getSignature();
            for (int i = 0; i < tableHeaderSignature.getNumberOfArguments(); i++) {
                String paramName = tableHeaderSignature.getParameterName(i);
                params.put(paramName, id2title(paramName));
            }
            return params;
        }
        return null;
    }

    /**
     * Returns Decision table from node.
     * 
     * @param node Decision table node
     * @return Decision table
     */
    private static DecisionTable getDecisionTable(TableSyntaxNode node) {
        if (node == null) {
            throw new IllegalArgumentException("syntax node is null");
        }
        IOpenMember member = node.getMember();
        if (member != null) {
            if (!(member instanceof DecisionTable)) {
                throw new IllegalArgumentException("syntax node is not a decision table node");
            }
            return (DecisionTable) member;
        }
        return null;
    }

    private static String id2title(String id) {
        StringBuilder sb = new StringBuilder();
        boolean space = true;
        for (int i = 0; i < id.length(); ++i) {
            char c = id.charAt(i);
            if (Character.isWhitespace(c) || c == '_') {
                if (!space) {
                    space = true;
                    sb.append(" ");
                }
            } else if (space || Character.isUpperCase(c)) {
                space = false;
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        } else {
            return id;
        }
        return sb.toString().trim();
    }
}
