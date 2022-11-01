package com.yupi.sqlfather.sort;

import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Description: 排序字段收集器
 *
 * @author wangzhen
 * @since 2022/10/22 11:58
 */
public class SortSqlCollector implements Collector<SortField, StringJoiner, String> {
    private static final String ORDER_BY = "ORDER BY";
    private static final String ASC = " ASC";
    private static final String DESC = " DESC";
    private static final String CONVERT = " CONVERT(`";
    private static final String COLLATE = "` USING gbk) COLLATE gbk_chinese_ci";

    @Override
    public Supplier<StringJoiner> supplier() {
        return () -> new StringJoiner(",", ORDER_BY, "");
    }

    @Override
    public BiConsumer<StringJoiner, SortField> accumulator() {
        return (sj, sortField) -> {
            String fieldName = sortField.getFieldName();
            boolean asc = sortField.getAsc() > 0;
            if (sortField.isContainZh()) {
                String funZhField = CONVERT + fieldName + COLLATE;
                sj = asc ? sj.add(funZhField + ASC) : sj.add(funZhField + DESC);
            } else {
                sj = asc ? sj.add(fieldName + ASC) : sj.add(fieldName + DESC);
            }
        };
    }

    @Override
    public BinaryOperator<StringJoiner> combiner() {
        return StringJoiner::merge;
    }

    @Override
    public Function<StringJoiner, String> finisher() {
        return StringJoiner::toString;
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
