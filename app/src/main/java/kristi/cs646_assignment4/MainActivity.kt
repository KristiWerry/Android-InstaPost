package kristi.cs646_assignment4

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_home.*
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import kotlin.collections.ArrayList

private const val MAIN = "Main"
class MainActivity : AppCompatActivity() {
    private var storageReference: StorageReference? = null //storage reference
    private val db = FirebaseFirestore.getInstance() //database reference
    private val currentUser = FirebaseAuth.getInstance().currentUser //authentication
    private var posts: ArrayList<Post> = arrayListOf() //list of posts (see post class)
    private var listOfUsers:MutableList<String> = mutableListOf() //list of all the users that we want to see posts for
    private var username = currentUser?.displayName //current users username
    private val uid = currentUser?.uid //current users ID
    private val RESULT_FROM_USERS = 1234 //code for userlist
    private var RESULT_FROM_HASHTAGS = 4321 //code for hashtaglist
    private var fromLists:String? = null //the returned string from userlist or hashtaglist to get redirected
    private var whatToPost:Int = 0 //states, 0 is all posts, 1 is a specific user's posts, 2 is a hashtag's posts
    private var recycle:RecyclerView? = null  //findViewById(R.id.recyclerView) as RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recycle = findViewById(R.id.recyclerView) as RecyclerView

