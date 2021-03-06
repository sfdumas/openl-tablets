package org.openl.rules.maven.gen;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateInitializationWriter implements TypeInitializationWriter {

    private static final String DATE_FORMAT_FOR_DATE_CONSTRUCTOR = "MM/dd/yyyy hh:mm:ss a";

    public String getInitialization(Object value) {
        Date date = (Date) value;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_FORMAT_FOR_DATE_CONSTRUCTOR, Locale.US);
        String stringValue = simpleDateFormat.format(date);
        return String.format("new %s(\"%s\")", JavaClassGeneratorHelper.filterTypeSimpleName(value.getClass()), stringValue);
    }
}
