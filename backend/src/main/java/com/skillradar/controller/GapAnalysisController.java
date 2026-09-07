package com.skillradar.controller;

import com.skillradar.dto.GapItem;
import com.skillradar.service.GapAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GapAnalysisController {

    private final GapAnalysisService gapAnalysisService;

    /** userId 先给默认值 1（种子数据里的演示账号）——还没接登录，等前端有用户态了再改成必填。 */
    @GetMapping("/api/gap-analysis")
    public List<GapItem> gapAnalysis(@RequestParam Long categoryId,
                                      @RequestParam(defaultValue = "1") Long userId) {
        return gapAnalysisService.analyze(categoryId, userId);
    }
}
