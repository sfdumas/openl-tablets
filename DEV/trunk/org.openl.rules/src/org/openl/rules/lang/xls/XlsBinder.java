/*
 * Created on Oct 2, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundNode;
import org.openl.binding.ICastFactory;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BoundCode;
import org.openl.binding.impl.module.ModuleNode;
import org.openl.conf.IExecutable;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.OpenLBuilderImpl;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenLCompilationException;
import org.openl.main.OpenLWrapper;
import org.openl.meta.IVocabulary;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.SpreadsheetNodeBinder;
import org.openl.rules.cmatch.ColumnMatchNodeBinder;
import org.openl.rules.data.DataBase;
import org.openl.rules.data.DataNodeBinder;
import org.openl.rules.data.IDataBase;
import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeNodeBinder;
import org.openl.rules.dt.DecisionTableNodeBinder;
import org.openl.rules.extension.bind.IExtensionBinder;
import org.openl.rules.extension.bind.NameConventionBinderFactory;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.method.table.MethodTableNodeBinder;
import org.openl.rules.property.PropertyTableBinder;
import org.openl.rules.table.properties.PropertiesLoader;
import org.openl.rules.tbasic.AlgorithmNodeBinder;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.validation.properties.dimentional.DispatcherTablesBuilder;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Implements {@link IOpenBinder} abstraction for Excel files.
 * 
 * @author snshor
 * 
 */
public class XlsBinder implements IOpenBinder {

    private final Log log = LogFactory.getLog(XlsBinder.class);
    private static Map<String, AXlsTableBinder> binderFactory;

    public static final String DEFAULT_OPENL_NAME = "org.openl.rules.java";
    
    private static final String[][] BINDERS = { { XlsNodeTypes.XLS_DATA.toString(), DataNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_DATATYPE.toString(), DatatypeNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_DT.toString(), DecisionTableNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_SPREADSHEET.toString(), SpreadsheetNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_METHOD.toString(), MethodTableNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_TEST_METHOD.toString(), TestMethodNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_RUN_METHOD.toString(), TestMethodNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_TBASIC.toString(), AlgorithmNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_COLUMN_MATCH.toString(), ColumnMatchNodeBinder.class.getName() },
            { XlsNodeTypes.XLS_PROPERTIES.toString(), PropertyTableBinder.class.getName() }, };

    public static synchronized Map<String, AXlsTableBinder> getBinderFactory() {

        if (binderFactory == null) {
            binderFactory = new HashMap<String, AXlsTableBinder>();

            for (int i = 0; i < BINDERS.length; i++) {

                try {
                    binderFactory.put(BINDERS[i][0], (AXlsTableBinder) Class.forName(BINDERS[i][1]).newInstance());
                } catch (Exception ex) {
                    throw RuntimeExceptionWrapper.wrap(ex);
                }
            }
        }

        return binderFactory;
    }

    private IUserContext userContext;

    public XlsBinder(IUserContext userContext) {
        this.userContext = userContext;
    }

    public ICastFactory getCastFactory() {
        return null;
    }

    public INameSpacedMethodFactory getMethodFactory() {
        return null;
    }

    public INodeBinderFactory getNodeBinderFactory() {
        return null;
    }

    public INameSpacedTypeFactory getTypeFactory() {
        return null;
    }

    public INameSpacedVarFactory getVarFactory() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.IOpenBinder#makeBindingContext()
     */
    public IBindingContext makeBindingContext() {
        throw new UnsupportedOperationException("XlsBinder is top level Binder");
    }

    public IBoundCode bind(IParsedCode parsedCode) {
        return bind(parsedCode, null);
    }

