package com.paiagent.skill.registry;

import com.paiagent.skill.model.KnowledgeRef;
import com.paiagent.skill.model.Skill;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkillRegistry —— Skill 自动加载与注册中心。
 *
 * <p>启动时自动扫描 classpath:skills/*.json 并加载所有预置知识包。
 * 同时管理 Reference 内容缓存，避免重复 I/O。
 */
@Slf4j
@Component
public class SkillRegistry {

    /** id -> Skill 映射 */
    private final Map<String, Skill> skillMap = new ConcurrentHashMap<>();

    /** refId -> 已解析内容缓存 */
    private final Map<String, String> contentCache = new ConcurrentHashMap<>();

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @PostConstruct
    public void init() {
        loadBuiltinSkills();
        log.info("SkillRegistry 加载完成: 共 {} 个 Skill", skillMap.size());
    }

    /**
     * 扫描并加载 classpath:skills/*.json 文件。
     */
    private void loadBuiltinSkills() {
        try {
            Resource[] resources = resolver.getResources("classpath:skills/*.json");
            for (Resource res : resources) {
                try {
                    String json = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    Skill skill = parseSkillFromJson(json);
                    if (skill != null && skill.getId() != null) {
                        skillMap.put(skill.getId(), skill);
                        log.info("SkillRegistry 加载 Skill: id={}, name={}", skill.getId(), skill.getName());
                    }
                } catch (Exception e) {
                    log.warn("加载 Skill 文件失败: {}, error={}", res.getFilename(), e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("扫描 Skill 文件失败: {}", e.getMessage());
        }

        // 如果没有外部文件，注册内置示例
        if (skillMap.isEmpty()) {
            registerBuiltinSkills();
        }
    }

    /**
     * 注册内置示例 Skill（无外部配置时使用）。
     */
    private void registerBuiltinSkills() {
        Skill legalAssistant = Skill.builder()
                .id("legal-assistant")
                .name("法律助手")
                .description("具备中国法律法规知识，可回答法律相关问题")
                .injectionMode(Skill.InjectionMode.FULL)
                .references(List.of(
                        KnowledgeRef.builder()
                                .id("legal-base")
                                .contentType(KnowledgeRef.ContentType.TEXT)
                                .content("你是一名专业的法律助手，熟悉《民法典》、《刑法》、《合同法》等中国法律法规。")
                                .weight(1.0)
                                .build()
                ))
                .build();
        skillMap.put(legalAssistant.getId(), legalAssistant);

        Skill codeReviewer = Skill.builder()
                .id("code-reviewer")
                .name("代码审查员")
                .description("专业代码审查，提供优化建议")
                .injectionMode(Skill.InjectionMode.PROGRESSIVE)
                .references(List.of(
                        KnowledgeRef.builder()
                                .id("review-guide")
                                .contentType(KnowledgeRef.ContentType.TEXT)
                                .content("你是一名资深代码审查员，请从代码质量、性能、安全性、可维护性等角度审查代码。")
                                .weight(1.0)
                                .build()
                ))
                .build();
        skillMap.put(codeReviewer.getId(), codeReviewer);
    }

    /**
     * 简易 JSON 解析（不依赖 Jackson 以减少依赖，实际生产应使用 ObjectMapper）。
     * 这里仅作演示，实际使用时建议注入 ObjectMapper。
     */
    private Skill parseSkillFromJson(String json) {
        // 简化实现：假设使用 Jackson 解析
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Skill.class);
        } catch (Exception e) {
            log.warn("解析 Skill JSON 失败: {}", e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // 公开 API
    // -----------------------------------------------------------------------

    public Skill getSkill(String id) {
        return skillMap.get(id);
    }

    public Collection<Skill> getAllSkills() {
        return skillMap.values();
    }

    /**
     * 获取引用的已解析内容（带缓存）。
     */
    public String getReferenceContent(KnowledgeRef ref) {
        return contentCache.computeIfAbsent(ref.getId(), k -> resolveContent(ref));
    }

    private String resolveContent(KnowledgeRef ref) {
        if (ref.getContentType() == KnowledgeRef.ContentType.TEXT) {
            return ref.getContent();
        }
        // URL / FILE 类型：实际应发起 HTTP 请求或读取文件
        // 此处简化处理，返回占位符
        log.warn("暂不支持的 ContentType: {}", ref.getContentType());
        return ref.getContent();
    }

    /**
     * 注册新的 Skill（运行时动态添加）。
     */
    public void register(Skill skill) {
        if (skill != null && skill.getId() != null) {
            skillMap.put(skill.getId(), skill);
            log.info("SkillRegistry 注册 Skill: id={}", skill.getId());
        }
    }
}
