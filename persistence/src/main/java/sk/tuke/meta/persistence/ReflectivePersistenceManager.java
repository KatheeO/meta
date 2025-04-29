package sk.tuke.meta.persistence;

import sk.tuke.meta.persistence.annotations.Column;
import sk.tuke.meta.persistence.annotations.Id;
import sk.tuke.meta.persistence.annotations.Table;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.*;
import java.util.*;

// https://www.geeksforgeeks.org/create-edit-alter-tables-using-java/


public class ReflectivePersistenceManager implements PersistenceManager {
    private final Connection connection;

    public ReflectivePersistenceManager(Connection connection) {
        if (connection == null) {
            System.out.println("Connection is null!");
        }
        this.connection = connection;
    }

    private String getTableName(Class<?> type) {
        Table tableAnnotation = type.getAnnotation(Table.class);
        String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
                ? tableAnnotation.name()
                : type.getSimpleName();
        return tableName;
    }

    private Field getIdField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new RuntimeException("No @Id field found in class: " + type.getSimpleName());
    }


    @Override
    public void createTables(Class<?>... types) throws SQLException {
        for (Class<?> type : types) {
//            Table tableAnnotation = type.getAnnotation(Table.class);
//            if (tableAnnotation == null) {
//                System.out.println("Skipping class without @Table annotation: " + type.getSimpleName());
//                continue;
//            }
//
//            String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
//                    ? tableAnnotation.name()
//                    : type.getSimpleName();
            String tableName = getTableName(type);

            System.out.println("Creating table: " + tableName);

            StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            sql.append("'").append(tableName).append("' (");

            List<String> columnDefs = new ArrayList<>();
            List<String> foreignKeys = new ArrayList<>();

            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                Id idAnnotation = field.getAnnotation(Id.class);

                if (idAnnotation != null) {
                    columnDefs.add("'" + field.getName() + "' INTEGER PRIMARY KEY AUTOINCREMENT");
                } else if (columnAnnotation != null) {
                    String columnName = !columnAnnotation.name().isEmpty()
                            ? columnAnnotation.name()
                            : field.getName();

                    StringBuilder colDef = new StringBuilder();
                    colDef.append("'").append(columnName).append("' ")
                            .append(getSQLType(field.getType()));

                    if (!columnAnnotation.nullable()) {
                        colDef.append(" NOT NULL");
                    }
                    if (columnAnnotation.unique()) {
                        colDef.append(" UNIQUE");
                    }

                    columnDefs.add(colDef.toString());
                } else if (isEntityReference(field)) {
                    // Reference to another @Table entity
                    Class<?> refType = field.getType();
                    Table refTable = refType.getAnnotation(Table.class);
                    if (refTable != null) {
                        String refTableName = (refTable != null && !refTable.name().isEmpty())
                                ? refTable.name()
                                : refType.getSimpleName();
                        String columnName = field.getName() + "_id"; // Corrected column name for FK
                        columnDefs.add("'" + columnName + "' INTEGER");
                        foreignKeys.add("FOREIGN KEY('" + columnName + "') REFERENCES '" + refTableName + "'(id)");
                    }
                }
            }

            // Combine columns and foreign key constraints
            sql.append(String.join(", ", columnDefs));
            if (!foreignKeys.isEmpty()) {
                sql.append(", ").append(String.join(", ", foreignKeys));
            }

            sql.append(");");

            System.out.println(sql.toString());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql.toString());
            }
        }
    }

