package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

import java.lang.reflect.Array;

/**
 * A converter for arrays. It converts strings to an array of a specified type and vice versa.
 * E.g. for int[]: "1,2,4,8" <==> int[]{1,2,4,8}
 *
 * @author Yury Molchan
 */
class String2ArrayConvertor<C, T> implements IString2DataConvertor<T> , IString2DataConverterWithContext<T> {

    /**
     * Constant for escaping {@link #ARRAY_ELEMENTS_SEPARATOR} of elements. It is needed when the element contains
     * separator as part of object name, e.g: Mike\\,Sara`s Son.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";

    /**
     * Separator for elements of array, represented as <code>{@link String}</code>.
     */
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";

    private Class<C> componentType;

    /**
     * @param componentType a component type of an array.
     */
    public String2ArrayConvertor(Class<C> componentType) {
        this.componentType = componentType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;
        if (data.length() == 0) return (T) Array.newInstance(componentType, 0);

        String[] elementValues = StringTool.splitAndEscape(data, ARRAY_ELEMENTS_SEPARATOR,
                ARRAY_ELEMENTS_SEPARATOR_ESCAPER);
        T resultArray = (T) Array.newInstance(componentType, elementValues.length);

        IString2DataConvertor<C> converter = String2DataConvertorFactory.getConvertor(componentType);
        int i = 0;
        for (String elementValue : elementValues) {
            Object element;
            if (elementValue.length() == 0) {
                element = null;
            } else {
                if (cxt != null && converter instanceof IString2DataConverterWithContext) {
                    IString2DataConverterWithContext<C> convertorCxt = (IString2DataConverterWithContext<C>) converter;
                    element = convertorCxt.parse(elementValue, format, cxt);
                }else{
                    element = converter.parse(elementValue, format);
                }
            }
            Array.set(resultArray, i, element);
            i++;
        }

        return resultArray;
    }
    
    /**
     * Converts an input array of elements to <code>{@link String}</code>. Elements in the return value will separated by
     * {@link #ARRAY_ELEMENTS_SEPARATOR}. Null safety.
     *
     * @param data array of elements that should be represented as <code>{@link String}</code>.
     * @return <code>{@link String}</code> representation of the income array. <code>NULL</code> if the income value is
     * <code>NULL</code> or if income value is not an array.
     */
    @Override
    public String format(T data, String format) {
        if (data == null) return null;

        IString2DataConvertor<C> converter = String2DataConvertorFactory.getConvertor(componentType);

        int length = Array.getLength(data);
        String[] elementResults = new String[length];
        for (int i = 0; i < length; i++) {
            C element = (C) Array.get(data, i);;
            elementResults[i] = converter.format(element, format);
        }

        String result = StringUtils.join(elementResults, ARRAY_ELEMENTS_SEPARATOR);
        return result;
    }

    /**
     * @param data <code>{@link String}</code> representation of the array.
     * @return array of elements. <code>NULL</code> if input is empty or can`t get the component type of the array.
     */
    @Override
    public T parse(String data, String format) {
        return parse(data, format, null);
    }
}
