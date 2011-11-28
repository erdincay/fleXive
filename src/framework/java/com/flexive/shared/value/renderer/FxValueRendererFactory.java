/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.shared.value.renderer;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;
import org.apache.commons.lang.StringUtils;

import java.math.RoundingMode;
import java.text.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for FxValueRenderer. A FxValueRenderer provides a transparent way of formatting
 * any FxValue object in a given language.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxValueRendererFactory {

    private static final ConcurrentMap<FxLanguage, FxValueRendererImpl> renderers = new ConcurrentHashMap<FxLanguage, FxValueRendererImpl>();
    /**
     * Internal fallback default language for locale-agnostic formatters
     */
    static final FxLanguage DEFAULT = FxLanguage.DEFAULT;

    /**
     * FxDate formatter.
     */
    private static class FxDateFormatter implements FxValueFormatter<Date, FxDate> {
        public String format(FxDate value, Date translation, FxLanguage outputLanguage) {
            return translation != null
                    ? DateFormat.getDateInstance(DateFormat.MEDIUM, outputLanguage.getLocale()).format(translation)
                    : getEmptyMessage(outputLanguage);
        }
    }

    /**
     * FxDateRange formatter
     */
    private static class FxDateRangeFormatter implements FxValueFormatter<DateRange, FxDateRange> {
        public String format(FxDateRange container, DateRange value, FxLanguage outputLanguage) {
            return value != null
                    ? formatDate(outputLanguage, value.getLower()) + " - " + formatDate(outputLanguage, value.getUpper())
                    : getEmptyMessage(outputLanguage);
        }

        private String formatDate(FxLanguage outputLanguage, Date date) {
            return DateFormat.getDateInstance(DateFormat.MEDIUM, outputLanguage.getLocale()).format(date);
        }
    }

    /**
     * FxDateTime formatter.
     */
    private static class FxDateTimeFormatter implements FxValueFormatter<Date, FxDateTime> {
        public String format(FxDateTime container, Date value, FxLanguage outputLanguage) {
            return value != null
                    ? DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, outputLanguage.getLocale()).format(value)
                    : getEmptyMessage(outputLanguage);
        }
    }

    /**
     * FxDateTimeRange formatter.
     */
    private static class FxDateTimeRangeFormatter implements FxValueFormatter<DateRange, FxDateTimeRange> {
        public String format(FxDateTimeRange container, DateRange value, FxLanguage outputLanguage) {
            return value != null
                    ? formatDateTime(outputLanguage, value.getLower()) + " - " + formatDateTime(outputLanguage, value.getUpper())
                    : getEmptyMessage(outputLanguage);
        }

        private String formatDateTime(FxLanguage outputLanguage, Date dateTime) {
            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, outputLanguage.getLocale()).format(dateTime);
        }
    }


    final static Map<Locale, Map<String, NumberFormat>> NUMBER_FORMATS = new HashMap<Locale, Map<String, NumberFormat>>(10);
    final static Map<Locale, Map<String, DateFormat>> DATE_FORMATS = new HashMap<Locale, Map<String, DateFormat>>(10);
    final static Map<Locale, Map<String, DateFormat>> TIME_FORMATS = new HashMap<Locale, Map<String, DateFormat>>(10);
    final static Map<Locale, Map<String, DateFormat>> DATETIME_FORMATS = new HashMap<Locale, Map<String, DateFormat>>(10);

    /**
     * Get a number format instance depending on the current users formatting options
     *
     * @return NumberFormat
     */
    public static NumberFormat getNumberFormatInstance() {
        return getNumberFormatInstance(FxContext.getUserTicket().getLanguage().getLocale());
    }

    final static DecimalFormat PORTABLE_NUMBERFORMAT;
    static {
        PORTABLE_NUMBERFORMAT = (DecimalFormat)DecimalFormat.getNumberInstance();
        PORTABLE_NUMBERFORMAT.setGroupingUsed(false);
        PORTABLE_NUMBERFORMAT.setMaximumIntegerDigits(Integer.MAX_VALUE);
        PORTABLE_NUMBERFORMAT.setMaximumFractionDigits(Integer.MAX_VALUE);
        DecimalFormatSymbols dfs = (DecimalFormatSymbols)PORTABLE_NUMBERFORMAT.getDecimalFormatSymbols().clone();
        dfs.setDecimalSeparator('.');
        dfs.setGroupingSeparator(',');
        PORTABLE_NUMBERFORMAT.setDecimalFormatSymbols(dfs);
    }

    public static NumberFormat getPortableNumberFormatInstance() {
        return PORTABLE_NUMBERFORMAT;
    }

    /**
     * Get a number format instance depending on the current users formatting options
     *
     * @param locale locale to use
     * @return NumberFormat
     */
    public static NumberFormat getNumberFormatInstance(Locale locale) {
        final String currentUserKey = buildCurrentUserNumberFormatKey();
        synchronized (NUMBER_FORMATS) {
            if(NUMBER_FORMATS.containsKey(locale)) {
                Map<String, NumberFormat> map = NUMBER_FORMATS.get(locale);
                if(map.containsKey(currentUserKey))
                    return map.get(currentUserKey);
            } else
                NUMBER_FORMATS.put(locale, new HashMap<String, NumberFormat>(5));
            Map<String, NumberFormat> map = NUMBER_FORMATS.get(locale);
            DecimalFormat format = (DecimalFormat)DecimalFormat.getNumberInstance(locale);
            DecimalFormatSymbols dfs = (DecimalFormatSymbols)format.getDecimalFormatSymbols().clone();
            final FxContext ctx = FxContext.get();
            dfs.setDecimalSeparator(ctx.getDecimalSeparator());
            dfs.setGroupingSeparator(ctx.getGroupingSeparator());
            format.setGroupingUsed(ctx.useGroupingSeparator());
            format.setDecimalFormatSymbols(dfs);
            map.put(currentUserKey, format);
            return format;
        }
    }

    /**
     * Get the date formatter for the current users locale
     *
     * @return DateFormat
     */
    public static DateFormat getDateFormat() {
        return getDateFormat(FxContext.getUserTicket().getLanguage().getLocale());
    }

    /**
     * Get the date formatter for the requested locale
     *
     * @param locale requested locale
     * @return DateFormat
     */
    public static DateFormat getDateFormat(Locale locale) {
        final String currentUserKey = FxContext.get().getDateFormat();
        synchronized (DATE_FORMATS) {
            if(DATE_FORMATS.containsKey(locale)) {
                Map<String, DateFormat> map = DATE_FORMATS.get(locale);
                if(map.containsKey(currentUserKey))
                    return map.get(currentUserKey);
            } else
                DATE_FORMATS.put(locale, new HashMap<String, DateFormat>(5));
            Map<String, DateFormat> map = DATE_FORMATS.get(locale);
            DateFormat format = new SimpleDateFormat(currentUserKey, locale);
            map.put(currentUserKey, format);
            return format;
        }
    }

    /**
     * Get the time formatter for the current users locale
     *
     * @return DateFormat
     */
    public static DateFormat getTimeFormat() {
        return getTimeFormat(FxContext.getUserTicket().getLanguage().getLocale());
    }

    /**
     * Get the time formatter for the requested locale
     *
     * @param locale requested locale
     * @return DateFormat
     */
    public static DateFormat getTimeFormat(Locale locale) {
        final String currentUserKey = FxContext.get().getTimeFormat();
        synchronized (TIME_FORMATS) {
            if(TIME_FORMATS.containsKey(locale)) {
                Map<String, DateFormat> map = TIME_FORMATS.get(locale);
                if(map.containsKey(currentUserKey))
                    return map.get(currentUserKey);
            } else
                TIME_FORMATS.put(locale, new HashMap<String, DateFormat>(5));
            Map<String, DateFormat> map = TIME_FORMATS.get(locale);
            DateFormat format = new SimpleDateFormat(currentUserKey, locale);
            map.put(currentUserKey, format);
            return format;
        }
    }

    /**
     * Get the date/time formatter for the current users locale
     *
     * @return DateFormat
     */
    public static DateFormat getDateTimeFormat() {
        return getDateTimeFormat(FxContext.getUserTicket().getLanguage().getLocale());
    }

    /**
     * Get the date/time formatter for the requested locale
     *
     * @param locale requested locale
     * @return DateFormat
     */
    public static DateFormat getDateTimeFormat(Locale locale) {
        final String currentUserKey = FxContext.get().getDateTimeFormat();
        synchronized (DATETIME_FORMATS) {
            if(DATETIME_FORMATS.containsKey(locale)) {
                Map<String, DateFormat> map = DATETIME_FORMATS.get(locale);
                if(map.containsKey(currentUserKey))
                    return map.get(currentUserKey);
            } else
                DATETIME_FORMATS.put(locale, new HashMap<String, DateFormat>(5));
            Map<String, DateFormat> map = DATETIME_FORMATS.get(locale);
            DateFormat format = new SimpleDateFormat(currentUserKey, locale);
            map.put(currentUserKey, format);
            return format;
        }
    }

    private static String buildCurrentUserNumberFormatKey() {
        FxContext ctx = FxContext.get();
        return String.valueOf(ctx.getDecimalSeparator())+String.valueOf(ctx.getGroupingSeparator())+String.valueOf(ctx.useGroupingSeparator());
    }

    /**
     * FxDouble formatter.
     */
    private static class FxDoubleFormatter implements FxValueFormatter<Double, FxDouble> {
        public String format(FxDouble value, Double translation, FxLanguage outputLanguage) {
            return translation != null
                    ? getNumberFormatInstance(outputLanguage.getLocale()).format(translation)
                    : getEmptyMessage(outputLanguage);
        }
    }

    /**
     * FxFloat formatter.
     */
    private static class FxFloatFormatter implements FxValueFormatter<Float, FxFloat> {
        public String format(FxFloat value, Float translation, FxLanguage outputLanguage) {
            return translation != null
                    ? getNumberFormatInstance(outputLanguage.getLocale()).format(translation)
                    : getEmptyMessage(outputLanguage);
        }
    }

    /**
     * FxSelectOne formatter
     */
    private static class FxSelectOneFormatter implements FxValueFormatter<FxSelectListItem, FxSelectOne> {
        public String format(FxSelectOne value, FxSelectListItem translation, FxLanguage outputLanguage) {
            return translation != null
                    ? translation.getLabelBreadcrumbPath(outputLanguage)
                    : getEmptyMessage(outputLanguage);
        }
    }

    /**
     * FxSelectMany formatter
     */
    private static class FxSelectManyFormatter implements FxValueFormatter<SelectMany, FxSelectMany> {
        public String format(FxSelectMany value, SelectMany translation, FxLanguage outputLanguage) {
            if (translation == null) {
                return getEmptyMessage(outputLanguage);
            }
            final List<String> out = new ArrayList<String>(translation.getSelected().size());
            for (FxSelectListItem item : translation.getSelected()) {
                out.add(item.getLabelBreadcrumbPath(outputLanguage));
            }
            return StringUtils.join(out.iterator(), ", ");
        }
    }

    /**
     * FxReference formatter
     */
    private static class FxReferenceFormatter implements FxValueFormatter<ReferencedContent, FxReference> {
        public String format(FxReference container, ReferencedContent translation, FxLanguage outputLanguage) {
            if (translation == null) {
                return getEmptyMessage(outputLanguage);
            }
            return StringUtils.defaultIfEmpty(translation.getCaption(), translation.toString());
        }
    }

    private static class FxNoAccessFormatter implements FxValueFormatter<Object, FxNoAccess> {
        public String format(FxNoAccess container, Object value, FxLanguage outputLanguage) {
            final UserTicket ticket = FxContext.getUserTicket();
            return FxSharedUtils.getLocalizedMessage(FxSharedUtils.SHARED_BUNDLE, ticket.getLanguage().getId(),
                    ticket.getLanguage().getIso2digit(), "shared.noAccess");
        }
    }

    /**
     * Generic object formatter for all types that are not explicitly covered.
     */
    private static class ObjectFormatter implements FxValueFormatter {
        public String format(FxValue value, Object translation, FxLanguage outputLanguage) {
            return translation != null ? translation.toString() : getEmptyMessage(outputLanguage);
        }

    }

    static {
        addRenderer(DEFAULT, FxDate.class, new FxDateFormatter());
        addRenderer(DEFAULT, FxDateTime.class, new FxDateTimeFormatter());
        addRenderer(DEFAULT, FxDouble.class, new FxDoubleFormatter());
        addRenderer(DEFAULT, FxFloat.class, new FxFloatFormatter());
        addRenderer(DEFAULT, FxSelectOne.class, new FxSelectOneFormatter());
        addRenderer(DEFAULT, FxSelectMany.class, new FxSelectManyFormatter());
        addRenderer(DEFAULT, FxReference.class, new FxReferenceFormatter());
        addRenderer(DEFAULT, FxNoAccess.class, new FxNoAccessFormatter());
        addRenderer(DEFAULT, FxDateRange.class, new FxDateRangeFormatter());
        addRenderer(DEFAULT, FxDateTimeRange.class, new FxDateTimeRangeFormatter());
        //noinspection unchecked
        addRenderer(DEFAULT, FxValue.class, new ObjectFormatter());
    }

    /**
     * Return a <code>FxValueRenderer</code> instance for the user's current language.
     *
     * @return a <code>FxValueRenderer</code> instance for the user's current language.
     */
    public static FxValueRenderer getInstance() {
        final UserTicket ticket = FxContext.getUserTicket();
        return getInstance(ticket != null ? ticket.getLanguage() : null);
    }

    /**
     * Returns a <code>FxValueRenderer</code> instance for the given language.
     * If <code>language</code> is null, the default renderer is returned.
     *
     * @param language the target language. Both the output formatting and the value to be
     *                 rendered may depend on the renderer's language.
     * @return a <code>FxValueRenderer</code> instance for the given language.
     */
    public static FxValueRenderer getInstance(FxLanguage language) {
        if (language == null) {
            // default renderer always exists
            return renderers.get(DEFAULT);
        }
        if (!renderers.containsKey(language)) {
            renderers.putIfAbsent(language, new FxValueRendererImpl(language));
        }
        return renderers.get(language);
    }

    /**
     * Return the default FxValue formatter for the given FxValue subclass.
     *
     * @param valueType class of the value to be formatted
     * @return the default FxValue formatter for the given FxValue subclass.
     */
    public static FxValueFormatter getDefaultFormatter(Class valueType) {
        // this works because the addRenderer methods are bounded
        //noinspection unchecked
        return renderers.get(DEFAULT).get(valueType);
    }

    private static <DT, T extends FxValue<DT, T>>
    void addRenderer(FxLanguage language, Class<T> valueType, FxValueFormatter<DT, T> formatter) {
        renderers.putIfAbsent(language, new FxValueRendererImpl(language));
        renderers.get(language).put(valueType, formatter);
    }

    private static String getEmptyMessage(FxLanguage outputLanguage) {
        return ""; // TODO
    }
}
