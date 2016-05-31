package dg.com.configManagement.web.service;

import dg.com.configManagement.web.service.pojo.Order;

public interface OrderRepository {
	void saveOrder(Order order);
}
