package org.xc.campus;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 极简压测工具（学习用，生产/简历场景换 JMeter）。
 * 三个并发知识点：
 *   CountDownLatch 发令枪 —— 保证 200 个线程"同一瞬间"起跑，否则统计的 QPS 不准；
 *   AtomicLong     原子计数 —— 多线程同时 ++ 会丢数，必须用原子类；
 *   线程池         固定 200 个线程模拟 200 个并发用户。
 * 用法：IDEA 右键 Run main；或改下面三个常量做实验。
 */
public class PressTest {

    static String url = "http://localhost:8080/api/product/1";
    static int threads = 200; // 并发线程数
    static int times = 50;    // 每线程请求数（总请求 = threads × times）

    public static void main(String[] args) throws Exception {
        if (args.length >= 1) url = args[0];
        if (args.length >= 2) threads = Integer.parseInt(args[1]);
        if (args.length >= 3) times = Integer.parseInt(args[2]);

        // 预热：先打 1 次，让 Caffeine/Redis 就位，避免冷启动 SQL 污染统计
        httpGet(url);
        Thread.sleep(500);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1); // 发令枪
        CountDownLatch finish = new CountDownLatch(threads);
        AtomicLong ok = new AtomicLong();
        AtomicLong fail = new AtomicLong();
        List<Long> costs = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGun.await(); // 全体就位，等鸣枪
                    for (int j = 0; j < times; j++) {
                        long t0 = System.currentTimeMillis();
                        try {
                            httpGet(url);
                            ok.incrementAndGet();
                        } catch (Exception e) {
                            fail.incrementAndGet();
                        }
                        costs.add(System.currentTimeMillis() - t0);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    finish.countDown(); // 该线程完工
                }
            });
        }

        long begin = System.currentTimeMillis();
        startGun.countDown(); // 鸣枪！全员同时开跑
        finish.await();       // 等最后一名跑完
        long costMs = System.currentTimeMillis() - begin;
        pool.shutdown();

        Collections.sort(costs);
        long p99 = costs.get((int) Math.min(costs.size() * 0.99, costs.size() - 1));
        long sum = 0;
        for (Long c : costs) sum += c;
        long avg = sum / costs.size();
        System.out.println("========== 压测报告 ==========");
        System.out.println("目标 : " + url);
        System.out.println("规模 : " + threads + " 线程 × " + times + " 次 = " + (threads * times) + " 请求");
        System.out.println("成功 : " + ok + "   失败 : " + fail);
        System.out.println("总耗时 : " + costMs + " ms");
        System.out.println("QPS : " + (threads * times * 1000L / Math.max(costMs, 1)));
        System.out.println("平均 : " + avg + " ms | P99 : " + p99 + " ms | 最大 : " + costs.get(costs.size() - 1) + " ms");
    }

    static void httpGet(String u) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(5000);
        try (InputStream in = conn.getInputStream()) {
            in.readAllBytes(); // 必须读完响应体，连接才算完整
        } finally {
            conn.disconnect();
        }
    }
}
