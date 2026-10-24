package jp.safeguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Bean 名は "localeResolver" でなければ DispatcherServlet に使われない。 */
    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        return new LangLocaleResolver();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Service Worker は常に最新を取りに行かせる（キャッシュ更新が止まる事故を防ぐ）
        registry.addResourceHandler("/sw.js")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
        // 静的ファイルは1日キャッシュ（更新時は Service Worker のバージョンを上げる）
        for (String dir : new String[] {"css", "js", "img", "vendor", "video", "data"}) {
            registry.addResourceHandler("/" + dir + "/**")
                    .addResourceLocations("classpath:/static/" + dir + "/")
                    .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
        }
    }
}
