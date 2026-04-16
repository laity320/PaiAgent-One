package com.paiagent.skill.model;

import lombok.Builder;
import lombok.Data;

/**
 * KnowledgeRef —— 知识引用片段。
 *
 * <p>每个引用可包含静态内容或动态引用外部资源（URL/文件），
 * 支持缓存以避免重复加载。
 */
@Data
@Builder
public class KnowledgeRef {

    /** 引用标识 */
    private String id;

    /** 内容类型：TEXT（纯文本）/ URL（远程资源）/ FILE（本地文件） */
    private ContentType contentType;

    /** 原始内容（TEXT 类型直接存储，URL/FILE 类型存储路径） */
    private String content;

    /** 摘要（用于渐进式注入时匹配相关性） */
    private String summary;

    /** 权重（用于排序，渐进式注入时优先选择高权重片段） */
    private double weight;

    public enum ContentType {
        TEXT,
        URL,
        FILE
    }
}
