package dg.com.zk.example.client;

import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;
import org.junit.*;

public class zkTest {

    private static final String HOSTS = "localhost:2181";
    private static final int SESSION_TIME_OUT = 2000;
    private static ZooKeeper zk = null;

    @Before
    public void init() throws Exception {
        zk = new ZooKeeper(HOSTS, SESSION_TIME_OUT, new Watcher() {

            // 收到watch通知后的回调函数
            public void process(WatchedEvent event) {
                System.out.println("收到的事件类型：" + event.getType() + " 路径是：" + event.getPath());
                try {
                    // 因为监听器只会监听一次，这样可以一直监听,且只监听"/"目录,
                    // ture:还用上一个监听器
                    zk.getChildren("/", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 判断znode是否存在
     */
    @Test
    public void isExist() throws Exception {
        Stat exists = zk.exists("/aa", true);
        String res = "";
        if (exists == null)
            res = "不存在";
        else
            res = "存在";
        System.out.println(res);
    }

    /**
     * 获取子节点
     */
    @Test
    public void getChildren() throws Exception {
        List<String> children = zk.getChildren("/", true);
        for (String child : children) {
            System.out.println("子节点：" + child);
        }
    }

    /**
     * 获取znode数据
     */
    @Test
    public void getData() throws Exception {
        byte[] data = zk.getData("/abc", true, new Stat());
        System.out.println("数据：" + new String(data));
    }

    /**
     * 权限类型：Ids.OPEN_ACL_UNSAFE
     *
     * 数据类型：PERSISTENT_SEQUENTIAL 短暂/持久/有序
     */
    @Test
    public void create() throws Exception {

        String znodePath = zk.create("/abc", "test content 1".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        System.out.println("返回的路径 为：" + znodePath);
    }

    /**
     * 要更新的版本：-1 所有版本
     */
    @Test
    public void setData() throws Exception {

        zk.setData("/abc", "test content 2".getBytes(), -1);
        byte[] data = zk.getData("/abc", true, new Stat());
        System.out.println("返回的路径 为：" + new String(data));
    }

    @Test
    public void delete() throws Exception {

        zk.delete("/abc", -1);
    }

}

