/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.strimzi.kproxy.filter;

import org.apache.kafka.common.message.MetadataRequestData;
import org.apache.kafka.common.message.ProduceRequestData;
import org.apache.kafka.common.protocol.ApiKeys;

import io.strimzi.kproxy.codec.DecodedRequestFrame;

/**
 * <p>Interface for {@code *RequestFilter}s.
 * This interface can be implemented in two ways:
 * <ul>
 *     <li>filter classes can (multiply) implement one of the RPC-specific subinterfaces such as {@link ProduceRequestFilter} for a type-safe API</li>
 *     <li>filter classes can extend {@link KrpcGenericRequestFilter}</li>
 * </ul>
 *
 * <p>When implementing one or more of the {@code *RequestFilter} subinterfaces you need only implement
 * the {@code on*Request} method(s), unless your filter can avoid deserialization in which case
 * you can override {@link #shouldDeserializeRequest(ApiKeys, short)} as well.</p>
 *
 * <p>When extending {@link KrpcGenericRequestFilter} you need to override {@link #apply(DecodedRequestFrame, KrpcFilterContext)},
 * and may override {@link #shouldDeserializeRequest(ApiKeys, short)} as well.</p>
 *
 * <h3>Guarantees</h3>
 * <p>Implementors of this API may assume the following:</p>
 * <ol>
 *     <li>That each instance of the filter is associated with a single channel</li>
 *     <li>That {@link #shouldDeserializeRequest(ApiKeys, short)} and
 *     {@link #apply(DecodedRequestFrame, KrpcFilterContext)} (or {@code on*Request} as appropriate)
 *     will always be invoked on the same thread.</li>
 *     <li>That filters are applied in the order they were configured.</li>
 * </ol>
 * <p>From 1. and 2. it follows that you can use member variables in your filter to
 * store channel-local state.</p>
 *
 * <p>Implementors should <strong>not</strong> assume:</p>
 * <ol>
 *     <li>That filters in the same chain execute on the same thread. Thus inter-filter communication/state
 *     transfer needs to be thread-safe</li>
 * </ol>
 */
public /* sealed */ interface KrpcRequestFilter extends KrpcFilter /* TODO permits ... */ {

    /**
     * Apply the filter to the given {@code decodedFrame} using the given {@code filterContext}.
     * @param decodedFrame The request frame.
     * @param filterContext The filter context.
     * @return The state of the filter.
     */
    public default KrpcFilterState apply(DecodedRequestFrame<?> decodedFrame,
                                         KrpcFilterContext filterContext) {
        KrpcFilterState state;
        switch (decodedFrame.apiKey()) {
            case METADATA:
                state = ((MetadataRequestFilter) this).onMetadataRequest((MetadataRequestData) decodedFrame.body(), filterContext);
                break;
            case PRODUCE:
                state = ((ProduceRequestFilter) this).onProduceRequest((ProduceRequestData) decodedFrame.body(), filterContext);
                break;
            default:
                throw new IllegalStateException("Unsupported RPC " + decodedFrame.apiKey());
            // TODO and so on (generate this code)
        }
        return state;
    }

    /**
     * <p>Determines whether a request with the given {@code apiKey} and {@code apiVersion} should be deserialized.
     * Note that it is not guaranteed that this method will be called once per request,
     * or that two consecutive calls refer to the same request.
     * That is, the sequences of invocations like the following are allowed:</p>
     * <ol>
     *     <li>{@code shouldDeserializeRequest} on request A</li>
     *     <li>{@code shouldDeserializeRequest} on request B</li>
     *     <li>{@code shouldDeserializeRequest} on request A</li>
     *     <li>{@code apply} on request A</li>
     *     <li>{@code apply} on request B</li>
     * </ol>
     * @param apiKey The API key
     * @param apiVersion The API version
     * @return
     */
    default boolean shouldDeserializeRequest(ApiKeys apiKey, short apiVersion) {
        switch (apiKey) {
            case PRODUCE:
                return this instanceof ProduceRequestFilter;
            case METADATA:
                return this instanceof MetadataRequestFilter;
            case FETCH:
                return this instanceof FetchRequestFilter;
            case API_VERSIONS:
                return this instanceof ApiVersionsRequestFilter;
            case SASL_AUTHENTICATE:
                return this instanceof SaslAuthenticateRequestFilter;
            case SASL_HANDSHAKE:
                return this instanceof SaslHandshakeRequestFilter;
            // TODO and so on
            default:
                throw new IllegalStateException("Unsupported API key " + apiKey);
        }
    }
}