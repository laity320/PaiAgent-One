package com.paiagent.skill.injector;

import com.paiagent.skill.model.KnowledgeRef;
import com.paiagent.skill.model.Skill;
import com.paiagent.skill.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SkillInjector —— 将 Skill 知识包注入到提示词中。
 *
 * <p>支持两种注入模式：
 * <ul>
 *   <li>FULL：全量注入，将所有 references 拼接后追加到 systemPrompt</li>
 *   <li>PROGRESSIVE：渐进式注入，按权重筛选 Top-K 片段注入</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillInjector {

    private final SkillRegistry skillRegistry;

    private static final int PROGRESSIVE_TOP_K = 3;

    /**
     * 将指定 Skill 注入到系统提示词中。
     *
     * @param systemPrompt 原始系统提示词
     * @param skillId      Skill 标识
     * @return 注入后的系统提示词
     */
    public String inject(String systemPrompt, String skillId) {
        if (skillId == null || skillId.isEmpty()) {
            return systemPrompt;
        }
        Skill skill = skillRegistry.getSkill(skillId);
        if (skill == null) {
            log.warn("未找到 Skill: {}", skillId);
            return systemPrompt;
        }
        return inject(systemPrompt, skill);
    }

    /**
     * 将 Skill 注入到系统提示词中。
     */
    public String inject(String systemPrompt, Skill skill) {
        if (skill == null || skill.getReferences() == null || skill.getReferences().isEmpty()) {
            return systemPrompt;
        }

        String knowledgeContent = switch (skill.getInjectionMode()) {
            case FULL -> injectFull(skill.getReferences());
            case PROGRESSIVE -> injectProgressive(skill.getReferences());
        };

        if (knowledgeContent.isEmpty()) {
            return systemPrompt;
        }

        // 拼接格式：原始提示词 + "\n\n【知识库】\n" + 知识内容
        String base = (systemPrompt == null || systemPrompt.isEmpty()) ? "" : systemPrompt;
        return base + "\n\n【知识库】\n" + knowledgeContent;
    }

    /**
     * 全量注入：拼接所有引用内容。
     */
    private String injectFull(List<KnowledgeRef> refs) {
        return refs.stream()
                .map(skillRegistry::getReferenceContent)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 渐进式注入：按权重排序取 Top-K。
     */
    private String injectProgressive(List<KnowledgeRef> refs) {
        return refs.stream()
                .sorted(Comparator.comparingDouble(KnowledgeRef::getWeight).reversed())
                .limit(PROGRESSIVE_TOP_K)
                .map(skillRegistry::getReferenceContent)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 仅返回知识内容（不与提示词拼接），用于用户提示词注入。
     */
    public String getKnowledgeContent(String skillId) {
        Skill skill = skillRegistry.getSkill(skillId);
        if (skill == null) return "";
        return switch (skill.getInjectionMode()) {
            case FULL -> injectFull(skill.getReferences());
            case PROGRESSIVE -> injectProgressive(skill.getReferences());
        };
    }
}
