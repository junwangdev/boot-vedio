package org.gjw.websocket.model.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 10:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WSMessage {

    /**
     * 消息事件模式
     */
    private String eventCode;

    private String msg;

    private boolean success;

    private Object data;

    public WSMessage(SocketContext.RequestEventType responseEventType){
        this.eventCode = responseEventType.getEventCode();
        this.msg = responseEventType.getEventDesc();

        if(!SocketContext.RequestEventType.ERROR.equals(responseEventType)){
            this.success = true;
        }
    }

    public WSMessage(SocketContext.ResponseEventType responseEventType){
        this.eventCode = responseEventType.getEventCode();
        this.msg = responseEventType.getEventDesc();

        if(!SocketContext.ResponseEventType.ERROR.equals(responseEventType)){
            this.success = true;
        }
    }
}
