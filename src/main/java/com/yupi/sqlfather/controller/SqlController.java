package com.yupi.sqlfather.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yupi.sqlfather.common.BaseResponse;
import com.yupi.sqlfather.common.ErrorCode;
import com.yupi.sqlfather.common.ResultUtils;
import com.yupi.sqlfather.core.GeneratorFacade;
import com.yupi.sqlfather.core.model.vo.GenerateVO;
import com.yupi.sqlfather.core.schema.TableSchema;
import com.yupi.sqlfather.core.schema.TableSchema.Field;
import com.yupi.sqlfather.core.schema.TableSchemaBuilder;
import com.yupi.sqlfather.exception.BusinessException;
import com.yupi.sqlfather.mapper.MonitorUserMapper;
import com.yupi.sqlfather.model.dto.GenerateByAutoRequest;
import com.yupi.sqlfather.model.dto.GenerateBySqlRequest;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.yupi.sqlfather.model.entity.MonitorUser;
import com.yupi.sqlfather.sort.SortField;
import com.yupi.sqlfather.sort.SortUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * SQL 相关接口
 *
 * @author https://github.com/liyupi
 */
@RestController
@RequestMapping("/sql")
@Slf4j
public class SqlController {

    @Resource
    private MonitorUserMapper mapper;

    @PostMapping("/generate/schema")
    public BaseResponse<GenerateVO> generateBySchema(@RequestBody TableSchema tableSchema) {
        return ResultUtils.success(GeneratorFacade.generateAll(tableSchema));
    }

    @GetMapping("/generate/test")
    public BaseResponse<Object> test(@RequestParam String type) {
        LambdaQueryWrapper<MonitorUser> queryWrapper = new LambdaQueryWrapper<>();
        List<MonitorUser> monitorUsers = mapper.selectList(queryWrapper);
        System.out.println();
        ArrayList<SortField> sortFields = new ArrayList<>();
        sortFields.add(build("city", 1, true));
        sortFields.add(build("college", 1, true));
        sortFields.add(build("age", 1, false));
        sortFields.add(build("username", 1, true));
        HashMap<String, Object> map = new HashMap<>();
        if ("1".equals(type)) {
            long begin = System.currentTimeMillis();
            List<MonitorUser> list = monitorUsers.stream().sorted(SortUtil.sort(sortFields)).collect(Collectors.toList());
            long after = System.currentTimeMillis();
            map.put("list", list);
            map.put("time", after -begin);
            return ResultUtils.success(map);
        } else if ("2".equals(type)) {
            long begin = System.currentTimeMillis();
            List<MonitorUser> list = monitorUsers.stream().sorted(SortUtil.sort(MonitorUser.class, sortFields)).collect(Collectors.toList());
            long after = System.currentTimeMillis();
            map.put("list", list);
            map.put("time", after -begin);
            return ResultUtils.success(map);
        } else {
            return ResultUtils.success(monitorUsers.size());
        }
    }

    private static SortField build(String fieldName, int asc, boolean containZh) {
        SortField sortField = new SortField();
        sortField.setFieldName(fieldName);
        sortField.setAsc(asc);
        sortField.setContainZh(containZh);
        return sortField;
    }

    @PostMapping("/get/schema/auto")
    public BaseResponse<TableSchema> getSchemaByAuto(@RequestBody GenerateByAutoRequest autoRequest) {
        if (autoRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(TableSchemaBuilder.buildFromAuto(autoRequest.getContent()));
    }

    /**
     * 根据 SQL 获取 schema
     *
     * @param sqlRequest
     * @return
     */
    @PostMapping("/get/schema/sql")
    public BaseResponse<TableSchema> getSchemaBySql(@RequestBody GenerateBySqlRequest sqlRequest) {
        if (sqlRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取 tableSchema
        return ResultUtils.success(TableSchemaBuilder.buildFromSql(sqlRequest.getSql()));
    }

    @PostMapping("/get/schema/excel")
    public BaseResponse<TableSchema> getSchemaByExcel(MultipartFile file) {
        return ResultUtils.success(TableSchemaBuilder.buildFromExcel(file));
    }

    /**
     * 下载模拟数据 Excel
     *
     * @param response
     */
    @PostMapping("/download/data/excel")
    public void downloadDataExcel(@RequestBody GenerateVO generateVO, HttpServletResponse response) {
        TableSchema tableSchema = generateVO.getTableSchema();
        String tableName = tableSchema.getTableName();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里 URLEncoder.encode 可以防止中文乱码
            String fileName = URLEncoder.encode(tableName + "表数据", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            // 设置表头
            List<List<String>> headList = new ArrayList<>();
            for (Field field : tableSchema.getFieldList()) {
                List<String> head = Collections.singletonList(field.getFieldName());
                headList.add(head);
            }
            List<String> fieldNameList = tableSchema.getFieldList().stream()
                    .map(Field::getFieldName).collect(Collectors.toList());
            // 设置数据
            List<List<Object>> dataList = new ArrayList<>();
            for (Map<String, Object> data : generateVO.getDataList()) {
                List<Object> dataRow = fieldNameList.stream().map(data::get).collect(Collectors.toList());
                dataList.add(dataRow);
            }
            // 这里需要设置不关闭流
            EasyExcel.write(response.getOutputStream())
                    .autoCloseStream(Boolean.FALSE)
                    .head(headList)
                    .sheet(tableName + "表")
                    .doWrite(dataList);
        } catch (Exception e) {
            // 重置 response
            response.reset();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }

}
