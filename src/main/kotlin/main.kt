import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val BASE_URL = "http://localhost:9999/api/slow/"
fun main() {
    CoroutineScope(EmptyCoroutineContext).launch {
        val posts = parseResult("${BASE_URL}/posts", object : TypeToken<List<Post>>() {} )

        //forEach
        val postsWitAuthor = posts.map {post ->
            val author = parseResult("${BASE_URL}authors/${post.authorId}", object : TypeToken<Author>() {} )
            PostWithAuthor(post, author)
        }

        println(postsWitAuthor)

    }
    Thread.sleep(35_000)
}

private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

private val gson = Gson()

// получается 2 саспенд-функции: 1)результат, 2) парсинг
suspend fun <T> parseResult(url: String, typeToken: TypeToken<T>) : T {
    val requestBody = requireNotNull(makeRequest(url).body).string()
    return withContext(Dispatchers.Default) { gson.fromJson(requestBody, typeToken.type) }
}

// модифайер suspend "приостановить"
suspend fun makeRequest(url: String): Response = suspendCoroutine{ continuation ->
    Request.Builder()
        .url(url)
        .build()
        .let(client::newCall)
        .enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

        })
}