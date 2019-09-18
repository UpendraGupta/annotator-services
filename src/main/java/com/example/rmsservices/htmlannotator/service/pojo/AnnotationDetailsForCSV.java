package com.example.rmsservices.htmlannotator.service.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
public class AnnotationDetailsForCSV {
    
        private String document;
        
        private String position;
        
        private String value;
        
        private String type;
        
        private String user;

        public AnnotationDetailsForCSV(String document, String position, String value, String type,
                        String user) {
            super();
            this.document = document;
            this.position = position;
            this.value = value;
            this.type = type;
            this.user = user;
        }
                
}

