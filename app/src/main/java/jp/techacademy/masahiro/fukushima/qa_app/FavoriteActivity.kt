package jp.techacademy.masahiro.fukushima.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.database.FirebaseDatabase
import android.widget.ListView
import com.google.firebase.database.DatabaseReference

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import android.util.Base64
import com.google.firebase.database.DatabaseError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FavoriteActivity : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mListView: ListView
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private var mGenre = 0
    private lateinit var mFavoriteRef: DatabaseReference
    private lateinit var mQuestion: Question

    private val mQuestionEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""

            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                mGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }

        override fun onCancelled(p0: DatabaseError) {
        }
    }

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {

            val dataBaseReference = FirebaseDatabase.getInstance().reference

            val map = dataSnapshot.value as Map<String, String>
            val genre = map["genre"] ?: ""

            val questionId = dataSnapshot.key

            val mQuestionRef = dataBaseReference.child(ContentsPATH).child(genre).child(questionId.toString())
            //mQuestionRef.addChildEventListener(mQuestionEventListener)
            mQuestionRef.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // snapshot.valueをMap<* ,*>?にキャスト
                    val data = snapshot.value as Map<* ,*>?
                    // nameに紐づくvalueを取得し、String型にキャスト
                    //saveName(data!![NameKEY] as String)


                    val map = snapshot.value as Map<String, String>

                    val title = map["title"] ?: ""
                    val body = map["body"] ?: ""
                    val name = map["name"] ?: ""
                    val uid = map["uid"] ?: ""
                    val imageString = map["image"] ?: ""

                    val bytes =
                        if (imageString.isNotEmpty()) {
                            Base64.decode(imageString, Base64.DEFAULT)
                        } else {
                            byteArrayOf()
                        }

                    val answerArrayList = ArrayList<Answer>()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            answerArrayList.add(answer)
                        }
                    }

                    val question = Question(title, body, name, uid, snapshot.key ?: "",
                        genre.toInt(), bytes, answerArrayList)
                    mQuestionArrayList.add(question)
                    mAdapter.notifyDataSetChanged()
                }

                // 読み取りがキャンセルされた場合に読み出される
                override fun onCancelled(firebaseError: DatabaseError) {}
            })
        }

    override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
    }

    override fun onChildRemoved(p0: DataSnapshot) {
    }

    override fun onChildMoved(p0: DataSnapshot, p1: String?) {
    }

    override fun onCancelled(p0: DatabaseError) {
    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // UIの初期設定
        title = "お気に入り"

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mListView = findViewById(R.id.favorite_listView)
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        mListView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            intent.putExtra("genre", mGenre)
            startActivity(intent)
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val id = FirebaseAuth.getInstance().currentUser!!.uid
            val dataBaseReference = FirebaseDatabase.getInstance().reference
            mFavoriteRef =
                dataBaseReference.child(FavoritesPATH).child(id)
            mFavoriteRef.addChildEventListener(mEventListener)
        }
    }
}
