package org.fuse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class Project {
    private String githubOrganization;
    private String githubProject;
    private String branch;
    private ArrayList<String> pomFiles;
    private Hashtable<String,Properties> properties;

    private Git git = null;

    private static PasswordAuthentication auth;

    public Project(String githubOrganization, String githubProject, String branch, 
        PasswordAuthentication auth) {
        this.githubOrganization = githubOrganization;
        this.githubProject = githubProject;
        this.branch = branch;
        this.auth = auth;
        properties = new Hashtable<String,Properties>();
    }    

    /**
     * @return String return the githubOrganization
     */
    public String getGithubOrganization() {
        return githubOrganization;
    }

    /**
     * @param githubOrganization the githubOrganization to set
     */
    public void setGithubOrganization(String githubOrganization) {
        this.githubOrganization = githubOrganization;
    }

    /**
     * @return String return the projectName
     */
    public String getGithubProject() {
        return githubProject;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setGithubProject(String githubProject) {
        this.githubProject = githubProject;
    }

    /**
     * @return String return the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @param branch the branch to set
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * @return ArrayList return the pomFiles
     */
    public ArrayList getPomFiles() {
        return pomFiles;
    }

    /**
     * @param pomFiles the pomFiles to set
     */
    public void setPomFiles(ArrayList pomFiles) {
        this.pomFiles = pomFiles;
    }

    public Collection<File> findPomFiles(File directory) {
        return FileUtils.listFiles(directory,
                FileFilterUtils.nameFileFilter("pom.xml"), TrueFileFilter.INSTANCE);
    }

    public void cloneProject(String path) throws GitAPIException {
        String repoUrl = "https://github.com/" + githubOrganization + "/" + githubProject + ".git";
        File localPath = new File(path);
           
        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(auth.getUserName(), 
            new String(auth.getPassword()));

        System.out.println("Cloning " + githubProject + " into " + path);
        git = Git.cloneRepository()
            .setURI(repoUrl)
            .setCredentialsProvider(cp)
            .setDirectory(localPath)
            .call();
        System.out.println("Completed Cloning");

        git.fetch().setCredentialsProvider(cp).call();

        if (!branch.equals("master")) {
            git.checkout().setCreateBranch(true)
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                .setStartPoint("origin/" + branch)
                .call();
        }
    }

    public void getProperties(File pomFile) throws Exception {

        org.apache.maven.model.io.xpp3.MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(pomFile));
    
        Properties props = model.getProperties();
        Enumeration enums = props.keys();
        properties.put(pomFile.toString(), props);
        while (enums.hasMoreElements()) { 
            String key = (String) enums.nextElement();
        }       
    }

    public void listProperties() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter("properties.csv", true));
        Enumeration<String> e = properties.keys();
        while (e.hasMoreElements()) {
            String pomFile = (String) e.nextElement();
            Properties props = properties.get(pomFile);
            Enumeration pEnum = props.keys();
            while (pEnum.hasMoreElements()) {
                String key = (String) pEnum.nextElement();
                String value = (String) props.get(key);
                writer.write(
                    githubProject + "," + 
                    branch + "," +
                    pomFile + "," +
                    key.replaceAll(",", "") + "," +
                    value.replaceAll(",", "").replaceAll("\\r|\\n", "") + "\n"
                );
            }
        }
        writer.close();
    }
}