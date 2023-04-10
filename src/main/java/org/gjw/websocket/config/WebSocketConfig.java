package org.gjw.websocket.config;

import org.gjw.websocket.interceptor.SocketRequestResolverInterceptor;
import org.gjw.websocket.model.interfaces.AbstractWSHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * @author guojunwang
 * Description
 * Date 2023/3/27 20:50
 */
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private List<AbstractWSHandler> wsHandlerMap;

    @Autowired
    private SocketRequestResolverInterceptor messageInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        wsHandlerMap.forEach( handler -> {
            registry.addHandler(handler,handler.handlerMapping())
                    .addInterceptors(messageInterceptor)
                    .setAllowedOrigins("*");
        });

    }
}
