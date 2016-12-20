package netty.beans;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ReferenceCountUtil;

@Component
public class Server {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void run() throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "9999"));
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap(); // (2)
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        System.out.println("add handler");
                        ch.pipeline().addLast(new ServerHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128) // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, false); // (6)

        // Bind and start to accept incoming connections.
        ChannelFuture f = b.bind(port).sync(); // (7)

        // Wait until the server socket is closed.
        // In this example, this does not happen, but you can do that to
        // gracefully
        // shut down your server.
        f.channel().closeFuture().sync();
    }

    @PreDestroy
    public void stop() throws Exception {
        System.out.println("shutdown");
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

    public static class ServerHandler extends ChannelInboundHandlerAdapter { // (1)

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
            ByteBuf in = (ByteBuf) msg;
            try {
                while (in.isReadable()) { // (1)
                    System.out.print((char) in.readByte());
                    System.out.flush();
                }
                ctx.channel().close();
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }

}
