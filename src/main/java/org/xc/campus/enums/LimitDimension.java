package org.xc.campus.enums;

/**
 * 限流维度（对应文档 6.5 多维限流）：
 * USER —— 防单一账号刷接口
 * IP   —— 防爬虫/脚本（游客场景唯一可用维度）
 * API  —— 接口总闸，保护下游（如 MySQL）
 */
public enum LimitDimension {
    USER,
    IP,
    API
}
