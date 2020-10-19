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
package org.apache.rocketmq.store;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageExtBatch;
import org.apache.rocketmq.store.config.BrokerRole;
import org.apache.rocketmq.store.stats.BrokerStatsManager;

/**
 * This class defines contracting interfaces to implement, allowing third-party vendor to use customized message store.
 * 该类定义了要实现的接口，允许第三方来自定义MessageStore
 */
public interface MessageStore {

    /**
     * Load previously stored messages.
     * 在存储消息之前加载
     *
     * @return true if success; false otherwise.
     */
    boolean load();

    /**
     * Launch this message store.
     * 加载MessageStore
     *
     * @throws Exception if there is any error.
     */
    void start() throws Exception;

    /**
     * Shutdown this message store.
     * 关闭MessageStore
     */
    void shutdown();

    /**
     * Destroy this message store. Generally, all persistent files should be removed after invocation.
     * 销毁MessageStore，所有已存在在的文件在这个方法被调用之后都会被删除
     */
    void destroy();

    /** Store a message into store in async manner, the processor can process the next request
     *  rather than wait for result
     *  when result is completed, notify the client in async manner
     *  异步模式存储单条消息，线程可以处理下一个请求而不是去等待着结果返回，当结果处理完成会异步的方式通知客户端
     *
     * @param msg MessageInstance to store
     * @return a CompletableFuture for the result of store operation
     */
    default CompletableFuture<PutMessageResult> asyncPutMessage(final MessageExtBrokerInner msg) {
        return CompletableFuture.completedFuture(putMessage(msg));
    }

    /**
     * Store a batch of messages in async manner
     * 异步方式存储批量消息
     * @param messageExtBatch the message batch
     * @return a CompletableFuture for the result of store operation
     */
    default CompletableFuture<PutMessageResult> asyncPutMessages(final MessageExtBatch messageExtBatch) {
        return CompletableFuture.completedFuture(putMessages(messageExtBatch));
    }

    /**
     * Store a message into store.
     * 同步方式存储单条消息
     *
     * @param msg Message instance to store
     * @return result of store operation.
     */
    PutMessageResult putMessage(final MessageExtBrokerInner msg);

    /**
     * Store a batch of messages.
     * 同步方式存储批量消息
     *
     * @param messageExtBatch Message batch.
     * @return result of storing batch messages.
     */
    PutMessageResult putMessages(final MessageExtBatch messageExtBatch);

    /**
     * Query at most <code>maxMsgNums</code> messages belonging to <code>topic</code> at <code>queueId</code> starting
     * from given <code>offset</code>. Resulting messages will further be screened using provided message filter.
     *
     * @param group Consumer group that launches this query.
     * @param topic Topic to query.
     * @param queueId Queue ID to query.
     * @param offset Logical offset to start from.
     * @param maxMsgNums Maximum count of messages to query.
     * @param messageFilter Message filter used to screen desired messages.
     * @return Matched messages.
     */
    GetMessageResult getMessage(final String group, final String topic, final int queueId,
        final long offset, final int maxMsgNums, final MessageFilter messageFilter);

    /**
     * Get maximum offset of the topic queue.
     * 获取队列中的最大逻辑位点
     *
     * @param topic Topic name.
     * @param queueId Queue ID.
     * @return Maximum offset at present.
     */
    long getMaxOffsetInQueue(final String topic, final int queueId);

    /**
     * Get the minimum offset of the topic queue.
     * 获取队列中的最小位点
     *
     * @param topic Topic name.
     * @param queueId Queue ID.
     * @return Minimum offset at present.
     */
    long getMinOffsetInQueue(final String topic, final int queueId);

    /**
     * Get the offset of the message in the commit log, which is also known as physical offset.
     * 获取commitLog中已知的物理位点
     *
     * @param topic Topic of the message to lookup.
     * @param queueId Queue ID.
     * @param consumeQueueOffset offset of consume queue.
     * @return physical offset.
     */
    long getCommitLogOffsetInQueue(final String topic, final int queueId, final long consumeQueueOffset);

