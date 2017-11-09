package com.example.client;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;

@RestController
@SpringBootApplication(scanBasePackages = "com.example")
public class ClientApplication extends WebSecurityConfigurerAdapter /*implements CommandLineRunner*/ {
    static {
        System.setProperty("javax.net.debug", "ssl");
    }

    @Value("${api.server.ping}")
    private String serverPing;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-password}")
    private String keyPassword;

    @Value("${server.ssl.trust-store-password}")
    private String trustStorePassword;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RestTemplate restTemplate;

//    @Override
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
        ResponseEntity<String> res = restTemplate.getForEntity(this.serverPing, String.class);
        return ResponseEntity.ok(String.format("Response from Server: %s", res.getBody()));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll();
    }

    @Bean
    public RestTemplate restTemplateProvider() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(resourceLoader.getResource("classpath:client-keystore.jks").getInputStream(), keyStorePassword.toCharArray());
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(resourceLoader.getResource("classpath:client-truststore.jks").getInputStream(), trustStorePassword.toCharArray());

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                new SSLContextBuilder()
                        .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                        .loadKeyMaterial(keyStore, keyPassword.toCharArray()).build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
    }
}