    public IBoundCode bind(IParsedCode parsedCode, IBindingContextDelegator bindingContextDelegator) {

        XlsModuleSyntaxNode moduleNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();

        OpenL openl = null;

        try {
            openl = makeOpenL(moduleNode);
        } catch (OpenConfigurationException ex) {

            OpenlSyntaxNode syntaxNode = moduleNode.getOpenlNode();

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Error Creating OpenL", ex, syntaxNode);
            BindHelper.processError(error);

            return BindHelper.makeInvalidCode(parsedCode, syntaxNode, new SyntaxNodeException[] { error });
        }

        IOpenBinder openlBinder = openl.getBinder();
        IBindingContext bindingContext = openlBinder.makeBindingContext();
        bindingContext = BindHelper.delegateContext(bindingContext, bindingContextDelegator);

        if (parsedCode.getExternalParams() != null) {
            bindingContext.setExternalParams(parsedCode.getExternalParams());
        }
        
        IBoundNode topNode = null;

        if (!parsedCode.getCompiledDependencies().isEmpty()) {
            topNode = bindWithDependencies(moduleNode, openl, bindingContext, parsedCode.getCompiledDependencies());
        } else {
            topNode = bind(moduleNode, openl, bindingContext);
        }

        return new BoundCode(parsedCode, topNode, bindingContext.getErrors(), 0);
    }
    
    
    /**
     * Bind module with processing dependent modules, previously compiled.<br>
     * Creates {@link XlsModuleOpenClass} with dependencies and<br>
     * populates {@link RulesModuleBindingContext} for current module with types<br>
     * from dependent modules.
     * 
     * @param moduleNode
     * @param openl
     * @param bindingContext
     * @param moduleDependencies
     * @return
     */
    private IBoundNode bindWithDependencies(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext bindingContext,
            Set<CompiledOpenClass> moduleDependencies) {
        XlsModuleOpenClass moduleOpenClass = createModuleOpenClass(moduleNode, openl, getModuleDatabase(), moduleDependencies);        
        
        RulesModuleBindingContext moduleContext = populateBindingContextWithDependencies(moduleNode, 
            bindingContext, moduleDependencies, moduleOpenClass);
        return processBinding(moduleNode, openl, moduleContext, moduleOpenClass);
    }
    
    protected IDataBase getModuleDatabase() {
		return new DataBase();
	}

