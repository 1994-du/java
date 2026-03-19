package com.springbootproject.Service;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeepseekService {

    private static final String API_KEY = "sk-f15f54c7e8914b769da5745644b2838a";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    public String callDeepseek(String message) {
        RestTemplate restTemplate = new RestTemplate();

        String body = """
        {
          "model": "deepseek-chat",
          "messages": [
            {
              "role": "user",
              "content": "%s"
            }
          ],
          "temperature": 0.7
        }
        """.formatted(message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(API_URL, request, String.class);

        return response.getBody();
    }
}