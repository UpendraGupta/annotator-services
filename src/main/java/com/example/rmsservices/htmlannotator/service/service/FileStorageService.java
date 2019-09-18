package com.example.rmsservices.htmlannotator.service.service;

import com.example.rmsservices.htmlannotator.service.exception.FileStorageException;
import com.example.rmsservices.htmlannotator.service.exception.MyFileNotFoundException;
import com.example.rmsservices.htmlannotator.service.property.FileStorageProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    public final Path fileStorageLocation;
    public final Path annotatedFileStorageLocation;
    public final Path jsonFileStorageLocation;
    public final Path csvFileStorageLocation;
    
    public static final String TYPE_MAIN_FILE = "mainHTML";
    public static final String TYPE_ANNOTATED_FILE = "annotatedHTML";
    public static final String TYPE_JSON_FILE = "json";
    public static final String TYPE_CSV_FILE = "csv";

    @Autowired
    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
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
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file, String type) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = null;
            ArrayList<String> fileNames = null;
            switch(type) {
                case TYPE_MAIN_FILE : 
                    fileNames = getList();
                    targetLocation = this.fileStorageLocation.resolve(fileNames.size() + "_" + fileName).normalize();
                    break;
                case TYPE_ANNOTATED_FILE :
                    fileNames = getList();
                    targetLocation = this.annotatedFileStorageLocation.resolve(fileNames.size() + "_" + fileName).normalize();
                    break;
                case TYPE_JSON_FILE :
                    targetLocation = this.jsonFileStorageLocation.resolve(fileName);
                    break;
                case TYPE_CSV_FILE :
                    targetLocation = this.csvFileStorageLocation.resolve(fileName);
                    break;
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName, String type) {
        try {
            //Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Path filePath = null;
            ArrayList<String> fileNames = null;
            switch(type) {
                case TYPE_MAIN_FILE : 
                    
                    filePath = this.fileStorageLocation.resolve(fileName);
                    break;
                case TYPE_ANNOTATED_FILE :
                    filePath = this.annotatedFileStorageLocation.resolve(fileName);
                    break;
                case TYPE_JSON_FILE :
                    filePath = this.jsonFileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_CSV_FILE :
                    filePath = this.csvFileStorageLocation.resolve(fileName).normalize();
                    break;
            }
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
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

    public void generateCSV(String annotatedFileName, String jsonFileName) throws IOException {
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
          String csvData = convertJSONToCSV(jsonFileName);
          
          String mainData = cleanAnnotatedData(annotatedData, "");
        
    }

    private String cleanAnnotatedData(String annotatedData, String string) {
        // TODO Auto-generated method stub
        return null;
    }

    private String convertJSONToCSV(String jsonFileName) throws JsonProcessingException, IOException {
        JsonNode jsonTree = new ObjectMapper().readTree(new File((this.jsonFileStorageLocation.resolve(jsonFileName)).toString()));
        return null;
    }
    
    public String replaceWithPattern(String str, String replace){
        Pattern ptn = Pattern.compile("\\s+");
        Matcher mtch = ptn.matcher(str);
        return mtch.replaceAll(replace);
    }
}
