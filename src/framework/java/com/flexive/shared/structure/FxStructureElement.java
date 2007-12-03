/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.shared.structure;

import com.flexive.shared.SelectableObject;
import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.List;

/**
 * Abstract base class for FxGroup and FxProperty
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class FxStructureElement implements Serializable, SelectableObject {
    private static final long serialVersionUID = -5878281678610887810L;
    private long id;
    protected String name;
    protected FxString label;
    protected FxString hint;
    protected FxMultiplicity multiplicity;
    protected boolean overrideMultiplicity;
    protected boolean referenced;
    protected List<FxStructureOption> options;

    /**
     * Constructor
     *
     * @param id                       id
     * @param name                     name
     * @param label                    label
     * @param hint                     hint
     * @param overrideMultiplicity may override base multiplicity
     * @param multiplicity             the multiplicity
     * @param options                  options
     */
    protected FxStructureElement(long id, String name, FxString label, FxString hint, boolean overrideMultiplicity,
                                 FxMultiplicity multiplicity, List<FxStructureOption> options) {
        this.id = id;
        this.name = name;
        this.label = label;
        this.hint = hint;
        this.overrideMultiplicity = overrideMultiplicity;
        this.multiplicity = multiplicity;
        this.referenced = false;
        this.options = options;
        if (this.options == null)
            this.options = FxStructureOption.getEmptyOptionList(1);
    }

    /**
     * Internal id of this element
     *
     * @return internal id of this element
     */
    public long getId() {
        return id;
    }

    /**
     * Name of this element (may be overriden with an alias)
     *
     * @return name of this element
     */
    public String getName() {
        return name;
    }

    /**
     * Multilingual label of this element
     *
     * @return label of this element
     */
    public FxString getLabel() {
        return label;
    }


    /**
     * Get hint text for UI
     *
     * @return hint text for UI
     */
    public FxString getHint() {
        return hint;
    }

    /**
     * Indicate if assignments of this element may override the default multiplicity
     *
     * @return indicate if assignments of this element may override the default multiplicity
     */
    public boolean mayOverrideBaseMultiplicity() {
        return overrideMultiplicity;
    }

    /**
     * (Base) multiplicity of this element, may be overriden in assignment depending on <code>mayOverrideMultiplicity</code>
     *
     * @return multiplicity of this element
     * @see com.flexive.shared.structure.FxStructureElement#mayOverrideBaseMultiplicity()
     */
    public FxMultiplicity getMultiplicity() {
        return multiplicity;
    }

    /**
     * Is this element referenced? (=in use)
     *
     * @return if this element is referenced
     */
    public boolean isReferenced() {
        return referenced;
    }

    /**
     * Set if this element is referenced (not to be called 'by hand'!!!)
     *
     * @param referenced is referenced
     */
    public void setReferenced(boolean referenced) {
        this.referenced = referenced;
    }

    /**
     * Check if an option is set for the requested key
     *
     * @param key option key
     * @return if an option is set for the requested key
     */
    public boolean hasOption(String key) {
        return FxStructureOption.hasOption(key, options);
    }

    /**
     * Get an option entry for the given key, if the key is invalid or not found a <code>FxStructureOption</code> object
     * will be returned with <code>set</code> set to <code>false</code>, overrideable set to <code>false</code> and value
     * set to an empty String.
     *
     * @param key option key
     * @return the found option or an object that indicates that the option is not set
     */
    public FxStructureOption getOption(String key) {
        return FxStructureOption.getOption(key, options);
    }
}
