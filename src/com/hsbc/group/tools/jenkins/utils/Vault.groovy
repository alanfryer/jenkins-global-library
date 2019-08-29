package com.hsbc.group.tools.jenkins.utils
import groovy.json.JsonSlurper
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import groovy.transform.Field


@Field def vaultApiUrl
@Field def secretPath
@Field def vaultRole
@Field def clientToken
def getKubernetesToken() {
	try {
		return new File('/var/run/secrets/kubernetes.io/serviceaccount/token').getText().replace('\n', '')
	} catch (Exception e) {
		println('getKubernetesToken: ' + e.toString())
	}
}

def callHttp(url) {
	def response = [:]
	def http = new URL(url).openConnection() as HttpURLConnection
	try {
		
		http.setRequestMethod('GET')
		http.setDoOutput(true)
		http.setRequestProperty('X-Vault-Token', this.clientToken)
		http.connect()

		if (http.responseCode == 200) {
			response = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
			return response
		} else {
			response = new JsonSlurper().parseText(http.errorStream.getText('UTF-8'))
			response['errors']
			throw new Exception("HTTP Request failed ${response['errors']}")
		}
	} catch (Exception e) {
		println('callHttp: ' + e.toString())
	} finally {
		http.disconnect()
	}
}

def getClientToken() {
	def response = [:]
	def http = new URL(this.vaultApiUrl + '/auth/kubernetes/login').openConnection() as HttpURLConnection
	try {
		def kubeToken = getKubernetesToken()
		def body = '{"jwt": "' + kubeToken + '", "role": "' + this.vaultRole + '"}'
		
		http.setRequestMethod('POST')
		http.setDoOutput(true)
		http.outputStream.write(body.getBytes('UTF-8'))
		http.connect()

		if (http.responseCode == 200) {
			response = new JsonSlurper().parseText(http.inputStream.getText('UTF-8'))
			return response['auth']['client_token']
		} else {
			response = new JsonSlurper().parseText(http.errorStream.getText('UTF-8'))
			response['errors']
			throw new Exception("Error getting the Vault Client Token ${response['errors']}")
		}
	} catch (Exception e) {
		println('getClientToken: ' + e.toString())
	} finally {
		http.disconnect()
	}
}

def getCredentialList() {
	def response = [:]
	def url =  this.vaultApiUrl + '/secret/' + this.secretPath + '?list=true'
	try {
		response = callHttp(url)
		return  response['data']['keys']
	} catch (Exception e) {
		println('getVaultCredential: ' + e.toString())
	}
}

def getVaultCredential(secretName) {
	def response = [:]
	def creds = [:]
	def url = this.vaultApiUrl + '/secret/' + this.secretPath + '/' + secretName
	try {
		response = callHttp(url)
		creds['username'] = response['data']['username']
		creds['password'] = response['data']['password']
		return creds
	} catch (Exception e) {
		println('getVaultCredential: ' + e.toString())
	}
}

def getUsername(cred) {
	return cred['username']
}

def getPassword(cred) {
	return cred['password']
}

def addJenkinsCredential(user, pwd) {
	Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, user, user + ' EASYCICD Credentials', user, pwd)
	SystemCredentialsProvider.getInstance().getStore().addCredentials(Domain.global(), c)
}


def addJenkinsCredentials(path, role, apiUrl) {
	this.vaultApiUrl = apiUrl
	this.secretPath = path
	this.vaultRole = role
	this.clientToken = getClientToken()

	for (cred in getCredentialList()) {
		credential = getVaultCredential(cred)
		addJenkinsCredential(getUsername(credential), getPassword(credential))
		println('Added UserPasswordCredentials for: ' + getUsername(credential))
	}
}
