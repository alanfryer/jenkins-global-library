import com.hsbc.group.tools.jenkins.utils.Vault

def call(Map config) {
	Vault vault = new Vault()
	if (isValidInput(config)) {
		vault.addJenkinsCredentials(config.secretPath, config.vaultRole, config.vaultApiUrl)
	}
}

def isValidInput(Map config) {
	def message
	if (!config.secretPath || !config.vaultRole || !config.vaultApiUrl) {
		println(config.secretPath + ' ' + config.vaultRole + ' ' + config.vaultApiUrl)
		message = 'secretPath or vaultRole or vaultApiUrl cannot be blank'
		println(message)
		return false
	} else {
		return true
	}
}