import com.hsbc.group.tools.jenkins.utils.Vault

def call(MAp config) {
	Vault vault = new Vault()
	vault.addJenkinsCredentials('easycicd', 'easycicd')
	
}