package com.example.yueyeushaokaojiaoziguan.merchant

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL

class MerchantHttpClient(
    private val baseUrl: String = MerchantApiConfig.baseApiUrl
) {
    fun get(path: String): MerchantApiResult<String> {
        return request(path = path, method = "GET")
    }

    fun post(path: String, body: String): MerchantApiResult<String> {
        return request(path = path, method = "POST", body = body)
    }

    private fun request(path: String, method: String, body: String? = null): MerchantApiResult<String> {
        val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Content-Type", "application/json")
            doInput = true
            if (method == "POST") {
                doOutput = true
            }
        }

        return try {
            if (!body.isNullOrBlank()) {
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code in 200..299) {
                MerchantApiResult.Success(text)
            } else {
                MerchantApiResult.Error(
                    message = if (text.isNotBlank()) text else "HTTP request failed",
                    code = code
                )
            }
        } catch (error: UnknownHostException) {
            MerchantApiResult.Error("DNS解析失败: ${baseUrl}")
        } catch (error: SocketTimeoutException) {
            MerchantApiResult.Error("请求超时(8s): $path")
        } catch (error: javax.net.ssl.SSLException) {
            MerchantApiResult.Error("SSL错误: ${error.message}")
        } catch (error: Exception) {
            MerchantApiResult.Error("${error.javaClass.simpleName}: ${error.message}")
        } finally {
            connection.disconnect()
        }
    }
}
