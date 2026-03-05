package com.interviewpai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interviewpai.common.PageResult;
import com.interviewpai.dto.AudioRecordDTO;
import com.interviewpai.dto.AudioUploadResponse;
import com.interviewpai.entity.AudioRecord;
import com.interviewpai.mapper.AudioRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class AudioService {
    
    private final AudioRecordMapper audioRecordMapper;
    private final MinioService minioService;
    
    private static final List<String> ALLOWED_TYPES = Arrays.asList("mp3", "wav", "m4a");
    
    public AudioService(AudioRecordMapper audioRecordMapper, MinioService minioService) {
        this.audioRecordMapper = audioRecordMapper;
        this.minioService = minioService;
    }
    
    public AudioUploadResponse uploadAudio(Long userId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new RuntimeException("不支持的文件格式，仅支持 mp3, wav, m4a");
        }
        
        String objectName = minioService.uploadFile(file, "audio");
        String fileUrl = minioService.getPresignedUrl(objectName);
        
        AudioRecord record = new AudioRecord();
        record.setUserId(userId);
        record.setFileName(originalFilename);
        record.setFileUrl(objectName);
        record.setStatus("UPLOADED");
        
        audioRecordMapper.insert(record);
        
        return new AudioUploadResponse(record.getId(), fileUrl, originalFilename);
    }
    
    public PageResult<AudioRecordDTO> getAudioList(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<AudioRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AudioRecord::getUserId, userId)
                .orderByDesc(AudioRecord::getCreateTime);
        
        Page<AudioRecord> pageParam = new Page<>(page, size);
        Page<AudioRecord> result = audioRecordMapper.selectPage(pageParam, wrapper);
        
        List<AudioRecordDTO> records = result.getRecords().stream().map(this::toDTO).toList();
        
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    public AudioRecordDTO getAudioDetail(Long userId, Long audioId) {
        AudioRecord record = audioRecordMapper.selectById(audioId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("音频记录不存在");
        }
        return toDTO(record);
    }
    
    public void deleteAudio(Long userId, Long audioId) {
        AudioRecord record = audioRecordMapper.selectById(audioId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("音频记录不存在");
        }
        
        if (record.getFileUrl() != null) {
            minioService.deleteFile(record.getFileUrl());
        }
        
        audioRecordMapper.deleteById(audioId);
    }
    
    public void updateStatus(Long audioId, String status) {
        AudioRecord record = audioRecordMapper.selectById(audioId);
        if (record != null) {
            record.setStatus(status);
            audioRecordMapper.updateById(record);
        }
    }
    
    private AudioRecordDTO toDTO(AudioRecord record) {
        AudioRecordDTO dto = new AudioRecordDTO();
        dto.setId(record.getId());
        dto.setFileName(record.getFileName());
        dto.setFileUrl(minioService.getPresignedUrl(record.getFileUrl()));
        dto.setDuration(record.getDuration());
        dto.setStatus(record.getStatus());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }
}
