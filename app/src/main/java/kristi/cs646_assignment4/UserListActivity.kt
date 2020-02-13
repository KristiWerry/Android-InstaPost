package kristi.cs646_assignment4

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
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
import kotlinx.android.synthetic.main.activity_user_list.*

private const val USER = "UserList"
class UserListActivity : AppCompatActivity() {
    private var storageReference: StorageReference? = null //storage reference
    private val db = FirebaseFirestore.getInstance() //database reference
    private var listOfUsernames = arrayListOf<String>() //list of usernames/nicknames
    private var listOfUsers = arrayListOf<String>() //list of user id
    //set the recyclerview in the layout
    private var recycle:RecyclerView? = null// = findViewById(R.id.recyclerViewUsers) as RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        //set the recyclerview in the layout
        recycle = findViewById(R.id.recyclerViewUsers) as RecyclerView

        postButtonUsers.setOnClickListener{
            //if in this activity and the user wants to post something
            //go to the post activity
            val intent = Intent(this@UserListActivity, PostActivity::class.java)
            startActivity(intent)
        }
        listUsersUsers.setOnClickListener {
            //sets up the recyclerview and the information in it
            //refreshpage will call the database and storage and show all the results
            refreshPage()
        }
        listHashtagsUsers.setOnClickListener {
            //put extra go to hashtag activity
            Log.i(USER, "Going to HashtagList")
            val activityToBack = intent
            activityToBack.putExtra("next", "hashtag")
            //next is checked in main activity which will redirect to user list
            setResult(Activity.RESULT_OK, activityToBack)
            finish()
        }
        refreshButtonUsers.setOnClickListener {
            refreshPage()
        }
        Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show()
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
        listOfUsernames.clear() //since this function can get called multiple times, we need to clear the
        //old list so we don't show repeat posts
        getUsers() //gets the list of users in the database
        storageReference = FirebaseStorage.getInstance().reference
        //set the recyclerview in the layout
        recycle = findViewById(R.id.recyclerViewUsers) as RecyclerView
        val layout = LinearLayoutManager(this@UserListActivity)
        recycle?.setLayoutManager(layout)
    }

    private inner class ViewHolder(itemView: View, private val context: UserListActivity) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val userText: TextView //textview that will show the username/nickname
        private var user: String? = null //the string that will be in the textview //username/nickname
        private var userID:String? = null //the string that will be sent back to main activity //user id
        init {
            itemView.setOnClickListener(this)
            userText = itemView.findViewById(R.id.userText) as TextView
        }
        fun bindPost(location: Int, curUser: String, curID:String) {
            //gets called for every user in the list and sets the text and info
            this.user = curUser //setting the current username/nickname to this textview spot
            this.userID = curID //setting the current user ID to this textview spot
            userText.text = curUser //setting the textview to show the current username/nickname
        }
        override fun onClick(source: View) {
            //if an item is clicked on, take that item and send it back to main activity
            //also put in next as 1 which gets checked in main
            Log.i(USER, "ID:${this.userID} name:${this.user}")
            val userToBack = intent
            userToBack.putExtra("user", this.userID)
            userToBack.putExtra("next", "1")
            setResult(Activity.RESULT_OK, userToBack)
            finish()
        }
    }
    private inner class RecycleAdapter(private val users: List<String>, private val userID:List<String>,
                                       private val context: UserListActivity) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            //inflating the correct view
            val layoutInflater = LayoutInflater.from(this@UserListActivity)
            val view = layoutInflater.inflate(R.layout.see_list_item, parent, false)
            return ViewHolder(view, context)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            //for every user in the list of users, we get the position and bind it into a list item
            val curPost = users[position]
            val curID = userID[position]
            holder.bindPost(position, curPost, curID)
        }
        override fun getItemCount(): Int {
            return users.size
        }
    }
    private fun getUsers() {
        //getting all the users from the firestore database and adding them to the list of users
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    listOfUsers.add(document.id)
                    listOfUsernames.add(document.data["Nickname"].toString())
                    Log.i(USER, "Here: ${document.id}")
                }
                //load the view
                val postAdapter = RecycleAdapter(listOfUsernames, listOfUsers, this@UserListActivity)
                recycle?.setAdapter(postAdapter)
            }
            .addOnFailureListener { exception ->
                Log.i(USER, "Error getting documents.", exception)
            }
    }
}
