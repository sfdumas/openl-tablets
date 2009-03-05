package org.openl.rules.web.jsf.util;

import java.util.Map;
import java.util.Collection;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Various generic helpful methods to simplify common operations with JSF.
 * 
 * @author Andrey Naumenko
 */
public abstract class FacesUtils {
    /**
     * Returns variable from faces context by name using ValueBinding.
     * 
     * @param name bean name.
     * 
     * @return bean from context.
     */
    public static Object getFacesVariable(String name) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        return facesContext.getApplication().createValueBinding(getBinding(name)).getValue(facesContext);
    }

    private static String getBinding(String name) {
        if (name.startsWith("#{")) {
            return name;
        }
        return "#{" + name + "}";
    }

    @SuppressWarnings("unchecked")
    public static Map getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    @SuppressWarnings("unchecked")
    public static Map getRequestMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestMap();
    }

    @SuppressWarnings("unchecked")
    public static Map getRequestParameterMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
    }

    /**
     * Returns request parameter from HttpServletRequest object through current
     * FacesContext.
     * 
     * @param parameterName parameter name
     * 
     * @return parameter value - if parameter exists, <code>null</code> -
     *         otherwise.
     */
    public static String getRequestParameter(String parameterName) {
        return (String) getRequestParameterMap().get(parameterName);
    }

    /**
     * Creates an array of <code>SelectItem</code>s from array of
     * <code>String</code>s;
     * 
     * @param values an array of <code>SelectItem</code> values.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(String[] values) {
        SelectItem[] items = new SelectItem[values.length];
        for (int i = 0; i < items.length; ++i) {
            items[i] = new SelectItem(values[i]);
        }
        return items;
    }

    /**
     * Creates an array of <code>SelectItem</code>s from collection of
     * <code>String</code>s;
     * 
     * @param values an array of <code>SelectItem</code> values.
     * @return array of JSF objects representing items.
     */
    public static SelectItem[] createSelectItems(Collection<String> values) {
        SelectItem[] items = new SelectItem[values.size()];
        int index = 0;
        for (String value : values) {
            items[index++] = new SelectItem(value);
        }
        return items;
    }

    public static FacesContext getFacesContext(HttpServletRequest request, HttpServletResponse response) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            FacesContextFactory contextFactory = (FacesContextFactory) FactoryFinder
                    .getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
            LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder
                    .getFactory(FactoryFinder.LIFECYCLE_FACTORY);
            Lifecycle lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
            facesContext = contextFactory.getFacesContext(request.getSession().getServletContext(), request, response,
                    lifecycle);
        }
        return facesContext;
    }

}