	/**
     * Creates {@link RulesModuleBindingContext} and populates it with types from dependent modules.
     * 
     * @param moduleNode just for processing error
     * @param bindingContext 
     * @param moduleDependencies
     * @param moduleOpenClass
     * @return {@link RulesModuleBindingContext} created with bindingContext and moduleOpenClass.
     */
    private RulesModuleBindingContext populateBindingContextWithDependencies(XlsModuleSyntaxNode moduleNode, 
            IBindingContext bindingContext, Set<CompiledOpenClass> moduleDependencies,
            XlsModuleOpenClass moduleOpenClass) {
        RulesModuleBindingContext moduleContext = createRulesBindingContext(bindingContext, moduleOpenClass);
        for (CompiledOpenClass compiledDependency : moduleDependencies) {
            try {
                moduleContext.addTypes(filterDependencyTypes(compiledDependency.getTypes(), moduleContext.getInternalTypes()));
            } catch (Exception ex) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(
                        "Can`t add datatype from dependency", ex, (ISyntaxNode) moduleNode);
                BindHelper.processError(error);
            }
        }
        return moduleContext;
    }
    
    /**
     * Filter the datatypes from dependency, remove those that already presents in datatypes from context and are equal.
     * Handles the case when for example 'main' module includes 'dependency1'(with A, B datatypes) and 'dependency2' (with C datatype, and
     * 'dependency2' includes 'dependency1' itself). So to prevent adding datatypes A, B from 'dependency1' and 'dependency2' we handle this case.
     * 
     * @param dependencyDatatypes datatypes from dependency module
     * @param contextTypes datatypes already presented in the context
     * @return filtered dependency datatypes
     * 
     * @author DLiauchuk
     */
    private Map<String, IOpenClass> filterDependencyTypes(Map<String, IOpenClass> dependencyDatatypes, Map<String, IOpenClass> contextTypes) {
    	Map<String, IOpenClass> filteredDependencyDatatypes = new HashMap<String, IOpenClass>();
    	for (String key : dependencyDatatypes.keySet()) {
    		IOpenClass dependencyDatatype = dependencyDatatypes.get(key);
    		IOpenClass contextDatatype = contextTypes.get(key);
    		if (!dependencyDatatype.equals(contextDatatype)) {
    			filteredDependencyDatatypes.put(key, dependencyDatatype);
    		}
    	}
    	return filteredDependencyDatatypes;
    }

    public IBoundNode bind(XlsModuleSyntaxNode moduleNode, OpenL openl, IBindingContext bindingContext) {

        XlsModuleOpenClass moduleOpenClass = createModuleOpenClass(moduleNode, openl, getModuleDatabase(),  null);
        
        RulesModuleBindingContext moduleContext = createRulesBindingContext(bindingContext, moduleOpenClass);

        return processBinding(moduleNode, openl, moduleContext, moduleOpenClass);
    }
    
    /**
     * Common binding cycle.
     * 
     * @param moduleNode
     * @param openl
     * @param moduleContext
     * @param moduleOpenClass
     * @return
     */
    private IBoundNode processBinding(XlsModuleSyntaxNode moduleNode,
            OpenL openl,
            RulesModuleBindingContext moduleContext,
            XlsModuleOpenClass moduleOpenClass) {
    	
    	
//    	StopWatch.start("xlsbinding-"+moduleOpenClass.getName());
        processExtensions(moduleOpenClass, moduleNode, moduleNode.getExtensionNodes(), moduleContext);
        
        IVocabulary vocabulary = makeVocabulary(moduleNode);

        if (vocabulary != null) {
            processVocabulary(vocabulary, moduleContext);
        }

        ASelector<ISyntaxNode> propertiesSelector = new ASelector.StringValueSelector<ISyntaxNode>(
                XlsNodeTypes.XLS_PROPERTIES.toString(), new SyntaxNodeConvertor());

        // Bind property node at first.
        //
        TableSyntaxNode[] propertyNodes = getTableSyntaxNodes(moduleNode, propertiesSelector, null);
        bindInternal(moduleNode, moduleOpenClass, propertyNodes, openl, moduleContext);

        bindPropertiesForAllTables(moduleNode, moduleOpenClass, openl, moduleContext);
        
        // Bind datatype nodes.
        //
        ASelector<ISyntaxNode> dataTypeSelector = new ASelector.StringValueSelector<ISyntaxNode>(
                XlsNodeTypes.XLS_DATATYPE.toString(), new SyntaxNodeConvertor());
        TableSyntaxNode[] datatypeNodes = getTableSyntaxNodes(moduleNode, dataTypeSelector, null);
        TableSyntaxNode[] processedDatatypeNodes = processDatatypes(datatypeNodes, moduleContext);
        
        bindInternal(moduleNode, moduleOpenClass, processedDatatypeNodes, openl, moduleContext);
        
        // Bind other tables.
        //
        ISelector<ISyntaxNode> notPropertiesSelector = propertiesSelector.not();
        ISelector<ISyntaxNode> notDataTypeSelector = dataTypeSelector.not();

        ISelector<ISyntaxNode> notProp_And_NotDatatype = notDataTypeSelector.and(notPropertiesSelector);

        LinkedList<Comparator<TableSyntaxNode>> nodesComparator = getNodesComparator();        
        TableSyntaxNode[] otherNodes = getTableSyntaxNodes(moduleNode, notProp_And_NotDatatype, nodesComparator);        
        IBoundNode topNode = bindInternal(moduleNode, moduleOpenClass, otherNodes, openl, moduleContext);
        
        DispatcherTablesBuilder dispTableBuilder = new DispatcherTablesBuilder((XlsModuleOpenClass) topNode.getType(),
            moduleContext);
        dispTableBuilder.build();

//    	StopWatch.end("xlsbinding-"+moduleOpenClass.getName(), true);
        
        
        return topNode;
    }

    private LinkedList<Comparator<TableSyntaxNode>> getNodesComparator() {
        LinkedList<Comparator<TableSyntaxNode>> nodeComparator = new LinkedList<Comparator<TableSyntaxNode>>();
        if (OpenLSystemProperties.isCustomSpreadsheetType()) {
        	SpreadsheetNodeComparator spreadsheetComparator = new SpreadsheetNodeComparator();
            nodeComparator.add(spreadsheetComparator);
        }
        TestAndMethodTableNodeComparator testAndMethodComparator = new TestAndMethodTableNodeComparator();
        nodeComparator.add(testAndMethodComparator);
       
        return nodeComparator;
    }

    private RulesModuleBindingContext createRulesBindingContext(IBindingContext bindingContext,
            XlsModuleOpenClass moduleOpenClass) {
        return new RulesModuleBindingContext(bindingContext, moduleOpenClass);        
    }
    
    /**
     * Creates {@link XlsModuleOpenClass}
     * 
     * @param moduleNode
     * @param openl
     * @param dependencies set of dependent modules for creating module.
     * @return
     */
    protected XlsModuleOpenClass createModuleOpenClass(XlsModuleSyntaxNode moduleNode, OpenL openl, 
    	IDataBase dbase,	
        Set<CompiledOpenClass> moduleDependencies) {

        XlsModuleOpenClass module = null;
        if (moduleDependencies == null) {
            module = new XlsModuleOpenClass(null, XlsHelper.getModuleName(moduleNode), new XlsMetaInfo(moduleNode),
                openl, dbase);
        } else {
            module = new XlsModuleOpenClass(null, XlsHelper.getModuleName(moduleNode), new XlsMetaInfo(moduleNode),
                openl, dbase, moduleDependencies);
        }
        
        return module;
    }

    private void bindPropertiesForAllTables(XlsModuleSyntaxNode moduleNode, XlsModuleOpenClass module, OpenL openl, RulesModuleBindingContext bindingContext){
        ASelector<ISyntaxNode> propertiesSelector = new ASelector.StringValueSelector<ISyntaxNode>(
                XlsNodeTypes.XLS_PROPERTIES.toString(), new SyntaxNodeConvertor());
        ASelector<ISyntaxNode> otherNodesSelector = new ASelector.StringValueSelector<ISyntaxNode>(
                XlsNodeTypes.XLS_OTHER.toString(), new SyntaxNodeConvertor());
        ISelector<ISyntaxNode> notProperties_and_notOther_NodesSelector = propertiesSelector.not().and(otherNodesSelector.not());
 
        TableSyntaxNode[] tableSyntaxNodes = getTableSyntaxNodes(moduleNode, notProperties_and_notOther_NodesSelector, null);
        
        PropertiesLoader propLoader = new PropertiesLoader(openl, bindingContext, module);
        for (TableSyntaxNode tsn : tableSyntaxNodes) {
            try {
                propLoader.loadProperties(tsn);
            } catch (SyntaxNodeException error) {
                processError(error, tsn, bindingContext);
            } catch (CompositeSyntaxNodeException ex) {
                for (SyntaxNodeException error : ex.getErrors()) {
                    processError(error, tsn, bindingContext);
                }
            } catch (Throwable t) {
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tsn);
                processError(error, tsn, bindingContext);
            }
        }
    }

    /**
     * Processes datatype table nodes before the bind operation. Checks type
     * declarations and finds invalid using of inheritance feature at this step.
     * 
     * @param datatypeNodes array of datatype nodes
     * @param bindingContext binding context
     * @return array of datatypes in order of binding
     */
    private TableSyntaxNode[] processDatatypes(TableSyntaxNode[] datatypeNodes, IBindingContext bindingContext) {
        Map<String, TableSyntaxNode> typesMap = DatatypeHelper.createTypesMap(datatypeNodes, bindingContext);
        
        TableSyntaxNode[] orderedTypes = DatatypeHelper.orderDatatypes(typesMap, bindingContext);
        
        return orderedTypes;
    }

    private void processVocabulary(IVocabulary vocabulary,            
            RulesModuleBindingContext moduleContext) {
        IOpenClass[] types = null;

        try {
            types = vocabulary.getVocabularyTypes();
        } catch (SyntaxNodeException error) {
            BindHelper.processError(error, moduleContext);
        }

        if (types != null) {

            for (int i = 0; i < types.length; i++) {

                try {
                    moduleContext.addType(ISyntaxConstants.THIS_NAMESPACE, types[i]);
                } catch (Throwable t) {
                    BindHelper.processError(null, t, moduleContext);
                }
            }
        }
    }

    private void processExtensions(XlsModuleOpenClass module,
            XlsModuleSyntaxNode moduleNode,
            List<IdentifierNode> extensionNodes,RulesModuleBindingContext moduleContext) {

        for (int i = 0; i < extensionNodes.size(); i++) {

            IdentifierNode identifierNode = extensionNodes.get(i);
            IExtensionBinder binder = NameConventionBinderFactory.INSTANCE.getNodeBinder(identifierNode);

            if (binder != null && binder.getNodeType().equals(identifierNode.getType())) {
                binder.bind(module, moduleNode, identifierNode, moduleContext);
            }
        }
    }

    private OpenL makeOpenL(XlsModuleSyntaxNode moduleNode) {

        String openlName = getOpenLName(moduleNode.getOpenlNode());
        List<String> allImports = moduleNode.getAllImports();

        if (allImports == null) {
            return OpenL.getInstance(openlName, userContext);
        }

        OpenLBuilderImpl builder = new OpenLBuilderImpl();

        builder.setExtendsCategory(openlName);

        String category = openlName + "::" + moduleNode.getModule().getUri(0);
        builder.setCategory(category);
        builder.setImports(allImports);
        builder.setContexts(null, userContext);
        builder.setInheritExtendedConfigurationLoader(true);

        return OpenL.getInstance(category, userContext, builder);
    }

    private IVocabulary makeVocabulary(XlsModuleSyntaxNode moduleNode) {

        final IdentifierNode vocabularyNode = moduleNode.getVocabularyNode();

        if (vocabularyNode == null) {
            return null;
        }

        final ClassLoader userClassLoader = userContext.getUserClassLoader();
        Thread.currentThread().setContextClassLoader(userClassLoader);

        IVocabulary vocabulary = (IVocabulary) userContext.execute(new IExecutable() {

            public Object execute() {

                String vocabularyClassName = vocabularyNode.getIdentifier();

                try {
                    Class<?> vClass = userClassLoader.loadClass(vocabularyClassName);

                    return (IVocabulary) vClass.newInstance();
                } catch (Throwable t) {
                    String message = String.format("Vocabulary type '%s' cannot be loaded", vocabularyClassName);
                    BindHelper.processError(message, vocabularyNode, t);

                    return null;
                }
            }
        });

        return vocabulary;
    }

    private IMemberBoundNode preBindXlsNode(ISyntaxNode syntaxNode,
            OpenL openl,
            RulesModuleBindingContext bindingContext,
            XlsModuleOpenClass moduleOpenClass) throws Exception {

        String tableSyntaxNodeType = syntaxNode.getType();
        AXlsTableBinder binder = findBinder(tableSyntaxNodeType);

        if (binder == null) {
            String message = String.format("Unknown table type '%s'", tableSyntaxNodeType);
            log.debug(message);

            return null;
        }

        TableSyntaxNode tableSyntaxNode = (TableSyntaxNode) syntaxNode;
        return binder.preBind(tableSyntaxNode, openl, bindingContext, moduleOpenClass);
    }

    
    protected AXlsTableBinder findBinder(String tableSyntaxNodeType) {
		return getBinderFactory().get(tableSyntaxNodeType);
	}

	protected String getDefaultOpenLName()
    {
    	return DEFAULT_OPENL_NAME;
    }
    
    
    private String getOpenLName(OpenlSyntaxNode osn) {
        return osn == null ? getDefaultOpenLName()  : osn.getOpenlName();
    }

    private TableSyntaxNode[] getTableSyntaxNodes(XlsModuleSyntaxNode moduleSyntaxNode,
            ISelector<ISyntaxNode> childSelector,
            LinkedList<Comparator<TableSyntaxNode>> tableComparators) {

        ArrayList<ISyntaxNode> childSyntaxNodes = new ArrayList<ISyntaxNode>();

        for (TableSyntaxNode tsn : moduleSyntaxNode.getXlsTableSyntaxNodes()) {

            if (childSelector == null || childSelector.select(tsn)) {
                childSyntaxNodes.add(tsn);
            }
        }

        TableSyntaxNode[] tableSyntaxNodes = childSyntaxNodes.toArray(new TableSyntaxNode[childSyntaxNodes.size()]);
		/*if (tableComparators != null && tableComparators.size() > 0) {
			final LinkedList<Comparator<TableSyntaxNode>> finalTableComparators = tableComparators;
			Arrays.sort(tableSyntaxNodes, new Comparator<TableSyntaxNode>() {
				@Override
				public int compare(TableSyntaxNode o1, TableSyntaxNode o2) {
					int result = 0;
					for (Comparator<TableSyntaxNode> comparator : finalTableComparators) {
						int tmp = comparator.compare(o1, o2);
						if (tmp != 0) {
							result = tmp;
						}
					}
					return result;
				}
			});
		}*/
        //BAD CODE WARNING!!!
		if (tableComparators != null && tableComparators.size() > 0) {
			for (Comparator<TableSyntaxNode> comparator : tableComparators) {
				try {
					Arrays.sort(tableSyntaxNodes, comparator);
				} catch (Exception e) {
					// ignore sort exceptions.
				}
			}
		}

        return tableSyntaxNodes;
    }

    protected IBoundNode bindInternal(XlsModuleSyntaxNode moduleSyntaxNode,
            XlsModuleOpenClass module,
            TableSyntaxNode[] tableSyntaxNodes,
            OpenL openl,
            RulesModuleBindingContext moduleContext) {

        IMemberBoundNode[] children = new IMemberBoundNode[tableSyntaxNodes.length];

        for (int i = 0; i < tableSyntaxNodes.length; i++) {

            IMemberBoundNode child = beginBind(tableSyntaxNodes[i], module, openl, moduleContext);
            children[i] = child;

            if (child != null) {
                child.addTo(module);
            }
        }

        for (int i = 0; i < children.length; i++) {
            if (children[i] != null) {
                finilizeBind(children[i], tableSyntaxNodes[i], moduleContext);
            }
        }
        
        if (moduleContext.isExecutionMode()) {
            removeDebugInformation(children, tableSyntaxNodes, moduleContext);
        } else {
            // Importing all classes is needed only in edit mode 
            addImportedClasses(module, moduleSyntaxNode);
        }

        return new ModuleNode(moduleSyntaxNode, moduleContext.getModule());
    }

    protected void finilizeBind(IMemberBoundNode memberBoundNode,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext moduleContext) {

        try {
            memberBoundNode.finalizeBind(moduleContext);

        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, moduleContext);

        } catch (CompositeSyntaxNodeException ex) {
        	if (ex.getErrors() != null) {
        		for (SyntaxNodeException error : ex.getErrors()) {
        			processError(error, tableSyntaxNode, moduleContext);
        		}
        	}
        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);
        }
    }

    protected void removeDebugInformation(IMemberBoundNode[] boundNodes, TableSyntaxNode[] tableSyntaxNodes,
            RulesModuleBindingContext moduleContext) {
        for (int i = 0; i < boundNodes.length; i++) {
            if (boundNodes[i] != null) {
                try {
                    boundNodes[i].removeDebugInformation(moduleContext);

                } catch (SyntaxNodeException error) {
                    processError(error, tableSyntaxNodes[i], moduleContext);

                } catch (CompositeSyntaxNodeException ex) {

                    for (SyntaxNodeException error : ex.getErrors()) {
                        processError(error, tableSyntaxNodes[i], moduleContext);
                    }

                } catch (Throwable t) {

                    SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNodes[i]);
                    processError(error, tableSyntaxNodes[i], moduleContext);
                }
            }
        }
    }

    protected IMemberBoundNode beginBind(TableSyntaxNode tableSyntaxNode,
            XlsModuleOpenClass module,
            OpenL openl,
            RulesModuleBindingContext moduleContext) {

        try {
            return preBindXlsNode(tableSyntaxNode, openl, moduleContext, module);

        } catch (SyntaxNodeException error) {
            processError(error, tableSyntaxNode, moduleContext);

            return null;

        } catch (CompositeSyntaxNodeException ex) {

            for (SyntaxNodeException error : ex.getErrors()) {
                processError(error, tableSyntaxNode, moduleContext);
            }

            return null;

        } catch (Throwable t) {

            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(t, tableSyntaxNode);
            processError(error, tableSyntaxNode, moduleContext);

            return null;
        }
    }

    protected void processError(SyntaxNodeException error,
            TableSyntaxNode tableSyntaxNode,
            RulesModuleBindingContext moduleContext) {

        error.setTopLevelSyntaxNode(tableSyntaxNode);

        tableSyntaxNode.addError(error);
        BindHelper.processError(error, moduleContext);
    }


    /**
     * Add to an xls module class a classes from imported packages.
     * 
     * @param module module class that will contain a classes from imported
     *            packages
     * @param moduleSyntaxNode module source
     */
    private void addImportedClasses(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleSyntaxNode) {
        for (String packageName : moduleSyntaxNode.getAllImports()) {
            for (Class<?> type : getClasses(packageName)) {
                try {
                    IOpenClass openType = JavaOpenClass.getOpenClass(type);
                    if (module.getTypes().values().contains(openType) || !isValid(openType))
                        continue;
                    
                    module.addType(ISyntaxConstants.THIS_NAMESPACE, openType);
                } catch (OpenLCompilationException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Check if type is valid (for example, it can be used in a DataType tables,
     * Data tables etc)
     * 
     * @param openType checked type
     * @return true if class is valid.
     */
    private boolean isValid(IOpenClass openType) {
        Class<?> instanceClass = openType.getInstanceClass();

        int modifiers = instanceClass.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return false;
        }

        if (OpenLWrapper.class.isAssignableFrom(instanceClass)) {
            // generated class for tutorial for example.
            return false;
        }

        Map<String, IOpenField> fields = openType.getFields();
        if (fields.size() <= 1) {
            // Every field has a "class" field. We skip a classes that doesn't
            // have any other field.
            return false;
        }

        return true;
    }
    
    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package.
     * 
     * @param packageName The package
     * @return The classes
     */
    private Class<?>[] getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new Class[0];
        }
        List<File> dirs = new ArrayList<File>();
        List<URL> jars = new ArrayList<URL>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try {
                URI uri = resource.toURI();
                String scheme = uri.getScheme();
                
                if (scheme != null) {
                    if ("file".equalsIgnoreCase(scheme)) {
                        dirs.add(new File(uri));
                    } else if ("jar".equalsIgnoreCase(scheme)) {
                        try {
                            String jarPath = resource.getFile().split("!")[0];
                            jars.add(new URL(jarPath));
                        } catch (MalformedURLException e) {
                            log.error(e.getMessage(), e);
                            continue;
                        }
                    }
                }
            } catch (URISyntaxException e) {
                // This should not be happen, but...
                log.error(e.getMessage(), e);
            }
        }
        ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName, classLoader));
        }
        for (URL jar : jars) {
            classes.addAll(findClassesFromJar(jar, packageName, classLoader));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * A method is used to find all classes in a given directory. If a class
     * cannot be loaded, it is skipped (in our case we don't need such classes).
     * 
     * @param directory The directory
     * @param packageName The package name for classes found inside the
     *            directory
     * @param classLoader a ClassLoader that is used to load a classes 
     * @return The classes
     */
    private List<Class<?>> findClasses(File directory, String packageName, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                continue;
            } else {
                String suffix = ".class";
                if (fileName.endsWith(suffix) && !fileName.contains("$")) {
                    try {
                        String className = fileName.substring(0, fileName.length() - suffix.length());
                        String fullClassName = packageName + '.' + className;
                        Class<?> type = Class.forName(fullClassName, true, classLoader);
                        classes.add(type);
                    } catch (Throwable t) {
                        // Cannot load a class. Skip it
                        continue;
                    }
                }
            }
        }
        return classes;
    }

    /**
     * A method is used to find all classes in a given jar. If a class cannot be
     * loaded, it is skipped (in our case we don't need such classes).
     * 
     * @param jar URL of a jar
     * @param packageName The package name for classes found inside the jar
     * @param classLoader a ClassLoader that is used to load a classes
     * @return The classes
     */
    private List<Class<?>> findClassesFromJar(URL jar, String packageName, ClassLoader classLoader) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace(".class", "").replace('/', '.');
                    if (className.startsWith(packageName)) {
                        try {
                            classes.add(Class.forName(className, true, classLoader));
                        } catch (ClassNotFoundException e) {
                            // Cannot load a class. Skip it
                            continue;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return classes;
    }
}
