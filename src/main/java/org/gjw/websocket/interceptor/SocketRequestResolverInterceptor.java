package org.gjw.websocket.interceptor;

import cn.hutool.core.text.StrMatcher;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author guojunwang
 * Description
 * Date 2023/3/27 20:46
 */
@Order
@Component
@Slf4j
public class SocketRequestResolverInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        ServletServerHttpRequest serverHttpRequest =  (ServletServerHttpRequest)request;

        //获取请求路径参数map
        Map<String, String> pathVariables = getPathVariables(serverHttpRequest);

        //获取请求参数
        Map<String, String> requestParams = getRequestParams(serverHttpRequest);

        //获取请求头
        Map<String, String> requestHeader = getHeaders( serverHttpRequest );


        log.info( "pathVariables  ->{}", JSONUtil.toJsonStr( pathVariables ) );
        log.info( "requestParams  ->{}", JSONUtil.toJsonStr( requestParams ) );
        log.info( "requestHeaders ->{}", JSONUtil.toJsonStr( requestHeader ) );


        attributes.put("param",requestParams);
        attributes.put("pathVariable",pathVariables);
        attributes.put("header",requestHeader);

        //返回true允许通行
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }

    /**
     * 获取请求路径参数
     */
    private Map<String,String> getPathVariables( ServletServerHttpRequest request ){
        //旧版本直接通过 URI_TEMPLATE_VARIABLES_ATTRIBUTE即可获取pathVariable

        //当前版本为SpringBoot3需要手动解析
        String pattern = (String)request.getServletRequest().getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        pattern = pattern.replace("{","${");

        String requestUri = request.getServletRequest().getRequestURI();

        StrMatcher strMatcher = new StrMatcher(pattern);

        return strMatcher.match(requestUri);
    }

    /**
     * 获取请求参数
     */
    private Map<String,String> getRequestParams(ServletServerHttpRequest request ){
        Map<String, String[]> parameterMap = request.getServletRequest().getParameterMap();

        if( null == parameterMap ) return new HashMap(8);

        HashMap result = new HashMap(parameterMap.size());

        //转换为 string
        parameterMap.keySet().forEach(k ->{
            result.put( k, String.join(",", parameterMap.get(k)));
        } );


        return result;
    }

    /**
     * 获取请求头
     */
    private Map<String,String> getHeaders( ServletServerHttpRequest request ){
        return request.getHeaders().toSingleValueMap();
    }
}
