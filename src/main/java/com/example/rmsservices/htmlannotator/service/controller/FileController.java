package com.example.rmsservices.htmlannotator.service.controller;

import com.example.rmsservices.htmlannotator.service.payload.UploadFileResponse;
import com.example.rmsservices.htmlannotator.service.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @CrossOrigin
    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        
        String fileName = fileStorageService.storeFile(file, FileStorageService.TYPE_ANNOTATED_FILE, false);
        fileStorageService.storeFile(file, FileStorageService.TYPE_MAIN_FILE, false);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @CrossOrigin
    @PostMapping("/uploadMultipleFiles")
    public ResponseEntity<List<UploadFileResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return new ResponseEntity<List<UploadFileResponse>>(Arrays.asList(files)
                        .stream()
                        .map(file -> uploadFile(file))
                        .collect(Collectors.toList()), HttpStatus.CREATED);
    }

    @CrossOrigin
    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.ANNOTATED_FILE, "");
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName, FileStorageService.TYPE_MAIN_FILE);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @CrossOrigin
    @GetMapping("/downloadFiles")
    public void downloadFiles(@RequestParam("fileNames") List<String> fileNames, HttpServletResponse response) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=download.zip");
        response.setStatus(HttpServletResponse.SC_OK);
        
        try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
            for(String fileName:fileNames) {
                fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.ANNOTATED_FILE, "");
                FileSystemResource resource = new FileSystemResource(fileStorageService.fileStorageLocation.resolve(fileName));
    
                ZipEntry e = new ZipEntry(resource.getFilename());
                // Configure the zip entry, the properties of the file
                e.setSize(resource.contentLength());
                e.setTime(System.currentTimeMillis());
                // etc.
                zippedOut.putNextEntry(e);
                // And the content of the resource:
                StreamUtils.copy(resource.getInputStream(), zippedOut);
                zippedOut.closeEntry();
    
            }
            zippedOut.finish();
        } catch (Exception e) {
            // Exception handling goes here
        }

    }
    @CrossOrigin
    @GetMapping("/downloadCSV/{fileName:.+}")
    public ResponseEntity<Resource> downloadCSV(@PathVariable String fileName, HttpServletRequest request) {
        fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.ANNOTATED_FILE, "");
        fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.TYPE_HTML,FileStorageService.TYPE_CSV);
        
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName, FileStorageService.TYPE_CSV_FILE);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @CrossOrigin
    @GetMapping("/downloadCSVs")
    public void downloadCSVs(@RequestParam("fileNames") List<String> fileNames, HttpServletResponse response) {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=download.zip");
        response.setStatus(HttpServletResponse.SC_OK);
        
        try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
            for(String fileName:fileNames) {
                fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.ANNOTATED_FILE, "");
                FileSystemResource resource = new FileSystemResource(fileStorageService.csvFileStorageLocation.resolve(fileName));
    
                ZipEntry e = new ZipEntry(resource.getFilename());
                // Configure the zip entry, the properties of the file
                e.setSize(resource.contentLength());
                e.setTime(System.currentTimeMillis());
                // etc.
                zippedOut.putNextEntry(e);
                // And the content of the resource:
                StreamUtils.copy(resource.getInputStream(), zippedOut);
                zippedOut.closeEntry();
    
            }
            zippedOut.finish();
        } catch (Exception e) {
            // Exception handling goes here
        }

    }
    
    @CrossOrigin
    @GetMapping("/list")
    public ResponseEntity<ArrayList<String>> getList() {
        
        try {
            
            ArrayList<String> fileNames = (ArrayList<String>)fileStorageService.getList();
            Collections.sort(fileNames, Collections.reverseOrder());
            return new ResponseEntity<ArrayList<String>>(fileNames, HttpStatus.OK); 
           
        } catch (Exception e) {
            logger.error("No Data Found while fetching list");
        }
        return new ResponseEntity<ArrayList<String>>((ArrayList<String>)Collections.EMPTY_LIST, HttpStatus.OK);

    }
    
    @CrossOrigin
    @GetMapping("/getFile/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName, FileStorageService.TYPE_ANNOTATED_FILE);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @CrossOrigin
    @GetMapping("/getJson/{fileName:.+}")
    public ResponseEntity<Resource> getJSON(@PathVariable String fileName, HttpServletRequest request) {
        fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.ANNOTATED_FILE, "");
        fileName = fileStorageService.replaceWithPattern(fileName, FileStorageService.TYPE_HTML, FileStorageService.TYPE_JSON);
        
        Resource resource = fileStorageService.loadFileAsResource(fileName, FileStorageService.TYPE_JSON_FILE);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);

    }
    
    @CrossOrigin
    @PostMapping("/save")
    public ResponseEntity<UploadFileResponse> save(@RequestParam("file") MultipartFile file, @RequestParam("json") String json, @RequestParam("regexToBeRemoved") String regexToBeRemoved) throws Exception {
        String annotatedFileName = fileStorageService.storeFile(file, FileStorageService.TYPE_ANNOTATED_FILE, true);
        String jsonFileName = fileStorageService.replaceWithPattern(annotatedFileName, FileStorageService.ANNOTATED_FILE, "");
        jsonFileName = fileStorageService.replaceWithPattern(jsonFileName, FileStorageService.TYPE_HTML, "");
        jsonFileName = jsonFileName.concat(FileStorageService.TYPE_JSON);
        
//        if(regexToBeRemoved.isEmpty()) {
//            regexToBeRemoved = "\s*data-annotate=\\\"\d{9}\"";
//        }
//        Gson g = new Gson();
//        Map<String, AnnotationDetailsFromJSON> annotationDetails = g.fromJson(json, Map.class);
        //Player p = g.fromJson(jsonString, Player.class)


        fileStorageService.writeDataToFile(json, fileStorageService.jsonFileStorageLocation.resolve(jsonFileName).toString());
        //String jsonFileName = fileStorageService.storeFile(json, FileStorageService.TYPE_JSON_FILE, false);
        // regex for tag : <\s*tag[^>]*>(.*?)<\s*/\s*tag>
        // regex for annotation attribute : "\\s*data-annotate=\\"_\\d{9}\\""    main   \s*data-annotate=\"_\d{9}\"
        fileStorageService.generateCSV(annotatedFileName, jsonFileName, regexToBeRemoved);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(annotatedFileName)
                .toUriString();
        
        return new ResponseEntity<UploadFileResponse>(new UploadFileResponse(annotatedFileName, fileDownloadUri,
                        file.getContentType(), file.getSize()), HttpStatus.OK);
        
    }

}
