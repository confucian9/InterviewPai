package com.interviewpai.service;

import com.alibaba.dashscope.audio.asr.transcription.*;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

@Service
public class AsrService {
    
    @Value("${dashscope.api-key}")
    private String apiKey;
    
    public String transcribe(String audioUrl) {
        try {
            TranscriptionParam param = TranscriptionParam.builder()
                    .apiKey(apiKey)
                    .model("fun-asr")
                    .parameter("language_hints", new String[]{"zh", "en"})
                    .fileUrls(Arrays.asList(audioUrl))
                    .build();
            
            Transcription transcription = new Transcription();
            TranscriptionResult result = transcription.asyncCall(param);
            
            result = transcription.wait(TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
            
            if (result.getResults() != null && !result.getResults().isEmpty()) {
                TranscriptionTaskResult taskResult = result.getResults().get(0);
                String transcriptionUrl = taskResult.getTranscriptionUrl();
                return fetchTranscriptionResult(transcriptionUrl);
            }
            
            throw new RuntimeException("语音识别失败：无结果返回");
        } catch (Exception e) {
            throw new RuntimeException("语音识别失败: " + e.getMessage());
        }
    }
    
    private String fetchTranscriptionResult(String transcriptionUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(transcriptionUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject jsonResult = gson.fromJson(reader, JsonObject.class);
            
            return parseTranscriptionResult(jsonResult);
        } catch (Exception e) {
            throw new RuntimeException("获取转写结果失败: " + e.getMessage());
        }
    }
    
    private String parseTranscriptionResult(JsonObject jsonResult) {
        StringBuilder result = new StringBuilder();
        
        try {
            if (jsonResult.has("transcripts")) {
                JsonArray transcripts = jsonResult.getAsJsonArray("transcripts");
                for (JsonElement transcript : transcripts) {
                    JsonObject transcriptObj = transcript.getAsJsonObject();
                    if (transcriptObj.has("text")) {
                        result.append(transcriptObj.get("text").getAsString()).append("\n");
                    }
                }
            } else if (jsonResult.has("results")) {
                JsonArray results = jsonResult.getAsJsonArray("results");
                for (JsonElement element : results) {
                    JsonObject resultObj = element.getAsJsonObject();
                    if (resultObj.has("transcription_url")) {
                        String url = resultObj.get("transcription_url").getAsString();
                        return fetchTranscriptionResult(url);
                    }
                }
            }
            
            if (result.length() == 0) {
                result.append(jsonResult.toString());
            }
        } catch (Exception e) {
            result.append(jsonResult.toString());
        }
        
        return result.toString();
    }
}
