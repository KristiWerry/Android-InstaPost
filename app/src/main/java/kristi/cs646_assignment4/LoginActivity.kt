package kristi.cs646_assignment4

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

private const val LOGIN = "Login"
class LoginActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private val password:String = "password" //everyone's password is password

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance() //get authentication instance
        enterLogInInfo.setOnClickListener{ login()} //if login button pushed go to login function
    }

    private fun login() {
        val email = emailLogIn.text.toString() //get the email from editTextView
        if(TextUtils.isEmpty(email)) { //make sure we have an email
            Toast.makeText(this, "Must enter an email", Toast.LENGTH_SHORT)
        }
        else {
            Log.i(LOGIN, "Logging in User")
            auth!!.signInWithEmailAndPassword(email, password) //try to login with give email
                .addOnCompleteListener(this) {task -> //if login was successful, go to main activity
                    if(task.isSuccessful) {
                        Log.i(LOGIN, "Login Success")
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    }
                    else { //if login was unsuccessfull, report to user and say in same activity
                        Log.i(LOGIN, "Login Unsuccessful")
                        Toast.makeText(this, "Login Unsuccessful", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
