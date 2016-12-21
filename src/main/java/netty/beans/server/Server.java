package netty.beans.server;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@Component
public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Autowired
    private ChannelInitializer channelInitializer;

    @PostConstruct
    public void run() {
        LOG.info("Server.run()");
        try {
            int port = Integer.parseInt(System.getProperty("port", "9990"));
            EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(channelInitializer)
                    .option(ChannelOption.SO_BACKLOG, 128) // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, false); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to
            // gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        LOG.info("Server.stop()");
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
