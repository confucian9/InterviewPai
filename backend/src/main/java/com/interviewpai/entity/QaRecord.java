package com.interviewpai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("qa_record")
public class QaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long audioId;
    
    private String question;
    
    private String answer;
    
    private String tags;
    
    private BigDecimal confidence;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
