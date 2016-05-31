package dg.com.configManagement.web.service;

import java.util.HashMap;

import dg.com.configManagement.web.service.pojo.Product;

public interface ProductRepository {
	Product selectProductById(Long id);
	void reduceNum(HashMap<String, Integer> hm);
}
