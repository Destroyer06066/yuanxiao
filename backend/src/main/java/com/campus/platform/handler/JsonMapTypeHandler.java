package com.campus.platform.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Type handler for Map&lt;String, BigDecimal&gt; fields → PostgreSQL JSONB column.
 */
@MappedTypes(Map.class)
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, BigDecimal>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, BigDecimal>> TYPE_REF =
            new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     Map<String, BigDecimal> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            String json = MAPPER.writeValueAsString(parameter);
            ps.setObject(i, json, java.sql.Types.OTHER);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize Map to JSON", e);
        }
    }

    @Override
    public Map<String, BigDecimal> getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        String json = rs.getString(columnName);
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize JSON to Map", e);
        }
    }

    @Override
    public Map<String, BigDecimal> getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize JSON to Map", e);
        }
    }

    @Override
    public Map<String, BigDecimal> getNullableResult(java.sql.CallableStatement cs,
                                                      int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize JSON to Map", e);
        }
    }
}
