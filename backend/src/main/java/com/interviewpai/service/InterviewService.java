package com.interviewpai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpai.common.PageResult;
import com.interviewpai.dto.*;
import com.interviewpai.entity.*;
import com.interviewpai.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterviewService {
    
    private final AudioRecordMapper audioRecordMapper;
    private final AudioTranscriptMapper audioTranscriptMapper;
    private final QaRecordMapper qaRecordMapper;
    private final InterviewSummaryMapper interviewSummaryMapper;
    private final OssService ossService;
    private final DeepSeekService deepSeekService;
    
    public InterviewService(AudioRecordMapper audioRecordMapper,
                           AudioTranscriptMapper audioTranscriptMapper,
                           QaRecordMapper qaRecordMapper,
                           InterviewSummaryMapper interviewSummaryMapper,
                           OssService ossService,
                           DeepSeekService deepSeekService) {
        this.audioRecordMapper = audioRecordMapper;
        this.audioTranscriptMapper = audioTranscriptMapper;
        this.qaRecordMapper = qaRecordMapper;
        this.interviewSummaryMapper = interviewSummaryMapper;
        this.ossService = ossService;
        this.deepSeekService = deepSeekService;
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
        dto.setFileUrl(ossService.getPresignedUrl(audioRecord.getFileUrl()));
        dto.setDuration(audioRecord.getDuration());
        dto.setStatus(audioRecord.getStatus());
        dto.setCreateTime(audioRecord.getCreateTime());
        
        LambdaQueryWrapper<AudioTranscript> transcriptWrapper = new LambdaQueryWrapper<>();
        transcriptWrapper.eq(AudioTranscript::getAudioId, audioId)
                .last("LIMIT 1");
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
        summaryWrapper.eq(InterviewSummary::getAudioId, audioId)
                .last("LIMIT 1");
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
    
    public GenerateExperienceResponse generateExperience(Long userId, Long audioId, GenerateExperienceRequest request) {
        AudioRecord audioRecord = audioRecordMapper.selectById(audioId);
        if (audioRecord == null || !audioRecord.getUserId().equals(userId)) {
            throw new RuntimeException("面试记录不存在");
        }
        
        if (!"FINISHED".equals(audioRecord.getStatus())) {
            throw new RuntimeException("面试尚未处理完成，无法生成面经");
        }
        
        LambdaQueryWrapper<AudioTranscript> transcriptWrapper = new LambdaQueryWrapper<>();
        transcriptWrapper.eq(AudioTranscript::getAudioId, audioId)
                .last("LIMIT 1");
        AudioTranscript transcript = audioTranscriptMapper.selectOne(transcriptWrapper);
        if (transcript == null) {
            throw new RuntimeException("面试转写记录不存在");
        }
        
        LambdaQueryWrapper<QaRecord> qaWrapper = new LambdaQueryWrapper<>();
        qaWrapper.eq(QaRecord::getAudioId, audioId)
                .orderByAsc(QaRecord::getCreateTime);
        List<QaRecord> qaRecords = qaRecordMapper.selectList(qaWrapper);
        
        LambdaQueryWrapper<InterviewSummary> summaryWrapper = new LambdaQueryWrapper<>();
        summaryWrapper.eq(InterviewSummary::getAudioId, audioId)
                .last("LIMIT 1");
        InterviewSummary summary = interviewSummaryMapper.selectOne(summaryWrapper);
        
        String title = request != null && request.getTitle() != null && !request.getTitle().isEmpty() 
                ? request.getTitle() 
                : "面经_" + audioId;
        
        log.info("开始生成面经，audioId: {}", audioId);
        
        String markdownContent = buildMarkdownContent(title, transcript.getTranscriptText(), qaRecords, summary);
        
        String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".md";
        String localPath = saveToLocalFile(markdownContent, fileName);
        
        log.info("面经生成完成，audioId: {}, fileName: {}, localPath: {}", audioId, fileName, localPath);
        
        GenerateExperienceResponse response = new GenerateExperienceResponse();
        response.setTitle(title);
        response.setContent(markdownContent);
        response.setFileUrl(localPath);
        
        return response;
    }
    
    private String buildMarkdownContent(String title, String transcript, List<QaRecord> qaRecords, InterviewSummary summary) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# ").append(title).append("\n\n");
        
        sb.append("## 📝 面试总结\n\n");
        if (summary != null && summary.getSummary() != null && !summary.getSummary().isEmpty()) {
            sb.append(summary.getSummary()).append("\n\n");
        } else {
            sb.append("暂无总结\n\n");
        }
        
        if (summary != null && summary.getKeywords() != null && !summary.getKeywords().isEmpty()) {
            sb.append("**关键词：** ");
            String[] keywords = summary.getKeywords().split(",");
            for (int i = 0; i < keywords.length; i++) {
                sb.append("`").append(keywords[i].trim()).append("`");
                if (i < keywords.length - 1) sb.append(" ");
            }
            sb.append("\n\n");
        }
        
        sb.append("---\n\n");
        sb.append("## 💬 问答记录\n\n");
        
        if (qaRecords != null && !qaRecords.isEmpty()) {
            for (int i = 0; i < qaRecords.size(); i++) {
                QaRecord qa = qaRecords.get(i);
                sb.append("### Q").append(i + 1).append("：").append(qa.getQuestion()).append("\n\n");
                sb.append("**答案：**\n\n").append(qa.getAnswer() != null ? qa.getAnswer() : "暂无答案").append("\n\n");
                
                if (qa.getTags() != null && !qa.getTags().isEmpty()) {
                    sb.append("**标签：** ");
                    String[] tags = qa.getTags().split(",");
                    for (int j = 0; j < tags.length; j++) {
                        sb.append("`").append(tags[j].trim()).append("`");
                        if (j < tags.length - 1) sb.append(" ");
                    }
                    sb.append("\n\n");
                }
                sb.append("---\n\n");
            }
        } else {
            sb.append("暂无问答记录\n\n");
        }
        
        sb.append("## 📄 转写原文\n\n");
        sb.append("```\n").append(transcript != null ? transcript : "暂无转写文本").append("\n```\n");
        
        return sb.toString();
    }
    
    private String saveToLocalFile(String content, String fileName) {
        try {
            String uploadDir = System.getProperty("user.home") + "/interviewpai/experience";
            java.nio.file.Path dirPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(dirPath)) {
                java.nio.file.Files.createDirectories(dirPath);
            }
            
            java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir, fileName);
            java.nio.file.Files.writeString(filePath, content, java.nio.charset.StandardCharsets.UTF_8);
            
            return filePath.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("保存面经文件失败: " + e.getMessage());
        }
    }
    
    private String buildQaListJson(List<QaRecord> qaRecords) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"qa_list\": [\n");
        
        for (int i = 0; i < qaRecords.size(); i++) {
            QaRecord qa = qaRecords.get(i);
            sb.append("    {\n");
            sb.append("      \"question\": \"").append(escapeJson(qa.getQuestion())).append("\",\n");
            sb.append("      \"answer\": \"").append(escapeJson(qa.getAnswer())).append("\",\n");
            sb.append("      \"tags\": [").append(qa.getTags() != null ? qa.getTags() : "").append("]\n");
            sb.append("    }");
            if (i < qaRecords.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("  ]\n}");
        return sb.toString();
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    private AudioRecordDTO toAudioDTO(AudioRecord record) {
        AudioRecordDTO dto = new AudioRecordDTO();
        dto.setId(record.getId());
        dto.setFileName(record.getFileName());
        dto.setFileUrl(ossService.getPresignedUrl(record.getFileUrl()));
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
