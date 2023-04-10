package org.gjw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableWebSocket
@SpringBootApplication
@MapperScan("org.gjw.mvc.mapper")
public class BootVedioApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootVedioApplication.class, args);
    }

}
