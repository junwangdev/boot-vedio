package org.gjw.websocket.model.common;

import lombok.*;
import org.gjw.websocket.model.interfaces.AbstractWSHandler;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 10:47
 */
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class AnalyzerProperties {

    /**
     * 绑定的webSocketHandler
     */
    private Class<? extends AbstractWSHandler> webSocketHandler;

    /**
     * 处理的事件类型
     */
    private SocketContext.RequestEventType handlerEvent;
}
