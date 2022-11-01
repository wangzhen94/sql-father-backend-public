package com.yupi.sqlfather.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * Description: TODO
 *
 * @author wangzhen
 * @since 2022/11/1 15:41
 */
@Data
@TableName("monitor_user")
public class MonitorUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 用户名
     */
    @TableField("age")
    private Integer age;

    /**
     * 用户名
     */
    @TableField("city")
    private String city;

    /**
     * 用户名
     */
    @TableField("college")
    private String college;

    /**
     * 用户名
     */
    @TableField("ip")
    private String ip;

    /**
     * 用户名
     */
    @TableField("email")
    private String email;

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 是否删除(0-未删, 1-已删)
     */
/*    @TableLogic
    private Integer isDeleted;*/

}

