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
package com.cognitree.flume.sink.elasticsearch.client;

import com.cognitree.flume.sink.elasticsearch.Constants;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.cognitree.flume.sink.elasticsearch.Constants.DEFAULT_ES_PORT;

/**
 * This class creates  an instance of the {@link RestHighLevelClient}
 * Set the hosts for the client
 *
 * @author zhaogd
 */
public class ElasticsearchClientBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchClientBuilder.class);

    private String clusterName;

    private List<HttpHost> httpHosts;

    public ElasticsearchClientBuilder(String clusterName, String[] hostNames) {
        this.clusterName = clusterName;
        setHttpHosts(hostNames);
    }

    private void setHttpHosts(String[] hostNames) {
        this.httpHosts = new ArrayList<>(hostNames.length);
        for (String hostName : hostNames) {
            String[] hostDetails = hostName.split(Constants.COLONS);
            String address = hostDetails[0];
            int port = hostDetails.length > 1 ? Integer.parseInt(hostDetails[1]) : DEFAULT_ES_PORT;
            httpHosts.add(new HttpHost(address, port));
        }
    }

    public RestHighLevelClient build() {
        logger.info("Cluster Name: [{}], HostName: [{}] ", clusterName, httpHosts);
        return new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[]{})));
    }
}