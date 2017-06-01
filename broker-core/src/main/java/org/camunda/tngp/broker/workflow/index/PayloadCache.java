/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.tngp.broker.workflow.index;

import static org.camunda.tngp.util.EnsureUtil.ensureNotNull;

import org.agrona.DirectBuffer;
import org.camunda.tngp.broker.logstreams.processor.HashIndexSnapshotSupport;
import org.camunda.tngp.broker.workflow.data.WorkflowInstanceEvent;
import org.camunda.tngp.hashindex.Long2LongHashIndex;
import org.camunda.tngp.hashindex.store.IndexStore;
import org.camunda.tngp.logstreams.log.LogStreamReader;
import org.camunda.tngp.logstreams.log.LoggedEvent;
import org.camunda.tngp.logstreams.spi.SnapshotSupport;

/**
 * Cache of workflow instance payload. It contains an LRU cache of the payload
 * and an index which holds the position of the payload events.
 *
 * <p>
 * When a payload is requested then the it is returned from the cache. If it is
 * not present in the cache then the payload event is seek in the log stream.
 */
public class PayloadCache
{
    private final WorkflowInstanceEvent workflowInstanceEvent = new WorkflowInstanceEvent();

    private final Long2LongHashIndex index;
    private final HashIndexSnapshotSupport<Long2LongHashIndex> snapshotSupport;

    private final ExpandableBufferLruCache cache;
    private final LogStreamReader logStreamReader;

    public PayloadCache(IndexStore indexStore, int cacheSize, LogStreamReader logStreamReader)
    {
        this.index = new Long2LongHashIndex(indexStore, Short.MAX_VALUE, 256);
        this.snapshotSupport = new HashIndexSnapshotSupport<>(index, indexStore);

        this.logStreamReader = logStreamReader;
        this.cache = new ExpandableBufferLruCache(cacheSize, 1024, this::lookupPayload);
    }

    private DirectBuffer lookupPayload(long position)
    {
        DirectBuffer payload = null;

        final boolean found = logStreamReader.seek(position);
        if (found && logStreamReader.hasNext())
        {
            final LoggedEvent event = logStreamReader.next();

            workflowInstanceEvent.reset();
            event.readValue(workflowInstanceEvent);

            payload = workflowInstanceEvent.getPayload();
        }

        return payload;
    }

    public DirectBuffer getPayload(long workflowInstanceKey)
    {
        DirectBuffer payload = null;

        final long position = index.get(workflowInstanceKey, -1L);

        if (position > 0)
        {
            payload = cache.lookupBuffer(position);
        }

        ensureNotNull("payload", payload);
        return payload;
    }

    public void addPayload(long workflowInstanceKey, long payloadEventPosition)
    {
        index.put(workflowInstanceKey, payloadEventPosition);
    }

    public void remove(long workflowInstanceKey)
    {
        index.remove(workflowInstanceKey, -1L);
    }

    public SnapshotSupport getSnapshotSupport()
    {
        return snapshotSupport;
    }

}
