package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 激励机制里的一枚徽章。earned=false 的也返回，前端置灰展示，让用户知道下一个目标是什么。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Badge {
    private String key;
    private String icon;
    private String name;
    private String description;
    private boolean earned;
}
