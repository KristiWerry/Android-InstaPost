package kristi.cs646_assignment4

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_sign_up.*

private const val SIGNUP = "SignUp"
class SignUpActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null //authentication
    private val password:String = "password" //everyone's password is password

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance() //set authentication instance
        var fireDB = FirebaseFirestore.getInstance() //get the firestore instance

        enterSignUpInfo.setOnClickListener{ //when the signup button is pushed
            val username = nameEditText.text.toString() //get the email, name, and nickname
            val nickname = nicknameEditText.text.toString()
            val email = emailSignUp.text.toString()
            if(username.isEmpty() || nickname.isEmpty() ||
                email.isEmpty()) {
                Log.i(SIGNUP, "Not all sign up fields are filled")
                Toast.makeText(this, "All fields required!", Toast.LENGTH_SHORT)
            }
            else {
                Log.i(SIGNUP, "Going to register")
                register(username, nickname, email, fireDB)
            }
        }
    }
    private fun register(username:String, nickname:String, email:String, fireDB: FirebaseFirestore) {
        //try to create the account with given information
        this.auth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task: Task<AuthResult> ->
                if(task.isSuccessful) {
                    Log.i(SIGNUP, "Created User")
                    val userID = auth!!.currentUser!!.uid
                    //create a hashmap woth the user's information
                    val map = HashMap<String, String>()
                    map["Name"] = username
                    map["Nickname"] = nickname
                    map["email"] = email
                    //enter the hashmap into the firestore database
                    fireDB.collection("users").document(userID)
                        .set(map)
                        .addOnSuccessListener { Log.i(SIGNUP, "User added to Document") }
                        .addOnFailureListener{e -> Log.i(SIGNUP, ""+e.message)}
                    //grant access to the main activity
                    val intent = Intent(this@SignUpActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
                else {
                    //report to the user if something went wrong and stay in current activity
                    Log.i(SIGNUP, "Creating User Failed")
                    Toast.makeText(this, "Could not Sign Up", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
