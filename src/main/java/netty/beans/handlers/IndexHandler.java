package netty.beans.handlers;

import org.springframework.stereotype.Service;

import netty.annotations.HttpHandler;
import netty.annotations.HttpUrl;

@HttpHandler
@Service
public class IndexHandler {
    @HttpUrl("/")
    String handle() {
        return "Hello world!";
    }
}
