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
import sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation;

// Add import for entity if not in example package
// This ensures that if the entity is in *either* of those common example packages, the explicit import isn't generated (as it would be redundant).


public class PersonDAO implements EntityDAO<Person> {
    private Connection connection;
    private DAOPersistenceManager daoPersistenceManager;

    public PersonDAO(DAOPersistenceManager daoPersistenceManager) {
        this.connection = daoPersistenceManager.getConnection();
        this.daoPersistenceManager = daoPersistenceManager;
    }

    @Override
    public void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS \"Person\" ("
                // Use the potentially adjusted SqlType and explicitly add PRIMARY KEY
                + "\"id\" INTEGER PRIMARY KEY";

            // --- START Foreign Key Handling ---
                // --- END Foreign Key Handling ---
                sql += ", \"surname\" VARCHAR(255)";
            // --- START Foreign Key Handling ---
                // --- END Foreign Key Handling ---
                sql += ", \"name\" VARCHAR(255)";
            // --- START Foreign Key Handling ---
                // --- END Foreign Key Handling ---
                sql += ", \"age\" INTEGER";
            // --- START Foreign Key Handling ---
                // Assuming FKs are BIGINT, adjust if needed.
                sql += ", \"department\" BIGINT";
        sql += ")";

        try (java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create table Person", e);
        }
    }

    @Override
    public Optional<Person> get(long id) {
        // Build SQL: SELECT "col1", "col2", ... "idCol" FROM "TableName" WHERE "idCol" = ?
        String sql = "SELECT ";
   // Default for ID and regular columns

                                sql += "\"id\"";
   // Default for ID and regular columns

                                sql += ", \"surname\"";
  // Default for ID and regular columns

                                sql += ", \"name\"";
  // Default for ID and regular columns

                                sql += ", \"age\"";
  // Default for ID and regular columns
 // For data columns that are entity references
 //FK column name is field name

                                sql += ", \"department\"";
        sql += " FROM \"Person\" WHERE \"id\" = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            // Set the ID parameter (assuming ID is long)
            statement.setLong(1, id);

            try (java.sql.ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    // Row found, map it to an entity object
                    Person entity = new Person();

                    // Map ID column
                    entity.setId(rs.getLong("id"));

                    // Map data columns
                        // --- START Foreign Key Handling ---
                                                    // --- END Foreign Key Handling ---
                                entity.setSurname(rs.getString("surname"));
                        // --- START Foreign Key Handling ---
                                                    // --- END Foreign Key Handling ---
                                entity.setName(rs.getString("name"));
                        // --- START Foreign Key Handling ---
                                                    // --- END Foreign Key Handling ---
                                entity.setAge(rs.getInt("age"));
                        // --- START Foreign Key Handling ---
                                                    // Define the FK column name based on convention
                            // Get the foreign key ID from the result set
                            long relatedId_Department = rs.getLong("department"); // MODIFIED variable name                            // Check if the ID is valid (not 0 and not SQL NULL)
                            if (relatedId_Department > 0 && !rs.wasNull()) { // Get the DAO for the related entity type
                                var relatedDAO_Department = this.daoPersistenceManager.getDAO(Department.class);
                                // Fetch the related entity using its DAO
                                Optional<Department> relatedEntityOpt_Department = relatedDAO_Department.get(relatedId_Department);
                                // Set the related entity on the current entity if found
                                relatedEntityOpt_Department.ifPresent(relatedEntity -> entity.setDepartment(relatedEntity));
                                // If not found (Optional is empty), the field remains null (default)
                            }

                    return Optional.of(entity);
                } else {
                    // No row found with that ID
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to get Person with ID " + id, e);
        }
    }

    @Override
    public List<Person> getAll() {
        List<Person> entities = new ArrayList<>(); // Initialize the list to store results
        // Build SQL: SELECT "col1", "col2", ... "idCol" FROM "TableName"
        String sql = "SELECT ";
   // Default for ID and regular columns

                sql += "\"id\"";    // Default for ID and regular columns

                sql += ", \"surname\"";   // Default for ID and regular columns

                sql += ", \"name\"";   // Default for ID and regular columns

                sql += ", \"age\"";   // Default for ID and regular columns
 // For data columns that are entity references
 // FK column name is field name

                sql += ", \"department\"";         sql += " FROM \"Person\"";

                try (Statement statement = connection.createStatement();
             java.sql.ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {                                 Person entity = new Person(); 
                                entity.setId(rs.getLong("id"));

                                    // --- START Foreign Key Handling ---
                                            // --- END Foreign Key Handling ---
                        // Handle regular data field
                            entity.setSurname(rs.getString("surname"));
                     // --- START Foreign Key Handling ---
                                            // --- END Foreign Key Handling ---
                        // Handle regular data field
                            entity.setName(rs.getString("name"));
                     // --- START Foreign Key Handling ---
                                            // --- END Foreign Key Handling ---
                        // Handle regular data field
                            entity.setAge(rs.getInt("age"));
                     // --- START Foreign Key Handling ---
                                                                    // Get the foreign key ID from the result set
                        long relatedId_Department = rs.getLong("department");
                        // Check if the ID is valid (not 0 and not SQL NULL)
                        if (relatedId_Department > 0 && !rs.wasNull()) {
                            // Get the DAO for the related entity type
                            var relatedDAO_Department = this.daoPersistenceManager.getDAO(Department.class);                            // Fetch the related entity using its DAO
                            Optional<Department> relatedEntityOpt_Department = relatedDAO_Department.get(relatedId_Department);                            // Set the related entity on the current entity if found
                            relatedEntityOpt_Department.ifPresent(relatedEntity -> entity.setDepartment(relatedEntity)); // relatedEntity here is fine (lambda scope)                            // If not found (Optional is empty), the field remains null (default)
                        }
  
                entities.add(entity); // Add the populated entity to the list
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to get all instances of Person", e);
        }
                                
        return entities;
    }

    @Override
    public void save(Object obj) {
        Person entity = (Person) obj;
        String sql;

        // Check if the entity is new (ID is 0 or default) or existing
        if (entity.getId() == 0) {
            // --- INSERT ---
            // Build SQL: INSERT INTO "TableName" ("col1", "col2") VALUES (?, ?)
            sql = "INSERT INTO \"Person\" (";

                    sql += "\"surname\"";

                    sql += ", \"name\"";
                    

                    sql += ", \"age\"";
                    
 // FK column name is field name

                    sql += ", \"department\"";
                    
            sql += ") VALUES (?, ?, ?, ?)";


            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                                int index = 1;
                                            // --- END Foreign Key Handling ---
                        // Handle regular data field (existing logic)
                            statement.setString(index++, entity.getSurname());
                                             // --- END Foreign Key Handling ---
                        // Handle regular data field (existing logic)
                            statement.setString(index++, entity.getName());
                                             // --- END Foreign Key Handling ---
                        // Handle regular data field (existing logic)
                            statement.setInt(index++, entity.getAge());
                                             // Handle relationship field
                        Object relatedEntity_Department = entity.getDepartment();
                        if (relatedEntity_Department == null) {
                            // Set the FK column to NULL. Adjust SQL type if FK isn't BIGINT.
                            statement.setNull(index++, java.sql.Types.BIGINT);
                        } else {
                            // Assumes the related entity has a getId() method returning long.
                            // (Department) will be replaced by the actual class name like "Department".
                            long relatedIdValue = ((Department) relatedEntity_Department).getId(); // Renamed inner var for clarity
                            statement.setLong(index++, relatedIdValue);
                        }
                  int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    throw new PersistenceException("Creating entity failed, no rows affected.");
                }

                // Retrieve and set the generated ID
                try (java.sql.ResultSet generatedKeys = statement.getGeneratedKeys()) {                     if (generatedKeys.next()) {
                        entity.setId(generatedKeys.getLong(1));
                    } else {
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
            sql = "UPDATE \"Person\" SET ";
                                
                    sql += "\"surname\" = ?";                                
                    sql += ", \"name\" = ?";                                
                    sql += ", \"age\" = ?";                                 
                    sql += ", \"department\" = ?";            sql += " WHERE \"id\" = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // Set parameters for data columns
                int index = 1;
                    // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            statement.setString(index++, entity.getSurname());
                     // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            statement.setString(index++, entity.getName());
                     // --- START Foreign Key Handling ---
                        // --- END Foreign Key Handling ---
                        // Handle regular data field
                            statement.setInt(index++, entity.getAge());
                     // --- START Foreign Key Handling ---
                         // Handle relationship field
                        Object relatedEntity_Department = entity.getDepartment();
                        if (relatedEntity_Department == null) {
                            statement.setNull(index++, java.sql.Types.BIGINT);
                        } else {
                            // Assumes the related entity has a getId() method returning long.
                            // (Department) will be replaced by the actual class name (e.g., "Department").
                            long relatedIdValue = ((Department) relatedEntity_Department).getId();
                            statement.setLong(index++, relatedIdValue);
                        }
  
                // Set the ID parameter for the WHERE clause (remains the same)
                statement.setLong(index, entity.getId());

                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    System.err.println("Warning: Update for " + entity.getName() + " with ID " + entity.getId() + " affected 0 rows.");
                    //throw new PersistenceException("Updating entity failed, no rows affected (ID " + entity.getId() + " might not exist).");
                }

            } catch (SQLException e) {
                throw new PersistenceException("Failed to save (update) entity: " + entity, e);
            }
            // --- End UPDATE ---
        }
    }

    @Override
    public void delete(Object obj) {
        Person entity = (Person) obj;
        if (entity.getId() == 0){
            throw new PersistenceException("Cannot delete entity with no ID: " + entity);
        }
        // In delete, we only care about the ID column, which is not an entity reference itself
        String sql = "DELETE FROM \"Person\" WHERE \"id\" = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)){
            // Set the correct type based on IdColumn.JavaType (assuming long for now)
            statement.setLong(1, entity.getId());
            //statement.execute();
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot delete Person with ID " + entity.getId(), e);
        }
    }
}
