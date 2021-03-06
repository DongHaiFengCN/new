package bean.kitchenmanage.depot;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bean.kitchenmanage.user.Employee;


/**
 * @ClassName: Inventory
 * @Description: 盘点类
 * @author loongsun
 * @date 2016-01-01 上午1:19:08
 *
 */
public class Inventory implements Serializable
{
    /**
     *对象id,等于docmentid,一般用于Pojo操作时使用。
     */
    private  String id;
    /**
     * 公司唯一身份id,用于数据同步,做为唯一管道符
     */
    private String channelId;
    /**
     * 类名，用于数据库查询类过滤
     */
    private String className = "Inventory";
    /**
     * 数据分两大类，一个是基础数据 BaseData，一个业务实时数据 UserData
     */
    private String dataType = "BaseData";
    /**
     * 盘点编号
     */
    private String num;
    /**
     * 创建日期
     */
    private String createdTime;
    /**
     * 盘点人
     */
    private Employee operator;
    /**
     * 备注
     */
    private String note;
    /**
     * 状态 1、草稿；2、提交
     */
    private int state;
    /**
     * 每个源料的盘点记录
     */
    private List<InventoryItem> inventoryItemList;

    public Inventory() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public Employee getOperator() {
        return operator;
    }

    public void setOperator(Employee operator) {
        this.operator = operator;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public List<InventoryItem> getInventoryItemList() {
        return inventoryItemList;
    }

    public void setInventoryItemList(List<InventoryItem> inventoryItemList) {
        this.inventoryItemList = inventoryItemList;
    }
}
