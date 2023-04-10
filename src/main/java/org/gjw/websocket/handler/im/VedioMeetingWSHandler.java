package org.gjw.websocket.handler.im;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSMessage;
import org.gjw.websocket.model.interfaces.AbstractWSHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author guojunwang
 * Description
 * Date 2023/3/27 20:39
 */
@Slf4j
@Component
public class VedioMeetingWSHandler extends AbstractWSHandler<IMMessageData> {

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    /**
     * 建立连接成功
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        WSMessage WSMessage = new WSMessage(SocketContext.ResponseEventType.SUESS);
        WSMessage.setMsg("connect is success");
        WSMessage.setSuccess(true);

        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSMessage)));
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

    /**
     * 接收到消息时的处理
     */
    @Override
    public IMMessageData messageDisponse(WSMessage wsMessage) {
        IMMessageData imMessageData = new IMMessageData();

        if(Objects.nonNull(wsMessage.getData())){
            JSONObject jsonData = JSONUtil.parseObj(wsMessage.getData());
            imMessageData.setRoomNumber(jsonData.getStr("roomNumber"));
            imMessageData.setRemoteUserId(jsonData.getStr("remoteUserId"));
            imMessageData.setUserId(jsonData.getStr("userId"));

            String rtcData = jsonData.getStr("rtcData");
            if(StrUtil.isNotBlank(rtcData)){
                imMessageData.setRtcData(JSONUtil.parseObj(rtcData).toBean(Map.class));
            }

        }

        imMessageData.setEventCode(wsMessage.getEventCode());
        return imMessageData;
    }
}
