-- ============================================================
-- 校园文创平台 数据库初始化脚本
-- 对应开发文档 4.2 建表 DDL（含修正与演示数据）
-- 执行方式: mysql -uroot -p < docs/db/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `campus_cultural`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `campus_cultural`;

-- ===================== 用户表 =====================
CREATE TABLE IF NOT EXISTS `user` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `phone`       VARCHAR(11)     NOT NULL COMMENT '手机号',
  `nick_name`   VARCHAR(64)     DEFAULT '' COMMENT '昵称',
  `icon`        VARCHAR(255)    DEFAULT '' COMMENT '头像URL',
  `role`        TINYINT         NOT NULL DEFAULT 0 COMMENT '角色 0普通 1管理员',
  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ===================== 商品分类 =====================
CREATE TABLE IF NOT EXISTS `category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64) NOT NULL COMMENT '分类名称',
  `sort`        INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ===================== 文创商品表 =====================
CREATE TABLE IF NOT EXISTS `product` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `category_id`    BIGINT          NOT NULL COMMENT '分类ID',
  `name`           VARCHAR(128)    NOT NULL COMMENT '商品名称',
  `sub_title`      VARCHAR(255)    DEFAULT '' COMMENT '卖点副标题',
  `cover`          VARCHAR(255)    DEFAULT '' COMMENT '封面图',
  `images`         VARCHAR(1024)   DEFAULT '' COMMENT '轮播图，逗号分隔',
  `detail`         TEXT            COMMENT '详情富文本',
  `price`          DECIMAL(10,2)   NOT NULL COMMENT '现价',
  `original_price` DECIMAL(10,2)   DEFAULT NULL COMMENT '原价',
  `stock`          INT             NOT NULL DEFAULT 0 COMMENT '普通库存',
  `sales`          INT             NOT NULL DEFAULT 0 COMMENT '销量',
  `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文创商品表';

-- ===================== 秒杀商品表 =====================
CREATE TABLE IF NOT EXISTS `seckill_product` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '秒杀活动ID',
  `product_id`    BIGINT       NOT NULL COMMENT '关联商品ID',
  `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `stock`         INT          NOT NULL COMMENT '秒杀库存',
  `begin_time`    DATETIME     NOT NULL COMMENT '活动开始时间',
  `end_time`      DATETIME     NOT NULL COMMENT '活动结束时间',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_time` (`begin_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品表';

-- ===================== 订单表（秒杀单/普通单共用）=====================
CREATE TABLE IF NOT EXISTS `orders` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no`     VARCHAR(32)     NOT NULL COMMENT '订单号',
  `user_id`      BIGINT          NOT NULL COMMENT '下单用户',
  `product_id`   BIGINT          NOT NULL COMMENT '商品ID',
  `seckill_flag` TINYINT         NOT NULL DEFAULT 0 COMMENT '1秒杀单 0普通单',
  `seckill_id`   BIGINT          DEFAULT NULL COMMENT '秒杀活动ID',
  `count`        INT             NOT NULL DEFAULT 1 COMMENT '数量',
  `amount`       DECIMAL(10,2)   NOT NULL COMMENT '实付金额',
  `pay_type`     TINYINT         DEFAULT NULL COMMENT '支付方式 1模拟支付',
  `pay_time`     DATETIME        DEFAULT NULL COMMENT '支付时间',
  `status`       TINYINT         NOT NULL DEFAULT 0 COMMENT '状态机：0待支付 1已支付 2已发货 3已完成 4已取消 5已关闭',
  `version`      INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  -- 文档 6.1 第三层保险：秒杀单一user一activity唯一；普通单 seckill_id 为 NULL 不受影响
  UNIQUE KEY `uk_user_seckill` (`user_id`, `seckill_id`),
  KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ===================== 购物车 =====================
CREATE TABLE IF NOT EXISTS `cart` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL,
  `product_id`  BIGINT      NOT NULL,
  `count`       INT         NOT NULL DEFAULT 1,
  `checked`     TINYINT     NOT NULL DEFAULT 1 COMMENT '1选中 0未选中',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ===================== 收藏 =====================
CREATE TABLE IF NOT EXISTS `favorite` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      NOT NULL,
  `product_id`  BIGINT      NOT NULL,
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ===================== 文创预约 =====================
CREATE TABLE IF NOT EXISTS `reservation` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '预约用户',
  `product_id`  BIGINT       NOT NULL COMMENT '预约商品',
  `phone`       VARCHAR(11)  DEFAULT '' COMMENT '预留联系方式',
  `progress`    TINYINT      NOT NULL DEFAULT 0 COMMENT '进度 0排队中 1制作中 2可领取 3已完成',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '0有效 1已取消',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- ============================================================
-- 演示数据（冒烟测试用）
-- ============================================================

INSERT INTO `category` (`id`, `name`, `sort`) VALUES
  (1, '徽章徽牌', 1),
  (2, '服饰穿搭', 2),
  (3, '文具书签', 3),
  (4, '明信片贺卡', 4),
  (5, '生活周边', 5);

INSERT INTO `product` (`id`, `category_id`, `name`, `sub_title`, `cover`, `price`, `original_price`, `stock`, `sales`, `status`) VALUES
  (1,  1, '校徽金属徽章',     '百年校庆限定款，锌合金电镀',      '/img/p1.jpg', 19.90,  39.90, 500, 2000, 1),
  (2,  2, '校庆纪念T恤',      '纯棉面料，正面校徽印花',          '/img/p2.jpg', 79.00,  99.00, 300, 1200, 1),
  (3,  5, '校园文创帆布袋',   '加厚帆布，可装笔记本电脑',        '/img/p3.jpg', 29.90,  49.90, 800, 3500, 1),
  (4,  4, '校园风景明信片',   '12张套装，手绘水彩风格',          '/img/p4.jpg', 15.00,  25.00, 1000, 5000, 1),
  (5,  3, '校训黄铜书签',     '镂空雕刻，附赠流苏',              '/img/p5.jpg', 12.90,  19.90, 1500, 800,  1),
  (6,  3, '校园手账笔记本',   'A5空白页，附校园地图夹页',        '/img/p6.jpg', 25.00,  35.00, 600, 600,  1),
  (7,  1, '校徽金属钥匙扣',   '旋转钥匙扣，双面浮雕',            '/img/p7.jpg', 9.90,   15.90, 2000, 4000, 1),
  (8,  5, '校园地标亚克力摆件','图书馆造型，夜光底座',            '/img/p8.jpg', 49.00,  69.00, 200, 300,  1);

-- 秒杀活动：用 NOW() 相对时间，保证任何时刻导入都处于“进行中”(status=1)
INSERT INTO `seckill_product` (`product_id`, `seckill_price`, `stock`, `begin_time`, `end_time`, `status`) VALUES
  (1, 9.90, 100, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), 1),
  (3, 19.90, 50,  DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 3 DAY), 1);
