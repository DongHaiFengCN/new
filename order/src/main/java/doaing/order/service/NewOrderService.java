package doaing.order.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;
import com.couchbase.lite.Meta;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Ordering;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.QueryChange;
import com.couchbase.lite.QueryChangeListener;
import com.couchbase.lite.Result;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.io.GpDevice;
import com.gprinter.io.PortParameters;
import com.gprinter.save.PortParamDataBase;

import org.apache.commons.lang.ArrayUtils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import bean.kitchenmanage.kitchen.KitchenClientC;
import bean.kitchenmanage.order.GoodsC;
import bean.kitchenmanage.order.OrderC;
import doaing.mylibrary.MyApplication;
import doaing.order.untils.GlobalConstant;
import doaing.order.untils.MyBigDecimal;
import doaing.order.untils.MyLog;
import doaing.order.view.DeskActivity;
import doaing.order.view.MainActivity;
import tools.CDBHelper;

public class NewOrderService extends Service {

    private Database db;
    //打印机连接
    private GpService mGpService = null;
    private ExecutorService myExecutor;
    private Map<String, ArrayList<GoodsC>> allKitchenClientGoods = new HashMap<String, ArrayList<GoodsC>>();
    private Map<String, String> allKitchenClientPrintNames = new HashMap<String, String>();
    private String tableName, areaName, currentPersions, serNum,gOrderId;

    private String Tag;

