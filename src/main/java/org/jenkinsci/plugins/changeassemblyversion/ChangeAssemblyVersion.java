package org.jenkinsci.plugins.changeassemblyversion;

import java.io.PrintWriter;
import java.io.StringWriter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:leonardo.kobus@hbsis.com.br">Leonardo Kobus</a>
 */
public class ChangeAssemblyVersion extends Builder implements SimpleBuildStep {

    private final String versionPattern;
    private final String assemblyFile;
    private final String regexPattern;
    private final String replacementPattern;
    private final String assemblyTitle;
    private final String assemblyDescription;
    private final String assemblyCompany;
    private final String assemblyProduct;
    private final String assemblyCopyright;
    private final String assemblyTrademark;
    private final String assemblyCulture;

    @DataBoundConstructor
    public ChangeAssemblyVersion(String versionPattern, 
        String assemblyFile, 
        String regexPattern, 
        String replacementPattern, 
        String assemblyTitle,
        String assemblyDescription,
        String assemblyCompany,
        String assemblyProduct,
        String assemblyCopyright,
        String assemblyTrademark,
        String assemblyCulture
        ) {
        this.versionPattern = versionPattern;
        this.assemblyFile = assemblyFile;
        this.regexPattern = regexPattern;
        this.replacementPattern = replacementPattern;
        this.assemblyTitle = assemblyTitle;
        this.assemblyDescription = assemblyDescription;
        this.assemblyCompany = assemblyCompany;
        this.assemblyProduct = assemblyProduct;
        this.assemblyCopyright = assemblyCopyright;
        this.assemblyTrademark = assemblyTrademark;
        this.assemblyCulture = assemblyCulture;       
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public String getVersionPattern() {
        return this.versionPattern;
    }

    public String getAssemblyFile() {
        return this.assemblyFile;
    }

    public String getRegexPattern() {
        return this.regexPattern;
    }

    public String getReplacementPattern() {
        return this.replacementPattern;
    }
    
    public String getAssemblyTitle() {
        return this.assemblyTitle;
    }
    
    public String getAssemblyDescription() {
        return this.assemblyDescription;
    }
    
    public String getAssemblyCompany() {
        return this.assemblyCompany;
    }
    
    public String getAssemblyProduct() {
        return this.assemblyProduct;
    }            

    public String getAssemblyCopyright() {
        return this.assemblyCopyright;
    }

    public String getAssemblyTrademark() {
        return this.assemblyTrademark;
    }
    
    public String getAssemblyCulture() {
        return this.assemblyCulture;
    }
    
    /**
     *
     * The perform method is gonna search all the file named "Assemblyinfo.cs"
     * in any folder below, and after found will change the version of
     * AssemblyVersion and AssemblyFileVersion in the file for the inserted
     * version (versionPattern property value).
     *
     *
     * OBS: The inserted value can be some jenkins variable like ${BUILD_NUMBER}
     * just the variable alone, but not implemented to treat
     * 0.0.${BUILD_NUMBER}.0 I think this plugin must be used with Version
     * Number Plugin.
     *
     *
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            String assemblyGlob = this.assemblyFile == null || this.assemblyFile.equals("") ? "**/AssemblyInfo.cs" : this.assemblyFile;

            EnvVars envVars = build.getEnvironment(listener);
            String version = new AssemblyVersion(this.versionPattern, envVars).getVersion();
            if (versionPattern == null || StringUtils.isEmpty(versionPattern))
            {
                listener.getLogger().println("Please provide a valid version pattern.");
                return false;
            }
            
            // Expand env variables
            String assemblyTitle = envVars.expand(this.assemblyTitle);
            String assemblyDescription = envVars.expand(this.assemblyDescription);
            String assemblyCompany = envVars.expand(this.assemblyCompany);
            String assemblyProduct = envVars.expand(this.assemblyProduct);
            String assemblyCopyright = envVars.expand(this.assemblyCopyright);
            String assemblyTrademark = envVars.expand(this.assemblyTrademark);
            String assemblyCulture = envVars.expand(this.assemblyCulture);
            
            
            // Log new expanded values
            listener.getLogger().println(String.format("Changing File(s): %s", assemblyGlob));
            listener.getLogger().println(String.format("Assembly Version : %s",  version));
            listener.getLogger().println(String.format("Assembly Title : %s",  assemblyTitle));
            listener.getLogger().println(String.format("Assembly Description : %s",  assemblyDescription));
            listener.getLogger().println(String.format("Assembly Company : %s",  assemblyCompany));
            listener.getLogger().println(String.format("Assembly Product : %s",  assemblyProduct));
            listener.getLogger().println(String.format("Assembly Copyright : %s",  assemblyCopyright));
            listener.getLogger().println(String.format("Assembly Trademark : %s",  assemblyTrademark));
            listener.getLogger().println(String.format("Assembly Culture : %s",  assemblyCulture));
            
            for (FilePath f : build.getWorkspace().list(assemblyGlob))
            {
                // Update the AssemblyVerion and AssemblyFileVersion
                new ChangeTools(f, this.regexPattern, this.replacementPattern).Replace(version, listener);
                
                // Set new things, empty string being ok for them.
                // TODO: Would we need a regex for these or just blast as we are doing now?
                new ChangeTools(f, "AssemblyTitle[(]\".*\"[)]", "AssemblyTitle(\"%s\")").Replace(assemblyTitle, listener);            
                new ChangeTools(f, "AssemblyDescription[(]\".*\"[)]", "AssemblyDescription(\"%s\")").Replace(assemblyDescription, listener);
                new ChangeTools(f, "AssemblyCompany[(]\".*\"[)]", "AssemblyCompany(\"%s\")").Replace(assemblyCompany, listener);
                new ChangeTools(f, "AssemblyProduct[(]\".*\"[)]", "AssemblyProduct(\"%s\")").Replace(assemblyProduct, listener);
                new ChangeTools(f, "AssemblyCopyright[(]\".*\"[)]", "AssemblyCopyright(\"%s\")").Replace(assemblyCopyright, listener);
                new ChangeTools(f, "AssemblyTrademark[(]\".*\"[)]", "AssemblyTrademark(\"%s\")").Replace(assemblyTrademark, listener);
                new ChangeTools(f, "AssemblyCulture[(]\".*\"[)]", "AssemblyCulture(\"%s\")").Replace(assemblyCulture, listener);
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            listener.getLogger().println(sw.toString());
            return false;
        }
        return true;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) {
        try {
            
            writeHeader(DESCRIPTOR.getDisplayName(), listener);
                       
            EnvVars envVars = run.getEnvironment(listener);

            String assemblyGlob = this.assemblyFile == null || this.assemblyFile.equals("") ? "**/AssemblyInfo.*" : this.assemblyFile;
            listener.getLogger().println(String.format("Changing File(s): %s", assemblyGlob));
            
            for (FilePath file : workspace.list(assemblyGlob))
            {
                UpdateAssemblyVersion(envVars, listener, file);                
                UpdateAssemblyTitle(envVars, listener, file);
                UpdateAssemblyDescription(envVars, listener, file);
                UpdateAssemblyCompany(envVars, listener, file);
                UpdateAssemblyProduct(envVars, listener, file);
                UpdateAssemblyCopyright(envVars, listener, file);
                UpdateAssemblyTrademark(envVars, listener, file);
                UpdateAssemblyCulture(envVars, listener, file);
            }
            
            writeFooter(listener);
            
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            listener.getLogger().println(sw.toString());
            run.setResult(Result.FAILURE);
        }
        run.setResult(Result.SUCCESS);        
    }  

