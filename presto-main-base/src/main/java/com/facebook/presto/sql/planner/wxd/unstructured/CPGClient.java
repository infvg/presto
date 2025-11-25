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
package com.facebook.presto.sql.planner.wxd.unstructured;

import com.facebook.airlift.log.Logger;
import com.facebook.presto.common.QualifiedObjectName;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class CPGClient
{
    private static final Logger log = Logger.get(CPGClient.class);
    private static final String LH_INSTANCE_ID_CPD = "LH_INSTANCE_ID";
    private static final String MDS_REST_URL = "MDS_REST_URL";
    private static final String LH_INSTANCE_ID_SAAS = "ID";
    private static final String LH_INSTANCE_ID_ROKS = "CRN";
    private static final String LH_INSTANCE_ID_LITE = "REAL_INSTANCE_ID";
    private static final String LH_INSTANCE_ID_PATH_LITE = "REAL_INSTANCE_ID_PATH";
    private static final String LH_INSTANCE_SECRET = "LH_INSTANCE_SECRET";
    private static final String ACL_STORAGE_API = "/lakehouse/api/v3/acl_storage";
    private static String baseUrl;
    private final LoadingCache<String, QualifiedObjectName> aclTableCache;
    private final LoadingCache<String, Boolean> isUnstructuredTableCache;
    private final LoadingCache<String, HashSet<String>> groupDetailsCache;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String lhInstanceSecret;
    private final String lhInstanceId;
    private final String lhContext;

    public CPGClient()
    {
        this.lhContext = System.getenv("LH_CONTEXT");
        this.lhInstanceSecret = getLhInstanceSecret();
        this.lhInstanceId = getLhInstanceId(lhContext);
        this.httpClient = new OkHttpClient();
        this.aclTableCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, QualifiedObjectName>()
                {
                    @Override
                    public QualifiedObjectName load(String bearerToken)
                    {
                        try {
                            return fetchAclFromRemote(bearerToken);
                        }
                        catch (Exception e) {
                            log.error(e, "Exception occurred in load function for getting ACL table Name ");
                            throw new RuntimeException("ACL table name read fails", e);
                        }
                    }
                });
        this.isUnstructuredTableCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, Boolean>()
                {
                    @Override
                    public Boolean load(String cacheKey)
                    {
                        try {
                            return fetchUnstructuredStatusFromRemote(cacheKey);
                        }
                        catch (Exception e) {
                            log.error(e, "Exception occurred in load function");
                            return false;
                        }
                    }
                });
        this.groupDetailsCache = CacheBuilder.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<String, HashSet<String>>()
                {
                    @Override
                    public HashSet<String> load(String bearerToken)
                            throws Exception
                    {
                        Set<String> result = fetchGroupDetailsFromRemote(bearerToken);
                        return new HashSet<>(result);
                    }
                });
    }

    public boolean isUnstructuredTable(String bearerToken, QualifiedObjectName qualifiedTableName)
    {
        String cacheKey = bearerToken + "_token_" + qualifiedTableName.toString();
        try {
            return isUnstructuredTableCache.get(cacheKey);
        }
        catch (Exception e) {
            log.error(e, "Failed to load unstructured table status ");
            return false;
        }
    }

    public QualifiedObjectName getaclTable(String bearerToken)
    {
        try {
            return aclTableCache.get(bearerToken);
        }
        catch (Exception e) {
            log.warn(e, "Failed to fetch ACL table");
            throw new RuntimeException("Malformed response", e);
        }
    }

    public Set<String> getGroupDetails(String bearerToken)
    {
        try {
            return groupDetailsCache.get(bearerToken);
        }
        catch (Exception e) {
            log.error(e, "Failed to load group details");
            return Collections.emptySet();
        }
    }

    private QualifiedObjectName fetchAclFromRemote(String bearerToken)
    {
        log.debug("Starting to fetch ACL remote details...");

        String consoleUrl = System.getenv("CONSOLE_API_URL");
        if (consoleUrl == null || consoleUrl.isEmpty()) {
            log.error("CRITICAL: The CONSOLE_API_URL environment variable is not set or is empty.");
            throw new RuntimeException("CONSOLE_API_URL environment variable is not set or is empty");
        }
        log.debug("Using base console URL: %s", consoleUrl);

        String apiEndpoint = String.format("%s%s", consoleUrl, ACL_STORAGE_API);
        log.debug("Constructed API endpoint for ACL storage: %s", apiEndpoint);
        log.debug("Using LhInstanceId: %s", lhInstanceId);

        Request request = new Request.Builder()
                .url(apiEndpoint)
                .addHeader("secret", lhInstanceSecret)
                .addHeader("LhInstanceId", lhInstanceId)
                .build();
        log.debug("Built request for %s with required headers.", apiEndpoint);

        // Execute the API call
        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                throw new RuntimeException("Response body is null for request: " + apiEndpoint);
            }
            String responseBody = response.body().string();
            log.debug("Received response from API with status code: %d", response.code());
            log.debug("Full response body: %s", responseBody);
            String aclCatalogName;

            if (response.isSuccessful()) {
                log.debug("Response was successful. Proceeding to parse the body.");
                AclCatalogResponseDto responseDto = objectMapper.readValue(responseBody, AclCatalogResponseDto.class);
                aclCatalogName = extractFirstCatalogName(responseDto);

                if (aclCatalogName == null || aclCatalogName.isEmpty()) {
                    log.error("API response was successful, but the ACL catalog name was null or empty in the response body.");
                    throw new RuntimeException("ACL catalog name is missing in API response");
                }
                log.debug("Successfully extracted ACL Catalog Name: '%s'", aclCatalogName);

                String schema = System.getenv("ACL_SCHEMA");
                String table = System.getenv("ACL_TABLE");

                if (schema == null || table == null) {
                    log.error("CRITICAL: The ACL_SCHEMA or ACL_TABLE environment variable is missing.");
                    throw new RuntimeException("ACL schema/table name is null/empty");
                }
                log.debug("Using ACL_SCHEMA='%s' and ACL_TABLE='%s' from environment.", schema, table);

                //  Build and return the final object
                QualifiedObjectName finalObjectName = new QualifiedObjectName(aclCatalogName, schema, table);
                log.debug("Successfully constructed QualifiedObjectName: %s", finalObjectName);
                return finalObjectName;
            }
            else {
                log.error("Request failed with code: %d. The response body contains details.", response.code());
                throw new RuntimeException(format("Failed to fetch ACL catalog from API. HTTP status: %d", response.code()));
            }
        }
        catch (IOException ex) {
            log.error(ex, "Console call to %s failed", ACL_STORAGE_API);
            throw new RuntimeException("Failed to fetch ACL catalog", ex);
        }
    }

    private boolean fetchUnstructuredStatusFromRemote(String cacheKey)
    {
        String[] parts = cacheKey.split("_token_", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cache key format");
        }
        String qualifiedTableName = parts[1];
        List<String> encodedParts;
        try {
            encodedParts = Arrays.stream(qualifiedTableName.split("\\."))
                    .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8))
                    .collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error(e, "Table name encoding failed");
            throw new RuntimeException("Error encoding table name", e);
        }
        String baseUrl = getBaseUrl();
        String endPoint = format(
                "/mds/metadata/v1/catalogs/%s/schemas/%s/tables/%s/properties/unstructured_flag",
                encodedParts.get(0), encodedParts.get(1), encodedParts.get(2));
        String apiEndpoint = format("%s%s", baseUrl, endPoint);
        Request request = new Request.Builder().url(apiEndpoint)
                .addHeader("secret", lhInstanceSecret)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                UnstructuredTableResponseDto unstructuredTableResponseDto = objectMapper.readValue(response.body().string(), UnstructuredTableResponseDto.class);
                log.debug("Secret:: " + lhInstanceSecret);
                log.debug("Is unstructured flag :: " + unstructuredTableResponseDto.toString());
                return unstructuredTableResponseDto.isUnstructured();
            }
            else {
                log.debug("For API endpoint [%s]. response received [%d]. IsUnstructured flag checkf failed returning false", apiEndpoint, response.code());

                return false;
            }
        }
        catch (IOException ex) {
            log.error(ex, "MDS call to /mds/metadata/v1/catalogs failed for table %s", qualifiedTableName);
            return false;
        }
    }

    private Set<String> fetchGroupDetailsFromRemote(String bearerToken)
    {
        String apiEndpoint = format("%s/v1/access/user_groups", getBaseUrl());
        Request request = addRequestHeaders(new Request.Builder().url(apiEndpoint), bearerToken)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                GroupListResponseDto groupListResponseDto = objectMapper.readValue(response.body().string(), GroupListResponseDto.class);
                return groupListResponseDto.getGroups();
            }
            else {
                return Collections.emptySet();
            }
        }
        catch (IOException ex) {
            log.error(ex, "CPG call to /v1/access/user_groups failed: %s");
            throw new RuntimeException("Malformed response from CPG", ex);
        }
    }

    public Request.Builder addRequestHeaders(Request.Builder builder, String bearerToken)
    {
        log.debug("cpglogs:lhInstanceId = %s", lhInstanceId);
        return builder
                .addHeader("Authorization", "Bearer " + bearerToken)
                .addHeader("LhInstanceId", lhInstanceId)
                .addHeader("Content-Type", "application/json");
    }

    public String getBaseUrl()
    {
        String mdsUrl = System.getenv(MDS_REST_URL);
        if (isSwContext(lhContext)) {
            baseUrl = mdsUrl;
        }
        else {
            try {
                baseUrl = Files.readString(Paths.get(mdsUrl)).trim();
                if (baseUrl.isEmpty()) {
                    baseUrl = mdsUrl;
                }
            }
            catch (Exception e) {
                log.error(e, "Error reading the config file");
                baseUrl = mdsUrl;
            }
        }
        return baseUrl;
    }

    public String getLhInstanceId(String lhContext)
    {
        if (isSwContext(lhContext)) {
            return System.getenv(LH_INSTANCE_ID_CPD);
        }
        String instanceId = System.getenv(LH_INSTANCE_ID_LITE);
        if (instanceId != null && !instanceId.isEmpty()) {
            return instanceId;
        }
        String instanceIdPath = System.getenv(LH_INSTANCE_ID_PATH_LITE);
        if (instanceIdPath != null && !instanceIdPath.isEmpty()) {
            try {
                String fileContent = Files.readString(Paths.get(instanceIdPath)).trim();
                if (!fileContent.isEmpty()) {
                    return fileContent;
                }
            }
            catch (IOException e) {
                log.debug("Instance Id path variable is empty,returning ID environment variable");
            }
        }
        String instanceIdRoks = System.getenv(LH_INSTANCE_ID_ROKS);
        if (instanceIdRoks != null && !instanceIdRoks.isEmpty()) {
            return instanceIdRoks;
        }
        return System.getenv(LH_INSTANCE_ID_SAAS);
    }

    public String getLhInstanceSecret()
    {
        String lhInstanceSecret = System.getenv(LH_INSTANCE_SECRET);
        String lHinstanceSecretSaaS;
        if (isSwContext(lhContext)) {
            return lhInstanceSecret;
        }
        else {
            try {
                lHinstanceSecretSaaS = Files.readString(Paths.get(lhInstanceSecret)).trim();
                if (lHinstanceSecretSaaS.isEmpty()) {
                    lHinstanceSecretSaaS = lhInstanceSecret;
                }
            }
            catch (Exception e) {
                log.error(e, "Error reading the config file");
                lHinstanceSecretSaaS = lhInstanceSecret;
            }
        }
        return lHinstanceSecretSaaS;
    }

    private String extractFirstCatalogName(AclCatalogResponseDto dto)
    {
        log.debug("Attempting to extract the first catalog name from the response DTO...");

        if (dto == null || dto.getAssociatedCatalogs() == null || dto.getAssociatedCatalogs().isEmpty()) {
            log.warn("The 'associated_catalogs' list was null or empty. No catalog name to extract.");
            return null;
        }

        List<AclCatalogResponseDto.AssociatedCatalog> associatedCatalogs = dto.getAssociatedCatalogs();
        log.debug("Found {} associated catalog(s).", associatedCatalogs.size());
        log.debug("Catalogs: {}", associatedCatalogs);

        AclCatalogResponseDto.AssociatedCatalog firstCatalog = associatedCatalogs.get(0);
        if (firstCatalog == null) {
            log.warn("The first item in the associated_catalogs list is null.");
            return null;
        }

        String catalogName = firstCatalog.getCatalogName();
        log.debug("Extracted catalog name: '{}'", catalogName);
        return catalogName;
    }

    private static boolean isSwContext(String context)
    {
        return "sw_dev".equals(context) || "sw_ent".equals(context) || "sw_env".equals(context);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AclCatalogResponseDto
    {
        @JsonProperty("associated_catalogs")
        private List<AssociatedCatalog> associatedCatalogs;

        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class AssociatedCatalog
        {
            @JsonProperty("catalog_name")
            private String catalogName;

            public String getCatalogName()
            {
                return catalogName;
            }
        }

        public List<AssociatedCatalog> getAssociatedCatalogs()
        {
            return associatedCatalogs;
        }

        public void setAssociatedCatalogs(List<AssociatedCatalog> associatedCatalogs)
        {
            this.associatedCatalogs = associatedCatalogs;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class UnstructuredTableResponseDto
    {
        private final boolean isUnstructured;

        @JsonCreator
        public UnstructuredTableResponseDto(@JsonProperty("data") String data)
        {
            this.isUnstructured = Boolean.parseBoolean(data);
        }

        @JsonProperty
        public boolean isUnstructured()
        {
            return isUnstructured;
        }

        @Override
        public String toString()
        {
            return "UnstructuredTableResponseDto{ " +
                    ",Introduce local variable" + isUnstructured +
                    '}';
        }
    }

    private static class GroupListResponseDto
    {
        private final String groupList;

        @JsonCreator
        public GroupListResponseDto(@JsonProperty("user_groups") String groupList)
        {
            this.groupList = groupList;
        }

        @JsonProperty
        public Set<String> getGroups()
        {
            if (groupList == null || groupList.isEmpty()) {
                return Collections.emptySet();
            }
            return Arrays.stream(groupList.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        @Override
        public String toString()
        {
            return "GroupListResponseDto{ " +
                    ", Group list=" + groupList +
                    '}';
        }
    }
}
