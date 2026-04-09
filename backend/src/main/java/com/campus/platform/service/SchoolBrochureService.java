package com.campus.platform.service;

import com.campus.platform.entity.SchoolBrochure;
import com.campus.platform.repository.SchoolBrochureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolBrochureService {

    private final SchoolBrochureRepository brochureRepository;

    public SchoolBrochure getBySchool(UUID schoolId) {
        return brochureRepository.findBySchoolId(schoolId).orElse(null);
    }

    public List<SchoolBrochure> getAll() {
        return brochureRepository.selectList(null);
    }

    @Transactional
    public void save(UUID schoolId, String title, String content) {
        SchoolBrochure existing = brochureRepository.findBySchoolId(schoolId).orElse(null);
        if (existing != null) {
            existing.setTitle(title);
            existing.setContent(content);
            brochureRepository.updateById(existing);
            log.info("更新招生简章: schoolId={}", schoolId);
        } else {
            SchoolBrochure brochure = new SchoolBrochure();
            brochure.setBrochureId(UUID.randomUUID());
            brochure.setSchoolId(schoolId);
            brochure.setTitle(title);
            brochure.setContent(content);
            brochure.setStatus("DRAFT");
            brochureRepository.insert(brochure);
            log.info("新建招生简章: schoolId={}", schoolId);
        }
    }
}
