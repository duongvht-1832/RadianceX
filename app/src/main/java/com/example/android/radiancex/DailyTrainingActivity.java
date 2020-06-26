package com.example.android.radiancex;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DailyTrainingActivity extends AppCompatActivity {

    TextView jaSentence;
    TextView translation;
    TextView hint;
    TextView id;
    TextView totalNumberOfCards;
    Button btnLoadFile;
    Button btnGetNewCollection;
    Button btnNextWord;
    Switch switchShowJA;
    Switch switchShowTranslation;
    Switch switchShowHint;

    DiEntryViewModel mDiEntryViewModel;

    ProgressDialog progressDialog;
    Handler handler;

    private ArrayList<DiEntry> currentDeck;
    private DiEntry currentSentence;
    private int entryCount;

    final int DECK_SIZE = 20;
    final int ID_FIELD_CODE = 0;
    final int JAPANESE_FIELD_CODE = 1;
    final int VIETNAMESE_FIELD_CODE = 2;
    final int NOTE_FIELD_CODE = 3;


    static {
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLInputFactory",
                "com.fasterxml.aalto.stax.InputFactoryImpl"
        );
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                "com.fasterxml.aalto.stax.OutputFactoryImpl"
        );
        System.setProperty(
                "org.apache.poi.javax.xml.stream.XMLEventFactory",
                "com.fasterxml.aalto.stax.EventFactoryImpl"
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_training);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Daily Training");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        jaSentence = findViewById(R.id.jaSentence);
        translation = findViewById(R.id.translation);
        hint = findViewById(R.id.hint);
        id = findViewById(R.id.id);
        totalNumberOfCards = findViewById(R.id.totalNumberOfCards);
        btnLoadFile = findViewById(R.id.loadFile);
        btnGetNewCollection = findViewById(R.id.getNewCollection);
        btnNextWord = findViewById(R.id.nextWord);
        switchShowJA = findViewById(R.id.showJAswitch);
        switchShowTranslation = findViewById(R.id.showTranslationSwitch);
        switchShowHint = findViewById(R.id.showHintSwitch);

        handler = new Handler();
        mDiEntryViewModel = new ViewModelProvider(this).get(DiEntryViewModel.class);

        currentDeck = new ArrayList<>();

        initializeData();

        mDiEntryViewModel.getAllEntries().observe(this, new Observer<List<DiEntry>>() {
            @Override
            public void onChanged(@Nullable final List<DiEntry> sentences) {
                totalNumberOfCards.setText(sentences.size() + "");
                generateNewDeck();
                goToNextWord();
            }
        });

        Observable<DiEntry> currentDeckObservable = Observable.fromIterable(currentDeck);
        currentDeckObservable.subscribeOn(Schedulers.newThread())
                .subscribe(new DisposableObserver<DiEntry>() {

                    @Override
                    public void onNext(DiEntry t) {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onComplete() {
                        goToNextWord();
                    }
                });

        btnLoadFile.setOnClickListener(v -> {
            btnLoadFile.setEnabled(false);
            btnGetNewCollection.setEnabled(true);
            btnNextWord.setEnabled(true);
            try {
                populateDatabaseFromFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
//            generateNewDeck();
//            goToNextWord();
        });

        btnGetNewCollection.setOnClickListener(v -> {
            generateNewDeck();
        });

        btnNextWord.setOnClickListener(v -> goToNextWord());

        switchShowJA.setOnClickListener(v -> {
            if (currentSentence != null) {
                jaSentence.setText((switchShowJA.isChecked()) ? currentSentence.getJpn() : "");
            }
        });

        switchShowTranslation.setOnClickListener(v -> {
            if (currentSentence != null) {
                translation.setText((switchShowTranslation.isChecked()) ? currentSentence.getVie() : "");
            }
        });

        switchShowHint.setOnClickListener(v -> {
            if (currentSentence != null) {
                hint.setText((switchShowHint.isChecked()) ? currentSentence.getMeaning() : "");
            }
        });

    }

    private void initializeData() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Initializing");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new Thread(() -> {
            entryCount = mDiEntryViewModel.getNumberOfEntriesSynchronous();
            handler.post(() -> {
                progressDialog.dismiss();
            });
            if (entryCount == 0) {
                handler.post(() -> {
                    Toast.makeText(this, "Database empty", Toast.LENGTH_LONG).show();
                });
                try {
                    populateDatabaseFromFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                btnLoadFile.setEnabled(true);
                btnGetNewCollection.setEnabled(false);
                btnNextWord.setEnabled(false);
            } else {
                btnLoadFile.setEnabled(false);
                btnGetNewCollection.setEnabled(true);
                btnNextWord.setEnabled(true);
            }
            generateNewDeck();
        }).start();
    }


    public void populateDatabaseFromFile() throws IOException {
        handler.post(() -> {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Populating database");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        });
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.getAssets().open("BST Câu.tsv"), StandardCharsets.UTF_8))) {
            String line;
            String[] fields;
            String id;
            String japanese;
            String meaning;
            String english;
            String vietnamese;
            String note;
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {
                fields = line.split("\t");
                id = fields.length >= 1 ? fields[ID_FIELD_CODE] : "";
                japanese = fields.length >= 2 ? fields[JAPANESE_FIELD_CODE] : "";
                vietnamese = fields.length >= 3 ? fields[VIETNAMESE_FIELD_CODE] : "";
                note = fields.length >= 4 ? fields[NOTE_FIELD_CODE] : "";
                mDiEntryViewModel.insert(new DiEntry(id, japanese, "", "", vietnamese, note));
                Log.d("Fields ----", id + "|" + japanese + "|" + vietnamese + "|" + note);
//                    mDiEntryViewModel.insert(new DiEntry(count + "", "", "", "", "", ""));
//                    Log.e("Entry count", "finished, count: " + mDiEntryViewModel.getNumberOfEntriesSynchronous());
                count++;
                Thread.sleep(3);
            }
            entryCount = mDiEntryViewModel.getNumberOfEntriesSynchronous();
            handler.post(() -> {
                progressDialog.dismiss();
                Toast.makeText(this, entryCount + " entries imported", Toast.LENGTH_SHORT).show();
            });
            new Thread(() -> {
                Log.d("Database population - ", "Finished, count: " + entryCount);
            }).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void generateNewDeck() {
        currentDeck.clear();
        new Thread(() -> {
            for (int i = 0; i < DECK_SIZE; i++) {
                currentDeck.add(mDiEntryViewModel.findDiEntryByIdSynchronous((int) (Math.random() * entryCount) + ""));
            }
        }).start();
    }

    public void goToNextWord() {
        if (currentDeck != null && currentDeck.size() != 0) {
            currentSentence = currentDeck.get((int) (Math.random() * currentDeck.size()));
            jaSentence.setText((switchShowJA.isChecked()) ? currentSentence.getJpn() : "");
            translation.setText((switchShowTranslation.isChecked()) ? currentSentence.getVie() : "");
            hint.setText((switchShowHint.isChecked()) ? currentSentence.getNote() : "");
            id.setText(currentSentence.getId());
        }
    }


}