package netty.beans.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

public interface IWebHandler {

    String getContentType();

    void process(QueryStringDecoder queryStringDecoder, HttpRequest request, DefaultFullHttpResponse response, ByteBuf buf);

}
