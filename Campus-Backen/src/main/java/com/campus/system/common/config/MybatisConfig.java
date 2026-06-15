package com.campus.system.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//配置数据库的分页插件以及实体类公共字段的自动填充，如创建时间、更新时间、逻辑删除状态等
@Configuration
public class MybatisConfig {

    // 拦截器
    @Bean
    MybatisPlusInterceptor interceptor() {
        MybatisPlusInterceptor value = new MybatisPlusInterceptor();
        // 注入分页拦截器，支持各种主流数据库的分页查询
        value.addInnerInterceptor(new PaginationInnerInterceptor());
        return value;
    }

    //在往数据库插入或修改数据时，自动为指定的公共字段赋值，避免在业务代码中频繁手动set
    @Bean
    MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject o) {
                strictInsertFill(o, "createTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(o, "updateTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(o, "deleteState", Integer.class, 0);
            }
            @Override
            public void updateFill(MetaObject o) {
                strictUpdateFill(o, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
