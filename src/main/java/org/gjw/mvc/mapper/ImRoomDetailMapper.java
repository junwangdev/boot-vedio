package org.gjw.mvc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.gjw.mvc.bean.ImRoomDetail;
import org.mybatis.spring.annotation.MapperScan;

/**
 * @author guojunwang
 * Description
 * Date 2023/4/2 14:58
 */
@Mapper
public interface ImRoomDetailMapper extends BaseMapper<ImRoomDetail> {
}
