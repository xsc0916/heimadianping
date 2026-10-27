-- 秒杀原子扣减脚本（对应文档 6.1）
-- KEYS[1] = seckill:stock:{seckillId}   库存(String)
-- KEYS[2] = seckill:buyers:{seckillId}  已购用户集合(Set)
-- ARGV[1] = userId
-- 返回：0=下单成功(已预扣)  1=重复购买  2=库存不足  3=活动不存在(库存key没预热)
if (redis.call('exists', KEYS[1]) == 1) then
    -- 第一关：一人一单 —— 在买家集合里查这个用户
    if (redis.call('sismember', KEYS[2], ARGV[1]) == 1) then
        return 1
    end
    -- 第二关：库存够不够
    local stock = tonumber(redis.call('get', KEYS[1]))
    if (stock <= 0) then
        return 2
    end
    -- 两关都过：扣库存 + 记入买家集合（同一时刻其他请求不可能插进来）
    redis.call('decrby', KEYS[1], 1)
    redis.call('sadd', KEYS[2], ARGV[1])
    return 0
end
return 3
