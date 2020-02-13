package kristi.cs646_assignment4

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

private const val START = "Start"
class StartActivity : AppCompatActivity() {
    //The beginning Activity in the app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //layout is called main
        //MainActivity's layout is activity_home

        //has two buttons which take the user either to either sign up or login
        signUpButton.setOnClickListener{
            Log.i(START, "Sign In Button Pushed")
            val intent = Intent(this@StartActivity, SignUpActivity::class.java)
            startActivity(intent)
        }
        loginButton.setOnClickListener{
            Log.i(START, "Login Button Pushed")
            val intent = Intent(this@StartActivity, LoginActivity::class.java)
            startActivity(intent)
        }

    }

}


