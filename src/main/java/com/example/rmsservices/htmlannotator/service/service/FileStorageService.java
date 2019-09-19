package com.example.rmsservices.htmlannotator.service.service;

import com.example.rmsservices.htmlannotator.service.exception.FileStorageException;
import com.example.rmsservices.htmlannotator.service.exception.MyFileNotFoundException;
import com.example.rmsservices.htmlannotator.service.mapper.DtoMapper;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsForCSV;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsFromJSON;
import com.example.rmsservices.htmlannotator.service.property.FileStorageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;



@Service
public class FileStorageService {
    
    @Autowired
    private DtoMapper dtoMapper;


    public final Path fileStorageLocation;
    public final Path annotatedFileStorageLocation;
    public final Path jsonFileStorageLocation;
    public final Path csvFileStorageLocation;

    public static final String TYPE_MAIN_FILE = "mainHTML";
    public static final String TYPE_ANNOTATED_FILE = "annotatedHTML";
    public static final String TYPE_JSON_FILE = "json";
    public static final String TYPE_CSV_FILE = "csv";
    private static final String CSV_SEPARATOR = ",";

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath()
                        .normalize();
        this.annotatedFileStorageLocation = Paths.get(fileStorageProperties.getAnnotateUploadDir())
                        .toAbsolutePath().normalize();
        this.jsonFileStorageLocation = Paths.get(fileStorageProperties.getJsonUploadDir())
                        .toAbsolutePath().normalize();
        this.csvFileStorageLocation = Paths.get(fileStorageProperties.getCsvUploadDir())
                        .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.annotatedFileStorageLocation);
            Files.createDirectories(this.jsonFileStorageLocation);
            Files.createDirectories(this.csvFileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException(
                            "Could not create the directory where the uploaded files will be stored.",
                            ex);
        }
    }

    public String storeFile(MultipartFile file, String type) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException(
                                "Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = null;
            ArrayList<String> fileNames = null;
            switch (type) {
                case TYPE_MAIN_FILE:
                    fileNames = getList();
                    targetLocation = this.fileStorageLocation
                                    .resolve(fileNames.size() + "_" + fileName).normalize();
                    break;
                case TYPE_ANNOTATED_FILE:
                    fileNames = getList();
                    targetLocation = this.annotatedFileStorageLocation
                                    .resolve(fileNames.size() + "_" + fileName).normalize();
                    break;
                case TYPE_JSON_FILE:
                    targetLocation = this.jsonFileStorageLocation.resolve(fileName);
                    break;
                case TYPE_CSV_FILE:
                    targetLocation = this.csvFileStorageLocation.resolve(fileName);
                    break;
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException(
                            "Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName, String type) {
        try {
            // Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Path filePath = null;
            ArrayList<String> fileNames = null;
            switch (type) {
                case TYPE_MAIN_FILE:

                    filePath = this.fileStorageLocation.resolve(fileName);
                    break;
                case TYPE_ANNOTATED_FILE:
                    filePath = this.annotatedFileStorageLocation.resolve(fileName);
                    break;
                case TYPE_JSON_FILE:
                    filePath = this.jsonFileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_CSV_FILE:
                    filePath = this.csvFileStorageLocation.resolve(fileName).normalize();
                    break;
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }

    public ArrayList<String> getList() {
        ArrayList<String> fileNames = new ArrayList<String>();

        final File folder = new File(annotatedFileStorageLocation.toString());
        for (final File fileEntry : folder.listFiles()) {
            fileNames.add(fileEntry.getName());
            System.out.println(fileEntry.getName());

        }
        return fileNames;
    }

    public void generateCSV(String annotatedFileName, String jsonFileName, String regExpToBeRemoved) throws Exception {
        Path annotatedFilePath = this.annotatedFileStorageLocation.resolve(annotatedFileName);
        Path jsonFilePath = this.jsonFileStorageLocation.resolve(jsonFileName);

        Stream<String> annotatedlines = Files.lines(annotatedFilePath);
        String annotatedData = annotatedlines.collect(Collectors.joining("\n"));
        annotatedlines.close();
        annotatedData = annotatedData.trim();

        Stream<String> jsonlines = Files.lines(jsonFilePath);
        String jsonData = jsonlines.collect(Collectors.joining("\n"));
        jsonlines.close();
        jsonData = jsonData.trim();
        //String csvData = convertJSONToCSV(jsonFileName);
        Map<String, AnnotationDetailsFromJSON> annotationDetails = DtoMapper.getAnnotationDetailsMapFromJSON(jsonFilePath.toString());
        
        updateAnnotationDetailsForCSV(annotatedData, new ArrayList<AnnotationDetailsFromJSON>(annotationDetails.values()), regExpToBeRemoved, replaceWithPattern(annotatedFileName, ".html", ""));
        //String mainData = cleanAnnotatedData(annotatedData, "");

    }

    private void updateAnnotationDetailsForCSV(String annotatedData,
                    ArrayList<AnnotationDetailsFromJSON> annotationDetails, String regExpToBeRemoved, String fileName) throws Exception {
        Pattern pattern = Pattern.compile(regExpToBeRemoved);
        Matcher matcher = null;
        int count = 0;
        Integer diffStart = 0;
        Integer diffEnd = 0;
        ArrayList<AnnotationDetailsForCSV> annotationDetailsForCSVs = new ArrayList<>();
        for(AnnotationDetailsFromJSON annotationDetail: annotationDetails) {
            String expectedValue = annotationDetail.getValue();
            //String[] positions = annotationDetail.getPosition().split("-");
            Integer startIndex = annotationDetail.getStart();
            Integer endIndex = annotationDetail.getEnd();
            String actualValue = annotatedData.substring(startIndex, endIndex+1);
            
            if(expectedValue.equals(actualValue)) {
                String subAnnotatedData = annotatedData.substring(0, startIndex);
                count = 0;                
                matcher = pattern.matcher(subAnnotatedData);
                ArrayList<String> matches = new ArrayList<>();
                while (matcher.find()) {
                    matches.add(matcher.group(0));
                }
                diffStart = String.join("", matches).length();
                
                matcher = pattern.matcher(expectedValue);
                while (matcher.find()) {
                    matches.add(matcher.group(0));
                }
                diffEnd = String.join("", matches).length();
                annotationDetailsForCSVs.add(dtoMapper.getAnnotationDetailsForCSV(annotationDetail, fileName, "User_1", diffStart, diffEnd));
                
                
            } else {
                String msg = annotationDetail.getValue() + " is not found at : " + startIndex + "-" + endIndex + ",  and the found value is : " + actualValue;
                throw new Exception(msg);
            }
            
        }
        matcher = pattern.matcher(annotatedData);
        String mainHTML = matcher.replaceAll(regExpToBeRemoved);
        writeDataToFile(mainHTML, this.fileStorageLocation.resolve(fileName).toString());
        writeToCSV(annotationDetailsForCSVs, this.csvFileStorageLocation.resolve(fileName).toString());
    }
    
    
    private static Boolean writeToCSV(ArrayList<AnnotationDetailsForCSV> details, String csvFileName)
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFileName), "UTF-8"));
            for (AnnotationDetailsForCSV detail : details)
            {
                StringBuffer oneLine = new StringBuffer();
                oneLine.append(detail.getDocument());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getPosition());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getValue());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getType());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getUser());
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public void writeDataToFile(String data, String fileName) throws IOException {
      
      FileOutputStream outputStream = new FileOutputStream(fileName);
      byte[] strToBytes = data.getBytes();
      outputStream.write(strToBytes);
      outputStream.close();
   }

    private String cleanAnnotatedData(String annotatedData, String string) {
        // TODO Auto-generated method stub
        return null;
    }

    

   