    /**
     * Look up the physical offset of the message whose store timestamp is as specified.
     * 根据指定的存储时间查询物理位点（二分查找）
     *
     * @param topic Topic of the message.
     * @param queueId Queue ID.
     * @param timestamp Timestamp to look up.
     * @return physical offset which matches.
     */
    long getOffsetInQueueByTime(final String topic, final int queueId, final long timestamp);

    /**
     * Look up the message by given commit log offset.
     * 根据commitLog的位点查询消息
     *
     * @param commitLogOffset physical offset.
     * @return Message whose physical offset is as specified.
     */
    MessageExt lookMessageByOffset(final long commitLogOffset);

    /**
     * Get one message from the specified commit log offset.
     * 指定的commitLog位点获取单条消息
     *
     * @param commitLogOffset commit log offset.
     * @return wrapped result of the message.
     */
    SelectMappedBufferResult selectOneMessageByOffset(final long commitLogOffset);

    /**
     * Get one message from the specified commit log offset.
     * 根据指定的commitLog位点和消息大小获取单条消息
     *
     * @param commitLogOffset commit log offset.
     * @param msgSize message size.
     * @return wrapped result of the message.
     */
    SelectMappedBufferResult selectOneMessageByOffset(final long commitLogOffset, final int msgSize);

    /**
     * Get the running information of this store.
     * 获取store的运行时信息
     *
     * @return message store running info.
     */
    String getRunningDataInfo();

    /**
     * Message store runtime information, which should generally contains various statistical information.
     * 获取store的运行时信息，这些信息封装了一些可用的统计参数
     *
     * @return runtime information of the message store in format of key-value pairs.
     */
    HashMap<String, String> getRuntimeInfo();

    /**
     * Get the maximum commit log offset.
     * 获取最大的物理位点
     *
     * @return maximum commit log offset.
     */
    long getMaxPhyOffset();

    /**
     * Get the minimum commit log offset.
     * 获取最小的物理位点
     *
     * @return minimum commit log offset.
     */
    long getMinPhyOffset();

    /**
     * Get the store time of the earliest message in the given queue.
     * 指定队列获取最早的消息存储时间
     *
     * @param topic Topic of the messages to query.
     * @param queueId Queue ID to find.
     * @return store time of the earliest message.
     */
    long getEarliestMessageTime(final String topic, final int queueId);

    /**
     * Get the store time of the earliest message in this store.
     * 获取整个存储系统的最早消息的存储时间
     *
     * @return timestamp of the earliest message in this store.
     */
    long getEarliestMessageTime();

    /**
     * Get the store time of the message specified.
     * 获取指定消息的存储时间
     *
     * @param topic message topic.
     * @param queueId queue ID.
     * @param consumeQueueOffset consume queue offset.
     * @return store timestamp of the message.
     */
    long getMessageStoreTimeStamp(final String topic, final int queueId, final long consumeQueueOffset);

    /**
     * Get the total number of the messages in the specified queue.
     * 指定队列中获取总的消息数
     *
     * @param topic Topic
     * @param queueId Queue ID.
     * @return total number.
     */
    long getMessageTotalInQueue(final String topic, final int queueId);

    /**
     * Get the raw commit log data starting from the given offset, which should used for replication purpose.
     * 从给定的位点中获取一行commitLog数据，该数据用于复制
     *
     * @param offset starting offset.
     * @return commit log data.
     */
    SelectMappedBufferResult getCommitLogData(final long offset);

    /**
     * Append data to commit log.
     * 追加数据到commitLog中
     *
     * @param startOffset starting offset.
     * @param data data to append.
     * @return true if success; false otherwise.
     */
    boolean appendToCommitLog(final long startOffset, final byte[] data);

    /**
     * Execute file deletion manually.
     * 手动执行文件删除
     *
     */
    void executeDeleteFilesManually();

