package com.skillradar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 某个用户和一条招聘信息的技能匹配度。
 * basis = posting：按这条 JD 里抽取出的技能算；
 * basis = category：这条 JD 没抽出技能，退而按同类岗位的常见技能要求估算。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingMatch {
    private int percent;
    private String basis;
    private List<SkillSummary> matched;
    private List<SkillSummary> missing;
}
