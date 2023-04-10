package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.handler.im.VedioMeetingWSHandler;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.common.WSIMMessage;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.gjw.websocket.util.WSContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * webRtc通信处理
 * @author guojunwang
 * Description
 * Date 2023/4/5 15:58
 */
@Slf4j
@Component
public class OnCandidateAnalyzer  extends WSMessageAnalyzer {

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    @Override
    public void analyze(WebSocketSession session, WSIMMessage message) throws Throwable {
        final String userId = WSContextUtil.getUserId(session);
        JSONObject reqJsonObj = JSONUtil.parseObj(message.getData());

        String roomNumber = reqJsonObj.getStr("roomNumber");
        String remoteUserId = reqJsonObj.getStr("remoteUserId");

        List<RoomJoinRecord> recordList = roomJoinRecordService.lambdaQuery()
                .eq(RoomJoinRecord::getRoomNumber, roomNumber)
                .eq(RoomJoinRecord::getUserId, remoteUserId)
                .isNull(RoomJoinRecord::getLeaveDateTime)
                .list();

        if(CollUtil.isEmpty(recordList)){
            log.warn("房间不存在");
            WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.ResponseEventType.ERROR);
            WSIMMessage.setMsg("房间不存在");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
            return;
        }


        WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.RequestEventType.CANDIDATE);
        Map<String,Object> result = new HashMap<>();
        result.put("roomNumber",reqJsonObj.getStr("roomNumber"));
        result.put("userId",reqJsonObj.getStr("remoteUserId"));
        result.put("remoteUserId",userId);
        result.put("rtcData",reqJsonObj.getJSONObject("rtcData"));
        WSIMMessage.setData(result);


        WebSocketSession remoteSession = getSession(remoteUserId);
        remoteSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.CANDIDATE);
    }
}
