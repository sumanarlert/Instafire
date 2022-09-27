package bits.suman.instafire

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import bits.suman.instafire.model.Post
import bits.suman.instafire.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

private const val REQUEST_CODE = 42
private const val FILE_NAME = "photo.jpg"
private const val TAG = "CreateActivity"

class CreateActivity : AppCompatActivity() {
    private var signedInUser: User? = null
    private lateinit var firestoreDb : FirebaseFirestore
    private lateinit var storageReference: StorageReference
    private lateinit var photoFile: File
    private lateinit var takenImage: Bitmap
    private lateinit var fileProvider: Uri
    private var imageExist: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        firestoreDb = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

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

        findViewById<Button>(R.id.btnClick).setOnClickListener{
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoFile = getPhotoFile(FILE_NAME)

            fileProvider = FileProvider.getUriForFile(this, "bits.suman.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

            if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_CODE)
            } else {
                Toast.makeText(this, "unable to open camera app", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            handleSubmitButton()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            val takenImage = data?.extras?.get("data") as Bitmap
            takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)
            findViewById<ImageView>(R.id.ivPreview).setImageBitmap(takenImage)
            imageExist = true
        } else {
            Log.i(TAG, "unable to get image")
            Toast.makeText(this, "unable to get image", Toast.LENGTH_SHORT).show()
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleSubmitButton() {
        if (!imageExist) {
            Toast.makeText(this, "Please click a photo first", Toast.LENGTH_LONG).show()
            return
        }
        if(findViewById<EditText>(R.id.etDescription).text.isBlank()) {
            Toast.makeText(this, "The Description cannot be empty", Toast.LENGTH_LONG).show()
            return
        }
        if(signedInUser == null){
            Toast.makeText(this, "User cannot be identified", Toast.LENGTH_SHORT).show()
            return
        }

        findViewById<Button>(R.id.btnSubmit).isEnabled = false

        val photoReference = storageReference.child("${System.currentTimeMillis()}-photo.jpg")
        photoReference.putFile(fileProvider)
            .continueWithTask { photoUploadTask ->
                Log.i(TAG, "upload bytes: ${photoUploadTask.result?.bytesTransferred}")
                photoReference.downloadUrl
            }.continueWithTask { downloadUrlTask ->
                val post = Post(
                    findViewById<EditText>(R.id.etDescription).text.toString(),
                    downloadUrlTask.result.toString(),
                    System.currentTimeMillis(),
                    signedInUser)
                firestoreDb.collection("posts").add(post)
            }.addOnCompleteListener { postCreationTask ->
                findViewById<Button>(R.id.btnSubmit).isEnabled = true
                if(!postCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception during Firebase operations", postCreationTask.exception)
                    Toast.makeText(this, "Failed to save post", Toast.LENGTH_SHORT).show()
                }
                findViewById<EditText>(R.id.etDescription).text.clear()
                findViewById<ImageView>(R.id.ivPreview).setImageResource(0)
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                val profileIntent = Intent(this, PostActivity::class.java)
                profileIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                startActivity(profileIntent)
                finish()
            }
    }

    private fun getPhotoFile(fileName: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }


}