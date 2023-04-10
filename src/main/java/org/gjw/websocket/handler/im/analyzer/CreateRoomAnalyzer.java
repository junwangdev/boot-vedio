package org.gjw.websocket.handler.im.analyzer;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import org.gjw.mvc.bean.ImRoomDetail;
import org.gjw.mvc.service.ImRoomDetailService;
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 15:01
 */
@Component
public class CreateRoomAnalyzer extends WSMessageAnalyzer {

    @Autowired
    private ImRoomDetailService imRoomDetailService;

    @Override
    public void analyze(WebSocketSession session, WSIMMessage message) throws Throwable{
        String userId = WSContextUtil.getUserId(session);

        String roomId = RandomUtil.randomString(6);

        //数据库插入数据
        ImRoomDetail roomDetailRecord = ImRoomDetail.builder()
                .roomNumber(roomId)
                .createUserId(userId)
                .startDateTime(new Date())
                .build();

        imRoomDetailService.save(roomDetailRecord);

        Map<String,String> result= new HashMap<>();
        result.put("roomNumber",roomId);

        WSIMMessage WSIMMessage = new WSIMMessage(SocketContext.RequestEventType.CREATE);
        WSIMMessage.setData(result);
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(WSIMMessage)));
    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.CREATE);
    }
}