package dev.modsweb;

import dev.modsweb.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.File;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class ModsWebApplication {

    public static void main(String[] args) {
        // SQLite 需要 data/ 目录存在才会创建数据库文件；uploads/ 存模组文件
        new File("data").mkdirs();
        new File("uploads").mkdirs();
        SpringApplication.run(ModsWebApplication.class, args);
    }
}
