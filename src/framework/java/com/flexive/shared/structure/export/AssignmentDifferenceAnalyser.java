/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.structure.export;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Difference Analyser - Records any differences between a base property / group and an FxAssignment (if within the
 * same type)
 * The analysis will always return the (GTB) values of the property's / group's assignments which differ from the base
 * (i.e. --> write "base" first, then change the assignment if different)
 */
public final class AssignmentDifferenceAnalyser {

    private AssignmentDifferenceAnalyser() {
    }

    public static List<String> analyse(FxAssignment a, boolean isDerived) {
        if (a instanceof FxPropertyAssignment) {
            if (isDerived)
                return derivedPropComparison((FxPropertyAssignment) a);
            else
                return propComparison((FxPropertyAssignment) a);

        } else if (a instanceof FxGroupAssignment) {
            if (isDerived)
                return derivedGroupComparison((FxGroupAssignment) a);
            else
                return groupComparison((FxGroupAssignment) a);
        }
        return null;
    }

    /**
     * Performs a comparison between a given FxPropertyAssignment and its base property
     *
     * @param pa the FxPropertyAssignment
     * @return returns a List of String containing the builder map keys of differences
     */
    private static List<String> propComparison(FxPropertyAssignment pa) {
        final FxProperty prop = pa.getProperty();
        final List<String> result = new ArrayList<String>();

        if (prop.getACL() != pa.getACL())
            result.add("acl");

        final FxValue propDefault = prop.getDefaultValue();
        final FxValue paDefault = pa.getDefaultValue();
        if (propDefault != null && paDefault != null) {
            if (!propDefault.getBestTranslation().equals(paDefault.getBestTranslation()))
                result.add("defaultValue");
        } else if (propDefault == null && paDefault != null) {
            result.add("defaultValue");
        }

        if(compareTranslations(prop.getHint(), prop.getHint().getTranslatedLanguages(), pa.getHint(), pa.getHint().getTranslatedLanguages()))
            result.add("hint");

        if(compareTranslations(prop.getLabel(), prop.getLabel().getTranslatedLanguages(), pa.getLabel(), pa.getLabel().getTranslatedLanguages()))
            result.add("label");

        if (prop.getMultiLines() != pa.getMultiLines())
            result.add("multiline");

        final int min = pa.getMultiplicity().getMin();
        final int max = pa.getMultiplicity().getMax();
        if (prop.getMultiplicity().getMin() != min
                || prop.getMultiplicity().getMax() != max)
            result.add("multiplicity");

        if (prop.isMultiLang() != pa.isMultiLang())
            result.add("multilang");

        if (prop.getMaxLength() != pa.getMaxLength())
            result.add("maxLength");

        if (prop.isInOverview() != pa.isInOverview())
            result.add("inOverview");

        if (prop.isSearchable() != pa.isSearchable()) {
            result.add("searchable");
        }

        if (prop.isUseHTMLEditor() != pa.isUseHTMLEditor()) {
            result.add("useHtmlEditor");
        }

        // FxStructureOption differences
        List<FxStructureOption> propOptions = prop.getOptions();
        List<FxStructureOption> aOptions = pa.getOptions();

        if (aOptions.size() != propOptions.size()) {
            result.add("structureoptions");
        } else {
            Outer:
            for (FxStructureOption propOpt : propOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (propOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != propOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!propOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Compares a given FxPropertyAssignment with the settings of the assignment it was derived from
     *
     * @param pa the given FxPropertyAssignment
     * @return the List of builder map keys which are different
     */
    private static List<String> derivedPropComparison(FxPropertyAssignment pa) {
        final List<String> result = new ArrayList<String>();

        final FxPropertyAssignment base = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(pa.getBaseAssignmentId());

        if (base.getACL() != pa.getACL())
            result.add("acl");

        final FxValue baseDefault = base.getDefaultValue();
        final FxValue paDefault = pa.getDefaultValue();
        if (baseDefault != null && paDefault != null) {
            if (!baseDefault.getBestTranslation().equals(paDefault.getBestTranslation()))
                result.add("defaultValue");
        } else if (baseDefault == null && paDefault != null) {
            result.add("defaultValue");
        }

        if(compareTranslations(base.getHint(), base.getHint().getTranslatedLanguages(), pa.getHint(), pa.getHint().getTranslatedLanguages()))
            result.add("hint");

        if(compareTranslations(base.getLabel(), base.getLabel().getTranslatedLanguages(), pa.getLabel(), pa.getLabel().getTranslatedLanguages()))
            result.add("label");

        if (base.getMultiLines() != pa.getMultiLines())
            result.add("multiline");

        final int min = pa.getMultiplicity().getMin();
        final int max = pa.getMultiplicity().getMax();
        if (base.getMultiplicity().getMin() != min
                || base.getMultiplicity().getMax() != max)
            result.add("multiplicity");

        if (base.isMultiLang() != pa.isMultiLang())
            result.add("multilang");

        if (base.getMaxLength() != pa.getMaxLength())
            result.add("maxLength");

        if (base.isInOverview() != pa.isInOverview())
            result.add("inOverview");

        if (base.isSearchable() != pa.isSearchable()) {
            result.add("searchable");
        }

        if (base.isUseHTMLEditor() != pa.isUseHTMLEditor()) {
            result.add("useHtmlEditor");
        }

        // comparisons unique to assignments
        if (base.getDefaultMultiplicity() != pa.getDefaultMultiplicity()) {
            result.add("defaultMultiplicity");
        }

        if (!base.getAlias().equalsIgnoreCase(pa.getAlias())) {
            result.add("alias");
        }

        if (base.isEnabled() != pa.isEnabled()) {
            result.add("enabled");
        }

        if (base.getDefaultLanguage() != pa.getDefaultLanguage()) {
            result.add("defaultLanguage");
        }

        // FxStructureOption differences
        List<FxStructureOption> propOptions = base.getOptions();
        List<FxStructureOption> aOptions = pa.getOptions();

        if (aOptions.size() != propOptions.size()) {
            result.add("structureoptions");
        } else {
            Outer:
            for (FxStructureOption propOpt : propOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (propOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != propOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!propOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }
        }

        return result;

    }

    /**
     * Performs a comparison between a given FxGroupAssignment and its base group
     *
     * @param ga the FxGroupAssignment
     * @return returns a List of String containing the builder map keys of differences
     */
    private static List<String> groupComparison(FxGroupAssignment ga) {
        final FxGroup group = ga.getGroup();
        List<String> result = new ArrayList<String>();

        if(compareTranslations(group.getHint(), group.getHint().getTranslatedLanguages(), ga.getHint(), ga.getHint().getTranslatedLanguages()))
            result.add("hint");

        if(compareTranslations(group.getLabel(), group.getLabel().getTranslatedLanguages(), ga.getLabel(), ga.getLabel().getTranslatedLanguages()))
            result.add("label");

        final int min = ga.getMultiplicity().getMin();
        final int max = ga.getMultiplicity().getMax();
        if (group.getMultiplicity().getMin() != min
                || group.getMultiplicity().getMax() != max)
            result.add("multiplicity");

        List<FxStructureOption> groupOptions = group.getOptions();
        List<FxStructureOption> aOptions = ga.getOptions();

        if (aOptions.size() != groupOptions.size()) {
            result.add("structureoptions");
        } else {
            Outer:
            for (FxStructureOption groupOpt : groupOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (groupOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != groupOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!groupOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Compares a given FxGroupAssignment with the settings of the assignment it was derived from
     *
     * @param ga the given FxPropertyAssignment
     * @return the List of builder map keys which are different
     */
    private static List<String> derivedGroupComparison(FxGroupAssignment ga) {
        final List<String> result = new ArrayList<String>();
        final FxGroupAssignment base;
        try {
            base = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(ga.getBaseAssignmentId());
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }

        if (compareTranslations(base.getHint(), base.getHint().getTranslatedLanguages(), ga.getHint(), ga.getHint().getTranslatedLanguages()))
            result.add("hint");

        if (compareTranslations(base.getLabel(), base.getLabel().getTranslatedLanguages(), ga.getLabel(), ga.getLabel().getTranslatedLanguages()))
            result.add("label");

        final int min = ga.getMultiplicity().getMin();
        final int max = ga.getMultiplicity().getMax();
        if (base.getMultiplicity().getMin() != min
                || base.getMultiplicity().getMax() != max)
            result.add("multiplicity");

        if (base.getDefaultMultiplicity() != ga.getDefaultMultiplicity())
            result.add("defaultMultiplicity");

        if (!base.getAlias().equalsIgnoreCase(ga.getAlias()))
            result.add("alias");

        if (base.isEnabled() != ga.isEnabled())
            result.add("enabled");

        if (base.getMode().getId() != ga.getMode().getId())
            result.add("groupMode");

        List<FxStructureOption> groupOptions = base.getOptions();
        List<FxStructureOption> aOptions = ga.getOptions();

        if (aOptions.size() != groupOptions.size()) {
            result.add("structureoptions");
        } else {
            Outer:
            for (FxStructureOption groupOpt : groupOptions) {
                for (FxStructureOption aOpt : aOptions) {
                    if (groupOpt.getKey().equals(aOpt.getKey())) {
                        if (aOpt.getIntValue() != groupOpt.getIntValue()) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    } else {
                        if (!groupOptions.contains(aOpt)) {
                            result.add("structureoptions");
                            break Outer;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Compares the language arrays
     *
     * @param s1 FxString obj 1
     * @param languages1 long[] array of lang ids
     * @param s2 FxString obj 2
     * @param languages2 long[] array of lang ids
     * @return true if the language arrays differ
     */
    private static boolean compareTranslations(FxString s1, long[] languages1, FxString s2, long[] languages2) {
        if (languages1.length != languages2.length) {
            return true;
        } else {
            // convert and compare
            final List<Long> languages1List = new ArrayList<Long>(languages1.length);
            final List<Long> languages2List = new ArrayList<Long>(languages2.length);
            for(long l : languages1)
                languages1List.add(l);
            for(long l : languages2)
                languages2List.add(l);
            Collections.sort(languages2List);
            Collections.sort(languages1List);

            for (int i = 0; i < languages1List.size(); i++) {
                final long l1 = languages1List.get(i);
                final long l2 = languages2List.get(i);
                if (l1 != l2)
                    return true;
                if (!s1.getTranslation(l1).equals(s2.getTranslation(l2)))
                    return true;
            }
        }
        return false;
    }
}
