package org.gjw.websocket.handler.im;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.service.ImRoomDetailService;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSIMMessage;
import org.gjw.websocket.model.interfaces.AbstractWSHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.HashMap;

/**
 * @author guojunwang
 * Description
 * Date 2023/3/27 20:39
 */
@Slf4j
@Component
public class VedioMeetingWSHandler extends AbstractWSHandler {

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    @Autowired
    private ImRoomDetailService imRoomDetailService;
    /**
     * 建立连接成功
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.ResponseEventType.SUESS);
        WSIMMessage.setMsg("connect is success");
        WSIMMessage.setSuccess(true);

        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
    }

    /**
     * 获取session的主键
     */
    @Override
    public String getSessionPrimaryKey(WebSocketSession webSocketSession) {
        HashMap pathVariable = (HashMap)webSocketSession.getAttributes().get("pathVariable");
        return pathVariable.get("userId").toString();
    }

    /**
     * 处理的ws地址
     */
    @Override
    public String handlerMapping() {
        return "/ws/{userId}";
    }

    /**
     * 传输异常时
     *
     * @param session
     * @param exception
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session,exception);

        String userId = getSessionPrimaryKey(session);

        roomJoinRecordService.lambdaUpdate()
                .eq(RoomJoinRecord::getUserId,userId)
                .set(RoomJoinRecord::getLeaveDateTime,new Date())
                .update();
    }


    /**
     * 连接关闭时
     *
     * @param session
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String userId = getSessionPrimaryKey(session);

        roomJoinRecordService.lambdaUpdate()
                .eq(RoomJoinRecord::getUserId,userId)
                .set(RoomJoinRecord::getLeaveDateTime,new Date())
                .update();
    }
}
