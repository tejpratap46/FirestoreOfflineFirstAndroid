package com.tejpratapsingh.firestoreofflinefirstandroid

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.tejpratapsingh.firestoreofflinefirst.firestore.FirestoreOfflineManager
import com.tejpratapsingh.firestoreofflinefirst.firestore.FirestoreSyncManager
import com.tejpratapsingh.firestoreofflinefirstandroid.memory.UserTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val auth: FirebaseAuth = Firebase.auth

        auth.signInWithEmailAndPassword("tejpratap46@gmail.com", "9876543210")
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    if (user == null) {
                        return@addOnCompleteListener
                    }

                    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

                    // Initialise offline store
                    FirestoreOfflineManager.getInstance()
                        .initialise(applicationContext, firestore, user.uid)

                    FirestoreOfflineManager.getInstance().addData(
                        "user",
                        user.uid,
                        UserTable(
                            userId = user.uid,
                            name = user.displayName ?: "NO NAME",
                            phone = "9876543210"
                        )
                    )

                    val coroutineScope = CoroutineScope(Dispatchers.Default)
                    coroutineScope.async {
                        FirestoreSyncManager.getInstance()
                            .startSync(applicationContext, firestore, user.uid)
                    }

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}