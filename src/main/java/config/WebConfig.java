package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Authinterceptor authinterceptor;

    public WebConfig(Authinterceptor authinterceptor) {
        this.authinterceptor = authinterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authinterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/shop",
                        "/shop/**",
                        "/cart",
                        "/cart/**",
                        "/checkout",
                        "/order-success",
                        "/login",
                        "/logout",
                        "/register",
                        "/customer/login",
                        "/customer/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/img/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error"
                );
    }
}