/*
 * Created on Jul 29, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

/**
 * @author snshor
 * 
 */
public interface IBoundModuleNode extends IBoundNode {

    public IBoundMethodNode getMethodNode(String name);

}
