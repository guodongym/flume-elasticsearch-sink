package com.cognitree.flume.sink.elasticsearch.client;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.*;

import java.io.IOException;

/**
 * Created on 2019/4/24.
 *
 * @author zhaogd
 */
@Ignore
public class ElasticsearchClientBuilderTest {

    private RestHighLevelClient client;
    private ElasticsearchClientBuilder clientBuilder;

    @Before
    public void setUp() {
        clientBuilder = new ElasticsearchClientBuilder("es-cluster", new String[]{"192.168.1.109:9200"});
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void build() throws IOException {
        client = clientBuilder.build();
        final boolean ping = client.ping(RequestOptions.DEFAULT);
        Assert.assertTrue(ping);
    }
}