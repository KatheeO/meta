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

public class PersonDAO implements EntityDAO<Person> {
    private Connection connection;
    private DAOPersistenceManager daoPersistenceManager;

    public PersonDAO(DAOPersistenceManager daoPersistenceManager) {
        this.connection = daoPersistenceManager.getConnection();
        this.daoPersistenceManager = daoPersistenceManager;
    }

    @Override
    public void createTable() {
        // Start with CREATE TABLE and the primary key definition
        String sql = "CREATE TABLE IF NOT EXISTS \"Person\" ("
                + "\"id\" BIGINT PRIMARY KEY";

        // Append the other columns
            sql += ", \"surname\" VARCHAR(255)";            sql += ", \"name\" VARCHAR(255)";            sql += ", \"age\" INTEGER";
        // Append the closing parenthesis after the loop
        sql += ")";

        try (java.sql.Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create table Person", e);
        }
    }

    @Override
    public Optional<Person> get(long id) {
        // TODO: Implement select by id
        return Optional.empty();
    }

    @Override
    public List<Person> getAll() {
        // TODO: implement SELECT ALL
        return List.of();
    }

    @Override
    public void save(Object obj) {
        Person entity = (Person) obj;
        // TODO: implement insert and update
        // check if entity.getID is 0 or null --> insert, otherwise update
        // retrieve and set generated id after insert
    }

    @Override
    public void delete(Object obj) {
        Person entity = (Person) obj;
        if (entity.getId() == 0){
            throw new PersistenceException("Cannot delete entity with no ID: " + entity);
        }
        try {
            var statement = connection.prepareStatement(
                    "delete from \"Person\" where id=?");
            // Set the correct type based on IdColumn.JavaType (assuming long for now)
            statement.setLong(1, entity.getId());
            statement.execute();
        } catch (SQLException e) {
            throw new PersistenceException("Cannot delete Person with ID " + entity.getId(), e);
        }
    }
}
