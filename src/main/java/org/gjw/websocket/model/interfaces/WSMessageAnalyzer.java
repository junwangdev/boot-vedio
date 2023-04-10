package org.gjw.websocket.model.interfaces;

import cn.hutool.extra.spring.SpringUtil;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.WSMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * webSocket消息分析器
 * @author guojunwang
 * Description
 * Date 2023/4/2 10:43
 */
public abstract class WSMessageAnalyzer<T> {


    public abstract void analyze(WebSocketSession session, T message) throws Throwable;

    /**
     * 分析器属性
     */
    public abstract AnalyzerProperties properties();

    public WebSocketSession getSession(String sessionPrimaryKey){
        AbstractWSHandler wsHandler = SpringUtil.getBean(properties().getWebSocketHandler());
        return (WebSocketSession)wsHandler.getSessionMap().get(sessionPrimaryKey);
    }

}
