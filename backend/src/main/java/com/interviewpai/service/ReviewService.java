package com.interviewpai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewpai.dto.QaRecordDTO;
import com.interviewpai.entity.AudioRecord;
import com.interviewpai.entity.QaRecord;
import com.interviewpai.mapper.AudioRecordMapper;
import com.interviewpai.mapper.QaRecordMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    
    private final QaRecordMapper qaRecordMapper;
    private final AudioRecordMapper audioRecordMapper;
    
    public ReviewService(QaRecordMapper qaRecordMapper, AudioRecordMapper audioRecordMapper) {
        this.qaRecordMapper = qaRecordMapper;
        this.audioRecordMapper = audioRecordMapper;
    }
    
    public List<QaRecordDTO> getRandomQuestions(Long userId, Integer count) {
        LambdaQueryWrapper<AudioRecord> audioWrapper = new LambdaQueryWrapper<>();
        audioWrapper.eq(AudioRecord::getUserId, userId)
                .eq(AudioRecord::getStatus, "FINISHED")
                .select(AudioRecord::getId);
        List<Long> audioIds = audioRecordMapper.selectList(audioWrapper)
                .stream()
                .map(AudioRecord::getId)
                .toList();
        
        if (audioIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<QaRecord> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.in(QaRecord::getAudioId, audioIds)
                .last("ORDER BY RAND() LIMIT " + count);
        
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        return qaRecords.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<QaRecordDTO> getQuestionsByTag(Long userId, String tag, Integer count) {
        LambdaQueryWrapper<AudioRecord> audioWrapper = new LambdaQueryWrapper<>();
        audioWrapper.eq(AudioRecord::getUserId, userId)
                .eq(AudioRecord::getStatus, "FINISHED")
                .select(AudioRecord::getId);
        List<Long> audioIds = audioRecordMapper.selectList(audioWrapper)
                .stream()
                .map(AudioRecord::getId)
                .toList();
        
        if (audioIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<QaRecord> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.in(QaRecord::getAudioId, audioIds)
                .like(QaRecord::getTags, tag)
                .last("ORDER BY RAND() LIMIT " + count);
        
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        return qaRecords.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<QaRecordDTO> getAllQuestionsForReview(Long userId) {
        LambdaQueryWrapper<AudioRecord> audioWrapper = new LambdaQueryWrapper<>();
        audioWrapper.eq(AudioRecord::getUserId, userId)
                .eq(AudioRecord::getStatus, "FINISHED")
                .select(AudioRecord::getId);
        List<Long> audioIds = audioRecordMapper.selectList(audioWrapper)
                .stream()
                .map(AudioRecord::getId)
                .toList();
        
        if (audioIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<QaRecord> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.in(QaRecord::getAudioId, audioIds)
                .orderByAsc(QaRecord::getCreateTime);
        
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        Collections.shuffle(qaRecords);
        
        return qaRecords.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    private QaRecordDTO toDTO(QaRecord record) {
        QaRecordDTO dto = new QaRecordDTO();
        dto.setId(record.getId());
        dto.setAudioId(record.getAudioId());
        dto.setQuestion(record.getQuestion());
        dto.setAnswer(record.getAnswer());
        dto.setTags(record.getTags());
        dto.setConfidence(record.getConfidence());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }
}
