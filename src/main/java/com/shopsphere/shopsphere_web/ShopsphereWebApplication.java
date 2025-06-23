package com.shopsphere.shopsphere_web;

import com.shopsphere.shopsphere_web.config.KakaoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KakaoProperties.class)
public class ShopsphereWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopsphereWebApplication.class, args);
	}

}
