package sk.tuke.meta.persistence;

public class ColumnStructure {
    private final String fieldName;
    private final String columnName;
    private final String javaType;
    private final boolean isId;
    private final boolean isEntityReference;

    public ColumnStructure(String fieldName, String columnName, String javaType, boolean isId, boolean isEntityReference) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.isId = isId;
        this.isEntityReference = isEntityReference;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getGetterName() {
        if (("boolean".equals(javaType) || "Boolean".equals(javaType)) && !fieldName.startsWith("is")) {
            if (fieldName.length() > 0 && Character.isUpperCase(fieldName.charAt(0))) {
                return "is" + fieldName;
            } else if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1)) && fieldName.startsWith("is")) {
                return fieldName;
            }
            return "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public String getSetterName() {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public String getJavaType() {
        return javaType;
    }

    public boolean isId() {
        return isId;
    }

    public boolean getEntityReference() {
        return isEntityReference;
    }

    public String getSqlType() {
        if (this.isEntityReference) { // Still use the field directly here for internal logic
            return "BIGINT";
        }
        if (this.isId && (javaType.equals("long") || javaType.equals("Long") || javaType.equals("int") || javaType.equals("Integer"))) {
            return "INTEGER";
        }
        return switch (javaType) {
            case "String" -> "VARCHAR(255)";
            case "int", "Integer" -> "INTEGER";
            case "long", "Long" -> "BIGINT";
            case "boolean", "Boolean" -> "INTEGER";
            case "double", "Double", "float", "Float" -> "REAL";
            case "Date", "LocalDate", "LocalDateTime" -> "TEXT";
            default -> "TEXT";
        };
    }
}