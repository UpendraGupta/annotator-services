package com.example.rmsservices.htmlannotator.service.mapper;


import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsForCSV;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsFromJSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DtoMapper {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ObjectMapper objectMapper;

    

    private static final Logger LOG = LoggerFactory.getLogger(DtoMapper.class);


    
    public AnnotationDetailsForCSV getAnnotationDetailsForCSV(AnnotationDetailsFromJSON annotationDetailsFromJSON, String fileName, String user, Integer diffStart, Integer diffEnd) throws Exception {

        final AnnotationDetailsForCSV annotationDetailsForCSV = modelMapper.map(annotationDetailsFromJSON, AnnotationDetailsForCSV.class);
        annotationDetailsForCSV.setPosition((annotationDetailsFromJSON.getStart() - diffStart) + "-" + (annotationDetailsFromJSON.getEnd() - diffEnd));
        annotationDetailsForCSV.setDocument(fileName);
        annotationDetailsForCSV.setUser(user);
        return annotationDetailsForCSV;
    }
}
