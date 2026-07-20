package com.syty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@MapperScan({"com.syty.mapper"})
@EnableAsync
public class SytyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SytyApplication.class, args);
    }
}
