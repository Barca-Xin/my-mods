package dev.modsweb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Storage storage = new Storage();
    private final Admin admin = new Admin();
    private final Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Storage {
        private String type = "local";
        private String uploadDir = "./uploads";
    }

    @Getter
    @Setter
    public static class Admin {
        private String username;
        private String passwordHash;
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expireHours = 72;
    }
}
