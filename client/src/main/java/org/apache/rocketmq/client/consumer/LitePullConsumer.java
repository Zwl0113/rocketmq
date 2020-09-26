/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.consumer;

import java.util.Collection;
import java.util.List;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * @author weidian
 */
public interface LitePullConsumer {

    /**
     * Start the consumer
     * 开启consumer
     */
    void start() throws MQClientException;

    /**
     * Shutdown the consumer
     * 关闭consumer
     */
    void shutdown();

    /**
     * Subscribe some topic with subExpression
     * 使用订阅表达式订阅一些topic
     *
     * @param subExpression subscription expression.it only support or operation such as "tag1 || tag2 || tag3" <br> if
     * null or * expression,meaning subscribe all
     * @throws MQClientException if there is any client error.
     */
    void subscribe(final String topic, final String subExpression) throws MQClientException;

    /**
     * Subscribe some topic with selector.
     * 使用消息选择器订阅topic
     *
     * @param selector message selector({@link MessageSelector}), can be null.
     * @throws MQClientException if there is any client error.
     */
    void subscribe(final String topic, final MessageSelector selector) throws MQClientException;

    /**
     * Unsubscribe consumption some topic
     * 取消订阅
     *
     * @param topic Message topic that needs to be unsubscribe.
     */
    void unsubscribe(final String topic);

    /**
     * Manually assign a list of message queues to this consumer. This interface does not allow for incremental
     * assignment and will replace the previous assignment (if there is one).
     * 管理consumer分配的消息队列，这个接口不允许增量分配如果有则替换
     *
     * @param messageQueues Message queues that needs to be assigned.
     *                      需要被分配的消息队列
     */
    void assign(Collection<MessageQueue> messageQueues);

    /**
     * Fetch data for the topics or partitions specified using assign API
     * 使用分配API从topic或partition中拉取数据
     *
     * @return list of message, can be null.
     */
    List<MessageExt> poll();

    /**
     * Fetch data for the topics or partitions specified using assign API
     * 使用分配API从topic或partition中拉取数据（携带超时参数）
     *
     * @param timeout The amount time, in milliseconds, spent waiting in poll if data is not available. Must not be
     * negative
     * @return list of message, can be null.
     */
    List<MessageExt> poll(long timeout);

    /**
     * Overrides the fetch offsets that the consumer will use on the next poll. If this API is invoked for the same
     * message queue more than once, the latest offset will be used on the next poll(). Note that you may lose data if
     * this API is arbitrarily used in the middle of consumption.
     * 重置消费者将在下一次拉取的位点，如果这个方法被同一个mq超过一次调用，下一次的拉取则会使用最近的位点
     * 注意如果在消费的过程中任意使用改方法则可能会造成丢数据
     *
     * @param messageQueue
     * @param offset
     */
    void seek(MessageQueue messageQueue, long offset) throws MQClientException;

    /**
     * Suspend pulling from the requested message queues.
     *
     * Because of the implementation of pre-pull, fetch data in {@link #poll()} will not stop immediately until the
     * messages of the requested message queues drain.
     *
     * Note that this method does not affect message queue subscription. In particular, it does not cause a group
     * rebalance.
     * 暂停拉取消息
     * 由于前置拉取的实现，拉取数据将不会立即终止直到已经请求的消息队列耗尽
     * 注意这个方法不会影响消息队列的订阅，尤其不会使消费分组负载均衡
     *
     *
     * @param messageQueues Message queues that needs to be paused.
     */
    void pause(Collection<MessageQueue> messageQueues);

    /**
     * Resume specified message queues which have been paused with {@link #pause(Collection)}.
     * 重新消费已经被中断的mq
     *
     * @param messageQueues Message queues that needs to be resumed.
     */
    void resume(Collection<MessageQueue> messageQueues);

    /**
     * Whether to enable auto-commit consume offset.
     * 是否开启自动提交消费位点
     *
     * @return true if enable auto-commit, false if disable auto-commit.
     */
    boolean isAutoCommit();

    /**
     * Set whether to enable auto-commit consume offset.
     * 设置自动提交消费位点
     *
     * @param autoCommit Whether to enable auto-commit.
     */
    void setAutoCommit(boolean autoCommit);

    /**
     * Get metadata about the message queues for a given topic.
     * 根据topic获取指定的消息队列
     *
     *
     * @param topic The topic that need to get metadata.
     * @return collection of message queues
     * @throws MQClientException if there is any client error.
     */
    Collection<MessageQueue> fetchMessageQueues(String topic) throws MQClientException;

    /**
     * Look up the offsets for the given message queue by timestamp. The returned offset for each message queue is the
     * earliest offset whose timestamp is greater than or equal to the given timestamp in the corresponding message
     * queue.
     * 通过时间戳查找给定消息队列的位点，返回的每个消息队列的位点是最早的位点，条件是时间戳大于或等于给定的消息队列的时间戳标记
     *
     * @param messageQueue Message queues that needs to get offset by timestamp.
     * @param timestamp
     * @return offset
     * @throws MQClientException if there is any client error.
     */
    Long offsetForTimestamp(MessageQueue messageQueue, Long timestamp) throws MQClientException;

    /**
     * Manually commit consume offset.
     * 提交位点
     */
    void commitSync();

    /**
     * Get the last committed offset for the given message queue.
     * 获取指定消息队列的最近一次的提交位点
     *
     * @param messageQueue
     * @return offset, if offset equals -1 means no offset in broker.
     * @throws MQClientException if there is any client error.
     */
    Long committed(MessageQueue messageQueue) throws MQClientException;

    /**
     * Register a callback for sensing topic metadata changes.
     * 对指定topic注册一个监听器用于监听发送元数据的变化
     *
     * @param topic The topic that need to monitor.
     * @param topicMessageQueueChangeListener Callback when topic metadata changes, refer {@link
     * TopicMessageQueueChangeListener}
     * @throws MQClientException if there is any client error.
     */
    void registerTopicMessageQueueChangeListener(String topic,
        TopicMessageQueueChangeListener topicMessageQueueChangeListener) throws MQClientException;

    /**
     * Update name server addresses.
     * 更新nameSrv的地址
     */
    void updateNameServerAddress(String nameServerAddress);

    /**
     * Overrides the fetch offsets with the begin offset that the consumer will use on the next poll. If this API is
     * invoked for the same message queue more than once, the latest offset will be used on the next poll(). Note that
     * you may lose data if this API is arbitrarily used in the middle of consumption.
     * 用使用者在下一次拉取的开始偏移量覆盖拉取偏移量，如果这个方法被相同的mq超过一次调用，则下一次poll会使用最近一次的偏移量，注意如果
     * 这个方法在消费中被随意调用则可能会丢数据
     *
     * @param messageQueue
     */
    void seekToBegin(MessageQueue messageQueue)throws MQClientException;

    /**
     * Overrides the fetch offsets with the end offset that the consumer will use on the next poll. If this API is
     * invoked for the same message queue more than once, the latest offset will be used on the next poll(). Note that
     * you may lose data if this API is arbitrarily used in the middle of consumption.
     * 用使用者在下一次拉取的结束偏移量覆盖拉取偏移量，如果这个方法被相同的mq超过一次调用，则下一次poll会使用最近一次的偏移量，注意如果
     * 这个方法在消费中被随意调用则可能会丢数据
     *
     *
     * @param messageQueue
     */
    void seekToEnd(MessageQueue messageQueue)throws MQClientException;
}
