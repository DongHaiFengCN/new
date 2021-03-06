package doaing.order.untils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;


import com.couchbase.lite.Document;
import com.couchbase.lite.Expression;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bean.kitchenmanage.order.CheckOrder;
import bean.kitchenmanage.order.Goods;
import bean.kitchenmanage.order.HangInfo;
import bean.kitchenmanage.order.Order;
import bean.kitchenmanage.order.PayDetail;
import bean.kitchenmanage.table.Area;
import bean.kitchenmanage.user.Company;
import doaing.mylibrary.MyApplication;
import doaing.order.view.PayActivity;
import tools.CDBHelper;
import tools.MyLog;

/*
*
 * 项目名称：Order
 * 类描述：
 * 创建人：donghaifeng
 * 创建时间：2017/9/28 10:13
 * 修改人：donghaifeng
 * 修改时间：2017/9/28 10:13
 * 修改备注：
*/



public class ProgressBarasyncTask extends AsyncTask<Integer, Integer, String> {
    private  Date date;

    private String flag = "";

    private BluetoothAdapter btAdapter;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private PayActivity payActivity;
    private CheckOrder checkOrderC;
    private String str; //临时变量
    private float total;
    private List<Goods> goodsCList = new ArrayList<>();

    public ProgressBarasyncTask(PayActivity payActivity) {

        this.payActivity = payActivity;

    }
    //首先执行的是onPreExecute方法

    //该方法运行在Ui线程内，可以对UI线程内的控件设置和修改其属性

    @Override
    protected void onPreExecute() {

         //this.payActivity.showDialog();
    }

    //其次是执行doInBackground方法
    @Override
    protected String doInBackground(Integer... integers) {

        btAdapter = BluetoothUtil.getBTAdapter();

       if (btAdapter == null) {



            return "本机没有找到蓝牙硬件或驱动!";
        }

        device = BluetoothUtil.getDevice(btAdapter);

       if (device == null) {

            return "请确保InnterPrinter 蓝牙打印设备打开!";
        }

        try {
            socket = BluetoothUtil.getSocket(device);
            PrintUtils.setOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }

        total = checkOrderC.getLastPay();
        str = String.valueOf(total);
        MyLog.e(str);
        onPrint();


        return flag;
    }

