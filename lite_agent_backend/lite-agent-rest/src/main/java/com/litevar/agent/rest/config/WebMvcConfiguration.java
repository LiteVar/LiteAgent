package com.litevar.agent.rest.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.litevar.agent.auth.filter.TokenFilter;
import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author uncle
 * @since 2024/7/3 17:00
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("*"));
        corsConfig.setAllowedMethods(List.of("POST", "GET", "PUT", "DELETE"));
        corsConfig.setAllowCredentials(false);
        corsConfig.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        registrationBean.setFilter(new CorsFilter(source));
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<TokenFilter> loggingFilter(ApplicationContext applicationContext) {
        FilterRegistrationBean<TokenFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TokenFilter(applicationContext));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    /**
     * json自定义序列化工具
     * Long转String，防止精度丢失
     * LocalDateTime 时间格式化
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()))
                .simpleDateFormat(dateTimeFormat)
                .serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)))
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
    }

    /**
     * 用于处理spring mvc异步请求的线程池
     * 默认使用SimpleAsyncTaskExecutor
     */
    @Bean(name = "asyncTaskExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        //核心线程数
        executor.setCorePoolSize(20);
        //最大线程数
        executor.setMaxPoolSize(100);
        //等待队列大小
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("asyncExecutor-");
        //线程空闲存活时间
        executor.setKeepAliveSeconds(60);

        //允许回收核心线程
        executor.setAllowCoreThreadTimeOut(true);
        //拒绝策略:CallerRunsPolicy在调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //线程池中任务等待时间,超过等待时间直接销毁
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                                .responseTimeout(Duration.ofSeconds(120))
                ))
                .build();
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncTaskExecutor());
        configurer.setDefaultTimeout(30000);
    }
}