//    @Override
//    public void createTables(Class<?>... types) throws SQLException {
//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//        System.out.println("Creating tables for " + types.length + " classes");
//
//        try {
//            System.out.println("Try block");
//            Enumeration<URL> resources = classLoader.getResources("META-INF/table-definitions");
//            System.out.println("Found " + resources + " resources");
//            while (resources.hasMoreElements()) {
//                System.out.println("First while: " + resources.nextElement());
//                URL resource = resources.nextElement();
//                File dir = new File(resource.toURI());
//
//                for (File file : Objects.requireNonNull(dir.listFiles())) {
//                    System.out.println("File: " + file.getAbsolutePath());
//                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//                        String tableName = null;
//                        List<String> columns = new ArrayList<>();
//
//                        String line;
//                        while ((line = reader.readLine()) != null) {
//                            if (line.startsWith("table=")) {
//                                tableName = line.substring("table=".length());
//                                System.out.println(tableName);
//                            } else if (line.startsWith("columns=")) {
//                                String colsStr = line.substring("columns=".length());
//                                System.out.println(colsStr);
//                                columns.addAll(Arrays.asList(colsStr.split(",")));
//                            }
//                        }
//
//                        // Use tableName and columns to build SQL
//                        System.out.println("CREATE TABLE for " + tableName + " with " + columns);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

    private boolean isEntityReference(Field field) {
        Class<?> type = field.getType();
        return !type.isPrimitive() &&
                !type.equals(String.class) &&
                type.isAnnotationPresent(Table.class);
    }



    private String getSQLType(Class<?> type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "INTEGER";
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "REAL";
        } else if (type == String.class) {
            return "TEXT";
        } else {
            return "TEXT";
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type, long id) {
        try {
            // Get table name
            Table tableAnnotation = type.getAnnotation(Table.class);
            String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
                    ? tableAnnotation.name()
                    : type.getSimpleName();

            System.out.println("Executing SQL: SELECT * FROM '" + tableName + "' WHERE id = ?");

            String sql = "SELECT * FROM '" + tableName + "' WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        T instance = type.getDeclaredConstructor().newInstance();

                        for (Field field : type.getDeclaredFields()) {
                            field.setAccessible(true);

                            // Skip id field for now, we'll set it separately
                            if (field.isAnnotationPresent(Id.class)) {
                                field.set(instance, id);
                                continue;
                            }

                            // Handle @Column fields
                            Column columnAnnotation = field.getAnnotation(Column.class);
                            if (columnAnnotation != null) {
                                String columnName = !columnAnnotation.name().isEmpty()
                                        ? columnAnnotation.name()
                                        : field.getName();

                                Object value = null;

                                // Convert appropriate data types
                                if (field.getType() == String.class) {
                                    value = rs.getString(columnName);
                                } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                    value = rs.getInt(columnName);
                                } else if (field.getType() == long.class || field.getType() == Long.class) {
                                    value = rs.getLong(columnName);
                                } else if (field.getType() == double.class || field.getType() == Double.class) {
                                    value = rs.getDouble(columnName);
                                } else {
                                    value = rs.getObject(columnName);
                                }

                                if (!rs.wasNull()) {
                                    field.set(instance, value);
                                }
                            }
                            // Handle entity references (foreign keys)
                            else if (isEntityReference(field)) {
                                String columnName = field.getName() + "_id";
                                long refId = rs.getLong(columnName);

                                if (!rs.wasNull() && refId > 0) {
                                    Object refObject = get(field.getType(), refId).orElse(null);
                                    field.set(instance, refObject);
                                }
                            }
                        }

                        return Optional.of(instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }



    @Override
    public <T> List<T> getAll(Class<T> type) {
        List<T> results = new ArrayList<>();

        try {
            // Get table name
//            Table tableAnnotation = type.getAnnotation(Table.class);
//            String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
//                    ? tableAnnotation.name()
//                    : type.getSimpleName();
            String tableName = getTableName(type);

            System.out.println("Executing SQL: SELECT * FROM '" + tableName + "'");

            String sql = "SELECT * FROM '" + tableName + "'";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    T instance = type.getDeclaredConstructor().newInstance();

                    for (Field field : type.getDeclaredFields()) {
                        field.setAccessible(true);

                        // Handle @Id field
                        if (field.isAnnotationPresent(Id.class)) {
                            field.set(instance, rs.getLong("id"));
                            continue;
                        }

                        // Handle @Column fields
                        Column columnAnnotation = field.getAnnotation(Column.class);
                        if (columnAnnotation != null) {
                            String columnName = !columnAnnotation.name().isEmpty()
                                    ? columnAnnotation.name()
                                    : field.getName();

                            Object value = null;

                            // Convert appropriate data types
                            if (field.getType() == String.class) {
                                value = rs.getString(columnName);
                            } else if (field.getType() == int.class || field.getType() == Integer.class) {
                                value = rs.getInt(columnName);
                            } else if (field.getType() == long.class || field.getType() == Long.class) {
                                value = rs.getLong(columnName);
                            } else if (field.getType() == double.class || field.getType() == Double.class) {
                                value = rs.getDouble(columnName);
                            } else {
                                value = rs.getObject(columnName);
                            }

                            if (!rs.wasNull()) {
                                field.set(instance, value);
                            }
                        }
                        // Handle entity references (foreign keys)
                        else if (isEntityReference(field)) {
                            String columnName = field.getName() + "_id";
                            long refId = rs.getLong(columnName);

                            if (!rs.wasNull() && refId > 0) {
                                Object refObject = get(field.getType(), refId).orElse(null);
                                field.set(instance, refObject);
                            }
                        }
                    }

                    results.add(instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }


    @Override
    public void save(Object entity) throws SQLException {
        try {
            Class<?> type = entity.getClass();
//            Field idField = type.getDeclaredField("id");
//            idField.setAccessible(true);
//            long id = (long) idField.get(entity);

            // Check for foreign key references
            Field idField = null;
            long id = 0;

            for (Field field : type.getDeclaredFields()) {
                // Find ID field
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    idField.setAccessible(true);
                    id = (long) idField.get(entity);
                }
                else {
                    idField = type.getDeclaredField("id");
                    idField.setAccessible(true);
                    id = (long) idField.get(entity);
                }

                field.setAccessible(true);
                Object fieldValue = field.get(entity);

                if (fieldValue != null && !field.getType().isPrimitive() && !field.getType().equals(String.class)) {
                    // Ensure referenced object is already saved
                    Field refIdField = field.getType().getDeclaredField("id");
                    refIdField.setAccessible(true);
                    long refId = (long) refIdField.get(fieldValue);

                    if (refId == 0) {
                        throw new IllegalArgumentException("Cannot save entity with unsaved reference: " + field.getName());
                    }
                }
            }

            if (id == 0) {
                // Insert new entity
                StringBuilder sql = new StringBuilder("INSERT INTO ");
                String tableName = getTableName(type);
                // -----------------------------------------------------------------------------------------------------------------
//                Table tableAnnotation = type.getAnnotation(Table.class);
//                String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
//                        ? tableAnnotation.name()
//                        : type.getSimpleName();

                sql.append("'").append(tableName).append("' (");

                List<String> columnNames = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.getAnnotation(Id.class) != null) {
                        continue; // Skip ID
                    }

                    Column columnAnnotation = field.getAnnotation(Column.class);
                    if (columnAnnotation != null) {
                        String columnName = !columnAnnotation.name().isEmpty()
                                ? columnAnnotation.name()
                                : field.getName();
                       // System.out.println("Column: " + columnName);
                        columnNames.add("'" + columnName + "'");
                        values.add(field.get(entity));
                    } else if (isEntityReference(field)) {
                        // Add foreign key reference
                        Object refObject = field.get(entity);
                        if (refObject != null) {
                            Field refIdField = refObject.getClass().getDeclaredField("id");
                            refIdField.setAccessible(true);
                            long refId = (long) refIdField.get(refObject);

                            if (refId == 0) {
                                throw new IllegalArgumentException("Referenced object not saved: " + field.getName());
                            }

                            columnNames.add("'" + field.getName() + "_id'");
                            values.add(refId);
                        }
                    }
                }

                sql.append(String.join(", ", columnNames));
                sql.append(") VALUES (").append("?,".repeat(values.size()));
                sql.setLength(sql.length() - 1); // Remove last comma
                sql.append(");");

                try (PreparedStatement stmt = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                    for (int i = 0; i < values.size(); i++) {
                        stmt.setObject(i + 1, values.get(i));
                    }
                    stmt.executeUpdate();

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            idField.set(entity, generatedKeys.getLong(1));
                        }
                    } catch (SQLException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                // Update existing entity
                StringBuilder sql = new StringBuilder("UPDATE ");
//                Table tableAnnotation = type.getAnnotation(Table.class);
//                String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
//                        ? tableAnnotation.name()
//                        : type.getSimpleName();
                String tableName = getTableName(type);

                sql.append("'").append(tableName).append("' SET ");

                List<String> assignments = new ArrayList<>();
                List<Object> values = new ArrayList<>();

                for (Field field : type.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.getAnnotation(Id.class) != null) continue;

                    Column columnAnnotation = field.getAnnotation(Column.class);
                    if (columnAnnotation != null) {
                        String columnName = !columnAnnotation.name().isEmpty()
                                ? columnAnnotation.name()
                                : field.getName();
                        assignments.add("'" + columnName + "' = ?");
                        values.add(field.get(entity));
                    } else if (isEntityReference(field)) {
                        Object refObject = field.get(entity);
                        if (refObject != null) {
                            Field refIdField = refObject.getClass().getDeclaredField("id");
                            refIdField.setAccessible(true);
                            long refId = (long) refIdField.get(refObject);

                            if (refId == 0) {
                                throw new IllegalArgumentException("Referenced object not saved: " + field.getName());
                            }

                            assignments.add("'" + field.getName() + "_id' = ?");
                            values.add(refId);
                        }
                    }
                }

                sql.append(String.join(", ", assignments));
                sql.append(" WHERE id = ?;");
                values.add(id);

                try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    for (int i = 0; i < values.size(); i++) {
                        stmt.setObject(i + 1, values.get(i));
                    }
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Object entity) {
        try {
            Class<?> type = entity.getClass();

            // Get table name
//            Table tableAnnotation = type.getAnnotation(Table.class);
//            String tableName = (tableAnnotation != null && !tableAnnotation.name().isEmpty())
//                    ? tableAnnotation.name()
//                    : type.getSimpleName();
            String tableName = getTableName(type);

            // Find ID field
            Field idField = null;
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    idField = field;
                    idField.setAccessible(true);
                    break;
                }
            }

            if (idField == null) {
                throw new RuntimeException("No @Id field found in class: " + type.getSimpleName());
            }

            long id = (long) idField.get(entity);

            if (id == 0) {
                throw new IllegalArgumentException("Cannot delete an unsaved entity (id is 0).");
            }

            System.out.println("Executing SQL: DELETE FROM '" + tableName + "' WHERE id = ?");

            String sql = "DELETE FROM '" + tableName + "' WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
