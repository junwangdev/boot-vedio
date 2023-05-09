package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.handler.im.IMMessageData;
import org.gjw.websocket.handler.im.VedioMeetingWSHandler;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSMessage;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.gjw.websocket.util.WSContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * webRtc通信处理
 * @author guojunwang
 * Description
 * Date 2023/4/5 15:58
 */
@Slf4j
@Component
public class OnCandidateAnalyzer extends WSMessageAnalyzer<IMMessageData> {

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    @Override
    public void analyze(WebSocketSession session, IMMessageData message) throws Throwable {
        final String userId = WSContextUtil.getUserId(session);
        final String roomNumber = message.getRoomNumber();
        final String remoteUserId = message.getRemoteUserId();

        List<RoomJoinRecord> recordList = roomJoinRecordService.lambdaQuery()
                .eq(RoomJoinRecord::getRoomNumber, roomNumber)
                .in(RoomJoinRecord::getUserId, remoteUserId,userId)
                .isNull(RoomJoinRecord::getLeaveDateTime)
                .list();

        if(CollUtil.isEmpty(recordList)){
            log.warn("房间不存在");
            WSMessage wsMessage = new WSMessage(SocketContext.ResponseEventType.ERROR);
            wsMessage.setMsg("房间不存在");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
            return;
        }


        //创建响应消息
        IMMessageData responseData = BeanUtil.copyProperties(message, IMMessageData.class);
        responseData.setUserId(message.getRemoteUserId());
        responseData.setRemoteUserId(message.getUserId());

        WSMessage wsMessage = new WSMessage(SocketContext.RequestEventType.CANDIDATE);
        wsMessage.setData(responseData);

        //给指定用户发送消息
        WebSocketSession remoteSession = getSession(remoteUserId);
        remoteSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.CANDIDATE);
    }
}
