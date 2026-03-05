package com.interviewpai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("interview_summary")
public class InterviewSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long audioId;
    
    private String summary;
    
    private String keywords;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
