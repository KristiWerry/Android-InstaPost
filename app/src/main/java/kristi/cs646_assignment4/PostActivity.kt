package kristi.cs646_assignment4

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_post.*
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.IOException


private const val POST = "Post"
class PostActivity : AppCompatActivity() {
    private var imageUri: Uri? = null //the image URI
    private var storageReference: StorageReference? = null //storage reference
    private val GALLERY = 123 //request code for when we open the gallery (or whatever) to get a photo
    private var username: String? = null //current user's username

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        close.setOnClickListener {
            finish() //if close click, go back
        }
        postIt.setOnClickListener {
            if(imageUri != null) {
                uploadImage() //upload the image and information
                finish() //then return to the previous activity
            }
            else {
                Toast.makeText(this, "No Photo Chosen", Toast.LENGTH_SHORT).show()
            }
        }
        choosePhotoButton.setOnClickListener {
            chooseImageFromGallery() //if they want to get a different picture, start gallery intent again
        }
        getUsername() //get current user's username
        chooseImageFromGallery() //get a picture
    }

    private fun chooseImageFromGallery() {
        //get a picture from starting an intent to find a photo located on the photo storage to upload
        val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY)
    }

    private fun getUsername() {
        //we get the username first so we can save the username in the hashtag database
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get() //get all the list of users
            .addOnSuccessListener { result ->
                for (document in result) { //search though to find the current user
                    if (document.id == FirebaseAuth.getInstance().currentUser!!.uid) {
                        //and set the username to the given nickname
                        username = document.data["Nickname"].toString()
                        Log.i(POST, "${document.id} => ${document.data}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.i(POST, "Error getting documents.", exception)
            }
    }

    private fun uploadImage() {
        //get the storage path which is posts/[current user uid]/[time in millisecond].[jpg, gif, etc]
        var photoPath = "posts/${FirebaseAuth.getInstance().currentUser!!.uid}/${System.currentTimeMillis().toString()+"."+getFileExtension(imageUri!!)}"
        storageReference = FirebaseStorage.getInstance().reference.child(photoPath)
        if(imageUri != null){ //make sure we have a selected image
            storageReference!!.putFile(imageUri!!) //then try to upload the photo
                .addOnSuccessListener { task -> //let the user know the photo was uploaded
                    Log.i(POST, "Photo Uploaded")
                    Toast.makeText(this, "Photo Uploaded", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{ e -> //let user know that the photo was not uploaded
                    Log.i(POST, "Photo NOT Uploaded")
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener{ task ->
                    if(task.isSuccessful) { //check if the photo was uploaded successfully
                        //val downloadUri = task.result
                        val fireDB = FirebaseFirestore.getInstance()
                        //put the information into a hashmap (image storage path, description, ans the publisher)
                        val hashmap = HashMap<String, String>()
                        hashmap.put("imageurl", photoPath)
                        hashmap.put("description", description.text.toString())
                        hashmap.put("publisher", FirebaseAuth.getInstance().currentUser!!.uid)
                        //add the hashmap into the firestore database under users/current user's uid/posts
                        fireDB.collection("users").document(FirebaseAuth.getInstance().currentUser!!.uid)
                            .collection("posts")
                            .add(hashmap)
                            .addOnSuccessListener { ref ->
                                Log.i(POST, "Post id ${ref.id}")
                                val postId = ref.id //get the post id
                                //also add the information to the database under hashtags
                                findHashtags(description.text.toString(), username!!, photoPath, postId)
                            }

                    }
                    else { //if photo not uploaded successfully
                        Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        else {
            //no photo
            Toast.makeText(this, "No Photo", Toast.LENGTH_SHORT).show()
        }
    }

    //find all the hashtags in the description and add the post information in to the database under the hashtag
    private fun findHashtags(description: String, publisher: String, iUrl: String, id:String) {
        val pattern = "#([A-Za-z0-9_-]+)".toRegex() //regular expression to find hashtags
        val found = pattern.findAll(description) //match the pattern against the post description
        val notDuplicates: MutableList<String> = mutableListOf()
        //list of hashtags in the post to make sure we don't have duplicate hashtags in one description
        found.forEach { f -> //then for each hashtag found
            if(f.toString() !in notDuplicates) { //make sure we have not already seen it
                notDuplicates += f.toString() //add the hashtag to the list of seen hashtags
                //put the hashtag into the firestore database with the collection name as the hashtag
                //along with the field hashtag = the hashtag found
                //we need to do this or we can't call just the collection names (ie we crash)
                //if the hashtag is already there, then we just update the field
                val fireDB = FirebaseFirestore.getInstance()
                val map = HashMap<String, String>()
                map.put("hashname", f.value)
                fireDB.collection("hashtags").document(f.value)
                    .set(map)
                    .addOnCompleteListener { Log.i(POST, "Hashtag $f des")}
                    .addOnFailureListener{ Log.i(POST, "Hashtag failed")}

                //make a hashmap for the information of the post with the storage image path, description,
                //and the username previously gotten
                val hashmap = HashMap<String, String>()
                hashmap.put("imageurl", iUrl)
                hashmap.put("description", description)
                hashmap.put("publisher", publisher)
                //and add all this information to the firestore database under the path
                //hashtags/[the current found hashtag]/posts/[post id]
                fireDB.collection("hashtags").document(f.value)
                    .collection("posts")
                    .document(id)
                    .set(hashmap)
                    .addOnSuccessListener { ref -> Log.i(POST, "Hashtag $f added") }
                    .addOnFailureListener{ Log.i(POST, "Hashtag failed to add") }
            }
            else {
                //do nothing //skip duplicate
            }
        }
    }
    //this function gets the end of the file extension like jpg, gif, etc...
    private fun getFileExtension(uri: Uri): String {
        val contentResolver = contentResolver
        val mime = MimeTypeMap.getSingleton()
        return mime.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == GALLERY) {
            if(data != null) { //check if we got data
                imageUri = data.data //set the image uri to the selected photo from the phone storage
                try{
                    //load the image into the activity to show to the user
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,imageUri)
                    Toast.makeText(this, "Got Image", Toast.LENGTH_SHORT).show()
                    image_add!!.setImageBitmap(bitmap)
                }
                catch (e:IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to Get Image", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                return
            }
        }
        else {
            return
        }
    }
}
