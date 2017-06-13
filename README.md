**Elasticsearch Sink**

The sink reads events from a channel, serializes them into json documents and batches them into a bulk processor.
Bulk processor batches the writes to elasticsearch as per configuration.

The elasticsearch index and type for each event can be defined statically in the configuration file or can be derived dynamically using a custom IndexBuilder.

By default, events are assumed to be in json format.
This assumption can be overridden by implementing the Serializer interface.

Follow these steps to use this sink in Apache flume:

1. Build the plugin

`mvn clean assembly:assembly`

This command will create the zip file inside the target directory.

2. Extract the file into the flume installation directories plugin.d folder.

3. Configure the sink in the flume configuration file with properties as below

Required properties are in bold.

| Property Name                              | Default      | Description                                                                                     |
|--------------------------------------------|:--------------:|:----------------------------------------------------------------------------------------------|
| **channel**                                | -              |                                                                                               |
| **type**                                   | -              | The component type name, needs to be com.cognitree.flume.sink.elasticsearch.ElasticSearchSink |
| **es.cluster.name**                        | elasticsearch  | Name of the elastic search cluster to connect to                                              |
| **es.client.hosts**                        | -              | Comma separated hostname:port value i.e. host1:9300,host2:9300, Default port is 9300                                | 
| es.bulkActions                             | 1000           | Execute the bulk every mentioned requests                                                     |
| es.bulkProcessor.name                      | flume          | Name of the bulk processor                                                                    |
| es.bulkSize                                | 5              | Flush the bulk request every mentioned size                                                   |
| es.bulkSize.unit                           | MB             | Bulk request unit                                                                             |
| es.concurrent.request                      | 1              |  Concurrent request is allowed to be executed while accumulating new bulk requests.           |
| es.flush.interval.time                     | 10s            | Flush the bulk request every mentioned seconds whatever the number of requests                |
| es.backoff.policy.time.interval            | 50M            | Backoff policy time interval, wait initially for the 50 mili seconds                          |
| es.backoff.policy.retries                  | 8              | Backoff policy retries                                                                        |
| es.client.transport.sniff                  | false          | To enable or disable the sniff feature of the elastic search                                  |
| es.client.transport.ignore_cluster_name    | false          | To ignore cluster name validation of connected nodes                                          |
| es.client.transport.ping_timeout           | 5s             | The time to wait for a ping response from a node                                              |
| es.client.transport.nodes_sampler_interval | 5s             | How often to sample / ping the nodes listed and connected                                     |
| es.index                                   | default        | Index name to be used to store the documents                                                  |
| es.type                                    | default        | Type to be used to store the documents                                                        |
| es.index.builder                           |com.cognitree.flume.sink.elasticsearch.StaticIndexBuilder          | Implementation of com.cognitree.flume.sink.elasticsearch.IndexBuilder interface accepted |
| es.serializer                      |com.cognitree.flume.sink.elasticsearch.SimpleSerializer            | Implementation of com.cognitree.flume.sink.elasticsearch.Serializer interface accepted |


Example of agent named agent
````
  agent.channels = es_channel
  agent.sinks = es_sink
  agent.sinks.es_sink.type=com.cognitree.flume.sink.elasticsearch.ElasticSearchSink
  agent.sinks.es_sink.es.bulkActions=5
  agent.sinks.es_sink.es.bulkProcessor.name=bulkprocessor
  agent.sinks.es_sink.es.bulkSize=5
  agent.sinks.es_sink.es.bulkSize.unit=MB
  agent.sinks.es_sink.es.concurrent.request=1
  agent.sinks.es_sink.es.flush.interval.time=5m
  agent.sinks.es_sink.es.backoff.policy.time.interval=50M
  agent.sinks.es_sink.es.backoff.policy.retries=8
  agent.sinks.es_sink.es.cluster.name=es-cluster
  agent.sinks.es_sink.es.client.transport.sniff=false
  agent.sinks.es_sink.es.client.transport.ignore_cluster_name=false
  agent.sinks.es_sink.es.client.transport.ping_timeout=5s
  agent.sinks.es_sink.es.client.transport.nodes_sampler_interval=5s
  agent.sinks.es_sink.es.client.hostName=localhost
  agent.sinks.es_sink.es.client.port=9300
  agent.sinks.es_sink.es.index.name=defaultindex
  agent.sinks.es_sink.es.index.type=defaulttype
  agent.sinks.es_sink.es.index.builder=com.cognitree.flume.sink.elasticsearch.HeaderBasedIndexBuilder
  agent.sinks.es_sink.es.serializer=com.cognitree.flume.sink.elasticsearch.SimpleSerializer
````