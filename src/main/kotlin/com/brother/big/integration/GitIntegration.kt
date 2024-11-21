package com.brother.big.integration

import com.brother.big.model.Commit
import com.brother.big.utils.BigLogger.logError
import com.brother.big.utils.Config
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import java.io.File
import java.io.IOException

class GitIntegration {
    companion object {
        val MAX_COMMITS: Int = Config["git.maxCommits"]?.toInt() ?: 1000
        val MAX_DEV_COMMITS: Int = Config["git.maxDevCommits"]?.toInt() ?: 10
        val MAX_TEMP_DIR_SIZE_MB: Long = Config["git.maxTempSpaceMb"]?.toLong() ?: 100
    }

    fun getCommits(developerName: String, token: String?, repositoryUrl: String): List<Commit> {
        val tmpDir = createRestrictedTempDir(developerName)

        try {
            val gitBuilder = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(tmpDir)
                .setCloneAllBranches(true)
                .setDepth(MAX_COMMITS)

            token?.let {
                gitBuilder.setCredentialsProvider(
                    org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider(developerName, token)
                )
            }

            gitBuilder.call().use { git ->
                if (tmpDir.length() > convertMbToBytes(MAX_TEMP_DIR_SIZE_MB)) {
                    cleanupTempDir(tmpDir)
                    logError("Temporary directory size exceeded the allowed limit of $MAX_TEMP_DIR_SIZE_MB MB")
                }

                val repository = git.repository
                val commits: MutableList<Commit> = mutableListOf()

                git.log().all().call().asSequence()
                    .filter { revCommit ->
                        revCommit.authorIdent.name == developerName ||
                                revCommit.authorIdent.emailAddress.toString().contains(developerName)
                    }
                    .take(MAX_DEV_COMMITS)
                    .forEach { revCommit ->
                        val commit = extractCommit(repository, revCommit, developerName)
                        commits.add(commit)
                    }

                return commits
            }
        } catch (e: IOException) {
            e.printStackTrace()
            logError("Failed to clone repository: ${e.message}")
            throw RuntimeException("Failed to clone repository: ${e.message}")
        }
    }

    private fun createRestrictedTempDir(developerName: String): File {
        val tmpDir = File.createTempFile("repo_", "to_$developerName")
        if (!tmpDir.delete() || !tmpDir.mkdir()) {
            logError("Failed to create temporary directory")
        }
        return tmpDir
    }

    private fun cleanupTempDir(tmpDir: File) {
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
        }
    }

    private fun convertMbToBytes(mb: Long): Long = mb * 1024 * 1024

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