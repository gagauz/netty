package netty.beans.server;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.internal.StringUtil;
import netty.annotations.HttpHandler;
import netty.annotations.HttpUrl;
import netty.annotations.Param;
import netty.beans.handlers.IWebHandler;
import netty.consts.HttpMethods;

@Service
public class ChannelHandlerFactory extends ChannelInitializer<SocketChannel> {

    @Autowired
    @HttpHandler
    private Object[] httpHandlerBeans;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("handler", new ServerHandler());
    }

    private IWebHandler createHandler(Object object, Map<String, IWebHandler> handlers0) {

        for (Method method : object.getClass().getMethods()) {
            HttpUrl annotation = object.getClass().getAnnotation(HttpUrl.class);
            if (annotation != null) {
                IWebHandler webHandler = new IWebHandler() {

                    @Override
                    public void process(QueryStringDecoder queryStringDecoder, HttpRequest request, DefaultFullHttpResponse response,
                            ByteBuf buf) {
                        List<Object> arguments = new ArrayList<>();
                        for (Parameter p : method.getParameters()) {
                            Param param = p.getAnnotation(Param.class);
                            if (null != param) {
                                List<String> values = queryStringDecoder.parameters().get(param.name());
                                if (null != values) {
                                    if (p.getType().isArray()) {
                                        arguments.add(values.toArray(new String[values.size()]));
                                    } else if (Collection.class.isAssignableFrom(p.getType())) {
                                        arguments.add(values);
                                    } else if (!values.isEmpty()) {
                                        arguments.add(values.get(0));
                                    }
                                } else {
                                    arguments.add(null);
                                }
                            }
                        }
                        try {
                            Object result = method.invoke(object, arguments.toArray(new Object[arguments.size()]));
                            if (null != result) {
                                buf.writeBytes(result.toString().getBytes());
                            }
                        } catch (Exception e) {
                            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public String getContentType() {
                        return StringUtil.isNullOrEmpty(annotation.produce()) ? null : annotation.produce();
                    }
                };
                for (HttpMethods httpMethod : annotation.methods()) {
                    handlers0.put(httpMethod.name() + '_' + annotation.value(), webHandler);
                }
                if (annotation.methods().length == 0) {
                    handlers0.put(annotation.value(), webHandler);
                }
            }
        }
        return null;
    }

    class ServerHandler extends SimpleChannelInboundHandler<Object> {

        private HttpRequest request;
        private final StringBuilder buf = new StringBuilder();
        private Map<String, IWebHandler> handlers = new HashMap<>();

        public ServerHandler() {
            if (handlers.size() == 0) {
                try {
                    for (Object object : httpHandlerBeans) {
                        createHandler(object, handlers);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            IWebHandler handler = null;
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            ByteBuf buf = Unpooled.buffer();
            if (msg instanceof HttpRequest) {
                HttpRequest request = (HttpRequest) msg;
                HttpMethod method = request.method();
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
                final String requestPath = queryStringDecoder.path();
                handler = handlers.get(method.name() + '_' + requestPath);
                if (null == handler) {
                    handler = handlers.get(requestPath);
                }
                if (handler != null) {
                    handler.process(queryStringDecoder, request, response, buf);
                }
            }
            if (msg instanceof LastHttpContent) {

                response.setStatus(((LastHttpContent) msg).decoderResult().isSuccess()
                        ? HttpResponseStatus.OK
                        : HttpResponseStatus.BAD_REQUEST);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                        handler != null ? handler.getContentType() : "text/plain; charset=UTF-8");

                if (HttpUtil.isKeepAlive(request)) {
                    response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
                }
                ctx.write(response.replace(buf));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