    /**
     * Query messages by given key.
     * 给定key查询消息
     *
     * @param topic topic of the message.
     * @param key message key.
     * @param maxNum maximum number of the messages possible.
     * @param begin begin timestamp.
     * @param end end timestamp.
     */
    QueryMessageResult queryMessage(final String topic, final String key, final int maxNum, final long begin,
        final long end);

    /**
     * Update HA master address.
     * 更新HA主节点地址
     *
     * @param newAddr new address.
     */
    void updateHaMasterAddress(final String newAddr);

    /**
     * Return how much the slave falls behind.
     * 返回从节点落后多少
     *
     * @return number of bytes that slave falls behind.
     */
    long slaveFallBehindMuch();

    /**
     * Return the current timestamp of the store.
     * 返回存储系统的当前时间
     *
     * @return current time in milliseconds since 1970-01-01.
     */
    long now();

    /**
     * Clean unused topics.
     * 清理不需要的topic
     *
     * @param topics all valid topics.
     * @return number of the topics deleted.
     */
    int cleanUnusedTopic(final Set<String> topics);

    /**
     * Clean expired consume queues.
     * 清理多余的cq
     */
    void cleanExpiredConsumerQueue();

    /**
     * Check if the given message has been swapped out of the memory.
     * 检查给定的消息是否被交换出内存
     *
     * @param topic topic.
     * @param queueId queue ID.
     * @param consumeOffset consume queue offset.
     * @return true if the message is no longer in memory; false otherwise.
     */
    boolean checkInDiskByConsumeOffset(final String topic, final int queueId, long consumeOffset);

    /**
     * Get number of the bytes that have been stored in commit log and not yet dispatched to consume queue.
     * 获取已经存储到store但是还未调度到cq的消息数量
     *
     * @return number of the bytes to dispatch.
     */
    long dispatchBehindBytes();

    /**
     * Flush the message store to persist all data.
     * 刷新MessageStore用于持久化数据
     *
     * @return maximum offset flushed to persistent storage device.
     */
    long flush();

    /**
     * Reset written offset.
     * 重置写位点
     *
     * @param phyOffset new offset.
     * @return true if success; false otherwise.
     */
    boolean resetWriteOffset(long phyOffset);

    /**
     * Get confirm offset.
     * 获取确认位点
     *
     * @return confirm offset.
     */
    long getConfirmOffset();

    /**
     * Set confirm offset.
     * 设置确认位点
     *
     * @param phyOffset confirm offset to set.
     */
    void setConfirmOffset(long phyOffset);

    /**
     * Check if the operation system page cache is busy or not.
     * 检查操作系统的也缓存是否busy
     *
     * @return true if the OS page cache is busy; false otherwise.
     */
    boolean isOSPageCacheBusy();

    /**
     * Get lock time in milliseconds of the store by far.
     * 获取到目前为止的锁定时间
     *
     * @return lock time in milliseconds.
     */
    long lockTimeMills();

    /**
     * Check if the transient store pool is deficient.
     * 检查临时存储池是否不足
     *
     * @return true if the transient store pool is running out; false otherwise.
     */
    boolean isTransientStorePoolDeficient();

    /**
     * Get the dispatcher list.
     * 获取分发列表
     *
     * @return list of the dispatcher.
     */
    LinkedList<CommitLogDispatcher> getDispatcherList();

    /**
     * Get consume queue of the topic/queue.
     * 获取topic/queue对应的cq
     *
     * @param topic Topic.
     * @param queueId Queue ID.
     * @return Consume queue.
     */
    ConsumeQueue getConsumeQueue(String topic, int queueId);

    /**
     * Get BrokerStatsManager of the messageStore.
     * 获取messageStore的broker状态管理器
     *
     * @return BrokerStatsManager.
     */
    BrokerStatsManager getBrokerStatsManager();

    /**
     * handle
     * @param brokerRole
     */
    void handleScheduleMessageService(BrokerRole brokerRole);
}
