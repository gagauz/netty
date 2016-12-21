package netty;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import netty.beans.server.Server;

public final class Application {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(netty.ServerConfiguration.class);
        Server server = context.getBean(Server.class);
    }
}
