package org.openl.rules.ui.tree.view;

import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryNTreeNodeBuilder;
import org.openl.rules.ui.tree.CategoryPropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.ModulePropertiesTableNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;

public class CategoryDetailedView implements RulesTreeView {

    private final BaseTableTreeNodeBuilder[] sorters = {
        new ModulePropertiesTableNodeBuilder(), 
        new CategoryNTreeNodeBuilder(0, "-"),
        new CategoryNTreeNodeBuilder(1, "-"),
        new CategoryPropertiesTableNodeBuilder(), 
        new OpenMethodInstancesGroupTreeNodeBuilder(),
        new TableInstanceTreeNodeBuilder(),
        new TableVersionTreeNodeBuilder()
    };

    public String getName() {
    	return "categoryDetailed";
    }
    
    public String getDisplayName() {
    	return "Category Detailed";
    }

    public String getDescription() {
    	return "Provides more detailed categorized view";
    }

    @SuppressWarnings("unchecked")
    public TreeNodeBuilder[] getBuilders() {
        return sorters;
    }

}
