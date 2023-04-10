package org.gjw.mvc.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.gjw.mvc.bean.RoomJoinRecord;
import org.gjw.mvc.mapper.RoomJoinRecordMapper;
import org.gjw.mvc.service.RoomJoinRecordService;
import org.springframework.stereotype.Service;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 15:33
 */
@Service
public class RoomJoinRecordServiceImpl extends ServiceImpl<RoomJoinRecordMapper,RoomJoinRecord> implements RoomJoinRecordService {
}
