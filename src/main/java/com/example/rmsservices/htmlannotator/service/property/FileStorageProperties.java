package com.example.rmsservices.htmlannotator.service.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {
    private String uploadDir;
    private String annotateUploadDir;
    private String jsonUploadDir;
    private String csvUploadDir;

    public String getAnnotateUploadDir() {
        return annotateUploadDir;
    }

    public void setAnnotateUploadDir(String annotateUploadDir) {
        this.annotateUploadDir = annotateUploadDir;
    }

    public String getJsonUploadDir() {
        return jsonUploadDir;
    }

    public void setJsonUploadDir(String jsonUploadDir) {
        this.jsonUploadDir = jsonUploadDir;
    }

    public String getCsvUploadDir() {
        return csvUploadDir;
    }

    public void setCsvUploadDir(String csvUploadDir) {
        this.csvUploadDir = csvUploadDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
