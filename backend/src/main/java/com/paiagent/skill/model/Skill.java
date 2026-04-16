package com.paiagent.skill.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Skill —— 预置知识包模型。
 *
 * <p>Skill 包含名称、描述、知识片段列表（参考文档、提示词片段等），
 * 可被 LLM 节点注入到系统提示词或用户提示词中。
 */
@Data
@Builder
public class Skill {

    /** 唯一标识，如 "legal-assistant"、"code-reviewer" */
    private String id;

    /** 显示名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 知识片段列表 */
    private List<KnowledgeRef> references;

    /** 默认注入模式：FULL（全量）/ PROGRESSIVE（渐进式） */
    private InjectionMode injectionMode;

    public enum InjectionMode {
        /** 全量注入：一次性将所有 references 拼接注入 */
        FULL,
        /** 渐进式注入：按需加载，根据上下文动态选择相关片段 */
        PROGRESSIVE
    }
}
