package bean.kitchenmanage.kitchen;

import java.util.List;

/**
 * 打印机连接类或者厨房连接类，连接终终信息有独特性，不与其它终端通用，只存在终端数据库中，不同步到服务器
 * <p>
 * Created by loongsun on 2017/5/15.
 * <p>
 * email: 125736964@qq.com
 */

public class KitchenClient {
    /**
     * docId
     */
    private String id;
    /**
     * 类名称
     */
    private String className="KitchenClient";
    /**
     * 厨房菜间名称
     */
    private String name;
    /**
     * 关联所属菜品类
     */
    private List<String> kindIds;
    /**
     * 厨房地址,在厨房打印机模式下叫打印机名称；在厨房平板模式下是平板ip地址
     */
    private String ip;
    /**
     * 打印机序号
     */
    private int printerId;

    /**
     * 是否接收微信小程序订单并打印
     */
    private boolean wxReceiveFlag;

    public KitchenClient()
    {

    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getKindIds() {
        return kindIds;
    }

    public void setKindIds(List<String> kindIds) {
        this.kindIds = kindIds;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPrinterId() {
        return printerId;
    }

    public boolean isWxReceiveFlag() {
        return wxReceiveFlag;
    }

    public void setWxReceiveFlag(boolean wxReceiveFlag) {
        this.wxReceiveFlag = wxReceiveFlag;
    }

    public void setPrinterId(int printerId) {
        this.printerId = printerId;
    }
}
