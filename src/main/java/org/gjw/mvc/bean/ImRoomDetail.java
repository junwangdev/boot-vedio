package org.gjw.mvc.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 14:51
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class ImRoomDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间编号
     */
    private String roomNumber;

    /**
     * 呼叫者编码对应sys_user 表的 user_code
     */
    private String createUserId;

    /**
     * 呼叫时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDateTime;

    /**
     * 结束时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDateTime;

}
