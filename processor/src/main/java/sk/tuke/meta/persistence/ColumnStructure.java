package sk.tuke.meta.persistence;

public class ColumnStructure {
    private String fieldName;
    private String columnName;
    private String javaType;
    private boolean isId;

    public ColumnStructure(String fieldName, String columnName, String javaType, boolean isId) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.javaType = javaType;
        this.isId = isId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }


    public String getGetterName() {
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

    // Helper function to get SQL type
    public String getSqlType() {
        // Special handling for auto-increment PK
        if (this.isId && (javaType.equals("long") || javaType.equals("Long") || javaType.equals("int") || javaType.equals("Integer"))) {
            return "INTEGER"; // The PK part is added in the DAO template
        }

        // Handle other types
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
