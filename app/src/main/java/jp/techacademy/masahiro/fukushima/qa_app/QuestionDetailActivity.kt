package jp.techacademy.masahiro.fukushima.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.google.firebase.FirebaseError

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private var mGenre: Int = 0
    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private var favoriteStatus= 0

    private lateinit var mFavoriteRef: DatabaseReference

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mFavoriteEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            favoriteStatus = 1
            favorite_button.setBackgroundResource(R.drawable.favorite_on)
        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // ログインしていなければお気に入りボタンを消す
            favorite_button.setVisibility(View.INVISIBLE)
        }

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // 渡ってきたジャンルの番号を保持する
        mGenre = extras.getInt("genre")

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        favorite_button.setOnClickListener {
            if (favoriteStatus == 0) {
                favoriteStatus = 1
                favorite_button.setBackgroundResource(R.drawable.favorite_on)
                // ログイン済みのユーザーを取得する
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // ログインしていればお気に入りのデータベース領域を作成
                    val id = FirebaseAuth.getInstance().currentUser!!.uid
                    val dataBaseReference = FirebaseDatabase.getInstance().reference
                    val favoriteRef = dataBaseReference.child(FavoritesPATH).child(id).child(mQuestion.questionUid)
                    val data = HashMap<String, String>()
                    data["genre"] = mGenre.toString()
                    favoriteRef.setValue(data)
                }
            } else {
                favoriteStatus = 0
                favorite_button.setBackgroundResource(R.drawable.favorite_off)
                // ログイン済みのユーザーを取得する
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    val dataBaseReference = FirebaseDatabase.getInstance().reference
                    val id = FirebaseAuth.getInstance().currentUser!!.uid
                    val favoriteRef = dataBaseReference.child(FavoritesPATH).child(id).child(mQuestion.questionUid)
                    favoriteRef.removeValue()
                }
            }
        }

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        if (user != null) {
            val id = FirebaseAuth.getInstance().currentUser!!.uid
            mFavoriteRef =
                dataBaseReference.child(FavoritesPATH).child(id).child(mQuestion.questionUid)
            mFavoriteRef.addChildEventListener(mFavoriteEventListener)
        }

    }

    // ログイン画面から戻ってきた場合にお気に入りボタンを表示させる
    override fun onRestart() {
        super.onRestart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            favorite_button.setVisibility(View.VISIBLE)
            if (favoriteStatus == 0) {
                favorite_button.setBackgroundResource(R.drawable.favorite_on)
            } else {
                favorite_button.setBackgroundResource(R.drawable.favorite_off)
            }
        }
    }
}

