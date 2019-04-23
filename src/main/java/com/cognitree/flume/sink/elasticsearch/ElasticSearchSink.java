/*
 * Copyright 2017 Cognitree Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.cognitree.flume.sink.elasticsearch;

import com.cognitree.flume.sink.elasticsearch.client.BulkProcessorBuilder;
import com.cognitree.flume.sink.elasticsearch.client.ElasticsearchClientBuilder;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.instrumentation.SinkCounter;
import org.apache.flume.sink.AbstractSink;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.cognitree.flume.sink.elasticsearch.Constants.*;

/**
 * This sink will read the events from a channel and add them to the bulk processor.
 * <p>
 * This sink must be configured with mandatory parameters detailed in
 * {@link Constants}
 *
 * @author zhaogd
 */
public class ElasticSearchSink extends AbstractSink implements Configurable {

    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchSink.class);

    private static final int CHECK_CONNECTION_PERIOD = 3000;

    private long batchSize;

    private ElasticsearchClientBuilder clientBuilder;

    private BulkProcessorBuilder bulkProcessorBuilder;

    private BulkProcessor bulkProcessor;

    private IndexBuilder indexBuilder;

    private Serializer serializer;

    private RestHighLevelClient client;

    private AtomicBoolean shouldBackOff = new AtomicBoolean(false);

    private SinkCounter sinkCounter;

    @Override
    public void configure(Context context) {
        final String hostString = context.getString(ES_HOSTS);
        Preconditions.checkNotNull(hostString,
                "es.client.hosts cannot be empty, please specify in configuration file");

        String[] hosts = hostString.split(COMMA);
        Preconditions.checkArgument(ArrayUtils.isNotEmpty(hosts),
                "es.client.hosts cannot be empty, please specify in configuration file");

        batchSize = context.getLong(CONFIG_BATCHSIZE, 1000L);

        final String clusterName = context.getString(PREFIX + ES_CLUSTER_NAME, DEFAULT_ES_CLUSTER_NAME);

        clientBuilder = new ElasticsearchClientBuilder(clusterName, hosts);
        bulkProcessorBuilder = new BulkProcessorBuilder(context, this);

        buildIndexBuilder(context);
        buildSerializer(context);

        // 实例化计数器
        sinkCounter = new SinkCounter(this.getName());
    }

    @Override
    public Status process() {
        if (shouldBackOff.get()) {
            throw new NoNodeAvailableException("Check whether Elasticsearch is down or not.");
        }
        Channel channel = getChannel();
        Transaction txn = channel.getTransaction();
        try {
            txn.begin();
            // 循环处理达到批次大小开始提交
            long i = 0;
            for (; i < batchSize; i++) {
                Event event = channel.take();
                if (event == null) {
                    if (i == 0) {
                        sinkCounter.incrementBatchEmptyCount();
                        return Status.BACKOFF;
                    } else {
                        sinkCounter.incrementBatchUnderflowCount();
                    }
                    break;
                }
                String body = new String(event.getBody(), Charsets.UTF_8);
                if (Strings.isNullOrEmpty(body)) {
                    logger.warn("body is null or empty");
                    continue;
                }

                logger.debug("start to sink event [{}].", body);
                String index = indexBuilder.getIndex(event);
                String id = indexBuilder.getId(event);

                XContentBuilder xContentBuilder = serializer.serialize(event);
                if (xContentBuilder == null) {
                    logger.error("Could not serialize the event body [{}] for index [{}], id [{}] ", body, index, id);
                    continue;
                }

                if (Strings.isNullOrEmpty(id)) {
                    bulkProcessor.add(new IndexRequest(index).source(xContentBuilder));
                } else {
                    bulkProcessor.add(new IndexRequest(index).id(id).source(xContentBuilder));
                }
                logger.debug("sink event [{}] successfully.", body);
            }

            if (i == batchSize) {
                sinkCounter.incrementBatchCompleteCount();
            }
            sinkCounter.addToEventDrainAttemptCount(i);

            txn.commit();
            sinkCounter.addToEventDrainSuccessCount(i);
            return Status.READY;
        } catch (Throwable tx) {
            try {
                txn.rollback();
            } catch (Exception ex) {
                logger.error("exception in rollback.", ex);
            }
            logger.error("transaction rolled back.", tx);
            return Status.BACKOFF;
        } finally {
            txn.close();
        }
    }

    @Override
    public synchronized void start() {
        client = clientBuilder.build();
        bulkProcessor = bulkProcessorBuilder.build(client);

        super.start();
        sinkCounter.incrementConnectionCreatedCount();
        sinkCounter.start();
    }

    @Override
    public synchronized void stop() {
        if (bulkProcessor != null) {
            bulkProcessor.flush();
            try {
                bulkProcessor.awaitClose(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.error("bulkProcessor 关闭异常", e);
            }
        }
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                logger.error("client 关闭异常", e);
            }
        }

        sinkCounter.incrementConnectionClosedCount();
        sinkCounter.stop();
        super.stop();
    }

    /**
     * builds Index builder
     */
    private void buildIndexBuilder(Context context) {
        String indexBuilderClass = DEFAULT_ES_INDEX_BUILDER;
        if (StringUtils.isNotBlank(context.getString(ES_INDEX_BUILDER))) {
            indexBuilderClass = context.getString(ES_INDEX_BUILDER);
        }
        this.indexBuilder = instantiateClass(indexBuilderClass);
        if (this.indexBuilder != null) {
            this.indexBuilder.configure(context);
        }
    }

    /**
     * builds Serializer
     */
    private void buildSerializer(Context context) {
        String serializerClass = DEFAULT_ES_SERIALIZER;
        if (StringUtils.isNotEmpty(context.getString(ES_SERIALIZER))) {
            serializerClass = context.getString(ES_SERIALIZER);
        }
        this.serializer = instantiateClass(serializerClass);
        if (this.serializer != null) {
            this.serializer.configure(context);
        }
    }

    private <T> T instantiateClass(String className) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> aClass = (Class<T>) Class.forName(className);
            return aClass.newInstance();
        } catch (Exception e) {
            logger.error("Could not instantiate class " + className, e);
            Throwables.propagate(e);
            return null;
        }
    }

    /**
     * Checks for elasticsearch connection
     * Sets shouldBackOff to true if bulkProcessor failed to deliver the request.
     * Resets shouldBackOff to false once the connection to elasticsearch is established.
     */
    public void assertConnection() {
        shouldBackOff.set(true);
        final Timer timer = new Timer();
        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (checkConnection()) {
                        shouldBackOff.set(false);
                        timer.cancel();
                        timer.purge();
                    }
                } catch (IOException e) {
                    logger.error("ping request for elasticsearch failed " + e.getMessage(), e);
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, CHECK_CONNECTION_PERIOD);
    }

    private boolean checkConnection() throws IOException {
        return client.ping(RequestOptions.DEFAULT);
    }
}
