package com.nanhua.yupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiTagResult {
    private Long id;
    private String name;
    private String category;
    private List<String> tags;
}
