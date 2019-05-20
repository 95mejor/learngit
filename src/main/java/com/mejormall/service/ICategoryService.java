package com.mejormall.service;

import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.Category;

import java.util.List;
import java.util.Set;

public interface ICategoryService {
    ServiceResponse addCategory(String categoryName, Integer parentId);

    ServiceResponse setCategory(String categoryName,Integer categoryId);

    ServiceResponse findParallelCategory(Integer parentId);

    ServiceResponse<List<Integer>> findAllCategory(Integer categoryId);
}
