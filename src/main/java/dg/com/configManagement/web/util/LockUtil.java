package dg.com.configManagement.web.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;

/**
 * ���ڿ�ϵͳ���߿������֮���������������ͬһ�������
 * @author Administrator
 *
 */
public class LockUtil {
	private static CuratorFramework client = null;
	private static Logger logger = Logger.getLogger(LockUtil.class);
	protected static CountDownLatch latch = new CountDownLatch(1);
	protected static CountDownLatch shardLocklatch = new CountDownLatch(1);
	private static String selfIdentity=null;
	private static String selfNodeName= null;
	public static synchronized void init(String connectString) {
		if (client != null)
			return;

		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		client = CuratorFrameworkFactory.builder().connectString(connectString)
				.sessionTimeoutMs(10000).retryPolicy(retryPolicy)
				.namespace("LockService").build();
		client.start();

		// ������Ŀ¼
		try {
			if (client.checkExists().forPath("/ExclusiveLockDemo") == null) {
				client.create().creatingParentsIfNeeded()
						.withMode(CreateMode.PERSISTENT)
						.withACL(Ids.OPEN_ACL_UNSAFE)
						.forPath("/ExclusiveLockDemo");
			}
			// ����������
			addChildWatcher("/ExclusiveLockDemo");
			if (client.checkExists().forPath("/ShardLockDemo") == null) {
				client.create().creatingParentsIfNeeded()
						.withMode(CreateMode.PERSISTENT)
						.withACL(Ids.OPEN_ACL_UNSAFE).forPath("/ShardLockDemo");
			}
		} catch (Exception e) {
			logger.error("ZK���������Ӳ���");
			throw new RuntimeException("ZK���������Ӳ���");
		}
	}

	public static synchronized void getExclusiveLock() {
		while (true) {
			try {
				client.create().creatingParentsIfNeeded()
						.withMode(CreateMode.EPHEMERAL)
						.withACL(Ids.OPEN_ACL_UNSAFE)
						.forPath("/ExclusiveLockDemo/lock");
				logger.info("�ɹ���ȡ����");
				return;// ����ڵ㴴���ɹ�����˵����ȡ���ɹ�
			} catch (Exception e) {
				logger.info("�˴λ�ȡ��û�гɹ�");
				try {
					//���û�л�ȡ��������Ҫ��������ͬ����Դֵ
					if(latch.getCount()<=0){
						latch = new CountDownLatch(1);
					}
					latch.await();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					logger.error("", e1);
				}
			}
		}
	}

	/**
	 * 
	 * @param type
	 *            0Ϊ������1Ϊд��
	 * @param identity
	 *            ��ȡ��ǰ����������
	 */
	public static boolean getShardLock(int type, String identity) {
		if (identity == null || "".equals(identity)) {
			throw new RuntimeException("identity����Ϊ��");
		}
		if (identity.indexOf("-") != -1) {
			throw new RuntimeException("identity���ܰ����ַ�-");
		}
		if (type != 0 && type != 1) {
			throw new RuntimeException("typeֻ��Ϊ0����1");
		}
		String nodeName = null;
		if (type == 0) {
			nodeName = "R" + identity + "-";
		} else if (type == 1) {
			nodeName = "W" + identity + "-";
		}
		selfIdentity = nodeName;
		try {
			//if (client.checkExists().forPath("/ShardLockDemo/" + nodeName) == null)
				 selfNodeName = client.create().creatingParentsIfNeeded()
						.withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
						.withACL(Ids.OPEN_ACL_UNSAFE)
						.forPath("/ShardLockDemo/" + nodeName);
				logger.info("�����ڵ�:"+selfNodeName);
			List<String> lockChildrens = client.getChildren().forPath(
					"/ShardLockDemo");
			if (!canGetLock(lockChildrens, type,
					nodeName.substring(0, nodeName.length() - 1),false)) {
				shardLocklatch.await();
			}
			// return;// ������ɹ��ͷ���
		} catch (Exception e) {
			logger.info("�����쳣", e);
			return false;
		}
		
		logger.info("�ɹ���ȡ��");		
		return true;
	}

