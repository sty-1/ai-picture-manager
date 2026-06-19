package com.nanhua.yupicturebackend.api.imagesearch;

import cn.hutool.core.util.URLUtil;
import com.nanhua.yupicturebackend.api.imagesearch.model.ImageSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 构建百度公开以图搜图页面 URL（graph.baidu.com/upload 已封禁，改用公开页面）
     *
     * @param imageUrl 图片 URL
     * @return 百度以图搜图结果页面 URL
     */
    public static String getPublicSearchUrl(String imageUrl) {
        String encodedUrl = URLUtil.encode(imageUrl, StandardCharsets.UTF_8);
        return "https://image.baidu.com/pcdutu?queryImageUrl=" + encodedUrl;
    }

    /**
     * 搜索图片（兼容旧接口，返回包含百度搜索 URL 的结果）
     *
     * @param imageUrl 图片 URL
     * @return 包含百度搜索跳转地址的结果列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String searchUrl = getPublicSearchUrl(imageUrl);
        List<ImageSearchResult> list = new ArrayList<>();
        ImageSearchResult result = new ImageSearchResult();
        result.setThumbUrl(imageUrl);
        result.setFromUrl(searchUrl);
        list.add(result);
        return list;
    }
}
