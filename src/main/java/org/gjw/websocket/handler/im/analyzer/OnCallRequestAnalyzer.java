package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.service.ImRoomDetailService;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.handler.im.IMMessageData;
import org.gjw.websocket.handler.im.VedioMeetingWSHandler;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSMessage;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * 一对一通话-请求通话功能
 * @author guojunwang
 * Description
 * Date 2023/4/15 15:42
 */
@Slf4j
@Component
public class OnCallRequestAnalyzer extends WSMessageAnalyzer<IMMessageData> {

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    @Autowired
    private ImRoomDetailService imRoomDetailService;

    @Override
    public void analyze(WebSocketSession session, IMMessageData message) throws Throwable {
        //查询对方是否在线
        String remoteUserId = message.getRemoteUserId();
        WebSocketSession remoteSession = getSession(remoteUserId);

        if(Objects.isNull(remoteSession)){
            WSMessage wsMessage = new WSMessage(SocketContext.ResponseEventType.ERROR);
            wsMessage.setMsg("对方不在线");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
            return;
        }

        //创建响应消息
        IMMessageData responseData = BeanUtil.copyProperties(message, IMMessageData.class);
        responseData.setUserId(message.getRemoteUserId());
        responseData.setRemoteUserId(message.getUserId());

        WSMessage wsMessage = new WSMessage(SocketContext.RequestEventType.CALL_REQUEST);
        wsMessage.setData(responseData);

        remoteSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.CALL_REQUEST);
    }

}
