package org.jenkinsci.plugins.changeassemblyversion;

import hudson.EnvVars;

public class AssemblyVersion {
	
	
	private final String version;
	
	/**
	 * The instance of this class gonna return in the property version the value to be used on ChangeTools.
	 * @param version
	 * @param envVars
	 */
	public AssemblyVersion(String version, EnvVars envVars){
            this.version = envVars.expand(version);
	}
	
	public String getVersion(){
		return this.version;
        }
}
