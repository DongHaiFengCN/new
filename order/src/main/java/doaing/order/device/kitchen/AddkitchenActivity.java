package doaing.order.device.kitchen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.couchbase.lite.Array;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.MutableArray;
import com.couchbase.lite.MutableDocument;
import com.gprinter.aidl.GpService;
import com.gprinter.command.GpCom;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.save.PortParamDataBase;
import com.gprinter.service.GpPrintService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bean.kitchenmanage.dishes.DishesKindC;
import bean.kitchenmanage.kitchen.KitchenClientC;
import doaing.mylibrary.MyApplication;
import doaing.order.R;
import doaing.order.device.PortConfigurationActivity;
import doaing.order.untils.GlobalConstant;
import doaing.order.view.DeskActivity;
import tools.CDBHelper;
import tools.MyLog;
import view.BaseToobarActivity;
import doaing.order.device.kitchen.AddKitchenAdapter.ViewHolder;

import static doaing.order.device.ListViewAdapter.MESSAGE_CONNECT;
import static com.gprinter.command.GpCom.ACTION_CONNECT_STATUS;
/*
*
 * Created by lenovo on 2018/2/1.

*/


public class AddkitchenActivity extends BaseToobarActivity implements View.OnClickListener{
    ImageView addclientBtBack;
    EditText etClientname;
    Button spPrinter;
    ListView listViewCk;
    Toolbar toolbar;
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;

    private List<String> allDishKindIdList;
    private List<String> listSelectedDocId;
    private MyApplication myapp;
    private int position = 0;
    private AddKitchenAdapter addKitchenAdapter;
    private String  selectDocId ;
    //private static int MAX_PRINTER_CNTMY = 1;//默认为1台
    private PortParameters mPortParam;
    private int mPrinterId = 0;
    private GpService mGpService = null;
    private static final int INTENT_PORT_SETTINGS = 0;
    List<String> infomations;
    @Override
    protected int setMyContentView() {
        return R.layout.activity_addkitchen;
    }

    @Override
    public void initData(Intent intent) {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        myapp = (MyApplication) getApplication();
        if (mGpService == null){
            mGpService = DeskActivity.getmGpService();
        }
        mPortParam = new PortParameters();
        listSelectedDocId = new ArrayList<>();
        initView();

        infomations = (List<String>) getIntent().getSerializableExtra("infomations");
        if (infomations != null)//添加进来
        {
            addKitchenData();
        }
        else //编辑进来
        {

            editKitchenData();
        }
    }

