package com.brother.big.integration

import com.brother.big.model.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import java.io.IOException

class GitIntegration {

    fun getCommits(developerName: String, token: String?, repositoryUrl: String): List<Commit> {
        // TODO - change storsge method
        val tmpDir = File.createTempFile("repo_", "to_$developerName")
        tmpDir.delete()

        try {
            val gitBuilder = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(tmpDir)
                .setCloneAllBranches(true)

            if (token != null) {
                gitBuilder.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(developerName, token)
                )
            }

            gitBuilder.call().use { git ->
                val repository = git.repository
                val commits: MutableList<Commit> = mutableListOf()

                git.log().all().call().forEach { revCommit ->
                    val userEmail = revCommit.authorIdent.emailAddress
                    val flag = userEmail.toString().contains(developerName)
                    if (revCommit.authorIdent.name == developerName || flag) {
                        val commit = extractCommit(repository, revCommit, developerName)
                        commits.add(commit)
                    }
                }

                return commits
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Failed to clone repository: ${e.message}")
        } finally {
            // tmpDir.deleteRecursively() SERVER BABAX // TODO
        }
    }

    private fun extractCommit(repository: org.eclipse.jgit.lib.Repository, commit: RevCommit, developerName: String): Commit {
        val commitId = commit.id.name
        val commitMessage = commit.fullMessage
        val commitTime = commit.commitTime.toLong() * 1000
        val files: MutableList<File> = mutableListOf()

        val treeWalk = TreeWalk(repository).apply {
            addTree(commit.tree)
            isRecursive = true
        }

        while (treeWalk.next()) {
            val path = treeWalk.pathString
            val file = File(repository.directory.parent, path)
            if (file.exists()) {
                files.add(file)
            }
        }
        return Commit(id = commitId, developer = developerName, message = commitMessage, timestamp = commitTime, files = files)
    }
}