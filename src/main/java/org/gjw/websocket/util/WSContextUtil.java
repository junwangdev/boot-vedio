package org.gjw.websocket.util;

import cn.hutool.core.util.StrUtil;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 15:07
 */
public class WSContextUtil {

    public static String getUserId(WebSocketSession session){
        Map pathVariable = (Map)session.getAttributes().get("pathVariable");
        return StrUtil.toString(pathVariable.get("userId"));
    }
}