    private void hideNavigationBar() {

        int systemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        // Navigation bar hiding:  Backwards compatible to ICS.

        if (Build.VERSION.SDK_INT >= 14) {

            systemUiVisibility ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        }

        // 全屏展示

        if (Build.VERSION.SDK_INT >= 16) {

            systemUiVisibility ^= View.SYSTEM_UI_FLAG_FULLSCREEN;

        }

        if (Build.VERSION.SDK_INT >= 18) {

            systemUiVisibility ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        }
        getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);

    }

    private void addKitchenData()
    {
        if (infomations.size() == 0){

            mPrinterId = 0;
        }else {
            Document doc = CDBHelper.getDocByID(getApplicationContext(),infomations.get(infomations.size()-1));
            int pos = doc.getInt("indexPrinter");
            mPrinterId = pos+1;//下一个打印机id

        }
    }
    private void editKitchenData()
    {
        Intent intent1=getIntent();
        mPrinterId=intent1.getIntExtra("indexPrinter",-1);//
        String strname=intent1.getStringExtra("clientName");//厨房名字
        selectDocId=intent1.getStringExtra("docId");

        etClientname.setText(strname);
        PortParamDataBase database = new PortParamDataBase(this);
        mPortParam = database.queryPortParamDataBase("" +mPrinterId);

        try {
            if (mGpService.getPrinterConnectStatus(mPrinterId) == GpDevice.STATE_CONNECTED)
            {
                spPrinter.setText("连接成功");
                mPortParam.setPortOpenState(true);
            }else {
                spPrinter.setText("点击配置");
                mPortParam.setPortOpenState(false);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Initcpplfromdata(selectDocId);
    }
    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        etClientname = findViewById(R.id.et_clientname);
        spPrinter = findViewById(R.id.spPrinter);
        listViewCk = findViewById(R.id.listView_ck);
        setToolbarName("添加厨房打印机");
        findViewById(R.id.selectall).setOnClickListener(this);
        findViewById(R.id.bt_deselectall).setOnClickListener(this);
        findViewById(R.id.btn_kcsave).setOnClickListener(this);
        spPrinter.setOnClickListener(this);

        allDishKindIdList = CDBHelper.getIdsByClass(null,DishesKindC.class);
        addKitchenAdapter = new AddKitchenAdapter(AddkitchenActivity.this, allDishKindIdList);
        listViewCk.setAdapter(addKitchenAdapter);
        listViewCk.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Toast.makeText(AddkitchenActivity.this,""+position,Toast.LENGTH_SHORT).show();
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                viewHolder.itemAddkitchenCb.toggle();
                // 将CheckBox的选中状况记录下来
                addKitchenAdapter.getIsSelected().put(position, viewHolder.itemAddkitchenCb.isChecked());
                addKitchenAdapter.notifyDataSetChanged();
            }
        });


    }

    @Override
    protected Toolbar setToolBarInfo() {
        return toolbar;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            return true;

        }
        return false;

    }

    public void onClick(View view) {
        int i1 = view.getId();
        if (i1 == R.id.selectall) {
            for (int i = 0; i < allDishKindIdList.size(); i++) {
                AddKitchenAdapter.isSelected.put(i, true);
            }
            addKitchenAdapter.notifyDataSetChanged();

        } else if (i1 == R.id.bt_deselectall) {
            for (int i = 0; i < allDishKindIdList.size(); i++) {
                if (addKitchenAdapter.getIsSelected().get(i)) {
                    addKitchenAdapter.getIsSelected().put(i, false);
                } else {
                    addKitchenAdapter.getIsSelected().put(i, true);
                }
            }
            addKitchenAdapter.notifyDataSetChanged();

        }  else if (i1 == R.id.btn_kcsave) {
            if (isNull(selectDocId)) {
                IpCheck();
            } else {
                UpDataIP(selectDocId);
            }


        } else if (i1 == R.id.spPrinter) {
            //dialog();
            Intent intent = new Intent(AddkitchenActivity.this, PortConfigurationActivity.class);
            startActivityForResult(intent, INTENT_PORT_SETTINGS);

        }
    }
    private boolean isNull(String str)
    {
        return str == null || "".equals(str);
    }

    private void IpCheck()
    {

        if (CheckPortParamters(mPortParam))
        {
            getcheckinfo();
            if(listSelectedDocId.size()<=0)
            {
                Toast.makeText(this, "请选择要接收的菜品类",Toast.LENGTH_LONG).show();
            }
            else
            {
                savecheckinfo();
                Intent intent = new Intent();
                //设置返回数据
                this.setResult(2015, intent);
                this.finish();
            }
        }
        else
        {
            Toast.makeText(this, "请选择打印机",Toast.LENGTH_LONG).show();
        }
    }

    //保存到数据库
    private void savecheckinfo() {

        KitchenClientC obj=new KitchenClientC(myapp.getCompany_ID());

        obj.setName(etClientname.getText().toString().trim());

          obj.setIndexPrinter(mPrinterId);
       // obj.setKitchenAdress(""+pos);
        //保存一下打印机状态，实时反馈到状态界面上
        boolean state = mPortParam.getPortOpenState();
        obj.setStatePrinter(state);

        List<String> dishKindIDList=new ArrayList<String>();
        for(int i=0;i<listSelectedDocId.size();i++)
        {
            dishKindIDList.add(listSelectedDocId.get(i));
        }
        obj.setDishesKindIDList(dishKindIDList);
        CDBHelper.createAndUpdate(getApplicationContext(),obj);

    }

    //打勾的菜类添加到listSelectedDocId
    private void getcheckinfo()
    {
        for (int i = 0; i < allDishKindIdList.size(); i++)
        {
            if (addKitchenAdapter.getIsSelected().get(i))
            {

                listSelectedDocId.add(allDishKindIdList.get(i));
            }
        }
    }

    //编辑模式进来保存
    private void UpDataIP( String docId)
    {

            getcheckinfo();
            if(listSelectedDocId.size()<=0)
            {
                Toast.makeText(this, "请选择要接收的菜品类",Toast.LENGTH_LONG).show();
            }
            else
            {
                Updatecheckinfo(docId);
                Intent intent = new Intent();
                //把返回数据存入Intent
                //intent.putExtra("result", "My name is xxx");
                //设置返回数据
                this.setResult(2015, intent);
                this.finish();
            }


    }

    //保存数据到数据库
    private void Updatecheckinfo(String docId) {

        MutableArray dishKindIdArray = new MutableArray();
        for(int i=0;i<listSelectedDocId.size();i++)
        {
            dishKindIdArray.addString(listSelectedDocId.get(i));
        }
        Document docKitchenClient=CDBHelper.getDocByID(getApplicationContext(),docId);
        if(docKitchenClient!=null)
        {
            MutableDocument muDoc = docKitchenClient.toMutable();
            muDoc.setString("name",etClientname.getText().toString().trim());


            //保存一下打印机状态，实时反馈到状态界面上
            boolean state = mPortParam.getPortOpenState();

            muDoc.setInt("indexPrinter",mPrinterId);
            muDoc.setBoolean("statePrinter",state);
            muDoc.setValue("dishesKindIDList",dishKindIdArray);
            CDBHelper.saveDocument(getApplicationContext(),muDoc);
        }

    }




    private void Initcpplfromdata(String docId)
    {
        Document docKC = CDBHelper.getDocByID(getApplicationContext(),docId);

        if(docKC!=null)
        {

            Array dishKindIDList=docKC.getArray("dishesKindIDList");
            for (int i = 0; i < allDishKindIdList.size(); i++)
            {
                for(int j=0;j<dishKindIDList.count();j++)
                {
                    String DishKindId=dishKindIDList.getString(j);

                    if (allDishKindIdList.get(i).equals(DishKindId))
                    {
                        addKitchenAdapter.getIsSelected().put(i, true);
                        break;
                    }
                }
            }
            // 刷新listview和TextView的显示
           addKitchenAdapter.notifyDataSetChanged();
        }
    }


    //初始化打印机
    private void initPortParam() {
          boolean state = false;
            try {

                if (mGpService.getPrinterConnectStatus(mPrinterId) == GpDevice.STATE_CONNECTED) 
                {
                    state= true;
                }
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            PortParamDataBase database = new PortParamDataBase(this);
            mPortParam = new PortParameters();
            mPortParam = database.queryPortParamDataBase("" +mPrinterId);
            mPortParam.setPortOpenState(state);
        
    }
    public Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_CONNECT:
                    connectOrDisConnectToDevice();
            }
            return false;
        }
    });

    void connectOrDisConnectToDevice() {
      
        int rel = 0;
        if (mPortParam.getPortOpenState() == false)
        {
            if (CheckPortParamters(mPortParam))
            {
                try {
                    mGpService.closePort(mPrinterId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                switch (mPortParam.getPortType()) {
                    case PortParameters.USB:
                        try {

                            rel = mGpService.openPort(mPrinterId, mPortParam.getPortType(),
                                    mPortParam.getUsbDeviceName(), 0);
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    case PortParameters.ETHERNET:
                        try {
                            rel = mGpService.openPort(mPrinterId, mPortParam.getPortType(),
                                    mPortParam.getIpAddr(), mPortParam.getPortNumber());
                        } catch (RemoteException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        break;
                    case PortParameters.BLUETOOTH:
                        try {
                            rel = mGpService.openPort(mPrinterId, mPortParam.getPortType(),
                                    mPortParam.getBluetoothAddr(), 0);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
                if (r != GpCom.ERROR_CODE.SUCCESS) {
                    if (r == GpCom.ERROR_CODE.DEVICE_ALREADY_OPEN) {
                        mPortParam.setPortOpenState(true);
                    } else {
                        messageBox(GpCom.getErrorText(r));
                    }
                }
            } else {
                messageBox(getString(R.string.port_parameters_wrong));
            }
        }
//        else {
//            setProgressBarIndeterminateVisibility(true);
//            try {
//                mGpService.closePort(mPrinterId);
//            } catch (RemoteException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideNavigationBar();
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBroadcast();

        this.sendBroadcast(new Intent(GlobalConstant.printer_msg_pause));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()){
            this.unregisterReceiver(PrinterStatusBroadcastReceiver);
            this.sendBroadcast(new Intent(GlobalConstant.printer_msg_resum));
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();

    }


    //检查是哪个打印机
    Boolean CheckPortParamters(PortParameters param) {
        boolean rel = false;
        int type = param.getPortType();
        if (type == PortParameters.BLUETOOTH) {
            if (!param.getBluetoothAddr().equals("")) {
                rel = true;
            }
        } else if (type == PortParameters.ETHERNET) {
            if ((!param.getIpAddr().equals("")) && (param.getPortNumber() != 0)) {
                rel = true;
            }
        } else if (type == PortParameters.USB) {
            if (!param.getUsbDeviceName().equals("")) {
                rel = true;
            }
        }
        return rel;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_PORT_SETTINGS) {
            // getIP settings info from IP settings dialog
            if (resultCode == RESULT_OK) {
                Bundle bundle = new Bundle();
                bundle = data.getExtras();
                //Log.d(DEBUG_TAG, "PrinterId " + mPrinterId);
                int param = bundle.getInt(GpPrintService.PORT_TYPE);
                mPortParam.setPortType(param);
                //Log.d(DEBUG_TAG, "PortType " + param);
                String str = bundle.getString(GpPrintService.IP_ADDR);
                mPortParam.setIpAddr(str);
                //Log.d(DEBUG_TAG, "IP addr " + str);
                param = bundle.getInt(GpPrintService.PORT_NUMBER);
                mPortParam.setPortNumber(param);
                //Log.d(DEBUG_TAG, "PortNumber " + param);
                str = bundle.getString(GpPrintService.BLUETOOT_ADDR);
                mPortParam.setBluetoothAddr(str);
                //Log.d(DEBUG_TAG, "BluetoothAddr " + str);
                str = bundle.getString(GpPrintService.USB_DEVICE_NAME);
                mPortParam.setUsbDeviceName(str);
                //Log.d(DEBUG_TAG, "USBDeviceName " + str);
                if (CheckPortParamters(mPortParam))
                {
                    PortParamDataBase database = new PortParamDataBase(this);
                    database.deleteDataBase("" + mPrinterId);
                    database.insertPortParam(mPrinterId, mPortParam);
                    Message message = new Message();
                    message.what = MESSAGE_CONNECT;
                    mHandler.sendMessage(message);
                } else {
                    messageBox(getString(R.string.port_parameters_wrong));
                }


            } else {
                messageBox(getString(R.string.port_parameters_is_not_save));
            }
        }

    }
    private void messageBox(String err) {
        Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
    }


    //注册广播
    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(GpCom.ACTION_CONNECT_STATUS);
        this.registerReceiver(PrinterStatusBroadcastReceiver, filter);
    }

    //打印机状态广播接收器
    private BroadcastReceiver PrinterStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GpCom.ACTION_CONNECT_STATUS.equals(intent.getAction()))
            {
                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);

                MyLog.e("ACTION_CONNECT_STATUS","type="+type);
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);
                if(id!=mPrinterId)
                    return;
                if (type == GpDevice.STATE_CONNECTING)  //2 正在连接
                {
                    setProgressBarIndeterminateVisibility(true);
                    mPortParam.setPortOpenState(false);
                    spPrinter.setText("正在连接");

                } else if (type == GpDevice.STATE_NONE)//0 没有连接
                {
                    List<Document>    docList  = CDBHelper.getDocmentsByWhere(null,
                            Expression.property("className").equalTo(Expression.string("KitchenClientC"))
                                    .and(Expression.property("indexPrinter").equalTo(Expression.intValue(id))),
                            null);

                    if(docList.size()>0)
                    {
                        Document doc  = docList.get(0);
                        MutableDocument mutableDocument = doc.toMutable();
                        mutableDocument.setBoolean("statePrinter",false);
                        CDBHelper.saveDocument(null,mutableDocument);
                    }

                    setProgressBarIndeterminateVisibility(false);
                    mPortParam.setPortOpenState(false);
                     spPrinter.setText("未连接");

                } else if (type == GpDevice.STATE_VALID_PRINTER)//5 连接成功
                {

                    List<Document>    docList  = CDBHelper.getDocmentsByWhere(null,
                            Expression.property("className").equalTo(Expression.string("KitchenClientC"))
                                    .and(Expression.property("indexPrinter").equalTo(Expression.intValue(id))),
                            null);

                    if(docList.size()>0)
                    {
                        Document doc  = docList.get(0);
                        MutableDocument mutableDocument = doc.toMutable();
                        mutableDocument.setBoolean("statePrinter",true);
                        CDBHelper.saveDocument(null,mutableDocument);
                    }

                    setProgressBarIndeterminateVisibility(false);
                    mPortParam.setPortOpenState(true);
                    spPrinter.setText("连接成功");

                } else if (type == GpDevice.STATE_INVALID_PRINTER)//4 连接中断
                {
                    setProgressBarIndeterminateVisibility(false);
                    mPortParam.setPortOpenState(false);
                    messageBox("Please use Gprinter!");
                    spPrinter.setText("连接中断");
                }else if ((intent.getAction()).equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {

                    // 业务逻辑的请求码，对应哪里查询做什么操作

                    int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);

                    // 判断请求码，是则进行业务操作

                    if (requestCode == MAIN_QUERY_PRINTER_STATUS) {


                        int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);

                        String str;

                        if (status == GpCom.STATE_NO_ERR) {

                            str = "打印机正常";

                            //printerSat = true;

                        } else {

                            str = "打印机 ";

                            if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {

                                str += "脱机";

                            }

                            if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {

                                str += "缺纸";

                            }

                            if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {

                                str += "打印机开盖";

                            }

                            if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {

                                str += "打印机出错";

                            }

                            if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {

                                str += "查询超时";

                            }
                            spPrinter.setText(str);
                        }
                    }

                }
            }

        }
    };

}