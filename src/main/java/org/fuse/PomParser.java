package org.fuse;

import java.io.Reader;
import java.net.PasswordAuthentication;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.maven.model.Model;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/**
 * Parse poms to find all the properties.
 */
public class PomParser 
{
    private ArrayList<Project> projects = null;
    private static String TMPDIRECTORY = "/tmp/clones";

    public PomParser() {
        projects = new ArrayList<Project>();
    }

    public PasswordAuthentication getPasswordAuthentication() {
        java.io.Console console = System.console();
        //String username = console.readLine("Username: ");
        //String password = new String(console.readPassword("Password: "));
        return new PasswordAuthentication(
            console.readLine("Username: "),
            console.readPassword("Password: "));
    }
    
    public void readCSVFile(String csvFile) throws IOException {

        
        PasswordAuthentication auth = getPasswordAuthentication();

        Reader reader = Files.newBufferedReader(Paths.get(csvFile), StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        for (CSVRecord csvRecord : csvParser) {
            System.out.println(csvRecord.get(0) + " " + csvRecord.get(1) + " " + csvRecord.get(2));
            Project proj = new Project(csvRecord.get(0), csvRecord.get(1), csvRecord.get(2), auth);
            projects.add(proj);
        }
    }

    public void findProperties() {
        File propertiesCSV = new File("properties.csv");
        if (propertiesCSV.exists()) {
            propertiesCSV.delete();
        }

        for (Project p : projects) {
            try {
                File directory = new File(TMPDIRECTORY + File.separator + p.getGithubProject());
                if (!directory.exists()) {
                    p.cloneProject(TMPDIRECTORY + File.separator + p.getGithubProject());
                }

                Collection<File> pomFiles = p.findPomFiles(directory);
                for (File pomFile : pomFiles) {
                    p.getProperties(pomFile);
                }
                p.listProperties();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main( String[] args )
    {
        PomParser pp = new PomParser();
        
        // Remove the temp directory
        File index = new File(TMPDIRECTORY);
        if (index.exists()) {
            index.delete();
        }        

        String csvFile = args[0]; 
        try {
            pp.readCSVFile(csvFile);
            pp.findProperties();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
