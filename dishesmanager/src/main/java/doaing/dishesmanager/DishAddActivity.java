package doaing.dishesmanager;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.couchbase.lite.Blob;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.QueryChange;
import com.couchbase.lite.QueryChangeListener;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.jakewharton.rxbinding.view.RxView;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import doaing.dishesmanager.widget.DishesKindSpinner;
import doaing.dishesmanager.widget.TasteSelectAdapter;
import doaing.mylibrary.MyApplication;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.functions.Action1;
import tools.CDBHelper;
import tools.ToolUtil;

public class DishAddActivity extends AppCompatActivity {
    private Database database;
    private static final int THUMBNAIL_SIZE = 150;
    private Bitmap bitmap;
    private List<Document> list;
    String url = "https://www.yaodiandian.net/dishes/";
    private String newFileUrl;
    Toolbar toolbar;
    EditText disheName;
    EditText dishePriceEt;
    DishesKindSpinner disheKindSp;
    Button disheSubmitBt;

    ImageView disheIm;

    ImageView tasteImBt;
    MutableDocument disheDocument;
  //  MutableDocument dishesDoc;
    String[] strings;
    private int position = 0;
    private int sortNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dish_add);
        initView();
    }

    /**
     * 加载口味选择器
     */
    private void initTasteData() {

        list = new ArrayList<>();

        //初始化适配器
        final TasteSelectAdapter oap = new TasteSelectAdapter(list, getApplicationContext());
        final Database database = CDBHelper.getDatabase();

        final List<Document> tasteList = new ArrayList<>();
        Query query = QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(Expression.property("className").equalTo(Expression.string("Taste")));

        query.addChangeListener(new QueryChangeListener() {
            @Override
            public void changed(QueryChange change) {

                if (!tasteList.isEmpty()) {
                    tasteList.clear();
                }
                ResultSet rows = change.getResults();
                Result row;

                while ((row = rows.next()) != null) {

                    String id = row.getString(0);
                    Document doc = database.getDocument(id);
                    tasteList.add(doc);

                }
                strings = new String[tasteList.size()];
                for (int i = 0; i < strings.length; i++) {
                    strings[i] = tasteList.get(i).getString("name");
                }
            }
        });

        tasteImBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(DishAddActivity.this);
                builder.setMultiChoiceItems(strings, new boolean[strings.length], new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {

                            list.add(tasteList.get(which));

                        } else {

                            list.remove(tasteList.get(which));

                        }
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        oap.notifyDataSetChanged();

                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
            }
        });


        RecyclerView recyclerView = findViewById(R.id.reyview);

        //LinearLayoutMannager 是一个布局排列 ， 管理的接口,子类都都需要按照接口的规范来实现。
        LinearLayoutManager ms = new LinearLayoutManager(this);

        // 设置 recyclerview 布局方式为横向布局
        ms.setOrientation(LinearLayoutManager.HORIZONTAL);

        //给RecyClerView 添加设置好的布局样式
        recyclerView.setLayoutManager(ms);

        //对 recyclerview 添加数据内容
        recyclerView.setAdapter(oap);
    }

    private void initView() {
        database = CDBHelper.getDatabase();

        disheDocument = new MutableDocument("Dish." + ToolUtil.getUUID());
        disheDocument.setString("dataType", "BaseData");
        disheDocument.setString("channelId", ((MyApplication) getApplication()).getCompany_ID());
        disheDocument.setString("className", "Dish");


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        disheName = findViewById(R.id.dishe_name);

        dishePriceEt = findViewById(R.id.dishe_price_et);

        disheKindSp = findViewById(R.id.disheKind_sp);

        disheSubmitBt = findViewById(R.id.dishe_submit_bt);

        disheIm = findViewById(R.id.dishe_im);

        tasteImBt = findViewById(R.id.taste_im_bt);
        //初始化口味
        initTasteData();

        int p = getIntent().getIntExtra("kindPosition", 0);

        disheKindSp.setSelection(p);

        disheDocument.setString("kindId",disheKindSp.getDishesKindList().get(p).getId());

        disheKindSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int p, long id) {

                position = p;
                //KindId保存到菜品中
               List<Document> documents = CDBHelper.getDocmentsByWhere(
                        Expression.property("className").equalTo(Expression.string("Dish"))
                                .and(Expression.property("kindId").equalTo(Expression.string(disheKindSp.getDishesKindList().get(p).getId())))
                        , null);
                sortNum = documents.size();
                //默认依次添加到队尾
                disheDocument.setInt("sortNum", sortNum);

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        RxView.clicks(disheIm).throttleFirst(300, TimeUnit.MILLISECONDS).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1);

            }
        });

        //提交菜品所有信息
        RxView.clicks(disheSubmitBt).throttleFirst(300, TimeUnit.MILLISECONDS).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                if ("".equals(disheName.getText().toString())) {

                    disheName.setError("菜品名称不能为空");

                    return;

                } else {

                    String dishesName = disheName.getText().toString();
                    disheDocument.setString("name", dishesName);

                    String dishesNameCode26 = ToolUtil.getFirstSpell(dishesName);
                    disheDocument.setString("code26", dishesNameCode26);

                    assert dishesNameCode26 != null;
                    String dishesNameCode9 = ToolUtil.ChangeSZ(dishesNameCode26);
                    disheDocument.setString("code9", dishesNameCode9);

                }
                if ("".equals(dishePriceEt.getText().toString())) {

                    dishePriceEt.setError("价格不能为空");

                    return;

                } else {

                    disheDocument.setFloat("price", Float.valueOf(dishePriceEt.getText().toString()));
                }

                //判断菜类是否存在
                if (disheKindSp.getDishesKindList().isEmpty()) {

                    Toast.makeText(DishAddActivity.this, "请先添加菜类信息！", Toast.LENGTH_LONG).show();

                    return;
                }

                //附加图片到Docment，允许图片为空
                disheDocument = attachImage(disheDocument, bitmap);
                disheDocument.setString("kindId", disheKindSp.getDishesKindList().get(disheKindSp.getSelectedItemPosition()).getId());

                Log.e("DOAING","保存时的 kind id"+disheKindSp.getDishesKindList().get(disheKindSp.getSelectedItemPosition()).getId());

                //添加口味
                MutableArray array = new MutableArray();
                if (list.size() > 0) {

                    for (int i = 0; i < list.size(); i++) {

                        array.addString(list.get(i).getId());
                    }

                }
                disheDocument.setArray("tasteIds", array);
               // disheDocument.setInt("sortNum", sortNum);
                try {
                    database.save(disheDocument);
                } catch (CouchbaseLiteException e) {
                    e.printStackTrace();
                }




                EventBus.getDefault().postSticky(position);
                //提交静态图片
                if (newFileUrl != null && !newFileUrl.isEmpty()) {
                    upDataPicture(new File(newFileUrl));
                }
                finish();

            }
        });
    }

    /**
     * document附加图片上去
     */

    private MutableDocument attachImage(MutableDocument task, Bitmap image) {

        if (image != null) {
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(image, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            Blob blob = new Blob("image/jpg", in);
            task.setBlob("image", blob);

        }
        return task;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            ContentResolver cr = this.getContentResolver();
            try {

                //获取文件的绝对路径以及文件的名字
                String name = getRealPathFromURI(uri);
                newFileUrl = name;

                String[] a = name.split("/");
                name = a[a.length - 1];
                int idx = name.indexOf(".");
                name = name.substring(0, idx);

                //显示图片名字
                disheName.setText(name);
                //显示图片
                Glide.with(this).load(uri).into(disheIm);

                //获取附加到Document的数据
                if (uri != null) {
                    bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                }


            } catch (FileNotFoundException e) {
                Log.e("Exception", e.getMessage(), e);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 获取图片的绝对路径
     *
     * @param contentUri 文件相对路径
     * @return 绝对路径
     */

    private String getRealPathFromURI(Uri contentUri) {
        String[] prob = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(DishAddActivity.this, contentUri, prob, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(columnIndex);
    }

    /**
     * 上传图片静态资源
     *
     * @param file 菜品图片文件
     */

    public void upDataPicture(File file) {

        final boolean[] flag = {false};
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(url)
                .build();
        FileAddService service = retrofit.create(FileAddService.class);
        RequestBody requestFile =
                RequestBody.create(MediaType.parse("application/otcet-stream"), file);

        MultipartBody.Part body = MultipartBody.Part.createFormData("aFile", file.getName(), requestFile);

        String descriptionString = disheDocument.getId();
        RequestBody description =
                RequestBody.create(MediaType.parse("multipart/form-data"), descriptionString);
        Call<ResponseBody> call = service.upload(description, body);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                flag[0] = response.isSuccessful();

                if (flag[0]) {
                    Toast.makeText(DishAddActivity.this, "图片上传成功！", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(DishAddActivity.this, "图片上传失败！", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DishAddActivity.this, "请求访问失败！", Toast.LENGTH_LONG).show();

            }
        });

    }
}
