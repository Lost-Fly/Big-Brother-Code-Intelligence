package com.brother.big.integration

import com.brother.big.model.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import java.io.IOException

class GitIntegration {

    fun getCommits(developerName: String, token: String?, repositoryUrl: String): List<Commit> {
        val tmpDir = File.createTempFile("repo_", "to_$developerName") // TODO - add restriction dir size for cloning! If there ae no enough space at server to store files drop request
        tmpDir.delete()

        try {
            val gitBuilder = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(tmpDir)
                .setCloneAllBranches(true) // TODO - add restriction dir size for cloning! If there ae no enough space at server to store files drop request

            if (token != null) {
                gitBuilder.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(developerName, token)
                )
            }

            gitBuilder.call().use { git -> // TODO - add restriction dir size for cloning! If there ae no enough space at server to store files drop request
                val repository = git.repository
                val commits: MutableList<Commit> = mutableListOf() // TODO restrict commits length by config parameter

                git.log().all().call().forEach { revCommit -> // TODO parse and save only last N commits, N - config value
                    val userEmail = revCommit.authorIdent.emailAddress
                    val flag = userEmail.toString().contains(developerName)
                    if (revCommit.authorIdent.name == developerName || flag) {
                        val commit = extractCommit(repository, revCommit, developerName)
                        // TODO add commit message analyse (determine if commit is important or there are just small fixes by LLM Model)
                        commits.add(commit) // TODO restrict commits length by config parameter if limit exceeded - stop adding commits and continue
                    }
                }

                return commits
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException("Failed to clone repository: ${e.message}")
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