package com.nanhua.yupicturebackend.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nanhua.yupicturebackend.api.aliyunai.AliYunAiApi;
import com.nanhua.yupicturebackend.exception.BusinessException;
import com.nanhua.yupicturebackend.exception.ErrorCode;
import com.nanhua.yupicturebackend.mapper.PictureMapper;
import com.nanhua.yupicturebackend.model.entity.Picture;
import com.nanhua.yupicturebackend.model.enums.PictureReviewStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI 内容安全审核服务
 */
@Slf4j
@Service
public class ContentSafetyService {

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String SAFETY_PROMPT =
            "你是一个严格的内容安全审核专家，服务于公共图库平台。\n"
                    + "\n"
                    + "## 检查项\n"
                    + "1. 涉黄：裸露、性暗示、低俗色情内容\n"
                    + "2. 涉政：政治敏感符号、旗帜、人物画像、敏感标语\n"
                    + "3. 暴力血腥：打斗流血、恐怖场景、自残画面\n"
                    + "4. 违法：赌博、毒品、管制物品\n"
                    + "\n"
                    + "## 判定标准\n"
                    + "- safe：图片内容健康，无任何违规\n"
                    + "- suspicious：内容灰色地带，可能引起争议但不确定违规\n"
                    + "- danger：明确含有上述违规内容\n"
                    + "\n"
                    + "## 输出要求\n"
                    + "只输出一行合法 JSON，不要 markdown 包裹，不要解释。\n"
                    + "格式：{\"safe\":true,\"riskLevel\":\"safe|suspicious|danger\",\"reason\":\"判定理由\"}";

    /**
     * 异步审核图片内容安全（仅公共图库）
     */
    @Async
    public void reviewPictureAsync(Picture picture) {
        if (picture == null || picture.getSpaceId() != null) {
            return; // 只审核公共图库图片
        }

        String imageUrl = picture.getUrl();
        if (StrUtil.isBlank(imageUrl)) {
            return;
        }

        try {
            String result = callSafetyAPI(imageUrl);
            JSONObject json = JSONUtil.parseObj(result);

            boolean safe = json.getBool("safe", true);
            String riskLevel = json.getStr("riskLevel", "safe");
            String reason = json.getStr("reason", "AI 自动审核");

            int newStatus;
            String newMessage;
            Date newTime = null;

            if ("danger".equals(riskLevel) || !safe) {
                newStatus = PictureReviewStatusEnum.REJECT.getValue();
                newMessage = "AI 审核拒绝：" + reason;
                newTime = new Date();
            } else if ("suspicious".equals(riskLevel)) {
                newStatus = PictureReviewStatusEnum.REVIEWING.getValue();
                newMessage = "AI 审核待定：" + reason;
            } else {
                newStatus = PictureReviewStatusEnum.PASS.getValue();
                newMessage = "AI 审核通过";
                newTime = new Date();
            }

            // 使用 LambdaUpdateWrapper 确保 WHERE 条件包含分片键 spaceId，ShardingSphere 才能正确路由
            LambdaUpdateWrapper<Picture> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Picture::getId, picture.getId())
                   .isNull(Picture::getSpaceId);
            wrapper.set(Picture::getReviewStatus, newStatus);
            wrapper.set(Picture::getReviewMessage, newMessage);
            if (newTime != null) {
                wrapper.set(Picture::getReviewTime, newTime);
            }
            int rows = pictureMapper.update(null, wrapper);
            if (rows > 0) {
                log.info("AI 审核更新成功，图片 id={}，状态={}，原因={}", picture.getId(), newStatus, newMessage);
                // 清除公开图库缓存
                Set<String> keys = stringRedisTemplate.keys("yupicture:listPictureVOByPage:*");
                if (keys != null && !keys.isEmpty()) {
                    stringRedisTemplate.delete(keys);
                }
            } else {
                log.warn("AI 审核更新失败（0 行受影响），图片 id={}，请检查分片路由", picture.getId());
            }
        } catch (Exception e) {
            log.error("AI 安全审核异常，图片 id={}", picture.getId(), e);
        }
    }

    private String callSafetyAPI(String imageUrl) {
        String apiKey = aliYunAiApi.getApiKey();
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI API Key 未配置");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "qwen-vl-max");

        // response_format
        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        // messages
        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");

        Map<String, Object> imagePart = new LinkedHashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrlObj = new LinkedHashMap<>();
        imageUrlObj.put("url", imageUrl);
        imagePart.put("image_url", imageUrlObj);

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", SAFETY_PROMPT);

        userMsg.put("content", new Object[]{imagePart, textPart});
        body.put("messages", Collections.singletonList(userMsg));

        HttpResponse response = HttpRequest
                .post("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .execute();

        if (!response.isOk()) {
            log.error("AI 安全审核 API 调用失败：{}", response.body());
            return "{\"safe\":true,\"riskLevel\":\"safe\",\"reason\":\"API 调用异常，默认通过\"}";
        }

        JSONObject respJson = JSONUtil.parseObj(response.body());
        String content = respJson.getByPath("choices[0].message.content", String.class);
        if (StrUtil.isBlank(content)) {
            return "{\"safe\":true,\"riskLevel\":\"safe\",\"reason\":\"AI 无输出，默认通过\"}";
        }

        // 清理可能的 markdown 包裹
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```(?:json)?\\s*", "")
                    .replaceAll("\\s*```$", "");
        }
        return content;
    }
}
