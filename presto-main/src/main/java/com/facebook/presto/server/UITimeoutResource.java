/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.server;

import com.facebook.airlift.log.Logger;
import com.facebook.drift.annotations.ThriftConstructor;
import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftStruct;
import com.facebook.presto.sql.analyzer.FeaturesConfig;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.Immutable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.util.Objects;

import static com.facebook.presto.server.security.RoleType.ADMIN;
import static com.facebook.presto.server.security.RoleType.USER;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Objects.requireNonNull;

@Path("/")
@RolesAllowed({ADMIN, USER})
public class UITimeoutResource
{
    private final long uiTimeoutMillis;
    Logger log = Logger.get(UITimeoutResource.class);

    @Inject
    public UITimeoutResource(FeaturesConfig featuresConfig)
    {
        requireNonNull(featuresConfig, "featuresConfig is null");
        uiTimeoutMillis = featuresConfig.getUITimeout().toMillis();
    }

    @GET
    @Path("/v1/ui/logout")
    public void logout(@Context HttpServletResponse response)
            throws IOException
    {
        log.debug("Logging out user");
        response.setHeader(WWW_AUTHENTICATE, "Basic realm=\"Presto\"");
        response.sendError(SC_UNAUTHORIZED);
        response.setContentType("text/plain");
        response.getWriter().write("");
        response.getWriter().flush();
    }

    @GET
    @Path("/v1/ui/timeout")
    @Produces(APPLICATION_JSON)
    public TimeoutDto timeout()
    {
        return new TimeoutDto(uiTimeoutMillis);
    }

    @Immutable
    @ThriftStruct
    public static class TimeoutDto
    {
        private final long timeout;

        @ThriftConstructor
        @JsonCreator
        public TimeoutDto(@JsonProperty("timeout") long timeout)
        {
            this.timeout = timeout;
        }

        @ThriftField(1)
        @JsonProperty
        public long getTimeout()
        {
            return timeout;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TimeoutDto that = (TimeoutDto) o;
            return timeout == that.timeout;
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(timeout);
        }

        @Override
        public String toString()
        {
            return "TimeoutDto{" +
                    "timeout=" + timeout +
                    '}';
        }
    }
}