    private void onPrint() {

        if(true){ //支付成功

            String waiter = "默认";
            MyApplication m = (MyApplication) payActivity.getApplicationContext();
            Document document = CDBHelper.getDocByID(m.getEmployee().getId()) ;
            if(m.getEmployee() != null &&document.getString("name") != null && !document.getString("name").isEmpty()){

                waiter =document.getString("name");
            }


            setAll();
            //List<OrderC> list = checkOrderC.getOrderList();
            List<Company> companyCs = CDBHelper.getObjByClass(Company.class);
            Document tableDoc = CDBHelper.getDocByID(m.getTable_sel_obj().getString("tableId"));
            Document areaDoc = CDBHelper.getDocByID(m.getTable_sel_obj().getString("msgTable_areaId"));
            PrintUtils.selectCommand(PrintUtils.RESET);
            PrintUtils.selectCommand(PrintUtils.LINE_SPACING_DEFAULT);
            PrintUtils.selectCommand(PrintUtils.ALIGN_CENTER);
            if (companyCs.size() != 0){
                PrintUtils.printText(companyCs.get(0).getName()+"\n\n");
            }
            PrintUtils.selectCommand(PrintUtils.DOUBLE_HEIGHT_WIDTH);
            PrintUtils.printText(areaDoc.getString("name")+"/"+tableDoc.getString("name")+"\n\n");
            PrintUtils.selectCommand(PrintUtils.NORMAL);
            PrintUtils.selectCommand(PrintUtils.ALIGN_LEFT);
            Order orderC = CDBHelper.getObjById(checkOrderC.getOrderId().get(0),Order.class);
            PrintUtils.printText(PrintUtils.printTwoData("订单编号", orderC.getSerialNum()+"\n"));
            PrintUtils.printText(PrintUtils.printTwoData("下单时间", checkOrderC.getCheckTime()+"\n"));
            PrintUtils.printText(PrintUtils.printTwoData("人数："+m.getTable_sel_obj().getInt("currentPersons"), "收银员："+waiter+"\n"));
            PrintUtils.printText("--------------------------------\n");
            PrintUtils.selectCommand(PrintUtils.BOLD);
            PrintUtils.printText(PrintUtils.printThreeData("项目", "数量", "金额\n"));
            PrintUtils.printText("--------------------------------\n");
            PrintUtils.selectCommand(PrintUtils.BOLD_CANCEL);

            //for (int i = 0; i < list.size(); i++) {


                for (int j = 0; j < goodsCList.size(); j++) {

                    Goods goodsC = goodsCList.get(j);
                    String taste = "";
                    if (goodsC.getDishesTaste() != null) {
                        taste = "(" + goodsC.getDishesTaste() + ")";
                    }

                    PrintUtils.printText(PrintUtils.printThreeData(goodsC.getDishesName()+taste,goodsC.getDishesCount()+"", MyBigDecimal.mul(goodsC.getPrice(),goodsC.getDishesCount(),1)+"\n"));


                }

            //}
            PrintUtils.printText("--------------------------------\n");
            PrintUtils.printText(PrintUtils.printTwoData("合计", total+"\n"));
            PrintUtils.printText("--------------------------------\n");
            if (!payActivity.Margin.equals("")){
                PrintUtils.printText(PrintUtils.printTwoData("抹零", payActivity.Margin+"\n"));
                PrintUtils.printText("--------------------------------\n");
                payActivity.Margin = "";
            }
            PrintUtils.printText(PrintUtils.printTwoData("实收", checkOrderC.getNeedPay()+"\n"));
            PrintUtils.printText("--------------------------------\n\n");
            PrintUtils.selectCommand(PrintUtils.ALIGN_LEFT);

            List<PayDetail> payDetailCList = checkOrderC.getPayDetailList();
            StringBuffer stringBuffer = new StringBuffer("");

            if(payDetailCList != null && !payDetailCList.isEmpty()){


                for (int i = 0; i < payDetailCList.size(); i++) {

                    PayDetail p = payDetailCList.get(i);

                    switch (p.getPayTypes()){

                        case 1:
                            stringBuffer.append("现金 ");
                            break;
                        case 2:
                            stringBuffer.append("银行卡 ");
                            break;
                        case 3:
                            stringBuffer.append("微信 ");
                            break;
                        case 4:
                            stringBuffer.append("支付宝 ");
                            break;
                        case 5:
                            stringBuffer.append("美团 ");
                            break;
                        case 6:
                            stringBuffer.append("会员卡 ");
                            break;
                        case 7:
                            stringBuffer.append("抹零 ");
                            break;
                        case 8:
                            stringBuffer.append("赠卷 ");
                            break;
                        case 10:
                            stringBuffer.append("挂账 ");
                            break;
                        case 11:
                            stringBuffer.append("团购 ");
                            break;


                        default:
                            break;
                    }
                }
            }
            PrintUtils.printText("支付方式："+stringBuffer.toString());
            PrintUtils.printText("\n\n\n\n\n");

            if (checkOrderC.getHangInfo() != null){
                PrintUtils.printText("支付方式："+"挂账");
                PrintUtils.printText("\n\n");
                if (payActivity.isGuaZ) {
                    PrintUtils.printText("联系方式："+checkOrderC.getHangInfo().getMobile()+"\n");
                    PrintUtils.printText("单位或姓名："+checkOrderC.getHangInfo().getName()+"\n");
                    PrintUtils.printText("挂账签名：");
                    PrintUtils.printText("\n\n\n\n\n");
                    PrintUtils.printText("\n\n\n\n");
                    payActivity.isGuaZ = false;
                }
                PrintUtils.closeOutputStream();
            }
            PrintUtils.closeOutputStream();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        };


    }

    //在doInBackground方法当中，每次调用publishProgrogress()方法之后，都会触发该方法
    @Override
    protected void onProgressUpdate(Integer... values) {


    }
    //在doInBackground方法执行结束后再运行，并且运行在UI线程当中
    //主要用于将异步操作任务执行的结果展示给用户
    @Override
    protected void onPostExecute(String result) {

        //payActivity.closeDialog();

        payActivity.turnDesk();

    }
    /*
*
     *
     * @param checkOrderC 需要打印的参数

*/
    public void setDate(Document checkOrderC){

        this.checkOrderC = CDBHelper.getObjById(checkOrderC.getId(),CheckOrder.class);
    }

    private void setAll() {
        boolean flag ;

        for (String orderCId: checkOrderC.getOrderId()) {
            Order orderC = CDBHelper.getObjById(orderCId,Order.class);
            for (Goods goodsb : orderC.getGoodsList()) {
                flag = false;
                for (Goods goodsC : goodsCList) {

                    if (goodsC.getDishesName().equals(goodsb.getDishesName())) {
                        if (goodsb.getDishesTaste() != null) {
                            if (goodsb.getDishesTaste().equals(goodsC.getDishesTaste())) {
                                float count = MyBigDecimal.add(goodsC.getDishesCount(), goodsb.getDishesCount(), 1);
                                goodsC.setDishesCount(count);
                                flag = true;
                            }

                        } else {

                            float count = MyBigDecimal.add(goodsC.getDishesCount(), goodsb.getDishesCount(), 1);
                            goodsC.setDishesCount(count);

                            flag = true;
                        }

                        break;
                    }


                }
                if (!flag) {
                    Goods objClone = null;
                    try {
                        objClone = (Goods) goodsb.clone();
                    } catch (CloneNotSupportedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    goodsCList.add(objClone);

                }
            }
        }
    }

}
