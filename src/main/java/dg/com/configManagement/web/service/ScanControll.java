package dg.com.configManagement.web.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs.Ids;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class ScanControll {
	//��ɨ��İ���
	private String scanPath = "";
	private CuratorFramework client = null;
	private String connectString = "";
	//Ӧ����
	private String bizCode = "sampleweb";

	public ScanControll(String path, String connectString, String bizcode) {
		scanPath = path;
		this.connectString = connectString;
		this.bizCode = bizcode;
		System.out.println(scanPath + "--" + connectString + "---" + bizCode);
	}

	private void buildZKclient() {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		client = CuratorFrameworkFactory.builder().connectString(connectString)
				.sessionTimeoutMs(10000).retryPolicy(retryPolicy)
				.namespace("webServiceCenter").build();
		client.start();
	}
	
	/**
	 * ��ȡָ�����������б���ӷ���ע�����
	 * ����ע��Ϊcontroll�ͷ����ϵ�requestMapping
	 * @param pack
	 * @return
	 */
	public Set<Class<?>> getClasses(String pack) {

		// ��һ��class��ļ���
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		// �Ƿ�ѭ������
		boolean recursive = true;
		// ��ȡ�������� �������滻
		String packageName = pack;
		String packageDirName = packageName.replace('.', '/');
		// ����һ��ö�ٵļ��� ������ѭ�����������Ŀ¼�µ�things
		Enumeration<URL> dirs;
		System.out.println("packageDirName=" + packageDirName);
		try {
			dirs = Thread.currentThread().getContextClassLoader()
					.getResources(packageDirName);
			// ѭ��������ȥ
			while (dirs.hasMoreElements()) {
				// ��ȡ��һ��Ԫ��
				URL url = dirs.nextElement();
				System.out.println("path=" + url.getPath());
				// �õ�Э�������
				String protocol = url.getProtocol();
				// ��������ļ�����ʽ�����ڷ�������
				if ("file".equals(protocol)) {
					System.err.println("file���͵�ɨ��");
					// ��ȡ��������·��
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// ���ļ��ķ�ʽɨ���������µ��ļ� ����ӵ�������
					findAndAddClassesInPackageByFile(packageName, filePath,
							recursive, classes);
				} else if ("jar".equals(protocol)) {
					// �����jar���ļ�
					// ����һ��JarFile
					System.err.println("jar���͵�ɨ��");
					JarFile jar;
					try {
						// ��ȡjar
						jar = ((JarURLConnection) url.openConnection())
								.getJarFile();
						// �Ӵ�jar�� �õ�һ��ö����
						Enumeration<JarEntry> entries = jar.entries();
						// ͬ���Ľ���ѭ������
						while (entries.hasMoreElements()) {
							// ��ȡjar���һ��ʵ�� ������Ŀ¼ ��һЩjar����������ļ� ��META-INF���ļ�
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// �������/��ͷ��
							if (name.charAt(0) == '/') {
								// ��ȡ������ַ���
								name = name.substring(1);
							}
							// ���ǰ�벿�ֺͶ���İ�����ͬ
							if (name.startsWith(packageDirName)) {
								int idx = name.lastIndexOf('/');
								// �����"/"��β ��һ����
								if (idx != -1) {
									// ��ȡ���� ��"/"�滻��"."
									packageName = name.substring(0, idx)
											.replace('/', '.');
								}
								// ������Ե�����ȥ ������һ����
								if ((idx != -1) || recursive) {
									// �����һ��.class�ļ� ���Ҳ���Ŀ¼
									if (name.endsWith(".class")
											&& !entry.isDirectory()) {
										// ȥ�������".class" ��ȡ����������
										String className = name.substring(
												packageName.length() + 1,
												name.length() - 6);
										try {
											// ��ӵ�classes
											classes.add(Class
													.forName(packageName + '.'
															+ className));
										} catch (ClassNotFoundException e) {
											// log
											// .error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�");
											e.printStackTrace();
										}
									}
								}
							}
						}
					} catch (IOException e) {
						// log.error("��ɨ���û�������ͼʱ��jar����ȡ�ļ�����");
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return classes;
	}

	public void findAndAddClassesInPackageByFile(String packageName,
			String packagePath, final boolean recursive, Set<Class<?>> classes) {
		// ��ȡ�˰���Ŀ¼ ����һ��File
		File dir = new File(packagePath);
		// ��������ڻ��� Ҳ����Ŀ¼��ֱ�ӷ���
		if (!dir.exists() || !dir.isDirectory()) {
			// log.warn("�û�������� " + packageName + " ��û���κ��ļ�");
			return;
		}
		// ������� �ͻ�ȡ���µ������ļ� ����Ŀ¼
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// �Զ�����˹��� �������ѭ��(������Ŀ¼) ��������.class��β���ļ�(����õ�java���ļ�)
			public boolean accept(File file) {
				return (recursive && file.isDirectory())
						|| (file.getName().endsWith(".class"));
			}
		});
		// ѭ�������ļ�
		for (File file : dirfiles) {
			// �����Ŀ¼ �����ɨ��
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(
						packageName + "." + file.getName(),
						file.getAbsolutePath(), recursive, classes);
			} else {
				// �����java���ļ� ȥ�������.class ֻ��������
				String className = file.getName().substring(0,
						file.getName().length() - 6);
				try {
					// ��ӵ�������ȥ
					// classes.add(Class.forName(packageName + '.' +
					// className));
					// �����ظ�ͬѧ�����ѣ�������forName��һЩ���ã��ᴥ��static������û��ʹ��classLoader��load�ɾ�
					classes.add(Thread.currentThread().getContextClassLoader()
							.loadClass(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					// log.error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�");
					e.printStackTrace();
				}
			}
		}
	}

	public void init() {
		try {
			System.out.println("ɨ���ʼ��------");
			//��ʼ��zk�ͻ���
			buildZKclient();
			registBiz();
			
			//ɨ������action��ͷ���
			Set classes = getClasses(scanPath);
			if (classes.size() < 1)
				return;
			
			//ͨ��ע��õ������ַ
			List<String> services = getServicePath(classes);
			for (String s : services)
				System.out.println("service=" + s);
			System.out.println("------------------size=");
			//�ѷ���ע�ᵽzk
			registBizServices(services);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registBiz() {
		try {
			if (client.checkExists().forPath("/" + bizCode) == null) {
			client.create().creatingParentsIfNeeded()
					.withMode(CreateMode.PERSISTENT)
					.withACL(Ids.OPEN_ACL_UNSAFE)
					.forPath("/" + bizCode, (bizCode + "�ṩ�ķ����б�").getBytes());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void registBizServices(List<String> services) {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String ip = addr.getHostAddress().toString();
			for (String s : services) {
				String temp = s.replace("/", ".");
				if(temp.startsWith("."))
					temp = temp.substring(1);
				if (client.checkExists().forPath("/" + bizCode +"/"+ temp) == null) {
					client.create().creatingParentsIfNeeded()
							.withMode(CreateMode.PERSISTENT)
							.withACL(Ids.OPEN_ACL_UNSAFE)
							.forPath("/" + bizCode +"/"+ temp, ("1").getBytes());
				}
				client.create()
						.creatingParentsIfNeeded()
						.withMode(CreateMode.EPHEMERAL)
						.withACL(Ids.OPEN_ACL_UNSAFE)
						.forPath("/" + bizCode +"/"+ temp + "/" + ip, ("1").getBytes());

			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public List<String> getServicePath(Set<Class> classes) {
		List<String> services = new ArrayList<String>();
		//HashMap<String, Map<String, String>> serviceParams = new HashMap<String, Map<String, String>>();
		StringBuffer sb = null;
		Controller classContorllA = null;
		RequestMapping classRequestmA = null;
		RequestMapping methodRequestmA = null;
		Annotation ann = null;
		for (Class cls : classes) {
			ann = cls.getAnnotation(Controller.class);
			if (ann == null)
				continue;
			else
				classContorllA = (Controller) ann;

			ann = cls.getAnnotation(RequestMapping.class);
			String basePath = getRequestMappingPath(ann);
			Method ms[] = cls.getMethods();
			if (ms == null || ms.length == 0)
				continue;
			for (Method m : ms) {
				ann = m.getAnnotation(RequestMapping.class);

				String path = getRequestMappingPath(ann);
				if (path != null) {
					sb = new StringBuffer();
					if (basePath != null)
						sb.append(basePath);
					if (path.startsWith("/"))
						sb.append(path);
					else
						sb.append("/" + path);
				} else
					continue;
				if (sb != null) {
					services.add(sb.toString());
//					Class paramC[] = m.getParameterTypes();
//					TypeVariable tvs[] = m.getTypeParameters();
//					// int pl = paramC.length;
//					if (paramC.length > 0) {
//						for (int pl = 0; pl < paramC.length; pl++) {
//							System.out.println("params=" + tvs[pl].getName()
//									+ "");
//							System.out.println("params type="
//									+ paramC[pl].getName() + "");
//						}
//					}
				}
				sb = null;
			}
			// sb.append(classA.)
		}
		return services;
	}

	private String getRequestMappingPath(Annotation ann) {
		if (ann == null)
			return null;
		else {
			RequestMapping rma = (RequestMapping) ann;
			String[] paths = rma.value();
			if (paths != null && paths.length > 0)
				return paths[0];
			else
				return null;
		}
	}
}
