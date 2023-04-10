package org.gjw.websocket.model.interfaces;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSHandlerFactory;
import org.gjw.websocket.model.common.WSIMMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目中所有的WebSocket处理器父类
 * @author guojunwang
 * Date 2023/4/2 10:34
 */
@Slf4j
@EqualsAndHashCode
public abstract class AbstractWSHandler extends TextWebSocketHandler {

    @Getter
    private Map<String,WebSocketSession> sessionMap = new ConcurrentHashMap<>(64);

    /**
     * 建立连接成功
     */
    public void afterConnectionEstablished(WebSocketSession session) throws Exception{
        sessionMap.put(getSessionPrimaryKey(session),session);
    };


    /**
     * 接收到客户端数据
     */
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
        //json转message对象
        WSIMMessage webSocketMessage = JSONUtil.parseObj(message.getPayload()).toBean(WSIMMessage.class);

        if(Objects.isNull(webSocketMessage) || StrUtil.isBlank(webSocketMessage.getEventCode())){
            WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.RequestEventType.ERROR);
            WSIMMessage.setMsg("不支持的消息类型");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
            return;
        }

        //获取消息的解析器
        WSMessageAnalyzer analyzer = analyzerMap()
                .get(SocketContext.RequestEventType.getEvent(webSocketMessage.getEventCode()));

        if(Objects.isNull(analyzer)){
            WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.RequestEventType.ERROR);
            WSIMMessage.setMsg("不支持的消息类型");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
            return;
        }

        try {
            analyzer.analyze(session,webSocketMessage);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 传输异常时
     */
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception{
        log.error("webSocket传输发生异常",exception);
    };

    /**
     * 连接关闭时
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception{
        sessionMap.remove(getSessionPrimaryKey(session));
    };


    public boolean supportsPartialMessages() {
        return true;
    }


    /**
     * 当前handler所有的分析器
     */
    public Map<SocketContext.RequestEventType,WSMessageAnalyzer> analyzerMap(){
        //从工厂获取当前handler的所有解析器
        List<WSMessageAnalyzer> analyzerList = WSHandlerFactory.getAnalyzerList(this.getClass());

        return Objects.isNull(analyzerList) ? Collections.emptyMap() : analyzerList.stream()
                .collect(Collectors.toMap(analyzer ->analyzer.properties().getHandlerEvent(), Function.identity(),(o1,o2)->o2));
    }


    /**
     * 获取session的主键
     */
    public abstract String getSessionPrimaryKey(WebSocketSession webSocketSession);

    /**
     * 处理的ws地址
     */
    public abstract String handlerMapping();
}
