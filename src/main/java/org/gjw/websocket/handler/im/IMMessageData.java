package org.gjw.websocket.handler.im;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/10 15:41
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IMMessageData {

    private String eventCode;

    private String userId;

    private String remoteUserId;

    private String roomNumber;

    private Map rtcData;
}
