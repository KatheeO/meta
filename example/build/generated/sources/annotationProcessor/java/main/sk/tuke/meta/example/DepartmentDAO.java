package sk.tuke.meta.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.sql.Statement;

import sk.tuke.meta.persistence.DAOPersistenceManager;
import sk.tuke.meta.persistence.EntityDAO;
import sk.tuke.meta.persistence.PersistenceException;

// Add import for entity if not in example package

public class DepartmentDAO implements EntityDAO<Department> {
    private Connection connection;
    private DAOPersistenceManager daoPersistenceManager;

    public DepartmentDAO(DAOPersistenceManager daoPersistenceManager) {
        this.connection = daoPersistenceManager.getConnection();
        this.daoPersistenceManager = daoPersistenceManager;
    }

    @Override
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS \"Department\" ("
                // Use the potentially adjusted SqlType and explicitly add PRIMARY KEY
                + "\"id\" INTEGER PRIMARY KEY";

            // --- START Foreign Key Handling ---
                // --- END Foreign Key Handling ---
                sql += ", \"name\" VARCHAR(255)";
            // --- START Foreign Key Handling ---
                // --- END Foreign Key Handling ---
                sql += ", \"code\" VARCHAR(255)";
        sql += ")";

        try (java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create table Department", e);
        }
    }

    @Override
    public Optional<Department> get(long id) {
        // Build SQL: SELECT "col1", "col2", ... "idCol" FROM "TableName" WHERE "idCol" = ?
        String sql = "SELECT ";
                  sql += "\"id\"";                   sql += ", \"name\"";                  sql += ", \"code\"";         sql += " FROM \"Department\" WHERE \"id\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // Set the ID parameter (assuming ID is long)
            statement.setLong(1, id);

            try (java.sql.ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    // Row found, map it to an entity object
                    Department entity = new Department();

                    // Map ID column
                    entity.setId(rs.getLong("id"));

                    // Map data columns
                        // --- START Foreign Key Handling ---
                            // --- END Foreign Key Handling ---
                                entity.setName(rs.getString("name"));
                        // --- START Foreign Key Handling ---
                            // --- END Foreign Key Handling ---
                                entity.setCode(rs.getString("code"));

                    return Optional.of(entity);
                } else {
                    // No row found with that ID
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to get Department with ID " + id, e);
        }
    }

    @Override
    public List<Department> getAll() {
        List<Department> entities = new ArrayList<>(); // Initialize the list to store results
        // Build SQL: SELECT "col1", "col2", ... "idCol" FROM "TableName"
        String sql = "SELECT ";
                  sql += "\"id\"";                   sql += ", \"name\"";                  sql += ", \"code\"";         sql += " FROM \"Department\"";

        // Use a simple Statement as there are no parameters
        try (Statement statement = connection.createStatement();
             java.sql.ResultSet rs = statement.executeQuery(sql)) { // Execute the query

            while (rs.next()) { // Iterate through all rows in the result set
                // For each row, map it to an entity object
                Department entity = new Department(); // Assumes no-arg constructor

                // Map ID column
                entity.setId(rs.getLong("id"));

                // Map data columns
                    // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            entity.setName(rs.getString("name"));
                     // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            entity.setCode(rs.getString("code"));
  
                entities.add(entity); // Add the populated entity to the list
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to get all instances of Department", e);
        }
        // Add catch for ClassNotFoundException if using Class.forName in the default case above
        // catch (ClassNotFoundException e) {
        //     throw new PersistenceException("Failed to load class for mapping in getAll method", e);
        // }

        return entities;
    }

    @Override
    public void save(Object obj) {
        Department entity = (Department) obj;
        String sql;

        // Check if the entity is new (ID is 0 or default) or existing
        if (entity.getId() == 0) {
            // --- INSERT ---
            // Build SQL: INSERT INTO "TableName" ("col1", "col2") VALUES (?, ?)
            sql = "INSERT INTO \"Department\" (";
                    sql += "\"name\"";                    sql += ", \"code\"";            sql += ") VALUES (";
                    sql += "?";                    sql += ", ?";            sql += ")";

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                // Set parameters using data columns
                int index = 1;
                    // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field (existing logic)
                            statement.setString(index++, entity.getName());
                     // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field (existing logic)
                            statement.setString(index++, entity.getCode());
                  int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new PersistenceException("Creating entity failed, no rows affected.");
                }

                // Retrieve and set the generated ID
                // Retrieve and set the generated ID
                try (java.sql.ResultSet generatedKeys = statement.getGeneratedKeys()) {                     if (generatedKeys.next()) {
                        long generatedId = generatedKeys.getLong(1);
                        //System.out.println(">>> DAO: Generated ID for Department: " + generatedId);
                        // Assuming ID is long. Adjust if type is different.
                        entity.setId(generatedId); // Use the retrieved value
                        //System.out.println(">>> DAO: ID in entity object AFTER set for Department: " + entity.getId());
                    } else {
                        //System.out.println(">>> DAO: No generated ID obtained for Department."); // Temporary logging
                        // ------------------------
                        throw new PersistenceException("Creating entity failed, no ID obtained.");
                    }
                }

            } catch (SQLException e) {
                throw new PersistenceException("Failed to save (insert) entity: " + entity, e);
            }
            // --- End INSERT ---
        } else {
            // --- UPDATE ---
            // Build SQL: UPDATE "TableName" SET "col1" = ?, "col2" = ? WHERE "idCol" = ?
            sql = "UPDATE \"Department\" SET ";
                // --- START Foreign Key Handling ---
                 // --- END Foreign Key Handling ---

                    sql += "\"name\" = ?";                // --- START Foreign Key Handling ---
                 // --- END Foreign Key Handling ---

                    sql += ", \"code\" = ?";            sql += " WHERE \"id\" = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set parameters for data columns
                int index = 1;
                    // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            statement.setString(index++, entity.getName());
                     // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            statement.setString(index++, entity.getCode());
  
                // Set the ID parameter for the WHERE clause (remains the same)
                statement.setLong(index, entity.getId());

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new PersistenceException("Updating entity failed, no rows affected (ID " + entity.getId() + " might not exist).");
                }

            } catch (SQLException e) {
                throw new PersistenceException("Failed to save (update) entity: " + entity, e);
            }
            // --- End UPDATE ---
        }
    }

    @Override
    public void delete(Object obj) {
        Department entity = (Department) obj;
        if (entity.getId() == 0){
            throw new PersistenceException("Cannot delete entity with no ID: " + entity);
        }
        try {
            var statement = connection.prepareStatement(
                    "delete from \"Department\" where id=?");
            // Set the correct type based on IdColumn.JavaType (assuming long for now)
            statement.setLong(1, entity.getId());
            statement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot delete Department with ID " + entity.getId(), e);
        }
    }
}
