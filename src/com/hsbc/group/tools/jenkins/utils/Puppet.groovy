package com.hsbc.group.tools.jenkins.utils
import java.util.regex.*
import groovy.json.JsonSlurper
import com.cloudbees.plugins.credentials.impl.*;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.domains.*;

def fileName = 'C:/Users/alanf/eclipse-workspace/jenkins-global-library/src/data.txt'

def parseRepoString(repoStr, String refLine) {
	def tokens = repoStr.split('[@:/]')
	def repoDetails = [:]
	repoDetails['host'] = tokens[1]
	repoDetails['org'] = tokens[2]
	repoDetails['repo'] = tokens[3].toString().replace('.git', '')
	repoDetails['ref'] = refLine.substring(refLine.lastIndexOf('=>') + 2).replaceAll("'", "").stripIndent()
	return repoDetails
}

def printRepo(java.util.Map repo) {
	def msg = sprintf("Checking %s : %s : %s : %s ",repo['host'], repo['org'], repo['repo'], repo['ref']   )
	println(msg)
}

def getModule(java.util.Map repoDetails) {
	def gitHost = repoDetails['host']
	def org = repoDetails['org']
	def repository = repoDetails['repo']
	def refId = repoDetails['ref']
	checkout ([$class: 'GitSCM',
		branches:[[name: refId ]],
		userRemoteConfig: [[
				credentialsId: 'github',
				url: gitHost + '/' +org + '/' + repostory]]])
}

def getPuppetRepos(puppetFile) {
	File file = new File( puppetFile )
	def repos = []
	def regex = 'git@.*.git'
	def repoStr
	def incr = 0

	if( file.exists() ) {

		file.eachLine { line ->

			if (line.startsWith('mod') ) {
				incr=0
				repoStr = ''
			}

			if (line.contains(':git') && incr >= 1) {
				Matcher match = Pattern.compile(regex).matcher(line)
				if (match)
					repoStr = match.group()
			}

			if (incr >=2 && (line.contains(':commit') || line.contains(':ref')))
				repos.add(parseRepoString(repoStr, line))

			incr++
		}
	} else {
		println "File does not exist"
	}

	return repos
}


repos =getPuppetRepos(fileName)
for ( repo in repos) {
	printRepo(repo)
	getModule(repo)
}
