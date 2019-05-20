package com.mejormall.service;

import com.github.pagehelper.PageInfo;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.Product;
import com.mejormall.vo.ProductDetailVo;

public interface IProductService {
    ServiceResponse saveOrUpdateProduct(Product product);

    ServiceResponse setSaleStatus(Integer productId,Integer status);

    ServiceResponse<ProductDetailVo> manageProductDetail(Integer productId);

    ServiceResponse<PageInfo> getProductList(Integer pageNum, Integer pageSize);

    ServiceResponse<PageInfo> productSearch(String productName,Integer productId,Integer pageNum,Integer pageSize);

    ServiceResponse<ProductDetailVo> getProductDetail(Integer productId);

    ServiceResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,Integer pageNum,Integer pageSize,String orderBy);
}
