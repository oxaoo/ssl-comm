package com.example.client;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
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
    @Autowired
    private Environment env;


    @Value("${api.server.ping}")
    private String serverPing;

    @Value("${server.ssl.key-store}")
    private Resource keyStore;

    @Value("${server.ssl.key-store-password}")
    private char[] keyStorePassword;

    @Value("${server.ssl.trust-store}")
    private Resource trustStore;

    @Value("${server.ssl.trust-store-password}")
    private char[] trustStorePassword;

//    @Value("${http.client.maxPoolSize}")
    private Integer maxPoolSize = 10;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RestTemplate restTemplate;// = new RestTemplate();

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

//    @PostConstruct
//    public void setUp() {
//        System.setProperty("javax.net.debug", "ssl");
//        System.setProperty("https.protocols", "TLSv1.2");
//        System.setProperty("javax.net.ssl.keyStore", env.getProperty("server.ssl.key-store"));
//        System.setProperty("javax.net.ssl.keyStorePassword", env.getProperty("server.ssl.key-store-password"));
//        System.setProperty("javax.net.ssl.trustStore", env.getProperty("server.ssl.trust-store"));
//        System.setProperty("javax.net.ssl.trustStorePassword", env.getProperty("server.ssl.trust-store-password"));
//    }

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
        keyStore.load(this.keyStore.getInputStream(), keyStorePassword);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(this.trustStore.getInputStream(), trustStorePassword);

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                new SSLContextBuilder()
                        .loadTrustMaterial(trustStore, (x509Certificates, s) -> false)
                        .loadKeyMaterial(keyStore, keyStorePassword).build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(requestFactory);
//        return new RestTemplate();
    }


//    @Bean
//    public ClientHttpRequestFactory httpRequestFactory() {
//        return new HttpComponentsClientHttpRequestFactory(httpClient());
//    }
//
//    @Bean
//    public CloseableHttpClient httpClient() {
//
//        // Trust own CA and all child certs
//        Registry<ConnectionSocketFactory> socketFactoryRegistry = null;
//        try {
//            SSLContext sslContext = SSLContexts
//                    .custom()
//                    .loadTrustMaterial(trustStore.getFile(),
//                            trustStorePassword)
//                    .build();
//
//            // Since only our own certs are trusted, hostname verification is probably safe to bypass
//            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext,
//                    new HostnameVerifier() {
//
//                        @Override
//                        public boolean verify(final String hostname,
//                                              final SSLSession session) {
//                            return true;
//                        }
//                    });
//
//            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
//                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
//                    .register("https", sslSocketFactory)
//                    .build();
//
//        } catch (Exception e) {
//            //TODO: handle exceptions
//            e.printStackTrace();
//        }
//
//        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
//        connectionManager.setMaxTotal(maxPoolSize);
//        // This client is for internal connections so only one route is expected
//        connectionManager.setDefaultMaxPerRoute(maxPoolSize);
//        return HttpClientBuilder.create()
//                .setConnectionManager(connectionManager)
//                .disableCookieManagement()
//                .disableAuthCaching()
//                .build();
//    }
//
//    @Bean
//    public RestTemplate restTemplate() {
//        RestTemplate restTemplate = new RestTemplate();
//        restTemplate.setRequestFactory(httpRequestFactory());
//        return restTemplate;
//    }
}
