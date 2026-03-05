package com.interviewpai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("qa_tag")
public class QaTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long qaId;
    
    private Long tagId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
