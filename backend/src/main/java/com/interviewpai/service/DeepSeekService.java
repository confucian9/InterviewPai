package com.interviewpai.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
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
    
    public String generateExperience(String title, String transcript, String qaList, String summary) {
        String systemPrompt = """
                你是一个面试经验整理专家。请根据以下面试内容生成一份结构清晰、内容丰富的面经文档。
                
                要求：
                1. 使用 Markdown 格式输出
                2. 标题要吸引人，突出面试特点
                3. 内容要包含：面试背景、问题汇总、经验总结、注意事项等
                4. 语言要生动有趣，便于阅读
                5. 适当添加表情符号增加可读性
                6. 对于技术问题，给出标准答案或参考思路
                """;
        
        StringBuilder userMessage = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            userMessage.append("面经标题：").append(title).append("\n\n");
        }
        userMessage.append("面试转写文本：\n").append(transcript).append("\n\n");
        userMessage.append("问答列表：\n").append(qaList).append("\n\n");
        userMessage.append("面试总结：\n").append(summary);
        
        return chat(systemPrompt, userMessage.toString(), 0.8);
    }
    
    public void chatStream(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        chatStream(systemPrompt, userMessage, 0.7, onChunk);
    }
    
    public void chatStream(String systemPrompt, String userMessage, double temperature, Consumer<String> onChunk) {
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "deepseek-chat");
        requestBody.set("temperature", temperature);
        requestBody.set("stream", true);
        
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
            URL url = new URL(baseUrl + "/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "text/event-stream");
            connection.setDoOutput(true);
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(300000);
            
            connection.getOutputStream().write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
            connection.getOutputStream().flush();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("DeepSeek API调用失败: " + responseCode);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            StringBuilder fullContent = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    
                    try {
                        JSONObject chunk = JSONUtil.parseObj(data);
                        JSONArray choices = chunk.getJSONArray("choices");
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                            if (delta != null && delta.containsKey("content")) {
                                String content = delta.getStr("content");
                                if (content != null) {
                                    fullContent.append(content);
                                    onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("解析SSE数据失败: {}", data);
                    }
                }
            }
            
            reader.close();
            connection.disconnect();
            
        } catch (Exception e) {
            log.error("DeepSeek流式API调用异常", e);
            throw new RuntimeException("DeepSeek流式API调用异常: " + e.getMessage());
        }
    }
    
    public void extractQAStream(String transcript, Consumer<String> onChunk) {
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
        
        chatStream(systemPrompt, transcript, onChunk);
    }
    
    public void summarizeStream(String transcript, String qaList, Consumer<String> onChunk) {
        String systemPrompt = """
                你是一个面试经验分析专家。请根据以下面试内容生成一份面试总结。
                
                请按照以下JSON格式输出，不要输出其他内容：
                {
                    "summary": "面试总结内容，包括面试的主要话题、候选人的表现评价等",
                    "keywords": ["关键词1", "关键词2", "关键词3"]
                }
                """;
        
        String userMessage = "面试转写文本：\n" + transcript + "\n\n问答列表：\n" + qaList;
        chatStream(systemPrompt, userMessage, onChunk);
    }
}
