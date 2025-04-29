package sk.tuke.meta.persistence;

import java.util.ArrayList;
import java.util.List;

public class EntityStructure {
    private final String name;
    private final String packageName;
    private ColumnStructure idColumn = null;
    private List<ColumnStructure> columns = new ArrayList<>();

    public EntityStructure(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
    }

    // Add column during analysis
    public void addColumn(ColumnStructure column) {
        this.columns.add(column);
        if (column.isId()){
            if (this.idColumn != null){
                System.err.println("Warning: Multiple @Id annotations found in " + getFullName());
            }
            this.idColumn = column;
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getDaoName() {
        return name + "DAO";
    }

    // Use table name from @Table if provided, otherwise use class name
    public String getTableName() {
        return name; // right now, use class name, need to update this later to use @Table annotation
    }

    public ColumnStructure getIdColumn() {
        if (idColumn == null) {
            throw new IllegalStateException("No @Id column found in " + getFullName());
        }
        return idColumn;
    }

    public List<ColumnStructure> getAllColumns() {
        return columns;
    }

    public List<ColumnStructure> getDataColumns(){
        List<ColumnStructure> dataColumns = new ArrayList<>();
        for (ColumnStructure column : columns) {
            if (!column.isId()){
                dataColumns.add(column);
            }
        }
        return dataColumns;
    }

    public String getFullName() {
        return packageName + "." + name;
    }

    public String getFullDaoName() {
        return packageName + "." + getDaoName();
    }

}
