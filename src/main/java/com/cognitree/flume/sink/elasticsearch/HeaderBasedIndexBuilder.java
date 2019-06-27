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

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;

import java.util.Map;

import static com.cognitree.flume.sink.elasticsearch.Constants.*;

/**
 * This class create the index type and Id based on header
 *
 * @author zhaogd
 */
public class HeaderBasedIndexBuilder extends StaticIndexBuilder {

    /**
     * Returns the index name from the headers
     */
    @Override
    public String getIndex(Event event) {
        Map<String, String> headers = event.getHeaders();
        String index;
        if (headers.get(INDEX) != null) {
            index = headers.get(INDEX);
        } else {
            index = super.getIndex(event);
        }
        return index;
    }

    /**
     * Returns the index Id from the headers.
     */
    @Override
    public String getId(Event event) {
        Map<String, String> headers = event.getHeaders();
        return headers.get(ID);
    }

    /**
     * 返回处理类型
     */
    @Override
    public ActionTypeEnum getActionType(Event event) {
        Map<String, String> headers = event.getHeaders();
        final String type = headers.get(ACTION);
        if (StringUtils.isBlank(type)) {
            return super.getActionType(event);
        }
        return ActionTypeEnum.fromString(type);
    }

    @Override
    public void configure(Context context) {
        super.configure(context);
    }
}
