-- 创建数据库
CREATE DATABASE IF NOT EXISTS interviewpai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE interviewpai;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `avatar` VARCHAR(500) COMMENT '头像',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 音频记录表
CREATE TABLE IF NOT EXISTS `audio_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '音频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `file_name` VARCHAR(255) COMMENT '文件名',
    `file_url` VARCHAR(500) COMMENT '文件地址',
    `duration` BIGINT COMMENT '音频时长(秒)',
    `status` VARCHAR(20) DEFAULT 'UPLOADED' COMMENT '处理状态: UPLOADED, PROCESSING, FINISHED, FAILED',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频记录表';

-- 转写文本表
CREATE TABLE IF NOT EXISTS `audio_transcript` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `audio_id` BIGINT NOT NULL COMMENT '音频ID',
    `transcript_text` LONGTEXT COMMENT '转写文本',
    `transcript_url` VARCHAR(500) COMMENT '转写文本存储URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_audio_id` (`audio_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转写文本表';

-- 问答记录表
CREATE TABLE IF NOT EXISTS `qa_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `audio_id` BIGINT NOT NULL COMMENT '音频ID',
    `question` TEXT NOT NULL COMMENT '问题',
    `answer` TEXT COMMENT '答案',
    `tags` VARCHAR(500) COMMENT '标签(逗号分隔)',
    `confidence` DECIMAL(5,2) COMMENT '置信度',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_audio_id` (`audio_id`),
    FULLTEXT INDEX `ft_question` (`question`),
    FULLTEXT INDEX `ft_answer` (`answer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答记录表';

-- 面试总结表
CREATE TABLE IF NOT EXISTS `interview_summary` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `audio_id` BIGINT NOT NULL COMMENT '音频ID',
    `summary` TEXT COMMENT '总结',
    `keywords` VARCHAR(500) COMMENT '关键词',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_audio_id` (`audio_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试总结表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 问答标签关联表
CREATE TABLE IF NOT EXISTS `qa_tag` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    `qa_id` BIGINT NOT NULL COMMENT '问题ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_qa_id` (`qa_id`),
    INDEX `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答标签关联表';

-- 插入默认标签
INSERT INTO `tag` (`name`) VALUES 
('Java'),
('并发'),
('JVM'),
('Spring'),
('Redis'),
('MySQL'),
('微服务'),
('分布式'),
('算法'),
('数据结构'),
('网络'),
('操作系统'),
('设计模式'),
('消息队列'),
('Docker');
