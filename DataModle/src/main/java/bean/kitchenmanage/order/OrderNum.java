package bean.kitchenmanage.order;

/**
 * 流水号
 * <p>
 * Created by loongsun on 2017/11/28.
 * <p>
 * email: 125736964@qq.com
 */

public class OrderNum {
    /**
     * docId
     */
    private String id;
    /**
     * 公司唯一身份id,用于数据同步,做为唯一管道符
     */
    private String channelId;
    /**
     * 类名，用于数据库查询类过滤
     */
    private String className = "OrderNum";
    /**
     * 数据分两大类，一个是基础数据 BaseData，一个业务实时数据 UserData
     */
    private String dataType = "BaseData";
    /**
     *yyyy-MM-DD
     */
    private String Date;
    /**
     * 从1开始三位数
     */
    private int num;

    public OrderNum() {
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

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
