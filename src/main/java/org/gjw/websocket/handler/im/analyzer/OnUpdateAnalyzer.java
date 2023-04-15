package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.gjw.mvc.bean.ImRoomDetail;
import org.gjw.mvc.bean.RoomJoinRecord;
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/15 16:07
 */
@Component
public class OnUpdateAnalyzer extends WSMessageAnalyzer<IMMessageData> {

    @Autowired
    private ImRoomDetailService imRoomDetailService;

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;


    @Override
    public void analyze(WebSocketSession session, IMMessageData message) throws Throwable {

        //查询房间信息
        List<RoomJoinRecord> joinRecordList = roomJoinRecordService.lambdaQuery()
                .isNotNull(RoomJoinRecord::getLeaveDateTime)
                .ne(RoomJoinRecord::getUserId,message.getUserId())
                .eq(RoomJoinRecord::getRoomNumber, message.getRoomNumber())
                .list();

        if(CollUtil.isNotEmpty(joinRecordList)){
            List<WebSocketSession> sessionList = joinRecordList.stream().map(RoomJoinRecord::getUserId)
                    .filter(StrUtil::isNotBlank)
                    .map(this::getSession)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            for (WebSocketSession remoteSession : sessionList) {
                //创建响应消息
                IMMessageData responseData = BeanUtil.copyProperties(message, IMMessageData.class);
                responseData.setUserId(message.getRemoteUserId());
                responseData.setRemoteUserId(message.getUserId());

                WSMessage wsMessage = new WSMessage(SocketContext.RequestEventType.CALL_REQUEST);
                wsMessage.setData(responseData);

                remoteSession.sendMessage(new TextMessage(JSONUtil.toJsonStr(wsMessage)));
            }
        }


    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.UPDATE);
    }
}
