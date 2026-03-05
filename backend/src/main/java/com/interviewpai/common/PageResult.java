package com.interviewpai.common;

import lombok.Data;

@Data
public class PageResult<T> {
    private java.util.List<T> records;
    private Long total;
    private Long pages;
    private Long current;
    private Long size;
    
    public static <T> PageResult<T> of(java.util.List<T> records, Long total, Long current, Long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages((total + size - 1) / size);
        return result;
    }
}
