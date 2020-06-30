package com.example.android.radiancex

import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class DailyTrainingActivity() : AppCompatActivity() {
    var jaSentence: TextView? = null
    var translation: TextView? = null
    var hint: TextView? = null
    var id: TextView? = null
    var totalNumberOfCards: TextView? = null
    var btnLoadFile: Button? = null
    var btnGetNewCollection: Button? = null
    var btnNextWord: Button? = null
    var switchShowJA: Switch? = null
    var switchShowTranslation: Switch? = null
    var switchShowHint: Switch? = null
    var mDiEntryViewModel: DiEntryViewModel? = null
    var progressDialog: ProgressDialog? = null
    var handler: Handler? = null
    private var currentDeck: ArrayList<DiEntry>? = null
    private var currentSentence: DiEntry? = null
    private var entryCount = 0
    val DECK_SIZE = 20
    val ID_FIELD_CODE = 0
    val JAPANESE_FIELD_CODE = 1
    val VIETNAMESE_FIELD_CODE = 2
    val NOTE_FIELD_CODE = 3

    companion object {
        init {
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLInputFactory",
                    "com.fasterxml.aalto.stax.InputFactoryImpl"
            )
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                    "com.fasterxml.aalto.stax.OutputFactoryImpl"
            )
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLEventFactory",
                    "com.fasterxml.aalto.stax.EventFactoryImpl"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_training)
        val mToolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(mToolbar)
        supportActionBar!!.setTitle("Daily Training")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        jaSentence = findViewById(R.id.jaSentence)
        translation = findViewById(R.id.translation)
        hint = findViewById(R.id.hint)
        id = findViewById(R.id.id)
        totalNumberOfCards = findViewById(R.id.totalNumberOfCards)
        btnLoadFile = findViewById(R.id.loadFile)
        btnGetNewCollection = findViewById(R.id.getNewCollection)
        btnNextWord = findViewById(R.id.nextWord)
        switchShowJA = findViewById(R.id.showJAswitch)
        switchShowTranslation = findViewById(R.id.showTranslationSwitch)
        switchShowHint = findViewById(R.id.showHintSwitch)
        handler = Handler()
        mDiEntryViewModel = ViewModelProvider(this).get(DiEntryViewModel::class.java)
        currentDeck = ArrayList()
        initializeData()
        mDiEntryViewModel!!.allEntries.observe(this, androidx.lifecycle.Observer<List<DiEntry?>?> { sentences ->
            totalNumberOfCards.setText(sentences!!.size.toString() + "")
            generateNewDeck()
            goToNextWord()
        })
        btnLoadFile.setOnClickListener(View.OnClickListener { v: View? ->
            btnLoadFile.setEnabled(false)
            btnGetNewCollection.setEnabled(true)
            btnNextWord.setEnabled(true)
            try {
                populateDatabaseFromFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })
        btnGetNewCollection.setOnClickListener(View.OnClickListener { v: View? ->
            generateNewDeck()
            Toast.makeText(this, "New deck generated", Toast.LENGTH_SHORT).show()
        })
        btnNextWord.setOnClickListener(View.OnClickListener { v: View? -> goToNextWord() })
        switchShowJA.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentSentence != null) {
                jaSentence.setText(if ((switchShowJA.isChecked())) currentSentence!!.jpn else "")
            }
        })
        switchShowTranslation.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentSentence != null) {
                translation.setText(if ((switchShowTranslation.isChecked())) currentSentence!!.vie else "")
            }
        })
        switchShowHint.setOnClickListener(View.OnClickListener { v: View? ->
            if (currentSentence != null) {
                hint.setText(if ((switchShowHint.isChecked())) currentSentence!!.meaning else "")
            }
        })
    }

    private fun initializeData() {
        progressDialog = ProgressDialog(this)
        progressDialog!!.setMessage("Initializing")
        progressDialog!!.setCancelable(false)
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog!!.show()
        Thread(Runnable {
            entryCount = mDiEntryViewModel!!.numberOfEntriesSynchronous
            handler!!.post(Runnable { progressDialog!!.dismiss() })
            if (entryCount == 0) {
                handler!!.post(Runnable { Toast.makeText(this, "Database empty", Toast.LENGTH_LONG).show() })
                try {
                    populateDatabaseFromFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                btnLoadFile!!.setEnabled(true)
                btnGetNewCollection!!.setEnabled(false)
                btnNextWord!!.setEnabled(false)
            } else {
                btnLoadFile!!.setEnabled(false)
                btnGetNewCollection!!.setEnabled(true)
                btnNextWord!!.setEnabled(true)
            }
            generateNewDeck()
        }).start()
    }

    @Throws(IOException::class)
    fun populateDatabaseFromFile() {
        handler!!.post({
            progressDialog = ProgressDialog(this)
            progressDialog!!.setCancelable(false)
            progressDialog!!.setMessage("Populating database")
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.show()
        })
        try {
            BufferedReader(InputStreamReader(this.assets.open("BST Câu.tsv"), StandardCharsets.UTF_8)).use { bufferedReader ->
                var line: String
                var fields: Array<String>
                var id: String
                var japanese: String
                var meaning: String?
                var english: String?
                var vietnamese: String
                var note: String
                var count: Int = 0
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    fields = line.split("\t".toRegex()).toTypedArray()
                    id = if (fields.size >= 1) fields.get(ID_FIELD_CODE) else ""
                    japanese = if (fields.size >= 2) fields.get(JAPANESE_FIELD_CODE) else ""
                    vietnamese = if (fields.size >= 3) fields.get(VIETNAMESE_FIELD_CODE) else ""
                    note = if (fields.size >= 4) fields.get(NOTE_FIELD_CODE) else ""
                    mDiEntryViewModel!!.insert(DiEntry(id, japanese, "", "", vietnamese, note))
                    Log.d("Fields ----", id + "|" + japanese + "|" + vietnamese + "|" + note)
                    //                    mDiEntryViewModel.insert(new DiEntry(count + "", "", "", "", "", ""));
//                    Log.e("Entry count", "finished, count: " + mDiEntryViewModel.getNumberOfEntriesSynchronous());
                    count++
                    Thread.sleep(3)
                }
                entryCount = mDiEntryViewModel!!.numberOfEntriesSynchronous
                handler!!.post(Runnable {
                    progressDialog!!.dismiss()
                    Toast.makeText(this, entryCount.toString() + " entries imported", Toast.LENGTH_SHORT).show()
                })
                Thread(Runnable { Log.d("Database population - ", "Finished, count: " + entryCount) }).start()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun generateNewDeck() {
        currentDeck!!.clear()
        handler!!.post({
            progressDialog = ProgressDialog(this)
            progressDialog!!.setCancelable(false)
            progressDialog!!.setMessage("Generating new deck")
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.show()
        })
        Thread(Runnable {
            for (i in 0 until DECK_SIZE) {
                currentDeck!!.add(mDiEntryViewModel!!.findDiEntryByIdSynchronous((Math.random() * entryCount) as Int.toString() + ""))
            }
            handler!!.post(Runnable { progressDialog!!.dismiss() })
        }).start()
    }

    fun goToNextWord() {
        if (currentDeck != null && currentDeck!!.size != 0) {
            if (currentSentence != null) {
                while (true) {
                    val newSentenceId = (Math.random() * currentDeck!!.size).toInt()
                    if (currentDeck!!.get(newSentenceId).id != currentSentence!!.id) break
                }
            }
            currentSentence = currentDeck!![(Math.random() * currentDeck!!.size).toInt()]
            jaSentence!!.text = if ((switchShowJA!!.isChecked)) currentSentence!!.jpn else ""
            translation!!.text = if ((switchShowTranslation!!.isChecked)) currentSentence!!.vie else ""
            hint!!.text = if ((switchShowHint!!.isChecked)) currentSentence!!.note else ""
            id!!.text = currentSentence!!.id
        }
    }
}