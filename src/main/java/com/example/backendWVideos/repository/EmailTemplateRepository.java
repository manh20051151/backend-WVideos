package com.example.backendWVideos.repository;

import com.example.backendWVideos.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, String> {

    Optional<EmailTemplate> findByTemplateKey(String templateKey);

    void deleteByTemplateKey(String templateKey);
}