        postButton.setOnClickListener{
            //if in this activity and the user wants to post something
            //go to the post activity
            val intent = Intent(this@MainActivity, PostActivity::class.java)
            startActivity(intent)
        }
        listUsers.setOnClickListener {
            //Go to the list of users
            val intent = Intent(this@MainActivity, UserListActivity::class.java)
            startActivityForResult(intent, RESULT_FROM_USERS)
        }
        listHashtags.setOnClickListener {
            //go to the list of hashtags
            val intent = Intent(this@MainActivity, HashtagListActivity::class.java)
            startActivityForResult(intent, RESULT_FROM_HASHTAGS)
        }
        refreshButton.setOnClickListener {
            //sets up the recyclerview and the information in it
            //refreshpage will call the database and storage and show all the results
            refreshPage()
        }
        Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show()
        //check if we have internet connection
        val connMgr = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            refreshPage() //we want to show to view when we first get to the page
        } else {
            // display error
            Toast.makeText(this, "No Internet Connection",Toast.LENGTH_SHORT).show()
        }

    }
    private fun refreshPage() {
        //since this function can get called multiple times, we need to clear the
        //old list so we don't show repeat posts
        posts.clear()
        listOfUsers.clear()
        //We choose what we are going to post
        when(whatToPost) { //each of these functions gets a list of posts that we then feed into the recyclerview
            0 -> getAllUsers() //everything from every user
            1 -> getOneUser(fromLists) //everything from one user //fromLists has a specific user
            2 -> getHashtag() //everything from one hashtag
        }
        whatToPost = 0 //set back to zero so if user refreshes, we go back to getting everything
        storageReference = FirebaseStorage.getInstance().reference
        //set the recyclerview in the layout
        recycle = findViewById(R.id.recyclerView) as RecyclerView
        val layout = LinearLayoutManager(this@MainActivity)
        recycle?.setLayoutManager(layout)
    }

    private inner class ViewHolder(itemView: View, private val context: MainActivity) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val usernameText: TextView //textview for the username
        private val photoImage: ImageView //imageview for the post image
        private val desText: TextView //textview for the description
        private var post: Post? = null //post with all the information (see post class)
        init {
            itemView.setOnClickListener(this)
            usernameText = itemView.findViewById(R.id.username) as TextView
            photoImage = itemView.findViewById(R.id.post_image) as ImageView
            desText = itemView.findViewById(R.id.post_description) as TextView
        }
        fun bindPost(location: Int, post: Post) {
            //gets called for every post in the list and sets the text, photo, and info
            this.post = post
            usernameText.text = post.publisher
            photoImage.setImageURI(post.getPostImage())
            desText.text = post.description
        }
        override fun onClick(source: View) {
            //nothing
            //but could be used to download images to the phone storage
        }
    }
    private inner class RecycleAdapter(private val post: List<Post>,
                                       private val context: MainActivity) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            //inflating the correct view
            val layoutInflater = LayoutInflater.from(this@MainActivity)
            val view = layoutInflater.inflate(R.layout.see_posts_item, parent, false)
            return ViewHolder(view, context)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //for every post in the list of posts, we get the position and bind it into a list item
            val curPost = post.get(position)
            holder.bindPost(position, curPost)
        }
        override fun getItemCount(): Int {
            return post.size
        }
    }
    //gets all the users in the database and adds them to the list of users
    private fun getAllUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id == uid) { //get current user's nickname
                        username = document.data["Nickname"].toString()
                        Log.i(MAIN, "${document.id} => ${username}")
                    }
                    listOfUsers.add(document.id) //adding users to the list of users
                    //for every user, get all of their posts
                    getPosts(document.data["Nickname"].toString(), document.id)
                }
            }
            .addOnFailureListener { exception ->
                Log.i(MAIN, "Error getting documents.", exception)
            }
    }
    //get a specific user in the database and add them to the list of users
    private fun getOneUser(findUser: String?) {
        if(findUser == null){ //if their is no user to be found
            //do nothing
        }
        else {
            db.collection("users")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        if (document.id == findUser) { //compare each user to the user we are trying to find
                            username = document.data["Nickname"].toString()
                            Log.i(MAIN, "${document.id} => ${username}")
                            listOfUsers.add(document.id) //add the one user
                            //get all the posts this user has
                            getPosts(document.data["Nickname"].toString(), document.id)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.i("Main", "Error getting documents.", exception)
                }
        }
    }
    //get all the posts from a certain hashtag
    private fun getHashtag() {
        //fromLists has a hashtag value that we just use in the path to find all the posts lists
        //under that hashtag
        db.collection("hashtags").document(fromLists!!).collection("posts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) { //for every post, get post image
                    var imageUri = getPostImage(document.data["imageurl"].toString())
                    //then add the post info from the database to the list of posts
                    val currentPost = Post(document.id, imageUri,
                        document.data["description"].toString(), document.data["publisher"].toString())
                    posts.add(currentPost)
                    Log.i(MAIN, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.i(MAIN, "Error getting documents.", exception)
            }
    }
    //gets the posts for every user in the list of users from the database
    private fun getPosts(username: String, docID: String) {
        db.collection("users")
            .document(docID)
            .collection("posts")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    //for every post, get the post image
                    var imageUri = getPostImage(document.data["imageurl"].toString())
                    //then add the post info from the database and the image to the list of posts
                    val currentPost = Post(document.id, imageUri,
                        document.data["description"].toString(), username)
                    posts.add(currentPost)
                    Log.i(MAIN, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { e ->
                Log.i(MAIN, "Error getting documents.", e)
            }
    }
    //gets a post's image from storage
    private fun getPostImage(postPath: String): Uri {
        //postPath is the path in storage
        val storageReference = FirebaseStorage.getInstance().reference
        val postImage = storageReference!!.child(postPath) //find the path in storage
        val localFile = File.createTempFile("images", "jpg") //create a temporary file for the image to go
        postImage.getFile(localFile) //get the photo in storage using the path
            .addOnSuccessListener {
                Log.i(MAIN, "Downloaded Photo")
                //load the view after photo downloaded
                val postAdapter = RecycleAdapter(posts, this@MainActivity)
                recycle?.setAdapter(postAdapter)
            }
            .addOnFailureListener{
                Log.i(MAIN, "Failed to get photo!")
            }
        return Uri.fromFile(localFile) //return the photo as URI
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == RESULT_FROM_USERS){ //coming back from userlist
            when(resultCode) {
                Activity.RESULT_OK -> {
                    //check from userslist if we want to go to hashtaglist
                    if(data?.getStringExtra("next")== "1") {
                        //get the clicked user
                        fromLists = data?.getStringExtra("user")
                        Log.i(MAIN, "User from list: $fromLists")
                        whatToPost = 1 //want to show only posts from this one user
                        refreshPage()
                    }
                    else {
                        //user wants to go to hashtag list
                        val intent = Intent(this@MainActivity, HashtagListActivity::class.java)
                        startActivityForResult(intent, RESULT_FROM_HASHTAGS)
                    }
                }
                Activity.RESULT_CANCELED ->
                    Log.i(MAIN, "user result canceled")
            }
        }
        else if(requestCode == RESULT_FROM_HASHTAGS) { //coming back from hashtag list
            when(resultCode) {
                Activity.RESULT_OK -> {
                    //check from hashtag list if we want to go to userlist
                    if(data?.getStringExtra("next")== "1") {
                        //get the clicked hashtag
                        fromLists = data?.getStringExtra("hashtag")
                        Log.i(MAIN, "Hashtag from list: $fromLists")
                        whatToPost = 2 //want to show only posts from this one hashtag
                        refreshPage()
                    }
                    else {
                        //user want to go to userlist
                        val intent = Intent(this@MainActivity, UserListActivity::class.java)
                        startActivityForResult(intent, RESULT_FROM_USERS)
                    }
                }
                Activity.RESULT_CANCELED ->
                    Log.i(MAIN, "hashtag result canceled")
            }
        }
        else {
            Log.i(MAIN, "Something went wrong")
            return
        }
    }
}