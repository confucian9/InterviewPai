package com.interviewpai.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interviewpai.entity.AudioRecord;
import com.interviewpai.entity.AudioTranscript;
import com.interviewpai.entity.InterviewSummary;
import com.interviewpai.entity.QaRecord;
import com.interviewpai.mapper.AudioRecordMapper;
import com.interviewpai.mapper.AudioTranscriptMapper;
import com.interviewpai.mapper.InterviewSummaryMapper;
import com.interviewpai.mapper.QaRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AudioProcessService {
    
    private final AudioRecordMapper audioRecordMapper;
    private final AudioTranscriptMapper audioTranscriptMapper;
    private final QaRecordMapper qaRecordMapper;
    private final InterviewSummaryMapper interviewSummaryMapper;
    private final OssService ossService;
    private final AsrService asrService;
    private final DeepSeekService deepSeekService;
    private final AudioService audioService;
    
    public AudioProcessService(AudioRecordMapper audioRecordMapper, 
                              AudioTranscriptMapper audioTranscriptMapper,
                              QaRecordMapper qaRecordMapper,
                              InterviewSummaryMapper interviewSummaryMapper,
                              OssService ossService,
                              AsrService asrService,
                              DeepSeekService deepSeekService,
                              AudioService audioService) {
        this.audioRecordMapper = audioRecordMapper;
        this.audioTranscriptMapper = audioTranscriptMapper;
        this.qaRecordMapper = qaRecordMapper;
        this.interviewSummaryMapper = interviewSummaryMapper;
        this.ossService = ossService;
        this.asrService = asrService;
        this.deepSeekService = deepSeekService;
        this.audioService = audioService;
    }
    
    @Async
    @Transactional
    public void processAudio(Long audioId) {
        try {
            log.info("开始处理音频，audioId: {}", audioId);
            
            AudioRecord audioRecord = audioRecordMapper.selectById(audioId);
            if (audioRecord == null) {
                log.error("音频记录不存在，audioId: {}", audioId);
                return;
            }
            
            audioService.updateStatus(audioId, "PROCESSING");
            
            LambdaQueryWrapper<AudioTranscript> transcriptWrapper = new LambdaQueryWrapper<>();
            transcriptWrapper.eq(AudioTranscript::getAudioId, audioId).last("LIMIT 1");
            AudioTranscript existingTranscript = audioTranscriptMapper.selectOne(transcriptWrapper);
            
            String transcript;
            if (existingTranscript != null && existingTranscript.getTranscriptText() != null 
                    && !existingTranscript.getTranscriptText().isEmpty()) {
                log.info("使用已有的转写文本，audioId: {}", audioId);
                transcript = existingTranscript.getTranscriptText();
            } else {
                log.info("开始语音识别，audioId: {}", audioId);
                String audioUrl = ossService.getPresignedUrl(audioRecord.getFileUrl());
                transcript = asrService.transcribe(audioUrl);
                log.info("语音识别完成，audioId: {}", audioId);
                
                String transcriptObjectName = ossService.uploadText(transcript, "transcript", audioId + ".txt");
                
                AudioTranscript audioTranscript = new AudioTranscript();
                audioTranscript.setAudioId(audioId);
                audioTranscript.setTranscriptText(transcript);
                audioTranscript.setTranscriptUrl(transcriptObjectName);
                audioTranscriptMapper.insert(audioTranscript);
            }
            
            List<String> transcriptChunks = splitTextWithOverlap(transcript, 4000, 500);
            String qaResult;
            if (transcriptChunks.size() > 1) {
                log.info("转写文本过长，分{}片处理，audioId: {}", transcriptChunks.size(), audioId);
                qaResult = processQAInChunks(transcriptChunks);
            } else {
                qaResult = deepSeekService.extractQA(transcript);
            }
            log.info("问答提取完成，audioId: {}", audioId);
            log.debug("QA结果原始内容: {}", qaResult);
            
            String qaJsonStr = extractJson(qaResult);
            JSONObject qaJson = JSONUtil.parseObj(qaJsonStr);
            JSONArray qaList = qaJson.getJSONArray("qa_list");
            
            if (qaList != null && !qaList.isEmpty()) {
                for (int i = 0; i < qaList.size(); i++) {
                    JSONObject qa = qaList.getJSONObject(i);
                    QaRecord qaRecord = new QaRecord();
                    qaRecord.setAudioId(audioId);
                    qaRecord.setQuestion(qa.getStr("question"));
                    qaRecord.setAnswer(qa.getStr("answer"));
                    
                    JSONArray tags = qa.getJSONArray("tags");
                    if (tags != null && !tags.isEmpty()) {
                        qaRecord.setTags(String.join(",", tags.stream().map(Object::toString).toList()));
                    }
                    
                    qaRecord.setConfidence(java.math.BigDecimal.valueOf(0.85));
                    qaRecordMapper.insert(qaRecord);
                }
            }
            
            String summaryResult = deepSeekService.summarize(transcript, qaResult);
            log.info("AI总结完成，audioId: {}", audioId);
            log.debug("Summary结果原始内容: {}", summaryResult);
            
            String summaryJsonStr = extractJson(summaryResult);
            JSONObject summaryJson = JSONUtil.parseObj(summaryJsonStr);
            
            InterviewSummary interviewSummary = new InterviewSummary();
            interviewSummary.setAudioId(audioId);
            interviewSummary.setSummary(summaryJson.getStr("summary"));
            
            JSONArray keywords = summaryJson.getJSONArray("keywords");
            if (keywords != null && !keywords.isEmpty()) {
                interviewSummary.setKeywords(String.join(",", keywords.stream().map(Object::toString).toList()));
            }
            
            interviewSummaryMapper.insert(interviewSummary);
            
            audioService.updateStatus(audioId, "FINISHED");
            log.info("音频处理完成，audioId: {}", audioId);
            
        } catch (Exception e) {
            log.error("音频处理失败，audioId: {}, error: {}", audioId, e.getMessage(), e);
            audioService.updateStatus(audioId, "FAILED");
        }
    }
    
    private String extractJson(String content) {
        if (content == null || content.isEmpty()) {
            return "{}";
        }
        
        content = content.trim();
        
        if (content.startsWith("```json")) {
            int startIndex = 7;
            int endIndex = content.lastIndexOf("```");
            if (endIndex > startIndex) {
                content = content.substring(startIndex, endIndex).trim();
            }
        } else if (content.startsWith("```")) {
            int startIndex = 3;
            int endIndex = content.lastIndexOf("```");
            if (endIndex > startIndex) {
                content = content.substring(startIndex, endIndex).trim();
            }
        }
        
        int jsonStart = content.indexOf('{');
        int jsonEnd = content.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content = content.substring(jsonStart, jsonEnd + 1);
        }
        
        return content;
    }
    
    private List<String> splitTextWithOverlap(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            chunks.add("");
            return chunks;
        }
        
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf('。', end);
                int lastQuestion = text.lastIndexOf('？', end);
                int lastExclamation = text.lastIndexOf('！', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int lastComma = text.lastIndexOf('，', end);
                int lastSemicolon = text.lastIndexOf('；', end);
                
                int breakPoint = Math.max(
                    Math.max(lastPeriod, lastQuestion),
                    Math.max(
                        Math.max(lastExclamation, lastNewline),
                        Math.max(lastComma, lastSemicolon)
                    )
                );
                
                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1;
                }
            }
            
            chunks.add(text.substring(start, end).trim());
            
            if (end >= text.length()) {
                break;
            }
            
            start = end - overlap;
            if (start < 0) start = 0;
            
            while (start < text.length() && start > 0 && !isSentenceBoundary(text.charAt(start - 1))) {
                start--;
            }
        }
        
        return chunks;
    }
    
    private boolean isSentenceBoundary(char c) {
        return c == '。' || c == '？' || c == '！' || c == '\n' || c == '，' || c == '；';
    }
    
    private String processQAInChunks(List<String> chunks) {
        List<JSONObject> allQaList = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            log.info("处理第{}片转写文本，共{}片", i + 1, chunks.size());
            String chunkResult = deepSeekService.extractQA(chunks.get(i));
            log.debug("第{}片原始结果: {}", i + 1, chunkResult);
            
            String qaJsonStr = extractJson(chunkResult);
            log.debug("第{}片提取的JSON: {}", i + 1, qaJsonStr);
            
            try {
                JSONObject qaJson = JSONUtil.parseObj(qaJsonStr);
                JSONArray qaList = qaJson.getJSONArray("qa_list");
                if (qaList != null && !qaList.isEmpty()) {
                    for (int j = 0; j < qaList.size(); j++) {
                        try {
                            JSONObject qaItem = qaList.getJSONObject(j);
                            if (qaItem != null) {
                                allQaList.add(qaItem);
                            }
                        } catch (Exception itemEx) {
                            log.warn("解析第{}片第{}条QA失败: {}", i + 1, j + 1, itemEx.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析第{}片QA结果失败: {}, 原始JSON长度: {}", i + 1, e.getMessage(), qaJsonStr.length());
                try {
                    List<JSONObject> extracted = tryExtractQaFromBrokenJson(qaJsonStr);
                    allQaList.addAll(extracted);
                    log.info("从损坏的JSON中提取了{}条QA", extracted.size());
                } catch (Exception ex) {
                    log.error("尝试修复JSON也失败: {}", ex.getMessage());
                }
            }
        }
        
        JSONObject result = new JSONObject();
        result.set("qa_list", allQaList);
        return result.toString();
    }
    
    private List<JSONObject> tryExtractQaFromBrokenJson(String brokenJson) {
        List<JSONObject> results = new ArrayList<>();
        
        try {
            int qaListStart = brokenJson.indexOf("\"qa_list\"");
            if (qaListStart < 0) {
                qaListStart = brokenJson.indexOf("'qa_list'");
            }
            
            if (qaListStart >= 0) {
                int arrayStart = brokenJson.indexOf('[', qaListStart);
                if (arrayStart >= 0) {
                    String arrayContent = brokenJson.substring(arrayStart);
                    
                    int braceCount = 0;
                    int objStart = -1;
                    
                    for (int i = 0; i < arrayContent.length(); i++) {
                        char c = arrayContent.charAt(i);
                        if (c == '{') {
                            if (braceCount == 0) {
                                objStart = i;
                            }
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0 && objStart >= 0) {
                                String objStr = arrayContent.substring(objStart, i + 1);
                                try {
                                    JSONObject obj = JSONUtil.parseObj(objStr);
                                    if (obj.containsKey("question") || obj.containsKey("answer")) {
                                        results.add(obj);
                                    }
                                } catch (Exception e) {
                                    // 忽略单个对象解析失败
                                }
                                objStart = -1;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("尝试从损坏JSON提取失败: {}", e.getMessage());
        }
        
        return results;
    }
}
