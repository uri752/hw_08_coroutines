data class Post(
    val id: Long,
    val authorId: Long,
    val content: String,
    val published: Long,
    val likeByMe: Boolean,
    val likes: Int = 0,
    val attachment: Attachment? = null,
)