    public NewOrderService()
    {
    }
    private Query listsLiveQuery() {
        return QueryBuilder.select(SelectResult.expression(Meta.id))
                .from(DataSource.database(db))
                .where(Expression.property("className").equalTo(Expression.string("OrderC"))
                        .and(Expression.property("printFlag").equalTo(Expression.intValue(0)))//未打印
                        .and(Expression.property("deviceType").equalTo(Expression.intValue(2)))//手机点餐
                        .and(Expression.property("orderState").equalTo(Expression.intValue(1))))//未结账)
         ;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {


        Tag = getPackageName();
        Log.e(Tag,"onStartCommand");
        mGpService = DeskActivity.getmGpService();

       //只执行一个，后面的排队
        myExecutor   = Executors.newScheduledThreadPool(1);

        db = CDBHelper.getDatabase();
        Log.e(Tag,"CDBHelper.getDatabase");
        Query myquery = listsLiveQuery();
        myquery.addChangeListener(new QueryChangeListener() {
            @Override
            public void changed(QueryChange change)
            {
                ResultSet rs = change.getResults();
                Result result;
                while ((result = rs.next()) != null)
                {
                    String id=result.getString(0);
                    Log.e(Tag,"orderId="+id);
                    printNewOrder(id);
                    }
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void printNewOrder(String orderId)
    {

        final  String orderIdStr=orderId;
        myExecutor.execute( new Runnable() {
            @Override
            public void run()
            {

                printOrderToKitchen(orderIdStr);

            }
        });

    }
    private void changeTableState( String tableNo)
    {
        List<Document> documentList = CDBHelper.getDocmentsByWhere(getApplicationContext(),
                Expression.property("className").equalTo(Expression.string("TableC"))
                        .and(Expression.property("tableNum").equalTo(Expression.string(tableNo)))
                ,null);

        if(documentList.size()>0)
        {
            Document doc = documentList.get(0);

            currentPersions= ""+doc.getInt("currentPersions");

            MutableDocument mDoc = doc.toMutable();
            int state  = doc.getInt("state");
            Log.e(Tag,"this table state is"+state);
            if(state!=2)//没有置成使用状态
            {
                mDoc.setInt("state",2);
                CDBHelper.saveDocument(getApplicationContext(),mDoc);
            }
        }

    }
    // 厨房分单打印

    private void printOrderToKitchen(String order_id)

    {
        OrderC obj= CDBHelper.getObjById(getApplicationContext(),order_id,OrderC.class);
        List<GoodsC> goodsList = obj.getGoodsList();
        areaName = obj.getAreaName();
        tableName = obj.getTableName();
        if (obj.getOrderNum() == 1)//第一次下单

            serNum = obj.getSerialNum();//流水号

        else //多次下单
            serNum = obj.getSerialNum() + "_" + obj.getOrderNum();
        gOrderId = order_id;
        String tableNo = obj.getTableNo();
        changeTableState(tableNo);

        //1\ 查询出所有厨房,并分配菜品

        List<KitchenClientC> kitchenClientList = CDBHelper.getObjByClass(getApplicationContext(), KitchenClientC.class);

        if (kitchenClientList.size() <= 0)

        {
            Log.e(Tag,"未配置厨房数据");
              return;

        }
        allKitchenClientGoods.clear();

        allKitchenClientPrintNames.clear();

        for (KitchenClientC kitchenClientObj : kitchenClientList)//1 for 遍历所有厨房
        {
            boolean findflag = false;
            ArrayList<GoodsC> oneKitchenClientGoods = new ArrayList<GoodsC>();
            for (String dishKindId : kitchenClientObj.getDishesKindIDList())//2 for 遍历厨房下所含菜系
            {
                for (GoodsC goodsC : goodsList)//3 for 该厨房下所应得商品
                {
                    if (dishKindId.equals(goodsC.getDishesKindId())) {

                        findflag = true;
                        // g_printGoodsList.remove(goodsC);
                        // 为了降低循环次数，因为菜品只可能在一个厨房打印分发，故分发完后移除掉。
                        oneKitchenClientGoods.add(goodsC);
                    }

                }//end for 3

            }//end for 2
            if (findflag)  //如果有所属菜品，就去打印
            {

                String clientKtname = "" + kitchenClientObj.getName();//厨房名称

                String printname = "" + kitchenClientObj.getKitchenAdress();//打印机名称
                int printerId = Integer.parseInt(printname)-1;

                allKitchenClientGoods.put("" + printerId, oneKitchenClientGoods);

                allKitchenClientPrintNames.put("" + printerId, clientKtname);

                if (!isPrinterConnected(printerId)) // 未连接
                {

                   Log.e(Tag,""+clientKtname+"厨房打印机未连接，正在连接");

                    if (connectClientPrint(printerId) == 0)

                    {
                        MyLog.d("***********打印机连接命令发送成功");

                    } else {

                        MyLog.d("***********打印机连接命令发送失败");
                    }

                }

                else//已连接
                {
                    printGoodsAtRomoteByIndex(printerId);
                }

            }

            else//不分发打印，就直接跳转
            {

            }

        }//end for1


    }

    private void printGoodsAtRomoteByIndex(int printerId)

    {
        //1、程序连接上厨房端打印机后要进行分厨房打印

        ArrayList<GoodsC> myshangpinlist = allKitchenClientGoods.get("" + printerId);



        //2、获得该打印机内容 打印机名称

        String printname = allKitchenClientPrintNames.get("" + printerId);

        String printcontent = getPrintContentforClient(myshangpinlist, printname);

        if (printContent(printcontent, printerId) == 0)//打印成功，使用打印完成回调

        {

            MyLog.d(printname + "分单打印完成");



        }

        else

        {

            MyLog.d("厨房打印失败");


        }

        setOrderPrintState(gOrderId);



    }



    private void setOrderPrintState(String orderId) {



        OrderC obj = CDBHelper.getObjById(getApplicationContext(), orderId, OrderC.class);

        obj.setPrintFlag(1);

        CDBHelper.createAndUpdate(getApplicationContext(), obj);

    }



    private int printContent(String content, int printIndex)//0发送数据到打印机 成功 其它错误

    {

        int rel = 0;

        try {

            rel = mGpService.sendEscCommand(printIndex, content);

        } catch (RemoteException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

            return -2;

        }

        GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];

        if (r != GpCom.ERROR_CODE.SUCCESS) {

            //Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();

            return -2;

        } else

            return 0;//把数据发送打印机成功

    }



    private String getPrintContentforClient(ArrayList<GoodsC> myshangpinlist, String clientname)

    {



        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");// 设置日期格式

        String date = df.format(new Date());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");// 设置日期格式

        String endtime = sdf.format(new Date());

        EscCommand esc = new EscCommand();

        // 打印标题居中

        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);

        // 设置字体宽高增倍

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF); // 设置为倍高倍宽

        esc.addText(clientname + "\n");// 打印文字

        //打印并换行

        esc.addPrintAndLineFeed();

        // 打印文字

        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽

        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐



        if(false)//  80型宽度
        {



            // esc.addSetLeftMargin((short)10);

            esc.addText("流水号:" + serNum + "\n");//流水号生成机制开发

            esc.addText("房间:" + areaName + "   " + "桌位：" + tableName + "\n");// 打印文字

            esc.addText("人数:" + currentPersions + "\n");//流水号生成机制开发

            esc.addText("时间:" + date + " " + endtime + "\n"); // 时间

            esc.addText("--------------------------------\n");

            esc.addText("------------------------------------------\n");

            esc.addText("菜品名称         单价     数量    金额 \n"); // 菜品名称(14) 单价(6) 数量(5) 金额(7)

            esc.addText("\n");





            for (int i = 0; i < myshangpinlist.size(); i++) {

                float num = 1; // 数量 默认为1

                num = myshangpinlist.get(i).getDishesCount();

                esc.addText(myshangpinlist.get(i).getDishesName().toString());

                String temp = myshangpinlist.get(i).getDishesTaste();

                if (temp == null || "".equals(temp)) {

                    try {

                        for (int j = 0; j < (18 - myshangpinlist.get(i).getDishesName().toString().getBytes("gbk").length); j++)

                            esc.addText(" ");

                    } catch (UnsupportedEncodingException e) {

                        // TODO Auto-generated catch block

                        e.printStackTrace();

                    }

                } else {

                    esc.addText("(" + temp + ")");

                    try {

                        for (int j = 0; j < (18 - myshangpinlist.get(i).getDishesName().toString().getBytes("gbk").length

                                - temp.getBytes("gbk").length - 2); j++)

                            esc.addText(" ");

                    } catch (UnsupportedEncodingException e) {

                        // TODO Auto-generated catch block

                        e.printStackTrace();

                    }

                }

                // 查找菜品的单价



                String strprice = "" + myshangpinlist.get(i).getPrice();//""+ MyBigDecimal.div(myshangpinlist.get(i).getAllPrice(),myshangpinlist.get(i).getDishesCount(),2);//myshangpinlist.get(i).getSinglePrice;

                esc.addText(strprice);

                for (int j = 0; j < 9 - strprice.length(); j++)

                    esc.addText(" ");



                esc.addText("" + num);

                for (int j = 0; j < 7 - ("" + num).length(); j++)

                    esc.addText(" ");



                esc.addText("" + (MyBigDecimal.mul(myshangpinlist.get(i).getPrice(), myshangpinlist.get(i).getDishesCount(), 1)) + "\n");

                esc.addPrintAndLineFeed();



            }

            esc.addText("--------------------------------------------\n");

            esc.addPrintAndLineFeed();



            byte len = 0x01;

            esc.addCutAndFeedPaper(len);



        }

        else //58型打印机

        {

            esc.addText("流水号:" + serNum + "\n");//流水号生成机制开发

            esc.addText("房间:" + areaName + "   " + "桌位：" + tableName + "\n");// 打印文字

            esc.addText("人数:" + currentPersions + "\n");//流水号生成机制开发

            esc.addText("时间:" + date + " " + endtime + "\n"); // 时间

            esc.addText("--------------------------------\n"); //32横线==16个汉字

            esc.addText("菜品名称                数量    \n"); // 菜品名称+16个空格即占12个汉字长度；  数量+4个空格即占4个汉字长度 )

            esc.addText("\n");



            esc.addSetHorAndVerMotionUnits((byte)8, (byte) 0);//设置移动单位



            for (int i = 0; i < myshangpinlist.size(); i++)

            {

                String dishesName = myshangpinlist.get(i).getDishesName();

                float num = myshangpinlist.get(i).getDishesCount();

                String temp = myshangpinlist.get(i).getDishesTaste();

                esc.addSetAbsolutePrintPosition((short) 0);

                if (temp == null || "".equals(temp))//无口味

                {

                    esc.addText(dishesName);

                }

                else//有口味

                {

                    esc.addText(dishesName+"("+temp+")");

                }

                esc.addSetAbsolutePrintPosition((short) 13);

                esc.addText("" + num+"\n");

                //换行

                esc.addPrintAndLineFeed();



            }

            esc.addText("--------------------------------------------\n");

            esc.addPrintAndLineFeed();



        }





        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播

        esc.addQueryPrinterStatus();



        Vector<Byte> datas = esc.getCommand();

        // 发送数据

        Byte[] Bytes = datas.toArray(new Byte[datas.size()]);

        byte[] bytes = ArrayUtils.toPrimitive(Bytes);

        String str = Base64.encodeToString(bytes, Base64.DEFAULT);

        return str;



    }



    private int connectClientPrint(int index) {

        if (mGpService != null) {

            try {

                PortParamDataBase database = new PortParamDataBase(this);

                PortParameters mPortParam = new PortParameters();

                mPortParam = database.queryPortParamDataBase(""+index);


                Log.e("MainActivity",""+mPortParam.getPortNumber()+"----"+mPortParam.getIpAddr());

                int rel = -1;



                try {

                    mGpService.closePort(index);

                } catch (RemoteException e) {

                    e.printStackTrace();

                }

                switch (mPortParam.getPortType())

                {

                    case PortParameters.USB:

                        rel = mGpService.openPort(index, mPortParam.getPortType(),

                                mPortParam.getUsbDeviceName(), 0);

                        break;

                    case PortParameters.ETHERNET:



                        try {

                            rel = mGpService.openPort(index, mPortParam.getPortType(),

                                    mPortParam.getIpAddr(), mPortParam.getPortNumber());

                        } catch (RemoteException e) {

                            // TODO Auto-generated catch block

                            e.printStackTrace();

                        }

                        break;

                    case PortParameters.BLUETOOTH:

                        try {

                            rel = mGpService.openPort(index, mPortParam.getPortType(),

                                    mPortParam.getBluetoothAddr(), 0);

                        } catch (RemoteException e) {

                            e.printStackTrace();

                        }



                        break;

                }



                database.close();

                GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];

                if (r != GpCom.ERROR_CODE.SUCCESS) {

                    if (r == GpCom.ERROR_CODE.DEVICE_ALREADY_OPEN) {

                        return 0;

                    } else {

                        return -1;

                    }

                } else

                    return 0;



            } catch (Exception e) {

                // TODO Auto-generated catch block

                e.printStackTrace();

                return -1;

            }

        } else

            return -1;

    }





     /*打印机连接状态判断

     *

     * @param index

     * @return

     * */





    private Boolean isPrinterConnected(int index) {

//        if (!printerSat)

//            return false;

        // 一上来就先连接蓝牙设备

        int status = 0;

        if (mGpService == null)

            return false;

        try {

            status = mGpService.getPrinterConnectStatus(index);

            MyLog.d("printer statue=" + status);

        } catch (RemoteException e1) {

            // TODO Auto-generated catch block

            e1.printStackTrace();

        }

        return status == GpDevice.STATE_CONNECTED;

    }




}