//    private Boolean convertJSONToCSV(String jsonFileName) throws JsonProcessingException, IOException {
//        Path jsonFilePath = this.jsonFileStorageLocation.resolve(jsonFileName);
//        Stream<String> jsonlines = Files.lines(jsonFilePath);
//        String jsonData = jsonlines.collect(Collectors.joining("\n"));
//        jsonlines.close();
//        jsonData = jsonData.trim();
//        ObjectMapper mapper = new ObjectMapper();
////        ArrayList<AnnotationDetailsForCSV> emp = mapper.readValue(jsonData, ArrayList<AnnotationDetailsForCSV>.class);
////        mapper.re
//        try{
//            JsonNode jsonTree = new ObjectMapper().readTree(new File((this.jsonFileStorageLocation.resolve(jsonFileName)).toString()));
//        
//            Builder csvSchemaBuilder = CsvSchema.builder();
//            JsonNode firstObject = jsonTree.elements().next();
//            firstObject.fieldNames().forEachRemaining(fieldName -> {csvSchemaBuilder.addColumn(fieldName);} );
//            //CsvSchema csvSchema = csvSchemaBuilder.build().withHeader();
//            CsvMapper csvMapper = new CsvMapper();
//            CsvSchema csvSchema = csvMapper
//                            .schemaFor(AnnotationDetailsFromJSON.class)
//                            .withHeader(); 
//            String csvFileName = replaceWithPattern(jsonFileName, ".json", ".csv");
//            csvMapper.writerFor(JsonNode.class)
//              .with(csvSchema)
//              .writeValue(new File((this.csvFileStorageLocation.resolve(csvFileName)).toString()), jsonTree);
//            
//            Stream<String> csvLines = Files.lines(this.csvFileStorageLocation.resolve(csvFileName));
//            ArrayList<String> csvArrs = (ArrayList<String>) csvLines.collect(Collectors.toList());
//            for (String csvRow: csvArrs) {
//                
//                
//            }
//            return true;
//            
//        } catch(Exception exception) {
//            return false;
//        }
//    }

    public String replaceWithPattern(String str, String regExp, String replace) {
        Pattern ptn = Pattern.compile(regExp);// "\\s+");
        Matcher mtch = ptn.matcher(str);
        return mtch.replaceAll(replace);
    }
}
