package com.barbearia.saas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Propriedades tipadas para armazenamento de arquivos (local em disco ou S3/MinIO). */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    /** Provedor de armazenamento: "local" ou "s3". */
    private String provider = "local";
    private String dir = "uploads";
    private String s3Endpoint = "";
    private String s3Region = "us-east-1";
    private String s3Bucket = "";
    private String s3AccessKey = "";
    private String s3SecretKey = "";
    private String s3PublicBaseUrl = "";

    public boolean isS3() {
        return "s3".equalsIgnoreCase(provider);
    }
}
