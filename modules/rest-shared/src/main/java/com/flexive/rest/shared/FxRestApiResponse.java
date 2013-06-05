/**
 * This file is part of the [fleXive](R) framework.
 *
 * Copyright (c) 1999-2013
 * UCS - unique computing solutions gmbh (http://www.ucs.at)
 * All rights reserved
 *
 * The [fleXive](R) project is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public
 * License version 2.1 or higher as published by the Free Software Foundation.
 *
 * The GNU Lesser General Public License can be found at
 * http://www.gnu.org/licenses/lgpl.html.
 * A copy is found in the textfile LGPL.txt and important notices to the
 * license from the author are found in LICENSE.txt distributed with
 * these libraries.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For further information about UCS - unique computing solutions gmbh,
 * please see the company website: http://www.ucs.at
 *
 * For further information about [fleXive](R), please see the
 * project website: http://www.flexive.org
 *
 *
 * This copyright notice MUST APPEAR in all copies of the file!
 */
package com.flexive.rest.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * The standard response envelope.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxRestApiResponse {

    protected ResponseStatus status;
    protected Map<String, Object> body;

    /**
     * No-arg constructor for deserialization only
     */
    public FxRestApiResponse() {
    }

    protected FxRestApiResponse(ResponseStatus status, Map<String, Object> body) {
        this.body = body;
        this.status = status;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }

    public static FxRestApiResponse ok(Map<String, Object> body) {
        return new FxRestApiResponse(new StatusOK(), body);
    }

    public static FxRestApiResponse error(String message) {
        return new FxRestApiResponse(new StatusError(message), null);
    }

    public static FxRestApiResponse error(String code, String message) {
        return new FxRestApiResponse(new StatusError(code, message), null);
    }


    public static class ResponseStatus {
        private String code;
        private long timestampMillis;
        private long durationMillis;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        protected String message;

        /**
         * No-arg constructor for deserialization only
         */
        public ResponseStatus() {
        }

        protected ResponseStatus(String code) {
            this.code = code;
            this.timestampMillis = System.currentTimeMillis();
        }

        public String getCode() {
            return code;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public void setDurationMillis(long duration) {
            this.durationMillis = duration;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @JsonIgnore
        public boolean isSuccess() {
            return FxRestApiConst.STATUS_OK.equals(code);
        }
    }

    public static class StatusOK extends ResponseStatus {
        public StatusOK() {
            super(FxRestApiConst.STATUS_OK);
        }
    }

    public static class StatusError extends ResponseStatus {
        public StatusError(String message) {
            super(FxRestApiConst.STATUS_ERROR_GENERIC);
            this.message = message;
        }

        public StatusError(String code, String message) {
            super(code);
            this.message = message;
        }
    }
}
