package org.gjw.websocket.model.common;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author guojunwang
 * @Description
 * @Date 2023/4/2 10:49
 */
public class SocketContext {

    @Getter
    @AllArgsConstructor
    public enum RequestEventType {
        PING("ping", "ping心跳检测"),
        ERROR("error", "error"),
        CLOSE("close", "关闭socket连接"),
        OTHER_EVENT("other", "连接事件 用于连接服务 若当前能识别的 标记为other事件 统一处理"),
        CREATE("create", "创建房间"),
        JOIN("join", "加入"),
        CALL_REQUEST("request", "请求呼叫某⽤户（仅限⼀对⼀"),
        CALL_RESPONSE("response", "⽤于回应request（仅限⼀对⼀"),
        LEAVE("leave", "挂断，离开房间"),
        NEW_PEER("new_peer", "new_peer"),
        OFFER("offer", "通知"),
        ANSWER("answer", "回应"),
        CANDIDATE("candidate", "candidate"),
        UPDATE("update","更新"),
        ;

        private String eventCode;
        private String eventDesc;

        public static RequestEventType getEvent(String eventCode) {
            return Arrays.stream(values())
                    .filter(e -> StrUtil.equals(e.eventCode,eventCode))
                    .findFirst().orElse(null);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ResponseEventType {

        ERROR("error", "error"),
        SUESS("success", "success"),
        PONG("pong", "pong"),
        GET_MEMBER("get_member", "⽤户获取房间的其他在线⽤户"),
        NEW_PEER("new_peer", "房间有新⽤户加⼊"),
        PEER_LEAVE("peer_leave", "房间有⽤户离开房间"),
        UPDATE("update","更新"),
        ;

        private String eventCode;
        private String eventDesc;

        public static ResponseEventType getEvent(String eventCode) {
            return Arrays.stream(values())
                    .filter(e -> StrUtil.equals(e.eventCode,eventCode))
                    .findFirst().orElse(null);
        }
    }


}
