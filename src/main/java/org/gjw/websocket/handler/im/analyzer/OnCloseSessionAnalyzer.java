package org.gjw.websocket.handler.im.analyzer;

import org.gjw.mvc.service.ImRoomDetailService;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.gjw.websocket.handler.im.IMMessageData;
import org.gjw.websocket.handler.im.VedioMeetingWSHandler;
import org.gjw.websocket.model.common.AnalyzerProperties;
import org.gjw.websocket.model.common.SocketContext;
import org.gjw.websocket.model.interfaces.WSMessageAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/15 16:24
 */
@Component
public class OnCloseSessionAnalyzer extends WSMessageAnalyzer<IMMessageData> {


    @Override
    public void analyze(WebSocketSession session, IMMessageData message) throws Throwable {

        if(Objects.nonNull(session) && session.isOpen()){
            session.close();
        }

    }

    /**
     * 分析器属性
     */
    @Override
    public AnalyzerProperties properties() {
        return new AnalyzerProperties(VedioMeetingWSHandler.class, SocketContext.RequestEventType.CLOSE);
    }
}
