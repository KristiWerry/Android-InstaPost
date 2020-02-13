package kristi.cs646_assignment4

import android.net.Uri

//class to store the information about each post which includes the post ID, the URI image,
//description, and the publisher
class Post (var postid: String,
            var postimage:Uri,
            var description:String,
            var publisher: String) {

    fun getPostId(): String {
        return postid
    }
    fun getPostImage(): Uri {
        return postimage
    }
    fun getPostDescription():String {
        return description
    }
    fun getPostPublisher(): String {
        return publisher
    }
}
