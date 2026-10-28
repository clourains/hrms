package com.hrms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能人事管理系统启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.hrms.mapper")
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
        System.out.println("====================================");
        System.out.println("  智能人事管理系统启动成功！");
        System.out.println("  后端地址: http://localhost:8080/api");
        System.out.println("====================================");
    }
}
