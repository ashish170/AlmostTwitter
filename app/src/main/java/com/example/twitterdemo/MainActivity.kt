package com.example.twitterdemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets.view.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    var database = FirebaseDatabase.getInstance()
    var myRef = database.reference

    var tweetlist=ArrayList<ticket>()
    var  adapter:tweetAdapter?=null
    var email:String?=null
    var userid:String?=null
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var b: Bundle? =intent!!.extras
        email=b!!.getString("email")
        userid=b!!.getString("uid")


        tweetlist.add(ticket("x1","hi","url","ME"))

        adapter=tweetAdapter(this,tweetlist)
        list.adapter=adapter
        loadPost()
    }

    inner class tweetAdapter:BaseAdapter
    {   var listTweets=ArrayList<ticket>()
        var context:Context?=null
        constructor(context: Context,listTweets:ArrayList<ticket>):super()
        {
            this.listTweets=listTweets
            this.context=context
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var mytweet=listTweets[p0]
            if(mytweet.tweetPersonId.equals("ME"))
            {
                var myview= layoutInflater.inflate(R.layout.add_ticket,null)
                myview.attachview.setOnClickListener(View.OnClickListener {
                    loadimage()
                })
                myview.sendview.setOnClickListener(View.OnClickListener {
                  myRef.child("posts").push().setValue(postinfo(userid!!,myview!!.post.text.toString(),down!!))
                    myview.post.setText(" ")
                })
                return myview
            }
//            else if(mytweet.tweetPersonId.equals("loading"))
//            {
//                var myview= layoutInflater.inflate(R.layout.loading,null)
//                return myview
//
//            }
            else
            {
                var myview= layoutInflater.inflate(R.layout.tweets,null)
                myview.tweetText.text=mytweet.tweetText

                //myview.tweetpic.setImageURI(mytweet!!.imageUrl)
                Picasso.get().load(mytweet.imageUrl.toString()).into(myview.tweetpic)
                myRef.child("users").child(mytweet.tweetPersonId!!).addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        try {
                            var td=p0!!.value as HashMap<String,Any>
                            for (key in td.keys)
                            {
                                var userinfo=td[key] as  String
                                if(key.equals("ProfileImage"))
                                {
                                    Picasso.get().load(userinfo).into(myview.imagepath)
                                }
                                else
                                {
                                    myview.tvname.setText(userinfo)
                                }

                            }
                            //adapter!!.notifyDataSetChanged()
                        }
                        catch (ex:Exception)
                        {

                        }
                    }


                })
                return myview
            }
        }

        override fun getItem(p0: Int): Any {
            return listTweets[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return listTweets.size
        }

    }
    val pickimg = 234
    fun loadimage() {
        var intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intent, pickimg)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pickimg && data != null && resultCode == Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data)
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
           uploadImage(BitmapFactory.decodeFile(piturePath))


        }
    }
    var down:String?=null
    fun uploadImage(bitmap:Bitmap)
    {
//        tweetlist.add(0, ticket("0","him","url","loading"))
//        adapter!!.notifyDataSetChanged()
        val storage= FirebaseStorage.getInstance()
        val storageref=storage.getReferenceFromUrl("gs://twitterdemo-730e8.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj= Date()
        val imagPath=split(email!!)+"."+ df.format(dataobj)+".jpg"
        val imgRef=storageref.child("imagespost/"+imagPath)
        val baos= ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data=baos.toByteArray()
        val upload=imgRef.putBytes(data)
        upload.addOnFailureListener{
            Toast.makeText(applicationContext,"UPLOAD FAILED", Toast.LENGTH_LONG).show()

        }.addOnSuccessListener {
                taskSnapshot->  down= taskSnapshot.storage.downloadUrl!!.toString()
//            tweetlist.removeAt(0)
            adapter!!.notifyDataSetChanged()
        }


    }
    fun split(email:String):String
    {
        val s=email.split("@")
        return s[0]
    }
    
    fun loadPost()
    {
        myRef.child("posts").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                try {
                        tweetlist.clear()
                        tweetlist.add(ticket("x1","hi","url","ME"))
//                        tweetlist.add(ticket("x2","him","url","Me"))
                        var td=p0!!.value as HashMap<String,Any>
                    for (key in td.keys)
                    {
                        var post=td[key] as  HashMap<String,Any>
                        tweetlist.add(ticket(key,
                            post["text"] as String,
                            post["postimage"] as String,
                            post["userid"] as String))

                    }
                    adapter!!.notifyDataSetChanged()
                }
                catch (ex:Exception)
                {

                }
            }


        })
    }


}


