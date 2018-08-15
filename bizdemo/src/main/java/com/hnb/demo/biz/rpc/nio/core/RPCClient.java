package com.hnb.demo.biz.rpc.nio.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author xiaolu
 * @date 2018/8/14 下午3:29
 */
public class RPCClient {
    private SocketChannel channel;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);
    private static RPCClient client = new RPCClient();
    private Selector selector = null;

    private RPCClient() {
    }

    public static RPCClient getInstance() {
        return client;
    }


    public RPCClient init(String serverIp) {
        try {
            System.out.println("------客户端要启动了--------");
            selector = Selector.open();
            InetSocketAddress isa = new InetSocketAddress(serverIp, 3003);

            // 获取socket通道
            channel = SocketChannel.open(isa);

            // 连接服务器
            channel.configureBlocking(false);

            channel.register(selector, SelectionKey.OP_READ);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    /**
     * 获取代理
     *
     * @param clazz
     * @return
     */
    public Object getRemoteProxy(final Class clazz) {

        //动态产生实现类
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clazz}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {

                // TODO 自动生成的方法存根
                String methodName = method.getName();
                String clazzName = clazz
                        .getSimpleName();
                Object result = null;

                // 表示没有参数 它传递的类型
                if (args == null || args.length == 0) {

                    // 接口名/方法名()
                    channel.write(ByteBuffer
                            .wrap((clazzName + "/" + methodName + "()")
                                    .getBytes()));
                } else {
                    int size = args.length;
                    String[] types = new String[size];
                    StringBuffer content = new StringBuffer(clazzName)
                            .append("/").append(methodName).append("(");
                    for (int i = 0; i < size; i++) {
                        types[i] = args[i].getClass().getName();
                        content.append(types[i]).append(":").append(args[i]);
                        if (i != size - 1) {
                            content.append(",");
                        }
                    }
                    content.append(")");
                    channel.write(ByteBuffer
                            .wrap(content.toString().getBytes()));
                }

                // 获取结果
                result = getResult();


                return result;
            }
        });


    }


    private Object getResult() {
        // 解析结果 如果结尾为null或NULL则忽略
        try {
            while (selector.select() > 0) {
                for (SelectionKey sk : selector.selectedKeys()) {
                    selector.selectedKeys().remove(sk);
                    if (sk.isReadable()) {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        buffer.clear();
                        sc.read(buffer);
                        int postion = buffer.position();

                        String result = new String(buffer.array(), 0, postion);
                        result = result.trim();
                        buffer.clear();

                        if (result.endsWith("null") || result.endsWith("NULL")) {
                            return null;
                        }


                        String[] typeValue = result.split(":");
                        String type = typeValue[0];
                        String value = typeValue[1];
                        if (type.contains("Integer") || type.contains("int")) {
                            return Integer.parseInt(value);
                        } else if (type.contains("Float")
                                || type.contains("float")) {
                            return Float.parseFloat(value);
                        } else if (type.contains("Long") || type.contains("long")) {
                            return Long.parseLong(value);
                        } else {
                            return value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
