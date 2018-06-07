package szebra.senshu_timetable.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import szebra.senshu_timetable.R;
import szebra.senshu_timetable.models.Lecture;
import szebra.senshu_timetable.models.ToDo;
import szebra.senshu_timetable.views.MyDatePicker;

public class ToDoEditActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
  private Realm mRealm;
  private Lecture lecture;
  private ToDo todo;
  private EditText titleBox, deadlineBox, detailBox;
  private Spinner lectureSpinner;
  private DateFormat dateFormat;
  
  private Calendar deadline;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_to_do_edit);
    
    //Realmの初期化
    mRealm = Realm.getDefaultInstance();
    
    //UIパーツたち
    Toolbar toolbar = findViewById(R.id.toolbar_edit_todo);
    titleBox = findViewById(R.id.edit_todo_title);
    deadlineBox = findViewById(R.id.edit_todo_deadline);
    detailBox = findViewById(R.id.edit_todo_detail);
    lectureSpinner = findViewById(R.id.edit_todo_lecture);
    
    //UI初期化
    setTitle(R.string.title_activity_todo_add);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
    prepareLectureSpinner();
    deadlineBox.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        askDeadline();
      }
    });
    
    //日付(Deadline)周り
    deadline = Calendar.getInstance();
    dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    
    Intent intent = getIntent();
    
    //Lectureの取得
    int lectureId = intent.getIntExtra("Lecture", -1);
    try {
      if (lectureId != -1) {
        lecture = mRealm.where(Lecture.class).equalTo("id", lectureId).findFirst();
      }
    } catch (NullPointerException e) {
      Toast.makeText(this, "エラー: 授業IDが無効です", Toast.LENGTH_LONG).show();
    }
    
    
    int todoId = intent.getIntExtra("ToDo", -1);
    //新規作成
    if (todoId == -1) {
      int lastId = 0;
      try {
        lastId = mRealm.where(ToDo.class).findAllSorted("id", Sort.DESCENDING).first().getId();
      } catch (IndexOutOfBoundsException e) {
        Log.i("ToDoEditActivity", "Todoがありません");
      }
      todo = new ToDo();
      todo.setId(lastId + 1);
      todo.setLectureId(lectureId);
      deadlineBox.setText(dateFormat.format(Calendar.getInstance().getTime()));
    } else { //編集
      todo = mRealm.where(ToDo.class).equalTo("id", todoId).findFirst();
      if (todo == null) {
        Toast.makeText(this, "エラー: ToDo IDが無効です", Toast.LENGTH_LONG).show();
      } else {
        deadline.setTime(todo.getDeadline());
        titleBox.setText(todo.getTitle());
        deadlineBox.setText(dateFormat.format(deadline));
        detailBox.setText(todo.getDetailText());
      }
    }
  }
  
  public void prepareLectureSpinner() {
    RealmResults<Lecture> results = mRealm.where(Lecture.class).findAllSorted("id");
    Lecture[] lectures = new Lecture[results.size()];
    results.toArray(lectures);
    ArrayAdapter<Lecture> adapter = new ArrayAdapter<Lecture>(this, android.R.layout.simple_spinner_item, lectures);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    lectureSpinner.setAdapter(adapter);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        break;
      case R.id.menu_add:
        mRealm.beginTransaction();
        modifyToDoInstance();
        mRealm.copyToRealmOrUpdate(todo);
        mRealm.commitTransaction();
        finish();
        break;
    }
    return super.onOptionsItemSelected(item);
  }
  
  public void modifyToDoInstance() {
    todo.setTitle(titleBox.getText().toString());
    todo.setLectureId(((Lecture) lectureSpinner.getSelectedItem()).getId());
    todo.setDetailText(detailBox.getText().toString());
    todo.setDeadline(deadline.getTime());
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.finish_button, menu);
    return true;
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    mRealm.close();
  }
  
  @Override
  public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
    deadline.set(year, month, dayOfMonth);
    Log.d("onDateSet", String.valueOf(deadline.get(Calendar.YEAR)));
    Log.d("onDateSet", dateFormat.format(deadline.getTime()));
    deadlineBox.setText(SimpleDateFormat.getDateInstance(SimpleDateFormat.DEFAULT).format(deadline.getTime()));
  }
  
  public void askDeadline() {
    DialogFragment DPFragment = new MyDatePicker();
    DPFragment.show(getSupportFragmentManager(), "Select Deadline");
  }
}
