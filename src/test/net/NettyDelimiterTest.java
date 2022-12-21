package net;

import base.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ZT 2022-12-17 17:13
 */
public class NettyDelimiterTest {
    private Logger logger = LoggerFactory.getLogger(NettyDelimiterTest.class);

    ServerBootstrap b;
    ChannelFuture cf;
    private final int testPort = 38195;
    private ArrayBlockingQueue<String> actual = new ArrayBlockingQueue<>(102400);

    public NettyDelimiterTest() throws InterruptedException {
        String buffer;
        Thread thread = new Thread(() -> {

            EventLoopGroup bossGroup = new NioEventLoopGroup(Constants.NETTY_BOSS_THREAD_NUMBER);
            EventLoopGroup workGroup = new NioEventLoopGroup(Constants.NETTY_WORKER_THREAD_NUMBER);
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                socketChannel.pipeline()
                                        .addLast(new DelimiterBasedFrameDecoder(4096, Unpooled.copiedBuffer("\n".getBytes())))
                                        .addLast("measurement", new ChannelInboundHandlerAdapter() {
                                            @Override
                                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                                ByteBuf buf = (ByteBuf) msg;
                                                int i = buf.readableBytes();
                                                System.out.println(i);
                                                byte[] arr = new byte[i];
                                                buf.readBytes(arr);
                                                System.out.println(new String(arr));
                                                actual.add(new String(arr));
                                            }

                                            @Override
                                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                                logger.error("error: " + cause.getMessage());
                                            }
                                        })
                                ;
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);
                logger.info("waiting data");
                ChannelFuture f = b.bind(testPort).sync();
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                logger.error("connection error " + e.getMessage());
            } finally {
                workGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        });
        thread.setDaemon(true);
        thread.start();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        initClient();
    }

    private void initClient() throws InterruptedException {

        EventLoopGroup bossGroup = new NioEventLoopGroup();

        Bootstrap bs = new Bootstrap();

        bs.group(bossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // marshalling 序列化对象的解码
//                  socketChannel.pipeline().addLast(MarshallingCodefactory.buildDecoder());
                        // marshalling 序列化对象的编码
//                  socketChannel.pipeline().addLast(MarshallingCodefactory.buildEncoder());
                        // 处理来自服务端的响应信息
//                        socketChannel.pipeline().addLast(new ClientHandler());
                    }
                });

        // 客户端开启
        cf = bs.connect("localhost", testPort).sync();
        logger.info("connected");
    }

    @Before
    public void before() {
        actual.clear();
    }

    @Test
    public void test2() throws InterruptedException {
        String s1 = "1234567890\n12345";
        String s2 = "67890\n1234567890\n";
        cf.channel().writeAndFlush(Unpooled.copiedBuffer(s1.getBytes()));
        Thread.sleep(1000);
        cf.channel().writeAndFlush(Unpooled.copiedBuffer(s2.getBytes()));
        Thread.sleep(1000);
        Assert.assertEquals(actual.size(), 3);
        String a1 = actual.poll(1, TimeUnit.SECONDS);
        String a2 = actual.poll(1, TimeUnit.SECONDS);
        String a3 = actual.poll(1, TimeUnit.SECONDS);
        Assert.assertEquals(a1, a2);
        Assert.assertEquals(a3, a2);
        Assert.assertEquals("1234567890", a1);
    }

    @Test
    public void test1() throws Exception {
//        Thread.sleep(10000000);
        List<String> expected = new ArrayList<>(100000);
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();
        for (int i = 0; i < 1000000; i++) {
            int a = random.nextInt(100)+500;
//            int a = 5;
            String generatedString = random.ints(leftLimit, rightLimit + 1)
                    .limit(a)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
            // 发送客户端的请求
            String s = generatedString + "\n";
            cf.channel().write(Unpooled.copiedBuffer(s.getBytes()));
//            cf.channel().writeAndFlush(Unpooled.copiedBuffer(s.getBytes()));
            expected.add(generatedString);
            if (i % 1000 == 0) {
                for (int j = 0; j < expected.size(); j++) {
                    String actualString = actual.poll(1, TimeUnit.SECONDS);
                    Assert.assertEquals(generatedString, actualString);
                }
                logger.info(String.valueOf(i));
            }
        }
//        expected.sort(String::compareTo);
//        actual.sort(String::compareTo);
        Assert.assertArrayEquals(expected.toArray(), actual.toArray());

        // 等待直到连接中断
        cf.channel().close();
    }
}
