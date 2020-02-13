package kristi.cs646_assignment4

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_hashtag_list.*

private const val HASH = "HashtagList"
class HashtagListActivity : AppCompatActivity() {
    private var storageReference: StorageReference? = null //storage reference
    private val db = FirebaseFirestore.getInstance() //databade reference
    private var listOfHashtags = arrayListOf<String>() //list of all the hashtags in the database
    private var recycle:RecyclerView? = null// = findViewById(R.id.recyclerViewUsers) as RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hashtag_list)
        postButtonHashtag.setOnClickListener{
            //if in this activity and the user wants to post something
            //go to the post activity
            val intent = Intent(this@HashtagListActivity, PostActivity::class.java)
            startActivity(intent)
        }
        listUsersHashtag.setOnClickListener {
            //put extra go to user activity
            Log.i(HASH, "Going to UserList")
            val activityToBack = intent
            //next is checked in main activity which will redirect to user list
            activityToBack.putExtra("next", "user")
            setResult(Activity.RESULT_OK, activityToBack)
            finish()
        }
        listHashtagsHashtag.setOnClickListener {
            refreshPage() //sets up the recyclerview and the information in it
            //refreshpage will call the database and storage and show all the results
        }
        refreshButtonHashtag.setOnClickListener {
            refreshPage()
        }
        //check if we have internet connection
        val connMgr = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            refreshPage()//we want to show to view when we first get to the page
        } else {
            // display error
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshPage() {
        Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show()
        listOfHashtags.clear() //since this function can get called multiple times, we need to clear the
        //old list so we don't show repeat posts
        getHashtag() //gets all the hashtags in the database and stores them in a list
        storageReference = FirebaseStorage.getInstance().reference
        //set the recyclerview in the layout
        recycle = findViewById(R.id.recyclerViewHashtag) as RecyclerView
        val layout = LinearLayoutManager(this@HashtagListActivity)
        recycle?.setLayoutManager(layout)
    }

    private inner class ViewHolder(itemView: View, private val context: HashtagListActivity) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val hashtagText: TextView //the textview that will show the hashtag
        private var hashtag: String? = null //the string that will be in the textview
        init {
            itemView.setOnClickListener(this)
            hashtagText = itemView.findViewById(R.id.userText) as TextView
        }
        fun bindPost(location: Int, hashtag: String) { //for every hashtag in the database, put the text in textview
            this.hashtag = hashtag
            hashtagText.text = hashtag
        }
        override fun onClick(source: View) {
            //if an item is clicked on, take that item and send it back to main activity
            //also put in next as 1 which gets checked in main
            Log.i(HASH, "${this.hashtag}")
            val hashtagToBack = intent
            hashtagToBack.putExtra("hashtag", this.hashtag)
            hashtagToBack.putExtra("next", "1")
            setResult(Activity.RESULT_OK, hashtagToBack)
            finish()
        }
    }
    private inner class RecycleAdapter(private val hashtags: List<String>,
                                       private val context: HashtagListActivity) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            //inflating the correct view
            val layoutInflater = LayoutInflater.from(this@HashtagListActivity)
            val view = layoutInflater.inflate(R.layout.see_list_item, parent, false)
            return ViewHolder(view, context)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //for every hashtag in the list of hashtags, we get the position and bind it into a list item
            val curPost = hashtags[position]
            holder.bindPost(position, curPost)
        }
        override fun getItemCount(): Int {
            return hashtags.size
        }
    }
    private fun getHashtag() {
        //getting all the hashtags from the firestore database and adding them to the list of hashtags
        db.collection("hashtags")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    listOfHashtags.add(document.id)
                    Log.i(HASH, "${document.id}")
                }
                //load the view
                val postAdapter = RecycleAdapter(listOfHashtags, this@HashtagListActivity)
                recycle?.setAdapter(postAdapter)
            }
            .addOnFailureListener { exception ->
                Log.i(HASH, "Error getting documents.", exception)
            }
    }
}