    private void UpdateAssemblyCulture(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String culture = envVars.expand(this.assemblyCulture);
        if (culture != null && !culture.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Culture : %s",  culture));
        }
        new ChangeTools(f, "AssemblyCulture[(]\".*\"[)]", "AssemblyCulture(\"%s\")").Replace(culture, listener);
    }

    private void UpdateAssemblyTrademark(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String trademark = envVars.expand(this.assemblyTrademark);
        if (trademark != null && !trademark.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Trademark : %s",  trademark));
        }
        new ChangeTools(f, "AssemblyTrademark[(]\".*\"[)]", "AssemblyTrademark(\"%s\")").Replace(trademark, listener);
    }

    private void UpdateAssemblyCopyright(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String copyright = envVars.expand(this.assemblyCopyright);
        if (copyright != null && !copyright.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Copyright : %s",  copyright));
        }
        
        new ChangeTools(f, "AssemblyCopyright[(]\".*\"[)]", "AssemblyCopyright(\"%s\")").Replace(copyright, listener);
    }

    private void UpdateAssemblyProduct(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String product = envVars.expand(this.assemblyProduct);
        if (product != null && !product.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Product : %s",  product));
        }
        new ChangeTools(f, "AssemblyProduct[(]\".*\"[)]", "AssemblyProduct(\"%s\")").Replace(product, listener);
    }

