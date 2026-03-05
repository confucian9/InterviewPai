package com.interviewpai.service;

import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

@Slf4j
@Service
public class AsrService {
    
    @Value("${dashscope.api-key}")
    private String apiKey;
    
    public String transcribe(String audioUrl) {
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
        
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("DASHSCOPE_API_KEY 环境变量未配置");
            }
            
            log.info("开始语音识别，audioUrl: {}", audioUrl);
            
            TranscriptionParam param = TranscriptionParam.builder()
                    .apiKey(apiKey)
                    .model("fun-asr")
                    .parameter("language_hints", new String[]{"zh", "en"})
                    .fileUrls(Arrays.asList(audioUrl))
                    .build();
            
            Transcription transcription = new Transcription();
            TranscriptionResult result = transcription.asyncCall(param);
            log.info("提交转写请求成功，RequestId: {}, TaskId: {}", result.getRequestId(), result.getTaskId());
            
            result = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
            log.info("转写任务完成，结果: {}", result);
            
            if (result.getResults() != null && !result.getResults().isEmpty()) {
                TranscriptionTaskResult taskResult = result.getResults().get(0);
                log.info("TaskResult: {}", taskResult);
                
                if (taskResult == null) {
                    throw new RuntimeException("转写任务结果为空");
                }
                
                String transcriptionUrl = taskResult.getTranscriptionUrl();
                log.info("TranscriptionUrl: {}", transcriptionUrl);
                
                if (transcriptionUrl == null || transcriptionUrl.isEmpty()) {
                    log.error("转写结果URL为空，TaskResult: {}", taskResult);
                    throw new RuntimeException("转写结果URL为空");
                }
                return fetchTranscriptionResult(transcriptionUrl);
            }
            
            throw new RuntimeException("语音识别失败：无结果返回");
        } catch (Exception e) {
            log.error("语音识别失败", e);
            throw new RuntimeException("语音识别失败: " + e.getMessage());
        }
    }
    
    private String fetchTranscriptionResult(String transcriptionUrl) {
        try {
            if (transcriptionUrl == null || transcriptionUrl.isEmpty()) {
                throw new RuntimeException("转写结果URL为空");
            }
            
            URL url = new URL(transcriptionUrl);
            if (url == null) {
                throw new RuntimeException("URL对象创建失败");
            }
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                throw new RuntimeException("HTTP连接创建失败");
            }
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP请求失败，状态码: " + responseCode);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            String responseBody = response.toString();
            if (responseBody == null || responseBody.isEmpty()) {
                throw new RuntimeException("获取到的转写结果为空");
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject jsonResult = gson.fromJson(responseBody, JsonObject.class);
            
            return parseTranscriptionResult(jsonResult);
        } catch (Exception e) {
            throw new RuntimeException("获取转写结果失败: " + e.getMessage());
        }
    }
    
    private String parseTranscriptionResult(JsonObject jsonResult) {
        StringBuilder result = new StringBuilder();
        
        try {
            if (jsonResult == null) {
                return "JSON结果为空";
            }
            
            if (jsonResult.has("transcripts")) {
                com.google.gson.JsonArray transcripts = jsonResult.getAsJsonArray("transcripts");
                if (transcripts != null && !transcripts.isEmpty()) {
                    for (com.google.gson.JsonElement transcript : transcripts) {
                        if (transcript != null && transcript.isJsonObject()) {
                            JsonObject transcriptObj = transcript.getAsJsonObject();
                            if (transcriptObj.has("text")) {
                                String text = transcriptObj.get("text").getAsString();
                                if (text != null && !text.isEmpty()) {
                                    result.append(text).append("\n");
                                }
                            }
                        }
                    }
                }
            } else if (jsonResult.has("results")) {
                com.google.gson.JsonArray results = jsonResult.getAsJsonArray("results");
                if (results != null && !results.isEmpty()) {
                    for (com.google.gson.JsonElement element : results) {
                        if (element != null && element.isJsonObject()) {
                            JsonObject resultObj = element.getAsJsonObject();
                            if (resultObj.has("transcription_url")) {
                                String url = resultObj.get("transcription_url").getAsString();
                                if (url != null && !url.isEmpty()) {
                                    return fetchTranscriptionResult(url);
                                }
                            }
                        }
                    }
                }
            }
            
            if (result.length() == 0) {
                result.append(jsonResult.toString());
            }
        } catch (Exception e) {
            if (jsonResult != null) {
                result.append(jsonResult.toString());
            } else {
                result.append("解析失败: " + e.getMessage());
            }
        }
        
        return result.toString();
    }
}