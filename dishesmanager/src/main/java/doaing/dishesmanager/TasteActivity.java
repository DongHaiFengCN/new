package doaing.dishesmanager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.LiveQueryChange;
import com.couchbase.lite.LiveQueryChangeListener;
import com.couchbase.lite.Query;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import doaing.dishesmanager.view.MySwipeListLayout;

import module.MyApplication;
import tools.MyLog;
import tools.ToolUtil;
import view.BaseToobarActivity;

/**
 * @author donghaifeng
 * @Data 2018/1/18
 */
public class TasteActivity extends BaseToobarActivity {

    /**
     * 修改为Document
     */
    private List<Document> list = new ArrayList<>();

    private ListAdapter listAdapter;
    private Database database;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.taste_lv)
    ListView taste_lv;

    @Override
    protected int setMyContentView() {
        return R.layout.activity_taste;
    }

    @Override
    protected Toolbar setToolBarInfo() {
        return toolbar;
    }

    @Override
    public void initData(Intent intent) {

        initList();


    }

    /**
     * 加载数据库数据
     */
    private void initList() {

        listAdapter = new ListAdapter();
        taste_lv.setAdapter(listAdapter);

        database = ((MyApplication) getApplicationContext()).getDatabase();


        LiveQuery query = Query.select(SelectResult.expression(Expression.meta().getId()))
                .from(DataSource.database(database))
                .where(Expression.property("className").equalTo("DishesTasteC")).toLive();

        query.addChangeListener(new LiveQueryChangeListener() {
            @Override
            public void changed(LiveQueryChange change) {

                if (!list.isEmpty()) {
                    list.clear();
                }
                ResultSet rows = change.getRows();
                Result row = null;
                while ((row = rows.next()) != null) {


                    String id = row.getString(0);
                    Document doc = database.getDocument(id);
                    list.add(doc);

                }


                listAdapter.notifyDataSetChanged();
            }
        });

        query.run();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.taste, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) searchItem.getActionView();
        final SearchView.SearchAutoComplete mSearchAutoComplete = mSearchView.findViewById(R.id.search_src_text);
        ImageView searchButton = mSearchView.findViewById(R.id.search_button);
        searchButton.setImageResource(R.mipmap.icon_add);
        mSearchAutoComplete.setBackgroundColor(ContextCompat.getColor(this,R.color.md_white));
        //设置Hint文字颜色
        mSearchAutoComplete.setHintTextColor(ContextCompat.getColor(this,android.R.color.darker_gray));
        mSearchView.setQueryHint("添加口味");
        //设置输入文字颜色
        mSearchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.md_blue_grey_700));
        //设置是否显示搜索框展开时的提交按钮
        mSearchView.setSubmitButtonEnabled(false);
        mSearchAutoComplete.setImeOptions(EditorInfo.IME_ACTION_SEND);
        mSearchAutoComplete.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        mSearchAutoComplete.setSingleLine(true);
        mSearchAutoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {

                    //1.添加数据到数据库

                    Document document = new Document("DishesTasteC."+ ToolUtil.getUUID());
                    document.setString("channelId", "gysz");
                    document.setString("className", "DishesTasteC");
                    document.setString("tasteName", mSearchAutoComplete.getText().toString());
                    try {
                        database.save(document);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }

                    mSearchAutoComplete.setText("");


                }


                return false;
            }
        });
        return true;
    }


    class ListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int arg0) {
            return list.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(final int arg0, View view, ViewGroup arg2) {
            if (view == null) {
                view = LayoutInflater.from(TasteActivity.this).inflate(
                        R.layout.slip_list_item, null);
            }
            final TextView tv_name = view.findViewById(R.id.tv_name);
            tv_name.setText(list.get(arg0).getString("tasteName"));
            final MySwipeListLayout sll_main = view.findViewById(R.id.sll_main);
            TextView tv_edit = view.findViewById(R.id.tv_edit);
            TextView tv_delete = view.findViewById(R.id.tv_delete);

            tv_edit.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    final EditText editText = new EditText(TasteActivity.this);

                    new AlertDialog.Builder(TasteActivity.this).setTitle("修改口味名称")
                            .setView(editText)
                            .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    Document document = list.get(arg0);

                                    document.setString("tasteName",editText.getText().toString());

                                }
                            })
                            .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .show();


                    sll_main.setStatus(MySwipeListLayout.Status.Close, true);


                    notifyDataSetChanged();
                }
            });
            tv_delete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {

                    new AlertDialog.Builder(TasteActivity.this).setTitle("删除口味")
                            .setMessage(tv_name.getText().toString())
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    sll_main.setStatus(MySwipeListLayout.Status.Close, true);
                                    try {
                                        database.delete(list.get(arg0));
                                    } catch (CouchbaseLiteException e) {
                                        e.printStackTrace();
                                    }

                                }
                            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();

                }
            });
            return view;
        }

    }


}
