package org.apache.rocketmq.broker.learn;

import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * description：
 *
 * @author zhengwanli
 * @date 2020/5/17
 */
public class Person {

    /**
     * 获取该类型性别并进行打印
     *
     * @param type 0代表女生、1代表男生、其他数字提示参数有误
     */
    public String getSex(int type) {
        if (type == 0) {
            return "girl";
        } else if (type == 1) {
            return "boy";
        } else {
            return "error param";
        }
    }
    /**
     * 打印方法
     *
     * @param content
     */
    public void printing(String content) {
        //这里为了简单我们直接控制台输出
        System.out.print(content);
    }

    /**
     * 判断类型是否是男人
     *
     * @param type
     * @return
     */
    public boolean isMan(int type) {
        //处理逻辑这里为了方便我们直接进行简单判断
        return type == 1;
    }

    public static void main(String[] args) {
        Person person = mock(Person.class);
        when(person.getSex(0)).thenReturn("sb!!!");

        System.out.println(person.getSex(0));

    }
}
