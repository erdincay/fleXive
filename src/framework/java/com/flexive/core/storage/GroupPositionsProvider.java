package com.flexive.core.storage;

import com.flexive.shared.FxArrayUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Wrapper for FX_CONTENT.GROUP_POS (used during instance loading). For generating the GROUP_POS field, see
 * {@link com.flexive.core.storage.GroupPositionsProvider#builder()}.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.2.0
 */
public class GroupPositionsProvider {
    private static final Log LOG = LogFactory.getLog(GroupPositionsProvider.class);
    private static final char SEP_ASSIGNMENTS = ';';
    private static final char SEP_ASS_DATA = ':';
    private static final char SEP_POS = ',';
    private static final char SEP_XMULT_POS = '=';
    private static final char SEP_XMULT = '/';

    private final Map<Long, Map<String, Integer>> positions;

    public GroupPositionsProvider(String groupPos) {
        if (StringUtils.isBlank(groupPos)) {
            this.positions = Collections.emptyMap();
        } else {
            final String[] pairs = StringUtils.split(groupPos, SEP_ASSIGNMENTS);
            this.positions = Maps.newHashMapWithExpectedSize(pairs.length);
            for (String keyValue : pairs) {
                final String[] parts = StringUtils.split(keyValue, SEP_ASS_DATA);
                if (parts.length != 2) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Invalid group positions entry: " + keyValue);
                    }
                    continue;
                }
                try {
                    final long assignmentId = Long.parseLong(parts[0]);
                    // get indices/positions mapping
                    final String[] entries = StringUtils.split(parts[1], SEP_POS);
                    Map<String, Integer> mappings = null;
                    for (String entry : entries) {
                        final String[] entryParts = StringUtils.splitPreserveAllTokens(entry, SEP_XMULT_POS);
                        if (entryParts.length != 2) {
                            if (LOG.isWarnEnabled()) {
                                LOG.warn("Invalid group positions entry: " + keyValue);
                            }
                            continue;
                        }
                        final String xmult;
                        if (StringUtils.isBlank(entryParts[0])) {
                            xmult = "1";
                        } else {
                            final String[] indices = StringUtils.splitPreserveAllTokens(entryParts[0], SEP_XMULT);
                            final StringBuilder sb = new StringBuilder();
                            for (String idx : indices) {
                                if (sb.length() > 0) {
                                    sb.append(SEP_XMULT);
                                }
                                if (StringUtils.isBlank(idx)) {
                                    sb.append(1);
                                } else {
                                    sb.append(idx);
                                }
                            }
                            xmult = sb.toString();
                        }
                        final int value = Integer.parseInt(entryParts[1]);
                        if (entries.length == 1) {
                            // optimize for the common case (group without a multiplicity of 1)
                            mappings = ImmutableMap.of(xmult, value);
                        } else {
                            if (mappings == null) {
                                mappings = Maps.newHashMapWithExpectedSize(entries.length);
                            }
                            mappings.put(xmult, value);
                        }
                    }
                    positions.put(assignmentId, mappings);
                } catch (NumberFormatException e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Invalid group positions entry: " + keyValue);
                    }
                }
            }
        }
    }

    public Map<Long, Map<String, Integer>> getPositions() {
        return positions;
    }

    public int getPosition(long assignmentId, int[] indices) {
        final Map<String, Integer> mappings = positions.get(assignmentId);
        if (mappings == null) {
            throw new IllegalArgumentException("Assignment ID not found in group positions: " + assignmentId);
        }
        final String indicesKey = FxArrayUtils.toStringArray(indices, SEP_XMULT);
        final Integer pos = mappings.get(indicesKey);
        if (pos == null) {
            throw new IllegalArgumentException("No group position found for assignment #" + assignmentId + " and indices " + indicesKey);
        }
        return pos;
    }

    /**
     * @return  a builder for recording (group) assignment positions for FX_CONTENT.GROUP_POS
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final StringBuilder out;
        private long currentAssignmedId = -1;
        private int assignmentPosCount;

        public Builder() {
            this.out = new StringBuilder(100);
        }

        public Builder startAssignment(long assignmentId) {
            if (out.length() > 0) {
                out.append(SEP_ASSIGNMENTS);
            }
            out.append(assignmentId).append(SEP_ASS_DATA);
            currentAssignmedId = assignmentId;
            assignmentPosCount = 0;
            return this;
        }

        public Builder addPos(int[] indices, int pos) {
            if (currentAssignmedId == -1) {
                throw new IllegalStateException("No assignment started");
            }
            if (assignmentPosCount > 0) {
                out.append(SEP_POS);
            }
            for (int i = 0; i < indices.length; i++) {
                final int index = indices[i];
                if (i > 0) {
                    out.append(SEP_XMULT);
                }
                if (index != 1) {
                    // 'compressed' output - skip index if it's 1 (which coverse 90% of all cases)
                    out.append(index);
                }
            }
            out.append(SEP_XMULT_POS);
            out.append(pos);
            assignmentPosCount++;
            return this;
        }

        public String build() {
            return out.toString();
        }

        public boolean isEmpty() {
            return out.length() == 0;
        }
    }
}
