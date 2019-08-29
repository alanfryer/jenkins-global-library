import com.hsbc.group.tools.jenkins.utils.Vault

def call(Map config) {
	
	Vault vault = new Vault()
	vault.addJenkinsCredentials(config.secretPath, config.vaultRole, config.vaultApiUrl)
	
}