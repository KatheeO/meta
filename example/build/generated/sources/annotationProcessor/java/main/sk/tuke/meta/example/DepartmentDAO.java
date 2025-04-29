package sk.tuke.meta.example;

import java.sql.Connection;
import java.sql.SQLException;
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
        // Start with CREATE TABLE and the primary key definition
        String sql = "CREATE TABLE IF NOT EXISTS \"Department\" ("
                + "\"id\" BIGINT PRIMARY KEY";

        // Append the other columns
            sql += ", \"name\" VARCHAR(255)";            sql += ", \"code\" VARCHAR(255)";
        // Append the closing parenthesis after the loop
        sql += ")";

        try (java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create table Department", e);
        }
    }

    @Override
    public Optional<Department> get(long id) {
        // TODO: Implement select by id
        return Optional.empty();
    }

    @Override
    public List<Department> getAll() {
        // TODO: implement SELECT ALL
        return List.of();
    }

    @Override
    public void save(Object obj) {
        Department entity = (Department) obj;
        // TODO: implement insert and update
        // check if entity.getID is 0 or null --> insert, otherwise update
        // retrieve and set generated id after insert
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
