package com.skillradar.dto;

import lombok.Data;

/**
 * "个人中心"页保存资料用。这是整体替换（PUT），不是按字段的增量更新——
 * 前端表单本来就把当前值都回显出来了，直接把编辑后的完整表单提交上来最简单，
 * 不用纠结"null 到底是没传还是故意清空"这种局部更新才有的歧义。
 */
@Data
public class ProfileUpdateRequest {
    private String nickname;
    private Long majorId;
    private String location;
    private String targetLocation;
}
