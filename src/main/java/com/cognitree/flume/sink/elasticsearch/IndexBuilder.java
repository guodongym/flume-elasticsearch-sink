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

import org.apache.flume.Event;
import org.apache.flume.conf.Configurable;

/**
 * Interface to select an index, type and id for each event.
 * A single instance of the class is created when the Sink initializes and is destroyed when the Sink is stopped.
 * Config params can be taken through Configurable
 *
 * @author zhaogd
 */
public interface IndexBuilder extends Configurable {


    /**
     * 动态获取index名称
     *
     * @param event event
     * @return indexName
     */
    String getIndex(Event event);

    /**
     * 动态获取ID
     *
     * @param event event
     * @return id
     */
    String getId(Event event);

    /**
     * 返回处理类型
     *
     * @param event event
     * @return {@link com.cognitree.flume.sink.elasticsearch.Constants.ActionTypeEnum}
     */
    Constants.ActionTypeEnum getActionType(Event event);
}
