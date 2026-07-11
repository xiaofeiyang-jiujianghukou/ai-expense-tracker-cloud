package com.example.expense.ai.service;

import com.example.expense.ai.client.LlmClient;
import com.example.expense.ai.dto.CategorizeRequest;
import com.example.expense.ai.dto.CategorizeResponse;
import com.example.expense.category.entity.Category;
import com.example.expense.category.service.CategoryService;
import com.example.expense.common.enums.BillType;
import com.example.expense.common.exception.BusinessException;
import com.example.expense.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiCategoryService {

    private final CategoryService categoryService;
    private final LlmClient llmClient;
    private final AiCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategorizeResponse categorize(CategorizeRequest request, Long userId) {
        // 0. Check cache first
        String cached = cacheService.getCategorize(userId, request.getDescription(), request.getType());
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, CategorizeResponse.class);
            } catch (Exception ignored) { /* cache parse failed, fall through */ }
        }

        // 1. Get all categories for this user by type
        BillType txnType = BillType.valueOf(request.getType());
        List<Category> categories = categoryService.listByUser(userId, txnType);

        if (categories.isEmpty()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 2. Build category list for prompt
        String categoryList = categories.stream()
                .map(c -> String.format("{\"id\": %d, \"name\": \"%s\", \"type\": \"%s\"}",
                        c.getId(), c.getName(), c.getType()))
                .collect(Collectors.joining(", "));

        // 3. Build prompts
        String systemPrompt = """
                You are a personal finance assistant. Your job is to categorize expenses and income.
                Given a bill description, amount, and type, pick the most appropriate category
                from the user's existing categories. Respond ONLY with a JSON object, no other text.""";
        String userMessage = String.format("""
                User's categories: [%s]

                Bill description: "%s"
                Amount: %s
                Type: %s

                Pick the best matching category. Return only JSON:
                {"categoryId": <id>, "categoryName": "<name>", "confidence": 0.0-1.0, "reason": "<brief reason>"}
                If unsure, set confidence to 0 and explain why.""",
                categoryList,
                request.getDescription(),
                request.getAmount().toPlainString(),
                request.getType());

        try {
            String response = llmClient.chat(systemPrompt, userMessage);
            CategorizeResponse result = parseResponse(response, categories);
            // Cache the successful result
            try {
                cacheService.putCategorize(userId, request.getDescription(), request.getType(),
                        objectMapper.writeValueAsString(result));
            } catch (Exception ignored) { /* cache write failed, non-blocking */ }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_CATEGORIZE_FAILED);
        }
    }

    private CategorizeResponse parseResponse(String llmResponse, List<Category> categories) {
        try {
            // Strip markdown code fences if present
            String json = llmResponse.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            JsonNode root = objectMapper.readTree(json);
            long categoryId = root.path("categoryId").asLong();
            String categoryName = root.path("categoryName").asText();
            double confidence = root.path("confidence").asDouble();
            String reason = root.path("reason").asText();

            // Validate that the returned categoryId actually exists in user's categories
            boolean valid = categories.stream().anyMatch(c -> c.getId() == categoryId);
            if (!valid) {
                throw new BusinessException(ErrorCode.AI_CATEGORIZE_FAILED);
            }

            return CategorizeResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(categoryName)
                    .confidence(BigDecimal.valueOf(confidence))
                    .reason(reason)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_CATEGORIZE_FAILED);
        }
    }
}
