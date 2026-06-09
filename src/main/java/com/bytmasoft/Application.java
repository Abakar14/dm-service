package com.bytmasoft;


import com.bytmasoft.dm.config.DmSecurityProperties;
import com.bytmasoft.dm.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableDiscoveryClient
@EnableConfigurationProperties({StorageProperties.class, DmSecurityProperties.class
})
@EnableJpaAuditing  //for automatic time update create
@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
