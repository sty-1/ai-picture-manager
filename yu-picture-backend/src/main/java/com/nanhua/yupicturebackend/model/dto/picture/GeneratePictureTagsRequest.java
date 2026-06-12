package com.nanhua.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.util.List;

@Data
public class GeneratePictureTagsRequest {
    private List<Long> pictureIdList;
}
