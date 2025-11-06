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
package com.facebook.presto.resourcemanager;

import com.facebook.airlift.http.client.testing.TestingHttpClient;
import com.facebook.airlift.jaxrs.JsonMapper;
import com.facebook.airlift.jaxrs.testing.JaxrsTestingHttpProcessor;
import com.facebook.airlift.json.JsonCodec;
import com.facebook.airlift.json.JsonObjectMapperProvider;
import com.facebook.airlift.units.DataSize;
import com.facebook.airlift.units.Duration;
import com.facebook.presto.client.NodeVersion;
import com.facebook.presto.execution.QueryState;
import com.facebook.presto.execution.resourceGroups.ResourceGroupRuntimeInfo;
import com.facebook.presto.memory.MemoryInfo;
import com.facebook.presto.metadata.InMemoryNodeManager;
import com.facebook.presto.server.BasicQueryInfo;
import com.facebook.presto.server.BasicQueryStats;
import com.facebook.presto.server.NodeStatus;
import com.facebook.presto.server.ResourceManagerResource;
import com.facebook.presto.spi.QueryId;
import com.facebook.presto.spi.memory.ClusterMemoryPoolInfo;
import com.facebook.presto.spi.memory.MemoryPoolId;
import com.facebook.presto.spi.memory.MemoryPoolInfo;
import com.facebook.presto.spi.resourceGroups.ResourceGroupId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.facebook.airlift.http.client.HttpStatus.INTERNAL_SERVER_ERROR;
import static com.facebook.airlift.http.client.HttpStatus.OK;
import static com.facebook.airlift.http.client.testing.TestingResponse.mockResponse;
import static com.facebook.airlift.json.JsonCodec.listJsonCodec;
import static com.facebook.airlift.json.JsonCodec.mapJsonCodec;
import static com.facebook.airlift.units.DataSize.Unit.MEGABYTE;
import static com.facebook.presto.SessionTestUtils.TEST_SESSION;
import static com.facebook.presto.execution.QueryState.RUNNING;
import static com.facebook.presto.memory.LocalMemoryManager.GENERAL_POOL;
import static com.facebook.presto.metadata.SessionPropertyManager.createTestingSessionPropertyManager;
import static com.facebook.presto.operator.BlockedReason.WAITING_FOR_MEMORY;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestImplHttpResourceManagerClient
{
    private static final URI TEST_ADDRESS = URI.create("http://localhost:8080");
    InMemoryNodeManager internalNodeManager = new InMemoryNodeManager();

    private static final JsonCodec<List<ResourceGroupRuntimeInfo>> RESOURCE_GROUP_LIST_CODEC = listJsonCodec(ResourceGroupRuntimeInfo.class);
    private static final JsonCodec<Map<MemoryPoolId, ClusterMemoryPoolInfo>> MEMORY_POOL_CODEC = mapJsonCodec(MemoryPoolId.class, ClusterMemoryPoolInfo.class);

    @Test
    public void testTemporary()
    {
        ObjectMapper jsonMapper = new JsonObjectMapperProvider().get();
        InMemoryNodeManager manager = new InMemoryNodeManager();
        ResourceManagerClusterStateProvider provider = new ResourceManagerClusterStateProvider(manager, createTestingSessionPropertyManager(), 10, Duration.valueOf("4s"), Duration.valueOf("8s"), Duration.valueOf("5s"), Duration.valueOf("0s"), Duration.valueOf("4s"), true, newSingleThreadScheduledExecutor());

        ResourceManagerResource resourceManagerResource = new ResourceManagerResource(provider, listeningDecorator(Executors.newCachedThreadPool()));

        JaxrsTestingHttpProcessor processor = new JaxrsTestingHttpProcessor(
                URI.create("http://127.0.0.1:8080"),
                resourceManagerResource,
                new JsonMapper(jsonMapper));

        TestingHttpClient httpClient = new TestingHttpClient(processor);

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, manager);

        BasicQueryInfo queryInfo = createTestQueryInfo("query1", RUNNING);

        client.queryHeartbeat(Optional.of(URI.create("http://127.0.0.1:8080")), "test-node", queryInfo, 1);
    }

    @Test
    public void testQueryHeartbeat()
    {
        AtomicInteger requestCount = new AtomicInteger(0);
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            requestCount.incrementAndGet();
            assertEquals(request.getMethod(), "PUT");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/queryHeartbeat"));
            assertTrue(request.getUri().toString().contains("nodeId=test-node"));
            assertTrue(request.getUri().toString().contains("sequenceId=1"));
            return mockResponse(OK, MediaType.parse("application/json"), "");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        BasicQueryInfo queryInfo = createTestQueryInfo("query1", RUNNING);

        client.queryHeartbeat(Optional.of(TEST_ADDRESS), "test-node", queryInfo, 1);

        assertEquals(requestCount.get(), 1);
    }

    @Test
    public void testNodeHeartbeat()
    {
        AtomicInteger requestCount = new AtomicInteger(0);
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            requestCount.incrementAndGet();
            assertEquals(request.getMethod(), "PUT");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/nodeHeartbeat"));
            return mockResponse(OK, JSON_UTF_8, "");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        NodeStatus nodeStatus = createTestNodeStatus("node1");

        client.nodeHeartbeat(Optional.of(TEST_ADDRESS), nodeStatus);

        assertEquals(requestCount.get(), 1);
    }

    @Test
    public void testGetResourceGroupInfo()
    {
        List<ResourceGroupRuntimeInfo> expectedInfo = ImmutableList.of(
                ResourceGroupRuntimeInfo.builder(new ResourceGroupId("test-group"))
                        .addRunningQueries(5)
                        .addQueuedQueries(3)
                        .build());

        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            assertEquals(request.getMethod(), "GET");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/resourceGroupInfo"));
            assertTrue(request.getUri().toString().contains("excludingNode=test-node"));
            return mockResponse(OK, JSON_UTF_8, RESOURCE_GROUP_LIST_CODEC.toJson(expectedInfo));
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);

        List<ResourceGroupRuntimeInfo> result = client.getResourceGroupInfo(Optional.of(TEST_ADDRESS), "test-node");

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertEquals(result.get(0).getResourceGroupId(), new ResourceGroupId("test-group"));
        assertEquals(result.get(0).getRunningQueries(), 5);
        assertEquals(result.get(0).getQueuedQueries(), 3);
    }

    @Test
    public void testGetMemoryPoolInfo()
    {
        Map<MemoryPoolId, ClusterMemoryPoolInfo> expectedInfo = ImmutableMap.of(
                GENERAL_POOL, new ClusterMemoryPoolInfo(
                        new MemoryPoolInfo(1000, 500, 100, ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of()),
                        5,
                        2));

        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            assertEquals(request.getMethod(), "GET");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/memoryPoolInfo"));
            return mockResponse(OK, JSON_UTF_8, MEMORY_POOL_CODEC.toJson(expectedInfo));
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);

        Map<MemoryPoolId, ClusterMemoryPoolInfo> result = client.getMemoryPoolInfo(Optional.of(TEST_ADDRESS));

        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertTrue(result.containsKey(GENERAL_POOL));
        assertEquals(result.get(GENERAL_POOL).getAssignedQueries(), 2);
        assertEquals(result.get(GENERAL_POOL).getBlockedNodes(), 5);
    }

    @Test
    public void testResourceGroupRuntimeHeartbeat()
    {
        AtomicInteger requestCount = new AtomicInteger(0);
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            requestCount.incrementAndGet();
            assertEquals(request.getMethod(), "PUT");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/resourceGroupRuntimeHeartbeat"));
            assertTrue(request.getUri().toString().contains("node=test-node"));
            return mockResponse(OK, JSON_UTF_8, "");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        List<ResourceGroupRuntimeInfo> runtimeInfo = ImmutableList.of(
                ResourceGroupRuntimeInfo.builder(new ResourceGroupId("test-group"))
                        .addRunningQueries(2)
                        .build());

        client.resourceGroupRuntimeHeartbeat(Optional.of(TEST_ADDRESS), "test-node", runtimeInfo);

        assertEquals(requestCount.get(), 1);
    }

    @Test
    public void testGetRunningTaskCount()
    {
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            assertEquals(request.getMethod(), "GET");
            assertTrue(request.getUri().toString().contains("/v1/resourcemanager/getRunningTaskCount"));
            return mockResponse(OK, JSON_UTF_8, "42");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);

        int result = client.getRunningTaskCount(Optional.of(TEST_ADDRESS));

        assertEquals(result, 42);
    }

    @Test(expectedExceptions = ResourceManagerException.class)
    public void testErrorHandling()
    {
        TestingHttpClient httpClient = new TestingHttpClient(request ->
                mockResponse(INTERNAL_SERVER_ERROR, JSON_UTF_8, "Server error"));

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        NodeStatus nodeStatus = createTestNodeStatus("node1");

        client.nodeHeartbeat(Optional.of(TEST_ADDRESS), nodeStatus);
    }

    @Test(expectedExceptions = ResourceManagerException.class)
    public void testExceptionHandling()
    {
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            throw new RuntimeException("Network error");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        NodeStatus nodeStatus = createTestNodeStatus("node1");

        client.nodeHeartbeat(Optional.of(TEST_ADDRESS), nodeStatus);
    }

    @Test
    public void testBaseUriConstruction()
    {
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            String uri = request.getUri().toString();
            assertTrue(uri.startsWith("http://localhost:8080/"));
            return mockResponse(OK, JSON_UTF_8, "");
        });

        ImplHttpResourceManagerClient client = new ImplHttpResourceManagerClient(httpClient, internalNodeManager);
        client.nodeHeartbeat(Optional.of(TEST_ADDRESS), createTestNodeStatus("node1"));
    }

    private static BasicQueryInfo createTestQueryInfo(String queryId, QueryState state)
    {
        return new BasicQueryInfo(
                new QueryId(queryId),
                TEST_SESSION.toSessionRepresentation(),
                Optional.of(new ResourceGroupId("test-group")),
                state,
                GENERAL_POOL,
                true,
                URI.create("http://localhost"),
                "",
                new BasicQueryStats(
                        DateTime.now().getMillis(),
                        DateTime.now().getMillis(),
                        new Duration(1, SECONDS),
                        new Duration(1, SECONDS),
                        new Duration(1, SECONDS),
                        new Duration(1, SECONDS),
                        new Duration(1, SECONDS),
                        1, 1, 1, 1, 1, 100, 1, 1, 1, 100, 1, 1, 1, 100,
                        new DataSize(1, MEGABYTE),
                        1, 1, 1,
                        new DataSize(1, MEGABYTE),
                        new DataSize(1, MEGABYTE),
                        new DataSize(1, MEGABYTE),
                        new DataSize(1, MEGABYTE),
                        new DataSize(1, MEGABYTE),
                        new DataSize(1, MEGABYTE),
                        new Duration(1, SECONDS),
                        new Duration(1, SECONDS),
                        true,
                        ImmutableSet.of(WAITING_FOR_MEMORY),
                        new DataSize(1, MEGABYTE),
                        OptionalDouble.of(1.0)),
                null,
                Optional.empty(),
                ImmutableList.of(),
                Optional.empty());
    }

    private static NodeStatus createTestNodeStatus(String nodeId)
    {
        return new NodeStatus(
                nodeId,
                new NodeVersion("test"),
                "test-env",
                false,
                new Duration(1, SECONDS),
                "http://external",
                "http://internal",
                new MemoryInfo(new DataSize(1, MEGABYTE), ImmutableMap.of()),
                1, 1.0, 2.0, 1, 2, 3);
    }
}