	private static boolean canGetLock(List<String> childrens, int type,
			String identity,boolean reps) {
		boolean res = false;
		if(childrens.size()<=0)
			return true;
		
		try {
			String currentSeq = null;
			List<String> seqs = new ArrayList<String>();
			//List<String> identitys = new ArrayList<String>();
			Map<String,String> seqs_identitys = new HashMap<String,String>();
			for (String child : childrens) {
				String splits[] = child.split("-");
				seqs.add(splits[1]);
				//identitys.add(splits[0]);
				seqs_identitys.put(splits[1], splits[0]);
				if (identity.equals(splits[0]))
					currentSeq = splits[1];
			}

			List<String> sortSeqs = new ArrayList<String>();
			sortSeqs.addAll(seqs);
			Collections.sort(sortSeqs);

			// ��һ���ڵ㣬�������Ƕ�������д�������Ի�ȡ
			if (currentSeq.equals(sortSeqs.get(0))) {
				res = true;
				logger.info("������,��Ϊ�ǵ�һ�����������������Ի�ȡ�ɹ�");
				return res;
			} else {
				// д��
				if (type == 1) {
					res = false;
					//��һ������ȡ�������ü������Ժ�Ͳ������ˣ���Ϊ����һֱ����
					if(reps==false)
						addChildWatcher("/ShardLockDemo");
					logger.info("����д������Ϊǰ���������������Ի�ȡ��ʧ��");
					return res;
				}
			}
			// int index =-1;
			boolean hasW = true;
			for (String seq : sortSeqs) {
				// ++index;
				if (seq.equals(currentSeq)) {
					break;
				}
				if (!seqs_identitys.get(seq).startsWith("W"))
					hasW = false;
			}
			if (type == 0 && hasW == false) {
				res = true;
			} else if (type == 0 && hasW == true) {
				res = false;
			}
			if (res == false) {
				// ��Ӽ���
				addChildWatcher("/ShardLockDemo");
				logger.info("��Ϊû�л�ȡ������������ļ�����");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	public static boolean unlockForExclusive() {
		try {
			if (client.checkExists().forPath("/ExclusiveLockDemo/lock") != null) {
				client.delete().forPath("/ExclusiveLockDemo/lock");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean unlockForShardLock() {
		try {
			if (client.checkExists().forPath(selfNodeName) != null) {
				client.delete().forPath(selfNodeName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static void addChildWatcher(String path) throws Exception {
		@SuppressWarnings("resource")
		final PathChildrenCache cache = new PathChildrenCache(client, path,
				true);
		cache.start(StartMode.POST_INITIALIZED_EVENT);// ppt����Ҫ��StartMode
		// System.out.println(cache.getCurrentData().size());
		cache.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client,
					PathChildrenCacheEvent event) throws Exception {
				if (event.getType().equals(
						PathChildrenCacheEvent.Type.INITIALIZED)) {

				} else if (event.getType().equals(
						PathChildrenCacheEvent.Type.CHILD_ADDED)) {

				} else if (event.getType().equals(
						PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
					String path = event.getData().getPath();
					System.out.println("�յ�����"+path);
					if(path.contains("ExclusiveLockDemo")){
						logger.info("������,�յ����ͷ�֪ͨ");						
						latch.countDown();
					}else if(path.contains("ShardLockDemo")){
						logger.info("������,�յ����ͷ�֪ͨ");	
						//�յ��Լ���֪ͨ�Ͳ�����
						if(path.contains(selfIdentity))
							return;
						List<String> lockChildrens = client.getChildren().forPath(
								"/ShardLockDemo");
						boolean isLock = false;
						try{
						if(selfIdentity.startsWith("R"))
							isLock = canGetLock(lockChildrens,0,selfIdentity.substring(0, selfIdentity.length() - 1),true);
						else if(selfIdentity.startsWith("W"))
							isLock = canGetLock(lockChildrens,1,selfIdentity.substring(0, selfIdentity.length() - 1),true);
						}catch(Exception e){
							e.printStackTrace();
						}
						logger.info("�յ����ͷż��������³��Ի�ȡ�������Ϊ:"+isLock);
						if(isLock){
							//�����
							logger.info("������������Ϊ��ȡ������������");
							shardLocklatch.countDown();
						}
					}
				} else if (event.getType().equals(
						PathChildrenCacheEvent.Type.CHILD_UPDATED)) {

				}
			}
		});
	}
}
