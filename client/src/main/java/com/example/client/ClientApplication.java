package com.example.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashSet;

@RestController
@SpringBootApplication
public class ClientApplication extends WebSecurityConfigurerAdapter implements CommandLineRunner {
    static {
        System.setProperty("javax.net.debug", "ssl");
    }

    @Value("${api.server.ping}")
    private String serverPing;

    @Override
    public void run(String... args) throws Exception {
        if (new HashSet<>(Arrays.asList(args)).contains("-f")) {
            ResponseEntity<String> res = this.pingServer();
            System.out.printf("Response from Server: %s", res.getBody());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @RequestMapping(value = "/server/ping")
    public ResponseEntity<String> pingServer() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> res = restTemplate.getForEntity(this.serverPing, String.class);
        return ResponseEntity.ok(String.format("Response from Server: %s", res.getBody()));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll();
    }
}
