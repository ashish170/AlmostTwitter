package com.example.twitterdemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*


class Login : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    var database = FirebaseDatabase.getInstance()
    var myRef = database.reference
   // private var mStorageRef: StorageReference?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()
        //mStorageRef = FirebaseStorage.getInstance().getReference();
        personicon.setOnClickListener(View.OnClickListener {
            checkpermission()

        })

    }
    val readimg: Int = 234
    fun checkpermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    readimg
                )
                return
            }
        }
        loadimage()
    }
    val pickimg = 234
    fun loadimage() {
        var intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent, pickimg)
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            readimg -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    loadimage()
                else
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickimg && data != null && resultCode == Activity.RESULT_OK) {
            val selectedimage = data.data
            val filepatch = arrayOf(MediaStore.Images.Media.DATA)
            val cursor =
                selectedimage?.let { contentResolver.query(it, filepatch, null, null, null) }
            if (cursor != null) {
                cursor.moveToFirst()
            }
            val Colindex = cursor!!.getColumnIndex(filepatch[0])
            val piturePath = cursor.getString(Colindex)
            cursor.close()
            personicon.setImageBitmap(BitmapFactory.decodeFile(piturePath))


        }
    }
    fun loginfire(email: String, pass: String) {
        mAuth!!.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this)
            { task ->
                if (task.isSuccessful) {
                    Toast.makeText(applicationContext,"SUCCESFUL",Toast.LENGTH_LONG).show()
                    saveFire()
                } else
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()

            }

    }
    fun saveFire()
    {
        var cur=mAuth!!.currentUser
        val email=cur!!.email.toString()
        val storage= FirebaseStorage.getInstance()
        val storageref=storage.getReferenceFromUrl("gs://twitterdemo-730e8.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj= Date()
        val imagPath=split(email)+ df.format(dataobj)+".jpg"
        val imgRef=storageref.child("images/"+imagPath)

        personicon.isDrawingCacheEnabled=true
        personicon.buildDrawingCache()

        val draw=personicon.drawable as BitmapDrawable
        val bitmap=draw.bitmap
        val baos= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data=baos.toByteArray()
        val upload=imgRef.putBytes(data)
        upload.addOnFailureListener{
            Toast.makeText(applicationContext,"UPLOAD FAILED",Toast.LENGTH_LONG).show()

        }.addOnSuccessListener {
            taskSnapshot-> var down= taskSnapshot.storage.downloadUrl.toString()
            myRef.child("users").child(cur.uid).child("email").setValue(cur.email)
            myRef.child("users").child(cur.uid).child("ProfileImage").setValue(down)
            loadtweet()
        }


    }
    fun loadtweet()
    {
        var cur=mAuth!!.currentUser
        if(cur!=null)
        {
            var intent=Intent(this,MainActivity:: class.java)
            intent.putExtra("email", cur.email)
            intent.putExtra("uid", cur.uid)

            startActivity(intent)
        }
    }
    override fun onStart() {
        super.onStart()
        loadtweet()
    }

    fun buLogin(view:View)
    {
        loginfire(email.text.toString(),password.text.toString())
    }
    fun split(email:String):String
    {
        val s=email.split("@")
        return s[0]
    }
}