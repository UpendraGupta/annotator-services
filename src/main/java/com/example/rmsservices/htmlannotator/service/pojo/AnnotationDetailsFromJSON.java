package com.example.rmsservices.htmlannotator.service.pojo;

import org.springframework.boot.configurationprocessor.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnotationDetailsFromJSON {
    
        private Double top;
        private Double left;
        private String value;
        
        private String tag;
        
        private Integer start;
        private Integer end;
        
        private JSONObject clientRects;
        private String text;
        
                
}

