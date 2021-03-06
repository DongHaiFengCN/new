package doaing.dishesmanager;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.couchbase.lite.Blob;
import com.couchbase.lite.Database;
import com.couchbase.lite.Dictionary;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import doaing.dishesmanager.adapter.DishesKindAdapter;
import tools.CDBHelper;
import view.BaseToobarActivity;

/**
 * @author donghaifeng
 */
public class DishesActivity extends BaseToobarActivity {

    @BindView(R2.id.toolbar)
    Toolbar toolbar;
    @BindView(R2.id.fab)
    FloatingActionButton fab;

    @BindView(R2.id.dishes_lv)
    ListView dishesLv;

    @BindView(R2.id.disheskind_lv)
    ListView dishekindLv;

    private DishesKindAdapter dishesKindAdapter;

    private DishesAdapter dishesAdapter;
    private int dishePosition = 0;

    private int kindPosition = 0;
    private Database database;

    @Override
    protected int setMyContentView() {
        return R.layout.activity_dishes;
    }


    @Override
    public void initData(final Intent intent) {
        setToolbarName("菜品管理");
        EventBus.getDefault().register(this);

        database = CDBHelper.getDatabase();
        initkindList();

        dishesLv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                //0 .停止滚动 1.用户使用触摸屏滚动，手指仍在屏幕上 2.用户以前使用触摸滚动，并进行了一次投掷。动画现在滑行到停止。
                //  MyLog.e(scrollState+"");

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                //上滑动隐藏 &&下滑动显示
                if (dishePosition < firstVisibleItem) {

                    fab.hide();

                } else if (dishePosition > firstVisibleItem) {
                    fab.show();
                }

                dishePosition = firstVisibleItem;

            }
        });

        dishesLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Document dishes = dishesAdapter.getList().get(position);

                Intent intent1 = new Intent(DishesActivity.this, DisheEditActivity.class);
                intent1.putExtra("dishes", dishes.getId());
                intent1.putExtra("position", kindPosition);
                startActivity(intent1);

            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DishesActivity.this, DishAddActivity.class);
                intent.putExtra("kindPosition", kindPosition);
                startActivity(intent);
            }
        });
    }

    @Override
    protected Toolbar setToolBarInfo() {
        return toolbar;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    public class DishesAdapter extends BaseAdapter {

        private List<Document> list;

        public List<Document> getList() {
            if (list != null) {
                return list;
            }
            return null;
        }

        public void setList(String kindId) {

            list = CDBHelper.getDocmentsByWhere(
                    Expression.property("className").equalTo(Expression.string("Dish"))
                            .and(Expression.property("kindId").equalTo(Expression.string(kindId)))
                    , null);
             notifyDataSetChanged();

        }

        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater;

            layoutInflater = LayoutInflater.from(DishesActivity.this);

            ViewHolder viewHolder;

            if (convertView == null) {

                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.dishe_list_item, null);
                viewHolder.disheIm = convertView.findViewById(R.id.dishe_im);
                viewHolder.dishenameTv = convertView.findViewById(R.id.dishe_tv);
                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();

            }

            viewHolder.dishenameTv.setText(list.get(position).getString("name"));
            Blob taskBlob = list.get(position).getBlob("image");

            if (taskBlob != null) {

                Glide.with(DishesActivity.this).load(taskBlob.getContent()).into(viewHolder.disheIm);

            } else {
                Glide.with(DishesActivity.this).load(R.mipmap.ic_launcher).into(viewHolder.disheIm);
            }
            Dictionary attachments = list.get(position).getDictionary("_attachments");
            if (attachments != null) {
                Blob blob = attachments.getBlob("blob_1");
                Glide.with(DishesActivity.this).load(blob.getContent()).into(viewHolder.disheIm);
            }
            return convertView;
        }

        class ViewHolder {
            TextView dishenameTv;
            ImageView disheIm;

        }
    }

    private void initkindList() {

        database = CDBHelper.getDatabase();

        //默认绑定dishesKindAdapter
        dishesKindAdapter = new DishesKindAdapter(this, database);
        dishekindLv.setAdapter(dishesKindAdapter);

        //默认绑定dishes
        dishesAdapter = new DishesAdapter();
        dishesLv.setAdapter(dishesAdapter);

        //kind的点击事件
        dishekindLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (dishesKindAdapter.getNames().size() > 0 && position < dishesKindAdapter.getNames().size()) {

                    dishesKindAdapter.changeSelected(position);

                    kindPosition = position;

                    dishesAdapter.setList(dishesKindAdapter.getNames().get(position));

                } else if (dishesKindAdapter.getNames().size() == 0) {

                    dishesAdapter.notifyDataSetChanged();
                }

            }
        });

        //默认选中第一项
        updatePosition(0);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void updatePosition(Integer integer) {
        if (dishesKindAdapter.getNames().size() == 0) {

            Toast.makeText(DishesActivity.this, "请添加菜类！", Toast.LENGTH_SHORT).show();

            fab.hide();
        }
        if (!fab.isShown()) {
            fab.show();
        }


        //刷新菜品信息

        dishekindLv.performItemClick(dishekindLv.getChildAt(integer), integer, dishekindLv
                .getItemIdAtPosition(integer));
    }

}
