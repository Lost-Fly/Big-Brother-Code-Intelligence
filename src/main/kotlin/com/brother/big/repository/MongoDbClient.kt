package com.brother.big.repository

import com.brother.big.model.*
import com.brother.big.utils.BigLogger.logInfo
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import org.bson.Document


class MongoDbClient(
    connectionString: String = "mongodb://localhost:27017", // TODO - move to properties all below
    databaseName: String = "bigBrotherDb", // TODO - move to properties
    developersCollectionName: String = "developers", // TODO - move to properties
    resultsCollectionName: String = "analysisResults" // TODO - move to properties
) {

    private val client: MongoClient = MongoClients.create(connectionString)
    private val database = client.getDatabase(databaseName)

    private val developersCollection: MongoCollection<Document> = database.getCollection(developersCollectionName)
    private val resultsCollection: MongoCollection<Document> = database.getCollection(resultsCollectionName)

    private val mapper = jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)

    fun saveDeveloper(developer: Developer): Boolean {
        return try {
            val developerDoc = Document.parse(mapper.writeValueAsString(developer))
            developersCollection.replaceOne(
                Filters.eq("name", developer.name),
                developerDoc,
                com.mongodb.client.model.ReplaceOptions().upsert(true)
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDeveloper(name: String): Developer? {
        return try {
            val developerDoc = developersCollection.find(Filters.eq("name", name)).first()

            developerDoc?.let { mapper.readValue(it.toJson(), Developer::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveAnalysisResult(developerName: String, analysisResult: AnalysisResult): Boolean {
        return try {
            val resultDoc = Document(mapOf(
                "developerName" to developerName,
                "analysisResult" to Document.parse(mapper.writeValueAsString(analysisResult))
            ))

            resultsCollection.replaceOne(
                Filters.eq("developerName", developerName),
                resultDoc,
                com.mongodb.client.model.ReplaceOptions().upsert(true)
            )
            logInfo("SAVED ANALYSE FOR DEV: $developerName TO DB")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun getAnalysisResultByDeveloper(developerName: String): AnalysisResult? {
        return try {
            val resultDoc = resultsCollection.find(Filters.eq("developerName", developerName)).first()

            val analysisResult = resultDoc?.get("analysisResult") as? Document
            analysisResult?.let { mapper.readValue(it.toJson(), AnalysisResult::class.java) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAllAnalysisResults(): List<AnalysisResult> {
        return try {
            resultsCollection.find().mapNotNull { resultDoc ->
                val analysisDocument = resultDoc.get("analysisResult") as? Document
                analysisDocument?.let {
                    mapper.readValue(it.toJson(), AnalysisResult::class.java)
                }
            }.toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}