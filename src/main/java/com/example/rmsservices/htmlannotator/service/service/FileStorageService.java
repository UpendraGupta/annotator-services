package com.example.rmsservices.htmlannotator.service.service;


import com.example.rmsservices.htmlannotator.service.exception.FileStorageException;
import com.example.rmsservices.htmlannotator.service.exception.MyFileNotFoundException;
import com.example.rmsservices.htmlannotator.service.mapper.DtoMapper;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsForCSV;
import com.example.rmsservices.htmlannotator.service.pojo.AnnotationDetailsFromJSON;
import com.example.rmsservices.htmlannotator.service.property.FileStorageProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String ANNOTATED_FILE = "annotated_";
    public static final String TYPE_CSV = ".csv";
    public static final String TYPE_JSON = ".json";
    public static final String TYPE_HTML = ".html";

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

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

    public static String decodeFileName(String fileName)  
    {  
              try {  
                   String prevURL="";  
                   String decodeURL=fileName;  
                   while(!prevURL.equals(decodeURL))  
                   {  
                        prevURL=decodeURL;  
                        decodeURL=URLDecoder.decode( decodeURL, "UTF-8" );  
                   }  
                   return decodeURL;  
              } catch (UnsupportedEncodingException e) {  
                   return "Issue while decoding" +e.getMessage();  
              }  
    }
    public String storeFile(MultipartFile file, String type, Boolean isAnnotated) {
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
                    fileNames = (ArrayList<String>) getList();
                    fileName = fileNames.size() + "_" + fileName;
                    targetLocation = this.fileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_ANNOTATED_FILE:
                    fileNames = (ArrayList<String>) getList();
                    if (isAnnotated) {
                        File oldFile = new File(this.annotatedFileStorageLocation.resolve(fileName)
                                        .normalize().toString());

                        if (oldFile.delete()) {
                            logger.info("File : "
                                            + this.annotatedFileStorageLocation.resolve(fileName)
                                                            .normalize().toString()
                                            + " deleted successfully");
                        }
                        if (fileName.indexOf(ANNOTATED_FILE) == -1) {
                            fileName = ANNOTATED_FILE + fileName;
                        }


                    } else {
                        fileName = fileNames.size() + "_" + fileName;

                    }

                    targetLocation = this.annotatedFileStorageLocation.resolve(fileName)
                                    .normalize();
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
            fileName = decodeFileName(fileName);
            // Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Path filePath = null;
            switch (type) {
                case TYPE_MAIN_FILE:

                    filePath = this.fileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_ANNOTATED_FILE:
                    filePath = this.annotatedFileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_JSON_FILE:
                    filePath = this.jsonFileStorageLocation.resolve(fileName).normalize();
                    break;
                case TYPE_CSV_FILE:
                    filePath = this.csvFileStorageLocation.resolve(fileName).normalize();
                    break;
                default:
                    throw new Exception("Type is incorrect!!!");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            // throw new MyFileNotFoundException("File not found " + fileName, ex);
            logger.error("File not found in loadFileAsResource at " + fileName, ex);
        } catch (Exception ex) {
            logger.error("Error occurred in loadFileAsResource at " + fileName, ex);
        }
        return null;
    }

    public List<String> getList() {
        ArrayList<String> fileNames = new ArrayList<>();

        final File folder = new File(annotatedFileStorageLocation.toString());
        File[] files = folder.listFiles();
        Arrays.sort(files, new Comparator<File>(){
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            } });
        for (final File fileEntry : files) {
               fileNames.add(fileEntry.getName());
        }
        logger.info(fileNames.toString());
        return fileNames;
    }

    public void generateCSV(String annotatedFileName, String jsonFileName, String regExpToBeRemoved)
                    throws Exception {
        Path annotatedFilePath = this.annotatedFileStorageLocation.resolve(annotatedFileName);
        Path jsonFilePath = this.jsonFileStorageLocation.resolve(jsonFileName);

        String annotatedData = getDataFromFilePath(annotatedFilePath);

        Map<String, AnnotationDetailsFromJSON> annotationDetails = DtoMapper.getMapFromJsonPath(
                        jsonFilePath.toString(),
                        new TypeReference<Map<String, AnnotationDetailsFromJSON>>() {});
        String fileName = replaceWithPattern(annotatedFileName, FileStorageService.ANNOTATED_FILE,
                        "");
        fileName = replaceWithPattern(fileName, TYPE_HTML, "");
        updateAnnotationDetailsForCSV(annotatedData,
                        new ArrayList<AnnotationDetailsFromJSON>(annotationDetails.values()),
                        regExpToBeRemoved, fileName);


    }

    public String getDataFromFilePath(Path filePath) throws IOException {
        Stream<String> annotatedlines = Files.lines(filePath);
        String annotatedData = annotatedlines.collect(Collectors.joining("\n"));
        annotatedlines.close();

        return annotatedData.trim();
    }

    private void updateAnnotationDetailsForCSV(String annotatedData,
                    ArrayList<AnnotationDetailsFromJSON> annotationDetails,
                    String regExpToBeRemoved, String fileName) throws Exception {
        Pattern pattern = Pattern.compile(regExpToBeRemoved);
        Matcher matcher = null;
        Integer diffStart = 0;
        Integer diffEnd = 0;;
        ArrayList<AnnotationDetailsForCSV> annotationDetailsForCSVs = new ArrayList<>();
        try {
            for (AnnotationDetailsFromJSON annotationDetail : annotationDetails) {
                String expectedValue = annotationDetail.getValue();
                Integer startIndex = annotationDetail.getStart();
                Integer endIndex = annotationDetail.getEnd();
                String actualValue = annotatedData.substring(startIndex, endIndex);

                if (expectedValue.compareTo(actualValue) == 0) {
                    String subAnnotatedData = annotatedData.substring(0, startIndex);
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

                    matcher = pattern.matcher(annotationDetail.getValue());
                    annotationDetail.setValue(matcher.replaceAll(""));
                    annotationDetailsForCSVs.add(dtoMapper.getAnnotationDetailsForCSV(
                                    annotationDetail, fileName, "User_1", diffStart, diffEnd));


                } else {
                    String msg = annotationDetail.getValue() + " is not found at : " + startIndex
                                    + "-" + endIndex + ",  and the found value is : " + actualValue;
                    throw new Exception(msg);

                }

            }
            matcher = pattern.matcher(annotatedData);
            String mainHTML = matcher.replaceAll("");
            String oldData = getDataFromFilePath(
                            this.fileStorageLocation.resolve(fileName + TYPE_HTML));
            checkMd5SumForOldAndNewFile(oldData, mainHTML.trim());
            writeDataToFile(mainHTML,
                            this.fileStorageLocation.resolve(fileName + TYPE_HTML).toString());
            writeToCSV(annotationDetailsForCSVs,
                            this.csvFileStorageLocation.resolve(fileName + TYPE_CSV).toString());
        } catch (Exception ex) {

            logger.error("Error occurred in updateAnnotationDetailsForCSV.", ex);
        }
    }


    private void checkMd5SumForOldAndNewFile(String oldData, String newData) throws Exception {

        String oldMd5Sum = getMd5Sum(oldData);
        String newMd5Sum = getMd5Sum(newData);

        if (!newMd5Sum.equals(oldMd5Sum)) {
            logger.error("OLD Resume Data. : ############################{}############################",
                            oldData);
            logger.error("NEW Resume Data. : ############################{}############################",
                            newData);
            throw new Exception("Old and New Md5Sums are not same. Please have a look");

        }
    }


    private String getMd5Sum(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // digest() method is called to calculate message digest
            // of an input digest() return array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    private static Boolean writeToCSV(ArrayList<AnnotationDetailsForCSV> details,
                    String csvFileName) {
        try (BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(csvFileName), "UTF-8"))) {

            for (AnnotationDetailsForCSV detail : details) {
                StringBuffer oneLine = new StringBuffer();
                oneLine.append(detail.getDocument());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getPosition());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getValue());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getTag());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(detail.getUser());
                bw.write(oneLine.toString());
                bw.newLine();
            }
            bw.flush();
            // bw.close();
        } catch (Exception ex) {
            logger.error("Error occurred in writeToCSV at " + csvFileName, ex);
            return false;
        }
        return true;
    }

    public void writeDataToFile(String data, String fileName) throws IOException {

        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            byte[] strToBytes = data.getBytes();
            outputStream.write(strToBytes);
            // outputStream.close();
        } catch (Exception ex) {
            logger.error("Error occurred in writeDataToFile at " + fileName, ex);
        }

    }


    public String replaceWithPattern(String str, String regExp, String replace) {
        Pattern ptn = Pattern.compile(regExp);// "\\s+");
        Matcher mtch = ptn.matcher(str);
        return mtch.replaceAll(replace);
    }

    public String replaceFirstWithPattern(String str, String regExp, String replace) {
        Pattern ptn = Pattern.compile(regExp);
        Matcher mtch = ptn.matcher(str);
        return mtch.replaceFirst(replace);
    }

    public static String insertString(String originalString, String stringToBeInserted, int index) {

        StringBuffer newString = new StringBuffer(originalString);
        newString.insert(index, stringToBeInserted);
        return newString.toString();
    }

    public String addAttributeToHTMLTags(String data) {
        //<\s*[a-z|A-Z]+\s*
        //<\\s*[a-z|A-Z]+\\s*
        //(<\s*[a-z|A-Z]+\s*)
        Random rand = new Random();
        
        int attributeId = 0;
        int x = 1;
        String patternStr = "(<\\s*[a-zA-Z]+)";
        Pattern pattern = Pattern.compile(patternStr);    
        Matcher matcher = null;    
        String newData = "";
        matcher = pattern.matcher(data);
        while (matcher.find()) {    
            
            //attributeId = (int)rand.nextDouble() * 1000000000; 
            //matcher.group(0);
            attributeId = 1000000000 + x++;
            newData = newData + insertString(data.substring(0, matcher.end()), " data-annotate=\"_" + attributeId + "\"", matcher.end());
            data = data.substring(matcher.end(), data.length());
            matcher = pattern.matcher(data);
        }    
        newData = newData + data; 
        return newData;
    }
}
