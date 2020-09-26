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

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.rocketmq.common.UtilAll;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MappedFileQueueTest {

    /**
     * 获取最近写入的mappedFile
     */
    @Test
    public void testGetLastMappedFile() {
        final String fixedMsg = "0123456789abcdef";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        for (int i = 0; i < 1024; i++) {
            //获取最近一次写的位点所在的mappedFile
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            //在改mappedFile上追加message
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    /**
     * 根据位点查询mappedFiled
     */
    @Test
    public void testFindMappedFileByOffset() {
        // four-byte string.
        final String fixedMsg = "abcd";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        //判断已经写入的所有mappedFile的大小和msg.getBytes() * 1024的大小是否一致
        assertThat(mappedFileQueue.getMappedMemorySize()).isEqualTo(fixedMsg.getBytes().length * 1024);

        //判断0位点所在的文件的起始位点是否为0
        MappedFile mappedFile = mappedFileQueue.findMappedFileByOffset(0);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(0);

        //判断100位点所在的文件的起始位点是否为0
        mappedFile = mappedFileQueue.findMappedFileByOffset(100);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(0);

        //判断1024位点所在的文件起始位点是否为1024
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024);

        //判断1024 + 100位点所在的文件起始位点是否为1024
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 + 100);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024);

        //判断1024 * 2位点所在的文件起始位点是否为1024 * 2
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 2);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024 * 2);

        //判断1024 * 2 + 100位点所在的文件起始位点是否为1024 * 2
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 2 + 100);
        assertThat(mappedFile).isNotNull();
        System.out.println(mappedFile.getFileFromOffset());
        assertThat(mappedFile.getFileFromOffset()).isEqualTo(1024 * 2);

        // over mapped memory size.
        //判断1024 * 4位点所在的文件是否为空
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 4);
        assertThat(mappedFile).isNull();

        //判断1024 * 4 + 100位点所在的文件是否为空
        mappedFile = mappedFileQueue.findMappedFileByOffset(1024 * 4 + 100);
        assertThat(mappedFile).isNull();

        //关闭文件队列
        mappedFileQueue.shutdown(1000);
        //销毁文件队列
        mappedFileQueue.destroy();
    }


    /**
     * 根据位点查询mappedFile（非零位点）
     */
    @Test
    public void testFindMappedFileByOffset_StartOffsetIsNonZero() {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        //Start from a non-zero offset
        MappedFile mappedFile = mappedFileQueue.getLastMappedFile(1024);
        assertThat(mappedFile).isNotNull();

        //都为空
        assertThat(mappedFileQueue.findMappedFileByOffset(1025)).isEqualTo(mappedFile);

        assertThat(mappedFileQueue.findMappedFileByOffset(0)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(123, false)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(123, true)).isEqualTo(mappedFile);

        assertThat(mappedFileQueue.findMappedFileByOffset(0, false)).isNull();
        assertThat(mappedFileQueue.findMappedFileByOffset(0, true)).isEqualTo(mappedFile);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    /**
     * 存储消息
     */
    @Test
    public void testAppendMessage() {
        final String fixedMsg = "0123456789abcdef";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }

        //flush一页 todo flush和appendMessage区别？
        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 2);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 3);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 4);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 5);

        assertThat(mappedFileQueue.flush(0)).isFalse();
        assertThat(mappedFileQueue.getFlushedWhere()).isEqualTo(1024 * 6);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    /**
     * 获取mappedFileQueue队列大小
     */
    @Test
    public void testGetMappedMemorySize() {
        final String fixedMsg = "abcd";

        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        for (int i = 0; i < 1024; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            assertThat(mappedFile.appendMessage(fixedMsg.getBytes())).isTrue();
        }
        //判断已经写入的所有mappedFile的大小和msg.getBytes() * 1024的大小是否一致
        assertThat(mappedFileQueue.getMappedMemorySize()).isEqualTo(fixedMsg.length() * 1024);
        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    /**
     * todo 遗漏方法
     */
    @Test
    public void testDeleteExpiredFileByOffset() {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 5120, null);

        for (int i = 0; i < 2048; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            ByteBuffer byteBuffer = ByteBuffer.allocate(ConsumeQueue.CQ_STORE_UNIT_SIZE);
            byteBuffer.putLong(i);
            byte[] padding = new byte[12];
            Arrays.fill(padding, (byte) '0');
            byteBuffer.put(padding);
            byteBuffer.flip();

            assertThat(mappedFile.appendMessage(byteBuffer.array())).isTrue();
        }

        MappedFile first = mappedFileQueue.getFirstMappedFile();
        first.hold();

        assertThat(mappedFileQueue.deleteExpiredFileByOffset(20480, ConsumeQueue.CQ_STORE_UNIT_SIZE)).isEqualTo(0);
        first.release();

        assertThat(mappedFileQueue.deleteExpiredFileByOffset(20480, ConsumeQueue.CQ_STORE_UNIT_SIZE)).isGreaterThan(0);
        first = mappedFileQueue.getFirstMappedFile();
        assertThat(first.getFileFromOffset()).isGreaterThan(0);

        mappedFileQueue.shutdown(1000);
        mappedFileQueue.destroy();
    }

    /**
     * 根据时间删除过期文件
     * @throws Exception
     */
    @Test
    public void testDeleteExpiredFileByTime() throws Exception {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);

        for (int i = 0; i < 100; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(0);
            assertThat(mappedFile).isNotNull();
            byte[] bytes = new byte[512];
            assertThat(mappedFile.appendMessage(bytes)).isTrue();
        }

        //判断文件队列中有多少个文件
        assertThat(mappedFileQueue.getMappedFiles().size()).isEqualTo(50);
        long expiredTime =  100 * 1000;
        for (int i = 0; i < mappedFileQueue.getMappedFiles().size(); i++) {
            MappedFile mappedFile = mappedFileQueue.getMappedFiles().get(i);
           if (i < 5) {
               //设置最后更改时间为当前时间 - 200s
               mappedFile.getFile().setLastModified(System.currentTimeMillis() - expiredTime * 2);
           }
           if (i > 20) {
               //设置最后更改时间为当前时间 - 200s
               mappedFile.getFile().setLastModified(System.currentTimeMillis() - expiredTime * 2);
           }
        }
        //删除当前时间最近100s未更新的文件
        mappedFileQueue.deleteExpiredFileByTime(expiredTime, 0, 0, false);
        assertThat(mappedFileQueue.getMappedFiles().size()).isEqualTo(45);
    }


    /**
     * 测试顺序打乱后是否能正确查找mappedFile
     */
    @Test
    public void testFindMappedFile_ByIteration() {
        MappedFileQueue mappedFileQueue =
            new MappedFileQueue("/Users/weidian/test", 1024, null);
        for (int i =0 ; i < 3; i++) {
            MappedFile mappedFile = mappedFileQueue.getLastMappedFile(1024 * i);
            mappedFile.wrotePosition.set(1024);
        }

        assertThat(mappedFileQueue.findMappedFileByOffset(1028).getFileFromOffset()).isEqualTo(1024);

        // Switch two MappedFiles and verify findMappedFileByOffset method
        MappedFile tmpFile = mappedFileQueue.getMappedFiles().get(1);
        mappedFileQueue.getMappedFiles().set(1, mappedFileQueue.getMappedFiles().get(2));
        mappedFileQueue.getMappedFiles().set(2, tmpFile);
        MappedFile mappedFileByOffset = mappedFileQueue.findMappedFileByOffset(1028);
        assertThat(mappedFileQueue.findMappedFileByOffset(1028).getFileFromOffset()).isEqualTo(1024);
    }

    @After
    public void destory() {
        File file = new File("/Users/weidian/test");
        UtilAll.deleteFile(file);
    }
}
