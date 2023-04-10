package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.gjw.mvc.bean.ImRoomDetail;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.service.ImRoomDetailService;
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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author guojunwang
 * DescriptionQ
 * Date 2023/4/2 15:01
 */
@Slf4j
@Component
public class JoinRoomAnalyzer extends WSMessageAnalyzer {

    @Autowired
    private ImRoomDetailService roomDetailRecordService;

    @Autowired
    private RoomJoinRecordService roomJoinRecordService;

    @Override
    public void analyze(WebSocketSession session, WSIMMessage message) throws Throwable {
        final String userId = WSContextUtil.getUserId(session);
        String roomNumber = JSONUtil.parseObj(message.getData()).getStr("roomNumber");

        //判断房间是否存在
        ImRoomDetail roomDetail = roomDetailRecordService.lambdaQuery()
                .eq(ImRoomDetail::getRoomNumber, StrUtil.trim(roomNumber))
                .isNull(ImRoomDetail::getEndDateTime)
                .orderByDesc(ImRoomDetail::getStartDateTime)
                .last("limit 1")
                .one();

        if(Objects.isNull(roomDetail)){
            log.warn("房间不存在");
            WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.ResponseEventType.ERROR);
            WSIMMessage.setMsg("房间不存在");
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
            return;
        }

        //查询房间内所有的用户
        List<RoomJoinRecord> joinRecordList = roomJoinRecordService.lambdaQuery()
                .eq(RoomJoinRecord::getRoomNumber, roomNumber)
                .isNotNull(RoomJoinRecord::getJoinDateTime)
                .isNull(RoomJoinRecord::getLeaveDateTime)
                .list();

        //判断是否加入群聊
        RoomJoinRecord roomJoinRecord = joinRecordList.stream()
                .filter(record -> StrUtil.equals(record.getUserId(), userId))
                .findFirst().orElse(null);

        //如果未加入,加入会议
        if(Objects.isNull(roomJoinRecord)){
            roomJoinRecord = RoomJoinRecord.builder()
                    .userId(userId)
                    .roomNumber(roomNumber)
                    .joinDateTime(new Date())
                    .build();
            roomJoinRecordService.save(roomJoinRecord);
            joinRecordList.add(roomJoinRecord);
        }

        //获取其他成员userId
        Set<String> otherMemberIdSet = joinRecordList.stream()
                .filter(record -> !StrUtil.equals(record.getUserId(), userId))
                .map(RoomJoinRecord::getUserId)
                .collect(Collectors.toSet());


        WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.ResponseEventType.NEW_PEER);
        WSIMMessage.setData(MapUtil.of("remoteUserId",userId));

        //群发消息 有新用户加入
        otherMemberIdSet.stream()
            .map(this::getSession)
            .forEach( s -> {
                try {
                    s.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
                } catch (IOException e) {}
            });

        //给自己发送消息
        WSIMMessage getMemberMessage = new WSIMMessage(SocketContext.ResponseEventType.GET_MEMBER);
        getMemberMessage.setData(CollUtil.emptyIfNull(otherMemberIdSet));
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(getMemberMessage)));
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.JOIN);
    }
}