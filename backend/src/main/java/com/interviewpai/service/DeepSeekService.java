package com.interviewpai.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeepSeekService {
    
    @Value("${deepseek.api-key}")
    private String apiKey;
    
    @Value("${deepseek.base-url}")
    private String baseUrl;
    
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, 0.7);
    }
    
    public String chat(String systemPrompt, String userMessage, double temperature) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "deepseek-chat");
        requestBody.set("temperature", temperature);
        
        JSONArray messages = new JSONArray();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.set("role", "system");
            systemMessage.set("content", systemPrompt);
            messages.add(systemMessage);
        }
        
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);
        
        requestBody.set("messages", messages);
        
        try {
            HttpResponse response = HttpRequest.post(baseUrl + "/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody.toString())
                    .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("DeepSeek API调用失败: " + response.getStatus());
            }
            
            JSONObject responseJson = JSONUtil.parseObj(response.body());
            JSONArray choices = responseJson.getJSONArray("choices");
            
            if (choices != null && !choices.isEmpty()) {
                JSONObject firstChoice = choices.getJSONObject(0);
                JSONObject message = firstChoice.getJSONObject("message");
                return message.getStr("content");
            }
            
            throw new RuntimeException("DeepSeek API返回结果为空");
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek API调用异常: " + e.getMessage());
        }
    }
    
    public String extractQA(String transcript) {
        String systemPrompt = """
                你是一个面试经验分析专家。请从以下面试录音转写的文本中提取出面试问题和对应的答案。
                
                请按照以下JSON格式输出，不要输出其他内容：
                {
                    "qa_list": [
                        {
                            "question": "面试问题",
                            "answer": "候选人的回答",
                            "tags": ["标签1", "标签2"]
                        }
                    ]
                }
                
                注意事项：
                1. 每个问题和答案要准确完整
                2. 标签要简洁，如：Java、并发、JVM、Spring、Redis、MySQL等
                3. 如果无法区分问题和答案，请合理推断
                4. 如果文本中没有明显的问答内容，请提取关键知识点作为问题和答案
                """;
        
        return chat(systemPrompt, transcript);
    }
    
    public String summarize(String transcript, String qaList) {
        String systemPrompt = """
                你是一个面试经验分析专家。请根据以下面试内容生成一份面试总结。
                
                请按照以下JSON格式输出，不要输出其他内容：
                {
                    "summary": "面试总结内容，包括面试的主要话题、候选人的表现评价等",
                    "keywords": ["关键词1", "关键词2", "关键词3"]
                }
                """;
        
        String userMessage = "面试转写文本：\n" + transcript + "\n\n问答列表：\n" + qaList;
        return chat(systemPrompt, userMessage);
    }
}
