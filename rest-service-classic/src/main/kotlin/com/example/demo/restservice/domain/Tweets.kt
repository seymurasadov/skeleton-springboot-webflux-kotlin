package com.example.demo.restservice.domain

//import com.example.demo.logging.AppLogger
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

data class Tweet(
        val id: String,
        val createdAt: Instant,
        val author: String,
        val message: String
)

@Component
class TweetService(private val repository: TweetRepository) {

    fun create(author: String, message: String) = Tweet(
            author = author,
            message = message,
            createdAt = Instant.now(),
            id = "${System.nanoTime()}-${UUID.randomUUID()}"
    )

    fun submit(tweet: Tweet) = repository.add(tweet)
    fun get(tweetId: String) = repository.getOrNull(tweetId)
    fun getAll() = repository.getItems()
    fun findByAuthor(author: String) = getAll().filter { it.author == author }
}

typealias TweetRepositoryCache = Cache<String, Tweet>
@Component
class TweetRepository() {
    // private val LOGGER = AppLogger.get(javaClass)

    private val cache: TweetRepositoryCache by lazy {
        val expiry = Duration.ofDays(3)

        Caffeine
                .newBuilder()
                .maximumSize(1_000_000)
                .expireAfterWrite(expiry.seconds, TimeUnit.SECONDS)
                .build<String, Tweet>()
    }

    fun add(item: Tweet) {
        cache.put(item.id, item)
        println("$this : add item to repository. itemId=${item.id}")
        // LOGGER.info("add item to repository. itemId=${item.id}")
    }

    fun getOrNull(itemId: String): Mono<Tweet> = Mono.justOrEmpty(cache.getIfPresent(itemId))

    fun getItems(): Flux<Tweet> {
        val items = cache.asMap().values
        return Flux.fromIterable(cache.asMap().values)
    }
}
