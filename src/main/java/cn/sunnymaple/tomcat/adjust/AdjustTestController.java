package cn.sunnymaple.tomcat.adjust;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

/**
 * tomcat调优测试
 * @author wangzb
 * @date 2019/12/28 17:54
 */
@RestController
@RequestMapping
public class AdjustTestController {

    /**
     * 假设该方法需要执行3s
     * 用于测试连接数的控制
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("/testCount")
    public String testCount() throws InterruptedException {
        Thread.sleep(3 * 1000);
        return "success";
    }

    /**
     * 参数调优测试
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("/test")
    public String test() throws InterruptedException {
        System.out.println("访问test：" + Thread.currentThread().getName());
        // 这段代码，一直运算。
        for (int i = 0; i < 200000; i++) {
            new Random().nextInt();
        }
        // 50毫秒的数据库等待，线程不干活
        Thread.sleep(50L);
        return "success";
    }
}
