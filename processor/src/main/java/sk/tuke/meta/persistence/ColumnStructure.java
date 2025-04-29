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

    // Helper function to get SQL type, based on Java type, add more later if needed
    public String getSqlType() {
        switch (javaType){
            case "String":
                return "VARCHAR(255)";
            case "int", "Integer":
                return "INTEGER";
            case "long", "Long":
                return "BIGINT";
            default:
                return "VARCHAR(255)";
        }
    }
}
