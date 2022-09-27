package bits.suman.instafire

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bits.suman.instafire.model.Post
import bits.suman.instafire.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.w3c.dom.Text

private const val TAG = "PostActivity"
const val EXTRA_USERNAME = "EXTRA_USERNAME"

open class PostActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb : FirebaseFirestore
    private lateinit var posts : MutableList<Post>
    private lateinit var adapter: PostAdapter

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        posts = mutableListOf()

        adapter = PostAdapter(this, posts)
        findViewById<RecyclerView>(R.id.rvPosts).adapter = adapter
        findViewById<RecyclerView>(R.id.rvPosts).layoutManager = LinearLayoutManager(this)
        firestoreDb = FirebaseFirestore.getInstance()

        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)
            .get()
            .addOnSuccessListener { userSnapshot ->
                    signedInUser  = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "username of current user: $signedInUser")
            }
            .addOnFailureListener{ exception ->
                Log.i(TAG, "Failed to get user", exception)
            }

        var postsReference = firestoreDb
            .collection("posts")
            .limit(20)
            .orderBy("creation_time", Query.Direction.DESCENDING)

        val username = intent.getStringExtra(EXTRA_USERNAME)
        if (username != null) {
            postsReference = postsReference.whereEqualTo("user.username", username)
        }

        postsReference.addSnapshotListener { snapshot, exception ->

            if (exception != null || snapshot == null) {
                Log.e(TAG, "Exception when querying post", exception)
                return@addSnapshotListener
            }

            val postList = snapshot.toObjects(Post::class.java)
            posts.clear()
            posts.addAll(postList)
            adapter.notifyDataSetChanged()
            for (post in postList) {
                Log.i(TAG, "post $post")
            }
        }

        findViewById<FloatingActionButton>(R.id.fabCreate).setOnClickListener {
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_posts, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.menu_profile) {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(EXTRA_USERNAME, signedInUser?.username)
            startActivity(intent)

        }
        return super.onOptionsItemSelected(item)
    }
}