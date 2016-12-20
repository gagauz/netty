package netty;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import netty.beans.Server;

@Configuration
@EnableCaching
@EnableScheduling
@ComponentScan(basePackageClasses = Server.class)
public class ServerConfiguration {

}
