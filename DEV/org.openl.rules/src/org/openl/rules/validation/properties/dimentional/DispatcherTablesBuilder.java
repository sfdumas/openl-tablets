package org.openl.rules.validation.properties.dimentional;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

/**
 * Builds dispatcher decision table for each
 * {@link MatchingOpenMethodDispatcher} in ModuleOpenClass.
 *
 * @author DLiauchuk
 *
 */
public class DispatcherTablesBuilder {

    public static final String DEFAULT_DISPATCHER_TABLE_NAME = "validateGapOverlap";

    /**
     * Checks whether the specified TableSyntaxNode is auto generated gap/overlap table or not.
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if table is dispatcher table.
     */
    public static boolean isDispatcherTable(TableSyntaxNode tsn) {
        IOpenMember member = tsn.getMember();
        if (member instanceof IOpenMethod) {
            return member.getName().startsWith(DEFAULT_DISPATCHER_TABLE_NAME);
        }
        return false;
    }

    private XlsModuleOpenClass moduleOpenClass;
    private RulesModuleBindingContext moduleContext;

    public DispatcherTablesBuilder(XlsModuleOpenClass moduleOpenClass,
            RulesModuleBindingContext moduleContext) {
        this.moduleContext = moduleContext;
        this.moduleOpenClass = moduleOpenClass;
    }

    /**
     * Builds dispatcher tables for every group of overloaded methods.
     * As a result new {@link TableSyntaxNode} objects appears in module.
     */
    public void build() {
        for (MatchingOpenMethodDispatcher dispatcher : getAllMethodDispatchers()) {
           build(dispatcher);
        }
    }

    public void build(MatchingOpenMethodDispatcher dispatcher) {
        Builder<TableSyntaxNode> tsnDispatcherBuilder = new TableSyntaxNodeDispatcherBuilder(moduleContext,
                moduleOpenClass, dispatcher);
        TableSyntaxNode tsn = tsnDispatcherBuilder.build();
        if (tsn != null) {
            if (!isVirtualWorkbook()) {
                addNewTsnToTopNode(tsn);
            } else {
                addNewTsnToCandidateModule(dispatcher, tsn);
            }
        }
    }

    private boolean isVirtualWorkbook(){
        return moduleOpenClass.getXlsMetaInfo().getXlsModuleNode().getModule() instanceof VirtualSourceCodeModule;
    }

    private void addNewTsnToTopNode(TableSyntaxNode tsn) {
        XlsMetaInfo xlsMetaInfo = moduleOpenClass.getXlsMetaInfo();
        xlsMetaInfo.getXlsModuleNode().getWorkbookSyntaxNodes()[0]
            .getWorksheetSyntaxNodes()[0].addNode(tsn);
    }

    private void addNewTsnToCandidateModule(MatchingOpenMethodDispatcher dispatcher, TableSyntaxNode tsn) {
        for (IOpenMethod method : dispatcher.getCandidates()) {
            if (method.getDeclaringClass() instanceof XlsModuleOpenClass) {
                XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) method.getDeclaringClass();
                XlsModuleSyntaxNode xlsModuleNode = xlsModuleOpenClass.getXlsMetaInfo().getXlsModuleNode();
                if (!(xlsModuleNode.getModule() instanceof VirtualSourceCodeModule)) {
                    xlsModuleNode.getWorkbookSyntaxNodes()[0].getWorksheetSyntaxNodes()[0].addNode(tsn);
                    break;
                }
            }
        }
    }

    private List<MatchingOpenMethodDispatcher> getAllMethodDispatchers(){
        List<MatchingOpenMethodDispatcher> dispatchers = new ArrayList<MatchingOpenMethodDispatcher>();
        for (IOpenMethod method : moduleOpenClass.getMethods()) {
            if (method instanceof MatchingOpenMethodDispatcher) {
                dispatchers.add((MatchingOpenMethodDispatcher) method);
            }
        }
        return dispatchers;
    }
}
