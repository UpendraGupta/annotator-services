package com.example.rmsservices.htmlannotator.service.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.rmsservices.htmlannotator.service.DTO.UploadDocument;



@RestController
public class htmlAnnotatorController {
	
    @Value("${main.filepath}")
    private String mainHTMLFiles;
    
    @Value("${annotated.filepath}")
    private String annotatedHTMLFiles;
    
    @Value("${csv.filepath}")
    private String csvForHTMLFiles;
    
	@GetMapping("/list")
    public List<String> getFileNames() 
    {
	    ArrayList<String> arrayList = new ArrayList<String>();
	    
	    final File folder = new File(annotatedHTMLFiles);
	    for (final File fileEntry : folder.listFiles()) {
	        arrayList.add(fileEntry.getName());
            System.out.println(fileEntry.getName());
	       
	    }
		return arrayList;
    }
	
	@PostMapping("/upload")
    public HttpStatus getEmployees(List<UploadDocument> documents) throws IOException 
    {
	    try {
	        final File folder = new File(annotatedHTMLFiles);
	        Integer totalDocuments = folder.listFiles().length;
	        for(UploadDocument document:documents) {
	            String fileContent = document.getHtmlContent();
	            String fileName = document.getFilename(); 
	            
	            BufferedWriter mainFileWriter = new BufferedWriter(new FileWriter(mainHTMLFiles + totalDocuments + "_" + fileName));
	            BufferedWriter annotatedFilewriter = new BufferedWriter(new FileWriter(mainHTMLFiles + totalDocuments + "_" + fileName));
	            mainFileWriter.write(fileContent);
	            annotatedFilewriter.write(fileContent);
	            annotatedFilewriter.close();
	            mainFileWriter.close();
	            ++totalDocuments;
	        }
	        
	        return HttpStatus.CREATED;
        } catch (Exception e) {
            return HttpStatus.BAD_REQUEST;
        }
    }
	
	
	@RequestMapping(value = "/zip", produces="application/zip", path="/download-csv")
    public void downloadCSV(HttpServletResponse response) 
    {
	    List<String> fileNames = new ArrayList<String>();
        
        final File folder = new File(annotatedHTMLFiles);
        for (final File fileEntry : folder.listFiles()) {
            fileNames.add(fileEntry.getName());
            System.out.println(fileEntry.getName());
           
        }
        
        
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=download.zip");
        response.setStatus(HttpServletResponse.SC_OK);

        System.out.println("############# file size ###########" + fileNames.size());

        try (ZipOutputStream zippedOut = new ZipOutputStream(response.getOutputStream())) {
            for (String file : fileNames) {
                FileSystemResource resource = new FileSystemResource(file);

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

}