    private void UpdateAssemblyCompany(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String company = envVars.expand(this.assemblyCompany);
        
        if (company != null && !company.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Company : %s",  company));
        }
        new ChangeTools(f, "AssemblyCompany[(]\".*\"[)]", "AssemblyCompany(\"%s\")").Replace(company, listener);
    }

    private void UpdateAssemblyDescription(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String description = envVars.expand(this.assemblyDescription);
        if (description != null && !description.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Description : %s",  description));
        }
        new ChangeTools(f, "AssemblyDescription[(]\".*\"[)]", "AssemblyDescription(\"%s\")").Replace(description, listener);
    }

    private void UpdateAssemblyTitle(EnvVars envVars, TaskListener listener, FilePath f) throws IOException, InterruptedException {
        String title = envVars.expand(this.assemblyTitle);
        if (title != null && !title.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Title : %s",  title));
        }
        new ChangeTools(f, "AssemblyTitle[(]\".*\"[)]", "AssemblyTitle(\"%s\")").Replace(title, listener);
    }

    private void UpdateAssemblyVersion(EnvVars envVars, TaskListener listener, FilePath f) throws IllegalArgumentException, Exception {

        if (versionPattern == null || StringUtils.isEmpty(versionPattern))
        {
            throw new IllegalArgumentException("Please provide a valid version pattern.");
        }
        String version = new AssemblyVersion(this.versionPattern, envVars).getVersion();
        if (version != null && !version.isEmpty())
        {
            listener.getLogger().println(String.format("Assembly Version : %s",  version));
        }
        new ChangeTools(f, this.regexPattern, this.replacementPattern).Replace(version, listener);
    }
    
    private void writeHeader(String header, TaskListener listener){
        listener.getLogger().println();
        listener.getLogger().println("################################");
        writeRow(header, listener);
        listener.getLogger().println("################################");
        listener.getLogger().println();
    }
    
    private void writeRow(String content, TaskListener listener){
        int availableCharacterSpace = 28;
        String formattedContent;

        if (content.length() > availableCharacterSpace){
            formattedContent = content.substring(0, 25) + "...";
        } else {
            formattedContent = content;
        }

        String padding = new String(new char[(availableCharacterSpace - formattedContent.length())/2]).replace("\0", " ");

        listener.getLogger().println(String.format("# %1s%2s%3s #", padding, formattedContent, padding));
    }

    private void writeFooter(TaskListener listener) {
        listener.getLogger().println();
    }

    @Override
    public BuildStepDescriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        protected DescriptorImpl() {
            super(ChangeAssemblyVersion.class);
        }

        @Override
        public String getDisplayName() {
            return "Change Assembly Info";
        }

        @Override
        public String getHelpFile() {
            return "help.html";
        }

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }        
    }
}
