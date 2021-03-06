/*
 * Created on Mar 9, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

public class ArrayIndex implements IOpenIndex {
    IOpenClass elementType;

    public ArrayIndex(IOpenClass elementType) {
        this.elementType = elementType;
    }

    public IOpenClass getElementType() {
        return elementType;
    }

    public IOpenClass getIndexType() {
        return JavaOpenClass.INT;
    }

    @Override
    public Collection getIndexes(Object container) {
        int length = Array.getLength(container);
        List<Integer> indexes = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            indexes.add(i);
        }
        return indexes;
    }

    public Object getValue(Object container, Object index) {
        return Array.get(container, (Integer) index);
    }

    public boolean isWritable() {
        return true;
    }

    public void setValue(Object container, Object index, Object value) {
        Array.set(container, (Integer) index, value);
    }

}