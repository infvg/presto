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
package com.facebook.presto.execution;

import com.facebook.airlift.log.Logger;
import org.weakref.jmx.Managed;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.facebook.presto.spi.ConnectorId.INFORMATION_SCHEMA_CONNECTOR_PREFIX;
import static com.facebook.presto.spi.ConnectorId.SYSTEM_TABLES_CONNECTOR_PREFIX;
import static com.facebook.presto.spi.ConnectorId.isInternalSystemConnector;

public class QueryCatalogStats
{
    private static final Logger log = Logger.get(QueryCatalogStats.class);
    private final ConcurrentMap<String, AtomicInteger> queriesByCatalog = new ConcurrentHashMap<>();

    public QueryCatalogStats()
    {}

    public void updateQueriesByCatalogDistribution(QueryInfo queryInfo)
    {
        queryInfo.getInputs().forEach(input -> {
            String catalog = extractCatalog(input);
            log.debug("catalog: " + catalog + " updated for query id: " + queryInfo.getQueryId().getId());
            if (!queriesByCatalog.containsKey(catalog)) {
                queriesByCatalog.put(catalog, new AtomicInteger());
            }
            queriesByCatalog.get(catalog).incrementAndGet();
        });
    }

    private String extractCatalog(Input input)
    {
        String catalog = input.getConnectorId().getCatalogName();
        if (isInternalSystemConnector(input.getConnectorId())) {
            if (input.getConnectorId().getCatalogName().startsWith(INFORMATION_SCHEMA_CONNECTOR_PREFIX)) {
                catalog = catalog.substring(INFORMATION_SCHEMA_CONNECTOR_PREFIX.length());
            }
            else {
                catalog = catalog.substring(SYSTEM_TABLES_CONNECTOR_PREFIX.length());
            }
        }
        return catalog;
    }

    @Managed
    public ConcurrentMap<String, AtomicInteger> getQueriesByCatalog()
    {
        return queriesByCatalog;
    }
}
