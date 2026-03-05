package com.interviewpai.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

@Slf4j
@Service
public class AudioProcessService {
    
    private final AudioRecordMapper audioRecordMapper;
    private final AudioTranscriptMapper audioTranscriptMapper;
    private final QaRecordMapper qaRecordMapper;
    private final InterviewSummaryMapper interviewSummaryMapper;
    private final MinioService minioService;
    private final AsrService asrService;
    private final DeepSeekService deepSeekService;
    private final AudioService audioService;
    
    public AudioProcessService(AudioRecordMapper audioRecordMapper, 
                              AudioTranscriptMapper audioTranscriptMapper,
                              QaRecordMapper qaRecordMapper,
                              InterviewSummaryMapper interviewSummaryMapper,
                              MinioService minioService,
                              AsrService asrService,
                              DeepSeekService deepSeekService,
                              AudioService audioService) {
        this.audioRecordMapper = audioRecordMapper;
        this.audioTranscriptMapper = audioTranscriptMapper;
        this.qaRecordMapper = qaRecordMapper;
        this.interviewSummaryMapper = interviewSummaryMapper;
        this.minioService = minioService;
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
            
            String audioUrl = minioService.getPresignedUrl(audioRecord.getFileUrl());
            
            String transcript = asrService.transcribe(audioUrl);
            log.info("语音识别完成，audioId: {}", audioId);
            
            String transcriptObjectName = minioService.uploadText(transcript, "transcript", audioId + ".txt");
            
            AudioTranscript audioTranscript = new AudioTranscript();
            audioTranscript.setAudioId(audioId);
            audioTranscript.setTranscriptText(transcript);
            audioTranscript.setTranscriptUrl(transcriptObjectName);
            audioTranscriptMapper.insert(audioTranscript);
            
            String qaResult = deepSeekService.extractQA(transcript);
            log.info("问答提取完成，audioId: {}", audioId);
            
            JSONObject qaJson = JSONUtil.parseObj(qaResult);
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
            
            JSONObject summaryJson = JSONUtil.parseObj(summaryResult);
            
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
}
