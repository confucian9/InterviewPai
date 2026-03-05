package com.interviewpai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpai.common.PageResult;
import com.interviewpai.dto.*;
import com.interviewpai.entity.*;
import com.interviewpai.mapper.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewService {
    
    private final AudioRecordMapper audioRecordMapper;
    private final AudioTranscriptMapper audioTranscriptMapper;
    private final QaRecordMapper qaRecordMapper;
    private final InterviewSummaryMapper interviewSummaryMapper;
    private final MinioService minioService;
    
    public InterviewService(AudioRecordMapper audioRecordMapper,
                           AudioTranscriptMapper audioTranscriptMapper,
                           QaRecordMapper qaRecordMapper,
                           InterviewSummaryMapper interviewSummaryMapper,
                           MinioService minioService) {
        this.audioRecordMapper = audioRecordMapper;
        this.audioTranscriptMapper = audioTranscriptMapper;
        this.qaRecordMapper = qaRecordMapper;
        this.interviewSummaryMapper = interviewSummaryMapper;
        this.minioService = minioService;
    }
    
    public PageResult<AudioRecordDTO> getInterviewList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<AudioRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AudioRecord::getUserId, userId)
                .in(AudioRecord::getStatus, "FINISHED", "PROCESSING")
                .orderByDesc(AudioRecord::getCreateTime);
        
        Page<AudioRecord> pageParam = new Page<>(page, size);
        Page<AudioRecord> result = audioRecordMapper.selectPage(pageParam, wrapper);
        
        List<AudioRecordDTO> records = result.getRecords().stream()
                .map(this::toAudioDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    public InterviewDetailDTO getInterviewDetail(Long userId, Long audioId) {
        AudioRecord audioRecord = audioRecordMapper.selectById(audioId);
        if (audioRecord == null || !audioRecord.getUserId().equals(userId)) {
            throw new RuntimeException("面试记录不存在");
        }
        
        InterviewDetailDTO dto = new InterviewDetailDTO();
        dto.setId(audioRecord.getId());
        dto.setFileName(audioRecord.getFileName());
        dto.setFileUrl(minioService.getPresignedUrl(audioRecord.getFileUrl()));
        dto.setDuration(audioRecord.getDuration());
        dto.setStatus(audioRecord.getStatus());
        dto.setCreateTime(audioRecord.getCreateTime());
        
        LambdaQueryWrapper<AudioTranscript> transcriptWrapper = new LambdaQueryWrapper<>();
        transcriptWrapper.eq(AudioTranscript::getAudioId, audioId);
        AudioTranscript transcript = audioTranscriptMapper.selectOne(transcriptWrapper);
        if (transcript != null) {
            dto.setTranscript(transcript.getTranscriptText());
        }
        
        LambdaQueryWrapper<QaRecord> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.eq(QaRecord::getAudioId, audioId)
                .orderByAsc(QaRecord::getCreateTime);
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        List<QaRecordDTO> qaList = qaRecords.stream()
                .map(this::toQaDTO)
                .collect(Collectors.toList());
        dto.setQaList(qaList);
        
        LambdaQueryWrapper<InterviewSummary> summaryWrapper = new LambdaQueryWrapper<>();
        summaryWrapper.eq(InterviewSummary::getAudioId, audioId);
        InterviewSummary summary = interviewSummaryMapper.selectOne(summaryWrapper);
        if (summary != null) {
            dto.setSummary(toSummaryDTO(summary));
        }
        
        return dto;
    }
    
    public PageResult<QaRecordDTO> getQaList(Long userId, Long audioId, Integer page, Integer size) {
        AudioRecord audioRecord = audioRecordMapper.selectById(audioId);
        if (audioRecord == null || !audioRecord.getUserId().equals(userId)) {
            throw new RuntimeException("面试记录不存在");
        }
        
        LambdaQueryWrapper<QaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QaRecord::getAudioId, audioId)
                .orderByAsc(QaRecord::getCreateTime);
        
        Page<QaRecord> pageParam = new Page<>(page, size);
        Page<QaRecord> result = qaRecordMapper.selectPage(pageParam, wrapper);
        
        List<QaRecordDTO> records = result.getRecords().stream()
                .map(this::toQaDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    public void updateQa(Long userId, Long qaId, UpdateQaRequest request) {
        QaRecord qaRecord = qaRecordMapper.selectById(qaId);
        if (qaRecord == null) {
            throw new RuntimeException("问答记录不存在");
        }
        
        AudioRecord audioRecord = audioRecordMapper.selectById(qaRecord.getAudioId());
        if (audioRecord == null || !audioRecord.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此记录");
        }
        
        if (request.getQuestion() != null) {
            qaRecord.setQuestion(request.getQuestion());
        }
        if (request.getAnswer() != null) {
            qaRecord.setAnswer(request.getAnswer());
        }
        if (request.getTags() != null) {
            qaRecord.setTags(String.join(",", request.getTags()));
        }
        
        qaRecordMapper.updateById(qaRecord);
    }
    
    public void deleteQa(Long userId, Long qaId) {
        QaRecord qaRecord = qaRecordMapper.selectById(qaId);
        if (qaRecord == null) {
            throw new RuntimeException("问答记录不存在");
        }
        
        AudioRecord audioRecord = audioRecordMapper.selectById(qaRecord.getAudioId());
        if (audioRecord == null || !audioRecord.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此记录");
        }
        
        qaRecordMapper.deleteById(qaId);
    }
    
    private AudioRecordDTO toAudioDTO(AudioRecord record) {
        AudioRecordDTO dto = new AudioRecordDTO();
        dto.setId(record.getId());
        dto.setFileName(record.getFileName());
        dto.setFileUrl(minioService.getPresignedUrl(record.getFileUrl()));
        dto.setDuration(record.getDuration());
        dto.setStatus(record.getStatus());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }
    
    private QaRecordDTO toQaDTO(QaRecord record) {
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
    
    private InterviewSummaryDTO toSummaryDTO(InterviewSummary summary) {
        InterviewSummaryDTO dto = new InterviewSummaryDTO();
        dto.setId(summary.getId());
        dto.setAudioId(summary.getAudioId());
        dto.setSummary(summary.getSummary());
        dto.setKeywords(summary.getKeywords());
        dto.setCreateTime(summary.getCreateTime());
        return dto;
    }
}
