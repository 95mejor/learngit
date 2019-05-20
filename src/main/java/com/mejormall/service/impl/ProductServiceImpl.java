package com.mejormall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.dao.CategoryMapper;
import com.mejormall.dao.ProductMapper;
import com.mejormall.pojo.Category;
import com.mejormall.pojo.Product;
import com.mejormall.service.ICategoryService;
import com.mejormall.service.IProductService;
import com.mejormall.util.DateTimeUtil;
import com.mejormall.util.PropertiesUtil;
import com.mejormall.vo.ProductDetailVo;
import com.mejormall.vo.ProductListVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("iProductService")
public class ProductServiceImpl implements IProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ICategoryService iCategoryService;

    @Override
    public ServiceResponse saveOrUpdateProduct(Product product) {
        if(product != null){
            if(StringUtils.isNotBlank(product.getSubImages())){
                String[] subImagesArray = product.getSubImages().split(",");
                if(subImagesArray.length > 0){
                    product.setMainImage(subImagesArray[0]);
                }
            }
            if(product.getId() != null){
                int rowCount = productMapper.updateByPrimaryKey(product);
                if(rowCount > 0){
                    return ServiceResponse.creatBySuccessMessage("更新产品信息成功");
                }else{
                    return ServiceResponse.creatByErrorMessage("更新产品信息失败");
                }
            }else{
                int rowCount = productMapper.insert(product);
                if(rowCount > 0){
                    return ServiceResponse.creatBySuccessMessage("添加产品信息成功");
                }else{
                    return ServiceResponse.creatByErrorMessage("添加产品信息失败");
                }
            }
        }
        return ServiceResponse.creatByErrorMessage("添加或更新产品信息参数错误");
    }

    @Override
    public ServiceResponse setSaleStatus(Integer productId, Integer status) {
        if(productId == null || status == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"上下架产品参数错误");
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        int rowCount = productMapper.updateByPrimaryKeySelective(product);
        if(rowCount > 0){
            return ServiceResponse.creatBySuccessMessage("修改产品销售状态成功");
        }
        return ServiceResponse.creatByErrorMessage("修改产品销售状态失败");
    }

    @Override
    public ServiceResponse<ProductDetailVo> manageProductDetail(Integer productId){
        if(productId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"获取详情参数错误");
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServiceResponse.creatByErrorMessage("产品已下架或者被删除");
        }
        //vo  ->  pojo
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServiceResponse.creatBySuccess(productDetailVo);
    }

    private ProductDetailVo assembleProductDetailVo(Product product){
        ProductDetailVo pdv = new ProductDetailVo();
        pdv.setId(product.getId());
        pdv.setCategoryId(product.getCategoryId());
        pdv.setName(product.getName());
        pdv.setDetail(product.getDetail());
        pdv.setMainImage(product.getMainImage());
        pdv.setPrice(product.getPrice());
        pdv.setStatus(product.getStatus());
        pdv.setStock(product.getStock());
        pdv.setSubImages(product.getSubImages());
        pdv.setSubTitle(product.getSubtitle());
        //配置服务器中图片地址
        pdv.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix","http://image.mejor.com/"));
        Category category = categoryMapper.selectByPrimaryKey(product.getCategoryId());
        if(category == null){
            pdv.setParentCategoryId(0);//默认是根节点
        }else{
            pdv.setParentCategoryId(category.getParentId());
        }
        pdv.setCreateTime(DateTimeUtil.dateToStr(product.getCreateTime()));
        pdv.setUpdateTime(DateTimeUtil.dateToStr(product.getUpdateTime()));
        return pdv;
    }

    @Override
    public ServiceResponse<PageInfo> getProductList(Integer pageNum,Integer pageSize){
        if(pageNum == null || pageSize == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"获取详情参数错误");
        }
        PageHelper.startPage(pageNum,pageSize);
        List<Product> products = productMapper.selectList();
        List<ProductListVo> listVos = Lists.newArrayList();
        for (Product product : products) {
            ProductListVo productListVo = assembleProductListVo(product);
            listVos.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(products);
        pageResult.setList(listVos);
        return ServiceResponse.creatBySuccess(pageResult);
    }

    private ProductListVo assembleProductListVo(Product product){
        ProductListVo productListVo = new ProductListVo();
        productListVo.setId(product.getId());
        productListVo.setCategoryId(product.getCategoryId());
        productListVo.setMainImage(product.getMainImage());
        productListVo.setName(product.getName());
        productListVo.setSubTitle(product.getSubtitle());
        productListVo.setStatus(product.getStatus());
        productListVo.setStock(product.getStock());
        productListVo.setPrice(product.getPrice());
        //配置服务器中图片地址
        productListVo.setImageHost(PropertiesUtil.getProperty("frp.sever.http.prefix","http://image.mejor.com/"));
        return productListVo;
    }

    public ServiceResponse<PageInfo> productSearch(String productName,Integer productId,Integer pageNum,Integer pageSize){
        PageHelper.startPage(pageNum,pageSize);
        if(StringUtils.isNotBlank(productName)){
            productName = new StringBuilder().append("%").append(productName).append("%").toString();
        }
        List<Product> products = productMapper.selectByNameAndProductId(productName,productId);
        List<ProductListVo> listVos = Lists.newArrayList();
        for (Product product : products) {
            ProductListVo productListVo = assembleProductListVo(product);
            listVos.add(productListVo);
        }
        PageInfo pageResult = new PageInfo(products);
        pageResult.setList(listVos);
        return ServiceResponse.creatBySuccess(pageResult);
    }

    public ServiceResponse<ProductDetailVo> getProductDetail(Integer productId){
        if(productId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"获取详情参数错误");
        }
        Product product = productMapper.selectByPrimaryKey(productId);
        if(product == null){
            return ServiceResponse.creatByErrorMessage("产品已下架或者被删除");
        }
        if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
            return ServiceResponse.creatByErrorMessage("产品已经下架或者被删除");
        }
        ProductDetailVo productDetailVo = assembleProductDetailVo(product);
        return ServiceResponse.creatBySuccess(productDetailVo);
    }

    public ServiceResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,Integer pageNum,Integer pageSize,String orderBy){
        if(StringUtils.isBlank(keyword) && categoryId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"获取商品详情参数错误");
        }
        List<Integer> categoryIdList = new ArrayList<Integer>();
        if(categoryId != null){
            Category category = categoryMapper.selectByPrimaryKey(categoryId);
            if(category == null && StringUtils.isBlank(keyword)){
                PageHelper.startPage(pageNum,pageSize);
                List<ProductDetailVo> productDetailVos = Lists.newArrayList();
                PageInfo pageInfo = new PageInfo(productDetailVos);
                return ServiceResponse.creatBySuccess(pageInfo);
            }
            categoryIdList = iCategoryService.findAllCategory(categoryId).getData();
        }
        if(StringUtils.isNotBlank(keyword)){
            keyword = new StringBuilder().append("%").append(keyword).append("%").toString();
        }
        PageHelper.startPage(pageNum,pageSize);
        if(StringUtils.isNotBlank(orderBy)){
            if(Const.ProductListOrderBy.PRICE_ASC_DESC.contains(orderBy)){
                String[] orderByList = orderBy.split("_");
                PageHelper.orderBy(orderByList[0] + " " + orderByList[1]);
            }
        }
        List<Product> products = productMapper.selectByNameAndCategoryId(StringUtils.isBlank(keyword)?null:keyword,categoryIdList.size()==0?null:categoryIdList);
        List<ProductListVo> productListVos = Lists.newArrayList();
        for (Product product : products) {
            ProductListVo productListVo = assembleProductListVo(product);
            productListVos.add(productListVo);
        }
        PageInfo pageInfo = new PageInfo(productListVos);
        return ServiceResponse.creatBySuccess(pageInfo);
    }
}
