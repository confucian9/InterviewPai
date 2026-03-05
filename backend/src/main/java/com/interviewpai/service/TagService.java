package com.interviewpai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewpai.dto.QaRecordDTO;
import com.interviewpai.entity.AudioRecord;
import com.interviewpai.entity.QaRecord;
import com.interviewpai.entity.Tag;
import com.interviewpai.mapper.AudioRecordMapper;
import com.interviewpai.mapper.QaRecordMapper;
import com.interviewpai.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TagService {
    
    private final TagMapper tagMapper;
    private final QaRecordMapper qaRecordMapper;
    private final AudioRecordMapper audioRecordMapper;
    
    public TagService(TagMapper tagMapper, QaRecordMapper qaRecordMapper, AudioRecordMapper audioRecordMapper) {
        this.tagMapper = tagMapper;
        this.qaRecordMapper = qaRecordMapper;
        this.audioRecordMapper = audioRecordMapper;
    }
    
    public List<Tag> getAllTags() {
        return tagMapper.selectList(null);
    }
    
    public List<QaRecordDTO> searchByKeyword(Long userId, String keyword) {
        LambdaQueryWrapper<AudioRecord> audioWrapper = new LambdaQueryWrapper<>();
        audioWrapper.eq(AudioRecord::getUserId, userId)
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
                .and(w -> w.like(QaRecord::getQuestion, keyword)
                        .or()
                        .like(QaRecord::getAnswer, keyword)
                        .or()
                        .like(QaRecord::getTags, keyword));
        
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        return qaRecords.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<QaRecordDTO> searchByTag(Long userId, String tag) {
        LambdaQueryWrapper<AudioRecord> audioWrapper = new LambdaQueryWrapper<>();
        audioWrapper.eq(AudioRecord::getUserId, userId)
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
                .like(QaRecord::getTags, tag);
        
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
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
