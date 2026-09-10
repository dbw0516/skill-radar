package com.skillradar.controller;

import com.skillradar.entity.Major;
import com.skillradar.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorRepository majorRepository;

    @GetMapping
    public List<Major> list() {
        return majorRepository.findAll();
    }
}
