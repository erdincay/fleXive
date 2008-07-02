/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.conversion;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxMultiplicity;
import com.flexive.shared.structure.FxStructureOption;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * XStream converter for assignments
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class FxAssignmentConverter implements Converter {

    /**
     * Helper class storing all common assignment data returned from unmarshalling
     */
    static class AssignmentData {
        private String alias;
        private String xpath;
        private int pos;
        private boolean enabled;
        private FxMultiplicity multiplicity;
        private int defaultMultiplicity;
        private String parentAssignment;
        private FxString label;
        private FxString hint;
        private List<FxStructureOption> options;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }

        public int getPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public FxMultiplicity getMultiplicity() {
            return multiplicity;
        }

        public void setMultiplicity(FxMultiplicity multiplicity) {
            this.multiplicity = multiplicity;
        }

        public int getDefaultMultiplicity() {
            return defaultMultiplicity;
        }

        public void setDefaultMultiplicity(int defaultMultiplicity) {
            this.defaultMultiplicity = defaultMultiplicity;
        }

        public String getParentAssignment() {
            return parentAssignment;
        }

        public void setParentAssignment(String parentAssignment) {
            this.parentAssignment = parentAssignment;
        }

        public FxString getLabel() {
            return label;
        }

        public void setLabel(FxString label) {
            this.label = label;
        }

        public FxString getHint() {
            return hint;
        }

        public void setHint(FxString hint) {
            this.hint = hint;
        }

        public List<FxStructureOption> getOptions() {
            return options;
        }

        public void setOptions(List<FxStructureOption> options) {
            this.options = options;
        }

        public String toString() {
            return "AssignmentData[alias=" + alias + ",xpath=" + xpath + ",pos=" + pos + ",enabled=" + enabled +
                    ",mult.=" + multiplicity + ",defaultMult.=" + defaultMultiplicity + ",parent=" + parentAssignment +
                    ",label=" + label + ",hint=" + hint + ",#options=" + (options == null ? 0 : options.size()) + "]";
        }
    }

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxAssignment as = (FxAssignment) o;
        writer.addAttribute("alias", as.getAlias());
        writer.addAttribute("xpath", as.getXPath());
        writer.addAttribute("pos", String.valueOf(as.getPosition()));
        writer.addAttribute("enabled", String.valueOf(as.isEnabled()));
        writer.addAttribute("multiplicity", as.getMultiplicity().toString());
        writer.addAttribute("defaultMultiplicity", String.valueOf(as.getDefaultMultiplicity()));

        if (as.isDerivedAssignment())
            writer.addAttribute("parent", CacheAdmin.getEnvironment().getAssignment(as.getBaseAssignmentId()).getXPath());

        writer.startNode("label");
        ctx.convertAnother(as.getLabel());
        writer.endNode();
        writer.startNode("hint");
        ctx.convertAnother(as.getHint());
        writer.endNode();

        marshallOptions(writer, as.getOptions());
    }

    /**
     * Marshall FxStructureOption's
     *
     * @param writer  HierarchicalStreamWriter
     * @param options List<FxStructureOption>
     */
    protected void marshallOptions(HierarchicalStreamWriter writer, List<FxStructureOption> options) {
        writer.startNode("options");
        for (FxStructureOption opt : options) {
            writer.startNode("option");
            writer.addAttribute("key", opt.getKey());
            writer.addAttribute("value", opt.getValue());
            writer.addAttribute("overrideable", String.valueOf(opt.isOverrideable()));
            writer.addAttribute("set", String.valueOf(opt.isSet()));
            writer.endNode();
        }
        writer.endNode();
    }

    /**
     * Unmarshall FxStructureOption's
     *
     * @param reader HierarchicalStreamReader
     * @param ctx    UnmarshallingContext
     * @return List<FxStructureOption>
     */
    protected List<FxStructureOption> unmarshallOptions(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        if (!reader.hasMoreChildren())
            throw new FxConversionException("ex.conversion.missingNode", "options").asRuntimeException();
        reader.moveDown();
        if (!"options".equals(reader.getNodeName()))
            throw new FxConversionException("ex.conversion.wrongNode", "options", reader.getNodeName()).asRuntimeException();
        List<FxStructureOption> options = new ArrayList<FxStructureOption>(20);
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if (!"option".equals(reader.getNodeName()))
                throw new FxConversionException("ex.conversion.wrongNode", "option", reader.getNodeName()).asRuntimeException();
            options.add(new FxStructureOption(reader.getAttribute("key"), Boolean.valueOf(reader.getAttribute("overrideable")),
                    Boolean.valueOf(reader.getAttribute("set")), reader.getAttribute("value")));
            reader.moveUp();
        }
        reader.moveUp();
        return options;
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        AssignmentData data = new AssignmentData();
        //base data
        data.setAlias(reader.getAttribute("alias"));
        data.setXpath(reader.getAttribute("xpath"));
        data.setPos(Integer.parseInt(reader.getAttribute("pos")));
        data.setEnabled(Boolean.parseBoolean(reader.getAttribute("enabled")));
        data.setMultiplicity(FxMultiplicity.fromString(reader.getAttribute("multiplicity")));
        data.setDefaultMultiplicity(Integer.parseInt(reader.getAttribute("defaultMultiplicity")));
        data.setParentAssignment(reader.getAttribute("parent"));
        //label
        data.setLabel(((FxString) ConversionEngine.getFxValue("label", this, reader, ctx)));
        //hint
        data.setHint(((FxString) ConversionEngine.getFxValue("hint", this, reader, ctx)));
        //options
        data.setOptions(unmarshallOptions(reader, ctx));
        return data;
    }
}
