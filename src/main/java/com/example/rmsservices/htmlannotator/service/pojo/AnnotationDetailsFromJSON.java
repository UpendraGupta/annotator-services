package com.example.rmsservices.htmlannotator.service.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
public abstract class AnnotationDetailsFromJSON {
    
        private String document;
        
        private String position;
        
        private String value;
        
        private String type;
        
        private String user;
        
        private Integer start;
        private Integer end;
        
                
}

