package com.example.rmsservices.htmlannotator.service.mapper;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsForCSV;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsFromJSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DtoMapper {

    @Autowired
    private ModelMapper modelMapper;
    
    @Autowired
    private static ObjectMapper objectMapper;

    
    private static final Logger logger = LoggerFactory.getLogger(DtoMapper.class);
    
    public AnnotationDetailsForCSV getAnnotationDetailsForCSV(AnnotationDetailsFromJSON annotationDetailsFromJSON, String fileName, String user, Integer diffStart, Integer diffEnd) {

        final AnnotationDetailsForCSV annotationDetailsForCSV = modelMapper.map(annotationDetailsFromJSON, AnnotationDetailsForCSV.class);
        annotationDetailsForCSV.setPosition((annotationDetailsFromJSON.getStart() - diffStart) + "-" + (annotationDetailsFromJSON.getEnd() - diffEnd));
        annotationDetailsForCSV.setDocument(fileName);
        annotationDetailsForCSV.setUser(user);
        return annotationDetailsForCSV;
    }
    
    public static Map<String, AnnotationDetailsFromJSON> getAnnotationDetailsMapFromJSON(String jsonFilePath) {
        
        try {
            
            return objectMapper.readValue(new File(
                            jsonFilePath), new TypeReference<Map<String, AnnotationDetailsFromJSON>>() {
                    });
         
            
        } catch (IOException ex) {
            logger.error("Error occurred in getAnnotationDetailsMapFromJSON at " + jsonFilePath, ex);
        }
        return null;
    }
    public static <E, T> Map<E, T> getMapFromJsonPath(String jsonPath,
                    TypeReference<Map<E, T>> mapType) throws IOException {

        byte[] jsonData = Files.readAllBytes(Paths.get(jsonPath));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Map<E, T> map = objectMapper.readValue(jsonData, mapType);
        return map;
    }
}
