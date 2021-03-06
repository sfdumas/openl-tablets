package org.openl.rules.ruleservice.publish.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.annotations.Name;
import org.openl.types.IOpenClass;
import org.openl.util.generation.GenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MethodUtil {
    private MethodUtil() {
    }

    public static List<Method> sort(Collection<Method> m) {
        List<Method> methods = new ArrayList<Method>(m);
        Collections.sort(methods, new Comparator<Method>() {
            public int compare(Method o1, Method o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getParameterTypes().length == o2.getParameterTypes().length) {
                        int i = 0;
                        while (i < o1.getParameterTypes().length && o1.getParameterTypes()[i]
                            .equals(o2.getParameterTypes()[i])) {
                            i++;
                        }
                        return o1.getParameterTypes()[i].getName().compareTo(o2.getParameterTypes()[i].getName());
                    } else {
                        return o1.getParameterTypes().length - o2.getParameterTypes().length;
                    }
                } else {
                    return o1.getName().compareTo(o2.getName());
                }
            };
        });
        return methods;
    }

    private static void validateAndUpdateParameterNames(String[] parameterNames) {
        Set<String> allNames = new HashSet<String>();
        for (String s : parameterNames) {
            allNames.add(s);
        }
        Set<String> usedNames = new HashSet<String>();
        for (int i = 0; i < parameterNames.length; i++) {
            if (allNames.contains(parameterNames[i])) {
                allNames.remove(parameterNames[i]);
                usedNames.add(parameterNames[i]);
            } else {
                int j = 0;
                while (allNames.contains("arg" + j) || usedNames.contains("arg" + j)) {
                    j++;
                }
                parameterNames[i] = "arg" + j;
            }
        }
    }

    public static String[] getParameterNames(Method method, OpenLService service) {
        if (service != null && service.getOpenClass() != null) {
            IOpenClass openClass = service.getOpenClass();
            boolean provideRuntimeContext = service.isProvideRuntimeContext();
            boolean provideVariations = service.isProvideVariations();
            String[] parameterNames = GenUtils
                .getParameterNames(method, openClass, provideRuntimeContext, provideVariations);

            int i = 0;
            for (Annotation[] annotations : method.getParameterAnnotations()) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Name) {
                        Name name = (Name) annotation;
                        if (!name.value().isEmpty()) {
                            parameterNames[i] = name.value();
                        } else {
                            Logger log = LoggerFactory.getLogger(MethodUtil.class);
                            if (log.isWarnEnabled()) {
                                log.warn("Invalid parameter name '" + name.value() + "'. Parameter name for '" + method
                                    .getClass()
                                    .getCanonicalName() + "#" + method.getName() + "' was skipped!");
                            }
                        }
                    }
                }
                i++;
            }

            validateAndUpdateParameterNames(parameterNames);

            return parameterNames;
        }
        return GenUtils.getParameterNames(method);
    }
}
