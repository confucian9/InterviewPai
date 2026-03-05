package com.interviewpai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audio_record")
public class AudioRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String fileName;
    
    private String fileUrl;
    
    private Long duration;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
