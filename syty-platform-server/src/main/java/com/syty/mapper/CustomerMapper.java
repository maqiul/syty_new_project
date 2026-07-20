package com.syty.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.syty.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 根据手机号查询客户
     */
    @Select("SELECT * FROM customer WHERE phone = #{phone} LIMIT 1")
    Customer selectByPhone(@Param("phone") String phone);
}
