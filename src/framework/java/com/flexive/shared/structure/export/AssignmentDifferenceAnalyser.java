/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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

import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.CacheAdmin;

import java.util.List;
import java.util.ArrayList;

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

        final String propHint = prop.getHint().getDefaultTranslation();
        final String paHint = pa.getHint().getDefaultTranslation();
        if (propHint != null && paHint != null) {
            if (!propHint.equals(paHint))
                result.add("hint");
        } else if (propHint == null && paHint != null) {
            result.add("hint");
        }

        if (!prop.getLabel().getDefaultTranslation().equals(pa.getLabel().getDefaultTranslation()))
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

        final String baseHint = base.getHint().getDefaultTranslation();
        final String paHint = pa.getHint().getDefaultTranslation();
        if (baseHint != null && paHint != null) {
            if (!baseHint.equals(paHint))
                result.add("hint");
        } else if (baseHint == null && paHint != null) {
            result.add("hint");
        }

        if (!base.getLabel().getDefaultTranslation().equals(pa.getLabel().getDefaultTranslation()))
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

        final String groupHint = group.getHint().getDefaultTranslation();
        final String gaHint = ga.getHint().getDefaultTranslation();
        if (groupHint != null && gaHint != null) {
            if (!groupHint.equals(gaHint))
                result.add("hint");
        } else if (groupHint == null && gaHint != null) {
            result.add("hint");
        }

        if (!group.getLabel().getDefaultTranslation().equals(ga.getLabel().getDefaultTranslation()))
            result.add("label");

        final int min = ga.getMultiplicity().getMin();
        final int max = ga.getMultiplicity().getMax();
        if (group.getMultiplicity().getMin() != min
                || group.getMultiplicity().getMax() != max)
            result.add("multiplicity");

        List<FxStructureOption> groupOptions = group.getOptions();
        List<FxStructureOption> aOptions = ga.getOptions();

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
        final FxGroupAssignment base = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(ga.getBaseAssignmentId());

        final String groupHint = base.getHint().getDefaultTranslation();
        final String gaHint = ga.getHint().getDefaultTranslation();
        if (groupHint != null && gaHint != null) {
            if (!groupHint.equals(gaHint))
                result.add("hint");
        } else if (groupHint == null && gaHint != null) {
            result.add("hint");
        }

        if (!base.getLabel().getDefaultTranslation().equals(ga.getLabel().getDefaultTranslation()))
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

        return result;
    }
}
