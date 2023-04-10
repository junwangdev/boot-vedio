package org.gjw.websocket.handler.im.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.gjw.websocket.handler.im.VedioMeetingWSHandler;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSIMMessage;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 15:00
 */
@Slf4j
@Component
public class PingAnalyzere extends WSMessageAnalyzer {
    @Override
    public void analyze(WebSocketSession session, WSIMMessage message) {
        Map pathVariable = (Map)session.getAttributes().get("pathVariable");
        log.info("客户端发送心跳,userId-->{},message-->{}",pathVariable.get("userId"),message);
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.PING);
    }
}
