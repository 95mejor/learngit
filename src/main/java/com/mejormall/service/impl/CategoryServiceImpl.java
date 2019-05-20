package com.mejormall.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mejormall.common.ServiceResponse;
import com.mejormall.dao.CategoryMapper;
import com.mejormall.pojo.Category;
import com.mejormall.service.ICategoryService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {
    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;
    @Override
    public ServiceResponse addCategory(String categoryName,Integer parentId){
        if(parentId == null || StringUtils.isBlank(categoryName)){
            return ServiceResponse.creatByErrorMessage("添加品类参数错误");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);
        category.setStatus(true);
        int row = categoryMapper.insert(category);
        if(row > 0){
            return ServiceResponse.creatBySuccessMessage("添加品类成功！");
        }
        return ServiceResponse.creatBySuccessMessage("添加品类失败！");
    }

    @Override
    public ServiceResponse setCategory(String categoryName,Integer categoryId){
        if(StringUtils.isBlank(categoryName) && categoryId == null){
            return ServiceResponse.creatByErrorMessage("更新品类参数错误！");
        }
        Category category = new Category();
        category.setId(categoryId);
        category.setName(categoryName);
        category.setStatus(true);

        int row = categoryMapper.updateByPrimaryKeySelective(category);
        if(row > 0){
            return ServiceResponse.creatBySuccessMessage("更新品类名字成功！");
        }
        return ServiceResponse.creatByErrorMessage("更新品类名字失败！");
    }

    @Override
    public ServiceResponse<List<Category>> findParallelCategory(Integer parentId){
        List<Category> list = categoryMapper.selectCategoryChildrenByParentId(parentId);
        if(CollectionUtils.isEmpty(list)){
            logger.info("未找到当前分类的子分类！");
        }
        return ServiceResponse.creatBySuccess(list);
    }

    @Override
    public ServiceResponse<List<Integer>> findAllCategory(Integer categoryId) {
        Set<Category> set = Sets.newHashSet();
        findCategory(set , categoryId);

        List<Integer> categoryIdList = Lists.newArrayList();
        if(categoryId != null){
            for (Category categoryItem :set) {
                categoryIdList.add(categoryItem.getId());
            }
        }
        return ServiceResponse.creatBySuccess(categoryIdList);
    }
    //递归算法，计算子节点
    private Set<Category> findCategory(Set<Category> categorySet,Integer categoryId){
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null){
            categorySet.add(category);
        }
        List<Category> list = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        for(Category listItem : list){
            findCategory(categorySet,listItem.getId());
        }
        return categorySet;
    }
}
