package com.example.canteen.data

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

class EmployeeFetcher {

    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existingCookies = cookieStore[url.host]?.toMutableList() ?: mutableListOf()
                for (newCookie in cookies) {
                    // Remove existing cookie with same name if exists (overwrite)
                    existingCookies.removeAll { it.name == newCookie.name }
                    existingCookies.add(newCookie)
                }
                cookieStore[url.host] = existingCookies
                
                // Debug logs for cookies
                Log.d("EmployeeFetcher", "Cookies saved for ${url.host}: ${cookies.map { it.name }}")
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val cookies = cookieStore[url.host] ?: emptyList()
                // Debug which cookies are being attached
                if (cookies.isNotEmpty()) {
                    Log.d("EmployeeFetcher", "Cookies sending to ${url.host}: ${cookies.map { it.name }}")
                }
                return cookies
            }
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun fetchEmployees(): List<Employee> {
        // We will throw exceptions to let the UI show exactly what failed.
        val baseUrl = "https://maxstreicherpipingtrackingsystem-gmbfg6dkg9bdcfge.westeurope-01.azurewebsites.net"
        
        val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // 1. GET Login Page
        val loginPageRequest = Request.Builder()
            .url("$baseUrl/Account/Login")
            .header("User-Agent", userAgent)
            .build()

        val loginPageResponse = client.newCall(loginPageRequest).execute()
        if (!loginPageResponse.isSuccessful) {
            throw Exception("Login Page Load Failed: HTTP ${loginPageResponse.code}")
        }

        val loginHtml = loginPageResponse.body?.string() ?: ""
        val loginDoc = Jsoup.parse(loginHtml)
        
        // Extract Verification Token
        val verificationToken = loginDoc.select("input[name=__RequestVerificationToken]").first()?.attr("value")
        
        // Extract Form Action dynamically
        val formAction = loginDoc.select("form").first()?.attr("action") ?: "/Account/Login"
        val loginPostUrl = if (formAction.startsWith("http")) formAction else "$baseUrl$formAction"
        
        Log.d("EmployeeFetcher", "Login Target: $loginPostUrl")
        
        if (verificationToken == null) {
            Log.e("EmployeeFetcher", "HTML dumped (first 500 chars): ${loginHtml.take(500)}")
            throw Exception("Anti-Forgery Token NOT FOUND on login page.")
        }

        // 2. POST to Login
        val formBody = FormBody.Builder()
            .add("Username", "F.Tonelli")
            .add("Password", "Streicher#2026")
            .add("__RequestVerificationToken", verificationToken)
            .build()

        val loginPostRequest = Request.Builder()
            .url(loginPostUrl) 
            .header("User-Agent", userAgent)
            .post(formBody)
            .build()

        val loginPostResponse = client.newCall(loginPostRequest).execute()
        if (!loginPostResponse.isSuccessful) {
            throw Exception("Login POST Failed: HTTP ${loginPostResponse.code}")
        }
        
        // Validate Login Success via URL or Content
        val postUrl = loginPostResponse.request.url.toString()
        val postBody = loginPostResponse.body?.string() ?: ""
        Log.d("EmployeeFetcher", "Post-Login URL: $postUrl")

        if (postUrl.contains("Login") || postBody.contains("Invalid login attempt")) {
             throw Exception("Login Failed: Still on login page after POST. URL: $postUrl")
        }
        
        // 3. Fetch Data from API
        // We use a large length to fetch all employees in one go (server permitting)
        val apiUrl = Request.Builder().url("$baseUrl/HR/GetEmployees").build().url.newBuilder()
            .addQueryParameter("draw", "1")
            .addQueryParameter("start", "0")
            .addQueryParameter("length", "10000") // Fetch up to 10k items
            .addQueryParameter("search[value]", "")
            .addQueryParameter("search[regex]", "false")
            .build()
            
        Log.d("EmployeeFetcher", "Fetching Data API: $apiUrl")

        val apiRequest = Request.Builder()
            .url(apiUrl)
            .header("User-Agent", userAgent)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .get()
            .build()

        val apiResponse = client.newCall(apiRequest).execute()
        val apiBody = apiResponse.body?.string() ?: ""
        
        if (!apiResponse.isSuccessful) {
             throw Exception("API Call Failed: HTTP ${apiResponse.code}")
        }
        
        // 4. Parse JSON
        val allEmployees = mutableListOf<Employee>()
        try {
            val jsonObject = org.json.JSONObject(apiBody)
            val dataArray = jsonObject.getJSONArray("data")
            
            Log.d("EmployeeFetcher", "Parsing ${dataArray.length()} items from JSON")
            
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                
                // NEW: Handle nested 'employee' object if present
                val source = if (item.has("employee") && !item.isNull("employee")) {
                    item.getJSONObject("employee")
                } else {
                    item
                }
                
                // Helper to find key case-insensitively in the source object
                fun getJsonString(vararg keys: String): String {
                    for (key in keys) {
                        if (source.has(key)) return source.optString(key, "")
                        val itemKeys = source.keys()
                        while (itemKeys.hasNext()) {
                            val k = itemKeys.next()
                            if (k.equals(key, ignoreCase = true)) return source.optString(k, "")
                        }
                    }
                    return ""
                }
                
                val name = getJsonString("fullName", "name", "displayName", "employeeName")
                val firstName = getJsonString("firstName", "name")
                val lastName = getJsonString("lastName", "surname")
                val company = getJsonString("company", "companyName")
                val status = getJsonString("status")
                
                // Filtering will happen in Repository now, we just pass raw data but exclude Demobilized to save memory/processing
                val isDemobilized = status.contains("Demobilized", ignoreCase = true)
                
                if (!isDemobilized && name.isNotBlank()) {
                    // Fallback if specific fields are empty but Name isn't
                    val finalFirst = if (firstName.isNotBlank()) firstName else name.split(" ").firstOrNull() ?: ""
                    val finalLast = if (lastName.isNotBlank()) lastName else name.split(" ").lastOrNull() ?: ""
                    
                    allEmployees.add(Employee(name, company, finalFirst, finalLast))
                } else if (i == 0) {
                    Log.d("EmployeeFetcher", "First item skipped. Name: '$name', Status: '$status', IsDemobilized: $isDemobilized, Source: $source")
                }
            }
        } catch (e: Exception) {
            Log.e("EmployeeFetcher", "JSON Parsing Error", e)
            throw Exception("Failed to parse API JSON: ${e.message}")
        }

        if (allEmployees.isEmpty()) {
            val firstItem = try {
                val jsonObject = org.json.JSONObject(apiBody)
                val dataArray = jsonObject.getJSONArray("data")
                if (dataArray.length() > 0) dataArray.getJSONObject(0).toString() else "Empty Array"
            } catch (e: Exception) { "Error retrieving first item" }
            
            Log.e("EmployeeFetcher", "No employees found. First item sample: $firstItem")
            throw Exception("Scraping finished but found 0 employees. JSON sample: $firstItem")
        }

        return allEmployees
    }
}
