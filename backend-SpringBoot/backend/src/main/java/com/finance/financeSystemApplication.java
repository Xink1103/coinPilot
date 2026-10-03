package com.finance;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.finance.filter.JwtAuthFilter;

@SpringBootApplication
@EnableScheduling 
public class financeSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(financeSystemApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate(){
		RestTemplate restTemplate = new RestTemplate();
		
		// 設定 JSON Converter 支援 text/html
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Arrays.asList(
			MediaType.APPLICATION_JSON,
			MediaType.APPLICATION_OCTET_STREAM,
			new MediaType("text", "html"),           // 支援 text/html
			new MediaType("text", "plain"),          // 支援 text/plain
			MediaType.ALL
		));
		
		// 移除預設的 converter 加入自訂的
		restTemplate.getMessageConverters().removeIf(
			c -> c instanceof MappingJackson2HttpMessageConverter
		);
		restTemplate.getMessageConverters().add(converter);
		
		// 設定 UTF-8 編碼
		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
		
		return restTemplate;
	}

	public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtAuthFilter jwtAuthFilter){
		FilterRegistrationBean<JwtAuthFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(jwtAuthFilter);
		registrationBean.addUrlPatterns("/api/*");
		registrationBean.setOrder(1);
		return registrationBean;
	}
}