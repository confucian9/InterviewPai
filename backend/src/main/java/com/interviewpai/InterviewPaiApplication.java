package com.interviewpai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.interviewpai.mapper")
public class InterviewPaiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewPaiApplication.class, args);
    }
}
